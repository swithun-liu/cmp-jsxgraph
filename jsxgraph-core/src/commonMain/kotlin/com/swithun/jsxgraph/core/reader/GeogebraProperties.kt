/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/geogebra.js -> colorProperties,
 * visualProperties, and isGGBVector.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Coords
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.utils.JsMath
import com.swithun.jsxgraph.core.utils.XML
import com.swithun.jsxgraph.core.utils.XmlElement
import com.swithun.jsxgraph.core.utils.XmlError
import com.swithun.jsxgraph.core.utils.XmlLimits
import com.swithun.jsxgraph.core.utils.XmlNode

internal data class GeogebraPropertyLimits(
    val preparation: ReaderPreparationLimits = ReaderPreparationLimits(),
    val xml: XmlLimits = XmlLimits(),
    val maxElements: Int = 100_000,
)

internal sealed interface GeogebraPropertyError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : GeogebraPropertyError

    data class PreparationFailed(
        val cause: ReaderPreparationError,
    ) : GeogebraPropertyError

    data class XmlParsingFailed(
        val cause: XmlError,
    ) : GeogebraPropertyError

    data class ElementLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : GeogebraPropertyError

    data class MissingElement(
        val index: Int,
        val available: Int,
    ) : GeogebraPropertyError

    data object MissingCoordinateData : GeogebraPropertyError

    data class MissingStartPoint(
        val name: String,
    ) : GeogebraPropertyError

    data class StartPointTypeMismatch(
        val name: String,
        val actualType: String,
    ) : GeogebraPropertyError
}

internal data class GeogebraElementAttributes(
    val fillColor: String? = null,
    val strokeColor: String? = null,
    val highlightFillColor: String? = null,
    val highlightStrokeColor: String? = null,
    val fillOpacity: Double? = null,
    val highlightFillOpacity: Double? = null,
    val strokeOpacity: Double? = null,
    val labelColor: String? = null,
    val visible: Boolean? = null,
    val withLabel: Boolean? = null,
    val size: Double? = null,
    val styleGGB: Double? = null,
    val face: String? = null,
    val slopeWidth: String? = null,
    val strokeWidth: Double? = null,
    val highlightStrokeWidth: Double? = null,
    val dashGGB: String? = null,
    val dash: Int? = null,
    val labelX: Double? = null,
    val labelY: Double? = null,
    val trace: String? = null,
    val fixed: String? = null,
)

internal sealed interface GeogebraCoordinateSource {
    fun x(): Double

    fun y(): Double

    data class Static(
        val x: Double,
        val y: Double,
    ) : GeogebraCoordinateSource {
        override fun x(): Double = x

        override fun y(): Double = y
    }

    data class PointAnchor(
        val point: Point,
        val offsetX: Double,
        val offsetY: Double,
    ) : GeogebraCoordinateSource {
        override fun x(): Double = point.X() + offsetX

        override fun y(): Double = point.Y() - offsetY
    }
}

internal data class GeogebraElementCoordinates(
    val source: GeogebraCoordinateSource,
    val z: Double?,
) {
    fun x(): Double = source.x()

    fun y(): Double = source.y()
}

internal object GeogebraProperties {
    // JSXGraph 1.13.3: src/reader/geogebra.js -> boardProperties.
    internal fun boardProperties(
        attributes: GeogebraElementAttributes,
    ): GeogebraElementAttributes = attributes

