package com.movieapp.people_service.service

import com.movieapp.people_service.entity.PersonEntity
import com.movieapp.people_service.exception.NotFoundException
import com.movieapp.people_service.exception.ValidationException
import com.movieapp.people_service.repository.PersonRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.time.Year
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PersonServiceTest {

    private lateinit var personRepository: PersonRepository
    private lateinit var personService: PersonService

    @BeforeEach
    fun setUp() {
        personRepository = mockk(relaxUnitFun = true)
        personService = PersonService(personRepository)

        every { personRepository.save(any<PersonEntity>()) } answers { firstArg() }
    }

    private fun person(id: Long = 1L) = PersonEntity(
        id = id,
        name = "Christopher Nolan",
        bio = "British-American director",
        birthYear = 1970
    )

    // ----- getPerson -----

    @Test
    fun `getPerson returns the person when it exists`() {
        every { personRepository.findById(1L) } returns Optional.of(person())

        val result = personService.getPerson(1L)

        assertEquals("Christopher Nolan", result.name)
    }

    @Test
    fun `getPerson throws NotFoundException when person does not exist`() {
        every { personRepository.findById(99L) } returns Optional.empty()

        val error = assertFailsWith<NotFoundException> { personService.getPerson(99L) }

        assertEquals("Person with id 99 not found", error.message)
    }

    // ----- listPeople and paging -----

    @Test
    fun `listPeople uses page and size and sorts by name`() {
        val pageable = slot<Pageable>()
        every { personRepository.findAll(capture(pageable)) } returns PageImpl(listOf(person()))

        val result = personService.listPeople(1, 15)

        assertEquals(1, pageable.captured.pageNumber)
        assertEquals(15, pageable.captured.pageSize)
        assertEquals(Sort.by("name"), pageable.captured.sort)
        assertEquals(1, result.content.size)
    }

    @Test
    fun `listPeople uses page 0 when page is negative`() {
        val pageable = slot<Pageable>()
        every { personRepository.findAll(capture(pageable)) } returns PageImpl(emptyList<PersonEntity>())

        personService.listPeople(-1, 10)

        assertEquals(0, pageable.captured.pageNumber)
    }

    @Test
    fun `listPeople uses default size when size is 0`() {
        val pageable = slot<Pageable>()
        every { personRepository.findAll(capture(pageable)) } returns PageImpl(emptyList<PersonEntity>())

        personService.listPeople(0, 0)

        assertEquals(PersonService.DEFAULT_PAGE_SIZE, pageable.captured.pageSize)
    }

    @Test
    fun `listPeople limits size to the maximum`() {
        val pageable = slot<Pageable>()
        every { personRepository.findAll(capture(pageable)) } returns PageImpl(emptyList<PersonEntity>())

        personService.listPeople(0, 5000)

        assertEquals(PersonService.MAX_PAGE_SIZE, pageable.captured.pageSize)
    }

    // ----- searchPeople -----

    @Test
    fun `searchPeople trims the keyword before searching`() {
        val keyword = slot<String>()
        every { personRepository.findByNameContainingIgnoreCase(capture(keyword), any()) } returns
            PageImpl(listOf(person()))

        val result = personService.searchPeople("  nolan ", 0, 10)

        assertEquals("nolan", keyword.captured)
        assertEquals(1, result.content.size)
    }

    @Test
    fun `searchPeople with blank query returns the normal list`() {
        every { personRepository.findAll(any<Pageable>()) } returns PageImpl(listOf(person()))

        personService.searchPeople("", 0, 10)

        verify(exactly = 1) { personRepository.findAll(any<Pageable>()) }
        verify(exactly = 0) { personRepository.findByNameContainingIgnoreCase(any(), any()) }
    }

    // ----- createPerson -----

    @Test
    fun `createPerson trims text and saves the person`() {
        val result = personService.createPerson(
            PersonInput(name = "  Christopher Nolan ", bio = " Director ", birthYear = 1970)
        )

        assertEquals("Christopher Nolan", result.name)
        assertEquals("Director", result.bio)
        assertEquals(1970, result.birthYear)
        verify(exactly = 1) { personRepository.save(any<PersonEntity>()) }
    }

    @Test
    fun `createPerson works with only a name`() {
        val result = personService.createPerson(PersonInput(name = "Zendaya"))

        assertEquals("Zendaya", result.name)
        assertNull(result.bio)
        assertNull(result.birthYear)
    }

    @Test
    fun `createPerson throws when name is blank`() {
        for (name in listOf("", "    ")) {
            val error = assertFailsWith<ValidationException> {
                personService.createPerson(PersonInput(name = name))
            }
            assertEquals("Name is required", error.message)
        }
        verify(exactly = 0) { personRepository.save(any<PersonEntity>()) }
    }

    @Test
    fun `createPerson accepts a name with exactly 255 characters`() {
        val name = "n".repeat(255)

        val result = personService.createPerson(PersonInput(name = name))

        assertEquals(name, result.name)
    }

    @Test
    fun `createPerson throws when name is longer than 255 characters`() {
        assertFailsWith<ValidationException> {
            personService.createPerson(PersonInput(name = "n".repeat(256)))
        }
    }

    @Test
    fun `createPerson accepts a bio with exactly 5000 characters`() {
        val result = personService.createPerson(PersonInput(name = "Zendaya", bio = "b".repeat(5000)))

        assertEquals(5000, result.bio?.length)
    }

    @Test
    fun `createPerson throws when bio is longer than 5000 characters`() {
        val error = assertFailsWith<ValidationException> {
            personService.createPerson(PersonInput(name = "Zendaya", bio = "b".repeat(5001)))
        }

        assertEquals("Bio must be 5000 characters or less", error.message)
    }

    @Test
    fun `createPerson accepts a very old birth year for classic writers`() {
        val result = personService.createPerson(PersonInput(name = "William Shakespeare", birthYear = 1564))

        assertEquals(1564, result.birthYear)
    }

    @Test
    fun `createPerson accepts the current year as birth year`() {
        val year = Year.now().value

        val result = personService.createPerson(PersonInput(name = "Baby actor", birthYear = year))

        assertEquals(year, result.birthYear)
    }

    @Test
    fun `createPerson throws when birth year is in the future`() {
        assertFailsWith<ValidationException> {
            personService.createPerson(PersonInput(name = "Not born", birthYear = Year.now().value + 1))
        }
    }

    @Test
    fun `createPerson throws when birth year is zero or negative`() {
        for (year in listOf(0, -1)) {
            assertFailsWith<ValidationException> {
                personService.createPerson(PersonInput(name = "Someone", birthYear = year))
            }
        }
    }

    // ----- updatePerson -----

    @Test
    fun `updatePerson changes the fields of an existing person`() {
        every { personRepository.findById(1L) } returns Optional.of(person())

        val result = personService.updatePerson(1L, PersonInput(name = "Emma Thomas", birthYear = 1971))

        assertEquals(1L, result.id)
        assertEquals("Emma Thomas", result.name)
        assertEquals(1971, result.birthYear)
        // full replace, so bio that was not sent is cleared
        assertNull(result.bio)
    }

    @Test
    fun `updatePerson throws NotFoundException when person does not exist`() {
        every { personRepository.findById(99L) } returns Optional.empty()

        assertFailsWith<NotFoundException> {
            personService.updatePerson(99L, PersonInput(name = "Someone"))
        }
        verify(exactly = 0) { personRepository.save(any<PersonEntity>()) }
    }

    @Test
    fun `updatePerson validates input before looking up the person`() {
        assertFailsWith<ValidationException> {
            personService.updatePerson(1L, PersonInput(name = ""))
        }
        verify(exactly = 0) { personRepository.findById(any()) }
    }

    // ----- deletePerson -----

    @Test
    fun `deletePerson deletes an existing person`() {
        val existing = person()
        every { personRepository.findById(1L) } returns Optional.of(existing)

        personService.deletePerson(1L)

        verify { personRepository.delete(existing) }
    }

    @Test
    fun `deletePerson throws NotFoundException when person does not exist`() {
        every { personRepository.findById(99L) } returns Optional.empty()

        assertFailsWith<NotFoundException> { personService.deletePerson(99L) }

        verify(exactly = 0) { personRepository.delete(any<PersonEntity>()) }
    }
}
