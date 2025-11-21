package com.example.registry.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "content_blocks",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "key"])],
    indexes = [Index(name = "idx_content_blocks_tenant", columnList = "tenant_id")]
)
data class ContentBlock(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,
    
    @Column(nullable = false, length = 100)
    val key: String,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    val draft: Map<String, Any>? = null,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    val published: Map<String, Any>? = null,
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)

