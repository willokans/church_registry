package com.example.registry.web.dto

import com.example.registry.domain.SacramentType
import com.example.registry.domain.entity.Person
import com.example.registry.domain.entity.SacramentEvent
import com.example.registry.repo.PersonRepository
import org.springframework.stereotype.Component

/**
 * Mapper for converting SacramentEvent entities to DTOs.
 * This centralizes the mapping logic and keeps controllers clean.
 */
@Component
class SacramentEventMapper(
    private val personRepository: PersonRepository
) {
    
    fun toDto(event: SacramentEvent): SacramentEventDto {
        // For BAPTISM records, fetch person details and extract baptismName from metadata
        val person: Person? = if (event.type == SacramentType.BAPTISM) {
            personRepository.findByUuid(event.personId)
        } else null
        
        val baptismName = if (event.type == SacramentType.BAPTISM) {
            // Try to get from metadata first, then from person entity
            event.metadata?.get("baptismName") as? String
                ?: person?.baptismName
        } else null
        
        return SacramentEventDto(
            id = event.id,
            tenantId = event.tenantId,
            type = event.type.name,
            personId = event.personId,
            date = event.date,
            priestName = event.priestName,
            bookNo = event.bookNo,
            pageNo = event.pageNo,
            entryNo = event.entryNo,
            status = event.status.name,
            createdAt = event.createdAt,
            updatedAt = event.updatedAt,
            baptismName = baptismName,
            firstName = person?.firstName,
            lastName = person?.lastName
        )
    }
    
    fun toDtoList(events: List<SacramentEvent>): List<SacramentEventDto> {
        return events.map { toDto(it) }
    }
}

