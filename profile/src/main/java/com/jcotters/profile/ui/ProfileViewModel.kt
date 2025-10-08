package com.jcotters.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.profile.domain.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Profile(
  val username: String,
)

data class ProfileViewState(
  val isLoading: Boolean = true,
  val profile: Profile? = null,
)

sealed interface ProfileViewEvent {
  data class UserSessionFound(val userId: Int) : ProfileViewEvent
  data object LogoutTapped : ProfileViewEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
  private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

  private val viewModelUiState = MutableStateFlow(ProfileViewState())
  val uiState: StateFlow<ProfileViewState> = viewModelUiState

  fun onViewEvent(event: ProfileViewEvent) {
    when (event) {
      is ProfileViewEvent.UserSessionFound -> loadProfile(event.userId)
      ProfileViewEvent.LogoutTapped -> logout()
    }
  }

  private fun loadProfile(userId: Int) {
    viewModelUiState.update { current ->
      current.copy(isLoading = false)
    }
  }

  private fun logout() {
    viewModelScope.launch {
      logoutUseCase.invoke()
    }
  }
}