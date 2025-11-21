package com.example.registry.integration

import com.example.registry.BaseIntegrationTest
import com.example.registry.domain.Role
import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import com.example.registry.repo.*
import com.example.registry.service.SacramentService
import com.example.registry.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

class SoftDeleteTest : BaseIntegrationTest() {
    
    @Autowired
    lateinit var tenantRepository: TenantRepository
    
    @Autowired
    lateinit var userService: UserService
    
    @Autowired
    lateinit var sacramentService: SacramentService
    
    @Autowired
    lateinit var sacramentEventRepository: SacramentEventRepository
    
    private var tenantId: Long = 0
    private var userId: Long = 0
    
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
        userService.grantMembership(userId, tenantId, Role.REGISTRAR, userId)
    }
    
    @Test
    fun `should soft delete sacrament event`() {
        val event = sacramentService.create(
            tenantId, SacramentType.BAPTISM, UUID.randomUUID(),
            LocalDate.now(), null, 1, 1, 1, userId
        )
        
        // Verify event is active
        val activeEvent = sacramentEventRepository.findById(event.id)
        assertThat(activeEvent).isPresent
        assertThat(activeEvent.get().status).isEqualTo(Status.ACTIVE)
        
        // Soft delete
        sacramentService.updateStatus(event.id, tenantId, Status.INACTIVE, "Test deactivation", userId)
        
        // Verify event is inactive
        val inactiveEvent = sacramentEventRepository.findById(event.id)
        assertThat(inactiveEvent).isPresent
        assertThat(inactiveEvent.get().status).isEqualTo(Status.INACTIVE)
        assertThat(inactiveEvent.get().deactivatedAt).isNotNull
        assertThat(inactiveEvent.get().deactivatedBy).isEqualTo(userId)
        assertThat(inactiveEvent.get().deactivationReason).isEqualTo("Test deactivation")
    }
    
    @Test
    fun `should prevent duplicate entries for active records only`() {
        val personId = UUID.randomUUID()
        
        val event1 = sacramentService.create(
            tenantId, SacramentType.BAPTISM, personId,
            LocalDate.now(), null, 1, 1, 1, userId
        )
        
        // Should fail - duplicate active entry
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            sacramentService.create(
                tenantId, SacramentType.BAPTISM, personId,
                LocalDate.now(), null, 1, 1, 1, userId
            )
        }
        
        // Soft delete first event
        sacramentService.updateStatus(event1.id, tenantId, Status.INACTIVE, "Deleted", userId)
        
        // Now should succeed - previous entry is inactive
        val event2 = sacramentService.create(
            tenantId, SacramentType.BAPTISM, personId,
            LocalDate.now(), null, 1, 1, 1, userId
        )
        
        assertThat(event2.id).isNotEqualTo(event1.id)
    }
}

