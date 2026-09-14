/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/complex.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

class Complex(
    real: Double = 0.0,
    imaginary: Double = 0.0,
) {
    val isComplex: Boolean = true

    var real: Double = real.jsDefaultZero()
        private set

    var imaginary: Double = imaginary.jsDefaultZero()
        private set

    constructor(value: Complex) : this(value.real, value.imaginary)

    // JSXGraph: src/math/complex.js -> toString
    fun toString(digits: Int?): String {
        val operator = if (imaginary < 0.0) " - " else " + "
        val absoluteImaginary = abs(imaginary)
        return if (digits != null) {
            JsNumberFormat.fixed(real, digits) +
                operator +
                JsNumberFormat.fixed(absoluteImaginary, digits) +
                "i"
        } else {
            JsNumberFormat.compact(real) +
                operator +
                JsNumberFormat.compact(absoluteImaginary) +
                "i"
        }
    }

    override fun toString(): String = toString(digits = null)

    // JSXGraph: src/math/complex.js -> toArray
    fun toArray(): DoubleArray = doubleArrayOf(real, imaginary)

    // JSXGraph: src/math/complex.js -> add
    fun add(value: Double): Complex {
        real += value
        return this
    }

    fun add(value: Complex): Complex {
        real += value.real
        imaginary += value.imaginary
        return this
    }

    // JSXGraph: src/math/complex.js -> sub
    fun sub(value: Double): Complex {
        real -= value
        return this
    }

    fun sub(value: Complex): Complex {
        real -= value.real
        imaginary -= value.imaginary
        return this
    }

    // JSXGraph: src/math/complex.js -> mult
    fun mult(value: Double): Complex {
        real *= value
        imaginary *= value
        return this
    }

    fun mult(value: Complex): Complex {
        val originalReal = real
        val originalImaginary = imaginary
        real = originalReal * value.real - originalImaginary * value.imaginary
        imaginary = originalReal * value.imaginary + originalImaginary * value.real
        return this
    }

    // JSXGraph: src/math/complex.js -> div
    fun div(value: Double): Complex {
        if (abs(value) < Mat.eps) {
            real = Double.POSITIVE_INFINITY
            imaginary = Double.POSITIVE_INFINITY
            return this
        }

        real /= value
        imaginary /= value
        return this
    }

    fun div(value: Complex): Complex {
        val threshold = Mat.eps * Mat.eps
        if (abs(value.real) < threshold && abs(value.imaginary) < threshold) {
            real = Double.POSITIVE_INFINITY
            imaginary = Double.POSITIVE_INFINITY
            return this
        }

        val denominator = value.real * value.real + value.imaginary * value.imaginary
        val originalReal = real
        val originalImaginary = imaginary
        real = (originalReal * value.real + originalImaginary * value.imaginary) / denominator
        imaginary =
            (originalImaginary * value.real - originalReal * value.imaginary) / denominator
        return this
    }

    // JSXGraph: src/math/complex.js -> conj
    fun conj(): Complex {
        imaginary *= -1.0
        return this
    }

    // JSXGraph: src/math/complex.js -> abs
    fun abs(): Double = sqrt(real * real + imaginary * imaginary)

    // JSXGraph: src/math/complex.js -> angle
    fun angle(): Double = atan2(imaginary, real)

    private fun Double.jsDefaultZero(): Double =
        if (this == 0.0 || isNaN()) 0.0 else this
}

object C {
    fun add(first: Complex, second: Complex): Complex = Complex(first).add(second)

    fun add(first: Complex, second: Double): Complex = Complex(first).add(second)

    fun add(first: Double, second: Complex): Complex = Complex(first).add(second)

    fun add(first: Double, second: Double): Complex = Complex(first).add(second)

    fun sub(first: Complex, second: Complex): Complex = Complex(first).sub(second)

    fun sub(first: Complex, second: Double): Complex = Complex(first).sub(second)

    fun sub(first: Double, second: Complex): Complex = Complex(first).sub(second)

    fun sub(first: Double, second: Double): Complex = Complex(first).sub(second)

    fun mult(first: Complex, second: Complex): Complex = Complex(first).mult(second)

    fun mult(first: Complex, second: Double): Complex = Complex(first).mult(second)

    fun mult(first: Double, second: Complex): Complex = Complex(first).mult(second)

    fun mult(first: Double, second: Double): Complex = Complex(first).mult(second)

    fun div(first: Complex, second: Complex): Complex = Complex(first).div(second)

    fun div(first: Complex, second: Double): Complex = Complex(first).div(second)

    fun div(first: Double, second: Complex): Complex = Complex(first).div(second)

    fun div(first: Double, second: Double): Complex = Complex(first).div(second)

    fun conj(value: Complex): Complex = Complex(value).conj()

    fun conj(value: Double): Complex = Complex(value).conj()

    fun abs(value: Complex): Double = Complex(value).abs()

    fun abs(value: Double): Double = Complex(value).abs()

    fun angle(value: Complex): Double = Complex(value).angle()

    fun angle(value: Double): Double = Complex(value).angle()

    fun copy(value: Complex): Complex = Complex(value)

    fun copy(value: Double): Complex = Complex(value)
}
