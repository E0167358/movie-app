package com.movieapp.people_service.grpc

import com.movieapp.grpc.people.AddCreditRequest
import com.movieapp.grpc.people.Credit
import com.movieapp.grpc.people.CreditListResponse
import com.movieapp.grpc.people.Person
import com.movieapp.grpc.people.PersonListResponse
import com.movieapp.grpc.people.Role
import com.movieapp.grpc.people.SavePersonRequest
import com.movieapp.people_service.entity.CreditEntity
import com.movieapp.people_service.entity.CreditRole
import com.movieapp.people_service.entity.PersonEntity
import com.movieapp.people_service.service.CreditInput
import com.movieapp.people_service.service.PersonInput
import org.springframework.data.domain.Page

// protobuf has no null, so null becomes 0 or empty string (and the other way round)

fun PersonEntity.toProto(): Person {
    return Person.newBuilder()
        .setId(id ?: 0L)
        .setName(name)
        .setBio(bio ?: "")
        .setBirthYear(birthYear ?: 0)
        .build()
}

fun Page<PersonEntity>.toProto(): PersonListResponse {
    return PersonListResponse.newBuilder()
        .addAllPeople(content.map { it.toProto() })
        .setTotalCount(totalElements)
        .build()
}

fun SavePersonRequest.toInput(): PersonInput {
    return PersonInput(
        name = name,
        bio = bio.ifBlank { null },
        birthYear = if (birthYear == 0) null else birthYear
    )
}

fun CreditEntity.toProto(): Credit {
    return Credit.newBuilder()
        .setId(id ?: 0L)
        .setMovieId(movieId)
        .setRole(role.toProto())
        .setCharacterName(characterName ?: "")
        .setPerson(person.toProto())
        .build()
}

fun List<CreditEntity>.toProto(): CreditListResponse {
    return CreditListResponse.newBuilder()
        .addAllCredits(map { it.toProto() })
        .build()
}

fun AddCreditRequest.toInput(): CreditInput {
    return CreditInput(
        movieId = movieId,
        personId = personId,
        role = role.toCreditRole(),
        characterName = characterName.ifBlank { null }
    )
}

fun CreditRole.toProto(): Role {
    return when (this) {
        CreditRole.ACTOR -> Role.ACTOR
        CreditRole.DIRECTOR -> Role.DIRECTOR
        CreditRole.WRITER -> Role.WRITER
        CreditRole.PRODUCER -> Role.PRODUCER
    }
}

// ROLE_UNSPECIFIED means the client did not choose a role
fun Role.toCreditRole(): CreditRole? {
    return when (this) {
        Role.ACTOR -> CreditRole.ACTOR
        Role.DIRECTOR -> CreditRole.DIRECTOR
        Role.WRITER -> CreditRole.WRITER
        Role.PRODUCER -> CreditRole.PRODUCER
        else -> null
    }
}
