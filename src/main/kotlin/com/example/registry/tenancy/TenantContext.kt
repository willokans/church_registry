package com.example.registry.tenancy

import java.util.*

object TenantContext {
    private val tenantId = ThreadLocal<UUID?>()
    
    fun set(tenantId: UUID?) {
        this.tenantId.set(tenantId)
    }
    
    fun get(): UUID? = tenantId.get()
    
    fun clear() {
        tenantId.remove()
    }
    
    fun require(): UUID = get() ?: throw IllegalStateException("No tenant context available")
}

