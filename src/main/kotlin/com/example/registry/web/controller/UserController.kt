package com.example.registry.web.controller

import com.example.registry.domain.Role
import com.example.registry.domain.Status
import com.example.registry.repo.AppUserRepository
import com.example.registry.security.AuthorizationService
import com.example.registry.service.UserService
import com.example.registry.tenancy.TenantContext
import com.example.registry.web.dto.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin/users")
class UserController(
    private val userService: UserService,
    private val appUserRepository: AppUserRepository,
    private val authorizationService: AuthorizationService
) {
    
    @GetMapping("/me")
    fun getCurrentUser(authentication: Authentication): ResponseEntity<UserProfileDto> {
        val jwt = (authentication as JwtAuthenticationToken).token as Jwt
        // JWT subject might be UUID string or email - look up user by email first, then by subject
        val email = jwt.claims["email"] as? String ?: jwt.subject
        val user = appUserRepository.findByEmail(email)
            ?: throw NoSuchElementException("User not found")
        
        val memberships = authorizationService.getMembershipsForUser(user.id)
        val permissions = memberships.flatMap { membership ->
            authorizationService.getPermissionsForRole(membership.role)
        }.distinct()
        
        val profile = UserProfileDto(
            id = user.id,
            email = user.email,
            fullName = user.fullName,
            mfaEnabled = user.mfaEnabled,
            memberships = memberships.map { MembershipDto(it.tenantId, it.role.name, "ACTIVE") },
            permissions = permissions.toList()
        )
        
        return ResponseEntity.ok(profile)
    }
    
    @GetMapping
    @PreAuthorize("hasPermission(@tenantContext.get(), 'users.manage')")
    fun getAllUsers(): ResponseEntity<List<UserDto>> {
        val tenantId = TenantContext.require()
        val users = userService.findAllByTenant(tenantId)
        
        val dtos = users.map { user ->
            UserDto(
                id = user.id,
                email = user.email,
                fullName = user.fullName,
                mfaEnabled = user.mfaEnabled,
                status = user.status.name,
                createdAt = user.createdAt
            )
        }
        
        return ResponseEntity.ok(dtos)
    }
    
    @PostMapping("/invite")
    @PreAuthorize("hasPermission(@tenantContext.get(), 'users.manage')")
    // Invites a user to a tenant
    fun inviteUser(
        @Valid @RequestBody request: InviteUserRequest,
        authentication: Authentication
    ): ResponseEntity<UserDto> {
        val tenantId = TenantContext.require()
        val jwt = (authentication as JwtAuthenticationToken).token as Jwt
        val email = jwt.claims["email"] as? String ?: jwt.subject
        val currentUser = appUserRepository.findByEmail(email)
            ?: throw NoSuchElementException("Current user not found")
        val grantedBy = currentUser.id
        
        var user = userService.findByEmail(request.email)
        if (user == null) {
            user = userService.createUser(request.email, request.fullName)
        }
        
        val role = Role.valueOf(request.role)
        userService.grantMembership(user.id, tenantId, role, grantedBy)
        
        val dto = UserDto(
            id = user.id,
            email = user.email,
            fullName = user.fullName,
            mfaEnabled = user.mfaEnabled,
            status = user.status.name,
            createdAt = user.createdAt
        )
        
        return ResponseEntity.ok(dto)
    }
    
    @PostMapping("/{id}/role")
    @PreAuthorize("hasPermission(@tenantContext.get(), 'permissions.grant')")
    fun updateUserRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserRoleRequest,
        authentication: Authentication
    ): ResponseEntity<MembershipDto> {
        val tenantId = TenantContext.require()
        val jwt = (authentication as JwtAuthenticationToken).token as Jwt
        val email = jwt.claims["email"] as? String ?: jwt.subject
        val currentUser = appUserRepository.findByEmail(email)
            ?: throw NoSuchElementException("Current user not found")
        val grantedBy = currentUser.id
        
        val role = Role.valueOf(request.role)
        val membership = userService.grantMembership(id, tenantId, role, grantedBy)
        
        val dto = MembershipDto(
            tenantId = membership.tenantId,
            role = membership.role.name,
            status = membership.status.name
        )
        
        return ResponseEntity.ok(dto)
    }
    
    @PostMapping("/{id}/status")
    @PreAuthorize("hasPermission(@tenantContext.get(), 'users.manage')")
    fun updateUserStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserStatusRequest,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val tenantId = TenantContext.require()
        val jwt = (authentication as JwtAuthenticationToken).token as Jwt
        val email = jwt.claims["email"] as? String ?: jwt.subject
        val currentUser = appUserRepository.findByEmail(email)
            ?: throw NoSuchElementException("Current user not found")
        val updatedBy = currentUser.id
        
        val status = Status.valueOf(request.status)
        userService.updateUserStatus(id, tenantId, status, request.reason, updatedBy)
        
        return ResponseEntity.ok().build()
    }
}

