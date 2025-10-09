package com.jcotters.profile.ui

sealed interface ProfileViewEvent {
  data class UserSessionFound(val userId: Int) : ProfileViewEvent
  data object LogoutTapped : ProfileViewEvent
}