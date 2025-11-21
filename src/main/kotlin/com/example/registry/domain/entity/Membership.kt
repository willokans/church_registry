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
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "tenant_id"])]
    // Indexes temporarily removed - will be added back after tables are created
    // Indexes can be created manually via SQL or added back once schema is stable
)
@Where(clause = "status = 'ACTIVE'")
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
    
    @Column(name = "granted_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(0)")
    val grantedAt: Instant = Instant.now()
)

