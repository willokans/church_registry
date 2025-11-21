package com.example.registry.tenancy

object TenantContext {
    private val tenantId = ThreadLocal<Long?>()
    
    fun set(tenantId: Long?) {
        this.tenantId.set(tenantId)
    }
    
    fun get(): Long? = tenantId.get()
    
    fun clear() {
        tenantId.remove()
    }
    
    fun require(): Long = get() ?: throw IllegalStateException("No tenant context available")
}

