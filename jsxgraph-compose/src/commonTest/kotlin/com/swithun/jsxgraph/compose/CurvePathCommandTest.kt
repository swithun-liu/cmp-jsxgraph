/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.geometry.Offset
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
}
