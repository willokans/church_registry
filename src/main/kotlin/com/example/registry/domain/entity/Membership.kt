package com.example.registry.domain.entity

import com.example.registry.domain.Role
import com.example.registry.domain.Status
import jakarta.persistence.*
import org.hibernate.annotations.Where
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "memberships",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "tenant_id"])],
    indexes = [
        Index(name = "idx_memberships_user_id", columnList = "user_id"),
        Index(name = "idx_memberships_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_memberships_role", columnList = "role"),
        Index(name = "idx_memberships_status", columnList = "status"),
        Index(name = "idx_memberships_user_tenant_status", columnList = "user_id,tenant_id,status")
    ]
)
@Where(clause = "\"status\" = 'ACTIVE'")
data class Membership(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    
    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val role: Role,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: Status = Status.ACTIVE,
    
    @Column(name = "granted_by")
    val grantedBy: Long? = null,
    
    @Column(name = "granted_at", nullable = false, updatable = false)
    val grantedAt: Instant = Instant.now(),
    
    @Column(name = "expires_at")
    val expiresAt: Instant? = null
) {
    // Helper method to check if membership has expired
    fun isExpired(): Boolean = expiresAt != null && expiresAt.isBefore(Instant.now())
}

