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

class RegularPolygonTest {
    @Test
    fun numericVertexCountCreatesOfficialRotationChainAndUpdates() {
        val board = board()
        val first = point(board, -3.0, -1.0, "first")
        val second = point(board, 0.0, 2.0, "second")
        val polygon = regularPolygon(
            RegularPolygon.create(
                board = board,
                point1 = first,
                point2 = second,
                numberOfVertices = 5.0,
                id = "regular",
                name = "",
                vertexIds = listOf("third", "fourth", "fifth"),
                vertexName = "",
            ),
        )
        board.update()

        assertEquals("regularpolygon", polygon.elType)
        assertEquals(Const.OBJECT_TYPE_POLYGON, polygon.type)
        assertEquals(Const.OBJECT_CLASS_AREA, polygon.elementClass)
        assertTrue(polygon.isDraggable)
        assertEquals(
            listOf("first", "second", "third", "fourth", "fifth", "first"),
            polygon.vertices.map(Point::id),
        )
        assertEquals(
            listOf(
                "first" to "second",
                "second" to "third",
                "third" to "fourth",
                "fourth" to "fifth",
                "fifth" to "first",
            ),
            polygon.borders.map { border ->
                border.point1.id to border.point2.id
            },
        )
        assertEquals(
            listOf("third", "fourth", "fifth"),
            polygon.implicitVertices.map(Point::id),
        )
        assertEquals(
            listOf(
                "first",
                "second",
                "third",
                "fourth",
                "fifth",
                polygon.borders[1].id,
                polygon.borders[2].id,
                polygon.borders[3].id,
                polygon.borders[4].id,
                polygon.borders[0].id,
                "regular",
            ),
            board.objectsList.map(GeometryElement::id),
        )
        assertCoordinates(
            doubleArrayOf(1.0, -1.926118565760618, 5.780220532010303),
            polygon.vertices[2].coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(1.0, -6.116525305762879, 5.116525305762881),
            polygon.vertices[3].coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(1.0, -6.7802205320103015, 0.9261185657606195),
            polygon.vertices[4].coords.usrCoords,
        )
        for ((index, helper) in polygon.vertices.drop(2).dropLast(1).withIndex()) {
            assertEquals(Const.OBJECT_TYPE_CAS, helper.type)
            assertEquals("point", helper.elType)
            assertTrue(helper.isDraggable)
            assertFalse(helper.isFixed)
            assertEquals(1, helper.transformations.size)
            assertSame(polygon.vertices[index], helper.baseElement)
        }
        assertEquals(30.968593210601398, polygon.Area(), 1.0e-12)
        assertEquals(21.213203435596423, polygon.Perimeter(), 1.0e-12)

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-4.0, 1.0),
        )
        second.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, 3.0),
        )
        board.update()

        assertCoordinates(
            doubleArrayOf(1.0, 0.6429719392844307, 8.373316570225661),
            polygon.vertices[2].coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(1.0, -4.5776835371752504, 9.694208842938133),
            polygon.vertices[3].coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(1.0, -7.447198004465042, 5.137248592725874),
            polygon.vertices[4].coords.usrCoords,
        )
    }

    @Test
    fun existingVerticesAreReboundAndSurvivePolygonRemoval() {
        val board = board()
        val vertices = listOf(
            point(board, -2.0, -2.0, "first"),
            point(board, 1.0, -2.0, "second"),
            point(board, 7.0, 3.0, "third"),
            point(board, -6.0, 5.0, "fourth"),
        )
        val polygon = regularPolygon(
            RegularPolygon.create(
                board = board,
                vertices = vertices,
                id = "regular",
                name = "",
                withLines = false,
            ),
        )
        board.update()

        assertTrue(polygon.borders.isEmpty())
        assertTrue(polygon.implicitVertices.isEmpty())
        assertCoordinates(
            doubleArrayOf(1.0, 1.0, 1.0),
            vertices[2].coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(1.0, -2.0, 1.0),
            vertices[3].coords.usrCoords,
        )
        assertSame(vertices[0], vertices[2].baseElement)
        assertSame(vertices[1], vertices[3].baseElement)
        assertTrue(vertices[2].parents.isEmpty())
        assertTrue(vertices[3].parents.isEmpty())

        vertices[3].setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(8.0, 8.0),
        )
        board.update(draggedElement = vertices[3])
        assertCoordinates(
            doubleArrayOf(1.0, -2.0, 1.0),
            vertices[3].coords.usrCoords,
        )

        vertices[0].setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-3.0, -1.0),
        )
        vertices[1].setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 0.0),
        )
        board.update()

        assertCoordinates(
            doubleArrayOf(1.0, 1.0, 5.0),
            vertices[2].coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(1.0, -4.0, 4.0),
            vertices[3].coords.usrCoords,
        )

        board.removeObject(polygon)

        assertEquals(vertices.map(Point::id).toSet(), board.objects.keys)
        assertEquals(1, vertices[2].transformations.size)
        assertEquals(1, vertices[3].transformations.size)
    }

    @Test
    fun coordinateParentsAreOwnedButGeneratedHelpersSurviveRemoval() {
        val board = board()
        val first = point(board, -2.0, -1.0)
        val second = point(board, 1.0, 1.0)
        val polygon = regularPolygon(
            RegularPolygon.create(
                board = board,
                point1 = first,
                point2 = second,
                numberOfVertices = 4.0,
                ownedPoints = setOf(first, second),
                id = "coordinate",
                name = "",
                withLines = false,
                vertexIds = listOf("third", "fourth"),
                vertexName = "",
            ),
        )
        val generated = polygon.vertices.subList(2, 4).toList()

        assertEquals(
            listOf(first, second) + generated,
            polygon.implicitVertices,
        )
        board.removeObject(polygon)

        assertEquals(setOf("third", "fourth"), board.objects.keys)
        assertSame(generated[0], board.elementById("third"))
        assertSame(generated[1], board.elementById("fourth"))
    }

    @Test
    fun fractionalVertexCountUsesCeilingButOriginalRotationAngle() {
        val board = board()
        val first = point(board, -1.0, 0.0)
        val second = point(board, 1.0, 0.0)
        val polygon = regularPolygon(
            RegularPolygon.create(
                board = board,
                point1 = first,
                point2 = second,
                numberOfVertices = 3.5,
                ownedPoints = setOf(first, second),
                id = "fractional",
                name = "",
                withLines = false,
                vertexIds = listOf("third", "fourth"),
                vertexName = "",
            ),
        )

        assertEquals(5, polygon.vertices.size)
        assertCoordinates(
            doubleArrayOf(1.0, 0.5549581320873715, 1.9498558243636472),
            polygon.vertices[2].coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(1.0, -1.2469796037174667, 1.0820883461285318),
            polygon.vertices[3].coords.usrCoords,
        )
    }

    @Test
    fun invalidCountsAndDuplicateIdsReturnErrorsWithoutLeakingState() {
        val board = board()
        val taken = point(board, 8.0, 8.0, "taken")
        val first = point(board, -1.0, 0.0, "first")
        val second = point(board, 1.0, 0.0, "second")

        assertIs<GMResult.Err<RegularPolygonError.InvalidVertexCount>>(
            RegularPolygon.create(
                board = board,
                point1 = first,
                point2 = second,
                numberOfVertices = 2.0,
            ),
        )
        assertIs<GMResult.Err<RegularPolygonError.InvalidVertexCount>>(
            RegularPolygon.create(
                board = board,
                point1 = first,
                point2 = second,
                numberOfVertices = Double.POSITIVE_INFINITY,
            ),
        )

        val helperFailure = assertIs<
            GMResult.Err<RegularPolygonError.PointCreation>,
            >(
            RegularPolygon.create(
                board = board,
                point1 = first,
                point2 = second,
                numberOfVertices = 4.0,
                id = "unused",
                vertexIds = listOf("temporary", taken.id),
            ),
        ).error
        assertEquals(3, helperFailure.vertexIndex)
        assertEquals(
            setOf("taken", "first", "second"),
            board.objects.keys,
        )

        val existingThird = point(board, 7.0, 3.0, "existingThird")
        val existingFourth = point(board, -6.0, 5.0, "existingFourth")
        val polygonFailure = assertIs<
            GMResult.Err<RegularPolygonError.PolygonCreation>,
            >(
            RegularPolygon.create(
                board = board,
                vertices = listOf(
                    first,
                    second,
                    existingThird,
                    existingFourth,
                ),
                id = taken.id,
            ),
        ).error
        assertEquals(
            PolygonError.Registration(
                BoardError.DuplicateElementId(taken.id),
            ),
            polygonFailure.error,
        )
        assertTrue(existingThird.transformations.isEmpty())
        assertTrue(existingFourth.transformations.isEmpty())
        assertCoordinates(
            doubleArrayOf(1.0, 7.0, 3.0),
            existingThird.coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(1.0, -6.0, 5.0),
            existingFourth.coords.usrCoords,
        )
    }

    private fun board(): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "board",
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

    private fun regularPolygon(
        result: GMResult<Polygon, RegularPolygonError>,
    ): Polygon = assertIs<GMResult.Ok<Polygon>>(result).value

    private fun assertCoordinates(
        expected: DoubleArray,
        actual: DoubleArray,
        tolerance: Double = 1.0e-12,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertTrue(
                abs(expected[index] - actual[index]) <= tolerance,
                "coordinate[$index]: expected=${expected[index]}, " +
                    "actual=${actual[index]}",
            )
        }
    }
}
