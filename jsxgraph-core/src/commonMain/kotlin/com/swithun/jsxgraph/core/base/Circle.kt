/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/circle.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.parser.GeonextParser
import com.swithun.jsxgraph.core.parser.GeonextParserError
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt
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

    data class DependencyParse(
        val error: GeonextParserError,
    ) : CircleError

    data class Registration(val error: BoardError) : CircleError
}

internal data class CircleRadiusFunction(
    val function: JessieCodeRuntimeValue.FunctionValue,
    val location: JessieCodeAstLocation,
)

internal data class CircleTraceSnapshot(
    val center: DoubleArray,
    val radius: Double,
)

/**
 * Translated JXG.Circle lifecycle.
 *
 * This implementation covers circles defined by two or three registered
 * points, a fixed
 * numeric radius, a registered line, or a registered circle. It includes
 * dependency links, standard and quadratic forms, cubic Bezier approximation,
 * numeric queries, JessieCode string or function radii, and static
 * `hasInnerPoints` incidence semantics. Transformations, rendering, screen
 * hit testing, labels, traces, snapping, and mutable radii are translated.
 */
internal open class Circle internal constructor(
    board: Board,
    internal val method: String,
    internal val center: Point,
    internal val point2: Point? = null,
    internal val line: Line? = null,
    internal val circle: Circle? = null,
    private var radiusValue: Double? = null,
    private var nativeRadiusFunction: (() -> Double)? = null,
    internal var updateRadius: JessieCodeExpressionFunction? = null,
    internal var radiusFunction: CircleRadiusFunction? = null,
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
    // JSXGraph 1.13.3: src/options.js -> Circle.hasInnerPoints.
    internal var hasInnerPoints: Boolean = false
    internal var radiusEvaluationError: CircleError? = null
        private set
    internal val points = mutableListOf<Coords>()
    internal val subs = linkedMapOf<String, GeometryElement>()
    internal override val inherits = mutableListOf<GeometryElement>()
    internal val traceSnapshots = mutableListOf<CircleTraceSnapshot>()

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
        if (getAttribute("trace") == true) {
            cloneToBackground()
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/circle.js -> updateRenderer.
    override fun updateRenderer(): Circle {
        needsUpdate = false
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
                val nativeFunction = nativeRadiusFunction
                val rawRadius = when {
                    nativeFunction != null ->
                        GMResult.Ok(nativeFunction.invoke())
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

    // JSXGraph 1.13.3: src/base/circle.js -> setRadius / Radius(value).
    internal fun setRadius(value: Double): Circle {
        radiusValue = value
        nativeRadiusFunction = null
        updateRadius = null
        radiusFunction = null
        useExternalRadiusFunction = false
        radius = normalizePointRadius(value)
        prepareUpdate()
        board.update()
        return this
    }

    // JSXGraph 1.13.3: src/base/board.js -> twoFingerTouchCircle,
    // pointRadius branch guarded by updateRadius.origin being numeric.
    internal fun scaleStaticRadiusForTwoFinger(
        factor: Double,
    ): Boolean {
        if (
            method != POINT_RADIUS_METHOD ||
            radiusValue == null ||
            nativeRadiusFunction != null ||
            updateRadius != null ||
            radiusFunction != null
        ) {
            return false
        }
        setRadius(radius * factor)
        return true
    }

    // JSXGraph 1.13.3: src/base/circle.js -> setRadius / Radius(value).
    internal fun setRadius(
        expression: String,
    ): GMResult<Circle, CircleError> {
        val compiled = when (
            val result = JessieCodeExpressionFunction.compile(
                source = expression,
                board = board,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                CircleError.RadiusExpressionCompile(result.error),
            )
        }
        val initial = when (
            val result = evaluateRadiusExpression(compiled)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        radiusValue = initial
        nativeRadiusFunction = null
        updateRadius = compiled
        radiusFunction = null
        useExternalRadiusFunction = false
        radius = normalizePointRadius(initial)
        addParentsFromJCFunctions(listOf(compiled))
        prepareUpdate()
        board.update()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/circle.js -> setRadius / Radius(value).
    internal fun setRadius(
        function: CircleRadiusFunction,
    ): GMResult<Circle, CircleError> {
        val initial = when (
            val result = evaluateRadiusFunction(
                radiusFunction = function,
                external = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        radiusValue = initial
        nativeRadiusFunction = null
        updateRadius = null
        radiusFunction = function
        useExternalRadiusFunction = true
        radius = normalizePointRadius(initial)
        for (dependency in function.function.dependencies.values) {
            dependency.addChild(this)
        }
        prepareUpdate()
        board.update()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/circle.js -> getRadius.
    // Kotlin's radius property already owns the JVM getRadius signature.
    internal fun legacyRadius(): Double = Radius()

    // JSXGraph 1.13.3: src/base/circle.js -> hasPoint.
    internal fun hasPoint(
        screenX: Double,
        screenY: Double,
        precision: Double = DEFAULT_HIT_PRECISION,
    ): Boolean {
        val point = Coords(
            method = Const.COORDS_BY_SCREEN,
            coordinates = doubleArrayOf(screenX, screenY),
            board = board,
            emitter = false,
        )
        val deltaX = center.coords.usrCoords[1] - point.usrCoords[1]
        val deltaY = center.coords.usrCoords[2] - point.usrCoords[2]
        val distance = Mat.hypot(deltaX, deltaY)
        val strokeWidth =
            (getAttribute("strokewidth") as? Number)?.toDouble() ?: 1.0
        val userTolerance =
            (precision + strokeWidth * 0.5) /
                sqrt(abs(board.unitX * board.unitY))
        return if (hasInnerPoints) {
            distance < Radius() + userTolerance
        } else {
            abs(distance - Radius()) < userTolerance
        }
    }

    // JSXGraph 1.13.3: src/base/circle.js -> generatePolynomial.
    override fun generatePolynomial(
        point: Point?,
    ): List<String> {
        val glider = point ?: return emptyList()
        val centerX = center.symbolic["x"].orEmpty()
        val centerY = center.symbolic["y"].orEmpty()
        val gliderX = glider.symbolic["x"].orEmpty()
        val gliderY = glider.symbolic["y"].orEmpty()
        val radiusSquared = generateRadiusSquared()
        if (radiusSquared.isEmpty()) {
            return emptyList()
        }
        return listOf(
            "(($gliderX)-($centerX))^2 + " +
                "(($gliderY)-($centerY))^2 - ($radiusSquared)",
        )
    }

    // JSXGraph 1.13.3: src/base/circle.js -> generateRadiusSquared.
    internal fun generateRadiusSquared(): String {
        return when (method) {
            TWO_POINTS_METHOD -> {
                val circumferencePoint = point2 ?: return ""
                val centerX = center.symbolic["x"].orEmpty()
                val centerY = center.symbolic["y"].orEmpty()
                val pointX = circumferencePoint.symbolic["x"].orEmpty()
                val pointY = circumferencePoint.symbolic["y"].orEmpty()
                "(($pointX)-($centerX))^2 + " +
                    "(($pointY)-($centerY))^2"
            }
            POINT_RADIUS_METHOD -> {
                if (
                    nativeRadiusFunction != null ||
                    updateRadius != null ||
                    radiusFunction != null
                ) {
                    ""
                } else {
                    radiusValue?.let {
                        JsNumberFormat.compact(it * it)
                    }.orEmpty()
                }
            }
            POINT_LINE_METHOD -> {
                val radiusLine = line ?: return ""
                val point1X = radiusLine.point1.symbolic["x"].orEmpty()
                val point1Y = radiusLine.point1.symbolic["y"].orEmpty()
                val point2X = radiusLine.point2.symbolic["x"].orEmpty()
                val point2Y = radiusLine.point2.symbolic["y"].orEmpty()
                "(($point1X)-($point2X))^2 + " +
                    "(($point1Y)-($point2Y))^2"
            }
            POINT_CIRCLE_METHOD ->
                circle?.Radius()?.let(JsNumberFormat::compact).orEmpty()
            else -> ""
        }
    }

    // JSXGraph 1.13.3: src/base/circle.js -> getTextAnchor.
    internal override fun getTextAnchor(): Coords = center.coords

    // JSXGraph 1.13.3: src/base/circle.js -> getLabelAnchor.
    internal override fun getLabelAnchor(): Coords =
        getLabelAnchor(
            position =
                (elementLabel?.evalVisProp("position") as? String)
                    ?: (evalVisProp("label.position") as? String)
                    ?: "ulft",
        )

    internal fun getLabelAnchor(
        position: String,
    ): Coords {
        val currentRadius = Radius()
        val centerCoordinates = center.coords.usrCoords
        val sqrtHalf = 7.071067811865e-1
        val coordinates = when (position) {
            "lft" -> doubleArrayOf(
                centerCoordinates[1] - currentRadius,
                centerCoordinates[2],
            )
            "llft" -> doubleArrayOf(
                centerCoordinates[1] - sqrtHalf * currentRadius,
                centerCoordinates[2] - sqrtHalf * currentRadius,
            )
            "rt" -> doubleArrayOf(
                centerCoordinates[1] + currentRadius,
                centerCoordinates[2],
            )
            "lrt" -> doubleArrayOf(
                centerCoordinates[1] + sqrtHalf * currentRadius,
                centerCoordinates[2] - sqrtHalf * currentRadius,
            )
            "urt" -> doubleArrayOf(
                centerCoordinates[1] + sqrtHalf * currentRadius,
                centerCoordinates[2] + sqrtHalf * currentRadius,
            )
            "top" -> doubleArrayOf(
                centerCoordinates[1],
                centerCoordinates[2] + currentRadius,
            )
            "bot" -> doubleArrayOf(
                centerCoordinates[1],
                centerCoordinates[2] - currentRadius,
            )
            else -> doubleArrayOf(
                centerCoordinates[1] - sqrtHalf * currentRadius,
                centerCoordinates[2] + sqrtHalf * currentRadius,
            )
        }
        return Coords(
            method = Const.COORDS_BY_USER,
            coordinates = coordinates,
            board = board,
        )
    }

    // JSXGraph 1.13.3: src/base/circle.js -> addTransform.
    internal fun addTransform(
        transformation: Transformation,
    ): Circle = addTransform(listOf(transformation))

    internal fun addTransform(
        values: Iterable<Transformation>,
    ): Circle {
        val additions = values.toList()
        center.transformations += additions
        if (method == TWO_POINTS_METHOD) {
            point2?.transformations?.addAll(additions)
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/circle.js -> removeTransform.
    internal fun removeTransform(
        transformation: Transformation,
    ): Circle = removeTransform(listOf(transformation))

    internal fun removeTransform(
        values: Iterable<Transformation>,
    ): Circle {
        for (transformation in values) {
            center.transformations.remove(transformation)
            if (method == TWO_POINTS_METHOD) {
                point2?.transformations?.remove(transformation)
            }
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/circle.js -> clearTransforms.
    internal fun clearTransforms(): Circle {
        center.transformations.clear()
        if (method == TWO_POINTS_METHOD) {
            point2?.transformations?.clear()
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/circle.js -> snapToGrid.
    internal override fun snapToGrid(): Circle {
        center.handleSnapToGrid()
        if (method == TWO_POINTS_METHOD) {
            point2?.handleSnapToGrid()
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/circle.js -> snapToPoints.
    internal override fun snapToPoints(): Circle {
        center.handleSnapToPoints()
        if (method == TWO_POINTS_METHOD) {
            point2?.handleSnapToPoints()
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/circle.js -> getParents.
    internal fun parentData(): List<Any?> =
        if (parents.size == 1) {
            parents + radius
        } else {
            parents.toList()
        }

    internal override fun dumpParents():
        GMResult<List<Any?>, GeometryElementDumpError> =
        GMResult.Ok(parentData())

    internal override fun dumpSubElements():
        Map<String, GeometryElement> = subs

    // JSXGraph 1.13.3: src/base/circle.js -> notifyParents.
    internal fun notifyParents(
        content: String,
    ): GMResult<Circle, CircleError> =
        when (
            val result = GeonextParser.findDependencies(
                dependent = this,
                source = content,
                board = board,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(this)
            is GMResult.Err -> GMResult.Err(
                CircleError.DependencyParse(result.error),
            )
        }

    // JSXGraph 1.13.3: src/base/circle.js -> cloneToBackground.
    internal override fun cloneToBackground(): Circle {
        traceSnapshots += CircleTraceSnapshot(
            center = center.coords.usrCoords.copyOf(),
            radius = Radius(),
        )
        return this
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
        private const val DEFAULT_HIT_PRECISION = 4.0
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
