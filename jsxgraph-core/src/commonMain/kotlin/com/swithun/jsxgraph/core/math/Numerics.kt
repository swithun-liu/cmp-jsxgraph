/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/numerics.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

sealed interface NumericsError {
    data class DimensionMismatch(
        val matrixRows: Int,
        val matrixColumns: Int,
        val vectorSize: Int,
    ) : NumericsError

    data object SingularMatrix : NumericsError

    data class InvalidInterval(val size: Int) : NumericsError

    data class InvalidIntegrationNodeCount(
        val nodeCount: Int,
        val type: IntegrationType,
    ) : NumericsError

    data class InvalidQuadratureOrder(val order: Int) : NumericsError

    data class InvalidSplineDefinition(
        val knotCount: Int,
        val valueCount: Int,
    ) : NumericsError

    data class InvalidSplineEvaluation(
        val knotCount: Int,
        val valueCount: Int,
        val secondDerivativeCount: Int,
    ) : NumericsError

    data class SplineValueOutOfDomain(
        val index: Int,
        val value: Double,
        val start: Double,
        val end: Double,
    ) : NumericsError

    data class InvalidSystemDimension(
        val dimension: Int,
        val initialValueCount: Int,
    ) : NumericsError

    data class InvalidFunctionResult(
        val expectedCount: Int,
        val actualCount: Int,
    ) : NumericsError

    data class InvalidJacobian(
        val expectedDimension: Int,
        val rowCount: Int,
        val shortestRowSize: Int,
    ) : NumericsError
}

enum class IntegrationType {
    TRAPEZ,
    SIMPSON,
    MILNE,
}

data class NewtonCotesConfig(
    val numberOfNodes: Int = 28,
    val integrationType: IntegrationType = IntegrationType.MILNE,
)

data class RombergConfig(
    val maxIterations: Int = 20,
    val epsilon: Double = 0.0000001,
)

data class JacobiResult(
    val diagonalizedMatrix: Array<DoubleArray>,
    val eigenvectors: Array<DoubleArray>,
)

data class DampedNewtonResult(
    val parameters: DoubleArray,
    val squaredResidual: Double,
)

object Numerics {
    private data class LegendreRule(
        val nodes: DoubleArray,
        val weights: DoubleArray,
    )

    private val defaultRandomSource = RandomSource { Random.nextDouble() }

    var maxIterationsRoot: Int = 80
    var maxIterationsMinimize: Int = 500

    // JSXGraph: src/math/numerics.js -> Gauss
    @Suppress("FunctionName")
    fun Gauss(
        inputMatrix: Array<DoubleArray>,
        inputVector: DoubleArray,
    ): GMResult<DoubleArray, NumericsError> {
        val columnCount = inputMatrix.firstOrNull()?.size ?: 0
        if (columnCount != inputVector.size || columnCount != inputMatrix.size) {
            return GMResult.Err(
                NumericsError.DimensionMismatch(
                    matrixRows = inputMatrix.size,
                    matrixColumns = columnCount,
                    vectorSize = inputVector.size,
                ),
            )
        }
        if (inputMatrix.any { it.size < columnCount }) {
            return GMResult.Err(
                NumericsError.DimensionMismatch(
                    matrixRows = inputMatrix.size,
                    matrixColumns = columnCount,
                    vectorSize = inputVector.size,
                ),
            )
        }

        val matrix = Array(columnCount) { row -> inputMatrix[row].copyOf(columnCount) }
        val solution = inputVector.copyOf(columnCount)
        for (column in 0 until columnCount) {
            for (row in columnCount - 1 downTo column + 1) {
                if (abs(matrix[row][column]) > Mat.eps) {
                    if (abs(matrix[column][column]) < Mat.eps) {
                        val rowSwap = matrix[row]
                        matrix[row] = matrix[column]
                        matrix[column] = rowSwap

                        val valueSwap = solution[row]
                        solution[row] = solution[column]
                        solution[column] = valueSwap
                    } else {
                        matrix[row][column] /= matrix[column][column]
                        solution[row] -= matrix[row][column] * solution[column]
                        for (innerColumn in column + 1 until columnCount) {
                            matrix[row][innerColumn] -=
                                matrix[row][column] * matrix[column][innerColumn]
                        }
                    }
                }
            }

            if (abs(matrix[column][column]) < Mat.eps) {
                return GMResult.Err(NumericsError.SingularMatrix)
            }
        }

        backwardSolve(matrix, solution, canModify = true)
        return GMResult.Ok(solution)
    }

    // JSXGraph: src/math/numerics.js -> backwardSolve
    fun backwardSolve(
        rightTriangularMatrix: Array<DoubleArray>,
        inputVector: DoubleArray,
        canModify: Boolean = false,
    ): DoubleArray {
        val solution = if (canModify) inputVector else inputVector.copyOf()
        val rowCount = rightTriangularMatrix.size
        val columnCount = rightTriangularMatrix.firstOrNull()?.size ?: 0
        for (row in rowCount - 1 downTo 0) {
            for (column in columnCount - 1 downTo row + 1) {
                solution[row] -= rightTriangularMatrix[row][column] * solution[column]
            }
            solution[row] /= rightTriangularMatrix[row][row]
        }
        return solution
    }

    // JSXGraph: src/math/numerics.js -> gaussBareiss
    fun gaussBareiss(input: Array<DoubleArray>): Double {
        var size = input.size
        if (size <= 0) {
            return 0.0
        }
        if (input[0].size < size) {
            size = input[0].size
        }

        val matrix = Array(size) { row -> input[row].copyOf(size) }
        var previousPivot = 1.0
        var sign = 1.0
        for (pivotIndex in 0 until size - 1) {
            var pivot = matrix[pivotIndex][pivotIndex]
            if (abs(pivot) < Mat.eps) {
                var replacementRow = pivotIndex + 1
                while (
                    replacementRow < size &&
                    abs(matrix[replacementRow][pivotIndex]) < Mat.eps
                ) {
                    replacementRow += 1
                }
                if (replacementRow == size) {
                    return 0.0
                }

                for (column in pivotIndex until size) {
                    val swap = matrix[replacementRow][column]
                    matrix[replacementRow][column] = matrix[pivotIndex][column]
                    matrix[pivotIndex][column] = swap
                }
                sign = -sign
                pivot = matrix[pivotIndex][pivotIndex]
            }

            for (row in pivotIndex + 1 until size) {
                for (column in pivotIndex + 1 until size) {
                    val transformed =
                        pivot * matrix[row][column] -
                            matrix[row][pivotIndex] * matrix[pivotIndex][column]
                    matrix[row][column] = transformed / previousPivot
                }
            }
            previousPivot = pivot
        }
        return sign * matrix[size - 1][size - 1]
    }

