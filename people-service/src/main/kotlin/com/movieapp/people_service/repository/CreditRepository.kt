package com.movieapp.people_service.repository

import com.movieapp.people_service.entity.CreditEntity
import com.movieapp.people_service.entity.CreditRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CreditRepository : JpaRepository<CreditEntity, Long> {

    // JOIN FETCH loads the person in the same query,
    // so we don't get one extra query for every credit
    @Query("SELECT c FROM CreditEntity c JOIN FETCH c.person WHERE c.movieId = :movieId ORDER BY c.role, c.id")
    fun findByMovieIdWithPerson(@Param("movieId") movieId: Long): List<CreditEntity>

    @Query("SELECT c FROM CreditEntity c JOIN FETCH c.person WHERE c.person.id = :personId ORDER BY c.movieId, c.role")
    fun findByPersonIdWithPerson(@Param("personId") personId: Long): List<CreditEntity>

    fun existsByPersonIdAndMovieIdAndRole(personId: Long, movieId: Long, role: CreditRole): Boolean

    @Modifying
    @Query("DELETE FROM CreditEntity c WHERE c.movieId = :movieId")
    fun deleteByMovieId(@Param("movieId") movieId: Long): Int
}
