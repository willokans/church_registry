package com.example.registry.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "permissions")
data class Permission(
    @Id
    @Column(name = "key", length = 100)
    val key: String,
    
    @Column(name = "description", length = 500)
    val description: String? = null,
    
    @Column(name = "category", length = 50) // e.g., "users", "sacraments", "settings"
    val category: String? = null,
    
    @Column(name = "parent_key", length = 100)
    val parentKey: String? = null // For hierarchical permissions, e.g., "sacraments.*" → "sacraments.create", "sacraments.update"
)

