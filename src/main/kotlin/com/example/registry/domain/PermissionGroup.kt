package com.example.registry.domain

/**
 * Represents a group of permissions for UI organization.
 * Used to display permissions in a grouped format in admin interfaces.
 */
data class PermissionGroup(
    val name: String,
    val permissions: List<String>
)

