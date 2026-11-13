package com.jcotters.internal.watchlists

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.cotters.data.network.NetworkMonitor
import com.cotters.data.network.NetworkStatus
import com.jcotters.contract.watchlists.data.ActivityPage
import com.jcotters.contract.watchlists.data.FilmActivity
import com.jcotters.contract.watchlists.data.ImportBatchResult
import com.jcotters.contract.watchlists.data.ImportUnavailableException
import com.jcotters.contract.watchlists.data.InvitePreview
import com.jcotters.contract.watchlists.data.SharedAccessDeniedException
import com.jcotters.contract.watchlists.data.SharedCommand
import com.jcotters.contract.watchlists.data.SharedConnectionException
import com.jcotters.contract.watchlists.data.SharedFilm
import com.jcotters.contract.watchlists.data.SharedWatchlistRemote
import com.jcotters.contract.watchlists.data.SharingUnavailableException
import com.jcotters.contract.watchlists.domain.ISharedWatchlistsRepository
import com.jcotters.contract.watchlists.domain.SharedSyncStatus
import com.jcotters.contract.watchlists.domain.WatchlistInvitation
import com.jcotters.contract.watchlists.domain.WatchlistNotFoundException
import com.jcotters.database.MoversDatabase
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.watchlists.DbSharedActivity
import com.jcotters.database.watchlists.DbSharedActivityCursor
import com.jcotters.database.watchlists.DbSharedList
import com.jcotters.database.watchlists.DbSharedOperation
import com.jcotters.database.watchlists.DbWatchlist
import com.jcotters.database.watchlists.DbWatchlistMovie
import com.jcotters.profile.domain.DeviceProfileRepository
import com.jcotters.profile.domain.ParticipantIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

