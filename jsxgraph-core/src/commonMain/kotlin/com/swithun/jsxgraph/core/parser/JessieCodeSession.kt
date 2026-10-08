/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> JessieCode constructor, _genericParse,
 * and parse
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult

internal data class JessieCodeSessionLimits(
    val maxStoredSourceLength: Int = 1_000_000,
)

internal sealed interface JessieCodeSessionError {
    data class InvalidLimits(
        val maxStoredSourceLength: Int,
    ) : JessieCodeSessionError

    data class SourceHistoryLimitExceeded(
        val limit: Int,
        val requestedSize: Long,
    ) : JessieCodeSessionError

    data class Parser(
        val error: JessieCodeParserError,
    ) : JessieCodeSessionError

    data class ComputerAlgebra(
        val error: JessieCodeComputerAlgebraError,
    ) : JessieCodeSessionError

    data class Runtime(
        val error: JessieCodeRuntimeError,
    ) : JessieCodeSessionError
}

/**
 * Stateful JessieCode parser and evaluator matching one upstream instance.
 *
 * Locals, function scopes, closures, and the selected Board survive across
 * parse calls. Evaluation budgets are reset for every top-level call.
 */
internal class JessieCodeSession(
    environment: JessieCodeRuntimeEnvironment =
        JessieCodeRuntimeEnvironment(),
    lexerLimits: JessieCodeLexerLimits = JessieCodeLexerLimits(),
    parserLimits: JessieCodeParserLimits = JessieCodeParserLimits(),
    computerAlgebraLimits: JessieCodeComputerAlgebraLimits =
        JessieCodeComputerAlgebraLimits(
            maxOutputNodes = parserLimits.maxAstNodes,
            maxOutputDepth = parserLimits.maxAstDepth,
        ),
    evaluatorLimits: JessieCodeEvaluatorLimits =
        JessieCodeEvaluatorLimits(),
    private val sessionLimits: JessieCodeSessionLimits =
        JessieCodeSessionLimits(),
) {
    private val parser = JessieCodeExpressionParser(
        lexerLimits = lexerLimits,
        parserLimits = parserLimits,
    )
    private val computerAlgebra =
        JessieCodeComputerAlgebra(computerAlgebraLimits)
    private val evaluator = JessieCodeEvaluationSession(
        limits = evaluatorLimits,
        environment = environment,
    )
    private val sourceHistory = StringBuilder()

    internal val code: String
        get() = sourceHistory.toString()

    internal val log: List<List<JessieCodeRuntimeValue>>
        get() = evaluator.log

    // JSXGraph: src/parser/jessiecode.js -> _genericParse, parse
    internal fun parse(
        source: String,
        storeSource: Boolean = true,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeSessionError> {
        val ast = when (val result = getAST(source, storeSource)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return evaluate(ast)
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> getAST.
    internal fun getAST(
        source: String,
        storeSource: Boolean = true,
    ): GMResult<JessieCodeAstNode, JessieCodeSessionError> {
        if (sessionLimits.maxStoredSourceLength < 0) {
            return GMResult.Err(
                JessieCodeSessionError.InvalidLimits(
                    maxStoredSourceLength =
                        sessionLimits.maxStoredSourceLength,
                ),
            )
        }
        if (storeSource) {
            val requestedSize =
                sourceHistory.length.toLong() + source.length.toLong() + 1L
            if (requestedSize > sessionLimits.maxStoredSourceLength) {
                return GMResult.Err(
                    JessieCodeSessionError.SourceHistoryLimitExceeded(
                        limit = sessionLimits.maxStoredSourceLength,
                        requestedSize = requestedSize,
                    ),
                )
            }
            sourceHistory.append(source)
            sourceHistory.append('\n')
        }
        val ast = when (val result = parser.parse(source)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                JessieCodeSessionError.Parser(result.error),
            )
        }
        val transformedAst = when (
            val result = computerAlgebra.transform(ast)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                JessieCodeSessionError.ComputerAlgebra(result.error),
            )
        }
        return GMResult.Ok(transformedAst)
    }

    internal fun evaluate(
        ast: JessieCodeAstNode,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeSessionError> =
        when (val result = evaluator.evaluate(ast)) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                JessieCodeSessionError.Runtime(result.error),
            )
        }

    internal val currentBoard
        get() = evaluator.currentBoard

    internal fun pushScope(
        arguments: List<String>,
    ): JessieCodeScopeSnapshot = evaluator.pushScope(arguments)

    internal fun popScope(): JessieCodeScopeSnapshot =
        evaluator.popScope()

    internal fun letVariable(
        name: String,
        value: JessieCodeRuntimeValue,
    ): JessieCodeScopeSnapshot = evaluator.letVariable(name, value)

    internal fun localVariableScope(
        name: String,
    ): JessieCodeScopeSnapshot? = evaluator.localVariableScope(name)

    internal fun parameterScope(
        name: String,
    ): JessieCodeScopeSnapshot? = evaluator.parameterScope(name)

    internal fun resolve(
        name: String,
    ): JessieCodeRuntimeValue = evaluator.resolve(name)

    internal fun resolveVariable(
        name: String,
        local: Boolean,
        functionName: Boolean,
    ): JessieCodeRuntimeValue =
        evaluator.resolveVariable(name, local, functionName)

    internal fun getElementById(
        id: String,
    ): JessieCodeRuntimeValue = evaluator.getElementById(id)

    internal fun isMathMethod(name: String): Boolean =
        evaluator.isMathMethod(name)

    internal fun isBuiltIn(name: String): Boolean =
        evaluator.isBuiltIn(name)

    internal fun addBuiltIn(
        name: String,
        callable: JessieCodeCallable,
    ): Boolean = evaluator.addBuiltIn(name, callable)

    internal fun callBuiltIn(
        name: String,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        evaluator.callBuiltIn(name, arguments, location)

    internal fun findSymbol(
        value: JessieCodeRuntimeValue,
        scopeId: Int,
    ): JessieCodeSymbol? = evaluator.findSymbol(value, scopeId)

    internal fun assignProperty(
        receiver: JessieCodeRuntimeValue,
        property: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Unit, JessieCodeRuntimeError> =
        evaluator.assignProperty(receiver, property, value, location)

    internal fun evaluateValue(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        evaluator.evaluateValue(value, location)

    internal fun possibleOperands(): JessieCodePossibleOperands =
        evaluator.possibleOperands()
}
