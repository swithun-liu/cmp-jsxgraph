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
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CircumcircleCompositionTest {
    @Test
    fun circumcenterAttributesAndCircumcircleMetadataMatchOfficialFactory() {
        val board = board("registered")
        val first = point(board, -4.0, -1.0, id = "a")
        val second = point(board, 1.0, 4.0, id = "b")
        val third = point(board, 5.0, -2.0, id = "c")
        val frozenCenter = circumcenter(
            CircumcenterPoint.create(
                board = board,
                point1 = first,
                point2 = second,
                point3 = third,
                id = "center",
                name = "center-name",
                needsRegularUpdate = false,
                fixed = true,
            ),
        )
        val output = circle(
            Circle.createCircumcircle(
                board = board,
                point1 = first,
                point2 = second,
                point3 = third,
                id = "circumcircle",
                name = "",
            ),
        )
        val helper = assertIs<CircumcenterPoint>(output.center)

        assertEquals("center", frozenCenter.id)
        assertEquals("center-name", frozenCenter.name)
        assertEquals("circumcenter", frozenCenter.elType)
        assertEquals(Const.OBJECT_TYPE_CAS, frozenCenter.type)
        assertFalse(frozenCenter.isDraggable)
        assertTrue(frozenCenter.isFixed)
        assertFalse(frozenCenter.needsRegularUpdate)
        assertTrue(frozenCenter.dump)
        assertEquals(listOf("a", "b", "c"), frozenCenter.parents)

        assertEquals("circumcircle", output.elType)
        assertEquals("twoPoints", output.method)
        assertEquals(listOf("a", "b", "c"), output.parents)
        assertSame(helper, output.midpoint)
        assertSame(first, output.point2)
        assertSame(helper, output.subs["center"])
        assertEquals(
            listOf<GeometryElement>(helper, first, output),
            output.inherits,
        )
        assertFalse(helper.dump)
        assertEquals(0.6, helper.X(), absoluteTolerance = TOLERANCE)
        assertEquals(-0.6, helper.Y(), absoluteTolerance = TOLERANCE)
        assertEquals(
            4.617358552246078,
            output.Radius(),
            absoluteTolerance = TOLERANCE,
        )
        assertSame(output, helper.childElements[output.id])
        for (source in listOf(first, second, third)) {
            assertSame(frozenCenter, source.childElements[frozenCenter.id])
            assertSame(helper, source.childElements[helper.id])
            assertSame(output, source.childElements[output.id])
        }

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-6.0, 2.0),
        )
        second.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 3.0),
        )
        third.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, -4.0),
        )
        board.update()

        assertEquals(0.6, frozenCenter.X(), absoluteTolerance = TOLERANCE)
        assertEquals(-0.6, frozenCenter.Y(), absoluteTolerance = TOLERANCE)
        assertEquals(
            -2.108695652173913,
            helper.X(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            -2.847826086956522,
            helper.Y(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            6.2164030835191495,
            output.Radius(),
            absoluteTolerance = TOLERANCE,
        )
    }

    @Test
    fun genericThreePointCircleRestoresOuterFactoryTypeAndInheritance() {
        val board = board("generic")
        val first = point(board, -4.0, -1.0, id = "a")
        val second = point(board, 1.0, 4.0, id = "b")
        val third = point(board, 5.0, -2.0, id = "c")

        val output = circle(
            Circle.create(
                board = board,
                point1 = first,
                point2 = second,
                point3 = third,
                id = "circle",
                name = "",
            ),
        )
        val helper = assertIs<CircumcenterPoint>(output.center)

        assertEquals("circle", output.elType)
        assertSame(helper, output.subs["center"])
        assertEquals(
            listOf<GeometryElement>(
                helper,
                first,
                output,
                first,
                second,
                third,
            ),
            output.inherits,
        )
    }

    @Test
    fun coordinateParentsAreOwnedByCenterAndRemovedInOfficialOrder() {
        val board = board("owned")
        val first = point(board, -4.0, -1.0)
        val second = point(board, 1.0, 4.0)
        val third = point(board, 5.0, -2.0)
        val output = circle(
            Circle.createCircumcircle(
                board = board,
                point1 = first,
                point2 = second,
                point3 = third,
                ownedPoints = setOf(first, second, third),
                name = "",
            ),
        )
        val helper = assertIs<CircumcenterPoint>(output.center)

        assertEquals(setOf(first, second, third), helper.ownedPoints)
        assertSame(first, helper.childElements[first.id])
        assertSame(second, helper.childElements[second.id])
        assertSame(third, helper.childElements[third.id])
        assertSame(output, helper.childElements[output.id])
        assertEquals(5, board.objects.size)

        board.removeObject(output)

        assertEquals(4, board.objects.size)
        assertSame(helper, board.elementById(helper.id))
        assertSame(first, board.elementById(first.id))
        assertSame(second, board.elementById(second.id))
        assertSame(third, board.elementById(third.id))

        board.removeObject(helper)

        assertTrue(board.objects.isEmpty())
        assertTrue(board.objectsList.isEmpty())
    }

    @Test
    fun failuresRollBackAndDegenerateValuesFollowOfficialArithmetic() {
        val board = board("failures")
        val first = point(board, -4.0, -1.0)
        val second = point(board, 1.0, 4.0)
        val third = point(board, 5.0, -2.0)
        val extra = point(board, 0.0, 0.0)

        assertEquals(
            CircumcenterError.OwnedPointNotParent(extra.id),
            assertIs<GMResult.Err<CircumcenterError>>(
                CircumcenterPoint.create(
                    board = board,
                    point1 = first,
                    point2 = second,
                    point3 = third,
                    ownedPoints = setOf(extra),
                ),
            ).error,
        )

        val duplicate = point(board, 2.0, 2.0, id = "duplicate")
        val objectIds = board.objects.keys.toSet()
        assertEquals(
            BoardError.DuplicateElementId("duplicate"),
            assertIs<GMResult.Err<CircleError.Registration>>(
                Circle.createCircumcircle(
                    board = board,
                    point1 = first,
                    point2 = second,
                    point3 = third,
                    id = duplicate.id,
                ),
            ).error.error,
        )
        assertEquals(objectIds, board.objects.keys)

        val collinearBoard = board("collinear")
        val collinear = circle(
            Circle.createCircumcircle(
                board = collinearBoard,
                point1 = point(collinearBoard, -3.0, 0.0),
                point2 = point(collinearBoard, 0.0, 0.0),
                point3 = point(collinearBoard, 4.0, 0.0),
                name = "",
            ),
        )
        assertEquals(0.0, collinear.center.Z())
        assertEquals(0.0, collinear.center.X())
        assertEquals(42.0, collinear.center.Y())
        assertTrue(collinear.Radius().isInfinite())

        val coincidentBoard = board("coincident")
        val coincident = circle(
            Circle.createCircumcircle(
                board = coincidentBoard,
                point1 = point(coincidentBoard, 1.0, 2.0),
                point2 = point(coincidentBoard, 1.0, 2.0),
                point3 = point(coincidentBoard, 1.0, 2.0),
                name = "",
            ),
        )
        assertTrue(coincident.Radius().isNaN())
        assertTrue(coincident.stdform.take(3).all(Double::isNaN))
    }

    private fun board(id: String): Board = Board(
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
        id: String = "",
    ): Point = assertIs<GMResult.Ok<Point>>(
        Point.create(
            board = board,
            coordinates = doubleArrayOf(x, y),
            id = id,
            name = "",
        ),
    ).value

    private fun circumcenter(
        result: GMResult<CircumcenterPoint, CircumcenterError>,
    ): CircumcenterPoint =
        assertIs<GMResult.Ok<CircumcenterPoint>>(result).value

    private fun circle(
        result: GMResult<Circle, CircleError>,
    ): Circle = assertIs<GMResult.Ok<Circle>>(result).value

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
