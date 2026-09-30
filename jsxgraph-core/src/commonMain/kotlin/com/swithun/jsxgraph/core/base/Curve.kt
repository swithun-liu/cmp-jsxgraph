/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/curve.js -> Curve, generateTerm, updateCurve,
 * createStepfunction, createDerivative, createSketchCurve,
 * interpolationFunctionFromArray,
 * createSpline, createCardinalSpline, createMetapostSpline,
 * createRiemannsum, createBoxPlot,
 * src/math/plot.js -> updateParametricCurveNaive /
 * updateParametricCurveOld / updateParametricCurve_v2,
 * src/element/comb.js -> createComb,
 * src/element/composition.js -> createInequality,
 * src/element/vectorfield.js -> createVectorField / createSlopeField,
 * src/element/conic.js ->
 * createEllipse / createHyperbola / createParabola / createConic
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphGrid2D
import com.swithun.jsxgraph.core.math.Clip
import com.swithun.jsxgraph.core.math.ClipBooleanOperation
import com.swithun.jsxgraph.core.math.ClipError
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.ImplicitPlot
import com.swithun.jsxgraph.core.math.ImplicitPlotConfig
import com.swithun.jsxgraph.core.math.ImplicitPlotError
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.MetaPost
import com.swithun.jsxgraph.core.math.MetaPostControlPair
import com.swithun.jsxgraph.core.math.MetaPostControls
import com.swithun.jsxgraph.core.math.MetaPostPoint
import com.swithun.jsxgraph.core.math.MetaPostPointControl
import com.swithun.jsxgraph.core.math.Numerics
import com.swithun.jsxgraph.core.math.NumericsError
import com.swithun.jsxgraph.core.math.NumericsPoint2D
import com.swithun.jsxgraph.core.math.Plot
import com.swithun.jsxgraph.core.math.PlotError
import com.swithun.jsxgraph.core.math.PlotFunction
import com.swithun.jsxgraph.core.math.PlotInterval
import com.swithun.jsxgraph.core.math.PlotIntervalFunction
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionFunction
import com.swithun.jsxgraph.core.parser.JessieCodeIntervalEvaluation
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

    data class InvalidAdaptivePointCount(
        val count: Int,
        val maximum: Int,
    ) : CurveError

    data class UnsupportedPlotVersion(
        val version: Int,
    ) : CurveError

    data class InvalidRecursionDepth(
        val depth: Int,
        val maximum: Int,
    ) : CurveError

    data class PlotEvaluationException(
        val coordinate: String,
        val parameter: Double,
        val message: String,
    ) : CurveError

    data class PlotNumerics(
        val operation: String,
        val error: NumericsError,
    ) : CurveError

    data class PlotExtrapolation(
        val coordinate: String,
        val parameter: Double,
        val message: String,
    ) : CurveError

    data class PlotInvalidSpecialInterval(
        val type: String,
        val start: Double,
        val end: Double,
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

    data object EmptyTransformationList : CurveError

    data object TransformationSourceBoardMismatch : CurveError

    data class TransformationSourceNotRegistered(
        val id: String,
    ) : CurveError

    data class TransformationEvaluation(
        val index: Int,
        val error: TransformationError,
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

    data class InvalidMetaPostControl(
        val control: String,
        val expected: String,
        val actualType: String,
    ) : CurveError

    data class InvalidCombFrequency(
        val frequency: Double,
    ) : CurveError

    data class InvalidInequalitySource(
        val elementType: String,
        val curveType: String?,
    ) : CurveError

    data class TraceParentBoardMismatch(
        val role: String,
    ) : CurveError

    data class TraceParentNotRegistered(
        val role: String,
        val id: String,
    ) : CurveError

    data class UnsupportedTraceSlideObject(
        val elementType: String,
    ) : CurveError

    data class TraceGliderPosition(
        val error: CoordinateTransformationError,
    ) : CurveError

    data class TraceGliderEvaluation(
        val error: GliderError,
    ) : CurveError

    data class DataUpdate(
        val error: CurveDataUpdateError,
    ) : CurveError
}

internal data class CurvePlotOptions(
    val doAdvancedPlot: Boolean = false,
    val plotVersion: Int = 2,
    val recursionDepthHigh: Int = 17,
    val rdpSmoothing: Boolean = false,
    val rdpThreshold: Double = 0.2,
)

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

    data class ImplicitCurve(
        val error: CurveImplicitUpdateError,
    ) : CurveDataUpdateError
}

internal data class CurveDataUpdate(
    val x: DoubleArray,
    val y: DoubleArray,
)

internal fun interface CurveDataUpdater {
    fun update(): GMResult<CurveDataUpdate, CurveDataUpdateError>
}

internal sealed interface CurveImplicitUpdateError {
    data class ExpressionEvaluation(
        val term: String,
        val error: JessieCodeRuntimeError,
    ) : CurveImplicitUpdateError

    data class InvalidValue(
        val term: String,
        val expected: String,
        val actualType: String,
    ) : CurveImplicitUpdateError

    data class InvalidDomain(
        val axis: String,
        val values: DoubleArray,
    ) : CurveImplicitUpdateError

    data class Plot(
        val error: ImplicitPlotError,
    ) : CurveImplicitUpdateError
}

internal data class CurveImplicitDefinition(
    val f: JessieCodeCoordinateFunction,
    val dfx: JessieCodeCoordinateFunction?,
    val dfy: JessieCodeCoordinateFunction?,
    val domainX: JessieCodeCoordinateFunction?,
    val domainY: JessieCodeCoordinateFunction?,
    val margin: JessieCodeCoordinateFunction,
    val resolutionOuter: JessieCodeCoordinateFunction,
    val resolutionInner: JessieCodeCoordinateFunction,
    val maxSteps: JessieCodeCoordinateFunction,
    val alpha0: JessieCodeCoordinateFunction,
    val tolU0: JessieCodeCoordinateFunction,
    val tolNewton: JessieCodeCoordinateFunction,
    val tolCusp: JessieCodeCoordinateFunction,
    val tolProgress: JessieCodeCoordinateFunction,
    val qdtBox: JessieCodeCoordinateFunction,
    val kappa0: JessieCodeCoordinateFunction,
    val delta0: JessieCodeCoordinateFunction,
    val hInitial: JessieCodeCoordinateFunction,
    val hCritical: JessieCodeCoordinateFunction,
    val hMax: JessieCodeCoordinateFunction,
    val loopDist: JessieCodeCoordinateFunction,
    val loopDir: JessieCodeCoordinateFunction,
    val loopDetection: JessieCodeCoordinateFunction,
) {
    internal fun expressions(): List<JessieCodeCoordinateFunction> =
        listOfNotNull(
            f,
            dfx,
            dfy,
            domainX,
            domainY,
            margin,
            resolutionOuter,
            resolutionInner,
            maxSteps,
            alpha0,
            tolU0,
            tolNewton,
            tolCusp,
            tolProgress,
            qdtBox,
            kappa0,
            delta0,
            hInitial,
            hCritical,
            hMax,
            loopDist,
            loopDir,
            loopDetection,
        )
}

internal data class CurveMesh3DDefinition(
    var requestedPointCount: Long = 0L,
)

private data class CurveBooleanDefinition(
    val subject: GeometryElement,
    val clip: GeometryElement,
    val operation: ClipBooleanOperation,
)