internal class SharedWatchlistsRepository(
    private val database: MoversDatabase,
    private val remote: SharedWatchlistRemote,
    private val identity: ParticipantIdentity,
    private val profiles: DeviceProfileRepository,
    private val network: NetworkMonitor,
) : ISharedWatchlistsRepository {
    private val cache = database.sharedCacheDao()
    private val lists = database.watchlistDao()
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val sync = kotlinx.coroutines.flow.MutableStateFlow(SharedSyncStatus())
    override val syncStatus = sync.asStateFlow()
    override val configured get() = remote.configured
    override fun observeActivity(listId: String) =
        cache.observeActivity(listId).map { rows -> rows.map { json.decodeFromString<FilmActivity>(it.payload) } }

    override suspend fun refresh(listId: String?): Result<Unit> {
        sync.value = sync.value.copy(refreshing = true)
        return try {
            refreshLists(listId).also { outcome ->
                sync.value = SharedSyncStatus(
                    checkedAt = if (outcome.isSuccess) kotlin.time.Clock.System.now()
                        .toEpochMilliseconds() else sync.value.checkedAt, error = outcome.exceptionOrNull()?.message
                )
            }
        } finally {
            sync.value = sync.value.copy(refreshing = false)
        }
    }

    private suspend fun refreshLists(listId: String?): Result<Unit> = result {
        if (!configured) return@result
        mutex.withLock {
            online()
            val id = identity.currentId() ?: run {
                check(
                    cache.lists().isEmpty() && cache.operations().isEmpty()
                ) { "This device's sharing identity is unavailable. Retry without clearing app data." }
                return@withLock
            }
            checkOwner(id)
            reconcile(id)
            val ids = remote.listIds().toSet()
            cache.lists().filter { it.listId !in ids }.forEach { purge(it.listId) }
            (if (listId == null) ids else setOf(listId).intersect(ids)).forEach { fetch(it, id) }
        }
    }

    override suspend fun publish(listId: String): Result<Unit> = result {
        mutex.withLock {
            val owner = participant()
            reconcile(owner)
            if (cache.get(listId) != null) return@withLock
            val content = lists.observeContent(listId).first() ?: throw WatchlistNotFoundException()
            val films = content.entries.map { entry ->
                entry.movie.let {
                    SharedFilm(
                        it.id,
                        it.title,
                        it.synopsis,
                        it.releaseDate,
                        it.posterUrl
                    )
                }
            }
            command(owner, SharedCommand.Publish(listId, content.list.name, films))
        }
    }

    override suspend fun preview(invitation: String): Result<InvitePreview> = result {
        mutex.withLock { participant(); remote.preview(WatchlistInvitation.token(invitation)) }
    }

    override suspend fun join(invitation: String): Result<String> = result {
        mutex.withLock {
            val owner = participant()
            reconcile(owner)
            val token = WatchlistInvitation.token(invitation)
            val preview = remote.preview(token)
            command(owner, SharedCommand.Join(preview.listId, token))
            preview.listId
        }
    }

    override suspend fun invite(listId: String): Result<String> = result {
        mutex.withLock {
            val owner = participant(); reconcile(owner); WatchlistInvitation.url(remote.createInvite(listId))
        }
    }

    override suspend fun revokeInvites(listId: String) = mutate(SharedCommand.RevokeInvites(listId))
    override suspend fun leave(listId: String) = mutate(SharedCommand.Leave(listId))
    override suspend fun loadActivity(listId: String, older: Boolean): Result<Boolean> = result {
        mutex.withLock {
            val owner = participant()
            val cursor = cache.cursor(listId)
            if (older && cursor != null && cursor.nextBefore == null) return@withLock false
            val page = remote.activity(listId, if (older) cursor?.nextBefore else null)
            checkOwner(owner)
            saveActivity(listId, page)
            page.nextBefore != null
        }
    }

    suspend fun mutate(value: SharedCommand): Result<Unit> = result {
        mutex.withLock { val owner = participant(); reconcile(owner); command(owner, value) }
    }

    suspend fun importMovies(
        requestId: String,
        listId: String,
        films: List<SharedFilm>,
        local: suspend () -> ImportBatchResult
    ): ImportBatchResult =
        mutex.withLock {
            if (cache.get(listId) == null) {
                check(
                    cache.operations()
                        .none { it.listId == listId }) { "This list is being shared. Reconnect and retry." }
                return@withLock local()
            }
            val owner = participant()
            // Reconcile other commands, but preserve this batch's original receipt/outcome.
            cache.operations().filter { it.id != requestId }.forEach { execute(it) }
            val command = SharedCommand.ImportFilms(listId, films)
            val payload = json.encodeToString<SharedCommand>(command)
            val operation = cache.operations().firstOrNull { it.id == requestId }?.also {
                check(it.participantId == owner && it.payload == payload) { "Import request changed. Reopen the import." }
            } ?: DbSharedOperation(requestId, listId, owner, payload).also { cache.insert(it) }
            checkNotNull(execute(operation))
        }

    /** The same mutex prevents a local write racing publication of its original contents. */
    suspend fun write(listIds: Set<String>, local: suspend () -> Unit, shared: suspend (String) -> SharedCommand) {
        mutex.withLock {
            val sharedIds = listIds.filter { cache.get(it) != null }
            if (sharedIds.isEmpty()) {
                check(
                    cache.operations()
                        .none { it.listId in listIds }) { "This list is being shared. Reconnect and retry." }
                local()
            } else {
                require(listIds.size == 1) { "Change one shared list at a time." }
                val owner = participant()
                reconcile(owner)
                command(owner, shared(sharedIds.single()))
            }
        }
    }

    private suspend fun participant(): String {
        online()
        if (!configured) throw SharingUnavailableException()
        val current = identity.currentId()
        check(
            current != null || (cache.lists().isEmpty() && cache.operations().isEmpty())
        ) { "This device's sharing identity is unavailable. Retry without clearing app data." }
        val profile = profiles.profile.first() ?: error("Set your name first.")
        check(profile.nameConfirmed) { "Confirm your name before sharing." }
        val id = identity.ensure(profile.displayName, profile.color)
        checkOwner(id)
        return id
    }

    private suspend fun online() {
        if (network.networkStatus.first() == NetworkStatus.Offline) throw SharedConnectionException()
    }

    private suspend fun checkOwner(id: String) {
        check(cache.lists().all { it.participantId == id } && cache.operations().all { it.participantId == id }) {
            "Saved lists belong to a different device identity. Sharing is unavailable."
        }
    }

    private suspend fun command(owner: String, command: SharedCommand) {
        val operation = DbSharedOperation(Uuid.random().toString(), command.listId, owner, json.encodeToString(command))
        cache.insert(operation)
        execute(operation)
    }

    private suspend fun reconcile(owner: String) {
        cache.operations().forEach { operation ->
            check(operation.participantId == owner)
            execute(operation)
        }
    }

    private suspend fun execute(operation: DbSharedOperation): ImportBatchResult? {
        val command = json.decodeFromString<SharedCommand>(operation.payload)
        try {
            val importResult =
                if (command is SharedCommand.ImportFilms) remote.executeImport(operation.id, command).also { result ->
                    check(result.outcomes.map { it.movieId }.toSet() == command.films.map { it.id }
                        .toSet() && result.outcomes.size == command.films.size) {
                        "Import confirmation is incomplete. Retry when connected."
                    }
                } else {
                    remote.execute(operation.id, command)
                    null
                }
            if (command is SharedCommand.Leave) purge(command.listId) else fetch(
                command.listId,
                operation.participantId
            )
            cache.complete(operation.id)
            return importResult
        } catch (e: SharedAccessDeniedException) {
            if (cache.get(command.listId) != null) purge(command.listId)
            cache.complete(operation.id)
            throw e
        } catch (e: IllegalArgumentException) {
            cache.complete(operation.id)
            throw e
        } catch (e: ImportUnavailableException) {
            cache.complete(operation.id)
            throw e
        }
        // A transport failure retains the request ID. Reconciliation repeats that exact command.
    }

    private suspend fun fetch(id: String, owner: String) {
        try {
            val snapshot = remote.snapshot(id)
            database.useWriterConnection { connection ->
                connection.immediateTransaction<Unit> {
                    if ((cache.get(id)?.revision ?: -1) <= snapshot.info.revision) {
                        database.movieDao().insertMissingMovies(snapshot.entries.map { it.film.toDb() })
                        lists.upsert(DbWatchlist(id, snapshot.name, snapshot.createdAt))
                        lists.clearMovies(id)
                        snapshot.entries.forEach {
                            lists.insertMovie(
                                DbWatchlistMovie(
                                    id,
                                    it.film.id,
                                    it.contribution.addedAt
                                )
                            )
                        }
                        cache.put(DbSharedList(id, owner, snapshot.info.revision, json.encodeToString(snapshot)))
                    }
                }
            }
        } catch (e: SharedAccessDeniedException) {
            purge(id); throw e
        }
    }

    private suspend fun saveActivity(id: String, page: ActivityPage) {
        database.movieDao().insertMissingMovies(page.events.map { it.film.toDb() })
        database.useWriterConnection { connection ->
            connection.immediateTransaction<Unit> {
                cache.putActivity(page.events.map { DbSharedActivity(id, it.id, it.sequence, json.encodeToString(it)) })
                cache.putCursor(DbSharedActivityCursor(id, page.nextBefore))
            }
        }
    }

    private suspend fun purge(id: String) {
        database.useWriterConnection { connection ->
            connection.immediateTransaction<Unit> {
                cache.remove(id); cache.clearActivity(id); cache.clearCursor(id); cache.discard(id); lists.delete(id)
            }
        }
    }

    private suspend fun <T> result(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun SharedFilm.toDb() = DbMovie(id, title, synopsis, releaseDate, posterUrl)
