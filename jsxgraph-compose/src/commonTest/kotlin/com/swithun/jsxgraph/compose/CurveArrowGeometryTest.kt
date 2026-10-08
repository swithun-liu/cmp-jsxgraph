/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.geometry.Offset
import com.swithun.jsxgraph.core.JsxGraphArrowHead
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CurveArrowGeometryTest {
    @Test
    fun arrowsUseFirstAndLastDrawableSubpathTangents() {
        val geometry = assertNotNull(
            curveArrowRenderGeometry(
                commands = listOf(
                    CurvePathCommand.MoveTo(Offset.Zero),
                    CurvePathCommand.LineTo(Offset(10.0f, 0.0f)),
                    CurvePathCommand.MoveTo(Offset(20.0f, 0.0f)),
                    CurvePathCommand.LineTo(Offset(20.0f, 10.0f)),
                ),
                strokeWidth = 2.0f,
                firstArrow = arrow(type = 1),
                lastArrow = arrow(type = 1),
            ),
        )

        val first = assertNotNull(geometry.firstArrow)
        val last = assertNotNull(geometry.lastArrow)
        assertEquals(Offset.Zero, first.points[1])
        assertEquals(Offset(20.0f, 10.0f), last.points[1])
        assertEquals(Offset(12.0f, -6.0f), first.points[0])
        assertEquals(Offset(26.0f, -2.0f), last.points[0])
    }

    @Test
    fun cubicArrowsUseEndpointControlTangentsWithDegenerateFallback() {
        val geometry = assertNotNull(
            curveArrowRenderGeometry(
                commands = listOf(
                    CurvePathCommand.MoveTo(Offset.Zero),
                    CurvePathCommand.CubicTo(
                        control1 = Offset.Zero,
                        control2 = Offset(10.0f, 0.0f),
                        end = Offset(10.0f, 10.0f),
                    ),
                ),
                strokeWidth = 2.0f,
                firstArrow = arrow(type = 7),
                lastArrow = arrow(type = 7),
            ),
        )

        val first = assertNotNull(geometry.firstArrow)
        val last = assertNotNull(geometry.lastArrow)
        assertEquals(Offset.Zero, first.points[3])
        assertEquals(Offset(10.0f, 10.0f), last.points[3])
        assertEquals(3, first.bezierDegree)
        assertEquals(3, last.bezierDegree)
    }

    @Test
    fun moveOnlyAndInvalidStrokeDoNotProduceArrowGeometry() {
        val moveOnly = assertNotNull(
            curveArrowRenderGeometry(
                commands = listOf(
                    CurvePathCommand.MoveTo(Offset.Zero),
                ),
                strokeWidth = 2.0f,
                firstArrow = arrow(type = 1),
                lastArrow = arrow(type = 1),
            ),
        )
        assertNull(moveOnly.firstArrow)
        assertNull(moveOnly.lastArrow)
        assertNull(
            curveArrowRenderGeometry(
                commands = listOf(
                    CurvePathCommand.MoveTo(Offset.Zero),
                    CurvePathCommand.LineTo(Offset(1.0f, 0.0f)),
                ),
                strokeWidth = Float.NaN,
                firstArrow = arrow(type = 1),
                lastArrow = null,
            ),
        )
    }

    private fun arrow(type: Int): JsxGraphArrowHead =
        JsxGraphArrowHead(
            type = type,
            size = 6.0,
            highlightSize = null,
        )
}
