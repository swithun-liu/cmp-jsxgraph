/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/line.js -> createTangent / createPolarLine
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.ContinuousCurve2D
import com.swithun.jsxgraph.core.math.ContinuousCurveType
import com.swithun.jsxgraph.core.math.DiscreteCurve2D
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.GeometryError
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.Numerics
import com.swithun.jsxgraph.core.math.ParametricCurve2D
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.floor

internal sealed interface TangentError {
    data class UnsupportedParents(
        val parentTypes: List<String>,
    ) : TangentError

    data class UnsupportedConic(
        val parentIndex: Int,
    ) : TangentError

    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : TangentError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : TangentError

    data class PointCreation(
        val pointIndex: Int,
        val error: PointError,
    ) : TangentError

    data class InvalidCurvePointCount(
        val pointCount: Int,
    ) : TangentError

    data class UnsupportedCurveDegree(
        val degree: Int,
    ) : TangentError

    data class CurveProjection(
        val error: GeometryError,
    ) : TangentError

    data class LineCreation(
        val error: LineError,
    ) : TangentError
}

internal object Tangent {
    private const val TANGENT_ELEMENT_TYPE = "tangent"
    private const val POLAR_LINE_ELEMENT_TYPE = "polarline"

    // JSXGraph: src/base/line.js -> createTangent Line/Point and
    // Circle/Point branches.
    internal fun create(
        board: Board,
        firstParent: GeometryElement,
        secondParent: GeometryElement,
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        straightFirst: Boolean = true,
        straightLast: Boolean = true,
        point1Id: String = "",
        point1Name: String? = null,
        point1NeedsRegularUpdate: Boolean = true,
        point2Id: String = "",
        point2Name: String? = null,
        point2NeedsRegularUpdate: Boolean = true,
    ): GMResult<Line, TangentError> {
        val lineParents = when {
            firstParent is Line && secondParent is Point ->
                LineParents(
                    line = firstParent,
                    point = secondParent,
                    lineIndex = 0,
                    pointIndex = 1,
                )
            firstParent is Point && secondParent is Line ->
                LineParents(
                    line = secondParent,
                    point = firstParent,
                    lineIndex = 1,
                    pointIndex = 0,
                )
            else -> null
        }
        if (lineParents != null) {
            return createLineTangent(
                board = board,
                firstParent = firstParent,
                secondParent = secondParent,
                parents = lineParents,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                straightFirst = straightFirst,
                straightLast = straightLast,
            )
        }
        val curveParents = when {
            firstParent is Curve && secondParent is Point ->
                CurveParents(
                    curve = firstParent,
                    point = secondParent,
                    curveIndex = 0,
                    pointIndex = 1,
                )
            firstParent is Point && secondParent is Curve ->
                CurveParents(
                    curve = secondParent,
                    point = firstParent,
                    curveIndex = 1,
                    pointIndex = 0,
                )
            else -> null
        }
        if (curveParents != null) {
            return createCurveTangent(
                board = board,
                firstParent = firstParent,
                secondParent = secondParent,
                parents = curveParents,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                straightFirst = straightFirst,
                straightLast = straightLast,
                point1Id = point1Id,
                point1Name = point1Name,
                point1NeedsRegularUpdate = point1NeedsRegularUpdate,
                point2Id = point2Id,
                point2Name = point2Name,
                point2NeedsRegularUpdate = point2NeedsRegularUpdate,
            )
        }
        return createCircleTangent(
            board = board,
            firstParent = firstParent,
            secondParent = secondParent,
            canonicalizeParents = false,
            resultElementType = TANGENT_ELEMENT_TYPE,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            straightFirst = straightFirst,
            straightLast = straightLast,
            point1Id = point1Id,
            point1Name = point1Name,
            point1NeedsRegularUpdate = point1NeedsRegularUpdate,
            point2Id = point2Id,
            point2Name = point2Name,
            point2NeedsRegularUpdate = point2NeedsRegularUpdate,
        )
    }

    // JSXGraph: src/base/line.js -> createPolarLine.
    internal fun createPolarLine(
        board: Board,
        firstParent: GeometryElement,
        secondParent: GeometryElement,
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        straightFirst: Boolean = true,
        straightLast: Boolean = true,
        point1Id: String = "",
        point1Name: String? = null,
        point1NeedsRegularUpdate: Boolean = true,
        point2Id: String = "",
        point2Name: String? = null,
        point2NeedsRegularUpdate: Boolean = true,
    ): GMResult<Line, TangentError> {
        conicParentIndex(firstParent, secondParent)?.let { parentIndex ->
            return GMResult.Err(TangentError.UnsupportedConic(parentIndex))
        }
        return createCircleTangent(
            board = board,
            firstParent = firstParent,
            secondParent = secondParent,
            canonicalizeParents = true,
            resultElementType = POLAR_LINE_ELEMENT_TYPE,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            straightFirst = straightFirst,
            straightLast = straightLast,
            point1Id = point1Id,
            point1Name = point1Name,
            point1NeedsRegularUpdate = point1NeedsRegularUpdate,
            point2Id = point2Id,
            point2Name = point2Name,
            point2NeedsRegularUpdate = point2NeedsRegularUpdate,
        )
    }

