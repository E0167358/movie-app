package com.movieapp.gateway_service.artwork

import com.movieapp.gateway_service.client.MovieClient
import com.movieapp.gateway_service.model.ArtworkInput
import com.movieapp.gateway_service.model.MovieDto
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

@Controller
class ArtworkController(
    private val movieClient: MovieClient
) {

    @MutationMapping
    fun uploadArtwork(@Argument input: ArtworkInput): MovieDto {
        val bytes = ArtworkDecoder.decode(input.base64Data)
        // file type and "movie exists" are checked by movie-service
        return movieClient.uploadArtwork(input.movieId, input.fileName, input.contentType, bytes)
    }
}
