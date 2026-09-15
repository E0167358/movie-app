package com.movieapp.people_service.exception

class NotFoundException(message: String) : RuntimeException(message)

class ValidationException(message: String) : RuntimeException(message)

class AlreadyExistsException(message: String) : RuntimeException(message)
