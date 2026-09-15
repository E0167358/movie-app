package com.movieapp.movie_service.grpc

import com.movieapp.grpc.movie.SaveMovieRequest
import com.movieapp.movie_service.entity.MovieEntity
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MovieMappersTest {

    @Test
    fun `toProto changes null fields to protobuf default values`() {
        val proto = MovieEntity(id = 5L, title = "Inception").toProto()

        assertEquals(5L, proto.id)
        assertEquals("Inception", proto.title)
        assertEquals("", proto.description)
        assertEquals("", proto.genre)
        assertEquals(0, proto.releaseYear)
        assertEquals(0, proto.durationMinutes)
        assertFalse(proto.hasArtwork)
    }

    @Test
    fun `toProto sets hasArtwork when the movie has an artwork file`() {
        val proto = MovieEntity(id = 1L, title = "Inception", artworkPath = "movie-1.png").toProto()

        assertTrue(proto.hasArtwork)
    }

    @Test
    fun `toInput changes 0 and blank strings to null`() {
        val request = SaveMovieRequest.newBuilder()
            .setTitle("Inception")
            .setDescription("   ")
            .build()

        val input = request.toInput()

        assertEquals("Inception", input.title)
        assertNull(input.description)
        assertNull(input.genre)
        assertNull(input.releaseYear)
        assertNull(input.durationMinutes)
    }

    @Test
    fun `toInput keeps values that were provided`() {
        val request = SaveMovieRequest.newBuilder()
            .setTitle("Inception")
            .setDescription("A dream heist")
            .setGenre("Sci-Fi")
            .setReleaseYear(2010)
            .setDurationMinutes(148)
            .build()

        val input = request.toInput()

        assertEquals("A dream heist", input.description)
        assertEquals("Sci-Fi", input.genre)
        assertEquals(2010, input.releaseYear)
        assertEquals(148, input.durationMinutes)
    }
}
