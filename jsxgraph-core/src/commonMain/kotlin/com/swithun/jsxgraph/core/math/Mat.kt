/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/math.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.utils.JsMath
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.acosh as kotlinAcosh
import kotlin.math.asinh as kotlinAsinh
import kotlin.math.cosh as kotlinCosh
import kotlin.math.sinh as kotlinSinh

object Mat {
    const val eps: Double = 0.000001

    private val factorialCache = mutableMapOf<Double, Double>()
    private val binomialCache = mutableMapOf<Pair<Double, Double>, Double>()

    // JSXGraph: src/math/math.js -> relDif
    fun relDif(a: Double, b: Double): Double {
        val denominator = max(abs(a), abs(b))
        return if (denominator == 0.0) 0.0 else abs(a - b) / denominator
    }

    // JSXGraph: src/math/math.js -> mod
    fun mod(a: Double, m: Double): Double = a - floor(a / m) * m

    // JSXGraph: src/math/math.js -> wrap
    fun wrap(x: Double, a: Double, b: Double): Double = a + mod(x - a, b - a)

    // JSXGraph: src/math/math.js -> clamp
    fun clamp(x: Double, a: Double, b: Double): Double = min(max(x, a), b)

    // JSXGraph: src/math/math.js -> wrapAndClamp
    fun wrapAndClamp(
        x: Double,
        a: Double,
        b: Double,
        period: Double,
    ): Double {
        val midpoint = 0.5 * (a + b)
        val halfPeriod = 0.5 * period
        return clamp(wrap(x, midpoint - halfPeriod, midpoint + halfPeriod), a, b)
    }

    // JSXGraph: src/math/math.js -> vector
    fun vector(n: Int, init: Double = 0.0): DoubleArray {
        val length = max(n, 0)
        val initialValue = if (init == 0.0 || init.isNaN()) 0.0 else init
        return DoubleArray(length) { initialValue }
    }

    // JSXGraph: src/math/math.js -> matrix
    fun matrix(
        n: Int,
        m: Int = n,
        init: Double = 0.0,
    ): Array<DoubleArray> {
        val rowCount = max(n, 0)
        val columnCount = max(if (m == 0) n else m, 0)
        val initialValue = if (init == 0.0 || init.isNaN()) 0.0 else init
        return Array(rowCount) { DoubleArray(columnCount) { initialValue } }
    }

    // JSXGraph: src/math/math.js -> identity
    fun identity(
        n: Int,
        m: Int? = null,
    ): Array<DoubleArray> {
        val columnCount = m ?: n
        val result = matrix(n, columnCount)
        for (index in 0 until min(max(n, 0), max(columnCount, 0))) {
            result[index][index] = 1.0
        }
        return result
    }

    // JSXGraph: src/math/math.js -> frustum
    fun frustum(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        near: Double,
        far: Double,
    ): Array<DoubleArray> {
        val result = matrix(4, 4)
        result[0][0] = near * 2.0 / (right - left)
        result[0][2] = (right + left) / (right - left)
        result[1][1] = near * 2.0 / (top - bottom)
        result[1][2] = (top + bottom) / (top - bottom)
        result[2][2] = -(far + near) / (far - near)
        result[2][3] = -(far * near * 2.0) / (far - near)
        result[3][2] = -1.0
        return result
    }

    // JSXGraph: src/math/math.js -> projection
    fun projection(
        fieldOfView: Double,
        ratio: Double,
        near: Double,
        far: Double,
    ): Array<DoubleArray> {
        val top = near * tan(fieldOfView / 2.0)
        val right = top * ratio
        return frustum(-right, right, -top, top, near, far)
    }

