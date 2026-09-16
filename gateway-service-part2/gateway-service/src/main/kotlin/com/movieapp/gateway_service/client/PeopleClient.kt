package com.movieapp.gateway_service.client

import com.movieapp.gateway_service.model.CreditDto
import com.movieapp.gateway_service.model.CreditInput
import com.movieapp.gateway_service.model.PageDto
import com.movieapp.gateway_service.model.PersonDto
import com.movieapp.gateway_service.model.PersonInput
import com.movieapp.grpc.people.CreditIdRequest
import com.movieapp.grpc.people.ListPeopleRequest
import com.movieapp.grpc.people.MovieCreditsRequest
import com.movieapp.grpc.people.PeopleServiceGrpc
import com.movieapp.grpc.people.PersonIdRequest
import com.movieapp.grpc.people.SearchPeopleRequest
import com.movieapp.grpc.people.UpdatePersonRequest
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class PeopleClient(
    private val stub: PeopleServiceGrpc.PeopleServiceBlockingStub,
    @Value("\${app.grpc.deadline-seconds}") private val deadlineSeconds: Long
) {

    private fun stub() = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)

    // ----- people -----

    fun getPerson(id: Long): PersonDto? {
        return try {
            stub().getPerson(PersonIdRequest.newBuilder().setId(id).build()).toDto()
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.Code.NOT_FOUND) null else throw e
        }
    }

    fun listPeople(page: Int, size: Int): PageDto<PersonDto> {
        val request = ListPeopleRequest.newBuilder().setPage(page).setSize(size).build()
        return stub().listPeople(request).toPage()
    }

    fun searchPeople(query: String, page: Int, size: Int): PageDto<PersonDto> {
        val request = SearchPeopleRequest.newBuilder().setQuery(query).setPage(page).setSize(size).build()
        return stub().searchPeople(request).toPage()
    }

    fun createPerson(input: PersonInput): PersonDto {
        return stub().createPerson(input.toProto()).toDto()
    }

    fun updatePerson(id: Long, input: PersonInput): PersonDto {
        val request = UpdatePersonRequest.newBuilder().setId(id).setPerson(input.toProto()).build()
        return stub().updatePerson(request).toDto()
    }

    fun deletePerson(id: Long) {
        stub().deletePerson(PersonIdRequest.newBuilder().setId(id).build())
    }

    // ----- credits -----

    fun addCredit(input: CreditInput): CreditDto {
        return stub().addCredit(input.toProto()).toDto()
    }

    fun removeCredit(id: Long) {
        stub().removeCredit(CreditIdRequest.newBuilder().setId(id).build())
    }

    fun getCreditsByMovie(movieId: Long): List<CreditDto> {
        val request = MovieCreditsRequest.newBuilder().setMovieId(movieId).build()
        return stub().getCreditsByMovie(request).creditsList.map { it.toDto() }
    }

    fun getCreditsByPerson(personId: Long): List<CreditDto> {
        val request = PersonIdRequest.newBuilder().setId(personId).build()
        return stub().getCreditsByPerson(request).creditsList.map { it.toDto() }
    }

    fun deleteCreditsByMovie(movieId: Long) {
        stub().deleteCreditsByMovie(MovieCreditsRequest.newBuilder().setMovieId(movieId).build())
    }
}
