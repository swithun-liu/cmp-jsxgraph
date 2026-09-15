package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CoordsElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NumericsRegressionTest {
    private val board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
    )

    @Test
    fun constantLinearAndQuadraticFitsMatchOfficialReferenceValues() {
        val x = doubleArrayOf(-2.0, -1.0, 0.0, 1.0, 2.0)
        val y = doubleArrayOf(5.0, 2.0, 1.0, 2.0, 5.0)

        val constant = polynomial(Numerics.regressionPolynomial(0.0, x, y))
        assertEquals(3.0, value(constant(-3.0)))
        assertEquals(3.0, value(constant(3.0)))
        assertEquals("(3.00)", constant.getTerm())

        val linear = polynomial(Numerics.regressionPolynomial(1.0, x, y))
        assertEquals(3.0, value(linear(-1.5)))
        assertEquals(3.0, value(linear(0.5)))
        assertEquals("(0.00)*x + (3.00)", linear.getTerm())

        val quadratic = polynomial(Numerics.regressionPolynomial(2.0, x, y))
        assertEquals(10.0, value(quadratic(-3.0)), absoluteTolerance = 1.0e-14)
        assertEquals(3.25, value(quadratic(-1.5)), absoluteTolerance = 1.0e-14)
        assertEquals(1.0, value(quadratic(0.0)), absoluteTolerance = 1.0e-14)
        assertEquals(1.25, value(quadratic(0.5)), absoluteTolerance = 1.0e-14)
        assertEquals("(1.00)*x<sup>2</sup> + (0.00)*x + (1.00)", quadratic.getTerm())
    }

    @Test
    fun noisyQuadraticMatchesOfficialReferenceValueAndTerm() {
        val polynomial = polynomial(
            Numerics.regressionPolynomial(
                degree = 2.0,
                dataX = doubleArrayOf(-2.0, -1.0, 0.0, 1.0, 2.0),
                dataY = doubleArrayOf(4.8, 2.2, 0.9, 2.1, 5.2),
            ),
        )

        assertEquals(
            1.3375000000000001,
            value(polynomial(0.5)),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(
            "(0.993)*x<sup>2</sup> + (0.0700)*x + (1.05)",
            polynomial.getTerm(),
        )
    }

    @Test
    fun dynamicDegreeAndPointsPreserveOfficialCacheTiming() {
        var degree = 1.0
        var degreeEvaluationCount = 0
        val points = points(
            -2.0 to 5.0,
            -1.0 to 2.0,
            0.0 to 1.0,
            1.0 to 2.0,
            2.0 to 5.0,
        )
        val polynomial = polynomial(
            Numerics.regressionPolynomial(
                degree = {
                    degreeEvaluationCount += 1
                    degree
                },
                points = points,
            ),
        )

        assertEquals(3.0, value(polynomial(0.5)))
        assertEquals(1, degreeEvaluationCount)
        assertEquals("(0.00)*x + (3.00)", polynomial.getTerm())

        points[0].moveTo(-3.0, 10.0)
        degree = 2.0
        assertIs<GMResult.Err<NumericsError.RegressionCoefficientsUnavailable>>(
            polynomial(0.5, suspendedUpdate = true),
        )
        assertEquals(2, degreeEvaluationCount)
        assertEquals("(0.00)*x + (3.00)", polynomial.getTerm())

        assertEquals(1.25, value(polynomial(0.5)), absoluteTolerance = 1.0e-14)
        assertEquals(3, degreeEvaluationCount)
        assertEquals(
            "(1.00)*x<sup>2</sup> + (0.00)*x + (1.00)",
            polynomial.getTerm(),
        )
    }

    @Test
    fun functionBackedDataIsReadOnEveryRefresh() {
        val x = doubleArrayOf(0.0, 1.0, 2.0)
        val y = doubleArrayOf(1.0, 3.0, 5.0)
        val polynomial = polynomial(
            Numerics.regressionPolynomial(
                degree = { 1.0 },
                dataX = x.indices.map { index -> { x[index] } },
                dataY = y.indices.map { index -> { y[index] } },
            ),
        )

        assertEquals(9.0, value(polynomial(4.0)), absoluteTolerance = 1.0e-14)
        assertEquals("(2.00)*x + (1.00)", polynomial.getTerm())

        y[2] = 8.0
        assertEquals(
            7.5,
            value(polynomial(2.0)),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals("(3.50)*x + (0.500)", polynomial.getTerm())
    }

    @Test
    fun invalidInputsAndSingularSystemsReturnExplicitErrors() {
        assertIs<GMResult.Err<NumericsError.InvalidRegressionData>>(
            Numerics.regressionPolynomial(
                degree = 1.0,
                dataX = doubleArrayOf(),
                dataY = doubleArrayOf(),
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidRegressionData>>(
            Numerics.regressionPolynomial(
                degree = 1.0,
                dataX = doubleArrayOf(0.0, 1.0, 2.0),
                dataY = doubleArrayOf(1.0, 2.0),
            ),
        )

        val negativeDegree = polynomial(
            Numerics.regressionPolynomial(
                degree = -1.0,
                dataX = doubleArrayOf(0.0, 1.0),
                dataY = doubleArrayOf(1.0, 2.0),
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidRegressionDegree>>(
            negativeDegree(0.5),
        )

        val singular = polynomial(
            Numerics.regressionPolynomial(
                degree = 2.0,
                dataX = doubleArrayOf(1.0, 1.0, 1.0),
                dataY = doubleArrayOf(2.0, 3.0, 4.0),
            ),
        )
        assertIs<GMResult.Err<NumericsError.SingularMatrix>>(singular(0.5))

        val excessiveDegree = polynomial(
            Numerics.regressionPolynomial(
                degree = 3.0,
                dataX = doubleArrayOf(0.0, 1.0),
                dataY = doubleArrayOf(1.0, 2.0),
            ),
        )
        assertIs<GMResult.Err<NumericsError.SingularMatrix>>(excessiveDegree(0.5))
    }

    @Test
    fun suspendedEvaluationBeforeFirstFitReturnsStateError() {
        val polynomial = polynomial(
            Numerics.regressionPolynomial(
                degree = 2.0,
                dataX = doubleArrayOf(-1.0, 0.0, 1.0),
                dataY = doubleArrayOf(1.0, 0.0, 1.0),
            ),
        )

        assertIs<GMResult.Err<NumericsError.RegressionCoefficientsUnavailable>>(
            polynomial(0.5, suspendedUpdate = true),
        )
        assertEquals("", polynomial.getTerm())
    }

    private fun points(vararg coordinates: Pair<Double, Double>): List<CoordsElement> =
        coordinates.map { (x, y) ->
            CoordsElement(
                board = board,
                coordinates = doubleArrayOf(x, y),
            )
        }

    private fun CoordsElement.moveTo(
        x: Double,
        y: Double,
    ) {
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(x, y),
        )
    }

    private fun polynomial(
        result: GMResult<RegressionPolynomial, NumericsError>,
    ): RegressionPolynomial =
        assertIs<GMResult.Ok<RegressionPolynomial>>(result).value

    private fun value(
        result: GMResult<Double, NumericsError>,
    ): Double = assertIs<GMResult.Ok<Double>>(result).value
}