    // JSXGraph: src/math/math.js -> matVecMult
    fun matVecMult(
        matrix: Array<DoubleArray>,
        vector: DoubleArray,
    ): DoubleArray {
        val result = DoubleArray(matrix.size)
        if (vector.size == 3) {
            for (row in matrix.indices) {
                result[row] =
                    matrix[row][0] * vector[0] +
                    matrix[row][1] * vector[1] +
                    matrix[row][2] * vector[2]
            }
            return result
        }

        for (row in matrix.indices) {
            var sum = 0.0
            for (column in vector.indices) {
                sum += matrix[row][column] * vector[column]
            }
            result[row] = sum
        }
        return result
    }

    // JSXGraph: src/math/math.js -> vecMatMult
    fun vecMatMult(
        vector: DoubleArray,
        matrix: Array<DoubleArray>,
    ): DoubleArray {
        val resultSize = if (vector.size == 3) matrix.size else vector.size
        val result = DoubleArray(resultSize)
        if (vector.size == 3) {
            for (column in matrix.indices) {
                result[column] =
                    vector[0] * matrix[0][column] +
                    vector[1] * matrix[1][column] +
                    vector[2] * matrix[2][column]
            }
            return result
        }

        for (column in vector.indices) {
            var sum = 0.0
            for (row in matrix.indices) {
                sum += vector[row] * matrix[row][column]
            }
            result[column] = sum
        }
        return result
    }

    // JSXGraph: src/math/math.js -> matMatMult
    fun matMatMult(
        first: Array<DoubleArray>,
        second: Array<DoubleArray>,
    ): Array<DoubleArray> {
        val rowCount = first.size
        val columnCount = if (rowCount > 0 && second.isNotEmpty()) second[0].size else 0
        val result = matrix(rowCount, columnCount)
        for (row in 0 until rowCount) {
            for (column in 0 until columnCount) {
                var sum = 0.0
                for (inner in second.indices) {
                    sum += first[row][inner] * second[inner][column]
                }
                result[row][column] = sum
            }
        }
        return result
    }

    // JSXGraph: src/math/math.js -> matNumberMult
    fun matNumberMult(
        matrix: Array<DoubleArray>,
        scalar: Double,
    ): Array<DoubleArray> {
        val rowCount = matrix.size
        val columnCount = if (rowCount > 0) matrix[0].size else 0
        val result = matrix(rowCount, columnCount)
        for (row in 0 until rowCount) {
            for (column in 0 until columnCount) {
                result[row][column] = matrix[row][column] * scalar
            }
        }
        return result
    }

    // JSXGraph: src/math/math.js -> matMatAdd
    fun matMatAdd(
        first: Array<DoubleArray>,
        second: Array<DoubleArray>,
    ): Array<DoubleArray> {
        val rowCount = first.size
        val columnCount = if (rowCount > 0) first[0].size else 0
        val result = matrix(rowCount, columnCount)
        for (row in 0 until rowCount) {
            for (column in 0 until columnCount) {
                result[row][column] = first[row][column] + second[row][column]
            }
        }
        return result
    }

    // JSXGraph: src/math/math.js -> transpose
    fun transpose(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val rowCount = matrix.size
        val columnCount = if (rowCount > 0) matrix[0].size else 0
        val result = matrix(columnCount, rowCount)
        for (column in 0 until columnCount) {
            for (row in 0 until rowCount) {
                result[column][row] = matrix[row][column]
            }
        }
        return result
    }

