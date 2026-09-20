/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/type.js -> createFunction and
 * src/parser/jessiecode.js -> snippet / defineFunction
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.GeometryElement

internal sealed interface JessieCodeExpressionCompileError {
    data class Parser(
        val error: JessieCodeParserError,
    ) : JessieCodeExpressionCompileError

    data class NameReplacement(
        val error: JessieCodeNameReplacementError,
    ) : JessieCodeExpressionCompileError

    data class Dependency(
        val error: JessieCodeDependencyError,
    ) : JessieCodeExpressionCompileError

    data class MultipleStatements(
        val statementCount: Int,
    ) : JessieCodeExpressionCompileError

    data class UnsupportedStatement(
        val operator: String,
    ) : JessieCodeExpressionCompileError
}

/**
 * Parsed JessieCode expression with the stable board references and
 * dependencies used by JSXGraph's Type.createFunction.
 */
internal class JessieCodeExpressionFunction private constructor(
    override val origin: String,
    internal val variableNames: List<String>,
    override val dependencies: Map<String, GeometryElement>,
    private val ast: JessieCodeAstNode,
    private val baseEnvironment: JessieCodeRuntimeEnvironment,
    private val evaluator: JessieCodeEvaluator,
) : JessieCodeCoordinateFunction {
    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        val variables = linkedMapOf<String, JessieCodeRuntimeValue>()
        variables.putAll(baseEnvironment.variables)
        for ((index, name) in variableNames.withIndex()) {
            variables[name] = arguments.getOrElse(index) {
                JessieCodeRuntimeValue.UndefinedValue
            }
        }
        return evaluator.evaluate(
            node = ast,
            environment = baseEnvironment.copy(variables = variables),
        )
    }

    internal companion object {
        // JSXGraph: src/utils/type.js -> createFunction string branch
        internal fun compile(
            source: String,
            board: Board,
            variableNames: List<String> = emptyList(),
            variables: Map<String, JessieCodeRuntimeValue> = emptyMap(),
            functions: Map<String, JessieCodeCallable> = emptyMap(),
            elementRuntime: JessieCodeElementRuntime =
                CoreGeometryElementRuntime,
            lexerLimits: JessieCodeLexerLimits = JessieCodeLexerLimits(),
            parserLimits: JessieCodeParserLimits =
                JessieCodeParserLimits(),
            evaluatorLimits: JessieCodeEvaluatorLimits =
                JessieCodeEvaluatorLimits(),
            dependencyLimits: JessieCodeDependencyLimits =
                JessieCodeDependencyLimits(),
            nameReplacementLimits: JessieCodeNameReplacementLimits =
                JessieCodeNameReplacementLimits(),
            forceValueCall: Boolean = true,
        ): GMResult<
            JessieCodeExpressionFunction,
            JessieCodeExpressionCompileError,
            > {
            val expressionSource = withStatementTerminator(source)
            val parsed = when (
                val result = JessieCodeExpressionParser(
                    lexerLimits = lexerLimits,
                    parserLimits = parserLimits,
                ).parse(expressionSource)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    JessieCodeExpressionCompileError.Parser(
                        result.error,
                    ),
                )
            }
            val statementCount = expressionStatementCount(parsed)
            if (statementCount > 1) {
                return GMResult.Err(
                    JessieCodeExpressionCompileError.MultipleStatements(
                        statementCount = statementCount,
                    ),
                )
            }
            val statementOperator = singleStatement(parsed)
                ?.let(::operationName)
            if (
                statementOperator != null &&
                statementOperator in STATEMENT_ONLY_OPERATIONS
            ) {
                return GMResult.Err(
                    JessieCodeExpressionCompileError.UnsupportedStatement(
                        operator = statementOperator,
                    ),
                )
            }
            val boundNames =
                variableNames.toSet() + variables.keys + functions.keys
            val replaced = when (
                val result = JessieCodeNameReplacer(
                    board = board,
                    limits = nameReplacementLimits,
                ).replace(
                    node = parsed,
                    boundNames = boundNames,
                    forceValueCall = forceValueCall,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    JessieCodeExpressionCompileError.NameReplacement(
                        result.error,
                    ),
                )
            }
            val dependencies = when (
                val result = JessieCodeDependencyCollector(
                    board = board,
                    limits = dependencyLimits,
                ).collect(
                    node = replaced,
                    parameterNames = variableNames.toSet(),
                    localNames = variables.keys + functions.keys,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    JessieCodeExpressionCompileError.Dependency(
                        result.error,
                    ),
                )
            }

            return GMResult.Ok(
                JessieCodeExpressionFunction(
                    origin = source,
                    variableNames = variableNames.toList(),
                    dependencies = dependencies.toMap(),
                    ast = replaced,
                    baseEnvironment = JessieCodeRuntimeEnvironment(
                        variables = variables.toMap(),
                        functions = functions.toMap(),
                        board = board,
                        elementRuntime = elementRuntime,
                    ),
                    evaluator = JessieCodeEvaluator(evaluatorLimits),
                ),
            )
        }

        private fun withStatementTerminator(source: String): String =
            if (source.trimEnd().endsWith(';')) {
                source
            } else {
                "$source;"
            }

        private fun expressionStatementCount(
            program: JessieCodeAstNode,
        ): Int {
            var count = 0
            var current = program
            while (
                current.type == JessieCodeAstNodeType.OPERATION &&
                (current.value as? JessieCodeAstValue.Text)?.value ==
                "op_none"
            ) {
                if (current.children.isEmpty()) {
                    return count
                }
                count += 1
                current = (
                    current.children.firstOrNull() as?
                        JessieCodeAstChild.Node
                    )?.value ?: return count
            }
            return count + 1
        }

        private fun singleStatement(
            program: JessieCodeAstNode,
        ): JessieCodeAstNode? =
            (
                program.children.getOrNull(1) as?
                    JessieCodeAstChild.Node
                )?.value

        private fun operationName(node: JessieCodeAstNode): String? =
            if (node.type == JessieCodeAstNodeType.OPERATION) {
                (node.value as? JessieCodeAstValue.Text)?.value
            } else {
                null
            }

        private val STATEMENT_ONLY_OPERATIONS = setOf(
            "op_block",
            "op_if",
            "op_if_else",
            "op_none",
            "op_while",
            "op_do",
            "op_for",
            "op_return",
            "op_delete",
        )
    }
}
