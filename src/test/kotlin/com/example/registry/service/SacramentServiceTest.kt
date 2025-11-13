package com.example.registry.service

import com.example.registry.BaseIntegrationTest
import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import com.example.registry.repo.SacramentEventRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*

class SacramentServiceTest : BaseIntegrationTest() {
    
    @Autowired
    lateinit var sacramentService: SacramentService
    
    @Autowired
    lateinit var sacramentEventRepository: SacramentEventRepository
    
    @Test
    fun `should create sacrament event`() {
        val tenantId = UUID.randomUUID()
        val personId = UUID.randomUUID()
        val createdBy = UUID.randomUUID()
        
        val event = sacramentService.create(
            tenantId = tenantId,
            type = SacramentType.BAPTISM,
            personId = personId,
            date = LocalDate.now(),
            ministerId = null,
            bookNo = 1,
            pageNo = 1,
            entryNo = 1,
            createdBy = createdBy
        )
        
        assertThat(event).isNotNull()
        assertThat(event.tenantId).isEqualTo(tenantId)
        assertThat(event.type).isEqualTo(SacramentType.BAPTISM)
        assertThat(event.status).isEqualTo(Status.ACTIVE)
    }
    
    @Test
    fun `should prevent duplicate entries`() {
        val tenantId = UUID.randomUUID()
        val personId = UUID.randomUUID()
        val createdBy = UUID.randomUUID()
        
        sacramentService.create(
            tenantId, SacramentType.BAPTISM, personId, LocalDate.now(),
            null, 1, 1, 1, createdBy
        )
        
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            sacramentService.create(
                tenantId, SacramentType.BAPTISM, personId, LocalDate.now(),
                null, 1, 1, 1, createdBy
            )
        }
    }
}

