package com.example.registry.web.dto.validation

import com.example.registry.domain.SacramentType
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Validates that a string value is a valid SacramentType enum value.
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [SacramentTypeValidatorImpl::class])
annotation class ValidSacramentType(
    val message: String = "Invalid sacrament type. Must be one of: BAPTISM, CONFIRMATION, EUCHARIST, RECONCILIATION, ANOINTING, HOLY_ORDERS, MATRIMONY",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class SacramentTypeValidatorImpl : ConstraintValidator<ValidSacramentType, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return false
        return try {
            SacramentType.valueOf(value.uppercase())
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}

