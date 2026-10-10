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
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.BoardZoomMode
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Composition
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Group
import com.swithun.jsxgraph.core.base.GroupCenter
import com.swithun.jsxgraph.core.base.GroupError
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Point3D
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.utils.EventHandler
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

    internal val log: List<List<JessieCodeRuntimeValue>>
        get() = state.log

    internal val currentBoard
        get() = state.currentBoardValue

    internal fun pushScope(
        arguments: List<String>,
    ): JessieCodeScopeSnapshot = state.pushScope(arguments)

    internal fun popScope(): JessieCodeScopeSnapshot = state.popScope()

    internal fun letVariable(
        name: String,
        value: JessieCodeRuntimeValue,
    ): JessieCodeScopeSnapshot = state.letVariable(name, value)

    internal fun localVariableScope(
        name: String,
    ): JessieCodeScopeSnapshot? = state.localVariableScope(name)

    internal fun parameterScope(
        name: String,
    ): JessieCodeScopeSnapshot? = state.parameterScope(name)

    internal fun resolve(
        name: String,
    ): JessieCodeRuntimeValue = state.resolve(name)

    internal fun resolveVariable(
        name: String,
        local: Boolean,
        functionName: Boolean,
    ): JessieCodeRuntimeValue =
        state.resolveVariable(name, local, functionName)

    internal fun getElementById(
        id: String,
    ): JessieCodeRuntimeValue = state.getElementById(id)

    internal fun isMathMethod(name: String): Boolean =
        state.isMathMethod(name)

    internal fun isBuiltIn(name: String): Boolean =
        state.isBuiltIn(name)

    internal fun addBuiltIn(
        name: String,
        callable: JessieCodeCallable,
    ): Boolean = state.addBuiltIn(name, callable)

    internal fun callBuiltIn(
        name: String,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        state.callBuiltIn(name, arguments, location)

    internal fun findSymbol(
        value: JessieCodeRuntimeValue,
        scopeId: Int = -1,
    ): JessieCodeSymbol? = state.findSymbol(value, scopeId)

    internal fun assignProperty(
        receiver: JessieCodeRuntimeValue,
        property: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Unit, JessieCodeRuntimeError> =
        state.assignFacadeProperty(receiver, property, value, location)

    internal fun evaluateValue(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        state.evaluateFacadeValue(value, location)

    internal fun possibleOperands(): JessieCodePossibleOperands =
        state.possibleOperands()
}

private class EvaluationState(
    private val limits: JessieCodeEvaluatorLimits,
    private val environment: JessieCodeRuntimeEnvironment,
) {
    private data class BoardEventHandlerKey(
        val board: Board,
        val event: String,
        val function: JessieCodeRuntimeValue.FunctionValue,
    )

    private class BoardEventHandlerState(
        val handler: EventHandler,
        var error: JessieCodeRuntimeError? = null,
    )

    private var evaluationSteps = 0
    private var functionCallDepth = 0
    private var currentBoard = environment.board
    private val scopes = mutableListOf<RuntimeScope>()
    private var currentScope = RuntimeScope(
        id = 0,
        parameters = emptyList(),
        locals = environment.variables.toMutableMap(),
        previous = null,
    ).also(scopes::add)
    private val addedBuiltIns =
        linkedMapOf<String, JessieCodeCallable>()
    private val logEntries =
        mutableListOf<List<JessieCodeRuntimeValue>>()
    private val importModules = JessieCodeImportModules()
    private val boardEventHandlers =
        mutableMapOf<BoardEventHandlerKey, BoardEventHandlerState>()

    internal val log: List<List<JessieCodeRuntimeValue>>
        get() = logEntries

    internal val currentBoardValue: Board?
        get() = currentBoard

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> pushScope.
    internal fun pushScope(
        arguments: List<String>,
    ): JessieCodeScopeSnapshot {
        val scope = RuntimeScope(
            id = scopes.size,
            parameters = arguments.toList(),
            locals = mutableMapOf(),
            previous = currentScope,
        )
        scopes += scope
        currentScope = scope
        return scope.snapshot()
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> popScope.
    internal fun popScope(): JessieCodeScopeSnapshot {
        currentScope = currentScope.previous ?: currentScope
        return currentScope.snapshot()
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> letvar.
    internal fun letVariable(
        name: String,
        value: JessieCodeRuntimeValue,
    ): JessieCodeScopeSnapshot {
        currentScope.locals[name] = value
        return currentScope.snapshot()
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> isLocalVariable.
    internal fun localVariableScope(
        name: String,
    ): JessieCodeScopeSnapshot? = localScope(name)?.snapshot()

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> isParameter.
    internal fun parameterScope(
        name: String,
    ): JessieCodeScopeSnapshot? {
        var scope: RuntimeScope? = currentScope
        while (scope != null) {
            if (name in scope.parameters) {
                return scope.snapshot()
            }
            scope = scope.previous
        }
        return null
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> resolve.
    internal fun resolve(name: String): JessieCodeRuntimeValue =
        localScope(name)?.locals?.get(name)
            ?: JessieCodeRuntimeValue.UndefinedValue

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> getvarJS.
    // The native target resolves the value directly instead of emitting JS.
    internal fun resolveVariable(
        name: String,
        local: Boolean,
        functionName: Boolean,
    ): JessieCodeRuntimeValue {
        localScope(name)?.locals?.get(name)?.let { return it }
        when (name) {
            "PI" -> return JessieCodeRuntimeValue.NumberValue(PI)
            "EULER" -> return JessieCodeRuntimeValue.NumberValue(E)
            "\$board" -> return currentBoard?.let {
                JessieCodeRuntimeValue.BoardReference(it)
            } ?: JessieCodeRuntimeValue.UndefinedValue
        }
        if (functionName) {
            val callable = standardCallable(name)
                ?: addedBuiltIns[name]
                ?: environment.functions[name]
            if (callable != null) {
                return JessieCodeRuntimeValue.FunctionValue(
                    name = name,
                    callable = callable,
                )
            }
            val creator = environment.creators[name]
                ?: NativeJessieCodeCreators.creator(name)
            if (creator != null) {
                return JessieCodeRuntimeValue.FunctionValue(
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
                )
            }
        }
        if (local) {
            return JessieCodeRuntimeValue.UndefinedValue
        }
        val element = currentBoard?.select(name)
        val group = currentBoard?.groupById(name)
        return element?.let {
            JessieCodeRuntimeValue.ElementReference(it)
        } ?: group?.let {
            JessieCodeRuntimeValue.CompositionReference(it)
        } ?: JessieCodeRuntimeValue.UndefinedValue
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> getElementById.
    internal fun getElementById(id: String): JessieCodeRuntimeValue {
        val element = currentBoard?.elementById(id)
        val group = currentBoard?.groupById(id)
        return element?.let {
            JessieCodeRuntimeValue.ElementReference(it)
        } ?: group?.let {
            JessieCodeRuntimeValue.CompositionReference(it)
        } ?: JessieCodeRuntimeValue.UndefinedValue
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> isMathMethod.
    internal fun isMathMethod(name: String): Boolean =
        name != "E" && name in MATH_METHOD_NAMES

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> isBuiltIn.
    internal fun isBuiltIn(name: String): Boolean =
        name in BUILT_IN_CONSTANT_NAMES ||
            name in BUILT_IN_CALLABLE_NAMES ||
            name in addedBuiltIns

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> addBuiltIn.
    internal fun addBuiltIn(
        name: String,
        callable: JessieCodeCallable,
    ): Boolean {
        if (isBuiltIn(name)) {
            return false
        }
        addedBuiltIns[name] = callable
        return true
    }

    internal fun callBuiltIn(
        name: String,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): EvaluationResult {
        val callable = standardCallable(name)
            ?: addedBuiltIns[name]
            ?: environment.functions[name]
            ?: return GMResult.Err(
                JessieCodeRuntimeError.NotCallable(
                    valueType = name,
                    location = location,
                ),
            )
        return callWithFreshBudget {
            callable.call(arguments, location)
        }
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> findSymbol.
    internal fun findSymbol(
        value: JessieCodeRuntimeValue,
        scopeId: Int,
    ): JessieCodeSymbol? {
        var scope = if (scopeId == -1) {
            currentScope
        } else {
            scopes.getOrNull(scopeId) ?: return null
        }
        while (true) {
            for ((name, candidate) in scope.locals) {
                if (sameRuntimeValue(candidate, value)) {
                    return JessieCodeSymbol(name, scope.snapshot())
                }
            }
            scope = scope.previous ?: return null
        }
    }

    internal fun assignFacadeProperty(
        receiver: JessieCodeRuntimeValue,
        property: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Unit, JessieCodeRuntimeError> =
        assignProperty(receiver, property, value, location)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> eval.
    internal fun evaluateFacadeValue(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): EvaluationResult = callWithFreshBudget {
        evaluateBuiltInValue(value, location)
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> getPossibleOperands
    // closures sort / pack.
    internal fun possibleOperands(): JessieCodePossibleOperands {
        val entries = mutableListOf<JessieCodeOperand>()
        for (name in BUILT_IN_CALLABLE_NAMES.sortedWith(NAME_COMPARATOR)) {
            if (!name.startsWith("\$")) {
                entries += JessieCodeOperand(
                    name = name,
                    type = JessieCodeOperandType.FUNCTION,
                    origin = if (name in MATH_METHOD_NAMES) {
                        JessieCodeOperandOrigin.MATH
                    } else {
                        JessieCodeOperandOrigin.JESSIE_CODE
                    },
                )
            }
        }
        for (name in addedBuiltIns.keys.sortedWith(NAME_COMPARATOR)) {
            entries += JessieCodeOperand(
                name = name,
                type = JessieCodeOperandType.FUNCTION,
                origin = JessieCodeOperandOrigin.JESSIE_CODE,
            )
        }
        entries += JessieCodeOperand(
            name = "EULER",
            type = JessieCodeOperandType.CONSTANT,
            origin = JessieCodeOperandOrigin.JESSIE_CODE,
            value = E,
        )
        entries += JessieCodeOperand(
            name = "PI",
            type = JessieCodeOperandType.CONSTANT,
            origin = JessieCodeOperandOrigin.JESSIE_CODE,
            value = PI,
        )
        val sorted = entries.distinctBy { it.name }
            .sortedWith(compareBy(NAME_COMPARATOR) { it.name })
        return JessieCodePossibleOperands(
            all = sorted.associateBy { it.name },
            list = sorted,
            functions = sorted.filter {
                it.type == JessieCodeOperandType.FUNCTION
            }.map { it.name },
            jessieCodeFunctions = sorted.filter {
                it.type == JessieCodeOperandType.FUNCTION &&
                    it.origin == JessieCodeOperandOrigin.JESSIE_CODE
            }.map { it.name },
            constants = sorted.filter {
                it.type == JessieCodeOperandType.CONSTANT
            }.map { it.name },
        )
    }

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
        if (local != null) {
            when (val value = local.locals.getValue(name)) {
                is JessieCodeRuntimeValue.ElementReference ->
                    currentBoard?.removeObject(value.element)
                is JessieCodeRuntimeValue.CompositionReference -> {
                    if (value.composition !is Group) {
                        currentBoard?.removeObject(value.composition)
                    }
                }
                else -> Unit
            }
        } else {
            currentBoard?.select(name)?.let { currentBoard?.removeObject(it) }
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
            id = scopes.size,
            parameters = parameters,
            locals = mutableMapOf(),
            previous = currentScope,
        )
        scopes += functionScope
        // The interpreter's defineFunction calls pushScope and intentionally
        // leaves that function scope current when compile mode is disabled.
        currentScope = functionScope

        val dependencies = currentBoard?.let { board ->
            when (
                val result = JessieCodeDependencyCollector(board).collect(
                    node = body,
                    parameterNames = parameters.toSet(),
                    localNames = visibleLocalNames(),
                    localElements = visibleLocalElements(),
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
        val externalCallable = JessieCodeCallable { arguments, location ->
            callWithFreshBudget {
                callFunction(
                    scope = functionScope,
                    parameters = parameters,
                    body = body,
                    arguments = arguments,
                    location = location,
                )
            }
        }
        val sourceExpression = when (
            val result = JessieCodeCompiler.compile(node)
        ) {
            is GMResult.Ok -> result.value.trim()
            is GMResult.Err -> null
        }
        val javaScriptSourceExpression = when (
            val result = JessieCodeCompiler.compile(
                node,
                JessieCodeFormatOptions(javaScript = true),
            )
        ) {
            is GMResult.Ok -> result.value.trim()
            is GMResult.Err -> null
        }
        return GMResult.Ok(
            JessieCodeRuntimeValue.FunctionValue(
                name = if (isMap) "map" else "function",
                callable = callable,
                externalCallable = externalCallable,
                parameterNames = parameters.toList(),
                isMap = isMap,
                dependencies = dependencies.toMap(),
                sourceExpression = sourceExpression,
                javaScriptSourceExpression =
                    javaScriptSourceExpression,
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

    private fun callWithFreshBudget(
        call: () -> EvaluationResult,
    ): EvaluationResult {
        val previousEvaluationSteps = evaluationSteps
        val previousFunctionCallDepth = functionCallDepth
        evaluationSteps = 0
        functionCallDepth = 0
        return try {
            call()
        } finally {
            evaluationSteps = previousEvaluationSteps
            functionCallDepth = previousFunctionCallDepth
        }
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
            is JessieCodeRuntimeValue.ElementReference ->
                environment.elementRuntime.assignProperty(
                    element = receiver.element,
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
            is JessieCodeRuntimeValue.CompositionReference ->
                GMResult.Ok(value)
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
                ?: addedBuiltIns[name]
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
        val group = currentBoard?.groupById(name)
        return GMResult.Ok(
            element?.let {
                JessieCodeRuntimeValue.ElementReference(it)
            } ?: group?.let {
                JessieCodeRuntimeValue.CompositionReference(it)
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

    private fun visibleLocalElements(): Map<String, GeometryElement> {
        val elements = linkedMapOf<String, GeometryElement>()
        val resolvedNames = linkedSetOf<String>()
        var scope: RuntimeScope? = currentScope
        while (scope != null) {
            for ((name, value) in scope.locals) {
                if (
                    value !== JessieCodeRuntimeValue.NullValue &&
                    value !== JessieCodeRuntimeValue.UndefinedValue &&
                    resolvedNames.add(name)
                ) {
                    val element = (
                        value as? JessieCodeRuntimeValue.ElementReference
                    )?.element
                    if (element != null) {
                        elements[name] = element
                    }
                }
            }
            scope = scope.previous
        }
        return elements
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
            is JessieCodeRuntimeValue.BoardReference ->
                resolveBoardProperty(
                    receiver = receiver,
                    property = property,
                    location = location,
                )
            is JessieCodeRuntimeValue.TransformationReference ->
                resolveTransformationProperty(
                    receiver = receiver,
                    property = property,
                    location = location,
                )
            is JessieCodeRuntimeValue.CompositionReference ->
                resolveCompositionProperty(
                    receiver = receiver,
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

    // JSXGraph 1.13.3: src/base/board.js -> methodMap.
    private fun resolveBoardProperty(
        receiver: JessieCodeRuntimeValue.BoardReference,
        property: String,
        location: JessieCodeAstLocation,
    ): EvaluationResult {
        val board = receiver.board
        return when (property) {
            "update" -> boardFunction("update") { arguments, callLocation ->
                val draggedElement = when (
                    val value = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    JessieCodeRuntimeValue.UndefinedValue,
                    JessieCodeRuntimeValue.NullValue,
                    -> null
                    is JessieCodeRuntimeValue.ElementReference ->
                        value.element
                    else -> return@boardFunction invalidArgumentType(
                        functionName = "update",
                        argumentIndex = 0,
                        expected = "element or undefined",
                        actual = value,
                        location = callLocation,
                    )
                }
                boardReference(board.update(draggedElement))
            }
            "fullUpdate" -> boardFunction("fullUpdate") { _, _ ->
                boardReference(board.fullUpdate())
            }
            "on" -> boardFunction("on") { arguments, callLocation ->
                boardOn(
                    board = board,
                    arguments = arguments,
                    location = callLocation,
                )
            }
            "off" -> boardFunction("off") { arguments, callLocation ->
                boardOff(
                    board = board,
                    arguments = arguments,
                    location = callLocation,
                )
            }
            "trigger" -> boardFunction("trigger") {
                    arguments,
                    callLocation,
                ->
                boardTrigger(
                    board = board,
                    arguments = arguments,
                    location = callLocation,
                )
            }
            "setAttribute" -> boardFunction("setAttribute") {
                    arguments,
                    callLocation,
                ->
                val attributes = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ObjectValue
                    ) ?: return@boardFunction invalidArgumentType(
                    functionName = "setAttribute",
                    argumentIndex = 0,
                    expected = "object",
                    actual = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue,
                    location = callLocation,
                )
                val converted = when (
                    val result = boardAttributeMap(
                        value = attributes,
                        functionName = "setAttribute",
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@boardFunction result
                }
                val force = when (
                    val value = arguments.getOrNull(1)
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    JessieCodeRuntimeValue.UndefinedValue -> false
                    is JessieCodeRuntimeValue.BooleanValue -> value.value
                    else -> return@boardFunction invalidArgumentType(
                        functionName = "setAttribute",
                        argumentIndex = 1,
                        expected = "boolean or undefined",
                        actual = value,
                        location = callLocation,
                    )
                }
                boardReference(board.setAttribute(converted, force))
            }
            "setBoundingBox", "setView" ->
                boardFunction("setBoundingBox") {
                        arguments,
                        callLocation,
                    ->
                    boardSetBoundingBox(
                        board = board,
                        arguments = arguments,
                        location = callLocation,
                    )
                }
            "getBoundingBox", "BoundingBox", "getView", "View" ->
                boardFunction("getBoundingBox") { _, _ ->
                    GMResult.Ok(numberArray(board.getBoundingBox()))
                }
            "migratePoint" -> boardFunction("migratePoint") {
                    arguments,
                    callLocation,
                ->
                val source = boardPointArgument(
                    functionName = "migratePoint",
                    arguments = arguments,
                    index = 0,
                    location = callLocation,
                )
                val destination = boardPointArgument(
                    functionName = "migratePoint",
                    arguments = arguments,
                    index = 1,
                    location = callLocation,
                )
                val sourcePoint = when (source) {
                    is GMResult.Ok -> source.value
                    is GMResult.Err -> return@boardFunction source
                }
                val destinationPoint = when (destination) {
                    is GMResult.Ok -> destination.value
                    is GMResult.Err -> return@boardFunction destination
                }
                val copyName = when (
                    val value = arguments.getOrNull(2)
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    JessieCodeRuntimeValue.UndefinedValue -> false
                    is JessieCodeRuntimeValue.BooleanValue -> value.value
                    else -> return@boardFunction invalidArgumentType(
                        functionName = "migratePoint",
                        argumentIndex = 2,
                        expected = "boolean or undefined",
                        actual = value,
                        location = callLocation,
                    )
                }
                boardReference(
                    board.migratePoint(
                        source = sourcePoint,
                        destination = destinationPoint,
                        copyName = copyName,
                    ),
                )
            }
            "colorblind" -> boardFunction("colorblind") {
                    arguments,
                    callLocation,
                ->
                val deficiency = when (
                    val value = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    JessieCodeRuntimeValue.UndefinedValue -> "none"
                    is JessieCodeRuntimeValue.StringValue -> value.value
                    else -> return@boardFunction invalidArgumentType(
                        functionName = "colorblind",
                        argumentIndex = 0,
                        expected = "string or undefined",
                        actual = value,
                        location = callLocation,
                    )
                }
                boardReference(board.emulateColorblindness(deficiency))
            }
            "suspendUpdate" -> boardFunction("suspendUpdate") { _, _ ->
                boardReference(board.suspendUpdate())
            }
            "unsuspendUpdate" ->
                boardFunction("unsuspendUpdate") { _, _ ->
                    boardReference(board.unsuspendUpdate())
                }
            "clearTraces" -> boardFunction("clearTraces") { _, _ ->
                boardReference(board.clearTraces())
            }
            "left" -> boardFunction("left") { _, _ ->
                boardReference(board.clickLeftArrow())
            }
            "right" -> boardFunction("right") { _, _ ->
                boardReference(board.clickRightArrow())
            }
            "up" -> boardFunction("up") { _, _ ->
                boardReference(board.clickUpArrow())
            }
            "down" -> boardFunction("down") { _, _ ->
                boardReference(board.clickDownArrow())
            }
            "zoomIn" -> boardFunction("zoomIn") {
                    arguments,
                    callLocation,
                ->
                when (
                    val center = boardOptionalCoordinatePair(
                        functionName = "zoomIn",
                        arguments = arguments,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> boardReference(
                        board.zoomIn(
                            userX = center.value?.first,
                            userY = center.value?.second,
                        ),
                    )
                    is GMResult.Err -> center
                }
            }
            "zoomOut" -> boardFunction("zoomOut") {
                    arguments,
                    callLocation,
                ->
                when (
                    val center = boardOptionalCoordinatePair(
                        functionName = "zoomOut",
                        arguments = arguments,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> boardReference(
                        board.zoomOut(
                            userX = center.value?.first,
                            userY = center.value?.second,
                        ),
                    )
                    is GMResult.Err -> center
                }
            }
            "zoom100" -> boardFunction("zoom100") { _, _ ->
                boardReference(board.zoom100())
            }
            "zoomElements" -> boardFunction("zoomElements") {
                    arguments,
                    callLocation,
                ->
                val values = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ArrayValue
                    )?.values ?: return@boardFunction invalidArgumentType(
                    functionName = "zoomElements",
                    argumentIndex = 0,
                    expected = "array",
                    actual = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue,
                    location = callLocation,
                )
                val elements = mutableListOf<Any>()
                for ((index, value) in values.withIndex()) {
                    when (value) {
                        is JessieCodeRuntimeValue.ElementReference ->
                            elements += value.element
                        is JessieCodeRuntimeValue.StringValue ->
                            elements += value.value
                        else -> return@boardFunction invalidArgumentType(
                            functionName = "zoomElements",
                            argumentIndex = index,
                            expected = "element or string reference",
                            actual = value,
                            location = callLocation,
                        )
                    }
                }
                boardReference(board.zoomElements(elements))
            }
            "remove", "removeObject" ->
                boardFunction("removeObject") {
                        arguments,
                        callLocation,
                    ->
                    boardRemoveObject(
                        board = board,
                        arguments = arguments,
                        location = callLocation,
                    )
                }
            else -> unknownProperty(receiver, property, location)
        }
    }

    private fun boardOn(
        board: Board,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): EvaluationResult {
        val event = (
            arguments.firstOrNull() as?
                JessieCodeRuntimeValue.StringValue
            )?.value ?: return invalidArgumentType(
            functionName = "on",
            argumentIndex = 0,
            expected = "string",
            actual = arguments.firstOrNull()
                ?: JessieCodeRuntimeValue.UndefinedValue,
            location = location,
        )
        val function = arguments.getOrNull(1) as?
            JessieCodeRuntimeValue.FunctionValue
            ?: return invalidArgumentType(
                functionName = "on",
                argumentIndex = 1,
                expected = "function",
                actual = arguments.getOrNull(1)
                    ?: JessieCodeRuntimeValue.UndefinedValue,
                location = location,
            )
        val key = BoardEventHandlerKey(board, event, function)
        val state = boardEventHandlers.getOrPut(key) {
            lateinit var handlerState: BoardEventHandlerState
            val handler = EventHandler { _, nativeArguments ->
                val callbackResult = function.externalCallable.call(
                    arguments = nativeArguments.map(::runtimeValue),
                    location = location,
                )
                handlerState.error = when (callbackResult) {
                    is GMResult.Ok -> null
                    is GMResult.Err -> callbackResult.error
                }
            }
            BoardEventHandlerState(handler).also {
                handlerState = it
            }
        }
        board.on(event, state.handler)
        return boardReference(board)
    }

    private fun boardOff(
        board: Board,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): EvaluationResult {
        val event = (
            arguments.firstOrNull() as?
                JessieCodeRuntimeValue.StringValue
            )?.value ?: return invalidArgumentType(
            functionName = "off",
            argumentIndex = 0,
            expected = "string",
            actual = arguments.firstOrNull()
                ?: JessieCodeRuntimeValue.UndefinedValue,
            location = location,
        )
        val function = when (
            val value = arguments.getOrNull(1)
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> null
            is JessieCodeRuntimeValue.FunctionValue -> value
            else -> return invalidArgumentType(
                functionName = "off",
                argumentIndex = 1,
                expected = "function or undefined",
                actual = value,
                location = location,
            )
        }
        if (function == null) {
            board.off(event)
            boardEventHandlers.keys.removeAll {
                it.board === board && it.event == event
            }
        } else {
            val key = BoardEventHandlerKey(board, event, function)
            boardEventHandlers.remove(key)?.let {
                board.off(event, it.handler)
            }
        }
        return boardReference(board)
    }

    private fun boardTrigger(
        board: Board,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): EvaluationResult {
        val events = when (
            val value = arguments.firstOrNull()
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            is JessieCodeRuntimeValue.StringValue -> listOf(value.value)
            is JessieCodeRuntimeValue.ArrayValue -> {
                val result = mutableListOf<String>()
                for ((index, event) in value.values.withIndex()) {
                    val name = (
                        event as? JessieCodeRuntimeValue.StringValue
                        )?.value ?: return invalidArgumentType(
                        functionName = "trigger",
                        argumentIndex = index,
                        expected = "string event name",
                        actual = event,
                        location = location,
                    )
                    result += name
                }
                result
            }
            else -> return invalidArgumentType(
                functionName = "trigger",
                argumentIndex = 0,
                expected = "string or string array",
                actual = value,
                location = location,
            )
        }
        val states = boardEventHandlers
            .filterKeys { it.board === board && it.event in events }
            .values
        states.forEach { it.error = null }
        board.trigger(
            events = events,
            arguments = arguments.drop(1),
        )
        states.firstNotNullOfOrNull { it.error }?.let {
            return GMResult.Err(it)
        }
        return boardReference(board)
    }

    private fun boardSetBoundingBox(
        board: Board,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): EvaluationResult {
        val bbox = when (
            val result = boardNumberArray(
                functionName = "setBoundingBox",
                value = arguments.firstOrNull()
                    ?: JessieCodeRuntimeValue.UndefinedValue,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val keepAspectRatio = when (
            val value = arguments.getOrNull(1)
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> false
            is JessieCodeRuntimeValue.BooleanValue -> value.value
            else -> return invalidArgumentType(
                functionName = "setBoundingBox",
                argumentIndex = 1,
                expected = "boolean or undefined",
                actual = value,
                location = location,
            )
        }
        val zoomMode = when (
            val value = arguments.getOrNull(2)
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> BoardZoomMode.RESET
            is JessieCodeRuntimeValue.StringValue -> when (
                value.value.lowercase()
            ) {
                "reset" -> BoardZoomMode.RESET
                "keep" -> BoardZoomMode.KEEP
                "update" -> BoardZoomMode.UPDATE
                else -> return invalidArgumentType(
                    functionName = "setBoundingBox",
                    argumentIndex = 2,
                    expected = "reset, keep, or update",
                    actual = value,
                    location = location,
                )
            }
            else -> return invalidArgumentType(
                functionName = "setBoundingBox",
                argumentIndex = 2,
                expected = "string or undefined",
                actual = value,
                location = location,
            )
        }
        return when (
            val result = board.setBoundingBox(
                bbox = bbox,
                keepAspectRatio = keepAspectRatio,
                setZoom = zoomMode,
            )
        ) {
            is GMResult.Ok -> boardReference(result.value)
            is GMResult.Err -> GMResult.Err(
                JessieCodeRuntimeError.BuiltInInvocationFailure(
                    functionName = "setBoundingBox",
                    reason = result.error.toString(),
                    location = location,
                ),
            )
        }
    }

    private fun boardRemoveObject(
        board: Board,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): EvaluationResult {
        val value = arguments.firstOrNull()
            ?: JessieCodeRuntimeValue.UndefinedValue
        val saveMethod = when (
            val raw = arguments.getOrNull(1)
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> false
            is JessieCodeRuntimeValue.BooleanValue -> raw.value
            else -> return invalidArgumentType(
                functionName = "removeObject",
                argumentIndex = 1,
                expected = "boolean or undefined",
                actual = raw,
                location = location,
            )
        }
        when (value) {
            is JessieCodeRuntimeValue.ElementReference ->
                board.removeObject(value.element, saveMethod)
            is JessieCodeRuntimeValue.StringValue ->
                board.removeObject(value.value, saveMethod)
            is JessieCodeRuntimeValue.CompositionReference ->
                board.removeObject(value.composition, saveMethod)
            is JessieCodeRuntimeValue.ArrayValue -> {
                val elements = mutableListOf<GeometryElement>()
                for ((index, child) in value.values.withIndex()) {
                    val element = when (child) {
                        is JessieCodeRuntimeValue.ElementReference ->
                            child.element
                        is JessieCodeRuntimeValue.StringValue ->
                            board.select(child.value)
                                ?: return invalidArgumentType(
                                    functionName = "removeObject",
                                    argumentIndex = index,
                                    expected =
                                        "registered element reference",
                                    actual = child,
                                    location = location,
                                )
                        else -> return invalidArgumentType(
                            functionName = "removeObject",
                            argumentIndex = index,
                            expected = "element or string reference",
                            actual = child,
                            location = location,
                        )
                    }
                    elements += element
                }
                board.removeObjects(elements, saveMethod)
            }
            else -> return invalidArgumentType(
                functionName = "removeObject",
                argumentIndex = 0,
                expected =
                    "element, composition, string reference, or array",
                actual = value,
                location = location,
            )
        }
        return boardReference(board)
    }

    private fun boardPointArgument(
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        index: Int,
        location: JessieCodeAstLocation,
    ): GMResult<Point, JessieCodeRuntimeError> {
        val value = arguments.getOrNull(index)
            ?: JessieCodeRuntimeValue.UndefinedValue
        val point = (
            value as? JessieCodeRuntimeValue.ElementReference
            )?.element as? Point
            ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = index,
                expected = "point",
                actual = value,
                location = location,
            )
        return GMResult.Ok(point)
    }

    private fun boardOptionalCoordinatePair(
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<Pair<Double, Double>?, JessieCodeRuntimeError> {
        if (arguments.isEmpty()) {
            return GMResult.Ok(null)
        }
        if (arguments.size < 2) {
            return GMResult.Err(
                JessieCodeRuntimeError.InvalidArgumentCount(
                    functionName = functionName,
                    expected = "0 or 2",
                    actual = arguments.size,
                    location = location,
                ),
            )
        }
        val x = (
            arguments[0] as? JessieCodeRuntimeValue.NumberValue
            )?.value ?: return invalidArgumentType(
            functionName = functionName,
            argumentIndex = 0,
            expected = "number",
            actual = arguments[0],
            location = location,
        )
        val y = (
            arguments[1] as? JessieCodeRuntimeValue.NumberValue
            )?.value ?: return invalidArgumentType(
            functionName = functionName,
            argumentIndex = 1,
            expected = "number",
            actual = arguments[1],
            location = location,
        )
        return GMResult.Ok(x to y)
    }

    private fun boardNumberArray(
        functionName: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<DoubleArray, JessieCodeRuntimeError> {
        val values = (
            value as? JessieCodeRuntimeValue.ArrayValue
            )?.values ?: return invalidArgumentType(
            functionName = functionName,
            argumentIndex = 0,
            expected = "numeric array",
            actual = value,
            location = location,
        )
        val result = DoubleArray(values.size)
        for ((index, child) in values.withIndex()) {
            result[index] = (
                child as? JessieCodeRuntimeValue.NumberValue
                )?.value ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = index,
                expected = "number",
                actual = child,
                location = location,
            )
        }
        return GMResult.Ok(result)
    }

    private fun boardAttributeMap(
        value: JessieCodeRuntimeValue.ObjectValue,
        functionName: String,
        location: JessieCodeAstLocation,
    ): GMResult<Map<String, Any?>, JessieCodeRuntimeError> {
        val result = linkedMapOf<String, Any?>()
        for ((property, child) in value.properties) {
            when (
                val converted = boardAttributeValue(
                    value = child,
                    functionName = functionName,
                    location = location,
                    depth = 0,
                )
            ) {
                is GMResult.Ok -> result[property] = converted.value
                is GMResult.Err -> return converted
            }
        }
        return GMResult.Ok(result)
    }

    private fun boardAttributeValue(
        value: JessieCodeRuntimeValue,
        functionName: String,
        location: JessieCodeAstLocation,
        depth: Int,
    ): GMResult<Any?, JessieCodeRuntimeError> {
        if (depth > MAX_RUNTIME_VALUE_DEPTH) {
            return GMResult.Err(
                JessieCodeRuntimeError.ResourceLimitExceeded(
                    resource = "$functionName attribute depth",
                    limit = MAX_RUNTIME_VALUE_DEPTH,
                    requestedSize = depth.toLong(),
                    location = location,
                ),
            )
        }
        return when (value) {
            JessieCodeRuntimeValue.UndefinedValue,
            JessieCodeRuntimeValue.NullValue,
            -> GMResult.Ok(null)
            is JessieCodeRuntimeValue.NumberValue -> GMResult.Ok(value.value)
            is JessieCodeRuntimeValue.BooleanValue ->
                GMResult.Ok(value.value)
            is JessieCodeRuntimeValue.StringValue ->
                GMResult.Ok(value.value)
            is JessieCodeRuntimeValue.BoardReference ->
                GMResult.Ok(value.board)
            is JessieCodeRuntimeValue.TransformationReference ->
                GMResult.Ok(value.transformation)
            is JessieCodeRuntimeValue.CompositionReference ->
                GMResult.Ok(value.composition)
            is JessieCodeRuntimeValue.ElementReference ->
                GMResult.Ok(value.element)
            is JessieCodeRuntimeValue.FunctionValue ->
                GMResult.Ok(value)
            is JessieCodeRuntimeValue.ArrayValue -> {
                val result = mutableListOf<Any?>()
                for (child in value.values) {
                    when (
                        val converted = boardAttributeValue(
                            value = child,
                            functionName = functionName,
                            location = location,
                            depth = depth + 1,
                        )
                    ) {
                        is GMResult.Ok -> result += converted.value
                        is GMResult.Err -> return converted
                    }
                }
                GMResult.Ok(result)
            }
            is JessieCodeRuntimeValue.ObjectValue -> {
                val result = linkedMapOf<String, Any?>()
                for ((property, child) in value.properties) {
                    when (
                        val converted = boardAttributeValue(
                            value = child,
                            functionName = functionName,
                            location = location,
                            depth = depth + 1,
                        )
                    ) {
                        is GMResult.Ok -> result[property] = converted.value
                        is GMResult.Err -> return converted
                    }
                }
                GMResult.Ok(result)
            }
        }
    }

    private fun boardFunction(
        name: String,
        callable: JessieCodeCallable,
    ): EvaluationResult =
        GMResult.Ok(
            JessieCodeRuntimeValue.FunctionValue(name, callable),
        )

    private fun boardReference(board: Board): EvaluationResult =
        GMResult.Ok(JessieCodeRuntimeValue.BoardReference(board))

    private fun numberArray(values: DoubleArray): JessieCodeRuntimeValue =
        JessieCodeRuntimeValue.ArrayValue(
            values.map(JessieCodeRuntimeValue::NumberValue),
        )

    // JSXGraph 1.13.3: src/base/transformation.js -> methodMap.
    private fun resolveTransformationProperty(
        receiver: JessieCodeRuntimeValue.TransformationReference,
        property: String,
        location: JessieCodeAstLocation,
    ): EvaluationResult {
        val transformation = receiver.transformation
        return when (property) {
            "apply" -> transformationFunction("apply") {
                    arguments,
                    callLocation,
                ->
                val element = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ElementReference
                    )?.element ?: return@transformationFunction invalidArgumentType(
                        functionName = "apply",
                        argumentIndex = 0,
                        expected = "coordinate element",
                        actual = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                val self = when (
                    val value = arguments.getOrNull(1)
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    JessieCodeRuntimeValue.UndefinedValue -> false
                    is JessieCodeRuntimeValue.StringValue ->
                        value.value == "self"
                    is JessieCodeRuntimeValue.BooleanValue -> value.value
                    else -> return@transformationFunction invalidArgumentType(
                        functionName = "apply",
                        argumentIndex = 1,
                        expected = "self, boolean, or undefined",
                        actual = value,
                        location = callLocation,
                    )
                }
                val result = when (element) {
                    is CoordsElement ->
                        transformation.applyResult(element, self)
                    is Point3D ->
                        transformation.applyResult(
                            if (self) {
                                element.initialCoords
                            } else {
                                element.coords
                            },
                        )
                    else -> return@transformationFunction invalidArgumentType(
                        functionName = "apply",
                        argumentIndex = 0,
                        expected = "2D or 3D point",
                        actual = arguments.first(),
                        location = callLocation,
                    )
                }
                when (result) {
                    is GMResult.Ok ->
                        GMResult.Ok(numberArray(result.value))
                    is GMResult.Err -> transformationFailure(
                        method = "apply",
                        reason = result.error.toString(),
                        location = callLocation,
                    )
                }
            }
            "applyOnce" -> transformationFunction("applyOnce") {
                    arguments,
                    callLocation,
                ->
                val elements = when (
                    val result = transformationElements(
                        functionName = "applyOnce",
                        value = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@transformationFunction result
                }
                for (element in elements) {
                    val result = when (element) {
                        is CoordsElement ->
                            transformation.applyOnce(element)
                        is Point3D ->
                            transformation.applyOnce(element)
                        else -> return@transformationFunction invalidArgumentType(
                            functionName = "applyOnce",
                            argumentIndex = 0,
                            expected = "2D or 3D point",
                            actual =
                                JessieCodeRuntimeValue.ElementReference(
                                    element,
                                ),
                            location = callLocation,
                        )
                    }
                    if (result is GMResult.Err) {
                        return@transformationFunction transformationFailure(
                            method = "applyOnce",
                            reason = result.error.toString(),
                            location = callLocation,
                        )
                    }
                }
                GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
            }
            "bindTo", "bind" ->
                transformationFunction("bindTo") {
                        arguments,
                        callLocation,
                    ->
                    val elements = when (
                        val result = transformationElements(
                            functionName = "bindTo",
                            value = arguments.firstOrNull()
                                ?: JessieCodeRuntimeValue.UndefinedValue,
                            location = callLocation,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err ->
                            return@transformationFunction result
                    }
                    transformation.bindTo(elements)
                    GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
                }
            "meltTo" -> transformationFunction("meltTo") {
                    arguments,
                    callLocation,
                ->
                val elements = when (
                    val result = transformationElements(
                        functionName = "meltTo",
                        value = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err ->
                        return@transformationFunction result
                }
                when (val result = transformation.meltTo(elements)) {
                    is GMResult.Ok ->
                        GMResult.Ok(
                            JessieCodeRuntimeValue.UndefinedValue,
                        )
                    is GMResult.Err -> transformationFailure(
                        method = "meltTo",
                        reason = result.error.toString(),
                        location = callLocation,
                    )
                }
            }
            "melt" -> transformationFunction("melt") {
                    arguments,
                    callLocation,
                ->
                val other = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.TransformationReference
                    )?.transformation
                    ?: return@transformationFunction invalidArgumentType(
                        functionName = "melt",
                        argumentIndex = 0,
                        expected = "transformation",
                        actual = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                when (val result = transformation.melt(other)) {
                    is GMResult.Ok -> GMResult.Ok(receiver)
                    is GMResult.Err -> transformationFailure(
                        method = "melt",
                        reason = result.error.toString(),
                        location = callLocation,
                    )
                }
            }
            else -> unknownProperty(receiver, property, location)
        }
    }

    private fun transformationElements(
        functionName: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<List<GeometryElement>, JessieCodeRuntimeError> {
        val values = if (value is JessieCodeRuntimeValue.ArrayValue) {
            value.values
        } else {
            listOf(value)
        }
        val result = mutableListOf<GeometryElement>()
        for ((index, child) in values.withIndex()) {
            val element = (
                child as? JessieCodeRuntimeValue.ElementReference
                )?.element ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = index,
                expected = "element",
                actual = child,
                location = location,
            )
            result += element
        }
        return GMResult.Ok(result)
    }

    private fun transformationFunction(
        name: String,
        callable: JessieCodeCallable,
    ): EvaluationResult =
        GMResult.Ok(
            JessieCodeRuntimeValue.FunctionValue(name, callable),
        )

    private fun transformationFailure(
        method: String,
        reason: String,
        location: JessieCodeAstLocation,
    ): EvaluationResult =
        GMResult.Err(
            JessieCodeRuntimeError.BuiltInInvocationFailure(
                functionName = method,
                reason = reason,
                location = location,
            ),
        )

    private fun runtimeValue(value: Any?): JessieCodeRuntimeValue =
        when (value) {
            null -> JessieCodeRuntimeValue.NullValue
            is JessieCodeRuntimeValue -> value
            is Boolean -> JessieCodeRuntimeValue.BooleanValue(value)
            is Number ->
                JessieCodeRuntimeValue.NumberValue(value.toDouble())
            is String -> JessieCodeRuntimeValue.StringValue(value)
            is Board -> JessieCodeRuntimeValue.BoardReference(value)
            is GeometryElement ->
                JessieCodeRuntimeValue.ElementReference(value)
            is List<*> ->
                JessieCodeRuntimeValue.ArrayValue(value.map(::runtimeValue))
            is DoubleArray ->
                JessieCodeRuntimeValue.ArrayValue(
                    value.map(JessieCodeRuntimeValue::NumberValue),
                )
            else -> JessieCodeRuntimeValue.UndefinedValue
        }

    // JSXGraph: src/base/composition.js -> member properties and methodMap
    private fun resolveCompositionProperty(
        receiver: JessieCodeRuntimeValue.CompositionReference,
        property: String,
        location: JessieCodeAstLocation,
    ): EvaluationResult {
        val composition = receiver.composition
        if (composition is Group) {
            return resolveGroupProperty(
                receiver = receiver,
                group = composition,
                property = property,
                location = location,
            )
        }
        composition.member(property)?.let { member ->
            return GMResult.Ok(
                JessieCodeRuntimeValue.ElementReference(member),
            )
        }
        return when (property) {
            "elType" -> string(composition.elType)
            "dump" -> GMResult.Ok(
                JessieCodeRuntimeValue.BooleanValue(composition.dump),
            )
            "parents" -> GMResult.Ok(
                JessieCodeRuntimeValue.UndefinedValue,
            )
            "elements", "objects" -> GMResult.Ok(
                JessieCodeRuntimeValue.ObjectValue(
                    composition.elements.mapValues { (_, element) ->
                        JessieCodeRuntimeValue.ElementReference(element)
                    },
                ),
            )
            "elementsByName" -> GMResult.Ok(
                JessieCodeRuntimeValue.ObjectValue(
                    composition.elementsByName.mapValues { (_, element) ->
                        JessieCodeRuntimeValue.ElementReference(element)
                    },
                ),
            )
            "objectsList" -> GMResult.Ok(
                JessieCodeRuntimeValue.ArrayValue(
                    composition.objectsList.map(
                        JessieCodeRuntimeValue::ElementReference,
                    ),
                ),
            )
            "subs" -> GMResult.Ok(
                JessieCodeRuntimeValue.ObjectValue(
                    composition.subs.mapValues { (_, element) ->
                        JessieCodeRuntimeValue.ElementReference(element)
                    },
                ),
            )
            "getParents" -> GMResult.Ok(
                JessieCodeRuntimeValue.FunctionValue(
                    name = "getParents",
                    callable = JessieCodeCallable { _, _ ->
                        GMResult.Ok(
                            JessieCodeRuntimeValue.UndefinedValue,
                        )
                    },
                ),
            )
            "getType" -> GMResult.Ok(
                JessieCodeRuntimeValue.FunctionValue(
                    name = "getType",
                    callable = JessieCodeCallable { _, _ ->
                        string(composition.getType())
                    },
                ),
            )
            "getAttributes" -> GMResult.Ok(
                JessieCodeRuntimeValue.FunctionValue(
                    name = "getAttributes",
                    callable = JessieCodeCallable { _, _ ->
                        GMResult.Ok(
                            JessieCodeRuntimeValue.ObjectValue(
                                composition.getAttributes().mapValues {
                                        (_, value) -> runtimeValue(value)
                                },
                            ),
                        )
                    },
                ),
            )
            "setAttribute", "setProperty" -> GMResult.Ok(
                JessieCodeRuntimeValue.FunctionValue(
                    name = "setAttribute",
                    callable = JessieCodeCallable {
                            arguments,
                            callLocation,
                        ->
                        val attributes = (
                            arguments.firstOrNull() as?
                                JessieCodeRuntimeValue.ObjectValue
                            ) ?: return@JessieCodeCallable invalidArgumentType(
                            functionName = "setAttribute",
                            argumentIndex = 0,
                            expected = "object",
                            actual = arguments.firstOrNull()
                                ?: JessieCodeRuntimeValue.UndefinedValue,
                            location = callLocation,
                        )
                        val converted = when (
                            val result = boardAttributeMap(
                                value = attributes,
                                functionName = "setAttribute",
                                location = callLocation,
                            )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err ->
                                return@JessieCodeCallable result
                        }
                        composition.setAttribute(converted)
                        GMResult.Ok(receiver)
                    },
                ),
            )
            "setParents" -> GMResult.Ok(
                JessieCodeRuntimeValue.FunctionValue(
                    name = "setParents",
                    callable = JessieCodeCallable {
                            arguments,
                            callLocation,
                        ->
                        val values = (
                            arguments.firstOrNull() as?
                                JessieCodeRuntimeValue.ArrayValue
                            )?.values ?: arguments
                        val parents = mutableListOf<GeometryElement>()
                        for ((index, value) in values.withIndex()) {
                            val parent = (
                                value as?
                                    JessieCodeRuntimeValue.ElementReference
                                )?.element
                                ?: return@JessieCodeCallable invalidArgumentType(
                                    functionName = "setParents",
                                    argumentIndex = index,
                                    expected = "element",
                                    actual = value,
                                    location = callLocation,
                                )
                            parents += parent
                        }
                        composition.setParents(parents)
                        GMResult.Ok(receiver)
                    },
                ),
            )
            "prepareUpdate" -> compositionFunction("prepareUpdate") { _, _ ->
                composition.prepareUpdate()
                GMResult.Ok(receiver)
            }
            "update" -> compositionFunction("update") { _, _ ->
                composition.update()
                GMResult.Ok(receiver)
            }
            "fullUpdate" -> compositionFunction("fullUpdate") { _, _ ->
                composition.fullUpdate()
                GMResult.Ok(receiver)
            }
            "updateRenderer" ->
                compositionFunction("updateRenderer") { _, _ ->
                    composition.updateRenderer()
                    GMResult.Ok(receiver)
                }
            "highlight" -> compositionFunction("highlight") {
                    arguments,
                    callLocation,
                ->
                val force = when (
                    val value = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    JessieCodeRuntimeValue.UndefinedValue -> false
                    is JessieCodeRuntimeValue.BooleanValue -> value.value
                    else -> return@compositionFunction invalidArgumentType(
                        functionName = "highlight",
                        argumentIndex = 0,
                        expected = "boolean or undefined",
                        actual = value,
                        location = callLocation,
                    )
                }
                composition.highlight(force)
                GMResult.Ok(receiver)
            }
            "noHighlight" -> compositionFunction("noHighlight") { _, _ ->
                composition.noHighlight()
                GMResult.Ok(receiver)
            }
            "add" -> GMResult.Ok(
                JessieCodeRuntimeValue.FunctionValue(
                    name = "add",
                    callable = JessieCodeCallable {
                            arguments,
                            callLocation,
                        ->
                        val role = (
                            arguments.firstOrNull() as?
                                JessieCodeRuntimeValue.StringValue
                            )?.value ?: return@JessieCodeCallable invalidArgumentType(
                                functionName = "add",
                                argumentIndex = 0,
                                expected = "string",
                                actual = arguments.firstOrNull()
                                    ?: JessieCodeRuntimeValue.UndefinedValue,
                                location = callLocation,
                            )
                        val element = (
                            arguments.getOrNull(1) as?
                                JessieCodeRuntimeValue.ElementReference
                            )?.element ?: return@JessieCodeCallable invalidArgumentType(
                                functionName = "add",
                                argumentIndex = 1,
                                expected = "element",
                                actual = arguments.getOrNull(1)
                                    ?: JessieCodeRuntimeValue.UndefinedValue,
                                location = callLocation,
                            )
                        GMResult.Ok(
                            JessieCodeRuntimeValue.BooleanValue(
                                composition.add(role, element),
                            ),
                        )
                    },
                ),
            )
            "remove" -> GMResult.Ok(
                JessieCodeRuntimeValue.FunctionValue(
                    name = "remove",
                    callable = JessieCodeCallable {
                            arguments,
                            callLocation,
                        ->
                        val role = (
                            arguments.firstOrNull() as?
                                JessieCodeRuntimeValue.StringValue
                            )?.value ?: return@JessieCodeCallable invalidArgumentType(
                                functionName = "remove",
                                argumentIndex = 0,
                                expected = "string",
                                actual = arguments.firstOrNull()
                                    ?: JessieCodeRuntimeValue.UndefinedValue,
                                location = callLocation,
                            )
                        GMResult.Ok(
                            JessieCodeRuntimeValue.BooleanValue(
                                composition.remove(role),
                            ),
                        )
                    },
                ),
            )
            "select" -> GMResult.Ok(
                JessieCodeRuntimeValue.FunctionValue(
                    name = "select",
                    callable = JessieCodeCallable {
                            arguments,
                            callLocation,
                        ->
                        selectComposition(
                            composition = composition,
                            filter = arguments.firstOrNull()
                                ?: JessieCodeRuntimeValue.UndefinedValue,
                            location = callLocation,
                        )
                    },
                ),
            )
            else -> unknownProperty(receiver, property, location)
        }
    }

    private fun compositionFunction(
        name: String,
        callable: JessieCodeCallable,
    ): EvaluationResult =
        GMResult.Ok(
            JessieCodeRuntimeValue.FunctionValue(
                name = name,
                callable = callable,
            ),
        )

    private fun selectComposition(
        composition: Composition,
        filter: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): EvaluationResult =
        when (filter) {
            is JessieCodeRuntimeValue.StringValue ->
                GMResult.Ok(
                    when (val selected = composition.select(filter.value)) {
                        is GeometryElement ->
                            JessieCodeRuntimeValue.ElementReference(selected)
                        is Composition ->
                            JessieCodeRuntimeValue.CompositionReference(selected)
                        is String ->
                            JessieCodeRuntimeValue.StringValue(selected)
                        else -> JessieCodeRuntimeValue.UndefinedValue
                    },
                )
            is JessieCodeRuntimeValue.ElementReference ->
                GMResult.Ok(
                    composition.select(filter.element)?.let {
                        JessieCodeRuntimeValue.ElementReference(it)
                    } ?: JessieCodeRuntimeValue.NullValue,
                )
            is JessieCodeRuntimeValue.FunctionValue,
            is JessieCodeRuntimeValue.ObjectValue,
            -> filterComposition(
                composition = composition,
                filter = filter,
                location = location,
            )
            else -> invalidArgumentType(
                functionName = "select",
                argumentIndex = 0,
                expected = "string, element, object, or function",
                actual = filter,
                location = location,
            )
        }

    private fun filterComposition(
        composition: Composition,
        filter: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): EvaluationResult {
        val selected = linkedMapOf<String, GeometryElement>()
        for (element in composition.objectsList) {
            val matches = when (filter) {
                is JessieCodeRuntimeValue.FunctionValue -> {
                    when (
                        val result = filter.callable.call(
                            listOf(
                                JessieCodeRuntimeValue.ElementReference(element),
                            ),
                            location,
                        )
                    ) {
                        is GMResult.Ok -> isTruthy(result.value)
                        is GMResult.Err -> return result
                    }
                }
                is JessieCodeRuntimeValue.ObjectValue -> {
                    when (
                        val result = compositionMatchesFilter(
                            element = element,
                            filter = filter,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                }
                else -> false
            }
            if (matches) {
                selected[element.id] = element
            }
        }
        return GMResult.Ok(
            JessieCodeRuntimeValue.CompositionReference(
                Composition(selected),
            ),
        )
    }

    private fun compositionMatchesFilter(
        element: GeometryElement,
        filter: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): GMResult<Boolean, JessieCodeRuntimeError> {
        for ((property, criterion) in filter.properties) {
            val direct = when (
                val result = environment.elementRuntime.resolveProperty(
                    element = element,
                    property = property,
                    location = location,
                )
            ) {
                is GMResult.Ok -> when (val value = result.value) {
                    is JessieCodeRuntimeValue.FunctionValue ->
                        when (
                            val invoked = value.callable.call(
                                emptyList(),
                                location,
                            )
                        ) {
                            is GMResult.Ok -> invoked.value
                            is GMResult.Err -> return invoked
                        }
                    else -> value
                }
                is GMResult.Err ->
                    if (
                        result.error is
                            JessieCodeRuntimeError.UnknownProperty
                    ) {
                        JessieCodeRuntimeValue.UndefinedValue
                    } else {
                        return result
                    }
            }
            val visual =
                if (property.lowercase() in element.visualProperties) {
                    runtimeValue(element.evalVisProp(property.lowercase()))
                } else {
                    JessieCodeRuntimeValue.UndefinedValue
                }
            val matches = if (
                criterion is JessieCodeRuntimeValue.FunctionValue
            ) {
                val directMatch = when (
                    val result = criterion.callable.call(
                        listOf(direct),
                        location,
                    )
                ) {
                    is GMResult.Ok -> isTruthy(result.value)
                    is GMResult.Err -> return result
                }
                if (directMatch) {
                    true
                } else {
                    when (
                        val result = criterion.callable.call(
                            listOf(visual),
                            location,
                        )
                    ) {
                        is GMResult.Ok -> isTruthy(result.value)
                        is GMResult.Err -> return result
                    }
                }
            } else {
                sameRuntimeValue(direct, criterion) ||
                    sameRuntimeValue(visual, criterion)
            }
            if (!matches) {
                return GMResult.Ok(false)
            }
        }
        return GMResult.Ok(true)
    }

    // JSXGraph 1.13.3: src/base/group.js -> Group properties and methodMap.
    private fun resolveGroupProperty(
        receiver: JessieCodeRuntimeValue.CompositionReference,
        group: Group,
        property: String,
        location: JessieCodeAstLocation,
    ): EvaluationResult =
        when (property) {
            "id" -> string(group.id)
            "name" -> string(group.name)
            "elType" -> string(group.elType)
            "needsRegularUpdate" -> GMResult.Ok(
                JessieCodeRuntimeValue.BooleanValue(
                    group.needsRegularUpdate,
                ),
            )
            "needsUpdate" -> GMResult.Ok(
                JessieCodeRuntimeValue.BooleanValue(group.needsUpdate),
            )
            "parents" -> GMResult.Ok(
                JessieCodeRuntimeValue.ArrayValue(
                    group.parentIds.map(
                        JessieCodeRuntimeValue::StringValue,
                    ),
                ),
            )
            "objects" -> GMResult.Ok(
                JessieCodeRuntimeValue.ObjectValue(
                    group.groupObjects.mapValues { (_, element) ->
                        JessieCodeRuntimeValue.ElementReference(element)
                    },
                ),
            )
            "coords" -> GMResult.Ok(
                JessieCodeRuntimeValue.ObjectValue(
                    group.coords.mapValues { (_, coordinates) ->
                        JessieCodeRuntimeValue.ObjectValue(
                            mapOf(
                                "usrCoords" to
                                    JessieCodeRuntimeValue.ArrayValue(
                                        coordinates.map(
                                            JessieCodeRuntimeValue::NumberValue,
                                        ),
                                    ),
                            ),
                        )
                    },
                ),
            )
            "rotationCenter" -> GMResult.Ok(
                groupCenterRuntimeValue(group.rotationCenter),
            )
            "scaleCenter" -> GMResult.Ok(
                group.scaleCenter?.let(::groupCenterRuntimeValue)
                    ?: JessieCodeRuntimeValue.NullValue,
            )
            "rotationPoints" -> GMResult.Ok(
                groupElementArray(group.rotationPoints),
            )
            "translationPoints" -> GMResult.Ok(
                groupElementArray(group.translationPoints),
            )
            "scalePoints" -> GMResult.Ok(
                groupElementArray(group.scalePoints),
            )
            "scaleDirections" -> GMResult.Ok(
                JessieCodeRuntimeValue.ObjectValue(
                    group.scaleDirections.mapValues { (_, direction) ->
                        JessieCodeRuntimeValue.StringValue(direction)
                    },
                ),
            )
            "getParents" -> groupFunction("getParents") { _, _ ->
                GMResult.Ok(
                    JessieCodeRuntimeValue.ArrayValue(
                        group.getParents().map(
                            JessieCodeRuntimeValue::StringValue,
                        ),
                    ),
                )
            }
            "getType" -> groupFunction("getType") { _, _ ->
                string(group.getType())
            }
            "ungroup" -> groupFunction("ungroup") { _, _ ->
                group.ungroup()
                GMResult.Ok(receiver)
            }
            "add", "addPoint" -> groupFunction("addPoint") {
                    arguments,
                    callLocation,
                ->
                val point = when (
                    val result = groupPointArgument(
                        group = group,
                        functionName = "addPoint",
                        arguments = arguments,
                        index = 0,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@groupFunction result
                }
                group.addPoint(point)
                GMResult.Ok(receiver)
            }
            "addPoints" -> groupFunction("addPoints") {
                    arguments,
                    callLocation,
                ->
                val points = when (
                    val result = groupPointList(
                        group = group,
                        functionName = "addPoints",
                        values = (
                            arguments.firstOrNull() as?
                                JessieCodeRuntimeValue.ArrayValue
                            )?.values ?: arguments,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@groupFunction result
                }
                group.addPoints(points)
                GMResult.Ok(receiver)
            }
            "addGroup" -> groupFunction("addGroup") {
                    arguments,
                    callLocation,
                ->
                val other = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.CompositionReference
                    )?.composition as? Group
                    ?: return@groupFunction invalidArgumentType(
                        functionName = "addGroup",
                        argumentIndex = 0,
                        expected = "group",
                        actual = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                group.addGroup(other)
                GMResult.Ok(receiver)
            }
            "remove", "removePoint" -> groupFunction("removePoint") {
                    arguments,
                    callLocation,
                ->
                val point = when (
                    val result = groupPointArgument(
                        group = group,
                        functionName = "removePoint",
                        arguments = arguments,
                        index = 0,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@groupFunction result
                }
                group.removePoint(point)
                GMResult.Ok(receiver)
            }
            "addParents", "setParents" -> groupFunction(property) {
                    arguments,
                    callLocation,
                ->
                val values = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ArrayValue
                    )?.values ?: arguments
                val ids = mutableListOf<String>()
                for ((index, value) in values.withIndex()) {
                    val id = groupParentId(group, value)
                        ?: return@groupFunction invalidArgumentType(
                            functionName = property,
                            argumentIndex = index,
                            expected = "element, group, or registered id",
                            actual = value,
                            location = callLocation,
                        )
                    ids += id
                }
                if (property == "setParents") {
                    group.setParentIds(ids)
                } else {
                    group.addParentIds(ids)
                }
                GMResult.Ok(receiver)
            }
            "setRotationCenter", "setScaleCenter" ->
                groupFunction(property) { arguments, callLocation ->
                    val center = when (
                        val result = groupCenter(
                            group = group,
                            functionName = property,
                            value = arguments.firstOrNull()
                                ?: JessieCodeRuntimeValue.UndefinedValue,
                            location = callLocation,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@groupFunction result
                    }
                    if (property == "setRotationCenter") {
                        group.setRotationCenter(center)
                    } else {
                        group.setScaleCenter(center)
                    }
                    GMResult.Ok(receiver)
                }
            "setRotationPoints", "setTranslationPoints" ->
                groupFunction(property) { arguments, callLocation ->
                    val values = (
                        arguments.firstOrNull() as?
                            JessieCodeRuntimeValue.ArrayValue
                        )?.values ?: arguments
                    val points = when (
                        val result = groupPointList(
                            group = group,
                            functionName = property,
                            values = values,
                            location = callLocation,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@groupFunction result
                    }
                    if (property == "setRotationPoints") {
                        group.setRotationPoints(points)
                    } else {
                        group.setTranslationPoints(points)
                    }
                    GMResult.Ok(receiver)
                }
            "addRotationPoint", "addTranslationPoint",
            "removeRotationPoint", "removeTranslationPoint",
            "removeScalePoint",
            -> groupFunction(property) { arguments, callLocation ->
                val point = when (
                    val result = groupPointArgument(
                        group = group,
                        functionName = property,
                        arguments = arguments,
                        index = 0,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@groupFunction result
                }
                when (property) {
                    "addRotationPoint" -> group.addRotationPoint(point)
                    "addTranslationPoint" ->
                        group.addTranslationPoint(point)
                    "removeRotationPoint" ->
                        group.removeRotationPoint(point)
                    "removeTranslationPoint" ->
                        group.removeTranslationPoint(point)
                    else -> group.removeScalePoint(point)
                }
                GMResult.Ok(receiver)
            }
            "setScalePoints" -> groupFunction("setScalePoints") {
                    arguments,
                    callLocation,
                ->
                val array = arguments.firstOrNull() as?
                    JessieCodeRuntimeValue.ArrayValue
                val values = array?.values ?: arguments.takeWhile {
                    it !is JessieCodeRuntimeValue.StringValue
                }
                val directionValue =
                    if (array != null) arguments.getOrNull(1)
                    else arguments.getOrNull(values.size)
                val direction = when (directionValue) {
                    null, JessieCodeRuntimeValue.UndefinedValue -> "xy"
                    is JessieCodeRuntimeValue.StringValue ->
                        directionValue.value
                    else -> return@groupFunction invalidArgumentType(
                        functionName = "setScalePoints",
                        argumentIndex =
                            if (array != null) 1 else values.size,
                        expected = "string or undefined",
                        actual = directionValue,
                        location = callLocation,
                    )
                }
                val points = when (
                    val result = groupPointList(
                        group = group,
                        functionName = "setScalePoints",
                        values = values,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@groupFunction result
                }
                group.setScalePoints(points, direction)
                GMResult.Ok(receiver)
            }
            "addScalePoint" -> groupFunction("addScalePoint") {
                    arguments,
                    callLocation,
                ->
                val point = when (
                    val result = groupPointArgument(
                        group = group,
                        functionName = "addScalePoint",
                        arguments = arguments,
                        index = 0,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@groupFunction result
                }
                val direction = when (
                    val value = arguments.getOrNull(1)
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    JessieCodeRuntimeValue.UndefinedValue -> "xy"
                    is JessieCodeRuntimeValue.StringValue -> value.value
                    else -> return@groupFunction invalidArgumentType(
                        functionName = "addScalePoint",
                        argumentIndex = 1,
                        expected = "string or undefined",
                        actual = value,
                        location = callLocation,
                    )
                }
                group.addScalePoint(point, direction)
                GMResult.Ok(receiver)
            }
            "setAttribute", "setProperty" ->
                groupFunction("setAttribute") {
                        arguments,
                        callLocation,
                    ->
                    val attributes = (
                        arguments.firstOrNull() as?
                            JessieCodeRuntimeValue.ObjectValue
                        ) ?: return@groupFunction invalidArgumentType(
                        functionName = "setAttribute",
                        argumentIndex = 0,
                        expected = "object",
                        actual = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                    val converted = when (
                        val result = boardAttributeMap(
                            value = attributes,
                            functionName = "setAttribute",
                            location = callLocation,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@groupFunction result
                    }
                    group.setAttribute(converted)
                    GMResult.Ok(receiver)
                }
            "update" -> groupFunction("update") { _, _ ->
                when (val result = group.updateResult()) {
                    is GMResult.Ok -> GMResult.Ok(receiver)
                    is GMResult.Err -> GMResult.Err(
                        JessieCodeRuntimeError.ElementMethodUnavailable(
                            elementId = group.id,
                            method = "update",
                            reason = result.error.toString(),
                            location = location,
                        ),
                    )
                }
            }
            else -> unknownProperty(receiver, property, location)
        }

    private fun groupFunction(
        name: String,
        callable: JessieCodeCallable,
    ): EvaluationResult =
        GMResult.Ok(
            JessieCodeRuntimeValue.FunctionValue(
                name = name,
                callable = callable,
            ),
        )

    private fun groupElementArray(
        elements: List<CoordsElement>,
    ): JessieCodeRuntimeValue.ArrayValue =
        JessieCodeRuntimeValue.ArrayValue(
            elements.map(JessieCodeRuntimeValue::ElementReference),
        )

    private fun groupPointArgument(
        group: Group,
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        index: Int,
        location: JessieCodeAstLocation,
    ): GMResult<CoordsElement, JessieCodeRuntimeError> {
        val value = arguments.getOrNull(index)
            ?: JessieCodeRuntimeValue.UndefinedValue
        val point = when (value) {
            is JessieCodeRuntimeValue.ElementReference ->
                value.element as? CoordsElement
            is JessieCodeRuntimeValue.StringValue ->
                group.board.select(value.value) as? CoordsElement
            else -> null
        }
        return if (point != null && point.board === group.board) {
            GMResult.Ok(point)
        } else {
            invalidArgumentType(
                functionName = functionName,
                argumentIndex = index,
                expected = "coordinate element on the group board",
                actual = value,
                location = location,
            )
        }
    }

    private fun groupPointList(
        group: Group,
        functionName: String,
        values: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<List<CoordsElement>, JessieCodeRuntimeError> {
        val result = mutableListOf<CoordsElement>()
        for ((index, value) in values.withIndex()) {
            when (
                val point = groupPointArgument(
                    group = group,
                    functionName = functionName,
                    arguments = values,
                    index = index,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result += point.value
                is GMResult.Err -> return point
            }
        }
        return GMResult.Ok(result)
    }

    private fun groupParentId(
        group: Group,
        value: JessieCodeRuntimeValue,
    ): String? =
        when (value) {
            is JessieCodeRuntimeValue.ElementReference ->
                value.element.id.takeIf {
                    value.element.board === group.board
                }
            is JessieCodeRuntimeValue.CompositionReference ->
                (value.composition as? Group)
                    ?.takeIf { it.board === group.board }
                    ?.id
            is JessieCodeRuntimeValue.StringValue ->
                value.value.takeIf {
                    group.board.elementById(it) != null ||
                        group.board.groupById(it) != null
                }
            else -> null
        }

    private fun groupCenter(
        group: Group,
        functionName: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<GroupCenter, JessieCodeRuntimeError> =
        when (value) {
            is JessieCodeRuntimeValue.ElementReference -> {
                val element = value.element as? CoordsElement
                if (element != null && element.board === group.board) {
                    GMResult.Ok(GroupCenter.Element(element))
                } else {
                    invalidArgumentType(
                        functionName = functionName,
                        argumentIndex = 0,
                        expected =
                            "coordinate element on the group board, " +
                                "\"centroid\", coordinate array, or function",
                        actual = value,
                        location = location,
                    )
                }
            }
            is JessieCodeRuntimeValue.StringValue ->
                if (value.value == "centroid") {
                    GMResult.Ok(GroupCenter.Centroid)
                } else {
                    invalidArgumentType(
                        functionName = functionName,
                        argumentIndex = 0,
                        expected = "\"centroid\"",
                        actual = value,
                        location = location,
                    )
                }
            is JessieCodeRuntimeValue.ArrayValue -> {
                val coordinates = DoubleArray(value.values.size)
                for ((index, coordinate) in value.values.withIndex()) {
                    coordinates[index] = (
                        coordinate as?
                            JessieCodeRuntimeValue.NumberValue
                        )?.value ?: return invalidArgumentType(
                        functionName = functionName,
                        argumentIndex = 0,
                        expected = "numeric coordinate array",
                        actual = value,
                        location = location,
                    )
                }
                if (coordinates.size < 2) {
                    invalidArgumentType(
                        functionName = functionName,
                        argumentIndex = 0,
                        expected = "coordinate array of length at least 2",
                        actual = value,
                        location = location,
                    )
                } else {
                    GMResult.Ok(
                        GroupCenter.Coordinates(
                            coordinates.copyOfRange(0, 2),
                        ),
                    )
                }
            }
            is JessieCodeRuntimeValue.FunctionValue -> GMResult.Ok(
                GroupCenter.Dynamic {
                    when (
                        val result = value.externalCallable.call(
                            arguments = emptyList(),
                            location = location,
                        )
                    ) {
                        is GMResult.Err -> GMResult.Err(
                            GroupError.CenterEvaluation(
                                reason = result.error.toString(),
                            ),
                        )
                        is GMResult.Ok -> {
                            val coordinates = (
                                result.value as?
                                    JessieCodeRuntimeValue.ArrayValue
                                )?.values?.mapNotNull {
                                    (
                                        it as?
                                            JessieCodeRuntimeValue.NumberValue
                                        )?.value
                                }
                            if (coordinates == null ||
                                coordinates.size < 2
                            ) {
                                GMResult.Err(
                                    GroupError.CenterEvaluation(
                                        reason =
                                            "Center function must return " +
                                                "a numeric coordinate array",
                                    ),
                                )
                            } else {
                                GMResult.Ok(
                                    doubleArrayOf(
                                        coordinates[0],
                                        coordinates[1],
                                    ),
                                )
                            }
                        }
                    }
                },
            )
            else -> invalidArgumentType(
                functionName = functionName,
                argumentIndex = 0,
                expected =
                    "coordinate element, \"centroid\", " +
                        "coordinate array, or function",
                actual = value,
                location = location,
            )
        }

    private fun groupCenterRuntimeValue(
        center: GroupCenter,
    ): JessieCodeRuntimeValue =
        when (center) {
            GroupCenter.Centroid ->
                JessieCodeRuntimeValue.StringValue("centroid")
            is GroupCenter.Element ->
                JessieCodeRuntimeValue.ElementReference(center.element)
            is GroupCenter.Coordinates ->
                JessieCodeRuntimeValue.ArrayValue(
                    center.coordinates.map(
                        JessieCodeRuntimeValue::NumberValue,
                    ),
                )
            is GroupCenter.Dynamic ->
                JessieCodeRuntimeValue.UndefinedValue
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

    // JSXGraph: src/parser/jessiecode.js -> defineBuiltIn
    private fun standardCallable(name: String): JessieCodeCallable? =
        when (name) {
            "\$" -> JessieCodeCallable { arguments, _ ->
                val id = arguments.firstOrNull()?.let(::propertyKey)
                val element = id?.let { currentBoard?.elementById(it) }
                val group = id?.let { currentBoard?.groupById(it) }
                GMResult.Ok(
                    element?.let {
                        JessieCodeRuntimeValue.ElementReference(it)
                    } ?: group?.let {
                        JessieCodeRuntimeValue.CompositionReference(it)
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
            "X" -> coordinateNumberCallable("X") { it.X() }
            "Y" -> coordinateNumberCallable("Y") { it.Y() }
            "V", "Value" -> JessieCodeCallable { arguments, location ->
                val element = when (
                    val result = elementArgument(
                        functionName = name,
                        arguments = arguments,
                        argumentIndex = 0,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@JessieCodeCallable result
                }
                environment.elementRuntime.valueOf(element, location)
            }
            "L", "Length" -> lengthNumberCallable(name)
            "A", "area", "Area" ->
                areaNumberCallable(name)
            "perimeter", "Perimeter" ->
                perimeterNumberCallable(name)
            "dist", "Dist" -> JessieCodeCallable { arguments, location ->
                val first = when (
                    val result = coordinateElementArgument(
                        functionName = name,
                        arguments = arguments,
                        argumentIndex = 0,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@JessieCodeCallable result
                }
                val second = when (
                    val result = coordinateElementArgument(
                        functionName = name,
                        arguments = arguments,
                        argumentIndex = 1,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@JessieCodeCallable result
                }
                number(first.Dist(second))
            }
            "R", "radius", "Radius" ->
                radiusNumberCallable(name)
            "slope", "Slope" ->
                lineNumberCallable(name) { it.Slope() }
            "getName", "name" -> JessieCodeCallable {
                    arguments,
                    _,
                ->
                val useId = arguments.getOrNull(1)?.let(::isTruthy) ?: false
                val element = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ElementReference
                    )?.element
                when {
                    element == null && useId ->
                        GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
                    element == null ->
                        GMResult.Ok(JessieCodeRuntimeValue.StringValue(""))
                    element.name.isEmpty() && useId ->
                        GMResult.Ok(
                            JessieCodeRuntimeValue.StringValue(element.id),
                        )
                    else -> GMResult.Ok(
                        JessieCodeRuntimeValue.StringValue(element.name),
                    )
                }
            }
            "import" -> JessieCodeCallable { arguments, location ->
                importModules.importModule(arguments, location)
            }
            "deg" -> angleCallable("deg", Geometry::trueAngle)
            "rad" -> angleCallable("rad", Geometry::rad)
            "binomial" -> binaryNumber(Mat::binomial)
            "gcd" -> binaryNumber(Mat::gcd)
            "lcm" -> JessieCodeCallable { arguments, location ->
                val first = arguments.getOrNull(0) as?
                    JessieCodeRuntimeValue.NumberValue
                val second = arguments.getOrNull(1) as?
                    JessieCodeRuntimeValue.NumberValue
                if (first == null || second == null) {
                    number(Double.NaN)
                } else if (first.value * second.value == 0.0) {
                    number(0.0)
                } else {
                    missingMathContext(name, location)
                }
            }
            "ratpow" -> JessieCodeCallable { arguments, location ->
                val numerator = arguments.getOrNull(1)
                val denominator = arguments.getOrNull(2)
                when {
                    numerator is JessieCodeRuntimeValue.NumberValue &&
                        numerator.value == 0.0 -> number(1.0)
                    denominator is JessieCodeRuntimeValue.NumberValue &&
                        denominator.value == 0.0 -> number(Double.NaN)
                    else -> missingMathContext(name, location)
                }
            }
            "randint" -> JessieCodeCallable { arguments, _ ->
                val minimum = toNumber(
                    arguments.getOrElse(0) {
                        JessieCodeRuntimeValue.UndefinedValue
                    },
                )
                val maximum = toNumber(
                    arguments.getOrElse(1) {
                        JessieCodeRuntimeValue.UndefinedValue
                    },
                )
                val stepValue = arguments.getOrNull(2)
                val step = when (stepValue) {
                    null,
                    JessieCodeRuntimeValue.NullValue,
                    JessieCodeRuntimeValue.UndefinedValue,
                    -> 1.0
                    else -> toNumber(stepValue)
                }
                number(
                    JsMath.round(
                        environment.randomSource.nextDouble() *
                            (maximum - minimum) / step,
                    ) * step + minimum,
                )
            }
            "IfThen" -> JessieCodeCallable { arguments, _ ->
                GMResult.Ok(
                    if (
                        isTruthy(
                            arguments.getOrElse(0) {
                                JessieCodeRuntimeValue.UndefinedValue
                            },
                        )
                    ) {
                        arguments.getOrElse(1) {
                            JessieCodeRuntimeValue.UndefinedValue
                        }
                    } else {
                        arguments.getOrElse(2) {
                            JessieCodeRuntimeValue.UndefinedValue
                        }
                    },
                )
            }
            "eval" -> JessieCodeCallable { arguments, location ->
                evaluateBuiltInValue(
                    value = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue,
                    location = location,
                )
            }
            "remove" -> JessieCodeCallable { arguments, _ ->
                when (val value = arguments.firstOrNull()) {
                    is JessieCodeRuntimeValue.ElementReference ->
                        currentBoard?.removeObject(value.element)
                    is JessieCodeRuntimeValue.CompositionReference -> {
                        if (value.composition !is Group) {
                            currentBoard?.removeObject(value.composition)
                        }
                    }
                    else -> Unit
                }
                GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
            }
            "\$log" -> JessieCodeCallable { arguments, _ ->
                logEntries += arguments.toList()
                GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
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

    private fun coordinateNumberCallable(
        functionName: String,
        value: (CoordsElement) -> Double,
    ): JessieCodeCallable =
        JessieCodeCallable { arguments, location ->
            when (
                val result = coordinateElementArgument(
                    functionName = functionName,
                    arguments = arguments,
                    argumentIndex = 0,
                    location = location,
                )
            ) {
                is GMResult.Ok -> number(value(result.value))
                is GMResult.Err -> result
            }
        }

    private fun lineNumberCallable(
        functionName: String,
        value: (Line) -> Double,
    ): JessieCodeCallable =
        JessieCodeCallable { arguments, location ->
            val element = when (
                val result = elementArgument(
                    functionName = functionName,
                    arguments = arguments,
                    argumentIndex = 0,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return@JessieCodeCallable result
            }
            val line = element as? Line
                ?: return@JessieCodeCallable invalidArgumentType(
                    functionName = functionName,
                    argumentIndex = 0,
                    expected = "line",
                    actual = arguments.first(),
                    location = location,
                )
            number(value(line))
        }

    private fun lengthNumberCallable(
        functionName: String,
    ): JessieCodeCallable =
        JessieCodeCallable { arguments, location ->
            val element = when (
                val result = elementArgument(
                    functionName = functionName,
                    arguments = arguments,
                    argumentIndex = 0,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return@JessieCodeCallable result
            }
            when (element) {
                is Line -> number(element.L())
                is Arc -> number(element.L())
                is Sector -> number(element.L())
                else -> invalidArgumentType(
                    functionName = functionName,
                    argumentIndex = 0,
                    expected = "line, arc, or sector",
                    actual = arguments.first(),
                    location = location,
                )
            }
        }

    private fun circleNumberCallable(
        functionName: String,
        value: (Circle) -> Double,
    ): JessieCodeCallable =
        JessieCodeCallable { arguments, location ->
            val element = when (
                val result = elementArgument(
                    functionName = functionName,
                    arguments = arguments,
                    argumentIndex = 0,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return@JessieCodeCallable result
            }
            val circle = element as? Circle
                ?: return@JessieCodeCallable invalidArgumentType(
                    functionName = functionName,
                    argumentIndex = 0,
                    expected = "circle",
                    actual = arguments.first(),
                    location = location,
                )
            number(value(circle))
        }

    private fun radiusNumberCallable(
        functionName: String,
    ): JessieCodeCallable =
        JessieCodeCallable { arguments, location ->
            val element = when (
                val result = elementArgument(
                    functionName = functionName,
                    arguments = arguments,
                    argumentIndex = 0,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return@JessieCodeCallable result
            }
            when (element) {
                is Circle -> number(element.Radius())
                is Arc -> number(element.Radius())
                is Sector -> number(element.Radius())
                else -> invalidArgumentType(
                    functionName = functionName,
                    argumentIndex = 0,
                    expected = "circle, arc, or sector",
                    actual = arguments.first(),
                    location = location,
                )
            }
        }

    private fun areaNumberCallable(
        functionName: String,
    ): JessieCodeCallable =
        JessieCodeCallable { arguments, location ->
            val element = when (
                val result = elementArgument(
                    functionName = functionName,
                    arguments = arguments,
                    argumentIndex = 0,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return@JessieCodeCallable result
            }
            when (element) {
                is Circle -> number(element.Area())
                is Polygon -> number(element.Area())
                is Sector -> number(element.Area())
                else -> invalidArgumentType(
                    functionName = functionName,
                    argumentIndex = 0,
                    expected = "circle, polygon, or sector",
                    actual = arguments.first(),
                    location = location,
                )
            }
        }

    private fun perimeterNumberCallable(
        functionName: String,
    ): JessieCodeCallable =
        JessieCodeCallable { arguments, location ->
            val element = when (
                val result = elementArgument(
                    functionName = functionName,
                    arguments = arguments,
                    argumentIndex = 0,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return@JessieCodeCallable result
            }
            when (element) {
                is Circle -> number(element.Perimeter())
                is Polygon -> number(element.Perimeter())
                is Sector -> number(element.Perimeter())
                else -> invalidArgumentType(
                    functionName = functionName,
                    argumentIndex = 0,
                    expected = "circle, polygon, or sector",
                    actual = arguments.first(),
                    location = location,
                )
            }
        }

    private fun angleCallable(
        functionName: String,
        value: (DoubleArray, DoubleArray, DoubleArray) -> Double,
    ): JessieCodeCallable =
        JessieCodeCallable { arguments, location ->
            val coordinates = mutableListOf<DoubleArray>()
            for (index in 0 until 3) {
                when (
                    val result = coordinateArgument(
                        functionName = functionName,
                        arguments = arguments,
                        argumentIndex = index,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> coordinates += result.value
                    is GMResult.Err -> return@JessieCodeCallable result
                }
            }
            number(value(coordinates[0], coordinates[1], coordinates[2]))
        }

    private fun elementArgument(
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        argumentIndex: Int,
        location: JessieCodeAstLocation,
    ): GMResult<GeometryElement, JessieCodeRuntimeError> {
        val argument = arguments.getOrElse(argumentIndex) {
            JessieCodeRuntimeValue.UndefinedValue
        }
        val element = (
            argument as? JessieCodeRuntimeValue.ElementReference
            )?.element ?: return invalidArgumentType(
            functionName = functionName,
            argumentIndex = argumentIndex,
            expected = "element",
            actual = argument,
            location = location,
        )
        return GMResult.Ok(element)
    }

    private fun coordinateElementArgument(
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        argumentIndex: Int,
        location: JessieCodeAstLocation,
    ): GMResult<CoordsElement, JessieCodeRuntimeError> {
        val element = when (
            val result = elementArgument(
                functionName = functionName,
                arguments = arguments,
                argumentIndex = argumentIndex,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val coordinates = element as? CoordsElement
            ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = argumentIndex,
                expected = "coordinate element",
                actual = arguments[argumentIndex],
                location = location,
            )
        return GMResult.Ok(coordinates)
    }

    private fun coordinateArgument(
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        argumentIndex: Int,
        location: JessieCodeAstLocation,
    ): GMResult<DoubleArray, JessieCodeRuntimeError> {
        val argument = arguments.getOrElse(argumentIndex) {
            JessieCodeRuntimeValue.UndefinedValue
        }
        if (argument is JessieCodeRuntimeValue.ElementReference) {
            val coordinates = argument.element as? CoordsElement
                ?: return invalidArgumentType(
                    functionName = functionName,
                    argumentIndex = argumentIndex,
                    expected = "coordinate element or numeric array",
                    actual = argument,
                    location = location,
                )
            return GMResult.Ok(coordinates.Coords())
        }
        val values = (argument as? JessieCodeRuntimeValue.ArrayValue)?.values
            ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = argumentIndex,
                expected = "coordinate element or numeric array",
                actual = argument,
                location = location,
            )
        val coordinates = DoubleArray(values.size)
        for ((index, coordinate) in values.withIndex()) {
            val number = (
                coordinate as? JessieCodeRuntimeValue.NumberValue
                )?.value ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = argumentIndex,
                expected = "numeric coordinate array",
                actual = coordinate,
                location = location,
            )
            coordinates[index] = number
        }
        return GMResult.Ok(coordinates)
    }

    private fun invalidArgumentType(
        functionName: String,
        argumentIndex: Int,
        expected: String,
        actual: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult.Err<JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.InvalidArgumentType(
                functionName = functionName,
                argumentIndex = argumentIndex,
                expected = expected,
                actual = typeName(actual),
                location = location,
            ),
        )

    private fun missingMathContext(
        functionName: String,
        location: JessieCodeAstLocation,
    ): EvaluationResult =
        GMResult.Err(
            JessieCodeRuntimeError.BuiltInInvocationFailure(
                functionName = functionName,
                reason =
                    "JSXGraph 1.13.3 interpreter calls this " +
                        "Mat method without its required context",
                location = location,
            ),
        )

    // JSXGraph: src/utils/type.js -> evaluate
    private fun evaluateBuiltInValue(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
        depth: Int = 1,
        activeArrays: MutableList<JessieCodeRuntimeValue.ArrayValue> =
            mutableListOf(),
    ): EvaluationResult {
        if (depth > limits.maxEvaluationDepth) {
            return GMResult.Err(
                JessieCodeRuntimeError.EvaluationDepthLimitExceeded(
                    limit = limits.maxEvaluationDepth,
                    location = location,
                ),
            )
        }
        evaluationSteps += 1
        if (evaluationSteps > limits.maxEvaluationSteps) {
            return GMResult.Err(
                JessieCodeRuntimeError.EvaluationStepLimitExceeded(
                    limit = limits.maxEvaluationSteps,
                    location = location,
                ),
            )
        }
        return when (value) {
            is JessieCodeRuntimeValue.FunctionValue ->
                value.callable.call(emptyList(), location)
            is JessieCodeRuntimeValue.ArrayValue -> {
                if (value.values.size > limits.maxCollectionSize) {
                    return GMResult.Err(
                        JessieCodeRuntimeError.CollectionSizeLimitExceeded(
                            limit = limits.maxCollectionSize,
                            requestedSize = value.values.size.toLong(),
                            location = location,
                        ),
                    )
                }
                if (activeArrays.any { it === value }) {
                    return GMResult.Err(
                        JessieCodeRuntimeError.BuiltInInvocationFailure(
                            functionName = "eval",
                            reason = "cyclic array",
                            location = location,
                        ),
                    )
                }
                activeArrays += value
                val evaluated =
                    mutableListOf<JessieCodeRuntimeValue>()
                for (item in value.values) {
                    when (
                        val result = evaluateBuiltInValue(
                            value = item,
                            location = location,
                            depth = depth + 1,
                            activeArrays = activeArrays,
                        )
                    ) {
                        is GMResult.Ok -> evaluated += result.value
                        is GMResult.Err -> return result
                    }
                }
                activeArrays.removeAt(activeArrays.lastIndex)
                GMResult.Ok(JessieCodeRuntimeValue.ArrayValue(evaluated))
            }
            else -> GMResult.Ok(value)
        }
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
            is JessieCodeRuntimeValue.TransformationReference,
            is JessieCodeRuntimeValue.CompositionReference,
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
                left is JessieCodeRuntimeValue.TransformationReference &&
                    right is
                        JessieCodeRuntimeValue.TransformationReference ->
                    left.transformation === right.transformation
                left is JessieCodeRuntimeValue.CompositionReference &&
                    right is JessieCodeRuntimeValue.CompositionReference ->
                    left.composition === right.composition
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
            is JessieCodeRuntimeValue.TransformationReference,
            is JessieCodeRuntimeValue.CompositionReference,
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
            is JessieCodeRuntimeValue.TransformationReference ->
                "transformation"
            is JessieCodeRuntimeValue.CompositionReference ->
                value.composition.elType.ifEmpty { "composition" }
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
        const val MAX_RUNTIME_VALUE_DEPTH = 32
        val NEGATIVE_ZERO_BITS = (-0.0).toBits()
        val ARRAY_INDEX = Regex("""^(0|[1-9][0-9]*)$""")
        val NAME_COMPARATOR = Comparator<String> { left, right ->
            left.lowercase().compareTo(right.lowercase())
        }
        val BUILT_IN_CONSTANT_NAMES = setOf("PI", "EULER", "\$board")
        val MATH_METHOD_NAMES = setOf(
            "abs",
            "acos",
            "acosh",
            "asin",
            "asinh",
            "atan",
            "atan2",
            "atanh",
            "cbrt",
            "ceil",
            "cos",
            "cosh",
            "exp",
            "floor",
            "log",
            "log10",
            "log2",
            "max",
            "min",
            "pow",
            "round",
            "sign",
            "sin",
            "sinh",
            "sqrt",
            "tan",
            "tanh",
            "trunc",
        )
        val BUILT_IN_CALLABLE_NAMES = MATH_METHOD_NAMES + setOf(
            "\$",
            "\$value",
            "\$log",
            "A",
            "Area",
            "D",
            "Dist",
            "IfThen",
            "L",
            "Length",
            "Perimeter",
            "R",
            "Radius",
            "Slope",
            "V",
            "Value",
            "X",
            "Y",
            "acot",
            "area",
            "binomial",
            "cot",
            "deg",
            "dist",
            "erf",
            "erfc",
            "erfi",
            "eval",
            "factorial",
            "gcd",
            "getName",
            "import",
            "lb",
            "lcm",
            "ld",
            "lg",
            "ln",
            "name",
            "ndtr",
            "ndtri",
            "nthroot",
            "perimeter",
            "rad",
            "radius",
            "randint",
            "ratpow",
            "remove",
            "slope",
        )
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
        val id: Int,
        val parameters: List<String>,
        val locals: MutableMap<String, JessieCodeRuntimeValue>,
        val previous: RuntimeScope?,
    ) {
        var implicitCreatorName: String? = null

        fun snapshot(): JessieCodeScopeSnapshot =
            JessieCodeScopeSnapshot(
                id = id,
                arguments = parameters.toList(),
                locals = locals.toMap(),
                previousId = previous?.id,
            )
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

private fun sameRuntimeValue(
    left: JessieCodeRuntimeValue,
    right: JessieCodeRuntimeValue,
): Boolean =
    when {
        left is JessieCodeRuntimeValue.NumberValue &&
            right is JessieCodeRuntimeValue.NumberValue ->
            left.value == right.value
        left is JessieCodeRuntimeValue.BooleanValue &&
            right is JessieCodeRuntimeValue.BooleanValue ->
            left.value == right.value
        left is JessieCodeRuntimeValue.StringValue &&
            right is JessieCodeRuntimeValue.StringValue ->
            left.value == right.value
        else -> left === right
    }

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
