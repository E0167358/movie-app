package com.movieapp.gateway_service.client

import com.movieapp.gateway_service.model.CreditDto
import com.movieapp.gateway_service.model.CreditInput
import com.movieapp.gateway_service.model.MovieDto
import com.movieapp.gateway_service.model.MovieInput
import com.movieapp.gateway_service.model.PageDto
import com.movieapp.gateway_service.model.PersonDto
import com.movieapp.gateway_service.model.PersonInput
import com.movieapp.gateway_service.model.Role
import com.movieapp.grpc.movie.Movie
import com.movieapp.grpc.movie.MovieListResponse
import com.movieapp.grpc.movie.SaveMovieRequest
import com.movieapp.grpc.people.AddCreditRequest
import com.movieapp.grpc.people.Credit
import com.movieapp.grpc.people.Person
import com.movieapp.grpc.people.PersonListResponse
import com.movieapp.grpc.people.SavePersonRequest
import com.movieapp.grpc.people.Role as ProtoRole

// protobuf uses 0 and "" for "not set", GraphQL uses null

// ----- movies -----

fun Movie.toDto(): MovieDto {
    return MovieDto(
        id = id,
        title = title,
        description = description.ifEmpty { null },
        releaseYear = releaseYear.takeIf { it != 0 },
        genre = genre.ifEmpty { null },
        durationMinutes = durationMinutes.takeIf { it != 0 },
        hasArtwork = hasArtwork
    )
}

fun MovieListResponse.toPage(): PageDto<MovieDto> {
    return PageDto(items = moviesList.map { it.toDto() }, totalCount = totalCount)
}

fun MovieInput.toProto(): SaveMovieRequest {
    return SaveMovieRequest.newBuilder()
        .setTitle(title)
        .setDescription(description ?: "")
        .setReleaseYear(releaseYear ?: 0)
        .setGenre(genre ?: "")
        .setDurationMinutes(durationMinutes ?: 0)
        .build()
}

// ----- people -----

fun Person.toDto(): PersonDto {
    return PersonDto(
        id = id,
        name = name,
        bio = bio.ifEmpty { null },
        birthYear = birthYear.takeIf { it != 0 }
    )
}

fun PersonListResponse.toPage(): PageDto<PersonDto> {
    return PageDto(items = peopleList.map { it.toDto() }, totalCount = totalCount)
}

fun PersonInput.toProto(): SavePersonRequest {
    return SavePersonRequest.newBuilder()
        .setName(name)
        .setBio(bio ?: "")
        .setBirthYear(birthYear ?: 0)
        .build()
}

// ----- credits -----

fun Credit.toDto(): CreditDto {
    return CreditDto(
        id = id,
        movieId = movieId,
        role = role.toDto(),
        characterName = characterName.ifEmpty { null },
        person = person.toDto()
    )
}

fun CreditInput.toProto(): AddCreditRequest {
    return AddCreditRequest.newBuilder()
        .setMovieId(movieId)
        .setPersonId(personId)
        .setRole(role.toProto())
        .setCharacterName(characterName ?: "")
        .build()
}

fun Role.toProto(): ProtoRole {
    return when (this) {
        Role.ACTOR -> ProtoRole.ACTOR
        Role.DIRECTOR -> ProtoRole.DIRECTOR
        Role.WRITER -> ProtoRole.WRITER
        Role.PRODUCER -> ProtoRole.PRODUCER
    }
}

fun ProtoRole.toDto(): Role {
    return when (this) {
        ProtoRole.ACTOR -> Role.ACTOR
        ProtoRole.DIRECTOR -> Role.DIRECTOR
        ProtoRole.WRITER -> Role.WRITER
        ProtoRole.PRODUCER -> Role.PRODUCER
        else -> throw IllegalStateException("Unknown role from people-service: $this")
    }
}
