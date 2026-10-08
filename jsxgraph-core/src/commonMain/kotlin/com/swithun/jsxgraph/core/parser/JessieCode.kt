/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> pushScope, popScope, getElementById,
 * log, letvar, isLocalVariable, isParameter, isMathMethod, isBuiltIn,
 * resolve, getvarJS, makeMap, functionCodeJS, mergeAttributes, setProp,
 * manipulate, simplify, format, getAST, resolveType, getLHS,
 * getLHSCompiler, compile, getName, X, Y, V, L, area, perimeter, dist,
 * radius, slope, lt, leq, gt, geq, DDD, ifthen, del, eval, findSymbol,
 * importModule, addBuiltIn, getPossibleOperands, _debug, _error, _warn,
 * and _log
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult

internal enum class JessieCodeResolvedType {
    ANY,
    FUNCTION,
}

internal enum class JessieCodeModule(
    val sourceName: String,
) {
    MATH("math"),
    GEOMETRY("math/geometry"),
    STATISTICS("math/statistics"),
    NUMERICS("math/numerics"),
}

internal enum class JessieCodeLogLevel {
    DEBUG,
    ERROR,
    WARNING,
    LOG,
}

internal data class JessieCodeLogEntry(
    val level: JessieCodeLogLevel,
    val line: Int,
    val message: String,
)

internal sealed interface JessieCodeFacadeError {
    data class Session(
        val error: JessieCodeSessionError,
    ) : JessieCodeFacadeError

    data class Compiler(
        val error: JessieCodeCompileError,
    ) : JessieCodeFacadeError

    data class Runtime(
        val error: JessieCodeRuntimeError,
    ) : JessieCodeFacadeError

    data class InvalidFunctionNode(
        val location: JessieCodeAstLocation,
    ) : JessieCodeFacadeError

    data class UnsupportedDerivativePlaceholder(
        val location: JessieCodeAstLocation,
    ) : JessieCodeFacadeError

    data class UnknownModule(
        val name: String,
    ) : JessieCodeFacadeError

    data class ExplicitError(
        val line: Int,
        val message: String,
    ) : JessieCodeFacadeError

    data class AttributeDepthLimitExceeded(
        val limit: Int,
    ) : JessieCodeFacadeError
}

/**
 * Function-level native facade for the public JessieCode prototype.
 *
 * The facade and parse path share one [JessieCodeEvaluationSession], so scope
 * mutations and added built-ins have the same lifetime as upstream.
 */