    // JSXGraph: src/base/line.js -> createTangent Line/Point branch.
    private fun createLineTangent(
        board: Board,
        firstParent: GeometryElement,
        secondParent: GeometryElement,
        parents: LineParents,
        id: String,
        name: String?,
        needsRegularUpdate: Boolean,
        straightFirst: Boolean,
        straightLast: Boolean,
    ): GMResult<Line, TangentError> {
        validateParent(
            board = board,
            element = parents.line,
            parentIndex = parents.lineIndex,
        )?.let {
            return GMResult.Err(it)
        }
        validateParent(
            board = board,
            element = parents.point,
            parentIndex = parents.pointIndex,
        )?.let {
            return GMResult.Err(it)
        }

        val tangent = when (
            val result = Line.create(
                board = board,
                point1 = parents.line.point1,
                point2 = parents.line.point2,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                TangentError.LineCreation(result.error),
            )
        }
        tangent.elType = TANGENT_ELEMENT_TYPE
        tangent.type = Const.OBJECT_TYPE_TANGENT
        tangent.configureVisibleRange(
            straightFirst = straightFirst,
            straightLast = straightLast,
        )
        tangent.glider = parents.point
        tangent.setParents(listOf(firstParent, secondParent))
        return GMResult.Ok(tangent)
    }

    // JSXGraph: src/base/line.js -> createTangent Curve/Point branch.
    private fun createCurveTangent(
        board: Board,
        firstParent: GeometryElement,
        secondParent: GeometryElement,
        parents: CurveParents,
        id: String,
        name: String?,
        needsRegularUpdate: Boolean,
        straightFirst: Boolean,
        straightLast: Boolean,
        point1Id: String,
        point1Name: String?,
        point1NeedsRegularUpdate: Boolean,
        point2Id: String,
        point2Name: String?,
        point2NeedsRegularUpdate: Boolean,
    ): GMResult<Line, TangentError> {
        validateParent(
            board = board,
            element = parents.curve,
            parentIndex = parents.curveIndex,
        )?.let {
            return GMResult.Err(it)
        }
        validateParent(
            board = board,
            element = parents.point,
            parentIndex = parents.pointIndex,
        )?.let {
            return GMResult.Err(it)
        }
        if (parents.curve.type == Const.OBJECT_TYPE_CONIC) {
            return GMResult.Err(
                TangentError.UnsupportedConic(parents.curveIndex),
            )
        }

        val coefficients = CurveTangentCoefficientFunction(
            curve = parents.curve,
            point = parents.point,
        )
        when (val result = coefficients.evaluateResult()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val point1 = when (
            val result = createEndpoint(
                board = board,
                coefficients = coefficients,
                firstEndpoint = true,
                id = point1Id,
                name = point1Name,
                needsRegularUpdate = point1NeedsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                TangentError.PointCreation(
                    pointIndex = 0,
                    error = result.error,
                ),
            )
        }
        val point2 = when (
            val result = createEndpoint(
                board = board,
                coefficients = coefficients,
                firstEndpoint = false,
                id = point2Id,
                name = point2Name,
                needsRegularUpdate = point2NeedsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(point1)
                return GMResult.Err(
                    TangentError.PointCreation(
                        pointIndex = 1,
                        error = result.error,
                    ),
                )
            }
        }
        val line = when (
            val result = Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObjects(listOf(point1, point2))
                return GMResult.Err(
                    TangentError.LineCreation(result.error),
                )
            }
        }

        line.elType = TANGENT_ELEMENT_TYPE
        line.type = Const.OBJECT_TYPE_TANGENT
        line.constrained = true
        line.isDraggable = false
        line.configureVisibleRange(
            straightFirst = straightFirst,
            straightLast = straightLast,
        )
        line.glider = parents.point
        line.setParents(listOf(firstParent, secondParent))
        parents.point.addChild(line)
        return GMResult.Ok(line)
    }

