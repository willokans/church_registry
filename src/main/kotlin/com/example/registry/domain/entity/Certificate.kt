package com.example.registry.domain.entity

import com.example.registry.domain.RevocationStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "certificates",
    indexes = [
        Index(name = "idx_certificates_event", columnList = "event_id"),
        Index(name = "idx_certificates_serial", columnList = "serial_no", unique = true)
    ]
)
data class Certificate(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "event_id", nullable = false)
    val eventId: UUID,
    
    @Column(name = "serial_no", nullable = false, unique = true, length = 26)
    val serialNo: String, // ULID
    
    @Column(name = "issued_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(0)")
    val issuedAt: Instant = Instant.now(),
    
    @Column(name = "issuer_id", nullable = false)
    val issuerId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "revocation_status", nullable = false, length = 20)
    val revocationStatus: RevocationStatus = RevocationStatus.ACTIVE
)

