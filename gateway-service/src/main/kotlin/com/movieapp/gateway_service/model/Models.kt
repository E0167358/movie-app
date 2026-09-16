package com.movieapp.gateway_service.model

// These are the objects GraphQL returns. We don't return the protobuf classes directly,
// so the GraphQL API does not change every time the proto files change.

data class MovieDto(
    val id: Long,
    val title: String,
    val description: String?,
    val releaseYear: Int?,
    val genre: String?,
    val durationMinutes: Int?,
    val hasArtwork: Boolean
) {
    // the image itself is served by the gateway at this url
    val artworkUrl: String?
        get() = if (hasArtwork) "/artwork/$id" else null
}

data class PersonDto(
    val id: Long,
    val name: String,
    val bio: String?,
    val birthYear: Int?
)

data class CreditDto(
    val id: Long,
    val movieId: Long,
    val role: Role,
    val characterName: String?,
    val person: PersonDto
)

enum class Role {
    ACTOR,
    DIRECTOR,
    WRITER,
    PRODUCER
}

data class PageDto<T>(
    val items: List<T>,
    val totalCount: Long
)

data class SearchParams(
    val query: String,
    val size: Int
)

// ----- inputs from GraphQL mutations -----

data class MovieInput(
    val title: String,
    val description: String? = null,
    val releaseYear: Int? = null,
    val genre: String? = null,
    val durationMinutes: Int? = null
)

data class PersonInput(
    val name: String,
    val bio: String? = null,
    val birthYear: Int? = null
)

data class CreditInput(
    val movieId: Long,
    val personId: Long,
    val role: Role,
    val characterName: String? = null
)
