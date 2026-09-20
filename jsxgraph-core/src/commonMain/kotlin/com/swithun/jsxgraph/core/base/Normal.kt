/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/line.js -> createNormal
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

internal sealed interface NormalError {
    data class UnsupportedParents(
        val parentTypes: List<String>,
    ) : NormalError

    data class UnsupportedConic(
        val parentIndex: Int,
    ) : NormalError

    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : NormalError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : NormalError

    data class PointCreation(
        val pointIndex: Int,
        val error: PointError,
    ) : NormalError

    data class InvalidCurvePointCount(
        val pointCount: Int,
    ) : NormalError

    data class UnsupportedCurveDegree(
        val degree: Int,
    ) : NormalError

    data class CurveProjection(
        val error: GeometryError,
    ) : NormalError

    data class LineCreation(
        val error: LineError,
    ) : NormalError
}

internal object Normal {
    private const val NORMAL_ELEMENT_TYPE = "normal"

    // JSXGraph: src/base/line.js -> createNormal.
    internal fun create(
        board: Board,
        firstParent: GeometryElement,
        secondParent: GeometryElement,
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        straightFirst: Boolean = true,
        straightLast: Boolean = true,
        pointId: String = "",
        pointName: String? = null,
        pointNeedsRegularUpdate: Boolean = true,
        point1Id: String = "",
        point1Name: String? = null,
        point1NeedsRegularUpdate: Boolean = true,
        point2Id: String = "",
        point2Name: String? = null,
        point2NeedsRegularUpdate: Boolean = true,
    ): GMResult<Line, NormalError> {
        val parents = resolveParents(firstParent, secondParent)
            ?: return GMResult.Err(
                NormalError.UnsupportedParents(
                    listOf(firstParent.elType, secondParent.elType),
                ),
            )
        validateParent(
            board = board,
            element = parents.element,
            parentIndex = parents.elementIndex,
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

        return when (val element = parents.element) {
            is Line -> createLineNormal(
                board = board,
                firstParent = firstParent,
                secondParent = secondParent,
                source = element,
                point = parents.point,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                straightFirst = straightFirst,
                straightLast = straightLast,
                pointId = pointId,
                pointName = pointName,
                pointNeedsRegularUpdate = pointNeedsRegularUpdate,
            )
            is Circle -> createCircleNormal(
                board = board,
                firstParent = firstParent,
                secondParent = secondParent,
                source = element,
                point = parents.point,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                straightFirst = straightFirst,
                straightLast = straightLast,
            )
            is Curve ->
                if (element.type == Const.OBJECT_TYPE_CONIC) {
                    GMResult.Err(
                        NormalError.UnsupportedConic(parents.elementIndex),
                    )
                } else {
                    createCurveNormal(
                        board = board,
                        firstParent = firstParent,
                        secondParent = secondParent,
                        source = element,
                        point = parents.point,
                        id = id,
                        name = name,
                        needsRegularUpdate = needsRegularUpdate,
                        straightFirst = straightFirst,
                        straightLast = straightLast,
                        point1Id = point1Id,
                        point1Name = point1Name,
                        point1NeedsRegularUpdate =
                            point1NeedsRegularUpdate,
                        point2Id = point2Id,
                        point2Name = point2Name,
                        point2NeedsRegularUpdate =
                            point2NeedsRegularUpdate,
                    )
                }
            else -> GMResult.Err(
                NormalError.UnsupportedParents(
                    listOf(firstParent.elType, secondParent.elType),
                ),
            )
        }
    }

    private fun createLineNormal(
        board: Board,
        firstParent: GeometryElement,
        secondParent: GeometryElement,
        source: Line,
        point: Point,
        id: String,
        name: String?,
        needsRegularUpdate: Boolean,
        straightFirst: Boolean,
        straightLast: Boolean,
        pointId: String,
        pointName: String?,
        pointNeedsRegularUpdate: Boolean,
    ): GMResult<Line, NormalError> {
        val helper = when (
            val result = Point.createConstrained(
                board = board,
                coordinateFunctions = listOf(
                    NormalLineDirectionFunction(source),
                ),
                id = pointId,
                name = pointName,
                needsRegularUpdate = pointNeedsRegularUpdate,
                fixed = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                NormalError.PointCreation(
                    pointIndex = 0,
                    error = result.error,
                ),
            )
        }
        helper.isDraggable = true

        val output = when (
            val result = Line.create(
                board = board,
                point1 = point,
                point2 = helper,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(helper)
                return GMResult.Err(
                    NormalError.LineCreation(result.error),
                )
            }
        }
        output.normalPoint = helper
        output.subs["point"] = helper
        output.inherits += helper
        configure(
            output = output,
            firstParent = firstParent,
            secondParent = secondParent,
            source = source,
            point = point,
            straightFirst = straightFirst,
            straightLast = straightLast,
        )
        return GMResult.Ok(output)
    }

    private fun createCircleNormal(
        board: Board,
        firstParent: GeometryElement,
        secondParent: GeometryElement,
        source: Circle,
        point: Point,
        id: String,
        name: String?,
        needsRegularUpdate: Boolean,
        straightFirst: Boolean,
        straightLast: Boolean,
    ): GMResult<Line, NormalError> {
        val output = when (
            val result = Line.create(
                board = board,
                point1 = source.center,
                point2 = point,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                NormalError.LineCreation(result.error),
            )
        }
        configure(
            output = output,
            firstParent = firstParent,
            secondParent = secondParent,
            source = source,
            point = point,
            straightFirst = straightFirst,
            straightLast = straightLast,
        )
        return GMResult.Ok(output)
    }

    private fun createCurveNormal(
        board: Board,
        firstParent: GeometryElement,
        secondParent: GeometryElement,
        source: Curve,
        point: Point,
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
    ): GMResult<Line, NormalError> {
        val coefficients = CurveNormalCoefficientFunction(source, point)
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
                NormalError.PointCreation(
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
                    NormalError.PointCreation(
                        pointIndex = 1,
                        error = result.error,
                    ),
                )
            }
        }
        val output = when (
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
                    NormalError.LineCreation(result.error),
                )
            }
        }
        output.constrained = true
        output.isDraggable = false
        configure(
            output = output,
            firstParent = firstParent,
            secondParent = secondParent,
            source = source,
            point = point,
            straightFirst = straightFirst,
            straightLast = straightLast,
        )
        return GMResult.Ok(output)
    }

    private fun configure(
        output: Line,
        firstParent: GeometryElement,
        secondParent: GeometryElement,
        source: GeometryElement,
        point: Point,
        straightFirst: Boolean,
        straightLast: Boolean,
    ) {
        output.elType = NORMAL_ELEMENT_TYPE
        output.configureVisibleRange(straightFirst, straightLast)
        output.setParents(listOf(firstParent, secondParent))
        point.addChild(output)
        source.addChild(output)
    }

    private fun createEndpoint(
        board: Board,
        coefficients: NormalCoefficientProvider,
        firstEndpoint: Boolean,
        id: String,
        name: String?,
        needsRegularUpdate: Boolean,
    ): GMResult<Point, PointError> =
        Point.createConstrained(
            board = board,
            coordinateFunctions = listOf(
                NormalEndpointFunction(
                    coefficients = coefficients,
                    firstEndpoint = firstEndpoint,
                ),
            ),
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            fixed = false,
        )

    private fun resolveParents(
        firstParent: GeometryElement,
        secondParent: GeometryElement,
    ): NormalParents? =
        when {
            firstParent is Point && isSupportedElement(secondParent) ->
                NormalParents(
                    element = secondParent,
                    point = firstParent,
                    elementIndex = 1,
                    pointIndex = 0,
                )
            secondParent is Point && isSupportedElement(firstParent) ->
                NormalParents(
                    element = firstParent,
                    point = secondParent,
                    elementIndex = 0,
                    pointIndex = 1,
                )
            else -> null
        }

    private fun isSupportedElement(element: GeometryElement): Boolean =
        element is Line || element is Circle || element is Curve

    private fun validateParent(
        board: Board,
        element: GeometryElement,
        parentIndex: Int,
    ): NormalError? =
        when {
            element.board !== board ->
                NormalError.ParentBoardMismatch(parentIndex)
            board.elementById(element.id) !== element ->
                NormalError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = element.id,
                )
            else -> null
        }

    private data class NormalParents(
        val element: GeometryElement,
        val point: Point,
        val elementIndex: Int,
        val pointIndex: Int,
    )
}

