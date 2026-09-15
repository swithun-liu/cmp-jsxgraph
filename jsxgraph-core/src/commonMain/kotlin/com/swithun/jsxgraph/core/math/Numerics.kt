/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/numerics.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Coords
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
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

    data class InvalidRungeKuttaStepCount(val stepCount: Int) : NumericsError

    data class InvalidButcherTableau(
        val stageCount: Int,
        val coefficientRowCount: Int,
        val invalidCoefficientRow: Int?,
        val weightCount: Int,
        val nodeCount: Int,
    ) : NumericsError

    data class InvalidIntegrationLimit(val limit: Int) : NumericsError

    data class InvalidIntegrationTolerance(
        val epsilonRelative: Double,
        val epsilonAbsolute: Double,
    ) : NumericsError

    data class InvalidInitialRootCount(
        val expectedCount: Int,
        val actualCount: Int,
    ) : NumericsError

    data class InvalidSimplificationTolerance(val tolerance: Double) : NumericsError

    data class InvalidSimplificationPointCount(val pointCount: Int) : NumericsError

    data class InvalidSimplificationTopology(val pointIndex: Int) : NumericsError

    data class InvalidPolynomialDegree(
        val degree: Int,
        val coefficientCount: Int,
    ) : NumericsError

    data class InvalidPolynomialPrecision(val precision: Int) : NumericsError
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

enum class GaussKronrodRule {
    FIFTEEN,
    TWENTY_ONE,
    THIRTY_ONE,
}

data class QagConfig(
    val limit: Int = 15,
    val epsilonRelative: Double = 0.0000001,
    val epsilonAbsolute: Double = 0.0000001,
    val rule: GaussKronrodRule = GaussKronrodRule.FIFTEEN,
)

data class GaussKronrodResult(
    val value: Double,
    val absoluteError: Double,
    val absoluteResult: Double,
    val absoluteDeviation: Double,
)

data class JacobiResult(
    val diagonalizedMatrix: Array<DoubleArray>,
    val eigenvectors: Array<DoubleArray>,
)

data class DampedNewtonResult(
    val parameters: DoubleArray,
    val squaredResidual: Double,
)

enum class RungeKuttaMethod {
    EULER,
    HEUN,
    RK4,
}

data class ButcherTableau(
    val stageCount: Int,
    val coefficients: Array<DoubleArray>,
    val weights: DoubleArray,
    val nodes: DoubleArray,
)

internal class NevilleInterpolation internal constructor(
    private val points: List<CoordsElement>,
) {
    private var weights = DoubleArray(0)

    internal fun x(
        parameter: Double,
        suspendedUpdate: Boolean = false,
    ): Double = evaluate(parameter, suspendedUpdate, CoordsElement::X)

    internal fun y(
        parameter: Double,
        suspendedUpdate: Boolean = false,
    ): Double = evaluate(parameter, suspendedUpdate, CoordsElement::Y)

    internal val start: Double = 0.0

    internal fun end(): Double = (points.size - 1).toDouble()

    private fun evaluate(
        parameter: Double,
        suspendedUpdate: Boolean,
        coordinate: (CoordsElement) -> Double,
    ): Double {
        val size = points.size
        if (!suspendedUpdate) {
            var sign = 1.0
            weights = DoubleArray(size) { index ->
                val weight = Mat.binomial((size - 1).toDouble(), index.toDouble()) * sign
                sign *= -1.0
                weight
            }
        }

        var difference = parameter
        var numerator = 0.0
        var denominator = 0.0
        for (index in 0 until size) {
            if (difference == 0.0) {
                return coordinate(points[index])
            }

            val weight = weights.getOrElse(index) { Double.NaN } / difference
            difference -= 1.0
            numerator += coordinate(points[index]) * weight
            denominator += weight
        }
        return numerator / denominator
    }
}

internal class LagrangePolynomial internal constructor(
    private val points: List<CoordsElement>,
) {
    private var weights = DoubleArray(0)

    internal operator fun invoke(
        value: Double,
        suspendedUpdate: Boolean = false,
    ): Double {
        val size = points.size
        if (!suspendedUpdate) {
            weights = DoubleArray(size)
            for (index in 0 until size) {
                val x = points[index].X()
                var weight = 1.0
                for (otherIndex in 0 until size) {
                    if (otherIndex != index) {
                        weight *= x - points[otherIndex].X()
                    }
                }
                weights[index] = 1.0 / weight
            }
        }

        var numerator = 0.0
        var denominator = 0.0
        for (index in 0 until size) {
            val x = points[index].X()
            if (value == x) {
                return points[index].Y()
            }

            val weight = weights.getOrElse(index) { Double.NaN } / (value - x)
            denominator += weight
            numerator += weight * points[index].Y()
        }
        return numerator / denominator
    }

    internal fun getTerm(
        digits: Int? = null,
        parameter: String? = null,
        multiplicationSymbol: String? = null,
    ): String = Numerics.lagrangePolynomialTerm(
        points = points,
        digits = digits,
        parameter = parameter,
        multiplicationSymbol = multiplicationSymbol,
    )()

    internal fun getCoefficients(): DoubleArray =
        Numerics.lagrangePolynomialCoefficients(points)()
}

object Numerics {
    private data class PolylineSplit(
        val distance: Double,
        val index: Int,
    )

    private data class VisvalingamNode(
        var volume: Double,
        val index: Int,
    )

    private data class VisvalingamLink(
        var left: Int?,
        var right: Int? = null,
        val node: VisvalingamNode?,
    )

    private data class LegendreRule(
        val nodes: DoubleArray,
        val weights: DoubleArray,
    )

    private data class IntegrationSubinterval(
        val start: Double,
        val end: Double,
        val result: Double,
        val error: Double,
    )

