/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> generated jison-lex lexer
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult

internal data class JessieCodeSourcePosition(
    val offset: Int,
    val line: Int,
    val column: Int,
)

internal data class JessieCodeSourceLocation(
    val start: JessieCodeSourcePosition,
    val end: JessieCodeSourcePosition,
)

internal enum class JessieCodeTokenType(
    val upstreamId: Int?,
) {
    EOF(5),
    IF(7),
    LEFT_PARENTHESIS(8),
    RIGHT_PARENTHESIS(10),
    ELSE(12),
    WHILE(14),
    FOR(15),
    SEMICOLON(16),
    DO(17),
    USE(19),
    IDENTIFIER(20),
    DELETE(21),
    RETURN(23),
    LEFT_BRACE(26),
    RIGHT_BRACE(27),
    ASSIGN(32),
    QUESTION(34),
    COLON(35),
    OR(37),
    AND(39),
    EQUAL(41),
    NOT_EQUAL(42),
    APPROX_EQUAL(43),
    LESS(45),
    GREATER(46),
    LESS_EQUAL(47),
    GREATER_EQUAL(48),
    PLUS(50),
    MINUS(51),
    MULTIPLY(53),
    DIVIDE(54),
    MODULO(55),
    EXPONENT(57),
    NOT(58),
    DOT(64),
    LEFT_BRACKET(65),
    RIGHT_BRACKET(66),
    NULL(74),
    TRUE(75),
    FALSE(76),
    STRING(77),
    NUMBER(78),
    NAN(79),
    INFINITY(80),
    SHIFT_LEFT(82),
    SHIFT_RIGHT(83),
    COMMA(86),
    FUNCTION(91),
    MAP(93),
    ARROW(94),
    HASH(null),
    INVALID(null),
}

internal data class JessieCodeToken(
    val type: JessieCodeTokenType,
    val lexeme: String,
    val location: JessieCodeSourceLocation,
)

internal data class JessieCodeLexerLimits(
    val maxSourceLength: Int = 1_000_000,
    val maxTokens: Int = 100_000,
)

internal sealed interface JessieCodeLexerError {
    data class InvalidLimits(
        val maxSourceLength: Int,
        val maxTokens: Int,
    ) : JessieCodeLexerError

    data class SourceLengthExceeded(
        val limit: Int,
        val actual: Int,
    ) : JessieCodeLexerError

    data class TokenLimitExceeded(
        val limit: Int,
        val location: JessieCodeSourcePosition,
    ) : JessieCodeLexerError
}

/**
 * Lexer translated from the jison-lex 0.3.4 output embedded in JessieCode.
 *
 * Lines are one-based and columns are zero-based, matching Jison locations.
 * Offsets and range ends use Kotlin String indices and are end-exclusive.
 */
