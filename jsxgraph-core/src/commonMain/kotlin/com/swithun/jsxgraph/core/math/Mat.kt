/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/math.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import kotlin.math.sqrt

object Mat {
    const val eps: Double = 0.000001

    // JSXGraph: src/math/math.js -> hypot
    fun hypot(vararg values: Double): Double {
        var sum = 0.0
        for (value in values) {
            sum += value * value
        }
        return sqrt(sum)
    }
}
