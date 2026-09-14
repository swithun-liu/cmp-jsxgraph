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

    data class InvalidInterval(val size: Int) : NumericsError

    data class InvalidIntegrationNodeCount(
        val nodeCount: Int,
        val type: IntegrationType,
    ) : NumericsError

    data class InvalidQuadratureOrder(val order: Int) : NumericsError
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

object Numerics {
    private data class LegendreRule(
        val nodes: DoubleArray,
        val weights: DoubleArray,
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
