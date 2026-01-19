package com.example.registry.integration

import com.example.registry.BaseIntegrationTest
import com.example.registry.domain.Role
import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import com.example.registry.domain.entity.SacramentEvent
import com.example.registry.repo.*
import com.example.registry.service.SacramentService
import com.example.registry.service.UserService
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
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
    
    @Autowired
    lateinit var entityManager: EntityManager
    
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
    @Transactional
    fun `should soft delete sacrament event`() {
        val event = sacramentService.create(
            tenantId, SacramentType.BAPTISM, UUID.randomUUID(),
            LocalDate.now(), "Rev. Test Priest", 1, 1, 1, userId
        )
        
        // Verify event is active
        val activeEvent = sacramentEventRepository.findById(event.id)
        assertThat(activeEvent).isPresent
        assertThat(activeEvent.get().status).isEqualTo(Status.ACTIVE)
        
        // Soft delete
        sacramentService.updateStatus(event.id, tenantId, Status.INACTIVE, "Test deactivation", userId)
        
        // Verify event is inactive - use native query to bypass @Where filter
        entityManager.flush() // Ensure update is persisted
        entityManager.clear() // Clear cache to ensure we get fresh data
        
        // Use native query with explicit column mapping to bypass @Where filter
        // All column names must be quoted for H2 with globally_quoted_identifiers
        val query = entityManager.createNativeQuery(
            """
            SELECT "id", "tenant_id", "type", "person_id", "date", "priest_name", "book_no", "page_no", "entry_no", 
                   "status", "created_by", "created_at", "updated_by", "updated_at", 
                   "deactivated_at", "deactivated_by", "deactivation_reason"
            FROM "sacrament_events" 
            WHERE "id" = :id
            """.trimIndent()
        )
        query.setParameter("id", event.id)
        val results = query.resultList
        assertThat(results).isNotEmpty().withFailMessage("Query should return the soft-deleted event")
        val result = results[0] as Array<Any>
        
        // Map result array to entity fields (H2 returns Object[] for native queries)
        // Column order: id(0), tenant_id(1), type(2), person_id(3), date(4), priest_name(5), 
        // book_no(6), page_no(7), entry_no(8), status(9), created_by(10), created_at(11),
        // updated_by(12), updated_at(13), deactivated_at(14), deactivated_by(15), deactivation_reason(16)
        val inactiveEventId = (result[0] as Number).toLong()
        val inactiveStatus = Status.valueOf(result[9] as String)
        // deactivated_at is at index 14, deactivated_by at 15, deactivation_reason at 16
        // H2 returns Timestamp for Instant fields in native queries
        val inactiveDeactivatedAt = when (val value = result[14]) {
            is Instant -> value
            is java.sql.Timestamp -> value.toInstant()
            is java.time.LocalDateTime -> value.atZone(java.time.ZoneId.of("UTC")).toInstant()
            is String -> try { Instant.parse(value) } catch (e: Exception) { null }
            null -> null
            else -> {
                // Try to convert via toString and parse
                try {
                    val str = value.toString()
                    if (str.contains("T") && str.contains("Z")) {
                        Instant.parse(str)
                    } else {
                        java.sql.Timestamp.valueOf(str).toInstant()
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
        val inactiveDeactivatedBy = (result[15] as? Number)?.toLong()
        val inactiveDeactivationReason = result[16] as? String
        
        assertThat(inactiveEventId).isEqualTo(event.id)
        assertThat(inactiveStatus).isEqualTo(Status.INACTIVE)
        assertThat(inactiveDeactivatedAt).isNotNull().withFailMessage("deactivatedAt should be set after soft delete, got: ${result[14]}")
        assertThat(inactiveDeactivatedBy).isEqualTo(userId)
        assertThat(inactiveDeactivationReason).isEqualTo("Test deactivation")
    }
    
    @Test
    fun `should prevent duplicate entries for active records only`() {
        val personId = UUID.randomUUID()
        
        val event1 = sacramentService.create(
            tenantId, SacramentType.BAPTISM, personId,
            LocalDate.now(), "Rev. Test Priest", 1, 1, 1, userId
        )
        
        // Should fail - duplicate active entry
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            sacramentService.create(
                tenantId, SacramentType.BAPTISM, personId,
                LocalDate.now(), "Rev. Test Priest", 1, 1, 1, userId
            )
        }
        
        // Soft delete first event
        sacramentService.updateStatus(event1.id, tenantId, Status.INACTIVE, "Deleted", userId)
        
        // Now should succeed - previous entry is inactive
        val event2 = sacramentService.create(
            tenantId, SacramentType.BAPTISM, personId,
            LocalDate.now(), "Rev. Test Priest", 1, 1, 1, userId
        )
        
        assertThat(event2.id).isNotEqualTo(event1.id)
    }
}

