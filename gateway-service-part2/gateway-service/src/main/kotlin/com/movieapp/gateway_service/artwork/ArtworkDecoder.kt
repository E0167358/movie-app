package com.movieapp.gateway_service.artwork

import com.movieapp.gateway_service.error.BadRequestException
import java.util.Base64

object ArtworkDecoder {

    // same limit as movie-service. we check here too, so a big file is rejected
    // before we decode it and stream it to movie-service
    const val MAX_BYTES = 5 * 1024 * 1024

    // base64 uses 4 characters for every 3 bytes
    const val MAX_BASE64_LENGTH = (MAX_BYTES + 2) / 3 * 4

    fun decode(base64Data: String): ByteArray {
        // accept plain base64 and also a data url like "data:image/png;base64,iVBOR..."
        val data = base64Data.substringAfter("base64,", base64Data).trim()

        if (data.isEmpty()) {
            throw BadRequestException("Artwork file is empty")
        }
        if (data.length > MAX_BASE64_LENGTH) {
            throw BadRequestException("Artwork must be smaller than 5 MB")
        }

        val bytes = try {
            Base64.getDecoder().decode(data)
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Artwork data is not valid base64")
        }

        // the length check above can let a file a few bytes over 5 MB through
        if (bytes.size > MAX_BYTES) {
            throw BadRequestException("Artwork must be smaller than 5 MB")
        }
        return bytes
    }
}
