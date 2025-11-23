package com.example.registry.web.dto

import java.time.Instant
import java.time.LocalDate
import java.util.*

data class SacramentEventDto(
    val id: Long,
    val tenantId: Long,
    val type: String,
    val personId: UUID,
    val date: LocalDate,
    val ministerId: Long,
    val bookNo: Int,
    val pageNo: Int,
    val entryNo: Int,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant?
)

data class CreateSacramentEventRequest(
    val type: String,
    val personId: UUID,
    val date: LocalDate,
    val ministerId: Long,
    val bookNo: Int,
    val pageNo: Int,
    val entryNo: Int
)

data class UpdateSacramentEventRequest(
    val personId: UUID?,
    val date: LocalDate?,
    val ministerId: Long,
    val bookNo: Int?,
    val pageNo: Int?,
    val entryNo: Int?
)

data class UpdateSacramentEventStatusRequest(
    val status: String,
    val reason: String?
)

