/*
 * Kotlin translation support for JSXGraph.
 * Upstream: src/parser/jessiecode.js -> JessieCode constructor,
 * _genericParse, parse, and use
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

data class JsxGraphJessieCodeBoardOptions(
    val containerId: String = "jxgbox",
    val boundingBox: JsxGraphBoundingBox =
        JsxGraphBoundingBox(
            left = -5.0,
            top = 5.0,
            right = 5.0,
            bottom = -5.0,
        ),
    val axis: Boolean = false,
    val axisAttributes: Map<String, Any?> = emptyMap(),
    val defaultAxesAttributes: Map<String, Any?> = emptyMap(),
    val grid: Boolean = false,
    val keepAspectRatio: Boolean = false,
    val theme: String = "default",
    val showNavigation: Boolean = false,
    val showZoom: Boolean = true,
    val showClearTraces: Boolean = false,
    val showFullscreen: Boolean = false,
    val showScreenshot: Boolean = false,
    val showReload: Boolean = false,
    val fullscreenSymbol: String = "\u26f6",
    val screenshotSymbol: String = "\u2318",
    val navbar: JsxGraphNavigationBarStyle =
        JsxGraphNavigationBarStyle(),
    val runtimeOptions: JsxGraphRuntimeOptions =
        JsxGraphRuntimeOptions(),
)

/**
 * Resource limits for native JessieCode parsing, evaluation, and rendering.
 *
 * Every limit applies independently. Evaluation step and depth budgets reset
 * for each [JsxGraphJessieCodeSession.execute] call, while stored source and
 * created-element limits apply to the complete session.
 */
data class JsxGraphJessieCodeLimits(
    val maxSourceLength: Int = 1_000_000,
    val maxTokens: Int = 100_000,
    val maxAstNodes: Int = 100_000,
    val maxAstDepth: Int = 256,
    val maxParserNesting: Int = 64,
    val maxComputerAlgebraSteps: Int = 1_000_000,
    val maxDerivativeOrder: Int = 64,
    val maxEvaluationSteps: Int = 100_000,
    val maxEvaluationDepth: Int = 64,
    val maxCollectionSize: Int = 100_000,
    val maxStoredSourceLength: Int = 1_000_000,
    val maxAttributeDepth: Int = 64,
    val maxObjects: Int = 10_000,
    val maxCurvePoints: Int = 10_000,
    val maxPolygonVertices: Int = 10_000,
    val maxTextLength: Int = 100_000,
    val maxImageSourceLength: Int = 1_000_000,
)

data class JsxGraphJessieCodeSourceRange(
    val line: Int,
    val column: Int,
    val endLine: Int,
    val endColumn: Int,
)

sealed interface JsxGraphJessieCodeError {
    val message: String

    data class InvalidConfiguration(
        override val message: String,
    ) : JsxGraphJessieCodeError

    data class SourceHistoryLimitExceeded(
        val limit: Int,
        val requestedSize: Long,
    ) : JsxGraphJessieCodeError {
        override val message: String =
            "Stored JessieCode length $requestedSize exceeds limit $limit"
    }

    data class Parse(
        val reason: String,
        val location: JsxGraphJessieCodeSourceRange?,
    ) : JsxGraphJessieCodeError {
        override val message: String =
            location?.let {
                "JessieCode parse failure at ${it.line}:${it.column}: $reason"
            } ?: "JessieCode parse failure: $reason"
    }

    data class Runtime(
        val reason: String,
        val location: JsxGraphJessieCodeSourceRange?,
    ) : JsxGraphJessieCodeError {
        override val message: String =
            location?.let {
                "JessieCode runtime failure at ${it.line}:${it.column}: $reason"
            } ?: "JessieCode runtime failure: $reason"
    }

    data class ResourceLimitExceeded(
        val resource: String,
        val limit: Int,
        val requestedSize: Long,
        val location: JsxGraphJessieCodeSourceRange?,
    ) : JsxGraphJessieCodeError {
        override val message: String =
            "$resource size $requestedSize exceeds limit $limit"
    }

    data class Scene(
        val error: JsxGraphDocumentError,
    ) : JsxGraphJessieCodeError {
        override val message: String = error.message
    }
}

