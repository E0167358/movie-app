package com.movieapp.people_service.repository

import com.movieapp.people_service.entity.PersonEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PersonRepository : JpaRepository<PersonEntity, Long> {

    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<PersonEntity>
}
