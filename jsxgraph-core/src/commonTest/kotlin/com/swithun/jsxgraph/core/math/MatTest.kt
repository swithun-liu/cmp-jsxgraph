package com.swithun.jsxgraph.core.math

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatTest {
    @Test
    fun scalarHelpersMatchUpstreamSemantics() {
        assertEquals(0.2, Mat.relDif(4.0, 5.0))
        assertEquals(0.0, Mat.relDif(0.0, 0.0))
        assertEquals(4.0, Mat.mod(-1.0, 5.0))
        assertEquals(-4.0, Mat.mod(1.0, -5.0))
        assertEquals(3.0, Mat.wrap(13.0, 0.0, 10.0))
        assertEquals(2.0, Mat.clamp(2.0, 1.0, 3.0))
        assertEquals(1.0, Mat.clamp(-2.0, 1.0, 3.0))
        assertEquals(10.0, Mat.wrapAndClamp(355.0, 10.0, 20.0, 360.0))
        assertEquals(20.0, Mat.wrapAndClamp(185.0, 10.0, 20.0, 360.0))
    }

    @Test
    fun vectorAndMatrixApplyJavaScriptFalsyDefaults() {
        assertContentEquals(doubleArrayOf(0.0, 0.0, 0.0), Mat.vector(3, Double.NaN))
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(7.0, 7.0),
                doubleArrayOf(7.0, 7.0),
            ),
            actual = Mat.matrix(2, m = 0, init = 7.0),
        )
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0),
            ),
            actual = Mat.identity(2, 3),
        )
    }

    @Test
    fun projectionMatricesMatchOfficialReferenceValues() {
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(0.5, 0.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 0.0, -1.2222222222222223, -2.2222222222222223),
                doubleArrayOf(0.0, 0.0, -1.0, 0.0),
            ),
            actual = Mat.frustum(
                left = -2.0,
                right = 2.0,
                bottom = -1.0,
                top = 1.0,
                near = 1.0,
                far = 10.0,
            ),
        )
    }

    @Test
    fun matrixAndVectorProductsMatchOfficialReferenceValues() {
        val matrix = arrayOf(
            doubleArrayOf(2.0, 1.0),
            doubleArrayOf(2.0, 3.0),
        )

        assertContentEquals(
            doubleArrayOf(13.0, 23.0),
            Mat.matVecMult(matrix, doubleArrayOf(4.0, 5.0)),
        )
        assertContentEquals(
            doubleArrayOf(18.0, 19.0),
            Mat.vecMatMult(doubleArrayOf(4.0, 5.0), matrix),
        )
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(19.0, 22.0),
                doubleArrayOf(43.0, 50.0),
            ),
            actual = Mat.matMatMult(
                first = arrayOf(
                    doubleArrayOf(1.0, 2.0),
                    doubleArrayOf(3.0, 4.0),
                ),
                second = arrayOf(
                    doubleArrayOf(5.0, 6.0),
                    doubleArrayOf(7.0, 8.0),
                ),
            ),
        )
    }

    @Test
    fun matrixOperationsReturnNewMatrices() {
        val matrix = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0),
        )

        assertMatrixEquals(
            arrayOf(doubleArrayOf(2.0, 4.0), doubleArrayOf(6.0, 8.0)),
            Mat.matNumberMult(matrix, 2.0),
        )
        assertMatrixEquals(
            arrayOf(doubleArrayOf(2.0, 4.0), doubleArrayOf(6.0, 8.0)),
            Mat.matMatAdd(matrix, matrix),
        )
        assertMatrixEquals(
            arrayOf(doubleArrayOf(1.0, 3.0), doubleArrayOf(2.0, 4.0)),
            Mat.transpose(matrix),
        )
        assertEquals(5.0, Mat.trace(matrix))
        assertTrue(Mat.trace(arrayOf(doubleArrayOf(1.0, 2.0))).isNaN())
    }

    @Test
    fun inverseMatchesOfficialReferenceAndSingularMatrixReturnsEmpty() {
        val inverse = Mat.inverse(
            arrayOf(
                doubleArrayOf(4.0, 7.0),
                doubleArrayOf(2.0, 6.0),
            ),
        )

        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(0.6, -0.7),
                doubleArrayOf(-0.2, 0.4),
            ),
            actual = inverse,
            tolerance = 1e-12,
        )
        assertTrue(
            Mat.inverse(
                arrayOf(
                    doubleArrayOf(1.0, 2.0),
                    doubleArrayOf(2.0, 4.0),
                ),
            ).isEmpty(),
        )
    }

    @Test
    fun vectorOperationsMatchOfficialReferenceValues() {
        assertEquals(
            32.0,
            Mat.innerProduct(doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(4.0, 5.0, 6.0)),
        )
        assertContentEquals(
            doubleArrayOf(-3.0, 6.0, -3.0),
            Mat.crossProduct(doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(4.0, 5.0, 6.0)),
        )
        assertEquals(5.0, Mat.norm(doubleArrayOf(3.0, 4.0)))
        assertContentEquals(
            doubleArrayOf(5.0, 8.0),
            Mat.axpy(2.0, doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)),
        )
        assertEquals(13.0, Mat.hypot(3.0, 4.0, 12.0))
    }

    @Test
    fun combinatoricsMatchOfficialReferenceValues() {
        assertEquals(1.0, Mat.factorial(0.0))
        assertEquals(120.0, Mat.factorial(5.9))
        assertTrue(Mat.factorial(-1.0).isNaN())
        assertEquals(10.0, Mat.binomial(5.0, 2.0))
        assertEquals(10.0, Mat.binomial(5.4, 2.4))
        assertTrue(Mat.binomial(2.0, 3.0).isNaN())
    }

    @Test
    fun rootsPowersAndLogarithmsMatchOfficialReferenceValues() {
        assertEquals(2.0, Mat.nthroot(16.0, 4.0), absoluteTolerance = 1e-12)
        assertEquals(-3.0, Mat.nthroot(-27.0, 3.0), absoluteTolerance = 1e-12)
        assertTrue(Mat.nthroot(-4.0, 2.0).isNaN())
        assertEquals(-2.0, Mat.cbrt(-8.0), absoluteTolerance = 1e-12)
        assertEquals(0.0, Mat.pow(0.0, -1.0))
        assertEquals(-512.0, Mat.pow(-8.0, 3.0))
        assertTrue(Mat.pow(-8.0, 1.0 / 3.0).isNaN())
        assertEquals(-2.0, Mat.ratpow(-8.0, 2.0, 6.0), absoluteTolerance = 1e-12)
        assertEquals(2.0, Mat.log10(100.0), absoluteTolerance = 1e-12)
        assertEquals(3.0, Mat.log2(8.0), absoluteTolerance = 1e-12)
        assertEquals(3.0, Mat.log(8.0, 2.0), absoluteTolerance = 1e-12)
        assertTrue(Mat.log(8.0, 1.0).isNaN())
    }

    @Test
    fun scalarUtilitiesMatchOfficialReferenceValues() {
        assertEquals(1.0, Mat.cot(kotlin.math.PI / 4.0), absoluteTolerance = 1e-12)
        assertTrue(Mat.cot(0.0).isNaN())
        assertEquals(kotlin.math.PI / 4.0, Mat.acot(1.0), absoluteTolerance = 1e-12)
        assertEquals(-kotlin.math.PI / 4.0, Mat.acot(-1.0), absoluteTolerance = 1e-12)
        assertEquals((-0.0).toBits(), Mat.sign(-0.0).toBits())
        assertEquals(-1.0, Mat.sign(-3.0))
        assertEquals(1.0, Mat.sign(3.0))
        assertTrue(Mat.sign(Double.NaN).isNaN())
        assertEquals(1024.0, Mat.squampow(2.0, 10.0))
        assertEquals(0.125, Mat.squampow(2.0, -3.0))
        assertEquals(2.0, Mat.squampow(4.0, 0.5))
        assertEquals(6.0, Mat.gcd(54.0, 24.0))
        assertEquals(-24.0, Mat.lcm(-6.0, 8.0))
        assertEquals(0.0, Mat.lcm(0.0, 8.0))
        assertEquals(2.5, Mat.roundToStep(2.74, step = 0.5))
        assertEquals(2.5, Mat.roundToStep(2.74, step = 0.5, minimum = 1.0, maximum = 2.6))
        assertEquals(0.0, Mat.roundToStep(-0.25, step = 0.5))
        assertEquals(0.0, Mat.hstep(-1.0))
        assertEquals(0.5, Mat.hstep(0.0))
        assertEquals(1.0, Mat.hstep(1.0))
        assertEquals(0.5, Mat.hstep(Double.NaN))
    }

    @Test
    fun gammaAndFractionsMatchOfficialReferenceValues() {
        assertEquals(
            1.7724538509055163,
            Mat.gamma(0.5),
            absoluteTolerance = 1e-12,
        )
        assertEquals(24.0000000000003, Mat.gamma(5.0), absoluteTolerance = 1e-12)
        assertEquals(-3.544907701811025, Mat.gamma(-0.5), absoluteTolerance = 1e-12)
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 1.0, 3.0),
            Mat.decToFraction(0.33333333),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 0.0, 1.0),
            Mat.decToFraction(0.0),
        )
        assertContentEquals(
            doubleArrayOf(-1.0, 10.0, 2.0, 3.0),
            Mat.decToFraction(-10.66666666666667),
        )
    }

    @Test
    fun standardFormNormalizationMatchesOfficialReferenceValues() {
        assertContentEquals(
            doubleArrayOf(5.25, 1.5, 2.0, 0.25, 1.0, 2.0, -3.0, -4.0),
            Mat.normalize(doubleArrayOf(2.0, 3.0, 4.0, 0.5, 2.0, 0.0, 0.0, 0.0)),
        )
        assertContentEquals(
            doubleArrayOf(3.0, 1.5, 2.0, 0.5, 0.5, 0.5, -1.5, -2.0),
            Mat.normalize(doubleArrayOf(2.0, 3.0, 4.0, 1.0, 1.0, 0.0, 0.0, 0.0)),
        )
    }

    @Test
    fun webGlConversionAndVietaMatchOfficialReferenceValues() {
        assertContentEquals(
            floatArrayOf(
                1f, 5f, 9f, 13f,
                2f, 6f, 10f, 14f,
                3f, 7f, 11f, 15f,
                4f, 8f, 12f, 16f,
            ),
            Mat.toGL(
                arrayOf(
                    doubleArrayOf(1.0, 2.0, 3.0, 4.0),
                    doubleArrayOf(5.0, 6.0, 7.0, 8.0),
                    doubleArrayOf(9.0, 10.0, 11.0, 12.0),
                    doubleArrayOf(13.0, 14.0, 15.0, 16.0),
                ),
            ),
        )
        assertContentEquals(doubleArrayOf(6.0, 11.0, 6.0), Mat.Vieta(doubleArrayOf(1.0, 2.0, 3.0)))
    }

    @Test
    fun logicalHelpersMatchJavaScriptNumberComparisons() {
        assertTrue(Mat.lt(1.0, 2.0))
        assertTrue(Mat.leq(2.0, 2.0))
        assertTrue(Mat.gt(3.0, 2.0))
        assertTrue(Mat.geq(2.0, 2.0))
        assertTrue(!Mat.eq(Double.NaN, Double.NaN))
        assertTrue(Mat.neq(Double.NaN, Double.NaN))
        assertTrue(!Mat.and(true, false))
        assertTrue(Mat.not(false))
        assertTrue(Mat.or(false, true))
        assertTrue(!Mat.xor(true, true))
        assertTrue(Mat.xor(true, false))
    }

    private fun assertMatrixEquals(
        expected: Array<DoubleArray>,
        actual: Array<DoubleArray>,
        tolerance: Double = 0.0,
    ) {
        assertEquals(expected.size, actual.size)
        for (row in expected.indices) {
            assertEquals(expected[row].size, actual[row].size)
            for (column in expected[row].indices) {
                assertEquals(
                    expected = expected[row][column],
                    actual = actual[row][column],
                    absoluteTolerance = tolerance,
                    message = "Mismatch at [$row][$column]",
                )
            }
        }
    }
}
