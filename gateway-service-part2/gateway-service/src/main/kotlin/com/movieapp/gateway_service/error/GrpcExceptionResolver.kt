package com.movieapp.gateway_service.error

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.slf4j.LoggerFactory
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

// turns gRPC errors from the services into readable GraphQL errors
@Component
class GrpcExceptionResolver : DataFetcherExceptionResolverAdapter() {

    private val log = LoggerFactory.getLogger(GrpcExceptionResolver::class.java)

    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
        val info = GraphQlErrorMapper.map(ex) ?: return null

        if (info.type == ErrorType.INTERNAL_ERROR) {
            log.error("Error while calling a backend service", ex)
        }

        return GraphqlErrorBuilder.newError(env)
            .errorType(info.type)
            .message(info.message)
            .extensions(mapOf("code" to info.code))
            .build()
    }
}
