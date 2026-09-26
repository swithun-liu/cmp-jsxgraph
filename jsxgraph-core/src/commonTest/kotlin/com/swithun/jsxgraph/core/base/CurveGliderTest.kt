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

class CurveGliderTest {
    @Test
    fun functionGraphGliderProjectsDragsAndTracksParentUpdates() {
        val board = board("function-graph")
        val driver = point(board, 0.0, 1.0, "driver")
        val curve = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "driver.Y() + 0.25 * x * x",
                minimumSource = "-4",
                maximumSource = "4",
                sampleCount = 64,
                id = "functionGraph",
                name = "",
            ),
        )
        val glider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(1.5, 4.0),
                slideObject = curve,
                id = "functionGlider",
                name = "",
            ),
        )

        assertPoint(
            x = 2.8622543136990153,
            y = 3.048124939072155,
            point = glider,
        )
        assertEquals(
            2.8622543136990153,
            assertIs<Double>(glider.position),
            absoluteTolerance = TOLERANCE,
        )
        assertSame(curve, glider.slideElement)
        assertSame(curve, glider.slideObject)
        assertSame(curve, glider.slideObjects.single())
        assertSame(glider, curve.childElements[glider.id])
        assertEquals(listOf(curve.id), glider.parents)

        glider.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, 0.0),
        )
        board.update(draggedElement = glider)
        assertPoint(
            x = 1.6354632777583658,
            y = 1.6686850332240344,
            point = glider,
        )
        assertEquals(
            1.6354632777583658,
            assertIs<Double>(glider.position),
            absoluteTolerance = TOLERANCE,
        )

        driver.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 2.0),
        )
        board.update()
        assertPoint(
            x = 1.6354632777583658,
            y = 1.6686850332240344,
            point = glider,
        )

        board.update()
        assertPoint(
            x = 1.6354632777583658,
            y = 2.6686850332240344,
            point = glider,
        )
        assertEquals(
            1.6354632777583658,
            assertIs<Double>(glider.position),
            absoluteTolerance = TOLERANCE,
        )
    }

    @Test
    fun parametricAndDataGlidersMatchOfficialProjectionParameters() {
        val parametricBoard = board("parametric")
        val parametric = curve(
            Curve.createParametric(
                board = parametricBoard,
                xSource = "2 * cos(x)",
                ySource = "sin(x)",
                minimumSource = "0",
                maximumSource = (2.0 * PI).toString(),
                sampleCount = 64,
                id = "parametric",
                name = "",
            ),
        )
        val parametricGlider = glider(
            Glider.create(
                board = parametricBoard,
                coordinates = doubleArrayOf(3.0, 0.4),
                slideObject = parametric,
                id = "parametricGlider",
                name = "",
            ),
        )
        assertPoint(
            x = 1.9827506191050663,
            y = 0.13105340747046232,
            point = parametricGlider,
        )
        assertEquals(
            0.13143147710988975,
            assertIs<Double>(parametricGlider.position),
            absoluteTolerance = TOLERANCE,
        )

        parametricGlider.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-3.0, 0.2),
        )
        parametricBoard.update(draggedElement = parametricGlider)
        assertPoint(
            x = -1.9955897190913985,
            y = 0.06637332494442595,
            point = parametricGlider,
        )
        assertEquals(
            3.075170498070034,
            assertIs<Double>(parametricGlider.position),
            absoluteTolerance = TOLERANCE,
        )

        parametricGlider.setGliderPosition(PI * 0.5)
        assertPoint(
            x = -1.9955897190913985,
            y = 0.06637332494442595,
            point = parametricGlider,
        )
        assertEquals(
            PI * 0.5,
            assertIs<Double>(parametricGlider.position),
        )

        val plotBoard = board("plot")
        val plot = curve(
            Curve.createData(
                board = plotBoard,
                dataX = doubleArrayOf(-4.0, -1.0, 2.0, 4.0),
                dataY = doubleArrayOf(-2.0, 2.0, -1.0, 2.0),
                id = "plot",
                name = "",
            ),
        )
        val plotGlider = glider(
            Glider.create(
                board = plotBoard,
                coordinates = doubleArrayOf(0.0, 0.0),
                slideObject = plot,
                id = "plotGlider",
                name = "",
            ),
        )
        assertPoint(0.5, 0.5, plotGlider)
        assertEquals(1.5, assertIs<Double>(plotGlider.position))

        plotGlider.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.5, 0.0),
        )
        plotBoard.update(draggedElement = plotGlider)
        assertPoint(
            x = 2.9230769230769234,
            y = 0.3846153846153846,
            point = plotGlider,
        )
        assertEquals(
            2.4615384615384617,
            assertIs<Double>(plotGlider.position),
            absoluteTolerance = TOLERANCE,
        )
    }

    @Test
    fun curveRemovalAndFailuresRemainAtomicAndStructured() {
        val board = board("lifecycle")
        val curve = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-2.0, 0.0, 2.0),
                dataY = doubleArrayOf(0.0, 2.0, 0.0),
                id = "curve",
                name = "",
            ),
        )
        val glider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 1.0),
                slideObject = curve,
                id = "glider",
                name = "",
            ),
        )
        board.removeObject(curve)
        assertTrue(curve.id !in board.objects)
        assertTrue(glider.id !in board.objects)

        val otherBoard = board("other")
        val foreignCurve = curve(
            Curve.createData(
                board = otherBoard,
                dataX = doubleArrayOf(-1.0, 1.0),
                dataY = doubleArrayOf(0.0, 0.0),
                id = "foreign",
                name = "",
            ),
        )
        val before = board.objects.keys.toList()
        assertIs<GMResult.Err<GliderError.SlideObjectBoardMismatch>>(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                slideObject = foreignCurve,
                id = "candidate",
            ),
        )
        assertEquals(before, board.objects.keys.toList())

        val focus1 = point(board, -2.0, 0.0, "focus1")
        val focus2 = point(board, 2.0, 0.0, "focus2")
        val ellipse = assertIs<GMResult.Ok<Curve>>(
            Ellipse.create(
                board = board,
                focus1 = focus1,
                focus2 = focus2,
                majorAxis = 6.0,
                id = "ellipse",
                name = "",
                centerId = "center",
            ),
        ).value
        val beforeConic = board.objects.keys.toList()
        assertIs<GMResult.Err<GliderError.UnsupportedSlideObject>>(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 2.0),
                slideObject = ellipse,
                id = "conicGlider",
            ),
        )
        assertEquals(beforeConic, board.objects.keys.toList())

        point(board, 4.0, 4.0, "collision")
        val duplicateHost = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-1.0, 1.0),
                dataY = doubleArrayOf(0.0, 0.0),
                id = "duplicateHost",
                name = "",
            ),
        )
        val beforeDuplicate = board.objects.keys.toList()
        val duplicate = assertIs<
            GMResult.Err<GliderError.Registration>,
            >(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                slideObject = duplicateHost,
                id = "collision",
            ),
        ).error
        assertEquals(
            BoardError.DuplicateElementId("collision"),
            duplicate.error,
        )
        assertEquals(beforeDuplicate, board.objects.keys.toList())
    }

    @Test
    fun dynamicCurveFailureIsExposedByTheGliderBoundary() {
        val board = board("dynamic-failure")
        val driver = point(board, -2.0, 2.0, "driver")
        val curve = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "x * x",
                minimumSource = "driver.X()",
                maximumSource = "driver.Y()",
                sampleCount = 16,
                id = "curve",
                name = "",
            ),
        )
        val glider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(1.0, 2.0),
                slideObject = curve,
                id = "glider",
                name = "",
            ),
        )

        driver.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, -3.0),
        )
        board.update()

        val gliderError = assertIs<GliderError.CurveEvaluation>(
            glider.evaluationError,
        )
        assertIs<CurveError.InvalidDomain>(gliderError.error)
    }

    private fun board(id: String): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        id = id,
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = id,
            ),
        ).value

    private fun curve(
        result: GMResult<Curve, CurveError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private fun glider(
        result: GMResult<Glider, GliderError>,
    ): Glider = assertIs<GMResult.Ok<Glider>>(result).value

    private fun assertPoint(
        x: Double,
        y: Double,
        point: Point,
    ) {
        assertEquals(x, point.X(), absoluteTolerance = TOLERANCE)
        assertEquals(y, point.Y(), absoluteTolerance = TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1.0e-6
    }
}
