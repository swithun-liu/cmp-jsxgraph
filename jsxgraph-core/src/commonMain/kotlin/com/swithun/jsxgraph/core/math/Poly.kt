/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/poly.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.utils.JsNumberFormat

sealed interface PolyError {
    val message: String

    data object MissingMonomialRing : PolyError {
        override val message =
            "JSXGraph error: In JXG.Math.Poly.monomial " +
                "missing parameter 'ring'."
    }

    data object MissingPolynomialRing : PolyError {
        override val message =
            "JSXGraph error: In JXG.Math.Poly.polynomial " +
                "missing parameter 'ring'."
    }

    data class UnsupportedStringParsing(
        val source: String,
        override val message: String =
            "JSXGraph error: JXG.Math.Poly.Polynomial string parsing " +
                "is not implemented by JSXGraph 1.13.3.",
    ) : PolyError

    data class RingMismatch(
        val operation: PolyOperation,
        override val message: String =
            "JSXGraph error: In JXG.Math.Poly.polynomial." +
                "${operation.upstreamName} either summand is undefined " +
                "or rings don't match.",
    ) : PolyError
}

enum class PolyOperation(
    internal val upstreamName: String,
) {
    ADD("add"),
    SUB("sub"),
}

object Poly {
    // JSXGraph 1.13.3: src/math/poly.js -> Mat.Poly.Ring.
    class Ring(
        val vars: List<String>,
    )

    sealed interface Operand {
        val ring: Ring
    }

    // JSXGraph 1.13.3: src/math/poly.js -> Mat.Poly.Monomial.
    class Monomial private constructor(
        override val ring: Ring,
        coefficient: Double,
        exponents: List<Double>,
    ) : Operand {
        var coefficient: Double = jsTruthyCoefficient(coefficient)
        val exponents: MutableList<Double> = normalizeExponents(
            exponents = exponents,
            variableCount = ring.vars.size,
        )

        // JSXGraph 1.13.3: src/math/poly.js -> Monomial.copy.
        fun copy(): Monomial = Monomial(
            ring = ring,
            coefficient = coefficient,
            exponents = exponents,
        )

        // JSXGraph 1.13.3: src/math/poly.js -> Monomial.print.
        fun print(): String {
            val variableFactors = ring.vars.indices.joinToString("*") { index ->
                ring.vars[index] +
                    "^" +
                    JsNumberFormat.compact(exponents[index])
            }
            return JsNumberFormat.compact(coefficient) + "*" + variableFactors
        }

        companion object {
            fun create(
                ring: Ring?,
                coefficient: Double,
                exponents: List<Double>? = null,
            ): GMResult<Monomial, PolyError> {
                if (ring == null) {
                    return GMResult.Err(PolyError.MissingMonomialRing)
                }
                return GMResult.Ok(
                    Monomial(
                        ring = ring,
                        coefficient = coefficient,
                        exponents = exponents.orEmpty(),
                    ),
                )
            }
        }
    }

    // JSXGraph 1.13.3: src/math/poly.js -> Mat.Poly.Polynomial.
    class Polynomial private constructor(
        override val ring: Ring,
    ) : Operand {
        val monomials: MutableList<Monomial> = mutableListOf()

        // JSXGraph 1.13.3: src/math/poly.js -> Polynomial.findSignature.
        fun findSignature(signature: List<Double>): Int {
            for (index in monomials.indices) {
                if (sameSignature(monomials[index].exponents, signature)) {
                    return index
                }
            }
            return -1
        }

        // JSXGraph 1.13.3: src/math/poly.js ->
        // Polynomial.addSubMonomial.
        fun addSubMonomial(
            monomial: Monomial,
            factor: Double,
        ) {
            val index = findSignature(monomial.exponents)
            if (index > -1) {
                monomials[index].coefficient +=
                    factor * monomial.coefficient
            } else {
                monomial.coefficient *= factor
                monomials += monomial
            }
        }

        // JSXGraph 1.13.3: src/math/poly.js -> Polynomial.add.
        fun add(operand: Operand?): GMResult<Unit, PolyError> =
            addOrSub(operand, factor = 1.0, PolyOperation.ADD)

        // JSXGraph 1.13.3: src/math/poly.js -> Polynomial.sub.
        fun sub(operand: Operand?): GMResult<Unit, PolyError> =
            addOrSub(operand, factor = -1.0, PolyOperation.SUB)

        // JSXGraph 1.13.3: src/math/poly.js -> Polynomial.copy.
        fun copy(): Polynomial {
            val polynomial = Polynomial(ring)
            for (monomial in monomials) {
                polynomial.monomials += monomial.copy()
            }
            return polynomial
        }

        // JSXGraph 1.13.3: src/math/poly.js -> Polynomial.print.
        fun print(): String =
            monomials.joinToString("+") { monomial ->
                "(${monomial.print()})"
            }

        private fun addOrSub(
            operand: Operand?,
            factor: Double,
            operation: PolyOperation,
        ): GMResult<Unit, PolyError> {
            if (operand == null || operand.ring !== ring) {
                return GMResult.Err(
                    PolyError.RingMismatch(operation),
                )
            }

            when (operand) {
                is Monomial -> addSubMonomial(operand, factor)
                is Polynomial -> {
                    var index = 0
                    while (index < operand.monomials.size) {
                        addSubMonomial(operand.monomials[index], factor)
                        index += 1
                    }
                }
            }
            return GMResult.Ok(Unit)
        }

        companion object {
            fun create(
                ring: Ring?,
                source: String? = null,
            ): GMResult<Polynomial, PolyError> {
                if (ring == null) {
                    return GMResult.Err(PolyError.MissingPolynomialRing)
                }
                if (source != null) {
                    return GMResult.Err(
                        PolyError.UnsupportedStringParsing(source),
                    )
                }
                return GMResult.Ok(Polynomial(ring))
            }
        }
    }

    private fun jsTruthyCoefficient(coefficient: Double): Double =
        if (coefficient == 0.0 || coefficient.isNaN()) {
            0.0
        } else {
            coefficient
        }

    private fun normalizeExponents(
        exponents: List<Double>,
        variableCount: Int,
    ): MutableList<Double> {
        val normalized = exponents.take(variableCount).toMutableList()
        while (normalized.size < variableCount) {
            normalized += 0.0
        }
        return normalized
    }

    private fun sameSignature(
        first: List<Double>,
        second: List<Double>,
    ): Boolean {
        if (first === second) {
            return true
        }
        if (first.size != second.size) {
            return false
        }
        for (index in first.indices) {
            if (first[index] != second[index]) {
                return false
            }
        }
        return true
    }
}
