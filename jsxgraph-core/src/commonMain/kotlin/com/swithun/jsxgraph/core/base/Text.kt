/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/text.js -> Text, setText, update, createText;
 * src/base/coordselement.js -> CoordsElement.create
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.utils.JsNumberFormat

internal sealed interface TextError {
    data class InvalidCoordinateCount(
        val count: Int,
    ) : TextError

    data class CoordinateExpressionCompile(
        val coordinateIndex: Int,
        val error: JessieCodeExpressionCompileError,
    ) : TextError

    data class CoordinateExpressionEvaluation(
        val error: CoordinateConstraintError,
    ) : TextError

    data class ContentTagSyntax(
        val offset: Int,
    ) : TextError

    data class ContentExpressionCompile(
        val expressionIndex: Int,
        val error: JessieCodeExpressionCompileError,
    ) : TextError

    data class ContentExpressionEvaluation(
        val expressionIndex: Int,
        val error: JessieCodeRuntimeError,
    ) : TextError

    data class ContentExpressionResult(
        val expressionIndex: Int,
        val actualType: String,
    ) : TextError

    data class UnsupportedParsedContent(
        val marker: String,
    ) : TextError

    data class Registration(
        val error: BoardError,
    ) : TextError
}

/**
 * Initial translated slice of JXG.Text.
 *
 * This slice covers static string/number content, JessieCode value tags, free
 * or constrained coordinates, Board registration, content replacement, and
 * the coordinate/content update lifecycle. Function-valued content,
 * HTML/MathJax/KaTeX, rich-text conversion, anchors owned by other elements,
 * rotation, measured bounds, and hit testing remain untranslated.
 */
