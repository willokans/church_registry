package com.example.registry.repo

import com.example.registry.domain.Role
import com.example.registry.domain.entity.RolePermission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RolePermissionRepository : JpaRepository<RolePermission, Long> {
    fun findAllByRole(role: Role): List<RolePermission>
    fun existsByRoleAndPermissionKey(role: Role, permissionKey: String): Boolean
}

