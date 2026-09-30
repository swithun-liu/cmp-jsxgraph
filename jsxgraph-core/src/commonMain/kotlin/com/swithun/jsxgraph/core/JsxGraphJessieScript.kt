/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiescript.js -> Board.construct, Board.addMacro
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

data class JsxGraphJessieScriptLimits(
    val maxSourceLength: Int = 1_000_000,
    val maxStatements: Int = 10_000,
    val maxMacros: Int = 1_000,
    val maxMacroDepth: Int = 64,
    val maxGeneratedSourceLength: Int = 1_000_000,
    val jessieCode: JsxGraphJessieCodeLimits = JsxGraphJessieCodeLimits(),
)

sealed interface JsxGraphJessieScriptError {
    val message: String

    data class InvalidConfiguration(
        override val message: String,
    ) : JsxGraphJessieScriptError

    data class SourceLengthExceeded(
        val limit: Int,
        val actual: Int,
    ) : JsxGraphJessieScriptError {
        override val message =
            "Legacy construction length $actual exceeds limit $limit"
    }

    data class StatementLimitExceeded(
        val limit: Int,
    ) : JsxGraphJessieScriptError {
        override val message =
            "Legacy construction statement count exceeds limit $limit"
    }

    data class MacroLimitExceeded(
        val limit: Int,
    ) : JsxGraphJessieScriptError {
        override val message =
            "Legacy construction macro count exceeds limit $limit"
    }

    data class MacroDepthExceeded(
        val limit: Int,
    ) : JsxGraphJessieScriptError {
        override val message =
            "Legacy construction macro depth exceeds limit $limit"
    }

    data class GeneratedSourceLengthExceeded(
        val limit: Int,
    ) : JsxGraphJessieScriptError {
        override val message =
            "Generated JessieCode exceeds length limit $limit"
    }

    data class MalformedStatement(
        val index: Int,
        val source: String,
    ) : JsxGraphJessieScriptError {
        override val message =
            "Malformed legacy construction statement $index: $source"
    }

    data class UnknownReference(
        val index: Int,
        val name: String,
    ) : JsxGraphJessieScriptError {
        override val message =
            "Unknown legacy construction reference '$name' at statement $index"
    }

    data class UnknownMacro(
        val index: Int,
        val name: String,
    ) : JsxGraphJessieScriptError {
        override val message =
            "Unknown legacy construction macro '$name' at statement $index"
    }

    data class MacroArgumentCount(
        val index: Int,
        val name: String,
        val expected: Int,
        val actual: Int,
    ) : JsxGraphJessieScriptError {
        override val message =
            "Legacy macro '$name' at statement $index expects $expected " +
                "arguments but received $actual"
    }

    data class UnsupportedPropertyMutation(
        val index: Int,
        val elementName: String,
        val propertyName: String,
    ) : JsxGraphJessieScriptError {
        override val message =
            "Legacy property mutation '$elementName.$propertyName' is not " +
                "supported"
    }

    data class UnsupportedDraftModifier(
        val index: Int,
    ) : JsxGraphJessieScriptError {
        override val message =
            "Legacy draft styling is not translated at statement $index"
    }

    data class JessieCode(
        val error: JsxGraphJessieCodeError,
    ) : JsxGraphJessieScriptError {
        override val message = error.message
    }
}