    // JSXGraph: src/base/line.js -> createTangent Circle/Point branch.
    private fun createCircleTangent(
        board: Board,
        firstParent: GeometryElement,
        secondParent: GeometryElement,
        canonicalizeParents: Boolean,
        resultElementType: String,
        id: String,
        name: String?,
        needsRegularUpdate: Boolean,
        straightFirst: Boolean,
        straightLast: Boolean,
        point1Id: String,
        point1Name: String?,
        point1NeedsRegularUpdate: Boolean,
        point2Id: String,
        point2Name: String?,
        point2NeedsRegularUpdate: Boolean,
    ): GMResult<Line, TangentError> {
        val canonicalParents = when {
            firstParent is Circle && secondParent is Point ->
                CanonicalParents(
                    circle = firstParent,
                    point = secondParent,
                    circleIndex = 0,
                    pointIndex = 1,
                )
            firstParent is Point && secondParent is Circle ->
                CanonicalParents(
                    circle = secondParent,
                    point = firstParent,
                    circleIndex = 1,
                    pointIndex = 0,
                )
            else -> return GMResult.Err(
                TangentError.UnsupportedParents(
                    parentTypes = listOf(
                        firstParent.elType,
                        secondParent.elType,
                    ),
                ),
            )
        }
        validateParent(
            board = board,
            element = canonicalParents.circle,
            parentIndex = canonicalParents.circleIndex,
        )?.let {
            return GMResult.Err(it)
        }
        validateParent(
            board = board,
            element = canonicalParents.point,
            parentIndex = canonicalParents.pointIndex,
        )?.let {
            return GMResult.Err(it)
        }

        val coefficients = CircleTangentCoefficientFunction(
            circle = canonicalParents.circle,
            point = canonicalParents.point,
        )
        val point1 = when (
            val result = createEndpoint(
                board = board,
                coefficients = coefficients,
                firstEndpoint = true,
                id = point1Id,
                name = point1Name,
                needsRegularUpdate = point1NeedsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                TangentError.PointCreation(
                    pointIndex = 0,
                    error = result.error,
                ),
            )
        }
        val point2 = when (
            val result = createEndpoint(
                board = board,
                coefficients = coefficients,
                firstEndpoint = false,
                id = point2Id,
                name = point2Name,
                needsRegularUpdate = point2NeedsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(point1)
                return GMResult.Err(
                    TangentError.PointCreation(
                        pointIndex = 1,
                        error = result.error,
                    ),
                )
            }
        }
        val line = when (
            val result = Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObjects(listOf(point1, point2))
                return GMResult.Err(
                    TangentError.LineCreation(result.error),
                )
            }
        }

        line.elType = resultElementType
        line.type = Const.OBJECT_TYPE_TANGENT
        line.constrained = true
        line.isDraggable = false
        line.configureVisibleRange(
            straightFirst = straightFirst,
            straightLast = straightLast,
        )
        line.glider = canonicalParents.point
        line.setParents(
            if (canonicalizeParents) {
                listOf(
                    canonicalParents.circle,
                    canonicalParents.point,
                )
            } else {
                listOf(firstParent, secondParent)
            },
        )
        canonicalParents.point.addChild(line)
        return GMResult.Ok(line)
    }

    private fun createEndpoint(
        board: Board,
        coefficients: TangentCoefficientProvider,
        firstEndpoint: Boolean,
        id: String,
        name: String?,
        needsRegularUpdate: Boolean,
    ): GMResult<Point, PointError> =
        Point.createConstrained(
            board = board,
            coordinateFunctions = listOf(
                TangentEndpointFunction(
                    coefficients = coefficients,
                    firstEndpoint = firstEndpoint,
                ),
            ),
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            fixed = false,
        )

    private fun validateParent(
        board: Board,
        element: GeometryElement,
        parentIndex: Int,
    ): TangentError? =
        when {
            element.board !== board ->
                TangentError.ParentBoardMismatch(parentIndex)
            board.elementById(element.id) !== element ->
                TangentError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = element.id,
                )
            else -> null
        }

    private fun conicParentIndex(
        firstParent: GeometryElement,
        secondParent: GeometryElement,
    ): Int? =
        when {
            firstParent.type == Const.OBJECT_TYPE_CONIC -> 0
            secondParent.type == Const.OBJECT_TYPE_CONIC -> 1
            else -> null
        }

    private data class CanonicalParents(
        val circle: Circle,
        val point: Point,
        val circleIndex: Int,
        val pointIndex: Int,
    )

    private data class LineParents(
        val line: Line,
        val point: Point,
        val lineIndex: Int,
        val pointIndex: Int,
    )

    private data class CurveParents(
        val curve: Curve,
        val point: Point,
        val curveIndex: Int,
        val pointIndex: Int,
    )
}

private fun interface TangentCoefficientProvider {
    fun evaluate(): DoubleArray
}

