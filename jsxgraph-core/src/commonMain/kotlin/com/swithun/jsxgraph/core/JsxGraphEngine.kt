/*
 * Kotlin translation support for JSXGraph.
 * Upstream: src/base/board.js -> create,
 * src/jxg.js -> registerElement,
 * src/base/element.js -> visual properties,
 * src/element/comb.js -> createComb,
 * src/element/composition.js -> createInequality
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.base.boxPlotPointCount
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeCreator
import com.swithun.jsxgraph.core.parser.JessieCodeEvaluatorLimits
import com.swithun.jsxgraph.core.parser.JessieCodeLexerError
import com.swithun.jsxgraph.core.parser.JessieCodeLexerLimits
import com.swithun.jsxgraph.core.parser.JessieCodeParserError
import com.swithun.jsxgraph.core.parser.JessieCodeParserLimits
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeEnvironment
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.parser.JessieCodeSession
import com.swithun.jsxgraph.core.parser.JessieCodeSessionError
import com.swithun.jsxgraph.core.parser.JessieCodeSessionLimits
import com.swithun.jsxgraph.core.parser.JessieCodeSourceLocation
import com.swithun.jsxgraph.core.parser.JessieCodeSourcePosition
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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.round

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

data class JsxGraphInteractionState(
    val pointCoordinates: Map<String, JsxGraphPoint2D>,
)

sealed interface JsxGraphInteractionError {
    data class UnknownPoint(
        val id: String,
    ) : JsxGraphInteractionError

    data class PointNotDraggable(
        val id: String,
    ) : JsxGraphInteractionError

    data class NonFiniteCoordinates(
        val id: String,
        val coordinates: JsxGraphPoint2D,
    ) : JsxGraphInteractionError

    data class StateSizeExceeded(
        val limit: Int,
        val actual: Int,
    ) : JsxGraphInteractionError

    data class ResourceLimitExceeded(
        val resource: String,
        val limit: Int,
        val requestedSize: Long,
    ) : JsxGraphInteractionError

    data class SceneUpdate(
        val error: JsxGraphDocumentError,
    ) : JsxGraphInteractionError
}

/**
 * Mutable production Board session for Point interaction.
 *
 * The session owns the translated Board. Compose and other clients only
 * exchange portable scene snapshots and explicit interaction state.
 */
