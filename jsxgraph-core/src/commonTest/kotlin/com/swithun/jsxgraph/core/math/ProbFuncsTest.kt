package com.swithun.jsxgraph.core.math

import kotlin.test.Test
import kotlin.test.assertEquals

class ProbFuncsTest {
    @Test
    fun errorFunctionsMatchOfficialReferenceValues() {
        val inputs = doubleArrayOf(-3.0, -1.0, -0.5, 0.0, 0.5, 1.0, 3.0)
        val expectedErf = doubleArrayOf(
            -0.9999779095030015,
            -0.8427007929497148,
            -0.5204998778130465,
            0.0,
            0.5204998778130465,
            0.8427007929497148,
            0.9999779095030014,
        )
        val expectedErfc = doubleArrayOf(
            1.9999779095030015,
            1.8427007929497148,
            1.5204998778130465,
            1.0,
            0.4795001221869535,
            0.15729920705028516,
            0.000022090496998585445,
        )

        for (index in inputs.indices) {
            assertEquals(expectedErf[index], Mat.erf(inputs[index]), absoluteTolerance = 1e-15)
            assertEquals(expectedErfc[index], Mat.erfc(inputs[index]), absoluteTolerance = 1e-15)
        }
    }

    @Test
    fun normalDistributionMatchesOfficialReferenceValues() {
        val inputs = doubleArrayOf(-3.0, -1.0, 0.0, 1.0, 3.0)
        val expected = doubleArrayOf(
            0.0013498980316300946,
            0.15865525393145707,
            0.5,
            0.8413447460685429,
            0.9986501019683699,
        )

        for (index in inputs.indices) {
            assertEquals(expected[index], Mat.ndtr(inputs[index]), absoluteTolerance = 1e-15)
        }
    }

    @Test
    fun inverseNormalDistributionMatchesOfficialReferenceValues() {
        val inputs = doubleArrayOf(1e-10, 0.001, 0.025, 0.5, 0.975, 0.999, 1.0 - 1e-10)
        val expected = doubleArrayOf(
            -6.361340902404056,
            -3.090232306167813,
            -1.9599639845400545,
            0.0,
            1.959963984540054,
            3.090232306167813,
            6.361340889697422,
        )

        for (index in inputs.indices) {
            assertEquals(expected[index], Mat.ndtri(inputs[index]), absoluteTolerance = 1e-13)
        }
        assertEquals(Double.NEGATIVE_INFINITY, Mat.ndtri(0.0))
        assertEquals(Double.POSITIVE_INFINITY, Mat.ndtri(1.0))
    }

    @Test
    fun inverseErrorFunctionMatchesOfficialReferenceValues() {
        val inputs = doubleArrayOf(-0.9, -0.5, 0.0, 0.5, 0.9)
        val expected = doubleArrayOf(
            -1.1630871536766743,
            -0.4769362762044699,
            0.0,
            0.4769362762044699,
            1.1630871536766738,
        )

        for (index in inputs.indices) {
            assertEquals(expected[index], Mat.erfi(inputs[index]), absoluteTolerance = 1e-13)
        }
    }

    @Test
    fun internalPolynomialHelpersMatchOfficialReferenceValues() {
        assertEquals(11.0, ProbFuncs.polevl(2.0, doubleArrayOf(1.0, 2.0, 3.0)))
        assertEquals(19.0, ProbFuncs.p1evl(2.0, doubleArrayOf(1.0, 2.0, 3.0)))
        assertEquals(
            0.00012340980408667956,
            ProbFuncs.expx2(3.0, -1),
            absoluteTolerance = 1e-18,
        )
        assertEquals(
            8103.083927575384,
            ProbFuncs.expx2(3.0, 1),
            absoluteTolerance = 1e-10,
        )
    }
}
