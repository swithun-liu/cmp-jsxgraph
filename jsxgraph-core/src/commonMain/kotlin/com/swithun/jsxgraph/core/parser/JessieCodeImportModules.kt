/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> priv.modules and importModule
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.Numerics
import com.swithun.jsxgraph.core.math.Statistics

/**
 * Session-owned JessieCode module registry.
 *
 * The upstream registry contains singleton JavaScript objects. Keeping these
 * runtime objects on EvaluationState preserves the same identity for repeated
 * and case-insensitive imports without introducing process-global mutation.
 */
internal class JessieCodeImportModules {
    private val modules: Map<String, JessieCodeRuntimeValue.ObjectValue> =
        mapOf(
            JessieCodeModule.MATH.sourceName to mathModule(),
            JessieCodeModule.GEOMETRY.sourceName to geometryModule(),
            JessieCodeModule.STATISTICS.sourceName to statisticsModule(),
            JessieCodeModule.NUMERICS.sourceName to numericsModule(),
        )

    // JSXGraph 1.13.3: src/parser/jessiecode.js -> importModule.
    internal fun importModule(
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        if (arguments.isEmpty()) {
            return invalidArgumentCount(
                functionName = "import",
                expected = "1",
                actual = 0,
                location = location,
            )
        }
        val moduleName = arguments[0] as?
            JessieCodeRuntimeValue.StringValue
            ?: return invalidArgumentType(
                functionName = "import",
                argumentIndex = 0,
                expected = "string",
                actual = arguments[0],
                location = location,
            )
        return GMResult.Ok(
            modules[moduleName.value.lowercase()]
                ?: JessieCodeRuntimeValue.UndefinedValue,
        )
    }

