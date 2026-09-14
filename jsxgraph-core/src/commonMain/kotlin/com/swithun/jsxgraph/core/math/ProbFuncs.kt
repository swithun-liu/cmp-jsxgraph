/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/probfuncs.js
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 *
 * The upstream implementation is ported from Cephes Math Library 2.9,
 * Copyright 1984, 1987, 1988, 1992, 2000 by Stephen L. Moshier.
 */
package com.swithun.jsxgraph.core.math

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sqrt

object ProbFuncs {
    const val MAXNUM: Double = 1.701411834604692317316873e38
    const val SQRTH: Double = 7.07106781186547524401e-1
    const val SQRT2: Double = 1.4142135623730950488
    const val MAXLOG: Double = 7.08396418532264106224e2

    private val P = doubleArrayOf(
        2.46196981473530512524e-10,
        5.64189564831068821977e-1,
        7.46321056442269912687,
        4.86371970985681366614e1,
        1.96520832956077098242e2,
        5.26445194995477358631e2,
        9.3452852717195760754e2,
        1.02755188689515710272e3,
        5.57535335369399327526e2,
    )

    private val Q = doubleArrayOf(
        1.32281951154744992508e1,
        8.67072140885989742329e1,
        3.54937778887819891062e2,
        9.75708501743205489753e2,
        1.82390916687909736289e3,
        2.24633760818710981792e3,
        1.65666309194161350182e3,
        5.57535340817727675546e2,
    )

    private val R = doubleArrayOf(
        5.64189583547755073984e-1,
        1.27536670759978104416,
        5.01905042251180477414,
        6.16021097993053585195,
        7.4097426995044893916,
        2.9788666537210024067,
    )

    private val S = doubleArrayOf(
        2.2605286322011727659,
        9.39603524938001434673,
        1.20489539808096656605e1,
        1.70814450747565897222e1,
        9.60896809063285878198,
        3.3690764510008151605,
    )

    private val T = doubleArrayOf(
        9.60497373987051638749,
        9.00260197203842689217e1,
        2.23200534594684319226e3,
        7.00332514112805075473e3,
        5.55923013010394962768e4,
    )

    private val U = doubleArrayOf(
        3.35617141647503099647e1,
        5.21357949780152679795e2,
        4.59432382970980127987e3,
        2.26290000613890934246e4,
        4.92673942608635921086e4,
    )

    private const val M: Double = 128.0
    private const val MINV: Double = 0.0078125

    private const val SQRT_TWO_PI: Double = 2.50662827463100050242

    private val P0 = doubleArrayOf(
        -5.99633501014107895267e1,
        9.80010754185999661536e1,
        -5.66762857469070293439e1,
        1.39312609387279679503e1,
        -1.23916583867381258016,
    )

    private val Q0 = doubleArrayOf(
        1.95448858338141759834,
        4.67627912898881538453,
        8.63602421390890590575e1,
        -2.25462687854119370527e2,
        2.00260212380060660359e2,
        -8.20372256168333339912e1,
        1.59056225126211695515e1,
        -1.18331621121330003142,
    )

    private val P1 = doubleArrayOf(
        4.05544892305962419923,
        3.15251094599893866154e1,
        5.71628192246421288162e1,
        4.408050738932008347e1,
        1.46849561928858024014e1,
        2.18663306850790267539,
        -1.40256079171354495875e-1,
        -3.50424626827848203418e-2,
        -8.57456785154685413611e-4,
    )

    private val Q1 = doubleArrayOf(
        1.57799883256466749731e1,
        4.53907635128879210584e1,
        4.1317203825467203044e1,
        1.50425385692907503408e1,
        2.50464946208309415979,
        -1.42182922854787788574e-1,
        -3.80806407691578277194e-2,
        -9.33259480895457427372e-4,
    )

    private val P2 = doubleArrayOf(
        3.2377489177694603597,
        6.91522889068984211695,
        3.93881025292474443415,
        1.33303460815807542389,
        2.01485389549179081538e-1,
        1.23716634817820021358e-2,
        3.01581553508235416007e-4,
        2.65806974686737550832e-6,
        6.2397453918498329373e-9,
    )

    private val Q2 = doubleArrayOf(
        6.02427039364742014255,
        3.67983563856160859403,
        1.37702099489081330271,
        2.1623699359449663589e-1,
        1.34204006088543189037e-2,
        3.28014464682127739104e-4,
        2.89247864745380683936e-6,
        6.79019408009981274425e-9,
    )

