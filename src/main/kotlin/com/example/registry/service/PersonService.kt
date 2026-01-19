package com.example.registry.service

import com.example.registry.domain.entity.Person
import com.example.registry.repo.PersonRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Service
class PersonService(
    private val personRepository: PersonRepository
) {
    
    @Transactional
    fun createPerson(
        tenantId: Long,
        firstName: String,
        lastName: String,
        middleName: String? = null,
        dateOfBirth: LocalDate? = null,
        gender: String? = null,
        baptismName: String? = null,
        fatherName: String? = null,
        motherName: String? = null
    ): Person {
        val person = Person(
            tenantId = tenantId,
            firstName = firstName,
            lastName = lastName,
            middleName = middleName,
            dateOfBirth = dateOfBirth,
            gender = gender,
            baptismName = baptismName,
            fatherName = fatherName,
            motherName = motherName
        )
        return personRepository.save(person)
    }
    
    fun findById(id: Long): Person? = personRepository.findById(id).orElse(null)
    
    fun findByTenantId(tenantId: Long): List<Person> = personRepository.findByTenantId(tenantId)
}

