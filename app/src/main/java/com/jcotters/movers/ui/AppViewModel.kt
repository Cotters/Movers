package com.jcotters.movers.ui

import androidx.lifecycle.ViewModel
import com.jcotters.auth.domain.GetSessionUseCase
import com.jcotters.auth.domain.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
  getSessionUseCase: GetSessionUseCase,
) : ViewModel() {

  val userSession: Flow<UserSession> = getSessionUseCase.session

}