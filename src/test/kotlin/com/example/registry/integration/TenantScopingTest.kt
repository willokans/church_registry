package com.example.registry.integration

import com.example.registry.BaseIntegrationTest
import com.example.registry.domain.Role
import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import com.example.registry.repo.*
import com.example.registry.service.SacramentService
import com.example.registry.service.UserService
import com.example.registry.tenancy.TenantContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

class TenantScopingTest : BaseIntegrationTest() {
    
    @Autowired
    lateinit var tenantRepository: TenantRepository
    
    @Autowired
    lateinit var userService: UserService
    
    @Autowired
    lateinit var sacramentService: SacramentService
    
    @Autowired
    lateinit var sacramentEventRepository: SacramentEventRepository
    
    private var tenant1: Long = 0
    private var tenant2: Long = 0
    private var user1: Long = 0
    private var user2: Long = 0
    
    @BeforeEach
    @Transactional
    fun setup() {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val t1 = tenantRepository.save(
            com.example.registry.domain.entity.Tenant(
                slug = "tenant1-$uuid1",
                name = "Tenant 1"
            )
        )
        val t2 = tenantRepository.save(
            com.example.registry.domain.entity.Tenant(
                slug = "tenant2-$uuid2",
                name = "Tenant 2"
            )
        )
        tenant1 = t1.id
        tenant2 = t2.id
        
        val uuid = UUID.randomUUID()
        val u1 = userService.createUser("user1-$uuid@test.com", "User 1")
        val u2 = userService.createUser("user2-$uuid@test.com", "User 2")
        user1 = u1.id
        user2 = u2.id
        
        userService.grantMembership(user1, tenant1, Role.REGISTRAR, user1)
        userService.grantMembership(user2, tenant2, Role.REGISTRAR, user2)
    }
    
    @Test
    fun `should enforce tenant scoping on sacrament events`() {
        TenantContext.set(tenant1)
        
        val event1 = sacramentService.create(
            tenant1, SacramentType.BAPTISM, UUID.randomUUID(),
            LocalDate.now(), "Rev. Test Priest 1", 1, 1, 1, user1
        )
        
        TenantContext.set(tenant2)
        val event2 = sacramentService.create(
            tenant2, SacramentType.BAPTISM, UUID.randomUUID(),
            LocalDate.now(), "Rev. Test Priest 2", 1, 1, 1, user2
        )
        
        TenantContext.set(tenant1)
        val events1 = sacramentService.findAll(tenant1, null, null, null, null, null, 100)
        assertThat(events1.content).hasSize(1)
        assertThat(events1.content[0].id).isEqualTo(event1.id)
        
        TenantContext.set(tenant2)
        val events2 = sacramentService.findAll(tenant2, null, null, null, null, null, 100)
        assertThat(events2.content).hasSize(1)
        assertThat(events2.content[0].id).isEqualTo(event2.id)
    }
}

