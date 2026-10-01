package com.jcotters.auth.data

import com.jcotters.auth.domain.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest
import java.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/** A new session or foreground recheck cancels the previous session's expiry timer. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun Flow<SessionState>.observeUserSession(clock: Clock): Flow<UserSession> =
    transformLatest { session ->
        val userId = session.userId
        var remainingMillis = session.expiresAt - clock.millis()
        if (userId == null || remainingMillis <= 0) {
            emit(UserSession.NotAuthenticated)
            return@transformLatest
        }
        emit(UserSession.Authenticated(userId))

        while (remainingMillis > 0) {
            delay(remainingMillis.milliseconds)
            remainingMillis = session.expiresAt - clock.millis()
        }
        emit(UserSession.NotAuthenticated)
    }.distinctUntilChanged()
