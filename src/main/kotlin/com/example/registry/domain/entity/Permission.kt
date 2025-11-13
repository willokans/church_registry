package com.example.registry.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "permissions")
data class Permission(
    @Id
    @Column(name = "key", length = 100)
    val key: String
)

