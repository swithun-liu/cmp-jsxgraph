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
 * Bounded mutable-container kernel for the object utilities in `JXG.Type`.
 */
internal object TypeMerge {
    // JSXGraph 1.13.3: src/utils/type.js -> merge.
    fun merge(
        target: Any?,
        source: Any?,
        limits: TypeMergeLimits,
    ): GMResult<Any?, TypeError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        return mergeValue(
            target = target,
            source = source,
            depth = 0,
            state = MergeState(limits),
        )
    }

    // JSXGraph 1.13.3: src/utils/type.js -> mergeAttr.
    fun mergeAttr(
        attributes: MutableMap<String, Any?>,
        special: Map<String, Any?>,
        toLower: Boolean,
        ignoreUndefinedSpecials: Boolean,
        limits: TypeMergeLimits,
    ): GMResult<Unit, TypeError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        return mergeAttributes(
            attributes = attributes,
            special = special,
            toLower = toLower,
            ignoreUndefinedSpecials = ignoreUndefinedSpecials,
            depth = 0,
            state = MergeState(limits),
        )
    }

    private fun mergeValue(
        target: Any?,
        source: Any?,
        depth: Int,
        state: MergeState,
    ): GMResult<Any?, TypeError> {
        visit(depth, state)?.let { return GMResult.Err(it) }
        return when (source) {
            null -> GMResult.Ok(target)
            is Map<*, *> ->
                withSourceContainer(source, state) {
                    mergeObject(
                        target = target,
                        source = source,
                        depth = depth,
                        state = state,
                    )
                }
            else -> {
                val array = arrayValues(source)
                if (array != null) {
                    withSourceContainer(source, state) {
                        mergeArrayObject(
                            target = target,
                            source = array,
                            depth = depth,
                            state = state,
                        )
                    }
                } else {
                    GMResult.Err(TypeError.UnsupportedMergeValue(source))
                }
            }
        }
    }

    private fun mergeObject(
        target: Any?,
        source: Map<*, *>,
        depth: Int,
        state: MergeState,
    ): GMResult<Any?, TypeError> {
        var receiver = target
        val entries = when (val result = orderedEntries(source)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        for ((key, sourceValue) in entries) {
            visit(depth + 1, state)?.let { return GMResult.Err(it) }
            val sourceArray = arrayValues(sourceValue)
            when {
                sourceArray != null -> {
                    val current = when (val result = readProperty(receiver, key)) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    var arrayTarget = current
                    if (!isTruthy(current)) {
                        arrayTarget = mutableListOf<Any?>()
                        when (
                            val result = writeProperty(
                                target = receiver,
                                property = key,
                                value = arrayTarget,
                            )
                        ) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                    } else {
                        when (
                            val result = makeMutableContainer(
                                value = current,
                                property = key,
                            )
                        ) {
                            is GMResult.Ok -> {
                                arrayTarget = result.value
                                if (arrayTarget !== current) {
                                    when (
                                        val written = writeProperty(
                                            target = receiver,
                                            property = key,
                                            value = arrayTarget,
                                        )
                                    ) {
                                        is GMResult.Ok -> Unit
                                        is GMResult.Err -> return written
                                    }
                                }
                            }
                            is GMResult.Err -> return result
                        }
                    }

                    for (index in sourceArray.indices) {
                        val property = index.toString()
                        val item = sourceArray[index]
                        val mergedItem = when {
                            item == null -> {
                                val existing = when (
                                    val result = readProperty(
                                        arrayTarget,
                                        property,
                                    )
                                ) {
                                    is GMResult.Ok -> result.value
                                    is GMResult.Err -> return result
                                }
                                mergeValue(
                                    target = existing,
                                    source = null,
                                    depth = depth + 1,
                                    state = state,
                                )
                            }
                            item is Map<*, *> ||
                                arrayValues(item) != null -> {
                                val existing = when (
                                    val result = readProperty(
                                        arrayTarget,
                                        property,
                                    )
                                ) {
                                    is GMResult.Ok -> result.value
                                    is GMResult.Err -> return result
                                }
                                mergeValue(
                                    target = existing,
                                    source = item,
                                    depth = depth + 1,
                                    state = state,
                                )
                            }
                            isFlatValue(item) -> GMResult.Ok(item)
                            else -> {
                                return GMResult.Err(
                                    TypeError.UnsupportedMergeValue(item),
                                )
                            }
                        }
                        when (mergedItem) {
                            is GMResult.Ok -> {
                                when (
                                    val result = writeProperty(
                                        target = arrayTarget,
                                        property = property,
                                        value = mergedItem.value,
                                    )
                                ) {
                                    is GMResult.Ok -> Unit
                                    is GMResult.Err -> return result
                                }
                            }
                            is GMResult.Err -> return mergedItem
                        }
                    }
                }
                sourceValue == null || sourceValue is Map<*, *> -> {
                    val current = when (val result = readProperty(receiver, key)) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    var objectTarget = current
                    if (!isTruthy(current)) {
                        objectTarget = linkedMapOf<String, Any?>()
                        when (
                            val result = writeProperty(
                                target = receiver,
                                property = key,
                                value = objectTarget,
                            )
                        ) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                    } else if (current is Map<*, *> || arrayValues(current) != null) {
                        when (
                            val result = makeMutableContainer(
                                value = current,
                                property = key,
                            )
                        ) {
                            is GMResult.Ok -> {
                                objectTarget = result.value
                                if (objectTarget !== current) {
                                    when (
                                        val written = writeProperty(
                                            target = receiver,
                                            property = key,
                                            value = objectTarget,
                                        )
                                    ) {
                                        is GMResult.Ok -> Unit
                                        is GMResult.Err -> return written
                                    }
                                }
                            }
                            is GMResult.Err -> return result
                        }
                    }
                    when (
                        val merged = mergeValue(
                            target = objectTarget,
                            source = sourceValue,
                            depth = depth + 1,
                            state = state,
                        )
                    ) {
                        is GMResult.Ok -> {
                            when (
                                val result = writeProperty(
                                    target = receiver,
                                    property = key,
                                    value = merged.value,
                                )
                            ) {
                                is GMResult.Ok -> Unit
                                is GMResult.Err -> return result
                            }
                        }
                        is GMResult.Err -> return merged
                    }
                }
                isFlatValue(sourceValue) -> {
                    if (receiver is Boolean) {
                        receiver = linkedMapOf<String, Any?>()
                    }
                    when (
                        val result = writeProperty(
                            target = receiver,
                            property = key,
                            value = sourceValue,
                        )
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return result
                    }
                }
                else -> {
                    return GMResult.Err(
                        TypeError.UnsupportedMergeValue(sourceValue),
                    )
                }
            }
        }
        return GMResult.Ok(receiver)
    }

    private fun mergeArrayObject(
        target: Any?,
        source: List<Any?>,
        depth: Int,
        state: MergeState,
    ): GMResult<Any?, TypeError> {
        var receiver = target
        for (index in source.indices) {
            val key = index.toString()
            val sourceValue = source[index]
            visit(depth + 1, state)?.let { return GMResult.Err(it) }
            if (sourceValue is Map<*, *> || arrayValues(sourceValue) != null) {
                val current = when (val result = readProperty(receiver, key)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (
                    val merged = mergeValue(
                        target = current,
                        source = sourceValue,
                        depth = depth + 1,
                        state = state,
                    )
                ) {
                    is GMResult.Ok -> {
                        when (
                            val result = writeProperty(
                                target = receiver,
                                property = key,
                                value = merged.value,
                            )
                        ) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                    }
                    is GMResult.Err -> return merged
                }
            } else if (sourceValue == null) {
                val current = when (val result = readProperty(receiver, key)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (
                    val result = writeProperty(
                        target = receiver,
                        property = key,
                        value = current,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            } else if (isFlatValue(sourceValue)) {
                if (receiver is Boolean) {
                    receiver = linkedMapOf<String, Any?>()
                }
                when (
                    val result = writeProperty(
                        target = receiver,
                        property = key,
                        value = sourceValue,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            } else {
                return GMResult.Err(
                    TypeError.UnsupportedMergeValue(sourceValue),
                )
            }
        }
        return GMResult.Ok(receiver)
    }

    private fun mergeAttributes(
        attributes: MutableMap<String, Any?>,
        special: Map<*, *>,
        toLower: Boolean,
        ignoreUndefinedSpecials: Boolean,
        depth: Int,
        state: MergeState,
    ): GMResult<Unit, TypeError> {
        visit(depth, state)?.let { return GMResult.Err(it) }
        return withSourceContainer(special, state) {
            val entries = when (val result = orderedEntries(special)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return@withSourceContainer result
            }
            // Upstream assigns `toLower = toLower || true`, so this is always true.
            val normalizeKeys = toLower || true
            for ((sourceKey, sourceValue) in entries) {
                visit(depth + 1, state)?.let {
                    return@withSourceContainer GMResult.Err(it)
                }
                val key = if (normalizeKeys) {
                    sourceKey.lowercase()
                } else {
                    sourceKey
                }

                if (
                    key != sourceKey &&
                    attributes.containsKey(sourceKey)
                ) {
                    if (attributes.containsKey(key)) {
                        val original = attributes[sourceKey]
                        if (original is Map<*, *>) {
                            val lower = when (
                                val result = mutableAttributeMap(
                                    value = attributes[key],
                                    property = key,
                                )
                            ) {
                                is GMResult.Ok -> result.value
                                is GMResult.Err ->
                                    return@withSourceContainer result
                            }
                            if (lower !== attributes[key]) {
                                when (
                                    val written = writeMapProperty(
                                        target = attributes,
                                        property = key,
                                        value = lower,
                                    )
                                ) {
                                    is GMResult.Ok -> Unit
                                    is GMResult.Err ->
                                        return@withSourceContainer written
                                }
                            }
                            when (
                                val merged = mergeAttributes(
                                    attributes = lower,
                                    special = original,
                                    toLower = toLower,
                                    ignoreUndefinedSpecials = false,
                                    depth = depth + 1,
                                    state = state,
                                )
                            ) {
                                is GMResult.Ok -> Unit
                                is GMResult.Err ->
                                    return@withSourceContainer merged
                            }
                        }
                    } else {
                        when (
                            val written = writeMapProperty(
                                target = attributes,
                                property = key,
                                value = attributes[sourceKey],
                            )
                        ) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err ->
                                return@withSourceContainer written
                        }
                    }
                    when (
                        val removed = removeMapProperty(
                            target = attributes,
                            property = sourceKey,
                        )
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err ->
                            return@withSourceContainer removed
                    }
                }

                if (
                    sourceValue is Map<*, *> &&
                    !hasExistingProperty(sourceValue, "board")
                ) {
                    val target = when (
                        val result = mutableAttributeMap(
                            value = attributes[key],
                            property = key,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err ->
                            return@withSourceContainer result
                    }
                    if (target !== attributes[key]) {
                        when (
                            val written = writeMapProperty(
                                target = attributes,
                                property = key,
                                value = target,
                            )
                        ) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err ->
                                return@withSourceContainer written
                        }
                    }
                    when (
                        val merged = mergeAttributes(
                            attributes = target,
                            special = sourceValue,
                            toLower = toLower,
                            ignoreUndefinedSpecials =
                                ignoreUndefinedSpecials,
                            depth = depth + 1,
                            state = state,
                        )
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err ->
                            return@withSourceContainer merged
                    }
                } else if (
                    !ignoreUndefinedSpecials ||
                    Type.exists(sourceValue)
                ) {
                    when (
                        val written = writeMapProperty(
                            target = attributes,
                            property = key,
                            value = sourceValue,
                        )
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err ->
                            return@withSourceContainer written
                    }
                }
            }
            GMResult.Ok(Unit)
        }
    }

    private fun mutableAttributeMap(
        value: Any?,
        property: String,
    ): GMResult<MutableMap<String, Any?>, TypeError> {
        if (value !is Map<*, *>) {
            return GMResult.Ok(linkedMapOf())
        }
        if (value is MutableMap<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return GMResult.Ok(value as MutableMap<String, Any?>)
        }
        val copy = linkedMapOf<String, Any?>()
        for ((key, entryValue) in value) {
            if (key !is String) {
                return GMResult.Err(
                    TypeError.InvalidMergeObjectKey(key),
                )
            }
            copy[key] = entryValue
        }
        return if (copy.isEmpty() && value.isNotEmpty()) {
            GMResult.Err(TypeError.InvalidMergeProperty(property, value))
        } else {
            GMResult.Ok(copy)
        }
    }

    private fun makeMutableContainer(
        value: Any?,
        property: String,
    ): GMResult<Any, TypeError> =
        when (value) {
            is MutableMap<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                GMResult.Ok(value as MutableMap<String, Any?>)
            }
            is Map<*, *> -> {
                val copy = linkedMapOf<String, Any?>()
                for ((key, entryValue) in value) {
                    if (key !is String) {
                        return GMResult.Err(
                            TypeError.InvalidMergeObjectKey(key),
                        )
                    }
                    copy[key] = entryValue
                }
                GMResult.Ok(copy)
            }
            is MutableList<*> -> {
                @Suppress("UNCHECKED_CAST")
                GMResult.Ok(value as MutableList<Any?>)
            }
            else -> {
                val array = arrayValues(value)
                if (array != null) {
                    GMResult.Ok(array.toMutableList())
                } else {
                    GMResult.Err(
                        TypeError.InvalidMergeProperty(property, value),
                    )
                }
            }
        }

    private fun readProperty(
        target: Any?,
        property: String,
    ): GMResult<Any?, TypeError> =
        when (target) {
            is Map<*, *> -> GMResult.Ok(target[property] ?: run {
                if (target.containsKey(property)) null else DumpUndefined
            })
            is List<*> -> {
                val index = property.toIntOrNull()
                    ?: return GMResult.Err(
                        TypeError.InvalidMergeProperty(property, target),
                    )
                GMResult.Ok(target.getOrNull(index) ?: run {
                    if (index in target.indices) null else DumpUndefined
                })
            }
            is Array<*> -> readListProperty(target.asList(), property)
            is BooleanArray ->
                readListProperty(target.map { value -> value }, property)
            is ByteArray ->
                readListProperty(target.map { value -> value }, property)
            is ShortArray ->
                readListProperty(target.map { value -> value }, property)
            is IntArray ->
                readListProperty(target.map { value -> value }, property)
            is LongArray ->
                readListProperty(target.map { value -> value }, property)
            is FloatArray ->
                readListProperty(target.map { value -> value }, property)
            is DoubleArray ->
                readListProperty(target.map { value -> value }, property)
            is Boolean -> GMResult.Ok(DumpUndefined)
            else -> GMResult.Err(TypeError.InvalidMergeTarget(target))
        }

    private fun readListProperty(
        target: List<Any?>,
        property: String,
    ): GMResult<Any?, TypeError> {
        val index = property.toIntOrNull()
            ?: return GMResult.Err(
                TypeError.InvalidMergeProperty(property, target),
            )
        return GMResult.Ok(
            if (index in target.indices) target[index] else DumpUndefined,
        )
    }

    private fun writeProperty(
        target: Any?,
        property: String,
        value: Any?,
    ): GMResult<Unit, TypeError> =
        when (target) {
            is MutableMap<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                writeMapProperty(
                    target = target as MutableMap<String, Any?>,
                    property = property,
                    value = value,
                )
            }
            is MutableList<*> -> {
                @Suppress("UNCHECKED_CAST")
                writeListProperty(
                    target = target as MutableList<Any?>,
                    property = property,
                    value = value,
                )
            }
            else -> GMResult.Err(TypeError.InvalidMergeTarget(target))
        }

    private fun writeMapProperty(
        target: MutableMap<String, Any?>,
        property: String,
        value: Any?,
    ): GMResult<Unit, TypeError> =
        try {
            target[property] = value
            GMResult.Ok(Unit)
        } catch (exception: Exception) {
            GMResult.Err(
                TypeError.MergeMutationFailed(
                    property = property,
                    message = exception.message
                        ?: exception::class.simpleName.orEmpty(),
                ),
            )
        }

    private fun removeMapProperty(
        target: MutableMap<String, Any?>,
        property: String,
    ): GMResult<Unit, TypeError> =
        try {
            target.remove(property)
            GMResult.Ok(Unit)
        } catch (exception: Exception) {
            GMResult.Err(
                TypeError.MergeMutationFailed(
                    property = property,
                    message = exception.message
                        ?: exception::class.simpleName.orEmpty(),
                ),
            )
        }

    private fun writeListProperty(
        target: MutableList<Any?>,
        property: String,
        value: Any?,
    ): GMResult<Unit, TypeError> {
        val index = property.toIntOrNull()
            ?: return GMResult.Err(
                TypeError.InvalidMergeProperty(property, target),
            )
        if (index < 0) {
            return GMResult.Err(
                TypeError.InvalidMergeProperty(property, target),
            )
        }
        return try {
            while (target.size <= index) {
                target += DumpUndefined
            }
            target[index] = value
            GMResult.Ok(Unit)
        } catch (exception: Exception) {
            GMResult.Err(
                TypeError.MergeMutationFailed(
                    property = property,
                    message = exception.message
                        ?: exception::class.simpleName.orEmpty(),
                ),
            )
        }
    }

    private fun orderedEntries(
        value: Map<*, *>,
    ): GMResult<List<Pair<String, Any?>>, TypeError> {
        val typed = linkedMapOf<String, Any?>()
        for ((key, entryValue) in value) {
            if (key !is String) {
                return GMResult.Err(
                    TypeError.InvalidMergeObjectKey(key),
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

    private fun arrayValues(value: Any?): List<Any?>? =
        when (value) {
            is List<*> -> value.map { item -> item }
            is Array<*> -> value.map { item -> item }
            is BooleanArray -> value.map { item -> item }
            is ByteArray -> value.map { item -> item }
            is ShortArray -> value.map { item -> item }
            is IntArray -> value.map { item -> item }
            is LongArray -> value.map { item -> item }
            is FloatArray -> value.map { item -> item }
            is DoubleArray -> value.map { item -> item }
            else -> null
        }

    private fun isFlatValue(value: Any?): Boolean =
        value === DumpUndefined ||
            value is String ||
            value is Number ||
            value is Boolean ||
            value is Function<*>

    private fun isTruthy(value: Any?): Boolean =
        when (value) {
            null, DumpUndefined -> false
            is Boolean -> value
            is Number -> value.toDouble() != 0.0 && !value.toDouble().isNaN()
            is String -> value.isNotEmpty()
            else -> true
        }

    private fun visit(
        depth: Int,
        state: MergeState,
    ): TypeError? {
        if (depth > state.limits.maxDepth) {
            return TypeError.MergeDepthLimitExceeded(
                state.limits.maxDepth,
            )
        }
        state.valueCount += 1
        if (state.valueCount > state.limits.maxValues) {
            return TypeError.MergeValueLimitExceeded(
                state.limits.maxValues,
            )
        }
        return null
    }

    private inline fun <T> withSourceContainer(
        value: Any,
        state: MergeState,
        block: () -> GMResult<T, TypeError>,
    ): GMResult<T, TypeError> {
        if (state.activeSources.any { active -> active === value }) {
            return GMResult.Err(TypeError.CyclicMergeValue)
        }
        state.activeSources += value
        val result = block()
        state.activeSources.removeAt(state.activeSources.lastIndex)
        return result
    }

    private fun validateLimits(
        limits: TypeMergeLimits,
    ): TypeError.InvalidMergeLimit? =
        when {
            limits.maxDepth < 0 ->
                TypeError.InvalidMergeLimit("maxDepth", limits.maxDepth)
            limits.maxValues <= 0 ->
                TypeError.InvalidMergeLimit("maxValues", limits.maxValues)
            else -> null
        }

    private class MergeState(
        val limits: TypeMergeLimits,
    ) {
        val activeSources = mutableListOf<Any>()
        var valueCount: Int = 0
    }
}
