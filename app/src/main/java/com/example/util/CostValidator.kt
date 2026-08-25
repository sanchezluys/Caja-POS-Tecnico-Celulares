package com.example.util

object CostValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val value: Double = 0.0,
        val errorMessage: String? = null
    )

    /**
     * Sanitizes user input while typing:
     * - Converts commas ',' to dots '.'
     * - Allows only digits and at most one dot
     * - Prevents negative signs
     */
    fun filterNumericInput(input: String): String {
        val sanitized = input.replace(',', '.')
        val result = StringBuilder()
        var hasDot = false

        for (ch in sanitized) {
            if (ch.isDigit()) {
                result.append(ch)
            } else if (ch == '.' && !hasDot) {
                result.append(ch)
                hasDot = true
            }
        }
        return result.toString()
    }

    /**
     * Validates if a string is a valid non-negative decimal amount.
     * Empty string is considered valid 0.0 unless required=true.
     */
    fun validate(input: String, isRequired: Boolean = false): ValidationResult {
        val trimmed = input.trim().replace(',', '.')
        if (trimmed.isBlank()) {
            return if (isRequired) {
                ValidationResult(isValid = false, value = 0.0, errorMessage = "Este costo es obligatorio")
            } else {
                ValidationResult(isValid = true, value = 0.0, errorMessage = null)
            }
        }

        val parsed = trimmed.toDoubleOrNull()
        if (parsed == null) {
            return ValidationResult(
                isValid = false,
                value = 0.0,
                errorMessage = "Debe ser un número válido (ej: 25.50)"
            )
        }

        if (parsed < 0.0) {
            return ValidationResult(
                isValid = false,
                value = 0.0,
                errorMessage = "El monto no puede ser negativo"
            )
        }

        return ValidationResult(isValid = true, value = parsed, errorMessage = null)
    }
}
