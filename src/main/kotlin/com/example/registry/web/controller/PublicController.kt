package com.example.registry.web.controller

import com.example.registry.repo.TenantRepository
import com.example.registry.service.ContentService
import com.example.registry.web.dto.ContentBlockDto
import com.example.registry.web.dto.HomePageDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/public")
class PublicController(
    private val tenantRepository: TenantRepository,
    private val contentService: ContentService
) {
    
    @GetMapping("/{tenantSlug}/home")
    fun getHomePage(@PathVariable tenantSlug: String): ResponseEntity<HomePageDto> {
        val tenant = tenantRepository.findBySlug(tenantSlug)
            ?: return ResponseEntity.notFound().build()
        
        val blocks = contentService.findAllByTenant(tenant.id)
            .map { ContentBlockDto(it.id, it.tenantId, it.key, it.published, it.updatedAt) }
        
        val homePage = HomePageDto(
            tenantSlug = tenant.slug,
            tenantName = tenant.name,
            theme = tenant.theme,
            blocks = blocks
        )
        
        return ResponseEntity.ok(homePage)
    }
}

