package com.example.registry.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "persons")
data class Person(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    val uuid: UUID = UUID.randomUUID(),
    
    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,
    
    @Column(name = "first_name", nullable = false, length = 100)
    val firstName: String,
    
    @Column(name = "last_name", nullable = false, length = 100)
    val lastName: String,
    
    @Column(name = "middle_name", length = 100)
    val middleName: String? = null,
    
    @Column(name = "date_of_birth")
    val dateOfBirth: LocalDate? = null,
    
    @Column(name = "gender", length = 20)
    val gender: String? = null, // MALE, FEMALE, OTHER
    
    @Column(name = "baptism_name", length = 100)
    val baptismName: String? = null, // Name given at baptism
    
    @Column(name = "father_name", length = 200)
    val fatherName: String? = null,
    
    @Column(name = "mother_name", length = 200)
    val motherName: String? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at")
    val updatedAt: Instant? = null
) {
    val fullName: String
        get() = listOfNotNull(firstName, middleName, lastName).joinToString(" ")
}

