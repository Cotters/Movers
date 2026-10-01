package com.jcotters.movers.ui

import com.jcotters.auth.domain.UserSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionNavigationShould {
    private fun reset(
        session: UserSession,
        previous: UserSession = UserSession.Loading,
        splash: Boolean = false,
        auth: Boolean = false,
        profileInStack: Boolean = false,
    ) = shouldResetToHome(previous, session, splash, auth, profileInStack)

    @Test
    fun `wait until initialization finishes`() {
        assertFalse(reset(UserSession.Loading, splash = true))
    }

    @Test
    fun `leave splash for both guests and authenticated users`() {
        assertTrue(reset(UserSession.NotAuthenticated, splash = true))
        assertTrue(reset(UserSession.Authenticated(1), splash = true))
    }

    @Test
    fun `preserve restored public screens for both guests and authenticated users`() {
        assertFalse(reset(UserSession.NotAuthenticated))
        assertFalse(reset(UserSession.Authenticated(1)))
    }

    @Test
    fun `preserve a restored profile if its session is valid`() {
        assertFalse(reset(UserSession.Authenticated(1), profileInStack = true))
    }

    @Test
    fun `purge expired restored profile even if details is currently on top`() {
        assertTrue(reset(UserSession.NotAuthenticated, profileInStack = true))
    }

    @Test
    fun `guests can open and restore login without being sent home`() {
        assertFalse(reset(UserSession.NotAuthenticated, auth = true))
        assertFalse(reset(UserSession.NotAuthenticated, previous = UserSession.NotAuthenticated, auth = true))
    }

    @Test
    fun `successful authentication removes the auth graph`() {
        assertTrue(reset(UserSession.Authenticated(1), previous = UserSession.NotAuthenticated, auth = true))
    }

    @Test
    fun `logout or expiry purges history from any screen`() {
        assertTrue(reset(UserSession.NotAuthenticated, previous = UserSession.Authenticated(1)))
    }

    @Test
    fun `ordinary navigation within the same session does not reset history`() {
        assertFalse(reset(UserSession.Authenticated(1), previous = UserSession.Authenticated(1)))
        assertFalse(reset(UserSession.NotAuthenticated, previous = UserSession.NotAuthenticated))
    }

    @Test
    fun `changing accounts discards screens owned by the old account`() {
        assertTrue(reset(UserSession.Authenticated(2), previous = UserSession.Authenticated(1)))
    }
}
