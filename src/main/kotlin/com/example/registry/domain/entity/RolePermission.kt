package com.example.registry.domain.entity

import com.example.registry.domain.Role
import jakarta.persistence.*

@Entity
@Table(
    name = "role_permissions",
    uniqueConstraints = [UniqueConstraint(columnNames = arrayOf("role", "permission_key"))],
    indexes = [Index(name = "idx_role_permissions_role", columnList = "role")]
)
data class RolePermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val role: Role,
    
    @Column(name = "permission_key", nullable = false, length = 100)
    val permissionKey: String
)
