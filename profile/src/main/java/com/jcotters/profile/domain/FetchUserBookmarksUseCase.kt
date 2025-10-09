package com.jcotters.profile.domain

import com.jcotters.auth.domain.IUserRepository
import com.jcotters.movie.detail.domain.IBookmarksRepository
import com.jcotters.movie.detail.domain.models.Movie
import javax.inject.Inject

class FetchUserBookmarksUseCase @Inject constructor(
  private val userRepository: IUserRepository,
  private val bookmarksRepository: IBookmarksRepository
) {
  private companion object {
    const val LOGIN_REQUIRED_MESSAGE = "Login required"
  }

  suspend fun invoke(): Result<List<Movie>> {
    val userId = userRepository.getUserIdOrNull()
    return if (userId != null) {
      bookmarksRepository.getUserBookmarks(userId = userId)
    } else {
      Result.failure(Throwable(LOGIN_REQUIRED_MESSAGE))
    }
  }
}