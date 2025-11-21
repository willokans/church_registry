package com.example.registry.web.controller

import com.example.registry.domain.entity.AppUser
import com.example.registry.domain.entity.Tenant
import com.example.registry.repo.AppUserRepository
import com.example.registry.service.TenantService
import com.example.registry.web.dto.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class TenantControllerTest {

    @Mock
    private lateinit var tenantService: TenantService

    @Mock
    private lateinit var appUserRepository: AppUserRepository

    @InjectMocks
    private lateinit var tenantController: TenantController

    private lateinit var testTenant: Tenant
    private lateinit var testUser: AppUser
    private lateinit var jwt: Jwt
    private lateinit var authentication: JwtAuthenticationToken

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

        testUser = AppUser(
            id = 1L,
            email = "test@example.com",
            fullName = "Test User",
            createdAt = Instant.now()
        )

        jwt = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .claim("sub", "test@example.com")
            .claim("email", "test@example.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        authentication = JwtAuthenticationToken(jwt, emptyList())
    }

    @Test
    fun `should get all tenants`() {
        // Given
        val tenants = listOf(
            testTenant,
            Tenant(id = 2L, slug = "another-tenant", name = "Another Tenant", createdAt = Instant.now())
        )
        `when`(tenantService.findAll()).thenReturn(tenants)

        // When
        val response = tenantController.getAllTenants()

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull()
        assertThat(response.body!!).hasSize(2)
        assertThat(response.body!![0].id).isEqualTo(1L)
        assertThat(response.body!![0].slug).isEqualTo("test-tenant")
        assertThat(response.body!![1].id).isEqualTo(2L)
        verify(tenantService).findAll()
    }

    @Test
    fun `should get tenant by id`() {
        // Given
        `when`(tenantService.findById(1L)).thenReturn(testTenant)

        // When
        val response = tenantController.getTenantById(1L)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull()
        assertThat(response.body!!.id).isEqualTo(1L)
        assertThat(response.body!!.slug).isEqualTo("test-tenant")
        assertThat(response.body!!.name).isEqualTo("Test Tenant")
        verify(tenantService).findById(1L)
    }

    @Test
    fun `should throw exception when tenant not found by id`() {
        // Given
        `when`(tenantService.findById(999L)).thenReturn(null)

        // When/Then
        assertThatThrownBy {
            tenantController.getTenantById(999L)
        }.isInstanceOf(NoSuchElementException::class.java)
            .hasMessageContaining("Tenant not found")

        verify(tenantService).findById(999L)
    }

    @Test
    fun `should get tenant by slug`() {
        // Given
        `when`(tenantService.findBySlug("test-tenant")).thenReturn(testTenant)

        // When
        val response = tenantController.getTenantBySlug("test-tenant")

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull()
        assertThat(response.body!!.slug).isEqualTo("test-tenant")
        verify(tenantService).findBySlug("test-tenant")
    }

    @Test
    fun `should throw exception when tenant not found by slug`() {
        // Given
        `when`(tenantService.findBySlug("non-existent")).thenReturn(null)

        // When/Then
        assertThatThrownBy {
            tenantController.getTenantBySlug("non-existent")
        }.isInstanceOf(NoSuchElementException::class.java)
            .hasMessageContaining("Tenant not found")

        verify(tenantService).findBySlug("non-existent")
    }

    @Test
    fun `should create tenant successfully with authentication`() {
        // Given
        val request = CreateTenantRequest(
            slug = "new-tenant",
            name = "New Tenant",
            parentId = null,
            theme = mapOf("primaryColor" to "#ff0000")
        )

        val createdTenant = testTenant.copy(
            slug = request.slug,
            name = request.name,
            theme = request.theme
        )

        `when`(appUserRepository.findByEmail("test@example.com")).thenReturn(testUser)
        `when`(tenantService.createTenant(
            slug = request.slug,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            createdBy = testUser.id
        )).thenReturn(createdTenant)

        // When
        val response = tenantController.createTenant(request, authentication)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull()
        assertThat(response.body!!.slug).isEqualTo("new-tenant")
        assertThat(response.body!!.name).isEqualTo("New Tenant")
        assertThat(response.body!!.theme).isEqualTo(request.theme)
        
        verify(appUserRepository).findByEmail("test@example.com")
        verify(tenantService).createTenant(
            slug = request.slug,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            createdBy = testUser.id
        )
    }

    @Test
    fun `should create tenant successfully without authentication`() {
        // Given
        val request = CreateTenantRequest(
            slug = "new-tenant",
            name = "New Tenant"
        )

        val createdTenant = testTenant.copy(
            slug = request.slug,
            name = request.name
        )

        `when`(tenantService.createTenant(
            slug = request.slug,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            createdBy = null
        )).thenReturn(createdTenant)

        // When
        val response = tenantController.createTenant(request, null)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull()
        assertThat(response.body!!.slug).isEqualTo("new-tenant")
        
        verify(appUserRepository, never()).findByEmail(anyString())
        verify(tenantService).createTenant(
            slug = request.slug,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            createdBy = null
        )
    }

    @Test
    fun `should create tenant with user email from subject when email claim missing`() {
        // Given
        val jwtWithSubject = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .claim("sub", "subject@example.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        val authWithSubject = JwtAuthenticationToken(jwtWithSubject, emptyList())

        val request = CreateTenantRequest(
            slug = "new-tenant",
            name = "New Tenant"
        )

        val createdTenant = testTenant.copy(
            slug = request.slug,
            name = request.name
        )

        `when`(appUserRepository.findByEmail("subject@example.com")).thenReturn(testUser)
        `when`(tenantService.createTenant(
            slug = request.slug,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            createdBy = testUser.id
        )).thenReturn(createdTenant)

        // When
        val response = tenantController.createTenant(request, authWithSubject)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(appUserRepository).findByEmail("subject@example.com")
    }

    @Test
    fun `should create tenant with null createdBy when user not found`() {
        // Given
        val request = CreateTenantRequest(
            slug = "new-tenant",
            name = "New Tenant"
        )

        val createdTenant = testTenant.copy(
            slug = request.slug,
            name = request.name
        )

        `when`(appUserRepository.findByEmail("test@example.com")).thenReturn(null)
        `when`(tenantService.createTenant(
            slug = request.slug,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            createdBy = null
        )).thenReturn(createdTenant)

        // When
        val response = tenantController.createTenant(request, authentication)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(tenantService).createTenant(
            slug = request.slug,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            createdBy = null
        )
    }

    @Test
    fun `should update tenant successfully`() {
        // Given
        val request = UpdateTenantRequest(
            name = "Updated Tenant Name",
            parentId = null,
            theme = mapOf("primaryColor" to "#00ff00")
        )

        val updatedTenant = testTenant.copy(
            name = request.name ?: testTenant.name,
            theme = request.theme ?: testTenant.theme
        )

        `when`(appUserRepository.findByEmail("test@example.com")).thenReturn(testUser)
        `when`(tenantService.updateTenant(
            id = 1L,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            updatedBy = testUser.id
        )).thenReturn(updatedTenant)

        // When
        val response = tenantController.updateTenant(1L, request, authentication)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull()
        assertThat(response.body!!.name).isEqualTo("Updated Tenant Name")
        assertThat(response.body!!.theme).isEqualTo(request.theme)
        
        verify(appUserRepository).findByEmail("test@example.com")
        verify(tenantService).updateTenant(
            id = 1L,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            updatedBy = testUser.id
        )
    }

    @Test
    fun `should update tenant with partial fields`() {
        // Given
        val request = UpdateTenantRequest(
            name = "Updated Name",
            parentId = null,
            theme = null
        )

        val updatedTenant = testTenant.copy(name = request.name ?: testTenant.name)

        `when`(appUserRepository.findByEmail("test@example.com")).thenReturn(testUser)
        `when`(tenantService.updateTenant(
            id = 1L,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            updatedBy = testUser.id
        )).thenReturn(updatedTenant)

        // When
        val response = tenantController.updateTenant(1L, request, authentication)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.name).isEqualTo("Updated Name")
        verify(tenantService).updateTenant(
            id = 1L,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            updatedBy = testUser.id
        )
    }

    @Test
    fun `should update tenant with null updatedBy when user not found`() {
        // Given
        val request = UpdateTenantRequest(name = "Updated Name")

        val updatedTenant = testTenant.copy(name = request.name ?: testTenant.name)

        `when`(appUserRepository.findByEmail("test@example.com")).thenReturn(null)
        `when`(tenantService.updateTenant(
            id = 1L,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            updatedBy = null
        )).thenReturn(updatedTenant)

        // When
        val response = tenantController.updateTenant(1L, request, authentication)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(tenantService).updateTenant(
            id = 1L,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            updatedBy = null
        )
    }

    @Test
    fun `should handle exception when updating tenant`() {
        // Given
        val request = UpdateTenantRequest(name = "Updated Name")

        `when`(appUserRepository.findByEmail("test@example.com")).thenThrow(RuntimeException("Database error"))

        `when`(tenantService.updateTenant(
            id = 1L,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            updatedBy = null
        )).thenReturn(testTenant)

        // When
        val response = tenantController.updateTenant(1L, request, authentication)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(tenantService).updateTenant(
            id = 1L,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            updatedBy = null
        )
    }

    @Test
    fun `should handle non-JwtAuthenticationToken authentication gracefully`() {
        // Given
        val request = CreateTenantRequest(
            slug = "new-tenant",
            name = "New Tenant"
        )

        val createdTenant = testTenant.copy(
            slug = request.slug,
            name = request.name
        )

        val mockAuth: Authentication = mock(Authentication::class.java)

        `when`(tenantService.createTenant(
            slug = request.slug,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            createdBy = null
        )).thenReturn(createdTenant)

        // When
        val response = tenantController.createTenant(request, mockAuth)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(tenantService).createTenant(
            slug = request.slug,
            name = request.name,
            parentId = request.parentId,
            theme = request.theme,
            createdBy = null
        )
    }
}

