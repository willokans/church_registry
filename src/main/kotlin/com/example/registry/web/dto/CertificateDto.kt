package com.example.registry.web.dto

import java.time.Instant
import java.util.*

data class CertificateDto(
    val id: Long,
    val eventId: Long,
    val serialNo: String,
    val issuedAt: Instant,
    val revocationStatus: String
)

data class CertificateVerificationDto(
    val serialNo: String,
    val valid: Boolean,
    val eventId: Long?,
    val issuedAt: Instant?,
    val revocationStatus: String?
)

