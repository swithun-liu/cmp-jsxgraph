/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/ia.js -> interval arithmetic used by Plot v4.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

internal sealed interface IntervalArithmeticError {
    data class NonSingletonBound(
        val bound: String,
        val interval: PlotInterval,
    ) : IntervalArithmeticError

    data class NonOverlappingUnion(
        val first: PlotInterval,
        val second: PlotInterval,
    ) : IntervalArithmeticError

    data class DisconnectedDifference(
        val source: PlotInterval,
        val removed: PlotInterval,
    ) : IntervalArithmeticError
}

// JSXGraph 1.13.3: src/math/ia.js -> DoubleBits.
// The raw-bit KMP implementation is endian-neutral and replaces the upstream
// doubleBitsLE/toDoubleLE/lowUintLE/highUintLE and
// doubleBitsBE/toDoubleBE/lowUintBE/highUintBE typed-array closures.
internal object DoubleBits {
    // JSXGraph 1.13.3: src/math/ia.js ->
    // DoubleBits.doubleBits.
    internal fun doubleBits(value: Double): LongArray {
        val bits = value.toRawBits()
        return longArrayOf(
            bits and UINT_MASK,
            bits.ushr(32) and UINT_MASK,
        )
    }

    // JSXGraph 1.13.3: src/math/ia.js -> DoubleBits.pack.
    internal fun pack(
        low: Long,
        high: Long,
    ): Double =
        Double.fromBits(
            (high and UINT_MASK).shl(32) or
                (low and UINT_MASK),
        )

    // JSXGraph 1.13.3: src/math/ia.js -> DoubleBits.lo.
    internal fun low(value: Double): Long =
        value.toRawBits() and UINT_MASK

    // JSXGraph 1.13.3: src/math/ia.js -> DoubleBits.hi.
    internal fun high(value: Double): Long =
        value.toRawBits().ushr(32) and UINT_MASK

    // JSXGraph 1.13.3: src/math/ia.js ->
    // DoubleBits.sign.
    internal fun sign(value: Double): Int =
        (high(value).ushr(31) and 1L).toInt()

    // JSXGraph 1.13.3: src/math/ia.js ->
    // DoubleBits.exponent.
    internal fun exponent(value: Double): Int =
        ((high(value).ushr(20) and 0x7ffL) - 1023L).toInt()

    // JSXGraph 1.13.3: src/math/ia.js ->
    // DoubleBits.fraction.
    internal fun fraction(value: Double): LongArray {
        val high = high(value)
        var fractionHigh = high and FRACTION_HIGH_MASK
        if (high and EXPONENT_MASK != 0L) {
            fractionHigh += IMPLICIT_BIT
        }
        return longArrayOf(low(value), fractionHigh)
    }

    // JSXGraph 1.13.3: src/math/ia.js ->
    // DoubleBits.denormalized.
    internal fun denormalized(value: Double): Boolean =
        high(value) and EXPONENT_MASK == 0L

    private const val UINT_MASK = 0xffff_ffffL
    private const val FRACTION_HIGH_MASK = 0x000f_ffffL
    private const val EXPONENT_MASK = 0x7ff0_0000L
    private const val IMPLICIT_BIT = 0x0010_0000L
}

