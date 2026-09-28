/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/ia.js -> interval arithmetic used by Plot v4.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Plot v4 calls `IntervalArithmetic.disable()` before interval evaluation, so
 * the translated operations intentionally use ordinary Double bounds without
 * nextafter expansion.
 */
internal object IntervalArithmetic {
    internal val empty: PlotInterval =
        PlotInterval(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
    internal val whole: PlotInterval =
        PlotInterval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)

    // JSXGraph 1.13.3: src/math/ia.js -> add.
    internal fun add(
        first: PlotInterval,
        second: PlotInterval,
    ): PlotInterval =
        PlotInterval(
            lo = first.lo + second.lo,
            hi = first.hi + second.hi,
        )

    // JSXGraph 1.13.3: src/math/ia.js -> sub.
    internal fun subtract(
        first: PlotInterval,
        second: PlotInterval,
    ): PlotInterval =
        PlotInterval(
            lo = first.lo - second.hi,
            hi = first.hi - second.lo,
        )

    // JSXGraph 1.13.3: src/math/ia.js -> negative.
    internal fun negative(value: PlotInterval): PlotInterval =
        PlotInterval(-value.hi, -value.lo)

    // JSXGraph 1.13.3: src/math/ia.js -> mul.
    internal fun multiply(
        first: PlotInterval,
        second: PlotInterval,
    ): PlotInterval {
        if (isEmpty(first) || isEmpty(second)) {
            return empty
        }
        val firstLow = first.lo
        val firstHigh = first.hi
        val secondLow = second.lo
        val secondHigh = second.hi
        return if (firstLow < 0.0) {
            if (firstHigh > 0.0) {
                if (secondLow < 0.0) {
                    if (secondHigh > 0.0) {
                        PlotInterval(
                            min(
                                firstLow * secondHigh,
                                firstHigh * secondLow,
                            ),
                            max(
                                firstLow * secondLow,
                                firstHigh * secondHigh,
                            ),
                        )
                    } else {
                        PlotInterval(
                            firstHigh * secondLow,
                            firstLow * secondLow,
                        )
                    }
                } else if (secondHigh > 0.0) {
                    PlotInterval(
                        firstLow * secondHigh,
                        firstHigh * secondHigh,
                    )
                } else {
                    PlotInterval(0.0, 0.0)
                }
            } else if (secondLow < 0.0) {
                if (secondHigh > 0.0) {
                    PlotInterval(
                        firstLow * secondHigh,
                        firstLow * secondLow,
                    )
                } else {
                    PlotInterval(
                        firstHigh * secondHigh,
                        firstLow * secondLow,
                    )
                }
            } else if (secondHigh > 0.0) {
                PlotInterval(
                    firstLow * secondHigh,
                    firstHigh * secondLow,
                )
            } else {
                PlotInterval(0.0, 0.0)
            }
        } else if (firstHigh > 0.0) {
            if (secondLow < 0.0) {
                if (secondHigh > 0.0) {
                    PlotInterval(
                        firstHigh * secondLow,
                        firstHigh * secondHigh,
                    )
                } else {
                    PlotInterval(
                        firstHigh * secondLow,
                        firstLow * secondHigh,
                    )
                }
            } else if (secondHigh > 0.0) {
                PlotInterval(
                    firstLow * secondLow,
                    firstHigh * secondHigh,
                )
            } else {
                PlotInterval(0.0, 0.0)
            }
        } else {
            PlotInterval(0.0, 0.0)
        }
    }

    // JSXGraph 1.13.3: src/math/ia.js -> div.
    internal fun divide(
        numerator: PlotInterval,
        denominator: PlotInterval,
    ): PlotInterval {
        if (isEmpty(numerator) || isEmpty(denominator)) {
            return empty
        }
        if (containsZero(denominator)) {
            if (
                denominator.lo != 0.0 &&
                denominator.hi != 0.0
            ) {
                return if (
                    numerator.lo == 0.0 &&
                    numerator.hi == 0.0
                ) {
                    numerator
                } else {
                    whole
                }
            }
            if (denominator.hi != 0.0) {
                return dividePositive(numerator, denominator.hi)
            }
            if (denominator.lo != 0.0) {
                return divideNegative(numerator, denominator.lo)
            }
            return empty
        }
        return divideNonZero(numerator, denominator)
    }

