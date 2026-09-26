/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphGrid2D
import com.swithun.jsxgraph.core.JsxGraphGridDrawZero
import com.swithun.jsxgraph.core.JsxGraphGridFace
import com.swithun.jsxgraph.core.JsxGraphGridForceSquare
import com.swithun.jsxgraph.core.JsxGraphGridLength
import com.swithun.jsxgraph.core.JsxGraphGridMinorElements
import com.swithun.jsxgraph.core.JsxGraphGridPair
import com.swithun.jsxgraph.core.JsxGraphGridResolveError
import com.swithun.jsxgraph.core.JsxGraphGridRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GridTest {
    @Test
    fun factoryCreatesMajorAndMinorCurvesWithOfficialRelationships() {
        val board = board()
        val major = grid(
            Grid.create(
                board = board,
                parentAxes = emptyList(),
                definition = definition(),
                id = "grid",
                name = "mesh",
            ),
        )
        val minor = requireNotNull(major.minorGrid)

        assertEquals(Const.OBJECT_TYPE_GRID, major.type)
        assertEquals(Const.OBJECT_TYPE_GRID, minor.type)
        assertEquals("grid", major.elType)
        assertEquals("grid", minor.elType)
        assertEquals("grid_minor", minor.id)
        assertEquals("mesh_minor", minor.name)
        assertFalse(minor.dump)
        assertSame(major, minor.majorGrid)
        assertTrue(minor in major.inherits)
        assertEquals(listOf(major, minor), board.grids)
        assertEquals(54, major.numberPoints)
        assertEquals(0, minor.dataX?.size)
    }

    @Test
    fun parentAxesSupplyAutomaticMajorAndMinorDistances() {
        val board = board()
        val xAxis = axis(board, horizontal = true, id = "xAxis", 2.0, 1)
        val yAxis = axis(board, horizontal = false, id = "yAxis", 2.5, 3)

        val major = grid(
            Grid.create(
                board = board,
                parentAxes = listOf(xAxis, yAxis),
                definition = definition(
                    minorElements = JsxGraphGridPair(
                        JsxGraphGridMinorElements.Auto,
                        JsxGraphGridMinorElements.Auto,
                    ),
                    parentMajorStep = JsxGraphGridPair(2.0, 2.5),
                    parentMinorElements = JsxGraphGridPair(1.0, 3.0),
                ),
                id = "grid",
            ),
        )

        assertEquals(listOf("xAxis", "yAxis"), major.parents)
        assertEquals(24, major.numberPoints)
        assertEquals(144, requireNotNull(major.minorGrid).numberPoints)
    }

    @Test
    fun removeLifecycleMatchesBoardGridRegistry() {
        val board = board()
        val first = grid(
            Grid.create(
                board,
                emptyList(),
                definition(),
                id = "first",
            ),
        )
        val firstMinor = requireNotNull(first.minorGrid)
        Grid.create(
            board,
            emptyList(),
            definition(),
            id = "second",
        )

        board.removeObject(first)

        assertNull(board.elementById(first.id))
        assertSame(firstMinor, board.elementById(firstMinor.id))
        assertEquals(4, board.grids.size)

        board.removeGrids()

        assertTrue(board.grids.isEmpty())
        assertTrue(board.objects.isEmpty())
    }

    @Test
    fun minorRegistrationFailureRollsBackMajorCurve() {
        val board = board()
        val occupied = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(1.0, 1.0),
                id = "occupied",
                name = "",
            ),
        ).value
        val originalIds = board.objects.keys.toSet()

        val error = assertIs<GMResult.Err<GridError>>(
            Grid.create(
                board = board,
                parentAxes = emptyList(),
                definition = definition(),
                id = "major",
                minorId = occupied.id,
            ),
        ).error

        assertIs<GridError.MinorCurveFactory>(error)
        assertEquals(originalIds, board.objects.keys)
        assertTrue(board.grids.isEmpty())
    }

    @Test
    fun nonAxisParentIsRejectedWithoutMutation() {
        val board = board()
        val first = point(board, -1.0, 0.0)
        val second = point(board, 1.0, 0.0)
        val line = assertIs<GMResult.Ok<Line>>(
            Line.create(board, first, second, id = "line", name = ""),
        ).value
        val originalIds = board.objects.keys.toSet()

        val error = assertIs<GMResult.Err<GridError>>(
            Grid.create(
                board = board,
                parentAxes = listOf(line),
                definition = definition(),
            ),
        ).error

        assertIs<GridError.InvalidParent>(error)
        assertEquals(originalIds, board.objects.keys)
    }

    @Test
    fun denseGridFailsBeforeUnboundedPointAllocationAndRollsBack() {
        val board = board()
        val dense = definition().copy(
            majorStep = JsxGraphGridPair(
                JsxGraphGridLength.User(0.002),
                JsxGraphGridLength.User(0.002),
            ),
            maximumPointCount = 100,
        )

        val error = assertIs<GMResult.Err<GridError>>(
            Grid.create(
                board = board,
                parentAxes = emptyList(),
                definition = dense,
                id = "dense",
            ),
        ).error

        val geometry = assertIs<GridError.Geometry>(error)
        assertEquals(JsxGraphGridRole.Major, geometry.role)
        val limit =
            assertIs<JsxGraphGridResolveError.PointLimitExceeded>(
                geometry.error,
            )
        assertEquals(100, limit.limit)
        assertTrue(limit.requestedSize > limit.limit)
        assertTrue(board.objects.isEmpty())
        assertTrue(board.grids.isEmpty())
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        id = "board",
    )

    private fun axis(
        board: Board,
        horizontal: Boolean,
        id: String,
        ticksDistance: Double,
        minorTicks: Int,
    ): Line {
        val point1 = point(board, 0.0, 0.0)
        val point2 =
            if (horizontal) point(board, 1.0, 0.0)
            else point(board, 0.0, 1.0)
        return assertIs<GMResult.Ok<Line>>(
            Axis.create(
                board = board,
                point1 = point1,
                point2 = point2,
                ticksAttributes = defaultAxisTicksAttributes().copy(
                    ticksDistance = ticksDistance,
                    minorTicks = minorTicks,
                    insertTicks = false,
                ),
                id = id,
            ),
        ).value
    }

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

    private fun grid(result: GMResult<Curve, GridError>): Curve =
        assertIs<GMResult.Ok<Curve>>(result).value

    private fun definition(
        minorElements:
            JsxGraphGridPair<JsxGraphGridMinorElements> =
            JsxGraphGridPair(
                JsxGraphGridMinorElements.Fixed(0.0),
                JsxGraphGridMinorElements.Fixed(0.0),
            ),
        parentMajorStep: JsxGraphGridPair<Double?> =
            JsxGraphGridPair(null, null),
        parentMinorElements: JsxGraphGridPair<Double?> =
            JsxGraphGridPair(null, null),
    ): JsxGraphGrid2D =
        JsxGraphGrid2D(
            role = JsxGraphGridRole.Major,
            majorStep = JsxGraphGridPair(
                JsxGraphGridLength.Auto,
                JsxGraphGridLength.Auto,
            ),
            minorElements = minorElements,
            forceSquare = JsxGraphGridForceSquare.None,
            includeBoundaries = false,
            major = JsxGraphGridFace(
                face = "line",
                size = JsxGraphGridPair(
                    JsxGraphGridLength.Pixels(5.0),
                    JsxGraphGridLength.Pixels(5.0),
                ),
                margin = 0.0,
                drawZero = JsxGraphGridDrawZero.All,
                polygonVertices = 6,
            ),
            minor = JsxGraphGridFace(
                face = "point",
                size = JsxGraphGridPair(
                    JsxGraphGridLength.Pixels(3.0),
                    JsxGraphGridLength.Pixels(3.0),
                ),
                margin = 0.0,
                drawZero = JsxGraphGridDrawZero.All,
                polygonVertices = 6,
            ),
            parentMajorStep = parentMajorStep,
            parentMinorElements = parentMinorElements,
        )
}
