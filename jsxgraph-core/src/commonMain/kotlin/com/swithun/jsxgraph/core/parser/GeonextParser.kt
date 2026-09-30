/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/geonext.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.GeometryElement

internal data class GeonextParserLimits(
    val maxSourceLength: Int = 1_000_000,
    val maxOperations: Int = 100_000,
    val maxDepth: Int = 256,
)

internal sealed interface GeonextParserError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : GeonextParserError

    data class SourceLimitExceeded(
        val limit: Int,
        val actual: Int,
    ) : GeonextParserError

    data class OperationLimitExceeded(
        val limit: Int,
    ) : GeonextParserError

    data class DepthLimitExceeded(
        val limit: Int,
    ) : GeonextParserError

    data class StalledPowerReplacement(
        val source: String,
    ) : GeonextParserError

    data object MissingOpeningParenthesis : GeonextParserError

    data object MissingClosingParenthesis : GeonextParserError

    data object MissingElementResolver : GeonextParserError

    data class InvalidDependencyPattern(
        val elementName: String,
    ) : GeonextParserError
}

internal fun interface GeonextElementResolver {
    fun idForName(name: String): String?
}

internal object GeonextParser {
    /*
     * JSXGraph 1.13.3: src/parser/geonext.js ->
     * GeonextParser.replacePow.
     */
    internal fun replacePow(
        source: String,
        limits: GeonextParserLimits = GeonextParserLimits(),
    ): GMResult<String, GeonextParserError> {
        validate(source, limits)?.let { return GMResult.Err(it) }
        var expression = source.replace(POWER_WHITESPACE, "^")
        var previousIndex = -1
        var operations = 0
        var powerIndex = expression.indexOf('^')
        while (
            powerIndex >= 0 &&
            powerIndex < expression.length - 1
        ) {
            if (operations >= limits.maxOperations) {
                return GMResult.Err(
                    GeonextParserError.OperationLimitExceeded(
                        limits.maxOperations,
                    ),
                )
            }
            operations += 1
            if (previousIndex == powerIndex) {
                return GMResult.Err(
                    GeonextParserError.StalledPowerReplacement(
                        expression,
                    ),
                )
            }
            previousIndex = powerIndex

            val left = expression.substring(0, powerIndex)
            val right = expression.substring(powerIndex + 1)
            val leftOperand = when (
                val result = leftOperand(left)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val rightOperand = when (
                val result = rightOperand(right)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (
                leftOperand.start == leftOperand.end ||
                rightOperand.start == rightOperand.end
            ) {
                powerIndex = expression.indexOf('^')
                continue
            }
            expression = buildString {
                append(left.substring(0, leftOperand.start))
                append("pow(")
                append(
                    left.substring(
                        leftOperand.start,
                        leftOperand.end,
                    ),
                )
                append(',')
                append(
                    right.substring(
                        rightOperand.start,
                        rightOperand.end,
                    ),
                )
                append(')')
                append(right.substring(rightOperand.end))
            }
            powerIndex = expression.indexOf('^')
        }
        return GMResult.Ok(expression)
    }

    /*
     * JSXGraph 1.13.3: src/parser/geonext.js ->
     * GeonextParser.replaceIf.
     */
    internal fun replaceIf(
        source: String,
        limits: GeonextParserLimits = GeonextParserLimits(),
    ): GMResult<String, GeonextParserError> {
        validate(source, limits)?.let { return GMResult.Err(it) }
        return replaceIf(
            source = source,
            limits = limits,
            depth = 0,
            operations = OperationCounter(),
        )
    }

    /*
     * JSXGraph 1.13.3: src/parser/geonext.js ->
     * GeonextParser.replaceNameById.
     */
    internal fun replaceNameById(
        source: String,
        resolver: GeonextElementResolver? = null,
        jessieCode: Boolean = false,
        limits: GeonextParserLimits = GeonextParserLimits(),
    ): GMResult<String, GeonextParserError> {
        validate(source, limits)?.let { return GMResult.Err(it) }
        val operations = OperationCounter()
        var expression = source
        for (function in UNARY_ELEMENT_FUNCTIONS) {
            expression = when (
                val result = replaceUnaryReferences(
                    source = expression,
                    function = function,
                    resolver = resolver,
                    jessieCode = jessieCode,
                    limits = limits,
                    operations = operations,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        }
        expression = when (
            val result = replaceFunctionReferences(
                source = expression,
                function = "Dist",
                argumentCount = 2,
                resolver = resolver,
                jessieCode = jessieCode,
                limits = limits,
                operations = operations,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        for (function in ANGLE_ELEMENT_FUNCTIONS) {
            expression = when (
                val result = replaceFunctionReferences(
                    source = expression,
                    function = function,
                    argumentCount = 3,
                    resolver = resolver,
                    jessieCode = jessieCode,
                    limits = limits,
                    operations = operations,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(expression)
    }

    /*
     * JSXGraph 1.13.3: src/parser/geonext.js ->
     * GeonextParser.replaceIdByObj.
     */
    internal fun replaceIdByObj(
        source: String,
        limits: GeonextParserLimits = GeonextParserLimits(),
    ): GMResult<String, GeonextParserError> {
        validate(source, limits)?.let { return GMResult.Err(it) }
        var expression = source
        expression = ELEMENT_VALUE.replace(expression) { match ->
            val method = match.groupValues[1]
            val id = match.groupValues[2]
            val target = "\$('$id')"
            if (method == "V") {
                "$target.Value()"
            } else {
                "$target.$method()"
            }
        }
        expression = DISTANCE.replace(expression) { match ->
            "dist(\$('${match.groupValues[1]}'), " +
                "\$('${match.groupValues[2]}'))"
        }
        expression = DEGREE.replace(expression) { match ->
            "deg(\$('${match.groupValues[1]}')," +
                "\$('${match.groupValues[2]}')," +
                "\$('${match.groupValues[3]}'))"
        }
        expression = RADIAN.replace(expression) { match ->
            "rad(\$('${match.groupValues[1]}')," +
                "\$('${match.groupValues[2]}')," +
                "\$('${match.groupValues[3]}'))"
        }
        expression = NUMERIC_WRAPPER.replace(expression, "($1)")
        return GMResult.Ok(expression)
    }

    /*
     * JSXGraph 1.13.3: src/parser/geonext.js ->
     * GeonextParser.geonext2JS.
     */
    internal fun geonext2JS(
        source: String,
        resolver: GeonextElementResolver? = null,
        limits: GeonextParserLimits = GeonextParserLimits(),
    ): GMResult<String, GeonextParserError> {
        validate(source, limits)?.let { return GMResult.Err(it) }
        var expression = decodeEntities(source)
        expression = when (
            val result = replaceNameById(
                source = expression,
                resolver = resolver,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        expression = when (
            val result = replaceIf(expression, limits)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        expression = when (
            val result = replacePow(expression, limits)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        expression = when (
            val result = replaceIdByObj(expression, limits)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        for ((from, to) in FUNCTION_REPLACEMENTS) {
            val pattern = Regex(
                """(\W|^)(${Regex.escape(from)})""",
                RegexOption.IGNORE_CASE,
            )
            expression = pattern.replace(expression) { match ->
                match.groupValues[1] + to
            }
        }
        return GMResult.Ok(
            expression
                .replace("True", "true")
                .replace("False", "false")
                .replace("fasle", "false")
                .replace("Pi", "PI")
                .replace('"', '\''),
        )
    }

    /*
     * JSXGraph 1.13.3: src/parser/geonext.js ->
     * GeonextParser.findDependencies.
     */
    internal fun findDependencies(
        dependent: GeometryElement,
        source: String,
        board: Board = dependent.board,
        isLabel: (GeometryElement) -> Boolean = { false },
        limits: GeonextParserLimits = GeonextParserLimits(),
    ): GMResult<Unit, GeonextParserError> {
        validate(source, limits)?.let { return GMResult.Err(it) }
        var operations = 0
        for ((name, element) in board.elementsByName) {
            if (operations >= limits.maxOperations) {
                return GMResult.Err(
                    GeonextParserError.OperationLimitExceeded(
                        limits.maxOperations,
                    ),
                )
            }
            operations += 1
            if (
                name == dependent.name ||
                (
                    element.elementClass == Const.OBJECT_CLASS_TEXT &&
                        isLabel(element)
                    )
            ) {
                continue
            }
            val escapedName = name
                .replace("[", "\\[")
                .replace("]", "\\]")
            val pattern = try {
                Regex(
                    "\\(([\\w\\[\\]'_ ]+,)*" +
                        "($escapedName)" +
                        "(,[\\w\\[\\]'_ ]+)*\\)",
                )
            } catch (_: IllegalArgumentException) {
                return GMResult.Err(
                    GeonextParserError.InvalidDependencyPattern(name),
                )
            }
            if (pattern.containsMatchIn(source)) {
                element.addChild(dependent)
            }
        }
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/parser/geonext.js ->
     * GeonextParser.gxt2jc.
     */
    internal fun gxt2jc(
        source: String,
        resolver: GeonextElementResolver? = null,
        limits: GeonextParserLimits = GeonextParserLimits(),
    ): GMResult<String, GeonextParserError> {
        validate(source, limits)?.let { return GMResult.Err(it) }
        val expression = when (
            val result = replaceNameById(
                source = decodeEntities(source),
                resolver = resolver,
                jessieCode = true,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            expression
                .replace("True", "true")
                .replace("False", "false")
                .replace("fasle", "false"),
        )
    }

    internal fun resolver(board: Board): GeonextElementResolver =
        GeonextElementResolver { name -> board.elementByName(name)?.id }

    private fun replaceIf(
        source: String,
        limits: GeonextParserLimits,
        depth: Int,
        operations: OperationCounter,
    ): GMResult<String, GeonextParserError> {
        if (depth > limits.maxDepth) {
            return GMResult.Err(
                GeonextParserError.DepthLimitExceeded(limits.maxDepth),
            )
        }
        var index = source.indexOf("If(")
        if (index < 0) {
            return GMResult.Ok(source)
        }
        var expression = source.replace("\"\"", "0")
        var output = ""
        var right = ""
        while (index >= 0) {
            if (operations.value >= limits.maxOperations) {
                return GMResult.Err(
                    GeonextParserError.OperationLimitExceeded(
                        limits.maxOperations,
                    ),
                )
            }
            operations.value += 1
            val left = expression.substring(0, index)
            right = expression.substring(index + 3)
            var count = 1
            var position = 0
            var firstComma = -1
            var secondComma = -1
            while (position < right.length && count > 0) {
                when (right[position]) {
                    ')' -> count -= 1
                    '(' -> count += 1
                    ',' -> if (count == 1) {
                        if (firstComma < 0) {
                            firstComma = position
                        } else {
                            secondComma = position
                        }
                    }
                }
                position += 1
            }
            val meatEnd = (position - 1).coerceAtLeast(0)
            val meat = right.substring(0, meatEnd)
            right = right.substring(position)
            if (firstComma < 0 || secondComma < 0) {
                return GMResult.Ok("")
            }
            val first = when (
                val result = replaceIf(
                    meat.substring(0, firstComma),
                    limits,
                    depth + 1,
                    operations,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = replaceIf(
                    meat.substring(firstComma + 1, secondComma),
                    limits,
                    depth + 1,
                    operations,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val third = when (
                val result = replaceIf(
                    meat.substring(secondComma + 1),
                    limits,
                    depth + 1,
                    operations,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            output +=
                "$left(($first)?($second):($third))"
            expression = right
            index = expression.indexOf("If(")
        }
        return GMResult.Ok(output + right)
    }

    private fun replaceUnaryReferences(
        source: String,
        function: String,
        resolver: GeonextElementResolver?,
        jessieCode: Boolean,
        limits: GeonextParserLimits,
        operations: OperationCounter,
    ): GMResult<String, GeonextParserError> {
        var expression = source
        val prefix = "$function("
        var position = expression.indexOf(prefix)
        while (position >= 0) {
            if (operations.value >= limits.maxOperations) {
                return GMResult.Err(
                    GeonextParserError.OperationLimitExceeded(
                        limits.maxOperations,
                    ),
                )
            }
            operations.value += 1
            val end = expression.indexOf(')', position + 2)
            if (end < 0) {
                return GMResult.Err(
                    GeonextParserError.MissingClosingParenthesis,
                )
            }
            val name = unescapeElementName(
                expression.substring(position + 2, end),
            )
            val id = when (resolver) {
                null -> {
                    return GMResult.Err(
                        GeonextParserError.MissingElementResolver,
                    )
                }
                else -> resolver.idForName(name)
            }
            if (id != null) {
                val replacement =
                    (if (jessieCode) "\$('" else "") +
                        printId(id, jessieCode)
                expression =
                    expression.substring(0, position + 2) +
                    replacement +
                    expression.substring(end)
            }
            val closing = expression.indexOf(')', position + 2)
            position = expression.indexOf(
                prefix,
                if (closing < 0) 0 else closing,
            )
        }
        return GMResult.Ok(expression)
    }

    private fun replaceFunctionReferences(
        source: String,
        function: String,
        argumentCount: Int,
        resolver: GeonextElementResolver?,
        jessieCode: Boolean,
        limits: GeonextParserLimits,
        operations: OperationCounter,
    ): GMResult<String, GeonextParserError> {
        var expression = source
        val prefix = "$function("
        var position = expression.indexOf(prefix)
        while (position >= 0) {
            if (operations.value >= limits.maxOperations) {
                return GMResult.Err(
                    GeonextParserError.OperationLimitExceeded(
                        limits.maxOperations,
                    ),
                )
            }
            operations.value += 1
            var argumentStart = position + prefix.length
            for (argument in 0 until argumentCount) {
                val delimiter = if (argument == argumentCount - 1) ')' else ','
                val end = expression.indexOf(delimiter, argumentStart)
                if (end < 0) {
                    return GMResult.Err(
                        GeonextParserError.MissingClosingParenthesis,
                    )
                }
                val name = unescapeElementName(
                    expression.substring(argumentStart, end),
                )
                val id = when (resolver) {
                    null -> {
                        return GMResult.Err(
                            GeonextParserError.MissingElementResolver,
                        )
                    }
                    else -> resolver.idForName(name)
                }
                if (id != null) {
                    val replacement = printId(id, jessieCode)
                    expression =
                        expression.substring(0, argumentStart) +
                        replacement +
                        expression.substring(end)
                    argumentStart += replacement.length
                } else {
                    argumentStart = end
                }
                argumentStart += 1
            }
            val closing = expression.indexOf(')', position + prefix.length)
            position = expression.indexOf(
                prefix,
                if (closing < 0) 0 else closing,
            )
        }
        return GMResult.Ok(expression)
    }

    private fun leftOperand(
        left: String,
    ): GMResult<OperandRange, GeonextParserError> {
        if (left.isEmpty()) {
            return GMResult.Ok(OperandRange(0, 0))
        }
        if (left.last() != ')') {
            var start = left.length
            while (start > 0 && left[start - 1].isJsWordOrDot()) {
                start -= 1
            }
            return GMResult.Ok(OperandRange(start, left.length))
        }

        var count = 1
        var position = left.length - 2
        while (position >= 0 && count > 0) {
            when (left[position]) {
                ')' -> count += 1
                '(' -> count -= 1
            }
            position -= 1
        }
        if (count != 0) {
            return GMResult.Err(
                GeonextParserError.MissingOpeningParenthesis,
            )
        }
        while (position >= 0 && left[position].isJsWordOrDot()) {
            position -= 1
        }
        return GMResult.Ok(
            OperandRange(position + 1, left.length),
        )
    }

    private fun rightOperand(
        right: String,
    ): GMResult<OperandRange, GeonextParserError> {
        var prefixEnd = 0
        while (
            prefixEnd < right.length &&
            right[prefixEnd].isJsWordOrDot()
        ) {
            prefixEnd += 1
        }
        if (
            prefixEnd < right.length &&
            right[prefixEnd] == '('
        ) {
            var count = 1
            var position = prefixEnd + 1
            while (position < right.length && count > 0) {
                when (right[position]) {
                    ')' -> count -= 1
                    '(' -> count += 1
                }
                position += 1
            }
            if (count != 0) {
                return GMResult.Err(
                    GeonextParserError.MissingClosingParenthesis,
                )
            }
            return GMResult.Ok(OperandRange(0, position))
        }
        return GMResult.Ok(OperandRange(0, prefixEnd))
    }

    private fun validate(
        source: String,
        limits: GeonextParserLimits,
    ): GeonextParserError? {
        val invalid = when {
            limits.maxSourceLength < 0 ->
                "maxSourceLength" to limits.maxSourceLength
            limits.maxOperations < 0 ->
                "maxOperations" to limits.maxOperations
            limits.maxDepth < 0 ->
                "maxDepth" to limits.maxDepth
            else -> null
        }
        if (invalid != null) {
            return GeonextParserError.InvalidLimits(
                name = invalid.first,
                value = invalid.second,
            )
        }
        return if (source.length > limits.maxSourceLength) {
            GeonextParserError.SourceLimitExceeded(
                limit = limits.maxSourceLength,
                actual = source.length,
            )
        } else {
            null
        }
    }

    private fun decodeEntities(source: String): String =
        source
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")

    private fun printId(
        id: String,
        jessieCode: Boolean,
    ): String = if (jessieCode) "\$('$id')" else id

    private fun unescapeElementName(name: String): String =
        ESCAPED_QUOTE.replace(name) { match ->
            match.groupValues[1]
        }

    private fun Char.isJsWordOrDot(): Boolean =
        this == '.' ||
            this == '_' ||
            this in '0'..'9' ||
            this in 'A'..'Z' ||
            this in 'a'..'z'

    private data class OperandRange(
        val start: Int,
        val end: Int,
    )

    private class OperationCounter(
        var value: Int = 0,
    )

    private val POWER_WHITESPACE = Regex("""\s*\^\s*""")
    private val ESCAPED_QUOTE = Regex("""\\(['"])?""")
    private val ELEMENT_VALUE =
        Regex("""(X|Y|L|V)\(([\w_]+)\)""")
    private val DISTANCE =
        Regex("""Dist\(([\w_]+),([\w_]+)\)""")
    private val DEGREE =
        Regex("""Deg\(([\w_]+),([ \w\[_]+),([\w_]+)\)""")
    private val RADIAN =
        Regex("""Rad\(([\w_]+),([\w_]+),([\w_]+)\)""")
    private val NUMERIC_WRAPPER = Regex("""N\((.+)\)""")
    private val UNARY_ELEMENT_FUNCTIONS = listOf("X", "Y", "L", "V")
    private val ANGLE_ELEMENT_FUNCTIONS = listOf("Deg", "Rad")
    private val FUNCTION_REPLACEMENTS = listOf(
        "Abs" to "abs",
        "ACos" to "acos",
        "ASin" to "asin",
        "ATan" to "atan",
        "Ceil" to "ceil",
        "Cos" to "cos",
        "Exp" to "exp",
        "Factorial" to "factorial",
        "Floor" to "floor",
        "Log" to "log",
        "Max" to "max",
        "Min" to "min",
        "Random" to "random",
        "Round" to "round",
        "Sin" to "sin",
        "Sqrt" to "sqrt",
        "Tan" to "tan",
        "Trunc" to "ceil",
    )
}