class JsxGraphJessieScriptSession internal constructor(
    private val delegate: JsxGraphJessieCodeSession,
) {
    val scene: JsxGraphScene
        get() = delegate.scene

    fun movePoint(
        id: String,
        coordinates: JsxGraphPoint2D,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> =
        delegate.movePoint(id, coordinates)

    fun interactControl(
        interaction: JsxGraphControlInteraction,
    ): GMResult<JsxGraphScene, JsxGraphInteractionError> =
        delegate.interactControl(interaction)
}

/**
 * Compatibility entry point for the pre-JessieCode construction syntax.
 *
 * The source is compiled into the translated JessieCode subset and executed
 * on its bounded native Board. No JavaScript engine is involved.
 */
object JsxGraphJessieScript {
    fun parse(
        source: String,
        boardOptions: JsxGraphJessieCodeBoardOptions =
            JsxGraphJessieCodeBoardOptions(),
        limits: JsxGraphJessieScriptLimits =
            JsxGraphJessieScriptLimits(),
    ): GMResult<JsxGraphScene, JsxGraphJessieScriptError> =
        when (val result = createSession(source, boardOptions, limits)) {
            is GMResult.Ok -> GMResult.Ok(result.value.scene)
            is GMResult.Err -> result
        }

    fun createSession(
        source: String,
        boardOptions: JsxGraphJessieCodeBoardOptions =
            JsxGraphJessieCodeBoardOptions(),
        limits: JsxGraphJessieScriptLimits =
            JsxGraphJessieScriptLimits(),
    ): GMResult<JsxGraphJessieScriptSession, JsxGraphJessieScriptError> {
        val compiled = when (
            val result = compile(
                source = source,
                limits = limits,
                boardOptions = boardOptions,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val session = when (
            val result = JsxGraphJessieCode.createSession(
                boardOptions = boardOptions,
                limits = limits.jessieCode,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err ->
                return GMResult.Err(
                    JsxGraphJessieScriptError.JessieCode(result.error),
                )
        }
        return when (val result = session.execute(compiled)) {
            is GMResult.Ok ->
                GMResult.Ok(JsxGraphJessieScriptSession(session))
            is GMResult.Err ->
                GMResult.Err(
                    JsxGraphJessieScriptError.JessieCode(result.error),
                )
        }
    }

    internal fun compile(
        source: String,
        limits: JsxGraphJessieScriptLimits =
            JsxGraphJessieScriptLimits(),
        boardOptions: JsxGraphJessieCodeBoardOptions =
            JsxGraphJessieCodeBoardOptions(),
    ): GMResult<String, JsxGraphJessieScriptError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        if (source.length > limits.maxSourceLength) {
            return GMResult.Err(
                JsxGraphJessieScriptError.SourceLengthExceeded(
                    limit = limits.maxSourceLength,
                    actual = source.length,
                ),
            )
        }
        val width =
            boardOptions.boundingBox.right -
                boardOptions.boundingBox.left
        val compiler = Compiler(
            limits = limits,
            curveMinimum = boardOptions.boundingBox.left - width * 0.1,
            curveMaximum = boardOptions.boundingBox.right + width * 0.1,
        )
        return compiler.compile(source)
    }

    private class Compiler(
        private val limits: JsxGraphJessieScriptLimits,
        private val curveMinimum: Double,
        private val curveMaximum: Double,
    ) {
        private val macros = linkedMapOf<String, Macro>()
        private val globalSymbols = linkedMapOf<String, Symbol>()
        private val generated = StringBuilder()
        private var statementCount = 0
        private var nextVariable = 0

        fun compile(
            source: String,
        ): GMResult<String, JsxGraphJessieScriptError> {
            when (
                val result = compileBlock(
                    source = source,
                    context = ExpansionContext(),
                    depth = 0,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            return GMResult.Ok(generated.toString())
        }

        private fun compileBlock(
            source: String,
            context: ExpansionContext,
            depth: Int,
        ): GMResult<Unit, JsxGraphJessieScriptError> {
            if (depth > limits.maxMacroDepth) {
                return GMResult.Err(
                    JsxGraphJessieScriptError.MacroDepthExceeded(
                        limits.maxMacroDepth,
                    ),
                )
            }
            val statements = when (val result = splitStatements(source)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            for (rawStatement in statements) {
                val statement = rawStatement.trim()
                if (statement.isEmpty()) {
                    continue
                }
                if (statementCount >= limits.maxStatements) {
                    return GMResult.Err(
                        JsxGraphJessieScriptError.StatementLimitExceeded(
                            limits.maxStatements,
                        ),
                    )
                }
                val index = statementCount
                statementCount += 1
                when (
                    val result = compileStatement(
                        statement = statement,
                        index = index,
                        context = context,
                        depth = depth,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            return GMResult.Ok(Unit)
        }

        private fun compileStatement(
            statement: String,
            index: Int,
            context: ExpansionContext,
            depth: Int,
        ): GMResult<Unit, JsxGraphJessieScriptError> {
            if ("Macro" in statement) {
                return addMacro(statement, index)
            }

            val assignment = splitAssignment(statement)
            if (assignment != null && '.' in assignment.first) {
                val path = assignment.first.split('.')
                return GMResult.Err(
                    JsxGraphJessieScriptError.UnsupportedPropertyMutation(
                        index = index,
                        elementName = path.dropLast(1).joinToString("."),
                        propertyName = path.last(),
                    ),
                )
            }

            val assignedName = assignment?.first?.trim().orEmpty()
            var expression = assignment?.second?.trim() ?: statement
            val modifiers = linkedMapOf<String, Any>()
            while (true) {
                val match = MODIFIER.find(expression) ?: break
                expression = match.groupValues[1].trimEnd()
                when (match.groupValues[2]) {
                    "draft" ->
                        return GMResult.Err(
                            JsxGraphJessieScriptError
                                .UnsupportedDraftModifier(index),
                        )
                    "invisible" -> modifiers["visible"] = false
                    "nolabel" -> modifiers["withLabel"] = false
                }
            }

            val macroCall = MACRO_CALL.matchEntire(expression)
            if (macroCall != null && macroCall.groupValues[1] in macros) {
                val name = macroCall.groupValues[1]
                val macro = macros.getValue(name)
                val arguments = splitArguments(macroCall.groupValues[2])
                if (arguments.size != macro.parameters.size) {
                    return GMResult.Err(
                        JsxGraphJessieScriptError.MacroArgumentCount(
                            index = index,
                            name = name,
                            expected = macro.parameters.size,
                            actual = arguments.size,
                        ),
                    )
                }
                val parameters = linkedMapOf<String, Symbol>()
                for ((parameter, argument) in
                    macro.parameters.zip(arguments)
                ) {
                    val symbol = when (
                        val result = resolveReference(
                            argument,
                            index,
                            context,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    parameters[parameter] = symbol
                }
                val prefix = when {
                    assignedName.isEmpty() -> context.prefix
                    context.prefix == null -> assignedName
                    else -> "${context.prefix}.$assignedName"
                }
                return compileBlock(
                    source = macro.body,
                    context = ExpansionContext(
                        prefix = prefix,
                        parameters = parameters,
                    ),
                    depth = depth + 1,
                )
            }

            LINE.matchEntire(expression)?.let { match ->
                return compileLine(
                    match = match,
                    name = assignedName,
                    modifiers = modifiers,
                    index = index,
                    context = context,
                )
            }
            CIRCLE.matchEntire(expression)?.let { match ->
                return compileCircle(
                    match = match,
                    name = assignedName,
                    modifiers = modifiers,
                    index = index,
                    context = context,
                )
            }
            POINT.matchEntire(expression)?.let { match ->
                return compilePoint(
                    match = match,
                    modifiers = modifiers,
                    context = context,
                )
            }
            GLIDER.matchEntire(expression)?.let { match ->
                return compileGlider(
                    match = match,
                    modifiers = modifiers,
                    index = index,
                    context = context,
                )
            }
            if (INTERSECTION.containsMatchIn(expression)) {
                return compileIntersection(
                    expression = expression,
                    name = assignedName,
                    modifiers = modifiers,
                    index = index,
                    context = context,
                )
            }
            PARALLEL_OR_NORMAL.matchEntire(expression)?.let { match ->
                return compileParallelOrNormal(
                    match = match,
                    name = assignedName,
                    modifiers = modifiers,
                    index = index,
                    context = context,
                )
            }
            ANGLE.matchEntire(expression)?.let { match ->
                return compileAngle(
                    match = match,
                    name = assignedName,
                    modifiers = modifiers,
                    index = index,
                    context = context,
                )
            }
            RATIO_POINT.matchEntire(expression)?.let { match ->
                return compileRatioPoint(
                    match = match,
                    name = assignedName,
                    modifiers = modifiers,
                    index = index,
                    context = context,
                )
            }
            FUNCTION_GRAPH.matchEntire(expression)?.let { match ->
                return compileFunctionGraph(
                    match = match,
                    modifiers = modifiers,
                    context = context,
                )
            }
            TEXT.matchEntire(expression)?.let { match ->
                return compileText(
                    match = match,
                    modifiers = modifiers,
                )
            }
            POLYGON.matchEntire(expression)?.let { match ->
                return compilePolygon(
                    match = match,
                    modifiers = modifiers,
                    index = index,
                    context = context,
                )
            }
            return if (macroCall != null) {
                GMResult.Err(
                    JsxGraphJessieScriptError.UnknownMacro(
                        index = index,
                        name = macroCall.groupValues[1],
                    ),
                )
            } else {
                GMResult.Err(
                    JsxGraphJessieScriptError.MalformedStatement(
                        index = index,
                        source = statement,
                    ),
                )
            }
        }

        private fun compileLine(
            match: MatchResult,
            name: String,
            modifiers: Map<String, Any>,
            index: Int,
            context: ExpansionContext,
        ): GMResult<Unit, JsxGraphJessieScriptError> {
            val content = match.groupValues[2].trim()
            val names =
                if (' ' in content) {
                    content.split(WHITESPACE).filter(String::isNotEmpty)
                } else {
                    content.map(Char::toString)
                }
            if (names.size < 2) {
                return malformed(index, match.value)
            }
            val first = when (
                val result = resolveReference(names[0], index, context)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolveReference(names[1], index, context)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return emit(
                type = "line",
                arguments = listOf(first.variable, second.variable),
                name = name,
                defaultWithLabel = name.isNotEmpty(),
                modifiers = modifiers + mapOf(
                    "straightFirst" to (match.groupValues[1] != "["),
                    "straightLast" to (match.groupValues[3] == "["),
                ),
                context = context,
                kind = ElementKind.LINE,
            )
        }

        private fun compileCircle(
            match: MatchResult,
            name: String,
            modifiers: Map<String, Any>,
            index: Int,
            context: ExpansionContext,
        ): GMResult<Unit, JsxGraphJessieScriptError> {
            val arguments = splitArguments(match.groupValues[1])
            if (arguments.size != 2) {
                return malformed(index, match.value)
            }
            val center = when (
                val result = resolveReference(arguments[0], index, context)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val radius = when {
                SEGMENT_REFERENCE.matches(arguments[1]) -> {
                    val segmentMatch =
                        SEGMENT_REFERENCE.matchEntire(arguments[1])
                            ?: return malformed(index, match.value)
                    val segment = segmentMatch.groupValues[1].trim()
                    val names =
                        if (' ' in segment) {
                            segment.split(WHITESPACE)
                                .filter(String::isNotEmpty)
                        } else {
                            segment.map(Char::toString)
                        }
                    if (names.size < 2) {
                        return malformed(index, match.value)
                    }
                    val first = when (
                        val result =
                            resolveReference(names[0], index, context)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    val second = when (
                        val result =
                            resolveReference(names[1], index, context)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    "function () { return " +
                        "dist(${first.variable}, ${second.variable}); }"
                }
                JS_NUMBER.matches(arguments[1].trim()) ->
                    arguments[1].trim()
                else -> when (
                    val result =
                        resolveReference(arguments[1], index, context)
                ) {
                    is GMResult.Ok -> result.value.variable
                    is GMResult.Err -> return result
                }
            }
            return emit(
                type = "circle",
                arguments = listOf(center.variable, radius),
                name = name,
                defaultWithLabel = name.isNotEmpty(),
                modifiers = modifiers,
                context = context,
                kind = ElementKind.CIRCLE,
            )
        }

        private fun compilePoint(
            match: MatchResult,
            modifiers: Map<String, Any>,
            context: ExpansionContext,
        ): GMResult<Unit, JsxGraphJessieScriptError> =
            emit(
                type = "point",
                arguments = listOf(
                    match.groupValues[2].trim(),
                    match.groupValues[3].trim(),
                ),
                name = match.groupValues[1],
                defaultWithLabel = true,
                modifiers = modifiers,
                context = context,
                kind = ElementKind.POINT,
            )

        private fun compileGlider(
            match: MatchResult,
            modifiers: Map<String, Any>,
            index: Int,
            context: ExpansionContext,
        ): GMResult<Unit, JsxGraphJessieScriptError> {
            val name = match.groupValues[1].trim()
            val arguments = splitArguments(match.groupValues[2])
            if (arguments.size !in 1..3) {
                return malformed(index, match.value)
            }
            val host = when (
                val result = resolveReference(arguments[0], index, context)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val x = arguments.getOrNull(1)?.trim().orEmpty().ifEmpty { "0" }
            val y = arguments.getOrNull(2)?.trim().orEmpty().ifEmpty { "0" }
            return emit(
                type = "glider",
                arguments = listOf(x, y, host.variable),
                name = name,
                defaultWithLabel = true,
                modifiers = modifiers,
                context = context,
                kind = ElementKind.POINT,
            )
        }

        private fun compileIntersection(
            expression: String,
            name: String,
            modifiers: Map<String, Any>,
            index: Int,
            context: ExpansionContext,
        ): GMResult<Unit, JsxGraphJessieScriptError> {
            val names = expression.split('&', limit = 2).map(String::trim)
            if (names.size != 2 || names.any(String::isEmpty)) {
                return malformed(index, expression)
            }
            val first = when (
                val result = resolveReference(names[0], index, context)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolveReference(names[1], index, context)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val count =
                if (
                    first.kind in LINE_OR_CURVE &&
                    second.kind in LINE_OR_CURVE
                ) {
                    1
                } else {
                    2
                }
            repeat(count) { branch ->
                val branchName =
                    if (count == 1 || name.isEmpty()) {
                        name
                    } else {
                        "${name}_${branch + 1}"
                    }
                when (
                    val result = emit(
                        type = "intersection",
                        arguments = listOf(
                            first.variable,
                            second.variable,
                            branch.toString(),
                        ),
                        name = branchName,
                        defaultWithLabel = branchName.isNotEmpty(),
                        modifiers = modifiers,
                        context = context,
                        kind = ElementKind.POINT,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            return GMResult.Ok(Unit)
        }

        private fun compileParallelOrNormal(
            match: MatchResult,
            name: String,
            modifiers: Map<String, Any>,
            index: Int,
            context: ExpansionContext,
        ): GMResult<Unit, JsxGraphJessieScriptError> {
            val first = when (
                val result =
                    resolveReference(match.groupValues[2], index, context)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result =
                    resolveReference(match.groupValues[3], index, context)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return emit(
                type =
                    if (match.groupValues[1] == "|") {
                        "parallel"
                    } else {
                        "normal"
                    },
                arguments = listOf(first.variable, second.variable),
                name = name,
                defaultWithLabel = name.isNotEmpty(),
                modifiers = modifiers,
                context = context,
                kind = ElementKind.LINE,
            )
        }

        private fun compileAngle(
            match: MatchResult,
            name: String,
            modifiers: Map<String, Any>,
            index: Int,
            context: ExpansionContext,
        ): GMResult<Unit, JsxGraphJessieScriptError> {
            val arguments = mutableListOf<String>()
            for (position in 1..3) {
                when (
                    val result = resolveReference(
                        match.groupValues[position],
                        index,
                        context,
                    )
                ) {
                    is GMResult.Ok -> arguments += result.value.variable
                    is GMResult.Err -> return result
                }
            }
            return emit(
                type = "angle",
                arguments = arguments,
                name = name,
                defaultWithLabel = name.isNotEmpty(),
                modifiers = modifiers,
                context = context,
                kind = ElementKind.AREA,
                displayName =
                    if (name in GREEK_NAMES) {
                        "&$name;"
                    } else {
                        name
                    },
            )
        }

        private fun compileRatioPoint(
            match: MatchResult,
            name: String,
            modifiers: Map<String, Any>,
            index: Int,
            context: ExpansionContext,
        ): GMResult<Unit, JsxGraphJessieScriptError> {
            val numerator = match.groupValues[1].toDouble()
            val denominator = match.groupValues[2].toDouble()
            val ratio = numerator / denominator
            val first = when (
                val result =
                    resolveReference(match.groupValues[3], index, context)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result =
                    resolveReference(match.groupValues[4], index, context)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val x =
                "function () { return (1-$ratio)*X(${first.variable})+" +
                    "$ratio*X(${second.variable}); }"
            val y =
                "function () { return (1-$ratio)*Y(${first.variable})+" +
                    "$ratio*Y(${second.variable}); }"
            return emit(
                type = "point",
                arguments = listOf(x, y),
                name = name,
                defaultWithLabel = name.isNotEmpty(),
                modifiers = modifiers,
                context = context,
                kind = ElementKind.POINT,
            )
        }

        private fun compileFunctionGraph(
            match: MatchResult,
            modifiers: Map<String, Any>,
            context: ExpansionContext,
        ): GMResult<Unit, JsxGraphJessieScriptError> =
            emit(
                type = "functiongraph",
                arguments = listOf(
                    "function (x) { return " +
                        "${match.groupValues[2].trim()}; }",
                    curveMinimum.toString(),
                    curveMaximum.toString(),
                ),
                name = match.groupValues[1],
                defaultWithLabel = false,
                modifiers = modifiers,
                context = context,
                kind = ElementKind.CURVE,
            )

        private fun compileText(
            match: MatchResult,
            modifiers: Map<String, Any>,
        ): GMResult<Unit, JsxGraphJessieScriptError> =
            emit(
                type = "text",
                arguments = listOf(
                    match.groupValues[2],
                    match.groupValues[3],
                    quote(match.groupValues[1].trim()),
                ),
                name = "",
                defaultWithLabel = false,
                modifiers = modifiers,
                context = ExpansionContext(),
                kind = ElementKind.TEXT,
            )

        private fun compilePolygon(
            match: MatchResult,
            modifiers: Map<String, Any>,
            index: Int,
            context: ExpansionContext,
        ): GMResult<Unit, JsxGraphJessieScriptError> {
            val arguments = mutableListOf<String>()
            for (name in splitArguments(match.groupValues[2])) {
                when (
                    val result = resolveReference(name, index, context)
                ) {
                    is GMResult.Ok -> arguments += result.value.variable
                    is GMResult.Err -> return result
                }
            }
            return emit(
                type = "polygon",
                arguments = arguments,
                name = match.groupValues[1],
                defaultWithLabel = true,
                modifiers = modifiers,
                context = context,
                kind = ElementKind.AREA,
            )
        }

        private fun emit(
            type: String,
            arguments: List<String>,
            name: String,
            defaultWithLabel: Boolean,
            modifiers: Map<String, Any>,
            context: ExpansionContext,
            kind: ElementKind,
            displayName: String = name,
        ): GMResult<Unit, JsxGraphJessieScriptError> {
            val attributes = linkedMapOf<String, Any>()
            if (displayName.isNotEmpty()) {
                attributes["name"] = displayName
            }
            if (defaultWithLabel && "withLabel" !in modifiers) {
                attributes["withLabel"] = true
            }
            attributes.putAll(modifiers)
            val fullName =
                if (name.isEmpty()) {
                    null
                } else if (context.prefix == null) {
                    name
                } else {
                    "${context.prefix}.$name"
                }
            if (context.prefix != null && fullName != null) {
                attributes["id"] = fullName
            }
            val variable =
                if (fullName == null) {
                    null
                } else {
                    "legacy${nextVariable++}"
                }
            val code = buildString {
                if (variable != null) {
                    append(variable)
                    append(" = ")
                }
                append(type)
                append('(')
                append(arguments.joinToString(", "))
                append(')')
                if (attributes.isNotEmpty()) {
                    append(" <<")
                    append(
                        attributes.entries.joinToString(", ") {
                            (key, value) ->
                            "$key: ${literal(value)}"
                        },
                    )
                    append(">>")
                }
                append(";\n")
            }
            if (generated.length + code.length > limits.maxGeneratedSourceLength) {
                return GMResult.Err(
                    JsxGraphJessieScriptError
                        .GeneratedSourceLengthExceeded(
                            limits.maxGeneratedSourceLength,
                        ),
                )
            }
            generated.append(code)
            if (fullName != null && variable != null) {
                val symbol = Symbol(variable, kind)
                globalSymbols[fullName] = symbol
                context.locals[name] = symbol
            }
            return GMResult.Ok(Unit)
        }

        private fun addMacro(
            statement: String,
            index: Int,
        ): GMResult<Unit, JsxGraphJessieScriptError> {
            val match = MACRO_DEFINITION.matchEntire(statement)
                ?: return malformed(index, statement)
            if (macros.size >= limits.maxMacros) {
                return GMResult.Err(
                    JsxGraphJessieScriptError.MacroLimitExceeded(
                        limits.maxMacros,
                    ),
                )
            }
            val name = match.groupValues[1]
            val parameters = splitArguments(match.groupValues[2])
            macros[name] = Macro(
                parameters = parameters,
                body = match.groupValues[3],
            )
            return GMResult.Ok(Unit)
        }

        private fun resolveReference(
            rawName: String,
            index: Int,
            context: ExpansionContext,
        ): GMResult<Symbol, JsxGraphJessieScriptError> {
            val name = rawName.trim()
            context.parameters[name]?.let { return GMResult.Ok(it) }
            context.locals[name]?.let { return GMResult.Ok(it) }
            val prefixed =
                context.prefix?.let { prefix ->
                    globalSymbols["$prefix.$name"]
                }
            val symbol = prefixed ?: globalSymbols[name]
            return symbol?.let { GMResult.Ok(it) }
                ?: GMResult.Err(
                    JsxGraphJessieScriptError.UnknownReference(index, name),
                )
        }

        private fun splitStatements(
            source: String,
        ): GMResult<List<String>, JsxGraphJessieScriptError> {
            val statements = mutableListOf<String>()
            var depth = 0
            var start = 0
            for ((index, character) in source.withIndex()) {
                when (character) {
                    '{' -> depth += 1
                    '}' -> {
                        depth -= 1
                        if (depth < 0) {
                            return malformed(statements.size, source)
                        }
                    }
                    ';' ->
                        if (depth == 0) {
                            statements += source.substring(start, index)
                            start = index + 1
                        }
                }
            }
            if (depth != 0) {
                return malformed(statements.size, source)
            }
            statements += source.substring(start)
            return GMResult.Ok(statements)
        }

        private fun splitAssignment(
            statement: String,
        ): Pair<String, String>? {
            val index = statement.indexOf('=')
            if (index < 0) {
                return null
            }
            return statement.substring(0, index) to
                statement.substring(index + 1)
        }

        private fun splitArguments(source: String): List<String> =
            source.split(',').map(String::trim)

        private fun literal(value: Any): String =
            when (value) {
                is Boolean -> value.toString()
                is String -> quote(value)
                else -> value.toString()
            }

        private fun quote(value: String): String = buildString {
            append('"')
            for (character in value) {
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(character)
                }
            }
            append('"')
        }

        private fun malformed(
            index: Int,
            source: String,
        ): GMResult.Err<JsxGraphJessieScriptError.MalformedStatement> =
            GMResult.Err(
                JsxGraphJessieScriptError.MalformedStatement(index, source),
            )
    }

    private data class Macro(
        val parameters: List<String>,
        val body: String,
    )

    private data class Symbol(
        val variable: String,
        val kind: ElementKind,
    )

    private data class ExpansionContext(
        val prefix: String? = null,
        val parameters: Map<String, Symbol> = emptyMap(),
        val locals: MutableMap<String, Symbol> = linkedMapOf(),
    )

    private enum class ElementKind {
        POINT,
        LINE,
        CIRCLE,
        CURVE,
        AREA,
        TEXT,
    }

    private fun validateLimits(
        limits: JsxGraphJessieScriptLimits,
    ): JsxGraphJessieScriptError.InvalidConfiguration? {
        val message = when {
            limits.maxSourceLength < 0 ->
                "maxSourceLength must not be negative"
            limits.maxStatements < 1 ->
                "maxStatements must be positive"
            limits.maxMacros < 0 ->
                "maxMacros must not be negative"
            limits.maxMacroDepth < 0 ->
                "maxMacroDepth must not be negative"
            limits.maxGeneratedSourceLength < 0 ->
                "maxGeneratedSourceLength must not be negative"
            else -> null
        }
        return message?.let(
            JsxGraphJessieScriptError::InvalidConfiguration,
        )
    }

    private val LINE = Regex("""^([\[\]])(.*)([\[\]])$""")
    private val CIRCLE = Regex("""^k\s*\((.*)\)$""")
    private val POINT = Regex(
        """^([A-Z]+\S*)\s*\(\s*([0-9.\-]+)\s*[,|]\s*([0-9.\-]+)\s*\)$""",
    )
    private val GLIDER = Regex("""^([A-Z]+.*)\((.*)\)$""")
    private val INTERSECTION = Regex(""".*&.*""")
    private val PARALLEL_OR_NORMAL = Regex(
        """^\|([|_])\s*\(\s*(\S*)\s*,\s*(\S*)\s*\)$""",
    )
    private val ANGLE = Regex(
        """^<\s*\(\s*(\S*)\s*,\s*(\S*)\s*,\s*(\S*)\s*\)$""",
    )
    private val RATIO_POINT = Regex(
        """^([0-9]+)/([0-9]+)\(\s*(\S*)\s*,\s*(\S*)\s*\)$""",
    )
    private val FUNCTION_GRAPH = Regex("""^(\S*)\s*:\s*(.*)$""")
    private val TEXT = Regex(
        """^#(.*)\(\s*([0-9])\s*[,|]\s*([0-9])\s*\)$""",
    )
    private val POLYGON = Regex("""^(\S*)\s*\[(.*)]$""")
    private val SEGMENT_REFERENCE = Regex("""^[\[\]](.*)[\[\]]$""")
    private val MACRO_CALL = Regex("""^(\S+)\s*\((.*)\)$""")
    private val MACRO_DEFINITION = Regex(
        """^(?:\s*(\S+)\s*=\s*)?Macro\(([\s\S]*)\)\s*\{([\s\S]*)}$""",
    )
    private val MODIFIER = Regex("""^(.*?)(draft|invisible|nolabel)$""")
    private val JS_NUMBER = Regex(
        """^[+-]?(?:(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?|Infinity)$""",
    )
    private val WHITESPACE = Regex("""\s+""")
    private val LINE_OR_CURVE = setOf(ElementKind.LINE, ElementKind.CURVE)
    private val GREEK_NAMES = setOf(
        "alpha",
        "beta",
        "gamma",
        "delta",
        "epsilon",
        "zeta",
        "eta",
        "theta",
        "iota",
        "kappa",
        "lambda",
        "mu",
        "nu",
        "xi",
        "omicron",
        "pi",
        "rho",
        "sigmaf",
        "sigma",
        "tau",
        "upsilon",
        "phi",
        "chi",
        "psi",
        "omega",
    )
}
