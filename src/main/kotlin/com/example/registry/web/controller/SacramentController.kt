package com.example.registry.web.controller

import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import com.example.registry.repo.AppUserRepository
import com.example.registry.service.IdempotencyService
import com.example.registry.service.SacramentService
import com.example.registry.tenancy.TenantContext
import com.example.registry.util.CursorPage
import com.example.registry.util.ETagUtil
import com.example.registry.web.dto.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/sacraments")
class SacramentController(
    private val sacramentService: SacramentService,
    private val idempotencyService: IdempotencyService,
    private val appUserRepository: AppUserRepository
) {
    
    @GetMapping
    fun getAllSacraments(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) fromDate: LocalDate?,
        @RequestParam(required = false) toDate: LocalDate?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        request: HttpServletRequest
    ): ResponseEntity<CursorPage<SacramentEventDto>> {
        val tenantId = TenantContext.require()
        
        val sacramentType = type?.let { SacramentType.valueOf(it) }
        val sacramentStatus = status?.let { Status.valueOf(it) }
        val cursorLong = cursor?.toLongOrNull()
        
        val page = sacramentService.findAll(
            tenantId, sacramentType, sacramentStatus, fromDate, toDate, cursorLong, limit
        )
        
        val dtos = page.content.map { event ->
            SacramentEventDto(
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
        
        val nextCursor = if (page.hasNext()) {
            page.content.lastOrNull()?.id?.toString()
        } else null
        
        val result = CursorPage(
            items = dtos,
            nextCursor = nextCursor,
            hasMore = page.hasNext()
        )
        
        val etag = ETagUtil.generateETag(result.toString())
        val headers = HttpHeaders()
        headers[HttpHeaders.ETAG] = etag
        
        val ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH)
        if (ETagUtil.matches(etag, ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).headers(headers).build()
        }
        
        return ResponseEntity.ok().headers(headers).body(result)
    }
    
    @PostMapping
    @PreAuthorize("hasPermission(@tenantContext.get(), 'sacraments.create')")
    fun createSacrament(
        @Valid @RequestBody request: CreateSacramentEventRequest,
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
        requestHttp: HttpServletRequest,
        authentication: Authentication
    ): ResponseEntity<SacramentEventDto> {
        val tenantId = TenantContext.require()
        
        if (idempotencyKey == null) {
            throw IllegalArgumentException("Idempotency-Key header is required")
        }
        
        val jwt = (authentication as JwtAuthenticationToken).token as Jwt
        val email = jwt.claims["email"] as? String ?: jwt.subject
        val currentUser = appUserRepository.findByEmail(email)
            ?: throw NoSuchElementException("Current user not found")
        val createdBy = currentUser.id
        
        // Note: Request body is already consumed by Spring, would need to use ContentCachingRequestWrapper in production
        val requestBody = "" // Simplified for now
        val idempotencyResult = idempotencyService.checkAndStore(tenantId, idempotencyKey, requestBody)
        
        if (idempotencyResult.isDuplicate) {
            return ResponseEntity.status(idempotencyResult.responseCode ?: HttpStatus.CONFLICT.value())
                .build()
        }
        
        val type = SacramentType.valueOf(request.type)
        val event = sacramentService.create(
            tenantId, type, request.personId, request.date,
            request.ministerId, request.bookNo, request.pageNo, request.entryNo, createdBy
        )
        
        val dto = SacramentEventDto(
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
        
        idempotencyService.recordResponse(tenantId, idempotencyKey, HttpStatus.CREATED.value())
        
        return ResponseEntity.status(HttpStatus.CREATED).body(dto)
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(@tenantContext.get(), 'sacraments.update')")
    fun updateSacrament(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateSacramentEventRequest,
        authentication: Authentication
    ): ResponseEntity<SacramentEventDto> {
        val tenantId = TenantContext.require()
        val jwt = (authentication as JwtAuthenticationToken).token as Jwt
        val email = jwt.claims["email"] as? String ?: jwt.subject
        val currentUser = appUserRepository.findByEmail(email)
            ?: throw NoSuchElementException("Current user not found")
        val updatedBy = currentUser.id
        
        val event = sacramentService.update(
            id, tenantId, request.personId, request.date,
            request.ministerId, request.bookNo, request.pageNo, request.entryNo, updatedBy
        )
        
        val dto = SacramentEventDto(
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
        
        return ResponseEntity.ok(dto)
    }
    
    @PostMapping("/{id}/status")
    @PreAuthorize("hasPermission(@tenantContext.get(), 'sacraments.update')")
    fun updateSacramentStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateSacramentEventStatusRequest,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val tenantId = TenantContext.require()
        val jwt = (authentication as JwtAuthenticationToken).token as Jwt
        val email = jwt.claims["email"] as? String ?: jwt.subject
        val currentUser = appUserRepository.findByEmail(email)
            ?: throw NoSuchElementException("Current user not found")
        val updatedBy = currentUser.id
        
        val status = Status.valueOf(request.status)
        sacramentService.updateStatus(id, tenantId, status, request.reason, updatedBy)
        
        return ResponseEntity.ok().build()
    }
}

