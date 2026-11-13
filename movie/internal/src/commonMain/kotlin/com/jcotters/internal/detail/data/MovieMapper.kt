package com.jcotters.internal.detail.data

import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.detail.domain.models.MovieGenre
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.DbMovieCatalogueEntry
import com.jcotters.database.movies.DbMovieGenre
import com.jcotters.internal.catalogue.data.models.CatalogueMovieDto
import com.jcotters.internal.detail.data.models.MovieDto

internal class MovieMapper {
    fun toDatabaseModel(dto: MovieDto, fetchedAt: Long): DbMovie? {
        val id = dto.id ?: return null
        val title = dto.title?.takeIf { it.isNotBlank() } ?: return null
        return DbMovie(
            id = id,
            title = title,
            synopsis = dto.overview.orEmpty(),
            releaseDate = dto.releaseDate.orEmpty(),
            posterUrl = imageUrl(dto.posterPath),
            backdropUrl = imageUrl(dto.backdropPath),
            tagline = dto.tagline?.takeIf { it.isNotBlank() },
            runtimeMinutes = dto.runtime?.takeIf { it > 0 },
            genres = dto.genres.orEmpty().mapNotNull { genre ->
                val id = genre?.id ?: return@mapNotNull null
                val name = genre.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                DbMovieGenre(id, name)
            }.distinctBy { it.id },
            voteAverage = validRating(dto.voteAverage),
            voteCount = dto.voteCount?.takeIf { it >= 0 },
            status = dto.status?.takeIf { it.isNotBlank() },
            originalLanguage = dto.originalLanguage?.takeIf { it.isNotBlank() },
            detailsFetchedAt = fetchedAt,
        )
    }

    fun toDomainModel(catalogueResults: List<CatalogueMovieDto?>): List<Movie> =
        toDatabaseModel(catalogueResults).map(::toDomainModel)

    fun toDatabaseModel(movies: List<CatalogueMovieDto?>): List<DbMovie> = movies.mapNotNull { dto ->
        DbMovie(
            id = dto?.id ?: return@mapNotNull null,
            title = dto.title ?: return@mapNotNull null,
            synopsis = dto.overview ?: return@mapNotNull null,
            releaseDate = dto.releaseDate ?: "Unknown release date",
            posterUrl = imageUrl(dto.posterPath),
            backdropUrl = imageUrl(dto.backdropPath),
            originalLanguage = dto.originalLanguage?.takeIf { it.isNotBlank() },
            voteAverage = validRating(dto.voteAverage),
            voteCount = dto.voteCount?.takeIf { it >= 0 },
        )
    }

    fun toDomainModel(dbMovie: DbMovie): Movie = Movie(
        id = dbMovie.id,
        title = dbMovie.title,
        synopsis = dbMovie.synopsis,
        releaseDate = dbMovie.releaseDate,
        posterUrl = dbMovie.posterUrl,
        backdropUrl = dbMovie.backdropUrl,
        tagline = dbMovie.tagline,
        runtimeMinutes = dbMovie.runtimeMinutes,
        genres = dbMovie.genres.orEmpty().map { MovieGenre(it.id, it.name) },
        voteAverage = dbMovie.voteAverage,
        voteCount = dbMovie.voteCount,
        status = dbMovie.status,
        originalLanguage = dbMovie.originalLanguage,
        detailsFetchedAt = dbMovie.detailsFetchedAt,
        fullSizePosterUrl = fullSizeImageUrl(dbMovie.posterUrl),
        fullSizeBackdropUrl = fullSizeImageUrl(dbMovie.backdropUrl),
    )

    fun toCatalogueEntry(catalogue: String, movies: List<DbMovie>, page: Int): List<DbMovieCatalogueEntry> =
        movies.mapIndexed { index, movie -> DbMovieCatalogueEntry(catalogue, movie.id, page, index) }

    private fun imageUrl(path: String?): String? = path?.takeIf { it.isNotBlank() }
        ?.let { "https://image.tmdb.org/t/p/w500/${it.trimStart('/')}" }

    private fun fullSizeImageUrl(url: String?): String? {
        val image = url?.takeIf { it.isNotBlank() } ?: return null
        val thumbnailPrefix = "https://image.tmdb.org/t/p/w500/"
        return if (image.startsWith(thumbnailPrefix)) {
            "https://image.tmdb.org/t/p/original/${image.removePrefix(thumbnailPrefix)}"
        } else image
    }

    private fun validRating(rating: Double?): Double? = rating?.takeIf { it.isFinite() && it in 0.0..10.0 }
}
