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
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.random.Random

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

fun interface RandomSource {
    fun nextDouble(): Double
}

data class HistogramOptions(
    val bins: Int = 10,
    val range: ClosedFloatingPointRange<Double>? = null,
    val density: Boolean = false,
    val cumulative: Boolean = false,
)

data class HistogramResult(
    val counts: DoubleArray,
    val bins: DoubleArray,
)

object Statistics {
    private val defaultRandomSource = RandomSource { Random.nextDouble() }
    private var hasGaussianSpare = false
    private var gaussianSpare = 0.0

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

    // JSXGraph: src/math/statistics.js -> divide (deprecated alias)
    @Deprecated("Use div")
    fun divide(first: Double, second: Double): Double = div(first, second)

    @Deprecated("Use div")
    fun divide(first: DoubleArray, second: Double): DoubleArray = div(first, second)

    @Deprecated("Use div")
    fun divide(first: Double, second: DoubleArray): DoubleArray = div(first, second)

    @Deprecated("Use div")
    fun divide(first: DoubleArray, second: DoubleArray): DoubleArray = div(first, second)

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

    // JSXGraph: src/math/statistics.js -> generateGaussian / randomNormal
    fun generateGaussian(
        mean: Double,
        standardDeviation: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        if (hasGaussianSpare) {
            hasGaussianSpare = false
            return gaussianSpare * standardDeviation + mean
        }

        var first: Double
        var second: Double
        var squareSum: Double
        do {
            first = random.nextDouble() * 2.0 - 1.0
            second = random.nextDouble() * 2.0 - 1.0
            squareSum = first * first + second * second
        } while (squareSum >= 1.0 || squareSum == 0.0)

        val scale = sqrt(-2.0 * ln(squareSum) / squareSum)
        gaussianSpare = second * scale
        hasGaussianSpare = true
        return mean + standardDeviation * first * scale
    }

    fun randomNormal(
        mean: Double,
        standardDeviation: Double,
        random: RandomSource = defaultRandomSource,
    ): Double = generateGaussian(mean, standardDeviation, random)

    // JSXGraph: src/math/statistics.js -> randomUniform
    fun randomUniform(
        minimum: Double,
        maximum: Double,
        random: RandomSource = defaultRandomSource,
    ): Double = random.nextDouble() * (maximum - minimum) + minimum

    // JSXGraph: src/math/statistics.js -> randomExponential
    fun randomExponential(
        lambda: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        if (lambda <= 0.0) {
            return Double.NaN
        }

        var value: Double
        do {
            value = random.nextDouble()
        } while (value == 0.0)
        return -ln(value) / lambda
    }

    // JSXGraph: src/math/statistics.js -> randomGamma
    fun randomGamma(
        shape: Double,
        scale: Double = 1.0,
        threshold: Double = 0.0,
        random: RandomSource = defaultRandomSource,
    ): Double {
        if (shape <= 0.0) {
            return Double.NaN
        }

        val resolvedScale = if (scale == 0.0 || scale.isNaN()) 1.0 else scale
        val resolvedThreshold = if (threshold == 0.0 || threshold.isNaN()) 0.0 else threshold
        if (shape == 1.0) {
            return resolvedScale * randomExponential(1.0, random) + resolvedThreshold
        }

        var x: Double
        if (shape < 1.0) {
            val split = E / (shape + E)
            var acceptance: Double
            var draw: Double
            do {
                val branch = random.nextDouble()
                do {
                    draw = random.nextDouble()
                } while (draw == 0.0)
                if (branch < split) {
                    x = draw.pow(1.0 / shape)
                    acceptance = exp(-x)
                } else {
                    x = 1.0 - ln(draw)
                    acceptance = x.pow(shape - 1.0)
                }
                draw = random.nextDouble()
            } while (draw >= acceptance)
            return resolvedScale * x + resolvedThreshold
        }

        var tangent: Double
        var acceptanceDraw: Double
        do {
            tangent = tan(kotlin.math.PI * random.nextDouble())
            x = sqrt(2.0 * shape - 1.0) * tangent + shape - 1.0
            if (x > 0.0) {
                acceptanceDraw = random.nextDouble()
            } else {
                acceptanceDraw = Double.POSITIVE_INFINITY
                continue
            }
        } while (
            x <= 0.0 ||
            acceptanceDraw >
            (1.0 + tangent * tangent) *
            exp(
                (shape - 1.0) * ln(x / (shape - 1.0)) -
                    sqrt(2.0 * shape - 1.0) * tangent,
            )
        )
        return resolvedScale * x + resolvedThreshold
    }

    // JSXGraph: src/math/statistics.js -> randomBeta
    fun randomBeta(
        alpha: Double,
        beta: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        if (alpha <= 0.0 || beta <= 0.0) {
            return Double.NaN
        }

        val first = randomGamma(alpha, random = random)
        val second = randomGamma(beta, random = random)
        return first / (first + second)
    }

    // JSXGraph: src/math/statistics.js -> randomChisquare
    fun randomChisquare(
        degreesOfFreedom: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        if (degreesOfFreedom <= 0.0) {
            return Double.NaN
        }
        return 2.0 * randomGamma(degreesOfFreedom * 0.5, random = random)
    }

    // JSXGraph: src/math/statistics.js -> randomF
    fun randomF(
        numeratorDegrees: Double,
        denominatorDegrees: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        if (numeratorDegrees <= 0.0 || denominatorDegrees <= 0.0) {
            return Double.NaN
        }

        val numerator = randomChisquare(numeratorDegrees, random)
        val denominator = randomChisquare(denominatorDegrees, random)
        return numerator * denominatorDegrees / (denominator * numeratorDegrees)
    }

