package com.example.registry.web.controller

import com.example.registry.repo.AppUserRepository
import com.example.registry.service.TenantService
import com.example.registry.web.dto.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/tenants")
open class TenantController(
    private val tenantService: TenantService,
    private val appUserRepository: AppUserRepository
) {
    
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    open fun getAllTenants(): ResponseEntity<List<TenantDto>> {
        val tenants = tenantService.findAll()
        
        val dtos = tenants.map { tenant ->
            TenantDto(
                id = tenant.id,
                slug = tenant.slug,
                name = tenant.name,
                parentId = tenant.parentId,
                theme = tenant.theme,
                createdAt = tenant.createdAt
            )
        }
        
        return ResponseEntity.ok(dtos)
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    open fun getTenantById(@PathVariable id: Long): ResponseEntity<TenantDto> {
        val tenant = tenantService.findById(id)
            ?: throw NoSuchElementException("Tenant not found")
        
        val dto = TenantDto(
            id = tenant.id,
            slug = tenant.slug,
            name = tenant.name,
            parentId = tenant.parentId,
            theme = tenant.theme,
            createdAt = tenant.createdAt
        )
        
        return ResponseEntity.ok(dto)
    }
    
    @GetMapping("/slug/{slug}")
    open fun getTenantBySlug(@PathVariable slug: String): ResponseEntity<TenantDto> {
        val tenant = tenantService.findBySlug(slug)
            ?: throw NoSuchElementException("Tenant not found")
        
        val dto = TenantDto(
            id = tenant.id,
            slug = tenant.slug,
            name = tenant.name,
            parentId = tenant.parentId,
            theme = tenant.theme,
            createdAt = tenant.createdAt
        )
        
        return ResponseEntity.ok(dto)
    }
    
    @PostMapping
    open fun createTenant(
        @Valid @RequestBody request: CreateTenantRequest,
        authentication: Authentication?
    ): ResponseEntity<TenantDto> {
        // Get current user ID if authenticated, otherwise null
        val createdBy = if (authentication != null) {
            val jwt = (authentication as? JwtAuthenticationToken)?.token
            val email = jwt?.claims?.get("email") as? String ?: jwt?.subject
            email?.let { appUserRepository.findByEmail(it)?.id }
        } else {
            null
        }
        
        val tenant = tenantService.createTenant(
            slug = request.slug,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            createdBy = createdBy
        )
        
        val dto = TenantDto(
            id = tenant.id,
            slug = tenant.slug,
            name = tenant.name,
            parentId = tenant.parentId,
            theme = tenant.theme,
            createdAt = tenant.createdAt
        )
        
        return ResponseEntity.ok(dto)
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    open fun updateTenant(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTenantRequest,
        authentication: Authentication
    ): ResponseEntity<TenantDto> {
        // Get current user ID if authenticated and user exists, otherwise null
        val updatedBy = try {
            val jwt = (authentication as? JwtAuthenticationToken)?.token
            val email = jwt?.claims?.get("email") as? String ?: jwt?.subject
            email?.let { appUserRepository.findByEmail(it)?.id }
        } catch (e: Exception) {
            null
        }
        
        val tenant = tenantService.updateTenant(
            id = id,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            updatedBy = updatedBy
        )
        
        val dto = TenantDto(
            id = tenant.id,
            slug = tenant.slug,
            name = tenant.name,
            parentId = tenant.parentId,
            theme = tenant.theme,
            createdAt = tenant.createdAt
        )
        
        return ResponseEntity.ok(dto)
    }
}

