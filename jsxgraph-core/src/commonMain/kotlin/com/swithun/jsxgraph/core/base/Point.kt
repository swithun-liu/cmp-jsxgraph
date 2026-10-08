/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/point.js
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
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.ParametricCurve2D
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionFunction
import kotlin.math.abs

internal sealed interface PointError {
    data class InvalidCoordinateCount(val count: Int) : PointError

    data class CoordinateExpressionCompile(
        val coordinateIndex: Int,
        val error: JessieCodeExpressionCompileError,
    ) : PointError

    data class CoordinateExpressionEvaluation(
        val error: CoordinateConstraintError,
    ) : PointError

    data class InvalidTransformationCount(
        val count: Int,
    ) : PointError

    data class InvalidStyleIndex(
        val index: Int,
    ) : PointError

    data class IntersectionConversion(
        val error: IntersectionError,
    ) : PointError

    data class Registration(val error: BoardError) : PointError
}

/**
 * Initial translated slice of JXG.Point.
 *
 * This slice covers numeric free points, JessieCode string coordinate
 * constraints, board registration, coordinate updates, bounds, and incidence
 * checks against the translated Point, Line, Segment, Circle, Curve, and
 * Polygon elements, including the static incidence visual flags. It also
 * covers transformed-point construction, persistent 2D transformations, and
 * the Line/Segment/Circle Intersection wrappers in [IntersectionPoint] and
 * [OtherIntersectionPoint], screen hit testing, trace snapshots, face/style
 * mutation, and coordinate-constraint replacement. Remaining specialized
 * visual APIs and intersection parent families stay outside this slice.
 */
