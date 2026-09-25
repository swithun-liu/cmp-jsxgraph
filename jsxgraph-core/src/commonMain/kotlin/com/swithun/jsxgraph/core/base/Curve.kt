/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/curve.js -> Curve, generateTerm, updateCurve,
 * createStepfunction, createDerivative, interpolationFunctionFromArray,
 * createSpline, createCardinalSpline, createRiemannsum, createBoxPlot,
 * src/math/plot.js -> updateParametricCurveNaive,
 * src/element/comb.js -> createComb,
 * src/element/composition.js -> createInequality,
 * src/element/vectorfield.js -> createVectorField / createSlopeField,
 * src/element/conic.js ->
 * createEllipse / createHyperbola / createParabola
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Clip
import com.swithun.jsxgraph.core.math.ClipBooleanOperation
import com.swithun.jsxgraph.core.math.ClipError
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.Numerics
import com.swithun.jsxgraph.core.math.NumericsError
import com.swithun.jsxgraph.core.math.NumericsPoint2D
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

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

    data class BooleanClipping(
        val error: ClipError,
    ) : CurveError

    data class Numerics(
        val operation: String,
        val error: NumericsError,
    ) : CurveError

    data class InvalidInterpolationPointCount(
        val creator: String,
        val count: Int,
        val minimum: Int,
    ) : CurveError

    data class InvalidCombFrequency(
        val frequency: Double,
    ) : CurveError

    data class InvalidInequalitySource(
        val elementType: String,
        val curveType: String?,
    ) : CurveError

    data class DataUpdate(
        val error: CurveDataUpdateError,
    ) : CurveError
}

internal sealed interface CurveDataUpdateError {
    data class Face3D(
        val error: Face3DError,
    ) : CurveDataUpdateError

    data class Ticks3D(
        val error: Ticks3DError,
    ) : CurveDataUpdateError

    data class Mesh3D(
        val error: Mesh3DError,
    ) : CurveDataUpdateError
}

internal data class CurveDataUpdate(
    val x: DoubleArray,
    val y: DoubleArray,
)

internal fun interface CurveDataUpdater {
    fun update(): GMResult<CurveDataUpdate, CurveDataUpdateError>
}

internal data class CurveMesh3DDefinition(
    var requestedPointCount: Long = 0L,
)

private data class CurveBooleanDefinition(
    val subject: GeometryElement,
    val clip: GeometryElement,
    val operation: ClipBooleanOperation,
)

internal interface CurveStepTerm {
    val length: Int
    val isFunction: Boolean

    fun valueAt(index: Int): Double
}

internal sealed interface CurveVectorFieldFunction {
    fun evaluate(
        x: Double,
        y: Double,
    ): GMResult<Pair<Double, Double>, CurveError>
}

internal class CurveVectorFieldComponentFunction(
    private val xTerm: JessieCodeCoordinateFunction,
    private val yTerm: JessieCodeCoordinateFunction,
) : CurveVectorFieldFunction {
    override fun evaluate(
        x: Double,
        y: Double,
    ): GMResult<Pair<Double, Double>, CurveError> {
        val arguments = listOf(
            JessieCodeRuntimeValue.NumberValue(x),
            JessieCodeRuntimeValue.NumberValue(y),
        )
        val xValue = when (
            val result = evaluateVectorFieldNumber(
                termName = "vectorfield.F[0]",
                term = xTerm,
                arguments = arguments,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val yValue = when (
            val result = evaluateVectorFieldNumber(
                termName = "vectorfield.F[1]",
                term = yTerm,
                arguments = arguments,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(xValue to yValue)
    }
}

internal class CurveVectorFieldArrayFunction(
    private val term: JessieCodeCoordinateFunction,
) : CurveVectorFieldFunction {
    override fun evaluate(
        x: Double,
        y: Double,
    ): GMResult<Pair<Double, Double>, CurveError> {
        val result = when (
            val evaluated = term.evaluate(
                listOf(
                    JessieCodeRuntimeValue.NumberValue(x),
                    JessieCodeRuntimeValue.NumberValue(y),
                ),
            )
        ) {
            is GMResult.Ok -> evaluated.value
            is GMResult.Err -> return GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = "vectorfield.F",
                    error = evaluated.error,
                ),
            )
        }
        val values = (
            result as? JessieCodeRuntimeValue.ArrayValue
            )?.values
        val xValue = values?.getOrNull(0) as?
            JessieCodeRuntimeValue.NumberValue
        val yValue = values?.getOrNull(1) as?
            JessieCodeRuntimeValue.NumberValue
        return if (xValue == null || yValue == null) {
            GMResult.Err(
                CurveError.NonNumericExpression(
                    term = "vectorfield.F",
                    actualType = curveRuntimeType(result),
                ),
            )
        } else {
            GMResult.Ok(xValue.value to yValue.value)
        }
    }
}

internal class CurveSlopeFieldFunction(
    private val term: JessieCodeCoordinateFunction,
) : CurveVectorFieldFunction {
    // JSXGraph 1.13.3:
    // src/element/vectorfield.js -> createSlopeField.
    override fun evaluate(
        x: Double,
        y: Double,
    ): GMResult<Pair<Double, Double>, CurveError> {
        val slope = when (
            val result = evaluateVectorFieldNumber(
                termName = "slopefield.F",
                term = term,
                arguments = listOf(
                    JessieCodeRuntimeValue.NumberValue(x),
                    JessieCodeRuntimeValue.NumberValue(y),
                ),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val norm = sqrt(1.0 + slope * slope)
        return GMResult.Ok(1.0 / norm to slope / norm)
    }
}

private fun evaluateVectorFieldNumber(
    termName: String,
    term: JessieCodeCoordinateFunction,
    arguments: List<JessieCodeRuntimeValue>,
): GMResult<Double, CurveError> =
    when (val result = term.evaluate(arguments)) {
        is GMResult.Err -> GMResult.Err(
            CurveError.ExpressionEvaluation(
                term = termName,
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
                        term = termName,
                        actualType = curveRuntimeType(value),
                    ),
                )
            }
        }
    }

private data class CurveStepDefinition(
    val xTerm: CurveStepTerm,
    val yTerm: CurveStepTerm,
)

private data class CurveDerivativeDefinition(
    val source: Curve,
    val xDerivative: (Double) -> Double,
    val yDerivative: (Double) -> Double,
    val minimum: Double,
    val maximum: Double,
)

internal sealed interface CurveSplinePoint {
    val parentElement: CoordsElement?

    fun coordinates(): GMResult<Pair<Double, Double>, CurveError>
}

internal class CurveElementSplinePoint(
    private val point: CoordsElement,
) : CurveSplinePoint {
    override val parentElement: CoordsElement = point

    override fun coordinates(): GMResult<Pair<Double, Double>, CurveError> =
        GMResult.Ok(point.X() to point.Y())
}

internal class CurveCoordinateSplinePoint(
    private val xTerm: JessieCodeCoordinateFunction,
    private val yTerm: JessieCodeCoordinateFunction,
) : CurveSplinePoint {
    override val parentElement: CoordsElement? = null

    override fun coordinates(): GMResult<Pair<Double, Double>, CurveError> {
        val x = when (val result = evaluate("x", xTerm)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val y = when (val result = evaluate("y", yTerm)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(x to y)
    }

    private fun evaluate(
        coordinate: String,
        term: JessieCodeCoordinateFunction,
    ): GMResult<Double, CurveError> =
        when (val result = term.evaluate()) {
            is GMResult.Err -> GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = "spline.$coordinate",
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
                            term = "spline.$coordinate",
                            actualType = curveRuntimeType(value),
                        ),
                    )
                }
            }
        }
}

internal class CurveFunctionSplinePoint(
    private val function: JessieCodeRuntimeValue.FunctionValue,
    private val location: JessieCodeAstLocation,
) : CurveSplinePoint {
    override val parentElement: CoordsElement? = null

    override fun coordinates(): GMResult<Pair<Double, Double>, CurveError> =
        when (
            val result = function.externalCallable.call(
                arguments = emptyList(),
                location = location,
            )
        ) {
            is GMResult.Err -> GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = "spline.point",
                    error = result.error,
                ),
            )
            is GMResult.Ok -> {
                val values = (
                    result.value as? JessieCodeRuntimeValue.ArrayValue
                    )?.values
                val x = values?.getOrNull(0) as?
                    JessieCodeRuntimeValue.NumberValue
                val y = values?.getOrNull(1) as?
                    JessieCodeRuntimeValue.NumberValue
                if (x == null || y == null) {
                    GMResult.Err(
                        CurveError.NonNumericExpression(
                            term = "spline.point",
                            actualType = curveRuntimeType(result.value),
                        ),
                    )
                } else {
                    GMResult.Ok(x.value to y.value)
                }
            }
        }
}

internal class CurveStaticPoint(
    private val x: Double,
    private val y: Double,
) : NumericsPoint2D {
    override fun X(): Double = x

    override fun Y(): Double = y
}

private fun curveRuntimeType(
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
        is JessieCodeRuntimeValue.TransformationReference ->
            "transformation"
        is JessieCodeRuntimeValue.CompositionReference -> "composition"
        is JessieCodeRuntimeValue.ElementReference -> "element"
    }

private data class CurveSplineDefinition(
    val points: List<CurveSplinePoint>,
    var knots: DoubleArray = doubleArrayOf(),
    var values: DoubleArray = doubleArrayOf(),
    var secondDerivatives: DoubleArray = doubleArrayOf(),
)

private data class CurveCardinalSplineDefinition(
    val points: List<NumericsPoint2D>,
    val tensionTerm: JessieCodeCoordinateFunction,
    val type: String,
    var interpolation:
        com.swithun.jsxgraph.core.math.CardinalSplineInterpolation? = null,
)

private data class CurveRiemannDefinition(
    val upperFunction: JessieCodeCoordinateFunction,
    val lowerFunction: JessieCodeCoordinateFunction?,
    val rectangleCount: JessieCodeCoordinateFunction,
    val approximationType: JessieCodeCoordinateFunction,
    val minimum: JessieCodeCoordinateFunction,
    val maximum: JessieCodeCoordinateFunction,
    var sum: Double = 0.0,
)

internal data class CurveBoxPlotSnapshot(
    val quantiles: DoubleArray,
    val outliers: DoubleArray?,
    val axis: Double,
    val width: Double,
    val direction: String,
    val smallWidth: Double,
    val outlierFace: String,
    val outlierSize: Double,
)

private data class CurveBoxPlotDefinition(
    val quantileTerms: List<JessieCodeCoordinateFunction>,
    val axisTerm: JessieCodeCoordinateFunction,
    val widthTerm: JessieCodeCoordinateFunction,
    val direction: String,
    val smallWidth: Double,
    val outlierFace: String,
    val outlierSize: Double,
    var snapshot: CurveBoxPlotSnapshot? = null,
)

