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
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TicksTest {
    @Test
    fun lineTicksRegisterWithTheirParentAndAreRemovedRecursively() {
        val board = board()
        val point1 = point(board, -2.0, 0.0)
        val point2 = point(board, 2.0, 0.0)
        val line = line(
            Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                id = "line",
                name = "",
            ),
        )

        val ticks = ticks(
            Ticks.create(
                board = board,
                parent = line,
                source = TicksSource.Fixed(
                    doubleArrayOf(-1.0, 0.0, 1.0),
                ),
                id = "ticks",
                name = "",
            ),
        )

        assertSame(ticks, board.elementById("ticks"))
        assertEquals(listOf(ticks), line.ticks)
        assertSame(ticks, line.childElements[ticks.id])
        assertSame(line, ticks.ancestors[line.id])
        assertEquals(listOf(line.id), ticks.parents)

        board.removeObject(line)

        assertNull(board.elementById(line.id))
        assertNull(board.elementById(ticks.id))
        assertTrue(line.ticks.isEmpty())
        assertEquals(setOf(point1.id, point2.id), board.objects.keys)
    }

    @Test
    fun fixedCurveTicksMatchOfficialParabolaFixture() {
        val board = board()
        val curve = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "x * x",
                minimumSource = "-2",
                maximumSource = "2",
                sampleCount = 8,
                id = "curve",
                name = "",
            ),
        )
        val ticks = ticks(
            Ticks.create(
                board = board,
                parent = curve,
                source = TicksSource.Fixed(
                    doubleArrayOf(0.0, 1.0, 2.0, 4.0),
                ),
                attributes = TicksAttributes(
                    labels = listOf(
                        "left",
                        "inside",
                        "middle",
                        "right",
                    ),
                    drawLabels = true,
                ),
                id = "curveTicks",
                name = "",
            ),
        )

        assertEquals(4, ticks.curveLocations.size)
        assertLocation(
            ticks.curveLocations[0],
            baseX = -2.0,
            baseY = 4.0,
            normalX = 1.75,
            normalY = 0.5,
            label = "left",
        )
        assertLocation(
            ticks.curveLocations[1],
            baseX = -1.0,
            baseY = 1.0,
            normalX = 2.0,
            normalY = 1.0,
            label = "inside",
        )
        assertLocation(
            ticks.curveLocations[2],
            baseX = 0.0,
            baseY = 0.0,
            normalX = 0.0,
            normalY = 1.0,
            label = "middle",
        )
        assertLocation(
            ticks.curveLocations[3],
            baseX = 2.0,
            baseY = 4.0,
            normalX = -1.25,
            normalY = 0.5,
            label = "right",
        )
        assertTrue(ticks.curveLocations.all(TicksCurveLocation::major))
    }

    @Test
    fun curveTicksRecomputeAfterAParentDependencyMoves() {
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
        val ticks = ticks(
            Ticks.create(
                board = board,
                parent = curve,
                source = TicksSource.Fixed(doubleArrayOf(1.0)),
                id = "ticks",
                name = "",
            ),
        )
        assertLocation(
            ticks.curveLocations.single(),
            baseX = -1.0,
            baseY = 1.0,
            normalX = 2.0,
            normalY = 1.0,
            label = null,
        )

        coefficient.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, 0.0),
        )
        board.update()

        assertLocation(
            ticks.curveLocations.single(),
            baseX = -1.0,
            baseY = 2.0,
            normalX = 4.0,
            normalY = 1.0,
            label = null,
        )
    }

    @Test
    fun excessiveTickRequestsFailBeforeRegistration() {
        val board = board()
        val curve = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "x",
                minimumSource = "-2",
                maximumSource = "2",
                sampleCount = 8,
                id = "curve",
                name = "",
            ),
        )

        val fixed = assertIs<GMResult.Err<TicksError.TickCountLimitExceeded>>(
            Ticks.create(
                board = board,
                parent = curve,
                source = TicksSource.Fixed(
                    doubleArrayOf(0.0, 1.0, 2.0),
                ),
                maximumTickCount = 2,
            ),
        ).error
        assertEquals(2, fixed.limit)
        assertEquals(3, fixed.requestedSize)

        val lineBoard = board()
        val lineParent = line(
            Line.createSegment(
                board = lineBoard,
                point1 = point(lineBoard, -2.0, 0.0),
                point2 = point(lineBoard, 2.0, 0.0),
                id = "line",
                name = "",
            ),
        )
        val fixedLine =
            assertIs<GMResult.Err<TicksError.TickCountLimitExceeded>>(
                Ticks.create(
                    board = lineBoard,
                    parent = lineParent,
                    source = TicksSource.Fixed(
                        doubleArrayOf(0.0, 1.0, 2.0),
                    ),
                    maximumTickCount = 2,
                ),
            ).error
        assertEquals(2, fixedLine.limit)
        assertEquals(3, fixedLine.requestedSize)
        assertTrue(lineParent.ticks.isEmpty())

        val equidistant =
            assertIs<GMResult.Err<TicksError.TickCountLimitExceeded>>(
                Ticks.create(
                    board = board,
                    parent = curve,
                    attributes = TicksAttributes(
                        minorTicks = 0,
                        ticksDistance = 0.1,
                    ),
                    maximumTickCount = 10,
                ),
            ).error
        assertEquals(10, equidistant.limit)
        assertTrue(equidistant.requestedSize > equidistant.limit)
        assertTrue(curve.ticks.isEmpty())
        assertEquals(setOf(curve.id), board.objects.keys)
    }

    @Test
    fun labelFormattingMatchesOfficialJavaScriptRules() {
        assertEquals(
            "1.235e+5",
            formatTicksLabel(
                value = 123456.0,
                maxLabelLength = 5,
                precision = 3,
                digits = 3,
                scaleSymbol = "",
                beautifulScientificTickLabels = false,
                useUnicodeMinus = true,
            ),
        )
        assertEquals(
            "\u22121.235\u202210\u207B\u2077",
            formatTicksLabel(
                value = -0.000000123456,
                maxLabelLength = 5,
                precision = 3,
                digits = 3,
                scaleSymbol = "",
                beautifulScientificTickLabels = true,
                useUnicodeMinus = true,
            ),
        )
        assertEquals(
            "\u2212\u03C0",
            formatTicksLabel(
                value = -1.0,
                maxLabelLength = 5,
                precision = 3,
                digits = 3,
                scaleSymbol = "\u03C0",
                beautifulScientificTickLabels = false,
                useUnicodeMinus = true,
            ),
        )
        assertEquals(
            "0",
            formatTicksLabel(
                value = 0.0,
                maxLabelLength = 5,
                precision = 3,
                digits = 3,
                scaleSymbol = "\u03C0",
                beautifulScientificTickLabels = false,
                useUnicodeMinus = true,
            ),
        )
    }

    @Test
    fun invalidInputsRemainStructuredAndAtomic() {
        val board = board()
        val line = line(
            Line.create(
                board = board,
                point1 = point(board, -1.0, 0.0),
                point2 = point(board, 1.0, 0.0),
                id = "line",
                name = "",
            ),
        )

        val invalidTick =
            assertIs<GMResult.Err<TicksError.InvalidFixedTick>>(
                Ticks.create(
                    board = board,
                    parent = line,
                    source = TicksSource.Fixed(
                        doubleArrayOf(0.0, Double.NaN),
                    ),
                ),
            ).error
        assertEquals(1, invalidTick.index)
        assertTrue(invalidTick.value.isNaN())

        val invalidAnchor =
            assertIs<GMResult.Err<TicksError.InvalidAttribute>>(
                Ticks.create(
                    board = board,
                    parent = line,
                    attributes = TicksAttributes(
                        anchor = TicksAnchor.Fraction(
                            Double.POSITIVE_INFINITY,
                        ),
                    ),
                ),
            ).error
        assertEquals("anchor", invalidAnchor.attribute)
        assertTrue(line.ticks.isEmpty())
        assertFalse(board.objects.values.any { it is Ticks })
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

    private fun ticks(result: GMResult<Ticks, TicksError>): Ticks =
        assertIs<GMResult.Ok<Ticks>>(result).value

    private fun assertLocation(
        actual: TicksCurveLocation,
        baseX: Double,
        baseY: Double,
        normalX: Double,
        normalY: Double,
        label: String?,
    ) {
        assertEquals(baseX, actual.baseX, absoluteTolerance = TOLERANCE)
        assertEquals(baseY, actual.baseY, absoluteTolerance = TOLERANCE)
        assertEquals(normalX, actual.normalX, absoluteTolerance = TOLERANCE)
        assertEquals(normalY, actual.normalY, absoluteTolerance = TOLERANCE)
        assertEquals(label, actual.label)
    }

    private companion object {
        const val TOLERANCE = 1.0e-8
    }
}
