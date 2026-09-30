/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AngleBehaviorTest {
    @Test
    fun displayTypesMatchOfficialGeometryAndDotState() {
        val sector = angle(type = "sector", orthoType = "sector")
        assertEquals("sector", sector.activeAngleDisplayType)
        assertEquals(3, sector.bezierDegree)
        assertEquals(19, sector.numberPoints)
        assertFalse(sector.dotVisible)
        assertPoint(requireNotNull(sector.dot), 0.0, 0.0)

        val square = angle(type = "square")
        assertEquals("square", square.activeAngleDisplayType)
        assertEquals(1, square.bezierDegree)
        assertEquals(5, square.numberPoints)
        assertPoint(square.points[0], 0.0, 0.0)
        assertPoint(square.points[1], 2.0, 0.0)
        assertPoint(square.points[2], 2.0, 2.0)
        assertPoint(square.points[3], 0.0, 2.0)
        assertPoint(square.points[4], 0.0, 0.0)

        val none = angle(type = "none")
        assertEquals("none", none.activeAngleDisplayType)
        assertEquals(1, none.bezierDegree)
        assertEquals(1, none.numberPoints)
        assertTrue(none.points.single().usrCoords[1].isNaN())

        val dot = angle(type = "sectordot")
        assertEquals("sectordot", dot.activeAngleDisplayType)
        assertTrue(dot.dotVisible)
        assertPoint(
            requireNotNull(dot.dot),
            sqrt(0.5),
            sqrt(0.5),
        )
    }

    @Test
    fun rightAngleUsesOrthoTypeAndSetAngleCanBeFreed() {
        val board = board()
        val first = point(board, 4.0, 0.0, "first")
        val vertex = point(board, 0.0, 0.0, "vertex")
        val third = point(board, 0.0, 4.0, "third")
        val angle = assertIs<GMResult.Ok<Sector>>(
            Sector.createAngle(
                board = board,
                first = first,
                vertex = vertex,
                third = third,
                radius = AngleRadius.Fixed(2.0),
                displayAttributes = AngleDisplayAttributes(
                    type = "sector",
                    orthoType = "square",
                ),
                id = "angle",
                name = "",
            ),
        ).value

        assertEquals("square", angle.activeAngleDisplayType)
        assertIs<GMResult.Ok<Sector>>(angle.setAngle(PI / 3.0))
        assertTrue(angle.hasFixedAngle)
        assertEquals(1, third.transformations.size)
        assertEquals(listOf("first"), third.parents)
        assertPoint(third, 2.0, 2.0 * sqrt(3.0))
        assertEquals(PI / 3.0, angle.Value("radians"), 1.0e-12)

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, 1.0),
        )
        board.fullUpdate()
        assertPoint(
            third,
            1.5 - 0.5 * sqrt(3.0),
            0.5 + 1.5 * sqrt(3.0),
        )

        assertSame(angle, angle.free())
        assertFalse(angle.hasFixedAngle)
        assertTrue(third.transformations.isEmpty())
        assertTrue(third.parents.isEmpty())
        third.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 3.0),
        )
        board.fullUpdate()
        assertPoint(third, -2.0, 3.0)
    }

    @Test
    fun twoLineAngleKeepsVirtualPointsAndMutationMethodsAreNoOps() {
        val board = board()
        val origin = point(board, 0.0, 0.0, "origin")
        val horizontal = point(board, 4.0, 0.0, "horizontal")
        val vertical = point(board, 0.0, 4.0, "vertical")
        val line1 = line(board, origin, horizontal, "line1")
        val line2 = line(board, origin, vertical, "line2")
        val angle = assertIs<GMResult.Ok<Sector>>(
            Sector.createAngleFromLines(
                board = board,
                line1 = line1,
                line2 = line2,
                direction1 = SectorDirection.Sign(-1.0),
                direction2 = SectorDirection.Sign(1.0),
                radius = AngleRadius.Fixed(2.0),
                displayAttributes = AngleDisplayAttributes(
                    orthoType = "sector",
                ),
                id = "angle",
                name = "",
            ),
        ).value

        assertTrue(angle.isTwoLine)
        assertPoint(angle.point1, 0.0, 0.0)
        assertPoint(angle.point2, -2.0, 0.0)
        assertPoint(angle.point3, 0.0, 2.0)
        assertEquals(listOf("line1", "line2"), angle.parents)
        assertIs<GMResult.Ok<Sector>>(angle.setAngle(Double.NaN))
        assertSame(angle, angle.free())
        assertFalse(angle.hasFixedAngle)
        assertTrue(angle.point3.transformations.isEmpty())
        assertPoint(angle.point2, -2.0, 0.0)
        assertPoint(angle.point3, 0.0, 2.0)

        val auto = assertIs<GMResult.Ok<Sector>>(
            Sector.createAngleFromLines(
                board = board,
                line1 = line1,
                line2 = line2,
                direction1 = SectorDirection.Sign(1.0),
                direction2 = SectorDirection.Sign(1.0),
                radius = AngleRadius.Auto,
                displayAttributes = AngleDisplayAttributes(
                    orthoType = "sector",
                ),
                name = "",
            ),
        ).value
        assertEquals(1.25, auto.Radius(), absoluteTolerance = 1.0e-12)
    }

    private fun angle(
        type: String,
        orthoType: String = type,
    ): Sector {
        val board = board()
        return assertIs<GMResult.Ok<Sector>>(
            Sector.createAngle(
                board = board,
                first = point(board, 4.0, 0.0, "first"),
                vertex = point(board, 0.0, 0.0, "vertex"),
                third = point(board, 0.0, 4.0, "third"),
                radius = AngleRadius.Fixed(2.0),
                displayAttributes = AngleDisplayAttributes(
                    type = type,
                    orthoType = orthoType,
                ),
                id = "angle",
                name = "",
            ),
        ).value
    }

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = "",
            ),
        ).value

    private fun line(
        board: Board,
        point1: Point,
        point2: Point,
        id: String,
    ): Line =
        assertIs<GMResult.Ok<Line>>(
            Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = id,
                name = "",
            ),
        ).value

    private fun board(): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        id = "board",
    )

    private fun assertPoint(
        point: Coords,
        x: Double,
        y: Double,
    ) {
        assertEquals(x, point.usrCoords[1], absoluteTolerance = 1.0e-12)
        assertEquals(y, point.usrCoords[2], absoluteTolerance = 1.0e-12)
    }

    private fun assertPoint(
        point: Point,
        x: Double,
        y: Double,
    ) {
        assertEquals(x, point.X(), absoluteTolerance = 1.0e-12)
        assertEquals(y, point.Y(), absoluteTolerance = 1.0e-12)
    }
}
