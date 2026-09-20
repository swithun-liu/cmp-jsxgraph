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
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.cos
import kotlin.math.sin

internal sealed interface TransformationDynamicParameterError {
    data class Rejected(
        val reason: String,
    ) : TransformationDynamicParameterError
}

internal fun interface TransformationDynamicParameter {
    fun evaluate(): GMResult<Double, TransformationDynamicParameterError>
}

internal sealed interface TransformationParameter {
    data class Numeric(
        val value: Double,
    ) : TransformationParameter

    data class Expression(
        val source: String,
    ) : TransformationParameter

    data class Dynamic(
        val evaluator: TransformationDynamicParameter,
    ) : TransformationParameter
}

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

    data class ParameterExpressionCompile(
        val transformationType: String,
        val parameterIndex: Int,
        val error: JessieCodeExpressionCompileError,
    ) : TransformationError

    data class ParameterExpressionEvaluation(
        val transformationType: String,
        val parameterIndex: Int,
        val error: JessieCodeRuntimeError,
    ) : TransformationError

    data class ParameterExpressionResult(
        val transformationType: String,
        val parameterIndex: Int,
        val actualType: String,
    ) : TransformationError

    data class DynamicParameterEvaluation(
        val transformationType: String,
        val parameterIndex: Int,
        val error: TransformationDynamicParameterError,
    ) : TransformationError

    data class DynamicMeltUnsupported(
        val transformationType: String,
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
 * 2D numerical and dynamic-parameter slice of JXG.Transformation.
 *
 * Persistent element bindings are represented by GeometryElement and
 * CoordsElement. The 3D matrix forms remain untranslated.
 */
internal class Transformation private constructor(
    internal val transformationType: TransformationType,
    matrix: Array<DoubleArray>,
    internal val isNumericMatrix: Boolean,
    private var matrixEvaluator:
        () -> GMResult<Array<DoubleArray>, TransformationError>,
) {
    internal val elementClass: Int = Const.OBJECT_CLASS_OTHER
    internal val type: Int = Const.OBJECT_TYPE_TRANSFORMATION
    internal val elType: String = ""
    internal val is3D: Boolean = false

    internal var matrix: Array<DoubleArray> = matrix
        private set

    internal var evaluationError: TransformationError? = null
        private set

    // JSXGraph: src/base/transformation.js -> update
    internal fun update(): Transformation {
        updateResult()
        return this
    }

    internal fun updateResult(): GMResult<
        Transformation,
        TransformationError,
        > =
        when (val result = matrixEvaluator()) {
            is GMResult.Ok -> {
                matrix = result.value
                evaluationError = null
                GMResult.Ok(this)
            }
            is GMResult.Err -> {
                evaluationError = result.error
                GMResult.Err(result.error)
            }
        }

    // JSXGraph: src/base/transformation.js -> apply
    internal fun apply(
        point: CoordsElement,
        self: Boolean = false,
    ): DoubleArray =
        when (val result = applyResult(point, self)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> DoubleArray(3) { Double.NaN }
        }

    internal fun applyResult(
        point: CoordsElement,
        self: Boolean = false,
    ): GMResult<DoubleArray, TransformationError> {
        when (val result = updateResult()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val coordinates =
            if (self) {
                point.initialCoords.usrCoords
            } else {
                point.coords.usrCoords
            }
        return GMResult.Ok(Mat.matVecMult(matrix, coordinates))
    }

    internal fun applyResult(
        coordinates: DoubleArray,
    ): GMResult<DoubleArray, TransformationError> {
        when (val result = updateResult()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        return GMResult.Ok(Mat.matVecMult(matrix, coordinates))
    }

    // JSXGraph: src/base/transformation.js -> applyOnce
    internal fun applyOnce(
        point: CoordsElement,
    ): GMResult<Unit, TransformationError> = applyOnce(listOf(point))

    // JSXGraph: src/base/transformation.js -> applyOnce
    internal fun applyOnce(
        points: Iterable<CoordsElement>,
    ): GMResult<Unit, TransformationError> {
        for (point in points) {
            when (val result = updateResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            point.coords.setCoordinates(
                coordType = Const.COORDS_BY_USER,
                coordinates = Mat.matVecMult(
                    matrix,
                    point.coords.usrCoords,
                ),
            )
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph: src/base/transformation.js -> bindTo / bind
    internal fun bindTo(element: GeometryElement) {
        element.transformations += this
    }

    internal fun bindTo(elements: Iterable<GeometryElement>) {
        for (element in elements) {
            bindTo(element)
        }
    }

    // JSXGraph: src/base/transformation.js -> meltTo
    internal fun meltTo(
        element: GeometryElement,
    ): GMResult<Unit, TransformationError> = meltTo(listOf(element))

    internal fun meltTo(
        elements: Iterable<GeometryElement>,
    ): GMResult<Unit, TransformationError> {
        if (!isNumericMatrix) {
            /*
             * JSXGraph 1.13.3 clone() returns null for a dynamic transform,
             * while meltTo() appends that null and fails on the next update.
             * Keep the unsupported upstream edge explicit in Kotlin.
             */
            return GMResult.Err(
                TransformationError.DynamicMeltUnsupported(
                    transformationType.upstreamName,
                ),
            )
        }

        for (element in elements) {
            val previous = element.transformations.lastOrNull()
            if (previous != null && previous.isNumericMatrix) {
                when (val result = previous.melt(this)) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return GMResult.Err(result.error)
                }
            } else {
                val copy = clone()
                    ?: return GMResult.Err(
                        TransformationError.DynamicMeltUnsupported(
                            transformationType.upstreamName,
                        ),
                    )
                element.transformations += copy
            }
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph: src/base/transformation.js -> clone
    internal fun clone(): Transformation? {
        update()
        if (!isNumericMatrix) {
            return null
        }
        val clonedMatrix = matrix.deepCopy()
        return Transformation(
            transformationType = transformationType,
            matrix = clonedMatrix,
            isNumericMatrix = true,
            matrixEvaluator = {
                GMResult.Ok(clonedMatrix.deepCopy())
            },
        )
    }

    // JSXGraph: src/base/transformation.js -> melt
    internal fun melt(
        transformation: Transformation,
    ): GMResult<Transformation, TransformationError> {
        if (!isNumericMatrix || !transformation.isNumericMatrix) {
            return GMResult.Err(
                TransformationError.DynamicMeltUnsupported(
                    transformationType.upstreamName,
                ),
            )
        }
        when (val result = updateResult()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        when (val result = transformation.updateResult()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val meltedMatrix =
            Mat.matMatMult(transformation.matrix, matrix)
        matrix = meltedMatrix
        matrixEvaluator = {
            GMResult.Ok(meltedMatrix.deepCopy())
        }
        return GMResult.Ok(this)
    }

    internal companion object {
        // JSXGraph: src/base/transformation.js -> setMatrix
        internal fun create(
            type: String,
            parameters: DoubleArray,
        ): GMResult<Transformation, TransformationError> =
            createResolved(
                type = type,
                parameters = parameters.mapIndexed {
                        index,
                        value,
                    ->
                    ResolvedParameter.numeric(index, value)
                },
            )

        // JSXGraph: src/base/transformation.js -> setMatrix;
        // src/utils/type.js -> createEvalFunction
        internal fun create(
            board: Board,
            type: String,
            parameters: List<TransformationParameter>,
        ): GMResult<Transformation, TransformationError> {
            val transformationType =
                when (val result = parseType(type)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            when (
                val result = validateParameterCount(
                    transformationType = transformationType,
                    originalType = type,
                    actualCount = parameters.size,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            val resolved = when (
                val result = resolveParameters(
                    board = board,
                    transformationType = transformationType,
                    parameters = parameters,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return createResolved(
                transformationType = transformationType,
                originalType = type,
                parameters = resolved,
            )
        }

        private fun createResolved(
            type: String,
            parameters: List<ResolvedParameter>,
        ): GMResult<Transformation, TransformationError> {
            val transformationType =
                when (val result = parseType(type)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            return createResolved(
                transformationType = transformationType,
                originalType = type,
                parameters = parameters,
            )
        }

        private fun createResolved(
            transformationType: TransformationType,
            originalType: String,
            parameters: List<ResolvedParameter>,
        ): GMResult<Transformation, TransformationError> {
            when (
                val result = validateParameterCount(
                    transformationType = transformationType,
                    originalType = originalType,
                    actualCount = parameters.size,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }

            val isNumericMatrix = parameters.all(ResolvedParameter::isNumeric)
            val evaluator = {
                when (
                    val result = evaluateParameters(
                        parameters = parameters,
                    )
                ) {
                    is GMResult.Ok -> GMResult.Ok(
                        matrixFromParameters(
                            transformationType = transformationType,
                            parameters = result.value,
                        ),
                    )
                    is GMResult.Err -> result
                }
            }
            val initialMatrix =
                if (isNumericMatrix) {
                    when (val result = evaluator()) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                } else {
                    Mat.identity(3)
                }
            return GMResult.Ok(
                Transformation(
                    transformationType = transformationType,
                    matrix = initialMatrix,
                    isNumericMatrix = isNumericMatrix,
                    matrixEvaluator = evaluator,
                ),
            )
        }

        // JSXGraph: src/base/transformation.js -> setMatrix
        internal fun create(
            type: String,
            matrix: Array<DoubleArray>,
        ): GMResult<Transformation, TransformationError> =
            createMatrix(
                type = type,
                matrix = matrix.map { row ->
                    row.mapIndexed { index, value ->
                        ResolvedParameter.numeric(index, value)
                    }
                },
            )

        // JSXGraph: src/base/transformation.js ->
        // setMatrix("affinematrix" | "matrix")
        internal fun createMatrix(
            board: Board,
            type: String,
            matrix: List<List<TransformationParameter>>,
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
            if (matrix.size != expectedSize ||
                matrix.any { it.size != expectedSize }
            ) {
                return GMResult.Err(
                    TransformationError.InvalidMatrixShape(
                        transformationType = type,
                        expectedRows = expectedSize,
                        expectedColumns = expectedSize,
                        actualRowSizes = matrix.map { row -> row.size },
                    ),
                )
            }
            val resolvedRows =
                mutableListOf<List<ResolvedParameter>>()
            for ((rowIndex, row) in matrix.withIndex()) {
                val resolved = when (
                    val result = resolveParameters(
                        board = board,
                        transformationType = transformationType,
                        parameters = row,
                        parameterOffset = rowIndex * expectedSize,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                resolvedRows += resolved
            }
            return createMatrix(
                transformationType = transformationType,
                originalType = type,
                matrix = resolvedRows,
            )
        }

        private fun createMatrix(
            type: String,
            matrix: List<List<ResolvedParameter>>,
        ): GMResult<Transformation, TransformationError> {
            val transformationType =
                when (val result = parseType(type)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            return createMatrix(
                transformationType = transformationType,
                originalType = type,
                matrix = matrix,
            )
        }

        private fun createMatrix(
            transformationType: TransformationType,
            originalType: String,
            matrix: List<List<ResolvedParameter>>,
        ): GMResult<Transformation, TransformationError> {
            val expectedSize =
                when (transformationType) {
                    TransformationType.AFFINE_MATRIX -> 2
                    TransformationType.MATRIX -> 3
                    else -> return GMResult.Err(
                        TransformationError.InvalidParameterForm(
                            transformationType = originalType,
                            expectedForm = "numeric parameters",
                        ),
                    )
                }
            if (matrix.size != expectedSize ||
                matrix.any { it.size != expectedSize }
            ) {
                return GMResult.Err(
                    TransformationError.InvalidMatrixShape(
                        transformationType = originalType,
                        expectedRows = expectedSize,
                        expectedColumns = expectedSize,
                        actualRowSizes = matrix.map(List<ResolvedParameter>::size),
                    ),
                )
            }

            val flattened = matrix.flatten()
            val isNumericMatrix =
                flattened.all(ResolvedParameter::isNumeric)
            val evaluator = {
                when (val result = evaluateParameters(flattened)) {
                    is GMResult.Ok -> GMResult.Ok(
                        matrixFromMatrixParameters(
                            transformationType = transformationType,
                            parameters = result.value,
                        ),
                    )
                    is GMResult.Err -> result
                }
            }
            val initialMatrix =
                if (isNumericMatrix) {
                    when (val result = evaluator()) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                } else {
                    Mat.identity(3)
                }
            return GMResult.Ok(
                Transformation(
                    transformationType = transformationType,
                    matrix = initialMatrix,
                    isNumericMatrix = isNumericMatrix,
                    matrixEvaluator = evaluator,
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
                constantTransformation(
                    transformationType = TransformationType.REFLECT,
                    matrix = reflectionMatrix(standardForm),
                ),
            )
        }

        // JSXGraph: src/base/transformation.js -> setMatrix("reflect", [line])
        internal fun createReflectionFromLine(
            line: Line,
        ): Transformation =
            dynamicTransformation(
                transformationType = TransformationType.REFLECT,
            ) {
                GMResult.Ok(reflectionMatrix(line.stdform))
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
                constantTransformation(
                    transformationType = TransformationType.REFLECT,
                    matrix = reflectionMatrix(
                        Mat.crossProduct(second, first),
                    ),
                ),
            )
        }

        // JSXGraph: src/base/transformation.js ->
        // setMatrix("reflect", [point, point])
        internal fun createReflectionFromPoints(
            first: CoordsElement,
            second: CoordsElement,
        ): Transformation =
            dynamicTransformation(
                transformationType = TransformationType.REFLECT,
            ) {
                GMResult.Ok(
                    reflectionMatrix(
                        Mat.crossProduct(
                            second.coords.usrCoords,
                            first.coords.usrCoords,
                        ),
                    ),
                )
            }

        // JSXGraph: src/base/transformation.js ->
        // setMatrix("rotate", [angle, point])
        internal fun createRotation(
            board: Board,
            angle: TransformationParameter,
            center: CoordsElement,
        ): GMResult<Transformation, TransformationError> {
            val resolved = when (
                val result = resolveParameters(
                    board = board,
                    transformationType = TransformationType.ROTATE,
                    parameters = listOf(angle),
                )
            ) {
                is GMResult.Ok -> result.value.single()
                is GMResult.Err -> return result
            }
            return GMResult.Ok(
                dynamicTransformation(
                    transformationType = TransformationType.ROTATE,
                ) {
                    when (val result = resolved.evaluate()) {
                        is GMResult.Ok -> GMResult.Ok(
                            rotationMatrix(
                                angle = result.value,
                                x = center.X(),
                                y = center.Y(),
                            ),
                        )
                        is GMResult.Err -> result
                    }
                },
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

        private fun validateParameterCount(
            transformationType: TransformationType,
            originalType: String,
            actualCount: Int,
        ): GMResult<Unit, TransformationError> {
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
                            transformationType = originalType,
                            expectedForm = "matrix",
                        ),
                    )
                }
            return if (actualCount in expectedCounts) {
                GMResult.Ok(Unit)
            } else {
                GMResult.Err(
                    TransformationError.InvalidParameterCount(
                        transformationType = originalType,
                        expectedCounts = expectedCounts,
                        actualCount = actualCount,
                    ),
                )
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

        private fun reflectionMatrix(
            line: DoubleArray,
        ): Array<DoubleArray> =
            Mat.identity(3).also { matrix ->
                setReflectionMatrix(matrix, line)
            }

        private fun rotationMatrix(
            angle: Double,
            x: Double? = null,
            y: Double? = null,
        ): Array<DoubleArray> {
            val cosine = cos(angle)
            val sine = sin(angle)
            return Mat.identity(3).also { matrix ->
                matrix[1][1] = cosine
                matrix[1][2] = -sine
                matrix[2][1] = sine
                matrix[2][2] = cosine
                if (x != null && y != null) {
                    matrix[1][0] = x * (1.0 - cosine) + y * sine
                    matrix[2][0] = y * (1.0 - cosine) - x * sine
                }
            }
        }

        private fun matrixFromParameters(
            transformationType: TransformationType,
            parameters: DoubleArray,
        ): Array<DoubleArray> {
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
                TransformationType.REFLECT -> setReflectionMatrix(
                    matrix = matrix,
                    line = Mat.crossProduct(
                        doubleArrayOf(
                            1.0,
                            parameters[2],
                            parameters[3],
                        ),
                        doubleArrayOf(
                            1.0,
                            parameters[0],
                            parameters[1],
                        ),
                    ),
                )
                TransformationType.ROTATE -> return rotationMatrix(
                    angle = parameters[0],
                    x = parameters.getOrNull(1),
                    y = parameters.getOrNull(2),
                )
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
                            matrix[row][column] =
                                parameters[row * 3 + column]
                        }
                    }
                }
                TransformationType.AFFINE_MATRIX,
                TransformationType.MATRIX,
                -> Unit
            }
            return matrix
        }

        private fun matrixFromMatrixParameters(
            transformationType: TransformationType,
            parameters: DoubleArray,
        ): Array<DoubleArray> =
            if (transformationType == TransformationType.AFFINE_MATRIX) {
                Mat.identity(3).also { result ->
                    for (row in 0 until 2) {
                        for (column in 0 until 2) {
                            result[row + 1][column + 1] =
                                parameters[row * 2 + column]
                        }
                    }
                }
            } else {
                Array(3) { row ->
                    DoubleArray(3) { column ->
                        parameters[row * 3 + column]
                    }
                }
            }

        private fun resolveParameters(
            board: Board,
            transformationType: TransformationType,
            parameters: List<TransformationParameter>,
            parameterOffset: Int = 0,
        ): GMResult<List<ResolvedParameter>, TransformationError> {
            val resolved = mutableListOf<ResolvedParameter>()
            for ((localIndex, parameter) in parameters.withIndex()) {
                val parameterIndex = parameterOffset + localIndex
                when (parameter) {
                    is TransformationParameter.Numeric ->
                        resolved += ResolvedParameter.numeric(
                            parameterIndex,
                            parameter.value,
                        )
                    is TransformationParameter.Dynamic ->
                        resolved += ResolvedParameter(
                            index = parameterIndex,
                            isNumeric = false,
                        ) {
                            when (val result = parameter.evaluator.evaluate()) {
                                is GMResult.Ok -> result
                                is GMResult.Err -> GMResult.Err(
                                    TransformationError.DynamicParameterEvaluation(
                                        transformationType =
                                            transformationType.upstreamName,
                                        parameterIndex = parameterIndex,
                                        error = result.error,
                                    ),
                                )
                            }
                        }
                    is TransformationParameter.Expression -> {
                        val function = when (
                            val result =
                                JessieCodeExpressionFunction.compile(
                                    source = parameter.source,
                                    board = board,
                                )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return GMResult.Err(
                                TransformationError.ParameterExpressionCompile(
                                    transformationType =
                                        transformationType.upstreamName,
                                    parameterIndex = parameterIndex,
                                    error = result.error,
                                ),
                            )
                        }
                        resolved += expressionParameter(
                            transformationType = transformationType,
                            parameterIndex = parameterIndex,
                            function = function,
                        )
                    }
                }
            }
            return GMResult.Ok(resolved)
        }

        private fun expressionParameter(
            transformationType: TransformationType,
            parameterIndex: Int,
            function: JessieCodeExpressionFunction,
        ): ResolvedParameter =
            ResolvedParameter(
                index = parameterIndex,
                isNumeric = false,
            ) {
                when (val result = function.evaluate()) {
                    is GMResult.Err -> GMResult.Err(
                        TransformationError.ParameterExpressionEvaluation(
                            transformationType =
                                transformationType.upstreamName,
                            parameterIndex = parameterIndex,
                            error = result.error,
                        ),
                    )
                    is GMResult.Ok -> {
                        val value = result.value
                        if (value is JessieCodeRuntimeValue.NumberValue) {
                            GMResult.Ok(value.value)
                        } else {
                            GMResult.Err(
                                TransformationError.ParameterExpressionResult(
                                    transformationType =
                                        transformationType.upstreamName,
                                    parameterIndex = parameterIndex,
                                    actualType = runtimeType(value),
                                ),
                            )
                        }
                    }
                }
            }

        private fun evaluateParameters(
            parameters: List<ResolvedParameter>,
        ): GMResult<DoubleArray, TransformationError> {
            val values = DoubleArray(parameters.size)
            for ((index, parameter) in parameters.withIndex()) {
                when (val result = parameter.evaluate()) {
                    is GMResult.Ok -> values[index] = result.value
                    is GMResult.Err -> return result
                }
            }
            return GMResult.Ok(values)
        }

        private fun constantTransformation(
            transformationType: TransformationType,
            matrix: Array<DoubleArray>,
        ): Transformation {
            val snapshot = matrix.deepCopy()
            return Transformation(
                transformationType = transformationType,
                matrix = snapshot,
                isNumericMatrix = true,
                matrixEvaluator = {
                    GMResult.Ok(snapshot.deepCopy())
                },
            )
        }

        private fun dynamicTransformation(
            transformationType: TransformationType,
            evaluator:
                () -> GMResult<Array<DoubleArray>, TransformationError>,
        ): Transformation =
            Transformation(
                transformationType = transformationType,
                matrix = Mat.identity(3),
                isNumericMatrix = false,
                matrixEvaluator = evaluator,
            )

        private fun runtimeType(
            value: JessieCodeRuntimeValue,
        ): String =
            when (value) {
                JessieCodeRuntimeValue.UndefinedValue -> "undefined"
                JessieCodeRuntimeValue.NullValue -> "null"
                is JessieCodeRuntimeValue.NumberValue -> "number"
                is JessieCodeRuntimeValue.BooleanValue -> "boolean"
                is JessieCodeRuntimeValue.StringValue -> "string"
                is JessieCodeRuntimeValue.ArrayValue -> "array"
                is JessieCodeRuntimeValue.ObjectValue -> "object"
                is JessieCodeRuntimeValue.FunctionValue -> "function"
                is JessieCodeRuntimeValue.BoardReference -> "board"
                is JessieCodeRuntimeValue.TransformationReference ->
                    "transformation"
                is JessieCodeRuntimeValue.CompositionReference ->
                    "composition"
                is JessieCodeRuntimeValue.ElementReference -> "element"
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

    private class ResolvedParameter(
        internal val index: Int,
        internal val isNumeric: Boolean,
        private val evaluator:
            () -> GMResult<Double, TransformationError>,
    ) {
        internal fun evaluate(): GMResult<Double, TransformationError> =
            evaluator()

        internal companion object {
            internal fun numeric(
                index: Int,
                value: Double,
            ): ResolvedParameter =
                ResolvedParameter(
                    index = index,
                    isNumeric = true,
                    evaluator = { GMResult.Ok(value) },
                )
        }
    }
}
