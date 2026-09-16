package com.movieapp.gateway_service.controller

import com.movieapp.gateway_service.client.MovieClient
import com.movieapp.gateway_service.client.PeopleClient
import com.movieapp.gateway_service.model.MovieDto
import com.movieapp.gateway_service.model.PageDto
import com.movieapp.gateway_service.model.PersonDto
import com.movieapp.gateway_service.model.SearchParams
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SearchControllerTest {

    private val movieClient = mockk<MovieClient>()
    private val peopleClient = mockk<PeopleClient>()

    // counts how many tasks were given to the executor, and runs them right away
    private val tasksStarted = AtomicInteger()
    private val executor = Executor { task ->
        tasksStarted.incrementAndGet()
        task.run()
    }

    private val controller = SearchController(movieClient, peopleClient, executor)

    private val inception = MovieDto(1L, "Inception", null, 2010, null, null, hasArtwork = false)
    private val nolan = PersonDto(1L, "Christopher Nolan", null, 1970)

    @Test
    fun `search trims the query and keeps the size`() {
        val params = controller.search("  nolan  ", 5)

        assertEquals("nolan", params.query)
        assertEquals(5, params.size)
    }

    @Test
    fun `movies and people are searched with the same query and size`() {
        every { movieClient.searchMovies("nolan", 0, 5) } returns PageDto(listOf(inception), 1)
        every { peopleClient.searchPeople("nolan", 0, 5) } returns PageDto(listOf(nolan), 1)
        val params = SearchParams("nolan", 5)

        val movies = controller.movies(params).get()
        val people = controller.people(params).get()

        assertEquals("Inception", movies.items[0].title)
        assertEquals("Christopher Nolan", people.items[0].name)
    }

    @Test
    fun `both searches run on the executor`() {
        every { movieClient.searchMovies(any(), any(), any()) } returns PageDto(emptyList(), 0)
        every { peopleClient.searchPeople(any(), any(), any()) } returns PageDto(emptyList(), 0)
        val params = SearchParams("x", 10)

        controller.movies(params).get()
        controller.people(params).get()

        assertEquals(2, tasksStarted.get())
    }

    @Test
    fun `people search failing does not break the movie search`() {
        every { movieClient.searchMovies("nolan", 0, 10) } returns PageDto(listOf(inception), 1)
        every { peopleClient.searchPeople("nolan", 0, 10) } throws StatusRuntimeException(Status.UNAVAILABLE)
        val params = SearchParams("nolan", 10)

        val movies = controller.movies(params)
        val people = controller.people(params)

        assertEquals(1, movies.get().items.size)
        val error = assertFailsWith<ExecutionException> { people.get() }
        assertTrue(error.cause is StatusRuntimeException)
    }

    @Test
    fun `search returns empty pages when nothing matches`() {
        every { movieClient.searchMovies("zzz", 0, 10) } returns PageDto(emptyList(), 0)
        every { peopleClient.searchPeople("zzz", 0, 10) } returns PageDto(emptyList(), 0)
        val params = SearchParams("zzz", 10)

        assertEquals(0L, controller.movies(params).get().totalCount)
        assertEquals(0L, controller.people(params).get().totalCount)
    }
}
