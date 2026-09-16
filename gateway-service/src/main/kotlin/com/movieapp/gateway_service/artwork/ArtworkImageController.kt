package com.movieapp.gateway_service.artwork

import com.movieapp.gateway_service.client.MovieClient
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

// images are loaded by <img src="/artwork/1"> in the browser, so this is a normal HTTP GET.
// all data reading and writing still goes through GraphQL
@RestController
class ArtworkImageController(
    private val movieClient: MovieClient
) {

    @GetMapping("/artwork/{movieId}")
    fun getArtwork(@PathVariable movieId: Long): ResponseEntity<ByteArray> {
        val artwork = movieClient.downloadArtwork(movieId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(artwork.contentType))
            // the url stays the same when the artwork is replaced, so the browser has to check again
            .cacheControl(CacheControl.noCache())
            .body(artwork.bytes)
    }
}