    private class IntegrationWorkspace(
        interval: DoubleArray,
        private val limit: Int,
    ) {
        private var size = 0
        private var maximumErrorPosition = 0
        private var currentIndex = 0
        private val starts = DoubleArray(limit)
        private val ends = DoubleArray(limit)
        private val results = DoubleArray(limit)
        private val errors = DoubleArray(limit)
        private val order = IntArray(limit)
        private val levels = IntArray(limit)

        init {
            starts[0] = interval[0]
            ends[0] = interval[1]
        }

        fun setInitialResult(
            result: Double,
            error: Double,
        ) {
            size = 1
            results[0] = result
            errors[0] = error
        }

        fun update(
            firstStart: Double,
            firstEnd: Double,
            firstResult: Double,
            firstError: Double,
            secondStart: Double,
            secondEnd: Double,
            secondResult: Double,
            secondError: Double,
        ) {
            val maximumErrorIndex = currentIndex
            val newIndex = size
            val newLevel = levels[currentIndex] + 1
            if (secondError > firstError) {
                starts[maximumErrorIndex] = secondStart
                results[maximumErrorIndex] = secondResult
                errors[maximumErrorIndex] = secondError
                levels[maximumErrorIndex] = newLevel

                starts[newIndex] = firstStart
                ends[newIndex] = firstEnd
                results[newIndex] = firstResult
                errors[newIndex] = firstError
                levels[newIndex] = newLevel
            } else {
                ends[maximumErrorIndex] = firstEnd
                results[maximumErrorIndex] = firstResult
                errors[maximumErrorIndex] = firstError
                levels[maximumErrorIndex] = newLevel

                starts[newIndex] = secondStart
                ends[newIndex] = secondEnd
                results[newIndex] = secondResult
                errors[newIndex] = secondError
                levels[newIndex] = newLevel
            }
            size += 1
            sortErrors()
        }

        fun retrieve(): IntegrationSubinterval {
            val index = currentIndex
            return IntegrationSubinterval(
                start = starts[index],
                end = ends[index],
                result = results[index],
                error = errors[index],
            )
        }

        fun sumResults(): Double {
            var sum = 0.0
            for (index in 0 until size) {
                sum += results[index]
            }
            return sum
        }

        fun subintervalTooSmall(
            firstStart: Double,
            secondStart: Double,
            secondEnd: Double,
        ): Boolean {
            val machineEpsilon = 2.2204460492503131e-16
            val minimumValue = 2.2250738585072014e-308
            val threshold =
                (1.0 + 100.0 * machineEpsilon) *
                    (abs(secondStart) + 1000.0 * minimumValue)
            return abs(firstStart) <= threshold && abs(secondEnd) <= threshold
        }

        private fun sortErrors() {
            val last = size - 1
            var errorPosition = maximumErrorPosition
            var maximumErrorIndex = order[errorPosition]
            if (last < 2) {
                order[0] = 0
                order[1] = 1
                currentIndex = maximumErrorIndex
                return
            }

            val maximumError = errors[maximumErrorIndex]
            while (
                errorPosition > 0 &&
                maximumError > errors[order[errorPosition - 1]]
            ) {
                order[errorPosition] = order[errorPosition - 1]
                errorPosition -= 1
            }

            val top = if (last < limit / 2.0 + 2.0) {
                last
            } else {
                limit - last + 1
            }
            var index = errorPosition + 1
            while (index < top && maximumError < errors[order[index]]) {
                order[index - 1] = order[index]
                index += 1
            }
            order[index - 1] = maximumErrorIndex

            val minimumError = errors[last]
            var reverseIndex = top - 1
            while (
                reverseIndex > index - 2 &&
                minimumError >= errors[order[reverseIndex]]
            ) {
                order[reverseIndex + 1] = order[reverseIndex]
                reverseIndex -= 1
            }
            order[reverseIndex + 1] = last

            maximumErrorIndex = order[errorPosition]
            currentIndex = maximumErrorIndex
            maximumErrorPosition = errorPosition
        }
    }

    private val defaultRandomSource = RandomSource { Random.nextDouble() }

    var maxIterationsRoot: Int = 80
    var maxIterationsMinimize: Int = 500

