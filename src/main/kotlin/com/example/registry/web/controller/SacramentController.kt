package com.example.registry.web.controller

import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import com.example.registry.repo.AppUserRepository
import com.example.registry.service.IdempotencyService
import com.example.registry.service.PersonService
import com.example.registry.service.SacramentService
import com.example.registry.tenancy.TenantContext
import com.example.registry.util.CursorPage
import com.example.registry.util.ETagUtil
import com.example.registry.web.dto.*
import com.example.registry.web.dto.SacramentEventMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Page
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
    private val appUserRepository: AppUserRepository,
    private val personService: PersonService,
    private val sacramentEventMapper: SacramentEventMapper
) {
    
    @GetMapping("/types")
    fun getSacramentTypes(): ResponseEntity<Map<String, List<String>>> {
        val types = SacramentType.values().map { it.name }
        return ResponseEntity.ok(mapOf("types" to types))
    }
    
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
        // Parse type if provided, otherwise null returns all types
        val sacramentType = type?.let { parseSacramentType(it) }
        val sacramentStatus = parseStatus(status)
        val cursorLong = parseCursor(cursor)
        
        val page = sacramentService.findAll(
            tenantId, sacramentType, sacramentStatus, fromDate, toDate, cursorLong, limit
        )
        
        return buildPaginatedResponse(page, request)
    }
    
    @GetMapping("/type/{type}")
    fun getSacramentsByType(
        @PathVariable type: String,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) fromDate: LocalDate?,
        @RequestParam(required = false) toDate: LocalDate?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        request: HttpServletRequest
    ): ResponseEntity<CursorPage<SacramentEventDto>> {
        val tenantId = TenantContext.require()
        val sacramentType = parseSacramentType(type)
        val sacramentStatus = parseStatus(status)
        val cursorLong = parseCursor(cursor)
        
        val page = sacramentService.findAllByType(
            tenantId, sacramentType, sacramentStatus, fromDate, toDate, cursorLong, limit
        )
        
        return buildPaginatedResponse(page, request)
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
        
        val createdBy = getCurrentUserId(authentication)
        
        // Note: Request body is already consumed by Spring, would need to use ContentCachingRequestWrapper in production
        val requestBody = "" // Simplified for now
        val idempotencyResult = idempotencyService.checkAndStore(tenantId, idempotencyKey, requestBody)
        
        if (idempotencyResult.isDuplicate) {
            return ResponseEntity.status(idempotencyResult.responseCode ?: HttpStatus.CONFLICT.value())
                .build()
        }
        
        // Validation annotation ensures type is valid, but we handle it gracefully
        val type = parseSacramentType(request.type)
        
        // Determine personId - create person if needed for BAPTISM
        val personId = when {
            request.personId != null -> {
                // Use existing person
                request.personId
            }
            type == SacramentType.BAPTISM -> {
                // Create new person for BAPTISM
                if (request.firstName == null || request.lastName == null) {
                    throw IllegalArgumentException("firstName and lastName are required when creating BAPTISM without personId")
                }
                if (request.baptismName == null) {
                    throw IllegalArgumentException("baptismName is required when creating BAPTISM")
                }
                
                val person = personService.createPerson(
                    tenantId = tenantId,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    middleName = request.middleName,
                    dateOfBirth = request.dateOfBirth,
                    gender = request.gender,
                    baptismName = request.baptismName,
                    fatherName = request.fatherName,
                    motherName = request.motherName
                )
                person.uuid
            }
            else -> {
                throw IllegalArgumentException("personId is required for sacrament type: ${type.name}")
            }
        }
        
        // Build metadata for BAPTISM-specific information
        val metadata = if (type == SacramentType.BAPTISM) {
            val meta = mutableMapOf<String, Any>()
            request.baptismName?.let { meta["baptismName"] = it }
            request.fatherName?.let { meta["fatherName"] = it }
            request.motherName?.let { meta["motherName"] = it }
            request.parentAddress?.let { meta["parentAddress"] = it }
            request.sponsor1Name?.let { meta["sponsor1Name"] = it }
            request.sponsor2Name?.let { meta["sponsor2Name"] = it }
            request.notes?.let { meta["notes"] = it }
            if (meta.isNotEmpty()) meta else null
        } else null
        
        val event = sacramentService.create(
            tenantId, type, personId, request.date,
            request.priestName, request.bookNo, request.pageNo, request.entryNo, createdBy, metadata
        )
        
        idempotencyService.recordResponse(tenantId, idempotencyKey, HttpStatus.CREATED.value())
        
        return ResponseEntity.status(HttpStatus.CREATED).body(sacramentEventMapper.toDto(event))
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(@tenantContext.get(), 'sacraments.update')")
    fun updateSacrament(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateSacramentEventRequest,
        authentication: Authentication
    ): ResponseEntity<SacramentEventDto> {
        val tenantId = TenantContext.require()
        val updatedBy = getCurrentUserId(authentication)
        
        val event = sacramentService.update(
            id, tenantId, request.personId, request.date,
            request.priestName, request.bookNo, request.pageNo, request.entryNo, updatedBy
        )
        
        return ResponseEntity.ok(sacramentEventMapper.toDto(event))
    }
    
    @PostMapping("/{id}/status")
    @PreAuthorize("hasPermission(@tenantContext.get(), 'sacraments.update')")
    fun updateSacramentStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateSacramentEventStatusRequest,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val tenantId = TenantContext.require()
        val updatedBy = getCurrentUserId(authentication)
        val status = Status.valueOf(request.status)
        
        sacramentService.updateStatus(id, tenantId, status, request.reason, updatedBy)
        
        return ResponseEntity.ok().build()
    }
    
    // Private helper methods
    private fun getCurrentUserId(authentication: Authentication): Long {
        val jwt = (authentication as JwtAuthenticationToken).token as Jwt
        val email = jwt.claims["email"] as? String ?: jwt.subject
        val currentUser = appUserRepository.findByEmail(email)
            ?: throw NoSuchElementException("Current user not found")
        return currentUser.id
    }
    
    private fun parseStatus(status: String?): Status? {
        return status?.let { Status.valueOf(it) }
    }
    
    private fun parseCursor(cursor: String?): Long? {
        return cursor?.toLongOrNull()
    }
    
    private fun parseSacramentType(type: String): SacramentType {
        return try {
            SacramentType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid sacrament type: $type. Valid types are: ${SacramentType.values().joinToString { it.name }}")
        }
    }
    
    private fun buildPaginatedResponse(
        page: Page<com.example.registry.domain.entity.SacramentEvent>,
        request: HttpServletRequest
    ): ResponseEntity<CursorPage<SacramentEventDto>> {
        val dtos = page.content.map { sacramentEventMapper.toDto(it) }
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
}

