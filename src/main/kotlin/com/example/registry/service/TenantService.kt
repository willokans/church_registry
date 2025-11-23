package com.example.registry.service

import com.example.registry.domain.Role
import com.example.registry.domain.entity.Tenant
import com.example.registry.repo.AppUserRepository
import com.example.registry.repo.TenantRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class TenantService(
    private val tenantRepository: TenantRepository,
    private val auditService: AuditService
) {
    @Autowired(required = false)
    private var userService: UserService? = null
    
    @Autowired(required = false)
    private var appUserRepository: AppUserRepository? = null
    
    @Autowired(required = false)
    private var environment: Environment? = null
    
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
        
        // In H2 profile, automatically grant super-admin membership to new tenant
        grantSuperAdminMembershipToNewTenant(saved.id)
        
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
    
    /**
     * In H2 profile, automatically grant super-admin membership to newly created tenant.
     * This ensures super-admin can access all tenants in local development.
     */
    private fun grantSuperAdminMembershipToNewTenant(tenantId: Long) {
        try {
            // Only run in H2 profile
            val isH2Profile = environment?.activeProfiles?.contains("h2") == true
            if (!isH2Profile) {
                return
            }
            
            val userService = this.userService
            val appUserRepository = this.appUserRepository
            
            if (userService != null && appUserRepository != null) {
                val superAdminUser = appUserRepository.findByEmail("super-admin@test.com")
                if (superAdminUser != null) {
                    try {
                        userService.grantMembership(
                            userId = superAdminUser.id,
                            tenantId = tenantId,
                            role = Role.SUPER_ADMIN,
                            grantedBy = superAdminUser.id
                        )
                        println("✓ Auto-granted SUPER_ADMIN membership to new tenant (ID: $tenantId)")
                    } catch (e: Exception) {
                        // Membership might already exist, that's okay
                        println("⚠ Could not auto-grant membership to new tenant (ID: $tenantId): ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            // Silently fail - this is just a convenience feature for H2
            println("⚠ Could not auto-grant super-admin membership: ${e.message}")
        }
    }
}

