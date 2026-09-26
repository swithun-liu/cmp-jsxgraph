/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/ticks.js -> createHatchmark
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.ceil

internal object Hatch {
    internal fun defaultAttributes(): TicksAttributes =
        TicksAttributes(
            anchor = TicksAnchor.Middle,
            drawZero = true,
            majorHeight = 20.0,
            ticksDistance = 0.2,
        )

    // JSXGraph 1.13.3: src/base/ticks.js -> createHatchmark.
    internal fun create(
        board: Board,
        parent: GeometryElement,
        numberOfHashes: Double,
        attributes: TicksAttributes = defaultAttributes(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        maximumTickCount: Int = Ticks.DEFAULT_MAXIMUM_TICK_COUNT,
    ): GMResult<Ticks, TicksError> {
        val requestedCount = positionCount(numberOfHashes)
        if (requestedCount > maximumTickCount) {
            return GMResult.Err(
                TicksError.TickCountLimitExceeded(
                    limit = maximumTickCount,
                    requestedSize = requestedCount,
                ),
            )
        }
        val width = attributes.ticksDistance
        val totalWidth = (numberOfHashes - 1.0) * width
        val base = -totalWidth * 0.5
        val positions = DoubleArray(requestedCount.toInt()) { index ->
            base + index * width
        }
        return when (
            val result = Ticks.create(
                board = board,
                parent = parent,
                source = TicksSource.Fixed(positions),
                attributes = attributes,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                maximumTickCount = maximumTickCount,
            )
        ) {
            is GMResult.Ok -> {
                val hatch = result.value
                hatch.elType = HATCH_ELEMENT_TYPE
                when (parent) {
                    is Line -> parent.inherits += hatch
                    is Curve -> parent.inherits += hatch
                }
                GMResult.Ok(hatch)
            }
            is GMResult.Err -> result
        }
    }

    internal fun positionCount(numberOfHashes: Double): Long = when {
        numberOfHashes.isNaN() || numberOfHashes <= 0.0 -> 0L
        numberOfHashes == Double.POSITIVE_INFINITY -> Long.MAX_VALUE
        else -> ceil(numberOfHashes)
            .coerceAtMost(Long.MAX_VALUE.toDouble())
            .toLong()
    }

    private const val HATCH_ELEMENT_TYPE = "hatch"
}
