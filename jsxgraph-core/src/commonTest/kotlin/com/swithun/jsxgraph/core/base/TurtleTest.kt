/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TurtleTest {
    @Test
    fun creationAndDrawingMatchOfficialStateAndPathValues() {
        val board = board()
        val turtle = turtle(
            Turtle.create(
                board = board,
                x = 1.0,
                y = 2.0,
                direction = 30.0,
                penAttributes = TurtlePenAttributes(
                    strokeWidth = 2.0,
                    strokeColor = "#123456",
                    highlightStrokeColor = "#abcdef",
                ),
                id = "turtle",
                name = "",
            ),
        )

        assertNull(board.elementById("turtle"))
        assertEquals(Const.OBJECT_TYPE_TURTLE, turtle.type)
        assertEquals(Const.OBJECT_CLASS_OTHER, turtle.elementClass)
        assertEquals("turtle", turtle.elType)
        assertContentEquals(doubleArrayOf(1.0, 2.0), turtle.position)
        assertEquals(30.0, turtle.direction)
        assertEquals(4, turtle.objects.size)
        assertEquals(
            listOf(
                Const.OBJECT_TYPE_CURVE,
                Const.OBJECT_TYPE_POINT,
                Const.OBJECT_TYPE_POINT,
                Const.OBJECT_TYPE_LINE,
            ),
            turtle.objects.map(GeometryElement::type),
        )
        assertPoint(turtle.turtle, 1.0, 2.0)
        assertPoint(
            turtle.turtle2,
            1.3061862178478973,
            2.176776695296637,
        )
        assertEquals(2.0, turtle.getPenSize())
        assertEquals("#123456", turtle.getPenColor())
        assertEquals("#abcdef", turtle.getHighlightPenColor())
        assertEquals(1.0, turtle.maxX())
        assertTrue(turtle.X(0.0).isNaN())
        assertEquals(1.0, turtle.X(1.0))

        ok(turtle.forward(4.0))
        turtle.left(60.0)
        ok(turtle.forward(2.0))
        turtle.penUp()
        ok(turtle.moveTo(doubleArrayOf(-2.0, 3.0)))
        ok(turtle.penDown())
        ok(turtle.setPenSize(5.0))
        ok(turtle.setPenColor("#654321"))
        ok(turtle.setHighlightPenColor("#fedcba"))
        ok(turtle.lookTo(doubleArrayOf(0.0, 0.0)))
        ok(turtle.forward(3.0))
        turtle.pushTurtle()
        turtle.right(45.0)
        ok(turtle.forward(1.0))

        assertPoint(
            turtle.turtle,
            -0.532015546462498,
            -0.47673155870445183,
        )
        assertEquals(
            -101.30993247402023,
            turtle.direction,
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(9.0, turtle.maxX())
        assertEquals(5, turtle.objects.filterIsInstance<Curve>().size)
        assertCurve(
            turtle.objects.filterIsInstance<Curve>().first(),
            doubleArrayOf(1.0, 4.464101615137755, 4.464101615137755),
            doubleArrayOf(2.0, 4.0, 6.0),
        )
        assertCurve(
            turtle.objects.filterIsInstance<Curve>().last(),
            doubleArrayOf(
                -2.0,
                -0.33589941132431367,
                -0.532015546462498,
            ),
            doubleArrayOf(
                3.0,
                0.5038491169864683,
                -0.47673155870445183,
            ),
        )
        assertEquals(5.0, turtle.getPenSize())
        assertEquals("#654321", turtle.getPenColor())
        assertEquals("#fedcba", turtle.getHighlightPenColor())
        assertEquals(1.0, turtle.X(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(
            4.464101615137755,
            turtle.X(1.0),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            6.0,
            turtle.Y(2.0),
            absoluteTolerance = TOLERANCE,
        )
        assertTrue(turtle.X(3.0).isNaN())
    }

    @Test
    fun stackVisibilityCleanAndClearMatchOfficialLifecycle() {
        val board = board()
        val turtle = turtle(
            Turtle.create(
                board = board,
                x = 1.0,
                y = 2.0,
                direction = 30.0,
                id = "turtle",
                name = "",
            ),
        )
        ok(turtle.forward(4.0))
        ok(turtle.setPenSize(2.0))
        turtle.pushTurtle()
        turtle.right(45.0)
        ok(turtle.forward(1.0))

        ok(turtle.popTurtle())
        assertTrue(turtle.stack.isEmpty())
        assertContentEquals(
            doubleArrayOf(4.464101615137755, 4.0),
            turtle.position,
        )
        assertEquals(30.0, turtle.direction, absoluteTolerance = TOLERANCE)

        turtle.hideTurtle()
        val hiddenHead = turtle.turtle.Coords()
        ok(turtle.forward(2.0))
        assertTrue(turtle.turtleIsHidden)
        assertFalse(turtle.arrowVisible)
        assertContentEquals(hiddenHead, turtle.turtle.Coords())

        ok(turtle.showTurtle())
        assertFalse(turtle.turtleIsHidden)
        assertTrue(turtle.arrowVisible)
        assertPoint(
            turtle.turtle,
            turtle.position[0],
            turtle.position[1],
        )

        val curvesBeforeClean =
            turtle.objects.filterIsInstance<Curve>().toList()
        ok(turtle.clean())
        val curvesAfterClean = turtle.objects.filterIsInstance<Curve>()
        assertFalse(curvesBeforeClean[0] in curvesAfterClean)
        assertFalse(curvesBeforeClean[1] in curvesAfterClean)
        assertTrue(curvesBeforeClean[2] in curvesAfterClean)
        assertSame(turtle.curve, curvesAfterClean.last())

        ok(turtle.clearScreen())
        assertContentEquals(doubleArrayOf(0.0, 0.0), turtle.position)
        assertEquals(90.0, turtle.direction)
        assertTrue(turtle.isPenDown)
        assertTrue(turtle.stack.isEmpty())
        assertEquals(4, turtle.objects.size)
        assertFalse(turtle.arrowVisible)
        assertEquals(1.0, turtle.maxX())
    }

    @Test
    fun invalidInputAndEmptyStackReturnStructuredErrors() {
        val board = board()
        val before = board.objects.keys.toList()
        assertEquals(
            TurtleError.InvalidCoordinate("x", Double.NaN),
            assertIs<GMResult.Err<TurtleError.InvalidCoordinate>>(
                Turtle.create(
                    board = board,
                    x = Double.NaN,
                ),
            ).error,
        )
        assertEquals(before, board.objects.keys.toList())

        val turtle = turtle(Turtle.create(board = board, name = ""))
        assertEquals(
            TurtleError.EmptyStack,
            assertIs<GMResult.Err<TurtleError.EmptyStack>>(
                turtle.popTurtle(),
            ).error,
        )
        assertEquals(
            TurtleError.InvalidPositionCount(1),
            assertIs<GMResult.Err<TurtleError.InvalidPositionCount>>(
                turtle.moveTo(doubleArrayOf(1.0)),
            ).error,
        )
    }

    private fun board(): Board =
        Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 40.0,
            unitY = 40.0,
            id = "board",
        )

    private fun turtle(
        result: GMResult<Turtle, TurtleError>,
    ): Turtle = assertIs<GMResult.Ok<Turtle>>(result).value

    private fun ok(
        result: GMResult<Turtle, TurtleError>,
    ): Turtle = assertIs<GMResult.Ok<Turtle>>(result).value

    private fun assertPoint(
        point: Point,
        x: Double,
        y: Double,
    ) {
        assertEquals(x, point.X(), absoluteTolerance = TOLERANCE)
        assertEquals(y, point.Y(), absoluteTolerance = TOLERANCE)
    }

    private fun assertCurve(
        curve: Curve,
        expectedX: DoubleArray,
        expectedY: DoubleArray,
    ) {
        assertContentEquals(expectedX, curve.dataX)
        assertContentEquals(expectedY, curve.dataY)
        assertEquals(expectedX.size, curve.numberPoints)
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