    // JSXGraph: src/math/math.js -> inverse
    fun inverse(input: Array<DoubleArray>): Array<DoubleArray> {
        val size = input.size
        val matrix = Array(size) { row ->
            DoubleArray(size) { column -> input[row][column] }
        }
        val permutation = IntArray(size) { it }
        val helper = DoubleArray(size)
        val singularThreshold = eps * eps

        for (column in 0 until size) {
            var maximum = abs(matrix[column][column])
            var pivotRow = column
            for (row in column + 1 until size) {
                if (abs(matrix[row][column]) > maximum) {
                    maximum = abs(matrix[row][column])
                    pivotRow = row
                }
            }

            if (maximum <= singularThreshold) {
                return emptyArray()
            }

            if (pivotRow > column) {
                for (index in 0 until size) {
                    val swap = matrix[column][index]
                    matrix[column][index] = matrix[pivotRow][index]
                    matrix[pivotRow][index] = swap
                }
                val swap = permutation[column]
                permutation[column] = permutation[pivotRow]
                permutation[pivotRow] = swap
            }

            val scale = 1.0 / matrix[column][column]
            for (row in 0 until size) {
                matrix[row][column] *= scale
            }
            matrix[column][column] = scale

            for (targetColumn in 0 until size) {
                if (targetColumn != column) {
                    for (row in 0 until size) {
                        if (row != column) {
                            matrix[row][targetColumn] -=
                                matrix[row][column] * matrix[column][targetColumn]
                        }
                    }
                    matrix[column][targetColumn] = -scale * matrix[column][targetColumn]
                }
            }
        }

        for (row in 0 until size) {
            for (column in 0 until size) {
                helper[permutation[column]] = matrix[row][column]
            }
            for (column in 0 until size) {
                matrix[row][column] = helper[column]
            }
        }
        return matrix
    }

    // JSXGraph: src/math/math.js -> trace
    fun trace(matrix: Array<DoubleArray>): Double {
        val rowCount = matrix.size
        val columnCount = if (rowCount > 0) matrix[0].size else 0
        if (rowCount != columnCount) {
            return Double.NaN
        }
        var result = 0.0
        for (index in 0 until columnCount) {
            result += matrix[index][index]
        }
        return result
    }

    // JSXGraph: src/math/math.js -> innerProduct
    fun innerProduct(
        first: DoubleArray,
        second: DoubleArray,
        length: Int = first.size,
    ): Double {
        var result = 0.0
        for (index in 0 until length) {
            result += first[index] * second[index]
        }
        return result
    }

    // JSXGraph: src/math/math.js -> crossProduct
    fun crossProduct(
        first: DoubleArray,
        second: DoubleArray,
    ): DoubleArray = doubleArrayOf(
        first[1] * second[2] - first[2] * second[1],
        first[2] * second[0] - first[0] * second[2],
        first[0] * second[1] - first[1] * second[0],
    )

    // JSXGraph: src/math/math.js -> norm
    fun norm(
        vector: DoubleArray,
        length: Int = vector.size,
    ): Double {
        var sum = 0.0
        for (index in 0 until length) {
            sum += vector[index] * vector[index]
        }
        return sqrt(sum)
    }

    // JSXGraph: src/math/math.js -> axpy
    fun axpy(
        scalar: Double,
        x: DoubleArray,
        y: DoubleArray,
    ): DoubleArray = DoubleArray(x.size) { index -> scalar * x[index] + y[index] }

    // JSXGraph: src/math/math.js -> factorial
    fun factorial(value: Double): Double {
        if (value < 0.0 || !value.isFinite()) {
            return Double.NaN
        }

        val integer = floor(value)
        if (integer == 0.0 || integer == 1.0) {
            return 1.0
        }
        return factorialCache.getOrPut(integer) {
            integer * factorial(integer - 1.0)
        }
    }

    // JSXGraph: src/math/math.js -> binomial
    fun binomial(
        nValue: Double,
        kValue: Double,
    ): Double {
        if (kValue > nValue || kValue < 0.0 || !nValue.isFinite() || !kValue.isFinite()) {
            return Double.NaN
        }

        val n = JsMath.round(nValue)
        val k = JsMath.round(kValue)
        if (k == 0.0 || k == n) {
            return 1.0
        }
        return binomialCache.getOrPut(n to k) {
            var result = 1.0
            var index = 0
            while (index < k) {
                result *= n - index
                result /= index + 1.0
                index += 1
            }
            result
        }
    }

    // JSXGraph: src/math/math.js -> cosh
    fun cosh(value: Double): Double = kotlinCosh(value)

    // JSXGraph: src/math/math.js -> sinh
    fun sinh(value: Double): Double = kotlinSinh(value)

