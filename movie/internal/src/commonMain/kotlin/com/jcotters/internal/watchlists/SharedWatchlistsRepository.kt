package com.jcotters.internal.watchlists

import com.cotters.data.network.NetworkMonitor
import com.cotters.data.network.NetworkStatus
import com.jcotters.contract.result.recoverableResult
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
import com.jcotters.database.watchlists.DbSharedOperation
import com.jcotters.profile.domain.DeviceProfileRepository
import com.jcotters.profile.domain.ParticipantIdentity
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

internal class SharedWatchlistsRepository(
    private val database: MoversDatabase,
    private val remote: SharedWatchlistRemote,
    private val identity: ParticipantIdentity,
    private val profiles: DeviceProfileRepository,
    private val network: NetworkMonitor,
) : ISharedWatchlistsRepository {
    private val cache = database.sharedCacheDao()
    private val lists = database.watchlistDao()
    private val outbox = database.sharedOperationDao()
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val persisted = SharedListCache(database, json)
    private val sync = kotlinx.coroutines.flow.MutableStateFlow(SharedSyncStatus())
    override val syncStatus = sync.asStateFlow()
    override val configured get() = remote.configured
    override fun observeActivity(listId: String) =
        database.sharedActivityDao().observeActivity(listId)
            .map { rows -> rows.map { json.decodeFromString<FilmActivity>(it.payload) } }

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

    private suspend fun refreshLists(listId: String?): Result<Unit> = recoverableResult {
        if (!configured) return@recoverableResult
        mutex.withLock {
            online()
            val id = identity.currentId() ?: run {
                check(
                    cache.lists().isEmpty() && outbox.operations().isEmpty()
                ) { "This device's sharing identity is unavailable. Retry without clearing app data." }
                return@withLock
            }
            persisted.checkOwner(id)
            reconcile(id)
            val ids = remote.listIds().toSet()
            cache.lists().filter { it.listId !in ids }.forEach { persisted.purge(it.listId) }
            (if (listId == null) ids else setOf(listId).intersect(ids)).forEach { fetch(it, id) }
        }
    }

    override suspend fun publish(listId: String): Result<Unit> = recoverableResult {
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

    override suspend fun preview(invitation: String): Result<InvitePreview> = recoverableResult {
        mutex.withLock { participant(); remote.preview(WatchlistInvitation.token(invitation)) }
    }

    override suspend fun join(invitation: String): Result<String> = recoverableResult {
        mutex.withLock {
            val owner = participant()
            reconcile(owner)
            val token = WatchlistInvitation.token(invitation)
            val preview = remote.preview(token)
            command(owner, SharedCommand.Join(preview.listId, token))
            preview.listId
        }
    }

    override suspend fun invite(listId: String): Result<String> = recoverableResult {
        mutex.withLock {
            val owner = participant(); reconcile(owner); WatchlistInvitation.url(remote.createInvite(listId))
        }
    }

    override suspend fun revokeInvites(listId: String) = mutate(SharedCommand.RevokeInvites(listId))
    override suspend fun leave(listId: String) = mutate(SharedCommand.Leave(listId))
    override suspend fun loadActivity(listId: String, older: Boolean): Result<Boolean> = recoverableResult {
        mutex.withLock {
            val owner = participant()
            val cursor = database.sharedActivityDao().cursor(listId)
            if (older && cursor != null && cursor.nextBefore == null) return@withLock false
            val page = remote.activity(listId, if (older) cursor?.nextBefore else null)
            persisted.checkOwner(owner)
            persisted.saveActivity(listId, page)
            page.nextBefore != null
        }
    }

    suspend fun mutate(value: SharedCommand): Result<Unit> = recoverableResult {
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
                    outbox.operations()
                        .none { it.listId == listId }) { "This list is being shared. Reconnect and retry." }
                return@withLock local()
            }
            val owner = participant()
            // Reconcile other commands, but preserve this batch's original receipt/outcome.
            outbox.operations().filter { it.id != requestId }.forEach { execute(it) }
            val command = SharedCommand.ImportFilms(listId, films)
            val payload = json.encodeToString<SharedCommand>(command)
            val operation = outbox.operations().firstOrNull { it.id == requestId }?.also {
                check(it.participantId == owner && it.payload == payload) { "Import request changed. Reopen the import." }
            } ?: DbSharedOperation(requestId, listId, owner, payload).also { outbox.insert(it) }
            checkNotNull(execute(operation))
        }

    /** The same mutex prevents a local write racing publication of its original contents. */
    suspend fun write(listIds: Set<String>, local: suspend () -> Unit, shared: suspend (String) -> SharedCommand) {
        mutex.withLock {
            val sharedIds = listIds.filter { cache.get(it) != null }
            if (sharedIds.isEmpty()) {
                check(
                    outbox.operations()
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
            current != null || (cache.lists().isEmpty() && outbox.operations().isEmpty())
        ) { "This device's sharing identity is unavailable. Retry without clearing app data." }
        val profile = profiles.profile.first() ?: error("Set your name first.")
        check(profile.nameConfirmed) { "Confirm your name before sharing." }
        val id = identity.ensure(profile.displayName, profile.color)
        persisted.checkOwner(id)
        return id
    }

    private suspend fun online() {
        if (network.networkStatus.first() == NetworkStatus.Offline) throw SharedConnectionException()
    }

    private suspend fun command(owner: String, command: SharedCommand) {
        val operation = DbSharedOperation(Uuid.random().toString(), command.listId, owner, json.encodeToString(command))
        outbox.insert(operation)
        execute(operation)
    }

    private suspend fun reconcile(owner: String) {
        outbox.operations().forEach { operation ->
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
            if (command is SharedCommand.Leave) persisted.purge(command.listId) else fetch(
                command.listId,
                operation.participantId
            )
            outbox.complete(operation.id)
            return importResult
        } catch (e: SharedAccessDeniedException) {
            if (cache.get(command.listId) != null) persisted.purge(command.listId)
            outbox.complete(operation.id)
            throw e
        } catch (e: IllegalArgumentException) {
            outbox.complete(operation.id)
            throw e
        } catch (e: ImportUnavailableException) {
            outbox.complete(operation.id)
            throw e
        }
        // A transport failure retains the request ID. Reconciliation repeats that exact command.
    }

    private suspend fun fetch(id: String, owner: String) {
        try {
            val snapshot = remote.snapshot(id)
            persisted.saveSnapshot(id, snapshot, owner)
        } catch (e: SharedAccessDeniedException) {
            persisted.purge(id); throw e
        }
    }

}
