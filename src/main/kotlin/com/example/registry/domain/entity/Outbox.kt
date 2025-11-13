package com.example.registry.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

/**
 * Outbox table for transactional messaging pattern.
 * Messages are written in the same transaction as business logic,
 * then published asynchronously by a separate process.
 */
@Entity
@Table(
    name = "outbox",
    indexes = [
        Index(name = "idx_outbox_tenant", columnList = "tenant_id"),
        Index(name = "idx_outbox_status", columnList = "status"),
        Index(name = "idx_outbox_created", columnList = "created_at")
    ]
)
data class Outbox(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "tenant_id")
    val tenantId: UUID? = null,
    
    @Column(nullable = false, length = 100)
    val topic: String,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,
    
    @Column(nullable = false, length = 50)
    val status: String = "PENDING", // PENDING, PUBLISHED, FAILED
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "published_at")
    val publishedAt: Instant? = null,
    
    @Column(name = "retry_count", nullable = false)
    val retryCount: Int = 0,
    
    @Column(name = "error_message", length = 1000)
    val errorMessage: String? = null
)

