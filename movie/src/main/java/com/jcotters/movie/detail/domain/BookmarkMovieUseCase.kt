package com.jcotters.movie.detail.domain

import com.jcotters.auth.domain.IUserRepository
import javax.inject.Inject

class BookmarkMovieUseCase @Inject constructor(
  private val userRepository: IUserRepository,
  private val bookmarksRepository: IBookmarksRepository
) {

  companion object {
    private const val LOGIN_REQUIRED_MESSAGE = "You must be logged in."
  }

  suspend fun invoke(movieId: Int): Result<Unit> {
    val userId = userRepository.getUserIdOrNull()
    return if (userId != null) {
      bookmarksRepository.bookmarkMovie(movieId = movieId, userId = userId)
    } else {
      Result.failure(Throwable(LOGIN_REQUIRED_MESSAGE))
    }
  }

}
