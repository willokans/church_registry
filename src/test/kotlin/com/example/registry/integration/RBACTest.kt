package com.example.registry.integration

import com.example.registry.BaseIntegrationTest
import com.example.registry.domain.Role
import com.example.registry.domain.Status
import com.example.registry.repo.*
import com.example.registry.security.AuthorizationService
import com.example.registry.service.UserService
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

class RBACTest : BaseIntegrationTest() {
    
    @Autowired
    lateinit var authorizationService: AuthorizationService
    
    @Autowired
    lateinit var userService: UserService
    
    @Autowired
    lateinit var tenantRepository: TenantRepository
    
    @Autowired
    lateinit var permissionRepository: PermissionRepository
    
    @Autowired
    lateinit var rolePermissionRepository: RolePermissionRepository
    
    @Autowired
    lateinit var membershipRepository: MembershipRepository
    
    @Autowired
    lateinit var entityManager: EntityManager
    
    @Autowired
    lateinit var cacheManager: CacheManager
    
    private var tenantId: Long = 0
    private var adminUserId: Long = 0
    private var viewerUserId: Long = 0
    private var adminEmail: String = ""
    private var viewerEmail: String = ""
    
    @BeforeEach
    @Transactional
    fun setup() {
        // Seed permissions if they don't exist
        val permissions = listOf(
            "users.manage",
            "users.view",
            "permissions.grant",
            "sacraments.create",
            "sacraments.update",
            "sacraments.view",
            "settings.edit",
            "audit.view"
        )
        permissions.forEach { key ->
            if (!permissionRepository.existsById(key)) {
                permissionRepository.save(com.example.registry.domain.entity.Permission(
                    key = key,
                    description = null,
                    category = null,
                    parentKey = null
                ))
            }
        }
        
        // Seed role permissions
        val allPermissions = permissions.toSet()
        val registrarPermissions = setOf("sacraments.create", "sacraments.update", "sacraments.view")
        val priestPermissions = setOf("sacraments.create", "sacraments.view")
        val viewerPermissions = setOf("sacraments.view", "users.view")
        
        // PARISH_ADMIN gets all permissions
        allPermissions.forEach { perm ->
            if (!rolePermissionRepository.existsByRoleAndPermissionKey(Role.PARISH_ADMIN, perm)) {
                rolePermissionRepository.save(com.example.registry.domain.entity.RolePermission(role = Role.PARISH_ADMIN, permissionKey = perm))
            }
        }
        
        // VIEWER permissions
        viewerPermissions.forEach { perm ->
            if (!rolePermissionRepository.existsByRoleAndPermissionKey(Role.VIEWER, perm)) {
                rolePermissionRepository.save(com.example.registry.domain.entity.RolePermission(role = Role.VIEWER, permissionKey = perm))
            }
        }
        
        val tenant = tenantRepository.save(
            com.example.registry.domain.entity.Tenant(
                slug = "test-tenant-${UUID.randomUUID()}",
                name = "Test Tenant"
            )
        )
        tenantId = tenant.id
        
        val uuid = UUID.randomUUID()
        adminEmail = "admin-$uuid@test.com"
        viewerEmail = "viewer-$uuid@test.com"
        val admin = userService.createUser(adminEmail, "Admin User")
        val viewer = userService.createUser(viewerEmail, "Viewer User")
        
        adminUserId = admin.id
        viewerUserId = viewer.id
        
        userService.grantMembership(adminUserId, tenantId, Role.PARISH_ADMIN, adminUserId)
        userService.grantMembership(viewerUserId, tenantId, Role.VIEWER, adminUserId)
        
        // Clear cache to ensure fresh data is loaded
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
    }
    
    @Test
    @Transactional
    fun `should grant permissions based on role`() {
        // Clear cache before test to ensure fresh data
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
        
        // Verify membership exists
        val membership = membershipRepository.findByUserIdAndTenantIdAndStatus(adminUserId, tenantId, Status.ACTIVE)
        assertThat(membership).isNotNull()
        assertThat(membership?.role).isEqualTo(Role.PARISH_ADMIN)
        
        // Verify permissions exist for the role
        val permissions = rolePermissionRepository.findAllByRole(Role.PARISH_ADMIN)
        assertThat(permissions).isNotEmpty()
        assertThat(permissions.map { it.permissionKey }).contains("users.manage")
        
        val adminJwt = createJwt(adminUserId, adminEmail)
        // Create authenticated JWT token with empty authorities (authorization is based on membership)
        val adminAuth = JwtAuthenticationToken(adminJwt, emptyList())
        
        assertThat(adminAuth.isAuthenticated).isTrue()
        assertThat(authorizationService.can(tenantId, "users.manage", adminAuth)).isTrue()
        assertThat(authorizationService.can(tenantId, "sacraments.create", adminAuth)).isTrue()
        assertThat(authorizationService.can(tenantId, "audit.view", adminAuth)).isTrue()
        
        val viewerJwt = createJwt(viewerUserId, viewerEmail)
        val viewerAuth = JwtAuthenticationToken(viewerJwt, emptyList())
        
        assertThat(authorizationService.can(tenantId, "sacraments.view", viewerAuth)).isTrue()
        assertThat(authorizationService.can(tenantId, "users.view", viewerAuth)).isTrue()
        assertThat(authorizationService.can(tenantId, "users.manage", viewerAuth)).isFalse()
        assertThat(authorizationService.can(tenantId, "sacraments.create", viewerAuth)).isFalse()
    }
    
    @Test
    fun `should deny access for users without membership`() {
        val otherUserId = 999L
        val otherJwt = createJwt(otherUserId)
        val otherAuth = JwtAuthenticationToken(otherJwt)
        
        assertThat(authorizationService.can(tenantId, "sacraments.view", otherAuth)).isFalse()
    }
    
    private fun createJwt(userId: Long, email: String = "user$userId@test.com"): Jwt {
        return Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .claim("sub", userId.toString())
            .claim("email", email) // Add email claim for lookup
            .claim("jti", UUID.randomUUID().toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
    }
}

