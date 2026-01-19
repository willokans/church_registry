package com.example.registry.web.dto

import com.example.registry.web.dto.validation.ValidSacramentType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.time.LocalDate
import java.util.*

data class SacramentEventDto(
    val id: Long,
    val tenantId: Long,
    val type: String, // SacramentType enum value (e.g., "BAPTISM", "CONFIRMATION")
    val personId: UUID,
    val date: LocalDate,
    val priestName: String,
    val bookNo: Int,
    val pageNo: Int,
    val entryNo: Int,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant?
)

data class CreateSacramentEventRequest(
    @field:NotBlank(message = "Sacrament type is required")
    @field:ValidSacramentType(message = "Invalid sacrament type. Must be one of: BAPTISM, CONFIRMATION, EUCHARIST, RECONCILIATION, ANOINTING, HOLY_ORDERS, MATRIMONY")
    val type: String, // Must be a valid SacramentType enum value
    
    // Person ID - required for existing persons, optional for BAPTISM (will create person if not provided)
    val personId: UUID? = null,
    
    @field:NotNull(message = "Date is required")
    val date: LocalDate,
    
    @field:NotBlank(message = "Priest name is required")
    val priestName: String,
    
    @field:NotNull(message = "Book number is required")
    val bookNo: Int,
    
    @field:NotNull(message = "Page number is required")
    val pageNo: Int,
    
    @field:NotNull(message = "Entry number is required")
    val entryNo: Int,
    
    // Person details - required when creating BAPTISM without existing personId
    // These fields are used when personId is null and type is BAPTISM
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val dateOfBirth: LocalDate? = null,
    val gender: String? = null, // MALE, FEMALE, OTHER
    
    // Baptism-specific fields - used when type is BAPTISM
    val baptismName: String? = null,
    val fatherName: String? = null,
    val motherName: String? = null,
    val parentAddress: String? = null,
    val sponsor1Name: String? = null,
    val sponsor2Name: String? = null,
    val notes: String? = null
)

data class UpdateSacramentEventRequest(
    val personId: UUID?,
    val date: LocalDate?,
    val priestName: String?,
    val bookNo: Int?,
    val pageNo: Int?,
    val entryNo: Int?
)

data class UpdateSacramentEventStatusRequest(
    val status: String,
    val reason: String?
)
