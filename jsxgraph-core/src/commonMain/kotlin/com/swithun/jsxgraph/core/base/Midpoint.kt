/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/composition.js -> createMidpoint
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.abs

internal sealed interface MidpointError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : MidpointError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : MidpointError

    data class OwnedPointNotParent(
        val id: String,
    ) : MidpointError

    data class Registration(
        val error: BoardError,
    ) : MidpointError
}

/**
 * A constrained Point at the arithmetic mean of two homogeneous Points.
 *
 * JSXGraph gives coordinate-array helper Points to the Midpoint as children,
 * while existing Points own the Midpoint as their child. [ownedPoints]
 * preserves that distinction for recursive removal.
 */
internal class MidpointPoint private constructor(
    board: Board,
    internal val point1: Point,
    internal val point2: Point,
    internal val ownedPoints: Set<Point>,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    fixed: Boolean,
) : Point(
    board = board,
    coordinates = midpoint(point1, point2),
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
    fixed = fixed,
) {
    init {
        type = Const.OBJECT_TYPE_CAS
        elType = MIDPOINT_ELEMENT_TYPE
        isDraggable = false
    }

    // JSXGraph: src/element/composition.js -> createMidpoint constraints
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = midpoint(point1, point2),
        )
        return this
    }

    internal companion object {
        private const val POINT_ID_PREFIX = "P"
        private const val MIDPOINT_ELEMENT_TYPE = "midpoint"

        fun create(
            board: Board,
            point1: Point,
            point2: Point,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<MidpointPoint, MidpointError> {
            validateParent(board, point1, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParent(board, point2, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }
            val parents = listOf(point1, point2)
            if (!ownedPoints.all { owned -> owned in parents }) {
                val invalid = ownedPoints.first { owned -> owned !in parents }
                return GMResult.Err(
                    MidpointError.OwnedPointNotParent(invalid.id),
                )
            }

            val midpoint = MidpointPoint(
                board = board,
                point1 = point1,
                point2 = point2,
                ownedPoints = ownedPoints,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )
            return when (
                val registration = board.setId(midpoint, POINT_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    for (parent in parents) {
                        if (parent in ownedPoints) {
                            midpoint.addChild(parent)
                        } else {
                            parent.addChild(midpoint)
                        }
                    }
                    midpoint.setParents(parents)
                    midpoint.update()
                    GMResult.Ok(midpoint)
                }

                is GMResult.Err -> GMResult.Err(
                    MidpointError.Registration(registration.error),
                )
            }
        }

        private fun validateParent(
            board: Board,
            point: Point,
            parentIndex: Int,
        ): MidpointError? {
            if (point.board !== board) {
                return MidpointError.ParentBoardMismatch(parentIndex)
            }
            if (board.elementById(point.id) !== point) {
                return MidpointError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = point.id,
                )
            }
            return null
        }

        private fun midpoint(
            point1: Point,
            point2: Point,
        ): DoubleArray {
            val first = point1.coords.usrCoords
            val second = point2.coords.usrCoords
            val x = first[1] + second[1]
            val y = first[2] + second[2]
            val hasIdealParent =
                abs(first[0]) < Mat.eps || abs(second[0]) < Mat.eps
            return doubleArrayOf(
                if (x.isNaN() || hasIdealParent) Double.NaN else x * 0.5,
                if (y.isNaN() || hasIdealParent) Double.NaN else y * 0.5,
            )
        }
    }
}
