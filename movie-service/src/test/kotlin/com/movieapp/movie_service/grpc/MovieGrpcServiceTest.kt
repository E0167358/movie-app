package com.movieapp.movie_service.grpc

import com.google.protobuf.ByteString
import com.movieapp.grpc.movie.ArtworkChunk
import com.movieapp.grpc.movie.ArtworkInfo
import com.movieapp.grpc.movie.DeleteResponse
import com.movieapp.grpc.movie.ListMoviesRequest
import com.movieapp.grpc.movie.Movie
import com.movieapp.grpc.movie.MovieIdRequest
import com.movieapp.grpc.movie.MovieListResponse
import com.movieapp.grpc.movie.SaveMovieRequest
import com.movieapp.grpc.movie.SearchMoviesRequest
import com.movieapp.grpc.movie.UpdateMovieRequest
import com.movieapp.movie_service.entity.MovieEntity
import com.movieapp.movie_service.exception.NotFoundException
import com.movieapp.movie_service.exception.ValidationException
import com.movieapp.movie_service.service.Artwork
import com.movieapp.movie_service.service.MovieInput
import com.movieapp.movie_service.service.MovieService
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MovieGrpcServiceTest {

    private lateinit var movieService: MovieService
    private lateinit var grpcService: MovieGrpcService

    @BeforeEach
    fun setUp() {
        movieService = mockk()
        grpcService = MovieGrpcService(movieService)
    }

    // fake StreamObserver that remembers what the server sent back
    private class TestObserver<T> : StreamObserver<T> {
        val values = mutableListOf<T>()
        var error: Throwable? = null
        var completed = false

        override fun onNext(value: T) {
            values.add(value)
        }

        override fun onError(t: Throwable) {
            error = t
        }

        override fun onCompleted() {
            completed = true
        }

        fun statusCode(): Status.Code? = error?.let { Status.fromThrowable(it).code }

        fun statusMessage(): String? = error?.let { Status.fromThrowable(it).description }
    }

    private fun idRequest(id: Long) = MovieIdRequest.newBuilder().setId(id).build()

    private fun infoMessage(movieId: Long = 1L, contentType: String = "image/png"): ArtworkChunk {
        val info = ArtworkInfo.newBuilder()
            .setMovieId(movieId)
            .setFileName("poster.png")
            .setContentType(contentType)
        return ArtworkChunk.newBuilder().setInfo(info).build()
    }

    private fun chunkMessage(bytes: ByteArray): ArtworkChunk {
        return ArtworkChunk.newBuilder().setChunk(ByteString.copyFrom(bytes)).build()
    }

    // ----- normal requests -----

    @Test
    fun `getMovie sends the movie and completes`() {
        every { movieService.getMovie(1L) } returns MovieEntity(id = 1L, title = "Inception")
        val observer = TestObserver<Movie>()

        grpcService.getMovie(idRequest(1L), observer)

        assertEquals(1, observer.values.size)
        assertEquals("Inception", observer.values[0].title)
        assertTrue(observer.completed)
        assertNull(observer.error)
    }

    @Test
    fun `listMovies returns movies and total count`() {
        val movies = listOf(MovieEntity(id = 1L, title = "A"), MovieEntity(id = 2L, title = "B"))
        every { movieService.listMovies(0, 20) } returns PageImpl(movies, PageRequest.of(0, 20), 42)
        val observer = TestObserver<MovieListResponse>()

        grpcService.listMovies(ListMoviesRequest.newBuilder().setPage(0).setSize(20).build(), observer)

        assertEquals(2, observer.values[0].moviesCount)
        assertEquals(42L, observer.values[0].totalCount)
        assertTrue(observer.completed)
    }

    @Test
    fun `searchMovies passes query page and size to the service`() {
        every { movieService.searchMovies("incep", 1, 5) } returns PageImpl(emptyList<MovieEntity>())
        val observer = TestObserver<MovieListResponse>()
        val request = SearchMoviesRequest.newBuilder().setQuery("incep").setPage(1).setSize(5).build()

        grpcService.searchMovies(request, observer)

        verify { movieService.searchMovies("incep", 1, 5) }
        assertEquals(0, observer.values[0].moviesCount)
    }

    @Test
    fun `createMovie treats 0 and empty strings as not provided`() {
        val input = slot<MovieInput>()
        every { movieService.createMovie(capture(input)) } returns MovieEntity(id = 1L, title = "Inception")

        grpcService.createMovie(SaveMovieRequest.newBuilder().setTitle("Inception").build(), TestObserver())

        assertEquals("Inception", input.captured.title)
        assertNull(input.captured.description)
        assertNull(input.captured.releaseYear)
        assertNull(input.captured.genre)
        assertNull(input.captured.durationMinutes)
    }

    @Test
    fun `updateMovie passes id and fields to the service`() {
        val input = slot<MovieInput>()
        every { movieService.updateMovie(7L, capture(input)) } returns MovieEntity(id = 7L, title = "Heat")
        val movie = SaveMovieRequest.newBuilder().setTitle("Heat").setReleaseYear(1995).build()
        val observer = TestObserver<Movie>()

        grpcService.updateMovie(UpdateMovieRequest.newBuilder().setId(7L).setMovie(movie).build(), observer)

        assertEquals("Heat", input.captured.title)
        assertEquals(1995, input.captured.releaseYear)
        assertEquals(7L, observer.values[0].id)
    }

    @Test
    fun `deleteMovie returns success`() {
        justRun { movieService.deleteMovie(1L) }
        val observer = TestObserver<DeleteResponse>()

        grpcService.deleteMovie(idRequest(1L), observer)

        assertTrue(observer.values[0].success)
        assertTrue(observer.completed)
    }

    // ----- error mapping -----

    @Test
    fun `NotFoundException becomes NOT_FOUND status`() {
        every { movieService.getMovie(99L) } throws NotFoundException("Movie with id 99 not found")
        val observer = TestObserver<Movie>()

        grpcService.getMovie(idRequest(99L), observer)

        assertEquals(Status.Code.NOT_FOUND, observer.statusCode())
        assertEquals("Movie with id 99 not found", observer.statusMessage())
        assertFalse(observer.completed)
        assertTrue(observer.values.isEmpty())
    }

    @Test
    fun `ValidationException becomes INVALID_ARGUMENT status`() {
        every { movieService.createMovie(any()) } throws ValidationException("Title is required")
        val observer = TestObserver<Movie>()

        grpcService.createMovie(SaveMovieRequest.getDefaultInstance(), observer)

        assertEquals(Status.Code.INVALID_ARGUMENT, observer.statusCode())
        assertEquals("Title is required", observer.statusMessage())
    }

    @Test
    fun `unexpected exception becomes INTERNAL status without showing the real message`() {
        every { movieService.getMovie(1L) } throws RuntimeException("database connection details")
        val observer = TestObserver<Movie>()

        grpcService.getMovie(idRequest(1L), observer)

        assertEquals(Status.Code.INTERNAL, observer.statusCode())
        assertEquals("Something went wrong", observer.statusMessage())
    }

    // ----- uploadArtwork (client streaming) -----

    @Test
    fun `uploadArtwork joins all chunks and saves the image`() {
        val savedBytes = slot<ByteArray>()
        every { movieService.saveArtwork(1L, "image/png", capture(savedBytes)) } returns
            MovieEntity(id = 1L, title = "Inception", artworkPath = "movie-1.png")
        val responseObserver = TestObserver<Movie>()

        val requestObserver = grpcService.uploadArtwork(responseObserver)
        requestObserver.onNext(infoMessage())
        requestObserver.onNext(chunkMessage(byteArrayOf(1, 2, 3)))
        requestObserver.onNext(chunkMessage(byteArrayOf(4, 5)))
        requestObserver.onCompleted()

        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), savedBytes.captured)
        assertTrue(responseObserver.values[0].hasArtwork)
        assertTrue(responseObserver.completed)
    }

    @Test
    fun `uploadArtwork fails when a chunk arrives before the info`() {
        val responseObserver = TestObserver<Movie>()

        val requestObserver = grpcService.uploadArtwork(responseObserver)
        requestObserver.onNext(chunkMessage(byteArrayOf(1, 2)))
        requestObserver.onCompleted()

        assertEquals(Status.Code.INVALID_ARGUMENT, responseObserver.statusCode())
        assertEquals("First message must contain artwork info", responseObserver.statusMessage())
        verify(exactly = 0) { movieService.saveArtwork(any(), any(), any()) }
    }

    @Test
    fun `uploadArtwork fails when the stream has no messages`() {
        val responseObserver = TestObserver<Movie>()

        val requestObserver = grpcService.uploadArtwork(responseObserver)
        requestObserver.onCompleted()

        assertEquals(Status.Code.INVALID_ARGUMENT, responseObserver.statusCode())
        assertEquals("No artwork info received", responseObserver.statusMessage())
    }

    @Test
    fun `uploadArtwork stops as soon as the file is bigger than 5 MB`() {
        val responseObserver = TestObserver<Movie>()

        val requestObserver = grpcService.uploadArtwork(responseObserver)
        requestObserver.onNext(infoMessage())
        requestObserver.onNext(chunkMessage(ByteArray(MovieService.MAX_ARTWORK_BYTES)))
        requestObserver.onNext(chunkMessage(ByteArray(1)))
        // anything after the error should be ignored
        requestObserver.onNext(chunkMessage(ByteArray(1)))
        requestObserver.onCompleted()

        assertEquals(Status.Code.INVALID_ARGUMENT, responseObserver.statusCode())
        assertEquals("Artwork must be smaller than 5 MB", responseObserver.statusMessage())
        assertFalse(responseObserver.completed)
        verify(exactly = 0) { movieService.saveArtwork(any(), any(), any()) }
    }

    @Test
    fun `uploadArtwork returns NOT_FOUND when the movie does not exist`() {
        every { movieService.saveArtwork(99L, any(), any()) } throws NotFoundException("Movie with id 99 not found")
        val responseObserver = TestObserver<Movie>()

        val requestObserver = grpcService.uploadArtwork(responseObserver)
        requestObserver.onNext(infoMessage(movieId = 99L))
        requestObserver.onNext(chunkMessage(byteArrayOf(1)))
        requestObserver.onCompleted()

        assertEquals(Status.Code.NOT_FOUND, responseObserver.statusCode())
    }

    @Test
    fun `uploadArtwork saves nothing when the client cancels`() {
        val responseObserver = TestObserver<Movie>()

        val requestObserver = grpcService.uploadArtwork(responseObserver)
        requestObserver.onNext(infoMessage())
        requestObserver.onNext(chunkMessage(byteArrayOf(1, 2, 3)))
        requestObserver.onError(RuntimeException("client cancelled"))

        verify(exactly = 0) { movieService.saveArtwork(any(), any(), any()) }
        assertFalse(responseObserver.completed)
    }

    // ----- downloadArtwork (server streaming) -----

    @Test
    fun `downloadArtwork sends info first and then the file in 64 KB chunks`() {
        val bytes = ByteArray(200 * 1024) { (it % 256).toByte() }
        every { movieService.getArtwork(1L) } returns Artwork("movie-1.png", "image/png", bytes)
        val observer = TestObserver<ArtworkChunk>()

        grpcService.downloadArtwork(idRequest(1L), observer)

        val first = observer.values[0]
        assertTrue(first.hasInfo())
        assertEquals("image/png", first.info.contentType)

        val chunks = observer.values.drop(1)
        // 200 KB = 64 + 64 + 64 + 8
        assertEquals(4, chunks.size)
        val joined = chunks.fold(ByteArray(0)) { all, chunk -> all + chunk.chunk.toByteArray() }
        assertContentEquals(bytes, joined)
        assertTrue(observer.completed)
    }

    @Test
    fun `downloadArtwork returns NOT_FOUND when the movie has no artwork`() {
        every { movieService.getArtwork(1L) } throws NotFoundException("Movie with id 1 has no artwork")
        val observer = TestObserver<ArtworkChunk>()

        grpcService.downloadArtwork(idRequest(1L), observer)

        assertEquals(Status.Code.NOT_FOUND, observer.statusCode())
        assertTrue(observer.values.isEmpty())
    }
}
