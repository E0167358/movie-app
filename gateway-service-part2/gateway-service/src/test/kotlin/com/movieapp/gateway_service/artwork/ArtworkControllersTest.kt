package com.movieapp.gateway_service.artwork

import com.movieapp.gateway_service.client.MovieClient
import com.movieapp.gateway_service.error.BadRequestException
import com.movieapp.gateway_service.model.ArtworkFile
import com.movieapp.gateway_service.model.ArtworkInput
import com.movieapp.gateway_service.model.MovieDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.util.Base64
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArtworkControllersTest {

    private val movieClient = mockk<MovieClient>()
    private val artworkController = ArtworkController(movieClient)
    private val imageController = ArtworkImageController(movieClient)

    private val inception = MovieDto(1L, "Inception", null, 2010, null, null, hasArtwork = true)

    // ----- uploadArtwork mutation -----

    @Test
    fun `uploadArtwork decodes the image and sends the bytes to movie-service`() {
        val bytes = byteArrayOf(1, 2, 3)
        val sent = slot<ByteArray>()
        every { movieClient.uploadArtwork(1L, "poster.png", "image/png", capture(sent)) } returns inception
        val input = ArtworkInput(1L, "poster.png", "image/png", Base64.getEncoder().encodeToString(bytes))

        val result = artworkController.uploadArtwork(input)

        assertContentEquals(bytes, sent.captured)
        assertEquals("/artwork/1", result.artworkUrl)
    }

    @Test
    fun `uploadArtwork does not call movie-service when the data is invalid`() {
        val input = ArtworkInput(1L, "poster.png", "image/png", "not base64!!")

        assertFailsWith<BadRequestException> { artworkController.uploadArtwork(input) }

        verify(exactly = 0) { movieClient.uploadArtwork(any(), any(), any(), any()) }
    }

    // ----- GET /artwork/{movieId} -----

    @Test
    fun `getArtwork returns the image with its content type`() {
        every { movieClient.downloadArtwork(1L) } returns ArtworkFile("image/png", byteArrayOf(9, 8, 7))

        val response = imageController.getArtwork(1L)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(MediaType.IMAGE_PNG, response.headers.contentType)
        assertContentEquals(byteArrayOf(9, 8, 7), response.body)
    }

    @Test
    fun `getArtwork tells the browser to check again because the url does not change`() {
        every { movieClient.downloadArtwork(1L) } returns ArtworkFile("image/jpeg", byteArrayOf(1))

        val response = imageController.getArtwork(1L)

        assertEquals("no-cache", response.headers.cacheControl)
    }

    @Test
    fun `getArtwork returns 404 when there is no artwork`() {
        every { movieClient.downloadArtwork(1L) } returns null

        val response = imageController.getArtwork(1L)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }
}
