package com.example.registry.service

import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import com.example.registry.domain.entity.SacramentEvent
import com.example.registry.repo.SacramentEventRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Service
class SacramentService(
    private val sacramentEventRepository: SacramentEventRepository,
    private val auditService: AuditService
) {
    
    fun findAll(
        tenantId: UUID,
        type: SacramentType?,
        status: Status?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        cursor: UUID?,
        limit: Int = 20
    ): Page<SacramentEvent> {
        val pageable = PageRequest.of(0, limit, Sort.by("id").ascending())
        return sacramentEventRepository.findAllByFilters(
            tenantId, type, status, fromDate, toDate, cursor, pageable
        )
    }
    
    fun findById(id: UUID): SacramentEvent? = sacramentEventRepository.findById(id).orElse(null)
    
    @Transactional
    fun create(
        tenantId: UUID,
        type: SacramentType,
        personId: UUID,
        date: LocalDate,
        ministerId: UUID?,
        bookNo: Int,
        pageNo: Int,
        entryNo: Int,
        createdBy: UUID
    ): SacramentEvent {
        // Check for duplicate entry
        val existing = sacramentEventRepository.findByTenantIdAndTypeAndBookNoAndPageNoAndEntryNoAndStatus(
            tenantId, type, bookNo, pageNo, entryNo, Status.ACTIVE
        )
        if (existing != null) {
            throw IllegalArgumentException("Duplicate entry: book $bookNo, page $pageNo, entry $entryNo")
        }
        
        val event = SacramentEvent(
            tenantId = tenantId,
            type = type,
            personId = personId,
            date = date,
            ministerId = ministerId,
            bookNo = bookNo,
            pageNo = pageNo,
            entryNo = entryNo,
            createdBy = createdBy
        )
        
        val saved = sacramentEventRepository.save(event)
        auditService.log(tenantId, createdBy, "CREATE", "SacramentEvent", saved.id, null, saved)
        return saved
    }
    
    @Transactional
    fun update(
        id: UUID,
        tenantId: UUID,
        personId: UUID?,
        date: LocalDate?,
        ministerId: UUID?,
        bookNo: Int?,
        pageNo: Int?,
        entryNo: Int?,
        updatedBy: UUID
    ): SacramentEvent {
        val existing = sacramentEventRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Sacrament event not found") }
        
        if (existing.tenantId != tenantId) {
            throw IllegalArgumentException("Event does not belong to tenant")
        }
        
        if (existing.status != Status.ACTIVE) {
            throw IllegalArgumentException("Cannot update inactive event")
        }
        
        val updated = existing.copy(
            personId = personId ?: existing.personId,
            date = date ?: existing.date,
            ministerId = ministerId ?: existing.ministerId,
            bookNo = bookNo ?: existing.bookNo,
            pageNo = pageNo ?: existing.pageNo,
            entryNo = entryNo ?: existing.entryNo,
            updatedBy = updatedBy,
            updatedAt = Instant.now()
        )
        
        val saved = sacramentEventRepository.save(updated)
        auditService.log(tenantId, updatedBy, "UPDATE", "SacramentEvent", saved.id, existing, saved)
        return saved
    }
    
    @Transactional
    fun updateStatus(
        id: UUID,
        tenantId: UUID,
        status: Status,
        reason: String?,
        updatedBy: UUID
    ) {
        val existing = sacramentEventRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Sacrament event not found") }
        
        if (existing.tenantId != tenantId) {
            throw IllegalArgumentException("Event does not belong to tenant")
        }
        
        val updated = existing.copy(
            status = status,
            deactivatedAt = if (status == Status.INACTIVE) Instant.now() else null,
            deactivatedBy = if (status == Status.INACTIVE) updatedBy else null,
            deactivationReason = if (status == Status.INACTIVE) reason else null
        )
        
        val saved = sacramentEventRepository.save(updated)
        auditService.log(tenantId, updatedBy, "UPDATE_STATUS", "SacramentEvent", saved.id, existing, saved)
    }
}

