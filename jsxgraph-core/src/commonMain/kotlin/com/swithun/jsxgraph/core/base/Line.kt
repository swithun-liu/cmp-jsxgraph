/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/line.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.abs

internal sealed interface LineError {
    data class ParentBoardMismatch(val parentIndex: Int) : LineError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : LineError

    data class Registration(val error: BoardError) : LineError
}

/**
 * Initial translated slice of JXG.Line.
 *
 * This slice covers lines defined by two registered points, their dependency
 * links, standard form, and coordinate-derived numeric queries. Coordinate
 * parents, constrained lines, rendering, ticks, arrows, and hit testing remain
 * untranslated.
 */
internal open class Line internal constructor(
    board: Board,
    internal var point1: Point,
    internal var point2: Point,
    id: String = "",
    name: String? = null,
    needsRegularUpdate: Boolean = true,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_LINE,
    elementClass = Const.OBJECT_CLASS_LINE,
    needsRegularUpdate = needsRegularUpdate,
) {
    init {
        elType = LINE_ELEMENT_TYPE
    }

    // JSXGraph: src/base/line.js -> update
    override fun update(fromParent: Boolean): Line {
        if (!needsUpdate) {
            return this
        }
        updateStdform()
        return this
    }

    // JSXGraph: src/base/line.js -> updateStdform
    internal fun updateStdform(): Line {
        val value = Mat.crossProduct(
            point1.coords.usrCoords,
            point2.coords.usrCoords,
        )
        stdform[0] = value[0]
        stdform[1] = value[1]
        stdform[2] = value[2]
        stdform[3] = 0.0
        normalize()
        return this
    }

    // JSXGraph: src/base/line.js -> getRise
    internal fun getRise(): Double =
        if (abs(stdform[2]) >= Mat.eps) {
            -stdform[0] / stdform[2]
        } else {
            Double.POSITIVE_INFINITY
        }

    // JSXGraph: src/base/line.js -> Slope
    internal fun Slope(): Double =
        if (abs(stdform[2]) >= Mat.eps) {
            -stdform[1] / stdform[2]
        } else {
            Double.POSITIVE_INFINITY
        }

    // JSXGraph: src/base/line.js -> Direction
    internal fun Direction(): DoubleArray {
        val coordinates1 = point1.coords.usrCoords
        val coordinates2 = point2.coords.usrCoords

        if (coordinates2[0] == 0.0 && coordinates1[0] != 0.0) {
            return coordinates2.copyOfRange(1, coordinates2.size)
        }
        if (coordinates1[0] == 0.0 && coordinates2[0] != 0.0) {
            return doubleArrayOf(-coordinates1[1], -coordinates1[2])
        }
        return doubleArrayOf(
            coordinates2[1] - coordinates1[1],
            coordinates2[2] - coordinates1[2],
        )
    }

    // JSXGraph: src/base/line.js -> isVertical
    internal fun isVertical(): Boolean {
        val direction = Direction()
        return direction[0] == 0.0 && direction[1] != 0.0
    }

    // JSXGraph: src/base/line.js -> L
    internal fun L(): Double = point1.Dist(point2)

    // JSXGraph: src/base/line.js -> bounds
    internal fun bounds(): DoubleArray {
        val coordinates1 = point1.coords.usrCoords
        val coordinates2 = point2.coords.usrCoords
        return doubleArrayOf(
            minOf(coordinates1[1], coordinates2[1]),
            maxOf(coordinates1[2], coordinates2[2]),
            maxOf(coordinates1[1], coordinates2[1]),
            minOf(coordinates1[2], coordinates2[2]),
        )
    }

    internal companion object {
        private const val LINE_ID_PREFIX = "L"
        private const val LINE_ELEMENT_TYPE = "line"

        // JSXGraph: src/base/line.js -> createLine / Line constructor
        fun create(
            board: Board,
            point1: Point,
            point2: Point,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Line, LineError> {
            validateParent(board, point1, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParent(board, point2, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }

            val line = Line(
                board = board,
                point1 = point1,
                point2 = point2,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (val registration = board.setId(line, LINE_ID_PREFIX)) {
                is GMResult.Ok -> {
                    point1.addChild(line)
                    point2.addChild(line)
                    line.setParents(listOf(point1, point2))
                    line.isDraggable = true
                    line.updateStdform()
                    GMResult.Ok(line)
                }

                is GMResult.Err -> GMResult.Err(
                    LineError.Registration(registration.error),
                )
            }
        }

        private fun validateParent(
            board: Board,
            point: Point,
            parentIndex: Int,
        ): LineError? {
            if (point.board !== board) {
                return LineError.ParentBoardMismatch(parentIndex)
            }
            if (board.elementById(point.id) !== point) {
                return LineError.ParentNotRegistered(parentIndex, point.id)
            }
            return null
        }
    }
}
