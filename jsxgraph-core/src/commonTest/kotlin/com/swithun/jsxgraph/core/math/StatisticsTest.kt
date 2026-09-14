package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Coords
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StatisticsTest {
    @Test
    fun aggregatesMatchOfficialReferenceValues() {
        assertEquals(6.0, Statistics.sum(doubleArrayOf(1.0, 2.0, 3.0)))
        assertEquals(24.0, Statistics.prod(doubleArrayOf(2.0, 3.0, 4.0)))
        assertEquals(2.5, Statistics.mean(doubleArrayOf(1.0, 2.0, 3.0, 4.0)))
        assertEquals(0.0, Statistics.mean(doubleArrayOf()))
        assertEquals(5.0, Statistics.median(doubleArrayOf(9.0, 1.0, 5.0)))
        assertEquals(2.5, Statistics.median(doubleArrayOf(4.0, 1.0, 3.0, 2.0)))
        assertEquals(1.6666666666666667, Statistics.variance(doubleArrayOf(1.0, 2.0, 3.0, 4.0)))
        assertEquals(
            1.2909944487358056,
            Statistics.sd(doubleArrayOf(1.0, 2.0, 3.0, 4.0)),
            absoluteTolerance = 1e-15,
        )
    }

    @Test
    fun percentilesMatchOfficialReferenceValues() {
        assertContentEquals(
            doubleArrayOf(2.0, 3.0, 4.0),
            Statistics.percentiles(
                values = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0),
                percentiles = doubleArrayOf(25.0, 50.0, 75.0),
            ),
        )
        assertTrue(Statistics.percentile(doubleArrayOf(1.0, 2.0, 3.0, 4.0), 100.0).isNaN())
        assertTrue(Statistics.percentile(doubleArrayOf(Double.NaN, Double.NaN), 50.0).isNaN())
        assertEquals(0.0, Statistics.percentile(doubleArrayOf(), 50.0))
    }

    @Test
    fun boxplotMatchesOfficialReferenceValues() {
        val result = Statistics.boxplot(
            doubleArrayOf(7.0, 1.0, 3.0, Double.NaN, 9.0, 2.0, 100.0, 4.0, 5.0, 6.0, 8.0),
        )

        val summary = assertIs<GMResult.Ok<BoxPlotSummary>>(result).value
        assertEquals(1.0, summary.minimum)
        assertEquals(3.0, summary.lowerQuartile)
        assertEquals(5.5, summary.median)
        assertEquals(8.0, summary.upperQuartile)
        assertEquals(9.0, summary.maximum)
        assertContentEquals(doubleArrayOf(100.0), summary.outliers)
        assertIs<GMResult.Err<StatisticsError.EmptyData>>(
            Statistics.boxplot(doubleArrayOf()),
        )
    }

    @Test
    fun weightedMeanReportsDimensionMismatch() {
        val valid = Statistics.weightedMean(
            values = doubleArrayOf(1.0, 2.0, 3.0),
            weights = doubleArrayOf(2.0, 3.0, 4.0),
        )
        assertEquals(6.666666666666667, assertIs<GMResult.Ok<Double>>(valid).value)

        val mismatch = Statistics.weightedMean(
            values = doubleArrayOf(1.0),
            weights = doubleArrayOf(1.0, 2.0),
        )
        assertEquals(
            StatisticsError.DimensionMismatch(valueCount = 1, weightCount = 2),
            assertIs<GMResult.Err<StatisticsError.DimensionMismatch>>(mismatch).error,
        )
    }

    @Test
    fun extremaAndArrayArithmeticMatchOfficialReferenceValues() {
        assertEquals(Double.NEGATIVE_INFINITY, Statistics.max(doubleArrayOf()))
        assertEquals(Double.POSITIVE_INFINITY, Statistics.min(doubleArrayOf()))
        assertContentEquals(doubleArrayOf(-2.0, 8.0), Statistics.range(doubleArrayOf(5.0, -2.0, 8.0)))
        assertContentEquals(doubleArrayOf(2.0, 3.0), Statistics.abs(doubleArrayOf(-2.0, 3.0)))
        assertContentEquals(
            doubleArrayOf(5.0, 7.0),
            Statistics.add(doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(4.0, 5.0)),
        )
        assertContentEquals(doubleArrayOf(4.0, 5.0), Statistics.add(doubleArrayOf(1.0, 2.0), 3.0))
        assertContentEquals(doubleArrayOf(4.0, 3.0), Statistics.div(12.0, doubleArrayOf(3.0, 4.0)))
        assertContentEquals(
            doubleArrayOf(-1.0, 1.0),
            Statistics.mod(doubleArrayOf(-1.0, 6.0), 5.0),
        )
        assertContentEquals(
            doubleArrayOf(4.0, 1.0),
            Statistics.mod(doubleArrayOf(-1.0, 6.0), 5.0, mathematical = true),
        )
        assertContentEquals(
            doubleArrayOf(3.0, 8.0),
            Statistics.multiply(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0, 5.0)),
        )
        assertContentEquals(
            doubleArrayOf(9.0, 8.0, 7.0),
            Statistics.subtract(10.0, doubleArrayOf(1.0, 2.0, 3.0)),
        )
    }

    @Test
    fun theilSenRegressionMatchesOfficialReferenceValues() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val coordinates = listOf(
            Coords(Const.COORDS_BY_USER, doubleArrayOf(0.0, 1.0), board),
            Coords(Const.COORDS_BY_USER, doubleArrayOf(1.0, 3.0), board),
            Coords(Const.COORDS_BY_USER, doubleArrayOf(2.0, 5.0), board),
            Coords(Const.COORDS_BY_USER, doubleArrayOf(3.0, 8.0), board),
        )

        assertContentEquals(
            doubleArrayOf(0.625, 2.375, -1.0),
            Statistics.TheilSenRegression(coordinates),
        )
    }

    @Test
    fun randomDistributionAlgorithmsMatchOfficialReferenceWithFixedDraws() {
        Statistics.resetGaussianState()
        val gaussianRandom = sequenceRandom(0.75, 0.25)
        assertEquals(
            4.4976638334730925,
            Statistics.generateGaussian(2.0, 3.0, gaussianRandom),
            absoluteTolerance = 1e-14,
        )
        assertEquals(
            -0.49766383347309295,
            Statistics.generateGaussian(2.0, 3.0, gaussianRandom),
            absoluteTolerance = 1e-14,
        )

        assertEquals(12.5, Statistics.randomUniform(10.0, 20.0, sequenceRandom(0.25)))
        assertEquals(
            0.34657359027997264,
            Statistics.randomExponential(2.0, sequenceRandom(0.5)),
            absoluteTolerance = 1e-15,
        )
        assertEquals(
            1.125,
            Statistics.randomGamma(
                shape = 0.5,
                scale = 2.0,
                threshold = 1.0,
                random = sequenceRandom(0.0, 0.25, 0.0),
            ),
        )
        assertEquals(
            2.7320508075688767,
            Statistics.randomGamma(2.0, random = sequenceRandom(0.25, 0.0)),
            absoluteTolerance = 1e-14,
        )
    }

    @Test
    fun discreteRandomAlgorithmsMatchOfficialReferenceWithFixedDraws() {
        assertEquals(
            2.0,
            Statistics.randomBinomial(10.0, 0.2, sequenceRandom(0.5)),
        )
        assertEquals(
            3.0,
            Statistics.randomGeometric(0.25, sequenceRandom(0.5)),
        )
        assertEquals(
            2.0,
            Statistics.randomPoisson(2.0, sequenceRandom(0.5)),
        )
        assertEquals(
            6.0,
            Statistics.randomPareto(2.0, 3.0, sequenceRandom(0.75)),
        )
        assertEquals(
            1.0,
            Statistics.randomHypergeometric(4.0, 6.0, 3.0, sequenceRandom(0.2, 0.8, 0.4)),
        )
    }

    @Test
    fun histogramMatchesOfficialReferenceValues() {
        val values = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0, 10.0)
        val histogram = Statistics.histogram(
            values,
            HistogramOptions(bins = 3, range = 0.0..4.0),
        )
        assertContentEquals(doubleArrayOf(2.0, 2.0, 1.0), histogram.counts)
        assertContentEquals(doubleArrayOf(0.0, 2.0, 4.0), histogram.bins)

        val cumulativeDensity = Statistics.histogram(
            values,
            HistogramOptions(
                bins = 3,
                range = 0.0..4.0,
                density = true,
                cumulative = true,
            ),
        )
        assertContentEquals(
            doubleArrayOf(
                0.3333333333333333,
                0.6666666666666666,
                0.8333333333333333,
            ),
            cumulativeDensity.counts,
        )
        assertContentEquals(doubleArrayOf(0.0, 2.0, 4.0), cumulativeDensity.bins)
    }

    private fun sequenceRandom(vararg values: Double): RandomSource {
        var index = 0
        return RandomSource {
            val value = values[index % values.size]
            index += 1
            value
        }
    }
}
