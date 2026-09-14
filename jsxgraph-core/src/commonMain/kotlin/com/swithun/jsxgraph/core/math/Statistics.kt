/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/statistics.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Coords
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

sealed interface StatisticsError {
    data object EmptyData : StatisticsError

    data class DimensionMismatch(
        val valueCount: Int,
        val weightCount: Int,
    ) : StatisticsError
}

data class BoxPlotSummary(
    val minimum: Double,
    val lowerQuartile: Double,
    val median: Double,
    val upperQuartile: Double,
    val maximum: Double,
    val outliers: DoubleArray,
)

object Statistics {
    // JSXGraph: src/math/statistics.js -> sum
    fun sum(values: DoubleArray): Double {
        var result = 0.0
        for (value in values) {
            result += value
        }
        return result
    }

    // JSXGraph: src/math/statistics.js -> prod
    fun prod(values: DoubleArray): Double {
        var result = 1.0
        for (value in values) {
            result *= value
        }
        return result
    }

    // JSXGraph: src/math/statistics.js -> mean
    fun mean(values: DoubleArray): Double =
        if (values.isNotEmpty()) sum(values) / values.size else 0.0

    // JSXGraph: src/math/statistics.js -> median
    fun median(values: DoubleArray): Double {
        if (values.isEmpty()) {
            return 0.0
        }

        val sorted = values.sortedArray()
        val length = sorted.size
        return if (length and 1 == 1) {
            sorted[length / 2]
        } else {
            (sorted[length / 2 - 1] + sorted[length / 2]) * 0.5
        }
    }

    // JSXGraph: src/math/statistics.js -> _getPercentiles
    private fun getPercentiles(
        sortedValues: DoubleArray,
        percentiles: DoubleArray,
    ): DoubleArray = DoubleArray(percentiles.size) { index ->
        val rank = sortedValues.size * percentiles[index] * 0.01
        val rankIndex = rank.toInt()
        if (rankIndex.toDouble() == rank) {
            (
                sortedValues.valueOrNaN(rankIndex - 1) +
                    sortedValues.valueOrNaN(rankIndex)
            ) * 0.5
        } else {
            sortedValues.valueOrNaN(rankIndex)
        }
    }

    // JSXGraph: src/math/statistics.js -> percentile
    fun percentile(
        values: DoubleArray,
        percentile: Double,
    ): Double {
        if (values.isEmpty()) {
            return 0.0
        }
        val sorted = values.filterNot(Double::isNaN).sorted().toDoubleArray()
        return getPercentiles(sorted, doubleArrayOf(percentile))[0]
    }

    fun percentiles(
        values: DoubleArray,
        percentiles: DoubleArray,
    ): DoubleArray {
        if (values.isEmpty()) {
            return DoubleArray(percentiles.size)
        }
        val sorted = values.filterNot(Double::isNaN).sorted().toDoubleArray()
        return getPercentiles(sorted, percentiles)
    }

    // JSXGraph: src/math/statistics.js -> boxplot
    fun boxplot(
        values: DoubleArray,
        coefficient: Double = 1.5,
    ): GMResult<BoxPlotSummary, StatisticsError> {
        if (values.isEmpty()) {
            return GMResult.Err(StatisticsError.EmptyData)
        }

        val factor = if (coefficient.isNaN()) 1.5 else coefficient
        val sorted = values.filterNot(Double::isNaN).sorted().toDoubleArray()
        val quartiles = getPercentiles(sorted, doubleArrayOf(25.0, 50.0, 75.0))
        val interquartileRange = factor * (quartiles[2] - quartiles[0])
        val inside = mutableListOf<Double>()
        val outliers = mutableListOf<Double>()
        if (factor == 0.0) {
            inside += sorted.toList()
        } else {
            for (value in sorted) {
                if (
                    value >= quartiles[0] - interquartileRange &&
                    value <= quartiles[2] + interquartileRange
                ) {
                    inside += value
                } else {
                    outliers += value
                }
            }
        }

        val insideArray = inside.toDoubleArray()
        return GMResult.Ok(
            BoxPlotSummary(
                minimum = min(insideArray),
                lowerQuartile = quartiles[0],
                median = quartiles[1],
                upperQuartile = quartiles[2],
                maximum = max(insideArray),
                outliers = outliers.toDoubleArray(),
            ),
        )
    }

    // JSXGraph: src/math/statistics.js -> variance
    fun variance(values: DoubleArray): Double {
        if (values.size <= 1) {
            return 0.0
        }

        val mean = mean(values)
        var result = 0.0
        for (value in values) {
            result += (value - mean) * (value - mean)
        }
        return result / (values.size - 1)
    }

    // JSXGraph: src/math/statistics.js -> sd
    fun sd(values: DoubleArray): Double = sqrt(variance(values))

    // JSXGraph: src/math/statistics.js -> weightedMean
    fun weightedMean(
        values: DoubleArray,
        weights: DoubleArray,
    ): GMResult<Double, StatisticsError> {
        if (values.size != weights.size) {
            return GMResult.Err(
                StatisticsError.DimensionMismatch(
                    valueCount = values.size,
                    weightCount = weights.size,
                ),
            )
        }
        return GMResult.Ok(if (values.isNotEmpty()) mean(multiply(values, weights)) else 0.0)
    }

    // JSXGraph: src/math/statistics.js -> max
    fun max(values: DoubleArray): Double {
        if (values.isEmpty()) {
            return Double.NEGATIVE_INFINITY
        }
        var result = Double.NEGATIVE_INFINITY
        for (value in values) {
            if (value.isNaN()) {
                return Double.NaN
            }
            if (value > result) {
                result = value
            }
        }
        return result
    }

