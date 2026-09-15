/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> AST.node and AST.createNode
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

internal enum class JessieCodeAstNodeType(
    val upstreamName: String,
) {
    OPERATION("node_op"),
    VARIABLE("node_var"),
    CONSTANT("node_const"),
    BOOLEAN_CONSTANT("node_const_bool"),
    STRING("node_str"),
}

internal sealed interface JessieCodeAstValue {
    data class Text(
        val value: String,
    ) : JessieCodeAstValue

    data class Number(
        val value: Double,
    ) : JessieCodeAstValue

    data class Boolean(
        val value: kotlin.Boolean,
    ) : JessieCodeAstValue

    data object Null : JessieCodeAstValue
}

internal sealed interface JessieCodeAstChild {
    data class Node(
        val value: JessieCodeAstNode,
    ) : JessieCodeAstChild

    data class NodeList(
        val value: List<JessieCodeAstNode>,
    ) : JessieCodeAstChild

    data class Text(
        val value: String,
    ) : JessieCodeAstChild

    /**
     * Upstream passes a raw `{}` child to the `op_emptyobject` node.
     */
    data object EmptyObject : JessieCodeAstChild

    /**
     * Upstream passes raw JavaScript `undefined` to a bare `return`.
     */
    data object Undefined : JessieCodeAstChild
}

internal data class JessieCodeAstLocation(
    val line: Int,
    val column: Int,
    val endLine: Int,
    val endColumn: Int,
)

/**
 * Generic JessieCode node shape used by the generated upstream parser.
 *
 * `isMath` is nullable because the generated semantic actions leave it absent
 * on some nodes rather than assigning `false`.
 */
internal data class JessieCodeAstNode(
    val type: JessieCodeAstNodeType,
    val value: JessieCodeAstValue,
    val children: List<JessieCodeAstChild>,
    val location: JessieCodeAstLocation,
    val isMath: Boolean? = null,
    val replaced: Boolean = false,
)
