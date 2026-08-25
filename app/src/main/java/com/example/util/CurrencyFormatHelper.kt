package com.example.util

import com.example.data.model.WorkshopConfig
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatHelper {

    const val SEPARATOR_DOT = "DOT"
    const val SEPARATOR_COMMA = "COMMA"
    const val SEPARATOR_NONE = "NONE"

    fun formatCop(
        amount: Double,
        thousandSeparator: String = SEPARATOR_DOT,
        includeSymbol: Boolean = true,
        includeCode: Boolean = false
    ): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern: String

        when (thousandSeparator.uppercase(Locale.ROOT)) {
            SEPARATOR_COMMA -> {
                symbols.groupingSeparator = ','
                symbols.decimalSeparator = '.'
                val hasDecimals = (amount % 1.0 != 0.0)
                pattern = if (hasDecimals) "#,##0.00" else "#,##0"
            }
            SEPARATOR_NONE -> {
                symbols.decimalSeparator = '.'
                val hasDecimals = (amount % 1.0 != 0.0)
                pattern = if (hasDecimals) "0.00" else "0"
            }
            else -> { // Default: SEPARATOR_DOT (Standard Colombian format)
                symbols.groupingSeparator = '.'
                symbols.decimalSeparator = ','
                val hasDecimals = (amount % 1.0 != 0.0)
                pattern = if (hasDecimals) "#,##0.00" else "#,##0"
            }
        }

        val df = DecimalFormat(pattern, symbols)
        if (thousandSeparator.uppercase(Locale.ROOT) == SEPARATOR_NONE) {
            df.isGroupingUsed = false
        } else {
            df.isGroupingUsed = true
            df.groupingSize = 3
        }

        val formattedNumber = df.format(amount)

        return buildString {
            if (includeSymbol) {
                append("$ ")
            }
            append(formattedNumber)
            if (includeCode) {
                append(" COP")
            }
        }
    }

    fun formatCop(
        amount: Double,
        config: WorkshopConfig?,
        includeSymbol: Boolean = true,
        includeCode: Boolean = false
    ): String {
        val sep = config?.thousandSeparator ?: SEPARATOR_DOT
        return formatCop(
            amount = amount,
            thousandSeparator = sep,
            includeSymbol = includeSymbol,
            includeCode = includeCode
        )
    }

    fun getSeparatorLabel(type: String): String {
        return when (type.uppercase(Locale.ROOT)) {
            SEPARATOR_DOT -> "Punto (.) ej: $ 150.000"
            SEPARATOR_COMMA -> "Coma (,) ej: $ 150,000"
            SEPARATOR_NONE -> "Sin separador ej: $ 150000"
            else -> "Punto (.) ej: $ 150.000"
        }
    }
}
