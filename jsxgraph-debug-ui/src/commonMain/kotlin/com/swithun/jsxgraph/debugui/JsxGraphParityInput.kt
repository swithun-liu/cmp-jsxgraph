/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphBoundingBox
import com.swithun.jsxgraph.core.JsxGraphEngine
import com.swithun.jsxgraph.core.JsxGraphJessieCode
import com.swithun.jsxgraph.core.JsxGraphJessieCodeBoardOptions
import com.swithun.jsxgraph.core.JsxGraphJessieCodeSession
import com.swithun.jsxgraph.core.JsxGraphScene
import com.swithun.jsxgraph.core.JsxGraphSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

private const val JESSIE_CODE_INPUT_KIND = "jessiecode"
private const val OFFICIAL_PARITY_CONTAINER_ID = "jxgbox"
private const val PARITY_ENVELOPE_SCHEMA_VERSION = 1
private const val MAX_PARITY_SOURCE_LENGTH = 1_000_000

internal sealed interface JsxGraphParityInput {
    data class ConstructionDocument(
        val source: String,
    ) : JsxGraphParityInput

    data class JessieCode(
        val source: String,
        val boardOptions: JsxGraphJessieCodeBoardOptions,
    ) : JsxGraphParityInput
}

internal sealed interface JsxGraphParitySession {
    val scene: JsxGraphScene

    data class ConstructionDocument(
        val session: JsxGraphSession,
    ) : JsxGraphParitySession {
        override val scene: JsxGraphScene
            get() = session.scene
    }

    data class JessieCode(
        val session: JsxGraphJessieCodeSession,
    ) : JsxGraphParitySession {
        override val scene: JsxGraphScene
            get() = session.scene
    }
}

internal fun parseParitySource(
    source: String,
): GMResult<JsxGraphScene, String> =
    when (val result = createParitySession(source)) {
        is GMResult.Ok -> GMResult.Ok(result.value.scene)
        is GMResult.Err -> result
    }

internal fun createParitySession(
    source: String,
): GMResult<JsxGraphParitySession, String> =
    when (val input = parseParityInput(source)) {
        is GMResult.Err -> input
        is GMResult.Ok -> when (val value = input.value) {
            is JsxGraphParityInput.ConstructionDocument ->
                createConstructionDocumentSession(value.source)
            is JsxGraphParityInput.JessieCode ->
                createJessieCodeSession(value)
        }
    }

internal fun parseParityInput(
    source: String,
): GMResult<JsxGraphParityInput, String> {
    if (source.length > MAX_PARITY_SOURCE_LENGTH) {
        return GMResult.Err(
            "Parity source length ${source.length} exceeds limit " +
                MAX_PARITY_SOURCE_LENGTH,
        )
    }
    val root = try {
        Json.parseToJsonElement(source)
    } catch (failure: Exception) {
        return GMResult.Err(
            "Invalid parity source JSON: ${failure.message ?: "Invalid JSON"}",
        )
    }
    val rootObject = root as? JsonObject
        ?: return GMResult.Err("Parity source must be a JSON object")
    if ("inputKind" !in rootObject) {
        return GMResult.Ok(
            JsxGraphParityInput.ConstructionDocument(source),
        )
    }

    val allowedFields = setOf(
        "schemaVersion",
        "inputKind",
        "boardOptions",
        "source",
    )
    rootObject.keys.firstOrNull { field -> field !in allowedFields }?.let { field ->
        return GMResult.Err("Unsupported JessieCode parity field: $field")
    }
    val schemaVersion = (rootObject["schemaVersion"] as? JsonPrimitive)?.intOrNull
        ?: return GMResult.Err(
            "JessieCode parity schemaVersion must be the integer " +
                PARITY_ENVELOPE_SCHEMA_VERSION,
        )
    if (schemaVersion != PARITY_ENVELOPE_SCHEMA_VERSION) {
        return GMResult.Err(
            "Unsupported JessieCode parity schemaVersion: $schemaVersion",
        )
    }
    val inputKind = when (
        val result = requiredStringField(rootObject, "inputKind")
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> return result
    }
    if (inputKind != JESSIE_CODE_INPUT_KIND) {
        return GMResult.Err("Unsupported parity inputKind: $inputKind")
    }
    val boardOptions = when (
        val result = parseBoardOptions(rootObject["boardOptions"])
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> return result
    }
    val jessieCodeSource = when (
        val result = requiredStringField(rootObject, "source")
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> return result
    }
    return GMResult.Ok(
        JsxGraphParityInput.JessieCode(
            source = jessieCodeSource,
            boardOptions = boardOptions,
        ),
    )
}

