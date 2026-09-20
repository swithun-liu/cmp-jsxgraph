/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/composition.js -> createCircumcenter / createCircumcircle
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry

internal sealed interface CircumcenterError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : CircumcenterError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : CircumcenterError

    data class OwnedPointNotParent(
        val id: String,
    ) : CircumcenterError

    data class Registration(
        val error: BoardError,
    ) : CircumcenterError
}

/**
 * The implicit constrained center used by a three-point circumcircle.
 *
 * JSXGraph creates this Point as a private sub-element before constructing the
 * visible Circle. It stays registered on the Board so dependency updates and
 * recursive removal follow the same element lifecycle as the upstream model.
 */
internal class CircumcenterPoint private constructor(
    board: Board,
    internal val point1: Point,
    internal val point2: Point,
    internal val point3: Point,
    internal val ownedPoints: Set<Point>,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    fixed: Boolean,
) : Point(
    board = board,
    coordinates = circumcenter(point1, point2, point3),
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
    fixed = fixed,
) {
    init {
        type = Const.OBJECT_TYPE_CAS
        elType = CIRCUMCENTER_ELEMENT_TYPE
        isDraggable = false
    }

    // JSXGraph keeps this invisible helper's initial isReal state because its
    // renderer update is skipped. Distances to an ideal circumcenter are
    // therefore Infinity rather than NaN.
    override val isReal: Boolean
        get() = true

    // JSXGraph: src/element/composition.js -> createCircumcenter constraint
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = circumcenter(point1, point2, point3),
        )
        return this
    }

    internal companion object {
        private const val POINT_ID_PREFIX = "P"
        private const val CIRCUMCENTER_ELEMENT_TYPE = "circumcenter"

        fun create(
            board: Board,
            point1: Point,
            point2: Point,
            point3: Point,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<CircumcenterPoint, CircumcenterError> {
            val parents = listOf(point1, point2, point3)
            for ((index, parent) in parents.withIndex()) {
                validateParent(board, parent, index)?.let {
                    return GMResult.Err(it)
                }
            }
            if (!ownedPoints.all(parents::contains)) {
                val invalid = ownedPoints.first { it !in parents }
                return GMResult.Err(
                    CircumcenterError.OwnedPointNotParent(invalid.id),
                )
            }

            val center = CircumcenterPoint(
                board = board,
                point1 = point1,
                point2 = point2,
                point3 = point3,
                ownedPoints = ownedPoints,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )
            return when (
                val registration = board.setId(center, POINT_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    for (parent in parents) {
                        if (parent in ownedPoints) {
                            center.addChild(parent)
                        } else {
                            parent.addChild(center)
                        }
                    }
                    center.setParents(parents)
                    center.update()
                    GMResult.Ok(center)
                }
                is GMResult.Err -> GMResult.Err(
                    CircumcenterError.Registration(registration.error),
                )
            }
        }

        private fun validateParent(
            board: Board,
            point: Point,
            parentIndex: Int,
        ): CircumcenterError? {
            if (point.board !== board) {
                return CircumcenterError.ParentBoardMismatch(parentIndex)
            }
            if (board.elementById(point.id) !== point) {
                return CircumcenterError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = point.id,
                )
            }
            return null
        }

        private fun circumcenter(
            point1: Point,
            point2: Point,
            point3: Point,
        ): DoubleArray =
            Geometry.circumcenter(
                point1.coords.usrCoords,
                point2.coords.usrCoords,
                point3.coords.usrCoords,
            )
    }
}
