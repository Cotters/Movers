package com.jcotters.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.auth.domain.IUserRepository
import com.jcotters.profile.domain.FetchUserProfileUseCase
import com.jcotters.profile.domain.ObserveUserBookmarksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val fetchUserProfileUseCase: FetchUserProfileUseCase,
    private val observeUserBookmarks: ObserveUserBookmarksUseCase,
    private val userRepository: IUserRepository,
) : ViewModel() {

    private val viewModelUiState = MutableStateFlow(ProfileViewState())
    val uiState: StateFlow<ProfileViewState> = viewModelUiState

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    init {
        loadProfile()
    }

    fun onViewEvent(event: ProfileViewEvent) {
        when (event) {
            ProfileViewEvent.LogoutTapped -> logout()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            fetchUserProfileUseCase.invoke()
                .onSuccess { profile ->
                    viewModelUiState.update { it.copy(isLoading = false, profile = profile) }
                }
                .onFailure { error ->
                    viewModelUiState.update { it.copy(isLoading = false) }
                    _errorMessage.emit(error.message ?: "Failed loading profile.")
                }
        }
        viewModelScope.launch {
            observeUserBookmarks()
                .catch { error ->
                    viewModelUiState.update { it.copy(isLoading = false) }
                    _errorMessage.emit(error.message ?: "Failed loading bookmarks.")
                }
                .collect { movies ->
                    viewModelUiState.update { it.copy(bookmarkedMovies = movies) }
                }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }
}
