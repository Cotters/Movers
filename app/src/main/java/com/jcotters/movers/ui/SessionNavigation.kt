package com.jcotters.movers.ui

import com.jcotters.auth.domain.UserSession

/** Decide when session changes require replacing the stack; public restored routes stay put. */
internal fun shouldResetToHome(
    previousSession: UserSession,
    session: UserSession,
    onSplash: Boolean,
    inAuthGraph: Boolean,
    hasProfileInBackStack: Boolean,
): Boolean {
    if (session == UserSession.Loading) return false
    if (onSplash) return true
    if (session is UserSession.Authenticated) {
        return inAuthGraph ||
            (previousSession is UserSession.Authenticated && previousSession.userId != session.userId)
    }
    return previousSession is UserSession.Authenticated || hasProfileInBackStack
}
