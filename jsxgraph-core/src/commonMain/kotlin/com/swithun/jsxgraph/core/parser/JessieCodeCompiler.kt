/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> compile, getLHSCompiler,
 * functionCodeJS
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.utils.JsNumberFormat

internal data class JessieCodeFormatOptions(
    val minParentheses: Boolean = false,
    val constToFixed: Int? = null,
    val printable: Boolean = false,
)

internal sealed interface JessieCodeCompileError {
    data class InvalidAst(
        val reason: String,
        val location: JessieCodeAstLocation,
    ) : JessieCodeCompileError

    data class InvalidAssignmentTarget(
        val location: JessieCodeAstLocation,
    ) : JessieCodeCompileError
}

internal sealed interface JessieCodeCompiledLhs {
    data class Variable(
        val name: String,
    ) : JessieCodeCompiledLhs

    data class Property(
        val receiver: JessieCodeAstNode,
        val property: String,
    ) : JessieCodeCompiledLhs

    data class Index(
        val receiver: JessieCodeAstNode,
        val index: JessieCodeAstNode,
    ) : JessieCodeCompiledLhs
}

/**
 * Native JessieCode source compiler. The JavaScript branch of upstream
 * `compile` is deliberately represented as AST/runtime values elsewhere;
 * production code never emits or evaluates JavaScript.
 */
internal object JessieCodeCompiler {
    // JSXGraph 1.13.3: src/parser/jessiecode.js -> compile.
    internal fun compile(
        ast: JessieCodeAstNode,
        format: JessieCodeFormatOptions = JessieCodeFormatOptions(),
    ): GMResult<String, JessieCodeCompileError> =
        Compiler(format).compile(ast)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> getLHSCompiler.
    internal fun getLHSCompiler(
        node: JessieCodeAstNode,
    ): GMResult<JessieCodeCompiledLhs, JessieCodeCompileError> =
        when {
            node.type == JessieCodeAstNodeType.VARIABLE -> {
                val name = textValue(node)
                    ?: return invalid(
                        node,
                        "Assignment variable must contain text.",
                    )
                GMResult.Ok(JessieCodeCompiledLhs.Variable(name))
            }
            operationName(node) == "op_property" -> {
                val receiver = nodeChild(node, 0)
                    ?: return invalid(node, "Missing property receiver.")
                val property = textChild(node, 1)
                    ?: return invalid(node, "Missing property name.")
                GMResult.Ok(
                    JessieCodeCompiledLhs.Property(receiver, property),
                )
            }
            operationName(node) == "op_extvalue" -> {
                val receiver = nodeChild(node, 0)
                    ?: return invalid(node, "Missing index receiver.")
                val index = nodeChild(node, 1)
                    ?: return invalid(node, "Missing index expression.")
                GMResult.Ok(
                    JessieCodeCompiledLhs.Index(receiver, index),
                )
            }
            else -> GMResult.Err(
                JessieCodeCompileError.InvalidAssignmentTarget(
                    location = node.location,
                ),
            )
        }

    private fun invalid(
        node: JessieCodeAstNode,
        reason: String,
    ): GMResult.Err<JessieCodeCompileError> =
        GMResult.Err(
            JessieCodeCompileError.InvalidAst(reason, node.location),
        )
}

