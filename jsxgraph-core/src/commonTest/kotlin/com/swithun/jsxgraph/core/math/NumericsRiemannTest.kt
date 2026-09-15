package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NumericsRiemannTest {
    private val quadratic = { value: Double -> value * value + 1.0 }

    @Test
    fun riemannValueTypesMatchOfficialReferenceValues() {
        assertEquals(2.0, Numerics.riemannValue(-1.0, quadratic, "left", 1.0))
        assertEquals(1.0, Numerics.riemannValue(-1.0, quadratic, "right", 1.0))
        assertEquals(1.25, Numerics.riemannValue(-1.0, quadratic, "middle", 1.0))
        assertEquals(1.0, Numerics.riemannValue(-1.0, quadratic, "lower", 1.0))
        assertEquals(2.0, Numerics.riemannValue(-1.0, quadratic, "upper", 1.0))
        assertEquals(
            1.3333333333333333,
            Numerics.riemannValue(-1.0, quadratic, "simpson", 1.0),
        )
        assertEquals(2.0, Numerics.riemannValue(-1.0, quadratic, "trapezoidal", 1.0))
        assertEquals(2.0, Numerics.riemannValue(-1.0, quadratic, "unknown", 1.0))
    }

    @Test
    fun negativeDeltaSwapsLowerAndUpperLikeOfficial() {
        assertEquals(5.0, Numerics.riemannValue(-1.0, quadratic, "left", -1.0))
        assertEquals(2.0, Numerics.riemannValue(-1.0, quadratic, "right", -1.0))
        assertEquals(3.25, Numerics.riemannValue(-1.0, quadratic, "middle", -1.0))
        assertEquals(5.0, Numerics.riemannValue(-1.0, quadratic, "lower", -1.0))
        assertEquals(2.0, Numerics.riemannValue(-1.0, quadratic, "upper", -1.0))
        assertEquals(
            3.3333333333333335,
            Numerics.riemannValue(-1.0, quadratic, "simpson", -1.0),
        )
        assertEquals(
            2.0,
            Numerics.riemannValue(-1.0, quadratic, "trapezoidal", -1.0),
        )
    }

    @Test
    fun singleFunctionGeometryAndSumsMatchOfficialReferences() {
        val left = result(
            Numerics.riemann(
                upperFunction = quadratic,
                rectangleCount = 2.0,
                type = "left",
                start = -1.0,
                end = 2.0,
            ),
        )
        assertEquals(4.875, left.sum)
        assertContentEquals(
            doubleArrayOf(-1.0, 0.5, 0.5, 2.0, 2.0, 0.5, 0.5, 0.5, -1.0, -1.0),
            left.xCoordinates,
        )
        assertContentEquals(
            doubleArrayOf(2.0, 2.0, 1.25, 1.25, 0.0, 0.0, 1.25, 0.0, 0.0, 2.0),
            left.yCoordinates,
        )

        val trapezoidal = result(
            Numerics.riemann(
                upperFunction = quadratic,
                rectangleCount = 4.8,
                type = "trapezoidal",
                start = -1.0,
                end = 2.0,
            ),
        )
        assertEquals(6.28125, trapezoidal.sum)
        assertEquals(20, trapezoidal.xCoordinates.size)
        assertEquals(20, trapezoidal.yCoordinates.size)

        val lower = result(
            Numerics.riemann(
                upperFunction = quadratic,
                rectangleCount = 4.0,
                type = "lower",
                start = -1.0,
                end = 2.0,
            ),
        )
        assertEquals(4.4062546875, lower.sum, absoluteTolerance = 1.0e-13)
    }

    @Test
    fun simpsonGeometryAndBetweenFunctionsMatchOfficialReferences() {
        val single = result(
            Numerics.riemann(
                upperFunction = quadratic,
                rectangleCount = 2.0,
                type = "simpson",
                start = -1.0,
                end = 2.0,
            ),
        )
        assertEquals(6.0, single.sum, absoluteTolerance = 1.0e-14)
        assertEquals(68, single.xCoordinates.size)
        assertEquals(68, single.yCoordinates.size)
        assertContentEquals(
            doubleArrayOf(-1.0, -0.95, -0.8999999999999999),
            single.xCoordinates.copyOfRange(0, 3),
        )
        assertContentEquals(
            doubleArrayOf(2.0, 1.9024999999999999, 1.81),
            single.yCoordinates.copyOfRange(0, 3),
        )

        val between = result(
            Numerics.riemann(
                upperFunction = quadratic,
                lowerFunction = { value -> value * 0.5 },
                rectangleCount = 3.0,
                type = "simpson",
                start = -1.0,
                end = 2.0,
            ),
        )
        assertEquals(5.25, between.sum, absoluteTolerance = 1.0e-14)
        assertEquals(189, between.xCoordinates.size)
        assertEquals(189, between.yCoordinates.size)
        assertContentEquals(
            doubleArrayOf(
                -0.36666666666666664,
                -0.38333333333333336,
                -0.4,
                -0.4166666666666667,
                -0.43333333333333335,
                -0.45,
                -0.4666666666666667,
                -0.48333333333333334,
                -0.5,
                2.0,
            ),
            between.yCoordinates.copyOfRange(
                between.yCoordinates.size - 10,
                between.yCoordinates.size,
            ),
        )
    }

    @Test
    fun betweenFunctionAndUnknownTypeSumsMatchOfficialReferences() {
        val lowerFunction = { value: Double -> value * 0.5 }
        val expectations = mapOf(
            "left" to 5.0,
            "right" to 6.5,
            "middle" to 5.0,
            "lower" to 2.5,
            "upper" to 9.0,
            "trapezoidal" to 5.75,
            "unknown" to 5.0,
        )

        for ((type, expected) in expectations) {
            val riemann = result(
                Numerics.riemann(
                    upperFunction = quadratic,
                    lowerFunction = lowerFunction,
                    rectangleCount = 3.0,
                    type = type,
                    start = -1.0,
                    end = 2.0,
                ),
            )
            assertEquals(expected, riemann.sum, absoluteTolerance = 1.0e-13)
            assertEquals(15, riemann.xCoordinates.size)
            assertEquals(15, riemann.yCoordinates.size)
        }
    }

    @Test
    fun injectedRandomSourceMakesRandomRiemannDeterministic() {
        val riemann = result(
            Numerics.riemann(
                upperFunction = quadratic,
                rectangleCount = 2.0,
                type = "random",
                start = 0.0,
                end = 2.0,
                random = RandomSource { 0.25 },
            ),
        )

        assertEquals(3.625, riemann.sum)
        assertContentEquals(
            doubleArrayOf(1.0625, 1.0625, 2.5625, 2.5625, 0.0, 0.0, 2.5625, 0.0, 0.0, 1.0625),
            riemann.yCoordinates,
        )
    }

    @Test
    fun invalidInputsAreExplicitAndZeroWidthLowerTerminates() {
        assertIs<GMResult.Err<NumericsError.InvalidRiemannRectangleCount>>(
            Numerics.riemann(
                upperFunction = quadratic,
                rectangleCount = Double.POSITIVE_INFINITY,
                type = "left",
                start = 0.0,
                end = 1.0,
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidRiemannInterval>>(
            Numerics.riemann(
                upperFunction = quadratic,
                rectangleCount = 2.0,
                type = "left",
                start = Double.NaN,
                end = 1.0,
            ),
        )

        val empty = result(
            Numerics.riemann(
                upperFunction = quadratic,
                rectangleCount = -2.0,
                type = "left",
                start = 0.0,
                end = 1.0,
            ),
        )
        assertContentEquals(doubleArrayOf(), empty.xCoordinates)
        assertContentEquals(doubleArrayOf(), empty.yCoordinates)
        assertEquals(0.0, empty.sum)

        assertEquals(5.0, Numerics.riemannValue(2.0, quadratic, "lower", 0.0))
        assertEquals(5.0, Numerics.riemannValue(2.0, quadratic, "upper", 0.0))
    }

    @Test
    fun deprecatedRiemannSumAliasReturnsTheSameSum() {
        assertEquals(
            6.0,
            sum(
                Numerics.riemannsum(
                    upperFunction = quadratic,
                    rectangleCount = 4.0,
                    type = "simpson",
                    start = -1.0,
                    end = 2.0,
                ),
            ),
            absoluteTolerance = 1.0e-14,
        )
    }

    private fun result(
        result: GMResult<RiemannResult, NumericsError>,
    ): RiemannResult = assertIs<GMResult.Ok<RiemannResult>>(result).value

    private fun sum(
        result: GMResult<Double, NumericsError>,
    ): Double = assertIs<GMResult.Ok<Double>>(result).value
}
