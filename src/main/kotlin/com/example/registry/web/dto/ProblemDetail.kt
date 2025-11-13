package com.example.registry.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.net.URI
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProblemDetail(
    val type: URI = URI.create("about:blank"),
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: URI? = null,
    val code: String? = null,
    val timestamp: Instant = Instant.now(),
    val errors: Map<String, List<String>>? = null
)

