/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/tracenpoche.js -> TracenpocheReader.tokenize.
 * Copyright 2011-2013 Emmanuel Ostenne and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult

internal data class TracenpocheTokenizerLimits(
    val maxSourceCharacters: Int = 16 * 1024 * 1024,
    val maxTokens: Int = 1_000_000,
)

internal sealed interface TracenpocheTokenizerError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : TracenpocheTokenizerError

    data class SourceLimitExceeded(
        val limit: Int,
        val actual: Int,
    ) : TracenpocheTokenizerError

    data class TokenLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : TracenpocheTokenizerError
}

internal enum class TracenpocheTokenType(
    val upstreamName: String,
) {
    NAME("name"),
    NUMBER("number"),
    STRING("string"),
    OPERATOR("operator"),
}

internal sealed interface TracenpocheTokenValue {
    data class Text(
        val value: String,
    ) : TracenpocheTokenValue

    data class Number(
        val value: Double,
    ) : TracenpocheTokenValue
}

internal data class TracenpocheToken(
    val type: TracenpocheTokenType,
    val value: TracenpocheTokenValue,
    val from: Int,
    val to: Int,
)

internal data class TracenpocheTokenizerDiagnostic(
    val type: String,
    val value: String,
    val message: String,
)

internal data class TracenpocheTokenization(
    val tokens: List<TracenpocheToken>,
    val diagnostics: List<TracenpocheTokenizerDiagnostic>,
)

internal object TracenpocheTokenizer {
    // JSXGraph 1.13.3: src/reader/tracenpoche.js -> tokenize.
    internal fun tokenize(
        input: String,
        prefix: String = DEFAULT_PREFIX,
        suffix: String = DEFAULT_SUFFIX,
        limits: TracenpocheTokenizerLimits =
            TracenpocheTokenizerLimits(),
    ): GMResult<TracenpocheTokenization, TracenpocheTokenizerError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        if (input.length > limits.maxSourceCharacters) {
            return GMResult.Err(
                TracenpocheTokenizerError.SourceLimitExceeded(
                    limit = limits.maxSourceCharacters,
                    actual = input.length,
                ),
            )
        }

        val tokens = mutableListOf<TracenpocheToken>()
        val diagnostics =
            mutableListOf<TracenpocheTokenizerDiagnostic>()
        var index = 0
        var current = input.getOrNull(index)

        fun appendToken(
            type: TracenpocheTokenType,
            value: TracenpocheTokenValue,
            from: Int,
            to: Int,
        ): TracenpocheTokenizerError.TokenLimitExceeded? {
            if (tokens.size >= limits.maxTokens) {
                return TracenpocheTokenizerError.TokenLimitExceeded(
                    limit = limits.maxTokens,
                    requested = tokens.size + 1,
                )
            }
            tokens += TracenpocheToken(type, value, from, to)
            return null
        }

        fun diagnostic(
            type: String,
            value: String,
            message: String,
        ) {
            diagnostics += TracenpocheTokenizerDiagnostic(
                type = type,
                value = value,
                message = message,
            )
        }

