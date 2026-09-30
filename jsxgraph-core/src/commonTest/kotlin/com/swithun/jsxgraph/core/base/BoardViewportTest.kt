/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import kotlin.test.Test
import kotlin.test.assertEquals

class BoardViewportTest {
    @Test
    fun initBoardExpandsBoundingBoxWhenKeepingAspectRatio() {
        val board = Board.fromBoundingBox(
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
            keepAspectRatio = true,
            defaultCurveMinimum = -9.6,
            defaultCurveMaximum = 9.6,
        )

        assertEquals(31.25, board.unitX)
        assertEquals(31.25, board.unitY)
        assertEquals(250.0, board.origin.scrCoords[1])
        assertEquals(250.0, board.origin.scrCoords[2])
        assertBoundingBox(
            expected = doubleArrayOf(-8.0, 8.0, 8.0, -8.0),
            actual = board.getBoundingBox(),
        )
    }

    @Test
    fun initBoardPreservesRequestedBoxWithoutAspectRatio() {
        val board = Board.fromBoundingBox(
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
            keepAspectRatio = false,
            defaultCurveMinimum = -9.6,
            defaultCurveMaximum = 9.6,
        )

        assertEquals(31.25, board.unitX)
        assertEquals(500.0 / 12.0, board.unitY)
        assertBoundingBox(
            expected = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
            actual = board.getBoundingBox(),
        )
    }

    private fun assertBoundingBox(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected[index],
                actual[index],
                ABSOLUTE_TOLERANCE,
            )
        }
    }

    private companion object {
        const val ABSOLUTE_TOLERANCE = 1e-12
    }
}
