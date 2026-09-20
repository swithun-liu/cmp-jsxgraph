/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/curve.js -> createBoxPlot.updateDataArray
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class BoxPlotGeometry(
    val xCoordinates: DoubleArray,
    val yCoordinates: DoubleArray,
)

internal fun boxPlotPointCount(
    outlierCount: Int?,
    outlierFace: String,
): Long {
    if (outlierCount == null) {
        return BOX_PLOT_BASE_POINT_COUNT
    }
    val pointsPerOutlier = when (outlierFace) {
        "-", "minus", "|", "divide" -> 3L
        "x", "cross",
        "[]", "square",
        "<>", "diamond",
        "<<>>", "diamond2",
        "+", "plus",
        -> 6L
        else -> 19L
    }
    return BOX_PLOT_BASE_POINT_COUNT + 1L +
        outlierCount.toLong() * pointsPerOutlier
}

// JSXGraph 1.13.3: src/base/curve.js ->
// createBoxPlot.updateDataArray.
internal fun createBoxPlotGeometry(
    quantiles: DoubleArray,
    outliers: DoubleArray?,
    axis: Double,
    width: Double,
    direction: String,
    smallWidth: Double,
    outlierFace: String,
    outlierSize: Double,
    unitX: Double,
    unitY: Double,
): BoxPlotGeometry {
    val left = axis - width * 0.5
    val smallLeft = axis - width * 0.5 * smallWidth
    val right = axis + width * 0.5
    val smallRight = axis + width * 0.5 * smallWidth
    val first = mutableListOf(
        axis,
        smallLeft,
        smallRight,
        axis,
        axis,
        left,
        left,
        right,
        right,
        axis,
        Double.NaN,
        left,
        right,
        Double.NaN,
        axis,
        axis,
        smallLeft,
        smallRight,
        axis,
    )
    val second = mutableListOf(
        quantiles[0],
        quantiles[0],
        quantiles[0],
        quantiles[0],
        quantiles[1],
        quantiles[1],
        quantiles[3],
        quantiles[3],
        quantiles[1],
        quantiles[1],
        Double.NaN,
        quantiles[2],
        quantiles[2],
        Double.NaN,
        quantiles[3],
        quantiles[4],
        quantiles[4],
        quantiles[4],
        quantiles[4],
    )

    if (outliers != null) {
        first += Double.NaN
        second += Double.NaN
        val sizeX: Double
        val sizeY: Double
        if (direction == BOX_PLOT_VERTICAL) {
            sizeX = outlierSize / unitX
            sizeY = outlierSize / unitY
        } else {
            sizeY = outlierSize / unitX
            sizeX = outlierSize / unitY
        }
        val sizeX2 = sizeX * sqrt(2.0)
        val sizeY2 = sizeY * sqrt(2.0)
        for (outlier in outliers) {
            when (outlierFace) {
                "x", "cross" -> {
                    first += listOf(
                        axis - sizeX,
                        axis + sizeX,
                        Double.NaN,
                        axis - sizeX,
                        axis + sizeX,
                        Double.NaN,
                    )
                    second += listOf(
                        outlier + sizeY,
                        outlier - sizeY,
                        Double.NaN,
                        outlier - sizeY,
                        outlier + sizeY,
                        Double.NaN,
                    )
                }
                "[]", "square" -> {
                    first += listOf(
                        axis - sizeX,
                        axis + sizeX,
                        axis + sizeX,
                        axis - sizeX,
                        axis - sizeX,
                        Double.NaN,
                    )
                    second += listOf(
                        outlier + sizeY,
                        outlier + sizeY,
                        outlier - sizeY,
                        outlier - sizeY,
                        outlier + sizeY,
                        Double.NaN,
                    )
                }
                "<>", "diamond" -> {
                    first += listOf(
                        axis,
                        axis + sizeX,
                        axis,
                        axis - sizeX,
                        axis,
                        Double.NaN,
                    )
                    second += listOf(
                        outlier + sizeY,
                        outlier,
                        outlier - sizeY,
                        outlier,
                        outlier + sizeY,
                        Double.NaN,
                    )
                }
                "<<>>", "diamond2" -> {
                    first += listOf(
                        axis,
                        axis + sizeX2,
                        axis,
                        axis - sizeX2,
                        axis,
                        Double.NaN,
                    )
                    second += listOf(
                        outlier + sizeY2,
                        outlier,
                        outlier - sizeY2,
                        outlier,
                        outlier + sizeY2,
                        Double.NaN,
                    )
                }
                "+", "plus" -> {
                    first += listOf(
                        axis - sizeX,
                        axis + sizeX,
                        Double.NaN,
                        axis,
                        axis,
                        Double.NaN,
                    )
                    second += listOf(
                        outlier,
                        outlier,
                        Double.NaN,
                        outlier - sizeY,
                        outlier + sizeY,
                        Double.NaN,
                    )
                }
                "-", "minus" -> {
                    first += listOf(
                        axis - sizeX,
                        axis + sizeX,
                        Double.NaN,
                    )
                    second += listOf(
                        outlier,
                        outlier,
                        Double.NaN,
                    )
                }
                "|", "divide" -> {
                    first += listOf(
                        axis,
                        axis,
                        Double.NaN,
                    )
                    second += listOf(
                        outlier - sizeY,
                        outlier + sizeY,
                        Double.NaN,
                    )
                }
                else -> {
                    val step = 2.0 * PI / BOX_PLOT_CIRCLE_SEGMENTS
                    for (index in 0..BOX_PLOT_CIRCLE_SEGMENTS) {
                        val parameter = index * step
                        first += axis - sizeX * sin(parameter)
                        second += outlier - sizeY * cos(parameter)
                    }
                    first += Double.NaN
                    second += Double.NaN
                }
            }
        }
    }

    return if (direction == BOX_PLOT_VERTICAL) {
        BoxPlotGeometry(
            xCoordinates = first.toDoubleArray(),
            yCoordinates = second.toDoubleArray(),
        )
    } else {
        BoxPlotGeometry(
            xCoordinates = second.toDoubleArray(),
            yCoordinates = first.toDoubleArray(),
        )
    }
}

internal const val BOX_PLOT_VERTICAL = "vertical"
private const val BOX_PLOT_BASE_POINT_COUNT = 19L
private const val BOX_PLOT_CIRCLE_SEGMENTS = 17
