/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsxGraphAutoRadiusAngleTest {
    @Test
    fun resolvesUpstreamMinimumProportionalAndMaximumRadii() {
        val angle = JsxGraphAutoRadiusAngle(
            first = JsxGraphPoint2D(6.0, 0.0),
            vertex = JsxGraphPoint2D(0.0, 0.0),
            third = JsxGraphPoint2D(0.0, 6.0),
            sign = 1.0,
        )

        assertRadius(
            points = angle.resolvePoints(cssPixelsPerUnitX = 10.0),
            expected = 2.0,
        )
        assertRadius(
            points = angle.resolvePoints(cssPixelsPerUnitX = 20.0),
            expected = 6.0 * 0.3333,
        )
        assertRadius(
            points = angle.resolvePoints(cssPixelsPerUnitX = 100.0),
            expected = 0.5,
        )
    }

    private fun assertRadius(
        points: List<JsxGraphPoint2D?>,
        expected: Double,
    ) {
        assertEquals(19, points.size)
        assertEquals(
            expected,
            assertIs<JsxGraphPoint2D>(points[3]).x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            expected,
            assertIs<JsxGraphPoint2D>(points[15]).y,
            absoluteTolerance = 1.0e-12,
        )
    }
}
