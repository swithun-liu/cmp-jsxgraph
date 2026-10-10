/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/element.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphLocaleNumberFormatError
import com.swithun.jsxgraph.core.JsxGraphLocaleNumberFormatRequest
import com.swithun.jsxgraph.core.JsxGraphLocaleOptionValue
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.utils.Color
import com.swithun.jsxgraph.core.utils.ColorError
import com.swithun.jsxgraph.core.utils.EventEmitter
import com.swithun.jsxgraph.core.utils.EventHandler
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import com.swithun.jsxgraph.core.utils.Type
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.math.ceil
import kotlin.math.floor

internal sealed interface GeometryElementEvaluationError {
    data class CallbackFailed(
        val message: String,
    ) : GeometryElementEvaluationError
}

internal sealed interface GeometryElementPositionError {
    data class InvalidCoordinateCount(
        val count: Int,
    ) : GeometryElementPositionError

    data class Transformation(
        val error: TransformationError,
    ) : GeometryElementPositionError

    data class GliderUpdate(
        val error: GliderError,
    ) : GeometryElementPositionError
}

internal sealed interface GeometryElementAnimationError {
    data class InvalidDuration(
        val milliseconds: Double,
    ) : GeometryElementAnimationError

    data class InvalidColor(
        val property: String,
        val error: ColorError,
    ) : GeometryElementAnimationError

    data class CallbackFailed(
        val message: String,
    ) : GeometryElementAnimationError
}

internal sealed interface GeometryElementLabelError {
    data class Creation(
        val error: TextError,
    ) : GeometryElementLabelError
}

internal sealed interface GeometryElementDumpError {
    data class ParentEvaluation(
        val elementId: String,
        val reason: String,
    ) : GeometryElementDumpError
}

internal fun interface GeometryElementPropertyEvaluator {
    fun evaluate(element: GeometryElement): Any?
}

internal fun interface GeometryElementPropertyResultEvaluator {
    fun evaluate(
        element: GeometryElement,
    ): GMResult<Any?, GeometryElementEvaluationError>
}

internal data class GeometryElementAnimationOptions(
    val callback: (() -> Unit)? = null,
)

/**
 * Initial lifecycle and dependency slice of JXG.GeometryElement.
 *
 * Browser renderer nodes are represented by platform-neutral state. The
 * portable label, trace, event, visibility, and animation contracts mirror
 * the upstream element lifecycle.
 * This class stays internal until those contracts are available.
 */
