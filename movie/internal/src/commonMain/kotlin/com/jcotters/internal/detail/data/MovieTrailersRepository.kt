package com.jcotters.internal.detail.data

import com.jcotters.contract.detail.domain.IMovieTrailersRepository
import com.jcotters.contract.detail.domain.MovieTrailer
import com.jcotters.internal.MovieApi
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
data class MovieVideosResponse(val id: Int? = null, val results: List<MovieVideoDto?>? = null)

@Serializable
data class MovieVideoDto(
    val name: String? = null,
    val key: String? = null,
    val site: String? = null,
    val type: String? = null,
    val official: Boolean = false,
)

internal class MovieTrailersRepository(private val api: MovieApi) : IMovieTrailersRepository {
    override suspend fun trailerForMovie(movieId: Int): Result<MovieTrailer?> = try {
        val response = api.getMovieVideos(movieId)
        require(response.id == movieId) { "Trailer response did not match the requested movie." }
        val candidates = response.results.orEmpty().filterNotNull().filter {
            it.site == "YouTube" && it.type == "Trailer" && it.key?.matches(videoKey) == true
        }
        val video = candidates.firstOrNull { it.official } ?: candidates.firstOrNull()
        Result.success(video?.let {
            MovieTrailer(
                title = it.name?.trim()?.takeIf(String::isNotEmpty) ?: "Trailer",
                watchUrl = "https://www.youtube.com/watch?v=${it.key}",
                embedUrl = "https://www.youtube.com/embed/${it.key}?playsinline=1&autoplay=0&fs=0",
            )
        })
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(IllegalStateException("Unable to find a trailer.", error))
    }

    private companion object {
        val videoKey = Regex("[A-Za-z0-9_-]{11}")
    }
}
