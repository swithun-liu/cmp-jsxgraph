/*
 * Kotlin translation support for JSXGraph number formatting.
 * Upstream: src/utils/type.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

internal object JsNumberFormat {
    fun compact(value: Double): String {
        if (!value.isFinite()) {
            return value.toString()
        }
        if (value == 0.0) {
            return "0"
        }
        if (floor(value) == value && abs(value) < 1.0e15) {
            return value.toLong().toString()
        }
        return value.toString()
    }

    fun fixed(
        value: Double,
        digits: Int,
    ): String {
        if (!value.isFinite()) {
            return value.toString()
        }

        val decimalPlaces = digits.coerceIn(0, 100)
        val factor = 10.0.pow(decimalPlaces)
        if (!factor.isFinite() || abs(value) >= 1.0e21) {
            return compact(value)
        }

        val scaled = JsMath.round(abs(value) * factor)
        val integerPart = floor(scaled / factor).toLong()
        if (decimalPlaces == 0) {
            return (if (value < 0.0) "-" else "") + integerPart
        }

        val fractionPart = (scaled - integerPart * factor).toLong()
        val fraction = fractionPart.toString().padStart(decimalPlaces, '0')
        return (if (value < 0.0) "-" else "") + integerPart + "." + fraction
    }

    /**
     * ECMAScript Number.toPrecision behavior used by JSXGraph polynomial terms.
     *
     * The caller owns the JavaScript precision range check (1..100).
     */
    fun precision(
        value: Double,
        precision: Int,
    ): String {
        if (!value.isFinite()) {
            return compact(value)
        }

        val sign = if (value < 0.0) "-" else ""
        val absoluteValue = abs(value)
        if (absoluteValue == 0.0) {
            return if (precision == 1) "0" else "0." + "0".repeat(precision - 1)
        }

        var exponent = floor(log10(absoluteValue)).toInt()
        val decimalPlaces = precision - exponent - 1
        val scale = 10.0.pow(decimalPlaces)
        val roundedValue = if (scale.isFinite() && scale != 0.0) {
            JsMath.round(absoluteValue * scale) / scale
        } else {
            absoluteValue
        }
        if (roundedValue > 0.0) {
            exponent = floor(log10(roundedValue)).toInt()
        }

        return if (exponent >= precision || exponent < -6) {
            val normalized = roundedValue / 10.0.pow(exponent)
            val mantissa = fixed(normalized, precision - 1)
            sign + mantissa + "e" + if (exponent >= 0) "+$exponent" else exponent.toString()
        } else {
            sign + fixed(roundedValue, precision - exponent - 1)
        }
    }

    /**
     * JSXGraph: src/utils/type.js -> _round10 through Env._round10.
     */
    fun roundDecimal(
        value: Double,
        decimalPlaces: Int,
    ): Double {
        if (decimalPlaces == 0) {
            return JsMath.round(value)
        }
        if (!value.isFinite()) {
            return Double.NaN
        }
        if (value == 0.0) {
            return 0.0
        }

        val shifted = shiftExponent(value, decimalPlaces)
        return shiftExponent(JsMath.round(shifted), -decimalPlaces)
    }

    private fun shiftExponent(
        value: Double,
        shift: Int,
    ): Double {
        val parts = value.toString().split('e', 'E', limit = 2)
        val exponent = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (parts[0] + "e" + (exponent + shift)).toDoubleOrNull() ?: Double.NaN
    }
}
