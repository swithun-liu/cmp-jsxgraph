/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> replaceNames and
 * createReplacementNode
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board

internal data class JessieCodeNameReplacementLimits(
    val maxVisitedNodes: Int = 100_000,
    val maxTraversalDepth: Int = 64,
)

internal sealed interface JessieCodeNameReplacementError {
    data class InvalidLimits(
        val maxVisitedNodes: Int,
        val maxTraversalDepth: Int,
    ) : JessieCodeNameReplacementError

    data class NodeLimitExceeded(
        val limit: Int,
        val location: JessieCodeAstLocation,
    ) : JessieCodeNameReplacementError

    data class DepthLimitExceeded(
        val limit: Int,
        val location: JessieCodeAstLocation,
    ) : JessieCodeNameReplacementError
}

internal class JessieCodeNameReplacer(
    private val board: Board,
    private val limits: JessieCodeNameReplacementLimits =
        JessieCodeNameReplacementLimits(),
) {
    /**
     * Replaces named board elements with stable `$("<id>")` calls.
     *
     * `forceValueCall` mirrors snippet's slider handling. The current parser
     * has no return node yet, so a whole-expression variable is treated as the
     * child of the return node used by upstream snippet().
     */
    internal fun replace(
        node: JessieCodeAstNode,
        boundNames: Set<String> = emptySet(),
        forceValueCall: Boolean = true,
    ): GMResult<
        JessieCodeAstNode,
        JessieCodeNameReplacementError,
        > {
        if (
            limits.maxVisitedNodes < 1 ||
            limits.maxTraversalDepth !in 1..MAX_REPLACEMENT_DEPTH
        ) {
            return GMResult.Err(
                JessieCodeNameReplacementError.InvalidLimits(
                    maxVisitedNodes = limits.maxVisitedNodes,
                    maxTraversalDepth = limits.maxTraversalDepth,
                ),
            )
        }
        return ReplacementState(
            board = board,
            limits = limits,
            boundNames = boundNames,
            forceValueCall = forceValueCall,
        ).replace(node)
    }

    private companion object {
        const val MAX_REPLACEMENT_DEPTH = 64
    }
}

