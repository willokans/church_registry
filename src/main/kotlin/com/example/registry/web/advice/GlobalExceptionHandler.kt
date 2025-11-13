package com.example.registry.web.advice

import com.example.registry.web.dto.ErrorCode
import com.example.registry.web.dto.ProblemDetail
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        e: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail(
            type = URI.create("about:blank"),
            title = "Bad Request",
            status = HttpStatus.BAD_REQUEST.value(),
            detail = e.message,
            instance = URI.create(request.requestURI),
            code = ErrorCode.VALIDATION_ERROR.value,
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem)
    }
    
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(
        e: NoSuchElementException,
        request: HttpServletRequest
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail(
            type = URI.create("about:blank"),
            title = "Not Found",
            status = HttpStatus.NOT_FOUND.value(),
            detail = e.message ?: "Resource not found",
            instance = URI.create(request.requestURI),
            code = ErrorCode.NOT_FOUND.value,
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem)
    }
    
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(
        e: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail(
            type = URI.create("about:blank"),
            title = "Unauthorized",
            status = HttpStatus.UNAUTHORIZED.value(),
            detail = "Authentication required",
            instance = URI.create(request.requestURI),
            code = ErrorCode.UNAUTHORIZED.value,
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem)
    }
    
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        e: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail(
            type = URI.create("about:blank"),
            title = "Forbidden",
            status = HttpStatus.FORBIDDEN.value(),
            detail = "Access denied",
            instance = URI.create(request.requestURI),
            code = ErrorCode.FORBIDDEN.value,
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem)
    }
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ProblemDetail> {
        val errors = e.bindingResult.fieldErrors.associate { error ->
            error.field to listOf(error.defaultMessage ?: "Invalid value")
        }
        
        val problem = ProblemDetail(
            type = URI.create("about:blank"),
            title = "Validation Error",
            status = HttpStatus.BAD_REQUEST.value(),
            detail = "Request validation failed",
            instance = URI.create(request.requestURI),
            code = ErrorCode.VALIDATION_ERROR.value,
            timestamp = Instant.now(),
            errors = errors
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem)
    }
    
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        e: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ProblemDetail> {
        val errors = e.constraintViolations.associate { violation ->
            violation.propertyPath.toString() to listOf(violation.message)
        }
        
        val problem = ProblemDetail(
            type = URI.create("about:blank"),
            title = "Validation Error",
            status = HttpStatus.BAD_REQUEST.value(),
            detail = "Request validation failed",
            instance = URI.create(request.requestURI),
            code = ErrorCode.VALIDATION_ERROR.value,
            timestamp = Instant.now(),
            errors = errors
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem)
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail(
            type = URI.create("about:blank"),
            title = "Internal Server Error",
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            detail = "An unexpected error occurred",
            instance = URI.create(request.requestURI),
            code = ErrorCode.INTERNAL_ERROR.value,
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem)
    }
}

