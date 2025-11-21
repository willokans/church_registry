package com.example.registry.service

import com.example.registry.domain.entity.Tenant
import com.example.registry.repo.TenantRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any as anyArg
import org.mockito.ArgumentMatchers.anyString as anyStringArg
import org.mockito.ArgumentMatchers.isNull as isNullArg
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class TenantServiceTest {

    @Mock
    private lateinit var tenantRepository: TenantRepository

    @Mock
    private lateinit var auditService: AuditService

    @InjectMocks
    private lateinit var tenantService: TenantService

    private lateinit var testTenant: Tenant

    @BeforeEach
    fun setup() {
        testTenant = Tenant(
            id = 1L,
            slug = "test-tenant",
            name = "Test Tenant",
            parentId = null,
            theme = mapOf("primaryColor" to "#0066cc"),
            createdAt = Instant.now()
        )
    }

    @Test
    fun `should find tenant by id`() {
        // Given
        `when`(tenantRepository.findById(1L)).thenReturn(Optional.of(testTenant))

        // When
        val result = tenantService.findById(1L)

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(1L)
        assertThat(result?.slug).isEqualTo("test-tenant")
        assertThat(result?.name).isEqualTo("Test Tenant")
        verify(tenantRepository).findById(1L)
    }

    @Test
    fun `should return null when tenant not found by id`() {
        // Given
        `when`(tenantRepository.findById(999L)).thenReturn(Optional.empty())

        // When
        val result = tenantService.findById(999L)

        // Then
        assertThat(result).isNull()
        verify(tenantRepository).findById(999L)
    }

    @Test
    fun `should find tenant by slug`() {
        // Given
        `when`(tenantRepository.findBySlug("test-tenant")).thenReturn(testTenant)

        // When
        val result = tenantService.findBySlug("test-tenant")

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.slug).isEqualTo("test-tenant")
        verify(tenantRepository).findBySlug("test-tenant")
    }

    @Test
    fun `should return null when tenant not found by slug`() {
        // Given
        `when`(tenantRepository.findBySlug("non-existent")).thenReturn(null)

        // When
        val result = tenantService.findBySlug("non-existent")

        // Then
        assertThat(result).isNull()
        verify(tenantRepository).findBySlug("non-existent")
    }

    @Test
    fun `should find all tenants`() {
        // Given
        val tenants = listOf(
            testTenant,
            Tenant(id = 2L, slug = "another-tenant", name = "Another Tenant", createdAt = Instant.now())
        )
        `when`(tenantRepository.findAll()).thenReturn(tenants)

        // When
        val result = tenantService.findAll()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].slug).isEqualTo("test-tenant")
        assertThat(result[1].slug).isEqualTo("another-tenant")
        verify(tenantRepository).findAll()
    }

    @Test
    fun `should create tenant successfully`() {
        // Given
        val slug = "new-tenant"
        val name = "New Tenant"
        val theme = mapOf("primaryColor" to "#ff0000")
        val createdBy = 1L

        `when`(tenantRepository.existsBySlug(slug)).thenReturn(false)
        `when`(tenantRepository.existsByName(name)).thenReturn(false)
        `when`(tenantRepository.save(any())).thenAnswer { invocation ->
            val tenant = invocation.getArgument<Tenant>(0)
            tenant.copy(id = 1L, createdAt = Instant.now())
        }

        // When
        val result = tenantService.createTenant(slug, name, null, theme, createdBy)

        // Then
        assertThat(result).isNotNull()
        assertThat(result.slug).isEqualTo(slug)
        assertThat(result.name).isEqualTo(name)
        assertThat(result.theme).isEqualTo(theme)
        
        val tenantCaptor = ArgumentCaptor.forClass(Tenant::class.java)
        verify(tenantRepository).save(tenantCaptor.capture())
        assertThat(tenantCaptor.value.slug).isEqualTo(slug)
        assertThat(tenantCaptor.value.name).isEqualTo(name)
        
        verify(auditService).log(null, createdBy, "CREATE", "Tenant", "1", null, result)
    }

    @Test
    fun `should throw exception when creating tenant with duplicate slug`() {
        // Given
        val slug = "existing-tenant"
        val name = "New Tenant"

        `when`(tenantRepository.existsBySlug(slug)).thenReturn(true)

        // When/Then
        assertThatThrownBy {
            tenantService.createTenant(slug, name)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Tenant with slug '$slug' already exists")

        verify(tenantRepository, never()).save(any<Tenant>())
        verify(auditService, never()).log(isNullArg(), isNullArg(), anyStringArg(), anyStringArg(), isNullArg(), isNullArg(), isNullArg())
    }

    @Test
    fun `should throw exception when creating tenant with duplicate name`() {
        // Given
        val slug = "new-tenant"
        val name = "Existing Tenant"

        `when`(tenantRepository.existsBySlug(slug)).thenReturn(false)
        `when`(tenantRepository.existsByName(name)).thenReturn(true)

        // When/Then
        assertThatThrownBy {
            tenantService.createTenant(slug, name)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Tenant with name '$name' already exists")

        verify(tenantRepository, never()).save(any<Tenant>())
        verify(auditService, never()).log(isNullArg(), isNullArg(), anyStringArg(), anyStringArg(), isNullArg(), isNullArg(), isNullArg())
    }

    @Test
    fun `should update tenant successfully`() {
        // Given
        val tenantId = 1L
        val newName = "Updated Tenant Name"
        val newTheme = mapOf("primaryColor" to "#00ff00")
        val updatedBy = 2L

        `when`(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant))
        `when`(tenantRepository.findByName(newName)).thenReturn(null)
        `when`(tenantRepository.save(any<Tenant>())).thenAnswer { invocation ->
            invocation.getArgument<Tenant>(0)
        }

        // When
        val result = tenantService.updateTenant(tenantId, newName, null, newTheme, updatedBy)

        // Then
        assertThat(result).isNotNull()
        assertThat(result.name).isEqualTo(newName)
        assertThat(result.theme).isEqualTo(newTheme)
        assertThat(result.slug).isEqualTo(testTenant.slug) // Slug should not change

        val tenantCaptor = ArgumentCaptor.forClass(Tenant::class.java)
        verify(tenantRepository).save(tenantCaptor.capture())
        assertThat(tenantCaptor.value.name).isEqualTo(newName)
        assertThat(tenantCaptor.value.theme).isEqualTo(newTheme)

        verify(auditService).log(null, updatedBy, "UPDATE", "Tenant", "1", testTenant, result)
    }

    @Test
    fun `should update tenant with partial fields`() {
        // Given
        val tenantId = 1L
        val newName = "Updated Name"

        `when`(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant))
        `when`(tenantRepository.save(any())).thenAnswer { invocation ->
            invocation.getArgument<Tenant>(0)
        }

        // When
        val result = tenantService.updateTenant(tenantId, newName, null, null, null)

        // Then
        assertThat(result.name).isEqualTo(newName)
        assertThat(result.theme).isEqualTo(testTenant.theme) // Theme should remain unchanged
        assertThat(result.parentId).isEqualTo(testTenant.parentId) // ParentId should remain unchanged
    }

    @Test
    fun `should throw exception when updating non-existent tenant`() {
        // Given
        val tenantId = 999L

        `when`(tenantRepository.findById(tenantId)).thenReturn(Optional.empty())

        // When/Then
        assertThatThrownBy {
            tenantService.updateTenant(tenantId, "New Name")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Tenant not found")

        verify(tenantRepository, never()).save(any<Tenant>())
        verify(auditService, never()).log(isNullArg(), isNullArg(), anyStringArg(), anyStringArg(), isNullArg(), isNullArg(), isNullArg())
    }

    @Test
    fun `should throw exception when updating tenant with duplicate name`() {
        // Given
        val tenantId = 1L
        val newName = "Duplicate Name"
        val existingTenantWithName = Tenant(
            id = 2L,
            slug = "other-tenant",
            name = newName,
            createdAt = Instant.now()
        )

        `when`(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant))
        `when`(tenantRepository.findByName(newName)).thenReturn(existingTenantWithName)

        // When/Then
        assertThatThrownBy {
            tenantService.updateTenant(tenantId, newName)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Tenant with name '$newName' already exists")

        verify(tenantRepository, never()).save(any<Tenant>())
        verify(auditService, never()).log(isNullArg(), isNullArg(), anyStringArg(), anyStringArg(), isNullArg(), isNullArg(), isNullArg())
    }

    @Test
    fun `should allow updating tenant with same name`() {
        // Given
        val tenantId = 1L
        val sameName = testTenant.name
        val newTheme = mapOf("primaryColor" to "#0000ff")

        `when`(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant))
        `when`(tenantRepository.save(any())).thenAnswer { invocation ->
            invocation.getArgument<Tenant>(0)
        }

        // When
        val result = tenantService.updateTenant(tenantId, sameName, null, newTheme, null)

        // Then
        assertThat(result.name).isEqualTo(sameName)
        assertThat(result.theme).isEqualTo(newTheme)
        verify(tenantRepository).save(any())
    }

    @Test
    fun `should update parentId`() {
        // Given
        val tenantId = 1L
        val newParentId = 5L

        `when`(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant))
        `when`(tenantRepository.save(any())).thenAnswer { invocation ->
            invocation.getArgument<Tenant>(0)
        }

        // When
        val result = tenantService.updateTenant(tenantId, null, newParentId, null, null)

        // Then
        assertThat(result.parentId).isEqualTo(newParentId)
        assertThat(result.name).isEqualTo(testTenant.name) // Name should remain unchanged
    }
}

