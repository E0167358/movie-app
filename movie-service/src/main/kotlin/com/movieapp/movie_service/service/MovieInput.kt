package com.movieapp.movie_service.service

data class MovieInput(
    val title: String,
    val description: String? = null,
    val releaseYear: Int? = null,
    val genre: String? = null,
    val durationMinutes: Int? = null
)

class Artwork(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray
)
