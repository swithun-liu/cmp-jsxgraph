/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ArcCompositionTest {
    @Test
    fun semicircleAndCircumcircleArcPreserveOfficialCompositionMetadata() {
        val board = board("registered-compositions")
        val first = point(board, -4.0, -1.0, id = "first")
        val second = point(board, 1.0, 4.0, id = "second")
        val third = point(board, 5.0, -2.0, id = "third")

        val semicircle = semicircle(
            Arc.createSemicircle(
                board = board,
                point1 = first,
                point2 = second,
                id = "semicircle",
                name = "",
            ),
        )
        val circumcircleArc = arc(
            Arc.createCircumcircleArc(
                board = board,
                point1 = first,
                point2 = second,
                point3 = third,
                id = "circumcircleArc",
                name = "",
            ),
        )
        val midpoint = assertIs<MidpointPoint>(semicircle.center)
        val circumcenter = assertIs<CircumcenterPoint>(
            circumcircleArc.center,
        )

        assertEquals("semicircle", semicircle.elType)
        assertEquals(listOf("first", "second"), semicircle.parents)
        assertSame(midpoint, semicircle.midpoint)
        assertSame(midpoint, semicircle.subs["midpoint"])
        assertEquals(
            listOf<GeometryElement>(midpoint),
            semicircle.inherits,
        )
        assertFalse(midpoint.dump)
        assertSame(second, semicircle.point2)
        assertSame(first, semicircle.point3)
        assertEquals(-1.5, midpoint.X(), absoluteTolerance = 1.0e-12)
        assertEquals(1.5, midpoint.Y(), absoluteTolerance = 1.0e-12)
        assertEquals(PI, semicircle.Value("radians"), 1.0e-12)
        assertPoint(semicircle.points.first(), 1.0, 4.0)
        assertPoint(semicircle.points.last(), -4.0, -1.0)
        assertSame(midpoint, first.childElements[midpoint.id])
        assertSame(semicircle, first.childElements[semicircle.id])
        assertSame(semicircle, midpoint.childElements[semicircle.id])

        assertEquals("circumcirclearc", circumcircleArc.elType)
        assertEquals(
            listOf("first", "second", "third"),
            circumcircleArc.parents,
        )
        assertTrue(circumcircleArc.useDirection)
        assertSame(second, circumcircleArc.directionpoint)
        assertSame(first, circumcircleArc.point2)
        assertSame(third, circumcircleArc.point3)
        assertSame(third, circumcircleArc.radiuspoint)
        assertSame(first, circumcircleArc.anglepoint)
        assertSame(circumcenter, circumcircleArc.subs["center"])
        assertEquals(
            listOf<GeometryElement>(circumcenter),
            circumcircleArc.inherits,
        )
        assertFalse(circumcenter.dump)
        assertEquals(0.6, circumcenter.X(), absoluteTolerance = 1.0e-12)
        assertEquals(-0.6, circumcenter.Y(), absoluteTolerance = 1.0e-12)
        assertPoint(circumcircleArc.points.first(), 5.0, -2.0)
        assertPoint(circumcircleArc.points.last(), -4.0, -1.0)
        assertSame(
            circumcircleArc,
            first.childElements[circumcircleArc.id],
        )
        assertFalse(circumcircleArc.id in second.childElements)
        assertTrue(circumcircleArc.id in second.descendants)
        assertSame(
            circumcircleArc,
            third.childElements[circumcircleArc.id],
        )
        assertSame(
            circumcircleArc,
            circumcenter.childElements[circumcircleArc.id],
        )

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

        assertEquals(-3.0, midpoint.X(), absoluteTolerance = 1.0e-12)
        assertEquals(2.5, midpoint.Y(), absoluteTolerance = 1.0e-12)
        assertEquals(
            -2.108695652173913,
            circumcenter.X(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -2.847826086956522,
            circumcenter.Y(),
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun compositionOutputAndHelperRemovalFollowOfficialOwnership() {
        val outputBoard = board("output-removal")
        val outputFirst = point(outputBoard, -4.0, -1.0)
        val outputSecond = point(outputBoard, 1.0, 4.0)
        val output = semicircle(
            Arc.createSemicircle(
                board = outputBoard,
                point1 = outputFirst,
                point2 = outputSecond,
                name = "",
            ),
        )
        val outputHelper = assertIs<MidpointPoint>(output.center)

        outputBoard.removeObject(output)

        assertEquals(null, outputBoard.elementById(output.id))
        assertSame(outputHelper, outputBoard.elementById(outputHelper.id))
        assertSame(outputFirst, outputBoard.elementById(outputFirst.id))
        assertSame(outputSecond, outputBoard.elementById(outputSecond.id))

        val helperBoard = board("helper-removal")
        val helperFirst = point(helperBoard, -4.0, -1.0)
        val helperSecond = point(helperBoard, 1.0, 4.0)
        val helperThird = point(helperBoard, 5.0, -2.0)
        val helperSemicircle = semicircle(
            Arc.createSemicircle(
                board = helperBoard,
                point1 = helperFirst,
                point2 = helperSecond,
                name = "",
            ),
        )
        val helperCircumcircleArc = arc(
            Arc.createCircumcircleArc(
                board = helperBoard,
                point1 = helperFirst,
                point2 = helperSecond,
                point3 = helperThird,
                name = "",
            ),
        )
        val midpoint = assertIs<MidpointPoint>(helperSemicircle.center)
        val circumcenter = assertIs<CircumcenterPoint>(
            helperCircumcircleArc.center,
        )

        helperBoard.removeObject(midpoint)

        assertEquals(null, helperBoard.elementById(midpoint.id))
        assertEquals(null, helperBoard.elementById(helperSemicircle.id))
        assertSame(
            helperCircumcircleArc,
            helperBoard.elementById(helperCircumcircleArc.id),
        )
        assertSame(circumcenter, helperBoard.elementById(circumcenter.id))
        assertSame(helperFirst, helperBoard.elementById(helperFirst.id))
        assertSame(helperSecond, helperBoard.elementById(helperSecond.id))
        assertSame(helperThird, helperBoard.elementById(helperThird.id))

        helperBoard.removeObject(circumcenter)

        assertEquals(null, helperBoard.elementById(circumcenter.id))
        assertEquals(null, helperBoard.elementById(helperCircumcircleArc.id))
        assertSame(helperFirst, helperBoard.elementById(helperFirst.id))
        assertSame(helperSecond, helperBoard.elementById(helperSecond.id))
        assertSame(helperThird, helperBoard.elementById(helperThird.id))
    }

    @Test
    fun coordinateOwnedSourcesRemainWithHelperUntilHelperRemoval() {
        val semicircleBoard = board("owned-semicircle")
        val first = point(semicircleBoard, -4.0, -1.0)
        val second = point(semicircleBoard, 1.0, 4.0)
        val output = semicircle(
            Arc.createSemicircle(
                board = semicircleBoard,
                point1 = first,
                point2 = second,
                ownedPoints = setOf(first, second),
                name = "",
            ),
        )
        val midpoint = assertIs<MidpointPoint>(output.center)

        assertEquals(setOf(first, second), midpoint.ownedPoints)
        assertSame(first, midpoint.childElements[first.id])
        assertSame(second, midpoint.childElements[second.id])
        assertSame(output, first.childElements[output.id])
        assertSame(output, second.childElements[output.id])
        assertEquals(4, semicircleBoard.objects.size)

        semicircleBoard.removeObject(output)

        assertEquals(3, semicircleBoard.objects.size)
        assertSame(midpoint, semicircleBoard.elementById(midpoint.id))
        assertSame(first, semicircleBoard.elementById(first.id))
        assertSame(second, semicircleBoard.elementById(second.id))

        semicircleBoard.removeObject(midpoint)

        assertTrue(semicircleBoard.objects.isEmpty())
        assertTrue(semicircleBoard.objectsList.isEmpty())

        val circumcircleBoard = board("owned-circumcircle-arc")
        val a = point(circumcircleBoard, -4.0, -1.0)
        val b = point(circumcircleBoard, 1.0, 4.0)
        val c = point(circumcircleBoard, 5.0, -2.0)
        val circumcircleArc = arc(
            Arc.createCircumcircleArc(
                board = circumcircleBoard,
                point1 = a,
                point2 = b,
                point3 = c,
                ownedPoints = setOf(a, b, c),
                name = "",
            ),
        )
        val circumcenter = assertIs<CircumcenterPoint>(
            circumcircleArc.center,
        )

        assertEquals(setOf(a, b, c), circumcenter.ownedPoints)
        assertSame(a, circumcenter.childElements[a.id])
        assertSame(b, circumcenter.childElements[b.id])
        assertSame(c, circumcenter.childElements[c.id])
        assertFalse(circumcircleArc.id in b.childElements)
        assertEquals(5, circumcircleBoard.objects.size)

        circumcircleBoard.removeObject(circumcircleArc)

        assertEquals(4, circumcircleBoard.objects.size)
        assertSame(circumcenter, circumcircleBoard.elementById(circumcenter.id))

        circumcircleBoard.removeObject(circumcenter)

        assertTrue(circumcircleBoard.objects.isEmpty())
        assertTrue(circumcircleBoard.objectsList.isEmpty())
    }

    @Test
    fun minorAndMajorFactoriesForceSelectionWhileKeepingArcType() {
        val board = board("minor-major")
        val center = point(board, 0.0, 0.0)
        val radiuspoint = point(board, 2.0, 0.0)
        val anglepoint = point(board, 0.0, 2.0)

        val minor = arc(
            Arc.createMinorArc(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                name = "",
            ),
        )
        val major = arc(
            Arc.createMajorArc(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                name = "",
            ),
        )

        assertEquals("arc", minor.elType)
        assertEquals(Arc.SELECTION_MINOR, minor.selection)
        assertEquals(13, minor.numberPoints)
        assertEquals("arc", major.elType)
        assertEquals(Arc.SELECTION_MAJOR, major.selection)
        assertEquals(13, major.numberPoints)
        assertPoint(minor.points.first(), 2.0, 0.0)
        assertPoint(major.points.first(), 2.0, 0.0)
        assertTrue(minor.points[6].usrCoords[2] > 0.0)
        assertTrue(major.points[6].usrCoords[2] < 0.0)
    }

    @Test
    fun compositionValidationIsAtomicAndRejectsForeignOwnership() {
        val board = board("composition-validation")
        val first = point(board, -4.0, -1.0)
        val second = point(board, 1.0, 4.0)
        val third = point(board, 5.0, -2.0)
        val foreign = point(board, 8.0, 8.0, id = "taken")
        val originalIds = board.objects.keys.toSet()

        assertEquals(
            ArcError.Registration(BoardError.DuplicateElementId("taken")),
            assertIs<GMResult.Err<ArcError>>(
                Arc.createSemicircle(
                    board = board,
                    point1 = first,
                    point2 = second,
                    id = "taken",
                ),
            ).error,
        )
        assertEquals(originalIds, board.objects.keys.toSet())

        assertEquals(
            ArcError.Registration(BoardError.DuplicateElementId("taken")),
            assertIs<GMResult.Err<ArcError>>(
                Arc.createCircumcircleArc(
                    board = board,
                    point1 = first,
                    point2 = second,
                    point3 = third,
                    id = "taken",
                ),
            ).error,
        )
        assertEquals(originalIds, board.objects.keys.toSet())

        assertEquals(
            ArcError.OwnedPointNotParent(foreign.id),
            assertIs<GMResult.Err<ArcError>>(
                Arc.createSemicircle(
                    board = board,
                    point1 = first,
                    point2 = second,
                    ownedPoints = setOf(foreign),
                ),
            ).error,
        )
        assertEquals(
            CircumcenterError.OwnedPointNotParent(foreign.id),
            assertIs<GMResult.Err<CircumcenterError>>(
                CircumcenterPoint.create(
                    board = board,
                    point1 = first,
                    point2 = second,
                    point3 = third,
                    ownedPoints = setOf(foreign),
                ),
            ).error,
        )
        assertEquals(originalIds, board.objects.keys.toSet())
    }

    @Test
    fun degenerateCompositionsPreserveOfficialNanAndInfinityResults() {
        val semicircleBoard = board("coincident-semicircle")
        val semicircleFirst = point(semicircleBoard, 1.0, 2.0)
        val semicircleSecond = point(semicircleBoard, 1.0, 2.0)
        val semicircle = semicircle(
            Arc.createSemicircle(
                board = semicircleBoard,
                point1 = semicircleFirst,
                point2 = semicircleSecond,
                name = "",
            ),
        )
        assertEquals(0.0, semicircle.Radius())
        assertEquals(0.0, semicircle.Value("radians"))
        assertEquals(1, semicircle.numberPoints)
        assertPoint(semicircle.points.single(), 1.0, 2.0)

        val collinearBoard = board("collinear-circumcircle-arc")
        val collinear = arc(
            Arc.createCircumcircleArc(
                board = collinearBoard,
                point1 = point(collinearBoard, -3.0, 0.0),
                point2 = point(collinearBoard, 0.0, 0.0),
                point3 = point(collinearBoard, 4.0, 0.0),
                name = "",
            ),
        )
        assertTrue(collinear.Radius().isInfinite())
        assertEquals(13, collinear.numberPoints)
        assertPoint(collinear.points.first(), 4.0, 0.0)
        assertTrue(
            collinear.points.drop(1).all { coordinates ->
                coordinates.usrCoords[1].isNaN() &&
                    coordinates.usrCoords[2].isNaN()
            },
        )

        val coincidentBoard = board("coincident-minor-major")
        val center = point(coincidentBoard, 1.0, 2.0)
        val radiuspoint = point(coincidentBoard, 1.0, 2.0)
        val anglepoint = point(coincidentBoard, 1.0, 2.0)
        val minor = arc(
            Arc.createMinorArc(
                board = coincidentBoard,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                name = "",
            ),
        )
        val major = arc(
            Arc.createMajorArc(
                board = coincidentBoard,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                name = "",
            ),
        )
        assertEquals(1, minor.numberPoints)
        assertPoint(minor.points.single(), 1.0, 2.0)
        assertEquals(13, major.numberPoints)
        assertTrue(
            major.points.withIndex().all { (index, coordinates) ->
                if (index % 3 == 0) {
                    coordinates.usrCoords[1] == 1.0 &&
                        coordinates.usrCoords[2] == 2.0
                } else {
                    coordinates.usrCoords[1].isNaN() &&
                        coordinates.usrCoords[2].isNaN()
                }
            },
        )
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
        id: String = "",
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = "",
            ),
        ).value

    private fun arc(result: GMResult<Arc, ArcError>): Arc =
        assertIs<GMResult.Ok<Arc>>(result).value

    private fun semicircle(result: GMResult<Arc, ArcError>): Arc =
        arc(result)

    private fun assertPoint(
        point: Coords,
        x: Double,
        y: Double,
    ) {
        assertEquals(x, point.usrCoords[1], absoluteTolerance = 1.0e-10)
        assertEquals(y, point.usrCoords[2], absoluteTolerance = 1.0e-10)
    }
}