    // JSXGraph: src/math/numerics.js -> det
    fun det(matrix: Array<DoubleArray>): Double {
        if (matrix.size == 2 && matrix[0].size == 2) {
            return matrix[0][0] * matrix[1][1] - matrix[1][0] * matrix[0][1]
        }
        return gaussBareiss(matrix)
    }

    // JSXGraph: src/math/numerics.js -> Jacobi
    @Suppress("FunctionName")
    fun Jacobi(input: Array<DoubleArray>): JacobiResult {
        val size = input.size
        val eigenvectors = Mat.identity(size)
        val matrix = Array(size) { row -> input[row].copyOf(size) }
        val threshold = Mat.eps * Mat.eps
        var matrixMagnitude = 0.0
        for (row in 0 until size) {
            for (column in 0 until size) {
                matrixMagnitude += abs(matrix[row][column])
            }
        }

        if (size == 1 || matrixMagnitude <= 0.0) {
            return JacobiResult(matrix, eigenvectors)
        }
        matrixMagnitude /= (size * size).toDouble()

        var iterations = 0
        do {
            var offDiagonalMagnitude = 0.0
            for (column in 1 until size) {
                for (row in 0 until column) {
                    val absoluteEntry = abs(matrix[row][column])
                    offDiagonalMagnitude += absoluteEntry
                    if (absoluteEntry >= threshold) {
                        val angle =
                            atan2(
                                2.0 * matrix[row][column],
                                matrix[row][row] - matrix[column][column],
                            ) * 0.5
                        val sine = sin(angle)
                        val cosine = cos(angle)

                        for (index in 0 until size) {
                            var temporary = matrix[index][row]
                            matrix[index][row] =
                                cosine * temporary + sine * matrix[index][column]
                            matrix[index][column] =
                                -sine * temporary + cosine * matrix[index][column]

                            temporary = eigenvectors[index][row]
                            eigenvectors[index][row] =
                                cosine * temporary + sine * eigenvectors[index][column]
                            eigenvectors[index][column] =
                                -sine * temporary + cosine * eigenvectors[index][column]
                        }

                        matrix[row][row] =
                            cosine * matrix[row][row] + sine * matrix[column][row]
                        matrix[column][column] =
                            -sine * matrix[row][column] + cosine * matrix[column][column]
                        matrix[row][column] = 0.0

                        for (index in 0 until size) {
                            matrix[row][index] = matrix[index][row]
                            matrix[column][index] = matrix[index][column]
                        }
                    }
                }
            }
            iterations += 1
        } while (
            abs(offDiagonalMagnitude) / matrixMagnitude > threshold &&
            iterations < 2000
        )

        return JacobiResult(matrix, eigenvectors)
    }

    // JSXGraph: src/math/numerics.js -> NewtonCotes
    @Suppress("FunctionName")
    fun NewtonCotes(
        interval: DoubleArray,
        function: (Double) -> Double,
        config: NewtonCotesConfig = NewtonCotesConfig(),
    ): GMResult<Double, NumericsError> {
        if (interval.size < 2) {
            return GMResult.Err(NumericsError.InvalidInterval(interval.size))
        }

        val nodeCount = config.numberOfNodes
        if (
            (config.integrationType == IntegrationType.SIMPSON && nodeCount % 2 != 0) ||
            (config.integrationType == IntegrationType.MILNE && nodeCount % 4 != 0)
        ) {
            return GMResult.Err(
                NumericsError.InvalidIntegrationNodeCount(nodeCount, config.integrationType),
            )
        }

        val start = interval[0]
        val end = interval[1]
        val step = (end - start) / nodeCount
        var result: Double
        when (config.integrationType) {
            IntegrationType.TRAPEZ -> {
                result = (function(start) + function(end)) * 0.5
                var evaluationPoint = start
                for (index in 0 until nodeCount - 1) {
                    evaluationPoint += step
                    result += function(evaluationPoint)
                }
                result *= step
            }

            IntegrationType.SIMPSON -> {
                val intervalCount = nodeCount / 2
                result = function(start) + function(end)
                var evaluationPoint = start
                for (index in 0 until intervalCount - 1) {
                    evaluationPoint += 2.0 * step
                    result += 2.0 * function(evaluationPoint)
                }

                evaluationPoint = start - step
                for (index in 0 until intervalCount) {
                    evaluationPoint += 2.0 * step
                    result += 4.0 * function(evaluationPoint)
                }
                result *= step / 3.0
            }

            IntegrationType.MILNE -> {
                val intervalCount = nodeCount / 4
                result = 7.0 * (function(start) + function(end))
                var evaluationPoint = start
                for (index in 0 until intervalCount - 1) {
                    evaluationPoint += 4.0 * step
                    result += 14.0 * function(evaluationPoint)
                }

                evaluationPoint = start - 3.0 * step
                for (index in 0 until intervalCount) {
                    evaluationPoint += 4.0 * step
                    result +=
                        32.0 *
                        (
                            function(evaluationPoint) +
                                function(evaluationPoint + 2.0 * step)
                        )
                }

                evaluationPoint = start - 2.0 * step
                for (index in 0 until intervalCount) {
                    evaluationPoint += 4.0 * step
                    result += 12.0 * function(evaluationPoint)
                }
                result *= 2.0 * step / 45.0
            }
        }
        return GMResult.Ok(result)
    }

