/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/circle.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal sealed interface CircleError {
    data class ParentBoardMismatch(val parentIndex: Int) : CircleError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : CircleError

    data class RadiusExpressionCompile(
        val error: JessieCodeExpressionCompileError,
    ) : CircleError

    data class RadiusExpressionEvaluation(
        val error: JessieCodeRuntimeError,
    ) : CircleError

    data class NonNumericRadiusExpression(
        val actualType: String,
    ) : CircleError

    data class RadiusFunctionEvaluation(
        val error: JessieCodeRuntimeError,
    ) : CircleError

    data class NonNumericRadiusFunction(
        val actualType: String,
    ) : CircleError

    data class CircumcenterFactory(val error: CircumcenterError) : CircleError

    data class Registration(val error: BoardError) : CircleError
}

internal data class CircleRadiusFunction(
    val function: JessieCodeRuntimeValue.FunctionValue,
    val location: JessieCodeAstLocation,
)

/**
 * Initial translated slice of JXG.Circle.
 *
 * This slice covers circles defined by two or three registered points, a fixed
 * numeric radius, a registered line, or a registered circle. It includes
 * dependency links, standard and quadratic forms, cubic Bezier approximation,
 * numeric queries, and JessieCode string or function radii. Transformations,
 * rendering, and hit testing remain untranslated.
 */
