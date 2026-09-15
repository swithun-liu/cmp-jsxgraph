/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> generated Jison expression productions
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult

internal data class JessieCodeParserLimits(
    val maxAstNodes: Int = 100_000,
    val maxAstDepth: Int = 256,
    val maxParserNesting: Int = 64,
)

internal sealed interface JessieCodeParserError {
    data class Lexer(
        val error: JessieCodeLexerError,
    ) : JessieCodeParserError

    data class InvalidLimits(
        val maxAstNodes: Int,
        val maxAstDepth: Int,
        val maxParserNesting: Int,
    ) : JessieCodeParserError

    data class UnexpectedToken(
        val token: JessieCodeToken,
        val expected: List<JessieCodeTokenType>,
        val parserLocation: JessieCodeSourceLocation,
    ) : JessieCodeParserError

    data class UnsupportedSyntax(
        val token: JessieCodeToken,
        val feature: String,
    ) : JessieCodeParserError

    data class InvalidNumberLiteral(
        val token: JessieCodeToken,
    ) : JessieCodeParserError

    data class AstNodeLimitExceeded(
        val limit: Int,
        val location: JessieCodeSourcePosition,
    ) : JessieCodeParserError

    data class AstDepthLimitExceeded(
        val limit: Int,
        val location: JessieCodeSourcePosition,
    ) : JessieCodeParserError

    data class ParserNestingLimitExceeded(
        val limit: Int,
        val location: JessieCodeSourcePosition,
    ) : JessieCodeParserError
}

/**
 * JessieCode parser for an empty program or expression statements.
 *
 * This slice implements `StatementList`, blocks, `if` statements, expression
 * statements, assignment, array and object literals. Loops, return/use/delete
 * statements, functions, maps, and creator attributes are intentionally left
 * for later slices.
 */
internal class JessieCodeExpressionParser(
    private val lexerLimits: JessieCodeLexerLimits = JessieCodeLexerLimits(),
    private val parserLimits: JessieCodeParserLimits =
        JessieCodeParserLimits(),
) {
    // JSXGraph: src/parser/jessiecode.js -> Program and expression productions
    internal fun parse(
        source: String,
    ): GMResult<JessieCodeAstNode, JessieCodeParserError> {
        if (
            parserLimits.maxAstNodes < 1 ||
            parserLimits.maxAstDepth < 1 ||
            parserLimits.maxParserNesting
                !in 1..MAX_SUPPORTED_PARSER_NESTING
        ) {
            return GMResult.Err(
                JessieCodeParserError.InvalidLimits(
                    maxAstNodes = parserLimits.maxAstNodes,
                    maxAstDepth = parserLimits.maxAstDepth,
                    maxParserNesting =
                        parserLimits.maxParserNesting,
                ),
            )
        }

        val tokens = when (
            val result = JessieCodeLexer(lexerLimits).tokenize(source)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    JessieCodeParserError.Lexer(result.error),
                )
            }
        }

        return ParserState(
            tokens = tokens,
            limits = parserLimits,
        ).parseProgram()
    }

    private companion object {
        // Keeps recursive grammar descent below the Wasm browser stack limit.
        const val MAX_SUPPORTED_PARSER_NESTING = 64
    }
}

