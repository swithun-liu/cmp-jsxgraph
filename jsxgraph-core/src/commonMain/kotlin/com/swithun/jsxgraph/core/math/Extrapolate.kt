/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/extrapolate.js -> Mat.Extrapolate
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.math.pow

enum class ExtrapolateClassification(
    val upstreamValue: String,
) {
    FINITE("finite"),
    INFINITE("infinite"),
    NAN("NaN"),
}

data class ExtrapolateResult(
    val value: Double,
    val classification: ExtrapolateClassification,
    val reliability: Double,
)

sealed interface ExtrapolateError<out E> {
    data class InvalidIndex(
        val index: Int,
    ) : ExtrapolateError<Nothing>

    data class InvalidUpper(
        val value: Int,
    ) : ExtrapolateError<Nothing>

    data class UnsupportedMethod(
        val method: String,
    ) : ExtrapolateError<Nothing>

    data class Evaluation<E>(
        val cause: E,
    ) : ExtrapolateError<E>

    data class EvaluationException(
        val message: String,
    ) : ExtrapolateError<Nothing>
}

fun interface ExtrapolateFunction<E> {
    fun evaluate(
        x: Double,
        suspendedUpdate: Boolean,
    ): GMResult<Double, E>
}

/**
 * Sequence transformations used by JSXGraph's adaptive curve plotting.
 *
 * Mutable lists stand in for JavaScript arrays. Missing entries are expanded
 * with [Double.NaN] so sparse-array arithmetic keeps the upstream propagation
 * behavior.
 */
object Extrapolate {
    var upper: Int = 15
    var infty: Double = 1.0e4

    private const val HUGE: Double = 1.0e20
    private const val TINY: Double = 1.0e-15
    private const val CONVERGENCE_THRESHOLD: Double = 1.0e-7
    private const val GEOMETRIC_STEP_RATIO: Double = 0.5

    // JSXGraph: src/math/extrapolate.js -> wynnEps.
    fun wynnEps(
        sequenceValue: Double,
        index: Int,
        state: MutableList<Double>,
    ): GMResult<Double, ExtrapolateError<Nothing>> {
        if (index < 0) {
            return GMResult.Err(ExtrapolateError.InvalidIndex(index))
        }

        writeState(state, index, sequenceValue)
        if (index == 0) {
            return GMResult.Ok(sequenceValue)
        }

        var aux2 = 0.0
        val initialFactor = 1.0
        for (stateIndex in index downTo 1) {
            val aux1 = aux2
            aux2 = state[stateIndex - 1]
            val difference = state[stateIndex] - aux2
            state[stateIndex - 1] = if (abs(difference) <= TINY) {
                HUGE
            } else {
                val factor =
                    if ((index - stateIndex + 1) % 2 == 1) initialFactor else 1.0
                aux1 * factor + 1.0 / difference
            }
        }
        return GMResult.Ok(state[index % 2])
    }

    // JSXGraph: src/math/extrapolate.js -> aitken.
    fun aitken(
        sequenceValue: Double,
        index: Int,
        state: MutableList<Double>,
    ): GMResult<Double, ExtrapolateError<Nothing>> {
        if (index < 0) {
            return GMResult.Err(ExtrapolateError.InvalidIndex(index))
        }

        writeState(state, index, sequenceValue)
        if (index < 2) {
            return GMResult.Ok(sequenceValue)
        }

        val lowMaximum = index / 2
        for (iteration in 1..lowMaximum) {
            val stateIndex = index - 2 * iteration
            val denominator =
                state[stateIndex + 2] -
                    2.0 * state[stateIndex + 1] +
                    state[stateIndex]
            if (abs(denominator) < TINY) {
                state[stateIndex] = HUGE
            } else {
                val value = state[stateIndex] - state[stateIndex + 1]
                state[stateIndex] -= value * value / denominator
            }
        }
        return GMResult.Ok(state[index % 2])
    }

