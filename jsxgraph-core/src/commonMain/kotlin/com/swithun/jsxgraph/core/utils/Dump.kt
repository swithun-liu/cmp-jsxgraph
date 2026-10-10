/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/dump.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.GeometryElementDumpError
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.parser.JessieCodeSourceBackedFunction
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

internal data class DumpLimits(
    val maxDepth: Int = 64,
    val maxValues: Int = 100_000,
    val maxElements: Int = 10_000,
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

    data class ElementLimitExceeded(
        val limit: Int,
    ) : DumpError

    data class OutputLimitExceeded(
        val limit: Int,
    ) : DumpError

    data class InvalidObjectKey(
        val key: String,
    ) : DumpError

    data class AttributeOperation(
        val error: TypeError,
    ) : DumpError

    data class ParentEvaluation(
        val error: GeometryElementDumpError,
    ) : DumpError

    data class Serialization(
        val error: TypeError,
    ) : DumpError

    data class InvalidAttributeObject(
        val context: String,
        val value: Any?,
    ) : DumpError

    data object UnsupportedValue : DumpError
}

internal data class DumpElementSnapshot(
    val id: String,
    val name: String,
    val elType: String,
    val attributes: Map<String, Any?>,
    val subs: Map<String, DumpElementSnapshot> = emptyMap(),
)

internal data class DumpMethodCall(
    val obj: String,
    val method: String,
    val params: List<Any?>,
)

internal data class DumpPropertyAssignment(
    val obj: String,
    val property: String,
    val value: Any?,
)

internal data class DumpedElement(
    val type: String,
    val parents: List<Any?>,
    val children: List<String>,
    val attributes: Map<String, Any?>,
    val properties: Map<String, Any?> = emptyMap(),
    val ancestors: List<String> = emptyList(),
)

internal data class DumpBoardSnapshot(
    val elements: List<DumpedElement>,
    val properties: List<DumpPropertyAssignment>,
    val methods: List<DumpMethodCall>,
    val userLog: List<Map<String, Any?>> = emptyList(),
)

/**
 * Typed marker for JavaScript's `undefined` value.
 */
internal data object DumpUndefined

/**
 * Raw function source used where JavaScript joins function-valued parents via
 * `Function.prototype.toString()`.
 */
private data class DumpCodeExpression(
    val source: String,
)

/**
 * Source-mapped pure-KMP translation of `JXG.Dump`.
 */
internal object Dump {
    // JSXGraph 1.13.3: src/utils/dump.js -> addMarkers.
    fun addMarkers(
        board: Board,
        markers: List<String>,
        values: List<Any?>,
    ): Board {
        val length = minOf(markers.size, values.size)
        for (element in board.objects.values) {
            for (index in 0 until length) {
                element.dumpMarkers[markers[index]] = values[index]
            }
        }
        return board
    }

    fun addMarkers(
        board: Board,
        marker: String,
        value: Any?,
    ): Board = addMarkers(board, listOf(marker), listOf(value))

    // JSXGraph 1.13.3: src/utils/dump.js -> deleteMarkers.
    fun deleteMarkers(
        board: Board,
        markers: List<String>,
    ): Board {
        for (element in board.objects.values) {
            for (marker in markers) {
                element.dumpMarkers.remove(marker)
            }
        }
        return board
    }

    fun deleteMarkers(
        board: Board,
        marker: String,
    ): Board = deleteMarkers(board, listOf(marker))

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

