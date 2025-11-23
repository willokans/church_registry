package com.example.registry.security

import com.example.registry.domain.Role
import com.example.registry.domain.Status
import com.example.registry.repo.AppUserRepository
import com.example.registry.repo.MembershipRepository
import com.example.registry.repo.PermissionRepository
import com.example.registry.repo.RolePermissionRepository
import com.example.registry.repo.TenantRolePermissionRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthorizationService(
    private val membershipRepository: MembershipRepository,
    private val rolePermissionRepository: RolePermissionRepository,
    private val tenantRolePermissionRepository: TenantRolePermissionRepository,
    private val appUserRepository: AppUserRepository,
    private val permissionRepository: PermissionRepository
) {
    
    fun can(tenantId: Long, permission: String, authentication: Authentication?): Boolean {
        if (authentication == null || !authentication.isAuthenticated) {
            return false
        }
        
        val jwt = (authentication as? JwtAuthenticationToken)?.token as? Jwt
            ?: return false
        
        val subject = jwt.subject ?: return false
        
        // Extract user ID from JWT subject
        // Subject can be either:
        // 1. Long ID (numeric string) - parse directly
        // 2. Email address - look up user by email
        // 3. Other identifier - check email claim or return false
        val userId = extractUserIdFromJwt(jwt, subject) ?: return false
        
        // Cache membership lookup per token ID (jti claim)
        val tokenId = jwt.id ?: jwt.subject
        val membership = getMembershipForUserAndTenant(userId, tenantId, tokenId)
            ?: return false
        
        // SUPER_ADMIN has all permissions
        if (membership.role == Role.SUPER_ADMIN) {
            return true
        }
        
        // Check tenant-specific permissions first (these override global role permissions)
        val tenantPermission = getTenantRolePermission(tenantId, membership.role, permission)
        if (tenantPermission != null) {
            return tenantPermission.granted
        }
        
        // Fall back to global role permissions
        val permissions = getPermissionsForRole(membership.role)
        return permissions.contains(permission)
    }
    
    @Cacheable(value = ["role-permissions"], key = "#role")
    fun getPermissionsForRole(role: Role): Set<String> {
        return rolePermissionRepository.findAllByRole(role)
            .map { it.permissionKey }
            .toSet()
    }
    
    @Cacheable(value = ["tenant-role-permissions"], key = "#tenantId + '_' + #role + '_' + #permissionKey")
    fun getTenantRolePermission(tenantId: Long, role: Role, permissionKey: String): com.example.registry.domain.entity.TenantRolePermission? {
        return tenantRolePermissionRepository.findByTenantIdAndRoleAndPermissionKey(tenantId, role, permissionKey)
    }
    
    @Cacheable(value = ["memberships"], key = "#userId + '_' + #tenantId + '_' + (#tokenId ?: 'none')")
    fun getMembershipForUserAndTenant(userId: Long, tenantId: Long, tokenId: String?): com.example.registry.domain.entity.Membership? {
        val membership = membershipRepository.findByUserIdAndTenantIdAndStatus(
            userId, tenantId, Status.ACTIVE
        )
        // Return null if membership is expired
        return if (membership?.isExpired() == true) null else membership
    }
    
    fun getMembershipsForUser(userId: Long): List<MembershipInfo> {
        return membershipRepository.findAllByUserId(userId)
            .filter { it.status == Status.ACTIVE && !it.isExpired() }
            .map { MembershipInfo(it.tenantId, it.role) }
    }
    
    /**
     * Check if user has a permission in ANY of their tenants.
     * Used for cross-tenant operations like tenant management.
     */
    fun hasPermissionInAnyTenant(permission: String, authentication: Authentication?): Boolean {
        if (authentication == null || !authentication.isAuthenticated) {
            return false
        }
        
        val jwt = (authentication as? JwtAuthenticationToken)?.token as? Jwt
            ?: return false
        
        val subject = jwt.subject ?: return false
        val userId = extractUserIdFromJwt(jwt, subject) ?: return false
        
        // Get all active memberships for the user
        val memberships = getMembershipsForUser(userId)
        
        // Check if user has the permission in any tenant
        return memberships.any { membership ->
            // SUPER_ADMIN has all permissions
            if (membership.role == Role.SUPER_ADMIN) {
                return true
            }
            
            // Check tenant-specific permissions first
            val tenantPermission = getTenantRolePermission(membership.tenantId, membership.role, permission)
            if (tenantPermission != null) {
                return tenantPermission.granted
            }
            
            // Fall back to global role permissions
            val permissions = getPermissionsForRole(membership.role)
            permissions.contains(permission)
        }
    }
    
    /**
     * Get permission groups for UI organization.
     * Groups permissions by their category for better display in admin interfaces.
     */
    fun getPermissionGroups(role: Role): List<com.example.registry.domain.PermissionGroup> {
        val permissions = getPermissionsForRole(role)
        
        // Group permissions by category using Permission entities
        val grouped = permissions.groupBy { permissionKey ->
            permissionRepository.findById(permissionKey).orElse(null)
                ?.category
                ?: inferCategoryFromKey(permissionKey)
        }
        
        return grouped.map { (category, permKeys) ->
            com.example.registry.domain.PermissionGroup(
                name = getGroupName(category),
                permissions = permKeys.sorted()
            )
        }.sortedBy { it.name }
    }
    
    /**
     * Infer permission category from permission key pattern.
     * This is a fallback when Permission entity is not found.
     */
    private fun inferCategoryFromKey(key: String): com.example.registry.domain.PermissionCategory {
        return when {
            key.startsWith("users.") || key.startsWith("permissions.") -> 
                com.example.registry.domain.PermissionCategory.USERS
            key.startsWith("sacraments.") -> 
                com.example.registry.domain.PermissionCategory.SACRAMENTS
            key.startsWith("settings.") -> 
                com.example.registry.domain.PermissionCategory.SETTINGS
            key.startsWith("audit.") -> 
                com.example.registry.domain.PermissionCategory.AUDIT
            else -> com.example.registry.domain.PermissionCategory.USERS
        }
    }
    
    /**
     * Get human-readable group name from category.
     */
    private fun getGroupName(category: com.example.registry.domain.PermissionCategory): String {
        return when (category) {
            com.example.registry.domain.PermissionCategory.USERS -> "User Management"
            com.example.registry.domain.PermissionCategory.SACRAMENTS -> "Sacraments"
            com.example.registry.domain.PermissionCategory.SETTINGS -> "Settings"
            com.example.registry.domain.PermissionCategory.AUDIT -> "Audit"
        }
    }
    
    /**
     * Extract user ID from JWT token.
     * Handles multiple formats:
     * - Long ID (numeric string): parses directly
     * - Email address: looks up user by email
     * - Other: checks email claim in JWT
     */
    private fun extractUserIdFromJwt(jwt: Jwt, subject: String): Long? {
        // Try parsing as Long ID first (most common case)
        val userId = try {
            subject.toLong()
        } catch (e: NumberFormatException) {
            null
        }
        if (userId != null) {
            return userId
        }
        
        // If subject is not a Long, try to extract email
        val email = when {
            // Subject itself is an email (contains @)
            subject.contains("@") -> subject
            // Check email claim in JWT
            jwt.claims["email"] is String -> jwt.claims["email"] as String
            else -> null
        }
        
        // Look up user by email
        return email?.let { appUserRepository.findByEmail(it)?.id }
    }
    
    /**
     * Evict cache for role permissions when role permissions are updated.
     * Call this method after creating, updating, or deleting RolePermission entities.
     */
    @CacheEvict(value = ["role-permissions"], key = "#role")
    fun evictRolePermissionsCache(role: Role) {
        // Method body is empty - annotation handles cache eviction
    }
    
    /**
     * Evict all role permissions cache entries.
     * Use this when permissions for multiple roles are updated.
     */
    @CacheEvict(value = ["role-permissions"], allEntries = true)
    fun evictAllRolePermissionsCache() {
        // Method body is empty - annotation handles cache eviction
    }
    
    /**
     * Evict cache for tenant role permissions when tenant-specific permissions are updated.
     * Call this method after creating, updating, or deleting TenantRolePermission entities.
     */
    @CacheEvict(value = ["tenant-role-permissions"], key = "#tenantId + '_' + #role + '_' + #permissionKey")
    fun evictTenantRolePermissionCache(tenantId: Long, role: Role, permissionKey: String) {
        // Method body is empty - annotation handles cache eviction
    }
    
    /**
     * Evict all tenant role permissions cache entries for a specific tenant and role.
     * Use this when multiple permissions for a tenant/role combination are updated.
     */
    @CacheEvict(value = ["tenant-role-permissions"], allEntries = true)
    fun evictAllTenantRolePermissionsCache() {
        // Method body is empty - annotation handles cache eviction
    }
    
    /**
     * Evict membership cache when membership is updated.
     * Call this method after creating, updating, or deleting Membership entities.
     */
    @CacheEvict(value = ["memberships"], key = "#userId + '_' + #tenantId + '_' + (#tokenId ?: 'none')")
    fun evictMembershipCache(userId: Long, tenantId: Long, tokenId: String?) {
        // Method body is empty - annotation handles cache eviction
    }
    
    /**
     * Evict all membership cache entries.
     * Use this when multiple memberships are updated.
     */
    @CacheEvict(value = ["memberships"], allEntries = true)
    fun evictAllMembershipsCache() {
        // Method body is empty - annotation handles cache eviction
    }
    
    data class MembershipInfo(
        val tenantId: Long,
        val role: Role
    )
}