private class Compiler(
    private val format: JessieCodeFormatOptions,
) {
    fun compile(
        node: JessieCodeAstNode,
    ): GMResult<String, JessieCodeCompileError> =
        compileNode(node, previousOperator = null, position = -1)

    private fun compileNode(
        node: JessieCodeAstNode,
        previousOperator: String?,
        position: Int,
    ): GMResult<String, JessieCodeCompileError> {
        val compiled = when (node.type) {
            JessieCodeAstNodeType.OPERATION ->
                compileOperation(node)
            JessieCodeAstNodeType.VARIABLE ->
                textValue(node)?.let { GMResult.Ok(it) }
                    ?: invalid(node, "Variable value must be text.")
            JessieCodeAstNodeType.CONSTANT ->
                compileConstant(node, previousOperator, position)
            JessieCodeAstNodeType.BOOLEAN_CONSTANT ->
                (node.value as? JessieCodeAstValue.Boolean)?.value?.let {
                    GMResult.Ok(it.toString())
                } ?: invalid(node, "Boolean value must be boolean.")
            JessieCodeAstNodeType.STRING ->
                textValue(node)?.let {
                    GMResult.Ok("'${escapeString(it)}'")
                } ?: invalid(node, "String value must be text.")
        }
        val source = when (compiled) {
            is GMResult.Ok -> compiled.value
            is GMResult.Err -> return compiled
        }
        return GMResult.Ok(
            if (
                format.printable &&
                previousOperator == "op_exp" &&
                position == 1
            ) {
                "{$source}"
            } else if (format.printable) {
                source.replace("\n", "")
            } else {
                source
            },
        )
    }

    private fun compileOperation(
        node: JessieCodeAstNode,
    ): GMResult<String, JessieCodeCompileError> {
        val operator = textValue(node)
            ?: return invalid(node, "Operation value must be text.")
        return when (operator) {
            "op_none" -> compileSequence(node)
            "op_block" -> unaryNode(node) { body ->
                GMResult.Ok("{\n$body }\n")
            }
            "op_assign" -> binaryNodes(node) { left, right ->
                GMResult.Ok("$left = $right;\n")
            }
            "op_if" -> binaryNodes(node) { condition, body ->
                GMResult.Ok(" if ($condition) $body")
            }
            "op_if_else" -> ternaryNodes(node) {
                    condition,
                    thenBody,
                    elseBody,
                ->
                GMResult.Ok(
                    " if ($condition)$thenBody else $elseBody",
                )
            }
            "op_conditional" -> ternaryNodes(node) {
                    condition,
                    thenValue,
                    elseValue,
                ->
                GMResult.Ok(
                    "(($condition)?($thenValue):($elseValue))",
                )
            }
            "op_while" -> binaryNodes(node) { condition, body ->
                GMResult.Ok(" while ($condition) {\n$body}\n")
            }
            "op_do" -> binaryNodes(node) { body, condition ->
                GMResult.Ok(" do {\n$body} while ($condition);\n")
            }
            "op_for" -> compileFor(node)
            "op_proplst" -> binaryNodes(node) { left, right ->
                GMResult.Ok("$left, $right")
            }
            "op_prop" -> compilePropertyEntry(node)
            "op_emptyobject" -> GMResult.Ok("<< >>")
            "op_proplst_val" -> unaryNode(node) {
                GMResult.Ok(it)
            }
            "op_array" -> compileArray(node)
            "op_extvalue" -> binaryNodes(node) { receiver, index ->
                GMResult.Ok("$receiver[$index]")
            }
            "op_return" -> compileReturn(node)
            "op_map" -> compileFunction(node, isMap = true)
            "op_function" -> compileFunction(node, isMap = false)
            "op_execfunmath" -> GMResult.Ok("-1")
            "op_execfun" -> compileCall(node)
            "op_property" -> compileProperty(node)
            "op_use" -> textChild(node, 0)?.let {
                GMResult.Ok("use('$it');")
            } ?: invalid(node, "Missing board name.")
            "op_delete" -> textChild(node, 0)?.let {
                GMResult.Ok("remove($it)")
            } ?: invalid(node, "Missing delete target.")
            "op_eq" -> binaryOperator(node, " === ")
            "op_neq" -> binaryOperator(node, " !== ")
            "op_approx" -> binaryOperator(node, " ~= ")
            "op_gt" -> binaryOperator(node, " > ")
            "op_lt" -> binaryOperator(node, " < ")
            "op_geq" -> binaryOperator(node, " >= ")
            "op_leq" -> binaryOperator(node, " <= ")
            "op_or" -> binaryOperator(node, " || ")
            "op_and" -> binaryOperator(node, " && ")
            "op_not" -> unaryNode(node) { GMResult.Ok("!($it)") }
            "op_add" -> arithmetic(node, " + ")
            "op_sub" -> arithmetic(node, " - ")
            "op_div" -> arithmetic(node, " / ")
            "op_mod" -> arithmetic(node, " % ")
            "op_mul" -> arithmetic(node, " * ")
            "op_exp" -> arithmetic(node, "^")
            "op_neg" -> compileNegation(node)
            else -> invalid(node, "Unsupported operator $operator.")
        }
    }

    private fun compileSequence(
        node: JessieCodeAstNode,
    ): GMResult<String, JessieCodeCompileError> {
        if (node.children.isEmpty()) {
            return GMResult.Ok("")
        }
        val result = StringBuilder()
        for (index in node.children.indices) {
            when (
                val child = compileNodeChild(
                    node,
                    index,
                    "op_none",
                )
            ) {
                is GMResult.Ok -> result.append(child.value)
                is GMResult.Err -> return child
            }
        }
        return GMResult.Ok(result.toString())
    }

    private fun compileFor(
        node: JessieCodeAstNode,
    ): GMResult<String, JessieCodeCompileError> {
        val parts = mutableListOf<String>()
        for (index in 0 until 4) {
            when (
                val child = compileNodeChild(node, index, "op_for")
            ) {
                is GMResult.Ok -> parts += child.value
                is GMResult.Err -> return child
            }
        }
        val counting = parts[2].removeSuffix(";\n")
        return GMResult.Ok(
            " for (${parts[0]}${parts[1]}; " +
                "$counting) {\n" +
                "${parts[3]}\n}\n",
        )
    }

    private fun compilePropertyEntry(
        node: JessieCodeAstNode,
    ): GMResult<String, JessieCodeCompileError> {
        val name = textChild(node, 0)
            ?: return invalid(node, "Missing property name.")
        val value = when (
            val result = compileNodeChild(node, 1, "op_prop")
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok("$name: $value")
    }

    private fun compileArray(
        node: JessieCodeAstNode,
    ): GMResult<String, JessieCodeCompileError> {
        val children = nodeListChild(node, 0)
            ?: return invalid(node, "Missing array values.")
        val values = mutableListOf<String>()
        for (child in children) {
            when (
                val result = compileNode(
                    child,
                    previousOperator = "op_array",
                    position = -1,
                )
            ) {
                is GMResult.Ok -> values += result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok("[${values.joinToString(", ")}]")
    }

    private fun compileReturn(
        node: JessieCodeAstNode,
    ): GMResult<String, JessieCodeCompileError> {
        if (node.children.firstOrNull() == JessieCodeAstChild.Undefined) {
            return GMResult.Ok(" return;\n")
        }
        return unaryNode(node) { GMResult.Ok(" return $it;\n") }
    }

    private fun compileFunction(
        node: JessieCodeAstNode,
        isMap: Boolean,
    ): GMResult<String, JessieCodeCompileError> {
        val parameters = textListChild(node, 0)
            ?: return invalid(node, "Missing function parameters.")
        val body = when (
            val result = compileNodeChild(
                node,
                index = 1,
                previousOperator =
                    if (isMap) "op_map" else "op_function",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            if (isMap) {
                "map (${parameters.joinToString(", ")}) -> $body"
            } else {
                " function (${parameters.joinToString(", ")}) $body"
            },
        )
    }

    private fun compileCall(
        node: JessieCodeAstNode,
    ): GMResult<String, JessieCodeCompileError> {
        val function = when (
            val result = compileNodeChild(node, 0, "op_execfun")
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val arguments = nodeListChild(node, 1)
            ?: return invalid(node, "Missing call arguments.")
        val compiledArguments = mutableListOf<String>()
        for (argument in arguments) {
            when (
                val result = compileNode(
                    argument,
                    previousOperator = "op_execfun",
                    position = compiledArguments.size,
                )
            ) {
                is GMResult.Ok -> compiledArguments += result.value
                is GMResult.Err -> return result
            }
        }
        val attributes = (
            node.children.getOrNull(2) as?
                JessieCodeAstChild.NodeList
            )?.value.orEmpty()
        val compiledAttributes = mutableListOf<String>()
        for (attribute in attributes) {
            when (
                val result = compileNode(
                    attribute,
                    previousOperator = "op_execfun",
                    position = -1,
                )
            ) {
                is GMResult.Ok -> compiledAttributes += result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(
            "$function(${compiledArguments.joinToString(", ")})" +
                if (compiledAttributes.isEmpty()) {
                    ""
                } else {
                    " ${compiledAttributes.joinToString(", ")}"
                },
        )
    }

    private fun compileProperty(
        node: JessieCodeAstNode,
    ): GMResult<String, JessieCodeCompileError> {
        val receiver = when (
            val result = compileNodeChild(node, 0, "op_property")
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val property = textChild(node, 1)
            ?: return invalid(node, "Missing property name.")
        return GMResult.Ok("$receiver.$property")
    }

    private fun binaryOperator(
        node: JessieCodeAstNode,
        symbol: String,
    ): GMResult<String, JessieCodeCompileError> =
        binaryNodes(node) { left, right ->
            GMResult.Ok("($left$symbol$right)")
        }

    private fun arithmetic(
        node: JessieCodeAstNode,
        symbol: String,
    ): GMResult<String, JessieCodeCompileError> {
        val operator = textValue(node)
            ?: return invalid(node, "Operation value must be text.")
        val leftNode = nodeChild(node, 0)
            ?: return invalid(node, "Missing left operand.")
        val rightNode = nodeChild(node, 1)
            ?: return invalid(node, "Missing right operand.")
        val left = when (
            val result = compileNode(leftNode, operator, 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val right = when (
            val result = compileNode(rightNode, operator, 1)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (!format.minParentheses) {
            return GMResult.Ok(
                if (operator == "op_neg") "-($left)" else "($left$symbol$right)",
            )
        }
        val parentPriority = priority(node)
        val leftNeedsParentheses = when (operator) {
            "op_exp" -> parentPriority >= priority(leftNode)
            else -> parentPriority > priority(leftNode)
        }
        val rightNeedsParentheses = when (operator) {
            "op_sub", "op_div", "op_mod" ->
                parentPriority >= priority(rightNode)
            else -> parentPriority > priority(rightNode)
        }
        return GMResult.Ok(
            parenthesize(left, leftNeedsParentheses) +
                symbol +
                parenthesize(right, rightNeedsParentheses),
        )
    }

    private fun compileNegation(
        node: JessieCodeAstNode,
    ): GMResult<String, JessieCodeCompileError> {
        val child = nodeChild(node, 0)
            ?: return invalid(node, "Missing negated operand.")
        val source = when (
            val result = compileNode(child, "op_neg", 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            if (!format.minParentheses) {
                "(-$source)"
            } else if (priority(node) >= priority(child)) {
                "-($source)"
            } else {
                "-$source"
            },
        )
    }

    private fun compileConstant(
        node: JessieCodeAstNode,
        previousOperator: String?,
        position: Int,
    ): GMResult<String, JessieCodeCompileError> {
        val value = when (val astValue = node.value) {
            is JessieCodeAstValue.Number -> astValue.value
            JessieCodeAstValue.Null -> return GMResult.Ok("null")
            else -> return invalid(node, "Constant value must be numeric or null.")
        }
        val source = format.constToFixed?.let {
            JsNumberFormat.fixed(value, it)
        } ?: JsNumberFormat.compact(value)
        return GMResult.Ok(
            if (
                format.minParentheses &&
                value < 0.0 &&
                previousOperator != "op_execfun" &&
                position != 0
            ) {
                "($source)"
            } else {
                source
            },
        )
    }

    private fun unaryNode(
        node: JessieCodeAstNode,
        transform: (String) ->
            GMResult<String, JessieCodeCompileError>,
    ): GMResult<String, JessieCodeCompileError> =
        when (
            val result = compileNodeChild(
                node,
                index = 0,
                previousOperator = textValue(node),
            )
        ) {
            is GMResult.Ok -> transform(result.value)
            is GMResult.Err -> result
        }

    private fun binaryNodes(
        node: JessieCodeAstNode,
        transform: (
            String,
            String,
        ) -> GMResult<String, JessieCodeCompileError>,
    ): GMResult<String, JessieCodeCompileError> {
        val operator = textValue(node)
        val left = when (
            val result = compileNodeChild(node, 0, operator)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val right = when (
            val result = compileNodeChild(node, 1, operator)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return transform(left, right)
    }

    private fun ternaryNodes(
        node: JessieCodeAstNode,
        transform: (
            String,
            String,
            String,
        ) -> GMResult<String, JessieCodeCompileError>,
    ): GMResult<String, JessieCodeCompileError> {
        val operator = textValue(node)
        val values = mutableListOf<String>()
        for (index in 0 until 3) {
            when (
                val result = compileNodeChild(node, index, operator)
            ) {
                is GMResult.Ok -> values += result.value
                is GMResult.Err -> return result
            }
        }
        return transform(values[0], values[1], values[2])
    }

    private fun compileNodeChild(
        node: JessieCodeAstNode,
        index: Int,
        previousOperator: String?,
    ): GMResult<String, JessieCodeCompileError> {
        val child = nodeChild(node, index)
            ?: return invalid(node, "Missing node child at index $index.")
        return compileNode(child, previousOperator, index)
    }

    private fun invalid(
        node: JessieCodeAstNode,
        reason: String,
    ): GMResult.Err<JessieCodeCompileError> =
        GMResult.Err(
            JessieCodeCompileError.InvalidAst(reason, node.location),
        )

    private fun priority(node: JessieCodeAstNode): Int =
        when (node.type) {
            JessieCodeAstNodeType.CONSTANT,
            JessieCodeAstNodeType.BOOLEAN_CONSTANT,
            JessieCodeAstNodeType.STRING,
            JessieCodeAstNodeType.VARIABLE,
            -> 10
            JessieCodeAstNodeType.OPERATION -> when (textValue(node)) {
                "op_none" -> 0
                "op_assign" -> 1
                "op_map", "op_function", "op_return" -> 2
                "op_add", "op_sub" -> 3
                "op_neg" -> 4
                "op_mul", "op_div", "op_mod" -> 5
                "op_exp" -> 6
                "op_array", "op_execfun" -> 7
                else -> 0
            }
        }

    private fun parenthesize(
        source: String,
        needed: Boolean,
    ): String = if (needed) "($source)" else source

    private fun escapeString(value: String): String =
        value.replace("\\", "\\\\").replace("'", "\\'")
}

private fun operationName(node: JessieCodeAstNode): String? =
    if (node.type == JessieCodeAstNodeType.OPERATION) {
        textValue(node)
    } else {
        null
    }

private fun textValue(node: JessieCodeAstNode): String? =
    (node.value as? JessieCodeAstValue.Text)?.value

private fun nodeChild(
    node: JessieCodeAstNode,
    index: Int,
): JessieCodeAstNode? =
    (node.children.getOrNull(index) as? JessieCodeAstChild.Node)?.value

private fun nodeListChild(
    node: JessieCodeAstNode,
    index: Int,
): List<JessieCodeAstNode>? =
    (node.children.getOrNull(index) as? JessieCodeAstChild.NodeList)?.value

private fun textChild(
    node: JessieCodeAstNode,
    index: Int,
): String? =
    (node.children.getOrNull(index) as? JessieCodeAstChild.Text)?.value

private fun textListChild(
    node: JessieCodeAstNode,
    index: Int,
): List<String>? =
    (node.children.getOrNull(index) as? JessieCodeAstChild.TextList)?.value
