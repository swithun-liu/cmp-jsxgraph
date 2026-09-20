/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/polygon.js -> createParallelogram
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult

internal sealed interface ParallelogramError {
    data class ParallelPointCreation(
        val error: ParallelConstructionError,
    ) : ParallelogramError

    data class PolygonCreation(
        val error: PolygonError,
    ) : ParallelogramError
}

internal object Parallelogram {
    private const val PARALLELOGRAM_ELEMENT_TYPE = "parallelogram"

    // JSXGraph: src/base/polygon.js -> createParallelogram
    internal fun create(
        board: Board,
        point1: Point,
        point2: Point,
        point3: Point,
        ownedPoints: Set<Point> = emptySet(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        withLines: Boolean = true,
        parallelPointId: String = "",
        parallelPointName: String? = "",
        parallelPointNeedsRegularUpdate: Boolean = true,
    ): GMResult<Polygon, ParallelogramError> {
        val parallelPoint = when (
            val result = ParallelPoint.create(
                board = board,
                point1 = point1,
                point2 = point2,
                point3 = point3,
                ownedPoints = ownedPoints,
                id = parallelPointId,
                name = parallelPointName,
                needsRegularUpdate = parallelPointNeedsRegularUpdate,
                fixed = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                ParallelogramError.ParallelPointCreation(result.error),
            )
        }

        val polygon = when (
            val result = Polygon.create(
                board = board,
                vertices = listOf(
                    point1,
                    point2,
                    parallelPoint,
                    point3,
                ),
                withLines = withLines,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(parallelPoint)
                return GMResult.Err(
                    ParallelogramError.PolygonCreation(result.error),
                )
            }
        }

        polygon.elType = PARALLELOGRAM_ELEMENT_TYPE
        polygon.parallelPoint = parallelPoint
        polygon.implicitVertices = ownedPoints.toList() + parallelPoint
        polygon.isDraggable = true
        parallelPoint.isDraggable = true
        parallelPoint.isFixed = false
        return GMResult.Ok(polygon)
    }
}
