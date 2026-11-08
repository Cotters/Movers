package com.jcotters.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.detail.domain.IBookmarksRepository
import com.jcotters.profile.domain.DeviceProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProfileViewModel(
    private val profiles: DeviceProfileRepository,
    private val bookmarks: IBookmarksRepository,
) : ViewModel() {
    private val state = MutableStateFlow(ProfileViewState())
    val uiState = state.asStateFlow()
    private var observation: Job? = null

    init { retry() }

    fun retry() {
        if (observation?.isActive == true) return
        state.update { it.copy(errorMessage = null) }
        observation = viewModelScope.launch {
            combine(profiles.profile, bookmarks.observeBookmarks()) { profile, movies ->
                ProfileViewState(isLoading = false, profile = profile, bookmarkedMovies = movies)
            }.catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                state.update { it.copy(isLoading = false, errorMessage = "Unable to read saved profile data.") }
            }.collect { state.value = it }
        }
    }
}
