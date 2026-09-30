/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/ca.js -> JXG.CA
 * Copyright 2011-2019 Michael Gerhaeuser and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult

internal data class JessieCodeComputerAlgebraLimits(
    val maxTransformationSteps: Int = 1_000_000,
    val maxOutputNodes: Int = 100_000,
    val maxOutputDepth: Int = 256,
    val maxDerivativeOrder: Int = 64,
)

internal sealed interface JessieCodeComputerAlgebraError {
    val location: JessieCodeAstLocation?

    data class InvalidLimits(
        val limits: JessieCodeComputerAlgebraLimits,
    ) : JessieCodeComputerAlgebraError {
        override val location: JessieCodeAstLocation? = null
    }

    data class TransformationStepLimitExceeded(
        val limit: Int,
        override val location: JessieCodeAstLocation,
    ) : JessieCodeComputerAlgebraError

    data class OutputNodeLimitExceeded(
        val limit: Int,
        val requestedSize: Long,
        override val location: JessieCodeAstLocation,
    ) : JessieCodeComputerAlgebraError

    data class OutputDepthLimitExceeded(
        val limit: Int,
        val requestedDepth: Int,
        override val location: JessieCodeAstLocation,
    ) : JessieCodeComputerAlgebraError

    data class DerivativeOrderLimitExceeded(
        val limit: Int,
        val requestedOrder: Double,
        override val location: JessieCodeAstLocation,
    ) : JessieCodeComputerAlgebraError

    data class UnknownElementaryDerivative(
        val functionName: String,
        override val location: JessieCodeAstLocation,
    ) : JessieCodeComputerAlgebraError

    data class InvalidAst(
        val reason: String,
        override val location: JessieCodeAstLocation,
    ) : JessieCodeComputerAlgebraError
}

/**
 * Immutable translation of `src/parser/ca.js`.
 *
 * Upstream mutates parser nodes in place. This implementation returns copied
 * nodes so a failed bounded transformation cannot leave a partially rewritten
 * AST behind.
 */
internal class JessieCodeComputerAlgebra(
    private val limits: JessieCodeComputerAlgebraLimits =
        JessieCodeComputerAlgebraLimits(),
) {
    internal fun transform(
        ast: JessieCodeAstNode,
    ): GMResult<JessieCodeAstNode, JessieCodeComputerAlgebraError> {
        if (
            limits.maxTransformationSteps < 1 ||
            limits.maxOutputNodes < 1 ||
            limits.maxOutputDepth < 1 ||
            limits.maxDerivativeOrder < 1
        ) {
            return GMResult.Err(
                JessieCodeComputerAlgebraError.InvalidLimits(limits),
            )
        }
        return Transformer(limits).transform(ast)
    }
}

private typealias AlgebraResult<T> =
    GMResult<T, JessieCodeComputerAlgebraError>

private data class Simplification(
    val node: JessieCodeAstNode,
    val revisit: Boolean = false,
)

