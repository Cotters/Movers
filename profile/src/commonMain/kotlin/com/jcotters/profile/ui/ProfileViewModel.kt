package com.jcotters.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.profile.domain.DeviceProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProfileViewModel(
    private val profiles: DeviceProfileRepository,
) : ViewModel() {
    private val state = MutableStateFlow(ProfileViewState())
    val uiState = state.asStateFlow()
    private var observation: Job? = null

    init { retry() }

    val savingName = MutableStateFlow(false)
    val nameError = MutableStateFlow<String?>(null)
    fun saveName(name: String) {
        if (savingName.value || savingColor.value) return
        savingName.value = true
        viewModelScope.launch {
            try { profiles.saveName(name); nameError.value = null }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { nameError.value = e.message ?: "Unable to save your name." }
            finally { savingName.value = false }
        }
    }

    val savingColor = MutableStateFlow(false)
    val colorError = MutableStateFlow<String?>(null)
    fun saveColor(color: Int) {
        if (savingName.value || savingColor.value) return
        savingColor.value = true
        colorError.value = null
        viewModelScope.launch {
            try { profiles.saveColor(color) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { colorError.value = "Unable to save your colour. Try again." }
            finally { savingColor.value = false }
        }
    }

    fun retry() {
        if (observation?.isActive == true) return
        state.update { it.copy(errorMessage = null) }
        observation = viewModelScope.launch {
            profiles.profile.catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                state.update { it.copy(isLoading = false, errorMessage = "Unable to read saved profile data.") }
            }.collect { profile -> state.value = ProfileViewState(isLoading = false, profile = profile) }
        }
    }
}
