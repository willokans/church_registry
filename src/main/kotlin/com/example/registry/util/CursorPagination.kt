package com.example.registry.util

data class CursorPage<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean
) {
    companion object {
        fun <T> empty(): CursorPage<T> = CursorPage(emptyList(), null, false)
    }
}

