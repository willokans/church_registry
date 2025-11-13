package com.example.registry.repo

import com.example.registry.domain.Role
import com.example.registry.domain.Status
import com.example.registry.domain.entity.Membership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MembershipRepository : JpaRepository<Membership, Long> {
    fun findByUserIdAndTenantId(userId: UUID, tenantId: UUID): Membership?
    fun findByUserIdAndTenantIdAndStatus(userId: UUID, tenantId: UUID, status: Status): Membership?
    fun findAllByTenantId(tenantId: UUID): List<Membership>
    fun findAllByUserId(userId: UUID): List<Membership>
    fun findAllByTenantIdAndRole(tenantId: UUID, role: Role): List<Membership>
}

