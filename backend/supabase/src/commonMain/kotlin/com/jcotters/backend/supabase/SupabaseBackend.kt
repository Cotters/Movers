package com.jcotters.backend.supabase

import com.jcotters.contract.result.recoverableResult
import com.jcotters.contract.watchlists.data.ActivityPage
import com.jcotters.contract.watchlists.data.ImportBatchResult
import com.jcotters.contract.watchlists.data.ImportUnavailableException
import com.jcotters.contract.watchlists.data.InvitePreview
import com.jcotters.contract.watchlists.data.PushRegistration
import com.jcotters.contract.watchlists.data.PushRegistrations
import com.jcotters.contract.watchlists.data.SharedAccessDeniedException
import com.jcotters.contract.watchlists.data.SharedCommand
import com.jcotters.contract.watchlists.data.SharedConnectionException
import com.jcotters.contract.watchlists.data.SharedSnapshot
import com.jcotters.contract.watchlists.data.SharedWatchlistRemote
import com.jcotters.contract.watchlists.data.SharingUnavailableException
import com.jcotters.profile.domain.ParticipantIdentity
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal interface SecureCredentialStore {
    suspend fun read(): String?
    suspend fun write(value: String)
    suspend fun clear()
}

internal class SecureSessions(private val store: SecureCredentialStore) : SessionManager {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    var failure: Throwable? = null
        private set

    override suspend fun saveSession(session: UserSession) = protect { store.write(json.encodeToString(session)) }
    override suspend fun loadSession(): UserSession = loadSessionOrNull() ?: throw NoSessionFoundException()
    override suspend fun loadSessionOrNull(): UserSession? =
        protect { store.read()?.let { json.decodeFromString<UserSession>(it) } }

    override suspend fun deleteSession() = protect { store.clear() }
    private suspend fun <T> protect(block: suspend () -> T): T = recoverableResult {
        mutex.withLock { block().also { failure = null } }
    }.getOrElse { error ->
        failure = error
        throw IllegalStateException("Unable to read or save this device's sharing identity.", error)
    }
}

/** Provider implementation. Applications bind its neutral interfaces at composition. */
class SupabaseBackend internal constructor(url: String, key: String, store: SecureCredentialStore) :
    SharedWatchlistRemote, ParticipantIdentity, PushRegistrations {
    override val configured = url.startsWith("https://") && key.isNotBlank() && !key.startsWith("$(")
    private val sessions = SecureSessions(store)
    private val identityMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = if (configured) createSupabaseClient(url, key) {
        install(Auth) { sessionManager = sessions }
        install(Postgrest)
        install(Realtime)
    } else null

    private fun backend() = client ?: throw SharingUnavailableException()
    override suspend fun currentId(): String? {
        val client = backend()
        recoverableResult { client.auth.awaitInitialization() }.getOrElse { error ->
            throw IllegalStateException("Unable to restore your sharing identity. Reopen the app to retry.", error)
        }
        check(sessions.failure == null) {
            "Unable to restore your sharing identity. Reopen the app to retry; do not clear app data."
        }
        val saved = sessions.loadSessionOrNull()
        if (saved != null && client.auth.currentUserOrNull() == null) {
            recoverableResult { client.auth.importSession(saved) }.getOrElse { error ->
                throw IllegalStateException("Unable to restore your sharing identity. Reconnect and retry.", error)
            }
        }
        val id = client.auth.currentUserOrNull()?.id
        check(saved == null || id != null) { "Unable to restore your sharing identity. Reconnect and retry." }
        return id
    }

    override suspend fun ensure(name: String, color: Int): String = identityMutex.withLock {
        val client = backend()
        val id = currentId() ?: run {
            recoverableResult { client.auth.signInAnonymously() }.getOrElse { error ->
                throw IllegalStateException(
                    "Unable to create this device's sharing identity. Check your connection and retry later.", error
                )
            }
            currentId() ?: error("Unable to create this device's sharing identity.")
        }
        update(name, color)
        id
    }

    override suspend fun update(name: String, color: Int) {
        rpc("movers_profile", buildJsonObject { put("p_name", name); put("p_color", color) })
    }

    override suspend fun registerPush(registration: PushRegistration) {
        check(currentId() != null) { "Sharing identity required." }
        rpc("movers_push_register", buildJsonObject {
            put("p_device", registration.deviceId)
            put("p_environment", registration.environment.wireName)
            put("p_token", registration.token?.let(::JsonPrimitive) ?: JsonNull)
            put("p_enabled", registration.enabled)
        })
    }

    override suspend fun snapshot(listId: String): SharedSnapshot =
        json.decodeFromJsonElement(rpc("movers_snapshot", params(listId)))

    override suspend fun listIds(): List<String> =
        json.decodeFromJsonElement(rpc("movers_list_ids", buildJsonObject {}))

    override suspend fun execute(requestId: String, command: SharedCommand) {
        rpc(
            "movers_command",
            buildJsonObject { put("p_request", requestId); put("p_command", json.encodeToJsonElement(command)) })
    }

    override suspend fun executeImport(requestId: String, command: SharedCommand.ImportFilms): ImportBatchResult =
        json.decodeFromJsonElement(rpc("movers_import", buildJsonObject {
            put("p_request", requestId); put("p_command", json.encodeToJsonElement<SharedCommand>(command))
        }))

    override suspend fun preview(token: String): InvitePreview =
        json.decodeFromJsonElement(rpc("movers_preview", buildJsonObject { put("p_token", token) }))

    override suspend fun createInvite(listId: String): String =
        rpc("movers_invite", params(listId)).jsonPrimitive.content

    override suspend fun activity(listId: String, before: Long?): ActivityPage =
        json.decodeFromJsonElement(rpc("movers_activity", buildJsonObject {
            put("p_list", listId); put("p_before", before?.let(::JsonPrimitive) ?: JsonNull)
        }))

    override fun invalidations(): Flow<Unit> = channelFlow {
        val channel = backend().channel("movers-lists")
        try {
            coroutineScope {
                val collection = launch {
                    channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "movers_list_signals"
                    }.collect { send(Unit) }
                }
                channel.subscribe(blockUntilSubscribed = true)
                send(Unit)
                collection.join()
            }
        } finally {
            withContext(NonCancellable) { backend().realtime.removeChannel(channel) }
        }
    }

    private fun params(id: String) = buildJsonObject { put("p_list", id) }
    private suspend fun rpc(name: String, params: JsonObject): JsonElement = recoverableResult {
        val data = backend().postgrest.rpc(name, params).data
        if (data.isBlank()) JsonNull else json.parseToJsonElement(data)
    }.getOrElse { throw rpcFailure(name, it) }

    private fun rpcFailure(name: String, error: Throwable): Throwable = when (error) {
        is PostgrestRestException -> when (error.code) {
            "PGRST202" -> if (name == "movers_import") ImportUnavailableException()
            else IllegalStateException("This backend needs updating.", error)

            "42501" -> SharedAccessDeniedException()
            "22023", "23514", "23502", "23505", "22P02", "22001" -> IllegalArgumentException(
                "This change or invitation is invalid. Check the name or request a new invitation.", error
            )

            else -> IllegalStateException("Unable to save this shared change. Retry when connected.", error)
        }

        is SharingUnavailableException -> error
        else -> SharedConnectionException()
    }
}
