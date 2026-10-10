/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/renderer/abstract.js -> joinTransforms
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat

internal sealed interface ScreenTransformationError {
    data class InvalidBoardMetric(
        val name: String,
        val value: Double,
    ) : ScreenTransformationError

    data class InvalidMatrixShape(
        val transformationIndex: Int,
        val rowSizes: List<Int>,
    ) : ScreenTransformationError

    data class Evaluation(
        val transformationIndex: Int,
        val error: TransformationError,
    ) : ScreenTransformationError
}

/**
 * JSXGraph 1.13.3: src/renderer/abstract.js -> joinTransforms.
 *
 * The returned homogeneous matrix maps absolute screen coordinates. Its
 * Canvas/SVG/CSS six-parameter form is
 * `[m[1][1], m[2][1], m[1][2], m[2][2], m[1][0], m[2][0]]`.
 */
internal fun joinScreenTransformations(
    board: Board,
    transformations: List<Transformation>,
): GMResult<Array<DoubleArray>, ScreenTransformationError> {
    val matrices = mutableListOf<Array<DoubleArray>>()
    for ((index, transformation) in transformations.withIndex()) {
        when (val result = transformation.updateResult()) {
            is GMResult.Ok -> matrices += transformation.matrix
            is GMResult.Err -> return GMResult.Err(
                ScreenTransformationError.Evaluation(
                    transformationIndex = index,
                    error = result.error,
                ),
            )
        }
    }
    return joinScreenTransformationMatrices(
        originX = board.origin.scrCoords[1],
        originY = board.origin.scrCoords[2],
        unitX = board.unitX,
        unitY = board.unitY,
        transformations = matrices,
    )
}

internal fun joinScreenTransformationMatrices(
    originX: Double,
    originY: Double,
    unitX: Double,
    unitY: Double,
    transformations: List<Array<DoubleArray>>,
): GMResult<Array<DoubleArray>, ScreenTransformationError> {
    for (
        (name, value) in listOf(
            "originX" to originX,
            "originY" to originY,
            "unitX" to unitX,
            "unitY" to unitY,
        )
    ) {
        if (!value.isFinite() || (name.startsWith("unit") && value == 0.0)) {
            return GMResult.Err(
                ScreenTransformationError.InvalidBoardMetric(name, value),
            )
        }
    }

    var matrix = arrayOf(
        doubleArrayOf(1.0, 0.0, 0.0),
        doubleArrayOf(-originX / unitX, 1.0 / unitX, 0.0),
        doubleArrayOf(originY / unitY, 0.0, -1.0 / unitY),
    )
    for ((index, transformationMatrix) in transformations.withIndex()) {
        if (
            transformationMatrix.size != 3 ||
            transformationMatrix.any { row -> row.size != 3 }
        ) {
            return GMResult.Err(
                ScreenTransformationError.InvalidMatrixShape(
                    transformationIndex = index,
                    rowSizes = transformationMatrix.map(DoubleArray::size),
                ),
            )
        }
        matrix = Mat.matMatMult(transformationMatrix, matrix)
    }
    return GMResult.Ok(
        Mat.matMatMult(
            arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(originX, unitX, 0.0),
                doubleArrayOf(originY, 0.0, -unitY),
            ),
            matrix,
        ),
    )
}
