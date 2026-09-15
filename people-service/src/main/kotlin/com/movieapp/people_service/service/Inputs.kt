package com.movieapp.people_service.service

import com.movieapp.people_service.entity.CreditRole

data class PersonInput(
    val name: String,
    val bio: String? = null,
    val birthYear: Int? = null
)

data class CreditInput(
    val movieId: Long,
    val personId: Long,
    val role: CreditRole?,
    val characterName: String? = null
)
