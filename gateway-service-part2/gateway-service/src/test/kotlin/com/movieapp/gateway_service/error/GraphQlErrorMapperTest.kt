package com.movieapp.gateway_service.error

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Test
import org.springframework.graphql.execution.ErrorType
import java.util.concurrent.CompletionException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GraphQlErrorMapperTest {

    private fun grpcError(status: Status) = StatusRuntimeException(status)

    @Test
    fun `NOT_FOUND keeps the message from the service`() {
        val info = GraphQlErrorMapper.map(grpcError(Status.NOT_FOUND.withDescription("Movie with id 9 not found")))

        assertNotNull(info)
        assertEquals(ErrorType.NOT_FOUND, info.type)
        assertEquals("Movie with id 9 not found", info.message)
        assertEquals("NOT_FOUND", info.code)
    }

    @Test
    fun `INVALID_ARGUMENT becomes BAD_REQUEST with the validation message`() {
        val info = GraphQlErrorMapper.map(grpcError(Status.INVALID_ARGUMENT.withDescription("Title is required")))

        assertEquals(ErrorType.BAD_REQUEST, info?.type)
        assertEquals("Title is required", info?.message)
        assertEquals("INVALID_ARGUMENT", info?.code)
    }

    @Test
    fun `ALREADY_EXISTS becomes BAD_REQUEST with its own code`() {
        val info = GraphQlErrorMapper.map(grpcError(Status.ALREADY_EXISTS.withDescription("already added")))

        assertEquals(ErrorType.BAD_REQUEST, info?.type)
        assertEquals("ALREADY_EXISTS", info?.code)
    }

    @Test
    fun `UNAVAILABLE and DEADLINE_EXCEEDED give a friendly message`() {
        for (status in listOf(Status.UNAVAILABLE, Status.DEADLINE_EXCEEDED)) {
            val info = GraphQlErrorMapper.map(grpcError(status.withDescription("io exception")))

            assertEquals(ErrorType.INTERNAL_ERROR, info?.type)
            assertEquals(GraphQlErrorMapper.SERVICE_UNAVAILABLE_MESSAGE, info?.message)
            assertEquals("SERVICE_UNAVAILABLE", info?.code)
        }
    }

    @Test
    fun `other gRPC errors hide the real message`() {
        val info = GraphQlErrorMapper.map(grpcError(Status.INTERNAL.withDescription("stack trace details")))

        assertEquals(ErrorType.INTERNAL_ERROR, info?.type)
        assertEquals(GraphQlErrorMapper.GENERIC_MESSAGE, info?.message)
    }

    @Test
    fun `status without description uses the generic message`() {
        val info = GraphQlErrorMapper.map(grpcError(Status.NOT_FOUND))

        assertEquals(GraphQlErrorMapper.GENERIC_MESSAGE, info?.message)
    }

    @Test
    fun `error wrapped in CompletionException is unwrapped`() {
        val wrapped = CompletionException(grpcError(Status.NOT_FOUND.withDescription("not here")))

        val info = GraphQlErrorMapper.map(wrapped)

        assertEquals(ErrorType.NOT_FOUND, info?.type)
        assertEquals("not here", info?.message)
    }

    @Test
    fun `gateway NotFoundException becomes NOT_FOUND`() {
        val info = GraphQlErrorMapper.map(NotFoundException("Movie with id 999 not found"))

        assertEquals(ErrorType.NOT_FOUND, info?.type)
        assertEquals("Movie with id 999 not found", info?.message)
    }

    @Test
    fun `gateway BadRequestException becomes BAD_REQUEST`() {
        val info = GraphQlErrorMapper.map(BadRequestException("Artwork data is not valid base64"))

        assertEquals(ErrorType.BAD_REQUEST, info?.type)
        assertEquals("Artwork data is not valid base64", info?.message)
        assertEquals("INVALID_ARGUMENT", info?.code)
    }

    @Test
    fun `unknown exceptions are left for Spring GraphQL to handle`() {
        assertNull(GraphQlErrorMapper.map(IllegalStateException("something else")))
    }
}
