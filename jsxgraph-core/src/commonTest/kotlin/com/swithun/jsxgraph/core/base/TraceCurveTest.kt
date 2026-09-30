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

class TraceCurveTest {
    @Test
    fun circleTraceMatchesOfficialSamplesAndRestoresDependentState() {
        val board = board("circle-trace")
        val center = point(board, 0.0, 0.0, "center")
        val radius = point(board, 2.0, 0.0, "radius")
        val circle = circle(
            Circle.create(
                board = board,
                center = center,
                point2 = radius,
                id = "circle",
                name = "",
            ),
        )
        val fixed = point(board, -3.0, 1.0, "fixed")
        val glider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(2.0, 1.0),
                slideObject = circle,
                id = "glider",
                name = "",
            ),
        )
        val midpoint = midpoint(
            MidpointPoint.create(
                board = board,
                point1 = glider,
                point2 = fixed,
                id = "midpoint",
                name = "",
            ),
        )
        val initialPosition = assertIs<Double>(glider.position)
        val initialGliderCoordinates = glider.coords.usrCoords.copyOf()
        val initialMidpointCoordinates = midpoint.coords.usrCoords.copyOf()

        val trace = curve(
            Curve.createTraceCurve(
                board = board,
                glider = glider,
                tracePoint = midpoint,
                sampleCount = 4,
                id = "trace",
                name = "",
            ),
        )

        assertTrue(trace.isTraceCurve)
        assertEquals("curve", trace.elType)
        assertEquals("plot", trace.curveType)
        assertEquals(5L, trace.requestedPointCount())
        assertTrue(trace.parents.isEmpty())
        assertFalse(trace.id in glider.childElements)
        assertFalse(trace.id in midpoint.childElements)
        assertCurveCoordinates(
            trace,
            listOf(
                -0.5 to 0.5,
                -1.5 to 1.5,
                -2.5 to 0.5,
                -1.5 to -0.5,
                -0.5 to 0.5,
            ),
        )
        assertEquals(
            initialPosition,
            assertIs<Double>(glider.position),
            absoluteTolerance = TOLERANCE,
        )
        assertContentEquals(initialGliderCoordinates, glider.coords.usrCoords)
        assertContentEquals(
            initialMidpointCoordinates,
            midpoint.coords.usrCoords,
        )
        assertSame(midpoint, glider.childElements[midpoint.id])

        radius.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 0.0),
        )
        board.update()

        assertCurveCoordinates(
            trace,
            listOf(
                0.5 to 0.5,
                -1.5 to 2.5,
                -3.5 to 0.5,
                -1.5 to -1.5,
                0.5 to 0.5,
            ),
        )
        assertEquals(
            initialPosition,
            assertIs<Double>(glider.position),
            absoluteTolerance = TOLERANCE,
        )
        assertPoint(
            x = 3.577708763999664,
            y = 1.7888543819998315,
            point = glider,
        )
        assertPoint(
            x = 0.28885438199983193,
            y = 1.3944271909999157,
            point = midpoint,
        )
    }

    @Test
    fun curveAndLineHostsUseOfficialOpenAndClosedSampling() {
        val curveBoard = board("curve-host")
        val host = curve(
            Curve.createParametric(
                board = curveBoard,
                xSource = "x",
                ySource = "x * x - 1",
                minimumSource = "-2",
                maximumSource = "2",
                sampleCount = 5,
                plotOptions = CurvePlotOptions(
                    doAdvancedPlot = false,
                    rdpSmoothing = false,
                ),
                id = "host",
                name = "",
            ),
        )
        val curveGlider = glider(
            Glider.create(
                board = curveBoard,
                coordinates = doubleArrayOf(0.0, -1.0),
                slideObject = host,
                id = "glider",
                name = "",
            ),
        )
        val fixed = point(curveBoard, 2.0, 3.0, "fixed")
        val midpoint = midpoint(
            MidpointPoint.create(
                board = curveBoard,
                point1 = curveGlider,
                point2 = fixed,
                id = "midpoint",
                name = "",
            ),
        )
        val curveTrace = curve(
            Curve.createTraceCurve(
                board = curveBoard,
                glider = curveGlider,
                tracePoint = midpoint,
                sampleCount = 4,
                id = "trace",
                name = "",
            ),
        )

        assertCurveCoordinates(
            curveTrace,
            listOf(
                0.000001477614431699692 to 2.99999408954664,
                0.4999998140108085 to 1.500000371978452,
                1.0 to 1.0,
                1.5000001859891916 to 1.500000371978452,
            ),
            tolerance = OFFICIAL_PROJECTION_TOLERANCE,
        )
        assertEquals(4L, curveTrace.requestedPointCount())
        assertPoint(0.0, -1.0, curveGlider)
        assertPoint(1.0, 1.0, midpoint)

        val lineBoard = board("line-host")
        val start = point(lineBoard, -2.0, -1.0, "start")
        val end = point(lineBoard, 2.0, 3.0, "end")
        val line = line(
            Line.createSegment(
                board = lineBoard,
                point1 = start,
                point2 = end,
                id = "line",
                name = "",
            ),
        )
        val lineGlider = glider(
            Glider.create(
                board = lineBoard,
                coordinates = doubleArrayOf(0.0, 1.0),
                slideObject = line,
                id = "glider",
                name = "",
            ),
        )
        val lineTrace = curve(
            Curve.createTraceCurve(
                board = lineBoard,
                glider = lineGlider,
                tracePoint = lineGlider,
                sampleCount = 4,
                id = "trace",
                name = "",
            ),
        )

        assertCurveCoordinates(
            lineTrace,
            listOf(
                -2.0 to -1.0,
                -1.0 to 0.0,
                0.0 to 1.0,
                1.0 to 2.0,
                2.0 to 3.0,
            ),
        )
        assertEquals(5L, lineTrace.requestedPointCount())
        assertEquals(0.5, lineGlider.position)
        assertPoint(0.0, 1.0, lineGlider)
    }

    @Test
    fun invalidTraceInputsReturnStructuredErrors() {
        val board = board("trace-errors")
        val start = point(board, -1.0, 0.0, "start")
        val end = point(board, 1.0, 0.0, "end")
        val line = line(
            Line.createSegment(
                board = board,
                point1 = start,
                point2 = end,
                id = "line",
                name = "",
            ),
        )
        val glider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                slideObject = line,
                id = "glider",
                name = "",
            ),
        )

        assertEquals(
            CurveError.InvalidSampleCount(
                count = 0,
                maximum = Curve.MAX_SAMPLE_COUNT,
            ),
            assertIs<GMResult.Err<CurveError>>(
                Curve.createTraceCurve(
                    board = board,
                    glider = glider,
                    tracePoint = glider,
                    sampleCount = 0,
                ),
            ).error,
        )

        val otherBoard = board("other")
        val otherPoint = point(otherBoard, 0.0, 0.0, "other-point")
        assertEquals(
            CurveError.TraceParentBoardMismatch("tracepoint"),
            assertIs<GMResult.Err<CurveError>>(
                Curve.createTraceCurve(
                    board = board,
                    glider = glider,
                    tracePoint = otherPoint,
                    sampleCount = 4,
                ),
            ).error,
        )

        val pointHost = point(board, 2.0, 2.0, "point-host")
        val pointGlider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(3.0, 4.0),
                slideObject = pointHost,
                id = "point-glider",
                name = "",
            ),
        )
        assertEquals(
            CurveError.UnsupportedTraceSlideObject("point"),
            assertIs<GMResult.Err<CurveError>>(
                Curve.createTraceCurve(
                    board = board,
                    glider = pointGlider,
                    tracePoint = pointGlider,
                    sampleCount = 4,
                ),
            ).error,
        )
    }

    @Test
    fun tracePointCreatedBeforeGliderDoesNotReenterTraceUpdate() {
        val board = board("trace-order")
        val start = point(board, -1.0, 0.0, "start")
        val end = point(board, 1.0, 0.0, "end")
        val tracePoint = point(board, 3.0, 4.0, "trace-point")
        val line = line(
            Line.createSegment(
                board = board,
                point1 = start,
                point2 = end,
                id = "line",
                name = "",
            ),
        )
        val glider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                slideObject = line,
                id = "glider",
                name = "",
            ),
        )
        val trace = curve(
            Curve.createTraceCurve(
                board = board,
                glider = glider,
                tracePoint = tracePoint,
                sampleCount = 4,
                id = "trace",
                name = "",
            ),
        )

        board.update()

        assertCurveCoordinates(
            trace,
            List(5) { 3.0 to 4.0 },
        )
        assertPoint(0.0, 0.0, glider)
    }

    private fun board(id: String): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        boundingBox = doubleArrayOf(-5.0, 5.0, 5.0, -5.0),
        id = id,
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
    ): Point = assertIs<GMResult.Ok<Point>>(
        Point.create(
            board = board,
            coordinates = doubleArrayOf(x, y),
            id = id,
            name = "",
        ),
    ).value

    private fun circle(
        result: GMResult<Circle, CircleError>,
    ): Circle = assertIs<GMResult.Ok<Circle>>(result).value

    private fun line(
        result: GMResult<Line, LineError>,
    ): Line = assertIs<GMResult.Ok<Line>>(result).value

    private fun midpoint(
        result: GMResult<MidpointPoint, MidpointError>,
    ): MidpointPoint =
        assertIs<GMResult.Ok<MidpointPoint>>(result).value

    private fun glider(
        result: GMResult<Glider, GliderError>,
    ): Glider = assertIs<GMResult.Ok<Glider>>(result).value

    private fun curve(
        result: GMResult<Curve, CurveError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private fun assertPoint(
        x: Double,
        y: Double,
        point: Point,
    ) {
        assertEquals(x, point.X(), absoluteTolerance = TOLERANCE)
        assertEquals(y, point.Y(), absoluteTolerance = TOLERANCE)
    }

    private fun assertCurveCoordinates(
        curve: Curve,
        expected: List<Pair<Double, Double>>,
        tolerance: Double = TOLERANCE,
    ) {
        assertEquals(expected.size, curve.numberPoints)
        assertEquals(expected.size, curve.points.size)
        val dataX = assertIs<DoubleArray>(curve.dataX)
        val dataY = assertIs<DoubleArray>(curve.dataY)
        assertEquals(expected.size, dataX.size)
        assertEquals(expected.size, dataY.size)
        for (index in expected.indices) {
            assertEquals(
                expected[index].first,
                dataX[index],
                absoluteTolerance = tolerance,
            )
            assertEquals(
                expected[index].second,
                dataY[index],
                absoluteTolerance = tolerance,
            )
            assertEquals(
                expected[index].first,
                curve.points[index].usrCoords[1],
                absoluteTolerance = tolerance,
            )
            assertEquals(
                expected[index].second,
                curve.points[index].usrCoords[2],
                absoluteTolerance = tolerance,
            )
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-10
        const val OFFICIAL_PROJECTION_TOLERANCE = 3.0e-5
    }
}
