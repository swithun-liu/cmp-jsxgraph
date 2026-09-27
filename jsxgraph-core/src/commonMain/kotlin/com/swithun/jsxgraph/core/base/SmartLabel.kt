/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/smartlabel.js -> createSmartLabel;
 * src/options.js -> smartlabel*
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.parser.PrefixParser
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class SmartLabelAttributes(
    val id: String = "",
    val name: String? = null,
    val needsRegularUpdate: Boolean = true,
    val sanitizeHtml: Boolean = false,
    val values: Map<String, JessieCodeRuntimeValue> = emptyMap(),
)

internal sealed interface SmartLabelError {
    data class UnsupportedParent(
        val parentType: String,
    ) : SmartLabelError

    data class UnsupportedMeasure(
        val parentType: String,
        val measure: String,
    ) : SmartLabelError

    data class TextFactory(
        val error: TextError,
    ) : SmartLabelError

    data class AttributeEvaluation(
        val attribute: String,
        val error: JessieCodeRuntimeError,
    ) : SmartLabelError

    data class InvalidAttribute(
        val attribute: String,
        val expected: String,
        val actual: String,
    ) : SmartLabelError

}

internal enum class SmartLabelParentKind {
    POINT,
    LINE,
    CIRCLE,
    POLYGON,
    ANGLE,
}

internal enum class SmartLabelBoxKind {
    SOLID,
    OUTLINE,
    PURE,
}

internal data class SmartLabelBoxStyle(
    val kind: SmartLabelBoxKind,
    val parentKind: SmartLabelParentKind,
)

/**
 * Method, text, coordinate, and renderer state installed on the Text returned
 * by `createSmartLabel`.
 */
