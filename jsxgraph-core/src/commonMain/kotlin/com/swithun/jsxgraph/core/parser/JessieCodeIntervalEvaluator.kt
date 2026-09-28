/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js mathLib dispatch and src/math/ia.js.
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.math.IntervalArithmetic
import com.swithun.jsxgraph.core.math.PlotInterval
import kotlin.math.E
import kotlin.math.PI

internal sealed interface JessieCodeIntervalEvaluation {
    data class Value(
        val interval: PlotInterval,
    ) : JessieCodeIntervalEvaluation

    data class Unavailable(
        val operation: String,
    ) : JessieCodeIntervalEvaluation
}

/**
 * Evaluates the arithmetic subset that JSXGraph routes through
 * `board.mathLib` while Plot v4 temporarily installs IntervalArithmetic.
 * Expressions outside that subset return Unavailable so Plot v4 follows its
 * upstream scalar `fminbr` fallback.
 */
internal class JessieCodeIntervalEvaluator(
    private val variables: Map<String, JessieCodeRuntimeValue>,
    private val intervalVariables: Map<String, PlotInterval>,
) {
    internal fun evaluate(
        node: JessieCodeAstNode,
    ): JessieCodeIntervalEvaluation =
        when (node.type) {
            JessieCodeAstNodeType.CONSTANT -> evaluateConstant(node)
            JessieCodeAstNodeType.VARIABLE -> evaluateVariable(node)
            JessieCodeAstNodeType.OPERATION -> evaluateOperation(node)
            JessieCodeAstNodeType.BOOLEAN_CONSTANT,
            JessieCodeAstNodeType.STRING,
            -> unavailable(node)
        }

    private fun evaluateConstant(
        node: JessieCodeAstNode,
    ): JessieCodeIntervalEvaluation =
        when (val value = node.value) {
            is JessieCodeAstValue.Number ->
                value(IntervalArithmetic.singleton(value.value))
            else -> unavailable(node)
        }

    private fun evaluateVariable(
        node: JessieCodeAstNode,
    ): JessieCodeIntervalEvaluation {
        val name = (node.value as? JessieCodeAstValue.Text)?.value
            ?: return unavailable(node)
        intervalVariables[name]?.let { return value(it) }
        return when (name) {
            "PI" -> value(IntervalArithmetic.singleton(PI))
            "EULER" -> value(IntervalArithmetic.singleton(E))
            else -> {
                val runtimeValue = variables[name]
                if (runtimeValue is JessieCodeRuntimeValue.NumberValue) {
                    value(
                        IntervalArithmetic.singleton(runtimeValue.value),
                    )
                } else {
                    unavailable(name)
                }
            }
        }
    }

    private fun evaluateOperation(
        node: JessieCodeAstNode,
    ): JessieCodeIntervalEvaluation {
        val operation = (node.value as? JessieCodeAstValue.Text)?.value
            ?: return unavailable(node)
        return when (operation) {
            "op_none" -> evaluateSequence(node)
            "op_block",
            "op_return",
            -> evaluateNodeChild(node, 0)
            "op_neg" -> unary(node, IntervalArithmetic::negative)
            "op_add" -> binary(node, IntervalArithmetic::add)
            "op_sub" -> binary(node, IntervalArithmetic::subtract)
            "op_mul" -> binary(node, IntervalArithmetic::multiply)
            "op_div" -> binary(node, IntervalArithmetic::divide)
            "op_mod" -> binary(node, IntervalArithmetic::modulo)
            "op_exp" -> binary(node, IntervalArithmetic::power)
            "op_execfun" -> evaluateCall(node)
            else -> unavailable(operation)
        }
    }

    private fun evaluateSequence(
        node: JessieCodeAstNode,
    ): JessieCodeIntervalEvaluation {
        var result: JessieCodeIntervalEvaluation =
            value(IntervalArithmetic.singleton(0.0))
        for (child in node.children) {
            val childNode = (child as? JessieCodeAstChild.Node)?.value
                ?: return unavailable("op_none")
            result = evaluate(childNode)
            if (result is JessieCodeIntervalEvaluation.Unavailable) {
                return result
            }
        }
        return result
    }

    private fun evaluateCall(
        node: JessieCodeAstNode,
    ): JessieCodeIntervalEvaluation {
        val functionNode = nodeChild(node, 0)
            ?: return unavailable("op_execfun")
        if (functionNode.type != JessieCodeAstNodeType.VARIABLE) {
            return unavailable("op_execfun")
        }
        val name =
            (functionNode.value as? JessieCodeAstValue.Text)?.value
                ?: return unavailable("op_execfun")
        val argumentNodes =
            (node.children.getOrNull(1) as? JessieCodeAstChild.NodeList)
                ?.value
                ?: return unavailable(name)
        val arguments = mutableListOf<PlotInterval>()
        for (argumentNode in argumentNodes) {
            when (val result = evaluate(argumentNode)) {
                is JessieCodeIntervalEvaluation.Value ->
                    arguments += result.interval
                is JessieCodeIntervalEvaluation.Unavailable -> return result
            }
        }
        return when (name) {
            "sin" -> unaryCall(name, arguments, IntervalArithmetic::sin)
            "cos" -> unaryCall(name, arguments, IntervalArithmetic::cos)
            "tan" -> unaryCall(name, arguments, IntervalArithmetic::tan)
            "asin" -> unaryCall(name, arguments, IntervalArithmetic::asin)
            "acos" -> unaryCall(name, arguments, IntervalArithmetic::acos)
            "atan" -> unaryCall(name, arguments, IntervalArithmetic::atan)
            "sqrt" -> unaryCall(name, arguments, IntervalArithmetic::sqrt)
            "exp" -> unaryCall(name, arguments, IntervalArithmetic::exp)
            "abs" -> unaryCall(name, arguments, IntervalArithmetic::abs)
            "floor" -> unaryCall(name, arguments, IntervalArithmetic::floor)
            "ceil" -> unaryCall(name, arguments, IntervalArithmetic::ceil)
            "ln" -> unaryCall(name, arguments, IntervalArithmetic::log)
            "log" ->
                if (arguments.size == 1) {
                    value(IntervalArithmetic.log(arguments[0]))
                } else {
                    unavailable(name)
                }
            "pow" -> binaryCall(name, arguments, IntervalArithmetic::power)
            "min" ->
                if (arguments.isEmpty()) {
                    unavailable(name)
                } else {
                    value(IntervalArithmetic.minimum(arguments))
                }
            "max" ->
                if (arguments.isEmpty()) {
                    unavailable(name)
                } else {
                    value(IntervalArithmetic.maximum(arguments))
                }
            else -> unavailable(name)
        }
    }

    private fun unary(
        node: JessieCodeAstNode,
        operation: (PlotInterval) -> PlotInterval,
    ): JessieCodeIntervalEvaluation {
        val operand = when (val result = evaluateNodeChild(node, 0)) {
            is JessieCodeIntervalEvaluation.Value -> result.interval
            is JessieCodeIntervalEvaluation.Unavailable -> return result
        }
        return value(operation(operand))
    }

    private fun binary(
        node: JessieCodeAstNode,
        operation: (PlotInterval, PlotInterval) -> PlotInterval,
    ): JessieCodeIntervalEvaluation {
        val first = when (val result = evaluateNodeChild(node, 0)) {
            is JessieCodeIntervalEvaluation.Value -> result.interval
            is JessieCodeIntervalEvaluation.Unavailable -> return result
        }
        val second = when (val result = evaluateNodeChild(node, 1)) {
            is JessieCodeIntervalEvaluation.Value -> result.interval
            is JessieCodeIntervalEvaluation.Unavailable -> return result
        }
        return value(operation(first, second))
    }

    private fun unaryCall(
        name: String,
        arguments: List<PlotInterval>,
        operation: (PlotInterval) -> PlotInterval,
    ): JessieCodeIntervalEvaluation =
        if (arguments.size == 1) {
            value(operation(arguments[0]))
        } else {
            unavailable(name)
        }

    private fun binaryCall(
        name: String,
        arguments: List<PlotInterval>,
        operation: (PlotInterval, PlotInterval) -> PlotInterval,
    ): JessieCodeIntervalEvaluation =
        if (arguments.size == 2) {
            value(operation(arguments[0], arguments[1]))
        } else {
            unavailable(name)
        }

    private fun evaluateNodeChild(
        node: JessieCodeAstNode,
        index: Int,
    ): JessieCodeIntervalEvaluation =
        nodeChild(node, index)?.let(::evaluate)
            ?: unavailable(
                (node.value as? JessieCodeAstValue.Text)?.value
                    ?: node.type.upstreamName,
            )

    private fun nodeChild(
        node: JessieCodeAstNode,
        index: Int,
    ): JessieCodeAstNode? =
        (node.children.getOrNull(index) as? JessieCodeAstChild.Node)?.value

    private fun value(
        interval: PlotInterval,
    ): JessieCodeIntervalEvaluation =
        JessieCodeIntervalEvaluation.Value(interval)

    private fun unavailable(
        node: JessieCodeAstNode,
    ): JessieCodeIntervalEvaluation =
        unavailable(
            (node.value as? JessieCodeAstValue.Text)?.value
                ?: node.type.upstreamName,
        )

    private fun unavailable(
        operation: String,
    ): JessieCodeIntervalEvaluation =
        JessieCodeIntervalEvaluation.Unavailable(operation)
}
