package com.example.registry.service

import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import com.example.registry.domain.entity.Person
import com.example.registry.domain.entity.SacramentEvent
import com.example.registry.repo.SacramentEventRepository
import com.example.registry.service.PersonService
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
    private val auditService: AuditService,
    private val personService: PersonService
) {
    
    fun findAll(
        tenantId: Long,
        type: SacramentType?,
        status: Status?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        cursor: Long?,
        limit: Int = 20
    ): Page<SacramentEvent> {
        val pageable = PageRequest.of(0, limit, Sort.by("id").ascending())
        return sacramentEventRepository.findAllByFilters(
            tenantId, type, status, fromDate, toDate, cursor, pageable
        )
    }
    
    /**
     * Find all sacraments of a specific type.
     * This is a convenience method that ensures type is always provided.
     */
    fun findAllByType(
        tenantId: Long,
        type: SacramentType,
        status: Status?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        cursor: Long?,
        limit: Int = 20
    ): Page<SacramentEvent> {
        val pageable = PageRequest.of(0, limit, Sort.by("id").ascending())
        return sacramentEventRepository.findAllByFilters(
            tenantId, type, status, fromDate, toDate, cursor, pageable
        )
    }
    
    fun findById(id: Long): SacramentEvent? = sacramentEventRepository.findById(id).orElse(null)

    fun hasActiveSacrament(
        tenantId: Long,
        personId: UUID,
        type: SacramentType
    ): Boolean {
        return sacramentEventRepository.existsByTenantIdAndTypeAndPersonIdAndStatus(
            tenantId, type, personId, Status.ACTIVE
        )
    }
    
    @Transactional
    fun create(
        tenantId: Long,
        type: SacramentType,
        personId: UUID,
        date: LocalDate,
        priestName: String,
        bookNo: Int,
        pageNo: Int,
        entryNo: Int,
        createdBy: Long,
        metadata: Map<String, Any>? = null
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
            priestName = priestName,
            bookNo = bookNo,
            pageNo = pageNo,
            entryNo = entryNo,
            createdBy = createdBy,
            metadata = metadata
        )
        
        val saved = sacramentEventRepository.save(event)
        auditService.log(tenantId, createdBy, "CREATE", "SacramentEvent", saved.id.toString(), null, saved)
        return saved
    }
    
    @Transactional
    fun update(
        id: Long,
        tenantId: Long,
        personId: UUID?,
        date: LocalDate?,
        priestName: String?,
        bookNo: Int?,
        pageNo: Int?,
        entryNo: Int?,
        updatedBy: Long
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
            priestName = priestName ?: existing.priestName,
            bookNo = bookNo ?: existing.bookNo,
            pageNo = pageNo ?: existing.pageNo,
            entryNo = entryNo ?: existing.entryNo,
            updatedBy = updatedBy,
            updatedAt = Instant.now()
        )
        
        val saved = sacramentEventRepository.save(updated)
        auditService.log(tenantId, updatedBy, "UPDATE", "SacramentEvent", saved.id.toString(), existing, saved)
        return saved
    }
    
    @Transactional
    fun updateStatus(
        id: Long,
        tenantId: Long,
        status: Status,
        reason: String?,
        updatedBy: Long
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
        auditService.log(tenantId, updatedBy, "UPDATE_STATUS", "SacramentEvent", saved.id.toString(), existing, saved)
    }
    
    /**
     * Create a Baptism sacrament with person details.
     * This creates both the person and the baptism record in one transaction.
     */
    @Transactional
    fun createBaptismWithPerson(
        tenantId: Long,
        firstName: String,
        lastName: String,
        middleName: String?,
        dateOfBirth: LocalDate?,
        gender: String?,
        baptismName: String,
        fatherName: String?,
        motherName: String?,
        baptismDate: LocalDate,
        priestName: String,
        sponsor1Name: String?,
        sponsor2Name: String?,
        bookNo: Int,
        pageNo: Int,
        entryNo: Int,
        notes: String?,
        createdBy: Long
    ): Pair<Person, SacramentEvent> {
        // Create the person first
        val person = personService.createPerson(
            tenantId = tenantId,
            firstName = firstName,
            lastName = lastName,
            middleName = middleName,
            dateOfBirth = dateOfBirth,
            gender = gender,
            baptismName = baptismName,
            fatherName = fatherName,
            motherName = motherName
        )
        
        // Build metadata for baptism-specific information
        val metadata = mutableMapOf<String, Any>(
            "baptismName" to baptismName
        )
        fatherName?.let { metadata["fatherName"] = it }
        motherName?.let { metadata["motherName"] = it }
        sponsor1Name?.let { metadata["sponsor1Name"] = it }
        sponsor2Name?.let { metadata["sponsor2Name"] = it }
        notes?.let { metadata["notes"] = it }
        
        // Check for duplicate entry
        val existing = sacramentEventRepository.findByTenantIdAndTypeAndBookNoAndPageNoAndEntryNoAndStatus(
            tenantId, SacramentType.BAPTISM, bookNo, pageNo, entryNo, Status.ACTIVE
        )
        if (existing != null) {
            throw IllegalArgumentException("Duplicate entry: book $bookNo, page $pageNo, entry $entryNo")
        }
        
        // Create the baptism event
        val event = SacramentEvent(
            tenantId = tenantId,
            type = SacramentType.BAPTISM,
            personId = person.uuid,
            date = baptismDate,
            priestName = priestName,
            bookNo = bookNo,
            pageNo = pageNo,
            entryNo = entryNo,
            createdBy = createdBy,
            metadata = metadata
        )
        
        val savedEvent = sacramentEventRepository.save(event)
        auditService.log(tenantId, createdBy, "CREATE", "SacramentEvent", savedEvent.id.toString(), null, savedEvent)
        
        return Pair(person, savedEvent)
    }
}

