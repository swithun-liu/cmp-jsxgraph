package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CoordsElement
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NumericsInterpolationTest {
    private val board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
    )

    @Test
    fun nevilleMatchesOfficialReferenceValues() {
        val points = points(
            0.0 to -2.0,
            -1.5 to 5.0,
            1.0 to 4.0,
            3.0 to 3.0,
        )
        val interpolation = Numerics.Neville(points)

        assertEquals(0.0, interpolation.start)
        assertEquals(3.0, interpolation.end())
        assertEquals(-1.53125, interpolation.x(0.5), absoluteTolerance = 1.0e-14)
        assertEquals(
            2.9999999999999996,
            interpolation.y(0.5, suspendedUpdate = true),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(-1.5, interpolation.x(1.0))
        assertEquals(5.0, interpolation.y(1.0, suspendedUpdate = true))
        assertEquals(0.0, interpolation.x(4.0), absoluteTolerance = 1.0e-14)
        assertEquals(10.0, interpolation.y(4.0, suspendedUpdate = true))
    }

    @Test
    fun nevilleReadsMovedPointsAndPreservesSuspendedUpdateBehavior() {
        val points = points(
            0.0 to -2.0,
            -1.5 to 5.0,
            1.0 to 4.0,
            3.0 to 3.0,
        )
        val interpolation = Numerics.Neville(points)

        points[1].moveTo(-2.0, 7.0)

        assertEquals(-2.0, interpolation.x(0.5), absoluteTolerance = 1.0e-14)
        assertEquals(
            4.875,
            interpolation.y(0.5, suspendedUpdate = true),
            absoluteTolerance = 1.0e-14,
        )

        val uninitialized = Numerics.Neville(points(2.0 to 3.0, 4.0 to 5.0))
        assertTrue(uninitialized.x(0.25, suspendedUpdate = true).isNaN())
        assertEquals(2.0, uninitialized.x(0.0, suspendedUpdate = true))
    }

    @Test
    fun nevilleHandlesEmptyAndSinglePointInputsLikeOfficial() {
        val empty = Numerics.Neville(emptyList())
        assertTrue(empty.x(1.0).isNaN())
        assertTrue(empty.y(1.0, suspendedUpdate = true).isNaN())
        assertEquals(-1.0, empty.end())

        val single = Numerics.Neville(points(2.0 to 3.0))
        assertEquals(2.0, single.x(0.0))
        assertEquals(3.0, single.y(4.0, suspendedUpdate = true))
        assertEquals(0.0, single.end())
    }

    @Test
    fun lagrangePolynomialMatchesOfficialReferenceValues() {
        val polynomial = Numerics.lagrangePolynomial(
            points(
                -1.0 to 2.0,
                0.0 to 0.0,
                2.0 to 1.0,
            ),
        )

        assertEquals(5.666666666666667, polynomial(-2.0), absoluteTolerance = 1.0e-14)
        assertEquals(2.0, polynomial(-1.0))
        assertEquals(-0.375, polynomial(0.5))
        assertEquals(
            -0.40625,
            polynomial(0.75, suspendedUpdate = true),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(4.0, polynomial(3.0), absoluteTolerance = 1.0e-14)
        assertContentEquals(
            doubleArrayOf(0.8333333333333333, -1.1666666666666665, 0.0),
            polynomial.getCoefficients(),
        )
        assertEquals(
            "0.8333333333333333 * x^2 - 1.1666666666666665 * x",
            polynomial.getTerm(),
        )
        assertEquals("0.83 * t^2 - 1.17 * t", polynomial.getTerm(2, "t", " * "))
        assertEquals("1t^2 - 1t", polynomial.getTerm(0, "t", ""))
    }

    @Test
    fun lagrangePolynomialReadsMovedPointsForEveryDerivedRepresentation() {
        val points = points(
            -1.0 to 2.0,
            0.0 to 0.0,
            2.0 to 1.0,
        )
        val polynomial = Numerics.lagrangePolynomial(points)

        polynomial(0.5)
        points[2].moveTo(3.0, 4.0)

        assertEquals(
            -0.3333333333333334,
            polynomial(1.0),
            absoluteTolerance = 1.0e-14,
        )
        assertContentEquals(
            doubleArrayOf(0.8333333333333333, -1.1666666666666667, 0.0),
            polynomial.getCoefficients(),
        )
        assertEquals("0.833*z^2 - 1.167*z", polynomial.getTerm(3, "z", "*"))
    }

    @Test
    fun lagrangePolynomialPreservesDegenerateJavaScriptNumberResults() {
        val duplicate = Numerics.lagrangePolynomial(
            points(
                1.0 to 2.0,
                1.0 to 3.0,
            ),
        )

        assertEquals(2.0, duplicate(1.0))
        assertTrue(duplicate(2.0).isNaN())
        assertContentEquals(
            doubleArrayOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY),
            duplicate.getCoefficients(),
        )
        assertEquals("Infinity * x - Infinity", duplicate.getTerm())

        val empty = Numerics.lagrangePolynomial(emptyList())
        assertTrue(empty(1.0).isNaN())
        assertContentEquals(doubleArrayOf(), empty.getCoefficients())
        assertEquals("", empty.getTerm())

        val single = Numerics.lagrangePolynomial(points(2.0 to -3.0))
        assertEquals(-3.0, single(5.0))
        assertContentEquals(doubleArrayOf(-3.0), single.getCoefficients())
        assertEquals("-3", single.getTerm())
    }

    @Test
    fun polynomialTermGenerationMatchesOfficialAndReportsInvalidInputs() {
        assertEquals(
            "(1.235e+4)*q<sup>2</sup> + (-2.000)*q + (0.3333)",
            polynomialTerm(
                Numerics.generatePolynomialTerm(
                    coefficients = doubleArrayOf(1.0 / 3.0, -2.0, 12345.0),
                    degree = 2,
                    variableName = "q",
                    precision = 4,
                ),
            ),
        )
        assertEquals(
            "",
            polynomialTerm(
                Numerics.generatePolynomialTerm(
                    coefficients = doubleArrayOf(),
                    degree = -1,
                    variableName = "x",
                    precision = 3,
                ),
            ),
        )
        assertEquals(
            "(1.00e+3)*x<sup>2</sup> + (9.99)*x + (1.23e-7)",
            polynomialTerm(
                Numerics.generatePolynomialTerm(
                    coefficients = doubleArrayOf(1.23456e-7, 9.995, 999.5),
                    degree = 2,
                    variableName = "x",
                    precision = 3,
                ),
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidPolynomialDegree>>(
            Numerics.generatePolynomialTerm(
                coefficients = doubleArrayOf(1.0),
                degree = 1,
                variableName = "x",
                precision = 3,
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidPolynomialPrecision>>(
            Numerics.generatePolynomialTerm(
                coefficients = doubleArrayOf(1.0),
                degree = 0,
                variableName = "x",
                precision = 0,
            ),
        )
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

    private fun polynomialTerm(
        result: GMResult<String, NumericsError>,
    ): String = assertIs<GMResult.Ok<String>>(result).value
}
