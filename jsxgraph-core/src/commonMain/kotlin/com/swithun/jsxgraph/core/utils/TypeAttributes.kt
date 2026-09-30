/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/type.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult

internal data class TypeAttributeLimits(
    val copy: TypeCopyLimits = TypeCopyLimits(),
    val merge: TypeMergeLimits = TypeMergeLimits(),
)

/**
 * Generic map adapter for the option inheritance implemented by
 * `JXG.Type.copyAttributes`.
 */
internal object TypeAttributes {
    // JSXGraph 1.13.3: src/utils/type.js -> copyAttributes.
    fun copyAttributes(
        attributes: Map<String, Any?>?,
        options: Map<String, Any?>,
        path: List<String>,
        limits: TypeAttributeLimits,
    ): GMResult<Map<String, Any?>, TypeError> {
        val mainClass = path.firstOrNull()
        var result = if (
            path.isEmpty() ||
            path.size == 1 && mainClass in PRIMITIVE_CLASSES
        ) {
            val elements = when (
                val typed = typedMap(options["elements"])
            ) {
                is GMResult.Ok -> typed.value ?: emptyMap()
                is GMResult.Err -> return typed
            }
            when (
                val copied = copyMerged(
                    primary = elements,
                    secondary = null,
                    limits = limits.copy,
                )
            ) {
                is GMResult.Ok -> copied.value
                is GMResult.Err -> return copied
            }
        } else {
            linkedMapOf()
        }

        if (path.size < 2 && mainClass != null) {
            val layers = when (val typed = typedMap(options["layer"])) {
                is GMResult.Ok -> typed.value
                is GMResult.Err -> return typed
            }
            val layer = layers?.get(mainClass)
            if (Type.exists(layer)) {
                result["layer"] = layer
            }
        }

        when (val defaults = resolvePath(options, path)) {
            is GMResult.Err -> return defaults
            is GMResult.Ok -> {
                val branch = when (val typed = typedMap(defaults.value)) {
                    is GMResult.Ok -> typed.value
                    is GMResult.Err -> return typed
                }
                if (branch != null) {
                    result = when (
                        val copied = copyMerged(
                            primary = result,
                            secondary = branch,
                            limits = limits.copy,
                        )
                    ) {
                        is GMResult.Ok -> copied.value
                        is GMResult.Err -> return copied
                    }
                }
            }
        }

        val normalizedAttributes = if (attributes == null) {
            emptyMap()
        } else {
            when (
                val normalized = Type.keysToLowerCase(
                    value = attributes,
                    limits = limits.copy,
                )
            ) {
                is GMResult.Ok -> normalized.value
                is GMResult.Err -> return normalized
            }
        }
        when (
            val supplied = resolvePath(
                root = normalizedAttributes,
                path = path.drop(1).map(String::lowercase),
            )
        ) {
            is GMResult.Err -> return supplied
            is GMResult.Ok -> {
                val branch = when (val typed = typedMap(supplied.value)) {
                    is GMResult.Ok -> typed.value
                    is GMResult.Err -> return typed
                }
                if (branch != null) {
                    when (
                        val merged = Type.mergeAttr(
                            attributes = result,
                            special = branch,
                            toLower = true,
                            limits = limits.merge,
                        )
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return merged
                    }
                }
            }
        }

        if (mainClass == "board") {
            return GMResult.Ok(result)
        }

        val specificDefaults = when (
            val resolved = resolvePath(options, path)
        ) {
            is GMResult.Ok -> resolved.value
            is GMResult.Err -> return resolved
        }
        val specificMap = when (val typed = typedMap(specificDefaults)) {
            is GMResult.Ok -> typed.value
            is GMResult.Err -> return typed
        }
        val specificLabel = specificMap?.get("label")
        if (Type.exists(specificLabel)) {
            when (
                val copied = copyLabel(
                    defaults = specificLabel,
                    supplied = result["label"],
                    limits = limits.copy,
                )
            ) {
                is GMResult.Ok -> result["label"] = copied.value
                is GMResult.Err -> return copied
            }
        }

        when (
            val copied = copyLabel(
                defaults = options["label"],
                supplied = result["label"],
                limits = limits.copy,
            )
        ) {
            is GMResult.Ok -> result["label"] = copied.value
            is GMResult.Err -> return copied
        }
        return GMResult.Ok(result)
    }

    private fun copyMerged(
        primary: Map<String, Any?>,
        secondary: Map<String, Any?>?,
        limits: TypeCopyLimits,
    ): GMResult<MutableMap<String, Any?>, TypeError> =
        when (
            val copied = Type.deepCopy(
                value = primary,
                secondary = secondary,
                toLower = true,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> {
                val map = when (val typed = typedMap(copied.value)) {
                    is GMResult.Ok -> typed.value
                    is GMResult.Err -> return typed
                }
                if (map == null) {
                    GMResult.Err(
                        TypeError.InvalidCopyAttributesResult(copied.value),
                    )
                } else {
                    GMResult.Ok(map.toMutableMap())
                }
            }
            is GMResult.Err -> copied
        }

    private fun copyLabel(
        defaults: Any?,
        supplied: Any?,
        limits: TypeCopyLimits,
    ): GMResult<Any?, TypeError> {
        val suppliedMap = when (val typed = typedMap(supplied)) {
            is GMResult.Ok -> typed.value
            is GMResult.Err -> return typed
        }
        return Type.deepCopy(
            value = defaults,
            secondary = suppliedMap,
            toLower = true,
            limits = limits,
        )
    }

    private fun resolvePath(
        root: Map<String, Any?>,
        path: List<String>,
    ): GMResult<Any?, TypeError> {
        var current: Any? = root
        for (segment in path) {
            val currentMap = when (val typed = typedMap(current)) {
                is GMResult.Ok -> typed.value
                is GMResult.Err -> return typed
            } ?: return GMResult.Ok(DumpUndefined)
            val next = currentMap[segment]
            if (!Type.exists(next)) {
                return GMResult.Ok(DumpUndefined)
            }
            current = next
        }
        return GMResult.Ok(current)
    }

    private fun typedMap(
        value: Any?,
    ): GMResult<Map<String, Any?>?, TypeError> {
        if (value !is Map<*, *>) {
            return GMResult.Ok(null)
        }
        val result = linkedMapOf<String, Any?>()
        for ((key, entryValue) in value) {
            if (key !is String) {
                return GMResult.Err(TypeError.InvalidCopyObjectKey(key))
            }
            result[key] = entryValue
        }
        return GMResult.Ok(result)
    }

    private val PRIMITIVE_CLASSES = setOf(
        "circle",
        "curve",
        "foreignobject",
        "image",
        "line",
        "point",
        "polygon",
        "text",
        "ticks",
        "integral",
    )
}
