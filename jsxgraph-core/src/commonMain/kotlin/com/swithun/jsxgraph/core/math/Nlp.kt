/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/nlp.js -> Nlp
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Reinhard Oldenburg,
 * Andreas Walter, and Alfred Wassermann.
 * Copyright 2012 Anders Gustafsson, Cureos AB.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal enum class NlpStatus {
    NORMAL,
    MAX_ITERATIONS_REACHED,
    DIVERGING_ROUNDING_ERRORS,
}

internal sealed interface NlpError<out E> {
    data class InvalidVariableCount(
        val count: Int,
    ) : NlpError<Nothing>

    data class InvalidConstraintCount(
        val count: Int,
    ) : NlpError<Nothing>

    data class InvalidInitialValueCount(
        val expected: Int,
        val actual: Int,
    ) : NlpError<Nothing>

    data class InvalidTrustRegion(
        val initial: Double,
        val final: Double,
    ) : NlpError<Nothing>

    data class InvalidPrintLevel(
        val level: Int,
    ) : NlpError<Nothing>

    data class InvalidEvaluationLimit(
        val limit: Int,
    ) : NlpError<Nothing>

    data class Evaluation<E>(
        val cause: E,
    ) : NlpError<E>
}

internal fun interface NlpCalculation<E> {
    fun evaluate(
        variableCount: Int,
        constraintCount: Int,
        variables: DoubleArray,
        constraints: DoubleArray,
    ): GMResult<Double, E>
}

/**
 * Powell's Constrained Optimization BY Linear Approximation.
 *
 * The work arrays intentionally remain one-based to preserve the control flow
 * and indexing of JSXGraph's jcobyla-derived implementation.
 */
internal object Nlp {
    internal var lastNumberOfEvaluations: Int = 0
        private set

    // JSXGraph: src/math/nlp.js -> FindMinimum.
    internal fun <E> findMinimum(
        calculation: NlpCalculation<E>,
        variableCount: Int,
        constraintCount: Int,
        variables: DoubleArray,
        initialTrustRegion: Double,
        finalTrustRegion: Double,
        printLevel: Int = 0,
        maximumEvaluations: Int,
        testForRoundingErrors: Boolean = false,
        logger: ((String) -> Unit)? = null,
    ): GMResult<NlpStatus, NlpError<E>> {
        if (variableCount <= 0) {
            return GMResult.Err(
                NlpError.InvalidVariableCount(variableCount),
            )
        }
        if (constraintCount < 0) {
            return GMResult.Err(
                NlpError.InvalidConstraintCount(constraintCount),
            )
        }
        if (variables.size < variableCount) {
            return GMResult.Err(
                NlpError.InvalidInitialValueCount(
                    expected = variableCount,
                    actual = variables.size,
                ),
            )
        }
        if (
            !initialTrustRegion.isFinite() ||
            !finalTrustRegion.isFinite() ||
            initialTrustRegion <= 0.0 ||
            finalTrustRegion <= 0.0 ||
            finalTrustRegion > initialTrustRegion
        ) {
            return GMResult.Err(
                NlpError.InvalidTrustRegion(
                    initial = initialTrustRegion,
                    final = finalTrustRegion,
                ),
            )
        }
        if (printLevel !in 0..3) {
            return GMResult.Err(NlpError.InvalidPrintLevel(printLevel))
        }
        if (maximumEvaluations <= 0) {
            return GMResult.Err(
                NlpError.InvalidEvaluationLimit(maximumEvaluations),
            )
        }

        val mpp = constraintCount + 2
        val internalVariables = DoubleArray(variableCount + 1)
        copy(
            source = variables,
            sourceStart = 0,
            destination = internalVariables,
            destinationStart = 1,
            count = variableCount,
        )
        lastNumberOfEvaluations = 0

        val internalCalculation =
            InternalCalculation<E> { n, m, x, constraints ->
                val externalVariables = DoubleArray(n)
                copy(
                    source = x,
                    sourceStart = 1,
                    destination = externalVariables,
                    destinationStart = 0,
                    count = n,
                )
                val externalConstraints = DoubleArray(m)
                lastNumberOfEvaluations += 1
                when (
                    val result = calculation.evaluate(
                        n,
                        m,
                        externalVariables,
                        externalConstraints,
                    )
                ) {
                    is GMResult.Ok -> {
                        copy(
                            source = externalConstraints,
                            sourceStart = 0,
                            destination = constraints,
                            destinationStart = 1,
                            count = m,
                        )
                        GMResult.Ok(result.value)
                    }
                    is GMResult.Err ->
                        GMResult.Err(NlpError.Evaluation(result.error))
                }
            }

        val status = cobylb(
            calculation = internalCalculation,
            variableCount = variableCount,
            constraintCount = constraintCount,
            mpp = mpp,
            variables = internalVariables,
            initialTrustRegion = initialTrustRegion,
            finalTrustRegion = finalTrustRegion,
            printLevel = printLevel,
            maximumEvaluations = maximumEvaluations,
            testForRoundingErrors = testForRoundingErrors,
            logger = logger,
        )
        if (status is GMResult.Err) {
            return status
        }
        copy(
            source = internalVariables,
            sourceStart = 1,
            destination = variables,
            destinationStart = 0,
            count = variableCount,
        )
        return status
    }

