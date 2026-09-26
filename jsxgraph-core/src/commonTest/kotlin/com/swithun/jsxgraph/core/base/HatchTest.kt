/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HatchTest {
    @Test
    fun defaultLineHatchMatchesOfficialFactoryState() {
        val board = board()
        val line = line(
            Line.createSegment(
                board = board,
                point1 = point(board, -4.0, 2.0),
                point2 = point(board, 4.0, 2.0),
                id = "line",
                name = "",
            ),
        )

        val hatch = hatch(
            Hatch.create(
                board = board,
                parent = line,
                numberOfHashes = 3.0,
                id = "hatch",
                name = "",
            ),
        )

        assertEquals("hatch", hatch.elType)
        assertEquals(Const.OBJECT_TYPE_TICKS, hatch.type)
        assertContentEquals(
            doubleArrayOf(-0.2, 0.0, 0.2),
            assertIs<TicksSource.Fixed>(hatch.source).values,
        )
        assertEquals(TicksAnchor.Middle, hatch.attributes.anchor)
        assertTrue(hatch.attributes.drawZero)
        assertEquals(20.0, hatch.attributes.majorHeight)
        assertEquals(0.2, hatch.attributes.ticksDistance)
        assertTrue(hatch in line.inherits)
        assertSame(hatch, line.ticks.single())
        assertSame(hatch, line.childElements[hatch.id])
    }

    @Test
    fun fractionalAndEmptyCountsPreserveJavaScriptLoopSemantics() {
        val board = board()
        val line = line(
            Line.createSegment(
                board = board,
                point1 = point(board, -4.0, 0.0),
                point2 = point(board, 4.0, 0.0),
                id = "line",
                name = "",
            ),
        )

        val fractional = hatch(
            Hatch.create(
                board = board,
                parent = line,
                numberOfHashes = 2.5,
                id = "fractional",
                name = "",
            ),
        )
        assertContentEquals(
            doubleArrayOf(
                -0.15000000000000002,
                0.04999999999999999,
                0.25,
            ),
            assertIs<TicksSource.Fixed>(fractional.source).values,
        )

        listOf(0.0, -3.0, Double.NaN).forEachIndexed {
                index,
                count,
            ->
            val empty = hatch(
                Hatch.create(
                    board = board,
                    parent = line,
                    numberOfHashes = count,
                    id = "empty$index",
                    name = "",
                ),
            )
            assertContentEquals(
                doubleArrayOf(),
                assertIs<TicksSource.Fixed>(empty.source).values,
            )
        }
    }

    @Test
    fun curveHatchUpdatesItsFixedLocationsWithTheParent() {
        val board = board()
        val coefficient = point(
            board = board,
            x = 1.0,
            y = 0.0,
            name = "A",
        )
        val curve = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "A.X() * x * x",
                minimumSource = "-2",
                maximumSource = "2",
                sampleCount = 8,
                id = "curve",
                name = "",
            ),
        )
        val hatch = hatch(
            Hatch.create(
                board = board,
                parent = curve,
                numberOfHashes = 2.0,
                id = "hatch",
                name = "",
            ),
        )
        assertEquals(
            listOf(-0.1, 0.1),
            hatch.curveLocations.map(TicksCurveLocation::baseX),
        )
        hatch.curveLocations.forEach { location ->
            assertEquals(
                0.01,
                location.baseY,
                absoluteTolerance = TOLERANCE,
            )
        }

        coefficient.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, 0.0),
        )
        board.update()

        assertEquals(
            listOf(-0.1, 0.1),
            hatch.curveLocations.map(TicksCurveLocation::baseX),
        )
        hatch.curveLocations.forEach { location ->
            assertEquals(
                0.02,
                location.baseY,
                absoluteTolerance = TOLERANCE,
            )
        }
        assertTrue(hatch in curve.inherits)
    }

    @Test
    fun excessiveCountsFailBeforeAllocationOrRegistration() {
        val board = board()
        val line = line(
            Line.createSegment(
                board = board,
                point1 = point(board, -4.0, 0.0),
                point2 = point(board, 4.0, 0.0),
                id = "line",
                name = "",
            ),
        )
        val objectsBefore = board.objects.keys.toSet()
        val inheritsBefore = line.inherits.toList()

        val finite =
            assertIs<GMResult.Err<TicksError.TickCountLimitExceeded>>(
                Hatch.create(
                    board = board,
                    parent = line,
                    numberOfHashes = 2.5,
                    maximumTickCount = 2,
                ),
            ).error
        assertEquals(3L, finite.requestedSize)

        val infinite =
            assertIs<GMResult.Err<TicksError.TickCountLimitExceeded>>(
                Hatch.create(
                    board = board,
                    parent = line,
                    numberOfHashes = Double.POSITIVE_INFINITY,
                ),
            ).error
        assertEquals(Long.MAX_VALUE, infinite.requestedSize)
        assertTrue(line.ticks.isEmpty())
        assertEquals(inheritsBefore, line.inherits)
        assertEquals(objectsBefore, board.objects.keys)
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        id = "board",
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        name: String? = "",
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                name = name,
            ),
        ).value

    private fun line(result: GMResult<Line, LineError>): Line =
        assertIs<GMResult.Ok<Line>>(result).value

    private fun curve(result: GMResult<Curve, CurveError>): Curve =
        assertIs<GMResult.Ok<Curve>>(result).value

    private fun hatch(result: GMResult<Ticks, TicksError>): Ticks =
        assertIs<GMResult.Ok<Ticks>>(result).value

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
