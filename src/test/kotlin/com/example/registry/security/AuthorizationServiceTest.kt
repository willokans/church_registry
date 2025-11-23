package com.example.registry.security

import com.example.registry.domain.PermissionCategory
import com.example.registry.domain.PermissionGroup
import com.example.registry.domain.Role
import com.example.registry.domain.entity.Permission
import com.example.registry.domain.entity.RolePermission
import com.example.registry.repo.AppUserRepository
import com.example.registry.repo.MembershipRepository
import com.example.registry.repo.PermissionRepository
import com.example.registry.repo.RolePermissionRepository
import com.example.registry.repo.TenantRolePermissionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.Mockito.*
import java.util.*

@ExtendWith(MockitoExtension::class)
class AuthorizationServiceTest {

    @Mock
    private lateinit var membershipRepository: MembershipRepository

    @Mock
    private lateinit var rolePermissionRepository: RolePermissionRepository

    @Mock
    private lateinit var tenantRolePermissionRepository: TenantRolePermissionRepository

    @Mock
    private lateinit var appUserRepository: AppUserRepository

    @Mock
    private lateinit var permissionRepository: PermissionRepository

    @InjectMocks
    private lateinit var authorizationService: AuthorizationService

    @BeforeEach
    fun setup() {
        // Setup common mocks
    }

    @Test
    fun `should group permissions by category`() {
        // Given
        val role = Role.PARISH_ADMIN
        val rolePermissions = listOf(
            RolePermission(role = role, permissionKey = "users.manage"),
            RolePermission(role = role, permissionKey = "users.view"),
            RolePermission(role = role, permissionKey = "permissions.grant"),
            RolePermission(role = role, permissionKey = "sacraments.create"),
            RolePermission(role = role, permissionKey = "sacraments.view"),
            RolePermission(role = role, permissionKey = "settings.edit"),
            RolePermission(role = role, permissionKey = "audit.view")
        )

        `when`(rolePermissionRepository.findAllByRole(role)).thenReturn(rolePermissions)

        // Mock Permission entities with categories
        `when`(permissionRepository.findById("users.manage")).thenReturn(
            Optional.of(Permission(key = "users.manage", category = PermissionCategory.USERS))
        )
        `when`(permissionRepository.findById("users.view")).thenReturn(
            Optional.of(Permission(key = "users.view", category = PermissionCategory.USERS))
        )
        `when`(permissionRepository.findById("permissions.grant")).thenReturn(
            Optional.of(Permission(key = "permissions.grant", category = PermissionCategory.USERS))
        )
        `when`(permissionRepository.findById("sacraments.create")).thenReturn(
            Optional.of(Permission(key = "sacraments.create", category = PermissionCategory.SACRAMENTS))
        )
        `when`(permissionRepository.findById("sacraments.view")).thenReturn(
            Optional.of(Permission(key = "sacraments.view", category = PermissionCategory.SACRAMENTS))
        )
        `when`(permissionRepository.findById("settings.edit")).thenReturn(
            Optional.of(Permission(key = "settings.edit", category = PermissionCategory.SETTINGS))
        )
        `when`(permissionRepository.findById("audit.view")).thenReturn(
            Optional.of(Permission(key = "audit.view", category = PermissionCategory.AUDIT))
        )

        // When
        val groups = authorizationService.getPermissionGroups(role)

        // Then
        assertThat(groups).hasSize(4)
        
        val auditGroup = groups.find { it.name == "Audit" }
        assertThat(auditGroup).isNotNull()
        assertThat(auditGroup?.permissions).containsExactly("audit.view")

        val sacramentsGroup = groups.find { it.name == "Sacraments" }
        assertThat(sacramentsGroup).isNotNull()
        assertThat(sacramentsGroup?.permissions).containsExactly("sacraments.create", "sacraments.view")

        val settingsGroup = groups.find { it.name == "Settings" }
        assertThat(settingsGroup).isNotNull()
        assertThat(settingsGroup?.permissions).containsExactly("settings.edit")

        val userManagementGroup = groups.find { it.name == "User Management" }
        assertThat(userManagementGroup).isNotNull()
        assertThat(userManagementGroup?.permissions).containsExactlyInAnyOrder(
            "permissions.grant", "users.manage", "users.view"
        )
    }

