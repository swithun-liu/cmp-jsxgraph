/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/type.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult

/**
 * Bounded recursive evaluator for `JXG.Type.evaluate`.
 */
internal object TypeEvaluate {
    // JSXGraph 1.13.3: src/utils/type.js -> evaluate.
    fun evaluate(
        value: Any?,
        limits: TypeEvaluationLimits,
    ): GMResult<Any?, TypeError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        return evaluateValue(
            value = value,
            depth = 0,
            state = EvaluationState(limits),
        )
    }

    private fun evaluateValue(
        value: Any?,
        depth: Int,
        state: EvaluationState,
    ): GMResult<Any?, TypeError> {
        visit(depth, state)?.let { return GMResult.Err(it) }
        if (value is Function0<*>) {
            return try {
                GMResult.Ok(value())
            } catch (exception: Exception) {
                GMResult.Err(
                    TypeError.EvaluationFailed(
                        exception.message
                            ?: exception::class.simpleName.orEmpty(),
                    ),
                )
            }
        }
        if (value is Function<*>) {
            return GMResult.Err(
                TypeError.UnsupportedEvaluationFunction(value),
            )
        }

        val array = arrayValue(value) ?: return GMResult.Ok(value)
        return withArray(array.identity, state) {
            val result = mutableListOf<Any?>()
            for (entry in array.values) {
                when (
                    val evaluated = evaluateValue(
                        value = entry,
                        depth = depth + 1,
                        state = state,
                    )
                ) {
                    is GMResult.Ok -> result += evaluated.value
                    is GMResult.Err -> return@withArray evaluated
                }
            }
            GMResult.Ok(result)
        }
    }

    private fun arrayValue(value: Any?): ArrayValue? =
        when (value) {
            is List<*> ->
                ArrayValue(value, value.map { item -> item })
            is Array<*> ->
                ArrayValue(value, value.map { item -> item })
            is BooleanArray ->
                ArrayValue(value, value.map { item -> item })
            is ByteArray ->
                ArrayValue(value, value.map { item -> item })
            is ShortArray ->
                ArrayValue(value, value.map { item -> item })
            is IntArray ->
                ArrayValue(value, value.map { item -> item })
            is LongArray ->
                ArrayValue(value, value.map { item -> item })
            is FloatArray ->
                ArrayValue(value, value.map { item -> item })
            is DoubleArray ->
                ArrayValue(value, value.map { item -> item })
            else -> null
        }

    private fun visit(
        depth: Int,
        state: EvaluationState,
    ): TypeError? {
        if (depth > state.limits.maxDepth) {
            return TypeError.EvaluationDepthLimitExceeded(
                state.limits.maxDepth,
            )
        }
        state.valueCount += 1
        if (state.valueCount > state.limits.maxValues) {
            return TypeError.EvaluationValueLimitExceeded(
                state.limits.maxValues,
            )
        }
        return null
    }

    private inline fun <T> withArray(
        value: Any,
        state: EvaluationState,
        block: () -> GMResult<T, TypeError>,
    ): GMResult<T, TypeError> {
        if (state.activeArrays.any { active -> active === value }) {
            return GMResult.Err(TypeError.CyclicEvaluationValue)
        }
        state.activeArrays += value
        val result = block()
        state.activeArrays.removeAt(state.activeArrays.lastIndex)
        return result
    }

    private fun validateLimits(
        limits: TypeEvaluationLimits,
    ): TypeError.InvalidEvaluationLimit? =
        when {
            limits.maxDepth < 0 ->
                TypeError.InvalidEvaluationLimit(
                    "maxDepth",
                    limits.maxDepth,
                )
            limits.maxValues <= 0 ->
                TypeError.InvalidEvaluationLimit(
                    "maxValues",
                    limits.maxValues,
                )
            else -> null
        }

    private class EvaluationState(
        val limits: TypeEvaluationLimits,
    ) {
        val activeArrays = mutableListOf<Any>()
        var valueCount: Int = 0
    }

    private data class ArrayValue(
        val identity: Any,
        val values: List<Any?>,
    )
}
