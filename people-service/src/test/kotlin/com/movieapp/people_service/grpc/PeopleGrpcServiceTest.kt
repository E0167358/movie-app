package com.movieapp.people_service.grpc

import com.movieapp.grpc.people.AddCreditRequest
import com.movieapp.grpc.people.Credit
import com.movieapp.grpc.people.CreditIdRequest
import com.movieapp.grpc.people.CreditListResponse
import com.movieapp.grpc.people.DeleteResponse
import com.movieapp.grpc.people.ListPeopleRequest
import com.movieapp.grpc.people.MovieCreditsRequest
import com.movieapp.grpc.people.Person
import com.movieapp.grpc.people.PersonIdRequest
import com.movieapp.grpc.people.PersonListResponse
import com.movieapp.grpc.people.Role
import com.movieapp.grpc.people.SavePersonRequest
import com.movieapp.grpc.people.UpdatePersonRequest
import com.movieapp.people_service.entity.CreditEntity
import com.movieapp.people_service.entity.CreditRole
import com.movieapp.people_service.entity.PersonEntity
import com.movieapp.people_service.exception.AlreadyExistsException
import com.movieapp.people_service.exception.NotFoundException
import com.movieapp.people_service.exception.ValidationException
import com.movieapp.people_service.service.CreditInput
import com.movieapp.people_service.service.CreditService
import com.movieapp.people_service.service.PersonInput
import com.movieapp.people_service.service.PersonService
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeopleGrpcServiceTest {

    private lateinit var personService: PersonService
    private lateinit var creditService: CreditService
    private lateinit var grpcService: PeopleGrpcService

    private val nolan = PersonEntity(id = 1L, name = "Christopher Nolan", birthYear = 1970)

    @BeforeEach
    fun setUp() {
        personService = mockk()
        creditService = mockk()
        grpcService = PeopleGrpcService(personService, creditService)
    }

    private class TestObserver<T> : StreamObserver<T> {
        val values = mutableListOf<T>()
        var error: Throwable? = null
        var completed = false

        override fun onNext(value: T) {
            values.add(value)
        }

        override fun onError(t: Throwable) {
            error = t
        }

        override fun onCompleted() {
            completed = true
        }

        fun statusCode(): Status.Code? = error?.let { Status.fromThrowable(it).code }

        fun statusMessage(): String? = error?.let { Status.fromThrowable(it).description }
    }

    // ----- people -----

    @Test
    fun `getPerson sends the person and completes`() {
        every { personService.getPerson(1L) } returns nolan
        val observer = TestObserver<Person>()

        grpcService.getPerson(PersonIdRequest.newBuilder().setId(1L).build(), observer)

        assertEquals("Christopher Nolan", observer.values[0].name)
        assertEquals(1970, observer.values[0].birthYear)
        assertTrue(observer.completed)
    }

    @Test
    fun `listPeople returns people and total count`() {
        every { personService.listPeople(0, 10) } returns PageImpl(listOf(nolan), PageRequest.of(0, 10), 25)
        val observer = TestObserver<PersonListResponse>()

        grpcService.listPeople(ListPeopleRequest.newBuilder().setPage(0).setSize(10).build(), observer)

        assertEquals(1, observer.values[0].peopleCount)
        assertEquals(25L, observer.values[0].totalCount)
    }

    @Test
    fun `createPerson treats 0 and empty strings as not provided`() {
        val input = slot<PersonInput>()
        every { personService.createPerson(capture(input)) } returns nolan

        grpcService.createPerson(SavePersonRequest.newBuilder().setName("Christopher Nolan").build(), TestObserver())

        assertEquals("Christopher Nolan", input.captured.name)
        assertNull(input.captured.bio)
        assertNull(input.captured.birthYear)
    }

    @Test
    fun `updatePerson passes id and fields to the service`() {
        val input = slot<PersonInput>()
        every { personService.updatePerson(1L, capture(input)) } returns nolan
        val person = SavePersonRequest.newBuilder().setName("Chris Nolan").setBirthYear(1970).build()

        grpcService.updatePerson(UpdatePersonRequest.newBuilder().setId(1L).setPerson(person).build(), TestObserver())

        assertEquals("Chris Nolan", input.captured.name)
        assertEquals(1970, input.captured.birthYear)
    }

    @Test
    fun `deletePerson returns success`() {
        justRun { personService.deletePerson(1L) }
        val observer = TestObserver<DeleteResponse>()

        grpcService.deletePerson(PersonIdRequest.newBuilder().setId(1L).build(), observer)

        assertTrue(observer.values[0].success)
    }

    // ----- credits -----

    @Test
    fun `addCredit returns the credit with person and role`() {
        val credit = CreditEntity(id = 3L, person = nolan, movieId = 10L, role = CreditRole.DIRECTOR)
        every { creditService.addCredit(any()) } returns credit
        val observer = TestObserver<Credit>()
        val request = AddCreditRequest.newBuilder().setMovieId(10L).setPersonId(1L).setRole(Role.DIRECTOR).build()

        grpcService.addCredit(request, observer)

        val result = observer.values[0]
        assertEquals(3L, result.id)
        assertEquals(Role.DIRECTOR, result.role)
        assertEquals("Christopher Nolan", result.person.name)
        assertTrue(observer.completed)
    }

    @Test
    fun `addCredit sends null role to the service when role is not set`() {
        val input = slot<CreditInput>()
        every { creditService.addCredit(capture(input)) } throws ValidationException("Role is required")
        val observer = TestObserver<Credit>()

        grpcService.addCredit(AddCreditRequest.newBuilder().setMovieId(10L).setPersonId(1L).build(), observer)

        assertNull(input.captured.role)
        assertEquals(Status.Code.INVALID_ARGUMENT, observer.statusCode())
        assertEquals("Role is required", observer.statusMessage())
    }

    @Test
    fun `addCredit returns ALREADY_EXISTS for a duplicate credit`() {
        every { creditService.addCredit(any()) } throws AlreadyExistsException("already added")
        val observer = TestObserver<Credit>()
        val request = AddCreditRequest.newBuilder().setMovieId(10L).setPersonId(1L).setRole(Role.ACTOR).build()

        grpcService.addCredit(request, observer)

        assertEquals(Status.Code.ALREADY_EXISTS, observer.statusCode())
        assertFalse(observer.completed)
    }

    @Test
    fun `removeCredit returns NOT_FOUND when credit does not exist`() {
        every { creditService.removeCredit(99L) } throws NotFoundException("Credit with id 99 not found")
        val observer = TestObserver<DeleteResponse>()

        grpcService.removeCredit(CreditIdRequest.newBuilder().setId(99L).build(), observer)

        assertEquals(Status.Code.NOT_FOUND, observer.statusCode())
        assertEquals("Credit with id 99 not found", observer.statusMessage())
    }

    @Test
    fun `getCreditsByMovie returns all credits`() {
        val credits = listOf(
            CreditEntity(id = 1L, person = nolan, movieId = 10L, role = CreditRole.DIRECTOR),
            CreditEntity(id = 2L, person = nolan, movieId = 10L, role = CreditRole.WRITER)
        )
        every { creditService.getCreditsByMovie(10L) } returns credits
        val observer = TestObserver<CreditListResponse>()

        grpcService.getCreditsByMovie(MovieCreditsRequest.newBuilder().setMovieId(10L).build(), observer)

        assertEquals(2, observer.values[0].creditsCount)
    }

    @Test
    fun `deleteCreditsByMovie returns success`() {
        every { creditService.deleteCreditsByMovie(10L) } returns 2
        val observer = TestObserver<DeleteResponse>()

        grpcService.deleteCreditsByMovie(MovieCreditsRequest.newBuilder().setMovieId(10L).build(), observer)

        assertTrue(observer.values[0].success)
        assertTrue(observer.completed)
    }

    // ----- error mapping -----

    @Test
    fun `unexpected exception becomes INTERNAL status without showing the real message`() {
        every { personService.getPerson(1L) } throws RuntimeException("connection details")
        val observer = TestObserver<Person>()

        grpcService.getPerson(PersonIdRequest.newBuilder().setId(1L).build(), observer)

        assertEquals(Status.Code.INTERNAL, observer.statusCode())
        assertEquals("Something went wrong", observer.statusMessage())
    }
}
