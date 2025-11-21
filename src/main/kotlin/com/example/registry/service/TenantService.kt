package com.example.registry.service

import com.example.registry.domain.entity.Tenant
import com.example.registry.repo.TenantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class TenantService(
    private val tenantRepository: TenantRepository,
    private val auditService: AuditService
) {
    
    open fun findById(id: Long): Tenant? = tenantRepository.findById(id).orElse(null)
    
    open fun findBySlug(slug: String): Tenant? = tenantRepository.findBySlug(slug)
    
    open fun findAll(): List<Tenant> = tenantRepository.findAll()
    
    @Transactional
    open fun createTenant(
        slug: String,
        name: String,
        parentId: Long? = null,
        theme: Map<String, Any>? = null,
        createdBy: Long? = null
    ): Tenant {
        // Check for duplicate slug
        if (tenantRepository.existsBySlug(slug)) {
            throw IllegalArgumentException("Tenant with slug '$slug' already exists")
        }
        
        // Check for duplicate name
        if (tenantRepository.existsByName(name)) {
            throw IllegalArgumentException("Tenant with name '$name' already exists")
        }
        
        val tenant = Tenant(
            slug = slug,
            name = name,
            parentId = parentId,
            theme = theme
        )
        
        val saved = tenantRepository.save(tenant)
        auditService.log(null, createdBy, "CREATE", "Tenant", saved.id.toString(), null, saved)
        return saved
    }
    
    @Transactional
    open fun updateTenant(
        id: Long,
        name: String? = null,
        parentId: Long? = null,
        theme: Map<String, Any>? = null,
        updatedBy: Long? = null
    ): Tenant {
        val existing = tenantRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Tenant not found") }
        
        // Check for duplicate name if name is being updated
        if (name != null && name != existing.name) {
            val existingWithName = tenantRepository.findByName(name)
            if (existingWithName != null && existingWithName.id != id) {
                throw IllegalArgumentException("Tenant with name '$name' already exists")
            }
        }
        
        val updated = existing.copy(
            name = name ?: existing.name,
            parentId = parentId ?: existing.parentId,
            theme = theme ?: existing.theme
        )
        
        val saved = tenantRepository.save(updated)
        auditService.log(null, updatedBy, "UPDATE", "Tenant", saved.id.toString(), existing, saved)
        return saved
    }
}

