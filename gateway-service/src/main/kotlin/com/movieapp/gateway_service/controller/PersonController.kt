package com.movieapp.gateway_service.controller

import com.movieapp.gateway_service.client.PeopleClient
import com.movieapp.gateway_service.model.CreditDto
import com.movieapp.gateway_service.model.PageDto
import com.movieapp.gateway_service.model.PersonDto
import com.movieapp.gateway_service.model.PersonInput
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class PersonController(
    private val peopleClient: PeopleClient
) {

    @QueryMapping
    fun people(@Argument page: Int, @Argument size: Int): PageDto<PersonDto> {
        return peopleClient.listPeople(page, size)
    }

    @QueryMapping
    fun person(@Argument id: Long): PersonDto? {
        return peopleClient.getPerson(id)
    }

    @MutationMapping
    fun createPerson(@Argument input: PersonInput): PersonDto {
        return peopleClient.createPerson(input)
    }

    @MutationMapping
    fun updatePerson(@Argument id: Long, @Argument input: PersonInput): PersonDto {
        return peopleClient.updatePerson(id, input)
    }

    // credits of the person are removed by people-service itself (ON DELETE CASCADE)
    @MutationMapping
    fun deletePerson(@Argument id: Long): Boolean {
        peopleClient.deletePerson(id)
        return true
    }

    @SchemaMapping(typeName = "Person")
    fun credits(person: PersonDto): List<CreditDto> {
        return peopleClient.getCreditsByPerson(person.id)
    }
}
