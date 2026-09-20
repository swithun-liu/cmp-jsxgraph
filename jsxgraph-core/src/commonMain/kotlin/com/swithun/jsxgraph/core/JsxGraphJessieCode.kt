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
    val grid: Boolean = false,
    val keepAspectRatio: Boolean = false,
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
    val maxEvaluationSteps: Int = 100_000,
    val maxEvaluationDepth: Int = 64,
    val maxCollectionSize: Int = 100_000,
    val maxStoredSourceLength: Int = 1_000_000,
    val maxAttributeDepth: Int = 64,
    val maxObjects: Int = 10_000,
    val maxCurvePoints: Int = 10_000,
    val maxPolygonVertices: Int = 10_000,
    val maxTextLength: Int = 100_000,
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
 * Globals, functions, closures, Board selection, created elements, and Point
 * interaction state persist across calls. This class interprets JessieCode
 * only; it does not execute arbitrary JavaScript or expose a browser/DOM
 * environment.
 */
class JsxGraphJessieCodeSession internal constructor(
    initialScene: JsxGraphScene,
    private val executeSource: (
        source: String,
        storeSource: Boolean,
    ) -> GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    private val movePointSource: (
        id: String,
        coordinates: JsxGraphPoint2D,
    ) -> GMResult<JsxGraphScene, JsxGraphInteractionError>,
    private val storedSource: () -> String,
) {
    var scene: JsxGraphScene = initialScene
        private set

    val code: String
        get() = storedSource()

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