    val eulerTableau = ButcherTableau(
        stageCount = 1,
        coefficients = arrayOf(doubleArrayOf(0.0)),
        weights = doubleArrayOf(1.0),
        nodes = doubleArrayOf(0.0),
    )
    val heunTableau = ButcherTableau(
        stageCount = 2,
        coefficients = arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(1.0, 0.0),
        ),
        weights = doubleArrayOf(0.5, 0.5),
        nodes = doubleArrayOf(0.0, 1.0),
    )
    val rk4Tableau = ButcherTableau(
        stageCount = 4,
        coefficients = arrayOf(
            doubleArrayOf(0.0, 0.0, 0.0, 0.0),
            doubleArrayOf(0.5, 0.0, 0.0, 0.0),
            doubleArrayOf(0.0, 0.5, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0, 0.0),
        ),
        weights = doubleArrayOf(1.0 / 6.0, 1.0 / 3.0, 1.0 / 3.0, 1.0 / 6.0),
        nodes = doubleArrayOf(0.0, 0.5, 0.5, 1.0),
    )

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

    // JSXGraph: src/math/numerics.js -> GaussKronrod15
    @Suppress("FunctionName")
    fun GaussKronrod15(
        interval: DoubleArray,
        function: (Double) -> Double,
    ): GMResult<GaussKronrodResult, NumericsError> =
        gaussKronrod(
            interval = interval,
            function = function,
            abscissae = doubleArrayOf(
                0.991455371120812639206854697526329,
                0.949107912342758524526189684047851,
                0.864864423359769072789712788640926,
                0.741531185599394439863864773280788,
                0.58608723546769113029414483825873,
                0.405845151377397166906606412076961,
                0.207784955007898467600689403773245,
                0.0,
            ),
            gaussWeights = doubleArrayOf(
                0.129484966168869693270611432679082,
                0.27970539148927666790146777142378,
                0.381830050505118944950369775488975,
                0.417959183673469387755102040816327,
            ),
            kronrodWeights = doubleArrayOf(
                0.02293532201052922496373200805897,
                0.063092092629978553290700663189204,
                0.104790010322250183839876322541518,
                0.140653259715525918745189590510238,
                0.16900472663926790282658342659855,
                0.190350578064785409913256402421014,
                0.204432940075298892414161999234649,
                0.209482141084727828012999174891714,
            ),
        )

    // JSXGraph: src/math/numerics.js -> GaussKronrod21
    @Suppress("FunctionName")
    fun GaussKronrod21(
        interval: DoubleArray,
        function: (Double) -> Double,
    ): GMResult<GaussKronrodResult, NumericsError> =
        gaussKronrod(
            interval = interval,
            function = function,
            abscissae = doubleArrayOf(
                0.995657163025808080735527280689003,
                0.973906528517171720077964012084452,
                0.930157491355708226001207180059508,
                0.865063366688984510732096688423493,
                0.780817726586416897063717578345042,
                0.679409568299024406234327365114874,
                0.562757134668604683339000099272694,
                0.433395394129247190799265943165784,
                0.294392862701460198131126603103866,
                0.14887433898163121088482600112972,
                0.0,
            ),
            gaussWeights = doubleArrayOf(
                0.066671344308688137593568809893332,
                0.149451349150580593145776339657697,
                0.219086362515982043995534934228163,
                0.269266719309996355091226921569469,
                0.295524224714752870173892994651338,
            ),
            kronrodWeights = doubleArrayOf(
                0.011694638867371874278064396062192,
                0.03255816230796472747881897245939,
                0.05475589657435199603138130024458,
                0.07503967481091995276704314091619,
                0.093125454583697605535065465083366,
                0.109387158802297641899210590325805,
                0.123491976262065851077958109831074,
                0.134709217311473325928054001771707,
                0.142775938577060080797094273138717,
                0.147739104901338491374841515972068,
                0.149445554002916905664936468389821,
            ),
        )

    // JSXGraph: src/math/numerics.js -> GaussKronrod31
    @Suppress("FunctionName")
    fun GaussKronrod31(
        interval: DoubleArray,
        function: (Double) -> Double,
    ): GMResult<GaussKronrodResult, NumericsError> =
        gaussKronrod(
            interval = interval,
            function = function,
            abscissae = doubleArrayOf(
                0.998002298693397060285172840152271,
                0.987992518020485428489565718586613,
                0.967739075679139134257347978784337,
                0.937273392400705904307758947710209,
                0.897264532344081900882509656454496,
                0.848206583410427216200648320774217,
                0.790418501442465932967649294817947,
                0.724417731360170047416186054613938,
                0.650996741297416970533735895313275,
                0.570972172608538847537226737253911,
                0.485081863640239680693655740232351,
                0.394151347077563369897207370981045,
                0.299180007153168812166780024266389,
                0.201194093997434522300628303394596,
                0.101142066918717499027074231447392,
                0.0,
            ),
            gaussWeights = doubleArrayOf(
                0.030753241996117268354628393577204,
                0.070366047488108124709267416450667,
                0.107159220467171935011869546685869,
                0.139570677926154314447804794511028,
                0.166269205816993933553200860481209,
                0.186161000015562211026800561866423,
                0.198431485327111576456118326443839,
                0.202578241925561272880620199967519,
            ),
            kronrodWeights = doubleArrayOf(
                0.005377479872923348987792051430128,
                0.015007947329316122538374763075807,
                0.025460847326715320186874001019653,
                0.03534636079137584622203794847836,
                0.04458975132476487660822729937328,
                0.05348152469092808726534314723943,
                0.062009567800670640285139230960803,
                0.069854121318728258709520077099147,
                0.076849680757720378894432777482659,
                0.083080502823133021038289247286104,
                0.088564443056211770647275443693774,
                0.093126598170825321225486872747346,
                0.096642726983623678505179907627589,
                0.099173598721791959332393173484603,
                0.10076984552387559504494666261757,
                0.101330007014791549017374792767493,
            ),
        )

    // JSXGraph: src/math/numerics.js -> Qag
    @Suppress("FunctionName")
    fun Qag(
        interval: DoubleArray,
        function: (Double) -> Double,
        config: QagConfig = QagConfig(),
    ): GMResult<Double, NumericsError> {
        if (interval.size < 2) {
            return GMResult.Err(NumericsError.InvalidInterval(interval.size))
        }
        if (config.limit !in 1..1000) {
            return GMResult.Err(NumericsError.InvalidIntegrationLimit(config.limit))
        }
        if (
            config.epsilonAbsolute <= 0.0 &&
            (
                config.epsilonRelative < 50.0 * Mat.eps ||
                    config.epsilonRelative < 0.5e-28
            )
        ) {
            return GMResult.Err(
                NumericsError.InvalidIntegrationTolerance(
                    epsilonRelative = config.epsilonRelative,
                    epsilonAbsolute = config.epsilonAbsolute,
                ),
            )
        }

        val workspace = IntegrationWorkspace(interval, 1000)
        val initial = when (val result = applyGaussKronrod(config.rule, interval, function)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        workspace.setInitialResult(initial.value, initial.absoluteError)
        var tolerance = max(
            config.epsilonAbsolute,
            config.epsilonRelative * abs(initial.value),
        )
        val roundOff = 50.0 * 2.2204460492503131e-16 * initial.absoluteResult
        if (initial.absoluteError <= roundOff && initial.absoluteError > tolerance) {
            return GMResult.Ok(Double.NEGATIVE_INFINITY)
        }
        if (
            (
                initial.absoluteError <= tolerance &&
                    initial.absoluteError != initial.absoluteDeviation
            ) ||
            initial.absoluteError == 0.0
        ) {
            return GMResult.Ok(initial.value)
        }
        if (config.limit == 1) {
            return GMResult.Ok(Double.NEGATIVE_INFINITY)
        }

        var area = initial.value
        var errorSum = initial.absoluteError
        var iteration = 1
        var roundoffType1 = 0
        var roundoffType2 = 0
        var errorType = 0
        do {
            val largestError = workspace.retrieve()
            val firstStart = largestError.start
            val firstEnd = 0.5 * (largestError.start + largestError.end)
            val secondStart = firstEnd
            val secondEnd = largestError.end

            val first = when (
                val result = applyGaussKronrod(
                    config.rule,
                    doubleArrayOf(firstStart, firstEnd),
                    function,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = applyGaussKronrod(
                    config.rule,
                    doubleArrayOf(secondStart, secondEnd),
                    function,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val combinedArea = first.value + second.value
            val combinedError = first.absoluteError + second.absoluteError
            errorSum += combinedError - largestError.error
            area += combinedArea - largestError.result

            if (
                first.absoluteDeviation != first.absoluteError &&
                second.absoluteDeviation != second.absoluteError
            ) {
                val delta = largestError.result - combinedArea
                if (
                    abs(delta) <= 1.0e-5 * abs(combinedArea) &&
                    combinedError >= 0.99 * largestError.error
                ) {
                    roundoffType1 += 1
                }
                if (iteration >= 10 && combinedError > largestError.error) {
                    roundoffType2 += 1
                }
            }

            tolerance = max(config.epsilonAbsolute, config.epsilonRelative * abs(area))
            if (errorSum > tolerance) {
                if (roundoffType1 >= 6 || roundoffType2 >= 20) {
                    errorType = 2
                }
                if (workspace.subintervalTooSmall(firstStart, secondStart, secondEnd)) {
                    errorType = 3
                }
            }
            workspace.update(
                firstStart = firstStart,
                firstEnd = firstEnd,
                firstResult = first.value,
                firstError = first.absoluteError,
                secondStart = secondStart,
                secondEnd = secondEnd,
                secondResult = second.value,
                secondError = second.absoluteError,
            )
            iteration += 1
        } while (
            iteration < config.limit &&
            errorType == 0 &&
            errorSum > tolerance
        )
        return GMResult.Ok(workspace.sumResults())
    }

    // JSXGraph: src/math/numerics.js -> I
    @Suppress("FunctionName")
    fun I(
        interval: DoubleArray,
        function: (Double) -> Double,
    ): GMResult<Double, NumericsError> = Qag(interval, function)

    // JSXGraph: src/math/numerics.js -> Neville
    @Suppress("FunctionName")
    internal fun Neville(points: List<CoordsElement>): NevilleInterpolation =
        NevilleInterpolation(points)

    // JSXGraph: src/math/numerics.js -> generatePolynomialTerm
    internal fun generatePolynomialTerm(
        coefficients: DoubleArray,
        degree: Int,
        variableName: String,
        precision: Int,
    ): GMResult<String, NumericsError> {
        if (degree >= coefficients.size) {
            return GMResult.Err(
                NumericsError.InvalidPolynomialDegree(
                    degree = degree,
                    coefficientCount = coefficients.size,
                ),
            )
        }
        if (precision !in 1..100) {
            return GMResult.Err(NumericsError.InvalidPolynomialPrecision(precision))
        }

        val term = StringBuilder()
        for (index in degree downTo 0) {
            term.append('(')
            term.append(JsNumberFormat.precision(coefficients[index], precision))
            term.append(')')
            when {
                index > 1 -> {
                    term.append('*')
                    term.append(variableName)
                    term.append("<sup>")
                    term.append(index)
                    term.append("</sup> + ")
                }

                index == 1 -> {
                    term.append('*')
                    term.append(variableName)
                    term.append(" + ")
                }
            }
        }
        return GMResult.Ok(term.toString())
    }

    // JSXGraph: src/math/numerics.js -> lagrangePolynomial
    internal fun lagrangePolynomial(points: List<CoordsElement>): LagrangePolynomial =
        LagrangePolynomial(points)

    // JSXGraph: src/math/numerics.js -> lagrangePolynomialTerm
    internal fun lagrangePolynomialTerm(
        points: List<CoordsElement>,
        digits: Int? = null,
        parameter: String? = null,
        multiplicationSymbol: String? = null,
    ): () -> String = {
        val degree = points.size - 1
        val coefficients = lagrangePolynomialCoefficients(points)()
        val variableName = parameter?.takeIf { it.isNotEmpty() } ?: "x"
        val dot = multiplicationSymbol ?: " * "
        val term = StringBuilder()
        var isLeading = true

        for (index in coefficients.indices) {
            var coefficient = coefficients[index]
            if (abs(coefficient) < Mat.eps) {
                continue
            }
            if (digits != null) {
                coefficient = JsNumberFormat.roundDecimal(coefficient, digits)
            }

            if (isLeading) {
                if (coefficient > 0.0) {
                    term.append(JsNumberFormat.compact(coefficient))
                } else {
                    term.append('-')
                    term.append(JsNumberFormat.compact(-coefficient))
                }
                isLeading = false
            } else if (coefficient > 0.0) {
                term.append(" + ")
                term.append(JsNumberFormat.compact(coefficient))
            } else {
                term.append(" - ")
                term.append(JsNumberFormat.compact(-coefficient))
            }

            when {
                degree - index > 1 -> {
                    term.append(dot)
                    term.append(variableName)
                    term.append('^')
                    term.append(degree - index)
                }

                degree - index == 1 -> {
                    term.append(dot)
                    term.append(variableName)
                }
            }
        }
        term.toString()
    }

    // JSXGraph: src/math/numerics.js -> lagrangePolynomialCoefficients
    internal fun lagrangePolynomialCoefficients(
        points: List<CoordsElement>,
    ): () -> DoubleArray = {
        val size = points.size
        val coefficientSum = DoubleArray(size)

        for (index in 0 until size) {
            var scale = points[index].Y()
            val x = points[index].X()
            val zeroes = DoubleArray(size - 1)
            var zeroIndex = 0
            for (otherIndex in 0 until size) {
                if (otherIndex != index) {
                    scale /= x - points[otherIndex].X()
                    zeroes[zeroIndex] = points[otherIndex].X()
                    zeroIndex += 1
                }
            }

            val vietaCoefficients = Mat.Vieta(zeroes)
            for (coefficientIndex in 0 until size) {
                val coefficient = if (coefficientIndex == 0) {
                    1.0
                } else {
                    vietaCoefficients[coefficientIndex - 1]
                }
                coefficientSum[coefficientIndex] +=
                    (if (coefficientIndex % 2 == 1) -1.0 else 1.0) *
                    coefficient *
                    scale
            }
        }
        coefficientSum
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

    // JSXGraph: src/math/numerics.js -> rungeKutta
    fun rungeKutta(
        methodName: String,
        initialValues: DoubleArray,
        interval: DoubleArray,
        stepCount: Int,
        function: (Double, DoubleArray) -> DoubleArray,
    ): GMResult<Array<DoubleArray>, NumericsError> {
        val tableau = when (methodName) {
            "rk4" -> rk4Tableau
            "heun" -> heunTableau
            else -> eulerTableau
        }
        return rungeKutta(tableau, initialValues, interval, stepCount, function)
    }

    // JSXGraph: src/math/numerics.js -> rungeKutta
    fun rungeKutta(
        method: RungeKuttaMethod,
        initialValues: DoubleArray,
        interval: DoubleArray,
        stepCount: Int,
        function: (Double, DoubleArray) -> DoubleArray,
    ): GMResult<Array<DoubleArray>, NumericsError> {
        val tableau = when (method) {
            RungeKuttaMethod.EULER -> eulerTableau
            RungeKuttaMethod.HEUN -> heunTableau
            RungeKuttaMethod.RK4 -> rk4Tableau
        }
        return rungeKutta(tableau, initialValues, interval, stepCount, function)
    }

    // JSXGraph: src/math/numerics.js -> rungeKutta
    fun rungeKutta(
        tableau: ButcherTableau,
        initialValues: DoubleArray,
        interval: DoubleArray,
        stepCount: Int,
        function: (Double, DoubleArray) -> DoubleArray,
    ): GMResult<Array<DoubleArray>, NumericsError> {
        if (interval.size < 2) {
            return GMResult.Err(NumericsError.InvalidInterval(interval.size))
        }
        if (stepCount <= 0) {
            return GMResult.Err(NumericsError.InvalidRungeKuttaStepCount(stepCount))
        }
        if (initialValues.isEmpty()) {
            return GMResult.Err(
                NumericsError.InvalidSystemDimension(
                    dimension = 0,
                    initialValueCount = 0,
                ),
            )
        }

        val invalidCoefficientRow = (0 until tableau.stageCount).firstOrNull { row ->
            row >= tableau.coefficients.size ||
                tableau.coefficients[row].size < row
        }
        if (
            tableau.stageCount <= 0 ||
            invalidCoefficientRow != null ||
            tableau.weights.size < tableau.stageCount ||
            tableau.nodes.size < tableau.stageCount
        ) {
            return GMResult.Err(
                NumericsError.InvalidButcherTableau(
                    stageCount = tableau.stageCount,
                    coefficientRowCount = tableau.coefficients.size,
                    invalidCoefficientRow = invalidCoefficientRow,
                    weightCount = tableau.weights.size,
                    nodeCount = tableau.nodes.size,
                ),
            )
        }

        val step = (interval[1] - interval[0]) / stepCount
        var time = interval[0]
        val dimension = initialValues.size
        val state = initialValues.copyOf()
        val intermediateState = DoubleArray(dimension)
        val result = ArrayList<DoubleArray>(stepCount + 1)

        for (iteration in 0..stepCount) {
            result.add(state.copyOf())
            val stageDerivatives = ArrayList<DoubleArray>(tableau.stageCount)
            for (stage in 0 until tableau.stageCount) {
                intermediateState.fill(0.0)
                for (previousStage in 0 until stage) {
                    for (index in 0 until dimension) {
                        intermediateState[index] +=
                            tableau.coefficients[stage][previousStage] *
                            step *
                            stageDerivatives[previousStage][index]
                    }
                }
                for (index in 0 until dimension) {
                    intermediateState[index] += state[index]
                }

                val derivative = function(
                    time + tableau.nodes[stage] * step,
                    intermediateState,
                )
                if (derivative.size < dimension) {
                    return GMResult.Err(
                        NumericsError.InvalidFunctionResult(
                            expectedCount = dimension,
                            actualCount = derivative.size,
                        ),
                    )
                }
                stageDerivatives.add(derivative)
            }

            intermediateState.fill(0.0)
            for (stage in 0 until tableau.stageCount) {
                for (index in 0 until dimension) {
                    intermediateState[index] +=
                        tableau.weights[stage] * stageDerivatives[stage][index]
                }
            }
            for (index in 0 until dimension) {
                state[index] += step * intermediateState[index]
            }
            time += step
        }
        return GMResult.Ok(result.toTypedArray())
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

    // JSXGraph: src/math/numerics.js -> polzeros
    fun polzeros(
        coefficients: DoubleArray,
        degree: Int? = null,
        tolerance: Double = 2.220446049250313e-16,
        maxIterations: Int = 30,
        initialValues: Array<Complex>? = null,
    ): GMResult<Array<Complex>, NumericsError> =
        polzeros(
            coefficients = Array(coefficients.size) { index ->
                Complex(coefficients[index])
            },
            degree = degree,
            tolerance = tolerance,
            maxIterations = maxIterations,
            initialValues = initialValues,
        )

    // JSXGraph: src/math/numerics.js -> polzeros
    fun polzeros(
        coefficients: Array<Complex>,
        degree: Int? = null,
        tolerance: Double = 2.220446049250313e-16,
        maxIterations: Int = 30,
        initialValues: Array<Complex>? = null,
    ): GMResult<Array<Complex>, NumericsError> {
        var coefficientCount = coefficients.size
        if (degree != null && degree >= 0 && degree < coefficientCount - 1) {
            coefficientCount = degree + 1
        }

        val complexCoefficients = ArrayList<Complex>(coefficientCount)
        for (index in 0 until coefficientCount) {
            complexCoefficients.add(Complex(coefficients[index]))
        }

        val firstNonZero = complexCoefficients.indexOfFirst { coefficient ->
            coefficient.real != 0.0 || coefficient.imaginary != 0.0
        }.let { index -> if (index < 0) 0 else index }
        val obviousRoots = ArrayList<Complex>(firstNonZero)
        repeat(firstNonZero) {
            obviousRoots.add(Complex())
        }
        if (firstNonZero > 0) {
            repeat(firstNonZero) {
                complexCoefficients.removeAt(0)
            }
        }

        while (
            complexCoefficients.isNotEmpty() &&
            complexCoefficients.last().real == 0.0 &&
            complexCoefficients.last().imaginary == 0.0
        ) {
            complexCoefficients.removeAt(complexCoefficients.lastIndex)
        }
        if (complexCoefficients.size <= 1) {
            return GMResult.Ok(obviousRoots.toTypedArray())
        }

        val rootCount = complexCoefficients.size - 1
        val roots = if (initialValues != null) {
            if (initialValues.size < rootCount) {
                return GMResult.Err(
                    NumericsError.InvalidInitialRootCount(
                        expectedCount = rootCount,
                        actualCount = initialValues.size,
                    ),
                )
            }
            Array(rootCount) { index -> Complex(initialValues[index]) }
        } else {
            initialPolynomialRoots(complexCoefficients)
        }

        val effectiveTolerance =
            if (tolerance == 0.0 || tolerance.isNaN()) {
                2.220446049250313e-16
            } else {
                tolerance
            }
        val effectiveMaxIterations = if (maxIterations == 0) 30 else maxIterations
        aberthIteration(
            coefficients = complexCoefficients,
            tolerance = effectiveTolerance,
            maxIterations = effectiveMaxIterations,
            roots = roots,
        )

        val result = ArrayList<Complex>(obviousRoots.size + roots.size)
        result.addAll(obviousRoots)
        result.addAll(roots)
        result.sortWith { first, second ->
            when {
                first.real < second.real -> -1
                first.real > second.real -> 1
                else -> 0
            }
        }
        return GMResult.Ok(result.toTypedArray())
    }

    // JSXGraph: src/math/numerics.js -> RamerDouglasPeucker, _RDP, _RDPfindSplit
    internal fun RamerDouglasPeucker(
        points: List<Coords>,
        tolerance: Double,
        useUserCoordinates: Boolean = false,
    ): GMResult<List<Coords>, NumericsError> {
        if (!tolerance.isFinite() || tolerance < 0.0) {
            return GMResult.Err(
                NumericsError.InvalidSimplificationTolerance(tolerance),
            )
        }

        val simplified = mutableListOf<Coords>()
        var start = 0
        while (true) {
            while (start < points.size && points[start].hasNaNScreenCoordinates()) {
                start += 1
            }

            var end = start + 1
            while (end < points.size && !points[end].hasNaNScreenCoordinates()) {
                end += 1
            }
            end -= 1

            if (start < points.size && end > start) {
                simplified += simplifyPolylineSegment(
                    points = points,
                    start = start,
                    end = end,
                    tolerance = tolerance,
                    useUserCoordinates = useUserCoordinates,
                )
            }
            if (start >= points.size) {
                break
            }
            if (
                end < points.lastIndex &&
                points[end + 1].hasNaNScreenCoordinates()
            ) {
                simplified += points[end + 1]
            }
            start = end + 1
        }
        return GMResult.Ok(simplified)
    }

    // JSXGraph: src/math/numerics.js -> Visvalingam
    internal fun Visvalingam(
        points: List<Coords>,
        numberOfIntermediatePoints: Int,
    ): GMResult<List<Coords>, NumericsError> {
        if (numberOfIntermediatePoints < 0) {
            return GMResult.Err(
                NumericsError.InvalidSimplificationPointCount(
                    numberOfIntermediatePoints,
                ),
            )
        }
        if (points.size <= 2) {
            return GMResult.Ok(points)
        }

        val links = arrayOfNulls<VisvalingamLink>(points.size)
        val heap = mutableListOf<VisvalingamNode>()
        links[0] = VisvalingamLink(left = null, node = null)

        var left = 0
        for (index in 1 until points.lastIndex) {
            val volume = triangleVolume(
                points[index - 1],
                points[index],
                points[index + 1],
            )
            if (!volume.isNaN()) {
                val node = VisvalingamNode(volume = volume, index = index)
                heap += node
                links[index] = VisvalingamLink(
                    left = left,
                    node = node,
                )
                links[left]?.right = index
                left = index
            }
        }

        links[points.lastIndex] = VisvalingamLink(
            left = left,
            node = null,
        )
        links[left]?.right = points.lastIndex

        var lastVolume = Double.NEGATIVE_INFINITY
        while (heap.size > numberOfIntermediatePoints) {
            heap.sortWith { first, second ->
                when {
                    first.volume < second.volume -> 1
                    first.volume > second.volume -> -1
                    else -> 0
                }
            }

            val removed = heap.removeAt(heap.lastIndex)
            val removedLink = links[removed.index]
                ?: return GMResult.Err(
                    NumericsError.InvalidSimplificationTopology(removed.index),
                )
            lastVolume = removed.volume
            val leftIndex = removedLink.left
                ?: return GMResult.Err(
                    NumericsError.InvalidSimplificationTopology(removed.index),
                )
            val rightIndex = removedLink.right
                ?: return GMResult.Err(
                    NumericsError.InvalidSimplificationTopology(removed.index),
                )
            val leftLink = links[leftIndex]
                ?: return GMResult.Err(
                    NumericsError.InvalidSimplificationTopology(leftIndex),
                )
            val rightLink = links[rightIndex]
                ?: return GMResult.Err(
                    NumericsError.InvalidSimplificationTopology(rightIndex),
                )
            leftLink.right = rightIndex
            rightLink.left = leftIndex

            leftLink.left?.let { secondLeftIndex ->
                val volume = triangleVolume(
                    points[secondLeftIndex],
                    points[leftIndex],
                    points[rightIndex],
                )
                leftLink.node?.volume =
                    if (volume >= lastVolume) volume else lastVolume
            }
            rightLink.right?.let { secondRightIndex ->
                val volume = triangleVolume(
                    points[leftIndex],
                    points[rightIndex],
                    points[secondRightIndex],
                )
                rightLink.node?.volume =
                    if (volume >= lastVolume) volume else lastVolume
            }
        }

        val simplified = mutableListOf(points.first())
        var index = 0
        while (index != points.lastIndex) {
            index = links[index]?.right
                ?: return GMResult.Err(
                    NumericsError.InvalidSimplificationTopology(index),
                )
            simplified += points[index]
        }
        return GMResult.Ok(simplified)
    }

    private fun simplifyPolylineSegment(
        points: List<Coords>,
        start: Int,
        end: Int,
        tolerance: Double,
        useUserCoordinates: Boolean,
    ): List<Coords> {
        val keep = BooleanArray(end - start + 1)
        keep[0] = true
        keep[keep.lastIndex] = true
        val pending = mutableListOf(start to end)

        while (pending.isNotEmpty()) {
            val (segmentStart, segmentEnd) = pending.removeAt(pending.lastIndex)
            val split = rdpFindSplit(
                points = points,
                start = segmentStart,
                end = segmentEnd,
                useUserCoordinates = useUserCoordinates,
            )
            if (split.distance > tolerance) {
                keep[split.index - start] = true
                pending += segmentStart to split.index
                pending += split.index to segmentEnd
            }
        }

        return buildList {
            for (index in start..end) {
                if (keep[index - start]) {
                    add(points[index])
                }
            }
        }
    }

    private fun rdpFindSplit(
        points: List<Coords>,
        start: Int,
        end: Int,
        useUserCoordinates: Boolean,
    ): PolylineSplit {
        if (end - start < 2) {
            return PolylineSplit(distance = -1.0, index = start)
        }

        val startCoordinates = points[start].coordinates(useUserCoordinates)
        val endCoordinates = points[end].coordinates(useUserCoordinates)
        if (startCoordinates[1].isNaN() || startCoordinates[2].isNaN()) {
            return PolylineSplit(distance = Double.NaN, index = start)
        }
        if (endCoordinates[1].isNaN() || endCoordinates[2].isNaN()) {
            return PolylineSplit(distance = Double.NaN, index = end)
        }

        val lineX = clampSimplificationDifference(
            endCoordinates[1] - startCoordinates[1],
        )
        val lineY = clampSimplificationDifference(
            endCoordinates[2] - startCoordinates[2],
        )
        val denominator = lineX * lineX + lineY * lineY
        var maximumSquaredDistance = 0.0
        var splitIndex = start

        for (index in start + 1 until end) {
            val coordinates = points[index].coordinates(useUserCoordinates)
            if (coordinates[1].isNaN() || coordinates[2].isNaN()) {
                return PolylineSplit(distance = Double.NaN, index = index)
            }

            var pointX = clampSimplificationDifference(
                coordinates[1] - startCoordinates[1],
            )
            var pointY = clampSimplificationDifference(
                coordinates[2] - startCoordinates[2],
            )
            if (denominator > Mat.eps * Mat.eps) {
                val lambda =
                    (pointX * lineX + pointY * lineY) / denominator
                val boundedLambda = lambda.coerceIn(0.0, 1.0)
                pointX -= boundedLambda * lineX
                pointY -= boundedLambda * lineY
            }
            val squaredDistance = pointX * pointX + pointY * pointY
            if (squaredDistance > maximumSquaredDistance) {
                maximumSquaredDistance = squaredDistance
                splitIndex = index
            }
        }
        return PolylineSplit(
            distance = sqrt(maximumSquaredDistance),
            index = splitIndex,
        )
    }

    private fun triangleVolume(
        first: Coords,
        second: Coords,
        third: Coords,
    ): Double = abs(
        det(
            arrayOf(
                first.usrCoords,
                second.usrCoords,
                third.usrCoords,
            ),
        ),
    )

    private fun Coords.coordinates(useUserCoordinates: Boolean): DoubleArray =
        if (useUserCoordinates) usrCoords else scrCoords

    private fun Coords.hasNaNScreenCoordinates(): Boolean =
        (scrCoords[1] + scrCoords[2]).isNaN()

    private fun clampSimplificationDifference(value: Double): Double = when (value) {
        Double.POSITIVE_INFINITY -> 10000.0
        Double.NEGATIVE_INFINITY -> -10000.0
        else -> value
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

    private fun hornerComplex(
        coefficients: List<Complex>,
        value: Complex,
        derivative: Boolean = false,
    ): Complex {
        val degree = coefficients.size - 1
        var result = if (derivative) {
            C.mult(degree.toDouble(), coefficients[degree])
        } else {
            C.copy(coefficients[degree])
        }
        val end = if (derivative) 1 else 0
        for (index in degree - 1 downTo end) {
            result.mult(value)
            result.add(
                if (derivative) {
                    C.mult(coefficients[index], index.toDouble())
                } else {
                    coefficients[index]
                },
            )
        }
        return result
    }

    private fun hornerReciprocal(
        coefficients: List<Complex>,
        value: Complex,
        derivative: Boolean = false,
    ): Complex {
        val degree = coefficients.size - 1
        var result = if (derivative) {
            C.mult(degree.toDouble(), coefficients[0])
        } else {
            C.copy(coefficients[0])
        }
        val end = if (derivative) 1 else 0
        for (index in degree - 1 downTo end) {
            result.mult(value)
            result.add(
                if (derivative) {
                    C.mult(coefficients[degree - index], index.toDouble())
                } else {
                    coefficients[degree - index]
                },
            )
        }
        return result
    }

    private fun hornerReal(
        coefficients: DoubleArray,
        value: Double,
    ): Double {
        var result = coefficients.last()
        for (index in coefficients.lastIndex - 1 downTo 0) {
            result = result * value + coefficients[index]
        }
        return result
    }

    private fun initialPolynomialRoots(
        coefficients: List<Complex>,
    ): Array<Complex> {
        val degree = coefficients.size - 1
        val angleStep = kotlin.math.PI * 2.0 / degree
        val initialAngle = kotlin.math.PI / degree * 0.5
        val center = C.mult(-1.0, coefficients[degree - 1])
        center.div(C.mult(degree.toDouble(), coefficients[degree]))

        val polynomialAtCenter = C.div(
            hornerComplex(coefficients, center),
            coefficients[degree],
        )
        var radius = C.abs(polynomialAtCenter).pow(1.0 / degree)
        if (radius == 0.0) {
            radius = 1.0
        }
        return Array(degree) { index ->
            C.add(
                center,
                Complex(
                    real = radius * cos(angleStep * index + initialAngle),
                    imaginary = radius * sin(angleStep * index + initialAngle),
                ),
            )
        }
    }

    private fun aberthIteration(
        coefficients: List<Complex>,
        tolerance: Double,
        maxIterations: Int,
        roots: Array<Complex>,
    ) {
        val done = BooleanArray(roots.size)
        val stoppingCoefficients = DoubleArray(coefficients.size) { index ->
            C.abs(coefficients[index]) * (4.0 * index + 1.0)
        }
        var completedCount = 0

        for (iteration in 0 until maxIterations) {
            if (completedCount >= roots.size) {
                break
            }
            for (rootIndex in roots.indices) {
                if (done[rootIndex]) {
                    continue
                }

                var numerator = hornerComplex(coefficients, roots[rootIndex])
                val rootMagnitude = C.abs(roots[rootIndex])
                if (
                    C.abs(numerator) <
                    tolerance * hornerReal(stoppingCoefficients, rootMagnitude)
                ) {
                    done[rootIndex] = true
                    completedCount += 1
                    if (completedCount == roots.size) {
                        break
                    }
                    continue
                }

                if (rootMagnitude > 1.0) {
                    val reciprocalRoot = C.div(1.0, roots[rootIndex])
                    val derivative = hornerReciprocal(
                        coefficients,
                        reciprocalRoot,
                        derivative = true,
                    )
                    derivative.div(hornerReciprocal(coefficients, reciprocalRoot))
                    derivative.mult(reciprocalRoot)
                    numerator = C.sub(roots.size.toDouble(), derivative)
                    numerator = C.div(roots[rootIndex], numerator)
                } else {
                    numerator.div(
                        hornerComplex(
                            coefficients,
                            roots[rootIndex],
                            derivative = true,
                        ),
                    )
                }

                var denominator = Complex()
                for (otherIndex in roots.indices) {
                    if (otherIndex == rootIndex) {
                        continue
                    }
                    val reciprocalDifference =
                        C.div(1.0, C.sub(roots[rootIndex], roots[otherIndex]))
                    denominator.add(reciprocalDifference)
                }
                denominator.mult(numerator)
                denominator = C.sub(1.0, denominator)
                numerator.div(denominator)
                roots[rootIndex].sub(numerator)
            }
        }
    }

    private fun applyGaussKronrod(
        rule: GaussKronrodRule,
        interval: DoubleArray,
        function: (Double) -> Double,
    ): GMResult<GaussKronrodResult, NumericsError> = when (rule) {
        GaussKronrodRule.FIFTEEN -> GaussKronrod15(interval, function)
        GaussKronrodRule.TWENTY_ONE -> GaussKronrod21(interval, function)
        GaussKronrodRule.THIRTY_ONE -> GaussKronrod31(interval, function)
    }

    // JSXGraph: src/math/numerics.js -> _gaussKronrod
    private fun gaussKronrod(
        interval: DoubleArray,
        function: (Double) -> Double,
        abscissae: DoubleArray,
        gaussWeights: DoubleArray,
        kronrodWeights: DoubleArray,
    ): GMResult<GaussKronrodResult, NumericsError> {
        if (interval.size < 2) {
            return GMResult.Err(NumericsError.InvalidInterval(interval.size))
        }

        val count = abscissae.size
        val center = 0.5 * (interval[0] + interval[1])
        val halfLength = 0.5 * (interval[1] - interval[0])
        val absoluteHalfLength = abs(halfLength)
        val centerValue = function(center)
        var gaussResult = 0.0
        var kronrodResult = centerValue * kronrodWeights[count - 1]
        var absoluteResult = abs(kronrodResult)
        val firstValues = DoubleArray(count - 1)
        val secondValues = DoubleArray(count - 1)

        if (count % 2 == 0) {
            gaussResult = centerValue * gaussWeights[count / 2 - 1]
        }

        for (index in 0 until (count - 1) / 2) {
            val abscissaIndex = index * 2 + 1
            val abscissa = halfLength * abscissae[abscissaIndex]
            val firstValue = function(center - abscissa)
            val secondValue = function(center + abscissa)
            val sum = firstValue + secondValue
            firstValues[abscissaIndex] = firstValue
            secondValues[abscissaIndex] = secondValue
            gaussResult += gaussWeights[index] * sum
            kronrodResult += kronrodWeights[abscissaIndex] * sum
            absoluteResult +=
                kronrodWeights[abscissaIndex] *
                (abs(firstValue) + abs(secondValue))
        }

        for (index in 0 until count / 2) {
            val abscissaIndex = index * 2
            val abscissa = halfLength * abscissae[abscissaIndex]
            val firstValue = function(center - abscissa)
            val secondValue = function(center + abscissa)
            firstValues[abscissaIndex] = firstValue
            secondValues[abscissaIndex] = secondValue
            kronrodResult +=
                kronrodWeights[abscissaIndex] * (firstValue + secondValue)
            absoluteResult +=
                kronrodWeights[abscissaIndex] *
                (abs(firstValue) + abs(secondValue))
        }

        val mean = kronrodResult * 0.5
        var absoluteDeviation =
            kronrodWeights[count - 1] * abs(centerValue - mean)
        for (index in 0 until count - 1) {
            absoluteDeviation +=
                kronrodWeights[index] *
                (
                    abs(firstValues[index] - mean) +
                        abs(secondValues[index] - mean)
                )
        }

        val rawError = (kronrodResult - gaussResult) * halfLength
        val value = kronrodResult * halfLength
        absoluteResult *= absoluteHalfLength
        absoluteDeviation *= absoluteHalfLength
        return GMResult.Ok(
            GaussKronrodResult(
                value = value,
                absoluteError = rescaleIntegrationError(
                    error = rawError,
                    absoluteResult = absoluteResult,
                    absoluteDeviation = absoluteDeviation,
                ),
                absoluteResult = absoluteResult,
                absoluteDeviation = absoluteDeviation,
            ),
        )
    }

    // JSXGraph: src/math/numerics.js -> _rescale_error
    private fun rescaleIntegrationError(
        error: Double,
        absoluteResult: Double,
        absoluteDeviation: Double,
    ): Double {
        var result = abs(error)
        if (absoluteDeviation != 0.0 && result != 0.0) {
            val scale = (200.0 * result / absoluteDeviation).pow(1.5)
            result = if (scale < 1.0) absoluteDeviation * scale else absoluteDeviation
        }

        val minimumValue = 2.2250738585072014e-308
        val machineEpsilon = 2.2204460492503131e-16
        if (absoluteResult > minimumValue / (50.0 * machineEpsilon)) {
            val minimumError = 50.0 * machineEpsilon * absoluteResult
            if (minimumError > result) {
                result = minimumError
            }
        }
        return result
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
