/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardConditionsTest {
    @Test
    fun conditionsApplyAllSupportedPropertiesAndTrackDependencies() {
        val board = board()
        val control = point(board, "control", "Control", 2.0, 3.0)
        val target = point(board, "target", "Target", 0.0, 0.0)

        val result = board.addConditions(
            """
            <data>Target.x=Control.X() + 1</data>
            <data>Target.y=Control.Y() - 1</data>
            <data>Target.visible=Control.X() > 0</data>
            <data>Target.stroke="#11223380"</data>
            <data>Target.fill="#aabbcc40"</data>
            <data>Target.strokewidth=5</data>
            <data>Target.position=0.25</data>
            <data>Target.style=4</data>
            """.trimIndent(),
        )

        assertIs<GMResult.Ok<Board>>(result)
        assertEquals(3.0, target.X())
        assertEquals(2.0, target.Y())
        assertTrue(target.visualPropertiesCalculated["visible"] == true)
        assertEquals("#112233", target.getAttribute("strokecolor"))
        assertEquals(128.0 / 255.0, target.getAttribute("strokeopacity"))
        assertEquals("#aabbcc", target.getAttribute("fillcolor"))
        assertEquals(64.0 / 255.0, target.getAttribute("fillopacity"))
        assertEquals(5.0, target.getAttribute("strokewidth"))
        assertEquals(0.25, target.position)
        assertEquals("circle", target.getAttribute("face"))
        assertEquals(2, target.getAttribute("size"))
        assertNull(board.takeConditionError())

        control.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(-4.0, 8.0),
        )
        board.update()

        assertEquals(-3.0, target.X())
        assertEquals(7.0, target.Y())
        assertFalse(target.visualPropertiesCalculated["visible"] == true)
        assertNull(board.takeConditionError())
    }

    @Test
    fun missingElementsAndUnsupportedPropertiesMatchUpstreamNoOp() {
        val board = board()
        val target = point(board, "target", "Target", 1.0, 2.0)

        assertIs<GMResult.Ok<Board>>(
            board.addConditions(
                """
                <data>Missing.x=10</data>
                <data>Target.label="ignored"</data>
                <data>Target.unknown=20</data>
                """.trimIndent(),
            ),
        )

        assertEquals(1.0, target.X())
        assertEquals(2.0, target.Y())
        assertFalse(board.updateConditions())
        assertNull(board.takeConditionError())
    }

    @Test
    fun malformedAndInvalidConditionsReturnStructuredErrors() {
        val board = board()
        point(board, "target", "Target", 1.0, 2.0)

        assertIs<GMResult.Err<BoardError.UnsupportedConditionExpression>>(
            board.addConditions("<data>Target.x=3"),
        )
        assertIs<GMResult.Err<BoardError.ConditionExpressionCompile>>(
            board.addConditions("<data>Target.x=(</data>"),
        )
        assertIs<GMResult.Err<BoardError.InvalidConditionValue>>(
            board.addConditions("<data>Target.stroke=1</data>"),
        )
        assertIs<BoardError.InvalidConditionValue>(
            board.takeConditionError(),
        )
        assertNull(board.takeConditionError())
    }

    private fun board(): Board =
        Board.fromBoundingBox(
            boundingBox = doubleArrayOf(-5.0, 5.0, 5.0, -5.0),
            keepAspectRatio = false,
            defaultCurveMinimum = -6.0,
            defaultCurveMaximum = 6.0,
        )

    private fun point(
        board: Board,
        id: String,
        name: String,
        x: Double,
        y: Double,
    ): Point {
        val point = Point(
            board = board,
            coordinates = doubleArrayOf(x, y),
            id = id,
            name = name,
        )
        assertIs<GMResult.Ok<String>>(
            board.setId(point, Const.OBJECT_TYPE_POINT),
        )
        point.setAttribute(mapOf("visible" to true))
        point.updateVisibility()
        return point
    }
}
