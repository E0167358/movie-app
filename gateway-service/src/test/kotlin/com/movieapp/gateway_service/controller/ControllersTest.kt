package com.movieapp.gateway_service.controller

import com.movieapp.gateway_service.client.MovieClient
import com.movieapp.gateway_service.client.PeopleClient
import com.movieapp.gateway_service.error.NotFoundException
import com.movieapp.gateway_service.model.CreditDto
import com.movieapp.gateway_service.model.CreditInput
import com.movieapp.gateway_service.model.MovieDto
import com.movieapp.gateway_service.model.PersonDto
import com.movieapp.gateway_service.model.Role
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ControllersTest {

    private val movieClient = mockk<MovieClient>()
    private val peopleClient = mockk<PeopleClient>()

    private val movieController = MovieController(movieClient, peopleClient)
    private val personController = PersonController(peopleClient)
    private val creditController = CreditController(movieClient, peopleClient)

    private val inception = MovieDto(1L, "Inception", null, 2010, "Sci-Fi", 148, hasArtwork = false)
    private val nolan = PersonDto(1L, "Christopher Nolan", null, 1970)
    private val directorCredit = CreditDto(5L, movieId = 1L, role = Role.DIRECTOR, characterName = null, person = nolan)

    // ----- MovieController -----

    @Test
    fun `movie returns null when the movie does not exist`() {
        every { movieClient.getMovie(99L) } returns null

        assertNull(movieController.movie(99L))
    }

    @Test
    fun `deleteMovie deletes the movie first and then its credits`() {
        justRun { movieClient.deleteMovie(1L) }
        justRun { peopleClient.deleteCreditsByMovie(1L) }

        val result = movieController.deleteMovie(1L)

        assertTrue(result)
        verifyOrder {
            movieClient.deleteMovie(1L)
            peopleClient.deleteCreditsByMovie(1L)
        }
    }

    @Test
    fun `deleteMovie still returns true when credits cannot be deleted`() {
        justRun { movieClient.deleteMovie(1L) }
        every { peopleClient.deleteCreditsByMovie(1L) } throws StatusRuntimeException(Status.UNAVAILABLE)

        assertTrue(movieController.deleteMovie(1L))
    }

    @Test
    fun `deleteMovie does not touch credits when the movie delete fails`() {
        every { movieClient.deleteMovie(99L) } throws StatusRuntimeException(Status.NOT_FOUND)

        assertFailsWith<StatusRuntimeException> { movieController.deleteMovie(99L) }

        verify(exactly = 0) { peopleClient.deleteCreditsByMovie(any()) }
    }

    @Test
    fun `movie credits are loaded from people-service with the movie id`() {
        every { peopleClient.getCreditsByMovie(1L) } returns listOf(directorCredit)

        val credits = movieController.credits(inception)

        assertEquals(listOf(directorCredit), credits)
    }

    // ----- PersonController -----

    @Test
    fun `deletePerson returns true`() {
        justRun { peopleClient.deletePerson(1L) }

        assertTrue(personController.deletePerson(1L))
    }

    @Test
    fun `person credits are loaded with the person id`() {
        every { peopleClient.getCreditsByPerson(1L) } returns listOf(directorCredit)

        assertEquals(1, personController.credits(nolan).size)
    }

    // ----- CreditController -----

    @Test
    fun `addCredit checks that the movie exists before adding`() {
        every { movieClient.getMovie(999L) } returns null

        val error = assertFailsWith<NotFoundException> {
            creditController.addCredit(CreditInput(movieId = 999L, personId = 1L, role = Role.ACTOR))
        }

        assertEquals("Movie with id 999 not found", error.message)
        verify(exactly = 0) { peopleClient.addCredit(any()) }
    }

    @Test
    fun `addCredit adds the credit when the movie exists`() {
        val input = CreditInput(movieId = 1L, personId = 1L, role = Role.DIRECTOR)
        every { movieClient.getMovie(1L) } returns inception
        every { peopleClient.addCredit(input) } returns directorCredit

        val credit = creditController.addCredit(input)

        assertEquals(5L, credit.id)
    }

    @Test
    fun `removeCredit returns true`() {
        justRun { peopleClient.removeCredit(5L) }

        assertTrue(creditController.removeCredit(5L))
    }

    @Test
    fun `credit movie is null when the movie was deleted`() {
        every { movieClient.getMovie(1L) } returns null

        assertNull(creditController.movie(directorCredit))
    }
}
