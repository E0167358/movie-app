package com.movieapp.gateway_service.controller

import com.movieapp.gateway_service.client.MovieClient
import com.movieapp.gateway_service.client.PeopleClient
import com.movieapp.gateway_service.model.CreditDto
import com.movieapp.gateway_service.model.MovieDto
import com.movieapp.gateway_service.model.MovieInput
import com.movieapp.gateway_service.model.PageDto
import io.grpc.StatusRuntimeException
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class MovieController(
    private val movieClient: MovieClient,
    private val peopleClient: PeopleClient
) {

    private val log = LoggerFactory.getLogger(MovieController::class.java)

    @QueryMapping
    fun movies(@Argument page: Int, @Argument size: Int): PageDto<MovieDto> {
        return movieClient.listMovies(page, size)
    }

    @QueryMapping
    fun movie(@Argument id: Long): MovieDto? {
        return movieClient.getMovie(id)
    }

    @MutationMapping
    fun createMovie(@Argument input: MovieInput): MovieDto {
        return movieClient.createMovie(input)
    }

    @MutationMapping
    fun updateMovie(@Argument id: Long, @Argument input: MovieInput): MovieDto {
        return movieClient.updateMovie(id, input)
    }

    @MutationMapping
    fun deleteMovie(@Argument id: Long): Boolean {
        movieClient.deleteMovie(id)

        // credits are in people-service. if this call fails the movie is still deleted,
        // the leftover credits just point to a movie that does not exist (Credit.movie becomes null)
        try {
            peopleClient.deleteCreditsByMovie(id)
        } catch (e: StatusRuntimeException) {
            log.warn("Movie {} was deleted but its credits could not be removed: {}", id, e.status)
        }
        return true
    }

    // only runs when the client asks for "credits" in the query
    @SchemaMapping(typeName = "Movie")
    fun credits(movie: MovieDto): List<CreditDto> {
        return peopleClient.getCreditsByMovie(movie.id)
    }
}
