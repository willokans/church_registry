package com.example.registry.repo

import com.example.registry.domain.Role
import com.example.registry.domain.entity.TenantRolePermission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TenantRolePermissionRepository : JpaRepository<TenantRolePermission, Long> {
    fun findByTenantIdAndRole(tenantId: Long, role: Role): List<TenantRolePermission>
    fun findByTenantIdAndRoleAndPermissionKey(tenantId: Long, role: Role, permissionKey: String): TenantRolePermission?
    fun existsByTenantIdAndRoleAndPermissionKey(tenantId: Long, role: Role, permissionKey: String): Boolean
}

