package com.movieapp.gateway_service.controller

import com.movieapp.gateway_service.client.MovieClient
import com.movieapp.gateway_service.client.PeopleClient
import com.movieapp.gateway_service.model.MovieDto
import com.movieapp.gateway_service.model.PageDto
import com.movieapp.gateway_service.model.PersonDto
import com.movieapp.gateway_service.model.SearchParams
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@Controller
class SearchController(
    private val movieClient: MovieClient,
    private val peopleClient: PeopleClient,
    @Qualifier("searchExecutor") private val executor: Executor
) {

    // this only keeps the search text, the real work is in the two fields below
    @QueryMapping
    fun search(@Argument query: String, @Argument size: Int): SearchParams {
        return SearchParams(query.trim(), size)
    }

    // movies and people are separate fields that return a CompletableFuture,
    // so both services are called at the same time. if one fails,
    // the other result is still returned together with an error for the failed field
    @SchemaMapping(typeName = "SearchResult")
    fun movies(params: SearchParams): CompletableFuture<PageDto<MovieDto>> {
        return CompletableFuture.supplyAsync({ movieClient.searchMovies(params.query, 0, params.size) }, executor)
    }

    @SchemaMapping(typeName = "SearchResult")
    fun people(params: SearchParams): CompletableFuture<PageDto<PersonDto>> {
        return CompletableFuture.supplyAsync({ peopleClient.searchPeople(params.query, 0, params.size) }, executor)
    }
}
