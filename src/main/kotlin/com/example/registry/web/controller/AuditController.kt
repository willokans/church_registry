package com.example.registry.web.controller

import com.example.registry.repo.AuditLogRepository
import com.example.registry.tenancy.TenantContext
import com.example.registry.util.CursorPage
import com.example.registry.web.dto.AuditLogDto
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/audit")
class AuditController(
    private val auditLogRepository: AuditLogRepository
) {
    
    @GetMapping
    @PreAuthorize("hasPermission(@tenantContext.get(), 'audit.view')")
    fun getAuditLogs(
        @RequestParam(required = false) actorId: String?,
        @RequestParam(required = false) entity: String?,
        @RequestParam(required = false) entityId: String?,
        @RequestParam(required = false) fromTs: Instant?,
        @RequestParam(required = false) toTs: Instant?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<CursorPage<AuditLogDto>> {
        val tenantId = TenantContext.require()
        
        val actorLong = actorId?.toLongOrNull()
        val cursorLong = cursor?.toLongOrNull()
        
        val pageable = PageRequest.of(0, limit, Sort.by("id").ascending())
        val page = auditLogRepository.findAllByFilters(
            tenantId, actorLong, entity, entityId, fromTs, toTs, cursorLong, pageable
        )
        
        val dtos = page.content.map { log ->
            AuditLogDto(
                id = log.id,
                tenantId = log.tenantId,
                actorId = log.actorId,
                action = log.action,
                entity = log.entity,
                entityId = log.entityId,
                before = log.before,
                after = log.after,
                ts = log.ts,
                hash = log.hash
            )
        }
        
        val nextCursor = if (page.hasNext()) {
            page.content.lastOrNull()?.id?.toString()
        } else null
        
        val result = CursorPage(
            items = dtos,
            nextCursor = nextCursor,
            hasMore = page.hasNext()
        )
        
        return ResponseEntity.ok(result)
    }
}