private data class CurveCombDefinition(
    val point1: Point,
    val point2: Point,
    val frequencyTerm: JessieCodeCoordinateFunction,
    val widthTerm: JessieCodeCoordinateFunction,
    val angleTerm: JessieCodeCoordinateFunction,
    val reverseTerm: JessieCodeCoordinateFunction,
    var requestedPointCount: Long = 0L,
)

private data class CurveInequalityDefinition(
    val source: GeometryElement,
    val inverseTerm: JessieCodeCoordinateFunction,
    var requestedPointCount: Long = 0L,
)

internal data class CurveVectorFieldVectorSnapshot(
    val startX: Double,
    val startY: Double,
    val endX: Double,
    val endY: Double,
)

internal data class CurveVectorFieldSnapshot(
    val vectors: List<CurveVectorFieldVectorSnapshot>,
    val arrowEnabled: Boolean,
    val arrowSize: Double,
    val arrowAngle: Double,
)

private data class CurveVectorFieldDefinition(
    val elementType: String,
    val field: CurveVectorFieldFunction,
    val xData: List<JessieCodeCoordinateFunction>,
    val yData: List<JessieCodeCoordinateFunction>,
    val scaleTerm: JessieCodeCoordinateFunction,
    val arrowEnabledTerm: JessieCodeCoordinateFunction,
    val arrowSizeTerm: JessieCodeCoordinateFunction,
    val arrowAngleTerm: JessieCodeCoordinateFunction,
    var requestedPointCount: Long = 0L,
    var snapshot: CurveVectorFieldSnapshot? = null,
)

/**
 * Initial translated slice of JXG.Curve.
 *
 * This slice covers linear data plots and the upstream naive sampler for
 * explicit-domain parametric curves and function graphs, retained StepFunction
 * and RiemannSum data, BoxPlot geometry, splines, and Boolean clipping.
 * Advanced adaptive plotting, transformations, ordinary Curve fills, labels,
 * hit testing, and general curve mutation remain untranslated.
 */
