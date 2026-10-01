package com.jcotters.details.domain

import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.UserSession
import com.jcotters.contract.detail.domain.IBookmarksRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ObserveMovieBookmarkUseCase @Inject constructor(
    private val userRepository: IUserRepository,
    private val bookmarksRepository: IBookmarksRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(movieId: Int): Flow<Boolean> = userRepository.userSession.flatMapLatest { session ->
        when (session) {
            is UserSession.Authenticated -> bookmarksRepository.observeMovieBookmark(session.userId, movieId)
            else -> flowOf(false)
        }
    }
}
