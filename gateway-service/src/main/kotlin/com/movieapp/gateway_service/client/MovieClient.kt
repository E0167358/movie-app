package com.movieapp.gateway_service.client

import com.movieapp.gateway_service.model.MovieDto
import com.movieapp.gateway_service.model.MovieInput
import com.movieapp.gateway_service.model.PageDto
import com.movieapp.grpc.movie.ListMoviesRequest
import com.movieapp.grpc.movie.MovieIdRequest
import com.movieapp.grpc.movie.MovieServiceGrpc
import com.movieapp.grpc.movie.SearchMoviesRequest
import com.movieapp.grpc.movie.UpdateMovieRequest
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class MovieClient(
    private val stub: MovieServiceGrpc.MovieServiceBlockingStub,
    @Value("\${app.grpc.deadline-seconds}") private val deadlineSeconds: Long
) {

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
}