internal open class Circle internal constructor(
    board: Board,
    internal val method: String,
    internal val center: Point,
    internal val point2: Point? = null,
    internal val line: Line? = null,
    internal val circle: Circle? = null,
    private val radiusValue: Double? = null,
    private val nativeRadiusFunction: (() -> Double)? = null,
    internal val updateRadius: JessieCodeExpressionFunction? = null,
    internal val radiusFunction: CircleRadiusFunction? = null,
    internal val nonnegativeOnly: Boolean = false,
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
    internal var radiusEvaluationError: CircleError? = null
        private set
    internal val points = mutableListOf<Coords>()
    internal val subs = linkedMapOf<String, GeometryElement>()
    internal val inherits = mutableListOf<GeometryElement>()

    internal var numberPoints: Int = 0
    internal var dataX = doubleArrayOf()
    internal var dataY = doubleArrayOf()
    internal var bezierDegree: Int = 0
    private var useExternalRadiusFunction = false

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
            POINT_RADIUS_METHOD -> Radius()
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
    internal fun radiusResult(): GMResult<Double, CircleError> =
        when (method) {
            TWO_POINTS_METHOD -> {
                val circumferencePoint = point2
                    ?: return GMResult.Ok(Double.NaN)
                if (
                    circumferencePoint.coords.usrCoords.all { it == 0.0 } ||
                    center.coords.usrCoords.all { it == 0.0 }
                ) {
                    GMResult.Ok(Double.NaN)
                } else {
                    GMResult.Ok(center.Dist(circumferencePoint))
                }
            }

            POINT_RADIUS_METHOD -> {
                val expression = updateRadius
                val function = radiusFunction
                val rawRadius = when {
                    nativeRadiusFunction != null ->
                        GMResult.Ok(nativeRadiusFunction.invoke())
                    expression != null ->
                        evaluateRadiusExpression(expression)
                    function != null ->
                        evaluateRadiusFunction(
                            radiusFunction = function,
                            external = useExternalRadiusFunction,
                        )
                    else -> GMResult.Ok(radiusValue ?: Double.NaN)
                }
                when (rawRadius) {
                    is GMResult.Ok ->
                        GMResult.Ok(normalizePointRadius(rawRadius.value))
                    is GMResult.Err -> rawRadius
                }
            }
            POINT_LINE_METHOD,
            POINT_CIRCLE_METHOD,
            -> GMResult.Ok(radius)

            else -> GMResult.Ok(Double.NaN)
        }

    // JSXGraph: src/base/circle.js -> Radius
    internal fun Radius(): Double =
        when (val result = radiusResult()) {
            is GMResult.Ok -> {
                radiusEvaluationError = null
                result.value
            }
            is GMResult.Err -> {
                radiusEvaluationError = result.error
                Double.NaN
            }
        }

    private fun radiusFromSource(): Double =
        when (method) {
            TWO_POINTS_METHOD -> Radius()
            POINT_RADIUS_METHOD ->
                normalizePointRadius(
                    nativeRadiusFunction?.invoke()
                        ?: radiusValue
                        ?: Double.NaN,
                )
            POINT_LINE_METHOD -> line?.let {
                it.point1.coords.distance(
                    Const.COORDS_BY_USER,
                    it.point2.coords,
                )
            } ?: Double.NaN
            POINT_CIRCLE_METHOD -> circle?.Radius() ?: Double.NaN
            else -> Double.NaN
        }

    // JSXGraph: src/base/circle.js -> Radius pointRadius branch
    private fun normalizePointRadius(value: Double): Double =
        if (nonnegativeOnly) {
            max(0.0, value)
        } else {
            abs(value)
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
        private const val CIRCUMCIRCLE_ELEMENT_TYPE = "circumcircle"
        private const val CENTER_SUB_ELEMENT = "center"
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

        // JSXGraph: src/base/circle.js -> createCircle three-point branch
        fun create(
            board: Board,
            point1: Point,
            point2: Point,
            point3: Point,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Circle, CircleError> {
            return when (
                val result = createCircumcircle(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    point3 = point3,
                    ownedPoints = ownedPoints,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> {
                    val circle = result.value
                    circle.elType = CIRCLE_ELEMENT_TYPE
                    circle.inherits += listOf(point1, point2, point3)
                    GMResult.Ok(circle)
                }
                is GMResult.Err -> result
            }
        }

        // JSXGraph: src/element/composition.js -> createCircumcircle
        fun createCircumcircle(
            board: Board,
            point1: Point,
            point2: Point,
            point3: Point,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Circle, CircleError> {
            val points = listOf(point1, point2, point3)
            for ((index, point) in points.withIndex()) {
                validateParent(board, point, parentIndex = index)?.let {
                    return GMResult.Err(it)
                }
            }
            if (id.isNotEmpty() && board.elementById(id) != null) {
                return GMResult.Err(
                    CircleError.Registration(
                        BoardError.DuplicateElementId(id),
                    ),
                )
            }

            val center = when (
                val result = CircumcenterPoint.create(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    point3 = point3,
                    ownedPoints = ownedPoints,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    CircleError.CircumcenterFactory(result.error),
                )
            }
            return when (
                val result = create(
                    board = board,
                    center = center,
                    point2 = point1,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> {
                    val circle = result.value
                    center.dump = false
                    circle.elType = CIRCUMCIRCLE_ELEMENT_TYPE
                    circle.setParents(points)
                    circle.subs[CENTER_SUB_ELEMENT] = center
                    circle.inherits += circle
                    for (point in points) {
                        point.addChild(circle)
                    }
                    GMResult.Ok(circle)
                }
                is GMResult.Err -> {
                    board.removeObject(center)
                    result
                }
            }
        }

        // JSXGraph: src/base/circle.js -> createCircle pointRadius branch
        fun create(
            board: Board,
            center: Point,
            radius: Double,
            nonnegativeOnly: Boolean = false,
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
                    nonnegativeOnly = nonnegativeOnly,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                dependencies = listOf(center),
            )
        }

        // JSXGraph: src/base/circle.js -> createCircle pointRadius branch.
        // Used by source-mapped internal factories whose radius function does
        // not originate from JessieCode.
        internal fun create(
            board: Board,
            center: Point,
            radiusFunction: () -> Double,
            nonnegativeOnly: Boolean = false,
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
                    nativeRadiusFunction = radiusFunction,
                    nonnegativeOnly = nonnegativeOnly,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                ),
                dependencies = listOf(center),
            )
        }

        // JSXGraph: src/base/circle.js -> pointRadius / Type.createFunction
        fun create(
            board: Board,
            center: Point,
            radiusExpression: String,
            nonnegativeOnly: Boolean = false,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Circle, CircleError> {
            validateParent(board, center, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            val expression = when (
                val result = JessieCodeExpressionFunction.compile(
                    source = radiusExpression,
                    board = board,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    CircleError.RadiusExpressionCompile(result.error),
                )
            }
            val initialRadius = when (
                val result = evaluateRadiusExpression(expression)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val circle = Circle(
                board = board,
                method = POINT_RADIUS_METHOD,
                center = center,
                radiusValue = initialRadius,
                updateRadius = expression,
                nonnegativeOnly = nonnegativeOnly,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val result = register(
                    circle = circle,
                    dependencies = listOf(center),
                )
            ) {
                is GMResult.Ok -> {
                    circle.addParentsFromJCFunctions(listOf(expression))
                    result
                }
                is GMResult.Err -> result
            }
        }

        // JSXGraph: src/base/circle.js -> pointRadius / Type.createFunction
        fun create(
            board: Board,
            center: Point,
            radiusFunction: JessieCodeRuntimeValue.FunctionValue,
            radiusFunctionLocation: JessieCodeAstLocation,
            nonnegativeOnly: Boolean = false,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Circle, CircleError> {
            validateParent(board, center, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            for (dependency in radiusFunction.dependencies.values) {
                validateParent(board, dependency, parentIndex = 1)?.let {
                    return GMResult.Err(it)
                }
            }
            val source = CircleRadiusFunction(
                function = radiusFunction,
                location = radiusFunctionLocation,
            )
            val initialRadius = when (
                val result = evaluateRadiusFunction(
                    radiusFunction = source,
                    external = false,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val circle = Circle(
                board = board,
                method = POINT_RADIUS_METHOD,
                center = center,
                radiusValue = initialRadius,
                radiusFunction = source,
                nonnegativeOnly = nonnegativeOnly,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val result = register(
                    circle = circle,
                    dependencies = listOf(center),
                )
            ) {
                is GMResult.Ok -> {
                    circle.useExternalRadiusFunction = true
                    for (dependency in radiusFunction.dependencies.values) {
                        dependency.addChild(circle)
                    }
                    result
                }
                is GMResult.Err -> result
            }
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
                        if (dependency is Point) {
                            circle.inherits += dependency
                        }
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

        private fun evaluateRadiusExpression(
            expression: JessieCodeExpressionFunction,
        ): GMResult<Double, CircleError> =
            when (val result = expression.evaluate()) {
                is GMResult.Err -> GMResult.Err(
                    CircleError.RadiusExpressionEvaluation(
                        result.error,
                    ),
                )
                is GMResult.Ok -> {
                    val value = result.value
                    if (value is JessieCodeRuntimeValue.NumberValue) {
                        GMResult.Ok(value.value)
                    } else {
                        GMResult.Err(
                            CircleError.NonNumericRadiusExpression(
                                actualType = runtimeType(value),
                            ),
                        )
                    }
                }
            }

        private fun evaluateRadiusFunction(
            radiusFunction: CircleRadiusFunction,
            external: Boolean,
        ): GMResult<Double, CircleError> {
            val callable =
                if (external) {
                    radiusFunction.function.externalCallable
                } else {
                    radiusFunction.function.callable
                }
            return when (
                val result = callable.call(
                    arguments = emptyList(),
                    location = radiusFunction.location,
                )
            ) {
                is GMResult.Err -> GMResult.Err(
                    CircleError.RadiusFunctionEvaluation(result.error),
                )
                is GMResult.Ok -> {
                    val value = result.value
                    if (value is JessieCodeRuntimeValue.NumberValue) {
                        GMResult.Ok(value.value)
                    } else {
                        GMResult.Err(
                            CircleError.NonNumericRadiusFunction(
                                actualType = runtimeType(value),
                            ),
                        )
                    }
                }
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
    }
}
