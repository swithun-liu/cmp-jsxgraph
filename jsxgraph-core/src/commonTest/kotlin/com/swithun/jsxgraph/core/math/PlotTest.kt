/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlotTest {
    @Test
    fun smoothAdaptiveSamplingMatchesOfficialUnsmoothedPointSequence() {
        val calls = mutableListOf<Pair<PlotCoordinate, Boolean>>()
        val result = plot(
            x = { parameter, suspendedUpdate ->
                calls += PlotCoordinate.X to suspendedUpdate
                parameter
            },
            y = { parameter, suspendedUpdate ->
                calls += PlotCoordinate.Y to suspendedUpdate
                parameter * parameter
            },
        )

        assertEquals(523, result.points.size)
        assertContentEquals(
            doubleArrayOf(-2.0, 2.0),
            result.visibleArea,
        )
        assertPoint(
            result.points.first(),
            parameter = -2.0,
            x = -2.0,
            y = 4.0,
        )
        assertPoint(
            result.points[1],
            parameter = -1.99609375,
            x = -1.99609375,
            y = 3.9843902587890625,
        )
        assertPoint(
            result.points.last(),
            parameter = 2.0,
            x = 2.0,
            y = 4.0,
        )
        assertEquals(
            listOf(
                PlotCoordinate.X to false,
                PlotCoordinate.Y to false,
                PlotCoordinate.X to true,
                PlotCoordinate.Y to true,
            ),
            calls.take(4),
        )
        assertTrue(calls.drop(2).all { it.second })
    }

    @Test
    fun cuspAndJumpDetectionMatchOfficialReference() {
        val cusp = plot(
            x = { parameter, _ -> parameter },
            y = { parameter, _ -> kotlin.math.abs(parameter) },
        )
        assertEquals(257, cusp.points.size)
        assertPoint(
            cusp.points[128],
            parameter = 0.0,
            x = 0.0,
            y = 0.0,
        )

        val jump = plot(
            x = { parameter, _ -> parameter },
            y = { parameter, _ -> 1.0 / parameter },
        )
        assertEquals(644, jump.points.size)
        val nanIndices = jump.points.indices.filter { index ->
            val point = jump.points[index]
            point.usrCoords[1].isNaN() ||
                point.usrCoords[2].isNaN()
        }
        assertEquals(listOf(327), nanIndices)
        assertEquals(
            -0.000030517578125,
            jump.points[327].curveParameter,
        )
        assertPoint(
            jump.points[326],
            parameter = -0.00006103515625,
            x = -0.00006103515625,
            y = -16384.0,
        )
        assertPoint(
            jump.points[328],
            parameter = 0.068359375,
            x = 0.068359375,
            y = 14.628571428571428,
        )
    }

    @Test
    fun undefinedIntervalAndSingleNanMatchOfficialReference() {
        val border = plot(
            x = { parameter, _ -> parameter },
            y = { parameter, _ ->
                if (parameter < 0.0) {
                    Double.NaN
                } else {
                    kotlin.math.sqrt(parameter)
                }
            },
            random = { 0.375 },
        )
        assertEquals(144, border.points.size)
        assertTrue(border.points[0].usrCoords[2].isNaN())
        assertPoint(
            border.points[1],
            parameter = 0.0,
            x = 0.0,
            y = 0.0,
        )

        val isolated = plot(
            x = { parameter, _ -> parameter },
            y = { parameter, _ ->
                if (parameter == 0.0) Double.NaN else parameter
            },
            random = { 0.375 },
        )
        assertEquals(259, isolated.points.size)
        assertPoint(
            isolated.points[128],
            parameter = -0.00146484375,
            x = -0.00146484375,
            y = -0.00146484375,
        )
        assertTrue(isolated.points[129].usrCoords[1].isNaN())
        assertEquals(
            -0.00006103515625,
            isolated.points[129].curveParameter,
        )
        assertPoint(
            isolated.points[130],
            parameter = 0.0001220703125,
            x = 0.0001220703125,
            y = 0.0001220703125,
        )
    }

    @Test
    fun borderSearchPreservesTheUpstreamRepeatedProbeBehavior() {
        var evaluationCount = 0
        val evaluationCounts = mutableMapOf<Double, Int>()
        val result = plot(
            x = { parameter, _ -> parameter },
            y = { parameter, _ ->
                evaluationCount += 1
                evaluationCounts[parameter] =
                    (evaluationCounts[parameter] ?: 0) + 1
                if (parameter < 0.1) {
                    Double.NaN
                } else {
                    kotlin.math.sqrt(parameter - 0.1)
                }
            },
            random = { 0.375 },
        )

        assertEquals(139, result.points.size)
        assertEquals(1_441, evaluationCount)
        assertEquals(20, evaluationCounts[0.0998992919921875])
        assertTrue(result.points.first().usrCoords[2].isNaN())
        assertPoint(
            result.points[1],
            parameter = 0.10009765625,
            x = 0.10009765625,
            y = 0.009882117688025905,
        )
    }

    @Test
    fun versionTwoPreservesTheUpstreamUncroppedDomainBehavior() {
        val result = plot(
            minimum = -20.0,
            maximum = 20.0,
            x = { parameter, _ -> parameter },
            y = { parameter, _ -> parameter },
        )

        assertEquals(1557, result.points.size)
        assertContentEquals(
            doubleArrayOf(-20.0, 20.0),
            result.visibleArea,
        )
        assertPoint(
            result.points.first(),
            parameter = -20.0,
            x = -20.0,
            y = -20.0,
        )
        assertPoint(
            result.points.last(),
            parameter = 20.0,
            x = 20.0,
            y = 20.0,
        )
    }

    @Test
    fun versionThreeSmoothSamplingAndCallbackSuspensionMatchOfficialReference() {
        val calls = mutableListOf<Pair<PlotCoordinate, Boolean>>()
        val result = plotV3(
            x = { parameter, suspendedUpdate ->
                calls += PlotCoordinate.X to suspendedUpdate
                parameter
            },
            y = { parameter, suspendedUpdate ->
                calls += PlotCoordinate.Y to suspendedUpdate
                parameter * parameter
            },
        )

        assertEquals(453, result.points.size)
        assertContentEquals(
            doubleArrayOf(-2.0, 2.0),
            result.visibleArea,
        )
        assertPoint(
            result.points[1],
            parameter = -1.9951171875,
            x = -1.9951171875,
            y = 3.98049259185791,
        )
        assertEquals(8_194, calls.size)
        assertEquals(
            listOf(
                PlotCoordinate.X to false,
                PlotCoordinate.Y to false,
                PlotCoordinate.X to true,
                PlotCoordinate.Y to true,
            ),
            calls.take(4),
        )
        assertEquals(2, calls.count { !it.second })
        assertEquals(8_192, calls.count { it.second })
    }

    @Test
    fun versionThreeCropsOnlyIdentityXFunctionGraphs() {
        val functionGraph = plotV3(
            minimum = -20.0,
            maximum = 20.0,
            identityXTerm = true,
            x = { parameter, _ -> parameter },
            y = { parameter, _ -> parameter },
        )
        assertEquals(684, functionGraph.points.size)
        assertContentEquals(
            doubleArrayOf(-8.0, 8.0),
            functionGraph.visibleArea,
        )
        assertPoint(
            functionGraph.points.first(),
            parameter = -8.0,
            x = -8.0,
            y = -8.0,
        )
        assertPoint(
            functionGraph.points.last(),
            parameter = 8.0,
            x = 8.0,
            y = 8.0,
        )

        val parametric = plotV3(
            minimum = -20.0,
            maximum = 20.0,
            identityXTerm = false,
            x = { parameter, _ -> parameter },
            y = { parameter, _ -> parameter },
        )
        assertContentEquals(
            doubleArrayOf(-20.0, 20.0),
            parametric.visibleArea,
        )
    }

    @Test
    fun versionThreeLimitPointsMatchOfficialJumpReference() {
        val result = plotV3(
            x = { parameter, _ -> parameter },
            y = { parameter, _ -> 1.0 / parameter },
        )

        assertEquals(592, result.points.size)
        assertEquals(
            listOf(295),
            result.points.indices.filter { index ->
                val point = result.points[index]
                point.usrCoords[1].isNaN() ||
                    point.usrCoords[2].isNaN()
            },
        )
        assertPoint(
            result.points[294],
            parameter = -0.0001220703125,
            x = -0.000008400730446532596,
            y = Double.NEGATIVE_INFINITY,
        )
        assertPoint(
            result.points[296],
            parameter = -0.0001220703125,
            x = 0.000007550984046750604,
            y = -1176747.9318879026,
            tolerance = 1.0e-6,
        )
    }

    @Test
    fun versionThreeBorderAndIsolatedNanHandlingMatchOfficialReference() {
        val border = plotV3(
            x = { parameter, _ -> parameter },
            y = { parameter, _ ->
                if (parameter < 0.0) {
                    Double.NaN
                } else {
                    kotlin.math.sqrt(parameter)
                }
            },
        )
        assertEquals(132, border.points.size)
        assertEquals(
            listOf(0, 1, 3),
            border.points.indices.filter { index ->
                val point = border.points[index]
                point.usrCoords[1].isNaN() ||
                    point.usrCoords[2].isNaN()
            },
        )
        assertPoint(
            border.points[2],
            parameter = -0.0001220703125,
            x = 0.000007022181982346117,
            y = 0.0000908579407821851,
            tolerance = 1.0e-12,
        )

        val isolated = plotV3(
            x = { parameter, _ -> parameter },
            y = { parameter, _ ->
                if (parameter == 0.0) Double.NaN else parameter
            },
        )
        assertEquals(234, isolated.points.size)
        assertEquals(
            listOf(115, 118),
            isolated.points.indices.filter { index ->
                val point = isolated.points[index]
                point.usrCoords[1].isNaN() ||
                    point.usrCoords[2].isNaN()
            },
        )
    }

    @Test
    fun evaluatorAndPointLimitFailuresAreStructured() {
        val evaluation = Plot.updateParametricCurveV2(
            board = board(),
            minimum = -2.0,
            maximum = 2.0,
            recursionDepthHigh = 17,
            maximumPointCount = 10_000,
            x = PlotFunction<String> { parameter, _ ->
                if (parameter == 0.0) {
                    GMResult.Err("x failed")
                } else {
                    GMResult.Ok(parameter)
                }
            },
            y = PlotFunction { parameter, _ ->
                GMResult.Ok(parameter)
            },
            random = { 0.375 },
        )
        val evaluationError =
            assertIs<GMResult.Err<PlotError.Evaluation<String>>>(
                evaluation,
            ).error
        assertEquals(PlotCoordinate.X, evaluationError.coordinate)
        assertEquals(0.0, evaluationError.parameter)
        assertEquals("x failed", evaluationError.error)

        val exception = Plot.updateParametricCurveV2(
            board = board(),
            minimum = -2.0,
            maximum = 2.0,
            recursionDepthHigh = 17,
            maximumPointCount = 10_000,
            x = PlotFunction<Nothing> { _, _ ->
                error("x exception")
            },
            y = PlotFunction<Nothing> { parameter, _ ->
                GMResult.Ok(parameter)
            },
            random = { 0.375 },
        )
        val exceptionError =
            assertIs<GMResult.Err<PlotError.EvaluationException>>(
                exception,
            ).error
        assertEquals(PlotCoordinate.X, exceptionError.coordinate)
        assertEquals(-2.0, exceptionError.parameter)
        assertEquals("x exception", exceptionError.message)

        val pointLimit = Plot.updateParametricCurveV2(
            board = board(),
            minimum = -2.0,
            maximum = 2.0,
            recursionDepthHigh = 17,
            maximumPointCount = 10,
            x = PlotFunction<Nothing> { parameter, _ ->
                GMResult.Ok(parameter)
            },
            y = PlotFunction<Nothing> { parameter, _ ->
                GMResult.Ok(parameter * parameter)
            },
            random = { 0.375 },
        )
        val limitError =
            assertIs<GMResult.Err<PlotError.PointLimitExceeded>>(
                pointLimit,
            ).error
        assertEquals(11, limitError.attemptedCount)
        assertEquals(10, limitError.maximum)

        val versionThreeLimit = Plot.updateParametricCurveV3(
            board = board(),
            minimum = -2.0,
            maximum = 2.0,
            identityXTerm = false,
            recursionDepthHigh = 17,
            maximumPointCount = 10,
            x = PlotFunction<Nothing> { parameter, _ ->
                GMResult.Ok(parameter)
            },
            y = PlotFunction<Nothing> { parameter, _ ->
                GMResult.Ok(parameter * parameter)
            },
            random = { 0.375 },
        )
        val versionThreeLimitError =
            assertIs<GMResult.Err<PlotError.PointLimitExceeded>>(
                versionThreeLimit,
            ).error
        assertEquals(11, versionThreeLimitError.attemptedCount)
        assertEquals(10, versionThreeLimitError.maximum)
    }

    private fun plot(
        minimum: Double = -2.0,
        maximum: Double = 2.0,
        x: (Double, Boolean) -> Double,
        y: (Double, Boolean) -> Double,
        random: () -> Double = { 0.375 },
    ): PlotResult =
        assertIs<GMResult.Ok<PlotResult>>(
            Plot.updateParametricCurveV2(
                board = board(),
                minimum = minimum,
                maximum = maximum,
                recursionDepthHigh = 17,
                maximumPointCount = 10_000,
                x = PlotFunction<Nothing> { parameter, suspendedUpdate ->
                    GMResult.Ok(x(parameter, suspendedUpdate))
                },
                y = PlotFunction<Nothing> { parameter, suspendedUpdate ->
                    GMResult.Ok(y(parameter, suspendedUpdate))
                },
                random = random,
            ),
        ).value

    private fun plotV3(
        minimum: Double = -2.0,
        maximum: Double = 2.0,
        identityXTerm: Boolean = false,
        x: (Double, Boolean) -> Double,
        y: (Double, Boolean) -> Double,
        random: () -> Double = { 0.375 },
    ): PlotResult =
        assertIs<GMResult.Ok<PlotResult>>(
            Plot.updateParametricCurveV3(
                board = board(),
                minimum = minimum,
                maximum = maximum,
                identityXTerm = identityXTerm,
                recursionDepthHigh = 17,
                maximumPointCount = 10_000,
                x = PlotFunction<Nothing> { parameter, suspendedUpdate ->
                    GMResult.Ok(x(parameter, suspendedUpdate))
                },
                y = PlotFunction<Nothing> { parameter, suspendedUpdate ->
                    GMResult.Ok(y(parameter, suspendedUpdate))
                },
                random = random,
            ),
        ).value

    private fun assertPoint(
        point: com.swithun.jsxgraph.core.base.Coords,
        parameter: Double?,
        x: Double,
        y: Double,
        tolerance: Double = 1.0e-15,
    ) {
        assertEquals(parameter, point.curveParameter)
        assertEquals(x, point.usrCoords[1], absoluteTolerance = tolerance)
        assertEquals(y, point.usrCoords[2], absoluteTolerance = tolerance)
    }

    private fun board(): Board =
        Board(
            // Production engines retain user-space Board coordinates and
            // project the final scene separately.
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            boundingBox = doubleArrayOf(-5.0, 5.0, 5.0, -5.0),
            id = "plot-board",
        )
}
