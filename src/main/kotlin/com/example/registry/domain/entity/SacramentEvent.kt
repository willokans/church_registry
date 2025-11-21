package com.example.registry.domain.entity

import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import jakarta.persistence.*
import org.hibernate.annotations.Where
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "sacrament_events")
// Indexes temporarily removed - will be added back after tables are created
// Indexes can be created manually via SQL or added back once schema is stable
@Where(clause = "\"status\" = 'ACTIVE'")
data class SacramentEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val type: SacramentType,
    
    @Column(name = "person_id", nullable = false)
    val personId: UUID,
    
    @Column(nullable = false)
    val date: LocalDate,
    
    @Column(name = "minister_id")
    val ministerId: Long? = null,
    
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
    val createdBy: Long,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_by")
    val updatedBy: Long? = null,
    
    @Column(name = "updated_at")
    val updatedAt: Instant? = null,
    
    @Column(name = "deactivated_at")
    val deactivatedAt: Instant? = null,
    
    @Column(name = "deactivated_by")
    val deactivatedBy: Long? = null,
    
    @Column(name = "deactivation_reason", length = 500)
    val deactivationReason: String? = null
)

