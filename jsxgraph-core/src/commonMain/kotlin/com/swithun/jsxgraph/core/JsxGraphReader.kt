/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/jxg.js -> registerReader,
 * src/reader/file.js -> JXG.FileReader.parseString,
 * src/jsxgraph.js -> loadBoardFromString.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.createDefaultAxes
import com.swithun.jsxgraph.core.reader.FileReader
import com.swithun.jsxgraph.core.reader.ReaderError

data class JsxGraphReaderOptions(
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
    val runtimeOptions: JsxGraphRuntimeOptions =
        JsxGraphRuntimeOptions(),
)

enum class JsxGraphReaderFailureStage {
    CONSTRUCTION,
    READ,
    DOMAIN,
    SOURCE,
    CALLBACK,
}

sealed interface JsxGraphReaderError {
    val message: String

    data class InvalidConfiguration(
        override val message: String,
    ) : JsxGraphReaderError

    data class SourceLengthExceeded(
        val limit: Int,
        val actual: Int,
    ) : JsxGraphReaderError {
        override val message: String =
            "Reader source length $actual exceeds limit $limit."
    }

    data class UnknownFormat(
        val format: String,
    ) : JsxGraphReaderError {
        override val message: String =
            "No JSXGraph reader is registered for '$format'."
    }

    data class ReaderFailure(
        val format: String,
        val stage: JsxGraphReaderFailureStage,
        val reason: String?,
    ) : JsxGraphReaderError {
        override val message: String = buildString {
            append("Reader '")
            append(format)
            append("' failed during ")
            append(stage.name.lowercase())
            if (!reason.isNullOrEmpty()) {
                append(": ")
                append(reason)
            }
            append('.')
        }
    }

    data class ObjectLimitExceeded(
        val limit: Int,
        val actual: Int,
    ) : JsxGraphReaderError {
        override val message: String =
            "Reader created $actual Board objects; limit is $limit."
    }

    data class SceneSnapshotFailed(
        val cause: JsxGraphDocumentError,
    ) : JsxGraphReaderError {
        override val message: String = cause.message
    }
}

/**
 * Public pure-commonMain entry point for JSXGraph's registered text readers.
 *
 * Hosts remain responsible for network, filesystem, permission, and text
 * decoding policy. This API accepts already loaded text and never uses a
 * browser FileReader, XMLHttpRequest, DOM, or JavaScript runtime.
 */
object JsxGraphReader {
    val supportedFormats: Set<String>
        get() = JXG.readers().formats() + JESSIE_CODE_FORMAT

