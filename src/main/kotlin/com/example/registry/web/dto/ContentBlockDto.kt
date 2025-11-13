package com.example.registry.web.dto

import java.time.Instant
import java.util.*

data class ContentBlockDto(
    val id: UUID,
    val tenantId: UUID,
    val key: String,
    val published: Map<String, Any>?,
    val updatedAt: Instant
)

data class PublishContentBlockRequest(
    val content: Map<String, Any>
)

data class HomePageDto(
    val tenantSlug: String,
    val tenantName: String,
    val theme: Map<String, Any>?,
    val blocks: List<ContentBlockDto>
)

