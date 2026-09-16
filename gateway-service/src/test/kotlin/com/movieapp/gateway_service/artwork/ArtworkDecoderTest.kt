package com.movieapp.gateway_service.artwork

import com.movieapp.gateway_service.error.BadRequestException
import org.junit.jupiter.api.Test
import java.util.Base64
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArtworkDecoderTest {

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    @Test
    fun `decodes plain base64`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)

        assertContentEquals(bytes, ArtworkDecoder.decode(encode(bytes)))
    }

    @Test
    fun `decodes a data url from the browser`() {
        val bytes = byteArrayOf(10, 20, 30)

        val result = ArtworkDecoder.decode("data:image/png;base64," + encode(bytes))

        assertContentEquals(bytes, result)
    }

    @Test
    fun `ignores spaces around the data`() {
        val bytes = byteArrayOf(7, 8, 9)

        assertContentEquals(bytes, ArtworkDecoder.decode("  " + encode(bytes) + "\n"))
    }

    @Test
    fun `rejects empty data`() {
        for (data in listOf("", "   ", "data:image/png;base64,")) {
            val error = assertFailsWith<BadRequestException> { ArtworkDecoder.decode(data) }
            assertEquals("Artwork file is empty", error.message)
        }
    }

    @Test
    fun `rejects text that is not base64`() {
        val error = assertFailsWith<BadRequestException> { ArtworkDecoder.decode("this is not base64!!") }

        assertEquals("Artwork data is not valid base64", error.message)
    }

    @Test
    fun `accepts a file of exactly 5 MB`() {
        val bytes = ByteArray(ArtworkDecoder.MAX_BYTES)

        assertEquals(ArtworkDecoder.MAX_BYTES, ArtworkDecoder.decode(encode(bytes)).size)
    }

    @Test
    fun `rejects a file one byte over 5 MB`() {
        // same base64 length as exactly 5 MB, so this is caught by the size check after decoding
        val bytes = ByteArray(ArtworkDecoder.MAX_BYTES + 1)

        val error = assertFailsWith<BadRequestException> { ArtworkDecoder.decode(encode(bytes)) }

        assertEquals("Artwork must be smaller than 5 MB", error.message)
    }

    @Test
    fun `rejects very long base64 before decoding it`() {
        val tooLong = "A".repeat(ArtworkDecoder.MAX_BASE64_LENGTH + 4)

        val error = assertFailsWith<BadRequestException> { ArtworkDecoder.decode(tooLong) }

        assertEquals("Artwork must be smaller than 5 MB", error.message)
    }
}