    // JSXGraph: src/math/numerics.js -> Romberg
    @Suppress("FunctionName")
    fun Romberg(
        interval: DoubleArray,
        function: (Double) -> Double,
        config: RombergConfig = RombergConfig(),
    ): GMResult<Double, NumericsError> {
        if (interval.size < 2) {
            return GMResult.Err(NumericsError.InvalidInterval(interval.size))
        }

        val start = interval[0]
        val end = interval[1]
        var step = end - start
        var nodeCount = 1
        val estimates = DoubleArray(maxOf(config.maxIterations + 1, 1))
        estimates[0] = 0.5 * step * (function(start) + function(end))
        var integral = 0.0
        var last = Double.POSITIVE_INFINITY

        for (iteration in 0 until config.maxIterations) {
            var sum = 0.0
            step *= 0.5
            nodeCount *= 2
            var factor = 1.0
            var index = 1
            while (index < nodeCount) {
                sum += function(start + index * step)
                index += 2
            }

            estimates[iteration + 1] = 0.5 * estimates[iteration] + sum * step
            integral = estimates[iteration + 1]
            for (level in iteration - 1 downTo 0) {
                factor *= 4.0
                estimates[level] =
                    estimates[level + 1] +
                    (estimates[level + 1] - estimates[level]) / (factor - 1.0)
                integral = estimates[level]
            }

            if (abs(integral - last) < config.epsilon * abs(integral)) {
                break
            }
            last = integral
        }
        return GMResult.Ok(integral)
    }

    // JSXGraph: src/math/numerics.js -> GaussLegendre
    @Suppress("FunctionName")
    fun GaussLegendre(
        interval: DoubleArray,
        function: (Double) -> Double,
        requestedOrder: Int = 12,
    ): GMResult<Double, NumericsError> {
        if (interval.size < 2) {
            return GMResult.Err(NumericsError.InvalidInterval(interval.size))
        }

        val order = requestedOrder.coerceAtMost(18)
        val rule = legendreRule(order)
            ?: return GMResult.Err(NumericsError.InvalidQuadratureOrder(requestedOrder))
        val start = interval[0]
        val end = interval[1]
        val halfWidth = 0.5 * (end - start)
        val midpoint = 0.5 * (end + start)
        val halfOrder = (order + 1) shr 1

        var result = 0.0
        if (order and 1 == 1) {
            result = rule.weights[0] * function(midpoint)
            for (index in 1 until halfOrder) {
                result +=
                    rule.weights[index] *
                    (
                        function(midpoint + halfWidth * rule.nodes[index]) +
                            function(midpoint - halfWidth * rule.nodes[index])
                    )
            }
        } else {
            for (index in 0 until halfOrder) {
                result +=
                    rule.weights[index] *
                    (
                        function(midpoint + halfWidth * rule.nodes[index]) +
                            function(midpoint - halfWidth * rule.nodes[index])
                    )
            }
        }
        return GMResult.Ok(halfWidth * result)
    }

    // JSXGraph: src/math/numerics.js -> splineDef
    fun splineDef(
        knots: DoubleArray,
        values: DoubleArray,
    ): GMResult<DoubleArray, NumericsError> {
        val size = minOf(knots.size, values.size)
        if (size < 2) {
            return GMResult.Err(
                NumericsError.InvalidSplineDefinition(
                    knotCount = knots.size,
                    valueCount = values.size,
                ),
            )
        }
        if (size == 2) {
            return GMResult.Ok(doubleArrayOf(0.0, 0.0))
        }

        val sortedData = MutableList(size) { index -> knots[index] to values[index] }
        sortedData.sortWith { first, second ->
            when {
                first.first < second.first -> -1
                first.first > second.first -> 1
                else -> 0
            }
        }
        for (index in 0 until size) {
            knots[index] = sortedData[index].first
            values[index] = sortedData[index].second
        }

        val distances = DoubleArray(size - 1)
        for (index in distances.indices) {
            distances[index] = knots[index + 1] - knots[index]
        }
        val deltas = DoubleArray(size - 2)
        for (index in deltas.indices) {
            deltas[index] =
                6.0 * (values[index + 2] - values[index + 1]) / distances[index + 1] -
                6.0 * (values[index + 1] - values[index]) / distances[index]
        }

        val diagonal = DoubleArray(size - 2)
        val forwardSolution = DoubleArray(size - 2)
        diagonal[0] = 2.0 * (distances[0] + distances[1])
        forwardSolution[0] = deltas[0]
        for (index in 0 until size - 3) {
            val factor = distances[index + 1] / diagonal[index]
            diagonal[index + 1] =
                2.0 * (distances[index + 1] + distances[index + 2]) -
                factor * distances[index + 1]
            forwardSolution[index + 1] =
                deltas[index + 1] - factor * forwardSolution[index]
        }

        val secondDerivatives = DoubleArray(size)
        secondDerivatives[size - 3] =
            forwardSolution[size - 3] / diagonal[size - 3]
        for (index in size - 4 downTo 0) {
            secondDerivatives[index] =
                (
                    forwardSolution[index] -
                        distances[index + 1] * secondDerivatives[index + 1]
                ) / diagonal[index]
        }
        for (index in size - 3 downTo 0) {
            secondDerivatives[index + 1] = secondDerivatives[index]
        }
        secondDerivatives[0] = 0.0
        secondDerivatives[size - 1] = 0.0
        return GMResult.Ok(secondDerivatives)
    }

    // JSXGraph: src/math/numerics.js -> splineEval
    fun splineEval(
        value: Double,
        knots: DoubleArray,
        values: DoubleArray,
        secondDerivatives: DoubleArray,
    ): GMResult<Double, NumericsError> {
        val size = minOf(knots.size, values.size)
        val validationError = validateSplineEvaluation(
            size = size,
            knotCount = knots.size,
            valueCount = values.size,
            secondDerivativeCount = secondDerivatives.size,
        )
        if (validationError != null) {
            return GMResult.Err(validationError)
        }
        if (value < knots[0] || value > knots[size - 1]) {
            return GMResult.Err(
                NumericsError.SplineValueOutOfDomain(
                    index = 0,
                    value = value,
                    start = knots[0],
                    end = knots[size - 1],
                ),
            )
        }
        return GMResult.Ok(
            evaluateSpline(
                value = value,
                knots = knots,
                values = values,
                secondDerivatives = secondDerivatives,
                size = size,
            ),
        )
    }

