package com.movieapp.people_service.service

import com.movieapp.people_service.entity.PersonEntity
import com.movieapp.people_service.exception.NotFoundException
import com.movieapp.people_service.exception.ValidationException
import com.movieapp.people_service.repository.PersonRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.Year

@Service
class PersonService(
    private val personRepository: PersonRepository
) {

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 100
        const val MAX_NAME_LENGTH = 255
        const val MAX_BIO_LENGTH = 5000
    }

    fun getPerson(id: Long): PersonEntity {
        return personRepository.findById(id)
            .orElseThrow { NotFoundException("Person with id $id not found") }
    }

    fun listPeople(page: Int, size: Int): Page<PersonEntity> {
        return personRepository.findAll(pageRequest(page, size))
    }

    fun searchPeople(query: String, page: Int, size: Int): Page<PersonEntity> {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            return listPeople(page, size)
        }
        return personRepository.findByNameContainingIgnoreCase(keyword, pageRequest(page, size))
    }

    @Transactional
    fun createPerson(input: PersonInput): PersonEntity {
        validate(input)
        val person = PersonEntity(
            name = input.name.trim(),
            bio = input.bio?.trim(),
            birthYear = input.birthYear
        )
        return personRepository.save(person)
    }

    @Transactional
    fun updatePerson(id: Long, input: PersonInput): PersonEntity {
        validate(input)
        val person = getPerson(id)
        person.name = input.name.trim()
        person.bio = input.bio?.trim()
        person.birthYear = input.birthYear
        person.updatedAt = LocalDateTime.now()
        return personRepository.save(person)
    }

    // the credits of this person are removed by ON DELETE CASCADE in the database
    @Transactional
    fun deletePerson(id: Long) {
        val person = getPerson(id)
        personRepository.delete(person)
    }

    private fun validate(input: PersonInput) {
        if (input.name.isBlank()) {
            throw ValidationException("Name is required")
        }
        if (input.name.trim().length > MAX_NAME_LENGTH) {
            throw ValidationException("Name must be 255 characters or less")
        }
        if (input.bio != null && input.bio.trim().length > MAX_BIO_LENGTH) {
            throw ValidationException("Bio must be 5000 characters or less")
        }
        // no real minimum, a writer like Shakespeare can still get a credit
        val currentYear = Year.now().value
        if (input.birthYear != null && (input.birthYear <= 0 || input.birthYear > currentYear)) {
            throw ValidationException("Birth year must be between 1 and $currentYear")
        }
    }

    private fun pageRequest(page: Int, size: Int): PageRequest {
        val safePage = if (page < 0) 0 else page
        val safeSize = when {
            size <= 0 -> DEFAULT_PAGE_SIZE
            size > MAX_PAGE_SIZE -> MAX_PAGE_SIZE
            else -> size
        }
        return PageRequest.of(safePage, safeSize, Sort.by("name"))
    }
}
