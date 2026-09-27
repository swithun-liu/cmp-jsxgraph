/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/prefix.js -> PrefixParser
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.utils.JsMath
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.atanh
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.expm1
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.ln1p
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh
import kotlin.random.Random

internal data class PrefixParserLimits(
    val maxDepth: Int = 64,
    val maxNodes: Int = 10_000,
)

internal sealed interface PrefixParserError {
    data class InvalidLimits(
        val maxDepth: Int,
        val maxNodes: Int,
    ) : PrefixParserError

    data class DepthLimitExceeded(
        val limit: Int,
    ) : PrefixParserError

    data class NodeLimitExceeded(
        val limit: Int,
    ) : PrefixParserError

    data class InvalidTerm(
        val operation: String,
        val reason: String,
    ) : PrefixParserError

    data class Runtime(
        val error: JessieCodeRuntimeError,
    ) : PrefixParserError

    data class FunctionNotAllowed(
        val name: String,
    ) : PrefixParserError
}

/**
 * Safe translation of `JXG.PrefixParser`.
 *
 * JSXGraph accepts recursive JavaScript arrays. The Kotlin port keeps those
 * arrays as JessieCode runtime values and adds explicit resource bounds so a
 * hostile construction cannot exhaust the host stack or CPU.
 */
internal object PrefixParser {
    internal fun execute(
        board: Board,
        term: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
        limits: PrefixParserLimits = PrefixParserLimits(),
    ): GMResult<JessieCodeRuntimeValue, PrefixParserError> =
        withState(limits) { state ->
            execute(board, term, location, state, depth = 0)
        }

    internal fun dimension(
        board: Board,
        term: JessieCodeRuntimeValue,
        limits: PrefixParserLimits = PrefixParserLimits(),
    ): GMResult<Double, PrefixParserError> =
        withState(limits) { state ->
            dimension(board, term, state, depth = 0)
        }

    internal fun toPrefix(
        board: Board,
        term: JessieCodeRuntimeValue,
        limits: PrefixParserLimits = PrefixParserLimits(),
    ): GMResult<JessieCodeRuntimeValue, PrefixParserError> =
        withState(limits) { state ->
            toPrefix(board, term, state, depth = 0)
        }

    internal fun getParents(
        board: Board,
        term: JessieCodeRuntimeValue,
        limits: PrefixParserLimits = PrefixParserLimits(),
    ): GMResult<List<JessieCodeRuntimeValue>, PrefixParserError> =
        withState(limits) { state ->
            getParents(board, term, state, depth = 0)
        }

    private inline fun <T> withState(
        limits: PrefixParserLimits,
        operation: (PrefixParserState) -> GMResult<T, PrefixParserError>,
    ): GMResult<T, PrefixParserError> {
        if (limits.maxDepth <= 0 || limits.maxNodes <= 0) {
            return GMResult.Err(
                PrefixParserError.InvalidLimits(
                    maxDepth = limits.maxDepth,
                    maxNodes = limits.maxNodes,
                ),
            )
        }
        return operation(PrefixParserState(limits))
    }

