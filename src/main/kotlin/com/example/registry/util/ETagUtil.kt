package com.example.registry.util

import java.security.MessageDigest

object ETagUtil {
    fun generateETag(content: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(content.toByteArray())
        return "\"${hash.joinToString("") { "%02x".format(it) }}\""
    }
    
    fun matches(etag: String, ifNoneMatch: String?): Boolean {
        if (ifNoneMatch == null) return false
        return ifNoneMatch.contains(etag)
    }
}