class JsxGraphSession internal constructor(
    private val board: Board,
    private val points: Map<String, SessionPoint>,
    initialScene: JsxGraphScene,
    private val snapshotScene: () -> GMResult<
        JsxGraphScene,
        JsxGraphDocumentError,
        >,
) {
    private val initialInteractionState = captureInteractionState()

    var scene: JsxGraphScene = initialScene
        private set

    fun movePoint(
        id: String,
        coordinates: JsxGraphPoint2D,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> {
        val point = when (val result = draggablePoint(id, coordinates)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val previous = captureBoardPointCoordinates()
        point.setPositionDirectly(
            method = com.swithun.jsxgraph.core.base.Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(coordinates.x, coordinates.y),
        )
        board.update(draggedElement = point)
        return commitOrRollback(previous)
    }

    fun captureInteractionState(): JsxGraphInteractionState {
        val coordinates = linkedMapOf<String, JsxGraphPoint2D>()
        for ((id, handle) in points) {
            if (handle.draggable) {
                coordinates[id] = JsxGraphPoint2D(
                    x = handle.point.X(),
                    y = handle.point.Y(),
                )
            }
        }
        return JsxGraphInteractionState(coordinates)
    }

    fun restoreInteractionState(
        state: JsxGraphInteractionState,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> =
        applyInteractionState(
            state = state,
            settleDirectionSelection = false,
        )

    fun resetInteractionState():
        GMResult<JsxGraphScene, JsxGraphInteractionError> =
        applyInteractionState(
            state = initialInteractionState,
            settleDirectionSelection = true,
        )

    private fun applyInteractionState(
        state: JsxGraphInteractionState,
        settleDirectionSelection: Boolean,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> {
        if (state.pointCoordinates.size > points.size) {
            return GMResult.Err(
                JsxGraphInteractionError.StateSizeExceeded(
                    limit = points.size,
                    actual = state.pointCoordinates.size,
                ),
            )
        }
        val resolved = mutableListOf<Pair<Point, JsxGraphPoint2D>>()
        for ((id, coordinates) in state.pointCoordinates) {
            when (val result = draggablePoint(id, coordinates)) {
                is GMResult.Ok -> resolved += result.value to coordinates
                is GMResult.Err -> return result
            }
        }
        val previous = captureBoardPointCoordinates()
        for ((point, coordinates) in resolved) {
            point.setPositionDirectly(
                method = com.swithun.jsxgraph.core.base.Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(coordinates.x, coordinates.y),
            )
        }
        board.fullUpdate()
        if (settleDirectionSelection) {
            // JSXGraph 1.13.3: src/element/arc.js -> updateDataArray.
            // A useDirection Arc selects new endpoints after capturing the
            // current pair, so reset needs the same follow-up Board cycle that
            // makes a direction change visible in the upstream renderer.
            board.fullUpdate()
        }
        return commitOrRollback(previous)
    }

    private fun draggablePoint(
        id: String,
        coordinates: JsxGraphPoint2D,
    ): GMResult<Point, JsxGraphInteractionError> {
        val handle = points[id]
            ?: return GMResult.Err(
                JsxGraphInteractionError.UnknownPoint(id),
            )
        if (!handle.draggable) {
            return GMResult.Err(
                JsxGraphInteractionError.PointNotDraggable(id),
            )
        }
        if (!coordinates.x.isFinite() || !coordinates.y.isFinite()) {
            return GMResult.Err(
                JsxGraphInteractionError.NonFiniteCoordinates(
                    id = id,
                    coordinates = coordinates,
                ),
            )
        }
        return GMResult.Ok(handle.point)
    }

    private fun captureBoardPointCoordinates(): Map<Point, DoubleArray> =
        board.objectsList
            .filterIsInstance<Point>()
            .associateWith(Point::Coords)

    private fun commitOrRollback(
        previous: Map<Point, DoubleArray>,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> =
        when (val result = snapshotScene()) {
            is GMResult.Ok -> {
                scene = result.value
                result
            }
            is GMResult.Err -> {
                for ((point, coordinates) in previous) {
                    point.setPositionDirectly(
                        method =
                            com.swithun.jsxgraph.core.base.Const.COORDS_BY_USER,
                        coordinates = coordinates,
                    )
                }
                board.fullUpdate()
                GMResult.Err(
                    JsxGraphInteractionError.SceneUpdate(result.error),
                )
            }
        }
}

internal data class SessionPoint(
    val point: Point,
    val draggable: Boolean,
)

/**
 * Parses the construction document, creates translated Board elements through
 * the native creator registry, and snapshots those elements into a portable
 * render scene.
 */
object JsxGraphEngine {
    internal fun createJessieCodeSession(
        boardOptions: JsxGraphJessieCodeBoardOptions,
        limits: JsxGraphJessieCodeLimits,
    ): GMResult<
        JsxGraphJessieCodeSession,
        JsxGraphJessieCodeError,
        > {
        validateJessieCodeConfiguration(boardOptions, limits)?.let { error ->
            return GMResult.Err(error)
        }

        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "jxgBoard",
            boundingBox = doubleArrayOf(
                boardOptions.boundingBox.left,
                boardOptions.boundingBox.top,
                boardOptions.boundingBox.right,
                boardOptions.boundingBox.bottom,
            ),
            defaultCurveMinimum =
                boardOptions.boundingBox.left -
                    (
                        boardOptions.boundingBox.right -
                            boardOptions.boundingBox.left
                        ) * CURVE_DOMAIN_PADDING,
            defaultCurveMaximum =
                boardOptions.boundingBox.right +
                    (
                        boardOptions.boundingBox.right -
                            boardOptions.boundingBox.left
                        ) * CURVE_DOMAIN_PADDING,
        )
        val created = mutableListOf<CreatedSourceElement>()
        var creationCount = 0
        val trackedCreators = linkedMapOf<String, JessieCodeCreator>()
        for (creatorName in NativeJessieCodeCreators.names) {
            val nativeCreator =
                NativeJessieCodeCreators.creator(creatorName) ?: continue
            trackedCreators[creatorName] = JessieCodeCreator {
                    selectedBoard,
                    parents,
                    attributes,
                    location,
                ->
                val createsSceneElement = creatorName != "transform"
                val createdSceneElementCount =
                    sceneElementCount(creatorName).toLong()
                val requestedObjectCount =
                    creationCount.toLong() + createdSceneElementCount
                if (
                    createsSceneElement &&
                    requestedObjectCount > limits.maxObjects
                ) {
                    return@JessieCodeCreator GMResult.Err(
                        JessieCodeRuntimeError.ResourceLimitExceeded(
                            resource = "created element count",
                            limit = limits.maxObjects,
                            requestedSize = requestedObjectCount,
                            location = location,
                        ),
                    )
                }
                validateJessieCodeCreatorRequest(
                    creatorName = creatorName,
                    parents = parents,
                    attributes = attributes,
                    limits = limits,
                    location = location,
                )?.let { error ->
                    return@JessieCodeCreator GMResult.Err(error)
                }
                val sourceAttributes = when (
                    val result = JessieCodeAttributeSnapshotter(
                        maxDepth = limits.maxAttributeDepth,
                        maxValues = limits.maxCollectionSize,
                        location = location,
                        ignoredRootFunctionProperties =
                            when (creatorName) {
                                "comb" -> COMB_SEMANTIC_ATTRIBUTES
                                "inequality" ->
                                    INEQUALITY_SEMANTIC_ATTRIBUTES
                                "vectorfield", "slopefield" ->
                                    VECTOR_FIELD_SEMANTIC_ATTRIBUTES
                                else -> emptySet()
                            },
                        ignoredFunctionPropertiesByPath =
                            if (
                                creatorName == "vectorfield" ||
                                creatorName == "slopefield"
                            ) {
                                mapOf(
                                    "attributes.arrowhead" to
                                        VECTOR_FIELD_ARROW_HEAD_ATTRIBUTES,
                                )
                            } else {
                                emptyMap()
                            },
                    ).snapshot(attributes)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err ->
                        return@JessieCodeCreator result
                }
                when (
                    val result = nativeCreator.create(
                        board = selectedBoard,
                        parents = parents,
                        attributes = attributes,
                        location = location,
                    )
                ) {
                    is GMResult.Err -> result
                    is GMResult.Ok -> {
                        when (val value = result.value) {
                            is JessieCodeRuntimeValue
                                .TransformationReference ->
                                if (creatorName == "transform") {
                                    result
                                } else {
                                    GMResult.Err(
                                        JessieCodeRuntimeError.InvalidAst(
                                            reason =
                                                "Native creator " +
                                                    "'$creatorName' returned " +
                                                    "a transformation.",
                                            location = location,
                                        ),
                                    )
                                }
                            is JessieCodeRuntimeValue.ElementReference -> {
                                if (!createsSceneElement) {
                                    return@JessieCodeCreator GMResult.Err(
                                        JessieCodeRuntimeError.InvalidAst(
                                            reason =
                                                "Native transform creator " +
                                                    "returned an element.",
                                            location = location,
                                        ),
                                    )
                                }
                                val source = ParsedObject(
                                    index =
                                        creationCount +
                                            if (creatorName == "tangentto") {
                                                2
                                            } else {
                                                0
                                            },
                                    id = value.element.id,
                                    type = creatorName,
                                    parents = JsonArray(emptyList()),
                                    attributes = sourceAttributes,
                                )
                                if (creatorName == "tangentto") {
                                    val line = value.element as? Line
                                    if (line == null) {
                                        return@JessieCodeCreator invalidTangentToResult(
                                            location,
                                        )
                                    }
                                    val expanded =
                                        tangentToCreatedSourceElements(
                                            source = source,
                                            line = line,
                                            polarIndex = creationCount,
                                            pointIndex = creationCount + 1,
                                            tangentIndex = creationCount + 2,
                                        )
                                    if (expanded == null) {
                                        return@JessieCodeCreator invalidTangentToResult(
                                            location,
                                        )
                                    }
                                    created += expanded
                                    creationCount += expanded.size
                                } else {
                                    created += CreatedSourceElement(
                                        source = source,
                                        element = value.element,
                                    )
                                    creationCount += 1
                                }
                                result
                            }
                            is JessieCodeRuntimeValue.CompositionReference -> {
                                if (
                                    !createsSceneElement ||
                                    creatorName != "bisectorlines"
                                ) {
                                    return@JessieCodeCreator GMResult.Err(
                                        JessieCodeRuntimeError.InvalidAst(
                                            reason =
                                                "Native creator " +
                                                    "'$creatorName' returned " +
                                                    "a composition.",
                                            location = location,
                                        ),
                                    )
                                }
                                val composition = value.composition
                                val members = listOf("line1", "line2")
                                    .mapNotNull { role ->
                                        composition.member(role)?.let {
                                            role to it
                                        }
                                    }
                                if (members.size != 2) {
                                    return@JessieCodeCreator GMResult.Err(
                                        JessieCodeRuntimeError.InvalidAst(
                                            reason =
                                                "Native bisectorlines creator " +
                                                    "returned an incomplete " +
                                                    "composition.",
                                            location = location,
                                        ),
                                    )
                                }
                                for ((role, element) in members) {
                                    created += CreatedSourceElement(
                                        source = ParsedObject(
                                            index = creationCount,
                                            id = element.id,
                                            type = creatorName,
                                            parents = JsonArray(emptyList()),
                                            attributes =
                                                bisectorLineAttributes(
                                                    sourceAttributes,
                                                    role,
                                                ),
                                        ),
                                        element = element,
                                    )
                                    creationCount += 1
                                }
                                result
                            }
                            else -> GMResult.Err(
                                JessieCodeRuntimeError.InvalidAst(
                                    reason =
                                        "Native creator '$creatorName' did " +
                                            "not return a supported value.",
                                    location = location,
                                ),
                            )
                        }
                    }
                }
            }
        }

        val interpreter = JessieCodeSession(
            environment = JessieCodeRuntimeEnvironment(
                board = board,
                boardsByContainer = mapOf(
                    boardOptions.containerId to board,
                ),
                creators = trackedCreators,
            ),
            lexerLimits = JessieCodeLexerLimits(
                maxSourceLength = limits.maxSourceLength,
                maxTokens = limits.maxTokens,
            ),
            parserLimits = JessieCodeParserLimits(
                maxAstNodes = limits.maxAstNodes,
                maxAstDepth = limits.maxAstDepth,
                maxParserNesting = limits.maxParserNesting,
            ),
            evaluatorLimits = JessieCodeEvaluatorLimits(
                maxEvaluationSteps = limits.maxEvaluationSteps,
                maxEvaluationDepth = limits.maxEvaluationDepth,
                maxCollectionSize = limits.maxCollectionSize,
            ),
            sessionLimits = JessieCodeSessionLimits(
                maxStoredSourceLength = limits.maxStoredSourceLength,
            ),
        )
        val document = ParsedDocument(
            boundingBox = boardOptions.boundingBox,
            axis = boardOptions.axis,
            grid = boardOptions.grid,
            keepAspectRatio = boardOptions.keepAspectRatio,
            objects = emptyList(),
        )
        val initialScene = JsxGraphScene(
            boundingBox = document.boundingBox,
            axis = document.axis,
            grid = document.grid,
            keepAspectRatio = document.keepAspectRatio,
            elements = emptyList(),
        )
        val snapshotCurrentScene = {
            val active = created.filter { sourceElement ->
                board.elementById(sourceElement.element.id) ===
                    sourceElement.element
            }
            snapshotScene(
                document = document.copy(
                    objects = active.map(CreatedSourceElement::source),
                ),
                created = active,
            )
        }
        val dynamicCurveLimitError = {
            created.asSequence()
                .filter { sourceElement ->
                    board.elementById(sourceElement.element.id) ===
                        sourceElement.element
                }
                .mapNotNull { sourceElement ->
                    val curve = sourceElement.element as? Curve
                        ?: return@mapNotNull null
                    maxOf(
                        curve.requestedPointCount()
                            ?: curve.numberPoints.toLong(),
                        curve.numberPoints.toLong(),
                    )
                }
                .firstOrNull { requested ->
                    requested > limits.maxCurvePoints
                }
                ?.let { requested ->
                    JsxGraphJessieCodeError.ResourceLimitExceeded(
                        resource = "curve point count",
                        limit = limits.maxCurvePoints,
                        requestedSize = requested,
                        location = null,
                    )
                }
        }
        return GMResult.Ok(
            JsxGraphJessieCodeSession(
                initialScene = initialScene,
                executeSource = { source, storeSource ->
                    when (
                        val parseResult = interpreter.parse(
                            source = source,
                            storeSource = storeSource,
                        )
                    ) {
                        is GMResult.Err -> GMResult.Err(
                            publicJessieCodeError(parseResult.error),
                        )
                        is GMResult.Ok -> {
                            val limitError = dynamicCurveLimitError()
                            if (limitError != null) {
                                GMResult.Err(limitError)
                            } else {
                                board.fullUpdate()
                                val updatedLimitError =
                                    dynamicCurveLimitError()
                                if (updatedLimitError != null) {
                                    GMResult.Err(updatedLimitError)
                                } else {
                                    when (
                                        val sceneResult =
                                            snapshotCurrentScene()
                                    ) {
                                        is GMResult.Ok -> sceneResult
                                        is GMResult.Err -> GMResult.Err(
                                            JsxGraphJessieCodeError.Scene(
                                                sceneResult.error,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                movePointSource = movePoint@ { id, coordinates ->
                    val point = (
                        created.lastOrNull { sourceElement ->
                            sourceElement.element.id == id &&
                                board.elementById(id) ===
                                sourceElement.element
                        }
                    )?.element as? Point
                        ?: return@movePoint GMResult.Err(
                            JsxGraphInteractionError.UnknownPoint(id),
                        )
                    if (!point.isDraggable || point.isFixed) {
                        return@movePoint GMResult.Err(
                            JsxGraphInteractionError.PointNotDraggable(id),
                        )
                    }
                    if (
                        !coordinates.x.isFinite() ||
                        !coordinates.y.isFinite()
                    ) {
                        return@movePoint GMResult.Err(
                            JsxGraphInteractionError.NonFiniteCoordinates(
                                id = id,
                                coordinates = coordinates,
                            ),
                        )
                    }
                    val previous = board.objectsList
                        .filterIsInstance<Point>()
                        .associateWith(Point::Coords)
                    point.setPositionDirectly(
                        method =
                            com.swithun.jsxgraph.core.base.Const.COORDS_BY_USER,
                        coordinates =
                            doubleArrayOf(coordinates.x, coordinates.y),
                    )
                    board.update(draggedElement = point)
                    val limitError = dynamicCurveLimitError()
                    if (limitError != null) {
                        for ((previousPoint, previousCoordinates) in
                            previous
                        ) {
                            previousPoint.setPositionDirectly(
                                method =
                                    com.swithun.jsxgraph.core.base.Const
                                        .COORDS_BY_USER,
                                coordinates = previousCoordinates,
                            )
                        }
                        board.fullUpdate()
                        return@movePoint GMResult.Err(
                            JsxGraphInteractionError.ResourceLimitExceeded(
                                resource = limitError.resource,
                                limit = limitError.limit,
                                requestedSize = limitError.requestedSize,
                            ),
                        )
                    }
                    when (val result = snapshotCurrentScene()) {
                        is GMResult.Ok -> result
                        is GMResult.Err -> {
                            for ((previousPoint, previousCoordinates) in
                                previous
                            ) {
                                previousPoint.setPositionDirectly(
                                    method =
                                        com.swithun.jsxgraph.core.base.Const
                                            .COORDS_BY_USER,
                                    coordinates = previousCoordinates,
                                )
                            }
                            board.fullUpdate()
                            GMResult.Err(
                                JsxGraphInteractionError.SceneUpdate(
                                    result.error,
                                ),
                            )
                        }
                    }
                },
                storedSource = { interpreter.code },
            ),
        )
    }

    private fun validateJessieCodeConfiguration(
        boardOptions: JsxGraphJessieCodeBoardOptions,
        limits: JsxGraphJessieCodeLimits,
    ): JsxGraphJessieCodeError.InvalidConfiguration? {
        val bounds = boardOptions.boundingBox
        val invalid = when {
            boardOptions.containerId.isEmpty() ->
                "containerId must not be empty"
            !bounds.left.isFinite() ||
                !bounds.top.isFinite() ||
                !bounds.right.isFinite() ||
                !bounds.bottom.isFinite() ->
                "boundingBox values must be finite"
            bounds.left >= bounds.right || bounds.bottom >= bounds.top ->
                "boundingBox must have positive width and height"
            limits.maxSourceLength < 0 ->
                "maxSourceLength must not be negative"
            limits.maxTokens < 0 ->
                "maxTokens must not be negative"
            limits.maxAstNodes < 1 ->
                "maxAstNodes must be positive"
            limits.maxAstDepth < 1 ->
                "maxAstDepth must be positive"
            limits.maxParserNesting !in 1..64 ->
                "maxParserNesting must be in 1..64"
            limits.maxEvaluationSteps < 1 ->
                "maxEvaluationSteps must be positive"
            limits.maxEvaluationDepth !in 1..64 ->
                "maxEvaluationDepth must be in 1..64"
            limits.maxCollectionSize < 1 ->
                "maxCollectionSize must be positive"
            limits.maxStoredSourceLength < 0 ->
                "maxStoredSourceLength must not be negative"
            limits.maxAttributeDepth !in 1..256 ->
                "maxAttributeDepth must be in 1..256"
            limits.maxObjects < 1 ->
                "maxObjects must be positive"
            limits.maxCurvePoints < 1 ->
                "maxCurvePoints must be positive"
            limits.maxPolygonVertices < 1 ->
                "maxPolygonVertices must be positive"
            limits.maxTextLength < 0 ->
                "maxTextLength must not be negative"
            else -> null
        }
        return invalid?.let(
            JsxGraphJessieCodeError::InvalidConfiguration,
        )
    }

    private fun validateJessieCodeCreatorRequest(
        creatorName: String,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        limits: JsxGraphJessieCodeLimits,
        location: JessieCodeAstLocation,
    ): JessieCodeRuntimeError.ResourceLimitExceeded? {
        val curveXValues = (
            parents.getOrNull(0) as? JessieCodeRuntimeValue.ArrayValue
        )?.values
        val curveYValues = (
            parents.getOrNull(1) as? JessieCodeRuntimeValue.ArrayValue
        )?.values
        val stepSourceCount = when (
            val xTerm = parents.getOrNull(0)
        ) {
            is JessieCodeRuntimeValue.ArrayValue ->
                xTerm.values.size.toLong()
            is JessieCodeRuntimeValue.FunctionValue ->
                xTerm.parameterNames.size.toLong()
            else -> null
        }
        val requestedCurvePoints = when {
            creatorName == "curve" &&
                parents.size == 2 &&
                curveXValues != null &&
                curveYValues != null ->
                maxOf(
                    curveXValues.size.toLong(),
                    curveYValues.size.toLong(),
                )
            creatorName == "stepfunction" &&
                parents.size == 2 &&
                stepSourceCount != null ->
                stepFunctionPointCount(stepSourceCount)
            creatorName == "comb" && parents.size == 2 -> {
                val first = runtimeCurveLimitPoint(parents[0])
                val second = runtimeCurveLimitPoint(parents[1])
                val frequency = when (
                    val value = attributes.properties["frequency"]
                ) {
                    null,
                    JessieCodeRuntimeValue.UndefinedValue,
                    -> Curve.COMB_DEFAULT_FREQUENCY
                    is JessieCodeRuntimeValue.NumberValue -> value.value
                    else -> null
                }
                if (
                    first == null ||
                    second == null ||
                    frequency == null ||
                    !frequency.isFinite() ||
                    frequency <= 0.0
                ) {
                    null
                } else {
                    Curve.combPointCount(
                        distance = curveLimitDistance(first, second),
                        frequency = frequency,
                    )
                }
            }
            creatorName == "riemannsum" -> {
                val rectangleCount = (
                    parents.getOrNull(1) as?
                        JessieCodeRuntimeValue.NumberValue
                    )?.value
                val type = when (
                    val value = parents.getOrNull(2)
                ) {
                    is JessieCodeRuntimeValue.StringValue -> value.value
                    is JessieCodeRuntimeValue.NumberValue -> ""
                    else -> null
                }
                if (rectangleCount == null || type == null) {
                    null
                } else {
                    Curve.riemannPointCount(
                        rectangleCount = rectangleCount,
                        type = type,
                        hasLowerFunction = (
                            parents.firstOrNull() as?
                                JessieCodeRuntimeValue.ArrayValue
                            )?.values?.size == 2,
                    )
                }
            }
            creatorName == "boxplot" && curveXValues != null -> {
                val outlierCount = (
                    curveXValues.getOrNull(Curve.BOX_PLOT_QUANTILE_COUNT) as?
                        JessieCodeRuntimeValue.ArrayValue
                    )?.values?.size
                val outlierFace = (
                    (
                        attributes.properties["outlier"] as?
                            JessieCodeRuntimeValue.ObjectValue
                        )?.properties?.get("face") as?
                        JessieCodeRuntimeValue.StringValue
                    )?.value ?: "o"
                boxPlotPointCount(
                    outlierCount = outlierCount,
                    outlierFace = outlierFace,
                )
            }
            creatorName == "inequality" -> null
            creatorName == "vectorfield" ||
                creatorName == "slopefield" -> {
                val xSteps = (
                    (
                        parents.getOrNull(1) as?
                            JessieCodeRuntimeValue.ArrayValue
                        )?.values?.getOrNull(1) as?
                        JessieCodeRuntimeValue.NumberValue
                    )?.value
                val ySteps = (
                    (
                        parents.getOrNull(2) as?
                            JessieCodeRuntimeValue.ArrayValue
                        )?.values?.getOrNull(1) as?
                        JessieCodeRuntimeValue.NumberValue
                    )?.value
                val arrowEnabled = (
                    (
                        (
                            attributes.properties["arrowhead"] as?
                                JessieCodeRuntimeValue.ObjectValue
                            )?.properties?.get("enabled") as?
                            JessieCodeRuntimeValue.BooleanValue
                        )?.value
                    ) ?: (creatorName == "vectorfield")
                if (xSteps == null || ySteps == null) {
                    null
                } else {
                    Curve.vectorFieldPointCount(
                        xSteps = xSteps,
                        ySteps = ySteps,
                        arrowEnabled = arrowEnabled,
                    )
                }
            }
            creatorName in
                setOf(
                    "curve",
                    "ellipse",
                    "hyperbola",
                    "functiongraph",
                    "plot",
                    "derivative",
                    "spline",
                    "cardinalspline",
                ) -> {
                val configured = (
                    attributes.properties["numberpointshigh"] as?
                        JessieCodeRuntimeValue.NumberValue
                    )?.value
                when {
                    configured == null ->
                        Curve.DEFAULT_SAMPLE_COUNT.toLong()
                    configured.isNaN() -> 0L
                    else -> configured.toLong()
                }
            }
            else -> null
        }
        if (
            requestedCurvePoints != null &&
            requestedCurvePoints > limits.maxCurvePoints
        ) {
            return JessieCodeRuntimeError.ResourceLimitExceeded(
                resource = "curve point count",
                limit = limits.maxCurvePoints,
                requestedSize = requestedCurvePoints,
                location = location,
            )
        }
        val requestedPolygonVertices = when {
            creatorName == "regularpolygon" -> {
                val numericCount = (
                    parents.lastOrNull() as?
                        JessieCodeRuntimeValue.NumberValue
                    )?.value
                if (
                    parents.size == 3 &&
                    numericCount != null &&
                    numericCount.isFinite() &&
                    numericCount >= 3.0
                ) {
                    ceil(numericCount).toLong()
                } else {
                    parents.size.toLong()
                }
            }
            creatorName in
                setOf("polygon", "polygonalchain", "parallelogram") ->
                parents.size.toLong()
            else -> null
        }
        if (
            requestedPolygonVertices != null &&
            requestedPolygonVertices > limits.maxPolygonVertices
        ) {
            return JessieCodeRuntimeError.ResourceLimitExceeded(
                resource = "polygon vertex count",
                limit = limits.maxPolygonVertices,
                requestedSize = requestedPolygonVertices,
                location = location,
            )
        }
        if (creatorName == "text") {
            val requestedTextLength = (
                parents.lastOrNull() as?
                    JessieCodeRuntimeValue.StringValue
                )?.value?.length ?: 0
            if (requestedTextLength > limits.maxTextLength) {
                return JessieCodeRuntimeError.ResourceLimitExceeded(
                    resource = "text length",
                    limit = limits.maxTextLength,
                    requestedSize = requestedTextLength.toLong(),
                    location = location,
                )
            }
        }
        return null
    }

    private fun publicJessieCodeError(
        error: JessieCodeSessionError,
    ): JsxGraphJessieCodeError =
        when (error) {
            is JessieCodeSessionError.InvalidLimits ->
                JsxGraphJessieCodeError.InvalidConfiguration(
                    "maxStoredSourceLength must not be negative",
                )
            is JessieCodeSessionError.SourceHistoryLimitExceeded ->
                JsxGraphJessieCodeError.SourceHistoryLimitExceeded(
                    limit = error.limit,
                    requestedSize = error.requestedSize,
                )
            is JessieCodeSessionError.Parser ->
                JsxGraphJessieCodeError.Parse(
                    reason = error.error.toString(),
                    location = parserSourceRange(error.error),
                )
            is JessieCodeSessionError.Runtime -> {
                val runtimeError = error.error
                if (
                    runtimeError is
                        JessieCodeRuntimeError.ResourceLimitExceeded
                ) {
                    JsxGraphJessieCodeError.ResourceLimitExceeded(
                        resource = runtimeError.resource,
                        limit = runtimeError.limit,
                        requestedSize = runtimeError.requestedSize,
                        location =
                            runtimeError.location.toPublicSourceRange(),
                    )
                } else {
                    JsxGraphJessieCodeError.Runtime(
                        reason = runtimeError.toString(),
                        location = runtimeSourceRange(runtimeError),
                    )
                }
            }
        }

    private fun parserSourceRange(
        error: JessieCodeParserError,
    ): JsxGraphJessieCodeSourceRange? =
        when (error) {
            is JessieCodeParserError.Lexer ->
                lexerSourceRange(error.error)
            is JessieCodeParserError.InvalidLimits -> null
            is JessieCodeParserError.UnexpectedToken ->
                error.parserLocation.toPublicSourceRange()
            is JessieCodeParserError.UnsupportedSyntax ->
                error.token.location.toPublicSourceRange()
            is JessieCodeParserError.InvalidNumberLiteral ->
                error.token.location.toPublicSourceRange()
            is JessieCodeParserError.AstNodeLimitExceeded ->
                error.location.toPublicSourceRange()
            is JessieCodeParserError.AstDepthLimitExceeded ->
                error.location.toPublicSourceRange()
            is JessieCodeParserError.ParserNestingLimitExceeded ->
                error.location.toPublicSourceRange()
        }

    private fun lexerSourceRange(
        error: JessieCodeLexerError,
    ): JsxGraphJessieCodeSourceRange? =
        when (error) {
            is JessieCodeLexerError.InvalidLimits -> null
            is JessieCodeLexerError.SourceLengthExceeded -> null
            is JessieCodeLexerError.TokenLimitExceeded ->
                error.location.toPublicSourceRange()
        }

    private fun runtimeSourceRange(
        error: JessieCodeRuntimeError,
    ): JsxGraphJessieCodeSourceRange? {
        val location = when (error) {
            is JessieCodeRuntimeError.InvalidLimits -> null
            is JessieCodeRuntimeError.EvaluationStepLimitExceeded ->
                error.location
            is JessieCodeRuntimeError.EvaluationDepthLimitExceeded ->
                error.location
            is JessieCodeRuntimeError.InvalidAst -> error.location
            is JessieCodeRuntimeError.InvalidAssignmentTarget ->
                error.location
            is JessieCodeRuntimeError.AssignmentTargetUnavailable ->
                error.location
            is JessieCodeRuntimeError.CollectionSizeLimitExceeded ->
                error.location
            is JessieCodeRuntimeError.ResourceLimitExceeded ->
                error.location
            is JessieCodeRuntimeError.UnsupportedSceneAttribute ->
                error.location
            is JessieCodeRuntimeError.UnsupportedOperation ->
                error.location
            is JessieCodeRuntimeError.NotCallable -> error.location
            is JessieCodeRuntimeError.UnexpectedCreatorAttributes ->
                error.location
            is JessieCodeRuntimeError.BoardNotFound -> error.location
            is JessieCodeRuntimeError.CreatorFailure -> error.location
            is JessieCodeRuntimeError.UnknownProperty -> error.location
            is JessieCodeRuntimeError.FunctionPropertyAccess ->
                error.location
            is JessieCodeRuntimeError.InvalidMapBody -> error.location
            is JessieCodeRuntimeError.FunctionDependency -> error.location
            is JessieCodeRuntimeError.InvalidArgumentCount ->
                error.location
            is JessieCodeRuntimeError.InvalidArgumentType ->
                error.location
            is JessieCodeRuntimeError.ElementValueUnavailable ->
                error.location
            is JessieCodeRuntimeError.ElementPropertyUnavailable ->
                error.location
            is JessieCodeRuntimeError.ElementPropertyAssignmentUnavailable ->
                error.location
            is JessieCodeRuntimeError.InvalidElementPropertyValue ->
                error.location
            is JessieCodeRuntimeError.ElementCoordinateConstraintFailure ->
                error.location
            is JessieCodeRuntimeError.ElementMethodUnavailable ->
                error.location
            is JessieCodeRuntimeError.BuiltInInvocationFailure ->
                error.location
        }
        return location?.toPublicSourceRange()
    }

    private fun JessieCodeAstLocation.toPublicSourceRange():
        JsxGraphJessieCodeSourceRange =
        JsxGraphJessieCodeSourceRange(
            line = line,
            column = column,
            endLine = endLine,
            endColumn = endColumn,
        )

    private fun JessieCodeSourceLocation.toPublicSourceRange():
        JsxGraphJessieCodeSourceRange =
        JsxGraphJessieCodeSourceRange(
            line = start.line,
            column = start.column,
            endLine = end.line,
            endColumn = end.column,
        )

    private fun JessieCodeSourcePosition.toPublicSourceRange():
        JsxGraphJessieCodeSourceRange =
        JsxGraphJessieCodeSourceRange(
            line = line,
            column = column,
            endLine = line,
            endColumn = column,
        )

    // JSXGraph: src/element/composition.js ->
    // createAngularBisectorsOfTwoLines / Type.copyAttributes
    private fun bisectorLineAttributes(
        attributes: JsonObject,
        role: String,
    ): JsonObject {
        val effective = linkedMapOf<String, JsonElement>()
        val nested = attributes[role] as? JsonObject
        if (nested != null) {
            effective.putAll(nested)
        }
        return JsonObject(effective)
    }

    // JSXGraph 1.13.3: src/base/line.js -> createTangentTo;
    // src/options.js -> tangentto.
    private fun tangentToCreatedSourceElements(
        source: ParsedObject,
        line: Line,
        polarIndex: Int,
        pointIndex: Int,
        tangentIndex: Int,
    ): List<CreatedSourceElement>? {
        val polar = line.tangentToPolar ?: return null
        val point = line.tangentToPoint ?: return null
        val polarAttributes = nestedAttributes(
            attributes = source.attributes,
            name = "polar",
            defaults = mapOf(
                "visible" to JsonPrimitive(false),
                "strokewidth" to JsonPrimitive(1),
                "dash" to JsonPrimitive(3),
                "withlabel" to JsonPrimitive(false),
            ),
        )
        val pointAttributes = nestedAttributes(
            attributes = source.attributes,
            name = "point",
            defaults = mapOf(
                "visible" to JsonPrimitive(false),
                "withlabel" to JsonPrimitive(false),
            ),
        )
        val tangentAttributes = JsonObject(
            source.attributes.filterKeys { key ->
                key != "point" && key != "polar"
            },
        )
        return listOf(
            CreatedSourceElement(
                source = ParsedObject(
                    index = polarIndex,
                    id = polar.id,
                    type = "polar",
                    parents = JsonArray(emptyList()),
                    attributes = polarAttributes,
                ),
                element = polar,
            ),
            CreatedSourceElement(
                source = ParsedObject(
                    index = pointIndex,
                    id = point.id,
                    type = "intersection",
                    parents = JsonArray(emptyList()),
                    attributes = pointAttributes,
                ),
                element = point,
            ),
            CreatedSourceElement(
                source = source.copy(
                    index = tangentIndex,
                    attributes = tangentAttributes,
                ),
                element = line,
            ),
        )
    }

    private fun nestedAttributes(
        attributes: JsonObject,
        name: String,
        defaults: Map<String, JsonElement>,
    ): JsonObject {
        val effective = linkedMapOf<String, JsonElement>()
        effective.putAll(defaults)
        val nested = attributes[name] as? JsonObject
        if (nested != null) {
            effective.putAll(nested)
        }
        return JsonObject(effective)
    }

    private fun invalidTangentToResult(
        location: JessieCodeAstLocation,
    ): GMResult<Nothing, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.InvalidAst(
                reason =
                    "Native tangentto creator returned an incomplete line.",
                location = location,
            ),
        )

    private fun invalidTangentToDocumentResult(
        source: ParsedObject,
    ): JsxGraphDocumentError.ElementCreation =
        JsxGraphDocumentError.ElementCreation(
            objectIndex = source.index,
            id = source.id,
            type = source.type,
            reason = "creator returned an incomplete tangentto line",
        )

    private class JessieCodeAttributeSnapshotter(
        private val maxDepth: Int,
        private val maxValues: Int,
        private val location: JessieCodeAstLocation,
        private val ignoredRootFunctionProperties: Set<String>,
        private val ignoredFunctionPropertiesByPath:
            Map<String, Set<String>>,
    ) {
        private var valueCount = 0
        private val activeContainers =
            mutableListOf<JessieCodeRuntimeValue>()

        fun snapshot(
            attributes: JessieCodeRuntimeValue.ObjectValue,
        ): GMResult<JsonObject, JessieCodeRuntimeError> =
            when (
                val result = objectValue(
                    value = attributes,
                    path = "attributes",
                    depth = 1,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(result.value)
                is GMResult.Err -> result
            }

        private fun objectValue(
            value: JessieCodeRuntimeValue.ObjectValue,
            path: String,
            depth: Int,
        ): GMResult<JsonObject, JessieCodeRuntimeError> {
            validateContainer(value, path, depth)?.let { error ->
                return GMResult.Err(error)
            }
            activeContainers += value
            val properties = linkedMapOf<String, JsonElement>()
            for ((name, child) in value.properties) {
                if (
                    child is JessieCodeRuntimeValue.FunctionValue &&
                    (
                        (
                            path == "attributes" &&
                                name in ignoredRootFunctionProperties
                            ) ||
                            name in (
                                ignoredFunctionPropertiesByPath[path]
                                    ?: emptySet()
                                )
                        )
                ) {
                    continue
                }
                when (
                    val result = jsonValue(
                        value = child,
                        path = "$path.$name",
                        depth = depth + 1,
                    )
                ) {
                    is GMResult.Ok -> {
                        result.value?.let { properties[name] = it }
                    }
                    is GMResult.Err -> {
                        activeContainers.removeAt(
                            activeContainers.lastIndex,
                        )
                        return result
                    }
                }
            }
            activeContainers.removeAt(activeContainers.lastIndex)
            return GMResult.Ok(JsonObject(properties))
        }

        private fun arrayValue(
            value: JessieCodeRuntimeValue.ArrayValue,
            path: String,
            depth: Int,
        ): GMResult<JsonArray, JessieCodeRuntimeError> {
            validateContainer(value, path, depth)?.let { error ->
                return GMResult.Err(error)
            }
            activeContainers += value
            val children = mutableListOf<JsonElement>()
            for ((index, child) in value.values.withIndex()) {
                when (
                    val result = jsonValue(
                        value = child,
                        path = "$path[$index]",
                        depth = depth + 1,
                    )
                ) {
                    is GMResult.Ok ->
                        children += result.value ?: JsonNull
                    is GMResult.Err -> {
                        activeContainers.removeAt(
                            activeContainers.lastIndex,
                        )
                        return result
                    }
                }
            }
            activeContainers.removeAt(activeContainers.lastIndex)
            return GMResult.Ok(JsonArray(children))
        }

        private fun jsonValue(
            value: JessieCodeRuntimeValue,
            path: String,
            depth: Int,
        ): GMResult<JsonElement?, JessieCodeRuntimeError> {
            valueCount += 1
            if (valueCount > maxValues) {
                return GMResult.Err(
                    JessieCodeRuntimeError.ResourceLimitExceeded(
                        resource = "scene attribute value count",
                        limit = maxValues,
                        requestedSize = valueCount.toLong(),
                        location = location,
                    ),
                )
            }
            return when (value) {
                JessieCodeRuntimeValue.UndefinedValue ->
                    GMResult.Ok(null)
                JessieCodeRuntimeValue.NullValue ->
                    GMResult.Ok(JsonNull)
                is JessieCodeRuntimeValue.BooleanValue ->
                    GMResult.Ok(JsonPrimitive(value.value))
                is JessieCodeRuntimeValue.StringValue ->
                    GMResult.Ok(JsonPrimitive(value.value))
                is JessieCodeRuntimeValue.NumberValue ->
                    if (value.value.isFinite()) {
                        GMResult.Ok(JsonPrimitive(value.value))
                    } else {
                        unsupported(path, "non-finite number")
                    }
                is JessieCodeRuntimeValue.ArrayValue ->
                    arrayValue(value, path, depth)
                is JessieCodeRuntimeValue.ObjectValue ->
                    objectValue(value, path, depth)
                is JessieCodeRuntimeValue.FunctionValue ->
                    unsupported(path, "function")
                is JessieCodeRuntimeValue.BoardReference ->
                    unsupported(path, "board")
                is JessieCodeRuntimeValue.TransformationReference ->
                    unsupported(path, "transformation")
                is JessieCodeRuntimeValue.CompositionReference ->
                    unsupported(path, "composition")
                is JessieCodeRuntimeValue.ElementReference ->
                    unsupported(path, "element")
            }
        }

        private fun validateContainer(
            value: JessieCodeRuntimeValue,
            path: String,
            depth: Int,
        ): JessieCodeRuntimeError? =
            when {
                depth > maxDepth ->
                    JessieCodeRuntimeError.ResourceLimitExceeded(
                        resource = "scene attribute depth",
                        limit = maxDepth,
                        requestedSize = depth.toLong(),
                        location = location,
                    )
                activeContainers.any { it === value } ->
                    JessieCodeRuntimeError.UnsupportedSceneAttribute(
                        path = path,
                        valueType = "cyclic container",
                        location = location,
                    )
                else -> null
            }

        private fun <T> unsupported(
            path: String,
            valueType: String,
        ): GMResult<T, JessieCodeRuntimeError> =
            GMResult.Err(
                JessieCodeRuntimeError.UnsupportedSceneAttribute(
                    path = path,
                    valueType = valueType,
                    location = location,
                ),
            )
    }

    fun parse(
        source: String,
        limits: JsxGraphEngineLimits = JsxGraphEngineLimits(),
    ): GMResult<JsxGraphScene, JsxGraphDocumentError> =
        when (val result = createSession(source, limits)) {
            is GMResult.Ok -> GMResult.Ok(result.value.scene)
            is GMResult.Err -> result
        }

    fun createSession(
        source: String,
        limits: JsxGraphEngineLimits = JsxGraphEngineLimits(),
    ): GMResult<JsxGraphSession, JsxGraphDocumentError> {
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
        return createSession(document, limits)
    }

    private fun createSession(
        document: ParsedDocument,
        limits: JsxGraphEngineLimits,
    ): GMResult<JsxGraphSession, JsxGraphDocumentError> {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "jxgBoard",
            boundingBox = doubleArrayOf(
                document.boundingBox.left,
                document.boundingBox.top,
                document.boundingBox.right,
                document.boundingBox.bottom,
            ),
            defaultCurveMinimum =
                document.boundingBox.left -
                    (
                        document.boundingBox.right -
                            document.boundingBox.left
                        ) * CURVE_DOMAIN_PADDING,
            defaultCurveMaximum =
                document.boundingBox.right +
                    (
                        document.boundingBox.right -
                            document.boundingBox.left
                        ) * CURVE_DOMAIN_PADDING,
        )
        val created = mutableListOf<CreatedSourceElement>()

        for (sourceObject in document.objects) {
            if (sourceObject.type == "bisectorlines") {
                return GMResult.Err(
                    JsxGraphDocumentError.UnsupportedElementType(
                        objectIndex = sourceObject.index,
                        id = sourceObject.id,
                        type = sourceObject.type,
                    ),
                )
            }
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
            if (sourceObject.type == "tangentto") {
                val line = element as? Line
                    ?: return GMResult.Err(
                        invalidTangentToDocumentResult(sourceObject),
                    )
                val expanded = tangentToCreatedSourceElements(
                    source = sourceObject,
                    line = line,
                    polarIndex = sourceObject.index,
                    pointIndex = sourceObject.index,
                    tangentIndex = sourceObject.index,
                ) ?: return GMResult.Err(
                    invalidTangentToDocumentResult(sourceObject),
                )
                created += expanded
            } else {
                created += CreatedSourceElement(sourceObject, element)
            }
        }

        board.fullUpdate()
        val scene = when (
            val result = snapshotScene(
                document = document,
                created = created,
                maxCurvePoints = limits.maxCurvePoints,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val scenePoints = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Point>()
            .associateBy(JsxGraphSceneElement.Point::id)
        val sessionPoints = linkedMapOf<String, SessionPoint>()
        for (sourceElement in created) {
            val point = sourceElement.element as? Point ?: continue
            val scenePoint = scenePoints[sourceElement.source.id] ?: continue
            sessionPoints[sourceElement.source.id] = SessionPoint(
                point = point,
                draggable = scenePoint.draggable,
            )
        }
        return GMResult.Ok(
            JsxGraphSession(
                board = board,
                points = sessionPoints,
                initialScene = scene,
                snapshotScene = {
                    snapshotScene(
                        document = document,
                        created = created,
                        maxCurvePoints = limits.maxCurvePoints,
                    )
                },
            ),
        )
    }

    private fun snapshotScene(
        document: ParsedDocument,
        created: List<CreatedSourceElement>,
        maxCurvePoints: Int? = null,
    ): GMResult<JsxGraphScene, JsxGraphDocumentError> {
        val sceneElements = mutableListOf<JsxGraphSceneElement>()
        for (sourceElement in created) {
            val curve = sourceElement.element as? Curve
            if (
                curve != null &&
                maxCurvePoints != null &&
                curve.numberPoints > maxCurvePoints
            ) {
                return GMResult.Err(
                    JsxGraphDocumentError.CurvePointLimitExceeded(
                        objectIndex = sourceElement.source.index,
                        id = sourceElement.source.id,
                        limit = maxCurvePoints,
                        actual = curve.numberPoints,
                    ),
                )
            }
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
            is Point -> when (
                val result = pointSceneElement(
                    element = element,
                    attributes = attributes,
                    style = style,
                    respectFixedAttribute = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }

            is Line -> {
                element.fixedLengthEvaluationError?.let { error ->
                    return GMResult.Err(
                        JsxGraphDocumentError.ElementCreation(
                            objectIndex = source.index,
                            id = source.id,
                            type = source.type,
                            reason = error.toString(),
                        ),
                    )
                }
                val lineEndpoints = lineEndpoints(element)
                    ?: return GMResult.Err(attributes.nonFiniteGeometry())
                val (point1, point2) = lineEndpoints
                val isSegment = source.type == "segment"
                val isArrow =
                    source.type == "arrow" ||
                        source.type == "arrowparallel"
                val allowsCollapsed =
                    isSegment || source.type == "perpendicularsegment"
                if (!allowsCollapsed && point1 == point2) {
                    return GMResult.Err(attributes.nonFiniteGeometry())
                }
                // JSXGraph: src/base/line.js -> createSegment. The factory
                // overwrites both attributes even when callers pass true.
                // src/element/composition.js -> createPerpendicularSegment
                // instead calls createLine and inherits false defaults while
                // preserving explicitly supplied values.
                val straightDefault =
                    source.type != "perpendicularsegment"
                val straightFirst =
                    if (isSegment || isArrow) {
                        false
                    } else {
                        when (
                            val result = attributes.boolean(
                                name = "straightfirst",
                                default = straightDefault,
                            )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                    }
                val straightLast =
                    if (isSegment || isArrow) {
                        false
                    } else {
                        when (
                            val result = attributes.boolean(
                                name = "straightlast",
                                default = straightDefault,
                            )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                    }
                val firstArrow = when (
                    val result = attributes.arrowHead(
                        name = "firstarrow",
                        default = null,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val lastArrow = when (
                    val result = attributes.arrowHead(
                        name = "lastarrow",
                        default =
                            if (isArrow) {
                                DEFAULT_ARROW_HEAD
                            } else {
                                null
                            },
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                // JSXGraph 1.13.3:
                // src/math/geometry.js -> calcStraight. Ideal endpoint
                // weights override the visible straightFirst/straightLast
                // properties while delimiting the rendered line.
                val renderStraightFirst =
                    straightFirst || abs(element.point1.Z()) <= Mat.eps
                val renderStraightLast =
                    straightLast || abs(element.point2.Z()) <= Mat.eps
                JsxGraphSceneElement.Line(
                    id = element.id,
                    name = element.name,
                    style = style,
                    point1 = point1,
                    point2 = point2,
                    straightFirst = renderStraightFirst,
                    straightLast = renderStraightLast,
                    firstArrow = firstArrow,
                    lastArrow = lastArrow,
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
                val autoRadiusAngle =
                    if (element.usesAutoRadius) {
                        val first = point(element.point2)
                            ?: return GMResult.Err(
                                attributes.nonFiniteGeometry(),
                            )
                        val vertex = point(element.point1)
                            ?: return GMResult.Err(
                                attributes.nonFiniteGeometry(),
                            )
                        val third = point(element.point3)
                            ?: return GMResult.Err(
                                attributes.nonFiniteGeometry(),
                            )
                        JsxGraphAutoRadiusAngle(
                            first = first,
                            vertex = vertex,
                            third = third,
                            sign = element.directionSign(),
                        )
                    } else {
                        null
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
                        autoRadiusAngle = autoRadiusAngle,
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
                if (element.isInequality) {
                    when (
                        val result = attributes.boolean(
                            name = "inverse",
                            default = false,
                        )
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return result
                    }
                }
                when (
                    val result = curveSceneElement(
                        element = element,
                        points = element.points,
                        bezierDegree = element.bezierDegree,
                        style = style,
                        attributes = attributes,
                        allowFill =
                            element.isBooleanComposition ||
                                element.isRiemannSum ||
                                element.isBoxPlot ||
                                element.isInequality,
                        allowPathBreaks = true,
                        boxPlot = element.boxPlotSnapshot()?.let { boxPlot ->
                            JsxGraphBoxPlot(
                                quantiles = boxPlot.quantiles.toList(),
                                outliers = boxPlot.outliers?.toList(),
                                axis = boxPlot.axis,
                                width = boxPlot.width,
                                direction = boxPlot.direction,
                                smallWidth = boxPlot.smallWidth,
                                outlierFace = boxPlot.outlierFace,
                                outlierSize = boxPlot.outlierSize,
                            )
                        },
                        vectorField =
                            element.vectorFieldSnapshot()?.let { vectorField ->
                                JsxGraphVectorField(
                                    vectors = vectorField.vectors.map { vector ->
                                        JsxGraphVectorFieldVector(
                                            start = JsxGraphPoint2D(
                                                x = vector.startX,
                                                y = vector.startY,
                                            ),
                                            end = JsxGraphPoint2D(
                                                x = vector.endX,
                                                y = vector.endY,
                                            ),
                                        )
                                    },
                                    arrowEnabled = vectorField.arrowEnabled,
                                    arrowSize = vectorField.arrowSize,
                                    arrowAngle = vectorField.arrowAngle,
                                )
                            },
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }

            is Polygon -> {
                val isClosed = element.elType != "polygonalchain"
                val regularPolygonVertexAttributes =
                    if (element.elType == "regularpolygon") {
                        when (val result = attributes.nested("vertices")) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                    } else {
                        null
                    }
                val vertexCount =
                    (
                        element.vertices.size -
                            if (isClosed) 1 else 0
                        ).coerceAtLeast(0)
                val vertices = element.vertices
                    .take(vertexCount)
                    .mapNotNull(::point)
                if (vertices.size != vertexCount) {
                    return GMResult.Err(attributes.nonFiniteGeometry())
                }
                val implicitVertices = element.implicitVertices.map { vertex ->
                    val coordinates = point(vertex)
                        ?: return GMResult.Err(
                            attributes.nonFiniteGeometry(),
                        )
                    if (vertex === element.parallelPoint) {
                        val parallelPointAttributes = when (
                            val result = attributes.nested("parallelpoint")
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                        when (
                            val result =
                                parallelPointAttributes.validateSupported(
                                    vertex,
                                )
                        ) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                        val parallelPointStyle = when (
                            val result =
                                parallelPointAttributes.style(vertex)
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                        val withLabel = when (
                            val result = parallelPointAttributes.boolean(
                                name = "withlabel",
                                default = false,
                            )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                        if (withLabel && vertex.name.isNotEmpty()) {
                            return GMResult.Err(
                                parallelPointAttributes.unsupportedValue(
                                    attribute = "withLabel",
                                    value = "true with non-empty name",
                                ),
                            )
                        }
                        when (
                            val result = pointSceneElement(
                                element = vertex,
                                attributes = parallelPointAttributes,
                                style = parallelPointStyle,
                                respectFixedAttribute = false,
                            )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                    } else if (regularPolygonVertexAttributes != null) {
                        when (
                            val result =
                                regularPolygonVertexAttributes
                                    .validateSupported(
                                        element = vertex,
                                        additionalAttributes = setOf("ids"),
                                    )
                        ) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                        val vertexStyle = when (
                            val result =
                                regularPolygonVertexAttributes.style(vertex)
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                        val withLabel = when (
                            val result =
                                regularPolygonVertexAttributes.boolean(
                                    name = "withlabel",
                                    default = true,
                                )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                        if (withLabel && vertex.name.isNotEmpty()) {
                            return GMResult.Err(
                                regularPolygonVertexAttributes
                                    .unsupportedValue(
                                        attribute = "withLabel",
                                        value = "true with non-empty name",
                                    ),
                            )
                        }
                        when (
                            val result = pointSceneElement(
                                element = vertex,
                                attributes =
                                    regularPolygonVertexAttributes,
                                style = vertexStyle,
                                respectFixedAttribute =
                                    vertex in element.ownedVertices,
                            )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                    } else {
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
                                layer = DEFAULT_POINT_LAYER,
                            ),
                            coordinates = coordinates,
                            size = 3.0,
                            face = "o",
                        )
                    }
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
                        layer = DEFAULT_POLYGON_BORDER_LAYER,
                    ),
                    withLines = element.withLines,
                    isClosed = isClosed,
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

    private fun pointSceneElement(
        element: Point,
        attributes: AttributeReader,
        style: JsxGraphElementStyle,
        respectFixedAttribute: Boolean,
    ): GMResult<JsxGraphSceneElement.Point, JsxGraphDocumentError> {
        if (element is IntersectionPoint) {
            element.intersectionEvaluationError?.let { error ->
                return GMResult.Err(
                    attributes.elementCreation(error.toString()),
                )
            }
        }
        val isReal = element.isReal
        val coordinates =
            if (isReal) {
                point(element)
                    ?: return GMResult.Err(
                        attributes.nonFiniteGeometry(),
                    )
            } else if (element.type == Const.OBJECT_TYPE_INTERSECTION) {
                JsxGraphPoint2D(
                    x = element.X(),
                    y = element.Y(),
                )
            } else {
                return GMResult.Err(attributes.nonFiniteGeometry())
            }
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
        val fixed =
            if (respectFixedAttribute) {
                when (
                    val result = attributes.boolean(
                        name = "fixed",
                        default = false,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            } else {
                element.isFixed
            }
        return GMResult.Ok(
            JsxGraphSceneElement.Point(
                id = element.id,
                name = element.name,
                style = style,
                coordinates = coordinates,
                size = size,
                face = face,
                draggable =
                    element.isDraggable &&
                        !fixed &&
                        style.visible &&
                        isReal,
                isReal = isReal,
            ),
        )
    }

    private fun curveSceneElement(
        element: GeometryElement,
        points: List<com.swithun.jsxgraph.core.base.Coords>,
        bezierDegree: Int,
        style: JsxGraphElementStyle,
        attributes: AttributeReader,
        allowFill: Boolean,
        allowPathBreaks: Boolean,
        autoRadiusAngle: JsxGraphAutoRadiusAngle? = null,
        boxPlot: JsxGraphBoxPlot? = null,
        vectorField: JsxGraphVectorField? = null,
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
                autoRadiusAngle = autoRadiusAngle,
                boxPlot = boxPlot,
                vectorField = vectorField,
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
        val sceneObjectCount = objects.sumOf { sourceObject ->
            sceneElementCount(sourceObject.type).toLong()
        }
        if (sceneObjectCount > limits.maxObjects) {
            return GMResult.Err(
                JsxGraphDocumentError.ObjectLimitExceeded(
                    limit = limits.maxObjects,
                    actual = sceneObjectCount
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                ),
            )
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
            setOf(
                "curve",
                "functiongraph",
                "plot",
                "stepfunction",
                "derivative",
                "spline",
                "cardinalspline",
                "riemannsum",
                "boxplot",
                "comb",
                "inequality",
                "vectorfield",
                "slopefield",
                "ellipse",
                "hyperbola",
            )
        ) {
            return GMResult.Ok(Unit)
        }
        val dataPointCount = if (
            sourceObject.type in setOf("curve", "stepfunction") &&
            sourceObject.parents.size == 2
        ) {
            (sourceObject.parents.firstOrNull() as? JsonArray)?.size?.let {
                if (sourceObject.type == "stepfunction") {
                    stepFunctionPointCount(it.toLong()).toInt()
                } else {
                    it
                }
            }
        } else {
            null
        }
        val requested = if (sourceObject.type == "inequality") {
            0
        } else if (
            sourceObject.type == "vectorfield" ||
            sourceObject.type == "slopefield"
        ) {
            val xSteps = (
                (
                    sourceObject.parents.getOrNull(1) as?
                        JsonArray
                    )?.getOrNull(1) as? JsonPrimitive
                )?.doubleOrNull
            val ySteps = (
                (
                    sourceObject.parents.getOrNull(2) as?
                        JsonArray
                    )?.getOrNull(1) as? JsonPrimitive
                )?.doubleOrNull
            val arrowEnabled = (
                (
                    (
                        sourceObject.attributes["arrowhead"] as?
                            JsonObject
                        )?.get("enabled") as? JsonPrimitive
                    )?.booleanOrNull
                ) ?: (sourceObject.type == "vectorfield")
            if (xSteps == null || ySteps == null) {
                0
            } else {
                Curve.vectorFieldPointCount(
                    xSteps = xSteps,
                    ySteps = ySteps,
                    arrowEnabled = arrowEnabled,
                ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }
        } else if (sourceObject.type == "comb") {
            val first = jsonCurveLimitPoint(
                sourceObject.parents.getOrNull(0),
            )
            val second = jsonCurveLimitPoint(
                sourceObject.parents.getOrNull(1),
            )
            val frequency = (
                sourceObject.attributes["frequency"] as?
                    JsonPrimitive
                )?.doubleOrNull ?: Curve.COMB_DEFAULT_FREQUENCY
            if (
                first == null ||
                second == null ||
                !frequency.isFinite() ||
                frequency <= 0.0
            ) {
                0
            } else {
                Curve.combPointCount(
                    distance = curveLimitDistance(first, second),
                    frequency = frequency,
                ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }
        } else if (sourceObject.type == "riemannsum") {
            val rectangleCount = (
                sourceObject.parents.getOrNull(1) as?
                    JsonPrimitive
                )?.doubleOrNull
            val type = (
                sourceObject.parents.getOrNull(2) as?
                    JsonPrimitive
                )?.takeIf(JsonPrimitive::isString)
                ?.content
            if (rectangleCount == null || type == null) {
                0
            } else {
                Curve.riemannPointCount(
                    rectangleCount = rectangleCount,
                    type = type,
                    hasLowerFunction = (
                        sourceObject.parents.firstOrNull() as?
                            JsonArray
                        )?.size == 2,
                ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }
        } else if (sourceObject.type == "boxplot") {
            val quantiles = sourceObject.parents.firstOrNull() as? JsonArray
            val outlierCount = (
                quantiles?.getOrNull(Curve.BOX_PLOT_QUANTILE_COUNT) as?
                    JsonArray
                )?.size
            val outlierFace = (
                (
                    sourceObject.attributes["outlier"] as? JsonObject
                    )?.get("face") as? JsonPrimitive
                )?.takeIf(JsonPrimitive::isString)?.content ?: "o"
            boxPlotPointCount(
                outlierCount = outlierCount,
                outlierFace = outlierFace,
            ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } else {
            dataPointCount ?: (
                sourceObject.attributes["numberpointshigh"]
                    as? JsonPrimitive
                )?.intOrNull ?: Curve.DEFAULT_SAMPLE_COUNT
        }
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

    private fun stepFunctionPointCount(sourceCount: Long): Long =
        if (sourceCount == 0L) 0L else sourceCount * 2L - 1L

    private fun runtimeCurveLimitPoint(
        value: JessieCodeRuntimeValue,
    ): CurveLimitPoint? {
        val coordinates = (
            value as? JessieCodeRuntimeValue.ArrayValue
            )?.values ?: return null
        return curveLimitPoint(
            coordinates.map { coordinate ->
                (
                    coordinate as?
                        JessieCodeRuntimeValue.NumberValue
                    )?.value ?: return null
            },
        )
    }

    private fun jsonCurveLimitPoint(
        value: JsonElement?,
    ): CurveLimitPoint? {
        val coordinates = value as? JsonArray ?: return null
        return curveLimitPoint(
            coordinates.map { coordinate ->
                (
                    coordinate as? JsonPrimitive
                    )?.doubleOrNull ?: return null
            },
        )
    }

    private fun curveLimitPoint(
        coordinates: List<Double>,
    ): CurveLimitPoint? {
        if (coordinates.size < 2) {
            return null
        }
        if (coordinates.size == 2) {
            return CurveLimitPoint(
                weight = 1.0,
                x = coordinates[0],
                y = coordinates[1],
            )
        }
        val weight = coordinates[0]
        return if (abs(weight) > Mat.eps) {
            CurveLimitPoint(
                weight = 1.0,
                x = coordinates[1] / weight,
                y = coordinates[2] / weight,
            )
        } else {
            CurveLimitPoint(
                weight = weight,
                x = coordinates[1],
                y = coordinates[2],
            )
        }
    }

    private fun curveLimitDistance(
        first: CurveLimitPoint,
        second: CurveLimitPoint,
    ): Double =
        if (
            (first.weight - second.weight) *
                (first.weight - second.weight) >
            Mat.eps * Mat.eps
        ) {
            Double.POSITIVE_INFINITY
        } else {
            Mat.hypot(first.x - second.x, first.y - second.y)
        }

    private fun validatePolygonVertexLimit(
        sourceObject: ParsedObject,
        limit: Int,
    ): GMResult<Unit, JsxGraphDocumentError> {
        if (
            sourceObject.type != "polygon" &&
            sourceObject.type != "polygonalchain" &&
            sourceObject.type != "parallelogram" &&
            sourceObject.type != "regularpolygon"
        ) {
            return GMResult.Ok(Unit)
        }
        val numericCount = if (
            sourceObject.type == "regularpolygon" &&
            sourceObject.parents.size == 3
        ) {
            (
                sourceObject.parents.lastOrNull() as?
                    JsonPrimitive
                )?.doubleOrNull
        } else {
            null
        }
        val actual = if (
            numericCount != null &&
            numericCount.isFinite() &&
            numericCount >= 3.0
        ) {
            ceil(numericCount)
                .coerceAtMost(Int.MAX_VALUE.toDouble())
                .toInt()
        } else {
            sourceObject.parents.size
        }
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

    // JSXGraph: src/element/composition.js -> createParallel ideal point.
    // Compose consumes finite Cartesian endpoints, so preserve the ideal
    // point's direction while anchoring it at the finite line endpoint.
    private fun lineEndpoints(
        line: Line,
    ): Pair<JsxGraphPoint2D, JsxGraphPoint2D>? {
        val firstWeight = line.point1.Z()
        val secondWeight = line.point2.Z()
        if (!firstWeight.isFinite() || !secondWeight.isFinite()) {
            return null
        }
        val firstIsIdeal = abs(firstWeight) <= Mat.eps
        val secondIsIdeal = abs(secondWeight) <= Mat.eps
        if (!firstIsIdeal && !secondIsIdeal) {
            val first = point(line.point1) ?: return null
            val second = point(line.point2) ?: return null
            return first to second
        }
        if (firstIsIdeal == secondIsIdeal) {
            return null
        }

        val ideal = if (firstIsIdeal) line.point1 else line.point2
        val finite = if (firstIsIdeal) line.point2 else line.point1
        val anchor = point(finite) ?: return null
        val directionX = ideal.X()
        val directionY = ideal.Y()
        if (
            !directionX.isFinite() ||
            !directionY.isFinite() ||
            Mat.hypot(directionX, directionY) <= Mat.eps
        ) {
            return null
        }
        return if (firstIsIdeal) {
            JsxGraphPoint2D(
                x = anchor.x - directionX,
                y = anchor.y - directionY,
            ) to anchor
        } else {
            anchor to JsxGraphPoint2D(
                x = anchor.x + directionX,
                y = anchor.y + directionY,
            )
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

    private data class CurveLimitPoint(
        val weight: Double,
        val x: Double,
        val y: Double,
    )

    private class AttributeReader(
        private val source: ParsedObject,
        private val attributes: JsonObject = source.attributes,
        private val attributePrefix: String = "",
    ) {
        fun validateSupported(
            element: GeometryElement,
            additionalAttributes: Set<String> = emptySet(),
        ): GMResult<Unit, JsxGraphDocumentError> {
            val supported =
                COMMON_ATTRIBUTES +
                    when (element) {
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
                        is Curve ->
                            CURVE_ATTRIBUTES +
                                if (
                                    element.isEllipse ||
                                    element.isHyperbola
                                ) {
                                    CONIC_ATTRIBUTES
                                } else {
                                    emptySet()
                                } +
                                if (element.isBoxPlot) {
                                    BOX_PLOT_ATTRIBUTES
                                } else {
                                    emptySet()
                                } +
                                if (element.isComb) {
                                    COMB_ATTRIBUTES
                                } else {
                                    emptySet()
                                } +
                                if (element.isInequality) {
                                    INEQUALITY_SEMANTIC_ATTRIBUTES
                                } else {
                                    emptySet()
                                } +
                                if (element.isVectorField) {
                                    VECTOR_FIELD_SEMANTIC_ATTRIBUTES
                                } else {
                                    emptySet()
                                }
                        is Polygon ->
                            POLYGON_ATTRIBUTES +
                                when (element.elType) {
                                    "parallelogram" ->
                                        setOf("parallelpoint")
                                    "regularpolygon" ->
                                        setOf("vertices")
                                    else -> emptySet()
                                }
                        is Text -> TEXT_ATTRIBUTES
                        else -> emptySet()
                    } +
                    additionalAttributes
            attributes.keys.firstOrNull { it !in supported }?.let { name ->
                return GMResult.Err(
                    JsxGraphDocumentError.UnsupportedAttribute(
                        objectIndex = source.index,
                        id = source.id,
                        attribute = attributePath(name),
                    ),
                )
            }
            val nestedNames = when (element) {
                is Line -> listOf("point", "point1", "point2")
                is Circle -> listOf("center", "point2")
                is Arc -> listOf("center", "radiuspoint", "anglepoint")
                is Sector ->
                    listOf("center", "radiuspoint", "anglepoint", "arc")
                is Curve ->
                    when {
                        element.isComb -> listOf("point1", "point2")
                        element.isEllipse || element.isHyperbola ->
                            listOf("foci", "center")
                        else -> emptyList()
                    }
                else -> emptyList()
            }
            for (name in nestedNames) {
                when (
                    val result = validateHiddenSubElement(
                        name = name,
                        supportsIdentity =
                            element is Line &&
                                element.elType in setOf(
                                    "radicalaxis",
                                    "tangent",
                                    "tangentto",
                                    "polarline",
                                    "normal",
                                ) ||
                                element is Curve &&
                                (
                                    element.isComb ||
                                        element.isEllipse ||
                                        element.isHyperbola
                                    ),
                        supportsFixed =
                            element is Curve &&
                                (
                                    element.isComb ||
                                        element.isEllipse
                                    ),
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (element is Curve && element.isBoxPlot) {
                when (
                    val result = validateNestedAttributes(
                        name = "outlier",
                        supported = setOf("face", "size"),
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (element is Curve && element.isVectorField) {
                when (
                    val result = validateNestedAttributes(
                        name = "arrowhead",
                        supported = VECTOR_FIELD_ARROW_HEAD_ATTRIBUTES,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            return GMResult.Ok(Unit)
        }

        fun nested(
            name: String,
        ): GMResult<AttributeReader, JsxGraphDocumentError> {
            val value = attributes[name]
                ?: return GMResult.Ok(
                    AttributeReader(
                        source = source,
                        attributes = JsonObject(emptyMap()),
                        attributePrefix = attributePath(name),
                    ),
                )
            if (value === JsonNull) {
                return GMResult.Ok(
                    AttributeReader(
                        source = source,
                        attributes = JsonObject(emptyMap()),
                        attributePrefix = attributePath(name),
                    ),
                )
            }
            val nested = value as? JsonObject
                ?: return invalid(name, "an object")
            return GMResult.Ok(
                AttributeReader(
                    source = source,
                    attributes = nested,
                    attributePrefix = attributePath(name),
                ),
            )
        }

        fun style(
            element: GeometryElement,
        ): GMResult<JsxGraphElementStyle, JsxGraphDocumentError> {
            val defaultStroke = when (element) {
                is Point -> DEFAULT_POINT_COLOR
                is Text -> DEFAULT_TEXT_COLOR
                is Curve ->
                    when {
                        element.isInequality -> JsxGraphColor.Transparent
                        element.isComb -> DEFAULT_COMB_STROKE_COLOR
                        else -> DEFAULT_STROKE_COLOR
                    }
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
                is Curve ->
                    when {
                        element.isRiemannSum ->
                            DEFAULT_RIEMANN_FILL_COLOR
                        element.isBoxPlot ->
                            DEFAULT_STROKE_COLOR
                        element.isInequality ->
                            DEFAULT_POINT_COLOR
                        else -> JsxGraphColor.Transparent
                    }
                is Polygon ->
                    if (element.elType == "polygonalchain") {
                        JsxGraphColor.Transparent
                    } else {
                        DEFAULT_POLYGON_FILL_COLOR
                    }
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
            val layer = when (
                val result = nonNegativeInteger(
                    name = "layer",
                    default = defaultLayer(element),
                )
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
                        when {
                            element is Curve && element.isBoxPlot -> 2.0
                            element is Curve &&
                                element.isVectorField -> 0.5
                            element is Curve ||
                                element is Arc ||
                                element is Sector ||
                                element is Polygon -> 1.0
                            else -> 2.0
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
                        if (
                            element is Polygon ||
                            element is Sector ||
                            element is Curve &&
                                (
                                    element.isRiemannSum ||
                                        element.isBoxPlot ||
                                        element.isInequality
                                    )
                        ) {
                            if (
                                element is Curve &&
                                (
                                    element.isBoxPlot ||
                                        element.isInequality
                                    )
                            ) {
                                0.2
                            } else {
                                0.3
                            }
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
                val result = nonNegativeInteger("dash", default = 0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (dash > DASH_PATTERNS.size) {
                return GMResult.Err(
                    unsupportedValue("dash", dash.toString()),
                )
            }
            val dashScale = when (
                val result = boolean("dashscale", default = false)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val dashFactor = if (dashScale) strokeWidth * 0.5 else 1.0
            val strokeDashPattern =
                if (dash == 0) {
                    emptyList()
                } else {
                    DASH_PATTERNS[dash - 1].map { length ->
                        length * dashFactor
                    }
                }
            if (element !is Line) {
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
            }
            return GMResult.Ok(
                JsxGraphElementStyle(
                    visible = visible,
                    strokeColor = strokeColor,
                    fillColor = fillColor,
                    strokeWidth = strokeWidth,
                    strokeOpacity = strokeOpacity,
                    fillOpacity = fillOpacity,
                    layer = layer,
                    strokeDashPattern = strokeDashPattern,
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

        // JSXGraph 1.13.3:
        // src/renderer/abstract.js -> getArrowHeadData.
        fun arrowHead(
            name: String,
            default: JsxGraphArrowHead?,
        ): GMResult<JsxGraphArrowHead?, JsxGraphDocumentError> {
            val value = attributes[name] ?: return GMResult.Ok(default)
            if (value is JsonPrimitive) {
                val enabled = value.booleanOrNull
                    ?: return invalid(name, "a boolean or arrow-head object")
                return GMResult.Ok(
                    if (enabled) {
                        JsxGraphArrowHead(
                            type = DEFAULT_ARROW_TYPE,
                            size = DEFAULT_ARROW_SIZE,
                            highlightSize = null,
                        )
                    } else {
                        null
                    },
                )
            }
            val arrow = value as? JsonObject
                ?: return invalid(name, "a boolean or arrow-head object")
            arrow.keys.firstOrNull {
                it !in ARROW_HEAD_ATTRIBUTES
            }?.let { nestedName ->
                return GMResult.Err(
                    JsxGraphDocumentError.UnsupportedAttribute(
                        objectIndex = source.index,
                        id = source.id,
                        attribute = attributePath("$name.$nestedName"),
                    ),
                )
            }
            val type = when (
                val result = arrowInteger(
                    arrow = arrow,
                    path = "$name.type",
                    name = "type",
                    default = DEFAULT_ARROW_TYPE,
                    minimum = MIN_ARROW_TYPE,
                    maximum = MAX_ARROW_TYPE,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val size = when (
                val result = arrowNumber(
                    arrow = arrow,
                    path = "$name.size",
                    name = "size",
                    default = DEFAULT_ARROW_SIZE,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val highlightSize = if ("highlightsize" in arrow) {
                when (
                    val result = arrowNumber(
                        arrow = arrow,
                        path = "$name.highlightsize",
                        name = "highlightsize",
                        default = DEFAULT_ARROW_SIZE,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            } else {
                null
            }
            return GMResult.Ok(
                JsxGraphArrowHead(
                    type = type,
                    size = size,
                    highlightSize = highlightSize,
                ),
            )
        }

        private fun arrowNumber(
            arrow: JsonObject,
            path: String,
            name: String,
            default: Double,
        ): GMResult<Double, JsxGraphDocumentError> {
            val value = arrow[name] ?: return GMResult.Ok(default)
            val number = (value as? JsonPrimitive)?.doubleOrNull
            if (number == null || !number.isFinite() || number < 0.0) {
                return invalid(path, "a finite number >= 0.0")
            }
            return GMResult.Ok(number)
        }

        private fun arrowInteger(
            arrow: JsonObject,
            path: String,
            name: String,
            default: Int,
            minimum: Int,
            maximum: Int,
        ): GMResult<Int, JsxGraphDocumentError> {
            val value = arrow[name] ?: return GMResult.Ok(default)
            val number = (value as? JsonPrimitive)?.doubleOrNull
            val rounded = number?.let(::round)
            if (
                number == null ||
                !number.isFinite() ||
                rounded == null ||
                abs(number - rounded) >= Mat.eps ||
                rounded < minimum.toDouble() ||
                rounded > maximum.toDouble()
            ) {
                return invalid(
                    path,
                    "an integer in $minimum..$maximum",
                )
            }
            return GMResult.Ok(rounded.toInt())
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

        // JSXGraph 1.13.3: src/options.js -> validateNotNegativeInteger.
        private fun nonNegativeInteger(
            name: String,
            default: Int,
        ): GMResult<Int, JsxGraphDocumentError> {
            val value = attributes[name] ?: return GMResult.Ok(default)
            val number = (value as? JsonPrimitive)?.doubleOrNull
            val rounded = number?.let(::round)
            if (
                number == null ||
                !number.isFinite() ||
                rounded == null ||
                abs(number - rounded) >= Mat.eps ||
                rounded < 0.0 ||
                rounded > Int.MAX_VALUE.toDouble()
            ) {
                return invalid(
                    name,
                    "a non-negative integer no greater than ${Int.MAX_VALUE}",
                )
            }
            return GMResult.Ok(rounded.toInt())
        }

        // JSXGraph 1.13.3: src/options.js -> Options.layer;
        // src/utils/type.js -> copyAttributes.
        private fun defaultLayer(element: GeometryElement): Int =
            when (element) {
                is Point -> DEFAULT_POINT_LAYER
                is Text -> DEFAULT_TEXT_LAYER
                is Arc -> DEFAULT_ARC_LAYER
                is Line -> DEFAULT_LINE_LAYER
                is Circle -> DEFAULT_CIRCLE_LAYER
                is Sector -> DEFAULT_AREA_LAYER
                is Curve -> DEFAULT_CURVE_LAYER
                is Polygon -> DEFAULT_AREA_LAYER
                else -> DEFAULT_ELEMENT_LAYER
            }

        fun unsupportedValue(
            attribute: String,
            value: String,
        ): JsxGraphDocumentError.UnsupportedAttributeValue =
            JsxGraphDocumentError.UnsupportedAttributeValue(
                objectIndex = source.index,
                id = source.id,
                attribute = attributePath(attribute),
                value = value,
            )

        fun nonFiniteGeometry(): JsxGraphDocumentError.NonFiniteGeometry =
            JsxGraphDocumentError.NonFiniteGeometry(
                objectIndex = source.index,
                id = source.id,
            )

        fun elementCreation(
            reason: String,
        ): JsxGraphDocumentError.ElementCreation =
            JsxGraphDocumentError.ElementCreation(
                objectIndex = source.index,
                id = source.id,
                type = source.type,
                reason = reason,
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
            supportsIdentity: Boolean,
            supportsFixed: Boolean = false,
        ): GMResult<Unit, JsxGraphDocumentError> {
            val value = attributes[name] ?: return GMResult.Ok(Unit)
            if (value === JsonNull) {
                return GMResult.Ok(Unit)
            }
            val nested = value as? JsonObject
                ?: return invalid(name, "an object")
            val supported =
                if (supportsIdentity) {
                    setOf(
                        "id",
                        "name",
                        "needsregularupdate",
                        "visible",
                        "withlabel",
                    ) + if (supportsFixed) setOf("fixed") else emptySet()
                } else {
                    setOf("visible")
                }
            nested.keys.firstOrNull { it !in supported }?.let { nestedName ->
                return GMResult.Err(
                    JsxGraphDocumentError.UnsupportedAttribute(
                        objectIndex = source.index,
                        id = source.id,
                        attribute = attributePath("$name.$nestedName"),
                    ),
                )
            }
            for (attribute in listOf("visible", "withlabel")) {
                val raw = nested[attribute] ?: continue
                val enabled = (raw as? JsonPrimitive)?.booleanOrNull
                    ?: return invalid("$name.$attribute", "a boolean")
                if (enabled) {
                    return GMResult.Err(
                        unsupportedValue("$name.$attribute", "true"),
                    )
                }
            }
            return GMResult.Ok(Unit)
        }

        private fun validateNestedAttributes(
            name: String,
            supported: Set<String>,
        ): GMResult<Unit, JsxGraphDocumentError> {
            val value = attributes[name] ?: return GMResult.Ok(Unit)
            val nested = value as? JsonObject
                ?: return invalid(name, "an object")
            nested.keys.firstOrNull { it !in supported }?.let {
                    nestedName ->
                return GMResult.Err(
                    JsxGraphDocumentError.UnsupportedAttribute(
                        objectIndex = source.index,
                        id = source.id,
                        attribute = attributePath("$name.$nestedName"),
                    ),
                )
            }
            return GMResult.Ok(Unit)
        }

        private fun <T> invalid(
            name: String,
            expected: String,
        ): GMResult<T, JsxGraphDocumentError> =
            GMResult.Err(
                JsxGraphDocumentError.InvalidAttribute(
                    objectIndex = source.index,
                    id = source.id,
                    attribute = attributePath(name),
                    expected = expected,
                ),
            )

        private fun attributePath(name: String): String =
            if (attributePrefix.isEmpty()) {
                name
            } else {
                "$attributePrefix.$name"
            }
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
    private const val CURVE_DOMAIN_PADDING = 0.1
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
    private val DEFAULT_RIEMANN_FILL_COLOR =
        JsxGraphColor(red = 240, green = 228, blue = 66)
    private val DEFAULT_COMB_STROKE_COLOR =
        JsxGraphColor(red = 0, green = 0, blue = 255)
    private const val MIN_ARROW_TYPE = 1
    private const val MAX_ARROW_TYPE = 7
    private const val DEFAULT_ARROW_TYPE = 1
    private const val DEFAULT_ARROW_SIZE = 6.0
    private val DEFAULT_ARROW_HEAD = JsxGraphArrowHead(
        type = DEFAULT_ARROW_TYPE,
        size = DEFAULT_ARROW_SIZE,
        highlightSize = DEFAULT_ARROW_SIZE,
    )
    private const val DEFAULT_ELEMENT_LAYER = 0
    private const val DEFAULT_AREA_LAYER = 3
    private const val DEFAULT_CURVE_LAYER = 5
    private const val DEFAULT_POLYGON_BORDER_LAYER = 5
    private const val DEFAULT_CIRCLE_LAYER = 6
    private const val DEFAULT_LINE_LAYER = 7
    private const val DEFAULT_ARC_LAYER = 8
    private const val DEFAULT_POINT_LAYER = 9
    private const val DEFAULT_TEXT_LAYER = 9
    // JSXGraph 1.13.3: src/renderer/abstract.js -> dashArray.
    private val DASH_PATTERNS = listOf(
        listOf(2.0, 2.0),
        listOf(5.0, 5.0),
        listOf(10.0, 10.0),
        listOf(20.0, 20.0),
        listOf(20.0, 10.0, 10.0, 10.0),
        listOf(20.0, 5.0, 10.0, 5.0),
        listOf(0.0, 5.0),
    )
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
        "layer",
        "fixed",
        "highlight",
        "withlabel",
        "dash",
        "dashscale",
    )
    private val POINT_ATTRIBUTES = setOf(
        "size",
        "face",
        "alwaysintersect",
        "precision",
    )
    private val LINE_ATTRIBUTES = setOf(
        "straightfirst",
        "straightlast",
        "firstarrow",
        "lastarrow",
        "nonnegativeonly",
        "point",
        "point1",
        "point2",
    )
    private val ARROW_HEAD_ATTRIBUTES = setOf(
        "type",
        "size",
        "highlightsize",
    )
    private val CIRCLE_ATTRIBUTES = setOf(
        "nonnegativeonly",
        "center",
        "point2",
    )
    private val CURVE_ATTRIBUTES = setOf(
        "doadvancedplot",
        "numberpointshigh",
        "firstarrow",
        "lastarrow",
        "linecap",
        "createpoints",
        "isarrayofcoordinates",
        "points",
    )
    private val CONIC_ATTRIBUTES = setOf("foci", "center")
    private val BOX_PLOT_ATTRIBUTES = setOf(
        "dir",
        "smallwidth",
        "outlier",
    )
    private val COMB_SEMANTIC_ATTRIBUTES = setOf(
        "frequency",
        "width",
        "angle",
        "reverse",
    )
    private val COMB_ATTRIBUTES =
        COMB_SEMANTIC_ATTRIBUTES + setOf("point1", "point2")
    private val INEQUALITY_SEMANTIC_ATTRIBUTES = setOf("inverse")
    private val VECTOR_FIELD_SEMANTIC_ATTRIBUTES =
        setOf("scale", "arrowhead")
    private val VECTOR_FIELD_ARROW_HEAD_ATTRIBUTES =
        setOf("enabled", "size", "angle")

    private fun sceneElementCount(creatorName: String): Int =
        when (creatorName) {
            "bisectorlines" -> 2
            "tangentto" -> 3
            else -> 1
        }

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
