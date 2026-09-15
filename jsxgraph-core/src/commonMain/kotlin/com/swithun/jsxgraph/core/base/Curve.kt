/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/curve.js -> Curve, generateTerm, updateCurve,
 * src/math/plot.js -> updateParametricCurveNaive
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

internal sealed interface CurveError {
    data class InvalidSampleCount(
        val count: Int,
        val maximum: Int,
    ) : CurveError

    data class ExpressionCompile(
        val term: String,
        val error: JessieCodeExpressionCompileError,
    ) : CurveError

    data class ExpressionEvaluation(
        val term: String,
        val error: JessieCodeRuntimeError,
    ) : CurveError

    data class NonNumericExpression(
        val term: String,
        val actualType: String,
    ) : CurveError

    data class InvalidDomain(
        val minimum: Double,
        val maximum: Double,
    ) : CurveError

    data class Registration(
        val error: BoardError,
    ) : CurveError
}

/**
 * Initial translated slice of JXG.Curve.
 *
 * This slice covers linear data plots and the upstream naive sampler for
 * explicit-domain parametric curves and function graphs. Advanced adaptive
 * plotting, transformations, fills, labels, hit testing, and curve mutation
 * remain untranslated.
 */
internal class Curve private constructor(
    board: Board,
    internal val curveType: String,
    private val xTerm: JessieCodeExpressionFunction?,
    private val yTerm: JessieCodeExpressionFunction?,
    private val minimumTerm: JessieCodeExpressionFunction?,
    private val maximumTerm: JessieCodeExpressionFunction?,
    internal val dataX: DoubleArray?,
    internal val dataY: DoubleArray?,
    internal val sampleCount: Int,
    id: String = "",
    name: String? = null,
    needsRegularUpdate: Boolean = true,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_CURVE,
    elementClass = Const.OBJECT_CLASS_CURVE,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal val points = mutableListOf<Coords>()
    internal var numberPoints: Int = 0
        private set
    internal val bezierDegree: Int = 1
    internal var evaluationError: CurveError? = null
        private set

    init {
        elType = CURVE_ELEMENT_TYPE
        isDraggable = true
    }

    // JSXGraph: src/base/curve.js -> update
    override fun update(fromParent: Boolean): Curve {
        if (!needsUpdate) {
            return this
        }
        when (val result = updateCurve()) {
            is GMResult.Ok -> evaluationError = null
            is GMResult.Err -> {
                evaluationError = result.error
                points.clear()
                numberPoints = 0
            }
        }
        return this
    }

    // JSXGraph: src/base/curve.js -> updateCurve
    internal fun updateCurve(): GMResult<Curve, CurveError> {
        val xData = dataX
        if (xData != null) {
            points.clear()
            val yData = dataY
            for (index in xData.indices) {
                points += Coords(
                    method = Const.COORDS_BY_USER,
                    coordinates = doubleArrayOf(
                        xData[index],
                        yData?.getOrNull(index) ?: Double.NaN,
                    ),
                    board = board,
                )
            }
            numberPoints = xData.size
            return GMResult.Ok(this)
        }

        val xFunction = xTerm
            ?: return GMResult.Err(
                CurveError.NonNumericExpression(
                    term = "xterm",
                    actualType = "undefined",
                ),
            )
        val yFunction = yTerm
            ?: return GMResult.Err(
                CurveError.NonNumericExpression(
                    term = "yterm",
                    actualType = "undefined",
                ),
            )
        val minimum = when (
            val result = evaluateNumber("minX", minimumTerm)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val maximum = when (
            val result = evaluateNumber("maxX", maximumTerm)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            !minimum.isFinite() ||
            !maximum.isFinite() ||
            maximum < minimum
        ) {
            return GMResult.Err(
                CurveError.InvalidDomain(
                    minimum = minimum,
                    maximum = maximum,
                ),
            )
        }

        // JSXGraph: src/math/plot.js -> updateParametricCurveNaive
        val stepSize = (maximum - minimum) / sampleCount
        points.clear()
        for (index in 0 until sampleCount) {
            val parameter = minimum + index * stepSize
            val argument = listOf(
                JessieCodeRuntimeValue.NumberValue(parameter),
            )
            val x = when (
                val result = evaluateNumber("xterm", xFunction, argument)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val y = when (
                val result = evaluateNumber("yterm", yFunction, argument)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            points += Coords(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(x, y),
                board = board,
            )
        }
        numberPoints = sampleCount
        return GMResult.Ok(this)
    }

    internal fun minX(): Double =
        evaluateNumber("minX", minimumTerm).valueOrNaN()

    internal fun maxX(): Double =
        evaluateNumber("maxX", maximumTerm).valueOrNaN()

    internal fun X(parameter: Double): Double =
        evaluateNumber(
            term = "xterm",
            expression = xTerm,
            arguments = listOf(
                JessieCodeRuntimeValue.NumberValue(parameter),
            ),
        ).valueOrNaN()

    internal fun Y(parameter: Double): Double =
        evaluateNumber(
            term = "yterm",
            expression = yTerm,
            arguments = listOf(
                JessieCodeRuntimeValue.NumberValue(parameter),
            ),
        ).valueOrNaN()

    private fun evaluateNumber(
        term: String,
        expression: JessieCodeExpressionFunction?,
        arguments: List<JessieCodeRuntimeValue> = emptyList(),
    ): GMResult<Double, CurveError> {
        val function = expression
            ?: return GMResult.Err(
                CurveError.NonNumericExpression(
                    term = term,
                    actualType = "undefined",
                ),
            )
        return when (val result = function.evaluate(arguments)) {
            is GMResult.Err -> GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = term,
                    error = result.error,
                ),
            )
            is GMResult.Ok -> {
                val value = result.value
                if (value is JessieCodeRuntimeValue.NumberValue) {
                    GMResult.Ok(value.value)
                } else {
                    GMResult.Err(
                        CurveError.NonNumericExpression(
                            term = term,
                            actualType = runtimeType(value),
                        ),
                    )
                }
            }
        }
    }

    private fun GMResult<Double, CurveError>.valueOrNaN(): Double =
        when (this) {
            is GMResult.Ok -> value
            is GMResult.Err -> Double.NaN
        }

    internal companion object {
        internal const val DEFAULT_SAMPLE_COUNT: Int = 1600
        internal const val MAX_SAMPLE_COUNT: Int = 10_000

        private const val CURVE_ID_PREFIX = "G"
        private const val CURVE_ELEMENT_TYPE = "curve"
        private const val DATA_CURVE_TYPE = "plot"
        private const val PARAMETRIC_CURVE_TYPE = "parameter"
        private const val FUNCTION_GRAPH_CURVE_TYPE = "functiongraph"

        // JSXGraph: src/base/curve.js -> createCurve / generateTerm data branch
        internal fun createData(
            board: Board,
            dataX: DoubleArray,
            dataY: DoubleArray,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> =
            register(
                Curve(
                    board = board,
                    curveType = DATA_CURVE_TYPE,
                    xTerm = null,
                    yTerm = null,
                    minimumTerm = null,
                    maximumTerm = null,
                    dataX = dataX.copyOf(),
                    dataY = dataY.copyOf(),
                    sampleCount = dataX.size,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                expressions = emptyList(),
            )

        // JSXGraph: src/base/curve.js -> createCurve / generateTerm
        internal fun createParametric(
            board: Board,
            xSource: String,
            ySource: String,
            minimumSource: String,
            maximumSource: String,
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> =
            createContinuous(
                board = board,
                curveType = PARAMETRIC_CURVE_TYPE,
                xSource = xSource,
                ySource = ySource,
                minimumSource = minimumSource,
                maximumSource = maximumSource,
                sampleCount = sampleCount,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        // JSXGraph: src/base/curve.js -> createFunctiongraph
        internal fun createFunctionGraph(
            board: Board,
            ySource: String,
            minimumSource: String,
            maximumSource: String,
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> =
            createContinuous(
                board = board,
                curveType = FUNCTION_GRAPH_CURVE_TYPE,
                xSource = "x",
                ySource = ySource,
                minimumSource = minimumSource,
                maximumSource = maximumSource,
                sampleCount = sampleCount,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        private fun createContinuous(
            board: Board,
            curveType: String,
            xSource: String,
            ySource: String,
            minimumSource: String,
            maximumSource: String,
            sampleCount: Int,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
        ): GMResult<Curve, CurveError> {
            if (sampleCount !in 1..MAX_SAMPLE_COUNT) {
                return GMResult.Err(
                    CurveError.InvalidSampleCount(
                        count = sampleCount,
                        maximum = MAX_SAMPLE_COUNT,
                    ),
                )
            }
            val xTerm = when (
                val result = compile(
                    term = "xterm",
                    source = xSource,
                    board = board,
                    variableNames = listOf("x"),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val yTerm = when (
                val result = compile(
                    term = "yterm",
                    source = ySource,
                    board = board,
                    variableNames = listOf("x"),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val minimumTerm = when (
                val result = compile("minX", minimumSource, board)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val maximumTerm = when (
                val result = compile("maxX", maximumSource, board)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return register(
                Curve(
                    board = board,
                    curveType = curveType,
                    xTerm = xTerm,
                    yTerm = yTerm,
                    minimumTerm = minimumTerm,
                    maximumTerm = maximumTerm,
                    dataX = null,
                    dataY = null,
                    sampleCount = sampleCount,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                expressions = listOf(
                    xTerm,
                    yTerm,
                    minimumTerm,
                    maximumTerm,
                ),
            )
        }

        private fun compile(
            term: String,
            source: String,
            board: Board,
            variableNames: List<String> = emptyList(),
        ): GMResult<JessieCodeExpressionFunction, CurveError> =
            when (
                val result = JessieCodeExpressionFunction.compile(
                    source = source,
                    board = board,
                    variableNames = variableNames,
                )
            ) {
                is GMResult.Ok -> result
                is GMResult.Err -> GMResult.Err(
                    CurveError.ExpressionCompile(
                        term = term,
                        error = result.error,
                    ),
                )
            }

        private fun register(
            curve: Curve,
            expressions: List<JessieCodeExpressionFunction>,
        ): GMResult<Curve, CurveError> {
            when (val result = curve.updateCurve()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            return when (
                val registration = curve.board.setId(
                    curve,
                    CURVE_ID_PREFIX,
                )
            ) {
                is GMResult.Ok -> {
                    curve.addParentsFromJCFunctions(expressions)
                    curve.needsUpdate = false
                    GMResult.Ok(curve)
                }
                is GMResult.Err -> GMResult.Err(
                    CurveError.Registration(registration.error),
                )
            }
        }

        private fun runtimeType(
            value: JessieCodeRuntimeValue,
        ): String =
            when (value) {
                JessieCodeRuntimeValue.UndefinedValue -> "undefined"
                JessieCodeRuntimeValue.NullValue -> "null"
                is JessieCodeRuntimeValue.NumberValue -> "number"
                is JessieCodeRuntimeValue.BooleanValue -> "boolean"
                is JessieCodeRuntimeValue.StringValue -> "string"
                is JessieCodeRuntimeValue.ArrayValue -> "array"
                is JessieCodeRuntimeValue.ObjectValue -> "object"
                is JessieCodeRuntimeValue.FunctionValue -> "function"
                is JessieCodeRuntimeValue.BoardReference -> "board"
                is JessieCodeRuntimeValue.ElementReference -> "element"
            }
    }
}
