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
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import com.swithun.jsxgraph.core.utils.Type

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

    data class MeasurementContent(
        val error: MeasurementError,
    ) : TextError

    data class InvalidHtmlSliderValue(
        val value: Double,
        val minimum: Double,
        val maximum: Double,
    ) : TextError

    data class SmartLabelContent(
        val error: SmartLabelError,
    ) : TextError

    data object AnchorBoardMismatch : TextError

    data class AnchorNotRegistered(
        val id: String,
    ) : TextError

    data class Registration(
        val error: BoardError,
    ) : TextError
}

/**
 * Initial translated slice of JXG.Text.
 *
 * This slice covers static and function-valued content, JessieCode value tags,
 * free, constrained, and element-anchored coordinates, Board registration,
 * content replacement, portable size estimates, hit testing, and the
 * coordinate/content update lifecycle. DOM HTML, MathJax/KaTeX, exact browser
 * measurement, and remaining rich-text behavior stay platform-owned.
 */
internal class Text private constructor(
    board: Board,
    coordinates: DoubleArray,
    content: String,
    plaintext: String,
    contentSegments: List<ContentSegment>,
    parse: Boolean,
    internal val digits: Int,
    internal val anchor: GeometryElement? = null,
    internal val relativeCoordinates: DoubleArray? = null,
    internal val isLabel: Boolean = false,
    internal val isFixed: Boolean = false,
    internal val isVisible: Boolean = true,
    internal val strokeColor: String? = null,
    id: String = "",
    name: String? = null,
    needsRegularUpdate: Boolean = true,
    coordinateFunctions: List<JessieCodeCoordinateFunction> = emptyList(),
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
    internal var size: DoubleArray = doubleArrayOf(1.0, 1.0)
        private set
    internal var needsSizeUpdate: Boolean = false
        private set
    private var plaintextOld: String? = null
    private var contentSegments: List<ContentSegment> = contentSegments
    private var dynamicContent:
        (() -> GMResult<String, TextError>)? = null
    private var parseDynamicContent: Boolean = false
    private val parse: Boolean = parse
    internal var contentEvaluationError: TextError? = null
        private set
    internal var measurementDefinition: MeasurementDefinition? = null
    // JSXGraph 1.13.3: src/element/smartlabel.js -> createSmartLabel.
    internal var smartLabelDefinition: SmartLabelDefinition? = null
    // JSXGraph 1.13.3: src/element/button.js, checkbox.js, input.js and
    // src/base/text.js -> createHTMLSlider. These creators extend Text.
    internal var htmlControlDefinition: HtmlControlDefinition? = null
    // JSXGraph: src/base/text.js -> relativeCoords. Stored in CSS pixels and
    // resolved by the platform renderer after the user-coordinate transform.
    internal var screenOffset: DoubleArray = doubleArrayOf(0.0, 0.0)

    init {
        elType = TEXT_ELEMENT_TYPE
    }

    // JSXGraph: src/base/text.js -> setText / _setText /
    // _createFctUpdateText.
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
        dynamicContent = null
        parseDynamicContent = false
        contentEvaluationError = null
        prepareUpdate()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/text.js -> setTextJessieCode.
    internal fun setTextJessieCode(
        content: String,
    ): GMResult<Text, TextError> =
        setText(Type.sanitizeHTML(content))

    // JSXGraph 1.13.3: src/base/text.js -> setText(function).
    internal fun setDynamicText(
        parse: Boolean = this.parse,
        content: () -> GMResult<String, TextError>,
    ): Text {
        orgText = ""
        plaintext = ""
        contentSegments = emptyList()
        dynamicContent = content
        parseDynamicContent = parse
        contentEvaluationError = null
        prepareUpdate()
        return this
    }

    // JSXGraph: src/base/text.js -> update
    override fun update(fromParent: Boolean): Text {
        if (!needsUpdate) {
            return this
        }
        if (anchor == null || relativeCoordinates == null) {
            updateCoords(fromParent)
        } else {
            coords.setCoordinates(
                coordType = Const.COORDS_BY_USER,
                coordinates = anchoredCoordinates(
                    anchor = anchor,
                    relativeCoordinates = relativeCoordinates,
                    isLabel = isLabel,
                ),
            )
        }
        val result = dynamicContent?.invoke()
            ?: evaluateContent(contentSegments, digits)
        when (result) {
            is GMResult.Ok -> {
                plaintext = if (
                    dynamicContent != null &&
                    parseDynamicContent
                ) {
                    replaceSub(replaceSup(result.value))
                } else {
                    result.value
                }
                contentEvaluationError = null
            }
            is GMResult.Err -> {
                plaintext = ""
                contentEvaluationError = result.error
            }
        }
        if (
            (getAttribute("display") as? String)
                ?.lowercase() == "internal"
        ) {
            plaintext = utf8Decode(plaintext)
        }
        checkForSizeUpdate()
        if (needsSizeUpdate) {
            updateSize()
        }
        return this
    }

    // JSXGraph: src/base/text.js -> updateRenderer
    override fun updateRenderer(): Text {
        needsUpdate = false
        return this
    }

    // JSXGraph 1.13.3: src/base/text.js ->
    // priv.HTMLSliderInputEventHandler.
    internal fun HTMLSliderInputEventHandler(
        value: Double,
    ): GMResult<Text, TextError> {
        val slider =
            htmlControlDefinition as? HtmlControlDefinition.Slider
                ?: return GMResult.Err(
                    TextError.UnsupportedParsedContent("htmlslider"),
                )
        if (!value.isFinite() || value !in slider.minimum..slider.maximum) {
            return GMResult.Err(
                TextError.InvalidHtmlSliderValue(
                    value = value,
                    minimum = slider.minimum,
                    maximum = slider.maximum,
                ),
            )
        }
        slider.value = value
        board.update()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/text.js -> hasPoint.
    internal fun hasPoint(
        screenX: Double,
        screenY: Double,
        precision: Double = DEFAULT_HIT_PRECISION,
    ): Boolean {
        var x = screenX
        var y = screenY
        if (transformations.isNotEmpty()) {
            var matrix = Mat.identity(3)
            for (transformation in transformations) {
                when (transformation.updateResult()) {
                    is GMResult.Ok ->
                        matrix = Mat.matMatMult(
                            transformation.matrix,
                            matrix,
                        )
                    is GMResult.Err -> return false
                }
            }
            val inverse = Mat.inverse(matrix)
            if (inverse.isEmpty()) {
                return false
            }
            val transformed = Mat.matVecMult(
                inverse,
                doubleArrayOf(1.0, x, y),
            )
            x = transformed[1]
            y = transformed[2]
        }

        val anchorX =
            (getAttribute("anchorx") as? String)?.lowercase() ?: "left"
        val anchorY =
            (getAttribute("anchory") as? String)?.lowercase() ?: "bottom"
        val left = when (anchorX) {
            "right" -> coords.scrCoords[1] - size[0]
            "middle" -> coords.scrCoords[1] - 0.5 * size[0]
            else -> coords.scrCoords[1]
        }
        val right = left + size[0]
        val bottom = when (anchorY) {
            "top" -> coords.scrCoords[2] + size[1]
            "middle" -> coords.scrCoords[2] + 0.5 * size[1]
            else -> coords.scrCoords[2]
        }
        val top = bottom - size[1]
        return if (
            (getAttribute("dragarea") as? String)
                ?.lowercase() != "small"
        ) {
            x >= left - precision &&
                x < right + precision &&
                y >= top - precision &&
                y <= bottom + precision
        } else {
            y >= top - precision &&
                y <= bottom + precision &&
                (
                    x in (left - precision)..(left + 2.0 * precision) ||
                        x in (right - 2.0 * precision)..(right + precision)
                    )
        }
    }

    // JSXGraph 1.13.3: src/base/text.js -> updateSize.
    // Compose has no DOM node; this is the upstream Canvas/no-renderer branch.
    internal fun updateSize(): Text {
        size = crudeSizeEstimate()
        needsSizeUpdate = false
        return this
    }

    // JSXGraph 1.13.3: src/base/text.js -> crudeSizeEstimate.
    internal fun crudeSizeEstimate(): DoubleArray {
        val fontSize =
            (getAttribute("fontsize") as? Number)?.toDouble()
                ?: DEFAULT_FONT_SIZE
        return doubleArrayOf(
            fontSize * plaintext.length * 0.45,
            fontSize * 0.9,
        )
    }

    // JSXGraph 1.13.3: src/base/text.js -> utf8_decode.
    internal fun utf8Decode(value: String): String =
        UNICODE_ENTITY.replace(value) { match ->
            match.groupValues[1]
                .toIntOrNull(16)
                ?.takeIf { it in Char.MIN_VALUE.code..Char.MAX_VALUE.code }
                ?.toChar()
                ?.toString()
                ?: match.value
        }

    // JSXGraph 1.13.3: src/base/text.js -> getSize.
    // The size property already owns the JVM getSize signature.
    internal fun textSize(): DoubleArray = size.copyOf()

    // JSXGraph 1.13.3: src/base/text.js -> setCoords.
    internal fun setCoords(
        horizontal: Double,
        vertical: Double,
    ): Text {
        if (anchor != null && relativeCoordinates != null) {
            val anchorCoordinates = anchoredCoordinates(
                anchor = anchor,
                relativeCoordinates = doubleArrayOf(0.0, 0.0),
                isLabel = isLabel,
            )
            if (isLabel) {
                screenOffset = doubleArrayOf(
                    (horizontal - anchorCoordinates[0]) * board.unitX,
                    -(vertical - anchorCoordinates[1]) * board.unitY,
                )
                relativeCoordinates[0] = screenOffset[0]
                relativeCoordinates[1] = screenOffset[1]
            } else {
                relativeCoordinates[0] =
                    horizontal - anchorCoordinates[0]
                relativeCoordinates[1] =
                    vertical - anchorCoordinates[1]
            }
        } else {
            coords.setCoordinates(
                coordType = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(horizontal, vertical),
            )
        }
        fullUpdate()
        return this
    }

    internal fun setCoords(coordinates: DoubleArray): Text =
        setCoords(
            horizontal = coordinates.getOrElse(0) { Double.NaN },
            vertical = coordinates.getOrElse(1) { Double.NaN },
        )

    // JSXGraph 1.13.3: src/base/text.js -> checkForSizeUpdate.
    internal fun checkForSizeUpdate(): Text {
        needsSizeUpdate = plaintextOld != plaintext
        if (needsSizeUpdate) {
            plaintextOld = plaintext
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/text.js -> expandShortMath.
    internal fun expandShortMath(expression: String): String =
        SHORT_MATH_PATTERN.replace(expression) { match ->
            match.groupValues[1] + "*" + match.groupValues[2]
        }

    // JSXGraph 1.13.3: src/base/text.js -> generateTerm.
    // The Kotlin runtime consumes the equivalent compiled segments directly.
    internal fun generateTerm(
        content: String,
        expand: Boolean = false,
    ): String {
        var remaining = content
            .replace("\r", "")
            .replace("\n", "")
            .replace("\"", "'")
            .replace("'", "\\'")
            .replace("&amp;arc;", "&ang;")
            .replace(Regex("<arc\\s*/?>"), "&ang;")
            .replace(Regex("&lt;arc\\s*/?&gt;"), "&ang;")
            .replace(Regex("&lt;sqrt\\s*/?&gt;"), "&radic;")
            .replace("&lt;value&gt;", VALUE_TAG_OPEN)
            .replace("&lt;/value&gt;", VALUE_TAG_CLOSE)
        val output = StringBuilder("\"\"")
        while (true) {
            val open = remaining.indexOf(VALUE_TAG_OPEN)
            if (open < 0) {
                break
            }
            val close = remaining.indexOf(
                VALUE_TAG_CLOSE,
                open + VALUE_TAG_OPEN.length,
            )
            if (close < 0) {
                break
            }
            output.append(" + \"")
                .append(replaceSub(replaceSup(remaining.substring(0, open))))
                .append("\"")
            var term = remaining
                .substring(open + VALUE_TAG_OPEN.length, close)
                .filterNot(Char::isWhitespace)
            if (expand) {
                term = expandShortMath(term)
            }
            output.append("+(")
                .append(term.replace("\\\"", "'").replace("\\'", "'"))
                .append(")")
            remaining = remaining.substring(close + VALUE_TAG_CLOSE.length)
        }
        output.append(" + \"")
            .append(replaceSub(replaceSup(remaining)))
            .append("\"")
        return output.toString()
            .replace("&amp;", "&")
            .replace("\"", "'")
    }

    internal companion object {
        private const val TEXT_ID_PREFIX = "T"
        private const val TEXT_ELEMENT_TYPE = "text"
        private const val DEFAULT_HIT_PRECISION = 4.0
        private const val DEFAULT_FONT_SIZE = 12.0
        private val UNICODE_ENTITY = Regex("&#x([0-9A-Fa-f]+);")

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
            anchor: GeometryElement? = null,
            isLabel: Boolean = false,
            fixed: Boolean = false,
            visible: Boolean = true,
            strokeColor: String? = null,
        ): GMResult<Text, TextError> {
            if (coordinates.size !in 2..3) {
                return GMResult.Err(
                    TextError.InvalidCoordinateCount(coordinates.size),
                )
            }
            validateAnchor(board, anchor)?.let {
                return GMResult.Err(it)
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
            val relativeCoordinates =
                affineCoordinates(coordinates)
            val text = Text(
                board = board,
                coordinates =
                    if (anchor == null) {
                        coordinates
                    } else {
                        anchoredCoordinates(
                            anchor = anchor,
                            relativeCoordinates = relativeCoordinates,
                            isLabel = isLabel,
                        )
                    },
                content = content,
                plaintext = compiledContent.plaintext,
                contentSegments = compiledContent.segments,
                parse = parse,
                digits = digits,
                anchor = anchor,
                relativeCoordinates =
                    if (anchor == null) null else relativeCoordinates,
                isLabel = isLabel,
                isFixed = fixed,
                isVisible = visible,
                strokeColor = strokeColor,
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
                    anchor?.addChild(text)
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

        // JSXGraph: src/base/coordselement.js -> create / addConstraint.
        internal fun createConstrained(
            board: Board,
            coordinateFunctions: List<JessieCodeCoordinateFunction>,
            content: String,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            parse: Boolean = true,
            digits: Int = 2,
            xjc: String? = null,
            yjc: String? = null,
        ): GMResult<Text, TextError> {
            if (
                coordinateFunctions.size !in 2..3 &&
                !(
                    coordinateFunctions.size == 1 &&
                        coordinateFunctions[0].returnsCoordinateArray
                    )
            ) {
                return GMResult.Err(
                    TextError.InvalidCoordinateCount(
                        coordinateFunctions.size,
                    ),
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
                coordinates =
                    if (
                        coordinateFunctions.size == 2 ||
                        coordinateFunctions.size == 1
                    ) {
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
                coordinateFunctions = coordinateFunctions,
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
                    text.Xjc = xjc
                    text.Yjc = yjc
                    text.applyCoordinateConstraint(initialCoordinates)
                    text.addParentsFromJCFunctions(
                        coordinateFunctions + compiledContent.functions,
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

        private fun validateAnchor(
            board: Board,
            anchor: GeometryElement?,
        ): TextError? {
            if (anchor == null) {
                return null
            }
            if (anchor.board !== board) {
                return TextError.AnchorBoardMismatch
            }
            if (board.elementById(anchor.id) !== anchor) {
                return TextError.AnchorNotRegistered(anchor.id)
            }
            return null
        }

        // JSXGraph: src/base/coordselement.js -> addAnchor;
        // src/base/line.js -> getTextAnchor.
        private fun anchoredCoordinates(
            anchor: GeometryElement,
            relativeCoordinates: DoubleArray,
            isLabel: Boolean,
        ): DoubleArray {
            val anchorCoordinates =
                if (isLabel) {
                    anchor.getLabelAnchor()
                } else {
                    anchor.getTextAnchor()
                }
            if (!isLabel) {
                return doubleArrayOf(
                    anchorCoordinates.usrCoords[1] +
                        relativeCoordinates[0],
                    anchorCoordinates.usrCoords[2] +
                        relativeCoordinates[1],
                )
            }
            return Coords(
                method = Const.COORDS_BY_SCREEN,
                coordinates = doubleArrayOf(
                    anchorCoordinates.scrCoords[1] +
                        relativeCoordinates[0],
                    anchorCoordinates.scrCoords[2] -
                        relativeCoordinates[1],
                ),
                board = anchor.board,
                emitter = false,
            ).usrCoords.copyOfRange(1, 3)
        }

        private fun affineCoordinates(
            coordinates: DoubleArray,
        ): DoubleArray =
            if (coordinates.size == 2) {
                coordinates.copyOf()
            } else {
                doubleArrayOf(
                    coordinates[1] / coordinates[0],
                    coordinates[2] / coordinates[0],
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

        // JSXGraph 1.13.3: src/base/text.js -> replaceSup.
        private fun replaceSup(content: String): String =
            replaceScript(
                content = content,
                marker = '^',
                openTag = "<sup>",
                closeTag = "</sup>",
            )

        // JSXGraph 1.13.3: src/base/text.js -> replaceSub.
        private fun replaceSub(content: String): String =
            replaceScript(
                content = content,
                marker = '_',
                openTag = "<sub>",
                closeTag = "</sub>",
            )

        private fun replaceScript(
            content: String,
            marker: Char,
            openTag: String,
            closeTag: String,
        ): String {
            var output = content
            val bracedMarker = "$marker{"
            var start = output.indexOf(bracedMarker)
            while (start >= 0) {
                output = output.replaceRange(
                    startIndex = start,
                    endIndex = start + bracedMarker.length,
                    replacement = openTag,
                )
                val end = output.indexOf(
                    char = '}',
                    startIndex = start + openTag.length,
                )
                if (end >= 0) {
                    output = output.replaceRange(
                        startIndex = end,
                        endIndex = end + 1,
                        replacement = closeTag,
                    )
                }
                start = output.indexOf(bracedMarker)
            }

            start = output.indexOf(marker)
            while (start >= 0) {
                val valueEnd = minOf(start + 2, output.length)
                val value = output.substring(start + 1, valueEnd)
                output = output.replaceRange(
                    startIndex = start,
                    endIndex = valueEnd,
                    replacement = openTag + value + closeTag,
                )
                start = output.indexOf(marker)
            }
            return output
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
                is JessieCodeRuntimeValue.TransformationReference ->
                    "transformation"
                is JessieCodeRuntimeValue.CompositionReference ->
                    "composition"
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
