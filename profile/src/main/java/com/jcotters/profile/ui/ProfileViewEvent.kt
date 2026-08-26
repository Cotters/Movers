package com.jcotters.profile.ui

sealed interface ProfileViewEvent {
  data object LogoutTapped : ProfileViewEvent
}