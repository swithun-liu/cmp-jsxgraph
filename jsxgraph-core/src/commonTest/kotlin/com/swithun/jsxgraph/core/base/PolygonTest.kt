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
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.math.sqrt

class PolygonTest {
    @Test
    fun polygonClosesVerticesAndCreatesUpstreamBorderOrder() {
        val board = board()
        val first = point(board, 0.0, 0.0)
        val second = point(board, 4.0, 0.0)
        val third = point(board, 0.0, 3.0)

        val polygon = polygon(
            Polygon.create(
                board = board,
                vertices = listOf(first, second, third),
            ),
        )

        assertEquals("boardPy6", polygon.id)
        assertEquals("P_{a}", polygon.name)
        assertEquals("polygon", polygon.elType)
        assertEquals(Const.OBJECT_TYPE_POLYGON, polygon.type)
        assertEquals(Const.OBJECT_CLASS_AREA, polygon.elementClass)
        assertEquals(4, polygon.vertices.size)
        assertSame(first, polygon.vertices.last())
        assertEquals(3, polygon.borders.size)
        assertSame(first, polygon.borders[0].point1)
        assertSame(second, polygon.borders[0].point2)
        assertSame(second, polygon.borders[1].point1)
        assertSame(third, polygon.borders[1].point2)
        assertSame(third, polygon.borders[2].point1)
        assertSame(first, polygon.borders[2].point2)
        assertEquals(
            listOf(
                first,
                second,
                third,
                polygon.borders[1],
                polygon.borders[2],
                polygon.borders[0],
                polygon,
            ),
            board.objectsList,
        )
        assertEquals(6.0, polygon.Area())
        assertEquals(12.0, polygon.Perimeter())
        assertEquals(12.0, polygon.L())
        assertContentEquals(
            doubleArrayOf(0.0, 3.0, 4.0, 0.0),
            polygon.bounds(),
        )
        assertSame(polygon, board.select(polygon.id))
        assertSame(polygon, board.select(polygon.name))
        assertSame(polygon, first.childElements[polygon.id])
        assertTrue(
            polygon.borders.all {
                polygon.childElements[it.id] === it
            },
        )
    }

    @Test
    fun polygonalChainRemovesOnlyTheClosingVertexAndBorder() {
        val board = board()
        val first = point(board, 0.0, 0.0)
        val second = point(board, 4.0, 0.0)
        val third = point(board, 4.0, 3.0)
        val fourth = point(board, -1.0, 2.0)
        val chain = polygon(
            Polygon.createPolygonalChain(
                board = board,
                vertices = listOf(first, second, third, fourth),
            ),
        )

        assertEquals("polygonalchain", chain.elType)
        assertEquals(
            listOf(first, second, third, fourth),
            chain.vertices,
        )
        assertEquals(3, chain.borders.size)
        assertSame(first, chain.borders[0].point1)
        assertSame(second, chain.borders[0].point2)
        assertSame(second, chain.borders[1].point1)
        assertSame(third, chain.borders[1].point2)
        assertSame(third, chain.borders[2].point1)
        assertSame(fourth, chain.borders[2].point2)
        assertEquals(11.5, chain.Area())
        assertEquals(7.0 + sqrt(26.0), chain.Perimeter())
        assertEquals(chain.Perimeter(), chain.L())
        assertContentEquals(
            doubleArrayOf(0.0, 3.0, 4.0, 0.0),
            chain.bounds(),
        )
        assertEquals(8, board.objectsList.size)
        assertTrue(chain.borders.all { it.id in chain.childElements })

        board.removeObject(chain)

        assertEquals(
            listOf(first, second, third, fourth).map(Point::id),
            board.objectsList.map(GeometryElement::id),
        )
        assertTrue(
            listOf(first, second, third, fourth).all {
                chain.id !in it.childElements
            },
        )
    }

    @Test
    fun borderlessPolygonalChainKeepsItsOpenVertices() {
        val board = board()
        val vertices = listOf(
            point(board, 0.0, 0.0),
            point(board, 2.0, 0.0),
            point(board, 0.0, 2.0),
        )
        val chain = polygon(
            Polygon.createPolygonalChain(
                board = board,
                vertices = vertices,
                withLines = false,
                name = "",
            ),
        )

        assertEquals(vertices, chain.vertices)
        assertTrue(chain.borders.isEmpty())
        assertFalse(chain.vertices.first() === chain.vertices.last())
        assertEquals(4, board.numObjects)
    }

    @Test
    fun ownedVerticesAreRemovedWithPolygon() {
        val board = board()
        val first = point(board, 0.0, 0.0, name = "")
        val second = point(board, 1.0, 0.0, name = "")
        val third = point(board, 0.0, 1.0, name = "")
        val polygon = polygon(
            Polygon.create(
                board = board,
                vertices = listOf(first, second, third),
                ownedVertices = setOf(first, second, third),
            ),
        )

        assertTrue(polygon.ownedVertices.all { it.id in polygon.childElements })
        board.removeObject(polygon)

        assertTrue(board.objects.isEmpty())
        assertTrue(board.objectsList.isEmpty())
    }

    @Test
    fun borderlessPolygonDoesNotCreateSegmentChildren() {
        val board = board()
        val polygon = polygon(
            Polygon.create(
                board = board,
                vertices = listOf(
                    point(board, 0.0, 0.0),
                    point(board, 2.0, 0.0),
                    point(board, 0.0, 2.0),
                ),
                withLines = false,
                name = "",
            ),
        )

        assertTrue(polygon.borders.isEmpty())
        assertEquals(4, board.numObjects)
    }

    @Test
    fun registrationAndParentFailuresDoNotCreateBorders() {
        val board = board()
        val first = point(board, 0.0, 0.0)
        val second = point(board, 1.0, 0.0)
        val otherBoardPoint = point(board("other"), 0.0, 1.0)

        assertEquals(
            PolygonError.ParentBoardMismatch(parentIndex = 2),
            assertIs<GMResult.Err<PolygonError.ParentBoardMismatch>>(
                Polygon.create(
                    board = board,
                    vertices = listOf(first, second, otherBoardPoint),
                ),
            ).error,
        )
        assertEquals(2, board.numObjects)

        val existing = point(board, 0.0, 1.0, id = "polygon")
        assertEquals(
            PolygonError.Registration(
                BoardError.DuplicateElementId("polygon"),
            ),
            assertIs<GMResult.Err<PolygonError.Registration>>(
                Polygon.create(
                    board = board,
                    vertices = listOf(first, second, existing),
                    id = "polygon",
                ),
            ).error,
        )
        assertEquals(3, board.numObjects)
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
        name: String? = null,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = name,
            ),
        ).value

    private fun polygon(
        result: GMResult<Polygon, PolygonError>,
    ): Polygon = assertIs<GMResult.Ok<Polygon>>(result).value
}