    // JSXGraph 1.13.3: src/utils/dump.js -> _minimizeSubObject.
    fun minimizeSubObject(
        instance: MutableMap<String, Any?>,
        defaults: Map<String, Any?>,
    ): GMResult<Boolean, DumpError> {
        var deleteAll = true
        for ((property, defaultValue) in defaults) {
            val lowerProperty = property.lowercase()
            val instanceValue = instance[lowerProperty]
            when {
                jsStrictEquals(defaultValue, instanceValue) ||
                    !Type.exists(defaultValue) &&
                    !Type.exists(instanceValue) ->
                    instance.remove(lowerProperty)
                isArrayValue(defaultValue) &&
                    isArrayValue(instanceValue) -> {
                    if (
                        Type.cmpArrays(
                            arrayValues(defaultValue).orEmpty(),
                            arrayValues(instanceValue).orEmpty(),
                        )
                    ) {
                        instance.remove(lowerProperty)
                    } else {
                        deleteAll = false
                    }
                }
                defaultValue is Map<*, *> &&
                    instanceValue is Map<*, *> -> {
                    val defaultMap = when (
                        val result = stringMap(
                            defaultValue,
                            "default.$property",
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    val instanceMap = when (
                        val result = mutableStringMap(
                            instanceValue,
                            "instance.$lowerProperty",
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    val remove = when (
                        val result = minimizeSubObject(
                            instance = instanceMap,
                            defaults = defaultMap,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    if (remove) {
                        instance.remove(lowerProperty)
                    } else {
                        instance[lowerProperty] = instanceMap
                        deleteAll = false
                    }
                }
                else -> deleteAll = false
            }
        }
        if (
            deleteAll &&
            defaults.isEmpty() &&
            instance.isNotEmpty()
        ) {
            deleteAll = false
        }
        return GMResult.Ok(deleteAll)
    }

    // JSXGraph 1.13.3: src/utils/dump.js -> minimizeObject.
    fun minimizeObject(
        instance: Map<String, Any?>,
        elementDefaults: Map<String, Any?> = emptyMap(),
        defaults: List<Map<String, Any?>> = emptyList(),
    ): GMResult<Map<String, Any?>, DumpError> {
        val copy = when (
            val result = Type.deepCopy(instance)
        ) {
            is GMResult.Ok -> when (
                val typed = mutableStringMap(
                    result.value,
                    "instance",
                )
            ) {
                is GMResult.Ok -> typed.value
                is GMResult.Err -> return typed
            }
            is GMResult.Err -> return GMResult.Err(
                DumpError.AttributeOperation(result.error),
            )
        }
        var mergedDefaults: MutableMap<String, Any?> = linkedMapOf()
        for (source in listOf(elementDefaults) + defaults.asReversed()) {
            val merged = when (
                val result = Type.deepCopy(
                    value = mergedDefaults,
                    secondary = source,
                    toLower = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    DumpError.AttributeOperation(result.error),
                )
            }
            mergedDefaults = when (
                val result = mutableStringMap(
                    merged,
                    "defaults",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        }
        val removeAll = when (
            val result = minimizeSubObject(copy, mergedDefaults)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(if (removeAll) emptyMap() else copy)
    }

    // JSXGraph 1.13.3: src/utils/dump.js -> prepareAttributes.
    fun prepareAttributes(
        snapshot: DumpElementSnapshot,
        options: Map<String, Any?>,
    ): GMResult<Map<String, Any?>, DumpError> {
        val elements = when (
            val result = optionalStringMap(
                options["elements"],
                "options.elements",
            )
        ) {
            is GMResult.Ok -> result.value.orEmpty()
            is GMResult.Err -> return result
        }
        val elementOptions = when (
            val result = optionalStringMap(
                options[snapshot.elType],
                "options.${snapshot.elType}",
            )
        ) {
            is GMResult.Ok -> result.value.orEmpty()
            is GMResult.Err -> return result
        }
        val attributes = when (
            val result = minimizeObject(
                instance = snapshot.attributes,
                elementDefaults = elements,
                defaults = listOf(elementOptions),
            )
        ) {
            is GMResult.Ok -> result.value.toMutableMap()
            is GMResult.Err -> return result
        }

        for ((name, sub) in snapshot.subs) {
            val subtypeOptions = when (
                val result = optionalStringMap(
                    options[sub.elType],
                    "options.${sub.elType}",
                )
            ) {
                is GMResult.Ok -> result.value.orEmpty()
                is GMResult.Err -> return result
            }
            val inheritedOptions = when (
                val result = optionalStringMap(
                    elementOptions[name],
                    "options.${snapshot.elType}.$name",
                )
            ) {
                is GMResult.Ok -> result.value.orEmpty()
                is GMResult.Err -> return result
            }
            val subAttributes = when (
                val result = minimizeObject(
                    instance = sub.attributes,
                    elementDefaults = elements,
                    defaults = listOf(
                        inheritedOptions,
                        subtypeOptions,
                    ),
                )
            ) {
                is GMResult.Ok -> result.value.toMutableMap()
                is GMResult.Err -> return result
            }
            subAttributes["id"] = sub.id
            subAttributes["name"] = sub.name
            attributes[name] = subAttributes
        }

        val label = attributes["label"]
        if (label is Map<*, *>) {
            val labelAttributes = when (
                val result = stringMap(label, "attributes.label")
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val labelOptions = when (
                val result = optionalStringMap(
                    options["label"],
                    "options.label",
                )
            ) {
                is GMResult.Ok -> result.value.orEmpty()
                is GMResult.Err -> return result
            }
            val minimizedLabel = when (
                val result = minimizeObject(
                    instance = labelAttributes,
                    elementDefaults = elements,
                    defaults = listOf(labelOptions),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (minimizedLabel.isEmpty()) {
                attributes.remove("label")
            } else {
                attributes["label"] = minimizedLabel
            }
        }

        val layerDefaults = when (
            val result = optionalStringMap(
                options["layer"],
                "options.layer",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            "layer" in attributes &&
            jsStrictEquals(
                attributes["layer"],
                layerDefaults?.get(snapshot.elType),
            )
        ) {
            attributes.remove("layer")
        }
        if (attributes["draft"] == false) {
            attributes.remove("draft")
        }
        attributes["id"] = snapshot.id
        attributes["name"] = snapshot.name
        return GMResult.Ok(attributes)
    }

    // JSXGraph 1.13.3: src/utils/dump.js -> setBoundingBox.
    fun setBoundingBox(
        methods: MutableList<DumpMethodCall>,
        board: Board,
        boardVariableName: String,
    ): MutableList<DumpMethodCall> {
        methods += DumpMethodCall(
            obj = boardVariableName,
            method = "setBoundingBox",
            params = listOf(
                board.getBoundingBox().toList(),
                board.keepAspectRatio,
            ),
        )
        return methods
    }

    // JSXGraph 1.13.3: src/utils/dump.js -> dump.
    fun dump(
        board: Board,
        json: Boolean = false,
        javaScript: Boolean = false,
        options: Map<String, Any?> = emptyMap(),
        limits: DumpLimits = DumpLimits(),
    ): GMResult<DumpBoardSnapshot, DumpError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        val elements = mutableListOf<DumpedElement>()
        val properties = mutableListOf<DumpPropertyAssignment>()
        val methods = mutableListOf<DumpMethodCall>()

        addMarkers(board, "dumped", false)
        try {
            for (element in board.objectsList) {
                if (
                    element.dumpMarkers["dumped"] == true ||
                    !element.dump
                ) {
                    continue
                }
                if (elements.size >= limits.maxElements) {
                    return GMResult.Err(
                        DumpError.ElementLimitExceeded(limits.maxElements),
                    )
                }
                val rawParents = when (val result = element.dumpParents()) {
                    is GMResult.Ok -> result.value.toMutableList()
                    is GMResult.Err ->
                        return GMResult.Err(
                            DumpError.ParentEvaluation(result.error),
                        )
                }
                if (
                    element.getType() == "point" &&
                    (rawParents.firstOrNull() as? Number)
                        ?.toDouble() == 1.0
                ) {
                    rawParents.removeAt(0)
                }
                val parents =
                    if (json) {
                        rawParents.map(::normalizeJsonParent)
                    } else {
                        val prepared = mutableListOf<Any?>()
                        for (parent in rawParents) {
                            when (
                                val result = prepareCodeParent(
                                    parent,
                                    javaScript,
                                )
                            ) {
                                is GMResult.Ok -> prepared += result.value
                                is GMResult.Err -> return result
                            }
                        }
                        prepared
                    }
                val attributes = when (
                    val result = prepareAttributes(
                        snapshot = element.attributeSnapshot(),
                        options = options,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (
                    element.getType() == "glider" &&
                    (element as? CoordsElement)?.onPolygon == true
                ) {
                    properties += DumpPropertyAssignment(
                        obj = element.id,
                        property = "onPolygon",
                        value = true,
                    )
                }
                elements += DumpedElement(
                    type = element.getType(),
                    parents = parents,
                    children = emptyList(),
                    attributes = attributes,
                )
            }
        } finally {
            deleteMarkers(board, "dumped")
        }
        return GMResult.Ok(
            DumpBoardSnapshot(
                elements = elements,
                properties = properties,
                methods = methods,
            ),
        )
    }

    // JSXGraph 1.13.3: src/utils/dump.js -> toJessie.
    fun toJessie(
        board: Board,
        noAttributes: Boolean = false,
        options: Map<String, Any?> = emptyMap(),
        limits: DumpLimits = DumpLimits(),
    ): GMResult<String, DumpError> {
        val dumped = when (
            val result = dump(
                board = board,
                options = options,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val methods = dumped.methods.toMutableList()
        setBoundingBox(methods, board, "\$board")
        val output = BoundedOutput(limits.maxOutputLength)

        for ((index, element) in dumped.elements.withIndex()) {
            val attributes =
                if (noAttributes) {
                    element.attributes.filterKeys { key ->
                        key == "id" || key == "name"
                    }
                } else {
                    element.attributes
                }
            val name = attributes["name"] as? String
            if (!name.isNullOrEmpty()) {
                output.line("// $name")?.let { return GMResult.Err(it) }
            }
            val serializedAttributes = when (
                val result = toJCAN(attributes, limits)
            ) {
                is GMResult.Ok -> result.value.replaceFirst("\n", "\\n")
                is GMResult.Err -> return result
            }
            output.line(
                "s$index = ${element.type}(" +
                    element.parents.joinToString(", ", transform = ::jsString) +
                    ") $serializedAttributes;",
            )?.let { return GMResult.Err(it) }
            if (element.type == "axis") {
                val id = attributes["id"] as? String
                val line = id?.let(board.objects::get) as? Line
                if (line != null && line.defaultTicks == null) {
                    output.line("s$index.removeAllTicks();")?.let {
                        return GMResult.Err(it)
                    }
                }
            }
            output.line("")?.let { return GMResult.Err(it) }
        }

        for (method in methods) {
            val params = when (
                val result = arrayToParamStr(method.params, limits)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            output.line(
                "${method.obj}.${method.method}($params);",
            )?.let { return GMResult.Err(it) }
            output.line("")?.let { return GMResult.Err(it) }
        }
        for (property in dumped.properties) {
            val value = when (
                val result = toJCAN(property.value, limits)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            output.line(
                "${property.obj}.${property.property} = $value;",
            )?.let { return GMResult.Err(it) }
            output.line("")?.let { return GMResult.Err(it) }
        }
        return GMResult.Ok(output.value())
    }

    // JSXGraph 1.13.3: src/utils/dump.js -> toJSON.
    fun toJSON(
        board: Board,
        options: Map<String, Any?> = emptyMap(),
        limits: DumpLimits = DumpLimits(),
    ): GMResult<String, DumpError> {
        val dumped = when (
            val result = dump(
                board = board,
                json = true,
                options = options,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val enriched = dumped.elements.map { element ->
            val id = element.attributes["id"] as? String
            val source = id?.let(board.objects::get)
            if (source == null) {
                element
            } else {
                element.copy(
                    children = source.childElements.values
                        .filter(GeometryElement::dump)
                        .map(GeometryElement::id)
                        .distinct(),
                    properties = linkedMapOf(
                        "isDraggable" to (
                            source.isDraggable &&
                                source.visualPropertiesCalculated["visible"] ==
                                true
                            ),
                        "elType" to source.elType,
                    ),
                    ancestors = source.ancestors.values
                        .filter(GeometryElement::dump)
                        .map(GeometryElement::id)
                        .distinct(),
                )
            }
        }
        val serializable = linkedMapOf<String, Any?>(
            "elements" to enriched.map { element -> element.toMap() },
            "props" to dumped.properties.map { property ->
                property.toMap()
            },
            "methods" to dumped.methods.map { method -> method.toMap() },
            "userLog" to board.userLog.map { entry ->
                linkedMapOf(
                    "type" to entry.type,
                    "id" to entry.elementId,
                    "start" to entry.startMilliseconds,
                    "end" to entry.endMilliseconds,
                )
            },
        )
        return serializeJSON(serializable, limits)
    }

    // JSXGraph 1.13.3: src/utils/dump.js -> toJavaScript.
    fun toJavaScript(
        board: Board,
        options: Map<String, Any?> = emptyMap(),
        limits: DumpLimits = DumpLimits(),
    ): GMResult<String, DumpError> {
        val dumped = when (
            val result = dump(
                board = board,
                javaScript = true,
                options = options,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val methods = dumped.methods.toMutableList()
        setBoundingBox(methods, board, "board")
        val output = BoundedOutput(limits.maxOutputLength)

        for (element in dumped.elements) {
            val attributes = when (
                val result = serializeJSON(element.attributes, limits)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            output.line(
                "board.create(\"${element.type}\", [" +
                    element.parents.joinToString(", ", transform = ::jsString) +
                    "], $attributes);",
            )?.let { return GMResult.Err(it) }
            if (element.type == "axis") {
                val id = element.attributes["id"] as? String
                val line = id?.let(board.objects::get) as? Line
                if (line != null && line.defaultTicks == null) {
                    output.line(
                        "board.objects[\"$id\"].removeTicks(" +
                            "board.objects[\"$id\"].defaultTicks);",
                    )?.let { return GMResult.Err(it) }
                }
            }
        }
        for (method in methods) {
            val params = mutableListOf<String>()
            for (parameter in method.params) {
                when (val result = serializeJSON(parameter, limits)) {
                    is GMResult.Ok -> params += result.value
                    is GMResult.Err -> return result
                }
            }
            output.line(
                "${method.obj}.${method.method}(${params.joinToString(", ")});",
            )?.let { return GMResult.Err(it) }
            output.line("")?.let { return GMResult.Err(it) }
        }
        for (property in dumped.properties) {
            val value = when (
                val result = serializeJSON(property.value, limits)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            output.line(
                "${property.obj}.${property.property} = $value;",
            )?.let { return GMResult.Err(it) }
            output.line("")?.let { return GMResult.Err(it) }
        }
        return GMResult.Ok(output.value())
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

    private fun optionalStringMap(
        value: Any?,
        context: String,
    ): GMResult<Map<String, Any?>?, DumpError> =
        if (!Type.exists(value)) {
            GMResult.Ok(null)
        } else {
            when (val result = stringMap(value, context)) {
                is GMResult.Ok -> GMResult.Ok(result.value)
                is GMResult.Err -> result
            }
        }

    private fun stringMap(
        value: Any?,
        context: String,
    ): GMResult<Map<String, Any?>, DumpError> {
        val source = value as? Map<*, *>
            ?: return GMResult.Err(
                DumpError.InvalidAttributeObject(context, value),
            )
        val result = linkedMapOf<String, Any?>()
        for ((key, entryValue) in source) {
            if (key !is String) {
                return GMResult.Err(
                    DumpError.InvalidAttributeObject(context, value),
                )
            }
            result[key] = entryValue
        }
        return GMResult.Ok(result)
    }

    private fun mutableStringMap(
        value: Any?,
        context: String,
    ): GMResult<MutableMap<String, Any?>, DumpError> =
        when (val result = stringMap(value, context)) {
            is GMResult.Ok -> GMResult.Ok(result.value.toMutableMap())
            is GMResult.Err -> result
        }

    private fun jsStrictEquals(
        first: Any?,
        second: Any?,
    ): Boolean =
        when {
            first === second -> true
            first is Number && second is Number ->
                first.toDouble() == second.toDouble()
            first is String && second is String -> first == second
            first is Boolean && second is Boolean -> first == second
            else -> false
        }

    private fun isArrayValue(value: Any?): Boolean =
        arrayValues(value) != null

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

    private fun GeometryElement.attributeSnapshot(): DumpElementSnapshot =
        DumpElementSnapshot(
            id = id,
            name = name,
            elType = elType,
            attributes = getAttributes(),
            subs = dumpSubElements().mapValuesTo(linkedMapOf()) {
                (_, element) ->
                DumpElementSnapshot(
                    id = element.id,
                    name = element.name,
                    elType = element.elType,
                    attributes = element.getAttributes(),
                )
            },
        )

    private fun prepareCodeParent(
        value: Any?,
        javaScript: Boolean,
    ): GMResult<Any?, DumpError> =
        when (value) {
            is JessieCodeRuntimeValue.FunctionValue ->
                (
                    if (javaScript) {
                        value.javaScriptSourceExpression
                    } else {
                        value.sourceExpression
                    }
                    )?.let {
                    GMResult.Ok(DumpCodeExpression(it))
                } ?: GMResult.Err(DumpError.UnsupportedValue)
            is JessieCodeSourceBackedFunction ->
                (
                    if (javaScript) {
                        value.javaScriptSourceExpression
                    } else {
                        value.sourceExpression
                    }
                    )?.let {
                    GMResult.Ok(DumpCodeExpression(it))
                } ?: GMResult.Err(DumpError.UnsupportedValue)
            is String ->
                if (
                    value.startsWith("'") ||
                    value.startsWith("\"")
                ) {
                    GMResult.Ok(value)
                } else {
                    GMResult.Ok("\"$value\"")
                }
            null,
            DumpUndefined,
            JsonNull,
            is Number,
            is Boolean,
            -> GMResult.Ok(value)
            is JsonPrimitive ->
                if (value.isString) {
                    prepareCodeParent(value.content, javaScript)
                } else {
                    GMResult.Ok(value)
                }
            else -> {
                val values = arrayValues(value)
                    ?: return GMResult.Err(DumpError.UnsupportedValue)
                when (
                    val result = jsArrayToString(values, javaScript)
                ) {
                    is GMResult.Ok -> GMResult.Ok("[${result.value}]")
                    is GMResult.Err -> result
                }
            }
        }

    private fun jsArrayToString(
        values: List<Any?>,
        javaScript: Boolean,
    ): GMResult<String, DumpError> {
        val output = mutableListOf<String>()
        for (value in values) {
            when {
                value == null ||
                    value === DumpUndefined ||
                    value === JsonNull ->
                    output += ""
                sourceExpression(value, javaScript) != null ->
                    output +=
                        sourceExpression(value, javaScript).orEmpty()
                arrayValues(value) != null ->
                    when (
                        val result =
                            jsArrayToString(
                                arrayValues(value).orEmpty(),
                                javaScript,
                            )
                    ) {
                        is GMResult.Ok -> output += result.value
                        is GMResult.Err -> return result
                    }
                value is String ||
                    value is Number ||
                    value is Boolean ||
                    value is JsonPrimitive ->
                    output += jsString(value)
                else -> return GMResult.Err(DumpError.UnsupportedValue)
            }
        }
        return GMResult.Ok(output.joinToString(","))
    }

    private fun jsString(value: Any?): String =
        when (value) {
            null -> "null"
            DumpUndefined -> "undefined"
            JsonNull -> "null"
            is DumpCodeExpression -> value.source
            is String -> value
            is Number -> JsNumberFormat.compact(value.toDouble())
            is Boolean -> value.toString()
            is JsonPrimitive -> value.content
            else -> ""
        }

    private fun normalizeJsonParent(value: Any?): Any? =
        when {
            sourceExpression(value, javaScript = false) != null ||
                sourceExpression(value, javaScript = true) != null -> null
            value is List<*> -> value.map(::normalizeJsonParent)
            value is Array<*> -> value.map(::normalizeJsonParent)
            else -> value
        }

    private fun sourceExpression(
        value: Any?,
        javaScript: Boolean,
    ): String? =
        when (value) {
            is JessieCodeRuntimeValue.FunctionValue ->
                if (javaScript) {
                    value.javaScriptSourceExpression
                } else {
                    value.sourceExpression
                }
            is JessieCodeSourceBackedFunction ->
                if (javaScript) {
                    value.javaScriptSourceExpression
                } else {
                    value.sourceExpression
                }
            else -> null
        }

    private fun DumpedElement.toMap(): Map<String, Any?> =
        linkedMapOf(
            "type" to type,
            "parents" to parents,
            "children" to children,
            "attributes" to attributes,
            "properties" to properties,
            "ancestors" to ancestors,
        )

    private fun DumpPropertyAssignment.toMap(): Map<String, Any?> =
        linkedMapOf(
            "obj" to obj,
            "prop" to property,
            "val" to value,
        )

    private fun DumpMethodCall.toMap(): Map<String, Any?> =
        linkedMapOf(
            "obj" to obj,
            "method" to method,
            "params" to params,
        )

    private fun serializeJSON(
        value: Any?,
        limits: DumpLimits,
    ): GMResult<String, DumpError> =
        when (
            val result = Type.toJSON(
                value = value,
                limits = TypeJsonLimits(
                    maxDepth = limits.maxDepth,
                    maxValues = limits.maxValues,
                    maxOutputLength = limits.maxOutputLength,
                ),
            )
        ) {
            is GMResult.Ok ->
                when (val json = result.value) {
                    is TypeJsonValue.Serialized -> GMResult.Ok(json.value)
                    TypeJsonValue.Undefined -> GMResult.Ok("undefined")
                }
            is GMResult.Err ->
                GMResult.Err(DumpError.Serialization(result.error))
        }

    private fun validateLimits(
        limits: DumpLimits,
    ): DumpError.InvalidLimit? =
        when {
            limits.maxDepth < 0 ->
                DumpError.InvalidLimit("maxDepth", limits.maxDepth)
            limits.maxValues <= 0 ->
                DumpError.InvalidLimit("maxValues", limits.maxValues)
            limits.maxElements <= 0 ->
                DumpError.InvalidLimit(
                    "maxElements",
                    limits.maxElements,
                )
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

    private class BoundedOutput(
        private val limit: Int,
    ) {
        private val output = StringBuilder()
        private var hasLine = false

        fun line(value: String): DumpError.OutputLimitExceeded? {
            val separatorLength = if (hasLine) 1 else 0
            if (
                output.length >
                limit - separatorLength - value.length
            ) {
                return DumpError.OutputLimitExceeded(limit)
            }
            if (hasLine) {
                output.append('\n')
            }
            output.append(value)
            hasLine = true
            return null
        }

        fun value(): String = output.toString()
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
