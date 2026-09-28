/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/plot.js -> Plot algorithm v2.
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
import kotlin.random.Random

internal enum class PlotCoordinate {
    X,
    Y,
}

internal sealed interface PlotError<out E> {
    data class Evaluation<E>(
        val coordinate: PlotCoordinate,
        val parameter: Double,
        val error: E,
    ) : PlotError<E>

    data class EvaluationException(
        val coordinate: PlotCoordinate,
        val parameter: Double,
        val message: String,
    ) : PlotError<Nothing>

    data class PointLimitExceeded(
        val attemptedCount: Int,
        val maximum: Int,
    ) : PlotError<Nothing>

    data class Numerics(
        val operation: String,
        val error: NumericsError,
    ) : PlotError<Nothing>

    data class Extrapolation(
        val coordinate: PlotCoordinate,
        val parameter: Double,
        val message: String,
    ) : PlotError<Nothing>

    data class InvalidSpecialInterval(
        val type: String,
        val start: Double,
        val end: Double,
    ) : PlotError<Nothing>
}

internal data class PlotResult(
    val points: List<Coords>,
    val visibleArea: DoubleArray,
)

internal fun interface PlotFunction<E> {
    fun evaluate(
        parameter: Double,
        suspendedUpdate: Boolean,
    ): GMResult<Double, E>
}

/**
 * Adaptive curve plotting translated from JSXGraph 1.13.3.
 */
internal object Plot {
    // JSXGraph 1.13.3: src/math/plot.js -> updateParametricCurve_v2.
    internal fun <E> updateParametricCurveV2(
        board: Board,
        minimum: Double,
        maximum: Double,
        recursionDepthHigh: Int,
        maximumPointCount: Int,
        x: PlotFunction<E>,
        y: PlotFunction<E>,
        random: () -> Double = { Random.nextDouble() },
    ): GMResult<PlotResult, PlotError<E>> =
        PlotV2State(
            board = board,
            recursionDepthHigh = recursionDepthHigh,
            maximumPointCount = maximumPointCount,
            x = x,
            y = y,
            random = random,
        ).updateParametricCurveV2(minimum, maximum)

