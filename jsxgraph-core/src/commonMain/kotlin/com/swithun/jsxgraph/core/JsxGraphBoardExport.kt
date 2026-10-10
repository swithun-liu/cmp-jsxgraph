/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/dump.js -> dump, toJessie, toJSON, toJavaScript
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.GeometryElementDumpError
import com.swithun.jsxgraph.core.utils.Dump
import com.swithun.jsxgraph.core.utils.DumpError
import com.swithun.jsxgraph.core.utils.DumpLimits
import com.swithun.jsxgraph.core.utils.TypeError

enum class JsxGraphBoardExportFormat {
    JessieCode,
    Json,
    JavaScript,
}

data class JsxGraphBoardExportLimits(
    val maxDepth: Int = 64,
    val maxValues: Int = 100_000,
    val maxElements: Int = 10_000,
    val maxOutputLength: Int = 1_000_000,
)

sealed interface JsxGraphBoardExportError {
    val message: String

    data class InvalidLimit(
        val name: String,
        val value: Int,
    ) : JsxGraphBoardExportError {
        override val message: String =
            "Board export limit $name has invalid value $value"
    }

    data class DepthLimitExceeded(
        val limit: Int,
    ) : JsxGraphBoardExportError {
        override val message: String =
            "Board export nesting exceeds limit $limit"
    }

    data class ValueLimitExceeded(
        val limit: Int,
    ) : JsxGraphBoardExportError {
        override val message: String =
            "Board export value count exceeds limit $limit"
    }

    data class ElementLimitExceeded(
        val limit: Int,
    ) : JsxGraphBoardExportError {
        override val message: String =
            "Board export element count exceeds limit $limit"
    }

    data class OutputLimitExceeded(
        val limit: Int,
    ) : JsxGraphBoardExportError {
        override val message: String =
            "Board export output exceeds length limit $limit"
    }

    data class ParentEvaluation(
        val elementId: String,
        val reason: String,
    ) : JsxGraphBoardExportError {
        override val message: String =
            "Unable to export parents for element '$elementId': $reason"
    }

    data class UnsupportedValue(
        val reason: String,
    ) : JsxGraphBoardExportError {
        override val message: String =
            "Board export contains an unsupported value: $reason"
    }
}

internal fun exportBoardState(
    board: Board,
    format: JsxGraphBoardExportFormat,
    noAttributes: Boolean,
    limits: JsxGraphBoardExportLimits,
): GMResult<String, JsxGraphBoardExportError> {
    val internalLimits = DumpLimits(
        maxDepth = limits.maxDepth,
        maxValues = limits.maxValues,
        maxElements = limits.maxElements,
        maxOutputLength = limits.maxOutputLength,
    )
    val result = when (format) {
        JsxGraphBoardExportFormat.JessieCode ->
            Dump.toJessie(
                board = board,
                noAttributes = noAttributes,
                limits = internalLimits,
            )
        JsxGraphBoardExportFormat.Json ->
            Dump.toJSON(
                board = board,
                limits = internalLimits,
            )
        JsxGraphBoardExportFormat.JavaScript ->
            Dump.toJavaScript(
                board = board,
                limits = internalLimits,
            )
    }
    return when (result) {
        is GMResult.Ok -> result
        is GMResult.Err -> GMResult.Err(result.error.toPublicError())
    }
}

private fun DumpError.toPublicError(): JsxGraphBoardExportError =
    when (this) {
        is DumpError.InvalidLimit ->
            JsxGraphBoardExportError.InvalidLimit(name, value)
        is DumpError.DepthLimitExceeded ->
            JsxGraphBoardExportError.DepthLimitExceeded(limit)
        is DumpError.ValueLimitExceeded ->
            JsxGraphBoardExportError.ValueLimitExceeded(limit)
        is DumpError.ElementLimitExceeded ->
            JsxGraphBoardExportError.ElementLimitExceeded(limit)
        is DumpError.OutputLimitExceeded ->
            JsxGraphBoardExportError.OutputLimitExceeded(limit)
        is DumpError.ParentEvaluation ->
            when (val failure = error) {
                is GeometryElementDumpError.ParentEvaluation ->
                    JsxGraphBoardExportError.ParentEvaluation(
                        elementId = failure.elementId,
                        reason = failure.reason,
                    )
            }
        is DumpError.AttributeOperation -> error.toPublicError()
        is DumpError.Serialization -> error.toPublicError()
        is DumpError.InvalidObjectKey,
        is DumpError.InvalidAttributeObject,
        DumpError.UnsupportedValue ->
            JsxGraphBoardExportError.UnsupportedValue(toString())
    }

private fun TypeError.toPublicError(): JsxGraphBoardExportError =
    when (this) {
        is TypeError.InvalidCopyLimit ->
            JsxGraphBoardExportError.InvalidLimit(name, value)
        is TypeError.InvalidJsonLimit ->
            JsxGraphBoardExportError.InvalidLimit(name, value)
        is TypeError.CopyDepthLimitExceeded ->
            JsxGraphBoardExportError.DepthLimitExceeded(limit)
        is TypeError.JsonDepthLimitExceeded ->
            JsxGraphBoardExportError.DepthLimitExceeded(limit)
        is TypeError.CopyValueLimitExceeded ->
            JsxGraphBoardExportError.ValueLimitExceeded(limit)
        is TypeError.JsonValueLimitExceeded ->
            JsxGraphBoardExportError.ValueLimitExceeded(limit)
        is TypeError.JsonOutputLimitExceeded ->
            JsxGraphBoardExportError.OutputLimitExceeded(limit)
        else -> JsxGraphBoardExportError.UnsupportedValue(toString())
    }