internal class SmartLabelDefinition(
    internal val text: Text,
    internal val parentObject: GeometryElement,
    internal val parentKind: SmartLabelParentKind,
    private val userText: JessieCodeRuntimeValue,
    private val attributes: Map<String, JessieCodeRuntimeValue>,
    private val location: JessieCodeAstLocation,
) {
    // JSXGraph 1.13.3: src/element/smartlabel.js -> Value.
    internal fun Value(): GMResult<
        JessieCodeRuntimeValue,
        SmartLabelError,
        > {
        val measure = when (val result = measure()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val value = when (measure) {
            "length" -> (parentObject as? Line)?.L()
            "slope" -> (parentObject as? Line)?.Slope()
            "area" -> when (parentObject) {
                is Circle -> parentObject.Area()
                is Polygon -> parentObject.Area()
                else -> null
            }
            "radius" -> (parentObject as? Circle)?.Radius()
            "perimeter", "circumference" -> when (parentObject) {
                is Circle -> parentObject.Perimeter()
                is Polygon -> parentObject.Perimeter()
                else -> null
            }
            "rad" -> (parentObject as? Sector)?.Value("radians")
            "deg" -> (parentObject as? Sector)?.Value("radians")
                ?.times(180.0 / PI)
            "coords" -> (parentObject as? Point)?.let { point ->
                return GMResult.Ok(
                    JessieCodeRuntimeValue.ArrayValue(
                        listOf(
                            JessieCodeRuntimeValue.NumberValue(point.X()),
                            JessieCodeRuntimeValue.NumberValue(point.Y()),
                        ),
                    ),
                )
            }
            else -> 0.0
        } ?: 0.0
        return GMResult.Ok(JessieCodeRuntimeValue.NumberValue(value))
    }

    // JSXGraph 1.13.3: src/element/smartlabel.js -> Dimension.
    internal fun Dimension(): GMResult<
        JessieCodeRuntimeValue,
        SmartLabelError,
        > {
        val measure = when (val result = measure()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val dimension = when (measure) {
            "area" -> 2.0
            "length",
            "radius",
            "perimeter",
            "circumference",
            "coords",
            -> 1.0
            else -> 0.0
        }
        return GMResult.Ok(JessieCodeRuntimeValue.NumberValue(dimension))
    }

    // JSXGraph 1.13.3: src/element/smartlabel.js -> Unit.
    internal fun Unit(
        requestedDimension: JessieCodeRuntimeValue? = null,
    ): GMResult<JessieCodeRuntimeValue, SmartLabelError> {
        val dimension = if (
            requestedDimension == null ||
            requestedDimension === JessieCodeRuntimeValue.UndefinedValue
        ) {
            when (val result = Dimension()) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        } else {
            requestedDimension
        }
        if (dimension is JessieCodeRuntimeValue.ArrayValue) {
            val units = linkedMapOf<String, JessieCodeRuntimeValue>()
            for (item in dimension.values) {
                val unit = when (val result = Unit(item)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                units["dim${PrefixParser.jsString(item)}"] = unit
            }
            return GMResult.Ok(JessieCodeRuntimeValue.ObjectValue(units))
        }

        val units = when (
            val result = attribute(
                "units",
                JessieCodeRuntimeValue.ObjectValue(emptyMap()),
            )
        ) {
            is GMResult.Ok ->
                result.value as? JessieCodeRuntimeValue.ObjectValue
            is GMResult.Err -> return result
        }
        val dimensionKey = PrefixParser.jsString(dimension)
        val configured = units?.properties?.get(dimensionKey)
            ?: units?.properties?.get("dim$dimensionKey")
        if (
            configured != null &&
            configured !== JessieCodeRuntimeValue.UndefinedValue &&
            configured != JessieCodeRuntimeValue.BooleanValue(false)
        ) {
            return evaluate("units[$dimensionKey]", configured)
        }

        var unit = when (
            val result = stringAttribute("baseunit", "")
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (unit.isEmpty()) {
            unit = when (val result = stringAttribute("unit", "")) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        }
        val numericDimension =
            (dimension as? JessieCodeRuntimeValue.NumberValue)?.value
        unit = when {
            numericDimension == 0.0 -> ""
            numericDimension != null &&
                numericDimension > 1.0 &&
                unit.isNotEmpty() ->
                "$unit^{${JsNumberFormat.compact(numericDimension)}}"
            else -> unit
        }
        return GMResult.Ok(JessieCodeRuntimeValue.StringValue(unit))
    }

    // JSXGraph 1.13.3: src/element/smartlabel.js -> setText callback.
    internal fun displayText(): GMResult<String, SmartLabelError> {
        val supplied = when (val result = evaluate("text", userText)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val suppliedText = PrefixParser.jsString(supplied)
        if (suppliedText.isNotEmpty()) {
            return GMResult.Ok(suppliedText)
        }

        val value = when (val result = Value()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val unit = when (val result = Unit()) {
            is GMResult.Ok -> PrefixParser.jsString(result.value)
            is GMResult.Err -> return result
        }
        val prefix = when (
            val result = visibleText("showprefix", "prefix")
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val suffix = when (
            val result = visibleText("showsuffix", "suffix")
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val formatted = when (val result = formatValue(value)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (formatted is SmartLabelFormattedValue.Array) {
            return formatCoordinates(
                prefix = prefix,
                suffix = suffix,
                unit = unit,
                values = formatted.values,
            )
        }
        return GMResult.Ok(prefix + formatted.text + unit + suffix)
    }

    internal fun measure(): GMResult<String, SmartLabelError> {
        val default = when (parentKind) {
            SmartLabelParentKind.POINT -> "coords"
            SmartLabelParentKind.LINE -> "length"
            SmartLabelParentKind.CIRCLE -> "radius"
            SmartLabelParentKind.POLYGON -> "area"
            SmartLabelParentKind.ANGLE -> "deg"
        }
        return stringAttribute("measure", default)
    }

    internal fun rotation(): GMResult<Double, SmartLabelError> =
        when (parentKind) {
            SmartLabelParentKind.LINE -> {
                val orientation = when (val result = orientation()) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val add = when (orientation) {
                    "none" -> return GMResult.Ok(0.0)
                    "orthogonal" -> 270.0
                    "orthogonal-inverted" -> 90.0
                    "parallel-inverted", "inverted" -> 0.0
                    else -> 360.0
                }
                val slope = (parentObject as Line).Slope()
                GMResult.Ok((atan(slope) * 180.0 / PI + add) % 360.0)
            }
            SmartLabelParentKind.ANGLE -> {
                val angle = angleAnchorDirection()
                GMResult.Ok(
                    if (angle > 90.0 && angle < 270.0) {
                        angle + 180.0
                    } else {
                        angle
                    },
                )
            }
            else -> numberAttribute("rotate", 0.0)
        }

    internal fun anchorX(): GMResult<String, SmartLabelError> =
        if (parentKind == SmartLabelParentKind.ANGLE) {
            val angle = angleAnchorDirection()
            GMResult.Ok(
                if (angle > 90.0 && angle < 270.0) "right" else "left",
            )
        } else {
            stringAttribute(
                "anchorx",
                when (parentKind) {
                    SmartLabelParentKind.POINT,
                    SmartLabelParentKind.LINE,
                    SmartLabelParentKind.CIRCLE,
                    SmartLabelParentKind.POLYGON,
                    -> "middle"
                    SmartLabelParentKind.ANGLE -> "left"
                },
            )
        }

    internal fun anchorY(): GMResult<String, SmartLabelError> =
        stringAttribute(
            "anchory",
            if (parentKind == SmartLabelParentKind.POINT) "top" else "middle",
        )

    internal fun orientation(): GMResult<String, SmartLabelError> =
        stringAttribute("orientation", "parallel")

    internal fun visibleThreshold(): GMResult<Double, SmartLabelError> =
        numberAttribute(
            "visiblethreshold",
            if (parentKind == SmartLabelParentKind.CIRCLE) 0.6 else 0.7,
        )

    // JSXGraph 1.13.3: src/element/sector.js -> getLabelAnchor.
    // The upstream 12 CSS-pixel extension uses the live board unitX. Core has
    // no final viewport, so preserve it as a renderer-resolved screen offset.
    internal fun screenOffset(): DoubleArray {
        if (parentKind != SmartLabelParentKind.ANGLE) {
            return doubleArrayOf(0.0, 0.0)
        }
        val angle = parentObject as Sector
        val anchor = smartLabelAngleAnchor(angle)
        val center = angle.center.coords.usrCoords
        val dx = anchor[0] - center[1]
        val dy = anchor[1] - center[2]
        val distance = sqrt(dx * dx + dy * dy)
        return doubleArrayOf(
            SMART_LABEL_ANGLE_OFFSET_PX * dx / distance,
            -SMART_LABEL_ANGLE_OFFSET_PX * dy / distance,
        )
    }

    internal fun boxStyle(): GMResult<SmartLabelBoxStyle, SmartLabelError> {
        val typeClass = when (parentKind) {
            SmartLabelParentKind.POINT -> "smart-label-point"
            SmartLabelParentKind.LINE -> "smart-label-line"
            SmartLabelParentKind.CIRCLE -> "smart-label-circle"
            SmartLabelParentKind.POLYGON -> "smart-label-polygon"
            SmartLabelParentKind.ANGLE -> "smart-label-angle"
        }
        val cssClass = when (
            val result = stringAttribute(
                "cssclass",
                "smart-label-solid $typeClass",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val kind = when {
            "smart-label-outline" in cssClass ->
                SmartLabelBoxKind.OUTLINE
            "smart-label-pure" in cssClass -> SmartLabelBoxKind.PURE
            else -> SmartLabelBoxKind.SOLID
        }
        return GMResult.Ok(SmartLabelBoxStyle(kind, parentKind))
    }

    private fun formatCoordinates(
        prefix: String,
        suffix: String,
        unit: String,
        values: List<JessieCodeRuntimeValue>,
    ): GMResult<String, SmartLabelError> {
        val direction = when (val result = stringAttribute("dir", "row")) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val math = when (val result = usesMathTypesetting()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (direction != "row" && !direction.startsWith("col")) {
            return GMResult.Ok("")
        }
        val separator = if (direction == "row") " / " else "<br />"
        val body = values.joinToString(separator) { value ->
            PrefixParser.jsString(value) +
                if (math) "\\,$unit" else " $unit"
        }
        return GMResult.Ok(
            if (math) {
                if (direction == "row") {
                    "\\($prefix$body$suffix\\)"
                } else {
                    "\\($prefix\\left(\\array{" +
                        values.joinToString("\\\\ ") { value ->
                            PrefixParser.jsString(value) + "\\,$unit"
                        } +
                        "}\\right)$suffix\\)"
                }
            } else {
                prefix + body + suffix
            },
        )
    }

    internal fun usesMathTypesetting(): GMResult<Boolean, SmartLabelError> {
        val mathJax = when (
            val result = booleanAttribute("usemathjax", true)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val katex = when (
            val result = booleanAttribute("usekatex", false)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(mathJax || katex)
    }

    private fun formatValue(
        value: JessieCodeRuntimeValue,
    ): GMResult<SmartLabelFormattedValue, SmartLabelError> {
        val digits = when (val result = digits()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        var formatted: JessieCodeRuntimeValue = when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                JessieCodeRuntimeValue.StringValue(
                    JsNumberFormat.fixed(value.value, digits),
                )
            is JessieCodeRuntimeValue.ArrayValue ->
                JessieCodeRuntimeValue.ArrayValue(
                    value.values.map { item ->
                        if (item is JessieCodeRuntimeValue.NumberValue) {
                            JessieCodeRuntimeValue.StringValue(
                                JsNumberFormat.fixed(item.value, digits),
                            )
                        } else {
                            item
                        }
                    },
                )
            else -> value
        }
        val formatter = attributes["formatvalue"]
        if (formatter is JessieCodeRuntimeValue.FunctionValue) {
            formatted = when (
                val result = formatter.externalCallable.call(
                    arguments = listOf(
                        JessieCodeRuntimeValue.ElementReference(text),
                        formatted,
                    ),
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    SmartLabelError.AttributeEvaluation(
                        attribute = "formatValue",
                        error = result.error,
                    ),
                )
            }
        }
        return GMResult.Ok(
            if (formatted is JessieCodeRuntimeValue.ArrayValue) {
                SmartLabelFormattedValue.Array(formatted.values)
            } else {
                SmartLabelFormattedValue.Scalar(formatted)
            },
        )
    }

    private fun digits(): GMResult<Int, SmartLabelError> {
        val value = when (
            val result = attribute(
                "digits",
                JessieCodeRuntimeValue.NumberValue(2.0),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val number = (value as? JessieCodeRuntimeValue.NumberValue)?.value
        val integer = number?.toInt()
        return if (
            number != null &&
            number.isFinite() &&
            integer != null &&
            integer.toDouble() == number &&
            integer in 0..100
        ) {
            GMResult.Ok(integer)
        } else {
            invalidAttribute(
                name = "digits",
                expected = "integer from 0 to 100",
                actual = value,
            )
        }
    }

    private fun visibleText(
        visibleAttribute: String,
        textAttribute: String,
    ): GMResult<String, SmartLabelError> {
        val visible = when (
            val result = booleanAttribute(visibleAttribute, true)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return if (visible) {
            stringAttribute(textAttribute, "")
        } else {
            GMResult.Ok("")
        }
    }

    private fun angleAnchorDirection(): Double {
        val angle = parentObject as Sector
        val anchor = smartLabelAngleAnchor(angle)
        val center = angle.center.coords.usrCoords
        return (
            atan2(anchor[1] - center[2], anchor[0] - center[1]) *
                180.0 / PI + 360.0
            ) % 360.0
    }

    private fun attribute(
        name: String,
        default: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, SmartLabelError> =
        evaluate(name, attributes[name] ?: default)

    private fun evaluate(
        attribute: String,
        value: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, SmartLabelError> {
        if (value !is JessieCodeRuntimeValue.FunctionValue) {
            return GMResult.Ok(value)
        }
        return when (
            val result = value.externalCallable.call(
                arguments = emptyList(),
                location = location,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                SmartLabelError.AttributeEvaluation(attribute, result.error),
            )
        }
    }

    private fun stringAttribute(
        name: String,
        default: String,
    ): GMResult<String, SmartLabelError> =
        when (
            val result = attribute(
                name,
                JessieCodeRuntimeValue.StringValue(default),
            )
        ) {
            is GMResult.Ok -> when (val value = result.value) {
                is JessieCodeRuntimeValue.StringValue ->
                    GMResult.Ok(value.value)
                is JessieCodeRuntimeValue.NumberValue ->
                    GMResult.Ok(PrefixParser.jsString(value))
                else -> invalidAttribute(name, "string", value)
            }
            is GMResult.Err -> result
        }

    private fun booleanAttribute(
        name: String,
        default: Boolean,
    ): GMResult<Boolean, SmartLabelError> =
        when (
            val result = attribute(
                name,
                JessieCodeRuntimeValue.BooleanValue(default),
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(truthy(result.value))
            is GMResult.Err -> result
        }

    private fun numberAttribute(
        name: String,
        default: Double,
    ): GMResult<Double, SmartLabelError> =
        when (
            val result = attribute(
                name,
                JessieCodeRuntimeValue.NumberValue(default),
            )
        ) {
            is GMResult.Ok -> {
                val value =
                    (result.value as? JessieCodeRuntimeValue.NumberValue)
                        ?.value
                if (value == null || !value.isFinite()) {
                    invalidAttribute(name, "finite number", result.value)
                } else {
                    GMResult.Ok(value)
                }
            }
            is GMResult.Err -> result
        }

    private fun truthy(value: JessieCodeRuntimeValue): Boolean =
        when (value) {
            JessieCodeRuntimeValue.NullValue,
            JessieCodeRuntimeValue.UndefinedValue,
            -> false
            is JessieCodeRuntimeValue.BooleanValue -> value.value
            is JessieCodeRuntimeValue.NumberValue ->
                value.value != 0.0 && !value.value.isNaN()
            is JessieCodeRuntimeValue.StringValue -> value.value.isNotEmpty()
            else -> true
        }

    private fun <T> invalidAttribute(
        name: String,
        expected: String,
        actual: JessieCodeRuntimeValue,
    ): GMResult<T, SmartLabelError> =
        GMResult.Err(
            SmartLabelError.InvalidAttribute(
                attribute = name,
                expected = expected,
                actual = runtimeType(actual),
            ),
        )

}

internal object SmartLabel {
    // JSXGraph 1.13.3: src/element/smartlabel.js -> createSmartLabel.
    internal fun create(
        board: Board,
        parent: GeometryElement,
        userText: JessieCodeRuntimeValue =
            JessieCodeRuntimeValue.StringValue(""),
        attributes: SmartLabelAttributes = SmartLabelAttributes(),
        location: JessieCodeAstLocation = SOURCE_LOCATION,
    ): GMResult<Text, SmartLabelError> {
        val parentKind = when {
            parent.elementClass == Const.OBJECT_CLASS_POINT ->
                SmartLabelParentKind.POINT
            parent.elementClass == Const.OBJECT_CLASS_LINE ->
                SmartLabelParentKind.LINE
            parent.elementClass == Const.OBJECT_CLASS_CIRCLE ->
                SmartLabelParentKind.CIRCLE
            parent.type == Const.OBJECT_TYPE_POLYGON ->
                SmartLabelParentKind.POLYGON
            parent.type == Const.OBJECT_TYPE_ANGLE ->
                SmartLabelParentKind.ANGLE
            else -> return GMResult.Err(
                SmartLabelError.UnsupportedParent(parent.elType),
            )
        }
        val measure = when (
            val result = initialMeasure(
                parentKind = parentKind,
                attributes = attributes.values,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (!supports(parentKind, measure)) {
            return GMResult.Err(
                SmartLabelError.UnsupportedMeasure(
                    parentType = parent.elType,
                    measure = measure,
                ),
            )
        }
        val coordinateFunction = SmartLabelCoordinateFunction(
            parent = parent,
            parentKind = parentKind,
            measure = measure,
        )
        val text = when (
            val result = Text.createConstrained(
                board = board,
                coordinateFunctions = listOf(coordinateFunction),
                content = "",
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                parse = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                SmartLabelError.TextFactory(result.error),
            )
        }
        val definition = SmartLabelDefinition(
            text = text,
            parentObject = parent,
            parentKind = parentKind,
            userText = userText,
            attributes = attributes.values,
            location = location,
        )
        text.elType = "smartlabel"
        text.smartLabelDefinition = definition
        text.setDynamicText(parse = true) {
            when (val result = definition.displayText()) {
                is GMResult.Ok -> GMResult.Ok(
                    if (attributes.sanitizeHtml) {
                        sanitizeHtml(result.value)
                    } else {
                        result.value
                    },
                )
                is GMResult.Err -> GMResult.Err(
                    TextError.SmartLabelContent(result.error),
                )
            }
        }
        text.setParents(listOf(parent))
        parent.addChild(text)
        for (
            function in (
                attributes.values.values + userText
                ).filterIsInstance<JessieCodeRuntimeValue.FunctionValue>()
        ) {
            for (dependency in function.dependencies.values) {
                dependency.addChild(text)
            }
        }
        text.fullUpdate()
        val contentError = text.contentEvaluationError
        if (contentError is TextError.SmartLabelContent) {
            board.removeObject(text)
            return GMResult.Err(contentError.error)
        }
        return GMResult.Ok(text)
    }

    private fun initialMeasure(
        parentKind: SmartLabelParentKind,
        attributes: Map<String, JessieCodeRuntimeValue>,
    ): GMResult<String, SmartLabelError> {
        val default = when (parentKind) {
            SmartLabelParentKind.POINT -> "coords"
            SmartLabelParentKind.LINE -> "length"
            SmartLabelParentKind.CIRCLE -> "radius"
            SmartLabelParentKind.POLYGON -> "area"
            SmartLabelParentKind.ANGLE -> "deg"
        }
        if (
            parentKind == SmartLabelParentKind.POINT ||
            parentKind == SmartLabelParentKind.ANGLE
        ) {
            return GMResult.Ok(default)
        }
        val value = attributes["measure"]
            ?: return GMResult.Ok(default)
        return when (value) {
            is JessieCodeRuntimeValue.StringValue ->
                GMResult.Ok(value.value)
            else -> GMResult.Err(
                SmartLabelError.InvalidAttribute(
                    attribute = "measure",
                    expected = "static string",
                    actual = runtimeType(value),
                ),
            )
        }
    }

    private fun supports(
        parentKind: SmartLabelParentKind,
        measure: String,
    ): Boolean =
        when (parentKind) {
            SmartLabelParentKind.POINT -> true
            SmartLabelParentKind.LINE ->
                measure == "length" || measure == "slope"
            SmartLabelParentKind.CIRCLE ->
                measure in setOf(
                    "radius",
                    "area",
                    "perimeter",
                    "circumference",
                )
            SmartLabelParentKind.POLYGON ->
                measure == "area" || measure == "perimeter"
            SmartLabelParentKind.ANGLE -> true
        }

    private val SOURCE_LOCATION = JessieCodeAstLocation(
        line = 1,
        column = 1,
        endLine = 1,
        endColumn = 1,
    )
}

private class SmartLabelCoordinateFunction(
    private val parent: GeometryElement,
    private val parentKind: SmartLabelParentKind,
    private val measure: String,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()
    override val returnsCoordinateArray: Boolean = true

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        val coordinates = when (parentKind) {
            SmartLabelParentKind.POINT -> {
                val point = parent as Point
                doubleArrayOf(point.X(), point.Y())
            }
            SmartLabelParentKind.LINE -> {
                val line = parent as Line
                val firstWeight =
                    if (measure == "slope") 0.25 else 0.5
                val secondWeight =
                    if (measure == "slope") 0.75 else 0.5
                doubleArrayOf(
                    line.point1.X() * firstWeight +
                        line.point2.X() * secondWeight,
                    line.point1.Y() * firstWeight +
                        line.point2.Y() * secondWeight,
                )
            }
            SmartLabelParentKind.CIRCLE -> {
                val circle = parent as Circle
                when (measure) {
                    "radius" -> doubleArrayOf(
                        circle.center.X() + circle.Radius() * 0.5,
                        circle.center.Y(),
                    )
                    "area" -> doubleArrayOf(
                        circle.center.X(),
                        circle.center.Y() + circle.Radius() * 0.5,
                    )
                    else -> {
                        val offset = circle.Radius() / sqrt(2.0)
                        doubleArrayOf(
                            circle.center.X() + offset,
                            circle.center.Y() + offset,
                        )
                    }
                }
            }
            SmartLabelParentKind.POLYGON -> {
                val polygon = parent as Polygon
                if (measure == "perimeter" && polygon.borders.isNotEmpty()) {
                    val border = polygon.borders.last()
                    doubleArrayOf(
                        (border.point1.X() + border.point2.X()) * 0.5,
                        (border.point1.Y() + border.point2.Y()) * 0.5,
                    )
                } else {
                    val bounds = polygon.bounds()
                    doubleArrayOf(
                        (bounds[0] + bounds[2]) * 0.5,
                        (bounds[1] + bounds[3]) * 0.5,
                    )
                }
            }
            SmartLabelParentKind.ANGLE ->
                smartLabelAngleAnchor(parent as Sector)
        }
        return GMResult.Ok(
            JessieCodeRuntimeValue.ArrayValue(
                coordinates.map(JessieCodeRuntimeValue::NumberValue),
            ),
        )
    }
}

// JSXGraph 1.13.3: src/element/sector.js -> getLabelAnchor.
private fun smartLabelAngleAnchor(angle: Sector): DoubleArray {
    val a = angle.point2.coords.usrCoords
    val b = angle.point1.coords.usrCoords
    val radius = angle.Radius()
    val distance = angle.point2.Dist(angle.point1)
    var halfAngle = Geometry.rad(
        angle.point2.Coords(),
        angle.point1.Coords(),
        angle.point3.Coords(),
    )
    if (
        (
            angle.selection == Arc.SELECTION_MINOR &&
                halfAngle > PI
            ) ||
        (
            angle.selection == Arc.SELECTION_MAJOR &&
                halfAngle < PI
            ) ||
        (
            angle.selection == Arc.SELECTION_AUTO &&
                angle.orientation == Arc.ORIENTATION_CLOCKWISE
            )
    ) {
        halfAngle = -(2.0 * PI - halfAngle)
    }
    halfAngle *= 0.5
    val co = cos(halfAngle)
    val si = sin(halfAngle)
    val radiusPointX = b[1] + (a[1] - b[1]) * radius / distance
    val radiusPointY = b[2] + (a[2] - b[2]) * radius / distance
    val midpointX =
        b[1] - 0.5 * b[1] * co + 0.5 * b[2] * si +
            co * 0.5 * radiusPointX -
            si * 0.5 * radiusPointY
    val midpointY =
        b[2] - 0.5 * b[1] * si - 0.5 * b[2] * co +
            si * 0.5 * radiusPointX +
            co * 0.5 * radiusPointY
    val midpointDistance = sqrt(
        (midpointX - b[1]) * (midpointX - b[1]) +
            (midpointY - b[2]) * (midpointY - b[2]),
    )
    return doubleArrayOf(
        b[1] + (midpointX - b[1]) * radius / midpointDistance,
        b[2] + (midpointY - b[2]) * radius / midpointDistance,
    )
}

// JSXGraph 1.13.3: src/utils/type.js -> sanitizeHTML.
private fun sanitizeHtml(content: String): String =
    content.replace("<", "&lt;").replace(">", "&gt;")

private const val SMART_LABEL_ANGLE_OFFSET_PX = 12.0

private sealed interface SmartLabelFormattedValue {
    val text: String

    data class Scalar(
        val value: JessieCodeRuntimeValue,
    ) : SmartLabelFormattedValue {
        override val text: String
            get() = PrefixParser.jsString(value)
    }

    data class Array(
        val values: List<JessieCodeRuntimeValue>,
    ) : SmartLabelFormattedValue {
        override val text: String
            get() = PrefixParser.jsString(
                JessieCodeRuntimeValue.ArrayValue(values),
            )
    }
}

private fun runtimeType(value: JessieCodeRuntimeValue): String =
    when (value) {
        JessieCodeRuntimeValue.UndefinedValue -> "undefined"
        JessieCodeRuntimeValue.NullValue -> "null"
        is JessieCodeRuntimeValue.NumberValue -> "number"
        is JessieCodeRuntimeValue.BooleanValue -> "boolean"
        is JessieCodeRuntimeValue.StringValue -> "string"
        is JessieCodeRuntimeValue.ArrayValue -> "array"
        is JessieCodeRuntimeValue.ObjectValue -> "object"
        is JessieCodeRuntimeValue.FunctionValue -> "function"
        is JessieCodeRuntimeValue.BoardReference -> "board"
        is JessieCodeRuntimeValue.TransformationReference ->
            "transformation"
        is JessieCodeRuntimeValue.CompositionReference -> "composition"
        is JessieCodeRuntimeValue.ElementReference -> "element"
    }
