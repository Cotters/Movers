package com.jcotters.profile.domain

import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.UserSession
import com.jcotters.contract.detail.domain.IBookmarksRepository
import com.jcotters.contract.detail.domain.models.Movie
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart

class ObserveUserBookmarksUseCase @Inject constructor(
    private val userRepository: IUserRepository,
    private val bookmarksRepository: IBookmarksRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Movie>> = userRepository.userSession.flatMapLatest { session ->
        when (session) {
            is UserSession.Authenticated -> bookmarksRepository.observeUserBookmarks(session.userId)
                .onStart { emit(emptyList()) }
            else -> flowOf(emptyList())
        }
    }
}
