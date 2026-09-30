/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/geonext.js -> changeOriginIds, gEBTN,
 * colorProperties, firstLevelProperties, defProperties, visualProperties,
 * transformProperties, readNodes, readNode point/line/circle/arrow/
 * intersection/arc/angle/text property branches, subtreeToString, and
 * readViewPort.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.utils.Color
import com.swithun.jsxgraph.core.utils.ColorError
import com.swithun.jsxgraph.core.utils.XML
import com.swithun.jsxgraph.core.utils.XmlCDataSection
import com.swithun.jsxgraph.core.utils.XmlComment
import com.swithun.jsxgraph.core.utils.XmlDocumentType
import com.swithun.jsxgraph.core.utils.XmlElement
import com.swithun.jsxgraph.core.utils.XmlError
import com.swithun.jsxgraph.core.utils.XmlLimits
import com.swithun.jsxgraph.core.utils.XmlNode
import com.swithun.jsxgraph.core.utils.XmlProcessingInstruction
import com.swithun.jsxgraph.core.utils.XmlText

internal data class GeonextPropertyLimits(
    val preparation: ReaderPreparationLimits = ReaderPreparationLimits(),
    val xml: XmlLimits = XmlLimits(),
    val maxElements: Int = 100_000,
    val maxProperties: Int = 10_000,
)

internal sealed interface GeonextPropertyError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : GeonextPropertyError

    data class PreparationFailed(
        val cause: ReaderPreparationError,
    ) : GeonextPropertyError

    data class XmlParsingFailed(
        val cause: XmlError,
    ) : GeonextPropertyError

    data class MissingSection(
        val name: String,
    ) : GeonextPropertyError

    data class ElementLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : GeonextPropertyError

    data class MissingElement(
        val index: Int,
        val available: Int,
    ) : GeonextPropertyError

    data class UnsupportedElementType(
        val type: String,
    ) : GeonextPropertyError

    data class MissingTag(
        val parent: String,
        val tag: String,
    ) : GeonextPropertyError

    data class MissingText(
        val parent: String,
        val tag: String,
    ) : GeonextPropertyError

    data class ColorConversionFailed(
        val tag: String,
        val cause: ColorError,
    ) : GeonextPropertyError

    data class PropertyLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : GeonextPropertyError
}

internal sealed interface GeonextPropertyValue {
    data class Text(
        val value: String,
    ) : GeonextPropertyValue

    data class Number(
        val value: Double,
    ) : GeonextPropertyValue

    data class Flag(
        val value: Boolean,
    ) : GeonextPropertyValue

    data class Label(
        val opacity: Double,
    ) : GeonextPropertyValue
}

internal data class GeonextElementProperties(
    val values: Map<String, GeonextPropertyValue>,
    val outputs: Map<String, GeonextElementProperties> = emptyMap(),
    val lists: Map<String, List<String>> = emptyMap(),
    val borders: List<GeonextElementProperties> = emptyList(),
)

