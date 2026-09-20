/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.swithun.jsxgraph.core.JsxGraphArrowHead
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LineArrowGeometryTest {
    @Test
    fun shortensLineByOfficialOffsetForEveryArrowType() {
        val expectedOffsets = mapOf(
            1 to 12.0f,
            2 to 6.0f,
            3 to 4.0f,
            4 to 8.0f,
            5 to 8.0f,
            6 to 8.0f,
            7 to 0.0f,
        )

        for ((type, expectedOffset) in expectedOffsets) {
            val geometry = assertNotNull(
                lineRenderGeometry(
                    point1 = Offset.Zero,
                    point2 = Offset(100.0f, 0.0f),
                    straightFirst = false,
                    straightLast = false,
                    viewportSize = Size(200.0f, 100.0f),
                    strokeWidth = 2.0f,
                    firstArrow = null,
                    lastArrow = arrow(type),
                ),
            )

            assertEquals(0.0f, geometry.strokeStart.x)
            assertEquals(
                100.0f - expectedOffset,
                geometry.strokeEnd.x,
                absoluteTolerance = 1.0e-5f,
            )
            assertEquals(0.0f, geometry.strokeEnd.y)
        }
    }

    @Test
    fun translatesAllCanvasArrowShapesIncludingBezierTypeSeven() {
        val expectedPointCounts = mapOf(
            1 to 3,
            2 to 4,
            3 to 4,
            4 to 13,
            5 to 13,
            6 to 13,
            7 to 7,
        )

        for (type in 1..7) {
            val geometry = assertNotNull(
                lineRenderGeometry(
                    point1 = Offset.Zero,
                    point2 = Offset(100.0f, 0.0f),
                    straightFirst = false,
                    straightLast = false,
                    viewportSize = Size(200.0f, 100.0f),
                    strokeWidth = 2.0f,
                    firstArrow = arrow(type),
                    lastArrow = arrow(type),
                ),
            )
            val first = assertNotNull(geometry.firstArrow)
            val last = assertNotNull(geometry.lastArrow)

            assertEquals(expectedPointCounts.getValue(type), first.points.size)
            assertEquals(expectedPointCounts.getValue(type), last.points.size)
            assertEquals(if (type >= 4) 3 else 1, first.bezierDegree)
            assertEquals(if (type >= 4) 3 else 1, last.bezierDegree)
            assertEquals(type != 7, first.filled)
            assertEquals(type != 7, last.filled)
            assertTrue(first.points.all { it.x.isFinite() && it.y.isFinite() })
            assertTrue(last.points.all { it.x.isFinite() && it.y.isFinite() })
        }
    }

    @Test
    fun preservesArrowAnchorsAndMirrorsFirstAndLastHeads() {
        val geometry = assertNotNull(
            lineRenderGeometry(
                point1 = Offset.Zero,
                point2 = Offset(100.0f, 0.0f),
                straightFirst = false,
                straightLast = false,
                viewportSize = Size(200.0f, 100.0f),
                strokeWidth = 2.0f,
                firstArrow = arrow(type = 1),
                lastArrow = arrow(type = 1),
            ),
        )
        val first = assertNotNull(geometry.firstArrow)
        val last = assertNotNull(geometry.lastArrow)

        assertEquals(Offset(12.0f, -6.0f), first.points[0])
        assertEquals(Offset.Zero, first.points[1])
        assertEquals(Offset(12.0f, 6.0f), first.points[2])
        assertEquals(Offset(88.0f, -6.0f), last.points[0])
        assertEquals(Offset(100.0f, 0.0f), last.points[1])
        assertEquals(Offset(88.0f, 6.0f), last.points[2])
    }

    @Test
    fun typeSevenIgnoresConfiguredSizeAndKeepsLineEndpoint() {
        val small = assertNotNull(
            lineRenderGeometry(
                point1 = Offset.Zero,
                point2 = Offset(100.0f, 0.0f),
                straightFirst = false,
                straightLast = false,
                viewportSize = Size(200.0f, 100.0f),
                strokeWidth = 2.0f,
                firstArrow = null,
                lastArrow = arrow(type = 7, size = 1.0),
            ),
        )
        val large = assertNotNull(
            lineRenderGeometry(
                point1 = Offset.Zero,
                point2 = Offset(100.0f, 0.0f),
                straightFirst = false,
                straightLast = false,
                viewportSize = Size(200.0f, 100.0f),
                strokeWidth = 2.0f,
                firstArrow = null,
                lastArrow = arrow(type = 7, size = 100.0),
            ),
        )

        assertEquals(Offset(100.0f, 0.0f), small.strokeEnd)
        assertEquals(small.lastArrow, large.lastArrow)
        assertFalse(assertNotNull(small.lastArrow).filled)
        assertEquals(Offset(100.0f, 0.0f), small.lastArrow.points[3])
    }

    @Test
    fun shortLineKeepsStrokeUnshortenedButStillBuildsHeads() {
        val geometry = assertNotNull(
            lineRenderGeometry(
                point1 = Offset.Zero,
                point2 = Offset(5.0f, 0.0f),
                straightFirst = false,
                straightLast = false,
                viewportSize = Size(200.0f, 100.0f),
                strokeWidth = 2.0f,
                firstArrow = arrow(type = 1),
                lastArrow = arrow(type = 1),
            ),
        )

        assertEquals(Offset.Zero, geometry.strokeStart)
        assertEquals(Offset(5.0f, 0.0f), geometry.strokeEnd)
        assertNotNull(geometry.firstArrow)
        assertNotNull(geometry.lastArrow)
    }

    @Test
    fun straightArrowEndpointsUseOfficialFourPixelCanvasInset() {
        val geometry = assertNotNull(
            lineRenderGeometry(
                point1 = Offset(40.0f, 50.0f),
                point2 = Offset(60.0f, 50.0f),
                straightFirst = true,
                straightLast = true,
                viewportSize = Size(200.0f, 100.0f),
                strokeWidth = 2.0f,
                firstArrow = arrow(type = 1),
                lastArrow = arrow(type = 1),
            ),
        )

        assertEquals(16.0f, geometry.strokeStart.x)
        assertEquals(184.0f, geometry.strokeEnd.x)
        assertEquals(Offset(4.0f, 50.0f), geometry.firstArrow?.points?.get(1))
        assertEquals(
            Offset(196.0f, 50.0f),
            geometry.lastArrow?.points?.get(1),
        )
    }

    private fun arrow(
        type: Int,
        size: Double = 6.0,
    ): JsxGraphArrowHead =
        JsxGraphArrowHead(
            type = type,
            size = size,
            highlightSize = null,
        )
}
