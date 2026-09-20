/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.geometry.Offset
import com.swithun.jsxgraph.core.JsxGraphAutoRadiusAngle
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphElementStyle
import com.swithun.jsxgraph.core.JsxGraphPoint2D
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CurvePathCommandTest {
    @Test
    fun convertsCompleteCubicGroupsAndPreservesPathBreaks() {
        val commands = curvePathCommands(
            points = listOf(
                Offset(0.0f, 0.0f),
                Offset(1.0f, 0.0f),
                Offset(1.0f, 1.0f),
                Offset(2.0f, 1.0f),
                null,
                Offset(3.0f, 3.0f),
                Offset(4.0f, 3.0f),
                Offset(4.0f, 4.0f),
                Offset(5.0f, 4.0f),
            ),
            bezierDegree = 3,
        )

        assertEquals(4, commands.size)
        assertEquals(
            Offset(0.0f, 0.0f),
            assertIs<CurvePathCommand.MoveTo>(commands[0]).point,
        )
        assertEquals(
            Offset(2.0f, 1.0f),
            assertIs<CurvePathCommand.CubicTo>(commands[1]).end,
        )
        assertEquals(
            Offset(3.0f, 3.0f),
            assertIs<CurvePathCommand.MoveTo>(commands[2]).point,
        )
        assertEquals(
            Offset(5.0f, 4.0f),
            assertIs<CurvePathCommand.CubicTo>(commands[3]).end,
        )
    }

    @Test
    fun ignoresIncompleteCubicGroupsAndUnsupportedDegrees() {
        val incomplete = curvePathCommands(
            points = listOf(
                Offset.Zero,
                Offset(1.0f, 0.0f),
                Offset(1.0f, 1.0f),
            ),
            bezierDegree = 3,
        )

        assertEquals(1, incomplete.size)
        assertIs<CurvePathCommand.MoveTo>(incomplete.single())
        assertTrue(
            curvePathCommands(
                points = listOf(Offset.Zero, Offset(1.0f, 1.0f)),
                bezierDegree = 2,
            ).isEmpty(),
        )
    }

    @Test
    fun automaticAngleUsesCssPixelScaleAtEachViewportWidth() {
        val curve = JsxGraphSceneElement.Curve(
            id = "angle",
            name = "",
            style = JsxGraphElementStyle(
                visible = true,
                strokeColor = JsxGraphColor(0, 0, 0),
                fillColor = JsxGraphColor.Transparent,
                strokeWidth = 1.0,
                strokeOpacity = 1.0,
                fillOpacity = 0.0,
            ),
            points = emptyList(),
            bezierDegree = 3,
            lineCap = "round",
            autoRadiusAngle = JsxGraphAutoRadiusAngle(
                first = JsxGraphPoint2D(0.1, 0.0),
                vertex = JsxGraphPoint2D(0.0, 0.0),
                third = JsxGraphPoint2D(0.0, 0.1),
                sign = 1.0,
            ),
        )

        val desktop = curveScreenPoints(
            curve = curve,
            metrics = BoardMetrics(width = 1_200.0f, height = 1_000.0f),
            density = 2.0f,
        )
        val compact = curveScreenPoints(
            curve = curve,
            metrics = BoardMetrics(width = 600.0f, height = 500.0f),
            density = 2.0f,
        )

        assertEquals(
            40.0f,
            assertIs<Offset>(desktop[3]).x -
                assertIs<Offset>(desktop[0]).x,
            absoluteTolerance = 1.0e-4f,
        )
        assertEquals(
            40.0f,
            assertIs<Offset>(compact[3]).x -
                assertIs<Offset>(compact[0]).x,
            absoluteTolerance = 1.0e-4f,
        )
    }
}
