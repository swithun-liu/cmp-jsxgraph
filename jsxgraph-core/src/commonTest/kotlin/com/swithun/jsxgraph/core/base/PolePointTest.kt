/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PolePointTest {
    @Test
    fun officialGeometryParentOrderAndUpdatesArePreserved() {
        val board = board("geometry")
        val center = point(board, 1.0, 1.0, "center")
        val radiusPoint = point(board, 3.0, 1.0, "radiusPoint")
        val circle = circle(board, center, radiusPoint, "sourceCircle")
        val linePoint1 = point(board, -1.0, 4.0, "linePoint1")
        val linePoint2 = point(board, 4.0, -1.0, "linePoint2")
        val line = line(board, linePoint1, linePoint2, "sourceLine")
        val circleFirst = polePoint(
            PolePoint.create(
                board = board,
                firstParent = circle,
                secondParent = line,
                id = "circleFirst",
                name = "",
            ),
        )
        val lineFirst = polePoint(
            PolePoint.create(
                board = board,
                firstParent = line,
                secondParent = circle,
                id = "lineFirst",
                name = "",
            ),
        )
        board.update()

        for (point in listOf(circleFirst, lineFirst)) {
            assertEquals("polepoint", point.elType)
            assertEquals(Const.OBJECT_TYPE_CAS, point.type)
            assertEquals(Const.OBJECT_CLASS_POINT, point.elementClass)
            assertTrue(point.isConstrained)
            assertFalse(point.isDraggable)
            assertFalse(point.isFixed)
            assertTrue(point.dump)
            assertEquals(listOf("sourceCircle", "sourceLine"), point.parents)
            assertCoordinates(
                expected = doubleArrayOf(1.0, 5.0, 5.0),
                actual = point.coords.usrCoords,
            )
            assertEquals(
                setOf(
                    "sourceCircle",
                    "center",
                    "radiusPoint",
                    "sourceLine",
                    "linePoint1",
                    "linePoint2",
                ),
                point.ancestors.keys,
            )
        }
        assertEquals(
            listOf(
                "center",
                "radiusPoint",
                "sourceCircle",
                "linePoint1",
                "linePoint2",
                "sourceLine",
                "circleFirst",
                "lineFirst",
            ),
            board.objectsList.map(GeometryElement::id),
        )
        assertEquals(
            setOf("circleFirst", "lineFirst"),
            circle.childElements.keys,
        )
        assertEquals(
            setOf("circleFirst", "lineFirst"),
            line.childElements.keys,
        )

        center.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 2.0),
        )
        radiusPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, 2.0),
        )
        linePoint1.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-3.0, -1.0),
        )
        linePoint2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 4.0),
        )
        board.update()

        for (point in listOf(circleFirst, lineFirst)) {
            assertCoordinates(
                expected = doubleArrayOf(
                    1.0,
                    2.499999999999999,
                    -2.499999999999999,
                ),
                actual = point.coords.usrCoords,
            )
        }
    }

    @Test
    fun directAndParentRemovalMatchOfficialLifecycle() {
        val board = board("direct-removal")
        val circle = circle(
            board,
            point(board, 0.0, 0.0, "center"),
            point(board, 2.0, 0.0, "radius"),
            "circle",
        )
        val line = line(
            board,
            point(board, -2.0, -2.0, "linePoint1"),
            point(board, 2.0, 2.0, "linePoint2"),
            "line",
        )
        val pole = polePoint(
            PolePoint.create(
                board = board,
                firstParent = circle,
                secondParent = line,
                id = "pole",
            ),
        )

        board.removeObject(pole)

        assertSame(circle, board.elementById("circle"))
        assertSame(line, board.elementById("line"))
        assertEquals(null, board.elementById("pole"))
        assertTrue(circle.childElements.isEmpty())
        assertTrue(line.childElements.isEmpty())

        val parentBoard = board("parent-removal")
        val parentCircle = circle(
            parentBoard,
            point(parentBoard, 0.0, 0.0, "parentCenter"),
            point(parentBoard, 2.0, 0.0, "parentRadius"),
            "parentCircle",
        )
        val parentLine = line(
            parentBoard,
            point(parentBoard, -2.0, -2.0, "parentLinePoint1"),
            point(parentBoard, 2.0, 2.0, "parentLinePoint2"),
            "parentLine",
        )
        val parentPole = polePoint(
            PolePoint.create(
                board = parentBoard,
                firstParent = parentCircle,
                secondParent = parentLine,
                id = "parentPole",
            ),
        )

        parentBoard.removeObject(parentCircle)

        assertEquals(null, parentBoard.elementById(parentCircle.id))
        assertEquals(null, parentBoard.elementById(parentPole.id))
        assertSame(parentLine, parentBoard.elementById(parentLine.id))
        assertTrue(parentLine.childElements.isEmpty())
        assertEquals(
            listOf(
                "parentCenter",
                "parentRadius",
                "parentLinePoint1",
                "parentLinePoint2",
                "parentLine",
            ),
            parentBoard.objectsList.map(GeometryElement::id),
        )
    }

    @Test
    fun degenerateIdealLinePropagatesOfficialNaNArithmetic() {
        val board = board("degenerate")
        val circle = circle(
            board,
            point(board, 0.0, 0.0, "center"),
            point(board, 2.0, 0.0, "radius"),
            "circle",
        )
        val line = line(
            board,
            homogeneousPoint(
                board,
                doubleArrayOf(0.0, 1.0, 0.0),
                "idealPoint1",
            ),
            homogeneousPoint(
                board,
                doubleArrayOf(0.0, 0.0, 1.0),
                "idealPoint2",
            ),
            "idealLine",
        )
        val pole = polePoint(
            PolePoint.create(
                board = board,
                firstParent = circle,
                secondParent = line,
                id = "idealPole",
            ),
        )
        board.update()

        assertTrue(line.stdform.take(3).all { value -> !value.isFinite() })
        assertTrue(pole.coords.usrCoords.all(Double::isNaN))
    }

    @Test
    fun invalidParentsAndDuplicateIdsAreStructuredAndAtomic() {
        val board = board("errors")
        val circle = circle(
            board,
            point(board, 0.0, 0.0, "center"),
            point(board, 2.0, 0.0, "radius"),
            "circle",
        )
        val point1 = point(board, -1.0, -1.0, "point1")
        val point2 = point(board, 1.0, 1.0, "point2")
        val line = line(board, point1, point2, "line")

        assertEquals(
            PolePointError.UnsupportedParents(
                listOf("circle", "point"),
            ),
            assertIs<GMResult.Err<PolePointError>>(
                PolePoint.create(
                    board = board,
                    firstParent = circle,
                    secondParent = point1,
                ),
            ).error,
        )

        val otherBoard = board("other")
        val foreignLine = line(
            otherBoard,
            point(otherBoard, -1.0, 0.0, "foreignPoint1"),
            point(otherBoard, 1.0, 0.0, "foreignPoint2"),
            "foreignLine",
        )
        assertEquals(
            PolePointError.ParentBoardMismatch(1),
            assertIs<GMResult.Err<PolePointError>>(
                PolePoint.create(
                    board = board,
                    firstParent = circle,
                    secondParent = foreignLine,
                ),
            ).error,
        )

        board.removeObject(line)
        assertEquals(
            PolePointError.ParentNotRegistered(1, "line"),
            assertIs<GMResult.Err<PolePointError>>(
                PolePoint.create(
                    board = board,
                    firstParent = circle,
                    secondParent = line,
                ),
            ).error,
        )

        val replacementLine = line(
            board,
            point1,
            point2,
            "replacementLine",
        )
        val taken = point(board, 8.0, 8.0, "taken")
        val before = board.objects.keys.toSet()
        val failure = assertIs<
            GMResult.Err<PolePointError.PointCreation>,
            >(
            PolePoint.create(
                board = board,
                firstParent = replacementLine,
                secondParent = circle,
                id = taken.id,
            ),
        ).error

        assertEquals(
            PointError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            failure.error,
        )
        assertEquals(before, board.objects.keys.toSet())
        assertSame(taken, board.elementById("taken"))
        assertTrue(circle.childElements.isEmpty())
        assertTrue(replacementLine.childElements.isEmpty())
    }

    private fun board(id: String): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = id,
        )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
    ): Point = homogeneousPoint(
        board = board,
        coordinates = doubleArrayOf(x, y),
        id = id,
    )

    private fun homogeneousPoint(
        board: Board,
        coordinates: DoubleArray,
        id: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = coordinates,
                id = id,
                name = "",
            ),
        ).value

    private fun circle(
        board: Board,
        center: Point,
        radiusPoint: Point,
        id: String,
    ): Circle =
        assertIs<GMResult.Ok<Circle>>(
            Circle.create(
                board = board,
                center = center,
                point2 = radiusPoint,
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

    private fun polePoint(
        result: GMResult<Point, PolePointError>,
    ): Point = assertIs<GMResult.Ok<Point>>(result).value

    private fun assertCoordinates(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertTrue(
                abs(expected[index] - actual[index]) <= TOLERANCE,
                "Expected ${expected[index]} at $index, got ${actual[index]}",
            )
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
