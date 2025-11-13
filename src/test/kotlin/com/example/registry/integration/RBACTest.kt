package com.example.registry.integration

import com.example.registry.BaseIntegrationTest
import com.example.registry.domain.Role
import com.example.registry.repo.*
import com.example.registry.security.AuthorizationService
import com.example.registry.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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
    
    private lateinit var tenantId: UUID
    private lateinit var adminUserId: UUID
    private lateinit var viewerUserId: UUID
    
    @BeforeEach
    @Transactional
    fun setup() {
        val tenant = tenantRepository.save(
            com.example.registry.domain.entity.Tenant(
                slug = "test-tenant",
                name = "Test Tenant"
            )
        )
        tenantId = tenant.id
        
        val admin = userService.createUser("admin@test.com", "Admin User")
        val viewer = userService.createUser("viewer@test.com", "Viewer User")
        
        adminUserId = admin.id
        viewerUserId = viewer.id
        
        userService.grantMembership(adminUserId, tenantId, Role.PARISH_ADMIN, adminUserId)
        userService.grantMembership(viewerUserId, tenantId, Role.VIEWER, adminUserId)
    }
    
    @Test
    fun `should grant permissions based on role`() {
        val adminJwt = createJwt(adminUserId)
        val adminAuth = JwtAuthenticationToken(adminJwt)
        
        assertThat(authorizationService.can(tenantId, "users.manage", adminAuth)).isTrue()
        assertThat(authorizationService.can(tenantId, "sacraments.create", adminAuth)).isTrue()
        assertThat(authorizationService.can(tenantId, "audit.view", adminAuth)).isTrue()
        
        val viewerJwt = createJwt(viewerUserId)
        val viewerAuth = JwtAuthenticationToken(viewerJwt)
        
        assertThat(authorizationService.can(tenantId, "sacraments.view", viewerAuth)).isTrue()
        assertThat(authorizationService.can(tenantId, "users.view", viewerAuth)).isTrue()
        assertThat(authorizationService.can(tenantId, "users.manage", viewerAuth)).isFalse()
        assertThat(authorizationService.can(tenantId, "sacraments.create", viewerAuth)).isFalse()
    }
    
    @Test
    fun `should deny access for users without membership`() {
        val otherUserId = UUID.randomUUID()
        val otherJwt = createJwt(otherUserId)
        val otherAuth = JwtAuthenticationToken(otherJwt)
        
        assertThat(authorizationService.can(tenantId, "sacraments.view", otherAuth)).isFalse()
    }
    
    private fun createJwt(userId: UUID): Jwt {
        return Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .claim("sub", userId.toString())
            .claim("jti", UUID.randomUUID().toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
    }
}

