package com.jcotters.movers.domain

import com.jcotters.auth.domain.IUserRepository
import com.jcotters.contract.detail.domain.IBookmarksRepository
import javax.inject.Inject

class IsMovieBookmarkedUseCase @Inject constructor(
    private val userRepository: IUserRepository,
    private val bookmarkRepository: IBookmarksRepository,
) {

    suspend fun invoke(movieId: Int): Result<Boolean> {
        val userId = userRepository.getUserIdOrNull() ?: return Result.success(false)
        return bookmarkRepository.isMovieBookmarked(userId = userId, movieId = movieId)
    }

}