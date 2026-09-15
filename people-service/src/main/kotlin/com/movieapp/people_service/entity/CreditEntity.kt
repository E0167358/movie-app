package com.movieapp.people_service.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

// ACTOR is cast, the others are creators
enum class CreditRole {
    ACTOR,
    DIRECTOR,
    WRITER,
    PRODUCER
}

@Entity
@Table(name = "credits")
class CreditEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    var person: PersonEntity,

    // just an id, the movie itself lives in movie-service
    @Column(nullable = false)
    var movieId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: CreditRole,

    var characterName: String? = null
)