    // JSXGraph: src/math/numerics.js -> splineEval
    fun splineEval(
        evaluationPoints: DoubleArray,
        knots: DoubleArray,
        values: DoubleArray,
        secondDerivatives: DoubleArray,
    ): GMResult<DoubleArray, NumericsError> {
        val size = minOf(knots.size, values.size)
        val validationError = validateSplineEvaluation(
            size = size,
            knotCount = knots.size,
            valueCount = values.size,
            secondDerivativeCount = secondDerivatives.size,
        )
        if (validationError != null) {
            return GMResult.Err(validationError)
        }

        val result = DoubleArray(evaluationPoints.size)
        for (index in evaluationPoints.indices) {
            val value = evaluationPoints[index]
            if (value < knots[0] || value > knots[size - 1]) {
                return GMResult.Err(
                    NumericsError.SplineValueOutOfDomain(
                        index = index,
                        value = value,
                        start = knots[0],
                        end = knots[size - 1],
                    ),
                )
            }
            result[index] = evaluateSpline(
                value = value,
                knots = knots,
                values = values,
                secondDerivatives = secondDerivatives,
                size = size,
            )
        }
        return GMResult.Ok(result)
    }

    // JSXGraph: src/math/numerics.js -> generalizedDampedNewton
    fun generalizedDampedNewton(
        function: (DoubleArray, Int) -> DoubleArray,
        jacobian: (DoubleArray, Int) -> Array<DoubleArray>,
        dimension: Int,
        initialValues: DoubleArray,
        damping: Double,
        epsilon: Double,
        maxSteps: Int = 40,
    ): GMResult<DampedNewtonResult, NumericsError> {
        if (dimension <= 0 || initialValues.size < dimension) {
            return GMResult.Err(
                NumericsError.InvalidSystemDimension(
                    dimension = dimension,
                    initialValueCount = initialValues.size,
                ),
            )
        }

        val stepLimit = if (maxSteps == 0) 40 else maxSteps
        val parameters = initialValues.copyOf(dimension)
        var functionValues = function(parameters, dimension)
        if (functionValues.size < dimension) {
            return GMResult.Err(
                NumericsError.InvalidFunctionResult(
                    expectedCount = dimension,
                    actualCount = functionValues.size,
                ),
            )
        }

        functionValues = function(parameters, dimension)
        if (functionValues.size < dimension) {
            return GMResult.Err(
                NumericsError.InvalidFunctionResult(
                    expectedCount = dimension,
                    actualCount = functionValues.size,
                ),
            )
        }

        var squaredResidual = Mat.innerProduct(functionValues, functionValues, dimension)
        var iteration = 0
        if (dimension == 2) {
            var firstValue = functionValues[0]
            var secondValue = functionValues[1]
            squaredResidual =
                firstValue * firstValue + secondValue * secondValue
            while (squaredResidual > epsilon && iteration < stepLimit) {
                val derivative = jacobian(parameters, dimension)
                val jacobianError = validateJacobian(derivative, dimension)
                if (jacobianError != null) {
                    return GMResult.Err(jacobianError)
                }

                val firstFirst = derivative[0][0]
                val firstSecond = derivative[0][1]
                val secondFirst = derivative[1][0]
                val secondSecond = derivative[1][1]
                val determinant =
                    firstFirst * secondSecond - firstSecond * secondFirst
                if (abs(determinant) <= Mat.eps * Mat.eps) {
                    return GMResult.Err(NumericsError.SingularMatrix)
                }

                parameters[0] -=
                    damping *
                    (secondSecond * firstValue - firstSecond * secondValue) /
                    determinant
                parameters[1] -=
                    damping *
                    (firstFirst * secondValue - secondFirst * firstValue) /
                    determinant

                functionValues = function(parameters, dimension)
                if (functionValues.size < dimension) {
                    return GMResult.Err(
                        NumericsError.InvalidFunctionResult(
                            expectedCount = dimension,
                            actualCount = functionValues.size,
                        ),
                    )
                }
                firstValue = functionValues[0]
                secondValue = functionValues[1]
                squaredResidual =
                    firstValue * firstValue + secondValue * secondValue
                iteration += 1
            }
        } else {
            while (squaredResidual > epsilon && iteration < stepLimit) {
                val derivative = jacobian(parameters, dimension)
                val jacobianError = validateJacobian(derivative, dimension)
                if (jacobianError != null) {
                    return GMResult.Err(jacobianError)
                }
                val inverse = Mat.inverse(derivative)
                if (inverse.isEmpty()) {
                    return GMResult.Err(NumericsError.SingularMatrix)
                }
                val step = Mat.matVecMult(inverse, functionValues)
                for (index in 0 until dimension) {
                    parameters[index] -= damping * step[index]
                }

                functionValues = function(parameters, dimension)
                if (functionValues.size < dimension) {
                    return GMResult.Err(
                        NumericsError.InvalidFunctionResult(
                            expectedCount = dimension,
                            actualCount = functionValues.size,
                        ),
                    )
                }
                squaredResidual =
                    Mat.innerProduct(functionValues, functionValues, dimension)
                iteration += 1
            }
        }

        return GMResult.Ok(
            DampedNewtonResult(
                parameters = parameters,
                squaredResidual = squaredResidual,
            ),
        )
    }

    // JSXGraph: src/math/numerics.js -> D
    @Suppress("FunctionName")
    fun D(function: (Double) -> Double): (Double) -> Double = { value ->
        val step = 0.00001
        (function(value + step) - function(value - step)) / (2.0 * step)
    }

    // JSXGraph: src/math/numerics.js -> Newton
    @Suppress("FunctionName")
    fun Newton(
        function: (Double) -> Double,
        initialValue: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        var value = initialValue
        var functionValue = function(value)
        var iteration = 0
        while (iteration < 50 && abs(functionValue) > Mat.eps) {
            val derivative = D(function)(value)
            if (abs(derivative) > Mat.eps) {
                value -= functionValue / derivative
            } else {
                value += random.nextDouble() * 0.2 - 1.0
            }
            functionValue = function(value)
            iteration += 1
        }
        return value
    }

