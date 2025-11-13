package com.example.registry.repo

import com.example.registry.domain.entity.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE (:tenantId IS NULL OR a.tenantId = :tenantId)
        AND (:actorId IS NULL OR a.actorId = :actorId)
        AND (:entity IS NULL OR a.entity = :entity)
        AND (:entityId IS NULL OR a.entityId = :entityId)
        AND (:fromTs IS NULL OR a.ts >= :fromTs)
        AND (:toTs IS NULL OR a.ts <= :toTs)
        AND (:cursor IS NULL OR a.id > :cursor)
        ORDER BY a.id ASC
    """)
    fun findAllByFilters(
        @Param("tenantId") tenantId: UUID?,
        @Param("actorId") actorId: UUID?,
        @Param("entity") entity: String?,
        @Param("entityId") entityId: UUID?,
        @Param("fromTs") fromTs: Instant?,
        @Param("toTs") toTs: Instant?,
        @Param("cursor") cursor: Long?,
        pageable: Pageable
    ): Page<AuditLog>
}

