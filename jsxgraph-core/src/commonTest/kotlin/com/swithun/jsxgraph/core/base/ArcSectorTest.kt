/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ArcSectorTest {
    @Test
    fun arcUsesUpstreamCubicBezierGeometryAndTracksParents() {
        val board = board()
        val center = point(board, 0.0, 0.0)
        val radiuspoint = point(board, 2.0, 0.0)
        val anglepoint = point(board, 0.0, 3.0)

        val arc = assertIs<GMResult.Ok<Arc>>(
            Arc.create(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                name = "",
            ),
        ).value

        assertEquals(Const.OBJECT_TYPE_ARC, arc.type)
        assertEquals(Const.OBJECT_CLASS_CURVE, arc.elementClass)
        assertEquals("arc", arc.elType)
        assertEquals(3, arc.bezierDegree)
        assertEquals(13, arc.numberPoints)
        assertPoint(arc.points.first(), 2.0, 0.0)
        assertPoint(arc.points.last(), 0.0, 2.0)
        assertEquals(2.0, arc.Radius())
        assertEquals(PI, arc.Value("length"), absoluteTolerance = 1.0e-12)
        assertSame(arc, center.childElements[arc.id])
        assertSame(arc, radiuspoint.childElements[arc.id])
        assertSame(arc, anglepoint.childElements[arc.id])

        anglepoint.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(-2.0, 0.0),
        )
        board.update()

        assertPoint(arc.points.last(), -2.0, 0.0)
    }

    @Test
    fun directionPointArcSelectsItsPathWithoutLinkingFourthParent() {
        val board = board()
        val center = point(board, 0.0, 0.0)
        val first = point(board, 2.0, 0.0)
        val third = point(board, 0.0, 2.0)
        val direction = point(board, 0.0, -2.0)

        val arc = assertIs<GMResult.Ok<Arc>>(
            Arc.create(
                board = board,
                center = center,
                radiuspoint = first,
                anglepoint = third,
                directionpoint = direction,
                useDirection = true,
                name = "",
            ),
        ).value

        assertTrue(arc.useDirection)
        assertSame(direction, arc.directionpoint)
        assertSame(third, arc.radiuspoint)
        assertSame(first, arc.anglepoint)
        assertPoint(arc.points.first(), 0.0, 2.0)
        assertPoint(arc.points.last(), 2.0, 0.0)
        assertEquals(1.5 * PI, arc.Value("radians"), 1.0e-12)
        assertEquals(
            listOf(center.id, first.id, third.id, direction.id),
            arc.parents,
        )
        assertTrue(arc.id !in direction.childElements)
        assertTrue(arc.id !in direction.descendants)

        direction.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 3.0),
        )
        board.update()

        assertSame(first, arc.radiuspoint)
        assertSame(third, arc.anglepoint)
        assertPoint(arc.points.first(), 0.0, 2.0)
        assertPoint(arc.points.last(), 2.0, 0.0)
        assertEquals(0.5 * PI, arc.Value("radians"), 1.0e-12)

        board.update()

        assertPoint(arc.points.first(), 2.0, 0.0)
        assertPoint(arc.points.last(), 0.0, 2.0)
        assertEquals(0.5 * PI, arc.Value("radians"), 1.0e-12)
    }

    @Test
    fun sectorIncludesCubicRadialLegsAndSupportsClockwiseSelection() {
        val board = board()
        val center = point(board, 0.0, 0.0)
        val radiuspoint = point(board, 2.0, 0.0)
        val anglepoint = point(board, 0.0, 2.0)

        val sector = assertIs<GMResult.Ok<Sector>>(
            Sector.create(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                orientation = Arc.ORIENTATION_CLOCKWISE,
                name = "",
            ),
        ).value

        assertEquals(Const.OBJECT_TYPE_SECTOR, sector.type)
        assertEquals("sector", sector.elType)
        assertEquals(19, sector.numberPoints)
        assertPoint(sector.points.first(), 0.0, 0.0)
        assertPoint(sector.points[3], 2.0, 0.0)
        assertTrue(sector.points[6].usrCoords[2] < 0.0)
        assertPoint(sector.points.last(), 0.0, 0.0)
    }

    @Test
    fun angleReordersParentsAndAppliesFixedAndAutoRadius() {
        val board = board(unitX = 10.0)
        val first = point(board, 6.0, 0.0)
        val vertex = point(board, 0.0, 0.0)
        val third = point(board, 0.0, 4.0)

        val fixed = assertIs<GMResult.Ok<Sector>>(
            Sector.createAngle(
                board = board,
                first = first,
                vertex = vertex,
                third = third,
                radius = AngleRadius.Fixed(2.0),
            ),
        ).value

        assertEquals(Const.OBJECT_TYPE_ANGLE, fixed.type)
        assertEquals("angle", fixed.elType)
        assertSame(vertex, fixed.center)
        assertSame(first, fixed.radiuspoint)
        assertEquals(listOf(first.id, vertex.id, third.id), fixed.parents)
        assertEquals(2.0, fixed.Radius())
        assertPoint(fixed.points[3], 2.0, 0.0)
        assertPoint(fixed.points[15], 0.0, 2.0)
        assertTrue(fixed.name.isNotEmpty())

        val auto = assertIs<GMResult.Ok<Sector>>(
            Sector.createAngle(
                board = board,
                first = first,
                vertex = vertex,
                third = third,
                radius = AngleRadius.Auto,
                name = "",
            ),
        ).value
        assertEquals(2.0, auto.Radius(), absoluteTolerance = 1.0e-12)
    }

    @Test
    fun invalidAttributesAndParentsFailWithoutRegisteringArc() {
        val board = board()
        val center = point(board, 0.0, 0.0)
        val radiuspoint = point(board, 2.0, 0.0)
        val anglepoint = point(board, 0.0, 2.0)
        val objectCount = board.numObjects

        assertIs<GMResult.Err<ArcError.InvalidSelection>>(
            Arc.create(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                selection = "nearest",
            ),
        )
        assertEquals(
            ArcError.InvalidDirectionPoint(
                useDirection = true,
                hasDirectionPoint = false,
            ),
            assertIs<GMResult.Err<ArcError>>(
                Arc.create(
                    board = board,
                    center = center,
                    radiuspoint = radiuspoint,
                    anglepoint = anglepoint,
                    useDirection = true,
                ),
            ).error,
        )
        assertEquals(
            ArcError.InvalidDirectionPoint(
                useDirection = false,
                hasDirectionPoint = true,
            ),
            assertIs<GMResult.Err<ArcError>>(
                Arc.create(
                    board = board,
                    center = center,
                    radiuspoint = radiuspoint,
                    anglepoint = anglepoint,
                    directionpoint = center,
                ),
            ).error,
        )
        assertIs<GMResult.Err<SectorError.InvalidOrientation>>(
            Sector.create(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                orientation = "left",
            ),
        )
        assertEquals(objectCount, board.numObjects)
    }

    private fun board(unitX: Double = 1.0): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = unitX,
            unitY = unitX,
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

    private fun assertPoint(
        point: Coords,
        x: Double,
        y: Double,
    ) {
        assertEquals(x, point.usrCoords[1], absoluteTolerance = 1.0e-10)
        assertEquals(y, point.usrCoords[2], absoluteTolerance = 1.0e-10)
    }
}
