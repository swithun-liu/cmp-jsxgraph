/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/geogebra.js -> ggbAct, ggbParse, and functionParse.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.Slider
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.utils.JsMath
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

internal data class GeogebraExpressionLimits(
    val maxTokens: Int = 100_000,
    val maxDepth: Int = 256,
    val maxEvaluations: Int = 100_000,
)

internal sealed interface GeogebraExpressionError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : GeogebraExpressionError

    data class UnexpectedCharacter(
        val offset: Int,
        val character: Char,
    ) : GeogebraExpressionError

    data class UnexpectedToken(
        val offset: Int,
        val token: String,
        val expected: String,
    ) : GeogebraExpressionError

    data class TokenLimitExceeded(
        val limit: Int,
    ) : GeogebraExpressionError

    data class DepthLimitExceeded(
        val limit: Int,
    ) : GeogebraExpressionError

    data class EvaluationLimitExceeded(
        val limit: Int,
    ) : GeogebraExpressionError

    data class MissingReference(
        val name: String,
    ) : GeogebraExpressionError

    data class InvalidOperation(
        val operator: String,
        val leftType: String?,
        val rightType: String,
    ) : GeogebraExpressionError

    data class InvalidFunction(
        val name: String,
        val argumentCount: Int,
    ) : GeogebraExpressionError

    data class ExpectedScalar(
        val actualType: String,
    ) : GeogebraExpressionError

    data class ExpectedCoordinates(
        val actualType: String,
    ) : GeogebraExpressionError

}

internal fun interface GeogebraScalarValue {
    fun evaluate(): GMResult<Double, GeogebraExpressionError>
}

internal sealed interface GeogebraReaderValue {
    data class Element(
        val value: GeometryElement,
    ) : GeogebraReaderValue

    data class ElementScalar(
        val element: GeometryElement,
        val scalar: GeogebraScalarValue,
    ) : GeogebraReaderValue

    data class Scalar(
        val value: GeogebraScalarValue,
    ) : GeogebraReaderValue
}

internal sealed interface GeogebraExpressionValue {
    data class Scalar(
        val value: Double,
    ) : GeogebraExpressionValue

    data class Coordinates(
        val x: Double,
        val y: Double,
        val vector: Boolean,
    ) : GeogebraExpressionValue

    data class Text(
        val value: String,
    ) : GeogebraExpressionValue

    data class Flag(
        val value: Boolean,
    ) : GeogebraExpressionValue
}

