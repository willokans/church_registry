package com.example.registry.service

import com.example.registry.domain.Role
import com.example.registry.domain.Status
import com.example.registry.domain.entity.AppUser
import com.example.registry.domain.entity.Membership
import com.example.registry.repo.AppUserRepository
import com.example.registry.repo.MembershipRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class UserService(
    private val appUserRepository: AppUserRepository,
    private val membershipRepository: MembershipRepository,
    private val auditService: AuditService
) {
    
    fun findById(id: UUID): AppUser? = appUserRepository.findById(id).orElse(null)
    
    fun findByEmail(email: String): AppUser? = appUserRepository.findByEmail(email)
    
    fun findAllByTenant(tenantId: UUID): List<AppUser> {
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
        auditService.log(null, null, "CREATE", "AppUser", saved.id, null, saved)
        return saved
    }
    
    @Transactional
    fun grantMembership(
        userId: UUID,
        tenantId: UUID,
        role: Role,
        grantedBy: UUID
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
        return saved
    }
    
    @Transactional
    fun updateUserStatus(
        userId: UUID,
        tenantId: UUID,
        status: Status,
        reason: String?,
        updatedBy: UUID
    ) {
        val user = appUserRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        val before = user.copy()
        val updated = user.copy(status = status)
        val saved = appUserRepository.save(updated)
        
        auditService.log(
            tenantId, updatedBy, "UPDATE_STATUS", "AppUser", saved.id,
            before, saved
        )
    }
}

