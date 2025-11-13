package com.example.registry.repo

import com.example.registry.domain.entity.AppUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AppUserRepository : JpaRepository<AppUser, UUID> {
    fun findByEmail(email: String): AppUser?
    fun existsByEmail(email: String): Boolean
}

