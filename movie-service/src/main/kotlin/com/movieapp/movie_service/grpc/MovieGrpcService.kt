package com.movieapp.movie_service.grpc

import com.google.protobuf.ByteString
import com.movieapp.grpc.movie.ArtworkChunk
import com.movieapp.grpc.movie.ArtworkInfo
import com.movieapp.grpc.movie.DeleteResponse
import com.movieapp.grpc.movie.ListMoviesRequest
import com.movieapp.grpc.movie.Movie
import com.movieapp.grpc.movie.MovieIdRequest
import com.movieapp.grpc.movie.MovieListResponse
import com.movieapp.grpc.movie.MovieServiceGrpc
import com.movieapp.grpc.movie.SaveMovieRequest
import com.movieapp.grpc.movie.SearchMoviesRequest
import com.movieapp.grpc.movie.UpdateMovieRequest
import com.movieapp.movie_service.exception.NotFoundException
import com.movieapp.movie_service.exception.ValidationException
import com.movieapp.movie_service.service.MovieService
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

// Spring gRPC adds every BindableService bean to the gRPC server automatically
@Service
class MovieGrpcService(
    private val movieService: MovieService
) : MovieServiceGrpc.MovieServiceImplBase() {

    private val log = LoggerFactory.getLogger(MovieGrpcService::class.java)

    override fun getMovie(request: MovieIdRequest, responseObserver: StreamObserver<Movie>) {
        sendOne(responseObserver) { movieService.getMovie(request.id).toProto() }
    }

    override fun listMovies(request: ListMoviesRequest, responseObserver: StreamObserver<MovieListResponse>) {
        sendOne(responseObserver) { movieService.listMovies(request.page, request.size).toProto() }
    }

    override fun searchMovies(request: SearchMoviesRequest, responseObserver: StreamObserver<MovieListResponse>) {
        sendOne(responseObserver) {
            movieService.searchMovies(request.query, request.page, request.size).toProto()
        }
    }

    override fun createMovie(request: SaveMovieRequest, responseObserver: StreamObserver<Movie>) {
        sendOne(responseObserver) { movieService.createMovie(request.toInput()).toProto() }
    }

    override fun updateMovie(request: UpdateMovieRequest, responseObserver: StreamObserver<Movie>) {
        sendOne(responseObserver) {
            movieService.updateMovie(request.id, request.movie.toInput()).toProto()
        }
    }

    override fun deleteMovie(request: MovieIdRequest, responseObserver: StreamObserver<DeleteResponse>) {
        sendOne(responseObserver) {
            movieService.deleteMovie(request.id)
            DeleteResponse.newBuilder().setSuccess(true).build()
        }
    }

    // client sends the info first, then the image in chunks. we save it when the stream is finished
    override fun uploadArtwork(responseObserver: StreamObserver<Movie>): StreamObserver<ArtworkChunk> {
        return object : StreamObserver<ArtworkChunk> {
            private var info: ArtworkInfo? = null
            private val buffer = ByteArrayOutputStream()
            private var failed = false

            override fun onNext(message: ArtworkChunk) {
                if (failed) return

                if (message.hasInfo()) {
                    info = message.info
                    return
                }
                if (info == null) {
                    fail("First message must contain artwork info")
                    return
                }

                buffer.write(message.chunk.toByteArray())
                // stop early, don't keep reading a file that is too big
                if (buffer.size() > MovieService.MAX_ARTWORK_BYTES) {
                    fail("Artwork must be smaller than 5 MB")
                }
            }

            override fun onError(t: Throwable) {
                log.warn("Artwork upload cancelled by client: {}", t.message)
            }

            override fun onCompleted() {
                if (failed) return

                val artworkInfo = info
                if (artworkInfo == null) {
                    fail("No artwork info received")
                    return
                }
                sendOne(responseObserver) {
                    movieService.saveArtwork(
                        artworkInfo.movieId,
                        artworkInfo.contentType,
                        buffer.toByteArray()
                    ).toProto()
                }
            }

            private fun fail(message: String) {
                failed = true
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(message).asRuntimeException())
            }
        }
    }

    override fun downloadArtwork(request: MovieIdRequest, responseObserver: StreamObserver<ArtworkChunk>) {
        handleErrors(responseObserver) {
            val artwork = movieService.getArtwork(request.id)

            val info = ArtworkInfo.newBuilder()
                .setMovieId(request.id)
                .setFileName(artwork.fileName)
                .setContentType(artwork.contentType)
                .build()
            responseObserver.onNext(ArtworkChunk.newBuilder().setInfo(info).build())

            var offset = 0
            while (offset < artwork.bytes.size) {
                val length = minOf(CHUNK_SIZE, artwork.bytes.size - offset)
                val chunk = ByteString.copyFrom(artwork.bytes, offset, length)
                responseObserver.onNext(ArtworkChunk.newBuilder().setChunk(chunk).build())
                offset += length
            }
            responseObserver.onCompleted()
        }
    }

    private fun <T> sendOne(observer: StreamObserver<T>, block: () -> T) {
        handleErrors(observer) {
            observer.onNext(block())
            observer.onCompleted()
        }
    }

    // change our exceptions into gRPC status codes, so the gateway knows what went wrong
    private fun handleErrors(observer: StreamObserver<*>, block: () -> Unit) {
        try {
            block()
        } catch (e: NotFoundException) {
            observer.onError(Status.NOT_FOUND.withDescription(e.message).asRuntimeException())
        } catch (e: ValidationException) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException())
        } catch (e: Exception) {
            log.error("Unexpected error in movie gRPC service", e)
            observer.onError(Status.INTERNAL.withDescription("Something went wrong").asRuntimeException())
        }
    }

    companion object {
        private const val CHUNK_SIZE = 64 * 1024
    }
}
