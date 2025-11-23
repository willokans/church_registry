package com.example.registry.domain

enum class Role(val level: Int) {
    SUPER_ADMIN(100),      // Highest
    PARISH_ADMIN(80),
    REGISTRAR(60),
    PRIEST(40),
    VIEWER(20)             // Lowest
}

// Helper method to check if this role has permission over another role
fun Role.hasPermission(other: Role): Boolean = this.level >= other.level

