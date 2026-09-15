package com.movieapp.people_service.grpc

import com.movieapp.grpc.people.AddCreditRequest
import com.movieapp.grpc.people.Credit
import com.movieapp.grpc.people.CreditIdRequest
import com.movieapp.grpc.people.CreditListResponse
import com.movieapp.grpc.people.DeleteResponse
import com.movieapp.grpc.people.ListPeopleRequest
import com.movieapp.grpc.people.MovieCreditsRequest
import com.movieapp.grpc.people.PeopleServiceGrpc
import com.movieapp.grpc.people.Person
import com.movieapp.grpc.people.PersonIdRequest
import com.movieapp.grpc.people.PersonListResponse
import com.movieapp.grpc.people.SavePersonRequest
import com.movieapp.grpc.people.SearchPeopleRequest
import com.movieapp.grpc.people.UpdatePersonRequest
import com.movieapp.people_service.exception.AlreadyExistsException
import com.movieapp.people_service.exception.NotFoundException
import com.movieapp.people_service.exception.ValidationException
import com.movieapp.people_service.service.CreditService
import com.movieapp.people_service.service.PersonService
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PeopleGrpcService(
    private val personService: PersonService,
    private val creditService: CreditService
) : PeopleServiceGrpc.PeopleServiceImplBase() {

    private val log = LoggerFactory.getLogger(PeopleGrpcService::class.java)

    // ----- people -----

    override fun getPerson(request: PersonIdRequest, responseObserver: StreamObserver<Person>) {
        respond(responseObserver) { personService.getPerson(request.id).toProto() }
    }

    override fun listPeople(request: ListPeopleRequest, responseObserver: StreamObserver<PersonListResponse>) {
        respond(responseObserver) { personService.listPeople(request.page, request.size).toProto() }
    }

    override fun searchPeople(request: SearchPeopleRequest, responseObserver: StreamObserver<PersonListResponse>) {
        respond(responseObserver) {
            personService.searchPeople(request.query, request.page, request.size).toProto()
        }
    }

    override fun createPerson(request: SavePersonRequest, responseObserver: StreamObserver<Person>) {
        respond(responseObserver) { personService.createPerson(request.toInput()).toProto() }
    }

    override fun updatePerson(request: UpdatePersonRequest, responseObserver: StreamObserver<Person>) {
        respond(responseObserver) {
            personService.updatePerson(request.id, request.person.toInput()).toProto()
        }
    }

    override fun deletePerson(request: PersonIdRequest, responseObserver: StreamObserver<DeleteResponse>) {
        respond(responseObserver) {
            personService.deletePerson(request.id)
            DeleteResponse.newBuilder().setSuccess(true).build()
        }
    }

    // ----- credits -----

    override fun addCredit(request: AddCreditRequest, responseObserver: StreamObserver<Credit>) {
        respond(responseObserver) { creditService.addCredit(request.toInput()).toProto() }
    }

    override fun removeCredit(request: CreditIdRequest, responseObserver: StreamObserver<DeleteResponse>) {
        respond(responseObserver) {
            creditService.removeCredit(request.id)
            DeleteResponse.newBuilder().setSuccess(true).build()
        }
    }

    override fun getCreditsByMovie(request: MovieCreditsRequest, responseObserver: StreamObserver<CreditListResponse>) {
        respond(responseObserver) { creditService.getCreditsByMovie(request.movieId).toProto() }
    }

    override fun getCreditsByPerson(request: PersonIdRequest, responseObserver: StreamObserver<CreditListResponse>) {
        respond(responseObserver) { creditService.getCreditsByPerson(request.id).toProto() }
    }

    override fun deleteCreditsByMovie(request: MovieCreditsRequest, responseObserver: StreamObserver<DeleteResponse>) {
        respond(responseObserver) {
            val deleted = creditService.deleteCreditsByMovie(request.movieId)
            log.info("Deleted {} credits for movie {}", deleted, request.movieId)
            DeleteResponse.newBuilder().setSuccess(true).build()
        }
    }

    // send one response, or change our exceptions into gRPC status codes
    private fun <T> respond(observer: StreamObserver<T>, block: () -> T) {
        try {
            observer.onNext(block())
            observer.onCompleted()
        } catch (e: NotFoundException) {
            observer.onError(Status.NOT_FOUND.withDescription(e.message).asRuntimeException())
        } catch (e: ValidationException) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException())
        } catch (e: AlreadyExistsException) {
            observer.onError(Status.ALREADY_EXISTS.withDescription(e.message).asRuntimeException())
        } catch (e: Exception) {
            log.error("Unexpected error in people gRPC service", e)
            observer.onError(Status.INTERNAL.withDescription("Something went wrong").asRuntimeException())
        }
    }
}