private class ParserState(
    private val tokens: List<JessieCodeToken>,
    private val limits: JessieCodeParserLimits,
) {
    private var tokenIndex = 0
    private var astNodeCount = 0
    private var syntacticNesting = 0
    private var parserLocation = INITIAL_SOURCE_LOCATION

    fun parseProgram(): ParserResult<JessieCodeAstNode> =
        when (
            val result = parseStatementList(
                terminator = JessieCodeTokenType.EOF,
                initialLocation = INITIAL_SOURCE_LOCATION,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(result.value.node)
            is GMResult.Err -> result
        }

    // JSXGraph: StatementList
    private fun parseStatementList(
        terminator: JessieCodeTokenType,
        initialLocation: JessieCodeSourceLocation,
    ): ParserResult<ParsedExpression> {
        val initial = when (
            val result = createNode(
                type = JessieCodeAstNodeType.OPERATION,
                value = JessieCodeAstValue.Text("op_none"),
                children = emptyList(),
                nodeLocation = initialLocation,
                childDepths = emptyList(),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        var program = initial
        while (current().type != terminator) {
            if (current().type == JessieCodeTokenType.EOF) {
                return unexpected(listOf(terminator))
            }
            val statement = when (val result = parseStatement()) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            program = when (
                val result = createNode(
                    type = JessieCodeAstNodeType.OPERATION,
                    value = JessieCodeAstValue.Text("op_none"),
                    children = listOf(
                        JessieCodeAstChild.Node(program.node),
                        JessieCodeAstChild.Node(statement.node),
                    ),
                    nodeLocation = program.span,
                    childDepths = listOf(
                        program.depth,
                        statement.depth,
                    ),
                )
            ) {
                is GMResult.Ok -> result.value.copy(
                    span = span(program.span, statement.span),
                )
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(
            ParsedExpression(
                node = program.node,
                span = program.span,
                depth = program.depth,
            ),
        )
    }

    // JSXGraph: Statement and ExpressionStatement
    private fun parseStatement(): ParserResult<ParsedExpression> =
        when (current().type) {
            JessieCodeTokenType.IF -> {
                val location = current().location
                nested(location) { parseIfStatement() }
            }
            JessieCodeTokenType.LEFT_BRACE -> {
                val location = current().location
                nested(location) { parseStatementBlock() }
            }
            JessieCodeTokenType.SEMICOLON -> parseEmptyStatement()
            JessieCodeTokenType.WHILE,
            JessieCodeTokenType.FOR,
            JessieCodeTokenType.DO,
            -> unsupported("loop statements")
            JessieCodeTokenType.USE,
            JessieCodeTokenType.DELETE,
            -> unsupported("unary statements")
            JessieCodeTokenType.RETURN ->
                unsupported("return statements")
            else -> parseExpressionStatement()
        }

    private fun parseExpressionStatement(): ParserResult<ParsedExpression> {
        val expression = when (val result = parseAssignment()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (current().type == JessieCodeTokenType.SHIFT_LEFT) {
            return unsupported("call attribute lists")
        }
        val semicolon = when (
            val result = expect(JessieCodeTokenType.SEMICOLON)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            expression.copy(
                span = span(expression.span, semicolon.location),
            ),
        )
    }

    // JSXGraph: IfStatement
    private fun parseIfStatement(): ParserResult<ParsedExpression> {
        val ifToken = consume()
        when (
            val result = expect(JessieCodeTokenType.LEFT_PARENTHESIS)
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val condition = when (val result = parseAssignment()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        when (
            val result = expect(JessieCodeTokenType.RIGHT_PARENTHESIS)
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val whenTrue = when (val result = parseStatement()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (current().type != JessieCodeTokenType.ELSE) {
            return operation(
                upstreamName = "op_if",
                children = listOf(condition, whenTrue),
                nodeLocation = ifToken.location,
                span = span(ifToken.location, whenTrue.span),
                isMath = null,
            )
        }

        consume()
        val whenFalse = when (val result = parseStatement()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return operation(
            upstreamName = "op_if_else",
            children = listOf(condition, whenTrue, whenFalse),
            nodeLocation = ifToken.location,
            span = span(ifToken.location, whenFalse.span),
            isMath = null,
        )
    }

    // JSXGraph: StatementBlock
    private fun parseStatementBlock(): ParserResult<ParsedExpression> {
        val opening = consume()
        val statements = when (
            val result = parseStatementList(
                terminator = JessieCodeTokenType.RIGHT_BRACE,
                initialLocation = opening.location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val closing = when (
            val result = expect(JessieCodeTokenType.RIGHT_BRACE)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return operation(
            upstreamName = "op_block",
            children = listOf(statements),
            nodeLocation = opening.location,
            span = span(opening.location, closing.location),
            isMath = null,
        )
    }

    // JSXGraph: EmptyStatement
    private fun parseEmptyStatement(): ParserResult<ParsedExpression> {
        val semicolon = consume()
        return operation(
            upstreamName = "op_none",
            children = emptyList(),
            nodeLocation = semicolon.location,
            span = semicolon.location,
            isMath = null,
        )
    }

    // JSXGraph: AssignmentExpression
    private fun parseAssignment(): ParserResult<ParsedExpression> {
        val left = when (val result = parseConditional()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (current().type != JessieCodeTokenType.ASSIGN) {
            return GMResult.Ok(left)
        }
        if (!left.isLeftHandSideExpression) {
            return unexpected(listOf(JessieCodeTokenType.SEMICOLON))
        }

        consume()
        val right = when (
            val result = nested(current().location) {
                parseAssignment()
            }
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return operation(
            upstreamName = "op_assign",
            children = listOf(left, right),
            nodeLocation = left.span,
            span = span(left.span, right.span),
            isMath = false,
        )
    }

    // JSXGraph: ConditionalExpression
    private fun parseConditional(): ParserResult<ParsedExpression> {
        val condition = when (val result = parseLogicalOr()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (current().type != JessieCodeTokenType.QUESTION) {
            return GMResult.Ok(condition)
        }

        consume()
        val whenTrue = when (
            val result = nested(current().location) {
                parseAssignment()
            }
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        when (val result = expect(JessieCodeTokenType.COLON)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val whenFalse = when (
            val result = nested(current().location) {
                parseAssignment()
            }
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        return operation(
            upstreamName = "op_conditional",
            children = listOf(
                condition,
                whenTrue,
                whenFalse,
            ),
            nodeLocation = condition.span,
            span = span(condition.span, whenFalse.span),
            isMath = false,
        )
    }

    // JSXGraph: LogicalORExpression
    private fun parseLogicalOr(): ParserResult<ParsedExpression> =
        parseBinary(
            operand = ::parseLogicalAnd,
            operators = LOGICAL_OR_OPERATORS,
        )

    // JSXGraph: LogicalANDExpression
    private fun parseLogicalAnd(): ParserResult<ParsedExpression> =
        parseBinary(
            operand = ::parseEquality,
            operators = LOGICAL_AND_OPERATORS,
        )

    // JSXGraph: EqualityExpression
    private fun parseEquality(): ParserResult<ParsedExpression> =
        parseBinary(
            operand = ::parseRelational,
            operators = EQUALITY_OPERATORS,
        )

    // JSXGraph: RelationalExpression
    private fun parseRelational(): ParserResult<ParsedExpression> =
        parseBinary(
            operand = ::parseAdditive,
            operators = RELATIONAL_OPERATORS,
        )

    // JSXGraph: AdditiveExpression
    private fun parseAdditive(): ParserResult<ParsedExpression> =
        parseBinary(
            operand = ::parseMultiplicative,
            operators = ADDITIVE_OPERATORS,
        )

    // JSXGraph: MultiplicativeExpression
    private fun parseMultiplicative(): ParserResult<ParsedExpression> =
        parseBinary(
            operand = ::parseExponent,
            operators = MULTIPLICATIVE_OPERATORS,
        )

    private fun parseBinary(
        operand: () -> ParserResult<ParsedExpression>,
        operators: Map<JessieCodeTokenType, BinaryOperator>,
    ): ParserResult<ParsedExpression> {
        var left = when (val result = operand()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        while (true) {
            val operator = operators[current().type]
                ?: return GMResult.Ok(left)
            consume()
            val right = when (val result = operand()) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            left = when (
                val result = operation(
                    upstreamName = operator.upstreamName,
                    children = listOf(left, right),
                    nodeLocation = left.span,
                    span = span(left.span, right.span),
                    isMath = operator.isMath,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        }
    }

    // JSXGraph: ExponentExpression and UnaryExpression
    private fun parseExponent(): ParserResult<ParsedExpression> {
        val prefix = PREFIX_OPERATORS[current().type]
        if (prefix != null) {
            val operatorToken = consume()
            val operand = when (
                val result = nested(operatorToken.location) {
                    parseExponent()
                }
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }

            if (prefix.upstreamName == null) {
                return GMResult.Ok(
                    operand.copy(
                        span = span(operatorToken.location, operand.span),
                        isLeftHandSideExpression = false,
                    ),
                )
            }
            return operation(
                upstreamName = prefix.upstreamName,
                children = listOf(operand),
                nodeLocation = operatorToken.location,
                span = span(operatorToken.location, operand.span),
                isMath = prefix.isMath,
            )
        }

        val base = when (val result = parsePostfix()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (current().type != JessieCodeTokenType.EXPONENT) {
            return GMResult.Ok(base)
        }

        val operatorToken = consume()
        val exponent = when (
            val result = nested(operatorToken.location) {
                parseExponent()
            }
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return operation(
            upstreamName = "op_exp",
            children = listOf(base, exponent),
            nodeLocation = base.span,
            span = span(base.span, exponent.span),
            isMath = true,
            isLeftHandSideExpression = false,
        )
    }

    // JSXGraph: MemberExpression and CallExpression
    private fun parsePostfix(): ParserResult<ParsedExpression> {
        var expression = when (val result = parsePrimary()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        while (true) {
            expression = when (current().type) {
                JessieCodeTokenType.DOT -> {
                    consume()
                    val property = when (
                        val result = expect(
                            JessieCodeTokenType.IDENTIFIER,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    when (
                        val result = operationWithRawChildren(
                            upstreamName = "op_property",
                            children = listOf(
                                JessieCodeAstChild.Node(expression.node),
                                JessieCodeAstChild.Text(property.lexeme),
                            ),
                            childDepths = listOf(expression.depth),
                            nodeLocation = expression.span,
                            span = span(
                                expression.span,
                                property.location,
                            ),
                            isMath = true,
                            isLeftHandSideExpression = true,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                }

                JessieCodeTokenType.LEFT_BRACKET -> {
                    consume()
                    val index = when (
                        val result = nested(current().location) {
                            parseAssignment()
                        }
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    val closing = when (
                        val result = expect(
                            JessieCodeTokenType.RIGHT_BRACKET,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    when (
                        val result = operation(
                            upstreamName = "op_extvalue",
                            children = listOf(expression, index),
                            nodeLocation = expression.span,
                            span = span(
                                expression.span,
                                closing.location,
                            ),
                            isMath = true,
                            isLeftHandSideExpression = true,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                }

                JessieCodeTokenType.LEFT_PARENTHESIS -> {
                    consume()
                    val arguments = when (
                        val result = parseExpressionList(
                            closingType =
                                JessieCodeTokenType.RIGHT_PARENTHESIS,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    val closing = when (
                        val result = expect(
                            JessieCodeTokenType.RIGHT_PARENTHESIS,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    val argumentNodes = arguments.map { it.node }
                    val argumentDepths = arguments.map { it.depth }
                    when (
                        val result = operationWithRawChildren(
                            upstreamName = "op_execfun",
                            children = listOf(
                                JessieCodeAstChild.Node(expression.node),
                                JessieCodeAstChild.NodeList(argumentNodes),
                            ),
                            childDepths =
                                listOf(expression.depth) + argumentDepths,
                            nodeLocation = expression.span,
                            span = span(
                                expression.span,
                                closing.location,
                            ),
                            isMath = true,
                            isLeftHandSideExpression = true,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                }

                JessieCodeTokenType.SHIFT_LEFT -> {
                    return unsupported("call attribute lists")
                }

                else -> return GMResult.Ok(expression)
            }
        }
    }

    // JSXGraph: PrimaryExpression, BasicLiteral, and ArrayLiteral
    private fun parsePrimary(): ParserResult<ParsedExpression> =
        when (current().type) {
            JessieCodeTokenType.IDENTIFIER -> variable(consume())
            JessieCodeTokenType.NULL -> constant(
                token = consume(),
                value = JessieCodeAstValue.Null,
                isMath = false,
            )

            JessieCodeTokenType.TRUE -> booleanConstant(
                token = consume(),
                value = true,
            )

            JessieCodeTokenType.FALSE -> booleanConstant(
                token = consume(),
                value = false,
            )

            JessieCodeTokenType.STRING -> string(consume())
            JessieCodeTokenType.NUMBER -> number(consume())
            JessieCodeTokenType.NAN -> constant(
                token = consume(),
                value = JessieCodeAstValue.Number(Double.NaN),
                isMath = true,
            )

            JessieCodeTokenType.INFINITY -> constant(
                token = consume(),
                value =
                    JessieCodeAstValue.Number(Double.POSITIVE_INFINITY),
                isMath = true,
            )

            JessieCodeTokenType.LEFT_PARENTHESIS ->
                parenthesizedExpression()

            JessieCodeTokenType.LEFT_BRACKET -> arrayLiteral()
            JessieCodeTokenType.FUNCTION -> {
                unsupported("function expressions")
            }

            JessieCodeTokenType.MAP -> unsupported("map expressions")
            JessieCodeTokenType.SHIFT_LEFT -> objectLiteral()

            else -> unexpected(EXPRESSION_START_TOKENS)
        }

    private fun parenthesizedExpression(): ParserResult<ParsedExpression> {
        val opening = consume()
        val expression = when (
            val result = nested(opening.location) {
                parseAssignment()
            }
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val closing = when (
            val result = expect(
                JessieCodeTokenType.RIGHT_PARENTHESIS,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            expression.copy(
                span = span(opening.location, closing.location),
                isLeftHandSideExpression = true,
            ),
        )
    }

    private fun arrayLiteral(): ParserResult<ParsedExpression> {
        val opening = consume()
        val elements = when (
            val result = parseExpressionList(
                closingType = JessieCodeTokenType.RIGHT_BRACKET,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val closing = when (
            val result = expect(JessieCodeTokenType.RIGHT_BRACKET)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val created = when (
            val result = createNode(
                type = JessieCodeAstNodeType.OPERATION,
                value = JessieCodeAstValue.Text("op_array"),
                children = listOf(
                    JessieCodeAstChild.NodeList(
                        elements.map { it.node },
                    ),
                ),
                nodeLocation = opening.location,
                childDepths = elements.map { it.depth },
                isMath = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            ParsedExpression(
                node = created.node,
                span = span(opening.location, closing.location),
                depth = created.depth,
                isLeftHandSideExpression = true,
            ),
        )
    }

    // JSXGraph: ObjectLiteral, PropertyList, Property, and PropertyName
    private fun objectLiteral(): ParserResult<ParsedExpression> {
        val opening = consume()
        if (current().type == JessieCodeTokenType.SHIFT_RIGHT) {
            val closing = consume()
            return operationWithRawChildren(
                upstreamName = "op_emptyobject",
                children = listOf(JessieCodeAstChild.EmptyObject),
                childDepths = emptyList(),
                nodeLocation = opening.location,
                span = span(opening.location, closing.location),
                isMath = false,
                isLeftHandSideExpression = true,
            )
        }

        val properties = when (val result = parseObjectPropertyList()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val closing = when (
            val result = expect(JessieCodeTokenType.SHIFT_RIGHT)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return operationWithRawChildren(
            upstreamName = "op_proplst_val",
            children = listOf(
                JessieCodeAstChild.Node(properties.node),
            ),
            childDepths = listOf(properties.depth),
            nodeLocation = opening.location,
            span = span(opening.location, closing.location),
            isMath = false,
            isLeftHandSideExpression = true,
        )
    }

    private fun parseObjectPropertyList(): ParserResult<ParsedExpression> {
        var properties = when (val result = parseObjectProperty()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        while (current().type == JessieCodeTokenType.COMMA) {
            consume()
            val property = when (val result = parseObjectProperty()) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val combined = when (
                val result = operation(
                    upstreamName = "op_proplst",
                    children = listOf(
                        properties,
                        property,
                    ),
                    nodeLocation = properties.span,
                    span = span(
                        properties.span,
                        property.span,
                    ),
                    isMath = null,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            properties = combined
        }
        return GMResult.Ok(properties)
    }

    private fun parseObjectProperty(): ParserResult<ParsedExpression> {
        val propertyName = when (val result = parseObjectPropertyName()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        when (val result = expect(JessieCodeTokenType.COLON)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val value = when (
            val result = nested(current().location) {
                parseAssignment()
            }
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val property = when (
            val result = operationWithRawChildren(
                upstreamName = "op_prop",
                children = listOf(
                    propertyName.child,
                    JessieCodeAstChild.Node(value.node),
                ),
                childDepths =
                    listOf(propertyName.depth, value.depth),
                nodeLocation = propertyName.location,
                span = span(propertyName.location, value.span),
                isMath = null,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(property)
    }

    private fun parseObjectPropertyName(): ParserResult<ParsedPropertyName> {
        val token = current()
        if (token.type == JessieCodeTokenType.IDENTIFIER) {
            consume()
            return GMResult.Ok(
                ParsedPropertyName(
                    child = JessieCodeAstChild.Text(token.lexeme),
                    location = token.location,
                    depth = 0,
                ),
            )
        }

        val literal = when (token.type) {
            JessieCodeTokenType.STRING -> {
                consume()
                leaf(
                    type = JessieCodeAstNodeType.STRING,
                    value = JessieCodeAstValue.Text(
                        token.lexeme.substring(
                            1,
                            token.lexeme.length - 1,
                        ),
                    ),
                    token = token,
                    isMath = null,
                )
            }
            JessieCodeTokenType.NUMBER -> {
                consume()
                val value = token.lexeme.toDoubleOrNull()
                    ?: return GMResult.Err(
                        JessieCodeParserError.InvalidNumberLiteral(token),
                    )
                constant(
                    token = token,
                    value = JessieCodeAstValue.Number(value),
                    isMath = null,
                )
            }
            JessieCodeTokenType.NAN -> {
                consume()
                constant(
                    token = token,
                    value = JessieCodeAstValue.Number(Double.NaN),
                    isMath = null,
                )
            }
            JessieCodeTokenType.INFINITY -> {
                consume()
                constant(
                    token = token,
                    value = JessieCodeAstValue.Number(
                        Double.POSITIVE_INFINITY,
                    ),
                    isMath = null,
                )
            }
            else -> return unexpected(OBJECT_PROPERTY_NAME_TOKENS)
        }
        return when (literal) {
            is GMResult.Ok -> GMResult.Ok(
                ParsedPropertyName(
                    child = JessieCodeAstChild.Node(literal.value.node),
                    location = token.location,
                    depth = literal.value.depth,
                ),
            )
            is GMResult.Err -> literal
        }
    }

    private fun parseExpressionList(
        closingType: JessieCodeTokenType,
    ): ParserResult<List<ParsedExpression>> {
        if (current().type == closingType) {
            return GMResult.Ok(emptyList())
        }

        val expressions = mutableListOf<ParsedExpression>()
        while (true) {
            val expression = when (
                val result = nested(current().location) {
                    parseAssignment()
                }
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            expressions += expression

            if (current().type != JessieCodeTokenType.COMMA) {
                return GMResult.Ok(expressions)
            }
            consume()
        }
    }

    private fun variable(
        token: JessieCodeToken,
    ): ParserResult<ParsedExpression> =
        leaf(
            type = JessieCodeAstNodeType.VARIABLE,
            value = JessieCodeAstValue.Text(token.lexeme),
            token = token,
            isMath = null,
        )

    private fun booleanConstant(
        token: JessieCodeToken,
        value: Boolean,
    ): ParserResult<ParsedExpression> =
        leaf(
            type = JessieCodeAstNodeType.BOOLEAN_CONSTANT,
            value = JessieCodeAstValue.Boolean(value),
            token = token,
            isMath = false,
        )

    private fun string(
        token: JessieCodeToken,
    ): ParserResult<ParsedExpression> =
        leaf(
            type = JessieCodeAstNodeType.STRING,
            value = JessieCodeAstValue.Text(
                token.lexeme.substring(1, token.lexeme.length - 1),
            ),
            token = token,
            isMath = false,
        )

    private fun number(
        token: JessieCodeToken,
    ): ParserResult<ParsedExpression> {
        val value = token.lexeme.toDoubleOrNull()
            ?: return GMResult.Err(
                JessieCodeParserError.InvalidNumberLiteral(token),
            )
        return constant(
            token = token,
            value = JessieCodeAstValue.Number(value),
            isMath = true,
        )
    }

    private fun constant(
        token: JessieCodeToken,
        value: JessieCodeAstValue,
        isMath: Boolean?,
    ): ParserResult<ParsedExpression> =
        leaf(
            type = JessieCodeAstNodeType.CONSTANT,
            value = value,
            token = token,
            isMath = isMath,
        )

    private fun leaf(
        type: JessieCodeAstNodeType,
        value: JessieCodeAstValue,
        token: JessieCodeToken,
        isMath: Boolean?,
    ): ParserResult<ParsedExpression> {
        val created = when (
            val result = createNode(
                type = type,
                value = value,
                children = emptyList(),
                nodeLocation = token.location,
                childDepths = emptyList(),
                isMath = isMath,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            ParsedExpression(
                node = created.node,
                span = token.location,
                depth = created.depth,
                isLeftHandSideExpression = true,
            ),
        )
    }

    private fun operation(
        upstreamName: String,
        children: List<ParsedExpression>,
        nodeLocation: JessieCodeSourceLocation,
        span: JessieCodeSourceLocation,
        isMath: Boolean?,
        isLeftHandSideExpression: Boolean = false,
    ): ParserResult<ParsedExpression> =
        operationWithRawChildren(
            upstreamName = upstreamName,
            children = children.map {
                JessieCodeAstChild.Node(it.node)
            },
            childDepths = children.map { it.depth },
            nodeLocation = nodeLocation,
            span = span,
            isMath = isMath,
            isLeftHandSideExpression = isLeftHandSideExpression,
        )

    private fun operationWithRawChildren(
        upstreamName: String,
        children: List<JessieCodeAstChild>,
        childDepths: List<Int>,
        nodeLocation: JessieCodeSourceLocation,
        span: JessieCodeSourceLocation,
        isMath: Boolean?,
        isLeftHandSideExpression: Boolean = false,
    ): ParserResult<ParsedExpression> {
        val created = when (
            val result = createNode(
                type = JessieCodeAstNodeType.OPERATION,
                value = JessieCodeAstValue.Text(upstreamName),
                children = children,
                nodeLocation = nodeLocation,
                childDepths = childDepths,
                isMath = isMath,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            ParsedExpression(
                node = created.node,
                span = span,
                depth = created.depth,
                isLeftHandSideExpression = isLeftHandSideExpression,
            ),
        )
    }

    // JSXGraph: AST.createNode
    private fun createNode(
        type: JessieCodeAstNodeType,
        value: JessieCodeAstValue,
        children: List<JessieCodeAstChild>,
        nodeLocation: JessieCodeSourceLocation,
        childDepths: List<Int>,
        isMath: Boolean? = null,
    ): ParserResult<CreatedNode> {
        if (astNodeCount >= limits.maxAstNodes) {
            return GMResult.Err(
                JessieCodeParserError.AstNodeLimitExceeded(
                    limit = limits.maxAstNodes,
                    location = nodeLocation.start,
                ),
            )
        }

        var maxChildDepth = 0
        for (depth in childDepths) {
            if (depth > maxChildDepth) {
                maxChildDepth = depth
            }
        }
        val depth = maxChildDepth + 1
        if (depth > limits.maxAstDepth) {
            return GMResult.Err(
                JessieCodeParserError.AstDepthLimitExceeded(
                    limit = limits.maxAstDepth,
                    location = nodeLocation.start,
                ),
            )
        }

        astNodeCount += 1
        return GMResult.Ok(
            CreatedNode(
                node = JessieCodeAstNode(
                    type = type,
                    value = value,
                    children = children,
                    location = nodeLocation.toAstLocation(),
                    isMath = isMath,
                ),
                depth = depth,
                span = nodeLocation,
            ),
        )
    }

    private fun expect(
        type: JessieCodeTokenType,
    ): ParserResult<JessieCodeToken> {
        if (current().type != type) {
            return unexpected(listOf(type))
        }
        return GMResult.Ok(consume())
    }

    private fun unexpected(
        expected: List<JessieCodeTokenType>,
    ): GMResult.Err<JessieCodeParserError.UnexpectedToken> =
        GMResult.Err(
            JessieCodeParserError.UnexpectedToken(
                token = current(),
                expected = expected,
                parserLocation = parserLocation,
            ),
        )

    private fun unsupported(
        feature: String,
    ): GMResult.Err<JessieCodeParserError.UnsupportedSyntax> =
        GMResult.Err(
            JessieCodeParserError.UnsupportedSyntax(
                token = current(),
                feature = feature,
            ),
        )

    private fun <T> nested(
        location: JessieCodeSourceLocation,
        block: () -> ParserResult<T>,
    ): ParserResult<T> {
        if (syntacticNesting >= limits.maxParserNesting) {
            return GMResult.Err(
                JessieCodeParserError.ParserNestingLimitExceeded(
                    limit = limits.maxParserNesting,
                    location = location.start,
                ),
            )
        }

        syntacticNesting += 1
        val result = block()
        syntacticNesting -= 1
        return result
    }

    private fun current(): JessieCodeToken = tokens[tokenIndex]

    private fun consume(): JessieCodeToken {
        val token = current()
        parserLocation = token.location
        if (token.type != JessieCodeTokenType.EOF) {
            tokenIndex += 1
        }
        return token
    }

    private data class BinaryOperator(
        val upstreamName: String,
        val isMath: Boolean,
    )

    private data class PrefixOperator(
        val upstreamName: String?,
        val isMath: Boolean,
    )

    private data class ParsedExpression(
        val node: JessieCodeAstNode,
        val span: JessieCodeSourceLocation,
        val depth: Int,
        val isLeftHandSideExpression: Boolean = false,
    )

    private data class ParsedPropertyName(
        val child: JessieCodeAstChild,
        val location: JessieCodeSourceLocation,
        val depth: Int,
    )

    private data class CreatedNode(
        val node: JessieCodeAstNode,
        val depth: Int,
        val span: JessieCodeSourceLocation,
    )

    private companion object {
        val INITIAL_SOURCE_LOCATION = JessieCodeSourceLocation(
            start = JessieCodeSourcePosition(
                offset = 0,
                line = 1,
                column = 0,
            ),
            end = JessieCodeSourcePosition(
                offset = 0,
                line = 1,
                column = 0,
            ),
        )

        val EXPRESSION_START_TOKENS = listOf(
            JessieCodeTokenType.LEFT_PARENTHESIS,
            JessieCodeTokenType.IDENTIFIER,
            JessieCodeTokenType.PLUS,
            JessieCodeTokenType.MINUS,
            JessieCodeTokenType.NOT,
            JessieCodeTokenType.LEFT_BRACKET,
            JessieCodeTokenType.NULL,
            JessieCodeTokenType.TRUE,
            JessieCodeTokenType.FALSE,
            JessieCodeTokenType.STRING,
            JessieCodeTokenType.NUMBER,
            JessieCodeTokenType.NAN,
            JessieCodeTokenType.INFINITY,
            JessieCodeTokenType.SHIFT_LEFT,
            JessieCodeTokenType.FUNCTION,
            JessieCodeTokenType.MAP,
        )

        val OBJECT_PROPERTY_NAME_TOKENS = listOf(
            JessieCodeTokenType.IDENTIFIER,
            JessieCodeTokenType.STRING,
            JessieCodeTokenType.NUMBER,
            JessieCodeTokenType.NAN,
            JessieCodeTokenType.INFINITY,
        )

        val LOGICAL_OR_OPERATORS = mapOf(
            JessieCodeTokenType.OR to BinaryOperator(
                upstreamName = "op_or",
                isMath = false,
            ),
        )

        val LOGICAL_AND_OPERATORS = mapOf(
            JessieCodeTokenType.AND to BinaryOperator(
                upstreamName = "op_and",
                isMath = false,
            ),
        )

        val EQUALITY_OPERATORS = mapOf(
            JessieCodeTokenType.EQUAL to BinaryOperator(
                upstreamName = "op_eq",
                isMath = false,
            ),
            JessieCodeTokenType.NOT_EQUAL to BinaryOperator(
                upstreamName = "op_neq",
                isMath = false,
            ),
            JessieCodeTokenType.APPROX_EQUAL to BinaryOperator(
                upstreamName = "op_approx",
                isMath = false,
            ),
        )

        val RELATIONAL_OPERATORS = mapOf(
            JessieCodeTokenType.LESS to BinaryOperator(
                upstreamName = "op_lt",
                isMath = false,
            ),
            JessieCodeTokenType.GREATER to BinaryOperator(
                upstreamName = "op_gt",
                isMath = false,
            ),
            JessieCodeTokenType.LESS_EQUAL to BinaryOperator(
                upstreamName = "op_leq",
                isMath = false,
            ),
            JessieCodeTokenType.GREATER_EQUAL to BinaryOperator(
                upstreamName = "op_geq",
                isMath = false,
            ),
        )

        val ADDITIVE_OPERATORS = mapOf(
            JessieCodeTokenType.PLUS to BinaryOperator(
                upstreamName = "op_add",
                isMath = true,
            ),
            JessieCodeTokenType.MINUS to BinaryOperator(
                upstreamName = "op_sub",
                isMath = true,
            ),
        )

        val MULTIPLICATIVE_OPERATORS = mapOf(
            JessieCodeTokenType.MULTIPLY to BinaryOperator(
                upstreamName = "op_mul",
                isMath = true,
            ),
            JessieCodeTokenType.DIVIDE to BinaryOperator(
                upstreamName = "op_div",
                isMath = true,
            ),
            JessieCodeTokenType.MODULO to BinaryOperator(
                upstreamName = "op_mod",
                isMath = true,
            ),
        )

        val PREFIX_OPERATORS = mapOf(
            JessieCodeTokenType.NOT to PrefixOperator(
                upstreamName = "op_not",
                isMath = false,
            ),
            JessieCodeTokenType.PLUS to PrefixOperator(
                upstreamName = null,
                isMath = true,
            ),
            JessieCodeTokenType.MINUS to PrefixOperator(
                upstreamName = "op_neg",
                isMath = true,
            ),
        )
    }
}

private typealias ParserResult<T> =
    GMResult<T, JessieCodeParserError>

private fun span(
    start: JessieCodeSourceLocation,
    end: JessieCodeSourceLocation,
): JessieCodeSourceLocation =
    JessieCodeSourceLocation(
        start = start.start,
        end = end.end,
    )

private fun JessieCodeSourceLocation.toAstLocation():
    JessieCodeAstLocation =
    JessieCodeAstLocation(
        line = start.line,
        column = start.column,
        endLine = end.line,
        endColumn = end.column,
    )
