package com.example.registry.repo

import com.example.registry.domain.entity.Person
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PersonRepository : JpaRepository<Person, Long> {
    fun findByTenantId(tenantId: Long): List<Person>
    fun findByIdAndTenantId(id: Long, tenantId: Long): Optional<Person>
}

