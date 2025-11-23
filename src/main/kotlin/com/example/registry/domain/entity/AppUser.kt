package com.example.registry.domain.entity

import com.example.registry.domain.Status
import jakarta.persistence.*
import org.hibernate.annotations.Where
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Entity
@Table(name = "app_users", indexes = [
    Index(name = "idx_app_users_email", columnList = "email"),
    Index(name = "idx_app_users_status", columnList = "status")
])
@Where(clause = "\"status\" = 'ACTIVE'")
data class AppUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, unique = true, length = 255)
    val email: String, // CITEXT in PostgreSQL (handled by Liquibase), VARCHAR in H2
    
    @Column(name = "full_name", nullable = false, length = 200)
    val fullName: String,
    
    @Column(name = "mfa_enabled", nullable = false)
    val mfaEnabled: Boolean = false,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: Status = Status.ACTIVE,
    
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(0)")
    var createdAt: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)
) {
    @PrePersist
    fun prePersist() {
        createdAt = createdAt.truncatedTo(ChronoUnit.SECONDS)
    }
}

