package com.example.registry.domain.entity

import com.example.registry.domain.Status
import jakarta.persistence.*
import org.hibernate.annotations.Where
import java.time.Instant
import java.util.*

@Entity
@Table(name = "app_users", indexes = [
    Index(name = "idx_app_users_email", columnList = "email"),
    Index(name = "idx_app_users_status", columnList = "status")
])
@Where(clause = "status = 'ACTIVE'")
data class AppUser(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, unique = true, columnDefinition = "CITEXT")
    val email: String,
    
    @Column(name = "full_name", nullable = false, length = 200)
    val fullName: String,
    
    @Column(name = "mfa_enabled", nullable = false)
    val mfaEnabled: Boolean = false,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: Status = Status.ACTIVE,
    
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(0)")
    val createdAt: Instant = Instant.now()
)

