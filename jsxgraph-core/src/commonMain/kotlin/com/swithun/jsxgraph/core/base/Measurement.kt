/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/measure.js -> createMeasurement;
 * src/options.js -> measurement
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.parser.PrefixParser
import com.swithun.jsxgraph.core.parser.PrefixParserError
import com.swithun.jsxgraph.core.parser.PrefixParserLimits
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.abs

internal data class MeasurementAttributes(
    val id: String = "",
    val name: String? = null,
    val needsRegularUpdate: Boolean = true,
    val values: Map<String, JessieCodeRuntimeValue> = emptyMap(),
)

internal sealed interface MeasurementError {
    data class TextFactory(
        val error: TextError,
    ) : MeasurementError

    data class Prefix(
        val error: PrefixParserError,
    ) : MeasurementError

    data class AttributeEvaluation(
        val attribute: String,
        val error: JessieCodeRuntimeError,
    ) : MeasurementError

    data class InvalidAttribute(
        val attribute: String,
        val expected: String,
        val actual: String,
    ) : MeasurementError

    data class FormatterResult(
        val attribute: String,
        val actual: String,
    ) : MeasurementError
}

/**
 * Method and text-content state installed on the Text returned by
 * `createMeasurement`.
 */
internal class MeasurementDefinition(
    internal val text: Text,
    private val term: JessieCodeRuntimeValue,
    private val attributes: Map<String, JessieCodeRuntimeValue>,
    private val location: JessieCodeAstLocation,
    private val limits: PrefixParserLimits,
) {
    // JSXGraph 1.13.3: src/element/measure.js -> Value.
    internal fun Value(): GMResult<
        JessieCodeRuntimeValue,
        MeasurementError,
        > =
        when (
            val result = PrefixParser.execute(
                board = text.board,
                term = term,
                location = location,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                MeasurementError.Prefix(result.error),
            )
        }

    // JSXGraph 1.13.3: src/element/measure.js -> Dimension.
    internal fun Dimension(): GMResult<
        JessieCodeRuntimeValue,
        MeasurementError,
        > {
        val configured = when (
            val result = attribute("dim", JessieCodeRuntimeValue.NullValue)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        when (configured) {
            JessieCodeRuntimeValue.NullValue,
            JessieCodeRuntimeValue.UndefinedValue,
            -> Unit
            is JessieCodeRuntimeValue.NumberValue,
            is JessieCodeRuntimeValue.StringValue,
            -> return GMResult.Ok(configured)
            else -> return invalidAttribute(
                name = "dim",
                expected = "number, string, or null",
                actual = configured,
            )
        }
        return when (
            val result = PrefixParser.dimension(
                board = text.board,
                term = term,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(
                JessieCodeRuntimeValue.NumberValue(result.value),
            )
            is GMResult.Err -> GMResult.Err(
                MeasurementError.Prefix(result.error),
            )
        }
    }

    // JSXGraph 1.13.3: src/element/measure.js -> Unit.
    internal fun Unit(
        requestedDimension: JessieCodeRuntimeValue? = null,
    ): GMResult<JessieCodeRuntimeValue, MeasurementError> {
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
            val dimensions = linkedMapOf<String, JessieCodeRuntimeValue>()
            for (item in dimension.values) {
                val unit = when (val result = Unit(item)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                dimensions["dim${PrefixParser.jsString(item)}"] = unit
            }
            return GMResult.Ok(
                JessieCodeRuntimeValue.ObjectValue(dimensions),
            )
        }

        val units = when (
            val result = attribute(
                "units",
                JessieCodeRuntimeValue.ObjectValue(emptyMap()),
            )
        ) {
            is GMResult.Ok -> when (val value = result.value) {
                JessieCodeRuntimeValue.NullValue,
                JessieCodeRuntimeValue.UndefinedValue,
                -> null
                is JessieCodeRuntimeValue.ObjectValue -> value
                else -> return invalidAttribute(
                    name = "units",
                    expected = "object",
                    actual = value,
                )
            }
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
            return evaluate(
                attribute = "units[$dimensionKey]",
                value = configured,
            )
        }

        val baseUnit = when (
            val result = attribute(
                "baseunit",
                JessieCodeRuntimeValue.StringValue(""),
            )
        ) {
            is GMResult.Ok -> when (val value = result.value) {
                is JessieCodeRuntimeValue.StringValue -> value.value
                is JessieCodeRuntimeValue.NumberValue ->
                    PrefixParser.jsString(value)
                else -> return invalidAttribute(
                    name = "baseUnit",
                    expected = "string",
                    actual = value,
                )
            }
            is GMResult.Err -> return result
        }
        val numericDimension =
            (dimension as? JessieCodeRuntimeValue.NumberValue)?.value
        val unit = when {
            numericDimension == 0.0 -> ""
            numericDimension != null &&
                numericDimension > 1.0 &&
                baseUnit.isNotEmpty() ->
                "$baseUnit^{${JsNumberFormat.compact(numericDimension)}}"
            else -> baseUnit
        }
        return GMResult.Ok(JessieCodeRuntimeValue.StringValue(unit))
    }

    internal fun getTerm(): JessieCodeRuntimeValue = term

    internal fun getMethod(): GMResult<String, MeasurementError> {
        val values = (term as? JessieCodeRuntimeValue.ArrayValue)?.values
            ?: return invalidAttribute(
                name = "term",
                expected = "prefix expression",
                actual = term,
            )
        val method = (
            values.firstOrNull() as? JessieCodeRuntimeValue.StringValue
            )?.value ?: return invalidAttribute(
            name = "term[0]",
            expected = "string",
            actual = values.firstOrNull()
                ?: JessieCodeRuntimeValue.UndefinedValue,
        )
        return GMResult.Ok(if (method == "V") "Value" else method)
    }

    internal fun toPrefix(): GMResult<
        JessieCodeRuntimeValue,
        MeasurementError,
        > =
        when (
            val result = PrefixParser.toPrefix(
                board = text.board,
                term = term,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                MeasurementError.Prefix(result.error),
            )
        }

    internal fun getParents(): GMResult<
        List<JessieCodeRuntimeValue>,
        MeasurementError,
        > =
        when (
            val result = PrefixParser.getParents(
                board = text.board,
                term = term,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                MeasurementError.Prefix(result.error),
            )
        }

    internal fun parseTextContent(): GMResult<Boolean, MeasurementError> =
        when (
            val result = attribute(
                "parse",
                JessieCodeRuntimeValue.BooleanValue(true),
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(truthy(result.value))
            is GMResult.Err -> result
        }

    // JSXGraph 1.13.3: src/element/measure.js -> setText callback.
    internal fun displayText(): GMResult<String, MeasurementError> {
        val dimension = when (val result = Dimension()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
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
        val dimensionName =
            (dimension as? JessieCodeRuntimeValue.StringValue)?.value
        if (dimensionName == "coords" && formatted is FormattedValue.Array) {
            return formatCoordinates(prefix, suffix, formatted.values)
        }
        if (
            dimensionName == "direction" &&
            formatted is FormattedValue.Array
        ) {
            return formatDirection(prefix, suffix, formatted.values)
        }
        if (dimensionName != null) {
            return GMResult.Ok(prefix + formatted.text + suffix)
        }
        val number =
            (dimension as? JessieCodeRuntimeValue.NumberValue)?.value
        if (number == null || number.isNaN()) {
            return GMResult.Ok(prefix + "NaN" + suffix)
        }
        return GMResult.Ok(prefix + formatted.text + unit + suffix)
    }

    private fun formatCoordinates(
        prefix: String,
        suffix: String,
        source: List<JessieCodeRuntimeValue>,
    ): GMResult<String, MeasurementError> {
        val values = source.toMutableList()
        if (values.size == 2) {
            values.add(0, JessieCodeRuntimeValue.UndefinedValue)
        }
        while (values.size < 3) {
            values += JessieCodeRuntimeValue.UndefinedValue
        }
        val formatter = attributes["formatcoords"]
        val content = if (formatter is JessieCodeRuntimeValue.FunctionValue) {
            when (
                val result = formatter.externalCallable.call(
                    arguments = listOf(
                        JessieCodeRuntimeValue.ElementReference(text),
                        values[1],
                        values[2],
                        values[0],
                    ),
                    location = location,
                )
            ) {
                is GMResult.Ok -> PrefixParser.jsString(result.value)
                is GMResult.Err -> return GMResult.Err(
                    MeasurementError.AttributeEvaluation(
                        attribute = "formatCoords",
                        error = result.error,
                    ),
                )
            }
        } else {
            val z = numberLike(values[0])
            if (z != 1.0) {
                "Infinit coords"
            } else {
                "(${PrefixParser.jsString(values[1])}, " +
                    "${PrefixParser.jsString(values[2])})"
            }
        }
        return GMResult.Ok(prefix + content + suffix)
    }

    private fun formatDirection(
        prefix: String,
        suffix: String,
        source: List<JessieCodeRuntimeValue>,
    ): GMResult<String, MeasurementError> {
        val x = source.getOrElse(0) {
            JessieCodeRuntimeValue.UndefinedValue
        }
        val y = source.getOrElse(1) {
            JessieCodeRuntimeValue.UndefinedValue
        }
        val formatter = attributes["formatdirection"]
        val content = if (formatter is JessieCodeRuntimeValue.FunctionValue) {
            when (
                val result = formatter.externalCallable.call(
                    arguments = listOf(
                        JessieCodeRuntimeValue.ElementReference(text),
                        x,
                        y,
                    ),
                    location = location,
                )
            ) {
                is GMResult.Ok -> PrefixParser.jsString(result.value)
                is GMResult.Err -> return GMResult.Err(
                    MeasurementError.AttributeEvaluation(
                        attribute = "formatDirection",
                        error = result.error,
                    ),
                )
            }
        } else {
            "(${PrefixParser.jsString(x)}, ${PrefixParser.jsString(y)})"
        }
        return GMResult.Ok(prefix + content + suffix)
    }

    private fun formatValue(
        value: JessieCodeRuntimeValue,
    ): GMResult<FormattedValue, MeasurementError> {
        val digits = when (val result = digits()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (value) {
            is JessieCodeRuntimeValue.NumberValue -> {
                val formatted = formatNumber(value.value, digits)
                GMResult.Ok(
                    FormattedValue.Scalar(
                        JessieCodeRuntimeValue.StringValue(formatted),
                    ),
                )
            }
            is JessieCodeRuntimeValue.ArrayValue -> {
                val values = value.values.map { item ->
                    if (item is JessieCodeRuntimeValue.NumberValue) {
                        JessieCodeRuntimeValue.StringValue(
                            formatNumber(item.value, digits),
                        )
                    } else {
                        item
                    }
                }
                GMResult.Ok(FormattedValue.Array(values))
            }
            else -> GMResult.Ok(FormattedValue.Scalar(value))
        }
    }

    private fun digits(): GMResult<MeasurementDigits, MeasurementError> {
        val value = when (
            val result = attribute(
                "digits",
                JessieCodeRuntimeValue.NumberValue(2.0),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (value) {
            is JessieCodeRuntimeValue.StringValue -> when (value.value) {
                "none" -> GMResult.Ok(MeasurementDigits.None)
                "auto" -> GMResult.Ok(MeasurementDigits.Auto)
                else -> invalidAttribute(
                    name = "digits",
                    expected = "integer from 0 to 100, 'none', or 'auto'",
                    actual = value,
                )
            }
            is JessieCodeRuntimeValue.NumberValue -> {
                val integer = value.value.toInt()
                if (
                    value.value.isFinite() &&
                    integer.toDouble() == value.value &&
                    integer in 0..100
                ) {
                    GMResult.Ok(MeasurementDigits.Fixed(integer))
                } else {
                    invalidAttribute(
                        name = "digits",
                        expected =
                            "integer from 0 to 100, 'none', or 'auto'",
                        actual = value,
                    )
                }
            }
            else -> invalidAttribute(
                name = "digits",
                expected = "integer from 0 to 100, 'none', or 'auto'",
                actual = value,
            )
        }
    }

    private fun formatNumber(
        value: Double,
        digits: MeasurementDigits,
    ): String =
        when (digits) {
            MeasurementDigits.None -> JsNumberFormat.compact(value)
            MeasurementDigits.Auto -> autoDigits(value)
            is MeasurementDigits.Fixed ->
                JsNumberFormat.fixed(value, digits.value)
        }

    private fun autoDigits(value: Double): String =
        when (val absolute = abs(value)) {
            in 0.1..Double.POSITIVE_INFINITY ->
                JsNumberFormat.fixed(value, 2)
            in 0.01..<0.1 -> JsNumberFormat.fixed(value, 4)
            in 0.0001..<0.01 -> JsNumberFormat.fixed(value, 6)
            else -> JsNumberFormat.compact(value)
        }

    private fun visibleText(
        visibleAttribute: String,
        textAttribute: String,
    ): GMResult<String, MeasurementError> {
        val visible = when (
            val result = attribute(
                visibleAttribute,
                JessieCodeRuntimeValue.BooleanValue(true),
            )
        ) {
            is GMResult.Ok -> truthy(result.value)
            is GMResult.Err -> return result
        }
        if (!visible) {
            return GMResult.Ok("")
        }
        return when (
            val result = attribute(
                textAttribute,
                JessieCodeRuntimeValue.StringValue(""),
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(
                PrefixParser.jsString(result.value),
            )
            is GMResult.Err -> result
        }
    }

    private fun attribute(
        name: String,
        default: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, MeasurementError> =
        evaluate(
            attribute = name,
            value = attributes[name] ?: default,
        )

    private fun evaluate(
        attribute: String,
        value: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, MeasurementError> {
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
                MeasurementError.AttributeEvaluation(
                    attribute = attribute,
                    error = result.error,
                ),
            )
        }
    }

    private fun numberLike(value: JessieCodeRuntimeValue): Double =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue -> value.value
            is JessieCodeRuntimeValue.StringValue ->
                value.value.toDoubleOrNull() ?: Double.NaN
            else -> Double.NaN
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
    ): GMResult<T, MeasurementError> =
        GMResult.Err(
            MeasurementError.InvalidAttribute(
                attribute = name,
                expected = expected,
                actual = runtimeType(actual),
            ),
        )

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

    private sealed interface FormattedValue {
        val text: String

        data class Scalar(
            val value: JessieCodeRuntimeValue,
        ) : FormattedValue {
            override val text: String
                get() = PrefixParser.jsString(value)
        }

        data class Array(
            val values: List<JessieCodeRuntimeValue>,
        ) : FormattedValue {
            override val text: String
                get() = PrefixParser.jsString(
                    JessieCodeRuntimeValue.ArrayValue(values),
                )
        }
    }
}

internal object Measurement {
    // JSXGraph 1.13.3: src/element/measure.js -> createMeasurement.
    internal fun create(
        board: Board,
        coordinates: DoubleArray,
        term: JessieCodeRuntimeValue,
        attributes: MeasurementAttributes = MeasurementAttributes(),
        location: JessieCodeAstLocation = SOURCE_LOCATION,
        limits: PrefixParserLimits = PrefixParserLimits(),
    ): GMResult<Text, MeasurementError> =
        finish(
            board = board,
            term = term,
            attributes = attributes,
            location = location,
            limits = limits,
            textResult = Text.create(
                board = board,
                coordinates = coordinates,
                content = "",
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                parse = false,
            ),
        )

    internal fun create(
        board: Board,
        coordinateExpressions: List<String>,
        term: JessieCodeRuntimeValue,
        attributes: MeasurementAttributes = MeasurementAttributes(),
        location: JessieCodeAstLocation = SOURCE_LOCATION,
        limits: PrefixParserLimits = PrefixParserLimits(),
    ): GMResult<Text, MeasurementError> =
        finish(
            board = board,
            term = term,
            attributes = attributes,
            location = location,
            limits = limits,
            textResult = Text.create(
                board = board,
                coordinateExpressions = coordinateExpressions,
                content = "",
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                parse = false,
            ),
        )

    internal fun createConstrained(
        board: Board,
        coordinateFunctions: List<JessieCodeCoordinateFunction>,
        term: JessieCodeRuntimeValue,
        attributes: MeasurementAttributes = MeasurementAttributes(),
        location: JessieCodeAstLocation = SOURCE_LOCATION,
        limits: PrefixParserLimits = PrefixParserLimits(),
    ): GMResult<Text, MeasurementError> =
        finish(
            board = board,
            term = term,
            attributes = attributes,
            location = location,
            limits = limits,
            textResult = Text.createConstrained(
                board = board,
                coordinateFunctions = coordinateFunctions,
                content = "",
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                parse = false,
            ),
        )

    private fun finish(
        board: Board,
        term: JessieCodeRuntimeValue,
        attributes: MeasurementAttributes,
        location: JessieCodeAstLocation,
        limits: PrefixParserLimits,
        textResult: GMResult<Text, TextError>,
    ): GMResult<Text, MeasurementError> {
        val text = when (textResult) {
            is GMResult.Ok -> textResult.value
            is GMResult.Err -> return GMResult.Err(
                MeasurementError.TextFactory(textResult.error),
            )
        }
        val definition = MeasurementDefinition(
            text = text,
            term = term,
            attributes = attributes.values,
            location = location,
            limits = limits,
        )
        val parentValues = when (val result = definition.getParents()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(text)
                return result
            }
        }
        val parseTextContent = when (
            val result = definition.parseTextContent()
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(text)
                return result
            }
        }
        val parents = parentValues.mapNotNull { value ->
            when (value) {
                is JessieCodeRuntimeValue.ElementReference ->
                    board.select(value.element)
                is JessieCodeRuntimeValue.StringValue ->
                    board.select(value.value)
                else -> null
            }
        }.distinctBy(GeometryElement::id)
        text.type = Const.OBJECT_TYPE_MEASUREMENT
        text.elType = "measurement"
        text.measurementDefinition = definition
        text.setDynamicText(parse = parseTextContent) {
            when (val result = definition.displayText()) {
                is GMResult.Ok -> result
                is GMResult.Err -> GMResult.Err(
                    TextError.MeasurementContent(result.error),
                )
            }
        }
        text.addParents(parents)
        for (parent in parents) {
            parent.addChild(text)
        }
        for (
            function in attributes.values.values
                .filterIsInstance<JessieCodeRuntimeValue.FunctionValue>()
        ) {
            for (dependency in function.dependencies.values) {
                dependency.addChild(text)
            }
        }
        text.fullUpdate()
        val contentError = text.contentEvaluationError
        if (contentError is TextError.MeasurementContent) {
            board.removeObject(text)
            return GMResult.Err(contentError.error)
        }
        return GMResult.Ok(text)
    }

    private val SOURCE_LOCATION = JessieCodeAstLocation(
        line = 1,
        column = 1,
        endLine = 1,
        endColumn = 1,
    )
}

private sealed interface MeasurementDigits {
    data object None : MeasurementDigits

    data object Auto : MeasurementDigits

    data class Fixed(
        val value: Int,
    ) : MeasurementDigits
}