internal open class Point internal constructor(
    board: Board,
    coordinates: DoubleArray,
    id: String = "",
    name: String? = null,
    needsRegularUpdate: Boolean = true,
    fixed: Boolean = false,
    internal open val alwaysIntersect: Boolean = false,
    coordinateFunctions: List<JessieCodeCoordinateFunction> = emptyList(),
) : CoordsElement(
    board = board,
    coordinates = coordinates,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_POINT,
    elementClass = Const.OBJECT_CLASS_POINT,
    needsRegularUpdate = needsRegularUpdate,
    coordinateFunctions = coordinateFunctions,
) {
    // JSXGraph: src/options.js -> GeometryElement.fixed
    internal var isFixed: Boolean = fixed
    internal val traceCoordinates = mutableListOf<DoubleArray>()
    internal var convertedIntersectionError: IntersectionError? = null
        private set
    private var convertedIntersection: ConvertedIntersection? = null

    init {
        elType = POINT_ELEMENT_TYPE
    }

    // JSXGraph: src/base/point.js -> update
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        val intersection = convertedIntersection
        if (intersection == null) {
            updateCoords(fromParent)
        } else {
            if (intersection.first.needsUpdate) {
                intersection.first.update(fromParent = true)
            }
            if (intersection.second.needsUpdate) {
                intersection.second.update(fromParent = true)
            }
            when (
                val result = intersectionCoordinates(
                    first = intersection.first,
                    second = intersection.second,
                    firstIndex = intersection.firstIndex,
                    firstIndexIsFunction = false,
                    secondInitialParameter = intersection.secondIndex,
                    alwaysIntersect = alwaysIntersect,
                )
            ) {
                is GMResult.Ok -> {
                    coords.setCoordinates(
                        coordType = Const.COORDS_BY_USER,
                        coordinates = result.value,
                    )
                    convertedIntersectionError = null
                }
                is GMResult.Err -> {
                    coords.setCoordinates(
                        coordType = Const.COORDS_BY_USER,
                        coordinates =
                            doubleArrayOf(0.0, Double.NaN, Double.NaN),
                    )
                    convertedIntersectionError = result.error
                }
            }
        }
        if (getAttribute("trace") == true) {
            cloneToBackground()
        }
        return this
    }

    // JSXGraph: src/base/point.js -> updateTransform
    override fun updateTransform(fromParent: Boolean): CoordsElement {
        if (transformations.isEmpty() || baseElement == null) {
            return this
        }

        when (val result = transformedCoordinatesResult()) {
            is GMResult.Ok -> {
                coords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = result.value,
                )
                clearTransformationEvaluationError()
            }
            is GMResult.Err -> {
                coords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = DoubleArray(3) { Double.NaN },
                )
                setTransformationEvaluationError(result.error)
            }
        }
        return this
    }

    // JSXGraph: src/base/point.js -> bounds
    internal fun bounds(): DoubleArray =
        doubleArrayOf(X(), Y(), X(), Y())

    // JSXGraph 1.13.3: src/base/point.js -> hasPoint.
    internal fun hasPoint(
        screenX: Double,
        screenY: Double,
        precision: Double = DEFAULT_HIT_PRECISION,
    ): Boolean {
        var radius =
            (getAttribute("size") as? Number)?.toDouble()
                ?: DEFAULT_POINT_SIZE
        if (
            (getAttribute("sizeunit") as? String)
                ?.lowercase() == "user"
        ) {
            radius *= kotlin.math.sqrt(abs(board.unitX * board.unitY))
        }
        radius +=
            (
                (getAttribute("strokewidth") as? Number)?.toDouble()
                    ?: DEFAULT_STROKE_WIDTH
                ) * 0.5
        radius = maxOf(radius, precision)
        return abs(coords.scrCoords[1] - screenX) < radius + 2.0 &&
            abs(coords.scrCoords[2] - screenY) < radius + 2.0
    }

    // JSXGraph 1.13.3: src/base/point.js -> updateRenderer.
    override fun updateRenderer(): Point {
        needsUpdate = false
        return this
    }

    // JSXGraph 1.13.3: src/base/point.js -> makeIntersection.
    internal fun makeIntersection(
        first: GeometryElement,
        second: GeometryElement,
        firstIndex: Double = 0.0,
        secondIndex: Double = 0.0,
    ): GMResult<Point, PointError> {
        for ((index, element) in listOf(first, second).withIndex()) {
            if (element.board !== board) {
                return GMResult.Err(
                    PointError.IntersectionConversion(
                        IntersectionError.ParentBoardMismatch(index),
                    ),
                )
            }
            if (board.elementById(element.id) !== element) {
                return GMResult.Err(
                    PointError.IntersectionConversion(
                        IntersectionError.ParentNotRegistered(
                            parentIndex = index,
                            id = element.id,
                        ),
                    ),
                )
            }
        }
        if (!isSupportedIntersectionPair(first, second)) {
            return GMResult.Err(
                PointError.IntersectionConversion(
                    IntersectionError.UnsupportedParentTypes(
                        firstType = first.elType,
                        secondType = second.elType,
                    ),
                ),
            )
        }
        val initial = when (
            val result = intersectionCoordinates(
                first = first,
                second = second,
                firstIndex = firstIndex,
                firstIndexIsFunction = false,
                secondInitialParameter = secondIndex,
                alwaysIntersect = alwaysIntersect,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                PointError.IntersectionConversion(result.error),
            )
        }
        convertedIntersection = ConvertedIntersection(
            first = first,
            second = second,
            firstIndex = firstIndex,
            secondIndex = secondIndex,
        )
        replaceCoordinateFunctions(emptyList())
        first.addChild(this)
        second.addChild(this)
        setParents(listOf(first, second))
        type = Const.OBJECT_TYPE_INTERSECTION
        elType = INTERSECTION_ELEMENT_TYPE
        isDraggable = false
        coords.setCoordinates(Const.COORDS_BY_USER, initial)
        prepareUpdate().update()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/point.js -> setStyle.
    internal fun setStyle(index: Int): GMResult<Point, PointError> {
        if (index !in POINT_STYLE_FACES.indices) {
            return GMResult.Err(PointError.InvalidStyleIndex(index))
        }
        setAttribute(
            mapOf(
                "face" to POINT_STYLE_FACES[index],
                "size" to POINT_STYLE_SIZES[index],
            ),
        )
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/point.js -> normalizeFace.
    internal fun normalizeFace(value: String): String? =
        NORMALIZED_POINT_FACES[value]

    // JSXGraph 1.13.3: src/base/point.js -> face.
    internal fun face(value: String): Point {
        setAttribute(mapOf("face" to value))
        return this
    }

    // JSXGraph 1.13.3: src/base/point.js -> cloneToBackground.
    internal override fun cloneToBackground(): Point {
        traceCoordinates += coords.usrCoords.copyOf()
        return this
    }

    // JSXGraph: src/base/point.js -> isOn
    internal fun isOn(
        element: GeometryElement,
        tolerance: Double = Mat.eps,
    ): Boolean {
        val resolvedTolerance =
            if (tolerance == 0.0 || tolerance.isNaN()) {
                Mat.eps
            } else {
                tolerance
            }

        return when (element) {
            is Point -> Dist(element) < resolvedTolerance
            is Line -> {
                if (
                    element.elType == "segment" &&
                    !alwaysIntersect
                ) {
                    val projection = Geometry.projectCoordsToSegment(
                        point = coords.usrCoords,
                        first = element.point1.coords.usrCoords,
                        second = element.point2.coords.usrCoords,
                    )
                    projection.parameter in 0.0..1.0 &&
                        Geometry.distPointLine(
                            coords.usrCoords,
                            element.stdform,
                        ) < resolvedTolerance
                } else {
                    Geometry.distPointLine(
                        coords.usrCoords,
                        element.stdform,
                    ) < resolvedTolerance
                }
            }
            is Circle -> {
                val centerDistance = Dist(element.center)
                if (element.hasInnerPoints) {
                    centerDistance <
                        element.Radius() + resolvedTolerance
                } else {
                    abs(centerDistance - element.Radius()) <
                        resolvedTolerance
                }
            }
            is Curve -> isOnCurve(element, resolvedTolerance)
            is Polygon -> {
                val vertices = element.vertices.map {
                    it.coords.usrCoords
                }
                if (
                    element.hasInnerPoints &&
                    Geometry.pnpoly(X(), Y(), vertices)
                ) {
                    true
                } else {
                    when (
                        val projection = Geometry.projectCoordsToPolygon(
                            point = coords.usrCoords,
                            vertices = vertices,
                        )
                    ) {
                        is GMResult.Ok ->
                            Geometry.distance(
                                coords.usrCoords,
                                projection.value,
                                3,
                            ) < resolvedTolerance
                        is GMResult.Err -> false
                    }
                }
            }
            else -> false
        }
    }

    // JSXGraph 1.13.3: src/base/point.js -> isOn Curve branch;
    // src/math/geometry.js -> projectPointToCurve/projectCoordsToCurve.
    private fun isOnCurve(
        curve: Curve,
        tolerance: Double,
    ): Boolean {
        if (curve.evaluationError != null) {
            return false
        }
        val projection =
            if (curve.curveType == DATA_CURVE_TYPE) {
                val rawX = curve.dataX
                val rawY = curve.dataY
                val curvePoints =
                    if (rawX != null) {
                        List(minOf(curve.numberPoints, rawX.size)) { index ->
                            doubleArrayOf(
                                1.0,
                                rawX[index],
                                rawY?.getOrNull(index) ?: Double.NaN,
                            )
                        }
                    } else {
                        curve.points
                            .take(curve.numberPoints)
                            .map { it.usrCoords.copyOf() }
                    }
                Geometry.projectCoordsToCurve(
                    point = coords.usrCoords,
                    curve = DiscreteCurve2D(
                        points = curvePoints,
                        bezierDegree = curve.bezierDegree,
                    ),
                )
            } else {
                Geometry.projectCoordsToCurve(
                    horizontal = X(),
                    vertical = Y(),
                    initialParameter =
                        position ?: if (
                            curve.curveType ==
                            FUNCTION_GRAPH_CURVE_TYPE
                        ) {
                            X()
                        } else {
                            0.0
                        },
                    continuousCurve = ContinuousCurve2D(
                        curve = ParametricCurve2D(
                            x = curve::X,
                            y = curve::Y,
                        ),
                        minimumParameter = curve.minX(),
                        maximumParameter = curve.maxX(),
                        type =
                            if (
                                curve.curveType ==
                                FUNCTION_GRAPH_CURVE_TYPE
                            ) {
                                ContinuousCurveType.FUNCTION_GRAPH
                            } else {
                                ContinuousCurveType.PARAMETER
                            },
                    ),
                )
            }
        return when (projection) {
            is GMResult.Ok -> {
                val projectedCoordinates =
                    if (curve.transformations.isEmpty()) {
                        projection.value.point
                    } else {
                        curve.updateTransform(
                            Coords(
                                method = Const.COORDS_BY_USER,
                                coordinates = projection.value.point,
                                board = board,
                                emitter = false,
                            ),
                        ).usrCoords
                    }
                Geometry.distance(
                    coords.usrCoords,
                    projectedCoordinates,
                    3,
                ) < tolerance
            }
            is GMResult.Err -> false
        }
    }

    // JSXGraph: src/base/coordselement.js -> addConstraint
    internal fun replaceCoordinateConstraints(
        coordinateExpressions: List<String>,
    ): GMResult<Point, PointError> {
        if (coordinateExpressions.size < 2) {
            return GMResult.Err(
                PointError.InvalidCoordinateCount(
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
        val coordinates = when (
            val result = coordinateConstraintResult(functions)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                PointError.CoordinateExpressionEvaluation(result.error),
            )
        }

        replaceCoordinateFunctions(functions)
        type = Const.OBJECT_TYPE_CAS
        if (coordinateExpressions.size == 2) {
            Xjc = coordinateExpressions[0]
            Yjc = coordinateExpressions[1]
        } else {
            Xjc = null
            Yjc = null
        }
        applyCoordinateConstraint(coordinates)
        prepareUpdate().update()
        return GMResult.Ok(this)
    }

    internal companion object {
        private const val POINT_ID_PREFIX = "P"
        private const val POINT_ELEMENT_TYPE = "point"
        private const val DATA_CURVE_TYPE = "plot"
        private const val FUNCTION_GRAPH_CURVE_TYPE = "functiongraph"
        private const val INTERSECTION_ELEMENT_TYPE = "intersection"
        private const val DEFAULT_HIT_PRECISION = 4.0
        private const val DEFAULT_POINT_SIZE = 3.0
        private const val DEFAULT_STROKE_WIDTH = 2.0
        private val POINT_STYLE_FACES = listOf(
            "cross",
            "cross",
            "cross",
            "circle",
            "circle",
            "circle",
            "circle",
            "square",
            "square",
            "square",
            "plus",
            "plus",
            "plus",
        )
        private val POINT_STYLE_SIZES =
            listOf(2, 3, 4, 1, 2, 3, 4, 2, 3, 4, 2, 3, 4)
        private val NORMALIZED_POINT_FACES = mapOf(
            "cross" to "x",
            "x" to "x",
            "circle" to "o",
            "o" to "o",
            "square" to "[]",
            "[]" to "[]",
            "plus" to "+",
            "+" to "+",
            "divide" to "|",
            "|" to "|",
            "minus" to "-",
            "-" to "-",
            "diamond" to "<>",
            "<>" to "<>",
            "diamond2" to "<<>>",
            "<<>>" to "<<>>",
            "triangleup" to "^",
            "A" to "^",
            "a" to "^",
            "^" to "^",
            "triangledown" to "v",
            "v" to "v",
            "triangleleft" to "<",
            "<" to "<",
            "triangleright" to ">",
            ">" to ">",
        )

        // JSXGraph: src/base/point.js -> createPoint / Point constructor
        fun create(
            board: Board,
            coordinates: DoubleArray,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
            alwaysIntersect: Boolean = false,
        ): GMResult<Point, PointError> {
            if (coordinates.size < 2) {
                return GMResult.Err(
                    PointError.InvalidCoordinateCount(coordinates.size),
                )
            }

            val point = Point(
                board = board,
                coordinates = coordinates,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
                alwaysIntersect = alwaysIntersect,
            )
            point.baseElement = point
            return when (val registration = board.setId(point, POINT_ID_PREFIX)) {
                is GMResult.Ok -> {
                    point.handleSnapToGrid()
                    point.handleSnapToPoints()
                    point.handleAttractors()
                    GMResult.Ok(point)
                }

                is GMResult.Err -> GMResult.Err(
                    PointError.Registration(registration.error),
                )
            }
        }

        // JSXGraph: src/base/coordselement.js -> create / addConstraint
        fun create(
            board: Board,
            coordinateExpressions: List<String>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
            alwaysIntersect: Boolean = false,
        ): GMResult<Point, PointError> {
            if (coordinateExpressions.size < 2) {
                return GMResult.Err(
                    PointError.InvalidCoordinateCount(
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

            return createConstrained(
                board = board,
                coordinateFunctions = functions,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
                alwaysIntersect = alwaysIntersect,
                xjc = coordinateExpressions
                    .takeIf { it.size == 2 }
                    ?.get(0),
                yjc = coordinateExpressions
                    .takeIf { it.size == 2 }
                    ?.get(1),
            )
        }

        // JSXGraph: src/base/coordselement.js -> create / addConstraint
        fun createConstrained(
            board: Board,
            coordinateFunctions: List<JessieCodeCoordinateFunction>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
            alwaysIntersect: Boolean = false,
            xjc: String? = null,
            yjc: String? = null,
        ): GMResult<Point, PointError> {
            if (coordinateFunctions.isEmpty()) {
                return GMResult.Err(
                    PointError.InvalidCoordinateCount(0),
                )
            }

            val point = Point(
                board = board,
                coordinates =
                    if (coordinateFunctions.size == 2) {
                        doubleArrayOf(0.0, 0.0)
                    } else {
                        doubleArrayOf(1.0, 0.0, 0.0)
                    },
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
                alwaysIntersect = alwaysIntersect,
                coordinateFunctions = coordinateFunctions,
            )
            val initialCoordinates = when (
                val result = point.coordinateConstraintResult()
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    PointError.CoordinateExpressionEvaluation(
                        result.error,
                    ),
                )
            }

            return when (
                val registration = board.setId(
                    point,
                    POINT_ID_PREFIX,
                )
            ) {
                is GMResult.Ok -> {
                    point.type = Const.OBJECT_TYPE_CAS
                    point.Xjc = xjc
                    point.Yjc = yjc
                    point.applyCoordinateConstraint(initialCoordinates)
                    point.addParentsFromJCFunctions(coordinateFunctions)
                    point.handleSnapToGrid()
                    point.handleSnapToPoints()
                    point.handleAttractors()
                    GMResult.Ok(point)
                }
                is GMResult.Err -> GMResult.Err(
                    PointError.Registration(registration.error),
                )
            }
        }

        // JSXGraph: src/base/point.js -> createPoint;
        // src/base/coordselement.js -> CoordsElement.create transformation form
        fun create(
            board: Board,
            basePoint: CoordsElement,
            transformations: List<Transformation>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
            alwaysIntersect: Boolean = false,
        ): GMResult<Point, PointError> {
            if (transformations.isEmpty()) {
                return GMResult.Err(
                    PointError.InvalidTransformationCount(0),
                )
            }

            val point = Point(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
                alwaysIntersect = alwaysIntersect,
            )
            point.addTransform(basePoint, transformations)
            point.isDraggable = false
            return when (val registration = board.setId(point, POINT_ID_PREFIX)) {
                is GMResult.Ok -> {
                    point.addParents(listOf(basePoint))
                    point.handleSnapToGrid()
                    point.handleSnapToPoints()
                    point.handleAttractors()
                    GMResult.Ok(point)
                }
                is GMResult.Err -> GMResult.Err(
                    PointError.Registration(registration.error),
                )
            }
        }

        private fun compileCoordinateFunctions(
            board: Board,
            coordinateExpressions: List<String>,
        ): GMResult<List<JessieCodeExpressionFunction>, PointError> {
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
                        PointError.CoordinateExpressionCompile(
                            coordinateIndex = index,
                            error = result.error,
                        ),
                    )
                }
            }
            return GMResult.Ok(functions)
        }
    }

    private data class ConvertedIntersection(
        val first: GeometryElement,
        val second: GeometryElement,
        val firstIndex: Double,
        val secondIndex: Double,
    )
}
