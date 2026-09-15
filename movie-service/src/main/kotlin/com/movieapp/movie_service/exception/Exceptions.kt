package com.movieapp.movie_service.exception

class NotFoundException(message: String) : RuntimeException(message)

class ValidationException(message: String) : RuntimeException(message)
