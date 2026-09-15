package com.movieapp.people_service.grpc

import com.movieapp.grpc.people.AddCreditRequest
import com.movieapp.grpc.people.Role
import com.movieapp.grpc.people.SavePersonRequest
import com.movieapp.people_service.entity.CreditEntity
import com.movieapp.people_service.entity.CreditRole
import com.movieapp.people_service.entity.PersonEntity
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PeopleMappersTest {

    @Test
    fun `person toProto changes null fields to default values`() {
        val proto = PersonEntity(id = 1L, name = "Zendaya").toProto()

        assertEquals(1L, proto.id)
        assertEquals("Zendaya", proto.name)
        assertEquals("", proto.bio)
        assertEquals(0, proto.birthYear)
    }

    @Test
    fun `person toInput changes 0 and blank strings to null`() {
        val input = SavePersonRequest.newBuilder().setName("Zendaya").setBio("  ").build().toInput()

        assertEquals("Zendaya", input.name)
        assertNull(input.bio)
        assertNull(input.birthYear)
    }

    @Test
    fun `credit toProto includes person, role and character name`() {
        val person = PersonEntity(id = 2L, name = "Leonardo DiCaprio")
        val credit = CreditEntity(id = 5L, person = person, movieId = 10L, role = CreditRole.ACTOR, characterName = "Cobb")

        val proto = credit.toProto()

        assertEquals(5L, proto.id)
        assertEquals(10L, proto.movieId)
        assertEquals(Role.ACTOR, proto.role)
        assertEquals("Cobb", proto.characterName)
        assertEquals("Leonardo DiCaprio", proto.person.name)
    }

    @Test
    fun `credit toProto uses empty string when there is no character name`() {
        val credit = CreditEntity(person = PersonEntity(id = 1L, name = "Nolan"), movieId = 10L, role = CreditRole.DIRECTOR)

        assertEquals("", credit.toProto().characterName)
    }

    @Test
    fun `every role converts to protobuf and back`() {
        for (role in CreditRole.entries) {
            assertEquals(role, role.toProto().toCreditRole())
        }
    }

    @Test
    fun `ROLE_UNSPECIFIED converts to null`() {
        assertNull(Role.ROLE_UNSPECIFIED.toCreditRole())
    }

    @Test
    fun `addCredit request toInput changes blank character name to null`() {
        val request = AddCreditRequest.newBuilder()
            .setMovieId(10L)
            .setPersonId(1L)
            .setRole(Role.ACTOR)
            .setCharacterName("")
            .build()

        val input = request.toInput()

        assertEquals(10L, input.movieId)
        assertEquals(1L, input.personId)
        assertEquals(CreditRole.ACTOR, input.role)
        assertNull(input.characterName)
    }
}
