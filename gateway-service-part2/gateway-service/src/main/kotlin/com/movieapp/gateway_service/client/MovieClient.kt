package com.movieapp.gateway_service.client

import com.google.protobuf.ByteString
import com.movieapp.gateway_service.model.ArtworkFile
import com.movieapp.gateway_service.model.MovieDto
import com.movieapp.gateway_service.model.MovieInput
import com.movieapp.gateway_service.model.PageDto
import com.movieapp.grpc.movie.ArtworkChunk
import com.movieapp.grpc.movie.ArtworkInfo
import com.movieapp.grpc.movie.ListMoviesRequest
import com.movieapp.grpc.movie.Movie
import com.movieapp.grpc.movie.MovieIdRequest
import com.movieapp.grpc.movie.MovieServiceGrpc
import com.movieapp.grpc.movie.SearchMoviesRequest
import com.movieapp.grpc.movie.UpdateMovieRequest
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@Component
class MovieClient(
    private val stub: MovieServiceGrpc.MovieServiceBlockingStub,
    private val asyncStub: MovieServiceGrpc.MovieServiceStub,
    @Value("\${app.grpc.deadline-seconds}") private val deadlineSeconds: Long,
    @Value("\${app.grpc.upload-deadline-seconds}") private val uploadDeadlineSeconds: Long
) {

    companion object {
        const val CHUNK_SIZE = 64 * 1024
    }

    // every call gets a deadline, so a slow service can't keep the request waiting forever
    private fun stub() = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)

    // returns null instead of an error, because GraphQL "movie(id)" is allowed to be null
    fun getMovie(id: Long): MovieDto? {
        return try {
            stub().getMovie(MovieIdRequest.newBuilder().setId(id).build()).toDto()
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.Code.NOT_FOUND) null else throw e
        }
    }

    fun listMovies(page: Int, size: Int): PageDto<MovieDto> {
        val request = ListMoviesRequest.newBuilder().setPage(page).setSize(size).build()
        return stub().listMovies(request).toPage()
    }

    fun searchMovies(query: String, page: Int, size: Int): PageDto<MovieDto> {
        val request = SearchMoviesRequest.newBuilder().setQuery(query).setPage(page).setSize(size).build()
        return stub().searchMovies(request).toPage()
    }

    fun createMovie(input: MovieInput): MovieDto {
        return stub().createMovie(input.toProto()).toDto()
    }

    fun updateMovie(id: Long, input: MovieInput): MovieDto {
        val request = UpdateMovieRequest.newBuilder().setId(id).setMovie(input.toProto()).build()
        return stub().updateMovie(request).toDto()
    }

    fun deleteMovie(id: Long) {
        stub().deleteMovie(MovieIdRequest.newBuilder().setId(id).build())
    }

    // client streaming: send the info first, then the image in 64 KB chunks
    fun uploadArtwork(movieId: Long, fileName: String, contentType: String, bytes: ByteArray): MovieDto {
        val result = CompletableFuture<Movie>()

        val responseObserver = object : StreamObserver<Movie> {
            override fun onNext(value: Movie) {
                result.complete(value)
            }

            override fun onError(t: Throwable) {
                result.completeExceptionally(t)
            }

            override fun onCompleted() {
                // does nothing if onNext already completed the future
                result.completeExceptionally(IllegalStateException("movie-service did not return a movie"))
            }
        }

        val requestObserver = asyncStub
            .withDeadlineAfter(uploadDeadlineSeconds, TimeUnit.SECONDS)
            .uploadArtwork(responseObserver)

        val info = ArtworkInfo.newBuilder()
            .setMovieId(movieId)
            .setFileName(fileName)
            .setContentType(contentType)
            .build()
        requestObserver.onNext(ArtworkChunk.newBuilder().setInfo(info).build())

        var offset = 0
        // stop sending when the server already answered, for example it rejected the file
        while (offset < bytes.size && !result.isDone) {
            val length = minOf(CHUNK_SIZE, bytes.size - offset)
            val chunk = ByteString.copyFrom(bytes, offset, length)
            requestObserver.onNext(ArtworkChunk.newBuilder().setChunk(chunk).build())
            offset += length
        }
        requestObserver.onCompleted()

        return try {
            result.get().toDto()
        } catch (e: ExecutionException) {
            // throw the real gRPC error, so it is mapped to a GraphQL error like the other calls
            throw e.cause ?: e
        }
    }

    // server streaming: first message is the info, the rest are image chunks
    fun downloadArtwork(movieId: Long): ArtworkFile? {
        return try {
            val messages = stub().downloadArtwork(MovieIdRequest.newBuilder().setId(movieId).build())
            var contentType = "application/octet-stream"
            val output = ByteArrayOutputStream()

            messages.forEach { message ->
                if (message.hasInfo()) {
                    contentType = message.info.contentType
                } else {
                    output.write(message.chunk.toByteArray())
                }
            }
            ArtworkFile(contentType, output.toByteArray())
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.Code.NOT_FOUND) null else throw e
        }
    }
}