    // JSXGraph: src/math/statistics.js -> min
    fun min(values: DoubleArray): Double {
        if (values.isEmpty()) {
            return Double.POSITIVE_INFINITY
        }
        var result = Double.POSITIVE_INFINITY
        for (value in values) {
            if (value.isNaN()) {
                return Double.NaN
            }
            if (value < result) {
                result = value
            }
        }
        return result
    }

    // JSXGraph: src/math/statistics.js -> range
    fun range(values: DoubleArray): DoubleArray = doubleArrayOf(min(values), max(values))

    // JSXGraph: src/math/statistics.js -> abs
    fun abs(value: Double): Double = kotlin.math.abs(value)

    fun abs(values: DoubleArray): DoubleArray =
        DoubleArray(values.size) { index -> kotlin.math.abs(values[index]) }

    // JSXGraph: src/math/statistics.js -> add
    fun add(first: Double, second: Double): Double = first + second

    fun add(first: DoubleArray, second: Double): DoubleArray =
        DoubleArray(first.size) { index -> first[index] + second }

    fun add(first: Double, second: DoubleArray): DoubleArray =
        DoubleArray(second.size) { index -> first + second[index] }

    fun add(first: DoubleArray, second: DoubleArray): DoubleArray =
        zipShortest(first, second) { left, right -> left + right }

    // JSXGraph: src/math/statistics.js -> div
    fun div(first: Double, second: Double): Double = first / second

    fun div(first: DoubleArray, second: Double): DoubleArray =
        DoubleArray(first.size) { index -> first[index] / second }

    fun div(first: Double, second: DoubleArray): DoubleArray =
        DoubleArray(second.size) { index -> first / second[index] }

    fun div(first: DoubleArray, second: DoubleArray): DoubleArray =
        zipShortest(first, second) { left, right -> left / right }

    // JSXGraph: src/math/statistics.js -> mod
    fun mod(
        first: Double,
        second: Double,
        mathematical: Boolean = false,
    ): Double = if (mathematical) Mat.mod(first, second) else first % second

    fun mod(
        first: DoubleArray,
        second: Double,
        mathematical: Boolean = false,
    ): DoubleArray =
        DoubleArray(first.size) { index -> mod(first[index], second, mathematical) }

    fun mod(
        first: Double,
        second: DoubleArray,
        mathematical: Boolean = false,
    ): DoubleArray =
        DoubleArray(second.size) { index -> mod(first, second[index], mathematical) }

    fun mod(
        first: DoubleArray,
        second: DoubleArray,
        mathematical: Boolean = false,
    ): DoubleArray =
        zipShortest(first, second) { left, right -> mod(left, right, mathematical) }

    // JSXGraph: src/math/statistics.js -> multiply
    fun multiply(first: Double, second: Double): Double = first * second

    fun multiply(first: DoubleArray, second: Double): DoubleArray =
        DoubleArray(first.size) { index -> first[index] * second }

    fun multiply(first: Double, second: DoubleArray): DoubleArray =
        DoubleArray(second.size) { index -> first * second[index] }

    fun multiply(first: DoubleArray, second: DoubleArray): DoubleArray =
        zipShortest(first, second) { left, right -> left * right }

    // JSXGraph: src/math/statistics.js -> subtract
    fun subtract(first: Double, second: Double): Double = first - second

    fun subtract(first: DoubleArray, second: Double): DoubleArray =
        DoubleArray(first.size) { index -> first[index] - second }

    fun subtract(first: Double, second: DoubleArray): DoubleArray =
        DoubleArray(second.size) { index -> first - second[index] }

    fun subtract(first: DoubleArray, second: DoubleArray): DoubleArray =
        zipShortest(first, second) { left, right -> left - right }

    // JSXGraph: src/math/statistics.js -> TheilSenRegression
    @Suppress("FunctionName")
    internal fun TheilSenRegression(coordinates: List<Coords>): DoubleArray {
        val slopes = DoubleArray(coordinates.size)
        val intercepts = DoubleArray(coordinates.size)
        for (firstIndex in coordinates.indices) {
            val temporarySlopes = mutableListOf<Double>()
            for (secondIndex in coordinates.indices) {
                val xDifference =
                    coordinates[secondIndex].usrCoords[1] -
                        coordinates[firstIndex].usrCoords[1]
                if (abs(xDifference) > Mat.eps) {
                    while (temporarySlopes.size <= secondIndex) {
                        temporarySlopes += Double.NaN
                    }
                    temporarySlopes[secondIndex] =
                        (
                            coordinates[secondIndex].usrCoords[2] -
                                coordinates[firstIndex].usrCoords[2]
                        ) / xDifference
                }
            }
            slopes[firstIndex] = median(temporarySlopes.toDoubleArray())
            intercepts[firstIndex] =
                coordinates[firstIndex].usrCoords[2] -
                slopes[firstIndex] * coordinates[firstIndex].usrCoords[1]
        }
        return doubleArrayOf(median(intercepts), median(slopes), -1.0)
    }

    private fun zipShortest(
        first: DoubleArray,
        second: DoubleArray,
        operation: (Double, Double) -> Double,
    ): DoubleArray =
        DoubleArray(min(first.size, second.size)) { index ->
            operation(first[index], second[index])
        }

    private fun DoubleArray.valueOrNaN(index: Int): Double =
        if (index in indices) this[index] else Double.NaN
}
