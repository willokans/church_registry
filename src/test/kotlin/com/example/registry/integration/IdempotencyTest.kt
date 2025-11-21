package com.example.registry.integration

import com.example.registry.BaseIntegrationTest
import com.example.registry.repo.*
import com.example.registry.service.IdempotencyService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.*

class IdempotencyTest : BaseIntegrationTest() {
    
    @Autowired
    lateinit var idempotencyService: IdempotencyService
    
    @Autowired
    lateinit var tenantRepository: TenantRepository
    
    private var tenantId: Long = 0
    
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
    }
    
    @Test
    fun `should detect duplicate idempotency keys`() {
        val key = UUID.randomUUID().toString()
        val requestBody = """{"type":"BAPTISM","personId":"123"}"""
        
        val result1 = idempotencyService.checkAndStore(tenantId, key, requestBody)
        assertThat(result1.isDuplicate).isFalse()
        
        val result2 = idempotencyService.checkAndStore(tenantId, key, requestBody)
        assertThat(result2.isDuplicate).isTrue()
    }
    
    @Test
    fun `should record response code for idempotency key`() {
        val key = UUID.randomUUID().toString()
        val requestBody = """{"type":"BAPTISM"}"""
        
        idempotencyService.checkAndStore(tenantId, key, requestBody)
        idempotencyService.recordResponse(tenantId, key, 201)
        
        val result = idempotencyService.checkAndStore(tenantId, key, requestBody)
        assertThat(result.isDuplicate).isTrue()
        assertThat(result.responseCode).isEqualTo(201)
    }
}

