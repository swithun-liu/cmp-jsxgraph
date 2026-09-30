/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/geonext.js -> readNode point, line, circle, arrow,
 * intersection, arc, angle, polygon, graph, parametercurve, slider,
 * tracecurve, group, and text branches.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.AngleRadius
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.ArcError
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.BisectorLine
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.CircleError
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.CircumcenterError
import com.swithun.jsxgraph.core.base.CircumcenterPoint
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.CurveError
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Glider
import com.swithun.jsxgraph.core.base.GliderError
import com.swithun.jsxgraph.core.base.Group
import com.swithun.jsxgraph.core.base.GroupError
import com.swithun.jsxgraph.core.base.IntersectionError
import com.swithun.jsxgraph.core.base.IntersectionIndexSource
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.LineError
import com.swithun.jsxgraph.core.base.MidpointError
import com.swithun.jsxgraph.core.base.MidpointPoint
import com.swithun.jsxgraph.core.base.OrthogonalConstructionError
import com.swithun.jsxgraph.core.base.OrthogonalPoint
import com.swithun.jsxgraph.core.base.OrthogonalPointKind
import com.swithun.jsxgraph.core.base.ParallelConstructionError
import com.swithun.jsxgraph.core.base.ParallelPoint
import com.swithun.jsxgraph.core.base.PerpendicularSegmentLine
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.base.PointReflectionError
import com.swithun.jsxgraph.core.base.PointReflections
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.PolygonError
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.SectorError
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.base.TextError
import com.swithun.jsxgraph.core.base.TriangleCenterConstructionError
import com.swithun.jsxgraph.core.utils.XML
import com.swithun.jsxgraph.core.utils.XmlElement
import com.swithun.jsxgraph.core.utils.XmlError
import com.swithun.jsxgraph.core.utils.XmlLimits

internal data class GeonextReaderLimits(
    val preparation: ReaderPreparationLimits = ReaderPreparationLimits(),
    val xml: XmlLimits = XmlLimits(),
    val maxElements: Int = 100_000,
    val maxProperties: Int = 10_000,
)

internal sealed interface GeonextReaderError : ReaderDomainError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : GeonextReaderError

    data class PreparationFailed(
        val cause: ReaderPreparationError,
    ) : GeonextReaderError

    data class XmlParsingFailed(
        val cause: XmlError,
    ) : GeonextReaderError

    data class MissingSection(
        val name: String,
    ) : GeonextReaderError

    data class ElementLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : GeonextReaderError

    data class PropertyParsingFailed(
        val elementIndex: Int,
        val elementType: String,
        val cause: GeonextPropertyError,
    ) : GeonextReaderError

    data class UnsupportedElements(
        val elementIndices: List<Int>,
    ) : GeonextReaderError

    data class UnsupportedCompositionType(
        val definitionId: String,
        val type: String,
    ) : GeonextReaderError

    data class InvalidCompositionArity(
        val definitionId: String,
        val type: String,
        val expected: String,
        val actual: Int,
    ) : GeonextReaderError

    data class MissingCompositionOutput(
        val definitionId: String,
        val type: String,
        val outputIndex: Int,
    ) : GeonextReaderError

    data class MissingReference(
        val definitionId: String,
        val referenceId: String,
        val expectedType: String,
    ) : GeonextReaderError

    data class ReferenceTypeMismatch(
        val definitionId: String,
        val referenceId: String,
        val expectedType: String,
        val actualType: String,
    ) : GeonextReaderError

    data class BoardCreationFailed(
        val definitionId: String,
        val cause: GeonextCreationError,
    ) : GeonextReaderError
}

internal sealed interface GeonextCreationError {
    data class Point(val cause: PointError) : GeonextCreationError

    data class Line(val cause: LineError) : GeonextCreationError

    data class Circle(val cause: CircleError) : GeonextCreationError

    data class Intersection(
        val cause: IntersectionError,
    ) : GeonextCreationError

    data class Arc(val cause: ArcError) : GeonextCreationError

    data class Angle(val cause: SectorError) : GeonextCreationError

    data class Polygon(val cause: PolygonError) : GeonextCreationError

    data class Curve(val cause: CurveError) : GeonextCreationError

    data class Glider(val cause: GliderError) : GeonextCreationError

    data class Group(val cause: GroupError) : GeonextCreationError

    data class Text(val cause: TextError) : GeonextCreationError

    data class Midpoint(val cause: MidpointError) : GeonextCreationError

    data class Circumcenter(
        val cause: CircumcenterError,
    ) : GeonextCreationError

    data class PointReflection(
        val cause: PointReflectionError,
    ) : GeonextCreationError

    data class Parallel(
        val cause: ParallelConstructionError,
    ) : GeonextCreationError

    data class Orthogonal(
        val cause: OrthogonalConstructionError,
    ) : GeonextCreationError

    data class TriangleCenter(
        val cause: TriangleCenterConstructionError,
    ) : GeonextCreationError
}

internal sealed interface GeonextReaderDiagnostic {
    data class UnsupportedElement(
        val elementIndex: Int,
        val elementType: String,
        val elementId: String?,
    ) : GeonextReaderDiagnostic
}

internal sealed interface GeonextDefinition {
    val id: String
    val name: String
    val properties: GeonextElementProperties
}

