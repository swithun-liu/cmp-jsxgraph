/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/dump.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

internal data class DumpLimits(
    val maxDepth: Int = 64,
    val maxValues: Int = 100_000,
    val maxOutputLength: Int = 1_000_000,
)

internal sealed interface DumpError {
    data class InvalidLimit(
        val name: String,
        val value: Int,
    ) : DumpError

    data class DepthLimitExceeded(
        val limit: Int,
    ) : DumpError

    data class ValueLimitExceeded(
        val limit: Int,
    ) : DumpError

    data class OutputLimitExceeded(
        val limit: Int,
    ) : DumpError

    data class InvalidObjectKey(
        val key: String,
    ) : DumpError

    data object UnsupportedValue : DumpError
}

/**
 * Typed marker for JavaScript's `undefined` value.
 */
internal data object DumpUndefined

/**
 * Source-mapped pure serialization kernel from `JXG.Dump`.
 *
 * Board traversal and attribute minimization remain separate because the
 * translated element model does not yet expose a complete upstream-compatible
 * visual-property snapshot.
 */
internal object Dump {
    // JSXGraph 1.13.3: src/utils/dump.js -> str.
    fun str(value: Any?): Any? =
        if (
            value is String &&
            value.take(FUNCTION_PREFIX_LENGTH) != FUNCTION_PREFIX
        ) {
            "\"$value\""
        } else {
            value
        }