    internal fun parseElementProperties(
        source: String,
        elementIndex: Int = 0,
        initial: GeogebraElementAttributes = GeogebraElementAttributes(),
        limits: GeogebraPropertyLimits = GeogebraPropertyLimits(),
    ): GMResult<GeogebraElementAttributes, GeogebraPropertyError> {
        if (limits.maxElements < 0) {
            return GMResult.Err(
                GeogebraPropertyError.InvalidLimits(
                    name = "maxElements",
                    value = limits.maxElements,
                ),
            )
        }
        val element = when (
            val result = parseElement(
                source = source,
                elementIndex = elementIndex,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            visualProperties(
                data = element,
                attributes = colorProperties(element, initial),
            ),
        )
    }

    internal fun parseElementCoordinates(
        source: String,
        board: Board,
        elementIndex: Int = 0,
        limits: GeogebraPropertyLimits = GeogebraPropertyLimits(),
    ): GMResult<GeogebraElementCoordinates, GeogebraPropertyError> {
        if (limits.maxElements < 0) {
            return GMResult.Err(
                GeogebraPropertyError.InvalidLimits(
                    name = "maxElements",
                    value = limits.maxElements,
                ),
            )
        }
        val element = when (
            val result = parseElement(
                source = source,
                elementIndex = elementIndex,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return coordinates(board, element)
    }

    // JSXGraph 1.13.3: src/reader/geogebra.js -> colorProperties.
    internal fun colorProperties(
        data: XmlElement,
        attributes: GeogebraElementAttributes,
    ): GeogebraElementAttributes {
        val objectColor = data
            .getElementsByTagName(OBJECT_COLOR_TAG)
            .firstOrNull()
        val alpha = objectColor
            ?.getAttribute(ALPHA_ATTRIBUTE)
            ?.takeIf(String::isNotEmpty)
            ?.let(::jsParseFloat)
            ?: 0.0
        val red = colorChannel(objectColor?.getAttribute(RED_ATTRIBUTE))
        val green = colorChannel(objectColor?.getAttribute(GREEN_ATTRIBUTE))
        val blue = colorChannel(objectColor?.getAttribute(BLUE_ATTRIBUTE))
        val color = "#$red$green$blue"
        return attributes.copy(
            fillColor = color,
            strokeColor = color,
            highlightFillColor = color,
            highlightStrokeColor = color,
            fillOpacity = alpha,
            highlightFillOpacity = alpha,
            labelColor = color,
        )
    }

    // JSXGraph 1.13.3: src/reader/geogebra.js -> visualProperties.
    internal fun visualProperties(
        data: XmlElement,
        attributes: GeogebraElementAttributes,
    ): GeogebraElementAttributes {
        var result = attributes
        val show = data.getElementsByTagName(SHOW_TAG).firstOrNull()
        show?.getAttribute(OBJECT_ATTRIBUTE)
            ?.takeIf(String::isNotEmpty)
            ?.let { result = result.copy(visible = str2Bool(it)) }
        show?.getAttribute(LABEL_ATTRIBUTE)
            ?.takeIf(String::isNotEmpty)
            ?.let { result = result.copy(withLabel = str2Bool(it)) }

        data.getElementsByTagName(POINT_SIZE_TAG)
            .firstOrNull()
            ?.getAttribute(VALUE_ATTRIBUTE)
            ?.takeIf(String::isNotEmpty)
            ?.let { result = result.copy(size = jsParseInt(it)) }
        data.getElementsByTagName(POINT_STYLE_TAG)
            .firstOrNull()
            ?.getAttribute(VALUE_ATTRIBUTE)
            ?.takeIf(String::isNotEmpty)
            ?.let { result = result.copy(styleGGB = jsParseInt(it)) }
        result = applyPointStyle(result)

        data.getElementsByTagName(SLOPE_TRIANGLE_SIZE_TAG)
            .firstOrNull()
            ?.let { slope ->
                result = result.copy(
                    slopeWidth = slope.getAttribute(VALUE_ATTRIBUTE),
                )
            }

        data.getElementsByTagName(LINE_STYLE_TAG)
            .firstOrNull()
            ?.let { lineStyle ->
                val width = JsMath.round(
                    jsParseFloat(
                        lineStyle.getAttribute(THICKNESS_ATTRIBUTE),
                    ) / 2.0,
                )
                val dashGGB = lineStyle.getAttribute(TYPE_ATTRIBUTE)
                result = result.copy(
                    strokeWidth = width,
                    dashGGB = dashGGB,
                    dash = when (dashGGB) {
                        "0" -> 0
                        "10" -> 2
                        "15" -> 3
                        "20" -> 1
                        "30" -> 6
                        else -> result.dash
                    },
                )
            }
        result.strokeWidth?.takeIf(::isTruthy)?.let { width ->
            result = result.copy(highlightStrokeWidth = width + 1.0)
        }

        data.getElementsByTagName(LABEL_OFFSET_TAG)
            .firstOrNull()
            ?.let { labelOffset ->
                result = result.copy(
                    labelX = jsParseFloat(
                        labelOffset.getAttribute(X_ATTRIBUTE),
                    ),
                    labelY = jsParseFloat(
                        labelOffset.getAttribute(Y_ATTRIBUTE),
                    ),
                )
            }
        data.getElementsByTagName(TRACE_TAG)
            .firstOrNull()
            ?.let { trace ->
                result = result.copy(
                    trace = trace.getAttribute(VALUE_ATTRIBUTE),
                )
            }
        data.getElementsByTagName(FIX_TAG)
            .firstOrNull()
            ?.let { fix ->
                result = result.copy(
                    fixed = fix.getAttribute(VALUE_ATTRIBUTE),
                )
            }
        return result
    }

    // JSXGraph 1.13.3: src/reader/geogebra.js -> coordinates.
    internal fun coordinates(
        board: Board,
        data: XmlElement,
    ): GMResult<GeogebraElementCoordinates, GeogebraPropertyError> {
        val labelOffset = data
            .getElementsByTagName(LABEL_OFFSET_TAG)
            .firstOrNull()
        val offsetX =
            jsParseFloat(labelOffset?.getAttribute(X_ATTRIBUTE)) /
                board.unitX
        val offsetY =
            jsParseFloat(labelOffset?.getAttribute(Y_ATTRIBUTE)) /
                board.unitY
        val normalizedOffsetX = if (labelOffset == null) 0.0 else offsetX
        val normalizedOffsetY = if (labelOffset == null) 0.0 else offsetY

        data.getElementsByTagName(COORDS_TAG).firstOrNull()?.let {
            return GMResult.Ok(
                GeogebraElementCoordinates(
                    source = GeogebraCoordinateSource.Static(
                        x = jsParseFloat(it.getAttribute(X_ATTRIBUTE)),
                        y = jsParseFloat(it.getAttribute(Y_ATTRIBUTE)),
                    ),
                    z = jsParseFloat(it.getAttribute(Z_ATTRIBUTE)),
                ),
            )
        }
        data.getElementsByTagName(START_POINT_TAG).firstOrNull()?.let {
            val expression = it
                .getAttribute(EXPRESSION_ATTRIBUTE)
                ?.takeIf(String::isNotEmpty)
            if (expression != null) {
                val anchor = board.select(expression)
                    ?: return GMResult.Err(
                        GeogebraPropertyError.MissingStartPoint(expression),
                    )
                if (anchor !is Point) {
                    return GMResult.Err(
                        GeogebraPropertyError.StartPointTypeMismatch(
                            name = expression,
                            actualType =
                                anchor.elType.ifEmpty { "element" },
                        ),
                    )
                }
                return GMResult.Ok(
                    GeogebraElementCoordinates(
                        source = GeogebraCoordinateSource.PointAnchor(
                            point = anchor,
                            offsetX = normalizedOffsetX,
                            offsetY = normalizedOffsetY,
                        ),
                        z = null,
                    ),
                )
            }
            return GMResult.Ok(
                GeogebraElementCoordinates(
                    source = GeogebraCoordinateSource.Static(
                        x = jsParseFloat(it.getAttribute(X_ATTRIBUTE)),
                        y = jsParseFloat(it.getAttribute(Y_ATTRIBUTE)),
                    ),
                    z = jsParseFloat(it.getAttribute(Z_ATTRIBUTE)),
                ),
            )
        }
        data.getElementsByTagName(ABSOLUTE_SCREEN_LOCATION_TAG)
            .firstOrNull()
            ?.let {
                val coordinates = Coords(
                    method = Const.COORDS_BY_SCREEN,
                    coordinates = doubleArrayOf(
                        jsParseFloat(it.getAttribute(X_ATTRIBUTE)),
                        jsParseFloat(it.getAttribute(Y_ATTRIBUTE)),
                    ),
                    board = board,
                )
                return GMResult.Ok(
                    GeogebraElementCoordinates(
                        source = GeogebraCoordinateSource.Static(
                            x =
                                coordinates.usrCoords[1] +
                                    normalizedOffsetX,
                            y =
                                coordinates.usrCoords[2] +
                                    normalizedOffsetY,
                        ),
                        z = null,
                    ),
                )
            }
        return GMResult.Err(GeogebraPropertyError.MissingCoordinateData)
    }

    // JSXGraph 1.13.3: src/reader/geogebra.js -> getElement.
    internal fun getElement(
        tree: XmlNode,
        name: String,
        expression: Boolean = false,
    ): XmlElement? {
        for (
            construction in
            tree.getElementsByTagName(CONSTRUCTION_TAG)
        ) {
            if (!expression) {
                for (
                    element in
                    construction.getElementsByTagName(ELEMENT_TAG)
                ) {
                    if (name == element.getAttribute(LABEL_ATTRIBUTE)) {
                        return element
                    }
                }
                continue
            }
            for (
                expressionElement in
                construction.getElementsByTagName(EXPRESSION_TAG)
            ) {
                if (
                    name ==
                    expressionElement.getAttribute(LABEL_ATTRIBUTE)
                ) {
                    return expressionElement
                }
                if (
                    name ==
                    expressionElement.getAttribute(EXPRESSION_ATTRIBUTE)
                ) {
                    val label = expressionElement
                        .getAttribute(LABEL_ATTRIBUTE)
                        ?: return null
                    return getElement(
                        tree = tree,
                        name = label,
                        expression = false,
                    )
                }
            }
        }
        return null
    }

    // JSXGraph 1.13.3: src/reader/geogebra.js -> isGGBVector.
    internal fun isGGBVector(value: List<Double>?): Boolean =
        value != null && value.size == 3 && value[0] == 1.0

    private fun applyPointStyle(
        attributes: GeogebraElementAttributes,
    ): GeogebraElementAttributes =
        when (attributes.styleGGB) {
            0.0 -> attributes.copy(
                face = "circle",
                fillColor = attributes.strokeColor,
                fillOpacity = 1.0,
                highlightFillColor = attributes.strokeColor,
                highlightFillOpacity = 1.0,
                strokeColor = "black",
                strokeWidth = 1.0,
            )
            1.0 -> attributes.copy(face = "x")
            2.0 -> attributes.copy(
                face = "circle",
                fillColor = "none",
            )
            3.0 -> attributes.copy(
                face = "+",
                strokeOpacity = 1.0,
            )
            4.0 -> attributes.copy(
                face = "diamond",
                fillColor = attributes.strokeColor,
                fillOpacity = 1.0,
            )
            5.0 -> attributes.copy(
                face = "diamond",
                fillColor = "none",
            )
            6.0 -> attributes.copy(
                face = "triangleUp",
                fillColor = attributes.strokeColor,
                fillOpacity = 1.0,
            )
            7.0 -> attributes.copy(
                face = "triangleDown",
                fillColor = attributes.strokeColor,
                fillOpacity = 1.0,
            )
            8.0 -> attributes.copy(
                face = "triangleRight",
                fillColor = attributes.strokeColor,
                fillOpacity = 1.0,
            )
            9.0 -> attributes.copy(
                face = "triangleLeft",
                fillColor = attributes.strokeColor,
                fillOpacity = 1.0,
            )
            else -> attributes
        }

    private fun colorChannel(value: String?): String {
        if (value.isNullOrEmpty()) {
            return "0"
        }
        val parsed = jsParseIntValue(value)
        val hex = parsed?.toString(16) ?: "NaN"
        return if (hex.length == 1) "0$hex" else hex
    }

    private fun jsParseInt(value: String): Double =
        jsParseIntValue(value)?.toDouble() ?: Double.NaN

    private fun jsParseIntValue(value: String): Long? {
        val prefix = INTEGER_PREFIX.find(value.trimStart())?.value
            ?: return null
        return prefix.toLongOrNull()
    }

    private fun jsParseFloat(value: String?): Double {
        val prefix = value
            ?.trimStart()
            ?.let(FLOAT_PREFIX::find)
            ?.value
            ?: return Double.NaN
        return when (prefix) {
            "Infinity",
            "+Infinity",
            -> Double.POSITIVE_INFINITY
            "-Infinity" -> Double.NEGATIVE_INFINITY
            else -> prefix.toDoubleOrNull() ?: Double.NaN
        }
    }

    private fun str2Bool(value: String): Boolean =
        value.lowercase() == "true"

    private fun isTruthy(value: Double): Boolean =
        value != 0.0 && !value.isNaN()

    private fun parseElement(
        source: String,
        elementIndex: Int,
        limits: GeogebraPropertyLimits,
    ): GMResult<XmlElement, GeogebraPropertyError> {
        val prepared = when (
            val result = ReaderPreparation.prepareGeogebra(
                source = source,
                limits = limits.preparation,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    GeogebraPropertyError.PreparationFailed(result.error),
                )
            }
        }
        val document = when (
            val result = XML.parse(
                input = prepared,
                limits = limits.xml,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    GeogebraPropertyError.XmlParsingFailed(result.error),
                )
            }
        }
        val elements = document.getElementsByTagName(ELEMENT_TAG)
        if (elements.size > limits.maxElements) {
            return GMResult.Err(
                GeogebraPropertyError.ElementLimitExceeded(
                    limit = limits.maxElements,
                    requested = elements.size,
                ),
            )
        }
        return when (val element = elements.getOrNull(elementIndex)) {
            null -> GMResult.Err(
                GeogebraPropertyError.MissingElement(
                    index = elementIndex,
                    available = elements.size,
                ),
            )
            else -> GMResult.Ok(element)
        }
    }

    private const val ELEMENT_TAG = "element"
    private const val CONSTRUCTION_TAG = "construction"
    private const val EXPRESSION_TAG = "expression"
    private const val OBJECT_COLOR_TAG = "objColor"
    private const val SHOW_TAG = "show"
    private const val POINT_SIZE_TAG = "pointSize"
    private const val POINT_STYLE_TAG = "pointStyle"
    private const val SLOPE_TRIANGLE_SIZE_TAG = "slopeTriangleSize"
    private const val LINE_STYLE_TAG = "lineStyle"
    private const val LABEL_OFFSET_TAG = "labelOffset"
    private const val COORDS_TAG = "coords"
    private const val START_POINT_TAG = "startPoint"
    private const val ABSOLUTE_SCREEN_LOCATION_TAG =
        "absoluteScreenLocation"
    private const val TRACE_TAG = "trace"
    private const val FIX_TAG = "fix"
    private const val ALPHA_ATTRIBUTE = "alpha"
    private const val RED_ATTRIBUTE = "r"
    private const val GREEN_ATTRIBUTE = "g"
    private const val BLUE_ATTRIBUTE = "b"
    private const val OBJECT_ATTRIBUTE = "object"
    private const val LABEL_ATTRIBUTE = "label"
    private const val VALUE_ATTRIBUTE = "val"
    private const val THICKNESS_ATTRIBUTE = "thickness"
    private const val TYPE_ATTRIBUTE = "type"
    private const val X_ATTRIBUTE = "x"
    private const val Y_ATTRIBUTE = "y"
    private const val Z_ATTRIBUTE = "z"
    private const val EXPRESSION_ATTRIBUTE = "exp"

    private val INTEGER_PREFIX = Regex("^[+-]?\\d+")
    private val FLOAT_PREFIX =
        Regex(
            "^[+-]?(?:Infinity|" +
                "(?:(?:\\d+\\.?\\d*|\\.\\d+)" +
                "(?:[eE][+-]?\\d+)?))",
        )
}
