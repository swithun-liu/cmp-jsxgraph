/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/point.js -> createIntersectionPoint /
 * createOtherIntersectionPoint,
 * src/math/geometry.js -> intersectionFunction /
 * otherIntersectionFunction
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Clip
import com.swithun.jsxgraph.core.math.ClipError
import com.swithun.jsxgraph.core.math.ContinuousCurve2D
import com.swithun.jsxgraph.core.math.ContinuousCurveType
import com.swithun.jsxgraph.core.math.DiscreteCurve2D
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.GeometryError
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.ParametricCurve2D
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue

internal sealed interface IntersectionError {
    data class UnsupportedParentTypes(
        val firstType: String,
        val secondType: String,
    ) : IntersectionError

    data class UnsupportedConic(
        val parentIndex: Int,
    ) : IntersectionError

    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : IntersectionError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : IntersectionError

    data class ExcludedPointBoardMismatch(
        val pointIndex: Int,
    ) : IntersectionError

    data class ExcludedPointNotRegistered(
        val pointIndex: Int,
        val id: String,
    ) : IntersectionError

    data class IndexFunctionEvaluation(
        val error: JessieCodeRuntimeError,
    ) : IntersectionError

    data class NonNumericIndexFunction(
        val actualType: String,
    ) : IntersectionError

    data class GeometryComputation(
        val error: GeometryError,
    ) : IntersectionError

    data class ClipComputation(
        val error: ClipError,
    ) : IntersectionError

    data class Registration(
        val error: BoardError,
    ) : IntersectionError
}

internal sealed interface IntersectionIndexSource {
    data class Number(
        val value: Double,
    ) : IntersectionIndexSource

    data class Function(
        val value: JessieCodeRuntimeValue.FunctionValue,
        val location: JessieCodeAstLocation,
    ) : IntersectionIndexSource
}

/**
 * A constrained Point at one intersection of two translated one-dimensional
 * elements.
 *
 * This slice covers the translated Line, Circle, Curve, Arc, Sector,
 * Polygon/Line, and Polygon/path branches.
 */
