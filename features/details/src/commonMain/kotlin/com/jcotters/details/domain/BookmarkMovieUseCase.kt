package com.jcotters.details.domain

import com.jcotters.contract.detail.domain.IBookmarksRepository

class BookmarkMovieUseCase(private val repository: IBookmarksRepository) {
    suspend fun addBookmark(movieId: Int): Result<Unit> = repository.addBookmark(movieId)
    suspend fun removeBookmark(movieId: Int): Result<Unit> = repository.removeBookmark(movieId)
}