    // JSXGraph: src/math/numerics.js -> findBracket
    fun findBracket(
        function: (Double) -> Double,
        initialValue: Double,
    ): DoubleArray {
        var start = initialValue
        var startValue = function(start)
        val scale = if (start == 0.0) 1.0 else start
        val candidates = doubleArrayOf(
            start - 0.1 * scale,
            start + 0.1 * scale,
            start - 1.0,
            start + 1.0,
            start - 0.5 * scale,
            start + 0.5 * scale,
            start - 0.6 * scale,
            start + 0.6 * scale,
            start - scale,
            start + scale,
            start - 2.0 * scale,
            start + 2.0 * scale,
            start - 5.0 * scale,
            start + 5.0 * scale,
            start - 10.0 * scale,
            start + 10.0 * scale,
            start - 50.0 * scale,
            start + 50.0 * scale,
            start - 100.0 * scale,
            start + 100.0 * scale,
        )

        var end = candidates[0]
        var endValue = Double.NaN
        for (candidate in candidates) {
            end = candidate
            endValue = function(end)
            if (startValue * endValue <= 0.0) {
                break
            }
        }

        if (end < start) {
            val coordinate = start
            start = end
            end = coordinate

            val value = startValue
            startValue = endValue
            endValue = value
        }
        return doubleArrayOf(start, startValue, end, endValue)
    }

    // JSXGraph: src/math/numerics.js -> fzero
    fun fzero(
        function: (Double) -> Double,
        initialValue: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        val bracket = findBracket(function, initialValue)
        if (bracket[1] * bracket[3] > 0.0) {
            return Newton(function, bracket[0], random)
        }
        return fzeroBracketed(
            function = function,
            initialStart = bracket[0],
            initialStartValue = bracket[1],
            initialEnd = bracket[2],
            initialEndValue = bracket[3],
        )
    }

