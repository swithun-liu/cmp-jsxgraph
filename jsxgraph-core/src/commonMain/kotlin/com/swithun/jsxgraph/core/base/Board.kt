/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/board.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphChartTableProvider
import com.swithun.jsxgraph.core.JsxGraphLocaleNumberFormatter
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.utils.AutoDigitsValue
import com.swithun.jsxgraph.core.utils.Color
import com.swithun.jsxgraph.core.utils.EventEmitter
import com.swithun.jsxgraph.core.utils.EventHandler
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import com.swithun.jsxgraph.core.utils.Type
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sign

internal sealed interface BoardError {
    data class DuplicateElementId(val id: String) : BoardError

    data class InvalidBoundingBoxCoordinateCount(
        val count: Int,
    ) : BoardError

    data class UnsupportedConditionExpression(
        val expression: String,
    ) : BoardError

    data class ConditionExpressionCompile(
        val expression: String,
        val error: JessieCodeExpressionCompileError,
    ) : BoardError

    data class ConditionExpressionEvaluation(
        val expression: String,
        val error: JessieCodeRuntimeError,
    ) : BoardError

    data class InvalidConditionValue(
        val property: String,
        val expected: String,
        val actual: String,
    ) : BoardError

    data class ConditionColor(
        val property: String,
        val value: String,
        val error: com.swithun.jsxgraph.core.utils.ColorError,
    ) : BoardError

    data class ConditionPointStyle(
        val elementId: String,
        val value: Int,
        val error: PointError,
    ) : BoardError
}

private enum class BoardConditionProperty {
    X,
    Y,
    VISIBLE,
    POSITION,
    STROKE,
    STYLE,
    STROKE_WIDTH,
    FILL,
}

private data class BoardCondition(
    val element: GeometryElement,
    val property: BoardConditionProperty,
    val source: String,
    val expression: JessieCodeExpressionFunction,
)

internal enum class BoardZoomMode {
    RESET,
    KEEP,
    UPDATE,
}

internal data class BoardInputEvent(
    val screenX: Double,
    val screenY: Double,
    val pointerId: Long? = null,
    val shiftKey: Boolean = false,
    val controlKey: Boolean = false,
    val timestampMilliseconds: Long = 0L,
    val inputDevice: String = "mouse",
    val deltaY: Double = 0.0,
    val key: String? = null,
)

// JSXGraph 1.13.3: src/base/board.js -> touches.targets.
internal data class BoardTouchTarget(
    val pointerId: Long,
    var screenX: Double,
    var screenY: Double,
    var previousScreenX: Double = Double.NaN,
    var previousScreenY: Double = Double.NaN,
    val startPositions: List<DoubleArray> = emptyList(),
)

// JSXGraph 1.13.3: src/base/board.js -> touches.
internal data class BoardTouchedObject(
    val element: GeometryElement,
    val targets: MutableList<BoardTouchTarget>,
)

internal interface BoardHostAdapter {
    fun topLeft(): DoubleArray = doubleArrayOf(0.0, 0.0)
    fun size(): DoubleArray? = null

    fun resize(
        width: Double,
        height: Double,
    ) = Unit

    fun addEventHandlers() = Unit
    fun removeEventHandlers() = Unit
    fun startResizeObserver() = Unit
    fun stopResizeObserver() = Unit
    fun startIntersectionObserver() = Unit
    fun stopIntersectionObserver() = Unit
    fun setFullscreen(enabled: Boolean) = Unit
}

private object NoBoardHostAdapter : BoardHostAdapter

internal data class BoardUserLogEntry(
    val type: String,
    val elementId: String,
    val startMilliseconds: Long,
    var endMilliseconds: Long,
)

internal data class BoardAnimationFailure(
    val elementId: String,
    val reason: String,
)

internal fun interface BoardAnimationScheduler {
    fun schedule(
        intervalMilliseconds: Double,
        advance: () -> Boolean,
    )

    val runsSynchronously: Boolean
        get() = false
}

private object ImmediateBoardAnimationScheduler : BoardAnimationScheduler {
    override val runsSynchronously: Boolean = true

    override fun schedule(
        intervalMilliseconds: Double,
        advance: () -> Boolean,
    ) {
        while (advance()) {
            // Platform hosts may inject a frame-backed scheduler.
        }
    }
}

/**
 * Host-driven counterpart of the browser's setInterval animation loop.
 *
 * Production sessions advance this scheduler from their platform frame clock.
 * Core therefore stays free of coroutine and browser runtime dependencies.
 */
internal class HostDrivenBoardAnimationScheduler :
    BoardAnimationScheduler {
    private class ScheduledAnimation(
        val intervalMilliseconds: Double,
        val advance: () -> Boolean,
        var elapsedMilliseconds: Double = 0.0,
    )

    private val animations = mutableListOf<ScheduledAnimation>()

    internal val hasScheduledAnimations: Boolean
        get() = animations.isNotEmpty()

    override fun schedule(
        intervalMilliseconds: Double,
        advance: () -> Boolean,
    ) {
        val effectiveInterval =
            if (
                intervalMilliseconds.isFinite() &&
                intervalMilliseconds > 0.0
            ) {
                intervalMilliseconds
            } else {
                MINIMUM_INTERVAL_MILLISECONDS
            }
        animations += ScheduledAnimation(
            intervalMilliseconds = effectiveInterval,
            advance = advance,
        )
    }

    internal fun advance(elapsedMilliseconds: Double): Boolean {
        if (elapsedMilliseconds <= 0.0) {
            return animations.isNotEmpty()
        }
        for (animation in animations.toList()) {
            if (animation !in animations) {
                continue
            }
            animation.elapsedMilliseconds += elapsedMilliseconds
            if (
                animation.elapsedMilliseconds <
                animation.intervalMilliseconds
            ) {
                continue
            }
            animation.elapsedMilliseconds %=
                animation.intervalMilliseconds
            if (!animation.advance()) {
                animations.remove(animation)
            }
        }
        return animations.isNotEmpty()
    }

    private companion object {
        const val MINIMUM_INTERVAL_MILLISECONDS = 1.0
    }
}

/**
 * Translated platform-neutral JXG.Board state and lifecycle.
 *
 * Browser DOM ownership stays in host adapters; coordinate state, factories,
 * events, navigation, updates, animation, and object registries remain in
 * commonMain.
 */
