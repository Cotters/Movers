package com.jcotters.auth.domain

sealed interface UserSession {
    data object Loading : UserSession
    data class Authenticated(val userId: Int) : UserSession
    data object NotAuthenticated : UserSession
}
