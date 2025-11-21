package com.example.registry.service

import com.example.registry.BaseIntegrationTest
import com.example.registry.domain.Role
import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import com.example.registry.repo.SacramentEventRepository
import com.example.registry.repo.TenantRepository
import com.example.registry.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

class SacramentServiceTest : BaseIntegrationTest() {
    
    @Autowired
    lateinit var sacramentService: SacramentService
    
    @Autowired
    lateinit var sacramentEventRepository: SacramentEventRepository
    
    @Autowired
    lateinit var tenantRepository: TenantRepository
    
    @Autowired
    lateinit var userService: UserService
    
    private var tenantId: Long = 0
    private var userId: Long = 0
    
    @BeforeEach
    @Transactional
    fun setup() {
        val tenant = tenantRepository.save(
            com.example.registry.domain.entity.Tenant(
                slug = "test-tenant-${UUID.randomUUID()}",
                name = "Test Tenant"
            )
        )
        tenantId = tenant.id
        
        val user = userService.createUser("user-${UUID.randomUUID()}@test.com", "Test User")
        userId = user.id
        userService.grantMembership(userId, tenantId, Role.REGISTRAR, userId)
    }
    
    @Test
    fun `should create sacrament event`() {
        val personId = UUID.randomUUID()
        
        val event = sacramentService.create(
            tenantId = tenantId,
            type = SacramentType.BAPTISM,
            personId = personId,
            date = LocalDate.now(),
            ministerId = null,
            bookNo = 1,
            pageNo = 1,
            entryNo = 1,
            createdBy = userId
        )
        
        assertThat(event).isNotNull()
        assertThat(event.tenantId).isEqualTo(tenantId)
        assertThat(event.type).isEqualTo(SacramentType.BAPTISM)
        assertThat(event.status).isEqualTo(Status.ACTIVE)
    }
    
    @Test
    fun `should prevent duplicate entries`() {
        val personId = UUID.randomUUID()
        
        sacramentService.create(
            tenantId, SacramentType.BAPTISM, personId, LocalDate.now(),
            null, 1, 1, 1, userId
        )
        
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            sacramentService.create(
                tenantId, SacramentType.BAPTISM, personId, LocalDate.now(),
                null, 1, 1, 1, userId
            )
        }
    }
}

