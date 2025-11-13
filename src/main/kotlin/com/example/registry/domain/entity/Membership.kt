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
        Index(name = "idx_memberships_user", columnList = "user_id"),
        Index(name = "idx_memberships_tenant", columnList = "tenant_id"),
        Index(name = "idx_memberships_status", columnList = "status")
    ]
)
@Where(clause = "status = 'ACTIVE'")
data class Membership(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val role: Role,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: Status = Status.ACTIVE,
    
    @Column(name = "granted_by")
    val grantedBy: UUID? = null,
    
    @Column(name = "granted_at", nullable = false, updatable = false)
    val grantedAt: Instant = Instant.now()
)

