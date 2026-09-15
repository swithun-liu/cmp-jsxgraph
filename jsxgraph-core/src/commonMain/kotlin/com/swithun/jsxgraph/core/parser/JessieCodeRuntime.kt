/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> getvar, resolveProperty, and execute
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.GeometryElement

internal data class JessieCodeEvaluatorLimits(
    val maxEvaluationSteps: Int = 100_000,
    val maxEvaluationDepth: Int = 64,
)

internal sealed interface JessieCodeRuntimeError {
    data class InvalidLimits(
        val maxEvaluationSteps: Int,
        val maxEvaluationDepth: Int,
    ) : JessieCodeRuntimeError

    data class EvaluationStepLimitExceeded(
        val limit: Int,
        val location: JessieCodeAstLocation,
    ) : JessieCodeRuntimeError

    data class EvaluationDepthLimitExceeded(
        val limit: Int,
        val location: JessieCodeAstLocation,
    ) : JessieCodeRuntimeError

    data class InvalidAst(
        val reason: String,
        val location: JessieCodeAstLocation,
    ) : JessieCodeRuntimeError

    data class UnsupportedOperation(
        val operator: String,
        val operandTypes: List<String>,
        val location: JessieCodeAstLocation,
    ) : JessieCodeRuntimeError

    data class NotCallable(
        val valueType: String,
        val location: JessieCodeAstLocation,
    ) : JessieCodeRuntimeError

    data class UnknownProperty(
        val receiverType: String,
        val property: String,
        val location: JessieCodeAstLocation,
    ) : JessieCodeRuntimeError

    data class FunctionPropertyAccess(
        val property: String,
        val location: JessieCodeAstLocation,
    ) : JessieCodeRuntimeError

    data class InvalidArgumentCount(
        val functionName: String,
        val expected: String,
        val actual: Int,
        val location: JessieCodeAstLocation,
    ) : JessieCodeRuntimeError

    data class InvalidArgumentType(
        val functionName: String,
        val argumentIndex: Int,
        val expected: String,
        val actual: String,
        val location: JessieCodeAstLocation,
    ) : JessieCodeRuntimeError

    data class ElementValueUnavailable(
        val elementId: String,
        val location: JessieCodeAstLocation,
    ) : JessieCodeRuntimeError

    data class ElementPropertyUnavailable(
        val elementId: String,
        val property: String,
        val location: JessieCodeAstLocation,
    ) : JessieCodeRuntimeError
}

internal fun interface JessieCodeCallable {
    fun call(
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>
}

/**
 * Values visible to the translated JessieCode interpreter.
 *
 * Arrays, objects, functions, boards, and elements intentionally retain
 * identity equality, matching JavaScript object equality.
 */
internal sealed interface JessieCodeRuntimeValue {
    data class NumberValue(
        val value: Double,
    ) : JessieCodeRuntimeValue

    data class BooleanValue(
        val value: Boolean,
    ) : JessieCodeRuntimeValue

    data class StringValue(
        val value: String,
    ) : JessieCodeRuntimeValue

    data object NullValue : JessieCodeRuntimeValue

    data object UndefinedValue : JessieCodeRuntimeValue

    class ArrayValue(
        val values: List<JessieCodeRuntimeValue>,
    ) : JessieCodeRuntimeValue

    class ObjectValue(
        val properties: Map<String, JessieCodeRuntimeValue>,
    ) : JessieCodeRuntimeValue

    class FunctionValue(
        val name: String,
        val callable: JessieCodeCallable,
    ) : JessieCodeRuntimeValue

    class BoardReference(
        val board: Board,
    ) : JessieCodeRuntimeValue

    class ElementReference(
        val element: GeometryElement,
    ) : JessieCodeRuntimeValue
}

/**
 * Adapter for translated element methods and Value() semantics.
 *
 * The complete JSXGraph methodMap and slider contracts are not translated
 * yet. Callers can expose the currently available subset without teaching the
 * evaluator about individual element classes.
 */
internal interface JessieCodeElementRuntime {
    fun valueOf(
        element: GeometryElement,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>

    fun resolveProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>
}

internal object UnsupportedJessieCodeElementRuntime :
    JessieCodeElementRuntime {
    override fun valueOf(
        element: GeometryElement,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.ElementValueUnavailable(
                elementId = element.id,
                location = location,
            ),
        )

    override fun resolveProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.ElementPropertyUnavailable(
                elementId = element.id,
                property = property,
                location = location,
            ),
        )
}

/**
 * First runtime-scope slice.
 *
 * `variables` models the current local scope. Nested function scopes and
 * assignments are added together with their grammar productions.
 */
internal data class JessieCodeRuntimeEnvironment(
    val variables: Map<String, JessieCodeRuntimeValue> = emptyMap(),
    val functions: Map<String, JessieCodeCallable> = emptyMap(),
    val board: Board? = null,
    val elementRuntime: JessieCodeElementRuntime =
        UnsupportedJessieCodeElementRuntime,
)
