package com.example.registry.web.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * Request DTO for creating a Baptism sacrament with person details.
 * This allows creating both the person and the baptism record in one request.
 */
data class CreateBaptismRequest(
    // Person information
    @field:NotBlank(message = "First name is required")
    val firstName: String,
    
    @field:NotBlank(message = "Last name is required")
    val lastName: String,
    
    val middleName: String? = null,
    
    val dateOfBirth: LocalDate? = null,
    
    val gender: String? = null, // MALE, FEMALE, OTHER
    
    // Baptism-specific information
    @field:NotBlank(message = "Baptism name is required")
    val baptismName: String,
    
    @field:NotNull(message = "Baptism date is required")
    val baptismDate: LocalDate,
    
    // Parent information
    val fatherName: String? = null,
    val motherName: String? = null,
    
    // Minister/Priest information
    @field:NotBlank(message = "Priest name is required")
    val priestName: String,
    
    // Sponsor/Godparent information
    val sponsor1Name: String? = null,
    val sponsor2Name: String? = null,
    
    // Registry book information
    @field:NotNull(message = "Book number is required")
    val bookNo: Int,
    
    @field:NotNull(message = "Page number is required")
    val pageNo: Int,
    
    @field:NotNull(message = "Entry number is required")
    val entryNo: Int,
    
    // Additional notes
    val notes: String? = null
)

/**
 * Response DTO for Baptism with person details
 */
data class BaptismDto(
    val id: Long,
    val personId: Long,
    val personName: String,
    val baptismName: String,
    val baptismDate: LocalDate,
    val fatherName: String?,
    val motherName: String?,
    val priestName: String,
    val sponsor1Name: String?,
    val sponsor2Name: String?,
    val bookNo: Int,
    val pageNo: Int,
    val entryNo: Int,
    val notes: String?
)

