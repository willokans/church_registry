package com.example.registry.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "audit_logs",
    indexes = [
        Index(name = "idx_audit_logs_tenant", columnList = "tenant_id"),
        Index(name = "idx_audit_logs_actor", columnList = "actor_id"),
        Index(name = "idx_audit_logs_entity", columnList = "entity,entity_id"),
        Index(name = "idx_audit_logs_ts", columnList = "ts")
    ]
)
data class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "tenant_id")
    val tenantId: UUID? = null,
    
    @Column(name = "actor_id")
    val actorId: UUID? = null,
    
    @Column(nullable = false, length = 50)
    val action: String,
    
    @Column(nullable = false, length = 100)
    val entity: String,
    
    @Column(name = "entity_id")
    val entityId: UUID? = null,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    val before: Map<String, Any>? = null,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    val after: Map<String, Any>? = null,
    
    @Column(nullable = false, updatable = false)
    val ts: Instant = Instant.now(),
    
    @Column(length = 64)
    val hash: String? = null,
    
    @Column(name = "prev_hash", length = 64)
    val prevHash: String? = null
)