    // JSXGraph: src/math/math.js -> acosh
    fun acosh(value: Double): Double = kotlinAcosh(value)

    // JSXGraph: src/math/math.js -> asinh
    fun asinh(value: Double): Double = kotlinAsinh(value)

    // JSXGraph: src/math/math.js -> cot
    fun cot(value: Double): Double {
        val tangent = tan(value)
        return if (abs(tangent) < eps) Double.NaN else 1.0 / tangent
    }

    // JSXGraph: src/math/math.js -> acot
    fun acot(value: Double): Double =
        (if (value >= 0.0) 0.5 else -0.5) * PI - atan(value)

    // JSXGraph: src/math/math.js -> nthroot
    fun nthroot(
        radicand: Double,
        index: Double,
    ): Double {
        if (index <= 0.0 || floor(index) != index) {
            return Double.NaN
        }

        val inverse = 1.0 / index
        if (radicand == 0.0) {
            return 0.0
        }
        if (radicand > 0.0) {
            return exp(inverse * ln(radicand))
        }
        if (index % 2.0 == 1.0) {
            return -exp(inverse * ln(-radicand))
        }
        return Double.NaN
    }

    // JSXGraph: src/math/math.js -> cbrt
    fun cbrt(value: Double): Double = nthroot(value, 3.0)

    // JSXGraph: src/math/math.js -> pow
    fun pow(
        base: Double,
        exponent: Double,
    ): Double {
        if (base == 0.0) {
            return if (exponent == 0.0) 1.0 else 0.0
        }
        if (floor(exponent) == exponent) {
            return base.pow(exponent)
        }
        if (base > 0.0) {
            return exp(exponent * ln(base))
        }
        return Double.NaN
    }

    // JSXGraph: src/math/math.js -> ratpow
    fun ratpow(
        base: Double,
        numerator: Double,
        denominator: Double,
    ): Double {
        if (numerator == 0.0) {
            return 1.0
        }
        if (denominator == 0.0) {
            return Double.NaN
        }

        val divisor = gcd(numerator, denominator)
        return nthroot(pow(base, numerator / divisor), denominator / divisor)
    }

    // JSXGraph: src/math/math.js -> log10
    fun log10(value: Double): Double = ln(value) / ln(10.0)

    // JSXGraph: src/math/math.js -> log2
    fun log2(value: Double): Double = ln(value) / ln(2.0)

    // JSXGraph: src/math/math.js -> log
    fun log(
        value: Double,
        base: Double? = null,
    ): Double {
        if (base != null) {
            if (base <= 0.0 || abs(base - 1.0) < eps) {
                return Double.NaN
            }
            return ln(value) / ln(base)
        }
        return ln(value)
    }

    // JSXGraph: src/math/math.js -> sign
    fun sign(value: Double): Double {
        if (value == 0.0 || value.isNaN()) {
            return value
        }
        return if (value > 0.0) 1.0 else -1.0
    }

    // JSXGraph: src/math/math.js -> squampow
    fun squampow(
        initialBase: Double,
        initialExponent: Double,
    ): Double {
        if (floor(initialExponent) != initialExponent || !initialExponent.isFinite()) {
            return pow(initialBase, initialExponent)
        }

        var base = initialBase
        var exponent = initialExponent
        var result = 1.0
        if (exponent < 0.0) {
            base = 1.0 / base
            exponent *= -1.0
        }
        while (exponent != 0.0) {
            if (exponent % 2.0 == 1.0) {
                result *= base
            }
            exponent = floor(exponent / 2.0)
            base *= base
        }
        return result
    }

    // JSXGraph: src/math/math.js -> gcd
    fun gcd(
        first: Double,
        second: Double,
    ): Double {
        var a = abs(first)
        var b = abs(second)
        if (!a.isFinite() || !b.isFinite()) {
            return Double.NaN
        }
        if (a == 0.0) {
            return b
        }
        if (b == 0.0) {
            return a
        }
        if (b > a) {
            val swap = a
            a = b
            b = swap
        }

        while (true) {
            a %= b
            if (a == 0.0) {
                return b
            }
            b %= a
            if (b == 0.0) {
                return a
            }
        }
    }