    // JSXGraph 1.13.3: src/math/ia.js -> fmod.
    internal fun modulo(
        first: PlotInterval,
        second: PlotInterval,
    ): PlotInterval {
        if (isEmpty(first) || isEmpty(second)) {
            return empty
        }
        val divisor = if (first.lo < 0.0) second.lo else second.hi
        var quotient = first.lo / divisor
        quotient = if (quotient < 0.0) ceil(quotient) else floor(quotient)
        return subtract(
            first,
            multiply(second, singleton(quotient)),
        )
    }

    // JSXGraph 1.13.3: src/math/ia.js -> pow.
    internal fun power(
        value: PlotInterval,
        exponent: PlotInterval,
    ): PlotInterval {
        if (isEmpty(value) || exponent.lo != exponent.hi) {
            return empty
        }
        val power = exponent.lo
        if (power == 0.0) {
            return if (value.lo == 0.0 && value.hi == 0.0) {
                empty
            } else {
                singleton(1.0)
            }
        }
        if (power < 0.0) {
            return power(inverse(value), singleton(-power))
        }
        if (power % 1.0 != 0.0) {
            return empty
        }
        return when {
            value.hi < 0.0 -> {
                val low = (-value.hi).pow(power)
                val high = (-value.lo).pow(power)
                if (power.toLong() and 1L == 1L) {
                    PlotInterval(-high, -low)
                } else {
                    PlotInterval(low, high)
                }
            }
            value.lo < 0.0 -> {
                if (power.toLong() and 1L == 1L) {
                    PlotInterval(
                        -(-value.lo).pow(power),
                        value.hi.pow(power),
                    )
                } else {
                    PlotInterval(
                        0.0,
                        max(-value.lo, value.hi).pow(power),
                    )
                }
            }
            else -> PlotInterval(
                value.lo.pow(power),
                value.hi.pow(power),
            )
        }
    }

    // JSXGraph 1.13.3: src/math/ia.js -> sqrt / nthRoot.
    internal fun sqrt(value: PlotInterval): PlotInterval =
        when {
            isEmpty(value) || value.hi < 0.0 -> empty
            else -> PlotInterval(
                sqrt(max(0.0, value.lo)),
                sqrt(value.hi),
            )
        }

    // JSXGraph 1.13.3: src/math/ia.js -> exp.
    internal fun exp(value: PlotInterval): PlotInterval =
        if (isEmpty(value)) {
            empty
        } else {
            PlotInterval(exp(value.lo), exp(value.hi))
        }

    // JSXGraph 1.13.3: src/math/ia.js -> log.
    internal fun log(value: PlotInterval): PlotInterval =
        when {
            isEmpty(value) || value.hi <= 0.0 -> empty
            value.lo <= 0.0 ->
                PlotInterval(Double.NEGATIVE_INFINITY, ln(value.hi))
            else -> PlotInterval(ln(value.lo), ln(value.hi))
        }

    // JSXGraph 1.13.3: src/math/ia.js -> abs.
    internal fun abs(value: PlotInterval): PlotInterval =
        when {
            isEmpty(value) -> empty
            value.lo >= 0.0 -> value
            value.hi <= 0.0 -> negative(value)
            else -> PlotInterval(0.0, max(-value.lo, value.hi))
        }

    // JSXGraph 1.13.3: src/math/ia.js -> min / max.
    internal fun minimum(values: List<PlotInterval>): PlotInterval {
        val present = values.filterNot(::isEmpty)
        if (present.isEmpty()) {
            return empty
        }
        return PlotInterval(
            present.minOf { it.lo },
            present.minOf { it.hi },
        )
    }

    internal fun maximum(values: List<PlotInterval>): PlotInterval {
        val present = values.filterNot(::isEmpty)
        if (present.isEmpty()) {
            return empty
        }
        return PlotInterval(
            present.maxOf { it.lo },
            present.maxOf { it.hi },
        )
    }

    // JSXGraph 1.13.3: src/math/ia.js -> sin / cos / tan.
    internal fun sin(value: PlotInterval): PlotInterval =
        trigonometricRange(value, ::sin, PI * 0.5, PI * 2.0)

    internal fun cos(value: PlotInterval): PlotInterval =
        trigonometricRange(value, ::cos, 0.0, PI * 2.0)