private fun interface NormalCoefficientProvider {
    fun evaluate(): DoubleArray
}

// JSXGraph: src/base/line.js -> createNormal Line/Point helper closure.
private class NormalLineDirectionFunction(
    private val line: Line,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()
    override val returnsCoordinateArray: Boolean = true

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Ok(
            JessieCodeRuntimeValue.ArrayValue(
                listOf(
                    0.0,
                    -line.stdform[1],
                    -line.stdform[2],
                ).map(JessieCodeRuntimeValue::NumberValue),
            ),
        )
}

// JSXGraph: src/base/line.js -> createNormal Curve/Point coefficient closure.
private class CurveNormalCoefficientFunction(
    private val curve: Curve,
    private val point: Point,
) : NormalCoefficientProvider {
    override fun evaluate(): DoubleArray =
        when (val result = evaluateResult()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> DoubleArray(3) { Double.NaN }
        }

    fun evaluateResult(): GMResult<DoubleArray, NormalError> =
        if (curve.curveType == PLOT_CURVE_TYPE) {
            evaluatePlot()
        } else {
            evaluateContinuous()
        }

    private fun evaluateContinuous(): GMResult<DoubleArray, NormalError> {
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
                        NormalError.CurveProjection(result.error),
                    )
                }
            }
        val verticalDerivative = Numerics.D(curve::Y)(parameter)
        val horizontalDerivative = Numerics.D(curve::X)(parameter)
        val coordinates = point.coords.usrCoords
        return GMResult.Ok(
            doubleArrayOf(
                -coordinates[1] * horizontalDerivative -
                    coordinates[2] * verticalDerivative,
                coordinates[0] * horizontalDerivative,
                coordinates[0] * verticalDerivative,
            ),
        )
    }

    private fun evaluatePlot(): GMResult<DoubleArray, NormalError> {
        if (curve.numberPoints < 2) {
            return GMResult.Err(
                NormalError.InvalidCurvePointCount(curve.numberPoints),
            )
        }
        if (curve.bezierDegree != 1) {
            return GMResult.Err(
                NormalError.UnsupportedCurveDegree(curve.bezierDegree),
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
                NormalError.CurveProjection(result.error),
            )
        }
        var index = floor(projection.parameter).toInt()
        if (index == curve.numberPoints - 1) {
            index -= 1
        }
        if (index !in 0 until curve.numberPoints - 1) {
            return GMResult.Err(
                NormalError.InvalidCurvePointCount(curve.numberPoints),
            )
        }
        val lambda = projection.parameter - index
        val first = curve.points[index].usrCoords
        val second = curve.points[index + 1].usrCoords
        val projected = DoubleArray(3) { coordinateIndex ->
            first[coordinateIndex] +
                lambda * (second[coordinateIndex] - first[coordinateIndex])
        }
        val tangent = Mat.crossProduct(first, second)
        val idealNormal = doubleArrayOf(
            0.0,
            -tangent[1],
            -tangent[2],
        )
        return GMResult.Ok(Mat.crossProduct(projected, idealNormal))
    }

    private companion object {
        const val PLOT_CURVE_TYPE = "plot"
        const val FUNCTION_GRAPH_CURVE_TYPE = "functiongraph"
    }
}

// JSXGraph: src/base/line.js -> createLine one-function coefficient branch.
private class NormalEndpointFunction(
    private val coefficients: NormalCoefficientProvider,
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