// JSXGraph: src/base/line.js -> createTangent Circle/Point coefficient closure.
private class CircleTangentCoefficientFunction(
    private val circle: Circle,
    private val point: Point,
) : TangentCoefficientProvider {
    override fun evaluate(): DoubleArray =
        Mat.matVecMult(
            matrix = circle.quadraticform,
            vector = point.coords.usrCoords,
        )
}

// JSXGraph: src/base/line.js -> createTangent Curve/Point coefficient closure.
private class CurveTangentCoefficientFunction(
    private val curve: Curve,
    private val point: Point,
) : TangentCoefficientProvider {
    override fun evaluate(): DoubleArray =
        when (val result = evaluateResult()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> doubleArrayOf(
                Double.NaN,
                Double.NaN,
                Double.NaN,
            )
        }

    fun evaluateResult(): GMResult<DoubleArray, TangentError> =
        if (curve.curveType == PLOT_CURVE_TYPE) {
            evaluatePlot()
        } else {
            evaluateContinuous()
        }

    private fun evaluateContinuous(): GMResult<DoubleArray, TangentError> {
        val parameter =
            if (curve.curveType == FUNCTION_GRAPH_CURVE_TYPE) {
                point.X()
            } else {
                when (
                    val result = Geometry.projectCoordsToCurve(
                        horizontal = point.X(),
                        vertical = point.Y(),
                        initialParameter = 0.0,
                        continuousCurve = ContinuousCurve2D(
                            curve = ParametricCurve2D(
                                x = curve::X,
                                y = curve::Y,
                            ),
                            minimumParameter = curve.minX(),
                            maximumParameter = curve.maxX(),
                            type = ContinuousCurveType.PARAMETER,
                        ),
                    )
                ) {
                    is GMResult.Ok -> result.value.parameter
                    is GMResult.Err -> return GMResult.Err(
                        TangentError.CurveProjection(result.error),
                    )
                }
            }
        val verticalDerivative = Numerics.D(curve::Y)(parameter)
        val horizontalDerivative = Numerics.D(curve::X)(parameter)
        val coordinates = point.coords.usrCoords
        return GMResult.Ok(
            doubleArrayOf(
                -coordinates[1] * verticalDerivative +
                    coordinates[2] * horizontalDerivative,
                coordinates[0] * verticalDerivative,
                -coordinates[0] * horizontalDerivative,
            ),
        )
    }

    private fun evaluatePlot(): GMResult<DoubleArray, TangentError> {
        if (curve.numberPoints < 2) {
            return GMResult.Err(
                TangentError.InvalidCurvePointCount(curve.numberPoints),
            )
        }
        if (curve.bezierDegree != 1) {
            return GMResult.Err(
                TangentError.UnsupportedCurveDegree(curve.bezierDegree),
            )
        }
        val projection = when (
            val result = Geometry.projectCoordsToCurve(
                point = point.coords.usrCoords,
                curve = DiscreteCurve2D(
                    points = curve.points.map { it.usrCoords.copyOf() },
                    bezierDegree = curve.bezierDegree,
                ),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                TangentError.CurveProjection(result.error),
            )
        }
        var index = floor(projection.parameter).toInt()
        if (index == curve.numberPoints - 1) {
            index -= 1
        }
        if (index !in 0 until curve.numberPoints - 1) {
            return GMResult.Err(
                TangentError.InvalidCurvePointCount(curve.numberPoints),
            )
        }
        val first = curve.points[index].usrCoords
        val second = curve.points[index + 1].usrCoords
        return GMResult.Ok(
            doubleArrayOf(
                first[2] * second[1] - first[1] * second[2],
                second[2] - first[2],
                first[1] - second[1],
            ),
        )
    }

    private companion object {
        const val PLOT_CURVE_TYPE = "plot"
        const val FUNCTION_GRAPH_CURVE_TYPE = "functiongraph"
    }
}

// JSXGraph: src/base/line.js -> createLine one-function coefficient branch.
private class TangentEndpointFunction(
    private val coefficients: TangentCoefficientProvider,
    private val firstEndpoint: Boolean,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()
    override val returnsCoordinateArray: Boolean = true

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        val values = coefficients.evaluate()
        val a = values[0]
        val b = values[1]
        val c = values[2]
        val homogeneous = c * c + b * b
        val coordinates =
            if (firstEndpoint) {
                doubleArrayOf(
                    homogeneous * 0.5,
                    (c - b * a + c) * 0.5,
                    (-b - c * a - b) * 0.5,
                )
            } else {
                doubleArrayOf(
                    homogeneous,
                    -b * a + c,
                    -c * a - b,
                )
            }
        return GMResult.Ok(
            JessieCodeRuntimeValue.ArrayValue(
                coordinates.map(JessieCodeRuntimeValue::NumberValue),
            ),
        )
    }
}