private class ReplacementState(
    private val board: Board,
    private val limits: JessieCodeNameReplacementLimits,
    private val boundNames: Set<String>,
    private val forceValueCall: Boolean,
) {
    private var visitedNodes = 0

    fun replace(
        node: JessieCodeAstNode,
        callValue: Boolean = false,
        depth: Int = 1,
        programRoot: Boolean = true,
    ): GMResult<
        JessieCodeAstNode,
        JessieCodeNameReplacementError,
        > {
        if (depth > limits.maxTraversalDepth) {
            return GMResult.Err(
                JessieCodeNameReplacementError.DepthLimitExceeded(
                    limit = limits.maxTraversalDepth,
                    location = node.location,
                ),
            )
        }
        visitedNodes += 1
        if (visitedNodes > limits.maxVisitedNodes) {
            return GMResult.Err(
                JessieCodeNameReplacementError.NodeLimitExceeded(
                    limit = limits.maxVisitedNodes,
                    location = node.location,
                ),
            )
        }

        val variableName = (
            node.value as? JessieCodeAstValue.Text
            )?.value
        if (
            node.type == JessieCodeAstNodeType.VARIABLE &&
            variableName != null &&
            variableName !in boundNames &&
            variableName !in CONSTANT_NAMES
        ) {
            val element = board.elementByName(variableName)
            if (element != null) {
                return GMResult.Ok(
                    replacementNode(
                        source = node,
                        elementId = element.id,
                        callValue =
                            callValue && element.elType == "slider",
                    ),
                )
            }
        }

        val callValueForChildren =
            callValue || requiresNumericCallArguments(node)
        val children = node.children.toMutableList()
        for (index in node.children.indices.reversed()) {
            val child = node.children[index]
            val childCallValue = callValueForChildren ||
                (
                    programRoot &&
                        isWholeExpressionVariable(node, child, index)
                )
            when (child) {
                is JessieCodeAstChild.Node -> {
                    when (
                        val result = replace(
                            node = child.value,
                            callValue = childCallValue,
                            depth = depth + 1,
                            programRoot = false,
                        )
                    ) {
                        is GMResult.Ok -> {
                            children[index] = JessieCodeAstChild.Node(
                                result.value,
                            )
                        }
                        is GMResult.Err -> return result
                    }
                }
                is JessieCodeAstChild.NodeList -> {
                    val nodes = mutableListOf<JessieCodeAstNode>()
                    for (childNode in child.value) {
                        when (
                            val result = replace(
                                node = childNode,
                                callValue = childCallValue,
                                depth = depth + 1,
                                programRoot = false,
                            )
                        ) {
                            is GMResult.Ok -> nodes += result.value
                            is GMResult.Err -> return result
                        }
                    }
                    children[index] = JessieCodeAstChild.NodeList(nodes)
                }
                is JessieCodeAstChild.Text -> Unit
            }
        }
        return GMResult.Ok(node.copy(children = children))
    }

    private fun requiresNumericCallArguments(
        node: JessieCodeAstNode,
    ): Boolean {
        if (!forceValueCall || operationName(node) != "op_execfun") {
            return false
        }
        val functionNode = (
            node.children.getOrNull(0) as?
                JessieCodeAstChild.Node
            )?.value ?: return false
        val functionName = (
            functionNode.value as? JessieCodeAstValue.Text
            )?.value ?: return false
        if (
            functionName == "V" ||
            functionName == "\$" ||
            functionName !in NUMERIC_FUNCTION_NAMES
        ) {
            return false
        }
        val arguments = (
            node.children.getOrNull(1) as?
                JessieCodeAstChild.NodeList
            )?.value ?: return false
        return arguments.firstOrNull()?.type ==
            JessieCodeAstNodeType.VARIABLE
    }

    private fun isWholeExpressionVariable(
        parent: JessieCodeAstNode,
        child: JessieCodeAstChild,
        index: Int,
    ): Boolean =
        forceValueCall &&
            operationName(parent) == "op_none" &&
            index == parent.children.lastIndex &&
            child is JessieCodeAstChild.Node &&
            child.value.type == JessieCodeAstNodeType.VARIABLE

    private fun operationName(node: JessieCodeAstNode): String? =
        if (node.type == JessieCodeAstNodeType.OPERATION) {
            (node.value as? JessieCodeAstValue.Text)?.value
        } else {
            null
        }

    private fun replacementNode(
        source: JessieCodeAstNode,
        elementId: String,
        callValue: Boolean,
    ): JessieCodeAstNode =
        JessieCodeAstNode(
            type = JessieCodeAstNodeType.OPERATION,
            value = JessieCodeAstValue.Text("op_execfun"),
            children = listOf(
                JessieCodeAstChild.Node(
                    JessieCodeAstNode(
                        type = JessieCodeAstNodeType.VARIABLE,
                        value = JessieCodeAstValue.Text(
                            if (callValue) "\$value" else "\$",
                        ),
                        children = emptyList(),
                        location = source.location,
                    ),
                ),
                JessieCodeAstChild.NodeList(
                    listOf(
                        JessieCodeAstNode(
                            type = JessieCodeAstNodeType.STRING,
                            value = JessieCodeAstValue.Text(elementId),
                            children = emptyList(),
                            location = source.location,
                        ),
                    ),
                ),
            ),
            location = source.location,
            replaced = true,
        )

    private companion object {
        val CONSTANT_NAMES = setOf("PI", "EULER", "\$board")

        val NUMERIC_FUNCTION_NAMES = setOf(
            "abs",
            "acos",
            "acosh",
            "acot",
            "asin",
            "asinh",
            "atan",
            "atan2",
            "atanh",
            "binomial",
            "cbrt",
            "ceil",
            "cos",
            "cosh",
            "cot",
            "erf",
            "erfc",
            "erfi",
            "exp",
            "factorial",
            "floor",
            "gcd",
            "hypot",
            "lb",
            "lcm",
            "ld",
            "lg",
            "ln",
            "log",
            "log10",
            "log1p",
            "log2",
            "max",
            "min",
            "ndtr",
            "ndtri",
            "nthroot",
            "pow",
            "random",
            "ratpow",
            "round",
            "sign",
            "sin",
            "sinh",
            "sqrt",
            "tan",
            "tanh",
            "trunc",
        )
    }
}
