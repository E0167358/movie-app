package com.movieapp.movie_service.service

import com.movieapp.movie_service.entity.MovieEntity
import com.movieapp.movie_service.exception.NotFoundException
import com.movieapp.movie_service.exception.ValidationException
import com.movieapp.movie_service.repository.MovieRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.time.Year
import java.util.Optional
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MovieServiceTest {

    private lateinit var movieRepository: MovieRepository
    private lateinit var artworkStorage: ArtworkStorage
    private lateinit var movieService: MovieService

    @BeforeEach
    fun setUp() {
        movieRepository = mockk(relaxUnitFun = true)
        artworkStorage = mockk(relaxUnitFun = true)
        movieService = MovieService(movieRepository, artworkStorage)

        // save() gives back the same movie that was passed in, like the real repository
        every { movieRepository.save(any<MovieEntity>()) } answers { firstArg() }
    }

    private fun movie(id: Long = 1L, artworkPath: String? = null) = MovieEntity(
        id = id,
        title = "Inception",
        genre = "Sci-Fi",
        releaseYear = 2010,
        durationMinutes = 148,
        artworkPath = artworkPath,
        artworkContentType = if (artworkPath != null) "image/png" else null
    )

    // ----- getMovie -----

    @Test
    fun `getMovie returns the movie when it exists`() {
        every { movieRepository.findById(1L) } returns Optional.of(movie())

        val result = movieService.getMovie(1L)

        assertEquals("Inception", result.title)
    }

    @Test
    fun `getMovie throws NotFoundException when movie does not exist`() {
        every { movieRepository.findById(99L) } returns Optional.empty()

        val error = assertFailsWith<NotFoundException> { movieService.getMovie(99L) }

        assertEquals("Movie with id 99 not found", error.message)
    }

    // ----- listMovies and paging -----

    @Test
    fun `listMovies uses page and size from the request`() {
        val pageable = slot<Pageable>()
        every { movieRepository.findAll(capture(pageable)) } returns PageImpl(listOf(movie()))

        val result = movieService.listMovies(2, 10)

        assertEquals(2, pageable.captured.pageNumber)
        assertEquals(10, pageable.captured.pageSize)
        assertEquals(1, result.content.size)
    }

    @Test
    fun `listMovies sorts by title`() {
        val pageable = slot<Pageable>()
        every { movieRepository.findAll(capture(pageable)) } returns PageImpl(emptyList<MovieEntity>())

        movieService.listMovies(0, 10)

        assertEquals(Sort.by("title"), pageable.captured.sort)
    }

    @Test
    fun `listMovies uses page 0 when page is negative`() {
        val pageable = slot<Pageable>()
        every { movieRepository.findAll(capture(pageable)) } returns PageImpl(emptyList<MovieEntity>())

        movieService.listMovies(-3, 10)

        assertEquals(0, pageable.captured.pageNumber)
    }

    @Test
    fun `listMovies uses default size when size is 0`() {
        // grpc sends 0 when the client did not set the size
        val pageable = slot<Pageable>()
        every { movieRepository.findAll(capture(pageable)) } returns PageImpl(emptyList<MovieEntity>())

        movieService.listMovies(0, 0)

        assertEquals(MovieService.DEFAULT_PAGE_SIZE, pageable.captured.pageSize)
    }

    @Test
    fun `listMovies uses default size when size is negative`() {
        val pageable = slot<Pageable>()
        every { movieRepository.findAll(capture(pageable)) } returns PageImpl(emptyList<MovieEntity>())

        movieService.listMovies(0, -5)

        assertEquals(MovieService.DEFAULT_PAGE_SIZE, pageable.captured.pageSize)
    }

    @Test
    fun `listMovies limits size to the maximum`() {
        val pageable = slot<Pageable>()
        every { movieRepository.findAll(capture(pageable)) } returns PageImpl(emptyList<MovieEntity>())

        movieService.listMovies(0, 1000)

        assertEquals(MovieService.MAX_PAGE_SIZE, pageable.captured.pageSize)
    }

    // ----- searchMovies -----

    @Test
    fun `searchMovies trims the keyword before searching`() {
        val keyword = slot<String>()
        every { movieRepository.search(capture(keyword), any()) } returns PageImpl(listOf(movie()))

        val result = movieService.searchMovies("  incep  ", 0, 10)

        assertEquals("incep", keyword.captured)
        assertEquals(1, result.content.size)
    }

    @Test
    fun `searchMovies returns empty page when nothing matches`() {
        every { movieRepository.search("xyz", any()) } returns PageImpl(emptyList<MovieEntity>())

        val result = movieService.searchMovies("xyz", 0, 10)

        assertEquals(0, result.content.size)
    }

    @Test
    fun `searchMovies with blank query returns the normal list`() {
        every { movieRepository.findAll(any<Pageable>()) } returns PageImpl(listOf(movie()))

        movieService.searchMovies("   ", 0, 10)

        verify(exactly = 1) { movieRepository.findAll(any<Pageable>()) }
        verify(exactly = 0) { movieRepository.search(any(), any()) }
    }

    // ----- createMovie -----

    @Test
    fun `createMovie trims text fields and saves the movie`() {
        val input = MovieInput(
            title = "  Inception  ",
            description = " A dream heist ",
            releaseYear = 2010,
            genre = " Sci-Fi ",
            durationMinutes = 148
        )

        val result = movieService.createMovie(input)

        assertEquals("Inception", result.title)
        assertEquals("A dream heist", result.description)
        assertEquals("Sci-Fi", result.genre)
        assertEquals(2010, result.releaseYear)
        assertEquals(148, result.durationMinutes)
        verify(exactly = 1) { movieRepository.save(any<MovieEntity>()) }
    }

    @Test
    fun `createMovie works with only a title`() {
        val result = movieService.createMovie(MovieInput(title = "Inception"))

        assertEquals("Inception", result.title)
        assertNull(result.description)
        assertNull(result.releaseYear)
        assertNull(result.genre)
        assertNull(result.durationMinutes)
    }

    @Test
    fun `createMovie throws when title is empty`() {
        val error = assertFailsWith<ValidationException> {
            movieService.createMovie(MovieInput(title = ""))
        }

        assertEquals("Title is required", error.message)
        verify(exactly = 0) { movieRepository.save(any<MovieEntity>()) }
    }

    @Test
    fun `createMovie throws when title is only spaces`() {
        assertFailsWith<ValidationException> {
            movieService.createMovie(MovieInput(title = "     "))
        }
        verify(exactly = 0) { movieRepository.save(any<MovieEntity>()) }
    }

    @Test
    fun `createMovie accepts a title with exactly 255 characters`() {
        val title = "a".repeat(255)

        val result = movieService.createMovie(MovieInput(title = title))

        assertEquals(title, result.title)
    }

    @Test
    fun `createMovie throws when title is longer than 255 characters`() {
        val error = assertFailsWith<ValidationException> {
            movieService.createMovie(MovieInput(title = "a".repeat(256)))
        }

        assertEquals("Title must be 255 characters or less", error.message)
    }

    @Test
    fun `createMovie throws when genre is longer than 100 characters`() {
        val error = assertFailsWith<ValidationException> {
            movieService.createMovie(MovieInput(title = "Inception", genre = "g".repeat(101)))
        }

        assertEquals("Genre must be 100 characters or less", error.message)
    }

    @Test
    fun `createMovie accepts 1888 as release year`() {
        val result = movieService.createMovie(MovieInput(title = "Roundhay Garden Scene", releaseYear = 1888))

        assertEquals(1888, result.releaseYear)
    }

    @Test
    fun `createMovie throws when release year is before 1888`() {
        assertFailsWith<ValidationException> {
            movieService.createMovie(MovieInput(title = "Too old", releaseYear = 1887))
        }
    }

    @Test
    fun `createMovie accepts release year 5 years from now`() {
        val year = Year.now().value + 5

        val result = movieService.createMovie(MovieInput(title = "Upcoming", releaseYear = year))

        assertEquals(year, result.releaseYear)
    }

    @Test
    fun `createMovie throws when release year is more than 5 years from now`() {
        assertFailsWith<ValidationException> {
            movieService.createMovie(MovieInput(title = "Too far", releaseYear = Year.now().value + 6))
        }
    }

    @Test
    fun `createMovie throws when duration is zero or negative`() {
        for (duration in listOf(0, -1, -120)) {
            val error = assertFailsWith<ValidationException> {
                movieService.createMovie(MovieInput(title = "Inception", durationMinutes = duration))
            }
            assertEquals("Duration must be a positive number", error.message)
        }
    }

    // ----- updateMovie -----

    @Test
    fun `updateMovie changes the fields of an existing movie`() {
        every { movieRepository.findById(1L) } returns Optional.of(movie())

        val result = movieService.updateMovie(
            1L,
            MovieInput(title = "Interstellar", releaseYear = 2014, genre = "Drama", durationMinutes = 169)
        )

        assertEquals(1L, result.id)
        assertEquals("Interstellar", result.title)
        assertEquals(2014, result.releaseYear)
        assertEquals("Drama", result.genre)
        assertEquals(169, result.durationMinutes)
    }

    @Test
    fun `updateMovie clears optional fields that are not sent`() {
        // update is a full replace, so missing fields become null
        every { movieRepository.findById(1L) } returns Optional.of(movie())

        val result = movieService.updateMovie(1L, MovieInput(title = "Inception"))

        assertNull(result.genre)
        assertNull(result.releaseYear)
        assertNull(result.durationMinutes)
    }

    @Test
    fun `updateMovie keeps the artwork`() {
        every { movieRepository.findById(1L) } returns Optional.of(movie(artworkPath = "movie-1.png"))

        val result = movieService.updateMovie(1L, MovieInput(title = "Inception 2"))

        assertEquals("movie-1.png", result.artworkPath)
    }

    @Test
    fun `updateMovie throws NotFoundException when movie does not exist`() {
        every { movieRepository.findById(99L) } returns Optional.empty()

        assertFailsWith<NotFoundException> {
            movieService.updateMovie(99L, MovieInput(title = "Inception"))
        }
        verify(exactly = 0) { movieRepository.save(any<MovieEntity>()) }
    }

    @Test
    fun `updateMovie validates input before looking up the movie`() {
        assertFailsWith<ValidationException> {
            movieService.updateMovie(1L, MovieInput(title = ""))
        }
        verify(exactly = 0) { movieRepository.findById(any()) }
    }

    // ----- deleteMovie -----

    @Test
    fun `deleteMovie deletes the movie and its artwork file`() {
        val existing = movie(artworkPath = "movie-1.png")
        every { movieRepository.findById(1L) } returns Optional.of(existing)

        movieService.deleteMovie(1L)

        verify { movieRepository.delete(existing) }
        verify { artworkStorage.delete("movie-1.png") }
    }

    @Test
    fun `deleteMovie without artwork does not touch the storage`() {
        val existing = movie()
        every { movieRepository.findById(1L) } returns Optional.of(existing)

        movieService.deleteMovie(1L)

        verify { movieRepository.delete(existing) }
        verify(exactly = 0) { artworkStorage.delete(any()) }
    }

    @Test
    fun `deleteMovie throws NotFoundException when movie does not exist`() {
        every { movieRepository.findById(99L) } returns Optional.empty()

        assertFailsWith<NotFoundException> { movieService.deleteMovie(99L) }

        verify(exactly = 0) { movieRepository.delete(any<MovieEntity>()) }
    }

    // ----- saveArtwork -----

    @Test
    fun `saveArtwork stores the file and updates the movie`() {
        every { movieRepository.findById(1L) } returns Optional.of(movie())
        every { artworkStorage.save(1L, "image/png", any()) } returns "movie-1-123.png"

        val result = movieService.saveArtwork(1L, "image/png", byteArrayOf(1, 2, 3))

        assertEquals("movie-1-123.png", result.artworkPath)
        assertEquals("image/png", result.artworkContentType)
        verify(exactly = 0) { artworkStorage.delete(any()) }
    }

    @Test
    fun `saveArtwork deletes the old file only after the new one is saved`() {
        val existing = movie(artworkPath = "old.png")
        every { movieRepository.findById(1L) } returns Optional.of(existing)
        every { artworkStorage.save(1L, "image/jpeg", any()) } returns "new.jpg"

        movieService.saveArtwork(1L, "image/jpeg", byteArrayOf(1))

        verifyOrder {
            artworkStorage.save(1L, "image/jpeg", any())
            movieRepository.save(existing)
            artworkStorage.delete("old.png")
        }
    }

    @Test
    fun `saveArtwork accepts jpeg png and webp`() {
        every { movieRepository.findById(1L) } returns Optional.of(movie())
        every { artworkStorage.save(any(), any(), any()) } returns "file"

        for (type in listOf("image/jpeg", "image/png", "image/webp")) {
            val result = movieService.saveArtwork(1L, type, byteArrayOf(1))
            assertEquals(type, result.artworkContentType)
        }
    }

    @Test
    fun `saveArtwork rejects unsupported file types`() {
        for (type in listOf("image/gif", "application/pdf", "text/plain", "")) {
            val error = assertFailsWith<ValidationException> {
                movieService.saveArtwork(1L, type, byteArrayOf(1))
            }
            assertEquals("Only JPEG, PNG and WEBP images are allowed", error.message)
        }
        verify(exactly = 0) { artworkStorage.save(any(), any(), any()) }
    }

    @Test
    fun `saveArtwork rejects an empty file`() {
        val error = assertFailsWith<ValidationException> {
            movieService.saveArtwork(1L, "image/png", ByteArray(0))
        }

        assertEquals("Artwork file is empty", error.message)
    }

    @Test
    fun `saveArtwork accepts a file of exactly 5 MB`() {
        every { movieRepository.findById(1L) } returns Optional.of(movie())
        every { artworkStorage.save(any(), any(), any()) } returns "movie-1.png"

        val result = movieService.saveArtwork(1L, "image/png", ByteArray(MovieService.MAX_ARTWORK_BYTES))

        assertEquals("movie-1.png", result.artworkPath)
    }

    @Test
    fun `saveArtwork rejects a file bigger than 5 MB`() {
        val error = assertFailsWith<ValidationException> {
            movieService.saveArtwork(1L, "image/png", ByteArray(MovieService.MAX_ARTWORK_BYTES + 1))
        }

        assertEquals("Artwork must be smaller than 5 MB", error.message)
        verify(exactly = 0) { artworkStorage.save(any(), any(), any()) }
    }

    @Test
    fun `saveArtwork does not store a file when movie does not exist`() {
        every { movieRepository.findById(99L) } returns Optional.empty()

        assertFailsWith<NotFoundException> {
            movieService.saveArtwork(99L, "image/png", byteArrayOf(1))
        }
        verify(exactly = 0) { artworkStorage.save(any(), any(), any()) }
    }

    // ----- getArtwork -----

    @Test
    fun `getArtwork returns the file content and type`() {
        every { movieRepository.findById(1L) } returns Optional.of(movie(artworkPath = "movie-1.png"))
        every { artworkStorage.load("movie-1.png") } returns byteArrayOf(9, 8, 7)

        val artwork = movieService.getArtwork(1L)

        assertEquals("movie-1.png", artwork.fileName)
        assertEquals("image/png", artwork.contentType)
        assertContentEquals(byteArrayOf(9, 8, 7), artwork.bytes)
    }

    @Test
    fun `getArtwork throws NotFoundException when movie has no artwork`() {
        every { movieRepository.findById(1L) } returns Optional.of(movie())

        val error = assertFailsWith<NotFoundException> { movieService.getArtwork(1L) }

        assertEquals("Movie with id 1 has no artwork", error.message)
    }

    @Test
    fun `getArtwork throws NotFoundException when movie does not exist`() {
        every { movieRepository.findById(99L) } returns Optional.empty()

        assertFailsWith<NotFoundException> { movieService.getArtwork(99L) }
    }
}
