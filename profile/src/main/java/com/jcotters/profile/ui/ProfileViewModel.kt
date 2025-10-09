package com.jcotters.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.profile.domain.FetchUserBookmarksUseCase
import com.jcotters.profile.domain.FetchUserProfileUseCase
import com.jcotters.profile.domain.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
  private val fetchUserProfileUseCase: FetchUserProfileUseCase,
  private val fetchUserBookmarksUseCase: FetchUserBookmarksUseCase,
  private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

  private val viewModelUiState = MutableStateFlow(ProfileViewState())
  val uiState: StateFlow<ProfileViewState> = viewModelUiState

  private val _errorMessage = MutableSharedFlow<String>()
  val errorMessage = _errorMessage.asSharedFlow()

  fun onViewEvent(event: ProfileViewEvent) {
    when (event) {
      is ProfileViewEvent.UserSessionFound -> loadProfile(event.userId)
      ProfileViewEvent.LogoutTapped -> logout()
    }
  }

  private fun loadProfile(userId: Int) {
    viewModelScope.launch {
      fetchUserProfileUseCase.invoke(userId)
        .onSuccess {
          viewModelUiState.update { current ->
            current.copy(
              isLoading = false,
              profile = it,
            )
          }
        }
      fetchUserBookmarksUseCase.invoke()
        .onSuccess { movies ->
          viewModelUiState.update { current ->
            current.copy(
              isLoading = false,
              bookmarkedMovies = movies,
            )
          }
        }
        .onFailure {
          viewModelUiState.update { current ->
            current.copy(isLoading = false)
          }
          _errorMessage.emit(it.message ?: "Failed loading bookmarks.")
        }
    }
  }

  private fun logout() {
    viewModelScope.launch {
      logoutUseCase.invoke()
    }
  }
}