    fun parse(
        source: String,
        format: String,
        options: JsxGraphReaderOptions = JsxGraphReaderOptions(),
        limits: JsxGraphEngineLimits = JsxGraphEngineLimits(),
    ): GMResult<JsxGraphScene, JsxGraphReaderError> {
        JsxGraphEngine.validateLimits(limits)?.let { error ->
            return GMResult.Err(
                JsxGraphReaderError.InvalidConfiguration(error.message),
            )
        }
        validateOptions(options)?.let { error ->
            return GMResult.Err(error)
        }
        if (source.length > limits.maxSourceLength) {
            return GMResult.Err(
                JsxGraphReaderError.SourceLengthExceeded(
                    limit = limits.maxSourceLength,
                    actual = source.length,
                ),
            )
        }

        val boundingBox = options.boundingBox
        val board = Board.fromBoundingBox(
            boundingBox = doubleArrayOf(
                boundingBox.left,
                boundingBox.top,
                boundingBox.right,
                boundingBox.bottom,
            ),
            keepAspectRatio = options.keepAspectRatio,
            id = READER_BOARD_ID,
            defaultCurveMinimum =
                boundingBox.left -
                    (boundingBox.right - boundingBox.left) *
                    Board.CURVE_DOMAIN_PADDING,
            defaultCurveMaximum =
                boundingBox.right +
                    (boundingBox.right - boundingBox.left) *
                    Board.CURVE_DOMAIN_PADDING,
            maxCurvePoints = limits.maxCurvePoints,
            localeNumberFormatter =
                options.runtimeOptions.localeNumberFormatter,
            chartTableProvider =
                options.runtimeOptions.chartTableProvider,
        )
        if (options.axis || options.axisAttributes.isNotEmpty()) {
            when (
                val result = createDefaultAxes(
                    board = board,
                    axisAttributes = options.axisAttributes,
                    defaultAxesAttributes =
                        options.defaultAxesAttributes,
                )
            ) {
                is GMResult.Ok -> board.defaultAxes = result.value
                is GMResult.Err -> return GMResult.Err(
                    JsxGraphReaderError.ReaderFailure(
                        format = format.lowercase(),
                        stage = JsxGraphReaderFailureStage.CONSTRUCTION,
                        reason = result.error.toString(),
                    ),
                )
            }
        }
        val normalizedFormat = format.lowercase()
        when (
            val result = FileReader.parseString(
                source = source,
                board = board,
                format = normalizedFormat,
                registry = JXG.readers(),
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err ->
                return GMResult.Err(
                    publicError(normalizedFormat, result.error),
                )
        }
        board.fullUpdate()
        if (board.objectsList.size > limits.maxObjects) {
            return GMResult.Err(
                JsxGraphReaderError.ObjectLimitExceeded(
                    limit = limits.maxObjects,
                    actual = board.objectsList.size,
                ),
            )
        }
        return when (
            val result = JsxGraphEngine.snapshotReaderBoard(
                board = board,
                options = options,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                JsxGraphReaderError.SceneSnapshotFailed(result.error),
            )
        }
    }

    private fun validateOptions(
        options: JsxGraphReaderOptions,
    ): JsxGraphReaderError.InvalidConfiguration? {
        val boundingBox = options.boundingBox
        val values = listOf(
            boundingBox.left,
            boundingBox.top,
            boundingBox.right,
            boundingBox.bottom,
        )
        val invalid = when {
            values.any { !it.isFinite() } ->
                "boundingBox values must be finite"
            boundingBox.left >= boundingBox.right ->
                "boundingBox must have positive width"
            boundingBox.bottom >= boundingBox.top ->
                "boundingBox must have positive height"
            options.theme.length > MAX_THEME_LENGTH ->
                "theme length exceeds $MAX_THEME_LENGTH"
            else -> null
        }
        return invalid?.let(JsxGraphReaderError::InvalidConfiguration)
    }

    private fun publicError(
        format: String,
        error: ReaderError,
    ): JsxGraphReaderError =
        when (error) {
            is ReaderError.UnknownFormat ->
                JsxGraphReaderError.UnknownFormat(error.format)
            is ReaderError.ConstructionFailed ->
                failure(
                    format = error.format,
                    stage = JsxGraphReaderFailureStage.CONSTRUCTION,
                    reason = error.message,
                )
            is ReaderError.ReadFailed ->
                failure(
                    format = error.format,
                    stage = JsxGraphReaderFailureStage.READ,
                    reason = error.message,
                )
            is ReaderError.CallbackFailed ->
                failure(
                    format = error.format,
                    stage = JsxGraphReaderFailureStage.CALLBACK,
                    reason = error.message,
                )
            is ReaderError.DomainFailure ->
                failure(
                    format = format,
                    stage = JsxGraphReaderFailureStage.DOMAIN,
                    reason = error.cause.toString(),
                )
            is ReaderError.UnsupportedSource ->
                failure(
                    format = format,
                    stage = JsxGraphReaderFailureStage.SOURCE,
                    reason = "unsupported source ${error.source}",
                )
            is ReaderError.SourceLoadFailed ->
                failure(
                    format = format,
                    stage = JsxGraphReaderFailureStage.SOURCE,
                    reason = error.message,
                )
            ReaderError.RawContentHandlerMissing ->
                failure(
                    format = format,
                    stage = JsxGraphReaderFailureStage.SOURCE,
                    reason = "raw content handler is missing",
                )
        }

    private fun failure(
        format: String,
        stage: JsxGraphReaderFailureStage,
        reason: String?,
    ): JsxGraphReaderError.ReaderFailure =
        JsxGraphReaderError.ReaderFailure(
            format = format,
            stage = stage,
            reason = reason,
        )

    private const val JESSIE_CODE_FORMAT = "jessiecode"
    private const val READER_BOARD_ID = "jxgReaderBoard"
    private const val MAX_THEME_LENGTH = 128
}
