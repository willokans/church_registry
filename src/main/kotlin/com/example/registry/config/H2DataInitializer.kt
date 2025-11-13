package com.example.registry.config

import com.example.registry.domain.Role
import com.example.registry.domain.entity.*
import com.example.registry.repo.*
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Component
@org.springframework.context.annotation.Profile("h2")
class H2DataInitializer(
    private val tenantRepository: TenantRepository,
    private val permissionRepository: PermissionRepository,
    private val rolePermissionRepository: RolePermissionRepository
) {
    
    @PostConstruct
    @Transactional
    fun init() {
        // Only seed if database is empty
        if (tenantRepository.count() == 0L) {
            seedData()
        }
    }
    
    private fun seedData() {
        // Create sample tenant
        val tenant = Tenant(
            id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            slug = "sample-parish",
            name = "Sample Parish",
            theme = mapOf("primaryColor" to "#0066cc", "logo" to "/logo.png"),
            createdAt = Instant.now()
        )
        tenantRepository.save(tenant)
        
        // Create permissions
        val permissions = listOf(
            "users.manage",
            "users.view",
            "permissions.grant",
            "sacraments.create",
            "sacraments.update",
            "sacraments.view",
            "settings.edit",
            "audit.view"
        ).map { Permission(key = it) }
        permissionRepository.saveAll(permissions)
        
        // Create role permissions
        val allPermissions = permissions.map { it.key }.toSet()
        val registrarPermissions = setOf("sacraments.create", "sacraments.update", "sacraments.view")
        val priestPermissions = setOf("sacraments.create", "sacraments.view")
        val viewerPermissions = setOf("sacraments.view", "users.view")
        
        val rolePermissions = mutableListOf<RolePermission>()
        
        // SUPER_ADMIN and PARISH_ADMIN get all permissions
        listOf(Role.SUPER_ADMIN, Role.PARISH_ADMIN).forEach { role ->
            allPermissions.forEach { perm ->
                rolePermissions.add(RolePermission(role = role, permissionKey = perm))
            }
        }
        
        // REGISTRAR permissions
        registrarPermissions.forEach { perm ->
            rolePermissions.add(RolePermission(role = Role.REGISTRAR, permissionKey = perm))
        }
        
        // PRIEST permissions
        priestPermissions.forEach { perm ->
            rolePermissions.add(RolePermission(role = Role.PRIEST, permissionKey = perm))
        }
        
        // VIEWER permissions
        viewerPermissions.forEach { perm ->
            rolePermissions.add(RolePermission(role = Role.VIEWER, permissionKey = perm))
        }
        
        rolePermissionRepository.saveAll(rolePermissions)
    }
}

