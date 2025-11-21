package com.example.registry.repo

import com.example.registry.domain.Role
import com.example.registry.domain.Status
import com.example.registry.domain.entity.Membership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MembershipRepository : JpaRepository<Membership, Long> {
    fun findByUserIdAndTenantId(userId: Long, tenantId: Long): Membership?
    fun findByUserIdAndTenantIdAndStatus(userId: Long, tenantId: Long, status: Status): Membership?
    fun findAllByTenantId(tenantId: Long): List<Membership>
    fun findAllByUserId(userId: Long): List<Membership>
    fun findAllByTenantIdAndRole(tenantId: Long, role: Role): List<Membership>
}

