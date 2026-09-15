/*
 * Kotlin translation support for JSXGraph.
 * Upstream: src/base/board.js -> create,
 * src/jxg.js -> registerElement,
 * src/base/element.js -> visual properties
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.parser.NativeJessieCodeCreators
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

data class JsxGraphEngineLimits(
    val maxSourceLength: Int = 1_000_000,
    val maxJsonDepth: Int = 64,
    val maxJsonValues: Int = 100_000,
    val maxObjects: Int = 10_000,
    val maxCurvePoints: Int = 10_000,
    val maxPolygonVertices: Int = 10_000,
    val maxTextLength: Int = 100_000,
)

sealed interface JsxGraphDocumentError {
    val message: String

    data class InvalidLimits(
        override val message: String,
    ) : JsxGraphDocumentError

    data class SourceLengthExceeded(
        val limit: Int,
        val actual: Int,
    ) : JsxGraphDocumentError {
        override val message: String =
            "Source length $actual exceeds limit $limit"
    }

    data class JsonDepthExceeded(
        val limit: Int,
    ) : JsxGraphDocumentError {
        override val message: String =
            "JSON nesting exceeds limit $limit"
    }

    data class JsonValueLimitExceeded(
        val limit: Int,
    ) : JsxGraphDocumentError {
        override val message: String =
            "JSON value count exceeds limit $limit"
    }

    data class InvalidJson(
        override val message: String,
    ) : JsxGraphDocumentError

    data class InvalidField(
        val path: String,
        val expected: String,
    ) : JsxGraphDocumentError {
        override val message: String =
            "$path must be $expected"
    }

    data class UnsupportedSchemaVersion(
        val version: Int,
    ) : JsxGraphDocumentError {
        override val message: String =
            "Unsupported schemaVersion: $version"
    }

    data class UnsupportedField(
        val path: String,
    ) : JsxGraphDocumentError {
        override val message: String =
            "Unsupported field: $path"
    }

    data class ObjectLimitExceeded(
        val limit: Int,
        val actual: Int,
    ) : JsxGraphDocumentError {
        override val message: String =
            "Object count $actual exceeds limit $limit"
    }

    data class CurvePointLimitExceeded(
        val objectIndex: Int,
        val id: String,
        val limit: Int,
        val actual: Int,
    ) : JsxGraphDocumentError {
        override val message: String =
            "objects[$objectIndex] '$id' curve point count $actual exceeds limit $limit"
    }

    data class PolygonVertexLimitExceeded(
        val objectIndex: Int,
        val id: String,
        val limit: Int,
        val actual: Int,
    ) : JsxGraphDocumentError {
        override val message: String =
            "objects[$objectIndex] '$id' polygon vertex count $actual exceeds limit $limit"
    }

    data class TextLengthLimitExceeded(
        val objectIndex: Int,
        val id: String,
        val limit: Int,
        val actual: Int,
    ) : JsxGraphDocumentError {
        override val message: String =
            "objects[$objectIndex] '$id' text length $actual exceeds limit $limit"
    }

    data class DuplicateObjectId(
        val id: String,
    ) : JsxGraphDocumentError {
        override val message: String =
            "Duplicate object id: $id"
    }

    data class UnsupportedElementType(
        val objectIndex: Int,
        val id: String,
        val type: String,
    ) : JsxGraphDocumentError {
        override val message: String =
            "objects[$objectIndex] '$id' uses unsupported type '$type'"
    }

    data class ElementCreation(
        val objectIndex: Int,
        val id: String,
        val type: String,
        val reason: String,
    ) : JsxGraphDocumentError {
        override val message: String =
            "Could not create objects[$objectIndex] '$id' ($type): $reason"
    }

    data class InvalidAttribute(
        val objectIndex: Int,
        val id: String,
        val attribute: String,
        val expected: String,
    ) : JsxGraphDocumentError {
        override val message: String =
            "objects[$objectIndex] '$id' attribute '$attribute' must be $expected"
    }

    data class UnsupportedAttribute(
        val objectIndex: Int,
        val id: String,
        val attribute: String,
    ) : JsxGraphDocumentError {
        override val message: String =
            "objects[$objectIndex] '$id' uses unsupported attribute '$attribute'"
    }

    data class UnsupportedAttributeValue(
        val objectIndex: Int,
        val id: String,
        val attribute: String,
        val value: String,
    ) : JsxGraphDocumentError {
        override val message: String =
            "objects[$objectIndex] '$id' uses unsupported $attribute value '$value'"
    }

    data class NonFiniteGeometry(
        val objectIndex: Int,
        val id: String,
    ) : JsxGraphDocumentError {
        override val message: String =
            "objects[$objectIndex] '$id' produced non-finite geometry"
    }
}

/**
 * Parses the construction document, creates translated Board elements through
 * the native creator registry, and snapshots those elements into a portable
 * render scene.
 */
