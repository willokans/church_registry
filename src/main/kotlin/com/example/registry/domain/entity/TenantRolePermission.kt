package com.example.registry.domain.entity

import com.example.registry.domain.Role
import jakarta.persistence.*

@Entity
@Table(
    name = "tenant_role_permissions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "role", "permission_key"])],
    indexes = [
        Index(name = "idx_tenant_role_permissions_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_tenant_role_permissions_role", columnList = "role"),
        Index(name = "idx_tenant_role_permissions_tenant_role", columnList = "tenant_id,role")
    ]
)
data class TenantRolePermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val role: Role,

    @Column(name = "permission_key", nullable = false, length = 100)
    val permissionKey: String,

    @Column(nullable = false)
    val granted: Boolean = true // true = grant, false = revoke
)

