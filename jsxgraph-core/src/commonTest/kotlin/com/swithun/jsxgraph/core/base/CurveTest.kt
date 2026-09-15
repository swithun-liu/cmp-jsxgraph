/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CurveTest {
    @Test
    fun dataCurveUsesTheUpstreamXArrayLengthAndMissingYBecomesNaN() {
        val board = board()

        val curve = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-2.0, 0.0, 3.0),
                dataY = doubleArrayOf(4.0, 1.0),
            ),
        )

        assertEquals("boardG0", curve.id)
        assertEquals("s_{a}", curve.name)
        assertEquals("curve", curve.elType)
        assertEquals("plot", curve.curveType)
        assertEquals(Const.OBJECT_TYPE_CURVE, curve.type)
        assertEquals(Const.OBJECT_CLASS_CURVE, curve.elementClass)
        assertEquals(1, curve.bezierDegree)
        assertEquals(3, curve.numberPoints)
        assertEquals(3, curve.points.size)
        assertContentEquals(
            doubleArrayOf(1.0, -2.0, 4.0),
            curve.points[0].usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 1.0),
            curve.points[1].usrCoords,
        )
        assertTrue(curve.points[2].usrCoords[2].isNaN())
        assertSame(curve, board.select(curve.id))
        assertSame(curve, board.select(curve.name))
    }

    @Test
    fun naiveFunctionGraphSamplingMatchesOfficialRightOpenDomain() {
        val curve = curve(
            Curve.createFunctionGraph(
                board = board(),
                ySource = "x * x",
                minimumSource = "-2",
                maximumSource = "2",
                sampleCount = 4,
                name = "",
            ),
        )

        assertEquals("functiongraph", curve.curveType)
        assertEquals(4, curve.numberPoints)
        assertEquals(-2.0, curve.minX())
        assertEquals(2.0, curve.maxX())
        assertEquals(-1.5, curve.X(-1.5))
        assertEquals(2.25, curve.Y(-1.5))
        assertContentEquals(
            doubleArrayOf(-2.0, -1.0, 0.0, 1.0),
            curve.points.map { it.usrCoords[1] }.toDoubleArray(),
        )
        assertContentEquals(
            doubleArrayOf(4.0, 1.0, 0.0, 1.0),
            curve.points.map { it.usrCoords[2] }.toDoubleArray(),
        )
    }

    @Test
    fun parametricCurveExpressionsTrackBoardDependencies() {
        val board = board()
        val driver = point(
            board = board,
            coordinates = doubleArrayOf(2.0, 1.0),
            name = "A",
        )
        val curve = curve(
            Curve.createParametric(
                board = board,
                xSource = "x",
                ySource = "A.X() * x",
                minimumSource = "0",
                maximumSource = "2",
                sampleCount = 2,
                name = "",
            ),
        )

        assertSame(curve, driver.childElements[curve.id])
        assertContentEquals(
            doubleArrayOf(0.0, 2.0),
            curve.points.map { it.usrCoords[2] }.toDoubleArray(),
        )

        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(3.0, 1.0),
        )
        board.update()

        assertContentEquals(
            doubleArrayOf(0.0, 3.0),
            curve.points.map { it.usrCoords[2] }.toDoubleArray(),
        )
    }

    @Test
    fun curveCreationFailuresRemainExplicitAndAtomic() {
        val board = board()

        assertEquals(
            CurveError.InvalidSampleCount(
                count = 0,
                maximum = Curve.MAX_SAMPLE_COUNT,
            ),
            assertIs<GMResult.Err<CurveError.InvalidSampleCount>>(
                Curve.createFunctionGraph(
                    board = board,
                    ySource = "x",
                    minimumSource = "0",
                    maximumSource = "1",
                    sampleCount = 0,
                ),
            ).error,
        )
        val compileError = assertIs<
            GMResult.Err<CurveError.ExpressionCompile>
            >(
            Curve.createFunctionGraph(
                board = board,
                ySource = "x +",
                minimumSource = "0",
                maximumSource = "1",
                sampleCount = 4,
            ),
        ).error
        assertEquals("yterm", compileError.term)
        assertIs<JessieCodeExpressionCompileError.Parser>(compileError.error)
        assertIs<CurveError.NonNumericExpression>(
            assertIs<GMResult.Err<CurveError.NonNumericExpression>>(
                Curve.createFunctionGraph(
                    board = board,
                    ySource = "\"not a number\"",
                    minimumSource = "0",
                    maximumSource = "1",
                    sampleCount = 4,
                ),
            ).error,
        )
        assertIs<CurveError.InvalidDomain>(
            assertIs<GMResult.Err<CurveError.InvalidDomain>>(
                Curve.createFunctionGraph(
                    board = board,
                    ySource = "x",
                    minimumSource = "2",
                    maximumSource = "-2",
                    sampleCount = 4,
                ),
            ).error,
        )

        assertEquals(0, board.numObjects)
        assertTrue(board.objects.isEmpty())
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
        coordinates: DoubleArray,
        name: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = coordinates,
                name = name,
            ),
        ).value

    private fun curve(
        result: GMResult<Curve, CurveError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value
}
