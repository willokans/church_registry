package com.example.registry.config

import com.example.registry.domain.Role
import com.example.registry.domain.RevocationStatus
import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import com.example.registry.domain.entity.*
import com.example.registry.repo.*
import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.orm.jpa.EntityManagerFactoryUtils
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.TransactionDefinition
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Component
@Profile("h2")
class H2DataInitializer(
    private val tenantRepository: TenantRepository,
    private val permissionRepository: PermissionRepository,
    private val rolePermissionRepository: RolePermissionRepository,
    private val entityManagerFactory: EntityManagerFactory,
    private val transactionManager: PlatformTransactionManager
) : ApplicationListener<ApplicationReadyEvent> {
    
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        // Force table creation using Hibernate SchemaExport, then seed data
        Thread {
            try {
                // Force table creation programmatically
                forceTableCreationViaSchemaExport()
                
                // Wait a moment for tables to be fully created
                Thread.sleep(2000)
                
                // Now seed data
                seedDataIfNeeded()
            } catch (e: Exception) {
                System.err.println("⚠ Failed to initialize H2 database: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }
    
    private fun forceTableCreationViaSchemaExport() {
        // The root issue: Entities have default UUID values, so persist() fails with "detached entity"
        // Native SQL INSERT doesn't trigger Hibernate DDL
        // Solution: Use entityManager.merge() which will INSERT if entity doesn't exist
        // But merge() tries to SELECT first, which fails if table doesn't exist
        // 
        // The real fix: Execute CREATE TABLE statements directly using the exact DDL
        // that Hibernate would generate. We'll use Session.doWork() to execute DDL.
        
        val transactionTemplate = TransactionTemplate(transactionManager)
        transactionTemplate.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        
        try {
            transactionTemplate.executeWithoutResult {
                val em = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory)
                em?.let { entityManager ->
                    val session = entityManager.unwrap(org.hibernate.Session::class.java)
                    
                    session.doWork { connection ->
                        val statement = connection.createStatement()
                        val tablesCreated = mutableListOf<String>()
                        
                        try {
                            // Execute CREATE TABLE statements directly
                            // These match what Hibernate would generate for our entities
                            
                            // 1. tenants
                            try {
                                statement.execute("""
                                    CREATE TABLE IF NOT EXISTS tenants (
                                        id UUID PRIMARY KEY,
                                        slug VARCHAR(100) NOT NULL UNIQUE,
                                        name VARCHAR(200) NOT NULL,
                                        parent_id UUID,
                                        theme VARCHAR(255),
                                        created_at TIMESTAMP(0) NOT NULL
                                    )
                                """.trimIndent())
                                tablesCreated.add("tenants")
                            } catch (e: Exception) {
                                // Table might already exist
                            }
                            
                            // 2. permissions
                            try {
                                statement.execute("""
                                    CREATE TABLE IF NOT EXISTS permissions (
                                        "key" VARCHAR(100) PRIMARY KEY
                                    )
                                """.trimIndent())
                                tablesCreated.add("permissions")
                            } catch (e: Exception) {}
                            
                            // 3. role_permissions
                            try {
                                statement.execute("""
                                    CREATE TABLE IF NOT EXISTS role_permissions (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        role VARCHAR(50) NOT NULL,
                                        permission_key VARCHAR(100) NOT NULL,
                                        UNIQUE(role, permission_key)
                                    )
                                """.trimIndent())
                                tablesCreated.add("role_permissions")
                            } catch (e: Exception) {}
                            
                            // 4. app_users
                            try {
                                statement.execute("""
                                    CREATE TABLE IF NOT EXISTS app_users (
                                        id UUID PRIMARY KEY,
                                        email VARCHAR(255) NOT NULL UNIQUE,
                                        full_name VARCHAR(200) NOT NULL,
                                        mfa_enabled BOOLEAN NOT NULL,
                                        status VARCHAR(20) NOT NULL,
                                        created_at TIMESTAMP(0) NOT NULL
                                    )
                                """.trimIndent())
                                tablesCreated.add("app_users")
                            } catch (e: Exception) {}
                            
                            // 5. memberships
                            try {
                                statement.execute("""
                                    CREATE TABLE IF NOT EXISTS memberships (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        user_id UUID NOT NULL,
                                        tenant_id UUID NOT NULL,
                                        role VARCHAR(50) NOT NULL,
                                        status VARCHAR(20) NOT NULL,
                                        granted_at TIMESTAMP(0) NOT NULL,
                                        granted_by UUID
                                    )
                                """.trimIndent())
                                tablesCreated.add("memberships")
                            } catch (e: Exception) {}
                            
                            // 6. content_blocks
                            try {
                                statement.execute("""
                                    CREATE TABLE IF NOT EXISTS content_blocks (
                                        id UUID PRIMARY KEY,
                                        tenant_id UUID NOT NULL,
                                        "key" VARCHAR(100) NOT NULL,
                                        content TEXT,
                                        updated_at TIMESTAMP(0) NOT NULL
                                    )
                                """.trimIndent())
                                tablesCreated.add("content_blocks")
                            } catch (e: Exception) {}
                            
                            // 7. sacrament_events
                            try {
                                statement.execute("""
                                    CREATE TABLE IF NOT EXISTS sacrament_events (
                                        id UUID PRIMARY KEY,
                                        tenant_id UUID NOT NULL,
                                        type VARCHAR(50) NOT NULL,
                                        person_id UUID NOT NULL,
                                        date DATE NOT NULL,
                                        minister_id UUID,
                                        book_no INTEGER NOT NULL,
                                        page_no INTEGER NOT NULL,
                                        entry_no INTEGER NOT NULL,
                                        status VARCHAR(20) NOT NULL,
                                        created_by UUID NOT NULL,
                                        created_at TIMESTAMP(0) NOT NULL,
                                        updated_by UUID,
                                        updated_at TIMESTAMP(0),
                                        deactivated_at TIMESTAMP(0),
                                        deactivated_by UUID,
                                        deactivation_reason VARCHAR(500)
                                    )
                                """.trimIndent())
                                tablesCreated.add("sacrament_events")
                            } catch (e: Exception) {}
                            
                            // 8. certificates
                            try {
                                statement.execute("""
                                    CREATE TABLE IF NOT EXISTS certificates (
                                        id UUID PRIMARY KEY,
                                        event_id UUID NOT NULL,
                                        serial_no VARCHAR(26) NOT NULL,
                                        issued_at TIMESTAMP(0) NOT NULL,
                                        issuer_id UUID NOT NULL,
                                        revocation_status VARCHAR(20) NOT NULL
                                    )
                                """.trimIndent())
                                tablesCreated.add("certificates")
                            } catch (e: Exception) {}
                            
                            // 9. audit_logs
                            try {
                                statement.execute("""
                                    CREATE TABLE IF NOT EXISTS audit_logs (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        tenant_id UUID,
                                        actor_id UUID NOT NULL,
                                        action VARCHAR(100) NOT NULL,
                                        entity VARCHAR(100) NOT NULL,
                                        entity_id UUID,
                                        "before" VARCHAR(255),
                                        "after" VARCHAR(255),
                                        hash VARCHAR(64),
                                        prev_hash VARCHAR(64),
                                        ts TIMESTAMP(0) NOT NULL
                                    )
                                """.trimIndent())
                                tablesCreated.add("audit_logs")
                            } catch (e: Exception) {}
                            
                            // 10. idempotency_keys
                            try {
                                statement.execute("""
                                    CREATE TABLE IF NOT EXISTS idempotency_keys (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        tenant_id UUID NOT NULL,
                                        "key" VARCHAR(255) NOT NULL,
                                        request_hash VARCHAR(64),
                                        response_code INTEGER,
                                        created_at TIMESTAMP(0) NOT NULL
                                    )
                                """.trimIndent())
                                tablesCreated.add("idempotency_keys")
                            } catch (e: Exception) {}
                            
                            // 11. outbox
                            try {
                                statement.execute("""
                                    CREATE TABLE IF NOT EXISTS outbox (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        tenant_id UUID,
                                        topic VARCHAR(100) NOT NULL,
                                        payload TEXT NOT NULL,
                                        status VARCHAR(50) NOT NULL,
                                        created_at TIMESTAMP(0) NOT NULL,
                                        published_at TIMESTAMP(0),
                                        retry_count INTEGER NOT NULL,
                                        error_message VARCHAR(1000)
                                    )
                                """.trimIndent())
                                tablesCreated.add("outbox")
                            } catch (e: Exception) {}
                            
                            if (tablesCreated.isNotEmpty()) {
                                println("✓ Created ${tablesCreated.size} tables via DDL: ${tablesCreated.joinToString(", ")}")
                            }
                            
                        } finally {
                            statement.close()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("⚠ Could not create tables via DDL: ${e.message}")
            e.printStackTrace()
            // Fallback: try native SQL approach
            forceTableCreationViaQueries()
        }
    }
    
    private fun createTablesViaEntities() {
        val transactionTemplate = TransactionTemplate(transactionManager)
        transactionTemplate.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        
        try {
            transactionTemplate.executeWithoutResult {
                val em = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory)
                em?.let { entityManager ->
                    val tablesCreated = mutableListOf<String>()
                    var dummyTenantId: UUID? = null
                    var dummyUserId: UUID? = null
                    var dummyEventId: UUID? = null
                    
                    // Create and delete dummy entities to trigger table creation
                    // Order matters for entities with dependencies
                    // Use persist() for new entities - don't pre-assign UUIDs, let Hibernate generate them
                    
                    // 1. Tenant (no dependencies)
                    try {
                        val dummy = Tenant(
                            slug = "__dummy__",
                            name = "__dummy__",
                            theme = emptyMap(),
                            createdAt = Instant.now()
                        )
                        entityManager.persist(dummy)
                        entityManager.flush()
                        dummyTenantId = dummy.id
                        entityManager.remove(dummy)
                        entityManager.flush()
                        tablesCreated.add("tenants")
                    } catch (e: Exception) {
                        System.err.println("⚠ Failed to create tenants table: ${e.message}")
                        e.printStackTrace()
                        // Use random UUID as fallback
                        dummyTenantId = UUID.randomUUID()
                    }
                    
                    // 2. Permission (no dependencies)
                    try {
                        val dummy = Permission(key = "__dummy__")
                        entityManager.persist(dummy)
                        entityManager.flush()
                        entityManager.remove(dummy)
                        tablesCreated.add("permissions")
                    } catch (e: Exception) {
                        System.err.println("⚠ Failed to create permissions table: ${e.message}")
                        e.printStackTrace()
                    }
                    
                    // 3. RolePermission (depends on Permission)
                    try {
                        val dummy = RolePermission(role = Role.VIEWER, permissionKey = "__dummy__")
                        entityManager.persist(dummy)
                        entityManager.flush()
                        entityManager.remove(dummy)
                        tablesCreated.add("role_permissions")
                    } catch (e: Exception) {
                        System.err.println("⚠ Failed to create role_permissions table: ${e.message}")
                    }
                    
                    // 4. AppUser (no dependencies) - Note: CITEXT not supported in H2, will use VARCHAR
                    try {
                        val dummy = AppUser(
                            email = "__dummy__@example.com",
                            fullName = "__dummy__",
                            mfaEnabled = false,
                            status = Status.ACTIVE,
                            createdAt = Instant.now()
                        )
                        entityManager.persist(dummy)
                        entityManager.flush()
                        dummyUserId = dummy.id
                        entityManager.remove(dummy)
                        entityManager.flush()
                        tablesCreated.add("app_users")
                    } catch (e: Exception) {
                        System.err.println("⚠ Failed to create app_users table: ${e.message}")
                        e.printStackTrace()
                        // Use random UUID as fallback
                        if (dummyUserId == null) dummyUserId = UUID.randomUUID()
                    }
                    
                    // 5. Membership (depends on AppUser and Tenant) - Use random UUIDs for FK if entities don't exist
                    try {
                        val dummy = Membership(
                            userId = dummyUserId ?: UUID.randomUUID(),
                            tenantId = dummyTenantId ?: UUID.randomUUID(),
                            role = Role.VIEWER,
                            status = Status.ACTIVE,
                            grantedAt = Instant.now()
                        )
                        entityManager.persist(dummy)
                        entityManager.flush()
                        entityManager.remove(dummy)
                        entityManager.flush()
                        tablesCreated.add("memberships")
                    } catch (e: Exception) {
                        System.err.println("⚠ Failed to create memberships table: ${e.message}")
                        e.printStackTrace()
                    }
                    
                    // 6. ContentBlock (depends on Tenant)
                    try {
                        val dummy = ContentBlock(
                            tenantId = dummyTenantId ?: UUID.randomUUID(),
                            key = "__dummy__",
                            updatedAt = Instant.now()
                        )
                        entityManager.persist(dummy)
                        entityManager.flush()
                        val dummyId = dummy.id
                        entityManager.remove(entityManager.find(ContentBlock::class.java, dummyId))
                        entityManager.flush()
                        tablesCreated.add("content_blocks")
                    } catch (e: Exception) {
                        System.err.println("⚠ Failed to create content_blocks table: ${e.message}")
                        e.printStackTrace()
                    }
                    
                    // 7. SacramentEvent (depends on Tenant)
                    try {
                        val dummy = SacramentEvent(
                            tenantId = dummyTenantId ?: UUID.randomUUID(),
                            type = SacramentType.BAPTISM,
                            personId = UUID.randomUUID(),
                            date = LocalDate.now(),
                            bookNo = 1,
                            pageNo = 1,
                            entryNo = 1,
                            status = Status.ACTIVE,
                            createdBy = dummyUserId ?: UUID.randomUUID(),
                            createdAt = Instant.now()
                        )
                        entityManager.persist(dummy)
                        entityManager.flush()
                        dummyEventId = dummy.id
                        entityManager.remove(dummy)
                        entityManager.flush()
                        tablesCreated.add("sacrament_events")
                    } catch (e: Exception) {
                        System.err.println("⚠ Failed to create sacrament_events table: ${e.message}")
                        e.printStackTrace()
                    }
                    
                    // 8. Certificate (depends on SacramentEvent)
                    try {
                        val dummy = Certificate(
                            eventId = dummyEventId ?: UUID.randomUUID(),
                            serialNo = "01" + "0".repeat(24), // ULID format
                            issuedAt = Instant.now(),
                            issuerId = dummyUserId ?: UUID.randomUUID(),
                            revocationStatus = RevocationStatus.ACTIVE
                        )
                        entityManager.persist(dummy)
                        entityManager.flush()
                        val dummyCertId = dummy.id
                        entityManager.remove(entityManager.find(Certificate::class.java, dummyCertId))
                        entityManager.flush()
                        tablesCreated.add("certificates")
                    } catch (e: Exception) {
                        System.err.println("⚠ Failed to create certificates table: ${e.message}")
                        e.printStackTrace()
                    }
                    
                    // 9. AuditLog (no strict dependencies)
                    try {
                        val dummy = AuditLog(
                            tenantId = dummyTenantId ?: UUID.randomUUID(),
                            actorId = dummyUserId ?: UUID.randomUUID(),
                            action = "__dummy__",
                            entity = "__dummy__",
                            entityId = UUID.randomUUID(),
                            ts = Instant.now()
                        )
                        entityManager.persist(dummy)
                        entityManager.flush()
                        entityManager.remove(dummy)
                        tablesCreated.add("audit_logs")
                    } catch (e: Exception) {
                        System.err.println("⚠ Failed to create audit_logs table: ${e.message}")
                        e.printStackTrace()
                    }
                    
                    // 10. IdempotencyKey (depends on Tenant)
                    try {
                        val dummy = IdempotencyKey(
                            tenantId = dummyTenantId ?: UUID.randomUUID(),
                            key = "__dummy__",
                            createdAt = Instant.now()
                        )
                        entityManager.persist(dummy)
                        entityManager.flush()
                        entityManager.remove(dummy)
                        tablesCreated.add("idempotency_keys")
                    } catch (e: Exception) {
                        System.err.println("⚠ Failed to create idempotency_keys table: ${e.message}")
                        e.printStackTrace()
                    }
                    
                    // 11. Outbox (no strict dependencies)
                    try {
                        val dummy = Outbox(
                            tenantId = dummyTenantId ?: UUID.randomUUID(),
                            topic = "__dummy__",
                            payload = "{}",
                            status = "PENDING",
                            createdAt = Instant.now(),
                            retryCount = 0
                        )
                        entityManager.persist(dummy)
                        entityManager.flush()
                        entityManager.remove(dummy)
                        tablesCreated.add("outbox")
                    } catch (e: Exception) {
                        System.err.println("⚠ Failed to create outbox table: ${e.message}")
                        e.printStackTrace()
                    }
                    
                    entityManager.flush()
                    if (tablesCreated.isNotEmpty()) {
                        println("✓ Created ${tablesCreated.size} tables via entities: ${tablesCreated.joinToString(", ")}")
                    } else {
                        System.err.println("⚠ Warning: No tables were created via entities. Tables may already exist or there were errors.")
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("⚠ Warning: Could not create all tables: ${e.message}")
            e.printStackTrace()
        }
    }
    
    @Transactional
    private fun seedDataIfNeeded() {
        // Force table creation by executing a simple query on each entity type
        // This triggers Hibernate to create tables lazily with ddl-auto: create-drop
        forceTableCreationViaQueries()
        
        // Then wait a bit and check if tables exist
        var retries = 10
        var delay = 500L
        while (retries > 0) {
            try {
                val count = tenantRepository.count()
                if (count == 0L) {
                    seedData()
                    println("✓ H2 database seeded successfully")
                } else {
                    println("✓ H2 database already has data (${count} tenants)")
                }
                return // Success - exit the method
            } catch (e: Exception) {
                if (e.message?.contains("Table") == true && e.message?.contains("not found") == true) {
                    // Table doesn't exist yet - wait and retry
                    retries--
                    if (retries == 0) {
                        System.err.println("⚠ Failed to seed H2 database: Tables not created after all retries")
                        System.err.println("   Error: ${e.message}")
                    } else {
                        println("⏳ Waiting for tables to be created... (${retries} retries left)")
                        Thread.sleep(delay)
                        delay = (delay * 1.2).toLong() // Exponential backoff
                    }
                } else {
                    // Different error - log and exit
                    System.err.println("⚠ Failed to seed H2 database: ${e.message}")
                    e.printStackTrace()
                    return
                }
            }
        }
    }
    
    private fun forceTableCreationViaQueries() {
        // Force Hibernate to create tables by using native SQL INSERT statements
        // This triggers table creation with ddl-auto: create-drop
        val transactionTemplate = TransactionTemplate(transactionManager)
        transactionTemplate.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        
        try {
            transactionTemplate.executeWithoutResult {
                val em = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory)
                em?.let { entityManager ->
                    val tablesCreated = mutableListOf<String>()
                    
                    // Use native SQL INSERT to trigger table creation, then DELETE to clean up
                    // Order matters for tables with foreign keys
                    
                    // 1. tenants
                    try {
                        val tenantId = UUID.randomUUID()
                        entityManager.createNativeQuery("""
                            INSERT INTO tenants (id, slug, name, created_at) 
                            VALUES (?, '__dummy__', '__dummy__', CURRENT_TIMESTAMP)
                        """.trimIndent()).setParameter(1, tenantId).executeUpdate()
                        entityManager.createNativeQuery("DELETE FROM tenants WHERE slug = '__dummy__'").executeUpdate()
                        tablesCreated.add("tenants")
                    } catch (e: Exception) {
                        // Table might already exist or creation failed
                    }
                    
                    // 2. permissions
                    try {
                        entityManager.createNativeQuery("""
                            INSERT INTO permissions ("key") VALUES ('__dummy__')
                        """.trimIndent()).executeUpdate()
                        entityManager.createNativeQuery("DELETE FROM permissions WHERE \"key\" = '__dummy__'").executeUpdate()
                        tablesCreated.add("permissions")
                    } catch (e: Exception) {}
                    
                    // 3. role_permissions
                    try {
                        entityManager.createNativeQuery("""
                            INSERT INTO role_permissions (role, permission_key) 
                            VALUES ('VIEWER', '__dummy__')
                        """.trimIndent()).executeUpdate()
                        entityManager.createNativeQuery("DELETE FROM role_permissions WHERE permission_key = '__dummy__'").executeUpdate()
                        tablesCreated.add("role_permissions")
                    } catch (e: Exception) {}
                    
                    // 4. app_users
                    try {
                        val userId = UUID.randomUUID()
                        entityManager.createNativeQuery("""
                            INSERT INTO app_users (id, email, full_name, mfa_enabled, status, created_at) 
                            VALUES (?, '__dummy__@example.com', '__dummy__', false, 'ACTIVE', CURRENT_TIMESTAMP)
                        """.trimIndent()).setParameter(1, userId).executeUpdate()
                        entityManager.createNativeQuery("DELETE FROM app_users WHERE email = '__dummy__@example.com'").executeUpdate()
                        tablesCreated.add("app_users")
                    } catch (e: Exception) {}
                    
                    // 5. memberships
                    try {
                        val tenantId = UUID.randomUUID()
                        val userId = UUID.randomUUID()
                        entityManager.createNativeQuery("""
                            INSERT INTO memberships (user_id, tenant_id, role, status, granted_at) 
                            VALUES (?, ?, 'VIEWER', 'ACTIVE', CURRENT_TIMESTAMP)
                        """.trimIndent())
                            .setParameter(1, userId)
                            .setParameter(2, tenantId)
                            .executeUpdate()
                        entityManager.createNativeQuery("DELETE FROM memberships WHERE user_id = ?")
                            .setParameter(1, userId)
                            .executeUpdate()
                        tablesCreated.add("memberships")
                    } catch (e: Exception) {}
                    
                    // 6. content_blocks
                    try {
                        val blockId = UUID.randomUUID()
                        val tenantId = UUID.randomUUID()
                        entityManager.createNativeQuery("""
                            INSERT INTO content_blocks (id, tenant_id, "key", updated_at) 
                            VALUES (?, ?, '__dummy__', CURRENT_TIMESTAMP)
                        """.trimIndent())
                            .setParameter(1, blockId)
                            .setParameter(2, tenantId)
                            .executeUpdate()
                        entityManager.createNativeQuery("DELETE FROM content_blocks WHERE id = ?")
                            .setParameter(1, blockId)
                            .executeUpdate()
                        tablesCreated.add("content_blocks")
                    } catch (e: Exception) {}
                    
                    // 7. sacrament_events
                    try {
                        val eventId = UUID.randomUUID()
                        val tenantId = UUID.randomUUID()
                        val personId = UUID.randomUUID()
                        val createdBy = UUID.randomUUID()
                        entityManager.createNativeQuery("""
                            INSERT INTO sacrament_events (id, tenant_id, type, person_id, date, book_no, page_no, entry_no, status, created_by, created_at) 
                            VALUES (?, ?, 'BAPTISM', ?, CURRENT_DATE, 1, 1, 1, 'ACTIVE', ?, CURRENT_TIMESTAMP)
                        """.trimIndent())
                            .setParameter(1, eventId)
                            .setParameter(2, tenantId)
                            .setParameter(3, personId)
                            .setParameter(4, createdBy)
                            .executeUpdate()
                        entityManager.createNativeQuery("DELETE FROM sacrament_events WHERE id = ?")
                            .setParameter(1, eventId)
                            .executeUpdate()
                        tablesCreated.add("sacrament_events")
                    } catch (e: Exception) {}
                    
                    // 8. certificates
                    try {
                        val certId = UUID.randomUUID()
                        val eventId = UUID.randomUUID()
                        val issuerId = UUID.randomUUID()
                        entityManager.createNativeQuery("""
                            INSERT INTO certificates (id, event_id, serial_no, issued_at, issuer_id, revocation_status) 
                            VALUES (?, ?, '01000000000000000000000000', CURRENT_TIMESTAMP, ?, 'ACTIVE')
                        """.trimIndent())
                            .setParameter(1, certId)
                            .setParameter(2, eventId)
                            .setParameter(3, issuerId)
                            .executeUpdate()
                        entityManager.createNativeQuery("DELETE FROM certificates WHERE id = ?")
                            .setParameter(1, certId)
                            .executeUpdate()
                        tablesCreated.add("certificates")
                    } catch (e: Exception) {}
                    
                    // 9. audit_logs
                    try {
                        val tenantId = UUID.randomUUID()
                        val actorId = UUID.randomUUID()
                        val entityId = UUID.randomUUID()
                        entityManager.createNativeQuery("""
                            INSERT INTO audit_logs (tenant_id, actor_id, action, entity, entity_id, ts) 
                            VALUES (?, ?, '__dummy__', '__dummy__', ?, CURRENT_TIMESTAMP)
                        """.trimIndent())
                            .setParameter(1, tenantId)
                            .setParameter(2, actorId)
                            .setParameter(3, entityId)
                            .executeUpdate()
                        entityManager.createNativeQuery("DELETE FROM audit_logs WHERE action = '__dummy__'").executeUpdate()
                        tablesCreated.add("audit_logs")
                    } catch (e: Exception) {}
                    
                    // 10. idempotency_keys
                    try {
                        val tenantId = UUID.randomUUID()
                        entityManager.createNativeQuery("""
                            INSERT INTO idempotency_keys (tenant_id, "key", created_at) 
                            VALUES (?, '__dummy__', CURRENT_TIMESTAMP)
                        """.trimIndent())
                            .setParameter(1, tenantId)
                            .executeUpdate()
                        entityManager.createNativeQuery("DELETE FROM idempotency_keys WHERE \"key\" = '__dummy__'").executeUpdate()
                        tablesCreated.add("idempotency_keys")
                    } catch (e: Exception) {}
                    
                    // 11. outbox
                    try {
                        val tenantId = UUID.randomUUID()
                        entityManager.createNativeQuery("""
                            INSERT INTO outbox (tenant_id, topic, payload, status, created_at, retry_count) 
                            VALUES (?, '__dummy__', '{}', 'PENDING', CURRENT_TIMESTAMP, 0)
                        """.trimIndent())
                            .setParameter(1, tenantId)
                            .executeUpdate()
                        entityManager.createNativeQuery("DELETE FROM outbox WHERE topic = '__dummy__'").executeUpdate()
                        tablesCreated.add("outbox")
                    } catch (e: Exception) {}
                    
                    if (tablesCreated.isNotEmpty()) {
                        println("✓ Created ${tablesCreated.size} tables via native SQL: ${tablesCreated.joinToString(", ")}")
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore - tables will be created on first actual use
            println("⚠ Could not force table creation via native SQL: ${e.message}")
        }
    }
    
    private fun seedData() {
        // Create sample tenant
        val tenant = Tenant(
            id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            slug = "sample-parish",
            name = "Sample Parish",
            theme = mapOf("primaryColor" to "#0066cc", "logo" to "/logo.png"),
            createdAt = Instant.now()
        )
        tenantRepository.save(tenant)
        
        // Create permissions
        val permissions = listOf(
            "users.manage",
            "users.view",
            "permissions.grant",
            "sacraments.create",
            "sacraments.update",
            "sacraments.view",
            "settings.edit",
            "audit.view"
        ).map { Permission(key = it) }
        permissionRepository.saveAll(permissions)
        
        // Create role permissions
        val allPermissions = permissions.map { it.key }.toSet()
        val registrarPermissions = setOf("sacraments.create", "sacraments.update", "sacraments.view")
        val priestPermissions = setOf("sacraments.create", "sacraments.view")
        val viewerPermissions = setOf("sacraments.view", "users.view")
        
        val rolePermissions = mutableListOf<RolePermission>()
        
        // SUPER_ADMIN and PARISH_ADMIN get all permissions
        listOf(Role.SUPER_ADMIN, Role.PARISH_ADMIN).forEach { role ->
            allPermissions.forEach { perm ->
                rolePermissions.add(RolePermission(role = role, permissionKey = perm))
            }
        }
        
        // REGISTRAR permissions
        registrarPermissions.forEach { perm ->
            rolePermissions.add(RolePermission(role = Role.REGISTRAR, permissionKey = perm))
        }
        
        // PRIEST permissions
        priestPermissions.forEach { perm ->
            rolePermissions.add(RolePermission(role = Role.PRIEST, permissionKey = perm))
        }
        
        // VIEWER permissions
        viewerPermissions.forEach { perm ->
            rolePermissions.add(RolePermission(role = Role.VIEWER, permissionKey = perm))
        }
        
        rolePermissionRepository.saveAll(rolePermissions)
    }
}