    @Test
    fun `should use fallback category inference when permission entity not found`() {
        // Given
        val role = Role.VIEWER
        val rolePermissions = listOf(
            RolePermission(role = role, permissionKey = "sacraments.view"),
            RolePermission(role = role, permissionKey = "users.view")
        )

        `when`(rolePermissionRepository.findAllByRole(role)).thenReturn(rolePermissions)

        // Mock Permission entities as not found (empty Optional)
        `when`(permissionRepository.findById("sacraments.view")).thenReturn(Optional.empty())
        `when`(permissionRepository.findById("users.view")).thenReturn(Optional.empty())

        // When
        val groups = authorizationService.getPermissionGroups(role)

        // Then
        assertThat(groups).hasSize(2)
        
        // Should infer categories from key patterns
        val sacramentsGroup = groups.find { it.name == "Sacraments" }
        assertThat(sacramentsGroup).isNotNull()
        assertThat(sacramentsGroup?.permissions).containsExactly("sacraments.view")

        val userManagementGroup = groups.find { it.name == "User Management" }
        assertThat(userManagementGroup).isNotNull()
        assertThat(userManagementGroup?.permissions).containsExactly("users.view")
    }

    @Test
    fun `should return empty list when role has no permissions`() {
        // Given
        val role = Role.VIEWER
        `when`(rolePermissionRepository.findAllByRole(role)).thenReturn(emptyList())

        // When
        val groups = authorizationService.getPermissionGroups(role)

        // Then
        assertThat(groups).isEmpty()
    }

    @Test
    fun `should sort groups by name and permissions within groups`() {
        // Given
        val role = Role.SUPER_ADMIN
        val rolePermissions = listOf(
            RolePermission(role = role, permissionKey = "audit.view"),
            RolePermission(role = role, permissionKey = "users.manage"),
            RolePermission(role = role, permissionKey = "sacraments.create"),
            RolePermission(role = role, permissionKey = "settings.edit")
        )

        `when`(rolePermissionRepository.findAllByRole(role)).thenReturn(rolePermissions)

        `when`(permissionRepository.findById("audit.view")).thenReturn(
            Optional.of(Permission(key = "audit.view", category = PermissionCategory.AUDIT))
        )
        `when`(permissionRepository.findById("users.manage")).thenReturn(
            Optional.of(Permission(key = "users.manage", category = PermissionCategory.USERS))
        )
        `when`(permissionRepository.findById("sacraments.create")).thenReturn(
            Optional.of(Permission(key = "sacraments.create", category = PermissionCategory.SACRAMENTS))
        )
        `when`(permissionRepository.findById("settings.edit")).thenReturn(
            Optional.of(Permission(key = "settings.edit", category = PermissionCategory.SETTINGS))
        )

        // When
        val groups = authorizationService.getPermissionGroups(role)

        // Then
        assertThat(groups).hasSize(4)
        // Groups should be sorted alphabetically by name
        assertThat(groups.map { it.name }).containsExactly("Audit", "Sacraments", "Settings", "User Management")
        
        // Permissions within each group should be sorted
        groups.forEach { group ->
            val sorted = group.permissions.sorted()
            assertThat(group.permissions).isEqualTo(sorted)
        }
    }

    @Test
    fun `should handle mixed permission sources with and without entities`() {
        // Given
        val role = Role.REGISTRAR
        val rolePermissions = listOf(
            RolePermission(role = role, permissionKey = "sacraments.create"),
            RolePermission(role = role, permissionKey = "sacraments.view"),
            RolePermission(role = role, permissionKey = "users.view")
        )

        `when`(rolePermissionRepository.findAllByRole(role)).thenReturn(rolePermissions)

        // One permission has entity, others don't
        `when`(permissionRepository.findById("sacraments.create")).thenReturn(
            Optional.of(Permission(key = "sacraments.create", category = PermissionCategory.SACRAMENTS))
        )
        `when`(permissionRepository.findById("sacraments.view")).thenReturn(Optional.empty())
        `when`(permissionRepository.findById("users.view")).thenReturn(Optional.empty())

        // When
        val groups = authorizationService.getPermissionGroups(role)

        // Then
        assertThat(groups).hasSize(2) // Sacraments and User Management
        
        val sacramentsGroup = groups.find { it.name == "Sacraments" }
        assertThat(sacramentsGroup).isNotNull()
        assertThat(sacramentsGroup?.permissions).containsExactlyInAnyOrder(
            "sacraments.create", "sacraments.view"
        )
        
        val userManagementGroup = groups.find { it.name == "User Management" }
        assertThat(userManagementGroup).isNotNull()
        assertThat(userManagementGroup?.permissions).containsExactly("users.view")
    }
}

