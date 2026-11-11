package com.jcotters.contract.detail.domain

data class MovieTrailer(val title: String, val watchUrl: String, val embedUrl: String)

interface IMovieTrailersRepository {
    suspend fun trailerForMovie(movieId: Int): Result<MovieTrailer?>
}
