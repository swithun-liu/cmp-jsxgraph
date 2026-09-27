/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/plot.js -> Plot algorithm v3.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Coords
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

internal fun <E> updateParametricCurveV3Impl(
    board: Board,
    minimum: Double,
    maximum: Double,
    identityXTerm: Boolean,
    recursionDepthHigh: Int,
    maximumPointCount: Int,
    x: PlotFunction<E>,
    y: PlotFunction<E>,
    random: () -> Double = { Random.nextDouble() },
): GMResult<PlotResult, PlotError<E>> =
    PlotV3State(
        board = board,
        minimum = minimum,
        maximum = maximum,
        identityXTerm = identityXTerm,
        recursionDepthHigh = recursionDepthHigh,
        maximumPointCount = maximumPointCount,
        x = x,
        y = y,
        random = random,
    ).updateParametricCurveV3()

private class PlotV3State<E>(
    private val board: Board,
    private val minimum: Double,
    private val maximum: Double,
    private val identityXTerm: Boolean,
    private val recursionDepthHigh: Int,
    private val maximumPointCount: Int,
    private val x: PlotFunction<E>,
    private val y: PlotFunction<E>,
    private val random: () -> Double,
) {
    private val points = mutableListOf<Coords>()
    private val projection = PlotV3Projection(board.getBoundingBox())
    private val nanLevel = recursionDepthHigh - 4
    private var lastScreenCoordinates = DoubleArray(3) { Double.NaN }
    private var lastUserCoordinates = DoubleArray(3) { Double.NaN }

    // JSXGraph 1.13.3: src/math/plot.js -> updateParametricCurve_v3.
    fun updateParametricCurveV3(): GMResult<PlotResult, PlotError<E>> {
        val boundingBox = board.getBoundingBox()
        val horizontalMargin =
            (boundingBox[2] - boundingBox[0]) * DOMAIN_MARGIN_RATIO
        val startParameter = if (identityXTerm) {
            max(minimum, boundingBox[0] - horizontalMargin)
        } else {
            minimum
        }
        val endParameter = if (identityXTerm) {
            min(maximum, boundingBox[2] + horizontalMargin)
        } else {
            maximum
        }
        val first = when (
            val result = evaluatePoint(
                parameter = startParameter,
                suspendedUpdate = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val last = when (
            val result = evaluatePoint(
                parameter = endParameter,
                suspendedUpdate = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        first.coordinates.curveParameter = startParameter
        when (val result = append(first.coordinates)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        lastScreenCoordinates = first.screenCoordinates.copyOf()
        lastUserCoordinates = first.coordinates.usrCoords.copyOf()

        when (
            val result = plotNonRecursive(
                firstScreenCoordinates = first.screenCoordinates.copyOf(),
                firstParameter = startParameter,
                lastScreenCoordinates = last.screenCoordinates.copyOf(),
                lastParameter = endParameter,
                initialDepth = recursionDepthHigh,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }

        last.coordinates.curveParameter = endParameter
        when (val result = append(last.coordinates)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            PlotResult(
                points = points.toList(),
                visibleArea = doubleArrayOf(
                    startParameter,
                    endParameter,
                ),
            ),
        )
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _plotNonRecursive.
    private fun plotNonRecursive(
        firstScreenCoordinates: DoubleArray,
        firstParameter: Double,
        lastScreenCoordinates: DoubleArray,
        lastParameter: Double,
        initialDepth: Int,
    ): GMResult<Unit, PlotError<E>> {
        val stack = mutableListOf(
            PlotV3StackItem(
                firstScreenCoordinates = firstScreenCoordinates,
                firstParameter = firstParameter,
                lastScreenCoordinates = lastScreenCoordinates,
                lastParameter = lastParameter,
                depth = initialDepth,
                previousDistance = Double.POSITIVE_INFINITY,
            ),
        )
        while (stack.isNotEmpty()) {
            val item = stack.removeAt(stack.lastIndex)
            val a = item.firstScreenCoordinates
            val b = item.lastScreenCoordinates
            val ta = item.firstParameter
            val tb = item.lastParameter
            val depth = item.depth

            if (depth < nanLevel) {
                val undefined = when (
                    val result = isUndefined(a, ta, b, tb)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (undefined || isOutside(a, b)) {
                    continue
                }
            }

            val tc = (ta + tb) * 0.5
            val midpoint = when (
                val result = evaluatePoint(
                    parameter = tc,
                    suspendedUpdate = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val c = midpoint.screenCoordinates
            val distances = triangleDists(a, b, c)
            val aIsNaN = (a[1] + a[2]).isNaN()
            val bIsNaN = (b[1] + b[2]).isNaN()
            val special = when {
                aIsNaN != bIsNaN -> PlotV3Special.BORDER
                distances[0] >
                    CUSP_PREVIOUS_DISTANCE_RATIO *
                    item.previousDistance ||
                    distances[0] <
                    CUSP_THRESHOLD *
                    (distances[1] + distances[2]) ||
                    distances[1] >
                    CUSP_DISTANCE_RATIO * distances[2] ||
                    distances[2] >
                    CUSP_DISTANCE_RATIO * distances[1] ->
                    PlotV3Special.CUSP
                distances[2] >
                    JUMP_THRESHOLD * distances[0] ||
                    distances[1] >
                    JUMP_THRESHOLD * distances[0] ||
                    distances[0] == Double.POSITIVE_INFINITY ||
                    distances[1] == Double.POSITIVE_INFINITY ||
                    distances[2] == Double.POSITIVE_INFINITY ->
                    PlotV3Special.JUMP
                else -> null
            }
            var isSmooth =
                special == null &&
                    depth < SMOOTH_LEVEL &&
                    distances[3] < SMOOTH_THRESHOLD
            var limes: PlotV3Limes? = null
            if (depth < TEST_LEVEL && !isSmooth) {
                if (special == null) {
                    isSmooth = true
                } else {
                    limes = when (
                        val result = getLimes(
                            ta = ta,
                            a = a,
                            tc = tc,
                            c = c,
                            tb = tb,
                            b = b,
                            special = special,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                }
            }

            if (limes != null) {
                when (
                    val result = insertPoint(
                        point = nanPoint(),
                        parameter = tc,
                        depth = depth,
                        limes = limes,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            } else if (depth <= MINIMUM_DEPTH || isSmooth) {
                when (
                    val result = insertPoint(
                        point = midpoint,
                        parameter = tc,
                        depth = depth,
                        limes = null,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            } else {
                val nextDepth = depth - 1
                stack += PlotV3StackItem(
                    firstScreenCoordinates = c.copyOf(),
                    firstParameter = tc,
                    lastScreenCoordinates = b,
                    lastParameter = tb,
                    depth = nextDepth,
                    previousDistance = distances[0],
                )
                stack += PlotV3StackItem(
                    firstScreenCoordinates = a,
                    firstParameter = ta,
                    lastScreenCoordinates = c.copyOf(),
                    lastParameter = tc,
                    depth = nextDepth,
                    previousDistance = distances[0],
                )
            }
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _getBorderPos.
    private fun getBorderPosition(
        ta: Double,
        a: DoubleArray,
        tc: Double,
        c: DoubleArray,
        tb: Double,
        b: DoubleArray,
    ): GMResult<Double, PlotError<E>> {
        val initialBounds = when {
            (a[1] + a[2]).isNaN() &&
                !(c[1] + c[2]).isNaN() -> ta to tc
            (b[1] + b[2]).isNaN() &&
                !(c[1] + c[2]).isNaN() -> tb to tc
            (c[1] + c[2]).isNaN() &&
                !(b[1] + b[2]).isNaN() -> tc to tb
            (c[1] + c[2]).isNaN() &&
                !(a[1] + a[2]).isNaN() -> tc to ta
            else -> return GMResult.Err(
                PlotError.InvalidSpecialInterval(
                    type = PlotV3Special.BORDER.name,
                    start = ta,
                    end = tb,
                ),
            )
        }
        var badParameter = initialBounds.first
        var goodParameter = initialBounds.second
        var parameter: Double
        var iterations = 0
        do {
            parameter = 0.5 * (goodParameter + badParameter)
            val point = when (
                val result = evaluatePoint(
                    parameter = parameter,
                    suspendedUpdate = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (
                (
                    point.coordinates.usrCoords[1] +
                        point.coordinates.usrCoords[2]
                    ).isNaN()
            ) {
                badParameter = parameter
            } else {
                goodParameter = parameter
            }
            iterations += 1
        } while (
            iterations < MAX_BORDER_ITERATIONS &&
            abs(goodParameter - badParameter) > Mat.eps
        )
        return GMResult.Ok(parameter)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _getCuspPos.
    private fun getCuspPosition(
        ta: Double,
        tb: Double,
    ): GMResult<Double, PlotError<E>> {
        val a = when (
            val result = evaluatePoint(ta, suspendedUpdate = true)
        ) {
            is GMResult.Ok -> result.value.coordinates.usrCoords
            is GMResult.Err -> return result
        }
        val b = when (
            val result = evaluatePoint(tb, suspendedUpdate = true)
        ) {
            is GMResult.Ok -> result.value.coordinates.usrCoords
            is GMResult.Err -> return result
        }
        var evaluationError: PlotError<E>? = null
        val result = Numerics.fminbr(
            function = { parameter ->
                if (evaluationError != null) {
                    Double.NaN
                } else {
                    when (
                        val evaluated = evaluatePoint(
                            parameter = parameter,
                            suspendedUpdate = true,
                        )
                    ) {
                        is GMResult.Ok -> {
                            val point = evaluated.value.coordinates.usrCoords
                            -(
                                Mat.hypot(
                                    a[1] - point[1],
                                    a[2] - point[2],
                                ) +
                                    Mat.hypot(
                                        b[1] - point[1],
                                        b[2] - point[2],
                                    )
                                )
                        }
                        is GMResult.Err -> {
                            evaluationError = evaluated.error
                            Double.NaN
                        }
                    }
                }
            },
            interval = doubleArrayOf(ta, tb),
        )
        val error = evaluationError
        if (error != null) {
            return GMResult.Err(error)
        }
        return when (result) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                PlotError.Numerics(
                    operation = "_getCuspPos",
                    error = result.error,
                ),
            )
        }
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _getJumpPos.
    private fun getJumpPosition(
        ta: Double,
        tb: Double,
    ): GMResult<Double, PlotError<E>> {
        var evaluationError: PlotError<E>? = null
        val result = Numerics.fminbr(
            function = { parameter ->
                if (evaluationError != null) {
                    Double.NaN
                } else {
                    val first = when (
                        val evaluated = evaluatePoint(
                            parameter = parameter,
                            suspendedUpdate = true,
                        )
                    ) {
                        is GMResult.Ok -> evaluated.value.coordinates.usrCoords
                        is GMResult.Err -> {
                            evaluationError = evaluated.error
                            null
                        }
                    }
                    val second = if (first == null) {
                        null
                    } else {
                        when (
                            val evaluated = evaluatePoint(
                                parameter = parameter + Mat.eps * Mat.eps,
                                suspendedUpdate = true,
                            )
                        ) {
                            is GMResult.Ok ->
                                evaluated.value.coordinates.usrCoords
                            is GMResult.Err -> {
                                evaluationError = evaluated.error
                                null
                            }
                        }
                    }
                    if (first == null || second == null) {
                        Double.NaN
                    } else {
                        -abs(
                            (second[2] - first[2]) /
                                (second[1] - first[1]),
                        )
                    }
                }
            },
            interval = doubleArrayOf(ta, tb),
        )
        val error = evaluationError
        if (error != null) {
            return GMResult.Err(error)
        }
        return when (result) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                PlotError.Numerics(
                    operation = "_getJumpPos",
                    error = result.error,
                ),
            )
        }
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _getLimits.
    private fun getLimits(
        parameter: Double,
    ): GMResult<PlotV3Limes, PlotError<E>> {
        val step = 2.0 / (maximum - minimum)
        val leftX = when (
            val result = limit(
                coordinate = PlotCoordinate.X,
                parameter = parameter,
                step = -step,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val leftY = when (
            val result = limit(
                coordinate = PlotCoordinate.Y,
                parameter = parameter,
                step = -step,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val rightX = when (
            val result = limit(
                coordinate = PlotCoordinate.X,
                parameter = parameter,
                step = step,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val rightY = when (
            val result = limit(
                coordinate = PlotCoordinate.Y,
                parameter = parameter,
                step = step,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            PlotV3Limes(
                leftX = leftX,
                leftY = leftY,
                rightX = rightX,
                rightY = rightY,
            ),
        )
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _getLimes.
    private fun getLimes(
        ta: Double,
        a: DoubleArray,
        tc: Double,
        c: DoubleArray,
        tb: Double,
        b: DoubleArray,
        special: PlotV3Special,
    ): GMResult<PlotV3Limes, PlotError<E>> {
        val parameter = when (special) {
            PlotV3Special.BORDER ->
                getBorderPosition(ta, a, tc, c, tb, b)
            PlotV3Special.CUSP -> getCuspPosition(ta, tb)
            PlotV3Special.JUMP -> getJumpPosition(ta, tb)
        }
        return when (parameter) {
            is GMResult.Ok -> getLimits(parameter.value)
            is GMResult.Err -> parameter
        }
    }

    private fun limit(
        coordinate: PlotCoordinate,
        parameter: Double,
        step: Double,
    ): GMResult<Double, PlotError<E>> {
        val result = Extrapolate.limit(
            x0 = parameter,
            initialStep = step,
            function = ExtrapolateFunction<PlotError<E>> {
                    value,
                    _,
                ->
                evaluateCoordinate(
                    coordinate = coordinate,
                    parameter = value,
                    suspendedUpdate = false,
                )
            },
        )
        return when (result) {
            is GMResult.Ok -> {
                val extrapolated = result.value
                GMResult.Ok(
                    if (
                        extrapolated.classification ==
                        ExtrapolateClassification.INFINITE
                    ) {
                        Mat.sign(extrapolated.value) *
                            Double.POSITIVE_INFINITY
                    } else {
                        extrapolated.value
                    },
                )
            }
            is GMResult.Err -> when (val error = result.error) {
                is ExtrapolateError.Evaluation ->
                    GMResult.Err(error.cause)
                else -> GMResult.Err(
                    PlotError.Extrapolation(
                        coordinate = coordinate,
                        parameter = parameter,
                        message = error.toString(),
                    ),
                )
            }
        }
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _insertPoint.
    private fun insertPoint(
        point: PlotV3Point,
        parameter: Double,
        depth: Int,
        limes: PlotV3Limes?,
    ): GMResult<Unit, PlotError<E>> {
        if (limes != null) {
            return insertLimesPoint(
                point = point,
                parameter = parameter,
                depth = depth,
                limes = limes,
            )
        }

        val lastIsReal =
            !(lastScreenCoordinates[1] + lastScreenCoordinates[2]).isNaN()
        var pointIsReal =
            !(
                point.screenCoordinates[1] +
                    point.screenCoordinates[2]
                ).isNaN()
        pointIsReal =
            pointIsReal &&
                point.screenCoordinates[1] > -OUTSIDE_MARGIN &&
                point.screenCoordinates[2] > -OUTSIDE_MARGIN &&
                point.screenCoordinates[1] <
                CANONICAL_BOARD_WIDTH + OUTSIDE_MARGIN &&
                point.screenCoordinates[2] <
                CANONICAL_BOARD_HEIGHT + OUTSIDE_MARGIN

        if (!lastIsReal && !pointIsReal) {
            return GMResult.Ok(Unit)
        }
        if (
            pointIsReal &&
            lastIsReal &&
            abs(
                point.screenCoordinates[1] -
                    lastScreenCoordinates[1],
            ) < MINIMUM_SCREEN_DISTANCE &&
            abs(
                point.screenCoordinates[2] -
                    lastScreenCoordinates[2],
            ) < MINIMUM_SCREEN_DISTANCE
        ) {
            return GMResult.Ok(Unit)
        }
        if (
            (
                abs(point.screenCoordinates[1]) ==
                    Double.POSITIVE_INFINITY &&
                    abs(lastUserCoordinates[1]) ==
                    Double.POSITIVE_INFINITY
                ) ||
            (
                abs(point.screenCoordinates[2]) ==
                    Double.POSITIVE_INFINITY &&
                    abs(lastUserCoordinates[2]) ==
                    Double.POSITIVE_INFINITY
                )
        ) {
            return GMResult.Ok(Unit)
        }

        point.coordinates.curveParameter = parameter
        when (val result = append(point.coordinates)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        lastScreenCoordinates = point.screenCoordinates.copyOf()
        lastUserCoordinates = point.coordinates.usrCoords.copyOf()
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _insertLimesPoint.
    private fun insertLimesPoint(
        point: PlotV3Point,
        parameter: Double,
        depth: Int,
        limes: PlotV3Limes,
    ): GMResult<Unit, PlotError<E>> {
        if (
            (
                abs(lastUserCoordinates[1]) ==
                    Double.POSITIVE_INFINITY &&
                    abs(limes.leftX) ==
                    Double.POSITIVE_INFINITY
                ) ||
            (
                abs(lastUserCoordinates[2]) ==
                    Double.POSITIVE_INFINITY &&
                    abs(limes.leftY) ==
                    Double.POSITIVE_INFINITY
                )
        ) {
            return GMResult.Ok(Unit)
        }

        val left = pointFromUser(limes.leftX, limes.leftY)
        left.coordinates.curveParameter = parameter
        when (val result = append(left.coordinates)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }

        if (
            !limes.leftX.isNaN() &&
            !limes.leftY.isNaN() &&
            !limes.rightX.isNaN() &&
            !limes.rightY.isNaN() &&
            (
                abs(limes.leftX - limes.rightX) > Mat.eps ||
                    abs(limes.leftY - limes.rightY) > Mat.eps
                )
        ) {
            point.coordinates.curveParameter = parameter
            when (val result = append(point.coordinates)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        val right = pointFromUser(limes.rightX, limes.rightY)
        right.coordinates.curveParameter = parameter
        when (val result = append(right.coordinates)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        lastScreenCoordinates = right.screenCoordinates.copyOf()
        lastUserCoordinates = right.coordinates.usrCoords.copyOf()
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _triangleDists.
    private fun triangleDists(
        a: DoubleArray,
        b: DoubleArray,
        c: DoubleArray,
    ): DoubleArray {
        val middle = doubleArrayOf(
            a[0] * b[0],
            (a[1] + b[1]) * 0.5,
            (a[2] + b[2]) * 0.5,
        )
        return doubleArrayOf(
            Geometry.distance(a, b, 3),
            Geometry.distance(a, c, 3),
            Geometry.distance(c, b, 3),
            Geometry.distance(c, middle, 3),
        )
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _isUndefined.
    private fun isUndefined(
        a: DoubleArray,
        ta: Double,
        b: DoubleArray,
        tb: Double,
    ): GMResult<Boolean, PlotError<E>> {
        if (
            !(a[1] + a[2]).isNaN() ||
            !(b[1] + b[2]).isNaN()
        ) {
            return GMResult.Ok(false)
        }
        repeat(UNDEFINED_SAMPLE_COUNT) {
            val parameter = ta + random() * (tb - ta)
            val point = when (
                val result = evaluatePoint(
                    parameter = parameter,
                    suspendedUpdate = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (
                !(
                    point.screenCoordinates[0] +
                        point.screenCoordinates[1] +
                        point.screenCoordinates[2]
                    ).isNaN()
            ) {
                return GMResult.Ok(false)
            }
        }
        return GMResult.Ok(true)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _isOutside.
    private fun isOutside(
        a: DoubleArray,
        b: DoubleArray,
    ): Boolean =
        (a[1] < -OUTSIDE_MARGIN && b[1] < -OUTSIDE_MARGIN) ||
            (a[2] < -OUTSIDE_MARGIN && b[2] < -OUTSIDE_MARGIN) ||
            (
                a[1] > CANONICAL_BOARD_WIDTH + OUTSIDE_MARGIN &&
                    b[1] > CANONICAL_BOARD_WIDTH + OUTSIDE_MARGIN
                ) ||
            (
                a[2] > CANONICAL_BOARD_HEIGHT + OUTSIDE_MARGIN &&
                    b[2] > CANONICAL_BOARD_HEIGHT + OUTSIDE_MARGIN
                )

    private fun evaluatePoint(
        parameter: Double,
        suspendedUpdate: Boolean,
    ): GMResult<PlotV3Point, PlotError<E>> {
        val xValue = when (
            val result = evaluateCoordinate(
                coordinate = PlotCoordinate.X,
                parameter = parameter,
                suspendedUpdate = suspendedUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val yValue = when (
            val result = evaluateCoordinate(
                coordinate = PlotCoordinate.Y,
                parameter = parameter,
                suspendedUpdate = suspendedUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(pointFromUser(xValue, yValue))
    }

    private fun evaluateCoordinate(
        coordinate: PlotCoordinate,
        parameter: Double,
        suspendedUpdate: Boolean,
    ): GMResult<Double, PlotError<E>> {
        val function = when (coordinate) {
            PlotCoordinate.X -> x
            PlotCoordinate.Y -> y
        }
        val result = try {
            function.evaluate(parameter, suspendedUpdate)
        } catch (exception: Exception) {
            return GMResult.Err(
                PlotError.EvaluationException(
                    coordinate = coordinate,
                    parameter = parameter,
                    message = exception.message
                        ?: exception::class.simpleName
                        ?: "Plot evaluation failed",
                ),
            )
        }
        return when (result) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                PlotError.Evaluation(
                    coordinate = coordinate,
                    parameter = parameter,
                    error = result.error,
                ),
            )
        }
    }

    private fun pointFromUser(
        x: Double,
        y: Double,
    ): PlotV3Point =
        PlotV3Point(
            coordinates = Coords(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(x, y),
                board = board,
                emitter = false,
            ),
            screenCoordinates = projection.toScreen(x, y),
        )

    private fun nanPoint(): PlotV3Point =
        pointFromUser(Double.NaN, Double.NaN)

    private fun append(
        point: Coords,
    ): GMResult<Unit, PlotError<E>> {
        if (points.size >= maximumPointCount) {
            return GMResult.Err(
                PlotError.PointLimitExceeded(
                    attemptedCount = points.size + 1,
                    maximum = maximumPointCount,
                ),
            )
        }
        points += point
        return GMResult.Ok(Unit)
    }

    private data class PlotV3Point(
        val coordinates: Coords,
        val screenCoordinates: DoubleArray,
    )

    private data class PlotV3StackItem(
        val firstScreenCoordinates: DoubleArray,
        val firstParameter: Double,
        val lastScreenCoordinates: DoubleArray,
        val lastParameter: Double,
        val depth: Int,
        val previousDistance: Double,
    )

    private data class PlotV3Limes(
        val leftX: Double,
        val leftY: Double,
        val rightX: Double,
        val rightY: Double,
    )

    private class PlotV3Projection(
        boundingBox: DoubleArray,
    ) {
        private val unitX =
            CANONICAL_BOARD_WIDTH / (boundingBox[2] - boundingBox[0])
        private val unitY =
            CANONICAL_BOARD_HEIGHT / (boundingBox[1] - boundingBox[3])
        private val originX = -boundingBox[0] * unitX
        private val originY = boundingBox[1] * unitY

        fun toScreen(
            x: Double,
            y: Double,
        ): DoubleArray =
            doubleArrayOf(
                1.0,
                originX + x * unitX,
                originY - y * unitY,
            )
    }

    private enum class PlotV3Special {
        BORDER,
        CUSP,
        JUMP,
    }

    private companion object {
        const val CANONICAL_BOARD_WIDTH = 500.0
        const val CANONICAL_BOARD_HEIGHT = 500.0
        const val DOMAIN_MARGIN_RATIO = 0.3
        const val SMOOTH_LEVEL = 7
        const val TEST_LEVEL = 4
        const val MINIMUM_DEPTH = 0
        const val CUSP_PREVIOUS_DISTANCE_RATIO = 0.66
        const val CUSP_THRESHOLD = 0.5
        const val CUSP_DISTANCE_RATIO = 5.0
        const val JUMP_THRESHOLD = 0.99
        const val SMOOTH_THRESHOLD = 2.0
        const val OUTSIDE_MARGIN = 500.0
        const val MINIMUM_SCREEN_DISTANCE = 0.8
        const val MAX_BORDER_ITERATIONS = 30
        const val UNDEFINED_SAMPLE_COUNT = 20
    }
}
