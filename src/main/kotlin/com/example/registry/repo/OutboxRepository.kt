package com.example.registry.repo

import com.example.registry.domain.entity.Outbox
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface OutboxRepository : JpaRepository<Outbox, Long> {
    @Query("""
        SELECT o FROM Outbox o 
        WHERE o.status = 'PENDING'
        AND (:tenantId IS NULL OR o.tenantId = :tenantId)
        AND o.createdAt <= :before
        ORDER BY o.createdAt ASC
    """)
    fun findPendingMessages(
        @Param("tenantId") tenantId: Long?,
        @Param("before") before: Instant,
        limit: org.springframework.data.domain.Pageable
    ): List<Outbox>
    
    fun findAllByStatus(status: String): List<Outbox>
}