@Suppress("FunctionName")
internal class JessieCode(
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
    sessionLimits: JessieCodeSessionLimits = JessieCodeSessionLimits(),
) {
    private val session = JessieCodeSession(
        environment = environment,
        lexerLimits = lexerLimits,
        parserLimits = parserLimits,
        computerAlgebraLimits = computerAlgebraLimits,
        evaluatorLimits = evaluatorLimits,
        sessionLimits = sessionLimits,
    )
    private val diagnosticEntries = mutableListOf<JessieCodeLogEntry>()

    internal val code: String
        get() = session.code

    internal val log: List<List<JessieCodeRuntimeValue>>
        get() = session.log

    internal val diagnostics: List<JessieCodeLogEntry>
        get() = diagnosticEntries.toList()

    internal fun parse(
        source: String,
        storeSource: Boolean = true,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        session.parse(source, storeSource).mapSessionError()

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> pushScope.
    internal fun pushScope(
        arguments: List<String>,
    ): JessieCodeScopeSnapshot = session.pushScope(arguments)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> popScope.
    internal fun popScope(): JessieCodeScopeSnapshot = session.popScope()

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> getElementById.
    internal fun getElementById(id: String): JessieCodeRuntimeValue =
        session.getElementById(id)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> log.
    internal fun log(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        call("\$log", arguments)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> letvar.
    internal fun letvar(
        name: String,
        value: JessieCodeRuntimeValue,
    ): JessieCodeScopeSnapshot {
        if (isBuiltIn(name)) {
            _warn("\"$name\" is a predefined value.")
        }
        return session.letVariable(name, value)
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> isLocalVariable.
    internal fun isLocalVariable(
        name: String,
    ): JessieCodeScopeSnapshot? = session.localVariableScope(name)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> isParameter.
    internal fun isParameter(
        name: String,
    ): JessieCodeScopeSnapshot? = session.parameterScope(name)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> isMathMethod.
    internal fun isMathMethod(name: String): Boolean =
        session.isMathMethod(name)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> isBuiltIn.
    internal fun isBuiltIn(name: String): Boolean =
        session.isBuiltIn(name)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> resolve.
    internal fun resolve(name: String): JessieCodeRuntimeValue =
        session.resolve(name)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> getvarJS.
    // Native translation: resolve the value instead of generating JS source.
    internal fun getvarJS(
        name: String,
        local: Boolean = false,
        withProperties: Boolean = false,
    ): JessieCodeRuntimeValue {
        @Suppress("UNUSED_VARIABLE")
        val nativePropertiesAreResolvedAtCallSite = withProperties
        return session.resolveVariable(
            name = name,
            local = local,
            functionName = true,
        )
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> makeMap.
    internal fun makeMap(
        function: JessieCodeRuntimeValue.FunctionValue,
    ): JessieCodeRuntimeValue.FunctionValue =
        JessieCodeRuntimeValue.FunctionValue(
            name = function.name,
            callable = function.callable,
            externalCallable = function.externalCallable,
            parameterNames = function.parameterNames,
            isMap = true,
            dependencies = function.dependencies,
            creator = function.creator,
        )

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> functionCodeJS.
    // Native translation: compile the AST to an executable Kotlin runtime
    // function value instead of source for eval().
    internal fun functionCodeJS(
        node: JessieCodeAstNode,
    ): GMResult<
        JessieCodeRuntimeValue.FunctionValue,
        JessieCodeFacadeError,
        > {
        if (
            operationName(node) != "op_function" &&
            operationName(node) != "op_map"
        ) {
            return GMResult.Err(
                JessieCodeFacadeError.InvalidFunctionNode(node.location),
            )
        }
        return when (val result = session.evaluate(node)) {
            is GMResult.Ok -> {
                val function = result.value as?
                    JessieCodeRuntimeValue.FunctionValue
                    ?: return GMResult.Err(
                        JessieCodeFacadeError.InvalidFunctionNode(
                            node.location,
                        ),
                    )
                GMResult.Ok(function)
            }
            is GMResult.Err ->
                GMResult.Err(JessieCodeFacadeError.Session(result.error))
        }
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> mergeAttributes.
    internal fun mergeAttributes(
        attributes: List<JessieCodeRuntimeValue.ObjectValue>,
        maximumDepth: Int = DEFAULT_ATTRIBUTE_DEPTH,
    ): GMResult<
        JessieCodeRuntimeValue.ObjectValue,
        JessieCodeFacadeError,
        > {
        val merged = JessieCodeRuntimeValue.ObjectValue(emptyMap())
        for (attribute in attributes) {
            when (
                val result = mergeObject(
                    target = merged,
                    source = attribute,
                    depth = 1,
                    maximumDepth = maximumDepth,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(merged)
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> setProp.
    internal fun setProp(
        receiver: JessieCodeRuntimeValue,
        property: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation = DEFAULT_LOCATION,
    ): GMResult<Unit, JessieCodeFacadeError> =
        when (
            val result = session.assignProperty(
                receiver,
                property,
                value,
                location,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err ->
                GMResult.Err(JessieCodeFacadeError.Runtime(result.error))
        }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> manipulate.
    internal fun manipulate(
        source: String,
        storeSource: Boolean = true,
    ): GMResult<String, JessieCodeFacadeError> =
        parseAndCompile(source, storeSource, JessieCodeFormatOptions())

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> simplify.
    internal fun simplify(
        source: String,
    ): GMResult<String, JessieCodeFacadeError> =
        parseAndCompile(source, storeSource = true, JessieCodeFormatOptions())

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> format.
    internal fun format(
        source: String,
        options: JessieCodeFormatOptions,
    ): GMResult<String, JessieCodeFacadeError> =
        parseAndCompile(source, storeSource = true, options)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> getAST.
    internal fun getAST(
        source: String,
        storeSource: Boolean = true,
    ): GMResult<JessieCodeAstNode, JessieCodeFacadeError> =
        session.getAST(source, storeSource).mapSessionError()

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> resolveType.
    internal fun resolveType(
        name: String,
        node: JessieCodeAstNode,
    ): JessieCodeResolvedType {
        if (
            operationName(node) == "op_execfun" &&
            textValue(nodeChild(node, 0)) == name
        ) {
            return JessieCodeResolvedType.FUNCTION
        }
        if (
            operationName(node) in MATHEMATICAL_OPERATIONS &&
            node.children.any { child ->
                val childNode = (child as? JessieCodeAstChild.Node)?.value
                childNode?.type == JessieCodeAstNodeType.VARIABLE &&
                    textValue(childNode) == name
            }
        ) {
            return JessieCodeResolvedType.ANY
        }
        for (child in node.children) {
            val children = when (child) {
                is JessieCodeAstChild.Node -> listOf(child.value)
                is JessieCodeAstChild.NodeList -> child.value
                else -> emptyList()
            }
            for (nested in children) {
                if (
                    resolveType(name, nested) ==
                    JessieCodeResolvedType.FUNCTION
                ) {
                    return JessieCodeResolvedType.FUNCTION
                }
            }
        }
        return JessieCodeResolvedType.ANY
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> getLHS.
    internal fun getLHS(
        node: JessieCodeAstNode,
    ): GMResult<JessieCodeCompiledLhs, JessieCodeFacadeError> =
        getLHSCompiler(node)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> getLHSCompiler.
    // The descriptor is consumed directly by the native evaluator.
    internal fun getLHSCompiler(
        node: JessieCodeAstNode,
    ): GMResult<JessieCodeCompiledLhs, JessieCodeFacadeError> =
        when (val result = JessieCodeCompiler.getLHSCompiler(node)) {
            is GMResult.Ok -> result
            is GMResult.Err ->
                GMResult.Err(JessieCodeFacadeError.Compiler(result.error))
        }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> compile.
    internal fun compile(
        ast: JessieCodeAstNode,
        options: JessieCodeFormatOptions = JessieCodeFormatOptions(),
    ): GMResult<String, JessieCodeFacadeError> =
        when (val result = JessieCodeCompiler.compile(ast, options)) {
            is GMResult.Ok -> result
            is GMResult.Err ->
                GMResult.Err(JessieCodeFacadeError.Compiler(result.error))
        }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> getName.
    internal fun getName(
        value: JessieCodeRuntimeValue,
        useId: Boolean = false,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        call(
            "getName",
            listOf(value, JessieCodeRuntimeValue.BooleanValue(useId)),
        )

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> X.
    internal fun X(
        value: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        call("X", listOf(value))

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> Y.
    internal fun Y(
        value: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        call("Y", listOf(value))

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> V.
    internal fun V(
        value: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        call("V", listOf(value))

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> L.
    internal fun L(
        value: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        call("L", listOf(value))

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> area.
    internal fun area(
        value: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        call("area", listOf(value))

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> perimeter.
    internal fun perimeter(
        value: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        call("perimeter", listOf(value))

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> dist.
    internal fun dist(
        first: JessieCodeRuntimeValue,
        second: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        call("dist", listOf(first, second))

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> radius.
    internal fun radius(
        value: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        call("radius", listOf(value))

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> slope.
    internal fun slope(
        value: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        call("slope", listOf(value))

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> lt.
    internal fun lt(
        first: JessieCodeRuntimeValue,
        second: JessieCodeRuntimeValue,
    ): Boolean = compare(first, second)?.let { it < 0 } ?: false

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> leq.
    internal fun leq(
        first: JessieCodeRuntimeValue,
        second: JessieCodeRuntimeValue,
    ): Boolean = compare(first, second)?.let { it <= 0 } ?: false

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> gt.
    internal fun gt(
        first: JessieCodeRuntimeValue,
        second: JessieCodeRuntimeValue,
    ): Boolean = compare(first, second)?.let { it > 0 } ?: false

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> geq.
    internal fun geq(
        first: JessieCodeRuntimeValue,
        second: JessieCodeRuntimeValue,
    ): Boolean = compare(first, second)?.let { it >= 0 } ?: false

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> DDD.
    internal fun DDD(
        location: JessieCodeAstLocation = DEFAULT_LOCATION,
    ): GMResult<Nothing, JessieCodeFacadeError> =
        GMResult.Err(
            JessieCodeFacadeError.UnsupportedDerivativePlaceholder(
                location,
            ),
        )

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> ifthen.
    internal fun ifthen(
        condition: JessieCodeRuntimeValue,
        first: JessieCodeRuntimeValue,
        second: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue =
        if (isTruthy(condition)) first else second

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> del.
    internal fun del(
        value: JessieCodeRuntimeValue,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        call("remove", listOf(value))

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> eval.
    internal fun eval(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation = DEFAULT_LOCATION,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        when (val result = session.evaluateValue(value, location)) {
            is GMResult.Ok -> result
            is GMResult.Err ->
                GMResult.Err(JessieCodeFacadeError.Runtime(result.error))
        }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> findSymbol.
    internal fun findSymbol(
        value: JessieCodeRuntimeValue,
        scopeId: Int = -1,
    ): JessieCodeSymbol? = session.findSymbol(value, scopeId)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> importModule.
    internal fun importModule(
        name: String,
    ): GMResult<JessieCodeModule, JessieCodeFacadeError> {
        val module = JessieCodeModule.entries.firstOrNull {
            it.sourceName == name.lowercase()
        } ?: return GMResult.Err(
            JessieCodeFacadeError.UnknownModule(name),
        )
        return GMResult.Ok(module)
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> addBuiltIn.
    internal fun addBuiltIn(
        name: String,
        callable: JessieCodeCallable,
    ): Boolean = session.addBuiltIn(name, callable)

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> getPossibleOperands.
    internal fun getPossibleOperands(): JessieCodePossibleOperands =
        session.possibleOperands()

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> _debug.
    internal fun _debug(message: String) {
        diagnosticEntries += JessieCodeLogEntry(
            JessieCodeLogLevel.DEBUG,
            DEFAULT_LOCATION.line,
            message,
        )
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> _error.
    internal fun _error(
        message: String,
        line: Int = DEFAULT_LOCATION.line,
    ): GMResult<Nothing, JessieCodeFacadeError> {
        diagnosticEntries += JessieCodeLogEntry(
            JessieCodeLogLevel.ERROR,
            line,
            message,
        )
        return GMResult.Err(
            JessieCodeFacadeError.ExplicitError(line, message),
        )
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> _warn.
    internal fun _warn(
        message: String,
        line: Int = DEFAULT_LOCATION.line,
    ) {
        diagnosticEntries += JessieCodeLogEntry(
            JessieCodeLogLevel.WARNING,
            line,
            message,
        )
    }

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> _log.
    internal fun _log(
        message: String,
        line: Int = DEFAULT_LOCATION.line,
    ) {
        diagnosticEntries += JessieCodeLogEntry(
            JessieCodeLogLevel.LOG,
            line,
            message,
        )
    }

    private fun parseAndCompile(
        source: String,
        storeSource: Boolean,
        options: JessieCodeFormatOptions,
    ): GMResult<String, JessieCodeFacadeError> {
        val ast = when (val result = getAST(source, storeSource)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return compile(ast, options)
    }

    private fun call(
        name: String,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation = DEFAULT_LOCATION,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> =
        when (val result = session.callBuiltIn(name, arguments, location)) {
            is GMResult.Ok -> result
            is GMResult.Err ->
                GMResult.Err(JessieCodeFacadeError.Runtime(result.error))
        }

    private fun mergeObject(
        target: JessieCodeRuntimeValue.ObjectValue,
        source: JessieCodeRuntimeValue.ObjectValue,
        depth: Int,
        maximumDepth: Int,
    ): GMResult<Unit, JessieCodeFacadeError> {
        if (depth > maximumDepth) {
            return GMResult.Err(
                JessieCodeFacadeError.AttributeDepthLimitExceeded(
                    maximumDepth,
                ),
            )
        }
        for ((name, sourceValue) in source.properties) {
            val existing = target.properties[name]
            if (
                existing is JessieCodeRuntimeValue.ObjectValue &&
                sourceValue is JessieCodeRuntimeValue.ObjectValue
            ) {
                when (
                    val result = mergeObject(
                        existing,
                        sourceValue,
                        depth + 1,
                        maximumDepth,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            } else {
                target.properties[name] = when (
                    val result = copyAttributeValue(
                        value = sourceValue,
                        depth = depth + 1,
                        maximumDepth = maximumDepth,
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
        maximumDepth: Int,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeFacadeError> {
        if (depth > maximumDepth) {
            return GMResult.Err(
                JessieCodeFacadeError.AttributeDepthLimitExceeded(
                    maximumDepth,
                ),
            )
        }
        return when (value) {
            is JessieCodeRuntimeValue.ObjectValue -> {
                val copy = JessieCodeRuntimeValue.ObjectValue(emptyMap())
                when (
                    val result = mergeObject(
                        target = copy,
                        source = value,
                        depth = depth,
                        maximumDepth = maximumDepth,
                    )
                ) {
                    is GMResult.Ok -> GMResult.Ok(copy)
                    is GMResult.Err -> result
                }
            }
            is JessieCodeRuntimeValue.ArrayValue -> {
                val copied = mutableListOf<JessieCodeRuntimeValue>()
                for (entry in value.values) {
                    when (
                        val result = copyAttributeValue(
                            entry,
                            depth + 1,
                            maximumDepth,
                        )
                    ) {
                        is GMResult.Ok -> copied += result.value
                        is GMResult.Err -> return result
                    }
                }
                GMResult.Ok(JessieCodeRuntimeValue.ArrayValue(copied))
            }
            else -> GMResult.Ok(value)
        }
    }

    private fun compare(
        left: JessieCodeRuntimeValue,
        right: JessieCodeRuntimeValue,
    ): Int? {
        if (
            left is JessieCodeRuntimeValue.StringValue &&
            right is JessieCodeRuntimeValue.StringValue
        ) {
            return left.value.compareTo(right.value)
        }
        val leftNumber = toNumber(left)
        val rightNumber = toNumber(right)
        if (leftNumber.isNaN() || rightNumber.isNaN()) {
            return null
        }
        return leftNumber.compareTo(rightNumber)
    }

    private fun toNumber(value: JessieCodeRuntimeValue): Double =
        when (value) {
            JessieCodeRuntimeValue.UndefinedValue -> Double.NaN
            JessieCodeRuntimeValue.NullValue -> 0.0
            is JessieCodeRuntimeValue.NumberValue -> value.value
            is JessieCodeRuntimeValue.BooleanValue ->
                if (value.value) 1.0 else 0.0
            is JessieCodeRuntimeValue.StringValue ->
                value.value.trim().let {
                    if (it.isEmpty()) 0.0 else it.toDoubleOrNull()
                        ?: Double.NaN
                }
            else -> Double.NaN
        }

    private fun isTruthy(value: JessieCodeRuntimeValue): Boolean =
        when (value) {
            JessieCodeRuntimeValue.NullValue,
            JessieCodeRuntimeValue.UndefinedValue,
            -> false
            is JessieCodeRuntimeValue.BooleanValue -> value.value
            is JessieCodeRuntimeValue.NumberValue ->
                value.value != 0.0 && !value.value.isNaN()
            is JessieCodeRuntimeValue.StringValue -> value.value.isNotEmpty()
            else -> true
        }

    private fun <T> GMResult<T, JessieCodeSessionError>.mapSessionError():
        GMResult<T, JessieCodeFacadeError> =
        when (this) {
            is GMResult.Ok -> this
            is GMResult.Err ->
                GMResult.Err(JessieCodeFacadeError.Session(error))
        }

    private companion object {
        const val DEFAULT_ATTRIBUTE_DEPTH = 64
        val DEFAULT_LOCATION = JessieCodeAstLocation(
            line = 1,
            column = 0,
            endLine = 1,
            endColumn = 0,
        )
        val MATHEMATICAL_OPERATIONS = setOf(
            "op_add",
            "op_sub",
            "op_mul",
            "op_div",
            "op_mod",
            "op_exp",
            "op_neg",
        )
    }
}

private fun operationName(node: JessieCodeAstNode): String? =
    if (node.type == JessieCodeAstNodeType.OPERATION) {
        textValue(node)
    } else {
        null
    }

private fun textValue(node: JessieCodeAstNode?): String? =
    (node?.value as? JessieCodeAstValue.Text)?.value

private fun nodeChild(
    node: JessieCodeAstNode,
    index: Int,
): JessieCodeAstNode? =
    (node.children.getOrNull(index) as? JessieCodeAstChild.Node)?.value