object JsxGraphEngine {
    fun parse(
        source: String,
        limits: JsxGraphEngineLimits = JsxGraphEngineLimits(),
    ): GMResult<JsxGraphScene, JsxGraphDocumentError> {
        validateLimits(limits)?.let { error ->
            return GMResult.Err(error)
        }
        if (source.length > limits.maxSourceLength) {
            return GMResult.Err(
                JsxGraphDocumentError.SourceLengthExceeded(
                    limit = limits.maxSourceLength,
                    actual = source.length,
                ),
            )
        }
        if (exceedsJsonDepth(source, limits.maxJsonDepth)) {
            return GMResult.Err(
                JsxGraphDocumentError.JsonDepthExceeded(
                    limit = limits.maxJsonDepth,
                ),
            )
        }

        val root = try {
            Json.parseToJsonElement(source)
        } catch (failure: Exception) {
            return GMResult.Err(
                JsxGraphDocumentError.InvalidJson(
                    failure.message ?: "Invalid JSON",
                ),
            )
        }
        if (exceedsJsonValueLimit(root, limits.maxJsonValues)) {
            return GMResult.Err(
                JsxGraphDocumentError.JsonValueLimitExceeded(
                    limit = limits.maxJsonValues,
                ),
            )
        }
        val document = when (val result = parseDocument(root, limits)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return createScene(document)
    }

    private fun createScene(
        document: ParsedDocument,
    ): GMResult<JsxGraphScene, JsxGraphDocumentError> {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "jxgBoard",
        )
        val created = mutableListOf<CreatedSourceElement>()

        for (sourceObject in document.objects) {
            val creator = NativeJessieCodeCreators.creator(sourceObject.type)
                ?: return GMResult.Err(
                    JsxGraphDocumentError.UnsupportedElementType(
                        objectIndex = sourceObject.index,
                        id = sourceObject.id,
                        type = sourceObject.type,
                    ),
                )
            val parents = when (
                val result = runtimeArray(sourceObject.parents)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val attributes = when (
                val result = runtimeAttributes(sourceObject)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val value = when (
                val result = creator.create(
                    board = board,
                    parents = parents,
                    attributes = attributes,
                    location = SOURCE_LOCATION,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    JsxGraphDocumentError.ElementCreation(
                        objectIndex = sourceObject.index,
                        id = sourceObject.id,
                        type = sourceObject.type,
                        reason = result.error.toString(),
                    ),
                )
            }
            val element = (
                value as? JessieCodeRuntimeValue.ElementReference
            )?.element ?: return GMResult.Err(
                JsxGraphDocumentError.ElementCreation(
                    objectIndex = sourceObject.index,
                    id = sourceObject.id,
                    type = sourceObject.type,
                    reason = "creator did not return a geometry element",
                ),
            )
            created += CreatedSourceElement(sourceObject, element)
        }

        board.fullUpdate()
        val sceneElements = mutableListOf<JsxGraphSceneElement>()
        for (sourceElement in created) {
            when (val result = sceneElement(sourceElement)) {
                is GMResult.Ok -> sceneElements += result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(
            JsxGraphScene(
                boundingBox = document.boundingBox,
                axis = document.axis,
                grid = document.grid,
                keepAspectRatio = document.keepAspectRatio,
                elements = sceneElements,
            ),
        )
    }

    private fun sceneElement(
        sourceElement: CreatedSourceElement,
    ): GMResult<JsxGraphSceneElement, JsxGraphDocumentError> {
        val source = sourceElement.source
        val element = sourceElement.element
        val attributes = AttributeReader(source)
        when (val result = attributes.validateSupported(element)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val style = when (val result = attributes.style(element)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            when (
                val result = attributes.boolean(
                    name = "withlabel",
                    default = element is Point,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            } &&
            element.name.isNotEmpty()
        ) {
            return GMResult.Err(
                attributes.unsupportedValue(
                    attribute = "withLabel",
                    value = "true with non-empty name",
                ),
            )
        }

        val sceneElement = when (element) {
            is Point -> {
                val coordinates = point(element)
                    ?: return GMResult.Err(attributes.nonFiniteGeometry())
                val size = when (
                    val result = attributes.number(
                        name = "size",
                        default = 3.0,
                        minimum = 0.0,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val face = when (
                    val result = attributes.string(
                        name = "face",
                        default = "o",
                    )
                ) {
                    is GMResult.Ok -> normalizePointFace(result.value)
                    is GMResult.Err -> return result
                }
                if (face != "o") {
                    return GMResult.Err(
                        attributes.unsupportedValue(
                            attribute = "face",
                            value = face,
                        ),
                    )
                }
                JsxGraphSceneElement.Point(
                    id = element.id,
                    name = element.name,
                    style = style,
                    coordinates = coordinates,
                    size = size,
                    face = face,
                )
            }

            is Line -> {
                val point1 = point(element.point1)
                    ?: return GMResult.Err(attributes.nonFiniteGeometry())
                val point2 = point(element.point2)
                    ?: return GMResult.Err(attributes.nonFiniteGeometry())
                if (point1 == point2) {
                    return GMResult.Err(attributes.nonFiniteGeometry())
                }
                val straightFirst = when (
                    val result = attributes.boolean(
                        name = "straightfirst",
                        default = true,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val straightLast = when (
                    val result = attributes.boolean(
                        name = "straightlast",
                        default = true,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                JsxGraphSceneElement.Line(
                    id = element.id,
                    name = element.name,
                    style = style,
                    point1 = point1,
                    point2 = point2,
                    straightFirst = straightFirst,
                    straightLast = straightLast,
                )
            }

            is Circle -> {
                val center = point(element.center)
                    ?: return GMResult.Err(attributes.nonFiniteGeometry())
                val radius = element.Radius()
                if (!radius.isFinite() || radius < 0.0) {
                    return GMResult.Err(attributes.nonFiniteGeometry())
                }
                JsxGraphSceneElement.Circle(
                    id = element.id,
                    name = element.name,
                    style = style,
                    center = center,
                    radius = radius,
                )
            }

            is Arc -> {
                val useDirection = when (
                    val result = attributes.boolean(
                        name = "usedirection",
                        default = false,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (useDirection) {
                    return GMResult.Err(
                        attributes.unsupportedValue(
                            attribute = "useDirection",
                            value = "true",
                        ),
                    )
                }
                when (
                    val result = curveSceneElement(
                        element = element,
                        points = element.points,
                        bezierDegree = element.bezierDegree,
                        style = style,
                        attributes = attributes,
                        allowFill = false,
                        allowPathBreaks = false,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }

            is Sector -> {
                val useDirection = when (
                    val result = attributes.boolean(
                        name = "usedirection",
                        default = false,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (useDirection) {
                    return GMResult.Err(
                        attributes.unsupportedValue(
                            attribute = "useDirection",
                            value = "true",
                        ),
                    )
                }
                if (element.isAngle) {
                    val displayType = when (
                        val result = attributes.string(
                            name = "type",
                            default = "sector",
                        )
                    ) {
                        is GMResult.Ok -> result.value.lowercase()
                        is GMResult.Err -> return result
                    }
                    if (displayType != "sector") {
                        return GMResult.Err(
                            attributes.unsupportedValue(
                                attribute = "type",
                                value = displayType,
                            ),
                        )
                    }
                    val orthoType = when (
                        val result = attributes.string(
                            name = "orthotype",
                            default = "square",
                        )
                    ) {
                        is GMResult.Ok -> result.value.lowercase()
                        is GMResult.Err -> return result
                    }
                    val orthoSensitivity = when (
                        val result = attributes.number(
                            name = "orthosensitivity",
                            default = 1.0,
                            minimum = 0.0,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    val degrees = Geometry.trueAngle(
                        element.point2.Coords(),
                        element.point1.Coords(),
                        element.point3.Coords(),
                    )
                    if (
                        kotlin.math.abs(degrees - 90.0) <
                        orthoSensitivity +
                        com.swithun.jsxgraph.core.math.Mat.eps &&
                        orthoType != "sector"
                    ) {
                        return GMResult.Err(
                            attributes.unsupportedValue(
                                attribute = "orthoType",
                                value = orthoType,
                            ),
                        )
                    }
                }
                when (
                    val result = curveSceneElement(
                        element = element,
                        points = element.points,
                        bezierDegree = element.bezierDegree,
                        style = style,
                        attributes = attributes,
                        allowFill = true,
                        allowPathBreaks = false,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }

            is Curve -> {
                element.evaluationError?.let { error ->
                    return GMResult.Err(
                        JsxGraphDocumentError.ElementCreation(
                            objectIndex = source.index,
                            id = source.id,
                            type = source.type,
                            reason = error.toString(),
                        ),
                    )
                }
                when (
                    val result = curveSceneElement(
                        element = element,
                        points = element.points,
                        bezierDegree = element.bezierDegree,
                        style = style,
                        attributes = attributes,
                        allowFill = false,
                        allowPathBreaks = true,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }

            is Polygon -> {
                val withLines = when (
                    val result = attributes.boolean(
                        name = "withlines",
                        default = true,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val vertexCount = (element.vertices.size - 1).coerceAtLeast(0)
                val vertices = element.vertices
                    .take(vertexCount)
                    .mapNotNull(::point)
                if (vertices.size != vertexCount) {
                    return GMResult.Err(attributes.nonFiniteGeometry())
                }
                val implicitVertices = element.ownedVertices.map { vertex ->
                    val coordinates = point(vertex)
                        ?: return GMResult.Err(
                            attributes.nonFiniteGeometry(),
                        )
                    JsxGraphSceneElement.Point(
                        id = vertex.id,
                        name = vertex.name,
                        style = JsxGraphElementStyle(
                            visible = true,
                            strokeColor = DEFAULT_POINT_COLOR,
                            fillColor = DEFAULT_POINT_COLOR,
                            strokeWidth = 2.0,
                            strokeOpacity = 1.0,
                            fillOpacity = 1.0,
                        ),
                        coordinates = coordinates,
                        size = 3.0,
                        face = "o",
                    )
                }
                JsxGraphSceneElement.Polygon(
                    id = element.id,
                    name = element.name,
                    style = style,
                    vertices = vertices,
                    implicitVertices = implicitVertices,
                    borderStyle = JsxGraphElementStyle(
                        visible = true,
                        strokeColor = DEFAULT_STROKE_COLOR,
                        fillColor = JsxGraphColor.Transparent,
                        strokeWidth = 1.0,
                        strokeOpacity = 1.0,
                        fillOpacity = 1.0,
                    ),
                    withLines = withLines,
                )
            }

            is Text -> {
                element.contentEvaluationError?.let { error ->
                    return GMResult.Err(
                        JsxGraphDocumentError.ElementCreation(
                            objectIndex = source.index,
                            id = source.id,
                            type = source.type,
                            reason = error.toString(),
                        ),
                    )
                }
                val x = element.X()
                .takeIf(Double::isFinite)
                ?: return GMResult.Err(attributes.nonFiniteGeometry())
                val y = element.Y()
                    .takeIf(Double::isFinite)
                    ?: return GMResult.Err(attributes.nonFiniteGeometry())
                val fontSize = when (
                    val result = attributes.number(
                        name = "fontsize",
                        default = 12.0,
                        minimum = 0.0,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val fontUnit = when (
                    val result = attributes.string(
                        name = "fontunit",
                        default = "px",
                    )
                ) {
                    is GMResult.Ok -> result.value.lowercase()
                    is GMResult.Err -> return result
                }
                if (fontUnit != "px") {
                    return GMResult.Err(
                        attributes.unsupportedValue(
                            attribute = "fontUnit",
                            value = fontUnit,
                        ),
                    )
                }
                val anchorX = when (
                    val result = attributes.string(
                        name = "anchorx",
                        default = "left",
                    )
                ) {
                    is GMResult.Ok -> result.value.lowercase()
                    is GMResult.Err -> return result
                }
                if (anchorX !in TEXT_ANCHOR_X_VALUES) {
                    return GMResult.Err(
                        attributes.unsupportedValue(
                            attribute = "anchorX",
                            value = anchorX,
                        ),
                    )
                }
                val anchorY = when (
                    val result = attributes.string(
                        name = "anchory",
                        default = "middle",
                    )
                ) {
                    is GMResult.Ok -> result.value.lowercase()
                    is GMResult.Err -> return result
                }
                if (anchorY !in TEXT_ANCHOR_Y_VALUES) {
                    return GMResult.Err(
                        attributes.unsupportedValue(
                            attribute = "anchorY",
                            value = anchorY,
                        ),
                    )
                }
                val display = when (
                    val result = attributes.string(
                        name = "display",
                        default = "html",
                    )
                ) {
                    is GMResult.Ok -> result.value.lowercase()
                    is GMResult.Err -> return result
                }
                if (display !in TEXT_DISPLAY_VALUES) {
                    return GMResult.Err(
                        attributes.unsupportedValue(
                            attribute = "display",
                            value = display,
                        ),
                    )
                }
                for (name in TEXT_DISABLED_BOOLEAN_ATTRIBUTES) {
                    when (
                        val result = attributes.boolean(
                            name = name,
                            default = false,
                        )
                    ) {
                        is GMResult.Ok -> if (result.value) {
                            return GMResult.Err(
                                attributes.unsupportedValue(
                                    attribute = name,
                                    value = "true",
                                ),
                            )
                        }
                        is GMResult.Err -> return result
                    }
                }
                val rotate = when (
                    val result = attributes.number(
                        name = "rotate",
                        default = 0.0,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (rotate != 0.0) {
                    return GMResult.Err(
                        attributes.unsupportedValue(
                            attribute = "rotate",
                            value = rotate.toString(),
                        ),
                    )
                }
                JsxGraphSceneElement.Text(
                    id = element.id,
                    name = element.name,
                    style = style,
                    coordinates = JsxGraphPoint2D(x, y),
                    content = element.plaintext,
                    fontSize = fontSize,
                    anchorX = anchorX,
                    anchorY = anchorY,
                )
            }

            else -> return GMResult.Err(
                JsxGraphDocumentError.UnsupportedElementType(
                    objectIndex = source.index,
                    id = source.id,
                    type = source.type,
                ),
            )
        }
        return GMResult.Ok(sceneElement)
    }

    private fun curveSceneElement(
        element: GeometryElement,
        points: List<com.swithun.jsxgraph.core.base.Coords>,
        bezierDegree: Int,
        style: JsxGraphElementStyle,
        attributes: AttributeReader,
        allowFill: Boolean,
        allowPathBreaks: Boolean,
    ): GMResult<JsxGraphSceneElement.Curve, JsxGraphDocumentError> {
        val lineCap = when (
            val result = attributes.string(
                name = "linecap",
                default = "round",
            )
        ) {
            is GMResult.Ok -> result.value.lowercase()
            is GMResult.Err -> return result
        }
        if (lineCap != "round") {
            return GMResult.Err(
                attributes.unsupportedValue(
                    attribute = "lineCap",
                    value = lineCap,
                ),
            )
        }
        if (
            !allowFill &&
            style.fillColor.alpha > 0 &&
            style.fillOpacity > 0.0
        ) {
            return GMResult.Err(
                attributes.unsupportedValue(
                    attribute = "fillColor",
                    value = "non-transparent",
                ),
            )
        }
        val scenePoints = points.map { coordinates ->
            val x = coordinates.usrCoords[1]
            val y = coordinates.usrCoords[2]
            if (x.isFinite() && y.isFinite()) {
                JsxGraphPoint2D(x, y)
            } else {
                null
            }
        }
        if (!allowPathBreaks && scenePoints.any { it == null }) {
            return GMResult.Err(attributes.nonFiniteGeometry())
        }
        if (
            bezierDegree !in setOf(1, 3) ||
            (
                bezierDegree == 3 &&
                    scenePoints.isNotEmpty() &&
                    (scenePoints.size - 1) % 3 != 0
                )
        ) {
            return GMResult.Err(attributes.nonFiniteGeometry())
        }
        return GMResult.Ok(
            JsxGraphSceneElement.Curve(
                id = element.id,
                name = element.name,
                style = style,
                points = scenePoints,
                bezierDegree = bezierDegree,
                lineCap = lineCap,
            ),
        )
    }

    private fun parseDocument(
        root: JsonElement,
        limits: JsxGraphEngineLimits,
    ): GMResult<ParsedDocument, JsxGraphDocumentError> {
        val rootObject = root as? JsonObject
            ?: return invalidField("$", "a JSON object")
        val allowedFields = setOf(
            "schemaVersion",
            "boundingBox",
            "axis",
            "grid",
            "keepAspectRatio",
            "objects",
        )
        rootObject.keys.firstOrNull { it !in allowedFields }?.let { field ->
            return GMResult.Err(
                JsxGraphDocumentError.UnsupportedField(field),
            )
        }
        val versionElement = rootObject["schemaVersion"]
        if (versionElement != null) {
            val version = (versionElement as? JsonPrimitive)?.intOrNull
                ?: return invalidField("schemaVersion", "the integer 1")
            if (version != 1) {
                return GMResult.Err(
                    JsxGraphDocumentError.UnsupportedSchemaVersion(version),
                )
            }
        }
        val boundingBox = when (
            val result = parseBoundingBox(rootObject["boundingBox"])
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val axis = when (
            val result = booleanField(rootObject, "axis", default = false)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val grid = when (
            val result = booleanField(rootObject, "grid", default = false)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val keepAspectRatio = when (
            val result = booleanField(
                rootObject,
                "keepAspectRatio",
                default = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val objectArray = rootObject["objects"] as? JsonArray
            ?: return invalidField("objects", "an array")
        if (objectArray.size > limits.maxObjects) {
            return GMResult.Err(
                JsxGraphDocumentError.ObjectLimitExceeded(
                    limit = limits.maxObjects,
                    actual = objectArray.size,
                ),
            )
        }

        val objects = mutableListOf<ParsedObject>()
        val ids = mutableSetOf<String>()
        for ((index, element) in objectArray.withIndex()) {
            val sourceObject = when (
                val result = parseObject(index, element)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (!ids.add(sourceObject.id)) {
                return GMResult.Err(
                    JsxGraphDocumentError.DuplicateObjectId(
                        sourceObject.id,
                    ),
                )
            }
            when (
                val result = validateCurvePointLimit(
                    sourceObject,
                    limits.maxCurvePoints,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (
                val result = validatePolygonVertexLimit(
                    sourceObject,
                    limits.maxPolygonVertices,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (
                val result = validateTextLengthLimit(
                    sourceObject,
                    limits.maxTextLength,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            objects += sourceObject
        }
        return GMResult.Ok(
            ParsedDocument(
                boundingBox = boundingBox,
                axis = axis,
                grid = grid,
                keepAspectRatio = keepAspectRatio,
                objects = objects,
            ),
        )
    }

    private fun parseBoundingBox(
        element: JsonElement?,
    ): GMResult<JsxGraphBoundingBox, JsxGraphDocumentError> {
        val values = element as? JsonArray
            ?: return invalidField("boundingBox", "an array of four finite numbers")
        if (values.size != 4) {
            return invalidField("boundingBox", "an array of four finite numbers")
        }
        val numbers = mutableListOf<Double>()
        for (value in values) {
            val number = (value as? JsonPrimitive)?.doubleOrNull
            if (number == null || !number.isFinite()) {
                return invalidField(
                    "boundingBox",
                    "an array of four finite numbers",
                )
            }
            numbers += number
        }
        if (numbers[0] >= numbers[2] || numbers[3] >= numbers[1]) {
            return invalidField(
                "boundingBox",
                "[left, top, right, bottom] with positive width and height",
            )
        }
        return GMResult.Ok(
            JsxGraphBoundingBox(
                left = numbers[0],
                top = numbers[1],
                right = numbers[2],
                bottom = numbers[3],
            ),
        )
    }

    private fun parseObject(
        index: Int,
        element: JsonElement,
    ): GMResult<ParsedObject, JsxGraphDocumentError> {
        val objectValue = element as? JsonObject
            ?: return invalidField("objects[$index]", "a JSON object")
        val allowedFields = setOf("id", "type", "parents", "attributes")
        objectValue.keys.firstOrNull { it !in allowedFields }?.let { field ->
            return GMResult.Err(
                JsxGraphDocumentError.UnsupportedField(
                    "objects[$index].$field",
                ),
            )
        }
        val id = stringField(objectValue, "id")
            ?: return invalidField("objects[$index].id", "a non-empty string")
        if (id.isEmpty()) {
            return invalidField("objects[$index].id", "a non-empty string")
        }
        val type = stringField(objectValue, "type")
            ?.lowercase()
            ?: return invalidField("objects[$index].type", "a non-empty string")
        if (type.isEmpty()) {
            return invalidField("objects[$index].type", "a non-empty string")
        }
        val parents = objectValue["parents"] as? JsonArray
            ?: return invalidField("objects[$index].parents", "an array")
        val attributes = when (val value = objectValue["attributes"]) {
            null -> JsonObject(emptyMap())
            is JsonObject -> value
            else -> return invalidField(
                "objects[$index].attributes",
                "a JSON object",
            )
        }
        return GMResult.Ok(
            ParsedObject(
                index = index,
                id = id,
                type = type,
                parents = parents,
                attributes = normalizeObjectKeys(attributes),
            ),
        )
    }

    private fun validateCurvePointLimit(
        sourceObject: ParsedObject,
        limit: Int,
    ): GMResult<Unit, JsxGraphDocumentError> {
        if (
            sourceObject.type !in
            setOf("curve", "functiongraph", "plot")
        ) {
            return GMResult.Ok(Unit)
        }
        val dataPointCount = if (
            sourceObject.type == "curve" &&
            sourceObject.parents.size == 2
        ) {
            (sourceObject.parents.firstOrNull() as? JsonArray)?.size
        } else {
            null
        }
        val requested = dataPointCount ?: (
            sourceObject.attributes["numberpointshigh"]
                as? JsonPrimitive
            )?.intOrNull ?: Curve.DEFAULT_SAMPLE_COUNT
        return if (requested > limit) {
            GMResult.Err(
                JsxGraphDocumentError.CurvePointLimitExceeded(
                    objectIndex = sourceObject.index,
                    id = sourceObject.id,
                    limit = limit,
                    actual = requested,
                ),
            )
        } else {
            GMResult.Ok(Unit)
        }
    }

    private fun validatePolygonVertexLimit(
        sourceObject: ParsedObject,
        limit: Int,
    ): GMResult<Unit, JsxGraphDocumentError> {
        if (sourceObject.type != "polygon") {
            return GMResult.Ok(Unit)
        }
        val actual = sourceObject.parents.size
        return if (actual > limit) {
            GMResult.Err(
                JsxGraphDocumentError.PolygonVertexLimitExceeded(
                    objectIndex = sourceObject.index,
                    id = sourceObject.id,
                    limit = limit,
                    actual = actual,
                ),
            )
        } else {
            GMResult.Ok(Unit)
        }
    }

    private fun validateTextLengthLimit(
        sourceObject: ParsedObject,
        limit: Int,
    ): GMResult<Unit, JsxGraphDocumentError> {
        if (sourceObject.type != "text") {
            return GMResult.Ok(Unit)
        }
        val content = sourceObject.parents.lastOrNull()
        val actual = (
            content as? JsonPrimitive
        )?.takeIf(JsonPrimitive::isString)?.content?.length ?: 0
        return if (actual > limit) {
            GMResult.Err(
                JsxGraphDocumentError.TextLengthLimitExceeded(
                    objectIndex = sourceObject.index,
                    id = sourceObject.id,
                    limit = limit,
                    actual = actual,
                ),
            )
        } else {
            GMResult.Ok(Unit)
        }
    }

    private fun runtimeAttributes(
        sourceObject: ParsedObject,
    ): GMResult<JessieCodeRuntimeValue.ObjectValue, JsxGraphDocumentError> {
        val converted = when (
            val result = runtimeObject(sourceObject.attributes)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val explicitId = converted.properties["id"]
        if (
            explicitId != null &&
            (
                explicitId !is JessieCodeRuntimeValue.StringValue ||
                    explicitId.value != sourceObject.id
            )
        ) {
            return GMResult.Err(
                JsxGraphDocumentError.InvalidAttribute(
                    objectIndex = sourceObject.index,
                    id = sourceObject.id,
                    attribute = "id",
                    expected = "the same string as objects[${sourceObject.index}].id",
                ),
            )
        }
        converted.properties["id"] =
            JessieCodeRuntimeValue.StringValue(sourceObject.id)
        return GMResult.Ok(converted)
    }

    private fun runtimeArray(
        array: JsonArray,
    ): GMResult<List<JessieCodeRuntimeValue>, JsxGraphDocumentError> {
        val values = mutableListOf<JessieCodeRuntimeValue>()
        for (value in array) {
            when (val result = runtimeValue(value)) {
                is GMResult.Ok -> values += result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(values)
    }

    private fun runtimeObject(
        value: JsonObject,
    ): GMResult<JessieCodeRuntimeValue.ObjectValue, JsxGraphDocumentError> {
        val properties = linkedMapOf<String, JessieCodeRuntimeValue>()
        for ((name, child) in value) {
            when (val result = runtimeValue(child)) {
                is GMResult.Ok -> properties[name.lowercase()] = result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(JessieCodeRuntimeValue.ObjectValue(properties))
    }

    private fun runtimeValue(
        value: JsonElement,
    ): GMResult<JessieCodeRuntimeValue, JsxGraphDocumentError> =
        when (value) {
            JsonNull -> GMResult.Ok(JessieCodeRuntimeValue.NullValue)
            is JsonArray -> when (val result = runtimeArray(value)) {
                is GMResult.Ok -> GMResult.Ok(
                    JessieCodeRuntimeValue.ArrayValue(result.value),
                )
                is GMResult.Err -> result
            }
            is JsonObject -> runtimeObject(value)
            is JsonPrimitive -> when {
                value.isString -> GMResult.Ok(
                    JessieCodeRuntimeValue.StringValue(value.content),
                )
                value.booleanOrNull != null -> GMResult.Ok(
                    JessieCodeRuntimeValue.BooleanValue(
                        value.booleanOrNull ?: false,
                    ),
                )
                value.doubleOrNull?.isFinite() == true -> GMResult.Ok(
                    JessieCodeRuntimeValue.NumberValue(
                        value.doubleOrNull ?: Double.NaN,
                    ),
                )
                else -> invalidField("JSON value", "a finite JSON primitive")
            }
        }

    private fun validateLimits(
        limits: JsxGraphEngineLimits,
    ): JsxGraphDocumentError.InvalidLimits? {
        val invalid = when {
            limits.maxSourceLength <= 0 -> "maxSourceLength must be positive"
            limits.maxJsonDepth !in 1..MAX_JSON_DEPTH ->
                "maxJsonDepth must be in 1..$MAX_JSON_DEPTH"
            limits.maxJsonValues <= 0 -> "maxJsonValues must be positive"
            limits.maxObjects <= 0 -> "maxObjects must be positive"
            limits.maxCurvePoints <= 0 -> "maxCurvePoints must be positive"
            limits.maxPolygonVertices <= 0 ->
                "maxPolygonVertices must be positive"
            limits.maxTextLength <= 0 ->
                "maxTextLength must be positive"
            else -> null
        }
        return invalid?.let(JsxGraphDocumentError::InvalidLimits)
    }

    private fun exceedsJsonDepth(
        source: String,
        limit: Int,
    ): Boolean {
        var depth = 0
        var inString = false
        var escaped = false
        for (character in source) {
            if (inString) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == '"' -> inString = false
                }
                continue
            }
            when (character) {
                '"' -> inString = true
                '{', '[' -> {
                    depth += 1
                    if (depth > limit) {
                        return true
                    }
                }
                '}', ']' -> depth -= 1
            }
        }
        return false
    }

    private fun exceedsJsonValueLimit(
        root: JsonElement,
        limit: Int,
    ): Boolean {
        val pending = mutableListOf(root)
        var count = 0
        while (pending.isNotEmpty()) {
            val value = pending.removeAt(pending.lastIndex)
            count += 1
            if (count > limit) {
                return true
            }
            when (value) {
                is JsonArray -> pending.addAll(value)
                is JsonObject -> pending.addAll(value.values)
                else -> Unit
            }
        }
        return false
    }

    private fun booleanField(
        objectValue: JsonObject,
        name: String,
        default: Boolean,
    ): GMResult<Boolean, JsxGraphDocumentError> {
        val element = objectValue[name] ?: return GMResult.Ok(default)
        val value = (element as? JsonPrimitive)?.booleanOrNull
            ?: return invalidField(name, "a boolean")
        return GMResult.Ok(value)
    }

    private fun stringField(
        objectValue: JsonObject,
        name: String,
    ): String? {
        val primitive = objectValue[name] as? JsonPrimitive ?: return null
        return primitive.takeIf(JsonPrimitive::isString)?.content
    }

    private fun normalizeObjectKeys(
        value: JsonObject,
    ): JsonObject {
        val normalized = linkedMapOf<String, JsonElement>()
        for ((name, child) in value) {
            normalized[name.lowercase()] = when (child) {
                is JsonObject -> normalizeObjectKeys(child)
                else -> child
            }
        }
        return JsonObject(normalized)
    }

    private fun point(point: Point): JsxGraphPoint2D? {
        val x = point.X()
        val y = point.Y()
        return if (x.isFinite() && y.isFinite()) {
            JsxGraphPoint2D(x, y)
        } else {
            null
        }
    }

    private fun normalizePointFace(face: String): String =
        when (face.lowercase()) {
            "circle" -> "o"
            else -> face.lowercase()
        }

    private fun <T> invalidField(
        path: String,
        expected: String,
    ): GMResult<T, JsxGraphDocumentError> =
        GMResult.Err(
            JsxGraphDocumentError.InvalidField(
                path = path,
                expected = expected,
            ),
        )

    private data class ParsedDocument(
        val boundingBox: JsxGraphBoundingBox,
        val axis: Boolean,
        val grid: Boolean,
        val keepAspectRatio: Boolean,
        val objects: List<ParsedObject>,
    )

    private data class ParsedObject(
        val index: Int,
        val id: String,
        val type: String,
        val parents: JsonArray,
        val attributes: JsonObject,
    )

    private data class CreatedSourceElement(
        val source: ParsedObject,
        val element: GeometryElement,
    )

    private class AttributeReader(
        private val source: ParsedObject,
    ) {
        private val attributes = source.attributes

        fun validateSupported(
            element: GeometryElement,
        ): GMResult<Unit, JsxGraphDocumentError> {
            val supported = COMMON_ATTRIBUTES + when (element) {
                is Point -> POINT_ATTRIBUTES
                is Line -> LINE_ATTRIBUTES
                is Circle -> CIRCLE_ATTRIBUTES
                is Arc -> ARC_ATTRIBUTES
                is Sector ->
                    if (element.isAngle) {
                        ANGLE_ATTRIBUTES
                    } else {
                        SECTOR_ATTRIBUTES
                    }
                is Curve -> CURVE_ATTRIBUTES
                is Polygon -> POLYGON_ATTRIBUTES
                is Text -> TEXT_ATTRIBUTES
                else -> emptySet()
            }
            attributes.keys.firstOrNull { it !in supported }?.let { name ->
                return GMResult.Err(
                    JsxGraphDocumentError.UnsupportedAttribute(
                        objectIndex = source.index,
                        id = source.id,
                        attribute = name,
                    ),
                )
            }
            val nestedNames = when (element) {
                is Line -> listOf("point1", "point2")
                is Circle -> listOf("center", "point2")
                is Arc -> listOf("center", "radiuspoint", "anglepoint")
                is Sector ->
                    listOf("center", "radiuspoint", "anglepoint", "arc")
                else -> emptyList()
            }
            for (name in nestedNames) {
                when (val result = validateHiddenSubElement(name)) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            return GMResult.Ok(Unit)
        }

        fun style(
            element: GeometryElement,
        ): GMResult<JsxGraphElementStyle, JsxGraphDocumentError> {
            val defaultStroke = when (element) {
                is Point -> DEFAULT_POINT_COLOR
                is Text -> DEFAULT_TEXT_COLOR
                is Sector ->
                    if (element.isAngle) {
                        DEFAULT_ANGLE_COLOR
                    } else {
                        DEFAULT_STROKE_COLOR
                    }
                else -> DEFAULT_STROKE_COLOR
            }
            val defaultFill = when (element) {
                is Point -> DEFAULT_POINT_COLOR
                is Polygon -> DEFAULT_POLYGON_FILL_COLOR
                is Sector ->
                    if (element.isAngle) {
                        DEFAULT_ANGLE_COLOR
                    } else {
                        DEFAULT_POLYGON_FILL_COLOR
                    }
                else -> JsxGraphColor.Transparent
            }
            val visible = when (
                val result = boolean("visible", default = true)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val strokeColor = when (
                val result = color("strokecolor", defaultStroke)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val fillColor = when (
                val result = color("fillcolor", defaultFill)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val strokeWidth = when (
                val result = number(
                    "strokewidth",
                    default =
                        if (
                            element is Curve ||
                            element is Arc ||
                            element is Sector ||
                            element is Polygon
                        ) {
                            1.0
                        } else {
                            2.0
                        },
                    minimum = 0.0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val strokeOpacity = when (
                val result = number(
                    "strokeopacity",
                    default = 1.0,
                    minimum = 0.0,
                    maximum = 1.0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val fillOpacity = when (
                val result = number(
                    "fillopacity",
                    default =
                        if (element is Polygon || element is Sector) {
                            0.3
                        } else {
                            1.0
                        },
                    minimum = 0.0,
                    maximum = 1.0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val dash = when (
                val result = number("dash", default = 0.0, minimum = 0.0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (dash != 0.0) {
                return GMResult.Err(
                    unsupportedValue("dash", dash.toString()),
                )
            }
            for (arrow in listOf("firstarrow", "lastarrow")) {
                when (val value = attributes[arrow]) {
                    null -> Unit
                    is JsonPrimitive -> {
                        val enabled = value.booleanOrNull
                            ?: return invalid(arrow, "false")
                        if (enabled) {
                            return GMResult.Err(
                                unsupportedValue(arrow, "true"),
                            )
                        }
                    }
                    else -> return GMResult.Err(
                        unsupportedValue(arrow, value.toString()),
                    )
                }
            }
            return GMResult.Ok(
                JsxGraphElementStyle(
                    visible = visible,
                    strokeColor = strokeColor,
                    fillColor = fillColor,
                    strokeWidth = strokeWidth,
                    strokeOpacity = strokeOpacity,
                    fillOpacity = fillOpacity,
                ),
            )
        }

        fun boolean(
            name: String,
            default: Boolean,
        ): GMResult<Boolean, JsxGraphDocumentError> {
            val value = attributes[name] ?: return GMResult.Ok(default)
            val boolean = (value as? JsonPrimitive)?.booleanOrNull
                ?: return invalid(name, "a boolean")
            return GMResult.Ok(boolean)
        }

        fun string(
            name: String,
            default: String,
        ): GMResult<String, JsxGraphDocumentError> {
            val value = attributes[name] ?: return GMResult.Ok(default)
            val primitive = value as? JsonPrimitive
                ?: return invalid(name, "a string")
            if (!primitive.isString) {
                return invalid(name, "a string")
            }
            return GMResult.Ok(primitive.content)
        }

        fun number(
            name: String,
            default: Double,
            minimum: Double? = null,
            maximum: Double? = null,
        ): GMResult<Double, JsxGraphDocumentError> {
            val value = attributes[name] ?: return GMResult.Ok(default)
            val number = (value as? JsonPrimitive)?.doubleOrNull
            if (
                number == null ||
                !number.isFinite() ||
                minimum?.let { number < it } == true ||
                maximum?.let { number > it } == true
            ) {
                val range = when {
                    minimum != null && maximum != null ->
                        "a finite number in $minimum..$maximum"
                    minimum != null -> "a finite number >= $minimum"
                    maximum != null -> "a finite number <= $maximum"
                    else -> "a finite number"
                }
                return invalid(name, range)
            }
            return GMResult.Ok(number)
        }

        fun unsupportedValue(
            attribute: String,
            value: String,
        ): JsxGraphDocumentError.UnsupportedAttributeValue =
            JsxGraphDocumentError.UnsupportedAttributeValue(
                objectIndex = source.index,
                id = source.id,
                attribute = attribute,
                value = value,
            )

        fun nonFiniteGeometry(): JsxGraphDocumentError.NonFiniteGeometry =
            JsxGraphDocumentError.NonFiniteGeometry(
                objectIndex = source.index,
                id = source.id,
            )

        private fun color(
            name: String,
            default: JsxGraphColor,
        ): GMResult<JsxGraphColor, JsxGraphDocumentError> {
            val sourceColor = when (val result = string(name, default = "")) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (sourceColor.isEmpty()) {
                return GMResult.Ok(default)
            }
            return parseColor(sourceColor)?.let { color ->
                GMResult.Ok(color)
            }
                ?: GMResult.Err(
                    unsupportedValue(name, sourceColor),
                )
        }

        private fun validateHiddenSubElement(
            name: String,
        ): GMResult<Unit, JsxGraphDocumentError> {
            val value = attributes[name] ?: return GMResult.Ok(Unit)
            val nested = value as? JsonObject
                ?: return invalid(name, "an object")
            nested.keys.firstOrNull { it != "visible" }?.let { nestedName ->
                return GMResult.Err(
                    JsxGraphDocumentError.UnsupportedAttribute(
                        objectIndex = source.index,
                        id = source.id,
                        attribute = "$name.$nestedName",
                    ),
                )
            }
            val visible = nested["visible"] ?: return GMResult.Ok(Unit)
            val isVisible = (visible as? JsonPrimitive)?.booleanOrNull
                ?: return invalid("$name.visible", "a boolean")
            return if (isVisible) {
                GMResult.Err(
                    unsupportedValue("$name.visible", "true"),
                )
            } else {
                GMResult.Ok(Unit)
            }
        }

        private fun <T> invalid(
            name: String,
            expected: String,
        ): GMResult<T, JsxGraphDocumentError> =
            GMResult.Err(
                JsxGraphDocumentError.InvalidAttribute(
                    objectIndex = source.index,
                    id = source.id,
                    attribute = name,
                    expected = expected,
                ),
            )
    }

    private fun parseColor(value: String): JsxGraphColor? {
        val normalized = value.lowercase()
        NAMED_COLORS[normalized]?.let { return it }
        if (!normalized.startsWith("#")) {
            return null
        }
        val digits = normalized.drop(1)
        val expanded = when (digits.length) {
            3, 4 -> buildString {
                for (digit in digits) {
                    append(digit)
                    append(digit)
                }
            }
            6, 8 -> digits
            else -> return null
        }
        val channels = expanded.chunked(2).map { channel ->
            val first = channel[0].digitToIntOrNull(16) ?: return null
            val second = channel[1].digitToIntOrNull(16) ?: return null
            first * 16 + second
        }
        return JsxGraphColor(
            red = channels[0],
            green = channels[1],
            blue = channels[2],
            alpha = channels.getOrElse(3) { 255 },
        )
    }

    private const val MAX_JSON_DEPTH = 256
    private val SOURCE_LOCATION = JessieCodeAstLocation(
        line = 1,
        column = 0,
        endLine = 1,
        endColumn = 0,
    )
    private val DEFAULT_STROKE_COLOR =
        JsxGraphColor(red = 0, green = 114, blue = 178)
    private val DEFAULT_POINT_COLOR =
        JsxGraphColor(red = 213, green = 94, blue = 0)
    private val DEFAULT_POLYGON_FILL_COLOR =
        JsxGraphColor(red = 240, green = 228, blue = 66)
    private val DEFAULT_TEXT_COLOR =
        JsxGraphColor(red = 0, green = 0, blue = 0)
    private val DEFAULT_ANGLE_COLOR =
        JsxGraphColor(red = 230, green = 159, blue = 0)
    private val NAMED_COLORS = mapOf(
        "none" to JsxGraphColor.Transparent,
        "transparent" to JsxGraphColor.Transparent,
        "black" to JsxGraphColor(0, 0, 0),
        "white" to JsxGraphColor(255, 255, 255),
        "red" to JsxGraphColor(255, 0, 0),
        "green" to JsxGraphColor(0, 128, 0),
        "blue" to JsxGraphColor(0, 0, 255),
        "yellow" to JsxGraphColor(255, 255, 0),
        "gray" to JsxGraphColor(128, 128, 128),
        "grey" to JsxGraphColor(128, 128, 128),
    )
    private val COMMON_ATTRIBUTES = setOf(
        "id",
        "name",
        "needsregularupdate",
        "visible",
        "strokecolor",
        "fillcolor",
        "strokewidth",
        "strokeopacity",
        "fillopacity",
        "fixed",
        "highlight",
        "withlabel",
        "dash",
    )
    private val POINT_ATTRIBUTES = setOf(
        "size",
        "face",
    )
    private val LINE_ATTRIBUTES = setOf(
        "straightfirst",
        "straightlast",
        "firstarrow",
        "lastarrow",
        "point1",
        "point2",
    )
    private val CIRCLE_ATTRIBUTES = setOf(
        "center",
        "point2",
    )
    private val CURVE_ATTRIBUTES = setOf(
        "doadvancedplot",
        "numberpointshigh",
        "firstarrow",
        "lastarrow",
        "linecap",
    )
    private val ARC_ATTRIBUTES = setOf(
        "selection",
        "orientation",
        "usedirection",
        "firstarrow",
        "lastarrow",
        "linecap",
        "center",
        "radiuspoint",
        "anglepoint",
    )
    private val SECTOR_ATTRIBUTES = ARC_ATTRIBUTES + setOf(
        "arc",
    )
    private val ANGLE_ATTRIBUTES = SECTOR_ATTRIBUTES + setOf(
        "radius",
        "type",
        "orthotype",
        "orthosensitivity",
    )
    private val POLYGON_ATTRIBUTES = setOf(
        "withlines",
    )
    private val TEXT_ATTRIBUTES = setOf(
        "fontsize",
        "fontunit",
        "formatnumber",
        "digits",
        "parse",
        "display",
        "anchorx",
        "anchory",
        "rotate",
        "usemathjax",
        "usekatex",
        "useasciimathml",
        "tofraction",
    )
    private val TEXT_ANCHOR_X_VALUES =
        setOf("left", "middle", "right")
    private val TEXT_ANCHOR_Y_VALUES =
        setOf("top", "middle", "bottom")
    private val TEXT_DISPLAY_VALUES =
        setOf("html", "internal")
    private val TEXT_DISABLED_BOOLEAN_ATTRIBUTES = listOf(
        "usemathjax",
        "usekatex",
        "useasciimathml",
        "tofraction",
    )

}
