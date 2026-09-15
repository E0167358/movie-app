package com.movieapp.movie_service.repository

import com.movieapp.movie_service.entity.MovieEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MovieRepository : JpaRepository<MovieEntity, Long> {

    // search by title or genre, not case sensitive
    @Query(
        """
        SELECT m FROM MovieEntity m
        WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :query, '%'))
        """
    )
    fun search(@Param("query") query: String, pageable: Pageable): Page<MovieEntity>
}
