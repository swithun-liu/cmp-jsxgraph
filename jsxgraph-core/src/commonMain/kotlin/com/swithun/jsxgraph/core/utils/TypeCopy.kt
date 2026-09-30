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
 * Bounded JSON-like copy kernel for the object utilities in `JXG.Type`.
 */
internal object TypeCopy {
    // JSXGraph 1.13.3: src/utils/type.js -> deepCopy.
    fun deepCopy(
        value: Any?,
        secondary: Map<String, Any?>?,
        toLower: Boolean,
        limits: TypeCopyLimits,
    ): GMResult<Any?, TypeError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        return copyValue(
            value = value,
            secondary = secondary,
            toLower = toLower,
            depth = 0,
            state = CopyState(limits),
        )
    }

    // JSXGraph 1.13.3: src/utils/type.js -> keysToLowerCase.
    fun keysToLowerCase(
        value: Map<String, Any?>,
        limits: TypeCopyLimits,
    ): GMResult<Map<String, Any?>, TypeError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        return lowerCaseKeys(
            value = value,
            depth = 0,
            state = CopyState(limits),
        )
    }

    private fun copyValue(
        value: Any?,
        secondary: Map<*, *>?,
        toLower: Boolean,
        depth: Int,
        state: CopyState,
    ): GMResult<Any?, TypeError> {
        visit(depth, state)?.let { return GMResult.Err(it) }
        return when (value) {
            null,
            DumpUndefined,
            is String,
            is Number,
            is Boolean,
            is Function<*>,
            -> GMResult.Ok(value)
            is Map<*, *> ->
                copyMap(
                    value = value,
                    secondary = secondary,
                    toLower = toLower,
                    depth = depth,
                    state = state,
                )
            is List<*> ->
                copyList(value, toLower, depth, state)
            is Array<*> ->
                copyList(value.asList(), toLower, depth, state)
            is BooleanArray ->
                copyList(value.map { item -> item }, toLower, depth, state)
            is ByteArray ->
                copyList(value.map { item -> item }, toLower, depth, state)
            is ShortArray ->
                copyList(value.map { item -> item }, toLower, depth, state)
            is IntArray ->
                copyList(value.map { item -> item }, toLower, depth, state)
            is LongArray ->
                copyList(value.map { item -> item }, toLower, depth, state)
            is FloatArray ->
                copyList(value.map { item -> item }, toLower, depth, state)
            is DoubleArray ->
                copyList(value.map { item -> item }, toLower, depth, state)
            else -> GMResult.Err(TypeError.UnsupportedCopyValue(value))
        }
    }

    private fun copyList(
        value: List<*>,
        toLower: Boolean,
        depth: Int,
        state: CopyState,
    ): GMResult<Any?, TypeError> =
        withContainer(value, state) {
            val result = mutableListOf<Any?>()
            for (entry in value) {
                when (
                    val copied = copyPropertyValue(
                        value = entry,
                        existing = null,
                        toLower = toLower,
                        depth = depth + 1,
                        state = state,
                    )
                ) {
                    is GMResult.Ok -> result += copied.value
                    is GMResult.Err -> return@withContainer copied
                }
            }
            GMResult.Ok(result)
        }

    private fun copyMap(
        value: Map<*, *>,
        secondary: Map<*, *>?,
        toLower: Boolean,
        depth: Int,
        state: CopyState,
    ): GMResult<Any?, TypeError> =
        withContainer(value, state) {
            val result = linkedMapOf<String, Any?>()
            val primaryEntries = when (val entries = orderedEntries(value)) {
                is GMResult.Ok -> entries.value
                is GMResult.Err -> return@withContainer entries
            }
            for ((sourceKey, sourceValue) in primaryEntries) {
                val key = if (toLower) sourceKey.lowercase() else sourceKey
                when (
                    val copied = copyPropertyValue(
                        value = sourceValue,
                        existing = null,
                        toLower = toLower,
                        depth = depth + 1,
                        state = state,
                    )
                ) {
                    is GMResult.Ok -> result[key] = copied.value
                    is GMResult.Err -> return@withContainer copied
                }
            }

            if (secondary != null) {
                val secondaryEntries = when (
                    val entries = orderedEntries(secondary)
                ) {
                    is GMResult.Ok -> entries.value
                    is GMResult.Err -> return@withContainer entries
                }
                for ((sourceKey, sourceValue) in secondaryEntries) {
                    val key = if (toLower) {
                        sourceKey.lowercase()
                    } else {
                        sourceKey
                    }
                    val copied = copyPropertyValue(
                        value = sourceValue,
                        existing = result[key],
                        toLower = toLower,
                        depth = depth + 1,
                        state = state,
                    )
                    when (copied) {
                        is GMResult.Ok -> result[key] = copied.value
                        is GMResult.Err -> return@withContainer copied
                    }
                }
            }
            GMResult.Ok(result)
        }

    private fun copyPropertyValue(
        value: Any?,
        existing: Any?,
        toLower: Boolean,
        depth: Int,
        state: CopyState,
    ): GMResult<Any?, TypeError> {
        if (
            value is Map<*, *> &&
            hasExistingProperty(value, "board")
        ) {
            return GMResult.Ok(
                value.entries
                    .firstOrNull { entry -> entry.key == "id" }
                    ?.value
                    ?: DumpUndefined,
            )
        }
        return if (
            value is Map<*, *> &&
            Type.exists(existing)
        ) {
            copyValue(
                value = existing,
                secondary = value,
                toLower = toLower,
                depth = depth,
                state = state,
            )
        } else {
            copyValue(
                value = value,
                secondary = null,
                toLower = toLower,
                depth = depth,
                state = state,
            )
        }
    }

    private fun lowerCaseKeys(
        value: Map<*, *>,
        depth: Int,
        state: CopyState,
    ): GMResult<Map<String, Any?>, TypeError> {
        visit(depth, state)?.let { return GMResult.Err(it) }
        return withContainer(value, state) {
            val entries = when (val ordered = orderedEntries(value)) {
                is GMResult.Ok -> ordered.value
                is GMResult.Err -> return@withContainer ordered
            }
            val result = linkedMapOf<String, Any?>()
            for ((key, entryValue) in entries.asReversed()) {
                visit(depth + 1, state)?.let {
                    return@withContainer GMResult.Err(it)
                }
                val normalizedKey = key.lowercase()
                if (
                    entryValue is Map<*, *> &&
                    !hasExistingProperty(entryValue, "nodeType") &&
                    !hasExistingProperty(entryValue, "board")
                ) {
                    when (
                        val nested = lowerCaseKeys(
                            value = entryValue,
                            depth = depth + 1,
                            state = state,
                        )
                    ) {
                        is GMResult.Ok ->
                            result[normalizedKey] = nested.value
                        is GMResult.Err ->
                            return@withContainer nested
                    }
                } else {
                    result[normalizedKey] = entryValue
                }
            }
            GMResult.Ok(result)
        }
    }

    private fun orderedEntries(
        value: Map<*, *>,
    ): GMResult<List<Pair<String, Any?>>, TypeError> {
        val typed = linkedMapOf<String, Any?>()
        for ((key, entryValue) in value) {
            if (key !is String) {
                return GMResult.Err(
                    TypeError.InvalidCopyObjectKey(key),
                )
            }
            typed[key] = entryValue
        }
        return GMResult.Ok(
            Type.keys(typed).map { key -> key to typed[key] },
        )
    }

    private fun hasExistingProperty(
        value: Map<*, *>,
        property: String,
    ): Boolean =
        value.entries.any { (key, entryValue) ->
            key == property && Type.exists(entryValue)
        }

    private fun Any?.isArrayValue(): Boolean =
        this is List<*> ||
            this is Array<*> ||
            this is BooleanArray ||
            this is ByteArray ||
            this is ShortArray ||
            this is IntArray ||
            this is LongArray ||
            this is FloatArray ||
            this is DoubleArray

    private fun visit(
        depth: Int,
        state: CopyState,
    ): TypeError? {
        if (depth > state.limits.maxDepth) {
            return TypeError.CopyDepthLimitExceeded(
                state.limits.maxDepth,
            )
        }
        state.valueCount += 1
        if (state.valueCount > state.limits.maxValues) {
            return TypeError.CopyValueLimitExceeded(
                state.limits.maxValues,
            )
        }
        return null
    }

    private inline fun <T> withContainer(
        value: Any,
        state: CopyState,
        block: () -> GMResult<T, TypeError>,
    ): GMResult<T, TypeError> {
        if (state.activeContainers.any { active -> active === value }) {
            return GMResult.Err(TypeError.CyclicCopyValue)
        }
        state.activeContainers += value
        val result = block()
        state.activeContainers.removeAt(state.activeContainers.lastIndex)
        return result
    }

    private fun validateLimits(
        limits: TypeCopyLimits,
    ): TypeError.InvalidCopyLimit? =
        when {
            limits.maxDepth < 0 ->
                TypeError.InvalidCopyLimit("maxDepth", limits.maxDepth)
            limits.maxValues <= 0 ->
                TypeError.InvalidCopyLimit("maxValues", limits.maxValues)
            else -> null
        }

    private class CopyState(
        val limits: TypeCopyLimits,
    ) {
        val activeContainers = mutableListOf<Any>()
        var valueCount: Int = 0
    }
}