    // JSXGraph 1.13.3: src/utils/dump.js -> toJCAN.
    fun toJCAN(
        value: Any?,
        limits: DumpLimits = DumpLimits(),
    ): GMResult<String, DumpError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        val state = DumpState(limits)
        return when (val result = appendJCAN(value, state, depth = 0)) {
            is GMResult.Ok -> GMResult.Ok(state.output.toString())
            is GMResult.Err -> result
        }
    }

    // JSXGraph 1.13.3: src/utils/dump.js -> arrayToParamStr.
    fun arrayToParamStr(
        values: List<Any?>,
        limits: DumpLimits = DumpLimits(),
    ): GMResult<String, DumpError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        val state = DumpState(limits)
        for (index in values.indices) {
            if (index > 0) {
                append(", ", state)?.let { return GMResult.Err(it) }
            }
            when (val result = appendJCAN(values[index], state, depth = 0)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(state.output.toString())
    }

    private fun appendJCAN(
        value: Any?,
        state: DumpState,
        depth: Int,
    ): GMResult<Unit, DumpError> {
        if (depth > state.limits.maxDepth) {
            return GMResult.Err(
                DumpError.DepthLimitExceeded(state.limits.maxDepth),
            )
        }
        state.valueCount += 1
        if (state.valueCount > state.limits.maxValues) {
            return GMResult.Err(
                DumpError.ValueLimitExceeded(state.limits.maxValues),
            )
        }

        return when (value) {
            null, JsonNull -> appendResult("null", state)
            DumpUndefined -> appendResult("undefined", state)
            is String -> appendResult(quoteString(value), state)
            is Boolean -> appendResult(value.toString(), state)
            is Number -> appendResult(formatNumber(value), state)
            is JsonPrimitive -> appendJsonPrimitive(value, state)
            is JsonArray -> appendList(value, state, depth)
            is JsonObject -> appendObject(value, state, depth)
            is List<*> -> appendList(value, state, depth)
            is Array<*> -> appendList(value.asList(), state, depth)
            is BooleanArray ->
                appendList(value.map { item -> item }, state, depth)
            is ByteArray ->
                appendList(value.map { item -> item }, state, depth)
            is ShortArray ->
                appendList(value.map { item -> item }, state, depth)
            is IntArray ->
                appendList(value.map { item -> item }, state, depth)
            is LongArray ->
                appendList(value.map { item -> item }, state, depth)
            is FloatArray ->
                appendList(value.map { item -> item }, state, depth)
            is DoubleArray ->
                appendList(value.map { item -> item }, state, depth)
            is Map<*, *> -> appendObject(value, state, depth)
            else -> GMResult.Err(DumpError.UnsupportedValue)
        }
    }

    private fun appendJsonPrimitive(
        value: JsonPrimitive,
        state: DumpState,
    ): GMResult<Unit, DumpError> {
        if (value.isString) {
            return appendResult(quoteString(value.content), state)
        }
        value.booleanOrNull?.let {
            return appendResult(it.toString(), state)
        }
        value.doubleOrNull?.let {
            return appendResult(JsNumberFormat.compact(it), state)
        }
        return appendResult(value.content, state)
    }

    private fun appendList(
        values: List<*>,
        state: DumpState,
        depth: Int,
    ): GMResult<Unit, DumpError> {
        append("[", state)?.let { return GMResult.Err(it) }
        for (index in values.indices) {
            if (index > 0) {
                append(",", state)?.let { return GMResult.Err(it) }
            }
            when (
                val result = appendJCAN(
                    value = values[index],
                    state = state,
                    depth = depth + 1,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return appendResult("]", state)
    }

    private fun appendObject(
        values: Map<*, *>,
        state: DumpState,
        depth: Int,
    ): GMResult<Unit, DumpError> {
        val properties = mutableListOf<Pair<String, Any?>>()
        for ((key, value) in values) {
            if (key !is String) {
                return GMResult.Err(
                    DumpError.InvalidObjectKey(key.toString()),
                )
            }
            properties += key to value
        }
        val ordered = properties.sortedWith(JS_PROPERTY_ORDER)

        append("<<", state)?.let { return GMResult.Err(it) }
        for (index in ordered.indices) {
            if (index > 0) {
                append(", ", state)?.let { return GMResult.Err(it) }
            }
            append(ordered[index].first, state)?.let {
                return GMResult.Err(it)
            }
            append(": ", state)?.let { return GMResult.Err(it) }
            when (
                val result = appendJCAN(
                    value = ordered[index].second,
                    state = state,
                    depth = depth + 1,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return appendResult(">> ", state)
    }

    private fun appendResult(
        value: String,
        state: DumpState,
    ): GMResult<Unit, DumpError> =
        append(value, state)?.let { GMResult.Err(it) } ?: GMResult.Ok(Unit)

    private fun append(
        value: String,
        state: DumpState,
    ): DumpError.OutputLimitExceeded? {
        if (
            state.output.length >
            state.limits.maxOutputLength - value.length
        ) {
            return DumpError.OutputLimitExceeded(
                state.limits.maxOutputLength,
            )
        }
        state.output.append(value)
        return null
    }

    private fun quoteString(value: String): String = buildString {
        append('\'')
        for (character in value) {
            when (character) {
                '\\' -> append("\\\\")
                '\'', '"' -> {
                    append('\\')
                    append(character)
                }
                else -> append(character)
            }
        }
        append('\'')
    }

    private fun formatNumber(value: Number): String =
        JsNumberFormat.compact(value.toDouble())

    private fun validateLimits(
        limits: DumpLimits,
    ): DumpError.InvalidLimit? =
        when {
            limits.maxDepth < 0 ->
                DumpError.InvalidLimit("maxDepth", limits.maxDepth)
            limits.maxValues <= 0 ->
                DumpError.InvalidLimit("maxValues", limits.maxValues)
            limits.maxOutputLength <= 0 ->
                DumpError.InvalidLimit(
                    "maxOutputLength",
                    limits.maxOutputLength,
                )
            else -> null
        }

    private class DumpState(
        val limits: DumpLimits,
    ) {
        val output = StringBuilder()
        var valueCount: Int = 0
    }

    private const val FUNCTION_PREFIX = "function"
    private const val FUNCTION_PREFIX_LENGTH = 7

    private val JS_PROPERTY_ORDER =
        Comparator<Pair<String, Any?>> { first, second ->
            val firstIndex = first.first.toJsArrayIndexOrNull()
            val secondIndex = second.first.toJsArrayIndexOrNull()
            when {
                firstIndex != null && secondIndex != null ->
                    firstIndex.compareTo(secondIndex)
                firstIndex != null -> -1
                secondIndex != null -> 1
                else -> 0
            }
        }

    private fun String.toJsArrayIndexOrNull(): Long? {
        if (isEmpty() || (length > 1 && first() == '0')) {
            return null
        }
        val value = toLongOrNull() ?: return null
        return if (
            value in 0 until MAX_JS_ARRAY_INDEX &&
            value.toString() == this
        ) {
            value
        } else {
            null
        }
    }

    private const val MAX_JS_ARRAY_INDEX = 4_294_967_295L
}
