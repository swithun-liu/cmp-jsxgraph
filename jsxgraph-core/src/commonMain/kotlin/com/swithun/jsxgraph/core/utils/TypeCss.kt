/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/type.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

internal data class CssKeyValuePair(
    val key: String,
    val value: String,
)

/**
 * Source-compatible CSS string helpers from `JXG.Type`.
 */
internal object TypeCss {
    // JSXGraph 1.13.3: src/utils/type.js -> cssParse.
    fun parse(value: Any?): GMResult<Map<String, Any?>, TypeError> {
        if (value !is String) {
            return GMResult.Ok(emptyMap())
        }

        val transformed = buildString {
            append("{\"")
            append(
                ECMASCRIPT_TRAILING_SEMICOLON
                    .replace(value, "")
                    .let { source ->
                        ECMASCRIPT_SEMICOLON.replace(source, "\",\"")
                    }
                    .let { source ->
                        ECMASCRIPT_COLON.replace(source, "\":\"")
                    }
                    .trimEcmaScriptWhitespace(),
            )
            append("\"}")
        }
        return try {
            val parsed = Json.parseToJsonElement(transformed)
            val result = parsed as? JsonObject
                ?: return GMResult.Err(
                    TypeError.CssParseFailed(
                        message = "CSS conversion did not produce an object",
                    ),
                )
            GMResult.Ok(jsonObjectToMap(result))
        } catch (exception: Exception) {
            GMResult.Err(
                TypeError.CssParseFailed(
                    message = exception.message
                        ?: exception::class.simpleName.orEmpty(),
                ),
            )
        }
    }

    // JSXGraph 1.13.3: src/utils/type.js -> css2js.
    fun toKeyValuePairs(
        cssString: String,
    ): GMResult<List<CssKeyValuePair>, TypeError> {
        val normalized = cssString
            .trimEcmaScriptWhitespace()
            .removeSuffix(";")
        val pairs = mutableListOf<CssKeyValuePair>()
        for ((index, declaration) in normalized.split(';').withIndex()) {
            if (declaration.trimEcmaScriptWhitespace().isEmpty()) {
                continue
            }

            val parts = declaration.split(':')
            if (parts.size < 2) {
                return GMResult.Err(
                    TypeError.InvalidCssDeclaration(
                        index = index,
                        declaration = declaration,
                    ),
                )
            }
            val key = CSS_CAMEL_CASE
                .replace(parts[0]) { match ->
                    match.groupValues[1].uppercase()
                }
                .trimEcmaScriptWhitespace()
            pairs += CssKeyValuePair(
                key = key,
                value = parts[1].trimEcmaScriptWhitespace(),
            )
        }
        return GMResult.Ok(pairs)
    }

    // JSXGraph 1.13.3: src/utils/type.js -> cssStringify.
    fun stringify(styles: Any?): String {
        if (!Type.isObject(styles)) {
            return ""
        }
        val source = styles as? Map<*, *> ?: return ""
        val stringProperties = linkedMapOf<String, Any?>()
        for ((key, value) in source) {
            if (key is String) {
                stringProperties[key] = value
            }
        }

        return buildList {
            for (attribute in Type.keys(stringProperties)) {
                val value = stringProperties[attribute]
                val formatted = when (value) {
                    is String -> value
                    is Number -> Type.formatJsNumber(value.toDouble())
                    else -> continue
                }
                add("$attribute:$formatted;")
            }
        }.joinToString(separator = " ")
    }

    private fun jsonObjectToMap(
        value: JsonObject,
    ): Map<String, Any?> =
        linkedMapOf<String, Any?>().also { result ->
            for (key in Type.keys(value)) {
                result[key] = jsonElementToValue(value.getValue(key))
            }
        }

    private fun jsonElementToValue(value: JsonElement): Any? =
        when (value) {
            JsonNull -> null
            is JsonObject -> jsonObjectToMap(value)
            is JsonArray ->
                value.map { element -> jsonElementToValue(element) }
            is JsonPrimitive ->
                when {
                    value.isString -> value.content
                    value.booleanOrNull != null -> value.booleanOrNull
                    value.doubleOrNull != null -> value.doubleOrNull
                    else -> value.content
                }
        }

    private fun String.trimEcmaScriptWhitespace(): String {
        var start = 0
        while (
            start < length &&
            this[start].isEcmaScriptWhitespace()
        ) {
            start += 1
        }

        var end = length
        while (
            end > start &&
            this[end - 1].isEcmaScriptWhitespace()
        ) {
            end -= 1
        }
        return substring(start, end)
    }

    private fun Char.isEcmaScriptWhitespace(): Boolean =
        this in '\u0009'..'\u000D' ||
            this == '\u0020' ||
            this == '\u00A0' ||
            this == '\u1680' ||
            this in '\u2000'..'\u200A' ||
            this == '\u2028' ||
            this == '\u2029' ||
            this == '\u202F' ||
            this == '\u205F' ||
            this == '\u3000' ||
            this == '\uFEFF'

    private const val ECMASCRIPT_WHITESPACE =
        "\\u0009-\\u000D\\u0020\\u00A0\\u1680\\u2000-\\u200A" +
            "\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF"

    private val ECMASCRIPT_TRAILING_SEMICOLON =
        Regex("[$ECMASCRIPT_WHITESPACE]*;[$ECMASCRIPT_WHITESPACE]*$")
    private val ECMASCRIPT_SEMICOLON =
        Regex("[$ECMASCRIPT_WHITESPACE]*;[$ECMASCRIPT_WHITESPACE]*")
    private val ECMASCRIPT_COLON =
        Regex("[$ECMASCRIPT_WHITESPACE]*:[$ECMASCRIPT_WHITESPACE]*")
    private val CSS_CAMEL_CASE =
        Regex("-([a-z])", RegexOption.IGNORE_CASE)
}
