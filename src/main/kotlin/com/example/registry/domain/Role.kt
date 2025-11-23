package com.example.registry.domain

enum class Role(val level: Int, val description: String) {
    SUPER_ADMIN(100, "Full system access across all tenants"),      // Highest
    PARISH_ADMIN(80, "Administrative access within a tenant"),
    REGISTRAR(60, "Can create and update sacramental records"),
    PRIEST(40, "Can create sacramental records"),
    VIEWER(20, "Read-only access")             // Lowest
}

// Helper method to check if this role has permission over another role
fun Role.hasPermission(other: Role): Boolean = this.level >= other.level

