package com.example.registry.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "idempotency_keys",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "key"])]
    // Indexes temporarily removed - will be added back via Liquibase or manual SQL
)
data class IdempotencyKey(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,
    
    @Column(nullable = false, length = 255)
    val key: String,
    
    @Column(name = "request_hash", length = 64)
    val requestHash: String? = null,
    
    @Column(name = "response_code")
    val responseCode: Int? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