    // JSXGraph: src/math/math.js -> lcm
    fun lcm(
        first: Double,
        second: Double,
    ): Double {
        if (!first.isFinite() || !second.isFinite()) {
            return Double.NaN
        }
        val product = first * second
        return if (product != 0.0) product / gcd(first, second) else 0.0
    }

    // JSXGraph: src/math/math.js -> roundToStep
    fun roundToStep(
        value: Double,
        step: Double? = null,
        minimum: Double? = null,
        maximum: Double? = null,
    ): Double {
        if (step == null && minimum == null && maximum == null) {
            return value
        }

        var result = value
        if (maximum != null) {
            result = min(result, maximum)
        }
        if (minimum != null) {
            result = max(result, minimum)
        }

        val minimumOrZero =
            if (minimum == null || minimum == 0.0 || minimum.isNaN()) 0.0 else minimum
        if (step != null) {
            var quotient = (result - minimumOrZero) / step
            if (quotient.isFinite() && floor(quotient) == quotient) {
                return result
            }

            quotient = JsMath.round(quotient)
            result = minimumOrZero + quotient * step
        }

        if (maximum != null) {
            result = min(result, maximum)
        }
        if (minimum != null) {
            result = max(result, minimum)
        }
        return result
    }

    // JSXGraph: src/math/math.js -> erf / erfc / erfi / ndtr / ndtri
    fun erf(value: Double): Double = ProbFuncs.erf(value)

    fun erfc(value: Double): Double = ProbFuncs.erfc(value)

    fun erfi(value: Double): Double = ProbFuncs.erfi(value)

    fun ndtr(value: Double): Double = ProbFuncs.ndtr(value)

    fun ndtri(value: Double): Double = ProbFuncs.ndtri(value)

    // JSXGraph: src/math/math.js -> hypot
    fun hypot(vararg values: Double): Double {
        var sum = 0.0
        for (value in values) {
            sum += value * value
        }
        return sqrt(sum)
    }

    // JSXGraph: src/math/math.js -> hstep
    fun hstep(value: Double): Double = when {
        value > 0.0 -> 1.0
        value < 0.0 -> 0.0
        else -> 0.5
    }

    // JSXGraph: src/math/math.js -> gamma
    fun gamma(initialValue: Double): Double {
        val coefficients = doubleArrayOf(
            1.0,
            676.5203681218851,
            -1259.1392167224028,
            771.32342877765313,
            -176.61502916214059,
            12.507343278686905,
            -0.13857109526572012,
            9.9843695780195716e-6,
            1.5056327351493116e-7,
        )
        if (initialValue < 0.5) {
            return PI / (sin(PI * initialValue) * gamma(1.0 - initialValue))
        }

        val value = initialValue - 1.0
        var series = coefficients[0]
        for (index in 1 until coefficients.size) {
            series += coefficients[index] / (value + index)
        }
        val shifted = value + 7.5
        return sqrt(2.0 * PI) * shifted.pow(value + 0.5) * exp(-shifted) * series
    }

    // JSXGraph: src/math/math.js -> comparison and logical operators
    fun lt(a: Double, b: Double): Boolean = a < b

    fun leq(a: Double, b: Double): Boolean = a <= b

    fun gt(a: Double, b: Double): Boolean = a > b

    fun geq(a: Double, b: Double): Boolean = a >= b

    fun eq(a: Double, b: Double): Boolean = a == b

    fun neq(a: Double, b: Double): Boolean = a != b

    fun and(a: Boolean, b: Boolean): Boolean = a && b

    fun not(value: Boolean): Boolean = !value

    fun or(a: Boolean, b: Boolean): Boolean = a || b

