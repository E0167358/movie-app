package com.movieapp.movie_service.service

import com.movieapp.movie_service.entity.MovieEntity
import com.movieapp.movie_service.exception.NotFoundException
import com.movieapp.movie_service.exception.ValidationException
import com.movieapp.movie_service.repository.MovieRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.Year

@Service
class MovieService(
    private val movieRepository: MovieRepository,
    private val artworkStorage: ArtworkStorage
) {

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 100
        const val MAX_ARTWORK_BYTES = 5 * 1024 * 1024
        const val FIRST_MOVIE_YEAR = 1888
        val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }

    fun getMovie(id: Long): MovieEntity {
        return movieRepository.findById(id)
            .orElseThrow { NotFoundException("Movie with id $id not found") }
    }

    fun listMovies(page: Int, size: Int): Page<MovieEntity> {
        return movieRepository.findAll(pageRequest(page, size))
    }

    fun searchMovies(query: String, page: Int, size: Int): Page<MovieEntity> {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            return listMovies(page, size)
        }
        return movieRepository.search(keyword, pageRequest(page, size))
    }

    @Transactional
    fun createMovie(input: MovieInput): MovieEntity {
        validate(input)
        val movie = MovieEntity(
            title = input.title.trim(),
            description = input.description?.trim(),
            releaseYear = input.releaseYear,
            genre = input.genre?.trim(),
            durationMinutes = input.durationMinutes
        )
        return movieRepository.save(movie)
    }

    @Transactional
    fun updateMovie(id: Long, input: MovieInput): MovieEntity {
        validate(input)
        val movie = getMovie(id)
        movie.title = input.title.trim()
        movie.description = input.description?.trim()
        movie.releaseYear = input.releaseYear
        movie.genre = input.genre?.trim()
        movie.durationMinutes = input.durationMinutes
        movie.updatedAt = LocalDateTime.now()
        return movieRepository.save(movie)
    }

    @Transactional
    fun deleteMovie(id: Long) {
        val movie = getMovie(id)
        movieRepository.delete(movie)
        movie.artworkPath?.let { artworkStorage.delete(it) }
    }

    @Transactional
    fun saveArtwork(movieId: Long, contentType: String, bytes: ByteArray): MovieEntity {
        if (contentType !in ALLOWED_IMAGE_TYPES) {
            throw ValidationException("Only JPEG, PNG and WEBP images are allowed")
        }
        if (bytes.isEmpty()) {
            throw ValidationException("Artwork file is empty")
        }
        if (bytes.size > MAX_ARTWORK_BYTES) {
            throw ValidationException("Artwork must be smaller than 5 MB")
        }

        val movie = getMovie(movieId)
        val oldFile = movie.artworkPath

        movie.artworkPath = artworkStorage.save(movieId, contentType, bytes)
        movie.artworkContentType = contentType
        movie.updatedAt = LocalDateTime.now()
        val saved = movieRepository.save(movie)

        // delete the old image only after the new one is saved
        if (oldFile != null) {
            artworkStorage.delete(oldFile)
        }
        return saved
    }

    fun getArtwork(movieId: Long): Artwork {
        val movie = getMovie(movieId)
        val fileName = movie.artworkPath
            ?: throw NotFoundException("Movie with id $movieId has no artwork")
        return Artwork(
            fileName = fileName,
            contentType = movie.artworkContentType ?: "application/octet-stream",
            bytes = artworkStorage.load(fileName)
        )
    }

    private fun validate(input: MovieInput) {
        if (input.title.isBlank()) {
            throw ValidationException("Title is required")
        }
        if (input.title.trim().length > 255) {
            throw ValidationException("Title must be 255 characters or less")
        }
        if (input.genre != null && input.genre.trim().length > 100) {
            throw ValidationException("Genre must be 100 characters or less")
        }
        val maxYear = Year.now().value + 5
        if (input.releaseYear != null && (input.releaseYear < FIRST_MOVIE_YEAR || input.releaseYear > maxYear)) {
            throw ValidationException("Release year must be between $FIRST_MOVIE_YEAR and $maxYear")
        }
        if (input.durationMinutes != null && input.durationMinutes <= 0) {
            throw ValidationException("Duration must be a positive number")
        }
    }

    private fun pageRequest(page: Int, size: Int): PageRequest {
        val safePage = if (page < 0) 0 else page
        val safeSize = when {
            size <= 0 -> DEFAULT_PAGE_SIZE
            size > MAX_PAGE_SIZE -> MAX_PAGE_SIZE
            else -> size
        }
        return PageRequest.of(safePage, safeSize, Sort.by("title"))
    }
}
