package com.example.registry.domain.entity

import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import jakarta.persistence.*
import org.hibernate.annotations.Where
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(
    name = "sacrament_events",
    indexes = [
        Index(name = "idx_sacrament_events_tenant", columnList = "tenant_id"),
        Index(name = "idx_sacrament_events_type", columnList = "type"),
        Index(name = "idx_sacrament_events_date", columnList = "date"),
        Index(name = "idx_sacrament_events_status", columnList = "status"),
        Index(name = "idx_sacrament_events_book_page_entry", columnList = "tenant_id,type,book_no,page_no,entry_no")
    ]
)
@Where(clause = "status = 'ACTIVE'")
data class SacramentEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val type: SacramentType,
    
    @Column(name = "person_id", nullable = false)
    val personId: UUID,
    
    @Column(nullable = false)
    val date: LocalDate,
    
    @Column(name = "minister_id")
    val ministerId: UUID? = null,
    
    @Column(name = "book_no", nullable = false)
    val bookNo: Int,
    
    @Column(name = "page_no", nullable = false)
    val pageNo: Int,
    
    @Column(name = "entry_no", nullable = false)
    val entryNo: Int,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: Status = Status.ACTIVE,
    
    @Column(name = "created_by", nullable = false)
    val createdBy: UUID,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_by")
    val updatedBy: UUID? = null,
    
    @Column(name = "updated_at")
    val updatedAt: Instant? = null,
    
    @Column(name = "deactivated_at")
    val deactivatedAt: Instant? = null,
    
    @Column(name = "deactivated_by")
    val deactivatedBy: UUID? = null,
    
    @Column(name = "deactivation_reason", length = 500)
    val deactivationReason: String? = null
)

