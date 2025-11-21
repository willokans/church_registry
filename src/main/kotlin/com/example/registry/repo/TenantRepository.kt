package com.example.registry.repo

import com.example.registry.domain.entity.Tenant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TenantRepository : JpaRepository<Tenant, Long> {
    fun findBySlug(slug: String): Tenant?
    fun existsBySlug(slug: String): Boolean
}

