package com.movieapp.gateway_service.error

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.springframework.graphql.execution.ErrorType
import java.util.concurrent.CompletionException

data class ErrorInfo(
    val type: ErrorType,
    val message: String,
    // sent to the frontend in "extensions.code", so it can react to e.g. ALREADY_EXISTS
    val code: String
)

// kept separate from the Spring resolver so it is easy to unit test
object GraphQlErrorMapper {

    const val SERVICE_UNAVAILABLE_MESSAGE = "Service is not available right now, please try again"
    const val GENERIC_MESSAGE = "Something went wrong"

    fun map(ex: Throwable): ErrorInfo? {
        // errors from CompletableFuture (search) are wrapped in CompletionException
        val error = if (ex is CompletionException) ex.cause ?: ex else ex

        return when (error) {
            is NotFoundException -> ErrorInfo(ErrorType.NOT_FOUND, error.message ?: "Not found", "NOT_FOUND")
            is BadRequestException -> ErrorInfo(ErrorType.BAD_REQUEST, error.message ?: "Bad request", "INVALID_ARGUMENT")
            is StatusRuntimeException -> fromGrpcStatus(error.status)
            else -> null // not ours, let Spring GraphQL handle it
        }
    }

    private fun fromGrpcStatus(status: Status): ErrorInfo {
        val description = status.description ?: GENERIC_MESSAGE

        return when (status.code) {
            Status.Code.NOT_FOUND ->
                ErrorInfo(ErrorType.NOT_FOUND, description, "NOT_FOUND")

            Status.Code.INVALID_ARGUMENT ->
                ErrorInfo(ErrorType.BAD_REQUEST, description, "INVALID_ARGUMENT")

            Status.Code.ALREADY_EXISTS ->
                ErrorInfo(ErrorType.BAD_REQUEST, description, "ALREADY_EXISTS")

            Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED ->
                ErrorInfo(ErrorType.INTERNAL_ERROR, SERVICE_UNAVAILABLE_MESSAGE, "SERVICE_UNAVAILABLE")

            // don't show internal details to the user
            else ->
                ErrorInfo(ErrorType.INTERNAL_ERROR, GENERIC_MESSAGE, "INTERNAL")
        }
    }
}