    private fun execute(
        board: Board,
        term: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
        state: PrefixParserState,
        depth: Int,
    ): GMResult<JessieCodeRuntimeValue, PrefixParserError> {
        when (val budget = state.enter(depth)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return budget
        }
        if (
            term is JessieCodeRuntimeValue.NumberValue ||
            term is JessieCodeRuntimeValue.StringValue
        ) {
            return GMResult.Ok(term)
        }
        val values = arrayTerm(term, "parse") ?: return invalid(
            operation = "parse",
            reason = "term is not an array, number or string",
        )
        val method = methodName(values, "parse") ?: return invalid(
            operation = "parse",
            reason = "operator is not a string",
        )

        if (method in ARITHMETIC_OPERATORS) {
            var result = when (
                val parsed = execute(
                    board,
                    values[1],
                    location,
                    state,
                    depth + 1,
                )
            ) {
                is GMResult.Ok -> parsed.value
                is GMResult.Err -> return parsed
            }
            for (index in 2 until values.size) {
                val operand = when (
                    val parsed = execute(
                        board,
                        values[index],
                        location,
                        state,
                        depth + 1,
                    )
                ) {
                    is GMResult.Ok -> parsed.value
                    is GMResult.Err -> return parsed
                }
                result = arithmetic(method, result, operand)
            }
            return GMResult.Ok(result)
        }

        if (method == "exec") {
            val functionName = (
                values[1] as? JessieCodeRuntimeValue.StringValue
                )?.value ?: return invalid(
                operation = "parse",
                reason = "exec function name is not a string",
            )
            val arguments = mutableListOf<JessieCodeRuntimeValue>()
            for (index in 2 until values.size) {
                when (
                    val parsed = execute(
                        board,
                        values[index],
                        location,
                        state,
                        depth + 1,
                    )
                ) {
                    is GMResult.Ok -> arguments += parsed.value
                    is GMResult.Err -> return parsed
                }
            }
            return invokeMath(functionName, arguments)
        }

        val functionName = if (method == "V") "Value" else method
        val element = resolveElement(board, values[1]) ?: return invalid(
            operation = "parse",
            reason = "$functionName receiver is not an element",
        )
        if (functionName == "Coords") {
            if (values.size == 2) {
                values += JessieCodeRuntimeValue.StringValue("true")
            } else {
                values[2] = JessieCodeRuntimeValue.StringValue("true")
            }
        }
        val arguments = mutableListOf<JessieCodeRuntimeValue>()
        for (index in 2 until values.size) {
            when (
                val parsed = execute(
                    board,
                    values[index],
                    location,
                    state,
                    depth + 1,
                )
            ) {
                is GMResult.Ok -> arguments += parsed.value
                is GMResult.Err -> return parsed
            }
        }
        val property = when (
            val resolved = CoreGeometryElementRuntime.resolveProperty(
                element = element,
                property = functionName,
                location = location,
            )
        ) {
            is GMResult.Ok -> resolved.value
            is GMResult.Err -> return GMResult.Err(
                PrefixParserError.Runtime(resolved.error),
            )
        }
        val function = property as? JessieCodeRuntimeValue.FunctionValue
            ?: return invalid(
                operation = "parse",
                reason = "$functionName is not a method of ${element.id}",
            )
        return when (
            val result = function.callable.call(arguments, location)
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                PrefixParserError.Runtime(result.error),
            )
        }
    }

    private fun dimension(
        board: Board,
        term: JessieCodeRuntimeValue,
        state: PrefixParserState,
        depth: Int,
    ): GMResult<Double, PrefixParserError> {
        when (val budget = state.enter(depth)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return budget
        }
        if (term is JessieCodeRuntimeValue.NumberValue) {
            return GMResult.Ok(0.0)
        }
        val values = arrayTerm(term, "dimension") ?: return invalid(
            operation = "dimension",
            reason = "term is not an array",
        )
        val method = methodName(values, "dimension") ?: return invalid(
            operation = "dimension",
            reason = "operator is not a string",
        )

        if (method in ARITHMETIC_OPERATORS) {
            var result = when (
                val parsed = dimension(board, values[1], state, depth + 1)
            ) {
                is GMResult.Ok -> parsed.value
                is GMResult.Err -> return parsed
            }
            for (index in 2 until values.size) {
                val operand = when (
                    val parsed =
                        dimension(board, values[index], state, depth + 1)
                ) {
                    is GMResult.Ok -> parsed.value
                    is GMResult.Err -> return parsed
                }
                result = when (method) {
                    "+", "-" ->
                        if (operand == result) result else Double.NaN
                    "*" -> result + operand
                    "/" -> result - operand
                    else -> result
                }
            }
            return GMResult.Ok(result)
        }

        if (method == "exec") {
            val operand = values.getOrNull(2)
            val measurement = operand?.let { resolveElement(board, it) }
                as? Text
            val definition = measurement?.measurementDefinition
            return if (definition == null) {
                GMResult.Ok(0.0)
            } else {
                when (val result = definition.Dimension()) {
                    is GMResult.Ok -> GMResult.Ok(
                        (result.value as? JessieCodeRuntimeValue.NumberValue)
                            ?.value ?: Double.NaN,
                    )
                    is GMResult.Err -> GMResult.Err(
                        PrefixParserError.InvalidTerm(
                            operation = "dimension",
                            reason = result.error.toString(),
                        ),
                    )
                }
            }
        }

        return when (method) {
            "Slope", "Angle" -> GMResult.Ok(0.0)
            "L", "Length", "Perimeter", "Diameter", "Radius", "R",
            "DeltaX", "DeltaY",
            -> GMResult.Ok(1.0)
            "Area", "A" -> GMResult.Ok(2.0)
            else -> valueDimension(board, values, method)
        }
    }

    private fun valueDimension(
        board: Board,
        values: MutableList<JessieCodeRuntimeValue>,
        method: String,
    ): GMResult<Double, PrefixParserError> {
        val element = resolveElement(board, values[1])
            ?: return GMResult.Ok(0.0)
        val measurement = element as? Text
        val definition = measurement?.measurementDefinition
        if (definition != null) {
            return when (val result = definition.Dimension()) {
                is GMResult.Ok -> GMResult.Ok(
                    (result.value as? JessieCodeRuntimeValue.NumberValue)
                        ?.value ?: Double.NaN,
                )
                is GMResult.Err -> invalid(
                    operation = "dimension",
                    reason = result.error.toString(),
                )
            }
        }
        if (method != "Value" && method != "V") {
            return GMResult.Ok(0.0)
        }
        if (
            element.type != Const.OBJECT_TYPE_ARC &&
            element.type != Const.OBJECT_TYPE_SECTOR &&
            element.type != Const.OBJECT_TYPE_ANGLE
        ) {
            return GMResult.Ok(0.0)
        }
        val unit = (
            values.getOrNull(2) as? JessieCodeRuntimeValue.StringValue
            )?.value?.lowercase().orEmpty()
        return GMResult.Ok(
            when {
                unit.isEmpty() ->
                    if (element.type == Const.OBJECT_TYPE_ANGLE) 0.0 else 1.0
                unit.startsWith("len") -> 1.0
                else -> 0.0
            },
        )
    }

    private fun toPrefix(
        board: Board,
        term: JessieCodeRuntimeValue,
        state: PrefixParserState,
        depth: Int,
    ): GMResult<JessieCodeRuntimeValue, PrefixParserError> {
        when (val budget = state.enter(depth)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return budget
        }
        if (term is JessieCodeRuntimeValue.NumberValue) {
            return GMResult.Ok(term)
        }
        val values = arrayTerm(term, "toPrefix") ?: return invalid(
            operation = "toPrefix",
            reason = "term is not an array",
        )
        val method = methodName(values, "toPrefix") ?: return invalid(
            operation = "toPrefix",
            reason = "operator is not a string",
        )
        val result = mutableListOf<JessieCodeRuntimeValue>(
            JessieCodeRuntimeValue.StringValue(method),
        )
        if (method in ARITHMETIC_OPERATORS) {
            for (index in 1 until values.size) {
                when (
                    val converted =
                        toPrefix(board, values[index], state, depth + 1)
                ) {
                    is GMResult.Ok -> result += converted.value
                    is GMResult.Err -> return converted
                }
            }
            return GMResult.Ok(JessieCodeRuntimeValue.ArrayValue(result))
        }
        if (method == "exec") {
            result += values[1]
            for (index in 2 until values.size) {
                when (
                    val converted =
                        toPrefix(board, values[index], state, depth + 1)
                ) {
                    is GMResult.Ok -> result += converted.value
                    is GMResult.Err -> return converted
                }
            }
            return GMResult.Ok(JessieCodeRuntimeValue.ArrayValue(result))
        }
        for (index in 1 until values.size) {
            val operand = values[index]
            val receiver = resolveElement(board, operand)
            val nested = (receiver as? Text)?.measurementDefinition
            if (method == "V" && nested != null) {
                when (val converted = nested.toPrefix()) {
                    is GMResult.Ok -> {
                        val nestedValues = (
                            converted.value as?
                                JessieCodeRuntimeValue.ArrayValue
                            )?.values ?: return invalid(
                            operation = "toPrefix",
                            reason =
                                "nested measurement prefix is not an array",
                        )
                        result.clear()
                        result += nestedValues
                    }
                    is GMResult.Err -> return invalid(
                        operation = "toPrefix",
                        reason = converted.error.toString(),
                    )
                }
            } else {
                val id = when (operand) {
                    is JessieCodeRuntimeValue.ElementReference ->
                        operand.element.id
                    is JessieCodeRuntimeValue.StringValue ->
                        if (index == 1) {
                            board.select(operand.value)?.id
                        } else {
                            null
                        }
                    else -> null
                }
                result.clear()
                result += JessieCodeRuntimeValue.StringValue(method)
                result += id?.let(
                    JessieCodeRuntimeValue::StringValue,
                ) ?: JessieCodeRuntimeValue.UndefinedValue
            }
        }
        return GMResult.Ok(
            JessieCodeRuntimeValue.ArrayValue(result),
        )
    }

    private fun getParents(
        board: Board,
        term: JessieCodeRuntimeValue,
        state: PrefixParserState,
        depth: Int,
    ): GMResult<List<JessieCodeRuntimeValue>, PrefixParserError> {
        when (val budget = state.enter(depth)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return budget
        }
        if (term is JessieCodeRuntimeValue.NumberValue) {
            return GMResult.Ok(emptyList())
        }
        val values = arrayTerm(term, "getParents") ?: return invalid(
            operation = "getParents",
            reason = "term is not an array",
        )
        val method = methodName(values, "getParents") ?: return invalid(
            operation = "getParents",
            reason = "operator is not a string",
        )
        val result = mutableListOf<JessieCodeRuntimeValue>()
        if (method in ARITHMETIC_OPERATORS) {
            for (index in 1 until values.size) {
                when (
                    val parents =
                        getParents(board, values[index], state, depth + 1)
                ) {
                    is GMResult.Ok -> result += parents.value
                    is GMResult.Err -> return parents
                }
            }
            return GMResult.Ok(result)
        }
        if (method == "exec") {
            for (index in 2 until values.size) {
                when (
                    val parents =
                        getParents(board, values[index], state, depth + 1)
                ) {
                    is GMResult.Ok -> result += parents.value
                    is GMResult.Err -> return parents
                }
            }
            return GMResult.Ok(result)
        }
        for (index in 1 until values.size) {
            val operand = values[index]
            val element = resolveElement(board, operand)
            val nested = (element as? Text)?.measurementDefinition
            if (method == "V" && nested != null) {
                when (val parents = nested.getParents()) {
                    is GMResult.Ok -> result += parents.value
                    is GMResult.Err -> return invalid(
                        operation = "getParents",
                        reason = parents.error.toString(),
                    )
                }
            } else {
                result += operand
            }
        }
        return GMResult.Ok(result)
    }

    private fun arithmetic(
        operator: String,
        left: JessieCodeRuntimeValue,
        right: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue =
        if (
            operator == "+" &&
            (
                primitive(left) is JessieCodeRuntimeValue.StringValue ||
                    primitive(right) is JessieCodeRuntimeValue.StringValue
                )
        ) {
            JessieCodeRuntimeValue.StringValue(
                jsString(primitive(left)) + jsString(primitive(right)),
            )
        } else {
            val leftNumber = toNumber(primitive(left))
            val rightNumber = toNumber(primitive(right))
            JessieCodeRuntimeValue.NumberValue(
                when (operator) {
                    "+" -> leftNumber + rightNumber
                    "-" -> leftNumber - rightNumber
                    "*" -> leftNumber * rightNumber
                    "/" -> leftNumber / rightNumber
                    else -> Double.NaN
                },
            )
        }

    private fun invokeMath(
        name: String,
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, PrefixParserError> {
        val numbers = arguments.map(::toNumber)
        fun unary(function: (Double) -> Double) =
            JessieCodeRuntimeValue.NumberValue(
                function(numbers.getOrElse(0) { Double.NaN }),
            )
        fun binary(function: (Double, Double) -> Double) =
            JessieCodeRuntimeValue.NumberValue(
                function(
                    numbers.getOrElse(0) { Double.NaN },
                    numbers.getOrElse(1) { Double.NaN },
                ),
            )

        val value = when (name) {
            "abs" -> unary(::abs)
            "acos" -> unary(::acos)
            "acosh" -> unary(::acosh)
            "asin" -> unary(::asin)
            "asinh" -> unary(::asinh)
            "atan" -> unary(::atan)
            "atanh" -> unary(::atanh)
            "atan2" -> binary(::atan2)
            "cbrt" -> unary(Mat::cbrt)
            "ceil" -> unary(::ceil)
            "cos" -> unary(::cos)
            "cosh" -> unary(::cosh)
            "exp" -> unary(::exp)
            "expm1" -> unary(::expm1)
            "floor" -> unary(::floor)
            "fround" -> unary { it.toFloat().toDouble() }
            "hypot" -> JessieCodeRuntimeValue.NumberValue(
                Mat.hypot(*numbers.toDoubleArray()),
            )
            "imul" -> binary { first, second ->
                (first.toInt() * second.toInt()).toDouble()
            }
            "log" -> unary(::ln)
            "log1p" -> unary(::ln1p)
            "log2" -> unary(Mat::log2)
            "log10" -> unary(Mat::log10)
            "max" -> JessieCodeRuntimeValue.NumberValue(
                numbers.reduceOrNull(::max) ?: Double.NEGATIVE_INFINITY,
            )
            "min" -> JessieCodeRuntimeValue.NumberValue(
                numbers.reduceOrNull(::min) ?: Double.POSITIVE_INFINITY,
            )
            "pow" -> binary(Double::pow)
            "random" -> JessieCodeRuntimeValue.NumberValue(
                Random.Default.nextDouble(),
            )
            "round" -> unary(JsMath::round)
            "sign" -> unary(Mat::sign)
            "sin" -> unary(::sin)
            "sinh" -> unary(::sinh)
            "sqrt" -> unary(::sqrt)
            "tan" -> unary(::tan)
            "tanh" -> unary(::tanh)
            "trunc" -> unary { if (it < 0.0) ceil(it) else floor(it) }
            "binomial" -> binary(Mat::binomial)
            "factorial" -> unary(Mat::factorial)
            "mod" -> binary(Mat::mod)
            "relDif" -> binary(Mat::relDif)
            "nthroot" -> binary(Mat::nthroot)
            "cot" -> unary(Mat::cot)
            "acot" -> unary(Mat::acot)
            "erf" -> unary(Mat::erf)
            "erfc" -> unary(Mat::erfc)
            "erfi" -> unary(Mat::erfi)
            "ndtr" -> unary(Mat::ndtr)
            "ndtri" -> unary(Mat::ndtri)
            "hstep" -> unary(Mat::hstep)
            "gamma" -> unary(Mat::gamma)
            "gcd" -> binary(Mat::gcd)
            "lcm" -> binary(Mat::lcm)
            "roundToStep" -> binary(Mat::roundToStep)
            else -> return GMResult.Err(
                PrefixParserError.FunctionNotAllowed(name),
            )
        }
        return GMResult.Ok(value)
    }

    private fun arrayTerm(
        term: JessieCodeRuntimeValue,
        operation: String,
    ): MutableList<JessieCodeRuntimeValue>? =
        (term as? JessieCodeRuntimeValue.ArrayValue)
            ?.values
            ?.takeIf { it.size >= 2 }

    private fun methodName(
        values: List<JessieCodeRuntimeValue>,
        operation: String,
    ): String? =
        (values.firstOrNull() as? JessieCodeRuntimeValue.StringValue)?.value

    private fun resolveElement(
        board: Board,
        value: JessieCodeRuntimeValue,
    ): GeometryElement? =
        when (value) {
            is JessieCodeRuntimeValue.ElementReference -> value.element
            is JessieCodeRuntimeValue.StringValue -> board.select(value.value)
            else -> null
        }

    private fun primitive(
        value: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue =
        when (value) {
            is JessieCodeRuntimeValue.ArrayValue ->
                JessieCodeRuntimeValue.StringValue(
                    value.values.joinToString(",") { item ->
                        if (
                            item === JessieCodeRuntimeValue.NullValue ||
                            item === JessieCodeRuntimeValue.UndefinedValue
                        ) {
                            ""
                        } else {
                            jsString(item)
                        }
                    },
                )
            is JessieCodeRuntimeValue.ObjectValue,
            is JessieCodeRuntimeValue.BoardReference,
            is JessieCodeRuntimeValue.TransformationReference,
            is JessieCodeRuntimeValue.CompositionReference,
            is JessieCodeRuntimeValue.ElementReference,
            -> JessieCodeRuntimeValue.StringValue("[object Object]")
            is JessieCodeRuntimeValue.FunctionValue ->
                JessieCodeRuntimeValue.StringValue(
                    "function ${value.name}() { }",
                )
            else -> value
        }

    private fun toNumber(value: JessieCodeRuntimeValue): Double =
        when (value) {
            JessieCodeRuntimeValue.UndefinedValue -> Double.NaN
            JessieCodeRuntimeValue.NullValue -> 0.0
            is JessieCodeRuntimeValue.NumberValue -> value.value
            is JessieCodeRuntimeValue.BooleanValue ->
                if (value.value) 1.0 else 0.0
            is JessieCodeRuntimeValue.StringValue -> {
                val trimmed = value.value.trim()
                when {
                    trimmed.isEmpty() -> 0.0
                    trimmed == "Infinity" || trimmed == "+Infinity" ->
                        Double.POSITIVE_INFINITY
                    trimmed == "-Infinity" -> Double.NEGATIVE_INFINITY
                    else -> trimmed.toDoubleOrNull() ?: Double.NaN
                }
            }
            else -> toNumber(primitive(value))
        }

    internal fun jsString(value: JessieCodeRuntimeValue): String =
        when (value) {
            JessieCodeRuntimeValue.UndefinedValue -> "undefined"
            JessieCodeRuntimeValue.NullValue -> "null"
            is JessieCodeRuntimeValue.NumberValue ->
                JsNumberFormat.compact(value.value)
            is JessieCodeRuntimeValue.BooleanValue -> value.value.toString()
            is JessieCodeRuntimeValue.StringValue -> value.value
            is JessieCodeRuntimeValue.ArrayValue ->
                value.values.joinToString(",") { item ->
                    if (
                        item === JessieCodeRuntimeValue.NullValue ||
                        item === JessieCodeRuntimeValue.UndefinedValue
                    ) {
                        ""
                    } else {
                        jsString(item)
                    }
                }
            is JessieCodeRuntimeValue.ObjectValue,
            is JessieCodeRuntimeValue.BoardReference,
            is JessieCodeRuntimeValue.TransformationReference,
            is JessieCodeRuntimeValue.CompositionReference,
            is JessieCodeRuntimeValue.ElementReference,
            -> "[object Object]"
            is JessieCodeRuntimeValue.FunctionValue ->
                "function ${value.name}() { }"
        }

    private fun <T> invalid(
        operation: String,
        reason: String,
    ): GMResult<T, PrefixParserError> =
        GMResult.Err(
            PrefixParserError.InvalidTerm(
                operation = operation,
                reason = reason,
            ),
        )

    private class PrefixParserState(
        private val limits: PrefixParserLimits,
    ) {
        private var nodes: Int = 0

        fun enter(
            depth: Int,
        ): GMResult<Unit, PrefixParserError> {
            if (depth >= limits.maxDepth) {
                return GMResult.Err(
                    PrefixParserError.DepthLimitExceeded(limits.maxDepth),
                )
            }
            nodes += 1
            return if (nodes > limits.maxNodes) {
                GMResult.Err(
                    PrefixParserError.NodeLimitExceeded(limits.maxNodes),
                )
            } else {
                GMResult.Ok(Unit)
            }
        }
    }

    private val ARITHMETIC_OPERATORS = setOf("+", "-", "*", "/")
}
