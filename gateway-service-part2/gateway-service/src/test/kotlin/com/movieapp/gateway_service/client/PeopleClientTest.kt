package com.movieapp.gateway_service.client

import com.movieapp.gateway_service.model.CreditInput
import com.movieapp.gateway_service.model.PersonInput
import com.movieapp.gateway_service.model.Role
import com.movieapp.grpc.people.AddCreditRequest
import com.movieapp.grpc.people.Credit
import com.movieapp.grpc.people.CreditListResponse
import com.movieapp.grpc.people.MovieCreditsRequest
import com.movieapp.grpc.people.PeopleServiceGrpc
import com.movieapp.grpc.people.Person
import com.movieapp.grpc.people.SavePersonRequest
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.movieapp.grpc.people.DeleteResponse as ProtoDeleteResponse
import com.movieapp.grpc.people.Role as ProtoRole

class PeopleClientTest {

    private lateinit var stub: PeopleServiceGrpc.PeopleServiceBlockingStub
    private lateinit var client: PeopleClient

    private val nolan = Person.newBuilder().setId(1L).setName("Christopher Nolan").setBirthYear(1970).build()

    @BeforeEach
    fun setUp() {
        stub = mockk()
        every { stub.withDeadlineAfter(any<Long>(), any<TimeUnit>()) } returns stub
        client = PeopleClient(stub, 5)
    }

    @Test
    fun `getPerson maps the person and changes empty bio to null`() {
        every { stub.getPerson(any()) } returns nolan

        val person = client.getPerson(1L)

        assertEquals("Christopher Nolan", person?.name)
        assertEquals(1970, person?.birthYear)
        assertNull(person?.bio)
    }

    @Test
    fun `getPerson returns null when people-service says NOT_FOUND`() {
        every { stub.getPerson(any()) } throws StatusRuntimeException(Status.NOT_FOUND)

        assertNull(client.getPerson(99L))
    }

    @Test
    fun `createPerson sends missing optional fields as protobuf defaults`() {
        val request = slot<SavePersonRequest>()
        every { stub.createPerson(capture(request)) } returns nolan

        client.createPerson(PersonInput(name = "Christopher Nolan"))

        assertEquals("Christopher Nolan", request.captured.name)
        assertEquals("", request.captured.bio)
        assertEquals(0, request.captured.birthYear)
    }

    @Test
    fun `getCreditsByMovie maps role, person and empty character name`() {
        val credit = Credit.newBuilder()
            .setId(5L)
            .setMovieId(10L)
            .setRole(ProtoRole.DIRECTOR)
            .setPerson(nolan)
            .build()
        every { stub.getCreditsByMovie(any()) } returns CreditListResponse.newBuilder().addCredits(credit).build()

        val credits = client.getCreditsByMovie(10L)

        assertEquals(1, credits.size)
        assertEquals(Role.DIRECTOR, credits[0].role)
        assertEquals("Christopher Nolan", credits[0].person.name)
        assertEquals(10L, credits[0].movieId)
        assertNull(credits[0].characterName)
    }

    @Test
    fun `addCredit sends every role with the matching protobuf role`() {
        val request = slot<AddCreditRequest>()

        for (role in Role.entries) {
            val response = Credit.newBuilder().setId(1L).setMovieId(10L).setRole(ProtoRole.valueOf(role.name)).setPerson(nolan).build()
            every { stub.addCredit(capture(request)) } returns response

            val credit = client.addCredit(CreditInput(movieId = 10L, personId = 1L, role = role))

            assertEquals(role.name, request.captured.role.name)
            assertEquals(role, credit.role)
        }
    }

    @Test
    fun `addCredit sends empty string when there is no character name`() {
        val request = slot<AddCreditRequest>()
        every { stub.addCredit(capture(request)) } returns
            Credit.newBuilder().setId(1L).setRole(ProtoRole.WRITER).setPerson(nolan).build()

        client.addCredit(CreditInput(movieId = 10L, personId = 1L, role = Role.WRITER))

        assertEquals("", request.captured.characterName)
    }

    @Test
    fun `deleteCreditsByMovie sends the movie id`() {
        val request = slot<MovieCreditsRequest>()
        every { stub.deleteCreditsByMovie(capture(request)) } returns
            ProtoDeleteResponse.newBuilder().setSuccess(true).build()

        client.deleteCreditsByMovie(10L)

        assertEquals(10L, request.captured.movieId)
    }
}
