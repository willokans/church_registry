package com.example.registry.web.dto

import com.example.registry.domain.entity.SacramentEvent

/**
 * Mapper for converting SacramentEvent entities to DTOs.
 * This centralizes the mapping logic and keeps controllers clean.
 */
object SacramentEventMapper {
    
    fun toDto(event: SacramentEvent): SacramentEventDto {
        return SacramentEventDto(
            id = event.id,
            tenantId = event.tenantId,
            type = event.type.name,
            personId = event.personId,
            date = event.date,
            ministerId = event.ministerId,
            bookNo = event.bookNo,
            pageNo = event.pageNo,
            entryNo = event.entryNo,
            status = event.status.name,
            createdAt = event.createdAt,
            updatedAt = event.updatedAt
        )
    }
    
    fun toDtoList(events: List<SacramentEvent>): List<SacramentEventDto> {
        return events.map { toDto(it) }
    }
}

