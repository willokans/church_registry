package com.example.registry.service

import com.example.registry.domain.Role
import com.example.registry.domain.Status
import com.example.registry.domain.entity.AppUser
import com.example.registry.domain.entity.Membership
import com.example.registry.repo.AppUserRepository
import com.example.registry.repo.MembershipRepository
import com.example.registry.security.AuthorizationService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class UserService(
    private val appUserRepository: AppUserRepository,
    private val membershipRepository: MembershipRepository,
    private val auditService: AuditService,
    private val authorizationService: AuthorizationService,
    private val entityManager: EntityManager
) {
    
    fun findById(id: Long): AppUser? = appUserRepository.findById(id).orElse(null)
    
    fun findByEmail(email: String): AppUser? = appUserRepository.findByEmail(email)
    
    fun findAllByTenant(tenantId: Long): List<AppUser> {
        val memberships = membershipRepository.findAllByTenantId(tenantId)
        val userIds = memberships.map { it.userId }.toSet()
        return appUserRepository.findAllById(userIds)
    }
    
    @Transactional
    fun createUser(email: String, fullName: String): AppUser {
        if (appUserRepository.existsByEmail(email)) {
            throw IllegalArgumentException("User with email $email already exists")
        }
        
        val user = AppUser(
            email = email,
            fullName = fullName
        )
        
        val saved = appUserRepository.save(user)
        auditService.log(null, null, "CREATE", "AppUser", saved.id.toString(), null, saved)
        return saved
    }
    
    @Transactional
    fun grantMembership(
        userId: Long,
        tenantId: Long,
        role: Role,
        grantedBy: Long
    ): Membership {
        val existing = membershipRepository.findByUserIdAndTenantId(userId, tenantId)
        if (existing != null) {
            val updated = existing.copy(
                role = role,
                status = Status.ACTIVE,
                grantedBy = grantedBy
            )
            val saved = membershipRepository.save(updated)
            auditService.log(tenantId, grantedBy, "UPDATE", "Membership", null, existing, saved)
            // Evict membership cache
            authorizationService.evictAllMembershipsCache()
            return saved
        }
        
        val membership = Membership(
            userId = userId,
            tenantId = tenantId,
            role = role,
            grantedBy = grantedBy
        )
        val saved = membershipRepository.save(membership)
        auditService.log(tenantId, grantedBy, "CREATE", "Membership", null, null, saved)
        // Evict membership cache
        authorizationService.evictAllMembershipsCache()
        return saved
    }
    
    @Transactional
    fun updateUserStatus(
        userId: Long,
        tenantId: Long,
        status: Status,
        reason: String?,
        updatedBy: Long
    ) {
        // Use native query to bypass @Where clause filter
        // This allows finding inactive users to reactivate them
        val query = entityManager.createNativeQuery(
            "SELECT * FROM app_users WHERE id = :id",
            AppUser::class.java
        )
        query.setParameter("id", userId)
        
        @Suppress("UNCHECKED_CAST")
        val results = query.resultList as List<AppUser>
        val user = results.firstOrNull()
            ?: throw IllegalArgumentException("User not found")
        
        val before = user.copy()
        val updated = user.copy(status = status)
        val saved = appUserRepository.save(updated)
        
        auditService.log(
            tenantId, updatedBy, "UPDATE_STATUS", "AppUser", saved.id.toString(),
            before, saved
        )
    }
}

