/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/type.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult

internal interface TypeVisualPropertyCacheOwner {
    var visPropOld: MutableMap<String, Any?>
}

/**
 * Adapter for the visual state consumed by `JXG.Type.getCloneObject`.
 */
internal interface TypeCloneSource {
    val id: String
    var numTraces: Int
    val coords: Any?
    val visProp: Map<String, Any?>
    val traceLayer: Any?
    val board: Any?
    val elementClass: Int

    fun eval(value: Any?): GMResult<Any?, TypeError>

    fun evalVisProp(property: String): GMResult<Any?, TypeError>
}

internal class TypeCloneObject(
    val id: String,
    val coords: Any?,
    val visProp: MutableMap<String, Any?>,
    val board: Any?,
    val elementClass: Int,
    override var visPropOld: MutableMap<String, Any?> = linkedMapOf(),
    val visPropCalc: MutableMap<String, Any?> = linkedMapOf(),
) : TypeVisualPropertyCacheOwner {
    fun evalVisProp(property: String): Any? = visProp[property]

    fun eval(value: Any?): Any? = value
}

internal object TypeTraceClone {
    // JSXGraph 1.13.3: src/utils/type.js -> getCloneObject.
    fun getCloneObject(
        element: TypeCloneSource,
        limits: TypeCopyLimits,
    ): GMResult<TypeCloneObject, TypeError> {
        val cloneId = "${element.id}T${element.numTraces}"
        element.numTraces += 1

        val traceAttributes = when (
            val result = optionalStringMap(
                value = element.visProp["traceattributes"],
                property = "traceattributes",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val copied = when (
            val result = Type.deepCopy(
                value = element.visProp,
                secondary = traceAttributes,
                toLower = true,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val copiedProperties = when (
            val result = requiredStringMap(copied, "visProp")
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val visualProperties = linkedMapOf<String, Any?>()
        for (key in Type.keys(copiedProperties)) {
            if (shouldSkip(key)) {
                continue
            }
            val evaluated = try {
                element.eval(copiedProperties[key])
            } catch (exception: Exception) {
                return GMResult.Err(
                    TypeError.ClonePropertyEvaluationFailed(
                        property = key,
                        message = exception.message
                            ?: exception::class.simpleName.orEmpty(),
                    ),
                )
            }
            when (evaluated) {
                is GMResult.Ok -> visualProperties[key] = evaluated.value
                is GMResult.Err -> return evaluated
            }
        }
        visualProperties["layer"] = element.traceLayer
        visualProperties["tabindex"] = null
        visualProperties["highlight"] = false

        val clone = TypeCloneObject(
            id = cloneId,
            coords = element.coords,
            visProp = visualProperties,
            board = element.board,
            elementClass = element.elementClass,
        )
        clearVisPropOld(clone)
        val visible = try {
            element.evalVisProp("visible")
        } catch (exception: Exception) {
            return GMResult.Err(
                TypeError.ClonePropertyEvaluationFailed(
                    property = "visible",
                    message = exception.message
                        ?: exception::class.simpleName.orEmpty(),
                ),
            )
        }
        when (visible) {
            is GMResult.Ok ->
                clone.visPropCalc["visible"] = visible.value
            is GMResult.Err -> return visible
        }
        return GMResult.Ok(clone)
    }

    // JSXGraph 1.13.3: src/utils/type.js -> clearVisPropOld.
    fun <T : TypeVisualPropertyCacheOwner> clearVisPropOld(
        element: T,
    ): T {
        element.visPropOld = linkedMapOf(
            "cssclass" to "",
            "cssdefaultstyle" to "",
            "cssstyle" to "",
            "fillcolor" to "",
            "fillopacity" to "",
            "firstarrow" to false,
            "fontsize" to -1,
            "lastarrow" to false,
            "left" to -100000,
            "linecap" to "",
            "shadow" to false,
            "strokecolor" to "",
            "strokeopacity" to "",
            "strokewidth" to "",
            "tabindex" to -100000,
            "transitionduration" to 0,
            "top" to -100000,
            "visible" to null,
        )
        return element
    }

    private fun shouldSkip(property: String): Boolean =
        property.startsWith("aria") ||
            property.startsWith("highlight") ||
            property.startsWith("attractor") ||
            property == "label" ||
            property == "needsregularupdate" ||
            property == "infoboxdigits"

    private fun optionalStringMap(
        value: Any?,
        property: String,
    ): GMResult<Map<String, Any?>?, TypeError> {
        if (value == null || value === DumpUndefined) {
            return GMResult.Ok(null)
        }
        return when (val result = requiredStringMap(value, property)) {
            is GMResult.Ok -> GMResult.Ok(result.value)
            is GMResult.Err -> result
        }
    }

    private fun requiredStringMap(
        value: Any?,
        property: String,
    ): GMResult<Map<String, Any?>, TypeError> {
        val map = value as? Map<*, *>
            ?: return GMResult.Err(
                TypeError.InvalidClonePropertyMap(
                    property = property,
                    value = value,
                ),
            )
        val typed = linkedMapOf<String, Any?>()
        for ((key, entryValue) in map) {
            val name = key as? String
                ?: return GMResult.Err(
                    TypeError.InvalidClonePropertyKey(
                        property = property,
                        key = key,
                    ),
                )
            typed[name] = entryValue
        }
        return GMResult.Ok(typed)
    }
}
