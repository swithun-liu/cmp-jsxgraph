/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/point.js -> createPolePoint
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Numerics
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue

internal sealed interface PolePointError {
    data class UnsupportedParents(
        val parentTypes: List<String>,
    ) : PolePointError

    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : PolePointError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : PolePointError

    data class PointCreation(
        val error: PointError,
    ) : PolePointError
}

internal object PolePoint {
    private const val POLE_POINT_ELEMENT_TYPE = "polepoint"

    // JSXGraph: src/base/point.js -> createPolePoint
    internal fun create(
        board: Board,
        firstParent: GeometryElement,
        secondParent: GeometryElement,
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        fixed: Boolean = false,
    ): GMResult<Point, PolePointError> {
        val canonicalParents = when {
            firstParent is Circle && secondParent is Line ->
                CanonicalParents(
                    circle = firstParent,
                    line = secondParent,
                    circleIndex = 0,
                    lineIndex = 1,
                )
            firstParent is Line && secondParent is Circle ->
                CanonicalParents(
                    circle = secondParent,
                    line = firstParent,
                    circleIndex = 1,
                    lineIndex = 0,
                )
            else -> return GMResult.Err(
                PolePointError.UnsupportedParents(
                    parentTypes = listOf(
                        firstParent.elType,
                        secondParent.elType,
                    ),
                ),
            )
        }
        validateParent(
            board = board,
            element = canonicalParents.circle,
            parentIndex = canonicalParents.circleIndex,
        )?.let {
            return GMResult.Err(it)
        }
        validateParent(
            board = board,
            element = canonicalParents.line,
            parentIndex = canonicalParents.lineIndex,
        )?.let {
            return GMResult.Err(it)
        }

        val point = when (
            val result = Point.createConstrained(
                board = board,
                coordinateFunctions = listOf(
                    PoleCoordinateFunction(
                        circle = canonicalParents.circle,
                        line = canonicalParents.line,
                    ),
                ),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                PolePointError.PointCreation(result.error),
            )
        }

        point.elType = POLE_POINT_ELEMENT_TYPE
        point.setParents(
            listOf(
                canonicalParents.circle,
                canonicalParents.line,
            ),
        )
        canonicalParents.circle.addChild(point)
        canonicalParents.line.addChild(point)
        return GMResult.Ok(point)
    }

    private fun validateParent(
        board: Board,
        element: GeometryElement,
        parentIndex: Int,
    ): PolePointError? =
        when {
            element.board !== board ->
                PolePointError.ParentBoardMismatch(parentIndex)
            board.elementById(element.id) !== element ->
                PolePointError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = element.id,
                )
            else -> null
        }

    private data class CanonicalParents(
        val circle: Circle,
        val line: Line,
        val circleIndex: Int,
        val lineIndex: Int,
    )
}

// JSXGraph: src/base/point.js -> createPolePoint coordinate closure.
private class PoleCoordinateFunction(
    private val circle: Circle,
    private val line: Line,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()
    override val returnsCoordinateArray: Boolean = true

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        val quadraticForm = circle.quadraticform
        val standardForm = line.stdform.copyOfRange(0, 3)
        val coordinates = doubleArrayOf(
            Numerics.det(
                arrayOf(
                    standardForm,
                    quadraticForm[1],
                    quadraticForm[2],
                ),
            ),
            Numerics.det(
                arrayOf(
                    quadraticForm[0],
                    standardForm,
                    quadraticForm[2],
                ),
            ),
            Numerics.det(
                arrayOf(
                    quadraticForm[0],
                    quadraticForm[1],
                    standardForm,
                ),
            ),
        )
        return GMResult.Ok(
            JessieCodeRuntimeValue.ArrayValue(
                coordinates.map(JessieCodeRuntimeValue::NumberValue),
            ),
        )
    }
}