internal class JessieCodeLexer(
    private val limits: JessieCodeLexerLimits = JessieCodeLexerLimits(),
) {
    // JSXGraph: src/parser/jessiecode.js -> generated lexer.lex
    internal fun tokenize(
        source: String,
    ): GMResult<List<JessieCodeToken>, JessieCodeLexerError> {
        if (limits.maxSourceLength < 0 || limits.maxTokens < 0) {
            return GMResult.Err(
                JessieCodeLexerError.InvalidLimits(
                    maxSourceLength = limits.maxSourceLength,
                    maxTokens = limits.maxTokens,
                ),
            )
        }
        if (source.length > limits.maxSourceLength) {
            return GMResult.Err(
                JessieCodeLexerError.SourceLengthExceeded(
                    limit = limits.maxSourceLength,
                    actual = source.length,
                ),
            )
        }

        val tokens = mutableListOf<JessieCodeToken>()
        var position = JessieCodeSourcePosition(
            offset = 0,
            line = 1,
            column = 0,
        )
        val blockCommentMatcher = BlockCommentMatcher(source)

        while (position.offset < source.length) {
            val ignoredLength = matchIgnoredLength(
                source = source,
                offset = position.offset,
                blockCommentMatcher = blockCommentMatcher,
            )
            if (ignoredLength > 0) {
                position = advance(source, position, ignoredLength)
                continue
            }

            if (tokens.size >= limits.maxTokens) {
                return GMResult.Err(
                    JessieCodeLexerError.TokenLimitExceeded(
                        limit = limits.maxTokens,
                        location = position,
                    ),
                )
            }

            val match = matchToken(source, position.offset)
            val end = advance(source, position, match.length)
            tokens += JessieCodeToken(
                type = match.type,
                lexeme = source.substring(position.offset, end.offset),
                location = JessieCodeSourceLocation(
                    start = position,
                    end = end,
                ),
            )
            position = end
        }

        tokens += JessieCodeToken(
            type = JessieCodeTokenType.EOF,
            lexeme = "",
            location = JessieCodeSourceLocation(
                start = position,
                end = position,
            ),
        )
        return GMResult.Ok(tokens)
    }

    private fun matchIgnoredLength(
        source: String,
        offset: Int,
        blockCommentMatcher: BlockCommentMatcher,
    ): Int {
        var end = offset
        while (end < source.length && source[end].isJessieCodeWhitespace()) {
            end += 1
        }
        if (end > offset) {
            return end - offset
        }

        if (source.startsWith("//", offset)) {
            end = offset + 2
            while (
                end < source.length &&
                !source[end].isJavaScriptLineTerminator()
            ) {
                end += 1
            }
            return end - offset
        }

        if (source.startsWith("/*", offset)) {
            return blockCommentMatcher.matchLength(offset)
        }

        return 0
    }

    // JSXGraph: src/parser/jessiecode.js -> generated lexer rules
    private fun matchToken(
        source: String,
        offset: Int,
    ): TokenMatch {
        NUMBER_WITH_EXPONENT.matchLengthAt(source, offset)
            .takeIf { it > 0 }
            ?.let { return TokenMatch(JessieCodeTokenType.NUMBER, it) }

        DECIMAL_WITH_INTEGER_PART.matchLengthAt(source, offset)
            .takeIf { it > 0 }
            ?.let { return TokenMatch(JessieCodeTokenType.NUMBER, it) }

        DECIMAL_WITHOUT_INTEGER_PART.matchLengthAt(source, offset)
            .takeIf {
                it > 0 &&
                    !source.charOrNull(offset + it).isAsciiWordCharacter()
            }
            ?.let { return TokenMatch(JessieCodeTokenType.NUMBER, it) }

        INTEGER.matchLengthAt(source, offset)
            .takeIf { it > 0 }
            ?.let { return TokenMatch(JessieCodeTokenType.NUMBER, it) }

        matchStringLength(source, offset, '"')
            .takeIf { it > 0 }
            ?.let { return TokenMatch(JessieCodeTokenType.STRING, it) }

        matchStringLength(source, offset, '\'')
            .takeIf { it > 0 }
            ?.let { return TokenMatch(JessieCodeTokenType.STRING, it) }

        for (rule in KEYWORD_RULES) {
            if (
                source.startsWith(rule.lexeme, offset) &&
                !source.charOrNull(offset + rule.lexeme.length)
                    .isAsciiWordCharacter()
            ) {
                return TokenMatch(rule.type, rule.lexeme.length)
            }
        }

        for (rule in OPERATOR_RULES) {
            if (source.startsWith(rule.lexeme, offset)) {
                return TokenMatch(rule.type, rule.lexeme.length)
            }
        }

        val identifierLength = matchIdentifierLength(source, offset)
        if (identifierLength > 0) {
            return TokenMatch(
                type = JessieCodeTokenType.IDENTIFIER,
                length = identifierLength,
            )
        }

        // The final upstream rule is `.` and returns INVALID.
        return TokenMatch(
            type = JessieCodeTokenType.INVALID,
            length = 1,
        )
    }

    private fun matchStringLength(
        source: String,
        offset: Int,
        quote: Char,
    ): Int {
        if (source[offset] != quote) {
            return 0
        }

        var end = offset + 1
        while (end < source.length) {
            if (
                source[end] == '\\' &&
                source.charOrNull(end + 1) == quote
            ) {
                end += 2
            } else if (source[end] == quote) {
                return end + 1 - offset
            } else {
                end += 1
            }
        }
        return 0
    }

    private fun matchIdentifierLength(
        source: String,
        offset: Int,
    ): Int {
        if (!source[offset].isAsciiIdentifierStart()) {
            return 0
        }

        var end = offset + 1
        while (
            end < source.length &&
            source[end].isAsciiIdentifierContinuation()
        ) {
            end += 1
        }
        return end - offset
    }

    private fun advance(
        source: String,
        start: JessieCodeSourcePosition,
        length: Int,
    ): JessieCodeSourcePosition {
        val end = start.offset + length
        var index = start.offset
        var line = start.line
        var column = start.column

        while (index < end) {
            when (source[index]) {
                '\r' -> {
                    if (index + 1 < end && source[index + 1] == '\n') {
                        index += 1
                    }
                    line += 1
                    column = 0
                }

                '\n' -> {
                    line += 1
                    column = 0
                }

                else -> column += 1
            }
            index += 1
        }

        return JessieCodeSourcePosition(
            offset = end,
            line = line,
            column = column,
        )
    }

    private data class TokenMatch(
        val type: JessieCodeTokenType,
        val length: Int,
    )

    private data class FixedRule(
        val lexeme: String,
        val type: JessieCodeTokenType,
    )

    private class BlockCommentMatcher(
        private val source: String,
    ) {
        private var cachedCommentEnd = -1
        private var commentEndExhausted = false
        private var cachedUnicodeSeparator = -1
        private var unicodeSeparatorExhausted = false

        fun matchLength(offset: Int): Int {
            val contentStart = offset + 2
            val commentEnd = nextCommentEnd(contentStart)
            if (commentEnd < 0) {
                return 0
            }

            val separator = nextUnicodeSeparator(contentStart)
            if (separator in contentStart until commentEnd) {
                return 0
            }
            return commentEnd + 2 - offset
        }

        private fun nextCommentEnd(startIndex: Int): Int {
            if (commentEndExhausted) {
                return -1
            }
            if (cachedCommentEnd >= startIndex) {
                return cachedCommentEnd
            }

            cachedCommentEnd = source.indexOf("*/", startIndex = startIndex)
            if (cachedCommentEnd < 0) {
                commentEndExhausted = true
            }
            return cachedCommentEnd
        }

        private fun nextUnicodeSeparator(startIndex: Int): Int {
            if (unicodeSeparatorExhausted) {
                return -1
            }
            if (cachedUnicodeSeparator >= startIndex) {
                return cachedUnicodeSeparator
            }

            var index = startIndex
            while (
                index < source.length &&
                source[index] != '\u2028' &&
                source[index] != '\u2029'
            ) {
                index += 1
            }
            cachedUnicodeSeparator =
                if (index < source.length) index else -1
            if (cachedUnicodeSeparator < 0) {
                unicodeSeparatorExhausted = true
            }
            return cachedUnicodeSeparator
        }
    }

    private companion object {
        val NUMBER_WITH_EXPONENT =
            Regex("[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)")
        val DECIMAL_WITH_INTEGER_PART = Regex("[0-9]+\\.[0-9]*")
        val DECIMAL_WITHOUT_INTEGER_PART = Regex("[0-9]*\\.[0-9]+")
        val INTEGER = Regex("[0-9]+")

        val KEYWORD_RULES = listOf(
            FixedRule("if", JessieCodeTokenType.IF),
            FixedRule("else", JessieCodeTokenType.ELSE),
            FixedRule("while", JessieCodeTokenType.WHILE),
            FixedRule("do", JessieCodeTokenType.DO),
            FixedRule("for", JessieCodeTokenType.FOR),
            FixedRule("function", JessieCodeTokenType.FUNCTION),
            FixedRule("map", JessieCodeTokenType.MAP),
            FixedRule("use", JessieCodeTokenType.USE),
            FixedRule("return", JessieCodeTokenType.RETURN),
            FixedRule("delete", JessieCodeTokenType.DELETE),
            FixedRule("true", JessieCodeTokenType.TRUE),
            FixedRule("false", JessieCodeTokenType.FALSE),
            FixedRule("null", JessieCodeTokenType.NULL),
            FixedRule("Infinity", JessieCodeTokenType.INFINITY),
            FixedRule("NaN", JessieCodeTokenType.NAN),
        )

        val OPERATOR_RULES = listOf(
            FixedRule("->", JessieCodeTokenType.ARROW),
            FixedRule("=>", JessieCodeTokenType.ARROW),
            FixedRule("<<", JessieCodeTokenType.SHIFT_LEFT),
            FixedRule(">>", JessieCodeTokenType.SHIFT_RIGHT),
            FixedRule("{", JessieCodeTokenType.LEFT_BRACE),
            FixedRule("}", JessieCodeTokenType.RIGHT_BRACE),
            FixedRule(";", JessieCodeTokenType.SEMICOLON),
            FixedRule("#", JessieCodeTokenType.HASH),
            FixedRule("?", JessieCodeTokenType.QUESTION),
            FixedRule(":", JessieCodeTokenType.COLON),
            FixedRule(".", JessieCodeTokenType.DOT),
            FixedRule("[", JessieCodeTokenType.LEFT_BRACKET),
            FixedRule("]", JessieCodeTokenType.RIGHT_BRACKET),
            FixedRule("(", JessieCodeTokenType.LEFT_PARENTHESIS),
            FixedRule(")", JessieCodeTokenType.RIGHT_PARENTHESIS),
            FixedRule("!", JessieCodeTokenType.NOT),
            FixedRule("^", JessieCodeTokenType.EXPONENT),
            FixedRule("**", JessieCodeTokenType.EXPONENT),
            FixedRule("*", JessieCodeTokenType.MULTIPLY),
            FixedRule("/", JessieCodeTokenType.DIVIDE),
            FixedRule("%", JessieCodeTokenType.MODULO),
            FixedRule("+", JessieCodeTokenType.PLUS),
            FixedRule("-", JessieCodeTokenType.MINUS),
            FixedRule("<=", JessieCodeTokenType.LESS_EQUAL),
            FixedRule("<", JessieCodeTokenType.LESS),
            FixedRule(">=", JessieCodeTokenType.GREATER_EQUAL),
            FixedRule(">", JessieCodeTokenType.GREATER),
            FixedRule("==", JessieCodeTokenType.EQUAL),
            FixedRule("~=", JessieCodeTokenType.APPROX_EQUAL),
            FixedRule("!=", JessieCodeTokenType.NOT_EQUAL),
            FixedRule("&&", JessieCodeTokenType.AND),
            FixedRule("||", JessieCodeTokenType.OR),
            FixedRule("=", JessieCodeTokenType.ASSIGN),
            FixedRule(",", JessieCodeTokenType.COMMA),
        )
    }
}

