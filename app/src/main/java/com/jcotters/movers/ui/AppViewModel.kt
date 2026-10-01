package com.jcotters.movers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val userRepository: IUserRepository,
) : ViewModel() {

    val userSession: StateFlow<UserSession> = userRepository.userSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSession.Loading,
    )

    fun onResume() {
        userRepository.refreshSession()
    }
}