internal object GeonextProperties {
    internal fun parseElementProperties(
        source: String,
        elementIndex: Int = 0,
        limits: GeonextPropertyLimits = GeonextPropertyLimits(),
    ): GMResult<GeonextElementProperties, GeonextPropertyError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        val prepared = when (
            val result = ReaderPreparation.prepareGeonext(
                source = source,
                limits = limits.preparation,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    GeonextPropertyError.PreparationFailed(result.error),
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
                    GeonextPropertyError.XmlParsingFailed(result.error),
                )
            }
        }
        val section = document.getElementsByTagName(ELEMENTS_TAG).firstOrNull()
            ?: return GMResult.Err(
                GeonextPropertyError.MissingSection(ELEMENTS_TAG),
            )
        val elements = section.childElements()
        if (elements.size > limits.maxElements) {
            return GMResult.Err(
                GeonextPropertyError.ElementLimitExceeded(
                    limit = limits.maxElements,
                    requested = elements.size,
                ),
            )
        }
        val element = elements.getOrNull(elementIndex)
            ?: return GMResult.Err(
                GeonextPropertyError.MissingElement(
                    index = elementIndex,
                    available = elements.size,
                ),
            )
        return readElementProperties(element, limits)
    }

    internal fun changeOriginId(
        boardId: String,
        id: String,
    ): String =
        if (id in ORIGIN_IDS) boardId + id else id

    // JSXGraph 1.13.3: src/reader/geonext.js -> readViewPort.
    internal fun readViewPort(
        node: XmlElement,
    ): List<Double> {
        val viewport = node.getElementsByTagName(VIEWPORT_TAG).firstOrNull()
            ?: return emptyList()
        return listOf(
            jsParseFloat(textByTagName(viewport, LEFT_TAG)),
            jsParseFloat(textByTagName(viewport, TOP_TAG)),
            jsParseFloat(textByTagName(viewport, RIGHT_TAG)),
            jsParseFloat(textByTagName(viewport, BOTTOM_TAG)),
        )
    }

    internal fun readElementProperties(
        data: XmlElement,
        limits: GeonextPropertyLimits,
    ): GMResult<GeonextElementProperties, GeonextPropertyError> {
        val point = data.nodeName == POINT_TAG
        val lineLike =
            data.nodeName == LINE_TAG || data.nodeName == ARROW_TAG
        val circle = data.nodeName == CIRCLE_TAG
        val intersection = data.nodeName == INTERSECTION_TAG
        val arc = data.nodeName == ARC_TAG
        val angle = data.nodeName == ANGLE_TAG
        val polygon = data.nodeName == POLYGON_TAG
        val graph = data.nodeName == GRAPH_TAG
        val parameterCurve = data.nodeName == PARAMETER_CURVE_TAG
        val slider = data.nodeName == SLIDER_TAG
        val traceCurve = data.nodeName == TRACE_CURVE_TAG
        val group = data.nodeName == GROUP_TAG
        val text = data.nodeName == TEXT_TAG
        val composition = data.nodeName == COMPOSITION_TAG
        if (
            !point &&
            !lineLike &&
            !circle &&
            !intersection &&
            !arc &&
            !angle &&
            !polygon &&
            !graph &&
            !parameterCurve &&
            !slider &&
            !traceCurve &&
            !group &&
            !text &&
            !composition
        ) {
            return GMResult.Err(
                GeonextPropertyError.UnsupportedElementType(data.nodeName),
            )
        }
        val values = linkedMapOf<String, GeonextPropertyValue>()
        if (point || slider) {
            values[STROKE_WIDTH_LOWER] =
                GeonextPropertyValue.Number(1.0)
        }
        when (val result = definitionProperties(values, data)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        if (intersection) {
            return readIntersectionProperties(
                values = values,
                data = data,
                limits = limits,
            )
        }
        if (traceCurve) {
            return readTraceCurveProperties(
                values = values,
                data = data,
                limits = limits,
            )
        }
        if (graph) {
            return readGraphProperties(
                values = values,
                data = data,
                limits = limits,
            )
        }
        if (group) {
            return readGroupProperties(
                values = values,
                data = data,
                limits = limits,
            )
        }
        if (text) {
            return readTextProperties(
                values = values,
                data = data,
                limits = limits,
            )
        }
        if (composition) {
            return readCompositionProperties(
                values = values,
                data = data,
                limits = limits,
            )
        }
        when (val result = colorProperties(values, data)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        if (polygon) {
            return readPolygonProperties(
                values = values,
                data = data,
                limits = limits,
            )
        }
        visualProperties(values, data)
        firstLevelProperties(values, data)
        when {
            point -> {
                when (
                    val result = readNodes(
                        values = values,
                        data = data,
                        nodeType = DATA_TAG,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                values[FIXED_KEY] = GeonextPropertyValue.Flag(
                    str2Bool(
                        textByTagName(data, FIX_TAG),
                        missingIsFalse = true,
                    ),
                )
            }
            slider -> {
                when (
                    val result = readNodes(
                        values = values,
                        data = data,
                        nodeType = DATA_TAG,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                values[FIXED_KEY] = GeonextPropertyValue.Flag(
                    str2Bool(
                        textByTagName(data, FIX_TAG),
                        missingIsFalse = true,
                    ),
                )
                when (
                    val result = readNodes(
                        values = values,
                        data = data,
                        nodeType = ANIMATE_TAG,
                        prefix = ANIMATE_PREFIX,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            lineLike -> {
                when (
                    val result = readNodes(
                        values = values,
                        data = data,
                        nodeType = DATA_TAG,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                when (
                    val result = readNodes(
                        values = values,
                        data = data,
                        nodeType = STRAIGHT_TAG,
                        prefix = STRAIGHT_PREFIX,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            circle -> {
                val circleData =
                    data.getElementsByTagName(DATA_TAG).firstOrNull()
                        ?: return GMResult.Err(
                            GeonextPropertyError.MissingTag(
                                parent = data.nodeName,
                                tag = DATA_TAG,
                            ),
                        )
                val center = when (
                    val result = requiredTextByTagName(
                        circleData,
                        MIDPOINT_TAG,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val radiusTag =
                    if (
                        circleData.getElementsByTagName(RADIUS_TAG)
                            .isNotEmpty()
                    ) {
                        RADIUS_TAG
                    } else {
                        RADIUS_VALUE_TAG
                    }
                val radius = when (
                    val result = requiredTextByTagName(
                        circleData,
                        radiusTag,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                values[CENTER_KEY] = GeonextPropertyValue.Text(center)
                values[RADIUS_KEY] = GeonextPropertyValue.Text(radius)
            }
            arc || angle -> {
                when (
                    val result = readNodes(
                        values = values,
                        data = data,
                        nodeType = DATA_TAG,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                if (arc) {
                    val geonextFirstArrow = when (
                        val result = requiredTextByTagName(
                            data,
                            FIRST_ARROW_TAG,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    val geonextLastArrow = when (
                        val result = requiredTextByTagName(
                            data,
                            LAST_ARROW_TAG,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    /*
                     * The reader next assigns reversed camel-case keys, but
                     * Type.keysToLowerCase iterates keys in reverse and the
                     * original lowercase XML keys win. Preserve the observed
                     * JSXGraph 1.13.3 result rather than the dead assignment.
                     */
                    values[FIRST_ARROW_KEY] = GeonextPropertyValue.Flag(
                        str2Bool(
                            geonextFirstArrow,
                            missingIsFalse = true,
                        ),
                    )
                    values[LAST_ARROW_KEY] = GeonextPropertyValue.Flag(
                        str2Bool(
                            geonextLastArrow,
                            missingIsFalse = true,
                        ),
                    )
                }
            }
            parameterCurve -> {
                for (tag in PARAMETER_CURVE_VALUE_TAGS) {
                    val value = when (
                        val result = requiredTextByTagName(data, tag)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    values[tag] = GeonextPropertyValue.Text(value)
                }
            }
        }
        transformProperties(
            values = values,
            point = point || slider,
        )
        return finishElementProperties(
            values = values,
            limits = limits,
        )
    }

    /*
     * JSXGraph 1.13.3: src/reader/geonext.js ->
     * readNode composition branch.
     */
    private fun readCompositionProperties(
        values: MutableMap<String, GeonextPropertyValue>,
        data: XmlElement,
        limits: GeonextPropertyLimits,
    ): GMResult<GeonextElementProperties, GeonextPropertyError> {
        when (
            val result = readNodes(
                values = values,
                data = data,
                nodeType = DATA_TAG,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        firstLevelProperties(values, data)
        val compositionData =
            data.getElementsByTagName(DATA_TAG).firstOrNull()
                ?: return GMResult.Err(
                    GeonextPropertyError.MissingTag(
                        parent = data.nodeName,
                        tag = DATA_TAG,
                    ),
                )
        val inputs = mutableListOf<String>()
        for (input in compositionData.getElementsByTagName(INPUT_TAG)) {
            val value = input.firstChild?.data
                ?: return GMResult.Err(
                    GeonextPropertyError.MissingText(
                        parent = DATA_TAG,
                        tag = INPUT_TAG,
                    ),
                )
            inputs += value
        }

        val type = values[TYPE_KEY].asText().orEmpty()
        val outputs = linkedMapOf<String, GeonextElementProperties>()
        for (
            (index, output) in
            data.getElementsByTagName(OUTPUT_TAG).withIndex()
        ) {
            val outputValues =
                linkedMapOf<String, GeonextPropertyValue>()
            when (val result = definitionProperties(outputValues, output)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (val result = colorProperties(outputValues, output)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            visualProperties(outputValues, output)
            firstLevelProperties(outputValues, output)
            if (
                type == PERPENDICULAR_COMPOSITION_TYPE &&
                index == 1
            ) {
                when (
                    val result = readNodes(
                        values = outputValues,
                        data = output,
                        nodeType = STRAIGHT_TAG,
                        prefix = STRAIGHT_PREFIX,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            transformProperties(outputValues, point = false)
            if (
                index == 0 &&
                (
                    type == ARROW_PARALLEL_COMPOSITION_TYPE ||
                        type == PERPENDICULAR_COMPOSITION_TYPE
                )
            ) {
                textByTagName(output, FIX_TAG)?.let { fixed ->
                    outputValues[FIXED_KEY] =
                        GeonextPropertyValue.Text(fixed)
                }
            }
            outputs[compositionOutputKey(index)] =
                GeonextElementProperties(outputValues.toMap())
        }
        return finishElementProperties(
            values = values,
            limits = limits,
            lists = mapOf(INPUTS_KEY to inputs.toList()),
            outputs = outputs.toMap(),
        )
    }

    /*
     * JSXGraph 1.13.3: src/reader/geonext.js ->
     * readNode text branch / subtreeToString.
     */
    private fun readTextProperties(
        values: MutableMap<String, GeonextPropertyValue>,
        data: XmlElement,
        limits: GeonextPropertyLimits,
    ): GMResult<GeonextElementProperties, GeonextPropertyError> {
        when (val result = colorProperties(values, data)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        visualProperties(values, data)
        firstLevelProperties(values, data)
        when (
            val result = readNodes(
                values = values,
                data = data,
                nodeType = DATA_TAG,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val textData = data.getElementsByTagName(DATA_TAG).firstOrNull()
            ?: return GMResult.Err(
                GeonextPropertyError.MissingTag(
                    parent = data.nodeName,
                    tag = DATA_TAG,
                ),
            )
        val content = textData.getElementsByTagName(CONTENT_TAG).firstOrNull()
            ?: return GMResult.Err(
                GeonextPropertyError.MissingTag(
                    parent = DATA_TAG,
                    tag = CONTENT_TAG,
                ),
            )
        val contentText = content.firstChild?.data
            ?: return GMResult.Err(
                GeonextPropertyError.MissingText(
                    parent = DATA_TAG,
                    tag = CONTENT_TAG,
                ),
            )
        val serializedContent =
            textData.getElementsByTagName(MP_TAG).firstOrNull()
                ?.let(::serializeChildren)
                ?: serializeChildren(content)
        values[MP_STRING_KEY] =
            GeonextPropertyValue.Text(serializedContent)
        values[CONDITION_KEY] = GeonextPropertyValue.Text(
            textByTagName(data, CONDITION_TAG).orEmpty(),
        )
        values[CONTENT_KEY] = GeonextPropertyValue.Text(contentText)
        values[FIXED_KEY] =
            textByTagName(data, FIX_TAG)?.let { value ->
                GeonextPropertyValue.Text(value)
            }
                ?: GeonextPropertyValue.Flag(false)
        values[AUTO_DIGITS_KEY] =
            textByTagName(data, DIGITS_TAG)?.let { value ->
                GeonextPropertyValue.Text(value)
            }
                ?: GeonextPropertyValue.Number(2.0)
        return finishElementProperties(
            values = values,
            limits = limits,
        )
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> readNode graph branch.
    private fun readGraphProperties(
        values: MutableMap<String, GeonextPropertyValue>,
        data: XmlElement,
        limits: GeonextPropertyLimits,
    ): GMResult<GeonextElementProperties, GeonextPropertyError> {
        when (val result = colorProperties(values, data)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        firstLevelProperties(values, data)
        val graphData = data.getElementsByTagName(DATA_TAG).firstOrNull()
            ?: return GMResult.Err(
                GeonextPropertyError.MissingTag(
                    parent = data.nodeName,
                    tag = DATA_TAG,
                ),
            )
        val function = when (
            val result = requiredTextByTagName(graphData, FUNCTION_TAG)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        values[FUNCTION_KEY] = GeonextPropertyValue.Text(function)
        return finishElementProperties(
            values = values,
            limits = limits,
        )
    }

    /*
     * JSXGraph 1.13.3: src/reader/geonext.js ->
     * readNode tracecurve branch.
     */
    private fun readTraceCurveProperties(
        values: MutableMap<String, GeonextPropertyValue>,
        data: XmlElement,
        limits: GeonextPropertyLimits,
    ): GMResult<GeonextElementProperties, GeonextPropertyError> {
        for (tag in listOf(TRACE_POINT_TAG, TRACE_SLIDER_TAG)) {
            val value = when (
                val result = requiredTextByTagName(data, tag)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            values[tag] = GeonextPropertyValue.Text(value)
        }
        return finishElementProperties(
            values = values,
            limits = limits,
        )
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> readNode group branch.
    private fun readGroupProperties(
        values: MutableMap<String, GeonextPropertyValue>,
        data: XmlElement,
        limits: GeonextPropertyLimits,
    ): GMResult<GeonextElementProperties, GeonextPropertyError> {
        when (val result = colorProperties(values, data)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        firstLevelProperties(values, data)
        val groupData = data.getElementsByTagName(DATA_TAG).firstOrNull()
            ?: return GMResult.Err(
                GeonextPropertyError.MissingTag(
                    parent = data.nodeName,
                    tag = DATA_TAG,
                ),
            )
        val members = mutableListOf<String>()
        for (member in groupData.getElementsByTagName(MEMBER_TAG)) {
            val value = member.firstChild?.data
                ?: return GMResult.Err(
                    GeonextPropertyError.MissingText(
                        parent = DATA_TAG,
                        tag = MEMBER_TAG,
                    ),
                )
            members += value
        }
        return finishElementProperties(
            values = values,
            limits = limits,
            lists = mapOf(MEMBERS_KEY to members.toList()),
        )
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> readNode polygon branch.
    private fun readPolygonProperties(
        values: MutableMap<String, GeonextPropertyValue>,
        data: XmlElement,
        limits: GeonextPropertyLimits,
    ): GMResult<GeonextElementProperties, GeonextPropertyError> {
        firstLevelProperties(values, data)
        val polygonData = data.getElementsByTagName(DATA_TAG).firstOrNull()
            ?: return GMResult.Err(
                GeonextPropertyError.MissingTag(
                    parent = data.nodeName,
                    tag = DATA_TAG,
                ),
            )
        val vertices = mutableListOf<String>()
        for (vertex in polygonData.getElementsByTagName(VERTEX_TAG)) {
            val value = vertex.firstChild?.data
                ?: return GMResult.Err(
                    GeonextPropertyError.MissingText(
                        parent = DATA_TAG,
                        tag = VERTEX_TAG,
                    ),
                )
            vertices += value
        }
        val borders = mutableListOf<GeonextElementProperties>()
        for (border in data.getElementsByTagName(BORDER_TAG)) {
            when (val result = readPolygonBorderProperties(border)) {
                is GMResult.Ok -> borders += result.value
                is GMResult.Err -> return result
            }
        }
        transformProperties(values, point = false)
        return finishElementProperties(
            values = values,
            limits = limits,
            lists = mapOf(VERTICES_KEY to vertices.toList()),
            borders = borders.toList(),
        )
    }

    private fun readPolygonBorderProperties(
        data: XmlElement,
    ): GMResult<GeonextElementProperties, GeonextPropertyError> {
        val values = linkedMapOf<String, GeonextPropertyValue>()
        for (tag in listOf(NAME_TAG, ID_TAG)) {
            val value = when (
                val result = requiredTextByTagName(data, tag)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            values[tag] = GeonextPropertyValue.Text(value)
        }
        val straight = data.getElementsByTagName(STRAIGHT_TAG).firstOrNull()
            ?: return GMResult.Err(
                GeonextPropertyError.MissingTag(
                    parent = data.nodeName,
                    tag = STRAIGHT_TAG,
                ),
            )
        for ((tag, key) in listOf(
            FIRST_TAG to STRAIGHT_FIRST_KEY,
            LAST_TAG to STRAIGHT_LAST_KEY,
        )) {
            val value = when (
                val result = requiredTextByTagName(straight, tag)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            values[key] = GeonextPropertyValue.Flag(
                str2Bool(value, missingIsFalse = true),
            )
        }
        val strokeWidth =
            textByTagName(data, STROKE_WIDTH_LOWER)
                ?: when (
                    val result = requiredTextByTagName(data, WIDTH_TAG)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
        values[STROKE_WIDTH_KEY] =
            GeonextPropertyValue.Text(strokeWidth)
        textByTagName(data, DASH_TAG)?.let { dash ->
            values[DASH_KEY] = GeonextPropertyValue.Flag(
                str2Bool(dash, missingIsFalse = true),
            )
        }
        for (tag in listOf(VISIBLE_TAG, DRAFT_TAG, TRACE_TAG)) {
            val value = when (
                val result = requiredTextByTagName(data, tag)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            values[tag] = GeonextPropertyValue.Flag(
                str2Bool(value, missingIsFalse = true),
            )
        }

        val color = data.getElementsByTagName(COLOR_TAG).firstOrNull()
            ?: return GMResult.Err(
                GeonextPropertyError.MissingTag(
                    parent = data.nodeName,
                    tag = COLOR_TAG,
                ),
            )
        val stroke = when (val result = readColor(color, STROKE_TAG)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val lighting = when (val result = readColor(color, LIGHTING_TAG)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fill = when (val result = readColor(color, FILL_TAG)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val label = when (
            val result = requiredTextByTagName(color, LABEL_TAG)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val draft = when (
            val result = requiredTextByTagName(color, DRAFT_TAG)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        values[STROKE_COLOR_KEY] = GeonextPropertyValue.Text(stroke.rgb)
        values[STROKE_OPACITY_KEY] =
            GeonextPropertyValue.Number(stroke.opacity)
        values[HIGHLIGHT_STROKE_COLOR_KEY] =
            GeonextPropertyValue.Text(lighting.rgb)
        values[HIGHLIGHT_STROKE_OPACITY_KEY] =
            GeonextPropertyValue.Number(lighting.opacity)
        values[FILL_COLOR_KEY] = GeonextPropertyValue.Text(fill.rgb)
        values[FILL_OPACITY_KEY] =
            GeonextPropertyValue.Number(fill.opacity)
        values[HIGHLIGHT_FILL_COLOR_KEY] =
            GeonextPropertyValue.Text(fill.rgb)
        values[HIGHLIGHT_FILL_OPACITY_KEY] =
            GeonextPropertyValue.Number(fill.opacity)
        values[LABEL_COLOR_KEY] = GeonextPropertyValue.Text(label)
        values[COLOR_DRAFT_KEY] = GeonextPropertyValue.Text(draft)
        return GMResult.Ok(GeonextElementProperties(values.toMap()))
    }

    private fun finishElementProperties(
        values: Map<String, GeonextPropertyValue>,
        limits: GeonextPropertyLimits,
        lists: Map<String, List<String>> = emptyMap(),
        borders: List<GeonextElementProperties> = emptyList(),
        outputs: Map<String, GeonextElementProperties> = emptyMap(),
    ): GMResult<GeonextElementProperties, GeonextPropertyError> {
        val propertyCount =
            values.size +
                lists.values.sumOf(List<String>::size) +
                borders.sumOf { border -> border.values.size } +
                outputs.values.sumOf { output -> output.values.size }
        if (propertyCount > limits.maxProperties) {
            return GMResult.Err(
                GeonextPropertyError.PropertyLimitExceeded(
                    limit = limits.maxProperties,
                    requested = propertyCount,
                ),
            )
        }
        return GMResult.Ok(
            GeonextElementProperties(
                values = values.toMap(),
                outputs = outputs,
                lists = lists,
                borders = borders,
            ),
        )
    }

    /*
     * JSXGraph 1.13.3: src/reader/geonext.js ->
     * readNode intersection branch.
     */
    private fun readIntersectionProperties(
        values: MutableMap<String, GeonextPropertyValue>,
        data: XmlElement,
        limits: GeonextPropertyLimits,
    ): GMResult<GeonextElementProperties, GeonextPropertyError> {
        values[STROKE_WIDTH_LOWER] = GeonextPropertyValue.Number(1.0)
        when (
            val result = readNodes(
                values = values,
                data = data,
                nodeType = DATA_TAG,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }

        val firstOutputNode =
            data.getElementsByTagName(FIRST_TAG).getOrNull(1)
                ?: return GMResult.Err(
                    GeonextPropertyError.MissingTag(
                        parent = data.nodeName,
                        tag = FIRST_TAG,
                    ),
                )
        val firstOutput = when (
            val result = readIntersectionOutput(firstOutputNode)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val outputs =
            linkedMapOf(FIRST_OUTPUT_KEY to firstOutput)
        data.getElementsByTagName(LAST_TAG).getOrNull(1)?.let { lastNode ->
            when (val result = readIntersectionOutput(lastNode)) {
                is GMResult.Ok -> outputs[LAST_OUTPUT_KEY] = result.value
                is GMResult.Err -> return result
            }
        }
        val propertyCount =
            values.size + outputs.values.sumOf { it.values.size }
        if (propertyCount > limits.maxProperties) {
            return GMResult.Err(
                GeonextPropertyError.PropertyLimitExceeded(
                    limit = limits.maxProperties,
                    requested = propertyCount,
                ),
            )
        }
        return GMResult.Ok(
            GeonextElementProperties(
                values = values.toMap(),
                outputs = outputs.toMap(),
            ),
        )
    }

    private fun readIntersectionOutput(
        data: XmlElement,
    ): GMResult<GeonextElementProperties, GeonextPropertyError> {
        val values = linkedMapOf<String, GeonextPropertyValue>()
        when (val result = colorProperties(values, data)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        visualProperties(values, data)
        firstLevelProperties(values, data)
        val fixed = when (
            val result = requiredTextByTagName(data, FIX_TAG)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        values[FIXED_KEY] = GeonextPropertyValue.Flag(
            str2Bool(fixed, missingIsFalse = true),
        )
        transformProperties(values, point = true)
        return GMResult.Ok(GeonextElementProperties(values.toMap()))
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> defProperties.
    private fun definitionProperties(
        values: MutableMap<String, GeonextPropertyValue>,
        data: XmlElement,
    ): GMResult<Unit, GeonextPropertyError> {
        values[IDENT_KEY] = GeonextPropertyValue.Text(data.nodeName)
        val name =
            if (data.nodeName in NAMELESS_ELEMENT_TYPES) {
                ""
            } else {
                when (val result = requiredTextByTagName(data, NAME_TAG)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }
        val id = when (val result = requiredTextByTagName(data, ID_TAG)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        values[NAME_KEY] = GeonextPropertyValue.Text(name)
        values[ID_KEY] = GeonextPropertyValue.Text(id)
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> colorProperties.
    private fun colorProperties(
        values: MutableMap<String, GeonextPropertyValue>,
        data: XmlElement,
    ): GMResult<Unit, GeonextPropertyError> {
        val color = data.getElementsByTagName(COLOR_TAG).firstOrNull()
            ?: return GMResult.Err(
                GeonextPropertyError.MissingTag(
                    parent = data.nodeName,
                    tag = COLOR_TAG,
                ),
            )
        val stroke = when (val result = readColor(color, STROKE_TAG)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val lighting = when (val result = readColor(color, LIGHTING_TAG)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fill = when (val result = readColor(color, FILL_TAG)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val label = when (val result = readColor(color, LABEL_TAG)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val draft = when (val result = readColor(color, DRAFT_TAG)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        values[STROKE_COLOR_KEY] =
            GeonextPropertyValue.Text(stroke.rgb)
        values[STROKE_OPACITY_KEY] =
            GeonextPropertyValue.Number(stroke.opacity)
        values[HIGHLIGHT_STROKE_COLOR_KEY] =
            GeonextPropertyValue.Text(lighting.rgb)
        values[HIGHLIGHT_STROKE_OPACITY_KEY] =
            GeonextPropertyValue.Number(lighting.opacity)
        values[FILL_COLOR_KEY] =
            GeonextPropertyValue.Text(fill.rgb)
        values[FILL_OPACITY_KEY] =
            GeonextPropertyValue.Number(fill.opacity)
        values[HIGHLIGHT_FILL_COLOR_KEY] =
            GeonextPropertyValue.Text(fill.rgb)
        values[HIGHLIGHT_FILL_OPACITY_KEY] =
            GeonextPropertyValue.Number(fill.opacity)
        values[LABEL_COLOR_KEY] =
            GeonextPropertyValue.Text(label.rgb)
        values[WITH_LABEL_KEY] =
            GeonextPropertyValue.Flag(label.opacity > 0.0)
        values[LABEL_OPACITY_KEY] =
            GeonextPropertyValue.Number(label.opacity)
        values[COLOR_DRAFT_KEY] =
            GeonextPropertyValue.Text(draft.rgb)
        values[COLOR_STROKE_KEY] =
            GeonextPropertyValue.Text(stroke.rgb)
        values[COLOR_FILL_KEY] =
            GeonextPropertyValue.Text(fill.rgb)
        values[COLOR_LABEL_KEY] =
            GeonextPropertyValue.Text(label.rgb)
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> visualProperties.
    private fun visualProperties(
        values: MutableMap<String, GeonextPropertyValue>,
        data: XmlElement,
    ) {
        values[VISIBLE_KEY] = GeonextPropertyValue.Flag(
            str2Bool(
                textByTagName(data, VISIBLE_TAG),
                missingIsFalse = true,
            ),
        )
        values[TRACE_KEY] = GeonextPropertyValue.Flag(
            str2Bool(
                textByTagName(data, TRACE_TAG),
                missingIsFalse = true,
            ),
        )
    }

    /*
     * JSXGraph 1.13.3: src/reader/geonext.js ->
     * firstLevelProperties.
     */
    private fun firstLevelProperties(
        values: MutableMap<String, GeonextPropertyValue>,
        data: XmlElement,
    ) {
        for (child in data.childElements()) {
            if (
                child.nodeName != DATA_TAG &&
                child.nodeName != STRAIGHT_TAG
            ) {
                child.firstChild?.data?.let { text ->
                    val key =
                        if (child.nodeName == WIDTH_TAG) {
                            STROKE_WIDTH_LOWER
                        } else {
                            child.nodeName
                        }
                    values[key] = GeonextPropertyValue.Text(text)
                }
            }
        }
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> readNodes.
    private fun readNodes(
        values: MutableMap<String, GeonextPropertyValue>,
        data: XmlElement,
        nodeType: String,
        prefix: String? = null,
    ): GMResult<Unit, GeonextPropertyError> {
        val parent = data.getElementsByTagName(nodeType).firstOrNull()
            ?: return GMResult.Err(
                GeonextPropertyError.MissingTag(
                    parent = data.nodeName,
                    tag = nodeType,
                ),
            )
        for (child in parent.childElements()) {
            val text = child.firstChild?.data ?: continue
            val key =
                if (prefix != null) {
                    prefix + capitalize(child.nodeName)
                } else {
                    child.nodeName
                }
            values[key] = GeonextPropertyValue.Text(text)
        }
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/geonext.js ->
     * transformProperties.
     */
    private fun transformProperties(
        values: MutableMap<String, GeonextPropertyValue>,
        point: Boolean,
    ) {
        values[STROKE_WIDTH_KEY] =
            values[STROKE_WIDTH_LOWER] ?: GeonextPropertyValue.Text("")
        val styleIndex = values[STYLE_KEY].asText()?.let(::jsParseInt)
        values[FACE_KEY] = GeonextPropertyValue.Text(
            FACE_MAP.getOrNull(styleIndex ?: -1) ?: "cross",
        )
        values[SIZE_KEY] = GeonextPropertyValue.Number(
            SIZE_MAP.getOrNull(styleIndex ?: -1)?.toDouble() ?: 3.0,
        )
        values[STRAIGHT_FIRST_KEY] = GeonextPropertyValue.Flag(
            str2Bool(values[STRAIGHT_FIRST_KEY]),
        )
        values[STRAIGHT_LAST_KEY] = GeonextPropertyValue.Flag(
            str2Bool(values[STRAIGHT_LAST_KEY]),
        )
        values[VISIBLE_KEY] = GeonextPropertyValue.Flag(
            str2Bool(values[VISIBLE_KEY]),
        )
        values[DRAFT_KEY] = GeonextPropertyValue.Flag(
            str2Bool(values[DRAFT_KEY]),
        )
        values[TRACE_KEY] = GeonextPropertyValue.Flag(
            str2Bool(values[TRACE_KEY]),
        )

        if (point) {
            values[FILL_COLOR_KEY] = values.getValue(STROKE_COLOR_KEY)
            values[HIGHLIGHT_FILL_COLOR_KEY] =
                values.getValue(HIGHLIGHT_STROKE_COLOR_KEY)
            values[FILL_OPACITY_KEY] = values.getValue(STROKE_OPACITY_KEY)
            values[HIGHLIGHT_FILL_OPACITY_KEY] =
                values.getValue(HIGHLIGHT_STROKE_OPACITY_KEY)
        }

        if (values[LABEL_KEY] is GeonextPropertyValue.Text) {
            values.remove(LABEL_KEY)
        }
        if (!isTruthy(values[LABEL_KEY])) {
            values[LABEL_KEY] = GeonextPropertyValue.Label(
                opacity = values[LABEL_OPACITY_KEY].asNumber(),
            )
        }
        for (key in REMOVED_KEYS) {
            values.remove(key)
        }
    }

    private fun readColor(
        color: XmlElement,
        tag: String,
    ): GMResult<com.swithun.jsxgraph.core.utils.RgboColor, GeonextPropertyError> {
        val value = when (val result = requiredTextByTagName(color, tag)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (val result = Color.rgba2rgbo(value)) {
            is GMResult.Ok -> result
            is GMResult.Err -> {
                GMResult.Err(
                    GeonextPropertyError.ColorConversionFailed(
                        tag = tag,
                        cause = result.error,
                    ),
                )
            }
        }
    }

    private fun requiredTextByTagName(
        node: XmlElement,
        tag: String,
    ): GMResult<String, GeonextPropertyError> {
        val element = node.getElementsByTagName(tag).firstOrNull()
            ?: return GMResult.Err(
                GeonextPropertyError.MissingTag(
                    parent = node.nodeName,
                    tag = tag,
                ),
            )
        val text = element.firstChild?.data
            ?: return GMResult.Err(
                GeonextPropertyError.MissingText(
                    parent = node.nodeName,
                    tag = tag,
                ),
            )
        return GMResult.Ok(text)
    }

    private fun textByTagName(
        node: XmlElement,
        tag: String,
    ): String? =
        node.getElementsByTagName(tag).firstOrNull()?.firstChild?.data

    private fun GeonextPropertyValue?.asText(): String? =
        (this as? GeonextPropertyValue.Text)?.value

    private fun GeonextPropertyValue?.asNumber(): Double =
        when (this) {
            is GeonextPropertyValue.Number -> value
            is GeonextPropertyValue.Text -> jsParseFloat(value)
            else -> Double.NaN
        }

    private fun str2Bool(
        value: String?,
        missingIsFalse: Boolean,
    ): Boolean =
        if (value == null) {
            !missingIsFalse
        } else {
            value.lowercase() == "true"
        }

    private fun str2Bool(value: GeonextPropertyValue?): Boolean =
        when (value) {
            null -> true
            is GeonextPropertyValue.Flag -> value.value
            is GeonextPropertyValue.Text ->
                value.value.lowercase() == "true"
            else -> false
        }

    private fun isTruthy(value: GeonextPropertyValue?): Boolean =
        when (value) {
            null -> false
            is GeonextPropertyValue.Text -> value.value.isNotEmpty()
            is GeonextPropertyValue.Number ->
                value.value != 0.0 && !value.value.isNaN()
            is GeonextPropertyValue.Flag -> value.value
            is GeonextPropertyValue.Label -> true
        }

    internal fun jsParseInt(value: String?): Int? {
        val prefix = value
            ?.trimStart()
            ?.let(INTEGER_PREFIX::find)
            ?.value
            ?: return null
        return prefix.toIntOrNull()
    }

    internal fun jsParseFloat(value: String?): Double {
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

    private fun capitalize(value: String): String =
        value.take(1).uppercase() + value.drop(1).lowercase()

    private fun validateLimits(
        limits: GeonextPropertyLimits,
    ): GeonextPropertyError.InvalidLimits? {
        val invalid = when {
            limits.maxElements < 0 ->
                "maxElements" to limits.maxElements
            limits.maxProperties < 0 ->
                "maxProperties" to limits.maxProperties
            else -> null
        }
        return invalid?.let { (name, value) ->
            GeonextPropertyError.InvalidLimits(name, value)
        }
    }

    private fun XmlElement.childElements(): List<XmlElement> =
        childNodes.filterIsInstance<XmlElement>()

    private fun serializeChildren(element: XmlElement): String =
        element.childNodes.joinToString(separator = "", transform = ::serializeNode)

    private fun serializeNode(node: XmlNode): String =
        when (node) {
            is XmlElement -> {
                val attributes = node.attributes.joinToString(
                    separator = "",
                    prefix = if (node.attributes.length == 0) "" else " ",
                ) { attribute ->
                    "${attribute.nodeName}=\"${escapeAttribute(attribute.value)}\""
                }
                if (node.childNodes.isEmpty()) {
                    "<${node.nodeName}$attributes/>"
                } else {
                    "<${node.nodeName}$attributes>" +
                        node.childNodes.joinToString(
                            separator = "",
                            transform = ::serializeNode,
                        ) +
                        "</${node.nodeName}>"
                }
            }
            is XmlText -> escapeText(node.data)
            is XmlCDataSection -> "<![CDATA[${node.data}]]>"
            is XmlComment -> "<!--${node.data}-->"
            is XmlProcessingInstruction ->
                if (node.data.isEmpty()) {
                    "<?${node.nodeName}?>"
                } else {
                    "<?${node.nodeName} ${node.data}?>"
                }
            is XmlDocumentType -> "<!DOCTYPE ${node.nodeName}>"
            else -> node.childNodes.joinToString(
                separator = "",
                transform = ::serializeNode,
            )
        }

    private fun escapeText(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private fun escapeAttribute(value: String): String =
        escapeText(value)
            .replace("\"", "&quot;")
            .replace("\t", "&#9;")
            .replace("\n", "&#10;")
            .replace("\r", "&#13;")

    private const val ELEMENTS_TAG = "elements"
    private const val POINT_TAG = "point"
    private const val LINE_TAG = "line"
    private const val CIRCLE_TAG = "circle"
    private const val ARROW_TAG = "arrow"
    private const val INTERSECTION_TAG = "intersection"
    private const val ARC_TAG = "arc"
    private const val ANGLE_TAG = "angle"
    private const val POLYGON_TAG = "polygon"
    private const val GRAPH_TAG = "graph"
    private const val PARAMETER_CURVE_TAG = "parametercurve"
    private const val SLIDER_TAG = "slider"
    private const val TRACE_CURVE_TAG = "tracecurve"
    private const val GROUP_TAG = "group"
    private const val TEXT_TAG = "text"
    private const val COMPOSITION_TAG = "composition"
    private const val DATA_TAG = "data"
    private const val INPUT_TAG = "input"
    private const val OUTPUT_TAG = "output"
    private const val FIRST_TAG = "first"
    private const val LAST_TAG = "last"
    private const val VERTEX_TAG = "vertex"
    private const val BORDER_TAG = "border"
    private const val STRAIGHT_TAG = "straight"
    private const val STRAIGHT_PREFIX = "straight"
    private const val ANIMATE_TAG = "animate"
    private const val ANIMATE_PREFIX = "animate"
    private const val COLOR_TAG = "color"
    private const val STROKE_TAG = "stroke"
    private const val LIGHTING_TAG = "lighting"
    private const val FILL_TAG = "fill"
    private const val LABEL_TAG = "label"
    private const val DRAFT_TAG = "draft"
    private const val NAME_TAG = "name"
    private const val ID_TAG = "id"
    private const val FIX_TAG = "fix"
    private const val VISIBLE_TAG = "visible"
    private const val TRACE_TAG = "trace"
    private const val WIDTH_TAG = "width"
    private const val DASH_TAG = "dash"
    private const val FUNCTION_TAG = "function"
    private const val FUNCTION_X_TAG = "functionx"
    private const val FUNCTION_Y_TAG = "functiony"
    private const val MIN_TAG = "min"
    private const val MAX_TAG = "max"
    private const val TRACE_POINT_TAG = "tracepoint"
    private const val TRACE_SLIDER_TAG = "traceslider"
    private const val MEMBER_TAG = "member"
    private const val MP_TAG = "mp"
    private const val CONTENT_TAG = "content"
    private const val CONDITION_TAG = "condition"
    private const val DIGITS_TAG = "digits"
    private const val VIEWPORT_TAG = "viewport"
    private const val LEFT_TAG = "left"
    private const val TOP_TAG = "top"
    private const val RIGHT_TAG = "right"
    private const val BOTTOM_TAG = "bottom"
    private const val MIDPOINT_TAG = "midpoint"
    private const val RADIUS_TAG = "radius"
    private const val RADIUS_VALUE_TAG = "radiusvalue"
    private const val FIRST_ARROW_TAG = "firstarrow"
    private const val LAST_ARROW_TAG = "lastarrow"
    private const val TYPE_KEY = "type"

    private const val IDENT_KEY = "ident"
    private const val NAME_KEY = "name"
    private const val ID_KEY = "id"
    private const val STROKE_WIDTH_LOWER = "strokewidth"
    private const val STROKE_WIDTH_KEY = "strokeWidth"
    private const val STROKE_COLOR_KEY = "strokeColor"
    private const val STROKE_OPACITY_KEY = "strokeOpacity"
    private const val HIGHLIGHT_STROKE_COLOR_KEY = "highlightStrokeColor"
    private const val HIGHLIGHT_STROKE_OPACITY_KEY = "highlightStrokeOpacity"
    private const val FILL_COLOR_KEY = "fillColor"
    private const val FILL_OPACITY_KEY = "fillOpacity"
    private const val HIGHLIGHT_FILL_COLOR_KEY = "highlightFillColor"
    private const val HIGHLIGHT_FILL_OPACITY_KEY = "highlightFillOpacity"
    private const val LABEL_COLOR_KEY = "labelColor"
    private const val WITH_LABEL_KEY = "withLabel"
    private const val LABEL_OPACITY_KEY = "labelOpacity"
    private const val COLOR_DRAFT_KEY = "colorDraft"
    private const val COLOR_STROKE_KEY = "colorStroke"
    private const val COLOR_FILL_KEY = "colorFill"
    private const val COLOR_LABEL_KEY = "colorLabel"
    private const val VISIBLE_KEY = "visible"
    private const val TRACE_KEY = "trace"
    private const val DRAFT_KEY = "draft"
    private const val DASH_KEY = "dash"
    private const val FIXED_KEY = "fixed"
    private const val MP_STRING_KEY = "mpStr"
    private const val CONDITION_KEY = "condition"
    private const val CONTENT_KEY = "content"
    private const val AUTO_DIGITS_KEY = "autodigits"
    private const val STYLE_KEY = "style"
    private const val FACE_KEY = "face"
    private const val SIZE_KEY = "size"
    private const val STRAIGHT_FIRST_KEY = "straightFirst"
    private const val STRAIGHT_LAST_KEY = "straightLast"
    private const val LABEL_KEY = "label"
    private const val CENTER_KEY = "center"
    private const val RADIUS_KEY = "radius"
    private const val FIRST_ARROW_KEY = "firstArrow"
    private const val LAST_ARROW_KEY = "lastArrow"
    private const val FUNCTION_KEY = "function"
    internal const val VERTICES_KEY = "vertices"
    internal const val MEMBERS_KEY = "members"
    internal const val INPUTS_KEY = "inputs"
    internal const val FIRST_OUTPUT_KEY = "firstOutput"
    internal const val LAST_OUTPUT_KEY = "lastOutput"

    internal fun compositionOutputKey(index: Int): String =
        "compositionOutput$index"

    private const val ARROW_PARALLEL_COMPOSITION_TYPE = "210070"
    private const val PERPENDICULAR_COMPOSITION_TYPE = "210160"

    private val PARAMETER_CURVE_VALUE_TAGS = listOf(
        FUNCTION_X_TAG,
        FUNCTION_Y_TAG,
        MIN_TAG,
        MAX_TAG,
    )

    private val ORIGIN_IDS = setOf(
        "gOOe0",
        "gXOe0",
        "gYOe0",
        "gXLe0",
        "gYLe0",
    )
    private val NAMELESS_ELEMENT_TYPES = setOf(
        "text",
        "intersection",
        "composition",
    )
    private val FACE_MAP = listOf(
        "cross",
        "cross",
        "cross",
        "circle",
        "circle",
        "circle",
        "circle",
        "square",
        "square",
        "square",
        "plus",
        "plus",
        "plus",
    )
    private val SIZE_MAP = listOf(
        2,
        3,
        4,
        1,
        2,
        3,
        4,
        2,
        3,
        4,
        2,
        3,
        4,
    )
    private val REMOVED_KEYS = listOf(
        "color",
        "dash",
        "style",
        "style",
        "ident",
        "colordraft",
        "colorstroke",
        "colorfill",
        "colorlabel",
        "active",
        "area",
        "showinfo",
        "showcoord",
        "fix",
    )
    private val INTEGER_PREFIX = Regex("^[+-]?\\d+")
    private val FLOAT_PREFIX =
        Regex(
            "^[+-]?(?:Infinity|" +
                "(?:(?:\\d+\\.?\\d*|\\.\\d+)" +
                "(?:[eE][+-]?\\d+)?))",
        )
}
