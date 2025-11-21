package com.example.registry.web.dto

import java.time.Instant
import java.util.*

data class TenantDto(
    val id: Long,
    val slug: String,
    val name: String,
    val parentId: Long?,
    val theme: Map<String, Any>?,
    val createdAt: Instant
)

data class CreateTenantRequest(
    val slug: String,
    val name: String,
    val parentId: Long? = null,
    val theme: Map<String, Any>? = null
)

data class UpdateTenantRequest(
    val name: String? = null,
    val parentId: Long? = null,
    val theme: Map<String, Any>? = null
)

