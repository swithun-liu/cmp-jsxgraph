/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/transformation.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.cos
import kotlin.math.sin

internal sealed interface TransformationError {
    data class UnsupportedType(
        val transformationType: String,
    ) : TransformationError

    data class InvalidParameterCount(
        val transformationType: String,
        val expectedCounts: List<Int>,
        val actualCount: Int,
    ) : TransformationError

    data class InvalidParameterForm(
        val transformationType: String,
        val expectedForm: String,
    ) : TransformationError

    data class InvalidMatrixShape(
        val transformationType: String,
        val expectedRows: Int,
        val expectedColumns: Int,
        val actualRowSizes: List<Int>,
    ) : TransformationError

    data class InvalidCoordinateCount(
        val coordinateRole: String,
        val expectedCounts: List<Int>,
        val actualCount: Int,
    ) : TransformationError
}

internal enum class TransformationType(
    internal val upstreamName: String,
) {
    TRANSLATE("translate"),
    SCALE("scale"),
    REFLECT("reflect"),
    ROTATE("rotate"),
    SHEAR("shear"),
    AFFINE("affine"),
    AFFINE_MATRIX("affinematrix"),
    GENERIC("generic"),
    MATRIX("matrix"),
}

/**
 * Static 2D numerical slice of JXG.Transformation.
 *
 * Dynamic parameters, 3D matrices, persistent element bindings, and the
 * transformed-element update lifecycle remain untranslated.
 */
