/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> execute, add, sub, neg, mul, div,
 * mod, pow, object literal operations, and defineFunction
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.utils.JsMath
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.E
import kotlin.math.PI
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
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

internal class JessieCodeEvaluator(
    private val limits: JessieCodeEvaluatorLimits =
        JessieCodeEvaluatorLimits(),
) {
    // JSXGraph: src/parser/jessiecode.js -> execute
    internal fun evaluate(
        node: JessieCodeAstNode,
        environment: JessieCodeRuntimeEnvironment =
            JessieCodeRuntimeEnvironment(),
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        EvaluationState(
            limits = limits,
            environment = environment,
        ).evaluateRoot(node)
}

internal class JessieCodeEvaluationSession(
    limits: JessieCodeEvaluatorLimits = JessieCodeEvaluatorLimits(),
    environment: JessieCodeRuntimeEnvironment =
        JessieCodeRuntimeEnvironment(),
) {
    private val state = EvaluationState(
        limits = limits,
        environment = environment,
    )

    internal fun evaluate(
        node: JessieCodeAstNode,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        state.evaluateRoot(node)
}

private class EvaluationState(
    private val limits: JessieCodeEvaluatorLimits,
    private val environment: JessieCodeRuntimeEnvironment,
) {
    private var evaluationSteps = 0
    private var functionCallDepth = 0
    private var currentBoard = environment.board
    private var currentScope = RuntimeScope(
        parameters = emptyList(),
        locals = environment.variables.toMutableMap(),
        previous = null,
    )

    fun evaluateRoot(
        node: JessieCodeAstNode,
    ): EvaluationResult {
        if (
            limits.maxEvaluationSteps < 1 ||
            limits.maxEvaluationDepth
                !in 1..MAX_SUPPORTED_EVALUATION_DEPTH ||
            limits.maxCollectionSize < 1
        ) {
            return GMResult.Err(
                JessieCodeRuntimeError.InvalidLimits(
                    maxEvaluationSteps = limits.maxEvaluationSteps,
                    maxEvaluationDepth = limits.maxEvaluationDepth,
                    maxCollectionSize = limits.maxCollectionSize,
                ),
            )
        }
        evaluationSteps = 0
        functionCallDepth = 0
        return evaluate(node)
    }

    fun evaluate(
        node: JessieCodeAstNode,
        depth: Int = 1,
        functionPosition: Boolean = false,
    ): EvaluationResult {
        when (val result = enterNode(node, depth)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }

        return when (node.type) {
            JessieCodeAstNodeType.OPERATION ->
                evaluateOperation(node, depth)

            JessieCodeAstNodeType.VARIABLE ->
                evaluateVariable(node, functionPosition)

            JessieCodeAstNodeType.CONSTANT ->
                evaluateConstant(node)

            JessieCodeAstNodeType.BOOLEAN_CONSTANT ->
                evaluateBoolean(node)

            JessieCodeAstNodeType.STRING ->
                evaluateString(node)
        }
    }

    private fun enterNode(
        node: JessieCodeAstNode,
        depth: Int,
    ): GMResult<Unit, JessieCodeRuntimeError> {
        if (depth > limits.maxEvaluationDepth) {
            return GMResult.Err(
                JessieCodeRuntimeError.EvaluationDepthLimitExceeded(
                    limit = limits.maxEvaluationDepth,
                    location = node.location,
                ),
            )
        }
        evaluationSteps += 1
        if (evaluationSteps > limits.maxEvaluationSteps) {
            return GMResult.Err(
                JessieCodeRuntimeError.EvaluationStepLimitExceeded(
                    limit = limits.maxEvaluationSteps,
                    location = node.location,
                ),
            )
        }
        return GMResult.Ok(Unit)
    }

    private fun evaluateOperation(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        val operator = when (val value = node.value) {
            is JessieCodeAstValue.Text -> value.value
            else -> return invalidAst(
                node,
                "Operation value must be text.",
            )
        }

        return when (operator) {
            "op_none" -> evaluateSequence(node, depth)
            "op_block" -> evaluateNodeChild(node, 0, depth)
            "op_if" -> evaluateIf(node, depth, hasElse = false)
            "op_if_else" -> evaluateIf(node, depth, hasElse = true)
            "op_while" -> evaluateWhile(node, depth)
            "op_do" -> evaluateDoWhile(node, depth)
            "op_for" -> evaluateFor(node, depth)
            "op_return" -> evaluateReturn(node, depth)
            "op_use" -> evaluateUse(node)
            "op_delete" -> evaluateDelete(node)
            "op_function" -> evaluateFunction(node, isMap = false)
            "op_map" -> evaluateFunction(node, isMap = true)
            "op_assign" -> evaluateAssignment(node, depth)
            "op_array" -> evaluateArray(node, depth)
            "op_emptyobject" -> evaluateEmptyObject(node)
            "op_proplst_val" -> evaluateObject(node, depth)
            "op_proplst",
            "op_prop",
            -> invalidAst(
                node,
                "$operator must be evaluated inside op_proplst_val.",
            )
            "op_extvalue" -> evaluateIndex(node, depth)
            "op_execfun" -> evaluateCall(node, depth)
            "op_property" -> evaluateProperty(node, depth)
            "op_conditional" -> evaluateConditional(node, depth)
            "op_or" -> evaluateLogicalOr(node, depth)
            "op_and" -> evaluateLogicalAnd(node, depth)
            "op_not" -> evaluateUnary(node, depth) {
                GMResult.Ok(
                    JessieCodeRuntimeValue.BooleanValue(!isTruthy(it)),
                )
            }
            "op_neg" -> evaluateUnary(node, depth) {
                negate(it, node)
            }
            "op_eq" -> evaluateBinary(node, depth) { left, right ->
                GMResult.Ok(
                    JessieCodeRuntimeValue.BooleanValue(
                        looselyEqual(left, right),
                    ),
                )
            }
            "op_neq" -> evaluateBinary(node, depth) { left, right ->
                GMResult.Ok(
                    JessieCodeRuntimeValue.BooleanValue(
                        !looselyEqual(left, right),
                    ),
                )
            }
            "op_approx" -> evaluateBinary(node, depth) { left, right ->
                GMResult.Ok(
                    JessieCodeRuntimeValue.BooleanValue(
                        abs(toNumber(left) - toNumber(right)) < Mat.eps,
                    ),
                )
            }
            "op_lt" -> comparison(node, depth, Comparison.LESS)
            "op_gt" -> comparison(node, depth, Comparison.GREATER)
            "op_leq" -> comparison(node, depth, Comparison.LESS_OR_EQUAL)
            "op_geq" -> comparison(node, depth, Comparison.GREATER_OR_EQUAL)
            "op_add" -> evaluateBinary(node, depth) { left, right ->
                add(left, right, node)
            }
            "op_sub" -> evaluateBinary(node, depth) { left, right ->
                subtract(left, right, node)
            }
            "op_mul" -> evaluateBinary(node, depth) { left, right ->
                multiply(left, right, node)
            }
            "op_div" -> evaluateBinary(node, depth) { left, right ->
                divide(left, right, node)
            }
            "op_mod" -> evaluateBinary(node, depth) { left, right ->
                modulo(left, right, node)
            }
            "op_exp" -> evaluateBinary(node, depth) { left, right ->
                power(left, right, node)
            }
            else -> GMResult.Err(
                JessieCodeRuntimeError.UnsupportedOperation(
                    operator = operator,
                    operandTypes = emptyList(),
                    location = node.location,
                ),
            )
        }
    }

    private fun evaluateSequence(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        var result: JessieCodeRuntimeValue =
            JessieCodeRuntimeValue.NumberValue(0.0)
        for (child in node.children) {
            val childNode = when (child) {
                is JessieCodeAstChild.Node -> child.value
                else -> return invalidAst(
                    node,
                    "op_none children must be nodes.",
                )
            }
            result = when (
                val childResult = evaluate(childNode, depth + 1)
            ) {
                is GMResult.Ok -> childResult.value
                is GMResult.Err -> return childResult
            }
        }
        return GMResult.Ok(result)
    }

    private fun evaluateIf(
        node: JessieCodeAstNode,
        depth: Int,
        hasElse: Boolean,
    ): EvaluationResult {
        val condition = when (
            val result = evaluateNodeChild(node, 0, depth)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (isTruthy(condition)) {
            return evaluateNodeChild(node, 1, depth)
        }
        return if (hasElse) {
            evaluateNodeChild(node, 2, depth)
        } else {
            GMResult.Ok(JessieCodeRuntimeValue.NumberValue(0.0))
        }
    }

    private fun evaluateWhile(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        while (true) {
            val condition = when (
                val result = evaluateNodeChild(node, 0, depth)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (!isTruthy(condition)) {
                return GMResult.Ok(
                    JessieCodeRuntimeValue.NumberValue(0.0),
                )
            }
            when (val result = evaluateNodeChild(node, 1, depth)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
    }

    private fun evaluateDoWhile(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        while (true) {
            when (val result = evaluateNodeChild(node, 0, depth)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            val condition = when (
                val result = evaluateNodeChild(node, 1, depth)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (!isTruthy(condition)) {
                return GMResult.Ok(
                    JessieCodeRuntimeValue.NumberValue(0.0),
                )
            }
        }
    }

    private fun evaluateFor(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        when (val result = evaluateNodeChild(node, 0, depth)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        while (true) {
            val condition = when (
                val result = evaluateNodeChild(node, 1, depth)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (!isTruthy(condition)) {
                return GMResult.Ok(
                    JessieCodeRuntimeValue.NumberValue(0.0),
                )
            }
            when (val result = evaluateNodeChild(node, 3, depth)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (val result = evaluateNodeChild(node, 2, depth)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
    }

    private fun evaluateReturn(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        val child = node.children.firstOrNull()
            ?: return invalidAst(
                node,
                "op_return must contain one child.",
            )
        return when (child) {
            is JessieCodeAstChild.Node ->
                evaluate(child.value, depth + 1)
            JessieCodeAstChild.Undefined ->
                GMResult.Ok(JessieCodeRuntimeValue.NumberValue(0.0))
            else -> invalidAst(
                node,
                "op_return child must be a node or undefined.",
            )
        }
    }

    // JSXGraph: src/parser/jessiecode.js -> execute(op_use), use
    private fun evaluateUse(
        node: JessieCodeAstNode,
    ): EvaluationResult {
        val container = when (val result = textChild(node, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val board = environment.boardsByContainer[container]
            ?: return GMResult.Err(
                JessieCodeRuntimeError.BoardNotFound(
                    container = container,
                    location = node.location,
                ),
            )
        currentBoard = board
        return GMResult.Ok(JessieCodeRuntimeValue.NumberValue(0.0))
    }

    private fun evaluateDelete(
        node: JessieCodeAstNode,
    ): EvaluationResult {
        val name = when (val result = textChild(node, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val local = localScope(name)
        val element = if (local != null) {
            (
                local.locals.getValue(name) as?
                    JessieCodeRuntimeValue.ElementReference
                )?.element
        } else {
            currentBoard?.select(name)
        }
        if (element != null) {
            currentBoard?.removeObject(element)
        }
        return GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
    }

    // JSXGraph: src/parser/jessiecode.js -> defineFunction
    private fun evaluateFunction(
        node: JessieCodeAstNode,
        isMap: Boolean,
    ): EvaluationResult {
        val parameters = when (val result = textListChild(node, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val body = when (val result = nodeChild(node, 1)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            isMap &&
            body.isMath != true &&
            body.type != JessieCodeAstNodeType.VARIABLE
        ) {
            return GMResult.Err(
                JessieCodeRuntimeError.InvalidMapBody(
                    location = body.location,
                ),
            )
        }

        val functionScope = RuntimeScope(
            parameters = parameters,
            locals = mutableMapOf(),
            previous = currentScope,
        )
        // The interpreter's defineFunction calls pushScope and intentionally
        // leaves that function scope current when compile mode is disabled.
        currentScope = functionScope

        val dependencies = currentBoard?.let { board ->
            when (
                val result = JessieCodeDependencyCollector(board).collect(
                    node = body,
                    parameterNames = parameters.toSet(),
                    localNames = visibleLocalNames(),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return GMResult.Err(
                        JessieCodeRuntimeError.FunctionDependency(
                            error = result.error,
                            location = node.location,
                        ),
                    )
                }
            }
        } ?: emptyMap()

        val callable = JessieCodeCallable { arguments, location ->
            callFunction(
                scope = functionScope,
                parameters = parameters,
                body = body,
                arguments = arguments,
                location = location,
            )
        }
        return GMResult.Ok(
            JessieCodeRuntimeValue.FunctionValue(
                name = if (isMap) "map" else "function",
                callable = callable,
                parameterNames = parameters.toList(),
                isMap = isMap,
                dependencies = dependencies.toMap(),
            ),
        )
    }

    private fun callFunction(
        scope: RuntimeScope,
        parameters: List<String>,
        body: JessieCodeAstNode,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): EvaluationResult {
        if (functionCallDepth >= limits.maxEvaluationDepth) {
            return GMResult.Err(
                JessieCodeRuntimeError.EvaluationDepthLimitExceeded(
                    limit = limits.maxEvaluationDepth,
                    location = location,
                ),
            )
        }

        val previousScope = currentScope
        currentScope = scope
        for ((index, parameter) in parameters.withIndex()) {
            scope.locals[parameter] = arguments.getOrElse(index) {
                JessieCodeRuntimeValue.UndefinedValue
            }
        }
        functionCallDepth += 1
        val result = evaluate(body)
        functionCallDepth -= 1
        currentScope = previousScope
        return result
    }

    private fun evaluateArray(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        val elements = when (
            val result = nodeListChild(node, index = 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (elements.size > limits.maxCollectionSize) {
            return GMResult.Err(
                JessieCodeRuntimeError.CollectionSizeLimitExceeded(
                    limit = limits.maxCollectionSize,
                    requestedSize = elements.size.toLong(),
                    location = node.location,
                ),
            )
        }
        val values = mutableListOf<JessieCodeRuntimeValue>()
        for (element in elements) {
            when (val result = evaluate(element, depth + 1)) {
                is GMResult.Ok -> values += result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(JessieCodeRuntimeValue.ArrayValue(values))
    }

    private fun evaluateAssignment(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        val left = when (val result = nodeChild(node, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val target = when (
            val result = resolveAssignmentTarget(left, depth + 1)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val assignmentScope = currentScope
        val previousCreatorName = assignmentScope.implicitCreatorName
        assignmentScope.implicitCreatorName = target.implicitCreatorName
        val valueResult = evaluateNodeChild(node, 1, depth)
        assignmentScope.implicitCreatorName = previousCreatorName
        val value = when (valueResult) {
            is GMResult.Ok -> valueResult.value
            is GMResult.Err -> return valueResult
        }
        return when (
            val result = assign(
                target = target,
                value = value,
                location = node.location,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(value)
            is GMResult.Err -> result
        }
    }

    private fun resolveAssignmentTarget(
        node: JessieCodeAstNode,
        depth: Int,
    ): GMResult<AssignmentTarget, JessieCodeRuntimeError> {
        when (val result = enterNode(node, depth)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        return when {
            node.type == JessieCodeAstNodeType.VARIABLE -> {
                val name = (node.value as? JessieCodeAstValue.Text)?.value
                    ?: return invalidAst(
                        node,
                        "Assignment variable value must be text.",
                    )
                GMResult.Ok(
                    AssignmentTarget.Variable(
                        scope = currentScope,
                        name = name,
                    ),
                )
            }
            operationName(node) == "op_property" -> {
                val receiver = when (
                    val result = evaluateNodeChild(node, 0, depth)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val property = when (val result = textChild(node, 1)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                GMResult.Ok(
                    AssignmentTarget.Property(receiver, property),
                )
            }
            operationName(node) == "op_extvalue" -> {
                val receiver = when (
                    val result = evaluateNodeChild(node, 0, depth)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val index = when (
                    val result = evaluateNodeChild(node, 1, depth)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                GMResult.Ok(
                    AssignmentTarget.Property(
                        receiver = receiver,
                        property = propertyKey(index),
                    ),
                )
            }
            else -> GMResult.Err(
                JessieCodeRuntimeError.InvalidAssignmentTarget(
                    targetType = assignmentTargetType(node),
                    location = node.location,
                ),
            )
        }
    }

    private fun assign(
        target: AssignmentTarget,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Unit, JessieCodeRuntimeError> =
        when (target) {
            is AssignmentTarget.Variable -> {
                target.scope.locals[target.name] = value
                GMResult.Ok(Unit)
            }
            is AssignmentTarget.Property -> assignProperty(
                receiver = target.receiver,
                property = target.property,
                value = value,
                location = location,
            )
        }

    private fun assignProperty(
        receiver: JessieCodeRuntimeValue,
        property: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Unit, JessieCodeRuntimeError> =
        when (receiver) {
            is JessieCodeRuntimeValue.ObjectValue -> {
                if (
                    property !in receiver.properties &&
                    receiver.properties.size >= limits.maxCollectionSize
                ) {
                    GMResult.Err(
                        JessieCodeRuntimeError
                            .CollectionSizeLimitExceeded(
                                limit = limits.maxCollectionSize,
                                requestedSize =
                                    receiver.properties.size.toLong() + 1L,
                                location = location,
                            ),
                    )
                } else {
                    receiver.properties[property] = value
                    GMResult.Ok(Unit)
                }
            }
            is JessieCodeRuntimeValue.ArrayValue ->
                assignArrayProperty(
                    array = receiver,
                    property = property,
                    value = value,
                    location = location,
                )
            else -> GMResult.Err(
                JessieCodeRuntimeError.AssignmentTargetUnavailable(
                    receiverType = typeName(receiver),
                    property = property,
                    location = location,
                ),
            )
        }

    private fun assignArrayProperty(
        array: JessieCodeRuntimeValue.ArrayValue,
        property: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Unit, JessieCodeRuntimeError> {
        if (property == "length") {
            val length = toNumber(value)
            if (
                !length.isFinite() ||
                length < 0.0 ||
                floor(length) != length ||
                length > MAX_JAVASCRIPT_ARRAY_LENGTH.toDouble()
            ) {
                return assignmentUnavailable(
                    receiver = array,
                    property = property,
                    location = location,
                )
            }
            val requestedSize = length.toLong()
            if (requestedSize > limits.maxCollectionSize.toLong()) {
                return GMResult.Err(
                    JessieCodeRuntimeError.CollectionSizeLimitExceeded(
                        limit = limits.maxCollectionSize,
                        requestedSize = requestedSize,
                        location = location,
                    ),
                )
            }
            return resizeArray(array, requestedSize.toInt())
        }

        val index = arrayIndex(property)
        if (index != null) {
            val requestedSize = index + 1L
            if (requestedSize > limits.maxCollectionSize.toLong()) {
                return GMResult.Err(
                    JessieCodeRuntimeError.CollectionSizeLimitExceeded(
                        limit = limits.maxCollectionSize,
                        requestedSize = requestedSize,
                        location = location,
                    ),
                )
            }
            val targetIndex = index.toInt()
            while (array.values.size.toLong() < requestedSize) {
                array.values += JessieCodeRuntimeValue.UndefinedValue
            }
            array.values[targetIndex] = value
            return GMResult.Ok(Unit)
        }

        if (
            property !in array.properties &&
            array.properties.size >= limits.maxCollectionSize
        ) {
            return GMResult.Err(
                JessieCodeRuntimeError.CollectionSizeLimitExceeded(
                    limit = limits.maxCollectionSize,
                    requestedSize =
                        array.properties.size.toLong() + 1L,
                    location = location,
                ),
            )
        }
        array.properties[property] = value
        return GMResult.Ok(Unit)
    }

    private fun resizeArray(
        array: JessieCodeRuntimeValue.ArrayValue,
        requestedSize: Int,
    ): GMResult<Unit, JessieCodeRuntimeError> {
        while (array.values.size > requestedSize) {
            array.values.removeAt(array.values.lastIndex)
        }
        while (array.values.size < requestedSize) {
            array.values += JessieCodeRuntimeValue.UndefinedValue
        }
        return GMResult.Ok(Unit)
    }

    private fun evaluateEmptyObject(
        node: JessieCodeAstNode,
    ): EvaluationResult {
        if (
            node.children.size != 1 ||
            node.children[0] !== JessieCodeAstChild.EmptyObject
        ) {
            return invalidAst(
                node,
                "op_emptyobject must contain the upstream empty-object child.",
            )
        }
        return GMResult.Ok(
            JessieCodeRuntimeValue.ObjectValue(emptyMap()),
        )
    }

    private fun evaluateObject(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        val propertyList = when (val result = nodeChild(node, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val properties =
            linkedMapOf<String, JessieCodeRuntimeValue>()
        when (
            val result = collectObjectProperties(
                node = propertyList,
                depth = depth + 1,
                properties = properties,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            JessieCodeRuntimeValue.ObjectValue(properties.toMap()),
        )
    }

    private fun collectObjectProperties(
        node: JessieCodeAstNode,
        depth: Int,
        properties: MutableMap<String, JessieCodeRuntimeValue>,
    ): GMResult<Unit, JessieCodeRuntimeError> {
        when (val result = enterNode(node, depth)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val operator = (node.value as? JessieCodeAstValue.Text)?.value
            ?: return invalidAst(
                node,
                "Object property operation value must be text.",
            )
        return when (operator) {
            "op_proplst" -> {
                val left = when (val result = nodeChild(node, 0)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val right = when (val result = nodeChild(node, 1)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (
                    val result = collectObjectProperties(
                        node = left,
                        depth = depth + 1,
                        properties = properties,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                collectObjectProperties(
                    node = right,
                    depth = depth + 1,
                    properties = properties,
                )
            }
            "op_prop" -> {
                if (node.children.size != 2) {
                    return invalidAst(
                        node,
                        "op_prop must contain a name and value.",
                    )
                }
                val propertyName = when (
                    val child = node.children[0]
                ) {
                    is JessieCodeAstChild.Text -> child.value
                    is JessieCodeAstChild.Node -> {
                        if (
                            child.value.type !=
                            JessieCodeAstNodeType.STRING &&
                            child.value.type !=
                            JessieCodeAstNodeType.CONSTANT
                        ) {
                            return invalidAst(
                                node,
                                "Object property node must be a " +
                                    "string or number literal.",
                            )
                        }
                        // JSXGraph stores the AST node itself as the key.
                        // JavaScript converts that object to this string.
                        "[object Object]"
                    }
                    else -> return invalidAst(
                        node,
                        "Object property name has an invalid shape.",
                    )
                }
                val valueNode = when (val result = nodeChild(node, 1)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val value = when (
                    val result = evaluate(valueNode, depth + 1)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (
                    propertyName !in properties &&
                    properties.size >= limits.maxCollectionSize
                ) {
                    return GMResult.Err(
                        JessieCodeRuntimeError
                            .CollectionSizeLimitExceeded(
                                limit = limits.maxCollectionSize,
                                requestedSize =
                                    properties.size.toLong() + 1L,
                                location = node.location,
                            ),
                    )
                }
                properties[propertyName] = value
                GMResult.Ok(Unit)
            }
            else -> invalidAst(
                node,
                "Object property list contains $operator.",
            )
        }
    }

    private fun evaluateIndex(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        val receiver = when (
            val result = evaluateNodeChild(node, 0, depth)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val index = when (
            val result = evaluateNodeChild(node, 1, depth)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val numericIndex = (
            index as? JessieCodeRuntimeValue.NumberValue
        )?.value ?: return GMResult.Ok(
            JessieCodeRuntimeValue.UndefinedValue,
        )
        if (
            !numericIndex.isFinite() ||
            abs(JsMath.round(numericIndex) - numericIndex) >=
            INDEX_INTEGER_TOLERANCE
        ) {
            return GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
        }

        return GMResult.Ok(index(receiver, numericIndex))
    }

    private fun evaluateCall(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        val attributeNodes =
            if (node.children.size > CALL_ARGUMENT_CHILD_COUNT) {
                if (
                    node.children.size !=
                    CALL_WITH_ATTRIBUTES_CHILD_COUNT ||
                    (
                        node.children[3] as?
                            JessieCodeAstChild.BooleanFlag
                        )?.value != true
                ) {
                    return invalidAst(
                        node,
                        "Attributed op_execfun has an invalid shape.",
                    )
                }
                when (val result = nodeListChild(node, 2)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            } else {
                if (node.children.size != CALL_ARGUMENT_CHILD_COUNT) {
                    return invalidAst(
                        node,
                        "op_execfun must contain a function and arguments.",
                    )
                }
                null
            }
        val attributes = if (attributeNodes != null) {
            when (
                val result = evaluateCreatorAttributes(
                    nodes = attributeNodes,
                    depth = depth,
                    location = node.location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        } else {
            null
        }

        val functionNode = when (val result = nodeChild(node, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val argumentNodes = when (
            val result = nodeListChild(node, 1)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val function = when (
            val result = evaluate(
                node = functionNode,
                depth = depth + 1,
                functionPosition = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val callable = function as? JessieCodeRuntimeValue.FunctionValue
            ?: return GMResult.Err(
                JessieCodeRuntimeError.NotCallable(
                    valueType = typeName(function),
                    location = node.location,
                ),
            )
        if (attributes != null && callable.creator == null) {
            return GMResult.Err(
                JessieCodeRuntimeError.UnexpectedCreatorAttributes(
                    functionName = callable.name,
                    location = node.location,
                ),
            )
        }

        val arguments = mutableListOf<JessieCodeRuntimeValue>()
        for (argumentNode in argumentNodes) {
            when (val result = evaluate(argumentNode, depth + 1)) {
                is GMResult.Ok -> arguments += result.value
                is GMResult.Err -> return result
            }
        }

        val creatorAttributes = attributes
            ?: JessieCodeRuntimeValue.ObjectValue(emptyMap())
        if (
            callable.creator != null &&
            isUndefinedAttribute(creatorAttributes, "name") &&
            isUndefinedAttribute(creatorAttributes, "id")
        ) {
            currentScope.implicitCreatorName?.let {
                creatorAttributes.properties["name"] =
                    JessieCodeRuntimeValue.StringValue(it)
            }
        }
        return callable.creator?.create(
            board = currentBoard,
            parents = arguments,
            attributes = creatorAttributes,
            location = node.location,
        ) ?: callable.callable.call(arguments, node.location)
    }

    private fun isUndefinedAttribute(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
    ): Boolean =
        attributes.properties[name] == null ||
            attributes.properties[name] ===
            JessieCodeRuntimeValue.UndefinedValue

    private fun evaluateCreatorAttributes(
        nodes: List<JessieCodeAstNode>,
        depth: Int,
        location: JessieCodeAstLocation,
    ): GMResult<
        JessieCodeRuntimeValue.ObjectValue,
        JessieCodeRuntimeError,
        > {
        val attributes = JessieCodeRuntimeValue.ObjectValue(emptyMap())
        for (attributeNode in nodes) {
            val attribute = when (
                val result = evaluate(attributeNode, depth + 1)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            when (
                val result = mergeAttributeValue(
                    target = attributes,
                    source = attribute,
                    depth = 1,
                    location = location,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(attributes)
    }

    // JSXGraph: src/utils/type.js -> deepCopy(..., toLower=true)
    private fun mergeAttributeValue(
        target: JessieCodeRuntimeValue.ObjectValue,
        source: JessieCodeRuntimeValue,
        depth: Int,
        location: JessieCodeAstLocation,
    ): GMResult<Unit, JessieCodeRuntimeError> {
        if (depth > limits.maxEvaluationDepth) {
            return GMResult.Err(
                JessieCodeRuntimeError.EvaluationDepthLimitExceeded(
                    limit = limits.maxEvaluationDepth,
                    location = location,
                ),
            )
        }
        val entries = when (source) {
            is JessieCodeRuntimeValue.ObjectValue ->
                source.properties.entries.map { it.key to it.value }
            is JessieCodeRuntimeValue.ArrayValue ->
                source.values.mapIndexed { index, value ->
                    index.toString() to value
                } + source.properties.entries.map {
                    it.key to it.value
                }
            else -> return GMResult.Ok(Unit)
        }

        for ((sourceKey, sourceValue) in entries) {
            val key = sourceKey.lowercase()
            if (
                key !in target.properties &&
                target.properties.size >= limits.maxCollectionSize
            ) {
                return GMResult.Err(
                    JessieCodeRuntimeError.CollectionSizeLimitExceeded(
                        limit = limits.maxCollectionSize,
                        requestedSize =
                            target.properties.size.toLong() + 1L,
                        location = location,
                    ),
                )
            }
            val existing = target.properties[key]
            if (
                existing is JessieCodeRuntimeValue.ObjectValue &&
                sourceValue is JessieCodeRuntimeValue.ObjectValue
            ) {
                when (
                    val result = mergeAttributeValue(
                        target = existing,
                        source = sourceValue,
                        depth = depth + 1,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            } else {
                target.properties[key] = when (
                    val result = copyAttributeValue(
                        value = sourceValue,
                        depth = depth + 1,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }
        }
        return GMResult.Ok(Unit)
    }

    private fun copyAttributeValue(
        value: JessieCodeRuntimeValue,
        depth: Int,
        location: JessieCodeAstLocation,
    ): EvaluationResult {
        if (depth > limits.maxEvaluationDepth) {
            return GMResult.Err(
                JessieCodeRuntimeError.EvaluationDepthLimitExceeded(
                    limit = limits.maxEvaluationDepth,
                    location = location,
                ),
            )
        }
        return when (value) {
            is JessieCodeRuntimeValue.ObjectValue -> {
                val copy = JessieCodeRuntimeValue.ObjectValue(emptyMap())
                when (
                    val result = mergeAttributeValue(
                        target = copy,
                        source = value,
                        depth = depth,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> GMResult.Ok(copy)
                    is GMResult.Err -> result
                }
            }
            is JessieCodeRuntimeValue.ArrayValue -> {
                if (value.values.size > limits.maxCollectionSize) {
                    return GMResult.Err(
                        JessieCodeRuntimeError
                            .CollectionSizeLimitExceeded(
                                limit = limits.maxCollectionSize,
                                requestedSize = value.values.size.toLong(),
                                location = location,
                            ),
                    )
                }
                val values = mutableListOf<JessieCodeRuntimeValue>()
                for (entry in value.values) {
                    when (
                        val result = copyAttributeValue(
                            value = entry,
                            depth = depth + 1,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> values += result.value
                        is GMResult.Err -> return result
                    }
                }
                GMResult.Ok(JessieCodeRuntimeValue.ArrayValue(values))
            }
            is JessieCodeRuntimeValue.ElementReference ->
                GMResult.Ok(
                    JessieCodeRuntimeValue.StringValue(value.element.id),
                )
            else -> GMResult.Ok(value)
        }
    }

    private fun evaluateProperty(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        val receiver = when (
            val result = evaluateNodeChild(node, 0, depth)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val property = when (val result = textChild(node, 1)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return resolveProperty(receiver, property, node.location)
    }

    private fun evaluateConditional(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        val condition = when (
            val result = evaluateNodeChild(node, 0, depth)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return evaluateNodeChild(
            node = node,
            index = if (isTruthy(condition)) 1 else 2,
            parentDepth = depth,
        )
    }

    private fun evaluateLogicalOr(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        val left = when (
            val result = evaluateNodeChild(node, 0, depth)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return if (isTruthy(left)) {
            GMResult.Ok(left)
        } else {
            evaluateNodeChild(node, 1, depth)
        }
    }

    private fun evaluateLogicalAnd(
        node: JessieCodeAstNode,
        depth: Int,
    ): EvaluationResult {
        val left = when (
            val result = evaluateNodeChild(node, 0, depth)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return if (!isTruthy(left)) {
            GMResult.Ok(left)
        } else {
            evaluateNodeChild(node, 1, depth)
        }
    }

    private inline fun evaluateUnary(
        node: JessieCodeAstNode,
        depth: Int,
        operation: (JessieCodeRuntimeValue) -> EvaluationResult,
    ): EvaluationResult {
        val operand = when (
            val result = evaluateNodeChild(node, 0, depth)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return operation(operand)
    }

    private inline fun evaluateBinary(
        node: JessieCodeAstNode,
        depth: Int,
        operation: (
            JessieCodeRuntimeValue,
            JessieCodeRuntimeValue,
        ) -> EvaluationResult,
    ): EvaluationResult {
        val left = when (
            val result = evaluateNodeChild(node, 0, depth)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val right = when (
            val result = evaluateNodeChild(node, 1, depth)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return operation(left, right)
    }

    private fun evaluateNodeChild(
        node: JessieCodeAstNode,
        index: Int,
        parentDepth: Int,
    ): EvaluationResult =
        when (val result = nodeChild(node, index)) {
            is GMResult.Ok -> evaluate(
                node = result.value,
                depth = parentDepth + 1,
            )
            is GMResult.Err -> result
        }

    private fun evaluateVariable(
        node: JessieCodeAstNode,
        functionPosition: Boolean,
    ): EvaluationResult {
        val name = when (val value = node.value) {
            is JessieCodeAstValue.Text -> value.value
            else -> return invalidAst(
                node,
                "Variable value must be text.",
            )
        }
        val local = localScope(name)
        if (local != null) {
            return GMResult.Ok(
                local.locals.getValue(name),
            )
        }
        when (name) {
            "PI" -> return number(PI)
            "EULER" -> return number(E)
            "\$board" -> return GMResult.Ok(
                currentBoard?.let {
                    JessieCodeRuntimeValue.BoardReference(it)
                } ?: JessieCodeRuntimeValue.UndefinedValue,
            )
        }

        if (functionPosition) {
            val callable = standardCallable(name)
                ?: environment.functions[name]
            if (callable != null) {
                return GMResult.Ok(
                    JessieCodeRuntimeValue.FunctionValue(
                        name = name,
                        callable = callable,
                    ),
                )
            }
            val creator = environment.creators[name]
                ?: NativeJessieCodeCreators.creator(name)
            if (creator != null) {
                return GMResult.Ok(
                    JessieCodeRuntimeValue.FunctionValue(
                        name = name,
                        callable = JessieCodeCallable {
                                arguments,
                                location,
                            ->
                            creator.create(
                                board = currentBoard,
                                parents = arguments,
                                attributes =
                                    JessieCodeRuntimeValue.ObjectValue(
                                        emptyMap(),
                                    ),
                                location = location,
                            )
                        },
                        creator = creator,
                    ),
                )
            }
        }

        val element = currentBoard?.select(name)
        return GMResult.Ok(
            element?.let {
                JessieCodeRuntimeValue.ElementReference(it)
            } ?: JessieCodeRuntimeValue.UndefinedValue,
        )
    }

    private fun localScope(name: String): RuntimeScope? {
        var scope: RuntimeScope? = currentScope
        while (scope != null) {
            val value = scope.locals[name]
            if (
                value != null &&
                value !== JessieCodeRuntimeValue.NullValue &&
                value !== JessieCodeRuntimeValue.UndefinedValue
            ) {
                return scope
            }
            scope = scope.previous
        }
        return null
    }

    private fun visibleLocalNames(): Set<String> {
        val names = linkedSetOf<String>()
        var scope: RuntimeScope? = currentScope
        while (scope != null) {
            for ((name, value) in scope.locals) {
                if (
                    value !== JessieCodeRuntimeValue.NullValue &&
                    value !== JessieCodeRuntimeValue.UndefinedValue
                ) {
                    names += name
                }
            }
            scope = scope.previous
        }
        return names
    }

    private fun evaluateConstant(
        node: JessieCodeAstNode,
    ): EvaluationResult =
        when (val value = node.value) {
            is JessieCodeAstValue.Number -> number(value.value)
            JessieCodeAstValue.Null ->
                GMResult.Ok(JessieCodeRuntimeValue.NullValue)
            else -> invalidAst(
                node,
                "Constant value must be a number or null.",
            )
        }

    private fun evaluateBoolean(
        node: JessieCodeAstNode,
    ): EvaluationResult =
        when (val value = node.value) {
            is JessieCodeAstValue.Boolean -> GMResult.Ok(
                JessieCodeRuntimeValue.BooleanValue(value.value),
            )
            else -> invalidAst(
                node,
                "Boolean constant value must be boolean.",
            )
        }

    private fun evaluateString(
        node: JessieCodeAstNode,
    ): EvaluationResult =
        when (val value = node.value) {
            is JessieCodeAstValue.Text -> GMResult.Ok(
                JessieCodeRuntimeValue.StringValue(
                    removeJessieCodeEscapes(value.value),
                ),
            )
            else -> invalidAst(
                node,
                "String value must be text.",
            )
        }

    private fun comparison(
        node: JessieCodeAstNode,
        depth: Int,
        comparison: Comparison,
    ): EvaluationResult = evaluateBinary(node, depth) { left, right ->
        val result = compare(left, right)
        val matches = when {
            result == null -> false
            comparison == Comparison.LESS -> result < 0
            comparison == Comparison.GREATER -> result > 0
            comparison == Comparison.LESS_OR_EQUAL -> result <= 0
            else -> result >= 0
        }
        GMResult.Ok(JessieCodeRuntimeValue.BooleanValue(matches))
    }

    private fun add(
        initialLeft: JessieCodeRuntimeValue,
        initialRight: JessieCodeRuntimeValue,
        node: JessieCodeAstNode,
    ): EvaluationResult {
        val left = when (val result = evalSlider(initialLeft, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val right = when (val result = evalSlider(initialRight, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        if (
            left is JessieCodeRuntimeValue.ArrayValue &&
            right is JessieCodeRuntimeValue.ArrayValue
        ) {
            val length = minOf(left.values.size, right.values.size)
            return GMResult.Ok(
                JessieCodeRuntimeValue.ArrayValue(
                    List(length) { index ->
                        jsAdd(
                            left.values[index],
                            right.values[index],
                        )
                    },
                ),
            )
        }
        if (
            left is JessieCodeRuntimeValue.NumberValue &&
            right is JessieCodeRuntimeValue.NumberValue
        ) {
            return number(left.value + right.value)
        }
        if (
            left is JessieCodeRuntimeValue.StringValue ||
            right is JessieCodeRuntimeValue.StringValue
        ) {
            val leftString = explicitToString(left)
                ?: return unsupported(node, "op_add", left, right)
            val rightString = explicitToString(right)
                ?: return unsupported(node, "op_add", left, right)
            return string(leftString + rightString)
        }
        return unsupported(node, "op_add", left, right)
    }

    private fun subtract(
        initialLeft: JessieCodeRuntimeValue,
        initialRight: JessieCodeRuntimeValue,
        node: JessieCodeAstNode,
    ): EvaluationResult {
        val left = when (val result = evalSlider(initialLeft, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val right = when (val result = evalSlider(initialRight, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            left is JessieCodeRuntimeValue.ArrayValue &&
            right is JessieCodeRuntimeValue.ArrayValue
        ) {
            val length = minOf(left.values.size, right.values.size)
            return GMResult.Ok(
                JessieCodeRuntimeValue.ArrayValue(
                    List(length) { index ->
                        JessieCodeRuntimeValue.NumberValue(
                            toNumber(left.values[index]) -
                                toNumber(right.values[index]),
                        )
                    },
                ),
            )
        }
        if (
            left is JessieCodeRuntimeValue.NumberValue &&
            right is JessieCodeRuntimeValue.NumberValue
        ) {
            return number(left.value - right.value)
        }
        return unsupported(node, "op_sub", left, right)
    }

    private fun negate(
        initialValue: JessieCodeRuntimeValue,
        node: JessieCodeAstNode,
    ): EvaluationResult {
        val value = when (val result = evalSlider(initialValue, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (value) {
            is JessieCodeRuntimeValue.ArrayValue ->
                GMResult.Ok(
                    JessieCodeRuntimeValue.ArrayValue(
                        value.values.map {
                            JessieCodeRuntimeValue.NumberValue(
                                -toNumber(it),
                            )
                        },
                    ),
                )
            is JessieCodeRuntimeValue.NumberValue ->
                number(-value.value)
            else -> unsupported(node, "op_neg", value)
        }
    }

    private fun multiply(
        initialLeft: JessieCodeRuntimeValue,
        initialRight: JessieCodeRuntimeValue,
        node: JessieCodeAstNode,
    ): EvaluationResult {
        var left = when (val result = evalSlider(initialLeft, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        var right = when (val result = evalSlider(initialRight, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        if (
            left is JessieCodeRuntimeValue.ArrayValue &&
            right is JessieCodeRuntimeValue.NumberValue
        ) {
            // Preserve JSXGraph 1.13.3 exactly: its swap branch assigns the
            // numeric right operand to both operands.
            left = right
            right = left
        }

        if (
            left is JessieCodeRuntimeValue.ArrayValue &&
            right is JessieCodeRuntimeValue.ArrayValue
        ) {
            val length = minOf(left.values.size, right.values.size)
            var result = 0.0
            for (index in 0 until length) {
                result +=
                    toNumber(left.values[index]) *
                    toNumber(right.values[index])
            }
            return number(result)
        }
        if (
            left is JessieCodeRuntimeValue.NumberValue &&
            right is JessieCodeRuntimeValue.ArrayValue
        ) {
            return GMResult.Ok(
                JessieCodeRuntimeValue.ArrayValue(
                    right.values.map {
                        JessieCodeRuntimeValue.NumberValue(
                            left.value * toNumber(it),
                        )
                    },
                ),
            )
        }
        if (
            left is JessieCodeRuntimeValue.NumberValue &&
            right is JessieCodeRuntimeValue.NumberValue
        ) {
            return number(left.value * right.value)
        }
        return unsupported(node, "op_mul", left, right)
    }

    private fun divide(
        initialLeft: JessieCodeRuntimeValue,
        initialRight: JessieCodeRuntimeValue,
        node: JessieCodeAstNode,
    ): EvaluationResult {
        val left = when (val result = evalSlider(initialLeft, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val right = when (val result = evalSlider(initialRight, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            left is JessieCodeRuntimeValue.ArrayValue &&
            right is JessieCodeRuntimeValue.NumberValue
        ) {
            return GMResult.Ok(
                JessieCodeRuntimeValue.ArrayValue(
                    left.values.map {
                        JessieCodeRuntimeValue.NumberValue(
                            toNumber(it) / right.value,
                        )
                    },
                ),
            )
        }
        if (
            left is JessieCodeRuntimeValue.NumberValue &&
            right is JessieCodeRuntimeValue.NumberValue
        ) {
            return number(left.value / right.value)
        }
        return unsupported(node, "op_div", left, right)
    }

    private fun modulo(
        initialLeft: JessieCodeRuntimeValue,
        initialRight: JessieCodeRuntimeValue,
        node: JessieCodeAstNode,
    ): EvaluationResult {
        val left = when (val result = evalSlider(initialLeft, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val right = when (val result = evalSlider(initialRight, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            left is JessieCodeRuntimeValue.ArrayValue &&
            right is JessieCodeRuntimeValue.NumberValue
        ) {
            return GMResult.Ok(
                JessieCodeRuntimeValue.ArrayValue(
                    left.values.map {
                        JessieCodeRuntimeValue.NumberValue(
                            Mat.mod(toNumber(it), right.value),
                        )
                    },
                ),
            )
        }
        if (
            left is JessieCodeRuntimeValue.NumberValue &&
            right is JessieCodeRuntimeValue.NumberValue
        ) {
            return number(Mat.mod(left.value, right.value))
        }
        return unsupported(node, "op_mod", left, right)
    }

    private fun power(
        initialLeft: JessieCodeRuntimeValue,
        initialRight: JessieCodeRuntimeValue,
        node: JessieCodeAstNode,
    ): EvaluationResult {
        val left = when (val result = evalSlider(initialLeft, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val right = when (val result = evalSlider(initialRight, node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return number(Mat.pow(toNumber(left), toNumber(right)))
    }

    private fun evalSlider(
        value: JessieCodeRuntimeValue,
        node: JessieCodeAstNode,
    ): EvaluationResult {
        if (
            value is JessieCodeRuntimeValue.ElementReference &&
            value.element.type == Const.OBJECT_TYPE_GLIDER
        ) {
            return environment.elementRuntime.valueOf(
                element = value.element,
                location = node.location,
            )
        }
        return GMResult.Ok(value)
    }

    private fun resolveProperty(
        receiver: JessieCodeRuntimeValue,
        property: String,
        location: JessieCodeAstLocation,
    ): EvaluationResult =
        when (receiver) {
            is JessieCodeRuntimeValue.ArrayValue ->
                if (property == "length") {
                    number(receiver.values.size.toDouble())
                } else if (property in receiver.properties) {
                    GMResult.Ok(receiver.properties.getValue(property))
                } else {
                    unknownProperty(receiver, property, location)
                }
            is JessieCodeRuntimeValue.StringValue ->
                if (property == "length") {
                    number(receiver.value.length.toDouble())
                } else {
                    unknownProperty(receiver, property, location)
                }
            is JessieCodeRuntimeValue.ObjectValue ->
                receiver.properties[property]?.let {
                    GMResult.Ok(it)
                } ?: unknownProperty(receiver, property, location)
            is JessieCodeRuntimeValue.ElementReference ->
                environment.elementRuntime.resolveProperty(
                    element = receiver.element,
                    property = property,
                    location = location,
                )
            is JessieCodeRuntimeValue.FunctionValue -> GMResult.Err(
                JessieCodeRuntimeError.FunctionPropertyAccess(
                    property = property,
                    location = location,
                ),
            )
            else -> unknownProperty(receiver, property, location)
        }

    private fun index(
        receiver: JessieCodeRuntimeValue,
        numericIndex: Double,
    ): JessieCodeRuntimeValue {
        if (floor(numericIndex) != numericIndex) {
            return JessieCodeRuntimeValue.UndefinedValue
        }
        val property = JsNumberFormat.compact(numericIndex)
        return when (receiver) {
            is JessieCodeRuntimeValue.ArrayValue -> {
                val index = arrayIndex(property)
                if (
                    index != null &&
                    index <= Int.MAX_VALUE.toLong()
                ) {
                    receiver.values.getOrNull(index.toInt())
                        ?: receiver.properties[property]
                        ?: JessieCodeRuntimeValue.UndefinedValue
                } else {
                    receiver.properties[property]
                        ?: JessieCodeRuntimeValue.UndefinedValue
                }
            }
            is JessieCodeRuntimeValue.StringValue ->
                if (
                    numericIndex >= 0.0 &&
                    numericIndex <= Int.MAX_VALUE.toDouble()
                ) {
                    receiver.value.getOrNull(numericIndex.toInt())?.let {
                        JessieCodeRuntimeValue.StringValue(it.toString())
                    } ?: JessieCodeRuntimeValue.UndefinedValue
                } else {
                    JessieCodeRuntimeValue.UndefinedValue
                }
            is JessieCodeRuntimeValue.ObjectValue ->
                receiver.properties[property]
                    ?: JessieCodeRuntimeValue.UndefinedValue
            else -> JessieCodeRuntimeValue.UndefinedValue
        }
    }

    private fun standardCallable(name: String): JessieCodeCallable? =
        when (name) {
            "\$" -> JessieCodeCallable { arguments, _ ->
                val id = arguments.firstOrNull()?.let(::propertyKey)
                val element = id?.let { currentBoard?.elementById(it) }
                GMResult.Ok(
                    element?.let {
                        JessieCodeRuntimeValue.ElementReference(it)
                    } ?: JessieCodeRuntimeValue.UndefinedValue,
                )
            }
            "\$value" -> JessieCodeCallable { arguments, location ->
                val id = arguments.firstOrNull()?.let(::propertyKey)
                val element = id?.let { currentBoard?.elementById(it) }
                if (element == null) {
                    GMResult.Err(
                        JessieCodeRuntimeError.ElementValueUnavailable(
                            elementId = id ?: "undefined",
                            location = location,
                        ),
                    )
                } else {
                    environment.elementRuntime.valueOf(element, location)
                }
            }
            "sin" -> unaryNumber(::sin)
            "cos" -> unaryNumber(::cos)
            "tan" -> unaryNumber(::tan)
            "asin" -> unaryNumber(::asin)
            "acos" -> unaryNumber(::acos)
            "atan" -> unaryNumber(::atan)
            "atan2" -> binaryNumber(::atan2)
            "sinh" -> unaryNumber(::sinh)
            "cosh" -> unaryNumber(::cosh)
            "tanh" -> unaryNumber(::tanh)
            "asinh" -> unaryNumber(::asinh)
            "acosh" -> unaryNumber(::acosh)
            "atanh" -> unaryNumber(::atanh)
            "sqrt" -> unaryNumber(::sqrt)
            "exp" -> unaryNumber(::exp)
            "abs" -> unaryNumber(::abs)
            "floor" -> unaryNumber(::floor)
            "ceil" -> unaryNumber(::ceil)
            "round" -> unaryNumber(JsMath::round)
            "trunc" -> unaryNumber {
                if (it < 0.0) ceil(it) else floor(it)
            }
            "ln" -> unaryNumber(::ln)
            "log" -> JessieCodeCallable { arguments, _ ->
                val value = toNumber(
                    arguments.getOrElse(0) {
                        JessieCodeRuntimeValue.UndefinedValue
                    },
                )
                val base = arguments.getOrNull(1)
                number(
                    if (base == null) {
                        ln(value)
                    } else {
                        Mat.log(value, toNumber(base))
                    },
                )
            }
            "log2", "lb", "ld" -> unaryNumber(Mat::log2)
            "log10", "lg" -> unaryNumber(Mat::log10)
            "pow" -> binaryNumber(Mat::pow)
            "min" -> variadicExtrema(isMinimum = true)
            "max" -> variadicExtrema(isMinimum = false)
            "sign" -> unaryNumber(Mat::sign)
            "cbrt" -> unaryNumber(Mat::cbrt)
            "cot" -> unaryNumber(Mat::cot)
            "acot" -> unaryNumber(Mat::acot)
            "factorial" -> unaryNumber(Mat::factorial)
            "erf" -> unaryNumber(Mat::erf)
            "erfc" -> unaryNumber(Mat::erfc)
            "erfi" -> unaryNumber(Mat::erfi)
            "ndtr" -> unaryNumber(Mat::ndtr)
            "ndtri" -> unaryNumber(Mat::ndtri)
            "nthroot" -> binaryNumber(Mat::nthroot)
            else -> null
        }

    private fun unaryNumber(
        function: (Double) -> Double,
    ): JessieCodeCallable =
        JessieCodeCallable { arguments, _ ->
            number(
                function(
                    toNumber(
                        arguments.getOrElse(0) {
                            JessieCodeRuntimeValue.UndefinedValue
                        },
                    ),
                ),
            )
        }

    private fun binaryNumber(
        function: (Double, Double) -> Double,
    ): JessieCodeCallable =
        JessieCodeCallable { arguments, _ ->
            number(
                function(
                    toNumber(
                        arguments.getOrElse(0) {
                            JessieCodeRuntimeValue.UndefinedValue
                        },
                    ),
                    toNumber(
                        arguments.getOrElse(1) {
                            JessieCodeRuntimeValue.UndefinedValue
                        },
                    ),
                ),
            )
        }

    private fun variadicExtrema(
        isMinimum: Boolean,
    ): JessieCodeCallable =
        JessieCodeCallable { arguments, _ ->
            if (arguments.isEmpty()) {
                number(
                    if (isMinimum) {
                        Double.POSITIVE_INFINITY
                    } else {
                        Double.NEGATIVE_INFINITY
                    },
                )
            } else {
                var result = toNumber(arguments[0])
                for (index in 1 until arguments.size) {
                    val candidate = toNumber(arguments[index])
                    result = when {
                        result.isNaN() || candidate.isNaN() -> Double.NaN
                        isMinimum && candidate < result -> candidate
                        isMinimum &&
                            candidate == 0.0 &&
                            result == 0.0 &&
                            candidate.toBits() == NEGATIVE_ZERO_BITS ->
                            candidate
                        !isMinimum && candidate > result -> candidate
                        !isMinimum &&
                            candidate == 0.0 &&
                            result == 0.0 &&
                            result.toBits() == NEGATIVE_ZERO_BITS ->
                            candidate
                        else -> result
                    }
                }
                number(result)
            }
        }

    private fun nodeChild(
        node: JessieCodeAstNode,
        index: Int,
    ): NodeResult {
        val child = node.children.getOrNull(index)
            ?: return invalidAst(
                node,
                "Missing node child at index $index.",
            )
        return when (child) {
            is JessieCodeAstChild.Node -> GMResult.Ok(child.value)
            else -> invalidAst(
                node,
                "Child at index $index must be a node.",
            )
        }
    }

    private fun nodeListChild(
        node: JessieCodeAstNode,
        index: Int,
    ): NodeListResult {
        val child = node.children.getOrNull(index)
            ?: return invalidAst(
                node,
                "Missing node-list child at index $index.",
            )
        return when (child) {
            is JessieCodeAstChild.NodeList -> GMResult.Ok(child.value)
            else -> invalidAst(
                node,
                "Child at index $index must be a node list.",
            )
        }
    }

    private fun textChild(
        node: JessieCodeAstNode,
        index: Int,
    ): TextResult {
        val child = node.children.getOrNull(index)
            ?: return invalidAst(
                node,
                "Missing text child at index $index.",
            )
        return when (child) {
            is JessieCodeAstChild.Text -> GMResult.Ok(child.value)
            else -> invalidAst(
                node,
                "Child at index $index must be text.",
            )
        }
    }

    private fun textListChild(
        node: JessieCodeAstNode,
        index: Int,
    ): TextListResult {
        val child = node.children.getOrNull(index)
            ?: return invalidAst(
                node,
                "Missing text-list child at index $index.",
            )
        return when (child) {
            is JessieCodeAstChild.TextList -> GMResult.Ok(child.value)
            else -> invalidAst(
                node,
                "Child at index $index must be a text list.",
            )
        }
    }

    private fun invalidAst(
        node: JessieCodeAstNode,
        reason: String,
    ): GMResult.Err<JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.InvalidAst(
                reason = reason,
                location = node.location,
            ),
        )

    private fun unsupported(
        node: JessieCodeAstNode,
        operator: String,
        vararg operands: JessieCodeRuntimeValue,
    ): EvaluationResult =
        GMResult.Err(
            JessieCodeRuntimeError.UnsupportedOperation(
                operator = operator,
                operandTypes = operands.map(::typeName),
                location = node.location,
            ),
        )

    private fun unknownProperty(
        receiver: JessieCodeRuntimeValue,
        property: String,
        location: JessieCodeAstLocation,
    ): EvaluationResult =
        GMResult.Err(
            JessieCodeRuntimeError.UnknownProperty(
                receiverType = typeName(receiver),
                property = property,
                location = location,
            ),
        )

    private fun assignmentUnavailable(
        receiver: JessieCodeRuntimeValue,
        property: String,
        location: JessieCodeAstLocation,
    ): GMResult.Err<
        JessieCodeRuntimeError.AssignmentTargetUnavailable,
        > = GMResult.Err(
        JessieCodeRuntimeError.AssignmentTargetUnavailable(
            receiverType = typeName(receiver),
            property = property,
            location = location,
        ),
    )

    private fun operationName(node: JessieCodeAstNode): String? =
        if (node.type == JessieCodeAstNodeType.OPERATION) {
            (node.value as? JessieCodeAstValue.Text)?.value
        } else {
            null
        }

    private fun assignmentTargetType(node: JessieCodeAstNode): String =
        operationName(node) ?: node.type.upstreamName

    private fun arrayIndex(property: String): Long? {
        if (!ARRAY_INDEX.matches(property)) {
            return null
        }
        val index = property.toLongOrNull() ?: return null
        return index.takeIf { it <= MAX_JAVASCRIPT_ARRAY_INDEX }
    }

    private fun number(value: Double): EvaluationResult =
        GMResult.Ok(JessieCodeRuntimeValue.NumberValue(value))

    private fun string(value: String): EvaluationResult =
        GMResult.Ok(JessieCodeRuntimeValue.StringValue(value))

    private fun isTruthy(value: JessieCodeRuntimeValue): Boolean =
        when (value) {
            JessieCodeRuntimeValue.NullValue,
            JessieCodeRuntimeValue.UndefinedValue,
            -> false
            is JessieCodeRuntimeValue.BooleanValue -> value.value
            is JessieCodeRuntimeValue.NumberValue ->
                value.value != 0.0 && !value.value.isNaN()
            is JessieCodeRuntimeValue.StringValue ->
                value.value.isNotEmpty()
            is JessieCodeRuntimeValue.ArrayValue,
            is JessieCodeRuntimeValue.ObjectValue,
            is JessieCodeRuntimeValue.FunctionValue,
            is JessieCodeRuntimeValue.BoardReference,
            is JessieCodeRuntimeValue.ElementReference,
            -> true
        }

    private fun looselyEqual(
        left: JessieCodeRuntimeValue,
        right: JessieCodeRuntimeValue,
    ): Boolean {
        if (left::class == right::class) {
            return when {
                left === JessieCodeRuntimeValue.NullValue -> true
                left === JessieCodeRuntimeValue.UndefinedValue -> true
                left is JessieCodeRuntimeValue.NumberValue &&
                    right is JessieCodeRuntimeValue.NumberValue ->
                    left.value == right.value
                left is JessieCodeRuntimeValue.BooleanValue &&
                    right is JessieCodeRuntimeValue.BooleanValue ->
                    left.value == right.value
                left is JessieCodeRuntimeValue.StringValue &&
                    right is JessieCodeRuntimeValue.StringValue ->
                    left.value == right.value
                left is JessieCodeRuntimeValue.ElementReference &&
                    right is JessieCodeRuntimeValue.ElementReference ->
                    left.element === right.element
                left is JessieCodeRuntimeValue.BoardReference &&
                    right is JessieCodeRuntimeValue.BoardReference ->
                    left.board === right.board
                else -> left === right
            }
        }
        if (
            left === JessieCodeRuntimeValue.NullValue &&
            right === JessieCodeRuntimeValue.UndefinedValue ||
            left === JessieCodeRuntimeValue.UndefinedValue &&
            right === JessieCodeRuntimeValue.NullValue
        ) {
            return true
        }
        if (
            left is JessieCodeRuntimeValue.NumberValue &&
            right is JessieCodeRuntimeValue.StringValue
        ) {
            return left.value == toNumber(right)
        }
        if (
            left is JessieCodeRuntimeValue.StringValue &&
            right is JessieCodeRuntimeValue.NumberValue
        ) {
            return toNumber(left) == right.value
        }
        if (left is JessieCodeRuntimeValue.BooleanValue) {
            return looselyEqual(
                JessieCodeRuntimeValue.NumberValue(toNumber(left)),
                right,
            )
        }
        if (right is JessieCodeRuntimeValue.BooleanValue) {
            return looselyEqual(
                left,
                JessieCodeRuntimeValue.NumberValue(toNumber(right)),
            )
        }
        if (isObjectLike(left) && isPrimitive(right)) {
            return looselyEqual(toPrimitive(left), right)
        }
        if (isPrimitive(left) && isObjectLike(right)) {
            return looselyEqual(left, toPrimitive(right))
        }
        return false
    }

    private fun compare(
        left: JessieCodeRuntimeValue,
        right: JessieCodeRuntimeValue,
    ): Int? {
        val leftPrimitive = toPrimitive(left)
        val rightPrimitive = toPrimitive(right)
        if (
            leftPrimitive is JessieCodeRuntimeValue.StringValue &&
            rightPrimitive is JessieCodeRuntimeValue.StringValue
        ) {
            return leftPrimitive.value.compareTo(rightPrimitive.value)
        }
        val leftNumber = toNumber(leftPrimitive)
        val rightNumber = toNumber(rightPrimitive)
        if (leftNumber.isNaN() || rightNumber.isNaN()) {
            return null
        }
        return when {
            leftNumber < rightNumber -> -1
            leftNumber > rightNumber -> 1
            else -> 0
        }
    }

    private fun toNumber(value: JessieCodeRuntimeValue): Double =
        when (value) {
            JessieCodeRuntimeValue.UndefinedValue -> Double.NaN
            JessieCodeRuntimeValue.NullValue -> 0.0
            is JessieCodeRuntimeValue.NumberValue -> value.value
            is JessieCodeRuntimeValue.BooleanValue ->
                if (value.value) 1.0 else 0.0
            is JessieCodeRuntimeValue.StringValue ->
                parseJavaScriptNumber(value.value)
            else -> toNumber(toPrimitive(value))
        }

    private fun toPrimitive(
        value: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue =
        when (value) {
            is JessieCodeRuntimeValue.ArrayValue ->
                JessieCodeRuntimeValue.StringValue(
                    arrayToString(value),
                )
            is JessieCodeRuntimeValue.ObjectValue,
            is JessieCodeRuntimeValue.BoardReference,
            is JessieCodeRuntimeValue.ElementReference,
            -> JessieCodeRuntimeValue.StringValue("[object Object]")
            is JessieCodeRuntimeValue.FunctionValue ->
                JessieCodeRuntimeValue.StringValue(
                    "function ${value.name}() { }",
                )
            else -> value
        }

    private fun explicitToString(
        value: JessieCodeRuntimeValue,
    ): String? =
        when (value) {
            JessieCodeRuntimeValue.NullValue,
            JessieCodeRuntimeValue.UndefinedValue,
            -> null
            else -> toJsString(value)
        }

    private fun toJsString(value: JessieCodeRuntimeValue): String =
        when (value) {
            JessieCodeRuntimeValue.UndefinedValue -> "undefined"
            JessieCodeRuntimeValue.NullValue -> "null"
            is JessieCodeRuntimeValue.NumberValue ->
                JsNumberFormat.compact(value.value)
            is JessieCodeRuntimeValue.BooleanValue ->
                value.value.toString()
            is JessieCodeRuntimeValue.StringValue -> value.value
            is JessieCodeRuntimeValue.ArrayValue ->
                arrayToString(value)
            is JessieCodeRuntimeValue.ObjectValue,
            is JessieCodeRuntimeValue.BoardReference,
            is JessieCodeRuntimeValue.ElementReference,
            -> "[object Object]"
            is JessieCodeRuntimeValue.FunctionValue ->
                "function ${value.name}() { }"
        }

    private fun jsAdd(
        left: JessieCodeRuntimeValue,
        right: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue {
        val leftPrimitive = toPrimitive(left)
        val rightPrimitive = toPrimitive(right)
        return if (
            leftPrimitive is JessieCodeRuntimeValue.StringValue ||
            rightPrimitive is JessieCodeRuntimeValue.StringValue
        ) {
            JessieCodeRuntimeValue.StringValue(
                toJsString(leftPrimitive) + toJsString(rightPrimitive),
            )
        } else {
            JessieCodeRuntimeValue.NumberValue(
                toNumber(leftPrimitive) + toNumber(rightPrimitive),
            )
        }
    }

    private fun propertyKey(value: JessieCodeRuntimeValue): String =
        toJsString(toPrimitive(value))

    private fun arrayToString(
        value: JessieCodeRuntimeValue.ArrayValue,
    ): String =
        value.values.joinToString(separator = ",") {
            when (it) {
                JessieCodeRuntimeValue.NullValue,
                JessieCodeRuntimeValue.UndefinedValue,
                -> ""
                else -> toJsString(it)
            }
        }

    private fun isPrimitive(value: JessieCodeRuntimeValue): Boolean =
        value is JessieCodeRuntimeValue.NumberValue ||
            value is JessieCodeRuntimeValue.BooleanValue ||
            value is JessieCodeRuntimeValue.StringValue ||
            value === JessieCodeRuntimeValue.NullValue ||
            value === JessieCodeRuntimeValue.UndefinedValue

    private fun isObjectLike(value: JessieCodeRuntimeValue): Boolean =
        !isPrimitive(value)

    private fun typeName(value: JessieCodeRuntimeValue): String =
        when (value) {
            JessieCodeRuntimeValue.UndefinedValue -> "undefined"
            JessieCodeRuntimeValue.NullValue -> "null"
            is JessieCodeRuntimeValue.NumberValue -> "number"
            is JessieCodeRuntimeValue.BooleanValue -> "boolean"
            is JessieCodeRuntimeValue.StringValue -> "string"
            is JessieCodeRuntimeValue.ArrayValue -> "array"
            is JessieCodeRuntimeValue.ObjectValue -> "object"
            is JessieCodeRuntimeValue.FunctionValue -> "function"
            is JessieCodeRuntimeValue.BoardReference -> "board"
            is JessieCodeRuntimeValue.ElementReference -> "element"
        }

    private enum class Comparison {
        LESS,
        GREATER,
        LESS_OR_EQUAL,
        GREATER_OR_EQUAL,
    }

    private companion object {
        const val INDEX_INTEGER_TOLERANCE = 1.0e-12
        const val MAX_JAVASCRIPT_ARRAY_INDEX = 4_294_967_294L
        const val MAX_JAVASCRIPT_ARRAY_LENGTH = 4_294_967_295L
        const val CALL_ARGUMENT_CHILD_COUNT = 2
        const val CALL_WITH_ATTRIBUTES_CHILD_COUNT = 4
        val NEGATIVE_ZERO_BITS = (-0.0).toBits()
        val ARRAY_INDEX = Regex("""^(0|[1-9][0-9]*)$""")
    }

    private sealed interface AssignmentTarget {
        val implicitCreatorName: String

        data class Variable(
            val scope: RuntimeScope,
            val name: String,
        ) : AssignmentTarget {
            override val implicitCreatorName: String
                get() = name
        }

        data class Property(
            val receiver: JessieCodeRuntimeValue,
            val property: String,
        ) : AssignmentTarget {
            override val implicitCreatorName: String
                get() = property
        }
    }

    private class RuntimeScope(
        val parameters: List<String>,
        val locals: MutableMap<String, JessieCodeRuntimeValue>,
        val previous: RuntimeScope?,
    ) {
        var implicitCreatorName: String? = null
    }
}

private typealias EvaluationResult =
    GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>
private typealias NodeResult =
    GMResult<JessieCodeAstNode, JessieCodeRuntimeError>
private typealias NodeListResult =
    GMResult<List<JessieCodeAstNode>, JessieCodeRuntimeError>
private typealias TextResult =
    GMResult<String, JessieCodeRuntimeError>
private typealias TextListResult =
    GMResult<List<String>, JessieCodeRuntimeError>

private const val MAX_SUPPORTED_EVALUATION_DEPTH = 64

private fun parseJavaScriptNumber(source: String): Double {
    val value = source.trim()
    if (value.isEmpty()) {
        return 0.0
    }
    return when {
        value == "Infinity" || value == "+Infinity" ->
            Double.POSITIVE_INFINITY
        value == "-Infinity" -> Double.NEGATIVE_INFINITY
        HEX_NUMBER.matches(value) ->
            value.drop(2).toLongOrNull(radix = 16)?.toDouble()
                ?: Double.NaN
        BINARY_NUMBER.matches(value) ->
            value.drop(2).toLongOrNull(radix = 2)?.toDouble()
                ?: Double.NaN
        OCTAL_NUMBER.matches(value) ->
            value.drop(2).toLongOrNull(radix = 8)?.toDouble()
                ?: Double.NaN
        else -> value.toDoubleOrNull() ?: Double.NaN
    }
}

private fun removeJessieCodeEscapes(source: String): String =
    buildString(source.length) {
        var index = 0
        while (index < source.length) {
            val current = source[index]
            if (
                current == '\\' &&
                index + 1 < source.length &&
                source[index + 1] != '\n' &&
                source[index + 1] != '\r' &&
                source[index + 1] != '\u2028' &&
                source[index + 1] != '\u2029'
            ) {
                append(source[index + 1])
                index += 2
            } else {
                append(current)
                index += 1
            }
        }
    }

private val HEX_NUMBER = Regex("""^0[xX][0-9a-fA-F]+$""")
private val BINARY_NUMBER = Regex("""^0[bB][01]+$""")
private val OCTAL_NUMBER = Regex("""^0[oO][0-7]+$""")
