/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/point.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
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

    data class Registration(val error: BoardError) : PointError
}

/**
 * Initial translated slice of JXG.Point.
 *
 * This slice covers numeric free points, JessieCode string coordinate
 * constraints, board registration, coordinate updates, bounds, and incidence
 * checks against the currently translated point, line, and circle elements,
 * plus transformed-point construction, persistent 2D transformations, and
 * the Line/Segment/Circle Intersection wrappers in [IntersectionPoint] and
 * [OtherIntersectionPoint]. Visual attributes, screen hit testing, traces,
 * slider/Coords-object constraints, gliders, and the remaining intersection
 * parent families remain untranslated.
 */
internal open class Point internal constructor(
    board: Board,
    coordinates: DoubleArray,
    id: String = "",
    name: String? = null,
    needsRegularUpdate: Boolean = true,
    fixed: Boolean = false,
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

    init {
        elType = POINT_ELEMENT_TYPE
    }

    // JSXGraph: src/base/point.js -> update
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        updateCoords(fromParent)
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
            is Line ->
                Geometry.distPointLine(coords.usrCoords, element.stdform) <
                    resolvedTolerance
            is Circle ->
                abs(Dist(element.center) - element.Radius()) <
                    resolvedTolerance
            else -> false
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

        // JSXGraph: src/base/point.js -> createPoint / Point constructor
        fun create(
            board: Board,
            coordinates: DoubleArray,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
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
}