        while (current != null) {
            val from = index
            val character = current
            when {
                character.code <= SPACE_CODE -> {
                    index += 1
                    current = input.getOrNull(index)
                }

                character.isAsciiLetter() -> {
                    val startsLowercase = character in 'a'..'z'
                    val value = StringBuilder().append(character)
                    index += 1
                    while (true) {
                        current = input.getOrNull(index)
                        val next = current ?: break
                        val accepted =
                            if (startsLowercase) {
                                next.isAsciiLetter() ||
                                    next.isAsciiDigit() ||
                                    next == '\''
                            } else {
                                next.isAsciiDigit() ||
                                    next == '\''
                            }
                        if (!accepted) {
                            break
                        }
                        value.append(next)
                        index += 1
                    }

                    val previous = tokens.lastOrNull()
                    if (
                        previous?.type == TracenpocheTokenType.NAME &&
                        previous.textValue() != "var" &&
                        previous.textValue() != "for"
                    ) {
                        appendToken(
                            type = TracenpocheTokenType.OPERATOR,
                            value = TracenpocheTokenValue.Text("#"),
                            from = from,
                            to = index,
                        )?.let { return GMResult.Err(it) }
                    }
                    appendToken(
                        type = TracenpocheTokenType.NAME,
                        value =
                            TracenpocheTokenValue.Text(value.toString()),
                        from = from,
                        to = index,
                    )?.let { return GMResult.Err(it) }
                }

                character.isAsciiDigit() -> {
                    val value = StringBuilder().append(character)
                    index += 1
                    current = input.getOrNull(index)
                    while (current?.isAsciiDigit() == true) {
                        value.append(current)
                        index += 1
                        current = input.getOrNull(index)
                    }
                    if (current == '.') {
                        value.append('.')
                        index += 1
                        current = input.getOrNull(index)
                        while (current?.isAsciiDigit() == true) {
                            value.append(current)
                            index += 1
                            current = input.getOrNull(index)
                        }
                    }
                    if (current == 'e' || current == 'E') {
                        value.append(current)
                        index += 1
                        current = input.getOrNull(index)
                        if (current == '-' || current == '+') {
                            value.append(current)
                            index += 1
                            current = input.getOrNull(index)
                        }
                        if (current?.isAsciiDigit() != true) {
                            diagnostic(
                                type = "number",
                                value = value.toString(),
                                message = "Bad exponent",
                            )
                        }
                        do {
                            index += 1
                            if (current != null) {
                                value.append(current)
                            }
                            current = input.getOrNull(index)
                        } while (current?.isAsciiDigit() == true)
                    }
                    if (current != null && current in 'a'..'z') {
                        index += 1
                        value.append(current)
                        diagnostic(
                            type = "number",
                            value = value.toString(),
                            message = "Bad number",
                        )
                    }

                    val number = value.toString().toDoubleOrNull()
                    if (number != null && number.isFinite()) {
                        appendToken(
                            type = TracenpocheTokenType.NUMBER,
                            value =
                                TracenpocheTokenValue.Number(number),
                            from = from,
                            to = index,
                        )?.let { return GMResult.Err(it) }
                    } else {
                        diagnostic(
                            type = "number",
                            value = value.toString(),
                            message = "Bad number",
                        )
                    }
                }

                character == '\'' || character == '"' -> {
                    val quote = character
                    val value = StringBuilder()
                    index += 1
                    while (true) {
                        current = input.getOrNull(index)
                        if (
                            current == null ||
                            current.code < SPACE_CODE
                        ) {
                            diagnostic(
                                type = "string",
                                value = value.toString(),
                                message =
                                    if (
                                        current == '\n' ||
                                        current == '\r' ||
                                        current == null
                                    ) {
                                        "Unterminated string."
                                    } else {
                                        "Control character in string."
                                    },
                            )
                            break
                        }
                        if (current == quote) {
                            break
                        }
                        if (current == '\\') {
                            index += 1
                            if (index >= input.length) {
                                diagnostic(
                                    type = "string",
                                    value = value.toString(),
                                    message = "Unterminated string",
                                )
                                break
                            }
                            current = input[index]
                            current = when (val escaped = current) {
                                'b' -> '\b'
                                'f' -> '\u000C'
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                'u' -> {
                                    val unicode = parseHexPrefix(
                                        input.substring(
                                            startIndex = index + 1,
                                            endIndex = minOf(
                                                index + 5,
                                                input.length,
                                            ),
                                        ),
                                    )
                                    if (unicode == null || unicode < 0) {
                                        diagnostic(
                                            type = "string",
                                            value = value.toString(),
                                            message =
                                                "Unterminated string",
                                        )
                                    }
                                    index += 4
                                    ((unicode ?: 0) and 0xFFFF).toChar()
                                }
                                else -> escaped
                            }
                        }
                        value.append(current)
                        index += 1
                    }
                    index += 1
                    appendToken(
                        type = TracenpocheTokenType.STRING,
                        value =
                            TracenpocheTokenValue.Text(value.toString()),
                        from = from,
                        to = index,
                    )?.let { return GMResult.Err(it) }
                    current = input.getOrNull(index)
                }

                character == '/' &&
                    input.getOrNull(index + 1) == '/' -> {
                    index += 1
                    while (true) {
                        current = input.getOrNull(index)
                        if (current == null) {
                            break
                        }
                        if (
                            current == '\n' ||
                            current == '\r'
                        ) {
                            break
                        }
                        index += 1
                    }
                }

                character in prefix -> {
                    val value = StringBuilder().append(character)
                    index += 1
                    while (
                        index < input.length &&
                        input[index] in suffix
                    ) {
                        value.append(input[index])
                        index += 1
                    }
                    appendToken(
                        type = TracenpocheTokenType.OPERATOR,
                        value =
                            TracenpocheTokenValue.Text(value.toString()),
                        from = from,
                        to = index,
                    )?.let { return GMResult.Err(it) }
                    current = input.getOrNull(index)
                }

                else -> {
                    index += 1
                    appendToken(
                        type = TracenpocheTokenType.OPERATOR,
                        value =
                            TracenpocheTokenValue.Text(
                                character.toString(),
                            ),
                        from = from,
                        to = index,
                    )?.let { return GMResult.Err(it) }
                    current = input.getOrNull(index)
                }
            }
        }

        return GMResult.Ok(
            TracenpocheTokenization(
                tokens = tokens.toList(),
                diagnostics = diagnostics.toList(),
            ),
        )
    }

    private fun validateLimits(
        limits: TracenpocheTokenizerLimits,
    ): TracenpocheTokenizerError.InvalidLimits? {
        val invalid = when {
            limits.maxSourceCharacters < 0 ->
                "maxSourceCharacters" to limits.maxSourceCharacters
            limits.maxTokens < 0 ->
                "maxTokens" to limits.maxTokens
            else -> null
        }
        return invalid?.let { (name, value) ->
            TracenpocheTokenizerError.InvalidLimits(name, value)
        }
    }

    private fun parseHexPrefix(value: String): Int? {
        val prefix = HEX_PREFIX.find(value)?.value ?: return null
        return prefix.toIntOrNull(16)
    }

    private fun Char.isAsciiLetter(): Boolean =
        this in 'a'..'z' || this in 'A'..'Z'

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

    private fun TracenpocheToken.textValue(): String? =
        (value as? TracenpocheTokenValue.Text)?.value

    private const val DEFAULT_PREFIX = "<>+-&"
    private const val DEFAULT_SUFFIX = "=>&:"
    private const val SPACE_CODE = 32
    private val HEX_PREFIX = Regex("^[0-9A-Fa-f]+")
}