/**
 * Stateful native JessieCode interpreter backed by one translated Board.
 *
 * Globals, functions, closures, Board selection, created elements, Point
 * interaction state, and scheduled animations persist across calls. Hosts
 * advance animation time explicitly; this class does not execute arbitrary
 * JavaScript or expose a browser/DOM environment.
 */
class JsxGraphJessieCodeSession internal constructor(
    initialScene: JsxGraphScene,
    private val executeSource: (
        source: String,
        storeSource: Boolean,
    ) -> GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    private val exportBoardSource: (
        format: JsxGraphBoardExportFormat,
        noAttributes: Boolean,
        limits: JsxGraphBoardExportLimits,
    ) -> GMResult<String, JsxGraphBoardExportError>,
    private val movePointSource: (
        id: String,
        coordinates: JsxGraphPoint2D,
    ) -> GMResult<JsxGraphScene, JsxGraphInteractionError>,
    private val movePoint3DSource: (
        id: String,
        coordinates: JsxGraphPoint3DCoordinates,
        timeMilliseconds: Double,
        effect: String?,
        callback: (() -> Unit)?,
    ) -> GMResult<JsxGraphScene, JsxGraphInteractionError>,
    private val movePoint3DAlongSource: (
        id: String,
        path: List<JsxGraphPoint3DCoordinates>,
        timeMilliseconds: Double,
        callback: (() -> Unit)?,
    ) -> GMResult<JsxGraphScene, JsxGraphInteractionError>,
    private val interactControlSource: (
        interaction: JsxGraphControlInteraction,
    ) -> GMResult<JsxGraphScene, JsxGraphInteractionError>,
    private val interactNavigationSource: (
        action: JsxGraphNavigationAction,
    ) -> GMResult<
        JsxGraphNavigationInteractionResult,
        JsxGraphInteractionError,
        >,
    private val interactPointerSource: (
        interaction: JsxGraphBoardPointerInteraction,
    ) -> GMResult<JsxGraphScene, JsxGraphInteractionError>,
    private val interactView3DSource: (
        interaction: JsxGraphView3DInteraction,
    ) -> GMResult<
        JsxGraphView3DInteractionResult,
        JsxGraphInteractionError,
        >,
    private val hasActiveAnimationsSource: () -> Boolean,
    private val advanceAnimationsSource: (
        elapsedMilliseconds: Double,
    ) -> GMResult<JsxGraphScene, JsxGraphInteractionError>,
    private val storedSource: () -> String,
) {
    var scene: JsxGraphScene = initialScene
        private set

    val hasActiveAnimations: Boolean
        get() = hasActiveAnimationsSource()

    val code: String
        get() = storedSource()

    fun exportBoard(
        format: JsxGraphBoardExportFormat,
        noAttributes: Boolean = false,
        limits: JsxGraphBoardExportLimits =
            JsxGraphBoardExportLimits(),
    ): GMResult<String, JsxGraphBoardExportError> =
        exportBoardSource(format, noAttributes, limits)

    fun execute(
        source: String,
        storeSource: Boolean = true,
    ): GMResult<JsxGraphScene, JsxGraphJessieCodeError> =
        when (val result = executeSource(source, storeSource)) {
            is GMResult.Ok -> {
                scene = result.value
                result
            }
            is GMResult.Err -> result
        }

    fun movePoint(
        id: String,
        coordinates: JsxGraphPoint2D,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> =
        when (val result = movePointSource(id, coordinates)) {
            is GMResult.Ok -> {
                scene = result.value
                result
            }
            is GMResult.Err -> result
        }

    fun movePoint3D(
        id: String,
        coordinates: JsxGraphPoint3DCoordinates,
        timeMilliseconds: Double,
        effect: String? = null,
        callback: (() -> Unit)? = null,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> {
        if (!coordinates.isFinite()) {
            return GMResult.Err(
                JsxGraphInteractionError.NonFinitePoint3DCoordinates(
                    id = id,
                    coordinates = coordinates,
                ),
            )
        }
        return when (
            val result = movePoint3DSource(
                id,
                coordinates,
                timeMilliseconds,
                effect,
                callback,
            )
        ) {
            is GMResult.Ok -> {
                scene = result.value
                result
            }
            is GMResult.Err -> result
        }
    }

    fun movePoint3DAlong(
        id: String,
        path: List<JsxGraphPoint3DCoordinates>,
        timeMilliseconds: Double,
        callback: (() -> Unit)? = null,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> {
        path.firstOrNull { !it.isFinite() }?.let { coordinates ->
            return GMResult.Err(
                JsxGraphInteractionError.NonFinitePoint3DCoordinates(
                    id = id,
                    coordinates = coordinates,
                ),
            )
        }
        return when (
            val result = movePoint3DAlongSource(
                id,
                path,
                timeMilliseconds,
                callback,
            )
        ) {
            is GMResult.Ok -> {
                scene = result.value
                result
            }
            is GMResult.Err -> result
        }
    }

    fun interactControl(
        interaction: JsxGraphControlInteraction,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> =
        when (val result = interactControlSource(interaction)) {
            is GMResult.Ok -> {
                scene = result.value
                result
            }
            is GMResult.Err -> result
        }

    fun interactNavigation(
        action: JsxGraphNavigationAction,
    ): GMResult<
        JsxGraphNavigationInteractionResult,
        JsxGraphInteractionError,
        > = when (val result = interactNavigationSource(action)) {
        is GMResult.Ok -> {
            scene = result.value.scene
            result
        }
        is GMResult.Err -> result
    }

    fun interactPointer(
        interaction: JsxGraphBoardPointerInteraction,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> =
        when (val result = interactPointerSource(interaction)) {
            is GMResult.Ok -> {
                scene = result.value
                result
            }
            is GMResult.Err -> result
        }

    fun interactView3D(
        interaction: JsxGraphView3DInteraction,
    ): GMResult<
        JsxGraphView3DInteractionResult,
        JsxGraphInteractionError,
        > = when (val result = interactView3DSource(interaction)) {
        is GMResult.Ok -> {
            scene = result.value.scene
            result
        }
        is GMResult.Err -> result
    }

    fun advanceAnimations(
        elapsedMilliseconds: Double,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> {
        if (
            !elapsedMilliseconds.isFinite() ||
            elapsedMilliseconds < 0.0
        ) {
            return GMResult.Err(
                JsxGraphInteractionError.InvalidAnimationElapsedTime(
                    elapsedMilliseconds,
                ),
            )
        }
        return when (
            val result = advanceAnimationsSource(elapsedMilliseconds)
        ) {
            is GMResult.Ok -> {
                scene = result.value
                result
            }
            is GMResult.Err -> result
        }
    }

    fun clickButton(
        id: String,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> =
        interactControl(JsxGraphControlInteraction.ClickButton(id))

    fun changeCheckbox(
        id: String,
        checked: Boolean,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> =
        interactControl(
            JsxGraphControlInteraction.ChangeCheckbox(id, checked),
        )

    fun changeInput(
        id: String,
        value: String,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> =
        interactControl(
            JsxGraphControlInteraction.ChangeInput(id, value),
        )

    fun changeSlider(
        id: String,
        value: Double,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> =
        interactControl(
            JsxGraphControlInteraction.ChangeSlider(id, value),
        )
}

/**
 * Public entry point for the native JessieCode subset.
 *
 * The accepted language is the translated, bounded JessieCode grammar. It is
 * intentionally not a JavaScript compatibility API.
 */
object JsxGraphJessieCode {
    fun parse(
        source: String,
        boardOptions: JsxGraphJessieCodeBoardOptions =
            JsxGraphJessieCodeBoardOptions(),
        limits: JsxGraphJessieCodeLimits = JsxGraphJessieCodeLimits(),
    ): GMResult<JsxGraphScene, JsxGraphJessieCodeError> =
        when (val result = createSession(boardOptions, limits)) {
            is GMResult.Ok -> result.value.execute(source)
            is GMResult.Err -> result
        }

    fun createSession(
        boardOptions: JsxGraphJessieCodeBoardOptions =
            JsxGraphJessieCodeBoardOptions(),
        limits: JsxGraphJessieCodeLimits = JsxGraphJessieCodeLimits(),
    ): GMResult<
        JsxGraphJessieCodeSession,
        JsxGraphJessieCodeError,
        > = JsxGraphEngine.createJessieCodeSession(boardOptions, limits)
}