private class Transformer(
    private val limits: JessieCodeComputerAlgebraLimits,
) {
    private var transformationSteps = 0

    fun transform(ast: JessieCodeAstNode): AlgebraResult<JessieCodeAstNode> {
        when (val result = validateOutput(ast)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val expanded = when (
            val result = expandDerivatives(
                node = ast,
                parent = null,
                ast = ast,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val simplified = when (val result = removeTrivialNodes(expanded)) {
            is GMResult.Ok -> result.value.node
            is GMResult.Err -> return result
        }
        return when (val result = validateOutput(simplified)) {
            is GMResult.Ok -> GMResult.Ok(simplified)
            is GMResult.Err -> result
        }
    }

    // JSXGraph: src/parser/ca.js -> findMapNode
    private fun findMapNode(
        mapName: String,
        node: JessieCodeAstNode,
    ): JessieCodeAstNode? {
        if (
            operationName(node) == "op_assign" &&
            node.children.size >= 2 &&
            nodeValue(node.children[0]) == mapName
        ) {
            return (node.children[1] as? JessieCodeAstChild.Node)?.value
        }
        for (child in node.children) {
            if (child is JessieCodeAstChild.Node) {
                findMapNode(mapName, child.value)?.let { return it }
            }
        }
        return null
    }

    // JSXGraph: src/parser/ca.js -> setMath
    private fun setMath(node: JessieCodeAstNode): JessieCodeAstNode {
        val children = node.children.map { child ->
            when (child) {
                is JessieCodeAstChild.Node ->
                    JessieCodeAstChild.Node(setMath(child.value))
                is JessieCodeAstChild.NodeList ->
                    JessieCodeAstChild.NodeList(
                        child.value.map(::setMath),
                    )
                else -> child
            }
        }
        val isMath = when {
            node.type == JessieCodeAstNodeType.VARIABLE -> true
            node.type == JessieCodeAstNodeType.CONSTANT -> true
            operationName(node) in MATH_OPERATIONS -> true
            else -> node.isMath
        }
        return node.copy(children = children, isMath = isMath)
    }

    // JSXGraph: src/parser/ca.js -> expandDerivatives
    private fun expandDerivatives(
        node: JessieCodeAstNode,
        parent: JessieCodeAstNode?,
        ast: JessieCodeAstNode,
    ): AlgebraResult<JessieCodeAstNode> {
        tick(node.location)?.let { return GMResult.Err(it) }

        val transformedChildren = mutableListOf<JessieCodeAstChild>()
        for (child in node.children) {
            when (child) {
                is JessieCodeAstChild.Node -> {
                    val transformed = when (
                        val result = expandDerivatives(
                            node = child.value,
                            parent = node,
                            ast = ast,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    transformedChildren +=
                        JessieCodeAstChild.Node(transformed)
                }
                is JessieCodeAstChild.NodeList -> {
                    val transformed = mutableListOf<JessieCodeAstNode>()
                    for (listNode in child.value) {
                        when (
                            val result = expandDerivatives(
                                node = listNode,
                                parent = node,
                                ast = ast,
                            )
                        ) {
                            is GMResult.Ok -> transformed += result.value
                            is GMResult.Err -> return result
                        }
                    }
                    transformedChildren +=
                        JessieCodeAstChild.NodeList(transformed)
                }
                else -> transformedChildren += child
            }
        }
        val transformedNode = node.copy(children = transformedChildren)
        if (
            operationName(transformedNode) != "op_execfun" ||
            nodeValue(transformedNode.children.firstOrNull()) != "D"
        ) {
            return GMResult.Ok(transformedNode)
        }
        return expandDerivativeCall(transformedNode, parent, ast)
    }

    private fun expandDerivativeCall(
        node: JessieCodeAstNode,
        parent: JessieCodeAstNode?,
        ast: JessieCodeAstNode,
    ): AlgebraResult<JessieCodeAstNode> {
        val arguments = when (val result = nodeListChild(node, 1)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val expression = arguments.firstOrNull()
            ?: return invalidAst(
                node,
                "D requires an expression or map name.",
            )

        var parameters = listOf("x")
        var variableName: String? = "x"
        var codeNode = expression
        val mapName = textValue(expression)
        val unresolvedMapNode = if (
            expression.type == JessieCodeAstNodeType.VARIABLE &&
            mapName != null
        ) {
            findMapNode(mapName, ast)
        } else {
            null
        }
        val mapNode = if (unresolvedMapNode != null) {
            when (
                val result = expandDerivatives(
                    node = unresolvedMapNode,
                    parent = null,
                    ast = ast,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        } else {
            null
        }
        if (mapNode != null) {
            if (operationName(mapNode) != "op_map") {
                return invalidAst(
                    mapNode,
                    "D map reference '$mapName' does not resolve to a map.",
                )
            }
            parameters = when (val result = textListChild(mapNode, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            codeNode = when (val result = nodeChild(mapNode, 1)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            variableName = if (arguments.size >= 2) {
                textValue(arguments[1])
                    ?: return invalidAst(
                        arguments[1],
                        "D variable must be an identifier.",
                    )
            } else {
                parameters.firstOrNull()
            }
        } else if (arguments.size >= 2) {
            variableName = textValue(arguments[1])
                ?: return invalidAst(
                    arguments[1],
                    "D variable must be an identifier.",
                )
        }

        val requestedOrder = if (arguments.size >= 3) {
            numberValue(arguments[2])
                ?: return invalidAst(
                    arguments[2],
                    "D order must be numeric.",
                )
        } else {
            1.0
        }
        val order = when {
            requestedOrder.isNaN() || requestedOrder < 1.0 -> 0
            !requestedOrder.isFinite() ||
                requestedOrder > limits.maxDerivativeOrder.toDouble() ->
                return GMResult.Err(
                    JessieCodeComputerAlgebraError
                        .DerivativeOrderLimitExceeded(
                            limit = limits.maxDerivativeOrder,
                            requestedOrder = requestedOrder,
                            location = node.location,
                        ),
                )
            else -> requestedOrder.toInt()
        }

        var derivative = codeNode
        repeat(order) {
            derivative = when (
                val result = derivative(derivative, variableName)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            when (val result = validateOutput(derivative)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            derivative = when (
                val result = removeTrivialNodes(derivative)
            ) {
                is GMResult.Ok -> result.value.node
                is GMResult.Err -> return result
            }
        }

        val replacement = if (operationName(parent) == "op_assign") {
            operation(
                name = "op_map",
                children = listOf(
                    JessieCodeAstChild.TextList(parameters),
                    JessieCodeAstChild.Node(derivative),
                ),
                location = node.location,
                isMath = null,
            )
        } else {
            derivative
        }
        return GMResult.Ok(setMath(replacement))
    }

    // JSXGraph: src/parser/ca.js -> derivative
    private fun derivative(
        node: JessieCodeAstNode,
        variableName: String?,
    ): AlgebraResult<JessieCodeAstNode> {
        tick(node.location)?.let { return GMResult.Err(it) }
        return when (node.type) {
            JessieCodeAstNodeType.VARIABLE ->
                GMResult.Ok(
                    number(
                        value = if (
                            textValue(node) == variableName
                        ) {
                            1.0
                        } else {
                            0.0
                        },
                        location = node.location,
                    ),
                )
            JessieCodeAstNodeType.CONSTANT ->
                GMResult.Ok(number(0.0, node.location))
            JessieCodeAstNodeType.BOOLEAN_CONSTANT,
            JessieCodeAstNodeType.STRING,
            -> invalidAst(
                node,
                "Cannot differentiate ${node.type.upstreamName}.",
            )
            JessieCodeAstNodeType.OPERATION ->
                derivativeOperation(node, variableName)
        }
    }

    private fun derivativeOperation(
        node: JessieCodeAstNode,
        variableName: String?,
    ): AlgebraResult<JessieCodeAstNode> {
        val first = when (val result = nodeChild(node, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val name = operationName(node)
            ?: return invalidAst(node, "Operation value must be text.")
        return when (name) {
            "op_execfun" -> {
                val arguments = when (val result = nodeListChild(node, 1)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (textValue(first) == "pow") {
                    deriveElementary(node, variableName)
                } else if (arguments.isEmpty()) {
                    GMResult.Ok(number(0.0, node.location))
                } else {
                    val elementary = when (
                        val result = deriveElementary(node, variableName)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    val inner = when (
                        val result = derivative(
                            arguments[0],
                            variableName,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    GMResult.Ok(
                        binary(
                            "op_mul",
                            elementary,
                            inner,
                            node.location,
                        ),
                    )
                }
            }
            "op_div" -> {
                val second = when (val result = nodeChild(node, 1)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val firstDerivative = when (
                    val result = derivative(first, variableName)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val secondDerivative = when (
                    val result = derivative(second, variableName)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                GMResult.Ok(
                    binary(
                        "op_div",
                        binary(
                            "op_sub",
                            binary(
                                "op_mul",
                                firstDerivative,
                                second,
                                node.location,
                            ),
                            binary(
                                "op_mul",
                                first,
                                secondDerivative,
                                node.location,
                            ),
                            node.location,
                        ),
                        binary(
                            "op_mul",
                            second,
                            second,
                            node.location,
                        ),
                        node.location,
                    ),
                )
            }
            "op_mul" -> {
                val second = when (val result = nodeChild(node, 1)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val secondDerivative = when (
                    val result = derivative(second, variableName)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val firstDerivative = when (
                    val result = derivative(first, variableName)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                GMResult.Ok(
                    binary(
                        "op_add",
                        binary(
                            "op_mul",
                            first,
                            secondDerivative,
                            node.location,
                        ),
                        binary(
                            "op_mul",
                            firstDerivative,
                            second,
                            node.location,
                        ),
                        node.location,
                    ),
                )
            }
            "op_neg" -> {
                when (val result = derivative(first, variableName)) {
                    is GMResult.Ok -> GMResult.Ok(
                        unary("op_neg", result.value, node.location),
                    )
                    is GMResult.Err -> result
                }
            }
            "op_add",
            "op_sub",
            -> {
                val second = when (val result = nodeChild(node, 1)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val firstDerivative = when (
                    val result = derivative(first, variableName)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val secondDerivative = when (
                    val result = derivative(second, variableName)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                GMResult.Ok(
                    binary(
                        name,
                        firstDerivative,
                        secondDerivative,
                        node.location,
                    ),
                )
            }
            "op_exp" -> derivativePower(node, first, variableName)
            else -> invalidAst(
                node,
                "Derivative of operation '$name' is not implemented.",
            )
        }
    }

    private fun derivativePower(
        node: JessieCodeAstNode,
        base: JessieCodeAstNode,
        variableName: String?,
    ): AlgebraResult<JessieCodeAstNode> {
        val exponent = when (val result = nodeChild(node, 1)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val baseDerivative = when (
            val result = derivative(base, variableName)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val exponentDerivative = when (
            val result = derivative(exponent, variableName)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            binary(
                "op_mul",
                node,
                binary(
                    "op_add",
                    binary(
                        "op_mul",
                        baseDerivative,
                        binary(
                            "op_div",
                            exponent,
                            base,
                            node.location,
                        ),
                        node.location,
                    ),
                    binary(
                        "op_mul",
                        exponentDerivative,
                        call("log", listOf(base), node.location),
                        node.location,
                    ),
                    node.location,
                ),
                node.location,
            ),
        )
    }

    // JSXGraph: src/parser/ca.js -> deriveElementary
    private fun deriveElementary(
        node: JessieCodeAstNode,
        variableName: String?,
    ): AlgebraResult<JessieCodeAstNode> {
        val function = when (val result = nodeChild(node, 0)) {
            is GMResult.Ok -> textValue(result.value)
            is GMResult.Err -> return result
        } ?: return invalidAst(node, "Function name must be text.")
        val arguments = when (val result = nodeListChild(node, 1)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val argument = arguments.firstOrNull()
            ?: return invalidAst(
                node,
                "Function '$function' has no derivative argument.",
            )
        val location = node.location
        return when (function) {
            "abs" -> GMResult.Ok(
                binary(
                    "op_div",
                    argument,
                    call(
                        "sqrt",
                        listOf(
                            binary(
                                "op_mul",
                                argument,
                                argument,
                                location,
                            ),
                        ),
                        location,
                    ),
                    location,
                ),
            )
            "sqrt" -> GMResult.Ok(
                binary(
                    "op_div",
                    number(1.0, location),
                    binary(
                        "op_mul",
                        number(2.0, location),
                        node,
                        location,
                    ),
                    location,
                ),
            )
            "sin" -> GMResult.Ok(
                call("cos", arguments, location),
            )
            "cos" -> GMResult.Ok(
                unary(
                    "op_neg",
                    call("sin", arguments, location),
                    location,
                ),
            )
            "tan" -> GMResult.Ok(
                binary(
                    "op_div",
                    number(1.0, location),
                    binary(
                        "op_exp",
                        call("cos", arguments, location),
                        number(2.0, location),
                        location,
                    ),
                    location,
                ),
            )
            "cot" -> GMResult.Ok(
                unary(
                    "op_neg",
                    binary(
                        "op_div",
                        number(1.0, location),
                        binary(
                            "op_exp",
                            call("sin", arguments, location),
                            number(2.0, location),
                            location,
                        ),
                        location,
                    ),
                    location,
                ),
            )
            "exp" -> GMResult.Ok(node)
            "pow" -> derivePowCall(
                node = node,
                arguments = arguments,
                variableName = variableName,
            )
            "log",
            "ln",
            -> GMResult.Ok(
                binary(
                    "op_div",
                    number(1.0, location),
                    argument,
                    location,
                ),
            )
            "log2",
            "lb",
            "ld",
            -> GMResult.Ok(
                binary(
                    "op_mul",
                    binary(
                        "op_div",
                        number(1.0, location),
                        argument,
                        location,
                    ),
                    number(LOG2_FACTOR, location),
                    location,
                ),
            )
            "log10",
            "lg",
            -> GMResult.Ok(
                binary(
                    "op_mul",
                    binary(
                        "op_div",
                        number(1.0, location),
                        argument,
                        location,
                    ),
                    number(LOG10_FACTOR, location),
                    location,
                ),
            )
            "asin" -> GMResult.Ok(
                binary(
                    "op_div",
                    number(1.0, location),
                    call(
                        "sqrt",
                        listOf(
                            binary(
                                "op_sub",
                                number(1.0, location),
                                binary(
                                    "op_mul",
                                    argument,
                                    argument,
                                    location,
                                ),
                                location,
                            ),
                        ),
                        location,
                    ),
                    location,
                ),
            )
            "acos" -> GMResult.Ok(
                unary(
                    "op_neg",
                    binary(
                        "op_div",
                        number(1.0, location),
                        call(
                            "sqrt",
                            listOf(
                                binary(
                                    "op_sub",
                                    number(1.0, location),
                                    binary(
                                        "op_mul",
                                        argument,
                                        argument,
                                        location,
                                    ),
                                    location,
                                ),
                            ),
                            location,
                        ),
                        location,
                    ),
                    location,
                ),
            )
            "atan" -> GMResult.Ok(
                binary(
                    "op_div",
                    number(1.0, location),
                    binary(
                        "op_add",
                        number(1.0, location),
                        binary(
                            "op_mul",
                            argument,
                            argument,
                            location,
                        ),
                        location,
                    ),
                    location,
                ),
            )
            "acot" -> GMResult.Ok(
                unary(
                    "op_neg",
                    binary(
                        "op_div",
                        number(1.0, location),
                        binary(
                            "op_add",
                            number(1.0, location),
                            binary(
                                "op_mul",
                                argument,
                                argument,
                                location,
                            ),
                            location,
                        ),
                        location,
                    ),
                    location,
                ),
            )
            "sinh" -> GMResult.Ok(
                call("cosh", listOf(argument), location),
            )
            "cosh" -> GMResult.Ok(
                call("sinh", listOf(argument), location),
            )
            "tanh" -> GMResult.Ok(
                binary(
                    "op_sub",
                    number(1.0, location),
                    binary(
                        "op_exp",
                        call("tanh", listOf(argument), location),
                        number(2.0, location),
                        location,
                    ),
                    location,
                ),
            )
            "asinh" -> GMResult.Ok(
                binary(
                    "op_div",
                    number(1.0, location),
                    call(
                        "sqrt",
                        listOf(
                            binary(
                                "op_add",
                                binary(
                                    "op_mul",
                                    argument,
                                    argument,
                                    location,
                                ),
                                number(1.0, location),
                                location,
                            ),
                        ),
                        location,
                    ),
                    location,
                ),
            )
            "acosh" -> GMResult.Ok(
                binary(
                    "op_div",
                    number(1.0, location),
                    call(
                        "sqrt",
                        listOf(
                            binary(
                                "op_sub",
                                binary(
                                    "op_mul",
                                    argument,
                                    argument,
                                    location,
                                ),
                                number(1.0, location),
                                location,
                            ),
                        ),
                        location,
                    ),
                    location,
                ),
            )
            "atanh" -> GMResult.Ok(
                binary(
                    "op_div",
                    number(1.0, location),
                    binary(
                        "op_sub",
                        number(1.0, location),
                        binary(
                            "op_mul",
                            argument,
                            argument,
                            location,
                        ),
                        location,
                    ),
                    location,
                ),
            )
            else -> GMResult.Err(
                JessieCodeComputerAlgebraError
                    .UnknownElementaryDerivative(
                        functionName = function,
                        location = node.location,
                    ),
            )
        }
    }

    private fun derivePowCall(
        node: JessieCodeAstNode,
        arguments: List<JessieCodeAstNode>,
        variableName: String?,
    ): AlgebraResult<JessieCodeAstNode> {
        if (arguments.size < 2) {
            return invalidAst(node, "pow derivative requires two arguments.")
        }
        val base = arguments[0]
        val exponent = arguments[1]
        val baseDerivative = when (
            val result = derivative(base, variableName)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val exponentDerivative = when (
            val result = derivative(exponent, variableName)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            binary(
                "op_mul",
                node,
                binary(
                    "op_add",
                    binary(
                        "op_mul",
                        baseDerivative,
                        binary(
                            "op_div",
                            exponent,
                            base,
                            node.location,
                        ),
                        node.location,
                    ),
                    binary(
                        "op_mul",
                        exponentDerivative,
                        call("log", listOf(base), node.location),
                        node.location,
                    ),
                    node.location,
                ),
                node.location,
            ),
        )
    }

    // JSXGraph: src/parser/ca.js -> removeTrivialNodes
    private fun removeTrivialNodes(
        node: JessieCodeAstNode,
    ): AlgebraResult<Simplification> {
        tick(node.location)?.let { return GMResult.Err(it) }
        val children = mutableListOf<JessieCodeAstChild>()
        for (child in node.children) {
            when (child) {
                is JessieCodeAstChild.Node -> {
                    val simplified = when (
                        val result = simplifyChild(child.value)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    children += JessieCodeAstChild.Node(simplified)
                }
                is JessieCodeAstChild.NodeList -> {
                    val simplified = mutableListOf<JessieCodeAstNode>()
                    for (listNode in child.value) {
                        when (val result = simplifyChild(listNode)) {
                            is GMResult.Ok -> simplified += result.value
                            is GMResult.Err -> return result
                        }
                    }
                    children += JessieCodeAstChild.NodeList(simplified)
                }
                else -> children += child
            }
        }
        val transformed = node.copy(children = children)
        if (transformed.type != JessieCodeAstNodeType.OPERATION) {
            return GMResult.Ok(Simplification(transformed))
        }
        return simplifyOperation(transformed)
    }

    private fun simplifyChild(
        node: JessieCodeAstNode,
    ): AlgebraResult<JessieCodeAstNode> {
        var current = node
        do {
            val simplified = when (
                val result = removeTrivialNodes(current)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            current = simplified.node
        } while (simplified.revisit)
        return GMResult.Ok(current)
    }

    private fun simplifyOperation(
        node: JessieCodeAstNode,
    ): AlgebraResult<Simplification> {
        return when (operationName(node)) {
            "op_map" -> simplifyMap(node)
            "op_add" -> simplifyAdd(node)
            "op_mul" -> simplifyMultiply(node)
            "op_sub" -> simplifySubtract(node)
            "op_neg" -> simplifyNegative(node)
            "op_div" -> simplifyDivide(node)
            "op_exp" -> simplifyPower(node)
            "op_execfun" -> simplifyElementary(node)
            else -> GMResult.Ok(Simplification(node))
        }
    }

    private fun simplifyMap(
        node: JessieCodeAstNode,
    ): AlgebraResult<Simplification> {
        val parameters = when (val result = textListChild(node, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val body = when (val result = nodeChild(node, 1)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val transformedBody = if (
            body.type == JessieCodeAstNodeType.VARIABLE &&
            textValue(body) in parameters
        ) {
            body.copy(isMath = true)
        } else {
            body
        }
        return GMResult.Ok(
            Simplification(
                node.copy(
                    children = listOf(
                        JessieCodeAstChild.TextList(parameters),
                        JessieCodeAstChild.Node(transformedBody),
                    ),
                ),
            ),
        )
    }

    private fun simplifyAdd(
        node: JessieCodeAstNode,
    ): AlgebraResult<Simplification> {
        val pair = when (val result = binaryChildren(node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val left = pair.first
        val right = pair.second
        if (isNumber(left, 0.0)) {
            return simplified(right)
        }
        if (isNumber(right, 0.0)) {
            return simplified(left)
        }
        val leftNumber = numberValue(left)
        val rightNumber = numberValue(right)
        if (
            left.type == JessieCodeAstNodeType.CONSTANT &&
            right.type == JessieCodeAstNodeType.CONSTANT &&
            leftNumber != null &&
            rightNumber != null
        ) {
            return simplified(
                number(leftNumber + rightNumber, left.location),
            )
        }
        if (
            left.type == JessieCodeAstNodeType.VARIABLE &&
            right.type == JessieCodeAstNodeType.VARIABLE &&
            textValue(left) == textValue(right)
        ) {
            return simplified(
                binary(
                    "op_mul",
                    number(2.0, node.location),
                    right,
                    node.location,
                ),
            )
        }
        if (operationName(left) == "op_neg") {
            val operand = when (val result = nodeChild(left, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return simplified(
                binary(
                    "op_sub",
                    right,
                    operand,
                    node.location,
                ),
                revisit = true,
            )
        }
        if (operationName(right) == "op_neg") {
            val operand = when (val result = nodeChild(right, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return simplified(
                binary(
                    "op_sub",
                    left,
                    operand,
                    node.location,
                ),
                revisit = true,
            )
        }
        combineLikeTerms(
            node = node,
            left = left,
            right = right,
            coefficientOperator = "op_add",
        )?.let { return GMResult.Ok(it) }
        return GMResult.Ok(Simplification(node))
    }

    private fun simplifyMultiply(
        node: JessieCodeAstNode,
    ): AlgebraResult<Simplification> {
        val pair = when (val result = binaryChildren(node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        var left = pair.first
        var right = pair.second
        if (isNumber(left, 1.0)) {
            return simplified(right)
        }
        if (isNumber(right, 1.0)) {
            return simplified(left)
        }
        if (isNumber(left, 0.0)) {
            return simplified(left)
        }
        if (isNumber(right, 0.0)) {
            return simplified(right)
        }

        if (
            operationName(left) == "op_neg" &&
            operationName(right) == "op_neg"
        ) {
            val leftOperand = when (val result = nodeChild(left, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val rightOperand = when (val result = nodeChild(right, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return simplified(
                binary(
                    "op_mul",
                    leftOperand,
                    rightOperand,
                    node.location,
                ),
                revisit = true,
            )
        }
        if (
            operationName(left) == "op_neg" &&
            operationName(right) != "op_neg"
        ) {
            val leftOperand = when (val result = nodeChild(left, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return simplified(
                unary(
                    "op_neg",
                    binary(
                        "op_mul",
                        leftOperand,
                        right,
                        node.location,
                    ),
                    node.location,
                ),
                revisit = true,
            )
        }
        if (
            operationName(left) != "op_neg" &&
            operationName(right) == "op_neg"
        ) {
            val rightOperand = when (val result = nodeChild(right, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return simplified(
                unary(
                    "op_neg",
                    binary(
                        "op_mul",
                        left,
                        rightOperand,
                        node.location,
                    ),
                    node.location,
                ),
                revisit = true,
            )
        }

        reciprocalDenominator(left)?.let { denominator ->
            return simplified(
                binary(
                    "op_div",
                    right,
                    denominator,
                    node.location,
                ),
                revisit = true,
            )
        }
        reciprocalDenominator(right)?.let { denominator ->
            return simplified(
                binary(
                    "op_div",
                    left,
                    denominator,
                    node.location,
                ),
                revisit = true,
            )
        }
        if (
            left.type != JessieCodeAstNodeType.CONSTANT &&
            right.type == JessieCodeAstNodeType.CONSTANT
        ) {
            return simplified(
                binary("op_mul", right, left, node.location),
                revisit = true,
            )
        }
        if (
            left.type != JessieCodeAstNodeType.CONSTANT &&
            operationName(right) == "op_neg"
        ) {
            val operand = when (val result = nodeChild(right, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (operand.type == JessieCodeAstNodeType.CONSTANT) {
                return simplified(
                    binary("op_mul", right, left, node.location),
                    revisit = true,
                )
            }
        }
        if (
            left.type == JessieCodeAstNodeType.OPERATION &&
            operationName(left) != "op_execfun" &&
            (
                right.type == JessieCodeAstNodeType.VARIABLE ||
                    operationName(right) == "op_execfun"
                )
        ) {
            return simplified(
                binary("op_mul", right, left, node.location),
                revisit = true,
            )
        }
        if (
            left.type != JessieCodeAstNodeType.OPERATION &&
            operationName(right) == "op_neg"
        ) {
            val operand = when (val result = nodeChild(right, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (operand.type == JessieCodeAstNodeType.VARIABLE) {
                return simplified(
                    binary("op_mul", right, left, node.location),
                    revisit = true,
                )
            }
        }

        val rightNested = operationName(right)
        if (
            left.type != JessieCodeAstNodeType.CONSTANT &&
            rightNested in setOf("op_mul", "op_div")
        ) {
            val nested = when (val result = binaryChildren(right)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (nested.first.type == JessieCodeAstNodeType.CONSTANT) {
                return simplified(
                    binary(
                        "op_mul",
                        nested.first,
                        binary(
                            rightNested.orEmpty(),
                            left,
                            nested.second,
                            right.location,
                        ),
                        node.location,
                    ),
                    revisit = true,
                )
            }
        }
        if (
            right.type != JessieCodeAstNodeType.CONSTANT &&
            operationName(left) == "op_mul"
        ) {
            val nested = when (val result = binaryChildren(left)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (nested.first.type == JessieCodeAstNodeType.CONSTANT) {
                return simplified(
                    binary(
                        "op_mul",
                        nested.first,
                        binary(
                            "op_mul",
                            nested.second,
                            right,
                            node.location,
                        ),
                        node.location,
                    ),
                    revisit = true,
                )
            }
        }

        val leftNumber = numberValue(left)
        val rightNumber = numberValue(right)
        if (
            left.type == JessieCodeAstNodeType.CONSTANT &&
            right.type == JessieCodeAstNodeType.CONSTANT &&
            leftNumber != null &&
            rightNumber != null
        ) {
            return simplified(
                number(leftNumber * rightNumber, left.location),
            )
        }
        if (
            leftNumber != null &&
            left.type == JessieCodeAstNodeType.CONSTANT &&
            rightNested in setOf("op_mul", "op_div")
        ) {
            val nested = when (val result = binaryChildren(right)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val nestedNumber = numberValue(nested.first)
            if (
                nested.first.type == JessieCodeAstNodeType.CONSTANT &&
                nestedNumber != null
            ) {
                return simplified(
                    right.copy(
                        children = listOf(
                            JessieCodeAstChild.Node(
                                number(
                                    nestedNumber * leftNumber,
                                    nested.first.location,
                                ),
                            ),
                            JessieCodeAstChild.Node(nested.second),
                        ),
                    ),
                )
            }
        }

        if (structurallyEqual(left, right)) {
            return simplified(
                binary(
                    "op_exp",
                    left,
                    number(2.0, node.location),
                    node.location,
                ),
            )
        }
        if (operationName(right) == "op_exp") {
            val power = when (val result = binaryChildren(right)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (structurallyEqual(left, power.first)) {
                return simplified(
                    binary(
                        "op_exp",
                        power.first,
                        binary(
                            "op_add",
                            power.second,
                            number(1.0, node.location),
                            node.location,
                        ),
                        right.location,
                    ),
                    revisit = true,
                )
            }
        }
        if (
            operationName(left) == "op_exp" &&
            operationName(right) == "op_exp"
        ) {
            val leftPower = when (val result = binaryChildren(left)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val rightPower = when (val result = binaryChildren(right)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (structurallyEqual(leftPower.first, rightPower.first)) {
                return simplified(
                    binary(
                        "op_exp",
                        leftPower.first,
                        binary(
                            "op_add",
                            leftPower.second,
                            rightPower.second,
                            node.location,
                        ),
                        left.location,
                    ),
                    revisit = true,
                )
            }
        }
        return GMResult.Ok(Simplification(node))
    }

    private fun simplifySubtract(
        node: JessieCodeAstNode,
    ): AlgebraResult<Simplification> {
        val pair = when (val result = binaryChildren(node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val left = pair.first
        val right = pair.second
        if (isNumber(left, 0.0)) {
            return simplified(
                unary("op_neg", right, node.location),
            )
        }
        if (isNumber(right, 0.0)) {
            return simplified(left)
        }
        val leftNumber = numberValue(left)
        val rightNumber = numberValue(right)
        if (
            left.type == JessieCodeAstNodeType.CONSTANT &&
            right.type == JessieCodeAstNodeType.CONSTANT &&
            leftNumber != null &&
            rightNumber != null
        ) {
            return simplified(
                number(leftNumber - rightNumber, left.location),
            )
        }
        if (
            left.type == JessieCodeAstNodeType.VARIABLE &&
            right.type == JessieCodeAstNodeType.VARIABLE &&
            textValue(left) == textValue(right)
        ) {
            return simplified(number(0.0, node.location))
        }
        combineLikeTerms(
            node = node,
            left = left,
            right = right,
            coefficientOperator = "op_sub",
        )?.let { return GMResult.Ok(it) }
        if (operationName(right) == "op_neg") {
            val operand = when (val result = nodeChild(right, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return simplified(
                binary("op_add", left, operand, node.location),
                revisit = true,
            )
        }
        return GMResult.Ok(Simplification(node))
    }

    private fun combineLikeTerms(
        node: JessieCodeAstNode,
        left: JessieCodeAstNode,
        right: JessieCodeAstNode,
        coefficientOperator: String,
    ): Simplification? {
        val leftProduct = binaryChildrenOrNull(left, "op_mul")
        val rightProduct = binaryChildrenOrNull(right, "op_mul")
        if (
            leftProduct != null &&
            rightProduct != null &&
            structurallyEqual(leftProduct.second, rightProduct.second)
        ) {
            return Simplification(
                binary(
                    "op_mul",
                    binary(
                        coefficientOperator,
                        leftProduct.first,
                        rightProduct.first,
                        node.location,
                    ),
                    leftProduct.second,
                    node.location,
                ),
                revisit = true,
            )
        }
        if (
            leftProduct != null &&
            structurallyEqual(leftProduct.second, right)
        ) {
            return Simplification(
                binary(
                    "op_mul",
                    binary(
                        coefficientOperator,
                        leftProduct.first,
                        number(1.0, node.location),
                        node.location,
                    ),
                    right,
                    node.location,
                ),
                revisit = true,
            )
        }
        if (
            rightProduct != null &&
            structurallyEqual(rightProduct.second, left)
        ) {
            val firstCoefficient = if (
                coefficientOperator == "op_add"
            ) {
                number(1.0, node.location)
            } else {
                number(1.0, node.location)
            }
            return Simplification(
                binary(
                    "op_mul",
                    binary(
                        coefficientOperator,
                        firstCoefficient,
                        rightProduct.first,
                        node.location,
                    ),
                    left,
                    node.location,
                ),
                revisit = true,
            )
        }
        return null
    }

    private fun simplifyNegative(
        node: JessieCodeAstNode,
    ): AlgebraResult<Simplification> {
        val operand = when (val result = nodeChild(node, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (isNumber(operand, 0.0)) {
            return simplified(operand)
        }
        if (operationName(operand) == "op_neg") {
            return when (val result = nodeChild(operand, 0)) {
                is GMResult.Ok -> simplified(result.value)
                is GMResult.Err -> result
            }
        }
        return GMResult.Ok(Simplification(node))
    }

    private fun simplifyDivide(
        node: JessieCodeAstNode,
    ): AlgebraResult<Simplification> {
        val pair = when (val result = binaryChildren(node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val numerator = pair.first
        val denominator = pair.second
        val numeratorValue = numberValue(numerator)
        val denominatorValue = numberValue(denominator)
        if (
            numeratorValue != null &&
            denominatorValue != null &&
            numeratorValue == denominatorValue &&
            numeratorValue != 0.0
        ) {
            return simplified(number(1.0, numerator.location))
        }
        if (
            numeratorValue == 0.0 &&
            denominatorValue != null &&
            denominatorValue != 0.0
        ) {
            return simplified(number(0.0, numerator.location))
        }
        if (
            numeratorValue == 0.0 &&
            (
                denominator.type == JessieCodeAstNodeType.OPERATION ||
                    denominator.type == JessieCodeAstNodeType.VARIABLE
                )
        ) {
            return simplified(number(0.0, node.location))
        }
        if (
            numerator.type == JessieCodeAstNodeType.VARIABLE &&
            denominator.type == JessieCodeAstNodeType.VARIABLE &&
            textValue(numerator) == textValue(denominator)
        ) {
            return simplified(number(1.0, node.location))
        }
        if (
            numeratorValue != null &&
            numeratorValue != 0.0 &&
            denominatorValue == 0.0
        ) {
            return simplified(
                number(
                    if (numeratorValue > 0.0) {
                        Double.POSITIVE_INFINITY
                    } else {
                        Double.NEGATIVE_INFINITY
                    },
                    numerator.location,
                ),
            )
        }
        if (
            operationName(numerator) == "op_neg" &&
            operationName(denominator) == "op_neg"
        ) {
            val left = when (val result = nodeChild(numerator, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val right = when (val result = nodeChild(denominator, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return simplified(
                binary("op_div", left, right, node.location),
                revisit = true,
            )
        }
        if (
            operationName(numerator) == "op_neg" &&
            operationName(denominator) != "op_neg"
        ) {
            val operand = when (val result = nodeChild(numerator, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return simplified(
                unary(
                    "op_neg",
                    binary(
                        "op_div",
                        operand,
                        denominator,
                        node.location,
                    ),
                    node.location,
                ),
                revisit = true,
            )
        }
        if (
            operationName(numerator) != "op_neg" &&
            operationName(denominator) == "op_neg"
        ) {
            val operand = when (val result = nodeChild(denominator, 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return simplified(
                unary(
                    "op_neg",
                    binary(
                        "op_div",
                        numerator,
                        operand,
                        node.location,
                    ),
                    node.location,
                ),
                revisit = true,
            )
        }

        val numeratorPower = binaryChildrenOrNull(numerator, "op_exp")
        if (
            numeratorPower != null &&
            structurallyEqual(denominator, numeratorPower.first)
        ) {
            return simplified(
                binary(
                    "op_exp",
                    numeratorPower.first,
                    binary(
                        "op_sub",
                        numeratorPower.second,
                        number(1.0, node.location),
                        node.location,
                    ),
                    numerator.location,
                ),
                revisit = true,
            )
        }
        val numeratorProduct =
            binaryChildrenOrNull(numerator, "op_mul")
        if (
            denominator.type != JessieCodeAstNodeType.CONSTANT &&
            numeratorProduct?.first?.type ==
            JessieCodeAstNodeType.CONSTANT
        ) {
            return simplified(
                binary(
                    "op_mul",
                    numeratorProduct.first,
                    binary(
                        "op_div",
                        numeratorProduct.second,
                        denominator,
                        node.location,
                    ),
                    node.location,
                ),
                revisit = true,
            )
        }
        val denominatorPower =
            binaryChildrenOrNull(denominator, "op_exp")
        if (
            numeratorPower != null &&
            denominatorPower != null &&
            structurallyEqual(
                numeratorPower.first,
                denominatorPower.first,
            )
        ) {
            return simplified(
                binary(
                    "op_exp",
                    numeratorPower.first,
                    binary(
                        "op_sub",
                        numeratorPower.second,
                        denominatorPower.second,
                        node.location,
                    ),
                    numerator.location,
                ),
                revisit = true,
            )
        }
        return GMResult.Ok(Simplification(node))
    }

    private fun simplifyPower(
        node: JessieCodeAstNode,
    ): AlgebraResult<Simplification> {
        val pair = when (val result = binaryChildren(node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val base = pair.first
        val exponent = pair.second
        if (isNumber(exponent, 0.0)) {
            return simplified(number(1.0, exponent.location))
        }
        if (isNumber(exponent, 1.0)) {
            return simplified(base)
        }
        if (isNumber(base, 1.0)) {
            return simplified(base)
        }
        val exponentValue = numberValue(exponent)
        if (
            isNumber(base, 0.0) &&
            exponentValue != null &&
            exponentValue != 0.0
        ) {
            return simplified(base)
        }
        val nested = binaryChildrenOrNull(base, "op_exp")
        if (nested != null) {
            return simplified(
                binary(
                    "op_exp",
                    nested.first,
                    binary(
                        "op_mul",
                        nested.second,
                        exponent,
                        node.location,
                    ),
                    node.location,
                ),
            )
        }
        return GMResult.Ok(Simplification(node))
    }

    // JSXGraph: src/parser/ca.js -> simplifyElementary
    private fun simplifyElementary(
        node: JessieCodeAstNode,
    ): AlgebraResult<Simplification> {
        val function = when (val result = nodeChild(node, 0)) {
            is GMResult.Ok -> textValue(result.value)
            is GMResult.Err -> return result
        } ?: return GMResult.Ok(Simplification(node))
        val arguments = when (val result = nodeListChild(node, 1)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (arguments.isEmpty()) {
            return GMResult.Ok(Simplification(node))
        }
        val argument = arguments[0]
        return when (function) {
            "sin",
            "tan",
            -> {
                if (
                    isNumber(argument, 0.0) ||
                    (
                        argument.type ==
                            JessieCodeAstNodeType.VARIABLE &&
                            textValue(argument) == "PI"
                        ) ||
                    isIntegerMultipleOfPi(argument)
                ) {
                    simplified(number(0.0, node.location))
                } else {
                    GMResult.Ok(Simplification(node))
                }
            }
            "cos" -> when {
                isNumber(argument, 0.0) ->
                    simplified(number(1.0, node.location))
                argument.type == JessieCodeAstNodeType.VARIABLE &&
                    textValue(argument) == "PI" ->
                    simplified(
                        unary(
                            "op_neg",
                            number(1.0, node.location),
                            node.location,
                        ),
                    )
                else -> GMResult.Ok(Simplification(node))
            }
            "exp" -> if (isNumber(argument, 0.0)) {
                simplified(number(1.0, node.location))
            } else {
                GMResult.Ok(Simplification(node))
            }
            "pow" -> {
                if (arguments.size < 2) {
                    invalidAst(
                        node,
                        "pow simplification requires two arguments.",
                    )
                } else if (isNumber(arguments[1], 0.0)) {
                    simplified(number(1.0, node.location))
                } else {
                    GMResult.Ok(Simplification(node))
                }
            }
            else -> GMResult.Ok(Simplification(node))
        }
    }

    private fun isIntegerMultipleOfPi(
        node: JessieCodeAstNode,
    ): Boolean {
        val pair = binaryChildrenOrNull(node, "op_mul") ?: return false
        val coefficient = numberValue(pair.first) ?: return false
        return coefficient % 1.0 == 0.0 &&
            pair.second.type == JessieCodeAstNodeType.VARIABLE &&
            textValue(pair.second) == "PI"
    }

    private fun reciprocalDenominator(
        node: JessieCodeAstNode,
    ): JessieCodeAstNode? {
        val pair = binaryChildrenOrNull(node, "op_div") ?: return null
        return if (isNumber(pair.first, 1.0)) pair.second else null
    }

    private fun binaryChildrenOrNull(
        node: JessieCodeAstNode,
        operation: String,
    ): Pair<JessieCodeAstNode, JessieCodeAstNode>? {
        if (
            operationName(node) != operation ||
            node.children.size < 2
        ) {
            return null
        }
        val first =
            (node.children[0] as? JessieCodeAstChild.Node)?.value
                ?: return null
        val second =
            (node.children[1] as? JessieCodeAstChild.Node)?.value
                ?: return null
        return first to second
    }

    private fun binaryChildren(
        node: JessieCodeAstNode,
    ): AlgebraResult<Pair<JessieCodeAstNode, JessieCodeAstNode>> {
        val first = when (val result = nodeChild(node, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val second = when (val result = nodeChild(node, 1)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(first to second)
    }

    private fun nodeChild(
        node: JessieCodeAstNode,
        index: Int,
    ): AlgebraResult<JessieCodeAstNode> {
        val child = node.children.getOrNull(index)
        return if (child is JessieCodeAstChild.Node) {
            GMResult.Ok(child.value)
        } else {
            invalidAst(node, "Child $index must be an AST node.")
        }
    }

    private fun nodeListChild(
        node: JessieCodeAstNode,
        index: Int,
    ): AlgebraResult<List<JessieCodeAstNode>> {
        val child = node.children.getOrNull(index)
        return if (child is JessieCodeAstChild.NodeList) {
            GMResult.Ok(child.value)
        } else {
            invalidAst(node, "Child $index must be an AST node list.")
        }
    }

    private fun textListChild(
        node: JessieCodeAstNode,
        index: Int,
    ): AlgebraResult<List<String>> {
        val child = node.children.getOrNull(index)
        return if (child is JessieCodeAstChild.TextList) {
            GMResult.Ok(child.value)
        } else {
            invalidAst(node, "Child $index must be a text list.")
        }
    }

    private fun nodeValue(child: JessieCodeAstChild?): String? =
        (child as? JessieCodeAstChild.Node)?.value?.let(::textValue)

    private fun textValue(node: JessieCodeAstNode): String? =
        (node.value as? JessieCodeAstValue.Text)?.value

    private fun numberValue(node: JessieCodeAstNode): Double? =
        (node.value as? JessieCodeAstValue.Number)?.value

    private fun operationName(node: JessieCodeAstNode?): String? =
        if (node?.type == JessieCodeAstNodeType.OPERATION) {
            textValue(node)
        } else {
            null
        }

    private fun isNumber(
        node: JessieCodeAstNode,
        value: Double,
    ): Boolean =
        node.type == JessieCodeAstNodeType.CONSTANT &&
            numberValue(node) == value

    private fun number(
        value: Double,
        location: JessieCodeAstLocation,
    ): JessieCodeAstNode =
        JessieCodeAstNode(
            type = JessieCodeAstNodeType.CONSTANT,
            value = JessieCodeAstValue.Number(value),
            children = emptyList(),
            location = location,
            isMath = true,
        )

    private fun variable(
        name: String,
        location: JessieCodeAstLocation,
    ): JessieCodeAstNode =
        JessieCodeAstNode(
            type = JessieCodeAstNodeType.VARIABLE,
            value = JessieCodeAstValue.Text(name),
            children = emptyList(),
            location = location,
        )

    private fun operation(
        name: String,
        children: List<JessieCodeAstChild>,
        location: JessieCodeAstLocation,
        isMath: Boolean? = if (name in MATH_OPERATIONS) true else null,
    ): JessieCodeAstNode =
        JessieCodeAstNode(
            type = JessieCodeAstNodeType.OPERATION,
            value = JessieCodeAstValue.Text(name),
            children = children,
            location = location,
            isMath = isMath,
        )

    private fun unary(
        name: String,
        operand: JessieCodeAstNode,
        location: JessieCodeAstLocation,
    ): JessieCodeAstNode =
        operation(
            name = name,
            children = listOf(JessieCodeAstChild.Node(operand)),
            location = location,
        )

    private fun binary(
        name: String,
        left: JessieCodeAstNode,
        right: JessieCodeAstNode,
        location: JessieCodeAstLocation,
    ): JessieCodeAstNode =
        operation(
            name = name,
            children = listOf(
                JessieCodeAstChild.Node(left),
                JessieCodeAstChild.Node(right),
            ),
            location = location,
        )

    private fun call(
        functionName: String,
        arguments: List<JessieCodeAstNode>,
        location: JessieCodeAstLocation,
    ): JessieCodeAstNode =
        operation(
            name = "op_execfun",
            children = listOf(
                JessieCodeAstChild.Node(
                    variable(functionName, location),
                ),
                JessieCodeAstChild.NodeList(arguments),
            ),
            location = location,
        )

    private fun structurallyEqual(
        first: JessieCodeAstNode,
        second: JessieCodeAstNode,
    ): Boolean {
        if (
            first.type != second.type ||
            first.value != second.value ||
            first.children.size != second.children.size
        ) {
            return false
        }
        return first.children.indices.all { index ->
            structurallyEqual(
                first.children[index],
                second.children[index],
            )
        }
    }

    private fun structurallyEqual(
        first: JessieCodeAstChild,
        second: JessieCodeAstChild,
    ): Boolean =
        when {
            first is JessieCodeAstChild.Node &&
                second is JessieCodeAstChild.Node ->
                structurallyEqual(first.value, second.value)
            first is JessieCodeAstChild.NodeList &&
                second is JessieCodeAstChild.NodeList ->
                first.value.size == second.value.size &&
                    first.value.indices.all { index ->
                        structurallyEqual(
                            first.value[index],
                            second.value[index],
                        )
                    }
            else -> first == second
        }

    private fun validateOutput(
        root: JessieCodeAstNode,
    ): AlgebraResult<Unit> {
        val pending = mutableListOf(root to 1)
        var nodeCount = 0L
        while (pending.isNotEmpty()) {
            val (node, depth) = pending.removeAt(pending.lastIndex)
            nodeCount += 1L
            if (nodeCount > limits.maxOutputNodes.toLong()) {
                return GMResult.Err(
                    JessieCodeComputerAlgebraError
                        .OutputNodeLimitExceeded(
                            limit = limits.maxOutputNodes,
                            requestedSize = nodeCount,
                            location = node.location,
                        ),
                )
            }
            if (depth > limits.maxOutputDepth) {
                return GMResult.Err(
                    JessieCodeComputerAlgebraError
                        .OutputDepthLimitExceeded(
                            limit = limits.maxOutputDepth,
                            requestedDepth = depth,
                            location = node.location,
                        ),
                )
            }
            for (child in node.children) {
                when (child) {
                    is JessieCodeAstChild.Node ->
                        pending += child.value to depth + 1
                    is JessieCodeAstChild.NodeList ->
                        child.value.forEach {
                            pending += it to depth + 1
                        }
                    else -> Unit
                }
            }
        }
        return GMResult.Ok(Unit)
    }

    private fun tick(
        location: JessieCodeAstLocation,
    ): JessieCodeComputerAlgebraError? {
        transformationSteps += 1
        return if (
            transformationSteps > limits.maxTransformationSteps
        ) {
            JessieCodeComputerAlgebraError
                .TransformationStepLimitExceeded(
                    limit = limits.maxTransformationSteps,
                    location = location,
                )
        } else {
            null
        }
    }

    private fun invalidAst(
        node: JessieCodeAstNode,
        reason: String,
    ): GMResult.Err<JessieCodeComputerAlgebraError.InvalidAst> =
        GMResult.Err(
            JessieCodeComputerAlgebraError.InvalidAst(
                reason = reason,
                location = node.location,
            ),
        )

    private fun simplified(
        node: JessieCodeAstNode,
        revisit: Boolean = false,
    ): AlgebraResult<Simplification> =
        GMResult.Ok(Simplification(node, revisit))

    private companion object {
        const val LOG2_FACTOR = 1.4426950408889634
        const val LOG10_FACTOR = 0.43429448190325176

        val MATH_OPERATIONS = setOf(
            "op_add",
            "op_sub",
            "op_mul",
            "op_div",
            "op_neg",
            "op_execfun",
            "op_exp",
        )
    }
}