internal class Transformation private constructor(
    internal val transformationType: TransformationType,
    matrix: Array<DoubleArray>,
) {
    internal val elementClass: Int = Const.OBJECT_CLASS_OTHER
    internal val type: Int = Const.OBJECT_TYPE_TRANSFORMATION
    internal val elType: String = ""
    internal val is3D: Boolean = false
    internal val isNumericMatrix: Boolean = true

    internal var matrix: Array<DoubleArray> = matrix
        private set

    // JSXGraph: src/base/transformation.js -> update
    internal fun update(): Transformation = this

    // JSXGraph: src/base/transformation.js -> apply
    internal fun apply(
        point: CoordsElement,
        self: Boolean = false,
    ): DoubleArray {
        update()
        val coordinates =
            if (self) {
                point.initialCoords.usrCoords
            } else {
                point.coords.usrCoords
            }
        return Mat.matVecMult(matrix, coordinates)
    }

    // JSXGraph: src/base/transformation.js -> applyOnce
    internal fun applyOnce(point: CoordsElement) {
        applyOnce(listOf(point))
    }

    // JSXGraph: src/base/transformation.js -> applyOnce
    internal fun applyOnce(points: Iterable<CoordsElement>) {
        for (point in points) {
            update()
            point.coords.setCoordinates(
                coordType = Const.COORDS_BY_USER,
                coordinates = Mat.matVecMult(
                    matrix,
                    point.coords.usrCoords,
                ),
            )
        }
    }

    // JSXGraph: src/base/transformation.js -> clone
    internal fun clone(): Transformation {
        update()
        return Transformation(
            transformationType = transformationType,
            matrix = matrix.deepCopy(),
        )
    }

    // JSXGraph: src/base/transformation.js -> melt
    internal fun melt(transformation: Transformation): Transformation {
        update()
        transformation.update()
        matrix = Mat.matMatMult(transformation.matrix, matrix)
        return this
    }

    internal companion object {
        // JSXGraph: src/base/transformation.js -> setMatrix
        internal fun create(
            type: String,
            parameters: DoubleArray,
        ): GMResult<Transformation, TransformationError> {
            val transformationType =
                when (val result = parseType(type)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }

            val expectedCounts =
                when (transformationType) {
                    TransformationType.TRANSLATE -> listOf(2)
                    TransformationType.SCALE -> listOf(2)
                    TransformationType.REFLECT -> listOf(4)
                    TransformationType.ROTATE -> listOf(1, 3)
                    TransformationType.SHEAR -> listOf(2)
                    TransformationType.AFFINE -> listOf(4)
                    TransformationType.GENERIC -> listOf(9)
                    TransformationType.AFFINE_MATRIX,
                    TransformationType.MATRIX,
                    -> return GMResult.Err(
                        TransformationError.InvalidParameterForm(
                            transformationType = type,
                            expectedForm = "matrix",
                        ),
                    )
                }
            if (parameters.size !in expectedCounts) {
                return GMResult.Err(
                    TransformationError.InvalidParameterCount(
                        transformationType = type,
                        expectedCounts = expectedCounts,
                        actualCount = parameters.size,
                    ),
                )
            }

            val matrix = Mat.identity(3)
            when (transformationType) {
                TransformationType.TRANSLATE -> {
                    matrix[1][0] = parameters[0]
                    matrix[2][0] = parameters[1]
                }
                TransformationType.SCALE -> {
                    matrix[1][1] = parameters[0]
                    matrix[2][2] = parameters[1]
                }
                TransformationType.REFLECT -> {
                    setReflectionMatrix(
                        matrix = matrix,
                        line = Mat.crossProduct(
                            doubleArrayOf(1.0, parameters[2], parameters[3]),
                            doubleArrayOf(1.0, parameters[0], parameters[1]),
                        ),
                    )
                }
                TransformationType.ROTATE -> {
                    val angle = parameters[0]
                    val cosine = cos(angle)
                    val sine = sin(angle)
                    matrix[1][1] = cosine
                    matrix[1][2] = -sine
                    matrix[2][1] = sine
                    matrix[2][2] = cosine
                    if (parameters.size == 3) {
                        val x = parameters[1]
                        val y = parameters[2]
                        matrix[1][0] = x * (1.0 - cosine) + y * sine
                        matrix[2][0] = y * (1.0 - cosine) - x * sine
                    }
                }
                TransformationType.SHEAR -> {
                    matrix[1][2] = parameters[0]
                    matrix[2][1] = parameters[1]
                }
                TransformationType.AFFINE -> {
                    /*
                     * JSXGraph 1.13.3 requests nine evaluators for four
                     * parameters here. Its update function only reads these
                     * four documented entries.
                     */
                    matrix[1][1] = parameters[0]
                    matrix[1][2] = parameters[1]
                    matrix[2][1] = parameters[2]
                    matrix[2][2] = parameters[3]
                }
                TransformationType.GENERIC -> {
                    for (row in 0 until 3) {
                        for (column in 0 until 3) {
                            matrix[row][column] = parameters[row * 3 + column]
                        }
                    }
                }
                TransformationType.AFFINE_MATRIX,
                TransformationType.MATRIX,
                -> Unit
            }
            return GMResult.Ok(
                Transformation(
                    transformationType = transformationType,
                    matrix = matrix,
                ),
            )
        }

        // JSXGraph: src/base/transformation.js -> setMatrix
        internal fun create(
            type: String,
            matrix: Array<DoubleArray>,
        ): GMResult<Transformation, TransformationError> {
            val transformationType =
                when (val result = parseType(type)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            val expectedSize =
                when (transformationType) {
                    TransformationType.AFFINE_MATRIX -> 2
                    TransformationType.MATRIX -> 3
                    else -> return GMResult.Err(
                        TransformationError.InvalidParameterForm(
                            transformationType = type,
                            expectedForm = "numeric parameters",
                        ),
                    )
                }
            if (
                matrix.size != expectedSize ||
                matrix.any { it.size != expectedSize }
            ) {
                return GMResult.Err(
                    TransformationError.InvalidMatrixShape(
                        transformationType = type,
                        expectedRows = expectedSize,
                        expectedColumns = expectedSize,
                        actualRowSizes = matrix.map(DoubleArray::size),
                    ),
                )
            }

            val transformationMatrix =
                if (transformationType == TransformationType.AFFINE_MATRIX) {
                    Mat.identity(3).also { result ->
                        for (row in 0 until 2) {
                            for (column in 0 until 2) {
                                result[row + 1][column + 1] =
                                    matrix[row][column]
                            }
                        }
                    }
                } else {
                    matrix.deepCopy()
                }
            return GMResult.Ok(
                Transformation(
                    transformationType = transformationType,
                    matrix = transformationMatrix,
                ),
            )
        }

        // JSXGraph: src/base/transformation.js -> setMatrix("reflect")
        internal fun createReflectionFromLine(
            standardForm: DoubleArray,
        ): GMResult<Transformation, TransformationError> {
            if (standardForm.size != 3 && standardForm.size != 4) {
                return GMResult.Err(
                    TransformationError.InvalidCoordinateCount(
                        coordinateRole = "line standard form",
                        expectedCounts = listOf(3, 4),
                        actualCount = standardForm.size,
                    ),
                )
            }
            return GMResult.Ok(
                Transformation(
                    transformationType = TransformationType.REFLECT,
                    matrix = Mat.identity(3).also { matrix ->
                        setReflectionMatrix(matrix, standardForm)
                    },
                ),
            )
        }

        // JSXGraph: src/base/transformation.js -> setMatrix("reflect")
        internal fun createReflectionFromPoints(
            first: DoubleArray,
            second: DoubleArray,
        ): GMResult<Transformation, TransformationError> {
            if (first.size != 3) {
                return invalidPointCoordinates("first reflection point", first)
            }
            if (second.size != 3) {
                return invalidPointCoordinates("second reflection point", second)
            }
            return GMResult.Ok(
                Transformation(
                    transformationType = TransformationType.REFLECT,
                    matrix = Mat.identity(3).also { matrix ->
                        setReflectionMatrix(
                            matrix = matrix,
                            line = Mat.crossProduct(second, first),
                        )
                    },
                ),
            )
        }

        private fun parseType(
            type: String,
        ): GMResult<TransformationType, TransformationError> {
            val transformationType =
                TransformationType.entries.firstOrNull {
                    it.upstreamName == type
                }
            return if (transformationType == null) {
                GMResult.Err(TransformationError.UnsupportedType(type))
            } else {
                GMResult.Ok(transformationType)
            }
        }

        private fun setReflectionMatrix(
            matrix: Array<DoubleArray>,
            line: DoubleArray,
        ) {
            var x = line[1]
            var y = line[2]
            val z = line[0]
            val projection = doubleArrayOf(
                -z * x,
                -z * y,
                x * x + y * y,
            )
            val denominator = projection[2]
            val xOffset = projection[0] / projection[2]
            val yOffset = projection[1] / projection[2]

            x = -line[2]
            y = line[1]
            matrix[1][1] = (x * x - y * y) / denominator
            matrix[1][2] = (2.0 * x * y) / denominator
            matrix[2][1] = matrix[1][2]
            matrix[2][2] = -matrix[1][1]
            matrix[1][0] =
                xOffset * (1.0 - matrix[1][1]) -
                yOffset * matrix[1][2]
            matrix[2][0] =
                yOffset * (1.0 - matrix[2][2]) -
                xOffset * matrix[2][1]
        }

        private fun invalidPointCoordinates(
            coordinateRole: String,
            coordinates: DoubleArray,
        ): GMResult<Transformation, TransformationError> =
            GMResult.Err(
                TransformationError.InvalidCoordinateCount(
                    coordinateRole = coordinateRole,
                    expectedCounts = listOf(3),
                    actualCount = coordinates.size,
                ),
            )

        private fun Array<DoubleArray>.deepCopy(): Array<DoubleArray> =
            Array(size) { index -> this[index].copyOf() }
    }
}