private fun Regex.matchLengthAt(
    source: String,
    offset: Int,
): Int {
    val match = matchAt(source, index = offset) ?: return 0
    return match.value.length
}

private fun Char?.isAsciiWordCharacter(): Boolean =
    this != null &&
        (
            this in 'A'..'Z' ||
                this in 'a'..'z' ||
                this in '0'..'9' ||
                this == '_'
            )

private fun Char.isAsciiIdentifierStart(): Boolean =
    this in 'A'..'Z' ||
        this in 'a'..'z' ||
        this == '_' ||
        this == '$'

private fun Char.isAsciiIdentifierContinuation(): Boolean =
    this in 'A'..'Z' ||
        this in 'a'..'z' ||
        this in '0'..'9' ||
        this == '_'

private fun Char.isJavaScriptLineTerminator(): Boolean =
    this == '\n' ||
        this == '\r' ||
        this == '\u2028' ||
        this == '\u2029'

private fun Char.isJessieCodeWhitespace(): Boolean =
    this == '\u0009' ||
        this == '\u000B' ||
        this == '\u000C' ||
        this == '\u0020' ||
        this == '\u00A0' ||
        this == '\u1680' ||
        this in '\u2000'..'\u200A' ||
        this == '\u2028' ||
        this == '\u2029' ||
        this == '\u202F' ||
        this == '\u205F' ||
        this == '\u3000' ||
        this == '\uFEFF' ||
        this == '\n' ||
        this == '\r'

private fun String.charOrNull(index: Int): Char? =
    if (index in indices) this[index] else null
