/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImplicitPlotTest {
    @Test
    fun directMathDefaultsPreserveUpstreamMaximumStepWidth() {
        assertEquals(1.0, ImplicitPlotConfig().hMax)
    }

    @Test
    fun circleTraceMatchesOfficialImplicitPlotSequence() {
        val plot = circlePlot()
        val trace = plot.traceComponent(
            initialPoint = doubleArrayOf(1.0, 1.0, 0.0),
        )

        assertEquals(129, trace.first.size)
        assertEquals(129, trace.second.size)
        assertClose(-0.9941520442251706, trace.first[0])
        assertClose(0.10798941134910316, trace.second[0])
        assertClose(1.0, trace.first[64])
        assertClose(0.0, trace.second[64])
        assertClose(-0.9941520442251706, trace.first.last())
        assertClose(-0.10798941134910316, trace.second.last())
    }

    @Test
    fun circlePlotMatchesOfficialComponentAndEndpointSamples() {
        val result = assertIs<GMResult.Ok<ImplicitPlotResult>>(
            circlePlot().plot(),
        ).value

        assertEquals(1, result.componentCount)
        assertEquals(129, result.dataX.size)
        assertEquals(129, result.dataY.size)
        assertClose(0.9941520442251717, result.dataX.first())
        assertClose(-0.10798941134909334, result.dataY.first())
        assertClose(-1.0, result.dataX[64])
        assertClose(6.938893903907228e-17, result.dataY[64])
        assertClose(0.9941520442251773, result.dataX.last())
        assertClose(0.10798941134904091, result.dataY.last())
    }

    @Test
    fun foliumPlotMatchesOfficialComponentSeparation() {
        val result = assertIs<GMResult.Ok<ImplicitPlotResult>>(
            ImplicitPlot(
                boundingBox = doubleArrayOf(-2.0, 2.0, 2.0, -2.0),
                config = referenceConfig(),
                f = { x, y -> x * x * x - 2.0 * x * y + y * y * y },
                dfx = { x, y -> 3.0 * x * x - 2.0 * y },
                dfy = { x, y -> -2.0 * x + 3.0 * y * y },
            ).plot(),
        ).value

        assertEquals(2, result.componentCount)
        assertEquals(119, result.dataX.size)
        assertEquals(119, result.dataY.size)
        assertEquals(57, result.dataX.indexOfFirst(Double::isNaN))
        assertEquals(57, result.dataY.indexOfFirst(Double::isNaN))
        assertClose(1.0580380132683052, result.dataX.first())
        assertClose(0.8521899178634239, result.dataY.first())
        assertClose(1.36502731433911, result.dataX[58])
        assertClose(-2.0, result.dataY[58])
        assertClose(0.8973995944932868, result.dataX.last())
        assertClose(1.0527703408578069, result.dataY.last())
    }

    @Test
    fun tangentBifurcationAndQuasiNewtonHelpersMatchOfficialValues() {
        val circle = circlePlot()
        val tangent = assertNotNull(circle.tangent(doubleArrayOf(1.0, 0.0)))
        assertClose(0.0, tangent[0])
        assertClose(1.0, tangent[1])

        val approximate = circle.tangentA(doubleArrayOf(2.0, 0.0))
        assertClose(0.0, approximate[0])
        assertClose(1.0, approximate[1])

        val a = doubleArrayOf(2.0, 0.0)
        val updated = circle.updateA(
            initialA = a,
            initialPoint = doubleArrayOf(1.0, 0.0),
            finalPoint = doubleArrayOf(0.9, 0.1),
        )
        assertTrue(updated === a)
        assertClose(1.9, updated[0])
        assertClose(0.10000000000000012, updated[1])

        val bifurcation = ImplicitPlot(
            boundingBox = doubleArrayOf(-2.0, 2.0, 2.0, -2.0),
            config = referenceConfig(),
            f = { x, y -> x * x - y * y },
            dfx = { x, _ -> 2.0 * x },
            dfy = { _, y -> -2.0 * y },
        )
        assertTrue(
            bifurcation.isBifurcation(
                point = doubleArrayOf(0.0, 0.0),
                tolerance = 0.05,
            ),
        )
        assertTrue(
            bifurcation.isBifurcation(
                point = doubleArrayOf(1.0, 1.0),
                tolerance = 0.05,
            ),
        )
    }

    @Test
    fun pointLimitAndInvalidInputReturnStructuredErrors() {
        val limited = circlePlot(maximumPointCount = 10).plot()
        val limitedError =
            assertIs<GMResult.Err<ImplicitPlotError>>(limited).error
        assertIs<ImplicitPlotError.PointLimitExceeded>(limitedError)

        val invalid = ImplicitPlot(
            boundingBox = doubleArrayOf(-1.0, 1.0),
            config = referenceConfig(),
            f = { x, y -> x + y },
        ).plot()
        assertEquals(
            ImplicitPlotError.InvalidBoundingBox(size = 2),
            assertIs<GMResult.Err<ImplicitPlotError>>(invalid).error,
        )
    }

    private fun circlePlot(
        maximumPointCount: Int = Int.MAX_VALUE,
    ): ImplicitPlot =
        ImplicitPlot(
            boundingBox = doubleArrayOf(-2.0, 2.0, 2.0, -2.0),
            config = referenceConfig(),
            f = { x, y -> x * x + y * y - 1.0 },
            dfx = { x, _ -> 2.0 * x },
            dfy = { _, y -> 2.0 * y },
            maximumPointCount = maximumPointCount,
        )

    private fun referenceConfig(): ImplicitPlotConfig =
        ImplicitPlotConfig(
            resolutionOuter = 5.0,
            resolutionInner = 5.0,
            maxSteps = 64.0,
            hInitial = 0.1,
            hMax = 0.5,
            unitX = 10.0,
            unitY = 10.0,
        )

    private fun assertClose(
        expected: Double,
        actual: Double,
        tolerance: Double = 1.0e-10,
    ) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "Expected $expected, got $actual",
        )
    }
}
