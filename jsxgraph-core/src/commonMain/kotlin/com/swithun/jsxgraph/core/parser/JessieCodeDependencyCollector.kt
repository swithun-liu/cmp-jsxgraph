/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> collectDependencies
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.utils.JsNumberFormat

internal data class JessieCodeDependencyLimits(
    val maxVisitedNodes: Int = 100_000,
    val maxTraversalDepth: Int = 256,
)

internal sealed interface JessieCodeDependencyError {
    data class InvalidLimits(
        val maxVisitedNodes: Int,
        val maxTraversalDepth: Int,
    ) : JessieCodeDependencyError

    data class NodeLimitExceeded(
        val limit: Int,
        val location: JessieCodeAstLocation,
    ) : JessieCodeDependencyError

    data class DepthLimitExceeded(
        val limit: Int,
        val location: JessieCodeAstLocation,
    ) : JessieCodeDependencyError

    data class MissingExplicitElement(
        val id: String,
        val location: JessieCodeAstLocation,
    ) : JessieCodeDependencyError
}

internal class JessieCodeDependencyCollector(
    private val board: Board,
    private val limits: JessieCodeDependencyLimits =
        JessieCodeDependencyLimits(),
) {
    // JSXGraph: src/parser/jessiecode.js -> collectDependencies
    internal fun collect(
        node: JessieCodeAstNode,
        parameterNames: Set<String> = emptySet(),
        localNames: Set<String> = emptySet(),
    ): GMResult<
        Map<String, GeometryElement>,
        JessieCodeDependencyError,
        > {
        if (
            limits.maxVisitedNodes < 1 ||
            limits.maxTraversalDepth < 1
        ) {
            return GMResult.Err(
                JessieCodeDependencyError.InvalidLimits(
                    maxVisitedNodes = limits.maxVisitedNodes,
                    maxTraversalDepth = limits.maxTraversalDepth,
                ),
            )
        }

        val state = DependencyState(
            board = board,
            limits = limits,
            excludedNames = parameterNames + localNames + CONSTANT_NAMES,
        )
        return when (val result = state.visit(node)) {
            is GMResult.Ok -> GMResult.Ok(state.dependencies)
            is GMResult.Err -> result
        }
    }

    private companion object {
        val CONSTANT_NAMES = setOf("PI", "EULER", "\$board")
    }
}

private class DependencyState(
    private val board: Board,
    private val limits: JessieCodeDependencyLimits,
    private val excludedNames: Set<String>,
) {
    val dependencies = linkedMapOf<String, GeometryElement>()
    private var visitedNodes = 0

    fun visit(
        root: JessieCodeAstNode,
    ): GMResult<Unit, JessieCodeDependencyError> {
        val stack = mutableListOf(TraversalEntry(root, depth = 1))
        while (stack.isNotEmpty()) {
            val entry = stack.removeAt(stack.lastIndex)
            val node = entry.node
            if (entry.depth > limits.maxTraversalDepth) {
                return GMResult.Err(
                    JessieCodeDependencyError.DepthLimitExceeded(
                        limit = limits.maxTraversalDepth,
                        location = node.location,
                    ),
                )
            }
            visitedNodes += 1
            if (visitedNodes > limits.maxVisitedNodes) {
                return GMResult.Err(
                    JessieCodeDependencyError.NodeLimitExceeded(
                        limit = limits.maxVisitedNodes,
                        location = node.location,
                    ),
                )
            }

            collectVariable(node)
            when (val result = collectExplicitElement(node)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }

            // Upstream walks node.children from right to left. A child which
            // is an array is then walked from left to right.
            val nextNodes = mutableListOf<JessieCodeAstNode>()
            for (child in node.children.asReversed()) {
                when (child) {
                    is JessieCodeAstChild.Node ->
                        nextNodes += child.value
                    is JessieCodeAstChild.NodeList ->
                        nextNodes += child.value
                    is JessieCodeAstChild.Text,
                    is JessieCodeAstChild.TextList,
                    is JessieCodeAstChild.BooleanFlag,
                    JessieCodeAstChild.EmptyObject,
                    JessieCodeAstChild.Undefined,
                    -> Unit
                }
            }
            for (childNode in nextNodes.asReversed()) {
                stack += TraversalEntry(
                    node = childNode,
                    depth = entry.depth + 1,
                )
            }
        }
        return GMResult.Ok(Unit)
    }

    private fun collectVariable(node: JessieCodeAstNode) {
        if (node.type != JessieCodeAstNodeType.VARIABLE) {
            return
        }
        val name = (node.value as? JessieCodeAstValue.Text)?.value
            ?: return
        if (name in excludedNames) {
            return
        }
        val element = board.select(name) ?: return
        dependencies[element.id] = element
    }

    private fun collectExplicitElement(
        node: JessieCodeAstNode,
    ): GMResult<Unit, JessieCodeDependencyError> {
        if (
            node.type != JessieCodeAstNodeType.OPERATION ||
            (node.value as? JessieCodeAstValue.Text)?.value !=
            "op_execfun"
        ) {
            return GMResult.Ok(Unit)
        }
        val functionNode = (
            node.children.getOrNull(0) as?
                JessieCodeAstChild.Node
            )?.value ?: return GMResult.Ok(Unit)
        val functionName = (
            functionNode.value as? JessieCodeAstValue.Text
            )?.value
        if (functionName != "\$" && functionName != "\$value") {
            return GMResult.Ok(Unit)
        }
        val arguments = (
            node.children.getOrNull(1) as?
                JessieCodeAstChild.NodeList
            )?.value ?: return GMResult.Ok(Unit)
        val firstArgument = arguments.firstOrNull()
            ?: return GMResult.Ok(Unit)
        val id = dependencyId(firstArgument.value)
        val element = board.elementById(id)
            ?: return GMResult.Err(
                JessieCodeDependencyError.MissingExplicitElement(
                    id = id,
                    location = firstArgument.location,
                ),
            )
        dependencies[id] = element
        return GMResult.Ok(Unit)
    }

    private fun dependencyId(value: JessieCodeAstValue): String =
        when (value) {
            is JessieCodeAstValue.Text -> value.value
            is JessieCodeAstValue.Number ->
                JsNumberFormat.compact(value.value)
            is JessieCodeAstValue.Boolean -> value.value.toString()
            JessieCodeAstValue.Null -> "null"
        }

    private data class TraversalEntry(
        val node: JessieCodeAstNode,
        val depth: Int,
    )
}