// JSXGraph 1.13.3: src/math/ia.js -> MatInterval.
internal class MutablePlotInterval(
    internal var lo: Double = 0.0,
    internal var hi: Double = lo,
) {
    // JSXGraph 1.13.3: src/math/ia.js -> MatInterval.print.
    internal fun print(): String = "[ $lo $hi ]"

    // JSXGraph 1.13.3: src/math/ia.js -> MatInterval.set.
    internal fun set(
        low: Double,
        high: Double,
    ): MutablePlotInterval {
        lo = low
        hi = high
        return this
    }

    // JSXGraph 1.13.3: src/math/ia.js -> MatInterval.bounded.
    internal fun bounded(
        low: Double,
        high: Double,
    ): MutablePlotInterval =
        set(
            IntervalArithmetic.prev(low),
            IntervalArithmetic.next(high),
        )

    // JSXGraph 1.13.3: src/math/ia.js ->
    // MatInterval.boundedSingleton.
    internal fun boundedSingleton(value: Double): MutablePlotInterval =
        bounded(value, value)

    // JSXGraph 1.13.3: src/math/ia.js -> MatInterval.assign.
    internal fun assign(
        low: Double,
        high: Double,
    ): MutablePlotInterval =
        if (low.isNaN() || high.isNaN() || low > high) {
            setEmpty()
        } else {
            set(low, high)
        }

    // JSXGraph 1.13.3: src/math/ia.js -> MatInterval.setEmpty.
    internal fun setEmpty(): MutablePlotInterval =
        set(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)

    // JSXGraph 1.13.3: src/math/ia.js -> MatInterval.setWhole.
    internal fun setWhole(): MutablePlotInterval =
        set(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)

    // JSXGraph 1.13.3: src/math/ia.js -> MatInterval.open.
    internal fun open(
        low: Double,
        high: Double,
    ): MutablePlotInterval =
        assign(
            IntervalArithmetic.next(low),
            IntervalArithmetic.prev(high),
        )

    // JSXGraph 1.13.3: src/math/ia.js ->
    // MatInterval.halfOpenLeft.
    internal fun halfOpenLeft(
        low: Double,
        high: Double,
    ): MutablePlotInterval =
        assign(IntervalArithmetic.next(low), high)

    // JSXGraph 1.13.3: src/math/ia.js ->
    // MatInterval.halfOpenRight.
    internal fun halfOpenRight(
        low: Double,
        high: Double,
    ): MutablePlotInterval =
        assign(low, IntervalArithmetic.prev(high))

    // JSXGraph 1.13.3: src/math/ia.js -> MatInterval.toArray.
    internal fun toArray(): DoubleArray = doubleArrayOf(lo, hi)

    // JSXGraph 1.13.3: src/math/ia.js -> MatInterval.clone.
    internal fun cloneInterval(): MutablePlotInterval =
        MutablePlotInterval(lo, hi)

    internal fun toPlotInterval(): PlotInterval = PlotInterval(lo, hi)
}

internal object IntervalArithmetic {
    private const val PI_LOW =
        (3373259426.0 + 273688.0 / (1 shl 21)) / (1L shl 30)
    private const val PI_HIGH =
        (3373259426.0 + 273689.0 / (1 shl 21)) / (1L shl 30)
    private const val PI_HALF_LOW = PI_LOW * 0.5
    private const val PI_HALF_HIGH = PI_HIGH * 0.5
    private const val PI_TWICE_LOW = PI_LOW * 2.0
    private const val PI_TWICE_HIGH = PI_HIGH * 2.0

    private var outwardRoundingEnabled = false