internal class Board(
    originX: Double,
    originY: Double,
    unitX: Double,
    unitY: Double,
    internal var canvasWidth: Double = DEFAULT_CANVAS_WIDTH,
    internal var canvasHeight: Double = DEFAULT_CANVAS_HEIGHT,
    internal var updateQuality: Int = BOARD_QUALITY_HIGH,
    internal var zoomX: Double = 1.0,
    internal var zoomY: Double = 1.0,
    internal var keepAspectRatio: Boolean = false,
    internal val id: String = "jxgBoard1",
    internal val maxNameLength: Int = 1,
    boundingBox: DoubleArray = doubleArrayOf(
        -originX / (unitX * zoomX),
        originY / (unitY * zoomY),
        (canvasWidth - originX) / (unitX * zoomX),
        (originY - canvasHeight) / (unitY * zoomY),
    ),
    internal val defaultCurveMinimum: Double =
        (-DEFAULT_CANVAS_WIDTH * CURVE_DOMAIN_PADDING - originX) /
            (unitX * zoomX),
    internal val defaultCurveMaximum: Double =
        (
            DEFAULT_CANVAS_WIDTH * (1.0 + CURVE_DOMAIN_PADDING) -
                originX
            ) / (unitX * zoomX),
    internal val maxCurvePoints: Int = 10_000,
    internal var animationDelay: Double = DEFAULT_ANIMATION_DELAY,
    internal val animationScheduler: BoardAnimationScheduler =
        ImmediateBoardAnimationScheduler,
    internal var zoomFactorX: Double = DEFAULT_ZOOM_FACTOR,
    internal var zoomFactorY: Double = DEFAULT_ZOOM_FACTOR,
    internal var minimumZoom: Double = DEFAULT_MINIMUM_ZOOM,
    internal var maximumZoom: Double = DEFAULT_MAXIMUM_ZOOM,
    internal var maxBoundingBox: DoubleArray = doubleArrayOf(
        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY,
    ),
    internal var maximumFrameRate: Double = DEFAULT_MAXIMUM_FRAME_RATE,
    internal val hostAdapter: BoardHostAdapter = NoBoardHostAdapter,
    internal val localeNumberFormatter: JsxGraphLocaleNumberFormatter? = null,
    internal val chartTableProvider: JsxGraphChartTableProvider? = null,
) {
    // JSXGraph 1.13.3: src/options.js -> Board.pan / zoom / keyboard.
    internal var panEnabled: Boolean = true
    internal var panNeedsShift: Boolean = true
    internal var panNeedsControl: Boolean = false
    internal var panNeedsTwoFingers: Boolean = false
    internal var zoomEnabled: Boolean = true
    internal var zoomNeedsShift: Boolean = true
    internal var zoomNeedsControl: Boolean = false
    internal var zoomCenter: String = "auto"
    internal var zoomPinch: Boolean = true
    internal var zoomPinchHorizontal: Boolean = true
    internal var zoomPinchVertical: Boolean = true
    internal var zoomPinchSensitivity: Double = 7.0
    internal var selectionEnabled: Boolean = false
    internal var selectionNeedsShift: Boolean = false
    internal var selectionNeedsControl: Boolean = true
    internal var keyboardEnabled: Boolean = true
    internal var keyboardDeltaX: Double = DEFAULT_KEYBOARD_DELTA
    internal var keyboardDeltaY: Double = DEFAULT_KEYBOARD_DELTA
    internal var keyboardPanShift: Boolean = true
    internal var keyboardPanControl: Boolean = false

    private val initialZoomX = zoomX
    private val initialZoomY = zoomY
    private val initialBoundingBox = boundingBox.copyOf()
    private val events = EventEmitter(this)
    private val hooks =
        mutableListOf<Pair<String, EventHandler>?>()
    internal val dependentBoards = mutableListOf<Board>()
    internal val animationObjects =
        linkedMapOf<String, GeometryElement>()
    private var animationLoopScheduled: Boolean = false
    internal val attributes = linkedMapOf<String, Any?>(
        "animationdelay" to animationDelay,
        "boundingbox" to initialBoundingBox.copyOf(),
        "intl" to linkedMapOf<String, Any?>(
            "enabled" to false,
            "locale" to null,
        ),
        "keepaspectratio" to keepAspectRatio,
        "zoomx" to zoomX,
        "zoomy" to zoomY,
    )
    internal val userLog = mutableListOf<BoardUserLogEntry>()
    internal var currentColorBlindness: String = "none"
        private set
    internal val colorBlindnessErrors =
        linkedMapOf<String, com.swithun.jsxgraph.core.utils.ColorError>()
    internal var snapSizeX: Double = 1.0
        private set
    internal var snapSizeY: Double = 1.0
        private set
    internal var mode: Int = BOARD_MODE_NONE
        private set
    internal var dragDx: Double = 0.0
        private set
    internal var dragDy: Double = 0.0
        private set
    internal var dragPosition: DoubleArray = doubleArrayOf(0.0, 0.0)
        private set
    internal var dragOffset: DoubleArray = doubleArrayOf(0.0, 0.0)
        private set
    internal var activeDragElement: GeometryElement? = null
        private set
    internal val downObjects = mutableListOf<GeometryElement>()
    internal val clickObjects =
        linkedMapOf<String, GeometryElement>()
    internal val focusObjects = mutableListOf<String>()
    internal val pointerPositions =
        linkedMapOf<Long, BoardInputEvent>()
    internal val touches = mutableListOf<BoardTouchedObject>()
    internal var inputDevice: String = "mouse"
        private set
    internal var hasMouseHandlers: Boolean = false
        private set
    internal var hasTouchHandlers: Boolean = false
        private set
    internal var hasPointerHandlers: Boolean = false
        private set
    internal var hasWheelHandlers: Boolean = false
        private set
    internal var hasKeyboardHandlers: Boolean = false
        private set
    internal var hasResizeHandlers: Boolean = false
        private set
    internal var hasFullscreenHandlers: Boolean = false
        private set
    internal var resizeObserverStarted: Boolean = false
        private set
    internal var intersectionObserverStarted: Boolean = false
        private set
    internal var fullscreen: Boolean = false
        private set
    internal var infoboxVisible: Boolean = false
        private set
    internal var infoboxText: String = ""
        private set
    internal var infoboxElement: GeometryElement? = null
        private set
    internal var selectingMode: Boolean = false
        private set
    internal var isSelecting: Boolean = false
        private set
    internal val selectingBox = arrayOf(
        doubleArrayOf(0.0, 0.0),
        doubleArrayOf(0.0, 0.0),
    )
    internal val sketchPoints = arrayOf(
        mutableListOf<DoubleArray>(),
        mutableListOf<DoubleArray>(),
    )
    internal val isSketching = booleanArrayOf(false, false)
    internal var geonextCompatibilityMode: Boolean = false
        private set
    internal var xmlString: String = ""
    private var touchMoveLastMilliseconds: Long = 0L
    private var touchMoveLastId: Long? = null
    private var gesturePreviousDistance: Double = Double.NaN
    private var gesturePreviousScale: Double = 1.0
    private var gesturePreviousCoordinates: Array<DoubleArray>? = null
    private var previousGesture: String = "none"

    // JSXGraph: src/base/board.js -> _shiftKey / _ctrlKey.
    internal var shiftKeyPressed: Boolean = false
    internal var controlKeyPressed: Boolean = false

    internal class Origin(
        val usrCoords: DoubleArray,
        val scrCoords: DoubleArray,
    )

    // JSXGraph: src/base/board.js -> Board constructor origin initialization
    internal val origin = Origin(
        usrCoords = doubleArrayOf(1.0, 0.0, 0.0),
        scrCoords = doubleArrayOf(1.0, originX, originY),
    )

    // JSXGraph: src/base/board.js -> Board constructor unitX / unitY
    internal var unitX: Double = unitX * zoomX
    internal var unitY: Double = unitY * zoomY

    private val boundingBox = boundingBox.copyOf()

    internal val objects = linkedMapOf<String, GeometryElement>()
    internal val objectsList = mutableListOf<GeometryElement>()
    internal val elementsByName = linkedMapOf<String, GeometryElement>()
    // JSXGraph 1.13.3: src/base/board.js -> Board constructor /
    // highlightedObjects.
    internal val highlightedObjects =
        linkedMapOf<String, GeometryElement>()
    // JSXGraph 1.13.3: src/base/board.js -> Board constructor / groups.
    internal val groups = linkedMapOf<String, Group>()
    // JSXGraph 1.13.3: src/base/board.js -> Board constructor / grids.
    internal val grids = mutableListOf<Curve>()
    // JSXGraph 1.13.3: src/jsxgraph.js -> initBoard defaultAxes;
    // src/base/board.js -> setAttribute("axis" / "defaultaxes").
    internal var defaultAxes: BoardDefaultAxes? = null
    internal var defaultAxesError: BoardDefaultAxesError? = null
        private set
    internal var numObjects: Int = 0
        private set

    internal var needsFullUpdate: Boolean = false
        private set
    internal var inUpdate: Boolean = false
        private set
    internal var isSuspendedUpdate: Boolean = false
        private set
    private var conditions: List<BoardCondition> = emptyList()
    internal var conditionError: BoardError? = null
        private set

    // JSXGraph: src/base/board.js -> setId
    internal fun setId(
        element: GeometryElement,
        type: Int,
    ): GMResult<String, BoardError> = setId(element, type.toString())

    // JSXGraph: src/base/board.js -> setId
    internal fun setId(
        element: GeometryElement,
        type: String,
    ): GMResult<String, BoardError> {
        val creationIndex = numObjects
        val requestedId = element.id
        if (requestedId.isNotEmpty() && requestedId in objects) {
            return GMResult.Err(BoardError.DuplicateElementId(requestedId))
        }

        var elementId = requestedId
        if (elementId.isEmpty()) {
            val baseId = "$id$type$creationIndex"
            elementId = baseId
            var collisionIndex = 1
            while (elementId in objects) {
                elementId = "$baseId-$collisionIndex"
                collisionIndex += 1
            }
        }

        numObjects += 1
        element.id = elementId
        objects[elementId] = element
        element.positionInBoard = objectsList.size
        objectsList += element
        element.finalizeName()
        return GMResult.Ok(elementId)
    }

    internal fun elementById(id: String): GeometryElement? = objects[id]

    internal fun elementByName(name: String): GeometryElement? =
        elementsByName[name]

    internal fun groupById(id: String): Group? = groups[id]

    // JSXGraph 1.13.3: src/base/board.js -> generateId.
    internal fun generateId(): String =
        "$GENERATED_BOARD_ID_PREFIX${nextGeneratedBoardId++}"

    // JSXGraph 1.13.3: src/base/board.js ->
    // finalizeAdding / finalizeLabel.
    internal fun finalizeAdding(element: GeometryElement): Board {
        if (element.evalVisProp("visible") == false) {
            element.setDisplayRendNode(false)
        }
        return this
    }

    internal fun finalizeLabel(element: GeometryElement): Board {
        val label = element.elementLabel
        if (
            element.hasLabel &&
            label != null &&
            label.evalVisProp("islabel") != true &&
            label.evalVisProp("visible") == false
        ) {
            label.setDisplayRendNode(false)
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> checkFrameRate.
    internal fun checkFrameRate(event: BoardInputEvent): Boolean {
        var handleEvent = false
        if (
            event.pointerId != null &&
            touchMoveLastId != event.pointerId
        ) {
            handleEvent = true
            touchMoveLastId = event.pointerId
        }
        if (
            !handleEvent &&
            (
                event.timestampMilliseconds -
                    touchMoveLastMilliseconds
                ) * maximumFrameRate >= 1_000.0
        ) {
            handleEvent = true
        }
        if (handleEvent) {
            touchMoveLastMilliseconds = event.timestampMilliseconds
        }
        return handleEvent
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // getCoordsTopLeftCorner / getMousePosition.
    internal fun getCoordsTopLeftCorner(): DoubleArray =
        hostAdapter.topLeft().copyOf(2)

    internal fun getMousePosition(
        event: BoardInputEvent,
        touchIndex: Int? = null,
    ): DoubleArray {
        @Suppress("UNUSED_VARIABLE")
        val upstreamTouchIndex = touchIndex
        val topLeft = getCoordsTopLeftCorner()
        return doubleArrayOf(
            event.screenX - topLeft.getOrElse(0) { 0.0 },
            event.screenY - topLeft.getOrElse(1) { 0.0 },
        )
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // getPointLoc / getLocationOrigin.
    internal fun getPointLoc(
        position: DoubleArray,
        margin: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0, 0.0),
    ): IntArray {
        val coordinates =
            if (position.size == 2) {
                doubleArrayOf(1.0, position[0], position[1])
            } else {
                position
            }
        val normalizedMargin =
            if (margin.size == 1) {
                DoubleArray(4) { margin[0] }
            } else {
                DoubleArray(4) {
                    margin.getOrElse(it) { 0.0 }
                }
            }
        val bbox = getBoundingBox()
        val result = intArrayOf(0, 0)
        if (coordinates.getOrElse(1) { Double.NaN } >
            bbox[2] - normalizedMargin[1]
        ) {
            result[0] = 1
        }
        if (coordinates.getOrElse(1) { Double.NaN } <
            bbox[0] + normalizedMargin[3]
        ) {
            result[0] = -1
        }
        if (coordinates.getOrElse(2) { Double.NaN } >
            bbox[1] - normalizedMargin[0]
        ) {
            result[1] = 1
        }
        if (coordinates.getOrElse(2) { Double.NaN } <
            bbox[3] + normalizedMargin[2]
        ) {
            result[1] = -1
        }
        return result
    }

    internal fun getPointLoc(
        position: DoubleArray,
        margin: Double,
    ): IntArray = getPointLoc(position, doubleArrayOf(margin))

    internal fun getLocationOrigin(
        margin: DoubleArray =
            doubleArrayOf(0.0, 0.0, 0.0, 0.0),
    ): IntArray = getPointLoc(doubleArrayOf(0.0, 0.0), margin)

    internal fun getLocationOrigin(margin: Double): IntArray =
        getPointLoc(doubleArrayOf(0.0, 0.0), margin)

    // JSXGraph: src/base/board.js -> getBoundingBox
    internal fun getBoundingBox(): DoubleArray = boundingBox.copyOf()

    // JSXGraph 1.13.3: src/base/board.js -> setBoundingBox.
    internal fun setBoundingBox(
        bbox: DoubleArray,
        keepAspectRatio: Boolean = false,
        setZoom: BoardZoomMode = BoardZoomMode.RESET,
    ): GMResult<Board, BoardError> {
        if (bbox.size < BOUNDING_BOX_COORDINATE_COUNT) {
            return GMResult.Err(
                BoardError.InvalidBoundingBoxCoordinateCount(bbox.size),
            )
        }
        if (
            bbox[0] <
            maxBoundingBox[0] -
            com.swithun.jsxgraph.core.math.Mat.eps ||
            bbox[1] >
            maxBoundingBox[1] +
            com.swithun.jsxgraph.core.math.Mat.eps ||
            bbox[2] >
            maxBoundingBox[2] +
            com.swithun.jsxgraph.core.math.Mat.eps ||
            bbox[3] <
            maxBoundingBox[3] -
            com.swithun.jsxgraph.core.math.Mat.eps
        ) {
            return GMResult.Ok(this)
        }

        val previousUnitX = unitX
        val previousUnitY = unitY
        var offsetX = 0.0
        var offsetY = 0.0
        var zoomRatio = 1.0
        if (keepAspectRatio) {
            var ratio =
                if (this.keepAspectRatio) {
                    previousUnitX / previousUnitY
                } else {
                    1.0
                }
            if (ratio.isNaN()) {
                ratio = 1.0
            }
            if (setZoom == BoardZoomMode.KEEP) {
                zoomRatio = zoomX / zoomY
            }
            val width = bbox[2] - bbox[0]
            val height = bbox[1] - bbox[3]
            val previousWidth = previousUnitX * width
            val previousHeight = previousUnitY * height
            if (canvasWidth >= canvasHeight) {
                if (previousWidth >= previousHeight) {
                    unitY = canvasHeight / height
                    unitX = unitY * ratio
                } else {
                    unitY =
                        canvasHeight / abs(width) *
                            height.sign / zoomRatio
                    unitX = unitY * ratio
                }
            } else if (previousHeight > previousWidth) {
                unitX = canvasWidth / width
                unitY = unitX / ratio
            } else {
                unitX =
                    canvasWidth / abs(height) *
                        width.sign * zoomRatio
                unitY = unitX / ratio
            }
            offsetX = (canvasWidth / unitX - width) * 0.5
            offsetY = (canvasHeight / unitY - height) * 0.5
            this.keepAspectRatio = true
        } else {
            unitX = canvasWidth / (bbox[2] - bbox[0])
            unitY = canvasHeight / (bbox[1] - bbox[3])
            this.keepAspectRatio = false
        }

        origin.scrCoords[1] = -unitX * (bbox[0] - offsetX)
        origin.scrCoords[2] = unitY * (bbox[1] + offsetY)
        when (setZoom) {
            BoardZoomMode.UPDATE -> {
                zoomX *= unitX / previousUnitX
                zoomY *= unitY / previousUnitY
            }
            BoardZoomMode.RESET -> {
                zoomX = initialZoomX
                zoomY = initialZoomY
            }
            BoardZoomMode.KEEP -> Unit
        }
        updateBoundingBoxFromViewport()
        return GMResult.Ok(this)
    }

    private fun updateBoundingBoxFromViewport() {
        boundingBox[0] = -origin.scrCoords[1] / unitX
        boundingBox[1] = origin.scrCoords[2] / unitY
        boundingBox[2] =
            (canvasWidth - origin.scrCoords[1]) / unitX
        boundingBox[3] =
            (origin.scrCoords[2] - canvasHeight) / unitY
    }

    // JSXGraph 1.13.3: src/base/board.js -> initMoveOrigin.
    internal fun initMoveOrigin(
        screenX: Double,
        screenY: Double,
    ): Board {
        dragDx = screenX - origin.scrCoords[1]
        dragDy = screenY - origin.scrCoords[2]
        mode = BOARD_MODE_MOVE_ORIGIN
        updateQuality = BOARD_QUALITY_LOW
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> initMoveObject.
    internal fun initMoveObject(
        screenX: Double,
        screenY: Double,
        event: BoardInputEvent,
        type: String = event.inputDevice,
    ): List<GeometryElement> {
        shiftKeyPressed = event.shiftKey
        controlKeyPressed = event.controlKey
        val draggable = mutableListOf<GeometryElement>()
        for (element in objectsList) {
            val hit = element.hasPoint(screenX, screenY)
            if (
                element.visualPropertiesCalculated["visible"] == true &&
                hit
            ) {
                element.trigger(
                    listOf("${type}down", "down"),
                    listOf(event),
                )
                downObjects += element
            }
            if (
                hit &&
                element.isDraggable &&
                element.visualPropertiesCalculated["visible"] == true &&
                element.evalVisProp("fixed") != true
            ) {
                draggable += element
            }
        }
        val selected = draggable.maxWithOrNull(
            compareBy<GeometryElement> {
                (it.evalVisProp("layer") as? Number)?.toDouble()
                    ?: 0.0
            }.thenBy { it.positionInBoard },
        )
        if (selected != null) {
            activeDragElement = selected
            mode = BOARD_MODE_DRAG
            dragOffset =
                if (selected is CoordsElement) {
                    doubleArrayOf(
                        selected.coords.scrCoords[1] - screenX,
                        selected.coords.scrCoords[2] - screenY,
                    )
                } else {
                    doubleArrayOf(0.0, 0.0)
                }
            selected.highlight(force = true)
            trigger(
                events = listOf("mousehit", "hit"),
                arguments = listOf(event, selected),
            )
            return listOf(selected)
        }
        dragOffset = doubleArrayOf(0.0, 0.0)
        return emptyList()
    }

    // JSXGraph 1.13.3: src/base/board.js -> moveObject.
    internal fun moveObject(
        screenX: Double,
        screenY: Double,
        element: GeometryElement? = activeDragElement,
        event: BoardInputEvent,
        type: String = event.inputDevice,
    ): Board {
        val draggedElement = element ?: return this
        val oldPosition =
            if (draggedElement is CoordsElement) {
                draggedElement.coords.scrCoords.copyOfRange(1, 3)
            } else {
                dragPosition.copyOf()
            }
        dragPosition = doubleArrayOf(
            screenX + dragOffset[0],
            screenY + dragOffset[1],
        )
        shiftKeyPressed = event.shiftKey
        controlKeyPressed = event.controlKey
        if (draggedElement is CoordsElement) {
            draggedElement.setPosition(
                method = Const.COORDS_BY_SCREEN,
                coordinates = dragPosition,
            )
        } else {
            draggedElement.setPositionDirectlyBase(
                method = Const.COORDS_BY_SCREEN,
                coordinates = dragPosition,
                oldCoordinates = oldPosition,
            )
        }
        draggedElement.trigger(
            events = listOf("${type}drag", "drag"),
            arguments = listOf(event),
        )
        update(draggedElement)
        draggedElement.highlight(force = true)
        trigger(
            events = listOf("mousehit", "hit"),
            arguments = listOf(event, draggedElement),
        )
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> highlightElements.
    internal fun highlightElements(
        screenX: Double,
        screenY: Double,
        event: BoardInputEvent,
        target: Any? = null,
    ): Board {
        val overObjects = linkedSetOf<String>()
        for (element in objectsList) {
            if (
                element.visualPropertiesCalculated["visible"] == true &&
                element.hasPoint(screenX, screenY)
            ) {
                val coordinates = Coords(
                    method = Const.COORDS_BY_SCREEN,
                    coordinates = doubleArrayOf(screenX, screenY),
                    board = this,
                    emitter = false,
                ).usrCoords
                highlightInfobox(
                    userX = coordinates[1],
                    userY = coordinates[2],
                    element = element,
                )
                if (element.id !in highlightedObjects) {
                    overObjects += element.id
                    element.highlight()
                    trigger(
                        events = listOf("mousehit", "hit"),
                        arguments = listOf(event, element, target),
                    )
                }
                element.trigger(
                    events =
                        if (element.mouseover) {
                            listOf("mousemove", "move")
                        } else {
                            listOf("mouseover", "over")
                        },
                    arguments = listOf(event),
                )
                element.mouseover = true
            }
        }
        for (element in objectsList) {
            if (element.mouseover && element.id !in overObjects) {
                element.trigger(
                    events = listOf("mouseout", "out"),
                    arguments = listOf(event),
                )
                element.mouseover = false
            }
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> saveStartPos.
    internal fun saveStartPos(
        element: GeometryElement,
    ): List<DoubleArray> =
        when (element) {
            is Line -> listOf(
                element.point1.coords.usrCoords.copyOf(),
                element.point2.coords.usrCoords.copyOf(),
            )
            is Circle -> buildList {
                add(element.center.coords.usrCoords.copyOf())
                element.point2?.let {
                    add(it.coords.usrCoords.copyOf())
                }
            }
            is Polygon -> element.vertices
                .dropLast(1)
                .map { it.coords.usrCoords.copyOf() }
            is Sector -> listOf(
                element.point1.coords.usrCoords.copyOf(),
                element.point2.coords.usrCoords.copyOf(),
                element.point3.coords.usrCoords.copyOf(),
            )
            is CoordsElement ->
                listOf(element.coords.usrCoords.copyOf())
            is Curve -> element.points.firstOrNull()
                ?.usrCoords
                ?.copyOf()
                ?.let(::listOf)
                .orEmpty()
            else -> emptyList()
        }

    // JSXGraph 1.13.3: src/base/board.js ->
    // mouseOriginMoveStart / mouseOriginMove /
    // touchStartMoveOriginOneFinger / touchOriginMove.
    internal fun mouseOriginMoveStart(
        event: BoardInputEvent,
        panEnabled: Boolean = true,
    ): Boolean {
        if (!panEnabled) {
            return false
        }
        val position = getMousePosition(event)
        initMoveOrigin(position[0], position[1])
        return true
    }

    internal fun mouseOriginMove(event: BoardInputEvent): Boolean {
        if (mode != BOARD_MODE_MOVE_ORIGIN) {
            return false
        }
        val position = getMousePosition(event)
        moveOrigin(position[0], position[1], diff = true)
        return true
    }

    internal fun touchStartMoveOriginOneFinger(
        event: BoardInputEvent,
        panEnabled: Boolean = true,
        needsTwoFingers: Boolean = false,
    ): Boolean =
        if (panEnabled && !needsTwoFingers) {
            mouseOriginMoveStart(event)
        } else {
            false
        }

    internal fun touchOriginMove(event: BoardInputEvent): Boolean =
        mouseOriginMove(event)

    // JSXGraph 1.13.3: src/base/board.js -> suppressDefault.
    internal fun suppressDefault(event: BoardInputEvent?): Boolean {
        @Suppress("UNUSED_VARIABLE")
        val platformEvent = event
        return false
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // addEventHandlers / removeEventHandlers /
    // addPointerEventHandlers / removePointerEventHandlers /
    // addMouseEventHandlers / removeMouseEventHandlers /
    // addTouchEventHandlers / removeTouchEventHandlers /
    // addWheelEventHandlers / addKeyboardEventHandlers /
    // removeKeyboardEventHandlers / addResizeEventHandlers /
    // removeResizeEventHandlers / addFullscreenEventHandlers /
    // removeFullscreenEventHandlers.
    internal fun addEventHandlers(): Board {
        hostAdapter.addEventHandlers()
        hasPointerHandlers = true
        return this
    }

    internal fun removeEventHandlers(): Board {
        hostAdapter.removeEventHandlers()
        hasPointerHandlers = false
        hasMouseHandlers = false
        hasTouchHandlers = false
        return this
    }

    internal fun addPointerEventHandlers(): Board {
        hasPointerHandlers = true
        return this
    }

    internal fun removePointerEventHandlers(): Board {
        hasPointerHandlers = false
        pointerPositions.clear()
        return this
    }

    internal fun addMouseEventHandlers(): Board {
        hasMouseHandlers = true
        return this
    }

    internal fun removeMouseEventHandlers(): Board {
        hasMouseHandlers = false
        return this
    }

    internal fun addTouchEventHandlers(
        appleGestures: Boolean = false,
    ): Board {
        @Suppress("UNUSED_VARIABLE")
        val platformGestureMode = appleGestures
        hasTouchHandlers = true
        return this
    }

    internal fun removeTouchEventHandlers(): Board {
        hasTouchHandlers = false
        return this
    }

    internal fun addWheelEventHandlers(): Board {
        hasWheelHandlers = true
        return this
    }

    internal fun addKeyboardEventHandlers(): Board {
        hasKeyboardHandlers = true
        return this
    }

    internal fun removeKeyboardEventHandlers(): Board {
        hasKeyboardHandlers = false
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> addResizeEventHandlers.
    internal fun addResizeEventHandlers(): Board {
        hasResizeHandlers = true
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> removeResizeEventHandlers.
    internal fun removeResizeEventHandlers(): Board {
        hasResizeHandlers = false
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> addFullscreenEventHandlers.
    internal fun addFullscreenEventHandlers(): Board {
        hasFullscreenHandlers = true
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> removeFullscreenEventHandlers.
    internal fun removeFullscreenEventHandlers(): Board {
        hasFullscreenHandlers = false
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // _isRequiredKeyPressed / _isPointerRegistered /
    // _pointerStorePosition / _pointerRemoveTouches /
    // _pointerClearTouches / _getPointerInputDevice.
    internal fun _isRequiredKeyPressed(
        event: BoardInputEvent,
        action: String,
    ): Boolean {
        val enabled: Boolean
        val needsShift: Boolean
        val needsControl: Boolean
        when (action.lowercase()) {
            "pan" -> {
                enabled = panEnabled
                needsShift = panNeedsShift
                needsControl = panNeedsControl
            }
            "zoom" -> {
                enabled = zoomEnabled
                needsShift = zoomNeedsShift
                needsControl = zoomNeedsControl
            }
            "selection" -> {
                enabled = selectionEnabled
                needsShift = selectionNeedsShift
                needsControl = selectionNeedsControl
            }
            else -> return false
        }
        return enabled &&
            event.shiftKey == needsShift &&
            event.controlKey == needsControl
    }

    internal fun _isPointerRegistered(
        event: BoardInputEvent,
    ): Boolean =
        event.pointerId?.let(pointerPositions::containsKey) == true

    internal fun _pointerStorePosition(
        event: BoardInputEvent,
    ): Board {
        event.pointerId?.let { pointerPositions[it] = event }
        return this
    }

    internal fun _pointerRemoveTouches(
        event: BoardInputEvent,
    ): Board {
        event.pointerId?.let(pointerPositions::remove)
        return this
    }

    internal fun _pointerClearTouches(
        pointerId: Long? = null,
    ): Board {
        if (pointerId == null) {
            pointerPositions.clear()
        } else {
            pointerPositions.remove(pointerId)
        }
        return this
    }

    internal fun _getPointerInputDevice(
        event: BoardInputEvent,
    ): String =
        event.inputDevice
            .lowercase()
            .takeIf { it in setOf("touch", "mouse", "pen") }
            ?: "mouse"

    // JSXGraph 1.13.3: src/base/board.js -> pointerDownListener.
    internal fun pointerDownListener(
        event: BoardInputEvent,
        element: GeometryElement? = null,
        allowDefaultEventHandling: Boolean = false,
    ): Boolean {
        if (element == null && _isPointerRegistered(event)) {
            return false
        }
        inputDevice = _getPointerInputDevice(event)
        val position = getMousePosition(event)
        val selected =
            if (element != null && element.draggable()) {
                activeDragElement = element
                mode = BOARD_MODE_DRAG
                listOf(element)
            } else {
                initMoveObject(
                    screenX = position[0],
                    screenY = position[1],
                    event = event,
                    type = inputDevice,
                )
            }
        val pointerId = event.pointerId
        if (selected.isNotEmpty() && pointerId != null) {
            val target = selected.last()
            val touched = touches.firstOrNull {
                it.element === target
            } ?: BoardTouchedObject(
                element = target,
                targets = mutableListOf(),
            ).also(touches::add)
            touched.targets += BoardTouchTarget(
                pointerId = pointerId,
                screenX = position[0],
                screenY = position[1],
                startPositions = saveStartPos(target),
            )
            dehighlightAll()
            target.highlight(force = true)
        }
        if (selected.isEmpty() && inputDevice != "touch") {
            mouseOriginMoveStart(
                event,
                panEnabled = _isRequiredKeyPressed(event, "pan"),
            )
        }
        _pointerStorePosition(event)
        if (inputDevice == "touch" && selected.isEmpty()) {
            when {
                pointerPositions.size == 1 &&
                    mode == BOARD_MODE_NONE &&
                    panEnabled &&
                    !panNeedsTwoFingers ->
                    initMoveOrigin(position[0], position[1])
                pointerPositions.size == 2 &&
                    (
                        mode == BOARD_MODE_NONE ||
                            mode == BOARD_MODE_MOVE_ORIGIN
                        ) -> {
                    if (mode == BOARD_MODE_MOVE_ORIGIN) {
                        originMoveEnd()
                    }
                    gestureStartListener(pointerPositions.values.toList())
                }
            }
        }
        trigger(
            events = listOf(
                "touchstart",
                "down",
                "pointerdown",
                "MSPointerDown",
            ),
            arguments = listOf(event),
        )
        @Suppress("UNUSED_VARIABLE")
        val platformDefaultHandling = allowDefaultEventHandling
        return true
    }

    // JSXGraph 1.13.3: src/base/board.js -> pointerMoveListener.
    internal fun pointerMoveListener(
        event: BoardInputEvent,
    ): Boolean {
        if (
            _getPointerInputDevice(event) == "touch" &&
            event.pointerId != null &&
            !_isPointerRegistered(event)
        ) {
            return mode == BOARD_MODE_NONE
        }
        if (!checkFrameRate(event)) {
            return false
        }
        if (mode != BOARD_MODE_DRAG) {
            dehighlightAll()
            displayInfobox(false)
        }
        updateQuality = BOARD_QUALITY_LOW
        inputDevice = _getPointerInputDevice(event)
        val position = getMousePosition(event)
        when {
            mode == BOARD_MODE_DRAG && event.pointerId != null ->
                moveTouchedObject(
                    event = event,
                    screenX = position[0],
                    screenY = position[1],
                )
            mode == BOARD_MODE_MOVE_ORIGIN -> mouseOriginMove(event)
            else -> {
                _pointerStorePosition(event)
                if (
                    inputDevice == "touch" &&
                    pointerPositions.size == 2
                ) {
                    gestureChangeListener(
                        pointerPositions.values.toList(),
                    )
                }
                highlightElements(
                    screenX = position[0],
                    screenY = position[1],
                    event = event,
                )
            }
        }
        _pointerStorePosition(event)
        trigger(
            events = listOf("pointermove", "MSPointerMove", "move"),
            arguments = listOf(event, mode),
        )
        updateQuality = BOARD_QUALITY_HIGH
        return mode == BOARD_MODE_NONE
    }

    // JSXGraph 1.13.3: src/base/board.js -> pointerUpListener.
    internal fun pointerUpListener(
        event: BoardInputEvent,
    ): Boolean {
        trigger(
            events = listOf(
                "touchend",
                "up",
                "pointerup",
                "MSPointerUp",
            ),
            arguments = listOf(event),
        )
        val pointerId = event.pointerId
        if (pointerId != null) {
            val iterator = touches.iterator()
            while (iterator.hasNext()) {
                val touched = iterator.next()
                touched.targets.removeAll {
                    it.pointerId == pointerId
                }
                if (touched.targets.isEmpty()) {
                    iterator.remove()
                }
            }
        }
        _pointerRemoveTouches(event)
        originMoveEnd()
        update()
        var updateNeeded = false
        val upEvents = linkedSetOf(
            "touchend",
            "up",
            "pointerup",
            "MSPointerUp",
            "${inputDevice}up",
        ).toList()
        for (index in downObjects.indices.reversed()) {
            val element = downObjects[index]
            val stillTouched = touches.any {
                it.element.id == element.id
            }
            if (!stillTouched) {
                element.trigger(
                    events = upEvents,
                    arguments = listOf(event),
                )
                if (element !is CoordsElement) {
                    element.snapToGrid()
                    element.snapToPoints()
                    updateNeeded = true
                }
                downObjects.removeAt(index)
            }
        }
        activeDragElement = null
        if (inputDevice != "touch") {
            dehighlightAll()
        }
        if (updateNeeded) {
            update()
        }
        return true
    }

    // JSXGraph 1.13.3: src/base/board.js -> pointerMoveListener,
    // touches target loop.
    private fun moveTouchedObject(
        event: BoardInputEvent,
        screenX: Double,
        screenY: Double,
    ) {
        val pointerId = event.pointerId ?: return
        for (touched in touches) {
            val target = touched.targets.firstOrNull {
                it.pointerId == pointerId
            } ?: continue
            target.screenX = screenX
            target.screenY = screenY
            when (touched.targets.size) {
                1 ->
                    if (touched.element is CoordsElement) {
                        moveObject(
                            screenX = screenX,
                            screenY = screenY,
                            element = touched.element,
                            event = event,
                            type = inputDevice,
                        )
                    } else {
                        moveTouchedNonCoordsElement(
                            touched = touched,
                            target = target,
                            event = event,
                        )
                    }
                2 -> {
                    twoFingerMove(
                        touched = touched,
                        movedPointerId = pointerId,
                        event = event,
                    )
                    target.previousScreenX = screenX
                    target.previousScreenY = screenY
                }
            }
            return
        }
    }

    // JSXGraph 1.13.3: src/base/board.js -> moveObject,
    // non-CoordsElement branch.
    private fun moveTouchedNonCoordsElement(
        touched: BoardTouchedObject,
        target: BoardTouchTarget,
        event: BoardInputEvent,
    ) {
        if (
            target.previousScreenX.isFinite() &&
            target.previousScreenY.isFinite()
        ) {
            touched.element.setPositionDirectlyBase(
                method = Const.COORDS_BY_SCREEN,
                coordinates = doubleArrayOf(
                    target.screenX,
                    target.screenY,
                ),
                oldCoordinates = doubleArrayOf(
                    target.previousScreenX,
                    target.previousScreenY,
                ),
            )
        }
        target.previousScreenX = target.screenX
        target.previousScreenY = target.screenY
        touched.element.trigger(
            events = listOf("${inputDevice}drag", "drag"),
            arguments = listOf(event),
        )
        update(touched.element)
        touched.element.highlight(force = true)
        trigger(
            events = listOf("mousehit", "hit"),
            arguments = listOf(event, touched.element),
        )
    }

    // JSXGraph 1.13.3: src/base/board.js -> pointerLeaveListener.
    internal fun pointerLeaveListener(
        event: BoardInputEvent,
    ): Boolean {
        @Suppress("UNUSED_VARIABLE")
        val platformEvent = event
        displayInfobox(false)
        dehighlightAll()
        return true
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // _handleClicks / _handleDblClicks / pointerClickListener /
    // pointerDblClickListener / mouseClickListener /
    // mouseDblClickListener.
    internal fun _handleClicks(
        event: BoardInputEvent,
        eventNames: List<String>,
    ): Board {
        trigger(eventNames, listOf(event))
        for (element in clickObjects.values) {
            element.trigger(eventNames, listOf(event))
        }
        clickObjects.clear()
        return this
    }

    internal fun _handleDblClicks(
        event: BoardInputEvent,
        eventNames: List<String>,
    ): Board = _handleClicks(event, eventNames)

    internal fun pointerClickListener(event: BoardInputEvent): Board =
        _handleClicks(event, listOf("click", "pointerclick"))

    internal fun pointerDblClickListener(event: BoardInputEvent): Board =
        _handleDblClicks(event, listOf("dblclick", "pointerdblclick"))

    internal fun mouseClickListener(event: BoardInputEvent): Board =
        _handleClicks(event, listOf("click", "mouseclick"))

    internal fun mouseDblClickListener(event: BoardInputEvent): Board =
        _handleDblClicks(event, listOf("dblclick", "mousedblclick"))

    // JSXGraph 1.13.3: src/base/board.js ->
    // mouseDownListener / mouseMoveListener / mouseUpListener /
    // touchStartListener / touchMoveListener / touchEndListener.
    internal fun mouseDownListener(event: BoardInputEvent): Boolean =
        pointerDownListener(event.copy(inputDevice = "mouse"))

    internal fun mouseMoveListener(event: BoardInputEvent): Boolean =
        pointerMoveListener(event.copy(inputDevice = "mouse"))

    internal fun mouseUpListener(event: BoardInputEvent): Boolean =
        pointerUpListener(event.copy(inputDevice = "mouse"))

    internal fun touchStartListener(event: BoardInputEvent): Boolean =
        pointerDownListener(event.copy(inputDevice = "touch"))

    internal fun touchMoveListener(event: BoardInputEvent): Boolean =
        pointerMoveListener(event.copy(inputDevice = "touch"))

    internal fun touchEndListener(event: BoardInputEvent): Boolean =
        pointerUpListener(event.copy(inputDevice = "touch"))

    // JSXGraph 1.13.3: src/base/board.js -> mouseWheelListener.
    internal fun mouseWheelListener(event: BoardInputEvent): Board {
        if (!zoomEnabled || !_isRequiredKeyPressed(event, "zoom")) {
            return this
        }
        val user = getUsrCoordsOfMouse(event)
        if (event.deltaY < 0.0) {
            zoomIn(user[0], user[1])
        } else if (event.deltaY > 0.0) {
            zoomOut(user[0], user[1])
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // keyDownListener / keyFocusInListener / keyFocusOutListener.
    internal fun keyDownListener(
        event: BoardInputEvent,
        element: GeometryElement? =
            focusObjects.firstOrNull()?.let(::select),
    ): Boolean {
        if (!keyboardEnabled || event.key == "Tab") {
            return false
        }
        val panKeyPressed =
            (keyboardPanShift && event.shiftKey) ||
                (keyboardPanControl && event.controlKey)
        var handled = true
        if (panKeyPressed) {
            when (event.key) {
                "ArrowLeft" -> clickLeftArrow()
                "ArrowRight" -> clickRightArrow()
                "ArrowUp" -> clickUpArrow()
                "ArrowDown" -> clickDownArrow()
                "+" -> if (zoomEnabled) zoomIn() else handled = false
                "-" -> if (zoomEnabled) zoomOut() else handled = false
                "o", "O" ->
                    if (zoomEnabled) zoom100() else handled = false
                else -> handled = false
            }
        } else if (!event.shiftKey && !event.controlKey) {
            handled = moveElementFromKeyboard(event, element)
        } else {
            handled = false
        }
        update()
        return handled
    }

    private fun moveElementFromKeyboard(
        event: BoardInputEvent,
        element: GeometryElement?,
    ): Boolean {
        val target = element ?: return false
        var deltaX = keyboardDeltaX / unitX
        var deltaY = keyboardDeltaY / unitY
        val coordinates = target as? CoordsElement
        if (
            coordinates?.evalVisProp("snaptogrid") == true
        ) {
            val snapSizes = coordinates.getSnapSizes()
            deltaX = snapSizes[0]
            deltaY = snapSizes[1]
        } else if (
            coordinates?.evalVisProp("attracttogrid") == true
        ) {
            var distance =
                (
                    coordinates.evalVisProp("attractordistance")
                        as? Number
                    )?.toDouble() ?: 0.0
            if (
                coordinates.evalVisProp("attractorunit")
                    ?.toString()
                    ?.lowercase() == "screen"
            ) {
                distance /= unitX
            }
            deltaX = max(deltaX, 1.1 * distance)
            deltaY = max(deltaY, 1.1 * distance)
        }
        val direction = when (event.key) {
            "ArrowUp" -> doubleArrayOf(0.0, deltaY)
            "ArrowDown" -> doubleArrayOf(0.0, -deltaY)
            "ArrowLeft" -> doubleArrayOf(-deltaX, 0.0)
            "ArrowRight" -> doubleArrayOf(deltaX, 0.0)
            else -> return false
        }
        if (
            !target.isDraggable ||
            target.visualPropertiesCalculated["visible"] != true ||
            target.evalVisProp("fixed") == true ||
            (
                geonextCompatibilityMode &&
                    target !is Point &&
                    target !is Text
                )
        ) {
            return true
        }
        mode = BOARD_MODE_DRAG
        if (coordinates != null) {
            coordinates.setPosition(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(
                    coordinates.X() + direction[0],
                    coordinates.Y() + direction[1],
                ),
            )
            updateInfobox(coordinates)
        } else {
            displayInfobox(false)
            target.setPositionDirectlyBase(
                method = Const.COORDS_BY_USER,
                coordinates = direction,
                oldCoordinates = doubleArrayOf(0.0, 0.0),
            )
        }
        trigger(listOf("keymove", "move"), listOf(event, mode))
        target.trigger(listOf("keydrag", "drag"), listOf(event))
        mode = BOARD_MODE_NONE
        return true
    }

    internal fun keyFocusInListener(element: GeometryElement): Board {
        focusObjects.clear()
        focusObjects += element.id
        element.highlight(force = true)
        element.trigger(listOf("hit"), emptyList())
        updateInfobox(element)
        return this
    }

    internal fun keyFocusOutListener(element: GeometryElement): Board {
        @Suppress("UNUSED_VARIABLE")
        val upstreamTarget = element
        focusObjects.clear()
        dehighlightAll()
        displayInfobox(false)
        return this
    }

    /*
     * JSXGraph 1.13.3: src/base/board.js -> __evt__attribute,
     * __evt__down, __evt__mousedown, __evt__pendown,
     * __evt__pointerdown, __evt__touchstart, __evt__up,
     * __evt__mouseup, __evt__pointerup, __evt__touchend,
     * __evt__click, __evt__dblclick, __evt__mouseclick,
     * __evt__mousedblclick, __evt__pointerclick,
     * __evt__pointerdblclick, __evt__move, __evt__mousemove,
     * __evt__penmove, __evt__pointermove, __evt__touchmove,
     * __evt__keymove, __evt__hit, __evt__mousehit, __evt__update,
     * __evt__boundingbox, __evt__startselecting,
     * __evt__mousestartselecting, __evt__pointerstartselecting,
     * __evt__touchstartselecting, __evt__stopselecting,
     * __evt__mousestopselecting, __evt__pointerstopselecting,
     * __evt__touchstopselecting, __evt__moveselecting,
     * __evt__mousemoveselecting, __evt__pointermoveselecting,
     * __evt__touchmoveselecting, __evt.
     *
     * These methods are empty in JSXGraph and only declare the event API.
     */
    internal fun __evt__attribute(event: Any?) = consumeEvent(event)
    internal fun __evt__down(event: Any?) = consumeEvent(event)
    internal fun __evt__mousedown(event: Any?) = consumeEvent(event)
    internal fun __evt__pendown(event: Any?) = consumeEvent(event)
    internal fun __evt__pointerdown(event: Any?) = consumeEvent(event)
    internal fun __evt__touchstart(event: Any?) = consumeEvent(event)
    internal fun __evt__up(event: Any?) = consumeEvent(event)
    internal fun __evt__mouseup(event: Any?) = consumeEvent(event)
    internal fun __evt__pointerup(event: Any?) = consumeEvent(event)
    internal fun __evt__touchend(event: Any?) = consumeEvent(event)
    internal fun __evt__click(event: Any?) = consumeEvent(event)
    internal fun __evt__dblclick(event: Any?) = consumeEvent(event)
    internal fun __evt__mouseclick(event: Any?) = consumeEvent(event)
    internal fun __evt__mousedblclick(event: Any?) = consumeEvent(event)
    internal fun __evt__pointerclick(event: Any?) = consumeEvent(event)
    internal fun __evt__pointerdblclick(event: Any?) = consumeEvent(event)
    // JSXGraph 1.13.3: src/base/board.js -> __evt__move,
    // __evt__mousemove, __evt__penmove, __evt__pointermove,
    // __evt__touchmove, __evt__keymove.
    internal fun __evt__move(event: Any?) = consumeEvent(event)
    internal fun __evt__mousemove(event: Any?) = consumeEvent(event)
    internal fun __evt__penmove(event: Any?) = consumeEvent(event)
    internal fun __evt__pointermove(event: Any?) = consumeEvent(event)
    internal fun __evt__touchmove(event: Any?) = consumeEvent(event)
    internal fun __evt__keymove(event: Any?) = consumeEvent(event)

    // JSXGraph 1.13.3: src/base/board.js -> __evt__hit,
    // __evt__mousehit, __evt__update, __evt__boundingbox,
    // __evt__startselecting, __evt__mousestartselecting,
    // __evt__pointerstartselecting, __evt__touchstartselecting,
    // __evt__stopselecting, __evt__mousestopselecting,
    // __evt__pointerstopselecting, __evt__touchstopselecting,
    // __evt__moveselecting, __evt__mousemoveselecting,
    // __evt__pointermoveselecting, __evt__touchmoveselecting, __evt.
    internal fun __evt__hit(event: Any?) = consumeEvent(event)
    internal fun __evt__mousehit(event: Any?) = consumeEvent(event)
    internal fun __evt__update() = Unit
    internal fun __evt__boundingbox() = Unit
    internal fun __evt__startselecting() = Unit
    internal fun __evt__mousestartselecting() = Unit
    internal fun __evt__pointerstartselecting() = Unit
    internal fun __evt__touchstartselecting() = Unit
    internal fun __evt__stopselecting() = Unit
    internal fun __evt__mousestopselecting() = Unit
    internal fun __evt__pointerstopselecting() = Unit
    internal fun __evt__touchstopselecting() = Unit
    // JSXGraph 1.13.3: src/base/board.js -> __evt__moveselecting,
    // __evt__mousemoveselecting, __evt__pointermoveselecting,
    // __evt__touchmoveselecting, __evt.
    internal fun __evt__moveselecting() = Unit
    internal fun __evt__mousemoveselecting() = Unit
    internal fun __evt__pointermoveselecting() = Unit
    internal fun __evt__touchmoveselecting() = Unit
    internal fun __evt__() = Unit

    private fun consumeEvent(event: Any?) {
        @Suppress("UNUSED_VARIABLE")
        val upstreamEvent = event
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // gestureStartListener / gestureChangeListener.
    internal fun gestureStartListener(
        events: List<BoardInputEvent>,
    ): Boolean {
        if (events.size < 2) {
            return false
        }
        val first = getMousePosition(events[0])
        val second = getMousePosition(events[1])
        gesturePreviousDistance =
            com.swithun.jsxgraph.core.math.Mat.hypot(
                second[0] - first[0],
                second[1] - first[1],
            )
        gesturePreviousScale = 1.0
        gesturePreviousCoordinates = arrayOf(
            first.copyOf(),
            second.copyOf(),
        )
        previousGesture = "none"
        initMoveOrigin(first[0], first[1])
        mode = BOARD_MODE_ZOOM
        return gesturePreviousDistance.isFinite() &&
            gesturePreviousDistance > 0.0
    }

    internal fun gestureChangeListener(
        events: List<BoardInputEvent>,
    ): Boolean {
        if (mode != BOARD_MODE_ZOOM || events.size < 2) {
            return false
        }
        val first = getMousePosition(events[0])
        val second = getMousePosition(events[1])
        val previous = gesturePreviousCoordinates ?: return false
        val distance = com.swithun.jsxgraph.core.math.Mat.hypot(
            second[0] - first[0],
            second[1] - first[1],
        )
        if (
            !distance.isFinite() ||
            distance <= 0.0 ||
            !gesturePreviousDistance.isFinite() ||
            gesturePreviousDistance <= 0.0
        ) {
            return false
        }
        val firstDirection = doubleArrayOf(
            first[0] - previous[0][0],
            first[1] - previous[0][1],
        )
        val secondDirection = doubleArrayOf(
            second[0] - previous[1][0],
            second[1] - previous[1][1],
        )
        if (
            firstDirection.squaredLength() <
            MINIMUM_GESTURE_MOVEMENT_SQUARED &&
            secondDirection.squaredLength() <
            MINIMUM_GESTURE_MOVEMENT_SQUARED
        ) {
            return false
        }
        var isPinch = false
        val angle = Geometry.rad(
            firstDirection,
            doubleArrayOf(0.0, 0.0),
            secondDirection,
        )
        if (
            previousGesture != "pan" &&
            abs(angle) > PI * 0.2 &&
            abs(angle) < PI * 1.8
        ) {
            isPinch = true
        }
        val scale = distance / gesturePreviousDistance
        if (
            previousGesture != "pan" &&
            !isPinch &&
            (abs(scale) < 0.77 || abs(scale) > 1.3)
        ) {
            isPinch = true
        }
        val factor = scale / gesturePreviousScale
        gesturePreviousScale = scale
        gesturePreviousCoordinates = arrayOf(
            first.copyOf(),
            second.copyOf(),
        )
        val firstCoordinates = Coords(
            method = Const.COORDS_BY_SCREEN,
            coordinates = first,
            board = this,
            emitter = false,
        )
        if (panEnabled && panNeedsTwoFingers && !isPinch) {
            previousGesture = "pan"
            moveOrigin(first[0], first[1], diff = true)
            mode = BOARD_MODE_ZOOM
            return true
        }
        if (!zoomEnabled || abs(factor - 1.0) >= 0.5) {
            return false
        }
        var factorX = 1.0
        var factorY = 1.0
        var centerX = 0.0
        var centerY = 0.0
        var doZoom = false
        val deltaX = abs(first[0] - second[0])
        val deltaY = abs(first[1] - second[1])
        val theta = abs(atan2(deltaY, deltaX))
        val bound = PI * zoomPinchSensitivity / 90.0
        when {
            !keepAspectRatio &&
                zoomPinchHorizontal &&
                theta < bound -> {
                factorX = factor
                doZoom = true
            }
            !keepAspectRatio &&
                zoomPinchVertical &&
                abs(theta - PI * 0.5) < bound -> {
                factorY = factor
                doZoom = true
            }
            zoomPinch -> {
                factorX = factor
                factorY = factor
                centerX = firstCoordinates.usrCoords[1]
                centerY = firstCoordinates.usrCoords[2]
                doZoom = true
            }
        }
        if (!doZoom) {
            return false
        }
        val previousFactorX = zoomFactorX
        val previousFactorY = zoomFactorY
        zoomFactorX = factorX
        zoomFactorY = factorY
        if (zoomCenter == "board") {
            zoomIn()
        } else {
            zoomIn(centerX, centerY)
        }
        zoomFactorX = previousFactorX
        zoomFactorY = previousFactorY
        mode = BOARD_MODE_ZOOM
        return true
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // twoFingerMove / getTwoFingerTransform /
    // twoFingerTouchObject / twoFingerTouchCircle.
    internal fun twoFingerMove(
        touched: BoardTouchedObject,
        movedPointerId: Long,
        event: BoardInputEvent? = null,
    ): Board {
        val element = touched.element
        when (element) {
            is Line, is Polygon ->
                twoFingerTouchObject(
                    targets = touched.targets,
                    element = element,
                )
            is Circle ->
                twoFingerTouchCircle(
                    targets = touched.targets,
                    circle = element,
                    movedPointerId = movedPointerId,
                )
        }
        if (event != null) {
            element.trigger(
                events = listOf("touchdrag", "drag"),
                arguments = listOf(event),
            )
        }
        return this
    }

    internal fun getTwoFingerTransform(
        first: BoardTouchTarget,
        second: BoardTouchTarget,
        scalable: Boolean,
        rotatable: Boolean,
    ): DoubleArray {
        val previousFirst = Coords(
            Const.COORDS_BY_SCREEN,
            doubleArrayOf(
                first.previousScreenX,
                first.previousScreenY,
            ),
            this,
        ).usrCoords
        val previousSecond = Coords(
            Const.COORDS_BY_SCREEN,
            doubleArrayOf(
                second.previousScreenX,
                second.previousScreenY,
            ),
            this,
        ).usrCoords
        val currentFirst = Coords(
            Const.COORDS_BY_SCREEN,
            doubleArrayOf(first.screenX, first.screenY),
            this,
        ).usrCoords
        val currentSecond = Coords(
            Const.COORDS_BY_SCREEN,
            doubleArrayOf(second.screenX, second.screenY),
            this,
        ).usrCoords
        val deltaX = previousSecond[1] - previousFirst[1]
        val deltaY = previousSecond[2] - previousFirst[2]
        val currentDeltaX = currentSecond[1] - currentFirst[1]
        val currentDeltaY = currentSecond[2] - currentFirst[2]
        val squaredLength = deltaX * deltaX + deltaY * deltaY
        var cosine = (
            currentDeltaX * deltaX +
                currentDeltaY * deltaY
            ) / squaredLength
        var sine = (
            currentDeltaY * deltaX -
                currentDeltaX * deltaY
            ) / squaredLength
        if (!scalable) {
            val scale =
                com.swithun.jsxgraph.core.math.Mat.hypot(cosine, sine)
            cosine /= scale
            sine /= scale
        }
        if (!rotatable) {
            sine = 0.0
        }
        val translateX = 0.5 * (
            currentFirst[1] +
                currentSecond[1] -
                cosine * (previousFirst[1] + previousSecond[1]) +
                sine * (previousFirst[2] + previousSecond[2])
            )
        val translateY = 0.5 * (
            currentFirst[2] +
                currentSecond[2] -
                sine * (previousFirst[1] + previousSecond[1]) -
                cosine * (previousFirst[2] + previousSecond[2])
            )
        return doubleArrayOf(
            1.0, 0.0, 0.0,
            translateX, cosine, -sine,
            translateY, sine, cosine,
        )
    }

    internal fun twoFingerTouchObject(
        targets: List<BoardTouchTarget>,
        element: GeometryElement,
    ): Board {
        if (
            targets.size < 2 ||
            targets.take(2).any {
                !it.previousScreenX.isFinite() ||
                    !it.previousScreenY.isFinite()
            }
        ) {
            return this
        }
        val matrix = getTwoFingerTransform(
            first = targets[0],
            second = targets[1],
            scalable =
                (element.evalVisProp("scalable") as? Boolean) ?: true,
            rotatable =
                (element.evalVisProp("rotatable") as? Boolean) ?: true,
        )
        val transformation = when (
            val result = Transformation.create(
                type = "generic",
                parameters = matrix,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return this
        }
        when (element) {
            is Line -> {
                when (
                    transformation.applyOnce(
                        listOf(element.point1, element.point2)
                            .filter(Point::draggable),
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return this
                }
            }
            is Polygon -> {
                val vertices = element.vertices.dropLast(1)
                val snap =
                    element.evalVisProp("snaptogrid") == true ||
                        element.evalVisProp("snaptopoints") == true ||
                        vertices.any {
                            it.evalVisProp("snaptogrid") == true ||
                                it.evalVisProp("snaptopoints") == true ||
                                !it.draggable()
                        }
                if (!snap) {
                    when (
                        transformation.applyOnce(
                            vertices.filter(Point::draggable),
                        )
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return this
                    }
                }
            }
            else -> return this
        }
        update()
        element.highlight(force = true)
        return this
    }

    internal fun twoFingerTouchCircle(
        targets: List<BoardTouchTarget>,
        circle: Circle,
        movedPointerId: Long,
    ): Board {
        if (
            circle.method == "pointCircle" ||
            circle.method == "pointLine" ||
            targets.size < 2 ||
            targets.take(2).any {
                !it.previousScreenX.isFinite() ||
                    !it.previousScreenY.isFinite()
            }
        ) {
            return this
        }
        val movingIndex =
            if (targets[0].pointerId == movedPointerId) 0 else 1
        val fixedIndex = 1 - movingIndex
        val fixedTarget = targets[fixedIndex]
        val movingTarget = targets[movingIndex]
        val fixed = Coords(
            Const.COORDS_BY_SCREEN,
            doubleArrayOf(
                fixedTarget.previousScreenX,
                fixedTarget.previousScreenY,
            ),
            this,
        ).usrCoords.copyOfRange(1, 3)
        val previous = Coords(
            Const.COORDS_BY_SCREEN,
            doubleArrayOf(
                movingTarget.previousScreenX,
                movingTarget.previousScreenY,
            ),
            this,
        ).usrCoords.copyOfRange(1, 3)
        val current = Coords(
            Const.COORDS_BY_SCREEN,
            doubleArrayOf(
                movingTarget.screenX,
                movingTarget.screenY,
            ),
            this,
        ).usrCoords.copyOfRange(1, 3)
        val angle = Geometry.rad(previous, fixed, current)
        val translateToOrigin = transformation(
            type = "translate",
            parameters = doubleArrayOf(-fixed[0], -fixed[1]),
        ) ?: return this
        val rotate = transformation(
            type = "rotate",
            parameters = doubleArrayOf(angle),
        ) ?: return this
        if (translateToOrigin.melt(rotate) is GMResult.Err) {
            return this
        }
        var scale = Double.NaN
        val scalable =
            (circle.evalVisProp("scalable") as? Boolean) ?: true
        if (scalable) {
            scale =
                Geometry.distance(fixed, current) /
                    Geometry.distance(fixed, previous)
            val scaleTransformation = transformation(
                type = "scale",
                parameters = doubleArrayOf(scale, scale),
            ) ?: return this
            if (
                translateToOrigin.melt(scaleTransformation)
                is GMResult.Err
            ) {
                return this
            }
        }
        val translateBack = transformation(
            type = "translate",
            parameters = fixed,
        ) ?: return this
        if (translateToOrigin.melt(translateBack) is GMResult.Err) {
            return this
        }
        if (
            circle.center.draggable() &&
            translateToOrigin.applyOnce(circle.center) is GMResult.Err
        ) {
            return this
        }
        when (circle.method) {
            "twoPoints" -> {
                val point = circle.point2
                if (
                    point != null &&
                    point.draggable() &&
                    translateToOrigin.applyOnce(point) is GMResult.Err
                ) {
                    return this
                }
            }
            "pointRadius" ->
                circle.scaleStaticRadiusForTwoFinger(scale)
        }
        update(circle.center)
        circle.highlight(force = true)
        return this
    }

    private fun transformation(
        type: String,
        parameters: DoubleArray,
    ): Transformation? =
        when (val result = Transformation.create(type, parameters)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> null
        }

    private fun DoubleArray.squaredLength(): Double =
        sumOf { it * it }

    // JSXGraph 1.13.3: src/base/board.js ->
    // startSelectionMode / stopSelectionMode / _startSelecting /
    // _moveSelecting / _stopSelecting / _setSelectionPolygonFromBox /
    // _testForSelection / _createSelectionPolygon.
    internal fun startSelectionMode(): Board {
        selectingMode = true
        return this
    }

    internal fun stopSelectionMode(): Board {
        selectingMode = false
        isSelecting = false
        return this
    }

    internal fun _startSelecting(position: DoubleArray): Board {
        selectingBox[0] = position.copyOf(2)
        selectingBox[1] = position.copyOf(2)
        isSelecting = true
        return this
    }

    internal fun _moveSelecting(position: DoubleArray): Board {
        if (isSelecting) {
            selectingBox[1] = position.copyOf(2)
        }
        return this
    }

    internal fun _stopSelecting(
        event: BoardInputEvent? = null,
    ): List<DoubleArray> {
        @Suppress("UNUSED_VARIABLE")
        val platformEvent = event
        isSelecting = false
        return _setSelectionPolygonFromBox()
    }

    internal fun _setSelectionPolygonFromBox(): List<DoubleArray> {
        val first = selectingBox[0]
        val second = selectingBox[1]
        return listOf(
            doubleArrayOf(first[0], first[1]),
            doubleArrayOf(second[0], first[1]),
            doubleArrayOf(second[0], second[1]),
            doubleArrayOf(first[0], second[1]),
        )
    }

    internal fun _testForSelection(
        event: BoardInputEvent,
    ): Boolean {
        if (_isRequiredKeyPressed(event, "selection")) {
            startSelectionMode()
        }
        return selectingMode
    }

    internal fun _createSelectionPolygon(
        attributes: Map<String, Any?> = emptyMap(),
    ): List<DoubleArray> {
        @Suppress("UNUSED_VARIABLE")
        val portableAttributes = attributes
        return _setSelectionPolygonFromBox()
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // initSketchCurve / addToSketchCurve / finalizeSketchCurve.
    internal fun initSketchCurve(
        event: BoardInputEvent,
        index: Int = 0,
    ): Board {
        if (index !in sketchPoints.indices) {
            return this
        }
        sketchPoints[index].clear()
        sketchPoints[index] += getUsrCoordsOfMouse(event)
        isSketching[index] = true
        return this
    }

    internal fun addToSketchCurve(
        event: BoardInputEvent,
        index: Int = 0,
    ): Board {
        if (index in sketchPoints.indices && isSketching[index]) {
            sketchPoints[index] += getUsrCoordsOfMouse(event)
        }
        return this
    }

    internal fun finalizeSketchCurve(
        event: BoardInputEvent?,
        index: Int = 0,
    ): List<DoubleArray> {
        @Suppress("UNUSED_VARIABLE")
        val platformEvent = event
        if (index !in sketchPoints.indices) {
            return emptyList()
        }
        isSketching[index] = false
        return sketchPoints[index].map(DoubleArray::copyOf)
    }

    // JSXGraph 1.13.3: src/base/board.js -> originMoveEnd.
    internal fun originMoveEnd(): Board {
        mode = BOARD_MODE_NONE
        updateQuality = BOARD_QUALITY_HIGH
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> moveOrigin.
    internal fun moveOrigin(
        screenX: Double,
        screenY: Double,
        diff: Boolean = false,
    ): Board {
        val oldX = origin.scrCoords[1]
        val oldY = origin.scrCoords[2]
        origin.scrCoords[1] =
            if (diff) screenX - dragDx else screenX
        origin.scrCoords[2] =
            if (diff) screenY - dragDy else screenY
        val upperLeft = Coords(
            method = Const.COORDS_BY_SCREEN,
            coordinates = doubleArrayOf(0.0, 0.0),
            board = this,
            emitter = false,
        ).usrCoords
        val lowerRight = Coords(
            method = Const.COORDS_BY_SCREEN,
            coordinates = doubleArrayOf(canvasWidth, canvasHeight),
            board = this,
            emitter = false,
        ).usrCoords
        if (
            upperLeft[1] < maxBoundingBox[0] - com.swithun.jsxgraph.core.math.Mat.eps ||
            upperLeft[2] > maxBoundingBox[1] + com.swithun.jsxgraph.core.math.Mat.eps ||
            lowerRight[1] > maxBoundingBox[2] + com.swithun.jsxgraph.core.math.Mat.eps ||
            lowerRight[2] < maxBoundingBox[3] - com.swithun.jsxgraph.core.math.Mat.eps
        ) {
            origin.scrCoords[1] = oldX
            origin.scrCoords[2] = oldY
        }
        updateBoundingBoxFromViewport()
        updateCoords()
        fullUpdate()
        trigger(
            events = listOf("boundingbox"),
            arguments = listOf(this),
        )
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // clickLeftArrow / clickRightArrow / clickUpArrow / clickDownArrow.
    internal fun clickLeftArrow(): Board =
        moveOrigin(
            origin.scrCoords[1] + canvasWidth * 0.1,
            origin.scrCoords[2],
        )

    internal fun clickRightArrow(): Board =
        moveOrigin(
            origin.scrCoords[1] - canvasWidth * 0.1,
            origin.scrCoords[2],
        )

    internal fun clickUpArrow(): Board =
        moveOrigin(
            origin.scrCoords[1],
            origin.scrCoords[2] - canvasHeight * 0.1,
        )

    internal fun clickDownArrow(): Board =
        moveOrigin(
            origin.scrCoords[1],
            origin.scrCoords[2] + canvasHeight * 0.1,
        )

    // JSXGraph 1.13.3: src/base/board.js ->
    // getScrCoordsOfMouse / getUsrCoordsOfMouse.
    internal fun getScrCoordsOfMouse(
        screenX: Double,
        screenY: Double,
    ): DoubleArray = doubleArrayOf(screenX, screenY)

    internal fun getUsrCoordsOfMouse(
        event: BoardInputEvent,
    ): DoubleArray {
        val screen = getMousePosition(event)
        return Coords(
            method = Const.COORDS_BY_SCREEN,
            coordinates = screen,
            board = this,
            emitter = false,
        ).usrCoords.copyOfRange(1, 3)
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // getAllObjectsUnderMouse / getAllUnderMouse.
    internal fun getAllObjectsUnderMouse(
        event: BoardInputEvent,
    ): List<GeometryElement> {
        val screen = getMousePosition(event)
        return objectsList.filter { element ->
            element.visualPropertiesCalculated["visible"] == true &&
                element.hasPoint(screen[0], screen[1])
        }
    }

    internal fun getAllUnderMouse(
        event: BoardInputEvent,
    ): List<Any> =
        getAllObjectsUnderMouse(event) + listOf(getUsrCoordsOfMouse(event))

    // JSXGraph 1.13.3: src/base/board.js -> hasPoint.
    internal fun hasPoint(
        userX: Double,
        userY: Double,
    ): Boolean {
        val bbox = getBoundingBox()
        return userX > bbox[0] &&
            userX < bbox[2] &&
            userY < bbox[1] &&
            userY > bbox[3]
    }

    internal fun hasPoint(coordinates: Coords): Boolean =
        hasPoint(
            coordinates.usrCoords[1],
            coordinates.usrCoords[2],
        )

    // JSXGraph 1.13.3: src/reader/intergeo.js -> read coordinate reset.
    internal fun setCoordinateSystem(
        originX: Double,
        originY: Double,
        unitX: Double,
        unitY: Double,
    ): Board {
        origin.usrCoords[0] = 1.0
        origin.usrCoords[1] = 0.0
        origin.usrCoords[2] = 0.0
        origin.scrCoords[0] = 1.0
        origin.scrCoords[1] = originX
        origin.scrCoords[2] = originY
        this.unitX = unitX
        this.unitY = unitY
        updateBoundingBoxFromViewport()
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> resizeContainer.
    internal fun resizeContainer(
        width: Double,
        height: Double,
        dontSetHostSize: Boolean = false,
        dontSetBoundingBox: Boolean = false,
    ): Board {
        if (!width.isFinite() || !height.isFinite()) {
            return this
        }
        val oldWidth = canvasWidth
        val oldHeight = canvasHeight
        val oldBoundingBox =
            if (dontSetBoundingBox) null else getBoundingBox()
        canvasWidth = width
        canvasHeight = height
        if (!dontSetHostSize) {
            hostAdapter.resize(width, height)
        }
        if (oldBoundingBox != null) {
            setBoundingBox(
                bbox = oldBoundingBox,
                keepAspectRatio = keepAspectRatio,
                setZoom = BoardZoomMode.KEEP,
            )
        } else {
            moveOrigin(
                screenX = origin.scrCoords[1] +
                    (canvasWidth - oldWidth) * 0.5,
                screenY = origin.scrCoords[2] +
                    (canvasHeight - oldHeight) * 0.5,
            )
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // updateContainerDims / resizeListener / scrollListener.
    internal fun updateContainerDims(
        width: Double? = null,
        height: Double? = null,
    ): Board {
        val hostSize = hostAdapter.size()
        val resolvedWidth = width
            ?: hostSize?.getOrNull(0)
            ?: canvasWidth
        val resolvedHeight = height
            ?: hostSize?.getOrNull(1)
            ?: canvasHeight
        return resizeContainer(
            width = resolvedWidth,
            height = resolvedHeight,
            dontSetHostSize = true,
        )
    }

    internal fun resizeListener(): Board = updateContainerDims()

    internal fun scrollListener(
        event: BoardInputEvent? = null,
    ): Board {
        @Suppress("UNUSED_VARIABLE")
        val platformEvent = event
        getCoordsTopLeftCorner()
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // startResizeObserver / stopResizeObserver /
    // startIntersectionObserver / stopIntersectionObserver.
    internal fun startResizeObserver(): Board {
        hostAdapter.startResizeObserver()
        resizeObserverStarted = true
        return this
    }

    internal fun stopResizeObserver(): Board {
        hostAdapter.stopResizeObserver()
        resizeObserverStarted = false
        return this
    }

    internal fun startIntersectionObserver(): Board {
        hostAdapter.startIntersectionObserver()
        intersectionObserverStarted = true
        return this
    }

    internal fun stopIntersectionObserver(): Board {
        hostAdapter.stopIntersectionObserver()
        intersectionObserverStarted = false
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // printListener / printListenerMatch.
    internal fun printListener(): Board = resizeListener()

    internal fun printListenerMatch(matches: Boolean): Board {
        @Suppress("UNUSED_VARIABLE")
        val printMediaMatches = matches
        return resizeListener()
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // toFullscreen / fullscreenListener /
    // startFullscreenResizeObserver / stopFullscreenResizeObserver.
    internal fun toFullscreen(containerId: String? = null): Board {
        @Suppress("UNUSED_VARIABLE")
        val requestedContainer = containerId
        fullscreen = !fullscreen
        hostAdapter.setFullscreen(fullscreen)
        return this
    }

    internal fun fullscreenListener(enabled: Boolean): Board {
        fullscreen = enabled
        return resizeListener()
    }

    internal fun startFullscreenResizeObserver(
        node: Any? = null,
    ): Board {
        @Suppress("UNUSED_VARIABLE")
        val platformNode = node
        return startResizeObserver()
    }

    internal fun stopFullscreenResizeObserver(
        node: Any? = null,
    ): Board {
        @Suppress("UNUSED_VARIABLE")
        val platformNode = node
        return stopResizeObserver()
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // initInfobox / updateInfobox / displayInfobox / showInfobox /
    // highlightInfobox / highlightCustomInfobox.
    internal fun initInfobox(
        attributes: Map<String, Any?> = emptyMap(),
    ): Board {
        infoboxText =
            attributes["text"]?.toString().orEmpty()
        infoboxVisible = false
        infoboxElement = null
        return this
    }

    internal fun updateInfobox(element: GeometryElement): Board {
        infoboxElement = element
        infoboxText = element.name
        return this
    }

    internal fun displayInfobox(visible: Boolean): Board {
        infoboxVisible = visible
        return this
    }

    internal fun showInfobox(visible: Boolean): Board =
        displayInfobox(visible)

    internal fun highlightInfobox(
        userX: Double,
        userY: Double,
        element: GeometryElement,
    ): Board {
        // JSXGraph 1.13.3: src/3d/view3d.js -> highlightInfobox.
        val point3D = when (val source = element.evalVisProp("element3d")) {
            is Point3D -> source
            else -> element as? Point3D
        }
        if (point3D == null) {
            return highlightCustomInfobox(
                "(${JsNumberFormat.compact(userX)}, " +
                    "${JsNumberFormat.compact(userY)})",
                element,
            )
        }
        val foot = doubleArrayOf(1.0, 0.0, 0.0, point3D.Z())
        val projected = point3D.view.project2DTo3DPlane(
            coordinates2D = doubleArrayOf(1.0, userX, userY),
            normal = doubleArrayOf(1.0, 0.0, 0.0, 1.0),
            foot = foot,
        )
        if (!point3D.view.isInCube(projected)) {
            return highlightCustomInfobox("", point3D)
        }
        val prefix =
            if (mode == BOARD_MODE_DRAG) {
                if (point3D.view.isVerticalDrag()) {
                    "\u21C5 "
                } else {
                    "\u21C4 "
                }
            } else {
                ""
            }
        val digits = point3D.evalVisProp("infoboxdigits")
        val values = listOf(point3D.X(), point3D.Y(), point3D.Z())
            .joinToString(separator = " | ") {
                formatInfoboxCoordinate(point3D, it, digits)
            }
        return highlightCustomInfobox("$prefix($values)", point3D)
    }

    internal fun highlightCustomInfobox(
        text: String,
        element: GeometryElement,
    ): Board {
        infoboxText = text
        infoboxElement = element
        infoboxVisible = true
        return this
    }

    private fun formatInfoboxCoordinate(
        element: GeometryElement,
        value: Double,
        digits: Any?,
    ): String =
        if (element.useLocale()) {
            element.formatNumberLocaleText(
                value,
                (digits as? Number)?.toInt(),
            )
        } else if (digits == null || digits == "auto") {
            when (val formatted = Type.autoDigits(value)) {
                is AutoDigitsValue.Formatted -> formatted.value
                is AutoDigitsValue.Raw ->
                    JsNumberFormat.compact(formatted.value)
            }
        } else {
            val precision = (digits as? Number)?.toInt()
            if (precision == null) {
                JsNumberFormat.compact(value)
            } else {
                when (val formatted = Type.toFixed(value, precision)) {
                    is GMResult.Ok -> formatted.value
                    is GMResult.Err -> JsNumberFormat.compact(value)
                }
            }
        }

    // JSXGraph 1.13.3: src/base/board.js -> updateCSSTransforms.
    // Compose supplies pointer positions in the board's local coordinate
    // space, so there is no browser CSS matrix to invert.
    internal fun updateCSSTransforms(): Board = this

    // JSXGraph 1.13.3: src/base/board.js ->
    // initGeonextBoard / showDependencies / showXML.
    internal fun initGeonextBoard(): Board {
        geonextCompatibilityMode = true
        return this
    }

    internal fun showDependencies(): String =
        objects.values.joinToString(
            separator = "\n",
        ) { element ->
            "${element.id}: " +
                element.childElements.values.joinToString { child ->
                    "${child.id}(${child.name})"
                }
        }

    internal fun showXML(): String = xmlString

    // JSXGraph 1.13.3: src/base/board.js ->
    // addConditions / updateConditions.
    internal fun addConditions(
        expression: String,
    ): GMResult<Board, BoardError> {
        var remaining = expression
        var start = remaining.indexOf(CONDITION_OPEN_TAG)
        if (start < 0) {
            return GMResult.Ok(this)
        }
        val compiled = mutableListOf<BoardCondition>()
        while (start >= 0) {
            val end = remaining.indexOf(
                string = CONDITION_CLOSE_TAG,
                startIndex = start + CONDITION_OPEN_TAG.length,
            )
            if (end < 0) {
                return GMResult.Err(
                    BoardError.UnsupportedConditionExpression(expression),
                )
            }
            val term = remaining.substring(
                startIndex = start + CONDITION_OPEN_TAG.length,
                endIndex = end,
            )
            val assignment = term.indexOf('=')
            if (assignment <= 0) {
                return GMResult.Err(
                    BoardError.UnsupportedConditionExpression(term),
                )
            }
            val left = term.substring(0, assignment)
            val propertySeparator = left.indexOf('.')
            if (propertySeparator <= 0) {
                return GMResult.Err(
                    BoardError.UnsupportedConditionExpression(term),
                )
            }
            val name = left.substring(0, propertySeparator)
            val element = elementsByName[Type.unescapeHTML(name)]
            val propertyName = left
                .substring(propertySeparator + 1)
                .filterNot(Char::isWhitespace)
                .lowercase()
            val property = conditionProperty(propertyName)
            if (element != null && property != null) {
                val source = term.substring(assignment + 1)
                val function = when (
                    val result = JessieCodeExpressionFunction.compile(
                        source = source,
                        board = this,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        BoardError.ConditionExpressionCompile(
                            expression = source,
                            error = result.error,
                        ),
                    )
                }
                compiled += BoardCondition(
                    element = element,
                    property = property,
                    source = source,
                    expression = function,
                )
            }
            remaining = remaining.substring(
                end + CONDITION_CLOSE_TAG.length,
            )
            start = remaining.indexOf(CONDITION_OPEN_TAG)
        }
        conditions = compiled.toList()
        return when (val result = updateConditionsResult()) {
            is GMResult.Ok -> GMResult.Ok(this)
            is GMResult.Err -> result
        }
    }

    internal fun updateConditions(): Boolean =
        when (updateConditionsResult()) {
            is GMResult.Ok -> conditions.isNotEmpty()
            is GMResult.Err -> false
        }

    internal fun takeConditionError(): BoardError? {
        val error = conditionError
        conditionError = null
        return error
    }

    private fun updateConditionsResult(): GMResult<Boolean, BoardError> {
        if (conditions.isEmpty()) {
            conditionError = null
            return GMResult.Ok(false)
        }
        for (condition in conditions) {
            val value = when (
                val result = condition.expression.evaluate(emptyList())
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    val error = BoardError.ConditionExpressionEvaluation(
                        expression = condition.source,
                        error = result.error,
                    )
                    conditionError = error
                    return GMResult.Err(error)
                }
            }
            when (
                val result = applyCondition(
                    condition = condition,
                    value = value,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> {
                    conditionError = result.error
                    return result
                }
            }
        }
        prepareUpdate().updateElements()
        conditionError = null
        return GMResult.Ok(true)
    }

    private fun applyCondition(
        condition: BoardCondition,
        value: JessieCodeRuntimeValue,
    ): GMResult<Unit, BoardError> =
        when (condition.property) {
            BoardConditionProperty.X ->
                xyFun(condition, value, coordinateIndex = 2)
            BoardConditionProperty.Y ->
                xyFun(condition, value, coordinateIndex = 1)
            BoardConditionProperty.VISIBLE ->
                visFun(condition, value)
            BoardConditionProperty.STROKE ->
                colFun(condition, value, prefix = "stroke")
            BoardConditionProperty.POSITION ->
                posFun(condition, value)
            BoardConditionProperty.STYLE ->
                styleFun(condition, value)
            BoardConditionProperty.STROKE_WIDTH ->
                colFun(condition, value, prefix = "strokewidth")
            BoardConditionProperty.FILL ->
                colFun(condition, value, prefix = "fill")
        }

    // JSXGraph 1.13.3: src/base/board.js -> xyFun.
    private fun xyFun(
        condition: BoardCondition,
        value: JessieCodeRuntimeValue,
        coordinateIndex: Int,
    ): GMResult<Unit, BoardError> {
        val element = condition.element as? CoordsElement
            ?: return invalidConditionValue(
                condition = condition,
                expected = "coordinate element",
                value = value,
            )
        val coordinate = conditionNumber(value)
        val other = element.coords.usrCoords[coordinateIndex]
        val coordinates =
            if (coordinateIndex == 2) {
                doubleArrayOf(coordinate, other)
            } else {
                doubleArrayOf(other, coordinate)
            }
        element
            .setPositionDirectly(Const.COORDS_BY_USER, coordinates)
            .prepareUpdate()
            .update()
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/base/board.js -> visFun.
    private fun visFun(
        condition: BoardCondition,
        value: JessieCodeRuntimeValue,
    ): GMResult<Unit, BoardError> {
        condition.element.setAttribute(
            mapOf("visible" to conditionTruthy(value)),
        )
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/base/board.js -> colFun.
    private fun colFun(
        condition: BoardCondition,
        value: JessieCodeRuntimeValue,
        prefix: String,
    ): GMResult<Unit, BoardError> {
        if (prefix == "strokewidth") {
            condition.element.setAttribute(
                mapOf("strokewidth" to conditionNumber(value)),
            )
            return GMResult.Ok(Unit)
        }
        val color = (value as? JessieCodeRuntimeValue.StringValue)?.value
            ?: return invalidConditionValue(
                condition = condition,
                expected = "color string",
                value = value,
            )
        val split = when (val result = Color.rgba2rgbo(color)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                BoardError.ConditionColor(
                    property = prefix,
                    value = color,
                    error = result.error,
                ),
            )
        }
        condition.element.setAttribute(
            mapOf(
                "${prefix}color" to split.rgb,
                "${prefix}opacity" to split.opacity,
            ),
        )
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/base/board.js -> posFun.
    private fun posFun(
        condition: BoardCondition,
        value: JessieCodeRuntimeValue,
    ): GMResult<Unit, BoardError> {
        val element = condition.element as? CoordsElement
            ?: return invalidConditionValue(
                condition = condition,
                expected = "coordinate element",
                value = value,
            )
        element.position = conditionNumber(value)
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/base/board.js -> styleFun.
    private fun styleFun(
        condition: BoardCondition,
        value: JessieCodeRuntimeValue,
    ): GMResult<Unit, BoardError> {
        val point = condition.element as? Point
            ?: return invalidConditionValue(
                condition = condition,
                expected = "point",
                value = value,
            )
        val number = conditionNumber(value)
        if (!number.isFinite() || number != number.toInt().toDouble()) {
            return invalidConditionValue(
                condition = condition,
                expected = "integer point style",
                value = value,
            )
        }
        val style = number.toInt()
        return when (val result = point.setStyle(style)) {
            is GMResult.Ok -> GMResult.Ok(Unit)
            is GMResult.Err -> GMResult.Err(
                BoardError.ConditionPointStyle(
                    elementId = point.id,
                    value = style,
                    error = result.error,
                ),
            )
        }
    }

    private fun invalidConditionValue(
        condition: BoardCondition,
        expected: String,
        value: JessieCodeRuntimeValue,
    ): GMResult.Err<BoardError.InvalidConditionValue> =
        GMResult.Err(
            BoardError.InvalidConditionValue(
                property = condition.property.name.lowercase(),
                expected = expected,
                actual = conditionValueType(value),
            ),
        )

    private fun conditionProperty(
        property: String,
    ): BoardConditionProperty? =
        when (property) {
            "x" -> BoardConditionProperty.X
            "y" -> BoardConditionProperty.Y
            "visible" -> BoardConditionProperty.VISIBLE
            "position" -> BoardConditionProperty.POSITION
            "stroke" -> BoardConditionProperty.STROKE
            "style" -> BoardConditionProperty.STYLE
            "strokewidth" -> BoardConditionProperty.STROKE_WIDTH
            "fill" -> BoardConditionProperty.FILL
            else -> null
        }

    private fun conditionNumber(value: JessieCodeRuntimeValue): Double =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue -> value.value
            is JessieCodeRuntimeValue.BooleanValue ->
                if (value.value) 1.0 else 0.0
            is JessieCodeRuntimeValue.StringValue ->
                if (value.value.trim().isEmpty()) {
                    0.0
                } else {
                    value.value.toDoubleOrNull() ?: Double.NaN
                }
            JessieCodeRuntimeValue.NullValue -> 0.0
            JessieCodeRuntimeValue.UndefinedValue,
            is JessieCodeRuntimeValue.ArrayValue,
            is JessieCodeRuntimeValue.ObjectValue,
            is JessieCodeRuntimeValue.FunctionValue,
            is JessieCodeRuntimeValue.BoardReference,
            is JessieCodeRuntimeValue.TransformationReference,
            is JessieCodeRuntimeValue.CompositionReference,
            is JessieCodeRuntimeValue.ElementReference,
            -> Double.NaN
        }

    private fun conditionTruthy(value: JessieCodeRuntimeValue): Boolean =
        when (value) {
            JessieCodeRuntimeValue.NullValue,
            JessieCodeRuntimeValue.UndefinedValue,
            -> false
            is JessieCodeRuntimeValue.BooleanValue -> value.value
            is JessieCodeRuntimeValue.NumberValue ->
                value.value != 0.0 && !value.value.isNaN()
            is JessieCodeRuntimeValue.StringValue ->
                value.value.isNotEmpty()
            is JessieCodeRuntimeValue.ArrayValue,
            is JessieCodeRuntimeValue.ObjectValue,
            is JessieCodeRuntimeValue.FunctionValue,
            is JessieCodeRuntimeValue.BoardReference,
            is JessieCodeRuntimeValue.TransformationReference,
            is JessieCodeRuntimeValue.CompositionReference,
            is JessieCodeRuntimeValue.ElementReference,
            -> true
        }

    private fun conditionValueType(
        value: JessieCodeRuntimeValue,
    ): String =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue -> "number"
            is JessieCodeRuntimeValue.BooleanValue -> "boolean"
            is JessieCodeRuntimeValue.StringValue -> "string"
            JessieCodeRuntimeValue.NullValue -> "null"
            JessieCodeRuntimeValue.UndefinedValue -> "undefined"
            is JessieCodeRuntimeValue.ArrayValue -> "array"
            is JessieCodeRuntimeValue.ObjectValue -> "object"
            is JessieCodeRuntimeValue.FunctionValue -> "function"
            is JessieCodeRuntimeValue.BoardReference -> "board"
            is JessieCodeRuntimeValue.TransformationReference ->
                "transformation"
            is JessieCodeRuntimeValue.CompositionReference ->
                "composition"
            is JessieCodeRuntimeValue.ElementReference -> "element"
        }

    // JSXGraph 1.13.3: src/base/board.js ->
    // _set / setAttribute.
    internal fun _set(
        key: String,
        value: Any?,
    ): Board {
        val normalized = normalizeAttributeKey(key)
        val existing = attributes[normalized]
        attributes[normalized] =
            if (existing is MutableMap<*, *> && value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                mergeAttributeMaps(
                    existing as MutableMap<String, Any?>,
                    value,
                )
            } else {
                copyAttributeValue(value)
            }
        return this
    }

    internal fun setAttribute(
        changedAttributes: Map<String, Any?>,
        force: Boolean = false,
    ): Board {
        val normalizedChanges = linkedMapOf<String, Any?>()
        for ((rawKey, rawValue) in changedAttributes) {
            val key = normalizeAttributeKey(rawKey)
            val value =
                if (rawValue == "false") false else rawValue
            val oldValue = attributes[key]
            if (!force && oldValue == value) {
                continue
            }
            when (key) {
                "axis" -> {
                    val axes = defaultAxes
                    if (axes != null) {
                        axes.x.setAttribute(mapOf("visible" to value))
                        axes.y.setAttribute(mapOf("visible" to value))
                        _set(key, value)
                        defaultAxesError = null
                    } else if (value == true) {
                        when (val result = createDefaultAxes(this)) {
                            is GMResult.Ok -> {
                                defaultAxes = result.value
                                defaultAxesError = null
                                _set(key, true)
                            }
                            is GMResult.Err ->
                                defaultAxesError = result.error
                        }
                    }
                }
                "defaultaxes" -> {
                    val axes = defaultAxes
                    val axisAttributes = attributeMap(value)
                    if (axes != null) {
                        attributeMap(axisAttributes["x"])
                            .takeIf { it.isNotEmpty() }
                            ?.let(axes.x::setAttribute)
                        attributeMap(axisAttributes["y"])
                            .takeIf { it.isNotEmpty() }
                            ?.let(axes.y::setAttribute)
                        defaultAxesError = null
                    }
                }
                "animationdelay" -> {
                    (value as? Number)?.toDouble()?.let {
                        if (it.isFinite() && it > 0.0) {
                            animationDelay = it
                        }
                    }
                    _set(key, animationDelay)
                }
                "boundingbox" -> {
                    val bbox = numberArray(value)
                    if (bbox != null) {
                        setBoundingBox(
                            bbox,
                            keepAspectRatio,
                            BoardZoomMode.RESET,
                        )
                        _set(key, bbox)
                    }
                }
                "keepaspectratio" -> {
                    keepAspectRatio = value == true
                    _set(key, keepAspectRatio)
                    setBoundingBox(
                        getBoundingBox(),
                        keepAspectRatio,
                        BoardZoomMode.KEEP,
                    )
                }
                "maxboundingbox" -> {
                    numberArray(value)?.let {
                        if (it.size >= BOUNDING_BOX_COORDINATE_COUNT) {
                            maxBoundingBox = it.copyOf(4)
                            _set(key, maxBoundingBox)
                        }
                    }
                }
                "zoomx" -> {
                    (value as? Number)?.toDouble()?.let {
                        setZoom(it, zoomY)
                        _set(key, it)
                    }
                }
                "zoomy" -> {
                    (value as? Number)?.toDouble()?.let {
                        setZoom(zoomX, it)
                        _set(key, it)
                    }
                }
                "registerevents", "renderer" -> Unit
                else -> _set(key, value)
            }
            normalizedChanges[key] = value
        }
        trigger(
            events = listOf("attribute"),
            arguments = listOf(normalizedChanges, this),
        )
        fullUpdate()
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> dehighlightAll.
    internal fun dehighlightAll(): Board {
        val stillHighlighted =
            linkedMapOf<String, GeometryElement>()
        for ((id, element) in highlightedObjects.toMap()) {
            if (id in focusObjects) {
                stillHighlighted[id] = element
            } else {
                // Browser JSXGraph guards renderer work with its registered
                // handler flags. Native hosts call this kernel directly.
                element.noHighlight()
            }
        }
        highlightedObjects.clear()
        highlightedObjects.putAll(stillHighlighted)
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> emulateColorblindness.
    internal fun emulateColorblindness(
        deficiency: String = "none",
    ): Board {
        val normalized = deficiency.lowercase()
        if (normalized == currentColorBlindness) {
            return this
        }
        colorBlindnessErrors.clear()
        for (element in objects.values) {
            if (normalized == "none") {
                val original = element.originalVisualProperties ?: continue
                element.setAttribute(original)
                continue
            }
            if (currentColorBlindness == "none") {
                element.originalVisualProperties = listOf(
                    "strokecolor",
                    "fillcolor",
                    "highlightstrokecolor",
                    "highlightfillcolor",
                ).associateWith(element::getAttribute)
            }
            val original = element.originalVisualProperties ?: continue
            val converted = linkedMapOf<String, Any?>()
            for ((property, value) in original) {
                val color = value as? String ?: continue
                when (val result = Color.rgb2cb(color, normalized)) {
                    is GMResult.Ok -> converted[property] = result.value
                    is GMResult.Err ->
                        colorBlindnessErrors["${element.id}:$property"] =
                            result.error
                }
            }
            element.setAttribute(converted)
        }
        currentColorBlindness = normalized
        update()
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // on / off / trigger / addEvent / removeEvent.
    internal fun on(
        event: String,
        handler: EventHandler,
        context: Any = this,
    ): Board {
        events.on(event, handler, context)
        return this
    }

    internal fun off(
        event: String,
        handler: EventHandler? = null,
    ): Board {
        events.off(event, handler)
        return this
    }

    internal fun trigger(
        events: List<String>,
        arguments: List<Any?> = emptyList(),
    ): Board {
        this.events.trigger(events, arguments)
        return this
    }

    internal fun addEvent(
        event: String,
        handler: EventHandler,
        context: Any = this,
    ): Board = on(event, handler, context)

    internal fun removeEvent(
        event: String,
        handler: EventHandler? = null,
    ): Board = off(event, handler)

    // JSXGraph 1.13.3: src/base/board.js ->
    // addHook / removeHook / updateHooks.
    internal fun addHook(
        hook: EventHandler,
        event: String = "update",
        context: Any = this,
    ): Int {
        hooks += event to hook
        on(event, hook, context)
        return hooks.lastIndex
    }

    internal fun removeHook(id: Int): Board {
        val hook = hooks.getOrNull(id) ?: return this
        off(hook.first, hook.second)
        hooks[id] = null
        return this
    }

    internal fun removeHook(handler: EventHandler): Board {
        for ((index, hook) in hooks.withIndex()) {
            if (hook?.second === handler) {
                off(hook.first, handler)
                hooks[index] = null
            }
        }
        return this
    }

    internal fun updateHooks(
        event: String = "update",
        arguments: List<Any?> = emptyList(),
    ): Board = trigger(listOf(event), arguments)

    // JSXGraph 1.13.3: src/base/board.js -> addChild / removeChild.
    internal fun addChild(board: Board): Board {
        if (board !== this && board !in dependentBoards) {
            dependentBoards += board
            update()
        }
        return this
    }

    internal fun removeChild(board: Board): Board {
        dependentBoards.removeAll { it === board }
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> createElement.
    // The public engine owns type dispatch; this keeps the deprecated alias
    // available without introducing a second element registry in Board.
    internal fun createElement(
        creator: () -> GeometryElement,
    ): GeometryElement = creator()

    // JSXGraph 1.13.3: src/base/board.js -> migratePoint.
    internal fun migratePoint(
        source: Point,
        destination: Point,
        copyName: Boolean = false,
    ): Board {
        if (
            select(source) !== source ||
            select(destination) !== destination ||
            source === destination
        ) {
            return this
        }
        source.elementLabel?.let(::removeObject)
        for (child in source.childElements.values.toList()) {
            var replaced = false
            when (child) {
                is Line -> {
                    if (child.point1 === source) {
                        child.point1 = destination
                        replaced = true
                    }
                    if (child.point2 === source) {
                        child.point2 = destination
                        replaced = true
                    }
                }
                is Polygon -> {
                    for (index in child.vertices.indices) {
                        if (child.vertices[index] === source) {
                            child.vertices[index] = destination
                            replaced = true
                        }
                    }
                }
            }
            for (index in child.parents.indices) {
                if (child.parents[index] == source.id) {
                    child.parents[index] = destination.id
                    replaced = true
                }
            }
            destination.addChild(child)
            if (replaced) {
                source.childElements.remove(child.id)
            }
        }
        if (copyName) {
            val sourceName = source.name
            elementsByName.remove(destination.name)
            destination.setName(sourceName)
            if (source.hasLabel) {
                destination.setLabel(sourceName)
            }
        }
        removeObject(source)
        if (destination.name.isNotEmpty()) {
            elementsByName[destination.name] = destination
        }
        fullUpdate()
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> addLogEntry.
    internal fun addLogEntry(
        type: String,
        element: GeometryElement?,
        timestampMilliseconds: Long,
    ): Board {
        val last = userLog.lastOrNull()
        if (
            last != null &&
            last.type == type &&
            last.elementId == element?.id.orEmpty() &&
            timestampMilliseconds - last.endMilliseconds <
            USER_LOG_MERGE_WINDOW_MILLISECONDS
        ) {
            last.endMilliseconds = timestampMilliseconds
        } else {
            userLog += BoardUserLogEntry(
                type = type,
                elementId = element?.id.orEmpty(),
                startMilliseconds = timestampMilliseconds,
                endMilliseconds = timestampMilliseconds,
            )
        }
        return this
    }

    // JSXGraph: src/base/board.js -> generateName
    internal fun generateName(element: GeometryElement): String {
        return generateName(
            type = element.type,
            elementClass = element.elementClass,
        )
    }

    // JSXGraph 1.13.3: src/base/group.js -> Group name initialization.
    internal fun generateGroupName(): String =
        "group_" + generateName(
            type = Const.OBJECT_TYPE_POINT,
            elementClass = Const.OBJECT_CLASS_POINT,
        )

    private fun generateName(
        type: Int,
        elementClass: Int,
    ): String {
        if (type == Const.OBJECT_TYPE_TICKS || maxNameLength <= 0) {
            return ""
        }

        val possibleNames = when {
            elementClass == Const.OBJECT_CLASS_POINT ||
                type == Const.OBJECT_TYPE_POINT3D -> CAPITAL_NAMES
            type == Const.OBJECT_TYPE_ANGLE -> ANGLE_NAMES
            else -> LOWERCASE_NAMES
        }
        val (prefix, suffix) = when {
            elementClass == Const.OBJECT_CLASS_POINT ||
                type == Const.OBJECT_TYPE_POINT3D ||
                elementClass == Const.OBJECT_CLASS_LINE ||
                type == Const.OBJECT_TYPE_ANGLE -> "" to ""
            type == Const.OBJECT_TYPE_POLYGON -> "P_{" to "}"
            elementClass == Const.OBJECT_CLASS_CIRCLE -> "k_{" to "}"
            elementClass == Const.OBJECT_CLASS_TEXT -> "t_{" to "}"
            else -> "s_{" to "}"
        }
        val indices = IntArray(maxNameLength)

        while (indices[maxNameLength - 1] < possibleNames.size) {
            for (firstIndex in 1 until possibleNames.size) {
                indices[0] = firstIndex
                val candidate = buildString {
                    append(prefix)
                    for (index in maxNameLength downTo 1) {
                        append(possibleNames[indices[index - 1]])
                    }
                    append(suffix)
                }
                if (candidate !in elementsByName) {
                    return candidate
                }
            }

            indices[0] = possibleNames.size
            for (index in 1 until maxNameLength) {
                if (indices[index - 1] == possibleNames.size) {
                    indices[index - 1] = 1
                    indices[index] += 1
                }
            }
        }

        return ""
    }

    // JSXGraph 1.13.3: src/base/group.js -> Group constructor registration.
    internal fun registerGroup(
        group: Group,
        requestedId: String,
    ): String {
        val creationIndex = numObjects
        numObjects += 1
        val groupId =
            if (requestedId.isEmpty()) {
                "${id}Group$creationIndex"
            } else {
                requestedId
            }
        groups[groupId] = group
        return groupId
    }

    // JSXGraph: src/base/board.js -> select
    internal fun select(reference: String): GeometryElement? {
        if (reference.isEmpty()) {
            return null
        }
        return objects[reference] ?: elementsByName[reference]
    }

    // JSXGraph: src/base/board.js -> select
    internal fun select(element: GeometryElement?): GeometryElement? {
        if (element == null || element.id !in objects) {
            return null
        }
        return element
    }

    // JSXGraph: src/base/board.js -> _removeObj
    private fun removeElement(
        element: GeometryElement?,
        saveMethod: Boolean,
    ) {
        if (element == null || objects[element.id] !== element) {
            return
        }

        // The upstream recursive call intentionally does not forward
        // saveMethod, so descendants always use the ancestor-based path.
        for (child in element.childElements.values.toList()) {
            child.board.removeElement(child, saveMethod = false)
        }

        if (saveMethod) {
            for (candidate in objects.values) {
                candidate.childElements.remove(element.id)
                candidate.descendants.remove(element.id)
            }
        } else {
            for (ancestor in element.ancestors.values) {
                ancestor.childElements.remove(element.id)
                ancestor.descendants.remove(element.id)
            }
        }

        val position = element.positionInBoard
        if (
            position >= 0 &&
            position < objectsList.size &&
            objectsList[position] === element
        ) {
            objectsList.removeAt(position)
            for (index in position until objectsList.size) {
                objectsList[index].positionInBoard = index
            }
        }

        objects.remove(element.id)
        elementsByName.remove(element.name)
        element.positionInBoard = -1
        element.remove()
    }

    // JSXGraph: src/base/board.js -> removeObject
    internal fun removeObject(
        element: GeometryElement?,
        saveMethod: Boolean = false,
    ): Board {
        removeElement(element, saveMethod)
        update()
        return this
    }

    // JSXGraph: src/base/board.js -> removeObject
    internal fun removeObject(
        elementReference: String,
        saveMethod: Boolean = false,
    ): Board = removeObject(select(elementReference), saveMethod)

    // JSXGraph: src/base/board.js -> _removeObj object.objects branch
    internal fun removeObject(
        composition: Composition,
        saveMethod: Boolean = false,
    ): Board {
        for (element in composition.objects.values.toList()) {
            removeElement(element, saveMethod)
        }
        update()
        return this
    }

    // JSXGraph: src/base/board.js -> removeObject
    internal fun removeObjects(
        elements: Iterable<GeometryElement>,
        saveMethod: Boolean = false,
    ): Board {
        for (element in elements.toList()) {
            removeElement(element, saveMethod)
        }
        update()
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> removeGrids.
    internal fun removeGrids(): Board {
        removeObjects(grids.toList())
        grids.clear()
        update()
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> addGrid.
    internal fun addGrid(grid: Curve): Board {
        if (grid !in grids) {
            grids += grid
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> clearTraces.
    internal fun clearTraces(): Board {
        for (element in objectsList) {
            element.clearTrace()
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // addAnimation / stopAllAnimation / animate.
    internal fun addAnimation(element: GeometryElement): Board {
        animationObjects[element.id] = element
        if (!animationLoopScheduled) {
            animationLoopScheduled = true
            animationScheduler.schedule(animationDelay) {
                animate()
                val keepRunning = animationObjects.isNotEmpty()
                if (!keepRunning) {
                    animationLoopScheduled = false
                }
                keepRunning
            }
        }
        return this
    }

    internal fun stopAllAnimation(): Board {
        for (element in animationObjects.values) {
            element.cancelAnimation()
        }
        animationObjects.clear()
        animationLoopScheduled = false
        return this
    }

    internal fun animate(): Board {
        var updatedElement: GeometryElement? = null
        for ((id, element) in animationObjects.toMap()) {
            val keepAnimating = element.advanceAnimationFrame()
            updatedElement = element
            if (!keepAnimating) {
                animationObjects.remove(id)
            }
        }
        if (updatedElement != null) {
            update(updatedElement)
        }
        return this
    }

    internal fun takeAnimationFailure(): BoardAnimationFailure? {
        for (element in objectsList) {
            val movementError = when (element) {
                is Point3D -> element.takeMovementError()
                is CoordsElement -> element.takeMovementError()
                else -> null
            }
            if (movementError != null) {
                return BoardAnimationFailure(
                    elementId = element.id,
                    reason = movementError.toString(),
                )
            }
            val propertyError = element.takeAnimationError()
            if (propertyError != null) {
                return BoardAnimationFailure(
                    elementId = element.id,
                    reason = propertyError.toString(),
                )
            }
        }
        return null
    }

    // JSXGraph 1.13.3: src/base/board.js -> updateCoords.
    internal fun updateCoords(): Board {
        for (element in objectsList) {
            val coordsElement = element as? CoordsElement ?: continue
            if (coordsElement.evalVisProp("frozen") == true) {
                coordsElement.coords.screen2usr()
                coordsElement.actualCoords.screen2usr()
            } else {
                coordsElement.coords.usr2screen()
                coordsElement.actualCoords.usr2screen()
            }
        }
        updateBoundingBoxFromViewport()
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> calculateSnapSizes.
    internal fun calculateSnapSizes(
        gridStepX: Any? = "auto",
        gridStepY: Any? = "auto",
    ): Board {
        val bbox = getBoundingBox()
        val stepX =
            if (gridStepX == "auto") {
                1.0
            } else {
                com.swithun.jsxgraph.core.utils.Type.parseNumber(
                    gridStepX,
                    abs(bbox[1] - bbox[3]),
                    1.0 / unitX,
                )
            }
        val stepY =
            if (gridStepY == "auto") {
                1.0
            } else {
                com.swithun.jsxgraph.core.utils.Type.parseNumber(
                    gridStepY,
                    abs(bbox[0] - bbox[2]),
                    1.0 / unitY,
                )
            }
        var screenStepX = abs(stepX * unitX)
        var screenStepY = abs(stepY * unitY)
        snapSizeX = stepX
        while (screenStepX > MAXIMUM_SNAP_PIXEL_DISTANCE) {
            snapSizeX *= 2.0
            screenStepX *= 0.5
        }
        snapSizeY = stepY
        while (screenStepY > MAXIMUM_SNAP_PIXEL_DISTANCE) {
            snapSizeY *= 2.0
            screenStepY *= 0.5
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> applyZoom.
    internal fun applyZoom(): Board =
        updateCoords()
            .calculateSnapSizes()
            .clearTraces()
            .fullUpdate()

    // JSXGraph 1.13.3: src/base/board.js -> zoomIn / zoomOut.
    internal fun zoomIn(
        userX: Double? = null,
        userY: Double? = null,
    ): Board {
        if (
            (zoomX > maximumZoom && zoomFactorX > 1.0) ||
            (zoomY > maximumZoom && zoomFactorY > 1.0) ||
            (zoomX < minimumZoom && zoomFactorX < 1.0) ||
            (zoomY < minimumZoom && zoomFactorY < 1.0)
        ) {
            return this
        }
        return zoomAround(
            factorX = zoomFactorX,
            factorY = zoomFactorY,
            userX = userX,
            userY = userY,
            inverse = true,
        )
    }

    internal fun zoomOut(
        userX: Double? = null,
        userY: Double? = null,
    ): Board {
        if (zoomX < minimumZoom || zoomY < minimumZoom) {
            return this
        }
        return zoomAround(
            factorX = zoomFactorX,
            factorY = zoomFactorY,
            userX = userX,
            userY = userY,
            inverse = false,
        )
    }

    private fun zoomAround(
        factorX: Double,
        factorY: Double,
        userX: Double?,
        userY: Double?,
        inverse: Boolean,
    ): Board {
        val bbox = getBoundingBox()
        val appliedX = if (inverse) 1.0 / factorX else factorX
        val appliedY = if (inverse) 1.0 / factorY else factorY
        val deltaX = (bbox[2] - bbox[0]) * (1.0 - appliedX)
        val deltaY = (bbox[1] - bbox[3]) * (1.0 - appliedY)
        val horizontalRatio =
            if (userX != null && userY != null) {
                (userX - bbox[0]) / (bbox[2] - bbox[0])
            } else {
                0.5
            }
        val verticalRatio =
            if (userX != null && userY != null) {
                (bbox[1] - userY) / (bbox[1] - bbox[3])
            } else {
                0.5
            }
        setBoundingBox(
            bbox = doubleArrayOf(
                bbox[0] + deltaX * horizontalRatio,
                bbox[1] - deltaY * verticalRatio,
                bbox[2] - deltaX * (1.0 - horizontalRatio),
                bbox[3] + deltaY * (1.0 - verticalRatio),
            ),
            keepAspectRatio = keepAspectRatio,
            setZoom = BoardZoomMode.UPDATE,
        )
        return applyZoom()
    }

    // JSXGraph 1.13.3: src/base/board.js -> zoom100.
    internal fun zoom100(): Board {
        setBoundingBox(
            bbox = initialBoundingBox,
            keepAspectRatio = keepAspectRatio,
            setZoom = BoardZoomMode.RESET,
        )
        return applyZoom()
    }

    // JSXGraph 1.13.3: src/base/board.js -> zoomAllPoints.
    internal fun zoomAllPoints(): Board {
        var minimumX = 0.0
        var maximumX = 0.0
        var minimumY = 0.0
        var maximumY = 0.0
        for (point in objectsList.filterIsInstance<Point>()) {
            if (
                point.visualPropertiesCalculated["visible"] == false ||
                !point.coords.isReal()
            ) {
                continue
            }
            minimumX = minOf(minimumX, point.X())
            maximumX = maxOf(maximumX, point.X())
            minimumY = minOf(minimumY, point.Y())
            maximumY = maxOf(maximumY, point.Y())
        }
        val borderX = ZOOM_ALL_BORDER_PIXELS / unitX
        val borderY = ZOOM_ALL_BORDER_PIXELS / unitY
        setBoundingBox(
            bbox = doubleArrayOf(
                minimumX - borderX,
                maximumY + borderY,
                maximumX + borderX,
                minimumY - borderY,
            ),
            keepAspectRatio = keepAspectRatio,
            setZoom = BoardZoomMode.UPDATE,
        )
        return applyZoom()
    }

    // JSXGraph 1.13.3: src/base/board.js -> zoomElements.
    internal fun zoomElements(
        elements: Iterable<*>,
    ): Board {
        val selected = elements.mapNotNull { element ->
            when (element) {
                is GeometryElement -> select(element)
                is String -> select(element)
                else -> null
            }
        }
        if (selected.isEmpty()) {
            return this
        }
        val combined = doubleArrayOf(
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
        )
        for (element in selected) {
            val bounds = element.getBoundingBox()
            combined[0] = minOf(combined[0], bounds[0])
            combined[1] = maxOf(combined[1], bounds[1])
            combined[2] = maxOf(combined[2], bounds[2])
            combined[3] = minOf(combined[3], bounds[3])
        }
        if (combined.any { !it.isFinite() }) {
            return this
        }
        val centerX = 0.5 * (combined[0] + combined[2])
        val centerY = 0.5 * (combined[1] + combined[3])
        val radius = max(
            0.75 * (combined[2] - combined[0]),
            0.75 * (combined[1] - combined[3]),
        )
        if (!radius.isFinite() || radius <= 0.0) {
            return this
        }
        setBoundingBox(
            bbox = doubleArrayOf(
                centerX - radius,
                centerY + radius,
                centerX + radius,
                centerY - radius,
            ),
            keepAspectRatio = keepAspectRatio,
            setZoom = BoardZoomMode.UPDATE,
        )
        return applyZoom()
    }

    // JSXGraph 1.13.3: src/base/board.js -> setZoom.
    internal fun setZoom(
        factorX: Double,
        factorY: Double,
    ): Board {
        val previousX = zoomFactorX
        val previousY = zoomFactorY
        zoomFactorX = factorX / zoomX
        zoomFactorY = factorY / zoomY
        zoomIn()
        zoomFactorX = previousX
        zoomFactorY = previousY
        return this
    }

    // JSXGraph: src/base/board.js -> removeAncestors
    internal fun removeAncestors(element: GeometryElement): Board {
        for (ancestor in element.ancestors.values.toList()) {
            removeAncestors(ancestor)
        }
        removeObject(element)
        return this
    }

    // JSXGraph: src/base/board.js -> prepareUpdate
    internal fun prepareUpdate(): Board {
        for (element in objectsList) {
            element.needsUpdate =
                element.needsRegularUpdate || needsFullUpdate
        }
        for (group in groups.values) {
            group.needsUpdate =
                group.needsRegularUpdate || needsFullUpdate
        }
        return this
    }

    // JSXGraph: src/base/board.js -> updateElements
    internal fun updateElements(
        draggedElement: GeometryElement? = null,
    ): Board {
        var index = 0
        while (index < objectsList.size) {
            val element = objectsList[index]
            element
                .update(
                    fromParent =
                        draggedElement == null ||
                            element.id != draggedElement.id,
                )
                .updateVisibility()
            index += 1
        }
        for (group in groups.values.toList()) {
            group.update(draggedElement)
        }
        return this
    }

    // JSXGraph: src/base/board.js -> updateRenderer
    internal fun updateRenderer(): Board {
        for (element in objectsList) {
            element.updateRenderer()
        }
        return this
    }

    // JSXGraph: src/base/board.js -> update
    internal fun update(
        draggedElement: GeometryElement? = null,
    ): Board {
        if (inUpdate || isSuspendedUpdate) {
            return this
        }
        inUpdate = true
        try {
            for (chart in objectsList.filterIsInstance<Chart>()) {
                chart.prepareDataArrayUpdate()
            }
            prepareUpdate().updateElements(draggedElement)
            updateConditions()
            updateRenderer()
            trigger(listOf("update"))
            for (dependent in dependentBoards.toList()) {
                if (dependent !== this) {
                    dependent.updateQuality = updateQuality
                    dependent.update()
                }
            }
        } finally {
            inUpdate = false
        }
        return this
    }

    // JSXGraph: src/base/board.js -> fullUpdate
    internal fun fullUpdate(): Board {
        needsFullUpdate = true
        try {
            update()
        } finally {
            needsFullUpdate = false
        }
        return this
    }

    internal fun suspendUpdate(): Board {
        if (!inUpdate) {
            isSuspendedUpdate = true
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> unsuspendUpdate.
    internal fun unsuspendUpdate(): Board {
        if (isSuspendedUpdate) {
            isSuspendedUpdate = false
            fullUpdate()
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> createRoulette.
    internal fun createRoulette(
        firstCurve: Curve,
        secondCurve: Curve,
        startFirstCurve: Double,
        stepSize: Double,
        direction: Double,
        delayMilliseconds: Double,
        points: List<CoordsElement>,
    ): BoardRoulette =
        BoardRoulette(
            board = this,
            firstCurve = firstCurve,
            secondCurve = secondCurve,
            startFirstCurve = startFirstCurve,
            stepSize = stepSize,
            direction = direction,
            delayMilliseconds = delayMilliseconds,
            points = points,
        )

    private fun normalizeAttributeKey(key: String): String =
        key.filterNot(Char::isWhitespace).lowercase()

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

    private fun attributeMap(value: Any?): Map<String, Any?> {
        val source = value as? Map<*, *> ?: return emptyMap()
        return source.entries.associateTo(linkedMapOf()) {
            (key, nestedValue) ->
            normalizeAttributeKey(key.toString()) to
                copyAttributeValue(nestedValue)
        }
    }

    private fun numberArray(value: Any?): DoubleArray? =
        when (value) {
            is DoubleArray -> value.copyOf()
            is List<*> -> value.map {
                (it as? Number)?.toDouble() ?: return null
            }.toDoubleArray()
            else -> null
        }

    internal companion object {
        // JSXGraph 1.13.3: src/jsxgraph.js -> JSXGraph.initBoard.
        internal fun fromBoundingBox(
            boundingBox: DoubleArray,
            keepAspectRatio: Boolean,
            id: String = "jxgBoard1",
            canvasWidth: Double = DEFAULT_CANVAS_WIDTH,
            canvasHeight: Double = DEFAULT_CANVAS_HEIGHT,
            defaultCurveMinimum: Double,
            defaultCurveMaximum: Double,
            maxCurvePoints: Int = 10_000,
            animationScheduler: BoardAnimationScheduler =
                ImmediateBoardAnimationScheduler,
            localeNumberFormatter: JsxGraphLocaleNumberFormatter? = null,
            chartTableProvider: JsxGraphChartTableProvider? = null,
        ): Board {
            var unitX =
                canvasWidth / (boundingBox[2] - boundingBox[0])
            var unitY =
                canvasHeight / (boundingBox[1] - boundingBox[3])
            var offsetX = 0.0
            var offsetY = 0.0
            if (keepAspectRatio) {
                if (abs(unitX) < abs(unitY)) {
                    unitY = abs(unitX) * unitY / abs(unitY)
                    offsetY = (
                        canvasHeight / unitY -
                            (boundingBox[1] - boundingBox[3])
                        ) * 0.5
                } else {
                    unitX = abs(unitY) * unitX / abs(unitX)
                    offsetX = (
                        canvasWidth / unitX -
                            (boundingBox[2] - boundingBox[0])
                        ) * 0.5
                }
            }
            val originX = -unitX * (boundingBox[0] - offsetX)
            val originY = unitY * (boundingBox[1] + offsetY)
            return Board(
                originX = originX,
                originY = originY,
                unitX = unitX,
                unitY = unitY,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                keepAspectRatio = keepAspectRatio,
                id = id,
                defaultCurveMinimum = defaultCurveMinimum,
                defaultCurveMaximum = defaultCurveMaximum,
                maxCurvePoints = maxCurvePoints,
                animationScheduler = animationScheduler,
                localeNumberFormatter = localeNumberFormatter,
                chartTableProvider = chartTableProvider,
            )
        }

        // JSXGraph 1.13.3: src/base/board.js -> BOARD_QUALITY_*.
        const val BOARD_QUALITY_LOW = 0x1
        const val BOARD_QUALITY_HIGH = 0x2

        const val DEFAULT_CANVAS_WIDTH = 500.0
        const val DEFAULT_CANVAS_HEIGHT = 500.0
        const val CURVE_DOMAIN_PADDING = 0.1
        const val BOUNDING_BOX_COORDINATE_COUNT = 4
        const val DEFAULT_ANIMATION_DELAY = 35.0
        const val DEFAULT_ZOOM_FACTOR = 1.25
        const val DEFAULT_MINIMUM_ZOOM = 0.0001
        const val DEFAULT_MAXIMUM_ZOOM = 10_000.0
        const val DEFAULT_MAXIMUM_FRAME_RATE = 40.0
        const val DEFAULT_KEYBOARD_DELTA = 10.0
        const val MAXIMUM_SNAP_PIXEL_DISTANCE = 25.0
        const val ZOOM_ALL_BORDER_PIXELS = 50.0
        const val USER_LOG_MERGE_WINDOW_MILLISECONDS = 500L
        const val BOARD_MODE_NONE = 0
        const val BOARD_MODE_DRAG = 1
        const val BOARD_MODE_MOVE_ORIGIN = 2
        const val BOARD_MODE_ZOOM = 5
        private const val MINIMUM_GESTURE_MOVEMENT_SQUARED = 100.0
        private const val CONDITION_OPEN_TAG = "<data>"
        private const val CONDITION_CLOSE_TAG = "</data>"
        const val GENERATED_BOARD_ID_PREFIX = "jxgBoard"

        private var nextGeneratedBoardId: Long = 1L

        val CAPITAL_NAMES = listOf(
            "",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
        )
        val LOWERCASE_NAMES = listOf(
            "",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
            "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
        )
        val ANGLE_NAMES = listOf(
            "",
            "&alpha;", "&beta;", "&gamma;", "&delta;", "&epsilon;", "&zeta;",
            "&eta;", "&theta;", "&iota;", "&kappa;", "&lambda;", "&mu;", "&nu;",
            "&xi;", "&omicron;", "&pi;", "&rho;", "&sigma;", "&tau;", "&upsilon;",
            "&phi;", "&chi;", "&psi;", "&omega;",
        )
    }
}