    internal fun tan(value: PlotInterval): PlotInterval {
        if (isEmpty(value)) {
            return empty
        }
        val firstPole = ceil((value.lo - PI * 0.5) / PI)
        val pole = PI * 0.5 + firstPole * PI
        return if (pole in value.lo..value.hi) {
            whole
        } else {
            PlotInterval(tan(value.lo), tan(value.hi))
        }
    }

    internal fun asin(value: PlotInterval): PlotInterval =
        if (isEmpty(value) || value.hi < -1.0 || value.lo > 1.0) {
            empty
        } else {
            PlotInterval(
                asin(max(-1.0, value.lo)),
                asin(min(1.0, value.hi)),
            )
        }

    internal fun acos(value: PlotInterval): PlotInterval =
        if (isEmpty(value) || value.hi < -1.0 || value.lo > 1.0) {
            empty
        } else {
            PlotInterval(
                acos(min(1.0, value.hi)),
                acos(max(-1.0, value.lo)),
            )
        }

    internal fun atan(value: PlotInterval): PlotInterval =
        if (isEmpty(value)) {
            empty
        } else {
            PlotInterval(atan(value.lo), atan(value.hi))
        }

    internal fun floor(value: PlotInterval): PlotInterval =
        if (isEmpty(value)) {
            empty
        } else {
            PlotInterval(floor(value.lo), floor(value.hi))
        }

    internal fun ceil(value: PlotInterval): PlotInterval =
        if (isEmpty(value)) {
            empty
        } else {
            PlotInterval(ceil(value.lo), ceil(value.hi))
        }

    internal fun singleton(value: Double): PlotInterval =
        PlotInterval(value, value)

    private fun inverse(value: PlotInterval): PlotInterval =
        divide(singleton(1.0), value)

    private fun dividePositive(
        numerator: PlotInterval,
        denominatorHigh: Double,
    ): PlotInterval =
        when {
            numerator.lo == 0.0 && numerator.hi == 0.0 -> numerator
            containsZero(numerator) -> whole
            numerator.hi < 0.0 ->
                PlotInterval(
                    Double.NEGATIVE_INFINITY,
                    numerator.hi / denominatorHigh,
                )
            else ->
                PlotInterval(
                    numerator.lo / denominatorHigh,
                    Double.POSITIVE_INFINITY,
                )
        }

    private fun divideNegative(
        numerator: PlotInterval,
        denominatorLow: Double,
    ): PlotInterval =
        when {
            numerator.lo == 0.0 && numerator.hi == 0.0 -> numerator
            containsZero(numerator) -> whole
            numerator.hi < 0.0 ->
                PlotInterval(
                    numerator.hi / denominatorLow,
                    Double.POSITIVE_INFINITY,
                )
            else ->
                PlotInterval(
                    Double.NEGATIVE_INFINITY,
                    numerator.lo / denominatorLow,
                )
        }

    private fun divideNonZero(
        numerator: PlotInterval,
        denominator: PlotInterval,
    ): PlotInterval {
        val values = doubleArrayOf(
            numerator.lo / denominator.lo,
            numerator.lo / denominator.hi,
            numerator.hi / denominator.lo,
            numerator.hi / denominator.hi,
        ).filterNot(Double::isNaN)
        return if (values.isEmpty()) {
            empty
        } else {
            PlotInterval(values.min(), values.max())
        }
    }

    private fun trigonometricRange(
        value: PlotInterval,
        function: (Double) -> Double,
        extremumOffset: Double,
        period: Double,
    ): PlotInterval {
        if (isEmpty(value) || !value.lo.isFinite() || !value.hi.isFinite()) {
            return if (isEmpty(value)) empty else PlotInterval(-1.0, 1.0)
        }
        if (value.hi - value.lo >= period) {
            return PlotInterval(-1.0, 1.0)
        }
        var low = min(function(value.lo), function(value.hi))
        var high = max(function(value.lo), function(value.hi))
        var extremum =
            extremumOffset +
                ceil((value.lo - extremumOffset) / PI) * PI
        while (extremum <= value.hi) {
            val sample = function(extremum)
            low = min(low, sample)
            high = max(high, sample)
            extremum += PI
        }
        return PlotInterval(low, high)
    }

    private fun containsZero(value: PlotInterval): Boolean =
        !isEmpty(value) && value.lo <= 0.0 && value.hi >= 0.0

    private fun isEmpty(value: PlotInterval): Boolean =
        value.lo > value.hi
}
