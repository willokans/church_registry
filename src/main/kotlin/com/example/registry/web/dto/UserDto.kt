package com.example.registry.web.dto

import java.time.Instant
import java.util.*

data class UserDto(
    val id: UUID,
    val email: String,
    val fullName: String,
    val mfaEnabled: Boolean,
    val status: String,
    val createdAt: Instant
)

data class MembershipDto(
    val tenantId: UUID,
    val role: String,
    val status: String
)

data class UserProfileDto(
    val id: UUID,
    val email: String,
    val fullName: String,
    val mfaEnabled: Boolean,
    val memberships: List<MembershipDto>,
    val permissions: List<String>
)

data class InviteUserRequest(
    val email: String,
    val fullName: String,
    val role: String
)

data class UpdateUserRoleRequest(
    val role: String
)

data class UpdateUserStatusRequest(
    val status: String,
    val reason: String?
)