    fun xor(a: Boolean, b: Boolean): Boolean = (a || b) && !(a && b)

    // JSXGraph: src/math/math.js -> decToFraction
    fun decToFraction(
        initialValue: Double,
        order: Double = 0.001,
    ): DoubleArray {
        var value = JsMath.round(initialValue * 1.0e12) * 1.0e-12
        val sign = if (value < 0.0) -1.0 else 1.0
        value = abs(value)

        val leading = floor(value)
        value -= floor(value)
        var coefficient = 0.0
        var numeratorBeforePrevious = 1.0
        var numerator = coefficient
        var numeratorPrevious = coefficient
        var denominatorBeforePrevious = 0.0
        var denominator = 1.0
        var denominatorPrevious = 1.0
        var iteration = 0

        while (value - floor(value) > order && iteration < 20) {
            value = 1.0 / (value - coefficient)
            coefficient = floor(value)
            numerator = numeratorBeforePrevious + coefficient * numeratorPrevious
            denominator = denominatorBeforePrevious + coefficient * denominatorPrevious
            numeratorBeforePrevious = numeratorPrevious
            denominatorBeforePrevious = denominatorPrevious
            numeratorPrevious = numerator
            denominatorPrevious = denominator
            iteration += 1
        }
        return doubleArrayOf(sign, leading, numerator, denominator)
    }

    // JSXGraph: src/math/math.js -> normalize
    fun normalize(standardForm: DoubleArray): DoubleArray {
        val doubledA = 2.0 * standardForm[3]
        val radius = standardForm[4] / doubledA
        standardForm[5] = radius
        standardForm[6] = -standardForm[1] / doubledA
        standardForm[7] = -standardForm[2] / doubledA

        if (!radius.isFinite()) {
            val norm = hypot(standardForm[1], standardForm[2])
            standardForm[0] /= norm
            standardForm[1] /= norm
            standardForm[2] /= norm
            standardForm[3] = 0.0
            standardForm[4] = 1.0
        } else if (abs(radius) >= 1.0) {
            standardForm[0] =
                (
                    standardForm[6] * standardForm[6] +
                        standardForm[7] * standardForm[7] -
                        radius * radius
                ) / (2.0 * radius)
            standardForm[1] = -standardForm[6] / radius
            standardForm[2] = -standardForm[7] / radius
            standardForm[3] = 1.0 / (2.0 * radius)
            standardForm[4] = 1.0
        } else {
            val radiusSign = if (radius <= 0.0) -1.0 else 1.0
            standardForm[0] =
                radiusSign *
                (
                    standardForm[6] * standardForm[6] +
                        standardForm[7] * standardForm[7] -
                        radius * radius
                ) *
                0.5
            standardForm[1] = -radiusSign * standardForm[6]
            standardForm[2] = -radiusSign * standardForm[7]
            standardForm[3] = radiusSign / 2.0
            standardForm[4] = radiusSign * radius
        }
        return standardForm
    }

    // JSXGraph: src/math/math.js -> toGL
    fun toGL(matrix: Array<DoubleArray>): FloatArray {
        val result = FloatArray(16)
        val columnCount = matrix.firstOrNull()?.size ?: 0
        if (matrix.size != 4 && columnCount != 4) {
            return result
        }

        for (row in 0 until 4) {
            for (column in 0 until 4) {
                result[row + 4 * column] =
                    (matrix.getOrNull(row)?.getOrNull(column) ?: Double.NaN).toFloat()
            }
        }
        return result
    }

    // JSXGraph: src/math/math.js -> Vieta
    @Suppress("FunctionName")
    fun Vieta(roots: DoubleArray): DoubleArray {
        val result = roots.copyOf()
        for (degree in 1 until roots.size) {
            val root = result[degree]
            result[degree] *= result[degree - 1]
            for (index in degree - 1 downTo 1) {
                result[index] += result[index - 1] * root
            }
            result[0] += root
        }
        return result
    }
}