internal data class GeonextPointDefinition(
    override val id: String,
    override val name: String,
    val x: Double,
    val y: Double,
    val fixed: Boolean,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class GeonextLineDefinition(
    override val id: String,
    override val name: String,
    val firstId: String,
    val lastId: String,
    val straightFirst: Boolean,
    val straightLast: Boolean,
    val arrow: Boolean,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal sealed interface GeonextCircleRadius {
    data class PointReference(
        val id: String,
    ) : GeonextCircleRadius

    data class ValueExpression(
        val source: String,
    ) : GeonextCircleRadius
}

internal data class GeonextCircleDefinition(
    override val id: String,
    override val name: String,
    val centerId: String,
    val radius: GeonextCircleRadius,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class GeonextIntersectionOutput(
    val id: String,
    val name: String,
    val fixed: Boolean,
    val properties: GeonextElementProperties,
)

internal data class GeonextIntersectionDefinition(
    override val id: String,
    override val name: String,
    val firstId: String,
    val lastId: String,
    val firstOutput: GeonextIntersectionOutput,
    val lastOutput: GeonextIntersectionOutput?,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class GeonextArcDefinition(
    override val id: String,
    override val name: String,
    val centerId: String,
    val radiusId: String,
    val angleId: String,
    val firstArrow: Boolean,
    val lastArrow: Boolean,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class GeonextAngleDefinition(
    override val id: String,
    override val name: String,
    val firstId: String,
    val middleId: String,
    val lastId: String,
    val radius: Double,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class GeonextPolygonBorderDefinition(
    val id: String,
    val name: String,
    val straightFirst: Boolean,
    val straightLast: Boolean,
    val properties: GeonextElementProperties,
)

internal data class GeonextPolygonDefinition(
    override val id: String,
    override val name: String,
    val vertexIds: List<String>,
    val borders: List<GeonextPolygonBorderDefinition>,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class GeonextGraphDefinition(
    override val id: String,
    override val name: String,
    val function: String,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class GeonextParameterCurveDefinition(
    override val id: String,
    override val name: String,
    val functionX: String,
    val functionY: String,
    val minimum: String,
    val maximum: String,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class GeonextSliderDefinition(
    override val id: String,
    override val name: String,
    val x: Double,
    val y: Double,
    val parentId: String,
    val position: Double,
    val fixed: Boolean,
    val onPolygon: Boolean,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class GeonextTraceCurveDefinition(
    override val id: String,
    override val name: String,
    val tracePointId: String,
    val traceSliderId: String,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class GeonextGroupDefinition(
    override val id: String,
    override val name: String,
    val memberIds: List<String>,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class GeonextTextDefinition(
    override val id: String,
    override val name: String,
    val x: Double,
    val y: Double,
    val content: String,
    val parentId: String?,
    val digits: Int,
    val fixed: Boolean,
    val visible: Boolean,
    val strokeColor: String?,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class GeonextCompositionOutput(
    val id: String,
    val name: String,
    val properties: GeonextElementProperties,
)

internal data class GeonextCompositionDefinition(
    override val id: String,
    override val name: String,
    val type: String,
    val inputIds: List<String>,
    val outputs: List<GeonextCompositionOutput>,
    override val properties: GeonextElementProperties,
) : GeonextDefinition

internal data class ParsedGeonext(
    val definitions: List<GeonextDefinition>,
    val diagnostics: List<GeonextReaderDiagnostic>,
)

internal data class DrawnGeonext(
    val parsed: ParsedGeonext,
    val objects: Map<String, GeometryElement>,
    val groups: Map<String, Group> = emptyMap(),
)

private data class CreatedGeonext(
    val objects: Map<String, GeometryElement> = emptyMap(),
    val groups: Map<String, Group> = emptyMap(),
)

/**
 * Source-mapped GEONExT reader slice for point, line, circle, arrow,
 * intersection, arc, angle, polygon, graph, parametercurve, slider,
 * tracecurve, group, and text. Remaining readNode branches are reported
 * explicitly.
 */
internal class GeonextReader(
    private val data: String,
) {
    // JSXGraph 1.13.3: src/reader/geonext.js -> constructor and readNode.
    internal fun parse(
        limits: GeonextReaderLimits = GeonextReaderLimits(),
    ): GMResult<ParsedGeonext, GeonextReaderError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        val prepared = when (
            val result = ReaderPreparation.prepareGeonext(
                source = data,
                limits = limits.preparation,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    GeonextReaderError.PreparationFailed(result.error),
                )
            }
        }
        val tree = when (
            val result = XML.parse(
                input = prepared,
                limits = limits.xml,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    GeonextReaderError.XmlParsingFailed(result.error),
                )
            }
        }
        val elements = tree.getElementsByTagName(ELEMENTS_TAG).firstOrNull()
            ?: return GMResult.Err(
                GeonextReaderError.MissingSection(ELEMENTS_TAG),
            )
        val elementNodes = elements.childElements()
        if (elementNodes.size > limits.maxElements) {
            return GMResult.Err(
                GeonextReaderError.ElementLimitExceeded(
                    limit = limits.maxElements,
                    requested = elementNodes.size,
                ),
            )
        }

        val propertyLimits = GeonextPropertyLimits(
            preparation = limits.preparation,
            xml = limits.xml,
            maxElements = limits.maxElements,
            maxProperties = limits.maxProperties,
        )
        val definitions = mutableListOf<GeonextDefinition>()
        val diagnostics = mutableListOf<GeonextReaderDiagnostic>()
        for ((index, element) in elementNodes.withIndex()) {
            if (element.nodeName !in SUPPORTED_ELEMENT_TYPES) {
                diagnostics += GeonextReaderDiagnostic.UnsupportedElement(
                    elementIndex = index,
                    elementType = element.nodeName,
                    elementId = textByTagName(element, ID_TAG),
                )
                continue
            }
            if (
                element.nodeName == TEXT_TAG &&
                textByTagName(element, ID_TAG)?.contains(OLD_VERSION_MARKER) ==
                true
            ) {
                continue
            }
            val properties = when (
                val result = GeonextProperties.readElementProperties(
                    data = element,
                    limits = propertyLimits,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return GMResult.Err(
                        GeonextReaderError.PropertyParsingFailed(
                            elementIndex = index,
                            elementType = element.nodeName,
                            cause = result.error,
                        ),
                    )
                }
            }
            if (element.nodeName == COMPOSITION_TAG) {
                val type = properties.text(TYPE_KEY).orEmpty()
                if (type !in SUPPORTED_COMPOSITION_TYPES) {
                    return GMResult.Err(
                        GeonextReaderError.UnsupportedCompositionType(
                            definitionId =
                                properties.text(ID_KEY).orEmpty(),
                            type = type,
                        ),
                    )
                }
            }
            definitions += parseDefinition(element, properties)
        }
        return GMResult.Ok(
            ParsedGeonext(
                definitions = definitions.toList(),
                diagnostics = diagnostics.toList(),
            ),
        )
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> read / readNode.
    internal fun read(
        board: Board,
        limits: GeonextReaderLimits = GeonextReaderLimits(),
        failOnUnsupportedElements: Boolean = false,
    ): GMResult<DrawnGeonext, GeonextReaderError> {
        val parsed = when (val result = parse(limits)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            failOnUnsupportedElements &&
            parsed.diagnostics.isNotEmpty()
        ) {
            return GMResult.Err(
                GeonextReaderError.UnsupportedElements(
                    parsed.diagnostics.map { diagnostic ->
                        when (diagnostic) {
                            is GeonextReaderDiagnostic.UnsupportedElement ->
                                diagnostic.elementIndex
                        }
                    },
                ),
            )
        }

        val existingIds = board.objects.keys.toSet()
        val existingNames = board.elementsByName.toMap()
        val existingGroups = board.groups.toMap()
        val existingGroupMemberships = board.objects.values
            .filterIsInstance<CoordsElement>()
            .associateWith { element -> element.groups.toList() }
        val objects = linkedMapOf<String, GeometryElement>()
        val groups = linkedMapOf<String, Group>()
        board.suspendUpdate()
        for (definition in parsed.definitions) {
            val created = when (
                val result = createDefinition(
                    board = board,
                    objects = objects,
                    definition = definition,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    rollbackBoardRead(
                        board = board,
                        existingIds = existingIds,
                        existingNames = existingNames,
                        existingGroups = existingGroups,
                        existingGroupMemberships = existingGroupMemberships,
                    )
                    return result
                }
            }
            objects.putAll(created.objects)
            groups.putAll(created.groups)
        }
        board.unsuspendUpdate()
        return GMResult.Ok(
            DrawnGeonext(
                parsed = parsed,
                objects = objects.toMap(),
                groups = groups.toMap(),
            ),
        )
    }

    private fun parseDefinition(
        element: XmlElement,
        properties: GeonextElementProperties,
    ): GeonextDefinition {
        val id = properties.text(ID_KEY).orEmpty()
        val name = properties.text(NAME_KEY).orEmpty()
        return when (element.nodeName) {
            POINT_TAG -> GeonextPointDefinition(
                id = id,
                name = name,
                x = GeonextProperties.jsParseFloat(
                    properties.text(X_KEY),
                ),
                y = GeonextProperties.jsParseFloat(
                    properties.text(Y_KEY),
                ),
                fixed = properties.flag(FIXED_KEY),
                properties = properties,
            )
            LINE_TAG,
            ARROW_TAG,
            -> GeonextLineDefinition(
                id = id,
                name = name,
                firstId = properties.text(FIRST_KEY).orEmpty(),
                lastId = properties.text(LAST_KEY).orEmpty(),
                straightFirst = properties.flag(STRAIGHT_FIRST_KEY),
                straightLast = properties.flag(STRAIGHT_LAST_KEY),
                arrow = element.nodeName == ARROW_TAG,
                properties = properties,
            )
            CIRCLE_TAG -> {
                val circleData =
                    element.getElementsByTagName(DATA_TAG).firstOrNull()
                val radius =
                    if (
                        circleData
                            ?.getElementsByTagName(RADIUS_TAG)
                            ?.isNotEmpty() == true
                    ) {
                        GeonextCircleRadius.PointReference(
                            properties.text(RADIUS_KEY).orEmpty(),
                        )
                    } else {
                        GeonextCircleRadius.ValueExpression(
                            properties.text(RADIUS_KEY).orEmpty(),
                        )
                    }
                GeonextCircleDefinition(
                    id = id,
                    name = name,
                    centerId = properties.text(CENTER_KEY).orEmpty(),
                    radius = radius,
                    properties = properties,
                )
            }
            INTERSECTION_TAG -> {
                val firstOutput =
                    properties.outputs.getValue(
                        GeonextProperties.FIRST_OUTPUT_KEY,
                    )
                val lastOutput =
                    properties.outputs[
                        GeonextProperties.LAST_OUTPUT_KEY
                    ]
                GeonextIntersectionDefinition(
                    id = id,
                    name = name,
                    firstId = properties.text(FIRST_KEY).orEmpty(),
                    lastId = properties.text(LAST_KEY).orEmpty(),
                    firstOutput = firstOutput.toIntersectionOutput(),
                    lastOutput = lastOutput?.toIntersectionOutput(),
                    properties = properties,
                )
            }
            ARC_TAG -> GeonextArcDefinition(
                id = id,
                name = name,
                centerId = properties.text(MIDPOINT_KEY).orEmpty(),
                radiusId = properties.text(RADIUS_KEY).orEmpty(),
                angleId = properties.text(ANGLE_KEY).orEmpty(),
                firstArrow = properties.flag(FIRST_ARROW_KEY),
                lastArrow = properties.flag(LAST_ARROW_KEY),
                properties = properties,
            )
            POLYGON_TAG -> GeonextPolygonDefinition(
                id = id,
                name = name,
                vertexIds = properties.lists[
                    GeonextProperties.VERTICES_KEY
                ].orEmpty().dropLast(1),
                borders = properties.borders.map { border ->
                    GeonextPolygonBorderDefinition(
                        id = border.text(ID_KEY).orEmpty(),
                        name = border.text(NAME_KEY).orEmpty(),
                        straightFirst = border.flag(STRAIGHT_FIRST_KEY),
                        straightLast = border.flag(STRAIGHT_LAST_KEY),
                        properties = border,
                    )
                },
                properties = properties,
            )
            GRAPH_TAG -> GeonextGraphDefinition(
                id = id,
                name = name,
                function = properties.text(FUNCTION_KEY).orEmpty(),
                properties = properties,
            )
            PARAMETER_CURVE_TAG -> GeonextParameterCurveDefinition(
                id = id,
                name = name,
                functionX = properties.text(FUNCTION_X_KEY).orEmpty(),
                functionY = properties.text(FUNCTION_Y_KEY).orEmpty(),
                minimum = properties.text(MIN_KEY).orEmpty(),
                maximum = properties.text(MAX_KEY).orEmpty(),
                properties = properties,
            )
            SLIDER_TAG -> GeonextSliderDefinition(
                id = id,
                name = name,
                x = GeonextProperties.jsParseFloat(
                    properties.text(X_KEY),
                ),
                y = GeonextProperties.jsParseFloat(
                    properties.text(Y_KEY),
                ),
                parentId = properties.text(PARENT_KEY).orEmpty(),
                position = GeonextProperties.jsParseFloat(
                    properties.text(POSITION_KEY),
                ),
                fixed = properties.flag(FIXED_KEY),
                onPolygon =
                    properties.text(ON_POLYGON_KEY)
                        ?.lowercase() == "true",
                properties = properties,
            )
            TRACE_CURVE_TAG -> GeonextTraceCurveDefinition(
                id = id,
                name = name,
                tracePointId = properties.text(TRACE_POINT_KEY).orEmpty(),
                traceSliderId =
                    properties.text(TRACE_SLIDER_KEY).orEmpty(),
                properties = properties,
            )
            GROUP_TAG -> GeonextGroupDefinition(
                id = id,
                name = name,
                memberIds = properties.lists[
                    GeonextProperties.MEMBERS_KEY
                ].orEmpty(),
                properties = properties,
            )
            TEXT_TAG -> GeonextTextDefinition(
                id = id,
                name = name,
                x = GeonextProperties.jsParseFloat(
                    properties.text(X_KEY),
                ),
                y = GeonextProperties.jsParseFloat(
                    properties.text(Y_KEY),
                ),
                content = properties.text(MP_STRING_KEY).orEmpty(),
                parentId = properties.text(PARENT_KEY)
                    ?.takeIf(String::isNotEmpty),
                digits = when (
                    val value = properties.values[AUTO_DIGITS_KEY]
                ) {
                    is GeonextPropertyValue.Text ->
                        GeonextProperties.jsParseInt(value.value) ?: 2
                    is GeonextPropertyValue.Number -> value.value.toInt()
                    else -> 2
                },
                fixed = properties.boolean(FIXED_KEY),
                visible = properties.boolean(VISIBLE_KEY),
                strokeColor = properties.text(COLOR_LABEL_KEY),
                properties = properties,
            )
            COMPOSITION_TAG -> GeonextCompositionDefinition(
                id = id,
                name = name,
                type = properties.text(TYPE_KEY).orEmpty(),
                inputIds = properties.lists[
                    GeonextProperties.INPUTS_KEY
                ].orEmpty(),
                outputs = properties.outputs.values.map { output ->
                    GeonextCompositionOutput(
                        id = output.text(ID_KEY).orEmpty(),
                        name = output.text(NAME_KEY).orEmpty(),
                        properties = output,
                    )
                },
                properties = properties,
            )
            else -> GeonextAngleDefinition(
                id = id,
                name = name,
                firstId = properties.text(FIRST_KEY).orEmpty(),
                middleId = properties.text(MIDDLE_KEY).orEmpty(),
                lastId = properties.text(LAST_KEY).orEmpty(),
                radius = GeonextProperties.jsParseFloat(
                    properties.text(RADIUS_KEY),
                ),
                properties = properties,
            )
        }
    }

    private fun GeonextElementProperties.toIntersectionOutput():
        GeonextIntersectionOutput =
        GeonextIntersectionOutput(
            id = text(ID_KEY).orEmpty(),
            name = text(NAME_KEY).orEmpty(),
            fixed = flag(FIXED_KEY),
            properties = this,
        )

    private fun createDefinition(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        val created = when (definition) {
            is GeonextPointDefinition ->
                createPoint(board, definition)
            is GeonextLineDefinition ->
                createLine(board, objects, definition)
            is GeonextCircleDefinition ->
                createCircle(board, objects, definition)
            is GeonextIntersectionDefinition -> {
                return when (
                    val result = createIntersection(
                        board,
                        objects,
                        definition,
                    )
                ) {
                    is GMResult.Ok -> GMResult.Ok(
                        CreatedGeonext(objects = result.value),
                    )
                    is GMResult.Err -> result
                }
            }
            is GeonextArcDefinition ->
                createArc(board, objects, definition)
            is GeonextAngleDefinition ->
                createAngle(board, objects, definition)
            is GeonextPolygonDefinition ->
                createPolygon(board, objects, definition)
            is GeonextGraphDefinition ->
                createGraph(board, definition)
            is GeonextParameterCurveDefinition ->
                createParameterCurve(board, definition)
            is GeonextSliderDefinition ->
                createSlider(board, objects, definition)
            is GeonextTraceCurveDefinition ->
                createTraceCurve(board, objects, definition)
            is GeonextGroupDefinition ->
                return createGroup(board, objects, definition)
            is GeonextTextDefinition ->
                createText(board, objects, definition)
            is GeonextCompositionDefinition ->
                return createComposition(board, objects, definition)
        }
        return when (created) {
            is GMResult.Ok -> GMResult.Ok(
                CreatedGeonext(
                    objects = mapOf(definition.id to created.value),
                ),
            )
            is GMResult.Err -> created
        }
    }

    private fun createPoint(
        board: Board,
        definition: GeonextPointDefinition,
    ): GMResult<GeometryElement, GeonextReaderError> =
        when (
            val result = Point.create(
                board = board,
                coordinates = doubleArrayOf(definition.x, definition.y),
                id = definition.id,
                name = definition.name,
                fixed = definition.fixed,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Point(result.error),
            )
        }

    private fun createLine(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextLineDefinition,
    ): GMResult<GeometryElement, GeonextReaderError> {
        val first = when (
            val result = resolvePoint(
                board = board,
                objects = objects,
                definitionId = definition.id,
                referenceId = definition.firstId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val last = when (
            val result = resolvePoint(
                board = board,
                objects = objects,
                definitionId = definition.id,
                referenceId = definition.lastId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Line.create(
                board = board,
                point1 = first,
                point2 = last,
                id = definition.id,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> {
                if (definition.arrow) {
                    result.value.configureArrow()
                } else {
                    result.value.configureVisibleRange(
                        straightFirst = definition.straightFirst,
                        straightLast = definition.straightLast,
                    )
                }
                result
            }
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Line(result.error),
            )
        }
    }

    private fun createCircle(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCircleDefinition,
    ): GMResult<GeometryElement, GeonextReaderError> {
        val center = when (
            val result = resolvePoint(
                board = board,
                objects = objects,
                definitionId = definition.id,
                referenceId = definition.centerId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val circle = when (val radius = definition.radius) {
            is GeonextCircleRadius.PointReference -> {
                val radiusPoint = when (
                    val result = resolvePoint(
                        board = board,
                        objects = objects,
                        definitionId = definition.id,
                        referenceId = radius.id,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                Circle.create(
                    board = board,
                    center = center,
                    point2 = radiusPoint,
                    id = definition.id,
                    name = definition.name,
                )
            }
            is GeonextCircleRadius.ValueExpression ->
                Circle.create(
                    board = board,
                    center = center,
                    radiusExpression = radius.source,
                    id = definition.id,
                    name = definition.name,
                )
        }
        return when (circle) {
            is GMResult.Ok -> circle
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Circle(circle.error),
            )
        }
    }

    /*
     * JSXGraph 1.13.3: src/reader/geonext.js ->
     * readNode intersection branch.
     */
    private fun createIntersection(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextIntersectionDefinition,
    ): GMResult<Map<String, GeometryElement>, GeonextReaderError> {
        val first = when (
            val result = resolveElement(
                board = board,
                objects = objects,
                definitionId = definition.id,
                referenceId = definition.firstId,
                expectedType = "intersection parent",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val last = when (
            val result = resolveElement(
                board = board,
                objects = objects,
                definitionId = definition.id,
                referenceId = definition.lastId,
                expectedType = "intersection parent",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val outputs =
            if (
                first.elementClass == Const.OBJECT_CLASS_LINE &&
                last.elementClass == Const.OBJECT_CLASS_LINE
            ) {
                listOf(0 to definition.firstOutput)
            } else {
                val secondOutput =
                    definition.lastOutput
                        ?: return GMResult.Ok(emptyMap())
                listOf(
                    0 to definition.firstOutput,
                    1 to secondOutput,
                )
            }
        val created = linkedMapOf<String, GeometryElement>()
        for ((index, output) in outputs) {
            val intersection = when (
                val result = IntersectionPoint.create(
                    board = board,
                    first = first,
                    second = last,
                    firstIndex = IntersectionIndexSource.Number(
                        index.toDouble(),
                    ),
                    id = output.id,
                    name = output.name,
                    fixed = output.fixed,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return creationFailure(
                        definition.id,
                        GeonextCreationError.Intersection(result.error),
                    )
                }
            }
            created[output.id] = intersection
        }
        return GMResult.Ok(created.toMap())
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> readNode arc branch.
    private fun createArc(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextArcDefinition,
    ): GMResult<GeometryElement, GeonextReaderError> {
        val center = when (
            val result = resolvePoint(
                board,
                objects,
                definition.id,
                definition.centerId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val radiusPoint = when (
            val result = resolvePoint(
                board,
                objects,
                definition.id,
                definition.radiusId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val anglePoint = when (
            val result = resolvePoint(
                board,
                objects,
                definition.id,
                definition.angleId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Arc.create(
                board = board,
                center = center,
                radiuspoint = radiusPoint,
                anglepoint = anglePoint,
                id = definition.id,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> {
                result.value.configureVisualArrows(
                    firstArrow = definition.firstArrow,
                    lastArrow = definition.lastArrow,
                )
                result
            }
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Arc(result.error),
            )
        }
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> readNode angle branch.
    private fun createAngle(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextAngleDefinition,
    ): GMResult<GeometryElement, GeonextReaderError> {
        val first = when (
            val result = resolvePoint(
                board,
                objects,
                definition.id,
                definition.firstId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val middle = when (
            val result = resolvePoint(
                board,
                objects,
                definition.id,
                definition.middleId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val last = when (
            val result = resolvePoint(
                board,
                objects,
                definition.id,
                definition.lastId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Sector.createAngle(
                board = board,
                first = first,
                vertex = middle,
                third = last,
                radius = AngleRadius.Fixed(definition.radius),
                id = definition.id,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Angle(result.error),
            )
        }
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> readNode polygon branch.
    private fun createPolygon(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextPolygonDefinition,
    ): GMResult<GeometryElement, GeonextReaderError> {
        val vertices = mutableListOf<Point>()
        for (vertexId in definition.vertexIds) {
            val vertex = when (
                val result = resolvePoint(
                    board = board,
                    objects = objects,
                    definitionId = definition.id,
                    referenceId = vertexId,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            vertices += vertex
        }
        return when (
            val result = Polygon.create(
                board = board,
                vertices = vertices,
                id = definition.id,
                name = definition.name,
                borderIds = definition.borders.map { it.id },
                borderNames = definition.borders.map { it.name },
            )
        ) {
            is GMResult.Ok -> {
                for ((index, border) in result.value.borders.withIndex()) {
                    definition.borders.getOrNull(index)?.let {
                        border.configureVisibleRange(
                            straightFirst = it.straightFirst,
                            straightLast = it.straightLast,
                        )
                    }
                }
                result
            }
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Polygon(result.error),
            )
        }
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> readNode graph branch.
    private fun createGraph(
        board: Board,
        definition: GeonextGraphDefinition,
    ): GMResult<GeometryElement, GeonextReaderError> =
        when (
            val result = Curve.createFunctionGraph(
                board = board,
                ySource = definition.function,
                minimumSource = board.defaultCurveMinimum.toString(),
                maximumSource = board.defaultCurveMaximum.toString(),
                id = definition.id,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Curve(result.error),
            )
        }

    /*
     * JSXGraph 1.13.3: src/reader/geonext.js ->
     * readNode parametercurve branch.
     */
    private fun createParameterCurve(
        board: Board,
        definition: GeonextParameterCurveDefinition,
    ): GMResult<GeometryElement, GeonextReaderError> =
        when (
            val result = Curve.createParametric(
                board = board,
                xSource = definition.functionX,
                ySource = definition.functionY,
                minimumSource = definition.minimum,
                maximumSource = definition.maximum,
                id = definition.id,
                name = definition.name,
                parameterName = "t",
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Curve(result.error),
            )
        }

    // JSXGraph 1.13.3: src/reader/geonext.js -> readNode slider branch.
    private fun createSlider(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextSliderDefinition,
    ): GMResult<GeometryElement, GeonextReaderError> {
        val parent = when (
            val result = resolveElement(
                board = board,
                objects = objects,
                definitionId = definition.id,
                referenceId = definition.parentId,
                expectedType = "glider parent",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Glider.create(
                board = board,
                coordinates = doubleArrayOf(definition.x, definition.y),
                slideObject = parent,
                id = definition.id,
                name = definition.name,
                fixed = definition.fixed,
            )
        ) {
            is GMResult.Ok -> {
                result.value.onPolygon = definition.onPolygon
                result
            }
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Glider(result.error),
            )
        }
    }

    /*
     * JSXGraph 1.13.3: src/reader/geonext.js ->
     * readNode tracecurve branch.
     */
    private fun createTraceCurve(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextTraceCurveDefinition,
    ): GMResult<GeometryElement, GeonextReaderError> {
        val slider = when (
            val result = resolveGlider(
                board = board,
                objects = objects,
                definitionId = definition.id,
                referenceId = definition.traceSliderId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val tracePoint = when (
            val result = resolvePoint(
                board = board,
                objects = objects,
                definitionId = definition.id,
                referenceId = definition.tracePointId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Curve.createTraceCurve(
                board = board,
                glider = slider,
                tracePoint = tracePoint,
                id = definition.id,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Curve(result.error),
            )
        }
    }

    /*
     * JSXGraph 1.13.3: src/reader/geonext.js -> readNode group branch.
     * Upstream omits the constructor attributes argument and throws after
     * registration. This translation preserves the intended member group,
     * keeps its direct-constructor empty parent list, and returns failures.
     */
    private fun createGroup(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextGroupDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        val members = mutableListOf<GeometryElement>()
        for (memberId in definition.memberIds) {
            val member = when (
                val result = resolveElement(
                    board = board,
                    objects = objects,
                    definitionId = definition.id,
                    referenceId = memberId,
                    expectedType = "group member",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            members += member
        }
        return when (
            val result = Group.create(
                board = board,
                parents = members,
                id = definition.id,
                name = definition.name,
                recordParents = false,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(
                CreatedGeonext(
                    groups = mapOf(definition.id to result.value),
                ),
            )
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Group(result.error),
            )
        }
    }

    // JSXGraph 1.13.3: src/reader/geonext.js -> readNode text branch.
    private fun createText(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextTextDefinition,
    ): GMResult<GeometryElement, GeonextReaderError> {
        val anchor = definition.parentId?.let { parentId ->
            val element = when (
                val result = resolveElement(
                    board = board,
                    objects = objects,
                    definitionId = definition.id,
                    referenceId = parentId,
                    expectedType = "text anchor",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (element !is Line) {
                return GMResult.Err(
                    GeonextReaderError.ReferenceTypeMismatch(
                        definitionId = definition.id,
                        referenceId = parentId,
                        expectedType = "line text anchor",
                        actualType = element.elType.ifEmpty { "element" },
                    ),
                )
            }
            element
        }
        return when (
            val result = Text.create(
                board = board,
                coordinates = doubleArrayOf(definition.x, definition.y),
                content = definition.content,
                id = definition.id,
                name = definition.name,
                digits = definition.digits,
                anchor = anchor,
                fixed = definition.fixed,
                visible = definition.visible,
                strokeColor = definition.strokeColor,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Text(result.error),
            )
        }
    }

    /*
     * JSXGraph 1.13.3: src/reader/geonext.js ->
     * readNode composition branch.
     */
    private fun createComposition(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> =
        when (definition.type) {
            ARROW_PARALLEL_TYPE ->
                createArrowParallel(board, objects, definition)
            BISECTOR_TYPE ->
                createBisector(board, objects, definition)
            CIRCUMCIRCLE_TYPE ->
                createCircumcircle(board, objects, definition)
            CIRCUMCENTER_TYPE ->
                createCircumcenter(board, objects, definition)
            MIDPOINT_TYPE ->
                createMidpoint(board, objects, definition)
            MIRROR_LINE_TYPE ->
                createMirrorLine(board, objects, definition)
            MIRROR_POINT_TYPE ->
                createMirrorPoint(board, objects, definition)
            NORMAL_TYPE ->
                createNormal(board, objects, definition)
            PARALLEL_TYPE ->
                createParallel(board, objects, definition)
            PARALLELOGRAM_POINT_TYPE ->
                createParallelogramPoint(board, objects, definition)
            PERPENDICULAR_TYPE ->
                createPerpendicular(board, objects, definition)
            PERPENDICULAR_POINT_TYPE ->
                createPerpendicularPoint(board, objects, definition)
            SECTOR_TYPE ->
                createSector(board, objects, definition)
            else -> GMResult.Err(
                GeonextReaderError.UnsupportedCompositionType(
                    definitionId = definition.id,
                    type = definition.type,
                ),
            )
        }

    private fun createMidpoint(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        val output = when (
            val result = compositionOutput(definition, 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val points = when (definition.inputIds.size) {
            1 -> {
                val line = when (
                    val result = resolveLine(
                        board,
                        objects,
                        definition.id,
                        definition.inputIds[0],
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                listOf(line.point1, line.point2)
            }
            2 -> {
                when (
                    val result = resolveCompositionPoints(
                        board,
                        objects,
                        definition,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }
            else -> {
                return invalidCompositionArity(
                    definition = definition,
                    expected = "1 line or 2 points",
                )
            }
        }
        return when (
            val result = MidpointPoint.create(
                board = board,
                point1 = points[0],
                point2 = points[1],
                id = output.id,
                name = output.name,
                fixed = output.fixed(default = false),
            )
        ) {
            is GMResult.Ok -> createdComposition(result.value)
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Midpoint(result.error),
            )
        }
    }

    private fun createCircumcenter(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        val output = when (
            val result = compositionOutput(definition, 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val points = when (
            val result = resolveCompositionPoints(
                board,
                objects,
                definition,
                expectedCount = 3,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = CircumcenterPoint.create(
                board = board,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                id = output.id,
                name = output.name,
                fixed = output.fixed(default = false),
            )
        ) {
            is GMResult.Ok -> createdComposition(result.value)
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Circumcenter(result.error),
            )
        }
    }

    private fun createMirrorLine(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        if (definition.inputIds.size != 2) {
            return invalidCompositionArity(definition, "2")
        }
        val output = when (
            val result = compositionOutput(definition, 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        /*
         * Preserve the upstream parent reversal. Some historical files place
         * [point, line] here and JSXGraph 1.13.3 consequently rejects them.
         */
        val source = when (
            val result = resolvePoint(
                board,
                objects,
                definition.id,
                definition.inputIds[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val line = when (
            val result = resolveLine(
                board,
                objects,
                definition.id,
                definition.inputIds[0],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = PointReflections.createReflection(
                board = board,
                source = source,
                line = line,
                id = output.id,
                name = output.name,
                fixed = output.fixed(default = true),
            )
        ) {
            is GMResult.Ok -> createdComposition(result.value)
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.PointReflection(result.error),
            )
        }
    }

    private fun createMirrorPoint(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        val points = when (
            val result = resolveCompositionPoints(
                board,
                objects,
                definition,
                expectedCount = 2,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val output = when (
            val result = compositionOutput(definition, 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = PointReflections.createMirrorPoint(
                board = board,
                source = points[0],
                mirror = points[1],
                id = output.id,
                name = output.name,
                fixed = output.fixed(default = true),
            )
        ) {
            is GMResult.Ok -> createdComposition(result.value)
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.PointReflection(result.error),
            )
        }
    }

    private fun createParallelogramPoint(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        val points = when (
            val result = resolveCompositionPoints(
                board,
                objects,
                definition,
                expectedCount = 3,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val output = when (
            val result = compositionOutput(definition, 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = ParallelPoint.create(
                board = board,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                id = output.id,
                name = output.name,
                fixed = output.fixed(default = false),
            )
        ) {
            is GMResult.Ok -> createdComposition(result.value)
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Parallel(result.error),
            )
        }
    }

    private fun createPerpendicularPoint(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        if (definition.inputIds.size != 2) {
            return invalidCompositionArity(definition, "2")
        }
        val point = when (
            val result = resolvePoint(
                board,
                objects,
                definition.id,
                definition.inputIds[0],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val line = when (
            val result = resolveLine(
                board,
                objects,
                definition.id,
                definition.inputIds[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val output = when (
            val result = compositionOutput(definition, 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = OrthogonalPoint.create(
                board = board,
                point = point,
                line = line,
                kind = OrthogonalPointKind.PERPENDICULAR_POINT,
                id = output.id,
                name = output.name,
                fixed = output.fixed(default = false),
            )
        ) {
            is GMResult.Ok -> createdComposition(result.value)
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Orthogonal(result.error),
            )
        }
    }

    private fun createNormal(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        if (definition.inputIds.size != 2) {
            return invalidCompositionArity(definition, "2")
        }
        val point = when (
            val result = resolvePoint(
                board,
                objects,
                definition.id,
                definition.inputIds[0],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val line = when (
            val result = resolveLine(
                board,
                objects,
                definition.id,
                definition.inputIds[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val output = when (
            val result = compositionOutput(definition, 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = PerpendicularSegmentLine.create(
                board = board,
                line = line,
                sourcePoint = point,
                id = output.id,
                name = output.name,
            )
        ) {
            is GMResult.Ok -> {
                result.value.configureVisibleRange(
                    straightFirst = output.straightFirst(),
                    straightLast = output.straightLast(),
                )
                createdComposition(result.value)
            }
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Orthogonal(result.error),
            )
        }
    }

    private fun createBisector(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        val points = when (
            val result = resolveCompositionPoints(
                board,
                objects,
                definition,
                expectedCount = 3,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val output = when (
            val result = compositionOutput(definition, 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = BisectorLine.create(
                board = board,
                point1 = points[0],
                vertex = points[1],
                point3 = points[2],
                id = output.id,
                name = output.name,
            )
        ) {
            is GMResult.Ok -> {
                result.value.configureVisibleRange(
                    straightFirst = false,
                    straightLast = output.straightLast(),
                )
                createdComposition(result.value)
            }
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.TriangleCenter(result.error),
            )
        }
    }

    private fun createArrowParallel(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        if (definition.inputIds.size != 2) {
            return invalidCompositionArity(definition, "2")
        }
        val lineOutput = when (
            val result = compositionOutput(definition, 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointOutput = when (
            val result = compositionOutput(definition, 1)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val throughPoint = when (
            val result = resolvePoint(
                board,
                objects,
                definition.id,
                definition.inputIds[0],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sourceLine = when (
            val result = resolveLine(
                board,
                objects,
                definition.id,
                definition.inputIds[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val parallelPoint = when (
            val result = ParallelPoint.create(
                board = board,
                sourceLine = sourceLine,
                throughPoint = throughPoint,
                id = pointOutput.id,
                name = pointOutput.name,
                fixed = pointOutput.fixed(default = false),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    definition.id,
                    GeonextCreationError.Parallel(result.error),
                )
            }
        }
        return when (
            val result = Line.createSegment(
                board = board,
                point1 = throughPoint,
                point2 = parallelPoint,
                id = lineOutput.id,
                name = lineOutput.name,
            )
        ) {
            is GMResult.Ok -> {
                val line = result.value.configureVisualArrows(
                    firstArrow = false,
                    lastArrow = true,
                )
                line.parallelPoint = parallelPoint
                createdComposition(parallelPoint, line)
            }
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Line(result.error),
            )
        }
    }

    private fun createParallel(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        if (definition.inputIds.size != 2) {
            return invalidCompositionArity(definition, "2")
        }
        val output = when (
            val result = compositionOutput(definition, 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val throughPoint = when (
            val result = resolvePoint(
                board,
                objects,
                definition.id,
                definition.inputIds[0],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sourceLine = when (
            val result = resolveLine(
                board,
                objects,
                definition.id,
                definition.inputIds[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val parallelPoint = when (
            val result = ParallelPoint.create(
                board = board,
                sourceLine = sourceLine,
                throughPoint = throughPoint,
                name = "",
                fixed = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    definition.id,
                    GeonextCreationError.Parallel(result.error),
                )
            }
        }
        return when (
            val result = Line.create(
                board = board,
                point1 = throughPoint,
                point2 = parallelPoint,
                id = output.id,
                name = output.name,
            )
        ) {
            is GMResult.Ok -> {
                val line = result.value.configureVisibleRange(
                    straightFirst = output.straightFirst(),
                    straightLast = output.straightLast(),
                )
                line.parallelPoint = parallelPoint
                createdComposition(line)
            }
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Line(result.error),
            )
        }
    }

    private fun createPerpendicular(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        if (definition.inputIds.size != 2) {
            return invalidCompositionArity(definition, "2")
        }
        val pointOutput = when (
            val result = compositionOutput(definition, 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val lineOutput = when (
            val result = compositionOutput(definition, 1)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point = when (
            val result = resolvePoint(
                board,
                objects,
                definition.id,
                definition.inputIds[0],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val line = when (
            val result = resolveLine(
                board,
                objects,
                definition.id,
                definition.inputIds[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = PerpendicularSegmentLine.create(
                board = board,
                line = line,
                sourcePoint = point,
                pointId = pointOutput.id,
                pointName = pointOutput.name,
                pointFixed = pointOutput.fixed(default = false),
                id = lineOutput.id,
                name = lineOutput.name,
            )
        ) {
            is GMResult.Ok -> {
                val perpendicular = result.value.configureVisibleRange(
                    straightFirst = lineOutput.straightFirst(),
                    straightLast = lineOutput.straightLast(),
                )
                createdComposition(result.value.point, perpendicular)
            }
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Orthogonal(result.error),
            )
        }
    }

    private fun createCircumcircle(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        when (val result = compositionOutput(definition, 0)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val circleOutput = when (
            val result = compositionOutput(definition, 1)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val points = when (
            val result = resolveCompositionPoints(
                board,
                objects,
                definition,
                expectedCount = 3,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        /*
         * The upstream reader stores output[0] under the unused key `point`;
         * createCircumcircle reads `center`, so that declared output is not
         * registered. Preserve the observed 1.13.3 behavior.
         */
        return when (
            val result = Circle.createCircumcircle(
                board = board,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                id = circleOutput.id,
                name = circleOutput.name,
            )
        ) {
            is GMResult.Ok -> createdComposition(result.value)
            is GMResult.Err -> creationFailure(
                definition.id,
                GeonextCreationError.Circle(result.error),
            )
        }
    }

    private fun createSector(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
    ): GMResult<CreatedGeonext, GeonextReaderError> {
        val outputs = mutableListOf<GeonextCompositionOutput>()
        for (index in 0 until 4) {
            when (val result = compositionOutput(definition, index)) {
                is GMResult.Ok -> outputs += result.value
                is GMResult.Err -> return result
            }
        }
        val points = when (
            val result = resolveCompositionPoints(
                board,
                objects,
                definition,
                expectedCount = 3,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sector = when (
            val result = Sector.create(
                board = board,
                center = points[0],
                radiuspoint = points[1],
                anglepoint = points[2],
                id = outputs[0].id,
                name = outputs[0].name,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    definition.id,
                    GeonextCreationError.Angle(result.error),
                )
            }
        }
        /*
         * JSXGraph 1.13.3 multiplies by `el.Radius` instead of invoking it.
         * The resulting GEONExT sector helper point is therefore NaN.
         */
        val radiusPoint = when (
            val result = Point.create(
                board = board,
                coordinates = doubleArrayOf(Double.NaN, Double.NaN),
                id = outputs[1].id,
                name = outputs[1].name,
                fixed = outputs[1].fixed(default = false),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    definition.id,
                    GeonextCreationError.Point(result.error),
                )
            }
        }
        val firstSegment = when (
            val result = Line.createSegment(
                board = board,
                point1 = points[0],
                point2 = points[1],
                id = outputs[2].id,
                name = outputs[2].name,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    definition.id,
                    GeonextCreationError.Line(result.error),
                )
            }
        }
        val lastSegment = when (
            val result = Line.createSegment(
                board = board,
                point1 = points[1],
                point2 = radiusPoint,
                id = outputs[3].id,
                name = outputs[3].name,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    definition.id,
                    GeonextCreationError.Line(result.error),
                )
            }
        }
        return createdComposition(
            sector,
            radiusPoint,
            firstSegment,
            lastSegment,
        )
    }

    private fun compositionOutput(
        definition: GeonextCompositionDefinition,
        index: Int,
    ): GMResult<GeonextCompositionOutput, GeonextReaderError> {
        val output = definition.outputs.getOrNull(index)
            ?: return GMResult.Err(
                GeonextReaderError.MissingCompositionOutput(
                    definitionId = definition.id,
                    type = definition.type,
                    outputIndex = index,
                ),
            )
        return GMResult.Ok(output)
    }

    private fun resolveCompositionPoints(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: GeonextCompositionDefinition,
        expectedCount: Int? = null,
    ): GMResult<List<Point>, GeonextReaderError> {
        if (
            expectedCount != null &&
            definition.inputIds.size != expectedCount
        ) {
            return invalidCompositionArity(
                definition = definition,
                expected = expectedCount.toString(),
            )
        }
        val points = mutableListOf<Point>()
        for (inputId in definition.inputIds) {
            when (
                val result = resolvePoint(
                    board,
                    objects,
                    definition.id,
                    inputId,
                )
            ) {
                is GMResult.Ok -> points += result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(points.toList())
    }

    private fun resolveLine(
        board: Board,
        objects: Map<String, GeometryElement>,
        definitionId: String,
        referenceId: String,
    ): GMResult<Line, GeonextReaderError> {
        val element = when (
            val result = resolveElement(
                board = board,
                objects = objects,
                definitionId = definitionId,
                referenceId = referenceId,
                expectedType = LINE_TAG,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return if (element is Line) {
            GMResult.Ok(element)
        } else {
            GMResult.Err(
                GeonextReaderError.ReferenceTypeMismatch(
                    definitionId = definitionId,
                    referenceId = referenceId,
                    expectedType = LINE_TAG,
                    actualType = element.elType.ifEmpty { "element" },
                ),
            )
        }
    }

    private fun invalidCompositionArity(
        definition: GeonextCompositionDefinition,
        expected: String,
    ): GMResult.Err<GeonextReaderError> =
        GMResult.Err(
            GeonextReaderError.InvalidCompositionArity(
                definitionId = definition.id,
                type = definition.type,
                expected = expected,
                actual = definition.inputIds.size,
            ),
        )

    private fun createdComposition(
        vararg elements: GeometryElement,
    ): GMResult<CreatedGeonext, GeonextReaderError> =
        GMResult.Ok(
            CreatedGeonext(
                objects = elements.associateByTo(linkedMapOf()) { it.id },
            ),
        )

    private fun GeonextCompositionOutput.fixed(default: Boolean): Boolean =
        when (val value = properties.values[FIXED_KEY]) {
            is GeonextPropertyValue.Flag -> value.value
            is GeonextPropertyValue.Text ->
                value.value.lowercase() == "true"
            else -> default
        }

    private fun GeonextCompositionOutput.straightFirst(): Boolean =
        properties.flag(STRAIGHT_FIRST_KEY)

    private fun GeonextCompositionOutput.straightLast(): Boolean =
        properties.flag(STRAIGHT_LAST_KEY)

    private fun resolveGlider(
        board: Board,
        objects: Map<String, GeometryElement>,
        definitionId: String,
        referenceId: String,
    ): GMResult<Glider, GeonextReaderError> {
        val element = when (
            val result = resolveElement(
                board = board,
                objects = objects,
                definitionId = definitionId,
                referenceId = referenceId,
                expectedType = SLIDER_TAG,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return if (element is Glider) {
            GMResult.Ok(element)
        } else {
            GMResult.Err(
                GeonextReaderError.ReferenceTypeMismatch(
                    definitionId = definitionId,
                    referenceId = referenceId,
                    expectedType = SLIDER_TAG,
                    actualType = element.elType.ifEmpty { "element" },
                ),
            )
        }
    }

    private fun resolvePoint(
        board: Board,
        objects: Map<String, GeometryElement>,
        definitionId: String,
        referenceId: String,
    ): GMResult<Point, GeonextReaderError> {
        val element = when (
            val result = resolveElement(
                board = board,
                objects = objects,
                definitionId = definitionId,
                referenceId = referenceId,
                expectedType = POINT_TAG,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return if (element is Point) {
            GMResult.Ok(element)
        } else {
            GMResult.Err(
                GeonextReaderError.ReferenceTypeMismatch(
                    definitionId = definitionId,
                    referenceId = referenceId,
                    expectedType = POINT_TAG,
                    actualType = element.elType.ifEmpty { "element" },
                ),
            )
        }
    }

    private fun resolveElement(
        board: Board,
        objects: Map<String, GeometryElement>,
        definitionId: String,
        referenceId: String,
        expectedType: String,
    ): GMResult<GeometryElement, GeonextReaderError> {
        val adjustedId =
            GeonextProperties.changeOriginId(board.id, referenceId)
        val element =
            objects[adjustedId]
                ?: objects[referenceId]
                ?: board.select(adjustedId)
                ?: board.select(referenceId)
                ?: return GMResult.Err(
                    GeonextReaderError.MissingReference(
                        definitionId = definitionId,
                        referenceId = referenceId,
                        expectedType = expectedType,
                    ),
                )
        return GMResult.Ok(element)
    }

    private fun <T> creationFailure(
        definitionId: String,
        cause: GeonextCreationError,
    ): GMResult<T, GeonextReaderError> =
        GMResult.Err(
            GeonextReaderError.BoardCreationFailed(
                definitionId = definitionId,
                cause = cause,
            ),
        )

    private fun rollbackBoardRead(
        board: Board,
        existingIds: Set<String>,
        existingNames: Map<String, GeometryElement>,
        existingGroups: Map<String, Group>,
        existingGroupMemberships: Map<CoordsElement, List<String>>,
    ) {
        val createdGroups = board.groups.filter { (id, group) ->
            existingGroups[id] !== group
        }
        for (group in createdGroups.values.toList().asReversed()) {
            group.ungroup()
        }
        board.groups.clear()
        board.groups.putAll(existingGroups)
        val created = board.objects
            .filterKeys { it !in existingIds }
            .values
            .toList()
            .asReversed()
        board.removeObjects(created)
        board.elementsByName.clear()
        board.elementsByName.putAll(existingNames)
        for ((element, memberships) in existingGroupMemberships) {
            element.groups.clear()
            element.groups.addAll(memberships)
        }
        board.unsuspendUpdate()
    }

    private fun validateLimits(
        limits: GeonextReaderLimits,
    ): GeonextReaderError.InvalidLimits? {
        val invalid = when {
            limits.maxElements < 0 ->
                "maxElements" to limits.maxElements
            limits.maxProperties < 0 ->
                "maxProperties" to limits.maxProperties
            else -> null
        }
        return invalid?.let { (name, value) ->
            GeonextReaderError.InvalidLimits(name, value)
        }
    }

    private fun GeonextElementProperties.text(key: String): String? =
        (values[key] as? GeonextPropertyValue.Text)?.value

    private fun GeonextElementProperties.flag(key: String): Boolean =
        (values[key] as? GeonextPropertyValue.Flag)?.value ?: false

    private fun GeonextElementProperties.boolean(key: String): Boolean =
        when (val value = values[key]) {
            is GeonextPropertyValue.Flag -> value.value
            is GeonextPropertyValue.Text ->
                value.value.lowercase() == "true"
            else -> false
        }

    private fun textByTagName(
        node: XmlElement,
        tag: String,
    ): String? =
        node.getElementsByTagName(tag).firstOrNull()?.firstChild?.data

    private fun XmlElement.childElements(): List<XmlElement> =
        childNodes.filterIsInstance<XmlElement>()

    private companion object {
        const val ELEMENTS_TAG = "elements"
        const val POINT_TAG = "point"
        const val LINE_TAG = "line"
        const val CIRCLE_TAG = "circle"
        const val ARROW_TAG = "arrow"
        const val INTERSECTION_TAG = "intersection"
        const val ARC_TAG = "arc"
        const val ANGLE_TAG = "angle"
        const val POLYGON_TAG = "polygon"
        const val GRAPH_TAG = "graph"
        const val PARAMETER_CURVE_TAG = "parametercurve"
        const val SLIDER_TAG = "slider"
        const val TRACE_CURVE_TAG = "tracecurve"
        const val GROUP_TAG = "group"
        const val TEXT_TAG = "text"
        const val COMPOSITION_TAG = "composition"
        const val DATA_TAG = "data"
        const val ID_TAG = "id"
        const val RADIUS_TAG = "radius"

        const val ID_KEY = "id"
        const val NAME_KEY = "name"
        const val X_KEY = "x"
        const val Y_KEY = "y"
        const val FIXED_KEY = "fixed"
        const val FIRST_KEY = "first"
        const val LAST_KEY = "last"
        const val STRAIGHT_FIRST_KEY = "straightFirst"
        const val STRAIGHT_LAST_KEY = "straightLast"
        const val CENTER_KEY = "center"
        const val MIDPOINT_KEY = "midpoint"
        const val RADIUS_KEY = "radius"
        const val ANGLE_KEY = "angle"
        const val MIDDLE_KEY = "middle"
        const val FIRST_ARROW_KEY = "firstArrow"
        const val LAST_ARROW_KEY = "lastArrow"
        const val FUNCTION_KEY = "function"
        const val FUNCTION_X_KEY = "functionx"
        const val FUNCTION_Y_KEY = "functiony"
        const val MIN_KEY = "min"
        const val MAX_KEY = "max"
        const val PARENT_KEY = "parent"
        const val POSITION_KEY = "position"
        const val ON_POLYGON_KEY = "onpolygon"
        const val TRACE_POINT_KEY = "tracepoint"
        const val TRACE_SLIDER_KEY = "traceslider"
        const val MP_STRING_KEY = "mpStr"
        const val AUTO_DIGITS_KEY = "autodigits"
        const val VISIBLE_KEY = "visible"
        const val COLOR_LABEL_KEY = "colorLabel"
        const val OLD_VERSION_MARKER = "oldVersion"
        const val TYPE_KEY = "type"

        const val ARROW_PARALLEL_TYPE = "210070"
        const val BISECTOR_TYPE = "210080"
        const val CIRCUMCIRCLE_TYPE = "210090"
        const val CIRCUMCENTER_TYPE = "210100"
        const val MIDPOINT_TYPE = "210110"
        const val MIRROR_LINE_TYPE = "210120"
        const val MIRROR_POINT_TYPE = "210125"
        const val NORMAL_TYPE = "210130"
        const val PARALLEL_TYPE = "210140"
        const val PARALLELOGRAM_POINT_TYPE = "210150"
        const val PERPENDICULAR_TYPE = "210160"
        const val PERPENDICULAR_POINT_TYPE = "210170"
        const val SECTOR_TYPE = "210190"

        val SUPPORTED_ELEMENT_TYPES = setOf(
            POINT_TAG,
            LINE_TAG,
            CIRCLE_TAG,
            ARROW_TAG,
            INTERSECTION_TAG,
            ARC_TAG,
            ANGLE_TAG,
            POLYGON_TAG,
            GRAPH_TAG,
            PARAMETER_CURVE_TAG,
            SLIDER_TAG,
            TRACE_CURVE_TAG,
            GROUP_TAG,
            TEXT_TAG,
            COMPOSITION_TAG,
        )

        val SUPPORTED_COMPOSITION_TYPES = setOf(
            ARROW_PARALLEL_TYPE,
            BISECTOR_TYPE,
            CIRCUMCIRCLE_TYPE,
            CIRCUMCENTER_TYPE,
            MIDPOINT_TYPE,
            MIRROR_LINE_TYPE,
            MIRROR_POINT_TYPE,
            NORMAL_TYPE,
            PARALLEL_TYPE,
            PARALLELOGRAM_POINT_TYPE,
            PERPENDICULAR_TYPE,
            PERPENDICULAR_POINT_TYPE,
            SECTOR_TYPE,
        )
    }
}

internal object GeonextReaderFactory :
    JsxGraphReaderFactory<Board> {
    override fun create(
        board: Board,
        source: String,
    ): GMResult<JsxGraphReader, ReaderError> =
        GMResult.Ok(
            JsxGraphReader {
                when (
                    val result = GeonextReader(source).read(
                        board = board,
                        failOnUnsupportedElements = true,
                    )
                ) {
                    is GMResult.Ok -> GMResult.Ok(Unit)
                    is GMResult.Err -> {
                        GMResult.Err(
                            ReaderError.DomainFailure(result.error),
                        )
                    }
                }
            },
        )
}

internal fun ReaderRegistry<Board>.registerGeonextReader() {
    registerReader(
        reader = GeonextReaderFactory,
        extensions = listOf("gxt", "geonext"),
    )
}