    // JSXGraph: src/math/statistics.js -> randomT
    fun randomT(
        degreesOfFreedom: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        if (degreesOfFreedom <= 0.0) {
            return Double.NaN
        }

        val normal = randomNormal(0.0, 1.0, random)
        val chiSquare = randomChisquare(degreesOfFreedom, random)
        return normal / sqrt(chiSquare / degreesOfFreedom)
    }

    // JSXGraph: src/math/statistics.js -> randomBinomial
    fun randomBinomial(
        trials: Double,
        probability: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        if (probability < 0.0 || probability > 1.0 || trials < 0.0) {
            return Double.NaN
        }
        if (probability == 0.0 || trials == 0.0) {
            return 0.0
        }
        if (probability == 1.0) {
            return trials
        }
        if (trials == 1.0) {
            return if (random.nextDouble() < probability) 1.0 else 0.0
        }
        if (probability > 0.5) {
            return trials - randomBinomial(trials, 1.0 - probability, random)
        }

        if (trials < 100.0) {
            var result = -1.0
            var consumedTrials = 0.0
            val logFailureProbability = ln(1.0 - probability)
            if (logFailureProbability == 0.0) {
                return 0.0
            }
            do {
                result += 1.0
                consumedTrials += floor(ln(random.nextDouble()) / logFailureProbability) + 1.0
            } while (consumedTrials < trials)
            return result
        }

        val firstShape = 1.0 + floor(trials * 0.5)
        val secondShape = trials - firstShape + 1.0
        val beta = randomBeta(firstShape, secondShape, random)
        return if (beta >= probability) {
            randomBinomial(firstShape - 1.0, probability / beta, random)
        } else {
            firstShape +
                randomBinomial(
                    secondShape - 1.0,
                    (probability - beta) / (1.0 - beta),
                    random,
                )
        }
    }

    // JSXGraph: src/math/statistics.js -> randomGeometric
    fun randomGeometric(
        probability: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        if (probability < 0.0 || probability > 1.0) {
            return Double.NaN
        }
        return ceil(ln(random.nextDouble()) / ln(1.0 - probability))
    }

    // JSXGraph: src/math/statistics.js -> randomPoisson
    fun randomPoisson(
        mean: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        if (mean <= 0.0) {
            return Double.NaN
        }

        if (mean < 10.0) {
            val threshold = exp(-mean)
            var count = 0
            var product = 1.0
            do {
                product *= random.nextDouble()
                count += 1
            } while (product > threshold)
            return (count - 1).toDouble()
        }

        val shape = floor(7.0 / 8.0 * mean)
        val gamma = randomGamma(shape, random = random)
        return if (gamma < mean) {
            shape + randomPoisson(mean - gamma, random)
        } else {
            randomBinomial(shape - 1.0, mean / gamma, random)
        }
    }

    // JSXGraph: src/math/statistics.js -> randomPareto
    fun randomPareto(
        shape: Double,
        scale: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        val draw = random.nextDouble()
        if (shape <= 0.0 || scale <= 0.0) {
            return Double.NaN
        }
        return scale * (1.0 - draw).pow(-1.0 / shape)
    }

    // JSXGraph: src/math/statistics.js -> randomHypergeometric
    fun randomHypergeometric(
        good: Double,
        bad: Double,
        samples: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        if (good < 1.0 || bad < 1.0 || samples > good + bad) {
            return Double.NaN
        }

        var remainingSamples = samples
        val denominatorBase = good + bad - samples
        val smallerGroup = min(good, bad)
        var remainingSmallerGroup = smallerGroup
        while (remainingSmallerGroup * remainingSamples > 0.0) {
            val draw = random.nextDouble()
            remainingSmallerGroup -=
                floor(draw + remainingSmallerGroup / (denominatorBase + remainingSamples))
            remainingSamples -= 1.0
        }
        val selectedFromSmallerGroup = smallerGroup - remainingSmallerGroup
        return if (good <= bad) {
            selectedFromSmallerGroup
        } else {
            samples - selectedFromSmallerGroup
        }
    }

    // JSXGraph: src/math/statistics.js -> histogram
    fun histogram(
        values: DoubleArray,
        options: HistogramOptions = HistogramOptions(),
    ): HistogramResult {
        val numberOfBins = if (options.bins == 0) 10 else options.bins
        val minimum = options.range?.start ?: min(values)
        val maximum = options.range?.endInclusive ?: max(values)
        val delta = if (numberOfBins > 0) {
            (maximum - minimum) / (numberOfBins - 1)
        } else {
            0.0
        }

        val counts = DoubleArray(maxOf(numberOfBins, 0))
        val bins = DoubleArray(maxOf(numberOfBins, 0)) { index -> minimum + index * delta }
        var outsideBinCount = 0
        for (value in values) {
            val bin = floor((value - minimum) / delta)
            if (bin >= 0.0 && bin < numberOfBins.toDouble()) {
                counts[bin.toInt()] += 1.0
            } else {
                outsideBinCount += 1
            }
        }

        if (options.density) {
            val normalizer = sum(counts) + outsideBinCount
            for (index in counts.indices) {
                counts[index] /= normalizer * delta
            }
        }

        if (options.cumulative) {
            if (options.density) {
                for (index in counts.indices) {
                    counts[index] *= delta
                }
            }
            for (index in 1 until counts.size) {
                counts[index] += counts[index - 1]
            }
        }
        return HistogramResult(counts = counts, bins = bins)
    }

    internal fun resetGaussianState() {
        hasGaussianSpare = false
        gaussianSpare = 0.0
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
