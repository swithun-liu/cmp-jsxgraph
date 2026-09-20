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

class SectorCompositionTest {
    @Test
    fun directionPointSectorSelectsEndpointsAndTracksFourthParent() {
        val board = board("direction-sector")
        val center = point(board, 0.0, 0.0, id = "center")
        val first = point(board, 2.0, 0.0, id = "first")
        val third = point(board, 0.0, 2.0, id = "third")
        val direction = point(board, 0.0, -2.0, id = "direction")

        val sector = sector(
            Sector.create(
                board = board,
                center = center,
                radiuspoint = first,
                anglepoint = third,
                directionpoint = direction,
                useDirection = true,
                id = "sector",
                name = "",
            ),
        )

        assertTrue(sector.useDirection)
        assertSame(direction, sector.directionpoint)
        assertEquals(
            listOf("center", "first", "third", "direction"),
            sector.parents,
        )
        assertSame(sector, direction.childElements[sector.id])
        assertPoint(sector.points[3], 0.0, 2.0)
        assertPoint(sector.points[15], 2.0, 0.0)

        direction.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 3.0),
        )
        board.update()

        assertPoint(sector.points[3], 2.0, 0.0)
        assertPoint(sector.points[15], 0.0, 2.0)
    }

    @Test
    fun circumcircleSectorPreservesOfficialCompositionMetadataAndUpdates() {
        val board = board("circumcircle-sector")
        val first = point(board, -4.0, -1.0, id = "first")
        val second = point(board, 1.0, 4.0, id = "second")
        val third = point(board, 5.0, -2.0, id = "third")

        val output = sector(
            Sector.createCircumcircleSector(
                board = board,
                point1 = first,
                point2 = second,
                point3 = third,
                id = "output",
                name = "",
            ),
        )
        val center = assertIs<CircumcenterPoint>(output.center)

        assertEquals("circumcirclesector", output.elType)
        assertEquals(Const.OBJECT_TYPE_SECTOR, output.type)
        assertEquals(listOf("first", "second", "third"), output.parents)
        assertTrue(output.useDirection)
        assertSame(first, output.point2)
        assertSame(third, output.point3)
        assertSame(second, output.point4)
        assertSame(center, output.subs["center"])
        assertTrue(output.inherits.isEmpty())
        assertFalse(center.dump)
        assertEquals(0.6, center.X(), absoluteTolerance = 1.0e-12)
        assertEquals(-0.6, center.Y(), absoluteTolerance = 1.0e-12)
        assertPoint(output.points[3], 5.0, -2.0)
        assertPoint(output.points[15], -4.0, -1.0)
        assertEquals(
            2.746801533890032,
            output.Value("radians"),
            absoluteTolerance = 1.0e-12,
        )
        assertSame(output, center.childElements[output.id])
        assertSame(output, first.childElements[output.id])
        assertSame(output, second.childElements[output.id])
        assertSame(output, third.childElements[output.id])

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

        assertEquals(
            -2.108695652173913,
            center.X(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -2.847826086956522,
            center.Y(),
            absoluteTolerance = 1.0e-12,
        )
        assertPoint(output.points[3], 4.0, -4.0)
        assertPoint(output.points[15], -6.0, 2.0)
    }

    @Test
    fun minorAndMajorSectorFactoriesForceSelectionAndKeepSectorType() {
        val board = board("minor-major-sector")
        val center = point(board, 0.0, 0.0)
        val first = point(board, 2.0, 0.0)
        val third = point(board, 0.0, 2.0)

        val minor = sector(
            Sector.createMinorSector(
                board = board,
                center = center,
                radiuspoint = first,
                anglepoint = third,
                name = "",
            ),
        )
        val major = sector(
            Sector.createMajorSector(
                board = board,
                center = center,
                radiuspoint = first,
                anglepoint = third,
                name = "",
            ),
        )

        assertEquals("sector", minor.elType)
        assertEquals(Arc.SELECTION_MINOR, minor.selection)
        assertEquals("sector", major.elType)
        assertEquals(Arc.SELECTION_MAJOR, major.selection)
        assertTrue(minor.points[6].usrCoords[2] > 0.0)
        assertTrue(major.points[6].usrCoords[2] < 0.0)
        assertEquals(0.5 * PI, minor.Value("radians"), 1.0e-12)
        assertEquals(0.5 * PI, major.Value("radians"), 1.0e-12)
    }

    @Test
    fun nonreflexAndReflexAngleFactoriesUseForcedValueSemantics() {
        val board = board("angle-compositions")
        val first = point(board, -4.0, -1.0)
        val vertex = point(board, 1.0, 4.0)
        val third = point(board, 5.0, -2.0)

        val nonreflex = sector(
            Sector.createNonreflexAngle(
                board = board,
                first = first,
                vertex = vertex,
                third = third,
                radius = AngleRadius.Fixed(2.0),
                orientation = Arc.ORIENTATION_CLOCKWISE,
                name = "",
            ),
        )
        val reflex = sector(
            Sector.createReflexAngle(
                board = board,
                first = first,
                vertex = vertex,
                third = third,
                radius = AngleRadius.Fixed(2.0),
                orientation = Arc.ORIENTATION_CLOCKWISE,
                name = "",
            ),
        )

        assertEquals("angle", nonreflex.elType)
        assertEquals(Arc.SELECTION_MINOR, nonreflex.selection)
        assertEquals(
            1.3734007669450157,
            nonreflex.Value(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            2.746801533890031,
            nonreflex.Value("length"),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            78.69006752597979,
            nonreflex.Value("degrees"),
            absoluteTolerance = 1.0e-12,
        )

        assertEquals("angle", reflex.elType)
        assertEquals(Arc.SELECTION_MAJOR, reflex.selection)
        assertEquals(
            4.90978454023457,
            reflex.Value(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            9.819569080469138,
            reflex.Value("length"),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            281.30993247402023,
            reflex.Value("degrees"),
            absoluteTolerance = 1.0e-12,
        )
        assertTrue(reflex.Value("turn").isNaN())
    }

    @Test
    fun ownedDirectionAndCircumcenterRemovalFollowOfficialLifecycle() {
        val directionBoard = board("owned-direction-sector")
        val center = point(directionBoard, 0.0, 0.0)
        val first = point(directionBoard, 2.0, 0.0)
        val third = point(directionBoard, 0.0, 2.0)
        val direction = point(directionBoard, 0.0, -2.0)
        val directionSector = sector(
            Sector.create(
                board = directionBoard,
                center = center,
                radiuspoint = first,
                anglepoint = third,
                directionpoint = direction,
                useDirection = true,
                ownedPoints = setOf(center, first, third, direction),
                name = "",
            ),
        )

        directionBoard.removeObject(directionSector)

        assertTrue(directionBoard.objects.keys == setOf(direction.id))
        assertSame(direction, directionBoard.elementById(direction.id))

        val circumcircleBoard = board("owned-circumcircle-sector")
        val a = point(circumcircleBoard, -4.0, -1.0)
        val b = point(circumcircleBoard, 1.0, 4.0)
        val c = point(circumcircleBoard, 5.0, -2.0)
        val circumcircleSector = sector(
            Sector.createCircumcircleSector(
                board = circumcircleBoard,
                point1 = a,
                point2 = b,
                point3 = c,
                ownedPoints = setOf(a, b, c),
                name = "",
            ),
        )
        val circumcenter = assertIs<CircumcenterPoint>(
            circumcircleSector.center,
        )

        circumcircleBoard.removeObject(circumcircleSector)

        assertEquals(4, circumcircleBoard.objects.size)
        assertSame(circumcenter, circumcircleBoard.elementById(circumcenter.id))
        assertSame(a, circumcircleBoard.elementById(a.id))
        assertSame(b, circumcircleBoard.elementById(b.id))
        assertSame(c, circumcircleBoard.elementById(c.id))

        circumcircleBoard.removeObject(circumcenter)

        assertTrue(circumcircleBoard.objects.isEmpty())
    }

    @Test
    fun compositionValidationIsAtomicAndDegeneracyMatchesOfficialArithmetic() {
        val board = board("sector-validation")
        val first = point(board, -4.0, -1.0)
        val second = point(board, 1.0, 4.0)
        val third = point(board, 5.0, -2.0)
        val foreign = point(board, 8.0, 8.0, id = "taken")
        val originalIds = board.objects.keys.toSet()

        assertEquals(
            SectorError.InvalidDirectionPoint(
                useDirection = true,
                hasDirectionPoint = false,
            ),
            assertIs<GMResult.Err<SectorError>>(
                Sector.create(
                    board = board,
                    center = first,
                    radiuspoint = second,
                    anglepoint = third,
                    useDirection = true,
                ),
            ).error,
        )
        assertEquals(
            SectorError.Registration(BoardError.DuplicateElementId("taken")),
            assertIs<GMResult.Err<SectorError>>(
                Sector.createCircumcircleSector(
                    board = board,
                    point1 = first,
                    point2 = second,
                    point3 = third,
                    id = "taken",
                ),
            ).error,
        )
        assertEquals(
            SectorError.OwnedPointNotParent(foreign.id),
            assertIs<GMResult.Err<SectorError>>(
                Sector.createCircumcircleSector(
                    board = board,
                    point1 = first,
                    point2 = second,
                    point3 = third,
                    ownedPoints = setOf(foreign),
                ),
            ).error,
        )
        assertEquals(originalIds, board.objects.keys.toSet())

        val collinearBoard = board("collinear-circumcircle-sector")
        val collinear = sector(
            Sector.createCircumcircleSector(
                board = collinearBoard,
                point1 = point(collinearBoard, -3.0, 0.0),
                point2 = point(collinearBoard, 0.0, 0.0),
                point3 = point(collinearBoard, 4.0, 0.0),
                name = "",
            ),
        )
        assertTrue(collinear.Radius().isNaN())
        assertTrue(collinear.Value().isNaN())
        assertEquals(1, collinear.numberPoints)
        assertTrue(collinear.points.single().usrCoords[1].isNaN())
        assertTrue(collinear.points.single().usrCoords[2].isNaN())

        val coincidentBoard = board("coincident-reflex-angle")
        val coincidentFirst = point(coincidentBoard, 1.0, 2.0)
        val coincidentVertex = point(coincidentBoard, 1.0, 2.0)
        val coincidentThird = point(coincidentBoard, 1.0, 2.0)
        val reflex = sector(
            Sector.createReflexAngle(
                board = coincidentBoard,
                first = coincidentFirst,
                vertex = coincidentVertex,
                third = coincidentThird,
                radius = AngleRadius.Fixed(2.0),
                name = "",
            ),
        )
        assertEquals(2.0 * PI, reflex.Value(), 1.0e-12)
        assertEquals(0.0, reflex.Value("length"), 1.0e-12)
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

    private fun sector(result: GMResult<Sector, SectorError>): Sector =
        assertIs<GMResult.Ok<Sector>>(result).value

    private fun assertPoint(
        point: Coords,
        x: Double,
        y: Double,
    ) {
        assertEquals(x, point.usrCoords[1], absoluteTolerance = 1.0e-10)
        assertEquals(y, point.usrCoords[2], absoluteTolerance = 1.0e-10)
    }
}
