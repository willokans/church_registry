package com.example.registry.repo

import com.example.registry.domain.entity.IdempotencyKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface IdempotencyKeyRepository : JpaRepository<IdempotencyKey, Long> {
    fun findByTenantIdAndKey(tenantId: Long, key: String): IdempotencyKey?
    
    @Modifying
    @Query("DELETE FROM IdempotencyKey i WHERE i.createdAt < :cutoff")
    fun deleteOlderThan(@Param("cutoff") cutoff: Instant): Int
}

