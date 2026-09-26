/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/grid.js -> JXG.createGrid
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Andreas Walter, Alfred Wassermann, and contributors.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphGrid2D
import com.swithun.jsxgraph.core.JsxGraphGridResolveError
import com.swithun.jsxgraph.core.JsxGraphGridRole

internal sealed interface GridError {
    data class InvalidParentCount(val count: Int) : GridError

    data class InvalidParent(
        val index: Int,
        val id: String,
        val type: Int,
    ) : GridError

    data class ParentBoardMismatch(
        val index: Int,
        val id: String,
    ) : GridError

    data class ParentNotRegistered(
        val index: Int,
        val id: String,
    ) : GridError

    data class MajorCurveFactory(val error: CurveError) : GridError

    data class MinorCurveFactory(val error: CurveError) : GridError

    data class Geometry(
        val role: JsxGraphGridRole,
        val error: JsxGraphGridResolveError,
    ) : GridError
}

internal object Grid {
    // JSXGraph 1.13.3: src/element/grid.js -> JXG.createGrid.
    fun create(
        board: Board,
        parentAxes: List<Line>,
        definition: JsxGraphGrid2D,
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = false,
        minorId: String? = null,
        minorName: String? = null,
    ): GMResult<Curve, GridError> {
        if (parentAxes.size > 2) {
            return GMResult.Err(
                GridError.InvalidParentCount(parentAxes.size),
            )
        }
        parentAxes.forEachIndexed { index, axis ->
            when {
                axis.type != Const.OBJECT_TYPE_AXIS ->
                    return GMResult.Err(
                        GridError.InvalidParent(
                            index = index,
                            id = axis.id,
                            type = axis.type,
                        ),
                    )
                axis.board !== board ->
                    return GMResult.Err(
                        GridError.ParentBoardMismatch(index, axis.id),
                    )
                board.elementById(axis.id) !== axis ->
                    return GMResult.Err(
                        GridError.ParentNotRegistered(index, axis.id),
                    )
            }
        }

        val majorDefinition = definition.copy(role = JsxGraphGridRole.Major)
        val major = when (
            val result = Curve.createData(
                board = board,
                dataX = doubleArrayOf(),
                dataY = doubleArrayOf(),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err ->
                return GMResult.Err(GridError.MajorCurveFactory(result.error))
        }
        major.configureGrid(
            definition = majorDefinition,
            bezierDegree = majorDefinition.bezierDegree(),
        )
        when (val data = majorDefinition.data(board)) {
            is GMResult.Ok -> major.replaceData(data.value)
            is GMResult.Err -> {
                board.removeObject(major)
                return GMResult.Err(
                    GridError.Geometry(
                        role = JsxGraphGridRole.Major,
                        error = data.error,
                    ),
                )
            }
        }

        val resolvedMinorId = minorId
            ?.takeUnless { it == id }
            ?: "${major.id}_minor"
        val resolvedMinorName = minorName
            ?.takeUnless { it == name }
            ?: "${major.name}_minor"
        val minorDefinition = definition.copy(role = JsxGraphGridRole.Minor)
        val minor = when (
            val result = Curve.createData(
                board = board,
                dataX = doubleArrayOf(),
                dataY = doubleArrayOf(),
                id = resolvedMinorId,
                name = resolvedMinorName,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(major)
                return GMResult.Err(
                    GridError.MinorCurveFactory(result.error),
                )
            }
        }
        minor.configureGrid(
            definition = minorDefinition,
            bezierDegree = minorDefinition.bezierDegree(),
        )
        when (val data = minorDefinition.data(board)) {
            is GMResult.Ok -> minor.replaceData(data.value)
            is GMResult.Err -> {
                board.removeObject(minor)
                board.removeObject(major)
                return GMResult.Err(
                    GridError.Geometry(
                        role = JsxGraphGridRole.Minor,
                        error = data.error,
                    ),
                )
            }
        }
        minor.dump = false

        major.minorGrid = minor
        minor.majorGrid = major
        major.inherits += minor
        major.setParents(parentAxes)
        minor.setParents(parentAxes)
        board.grids += major
        board.grids += minor
        return GMResult.Ok(major)
    }

    private fun JsxGraphGrid2D.data(
        board: Board,
    ): GMResult<CurveDataUpdate, JsxGraphGridResolveError> {
        val boundingBox = board.getBoundingBox()
        return when (
            val resolved = resolve(
                visibleLeft = boundingBox[0],
                visibleTop = boundingBox[1],
                visibleRight = boundingBox[2],
                visibleBottom = boundingBox[3],
                cssPixelsPerUnitX = board.unitX,
                cssPixelsPerUnitY = board.unitY,
            )
        ) {
            is GMResult.Ok ->
                GMResult.Ok(
                    CurveDataUpdate(
                        x = resolved.value.points.map { point ->
                            point?.x ?: Double.NaN
                        }.toDoubleArray(),
                        y = resolved.value.points.map { point ->
                            point?.y ?: Double.NaN
                        }.toDoubleArray(),
                    ),
                )
            is GMResult.Err -> resolved
        }
    }

    private fun Curve.replaceData(data: CurveDataUpdate) {
        replaceData(data.x, data.y)
    }

    private fun JsxGraphGrid2D.bezierDegree(): Int {
        val face =
            if (role == JsxGraphGridRole.Major) major.face else minor.face
        return if (face.lowercase() in setOf("o", "circle")) 3 else 1
    }
}
