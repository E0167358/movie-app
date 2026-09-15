package com.movieapp.movie_service.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Component
class ArtworkStorage(@Value("\${app.artwork.dir}") artworkDir: String) {

    private val baseDir: Path = Paths.get(artworkDir).toAbsolutePath().normalize()

    init {
        Files.createDirectories(baseDir)
    }

    // we create the file name ourselves, so a file name from the user is never used as a path
    fun save(movieId: Long, contentType: String, bytes: ByteArray): String {
        val extension = when (contentType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val fileName = "movie-$movieId-${System.currentTimeMillis()}.$extension"
        Files.write(baseDir.resolve(fileName), bytes)
        return fileName
    }

    fun load(fileName: String): ByteArray {
        return Files.readAllBytes(baseDir.resolve(fileName))
    }

    fun delete(fileName: String) {
        Files.deleteIfExists(baseDir.resolve(fileName))
    }
}
