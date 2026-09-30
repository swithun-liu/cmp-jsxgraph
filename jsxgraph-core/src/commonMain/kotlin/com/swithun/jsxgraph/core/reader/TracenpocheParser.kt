/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/tracenpoche.js -> TracenpocheReader.parse.
 * Copyright 2011-2013 Emmanuel Ostenne and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult

internal data class TracenpocheParserLimits(
    val tokenizer: TracenpocheTokenizerLimits =
        TracenpocheTokenizerLimits(),
    val maxStatements: Int = 100_000,
    val maxNestingDepth: Int = 256,
)

internal sealed interface TracenpocheParserError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : TracenpocheParserError

    data class TokenizationFailed(
        val cause: TracenpocheTokenizerError,
    ) : TracenpocheParserError

    data class UnexpectedEnd(
        val expected: String?,
    ) : TracenpocheParserError

    data class UnexpectedToken(
        val from: Int,
        val value: String,
        val expected: String,
    ) : TracenpocheParserError

    data class InvalidAssignmentTarget(
        val from: Int,
    ) : TracenpocheParserError

    data class StatementLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : TracenpocheParserError

    data class NestingLimitExceeded(
        val limit: Int,
    ) : TracenpocheParserError
}

internal sealed interface TracenpocheExpression {
    data class NumberLiteral(
        val value: Double,
    ) : TracenpocheExpression

    data class StringLiteral(
        val value: String,
    ) : TracenpocheExpression

    data class BooleanLiteral(
        val value: Boolean,
    ) : TracenpocheExpression

    data class Reference(
        val name: String,
    ) : TracenpocheExpression

    data class IndexedReference(
        val name: String,
        val index: TracenpocheExpression,
    ) : TracenpocheExpression

    data class Unary(
        val operator: String,
        val operand: TracenpocheExpression,
    ) : TracenpocheExpression

    data class Binary(
        val operator: String,
        val left: TracenpocheExpression,
        val right: TracenpocheExpression,
    ) : TracenpocheExpression

    data class Assignment(
        val name: String,
        val value: TracenpocheExpression,
        val index: TracenpocheExpression? = null,
    ) : TracenpocheExpression

    data class Call(
        val callee: String,
        val arguments: List<TracenpocheExpression>,
        val attributes: List<String>,
    ) : TracenpocheExpression

    data class Function(
        val expression: TracenpocheExpression,
    ) : TracenpocheExpression

    data class Conditional(
        val condition: TracenpocheExpression,
        val whenTrue: TracenpocheExpression,
        val whenFalse: TracenpocheExpression,
    ) : TracenpocheExpression
}

internal sealed interface TracenpocheStatement {
    data class Expression(
        val expression: TracenpocheExpression,
    ) : TracenpocheStatement

    data class For(
        val initializer: TracenpocheExpression.Assignment,
        val endInclusive: TracenpocheExpression,
        val body: List<TracenpocheStatement>,
    ) : TracenpocheStatement
}

internal data class ParsedTracenpocheProgram(
    val statements: List<TracenpocheStatement>,
    val diagnostics: List<TracenpocheTokenizerDiagnostic>,
)

