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

class ParallelogramTest {
    @Test
    fun registeredParentsMatchOfficialGeometryOrderAndLifecycle() {
        val board = board()
        val first = point(board, -4.0, -1.0, "A")
        val second = point(board, 2.0, 3.0, "B")
        val third = point(board, 3.0, -3.0, "C")
        val parallelogram = parallelogram(
            Parallelogram.create(
                board = board,
                point1 = first,
                point2 = second,
                point3 = third,
                id = "registered",
                name = "",
                parallelPointId = "registeredHelper",
            ),
        )
        val helper = assertIs<ParallelPoint>(
            parallelogram.parallelPoint,
        )

        assertEquals("parallelogram", parallelogram.elType)
        assertEquals(Const.OBJECT_TYPE_POLYGON, parallelogram.type)
        assertEquals(Const.OBJECT_CLASS_AREA, parallelogram.elementClass)
        assertTrue(parallelogram.isDraggable)
        assertSame(helper, parallelogram.vertices[2])
        assertEquals(
            listOf("A", "B", "registeredHelper", "C", "A"),
            parallelogram.vertices.map(Point::id),
        )
        assertEquals(
            listOf(
                "A" to "B",
                "B" to "registeredHelper",
                "registeredHelper" to "C",
                "C" to "A",
            ),
            parallelogram.borders.map { border ->
                border.point1.id to border.point2.id
            },
        )
        assertEquals(
            listOf(
                "A",
                "B",
                "C",
                "registeredHelper",
                parallelogram.borders[1].id,
                parallelogram.borders[2].id,
                parallelogram.borders[3].id,
                parallelogram.borders[0].id,
                "registered",
            ),
            board.objectsList.map(GeometryElement::id),
        )
        assertCoordinates(
            doubleArrayOf(1.0, 9.0, 1.0),
            helper.coords.usrCoords,
        )
        assertTrue(helper.isDraggable)
        assertFalse(helper.isFixed)
        assertSame(parallelogram, helper.childElements[parallelogram.id])
        assertFalse(helper.id in parallelogram.childElements)
        assertEquals(40.0, parallelogram.Area())
        assertEquals(28.982424880416993, parallelogram.Perimeter())

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 2.0),
        )
        second.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 1.0),
        )
        third.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, -4.0),
        )
        board.update()

        assertCoordinates(
            doubleArrayOf(1.0, 7.0, -5.0),
            helper.coords.usrCoords,
        )
        assertEquals(33.0, parallelogram.Area())

        board.removeObject(parallelogram)

        assertSame(first, board.elementById("A"))
        assertSame(second, board.elementById("B"))
        assertSame(third, board.elementById("C"))
        assertSame(helper, board.elementById("registeredHelper"))
        assertEquals(
            setOf("A", "B", "C", "registeredHelper"),
            board.objects.keys,
        )
        assertTrue(helper.childElements.isEmpty())
    }

    @Test
    fun coordinateParentsBelongToParallelPointAndSurvivePolygonRemoval() {
        val board = board("coordinate-board")
        val points = listOf(
            point(board, -4.0, -1.0),
            point(board, 2.0, 3.0),
            point(board, 3.0, -3.0),
        )
        val parallelogram = parallelogram(
            Parallelogram.create(
                board = board,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                ownedPoints = points.toSet(),
                id = "coordinate",
                name = "",
                withLines = false,
                parallelPointId = "coordinateHelper",
            ),
        )
        val helper = assertIs<ParallelPoint>(
            parallelogram.parallelPoint,
        )

        assertTrue(parallelogram.borders.isEmpty())
        assertTrue(parallelogram.ownedVertices.isEmpty())
        assertEquals(
            points.map(Point::id).toSet() + parallelogram.id,
            helper.childElements.keys,
        )

        board.removeObject(parallelogram)

        assertSame(helper, board.elementById(helper.id))
        for (point in points) {
            assertSame(point, board.elementById(point.id))
        }

        board.removeObject(helper)
        assertTrue(board.objects.isEmpty())
    }

    @Test
    fun factoryFailuresAreStructuredAndRollbackCreatedHelperOwnership() {
        val board = board()
        val existing = point(board, 8.0, 8.0, "taken")
        val points = listOf(
            point(board, -4.0, -1.0),
            point(board, 2.0, 3.0),
            point(board, 3.0, -3.0),
        )

        val result = Parallelogram.create(
            board = board,
            point1 = points[0],
            point2 = points[1],
            point3 = points[2],
            ownedPoints = points.toSet(),
            id = existing.id,
            parallelPointId = "temporaryHelper",
        )

        val error = assertIs<
            GMResult.Err<ParallelogramError.PolygonCreation>,
            >(result).error
        assertEquals(
            PolygonError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            error.error,
        )
        assertEquals(setOf("taken"), board.objects.keys)
        assertSame(existing, board.elementById("taken"))
    }

    private fun board(id: String = "board"): Board =
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

    private fun parallelogram(
        result: GMResult<Polygon, ParallelogramError>,
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