    // JSXGraph: src/math/extrapolate.js -> brezinski.
    fun brezinski(
        sequenceValue: Double,
        index: Int,
        state: MutableList<Double>,
    ): GMResult<Double, ExtrapolateError<Nothing>> {
        if (index < 0) {
            return GMResult.Err(ExtrapolateError.InvalidIndex(index))
        }

        writeState(state, index, sequenceValue)
        if (index < 3) {
            return GMResult.Ok(sequenceValue)
        }

        val lowMaximum = index / 3
        var stateIndex = index
        for (iteration in 1..lowMaximum) {
            stateIndex -= 3
            val delta0 = state[stateIndex + 1] - state[stateIndex]
            val delta1 = state[stateIndex + 2] - state[stateIndex + 1]
            val delta2 = state[stateIndex + 3] - state[stateIndex + 2]
            val denominator =
                delta2 * (delta1 - delta0) -
                    delta0 * (delta2 - delta1)
            state[stateIndex] = if (abs(denominator) < TINY) {
                HUGE
            } else {
                state[stateIndex + 1] -
                    delta0 * delta1 * (delta2 - delta1) / denominator
            }
        }
        return GMResult.Ok(state[index % 3])
    }

    // JSXGraph: src/math/extrapolate.js -> iteration.
    fun <E> iteration(
        x0: Double,
        initialStep: Double,
        function: ExtrapolateFunction<E>,
        method: String,
        stepType: Int = 0,
    ): GMResult<ExtrapolateResult, ExtrapolateError<E>> {
        val configuredUpper = upper
        if (configuredUpper <= 0) {
            return GMResult.Err(ExtrapolateError.InvalidUpper(configuredUpper))
        }
        if (method != "wynnEps" && method != "aitken" && method != "brezinski") {
            return GMResult.Err(ExtrapolateError.UnsupportedMethod(method))
        }

        var index = 1
        var estimate = Double.NaN
        var classification = ExtrapolateClassification.FINITE
        var step = initialStep
        val state = mutableListOf<Double>()

        while (index <= upper) {
            step = if (stepType == 0) {
                initialStep / (index + 1)
            } else {
                step * GEOMETRIC_STEP_RATIO
            }
            val value = when (
                val evaluated = evaluateSafely(
                    function = function,
                    x = x0 + step,
                )
            ) {
                is GMResult.Ok -> evaluated.value
                is GMResult.Err -> return evaluated
            }
            val transformed = when (method) {
                "wynnEps" -> wynnEps(value, index - 1, state)
                "aitken" -> aitken(value, index - 1, state)
                else -> brezinski(value, index - 1, state)
            }
            val transformedValue = when (transformed) {
                is GMResult.Ok -> transformed.value
                is GMResult.Err -> return transformed
            }

            if (transformedValue.isNaN()) {
                classification = ExtrapolateClassification.NAN
                break
            }
            if (value != 0.0 && transformedValue / value > infty) {
                estimate = transformedValue
                classification = ExtrapolateClassification.INFINITE
                break
            }
            val difference = transformedValue - estimate
            if (abs(difference) < CONVERGENCE_THRESHOLD) {
                break
            }
            estimate = transformedValue
            index += 1
        }

        return GMResult.Ok(
            ExtrapolateResult(
                value = estimate,
                classification = classification,
                reliability = 1.0 - (index - 1).toDouble() / upper,
            ),
        )
    }

    // JSXGraph: src/math/extrapolate.js -> levin.
    fun levin(
        sequenceValue: Double,
        index: Int,
        omega: Double,
        beta: Double,
        numeratorState: MutableList<Double>,
        denominatorState: MutableList<Double>,
    ): GMResult<Double, ExtrapolateError<Nothing>> {
        if (index < 0) {
            return GMResult.Err(ExtrapolateError.InvalidIndex(index))
        }

        writeState(numeratorState, index, sequenceValue / omega)
        writeState(denominatorState, index, 1.0 / omega)
        var term = 1.0 / (beta + index)
        if (index > 0) {
            numeratorState[index - 1] =
                numeratorState[index] - numeratorState[index - 1]
            denominatorState[index - 1] =
                denominatorState[index] - denominatorState[index - 1]
            if (index > 1) {
                val ratio = (beta + index - 1.0) * term
                for (iteration in 2..index) {
                    val factor =
                        (beta + index - iteration) *
                            ratio.pow(iteration - 2) *
                            term
                    numeratorState[index - iteration] =
                        numeratorState[index - iteration + 1] -
                            factor * numeratorState[index - iteration]
                    denominatorState[index - iteration] =
                        denominatorState[index - iteration + 1] -
                            factor * denominatorState[index - iteration]
                    term *= ratio
                }
            }
        }

        return GMResult.Ok(
            if (abs(denominatorState[0]) < TINY) {
                HUGE
            } else {
                numeratorState[0] / denominatorState[0]
            },
        )
    }

