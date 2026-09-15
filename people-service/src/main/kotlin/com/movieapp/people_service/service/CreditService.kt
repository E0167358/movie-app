package com.movieapp.people_service.service

import com.movieapp.people_service.entity.CreditEntity
import com.movieapp.people_service.entity.CreditRole
import com.movieapp.people_service.exception.AlreadyExistsException
import com.movieapp.people_service.exception.NotFoundException
import com.movieapp.people_service.exception.ValidationException
import com.movieapp.people_service.repository.CreditRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreditService(
    private val creditRepository: CreditRepository,
    private val personService: PersonService
) {

    companion object {
        const val MAX_CHARACTER_NAME_LENGTH = 255
    }

    @Transactional
    fun addCredit(input: CreditInput): CreditEntity {
        validateMovieId(input.movieId)
        val role = input.role ?: throw ValidationException("Role is required")

        val characterName = input.characterName?.trim()?.ifEmpty { null }
        if (characterName != null && characterName.length > MAX_CHARACTER_NAME_LENGTH) {
            throw ValidationException("Character name must be 255 characters or less")
        }

        val person = personService.getPerson(input.personId)

        if (creditRepository.existsByPersonIdAndMovieIdAndRole(input.personId, input.movieId, role)) {
            throw AlreadyExistsException("${person.name} is already added as ${role.name.lowercase()} for this movie")
        }

        val credit = CreditEntity(
            person = person,
            movieId = input.movieId,
            role = role,
            // character name only makes sense for actors
            characterName = if (role == CreditRole.ACTOR) characterName else null
        )
        return creditRepository.save(credit)
    }

    @Transactional
    fun removeCredit(id: Long) {
        val credit = creditRepository.findById(id)
            .orElseThrow { NotFoundException("Credit with id $id not found") }
        creditRepository.delete(credit)
    }

    fun getCreditsByMovie(movieId: Long): List<CreditEntity> {
        validateMovieId(movieId)
        return creditRepository.findByMovieIdWithPerson(movieId)
    }

    fun getCreditsByPerson(personId: Long): List<CreditEntity> {
        // throws NotFoundException, so the caller can tell "no person" from "no credits"
        personService.getPerson(personId)
        return creditRepository.findByPersonIdWithPerson(personId)
    }

    // no error when the movie has no credits, so the gateway can safely call this again
    @Transactional
    fun deleteCreditsByMovie(movieId: Long): Int {
        validateMovieId(movieId)
        return creditRepository.deleteByMovieId(movieId)
    }

    private fun validateMovieId(movieId: Long) {
        if (movieId <= 0) {
            throw ValidationException("Movie id must be a positive number")
        }
    }
}