    // JSXGraph: src/math/nlp.js -> cobylb.
    private fun <E> cobylb(
        calculation: InternalCalculation<E>,
        variableCount: Int,
        constraintCount: Int,
        mpp: Int,
        variables: DoubleArray,
        initialTrustRegion: Double,
        finalTrustRegion: Double,
        printLevel: Int,
        maximumEvaluations: Int,
        testForRoundingErrors: Boolean,
        logger: ((String) -> Unit)?,
    ): GMResult<NlpStatus, NlpError<E>> {
        val alpha = 0.25
        val beta = 2.1
        val gamma = 0.5
        val delta = 1.1
        var objective = 0.0
        var maximumResidual = 0.0
        var total: Double
        val np = variableCount + 1
        val mp = constraintCount + 1
        var rho = initialTrustRegion
        var parmu = 0.0
        var acceptableSimplex = false
        var fullStep = false
        var parsig = 0.0
        var predictedConstraintReduction = 0.0
        var predictedMeritReduction = 0.0
        val constraints = DoubleArray(1 + mpp)
        val simplex = matrix(1 + variableCount, 1 + np)
        val inverseSimplex = matrix(1 + variableCount, 1 + variableCount)
        val dataMatrix = matrix(1 + mpp, 1 + np)
        val approximations = matrix(1 + variableCount, 1 + mp)
        val vertexSigma = DoubleArray(1 + variableCount)
        val vertexEta = DoubleArray(1 + variableCount)
        val sigmaBar = DoubleArray(1 + variableCount)
        val displacement = DoubleArray(1 + variableCount)
        val work = DoubleArray(1 + variableCount)
        var status = NlpStatus.NORMAL

        if (printLevel >= 2) {
            logger?.invoke(
                "The initial value of RHO is $rho and PARMU is set to zero.",
            )
        }
        if (testForRoundingErrors) {
            logger?.invoke(
                "Experimental feature 'testForRoundingErrors' is activated.",
            )
        }

        var numberOfEvaluations = 0
        var temporary = 1.0 / rho
        for (index in 1..variableCount) {
            simplex[index][np] = variables[index]
            simplex[index][index] = rho
            inverseSimplex[index][index] = temporary
        }

        var droppedVertex = np
        var branch = false
        var skipVertexIdentification: Boolean

        mainLoop@ while (true) {
            if (
                numberOfEvaluations >= maximumEvaluations &&
                numberOfEvaluations > 0
            ) {
                status = NlpStatus.MAX_ITERATIONS_REACHED
                break@mainLoop
            }

            numberOfEvaluations += 1
            objective = when (
                val result = calculation.evaluate(
                    variableCount,
                    constraintCount,
                    variables,
                    constraints,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            maximumResidual = 0.0
            for (index in 1..constraintCount) {
                maximumResidual =
                    max(maximumResidual, -constraints[index])
            }
            if (
                numberOfEvaluations == printLevel - 1 ||
                printLevel == 3
            ) {
                printIterationResult(
                    numberOfEvaluations,
                    objective,
                    maximumResidual,
                    variables,
                    variableCount,
                    printLevel,
                    logger,
                )
            }

            constraints[mp] = objective
            constraints[mpp] = maximumResidual
            skipVertexIdentification = true

            if (!branch) {
                skipVertexIdentification = false
                for (index in 1..mpp) {
                    dataMatrix[index][droppedVertex] = constraints[index]
                }

                if (numberOfEvaluations <= np) {
                    if (droppedVertex <= variableCount) {
                        if (dataMatrix[mp][np] <= objective) {
                            variables[droppedVertex] =
                                simplex[droppedVertex][np]
                        } else {
                            simplex[droppedVertex][np] =
                                variables[droppedVertex]
                            for (index in 1..mpp) {
                                dataMatrix[index][droppedVertex] =
                                    dataMatrix[index][np]
                                dataMatrix[index][np] = constraints[index]
                            }
                            for (column in 1..droppedVertex) {
                                simplex[droppedVertex][column] = -rho
                                temporary = 0.0
                                for (row in column..droppedVertex) {
                                    temporary -=
                                        inverseSimplex[row][column]
                                }
                                inverseSimplex[droppedVertex][column] =
                                    temporary
                            }
                        }
                    }
                    if (numberOfEvaluations <= variableCount) {
                        droppedVertex = numberOfEvaluations
                        variables[droppedVertex] += rho
                        continue@mainLoop
                    }
                }
                branch = true
            }

            rhoLoop@ while (true) {
                trustRegionStep@ do {
                    if (!skipVertexIdentification) {
                        var minimumMerit =
                            dataMatrix[mp][np] +
                                parmu * dataMatrix[mpp][np]
                        var bestVertex = np
                        for (column in 1..variableCount) {
                            temporary =
                                dataMatrix[mp][column] +
                                    parmu * dataMatrix[mpp][column]
                            if (temporary < minimumMerit) {
                                bestVertex = column
                                minimumMerit = temporary
                            } else if (
                                temporary == minimumMerit &&
                                parmu == 0.0 &&
                                dataMatrix[mpp][column] <
                                dataMatrix[mpp][bestVertex]
                            ) {
                                bestVertex = column
                            }
                        }

                        if (bestVertex <= variableCount) {
                            for (row in 1..mpp) {
                                temporary = dataMatrix[row][np]
                                dataMatrix[row][np] =
                                    dataMatrix[row][bestVertex]
                                dataMatrix[row][bestVertex] = temporary
                            }
                            for (row in 1..variableCount) {
                                temporary = simplex[row][bestVertex]
                                simplex[row][bestVertex] = 0.0
                                simplex[row][np] += temporary
                                var secondary = 0.0
                                for (column in 1..variableCount) {
                                    simplex[row][column] -= temporary
                                    secondary -=
                                        inverseSimplex[column][row]
                                }
                                inverseSimplex[bestVertex][row] = secondary
                            }
                        }

                        var inverseError = 0.0
                        if (testForRoundingErrors) {
                            for (row in 1..variableCount) {
                                for (column in 1..variableCount) {
                                    temporary =
                                        dotProductRowColumn(
                                            inverseSimplex,
                                            row,
                                            simplex,
                                            column,
                                            1,
                                            variableCount,
                                        ) -
                                        if (row == column) 1.0 else 0.0
                                    inverseError =
                                        max(inverseError, abs(temporary))
                                }
                            }
                        }
                        if (inverseError > 0.1) {
                            status = NlpStatus.DIVERGING_ROUNDING_ERRORS
                            break@mainLoop
                        }

                        for (column in 1..mp) {
                            constraints[column] = -dataMatrix[column][np]
                            for (row in 1..variableCount) {
                                work[row] =
                                    dataMatrix[column][row] +
                                        constraints[column]
                            }
                            for (row in 1..variableCount) {
                                approximations[row][column] =
                                    (if (column == mp) -1.0 else 1.0) *
                                    dotProductRowColumn(
                                        work,
                                        inverseSimplex,
                                        row,
                                        1,
                                        variableCount,
                                    )
                            }
                        }

                        acceptableSimplex = true
                        parsig = alpha * rho
                        val pareta = beta * rho
                        for (column in 1..variableCount) {
                            var sigmaWork = 0.0
                            var etaWork = 0.0
                            for (row in 1..variableCount) {
                                sigmaWork +=
                                    inverseSimplex[column][row] *
                                    inverseSimplex[column][row]
                                etaWork +=
                                    simplex[row][column] *
                                    simplex[row][column]
                            }
                            vertexSigma[column] = 1.0 / sqrt(sigmaWork)
                            vertexEta[column] = sqrt(etaWork)
                            if (
                                vertexSigma[column] < parsig ||
                                vertexEta[column] > pareta
                            ) {
                                acceptableSimplex = false
                            }
                        }

                        if (!branch && !acceptableSimplex) {
                            droppedVertex = 0
                            temporary = pareta
                            for (column in 1..variableCount) {
                                if (vertexEta[column] > temporary) {
                                    droppedVertex = column
                                    temporary = vertexEta[column]
                                }
                            }
                            if (droppedVertex == 0) {
                                for (column in 1..variableCount) {
                                    if (vertexSigma[column] < temporary) {
                                        droppedVertex = column
                                        temporary = vertexSigma[column]
                                    }
                                }
                            }

                            temporary =
                                gamma * rho * vertexSigma[droppedVertex]
                            for (index in 1..variableCount) {
                                displacement[index] =
                                    temporary *
                                    inverseSimplex[droppedVertex][index]
                            }
                            var maximumPositiveViolation = 0.0
                            var maximumNegativeViolation = 0.0
                            total = 0.0
                            for (column in 1..mp) {
                                total = dotProductRowColumn(
                                    displacement,
                                    approximations,
                                    column,
                                    1,
                                    variableCount,
                                )
                                if (column < mp) {
                                    temporary = dataMatrix[column][np]
                                    maximumPositiveViolation = max(
                                        maximumPositiveViolation,
                                        -total - temporary,
                                    )
                                    maximumNegativeViolation = max(
                                        maximumNegativeViolation,
                                        total - temporary,
                                    )
                                }
                            }
                            val displacementSign =
                                if (
                                    parmu *
                                    (
                                        maximumPositiveViolation -
                                            maximumNegativeViolation
                                    ) > 2.0 * total
                                ) {
                                    -1.0
                                } else {
                                    1.0
                                }

                            temporary = 0.0
                            for (row in 1..variableCount) {
                                displacement[row] =
                                    displacementSign * displacement[row]
                                simplex[row][droppedVertex] =
                                    displacement[row]
                                temporary +=
                                    inverseSimplex[droppedVertex][row] *
                                    displacement[row]
                            }
                            for (column in 1..variableCount) {
                                inverseSimplex[droppedVertex][column] /=
                                    temporary
                            }
                            for (row in 1..variableCount) {
                                if (row != droppedVertex) {
                                    temporary = dotProductRowVector(
                                        inverseSimplex,
                                        row,
                                        displacement,
                                        1,
                                        variableCount,
                                    )
                                    for (column in 1..variableCount) {
                                        inverseSimplex[row][column] -=
                                            temporary *
                                            inverseSimplex[
                                                droppedVertex
                                            ][column]
                                    }
                                }
                                variables[row] =
                                    simplex[row][np] + displacement[row]
                            }
                            continue@mainLoop
                        }

                        fullStep = trstlp(
                            variableCount,
                            constraintCount,
                            approximations,
                            constraints,
                            rho,
                            displacement,
                        )
                        if (!fullStep) {
                            temporary = 0.0
                            for (index in 1..variableCount) {
                                temporary +=
                                    displacement[index] *
                                    displacement[index]
                            }
                            if (temporary < 0.25 * rho * rho) {
                                branch = true
                                break@trustRegionStep
                            }
                        }

                        total = 0.0
                        var newResidual = 0.0
                        constraints[mp] = 0.0
                        for (column in 1..mp) {
                            total =
                                constraints[column] -
                                    dotProductRowColumn(
                                        displacement,
                                        approximations,
                                        column,
                                        1,
                                        variableCount,
                                    )
                            if (column < mp) {
                                newResidual = max(newResidual, total)
                            }
                        }

                        predictedConstraintReduction =
                            dataMatrix[mpp][np] - newResidual
                        val requiredPenalty =
                            if (predictedConstraintReduction > 0.0) {
                                total / predictedConstraintReduction
                            } else {
                                0.0
                            }
                        if (parmu < 1.5 * requiredPenalty) {
                            parmu = 2.0 * requiredPenalty
                            if (printLevel >= 2) {
                                logger?.invoke("Increase in PARMU to $parmu")
                            }
                            val poleMerit =
                                dataMatrix[mp][np] +
                                    parmu * dataMatrix[mpp][np]
                            for (column in 1..variableCount) {
                                temporary =
                                    dataMatrix[mp][column] +
                                        parmu * dataMatrix[mpp][column]
                                if (
                                    temporary < poleMerit ||
                                    (
                                        temporary == poleMerit &&
                                            parmu == 0.0 &&
                                            dataMatrix[mpp][column] <
                                            dataMatrix[mpp][np]
                                    )
                                ) {
                                    continue@rhoLoop
                                }
                            }
                        }
                        predictedMeritReduction =
                            parmu * predictedConstraintReduction - total
                        for (index in 1..variableCount) {
                            variables[index] =
                                simplex[index][np] + displacement[index]
                        }
                        branch = true
                        continue@mainLoop
                    }

                    skipVertexIdentification = false
                    val oldMerit =
                        dataMatrix[mp][np] + parmu * dataMatrix[mpp][np]
                    val newMerit = objective + parmu * maximumResidual
                    var trueReduction = oldMerit - newMerit
                    if (
                        parmu == 0.0 &&
                        objective == dataMatrix[mp][np]
                    ) {
                        predictedMeritReduction =
                            predictedConstraintReduction
                        trueReduction =
                            dataMatrix[mpp][np] - maximumResidual
                    }

                    var ratio = if (trueReduction <= 0.0) 1.0 else 0.0
                    droppedVertex = 0
                    for (row in 1..variableCount) {
                        temporary = abs(
                            dotProductRowVector(
                                inverseSimplex,
                                row,
                                displacement,
                                1,
                                variableCount,
                            ),
                        )
                        if (temporary > ratio) {
                            droppedVertex = row
                            ratio = temporary
                        }
                        sigmaBar[row] = temporary * vertexSigma[row]
                    }

                    var maximumEdge = delta * rho
                    var replacement = 0
                    for (column in 1..variableCount) {
                        if (
                            sigmaBar[column] >= parsig ||
                            sigmaBar[column] >= vertexSigma[column]
                        ) {
                            temporary = vertexEta[column]
                            if (trueReduction > 0.0) {
                                temporary = 0.0
                                for (row in 1..variableCount) {
                                    val difference =
                                        displacement[row] -
                                            simplex[row][column]
                                    temporary += difference * difference
                                }
                                temporary = sqrt(temporary)
                            }
                            if (temporary > maximumEdge) {
                                replacement = column
                                maximumEdge = temporary
                            }
                        }
                    }
                    if (replacement > 0) {
                        droppedVertex = replacement
                    }

                    if (droppedVertex != 0) {
                        temporary = 0.0
                        for (row in 1..variableCount) {
                            simplex[row][droppedVertex] =
                                displacement[row]
                            temporary +=
                                inverseSimplex[droppedVertex][row] *
                                displacement[row]
                        }
                        for (column in 1..variableCount) {
                            inverseSimplex[droppedVertex][column] /=
                                temporary
                        }
                        for (row in 1..variableCount) {
                            if (row != droppedVertex) {
                                temporary = dotProductRowVector(
                                    inverseSimplex,
                                    row,
                                    displacement,
                                    1,
                                    variableCount,
                                )
                                for (column in 1..variableCount) {
                                    inverseSimplex[row][column] -=
                                        temporary *
                                        inverseSimplex[
                                            droppedVertex
                                        ][column]
                                }
                            }
                        }
                        for (row in 1..mpp) {
                            dataMatrix[row][droppedVertex] =
                                constraints[row]
                        }
                        if (
                            trueReduction > 0.0 &&
                            trueReduction >=
                            0.1 * predictedMeritReduction
                        ) {
                            continue@rhoLoop
                        }
                    }
                } while (false)

                if (!acceptableSimplex) {
                    branch = false
                    continue@rhoLoop
                }
                if (rho <= finalTrustRegion) {
                    status = NlpStatus.NORMAL
                    break@mainLoop
                }

                var minimumConstraintValue = 0.0
                var maximumConstraintValue = 0.0
                rho *= 0.5
                if (rho <= 1.5 * finalTrustRegion) {
                    rho = finalTrustRegion
                }
                if (parmu > 0.0) {
                    var denominator = 0.0
                    for (column in 1..mp) {
                        minimumConstraintValue = dataMatrix[column][np]
                        maximumConstraintValue = minimumConstraintValue
                        for (row in 1..variableCount) {
                            minimumConstraintValue = min(
                                minimumConstraintValue,
                                dataMatrix[column][row],
                            )
                            maximumConstraintValue = max(
                                maximumConstraintValue,
                                dataMatrix[column][row],
                            )
                        }
                        if (
                            column <= constraintCount &&
                            minimumConstraintValue <
                            0.5 * maximumConstraintValue
                        ) {
                            temporary =
                                max(maximumConstraintValue, 0.0) -
                                    minimumConstraintValue
                            denominator =
                                if (denominator <= 0.0) {
                                    temporary
                                } else {
                                    min(denominator, temporary)
                                }
                        }
                    }
                    if (denominator == 0.0) {
                        parmu = 0.0
                    } else if (
                        maximumConstraintValue -
                        minimumConstraintValue <
                        parmu * denominator
                    ) {
                        parmu =
                            (
                                maximumConstraintValue -
                                    minimumConstraintValue
                            ) / denominator
                    }
                }
                if (printLevel >= 2) {
                    logger?.invoke(
                        "Reduction in RHO to $rho and PARMU = $parmu",
                    )
                }
                if (printLevel == 2) {
                    printIterationResult(
                        numberOfEvaluations,
                        dataMatrix[mp][np],
                        dataMatrix[mpp][np],
                        column(simplex, np),
                        variableCount,
                        printLevel,
                        logger,
                    )
                }
            }
        }

        if (status == NlpStatus.NORMAL && fullStep) {
            if (printLevel >= 1) {
                logger?.invoke("Normal return from subroutine COBYLA")
                printIterationResult(
                    numberOfEvaluations,
                    objective,
                    maximumResidual,
                    variables,
                    variableCount,
                    printLevel,
                    logger,
                )
            }
            return GMResult.Ok(status)
        }
        if (printLevel >= 1) {
            when (status) {
                NlpStatus.NORMAL ->
                    logger?.invoke("Normal return from subroutine COBYLA")
                NlpStatus.MAX_ITERATIONS_REACHED ->
                    logger?.invoke(
                        "Return from COBYLA because MAXFUN was reached.",
                    )
                NlpStatus.DIVERGING_ROUNDING_ERRORS ->
                    logger?.invoke(
                        "Return from COBYLA because rounding errors diverged.",
                    )
            }
        }
        for (index in 1..variableCount) {
            variables[index] = simplex[index][np]
        }
        objective = dataMatrix[mp][np]
        maximumResidual = dataMatrix[mpp][np]
        if (printLevel >= 1) {
            printIterationResult(
                numberOfEvaluations,
                objective,
                maximumResidual,
                variables,
                variableCount,
                printLevel,
                logger,
            )
        }
        return GMResult.Ok(status)
    }

    // JSXGraph: src/math/nlp.js -> trstlp.
    private fun trstlp(
        variableCount: Int,
        constraintCount: Int,
        approximations: Array<DoubleArray>,
        bounds: DoubleArray,
        rho: Double,
        displacement: DoubleArray,
    ): Boolean {
        var temporary: Double
        var previousActiveCount = 0
        var oldResidual = 0.0
        val z = matrix(1 + variableCount, 1 + variableCount)
        val zDotActive = DoubleArray(2 + constraintCount)
        val currentMultipliers = DoubleArray(2 + constraintCount)
        val searchDirection = DoubleArray(1 + variableCount)
        val newDisplacement = DoubleArray(1 + variableCount)
        val newMultipliers = DoubleArray(2 + constraintCount)
        val activeIndices = IntArray(2 + constraintCount)
        var mcon = constraintCount
        var activeCount = 0

        for (index in 1..variableCount) {
            z[index][index] = 1.0
            displacement[index] = 0.0
        }

        var icon = 0
        var maximumResidual = 0.0
        if (constraintCount >= 1) {
            for (index in 1..constraintCount) {
                if (bounds[index] > maximumResidual) {
                    maximumResidual = bounds[index]
                    icon = index
                }
            }
            for (index in 1..constraintCount) {
                activeIndices[index] = index
                currentMultipliers[index] =
                    maximumResidual - bounds[index]
            }
        }

        var first = true
        stageLoop@ while (true) {
            activeSetLoop@ while (true) {
                if (!first || maximumResidual == 0.0) {
                    mcon = constraintCount + 1
                    icon = mcon
                    activeIndices[mcon] = mcon
                    currentMultipliers[mcon] = 0.0
                }
                first = false

                var oldOptimum = 0.0
                var iterationCountdown = 0
                var step = 0.0
                var fullStep = 0.0

                searchLoop@ while (true) {
                    val newOptimum =
                        if (mcon == constraintCount) {
                            maximumResidual
                        } else {
                            -dotProductRowColumn(
                                displacement,
                                approximations,
                                mcon,
                                1,
                                variableCount,
                            )
                        }
                    if (
                        iterationCountdown == 0 ||
                        newOptimum < oldOptimum
                    ) {
                        oldOptimum = newOptimum
                        previousActiveCount = activeCount
                        iterationCountdown = 3
                    } else if (activeCount > previousActiveCount) {
                        previousActiveCount = activeCount
                        iterationCountdown = 3
                    } else {
                        iterationCountdown -= 1
                    }
                    if (iterationCountdown == 0) {
                        break@activeSetLoop
                    }

                    var ratio = 0.0
                    if (icon <= activeCount) {
                        if (icon < activeCount) {
                            val savedIndex = activeIndices[icon]
                            val savedMultiplier = currentMultipliers[icon]
                            var index = icon
                            do {
                                val nextIndex = index + 1
                                val activeIndex = activeIndices[nextIndex]
                                val scalarProduct = dotProductColumns(
                                    z,
                                    index,
                                    approximations,
                                    activeIndex,
                                    1,
                                    variableCount,
                                )
                                temporary =
                                    Mat.hypot(
                                        scalarProduct,
                                        zDotActive[nextIndex],
                                    )
                                val alpha =
                                    zDotActive[nextIndex] / temporary
                                val beta = scalarProduct / temporary
                                zDotActive[nextIndex] =
                                    alpha * zDotActive[index]
                                zDotActive[index] = temporary
                                for (row in 1..variableCount) {
                                    temporary =
                                        alpha * z[row][nextIndex] +
                                            beta * z[row][index]
                                    z[row][nextIndex] =
                                        alpha * z[row][index] -
                                        beta * z[row][nextIndex]
                                    z[row][index] = temporary
                                }
                                activeIndices[index] = activeIndex
                                currentMultipliers[index] =
                                    currentMultipliers[nextIndex]
                                index = nextIndex
                            } while (index < activeCount)
                            activeIndices[index] = savedIndex
                            currentMultipliers[index] = savedMultiplier
                        }
                        activeCount -= 1
                        if (mcon > constraintCount) {
                            temporary = 1.0 / zDotActive[activeCount]
                            for (index in 1..variableCount) {
                                searchDirection[index] =
                                    temporary * z[index][activeCount]
                            }
                        } else {
                            temporary = dotProductRowColumn(
                                searchDirection,
                                z,
                                activeCount + 1,
                                1,
                                variableCount,
                            )
                            for (index in 1..variableCount) {
                                searchDirection[index] -=
                                    temporary * z[index][activeCount + 1]
                            }
                        }
                    } else {
                        val activeIndex = activeIndices[icon]
                        for (index in 1..variableCount) {
                            newDisplacement[index] =
                                approximations[index][activeIndex]
                        }
                        var total = 0.0
                        var index = variableCount
                        while (index > activeCount) {
                            var scalarProduct = 0.0
                            var absoluteProduct = 0.0
                            for (row in 1..variableCount) {
                                temporary =
                                    z[row][index] *
                                    newDisplacement[row]
                                scalarProduct += temporary
                                absoluteProduct += abs(temporary)
                            }
                            val accuracyA =
                                absoluteProduct +
                                    0.1 * abs(scalarProduct)
                            val accuracyB =
                                absoluteProduct +
                                    0.2 * abs(scalarProduct)
                            if (
                                absoluteProduct >= accuracyA ||
                                accuracyA >= accuracyB
                            ) {
                                scalarProduct = 0.0
                            }
                            if (total == 0.0) {
                                total = scalarProduct
                            } else {
                                val nextIndex = index + 1
                                temporary = Mat.hypot(
                                    scalarProduct,
                                    total,
                                )
                                val alpha = scalarProduct / temporary
                                val beta = total / temporary
                                total = temporary
                                for (row in 1..variableCount) {
                                    temporary =
                                        alpha * z[row][index] +
                                            beta * z[row][nextIndex]
                                    z[row][nextIndex] =
                                        alpha * z[row][nextIndex] -
                                        beta * z[row][index]
                                    z[row][index] = temporary
                                }
                            }
                            index -= 1
                        }

                        if (total == 0.0) {
                            ratio = -1.0
                            index = activeCount
                            do {
                                var zDotVector = 0.0
                                var absoluteZDotVector = 0.0
                                for (row in 1..variableCount) {
                                    temporary =
                                        z[row][index] *
                                        newDisplacement[row]
                                    zDotVector += temporary
                                    absoluteZDotVector += abs(temporary)
                                }
                                val accuracyA =
                                    absoluteZDotVector +
                                        0.1 * abs(zDotVector)
                                val accuracyB =
                                    absoluteZDotVector +
                                        0.2 * abs(zDotVector)
                                if (
                                    absoluteZDotVector < accuracyA &&
                                    accuracyA < accuracyB
                                ) {
                                    temporary =
                                        zDotVector / zDotActive[index]
                                    if (
                                        temporary > 0.0 &&
                                        activeIndices[index] <=
                                        constraintCount
                                    ) {
                                        val candidate =
                                            currentMultipliers[index] /
                                                temporary
                                        if (
                                            ratio < 0.0 ||
                                            candidate < ratio
                                        ) {
                                            ratio = candidate
                                            icon = index
                                        }
                                    }
                                    if (index >= 2) {
                                        val constraintIndex =
                                            activeIndices[index]
                                        for (row in 1..variableCount) {
                                            newDisplacement[row] -=
                                                temporary *
                                                approximations[
                                                    row
                                                ][constraintIndex]
                                        }
                                    }
                                    newMultipliers[index] = temporary
                                } else {
                                    newMultipliers[index] = 0.0
                                }
                            } while (--index > 0)
                            if (ratio < 0.0) {
                                break@activeSetLoop
                            }

                            for (active in 1..activeCount) {
                                currentMultipliers[active] = max(
                                    0.0,
                                    currentMultipliers[active] -
                                        ratio * newMultipliers[active],
                                )
                            }
                            if (icon < activeCount) {
                                val savedIndex = activeIndices[icon]
                                val savedMultiplier =
                                    currentMultipliers[icon]
                                index = icon
                                do {
                                    val nextIndex = index + 1
                                    val constraintIndex =
                                        activeIndices[nextIndex]
                                    val scalarProduct =
                                        dotProductColumns(
                                            z,
                                            index,
                                            approximations,
                                            constraintIndex,
                                            1,
                                            variableCount,
                                        )
                                    temporary = Mat.hypot(
                                        scalarProduct,
                                        zDotActive[nextIndex],
                                    )
                                    val alpha =
                                        zDotActive[nextIndex] / temporary
                                    val beta = scalarProduct / temporary
                                    zDotActive[nextIndex] =
                                        alpha * zDotActive[index]
                                    zDotActive[index] = temporary
                                    for (row in 1..variableCount) {
                                        temporary =
                                            alpha * z[row][nextIndex] +
                                                beta * z[row][index]
                                        z[row][nextIndex] =
                                            alpha * z[row][index] -
                                            beta * z[row][nextIndex]
                                        z[row][index] = temporary
                                    }
                                    activeIndices[index] =
                                        constraintIndex
                                    currentMultipliers[index] =
                                        currentMultipliers[nextIndex]
                                    index = nextIndex
                                } while (index < activeCount)
                                activeIndices[index] = savedIndex
                                currentMultipliers[index] =
                                    savedMultiplier
                            }
                            temporary = dotProductColumns(
                                z,
                                activeCount,
                                approximations,
                                activeIndex,
                                1,
                                variableCount,
                            )
                            if (temporary == 0.0) {
                                break@activeSetLoop
                            }
                            zDotActive[activeCount] = temporary
                            currentMultipliers[icon] = 0.0
                            currentMultipliers[activeCount] = ratio
                        } else {
                            activeCount += 1
                            zDotActive[activeCount] = total
                            currentMultipliers[icon] =
                                currentMultipliers[activeCount]
                            currentMultipliers[activeCount] = 0.0
                        }

                        activeIndices[icon] = activeIndices[activeCount]
                        activeIndices[activeCount] = activeIndex
                        if (
                            mcon > constraintCount &&
                            activeIndex != mcon
                        ) {
                            val previousIndex = activeCount - 1
                            val scalarProduct = dotProductColumns(
                                z,
                                previousIndex,
                                approximations,
                                activeIndex,
                                1,
                                variableCount,
                            )
                            temporary =
                                Mat.hypot(
                                    scalarProduct,
                                    zDotActive[activeCount],
                                )
                            val alpha =
                                zDotActive[activeCount] / temporary
                            val beta = scalarProduct / temporary
                            zDotActive[activeCount] =
                                alpha * zDotActive[previousIndex]
                            zDotActive[previousIndex] = temporary
                            for (row in 1..variableCount) {
                                temporary =
                                    alpha * z[row][activeCount] +
                                        beta * z[row][previousIndex]
                                z[row][activeCount] =
                                    alpha * z[row][previousIndex] -
                                        beta * z[row][activeCount]
                                z[row][previousIndex] = temporary
                            }
                            activeIndices[activeCount] =
                                activeIndices[previousIndex]
                            activeIndices[previousIndex] = activeIndex
                            temporary =
                                currentMultipliers[previousIndex]
                            currentMultipliers[previousIndex] =
                                currentMultipliers[activeCount]
                            currentMultipliers[activeCount] = temporary
                        }

                        if (mcon > constraintCount) {
                            temporary = 1.0 / zDotActive[activeCount]
                            for (indexInDirection in 1..variableCount) {
                                searchDirection[indexInDirection] =
                                    temporary *
                                    z[indexInDirection][activeCount]
                            }
                        } else {
                            val constraintIndex =
                                activeIndices[activeCount]
                            temporary =
                                (
                                    dotProductRowColumn(
                                        searchDirection,
                                        approximations,
                                        constraintIndex,
                                        1,
                                        variableCount,
                                    ) - 1.0
                                ) / zDotActive[activeCount]
                            for (indexInDirection in 1..variableCount) {
                                searchDirection[indexInDirection] -=
                                    temporary *
                                    z[indexInDirection][activeCount]
                            }
                        }
                    }

                    var remainingSquaredLength = rho * rho
                    var displacementDirectionProduct = 0.0
                    var directionSquaredLength = 0.0
                    for (index in 1..variableCount) {
                        if (abs(displacement[index]) >= 1.0e-6 * rho) {
                            remainingSquaredLength -=
                                displacement[index] * displacement[index]
                        }
                        displacementDirectionProduct +=
                            displacement[index] * searchDirection[index]
                        directionSquaredLength +=
                            searchDirection[index] *
                                searchDirection[index]
                    }
                    if (remainingSquaredLength <= 0.0) {
                        break@activeSetLoop
                    }
                    temporary =
                        sqrt(
                            directionSquaredLength *
                                remainingSquaredLength,
                        )
                    if (
                        abs(displacementDirectionProduct) >=
                        1.0e-6 * temporary
                    ) {
                        temporary = sqrt(
                            directionSquaredLength *
                                remainingSquaredLength +
                                displacementDirectionProduct *
                                displacementDirectionProduct,
                        )
                    }
                    fullStep =
                        remainingSquaredLength /
                        (temporary + displacementDirectionProduct)
                    step = fullStep
                    if (mcon == constraintCount) {
                        val accuracyA = step + 0.1 * maximumResidual
                        val accuracyB = step + 0.2 * maximumResidual
                        if (step >= accuracyA || accuracyA >= accuracyB) {
                            break@searchLoop
                        }
                        step = min(step, maximumResidual)
                    }

                    for (index in 1..variableCount) {
                        newDisplacement[index] =
                            displacement[index] +
                                step * searchDirection[index]
                    }
                    if (mcon == constraintCount) {
                        oldResidual = maximumResidual
                        maximumResidual = 0.0
                        for (active in 1..activeCount) {
                            val constraintIndex = activeIndices[active]
                            temporary =
                                bounds[constraintIndex] -
                                    dotProductRowColumn(
                                        newDisplacement,
                                        approximations,
                                        constraintIndex,
                                        1,
                                        variableCount,
                                    )
                            maximumResidual =
                                max(maximumResidual, temporary)
                        }
                    }

                    var index = activeCount
                    do {
                        var zDotWork = 0.0
                        var absoluteZDotWork = 0.0
                        for (row in 1..variableCount) {
                            temporary =
                                z[row][index] * newDisplacement[row]
                            zDotWork += temporary
                            absoluteZDotWork += abs(temporary)
                        }
                        val accuracyA =
                            absoluteZDotWork + 0.1 * abs(zDotWork)
                        val accuracyB =
                            absoluteZDotWork + 0.2 * abs(zDotWork)
                        if (
                            absoluteZDotWork >= accuracyA ||
                            accuracyA >= accuracyB
                        ) {
                            zDotWork = 0.0
                        }
                        newMultipliers[index] =
                            zDotWork / zDotActive[index]
                        if (index >= 2) {
                            val constraintIndex = activeIndices[index]
                            for (row in 1..variableCount) {
                                newDisplacement[row] -=
                                    newMultipliers[index] *
                                    approximations[row][constraintIndex]
                            }
                        }
                    } while (index-- >= 2)
                    if (mcon > constraintCount) {
                        newMultipliers[activeCount] =
                            max(0.0, newMultipliers[activeCount])
                    }

                    for (variable in 1..variableCount) {
                        newDisplacement[variable] =
                            displacement[variable] +
                                step * searchDirection[variable]
                    }
                    if (mcon > activeCount) {
                        for (candidate in activeCount + 1..mcon) {
                            val constraintIndex = activeIndices[candidate]
                            var residual =
                                maximumResidual -
                                    bounds[constraintIndex]
                            var absoluteResidual =
                                maximumResidual +
                                    abs(bounds[constraintIndex])
                            for (row in 1..variableCount) {
                                temporary =
                                    approximations[
                                        row
                                    ][constraintIndex] *
                                    newDisplacement[row]
                                residual += temporary
                                absoluteResidual += abs(temporary)
                            }
                            val accuracyA =
                                absoluteResidual +
                                    0.1 * abs(residual)
                            val accuracyB =
                                absoluteResidual +
                                    0.2 * abs(residual)
                            if (
                                absoluteResidual >= accuracyA ||
                                accuracyA >= accuracyB
                            ) {
                                residual = 0.0
                            }
                            newMultipliers[candidate] = residual
                        }
                    }

                    ratio = 1.0
                    icon = 0
                    for (candidate in 1..mcon) {
                        if (newMultipliers[candidate] < 0.0) {
                            temporary =
                                currentMultipliers[candidate] /
                                (
                                    currentMultipliers[candidate] -
                                        newMultipliers[candidate]
                                )
                            if (temporary < ratio) {
                                ratio = temporary
                                icon = candidate
                            }
                        }
                    }

                    temporary = 1.0 - ratio
                    for (indexInStep in 1..variableCount) {
                        displacement[indexInStep] =
                            temporary * displacement[indexInStep] +
                                ratio * newDisplacement[indexInStep]
                    }
                    for (candidate in 1..mcon) {
                        currentMultipliers[candidate] = max(
                            0.0,
                            temporary * currentMultipliers[candidate] +
                                ratio * newMultipliers[candidate],
                        )
                    }
                    if (mcon == constraintCount) {
                        maximumResidual =
                            oldResidual +
                            ratio * (maximumResidual - oldResidual)
                    }
                    if (icon <= 0) {
                        break@searchLoop
                    }
                }

                if (step == fullStep) {
                    return true
                }
            }

            if (mcon != constraintCount) {
                break@stageLoop
            }
        }
        return false
    }

    private fun printIterationResult(
        numberOfEvaluations: Int,
        objective: Double,
        maximumResidual: Double,
        variables: DoubleArray,
        variableCount: Int,
        printLevel: Int,
        logger: ((String) -> Unit)?,
    ) {
        if (printLevel > 1) {
            logger?.invoke(
                "NFVALS = $numberOfEvaluations F = $objective " +
                    "MAXCV = $maximumResidual",
            )
            logger?.invoke(
                "X = " +
                    variables
                        .copyOfRange(1, variableCount + 1)
                        .joinToString(","),
            )
        }
    }

    private fun matrix(
        rows: Int,
        columns: Int,
    ): Array<DoubleArray> =
        Array(rows) { DoubleArray(columns) }

    private fun copy(
        source: DoubleArray,
        sourceStart: Int,
        destination: DoubleArray,
        destinationStart: Int,
        count: Int,
    ) {
        for (index in 0 until count) {
            destination[index + destinationStart] =
                source[index + sourceStart]
        }
    }

    private fun column(
        source: Array<DoubleArray>,
        column: Int,
    ): DoubleArray =
        DoubleArray(source.size) { row -> source[row][column] }

    private fun dotProductRowColumn(
        left: Array<DoubleArray>,
        row: Int,
        right: Array<DoubleArray>,
        column: Int,
        start: Int,
        end: Int,
    ): Double {
        var sum = 0.0
        for (index in start..end) {
            sum += left[row][index] * right[index][column]
        }
        return sum
    }

    private fun dotProductRowColumn(
        left: DoubleArray,
        right: Array<DoubleArray>,
        column: Int,
        start: Int,
        end: Int,
    ): Double {
        var sum = 0.0
        for (index in start..end) {
            sum += left[index] * right[index][column]
        }
        return sum
    }

    private fun dotProductRowVector(
        left: Array<DoubleArray>,
        row: Int,
        right: DoubleArray,
        start: Int,
        end: Int,
    ): Double {
        var sum = 0.0
        for (index in start..end) {
            sum += left[row][index] * right[index]
        }
        return sum
    }

    private fun dotProductColumns(
        left: Array<DoubleArray>,
        leftColumn: Int,
        right: Array<DoubleArray>,
        rightColumn: Int,
        start: Int,
        end: Int,
    ): Double {
        var sum = 0.0
        for (index in start..end) {
            sum += left[index][leftColumn] * right[index][rightColumn]
        }
        return sum
    }

    private fun interface InternalCalculation<E> {
        fun evaluate(
            variableCount: Int,
            constraintCount: Int,
            variables: DoubleArray,
            constraints: DoubleArray,
        ): GMResult<Double, NlpError<E>>
    }
}
