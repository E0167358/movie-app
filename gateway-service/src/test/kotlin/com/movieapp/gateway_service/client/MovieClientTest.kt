package com.movieapp.gateway_service.client

import com.google.protobuf.ByteString
import com.movieapp.gateway_service.model.MovieInput
import com.movieapp.grpc.movie.ArtworkChunk
import com.movieapp.grpc.movie.ArtworkInfo
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
import io.grpc.stub.StreamObserver
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MovieClientTest {

    private lateinit var stub: MovieServiceGrpc.MovieServiceBlockingStub
    private lateinit var asyncStub: MovieServiceGrpc.MovieServiceStub
    private lateinit var client: MovieClient

    @BeforeEach
    fun setUp() {
        stub = mockk()
        asyncStub = mockk()
        // withDeadlineAfter returns a new stub, here we just return the same mock
        every { stub.withDeadlineAfter(any<Long>(), any<TimeUnit>()) } returns stub
        every { asyncStub.withDeadlineAfter(any<Long>(), any<TimeUnit>()) } returns asyncStub
        client = MovieClient(stub, asyncStub, 5, 30)
    }

    // fake request stream that remembers what the client sent.
    // onMessage lets a test answer like the real server would
    private class FakeUploadStream(
        private val onMessage: (FakeUploadStream, ArtworkChunk) -> Unit = { _, _ -> },
        private val onDone: () -> Unit = {}
    ) : StreamObserver<ArtworkChunk> {
        val messages = mutableListOf<ArtworkChunk>()
        var completed = false

        override fun onNext(value: ArtworkChunk) {
            messages.add(value)
            onMessage(this, value)
        }

        override fun onError(t: Throwable) {}

        override fun onCompleted() {
            completed = true
            onDone()
        }
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

    // ----- artwork upload (client streaming) -----

    @Test
    fun `uploadArtwork sends info first and then the image in 64 KB chunks`() {
        val bytes = ByteArray(200 * 1024) { (it % 256).toByte() }
        lateinit var fakeStream: FakeUploadStream
        every { asyncStub.uploadArtwork(any()) } answers {
            val responseObserver = firstArg<StreamObserver<Movie>>()
            fakeStream = FakeUploadStream(onDone = {
                responseObserver.onNext(protoMovie(hasArtwork = true))
                responseObserver.onCompleted()
            })
            fakeStream
        }

        val movie = client.uploadArtwork(1L, "poster.png", "image/png", bytes)

        assertEquals("/artwork/1", movie.artworkUrl)
        assertTrue(fakeStream.completed)

        val first = fakeStream.messages[0]
        assertTrue(first.hasInfo())
        assertEquals(1L, first.info.movieId)
        assertEquals("poster.png", first.info.fileName)
        assertEquals("image/png", first.info.contentType)

        val chunks = fakeStream.messages.drop(1)
        // 200 KB = 64 + 64 + 64 + 8
        assertEquals(4, chunks.size)
        val joined = chunks.fold(ByteArray(0)) { all, chunk -> all + chunk.chunk.toByteArray() }
        assertContentEquals(bytes, joined)
    }

    @Test
    fun `uploadArtwork uses the longer upload deadline`() {
        every { asyncStub.uploadArtwork(any()) } answers {
            val responseObserver = firstArg<StreamObserver<Movie>>()
            FakeUploadStream(onDone = { responseObserver.onNext(protoMovie()) })
        }

        client.uploadArtwork(1L, "poster.png", "image/png", byteArrayOf(1))

        verify { asyncStub.withDeadlineAfter(30L, TimeUnit.SECONDS) }
    }

    @Test
    fun `uploadArtwork throws the gRPC error when movie-service rejects the file`() {
        every { asyncStub.uploadArtwork(any()) } answers {
            val responseObserver = firstArg<StreamObserver<Movie>>()
            FakeUploadStream(onDone = {
                responseObserver.onError(
                    StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Only JPEG, PNG and WEBP images are allowed"))
                )
            })
        }

        val error = assertFailsWith<StatusRuntimeException> {
            client.uploadArtwork(1L, "doc.pdf", "application/pdf", byteArrayOf(1, 2))
        }

        assertEquals(Status.Code.INVALID_ARGUMENT, error.status.code)
        assertEquals("Only JPEG, PNG and WEBP images are allowed", error.status.description)
    }

    @Test
    fun `uploadArtwork stops sending chunks once the server has answered with an error`() {
        lateinit var fakeStream: FakeUploadStream
        every { asyncStub.uploadArtwork(any()) } answers {
            val responseObserver = firstArg<StreamObserver<Movie>>()
            // server fails as soon as the first image chunk arrives
            fakeStream = FakeUploadStream(onMessage = { _, message ->
                if (!message.hasInfo()) {
                    responseObserver.onError(StatusRuntimeException(Status.NOT_FOUND))
                }
            })
            fakeStream
        }

        assertFailsWith<StatusRuntimeException> {
            client.uploadArtwork(99L, "poster.png", "image/png", ByteArray(1024 * 1024))
        }

        // info + 1 chunk, instead of info + 16 chunks for 1 MB
        assertEquals(2, fakeStream.messages.size)
    }

    // ----- artwork download (server streaming) -----

    @Test
    fun `downloadArtwork joins the chunks and keeps the content type`() {
        val info = ArtworkChunk.newBuilder()
            .setInfo(ArtworkInfo.newBuilder().setMovieId(1L).setContentType("image/webp"))
            .build()
        val part1 = ArtworkChunk.newBuilder().setChunk(ByteString.copyFrom(byteArrayOf(1, 2, 3))).build()
        val part2 = ArtworkChunk.newBuilder().setChunk(ByteString.copyFrom(byteArrayOf(4, 5))).build()
        every { stub.downloadArtwork(any()) } returns listOf(info, part1, part2).iterator()

        val artwork = client.downloadArtwork(1L)

        assertEquals("image/webp", artwork?.contentType)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), artwork?.bytes)
    }

    @Test
    fun `downloadArtwork returns null when there is no artwork`() {
        every { stub.downloadArtwork(any()) } throws StatusRuntimeException(Status.NOT_FOUND)

        assertNull(client.downloadArtwork(1L))
    }

    @Test
    fun `downloadArtwork rethrows other gRPC errors`() {
        every { stub.downloadArtwork(any()) } throws StatusRuntimeException(Status.UNAVAILABLE)

        assertFailsWith<StatusRuntimeException> { client.downloadArtwork(1L) }
    }
}
