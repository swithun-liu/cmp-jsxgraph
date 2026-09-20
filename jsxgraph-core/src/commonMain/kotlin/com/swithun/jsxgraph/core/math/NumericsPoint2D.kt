/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.math

/**
 * Point-like input accepted by JSXGraph numerical interpolation helpers.
 *
 * JSXGraph 1.13.3: src/math/numerics.js -> CardinalSpline(points, ...).
 */
internal interface NumericsPoint2D {
    fun X(): Double

    fun Y(): Double
}