internal object TracenpocheParser {
    /*
     * JSXGraph 1.13.3: src/reader/tracenpoche.js ->
     * parse / Pratt expression parser.
     */
    internal fun parse(
        source: String,
        limits: TracenpocheParserLimits = TracenpocheParserLimits(),
    ): GMResult<ParsedTracenpocheProgram, TracenpocheParserError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        val tokenization = when (
            val result = TracenpocheTokenizer.tokenize(
                input = source,
                prefix = COMBINING_PREFIX,
                suffix = COMBINING_SUFFIX,
                limits = limits.tokenizer,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    TracenpocheParserError.TokenizationFailed(result.error),
                )
            }
        }
        val parser = Parser(
            tokens = tokenization.tokens,
            limits = limits,
        )
        return when (val result = parser.parseProgram()) {
            is GMResult.Ok -> GMResult.Ok(
                ParsedTracenpocheProgram(
                    statements = result.value,
                    diagnostics = tokenization.diagnostics,
                ),
            )
            is GMResult.Err -> result
        }
    }

    private class Parser(
        private val tokens: List<TracenpocheToken>,
        private val limits: TracenpocheParserLimits,
    ) {
        private var index = 0
        private var nestingDepth = 0
        private var statementCount = 0

        fun parseProgram():
            GMResult<List<TracenpocheStatement>, TracenpocheParserError> =
            parseStatements(stopAtEnd = false)

        private fun parseStatements(
            stopAtEnd: Boolean,
        ): GMResult<List<TracenpocheStatement>, TracenpocheParserError> {
            val statements = mutableListOf<TracenpocheStatement>()
            while (index < tokens.size) {
                if (currentText() == END_KEYWORD) {
                    if (stopAtEnd) {
                        return GMResult.Ok(statements.toList())
                    }
                    return unexpectedCurrent("statement")
                }
                statementCount += 1
                if (statementCount > limits.maxStatements) {
                    return GMResult.Err(
                        TracenpocheParserError.StatementLimitExceeded(
                            limit = limits.maxStatements,
                            requested = statementCount,
                        ),
                    )
                }
                when (val result = parseStatement()) {
                    is GMResult.Ok -> statements += result.value
                    is GMResult.Err -> return result
                }
            }
            return if (stopAtEnd) {
                GMResult.Err(
                    TracenpocheParserError.UnexpectedEnd(END_KEYWORD),
                )
            } else {
                GMResult.Ok(statements.toList())
            }
        }

        private fun parseStatement():
            GMResult<TracenpocheStatement, TracenpocheParserError> {
            if (currentText() == VAR_KEYWORD) {
                index += 1
                return parseStatement()
            }
            if (currentText() == FOR_KEYWORD) {
                return parseFor()
            }
            val expression = when (val result = parseExpression(0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            when (val result = expect(OPERATOR_SEMICOLON)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            return GMResult.Ok(
                TracenpocheStatement.Expression(expression),
            )
        }

        private fun parseFor():
            GMResult<TracenpocheStatement, TracenpocheParserError> {
            index += 1
            val initializer = when (val result = parseExpression(0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (initializer !is TracenpocheExpression.Assignment) {
                return GMResult.Err(
                    TracenpocheParserError.InvalidAssignmentTarget(
                        from = current()?.from ?: sourceEnd(),
                    ),
                )
            }
            when (val result = expect(TO_KEYWORD)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            val end = when (val result = parseExpression(0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            when (val result = expect(DO_KEYWORD)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            if (currentText() == OPERATOR_SEMICOLON) {
                index += 1
            }
            val body = withNesting {
                parseStatements(stopAtEnd = true)
            }
            val statements = when (body) {
                is GMResult.Ok -> body.value
                is GMResult.Err -> return body
            }
            when (val result = expect(END_KEYWORD)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (val result = expect(OPERATOR_SEMICOLON)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            return GMResult.Ok(
                TracenpocheStatement.For(
                    initializer = initializer,
                    endInclusive = end,
                    body = statements,
                ),
            )
        }

        private fun parseExpression(
            rightBindingPower: Int,
        ): GMResult<TracenpocheExpression, TracenpocheParserError> {
            val first = current()
                ?: return GMResult.Err(
                    TracenpocheParserError.UnexpectedEnd("expression"),
                )
            index += 1
            var left = when (val result = parsePrefix(first)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            while (rightBindingPower < bindingPower(currentText())) {
                val operator = current()
                    ?: return GMResult.Err(
                        TracenpocheParserError.UnexpectedEnd("operator"),
                    )
                index += 1
                left = when (
                    val result = parseInfix(operator, left)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }
            return GMResult.Ok(left)
        }

        private fun parsePrefix(
            token: TracenpocheToken,
        ): GMResult<TracenpocheExpression, TracenpocheParserError> =
            when (token.type) {
                TracenpocheTokenType.NUMBER -> GMResult.Ok(
                    TracenpocheExpression.NumberLiteral(
                        (token.value as TracenpocheTokenValue.Number).value,
                    ),
                )
                TracenpocheTokenType.STRING -> GMResult.Ok(
                    TracenpocheExpression.StringLiteral(token.text()),
                )
                TracenpocheTokenType.NAME -> when (token.text()) {
                    TRUE_KEYWORD ->
                        GMResult.Ok(
                            TracenpocheExpression.BooleanLiteral(true),
                        )
                    FALSE_KEYWORD ->
                        GMResult.Ok(
                            TracenpocheExpression.BooleanLiteral(false),
                        )
                    FUNCTION_KEYWORD -> parseFunction()
                    else -> parseReference(token.text())
                }
                TracenpocheTokenType.OPERATOR ->
                    when (token.text()) {
                        OPERATOR_MINUS -> withNesting {
                            when (
                                val result =
                                    parseExpression(PREFIX_BINDING_POWER)
                            ) {
                                is GMResult.Ok -> GMResult.Ok(
                                    TracenpocheExpression.Unary(
                                        operator = OPERATOR_MINUS,
                                        operand = result.value,
                                    ),
                                )
                                is GMResult.Err -> result
                            }
                        }
                        OPERATOR_LEFT_BRACKET -> parseBracketExpression()
                        OPERATOR_LEFT_PARENTHESIS -> withNesting {
                            val expression = when (
                                val result = parseExpression(0)
                            ) {
                                is GMResult.Ok -> result.value
                                is GMResult.Err -> return@withNesting result
                            }
                            when (
                                val result =
                                    expect(OPERATOR_RIGHT_PARENTHESIS)
                            ) {
                                is GMResult.Ok -> GMResult.Ok(expression)
                                is GMResult.Err -> result
                            }
                        }
                        else -> unexpected(token, "expression")
                    }
            }

        private fun parseFunction():
            GMResult<TracenpocheExpression, TracenpocheParserError> =
            withNesting {
                when (
                    val result = expect(OPERATOR_LEFT_PARENTHESIS)
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return@withNesting result
                }
                val expression = when (
                    val result = parseExpression(0)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@withNesting result
                }
                when (
                    val result = expect(OPERATOR_RIGHT_PARENTHESIS)
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return@withNesting result
                }
                GMResult.Ok(
                    TracenpocheExpression.Function(expression),
                )
            }

        private fun parseInfix(
            token: TracenpocheToken,
            left: TracenpocheExpression,
        ): GMResult<TracenpocheExpression, TracenpocheParserError> =
            when (val operator = token.text()) {
                OPERATOR_LEFT_PARENTHESIS ->
                    parseCall(token, left)
                OPERATOR_ASSIGNMENT -> {
                    val target = when (left) {
                        is TracenpocheExpression.Reference ->
                            left.name to null
                        is TracenpocheExpression.IndexedReference ->
                            left.name to left.index
                        else -> null
                    }
                    if (target == null) {
                        return GMResult.Err(
                            TracenpocheParserError.InvalidAssignmentTarget(
                                token.from,
                            ),
                        )
                    }
                    /*
                     * JSXGraph 1.13.3 interprets `name=[...]` as an indexed
                     * assignment and then fails while reading its RHS.
                     */
                    if (currentText() == OPERATOR_LEFT_BRACKET) {
                        return unexpectedCurrent(
                            "right-hand expression after indexed assignment",
                        )
                    }
                    when (
                        val result =
                            parseExpression(ASSIGNMENT_BINDING_POWER - 1)
                    ) {
                        is GMResult.Ok -> GMResult.Ok(
                            TracenpocheExpression.Assignment(
                                name = target.first,
                                value = result.value,
                                index = target.second,
                            ),
                        )
                        is GMResult.Err -> result
                    }
                }
                in RIGHT_ASSOCIATIVE_OPERATORS -> {
                    when (
                        val result =
                            parseExpression(bindingPower(operator) - 1)
                    ) {
                        is GMResult.Ok -> GMResult.Ok(
                            TracenpocheExpression.Binary(
                                operator = operator,
                                left = left,
                                right = result.value,
                            ),
                        )
                        is GMResult.Err -> result
                    }
                }
                in LEFT_ASSOCIATIVE_OPERATORS -> {
                    when (
                        val result =
                            parseExpression(bindingPower(operator))
                    ) {
                        is GMResult.Ok -> GMResult.Ok(
                            TracenpocheExpression.Binary(
                                operator = operator,
                                left = left,
                                right = result.value,
                            ),
                        )
                        is GMResult.Err -> result
                    }
                }
                else -> unexpected(token, "infix operator")
            }

        private fun parseReference(
            name: String,
        ): GMResult<TracenpocheExpression, TracenpocheParserError> {
            if (currentText() != OPERATOR_LEFT_BRACKET) {
                return GMResult.Ok(
                    TracenpocheExpression.Reference(name),
                )
            }
            index += 1
            return when (val result = parseBracketExpression()) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheExpression.IndexedReference(
                        name = name,
                        index = result.value,
                    ),
                )
                is GMResult.Err -> result
            }
        }

        private fun parseBracketExpression():
            GMResult<TracenpocheExpression, TracenpocheParserError> =
            withNesting {
                val expressions = mutableListOf<TracenpocheExpression>()
                if (currentText() != OPERATOR_RIGHT_BRACKET) {
                    while (true) {
                        when (val result = parseExpression(0)) {
                            is GMResult.Ok -> expressions += result.value
                            is GMResult.Err -> return@withNesting result
                        }
                        if (currentText() != OPERATOR_COMMA) {
                            break
                        }
                        index += 1
                    }
                }
                when (
                    val result = expect(OPERATOR_RIGHT_BRACKET)
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return@withNesting result
                }
                when (expressions.size) {
                    1 -> GMResult.Ok(expressions[0])
                    3 -> GMResult.Ok(
                        TracenpocheExpression.Conditional(
                            condition = expressions[0],
                            whenTrue = expressions[1],
                            whenFalse = expressions[2],
                        ),
                    )
                    else -> unexpectedCurrent(
                        "one index or three conditional expressions",
                    )
                }
            }

        private fun parseCall(
            token: TracenpocheToken,
            left: TracenpocheExpression,
        ): GMResult<TracenpocheExpression, TracenpocheParserError> =
            withNesting {
                val callee = (left as? TracenpocheExpression.Reference)
                    ?.name
                    ?: return@withNesting unexpected(
                        token,
                        "named function",
                    )
                val arguments = mutableListOf<TracenpocheExpression>()
                if (currentText() != OPERATOR_RIGHT_PARENTHESIS) {
                    while (true) {
                        when (val result = parseExpression(0)) {
                            is GMResult.Ok -> arguments += result.value
                            is GMResult.Err -> return@withNesting result
                        }
                        if (currentText() != OPERATOR_COMMA) {
                            break
                        }
                        index += 1
                    }
                }
                when (
                    val result = expect(OPERATOR_RIGHT_PARENTHESIS)
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return@withNesting result
                }
                val attributes = when (
                    val result = parseAttributesIfPresent()
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@withNesting result
                }
                GMResult.Ok(
                    TracenpocheExpression.Call(
                        callee = callee,
                        arguments = arguments.toList(),
                        attributes = attributes,
                    ),
                )
            }

        private fun parseAttributesIfPresent():
            GMResult<List<String>, TracenpocheParserError> {
            if (currentText() != OPERATOR_LEFT_BRACE) {
                return GMResult.Ok(emptyList())
            }
            index += 1
            val attributes = mutableListOf<String>()
            if (currentText() != OPERATOR_RIGHT_BRACE) {
                while (true) {
                    val token = current()
                        ?: return GMResult.Err(
                            TracenpocheParserError.UnexpectedEnd(
                                "attribute",
                            ),
                        )
                    if (
                        token.type != TracenpocheTokenType.NAME &&
                        token.type != TracenpocheTokenType.STRING
                    ) {
                        return unexpected(token, "attribute")
                    }
                    attributes += token.text()
                    index += 1
                    if (currentText() != OPERATOR_COMMA) {
                        break
                    }
                    index += 1
                }
            }
            return when (val result = expect(OPERATOR_RIGHT_BRACE)) {
                is GMResult.Ok -> GMResult.Ok(attributes.toList())
                is GMResult.Err -> result
            }
        }

        private fun expect(
            value: String,
        ): GMResult<Unit, TracenpocheParserError> {
            val token = current()
                ?: return GMResult.Err(
                    TracenpocheParserError.UnexpectedEnd(value),
                )
            if (token.text() != value) {
                return unexpected(token, value)
            }
            index += 1
            return GMResult.Ok(Unit)
        }

        private fun <T> withNesting(
            block: () -> GMResult<T, TracenpocheParserError>,
        ): GMResult<T, TracenpocheParserError> {
            if (nestingDepth >= limits.maxNestingDepth) {
                return GMResult.Err(
                    TracenpocheParserError.NestingLimitExceeded(
                        limits.maxNestingDepth,
                    ),
                )
            }
            nestingDepth += 1
            val result = block()
            nestingDepth -= 1
            return result
        }

        private fun unexpectedCurrent(
            expected: String,
        ): GMResult.Err<TracenpocheParserError> {
            val token = current()
                ?: return GMResult.Err(
                    TracenpocheParserError.UnexpectedEnd(expected),
                )
            return unexpected(token, expected)
        }

        private fun unexpected(
            token: TracenpocheToken,
            expected: String,
        ): GMResult.Err<TracenpocheParserError> =
            GMResult.Err(
                TracenpocheParserError.UnexpectedToken(
                    from = token.from,
                    value = token.text(),
                    expected = expected,
                ),
            )

        private fun current(): TracenpocheToken? = tokens.getOrNull(index)

        private fun currentText(): String? = current()?.text()

        private fun sourceEnd(): Int = tokens.lastOrNull()?.to ?: 0
    }

    private fun bindingPower(operator: String?): Int =
        when (operator) {
            OPERATOR_ASSIGNMENT -> ASSIGNMENT_BINDING_POWER
            "&&",
            "||",
            -> LOGICAL_BINDING_POWER
            "==",
            "!=",
            "<",
            "<=",
            ">",
            ">=",
            -> COMPARISON_BINDING_POWER
            "+",
            "-",
            "%",
            "#",
            -> ADDITIVE_BINDING_POWER
            "*",
            "/",
            -> MULTIPLICATIVE_BINDING_POWER
            "^" -> POWER_BINDING_POWER
            OPERATOR_LEFT_PARENTHESIS -> CALL_BINDING_POWER
            else -> 0
        }

    private fun TracenpocheToken.text(): String =
        when (val tokenValue = value) {
            is TracenpocheTokenValue.Text -> tokenValue.value
            is TracenpocheTokenValue.Number -> tokenValue.value.toString()
        }

    private fun validateLimits(
        limits: TracenpocheParserLimits,
    ): TracenpocheParserError.InvalidLimits? {
        val invalid = when {
            limits.maxStatements < 0 ->
                "maxStatements" to limits.maxStatements
            limits.maxNestingDepth < 0 ->
                "maxNestingDepth" to limits.maxNestingDepth
            else -> null
        }
        return invalid?.let { (name, value) ->
            TracenpocheParserError.InvalidLimits(name, value)
        }
    }

    private const val COMBINING_PREFIX = "=<>!+-*&|/%^#"
    private const val COMBINING_SUFFIX = "=<>&|"
    private const val TRUE_KEYWORD = "true"
    private const val FALSE_KEYWORD = "false"
    private const val FUNCTION_KEYWORD = "fonction"
    private const val VAR_KEYWORD = "var"
    private const val FOR_KEYWORD = "for"
    private const val TO_KEYWORD = "to"
    private const val DO_KEYWORD = "do"
    private const val END_KEYWORD = "end"
    private const val OPERATOR_ASSIGNMENT = "="
    private const val OPERATOR_MINUS = "-"
    private const val OPERATOR_LEFT_PARENTHESIS = "("
    private const val OPERATOR_RIGHT_PARENTHESIS = ")"
    private const val OPERATOR_LEFT_BRACKET = "["
    private const val OPERATOR_RIGHT_BRACKET = "]"
    private const val OPERATOR_LEFT_BRACE = "{"
    private const val OPERATOR_RIGHT_BRACE = "}"
    private const val OPERATOR_COMMA = ","
    private const val OPERATOR_SEMICOLON = ";"
    private const val ASSIGNMENT_BINDING_POWER = 10
    private const val LOGICAL_BINDING_POWER = 30
    private const val COMPARISON_BINDING_POWER = 40
    private const val ADDITIVE_BINDING_POWER = 50
    private const val MULTIPLICATIVE_BINDING_POWER = 60
    private const val POWER_BINDING_POWER = 65
    private const val PREFIX_BINDING_POWER = 70
    private const val CALL_BINDING_POWER = 80

    private val RIGHT_ASSOCIATIVE_OPERATORS = setOf(
        "&&",
        "||",
        "==",
        "!=",
        "<",
        "<=",
        ">",
        ">=",
        "^",
    )
    private val LEFT_ASSOCIATIVE_OPERATORS = setOf(
        "+",
        "-",
        "*",
        "/",
        "%",
        "#",
    )
}
