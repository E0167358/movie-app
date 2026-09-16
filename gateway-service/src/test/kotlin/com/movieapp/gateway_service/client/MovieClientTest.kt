package com.movieapp.gateway_service.client

import com.movieapp.gateway_service.model.MovieInput
import com.movieapp.grpc.movie.ListMoviesRequest
import com.movieapp.grpc.movie.Movie
import com.movieapp.grpc.movie.MovieIdRequest
import com.movieapp.grpc.movie.MovieListResponse
import com.movieapp.grpc.movie.MovieServiceGrpc
import com.movieapp.grpc.movie.SaveMovieRequest
import com.movieapp.grpc.movie.SearchMoviesRequest
import com.movieapp.grpc.movie.UpdateMovieRequest
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MovieClientTest {

    private lateinit var stub: MovieServiceGrpc.MovieServiceBlockingStub
    private lateinit var client: MovieClient

    @BeforeEach
    fun setUp() {
        stub = mockk()
        // withDeadlineAfter returns a new stub, here we just return the same mock
        every { stub.withDeadlineAfter(any<Long>(), any<TimeUnit>()) } returns stub
        client = MovieClient(stub, 5)
    }

    private fun protoMovie(hasArtwork: Boolean = false): Movie {
        return Movie.newBuilder()
            .setId(1L)
            .setTitle("Inception")
            .setDescription("A dream heist")
            .setReleaseYear(2010)
            .setGenre("Sci-Fi")
            .setDurationMinutes(148)
            .setHasArtwork(hasArtwork)
            .build()
    }

    @Test
    fun `getMovie maps all fields`() {
        every { stub.getMovie(any()) } returns protoMovie()

        val movie = client.getMovie(1L)

        assertNotNull(movie)
        assertEquals(1L, movie.id)
        assertEquals("Inception", movie.title)
        assertEquals("A dream heist", movie.description)
        assertEquals(2010, movie.releaseYear)
        assertEquals("Sci-Fi", movie.genre)
        assertEquals(148, movie.durationMinutes)
    }

    @Test
    fun `getMovie changes empty protobuf values to null`() {
        every { stub.getMovie(any()) } returns Movie.newBuilder().setId(1L).setTitle("Inception").build()

        val movie = client.getMovie(1L)

        assertNotNull(movie)
        assertNull(movie.description)
        assertNull(movie.releaseYear)
        assertNull(movie.genre)
        assertNull(movie.durationMinutes)
        assertNull(movie.artworkUrl)
    }

    @Test
    fun `getMovie builds the artwork url when the movie has artwork`() {
        every { stub.getMovie(any()) } returns protoMovie(hasArtwork = true)

        val movie = client.getMovie(1L)

        assertEquals("/artwork/1", movie?.artworkUrl)
    }

    @Test
    fun `getMovie sends the id`() {
        val request = slot<MovieIdRequest>()
        every { stub.getMovie(capture(request)) } returns protoMovie()

        client.getMovie(1L)

        assertEquals(1L, request.captured.id)
    }

    @Test
    fun `getMovie returns null when movie-service says NOT_FOUND`() {
        every { stub.getMovie(any()) } throws StatusRuntimeException(Status.NOT_FOUND)

        assertNull(client.getMovie(99L))
    }

    @Test
    fun `getMovie rethrows other gRPC errors`() {
        every { stub.getMovie(any()) } throws StatusRuntimeException(Status.UNAVAILABLE)

        val error = assertFailsWith<StatusRuntimeException> { client.getMovie(1L) }

        assertEquals(Status.Code.UNAVAILABLE, error.status.code)
    }

    @Test
    fun `every call uses the configured deadline`() {
        every { stub.getMovie(any()) } returns protoMovie()

        client.getMovie(1L)

        verify { stub.withDeadlineAfter(5L, TimeUnit.SECONDS) }
    }

    @Test
    fun `listMovies sends page and size and maps the result`() {
        val request = slot<ListMoviesRequest>()
        every { stub.listMovies(capture(request)) } returns
            MovieListResponse.newBuilder().addMovies(protoMovie()).setTotalCount(30).build()

        val page = client.listMovies(2, 10)

        assertEquals(2, request.captured.page)
        assertEquals(10, request.captured.size)
        assertEquals(1, page.items.size)
        assertEquals(30L, page.totalCount)
    }

    @Test
    fun `searchMovies sends the query`() {
        val request = slot<SearchMoviesRequest>()
        every { stub.searchMovies(capture(request)) } returns MovieListResponse.getDefaultInstance()

        val page = client.searchMovies("incep", 0, 5)

        assertEquals("incep", request.captured.query)
        assertEquals(5, request.captured.size)
        assertEquals(0, page.items.size)
    }

    @Test
    fun `createMovie sends missing optional fields as protobuf defaults`() {
        val request = slot<SaveMovieRequest>()
        every { stub.createMovie(capture(request)) } returns protoMovie()

        client.createMovie(MovieInput(title = "Inception"))

        assertEquals("Inception", request.captured.title)
        assertEquals("", request.captured.description)
        assertEquals(0, request.captured.releaseYear)
        assertEquals("", request.captured.genre)
        assertEquals(0, request.captured.durationMinutes)
    }

    @Test
    fun `updateMovie sends the id and the fields`() {
        val request = slot<UpdateMovieRequest>()
        every { stub.updateMovie(capture(request)) } returns protoMovie()

        client.updateMovie(1L, MovieInput(title = "Inception", releaseYear = 2010))

        assertEquals(1L, request.captured.id)
        assertEquals("Inception", request.captured.movie.title)
        assertEquals(2010, request.captured.movie.releaseYear)
    }
}