    // JSXGraph 1.13.3: src/math/plot.js -> updateParametricCurve_v3.
    internal fun <E> updateParametricCurveV3(
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
        updateParametricCurveV3Impl(
            board = board,
            minimum = minimum,
            maximum = maximum,
            identityXTerm = identityXTerm,
            recursionDepthHigh = recursionDepthHigh,
            maximumPointCount = maximumPointCount,
            x = x,
            y = y,
            random = random,
        )

    // JSXGraph 1.13.3: src/math/plot.js -> updateParametricCurve_v4.
    internal fun <E> updateParametricCurveV4(
        board: Board,
        minimum: Double,
        maximum: Double,
        identityXTerm: Boolean,
        maximumPointCount: Int,
        x: PlotFunction<E>,
        y: PlotFunction<E>,
        intervalY: PlotIntervalFunction<E>? = null,
    ): GMResult<PlotResult, PlotError<E>> =
        updateParametricCurveV4Impl(
            board = board,
            minimum = minimum,
            maximum = maximum,
            identityXTerm = identityXTerm,
            maximumPointCount = maximumPointCount,
            x = x,
            y = y,
            intervalY = intervalY,
        )
}

private class PlotV2State<E>(
    private val board: Board,
    recursionDepthHigh: Int,
    private val maximumPointCount: Int,
    private val x: PlotFunction<E>,
    private val y: PlotFunction<E>,
    private val random: () -> Double,
) {
    private val points = mutableListOf<Coords>()
    /*
     * The portable scene Board intentionally stores user-space coordinates.
     * Reconstruct JSXGraph's canonical 500x500 screen projection from its
     * bounding box so Plot's pixel thresholds retain their upstream meaning.
     */
    private val projection = PlotProjection(board.getBoundingBox())
    private val smoothLevel = recursionDepthHigh - 9
    private val jumpLevel = 2
    private val nanLevel = recursionDepthHigh - 4
    private val recursionDepth = recursionDepthHigh
    private var lastCoordinates = DoubleArray(3) { Double.NaN }

    // JSXGraph 1.13.3: src/math/plot.js -> updateParametricCurve_v2.
    fun updateParametricCurveV2(
        minimum: Double,
        maximum: Double,
    ): GMResult<PlotResult, PlotError<E>> {
        /*
         * Upstream v2 checks `this.xterm`, where `this` is Mat.Plot at the
         * call site, so its function-graph bbox branch is not entered.
         */
        val minimumParameter = minimum
        val maximumParameter = maximum
        val first = when (
            val result = evaluatePoint(
                parameter = minimumParameter,
                suspendedUpdate = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val last = when (
            val result = evaluatePoint(
                parameter = maximumParameter,
                suspendedUpdate = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        val start = findStartPoint(
            coordinates = first.screenCoordinates,
            parameter = minimumParameter,
        )
        val end = findStartPoint(
            coordinates = last.screenCoordinates,
            parameter = maximumParameter,
        )

        first.coordinates.curveParameter = start.second
        when (val result = append(first.coordinates)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        lastCoordinates = first.screenCoordinates.copyOf()
        when (
            val result = plotRecursiveV2(
                a = first.screenCoordinates.copyOf(),
                ta = start.second,
                b = last.screenCoordinates.copyOf(),
                tb = end.second,
                depth = recursionDepth,
                delta = 2.0,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        last.coordinates.curveParameter = end.second
        when (val result = append(last.coordinates)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            PlotResult(
                points = points.toList(),
                visibleArea = doubleArrayOf(start.second, end.second),
            ),
        )
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _plotRecursive_v2.
    private fun plotRecursiveV2(
        a: DoubleArray,
        ta: Double,
        b: DoubleArray,
        tb: Double,
        depth: Int,
        delta: Double,
    ): GMResult<Unit, PlotError<E>> {
        if (depth < nanLevel) {
            val undefined = when (
                val result = isUndefined(a, ta, b, tb)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (undefined || isOutside(a, b)) {
                return GMResult.Ok(Unit)
            }
        }

        val tc = (ta + tb) * 0.5
        val midpoint = when (
            val result = evaluatePoint(tc, suspendedUpdate = true)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val c = midpoint.screenCoordinates
        val handledBorder = when (
            val result = borderCase(a, b, c, ta, tb, tc, depth)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (handledBorder) {
            return GMResult.Ok(Unit)
        }

        val distances = triangleDists(a, b, c)
        var isSmooth = depth < smoothLevel && distances[3] < delta
        val isJump =
            (
                depth <= jumpLevel &&
                    (
                        distances[0].isNaN() ||
                            distances[1].isNaN() ||
                            distances[2].isNaN()
                        )
                ) ||
                (
                    depth < jumpLevel &&
                        (
                            distances[2] > JUMP_THRESHOLD * distances[0] ||
                                distances[1] >
                                JUMP_THRESHOLD * distances[0] ||
                                distances[0] ==
                                Double.POSITIVE_INFINITY ||
                                distances[1] ==
                                Double.POSITIVE_INFINITY ||
                                distances[2] ==
                                Double.POSITIVE_INFINITY
                            )
                    )
        val isCusp =
            depth < smoothLevel + 2 &&
                distances[0] <
                CUSP_THRESHOLD * (distances[1] + distances[2])
        if (isCusp) {
            isSmooth = false
        }

        val nextDepth = depth - 1
        if (isJump) {
            return insertPointV2(
                point = PlotPoint(
                    coordinates = Coords(
                        method = Const.COORDS_BY_USER,
                        coordinates = doubleArrayOf(Double.NaN, Double.NaN),
                        board = board,
                        emitter = false,
                    ),
                    screenCoordinates = doubleArrayOf(
                        1.0,
                        Double.NaN,
                        Double.NaN,
                    ),
                ),
                parameter = tc,
            )
        }
        if (nextDepth <= MINIMUM_DEPTH || isSmooth) {
            return insertPointV2(midpoint, tc)
        }

        when (
            val result = plotRecursiveV2(
                a = a,
                ta = ta,
                b = c,
                tb = tc,
                depth = nextDepth,
                delta = delta,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        if (
            !(
                midpoint.screenCoordinates[1] +
                    midpoint.screenCoordinates[2]
                ).isNaN()
        ) {
            when (val result = insertPointV2(midpoint, tc)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return plotRecursiveV2(
            a = c,
            ta = tc,
            b = b,
            tb = tb,
            depth = nextDepth,
            delta = delta,
        )
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _insertPoint_v2.
    private fun insertPointV2(
        point: PlotPoint,
        parameter: Double?,
    ): GMResult<Unit, PlotError<E>> {
        val lastReal =
            !(lastCoordinates[1] + lastCoordinates[2]).isNaN()
        var newReal =
            !(
                point.screenCoordinates[1] +
                    point.screenCoordinates[2]
                ).isNaN()
        newReal =
            newReal &&
                point.screenCoordinates[1] > -OUTSIDE_MARGIN &&
                point.screenCoordinates[2] > -OUTSIDE_MARGIN &&
                point.screenCoordinates[1] <
                CANONICAL_BOARD_WIDTH + OUTSIDE_MARGIN &&
                point.screenCoordinates[2] <
                CANONICAL_BOARD_HEIGHT + OUTSIDE_MARGIN

        val shouldInsert =
            (!newReal && lastReal) ||
                (
                    newReal &&
                        (
                            !lastReal ||
                                abs(
                                    point.screenCoordinates[1] -
                                        lastCoordinates[1],
                                ) > MINIMUM_SCREEN_DISTANCE ||
                                abs(
                                    point.screenCoordinates[2] -
                                        lastCoordinates[2],
                                ) > MINIMUM_SCREEN_DISTANCE
                            )
                    )
        if (!shouldInsert) {
            return GMResult.Ok(Unit)
        }
        point.coordinates.curveParameter = parameter
        when (val result = append(point.coordinates)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        lastCoordinates = point.screenCoordinates.copyOf()
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> neighborhood_isNaN_v2.
    private fun neighborhoodIsNaNV2(
        parameter: Double,
    ): GMResult<Boolean, PlotError<E>> {
        val after = when (
            val result = evaluatePoint(
                parameter = parameter + Mat.eps,
                suspendedUpdate = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            !(
                after.coordinates.usrCoords[1] +
                    after.coordinates.usrCoords[2]
                ).isNaN()
        ) {
            val before = when (
                val result = evaluatePoint(
                    parameter = parameter - Mat.eps,
                    suspendedUpdate = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (
                !(
                    before.coordinates.usrCoords[1] +
                        before.coordinates.usrCoords[2]
                    ).isNaN()
            ) {
                return GMResult.Ok(false)
            }
        }
        return GMResult.Ok(true)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _borderCase.
    private fun borderCase(
        a: DoubleArray,
        b: DoubleArray,
        c: DoubleArray,
        ta: Double,
        tb: Double,
        tc: Double,
        depth: Int,
    ): GMResult<Boolean, PlotError<E>> {
        if (depth > 1) {
            return GMResult.Ok(false)
        }
        if (
            (a[1] + a[2]).isNaN() &&
            !(c[1] + c[2]).isNaN()
        ) {
            val neighborhood = when (
                val result = neighborhoodIsNaNV2(ta)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (!neighborhood) {
                return GMResult.Ok(false)
            }
        }
        if (
            (b[1] + b[2]).isNaN() &&
            !(c[1] + c[2]).isNaN()
        ) {
            val neighborhood = when (
                val result = neighborhoodIsNaNV2(tb)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (!neighborhood) {
                return GMResult.Ok(false)
            }
        }
        if (
            (c[1] + c[2]).isNaN() &&
            (
                !(a[1] + a[2]).isNaN() ||
                    !(b[1] + b[2]).isNaN()
                )
        ) {
            val neighborhood = when (
                val result = neighborhoodIsNaNV2(tc)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (!neighborhood) {
                return GMResult.Ok(false)
            }
        }

        val boundary = when {
            (a[1] + a[2]).isNaN() &&
                !(c[1] + c[2]).isNaN() -> ta to tc
            (b[1] + b[2]).isNaN() &&
                !(c[1] + c[2]).isNaN() -> tb to tc
            (c[1] + c[2]).isNaN() &&
                !(b[1] + b[2]).isNaN() -> tc to tb
            (c[1] + c[2]).isNaN() &&
                !(a[1] + a[2]).isNaN() -> tc to ta
            else -> return GMResult.Ok(false)
        }
        // Upstream resets t_nan/t_real on every pass and probes this same
        // midpoint up to 30 times; preserve that 1.13.3 behavior exactly.
        val parameter = 0.5 * (boundary.first + boundary.second)
        var point: PlotPoint
        var isUndefined: Boolean
        var iterations = 0
        do {
            point = when (
                val result = evaluatePoint(
                    parameter = parameter,
                    suspendedUpdate = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            isUndefined =
                (
                    point.coordinates.usrCoords[1] +
                        point.coordinates.usrCoords[2]
                    ).isNaN()
            iterations += 1
        } while (isUndefined && iterations < MAX_BORDER_ITERATIONS)

        if (iterations < MAX_BORDER_ITERATIONS) {
            /*
             * Upstream omits the t argument in this insertion. Preserve its
             * undefined `_t` as null.
             */
            return when (val result = insertPointV2(point, null)) {
                is GMResult.Ok -> GMResult.Ok(true)
                is GMResult.Err -> result
            }
        }
        return GMResult.Ok(false)
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

    // JSXGraph 1.13.3: src/math/plot.js -> _findStartPoint.
    private fun findStartPoint(
        coordinates: DoubleArray,
        parameter: Double,
    ): Pair<DoubleArray, Double> = coordinates to parameter

    private fun evaluatePoint(
        parameter: Double,
        suspendedUpdate: Boolean,
    ): GMResult<PlotPoint, PlotError<E>> {
        val xResult = try {
            x.evaluate(parameter, suspendedUpdate)
        } catch (exception: Exception) {
            return GMResult.Err(
                PlotError.EvaluationException(
                    coordinate = PlotCoordinate.X,
                    parameter = parameter,
                    message = exception.message
                        ?: exception::class.simpleName
                        ?: "Plot X evaluation failed",
                ),
            )
        }
        val xValue = when (
            val result = xResult
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                PlotError.Evaluation(
                    coordinate = PlotCoordinate.X,
                    parameter = parameter,
                    error = result.error,
                ),
            )
        }
        val yResult = try {
            y.evaluate(parameter, suspendedUpdate)
        } catch (exception: Exception) {
            return GMResult.Err(
                PlotError.EvaluationException(
                    coordinate = PlotCoordinate.Y,
                    parameter = parameter,
                    message = exception.message
                        ?: exception::class.simpleName
                        ?: "Plot Y evaluation failed",
                ),
            )
        }
        val yValue = when (
            val result = yResult
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                PlotError.Evaluation(
                    coordinate = PlotCoordinate.Y,
                    parameter = parameter,
                    error = result.error,
                ),
            )
        }
        return GMResult.Ok(
            PlotPoint(
                coordinates = Coords(
                    method = Const.COORDS_BY_USER,
                    coordinates = doubleArrayOf(xValue, yValue),
                    board = board,
                    emitter = false,
                ),
                screenCoordinates = projection.toScreen(xValue, yValue),
            ),
        )
    }

    private data class PlotPoint(
        val coordinates: Coords,
        val screenCoordinates: DoubleArray,
    )

    private class PlotProjection(
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

    private companion object {
        const val CANONICAL_BOARD_WIDTH = 500.0
        const val CANONICAL_BOARD_HEIGHT = 500.0
        const val MINIMUM_DEPTH = 0
        const val CUSP_THRESHOLD = 0.5
        const val JUMP_THRESHOLD = 0.99
        const val OUTSIDE_MARGIN = 500.0
        const val MINIMUM_SCREEN_DISTANCE = 0.7
        const val MAX_BORDER_ITERATIONS = 30
        const val UNDEFINED_SAMPLE_COUNT = 20
    }
}
