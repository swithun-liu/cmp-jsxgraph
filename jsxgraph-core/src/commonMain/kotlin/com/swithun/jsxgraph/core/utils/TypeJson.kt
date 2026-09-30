/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/type.js
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

/**
 * Bounded serializer for the deprecated `JXG.Type.toJSON` utility.
 */
internal object TypeJson {
    // JSXGraph 1.13.3: src/utils/type.js -> toJSON.
    fun serialize(
        value: Any?,
        noQuote: Boolean,
        limits: TypeJsonLimits,
    ): GMResult<TypeJsonValue, TypeError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        val state = JsonState(limits)
        return when (
            val result = serializeValue(
                value = value,
                noQuote = noQuote,
                position = JsonPosition.Root,
                depth = 0,
                state = state,
            )
        ) {
            is GMResult.Ok ->
                when (val fragment = result.value) {
                    is JsonFragment.Value ->
                        GMResult.Ok(
                            TypeJsonValue.Serialized(fragment.value),
                        )
                    JsonFragment.Omitted ->
                        GMResult.Ok(TypeJsonValue.Undefined)
                }
            is GMResult.Err -> result
        }
    }

    private fun serializeValue(
        value: Any?,
        noQuote: Boolean,
        position: JsonPosition,
        depth: Int,
        state: JsonState,
    ): GMResult<JsonFragment, TypeError> {
        visit(depth, state)?.let { return GMResult.Err(it) }
        return when (value) {
            null, JsonNull -> valueFragment("null", state)
            DumpUndefined -> unsupportedFragment(noQuote, position, state)
            is Function<*> ->
                unsupportedFragment(noQuote, position, state)
            is String ->
                valueFragment(
                    if (noQuote) {
                        quoteNoQuoteString(value)
                    } else {
                        JsonPrimitive(value).toString()
                    },
                    state,
                )
            is Boolean -> valueFragment(value.toString(), state)
            is Number ->
                valueFragment(
                    serializeNumber(value.toDouble(), noQuote),
                    state,
                )
            is JsonPrimitive ->
                serializeJsonPrimitive(value, noQuote, state)
            is JsonArray ->
                serializeArray(
                    identity = value,
                    values = value,
                    noQuote = noQuote,
                    depth = depth,
                    state = state,
                )
            is JsonObject ->
                serializeObject(
                    identity = value,
                    values = value,
                    noQuote = noQuote,
                    depth = depth,
                    state = state,
                )
            is List<*> ->
                serializeArray(
                    identity = value,
                    values = value,
                    noQuote = noQuote,
                    depth = depth,
                    state = state,
                )
            is Array<*> ->
                serializeArray(
                    identity = value,
                    values = value.asList(),
                    noQuote = noQuote,
                    depth = depth,
                    state = state,
                )
            is BooleanArray ->
                serializeArray(
                    identity = value,
                    values = value.map { item -> item },
                    noQuote = noQuote,
                    depth = depth,
                    state = state,
                )
            is ByteArray ->
                serializeArray(
                    identity = value,
                    values = value.map { item -> item },
                    noQuote = noQuote,
                    depth = depth,
                    state = state,
                )
            is ShortArray ->
                serializeArray(
                    identity = value,
                    values = value.map { item -> item },
                    noQuote = noQuote,
                    depth = depth,
                    state = state,
                )
            is IntArray ->
                serializeArray(
                    identity = value,
                    values = value.map { item -> item },
                    noQuote = noQuote,
                    depth = depth,
                    state = state,
                )
            is LongArray ->
                serializeArray(
                    identity = value,
                    values = value.map { item -> item },
                    noQuote = noQuote,
                    depth = depth,
                    state = state,
                )
            is FloatArray ->
                serializeArray(
                    identity = value,
                    values = value.map { item -> item },
                    noQuote = noQuote,
                    depth = depth,
                    state = state,
                )
            is DoubleArray ->
                serializeArray(
                    identity = value,
                    values = value.map { item -> item },
                    noQuote = noQuote,
                    depth = depth,
                    state = state,
                )
            is Map<*, *> ->
                serializeObject(
                    identity = value,
                    values = value,
                    noQuote = noQuote,
                    depth = depth,
                    state = state,
                )
            else -> GMResult.Err(TypeError.UnsupportedJsonValue(value))
        }
    }

    private fun serializeJsonPrimitive(
        value: JsonPrimitive,
        noQuote: Boolean,
        state: JsonState,
    ): GMResult<JsonFragment, TypeError> =
        valueFragment(
            value = if (value.isString && noQuote) {
                quoteNoQuoteString(value.content)
            } else {
                value.toString()
            },
            state = state,
        )

    private fun serializeArray(
        identity: Any,
        values: List<*>,
        noQuote: Boolean,
        depth: Int,
        state: JsonState,
    ): GMResult<JsonFragment, TypeError> =
        withContainer(identity, state) {
            val serialized = mutableListOf<String>()
            for (value in values) {
                when (
                    val result = serializeValue(
                        value = value,
                        noQuote = noQuote,
                        position = JsonPosition.Array,
                        depth = depth + 1,
                        state = state,
                    )
                ) {
                    is GMResult.Ok -> {
                        serialized += when (val fragment = result.value) {
                            is JsonFragment.Value -> fragment.value
                            JsonFragment.Omitted -> "null"
                        }
                    }
                    is GMResult.Err -> return@withContainer result
                }
            }
            valueFragment(
                value = "[" + serialized.joinToString(",") + "]",
                state = state,
            )
        }

    private fun serializeObject(
        identity: Any,
        values: Map<*, *>,
        noQuote: Boolean,
        depth: Int,
        state: JsonState,
    ): GMResult<JsonFragment, TypeError> =
        withContainer(identity, state) {
            val typed = linkedMapOf<String, Any?>()
            for ((key, value) in values) {
                if (key !is String) {
                    return@withContainer GMResult.Err(
                        TypeError.InvalidJsonObjectKey(key),
                    )
                }
                typed[key] = value
            }

            val serialized = mutableListOf<String>()
            for (property in Type.keys(typed)) {
                when (
                    val result = serializeValue(
                        value = typed[property],
                        noQuote = noQuote,
                        position = JsonPosition.ObjectProperty,
                        depth = depth + 1,
                        state = state,
                    )
                ) {
                    is GMResult.Ok -> {
                        val fragment = result.value
                        if (fragment is JsonFragment.Value) {
                            val key = if (noQuote) {
                                property
                            } else {
                                JsonPrimitive(property).toString()
                            }
                            serialized += "$key:${fragment.value}"
                        }
                    }
                    is GMResult.Err -> return@withContainer result
                }
            }
            valueFragment(
                value = "{" +
                    serialized.joinToString(",") +
                    "}" +
                    if (noQuote) " " else "",
                state = state,
            )
        }

    private fun unsupportedFragment(
        noQuote: Boolean,
        position: JsonPosition,
        state: JsonState,
    ): GMResult<JsonFragment, TypeError> =
        when {
            noQuote -> valueFragment("0", state)
            position == JsonPosition.Array -> valueFragment("null", state)
            else -> GMResult.Ok(JsonFragment.Omitted)
        }

    private fun serializeNumber(
        value: Double,
        noQuote: Boolean,
    ): String =
        if (!noQuote && !value.isFinite()) {
            "null"
        } else {
            Type.formatJsNumber(value)
        }

    private fun quoteNoQuoteString(value: String): String = buildString {
        append('\'')
        for (character in value) {
            if (character == '"' || character == '\'') {
                append('\\')
            }
            append(character)
        }
        append('\'')
    }

    private fun valueFragment(
        value: String,
        state: JsonState,
    ): GMResult<JsonFragment, TypeError> =
        if (value.length > state.limits.maxOutputLength) {
            GMResult.Err(
                TypeError.JsonOutputLimitExceeded(
                    state.limits.maxOutputLength,
                ),
            )
        } else {
            GMResult.Ok(JsonFragment.Value(value))
        }

    private fun visit(
        depth: Int,
        state: JsonState,
    ): TypeError? {
        if (depth > state.limits.maxDepth) {
            return TypeError.JsonDepthLimitExceeded(
                state.limits.maxDepth,
            )
        }
        state.valueCount += 1
        if (state.valueCount > state.limits.maxValues) {
            return TypeError.JsonValueLimitExceeded(
                state.limits.maxValues,
            )
        }
        return null
    }

    private inline fun <T> withContainer(
        value: Any,
        state: JsonState,
        block: () -> GMResult<T, TypeError>,
    ): GMResult<T, TypeError> {
        if (state.activeContainers.any { active -> active === value }) {
            return GMResult.Err(TypeError.CyclicJsonValue)
        }
        state.activeContainers += value
        val result = block()
        state.activeContainers.removeAt(state.activeContainers.lastIndex)
        return result
    }

    private fun validateLimits(
        limits: TypeJsonLimits,
    ): TypeError.InvalidJsonLimit? =
        when {
            limits.maxDepth < 0 ->
                TypeError.InvalidJsonLimit(
                    "maxDepth",
                    limits.maxDepth,
                )
            limits.maxValues <= 0 ->
                TypeError.InvalidJsonLimit(
                    "maxValues",
                    limits.maxValues,
                )
            limits.maxOutputLength <= 0 ->
                TypeError.InvalidJsonLimit(
                    "maxOutputLength",
                    limits.maxOutputLength,
                )
            else -> null
        }

    private sealed interface JsonFragment {
        data class Value(
            val value: String,
        ) : JsonFragment

        data object Omitted : JsonFragment
    }

    private enum class JsonPosition {
        Root,
        Array,
        ObjectProperty,
    }

    private class JsonState(
        val limits: TypeJsonLimits,
    ) {
        val activeContainers = mutableListOf<Any>()
        var valueCount: Int = 0
    }
}
