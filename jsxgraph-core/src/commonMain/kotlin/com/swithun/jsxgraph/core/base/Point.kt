/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/point.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult

internal sealed interface PointError {
    data class InvalidCoordinateCount(val count: Int) : PointError

    data class Registration(val error: BoardError) : PointError
}

/**
 * Initial translated slice of JXG.Point.
 *
 * This slice covers numeric free points, board registration, coordinate
 * updates, and bounds. Visual attributes, hit testing, traces, constraints,
 * transformations, gliders, and intersections remain untranslated.
 */
internal open class Point internal constructor(
    board: Board,
    coordinates: DoubleArray,
    id: String = "",
    name: String? = null,
    needsRegularUpdate: Boolean = true,
) : CoordsElement(
    board = board,
    coordinates = coordinates,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_POINT,
    elementClass = Const.OBJECT_CLASS_POINT,
    needsRegularUpdate = needsRegularUpdate,
) {
    init {
        elType = POINT_ELEMENT_TYPE
    }

    // JSXGraph: src/base/point.js -> update
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        updateCoords(fromParent)
        return this
    }

    // JSXGraph: src/base/point.js -> bounds
    internal fun bounds(): DoubleArray =
        doubleArrayOf(X(), Y(), X(), Y())

    internal companion object {
        private const val POINT_ID_PREFIX = "P"
        private const val POINT_ELEMENT_TYPE = "point"

        // JSXGraph: src/base/point.js -> createPoint / Point constructor
        fun create(
            board: Board,
            coordinates: DoubleArray,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Point, PointError> {
            if (coordinates.size < 2) {
                return GMResult.Err(
                    PointError.InvalidCoordinateCount(coordinates.size),
                )
            }

            val point = Point(
                board = board,
                coordinates = coordinates,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            point.baseElement = point
            return when (val registration = board.setId(point, POINT_ID_PREFIX)) {
                is GMResult.Ok -> {
                    point.handleSnapToGrid()
                    point.handleSnapToPoints()
                    point.handleAttractors()
                    GMResult.Ok(point)
                }

                is GMResult.Err -> GMResult.Err(
                    PointError.Registration(registration.error),
                )
            }
        }
    }
}
