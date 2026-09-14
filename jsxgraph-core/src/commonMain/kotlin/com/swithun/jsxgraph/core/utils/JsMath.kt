package com.swithun.jsxgraph.core.utils

import kotlin.math.floor

/**
 * ECMAScript numeric behavior which differs from Kotlin common math.
 */
internal object JsMath {
    /**
     * ECMAScript Math.round rounds ties toward positive infinity and preserves
     * negative zero for values in [-0.5, 0).
     */
    fun round(value: Double): Double = when {
        value == 0.0 -> value
        value < 0.0 && value >= -0.5 -> -0.0
        else -> floor(value + 0.5)
    }
}