internal class IntersectionPoint private constructor(
    board: Board,
    internal val firstElement: GeometryElement,
    internal val secondElement: GeometryElement,
    internal val firstIndex: IntersectionIndexSource,
    internal val secondIndex: IntersectionIndexSource,
    internal val alwaysIntersect: Boolean,
    initialCoordinates: DoubleArray,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    fixed: Boolean,
) : Point(
    board = board,
    coordinates = initialCoordinates,
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
    fixed = fixed,
) {
    internal var intersectionEvaluationError: IntersectionError? = null
        private set
    private var useExternalIndexFunctions = false

    init {
        type = Const.OBJECT_TYPE_INTERSECTION
        elType = INTERSECTION_ELEMENT_TYPE
        isDraggable = false
    }

    // JSXGraph: src/base/point.js -> createIntersectionPoint;
    // src/math/geometry.js -> intersectionFunction.
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        when (val result = coordinatesResult(useExternalIndexFunctions)) {
            is GMResult.Ok -> {
                intersectionEvaluationError = null
                coords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = result.value,
                )
            }
            is GMResult.Err -> {
                intersectionEvaluationError = result.error
                coords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = doubleArrayOf(
                        0.0,
                        Double.NaN,
                        Double.NaN,
                    ),
                )
            }
        }
        return this
    }

    internal fun currentIntersectionNumbers():
        GMResult<DoubleArray, IntersectionError> {
        val first = when (
            val result = evaluateIndex(
                source = firstIndex,
                external = useExternalIndexFunctions,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val second = when (
            val result = evaluateIndex(
                source = secondIndex,
                external = useExternalIndexFunctions,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(doubleArrayOf(first, second))
    }

    private fun coordinatesResult(
        external: Boolean,
    ): GMResult<DoubleArray, IntersectionError> {
        val firstIndexValue = when (
            val result = evaluateIndex(firstIndex, external)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return intersectionCoordinates(
            first = firstElement,
            second = secondElement,
            firstIndex = firstIndexValue,
            firstIndexIsFunction =
                firstIndex is IntersectionIndexSource.Function,
            secondInitialParameter =
                secondIndex.asUnevaluatedInitialParameter(),
            alwaysIntersect = alwaysIntersect,
        )
    }

    internal companion object {
        private const val POINT_ID_PREFIX = "P"
        private const val INTERSECTION_ELEMENT_TYPE = "intersection"

        fun create(
            board: Board,
            first: GeometryElement,
            second: GeometryElement,
            firstIndex: IntersectionIndexSource =
                IntersectionIndexSource.Number(0.0),
            secondIndex: IntersectionIndexSource =
                IntersectionIndexSource.Number(0.0),
            alwaysIntersect: Boolean = true,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<IntersectionPoint, IntersectionError> {
            validateIntersectionParent(board, first, 0)?.let {
                return GMResult.Err(it)
            }
            validateIntersectionParent(board, second, 1)?.let {
                return GMResult.Err(it)
            }
            conicParentIndex(first, second)?.let { parentIndex ->
                return GMResult.Err(
                    IntersectionError.UnsupportedConic(parentIndex),
                )
            }
            if (!isSupportedPair(first, second)) {
                return GMResult.Err(
                    IntersectionError.UnsupportedParentTypes(
                        firstType = first.elType,
                        secondType = second.elType,
                    ),
                )
            }
            for (
                dependency in
                indexDependencies(firstIndex) +
                    indexDependencies(secondIndex)
            ) {
                validateIntersectionParent(
                    board = board,
                    element = dependency,
                    parentIndex = 2,
                    requireSupportedType = false,
                )?.let {
                    return GMResult.Err(it)
                }
            }
            val initialCoordinates = when (
                val result = initialCoordinates(
                    first = first,
                    second = second,
                    firstIndex = firstIndex,
                    secondIndex = secondIndex,
                    alwaysIntersect = alwaysIntersect,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val output = IntersectionPoint(
                board = board,
                firstElement = first,
                secondElement = second,
                firstIndex = firstIndex,
                secondIndex = secondIndex,
                alwaysIntersect = alwaysIntersect,
                initialCoordinates = initialCoordinates,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )
            output.baseElement = output
            return when (
                val registration = board.setId(output, POINT_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    first.addChild(output)
                    second.addChild(output)
                    output.setParents(listOf(first, second))
                    output.useExternalIndexFunctions = true
                    output.update()
                    GMResult.Ok(output)
                }
                is GMResult.Err -> GMResult.Err(
                    IntersectionError.Registration(registration.error),
                )
            }
        }

        private fun initialCoordinates(
            first: GeometryElement,
            second: GeometryElement,
            firstIndex: IntersectionIndexSource,
            secondIndex: IntersectionIndexSource,
            alwaysIntersect: Boolean,
        ): GMResult<DoubleArray, IntersectionError> {
            val firstIndexValue = when (
                val result = evaluateIndex(
                    source = firstIndex,
                    external = false,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return intersectionCoordinates(
                first = first,
                second = second,
                firstIndex = firstIndexValue,
                firstIndexIsFunction =
                    firstIndex is IntersectionIndexSource.Function,
                secondInitialParameter =
                    secondIndex.asUnevaluatedInitialParameter(),
                alwaysIntersect = alwaysIntersect,
            )
        }
    }
}

/**
 * The remaining intersection of a Circle/Circle or Circle/Line pair after
 * excluding one or more existing Points.
 */
internal class OtherIntersectionPoint private constructor(
    board: Board,
    internal val firstElement: GeometryElement,
    internal val secondElement: GeometryElement,
    internal val excludedPoints: List<Point>,
    internal val alwaysIntersect: Boolean,
    internal val precision: Double,
    initialCoordinates: DoubleArray,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    fixed: Boolean,
) : Point(
    board = board,
    coordinates = initialCoordinates,
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
    fixed = fixed,
) {
    internal var intersectionEvaluationError: IntersectionError? = null
        private set

    init {
        type = Const.OBJECT_TYPE_INTERSECTION
        elType = OTHER_INTERSECTION_ELEMENT_TYPE
        isDraggable = false
    }

    // JSXGraph: src/base/point.js -> createOtherIntersectionPoint;
    // src/math/geometry.js -> otherIntersectionFunction.
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        when (
            val result = otherIntersectionCoordinates(
                first = firstElement,
                second = secondElement,
                excludedPoints = excludedPoints,
                alwaysIntersect = alwaysIntersect,
                precision = precision,
            )
        ) {
            is GMResult.Ok -> {
                intersectionEvaluationError = null
                coords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = result.value,
                )
            }
            is GMResult.Err -> {
                intersectionEvaluationError = result.error
                coords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = nonRealCoordinates(),
                )
            }
        }
        return this
    }

    internal companion object {
        private const val POINT_ID_PREFIX = "P"
        private const val OTHER_INTERSECTION_ELEMENT_TYPE =
            "otherintersection"

        fun create(
            board: Board,
            first: GeometryElement,
            second: GeometryElement,
            excludedPoints: List<Point>,
            alwaysIntersect: Boolean = true,
            precision: Double = 0.001,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<OtherIntersectionPoint, IntersectionError> {
            validateIntersectionParent(board, first, 0)?.let {
                return GMResult.Err(it)
            }
            validateIntersectionParent(board, second, 1)?.let {
                return GMResult.Err(it)
            }
            conicParentIndex(first, second)?.let { parentIndex ->
                return GMResult.Err(
                    IntersectionError.UnsupportedConic(parentIndex),
                )
            }
            if (!isSupportedOtherPair(first, second)) {
                return GMResult.Err(
                    IntersectionError.UnsupportedParentTypes(
                        firstType = first.elType,
                        secondType = second.elType,
                    ),
                )
            }
            for ((index, point) in excludedPoints.withIndex()) {
                if (point.board !== board) {
                    return GMResult.Err(
                        IntersectionError.ExcludedPointBoardMismatch(index),
                    )
                }
                if (board.elementById(point.id) !== point) {
                    return GMResult.Err(
                        IntersectionError.ExcludedPointNotRegistered(
                            pointIndex = index,
                            id = point.id,
                        ),
                    )
                }
            }

            val sorted = listOf(first, second)
                .sortedByDescending(GeometryElement::elementClass)
            val initialCoordinates = when (
                val result = otherIntersectionCoordinates(
                    first = sorted[0],
                    second = sorted[1],
                    excludedPoints = excludedPoints,
                    alwaysIntersect = alwaysIntersect,
                    precision = precision,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val output = OtherIntersectionPoint(
                board = board,
                firstElement = sorted[0],
                secondElement = sorted[1],
                excludedPoints = excludedPoints,
                alwaysIntersect = alwaysIntersect,
                precision = precision,
                initialCoordinates = initialCoordinates,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )
            output.baseElement = output
            return when (
                val registration = board.setId(output, POINT_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    first.addChild(output)
                    second.addChild(output)
                    output.setParents(listOf(first, second))
                    output.addParents(excludedPoints)
                    output.update()
                    GMResult.Ok(output)
                }
                is GMResult.Err -> GMResult.Err(
                    IntersectionError.Registration(registration.error),
                )
            }
        }
    }
}

private fun validateIntersectionParent(
    board: Board,
    element: GeometryElement,
    parentIndex: Int,
    requireSupportedType: Boolean = true,
): IntersectionError? {
    if (element.board !== board) {
        return IntersectionError.ParentBoardMismatch(parentIndex)
    }
    if (board.elementById(element.id) !== element) {
        return IntersectionError.ParentNotRegistered(
            parentIndex = parentIndex,
            id = element.id,
        )
    }
    if (
        requireSupportedType &&
        element !is Line &&
        element !is Circle &&
        element !is Curve &&
        element !is Arc &&
        element !is Sector &&
        element !is Polygon
    ) {
        return IntersectionError.UnsupportedParentTypes(
            firstType = element.elType,
            secondType = "",
        )
    }
    return null
}

private fun conicParentIndex(
    first: GeometryElement,
    second: GeometryElement,
): Int? =
    when {
        first.type == Const.OBJECT_TYPE_CONIC -> 0
        second.type == Const.OBJECT_TYPE_CONIC -> 1
        else -> null
    }

private fun isSupportedPair(
    first: GeometryElement,
    second: GeometryElement,
): Boolean {
    val firstIsArc = first.isArcType()
    val secondIsArc = second.isArcType()
    if (
        (first.isCurveClass() || second.isCurveClass()) &&
        first.isCurveOrCircleClass() &&
        second.isCurveOrCircleClass()
    ) {
        return true
    }
    if (
        (
            first.isCurveClass() &&
                !firstIsArc &&
                second is Line
            ) ||
        (
            second.isCurveClass() &&
                !secondIsArc &&
                first is Line
            )
    ) {
        return true
    }
    if (first is Polygon || second is Polygon) {
        val other = if (first is Polygon) second else first
        return other is Line ||
            other is Polygon ||
            other is Circle ||
            other is Curve ||
            other is Arc ||
            other is Sector
    }
    if (first is Line && second is Line) {
        return true
    }
    return first.isAnalyticIntersectionElement() &&
        second.isAnalyticIntersectionElement()
}

private fun isSupportedOtherPair(
    first: GeometryElement,
    second: GeometryElement,
): Boolean {
    val sorted = listOf(first, second)
        .sortedByDescending(GeometryElement::elementClass)
    val firstSorted = sorted[0]
    val secondSorted = sorted[1]
    return (
        firstSorted is Circle &&
            (secondSorted is Circle || secondSorted is Line)
        ) ||
        (
            firstSorted.isCurveClass() &&
                (
                    secondSorted.isCurveClass() ||
                        secondSorted is Circle ||
                        secondSorted is Line
                    )
            )
}

private fun intersectionCoordinates(
    first: GeometryElement,
    second: GeometryElement,
    firstIndex: Double,
    firstIndexIsFunction: Boolean,
    secondInitialParameter: Double,
    alwaysIntersect: Boolean,
): GMResult<DoubleArray, IntersectionError> {
    val firstIsArc = first.isArcType()
    val secondIsArc = second.isArcType()
    if (
        (first.isCurveClass() || second.isCurveClass()) &&
        first.isCurveOrCircleClass() &&
        second.isCurveOrCircleClass()
    ) {
        return meetCurveCurveElements(
            first = first,
            second = second,
            firstIndex = firstIndex,
            secondInitialParameter = secondInitialParameter,
        )
    }
    if (
        first.isCurveClass() &&
        !firstIsArc &&
        second is Line
    ) {
        return meetCurveLineElements(
            curve = first,
            line = second,
            intersectionIndex = firstIndex,
            alwaysIntersect = alwaysIntersect,
        )
    }
    if (
        second.isCurveClass() &&
        !secondIsArc &&
        first is Line
    ) {
        return meetCurveLineElements(
            curve = second,
            line = first,
            intersectionIndex = firstIndex,
            alwaysIntersect = alwaysIntersect,
        )
    }
    if (first is Polygon || second is Polygon) {
        val polygon = (first as? Polygon) ?: (second as Polygon)
        val line = (first as? Line) ?: (second as? Line)
        if (line != null) {
            return GMResult.Ok(
                Geometry.meetPolygonLine(
                    borders = polygon.borders.map { border ->
                        border.point1.coords.usrCoords to
                            border.point2.coords.usrCoords
                    },
                    lineFirst = line.point1.coords.usrCoords,
                    lineSecond = line.point2.coords.usrCoords,
                    intersectionIndex = firstIndex,
                    testLineSegment = !alwaysIntersect,
                ),
            )
        }
        return when (
            val result = Clip.meetPathPath(
                first = first,
                second = second,
                intersectionIndex = firstIndex,
                rawIndexIsFunction = firstIndexIsFunction,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                IntersectionError.ClipComputation(result.error),
            )
        }
    }

    val analyticIndex = if (firstIndex == 0.0) 0 else 1
    if (first is Line && second is Line) {
        if (
            !alwaysIntersect &&
            (
                !first.straightFirst ||
                    !first.straightLast ||
                    !second.straightFirst ||
                    !second.straightLast
            )
        ) {
            val result = Geometry.meetSegmentSegment(
                firstStart = first.point1.coords.usrCoords,
                firstEnd = first.point2.coords.usrCoords,
                secondStart = second.point1.coords.usrCoords,
                secondEnd = second.point2.coords.usrCoords,
            )
            if (
                (!first.straightFirst && result.firstParameter < 0.0) ||
                (!first.straightLast && result.firstParameter > 1.0) ||
                (!second.straightFirst && result.secondParameter < 0.0) ||
                (!second.straightLast && result.secondParameter > 1.0)
            ) {
                return GMResult.Ok(nonRealCoordinates())
            }
            return GMResult.Ok(result.point)
        }
        return GMResult.Ok(
            Geometry.meet(
                firstStandardForm = first.stdform,
                secondStandardForm = second.stdform,
                intersectionIndex = analyticIndex,
            ),
        )
    }

    val result = Geometry.meet(
        firstStandardForm = first.stdform,
        secondStandardForm = second.stdform,
        intersectionIndex = analyticIndex,
    )
    if (alwaysIntersect) {
        return GMResult.Ok(result)
    }
    for (element in listOf(first, second)) {
        val line = element as? Line ?: continue
        if (line.straightFirst && line.straightLast) {
            continue
        }
        val ratio = Geometry.affineRatio(
            first = line.point1.coords.usrCoords,
            second = line.point2.coords.usrCoords,
            third = result,
        )
        if (
            (!line.straightLast && ratio > 1.0 + Mat.eps) ||
            (!line.straightFirst && ratio < -Mat.eps)
        ) {
            return GMResult.Ok(nonRealCoordinates())
        }
    }
    if (firstIsArc) {
        var contains = first.containsArcCoordinates(result)
        if (contains && secondIsArc) {
            contains = second.containsArcCoordinates(result)
        }
        if (!contains) {
            return GMResult.Ok(nonRealCoordinates())
        }
    }
    return GMResult.Ok(result)
}

private fun otherIntersectionCoordinates(
    first: GeometryElement,
    second: GeometryElement,
    excludedPoints: List<Point>,
    alwaysIntersect: Boolean,
    precision: Double,
): GMResult<DoubleArray, IntersectionError> {
    var candidate = doubleArrayOf(0.0, 0.0, 0.0)
    for (index in excludedPoints.size downTo 0) {
        val result = when {
            first is Circle &&
                (second is Circle || second is Line) ->
                GMResult.Ok(
                    Geometry.meet(
                        firstStandardForm = first.stdform,
                        secondStandardForm = second.stdform,
                        intersectionIndex = index,
                    ),
                )

            first.isCurveClass() &&
                (
                    second.isCurveClass() ||
                        second is Circle
                    ) ->
                meetCurveCurveElements(
                    first = first,
                    second = second,
                    firstIndex = index.toDouble(),
                    secondInitialParameter = 0.0,
                )

            first.isCurveClass() && second is Line ->
                meetCurveLineElements(
                    curve = first,
                    line = second,
                    intersectionIndex = index.toDouble(),
                    alwaysIntersect = alwaysIntersect,
                    continuousAlwaysUsesExtendedLine = true,
                )

            else -> continue
        }
        candidate = when (result) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val isClose = excludedPoints.any { point ->
            val candidateWeight = candidate[0]
            val pointWeight = point.Z()
            val weightDifference = candidateWeight - pointWeight
            if (weightDifference * weightDifference > Mat.eps * Mat.eps) {
                false
            } else {
                Mat.hypot(
                    candidate[1] - point.X(),
                    candidate[2] - point.Y(),
                ) < precision
            }
        }
        if (!isClose) {
            return GMResult.Ok(candidate)
        }
    }
    return GMResult.Ok(candidate)
}

private fun meetCurveCurveElements(
    first: GeometryElement,
    second: GeometryElement,
    firstIndex: Double,
    secondInitialParameter: Double,
): GMResult<DoubleArray, IntersectionError> {
    val firstContinuous = (first as? Curve)
        ?.takeIf { it.dataX == null }
    val secondContinuous = (second as? Curve)
        ?.takeIf { it.dataX == null }
    if (firstContinuous != null && secondContinuous != null) {
        return Geometry.meetCurveCurveContinuous(
            first = firstContinuous.toContinuousCurve(),
            second = secondContinuous.toContinuousCurve(),
            intersectionIndex = firstIndex,
            secondInitialParameter = secondInitialParameter,
        ).asIntersectionResult()
    }

    val firstDiscrete = first.toDiscreteCurve()
        ?: return unsupportedIntersection(first, second)
    val secondDiscrete = second.toDiscreteCurve()
        ?: return unsupportedIntersection(first, second)
    return Geometry.meetCurveCurveDiscrete(
        first = firstDiscrete,
        second = secondDiscrete,
        intersectionIndex = firstIndex,
    ).asIntersectionResult()
}

private fun meetCurveLineElements(
    curve: GeometryElement,
    line: Line,
    intersectionIndex: Double,
    alwaysIntersect: Boolean,
    continuousAlwaysUsesExtendedLine: Boolean = false,
): GMResult<DoubleArray, IntersectionError> {
    val discrete = curve.toDiscreteCurve()
        ?: return unsupportedIntersection(curve, line)
    val continuous = (curve as? Curve)
        ?.takeIf { it.dataX == null }
    val testSegment =
        if (continuousAlwaysUsesExtendedLine && continuous != null) {
            false
        } else {
            !alwaysIntersect
        }
    val result = if (continuous != null) {
        Geometry.meetCurveLineContinuous(
            curve = continuous.toContinuousCurve(),
            discreteCurve = discrete,
            lineFirst = line.point1.coords.usrCoords,
            lineSecond = line.point2.coords.usrCoords,
            lineStandardForm = line.stdform,
            straightFirst = line.straightFirst,
            straightLast = line.straightLast,
            intersectionIndex = intersectionIndex,
            testSegment = testSegment,
        )
    } else {
        Geometry.meetCurveLineDiscrete(
            curve = discrete,
            lineFirst = line.point1.coords.usrCoords,
            lineSecond = line.point2.coords.usrCoords,
            lineStandardForm = line.stdform,
            straightFirst = line.straightFirst,
            straightLast = line.straightLast,
            intersectionIndex = intersectionIndex,
            testSegment = testSegment,
        )
    }
    return result.asIntersectionResult()
}

private fun GeometryElement.toDiscreteCurve(): DiscreteCurve2D? =
    when (this) {
        is Curve -> DiscreteCurve2D(
            points = points.map { it.usrCoords.copyOf() },
            bezierDegree = bezierDegree,
        )

        is Circle -> DiscreteCurve2D(
            points = points.map { it.usrCoords.copyOf() },
            bezierDegree = bezierDegree,
        )

        is Arc -> DiscreteCurve2D(
            points = points.map { it.usrCoords.copyOf() },
            bezierDegree = bezierDegree,
        )

        is Sector -> DiscreteCurve2D(
            points = points.map { it.usrCoords.copyOf() },
            bezierDegree = bezierDegree,
            isSector = type == Const.OBJECT_TYPE_SECTOR,
        )

        else -> null
    }

private fun Curve.toContinuousCurve(): ContinuousCurve2D =
    ContinuousCurve2D(
        curve = ParametricCurve2D(
            x = ::X,
            y = ::Y,
        ),
        minimumParameter = minX(),
        maximumParameter = maxX(),
        type =
            if (curveType == "functiongraph") {
                ContinuousCurveType.FUNCTION_GRAPH
            } else {
                ContinuousCurveType.PARAMETER
            },
    )

private fun GeometryElement.containsArcCoordinates(
    coordinates: DoubleArray,
): Boolean {
    val radiusPoint: Point
    val centerPoint: Point
    val anglePoint: Point
    val selectionValue: String
    val orientationValue: String
    when (this) {
        is Arc -> {
            radiusPoint = radiuspoint
            centerPoint = center
            anglePoint = anglepoint
            selectionValue = selection
            orientationValue = orientation
        }

        is Sector -> {
            radiusPoint = radiuspoint
            centerPoint = center
            anglePoint = anglepoint
            selectionValue = selection
            orientationValue = orientation
        }

        else -> return true
    }
    val selection = when (selectionValue) {
        Arc.SELECTION_MINOR ->
            com.swithun.jsxgraph.core.math.ArcSelection.MINOR

        Arc.SELECTION_MAJOR ->
            com.swithun.jsxgraph.core.math.ArcSelection.MAJOR

        else -> com.swithun.jsxgraph.core.math.ArcSelection.AUTO
    }
    val orientation =
        if (orientationValue == Arc.ORIENTATION_CLOCKWISE) {
            com.swithun.jsxgraph.core.math.ArcOrientation.CLOCKWISE
        } else {
            com.swithun.jsxgraph.core.math.ArcOrientation.COUNTERCLOCKWISE
        }
    return Geometry.coordsOnArc(
        radiusPoint = radiusPoint.Coords(),
        center = centerPoint.Coords(),
        anglePoint = anglePoint.Coords(),
        coordinates = coordinates.sliceArray(1..2),
        selection = selection,
        orientation = orientation,
    )
}

private fun GeometryElement.isCurveClass(): Boolean =
    elementClass == Const.OBJECT_CLASS_CURVE

private fun GeometryElement.isCurveOrCircleClass(): Boolean =
    isCurveClass() || elementClass == Const.OBJECT_CLASS_CIRCLE

private fun GeometryElement.isArcType(): Boolean =
    isCurveClass() &&
        (
            type == Const.OBJECT_TYPE_ARC ||
                type == Const.OBJECT_TYPE_SECTOR
            )

private fun GeometryElement.isAnalyticIntersectionElement(): Boolean =
    this is Line ||
        this is Circle ||
        this is Arc ||
        (
            this is Sector &&
                type == Const.OBJECT_TYPE_SECTOR
            )

private fun GMResult<DoubleArray, GeometryError>.asIntersectionResult():
    GMResult<DoubleArray, IntersectionError> =
    when (this) {
        is GMResult.Ok -> this
        is GMResult.Err -> GMResult.Err(
            IntersectionError.GeometryComputation(error),
        )
    }

private fun unsupportedIntersection(
    first: GeometryElement,
    second: GeometryElement,
): GMResult.Err<IntersectionError.UnsupportedParentTypes> =
    GMResult.Err(
        IntersectionError.UnsupportedParentTypes(
            firstType = first.elType,
            secondType = second.elType,
        ),
    )

private fun evaluateIndex(
    source: IntersectionIndexSource,
    external: Boolean,
): GMResult<Double, IntersectionError> =
    when (source) {
        is IntersectionIndexSource.Number -> GMResult.Ok(
            if (source.value.isNaN()) 0.0 else source.value,
        )
        is IntersectionIndexSource.Function -> {
            val callable =
                if (external) {
                    source.value.externalCallable
                } else {
                    source.value.callable
                }
            when (
                val result = callable.call(
                    arguments = emptyList(),
                    location = source.location,
                )
            ) {
                is GMResult.Err -> GMResult.Err(
                    IntersectionError.IndexFunctionEvaluation(result.error),
                )
                is GMResult.Ok -> {
                    val value = result.value
                    if (value is JessieCodeRuntimeValue.NumberValue) {
                        // The parent itself is truthy, so createIntersectionPoint
                        // does not apply `parents[2] || 0`. A function result of
                        // NaN reaches meetLineCircle and selects its nonzero
                        // branch because `NaN === 0` is false.
                        GMResult.Ok(value.value)
                    } else {
                        GMResult.Err(
                            IntersectionError.NonNumericIndexFunction(
                                actualType = runtimeType(value),
                            ),
                        )
                    }
                }
            }
        }
    }

/**
 * JSXGraph: src/base/point.js -> createIntersectionPoint and
 * src/math/geometry.js -> meetCurveCurve.
 *
 * The legacy `t2ini` argument is passed through without `Type.evaluate`.
 * Kotlin cannot store a JavaScript function object in a numeric parameter,
 * so its numeric projection is NaN; importantly, the function is not called.
 */
private fun IntersectionIndexSource.asUnevaluatedInitialParameter(): Double =
    when (this) {
        is IntersectionIndexSource.Number ->
            if (value.isNaN()) 0.0 else value
        is IntersectionIndexSource.Function -> Double.NaN
    }

private fun indexDependencies(
    source: IntersectionIndexSource,
): Collection<GeometryElement> =
    when (source) {
        is IntersectionIndexSource.Number -> emptyList()
        is IntersectionIndexSource.Function ->
            source.value.dependencies.values
    }

private fun nonRealCoordinates(): DoubleArray =
    doubleArrayOf(0.0, Double.NaN, Double.NaN)

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
        is JessieCodeRuntimeValue.CompositionReference -> "composition"
        is JessieCodeRuntimeValue.ElementReference -> "element"
    }
