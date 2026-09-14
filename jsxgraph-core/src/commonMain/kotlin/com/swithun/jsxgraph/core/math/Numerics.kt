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
import kotlin.math.sin

sealed interface NumericsError {
    data class DimensionMismatch(
        val matrixRows: Int,
        val matrixColumns: Int,
        val vectorSize: Int,
    ) : NumericsError

    data object SingularMatrix : NumericsError
}

data class JacobiResult(
    val diagonalizedMatrix: Array<DoubleArray>,
    val eigenvectors: Array<DoubleArray>,
)

object Numerics {
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
}
