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

class AxisTest {
    @Test
    fun factoryCreatesLineDefaultTicksAndOfficialRelationships() {
        val board = board()
        val point1 = point(board, 0.0, 0.0)
        val point2 = point(board, 1.0, 0.0)

        val axis = axis(
            Axis.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = "axis",
            ),
        )
        val ticks = assertIs<Ticks>(axis.defaultTicks)

        assertEquals(Const.OBJECT_TYPE_AXIS, axis.type)
        assertEquals(Const.OBJECT_TYPE_LINE, axis.originalType)
        assertEquals("axis", axis.elType)
        assertFalse(axis.isDraggable)
        assertFalse(point1.isDraggable)
        assertFalse(point2.isDraggable)
        assertEquals(Const.OBJECT_TYPE_AXISPOINT, point1.type)
        assertEquals(Const.OBJECT_TYPE_AXISPOINT, point2.type)
        assertSame(ticks, axis.subs["ticks"])
        assertTrue(ticks in axis.inherits)
        assertTrue(ticks in axis.ticks)
        assertFalse(ticks.dump)
        assertEquals(false, axis.needsRegularUpdate)
        assertEquals(false, ticks.needsRegularUpdate)
        assertTrue(ticks.attributes.drawLabels)
        assertTrue(ticks.attributes.insertTicks)
        assertEquals(-1.0, ticks.attributes.majorHeight)
        assertContentEquals(
            doubleArrayOf(0.0, 1.0),
            ticks.attributes.tickEndings,
        )
        assertContentEquals(
            doubleArrayOf(4.0, -9.0),
            ticks.attributes.labelOffset,
        )

        board.removeObject(axis)

        assertNull(board.elementById(axis.id))
        assertNull(board.elementById(ticks.id))
        assertEquals(setOf(point1.id, point2.id), board.objects.keys)
    }

    @Test
    fun fixedAndStickyPositionsMatchCreateAxisBranches() {
        val board = board()
        val fixedHorizontal = axis(
            Axis.create(
                board = board,
                point1 = point(board, 0.0, 0.0),
                point2 = point(board, 1.0, 0.0),
                attributes = AxisAttributes(
                    position = "fixed",
                    anchor = "right",
                ),
                id = "fixedHorizontal",
            ),
        )
        assertEquals(-4.0, fixedHorizontal.point1.Y())
        assertEquals(-4.0, fixedHorizontal.point2.Y())

        val fixedVertical = axis(
            Axis.create(
                board = board,
                point1 = point(board, 0.0, 0.0),
                point2 = point(board, 0.0, 1.0),
                attributes = AxisAttributes(
                    position = "fixed",
                    anchor = "left",
                ),
                id = "fixedVertical",
            ),
        )
        assertEquals(-4.0, fixedVertical.point1.X())
        assertEquals(-4.0, fixedVertical.point2.X())

        val sticky = axis(
            Axis.create(
                board = board,
                point1 = point(board, 0.0, 8.0),
                point2 = point(board, 1.0, 8.0),
                attributes = AxisAttributes(
                    position = "sticky",
                    anchor = "left",
                ),
                id = "sticky",
            ),
        )
        assertEquals(4.0, sticky.point1.Y())
        assertEquals(4.0, sticky.point2.Y())
    }

    @Test
    fun viewportResolutionPreservesPercentFractionAndPixelUnits() {
        val definition = AxisDefinition(
            originalPoint1 = doubleArrayOf(1.0, 0.0, 0.0),
            originalPoint2 = doubleArrayOf(1.0, 1.0, 0.0),
            attributes = AxisAttributes(
                position = "fixed",
                anchor = "right",
                anchorDist = AxisDistance.Pixels(25.0),
            ),
        )

        val resolved = definition.resolve(
            currentPoint1 = doubleArrayOf(1.0, 0.0, 0.0),
            currentPoint2 = doubleArrayOf(1.0, 1.0, 0.0),
            boundingBox = doubleArrayOf(-5.0, 5.0, 5.0, -5.0),
            pixelsPerUnitX = 50.0,
            pixelsPerUnitY = 40.0,
        )

        assertEquals(-4.5, resolved.point1[2])
        assertEquals(-4.5, resolved.point2[2])
        assertEquals(
            1.0,
            AxisDistance.Percent(10.0).resolve(10.0, 0.02),
        )
        assertEquals(
            2.5,
            AxisDistance.Fraction(0.25).resolve(10.0, 0.02),
        )
    }

    @Test
    fun ticksRegistrationFailureRollsBackAxisAndRestoresPoints() {
        val board = board()
        val point1 = point(board, 0.0, 0.0)
        val point2 = point(board, 1.0, 0.0)
        val originalObjectIds = board.objects.keys.toSet()

        val error = assertIs<GMResult.Err<AxisError>>(
            Axis.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = "axis",
                ticksId = point1.id,
            ),
        ).error

        assertIs<AxisError.TicksFactory>(error)
        assertEquals(originalObjectIds, board.objects.keys)
        assertEquals(Const.OBJECT_TYPE_POINT, point1.type)
        assertEquals(Const.OBJECT_TYPE_POINT, point2.type)
        assertTrue(point1.isDraggable)
        assertTrue(point2.isDraggable)
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        id = "board",
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                name = "",
            ),
        ).value

    private fun axis(result: GMResult<Line, AxisError>): Line =
        assertIs<GMResult.Ok<Line>>(result).value
}
