package com.example.registry.integration

import com.example.registry.BaseIntegrationTest
import com.example.registry.domain.Role
import com.example.registry.domain.SacramentType
import com.example.registry.repo.*
import com.example.registry.service.SacramentService
import com.example.registry.service.UserService
import com.example.registry.util.ETagUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

class ETagTest : BaseIntegrationTest() {
    
    @Autowired
    lateinit var tenantRepository: TenantRepository
    
    @Autowired
    lateinit var userService: UserService
    
    @Autowired
    lateinit var sacramentService: SacramentService
    
    private lateinit var tenantId: UUID
    private lateinit var userId: UUID
    
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
        
        val user = userService.createUser("user@test.com", "Test User")
        userId = user.id
        userService.grantMembership(userId, tenantId, Role.VIEWER, userId)
    }
    
    @Test
    fun `should generate consistent ETags`() {
        val content = "test content"
        val etag1 = ETagUtil.generateETag(content)
        val etag2 = ETagUtil.generateETag(content)
        
        assertThat(etag1).isEqualTo(etag2)
        assertThat(etag1).startsWith("\"")
        assertThat(etag1).endsWith("\"")
    }
    
    @Test
    fun `should match ETags correctly`() {
        val etag = "\"abc123\""
        val ifNoneMatch1 = "\"abc123\""
        val ifNoneMatch2 = "\"xyz789\", \"abc123\""
        val ifNoneMatch3 = "\"different\""
        
        assertThat(ETagUtil.matches(etag, ifNoneMatch1)).isTrue()
        assertThat(ETagUtil.matches(etag, ifNoneMatch2)).isTrue()
        assertThat(ETagUtil.matches(etag, ifNoneMatch3)).isFalse()
    }
}

