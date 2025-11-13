package com.example.registry.web.dto

import java.time.Instant
import java.util.*

data class AuditLogDto(
    val id: Long,
    val tenantId: UUID?,
    val actorId: UUID?,
    val action: String,
    val entity: String,
    val entityId: UUID?,
    val before: Map<String, Any>?,
    val after: Map<String, Any>?,
    val ts: Instant,
    val hash: String?
)

