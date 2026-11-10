package com.jcotters.profile.ui

import com.jcotters.profile.domain.Profile

data class ProfileViewState(
    val isLoading: Boolean = true,
    val profile: Profile? = null,
    val errorMessage: String? = null,
)