internal class Text private constructor(
    board: Board,
    coordinates: DoubleArray,
    content: String,
    plaintext: String,
    contentSegments: List<ContentSegment>,
    parse: Boolean,
    digits: Int,
    id: String = "",
    name: String? = null,
    needsRegularUpdate: Boolean = true,
    coordinateFunctions: List<JessieCodeExpressionFunction> = emptyList(),
) : CoordsElement(
    board = board,
    coordinates = coordinates,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_TEXT,
    elementClass = Const.OBJECT_CLASS_TEXT,
    needsRegularUpdate = needsRegularUpdate,
    coordinateFunctions = coordinateFunctions,
) {
    internal var orgText: String = content
        private set
    internal var plaintext: String = plaintext
        private set
    private var contentSegments: List<ContentSegment> = contentSegments
    private val parse: Boolean = parse
    private val digits: Int = digits
    internal var contentEvaluationError: TextError? = null
        private set

    init {
        elType = TEXT_ELEMENT_TYPE
    }

    // JSXGraph: src/base/text.js -> setText / _setText
    internal fun setText(content: String): GMResult<Text, TextError> {
        val compiled = when (
            val result = compileContent(
                board = board,
                content = content,
                parse = parse,
                digits = digits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        replaceContentDependencies(compiled.functions)
        orgText = content
        plaintext = compiled.plaintext
        contentSegments = compiled.segments
        contentEvaluationError = null
        prepareUpdate()
        return GMResult.Ok(this)
    }

    // JSXGraph: src/base/text.js -> update
    override fun update(fromParent: Boolean): Text {
        if (!needsUpdate) {
            return this
        }
        updateCoords(fromParent)
        when (val result = evaluateContent(contentSegments, digits)) {
            is GMResult.Ok -> {
                plaintext = result.value
                contentEvaluationError = null
            }
            is GMResult.Err -> {
                plaintext = ""
                contentEvaluationError = result.error
            }
        }
        return this
    }

    // JSXGraph: src/base/text.js -> updateRenderer
    override fun updateRenderer(): Text {
        needsUpdate = false
        return this
    }

    internal companion object {
        private const val TEXT_ID_PREFIX = "T"
        private const val TEXT_ELEMENT_TYPE = "text"

        // JSXGraph: src/base/text.js -> createText;
        // src/base/coordselement.js -> CoordsElement.create
        internal fun create(
            board: Board,
            coordinates: DoubleArray,
            content: String,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            parse: Boolean = true,
            digits: Int = 2,
        ): GMResult<Text, TextError> {
            if (coordinates.size !in 2..3) {
                return GMResult.Err(
                    TextError.InvalidCoordinateCount(coordinates.size),
                )
            }
            val compiledContent = when (
                val result = compileContent(
                    board = board,
                    content = content,
                    parse = parse,
                    digits = digits,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val text = Text(
                board = board,
                coordinates = coordinates,
                content = content,
                plaintext = compiledContent.plaintext,
                contentSegments = compiledContent.segments,
                parse = parse,
                digits = digits,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            text.baseElement = text
            return when (val registration = register(board, text)) {
                is GMResult.Ok -> {
                    text.addParentsFromJCFunctions(
                        compiledContent.functions,
                    )
                    registration
                }
                is GMResult.Err -> registration
            }
        }

        // JSXGraph: src/base/coordselement.js -> addConstraint
        internal fun create(
            board: Board,
            coordinateExpressions: List<String>,
            content: String,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            parse: Boolean = true,
            digits: Int = 2,
        ): GMResult<Text, TextError> {
            if (coordinateExpressions.size !in 2..3) {
                return GMResult.Err(
                    TextError.InvalidCoordinateCount(
                        coordinateExpressions.size,
                    ),
                )
            }
            val functions = when (
                val result = compileCoordinateFunctions(
                    board = board,
                    coordinateExpressions = coordinateExpressions,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val compiledContent = when (
                val result = compileContent(
                    board = board,
                    content = content,
                    parse = parse,
                    digits = digits,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val text = Text(
                board = board,
                coordinates =
                    if (functions.size == 2) {
                        doubleArrayOf(0.0, 0.0)
                    } else {
                        doubleArrayOf(1.0, 0.0, 0.0)
                    },
                content = content,
                plaintext = compiledContent.plaintext,
                contentSegments = compiledContent.segments,
                parse = parse,
                digits = digits,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                coordinateFunctions = functions,
            )
            val initialCoordinates = when (
                val result = text.coordinateConstraintResult()
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    TextError.CoordinateExpressionEvaluation(
                        result.error,
                    ),
                )
            }
            return when (val registration = register(board, text)) {
                is GMResult.Ok -> {
                    if (coordinateExpressions.size == 2) {
                        text.Xjc = coordinateExpressions[0]
                        text.Yjc = coordinateExpressions[1]
                    }
                    text.applyCoordinateConstraint(initialCoordinates)
                    text.addParentsFromJCFunctions(
                        functions + compiledContent.functions,
                    )
                    GMResult.Ok(text)
                }
                is GMResult.Err -> registration
            }
        }

        private fun register(
            board: Board,
            text: Text,
        ): GMResult<Text, TextError> =
            when (val registration = board.setId(text, TEXT_ID_PREFIX)) {
                is GMResult.Ok -> GMResult.Ok(text)
                is GMResult.Err -> GMResult.Err(
                    TextError.Registration(registration.error),
                )
            }

        private fun compileCoordinateFunctions(
            board: Board,
            coordinateExpressions: List<String>,
        ): GMResult<List<JessieCodeExpressionFunction>, TextError> {
            val functions =
                mutableListOf<JessieCodeExpressionFunction>()
            for ((index, source) in coordinateExpressions.withIndex()) {
                when (
                    val result = JessieCodeExpressionFunction.compile(
                        source = source,
                        board = board,
                    )
                ) {
                    is GMResult.Ok -> functions += result.value
                    is GMResult.Err -> return GMResult.Err(
                        TextError.CoordinateExpressionCompile(
                            coordinateIndex = index,
                            error = result.error,
                        ),
                    )
                }
            }
            return GMResult.Ok(functions)
        }

        // JSXGraph: src/base/text.js -> valueTagToJessieCode.
        private fun compileContent(
            board: Board,
            content: String,
            parse: Boolean,
            digits: Int,
        ): GMResult<CompiledContent, TextError> {
            if (!parse) {
                return GMResult.Ok(
                    CompiledContent(
                        segments = listOf(ContentSegment.Literal(content)),
                        plaintext = content,
                    ),
                )
            }
            TEXT_UNSUPPORTED_PARSE_MARKERS.firstOrNull(content::contains)
                ?.let { marker ->
                    return GMResult.Err(
                        TextError.UnsupportedParsedContent(marker),
                    )
                }

            val normalized = content
                .replace("\r", "")
                .replace("\n", "")
                .replace("&lt;value&gt;", VALUE_TAG_OPEN)
                .replace("&lt;/value&gt;", VALUE_TAG_CLOSE)
            val segments = mutableListOf<ContentSegment>()
            var offset = 0
            var expressionIndex = 0
            while (offset < normalized.length) {
                val open = normalized.indexOf(VALUE_TAG_OPEN, offset)
                val close = normalized.indexOf(VALUE_TAG_CLOSE, offset)
                if (open < 0) {
                    if (close >= 0) {
                        return GMResult.Err(
                            TextError.ContentTagSyntax(close),
                        )
                    }
                    segments += ContentSegment.Literal(
                        normalized.substring(offset),
                    )
                    break
                }
                if (close in 0 until open) {
                    return GMResult.Err(
                        TextError.ContentTagSyntax(close),
                    )
                }
                if (open > offset) {
                    segments += ContentSegment.Literal(
                        normalized.substring(offset, open),
                    )
                }
                val expressionStart = open + VALUE_TAG_OPEN.length
                val expressionEnd = normalized.indexOf(
                    VALUE_TAG_CLOSE,
                    expressionStart,
                )
                if (expressionEnd < 0) {
                    return GMResult.Err(
                        TextError.ContentTagSyntax(open),
                    )
                }
                val compactSource = normalized
                    .substring(expressionStart, expressionEnd)
                    .filterNot(Char::isWhitespace)
                val source = SHORT_MATH_PATTERN.replace(
                    compactSource,
                ) { match ->
                    match.groupValues[1] + "*" + match.groupValues[2]
                }
                val function = when (
                    val result = JessieCodeExpressionFunction.compile(
                        source = source,
                        board = board,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        TextError.ContentExpressionCompile(
                            expressionIndex = expressionIndex,
                            error = result.error,
                        ),
                    )
                }
                segments += ContentSegment.Expression(
                    index = expressionIndex,
                    function = function,
                )
                expressionIndex += 1
                offset = expressionEnd + VALUE_TAG_CLOSE.length
            }
            if (segments.isEmpty()) {
                segments += ContentSegment.Literal("")
            }
            val plaintext = when (
                val result = evaluateContent(segments, digits)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return GMResult.Ok(
                CompiledContent(
                    segments = segments,
                    plaintext = plaintext,
                ),
            )
        }

        private fun evaluateContent(
            segments: List<ContentSegment>,
            digits: Int,
        ): GMResult<String, TextError> {
            val output = StringBuilder()
            for (segment in segments) {
                when (segment) {
                    is ContentSegment.Literal ->
                        output.append(segment.value)
                    is ContentSegment.Expression -> {
                        val value = when (
                            val result = segment.function.evaluate()
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return GMResult.Err(
                                TextError.ContentExpressionEvaluation(
                                    expressionIndex = segment.index,
                                    error = result.error,
                                ),
                            )
                        }
                        if (value !is JessieCodeRuntimeValue.NumberValue) {
                            return GMResult.Err(
                                TextError.ContentExpressionResult(
                                    expressionIndex = segment.index,
                                    actualType = runtimeType(value),
                                ),
                            )
                        }
                        output.append(
                            JsNumberFormat.fixed(value.value, digits),
                        )
                    }
                }
            }
            return GMResult.Ok(output.toString())
        }

        private fun runtimeType(value: JessieCodeRuntimeValue): String =
            when (value) {
                is JessieCodeRuntimeValue.NumberValue -> "number"
                is JessieCodeRuntimeValue.BooleanValue -> "boolean"
                is JessieCodeRuntimeValue.StringValue -> "string"
                JessieCodeRuntimeValue.NullValue -> "null"
                JessieCodeRuntimeValue.UndefinedValue -> "undefined"
                is JessieCodeRuntimeValue.ArrayValue -> "array"
                is JessieCodeRuntimeValue.ObjectValue -> "object"
                is JessieCodeRuntimeValue.FunctionValue -> "function"
                is JessieCodeRuntimeValue.BoardReference -> "board"
                is JessieCodeRuntimeValue.ElementReference -> "element"
            }

        private const val VALUE_TAG_OPEN = "<value>"
        private const val VALUE_TAG_CLOSE = "</value>"
        private val SHORT_MATH_PATTERN =
            Regex("([)0-9.])([(a-zA-Z_])")
        private val TEXT_UNSUPPORTED_PARSE_MARKERS = listOf(
            "_",
            "^",
            "<arc",
            "<sqrt",
            "<overline",
            "<arrow",
            "<sketchoicon",
            "<sketchofont",
            "&amp;",
        )
    }

    private fun replaceContentDependencies(
        newFunctions: List<JessieCodeExpressionFunction>,
    ) {
        val coordinateDependencies = coordinateFunctions
            .flatMap { function -> function.dependencies.values }
            .associateBy { dependency -> dependency.id }
        val oldDependencies = contentSegments
            .filterIsInstance<ContentSegment.Expression>()
            .flatMap { segment -> segment.function.dependencies.values }
            .associateBy { dependency -> dependency.id }
        val newDependencies = newFunctions
            .flatMap { function -> function.dependencies.values }
            .associateBy { dependency -> dependency.id }
        for ((id, dependency) in oldDependencies) {
            if (id !in coordinateDependencies && id !in newDependencies) {
                dependency.removeChild(this)
            }
        }
        addParentsFromJCFunctions(newFunctions)
    }

    private sealed interface ContentSegment {
        data class Literal(
            val value: String,
        ) : ContentSegment

        data class Expression(
            val index: Int,
            val function: JessieCodeExpressionFunction,
        ) : ContentSegment
    }

    private data class CompiledContent(
        val segments: List<ContentSegment>,
        val plaintext: String,
    ) {
        val functions: List<JessieCodeExpressionFunction>
            get() = segments
                .filterIsInstance<ContentSegment.Expression>()
                .map(ContentSegment.Expression::function)
    }
}
