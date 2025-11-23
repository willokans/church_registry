package com.example.registry.security

import com.example.registry.domain.Role
import com.example.registry.domain.Status
import com.example.registry.repo.MembershipRepository
import com.example.registry.repo.RolePermissionRepository
import com.example.registry.repo.TenantRolePermissionRepository
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
    private val tenantRolePermissionRepository: TenantRolePermissionRepository
) {
    
    fun can(tenantId: Long, permission: String, authentication: Authentication?): Boolean {
        if (authentication == null || !authentication.isAuthenticated) {
            return false
        }
        
        val jwt = (authentication as? JwtAuthenticationToken)?.token as? Jwt
            ?: return false
        
        val subject = jwt.subject ?: return false
        // JWT subject is UUID string, but we need Long ID - parse as Long directly
        val userId = try {
            subject.toLong()
        } catch (e: NumberFormatException) {
            // Fallback: try parsing as UUID and look up user by email or other identifier
            // For now, return false - this needs proper user lookup
            return false
        }
        
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
    
    data class MembershipInfo(
        val tenantId: Long,
        val role: Role
    )
}

