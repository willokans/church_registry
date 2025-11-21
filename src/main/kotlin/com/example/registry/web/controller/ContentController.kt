package com.example.registry.web.controller

import com.example.registry.domain.entity.ContentBlock
import com.example.registry.repo.AppUserRepository
import com.example.registry.security.AuthorizationService
import com.example.registry.service.ContentService
import com.example.registry.tenancy.TenantContext
import com.example.registry.web.dto.ContentBlockDto
import com.example.registry.web.dto.PublishContentBlockRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/admin/content")
class ContentController(
    private val contentService: ContentService,
    private val authorizationService: AuthorizationService,
    private val appUserRepository: AppUserRepository
) {
    
    @PostMapping("/{key}/publish")
    @PreAuthorize("hasPermission(@tenantContext.get(), 'settings.edit')")
    fun publishContent(
        @PathVariable key: String,
        @Valid @RequestBody request: PublishContentBlockRequest,
        authentication: Authentication
    ): ResponseEntity<ContentBlockDto> {
        val tenantId = TenantContext.require()
        val jwt = (authentication as JwtAuthenticationToken).token as Jwt
        val email = jwt.claims["email"] as? String ?: jwt.subject
        val currentUser = appUserRepository.findByEmail(email)
            ?: throw NoSuchElementException("Current user not found")
        val updatedBy = currentUser.id
        
        val block = contentService.publish(tenantId, key, request.content, updatedBy)
        
        val dto = ContentBlockDto(
            id = block.id,
            tenantId = block.tenantId,
            key = block.key,
            published = block.published,
            updatedAt = block.updatedAt
        )
        
        return ResponseEntity.ok(dto)
    }
}

