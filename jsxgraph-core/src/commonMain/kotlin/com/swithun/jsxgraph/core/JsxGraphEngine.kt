/*
 * Kotlin translation support for JSXGraph.
 * Upstream: src/base/board.js -> create,
 * src/jxg.js -> registerElement,
 * src/base/element.js -> visual properties,
 * src/base/ticks.js -> createHatchmark,
 * src/element/comb.js -> createComb,
 * src/element/composition.js -> createInequality
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.AxisDistance
import com.swithun.jsxgraph.core.base.Axes3D
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Circle3D
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.Curve3D
import com.swithun.jsxgraph.core.base.Face3D
import com.swithun.jsxgraph.core.base.Face3DAttributes
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Hatch
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Line3D
import com.swithun.jsxgraph.core.base.Mesh3D
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Point3D
import com.swithun.jsxgraph.core.base.Plane3D
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.Polygon3D
import com.swithun.jsxgraph.core.base.Polyhedron3D
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.Sphere3D
import com.swithun.jsxgraph.core.base.Surface3D
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.base.Text3D
import com.swithun.jsxgraph.core.base.Ticks
import com.swithun.jsxgraph.core.base.TicksAnchor
import com.swithun.jsxgraph.core.base.TicksSource
import com.swithun.jsxgraph.core.base.Ticks3D
import com.swithun.jsxgraph.core.base.Transformation
import com.swithun.jsxgraph.core.base.View3D
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
import com.swithun.jsxgraph.core.utils.JsNumberFormat
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

    data class TickCountLimitExceeded(
        val objectIndex: Int,
        val id: String,
        val limit: Int,
        val actual: Int,
    ) : JsxGraphDocumentError {
        override val message: String =
            "objects[$objectIndex] '$id' tick count $actual exceeds limit $limit"
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
        point.setPosition(coordinates)
        board.update(draggedElement = point.element)
        return commitOrRollback(previous)
    }

    fun captureInteractionState(): JsxGraphInteractionState {
        val coordinates = linkedMapOf<String, JsxGraphPoint2D>()
        for ((id, handle) in points) {
            if (handle.draggable) {
                coordinates[id] = handle.coordinates()
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
        val resolved =
            mutableListOf<Pair<SessionPoint, JsxGraphPoint2D>>()
        for ((id, coordinates) in state.pointCoordinates) {
            when (val result = draggablePoint(id, coordinates)) {
                is GMResult.Ok -> resolved += result.value to coordinates
                is GMResult.Err -> return result
            }
        }
        val previous = captureBoardPointCoordinates()
        for ((point, coordinates) in resolved) {
            point.setPosition(coordinates)
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
    ): GMResult<SessionPoint, JsxGraphInteractionError> {
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
        return GMResult.Ok(handle)
    }

    private fun captureBoardPointCoordinates(): BoardPointCoordinates =
        BoardPointCoordinates(
            points2D = board.objectsList
                .filterIsInstance<Point>()
                .associateWith(Point::Coords),
            points3D = board.objectsList
                .filterIsInstance<Point3D>()
                .associateWith { it.coords.copyOf() },
        )

    private fun commitOrRollback(
        previous: BoardPointCoordinates,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> =
        when (val result = snapshotScene()) {
            is GMResult.Ok -> {
                scene = result.value
                result
            }
            is GMResult.Err -> {
                for ((point, coordinates) in previous.points3D) {
                    point.setPosition(coordinates)
                }
                for ((point, coordinates) in previous.points2D) {
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

internal sealed interface SessionPoint {
    val element: GeometryElement
    val draggable: Boolean

    fun coordinates(): JsxGraphPoint2D

    fun setPosition(coordinates: JsxGraphPoint2D)

    data class TwoDimensional(
        val point: Point,
        override val draggable: Boolean,
    ) : SessionPoint {
        override val element: GeometryElement
            get() = point

        override fun coordinates(): JsxGraphPoint2D =
            JsxGraphPoint2D(point.X(), point.Y())

        override fun setPosition(coordinates: JsxGraphPoint2D) {
            point.setPositionDirectly(
                method = Const.COORDS_BY_USER,
                coordinates =
                    doubleArrayOf(coordinates.x, coordinates.y),
            )
        }
    }

    data class ThreeDimensional(
        val point: Point3D,
        override val draggable: Boolean,
    ) : SessionPoint {
        override val element: GeometryElement
            get() = point

        override fun coordinates(): JsxGraphPoint2D {
            val projected = point.point2D.coords.usrCoords
            return JsxGraphPoint2D(projected[1], projected[2])
        }

        override fun setPosition(coordinates: JsxGraphPoint2D) {
            point.setPositionFrom2D(
                doubleArrayOf(coordinates.x, coordinates.y),
            )
        }
    }
}

private data class BoardPointCoordinates(
    val points2D: Map<Point, DoubleArray>,
    val points3D: Map<Point3D, DoubleArray>,
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
                val createsSceneElement =
                    creatorName !in NON_SCENE_CREATORS
                val createdSceneElementCount =
                    sceneElementCount(
                        creatorName = creatorName,
                        board = selectedBoard,
                        parents = parents,
                        attributes = attributes,
                    ).toLong()
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
                    board = selectedBoard,
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
                                "vectorfield",
                                "slopefield",
                                "vectorfield3d",
                                ->
                                    VECTOR_FIELD_SEMANTIC_ATTRIBUTES
                                else -> emptySet()
                            },
                        ignoredFunctionPropertiesByPath =
                            if (
                                creatorName == "vectorfield" ||
                                    creatorName == "slopefield" ||
                                    creatorName == "vectorfield3d"
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
                                if (
                                    creatorName in
                                    TRANSFORMATION_CREATORS
                                ) {
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
                                if (creatorName == "view3d") {
                                    val view = value.element as? View3D
                                    val axes = view?.defaultAxes
                                    if (view == null || axes == null) {
                                        view?.board?.removeObject(view)
                                        return@JessieCodeCreator invalidView3DDefaultAxesResult(
                                            location,
                                        )
                                    }
                                    val expanded =
                                        axes3DCreatedSourceElements(
                                            source = ParsedObject(
                                                index = creationCount,
                                                id = view.id,
                                                type = creatorName,
                                                parents =
                                                    JsonArray(emptyList()),
                                                attributes =
                                                    sourceAttributes,
                                            ),
                                            axes = axes,
                                        )
                                    created += expanded
                                    creationCount +=
                                        createdSceneElementCount(expanded)
                                    return@JessieCodeCreator result
                                }
                                if (!createsSceneElement) {
                                    return@JessieCodeCreator GMResult.Err(
                                        JessieCodeRuntimeError.InvalidAst(
                                            reason =
                                                "Native non-scene creator " +
                                                    "returned an " +
                                                    "unexpected element.",
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
                                if (creatorName == "polyhedron3d") {
                                    val polyhedron =
                                        value.element as? Polyhedron3D
                                    if (polyhedron == null) {
                                        return@JessieCodeCreator invalidPolyhedron3DResult(
                                            source = source,
                                            location = location,
                                        )
                                    }
                                    val expanded =
                                        polyhedron3DCreatedSourceElements(
                                            source = source,
                                            polyhedron = polyhedron,
                                        )
                                    created += expanded
                                    creationCount += expanded.size
                                } else if (
                                    creatorName == "parametricsurface3d" ||
                                    creatorName == "functiongraph3d"
                                ) {
                                    val surface =
                                        value.element as? Surface3D
                                    if (surface == null) {
                                        return@JessieCodeCreator GMResult.Err(
                                            JessieCodeRuntimeError.InvalidAst(
                                                reason =
                                                    "Native $creatorName " +
                                                        "creator returned an " +
                                                        "unexpected element.",
                                                location = location,
                                            ),
                                        )
                                    }
                                    created += CreatedSourceElement(
                                        source = source,
                                        element = surface,
                                    )
                                    val expanded =
                                        surface.polyhedron?.let {
                                                polyhedron,
                                            ->
                                            polyhedron3DCreatedSourceElements(
                                                source = source.copy(
                                                    index = source.index + 1,
                                                ),
                                                polyhedron = polyhedron,
                                            )
                                        }.orEmpty()
                                    created += expanded
                                    creationCount += 1 + expanded.size
                                } else if (creatorName == "plane3d") {
                                    val plane = value.element as? Plane3D
                                    if (plane == null) {
                                        return@JessieCodeCreator GMResult.Err(
                                            JessieCodeRuntimeError.InvalidAst(
                                                reason =
                                                    "Native plane3d creator " +
                                                        "returned an " +
                                                        "unexpected element.",
                                                location = location,
                                            ),
                                        )
                                    }
                                    created += CreatedSourceElement(
                                        source = source,
                                        element = plane,
                                    )
                                    val expanded =
                                        plane.surface3D?.let { surface ->
                                            polyhedron3DCreatedSourceElements(
                                                source = source.copy(
                                                    index = source.index + 1,
                                                ),
                                                polyhedron = surface,
                                            )
                                        }.orEmpty()
                                    created += expanded
                                    creationCount += 1 + expanded.size
                                } else if (creatorName == "tangentto") {
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
                                } else if (creatorName == "axis") {
                                    val line = value.element as? Line
                                    val expanded = line?.let {
                                        axisCreatedSourceElements(
                                            source = source,
                                            line = it,
                                        )
                                    }
                                    if (expanded == null) {
                                        return@JessieCodeCreator GMResult.Err(
                                            JessieCodeRuntimeError.InvalidAst(
                                                reason =
                                                    "Native axis creator " +
                                                        "returned an " +
                                                        "incomplete axis.",
                                                location = location,
                                            ),
                                        )
                                    }
                                    created += expanded
                                    creationCount += expanded.size
                                } else {
                                    created += CreatedSourceElement(
                                        source = source,
                                        element = value.element,
                                    )
                                    creationCount +=
                                        sceneElementOutputCount(value.element)
                                }
                                result
                            }
                            is JessieCodeRuntimeValue.CompositionReference -> {
                                if (!createsSceneElement) {
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
                                if (creatorName == "axes3d") {
                                    val axes = composition as? Axes3D
                                    if (axes == null) {
                                        return@JessieCodeCreator invalidAxes3DResult(location)
                                    }
                                    val expanded =
                                        axes3DCreatedSourceElements(
                                            source = ParsedObject(
                                                index = creationCount,
                                                id = "axes3d",
                                                type = creatorName,
                                                parents =
                                                    JsonArray(emptyList()),
                                                attributes =
                                                    sourceAttributes,
                                            ),
                                            axes = axes,
                                        )
                                    created += expanded
                                    creationCount +=
                                        createdSceneElementCount(expanded)
                                    return@JessieCodeCreator result
                                }
                                if (creatorName != "bisectorlines") {
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
                .flatMap { sourceElement ->
                    sourceCurvePointUsages(
                        sourceElement.element,
                    ).asSequence()
                }
                .map(CurvePointUsage::requested)
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
                    val pointElement = (
                        created.lastOrNull { sourceElement ->
                            sourceElement.element.id == id &&
                                board.elementById(id) ===
                                sourceElement.element
                        }
                    )?.element
                        ?: return@movePoint GMResult.Err(
                            JsxGraphInteractionError.UnknownPoint(id),
                        )
                    val point = when (pointElement) {
                        is Point -> SessionPoint.TwoDimensional(
                            point = pointElement,
                            draggable =
                                pointElement.isDraggable &&
                                    !pointElement.isFixed,
                        )
                        is Point3D -> SessionPoint.ThreeDimensional(
                            point = pointElement,
                            draggable =
                                pointElement.point2D.isDraggable &&
                                    !pointElement.isFixed,
                        )
                        else -> return@movePoint GMResult.Err(
                            JsxGraphInteractionError.UnknownPoint(id),
                        )
                    }
                    if (!point.draggable) {
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
                    val previous = BoardPointCoordinates(
                        points2D = board.objectsList
                            .filterIsInstance<Point>()
                            .associateWith(Point::Coords),
                        points3D = board.objectsList
                            .filterIsInstance<Point3D>()
                            .associateWith { it.coords.copyOf() },
                    )
                    point.setPosition(coordinates)
                    board.update(draggedElement = point.element)
                    val limitError = dynamicCurveLimitError()
                    if (limitError != null) {
                        for ((previousPoint, previousCoordinates) in
                            previous.points3D
                        ) {
                            previousPoint.setPosition(previousCoordinates)
                        }
                        for ((previousPoint, previousCoordinates) in
                            previous.points2D
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
                                previous.points3D
                            ) {
                                previousPoint.setPosition(
                                    previousCoordinates,
                                )
                            }
                            for ((previousPoint, previousCoordinates) in
                                previous.points2D
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
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        limits: JsxGraphJessieCodeLimits,
        location: JessieCodeAstLocation,
    ): JessieCodeRuntimeError.ResourceLimitExceeded? {
        val requestedTickCount =
            when (creatorName) {
                "ticks" ->
                (
                    parents.getOrNull(1) as?
                        JessieCodeRuntimeValue.ArrayValue
                    )?.values?.size?.toLong()
                "axis" -> runtimeAxisFixedTickCount(attributes)
                "hatch", "hash" ->
                    (
                        parents.getOrNull(1) as?
                            JessieCodeRuntimeValue.NumberValue
                        )?.value?.let(Hatch::positionCount)
                else -> null
            }
        if (
            requestedTickCount != null &&
            requestedTickCount > Ticks.DEFAULT_MAXIMUM_TICK_COUNT
        ) {
            return JessieCodeRuntimeError.ResourceLimitExceeded(
                resource = "tick count",
                limit = Ticks.DEFAULT_MAXIMUM_TICK_COUNT,
                requestedSize = requestedTickCount,
                location = location,
            )
        }
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
            creatorName == "polyhedron3d" ->
                runtimePolyhedron3DMaximumCurvePointCount(
                    board = board,
                    parents = parents,
                )
            creatorName == "mesh3d" ->
                runtimeMeshPointCount(
                    rangeU = parents.getOrNull(4),
                    rangeV = parents.getOrNull(5),
                    attributes = attributes,
                )
            creatorName == "plane3d" ->
                runtimePlaneMaximumCurvePointCount(
                    board = board,
                    parents = parents,
                    attributes = attributes,
                )
            creatorName == "parametricsurface3d" ||
                creatorName == "functiongraph3d" ->
                runtimeSurface3DMaximumCurvePointCount(attributes)
            creatorName == "ticks3d" -> {
                val length = (
                    parents.getOrNull(3) as?
                        JessieCodeRuntimeValue.NumberValue
                    )?.value
                val ticksDistance = when (
                    val value = attributes.properties["ticksdistance"]
                ) {
                    null,
                    JessieCodeRuntimeValue.UndefinedValue,
                    -> 1.0
                    is JessieCodeRuntimeValue.NumberValue -> value.value
                    else -> null
                }
                if (length == null || ticksDistance == null) {
                    null
                } else {
                    Ticks3D.curvePointCount(length, ticksDistance)
                }
            }
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
            creatorName == "vectorfield3d" ->
                runtimeVectorField3DPointCount(
                    parents = parents,
                    attributes = attributes,
                )
            creatorName == "curve3d" ||
                creatorName == "circle3d" ||
                creatorName == "intersectioncircle3d" ||
                creatorName == "sphere3d" ->
                runtimeCurve3DPointCount(
                    board = board,
                    parents = parents,
                    attributes = attributes,
                )
            creatorName in
                setOf(
                    "curve",
                    "ellipse",
                    "hyperbola",
                    "parabola",
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
            creatorName == "polygon3d" ->
                runtimePolygon3DVertexCount(
                    board = board,
                    parents = parents,
                )
            creatorName == "polyhedron3d" ->
                runtimePolyhedron3DMaximumFaceVertexCount(
                    board = board,
                    parents = parents,
                )
            creatorName == "plane3d" ->
                runtimePlaneMaximumFaceVertexCount(
                    board = board,
                    parents = parents,
                    attributes = attributes,
                )
            creatorName == "parametricsurface3d" ||
                creatorName == "functiongraph3d" ->
                runtimeSurface3DMaximumFaceVertexCount(attributes)
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
        if (creatorName == "text" || creatorName == "text3d") {
            val requestedTextLength = when (
                val content = parents.lastOrNull()
            ) {
                is JessieCodeRuntimeValue.StringValue ->
                    content.value.length
                is JessieCodeRuntimeValue.NumberValue ->
                    JsNumberFormat.compact(content.value).length
                else -> 0
            }
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

    // JSXGraph 1.13.3: src/base/line.js -> JXG.createAxis.
    private fun runtimeAxisFixedTickCount(
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Long? {
        val ticks = attributes.properties["ticks"] as?
            JessieCodeRuntimeValue.ObjectValue ?: return null
        if (
            ticks.properties["ticksdistance"] !==
            JessieCodeRuntimeValue.UndefinedValue
        ) {
            return null
        }
        return (
            ticks.properties["ticks"] as?
                JessieCodeRuntimeValue.ArrayValue
            )?.values?.size?.toLong()
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

    // JSXGraph 1.13.3: src/base/line.js -> createAxis defaultTicks.
    private fun axisCreatedSourceElements(
        source: ParsedObject,
        line: Line,
    ): List<CreatedSourceElement>? {
        val ticks = line.defaultTicks ?: return null
        return listOf(
            CreatedSourceElement(
                source = source,
                element = line,
            ),
            CreatedSourceElement(
                source = ParsedObject(
                    index = source.index + 1,
                    id = ticks.id,
                    type = "ticks",
                    parents = JsonArray(emptyList()),
                    attributes = axisTicksSourceAttributes(
                        source.attributes,
                    ),
                ),
                element = ticks,
            ),
        )
    }

    private fun axisTicksSourceAttributes(
        axisAttributes: JsonObject,
    ): JsonObject {
        val ticks = axisAttributes["ticks"] as? JsonObject
            ?: JsonObject(emptyMap())
        val sourceLabel = ticks["label"] as? JsonObject
            ?: JsonObject(emptyMap())
        val label = linkedMapOf<String, JsonElement>(
            "offset" to JsonArray(
                listOf(JsonPrimitive(4), JsonPrimitive(-9)),
            ),
            "visible" to JsonPrimitive("inherit"),
            "needsregularupdate" to JsonPrimitive(false),
            "layer" to JsonPrimitive(9),
        ).apply {
            putAll(sourceLabel)
        }
        val attributes = linkedMapOf<String, JsonElement>(
            "visible" to JsonPrimitive("inherit"),
            "needsregularupdate" to JsonPrimitive(false),
            "strokewidth" to JsonPrimitive(1),
            "strokecolor" to JsonPrimitive("#666666"),
            "drawlabels" to JsonPrimitive(true),
            "drawzero" to JsonPrimitive(false),
            "insertticks" to JsonPrimitive(true),
            "minticksdistance" to JsonPrimitive(5),
            "minorheight" to JsonPrimitive(10),
            "majorheight" to JsonPrimitive(-1),
            "tickendings" to JsonArray(
                listOf(JsonPrimitive(0), JsonPrimitive(1)),
            ),
            "majortickendings" to JsonArray(
                listOf(JsonPrimitive(1), JsonPrimitive(1)),
            ),
            "minorticks" to JsonPrimitive(4),
            "ticksdistance" to JsonPrimitive(1),
            "strokeopacity" to JsonPrimitive(0.25),
        ).apply {
            putAll(ticks)
            remove("ticks")
            this["label"] = JsonObject(label)
        }
        return JsonObject(attributes)
    }

    private fun axes3DCreatedSourceElements(
        source: ParsedObject,
        axes: Axes3D,
    ): List<CreatedSourceElement> = buildList {
        var offset = 0
        for (element in axes.objectsList) {
            val role = axes.memberRole(element) ?: element.id
            val attributes = axes3DMemberAttributes(
                source = source,
                role = role,
                element = element,
            )
            val memberSource = ParsedObject(
                index = source.index + offset,
                id = element.id,
                type = when {
                    element is Curve && element.isTicks3D -> "ticks3d"
                    element is Plane3D -> "plane3d"
                    element is Line3D -> "axis3d"
                    else -> element.elType
                },
                parents = JsonArray(emptyList()),
                attributes = attributes,
            )
            add(
                CreatedSourceElement(
                    source = memberSource,
                    element = element,
                ),
            )
            offset += sceneElementOutputCount(element)
            if (element is Plane3D) {
                element.surface3D?.let { surface ->
                    val expanded = polyhedron3DCreatedSourceElements(
                        source = memberSource.copy(
                            index = source.index + offset,
                        ),
                        polyhedron = surface,
                    )
                    addAll(expanded)
                    offset += expanded.size
                }
            }
        }
    }

    // JSXGraph: src/3d/polyhedron3d.js -> createPolyhedron3D.
    private fun polyhedron3DCreatedSourceElements(
        source: ParsedObject,
        polyhedron: Polyhedron3D,
    ): List<CreatedSourceElement> =
        polyhedron.faces.mapIndexed { offset, face ->
            CreatedSourceElement(
                source = source.copy(
                    index = source.index + offset,
                    id = face.id,
                    type = "face3d",
                    parents = JsonArray(emptyList()),
                    attributes = face3DSourceAttributes(face),
                ),
                element = face,
            )
        }

    private fun face3DSourceAttributes(face: Face3D): JsonObject {
        val attributes = face.faceAttributes
        val light = attributes.shader.light
        return JsonObject(
            linkedMapOf(
                "id" to JsonPrimitive(face.id),
                "name" to JsonPrimitive(face.name),
                "needsregularupdate" to
                    JsonPrimitive(attributes.needsRegularUpdate),
                "visible" to JsonPrimitive(attributes.visible),
                "strokecolor" to JsonPrimitive(attributes.strokeColor),
                "fillcolor" to JsonPrimitive(
                    face.resolvedFillColor(),
                ),
                "strokewidth" to JsonPrimitive(attributes.strokeWidth),
                "strokeopacity" to JsonPrimitive(attributes.strokeOpacity),
                "fillopacity" to JsonPrimitive(attributes.fillOpacity),
                "layer" to JsonPrimitive(attributes.layer),
                "fixed" to JsonPrimitive(attributes.fixed),
                "highlight" to JsonPrimitive(attributes.highlight),
                "withlabel" to JsonPrimitive(attributes.withLabel),
                "dash" to JsonPrimitive(attributes.dash),
                "dashscale" to JsonPrimitive(attributes.dashScale),
                "linecap" to JsonPrimitive(attributes.lineCap),
                "shader" to JsonObject(
                    linkedMapOf(
                        "enabled" to
                            JsonPrimitive(attributes.shader.enabled),
                        "fixed" to JsonPrimitive(attributes.shader.fixed),
                        "type" to JsonPrimitive(attributes.shader.type),
                        "hue" to JsonPrimitive(attributes.shader.hue),
                        "saturation" to
                            JsonPrimitive(attributes.shader.saturation),
                        "minlightness" to JsonPrimitive(
                            attributes.shader.minimumLightness,
                        ),
                        "maxlightness" to JsonPrimitive(
                            attributes.shader.maximumLightness,
                        ),
                        "light" to JsonObject(
                            linkedMapOf(
                                "type" to JsonPrimitive(light.type),
                                "az" to JsonPrimitive(light.azimuth),
                                "el" to JsonPrimitive(light.elevation),
                                "bank" to JsonPrimitive(light.bank),
                                "dir" to JsonPrimitive(light.direction),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun axes3DMemberAttributes(
        source: ParsedObject,
        role: String,
        element: GeometryElement,
    ): JsonObject {
        if (role == "O") {
            return JsonObject(
                linkedMapOf(
                    "id" to JsonPrimitive(element.id),
                    "name" to JsonPrimitive(""),
                    "withlabel" to JsonPrimitive(false),
                    "visible" to JsonPrimitive(false),
                ),
            )
        }
        val defaults = linkedMapOf<String, JsonElement>(
            "id" to JsonPrimitive(element.id),
            "name" to JsonPrimitive(element.name),
            "withlabel" to JsonPrimitive(false),
        )
        var inheritedVisibility = true
        val roleAttributes: JsonObject
        if (element is Curve && element.isTicks3D) {
            val axisRole = role.removeSuffix("Ticks")
            val axis = source.attributes[axisRole.lowercase()]
                as? JsonObject ?: JsonObject(emptyMap())
            roleAttributes = axis["ticks3d"]
                as? JsonObject ?: JsonObject(emptyMap())
            inheritedVisibility = (
                axis["visible"] as? JsonPrimitive
                )?.booleanOrNull ?: true
            defaults["ticksdistance"] = JsonPrimitive(1)
            defaults["majorheight"] = JsonPrimitive(10)
            defaults["minorticks"] = JsonPrimitive(0)
            defaults["tickendings"] = JsonArray(
                listOf(JsonPrimitive(0), JsonPrimitive(1)),
            )
            defaults["drawlabels"] = JsonPrimitive(true)
        } else {
            roleAttributes = source.attributes[role.lowercase()]
                as? JsonObject ?: JsonObject(emptyMap())
            when {
                element is Plane3D -> {
                    val rear = role.endsWith("Rear")
                    defaults["visible"] = JsonPrimitive(rear)
                    defaults["type"] = JsonPrimitive(
                        if (rear) "shader" else "wireframe",
                    )
                    defaults["strokewidth"] = JsonPrimitive(1)
                    defaults["strokecolor"] = JsonPrimitive("#dddddd")
                    defaults["fillcolor"] = JsonPrimitive(
                        if (rear) "#dddddd" else "none",
                    )
                    defaults["layer"] = JsonPrimitive(0)
                    defaults["mesh3d"] = JsonObject(
                        mapOf(
                            "visible" to JsonPrimitive(false),
                            "layer" to JsonPrimitive(1),
                        ),
                    )
                }
                role.endsWith("AxisBorder") -> {
                    defaults["visible"] = JsonPrimitive(true)
                    defaults["strokewidth"] = JsonPrimitive(1)
                    defaults["lastarrow"] = JsonPrimitive(false)
                }
                "PlaneRear" in role || "PlaneFront" in role -> {
                    val planeRole = AXES_3D_PLANE_ROLES.firstOrNull {
                        role.startsWith(it)
                    }
                    val defaultPlaneVisible =
                        planeRole?.endsWith("Rear") == true
                    val planeVisible = (
                        (
                            planeRole?.let {
                                source.attributes[it.lowercase()]
                            } as? JsonObject
                            )?.get("visible") as? JsonPrimitive
                        )?.booleanOrNull ?: defaultPlaneVisible
                    inheritedVisibility = planeVisible
                    defaults["visible"] = JsonPrimitive(planeVisible)
                    defaults["strokewidth"] = JsonPrimitive(1.2)
                    defaults["strokecolor"] = JsonPrimitive("#888888")
                    defaults["layer"] = JsonPrimitive(12)
                }
            }
        }
        defaults.putAll(roleAttributes)
        if (element is Line3D) {
            defaults.remove("ticks3d")
        }
        val inheritedVisible = (
            defaults["visible"] as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)
            ?.content
            ?.equals("inherit", ignoreCase = true) == true
        if (inheritedVisible) {
            defaults["visible"] = JsonPrimitive(inheritedVisibility)
        }
        defaults["id"] = JsonPrimitive(element.id)
        defaults["name"] = JsonPrimitive(element.name)
        return JsonObject(defaults)
    }

    private fun invalidAxes3DResult(
        location: JessieCodeAstLocation,
    ): GMResult<Nothing, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.InvalidAst(
                reason =
                    "Native axes3d creator returned an invalid composition.",
                location = location,
            ),
        )

    private fun invalidView3DDefaultAxesResult(
        location: JessieCodeAstLocation,
    ): GMResult<Nothing, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.InvalidAst(
                reason =
                    "Native view3d creator did not register default axes.",
                location = location,
            ),
        )

    private fun invalidAxes3DDocumentResult(
        source: ParsedObject,
    ): JsxGraphDocumentError.ElementCreation =
        JsxGraphDocumentError.ElementCreation(
            objectIndex = source.index,
            id = source.id,
            type = source.type,
            reason = "creator returned an invalid axes3d composition",
        )

    private fun invalidView3DDefaultAxesDocumentResult(
        source: ParsedObject,
    ): JsxGraphDocumentError.ElementCreation =
        JsxGraphDocumentError.ElementCreation(
            objectIndex = source.index,
            id = source.id,
            type = source.type,
            reason = "view3d creator did not register default axes",
        )

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

    private fun invalidPolyhedron3DResult(
        source: ParsedObject,
        location: JessieCodeAstLocation,
    ): GMResult<Nothing, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.InvalidAst(
                reason =
                    "Native ${source.type} creator did not return a " +
                        "Polyhedron3D.",
                location = location,
            ),
        )

    private fun invalidPolyhedron3DDocumentResult(
        source: ParsedObject,
    ): JsxGraphDocumentError.ElementCreation =
        JsxGraphDocumentError.ElementCreation(
            objectIndex = source.index,
            id = source.id,
            type = source.type,
            reason = "creator did not return a Polyhedron3D",
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
        val transformationsById = linkedMapOf<String, Transformation>()

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
            val runtimeParents = when (
                val result = runtimeArray(sourceObject.parents)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val parents = documentParents(
                sourceObject = sourceObject,
                parents = runtimeParents,
                transformationsById = transformationsById,
            )
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
            if (value is JessieCodeRuntimeValue.TransformationReference) {
                if (
                    sourceObject.type !in
                    TRANSFORMATION_CREATORS
                ) {
                    return GMResult.Err(
                        JsxGraphDocumentError.ElementCreation(
                            objectIndex = sourceObject.index,
                            id = sourceObject.id,
                            type = sourceObject.type,
                            reason =
                                "non-transform creator returned a " +
                                    "transformation",
                        ),
                    )
                }
                transformationsById[sourceObject.id] = value.transformation
                continue
            }
            if (value is JessieCodeRuntimeValue.CompositionReference) {
                if (sourceObject.type != "axes3d") {
                    return GMResult.Err(
                        JsxGraphDocumentError.ElementCreation(
                            objectIndex = sourceObject.index,
                            id = sourceObject.id,
                            type = sourceObject.type,
                            reason =
                                "creator returned an unsupported composition",
                        ),
                    )
                }
                val axes = value.composition as? Axes3D
                    ?: return GMResult.Err(
                        invalidAxes3DDocumentResult(sourceObject),
                    )
                created += axes3DCreatedSourceElements(
                    source = sourceObject,
                    axes = axes,
                )
                continue
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
            if (element is View3D) {
                val axes = element.defaultAxes
                    ?: return GMResult.Err(
                        invalidView3DDefaultAxesDocumentResult(sourceObject),
                    )
                created += axes3DCreatedSourceElements(
                    source = sourceObject,
                    axes = axes,
                )
                continue
            }
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
            } else if (sourceObject.type == "axis") {
                val line = element as? Line
                    ?: return GMResult.Err(
                        JsxGraphDocumentError.ElementCreation(
                            objectIndex = sourceObject.index,
                            id = sourceObject.id,
                            type = sourceObject.type,
                            reason =
                                "creator did not return an Axis Line",
                        ),
                    )
                val expanded = axisCreatedSourceElements(
                    source = sourceObject,
                    line = line,
                ) ?: return GMResult.Err(
                    JsxGraphDocumentError.ElementCreation(
                        objectIndex = sourceObject.index,
                        id = sourceObject.id,
                        type = sourceObject.type,
                        reason = "axis did not create defaultTicks",
                    ),
                )
                created += expanded
            } else if (sourceObject.type == "polyhedron3d") {
                val polyhedron = element as? Polyhedron3D
                    ?: return GMResult.Err(
                        invalidPolyhedron3DDocumentResult(sourceObject),
                    )
                created += polyhedron3DCreatedSourceElements(
                    source = sourceObject,
                    polyhedron = polyhedron,
                )
            } else if (
                sourceObject.type == "parametricsurface3d" ||
                sourceObject.type == "functiongraph3d"
            ) {
                val surface = element as? Surface3D
                    ?: return GMResult.Err(
                        JsxGraphDocumentError.ElementCreation(
                            objectIndex = sourceObject.index,
                            id = sourceObject.id,
                            type = sourceObject.type,
                            reason =
                                "creator did not return a Surface3D",
                        ),
                    )
                created += CreatedSourceElement(sourceObject, surface)
                surface.polyhedron?.let { polyhedron ->
                    created += polyhedron3DCreatedSourceElements(
                        source = sourceObject.copy(
                            index = sourceObject.index + 1,
                        ),
                        polyhedron = polyhedron,
                    )
                }
            } else if (sourceObject.type == "plane3d") {
                val plane = element as? Plane3D
                    ?: return GMResult.Err(
                        JsxGraphDocumentError.ElementCreation(
                            objectIndex = sourceObject.index,
                            id = sourceObject.id,
                            type = sourceObject.type,
                            reason =
                                "creator did not return a Plane3D",
                        ),
                    )
                created += CreatedSourceElement(sourceObject, plane)
                plane.surface3D?.let { surface ->
                    created += polyhedron3DCreatedSourceElements(
                        source = sourceObject.copy(
                            index = sourceObject.index + 1,
                        ),
                        polyhedron = surface,
                    )
                }
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
            val scenePoint = scenePoints[sourceElement.source.id] ?: continue
            val handle = when (val element = sourceElement.element) {
                is Point -> SessionPoint.TwoDimensional(
                    point = element,
                    draggable = scenePoint.draggable,
                )
                is Point3D -> SessionPoint.ThreeDimensional(
                    point = element,
                    draggable = scenePoint.draggable,
                )
                else -> continue
            }
            sessionPoints[sourceElement.source.id] = handle
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

    // Construction-document adaptation of
    // JSXGraph: src/base/transformation.js -> createTransform and
    // src/base/point.js -> createPoint transformation parent form.
    private fun documentParents(
        sourceObject: ParsedObject,
        parents: List<JessieCodeRuntimeValue>,
        transformationsById: Map<String, Transformation>,
    ): List<JessieCodeRuntimeValue> {
        val transformationIndex = when (sourceObject.type) {
            "point" -> 1
            "point3d" -> 2
            "line3d" -> 2
            "axis3d" -> 2
            "plane3d" -> 2
            "curve3d" -> 2
            "parametricsurface3d" -> 2
            "polygon3d" -> 2
            "polyhedron3d" -> 2
            else -> return parents
        }
        if (parents.size <= transformationIndex) {
            return parents
        }
        val transformationParent = when (
            val parent = parents[transformationIndex]
        ) {
            is JessieCodeRuntimeValue.StringValue ->
                transformationsById[parent.value]?.let {
                    JessieCodeRuntimeValue.TransformationReference(it)
                }
            is JessieCodeRuntimeValue.ArrayValue -> {
                val transformations =
                    parent.values.map { value ->
                        val id = (
                            value as?
                                JessieCodeRuntimeValue.StringValue
                            )?.value ?: return parents
                        transformationsById[id]
                            ?: return parents
                    }
                if (transformations.isEmpty()) {
                    return parents
                }
                JessieCodeRuntimeValue.ArrayValue(
                    transformations.map {
                        JessieCodeRuntimeValue.TransformationReference(it)
                    },
                )
            }
            else -> null
        } ?: return parents
        return parents.toMutableList().also {
            it[transformationIndex] = transformationParent
        }
    }

    private fun snapshotScene(
        document: ParsedDocument,
        created: List<CreatedSourceElement>,
        maxCurvePoints: Int? = null,
    ): GMResult<JsxGraphScene, JsxGraphDocumentError> {
        val sceneElements = mutableListOf<JsxGraphSceneElement>()
        val effectiveVisibilityByElement =
            mutableMapOf<GeometryElement, Boolean>()
        for (sourceElement in depthOrderedSourceElements(created)) {
            val curve = sourceElement.element as? Curve
            if (maxCurvePoints != null) {
                for (
                    usage in sourceCurvePointUsages(
                        sourceElement.element,
                    )
                ) {
                    val requested = usage.requested
                    if (requested > maxCurvePoints) {
                        return GMResult.Err(
                            JsxGraphDocumentError.CurvePointLimitExceeded(
                                objectIndex = sourceElement.source.index,
                                id = usage.id,
                                limit = maxCurvePoints,
                                actual = requested
                                    .coerceAtMost(Int.MAX_VALUE.toLong())
                                    .toInt(),
                            ),
                        )
                    }
                }
            }
            // JSXGraph 1.13.3: src/base/ticks.js -> createTicks;
            // src/base/element.js -> fullUpdate / updateVisibility.
            val inheritedVisibility =
                (sourceElement.element as? Ticks)?.let { ticks ->
                    effectiveVisibilityByElement[ticks.parent] ?: true
                }
            when (
                val result = sceneElement(
                    sourceElement = sourceElement,
                    inheritedVisibility = inheritedVisibility,
                )
            ) {
                is GMResult.Ok -> {
                    sceneElements += result.value
                    effectiveVisibilityByElement[sourceElement.element] =
                        result.value.style.visible
                    val ticks = curve?.ticks3DDefinition
                    val drawsLabels =
                        (
                            sourceElement.source.attributes["drawlabels"]
                                as? JsonPrimitive
                            )?.booleanOrNull ?: true
                    if (ticks != null && drawsLabels) {
                        val ticksScene =
                            (
                                result.value as?
                                    JsxGraphSceneElement.Curve
                                )?.ticks3D
                        val labelAttributes =
                            sourceElement.source.attributes["label"]
                                as? JsonObject ?: JsonObject(emptyMap())
                        for ((index, label) in
                            ticks.labelElements.withIndex()
                        ) {
                            val attributes = linkedMapOf<String, JsonElement>()
                            attributes.putAll(labelAttributes)
                            attributes["id"] = JsonPrimitive(label.id)
                            attributes["name"] = JsonPrimitive(label.name)
                            attributes["withlabel"] = JsonPrimitive(false)
                            when (
                                val labelResult = sceneElement(
                                    CreatedSourceElement(
                                        source =
                                            sourceElement.source.copy(
                                                id = label.id,
                                                type = "text3d",
                                                attributes =
                                                    JsonObject(attributes),
                                            ),
                                        element = label,
                                    ),
                                    ticks3DLabel =
                                        ticksScene?.tickBases3D
                                            ?.getOrNull(index)
                                            ?.let { tickBase ->
                                                JsxGraphTicks3DLabel(
                                                    tickBase3D = tickBase,
                                                    direction2 =
                                                        ticksScene.direction2,
                                                    positiveEnding =
                                                        ticksScene
                                                            .tickEndings[1],
                                                    majorHeight =
                                                        ticksScene.majorHeight,
                                                    projection =
                                                        ticksScene.projection,
                                                )
                                            },
                                )
                            ) {
                                is GMResult.Ok ->
                                    sceneElements += labelResult.value
                                is GMResult.Err -> return labelResult
                            }
                        }
                    }
                    val plane = sourceElement.element as? Plane3D
                    val mesh = plane?.mesh3D
                    if (mesh != null) {
                        when (
                            val meshResult = sceneElement(
                                planeMeshSourceElement(
                                    sourceElement = sourceElement,
                                    mesh = mesh,
                                ),
                            )
                        ) {
                            is GMResult.Ok ->
                                sceneElements += meshResult.value
                            is GMResult.Err -> return meshResult
                        }
                    }
                }
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
        ticks3DLabel: JsxGraphTicks3DLabel? = null,
        inheritedVisibility: Boolean? = null,
    ): GMResult<JsxGraphSceneElement, JsxGraphDocumentError> {
        val source =
            if (sourceElement.element is Face3D) {
                sourceElement.source.copy(
                    attributes =
                        face3DSourceAttributes(sourceElement.element),
                )
            } else {
                sourceElement.source
            }
        val element = sourceElement.element
        val attributes = AttributeReader(source)
        when (val result = attributes.validateSupported(element)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val style = when (
            val result = attributes.style(
                element = element,
                inheritedVisibility = inheritedVisibility,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            when (
                val result = attributes.boolean(
                    name = "withlabel",
                    default = element is Point || element is Point3D,
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
            is Point3D -> when (
                val result = point3DSceneElement(
                    element = element,
                    attributes = attributes,
                    style = style,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }

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

            is Ticks -> {
                element.evaluationError?.let { error ->
                    return GMResult.Err(attributes.elementCreation(error.toString()))
                }
                val ticksParent = when (val parent = element.parent) {
                    is Line -> {
                        val endpoints = lineEndpoints(parent)
                            ?: return GMResult.Err(
                                attributes.nonFiniteGeometry(),
                            )
                        JsxGraphTicksParent2D.Line(
                            point1 = endpoints.first,
                            point2 = endpoints.second,
                            straightFirst = parent.straightFirst,
                            straightLast = parent.straightLast,
                            axis = parent.type == Const.OBJECT_TYPE_AXIS,
                            axisDefinition =
                                axisSceneDefinition(parent),
                        )
                    }
                    is Curve ->
                        JsxGraphTicksParent2D.Curve(
                            locations = element.curveLocations.map {
                                location ->
                                JsxGraphCurveTickLocation(
                                    base = JsxGraphPoint2D(
                                        location.baseX,
                                        location.baseY,
                                    ),
                                    normal = JsxGraphPoint2D(
                                        location.normalX,
                                        location.normalY,
                                    ),
                                    major = location.major,
                                    label = location.label,
                                )
                            },
                        )
                    else -> return GMResult.Err(
                        attributes.elementCreation(
                            "Ticks parent is neither a Line nor a Curve",
                        ),
                    )
                }
                val labelAttributes = when (
                    val result = attributes.nested("label")
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val labelStyle = when (
                    val result = labelAttributes.style(
                        element = element,
                        inheritedVisibility = style.visible,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val config = element.attributes
                val anchor = when (val value = config.anchor) {
                    TicksAnchor.Left -> "left"
                    TicksAnchor.Right -> "right"
                    TicksAnchor.Middle -> "middle"
                    is TicksAnchor.Fraction ->
                        JsxGraphTicks2D.NUMERIC_ANCHOR_PREFIX + value.value
                }
                JsxGraphSceneElement.Ticks(
                    id = element.id,
                    name = element.name,
                    style = style,
                    definition = JsxGraphTicks2D(
                        parent = ticksParent,
                        fixedTicks =
                            (element.source as? TicksSource.Fixed)
                                ?.values?.toList(),
                        fixedLabels = config.labels,
                        anchor = anchor,
                        drawZero = config.drawZero,
                        insertTicks = config.insertTicks,
                        minTicksDistance = config.minTicksDistance,
                        minorHeight = config.minorHeight,
                        majorHeight = config.majorHeight,
                        tickEndings = config.tickEndings.toList(),
                        majorTickEndings =
                            config.majorTickEndings.toList(),
                        ignoreInfiniteTickEndings =
                            config.ignoreInfiniteTickEndings,
                        minorTicks = config.minorTicks,
                        ticksPerLabel = config.ticksPerLabel,
                        scale = config.scale,
                        scaleSymbol = config.scaleSymbol,
                        maxLabelLength = config.maxLabelLength,
                        precision = config.precision,
                        digits = config.digits,
                        beautifulScientificTickLabels =
                            config.beautifulScientificTickLabels,
                        useUnicodeMinus = config.useUnicodeMinus,
                        face = config.face,
                        includeBoundaries = config.includeBoundaries,
                        type = config.ticksType,
                        ticksDistance = config.ticksDistance,
                        drawLabels = config.drawLabels,
                        clip = config.clip,
                        labelStyle = JsxGraphTicksLabelStyle(
                            visible = labelStyle.visible,
                            color = labelStyle.strokeColor,
                            opacity = labelStyle.strokeOpacity,
                            fontSize = config.labelFontSize,
                            anchorX = config.labelAnchorX,
                            anchorY = config.labelAnchorY,
                            offsetX = config.labelOffset[0],
                            offsetY = config.labelOffset[1],
                        ),
                    ),
                )
            }

            is Line3D -> {
                val lifecycleError =
                    element.directionEvaluationError
                        ?: element.rangeEvaluationError
                        ?: element.transformationEvaluationError
                if (lifecycleError != null) {
                    return GMResult.Err(
                        JsxGraphDocumentError.ElementCreation(
                            objectIndex = source.index,
                            id = source.id,
                            type = source.type,
                            reason = lifecycleError.toString(),
                        ),
                    )
                }
                val point1 = point(element.endpoints[0].point2D)
                    ?: return GMResult.Err(attributes.nonFiniteGeometry())
                val point2 = point(element.endpoints[1].point2D)
                    ?: return GMResult.Err(attributes.nonFiniteGeometry())
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
                            if (source.type == "axis3d") {
                                DEFAULT_ARROW_HEAD
                            } else {
                                null
                            },
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
                    straightFirst = false,
                    straightLast = false,
                    firstArrow = firstArrow,
                    lastArrow = lastArrow,
                )
            }

            is Curve3D -> {
                element.evaluationError?.let { error ->
                    return GMResult.Err(
                        attributes.elementCreation(error.toString()),
                    )
                }
                when (
                    val result = curveSceneElement(
                        element = element,
                        points = element.curve2D.points,
                        bezierDegree = element.curve2D.bezierDegree,
                        style = style,
                        attributes = attributes,
                        allowFill = false,
                        allowPathBreaks = true,
                        vectorField3D =
                            element.vectorField3DSnapshot()?.let {
                                    vectorField ->
                                JsxGraphVectorField3D(
                                    vectors =
                                        vectorField.vectors.map { vector ->
                                            JsxGraphVectorField3DVector(
                                                start =
                                                    vector.start.toList(),
                                                vector =
                                                    vector.vector.toList(),
                                                scaledNorm =
                                                    vector.scaledNorm,
                                            )
                                        },
                                    arrowEnabled =
                                        vectorField.arrowEnabled,
                                    arrowSize = vectorField.arrowSize,
                                    arrowAngle = vectorField.arrowAngle,
                                    projection = JsxGraphProjection3D(
                                        matrix3D =
                                            element.view.matrix3D.map {
                                                it.toList()
                                            },
                                        central =
                                            element.view.projectionType ==
                                                "central",
                                        viewPortTransform =
                                            element.view
                                                .viewPortTransform
                                                ?.map { it.toList() },
                                    ),
                                )
                            },
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }

            is Circle3D -> {
                element.evaluationError?.let { error ->
                    return GMResult.Err(
                        attributes.elementCreation(error.toString()),
                    )
                }
                when (
                    val result = curveSceneElement(
                        element = element,
                        points = element.curve.curve2D.points,
                        bezierDegree = element.curve.curve2D.bezierDegree,
                        style = style.copy(
                            visible =
                                style.visible &&
                                    !element.Radius().isNaN(),
                        ),
                        attributes = attributes,
                        allowFill = false,
                        allowPathBreaks = true,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }

            is Sphere3D -> {
                element.evaluationError?.let { error ->
                    return GMResult.Err(
                        attributes.elementCreation(error.toString()),
                    )
                }
                val radius = element.Radius()
                if (!radius.isFinite()) {
                    return GMResult.Err(attributes.nonFiniteGeometry())
                }
                val proxy = element.element2D
                val points = when (proxy) {
                    is Circle -> proxy.points
                    is Curve -> proxy.points
                    else -> return GMResult.Err(
                        attributes.elementCreation(
                            "Sphere3D projection is unavailable",
                        ),
                    )
                }
                val bezierDegree = when (proxy) {
                    is Circle -> proxy.bezierDegree
                    is Curve -> proxy.bezierDegree
                    else -> 0
                }
                when (
                    val result = curveSceneElement(
                        element = element,
                        points = points,
                        bezierDegree = bezierDegree,
                        style = style.copy(
                            visible = style.visible && !radius.isNaN(),
                        ),
                        attributes = attributes,
                        allowFill = true,
                        allowPathBreaks = false,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }

            is Surface3D -> {
                element.evaluationError?.let { error ->
                    return GMResult.Err(
                        attributes.elementCreation(error.toString()),
                    )
                }
                when (
                    val result = curveSceneElement(
                        element = element,
                        points = element.curve2D.points,
                        bezierDegree = element.curve2D.bezierDegree,
                        style = style.copy(
                            visible =
                                style.visible &&
                                    element.surfaceAttributes.surfaceType ==
                                    "wireframe",
                        ),
                        attributes = attributes,
                        allowFill = false,
                        allowPathBreaks = true,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }

            is Plane3D -> {
                val lifecycleError =
                    element.directionEvaluationError
                        ?: element.rangeEvaluationError
                        ?: element.transformationEvaluationError
                if (lifecycleError != null) {
                    return GMResult.Err(
                        attributes.elementCreation(
                            lifecycleError.toString(),
                        ),
                    )
                }
                when (
                    val result = curveSceneElement(
                        element = element,
                        points = element.outline2D.points,
                        bezierDegree = element.outline2D.bezierDegree,
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

            is Face3D -> {
                element.evaluationError?.let { error ->
                    return GMResult.Err(
                        attributes.elementCreation(error.toString()),
                    )
                }
                when (
                    val result = curveSceneElement(
                        element = element,
                        points = element.curve2D.points,
                        bezierDegree = element.curve2D.bezierDegree,
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

            is Polygon3D -> when (
                val result = polygon3DSceneElement(
                    element = element,
                    attributes = attributes,
                    style = style,
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
                            if (source.type == "axis") {
                                DEFAULT_AXIS_ARROW_HEAD
                            } else if (isArrow) {
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
                    axis = axisSceneDefinition(element),
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

            is Text3D,
            is Text,
            -> {
                val text = when (element) {
                    is Text3D -> {
                        element.coordinateEvaluationError?.let { error ->
                            return GMResult.Err(
                                attributes.elementCreation(error.toString()),
                            )
                        }
                        element.text2D
                    }
                    is Text -> element
                    else -> return GMResult.Err(
                        attributes.elementCreation(
                            "Unexpected text element type",
                        ),
                    )
                }
                text.contentEvaluationError?.let { error ->
                    return GMResult.Err(
                        JsxGraphDocumentError.ElementCreation(
                            objectIndex = source.index,
                            id = source.id,
                            type = source.type,
                            reason = error.toString(),
                        ),
                    )
                }
                val x = text.X()
                .takeIf(Double::isFinite)
                ?: return GMResult.Err(attributes.nonFiniteGeometry())
                val y = text.Y()
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
                    content = text.plaintext,
                    fontSize = fontSize,
                    anchorX = anchorX,
                    anchorY = anchorY,
                    ticks3DLabel = ticks3DLabel,
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

    private fun point3DSceneElement(
        element: Point3D,
        attributes: AttributeReader,
        style: JsxGraphElementStyle,
    ): GMResult<JsxGraphSceneElement.Point, JsxGraphDocumentError> {
        element.coordinateEvaluationError?.let { error ->
            return GMResult.Err(
                attributes.elementCreation(error.toString()),
            )
        }
        element.transformationEvaluationError?.let { error ->
            return GMResult.Err(
                attributes.elementCreation(error.toString()),
            )
        }
        val projected = element.point2D.coords.usrCoords
        val coordinates = JsxGraphPoint2D(
            x = projected[1],
            y = projected[2],
        )
        val isReal =
            element.testIfFinite() &&
                coordinates.x.isFinite() &&
                coordinates.y.isFinite()
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
        return GMResult.Ok(
            JsxGraphSceneElement.Point(
                id = element.id,
                name = element.name,
                style = style,
                coordinates = coordinates,
                size = size,
                face = face,
                draggable =
                    element.point2D.isDraggable &&
                        !element.isFixed &&
                        style.visible &&
                        isReal,
                isReal = isReal,
            ),
        )
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

    // JSXGraph 1.13.3: src/3d/polygon3d.js -> createPolygon3D;
    // src/base/polygon.js -> Polygon constructor.
    private fun polygon3DSceneElement(
        element: Polygon3D,
        attributes: AttributeReader,
        style: JsxGraphElementStyle,
    ): GMResult<JsxGraphSceneElement.Polygon, JsxGraphDocumentError> {
        for (vertex in element.vertices) {
            val lifecycleError =
                vertex.coordinateEvaluationError
                    ?: vertex.transformationEvaluationError
            if (lifecycleError != null) {
                return GMResult.Err(
                    attributes.elementCreation(lifecycleError.toString()),
                )
            }
        }
        val vertices = element.vertices.map { vertex ->
            point(vertex.point2D)
                ?: return GMResult.Err(attributes.nonFiniteGeometry())
        }
        val vertexAttributes = when (
            val result = attributes.nested("vertices")
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val withVertexLabels = when (
            val result = vertexAttributes.boolean(
                name = "withlabel",
                default = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val implicitVertices = mutableListOf<JsxGraphSceneElement.Point>()
        for (vertex in element.vertices) {
            if (vertex !in element.ownedVertices) {
                continue
            }
            val proxy = vertex.point2D
            if (withVertexLabels && proxy.name.isNotEmpty()) {
                return GMResult.Err(
                    vertexAttributes.unsupportedValue(
                        attribute = "withLabel",
                        value = "true with non-empty name",
                    ),
                )
            }
            val vertexStyle = when (
                val result = vertexAttributes.style(proxy)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            when (
                val result = pointSceneElement(
                    element = proxy,
                    attributes = vertexAttributes,
                    style = vertexStyle,
                    respectFixedAttribute = false,
                )
            ) {
                is GMResult.Ok -> implicitVertices += result.value
                is GMResult.Err -> return result
            }
        }
        val borderAttributes = when (
            val result = attributes.nested("borders")
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val borderElement =
            element.polygon2D.borders.firstOrNull()
                ?: element.polygon2D
        val resolvedBorderStyle = when (
            val result = borderAttributes.style(borderElement)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val borderStyle =
            if (borderAttributes.has("layer")) {
                resolvedBorderStyle
            } else {
                resolvedBorderStyle.copy(
                    layer = DEFAULT_POLYGON_BORDER_LAYER,
                )
            }
        return GMResult.Ok(
            JsxGraphSceneElement.Polygon(
                id = element.id,
                name = element.name,
                style = style,
                vertices = vertices,
                implicitVertices = implicitVertices,
                borderStyle = borderStyle,
                withLines = element.withLines,
                isClosed = true,
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
        vectorField3D: JsxGraphVectorField3D? = null,
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
                vectorField3D = vectorField3D,
                ticks3D =
                    (element as? Curve)
                        ?.ticks3DDefinition
                        ?.let { definition ->
                            JsxGraphTicks3D(
                                tickBases3D =
                                    definition.tickBases3D.map {
                                        it.toList()
                                    },
                                direction2 =
                                    definition.normalizedDirection2.toList(),
                                tickEndings =
                                    definition.tickEndings.toList(),
                                majorHeight = definition.majorHeight,
                                projection = JsxGraphProjection3D(
                                    matrix3D =
                                        definition.view.matrix3D.map {
                                            it.toList()
                                        },
                                    central =
                                        definition.view.projectionType ==
                                            "central",
                                    viewPortTransform =
                                        definition.view.viewPortTransform
                                            ?.map { it.toList() },
                                ),
                            )
                        },
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
            when (val result = validateTickCountLimit(sourceObject)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            if (sourceObject.type != "curve3d") {
                when (
                    val result = validateCurvePointLimit(
                        sourceObject,
                        limits.maxCurvePoints,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (sourceObject.type != "polygon3d") {
                when (
                    val result = validatePolygonVertexLimit(
                        sourceObject,
                        limits.maxPolygonVertices,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
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
        val objectsById = objects.associateBy(ParsedObject::id)
        for (sourceObject in objects) {
            if (sourceObject.type == "curve3d") {
                when (
                    val result = validateCurvePointLimit(
                        sourceObject = sourceObject,
                        limit = limits.maxCurvePoints,
                        objectsById = objectsById,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (sourceObject.type == "polygon3d") {
                when (
                    val result = validatePolygonVertexLimit(
                        sourceObject = sourceObject,
                        limit = limits.maxPolygonVertices,
                        objectsById = objectsById,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
        }
        val sceneObjectCount = objects.sumOf { sourceObject ->
            sceneElementCount(sourceObject, objectsById).toLong()
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

    private fun validateTickCountLimit(
        sourceObject: ParsedObject,
    ): GMResult<Unit, JsxGraphDocumentError> {
        val requested = when (sourceObject.type) {
            "ticks" ->
                (sourceObject.parents.getOrNull(1) as? JsonArray)
                    ?.size?.toLong()
            "hatch", "hash" ->
                (
                    sourceObject.parents.getOrNull(1) as?
                        JsonPrimitive
                    )?.doubleOrNull?.let(Hatch::positionCount)
            else -> null
        } ?: return GMResult.Ok(Unit)
        return if (requested > Ticks.DEFAULT_MAXIMUM_TICK_COUNT) {
            GMResult.Err(
                JsxGraphDocumentError.TickCountLimitExceeded(
                    objectIndex = sourceObject.index,
                    id = sourceObject.id,
                    limit = Ticks.DEFAULT_MAXIMUM_TICK_COUNT,
                    actual = requested
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                ),
            )
        } else {
            GMResult.Ok(Unit)
        }
    }

    private fun validateCurvePointLimit(
        sourceObject: ParsedObject,
        limit: Int,
        objectsById: Map<String, ParsedObject> = emptyMap(),
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
                "vectorfield3d",
                "ellipse",
                "hyperbola",
                "parabola",
                "ticks3d",
                "mesh3d",
                "plane3d",
                "polyhedron3d",
                "curve3d",
                "circle3d",
                "intersectioncircle3d",
                "sphere3d",
                "parametricsurface3d",
                "functiongraph3d",
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
        val requested = if (sourceObject.type == "vectorfield3d") {
            jsonVectorField3DPointCount(sourceObject)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        } else if (
            sourceObject.type == "curve3d" ||
            sourceObject.type == "circle3d" ||
            sourceObject.type == "intersectioncircle3d" ||
            sourceObject.type == "sphere3d"
        ) {
            jsonCurve3DPointCount(
                source = sourceObject,
                objectsById = objectsById,
                visited = emptySet(),
            )
        } else if (sourceObject.type == "polyhedron3d") {
            jsonPolyhedron3DMaximumCurvePointCount(sourceObject)
        } else if (sourceObject.type == "mesh3d") {
            jsonMeshPointCount(
                rangeU = sourceObject.parents.getOrNull(4),
                rangeV = sourceObject.parents.getOrNull(5),
                attributes = sourceObject.attributes,
            ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } else if (sourceObject.type == "plane3d") {
            jsonPlaneMaximumCurvePointCount(sourceObject)
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } else if (
            sourceObject.type == "parametricsurface3d" ||
            sourceObject.type == "functiongraph3d"
        ) {
            jsonSurface3DMaximumCurvePointCount(sourceObject.attributes)
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } else if (sourceObject.type == "ticks3d") {
            val length = (
                sourceObject.parents.getOrNull(3) as? JsonPrimitive
                )?.doubleOrNull
            val ticksDistance = (
                sourceObject.attributes["ticksdistance"] as? JsonPrimitive
                )?.doubleOrNull ?: 1.0
            if (length == null) {
                0
            } else {
                Ticks3D.curvePointCount(length, ticksDistance)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
            }
        } else if (sourceObject.type == "inequality") {
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

    private fun runtimePolyhedron3DFaceVertexCounts(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
    ): List<Int>? {
        val base = when (
            val value = parents.getOrNull(1)
        ) {
            is JessieCodeRuntimeValue.ElementReference ->
                value.element as? Polyhedron3D
            is JessieCodeRuntimeValue.StringValue ->
                board?.select(value.value) as? Polyhedron3D
            else -> null
        }
        if (base != null) {
            return base.definition.faceKeys.map(List<String>::size)
        }
        val faces = (
            parents.getOrNull(2) as? JessieCodeRuntimeValue.ArrayValue
            )?.values ?: return null
        return faces.map { face ->
            val values = (
                face as? JessieCodeRuntimeValue.ArrayValue
                )?.values ?: return null
            val nestedVertices = values
                .takeIf { it.size == 2 }
                ?.getOrNull(0) as? JessieCodeRuntimeValue.ArrayValue
            val nestedAttributes = values
                .takeIf { it.size == 2 }
                ?.getOrNull(1) as? JessieCodeRuntimeValue.ObjectValue
            if (nestedVertices != null && nestedAttributes != null) {
                nestedVertices.values.size
            } else {
                values.size
            }
        }
    }

    private fun runtimePolyhedron3DMaximumCurvePointCount(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
    ): Long? =
        runtimePolyhedron3DFaceVertexCounts(board, parents)
            ?.maxOfOrNull(::polyhedron3DCurvePointCount)

    private fun runtimePolyhedron3DMaximumFaceVertexCount(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
    ): Long? =
        runtimePolyhedron3DFaceVertexCounts(board, parents)
            ?.maxOrNull()
            ?.toLong()

    private fun jsonPolyhedron3DFaceVertexCounts(
        source: ParsedObject,
    ): List<Int> {
        val faces = source.parents.getOrNull(2) as? JsonArray
            ?: return emptyList()
        return faces.mapNotNull { face ->
            val values = face as? JsonArray ?: return@mapNotNull null
            val nestedVertices = values
                .takeIf { it.size == 2 }
                ?.getOrNull(0) as? JsonArray
            val nestedAttributes = values
                .takeIf { it.size == 2 }
                ?.getOrNull(1) as? JsonObject
            if (nestedVertices != null && nestedAttributes != null) {
                nestedVertices.size
            } else {
                values.size
            }
        }
    }

    private fun jsonPolyhedron3DMaximumCurvePointCount(
        source: ParsedObject,
    ): Int =
        jsonPolyhedron3DFaceVertexCounts(source)
            .maxOfOrNull(::polyhedron3DCurvePointCount)
            ?.coerceAtMost(Int.MAX_VALUE.toLong())
            ?.toInt() ?: 0

    private fun jsonPolyhedron3DMaximumFaceVertexCount(
        source: ParsedObject,
    ): Int =
        jsonPolyhedron3DFaceVertexCounts(source).maxOrNull() ?: 0

    private fun runtimePolygon3DVertexCount(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
    ): Long? {
        val base = when (
            val value = parents.getOrNull(1)
        ) {
            is JessieCodeRuntimeValue.ElementReference ->
                value.element as? Polygon3D
            is JessieCodeRuntimeValue.StringValue ->
                board?.select(value.value) as? Polygon3D
            else -> null
        }
        if (base != null) {
            return (base.vertices.size - 1).coerceAtLeast(0).toLong()
        }
        val direct = parents.drop(1)
        if (direct.isEmpty()) {
            return 0L
        }
        val nested =
            if (direct.size == 1) {
                direct[0] as? JessieCodeRuntimeValue.ArrayValue
            } else {
                null
            }
        if (nested != null && nested.values.isNotEmpty()) {
            val isPointList = nested.values.all { value ->
                when (value) {
                    is JessieCodeRuntimeValue.ElementReference ->
                        value.element is Point3D
                    is JessieCodeRuntimeValue.StringValue ->
                        board?.select(value.value) is Point3D
                    else -> false
                }
            }
            val isCoordinateList = nested.values.all { value ->
                val coordinates = value as?
                    JessieCodeRuntimeValue.ArrayValue
                    ?: return@all false
                coordinates.values.firstOrNull() is
                    JessieCodeRuntimeValue.NumberValue
            }
            if (isPointList || isCoordinateList) {
                return nested.values.size.toLong()
            }
        }
        return direct.size.toLong()
    }

    private fun runtimeCurve3DPointCount(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Long {
        val direct = parents.drop(1)
        val base = direct.firstOrNull()?.let { value ->
            when (value) {
                is JessieCodeRuntimeValue.ElementReference ->
                    value.element as? Curve3D
                is JessieCodeRuntimeValue.StringValue ->
                    board?.select(value.value) as? Curve3D
                else -> null
            }
        }
        if (base != null && direct.size == 2) {
            return base.numberPoints.toLong()
        }
        if (direct.size == 1) {
            val matrix = direct[0] as?
                JessieCodeRuntimeValue.ArrayValue
            if (matrix != null) {
                return matrix.values.size.toLong()
            }
        }
        if (
            direct.size == 4 &&
            direct[0] is JessieCodeRuntimeValue.ArrayValue
        ) {
            return (
                direct[0] as JessieCodeRuntimeValue.ArrayValue
                ).values.size.toLong()
        }
        val configured = (
            attributes.properties["numberpointshigh"] as?
                JessieCodeRuntimeValue.NumberValue
            )?.value
        return when {
            configured == null -> Curve3D.DEFAULT_SAMPLE_COUNT.toLong()
            configured.isNaN() -> 0L
            else -> configured.toLong()
        }
    }

    private fun runtimeVectorField3DPointCount(
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Long {
        val steps = (2..4).map { parentIndex ->
            (
                (
                    parents.getOrNull(parentIndex) as?
                        JessieCodeRuntimeValue.ArrayValue
                    )?.values?.getOrNull(1) as?
                    JessieCodeRuntimeValue.NumberValue
                )?.value
        }
        if (steps.any { it == null }) {
            return 0L
        }
        val arrowEnabled = (
            (
                (
                    attributes.properties["arrowhead"] as?
                        JessieCodeRuntimeValue.ObjectValue
                    )?.properties?.get("enabled") as?
                    JessieCodeRuntimeValue.BooleanValue
                )?.value
            ) ?: true
        return Curve3D.vectorFieldPointCount(
            xSteps = steps[0] ?: return 0L,
            ySteps = steps[1] ?: return 0L,
            zSteps = steps[2] ?: return 0L,
            arrowEnabled = arrowEnabled,
        )
    }

    private fun runtimeSurface3DMaximumCurvePointCount(
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Long {
        val type = (
            attributes.properties["type"] as?
                JessieCodeRuntimeValue.StringValue
            )?.value?.lowercase() ?: "wireframe"
        if (type != "wireframe") {
            return polyhedron3DCurvePointCount(
                runtimeSurface3DMaximumFaceVertexCount(attributes).toInt(),
            )
        }
        val stepsU = runtimeNonNegativeIntegerAttribute(
            attributes = attributes,
            name = "stepsu",
            default = Surface3D.DEFAULT_STEPS_U,
        ) ?: return 0L
        val stepsV = runtimeNonNegativeIntegerAttribute(
            attributes = attributes,
            name = "stepsv",
            default = Surface3D.DEFAULT_STEPS_V,
        ) ?: return 0L
        return surface3DWireframePointCount(stepsU, stepsV)
    }

    private fun runtimeSurface3DMaximumFaceVertexCount(
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Long {
        val type = (
            attributes.properties["type"] as?
                JessieCodeRuntimeValue.StringValue
            )?.value?.lowercase() ?: "wireframe"
        if (type == "wireframe") {
            return 0L
        }
        val tiling = (
            attributes.properties["tiling"] as?
                JessieCodeRuntimeValue.StringValue
            )?.value?.lowercase() ?: "rectangle"
        return if (tiling == "triangle") 3L else 4L
    }

    private fun jsonSurface3DMaximumCurvePointCount(
        attributes: JsonObject,
    ): Long {
        val type = (
            attributes["type"] as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)
            ?.content?.lowercase() ?: "wireframe"
        if (type != "wireframe") {
            return polyhedron3DCurvePointCount(
                jsonSurface3DMaximumFaceVertexCount(attributes),
            )
        }
        val stepsU = jsonNonNegativeIntegerAttribute(
            attributes = attributes,
            name = "stepsu",
            default = Surface3D.DEFAULT_STEPS_U,
        ) ?: return 0L
        val stepsV = jsonNonNegativeIntegerAttribute(
            attributes = attributes,
            name = "stepsv",
            default = Surface3D.DEFAULT_STEPS_V,
        ) ?: return 0L
        return surface3DWireframePointCount(stepsU, stepsV)
    }

    private fun jsonSurface3DMaximumFaceVertexCount(
        attributes: JsonObject,
    ): Int {
        val type = (
            attributes["type"] as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)
            ?.content?.lowercase() ?: "wireframe"
        if (type == "wireframe") {
            return 0
        }
        val tiling = (
            attributes["tiling"] as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)
            ?.content?.lowercase() ?: "rectangle"
        return if (tiling == "triangle") 3 else 4
    }

    private fun surface3DWireframePointCount(
        stepsU: Int,
        stepsV: Int,
    ): Long {
        val rowCount = maxOf(stepsU, 1).toLong() + 1L
        val columnCount = maxOf(stepsV, 1).toLong() + 1L
        return (
            if (stepsU > 0) rowCount * columnCount else 0L
            ) +
            (
                if (stepsV > 0) rowCount * columnCount else 0L
                ) +
            rowCount +
            columnCount
    }

    private fun polyhedron3DCurvePointCount(vertexCount: Int): Long =
        vertexCount.toLong() +
            if (vertexCount > 0 && vertexCount != 2) 1L else 0L

    private fun runtimePlaneMeshPointCount(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Long? {
        if (!runtimePlaneCreatesMesh(board, parents, attributes)) {
            return null
        }
        val transformed =
            parents.size == 5 &&
                (
                    (
                        parents.getOrNull(1) as?
                            JessieCodeRuntimeValue.ElementReference
                        )?.element is Plane3D ||
                        (
                            parents.getOrNull(1) as?
                                JessieCodeRuntimeValue.StringValue
                            )?.value?.let { board?.select(it) } is Plane3D
                    )
        val rangeIndexes = if (transformed) 3 to 4 else 4 to 5
        val meshAttributes = attributes.properties["mesh3d"] as?
            JessieCodeRuntimeValue.ObjectValue
            ?: JessieCodeRuntimeValue.ObjectValue(emptyMap())
        return runtimeMeshPointCount(
            rangeU = parents.getOrNull(rangeIndexes.first),
            rangeV = parents.getOrNull(rangeIndexes.second),
            attributes = meshAttributes,
        )
    }

    private fun runtimePlaneMaximumCurvePointCount(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Long? =
        runtimePlaneMeshPointCount(board, parents, attributes)
            ?: runtimePlaneMaximumFaceVertexCount(
                board = board,
                parents = parents,
                attributes = attributes,
            )?.toInt()?.let(::polyhedron3DCurvePointCount)

    private fun runtimePlaneMaximumFaceVertexCount(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Long? {
        if (!runtimePlaneCreatesSurface(board, parents, attributes)) {
            return null
        }
        val tiling = (
            attributes.properties["tiling"] as?
                JessieCodeRuntimeValue.StringValue
            )?.value ?: "rectangle"
        return if (tiling.lowercase() == "triangle") 3L else 4L
    }

    private fun runtimeMeshPointCount(
        rangeU: JessieCodeRuntimeValue?,
        rangeV: JessieCodeRuntimeValue?,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Long? {
        val firstRange = runtimeNumberRange(rangeU) ?: return null
        val secondRange = runtimeNumberRange(rangeV) ?: return null
        val stepWidthU = when (
            val value = attributes.properties["stepwidthu"]
        ) {
            null, JessieCodeRuntimeValue.UndefinedValue -> 1.0
            is JessieCodeRuntimeValue.NumberValue -> value.value
            else -> return null
        }
        val stepWidthV = when (
            val value = attributes.properties["stepwidthv"]
        ) {
            null, JessieCodeRuntimeValue.UndefinedValue -> 1.0
            is JessieCodeRuntimeValue.NumberValue -> value.value
            else -> return null
        }
        if (
            !stepWidthU.isFinite() ||
            stepWidthU <= 0.0 ||
            !stepWidthV.isFinite() ||
            stepWidthV <= 0.0
        ) {
            return null
        }
        return Mesh3D.requestedPointCount(
            rangeU = firstRange,
            rangeV = secondRange,
            stepWidthU = stepWidthU,
            stepWidthV = stepWidthV,
        )
    }

    private fun runtimeNumberRange(
        value: JessieCodeRuntimeValue?,
    ): DoubleArray? {
        val values = (
            value as? JessieCodeRuntimeValue.ArrayValue
            )?.values ?: return null
        if (values.size != 2) {
            return null
        }
        val first = (
            values[0] as? JessieCodeRuntimeValue.NumberValue
            )?.value ?: return null
        val second = (
            values[1] as? JessieCodeRuntimeValue.NumberValue
            )?.value ?: return null
        return doubleArrayOf(first, second)
    }

    private fun jsonPlaneMeshPointCount(
        source: ParsedObject,
    ): Long {
        val type = (
            source.attributes["type"] as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)?.content ?: "shader"
        if (type.lowercase() != "wireframe" || source.parents.size != 6) {
            return 0L
        }
        val meshAttributes = source.attributes["mesh3d"] as? JsonObject
            ?: JsonObject(emptyMap())
        return jsonMeshPointCount(
            rangeU = source.parents.getOrNull(4),
            rangeV = source.parents.getOrNull(5),
            attributes = meshAttributes,
        )
    }

    private fun jsonPlaneMaximumCurvePointCount(
        source: ParsedObject,
    ): Long =
        jsonPlaneMeshPointCount(source).takeIf { it > 0L }
            ?: polyhedron3DCurvePointCount(
                jsonPlaneMaximumFaceVertexCount(source),
            )

    private fun jsonPlaneMaximumFaceVertexCount(
        source: ParsedObject,
    ): Int {
        if (!jsonPlaneCreatesSurface(source)) {
            return 0
        }
        val tiling = (
            source.attributes["tiling"] as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)?.content ?: "rectangle"
        return if (tiling.lowercase() == "triangle") 3 else 4
    }

    private fun jsonMeshPointCount(
        rangeU: JsonElement?,
        rangeV: JsonElement?,
        attributes: JsonObject,
    ): Long {
        val firstRange = jsonNumberRange(rangeU) ?: return 0L
        val secondRange = jsonNumberRange(rangeV) ?: return 0L
        val stepWidthU = (
            attributes["stepwidthu"] as? JsonPrimitive
            )?.doubleOrNull ?: 1.0
        val stepWidthV = (
            attributes["stepwidthv"] as? JsonPrimitive
            )?.doubleOrNull ?: 1.0
        if (
            !stepWidthU.isFinite() ||
            stepWidthU <= 0.0 ||
            !stepWidthV.isFinite() ||
            stepWidthV <= 0.0
        ) {
            return 0L
        }
        return Mesh3D.requestedPointCount(
            rangeU = firstRange,
            rangeV = secondRange,
            stepWidthU = stepWidthU,
            stepWidthV = stepWidthV,
        )
    }

    private fun jsonNumberRange(value: JsonElement?): DoubleArray? {
        val values = value as? JsonArray ?: return null
        if (values.size != 2) {
            return null
        }
        val first = (values[0] as? JsonPrimitive)?.doubleOrNull
            ?: return null
        val second = (values[1] as? JsonPrimitive)?.doubleOrNull
            ?: return null
        return doubleArrayOf(first, second)
    }

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
        objectsById: Map<String, ParsedObject> = emptyMap(),
    ): GMResult<Unit, JsxGraphDocumentError> {
        if (
            sourceObject.type != "polygon" &&
            sourceObject.type != "polygonalchain" &&
            sourceObject.type != "parallelogram" &&
            sourceObject.type != "regularpolygon" &&
            sourceObject.type != "polygon3d" &&
            sourceObject.type != "polyhedron3d" &&
            sourceObject.type != "plane3d" &&
            sourceObject.type != "parametricsurface3d" &&
            sourceObject.type != "functiongraph3d"
        ) {
            return GMResult.Ok(Unit)
        }
        if (
            sourceObject.type == "polygon3d" ||
            sourceObject.type == "polyhedron3d" ||
            sourceObject.type == "plane3d" ||
            sourceObject.type == "parametricsurface3d" ||
            sourceObject.type == "functiongraph3d"
        ) {
            val actual =
                when (sourceObject.type) {
                    "polygon3d" -> jsonPolygon3DVertexCount(
                        source = sourceObject,
                        objectsById = objectsById,
                        visited = emptySet(),
                    )
                    "polyhedron3d" ->
                        jsonPolyhedron3DMaximumFaceVertexCount(sourceObject)
                    "plane3d" ->
                        jsonPlaneMaximumFaceVertexCount(sourceObject)
                    else ->
                        jsonSurface3DMaximumFaceVertexCount(
                            sourceObject.attributes,
                        )
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

    private fun jsonPolygon3DVertexCount(
        source: ParsedObject,
        objectsById: Map<String, ParsedObject>,
        visited: Set<String>,
    ): Int {
        if (source.id in visited) {
            return 0
        }
        val baseId = (
            source.parents.getOrNull(1) as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)?.content
        val base = baseId?.let(objectsById::get)
            ?.takeIf { it.type == "polygon3d" }
        if (base != null) {
            return (
                jsonPolygon3DVertexCount(
                    source = base,
                    objectsById = objectsById,
                    visited = visited + source.id,
                ) - 1
                ).coerceAtLeast(0)
        }
        val direct = source.parents.drop(1)
        if (direct.isEmpty()) {
            return 0
        }
        val nested =
            if (direct.size == 1) direct[0] as? JsonArray else null
        if (nested != null && nested.isNotEmpty()) {
            val isPointList = nested.all { value ->
                (value as? JsonPrimitive)
                    ?.takeIf(JsonPrimitive::isString)
                    ?.content
                    ?.let(objectsById::get)
                    ?.type == "point3d"
            }
            val isCoordinateList = nested.all { value ->
                val coordinates = value as? JsonArray
                    ?: return@all false
                (
                    coordinates.firstOrNull() as? JsonPrimitive
                    )?.doubleOrNull != null
            }
            if (isPointList || isCoordinateList) {
                return nested.size
            }
        }
        return direct.size
    }

    private fun jsonCurve3DPointCount(
        source: ParsedObject,
        objectsById: Map<String, ParsedObject>,
        visited: Set<String>,
    ): Int {
        if (source.id in visited) {
            return 0
        }
        val direct = source.parents.drop(1)
        val baseId = (
            direct.firstOrNull() as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)?.content
        val base = baseId?.let(objectsById::get)
            ?.takeIf { it.type == "curve3d" }
        if (base != null && direct.size == 2) {
            return jsonCurve3DPointCount(
                source = base,
                objectsById = objectsById,
                visited = visited + source.id,
            )
        }
        if (direct.size == 1) {
            val matrix = direct[0] as? JsonArray
            if (matrix != null) {
                return matrix.size
            }
        }
        if (direct.size == 4) {
            val xCoordinates = direct[0] as? JsonArray
            if (xCoordinates != null) {
                return xCoordinates.size
            }
        }
        return (
            source.attributes["numberpointshigh"] as? JsonPrimitive
            )?.intOrNull ?: Curve3D.DEFAULT_SAMPLE_COUNT
    }

    private fun jsonVectorField3DPointCount(
        source: ParsedObject,
    ): Long {
        val steps = (2..4).map { parentIndex ->
            (
                (
                    source.parents.getOrNull(parentIndex) as?
                        JsonArray
                    )?.getOrNull(1) as? JsonPrimitive
                )?.doubleOrNull
        }
        if (steps.any { it == null }) {
            return 0L
        }
        val arrowEnabled = (
            (
                (
                    source.attributes["arrowhead"] as?
                        JsonObject
                    )?.get("enabled") as? JsonPrimitive
                )?.booleanOrNull
            ) ?: true
        return Curve3D.vectorFieldPointCount(
            xSteps = steps[0] ?: return 0L,
            ySteps = steps[1] ?: return 0L,
            zSteps = steps[2] ?: return 0L,
            arrowEnabled = arrowEnabled,
        )
    }

    private fun validateTextLengthLimit(
        sourceObject: ParsedObject,
        limit: Int,
    ): GMResult<Unit, JsxGraphDocumentError> {
        if (
            sourceObject.type != "text" &&
            sourceObject.type != "text3d"
        ) {
            return GMResult.Ok(Unit)
        }
        val content = sourceObject.parents.lastOrNull()
        val actual = (content as? JsonPrimitive)?.let { primitive ->
            if (primitive.isString) {
                primitive.content.length
            } else {
                primitive.doubleOrNull
                    ?.let(JsNumberFormat::compact)
                    ?.length ?: 0
            }
        } ?: 0
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

    private fun axisSceneDefinition(line: Line): JsxGraphAxis2D? {
        val definition = line.axisDefinition ?: return null
        fun point(coordinates: DoubleArray): JsxGraphPoint2D? {
            val weight = coordinates.getOrNull(0) ?: return null
            val x = coordinates.getOrNull(1) ?: return null
            val y = coordinates.getOrNull(2) ?: return null
            if (
                !weight.isFinite() ||
                !x.isFinite() ||
                !y.isFinite() ||
                abs(weight) <= Mat.eps
            ) {
                return null
            }
            return JsxGraphPoint2D(x / weight, y / weight)
        }
        val originalPoint1 = point(definition.originalPoint1)
            ?: return null
        val originalPoint2 = point(definition.originalPoint2)
            ?: return null
        fun distance(value: AxisDistance): JsxGraphAxisDistance2D =
            when (value) {
                is AxisDistance.User ->
                    JsxGraphAxisDistance2D.User(value.value)
                is AxisDistance.Percent ->
                    JsxGraphAxisDistance2D.Percent(value.value)
                is AxisDistance.Fraction ->
                    JsxGraphAxisDistance2D.Fraction(value.value)
                is AxisDistance.Pixels ->
                    JsxGraphAxisDistance2D.Pixels(value.value)
            }
        val attributes = definition.attributes
        return JsxGraphAxis2D(
            originalPoint1 = originalPoint1,
            originalPoint2 = originalPoint2,
            position = attributes.position,
            anchor = attributes.anchor,
            anchorDistance = distance(attributes.anchorDist),
            ticksAutoPos = attributes.ticksAutoPos,
            ticksAutoPosThreshold =
                distance(attributes.ticksAutoPosThreshold),
        )
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

    private data class CurvePointUsage(
        val id: String,
        val requested: Long,
    )

    private fun sourceCurvePointUsages(
        element: GeometryElement,
    ): List<CurvePointUsage> =
        if (element is Curve3D) {
            listOf(
                CurvePointUsage(
                    id = element.id,
                    requested = maxOf(
                        element.requestedPointCount()
                            ?: element.numberPoints.toLong(),
                        element.numberPoints.toLong(),
                    ),
                ),
            )
        } else {
            sourceCurves(element).map { curve ->
                CurvePointUsage(
                    id = curve.id,
                    requested = maxOf(
                        curve.requestedPointCount()
                            ?: curve.numberPoints.toLong(),
                        curve.numberPoints.toLong(),
                    ),
                )
            }
        }

    private fun sourceCurves(element: GeometryElement): List<Curve> =
        when (element) {
            is Curve -> listOf(element)
            is Curve3D -> listOf(element.curve2D)
            is Circle3D -> listOf(element.curve.curve2D)
            is Sphere3D ->
                listOfNotNull(element.element2D as? Curve)
            is Surface3D -> buildList {
                add(element.curve2D)
                element.polyhedron?.faces?.mapTo(this, Face3D::curve2D)
            }
            is Face3D -> listOf(element.curve2D)
            is Polygon3D -> emptyList()
            is Polyhedron3D -> element.faces.map(Face3D::curve2D)
            is Plane3D -> buildList {
                add(element.outline2D)
                element.mesh3D?.let(::add)
                element.surface3D?.faces?.mapTo(this, Face3D::curve2D)
            }
            else -> emptyList()
        }

    private fun depthOrderedSourceElements(
        elements: List<CreatedSourceElement>,
    ): List<CreatedSourceElement> {
        val ordered = mutableListOf<CreatedSourceElement>()
        var index = 0
        while (index < elements.size) {
            val face = elements[index].element as? Face3D
            if (face == null) {
                ordered += elements[index]
                index += 1
                continue
            }
            val definition = face.polyhedron
            val group = mutableListOf<CreatedSourceElement>()
            while (
                index < elements.size &&
                (elements[index].element as? Face3D)
                    ?.polyhedron === definition
            ) {
                group += elements[index]
                index += 1
            }
            ordered += group.sortedBy {
                (it.element as Face3D).zIndex
            }
        }
        return ordered
    }

    private fun planeMeshSourceElement(
        sourceElement: CreatedSourceElement,
        mesh: Curve,
    ): CreatedSourceElement {
        val planeAttributes = sourceElement.source.attributes
        val nested = planeAttributes["mesh3d"] as? JsonObject
            ?: JsonObject(emptyMap())
        val planeVisible = (
            planeAttributes["visible"] as? JsonPrimitive
            )?.booleanOrNull ?: true
        val attributes = linkedMapOf<String, JsonElement>()
        attributes.putAll(nested)
        val nestedVisibility = nested["visible"]
        if (
            nestedVisibility == null ||
            (
                nestedVisibility is JsonPrimitive &&
                    nestedVisibility.isString &&
                    nestedVisibility.content.lowercase() == "inherit"
                )
        ) {
            attributes["visible"] = JsonPrimitive(planeVisible)
        }
        attributes["id"] = JsonPrimitive(mesh.id)
        attributes["name"] = JsonPrimitive(mesh.name)
        attributes["withlabel"] = JsonPrimitive(false)
        return CreatedSourceElement(
            source = sourceElement.source.copy(
                id = mesh.id,
                type = "mesh3d",
                attributes = JsonObject(attributes),
            ),
            element = mesh,
        )
    }

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
                        is Face3D -> FACE_3D_ATTRIBUTES
                        is Line3D -> LINE_ATTRIBUTES
                        is Plane3D -> PLANE_3D_ATTRIBUTES
                        is Surface3D -> SURFACE_3D_ATTRIBUTES
                        is Curve3D ->
                            CURVE_ATTRIBUTES +
                                if (element.isVectorField3D) {
                                    VECTOR_FIELD_SEMANTIC_ATTRIBUTES
                                } else {
                                    emptySet()
                                }
                        is Circle3D -> CIRCLE_3D_ATTRIBUTES
                        is Sphere3D -> SPHERE_3D_ATTRIBUTES
                        is Polygon3D -> POLYGON_3D_ATTRIBUTES
                        is Point3D -> POINT_ATTRIBUTES
                        is Point -> POINT_ATTRIBUTES
                        is Ticks -> TICKS_ATTRIBUTES
                        is Line ->
                            LINE_ATTRIBUTES +
                                if (
                                    element.type ==
                                    Const.OBJECT_TYPE_AXIS
                                ) {
                                    AXIS_ATTRIBUTES
                                } else {
                                    emptySet()
                                }
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
                                if (element.isTicks3D) {
                                    TICKS_3D_ATTRIBUTES
                                } else {
                                    emptySet()
                                } +
                                if (element.isMesh3D) {
                                    MESH_3D_ATTRIBUTES
                                } else {
                                    emptySet()
                                } +
                                if (
                                    element.isEllipse ||
                                    element.isHyperbola ||
                                    element.isParabola
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
                        is Text3D, is Text -> TEXT_ATTRIBUTES
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
                is Line3D -> listOf("point", "point1", "point2")
                is Plane3D ->
                    listOf("point", "point1", "point2", "point3")
                is Circle3D -> listOf("point")
                is Sphere3D -> listOf("center", "point")
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
                        element.isParabola ->
                            listOf("foci", "center", "line")
                        else -> emptyList()
                    }
                else -> emptyList()
            }
            for (name in nestedNames) {
                when (
                    val result = validateHiddenSubElement(
                        name = name,
                        supportsIdentity =
                            element is Line3D ||
                                element is Plane3D ||
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
                                        element.isHyperbola ||
                                        element.isParabola
                                    ),
                        supportsFixed =
                            element is Line3D ||
                                element is Plane3D ||
                                element is Curve &&
                                (
                                    element.isComb ||
                                        element.isEllipse ||
                                        (
                                            element.isParabola &&
                                                name != "line"
                                            )
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
            if (element is Curve3D && element.isVectorField3D) {
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
            if (element is Ticks) {
                when (
                    val result = validateNestedAttributes(
                        name = "label",
                        supported = TICKS_LABEL_ATTRIBUTES,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (
                element is Line &&
                element.type == Const.OBJECT_TYPE_AXIS
            ) {
                when (
                    val result = validateNestedAttributes(
                        name = "ticks",
                        supported =
                            COMMON_ATTRIBUTES +
                                TICKS_ATTRIBUTES +
                                setOf("ticks"),
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                val ticks = when (val result = nested("ticks")) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (
                    val result = ticks.validateNestedAttributes(
                        name = "label",
                        supported = TICKS_LABEL_ATTRIBUTES,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (element is Plane3D || element is Surface3D) {
                when (
                    val result = validateNestedAttributes(
                        name = "mesh3d",
                        supported = MESH_3D_ATTRIBUTES,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                when (
                    val result = validateNestedAttributes(
                        name = "polyhedron",
                        supported = POLYHEDRON_3D_ATTRIBUTES,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                val polyhedron = when (val result = nested("polyhedron")) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (
                    val result = polyhedron.validateNestedAttributes(
                        name = "shader",
                        supported = FACE_3D_SHADER_ATTRIBUTES,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                val shader = when (
                    val result = polyhedron.nested("shader")
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (
                    val result = shader.validateNestedAttributes(
                        name = "light",
                        supported = FACE_3D_LIGHT_ATTRIBUTES,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                when (
                    val result = validateNestedAttributes(
                        name = "colormap",
                        supported = PLANE_3D_COLORMAP_ATTRIBUTES,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (element is Face3D) {
                when (
                    val result = validateNestedAttributes(
                        name = "shader",
                        supported = FACE_3D_SHADER_ATTRIBUTES,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                val shader = when (val result = nested("shader")) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (
                    val result = shader.validateNestedAttributes(
                        name = "light",
                        supported = FACE_3D_LIGHT_ATTRIBUTES,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (element is Polygon3D) {
                when (
                    val result = validateNestedAttributes(
                        name = "vertices",
                        supported = COMMON_ATTRIBUTES + POINT_ATTRIBUTES,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                when (
                    val result = validateNestedAttributes(
                        name = "borders",
                        supported = COMMON_ATTRIBUTES,
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

        fun has(name: String): Boolean = name in attributes

        fun style(
            element: GeometryElement,
            inheritedVisibility: Boolean? = null,
        ): GMResult<JsxGraphElementStyle, JsxGraphDocumentError> {
            val defaultStroke = when (element) {
                is Line3D -> DEFAULT_LINE_3D_COLOR
                is Surface3D -> DEFAULT_STROKE_COLOR
                is Curve3D -> DEFAULT_STROKE_COLOR
                is Circle3D -> DEFAULT_STROKE_COLOR
                is Sphere3D -> DEFAULT_SPHERE_3D_STROKE_COLOR
                is Point3D -> DEFAULT_STROKE_COLOR
                is Point -> DEFAULT_POINT_COLOR
                is Ticks ->
                    if (element.elType == "hatch") {
                        DEFAULT_STROKE_COLOR
                    } else {
                        DEFAULT_TICKS_COLOR
                    }
                is Text3D, is Text -> DEFAULT_TEXT_COLOR
                is Line ->
                    if (element.type == Const.OBJECT_TYPE_AXIS) {
                        DEFAULT_AXIS_COLOR
                    } else {
                        DEFAULT_STROKE_COLOR
                    }
                is Curve ->
                    when {
                        element.isMesh3D -> DEFAULT_MESH_3D_COLOR
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
                is Face3D -> DEFAULT_FACE_3D_FILL_COLOR
                is Plane3D -> DEFAULT_PLANE_3D_FILL_COLOR
                is Polygon3D -> JsxGraphColor.Transparent
                is Sphere3D -> DEFAULT_SPHERE_3D_FILL_COLOR
                is Point3D -> DEFAULT_POINT_3D_COLOR
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
                val result = inheritableBoolean(
                    name = "visible",
                    default = true,
                    inherited = inheritedVisibility,
                )
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
                            element is Line3D -> 1.0
                            element is Surface3D -> 0.75
                            element is Curve3D -> 1.0
                            element is Circle3D -> 1.0
                            element is Sphere3D -> 1.0
                            element is Face3D -> 1.0
                            element is Polygon3D -> 1.0
                            element is Point3D -> 0.0
                            element is Line &&
                                element.type == Const.OBJECT_TYPE_AXIS -> 1.0
                            element is Ticks ->
                                if (element.elType == "hatch") 2.0 else 1.0
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
                    default =
                        if (element is Curve && element.isMesh3D) {
                            0.6
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
            val fillOpacity = when (
                val result = number(
                    "fillopacity",
                    default =
                        if (
                            element is Face3D ||
                                element is Sphere3D
                        ) {
                            0.4
                        } else if (
                            element is Plane3D ||
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
            val fillGradient = when (
                val result = fillGradient(element)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
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
                    fillGradient = fillGradient,
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

        private fun inheritableBoolean(
            name: String,
            default: Boolean,
            inherited: Boolean?,
        ): GMResult<Boolean, JsxGraphDocumentError> {
            val value = attributes[name]
                ?: return GMResult.Ok(inherited ?: default)
            val primitive = value as? JsonPrimitive
                ?: return invalid(name, "a boolean")
            primitive.booleanOrNull?.let { return GMResult.Ok(it) }
            if (
                inherited != null &&
                primitive.isString &&
                primitive.content.equals("inherit", ignoreCase = true)
            ) {
                return GMResult.Ok(inherited)
            }
            val expected =
                if (inherited == null) {
                    "a boolean"
                } else {
                    "a boolean or 'inherit'"
                }
            return invalid(name, expected)
        }

        fun requireExplicitFalse(
            name: String,
            defaultValue: String,
        ): GMResult<Unit, JsxGraphDocumentError> {
            val value = attributes[name]
                ?: return GMResult.Err(
                    unsupportedValue(name, defaultValue),
                )
            val primitive = value as? JsonPrimitive
                ?: return invalid(name, "false")
            primitive.booleanOrNull?.let { enabled ->
                return if (!enabled) {
                    GMResult.Ok(Unit)
                } else {
                    GMResult.Err(
                        unsupportedValue(name, "true"),
                    )
                }
            }
            if (
                primitive.isString &&
                primitive.content.equals("inherit", ignoreCase = true)
            ) {
                return GMResult.Err(
                    unsupportedValue(name, primitive.content),
                )
            }
            return invalid(name, "false")
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
                is Face3D -> DEFAULT_FACE_3D_LAYER
                is Line3D -> DEFAULT_LINE_3D_LAYER
                is Surface3D -> DEFAULT_CURVE_3D_LAYER
                is Curve3D -> DEFAULT_CURVE_3D_LAYER
                is Circle3D -> DEFAULT_CURVE_3D_LAYER
                is Sphere3D -> DEFAULT_CURVE_3D_LAYER
                is Plane3D -> DEFAULT_CURVE_LAYER
                is Polygon3D -> DEFAULT_POLYGON_3D_LAYER
                is Point3D -> DEFAULT_POINT_3D_LAYER
                is Point -> DEFAULT_POINT_LAYER
                is Text3D, is Text -> DEFAULT_TEXT_LAYER
                is Arc -> DEFAULT_ARC_LAYER
                is Ticks -> DEFAULT_TICKS_LAYER
                is Line ->
                    if (element.type == Const.OBJECT_TYPE_AXIS) {
                        DEFAULT_AXIS_LAYER
                    } else {
                        DEFAULT_LINE_LAYER
                    }
                is Circle -> DEFAULT_CIRCLE_LAYER
                is Sector -> DEFAULT_AREA_LAYER
                is Curve ->
                    if (element.isMesh3D) {
                        DEFAULT_MESH_3D_LAYER
                    } else {
                        DEFAULT_CURVE_LAYER
                    }
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

        // JSXGraph 1.13.3: src/options.js -> gradient*;
        // src/options3d.js -> Options3D.sphere3d.
        private fun fillGradient(
            element: GeometryElement,
        ): GMResult<JsxGraphFillGradient?, JsxGraphDocumentError> {
            if (element !is Sphere3D) {
                return GMResult.Ok(null)
            }
            val type = when (
                val result = string("gradient", default = "radial")
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (type == "none") {
                return GMResult.Ok(null)
            }
            if (type != "linear" && type != "radial") {
                return GMResult.Err(
                    unsupportedValue("gradient", type),
                )
            }
            val secondColor = when (
                val result = color(
                    "gradientsecondcolor",
                    DEFAULT_SPHERE_3D_STROKE_COLOR,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val secondOpacity = when (
                val result = number(
                    "gradientsecondopacity",
                    default = 1.0,
                    minimum = 0.0,
                    maximum = 1.0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val startOffset = when (
                val result = number(
                    "gradientstartoffset",
                    default = 0.0,
                    minimum = 0.0,
                    maximum = 1.0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val endOffset = when (
                val result = number(
                    "gradientendoffset",
                    default = 1.0,
                    minimum = 0.0,
                    maximum = 1.0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (type == "linear") {
                val angle = when (
                    val result = number("gradientangle", default = 0.0)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                return GMResult.Ok(
                    JsxGraphFillGradient.Linear(
                        secondColor = secondColor,
                        secondOpacity = secondOpacity,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        angle = angle,
                    ),
                )
            }

            fun normalized(
                name: String,
                default: Double,
            ): GMResult<Double, JsxGraphDocumentError> =
                number(
                    name = name,
                    default = default,
                    minimum = 0.0,
                    maximum = 1.0,
                )

            val centerX = when (
                val result = normalized("gradientcx", default = 0.5)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val centerY = when (
                val result = normalized("gradientcy", default = 0.5)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val radius = when (
                val result = normalized("gradientr", default = 0.5)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val focalX = when (
                val result = normalized("gradientfx", default = 0.7)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val focalY = when (
                val result = normalized("gradientfy", default = 0.3)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val focalRadius = when (
                val result = normalized("gradientfr", default = 0.0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return GMResult.Ok(
                JsxGraphFillGradient.Radial(
                    secondColor = secondColor,
                    secondOpacity = secondOpacity,
                    startOffset = startOffset,
                    endOffset = endOffset,
                    centerX = centerX,
                    centerY = centerY,
                    radius = radius,
                    focalX = focalX,
                    focalY = focalY,
                    focalRadius = focalRadius,
                ),
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
    private val DEFAULT_POINT_3D_COLOR =
        JsxGraphColor(red = 255, green = 255, blue = 0)
    private val DEFAULT_LINE_3D_COLOR =
        JsxGraphColor(red = 0, green = 0, blue = 0)
    private val DEFAULT_MESH_3D_COLOR =
        JsxGraphColor(red = 154, green = 154, blue = 154)
    private val DEFAULT_FACE_3D_FILL_COLOR =
        JsxGraphColor(red = 255, green = 255, blue = 0)
    private val DEFAULT_SPHERE_3D_STROKE_COLOR =
        JsxGraphColor(red = 0, green = 255, blue = 128)
    private val DEFAULT_SPHERE_3D_FILL_COLOR =
        JsxGraphColor(red = 255, green = 255, blue = 255)
    private val DEFAULT_PLANE_3D_FILL_COLOR =
        JsxGraphColor(red = 187, green = 187, blue = 187)
    private val DEFAULT_POLYGON_FILL_COLOR =
        JsxGraphColor(red = 240, green = 228, blue = 66)
    private val DEFAULT_TEXT_COLOR =
        JsxGraphColor(red = 0, green = 0, blue = 0)
    private val DEFAULT_TICKS_COLOR =
        JsxGraphColor(red = 0, green = 0, blue = 0)
    private val DEFAULT_ANGLE_COLOR =
        JsxGraphColor(red = 230, green = 159, blue = 0)
    private val DEFAULT_RIEMANN_FILL_COLOR =
        JsxGraphColor(red = 240, green = 228, blue = 66)
    private val DEFAULT_COMB_STROKE_COLOR =
        JsxGraphColor(red = 0, green = 0, blue = 255)
    private val DEFAULT_AXIS_COLOR =
        JsxGraphColor(red = 102, green = 102, blue = 102)
    private const val MIN_ARROW_TYPE = 1
    private const val MAX_ARROW_TYPE = 7
    private const val DEFAULT_ARROW_TYPE = 1
    private const val DEFAULT_ARROW_SIZE = 6.0
    private val DEFAULT_ARROW_HEAD = JsxGraphArrowHead(
        type = DEFAULT_ARROW_TYPE,
        size = DEFAULT_ARROW_SIZE,
        highlightSize = DEFAULT_ARROW_SIZE,
    )
    private val DEFAULT_AXIS_ARROW_HEAD = JsxGraphArrowHead(
        type = DEFAULT_ARROW_TYPE,
        size = 8.0,
        highlightSize = 8.0,
    )
    private const val DEFAULT_ELEMENT_LAYER = 0
    private const val DEFAULT_AXIS_LAYER = 2
    private const val DEFAULT_AREA_LAYER = 3
    private const val DEFAULT_CURVE_LAYER = 5
    private const val DEFAULT_POLYGON_BORDER_LAYER = 5
    private const val DEFAULT_CIRCLE_LAYER = 6
    private const val DEFAULT_LINE_LAYER = 7
    private const val DEFAULT_ARC_LAYER = 8
    private const val DEFAULT_POINT_LAYER = 9
    private const val DEFAULT_TICKS_LAYER = 2
    private const val DEFAULT_POINT_3D_LAYER = 13
    private const val DEFAULT_LINE_3D_LAYER = 12
    private const val DEFAULT_CURVE_3D_LAYER = 12
    private const val DEFAULT_POLYGON_3D_LAYER = 12
    private const val DEFAULT_MESH_3D_LAYER = 12
    private const val DEFAULT_FACE_3D_LAYER = 12
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
    private val AXIS_ATTRIBUTES = setOf(
        "position",
        "anchor",
        "anchordist",
        "ticksautopos",
        "ticksautoposthreshold",
        "withticks",
        "ticks",
    )
    private val PLANE_3D_ATTRIBUTES = setOf(
        "type",
        "tiling",
        "stepsu",
        "stepsv",
        "threepoints",
        "point",
        "point1",
        "point2",
        "point3",
        "mesh3d",
        "polyhedron",
        "colormap",
    )
    private val SURFACE_3D_ATTRIBUTES = setOf(
        "type",
        "tiling",
        "stepsu",
        "stepsv",
        "polyhedron",
        "colormap",
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
    private val CIRCLE_3D_ATTRIBUTES =
        CURVE_ATTRIBUTES + setOf("point")
    private val SPHERE_3D_ATTRIBUTES =
        CURVE_ATTRIBUTES +
            setOf(
                "center",
                "point",
                "gradient",
                "gradientangle",
                "gradientcx",
                "gradientcy",
                "gradientendoffset",
                "gradientfr",
                "gradientsecondcolor",
                "gradientsecondopacity",
                "gradientstartoffset",
                "gradientfx",
                "gradientfy",
                "gradientr",
            )
    private val MESH_3D_ATTRIBUTES =
        COMMON_ATTRIBUTES +
            CURVE_ATTRIBUTES +
            setOf("stepwidthu", "stepwidthv")
    private val FACE_3D_ATTRIBUTES =
        COMMON_ATTRIBUTES + setOf("linecap", "shader")
    private val POLYHEDRON_3D_ATTRIBUTES =
        FACE_3D_ATTRIBUTES + setOf("fillcolorarray")
    private val FACE_3D_SHADER_ATTRIBUTES = setOf(
        "enabled",
        "fixed",
        "type",
        "hue",
        "saturation",
        "minlightness",
        "maxlightness",
        "light",
    )
    private val FACE_3D_LIGHT_ATTRIBUTES =
        setOf("type", "az", "el", "bank", "dir")
    private val PLANE_3D_COLORMAP_ATTRIBUTES =
        setOf("min", "max", "s", "v")
    private val TICKS_3D_ATTRIBUTES = setOf(
        "ticksdistance",
        "majorheight",
        "minorticks",
        "tickendings",
        "drawlabels",
        "label",
    )
    private val TICKS_ATTRIBUTES = setOf(
        "anchor",
        "beautifulscientificticklabels",
        "clip",
        "digits",
        "drawlabels",
        "drawzero",
        "face",
        "ignoreinfinitetickendings",
        "includeboundaries",
        "insertticks",
        "label",
        "labels",
        "majorheight",
        "majortickendings",
        "maxlabellength",
        "minticksdistance",
        "minorheight",
        "minorticks",
        "precision",
        "scale",
        "scalesymbol",
        "tickendings",
        "ticksdistance",
        "ticksperlabel",
        "type",
        "useunicodeminus",
    )
    private val TICKS_LABEL_ATTRIBUTES =
        COMMON_ATTRIBUTES +
            setOf(
                "anchorx",
                "anchory",
                "fontsize",
                "fontunit",
                "offset",
            )
    private val CONIC_ATTRIBUTES = setOf("foci", "center", "line")
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
    private val AXES_3D_PLANE_ROLES = listOf(
        "xPlaneRear",
        "xPlaneFront",
        "yPlaneRear",
        "yPlaneFront",
        "zPlaneRear",
        "zPlaneFront",
    )
    private val TRANSFORMATION_CREATORS =
        setOf("transform", "transform3d")
    private val NON_SCENE_CREATORS = TRANSFORMATION_CREATORS

    private fun sceneElementOutputCount(element: GeometryElement): Int {
        val labelCount = (element as? Curve)
            ?.ticks3DDefinition
            ?.labelElements
            ?.size ?: 0
        val meshCount =
            if (element is Plane3D && element.mesh3D != null) 1 else 0
        return 1 + labelCount + meshCount
    }

    private fun createdSceneElementCount(
        elements: List<CreatedSourceElement>,
    ): Int = elements.sumOf { sceneElementOutputCount(it.element) }

    private fun sceneElementCount(creatorName: String): Int =
        when (creatorName) {
            "bisectorlines" -> 2
            "tangentto" -> 3
            "axis" -> 2
            in NON_SCENE_CREATORS -> 0
            else -> 1
        }

    private fun sceneElementCount(
        creatorName: String,
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Int {
        if (creatorName == "ticks3d") {
            return ticks3DSceneElementCount(
                length = (
                    parents.getOrNull(3) as?
                        JessieCodeRuntimeValue.NumberValue
                    )?.value,
                attributes = attributes,
            )
        }
        if (creatorName == "axes3d" || creatorName == "view3d") {
            val boundingBox = when (creatorName) {
                "view3d" -> runtimeView3DBoundingBox(parents)
                else -> runtimeView3D(
                    board = board,
                    value = parents.firstOrNull(),
                )?.bbox3D
            }
            return axes3DSceneElementCount(
                axesPosition = (
                    attributes.properties["axesposition"] as?
                        JessieCodeRuntimeValue.StringValue
                    )?.value,
                labelCount =
                    runtimeAxes3DLabelCount(attributes, boundingBox),
                planeChildCount =
                    runtimeAxes3DPlaneChildCount(attributes),
            )
        }
        if (creatorName == "plane3d") {
            val childCount = when {
                runtimePlaneCreatesMesh(board, parents, attributes) -> 1L
                runtimePlaneCreatesSurface(board, parents, attributes) ->
                    runtimePlaneSurfaceFaceCount(attributes)
                else -> 0L
            }
            return (1L + childCount)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        }
        if (
            creatorName == "parametricsurface3d" ||
            creatorName == "functiongraph3d"
        ) {
            val type = (
                attributes.properties["type"] as?
                    JessieCodeRuntimeValue.StringValue
                )?.value?.lowercase() ?: "wireframe"
            val childCount =
                if (type == "wireframe") {
                    0L
                } else {
                    runtimeSurface3DFaceCount(attributes)
                }
            return (1L + childCount)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        }
        if (creatorName == "polyhedron3d") {
            return runtimePolyhedron3DFaceVertexCounts(board, parents)
                ?.size ?: 0
        }
        return sceneElementCount(creatorName)
    }

    private fun sceneElementCount(
        source: ParsedObject,
        objectsById: Map<String, ParsedObject>,
    ): Int {
        if (source.type == "ticks3d") {
            return ticks3DSceneElementCount(
                length = (
                    source.parents.getOrNull(3) as? JsonPrimitive
                    )?.doubleOrNull,
                attributes = source.attributes,
            )
        }
        if (source.type == "axes3d" || source.type == "view3d") {
            val viewSource = if (source.type == "view3d") {
                source
            } else {
                (
                    source.parents.firstOrNull() as? JsonPrimitive
                    )?.takeIf(JsonPrimitive::isString)
                    ?.content
                    ?.let(objectsById::get)
            }
            return axes3DSceneElementCount(
                axesPosition = (
                    source.attributes["axesposition"] as? JsonPrimitive
                    )?.takeIf(JsonPrimitive::isString)?.content,
                labelCount = jsonAxes3DLabelCount(
                    attributes = source.attributes,
                    boundingBox = viewSource?.let(::jsonView3DBoundingBox),
                ),
                planeChildCount =
                    jsonAxes3DPlaneChildCount(source.attributes),
            )
        }
        if (source.type == "plane3d") {
            val childCount = when {
                jsonPlaneCreatesMesh(source, objectsById) -> 1L
                jsonPlaneCreatesSurface(source) ->
                    jsonPlaneSurfaceFaceCount(source.attributes)
                else -> 0L
            }
            return (1L + childCount)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        }
        if (
            source.type == "parametricsurface3d" ||
            source.type == "functiongraph3d"
        ) {
            val type = (
                source.attributes["type"] as? JsonPrimitive
                )?.takeIf(JsonPrimitive::isString)
                ?.content?.lowercase() ?: "wireframe"
            val childCount =
                if (type == "wireframe") {
                    0L
                } else {
                    jsonSurface3DFaceCount(source.attributes)
                }
            return (1L + childCount)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        }
        if (source.type == "polyhedron3d") {
            return jsonPolyhedron3DFaceCount(
                source = source,
                objectsById = objectsById,
                visited = emptySet(),
            )
        }
        return sceneElementCount(source.type)
    }

    private fun jsonPolyhedron3DFaceCount(
        source: ParsedObject,
        objectsById: Map<String, ParsedObject>,
        visited: Set<String>,
    ): Int {
        if (source.id in visited) {
            return 0
        }
        val directFaces = source.parents.getOrNull(2) as? JsonArray
        if (directFaces != null) {
            return directFaces.size
        }
        val baseId = (
            source.parents.getOrNull(1) as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)?.content ?: return 0
        val base = objectsById[baseId]
            ?.takeIf { it.type == "polyhedron3d" } ?: return 0
        return jsonPolyhedron3DFaceCount(
            source = base,
            objectsById = objectsById,
            visited = visited + source.id,
        )
    }

    private fun axes3DSceneElementCount(
        axesPosition: String?,
        labelCount: Long,
        planeChildCount: Long,
    ): Int {
        val memberCount = when (axesPosition?.lowercase() ?: "center") {
            "none" -> 18
            "center" -> 22
            else -> 24
        }
        return (memberCount.toLong() + labelCount + planeChildCount)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun runtimeAxes3DPlaneChildCount(
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Long =
        AXES_3D_PLANE_ROLES.sumOf { role ->
            val planeAttributes =
                attributes.properties[role.lowercase()] as?
                    JessieCodeRuntimeValue.ObjectValue
                    ?: JessieCodeRuntimeValue.ObjectValue(emptyMap())
            val configured =
                planeAttributes.properties["type"] as?
                    JessieCodeRuntimeValue.StringValue
            val defaultType =
                if (role.endsWith("Rear")) "shader" else "wireframe"
            if (
                (configured?.value ?: defaultType).lowercase() ==
                "wireframe"
            ) {
                1L
            } else {
                runtimePlaneSurfaceFaceCount(
                    attributes = planeAttributes,
                    defaultSteps = 10,
                )
            }
        }

    private fun jsonAxes3DPlaneChildCount(
        attributes: JsonObject,
    ): Long =
        AXES_3D_PLANE_ROLES.sumOf { role ->
            val planeAttributes =
                attributes[role.lowercase()] as? JsonObject
                    ?: JsonObject(emptyMap())
            val configured =
                planeAttributes["type"] as? JsonPrimitive
            val defaultType =
                if (role.endsWith("Rear")) "shader" else "wireframe"
            val type =
                configured?.takeIf(JsonPrimitive::isString)?.content
                    ?: defaultType
            if (type.lowercase() == "wireframe") {
                1L
            } else {
                jsonPlaneSurfaceFaceCount(
                    attributes = planeAttributes,
                    defaultSteps = 10,
                )
            }
        }

    private fun runtimePlaneCreatesMesh(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Boolean {
        val type = (
            attributes.properties["type"] as?
                JessieCodeRuntimeValue.StringValue
            )?.value ?: "shader"
        if (type.lowercase() != "wireframe") {
            return false
        }
        val rangeIndexes = when {
            parents.size == 6 -> 4 to 5
            parents.size == 5 &&
                runtimeView3D(board, parents[0]) != null &&
                (
                    parents[1] as?
                        JessieCodeRuntimeValue.ElementReference
                    )?.element is Plane3D -> 3 to 4
            parents.size == 5 &&
                (
                    parents[1] as? JessieCodeRuntimeValue.StringValue
                    )?.value?.let { board?.select(it) } is Plane3D -> 3 to 4
            else -> return false
        }
        return runtimeFiniteRange(parents[rangeIndexes.first]) &&
            runtimeFiniteRange(parents[rangeIndexes.second])
    }

    private fun runtimePlaneCreatesSurface(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Boolean {
        val type = (
            attributes.properties["type"] as?
                JessieCodeRuntimeValue.StringValue
            )?.value ?: "shader"
        if (type.lowercase() == "wireframe") {
            return false
        }
        val rangeIndexes = when {
            parents.size == 6 -> 4 to 5
            parents.size == 5 &&
                (
                    (
                        parents[1] as?
                            JessieCodeRuntimeValue.ElementReference
                        )?.element is Plane3D ||
                        (
                            parents[1] as?
                                JessieCodeRuntimeValue.StringValue
                            )?.value?.let { board?.select(it) } is Plane3D
                    ) -> 3 to 4
            else -> return false
        }
        return runtimeFiniteRange(parents[rangeIndexes.first]) &&
            runtimeFiniteRange(parents[rangeIndexes.second])
    }

    private fun runtimePlaneSurfaceFaceCount(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        defaultSteps: Int = 6,
    ): Long {
        val stepsU = runtimePositiveIntegerAttribute(
            attributes = attributes,
            name = "stepsu",
            default = defaultSteps,
        ) ?: return 0L
        val stepsV = runtimePositiveIntegerAttribute(
            attributes = attributes,
            name = "stepsv",
            default = defaultSteps,
        ) ?: return 0L
        val tiling = (
            attributes.properties["tiling"] as?
                JessieCodeRuntimeValue.StringValue
            )?.value ?: "rectangle"
        return planeSurfaceFaceCount(
            tiling = tiling,
            stepsU = stepsU,
            stepsV = stepsV,
        )
    }

    private fun runtimeSurface3DFaceCount(
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Long {
        val stepsU = runtimeNonNegativeIntegerAttribute(
            attributes = attributes,
            name = "stepsu",
            default = Surface3D.DEFAULT_STEPS_U,
        ) ?: return 0L
        val stepsV = runtimeNonNegativeIntegerAttribute(
            attributes = attributes,
            name = "stepsv",
            default = Surface3D.DEFAULT_STEPS_V,
        ) ?: return 0L
        val tiling = (
            attributes.properties["tiling"] as?
                JessieCodeRuntimeValue.StringValue
            )?.value ?: "rectangle"
        return planeSurfaceFaceCount(tiling, stepsU, stepsV)
    }

    private fun runtimePositiveIntegerAttribute(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: Int,
    ): Int? {
        val value = attributes.properties[name]
            ?: return default
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return default
        }
        val number = (value as? JessieCodeRuntimeValue.NumberValue)?.value
            ?: return null
        val integer = number.toInt()
        return integer.takeIf {
            number.isFinite() &&
                integer.toDouble() == number &&
                integer > 0
        }
    }

    private fun runtimeNonNegativeIntegerAttribute(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: Int,
    ): Int? {
        val value = attributes.properties[name]
            ?: return default
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return default
        }
        val number = (value as? JessieCodeRuntimeValue.NumberValue)?.value
            ?: return null
        val integer = number.toInt()
        return integer.takeIf {
            number.isFinite() &&
                integer.toDouble() == number &&
                integer >= 0
        }
    }

    private fun runtimeFiniteRange(
        value: JessieCodeRuntimeValue,
    ): Boolean {
        val values = (
            value as? JessieCodeRuntimeValue.ArrayValue
            )?.values ?: return false
        return values.size == 2 &&
            values.all { coordinate ->
                when (coordinate) {
                    is JessieCodeRuntimeValue.NumberValue ->
                        coordinate.value.isFinite()
                    is JessieCodeRuntimeValue.FunctionValue -> true
                    else -> false
                }
            }
    }

    private fun jsonPlaneCreatesMesh(
        source: ParsedObject,
        objectsById: Map<String, ParsedObject>,
    ): Boolean {
        val type = (
            source.attributes["type"] as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)?.content ?: "shader"
        if (type.lowercase() != "wireframe") {
            return false
        }
        val transformed = (
            source.parents.getOrNull(1) as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)
            ?.content
            ?.let(objectsById::get)
            ?.type == "plane3d"
        val rangeIndexes = when {
            source.parents.size == 6 -> 4 to 5
            source.parents.size == 5 && transformed -> 3 to 4
            else -> return false
        }
        return jsonFiniteRange(source.parents[rangeIndexes.first]) &&
            jsonFiniteRange(source.parents[rangeIndexes.second])
    }

    private fun jsonPlaneCreatesSurface(
        source: ParsedObject,
    ): Boolean {
        val type = (
            source.attributes["type"] as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)?.content ?: "shader"
        if (type.lowercase() == "wireframe") {
            return false
        }
        val rangeIndexes = when (source.parents.size) {
            6 -> 4 to 5
            5 -> 3 to 4
            else -> return false
        }
        return jsonFiniteRange(source.parents[rangeIndexes.first]) &&
            jsonFiniteRange(source.parents[rangeIndexes.second])
    }

    private fun jsonPlaneSurfaceFaceCount(
        attributes: JsonObject,
        defaultSteps: Int = 6,
    ): Long {
        val stepsU = jsonPositiveIntegerAttribute(
            attributes = attributes,
            name = "stepsu",
            default = defaultSteps,
        ) ?: return 0L
        val stepsV = jsonPositiveIntegerAttribute(
            attributes = attributes,
            name = "stepsv",
            default = defaultSteps,
        ) ?: return 0L
        val tiling = (
            attributes["tiling"] as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)?.content ?: "rectangle"
        return planeSurfaceFaceCount(
            tiling = tiling,
            stepsU = stepsU,
            stepsV = stepsV,
        )
    }

    private fun jsonSurface3DFaceCount(
        attributes: JsonObject,
    ): Long {
        val stepsU = jsonNonNegativeIntegerAttribute(
            attributes = attributes,
            name = "stepsu",
            default = Surface3D.DEFAULT_STEPS_U,
        ) ?: return 0L
        val stepsV = jsonNonNegativeIntegerAttribute(
            attributes = attributes,
            name = "stepsv",
            default = Surface3D.DEFAULT_STEPS_V,
        ) ?: return 0L
        val tiling = (
            attributes["tiling"] as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)?.content ?: "rectangle"
        return planeSurfaceFaceCount(tiling, stepsU, stepsV)
    }

    private fun jsonPositiveIntegerAttribute(
        attributes: JsonObject,
        name: String,
        default: Int,
    ): Int? {
        val number = (attributes[name] as? JsonPrimitive)?.doubleOrNull
            ?: return if (name in attributes) null else default
        val integer = number.toInt()
        return integer.takeIf {
            number.isFinite() &&
                integer.toDouble() == number &&
                integer > 0
        }
    }

    private fun jsonNonNegativeIntegerAttribute(
        attributes: JsonObject,
        name: String,
        default: Int,
    ): Int? {
        val number = (attributes[name] as? JsonPrimitive)?.doubleOrNull
            ?: return if (name in attributes) null else default
        val integer = number.toInt()
        return integer.takeIf {
            number.isFinite() &&
                integer.toDouble() == number &&
                integer >= 0
        }
    }

    private fun planeSurfaceFaceCount(
        tiling: String,
        stepsU: Int,
        stepsV: Int,
    ): Long {
        val horizontal = stepsU.toLong()
        val vertical = stepsV.toLong()
        if (tiling.lowercase() != "triangle") {
            return horizontal * vertical
        }
        val oddRows = (vertical + 1L) / 2L
        val evenRows = vertical / 2L
        return oddRows * (2L * (horizontal + 1L)) +
            evenRows * (2L * horizontal + 1L)
    }

    private fun jsonFiniteRange(value: JsonElement): Boolean {
        val values = value as? JsonArray ?: return false
        return values.size == 2 &&
            values.all { coordinate ->
                (coordinate as? JsonPrimitive)?.doubleOrNull?.isFinite() ==
                    true
            }
    }

    private fun ticks3DSceneElementCount(
        length: Double?,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Int {
        val drawsLabels = (
            attributes.properties["drawlabels"] as?
                JessieCodeRuntimeValue.BooleanValue
            )?.value ?: true
        val ticksDistance = (
            attributes.properties["ticksdistance"] as?
                JessieCodeRuntimeValue.NumberValue
            )?.value ?: 1.0
        return sceneElementCountWithTickLabels(
            length = length,
            ticksDistance = ticksDistance,
            drawsLabels = drawsLabels,
        )
    }

    private fun ticks3DSceneElementCount(
        length: Double?,
        attributes: JsonObject,
    ): Int {
        val drawsLabels = (
            attributes["drawlabels"] as? JsonPrimitive
            )?.booleanOrNull ?: true
        val ticksDistance = (
            attributes["ticksdistance"] as? JsonPrimitive
            )?.doubleOrNull ?: 1.0
        return sceneElementCountWithTickLabels(
            length = length,
            ticksDistance = ticksDistance,
            drawsLabels = drawsLabels,
        )
    }

    private fun sceneElementCountWithTickLabels(
        length: Double?,
        ticksDistance: Double,
        drawsLabels: Boolean,
    ): Int {
        val labels = if (drawsLabels && length != null) {
            Ticks3D.tickCount(length, ticksDistance)
        } else {
            0L
        }
        return (1L + labels)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun runtimeView3D(
        board: Board?,
        value: JessieCodeRuntimeValue?,
    ): View3D? =
        when (value) {
            is JessieCodeRuntimeValue.ElementReference ->
                value.element as? View3D
            is JessieCodeRuntimeValue.StringValue ->
                board?.select(value.value) as? View3D
            else -> null
        }

    private fun runtimeView3DBoundingBox(
        parents: List<JessieCodeRuntimeValue>,
    ): Array<DoubleArray>? {
        val dimensions = (
            parents.getOrNull(2) as? JessieCodeRuntimeValue.ArrayValue
            )?.values ?: return null
        val boundingBox = dimensions.map { dimension ->
            val values = (
                dimension as? JessieCodeRuntimeValue.ArrayValue
                )?.values ?: return null
            DoubleArray(values.size) { index ->
                (
                    values[index] as?
                        JessieCodeRuntimeValue.NumberValue
                    )?.value ?: return null
            }
        }
        return boundingBox.toTypedArray()
    }

    private fun runtimeAxes3DLabelCount(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        boundingBox: Array<DoubleArray>?,
    ): Long {
        val axesPosition = (
            attributes.properties["axesposition"] as?
                JessieCodeRuntimeValue.StringValue
            )?.value ?: "center"
        if (!axesPosition.equals("border", ignoreCase = true)) {
            return 0L
        }
        val box = boundingBox
            ?.takeIf {
                it.size == 3 && it.all { dimension -> dimension.size == 2 }
            } ?: return 0L
        return listOf("x", "y", "z").mapIndexed { index, direction ->
            val axis = attributes.properties["${direction}axisborder"] as?
                JessieCodeRuntimeValue.ObjectValue
            val ticks = axis?.properties?.get("ticks3d") as?
                JessieCodeRuntimeValue.ObjectValue
            val drawsLabels = (
                ticks?.properties?.get("drawlabels") as?
                    JessieCodeRuntimeValue.BooleanValue
                )?.value ?: true
            val ticksDistance = (
                ticks?.properties?.get("ticksdistance") as?
                    JessieCodeRuntimeValue.NumberValue
                )?.value ?: 1.0
            if (drawsLabels) {
                Ticks3D.tickCount(
                    length = box[index][1] - box[index][0],
                    step = ticksDistance,
                )
            } else {
                0L
            }
        }.saturatingSum()
    }

    private fun jsonView3DBoundingBox(
        source: ParsedObject,
    ): Array<DoubleArray>? {
        val dimensions = source.parents.getOrNull(2) as? JsonArray
            ?: return null
        val boundingBox = dimensions.map { dimension ->
            val values = dimension as? JsonArray ?: return null
            DoubleArray(values.size) { index ->
                (
                    values[index] as? JsonPrimitive
                    )?.doubleOrNull ?: return null
            }
        }
        return boundingBox.toTypedArray()
    }

    private fun jsonAxes3DLabelCount(
        attributes: JsonObject,
        boundingBox: Array<DoubleArray>?,
    ): Long {
        val axesPosition = (
            attributes["axesposition"] as? JsonPrimitive
            )?.takeIf(JsonPrimitive::isString)?.content ?: "center"
        if (!axesPosition.equals("border", ignoreCase = true)) {
            return 0L
        }
        val box = boundingBox
            ?.takeIf {
                it.size == 3 && it.all { dimension -> dimension.size == 2 }
            } ?: return 0L
        return listOf("x", "y", "z").mapIndexed { index, direction ->
            val axis = attributes["${direction}axisborder"] as? JsonObject
            val ticks = axis?.get("ticks3d") as? JsonObject
            val drawsLabels = (
                ticks?.get("drawlabels") as? JsonPrimitive
                )?.booleanOrNull ?: true
            val ticksDistance = (
                ticks?.get("ticksdistance") as? JsonPrimitive
                )?.doubleOrNull ?: 1.0
            if (drawsLabels) {
                Ticks3D.tickCount(
                    length = box[index][1] - box[index][0],
                    step = ticksDistance,
                )
            } else {
                0L
            }
        }.saturatingSum()
    }

    private fun Iterable<Long>.saturatingSum(): Long {
        var sum = 0L
        for (value in this) {
            if (value > Long.MAX_VALUE - sum) {
                return Long.MAX_VALUE
            }
            sum += value
        }
        return sum
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
    private val POLYGON_3D_ATTRIBUTES =
        POLYGON_ATTRIBUTES + setOf("vertices", "borders")
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
