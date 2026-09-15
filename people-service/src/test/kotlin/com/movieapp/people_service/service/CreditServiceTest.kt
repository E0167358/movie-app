package com.movieapp.people_service.service

import com.movieapp.people_service.entity.CreditEntity
import com.movieapp.people_service.entity.CreditRole
import com.movieapp.people_service.entity.PersonEntity
import com.movieapp.people_service.exception.AlreadyExistsException
import com.movieapp.people_service.exception.NotFoundException
import com.movieapp.people_service.exception.ValidationException
import com.movieapp.people_service.repository.CreditRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CreditServiceTest {

    private lateinit var creditRepository: CreditRepository
    private lateinit var personService: PersonService
    private lateinit var creditService: CreditService

    private val nolan = PersonEntity(id = 1L, name = "Christopher Nolan")
    private val dicaprio = PersonEntity(id = 2L, name = "Leonardo DiCaprio")

    @BeforeEach
    fun setUp() {
        creditRepository = mockk(relaxUnitFun = true)
        personService = mockk()
        creditService = CreditService(creditRepository, personService)

        every { creditRepository.save(any<CreditEntity>()) } answers { firstArg() }
        every { creditRepository.existsByPersonIdAndMovieIdAndRole(any(), any(), any()) } returns false
        every { personService.getPerson(1L) } returns nolan
        every { personService.getPerson(2L) } returns dicaprio
        every { personService.getPerson(99L) } throws NotFoundException("Person with id 99 not found")
    }

    // ----- addCredit -----

    @Test
    fun `addCredit saves a credit with the person and role`() {
        val result = creditService.addCredit(CreditInput(movieId = 10L, personId = 1L, role = CreditRole.DIRECTOR))

        assertSame(nolan, result.person)
        assertEquals(10L, result.movieId)
        assertEquals(CreditRole.DIRECTOR, result.role)
        verify(exactly = 1) { creditRepository.save(any<CreditEntity>()) }
    }

    @Test
    fun `addCredit keeps the trimmed character name for an actor`() {
        val result = creditService.addCredit(
            CreditInput(movieId = 10L, personId = 2L, role = CreditRole.ACTOR, characterName = "  Cobb ")
        )

        assertEquals("Cobb", result.characterName)
    }

    @Test
    fun `addCredit ignores character name for roles that are not actor`() {
        val result = creditService.addCredit(
            CreditInput(movieId = 10L, personId = 1L, role = CreditRole.WRITER, characterName = "Cobb")
        )

        assertNull(result.characterName)
    }

    @Test
    fun `addCredit stores blank character name as null`() {
        val result = creditService.addCredit(
            CreditInput(movieId = 10L, personId = 2L, role = CreditRole.ACTOR, characterName = "   ")
        )

        assertNull(result.characterName)
    }

    @Test
    fun `addCredit throws when character name is longer than 255 characters`() {
        assertFailsWith<ValidationException> {
            creditService.addCredit(
                CreditInput(movieId = 10L, personId = 2L, role = CreditRole.ACTOR, characterName = "c".repeat(256))
            )
        }
        verify(exactly = 0) { creditRepository.save(any<CreditEntity>()) }
    }

    @Test
    fun `addCredit throws when role is missing`() {
        val error = assertFailsWith<ValidationException> {
            creditService.addCredit(CreditInput(movieId = 10L, personId = 1L, role = null))
        }

        assertEquals("Role is required", error.message)
        verify(exactly = 0) { creditRepository.save(any<CreditEntity>()) }
    }

    @Test
    fun `addCredit throws when movie id is zero or negative`() {
        for (movieId in listOf(0L, -5L)) {
            val error = assertFailsWith<ValidationException> {
                creditService.addCredit(CreditInput(movieId = movieId, personId = 1L, role = CreditRole.ACTOR))
            }
            assertEquals("Movie id must be a positive number", error.message)
        }
    }

    @Test
    fun `addCredit throws NotFoundException when person does not exist`() {
        assertFailsWith<NotFoundException> {
            creditService.addCredit(CreditInput(movieId = 10L, personId = 99L, role = CreditRole.ACTOR))
        }
        verify(exactly = 0) { creditRepository.save(any<CreditEntity>()) }
    }

    @Test
    fun `addCredit throws AlreadyExistsException for the same person, movie and role`() {
        every { creditRepository.existsByPersonIdAndMovieIdAndRole(1L, 10L, CreditRole.DIRECTOR) } returns true

        val error = assertFailsWith<AlreadyExistsException> {
            creditService.addCredit(CreditInput(movieId = 10L, personId = 1L, role = CreditRole.DIRECTOR))
        }

        assertEquals("Christopher Nolan is already added as director for this movie", error.message)
        verify(exactly = 0) { creditRepository.save(any<CreditEntity>()) }
    }

    @Test
    fun `addCredit allows the same person with a different role in the same movie`() {
        // Nolan is already the director, now we add him as writer
        every { creditRepository.existsByPersonIdAndMovieIdAndRole(1L, 10L, CreditRole.DIRECTOR) } returns true

        val result = creditService.addCredit(CreditInput(movieId = 10L, personId = 1L, role = CreditRole.WRITER))

        assertEquals(CreditRole.WRITER, result.role)
    }

    // ----- removeCredit -----

    @Test
    fun `removeCredit deletes an existing credit`() {
        val credit = CreditEntity(id = 5L, person = nolan, movieId = 10L, role = CreditRole.DIRECTOR)
        every { creditRepository.findById(5L) } returns Optional.of(credit)

        creditService.removeCredit(5L)

        verify { creditRepository.delete(credit) }
    }

    @Test
    fun `removeCredit throws NotFoundException when credit does not exist`() {
        every { creditRepository.findById(99L) } returns Optional.empty()

        val error = assertFailsWith<NotFoundException> { creditService.removeCredit(99L) }

        assertEquals("Credit with id 99 not found", error.message)
        verify(exactly = 0) { creditRepository.delete(any<CreditEntity>()) }
    }

    // ----- getCreditsByMovie -----

    @Test
    fun `getCreditsByMovie returns cast and creators of the movie`() {
        val credits = listOf(
            CreditEntity(id = 1L, person = dicaprio, movieId = 10L, role = CreditRole.ACTOR, characterName = "Cobb"),
            CreditEntity(id = 2L, person = nolan, movieId = 10L, role = CreditRole.DIRECTOR)
        )
        every { creditRepository.findByMovieIdWithPerson(10L) } returns credits

        val result = creditService.getCreditsByMovie(10L)

        assertEquals(2, result.size)
    }

    @Test
    fun `getCreditsByMovie returns empty list when movie has no credits`() {
        every { creditRepository.findByMovieIdWithPerson(10L) } returns emptyList()

        val result = creditService.getCreditsByMovie(10L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getCreditsByMovie throws when movie id is invalid`() {
        assertFailsWith<ValidationException> { creditService.getCreditsByMovie(0L) }

        verify(exactly = 0) { creditRepository.findByMovieIdWithPerson(any()) }
    }

    // ----- getCreditsByPerson -----

    @Test
    fun `getCreditsByPerson returns the credits of the person`() {
        val credits = listOf(CreditEntity(id = 1L, person = nolan, movieId = 10L, role = CreditRole.DIRECTOR))
        every { creditRepository.findByPersonIdWithPerson(1L) } returns credits

        val result = creditService.getCreditsByPerson(1L)

        assertEquals(1, result.size)
    }

    @Test
    fun `getCreditsByPerson throws NotFoundException when person does not exist`() {
        assertFailsWith<NotFoundException> { creditService.getCreditsByPerson(99L) }

        verify(exactly = 0) { creditRepository.findByPersonIdWithPerson(any()) }
    }

    // ----- deleteCreditsByMovie -----

    @Test
    fun `deleteCreditsByMovie returns how many credits were deleted`() {
        every { creditRepository.deleteByMovieId(10L) } returns 3

        assertEquals(3, creditService.deleteCreditsByMovie(10L))
    }

    @Test
    fun `deleteCreditsByMovie does not fail when movie has no credits`() {
        every { creditRepository.deleteByMovieId(10L) } returns 0

        assertEquals(0, creditService.deleteCreditsByMovie(10L))
    }

    @Test
    fun `deleteCreditsByMovie throws when movie id is invalid`() {
        assertFailsWith<ValidationException> { creditService.deleteCreditsByMovie(-1L) }

        verify(exactly = 0) { creditRepository.deleteByMovieId(any()) }
    }
}
