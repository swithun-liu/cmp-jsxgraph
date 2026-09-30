/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class PolyTest {
    @Test
    fun monomialNormalizationAndPrintingMatchOfficialFixture() {
        val ring = Poly.Ring(listOf("x", "y", "z"))
        val monomial = monomial(ring, 4.0, listOf(1.0, 2.0))
        val nanCoefficient = monomial(
            ring,
            Double.NaN,
            listOf(3.0, 4.0, 5.0, 6.0),
        )

        assertEquals(listOf("x", "y", "z"), ring.vars)
        assertEquals(4.0, monomial.coefficient)
        assertEquals(listOf(1.0, 2.0, 0.0), monomial.exponents)
        assertEquals("4*x^1*y^2*z^0", monomial.print())
        assertEquals(0.0, nanCoefficient.coefficient)
        assertEquals(
            listOf(3.0, 4.0, 5.0),
            nanCoefficient.exponents,
        )
        assertEquals("0*x^3*y^4*z^5", nanCoefficient.print())
    }

    @Test
    fun addSubtractCopyAndSignatureMatchOfficialFixture() {
        val ring = Poly.Ring(listOf("x", "y", "z"))
        val first = monomial(ring, 4.0, listOf(1.0, 2.0))
        val polynomial = polynomial(ring)

        assertIs<GMResult.Ok<Unit>>(polynomial.add(first))
        assertIs<GMResult.Ok<Unit>>(
            polynomial.add(
                monomial(ring, 3.0, listOf(1.0, 2.0)),
            ),
        )
        assertEquals(7.0, polynomial.monomials[0].coefficient)

        val subtracted = monomial(
            ring,
            5.0,
            listOf(0.0, 1.0, 0.0),
        )
        assertIs<GMResult.Ok<Unit>>(polynomial.sub(subtracted))
        assertEquals(-5.0, subtracted.coefficient)
        assertSame(subtracted, polynomial.monomials[1])

        val copy = polynomial.copy()
        assertNotSame(polynomial.monomials[0], copy.monomials[0])
        assertNotSame(
            polynomial.monomials[0].exponents,
            copy.monomials[0].exponents,
        )
        assertEquals(
            "(7*x^1*y^2*z^0)+(-5*x^0*y^1*z^0)",
            copy.print(),
        )

        first.coefficient = 99.0
        assertEquals(
            "(99*x^1*y^2*z^0)+(-5*x^0*y^1*z^0)",
            polynomial.print(),
        )
        assertEquals(
            "(7*x^1*y^2*z^0)+(-5*x^0*y^1*z^0)",
            copy.print(),
        )
        assertEquals(
            1,
            polynomial.findSignature(listOf(0.0, 1.0, 0.0)),
        )
        assertEquals(
            -1,
            polynomial.findSignature(listOf(0.0, 0.0, 1.0)),
        )
    }

    @Test
    fun ringAndStringFailuresAreStructured() {
        assertIs<GMResult.Err<PolyError.MissingMonomialRing>>(
            Poly.Monomial.create(
                ring = null,
                coefficient = 1.0,
            ),
        )
        assertIs<GMResult.Err<PolyError.MissingPolynomialRing>>(
            Poly.Polynomial.create(ring = null),
        )

        val ring = Poly.Ring(listOf("x", "y", "z"))
        val unsupported = assertIs<
            GMResult.Err<PolyError.UnsupportedStringParsing>,
        >(Poly.Polynomial.create(ring, source = "x"))
        assertEquals("x", unsupported.error.source)

        val polynomial = polynomial(ring)
        val mismatch = assertIs<
            GMResult.Err<PolyError.RingMismatch>,
        >(
            polynomial.add(
                monomial(
                    Poly.Ring(listOf("x", "y", "z")),
                    1.0,
                    listOf(0.0, 0.0, 0.0),
                ),
            ),
        )
        assertEquals(PolyOperation.ADD, mismatch.error.operation)
        assertEquals(
            "JSXGraph error: In JXG.Math.Poly.polynomial.add " +
                "either summand is undefined or rings don't match.",
            mismatch.error.message,
        )

        val missingOperand = assertIs<
            GMResult.Err<PolyError.RingMismatch>,
        >(polynomial.sub(null))
        assertEquals(PolyOperation.SUB, missingOperand.error.operation)
        assertEquals(
            "JSXGraph error: In JXG.Math.Poly.polynomial.sub " +
                "either summand is undefined or rings don't match.",
            missingOperand.error.message,
        )
    }

    private fun monomial(
        ring: Poly.Ring,
        coefficient: Double,
        exponents: List<Double>,
    ): Poly.Monomial =
        assertIs<GMResult.Ok<Poly.Monomial>>(
            Poly.Monomial.create(ring, coefficient, exponents),
        ).value

    private fun polynomial(ring: Poly.Ring): Poly.Polynomial =
        assertIs<GMResult.Ok<Poly.Polynomial>>(
            Poly.Polynomial.create(ring),
        ).value
}
