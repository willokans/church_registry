package com.example.registry.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "tenants")
data class Tenant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, unique = true, length = 100)
    val slug: String,
    
    @Column(nullable = false, length = 200)
    val name: String,
    
    @Column(name = "parent_id")
    val parentId: Long? = null,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    val theme: Map<String, Any>? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(0)")
    val createdAt: Instant = Instant.now()
)

