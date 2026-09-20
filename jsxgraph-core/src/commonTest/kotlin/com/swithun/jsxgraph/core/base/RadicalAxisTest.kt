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

class RadicalAxisTest {
    @Test
    fun officialGeometryMetadataAndUpdatesArePreserved() {
        val board = board("geometry")
        val center1 = point(board, -3.0, -1.0, "center1")
        val radius1 = point(board, -1.0, -1.0, "radius1")
        val circle1 = circle(board, center1, radius1, "circle1")
        val center2 = point(board, 2.0, 2.0, "center2")
        val radius2 = point(board, 5.0, 2.0, "radius2")
        val circle2 = circle(board, center2, radius2, "circle2")
        val axis = radicalAxis(
            RadicalAxis.create(
                board = board,
                circle1 = circle1,
                circle2 = circle2,
                id = "axis",
                name = "",
                straightFirst = false,
                straightLast = true,
                point1Id = "axisPoint1",
                point1Name = "",
                point2Id = "axisPoint2",
                point2Name = "",
            ),
        )
        board.update()

        assertEquals("radicalaxis", axis.elType)
        assertEquals(Const.OBJECT_TYPE_LINE, axis.type)
        assertEquals(Const.OBJECT_CLASS_LINE, axis.elementClass)
        assertTrue(axis.constrained)
        assertFalse(axis.isDraggable)
        assertFalse(axis.straightFirst)
        assertTrue(axis.straightLast)
        assertEquals(listOf("circle1", "circle2"), axis.parents)
        assertEquals(
            listOf<GeometryElement>(axis.point1, axis.point2),
            axis.inherits,
        )
        assertEquals(
            listOf(
                "center1",
                "radius1",
                "circle1",
                "center2",
                "radius2",
                "circle2",
                "axisPoint1",
                "axisPoint2",
                "axis",
            ),
            board.objectsList.map(GeometryElement::id),
        )
        assertHelper(
            point = axis.point1,
            expected = doubleArrayOf(
                1.0,
                1.6029411764705883,
                -3.8382352941176476,
            ),
        )
        assertHelper(
            point = axis.point2,
            expected = doubleArrayOf(
                1.0,
                0.5441176470588236,
                -2.073529411764706,
            ),
        )
        assertArrayMatches(
            expected = doubleArrayOf(
                -0.6002450479987809,
                -0.8574929257125443,
                -0.5144957554275265,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
            ),
            actual = axis.stdform,
        )
        assertSame(axis, circle1.childElements[axis.id])
        assertSame(axis, circle2.childElements[axis.id])
        assertSame(axis, axis.point1.childElements[axis.id])
        assertSame(axis, axis.point2.childElements[axis.id])
        assertEquals(
            setOf(
                "axisPoint1",
                "axisPoint2",
                "circle1",
                "circle2",
                "center1",
                "radius1",
                "center2",
                "radius2",
            ),
            axis.ancestors.keys,
        )

        center1.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-4.0, 1.0),
        )
        radius1.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-1.0, 1.0),
        )
        center2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, -2.0),
        )
        radius2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, -2.0),
        )
        board.update()

        assertArrayMatches(
            doubleArrayOf(
                1.0,
                -2.6323529411764706,
                -3.2205882352941178,
            ),
            axis.point1.coords.usrCoords,
        )
        assertArrayMatches(
            doubleArrayOf(
                1.0,
                -1.5735294117647058,
                -1.4558823529411766,
            ),
            axis.point2.coords.usrCoords,
        )
        assertArrayMatches(
            doubleArrayOf(
                -0.6002450479987806,
                -0.8574929257125441,
                0.5144957554275265,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            ),
            axis.stdform,
        )
    }

    @Test
    fun successfulRemovalPreservesImplicitHelpers() {
        val board = board("removal")
        val circle1 = circle(
            board,
            point(board, -2.0, 0.0, "center1"),
            point(board, -1.0, 0.0, "radius1"),
            "circle1",
        )
        val circle2 = circle(
            board,
            point(board, 2.0, 0.0, "center2"),
            point(board, 4.0, 0.0, "radius2"),
            "circle2",
        )
        val axis = radicalAxis(
            RadicalAxis.create(
                board = board,
                circle1 = circle1,
                circle2 = circle2,
                id = "axis",
                name = "",
                point1Id = "point1",
                point1Name = "",
                point2Id = "point2",
                point2Name = "",
            ),
        )
        val point1 = axis.point1
        val point2 = axis.point2

        board.removeObject(axis)

        assertSame(point1, board.elementById("point1"))
        assertSame(point2, board.elementById("point2"))
        assertTrue(circle1.childElements.isEmpty())
        assertTrue(circle2.childElements.isEmpty())
        assertTrue(point1.childElements.isEmpty())
        assertTrue(point2.childElements.isEmpty())

        val parentBoard = board("parent-removal")
        val parentCircle1 = circle(
            parentBoard,
            point(parentBoard, -2.0, 0.0, "parentCenter1"),
            point(parentBoard, -1.0, 0.0, "parentRadius1"),
            "parentCircle1",
        )
        val parentCircle2 = circle(
            parentBoard,
            point(parentBoard, 2.0, 0.0, "parentCenter2"),
            point(parentBoard, 4.0, 0.0, "parentRadius2"),
            "parentCircle2",
        )
        val parentAxis = radicalAxis(
            RadicalAxis.create(
                board = parentBoard,
                circle1 = parentCircle1,
                circle2 = parentCircle2,
                id = "parentAxis",
                name = "",
                point1Id = "parentPoint1",
                point1Name = "",
                point2Id = "parentPoint2",
                point2Name = "",
            ),
        )

        parentBoard.removeObject(parentCircle1)

        assertEquals(null, parentBoard.elementById(parentAxis.id))
        assertSame(
            parentAxis.point1,
            parentBoard.elementById("parentPoint1"),
        )
        assertSame(
            parentAxis.point2,
            parentBoard.elementById("parentPoint2"),
        )
        assertTrue(parentCircle2.childElements.isEmpty())
    }

    @Test
    fun degenerateCirclesFollowOfficialNaNArithmetic() {
        val board = board("degenerate")
        val commonCenter = point(board, 1.0, -1.0, "commonCenter")
        val circle1 = circle(
            board,
            commonCenter,
            point(board, 3.0, -1.0, "radiusA"),
            "circleA",
        )
        val circle2 = circle(
            board,
            commonCenter,
            point(board, 4.0, -1.0, "radiusB"),
            "circleB",
        )
        val concentric = radicalAxis(
            RadicalAxis.create(
                board = board,
                circle1 = circle1,
                circle2 = circle2,
                id = "concentric",
                point1Id = "concentricPoint1",
                point2Id = "concentricPoint2",
            ),
        )
        val duplicateParent = radicalAxis(
            RadicalAxis.create(
                board = board,
                circle1 = circle1,
                circle2 = circle1,
                id = "duplicateParent",
                point1Id = "duplicatePoint1",
                point2Id = "duplicatePoint2",
            ),
        )
        val identicalCircle1 = circle(
            board,
            point(board, -2.0, 2.0, "identicalCenter1"),
            point(board, 0.0, 2.0, "identicalRadius1"),
            "identicalCircle1",
        )
        val identicalCircle2 = circle(
            board,
            point(board, -2.0, 2.0, "identicalCenter2"),
            point(board, 0.0, 2.0, "identicalRadius2"),
            "identicalCircle2",
        )
        val identical = radicalAxis(
            RadicalAxis.create(
                board = board,
                circle1 = identicalCircle1,
                circle2 = identicalCircle2,
                id = "identical",
                point1Id = "identicalPoint1",
                point2Id = "identicalPoint2",
            ),
        )
        board.update()

        for (axis in listOf(concentric, duplicateParent, identical)) {
            assertArrayMatches(
                doubleArrayOf(0.0, 0.0, 0.0),
                axis.point1.coords.usrCoords,
            )
            assertArrayMatches(
                doubleArrayOf(0.0, 0.0, 0.0),
                axis.point2.coords.usrCoords,
            )
            assertTrue(axis.stdform.take(3).all(Double::isNaN))
        }
        assertEquals(listOf("circleA"), duplicateParent.parents)
    }

    @Test
    fun invalidParentsAndDuplicateIdsAreStructuredAndAtomic() {
        val board = board("errors")
        val circle1 = circle(
            board,
            point(board, -2.0, 0.0, "center1"),
            point(board, -1.0, 0.0, "radius1"),
            "circle1",
        )
        val circle2 = circle(
            board,
            point(board, 2.0, 0.0, "center2"),
            point(board, 4.0, 0.0, "radius2"),
            "circle2",
        )
        val otherBoard = board("other")
        val foreignCircle = circle(
            otherBoard,
            point(otherBoard, 0.0, 0.0, "foreignCenter"),
            point(otherBoard, 1.0, 0.0, "foreignRadius"),
            "foreignCircle",
        )
        assertEquals(
            RadicalAxisError.ParentBoardMismatch(1),
            assertIs<GMResult.Err<RadicalAxisError>>(
                RadicalAxis.create(
                    board = board,
                    circle1 = circle1,
                    circle2 = foreignCircle,
                ),
            ).error,
        )

        board.removeObject(circle2)
        assertEquals(
            RadicalAxisError.ParentNotRegistered(1, "circle2"),
            assertIs<GMResult.Err<RadicalAxisError>>(
                RadicalAxis.create(
                    board = board,
                    circle1 = circle1,
                    circle2 = circle2,
                ),
            ).error,
        )

        val replacementCircle = circle(
            board,
            point(board, 2.0, 0.0, "replacementCenter"),
            point(board, 4.0, 0.0, "replacementRadius"),
            "replacementCircle",
        )
        val taken = point(board, 8.0, 8.0, "taken")
        val beforeHelperFailure = board.objects.keys.toSet()
        val helperFailure = assertIs<
            GMResult.Err<RadicalAxisError.PointCreation>,
            >(
            RadicalAxis.create(
                board = board,
                circle1 = circle1,
                circle2 = replacementCircle,
                id = "unused",
                point1Id = "temporary",
                point2Id = taken.id,
            ),
        ).error
        assertEquals(1, helperFailure.pointIndex)
        assertEquals(
            PointError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            helperFailure.error,
        )
        assertEquals(beforeHelperFailure, board.objects.keys.toSet())

        val beforeLineFailure = board.objects.keys.toSet()
        val lineFailure = assertIs<
            GMResult.Err<RadicalAxisError.LineCreation>,
            >(
            RadicalAxis.create(
                board = board,
                circle1 = circle1,
                circle2 = replacementCircle,
                id = taken.id,
                point1Id = "temporary1",
                point2Id = "temporary2",
            ),
        ).error
        assertEquals(
            LineError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            lineFailure.error,
        )
        assertEquals(beforeLineFailure, board.objects.keys.toSet())
        assertSame(taken, board.elementById("taken"))
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
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
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

    private fun radicalAxis(
        result: GMResult<Line, RadicalAxisError>,
    ): Line = assertIs<GMResult.Ok<Line>>(result).value

    private fun assertHelper(
        point: Point,
        expected: DoubleArray,
    ) {
        assertEquals(Const.OBJECT_TYPE_CAS, point.type)
        assertTrue(point.isConstrained)
        assertFalse(point.isDraggable)
        assertFalse(point.isFixed)
        assertTrue(point.dump)
        assertTrue(point.parents.isEmpty())
        assertArrayMatches(expected, point.coords.usrCoords)
    }

    private fun assertArrayMatches(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            val expectedValue = expected[index]
            val actualValue = actual[index]
            when {
                expectedValue.isNaN() ->
                    assertTrue(actualValue.isNaN())
                expectedValue == Double.POSITIVE_INFINITY ->
                    assertEquals(Double.POSITIVE_INFINITY, actualValue)
                expectedValue == Double.NEGATIVE_INFINITY ->
                    assertEquals(Double.NEGATIVE_INFINITY, actualValue)
                else ->
                    assertTrue(
                        abs(expectedValue - actualValue) <= TOLERANCE,
                        "Expected $expectedValue at $index, got $actualValue",
                    )
            }
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
