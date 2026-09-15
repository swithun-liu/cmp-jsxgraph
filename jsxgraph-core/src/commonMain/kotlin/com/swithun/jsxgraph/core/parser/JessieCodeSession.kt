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
    evaluatorLimits: JessieCodeEvaluatorLimits =
        JessieCodeEvaluatorLimits(),
    private val sessionLimits: JessieCodeSessionLimits =
        JessieCodeSessionLimits(),
) {
    private val parser = JessieCodeExpressionParser(
        lexerLimits = lexerLimits,
        parserLimits = parserLimits,
    )
    private val evaluator = JessieCodeEvaluationSession(
        limits = evaluatorLimits,
        environment = environment,
    )
    private val sourceHistory = StringBuilder()

    internal val code: String
        get() = sourceHistory.toString()

    // JSXGraph: src/parser/jessiecode.js -> _genericParse, parse
    internal fun parse(
        source: String,
        storeSource: Boolean = true,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeSessionError> {
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
        return when (val result = evaluator.evaluate(ast)) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                JessieCodeSessionError.Runtime(result.error),
            )
        }
    }
}