internal class CompiledGeogebraExpression private constructor(
    internal val source: String,
    private val root: Node,
    private val limits: GeogebraExpressionLimits,
) {
    internal fun evaluate(
        board: Board,
        values: Map<String, GeogebraReaderValue>,
        variables: Map<String, Double> = emptyMap(),
    ): GMResult<GeogebraExpressionValue, GeogebraExpressionError> =
        Evaluator(
            board = board,
            values = values,
            variables = variables,
            limits = limits,
        ).evaluate(root)

    internal fun coordinateFunction(
        board: Board,
        values: Map<String, GeogebraReaderValue>,
        variableNames: List<String>,
        component: Int?,
    ): JessieCodeCoordinateFunction =
        GeogebraCoordinateFunction(
            origin = source,
            expression = this,
            board = board,
            values = values,
            variableNames = variableNames,
            component = component,
        )

    internal fun dependencies(
        values: Map<String, GeogebraReaderValue>,
    ): Map<String, GeometryElement> =
        buildMap {
            root.collectReferences().forEach { name ->
                val element = when (val value = values[name]) {
                    is GeogebraReaderValue.Element -> value.value
                    is GeogebraReaderValue.ElementScalar ->
                        value.element
                    is GeogebraReaderValue.Scalar, null -> null
                }
                if (element != null) {
                    put(element.id, element)
                }
            }
        }

    internal companion object {
        internal fun compile(
            source: String,
            limits: GeogebraExpressionLimits =
                GeogebraExpressionLimits(),
        ): GMResult<
            CompiledGeogebraExpression,
            GeogebraExpressionError,
            > {
            validateLimits(limits)?.let { return GMResult.Err(it) }
            val tokens = when (
                val result = tokenize(
                    normalizeDefinition(source),
                    limits,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val root = when (
                val result = Parser(tokens, limits).parse()
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return GMResult.Ok(
                CompiledGeogebraExpression(
                    source = source,
                    root = root,
                    limits = limits,
                ),
            )
        }

        internal fun functionDefinition(
            source: String,
        ): GeogebraFunctionDefinition {
            val match = FUNCTION_DEFINITION.matchEntire(source.trim())
            return if (match == null) {
                GeogebraFunctionDefinition(
                    parameters = listOf("x"),
                    expression = source.trim(),
                )
            } else {
                GeogebraFunctionDefinition(
                    parameters = match.groupValues[2]
                        .split(',')
                        .map(String::trim)
                        .filter(String::isNotEmpty),
                    expression = match.groupValues[3].trim(),
                )
            }
        }

        private fun normalizeDefinition(source: String): String =
            functionDefinition(source).expression
                .replace(DEGREE_SUFFIX) { match ->
                    "(${match.groupValues[1]}*PI/180)"
                }

        private fun validateLimits(
            limits: GeogebraExpressionLimits,
        ): GeogebraExpressionError.InvalidLimits? {
            val invalid = when {
                limits.maxTokens < 0 ->
                    "maxTokens" to limits.maxTokens
                limits.maxDepth < 0 ->
                    "maxDepth" to limits.maxDepth
                limits.maxEvaluations < 0 ->
                    "maxEvaluations" to limits.maxEvaluations
                else -> null
            }
            return invalid?.let { (name, value) ->
                GeogebraExpressionError.InvalidLimits(name, value)
            }
        }

        private fun tokenize(
            source: String,
            limits: GeogebraExpressionLimits,
        ): GMResult<List<Token>, GeogebraExpressionError> {
            val raw = mutableListOf<Token>()
            var index = 0
            while (index < source.length) {
                val character = source[index]
                when {
                    character.isWhitespace() -> index += 1
                    character.isDigit() || (
                        character == '.' &&
                            source.getOrNull(index + 1)?.isDigit() == true
                        ) -> {
                        val start = index
                        index += 1
                        while (
                            source.getOrNull(index)?.let {
                                it.isDigit() || it == '.'
                            } == true
                        ) {
                            index += 1
                        }
                        if (
                            source.getOrNull(index) == 'e' ||
                            source.getOrNull(index) == 'E'
                        ) {
                            index += 1
                            if (
                                source.getOrNull(index) == '+' ||
                                source.getOrNull(index) == '-'
                            ) {
                                index += 1
                            }
                            while (
                                source.getOrNull(index)?.isDigit() == true
                            ) {
                                index += 1
                            }
                        }
                        raw += Token(
                            TokenType.NUMBER,
                            source.substring(start, index),
                            start,
                        )
                    }
                    character.isLetter() ||
                        character == '_' ||
                        character == '$' -> {
                        val start = index
                        index += 1
                        while (
                            source.getOrNull(index)?.let {
                                it.isLetterOrDigit() ||
                                    it == '_' ||
                                    it == '\'' ||
                                    it == '$'
                            } == true
                        ) {
                            index += 1
                        }
                        raw += Token(
                            TokenType.IDENTIFIER,
                            source.substring(start, index),
                            start,
                        )
                    }
                    character == '"' || character == '\'' -> {
                        val quote = character
                        val start = index
                        index += 1
                        val value = StringBuilder()
                        while (
                            index < source.length &&
                            source[index] != quote
                        ) {
                            if (
                                source[index] == '\\' &&
                                index + 1 < source.length
                            ) {
                                index += 1
                            }
                            value.append(source[index])
                            index += 1
                        }
                        if (index >= source.length) {
                            return GMResult.Err(
                                GeogebraExpressionError.UnexpectedToken(
                                    offset = start,
                                    token = source.substring(start),
                                    expected = "closing quote",
                                ),
                            )
                        }
                        index += 1
                        raw += Token(
                            TokenType.STRING,
                            value.toString(),
                            start,
                        )
                    }
                    else -> {
                        val two = source.substring(
                            index,
                            minOf(index + 2, source.length),
                        )
                        val operator =
                            if (two in DOUBLE_OPERATORS) two
                            else character.toString()
                        val type = when (operator) {
                            "(" -> TokenType.LEFT_PARENTHESIS
                            ")" -> TokenType.RIGHT_PARENTHESIS
                            "," -> TokenType.COMMA
                            in OPERATORS -> TokenType.OPERATOR
                            else -> {
                                return GMResult.Err(
                                    GeogebraExpressionError
                                        .UnexpectedCharacter(
                                            index,
                                            character,
                                        ),
                                )
                            }
                        }
                        raw += Token(type, operator, index)
                        index += operator.length
                    }
                }
                if (raw.size > limits.maxTokens) {
                    return GMResult.Err(
                        GeogebraExpressionError.TokenLimitExceeded(
                            limits.maxTokens,
                        ),
                    )
                }
            }
            val expanded = mutableListOf<Token>()
            for (token in raw) {
                val previous = expanded.lastOrNull()
                if (
                    previous != null &&
                    previous.canEndValue() &&
                    token.canStartValue() &&
                    !(
                        previous.type == TokenType.IDENTIFIER &&
                            token.type == TokenType.LEFT_PARENTHESIS
                        )
                ) {
                    expanded += Token(
                        TokenType.OPERATOR,
                        "*",
                        token.offset,
                    )
                }
                expanded += token
            }
            expanded += Token(TokenType.END, "", source.length)
            return GMResult.Ok(expanded)
        }

        private val FUNCTION_DEFINITION = Regex(
            """^\s*([A-Za-z_$][A-Za-z0-9_$']*)\s*""" +
                """\(([^)]*)\)\s*=\s*([\s\S]*)$""",
        )
        private val DEGREE_SUFFIX = Regex(
            """(\d+(?:\.\d+)?)\s*°""",
        )
        private val DOUBLE_OPERATORS = setOf(
            "<=",
            ">=",
            "==",
            "!=",
            "&&",
            "||",
        )
        private val OPERATORS = DOUBLE_OPERATORS + setOf(
            "+",
            "-",
            "*",
            "/",
            "%",
            "^",
            "!",
            "<",
            ">",
        )
    }

    private class Parser(
        private val tokens: List<Token>,
        private val limits: GeogebraExpressionLimits,
    ) {
        private var index = 0
        private var depth = 0

        fun parse(): GMResult<Node, GeogebraExpressionError> {
            val value = when (val result = expression(0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return if (current().type == TokenType.END) {
                GMResult.Ok(value)
            } else {
                unexpected("end of expression")
            }
        }

        private fun expression(
            rightBindingPower: Int,
        ): GMResult<Node, GeogebraExpressionError> {
            val token = current()
            index += 1
            var left = when (val result = prefix(token)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            while (rightBindingPower < bindingPower(current())) {
                val operator = current()
                index += 1
                left = when (val result = infix(operator, left)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }
            return GMResult.Ok(left)
        }

        private fun prefix(
            token: Token,
        ): GMResult<Node, GeogebraExpressionError> =
            when (token.type) {
                TokenType.NUMBER -> {
                    val value = token.text.toDoubleOrNull()
                        ?: return GMResult.Err(
                            GeogebraExpressionError.UnexpectedToken(
                                token.offset,
                                token.text,
                                "number",
                            ),
                        )
                    GMResult.Ok(Node.Number(value))
                }
                TokenType.STRING -> GMResult.Ok(Node.Text(token.text))
                TokenType.IDENTIFIER -> GMResult.Ok(Node.Reference(token.text))
                TokenType.OPERATOR ->
                    if (token.text == "-" || token.text == "!") {
                        nested {
                            when (
                                val result =
                                    expression(GEOGEBRA_UNARY_POWER)
                            ) {
                                is GMResult.Ok -> GMResult.Ok(
                                    Node.Unary(token.text, result.value),
                                )
                                is GMResult.Err -> result
                            }
                        }
                    } else {
                        unexpectedAt(token, "value")
                    }
                TokenType.LEFT_PARENTHESIS -> nested {
                    val first = when (val result = expression(0)) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@nested result
                    }
                    if (current().type == TokenType.COMMA) {
                        index += 1
                        val second = when (
                            val result = expression(0)
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return@nested result
                        }
                        if (
                            current().type !=
                            TokenType.RIGHT_PARENTHESIS
                        ) {
                            return@nested unexpected(
                                "closing parenthesis",
                            )
                        }
                        index += 1
                        GMResult.Ok(Node.Coordinates(first, second))
                    } else {
                        if (
                            current().type !=
                            TokenType.RIGHT_PARENTHESIS
                        ) {
                            return@nested unexpected(
                                "closing parenthesis",
                            )
                        }
                        index += 1
                        GMResult.Ok(first)
                    }
                }
                else -> unexpectedAt(token, "value")
            }

        private fun infix(
            token: Token,
            left: Node,
        ): GMResult<Node, GeogebraExpressionError> {
            if (token.type == TokenType.LEFT_PARENTHESIS) {
                val arguments = mutableListOf<Node>()
                if (current().type != TokenType.RIGHT_PARENTHESIS) {
                    while (true) {
                        when (val result = nested { expression(0) }) {
                            is GMResult.Ok -> arguments += result.value
                            is GMResult.Err -> return result
                        }
                        if (current().type != TokenType.COMMA) {
                            break
                        }
                        index += 1
                    }
                }
                if (current().type != TokenType.RIGHT_PARENTHESIS) {
                    return unexpected("closing parenthesis")
                }
                index += 1
                val reference = left as? Node.Reference
                    ?: return unexpectedAt(token, "function name")
                return GMResult.Ok(
                    Node.Call(reference.name, arguments),
                )
            }
            if (token.type != TokenType.OPERATOR) {
                return unexpectedAt(token, "operator")
            }
            val rightPower =
                if (token.text == "^") {
                    bindingPower(token) - 1
                } else {
                    bindingPower(token)
                }
            return when (val result = nested {
                expression(rightPower)
            }) {
                is GMResult.Ok -> GMResult.Ok(
                    Node.Binary(token.text, left, result.value),
                )
                is GMResult.Err -> result
            }
        }

        private fun bindingPower(token: Token): Int =
            when {
                token.type == TokenType.LEFT_PARENTHESIS -> 90
                token.type != TokenType.OPERATOR -> 0
                token.text == "||" -> 20
                token.text == "&&" -> 30
                token.text in setOf("<", "<=", ">", ">=", "==", "!=") ->
                    40
                token.text in setOf("+", "-", "%") -> 50
                token.text in setOf("*", "/") -> 60
                token.text == "^" -> 65
                else -> 0
            }

        private fun <T> nested(
            block: () -> GMResult<T, GeogebraExpressionError>,
        ): GMResult<T, GeogebraExpressionError> {
            depth += 1
            if (depth > limits.maxDepth) {
                depth -= 1
                return GMResult.Err(
                    GeogebraExpressionError.DepthLimitExceeded(
                        limits.maxDepth,
                    ),
                )
            }
            return try {
                block()
            } finally {
                depth -= 1
            }
        }

        private fun current(): Token = tokens[index]

        private fun <T> unexpected(
            expected: String,
        ): GMResult<T, GeogebraExpressionError> =
            unexpectedAt(current(), expected)

        private fun <T> unexpectedAt(
            token: Token,
            expected: String,
        ): GMResult<T, GeogebraExpressionError> =
            GMResult.Err(
                GeogebraExpressionError.UnexpectedToken(
                    token.offset,
                    token.text,
                    expected,
                ),
            )
    }

    private class Evaluator(
        private val board: Board,
        private val values: Map<String, GeogebraReaderValue>,
        private val variables: Map<String, Double>,
        private val limits: GeogebraExpressionLimits,
    ) {
        private var evaluations = 0

        fun evaluate(
            node: Node,
        ): GMResult<GeogebraExpressionValue, GeogebraExpressionError> {
            evaluations += 1
            if (evaluations > limits.maxEvaluations) {
                return GMResult.Err(
                    GeogebraExpressionError.EvaluationLimitExceeded(
                        limits.maxEvaluations,
                    ),
                )
            }
            return when (node) {
                is Node.Number ->
                    GMResult.Ok(
                        GeogebraExpressionValue.Scalar(node.value),
                    )
                is Node.Text ->
                    GMResult.Ok(
                        GeogebraExpressionValue.Text(node.value),
                    )
                is Node.Reference -> reference(node.name)
                is Node.Coordinates -> coordinates(node)
                is Node.Unary -> unary(node)
                is Node.Binary -> binary(node)
                is Node.Call -> call(node)
            }
        }

        private fun reference(
            name: String,
        ): GMResult<GeogebraExpressionValue, GeogebraExpressionError> {
            variables[name]?.let {
                return GMResult.Ok(
                    GeogebraExpressionValue.Scalar(it),
                )
            }
            if (name == "PI" || name == "pi") {
                return GMResult.Ok(
                    GeogebraExpressionValue.Scalar(PI),
                )
            }
            val value = values[name]
                ?: board.select(name)?.let(
                    GeogebraReaderValue::Element,
                )
                ?: return GMResult.Err(
                    GeogebraExpressionError.MissingReference(name),
                )
            return when (value) {
                is GeogebraReaderValue.Scalar ->
                    when (val result = value.value.evaluate()) {
                        is GMResult.Ok -> GMResult.Ok(
                            GeogebraExpressionValue.Scalar(
                                result.value,
                            ),
                        )
                        is GMResult.Err -> result
                    }
                is GeogebraReaderValue.ElementScalar ->
                    when (val result = value.scalar.evaluate()) {
                        is GMResult.Ok -> GMResult.Ok(
                            GeogebraExpressionValue.Scalar(
                                result.value,
                            ),
                        )
                        is GMResult.Err -> result
                    }
                is GeogebraReaderValue.Element ->
                    elementValue(value.value)
            }
        }

        private fun elementValue(
            element: GeometryElement,
        ): GMResult<GeogebraExpressionValue, GeogebraExpressionError> =
            when (element) {
                is Slider -> GMResult.Ok(
                    GeogebraExpressionValue.Scalar(element.Value()),
                )
                is Polygon -> GMResult.Ok(
                    GeogebraExpressionValue.Scalar(element.Area()),
                )
                is Sector -> GMResult.Ok(
                    GeogebraExpressionValue.Scalar(element.Value()),
                )
                is Line ->
                    if (element.elType == "arrow") {
                        GMResult.Ok(
                            GeogebraExpressionValue.Coordinates(
                                x = element.point2.X() -
                                    element.point1.X(),
                                y = element.point2.Y() -
                                    element.point1.Y(),
                                vector = true,
                            ),
                        )
                    } else {
                        GMResult.Ok(
                            GeogebraExpressionValue.Scalar(
                                element.point1.Dist(element.point2),
                            ),
                        )
                    }
                is CoordsElement -> GMResult.Ok(
                    GeogebraExpressionValue.Coordinates(
                        x = element.X(),
                        y = element.Y(),
                        vector = false,
                    ),
                )
                is Curve -> GMResult.Ok(
                    GeogebraExpressionValue.Scalar(element.Value()),
                )
                else -> GMResult.Err(
                    GeogebraExpressionError.MissingReference(
                        element.name,
                    ),
                )
            }

        private fun coordinates(
            node: Node.Coordinates,
        ): GMResult<GeogebraExpressionValue, GeogebraExpressionError> {
            val x = when (val result = scalar(node.x)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val y = when (val result = scalar(node.y)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return GMResult.Ok(
                GeogebraExpressionValue.Coordinates(
                    x = x,
                    y = y,
                    vector = false,
                ),
            )
        }

        private fun unary(
            node: Node.Unary,
        ): GMResult<GeogebraExpressionValue, GeogebraExpressionError> {
            val value = when (val result = evaluate(node.operand)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return when (node.operator) {
                "-" -> when (value) {
                    is GeogebraExpressionValue.Scalar ->
                        GMResult.Ok(value.copy(value = -value.value))
                    is GeogebraExpressionValue.Coordinates ->
                        GMResult.Ok(
                            value.copy(x = -value.x, y = -value.y),
                        )
                    else -> invalid(node.operator, null, value)
                }
                "!" -> GMResult.Ok(
                    GeogebraExpressionValue.Flag(!truthy(value)),
                )
                else -> invalid(node.operator, null, value)
            }
        }

        private fun binary(
            node: Node.Binary,
        ): GMResult<GeogebraExpressionValue, GeogebraExpressionError> {
            val left = when (val result = evaluate(node.left)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val right = when (val result = evaluate(node.right)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (node.operator == "&&" || node.operator == "||") {
                return GMResult.Ok(
                    GeogebraExpressionValue.Flag(
                        if (node.operator == "&&") {
                            truthy(left) && truthy(right)
                        } else {
                            truthy(left) || truthy(right)
                        },
                    ),
                )
            }
            if (node.operator in GEOGEBRA_COMPARISONS) {
                val first = when (val result = scalarValue(left)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val second = when (val result = scalarValue(right)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val value = when (node.operator) {
                    "<" -> first < second
                    "<=" -> first <= second
                    ">" -> first > second
                    ">=" -> first >= second
                    "==" -> first == second
                    else -> first != second
                }
                return GMResult.Ok(GeogebraExpressionValue.Flag(value))
            }
            if (
                left is GeogebraExpressionValue.Text ||
                right is GeogebraExpressionValue.Text
            ) {
                return if (node.operator == "+") {
                    GMResult.Ok(
                        GeogebraExpressionValue.Text(
                            text(left) + text(right),
                        ),
                    )
                } else {
                    invalid(node.operator, left, right)
                }
            }
            if (
                left is GeogebraExpressionValue.Coordinates ||
                right is GeogebraExpressionValue.Coordinates
            ) {
                return coordinateBinary(node.operator, left, right)
            }
            val first = when (val result = scalarValue(left)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (val result = scalarValue(right)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val value = when (node.operator) {
                "+" -> first + second
                "-" -> first - second
                "*" -> first * second
                "/" -> first / second
                "%" -> first % second
                "^" -> first.pow(second)
                else -> return invalid(node.operator, left, right)
            }
            return GMResult.Ok(GeogebraExpressionValue.Scalar(value))
        }

        private fun coordinateBinary(
            operator: String,
            left: GeogebraExpressionValue,
            right: GeogebraExpressionValue,
        ): GMResult<GeogebraExpressionValue, GeogebraExpressionError> {
            val first = left as? GeogebraExpressionValue.Coordinates
            val second = right as? GeogebraExpressionValue.Coordinates
            if (
                operator == "*" &&
                first?.vector == true &&
                second?.vector == true
            ) {
                return GMResult.Ok(
                    GeogebraExpressionValue.Scalar(
                        first.x * second.x + first.y * second.y,
                    ),
                )
            }
            val firstScalar =
                (left as? GeogebraExpressionValue.Scalar)?.value
            val secondScalar =
                (right as? GeogebraExpressionValue.Scalar)?.value
            val x: Double
            val y: Double
            when {
                first != null && second != null -> {
                    x = arithmetic(
                        operator,
                        first.x,
                        second.x,
                    ) ?: return invalid(operator, left, right)
                    y = arithmetic(
                        operator,
                        first.y,
                        second.y,
                    ) ?: return invalid(operator, left, right)
                }
                first != null && secondScalar != null -> {
                    x = arithmetic(
                        operator,
                        first.x,
                        secondScalar,
                    ) ?: return invalid(operator, left, right)
                    y = arithmetic(
                        operator,
                        first.y,
                        secondScalar,
                    ) ?: return invalid(operator, left, right)
                }
                firstScalar != null && second != null -> {
                    x = arithmetic(
                        operator,
                        firstScalar,
                        second.x,
                    ) ?: return invalid(operator, left, right)
                    y = arithmetic(
                        operator,
                        firstScalar,
                        second.y,
                    ) ?: return invalid(operator, left, right)
                }
                else -> return invalid(operator, left, right)
            }
            return GMResult.Ok(
                GeogebraExpressionValue.Coordinates(
                    x = x,
                    y = y,
                    vector = first?.vector == true ||
                        second?.vector == true,
                ),
            )
        }

        private fun call(
            node: Node.Call,
        ): GMResult<GeogebraExpressionValue, GeogebraExpressionError> {
            val arguments = mutableListOf<GeogebraExpressionValue>()
            for (argument in node.arguments) {
                when (val result = evaluate(argument)) {
                    is GMResult.Ok -> arguments += result.value
                    is GMResult.Err -> return result
                }
            }
            val name = node.name.lowercase()
            if (name == "x" || name == "y") {
                val coordinates = arguments.singleOrNull() as?
                    GeogebraExpressionValue.Coordinates
                    ?: return GMResult.Err(
                        GeogebraExpressionError.InvalidFunction(
                            node.name,
                            arguments.size,
                        ),
                    )
                return GMResult.Ok(
                    GeogebraExpressionValue.Scalar(
                        if (name == "x") {
                            coordinates.x
                        } else {
                            coordinates.y
                        },
                    ),
                )
            }
            if (name == "name") {
                val text = arguments.singleOrNull()?.let(::text)
                    ?: return GMResult.Err(
                        GeogebraExpressionError.InvalidFunction(
                            node.name,
                            arguments.size,
                        ),
                    )
                return GMResult.Ok(
                    GeogebraExpressionValue.Text(text),
                )
            }
            val scalars = mutableListOf<Double>()
            for (argument in arguments) {
                when (val result = scalarValue(argument)) {
                    is GMResult.Ok -> scalars += result.value
                    is GMResult.Err -> return result
                }
            }
            val value = when {
                name == "abs" && scalars.size == 1 -> abs(scalars[0])
                name == "acos" && scalars.size == 1 -> acos(scalars[0])
                name == "asin" && scalars.size == 1 -> asin(scalars[0])
                name == "atan" && scalars.size == 1 -> atan(scalars[0])
                name == "ceil" && scalars.size == 1 -> ceil(scalars[0])
                name == "cos" && scalars.size == 1 -> cos(scalars[0])
                name == "exp" && scalars.size == 1 -> exp(scalars[0])
                name == "floor" && scalars.size == 1 -> floor(scalars[0])
                name == "log" && scalars.size == 1 -> ln(scalars[0])
                name == "max" && scalars.size == 2 ->
                    max(scalars[0], scalars[1])
                name == "min" && scalars.size == 2 ->
                    min(scalars[0], scalars[1])
                name == "pow" && scalars.size == 2 ->
                    scalars[0].pow(scalars[1])
                name == "round" && scalars.size == 1 ->
                    JsMath.round(scalars[0])
                name == "sin" && scalars.size == 1 -> sin(scalars[0])
                name == "sqrt" && scalars.size == 1 -> sqrt(scalars[0])
                name == "tan" && scalars.size == 1 -> tan(scalars[0])
                arguments.size == 1 -> {
                    val factor = when (
                        val result = reference(node.name)
                    ) {
                        is GMResult.Ok -> scalarValue(result.value)
                        is GMResult.Err -> return result
                    }
                    when (factor) {
                        is GMResult.Ok -> factor.value * scalars[0]
                        is GMResult.Err -> return factor
                    }
                }
                else -> {
                    return GMResult.Err(
                        GeogebraExpressionError.InvalidFunction(
                            node.name,
                            arguments.size,
                        ),
                    )
                }
            }
            return GMResult.Ok(GeogebraExpressionValue.Scalar(value))
        }

        private fun scalar(
            node: Node,
        ): GMResult<Double, GeogebraExpressionError> =
            when (val result = evaluate(node)) {
                is GMResult.Ok -> scalarValue(result.value)
                is GMResult.Err -> result
            }

        private fun scalarValue(
            value: GeogebraExpressionValue,
        ): GMResult<Double, GeogebraExpressionError> =
            when (value) {
                is GeogebraExpressionValue.Scalar ->
                    GMResult.Ok(value.value)
                is GeogebraExpressionValue.Flag ->
                    GMResult.Ok(if (value.value) 1.0 else 0.0)
                else -> GMResult.Err(
                    GeogebraExpressionError.ExpectedScalar(
                        value.typeName(),
                    ),
                )
            }

        private fun invalid(
            operator: String,
            left: GeogebraExpressionValue?,
            right: GeogebraExpressionValue,
        ): GMResult<GeogebraExpressionValue, GeogebraExpressionError> =
            GMResult.Err(
                GeogebraExpressionError.InvalidOperation(
                    operator = operator,
                    leftType = left?.typeName(),
                    rightType = right.typeName(),
                ),
            )

        private fun arithmetic(
            operator: String,
            left: Double,
            right: Double,
        ): Double? =
            when (operator) {
                "+" -> left + right
                "-" -> left - right
                "*" -> left * right
                "/" -> left / right
                "%" -> left % right
                "^" -> left.pow(right)
                else -> null
            }
    }

    internal sealed interface Node {
        data class Number(val value: Double) : Node

        data class Text(val value: String) : Node

        data class Reference(val name: String) : Node

        data class Coordinates(val x: Node, val y: Node) : Node

        data class Unary(val operator: String, val operand: Node) : Node

        data class Binary(
            val operator: String,
            val left: Node,
            val right: Node,
        ) : Node

        data class Call(
            val name: String,
            val arguments: List<Node>,
        ) : Node
    }

    private data class Token(
        val type: TokenType,
        val text: String,
        val offset: Int,
    ) {
        fun canEndValue(): Boolean =
            type == TokenType.NUMBER ||
                type == TokenType.IDENTIFIER ||
                type == TokenType.STRING ||
                type == TokenType.RIGHT_PARENTHESIS

        fun canStartValue(): Boolean =
            type == TokenType.NUMBER ||
                type == TokenType.IDENTIFIER ||
                type == TokenType.STRING ||
                type == TokenType.LEFT_PARENTHESIS
    }

    private enum class TokenType {
        NUMBER,
        IDENTIFIER,
        STRING,
        OPERATOR,
        LEFT_PARENTHESIS,
        RIGHT_PARENTHESIS,
        COMMA,
        END,
    }

}

internal data class GeogebraFunctionDefinition(
    val parameters: List<String>,
    val expression: String,
)

private class GeogebraCoordinateFunction(
    override val origin: String,
    private val expression: CompiledGeogebraExpression,
    private val board: Board,
    private val values: Map<String, GeogebraReaderValue>,
    private val variableNames: List<String>,
    private val component: Int?,
) : JessieCodeCoordinateFunction {
    override val dependencies: Map<String, GeometryElement> =
        expression.dependencies(values)

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        val variables = linkedMapOf<String, Double>()
        for ((index, name) in variableNames.withIndex()) {
            val value = arguments.getOrNull(index) as?
                JessieCodeRuntimeValue.NumberValue
                ?: return GMResult.Ok(
                    JessieCodeRuntimeValue.NumberValue(Double.NaN),
                )
            variables[name] = value.value
        }
        return when (
            val result = expression.evaluate(
                board = board,
                values = values,
                variables = variables,
            )
        ) {
            is GMResult.Ok -> {
                val value = when (val evaluated = result.value) {
                    is GeogebraExpressionValue.Scalar ->
                        evaluated.value
                    is GeogebraExpressionValue.Flag ->
                        if (evaluated.value) 1.0 else 0.0
                    is GeogebraExpressionValue.Coordinates ->
                        when (component) {
                            0 -> evaluated.x
                            1 -> evaluated.y
                            else -> Double.NaN
                        }
                    is GeogebraExpressionValue.Text -> Double.NaN
                }
                GMResult.Ok(
                    JessieCodeRuntimeValue.NumberValue(value),
                )
            }
            is GMResult.Err -> GMResult.Err(
                JessieCodeRuntimeError.InvalidAst(
                    reason =
                        "GeoGebra expression '$origin' failed: " +
                            result.error,
                    location = JessieCodeAstLocation(
                        line = 1,
                        column = 1,
                        endLine = 1,
                        endColumn = origin.length + 1,
                    ),
                ),
            )
        }
    }
}

private fun CompiledGeogebraExpression.Node.collectReferences():
    Set<String> =
    when (this) {
        is CompiledGeogebraExpression.Node.Number,
        is CompiledGeogebraExpression.Node.Text,
        -> emptySet()
        is CompiledGeogebraExpression.Node.Reference -> setOf(name)
        is CompiledGeogebraExpression.Node.Coordinates ->
            x.collectReferences() + y.collectReferences()
        is CompiledGeogebraExpression.Node.Unary ->
            operand.collectReferences()
        is CompiledGeogebraExpression.Node.Binary ->
            left.collectReferences() + right.collectReferences()
        is CompiledGeogebraExpression.Node.Call ->
            arguments.flatMap {
                it.collectReferences()
            }.toSet()
    }

internal fun GeogebraExpressionValue.typeName(): String =
    when (this) {
        is GeogebraExpressionValue.Scalar -> "number"
        is GeogebraExpressionValue.Coordinates ->
            if (vector) "vector" else "point"
        is GeogebraExpressionValue.Text -> "string"
        is GeogebraExpressionValue.Flag -> "boolean"
    }

private fun truthy(value: GeogebraExpressionValue): Boolean =
    when (value) {
        is GeogebraExpressionValue.Scalar ->
            value.value != 0.0 && !value.value.isNaN()
        is GeogebraExpressionValue.Coordinates -> true
        is GeogebraExpressionValue.Text -> value.value.isNotEmpty()
        is GeogebraExpressionValue.Flag -> value.value
    }

private fun text(value: GeogebraExpressionValue): String =
    when (value) {
        is GeogebraExpressionValue.Scalar ->
            if (value.value == 0.0) {
                "0"
            } else {
                value.value.toString().removeSuffix(".0")
            }
        is GeogebraExpressionValue.Coordinates ->
            "(${value.x}, ${value.y})"
        is GeogebraExpressionValue.Text -> value.value
        is GeogebraExpressionValue.Flag -> value.value.toString()
    }

private const val GEOGEBRA_UNARY_POWER = 70
private val GEOGEBRA_COMPARISONS =
    setOf("<", "<=", ">", ">=", "==", "!=")
