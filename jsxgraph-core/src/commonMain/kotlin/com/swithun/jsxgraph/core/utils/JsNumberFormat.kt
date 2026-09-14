package com.swithun.jsxgraph.core.utils

import kotlin.math.abs
import kotlin.math.floor
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
}