internal class Curve private constructor(
    board: Board,
    internal val curveType: String,
    private val xTerm: JessieCodeExpressionFunction?,
    private val yTerm: JessieCodeExpressionFunction?,
    private val minimumTerm: JessieCodeExpressionFunction?,
    private val maximumTerm: JessieCodeExpressionFunction?,
    dataX: DoubleArray?,
    dataY: DoubleArray?,
    internal val sampleCount: Int,
    private val booleanDefinition: CurveBooleanDefinition? = null,
    private val stepDefinition: CurveStepDefinition? = null,
    private val derivativeDefinition: CurveDerivativeDefinition? = null,
    private val splineDefinition: CurveSplineDefinition? = null,
    private val cardinalSplineDefinition: CurveCardinalSplineDefinition? = null,
    private val riemannDefinition: CurveRiemannDefinition? = null,
    private val boxPlotDefinition: CurveBoxPlotDefinition? = null,
    private val combDefinition: CurveCombDefinition? = null,
    private val inequalityDefinition: CurveInequalityDefinition? = null,
    private val vectorFieldDefinition: CurveVectorFieldDefinition? = null,
    private val ellipseDefinition: CurveEllipseDefinition? = null,
    private val hyperbolaDefinition: CurveHyperbolaDefinition? = null,
    private val parabolaDefinition: CurveParabolaDefinition? = null,
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
    internal var dataX: DoubleArray? = dataX
        private set
    internal var dataY: DoubleArray? = dataY
        private set
    internal val points = mutableListOf<Coords>()
    internal var numberPoints: Int = 0
        private set
    internal val bezierDegree: Int = 1
    internal var evaluationError: CurveError? = null
        private set
    internal val isBooleanComposition: Boolean
        get() = booleanDefinition != null
    internal val isStepFunction: Boolean
        get() = stepDefinition != null
    internal val isDerivative: Boolean
        get() = derivativeDefinition != null
    internal val isSpline: Boolean
        get() = splineDefinition != null
    internal val isCardinalSpline: Boolean
        get() = cardinalSplineDefinition != null
    internal val isRiemannSum: Boolean
        get() = riemannDefinition != null
    internal val isBoxPlot: Boolean
        get() = boxPlotDefinition != null
    internal val isComb: Boolean
        get() = combDefinition != null
    internal val isInequality: Boolean
        get() = inequalityDefinition != null
    internal val isVectorField: Boolean
        get() = vectorFieldDefinition != null
    internal val isEllipse: Boolean
        get() = ellipseDefinition != null
    internal val isHyperbola: Boolean
        get() = hyperbolaDefinition != null
    internal val isParabola: Boolean
        get() = parabolaDefinition != null
    internal val isTicks3D: Boolean
        get() = ticks3DDefinition != null
    internal val isMesh3D: Boolean
        get() = mesh3DDefinition != null
    internal val center: Point?
        get() =
            ellipseDefinition?.center
                ?: hyperbolaDefinition?.center
                ?: parabolaDefinition?.center
    internal val midpoint: Point?
        get() = center
    internal val foci: List<Point>
        get() {
            ellipseDefinition?.let {
                return listOf(it.focus1, it.focus2)
            }
            hyperbolaDefinition?.let {
                return listOf(it.focus1, it.focus2)
            }
            return emptyList()
        }
    internal val pointOnEllipse: Point?
        get() = ellipseDefinition?.pointOnEllipse
    internal val pointOnHyperbola: Point?
        get() = hyperbolaDefinition?.pointOnHyperbola
    internal val parabolaFocus: Point?
        get() = parabolaDefinition?.focus
    internal val parabolaDirectrix: Line?
        get() = parabolaDefinition?.directrix
    internal val inherits = mutableListOf<GeometryElement>()
    internal val subs = linkedMapOf<String, GeometryElement>()
    private var dataUpdater: CurveDataUpdater? = null
    internal var ticks3DDefinition: Ticks3DDefinition? = null
        private set
    internal var mesh3DDefinition: CurveMesh3DDefinition? = null
        private set

    internal fun requestedPointCount(): Long? =
        mesh3DDefinition?.requestedPointCount
            ?: vectorFieldDefinition?.requestedPointCount
            ?: inequalityDefinition?.requestedPointCount
            ?: combDefinition?.requestedPointCount
            ?: stepDefinition?.xTerm?.length?.let { sourceCount ->
            if (sourceCount == 0) {
                0L
            } else {
                sourceCount.toLong() * 2L - 1L
            }
        }

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
        dataUpdater?.let { updater ->
            when (val result = updater.update()) {
                is GMResult.Ok -> {
                    dataX = result.value.x.copyOf()
                    dataY = result.value.y.copyOf()
                }
                is GMResult.Err -> return GMResult.Err(
                    CurveError.DataUpdate(result.error),
                )
            }
        }
        val clipping = booleanDefinition
        if (clipping != null) {
            return when (
                val result = Clip.booleanOperation(
                    subject = clipping.subject,
                    clip = clipping.clip,
                    operation = clipping.operation,
                )
            ) {
                is GMResult.Ok -> {
                    replaceDataPoints(result.value.x, result.value.y)
                    GMResult.Ok(this)
                }
                is GMResult.Err -> GMResult.Err(
                    CurveError.BooleanClipping(result.error),
                )
            }
        }

        val step = stepDefinition
        if (step != null) {
            when (val result = updateStepDataArray(step)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        val comb = combDefinition
        if (comb != null) {
            when (val result = updateCombDefinition(comb)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        val inequality = inequalityDefinition
        if (inequality != null) {
            when (val result = updateInequalityDefinition(inequality)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        val vectorField = vectorFieldDefinition
        if (vectorField != null) {
            when (val result = updateVectorFieldDefinition(vectorField)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        val boxPlot = boxPlotDefinition
        if (boxPlot != null) {
            when (val result = updateBoxPlotDefinition(boxPlot)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        val riemann = riemannDefinition
        if (riemann != null) {
            when (val result = updateRiemannDefinition(riemann)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        val spline = splineDefinition
        if (spline != null) {
            when (val result = updateSplineDefinition(spline)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        val cardinalSpline = cardinalSplineDefinition
        if (cardinalSpline != null) {
            when (val result = updateCardinalSplineDefinition(cardinalSpline)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        val xData = dataX
        if (xData != null) {
            replaceDataPoints(xData, dataY ?: DoubleArray(0))
            return GMResult.Ok(this)
        }

        val minimum = when (val result = evaluateMinimum()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val maximum = when (val result = evaluateMaximum()) {
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
            val suspendedUpdate = index > 0
            val x = when (
                val result = evaluateX(
                    parameter,
                    argument,
                    suspendedUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val y = when (
                val result = evaluateY(
                    parameter,
                    argument,
                    suspendedUpdate,
                )
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

    // JSXGraph 1.13.3: src/base/curve.js -> createSpline.funcs.
    private fun updateSplineDefinition(
        definition: CurveSplineDefinition,
    ): GMResult<Unit, CurveError> {
        val knots = DoubleArray(definition.points.size)
        val values = DoubleArray(definition.points.size)
        for ((index, point) in definition.points.withIndex()) {
            when (val result = point.coordinates()) {
                is GMResult.Ok -> {
                    knots[index] = result.value.first
                    values[index] = result.value.second
                }
                is GMResult.Err -> return result
            }
        }
        val secondDerivatives = when (
            val result = Numerics.splineDef(knots, values)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                CurveError.Numerics("splineDef", result.error),
            )
        }
        definition.knots = knots
        definition.values = values
        definition.secondDerivatives = secondDerivatives
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3:
    // src/base/curve.js -> createCardinalSpline and
    // src/math/numerics.js -> CardinalSpline.
    private fun updateCardinalSplineDefinition(
        definition: CurveCardinalSplineDefinition,
    ): GMResult<Unit, CurveError> {
        val tension = when (val result = definition.tensionTerm.evaluate()) {
            is GMResult.Err -> return GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = "cardinalspline.tau",
                    error = result.error,
                ),
            )
            is GMResult.Ok -> {
                val value = result.value
                if (value !is JessieCodeRuntimeValue.NumberValue) {
                    return GMResult.Err(
                        CurveError.NonNumericExpression(
                            term = "cardinalspline.tau",
                            actualType = runtimeType(value),
                        ),
                    )
                }
                value.value
            }
        }
        definition.interpolation = Numerics.CardinalSpline(
            points = definition.points,
            tension = tension,
            type = definition.type,
        )
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3:
    // src/base/curve.js -> createStepfunction.updateDataArray.
    private fun updateStepDataArray(
        definition: CurveStepDefinition,
    ): GMResult<Unit, CurveError> {
        val sourceCount = definition.xTerm.length
        val pointCount = requestedPointCount() ?: 0L
        if (pointCount > MAX_SAMPLE_COUNT) {
            return GMResult.Err(
                CurveError.InvalidSampleCount(
                    count = pointCount.coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                    maximum = MAX_SAMPLE_COUNT,
                ),
            )
        }
        if (sourceCount == 0) {
            dataX = doubleArrayOf()
            dataY = doubleArrayOf()
            return GMResult.Ok(Unit)
        }

        val expandedX = DoubleArray(pointCount.toInt())
        val expandedY = DoubleArray(pointCount.toInt())
        var targetIndex = 0
        expandedX[targetIndex] = definition.xTerm.valueAt(0)
        expandedY[targetIndex] = definition.yTerm.valueAt(0)
        targetIndex += 1
        for (sourceIndex in 1 until sourceCount) {
            expandedX[targetIndex] = definition.xTerm.valueAt(sourceIndex)
            expandedY[targetIndex] = expandedY[targetIndex - 1]
            targetIndex += 1
            expandedX[targetIndex] = definition.xTerm.valueAt(sourceIndex)
            expandedY[targetIndex] =
                definition.yTerm.valueAt(sourceIndex)
            targetIndex += 1
        }
        dataX = expandedX
        dataY = expandedY
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/element/comb.js -> createComb.updateDataArray.
    private fun updateCombDefinition(
        definition: CurveCombDefinition,
    ): GMResult<Unit, CurveError> {
        val frequency = when (
            val result = evaluateCoordinateNumber(
                termName = "comb.frequency",
                term = definition.frequencyTerm,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (!frequency.isFinite() || frequency <= 0.0) {
            return GMResult.Err(
                CurveError.InvalidCombFrequency(frequency),
            )
        }
        val width = when (
            val result = evaluateCoordinateNumber(
                termName = "comb.width",
                term = definition.widthTerm,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        var angle = -when (
            val result = evaluateCoordinateNumber(
                termName = "comb.angle",
                term = definition.angleTerm,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val reverse = when (
            val result = evaluateCoordinateBoolean(
                termName = "comb.reverse",
                term = definition.reverseTerm,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        val maximumDistance = definition.point1.Dist(definition.point2)
        val requestedPointCount = combPointCount(
            distance = maximumDistance,
            frequency = frequency,
        )
        definition.requestedPointCount = requestedPointCount
        if (requestedPointCount > MAX_SAMPLE_COUNT) {
            return GMResult.Err(
                CurveError.InvalidSampleCount(
                    count = requestedPointCount
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                    maximum = MAX_SAMPLE_COUNT,
                ),
            )
        }
        if (maximumDistance.isNaN() || maximumDistance <= 0.0) {
            dataX = doubleArrayOf()
            dataY = doubleArrayOf()
            return GMResult.Ok(Unit)
        }

        var point1 = definition.point1
        var point2 = definition.point2
        if (reverse) {
            point1 = definition.point2
            point2 = definition.point1
            angle = -angle
        }
        var cosine = cos(angle)
        var sine = sin(angle)
        val deltaX = (point2.X() - point1.X()) / maximumDistance
        val deltaY = (point2.Y() - point1.Y()) / maximumDistance

        // Instead of lifting by sin(angle), the upstream scales by width.
        cosine *= width / abs(sine)
        sine *= width / abs(sine)

        val xCoordinates = mutableListOf<Double>()
        val yCoordinates = mutableListOf<Double>()
        var distance = 0.0
        while (distance < maximumDistance) {
            if (xCoordinates.size + COMB_POINTS_PER_TOOTH > MAX_SAMPLE_COUNT) {
                definition.requestedPointCount =
                    (xCoordinates.size + COMB_POINTS_PER_TOOTH).toLong()
                return GMResult.Err(
                    CurveError.InvalidSampleCount(
                        count = xCoordinates.size + COMB_POINTS_PER_TOOTH,
                        maximum = MAX_SAMPLE_COUNT,
                    ),
                )
            }
            val x = point1.X() + deltaX * distance
            val y = point1.Y() + deltaY * distance

            // The mutation of cosine/sine on the clipped final tooth is
            // intentional and follows the upstream implementation exactly.
            val factor =
                minOf(cosine, maximumDistance - distance) / abs(cosine)
            sine *= factor
            cosine *= factor

            xCoordinates += x
            yCoordinates += y
            xCoordinates += x + deltaX * cosine + deltaY * sine
            yCoordinates += y - deltaX * sine + deltaY * cosine
            xCoordinates += Double.NaN
            yCoordinates += Double.NaN
            distance += frequency
        }
        definition.requestedPointCount = maxOf(
            definition.requestedPointCount,
            xCoordinates.size.toLong(),
        )
        dataX = xCoordinates.toDoubleArray()
        dataY = yCoordinates.toDoubleArray()
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3:
    // src/element/composition.js -> createInequality.updateDataArray.
    private fun updateInequalityDefinition(
        definition: CurveInequalityDefinition,
    ): GMResult<Unit, CurveError> {
        val inverse = when (
            val result = evaluateCoordinateBoolean(
                termName = "inequality.inverse",
                term = definition.inverseTerm,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (val source = definition.source) {
            is Line -> updateLineInequality(
                definition = definition,
                source = source,
                inverse = inverse,
            )
            is Curve -> updateFunctionGraphInequality(
                definition = definition,
                source = source,
                inverse = inverse,
            )
            else -> GMResult.Err(
                CurveError.InvalidInequalitySource(
                    elementType = source.elType,
                    curveType = null,
                ),
            )
        }
    }

    // JSXGraph 1.13.3:
    // src/element/vectorfield.js -> createVectorField.updateDataArray.
    private fun updateVectorFieldDefinition(
        definition: CurveVectorFieldDefinition,
    ): GMResult<Unit, CurveError> {
        val elementType = definition.elementType
        val xValues = when (
            val result = evaluateVectorFieldMesh(
                elementType = elementType,
                axis = "x",
                terms = definition.xData,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val yValues = when (
            val result = evaluateVectorFieldMesh(
                elementType = elementType,
                axis = "y",
                terms = definition.yData,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val scale = when (
            val result = evaluateCoordinateNumber(
                termName = "$elementType.scale",
                term = definition.scaleTerm,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val showArrow = when (
            val result = evaluateCoordinateBoolean(
                termName = "$elementType.arrowhead.enabled",
                term = definition.arrowEnabledTerm,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val arrowSize =
            if (showArrow) {
                when (
                    val result = evaluateCoordinateNumber(
                        termName = "$elementType.arrowhead.size",
                        term = definition.arrowSizeTerm,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            } else {
                VECTOR_FIELD_DEFAULT_ARROW_SIZE
            }
        val arrowAngle =
            if (showArrow) {
                when (
                    val result = evaluateCoordinateNumber(
                        termName = "$elementType.arrowhead.angle",
                        term = definition.arrowAngleTerm,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            } else {
                VECTOR_FIELD_DEFAULT_ARROW_ANGLE
            }
        val pointCount = vectorFieldPointCount(
            xSteps = xValues[1],
            ySteps = yValues[1],
            arrowEnabled = showArrow,
        )
        definition.requestedPointCount = pointCount
        if (pointCount > MAX_SAMPLE_COUNT) {
            return GMResult.Err(
                CurveError.InvalidSampleCount(
                    count = pointCount.coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                    maximum = MAX_SAMPLE_COUNT,
                ),
            )
        }

        val xCount = vectorFieldAxisCount(xValues[1]).toInt()
        val yCount = vectorFieldAxisCount(yValues[1]).toInt()
        if (xCount == 0 || yCount == 0) {
            dataX = doubleArrayOf()
            dataY = doubleArrayOf()
            definition.snapshot = CurveVectorFieldSnapshot(
                vectors = emptyList(),
                arrowEnabled = showArrow,
                arrowSize = arrowSize,
                arrowAngle = arrowAngle,
            )
            return GMResult.Ok(Unit)
        }
        val deltaX = (xValues[2] - xValues[0]) / xValues[1]
        val deltaY = (yValues[2] - yValues[0]) / yValues[1]
        val xCoordinates = ArrayList<Double>(pointCount.toInt())
        val yCoordinates = ArrayList<Double>(pointCount.toInt())
        val vectors = ArrayList<CurveVectorFieldVectorSnapshot>(
            xCount * yCount,
        )
        val arrowLegX = arrowSize / board.unitX
        val arrowLegY = arrowSize / board.unitY

        var x = xValues[0]
        repeat(xCount) {
            var y = yValues[0]
            repeat(yCount) {
                val vector = when (
                    val result = definition.field.evaluate(x, y)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val vectorX = vector.first * scale
                val vectorY = vector.second * scale
                val endX = x + vectorX
                val endY = y + vectorY
                vectors += CurveVectorFieldVectorSnapshot(
                    startX = x,
                    startY = y,
                    endX = endX,
                    endY = endY,
                )
                xCoordinates.add(x)
                xCoordinates.add(endX)
                xCoordinates.add(Double.NaN)
                yCoordinates.add(y)
                yCoordinates.add(endY)
                yCoordinates.add(Double.NaN)

                if (
                    showArrow &&
                    abs(vectorX) + abs(vectorY) > 0.0
                ) {
                    val theta = atan2(vectorY, vectorX)
                    val firstAngle = theta + arrowAngle
                    val secondAngle = theta - arrowAngle
                    xCoordinates.add(endX - cos(firstAngle) * arrowLegX)
                    xCoordinates.add(endX)
                    xCoordinates.add(endX - cos(secondAngle) * arrowLegX)
                    xCoordinates.add(Double.NaN)
                    yCoordinates.add(endY - sin(firstAngle) * arrowLegY)
                    yCoordinates.add(endY)
                    yCoordinates.add(endY - sin(secondAngle) * arrowLegY)
                    yCoordinates.add(Double.NaN)
                }
                y += deltaY
            }
            x += deltaX
        }
        dataX = xCoordinates.toDoubleArray()
        dataY = yCoordinates.toDoubleArray()
        definition.snapshot = CurveVectorFieldSnapshot(
            vectors = vectors,
            arrowEnabled = showArrow,
            arrowSize = arrowSize,
            arrowAngle = arrowAngle,
        )
        return GMResult.Ok(Unit)
    }

    private fun evaluateVectorFieldMesh(
        elementType: String,
        axis: String,
        terms: List<JessieCodeCoordinateFunction>,
    ): GMResult<DoubleArray, CurveError> {
        val values = DoubleArray(3)
        for (index in values.indices) {
            when (
                val result = evaluateCoordinateNumber(
                    termName = "$elementType.${axis}Data[$index]",
                    term = terms[index],
                )
            ) {
                is GMResult.Ok -> values[index] = result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(values)
    }

    private fun updateLineInequality(
        definition: CurveInequalityDefinition,
        source: Line,
        inverse: Boolean,
    ): GMResult<Unit, CurveError> {
        definition.requestedPointCount = INEQUALITY_LINE_POINT_COUNT.toLong()
        val boundingBox = board.getBoundingBox()
        val factor = if (inverse) -1.0 else 1.0
        val expansion = 1.5
        val width = expansion * maxOf(
            boundingBox[2] - boundingBox[0],
            boundingBox[1] - boundingBox[3],
        )
        val edgePoint = doubleArrayOf(
            1.0,
            (boundingBox[0] + boundingBox[2]) * 0.5,
            if (inverse) boundingBox[1] else boundingBox[3],
        )
        val edgeProjection = Geometry.perpendicular(
            lineFirst = source.point1.coords.usrCoords,
            lineSecond = source.point2.coords.usrCoords,
            point = edgePoint,
            lineStandardForm = source.stdform,
        ).point
        var height = expansion * maxOf(
            Mat.hypot(
                edgeProjection[1] - edgePoint[1],
                edgeProjection[2] - edgePoint[2],
            ),
            width,
        )
        height *= factor

        val center = doubleArrayOf(
            1.0,
            (boundingBox[0] + boundingBox[2]) * 0.5,
            (boundingBox[1] + boundingBox[3]) * 0.5,
        )
        val basePoint =
            if (
                abs(Mat.innerProduct(center, source.stdform, 3)) >=
                Mat.eps
            ) {
                Geometry.perpendicular(
                    lineFirst = source.point1.coords.usrCoords,
                    lineSecond = source.point2.coords.usrCoords,
                    point = center,
                    lineStandardForm = source.stdform,
                ).point
            } else {
                center
            }
        val slopeX = source.stdform[1]
        val slopeY = source.stdform[2]
        val firstX = basePoint[1] + slopeY * width
        val firstY = basePoint[2] - slopeX * width
        val secondX = basePoint[1] - slopeY * width
        val secondY = basePoint[2] + slopeX * width

        dataX = doubleArrayOf(
            firstX,
            firstX + slopeX * height,
            secondX + slopeX * height,
            secondX,
            firstX,
        )
        dataY = doubleArrayOf(
            firstY,
            firstY + slopeY * height,
            secondY + slopeY * height,
            secondY,
            firstY,
        )
        return GMResult.Ok(Unit)
    }

    private fun updateFunctionGraphInequality(
        definition: CurveInequalityDefinition,
        source: Curve,
        inverse: Boolean,
    ): GMResult<Unit, CurveError> {
        if (source.curveType != FUNCTION_GRAPH_CURVE_TYPE) {
            return GMResult.Err(
                CurveError.InvalidInequalitySource(
                    elementType = source.elType,
                    curveType = source.curveType,
                ),
            )
        }
        val sourcePoints = source.points
        if (sourcePoints.isEmpty()) {
            definition.requestedPointCount = 0L
            dataX = doubleArrayOf()
            dataY = doubleArrayOf()
            return GMResult.Ok(Unit)
        }

        val segments = mutableListOf<Pair<Int, Int>>()
        var last = -1
        var requestedPointCount = 0L
        while (last < sourcePoints.lastIndex) {
            var first = sourcePoints.size
            for (index in last + 1 until sourcePoints.size) {
                if (sourcePoints[index].isReal()) {
                    first = index
                    break
                }
            }
            if (first >= sourcePoints.size) {
                break
            }
            last = sourcePoints.lastIndex
            for (index in first until sourcePoints.lastIndex) {
                if (!sourcePoints[index + 1].isReal()) {
                    last = index
                    break
                }
            }
            segments += first to last
            requestedPointCount +=
                (last - first + 1).toLong() +
                    INEQUALITY_SEGMENT_EXTRA_POINT_COUNT
            if (last < sourcePoints.lastIndex) {
                requestedPointCount += 1L
            }
        }
        definition.requestedPointCount = requestedPointCount
        if (requestedPointCount > MAX_SAMPLE_COUNT) {
            return GMResult.Err(
                CurveError.InvalidSampleCount(
                    count = requestedPointCount
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                    maximum = MAX_SAMPLE_COUNT,
                ),
            )
        }

        val boundingBox = board.getBoundingBox()
        val enlarge = boundingBox[1] - boundingBox[3]
        boundingBox[1] += enlarge
        boundingBox[3] -= enlarge
        val infinityIndex = if (inverse) 1 else 3
        val minimum = source.minX()
        val maximum = source.maxX()
        var minimumValue = Double.POSITIVE_INFINITY
        var maximumValue = Double.NEGATIVE_INFINITY
        val xCoordinates =
            ArrayList<Double>(requestedPointCount.toInt())
        val yCoordinates =
            ArrayList<Double>(requestedPointCount.toInt())

        for ((first, segmentLast) in segments) {
            val firstX = sourcePoints[first].usrCoords[1]
            val lastX = sourcePoints[segmentLast].usrCoords[1]

            // These assignments are intentionally retained even though the
            // next upstream block overwrites them.
            var curveMinimum =
                if (boundingBox[0] < minimum) minimum else boundingBox[0]
            var curveMaximum =
                if (boundingBox[2] > maximum) maximum else boundingBox[2]
            curveMinimum =
                if (first == 0) curveMinimum else maxOf(curveMinimum, firstX)
            curveMaximum =
                if (segmentLast == sourcePoints.lastIndex) {
                    curveMaximum
                } else {
                    minOf(curveMaximum, lastX)
                }
            curveMinimum = if (first == 0) minimum else firstX
            curveMaximum =
                if (segmentLast == sourcePoints.lastIndex) maximum else lastX

            val baselineIndex = xCoordinates.size
            xCoordinates += curveMinimum
            yCoordinates += boundingBox[infinityIndex]
            xCoordinates += curveMinimum
            yCoordinates += sourcePoints[first].usrCoords[2]
            for (index in first..segmentLast) {
                val coordinates = sourcePoints[index].usrCoords
                xCoordinates += coordinates[1]
                yCoordinates += coordinates[2]
                minimumValue = minOf(coordinates[2], minimumValue)
                maximumValue = maxOf(coordinates[2], maximumValue)
            }
            yCoordinates[baselineIndex] =
                if (inverse) {
                    maximumValue + enlarge
                } else {
                    minimumValue - enlarge
                }
            xCoordinates += curveMaximum
            yCoordinates += sourcePoints[segmentLast].usrCoords[2]
            xCoordinates += curveMaximum
            yCoordinates += boundingBox[infinityIndex]
            xCoordinates += xCoordinates[baselineIndex]
            yCoordinates += yCoordinates[baselineIndex]

            if (segmentLast < sourcePoints.lastIndex) {
                xCoordinates += Double.NaN
                yCoordinates += Double.NaN
            }
        }
        dataX = xCoordinates.toDoubleArray()
        dataY = yCoordinates.toDoubleArray()
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/base/curve.js -> createRiemannsum.updateDataArray.
    private fun updateRiemannDefinition(
        definition: CurveRiemannDefinition,
    ): GMResult<Unit, CurveError> {
        val rectangleCount = when (
            val result = evaluateCoordinateNumber(
                termName = "riemannsum.n",
                term = definition.rectangleCount,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val type = when (
            val result = definition.approximationType.evaluate()
        ) {
            is GMResult.Err -> return GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = "riemannsum.type",
                    error = result.error,
                ),
            )
            is GMResult.Ok ->
                (result.value as? JessieCodeRuntimeValue.StringValue)
                    ?.value ?: RIEMANN_DEFAULT_FALLBACK_TYPE
        }
        val minimum = when (
            val result = evaluateCoordinateNumber(
                termName = "riemannsum.minX",
                term = definition.minimum,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val maximum = when (
            val result = evaluateCoordinateNumber(
                termName = "riemannsum.maxX",
                term = definition.maximum,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val requestedPointCount = riemannPointCount(
            rectangleCount = rectangleCount,
            type = type,
            hasLowerFunction = definition.lowerFunction != null,
        )
        if (requestedPointCount > MAX_SAMPLE_COUNT) {
            return GMResult.Err(
                CurveError.InvalidSampleCount(
                    count = requestedPointCount
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                    maximum = MAX_SAMPLE_COUNT,
                ),
            )
        }

        var functionError: CurveError? = null
        val upperFunction = { value: Double ->
            evaluateRiemannFunction(
                termName = "riemannsum.f",
                term = definition.upperFunction,
                value = value,
                priorError = functionError,
                recordError = { functionError = it },
            )
        }
        val lowerFunction = definition.lowerFunction?.let { term ->
            { value: Double ->
                evaluateRiemannFunction(
                    termName = "riemannsum.g",
                    term = term,
                    value = value,
                    priorError = functionError,
                    recordError = { functionError = it },
                )
            }
        }
        val result = Numerics.riemann(
            upperFunction = upperFunction,
            lowerFunction = lowerFunction,
            rectangleCount = rectangleCount,
            type = type,
            start = minimum,
            end = maximum,
        )
        functionError?.let { return GMResult.Err(it) }
        return when (result) {
            is GMResult.Err -> GMResult.Err(
                CurveError.Numerics("riemann", result.error),
            )
            is GMResult.Ok -> {
                dataX = result.value.xCoordinates
                dataY = result.value.yCoordinates
                definition.sum = result.value.sum
                GMResult.Ok(Unit)
            }
        }
    }

    // JSXGraph 1.13.3: src/base/curve.js ->
    // createBoxPlot.updateDataArray.
    private fun updateBoxPlotDefinition(
        definition: CurveBoxPlotDefinition,
    ): GMResult<Unit, CurveError> {
        val quantiles = DoubleArray(BOX_PLOT_QUANTILE_COUNT)
        for (index in quantiles.indices) {
            quantiles[index] = when (
                val result = evaluateCoordinateNumber(
                    termName = "boxplot.Q[$index]",
                    term = definition.quantileTerms[index],
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        }
        val outliers = if (
            definition.quantileTerms.size > BOX_PLOT_QUANTILE_COUNT
        ) {
            when (
                val result = evaluateBoxPlotOutliers(
                    definition.quantileTerms[BOX_PLOT_QUANTILE_COUNT],
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        } else {
            null
        }
        val axis = when (
            val result = evaluateCoordinateNumber(
                termName = "boxplot.x",
                term = definition.axisTerm,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val width = when (
            val result = evaluateCoordinateNumber(
                termName = "boxplot.w",
                term = definition.widthTerm,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val requestedPointCount = boxPlotPointCount(
            outlierCount = outliers?.size,
            outlierFace = definition.outlierFace,
        )
        if (requestedPointCount > MAX_SAMPLE_COUNT) {
            return GMResult.Err(
                CurveError.InvalidSampleCount(
                    count = requestedPointCount
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                    maximum = MAX_SAMPLE_COUNT,
                ),
            )
        }
        val snapshot = CurveBoxPlotSnapshot(
            quantiles = quantiles,
            outliers = outliers,
            axis = axis,
            width = width,
            direction = definition.direction,
            smallWidth = definition.smallWidth,
            outlierFace = definition.outlierFace,
            outlierSize = definition.outlierSize,
        )
        val geometry = createBoxPlotGeometry(
            quantiles = snapshot.quantiles,
            outliers = snapshot.outliers,
            axis = snapshot.axis,
            width = snapshot.width,
            direction = snapshot.direction,
            smallWidth = snapshot.smallWidth,
            outlierFace = snapshot.outlierFace,
            outlierSize = snapshot.outlierSize,
            unitX = board.unitX,
            unitY = board.unitY,
        )
        dataX = geometry.xCoordinates
        dataY = geometry.yCoordinates
        definition.snapshot = snapshot
        return GMResult.Ok(Unit)
    }

    private fun evaluateBoxPlotOutliers(
        term: JessieCodeCoordinateFunction,
    ): GMResult<DoubleArray?, CurveError> =
        when (val result = term.evaluate()) {
            is GMResult.Err -> GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = "boxplot.Q[5]",
                    error = result.error,
                ),
            )
            is GMResult.Ok -> {
                val values = (
                    result.value as? JessieCodeRuntimeValue.ArrayValue
                    )?.values ?: return GMResult.Ok(null)
                val outliers = DoubleArray(values.size)
                for ((index, value) in values.withIndex()) {
                    val number = value as?
                        JessieCodeRuntimeValue.NumberValue
                        ?: return GMResult.Err(
                            CurveError.NonNumericExpression(
                                term = "boxplot.Q[5][$index]",
                                actualType = curveRuntimeType(value),
                            ),
                        )
                    outliers[index] = number.value
                }
                GMResult.Ok(outliers)
            }
        }

    private fun evaluateCoordinateNumber(
        termName: String,
        term: JessieCodeCoordinateFunction,
        arguments: List<JessieCodeRuntimeValue> = emptyList(),
    ): GMResult<Double, CurveError> =
        when (val result = term.evaluate(arguments)) {
            is GMResult.Err -> GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = termName,
                    error = result.error,
                ),
            )
            is GMResult.Ok -> {
                val number = result.value as?
                    JessieCodeRuntimeValue.NumberValue
                if (number == null) {
                    GMResult.Err(
                        CurveError.NonNumericExpression(
                            term = termName,
                            actualType = curveRuntimeType(result.value),
                        ),
                    )
                } else {
                    GMResult.Ok(number.value)
                }
            }
        }

    private fun evaluateCoordinateBoolean(
        termName: String,
        term: JessieCodeCoordinateFunction,
    ): GMResult<Boolean, CurveError> =
        when (val result = term.evaluate()) {
            is GMResult.Err -> GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = termName,
                    error = result.error,
                ),
            )
            is GMResult.Ok -> {
                val value = result.value
                if (value is JessieCodeRuntimeValue.BooleanValue) {
                    GMResult.Ok(value.value)
                } else {
                    GMResult.Err(
                        CurveError.NonNumericExpression(
                            term = termName,
                            actualType = curveRuntimeType(value),
                        ),
                    )
                }
            }
        }

    private fun evaluateRiemannFunction(
        termName: String,
        term: JessieCodeCoordinateFunction,
        value: Double,
        priorError: CurveError?,
        recordError: (CurveError) -> Unit,
    ): Double {
        if (priorError != null) {
            return Double.NaN
        }
        return when (
            val result = evaluateCoordinateNumber(
                termName = termName,
                term = term,
                arguments = listOf(
                    JessieCodeRuntimeValue.NumberValue(value),
                ),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                recordError(result.error)
                Double.NaN
            }
        }
    }

    private fun replaceDataPoints(
        xData: DoubleArray,
        yData: DoubleArray,
    ) {
        points.clear()
        for (index in xData.indices) {
            points += Coords(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(
                    xData[index],
                    yData.getOrNull(index) ?: Double.NaN,
                ),
                board = board,
            )
        }
        numberPoints = xData.size
    }

    // JSXGraph: src/base/curve.js -> updateDataArray assignment;
    // src/3d/linspace3d.js -> Plane3D element2D.updateDataArray.
    internal fun replaceData(
        xData: DoubleArray,
        yData: DoubleArray,
    ): Curve {
        dataX = xData.copyOf()
        dataY = yData.copyOf()
        replaceDataPoints(
            xData = dataX ?: DoubleArray(0),
            yData = dataY ?: DoubleArray(0),
        )
        evaluationError = null
        return this
    }

    // JSXGraph: src/base/curve.js -> updateDataArray assignment.
    internal fun setDataUpdater(
        updater: CurveDataUpdater,
        ticks3D: Ticks3DDefinition? = null,
        mesh3D: CurveMesh3DDefinition? = null,
    ): Curve {
        dataUpdater = updater
        ticks3DDefinition = ticks3D
        mesh3DDefinition = mesh3D
        return this
    }

    internal fun minX(): Double = evaluateMinimum().valueOrNaN()

    internal fun maxX(): Double = evaluateMaximum().valueOrNaN()

    internal fun Value(): Double = riemannDefinition?.sum ?: Double.NaN

    internal fun boxPlotSnapshot(): CurveBoxPlotSnapshot? =
        boxPlotDefinition?.snapshot

    internal fun vectorFieldSnapshot(): CurveVectorFieldSnapshot? =
        vectorFieldDefinition?.snapshot

    internal fun majorAxis(): Double =
        ellipseDefinition
            ?.let(::evaluateEllipseMajorAxis)
            ?.valueOrNaN()
            ?: hyperbolaDefinition
                ?.let(::evaluateHyperbolaMajorAxis)
            ?.valueOrNaN()
            ?: Double.NaN

    internal fun X(parameter: Double): Double =
        evaluateX(
            parameter = parameter,
            arguments = listOf(
                JessieCodeRuntimeValue.NumberValue(parameter),
            ),
        ).valueOrNaN()

    internal fun Y(parameter: Double): Double =
        evaluateY(
            parameter = parameter,
            arguments = listOf(
                JessieCodeRuntimeValue.NumberValue(parameter),
            ),
        ).valueOrNaN()

    private fun evaluateMinimum(): GMResult<Double, CurveError> {
        ellipseDefinition?.let { definition ->
            return GMResult.Ok(definition.minimum)
        }
        hyperbolaDefinition?.let { definition ->
            return GMResult.Ok(definition.minimum)
        }
        parabolaDefinition?.let { definition ->
            return GMResult.Ok(definition.minimum)
        }
        riemannDefinition?.let { definition ->
            return evaluateCoordinateNumber(
                termName = "riemannsum.minX",
                term = definition.minimum,
            )
        }
        splineDefinition?.let { definition ->
            return GMResult.Ok(
                definition.knots.firstOrNull() ?: Double.NaN,
            )
        }
        cardinalSplineDefinition?.let {
            return GMResult.Ok(0.0)
        }
        derivativeDefinition?.let { definition ->
            return GMResult.Ok(definition.minimum)
        }
        if (dataX != null) {
            return GMResult.Ok(board.defaultCurveMinimum)
        }
        return evaluateNumber("minX", minimumTerm)
    }

    private fun evaluateMaximum(): GMResult<Double, CurveError> {
        ellipseDefinition?.let { definition ->
            return GMResult.Ok(definition.maximum)
        }
        hyperbolaDefinition?.let { definition ->
            return GMResult.Ok(definition.maximum)
        }
        parabolaDefinition?.let { definition ->
            return GMResult.Ok(definition.maximum)
        }
        riemannDefinition?.let { definition ->
            return evaluateCoordinateNumber(
                termName = "riemannsum.maxX",
                term = definition.maximum,
            )
        }
        splineDefinition?.let { definition ->
            return GMResult.Ok(
                definition.knots.lastOrNull() ?: Double.NaN,
            )
        }
        cardinalSplineDefinition?.let { definition ->
            return GMResult.Ok((definition.points.size - 1).toDouble())
        }
        derivativeDefinition?.let { definition ->
            return GMResult.Ok(definition.maximum)
        }
        if (dataX != null) {
            return GMResult.Ok(board.defaultCurveMaximum)
        }
        return evaluateNumber("maxX", maximumTerm)
    }

    private fun evaluateX(
        parameter: Double,
        arguments: List<JessieCodeRuntimeValue>,
        suspendedUpdate: Boolean = false,
    ): GMResult<Double, CurveError> {
        ellipseDefinition?.let { definition ->
            val radius = when (
                val result = evaluateEllipseMajorAxis(definition)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val focalDistance = definition.focus2.Dist(definition.focus1)
            val radialDistance =
                (
                    0.5 *
                        (
                            focalDistance * focalDistance -
                                radius * radius
                            )
                    ) /
                    (focalDistance * cos(parameter) - radius)
            val beta = atan2(
                definition.focus2.Y() - definition.focus1.Y(),
                definition.focus2.X() - definition.focus1.X(),
            )
            if (!suspendedUpdate) {
                when (val result = updateEllipseQuadraticForm(definition)) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            return GMResult.Ok(
                definition.focus1.X() +
                    cos(beta + parameter) * radialDistance,
            )
        }
        hyperbolaDefinition?.let { definition ->
            val radius = when (
                val result = evaluateHyperbolaMajorAxis(definition)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val focalDistance = definition.focus2.Dist(definition.focus1)
            val radialDistance =
                (
                    0.5 *
                        (
                            focalDistance * focalDistance -
                                radius * radius
                            )
                    ) /
                    (focalDistance * cos(parameter) + radius)
            val beta = atan2(
                definition.focus2.Y() - definition.focus1.Y(),
                definition.focus2.X() - definition.focus1.X(),
            )
            if (!suspendedUpdate) {
                when (
                    val result = updateHyperbolaQuadraticForm(definition)
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            return GMResult.Ok(
                definition.focus1.X() +
                    cos(beta + parameter) * radialDistance,
            )
        }
        parabolaDefinition?.let { definition ->
            val radialDistance = parabolaRadialDistance(
                definition = definition,
                parameter = parameter,
            )
            if (!suspendedUpdate) {
                updateParabolaQuadraticForm(definition)
            }
            return GMResult.Ok(
                definition.focus.X() +
                    cos(parameter + definition.directrix.getAngle()) *
                    radialDistance,
            )
        }
        splineDefinition?.let {
            return GMResult.Ok(parameter)
        }
        cardinalSplineDefinition?.let { definition ->
            return GMResult.Ok(
                definition.interpolation?.x(
                    parameter,
                    suspendedUpdate,
                ) ?: Double.NaN,
            )
        }
        derivativeDefinition?.let { definition ->
            return GMResult.Ok(definition.source.X(parameter))
        }
        dataX?.let { values ->
            return GMResult.Ok(interpolateData(values, parameter))
        }
        return evaluateNumber("xterm", xTerm, arguments)
    }

    private fun evaluateY(
        parameter: Double,
        arguments: List<JessieCodeRuntimeValue>,
        suspendedUpdate: Boolean = false,
    ): GMResult<Double, CurveError> {
        ellipseDefinition?.let { definition ->
            val radius = when (
                val result = evaluateEllipseMajorAxis(definition)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val focalDistance = definition.focus2.Dist(definition.focus1)
            val radialDistance =
                (
                    0.5 *
                        (
                            focalDistance * focalDistance -
                                radius * radius
                            )
                    ) /
                    (focalDistance * cos(parameter) - radius)
            val beta = atan2(
                definition.focus2.Y() - definition.focus1.Y(),
                definition.focus2.X() - definition.focus1.X(),
            )
            return GMResult.Ok(
                definition.focus1.Y() +
                    sin(beta + parameter) * radialDistance,
            )
        }
        hyperbolaDefinition?.let { definition ->
            val radius = when (
                val result = evaluateHyperbolaMajorAxis(definition)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val focalDistance = definition.focus2.Dist(definition.focus1)
            val radialDistance =
                (
                    0.5 *
                        (
                            focalDistance * focalDistance -
                                radius * radius
                            )
                    ) /
                    (focalDistance * cos(parameter) + radius)
            val beta = atan2(
                definition.focus2.Y() - definition.focus1.Y(),
                definition.focus2.X() - definition.focus1.X(),
            )
            return GMResult.Ok(
                definition.focus1.Y() +
                    sin(beta + parameter) * radialDistance,
            )
        }
        parabolaDefinition?.let { definition ->
            val radialDistance = parabolaRadialDistance(
                definition = definition,
                parameter = parameter,
            )
            return GMResult.Ok(
                definition.focus.Y() +
                    sin(parameter + definition.directrix.getAngle()) *
                    radialDistance,
            )
        }
        splineDefinition?.let { definition ->
            return when (
                val result = Numerics.splineEval(
                    value = parameter,
                    knots = definition.knots,
                    values = definition.values,
                    secondDerivatives = definition.secondDerivatives,
                )
            ) {
                is GMResult.Ok -> result
                is GMResult.Err -> GMResult.Err(
                    CurveError.Numerics("splineEval", result.error),
                )
            }
        }
        cardinalSplineDefinition?.let { definition ->
            return GMResult.Ok(
                definition.interpolation?.y(
                    parameter,
                    suspendedUpdate,
                ) ?: Double.NaN,
            )
        }
        derivativeDefinition?.let { definition ->
            return GMResult.Ok(
                definition.yDerivative(parameter) /
                    definition.xDerivative(parameter),
            )
        }
        dataY?.let { values ->
            return GMResult.Ok(interpolateData(values, parameter))
        }
        return evaluateNumber("yterm", yTerm, arguments)
    }

    // JSXGraph 1.13.3: src/element/conic.js ->
    // createEllipse.majorAxis / polarForm.
    private fun evaluateEllipseMajorAxis(
        definition: CurveEllipseDefinition,
    ): GMResult<Double, CurveError> {
        definition.pointOnEllipse?.let { point ->
            return GMResult.Ok(
                point.Dist(definition.focus1) +
                    point.Dist(definition.focus2),
            )
        }
        val term = definition.majorAxisTerm
            ?: return GMResult.Ok(Double.NaN)
        return evaluateCoordinateNumber(
            termName = "ellipse.majorAxis",
            term = term,
        )
    }

    private fun updateEllipseQuadraticForm(
        definition: CurveEllipseDefinition,
    ): GMResult<Unit, CurveError> {
        val radius = when (
            val result = evaluateEllipseMajorAxis(definition)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val radiusSquared = radius * radius
        val ax = definition.focus1.X()
        val ay = definition.focus1.Y()
        val bx = definition.focus2.X()
        val by = definition.focus2.Y()
        val axbx = ax - bx
        val ayby = ay - by
        val f = (
            radiusSquared -
                ax * ax -
                ay * ay +
                bx * bx +
                by * by
            ) / (2.0 * radius)
        quadraticform = arrayOf(
            doubleArrayOf(
                f * f - bx * bx - by * by,
                f * axbx / radius + bx,
                f * ayby / radius + by,
            ),
            doubleArrayOf(
                f * axbx / radius + bx,
                axbx * axbx / radiusSquared - 1.0,
                axbx * ayby / radiusSquared,
            ),
            doubleArrayOf(
                f * ayby / radius + by,
                axbx * ayby / radiusSquared,
                ayby * ayby / radiusSquared - 1.0,
            ),
        )
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/element/conic.js ->
    // createHyperbola.majorAxis / polarForm.
    private fun evaluateHyperbolaMajorAxis(
        definition: CurveHyperbolaDefinition,
    ): GMResult<Double, CurveError> {
        definition.pointOnHyperbola?.let { point ->
            return GMResult.Ok(
                point.Dist(definition.focus1) -
                    point.Dist(definition.focus2),
            )
        }
        val term = definition.majorAxisTerm
            ?: return GMResult.Ok(Double.NaN)
        return evaluateCoordinateNumber(
            termName = "hyperbola.majorAxis",
            term = term,
        )
    }

    private fun updateHyperbolaQuadraticForm(
        definition: CurveHyperbolaDefinition,
    ): GMResult<Unit, CurveError> {
        val radius = when (
            val result = evaluateHyperbolaMajorAxis(definition)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val radiusSquared = radius * radius
        val ax = definition.focus1.X()
        val ay = definition.focus1.Y()
        val bx = definition.focus2.X()
        val by = definition.focus2.Y()
        val axbx = ax - bx
        val ayby = ay - by
        val f = (
            radiusSquared -
                ax * ax -
                ay * ay +
                bx * bx +
                by * by
            ) / (2.0 * radius)
        quadraticform = arrayOf(
            doubleArrayOf(
                f * f - bx * bx - by * by,
                f * axbx / radius + bx,
                f * ayby / radius + by,
            ),
            doubleArrayOf(
                f * axbx / radius + bx,
                axbx * axbx / radiusSquared - 1.0,
                axbx * ayby / radiusSquared,
            ),
            doubleArrayOf(
                f * ayby / radius + by,
                axbx * ayby / radiusSquared,
                ayby * ayby / radiusSquared - 1.0,
            ),
        )
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/element/conic.js ->
    // createParabola.X / createParabola.Y.
    private fun parabolaRadialDistance(
        definition: CurveParabolaDefinition,
        parameter: Double,
    ): Double {
        val directrix = definition.directrix
        var first = directrix.point1.coords.usrCoords
        var second = directrix.point2.coords.usrCoords
        if (first[0] == 0.0) {
            first = doubleArrayOf(
                1.0,
                second[1] + directrix.stdform[2],
                second[2] - directrix.stdform[1],
            )
        } else if (second[0] == 0.0) {
            second = doubleArrayOf(
                1.0,
                first[1] + directrix.stdform[2],
                first[2] - directrix.stdform[1],
            )
        }
        val focus = definition.focus.coords.usrCoords
        val determinant =
            if (
                (
                    (second[1] - first[1]) *
                        (focus[2] - first[2]) -
                        (second[2] - first[2]) *
                        (focus[1] - first[1])
                    ) >= 0.0
            ) {
                1.0
            } else {
                -1.0
            }
        val distance = Geometry.distPointLine(
            point = focus,
            line = directrix.stdform,
        )
        return determinant * distance / (1.0 - sin(parameter))
    }

    // JSXGraph 1.13.3: src/element/conic.js ->
    // createParabola.polarForm.
    private fun updateParabolaQuadraticForm(
        definition: CurveParabolaDefinition,
    ) {
        val directrix = definition.directrix
        val horizontal = directrix.stdform[1]
        val vertical = directrix.stdform[2]
        val constant = directrix.stdform[0]
        val squaredNorm =
            horizontal * horizontal + vertical * vertical
        val focusX = definition.focus.X()
        val focusY = definition.focus.Y()
        quadraticform = arrayOf(
            doubleArrayOf(
                constant * constant -
                    squaredNorm *
                    (focusX * focusX + focusY * focusY),
                constant * horizontal + squaredNorm * focusX,
                constant * vertical + squaredNorm * focusY,
            ),
            doubleArrayOf(
                constant * horizontal + squaredNorm * focusX,
                -vertical * vertical,
                horizontal * vertical,
            ),
            doubleArrayOf(
                constant * vertical + squaredNorm * focusY,
                horizontal * vertical,
                -horizontal * horizontal,
            ),
        )
    }

    // JSXGraph 1.13.3:
    // src/base/curve.js -> interpolationFunctionFromArray.
    private fun interpolateData(
        values: DoubleArray,
        parameter: Double,
    ): Double {
        if (parameter.isNaN() || values.isEmpty()) {
            return Double.NaN
        }
        if (parameter < 0.0) {
            return values[0]
        }
        val index =
            if (parameter > values.size - 2) {
                values.size - 2
            } else {
                floor(parameter).toInt()
            }
        if (index.toDouble() == parameter) {
            return values.getOrNull(index) ?: Double.NaN
        }
        val first = values.getOrNull(index) ?: Double.NaN
        val second = values.getOrNull(index + 1) ?: Double.NaN
        return first + (second - first) * (parameter - index)
    }

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
        internal const val COMB_DEFAULT_FREQUENCY: Double = 0.2
        internal const val COMB_DEFAULT_WIDTH: Double = 0.4
        internal const val COMB_DEFAULT_ANGLE: Double =
            kotlin.math.PI / 3.0
        internal const val VECTOR_FIELD_DEFAULT_SCALE: Double = 1.0
        internal const val VECTOR_FIELD_DEFAULT_ARROW_SIZE: Double = 5.0
        internal const val VECTOR_FIELD_DEFAULT_ARROW_ANGLE: Double =
            kotlin.math.PI * 0.125

        private const val CURVE_ID_PREFIX = "G"
        private const val CURVE_ELEMENT_TYPE = "curve"
        private const val SPLINE_ELEMENT_TYPE = "spline"
        private const val CARDINAL_SPLINE_ELEMENT_TYPE = "cardinalspline"
        private const val RIEMANN_DEFAULT_FALLBACK_TYPE =
            "__riemann_default__"
        private const val DATA_CURVE_TYPE = "plot"
        private const val PARAMETRIC_CURVE_TYPE = "parameter"
        private const val FUNCTION_GRAPH_CURVE_TYPE = "functiongraph"
        private const val COMB_POINTS_PER_TOOTH = 3
        private const val INEQUALITY_LINE_POINT_COUNT = 5
        private const val INEQUALITY_SEGMENT_EXTRA_POINT_COUNT = 5L
        private const val VECTOR_FIELD_BODY_POINT_COUNT = 3L
        private const val VECTOR_FIELD_ARROW_POINT_COUNT = 4L

        // JSXGraph 1.13.3: src/element/conic.js -> createEllipse.
        internal fun createEllipse(
            board: Board,
            definition: CurveEllipseDefinition,
            parentlessPoints: Set<Point>,
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            if (sampleCount !in 1..MAX_SAMPLE_COUNT) {
                return GMResult.Err(
                    CurveError.InvalidSampleCount(
                        count = sampleCount,
                        maximum = MAX_SAMPLE_COUNT,
                    ),
                )
            }
            return when (
                val result = register(
                    curve = Curve(
                        board = board,
                        curveType = PARAMETRIC_CURVE_TYPE,
                        xTerm = null,
                        yTerm = null,
                        minimumTerm = null,
                        maximumTerm = null,
                        dataX = null,
                        dataY = null,
                        sampleCount = sampleCount,
                        ellipseDefinition = definition,
                        id = id,
                        name = name,
                        needsRegularUpdate = needsRegularUpdate,
                    ),
                    expressions = listOfNotNull(definition.majorAxisTerm),
                )
            ) {
                is GMResult.Ok -> {
                    val curve = result.value
                    curve.type = Const.OBJECT_TYPE_CONIC
                    curve.subs["center"] = definition.center
                    curve.inherits += listOf(
                        definition.center,
                        definition.focus1,
                        definition.focus2,
                    )
                    definition.pointOnEllipse?.let(curve.inherits::add)
                    val dependencies = listOfNotNull(
                        definition.center,
                        definition.focus1,
                        definition.focus2,
                        definition.pointOnEllipse,
                    )
                    for (dependency in dependencies) {
                        dependency.addChild(curve)
                    }
                    curve.setParents(
                        listOfNotNull(
                            definition.focus1,
                            definition.focus2,
                            definition.pointOnEllipse,
                        ).filterNot(parentlessPoints::contains),
                    )
                    GMResult.Ok(curve)
                }
                is GMResult.Err -> result
            }
        }

        // JSXGraph 1.13.3: src/element/conic.js -> createHyperbola.
        internal fun createHyperbola(
            board: Board,
            definition: CurveHyperbolaDefinition,
            parentlessPoints: Set<Point>,
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            if (sampleCount !in 1..MAX_SAMPLE_COUNT) {
                return GMResult.Err(
                    CurveError.InvalidSampleCount(
                        count = sampleCount,
                        maximum = MAX_SAMPLE_COUNT,
                    ),
                )
            }
            return when (
                val result = register(
                    curve = Curve(
                        board = board,
                        curveType = PARAMETRIC_CURVE_TYPE,
                        xTerm = null,
                        yTerm = null,
                        minimumTerm = null,
                        maximumTerm = null,
                        dataX = null,
                        dataY = null,
                        sampleCount = sampleCount,
                        hyperbolaDefinition = definition,
                        id = id,
                        name = name,
                        needsRegularUpdate = needsRegularUpdate,
                    ),
                    expressions = listOfNotNull(definition.majorAxisTerm),
                )
            ) {
                is GMResult.Ok -> {
                    val curve = result.value
                    curve.type = Const.OBJECT_TYPE_CONIC
                    curve.subs["center"] = definition.center
                    curve.inherits += listOf(
                        definition.center,
                        definition.focus1,
                        definition.focus2,
                    )
                    definition.pointOnHyperbola?.let(curve.inherits::add)
                    val dependencies = listOfNotNull(
                        definition.center,
                        definition.focus1,
                        definition.focus2,
                        definition.pointOnHyperbola,
                    )
                    for (dependency in dependencies) {
                        dependency.addChild(curve)
                    }
                    curve.setParents(
                        listOfNotNull(
                            definition.focus1,
                            definition.focus2,
                            definition.pointOnHyperbola,
                        ).filterNot(parentlessPoints::contains),
                    )
                    GMResult.Ok(curve)
                }
                is GMResult.Err -> result
            }
        }

        // JSXGraph 1.13.3: src/element/conic.js -> createParabola.
        internal fun createParabola(
            board: Board,
            definition: CurveParabolaDefinition,
            parentlessElements: Set<GeometryElement>,
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            if (sampleCount !in 1..MAX_SAMPLE_COUNT) {
                return GMResult.Err(
                    CurveError.InvalidSampleCount(
                        count = sampleCount,
                        maximum = MAX_SAMPLE_COUNT,
                    ),
                )
            }
            return when (
                val result = register(
                    curve = Curve(
                        board = board,
                        curveType = PARAMETRIC_CURVE_TYPE,
                        xTerm = null,
                        yTerm = null,
                        minimumTerm = null,
                        maximumTerm = null,
                        dataX = null,
                        dataY = null,
                        sampleCount = sampleCount,
                        parabolaDefinition = definition,
                        id = id,
                        name = name,
                        needsRegularUpdate = needsRegularUpdate,
                    ),
                    expressions = emptyList(),
                )
            ) {
                is GMResult.Ok -> {
                    val curve = result.value
                    curve.type = Const.OBJECT_TYPE_CONIC
                    curve.subs["center"] = definition.center
                    curve.inherits += listOf(
                        definition.center,
                        definition.focus,
                    )
                    val dependencies = listOf(
                        definition.center,
                        definition.focus,
                        definition.directrix,
                    )
                    for (dependency in dependencies) {
                        dependency.addChild(curve)
                    }
                    curve.setParents(
                        listOf<GeometryElement>(
                            definition.focus,
                            definition.directrix,
                        ).filterNot(parentlessElements::contains),
                    )
                    GMResult.Ok(curve)
                }
                is GMResult.Err -> result
            }
        }

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

        // JSXGraph 1.13.3: src/base/curve.js -> createStepfunction.
        internal fun createStepfunction(
            board: Board,
            xTerm: CurveStepTerm,
            yTerm: CurveStepTerm,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> =
            register(
                Curve(
                    board = board,
                    curveType =
                        if (xTerm.isFunction) {
                            PARAMETRIC_CURVE_TYPE
                        } else {
                            DATA_CURVE_TYPE
                        },
                    xTerm = null,
                    yTerm = null,
                    minimumTerm = null,
                    maximumTerm = null,
                    dataX = null,
                    dataY = null,
                    sampleCount = 0,
                    stepDefinition = CurveStepDefinition(
                        xTerm = xTerm,
                        yTerm = yTerm,
                    ),
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                expressions = emptyList(),
            )

        // JSXGraph 1.13.3: src/element/comb.js -> createComb.
        internal fun createComb(
            board: Board,
            point1: Point,
            point2: Point,
            frequencyTerm: JessieCodeCoordinateFunction,
            widthTerm: JessieCodeCoordinateFunction,
            angleTerm: JessieCodeCoordinateFunction,
            reverseTerm: JessieCodeCoordinateFunction,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> =
            register(
                curve = Curve(
                    board = board,
                    curveType = DATA_CURVE_TYPE,
                    xTerm = null,
                    yTerm = null,
                    minimumTerm = null,
                    maximumTerm = null,
                    dataX = null,
                    dataY = null,
                    sampleCount = 0,
                    combDefinition = CurveCombDefinition(
                        point1 = point1,
                        point2 = point2,
                        frequencyTerm = frequencyTerm,
                        widthTerm = widthTerm,
                        angleTerm = angleTerm,
                        reverseTerm = reverseTerm,
                    ),
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                expressions = emptyList(),
            )

        // JSXGraph 1.13.3:
        // src/element/composition.js -> createInequality.
        internal fun createInequality(
            board: Board,
            source: GeometryElement,
            inverseTerm: JessieCodeCoordinateFunction,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            if (
                source !is Line &&
                (
                    source !is Curve ||
                        source.curveType != FUNCTION_GRAPH_CURVE_TYPE
                    )
            ) {
                return GMResult.Err(
                    CurveError.InvalidInequalitySource(
                        elementType = source.elType,
                        curveType = (source as? Curve)?.curveType,
                    ),
                )
            }
            return when (
                val result = register(
                    curve = Curve(
                        board = board,
                        curveType = DATA_CURVE_TYPE,
                        xTerm = null,
                        yTerm = null,
                        minimumTerm = null,
                        maximumTerm = null,
                        dataX = null,
                        dataY = null,
                        sampleCount = 0,
                        inequalityDefinition = CurveInequalityDefinition(
                            source = source,
                            inverseTerm = inverseTerm,
                        ),
                        id = id,
                        name = name,
                        needsRegularUpdate = needsRegularUpdate,
                    ),
                    expressions = emptyList(),
                )
            ) {
                is GMResult.Ok -> {
                    result.value.setParents(listOf(source))
                    result
                }
                is GMResult.Err -> result
            }
        }

        // JSXGraph 1.13.3:
        // src/element/vectorfield.js -> createVectorField.
        internal fun createVectorField(
            board: Board,
            field: CurveVectorFieldFunction,
            xData: List<JessieCodeCoordinateFunction>,
            yData: List<JessieCodeCoordinateFunction>,
            scaleTerm: JessieCodeCoordinateFunction,
            arrowEnabledTerm: JessieCodeCoordinateFunction,
            arrowSizeTerm: JessieCodeCoordinateFunction,
            arrowAngleTerm: JessieCodeCoordinateFunction,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> =
            createVectorFieldLike(
                board = board,
                elementType = "vectorfield",
                field = field,
                xData = xData,
                yData = yData,
                scaleTerm = scaleTerm,
                arrowEnabledTerm = arrowEnabledTerm,
                arrowSizeTerm = arrowSizeTerm,
                arrowAngleTerm = arrowAngleTerm,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        // JSXGraph 1.13.3:
        // src/element/vectorfield.js -> createSlopeField.
        internal fun createSlopeField(
            board: Board,
            field: CurveVectorFieldFunction,
            xData: List<JessieCodeCoordinateFunction>,
            yData: List<JessieCodeCoordinateFunction>,
            scaleTerm: JessieCodeCoordinateFunction,
            arrowEnabledTerm: JessieCodeCoordinateFunction,
            arrowSizeTerm: JessieCodeCoordinateFunction,
            arrowAngleTerm: JessieCodeCoordinateFunction,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> =
            createVectorFieldLike(
                board = board,
                elementType = "slopefield",
                field = field,
                xData = xData,
                yData = yData,
                scaleTerm = scaleTerm,
                arrowEnabledTerm = arrowEnabledTerm,
                arrowSizeTerm = arrowSizeTerm,
                arrowAngleTerm = arrowAngleTerm,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        private fun createVectorFieldLike(
            board: Board,
            elementType: String,
            field: CurveVectorFieldFunction,
            xData: List<JessieCodeCoordinateFunction>,
            yData: List<JessieCodeCoordinateFunction>,
            scaleTerm: JessieCodeCoordinateFunction,
            arrowEnabledTerm: JessieCodeCoordinateFunction,
            arrowSizeTerm: JessieCodeCoordinateFunction,
            arrowAngleTerm: JessieCodeCoordinateFunction,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
        ): GMResult<Curve, CurveError> {
            if (xData.size != 3 || yData.size != 3) {
                return GMResult.Err(
                    CurveError.NonNumericExpression(
                        term = "$elementType.mesh",
                        actualType = "invalid length",
                    ),
                )
            }
            return when (
                val result = register(
                    curve = Curve(
                        board = board,
                        curveType = DATA_CURVE_TYPE,
                        xTerm = null,
                        yTerm = null,
                        minimumTerm = null,
                        maximumTerm = null,
                        dataX = null,
                        dataY = null,
                        sampleCount = 0,
                        vectorFieldDefinition = CurveVectorFieldDefinition(
                            elementType = elementType,
                            field = field,
                            xData = xData,
                            yData = yData,
                            scaleTerm = scaleTerm,
                            arrowEnabledTerm = arrowEnabledTerm,
                            arrowSizeTerm = arrowSizeTerm,
                            arrowAngleTerm = arrowAngleTerm,
                        ),
                        id = id,
                        name = name,
                        needsRegularUpdate = needsRegularUpdate,
                    ),
                    expressions = emptyList(),
                )
            ) {
                is GMResult.Ok -> {
                    result.value.elType = elementType
                    result
                }
                is GMResult.Err -> result
            }
        }

        // JSXGraph 1.13.3: src/base/curve.js -> createSpline.
        internal fun createSpline(
            board: Board,
            points: List<CurveSplinePoint>,
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            if (points.size < 2) {
                return GMResult.Err(
                    CurveError.InvalidInterpolationPointCount(
                        creator = SPLINE_ELEMENT_TYPE,
                        count = points.size,
                        minimum = 2,
                    ),
                )
            }
            if (sampleCount !in 1..MAX_SAMPLE_COUNT) {
                return GMResult.Err(
                    CurveError.InvalidSampleCount(
                        count = sampleCount,
                        maximum = MAX_SAMPLE_COUNT,
                    ),
                )
            }
            return when (
                val result = register(
                    Curve(
                        board = board,
                        curveType = FUNCTION_GRAPH_CURVE_TYPE,
                        xTerm = null,
                        yTerm = null,
                        minimumTerm = null,
                        maximumTerm = null,
                        dataX = null,
                        dataY = null,
                        sampleCount = sampleCount,
                        splineDefinition = CurveSplineDefinition(points),
                        id = id,
                        name = name,
                        needsRegularUpdate = needsRegularUpdate,
                    ),
                    expressions = emptyList(),
                )
            ) {
                is GMResult.Ok -> {
                    result.value.elType = SPLINE_ELEMENT_TYPE
                    result.value.setParents(
                        points.mapNotNull(CurveSplinePoint::parentElement),
                    )
                    result
                }
                is GMResult.Err -> result
            }
        }

        // JSXGraph 1.13.3:
        // src/base/curve.js -> createCardinalSpline.
        internal fun createCardinalSpline(
            board: Board,
            points: List<NumericsPoint2D>,
            tensionTerm: JessieCodeCoordinateFunction,
            type: String = "uniform",
            ownedPoints: Set<Point> = emptySet(),
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            if (points.size < 2) {
                return GMResult.Err(
                    CurveError.InvalidInterpolationPointCount(
                        creator = CARDINAL_SPLINE_ELEMENT_TYPE,
                        count = points.size,
                        minimum = 2,
                    ),
                )
            }
            if (sampleCount !in 1..MAX_SAMPLE_COUNT) {
                return GMResult.Err(
                    CurveError.InvalidSampleCount(
                        count = sampleCount,
                        maximum = MAX_SAMPLE_COUNT,
                    ),
                )
            }
            return when (
                val result = register(
                    Curve(
                        board = board,
                        curveType = PARAMETRIC_CURVE_TYPE,
                        xTerm = null,
                        yTerm = null,
                        minimumTerm = null,
                        maximumTerm = null,
                        dataX = null,
                        dataY = null,
                        sampleCount = sampleCount,
                        cardinalSplineDefinition =
                            CurveCardinalSplineDefinition(
                                points = points,
                                tensionTerm = tensionTerm,
                                type = type,
                            ),
                        id = id,
                        name = name,
                        needsRegularUpdate = needsRegularUpdate,
                    ),
                    expressions = emptyList(),
                )
            ) {
                is GMResult.Ok -> {
                    val curve = result.value
                    curve.elType = CARDINAL_SPLINE_ELEMENT_TYPE
                    val pointElements = points.filterIsInstance<Point>()
                    curve.setParents(pointElements)
                    for (point in pointElements) {
                        if (point in ownedPoints) {
                            curve.addChild(point)
                        } else {
                            point.addChild(curve)
                        }
                    }
                    result
                }
                is GMResult.Err -> result
            }
        }

        // JSXGraph 1.13.3: src/base/curve.js -> createRiemannsum.
        internal fun createRiemannSum(
            board: Board,
            upperFunction: JessieCodeCoordinateFunction,
            lowerFunction: JessieCodeCoordinateFunction?,
            rectangleCount: JessieCodeCoordinateFunction,
            approximationType: JessieCodeCoordinateFunction,
            minimum: JessieCodeCoordinateFunction,
            maximum: JessieCodeCoordinateFunction,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> =
            register(
                curve = Curve(
                    board = board,
                    curveType = DATA_CURVE_TYPE,
                    xTerm = null,
                    yTerm = null,
                    minimumTerm = null,
                    maximumTerm = null,
                    dataX = null,
                    dataY = null,
                    sampleCount = 0,
                    riemannDefinition = CurveRiemannDefinition(
                        upperFunction = upperFunction,
                        lowerFunction = lowerFunction,
                        rectangleCount = rectangleCount,
                        approximationType = approximationType,
                        minimum = minimum,
                        maximum = maximum,
                    ),
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                expressions = listOfNotNull(
                    rectangleCount,
                    approximationType,
                    minimum,
                    maximum,
                ),
            )

        // JSXGraph 1.13.3: src/base/curve.js -> createBoxPlot.
        internal fun createBoxPlot(
            board: Board,
            quantileTerms: List<JessieCodeCoordinateFunction>,
            axisTerm: JessieCodeCoordinateFunction,
            widthTerm: JessieCodeCoordinateFunction,
            direction: String = BOX_PLOT_VERTICAL,
            smallWidth: Double = 0.5,
            outlierFace: String = "o",
            outlierSize: Double = 3.0,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            if (quantileTerms.size < BOX_PLOT_QUANTILE_COUNT) {
                return GMResult.Err(
                    CurveError.InvalidInterpolationPointCount(
                        creator = "boxplot",
                        count = quantileTerms.size,
                        minimum = BOX_PLOT_QUANTILE_COUNT,
                    ),
                )
            }
            return register(
                curve = Curve(
                    board = board,
                    curveType = DATA_CURVE_TYPE,
                    xTerm = null,
                    yTerm = null,
                    minimumTerm = null,
                    maximumTerm = null,
                    dataX = null,
                    dataY = null,
                    sampleCount = 0,
                    boxPlotDefinition = CurveBoxPlotDefinition(
                        quantileTerms = quantileTerms,
                        axisTerm = axisTerm,
                        widthTerm = widthTerm,
                        direction = direction,
                        smallWidth = smallWidth,
                        outlierFace = outlierFace,
                        outlierSize = outlierSize,
                    ),
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                expressions = quantileTerms + axisTerm + widthTerm,
            )
        }

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

        // JSXGraph: src/base/curve.js -> generateTerm Type.isString(xterm).
        // JessieCode string x-terms retain the upstream "functiongraph"
        // classification even when createCurve receives four parents.
        internal fun createStringParametric(
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
                curveType = FUNCTION_GRAPH_CURVE_TYPE,
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

        // JSXGraph 1.13.3: src/base/curve.js -> createDerivative.
        internal fun createDerivative(
            board: Board,
            source: Curve,
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            if (sampleCount !in 1..MAX_SAMPLE_COUNT) {
                return GMResult.Err(
                    CurveError.InvalidSampleCount(
                        count = sampleCount,
                        maximum = MAX_SAMPLE_COUNT,
                    ),
                )
            }
            val definition = CurveDerivativeDefinition(
                source = source,
                xDerivative = Numerics.D(source::X),
                yDerivative = Numerics.D(source::Y),
                minimum = source.minX(),
                maximum = source.maxX(),
            )
            return when (
                val result = register(
                    curve = Curve(
                        board = board,
                        curveType = PARAMETRIC_CURVE_TYPE,
                        xTerm = null,
                        yTerm = null,
                        minimumTerm = null,
                        maximumTerm = null,
                        dataX = null,
                        dataY = null,
                        sampleCount = sampleCount,
                        derivativeDefinition = definition,
                        id = id,
                        name = name,
                        needsRegularUpdate = needsRegularUpdate,
                    ),
                    expressions = emptyList(),
                )
            ) {
                is GMResult.Ok -> {
                    result.value.setParents(listOf(source))
                    result
                }
                is GMResult.Err -> result
            }
        }

        // JSXGraph: src/base/curve.js -> createCurveIntersection,
        // createCurveUnion, createCurveDifference.
        internal fun createBoolean(
            board: Board,
            subject: GeometryElement,
            clip: GeometryElement,
            operation: ClipBooleanOperation,
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
                    dataX = null,
                    dataY = null,
                    sampleCount = 0,
                    booleanDefinition = CurveBooleanDefinition(
                        subject = subject,
                        clip = clip,
                        operation = operation,
                    ),
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                expressions = emptyList(),
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
            expressions: List<JessieCodeCoordinateFunction>,
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
                is JessieCodeRuntimeValue.TransformationReference ->
                    "transformation"
                is JessieCodeRuntimeValue.CompositionReference ->
                    "composition"
                is JessieCodeRuntimeValue.ElementReference -> "element"
            }

        internal fun riemannPointCount(
            rectangleCount: Double,
            type: String,
            hasLowerFunction: Boolean,
        ): Long {
            if (!rectangleCount.isFinite()) {
                return Long.MAX_VALUE
            }
            val count = floor(rectangleCount)
            if (count <= 0.0) {
                return 0L
            }
            if (count > Long.MAX_VALUE.toDouble()) {
                return Long.MAX_VALUE
            }
            val multiplier =
                if (type == "simpson") {
                    if (hasLowerFunction) 63L else 34L
                } else {
                    5L
                }
            val integralCount = count.toLong()
            return if (integralCount > Long.MAX_VALUE / multiplier) {
                Long.MAX_VALUE
            } else {
                integralCount * multiplier
            }
        }

        internal fun combPointCount(
            distance: Double,
            frequency: Double,
        ): Long {
            if (
                distance.isNaN() ||
                distance <= 0.0
            ) {
                return 0L
            }
            if (
                !distance.isFinite() ||
                !frequency.isFinite() ||
                frequency <= 0.0
            ) {
                return Long.MAX_VALUE
            }
            val toothCount = ceil(distance / frequency)
            if (
                !toothCount.isFinite() ||
                toothCount > Long.MAX_VALUE / COMB_POINTS_PER_TOOTH
            ) {
                return Long.MAX_VALUE
            }
            return toothCount.toLong() * COMB_POINTS_PER_TOOTH
        }

        internal fun vectorFieldPointCount(
            xSteps: Double,
            ySteps: Double,
            arrowEnabled: Boolean,
        ): Long {
            val xCount = vectorFieldAxisCount(xSteps)
            val yCount = vectorFieldAxisCount(ySteps)
            if (xCount == 0L || yCount == 0L) {
                return 0L
            }
            if (xCount > Long.MAX_VALUE / yCount) {
                return Long.MAX_VALUE
            }
            val vectorCount = xCount * yCount
            val pointsPerVector =
                VECTOR_FIELD_BODY_POINT_COUNT +
                    if (arrowEnabled) {
                        VECTOR_FIELD_ARROW_POINT_COUNT
                    } else {
                        0L
                    }
            return if (vectorCount > Long.MAX_VALUE / pointsPerVector) {
                Long.MAX_VALUE
            } else {
                vectorCount * pointsPerVector
            }
        }

        private fun vectorFieldAxisCount(steps: Double): Long =
            when {
                steps.isNaN() || steps < 0.0 -> 0L
                !steps.isFinite() ||
                    steps >= Long.MAX_VALUE.toDouble() -> Long.MAX_VALUE
                else -> floor(steps).toLong() + 1L
            }

        internal const val BOX_PLOT_QUANTILE_COUNT = 5
    }
}