    // JSXGraph: src/math/probfuncs.js -> expx2
    internal fun expx2(
        initialValue: Double,
        sign: Int,
    ): Double {
        var value = abs(initialValue)
        if (sign < 0) {
            value = -value
        }

        val multiple = MINV * floor(M * value + 0.5)
        val residual = value - multiple
        var mainExponent = multiple * multiple
        var residualExponent = 2.0 * multiple * residual + residual * residual
        if (sign < 0) {
            mainExponent = -mainExponent
            residualExponent = -residualExponent
        }
        if (mainExponent + residualExponent > MAXLOG) {
            return Double.POSITIVE_INFINITY
        }
        return exp(mainExponent) * exp(residualExponent)
    }

    // JSXGraph: src/math/probfuncs.js -> polevl
    internal fun polevl(
        value: Double,
        coefficients: DoubleArray,
    ): Double {
        var result = 0.0
        for (coefficient in coefficients) {
            result = result * value + coefficient
        }
        return result
    }

    // JSXGraph: src/math/probfuncs.js -> p1evl
    internal fun p1evl(
        value: Double,
        coefficients: DoubleArray,
    ): Double {
        var result = 1.0
        for (coefficient in coefficients) {
            result = result * value + coefficient
        }
        return result
    }

    // JSXGraph: src/math/probfuncs.js -> ndtr
    fun ndtr(value: Double): Double {
        val scaled = value * SQRTH
        var magnitude = abs(scaled)
        var result: Double
        if (magnitude < 1.0) {
            result = 0.5 + 0.5 * erf(scaled)
        } else {
            result = 0.5 * erfce(magnitude)
            magnitude = expx2(value, -1)
            result *= sqrt(magnitude)
            if (scaled > 0.0) {
                result = 1.0 - result
            }
        }
        return result
    }

    private fun underflow(value: Double): Double = if (value < 0.0) 2.0 else 0.0

    // JSXGraph: src/math/probfuncs.js -> erfc
    fun erfc(value: Double): Double {
        val magnitude = abs(value)
        if (magnitude < 1.0) {
            return 1.0 - erf(value)
        }

        if (-value * value < -MAXLOG) {
            return underflow(value)
        }

        val exponential = expx2(value, -1)
        val numerator: Double
        val denominator: Double
        if (magnitude < 8.0) {
            numerator = polevl(magnitude, P)
            denominator = p1evl(magnitude, Q)
        } else {
            numerator = polevl(magnitude, R)
            denominator = p1evl(magnitude, S)
        }

        var result = exponential * numerator / denominator
        if (value < 0.0) {
            result = 2.0 - result
        }
        return if (result == 0.0) underflow(value) else result
    }

    // JSXGraph: src/math/probfuncs.js -> erfce
    internal fun erfce(value: Double): Double =
        if (value < 8.0) {
            polevl(value, P) / p1evl(value, Q)
        } else {
            polevl(value, R) / p1evl(value, S)
        }

    // JSXGraph: src/math/probfuncs.js -> erf
    fun erf(value: Double): Double {
        if (abs(value) > 1.0) {
            return 1.0 - erfc(value)
        }
        val square = value * value
        return value * polevl(square, T) / p1evl(square, U)
    }

    // JSXGraph: src/math/probfuncs.js -> ndtri
    fun ndtri(initialValue: Double): Double {
        if (initialValue <= 0.0) {
            return Double.NEGATIVE_INFINITY
        }
        if (initialValue >= 1.0) {
            return Double.POSITIVE_INFINITY
        }

        var code = 1
        var value = initialValue
        if (value > 1.0 - 0.13533528323661269189) {
            value = 1.0 - value
            code = 0
        }

        if (value > 0.13533528323661269189) {
            value -= 0.5
            val square = value * value
            var result =
                value +
                    value *
                    (
                        square * polevl(square, P0) /
                            p1evl(square, Q0)
                    )
            result *= SQRT_TWO_PI
            return result
        }

        var result = sqrt(-2.0 * ln(value))
        val leading = result - ln(result) / result
        val reciprocal = 1.0 / result
        val correction =
            if (result < 8.0) {
                reciprocal * polevl(reciprocal, P1) / p1evl(reciprocal, Q1)
            } else {
                reciprocal * polevl(reciprocal, P2) / p1evl(reciprocal, Q2)
            }
        result = leading - correction
        if (code != 0) {
            result = -result
        }
        return result
    }

    // JSXGraph: src/math/probfuncs.js -> erfi
    fun erfi(value: Double): Double = ndtri((value + 1.0) * 0.5) * SQRTH
}