    // JSXGraph 1.13.3: src/math/math.js -> eps, clamp.
    private fun mathModule(): JessieCodeRuntimeValue.ObjectValue =
        JessieCodeRuntimeValue.ObjectValue(
            mapOf(
                "eps" to number(Mat.eps),
                "clamp" to function("clamp") { arguments, location ->
                    val values = when (
                        val result = numberArguments(
                            functionName = "clamp",
                            arguments = arguments,
                            count = 3,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@function result
                    }
                    GMResult.Ok(
                        number(Mat.clamp(values[0], values[1], values[2])),
                    )
                },
            ),
        )

    // JSXGraph 1.13.3: src/math/geometry.js -> distance.
    private fun geometryModule(): JessieCodeRuntimeValue.ObjectValue =
        JessieCodeRuntimeValue.ObjectValue(
            mapOf(
                "distance" to function("distance") { arguments, location ->
                    if (arguments.size !in 2..3) {
                        return@function invalidArgumentCount(
                            functionName = "distance",
                            expected = "2 or 3",
                            actual = arguments.size,
                            location = location,
                        )
                    }
                    val first = when (
                        val result = numberArrayArgument(
                            functionName = "distance",
                            arguments = arguments,
                            argumentIndex = 0,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@function result
                    }
                    val second = when (
                        val result = numberArrayArgument(
                            functionName = "distance",
                            arguments = arguments,
                            argumentIndex = 1,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@function result
                    }
                    val requestedLength = if (arguments.size == 3) {
                        val value = when (
                            val result = numberArgument(
                                functionName = "distance",
                                arguments = arguments,
                                argumentIndex = 2,
                                location = location,
                            )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return@function result
                        }
                        if (
                            !value.isFinite() ||
                            value < 0.0 ||
                            value > Int.MAX_VALUE.toDouble()
                        ) {
                            return@function invalidArgumentType(
                                functionName = "distance",
                                argumentIndex = 2,
                                expected = "finite non-negative number",
                                actual = arguments[2],
                                location = location,
                            )
                        }
                        value.toInt()
                    } else {
                        null
                    }
                    if (requestedLength != null) {
                        val availableLength = minOf(first.size, second.size)
                        if (requestedLength > availableLength) {
                            return@function GMResult.Ok(number(Double.NaN))
                        }
                    }
                    GMResult.Ok(
                        number(
                            Geometry.distance(
                                first = first,
                                second = second,
                                requestedLength = requestedLength,
                            ),
                        ),
                    )
                },
            ),
        )

    // JSXGraph 1.13.3: src/math/statistics.js -> mean.
    private fun statisticsModule(): JessieCodeRuntimeValue.ObjectValue =
        JessieCodeRuntimeValue.ObjectValue(
            mapOf(
                "mean" to function("mean") { arguments, location ->
                    if (arguments.size != 1) {
                        return@function invalidArgumentCount(
                            functionName = "mean",
                            expected = "1",
                            actual = arguments.size,
                            location = location,
                        )
                    }
                    val values = when (
                        val result = numberArrayArgument(
                            functionName = "mean",
                            arguments = arguments,
                            argumentIndex = 0,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@function result
                    }
                    GMResult.Ok(number(Statistics.mean(values)))
                },
            ),
        )

    // JSXGraph 1.13.3: src/math/numerics.js -> det.
    private fun numericsModule(): JessieCodeRuntimeValue.ObjectValue =
        JessieCodeRuntimeValue.ObjectValue(
            mapOf(
                "det" to function("det") { arguments, location ->
                    if (arguments.size != 1) {
                        return@function invalidArgumentCount(
                            functionName = "det",
                            expected = "1",
                            actual = arguments.size,
                            location = location,
                        )
                    }
                    val matrix = when (
                        val result = squareMatrixArgument(
                            functionName = "det",
                            arguments = arguments,
                            argumentIndex = 0,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@function result
                    }
                    GMResult.Ok(number(Numerics.det(matrix)))
                },
            ),
        )

    private fun function(
        name: String,
        callable: JessieCodeCallable,
    ): JessieCodeRuntimeValue.FunctionValue =
        JessieCodeRuntimeValue.FunctionValue(
            name = name,
            callable = callable,
        )

    private fun number(
        value: Double,
    ): JessieCodeRuntimeValue.NumberValue =
        JessieCodeRuntimeValue.NumberValue(value)

    private fun numberArguments(
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        count: Int,
        location: JessieCodeAstLocation,
    ): GMResult<DoubleArray, JessieCodeRuntimeError> {
        if (arguments.size != count) {
            return invalidArgumentCount(
                functionName = functionName,
                expected = count.toString(),
                actual = arguments.size,
                location = location,
            )
        }
        val values = DoubleArray(count)
        for (index in 0 until count) {
            when (
                val result = numberArgument(
                    functionName = functionName,
                    arguments = arguments,
                    argumentIndex = index,
                    location = location,
                )
            ) {
                is GMResult.Ok -> values[index] = result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(values)
    }

    private fun numberArgument(
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        argumentIndex: Int,
        location: JessieCodeAstLocation,
    ): GMResult<Double, JessieCodeRuntimeError> {
        val value = arguments.getOrNull(argumentIndex)
            ?: return invalidArgumentCount(
                functionName = functionName,
                expected = "at least ${argumentIndex + 1}",
                actual = arguments.size,
                location = location,
            )
        return if (value is JessieCodeRuntimeValue.NumberValue) {
            GMResult.Ok(value.value)
        } else {
            invalidArgumentType(
                functionName = functionName,
                argumentIndex = argumentIndex,
                expected = "number",
                actual = value,
                location = location,
            )
        }
    }

    private fun numberArrayArgument(
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        argumentIndex: Int,
        location: JessieCodeAstLocation,
    ): GMResult<DoubleArray, JessieCodeRuntimeError> {
        val value = arguments.getOrNull(argumentIndex)
            ?: return invalidArgumentCount(
                functionName = functionName,
                expected = "at least ${argumentIndex + 1}",
                actual = arguments.size,
                location = location,
            )
        val array = value as? JessieCodeRuntimeValue.ArrayValue
            ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = argumentIndex,
                expected = "number array",
                actual = value,
                location = location,
            )
        val result = DoubleArray(array.values.size)
        for ((index, item) in array.values.withIndex()) {
            val numeric = item as? JessieCodeRuntimeValue.NumberValue
                ?: return invalidArgumentType(
                    functionName = functionName,
                    argumentIndex = argumentIndex,
                    expected = "number array",
                    actual = item,
                    location = location,
                )
            result[index] = numeric.value
        }
        return GMResult.Ok(result)
    }

    private fun squareMatrixArgument(
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        argumentIndex: Int,
        location: JessieCodeAstLocation,
    ): GMResult<Array<DoubleArray>, JessieCodeRuntimeError> {
        val value = arguments.getOrNull(argumentIndex)
            ?: return invalidArgumentCount(
                functionName = functionName,
                expected = "at least ${argumentIndex + 1}",
                actual = arguments.size,
                location = location,
            )
        val rows = value as? JessieCodeRuntimeValue.ArrayValue
            ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = argumentIndex,
                expected = "non-empty square number matrix",
                actual = value,
                location = location,
            )
        if (rows.values.isEmpty()) {
            return invalidArgumentType(
                functionName = functionName,
                argumentIndex = argumentIndex,
                expected = "non-empty square number matrix",
                actual = value,
                location = location,
            )
        }
        val matrix = Array(rows.values.size) { DoubleArray(rows.values.size) }
        for ((rowIndex, rowValue) in rows.values.withIndex()) {
            val row = rowValue as? JessieCodeRuntimeValue.ArrayValue
                ?: return invalidArgumentType(
                    functionName = functionName,
                    argumentIndex = argumentIndex,
                    expected = "non-empty square number matrix",
                    actual = rowValue,
                    location = location,
                )
            if (row.values.size != rows.values.size) {
                return invalidArgumentType(
                    functionName = functionName,
                    argumentIndex = argumentIndex,
                    expected = "non-empty square number matrix",
                    actual = row,
                    location = location,
                )
            }
            for ((columnIndex, item) in row.values.withIndex()) {
                val numeric = item as? JessieCodeRuntimeValue.NumberValue
                    ?: return invalidArgumentType(
                        functionName = functionName,
                        argumentIndex = argumentIndex,
                        expected = "non-empty square number matrix",
                        actual = item,
                        location = location,
                    )
                matrix[rowIndex][columnIndex] = numeric.value
            }
        }
        return GMResult.Ok(matrix)
    }

    private fun invalidArgumentCount(
        functionName: String,
        expected: String,
        actual: Int,
        location: JessieCodeAstLocation,
    ): GMResult.Err<JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.InvalidArgumentCount(
                functionName = functionName,
                expected = expected,
                actual = actual,
                location = location,
            ),
        )

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

    private fun typeName(value: JessieCodeRuntimeValue): String =
        when (value) {
            JessieCodeRuntimeValue.NullValue -> "null"
            JessieCodeRuntimeValue.UndefinedValue -> "undefined"
            is JessieCodeRuntimeValue.NumberValue -> "number"
            is JessieCodeRuntimeValue.BooleanValue -> "boolean"
            is JessieCodeRuntimeValue.StringValue -> "string"
            is JessieCodeRuntimeValue.ArrayValue -> "array"
            is JessieCodeRuntimeValue.ObjectValue -> "object"
            is JessieCodeRuntimeValue.FunctionValue -> "function"
            is JessieCodeRuntimeValue.BoardReference -> "board"
            is JessieCodeRuntimeValue.TransformationReference ->
                "transformation"
            is JessieCodeRuntimeValue.CompositionReference -> "composition"
            is JessieCodeRuntimeValue.ElementReference -> "element"
        }
}
