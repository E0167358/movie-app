package com.movieapp.people_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PeopleServiceApplication

fun main(args: Array<String>) {
	runApplication<PeopleServiceApplication>(*args)
}
