package com.swithun.jsxgraph.core.math

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComplexTest {
    @Test
    fun constructorAndFormattingMatchOfficialReferenceValues() {
        assertContentEquals(doubleArrayOf(0.0, 0.0), Complex(Double.NaN, -0.0).toArray())
        assertEquals("3 + 4i", Complex(3.0, 4.0).toString())
        assertEquals("3.00 + 4.00i", Complex(3.0, 4.0).toString(2))
        assertEquals("3 - 4i", Complex(3.0, -4.0).toString())
    }

    @Test
    fun mutatingArithmeticMatchesOfficialReferenceValues() {
        assertContentEquals(
            doubleArrayOf(4.0, -2.0),
            Complex(1.0, 2.0).add(Complex(3.0, -4.0)).toArray(),
        )
        assertContentEquals(doubleArrayOf(4.0, 2.0), Complex(1.0, 2.0).add(3.0).toArray())
        assertContentEquals(
            doubleArrayOf(-2.0, -2.0),
            Complex(1.0, 2.0).sub(Complex(3.0, 4.0)).toArray(),
        )
        assertContentEquals(
            doubleArrayOf(-5.0, 10.0),
            Complex(1.0, 2.0).mult(Complex(3.0, 4.0)).toArray(),
        )
        assertContentEquals(
            doubleArrayOf(0.44, 0.08),
            Complex(1.0, 2.0).div(Complex(3.0, 4.0)).toArray(),
        )
        assertContentEquals(doubleArrayOf(0.5, 1.0), Complex(1.0, 2.0).div(2.0).toArray())
        assertContentEquals(doubleArrayOf(1.0, -2.0), Complex(1.0, 2.0).conj().toArray())
    }

    @Test
    fun divisionByNearZeroMatchesUpstreamInfinitySentinel() {
        val scalarResult = Complex(1.0, 2.0).div(1e-7)
        val complexResult = Complex(1.0, 2.0).div(Complex(1e-13, 1e-13))

        assertTrue(scalarResult.real.isInfinite())
        assertTrue(scalarResult.imaginary.isInfinite())
        assertTrue(complexResult.real.isInfinite())
        assertTrue(complexResult.imaginary.isInfinite())
    }

    @Test
    fun polarValuesMatchOfficialReferenceValues() {
        val value = Complex(3.0, 4.0)

        assertEquals(5.0, value.abs())
        assertEquals(0.9272952180016122, value.angle(), absoluteTolerance = 1e-15)
    }

    @Test
    fun namespaceOperationsDoNotMutateInputs() {
        val first = Complex(1.0, 2.0)
        val second = Complex(3.0, 4.0)

        assertContentEquals(doubleArrayOf(-5.0, 10.0), C.mult(first, second).toArray())
        assertContentEquals(doubleArrayOf(1.0, 2.0), first.toArray())
        assertContentEquals(doubleArrayOf(3.0, 4.0), second.toArray())
        assertContentEquals(doubleArrayOf(4.0, 2.0), C.add(first, 3.0).toArray())
        assertContentEquals(doubleArrayOf(-2.0, -2.0), C.sub(first, second).toArray())
    }
}
