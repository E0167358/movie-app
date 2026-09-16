package com.movieapp.gateway_service.config

import com.movieapp.grpc.movie.MovieServiceGrpc
import com.movieapp.grpc.people.PeopleServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class GrpcClientConfig {

    // one channel per service. a channel keeps the connection open and is reused for all calls
    @Bean(destroyMethod = "shutdown")
    fun movieChannel(
        @Value("\${app.grpc.movie-service.host}") host: String,
        @Value("\${app.grpc.movie-service.port}") port: Int
    ): ManagedChannel {
        return ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext() // no TLS between our own services in local setup
            .build()
    }

    @Bean(destroyMethod = "shutdown")
    fun peopleChannel(
        @Value("\${app.grpc.people-service.host}") host: String,
        @Value("\${app.grpc.people-service.port}") port: Int
    ): ManagedChannel {
        return ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()
    }

    @Bean
    fun movieStub(@Qualifier("movieChannel") channel: ManagedChannel): MovieServiceGrpc.MovieServiceBlockingStub {
        return MovieServiceGrpc.newBlockingStub(channel)
    }

    @Bean
    fun peopleStub(@Qualifier("peopleChannel") channel: ManagedChannel): PeopleServiceGrpc.PeopleServiceBlockingStub {
        return PeopleServiceGrpc.newBlockingStub(channel)
    }

    // used by search to call both services at the same time.
    // virtual threads are cheap, so a blocking gRPC call per thread is fine
    @Bean
    fun searchExecutor(): ExecutorService {
        return Executors.newVirtualThreadPerTaskExecutor()
    }
}
