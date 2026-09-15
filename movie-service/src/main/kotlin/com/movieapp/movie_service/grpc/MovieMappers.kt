package com.movieapp.movie_service.grpc

import com.movieapp.grpc.movie.Movie
import com.movieapp.grpc.movie.MovieListResponse
import com.movieapp.grpc.movie.SaveMovieRequest
import com.movieapp.movie_service.entity.MovieEntity
import com.movieapp.movie_service.service.MovieInput
import org.springframework.data.domain.Page

// protobuf does not allow null, so null becomes 0 or empty string

fun MovieEntity.toProto(): Movie {
    return Movie.newBuilder()
        .setId(id ?: 0L)
        .setTitle(title)
        .setDescription(description ?: "")
        .setReleaseYear(releaseYear ?: 0)
        .setGenre(genre ?: "")
        .setDurationMinutes(durationMinutes ?: 0)
        .setHasArtwork(artworkPath != null)
        .build()
}

fun Page<MovieEntity>.toProto(): MovieListResponse {
    return MovieListResponse.newBuilder()
        .addAllMovies(content.map { it.toProto() })
        .setTotalCount(totalElements)
        .build()
}

// and the other way: 0 or empty string from the request means "not provided"
fun SaveMovieRequest.toInput(): MovieInput {
    return MovieInput(
        title = title,
        description = description.ifBlank { null },
        releaseYear = if (releaseYear == 0) null else releaseYear,
        genre = genre.ifBlank { null },
        durationMinutes = if (durationMinutes == 0) null else durationMinutes
    )
}