    // JSXGraph: src/math/extrapolate.js -> iteration_levin.
    fun <E> iterationLevin(
        x0: Double,
        initialStep: Double,
        function: ExtrapolateFunction<E>,
        stepType: Int = 0,
    ): GMResult<ExtrapolateResult, ExtrapolateError<E>> {
        val configuredUpper = upper
        if (configuredUpper <= 0) {
            return GMResult.Err(ExtrapolateError.InvalidUpper(configuredUpper))
        }

        var index = 1
        var estimate = Double.NaN
        var classification = ExtrapolateClassification.FINITE
        var step = initialStep
        val numeratorState = mutableListOf<Double>()
        val denominatorState = mutableListOf<Double>()
        var previousValue = when (
            val evaluated = evaluateSafely(
                function = function,
                x = x0 + initialStep,
            )
        ) {
            is GMResult.Ok -> evaluated.value
            is GMResult.Err -> return evaluated
        }

        while (index <= upper) {
            step = if (stepType == 0) {
                initialStep / (index + 1)
            } else {
                step * GEOMETRIC_STEP_RATIO
            }
            val value = when (
                val evaluated = evaluateSafely(
                    function = function,
                    x = x0 + step,
                )
            ) {
                is GMResult.Ok -> evaluated.value
                is GMResult.Err -> return evaluated
            }
            val delta = value - previousValue
            val omega = if (abs(delta) < 1.0) {
                (1.0 + index) * delta
            } else {
                delta
            }

            previousValue = value
            val transformedValue = when (
                val transformed = levin(
                    sequenceValue = value,
                    index = index - 1,
                    omega = omega,
                    beta = 1.0,
                    numeratorState = numeratorState,
                    denominatorState = denominatorState,
                )
            ) {
                is GMResult.Ok -> transformed.value
                is GMResult.Err -> return transformed
            }
            val difference = transformedValue - estimate

            if (transformedValue.isNaN()) {
                classification = ExtrapolateClassification.NAN
                break
            }
            if (value != 0.0 && transformedValue / value > infty) {
                estimate = transformedValue
                classification = ExtrapolateClassification.INFINITE
                break
            }
            if (abs(difference) < CONVERGENCE_THRESHOLD) {
                break
            }
            estimate = transformedValue
            index += 1
        }

        return GMResult.Ok(
            ExtrapolateResult(
                value = estimate,
                classification = classification,
                reliability = 1.0 - (index - 1).toDouble() / upper,
            ),
        )
    }

    // JSXGraph: src/math/extrapolate.js -> limit.
    fun <E> limit(
        x0: Double,
        initialStep: Double,
        function: ExtrapolateFunction<E>,
    ): GMResult<ExtrapolateResult, ExtrapolateError<E>> =
        iterationLevin(
            x0 = x0,
            initialStep = initialStep,
            function = function,
            stepType = 0,
        )

    private fun writeState(
        state: MutableList<Double>,
        index: Int,
        value: Double,
    ) {
        while (state.size <= index) {
            state.add(Double.NaN)
        }
        state[index] = value
    }

    private fun <E> evaluateSafely(
        function: ExtrapolateFunction<E>,
        x: Double,
    ): GMResult<Double, ExtrapolateError<E>> =
        try {
            when (val result = function.evaluate(x, true)) {
                is GMResult.Ok -> result
                is GMResult.Err -> GMResult.Err(
                    ExtrapolateError.Evaluation(result.error),
                )
            }
        } catch (error: Throwable) {
            GMResult.Err(
                ExtrapolateError.EvaluationException(
                    error.message ?: "Extrapolation callback failed",
                ),
            )
        }
}