    internal val empty: PlotInterval =
        PlotInterval(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
    internal val whole: PlotInterval =
        PlotInterval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
    internal val zero: PlotInterval = PlotInterval(0.0, 0.0)
    internal val one: PlotInterval = PlotInterval(1.0, 1.0)
    internal val pi: PlotInterval = PlotInterval(PI_LOW, PI_HIGH)
    internal val piHalf: PlotInterval =
        PlotInterval(PI_HALF_LOW, PI_HALF_HIGH)
    internal val piTwice: PlotInterval =
        PlotInterval(PI_TWICE_LOW, PI_TWICE_HIGH)

    // JSXGraph 1.13.3: src/math/ia.js -> Interval.
    internal fun interval(
        low: Double = 0.0,
        high: Double = low,
    ): PlotInterval = PlotInterval(low, high)

    internal fun interval(
        low: PlotInterval,
        high: PlotInterval,
    ): GMResult<PlotInterval, IntervalArithmeticError> {
        if (!isSingleton(low)) {
            return GMResult.Err(
                IntervalArithmeticError.NonSingletonBound("lo", low),
            )
        }
        if (!isSingleton(high)) {
            return GMResult.Err(
                IntervalArithmeticError.NonSingletonBound("hi", high),
            )
        }
        return GMResult.Ok(PlotInterval(low.lo, high.hi))
    }

    // JSXGraph 1.13.3: src/math/ia.js -> isInterval.
    internal fun isInterval(value: Any?): Boolean =
        value is PlotInterval || value is MutablePlotInterval

    // JSXGraph 1.13.3: src/math/ia.js -> add.
    internal fun add(
        first: PlotInterval,
        second: PlotInterval,
    ): PlotInterval =
        PlotInterval(
            lo = addLo(first.lo, second.lo),
            hi = addHi(first.hi, second.hi),
        )

    // JSXGraph 1.13.3: src/math/ia.js -> sub.
    internal fun subtract(
        first: PlotInterval,
        second: PlotInterval,
    ): PlotInterval =
        PlotInterval(
            lo = subLo(first.lo, second.hi),
            hi = subHi(first.hi, second.lo),
        )

    // JSXGraph 1.13.3: src/math/ia.js -> positive.
    internal fun positive(value: PlotInterval): PlotInterval =
        PlotInterval(value.lo, value.hi)

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
                                mulLo(firstLow, secondHigh),
                                mulLo(firstHigh, secondLow),
                            ),
                            max(
                                mulHi(firstLow, secondLow),
                                mulHi(firstHigh, secondHigh),
                            ),
                        )
                    } else {
                        PlotInterval(
                            mulLo(firstHigh, secondLow),
                            mulHi(firstLow, secondLow),
                        )
                    }
                } else if (secondHigh > 0.0) {
                    PlotInterval(
                        mulLo(firstLow, secondHigh),
                        mulHi(firstHigh, secondHigh),
                    )
                } else {
                    PlotInterval(0.0, 0.0)
                }
            } else if (secondLow < 0.0) {
                if (secondHigh > 0.0) {
                    PlotInterval(
                        mulLo(firstLow, secondHigh),
                        mulHi(firstLow, secondLow),
                    )
                } else {
                    PlotInterval(
                        mulLo(firstHigh, secondHigh),
                        mulHi(firstLow, secondLow),
                    )
                }
            } else if (secondHigh > 0.0) {
                PlotInterval(
                    mulLo(firstLow, secondHigh),
                    mulHi(firstHigh, secondLow),
                )
            } else {
                PlotInterval(0.0, 0.0)
            }
        } else if (firstHigh > 0.0) {
            if (secondLow < 0.0) {
                if (secondHigh > 0.0) {
                    PlotInterval(
                        mulLo(firstHigh, secondLow),
                        mulHi(firstHigh, secondHigh),
                    )
                } else {
                    PlotInterval(
                        mulLo(firstHigh, secondLow),
                        mulHi(firstLow, secondHigh),
                    )
                }
            } else if (secondHigh > 0.0) {
                PlotInterval(
                    mulLo(firstLow, secondLow),
                    mulHi(firstHigh, secondHigh),
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
                return divideZero(numerator)
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

    // JSXGraph 1.13.3: src/math/ia.js -> isSingleton / isEmpty /
    // isWhole / zeroIn / hasValue / hasInterval / intervalsOverlap.
    internal fun isSingleton(value: PlotInterval): Boolean =
        value.lo == value.hi

    internal fun isEmpty(value: PlotInterval): Boolean =
        value.lo > value.hi

    internal fun isWhole(value: PlotInterval): Boolean =
        value.lo == Double.NEGATIVE_INFINITY &&
            value.hi == Double.POSITIVE_INFINITY

    internal fun zeroIn(value: PlotInterval): Boolean =
        hasValue(value, 0.0)

    internal fun hasValue(
        interval: PlotInterval,
        value: Double,
    ): Boolean =
        !isEmpty(interval) &&
            interval.lo <= value &&
            value <= interval.hi

    internal fun hasInterval(
        first: PlotInterval,
        second: PlotInterval,
    ): Boolean =
        if (isEmpty(first)) {
            true
        } else {
            !isEmpty(second) &&
                second.lo <= first.lo &&
                first.hi <= second.hi
        }

    internal fun intervalsOverlap(
        first: PlotInterval,
        second: PlotInterval,
    ): Boolean =
        !isEmpty(first) &&
            !isEmpty(second) &&
            (
                first.lo <= second.lo && second.lo <= first.hi ||
                    second.lo <= first.lo && first.lo <= second.hi
                )

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
            return power(
                multiplicativeInverse(value),
                singleton(-power),
            )
        }
        if (power % 1.0 != 0.0) {
            return empty
        }
        return when {
            value.hi < 0.0 -> {
                val low = powLo(-value.hi, power)
                val high = powHi(-value.lo, power)
                if (power.toLong() and 1L == 1L) {
                    PlotInterval(-high, -low)
                } else {
                    PlotInterval(low, high)
                }
            }
            value.lo < 0.0 -> {
                if (power.toLong() and 1L == 1L) {
                    PlotInterval(
                        -powLo(-value.lo, power),
                        powHi(value.hi, power),
                    )
                } else {
                    PlotInterval(
                        0.0,
                        powHi(max(-value.lo, value.hi), power),
                    )
                }
            }
            else -> PlotInterval(
                powLo(value.lo, power),
                powHi(value.hi, power),
            )
        }
    }

    // JSXGraph 1.13.3: src/math/ia.js -> multiplicativeInverse.
    internal fun multiplicativeInverse(
        value: PlotInterval,
    ): PlotInterval {
        if (isEmpty(value)) {
            return empty
        }
        if (zeroIn(value)) {
            if (value.lo != 0.0) {
                return if (value.hi != 0.0) {
                    whole
                } else {
                    PlotInterval(
                        Double.NEGATIVE_INFINITY,
                        divHi(1.0, value.lo),
                    )
                }
            }
            if (value.hi != 0.0) {
                return PlotInterval(
                    divLo(1.0, value.hi),
                    Double.POSITIVE_INFINITY,
                )
            }
            return empty
        }
        return PlotInterval(
            divLo(1.0, value.hi),
            divHi(1.0, value.lo),
        )
    }

    // JSXGraph 1.13.3: src/math/ia.js -> sqrt.
    internal fun sqrt(value: PlotInterval): PlotInterval =
        nthRoot(value, 2.0)

    // JSXGraph 1.13.3: src/math/ia.js -> nthRoot.
    internal fun nthRoot(
        value: PlotInterval,
        root: Double,
    ): PlotInterval {
        if (isEmpty(value) || root < 0.0) {
            return empty
        }
        val power = 1.0 / root
        if (value.hi < 0.0) {
            return if (
                root % 1.0 == 0.0 &&
                root.toLong() and 1L == 1L
            ) {
                PlotInterval(
                    -powHi(-value.lo, power),
                    -powLo(-value.hi, power),
                )
            } else {
                empty
            }
        }
        if (value.lo < 0.0) {
            val positive = powHi(value.hi, power)
            return if (
                root % 1.0 == 0.0 &&
                root.toLong() and 1L == 1L
            ) {
                PlotInterval(
                    -powHi(-value.lo, power),
                    positive,
                )
            } else {
                PlotInterval(0.0, positive)
            }
        }
        return PlotInterval(
            powLo(value.lo, power),
            powHi(value.hi, power),
        )
    }

    // JSXGraph 1.13.3: src/math/ia.js -> exp.
    internal fun exp(value: PlotInterval): PlotInterval =
        if (isEmpty(value)) {
            empty
        } else {
            PlotInterval(expLo(value.lo), expHi(value.hi))
        }

    // JSXGraph 1.13.3: src/math/ia.js -> log / ln.
    internal fun log(value: PlotInterval): PlotInterval =
        if (isEmpty(value)) {
            empty
        } else {
            PlotInterval(
                if (value.lo <= 0.0) {
                    Double.NEGATIVE_INFINITY
                } else {
                    logLo(value.lo)
                },
                logHi(value.hi),
            )
        }

    internal fun ln(value: PlotInterval): PlotInterval = log(value)

    // JSXGraph 1.13.3: src/math/ia.js -> log10 / log2.
    internal fun log10(value: PlotInterval): PlotInterval =
        if (isEmpty(value)) {
            empty
        } else {
            divide(log(value), log(singleton(10.0)))
        }

    internal fun log2(value: PlotInterval): PlotInterval =
        if (isEmpty(value)) {
            empty
        } else {
            divide(log(value), log(singleton(2.0)))
        }

    // JSXGraph 1.13.3: src/math/ia.js -> hull / intersection / union /
    // difference / width.
    internal fun hull(
        first: PlotInterval,
        second: PlotInterval,
    ): PlotInterval =
        when {
            isEmpty(first) && isEmpty(second) -> empty
            isEmpty(first) -> second.copy()
            isEmpty(second) -> first.copy()
            else -> PlotInterval(
                min(first.lo, second.lo),
                max(first.hi, second.hi),
            )
        }

    internal fun intersection(
        first: PlotInterval,
        second: PlotInterval,
    ): PlotInterval {
        if (isEmpty(first) || isEmpty(second)) {
            return empty
        }
        val low = max(first.lo, second.lo)
        val high = min(first.hi, second.hi)
        return if (low <= high) {
            PlotInterval(low, high)
        } else {
            empty
        }
    }

    internal fun union(
        first: PlotInterval,
        second: PlotInterval,
    ): GMResult<PlotInterval, IntervalArithmeticError> =
        if (!intervalsOverlap(first, second)) {
            GMResult.Err(
                IntervalArithmeticError.NonOverlappingUnion(
                    first,
                    second,
                ),
            )
        } else {
            GMResult.Ok(
                PlotInterval(
                    min(first.lo, second.lo),
                    max(first.hi, second.hi),
                ),
            )
        }

    internal fun difference(
        source: PlotInterval,
        removed: PlotInterval,
    ): GMResult<PlotInterval, IntervalArithmeticError> {
        if (isEmpty(source) || isWhole(removed)) {
            return GMResult.Ok(empty)
        }
        if (!intervalsOverlap(source, removed)) {
            return GMResult.Ok(source.copy())
        }
        if (
            source.lo < removed.lo &&
            removed.hi < source.hi
        ) {
            return GMResult.Err(
                IntervalArithmeticError.DisconnectedDifference(
                    source,
                    removed,
                ),
            )
        }
        if (
            removed.lo <= source.lo &&
            removed.hi == Double.POSITIVE_INFINITY ||
            removed.hi >= source.hi &&
            removed.lo == Double.NEGATIVE_INFINITY
        ) {
            return GMResult.Ok(empty)
        }
        return GMResult.Ok(
            if (removed.lo <= source.lo) {
                PlotInterval(next(removed.hi), source.hi)
            } else {
                PlotInterval(source.lo, previous(removed.lo))
            },
        )
    }

    internal fun width(value: PlotInterval): Double =
        if (isEmpty(value)) {
            0.0
        } else {
            subHi(value.hi, value.lo)
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

    // JSXGraph 1.13.3: src/math/ia.js -> onlyInfinity.
    internal fun onlyInfinity(value: PlotInterval): Boolean =
        !value.lo.isFinite() && value.lo == value.hi

    // JSXGraph 1.13.3: src/math/ia.js -> _handleNegative.
    private fun handleNegative(value: PlotInterval): PlotInterval {
        if (value.lo >= 0.0) {
            return value
        }
        if (value.lo == Double.NEGATIVE_INFINITY) {
            return PlotInterval(0.0, Double.POSITIVE_INFINITY)
        }
        val periods = ceil(-value.lo / PI_TWICE_LOW)
        return PlotInterval(
            value.lo + PI_TWICE_LOW * periods,
            value.hi + PI_TWICE_LOW * periods,
        )
    }

    // JSXGraph 1.13.3: src/math/ia.js -> cos.
    internal fun cos(value: PlotInterval): PlotInterval {
        if (isEmpty(value) || onlyInfinity(value)) {
            return empty
        }
        val reduced = modulo(handleNegative(value), piTwice)
        if (width(reduced) >= piTwice.lo) {
            return PlotInterval(-1.0, 1.0)
        }
        if (reduced.lo >= PI_HIGH) {
            return negative(cos(subtract(reduced, pi)))
        }
        val low = cosLo(reduced.hi)
        val high = cosHi(reduced.lo)
        return when {
            reduced.hi <= PI_LOW -> PlotInterval(low, high)
            reduced.hi <= piTwice.lo ->
                PlotInterval(-1.0, max(low, high))
            else -> PlotInterval(-1.0, 1.0)
        }
    }

    // JSXGraph 1.13.3: src/math/ia.js -> sin.
    internal fun sin(value: PlotInterval): PlotInterval =
        if (isEmpty(value) || onlyInfinity(value)) {
            empty
        } else {
            cos(subtract(value, piHalf))
        }

    // JSXGraph 1.13.3: src/math/ia.js -> tan.
    internal fun tan(value: PlotInterval): PlotInterval {
        if (isEmpty(value) || onlyInfinity(value)) {
            return empty
        }
        var reduced = modulo(handleNegative(value), pi)
        if (reduced.lo >= PI_HALF_LOW) {
            reduced = subtract(reduced, pi)
        }
        return if (
            reduced.lo <= -PI_HALF_LOW ||
            reduced.hi >= PI_HALF_LOW
        ) {
            whole
        } else {
            PlotInterval(tanLo(reduced.lo), tanHi(reduced.hi))
        }
    }

    // JSXGraph 1.13.3: src/math/ia.js -> asin.
    internal fun asin(value: PlotInterval): PlotInterval =
        if (isEmpty(value) || value.hi < -1.0 || value.lo > 1.0) {
            empty
        } else {
            PlotInterval(
                if (value.lo <= -1.0) {
                    -PI_HALF_HIGH
                } else {
                    asinLo(value.lo)
                },
                if (value.hi >= 1.0) {
                    PI_HALF_HIGH
                } else {
                    asinHi(value.hi)
                },
            )
        }

    // JSXGraph 1.13.3: src/math/ia.js -> acos.
    internal fun acos(value: PlotInterval): PlotInterval =
        if (isEmpty(value) || value.hi < -1.0 || value.lo > 1.0) {
            empty
        } else {
            PlotInterval(
                if (value.hi >= 1.0) {
                    0.0
                } else {
                    acosLo(value.hi)
                },
                if (value.lo <= -1.0) {
                    PI_HIGH
                } else {
                    acosHi(value.lo)
                },
            )
        }

    // JSXGraph 1.13.3: src/math/ia.js -> atan.
    internal fun atan(value: PlotInterval): PlotInterval =
        if (isEmpty(value)) {
            empty
        } else {
            PlotInterval(atanLo(value.lo), atanHi(value.hi))
        }

    // JSXGraph 1.13.3: src/math/ia.js -> acot / sinh / cosh / tanh.
    internal fun acot(value: PlotInterval): PlotInterval =
        if (isEmpty(value)) {
            empty
        } else {
            PlotInterval(acotLo(value.lo), acotHi(value.hi))
        }

    internal fun sinh(value: PlotInterval): PlotInterval =
        if (isEmpty(value)) {
            empty
        } else {
            PlotInterval(sinhLo(value.lo), sinhHi(value.hi))
        }

    internal fun cosh(value: PlotInterval): PlotInterval =
        when {
            isEmpty(value) -> empty
            value.hi < 0.0 ->
                PlotInterval(coshLo(value.hi), coshHi(value.lo))
            value.lo >= 0.0 ->
                PlotInterval(coshLo(value.lo), coshHi(value.hi))
            else ->
                PlotInterval(
                    1.0,
                    coshHi(
                        if (-value.lo > value.hi) {
                            value.lo
                        } else {
                            value.hi
                        },
                    ),
                )
        }

    internal fun tanh(value: PlotInterval): PlotInterval =
        if (isEmpty(value)) {
            empty
        } else {
            PlotInterval(tanhLo(value.lo), tanhHi(value.hi))
        }

    // JSXGraph 1.13.3: src/math/ia.js -> equal / notEqual / lt / gt /
    // leq / geq.
    internal fun equal(
        first: PlotInterval,
        second: PlotInterval,
    ): Boolean =
        if (isEmpty(first)) {
            isEmpty(second)
        } else {
            !isEmpty(second) &&
                first.lo == second.lo &&
                first.hi == second.hi
        }

    internal fun notEqual(
        first: PlotInterval,
        second: PlotInterval,
    ): Boolean =
        if (isEmpty(first)) {
            !isEmpty(second)
        } else {
            isEmpty(second) ||
                first.hi < second.lo ||
                first.lo > second.hi
        }

    internal fun lessThan(
        first: PlotInterval,
        second: PlotInterval,
    ): Boolean =
        !isEmpty(first) &&
            !isEmpty(second) &&
            first.hi < second.lo

    internal fun greaterThan(
        first: PlotInterval,
        second: PlotInterval,
    ): Boolean =
        !isEmpty(first) &&
            !isEmpty(second) &&
            first.lo > second.hi

    internal fun lessThanOrEqual(
        first: PlotInterval,
        second: PlotInterval,
    ): Boolean =
        !isEmpty(first) &&
            !isEmpty(second) &&
            first.hi <= second.lo

    internal fun greaterThanOrEqual(
        first: PlotInterval,
        second: PlotInterval,
    ): Boolean =
        !isEmpty(first) &&
            !isEmpty(second) &&
            first.lo >= second.hi

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

    // JSXGraph 1.13.3: src/math/ia.js -> disable / enable.
    internal fun disable() {
        outwardRoundingEnabled = false
    }

    internal fun enable() {
        outwardRoundingEnabled = true
    }

    // JSXGraph 1.13.3: src/math/ia.js -> divPositive.
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
                    divHi(numerator.hi, denominatorHigh),
                )
            else ->
                PlotInterval(
                    divLo(numerator.lo, denominatorHigh),
                    Double.POSITIVE_INFINITY,
                )
        }

    // JSXGraph 1.13.3: src/math/ia.js -> divNegative.
    private fun divideNegative(
        numerator: PlotInterval,
        denominatorLow: Double,
    ): PlotInterval =
        when {
            numerator.lo == 0.0 && numerator.hi == 0.0 -> numerator
            containsZero(numerator) -> whole
            numerator.hi < 0.0 ->
                PlotInterval(
                    divLo(numerator.hi, denominatorLow),
                    Double.POSITIVE_INFINITY,
                )
            else ->
                PlotInterval(
                    Double.NEGATIVE_INFINITY,
                    divHi(numerator.lo, denominatorLow),
                )
        }

    // JSXGraph 1.13.3: src/math/ia.js -> divZero.
    private fun divideZero(
        numerator: PlotInterval,
    ): PlotInterval =
        if (
            numerator.lo == 0.0 &&
            numerator.hi == 0.0
        ) {
            numerator
        } else {
            whole
        }

    // JSXGraph 1.13.3: src/math/ia.js -> divNonZero.
    private fun divideNonZero(
        numerator: PlotInterval,
        denominator: PlotInterval,
    ): PlotInterval {
        val low = numerator.lo
        val high = numerator.hi
        val denominatorLow = denominator.lo
        val denominatorHigh = denominator.hi
        return if (high < 0.0) {
            if (denominatorHigh < 0.0) {
                PlotInterval(
                    divLo(high, denominatorLow),
                    divHi(low, denominatorHigh),
                )
            } else {
                PlotInterval(
                    divLo(low, denominatorLow),
                    divHi(high, denominatorHigh),
                )
            }
        } else if (low < 0.0) {
            if (denominatorHigh < 0.0) {
                PlotInterval(
                    divLo(high, denominatorHigh),
                    divHi(low, denominatorHigh),
                )
            } else {
                PlotInterval(
                    divLo(low, denominatorLow),
                    divHi(high, denominatorLow),
                )
            }
        } else if (denominatorHigh < 0.0) {
            PlotInterval(
                divLo(high, denominatorHigh),
                divHi(low, denominatorLow),
            )
        } else {
            PlotInterval(
                divLo(low, denominatorHigh),
                divHi(high, denominatorLow),
            )
        }
    }

    private fun containsZero(value: PlotInterval): Boolean =
        zeroIn(value)

    // JSXGraph 1.13.3: src/math/ia.js -> identity.
    private fun identity(value: Double): Double = value

    // JSXGraph 1.13.3: src/math/ia.js -> _prev / prev.
    private fun previous(value: Double): Double =
        if (outwardRoundingEnabled) {
            nextAfter(value, Double.NEGATIVE_INFINITY)
        } else {
            identity(value)
        }

    internal fun prev(value: Double): Double = previous(value)

    // JSXGraph 1.13.3: src/math/ia.js -> _next / next.
    internal fun next(value: Double): Double =
        if (outwardRoundingEnabled) {
            nextAfter(value, Double.POSITIVE_INFINITY)
        } else {
            identity(value)
        }

    // JSXGraph 1.13.3: src/math/ia.js -> addLo.
    private fun addLo(first: Double, second: Double): Double =
        previous(first + second)

    // JSXGraph 1.13.3: src/math/ia.js -> addHi.
    private fun addHi(first: Double, second: Double): Double =
        next(first + second)

    // JSXGraph 1.13.3: src/math/ia.js -> subLo.
    private fun subLo(first: Double, second: Double): Double =
        previous(first - second)

    // JSXGraph 1.13.3: src/math/ia.js -> subHi.
    private fun subHi(first: Double, second: Double): Double =
        next(first - second)

    // JSXGraph 1.13.3: src/math/ia.js -> mulLo.
    private fun mulLo(first: Double, second: Double): Double =
        previous(first * second)

    // JSXGraph 1.13.3: src/math/ia.js -> mulHi.
    private fun mulHi(first: Double, second: Double): Double =
        next(first * second)

    // JSXGraph 1.13.3: src/math/ia.js -> divLo.
    private fun divLo(first: Double, second: Double): Double =
        previous(first / second)

    // JSXGraph 1.13.3: src/math/ia.js -> divHi.
    private fun divHi(first: Double, second: Double): Double =
        next(first / second)

    // JSXGraph 1.13.3: src/math/ia.js -> toInteger.
    private fun toInteger(value: Double): Double =
        if (value < 0.0) ceil(value) else floor(value)

    // JSXGraph 1.13.3: src/math/ia.js -> intLo.
    private fun intLo(value: Double): Double =
        toInteger(previous(value))

    // JSXGraph 1.13.3: src/math/ia.js -> intHi.
    private fun intHi(value: Double): Double =
        toInteger(next(value))

    // JSXGraph 1.13.3: src/math/ia.js -> logLo.
    private fun logLo(value: Double): Double = previous(ln(value))

    // JSXGraph 1.13.3: src/math/ia.js -> logHi.
    private fun logHi(value: Double): Double = next(ln(value))

    // JSXGraph 1.13.3: src/math/ia.js -> expLo.
    private fun expLo(value: Double): Double = previous(exp(value))

    // JSXGraph 1.13.3: src/math/ia.js -> expHi.
    private fun expHi(value: Double): Double = next(exp(value))

    // JSXGraph 1.13.3: src/math/ia.js -> sinLo.
    private fun sinLo(value: Double): Double = previous(sin(value))

    // JSXGraph 1.13.3: src/math/ia.js -> sinHi.
    private fun sinHi(value: Double): Double = next(sin(value))

    // JSXGraph 1.13.3: src/math/ia.js -> cosLo.
    private fun cosLo(value: Double): Double = previous(cos(value))

    // JSXGraph 1.13.3: src/math/ia.js -> cosHi.
    private fun cosHi(value: Double): Double = next(cos(value))

    // JSXGraph 1.13.3: src/math/ia.js -> tanLo.
    private fun tanLo(value: Double): Double = previous(tan(value))

    // JSXGraph 1.13.3: src/math/ia.js -> tanHi.
    private fun tanHi(value: Double): Double = next(tan(value))

    // JSXGraph 1.13.3: src/math/ia.js -> asinLo.
    private fun asinLo(value: Double): Double = previous(asin(value))

    // JSXGraph 1.13.3: src/math/ia.js -> asinHi.
    private fun asinHi(value: Double): Double = next(asin(value))

    // JSXGraph 1.13.3: src/math/ia.js -> acosLo.
    private fun acosLo(value: Double): Double = previous(acos(value))

    // JSXGraph 1.13.3: src/math/ia.js -> acosHi.
    private fun acosHi(value: Double): Double = next(acos(value))

    // JSXGraph 1.13.3: src/math/ia.js -> sqrtLo.
    private fun sqrtLo(value: Double): Double = previous(sqrt(value))

    // JSXGraph 1.13.3: src/math/ia.js -> sqrtHi.
    private fun sqrtHi(value: Double): Double = next(sqrt(value))

    // JSXGraph 1.13.3: src/math/ia.js -> powLo.
    private fun powLo(
        value: Double,
        power: Double,
    ): Double {
        if (power % 1.0 != 0.0) {
            return previous(value.pow(power))
        }
        var base = value
        var exponent = power.toLong().toInt()
        var result = if (exponent and 1 == 1) base else 1.0
        exponent = exponent shr 1
        while (exponent > 0) {
            base = mulLo(base, base)
            if (exponent and 1 == 1) {
                result = mulLo(base, result)
            }
            exponent = exponent shr 1
        }
        return result
    }

    // JSXGraph 1.13.3: src/math/ia.js -> powHi.
    private fun powHi(
        value: Double,
        power: Double,
    ): Double {
        if (power % 1.0 != 0.0) {
            return next(value.pow(power))
        }
        var base = value
        var exponent = power.toLong().toInt()
        var result = if (exponent and 1 == 1) base else 1.0
        exponent = exponent shr 1
        while (exponent > 0) {
            base = mulHi(base, base)
            if (exponent and 1 == 1) {
                result = mulHi(base, result)
            }
            exponent = exponent shr 1
        }
        return result
    }

    // JSXGraph 1.13.3: src/math/ia.js -> atanLo.
    private fun atanLo(value: Double): Double = previous(atan(value))

    // JSXGraph 1.13.3: src/math/ia.js -> atanHi.
    private fun atanHi(value: Double): Double = next(atan(value))

    private fun acotLo(value: Double): Double =
        previous(PI * 0.5 - atan(value))

    private fun acotHi(value: Double): Double =
        next(PI * 0.5 - atan(value))

    // JSXGraph 1.13.3: src/math/ia.js -> sinhLo.
    private fun sinhLo(value: Double): Double = previous(sinh(value))

    // JSXGraph 1.13.3: src/math/ia.js -> sinhHi.
    private fun sinhHi(value: Double): Double = next(sinh(value))

    // JSXGraph 1.13.3: src/math/ia.js -> coshLo.
    private fun coshLo(value: Double): Double = previous(cosh(value))

    // JSXGraph 1.13.3: src/math/ia.js -> coshHi.
    private fun coshHi(value: Double): Double = next(cosh(value))

    // JSXGraph 1.13.3: src/math/ia.js -> tanhLo.
    private fun tanhLo(value: Double): Double = previous(tanh(value))

    // JSXGraph 1.13.3: src/math/ia.js -> tanhHi.
    private fun tanhHi(value: Double): Double = next(tanh(value))

    // JSXGraph 1.13.3: src/math/ia.js -> nextafter.
    private fun nextAfter(
        value: Double,
        direction: Double,
    ): Double {
        if (value.isNaN() || direction.isNaN()) {
            return Double.NaN
        }
        if (value == direction) {
            return value
        }
        if (value == 0.0) {
            return if (direction < 0.0) {
                -Double.MIN_VALUE
            } else {
                Double.MIN_VALUE
            }
        }
        val bits = value.toRawBits()
        val nextBits = if (
            (direction > value) == (value > 0.0)
        ) {
            bits + 1L
        } else {
            bits - 1L
        }
        return Double.fromBits(nextBits)
    }
}