internal data class CurveTraceDefinition(
    val glider: Glider,
    val tracePoint: Point,
    val sampleCount: Int,
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

internal data class CurveMetaPostPointControlDefinition(
    val index: Int,
    val type: String?,
    val curlTerm: JessieCodeCoordinateFunction?,
    val directionTerm: JessieCodeCoordinateFunction?,
    val tensionTerm: JessieCodeCoordinateFunction?,
)

internal data class CurveMetaPostControlsDefinition(
    val tensionTerm: JessieCodeCoordinateFunction,
    val isClosedTerm: JessieCodeCoordinateFunction,
    val pointControls: List<CurveMetaPostPointControlDefinition>,
)

private data class CurveMetaPostSplineDefinition(
    val points: List<NumericsPoint2D>,
    val controls: CurveMetaPostControlsDefinition,
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

internal data class CurveIntegralDefinition(
    val source: Curve,
    val curveLeft: Glider,
    val curveLeftDynamic: Boolean,
    val baseLeft: Point,
    val curveRight: Glider,
    val curveRightDynamic: Boolean,
    val baseRight: Point,
    val axis: String,
    val labelDigits: Int,
    var label: Text? = null,
    var labelUpdateError: TextError? = null,
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
    private val xTerm: JessieCodeCoordinateFunction?,
    private val yTerm: JessieCodeCoordinateFunction?,
    private val minimumTerm: JessieCodeCoordinateFunction?,
    private val maximumTerm: JessieCodeCoordinateFunction?,
    dataX: DoubleArray?,
    dataY: DoubleArray?,
    internal val sampleCount: Int,
    private val plotOptions: CurvePlotOptions = CurvePlotOptions(),
    private val identityXTerm: Boolean = false,
    private val booleanDefinition: CurveBooleanDefinition? = null,
    private val traceDefinition: CurveTraceDefinition? = null,
    private val stepDefinition: CurveStepDefinition? = null,
    private val derivativeDefinition: CurveDerivativeDefinition? = null,
    private val splineDefinition: CurveSplineDefinition? = null,
    private val cardinalSplineDefinition: CurveCardinalSplineDefinition? = null,
    private val metaPostSplineDefinition:
        CurveMetaPostSplineDefinition? = null,
    private val riemannDefinition: CurveRiemannDefinition? = null,
    internal val integralDefinition: CurveIntegralDefinition? = null,
    private val boxPlotDefinition: CurveBoxPlotDefinition? = null,
    private val combDefinition: CurveCombDefinition? = null,
    private val inequalityDefinition: CurveInequalityDefinition? = null,
    private val vectorFieldDefinition: CurveVectorFieldDefinition? = null,
    private val ellipseDefinition: CurveEllipseDefinition? = null,
    private val hyperbolaDefinition: CurveHyperbolaDefinition? = null,
    private val parabolaDefinition: CurveParabolaDefinition? = null,
    private val conicDefinition: CurveConicDefinition? = null,
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
    internal var bezierDegree: Int = 1
        private set
    internal var transformMat: Array<DoubleArray> = Mat.identity(3)
        private set
    internal var transformationSource: Curve? = null
        private set
    internal var evaluationError: CurveError? = null
        private set
    internal val isBooleanComposition: Boolean
        get() = booleanDefinition != null
    internal val isTraceCurve: Boolean
        get() = traceDefinition != null
    internal val isStepFunction: Boolean
        get() = stepDefinition != null
    internal val isDerivative: Boolean
        get() = derivativeDefinition != null
    internal val isSpline: Boolean
        get() = splineDefinition != null
    internal val isCardinalSpline: Boolean
        get() = cardinalSplineDefinition != null
    internal val isMetaPostSpline: Boolean
        get() = metaPostSplineDefinition != null
    internal val isImplicitCurve: Boolean
        get() = elType == IMPLICIT_CURVE_ELEMENT_TYPE
    internal val isSketchCurve: Boolean
        get() = elType == SKETCH_CURVE_ELEMENT_TYPE
    internal val isRiemannSum: Boolean
        get() = riemannDefinition != null
    internal val isIntegral: Boolean
        get() = integralDefinition != null
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
    internal val isGenericConic: Boolean
        get() = conicDefinition != null
    internal val isGrid: Boolean
        get() = gridDefinition != null
    internal val isTicks3D: Boolean
        get() = ticks3DDefinition != null
    internal val isMesh3D: Boolean
        get() = mesh3DDefinition != null
    internal val center: Point?
        get() =
            ellipseDefinition?.center
                ?: hyperbolaDefinition?.center
                ?: parabolaDefinition?.center
                ?: conicDefinition?.center
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
    internal var gridDefinition: JsxGraphGrid2D? = null
        private set
    internal var minorGrid: Curve? = null
    internal var majorGrid: Curve? = null
    private var dataUpdater: CurveDataUpdater? = null
    internal var ticks3DDefinition: Ticks3DDefinition? = null
        private set
    internal var mesh3DDefinition: CurveMesh3DDefinition? = null
        private set
    // JSXGraph 1.13.3: src/base/turtle.js -> _attributes / copyAttr.
    // Turtle snapshots the active pen attributes on every generated Curve.
    internal var turtlePenAttributes: TurtlePenAttributes? = null

    internal fun requestedPointCount(): Long? =
        mesh3DDefinition?.requestedPointCount
            ?: vectorFieldDefinition?.requestedPointCount
            ?: inequalityDefinition?.requestedPointCount
            ?: combDefinition?.requestedPointCount
            ?: traceDefinition?.let { definition ->
                definition.sampleCount.toLong() +
                    if (
                        definition.glider.slideObject
                            ?.elementClass != Const.OBJECT_CLASS_CURVE
                    ) {
                        1L
                    } else {
                        0L
                    }
            }
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
        when (val result = updateTransformMatrix()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        transformationSource?.let { source ->
            val count = minOf(source.numberPoints, source.points.size)
            dataX = DoubleArray(count) { index ->
                source.points[index].usrCoords[1]
            }
            dataY = DoubleArray(count) { index ->
                source.points[index].usrCoords[2]
            }
            bezierDegree = source.bezierDegree
        }
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
        traceDefinition?.let { definition ->
            when (val result = updateTraceDefinition(definition)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
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
                    finishCurveUpdate()
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

        val integral = integralDefinition
        if (integral != null) {
            updateIntegralDefinition(integral)
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

        val metaPostSpline = metaPostSplineDefinition
        if (metaPostSpline != null) {
            when (val result = updateMetaPostSplineDefinition(metaPostSpline)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        val xData = dataX
        if (xData != null) {
            replaceDataPoints(xData, dataY ?: DoubleArray(0))
            return finishCurveUpdate()
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

        if (plotOptions.doAdvancedPlot) {
            if (plotOptions.plotVersion !in SUPPORTED_PLOT_VERSIONS) {
                return GMResult.Err(
                    CurveError.UnsupportedPlotVersion(
                        plotOptions.plotVersion,
                    ),
                )
            }
            val xFunction = PlotFunction { parameter, suspendedUpdate ->
                evaluateX(
                    parameter = parameter,
                    arguments = listOf(
                        JessieCodeRuntimeValue.NumberValue(parameter),
                    ),
                    suspendedUpdate = suspendedUpdate,
                )
            }
            val yFunction = PlotFunction { parameter, suspendedUpdate ->
                evaluateY(
                    parameter = parameter,
                    arguments = listOf(
                        JessieCodeRuntimeValue.NumberValue(parameter),
                    ),
                    suspendedUpdate = suspendedUpdate,
                )
            }
            val result = when (plotOptions.plotVersion) {
                1 -> Plot.updateParametricCurveV1(
                    board = board,
                    minimum = minimum,
                    maximum = maximum,
                    maximumPointCount = board.maxCurvePoints,
                    x = xFunction,
                    y = yFunction,
                )
                2 -> Plot.updateParametricCurveV2(
                    board = board,
                    minimum = minimum,
                    maximum = maximum,
                    recursionDepthHigh = plotOptions.recursionDepthHigh,
                    maximumPointCount = board.maxCurvePoints,
                    x = xFunction,
                    y = yFunction,
                )
                3 -> Plot.updateParametricCurveV3(
                    board = board,
                    minimum = minimum,
                    maximum = maximum,
                    identityXTerm = identityXTerm,
                    recursionDepthHigh = plotOptions.recursionDepthHigh,
                    maximumPointCount = board.maxCurvePoints,
                    x = xFunction,
                    y = yFunction,
                )
                4 -> Plot.updateParametricCurveV4(
                    board = board,
                    minimum = minimum,
                    maximum = maximum,
                    identityXTerm = identityXTerm,
                    maximumPointCount = board.maxCurvePoints,
                    x = xFunction,
                    y = yFunction,
                    intervalY =
                        (yTerm as? JessieCodeExpressionFunction)?.let {
                                expression,
                            ->
                        PlotIntervalFunction {
                                intervalMinimum,
                                intervalMaximum,
                                _,
                            ->
                            val interval = PlotInterval(
                                lo = intervalMinimum,
                                hi = intervalMaximum,
                            )
                            when (
                                val intervalResult =
                                    expression.evaluateInterval(
                                        expression.variableNames.associateWith {
                                            interval
                                        },
                                    )
                            ) {
                                is JessieCodeIntervalEvaluation.Value ->
                                    GMResult.Ok(intervalResult.interval)
                                is JessieCodeIntervalEvaluation.Unavailable ->
                                    GMResult.Ok(null)
                            }
                        }
                    },
                )
                else -> return GMResult.Err(
                    CurveError.UnsupportedPlotVersion(
                        plotOptions.plotVersion,
                    ),
                )
            }
            when (result) {
                is GMResult.Ok -> {
                    points.clear()
                    points.addAll(result.value.points)
                    numberPoints = points.size
                }
                is GMResult.Err -> return when (val error = result.error) {
                    is PlotError.Evaluation -> GMResult.Err(error.error)
                    is PlotError.EvaluationException -> GMResult.Err(
                        CurveError.PlotEvaluationException(
                            coordinate = error.coordinate.name,
                            parameter = error.parameter,
                            message = error.message,
                        ),
                    )
                    is PlotError.PointLimitExceeded -> GMResult.Err(
                        CurveError.InvalidAdaptivePointCount(
                            count = error.attemptedCount,
                            maximum = error.maximum,
                        ),
                    )
                    is PlotError.Numerics -> GMResult.Err(
                        CurveError.PlotNumerics(
                            operation = error.operation,
                            error = error.error,
                        ),
                    )
                    is PlotError.Extrapolation -> GMResult.Err(
                        CurveError.PlotExtrapolation(
                            coordinate = error.coordinate.name,
                            parameter = error.parameter,
                            message = error.message,
                        ),
                    )
                    is PlotError.InvalidSpecialInterval -> GMResult.Err(
                        CurveError.PlotInvalidSpecialInterval(
                            type = error.type,
                            start = error.start,
                            end = error.end,
                        ),
                    )
                }
            }
            return finishCurveUpdate()
        }

        // JSXGraph: src/math/plot.js -> updateParametricCurveNaive.
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
            val point = Coords(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(x, y),
                board = board,
            )
            point.curveParameter = parameter
            points += point
        }
        numberPoints = sampleCount
        return finishCurveUpdate()
    }

    private fun finishCurveUpdate(): GMResult<Curve, CurveError> {
        when (val result = applyRdpSmoothing()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        for (index in 0 until minOf(numberPoints, points.size)) {
            updateTransform(points[index])
        }
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/curve.js -> updateCurve RDPsmoothing branch.
    private fun applyRdpSmoothing(): GMResult<Curve, CurveError> {
        if (!plotOptions.rdpSmoothing || bezierDegree != 1) {
            return GMResult.Ok(this)
        }
        val boundingBox = board.getBoundingBox()
        val tolerance = plotOptions.rdpThreshold * sqrt(
            (boundingBox[2] - boundingBox[0]) *
                (boundingBox[1] - boundingBox[3]),
        ) * 0.00125
        return when (
            val result = Numerics.RamerDouglasPeucker(
                points = points,
                tolerance = tolerance,
                useUserCoordinates = true,
            )
        ) {
            is GMResult.Ok -> {
                points.clear()
                points.addAll(result.value)
                numberPoints = points.size
                GMResult.Ok(this)
            }
            is GMResult.Err -> GMResult.Err(
                CurveError.Numerics(
                    operation = "RamerDouglasPeucker",
                    error = result.error,
                ),
            )
        }
    }

    // JSXGraph 1.13.3: src/base/curve.js -> updateTransformMatrix.
    internal fun updateTransformMatrix(): GMResult<Curve, CurveError> {
        var composite = Mat.identity(3)
        for ((index, transformation) in transformations.withIndex()) {
            when (val result = transformation.updateResult()) {
                is GMResult.Ok -> {
                    composite = Mat.matMatMult(
                        transformation.matrix,
                        composite,
                    )
                }
                is GMResult.Err -> return GMResult.Err(
                    CurveError.TransformationEvaluation(
                        index = index,
                        error = result.error,
                    ),
                )
            }
        }
        transformMat = composite
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/curve.js -> updateTransform.
    internal fun updateTransform(point: Coords): Coords {
        if (transformations.isNotEmpty()) {
            point.setCoordinates(
                coordType = Const.COORDS_BY_USER,
                coordinates = Mat.matVecMult(
                    transformMat,
                    point.usrCoords,
                ),
                doRound = false,
            )
        }
        return point
    }

    // JSXGraph 1.13.3: src/base/curve.js -> Ft.
    internal fun Ft(parameter: Double): DoubleArray {
        var coordinates = doubleArrayOf(
            1.0,
            X(parameter),
            Y(parameter),
        )
        if (transformations.isNotEmpty()) {
            coordinates = Mat.matVecMult(transformMat, coordinates)
        }
        coordinates[1] /= coordinates[0]
        coordinates[2] /= coordinates[0]
        coordinates[0] /= coordinates[0]
        return coordinates
    }

    // JSXGraph 1.13.3: src/base/curve.js -> addTransform.
    internal fun addTransform(
        transformation: Transformation,
    ): Curve = addTransform(listOf(transformation))

    internal fun addTransform(
        values: Iterable<Transformation>,
    ): Curve {
        transformations.addAll(values)
        return this
    }

    // JSXGraph 1.13.3: src/base/curve.js -> removeTransform.
    internal fun removeTransform(
        transformation: Transformation,
    ): Curve = removeTransform(listOf(transformation))

    internal fun removeTransform(
        values: Iterable<Transformation>,
    ): Curve {
        for (transformation in values) {
            val index = transformations.indexOf(transformation)
            if (index >= 0) {
                transformations.removeAt(index)
            }
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/curve.js -> clearTransforms.
    internal fun clearTransforms(): Curve {
        transformations.clear()
        return this
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
    // src/base/curve.js -> createMetapostSpline.updateDataArray;
    // src/math/metapost.js -> curve.
    private fun updateMetaPostSplineDefinition(
        definition: CurveMetaPostSplineDefinition,
    ): GMResult<Unit, CurveError> {
        val pointList = definition.points.map { point ->
            MetaPostPoint(point.X(), point.Y())
        }
        val tension = when (
            val result = evaluateCoordinateNumber(
                termName = "metapostspline.controls.tension",
                term = definition.controls.tensionTerm,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val isClosed = when (
            val result = evaluateCoordinateBoolean(
                termName = "metapostspline.controls.isClosed",
                term = definition.controls.isClosedTerm,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointControls = mutableListOf<MetaPostPointControl>()
        for (control in definition.controls.pointControls) {
            when (val result = evaluateMetaPostPointControl(control)) {
                is GMResult.Ok -> pointControls += result.value
                is GMResult.Err -> return result
            }
        }
        val result = MetaPost.curve(
            pointList = pointList,
            controls = MetaPostControls(
                tension = tension,
                isClosed = isClosed,
                pointControls = pointControls,
            ),
        )
        dataX = result.x
        dataY = result.y
        return GMResult.Ok(Unit)
    }

    private fun evaluateMetaPostPointControl(
        definition: CurveMetaPostPointControlDefinition,
    ): GMResult<MetaPostPointControl, CurveError> {
        val prefix = "metapostspline.controls.${definition.index}"
        val curl = if (definition.type == "curl") {
            definition.curlTerm?.let { term ->
                when (
                    val result = evaluateCoordinateNumber(
                        termName = "$prefix.curl",
                        term = term,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            } ?: 0.0
        } else {
            null
        }
        val direction = definition.directionTerm?.let { term ->
            when (
                val result = evaluateMetaPostControlPair(
                    termName = "$prefix.direction",
                    term = term,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        }
        val tension = definition.tensionTerm?.let { term ->
            when (
                val result = evaluateMetaPostControlPair(
                    termName = "$prefix.tension",
                    term = term,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(
            MetaPostPointControl(
                index = definition.index,
                type = definition.type,
                curl = curl,
                direction = direction,
                tension = tension,
            ),
        )
    }

    private fun evaluateMetaPostControlPair(
        termName: String,
        term: JessieCodeCoordinateFunction,
    ): GMResult<MetaPostControlPair, CurveError> =
        when (val result = term.evaluate()) {
            is GMResult.Err -> GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = termName,
                    error = result.error,
                ),
            )
            is GMResult.Ok -> when (val value = result.value) {
                is JessieCodeRuntimeValue.NumberValue -> GMResult.Ok(
                    MetaPostControlPair(
                        left = value.value,
                        right = value.value,
                    ),
                )
                is JessieCodeRuntimeValue.ArrayValue -> {
                    if (value.values.size < 2) {
                        return GMResult.Err(
                            CurveError.InvalidMetaPostControl(
                                control = termName,
                                expected =
                                    "a number or a two-entry array",
                                actualType = "array",
                            ),
                        )
                    }
                    val left = when (
                        val side = metaPostControlSide(
                            termName = "$termName[0]",
                            value = value.values[0],
                        )
                    ) {
                        is GMResult.Ok -> side.value
                        is GMResult.Err -> return side
                    }
                    val right = when (
                        val side = metaPostControlSide(
                            termName = "$termName[1]",
                            value = value.values[1],
                        )
                    ) {
                        is GMResult.Ok -> side.value
                        is GMResult.Err -> return side
                    }
                    GMResult.Ok(
                        MetaPostControlPair(
                            left = left,
                            right = right,
                        ),
                    )
                }
                else -> GMResult.Err(
                    CurveError.InvalidMetaPostControl(
                        control = termName,
                        expected = "a number or a two-entry array",
                        actualType = curveRuntimeType(value),
                    ),
                )
            }
        }

    private fun metaPostControlSide(
        termName: String,
        value: JessieCodeRuntimeValue,
    ): GMResult<Double?, CurveError> =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                GMResult.Ok(value.value)
            is JessieCodeRuntimeValue.BooleanValue
                if (!value.value) -> GMResult.Ok(null)
            else -> GMResult.Err(
                CurveError.InvalidMetaPostControl(
                    control = termName,
                    expected = "a number or false",
                    actualType = curveRuntimeType(value),
                ),
            )
        }

    // JSXGraph 1.13.3: src/base/curve.js ->
    // createTracecurve.updateDataArray.
    private fun updateTraceDefinition(
        definition: CurveTraceDefinition,
    ): GMResult<Unit, CurveError> {
        val glider = definition.glider
        val tracePoint = definition.tracePoint
        val slideObject = glider.slideObject ?: glider.slideElement
        val minimum: Double
        val maximum: Double
        val closed: Boolean
        val coordinatesAt: (Double) -> DoubleArray
        when (slideObject) {
            is Circle -> {
                minimum = slideObject.minX()
                maximum = slideObject.maxX()
                closed = true
                coordinatesAt = { parameter ->
                    val z = slideObject.Z(parameter)
                    doubleArrayOf(
                        slideObject.X(parameter) / z,
                        slideObject.Y(parameter) / z,
                    )
                }
            }
            is Line -> {
                minimum = slideObject.minX()
                maximum = slideObject.maxX()
                closed = true
                coordinatesAt = { parameter ->
                    val z = slideObject.Z(parameter)
                    doubleArrayOf(
                        slideObject.X(parameter) / z,
                        slideObject.Y(parameter) / z,
                    )
                }
            }
            is Curve -> {
                minimum = slideObject.minX()
                maximum = slideObject.maxX()
                closed = false
                coordinatesAt = { parameter ->
                    doubleArrayOf(
                        slideObject.X(parameter),
                        slideObject.Y(parameter),
                    )
                }
            }
            else -> return GMResult.Err(
                CurveError.UnsupportedTraceSlideObject(
                    elementType = slideObject.elType.ifEmpty { "element" },
                ),
            )
        }

        val pointCount =
            definition.sampleCount + if (closed) 1 else 0
        val step = (maximum - minimum) / definition.sampleCount
        val tracedX = DoubleArray(pointCount)
        val tracedY = DoubleArray(pointCount)
        val savedPosition = glider.position
        var failure: CurveError? = null

        try {
            for (index in 0 until pointCount) {
                val parameter = minimum + index * step
                when (
                    val result = glider.setPositionDirectlyResult(
                        method = Const.COORDS_BY_USER,
                        coordinates = coordinatesAt(parameter),
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> {
                        failure = CurveError.TraceGliderPosition(
                            result.error,
                        )
                        break
                    }
                }
                glider.evaluationError?.let { error ->
                    failure = CurveError.TraceGliderEvaluation(error)
                }
                if (failure != null) {
                    break
                }
                replayTraceElements(glider, tracePoint)
                tracedX[index] = tracePoint.X()
                tracedY[index] = tracePoint.Y()
            }
        } finally {
            glider.position = savedPosition
            replayTraceElements(glider, tracePoint)
        }

        failure?.let { return GMResult.Err(it) }
        dataX = tracedX
        dataY = tracedY
        return GMResult.Ok(Unit)
    }

    private fun replayTraceElements(
        glider: Glider,
        tracePoint: Point,
    ) {
        var fromGlider = false
        for (element in board.objectsList) {
            if (element === glider) {
                fromGlider = true
            }
            if (
                !fromGlider ||
                element === this ||
                !element.needsRegularUpdate
            ) {
                continue
            }
            element.needsUpdate = true
            element.update(fromParent = true)
            if (element === tracePoint) {
                break
            }
        }
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

    // JSXGraph 1.13.3:
    // src/element/composition.js -> createIntegral.updateDataArray.
    private fun updateIntegralDefinition(
        definition: CurveIntegralDefinition,
    ) {
        val source = definition.source
        val xCoordinates = mutableListOf<Double>()
        val yCoordinates = mutableListOf<Double>()
        if (definition.axis == "y") {
            val lower: Glider
            val upper: Glider
            if (definition.curveLeft.Y() < definition.curveRight.Y()) {
                lower = definition.curveLeft
                upper = definition.curveRight
            } else {
                lower = definition.curveRight
                upper = definition.curveLeft
            }
            val left = minOf(lower.X(), upper.X())
            val right = maxOf(lower.X(), upper.X())
            xCoordinates += 0.0
            yCoordinates += lower.Y()
            xCoordinates += lower.X()
            yCoordinates += lower.Y()
            for (point in source.points) {
                val coordinates = point.usrCoords
                if (
                    lower.Y() <= coordinates[2] &&
                    left <= coordinates[1] &&
                    coordinates[2] <= upper.Y() &&
                    coordinates[1] <= right
                ) {
                    xCoordinates += coordinates[1]
                    yCoordinates += coordinates[2]
                }
            }
            xCoordinates += upper.X()
            yCoordinates += upper.Y()
            xCoordinates += 0.0
            yCoordinates += upper.Y()
            xCoordinates += 0.0
            yCoordinates += lower.Y()
        } else {
            val left = minOf(
                definition.baseLeft.X(),
                definition.baseRight.X(),
            )
            val right = maxOf(
                definition.baseLeft.X(),
                definition.baseRight.X(),
            )
            xCoordinates += left
            yCoordinates += 0.0
            xCoordinates += left
            yCoordinates += source.Y(left)
            for (point in source.points) {
                val coordinates = point.usrCoords
                if (left <= coordinates[1] && coordinates[1] <= right) {
                    xCoordinates += coordinates[1]
                    yCoordinates += coordinates[2]
                }
            }
            xCoordinates += right
            yCoordinates += source.Y(right)
            xCoordinates += right
            yCoordinates += 0.0
            xCoordinates += left
            yCoordinates += 0.0
        }
        dataX = xCoordinates.toDoubleArray()
        dataY = yCoordinates.toDoubleArray()
        updateIntegralLabel(definition)
    }

    private fun updateIntegralLabel(
        definition: CurveIntegralDefinition,
    ) {
        val label = definition.label ?: return
        val boundingBox = board.getBoundingBox()
        val dx = (boundingBox[2] - boundingBox[0]) * 0.1
        val dy = (boundingBox[1] - boundingBox[3]) * 0.1
        val curveX = definition.curveRight.X()
        val x = when {
            curveX < boundingBox[0] -> boundingBox[0] + dx
            curveX > boundingBox[2] -> boundingBox[2] - dx
            else -> curveX
        }
        val curveY = definition.curveRight.Y()
        val y = when {
            curveY > boundingBox[1] -> boundingBox[1] - dy
            curveY < boundingBox[3] -> boundingBox[3] + dy
            else -> curveY
        }
        label.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(x, y),
        )
        when (
            val result = label.setText(
                "\u222b = " +
                    com.swithun.jsxgraph.core.utils.JsNumberFormat.fixed(
                        integralValue(definition),
                        definition.labelDigits,
                    ),
            )
        ) {
            is GMResult.Ok -> definition.labelUpdateError = null
            is GMResult.Err -> definition.labelUpdateError = result.error
        }
    }

    private fun integralValue(
        definition: CurveIntegralDefinition,
    ): Double =
        when (
            val result = Numerics.I(
                interval = doubleArrayOf(
                    definition.baseLeft.X(),
                    definition.baseRight.X(),
                ),
                function = definition.source::Y,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> Double.NaN
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

    // JSXGraph 1.13.3: src/base/turtle.js -> forward / moveTo,
    // curve.dataX.push / curve.dataY.push.
    internal fun appendDataPoint(
        x: Double,
        y: Double,
    ): Curve {
        val currentX = dataX ?: DoubleArray(0)
        val currentY = dataY ?: DoubleArray(0)
        replaceData(
            xData = currentX + x,
            yData = currentY + y,
        )
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

    // JSXGraph 1.13.3: src/element/grid.js -> createGrid.
    internal fun configureGrid(
        definition: JsxGraphGrid2D,
        bezierDegree: Int,
    ): Curve {
        gridDefinition = definition
        this.bezierDegree = bezierDegree
        type = Const.OBJECT_TYPE_GRID
        elType = "grid"
        isDraggable = false
        return this
    }

    internal fun minX(): Double = evaluateMinimum().valueOrNaN()

    internal fun maxX(): Double = evaluateMaximum().valueOrNaN()

    internal fun Value(): Double =
        integralDefinition?.let(::integralValue)
            ?: riemannDefinition?.sum
            ?: Double.NaN

    internal fun boxPlotSnapshot(): CurveBoxPlotSnapshot? =
        boxPlotDefinition?.snapshot

    internal fun vectorFieldSnapshot(): CurveVectorFieldSnapshot? =
        vectorFieldDefinition?.snapshot

    internal fun initializeConic(): GMResult<Curve, CurveError> =
        when (val result = updateCurve()) {
            is GMResult.Ok -> {
                needsUpdate = false
                result
            }
            is GMResult.Err -> result
        }

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
        conicDefinition?.let { definition ->
            return GMResult.Ok(definition.minimum)
        }
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
        conicDefinition?.let { definition ->
            return GMResult.Ok(definition.maximum)
        }
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
        conicDefinition?.let { definition ->
            return when (
                val result = evaluateConic(
                    definition = definition,
                    parameter = parameter,
                    suspendedUpdate = suspendedUpdate,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(result.value[1])
                is GMResult.Err -> result
            }
        }
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
        conicDefinition?.let { definition ->
            return when (
                val result = evaluateConic(
                    definition = definition,
                    parameter = parameter,
                    suspendedUpdate = suspendedUpdate,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(result.value[2])
                is GMResult.Err -> result
            }
        }
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
                is GMResult.Err
                    if (
                        result.error is
                            NumericsError.SplineValueOutOfDomain
                    ) ->
                    // JSXGraph 1.13.3: Numerics.splineEval returns NaN
                    // outside the knot domain. Plot v4 intentionally probes
                    // beyond both borders while finding components.
                    GMResult.Ok(Double.NaN)
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

    // JSXGraph 1.13.3: src/element/conic.js -> createConic polarForm
    private fun evaluateConic(
        definition: CurveConicDefinition,
        parameter: Double,
        suspendedUpdate: Boolean,
    ): GMResult<DoubleArray, CurveError> {
        if (!suspendedUpdate) {
            when (val result = updateConicPolarForm(definition)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        val eigenvalues = definition.eigenvalues
            ?: return GMResult.Ok(
                doubleArrayOf(1.0, Double.NaN, Double.NaN),
            )
        val vector = when {
            eigenvalues[1][1] <= 0.0 &&
                eigenvalues[2][2] <= 0.0 ->
                Mat.matVecMult(
                    definition.rotationMatrix,
                    doubleArrayOf(
                        1.0 / definition.c,
                        cos(parameter) / definition.a,
                        sin(parameter) / definition.b,
                    ),
                )
            eigenvalues[1][1] <= 0.0 &&
                eigenvalues[2][2] > 0.0 ->
                Mat.matVecMult(
                    definition.rotationMatrix,
                    doubleArrayOf(
                        cos(parameter) / definition.c,
                        1.0 / definition.a,
                        sin(parameter) / definition.b,
                    ),
                )
            eigenvalues[2][2] < 0.0 ->
                Mat.matVecMult(
                    definition.rotationMatrix,
                    doubleArrayOf(
                        sin(parameter) / definition.c,
                        cos(parameter) / definition.a,
                        1.0 / definition.b,
                    ),
                )
            else -> doubleArrayOf(1.0, Double.NaN, Double.NaN)
        }
        vector[1] /= vector[0]
        vector[2] /= vector[0]
        vector[0] = 1.0
        return GMResult.Ok(vector)
    }

    // JSXGraph 1.13.3: src/element/conic.js ->
    // createConic sym / degconic / fitConic / polarForm
    private fun updateConicPolarForm(
        definition: CurveConicDefinition,
    ): GMResult<Unit, CurveError> {
        val matrix = when (val source = definition.source) {
            is CurveConicSource.Points -> {
                val pointCoordinates = source.points.map {
                    it.coords.usrCoords
                }
                val firstDegenerate = conicDegenerateForm(
                    Mat.crossProduct(
                        pointCoordinates[0],
                        pointCoordinates[1],
                    ),
                    Mat.crossProduct(
                        pointCoordinates[2],
                        pointCoordinates[3],
                    ),
                )
                val secondDegenerate = conicDegenerateForm(
                    Mat.crossProduct(
                        pointCoordinates[0],
                        pointCoordinates[2],
                    ),
                    Mat.crossProduct(
                        pointCoordinates[1],
                        pointCoordinates[3],
                    ),
                )
                fitConic(
                    first = firstDegenerate,
                    second = secondDegenerate,
                    point = pointCoordinates[4],
                )
            }
            is CurveConicSource.Coefficients -> {
                val coefficients = DoubleArray(source.terms.size)
                for ((index, term) in source.terms.withIndex()) {
                    coefficients[index] = when (
                        val result = evaluateCoordinateNumber(
                            termName = "conic.coefficient[$index]",
                            term = term,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                }
                arrayOf(
                    doubleArrayOf(
                        coefficients[2],
                        coefficients[4],
                        coefficients[5],
                    ),
                    doubleArrayOf(
                        coefficients[4],
                        coefficients[0],
                        coefficients[3],
                    ),
                    doubleArrayOf(
                        coefficients[5],
                        coefficients[3],
                        coefficients[1],
                    ),
                )
            }
        }
        quadraticform = matrix

        val eigen = Numerics.Jacobi(matrix)
        val eigenvalues = eigen.diagonalizedMatrix
        if (eigenvalues[0][0] < 0.0) {
            eigenvalues[0][0] *= -1.0
            eigenvalues[1][1] *= -1.0
            eigenvalues[2][2] *= -1.0
        }
        definition.eigenvalues = eigenvalues
        definition.rotationMatrix = eigen.eigenvectors
        definition.c = sqrt(abs(eigenvalues[0][0]))
        definition.a = sqrt(abs(eigenvalues[1][1]))
        definition.b = sqrt(abs(eigenvalues[2][2]))
        return GMResult.Ok(Unit)
    }

    private fun conicDegenerateForm(
        first: DoubleArray,
        second: DoubleArray,
    ): Array<DoubleArray> {
        val matrix = Array(3) { row ->
            DoubleArray(3) { column ->
                first[row] * second[column]
            }
        }
        for (row in 0 until 3) {
            for (column in row until 3) {
                matrix[row][column] += matrix[column][row]
            }
        }
        for (row in 0 until 3) {
            for (column in 0 until row) {
                matrix[row][column] = matrix[column][row]
            }
        }
        return matrix
    }

    private fun fitConic(
        first: Array<DoubleArray>,
        second: Array<DoubleArray>,
        point: DoubleArray,
    ): Array<DoubleArray> {
        val pointSecondPoint = Mat.innerProduct(
            point,
            Mat.matVecMult(second, point),
        )
        val pointFirstPoint = Mat.innerProduct(
            point,
            Mat.matVecMult(first, point),
        )
        return Array(3) { row ->
            DoubleArray(3) { column ->
                pointSecondPoint * first[row][column] -
                    pointFirstPoint * second[row][column]
            }
        }
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
        if (bezierDegree == 3) {
            val last = (values.size - 1) / 3.0
            if (parameter >= last) {
                return values[values.lastIndex]
            }
            val index = floor(parameter).toInt() * 3
            val localParameter = parameter % 1.0
            val inverseParameter = 1.0 - localParameter
            val first = values.getOrNull(index) ?: return Double.NaN
            val firstControl =
                values.getOrNull(index + 1) ?: return Double.NaN
            val secondControl =
                values.getOrNull(index + 2) ?: return Double.NaN
            val second =
                values.getOrNull(index + 3) ?: return Double.NaN
            return (
                inverseParameter * inverseParameter *
                    (
                        inverseParameter * first +
                            3.0 * localParameter * firstControl
                        ) +
                    (
                        3.0 * inverseParameter * secondControl +
                            localParameter * second
                        ) *
                    localParameter * localParameter
                )
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
        expression: JessieCodeCoordinateFunction?,
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
        internal const val DEFAULT_PLOT_VERSION: Int = 2
        private val SUPPORTED_PLOT_VERSIONS: Set<Int> = setOf(1, 2, 3, 4)
        internal const val DEFAULT_RECURSION_DEPTH_HIGH: Int = 17
        internal const val MAX_RECURSION_DEPTH: Int = 30
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
        private const val SKETCH_CURVE_ELEMENT_TYPE = "sketchcurve"
        private const val IMPLICIT_CURVE_ELEMENT_TYPE = "implicitcurve"
        internal const val TRACE_DEFAULT_SAMPLE_COUNT: Int = 100
        private const val SPLINE_ELEMENT_TYPE = "spline"
        private const val CARDINAL_SPLINE_ELEMENT_TYPE = "cardinalspline"
        private const val METAPOST_SPLINE_ELEMENT_TYPE = "metapostspline"
        private const val RIEMANN_DEFAULT_FALLBACK_TYPE =
            "__riemann_default__"
        private const val DATA_CURVE_TYPE = "plot"
        private const val INTEGRAL_ELEMENT_TYPE = "integral"
        private const val PARAMETRIC_CURVE_TYPE = "parameter"
        private const val FUNCTION_GRAPH_CURVE_TYPE = "functiongraph"
        private const val COMB_POINTS_PER_TOOTH = 3
        private const val INEQUALITY_LINE_POINT_COUNT = 5
        private const val INEQUALITY_SEGMENT_EXTRA_POINT_COUNT = 5L
        private const val VECTOR_FIELD_BODY_POINT_COUNT = 3L
        private const val VECTOR_FIELD_ARROW_POINT_COUNT = 4L

        // JSXGraph 1.13.3: src/element/conic.js -> createConic.
        internal fun createConicShell(
            board: Board,
            definition: CurveConicDefinition,
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            plotOptions: CurvePlotOptions = CurvePlotOptions(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            when (
                val result = validateContinuousPlotting(
                    sampleCount = sampleCount,
                    plotOptions = plotOptions,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            val curve = Curve(
                board = board,
                curveType = PARAMETRIC_CURVE_TYPE,
                xTerm = null,
                yTerm = null,
                minimumTerm = null,
                maximumTerm = null,
                dataX = null,
                dataY = null,
                sampleCount = sampleCount,
                plotOptions = plotOptions,
                conicDefinition = definition,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val registration = board.setId(
                    curve,
                    CURVE_ID_PREFIX,
                )
            ) {
                is GMResult.Ok -> {
                    val coefficientTerms = (
                        definition.source as?
                            CurveConicSource.Coefficients
                        )?.terms.orEmpty()
                    curve.addParentsFromJCFunctions(coefficientTerms)
                    curve.type = Const.OBJECT_TYPE_CONIC
                    GMResult.Ok(curve)
                }
                is GMResult.Err -> GMResult.Err(
                    CurveError.Registration(registration.error),
                )
            }
        }

        // JSXGraph 1.13.3: src/element/conic.js -> createEllipse.
        internal fun createEllipse(
            board: Board,
            definition: CurveEllipseDefinition,
            parentlessPoints: Set<Point>,
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            plotOptions: CurvePlotOptions = CurvePlotOptions(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            when (
                val result = validateContinuousPlotting(
                    sampleCount = sampleCount,
                    plotOptions = plotOptions,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
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
                        plotOptions = plotOptions,
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
            plotOptions: CurvePlotOptions = CurvePlotOptions(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            when (
                val result = validateContinuousPlotting(
                    sampleCount = sampleCount,
                    plotOptions = plotOptions,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
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
                        plotOptions = plotOptions,
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
            plotOptions: CurvePlotOptions = CurvePlotOptions(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            when (
                val result = validateContinuousPlotting(
                    sampleCount = sampleCount,
                    plotOptions = plotOptions,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
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
                        plotOptions = plotOptions,
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

        // JSXGraph 1.13.3: src/base/curve.js -> createSketchCurve.
        internal fun createSketchCurve(
            board: Board,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> =
            when (
                val result = createData(
                    board = board,
                    dataX = doubleArrayOf(),
                    dataY = doubleArrayOf(),
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> {
                    result.value.elType = SKETCH_CURVE_ELEMENT_TYPE
                    result
                }
                is GMResult.Err -> result
            }

        // JSXGraph 1.13.3: src/base/curve.js -> createImplicitCurve.
        internal fun createImplicitCurve(
            board: Board,
            definition: CurveImplicitDefinition,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            val curve = Curve(
                board = board,
                curveType = DATA_CURVE_TYPE,
                xTerm = null,
                yTerm = null,
                minimumTerm = null,
                maximumTerm = null,
                dataX = doubleArrayOf(),
                dataY = doubleArrayOf(),
                sampleCount = 0,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            curve.setDataUpdater(
                CurveDataUpdater {
                    updateImplicitCurveData(
                        board = board,
                        definition = definition,
                    )
                },
            )
            when (val result = curve.updateCurve()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            return when (
                val registration = board.setId(curve, CURVE_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    curve.elType = IMPLICIT_CURVE_ELEMENT_TYPE
                    curve.addParentsFromJCFunctions(
                        definition.expressions(),
                    )
                    curve.needsUpdate = false
                    GMResult.Ok(curve)
                }
                is GMResult.Err -> GMResult.Err(
                    CurveError.Registration(registration.error),
                )
            }
        }

        // JSXGraph 1.13.3: src/base/curve.js -> createTracecurve.
        internal fun createTraceCurve(
            board: Board,
            glider: Glider,
            tracePoint: Point,
            sampleCount: Int = TRACE_DEFAULT_SAMPLE_COUNT,
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
            validateTraceParent(board, glider, "glider")?.let {
                return GMResult.Err(it)
            }
            validateTraceParent(board, tracePoint, "tracepoint")?.let {
                return GMResult.Err(it)
            }
            val slideObject = glider.slideObject ?: glider.slideElement
            if (
                slideObject !is Circle &&
                slideObject !is Line &&
                slideObject !is Curve
            ) {
                return GMResult.Err(
                    CurveError.UnsupportedTraceSlideObject(
                        elementType =
                            slideObject.elType.ifEmpty { "element" },
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
                    sampleCount = sampleCount,
                    traceDefinition = CurveTraceDefinition(
                        glider = glider,
                        tracePoint = tracePoint,
                        sampleCount = sampleCount,
                    ),
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                expressions = emptyList(),
            )
        }

        // JSXGraph 1.13.3: src/base/curve.js -> createCurve transformed
        // curve branch / updateDataArray.
        internal fun createTransformed(
            board: Board,
            source: Curve,
            transformations: List<Transformation>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            if (transformations.isEmpty()) {
                return GMResult.Err(CurveError.EmptyTransformationList)
            }
            if (source.board !== board) {
                return GMResult.Err(
                    CurveError.TransformationSourceBoardMismatch,
                )
            }
            if (
                source.id.isEmpty() ||
                board.elementById(source.id) !== source
            ) {
                return GMResult.Err(
                    CurveError.TransformationSourceNotRegistered(source.id),
                )
            }

            val curve = Curve(
                board = board,
                curveType = DATA_CURVE_TYPE,
                xTerm = null,
                yTerm = null,
                minimumTerm = null,
                maximumTerm = null,
                dataX = doubleArrayOf(),
                dataY = doubleArrayOf(),
                sampleCount = source.numberPoints,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            curve.transformationSource = source
            curve.addTransform(transformations)

            return when (
                val result = register(
                    curve = curve,
                    expressions = emptyList(),
                )
            ) {
                is GMResult.Ok -> {
                    source.addChild(curve)
                    curve.setParents(listOf(source))
                    result
                }
                is GMResult.Err -> result
            }
        }

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
            plotOptions: CurvePlotOptions = CurvePlotOptions(),
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
            when (
                val result = validateContinuousPlotting(
                    sampleCount = sampleCount,
                    plotOptions = plotOptions,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
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
                        plotOptions = plotOptions,
                        identityXTerm = true,
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
            plotOptions: CurvePlotOptions = CurvePlotOptions(),
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
            when (
                val result = validateContinuousPlotting(
                    sampleCount = sampleCount,
                    plotOptions = plotOptions,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
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
                        plotOptions = plotOptions,
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

        // JSXGraph 1.13.3:
        // src/base/curve.js -> createMetapostSpline.
        internal fun createMetaPostSpline(
            board: Board,
            points: List<NumericsPoint2D>,
            controls: CurveMetaPostControlsDefinition,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            if (points.size < 2) {
                return GMResult.Err(
                    CurveError.InvalidInterpolationPointCount(
                        creator = METAPOST_SPLINE_ELEMENT_TYPE,
                        count = points.size,
                        minimum = 2,
                    ),
                )
            }
            val curve = Curve(
                board = board,
                curveType = DATA_CURVE_TYPE,
                xTerm = null,
                yTerm = null,
                minimumTerm = null,
                maximumTerm = null,
                dataX = null,
                dataY = null,
                sampleCount = 0,
                metaPostSplineDefinition =
                    CurveMetaPostSplineDefinition(
                        points = points,
                        controls = controls,
                    ),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            curve.bezierDegree = 3
            return when (
                val result = register(
                    curve = curve,
                    expressions = listOfNotNull(
                        controls.tensionTerm,
                        controls.isClosedTerm,
                    ) + controls.pointControls.flatMap { control ->
                        listOfNotNull(
                            control.curlTerm,
                            control.directionTerm,
                            control.tensionTerm,
                        )
                    },
                )
            ) {
                is GMResult.Ok -> {
                    val registered = result.value
                    registered.elType = METAPOST_SPLINE_ELEMENT_TYPE
                    val pointElements = points.filterIsInstance<Point>()
                    registered.setParents(pointElements)
                    for (point in pointElements) {
                        point.addChild(registered)
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

        // JSXGraph 1.13.3:
        // src/element/composition.js -> createIntegral.
        internal fun createIntegral(
            board: Board,
            definition: CurveIntegralDefinition,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> =
            when (
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
                        integralDefinition = definition,
                        id = id,
                        name = name,
                        needsRegularUpdate = needsRegularUpdate,
                    ),
                    expressions = emptyList(),
                )
            ) {
                is GMResult.Ok -> {
                    result.value.elType = INTEGRAL_ELEMENT_TYPE
                    result.value.isDraggable = false
                    result
                }
                is GMResult.Err -> result
            }

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
            plotOptions: CurvePlotOptions = CurvePlotOptions(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            parameterName: String = "x",
        ): GMResult<Curve, CurveError> =
            createContinuous(
                board = board,
                curveType = PARAMETRIC_CURVE_TYPE,
                xSource = xSource,
                ySource = ySource,
                minimumSource = minimumSource,
                maximumSource = maximumSource,
                parameterName = parameterName,
                sampleCount = sampleCount,
                plotOptions = plotOptions,
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
            plotOptions: CurvePlotOptions = CurvePlotOptions(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            parameterName: String = "x",
        ): GMResult<Curve, CurveError> =
            createContinuous(
                board = board,
                curveType = FUNCTION_GRAPH_CURVE_TYPE,
                xSource = xSource,
                ySource = ySource,
                minimumSource = minimumSource,
                maximumSource = maximumSource,
                parameterName = parameterName,
                sampleCount = sampleCount,
                plotOptions = plotOptions,
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
            plotOptions: CurvePlotOptions = CurvePlotOptions(),
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
                parameterName = "x",
                sampleCount = sampleCount,
                plotOptions = plotOptions,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        // JSXGraph 1.13.3: src/base/curve.js -> createFunctiongraph.
        // Native composite factories pass an already compiled function just
        // as the JavaScript factory accepts a Function parent.
        internal fun createFunctionGraph(
            board: Board,
            yTerm: JessieCodeCoordinateFunction,
            minimumTerm: JessieCodeCoordinateFunction,
            maximumTerm: JessieCodeCoordinateFunction,
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            plotOptions: CurvePlotOptions = CurvePlotOptions(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            when (
                val result = validateContinuousPlotting(
                    sampleCount = sampleCount,
                    plotOptions = plotOptions,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            val xTerm = object : JessieCodeCoordinateFunction {
                override val origin: String? = null
                override val dependencies: Map<String, GeometryElement> =
                    emptyMap()

                override fun evaluate(
                    arguments: List<JessieCodeRuntimeValue>,
                ): GMResult<
                    JessieCodeRuntimeValue,
                    JessieCodeRuntimeError,
                    > = GMResult.Ok(
                    arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.NumberValue(Double.NaN),
                )
            }
            return register(
                curve = Curve(
                    board = board,
                    curveType = FUNCTION_GRAPH_CURVE_TYPE,
                    xTerm = xTerm,
                    yTerm = yTerm,
                    minimumTerm = minimumTerm,
                    maximumTerm = maximumTerm,
                    dataX = null,
                    dataY = null,
                    sampleCount = sampleCount,
                    plotOptions = plotOptions,
                    identityXTerm = true,
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

        // JSXGraph 1.13.3: src/base/curve.js -> createDerivative.
        internal fun createDerivative(
            board: Board,
            source: Curve,
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            plotOptions: CurvePlotOptions = CurvePlotOptions(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve, CurveError> {
            when (
                val result = validateContinuousPlotting(
                    sampleCount = sampleCount,
                    plotOptions = plotOptions,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
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
                        plotOptions = plotOptions,
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
            parameterName: String,
            sampleCount: Int,
            plotOptions: CurvePlotOptions,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
        ): GMResult<Curve, CurveError> {
            when (
                val result = validateContinuousPlotting(
                    sampleCount = sampleCount,
                    plotOptions = plotOptions,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            val xTerm = when (
                val result = compile(
                    term = "xterm",
                    source = xSource,
                    board = board,
                    variableNames = listOf(parameterName),
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
                    variableNames = listOf(parameterName),
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
                    plotOptions = plotOptions,
                    identityXTerm = xSource == parameterName,
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

        // JSXGraph 1.13.3:
        // src/base/curve.js -> createImplicitCurve.updateDataArray.
        private fun updateImplicitCurveData(
            board: Board,
            definition: CurveImplicitDefinition,
        ): GMResult<CurveDataUpdate, CurveDataUpdateError> {
            val boundingBox = if (
                definition.domainX == null &&
                definition.domainY == null
            ) {
                val margin = when (
                    val result = evaluateImplicitNumber(
                        term = "implicitcurve.margin",
                        function = definition.margin,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return implicitUpdateFailure(
                        result.error,
                    )
                }
                board.getBoundingBox().also { bounds ->
                    bounds[0] -= margin
                    bounds[1] += margin
                    bounds[2] += margin
                    bounds[3] -= margin
                }
            } else {
                val rangeX = when (
                    val result = evaluateImplicitRange(
                        axis = "x",
                        function = definition.domainX,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return implicitUpdateFailure(
                        result.error,
                    )
                }
                val rangeY = when (
                    val result = evaluateImplicitRange(
                        axis = "y",
                        function = definition.domainY,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return implicitUpdateFailure(
                        result.error,
                    )
                }
                doubleArrayOf(
                    minOf(rangeX[0], rangeX[1]),
                    maxOf(rangeY[0], rangeY[1]),
                    maxOf(rangeX[0], rangeX[1]),
                    minOf(rangeY[0], rangeY[1]),
                )
            }

            val numericTerms = listOf(
                "resolutionOuter" to definition.resolutionOuter,
                "resolutionInner" to definition.resolutionInner,
                "maxSteps" to definition.maxSteps,
                "alpha0" to definition.alpha0,
                "tolU0" to definition.tolU0,
                "tolNewton" to definition.tolNewton,
                "tolCusp" to definition.tolCusp,
                "tolProgress" to definition.tolProgress,
                "qdtBox" to definition.qdtBox,
                "kappa0" to definition.kappa0,
                "delta0" to definition.delta0,
                "hInitial" to definition.hInitial,
                "hCritical" to definition.hCritical,
                "hMax" to definition.hMax,
                "loopDist" to definition.loopDist,
                "loopDir" to definition.loopDir,
            )
            val values = linkedMapOf<String, Double>()
            for ((name, function) in numericTerms) {
                when (
                    val result = evaluateImplicitNumber(
                        term = "implicitcurve.$name",
                        function = function,
                    )
                ) {
                    is GMResult.Ok -> values[name] = result.value
                    is GMResult.Err -> return implicitUpdateFailure(
                        result.error,
                    )
                }
            }
            val loopDetection = when (
                val result = evaluateImplicitBoolean(
                    term = "implicitcurve.loopDetection",
                    function = definition.loopDetection,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return implicitUpdateFailure(
                    result.error,
                )
            }

            var evaluationError: CurveImplicitUpdateError? = null
            fun adapter(
                term: String,
                function: JessieCodeCoordinateFunction,
            ): (Double, Double) -> Double = { x, y ->
                if (evaluationError != null) {
                    Double.NaN
                } else {
                    when (
                        val result = evaluateImplicitNumber(
                            term = "implicitcurve.$term",
                            function = function,
                            arguments = listOf(
                                JessieCodeRuntimeValue.NumberValue(x),
                                JessieCodeRuntimeValue.NumberValue(y),
                            ),
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> {
                            evaluationError = result.error
                            Double.NaN
                        }
                    }
                }
            }

            val plot = ImplicitPlot(
                boundingBox = boundingBox,
                config = ImplicitPlotConfig(
                    resolutionOuter =
                        maxOf(0.01, values.getValue("resolutionOuter")),
                    resolutionInner =
                        maxOf(0.01, values.getValue("resolutionInner")),
                    maxSteps = values.getValue("maxSteps"),
                    alpha0 = values.getValue("alpha0"),
                    tolU0 = values.getValue("tolU0"),
                    tolNewton = values.getValue("tolNewton"),
                    tolCusp = values.getValue("tolCusp"),
                    tolProgress = values.getValue("tolProgress"),
                    qdtBox = values.getValue("qdtBox"),
                    kappa0 = values.getValue("kappa0"),
                    delta0 = values.getValue("delta0"),
                    hInitial = values.getValue("hInitial"),
                    hCritical = values.getValue("hCritical"),
                    hMax = values.getValue("hMax"),
                    loopDist = values.getValue("loopDist"),
                    loopDir = values.getValue("loopDir"),
                    loopDetection = loopDetection,
                    unitX = board.unitX,
                    unitY = board.unitY,
                ),
                f = adapter("f", definition.f),
                dfx = definition.dfx?.let { adapter("dfx", it) },
                dfy = definition.dfy?.let { adapter("dfy", it) },
                maximumPointCount = board.maxCurvePoints,
            )
            val result = plot.plot()
            evaluationError?.let {
                return implicitUpdateFailure(it)
            }
            return when (result) {
                is GMResult.Ok -> GMResult.Ok(
                    CurveDataUpdate(
                        x = result.value.dataX,
                        y = result.value.dataY,
                    ),
                )
                is GMResult.Err -> implicitUpdateFailure(
                    CurveImplicitUpdateError.Plot(result.error),
                )
            }
        }

        private fun evaluateImplicitNumber(
            term: String,
            function: JessieCodeCoordinateFunction,
            arguments: List<JessieCodeRuntimeValue> = emptyList(),
        ): GMResult<Double, CurveImplicitUpdateError> =
            when (val result = function.evaluate(arguments)) {
                is GMResult.Err -> GMResult.Err(
                    CurveImplicitUpdateError.ExpressionEvaluation(
                        term = term,
                        error = result.error,
                    ),
                )
                is GMResult.Ok -> {
                    val number = result.value as?
                        JessieCodeRuntimeValue.NumberValue
                    if (number != null) {
                        GMResult.Ok(number.value)
                    } else {
                        GMResult.Err(
                            CurveImplicitUpdateError.InvalidValue(
                                term = term,
                                expected = "number",
                                actualType = runtimeType(result.value),
                            ),
                        )
                    }
                }
            }

        private fun evaluateImplicitBoolean(
            term: String,
            function: JessieCodeCoordinateFunction,
        ): GMResult<Boolean, CurveImplicitUpdateError> =
            when (val result = function.evaluate()) {
                is GMResult.Err -> GMResult.Err(
                    CurveImplicitUpdateError.ExpressionEvaluation(
                        term = term,
                        error = result.error,
                    ),
                )
                is GMResult.Ok -> {
                    val boolean = result.value as?
                        JessieCodeRuntimeValue.BooleanValue
                    if (boolean != null) {
                        GMResult.Ok(boolean.value)
                    } else {
                        GMResult.Err(
                            CurveImplicitUpdateError.InvalidValue(
                                term = term,
                                expected = "boolean",
                                actualType = runtimeType(result.value),
                            ),
                        )
                    }
                }
            }

        private fun evaluateImplicitRange(
            axis: String,
            function: JessieCodeCoordinateFunction?,
        ): GMResult<DoubleArray, CurveImplicitUpdateError> {
            val rangeFunction = function ?: return GMResult.Err(
                CurveImplicitUpdateError.InvalidValue(
                    term = "implicitcurve.domain$axis",
                    expected = "array of two finite numbers",
                    actualType = "undefined",
                ),
            )
            return when (val result = rangeFunction.evaluate()) {
                is GMResult.Err -> GMResult.Err(
                    CurveImplicitUpdateError.ExpressionEvaluation(
                        term = "implicitcurve.domain$axis",
                        error = result.error,
                    ),
                )
                is GMResult.Ok -> {
                    val values = (
                        result.value as?
                            JessieCodeRuntimeValue.ArrayValue
                        )?.values
                    if (
                        values == null ||
                        values.size != 2 ||
                        values.any {
                            it !is JessieCodeRuntimeValue.NumberValue
                        }
                    ) {
                        GMResult.Err(
                            CurveImplicitUpdateError.InvalidValue(
                                term = "implicitcurve.domain$axis",
                                expected = "array of two finite numbers",
                                actualType = runtimeType(result.value),
                            ),
                        )
                    } else {
                        val numbers = DoubleArray(2) { index ->
                            (
                                values[index] as
                                    JessieCodeRuntimeValue.NumberValue
                                ).value
                        }
                        if (numbers.any { !it.isFinite() }) {
                            GMResult.Err(
                                CurveImplicitUpdateError.InvalidDomain(
                                    axis = axis,
                                    values = numbers,
                                ),
                            )
                        } else {
                            GMResult.Ok(numbers)
                        }
                    }
                }
            }
        }

        private fun <T> implicitUpdateFailure(
            error: CurveImplicitUpdateError,
        ): GMResult<T, CurveDataUpdateError> =
            GMResult.Err(CurveDataUpdateError.ImplicitCurve(error))

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

        private fun validateContinuousPlotting(
            sampleCount: Int,
            plotOptions: CurvePlotOptions,
        ): GMResult<Unit, CurveError> {
            if (sampleCount !in 1..MAX_SAMPLE_COUNT) {
                return GMResult.Err(
                    CurveError.InvalidSampleCount(
                        count = sampleCount,
                        maximum = MAX_SAMPLE_COUNT,
                    ),
                )
            }
            if (
                plotOptions.doAdvancedPlot &&
                plotOptions.plotVersion !in SUPPORTED_PLOT_VERSIONS
            ) {
                return GMResult.Err(
                    CurveError.UnsupportedPlotVersion(
                        version = plotOptions.plotVersion,
                    ),
                )
            }
            if (
                plotOptions.recursionDepthHigh !in
                1..MAX_RECURSION_DEPTH
            ) {
                return GMResult.Err(
                    CurveError.InvalidRecursionDepth(
                        depth = plotOptions.recursionDepthHigh,
                        maximum = MAX_RECURSION_DEPTH,
                    ),
                )
            }
            return GMResult.Ok(Unit)
        }

        private fun validateTraceParent(
            board: Board,
            parent: GeometryElement,
            role: String,
        ): CurveError? {
            if (parent.board !== board) {
                return CurveError.TraceParentBoardMismatch(role)
            }
            if (
                parent.id.isEmpty() ||
                board.elementById(parent.id) !== parent
            ) {
                return CurveError.TraceParentNotRegistered(
                    role = role,
                    id = parent.id,
                )
            }
            return null
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
