package com.movieapp.gateway_service.controller

import com.movieapp.gateway_service.client.MovieClient
import com.movieapp.gateway_service.client.PeopleClient
import com.movieapp.gateway_service.error.NotFoundException
import com.movieapp.gateway_service.model.CreditDto
import com.movieapp.gateway_service.model.CreditInput
import com.movieapp.gateway_service.model.MovieDto
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class CreditController(
    private val movieClient: MovieClient,
    private val peopleClient: PeopleClient
) {

    @MutationMapping
    fun addCredit(@Argument input: CreditInput): CreditDto {
        // people-service does not know about movies, so the gateway checks the movie exists first
        movieClient.getMovie(input.movieId)
            ?: throw NotFoundException("Movie with id ${input.movieId} not found")

        return peopleClient.addCredit(input)
    }

    @MutationMapping
    fun removeCredit(@Argument id: Long): Boolean {
        peopleClient.removeCredit(id)
        return true
    }

    // one call per credit. fine for the small number of credits a person has
    @SchemaMapping(typeName = "Credit")
    fun movie(credit: CreditDto): MovieDto? {
        return movieClient.getMovie(credit.movieId)
    }
}
