package com.example.registry.config

import com.example.registry.domain.Role
import com.example.registry.domain.entity.*
import com.example.registry.repo.*
import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.orm.jpa.EntityManagerFactoryUtils
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Component
@Profile("h2")
class H2DataInitializer(
    private val tenantRepository: TenantRepository,
    private val permissionRepository: PermissionRepository,
    private val rolePermissionRepository: RolePermissionRepository,
    private val entityManagerFactory: EntityManagerFactory
) : ApplicationListener<ApplicationReadyEvent> {
    
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        // Run in a separate thread to avoid blocking startup
        // and to ensure tables are fully created
        Thread {
            try {
                // Wait a bit for all tables to be created
                Thread.sleep(1000)
                seedDataIfNeeded()
            } catch (e: Exception) {
                System.err.println("⚠ Failed to seed H2 database: ${e.message}")
                // Don't crash the app - just log the error
            }
        }.start()
    }
    
    @Transactional
    private fun seedDataIfNeeded() {
        // Only seed if database is empty - use retry logic with exponential backoff
        var retries = 10
        var delay = 500L
        while (retries > 0) {
            try {
                val count = tenantRepository.count()
                if (count == 0L) {
                    seedData()
                    println("✓ H2 database seeded successfully")
                }
                return // Success - exit the method
            } catch (e: Exception) {
                retries--
                if (retries == 0) {
                    // Last attempt failed - log error but don't crash the app
                    System.err.println("⚠ Failed to seed H2 database after all retries: ${e.message}")
                    // Don't throw - let the app continue without seed data
                } else {
                    Thread.sleep(delay)
                    delay = (delay * 1.5).toLong() // Exponential backoff
                }
            }
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

