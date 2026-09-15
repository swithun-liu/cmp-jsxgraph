/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/circle.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal sealed interface CircleError {
    data class ParentBoardMismatch(val parentIndex: Int) : CircleError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : CircleError

    data class Registration(val error: BoardError) : CircleError
}

/**
 * Initial translated slice of JXG.Circle.
 *
 * This slice covers circles defined by two registered points, a fixed numeric
 * radius, a registered line, or a registered circle. It includes dependency
 * links, standard and quadratic forms, cubic Bezier approximation, and numeric
 * queries. Function/string radii, transformations, rendering, and hit testing
 * remain untranslated.
 */
internal open class Circle internal constructor(
    board: Board,
    internal val method: String,
    internal val center: Point,
    internal val point2: Point? = null,
    internal val line: Line? = null,
    internal val circle: Circle? = null,
    private val radiusValue: Double? = null,
    id: String = "",
    name: String? = null,
    needsRegularUpdate: Boolean = true,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_CIRCLE,
    elementClass = Const.OBJECT_CLASS_CIRCLE,
    needsRegularUpdate = needsRegularUpdate,
) {
    // JSXGraph: src/base/circle.js -> midpoint / radius / points
    internal val midpoint: Point = center
    internal var radius: Double = 0.0
    internal val points = mutableListOf<Coords>()

    internal var numberPoints: Int = 0
    internal var dataX = doubleArrayOf()
    internal var dataY = doubleArrayOf()
    internal var bezierDegree: Int = 0

    init {
        radius = radiusFromSource()
        elType = CIRCLE_ELEMENT_TYPE
    }

    // JSXGraph: src/base/circle.js -> update
    override fun update(fromParent: Boolean): Circle {
        if (!needsUpdate) {
            return this
        }

        radius = when (method) {
            POINT_RADIUS_METHOD -> radiusValue ?: Double.NaN
            POINT_LINE_METHOD -> line?.let {
                it.point1.coords.distance(
                    Const.COORDS_BY_USER,
                    it.point2.coords,
                )
            } ?: Double.NaN
            POINT_CIRCLE_METHOD -> circle?.Radius() ?: Double.NaN
            else -> radius
        }
        radius = abs(radius)
        updateStdform()
        updateQuadraticform()
        updateBezierApproximation()
        return this
    }

    // JSXGraph: src/base/circle.js -> updateQuadraticform
    internal fun updateQuadraticform(): Circle {
        val centerX = center.X()
        val centerY = center.Y()
        val currentRadius = Radius()
        quadraticform = arrayOf(
            doubleArrayOf(
                centerX * centerX + centerY * centerY - currentRadius * currentRadius,
                -centerX,
                -centerY,
            ),
            doubleArrayOf(-centerX, 1.0, 0.0),
            doubleArrayOf(-centerY, 0.0, 1.0),
        )
        return this
    }

    // JSXGraph: src/base/circle.js -> updateStdform
    internal fun updateStdform(): Circle {
        stdform[3] = 0.5
        stdform[4] = Radius()
        stdform[1] = -center.coords.usrCoords[1]
        stdform[2] = -center.coords.usrCoords[2]
        if (!stdform[4].isFinite()) {
            stdform[0] = point2?.let {
                -(
                    stdform[1] * it.coords.usrCoords[1] +
                        stdform[2] * it.coords.usrCoords[2]
                )
            } ?: 0.0
        }
        normalize()
        return this
    }

    // JSXGraph: src/base/circle.js -> Radius
    internal fun Radius(): Double =
        when (method) {
            TWO_POINTS_METHOD -> {
                val circumferencePoint = point2
                    ?: return Double.NaN
                if (
                    circumferencePoint.coords.usrCoords.all { it == 0.0 } ||
                    center.coords.usrCoords.all { it == 0.0 }
                ) {
                    Double.NaN
                } else {
                    center.Dist(circumferencePoint)
                }
            }

            POINT_RADIUS_METHOD -> abs(radiusValue ?: Double.NaN)
            POINT_LINE_METHOD,
            POINT_CIRCLE_METHOD,
            -> radius

            else -> Double.NaN
        }

    private fun radiusFromSource(): Double =
        when (method) {
            TWO_POINTS_METHOD -> Radius()
            POINT_RADIUS_METHOD -> radiusValue ?: Double.NaN
            POINT_LINE_METHOD -> line?.let {
                it.point1.coords.distance(
                    Const.COORDS_BY_USER,
                    it.point2.coords,
                )
            } ?: Double.NaN
            POINT_CIRCLE_METHOD -> circle?.Radius() ?: Double.NaN
            else -> Double.NaN
        }

    // JSXGraph: src/base/circle.js -> Diameter
    internal fun Diameter(): Double = 2.0 * Radius()

    // JSXGraph: src/base/circle.js -> X
    internal fun X(t: Double): Double =
        Radius() * cos(t * 2.0 * PI) + center.coords.usrCoords[1]

    // JSXGraph: src/base/circle.js -> Y
    internal fun Y(t: Double): Double =
        Radius() * sin(t * 2.0 * PI) + center.coords.usrCoords[2]

    // JSXGraph: src/base/circle.js -> Z
    @Suppress("UNUSED_PARAMETER")
    internal fun Z(t: Double): Double = 1.0

    // JSXGraph: src/base/circle.js -> minX / maxX
    internal fun minX(): Double = 0.0

    internal fun maxX(): Double = 1.0

    // JSXGraph: src/base/circle.js -> Area
    internal fun Area(): Double {
        val currentRadius = Radius()
        return currentRadius * currentRadius * PI
    }

    // JSXGraph: src/base/circle.js -> Perimeter
    internal fun Perimeter(): Double = 2.0 * Radius() * PI

    // JSXGraph: src/base/circle.js -> bounds
    internal fun bounds(): DoubleArray {
        val coordinates = center.coords.usrCoords
        val currentRadius = Radius()
        return doubleArrayOf(
            coordinates[1] - currentRadius,
            coordinates[2] + currentRadius,
            coordinates[1] + currentRadius,
            coordinates[2] - currentRadius,
        )
    }

    private fun updateBezierApproximation() {
        val homogeneous = center.coords.usrCoords[0]
        val centerX = center.coords.usrCoords[1] / homogeneous
        val centerY = center.coords.usrCoords[2] / homogeneous
        val currentRadius = Radius()
        val control = BEZIER_CONTROL

        numberPoints = 13
        dataX = doubleArrayOf(
            centerX + currentRadius,
            centerX + currentRadius,
            centerX + currentRadius * control,
            centerX,
            centerX - currentRadius * control,
            centerX - currentRadius,
            centerX - currentRadius,
            centerX - currentRadius,
            centerX - currentRadius * control,
            centerX,
            centerX + currentRadius * control,
            centerX + currentRadius,
            centerX + currentRadius,
        )
        dataY = doubleArrayOf(
            centerY,
            centerY + currentRadius * control,
            centerY + currentRadius,
            centerY + currentRadius,
            centerY + currentRadius,
            centerY + currentRadius * control,
            centerY,
            centerY - currentRadius * control,
            centerY - currentRadius,
            centerY - currentRadius,
            centerY - currentRadius,
            centerY - currentRadius * control,
            centerY,
        )
        bezierDegree = 3
        points.clear()
        for (index in 0 until numberPoints) {
            points += Coords(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(dataX[index], dataY[index]),
                board = board,
            )
        }
    }

    internal companion object {
        private const val CIRCLE_ID_PREFIX = "C"
        private const val CIRCLE_ELEMENT_TYPE = "circle"
        private const val TWO_POINTS_METHOD = "twoPoints"
        private const val POINT_RADIUS_METHOD = "pointRadius"
        private const val POINT_LINE_METHOD = "pointLine"
        private const val POINT_CIRCLE_METHOD = "pointCircle"
        private const val BEZIER_CONTROL = 0.551915024494

        // JSXGraph: src/base/circle.js -> createCircle / Circle constructor
        fun create(
            board: Board,
            center: Point,
            point2: Point,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Circle, CircleError> {
            validateParent(board, center, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParent(board, point2, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }

            val circle = Circle(
                board = board,
                method = TWO_POINTS_METHOD,
                center = center,
                point2 = point2,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return register(
                circle = circle,
                dependencies = listOf(center, point2),
            )
        }

        // JSXGraph: src/base/circle.js -> createCircle pointRadius branch
        fun create(
            board: Board,
            center: Point,
            radius: Double,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Circle, CircleError> {
            validateParent(board, center, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }

            return register(
                circle = Circle(
                    board = board,
                    method = POINT_RADIUS_METHOD,
                    center = center,
                    radiusValue = radius,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                dependencies = listOf(center),
            )
        }

        // JSXGraph: src/base/circle.js -> createCircle pointLine branch
        fun create(
            board: Board,
            center: Point,
            radiusLine: Line,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Circle, CircleError> {
            validateParent(board, center, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParent(board, radiusLine, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }

            return register(
                circle = Circle(
                    board = board,
                    method = POINT_LINE_METHOD,
                    center = center,
                    line = radiusLine,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                dependencies = listOf(center, radiusLine),
            )
        }

        // JSXGraph: src/base/circle.js -> createCircle pointCircle branch
        fun create(
            board: Board,
            center: Point,
            radiusCircle: Circle,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Circle, CircleError> {
            validateParent(board, center, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParent(board, radiusCircle, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }

            return register(
                circle = Circle(
                    board = board,
                    method = POINT_CIRCLE_METHOD,
                    center = center,
                    circle = radiusCircle,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                dependencies = listOf(center, radiusCircle),
            )
        }

        private fun register(
            circle: Circle,
            dependencies: List<GeometryElement>,
        ): GMResult<Circle, CircleError> =
            when (val registration = circle.board.setId(circle, CIRCLE_ID_PREFIX)) {
                is GMResult.Ok -> {
                    for (dependency in dependencies) {
                        dependency.addChild(circle)
                    }
                    circle.setParents(dependencies)
                    circle.isDraggable = true
                    circle.update()
                    GMResult.Ok(circle)
                }

                is GMResult.Err -> GMResult.Err(
                    CircleError.Registration(registration.error),
                )
            }

        private fun validateParent(
            board: Board,
            element: GeometryElement,
            parentIndex: Int,
        ): CircleError? {
            if (element.board !== board) {
                return CircleError.ParentBoardMismatch(parentIndex)
            }
            if (board.elementById(element.id) !== element) {
                return CircleError.ParentNotRegistered(parentIndex, element.id)
            }
            return null
        }
    }
}