    // JSXGraph: src/math/numerics.js -> fzero
    fun fzero(
        function: (Double) -> Double,
        interval: DoubleArray,
    ): GMResult<Double, NumericsError> {
        val domain = when (val result = findDomain(function, interval)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val startValue = function(domain[0])
        val endValue = function(domain[1])
        if (startValue * endValue > 0.0) {
            return fminbr(function, domain)
        }
        return GMResult.Ok(
            fzeroBracketed(
                function = function,
                initialStart = domain[0],
                initialStartValue = startValue,
                initialEnd = domain[1],
                initialEndValue = endValue,
            ),
        )
    }

    // JSXGraph: src/math/numerics.js -> chandrupatla
    fun chandrupatla(
        function: (Double) -> Double,
        initialValue: Double,
        random: RandomSource = defaultRandomSource,
    ): Double {
        val randomScale = 1.0 + random.nextDouble() * 0.001
        val bracket = findBracket(function, initialValue)
        if (bracket[1] * bracket[3] > 0.0) {
            return Newton(function, bracket[0], random)
        }
        return chandrupatlaBracketed(
            function = function,
            start = bracket[0],
            startValue = bracket[1],
            end = bracket[2],
            endValue = bracket[3],
            randomScale = randomScale,
        )
    }

    // JSXGraph: src/math/numerics.js -> chandrupatla
    fun chandrupatla(
        function: (Double) -> Double,
        interval: DoubleArray,
        random: RandomSource = defaultRandomSource,
    ): GMResult<Double, NumericsError> {
        if (interval.size < 2) {
            return GMResult.Err(NumericsError.InvalidInterval(interval.size))
        }

        val randomScale = 1.0 + random.nextDouble() * 0.001
        val startValue = function(interval[0])
        val endValue = function(interval[1])
        if (startValue * endValue > 0.0) {
            return fminbr(function, interval)
        }
        return GMResult.Ok(
            chandrupatlaBracketed(
                function = function,
                start = interval[0],
                startValue = startValue,
                end = interval[1],
                endValue = endValue,
                randomScale = randomScale,
            ),
        )
    }

    // JSXGraph: src/math/numerics.js -> root
    fun root(
        function: (Double) -> Double,
        initialValue: Double,
        random: RandomSource = defaultRandomSource,
    ): Double = chandrupatla(function, initialValue, random)

    // JSXGraph: src/math/numerics.js -> root
    fun root(
        function: (Double) -> Double,
        interval: DoubleArray,
        random: RandomSource = defaultRandomSource,
    ): GMResult<Double, NumericsError> = chandrupatla(function, interval, random)

    // JSXGraph: src/math/numerics.js -> findDomain
    fun findDomain(
        function: (Double) -> Double,
        interval: DoubleArray,
        outer: Boolean = true,
    ): GMResult<DoubleArray, NumericsError> {
        if (interval.size < 2) {
            return GMResult.Err(NumericsError.InvalidInterval(interval.size))
        }

        val result = interval.copyOf()
        val goldenRemainder = 1.0 - 1.0 / 1.61803398875
        val epsilon = 0.001
        val maxIterations = 20

        var start = result[0]
        var end = result[1]
        var functionValue = function(start)
        if (functionValue.isNaN()) {
            var iteration = 0
            while (end - start > epsilon && iteration < maxIterations) {
                val candidate = (end - start) * goldenRemainder + start
                functionValue = function(candidate)
                if (functionValue.isNaN()) {
                    start = candidate
                } else {
                    end = candidate
                }
                iteration += 1
            }
            result[0] = if (outer) start else end
        }

        start = result[0]
        end = result[1]
        functionValue = function(end)
        if (functionValue.isNaN()) {
            var iteration = 0
            while (end - start > epsilon && iteration < maxIterations) {
                val candidate = end - (end - start) * goldenRemainder
                functionValue = function(candidate)
                if (functionValue.isNaN()) {
                    end = candidate
                } else {
                    start = candidate
                }
                iteration += 1
            }
            result[1] = if (outer) end else start
        }
        return GMResult.Ok(result)
    }

    // JSXGraph: src/math/numerics.js -> fminbr
    fun fminbr(
        function: (Double) -> Double,
        interval: DoubleArray,
    ): GMResult<Double, NumericsError> {
        val domain = when (val result = findDomain(function, interval)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        var start = domain[0]
        var end = domain[1]
        val goldenSectionRatio = (3.0 - sqrt(5.0)) * 0.5
        val tolerance = Mat.eps
        val squareRootEpsilon = Mat.eps
        var previousBest = start + goldenSectionRatio * (end - start)
        var previousBestValue = function(previousBest)
        var best = previousBest
        var olderBest = previousBest
        var bestValue = previousBestValue
        var olderBestValue = previousBestValue
        var iteration = 0

        while (iteration < maxIterationsMinimize) {
            val range = end - start
            val middle = (start + end) * 0.5
            val actualTolerance =
                squareRootEpsilon * abs(best) + tolerance / 3.0
            if (abs(best - middle) + range * 0.5 <= 2.0 * actualTolerance) {
                return GMResult.Ok(best)
            }

            var newStep = goldenSectionRatio * if (best < middle) end - best else start - best
            if (abs(best - previousBest) >= actualTolerance) {
                val firstProduct = (best - previousBest) * (bestValue - olderBestValue)
                var denominator = (best - olderBest) * (bestValue - previousBestValue)
                var numerator =
                    (best - olderBest) * denominator -
                        (best - previousBest) * firstProduct
                denominator = 2.0 * (denominator - firstProduct)

                if (denominator > 0.0) {
                    numerator = -numerator
                } else {
                    denominator = -denominator
                }
                if (
                    abs(numerator) < abs(newStep * denominator) &&
                    numerator > denominator * (start - best + 2.0 * actualTolerance) &&
                    numerator < denominator * (end - best - 2.0 * actualTolerance)
                ) {
                    newStep = numerator / denominator
                }
            }

            if (abs(newStep) < actualTolerance) {
                newStep = if (newStep > 0.0) actualTolerance else -actualTolerance
            }

            val candidate = best + newStep
            val candidateValue = function(candidate)
            if (candidateValue <= bestValue) {
                if (candidate < best) {
                    end = best
                } else {
                    start = best
                }

                olderBest = previousBest
                previousBest = best
                best = candidate
                olderBestValue = previousBestValue
                previousBestValue = bestValue
                bestValue = candidateValue
            } else {
                if (candidate < best) {
                    start = candidate
                } else {
                    end = candidate
                }

                if (candidateValue <= previousBestValue || previousBest == best) {
                    olderBest = previousBest
                    previousBest = candidate
                    olderBestValue = previousBestValue
                    previousBestValue = candidateValue
                } else if (
                    candidateValue <= olderBestValue ||
                    olderBest == best ||
                    olderBest == previousBest
                ) {
                    olderBest = candidate
                    olderBestValue = candidateValue
                }
            }
            iteration += 1
        }
        return GMResult.Ok(best)
    }

    private fun fzeroBracketed(
        function: (Double) -> Double,
        initialStart: Double,
        initialStartValue: Double,
        initialEnd: Double,
        initialEndValue: Double,
    ): Double {
        var start = initialStart
        var end = initialEnd
        var startValue = initialStartValue
        var endValue = initialEndValue
        if (abs(startValue) <= Mat.eps) {
            return start
        }
        if (abs(endValue) <= Mat.eps) {
            return end
        }

        var opposite = start
        var oppositeValue = startValue
        var iteration = 0
        while (iteration < maxIterationsRoot) {
            val previousStep = end - start
            if (abs(oppositeValue) < abs(endValue)) {
                start = end
                end = opposite
                opposite = start

                startValue = endValue
                endValue = oppositeValue
                oppositeValue = startValue
            }

            val actualTolerance = 2.0 * Mat.eps * abs(end) + Mat.eps * 0.5
            var newStep = (opposite - end) * 0.5
            if (abs(newStep) <= actualTolerance || abs(endValue) <= Mat.eps) {
                return end
            }

            if (abs(previousStep) >= actualTolerance && abs(startValue) > abs(endValue)) {
                val oppositeDistance = opposite - end
                var numerator: Double
                var denominator: Double
                if (start == opposite) {
                    val ratio = endValue / startValue
                    numerator = oppositeDistance * ratio
                    denominator = 1.0 - ratio
                } else {
                    denominator = startValue / oppositeValue
                    val oppositeRatio = endValue / oppositeValue
                    val startRatio = endValue / startValue
                    numerator =
                        startRatio *
                        (
                            oppositeDistance * denominator * (denominator - oppositeRatio) -
                                (end - start) * (oppositeRatio - 1.0)
                        )
                    denominator =
                        (denominator - 1.0) *
                        (oppositeRatio - 1.0) *
                        (startRatio - 1.0)
                }

                if (numerator > 0.0) {
                    denominator = -denominator
                } else {
                    numerator = -numerator
                }
                if (
                    numerator <
                    0.75 * oppositeDistance * denominator -
                    abs(actualTolerance * denominator) * 0.5 &&
                    numerator < abs(previousStep * denominator * 0.5)
                ) {
                    newStep = numerator / denominator
                }
            }

            if (abs(newStep) < actualTolerance) {
                newStep = if (newStep > 0.0) actualTolerance else -actualTolerance
            }

            start = end
            startValue = endValue
            end += newStep
            endValue = function(end)
            if (
                (endValue > 0.0 && oppositeValue > 0.0) ||
                (endValue < 0.0 && oppositeValue < 0.0)
            ) {
                opposite = start
                oppositeValue = startValue
            }
            iteration += 1
        }
        return end
    }

    private fun chandrupatlaBracketed(
        function: (Double) -> Double,
        start: Double,
        startValue: Double,
        end: Double,
        endValue: Double,
        randomScale: Double,
    ): Double {
        var firstPoint = start
        var secondPoint = end
        var firstValue = startValue
        var secondValue = endValue
        var discardedPoint = Double.NaN
        var discardedValue = Double.NaN
        var interpolationFraction = 0.5 * randomScale
        var bestPoint = firstPoint
        var iteration = 0

        do {
            val candidate =
                firstPoint + interpolationFraction * (secondPoint - firstPoint)
            val candidateValue = function(candidate)

            if (sign(candidateValue) == sign(firstValue)) {
                discardedPoint = firstPoint
                firstPoint = candidate
                discardedValue = firstValue
                firstValue = candidateValue
            } else {
                discardedPoint = secondPoint
                secondPoint = firstPoint
                discardedValue = secondValue
                secondValue = firstValue
            }
            firstPoint = candidate
            firstValue = candidateValue

            bestPoint = firstPoint
            var bestValue = firstValue
            if (abs(secondValue) < abs(firstValue)) {
                bestPoint = secondPoint
                bestValue = secondValue
            }
            val tolerance = 2.0 * Mat.eps * abs(bestPoint) + 0.5 * 0.00001
            val relativeTolerance = tolerance / abs(secondPoint - firstPoint)
            if (relativeTolerance > 0.5 || bestValue == 0.0) {
                break
            }

            val pointRatio =
                (firstPoint - secondPoint) / (discardedPoint - secondPoint)
            val valueRatio =
                (firstValue - secondValue) / (discardedValue - secondValue)
            val lowerBound = 1.0 - sqrt(1.0 - pointRatio)
            val upperBound = sqrt(pointRatio)
            interpolationFraction = if (lowerBound < valueRatio && valueRatio < upperBound) {
                val discardedRatio =
                    (discardedPoint - firstPoint) / (secondPoint - firstPoint)
                val firstToSecond = firstValue / (secondValue - firstValue)
                val discardedToSecond =
                    discardedValue / (secondValue - discardedValue)
                val firstToDiscarded = firstValue / (discardedValue - firstValue)
                val secondToDiscarded =
                    secondValue / (discardedValue - secondValue)
                firstToSecond * discardedToSecond +
                    firstToDiscarded * secondToDiscarded * discardedRatio
            } else {
                0.5 * randomScale
            }
            if (interpolationFraction < relativeTolerance) {
                interpolationFraction = relativeTolerance
            }
            if (interpolationFraction > 1.0 - relativeTolerance) {
                interpolationFraction = 1.0 - relativeTolerance
            }
            iteration += 1
        } while (iteration <= maxIterationsRoot)
        return bestPoint
    }

    private fun validateJacobian(
        jacobian: Array<DoubleArray>,
        dimension: Int,
    ): NumericsError.InvalidJacobian? {
        val shortestRowSize = jacobian.minOfOrNull { it.size } ?: 0
        return if (jacobian.size != dimension || shortestRowSize < dimension) {
            NumericsError.InvalidJacobian(
                expectedDimension = dimension,
                rowCount = jacobian.size,
                shortestRowSize = shortestRowSize,
            )
        } else {
            null
        }
    }

    private fun validateSplineEvaluation(
        size: Int,
        knotCount: Int,
        valueCount: Int,
        secondDerivativeCount: Int,
    ): NumericsError.InvalidSplineEvaluation? =
        if (size < 2 || secondDerivativeCount < size) {
            NumericsError.InvalidSplineEvaluation(
                knotCount = knotCount,
                valueCount = valueCount,
                secondDerivativeCount = secondDerivativeCount,
            )
        } else {
            null
        }

    private fun evaluateSpline(
        value: Double,
        knots: DoubleArray,
        values: DoubleArray,
        secondDerivatives: DoubleArray,
        size: Int,
    ): Double {
        var intervalIndex = 1
        while (intervalIndex < size && value > knots[intervalIndex]) {
            intervalIndex += 1
        }
        intervalIndex -= 1

        val intervalWidth = knots[intervalIndex + 1] - knots[intervalIndex]
        val constant = values[intervalIndex]
        val linear =
            (values[intervalIndex + 1] - values[intervalIndex]) / intervalWidth -
                intervalWidth / 6.0 *
                (
                    secondDerivatives[intervalIndex + 1] +
                        2.0 * secondDerivatives[intervalIndex]
                )
        val quadratic = secondDerivatives[intervalIndex] / 2.0
        val cubic =
            (
                secondDerivatives[intervalIndex + 1] -
                    secondDerivatives[intervalIndex]
            ) / (6.0 * intervalWidth)
        val offset = value - knots[intervalIndex]
        return constant + (linear + (quadratic + cubic * offset) * offset) * offset
    }

    private fun legendreRule(order: Int): LegendreRule? = when (order) {
        2 -> LegendreRule(
            doubleArrayOf(0.5773502691896257645091488),
            doubleArrayOf(1.0),
        )

        3 -> LegendreRule(
            doubleArrayOf(0.0, 0.7745966692414833770358531),
            doubleArrayOf(0.8888888888888888888888889, 0.5555555555555555555555556),
        )

        4 -> LegendreRule(
            doubleArrayOf(0.3399810435848562648026658, 0.8611363115940525752239465),
            doubleArrayOf(0.6521451548625461426269361, 0.3478548451374538573730639),
        )

        5 -> LegendreRule(
            doubleArrayOf(0.0, 0.5384693101056830910363144, 0.9061798459386639927976269),
            doubleArrayOf(
                0.5688888888888888888888889,
                0.4786286704993664680412915,
                0.236926885056189087514264,
            ),
        )

        6 -> LegendreRule(
            doubleArrayOf(
                0.2386191860831969086305017,
                0.6612093864662645136613996,
                0.9324695142031520278123016,
            ),
            doubleArrayOf(
                0.4679139345726910473898703,
                0.3607615730481386075698335,
                0.1713244923791703450402961,
            ),
        )

        7 -> LegendreRule(
            doubleArrayOf(
                0.0,
                0.4058451513773971669066064,
                0.7415311855993944398638648,
                0.9491079123427585245261897,
            ),
            doubleArrayOf(
                0.417959183673469387755102,
                0.3818300505051189449503698,
                0.2797053914892766679014678,
                0.1294849661688696932706114,
            ),
        )

        8 -> LegendreRule(
            doubleArrayOf(
                0.1834346424956498049394761,
                0.525532409916328985817739,
                0.7966664774136267395915539,
                0.9602898564975362316835609,
            ),
            doubleArrayOf(
                0.3626837833783619829651504,
                0.3137066458778872873379622,
                0.222381034453374470544356,
                0.1012285362903762591525314,
            ),
        )

        9 -> LegendreRule(
            doubleArrayOf(
                0.0,
                0.324253423403808929038538,
                0.613371432700590397308702,
                0.8360311073266357942994298,
                0.9681602395076260898355762,
            ),
            doubleArrayOf(
                0.3302393550012597631645251,
                0.3123470770400028400686304,
                0.2606106964029354623187429,
                0.180648160694857404058472,
                0.0812743883615744119718922,
            ),
        )

        10 -> LegendreRule(
            doubleArrayOf(
                0.148874338981631210884826,
                0.4333953941292471907992659,
                0.6794095682990244062343274,
                0.8650633666889845107320967,
                0.973906528517171720077964,
            ),
            doubleArrayOf(
                0.295524224714752870173893,
                0.2692667193099963550912269,
                0.2190863625159820439955349,
                0.1494513491505805931457763,
                0.0666713443086881375935688,
            ),
        )

        11 -> LegendreRule(
            doubleArrayOf(
                0.0,
                0.269543155952344972331532,
                0.5190961292068118159257257,
                0.7301520055740493240934163,
                0.8870625997680952990751578,
                0.978228658146056992803938,
            ),
            doubleArrayOf(
                0.2729250867779006307144835,
                0.2628045445102466621806889,
                0.2331937645919904799185237,
                0.1862902109277342514260976,
                0.1255803694649046246346943,
                0.0556685671161736664827537,
            ),
        )

        12 -> LegendreRule(
            doubleArrayOf(
                0.1252334085114689154724414,
                0.3678314989981801937526915,
                0.5873179542866174472967024,
                0.7699026741943046870368938,
                0.9041172563704748566784659,
                0.9815606342467192506905491,
            ),
            doubleArrayOf(
                0.2491470458134027850005624,
                0.2334925365383548087608499,
                0.2031674267230659217490645,
                0.1600783285433462263346525,
                0.1069393259953184309602547,
                0.047175336386511827194616,
            ),
        )

        13 -> LegendreRule(
            doubleArrayOf(
                0.0,
                0.2304583159551347940655281,
                0.4484927510364468528779129,
                0.6423493394403402206439846,
                0.8015780907333099127942065,
                0.9175983992229779652065478,
                0.9841830547185881494728294,
            ),
            doubleArrayOf(
                0.2325515532308739101945895,
                0.2262831802628972384120902,
                0.2078160475368885023125232,
                0.1781459807619457382800467,
                0.1388735102197872384636018,
                0.0921214998377284479144218,
                0.0404840047653158795200216,
            ),
        )

        14 -> LegendreRule(
            doubleArrayOf(
                0.1080549487073436620662447,
                0.3191123689278897604356718,
                0.5152486363581540919652907,
                0.6872929048116854701480198,
                0.8272013150697649931897947,
                0.9284348836635735173363911,
                0.9862838086968123388415973,
            ),
            doubleArrayOf(
                0.2152638534631577901958764,
                0.2051984637212956039659241,
                0.1855383974779378137417166,
                0.1572031671581935345696019,
                0.1215185706879031846894148,
                0.0801580871597602098056333,
                0.0351194603317518630318329,
            ),
        )

        15 -> LegendreRule(
            doubleArrayOf(
                0.0,
                0.2011940939974345223006283,
                0.3941513470775633698972074,
                0.5709721726085388475372267,
                0.7244177313601700474161861,
                0.8482065834104272162006483,
                0.9372733924007059043077589,
                0.9879925180204854284895657,
            ),
            doubleArrayOf(
                0.2025782419255612728806202,
                0.1984314853271115764561183,
                0.1861610000155622110268006,
                0.1662692058169939335532009,
                0.1395706779261543144478048,
                0.1071592204671719350118695,
                0.0703660474881081247092674,
                0.0307532419961172683546284,
            ),
        )

        16 -> LegendreRule(
            doubleArrayOf(
                0.0950125098376374401853193,
                0.2816035507792589132304605,
                0.4580167776572273863424194,
                0.6178762444026437484466718,
                0.7554044083550030338951012,
                0.8656312023878317438804679,
                0.9445750230732325760779884,
                0.9894009349916499325961542,
            ),
            doubleArrayOf(
                0.1894506104550684962853967,
                0.1826034150449235888667637,
                0.1691565193950025381893121,
                0.1495959888165767320815017,
                0.1246289712555338720524763,
                0.0951585116824927848099251,
                0.0622535239386478928628438,
                0.0271524594117540948517806,
            ),
        )

        17 -> LegendreRule(
            doubleArrayOf(
                0.0,
                0.1784841814958478558506775,
                0.3512317634538763152971855,
                0.5126905370864769678862466,
                0.6576711592166907658503022,
                0.7815140038968014069252301,
                0.8802391537269859021229557,
                0.950675521768767761222717,
                0.990575475314417335675434,
            ),
            doubleArrayOf(
                0.1794464703562065254582656,
                0.176562705366992646325271,
                0.1680041021564500445099707,
                0.1540457610768102880814316,
                0.13513636846852547328632,
                0.1118838471934039710947884,
                0.0850361483171791808835354,
                0.0554595293739872011294402,
                0.02414830286854793196011,
            ),
        )

        18 -> LegendreRule(
            doubleArrayOf(
                0.0847750130417353012422619,
                0.2518862256915055095889729,
                0.4117511614628426460359318,
                0.5597708310739475346078715,
                0.6916870430603532078748911,
                0.8037049589725231156824175,
                0.8926024664975557392060606,
                0.9558239495713977551811959,
                0.991565168420930946730016,
            ),
            doubleArrayOf(
                0.1691423829631435918406565,
                0.1642764837458327229860538,
                0.154684675126265244925418,
                0.1406429146706506512047313,
                0.1225552067114784601845191,
                0.100942044106287165562814,
                0.0764257302548890565291297,
                0.0497145488949697964533349,
                0.0216160135264833103133427,
            ),
        )

        else -> null
    }
}