private fun createConstructionDocumentSession(
    source: String,
): GMResult<JsxGraphParitySession, String> =
    when (val result = JsxGraphEngine.createSession(source)) {
        is GMResult.Ok -> GMResult.Ok(
            JsxGraphParitySession.ConstructionDocument(result.value),
        )
        is GMResult.Err -> GMResult.Err(result.error.message)
    }

private fun createJessieCodeSession(
    input: JsxGraphParityInput.JessieCode,
): GMResult<JsxGraphParitySession, String> {
    val session = when (
        val result = JsxGraphJessieCode.createSession(input.boardOptions)
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> return GMResult.Err(result.error.message)
    }
    return when (val result = session.execute(input.source)) {
        is GMResult.Ok -> GMResult.Ok(
            JsxGraphParitySession.JessieCode(session),
        )
        is GMResult.Err -> GMResult.Err(result.error.message)
    }
}

private fun parseBoardOptions(
    element: kotlinx.serialization.json.JsonElement?,
): GMResult<JsxGraphJessieCodeBoardOptions, String> {
    val boardOptions = element as? JsonObject
        ?: return GMResult.Err("JessieCode parity boardOptions must be an object")
    val allowedFields = setOf(
        "containerId",
        "boundingBox",
        "axis",
        "grid",
        "keepAspectRatio",
    )
    boardOptions.keys.firstOrNull { field -> field !in allowedFields }?.let { field ->
        return GMResult.Err("Unsupported JessieCode boardOptions field: $field")
    }
    val containerId = when (
        val result = requiredStringField(boardOptions, "containerId")
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> return result
    }
    if (containerId.isBlank()) {
        return GMResult.Err(
            "JessieCode parity boardOptions.containerId must not be blank",
        )
    }
    if (containerId != OFFICIAL_PARITY_CONTAINER_ID) {
        return GMResult.Err(
            "JessieCode parity boardOptions.containerId must be " +
                OFFICIAL_PARITY_CONTAINER_ID,
        )
    }
    val boundingBox = when (
        val result = parseBoundingBox(boardOptions["boundingBox"])
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> return result
    }
    val axis = when (
        val result = requiredBooleanField(boardOptions, "axis")
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> return result
    }
    val grid = when (
        val result = requiredBooleanField(boardOptions, "grid")
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> return result
    }
    val keepAspectRatio = when (
        val result = requiredBooleanField(boardOptions, "keepAspectRatio")
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> return result
    }
    return GMResult.Ok(
        JsxGraphJessieCodeBoardOptions(
            containerId = containerId,
            boundingBox = boundingBox,
            axis = axis,
            grid = grid,
            keepAspectRatio = keepAspectRatio,
        ),
    )
}

private fun parseBoundingBox(
    element: kotlinx.serialization.json.JsonElement?,
): GMResult<JsxGraphBoundingBox, String> {
    val values = element as? JsonArray
        ?: return GMResult.Err(
            "JessieCode parity boardOptions.boundingBox must contain four numbers",
        )
    if (values.size != 4) {
        return GMResult.Err(
            "JessieCode parity boardOptions.boundingBox must contain four numbers",
        )
    }
    val numbers = mutableListOf<Double>()
    for (value in values) {
        val number = (value as? JsonPrimitive)?.doubleOrNull
        if (number == null || !number.isFinite()) {
            return GMResult.Err(
                "JessieCode parity boardOptions.boundingBox must contain " +
                    "four finite numbers",
            )
        }
        numbers += number
    }
    if (numbers[0] >= numbers[2] || numbers[3] >= numbers[1]) {
        return GMResult.Err(
            "JessieCode parity boardOptions.boundingBox must have positive size",
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

private fun requiredStringField(
    value: JsonObject,
    field: String,
): GMResult<String, String> {
    val primitive = value[field] as? JsonPrimitive
    if (primitive == null || !primitive.isString) {
        return GMResult.Err("JessieCode parity $field must be a string")
    }
    return GMResult.Ok(primitive.content)
}

private fun requiredBooleanField(
    value: JsonObject,
    field: String,
): GMResult<Boolean, String> {
    val boolean = (value[field] as? JsonPrimitive)?.booleanOrNull
        ?: return GMResult.Err(
            "JessieCode parity boardOptions.$field must be a boolean",
        )
    return GMResult.Ok(boolean)
}
