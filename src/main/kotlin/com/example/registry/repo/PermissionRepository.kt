package com.example.registry.repo

import com.example.registry.domain.entity.Permission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PermissionRepository : JpaRepository<Permission, String>

