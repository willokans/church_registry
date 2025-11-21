# Entitlement Engine Design - Analysis & Recommendations

## Current Architecture Overview

Your current design implements a **Role-Based Access Control (RBAC)** system with **multi-tenancy** support. This is a solid foundation for an entitlement engine.

### Current Entity Relationships

```
AppUser (1) ──< (Many) Membership (Many) >── (1) Tenant
                │
                └── Role (enum)
                    │
                    └──< (Many) RolePermission (Many) >── (1) Permission
```

### Entity Structure

1. **AppUser**: Global user entity (not tenant-specific)
2. **Tenant**: Organization/parish entity
3. **Membership**: Junction table linking User ↔ Tenant with Role
4. **Role**: Enum (SUPER_ADMIN, PARISH_ADMIN, REGISTRAR, PRIEST, VIEWER)
5. **Permission**: Individual permission keys (e.g., "users.manage", "sacraments.create")
6. **RolePermission**: Maps Roles to Permissions

---

## ✅ What's Good About Your Current Design

### 1. **Proper Separation of Concerns**

- Users are global (can belong to multiple tenants)
- Tenants are isolated
- Membership is the bridge with role assignment
- This allows users to have different roles in different tenants

### 2. **Many-to-Many Relationship**

- ✅ One user can belong to multiple tenants
- ✅ One tenant can have multiple users
- ✅ Unique constraint on `(userId, tenantId)` prevents duplicates
- ✅ Each membership has its own role per tenant

### 3. **RBAC Pattern**

- ✅ Roles are predefined (enum) - simple and performant
- ✅ Permissions are granular and flexible
- ✅ Role-Permission mapping is configurable
- ✅ Easy to add new permissions without code changes

### 4. **Audit Trail**

- ✅ `grantedBy` tracks who granted the membership
- ✅ `grantedAt` tracks when
- ✅ Soft delete via `status` field

### 5. **Performance Considerations**

- ✅ Caching in `AuthorizationService` for role-permissions
- ✅ Indexes on frequently queried fields

---

## 🔧 Recommended Improvements

### 1. **Add JPA Relationships (Optional but Recommended)**

While your current design works with manual ID references, adding JPA relationships can improve code clarity and enable better ORM features:

```kotlin
// AppUser.kt
@Entity
@Table(name = "app_users")
data class AppUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // ... existing fields ...

    // Add relationship (optional - for convenience)
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val memberships: List<Membership> = emptyList()
)

// Tenant.kt
@Entity
@Table(name = "tenants")
data class Tenant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // ... existing fields ...

    // Add relationship (optional)
    @OneToMany(mappedBy = "tenant", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val memberships: List<Membership> = emptyList()
)

// Membership.kt
@Entity
@Table(name = "memberships")
data class Membership(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: AppUser,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    val tenant: Tenant,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val role: Role,

    // ... rest of fields ...
)
```

**Note**: This is optional. Your current design with manual ID references is perfectly valid and may be preferred for:

- Simpler data classes (no circular dependencies)
- Better control over queries
- Avoiding N+1 query problems

### 2. **Add Missing Indexes**

Add indexes for better query performance:

```kotlin
@Table(
    name = "memberships",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "tenant_id"])],
    indexes = [
        Index(name = "idx_memberships_user_id", columnList = "user_id"),
        Index(name = "idx_memberships_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_memberships_role", columnList = "role"),
        Index(name = "idx_memberships_status", columnList = "status"),
        Index(name = "idx_memberships_user_tenant_status", columnList = "user_id,tenant_id,status")
    ]
)
```

### 3. **Consider Adding Expiration Dates**

For time-limited access:

```kotlin
data class Membership(
    // ... existing fields ...

    @Column(name = "expires_at")
    val expiresAt: Instant? = null,

    // Helper method
    fun isExpired(): Boolean = expiresAt != null && expiresAt.isBefore(Instant.now())
)
```

### 4. **Enhance Permission Model**

Consider adding permission metadata:

```kotlin
@Entity
@Table(name = "permissions")
data class Permission(
    @Id
    @Column(name = "key", length = 100)
    val key: String,

    @Column(name = "description", length = 500)
    val description: String? = null,

    @Column(name = "category", length = 50) // e.g., "users", "sacraments", "settings"
    val category: String? = null
)
```

### 5. **Add Tenant-Scoped Permissions (Advanced)**

For tenant-specific permission overrides:

```kotlin
@Entity
@Table(name = "tenant_role_permissions")
data class TenantRolePermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val role: Role,

    @Column(name = "permission_key", nullable = false, length = 100)
    val permissionKey: String,

    @Column(name = "granted", nullable = false)
    val granted: Boolean = true // true = grant, false = revoke

    // Unique constraint: (tenant_id, role, permission_key)
)
```

This allows:

- Tenant A: REGISTRAR can delete records
- Tenant B: REGISTRAR cannot delete records

---

## 🏗️ Best Practices for Entitlement Engines

### 1. **Principle of Least Privilege**

✅ Your design supports this - users get only the permissions their role grants

### 2. **Separation of Global vs Tenant-Scoped**

✅ Your design correctly separates:

- **Global**: AppUser (can exist without tenant)
- **Tenant-Scoped**: Membership (user's role within a tenant)

### 3. **Hierarchical Roles (Optional Enhancement)**

Consider role hierarchy:

```kotlin
enum class Role(val level: Int) {
    SUPER_ADMIN(100),      // Highest
    PARISH_ADMIN(80),
    REGISTRAR(60),
    PRIEST(40),
    VIEWER(20)             // Lowest
}

// Helper method
fun Role.hasPermission(other: Role): Boolean = this.level >= other.level
```

### 4. **Permission Inheritance**

Your current design doesn't support permission inheritance, but you could add:

```kotlin
@Entity
@Table(name = "permissions")
data class Permission(
    @Id
    @Column(name = "key", length = 100)
    val key: String,

    @Column(name = "parent_key", length = 100)
    val parentKey: String? = null, // For hierarchical permissions

    // e.g., "sacraments.*" → "sacraments.create", "sacraments.update"
)
```

### 5. **Caching Strategy**

Your current caching is good. Consider:

```kotlin
@Cacheable(value = ["role-permissions"], key = "#role")
fun getPermissionsForRole(role: Role): Set<String> {
    // Current implementation
}

// Add cache eviction on permission changes
@CacheEvict(value = ["role-permissions"], allEntries = true)
fun updateRolePermissions(role: Role, permissions: Set<String>) {
    // Update logic
}
```

---

## 📊 Relationship Diagram (Recommended Structure)

```
┌─────────────┐
│  AppUser    │
│─────────────│
│ id (PK)     │
│ email       │
│ fullName    │
│ status      │
└──────┬──────┘
       │
       │ 1
       │
       │ Many
       │
┌──────▼──────────┐
│   Membership    │
│─────────────────│
│ id (PK)         │
│ user_id (FK)    │──┐
│ tenant_id (FK)  │──┤
│ role            │  │
│ status          │  │
│ granted_by      │  │
│ granted_at      │  │
└──────┬──────────┘  │
       │             │
       │ Many        │
       │             │
       │ 1           │
┌──────▼──────┐      │
│   Tenant    │      │
│─────────────│      │
│ id (PK)     │      │
│ slug        │      │
│ name        │      │
│ parent_id   │      │
└─────────────┘      │
                     │
                     │
┌────────────────────┘
│
│ Role (enum)
│ ──────────
│ SUPER_ADMIN
│ PARISH_ADMIN
│ REGISTRAR
│ PRIEST
│ VIEWER
│
└──────┬─────────────┐
       │             │
       │ Many        │ Many
       │             │
┌──────▼─────────────▼──────┐
│   RolePermission         │
│──────────────────────────│
│ id (PK)                  │
│ role (enum)              │
│ permission_key (FK)      │──┐
└──────────────────────────┘  │
                               │
                               │
┌──────────────────────────────┘
│
│ Permission
│ ──────────
│ key (PK)
│ "users.manage"
│ "sacraments.create"
│ etc.
```

---

## 🎯 Recommended Implementation Strategy

### Phase 1: Current Design (✅ You're Here)

- Manual ID references
- Simple and performant
- Good for MVP

### Phase 2: Add JPA Relationships (Optional)

- Better code readability
- Easier navigation
- Watch for N+1 queries

### Phase 3: Enhanced Features (Future)

- Expiration dates
- Permission metadata
- Tenant-scoped permissions
- Role hierarchy

---

## 🔍 Authorization Flow

Your current authorization flow:

```
1. User authenticates → JWT token
2. Extract user ID from JWT (subject/email)
3. Get tenant from X-Tenant header
4. Lookup Membership(userId, tenantId, ACTIVE)
5. Get Role from Membership
6. If SUPER_ADMIN → Allow all
7. Else → Get Permissions for Role
8. Check if permission exists in set
```

**This is correct!** ✅

---

## 💡 Additional Recommendations

### 1. **Add Membership Expiration**

```kotlin
data class Membership(
    // ... existing fields ...
    @Column(name = "expires_at")
    val expiresAt: Instant? = null
)

// In AuthorizationService
fun getMembershipForUserAndTenant(...): Membership? {
    val membership = membershipRepository.findByUserIdAndTenantIdAndStatus(...)
    return if (membership?.isExpired() == true) null else membership
}
```

### 2. **Add Permission Categories**

Group permissions for UI:

```kotlin
enum class PermissionCategory {
    USERS,
    SACRAMENTS,
    SETTINGS,
    AUDIT
}

data class Permission(
    val key: String,
    val category: PermissionCategory,
    val description: String
)
```

### 3. **Add Role Descriptions**

```kotlin
enum class Role(val description: String) {
    SUPER_ADMIN("Full system access across all tenants"),
    PARISH_ADMIN("Administrative access within a tenant"),
    REGISTRAR("Can create and update sacramental records"),
    PRIEST("Can create sacramental records"),
    VIEWER("Read-only access")
}
```

### 4. **Consider Permission Groups**

For UI organization:

```kotlin
data class PermissionGroup(
    val name: String,
    val permissions: List<String>
)

// Example:
PermissionGroup(
    name = "User Management",
    permissions = ["users.manage", "users.view", "permissions.grant"]
)
```

---

## ✅ Conclusion

**Your current design is excellent for an entitlement engine!** It follows industry best practices:

1. ✅ Proper many-to-many relationship via Membership
2. ✅ Role-based access control
3. ✅ Granular permissions
4. ✅ Multi-tenancy support
5. ✅ Audit trail
6. ✅ Performance considerations (caching, indexes)

**Recommended Next Steps:**

1. Add missing indexes for performance
2. Consider JPA relationships (optional)
3. Add expiration dates if needed
4. Enhance permission metadata for better UX
5. Consider tenant-scoped permissions for advanced use cases

The foundation is solid - you can build on it incrementally as needs arise!
