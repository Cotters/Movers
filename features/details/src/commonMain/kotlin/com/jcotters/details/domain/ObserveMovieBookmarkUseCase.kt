package com.jcotters.details.domain

import com.jcotters.contract.detail.domain.IBookmarksRepository

class ObserveMovieBookmarkUseCase(private val repository: IBookmarksRepository) {
    operator fun invoke(movieId: Int) = repository.observeMovieBookmark(movieId)
}
