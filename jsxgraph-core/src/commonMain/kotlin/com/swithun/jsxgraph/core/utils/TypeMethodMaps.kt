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
 * Explicit representation of the JavaScript constructor/prototype pair used
 * by the upstream class-initialization helpers.
 */
internal class TypePrototypeDescriptor(
    val constructor: Any?,
    val properties: MutableMap<String, Any?> = linkedMapOf(),
)

/**
 * Represents an instance whose method map is initially inherited from its
 * prototype and becomes an own property on first extension.
 */
internal class TypeMethodMapInstance(
    private val prototypeMethodMap: Map<String, Any?>,
) {
    private var ownMethodMap: MutableMap<String, Any?>? = null

    val methodMap: Map<String, Any?>
        get() = ownMethodMap ?: prototypeMethodMap

    val hasOwnMethodMap: Boolean
        get() = ownMethodMap != null

    internal fun ensureOwnMethodMap(): MutableMap<String, Any?> {
        ownMethodMap?.let { return it }
        return prototypeMethodMap.toMutableMap().also { copy ->
            ownMethodMap = copy
        }
    }

    internal fun replaceMethodMap(value: MutableMap<String, Any?>) {
        ownMethodMap = value
    }
}

internal object TypeMethodMaps {
    // JSXGraph 1.13.3: src/utils/type.js -> copyPrototypeMethods.
    fun copyPrototypeMethods(
        subObject: TypePrototypeDescriptor,
        superObject: TypePrototypeDescriptor,
        constructorName: String,
        limits: TypeCopyLimits,
    ): GMResult<Unit, TypeError> {
        subObject.properties[constructorName] = superObject.constructor
        for (key in Type.keys(superObject.properties)) {
            if (key == METHOD_MAP_PROPERTY) {
                val extension = when (
                    val result = methodMap(
                        value = superObject.properties[key],
                        owner = "superObject.prototype",
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (
                    val copied = copyMethodMap(
                        objectClass = subObject,
                        extension = extension,
                        limits = limits,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return copied
                }
            } else {
                subObject.properties[key] = superObject.properties[key]
            }
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/utils/type.js -> copyMethodMap.
    fun copyMethodMap(
        objectClass: TypePrototypeDescriptor,
        extension: Map<String, Any?>,
        limits: TypeCopyLimits,
    ): GMResult<Unit, TypeError> {
        val current = when (
            val result = methodMap(
                value = objectClass.properties[METHOD_MAP_PROPERTY],
                owner = "objectClass.prototype",
                missingAsEmpty = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val copied = when (
            val result = Type.deepCopy(
                value = current,
                secondary = extension,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val copiedMap = when (
            val result = methodMap(
                value = copied,
                owner = "objectClass.prototype",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        objectClass.properties[METHOD_MAP_PROPERTY] =
            copiedMap.toMutableMap()
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/utils/type.js -> extendInstanceMethodMap.
    fun extendInstanceMethodMap(
        instance: TypeMethodMapInstance,
        extension: Map<String, Any?>,
        limits: TypeCopyLimits,
    ): GMResult<Unit, TypeError> {
        val current = instance.ensureOwnMethodMap()
        val copied = when (
            val result = Type.deepCopy(
                value = current,
                secondary = extension,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val copiedMap = when (
            val result = methodMap(
                value = copied,
                owner = "object",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        instance.replaceMethodMap(copiedMap.toMutableMap())
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/utils/type.js -> extendInstanceMethodMap.
    fun extendInstanceMethodMap(
        instance: TypeMethodMapInstance,
        extension: String,
        extensionValue: Any?,
    ): GMResult<Unit, TypeError> {
        val methodMap = instance.ensureOwnMethodMap()
        if (Type.exists(extensionValue)) {
            methodMap[extension] = extensionValue
        }
        return GMResult.Ok(Unit)
    }

    private fun methodMap(
        value: Any?,
        owner: String,
        missingAsEmpty: Boolean = false,
    ): GMResult<Map<String, Any?>, TypeError> {
        if (
            missingAsEmpty &&
            (value == null || value === DumpUndefined)
        ) {
            return GMResult.Ok(emptyMap())
        }
        val map = value as? Map<*, *>
            ?: return GMResult.Err(
                TypeError.InvalidMethodMap(
                    owner = owner,
                    value = value,
                ),
            )
        val typed = linkedMapOf<String, Any?>()
        for ((key, entryValue) in map) {
            val name = key as? String
                ?: return GMResult.Err(
                    TypeError.InvalidMethodMapKey(
                        owner = owner,
                        key = key,
                    ),
                )
            typed[name] = entryValue
        }
        return GMResult.Ok(typed)
    }

    private const val METHOD_MAP_PROPERTY = "methodMap"
}