internal open class GeometryElement(
    internal val board: Board,
    internal var id: String = "",
    name: String? = null,
    internal var type: Int = 0,
    internal val elementClass: Int = Const.OBJECT_CLASS_OTHER,
    internal var needsRegularUpdate: Boolean = true,
) {
    // JSXGraph: src/base/element.js -> elType / _org_type
    internal var elType: String = ""
    internal val originalType: Int = type

    // JSXGraph: src/base/element.js -> transformations / baseElement
    internal val transformations = mutableListOf<Transformation>()
    internal var baseElement: GeometryElement? = null

    // JSXGraph: src/base/element.js -> name / getName
    internal var name: String = name ?: ""
        private set
    private var needsGeneratedName: Boolean = name == null

    internal var needsUpdate: Boolean = true
    internal var positionInBoard: Int = -1
    internal var isDraggable: Boolean = false
    // JSXGraph 1.13.3: src/base/element.js -> GeometryElement constructor.
    internal var hasLabel: Boolean = false
    internal var elementLabel: Text? = null
        private set
    internal var labelError: GeometryElementLabelError? = null
        private set
    internal var highlighted: Boolean = false
    private var highlightHandler: ((force: Boolean) -> Unit)? = null
    private var noHighlightHandler: (() -> Unit)? = null
    internal var mouseover: Boolean = false
    internal var hitTestEnabled: Boolean = true
    internal var displayed: Boolean = false
        private set
    // JSXGraph 1.13.3: src/base/element.js -> inherits.
    internal open val inherits = mutableListOf<GeometryElement>()
    internal val notExistingParents =
        linkedMapOf<String, GeometryElement>()
    internal val traces = linkedMapOf<String, Any?>()
    internal var numTraces: Int = 0
    internal val visualPropertiesCalculated =
        linkedMapOf<String, Any?>("visible" to false)
    internal var propertyEvaluationError:
        GeometryElementEvaluationError? = null
        private set
    internal var positionError: GeometryElementPositionError? = null
        private set
    internal var animationError: GeometryElementAnimationError? = null
        private set
    internal var localeNumberFormatError:
        JsxGraphLocaleNumberFormatError? = null
        private set
    internal val animationData =
        linkedMapOf<String, MutableList<Any?>>()
    private var animationCallback: (() -> Unit)? = null
    private var animationFrameIndex: Int = 0
    private val events = EventEmitter(this)
    // JSXGraph: src/base/element.js -> dump
    internal var dump: Boolean = true
    // JSXGraph: src/base/element.js -> visProp.
    internal val visualProperties = linkedMapOf<String, Any?>()
    // JSXGraph 1.13.3: src/jxg.js -> JSXGraph.createElement attribute copy.
    // The translated engine keeps source attributes separately for scene
    // construction, so Dump mirrors that source state here.
    private val dumpCreationAttributes = linkedMapOf<String, Any?>()
    // JSXGraph 1.13.3: src/base/board.js -> emulateColorblindness /
    // visPropOriginal.
    internal var originalVisualProperties: Map<String, Any?>? = null
    // JSXGraph 1.13.3: src/utils/type.js -> providePoints /
    // providePoints3D `_is_new` helper marker.
    internal var isNewByProvision: Boolean = false
    // JSXGraph 1.13.3: src/utils/dump.js -> addMarkers / deleteMarkers.
    internal val dumpMarkers = linkedMapOf<String, Any?>()

    // JSXGraph: src/base/element.js -> stdform
    internal var stdform = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0)

    // JSXGraph: src/base/element.js -> quadraticform
    internal var quadraticform = arrayOf(
        doubleArrayOf(1.0, 0.0, 0.0),
        doubleArrayOf(0.0, 1.0, 0.0),
        doubleArrayOf(0.0, 0.0, 1.0),
    )

    internal val childElements = linkedMapOf<String, GeometryElement>()
    internal val descendants = linkedMapOf<String, GeometryElement>()
    internal val ancestors = linkedMapOf<String, GeometryElement>()
    // JSXGraph 1.13.3: src/base/element.js -> symbolic.
    internal val symbolic = linkedMapOf<String, String>()
    // JSXGraph 1.13.3: src/base/element.js -> addTicks / removeTicks /
    // removeAllTicks. Only Line and Curve parents accept entries.
    internal val ticks = mutableListOf<Ticks>()
    // JSXGraph: src/base/element.js -> parents / getParents
    internal val parents = mutableListOf<String>()

    // JSXGraph: src/base/element.js -> GeometryElement name initialization
    internal fun finalizeName() {
        if (needsGeneratedName) {
            name = board.generateName(this)
            needsGeneratedName = false
        }
        if (name.isNotEmpty()) {
            board.elementsByName[name] = this
        }
    }

    // JSXGraph 1.13.3: src/base/element.js -> getName.
    internal fun nameValue(): String = name

    // JSXGraph: src/base/element.js -> setName.
    internal fun setName(value: String): GeometryElement {
        val sanitized = Type.sanitizeHTML(value)
        if (elType != "slider") {
            setLabelText(sanitized)
        }
        assignName(sanitized)
        return this
    }

    private fun assignName(value: String) {
        board.elementsByName.remove(name)
        name = value
        needsGeneratedName = false
        board.elementsByName[name] = this
    }

    // JSXGraph: src/base/element.js -> setAttribute.
    internal open fun setAttribute(
        attributes: Map<String, Any?>,
    ): GeometryElement {
        val resolved = resolveShortcuts(attributes)
        for ((rawKey, rawValue) in resolved) {
            val key = normalizeAttributeKey(rawKey)
            val value = normalizeBooleanString(rawValue)
            val oldValue = copyAttributeValue(getAttribute(key))
            when (key) {
                "name" -> if (value is String) {
                    assignName(value)
                }
                "needsregularupdate" -> {
                    needsRegularUpdate = value != false
                    setVisualProperty(key, needsRegularUpdate)
                }
                "visible" -> {
                    setVisualProperty(key, value)
                    setDisplayRendNode(evalVisProp(key) as? Boolean)
                }
                "withlabel" -> {
                    setVisualProperty(key, value)
                    val enabled = evalVisProp(key) == true
                    if (enabled && elementLabel == null) {
                        createLabel()
                    }
                    elementLabel?.setAttribute(
                        mapOf(
                            "visible" to
                                if (enabled) "inherit" else false,
                        ),
                    )
                    hasLabel = enabled
                }
                "labelcolor" -> {
                    setVisualProperty(key, value)
                    elementLabel?.setAttribute(
                        mapOf("strokecolor" to value),
                    )
                }
                "face" -> {
                    // JSXGraph 1.13.3 only applies runtime face changes to
                    // Point elements. Curves such as Grid retain their
                    // creation-time face.
                    if (this is Point) {
                        setVisualProperty(key, value)
                    }
                }
                "trace" -> when (value) {
                    false -> {
                        clearTrace()
                        setVisualProperty(key, false)
                    }
                    "pause" -> setVisualProperty(key, false)
                    else -> setVisualProperty(key, true)
                }
                else -> setVisualProperty(key, value)
            }
            trigger(
                events = listOf("attribute:$key"),
                arguments = listOf(oldValue, value, this),
            )
        }
        trigger(
            events = listOf("attribute"),
            arguments = listOf(resolved, this),
        )
        prepareUpdate()
        return this
    }

    // JSXGraph 1.13.3: src/base/element.js -> resolveShortcuts.
    internal fun resolveShortcuts(
        attributes: Map<String, Any?>,
    ): Map<String, Any?> {
        val resolved = attributes.entries.associateTo(linkedMapOf()) {
            (key, value) ->
            key to copyAttributeValue(value)
        }
        for ((shortcut, expandedKeys) in ATTRIBUTE_SHORTCUTS) {
            val matchingKey = resolved.keys.firstOrNull {
                normalizeAttributeKey(it) == shortcut
            } ?: continue
            for (expandedKey in expandedKeys) {
                if (
                    resolved.keys.none {
                        normalizeAttributeKey(it) ==
                            normalizeAttributeKey(expandedKey)
                    }
                ) {
                    resolved[expandedKey] =
                        copyAttributeValue(resolved[matchingKey])
                }
            }
        }
        for (traceKey in listOf("traceattributes", "traceAttributes")) {
            val traceAttributes =
                resolved[traceKey] as? Map<*, *> ?: continue
            resolved[traceKey] = resolveShortcuts(
                traceAttributes.entries.associate {
                    (key, value) -> key.toString() to value
                },
            )
        }
        return resolved
    }

    // JSXGraph 1.13.3: src/base/element.js -> setProperty.
    internal fun setProperty(
        attributes: Map<String, Any?>,
    ): GeometryElement = setAttribute(attributes)

    // JSXGraph: src/base/element.js -> getAttribute.
    internal open fun getAttribute(key: String): Any? =
        when (val normalized = normalizeAttributeKey(key)) {
            "needsregularupdate" -> needsRegularUpdate
            "name" -> name
            "id" -> id
            "labelcolor" ->
                elementLabel?.getAttribute("strokecolor")
                    ?: getVisualProperty(normalized)
            "withlabel" -> hasLabel
            else -> getVisualProperty(normalized)
        }

    // JSXGraph 1.13.3: src/base/element.js -> getProperty.
    internal fun getProperty(key: String): Any? = getAttribute(key)

    // JSXGraph: src/base/element.js -> getAttributes.
    internal open fun getAttributes(): Map<String, Any?> =
        copyAttributeMap(dumpCreationAttributes).toMutableMap().apply {
            putAll(copyAttributeMap(visualProperties))
            this["id"] = id
            this["name"] = name
        }

    internal fun setDumpCreationAttributes(
        attributes: Map<String, Any?>,
    ): GeometryElement {
        dumpCreationAttributes.clear()
        for ((key, value) in attributes) {
            dumpCreationAttributes[normalizeAttributeKey(key)] =
                copyDumpAttributeValue(value)
        }
        return this
    }

    private fun setVisualProperty(
        key: String,
        value: Any?,
    ) {
        val path = key.split('.')
        if (path.size == 1) {
            val existing = visualProperties[key]
            visualProperties[key] =
                if (existing is MutableMap<*, *> && value is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    mergeAttributeMaps(
                        existing as MutableMap<String, Any?>,
                        value,
                    )
                } else {
                    copyAttributeValue(value)
                }
            return
        }
        var current: MutableMap<String, Any?> = visualProperties
        for (segment in path.dropLast(1)) {
            val nested = current[segment] as? MutableMap<*, *>
            val next =
                if (nested == null) {
                    linkedMapOf<String, Any?>().also {
                        current[segment] = it
                    }
                } else {
                    @Suppress("UNCHECKED_CAST")
                    (nested as MutableMap<String, Any?>)
                }
            current = next
        }
        current[path.last()] = copyAttributeValue(value)
    }

    // JSXGraph 1.13.3: src/base/element.js -> _set.
    internal fun _set(
        key: String,
        value: Any?,
    ): GeometryElement {
        val normalized = normalizeAttributeKey(key)
        if (
            "color" in normalized &&
            value is String &&
            value.length == 9 &&
            value.startsWith('#')
        ) {
            when (val result = Color.rgba2rgbo(value)) {
                is GMResult.Ok -> {
                    setVisualProperty(normalized, result.value.rgb)
                    setVisualProperty(
                        normalized.replace("color", "opacity"),
                        result.value.opacity,
                    )
                }
                is GMResult.Err -> setVisualProperty(normalized, value)
            }
        } else {
            setVisualProperty(normalized, value)
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/element.js -> evalVisProp.
    internal fun evalVisProp(key: String): Any? {
        val normalized = normalizeAttributeKey(key)
        var value = eval(getVisualProperty(normalized))
        if (
            value == "inherit" &&
            normalized != "visible" &&
            normalized != "showinfobox"
        ) {
            value = inheritedVisualProperty(normalized)
        }
        return value
    }

    private fun inheritedVisualProperty(key: String): Any? {
        for (candidate in descendants.values + ancestors.values) {
            if (this !in candidate.inherits) {
                continue
            }
            val inherited = candidate.evalVisProp(key)
            if (inherited != "inherit") {
                return inherited
            }
        }
        return "inherit"
    }

    // JSXGraph 1.13.3: src/base/element.js -> eval.
    internal fun eval(value: Any?): Any? =
        when (val result = evalResult(value)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> null
        }

    internal fun evalResult(
        value: Any?,
    ): GMResult<Any?, GeometryElementEvaluationError> {
        if (value is GeometryElementPropertyResultEvaluator) {
            val result = try {
                value.evaluate(evaluationTarget())
            } catch (exception: Exception) {
                GMResult.Err(
                    GeometryElementEvaluationError.CallbackFailed(
                        exception.message
                            ?: exception::class.simpleName.orEmpty(),
                    ),
                )
            }
            propertyEvaluationError = when (result) {
                is GMResult.Ok -> null
                is GMResult.Err -> result.error
            }
            return result
        }
        val evaluated = try {
            when (value) {
                is GeometryElementPropertyEvaluator ->
                    value.evaluate(evaluationTarget())
                is Function0<*> -> value.invoke()
                is Function1<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    (value as (GeometryElement) -> Any?).invoke(
                        evaluationTarget(),
                    )
                }
                else -> value
            }
        } catch (exception: Exception) {
            val error = GeometryElementEvaluationError.CallbackFailed(
                exception.message
                    ?: exception::class.simpleName.orEmpty(),
            )
            propertyEvaluationError = error
            return GMResult.Err(error)
        }
        propertyEvaluationError = null
        return GMResult.Ok(evaluated)
    }

    private fun evaluationTarget(): GeometryElement =
        when {
            visualProperties["islabel"] == true ->
                visualProperties["anchor"] as? GeometryElement ?: this
            visualProperties["element3d"] is GeometryElement ->
                visualProperties["element3d"] as GeometryElement
            else -> this
        }

    private fun getVisualProperty(key: String): Any? {
        var current: Any? = visualProperties
        for (segment in key.split('.')) {
            current = (current as? Map<*, *>)?.get(segment)
                ?: return null
        }
        return current
    }

    // JSXGraph: src/base/element.js -> addChild
    internal fun addChild(element: GeometryElement): GeometryElement {
        childElements[element.id] = element
        addDescendants(element)
        element.ancestors[id] = this

        for (descendant in descendants.values) {
            descendant.ancestors[id] = this
            for (ancestor in ancestors.values) {
                descendant.ancestors[ancestor.id] = ancestor
            }
        }
        for (ancestor in ancestors.values) {
            for (descendant in descendants.values) {
                ancestor.descendants[descendant.id] = descendant
            }
        }
        return this
    }

    // JSXGraph: src/base/element.js -> addDescendants
    private fun addDescendants(element: GeometryElement) {
        descendants[element.id] = element
        for (child in element.childElements.values) {
            addDescendants(child)
        }
    }

    // JSXGraph: src/base/element.js -> addParents
    internal fun addParents(elements: Iterable<GeometryElement>): GeometryElement {
        for (element in elements) {
            if (element.id !in parents) {
                parents += element.id
            }
        }
        return this
    }

    // JSXGraph: src/base/element.js -> addParents
    internal fun addParentIds(elementIds: Iterable<String>): GeometryElement {
        for (elementId in elementIds) {
            if (elementId in board.objects && elementId !in parents) {
                parents += elementId
            }
        }
        return this
    }

    // JSXGraph: src/base/element.js -> addParentsFromJCFunctions
    internal fun addParentsFromJCFunctions(
        functions: Iterable<JessieCodeCoordinateFunction>,
    ): GeometryElement {
        for (function in functions) {
            for (dependency in function.dependencies.values) {
                dependency.addChild(this)
            }
        }
        return this
    }

    // JSXGraph: src/base/element.js -> setParents
    internal fun setParents(elements: Iterable<GeometryElement>): GeometryElement {
        parents.clear()
        return addParents(elements)
    }

    // JSXGraph: src/base/element.js -> removeChild
    internal fun removeChild(element: GeometryElement): GeometryElement {
        childElements.remove(element.id)
        removeDescendants(element)
        element.ancestors.remove(id)
        return this
    }

    // JSXGraph: src/base/element.js -> removeDescendants
    private fun removeDescendants(element: GeometryElement) {
        descendants.remove(element.id)
        for (child in element.childElements.values) {
            removeDescendants(child)
        }
    }

    // JSXGraph: src/base/element.js -> countChildren
    internal fun countChildren(): Int =
        childElements.keys.count { childId -> "Label" !in childId }

    // JSXGraph 1.13.3: src/base/element.js -> draggable.
    internal fun draggable(): Boolean =
        isDraggable &&
            evalVisProp("fixed") != true &&
            type != Const.OBJECT_TYPE_GLIDER

    // JSXGraph 1.13.3: src/base/element.js -> addTransform /
    // removeTransform / clearTransforms. The base implementations are
    // intentional no-ops upstream; concrete geometry types own their
    // defining-point transformation semantics.
    internal fun addTransformBase(
        transformation: Transformation,
    ): GeometryElement = this

    internal fun removeTransformBase(
        transformation: Transformation,
    ): GeometryElement = this

    internal fun clearTransformsBase(): GeometryElement = this

    // JSXGraph 1.13.3: src/base/element.js -> setPosition.
    internal open fun setPosition(
        method: Int,
        coordinates: DoubleArray,
    ): GeometryElement {
        when (val result = setPositionResult(method, coordinates)) {
            is GMResult.Ok -> positionError = null
            is GMResult.Err -> positionError = result.error
        }
        return this
    }

    internal fun setPositionResult(
        method: Int,
        coordinates: DoubleArray,
    ): GMResult<GeometryElement, GeometryElementPositionError> {
        if (coordinates.size !in setOf(2, 3)) {
            return GMResult.Err(
                GeometryElementPositionError.InvalidCoordinateCount(
                    coordinates.size,
                ),
            )
        }
        val movableParents = parents.mapNotNull(board::select)
            .filterIsInstance<Point>()
        if (movableParents.any { !it.draggable() }) {
            return GMResult.Ok(this)
        }
        val translated = Coords(
            method = method,
            coordinates = coordinates,
            board = board,
            emitter = false,
        )
        val translation = translated.usrCoords.copyOfRange(1, 3)
        val transformation = when (
            val result = Transformation.create(
                type = "translate",
                parameters = translation,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                GeometryElementPositionError.Transformation(result.error),
            )
        }
        if (movableParents.isNotEmpty()) {
            when (val result = transformation.applyOnce(movableParents)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    GeometryElementPositionError.Transformation(
                        result.error,
                    ),
                )
            }
            for (parent in movableParents) {
                val glider = parent as? Glider ?: continue
                when (val result = glider.updateGlider()) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return GMResult.Err(
                        GeometryElementPositionError.GliderUpdate(
                            result.error,
                        ),
                    )
                }
            }
        } else {
            val previous = transformations.lastOrNull()
            if (previous != null && previous.isNumericMatrix) {
                when (val result = previous.melt(transformation)) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return GMResult.Err(
                        GeometryElementPositionError.Transformation(
                            result.error,
                        ),
                    )
                }
            } else {
                addElementTransformation(transformation)
            }
        }
        positionError = null
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/element.js -> setPositionDirectly.
    internal fun setPositionDirectlyBase(
        method: Int,
        coordinates: DoubleArray,
        oldCoordinates: DoubleArray,
    ): GMResult<GeometryElement, GeometryElementPositionError> {
        if (
            coordinates.size !in setOf(2, 3) ||
            oldCoordinates.size !in setOf(2, 3)
        ) {
            return GMResult.Err(
                GeometryElementPositionError.InvalidCoordinateCount(
                    minOf(coordinates.size, oldCoordinates.size),
                ),
            )
        }
        val current = Coords(
            method = method,
            coordinates = coordinates,
            board = board,
            emitter = false,
        ).usrCoords
        val previous = Coords(
            method = method,
            coordinates = oldCoordinates,
            board = board,
            emitter = false,
        ).usrCoords
        return setPositionResult(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(
                current[1] - previous[1],
                current[2] - previous[2],
            ),
        )
    }

    private fun addElementTransformation(
        transformation: Transformation,
    ) {
        when (this) {
            is Line -> addTransform(transformation)
            is Circle -> addTransform(transformation)
            is Curve -> addTransform(transformation)
            else -> transformations += transformation
        }
    }

    // JSXGraph 1.13.3: src/base/element.js -> toTopOfLayer.
    // Compose owns draw order in the immutable scene, so there is no
    // renderer node to reorder in core.
    internal fun toTopOfLayer(): GeometryElement = this

    // JSXGraph 1.13.3: src/base/element.js -> generatePolynomial.
    internal open fun generatePolynomial(
        point: Point? = null,
    ): List<String> = emptyList()

    // JSXGraph 1.13.3: src/base/element.js -> animate.
    internal fun animate(
        targetAttributes: Map<String, Any?>,
        timeMilliseconds: Double,
        options: GeometryElementAnimationOptions =
            GeometryElementAnimationOptions(),
    ): GeometryElement {
        animationError = null
        when (
            val result = animateResult(
                targetAttributes,
                timeMilliseconds,
                options,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> animationError = result.error
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/element.js ->
    // animate closures animateColor / animateFloat.
    internal fun animateResult(
        targetAttributes: Map<String, Any?>,
        timeMilliseconds: Double,
        options: GeometryElementAnimationOptions =
            GeometryElementAnimationOptions(),
    ): GMResult<GeometryElement, GeometryElementAnimationError> {
        if (!timeMilliseconds.isFinite() || timeMilliseconds < 0.0) {
            return GMResult.Err(
                GeometryElementAnimationError.InvalidDuration(
                    timeMilliseconds,
                ),
            )
        }
        val steps = maxOf(
            ceil(timeMilliseconds / board.animationDelay).toInt(),
            1,
        )
        animationData.clear()
        for ((rawProperty, target) in targetAttributes) {
            val property = normalizeAttributeKey(rawProperty)
            when (property) {
                "strokecolor", "fillcolor" -> {
                    val start = evalVisProp(property) as? String ?: continue
                    val end = target as? String ?: continue
                    val startHsv = when (val result = Color.rgb2hsv(start)) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return GMResult.Err(
                            GeometryElementAnimationError.InvalidColor(
                                property,
                                result.error,
                            ),
                        )
                    }
                    val endHsv = when (val result = Color.rgb2hsv(end)) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return GMResult.Err(
                            GeometryElementAnimationError.InvalidColor(
                                property,
                                result.error,
                            ),
                        )
                    }
                    animationData[property] = MutableList(steps) { index ->
                        val fraction = (index + 1).toDouble() / steps
                        Color.hsv2rgb(
                            startHsv[0] +
                                (endHsv[0] - startHsv[0]) * fraction,
                            startHsv[1] +
                                (endHsv[1] - startHsv[1]) * fraction,
                            startHsv[2] +
                                (endHsv[2] - startHsv[2]) * fraction,
                        )
                    }
                }
                "size" -> if (this is Point) {
                    addNumericAnimation(
                        property = property,
                        target = target,
                        steps = steps,
                        roundDown = true,
                    )
                }
                "strokeopacity",
                "strokewidth",
                "fillopacity",
                -> addNumericAnimation(
                    property = property,
                    target = target,
                    steps = steps,
                    roundDown = false,
                )
            }
        }
        animationCallback = options.callback
        animationFrameIndex = 0
        board.addAnimation(this)
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/board.js -> animate animationData branch.
    internal fun advanceAnimationFrame(): Boolean {
        val frameCount =
            animationData.values.maxOfOrNull { it.size } ?: 0
        if (animationFrameIndex < frameCount) {
            val attributes = animationData.mapNotNull { (property, values) ->
                values.getOrNull(animationFrameIndex)?.let {
                    property to it
                }
            }.toMap()
            if (attributes.isNotEmpty()) {
                setAttribute(attributes)
            }
            animationFrameIndex += 1
            // src/base/board.js removes an exhausted animationData object and
            // invokes its callback on the tick after the final value is used.
            return true
        }
        animationData.clear()
        animationFrameIndex = 0
        val callback = animationCallback
        animationCallback = null
        if (callback != null) {
            try {
                callback()
            } catch (exception: Exception) {
                animationError =
                    GeometryElementAnimationError.CallbackFailed(
                        exception.message
                            ?: exception::class.simpleName.orEmpty(),
                    )
            }
        }
        return false
    }

    internal fun cancelAnimation() {
        animationData.clear()
        animationFrameIndex = 0
        animationCallback = null
    }

    internal fun takeAnimationError(): GeometryElementAnimationError? {
        val error = animationError
        animationError = null
        return error
    }

    private fun addNumericAnimation(
        property: String,
        target: Any?,
        steps: Int,
        roundDown: Boolean,
    ) {
        val start = parseAnimationNumber(evalVisProp(property)) ?: return
        val end = parseAnimationNumber(target) ?: return
        animationData[property] = MutableList(steps) { index ->
            val fraction = (index + 1).toDouble() / steps
            val value = start + (end - start) * fraction
            if (roundDown) floor(value) else value
        }
    }

    // JSXGraph 1.13.3: src/base/element.js ->
    // setDisplayRendNode / hide / hideElement / show / showElement.
    internal fun setDisplayRendNode(value: Boolean? = null): GeometryElement {
        val visible = value
            ?: (visualPropertiesCalculated["visible"] as? Boolean)
            ?: false
        displayed = visible
        visualPropertiesCalculated["visible"] = visible
        for (inherited in inherits) {
            if (inherited.evalVisProp("visible") == "inherit") {
                inherited.setDisplayRendNode(visible)
            }
        }
        if (elementLabel?.evalVisProp("visible") == "inherit") {
            elementLabel?.setDisplayRendNode(visible)
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/element.js -> hide / show.
    internal fun hide(): GeometryElement =
        setAttribute(mapOf("visible" to false))

    internal open fun hideElement(): GeometryElement = hide()

    internal fun show(): GeometryElement =
        setAttribute(mapOf("visible" to true))

    internal open fun showElement(): GeometryElement = show()

    // JSXGraph 1.13.3: src/base/element.js -> setLabel / setLabelText.
    internal fun setLabel(value: String): GeometryElement {
        if (!hasLabel) {
            setAttribute(mapOf("withlabel" to true))
        }
        return setLabelText(value)
    }

    internal fun setLabelText(value: String): GeometryElement {
        elementLabel?.setText(Type.sanitizeHTML(value))
        return this
    }

    // JSXGraph 1.13.3: src/base/element.js ->
    // getTextAnchor / getLabelAnchor.
    internal open fun getTextAnchor(): Coords =
        Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 0.0),
            board = board,
            emitter = false,
        )

    internal open fun getLabelAnchor(): Coords = getTextAnchor()

    // JSXGraph 1.13.3: src/base/element.js -> createLabel.
    internal fun createLabel(): GeometryElement {
        when (val result = createLabelResult()) {
            is GMResult.Ok -> labelError = null
            is GMResult.Err -> labelError = result.error
        }
        return this
    }

    internal fun createLabelResult():
        GMResult<Text, GeometryElementLabelError> {
        elementLabel?.let { return GMResult.Ok(it) }
        if (evalVisProp("withlabel") != true) {
            return GMResult.Err(
                GeometryElementLabelError.Creation(
                    TextError.UnsupportedParsedContent("withlabel=false"),
                ),
            )
        }
        val labelAttributes =
            (getVisualProperty("label") as? Map<*, *>)
                ?.entries
                ?.associate { (key, value) ->
                    key.toString() to value
                }
                .orEmpty()
        val label = when (
            val result = Text.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                content = name,
                id = "${id}Label",
                name = "",
                parse = false,
                anchor = this,
                isLabel = true,
                visible = true,
                strokeColor =
                    labelAttributes["strokecolor"] as? String,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                GeometryElementLabelError.Creation(result.error),
            )
        }
        label.elType = "label"
        label.dump = false
        val offset = labelAttributes["offset"]
        label.screenOffset = when (offset) {
            is DoubleArray ->
                offset.copyOf(2)
            is List<*> ->
                doubleArrayOf(
                    (offset.getOrNull(0) as? Number)?.toDouble() ?: 0.0,
                    (offset.getOrNull(1) as? Number)?.toDouble() ?: 0.0,
                )
            else -> doubleArrayOf(0.0, 0.0)
        }
        label.setAttribute(
            labelAttributes + mapOf(
                "islabel" to true,
                "anchor" to this,
                "visible" to "inherit",
            ),
        )
        label.fullUpdate()
        elementLabel = label
        hasLabel = true
        addChild(label)
        return GMResult.Ok(label)
    }

    // JSXGraph 1.13.3: src/base/element.js -> setDash.
    internal fun setDash(value: Number): GeometryElement =
        setAttribute(mapOf("dash" to value))

    // JSXGraph 1.13.3: src/base/element.js -> setArrow.
    internal fun setArrow(
        firstArrow: Boolean,
        lastArrow: Boolean,
    ): GeometryElement {
        setVisualProperty("firstarrow", firstArrow)
        setVisualProperty("lastarrow", lastArrow)
        if (lastArrow) {
            type = Const.OBJECT_TYPE_VECTOR
            elType = "arrow"
        }
        return fullUpdate()
    }

    // JSXGraph 1.13.3: src/base/element.js -> createGradient.
    // Gradient nodes are materialized by the Compose renderer from visProp.
    internal fun createGradient(): GeometryElement = this

    // JSXGraph 1.13.3: src/base/element.js -> highlight / noHighlight.
    internal fun highlight(force: Boolean = false): GeometryElement {
        highlightHandler?.let {
            it(force)
            return this
        }
        if (
            evalVisProp("highlight") != false &&
            (!highlighted || force)
        ) {
            highlighted = true
            board.highlightedObjects[id] = this
        }
        return this
    }

    internal fun noHighlight(): GeometryElement {
        noHighlightHandler?.let {
            it()
            return this
        }
        if (highlighted) {
            highlighted = false
            board.highlightedObjects.remove(id)
        }
        return this
    }

    internal fun installHighlightHandlers(
        onHighlight: (force: Boolean) -> Unit,
        onNoHighlight: () -> Unit,
    ): GeometryElement {
        highlightHandler = onHighlight
        noHighlightHandler = onNoHighlight
        return this
    }

    // JSXGraph 1.13.3: src/base/element.js -> clearTrace.
    internal fun clearTrace(): GeometryElement {
        traces.clear()
        numTraces = 0
        return this
    }

    // JSXGraph 1.13.3: src/base/element.js ->
    // cloneToBackground / snapToGrid / snapToPoints / hasPoint.
    internal open fun cloneToBackground(): GeometryElement = this

    internal open fun snapToGrid(): GeometryElement = this

    internal open fun snapToPoints(): GeometryElement = this

    internal open fun hasPoint(
        screenX: Double,
        screenY: Double,
    ): Boolean {
        if (!hitTestEnabled) {
            return false
        }
        return when (this) {
            is Point -> hasPoint(screenX, screenY, DEFAULT_HIT_PRECISION)
            is Line -> hasPoint(screenX, screenY, DEFAULT_HIT_PRECISION)
            is Circle -> hasPoint(screenX, screenY, DEFAULT_HIT_PRECISION)
            is Arc -> hasPoint(screenX, screenY, DEFAULT_HIT_PRECISION)
            is Sector -> hasPoint(screenX, screenY, DEFAULT_HIT_PRECISION)
            is Polygon -> hasPoint(screenX, screenY, DEFAULT_HIT_PRECISION)
            is Curve -> hasPoint(screenX, screenY, DEFAULT_HIT_PRECISION)
            is Ticks -> hasPoint(screenX, screenY, DEFAULT_HIT_PRECISION)
            is Turtle -> hasPoint(screenX, screenY)
            is Text -> hasPoint(screenX, screenY, DEFAULT_HIT_PRECISION)
            is Image -> hasPoint(screenX, screenY, DEFAULT_HIT_PRECISION)
            is ForeignObject ->
                hasPoint(screenX, screenY, DEFAULT_HIT_PRECISION)
            else -> false
        }
    }

    // JSXGraph 1.13.3: src/base/element.js ->
    // on / off / trigger / addEvent / removeEvent.
    internal fun on(
        event: String,
        handler: EventHandler,
        context: Any = this,
    ): GeometryElement {
        events.on(event, handler, context)
        return this
    }

    internal fun off(
        event: String,
        handler: EventHandler? = null,
    ): GeometryElement {
        events.off(event, handler)
        return this
    }

    internal fun trigger(
        events: List<String>,
        arguments: List<Any?> = emptyList(),
    ): GeometryElement {
        this.events.trigger(events, arguments)
        return this
    }

    internal fun addEvent(
        event: String,
        handler: EventHandler,
        context: Any = this,
    ): GeometryElement = on(event, handler, context)

    internal fun removeEvent(
        event: String,
        handler: EventHandler? = null,
    ): GeometryElement = off(event, handler)

    /*
     * JSXGraph 1.13.3: src/base/element.js -> __evt__over,
     * __evt__mouseover, __evt__out, __evt__mouseout, __evt__move,
     * __evt__mousemove, __evt__drag, __evt__mousedrag, __evt__pendrag,
     * __evt__touchdrag, __evt__keydrag, __evt__down, __evt__mousedown,
     * __evt__pendown, __evt__touchdown, __evt__click, __evt__dblclick,
     * __evt__mouseclick, __evt__mousedblclick, __evt__pointerclick,
     * __evt__pointerdblclick, __evt__up, __evt__mouseup, __evt__penup,
     * __evt__touchup, __evt__attribute, __evt__attribute_, __evt.
     *
     * These upstream methods are documentation-only empty event hooks.
     * Kotlin keeps callable no-ops so platform input adapters expose the same
     * event surface without a browser Event dependency.
     */
    internal fun __evt__over(event: Any?) = consumeEvent(event)
    internal fun __evt__mouseover(event: Any?) = consumeEvent(event)
    internal fun __evt__out(event: Any?) = consumeEvent(event)
    internal fun __evt__mouseout(event: Any?) = consumeEvent(event)
    internal fun __evt__move(event: Any?) = consumeEvent(event)
    internal fun __evt__mousemove(event: Any?) = consumeEvent(event)
    internal fun __evt__drag(event: Any?) = consumeEvent(event)
    internal fun __evt__mousedrag(event: Any?) = consumeEvent(event)
    internal fun __evt__pendrag(event: Any?) = consumeEvent(event)
    internal fun __evt__touchdrag(event: Any?) = consumeEvent(event)
    internal fun __evt__keydrag(event: Any?) = consumeEvent(event)
    internal fun __evt__down(event: Any?) = consumeEvent(event)
    internal fun __evt__mousedown(event: Any?) = consumeEvent(event)
    internal fun __evt__pendown(event: Any?) = consumeEvent(event)
    internal fun __evt__touchdown(event: Any?) = consumeEvent(event)
    internal fun __evt__click(event: Any?) = consumeEvent(event)
    internal fun __evt__dblclick(event: Any?) = consumeEvent(event)
    internal fun __evt__mouseclick(event: Any?) = consumeEvent(event)
    // JSXGraph 1.13.3: src/base/element.js -> __evt__mousedblclick,
    // __evt__pointerclick, __evt__pointerdblclick, __evt__up,
    // __evt__mouseup, __evt__penup, __evt__touchup, __evt__attribute,
    // __evt__attribute_, __evt.
    internal fun __evt__mousedblclick(event: Any?) = consumeEvent(event)
    internal fun __evt__pointerclick(event: Any?) = consumeEvent(event)
    internal fun __evt__pointerdblclick(event: Any?) = consumeEvent(event)
    internal fun __evt__up(event: Any?) = consumeEvent(event)
    internal fun __evt__mouseup(event: Any?) = consumeEvent(event)
    internal fun __evt__penup(event: Any?) = consumeEvent(event)
    internal fun __evt__touchup(event: Any?) = consumeEvent(event)

    internal fun __evt__attribute(
        attributes: Any?,
        element: Any?,
    ) {
        consumeEvent(attributes)
        consumeEvent(element)
    }

    internal fun __evt__attribute_(
        oldValue: Any?,
        newValue: Any?,
        element: Any?,
    ) {
        consumeEvent(oldValue)
        consumeEvent(newValue)
        consumeEvent(element)
    }

    internal fun __evt__() = Unit

    private fun consumeEvent(event: Any?) {
        @Suppress("UNUSED_VARIABLE")
        val upstreamEvent = event
    }

    // JSXGraph 1.13.3: src/base/element.js -> toJSON.
    internal fun toJSON(): String = buildString {
        append("{\"name\":")
        append(jsonValue(name))
        append(", \"id\":")
        append(jsonValue(id))
        append(", \"visProp\":")
        append(jsonValue(visualProperties))
        append('}')
    }

    // JSXGraph 1.13.3: src/base/element.js ->
    // highlightStrokeColor, strokeColor, strokeWidth, fillColor,
    // highlightFillColor, labelColor, dash, visible, shadow.
    internal fun highlightStrokeColor(value: String): GeometryElement =
        setAttribute(mapOf("highlightStrokeColor" to value))

    internal fun strokeColor(value: String): GeometryElement =
        setAttribute(mapOf("strokeColor" to value))

    internal fun strokeWidth(value: Number): GeometryElement =
        setAttribute(mapOf("strokeWidth" to value))

    internal fun fillColor(value: String): GeometryElement =
        setAttribute(mapOf("fillColor" to value))

    internal fun highlightFillColor(value: String): GeometryElement =
        setAttribute(mapOf("highlightFillColor" to value))

    internal fun labelColor(value: String): GeometryElement =
        setAttribute(mapOf("labelColor" to value))

    internal fun dash(value: Number): GeometryElement =
        setAttribute(mapOf("dash" to value))

    internal fun visible(value: Boolean): GeometryElement =
        setAttribute(mapOf("visible" to value))

    internal fun shadow(value: Boolean): GeometryElement =
        setAttribute(mapOf("shadow" to value))

    // JSXGraph 1.13.3: src/base/element.js -> getType / getParents.
    internal fun getType(): String = elType

    internal fun parentIds(): List<String> = parents.toList()

    internal open fun dumpParents():
        GMResult<List<Any?>, GeometryElementDumpError> =
        GMResult.Ok(parents.toList())

    // JSXGraph 1.13.3: src/utils/dump.js -> prepareAttributes(obj.subs).
    // Kotlin keeps the source's dynamic `subs` property behind a virtual
    // accessor so Dump does not need element-type switches.
    internal open fun dumpSubElements(): Map<String, GeometryElement> =
        emptyMap()

    // JSXGraph 1.13.3: src/base/element.js ->
    // addTicks / removeAllTicks / removeTicks.
    internal fun addTicks(value: Ticks): String {
        if (value.id.isEmpty()) {
            value.id = "${id}_ticks_${ticks.size + 1}"
        }
        ticks += value
        return value.id
    }

    internal fun removeAllTicks(): GeometryElement {
        for (tick in ticks.asReversed().toList()) {
            removeTicks(tick)
        }
        board.update()
        return this
    }

    internal fun removeTicks(tick: Ticks): GeometryElement {
        if (this is Line && defaultTicks === tick) {
            defaultTicks = null
        }
        if (ticks.remove(tick)) {
            board.removeObject(tick)
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/element.js -> getBoundingBox.
    internal fun getBoundingBox(): DoubleArray =
        when (this) {
            is Polygon -> bounds()
            is Circle -> bounds()
            is Curve -> bounds()
            is Point -> bounds()
            is Line -> bounds()
            else -> doubleArrayOf(
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            )
        }

    // JSXGraph 1.13.3: src/base/element.js ->
    // formatNumberLocale / useLocale.
    internal fun formatNumberLocale(
        value: Number,
        digits: Int? = null,
    ): Any =
        when (val result = formatNumberLocaleResult(value, digits)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> value
        }

    // JavaScript string concatenation coerces the numeric fallback returned
    // by formatNumberLocale when Intl is unavailable or rejects the request.
    internal fun formatNumberLocaleText(
        value: Number,
        digits: Int? = null,
    ): String =
        when (val formatted = formatNumberLocale(value, digits)) {
            is String -> formatted
            is Number -> JsNumberFormat.compact(formatted.toDouble())
            else -> formatted.toString()
        }

    internal fun formatNumberLocaleResult(
        value: Number,
        digits: Int? = null,
    ): GMResult<Any, JsxGraphLocaleNumberFormatError> {
        if (!useLocale()) {
            localeNumberFormatError = null
            return GMResult.Ok(value)
        }
        val formatter = board.localeNumberFormatter
        if (formatter == null) {
            localeNumberFormatError = null
            return GMResult.Ok(value)
        }
        val request = when (
            val result = localeNumberFormatRequest(value, digits)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                localeNumberFormatError = result.error
                return result
            }
        }
        val result = try {
            formatter.format(request)
        } catch (exception: Exception) {
            GMResult.Err(
                JsxGraphLocaleNumberFormatError.CallbackFailed(
                    exception.message
                        ?: exception::class.simpleName.orEmpty(),
                ),
            )
        }
        localeNumberFormatError = when (result) {
            is GMResult.Ok -> null
            is GMResult.Err -> result.error
        }
        return result
    }

    internal fun useLocale(): Boolean {
        val intl = visualProperties["intl"] as? Map<*, *>
            ?: return false
        if (!intl.containsKey("enabled")) {
            return false
        }
        return when (eval(intl["enabled"])) {
            true -> true
            "inherit" -> {
                val boardIntl = board.attributes["intl"] as? Map<*, *>
                eval(boardIntl?.get("enabled")) == true
            }
            else -> false
        }
    }

    private fun localeNumberFormatRequest(
        value: Number,
        digits: Int?,
    ): GMResult<
        JsxGraphLocaleNumberFormatRequest,
        JsxGraphLocaleNumberFormatError,
        > {
        val intl = visualProperties["intl"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val locale = when (
            val result = localeValue(
                elementValue = intl["locale"],
                boardValue =
                    (board.attributes["intl"] as? Map<*, *>)
                        ?.get("locale"),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val rawOptions = intl["options"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val options =
            linkedMapOf<String, JsxGraphLocaleOptionValue>()
        for ((rawName, rawValue) in rawOptions) {
            val name = normalizeAttributeKey(rawName.toString())
            val evaluated = when (val result = evalResult(rawValue)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    JsxGraphLocaleNumberFormatError
                        .AttributeEvaluationFailed(
                            attribute = "intl.options.$name",
                            reason = result.error.toString(),
                        ),
                )
            }
            if (evaluated == null) {
                continue
            }
            val option = when (evaluated) {
                is String ->
                    JsxGraphLocaleOptionValue.StringValue(evaluated)
                is Number ->
                    JsxGraphLocaleOptionValue.NumberValue(
                        evaluated.toDouble(),
                    )
                is Boolean ->
                    JsxGraphLocaleOptionValue.BooleanValue(evaluated)
                else -> return GMResult.Err(
                    JsxGraphLocaleNumberFormatError.InvalidOption(
                        name = name,
                        actualType = localeValueType(evaluated),
                    ),
                )
            }
            options[LOCALE_OPTION_NAMES[name] ?: rawName.toString()] =
                option
        }
        if (
            "maximumfractiondigits" !in rawOptions.keys.map {
                normalizeAttributeKey(it.toString())
            } &&
            digits != null
        ) {
            options["maximumFractionDigits"] =
                JsxGraphLocaleOptionValue.NumberValue(digits.toDouble())
        }
        return GMResult.Ok(
            JsxGraphLocaleNumberFormatRequest(
                value = value.toDouble(),
                locale = locale,
                options = options,
            ),
        )
    }

    private fun localeValue(
        elementValue: Any?,
        boardValue: Any?,
    ): GMResult<String?, JsxGraphLocaleNumberFormatError> {
        val elementLocale = when (val result = evalResult(elementValue)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                JsxGraphLocaleNumberFormatError.AttributeEvaluationFailed(
                    attribute = "intl.locale",
                    reason = result.error.toString(),
                ),
            )
        }
        val selected =
            if (elementLocale == null || elementLocale == "") {
                when (val result = evalResult(boardValue)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        JsxGraphLocaleNumberFormatError
                            .AttributeEvaluationFailed(
                                attribute = "board.intl.locale",
                                reason = result.error.toString(),
                            ),
                    )
                }
            } else {
                elementLocale
            }
        return when (selected) {
            null -> GMResult.Ok(null)
            is String -> GMResult.Ok(selected)
            else -> GMResult.Err(
                JsxGraphLocaleNumberFormatError.InvalidLocale(
                    actualType = localeValueType(selected),
                ),
            )
        }
    }

    private fun localeValueType(value: Any?): String =
        when (value) {
            null -> "null"
            is String -> "string"
            is Number -> "number"
            is Boolean -> "boolean"
            is Map<*, *> -> "object"
            is Iterable<*> -> "array"
            else -> value::class.simpleName ?: "unknown"
        }

    // JSXGraph: src/base/element.js -> normalize
    internal fun normalize(): GeometryElement {
        stdform = Mat.normalize(stdform)
        return this
    }

    // JSXGraph: src/base/element.js -> prepareUpdate
    internal open fun prepareUpdate(): GeometryElement {
        needsUpdate = true
        return this
    }

    // JSXGraph: src/base/element.js -> update
    internal open fun update(fromParent: Boolean = true): GeometryElement = this

    // JSXGraph: src/base/element.js -> updateVisibility
    internal open fun updateVisibility(
        parentVisible: Boolean? = null,
    ): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        val requested = evalVisProp("visible")
        val visible = when {
            parentVisible != null -> parentVisible
            requested == "inherit" ->
                visualPropertiesCalculated["visible"] as? Boolean ?: false
            requested is Boolean -> requested
            requested == null -> true
            else -> requested != false
        }
        visualPropertiesCalculated["visible"] = visible
        for (inherited in inherits) {
            if (inherited.evalVisProp("visible") == "inherit") {
                inherited
                    .prepareUpdate()
                    .updateVisibility(visible)
            }
        }
        if (elementLabel?.evalVisProp("visible") == "inherit") {
            elementLabel
                ?.prepareUpdate()
                ?.updateVisibility(visible)
        }
        return this
    }

    // JSXGraph: src/base/element.js -> updateRenderer
    internal open fun updateRenderer(): GeometryElement = this

    // JSXGraph: src/base/element.js -> remove
    internal open fun remove(): GeometryElement = this

    // JSXGraph: src/base/element.js -> fullUpdate
    internal fun fullUpdate(): GeometryElement =
        prepareUpdate()
            .update()
            .updateVisibility()
            .updateRenderer()

    private fun normalizeAttributeKey(key: String): String =
        key.filterNot(Char::isWhitespace).lowercase()

    private fun normalizeBooleanString(value: Any?): Any? {
        if (value !is String) {
            return value
        }
        return when (value) {
            "true" -> true
            "false" -> false
            else -> value
        }
    }

    private fun parseAnimationNumber(value: Any?): Double? =
        when (value) {
            is Number -> value.toDouble()
            is String ->
                ANIMATION_NUMBER_PREFIX.find(value.trim())
                    ?.value
                    ?.toDoubleOrNull()
            else -> null
        }?.takeUnless(Double::isNaN)

    private fun mergeAttributeMaps(
        target: MutableMap<String, Any?>,
        source: Map<*, *>,
    ): MutableMap<String, Any?> {
        for ((rawKey, rawValue) in source) {
            val key = normalizeAttributeKey(rawKey.toString())
            val current = target[key]
            target[key] =
                if (
                    current is MutableMap<*, *> &&
                    rawValue is Map<*, *>
                ) {
                    @Suppress("UNCHECKED_CAST")
                    mergeAttributeMaps(
                        current as MutableMap<String, Any?>,
                        rawValue,
                    )
                } else {
                    copyAttributeValue(rawValue)
                }
        }
        return target
    }

    private fun copyAttributeMap(
        source: Map<String, Any?>,
    ): Map<String, Any?> =
        source.mapValuesTo(linkedMapOf()) { (_, value) ->
            copyAttributeValue(value)
        }

    private fun copyAttributeValue(value: Any?): Any? =
        when (value) {
            is Map<*, *> -> value.entries.associateTo(linkedMapOf()) {
                (key, nestedValue) ->
                normalizeAttributeKey(key.toString()) to
                    copyAttributeValue(nestedValue)
            }
            is List<*> -> value.map(::copyAttributeValue)
            is DoubleArray -> value.copyOf()
            is IntArray -> value.copyOf()
            else -> value
        }

    private fun copyDumpAttributeValue(value: Any?): Any? =
        when (value) {
            JsonNull -> null
            is JsonPrimitive ->
                when {
                    value.isString -> value.content
                    value.booleanOrNull != null -> value.booleanOrNull
                    value.longOrNull != null -> value.longOrNull
                    value.doubleOrNull != null -> value.doubleOrNull
                    else -> value.content
                }
            is JsonArray -> value.map(::copyDumpAttributeValue)
            is JsonObject -> value.entries.associateTo(linkedMapOf()) {
                (key, nestedValue) ->
                normalizeAttributeKey(key) to
                    copyDumpAttributeValue(nestedValue)
            }
            else -> copyAttributeValue(value)
        }

    private fun jsonValue(value: Any?): String =
        when (value) {
            null -> "null"
            is String -> "\"${escapeJson(value)}\""
            is Boolean, is Number -> value.toString()
            is Map<*, *> -> value.entries.joinToString(
                prefix = "{",
                postfix = "}",
            ) { (key, nested) ->
                "${jsonValue(key.toString())}:${jsonValue(nested)}"
            }
            is Iterable<*> -> value.joinToString(
                prefix = "[",
                postfix = "]",
                transform = ::jsonValue,
            )
            is DoubleArray -> value.joinToString(
                prefix = "[",
                postfix = "]",
            )
            is IntArray -> value.joinToString(
                prefix = "[",
                postfix = "]",
            )
            else -> jsonValue(value.toString())
        }

    private fun escapeJson(value: String): String = buildString {
        for (character in value) {
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
    }

    private companion object {
        val ANIMATION_NUMBER_PREFIX =
            Regex("""^[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?""")
        const val DEFAULT_HIT_PRECISION = 4.0

        val ATTRIBUTE_SHORTCUTS = mapOf(
            "color" to listOf("strokeColor", "fillColor"),
            "opacity" to listOf("strokeOpacity", "fillOpacity"),
            "highlightcolor" to listOf(
                "highlightStrokeColor",
                "highlightFillColor",
            ),
            "highlightopacity" to listOf(
                "highlightStrokeOpacity",
                "highlightFillOpacity",
            ),
            "strokewidth" to listOf(
                "strokeWidth",
                "highlightStrokeWidth",
            ),
        )

        val LOCALE_OPTION_NAMES = mapOf(
            "maximumfractiondigits" to "maximumFractionDigits",
            "minimumfractiondigits" to "minimumFractionDigits",
            "compactdisplay" to "compactDisplay",
            "currencydisplay" to "currencyDisplay",
            "currencysign" to "currencySign",
            "localematcher" to "localeMatcher",
            "numberingsystem" to "numberingSystem",
            "signdisplay" to "signDisplay",
            "unitdisplay" to "unitDisplay",
            "usegrouping" to "useGrouping",
            "roundingmode" to "roundingMode",
            "roundingpriority" to "roundingPriority",
            "roundingincrement" to "roundingIncrement",
            "trailingzerodisplay" to "trailingZeroDisplay",
            "minimumintegerdigits" to "minimumIntegerDigits",
            "minimumsignificantdigits" to "minimumSignificantDigits",
            "maximumsignificantdigits" to "maximumSignificantDigits",
        )
    }
}
