/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/line.js -> createRadicalAxis
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue

internal sealed interface RadicalAxisError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : RadicalAxisError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : RadicalAxisError

    data class PointCreation(
        val pointIndex: Int,
        val error: PointError,
    ) : RadicalAxisError

    data class LineCreation(
        val error: LineError,
    ) : RadicalAxisError
}

internal object RadicalAxis {
    private const val RADICAL_AXIS_ELEMENT_TYPE = "radicalaxis"

    // JSXGraph: src/base/line.js -> createRadicalAxis
    internal fun create(
        board: Board,
        circle1: Circle,
        circle2: Circle,
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        straightFirst: Boolean = true,
        straightLast: Boolean = true,
        point1Id: String = "",
        point1Name: String? = null,
        point1NeedsRegularUpdate: Boolean = true,
        point2Id: String = "",
        point2Name: String? = null,
        point2NeedsRegularUpdate: Boolean = true,
    ): GMResult<Line, RadicalAxisError> {
        validateParent(board, circle1, 0)?.let {
            return GMResult.Err(it)
        }
        validateParent(board, circle2, 1)?.let {
            return GMResult.Err(it)
        }

        val coefficients = RadicalAxisCoefficientFunction(
            circle1 = circle1,
            circle2 = circle2,
        )
        val point1 = when (
            val result = Point.createConstrained(
                board = board,
                coordinateFunctions = listOf(
                    RadicalAxisEndpointFunction(
                        coefficients = coefficients,
                        firstEndpoint = true,
                    ),
                ),
                id = point1Id,
                name = point1Name,
                needsRegularUpdate = point1NeedsRegularUpdate,
                fixed = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                RadicalAxisError.PointCreation(
                    pointIndex = 0,
                    error = result.error,
                ),
            )
        }
        val point2 = when (
            val result = Point.createConstrained(
                board = board,
                coordinateFunctions = listOf(
                    RadicalAxisEndpointFunction(
                        coefficients = coefficients,
                        firstEndpoint = false,
                    ),
                ),
                id = point2Id,
                name = point2Name,
                needsRegularUpdate = point2NeedsRegularUpdate,
                fixed = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(point1)
                return GMResult.Err(
                    RadicalAxisError.PointCreation(
                        pointIndex = 1,
                        error = result.error,
                    ),
                )
            }
        }
        val line = when (
            val result = Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObjects(listOf(point1, point2))
                return GMResult.Err(
                    RadicalAxisError.LineCreation(result.error),
                )
            }
        }

        line.elType = RADICAL_AXIS_ELEMENT_TYPE
        line.constrained = true
        line.isDraggable = false
        line.configureVisibleRange(
            straightFirst = straightFirst,
            straightLast = straightLast,
        )
        line.setParents(listOf(circle1, circle2))
        circle1.addChild(line)
        circle2.addChild(line)
        return GMResult.Ok(line)
    }

    private fun validateParent(
        board: Board,
        circle: Circle,
        parentIndex: Int,
    ): RadicalAxisError? =
        when {
            circle.board !== board ->
                RadicalAxisError.ParentBoardMismatch(parentIndex)
            board.elementById(circle.id) !== circle ->
                RadicalAxisError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = circle.id,
                )
            else -> null
        }
}

// JSXGraph: src/base/line.js -> createRadicalAxis coefficient closure.
private class RadicalAxisCoefficientFunction(
    private val circle1: Circle,
    private val circle2: Circle,
) {
    fun evaluate(): DoubleArray {
        val first = circle1.stdform
        val second = circle2.stdform
        return Mat.matVecMult(
            matrix = Mat.transpose(
                arrayOf(
                    first.copyOfRange(0, 3),
                    second.copyOfRange(0, 3),
                ),
            ),
            vector = doubleArrayOf(second[3], -first[3]),
        )
    }
}

// JSXGraph: src/base/line.js -> createLine one-function coefficient branch.
private class RadicalAxisEndpointFunction(
    private val coefficients: RadicalAxisCoefficientFunction,
    private val firstEndpoint: Boolean,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()
    override val returnsCoordinateArray: Boolean = true

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        val values = coefficients.evaluate()
        val a = values[0]
        val b = values[1]
        val c = values[2]
        val homogeneous = c * c + b * b
        val coordinates =
            if (firstEndpoint) {
                doubleArrayOf(
                    homogeneous * 0.5,
                    (c - b * a + c) * 0.5,
                    (-b - c * a - b) * 0.5,
                )
            } else {
                doubleArrayOf(
                    homogeneous,
                    -b * a + c,
                    -c * a - b,
                )
            }
        return GMResult.Ok(
            JessieCodeRuntimeValue.ArrayValue(
                coordinates.map(JessieCodeRuntimeValue::NumberValue),
            ),
        )
    }
}
