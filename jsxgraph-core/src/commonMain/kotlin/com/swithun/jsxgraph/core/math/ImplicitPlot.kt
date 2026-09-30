/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/implicitplot.js -> ImplicitPlot.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal data class ImplicitPlotConfig(
    val resolutionOuter: Double = 5.0,
    val resolutionInner: Double = 5.0,
    val maxSteps: Double = 1024.0,
    val alpha0: Double = 0.05,
    val tolU0: Double = Mat.eps,
    val tolNewton: Double = 1.0e-7,
    val tolCusp: Double = 0.05,
    val tolProgress: Double = 0.0001,
    val qdtBox: Double = 0.2,
    val kappa0: Double = 0.2,
    val delta0: Double = 0.05,
    val hInitial: Double = 0.1,
    val hCritical: Double = 0.001,
    val hMax: Double = 1.0,
    val loopDist: Double = 0.09,
    val loopDir: Double = 0.99,
    val loopDetection: Boolean = true,
    val unitX: Double = 10.0,
    val unitY: Double = 10.0,
)

internal sealed interface ImplicitPlotError {
    data class InvalidBoundingBox(
        val size: Int,
    ) : ImplicitPlotError

    data class InvalidConfiguration(
        val name: String,
        val value: Double,
    ) : ImplicitPlotError

    data class Numerics(
        val operation: String,
        val error: NumericsError,
    ) : ImplicitPlotError

    data class PointLimitExceeded(
        val requested: Long,
        val maximum: Int,
    ) : ImplicitPlotError
}

internal data class ImplicitPlotResult(
    val dataX: DoubleArray,
    val dataY: DoubleArray,
    val componentCount: Int,
)

internal data class ImplicitTraceResult(
    val pathX: DoubleArray,
    val pathY: DoubleArray,
    val loopClosed: Boolean,
)

private data class ImplicitPlotSegment(
    override val xlb: Double,
    override val xub: Double,
    override val ylb: Double,
    override val yub: Double,
    val index1: Int,
    val index2: Int,
    val component: Int,
) : BoxQuadtreeItem

private data class ImplicitSearchState(
    val dataX: MutableList<Double>,
    val dataY: MutableList<Double>,
    var componentCount: Int,
)

internal class ImplicitPlot(
    boundingBox: DoubleArray,
    private val config: ImplicitPlotConfig,
    private val f: (Double, Double) -> Double,
    dfx: ((Double, Double) -> Double)? = null,
    dfy: ((Double, Double) -> Double)? = null,
    private val maximumPointCount: Int = Int.MAX_VALUE,
) {
    private val boundingBox = boundingBox.copyOf()
    private val dfx: (Double, Double) -> Double =
        dfx ?: { x, y ->
            val h = Mat.eps * Mat.eps
            (f(x + h, y) - f(x - h, y)) * 0.5 / h
        }
    private val dfy: (Double, Double) -> Double =
        dfy ?: { x, y ->
            val h = Mat.eps * Mat.eps
            (f(x, y + h) - f(x, y - h)) * 0.5 / h
        }

    // JSXGraph 1.13.3: src/math/implicitplot.js -> constructor.
    private val qdt = BoxQuadtree<ImplicitPlotSegment>(
        depth = 20,
        capacity = 5,
        boundingBox = boundingBox.takeIf { it.size >= 4 },
    )

    // JSXGraph 1.13.3: src/math/implicitplot.js -> plot.
    internal fun plot(): GMResult<ImplicitPlotResult, ImplicitPlotError> {
        when (val validation = validateInput()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return validation
        }

        val minimumX = min(boundingBox[0], boundingBox[2]) - Mat.eps
        val maximumX = max(boundingBox[0], boundingBox[2])
        val minimumY = min(boundingBox[1], boundingBox[3]) + Mat.eps
        val maximumY = max(boundingBox[1], boundingBox[3])
        val state = ImplicitSearchState(
            dataX = mutableListOf(),
            dataY = mutableListOf(),
            componentCount = 0,
        )

        var delta = config.resolutionOuter / config.unitX
        delta *= 1.0 + Mat.eps
        var x = minimumX
        while (x < maximumX) {
            when (
                val result = searchLine(
                    minimizationFunction = { coordinate -> f(x, coordinate) },
                    maximizationFunction = { coordinate -> -f(x, coordinate) },
                    fixed = x,
                    interval = doubleArrayOf(minimumY, maximumY),
                    direction = SearchDirection.VERTICAL,
                    state = state,
                    level = MAX_SEARCH_LEVEL,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            x += delta
        }

        delta = config.resolutionOuter / config.unitY
        delta *= 1.0 + Mat.eps
        var y = minimumY
        while (y < maximumY) {
            when (
                val result = searchLine(
                    minimizationFunction = { coordinate -> f(coordinate, y) },
                    maximizationFunction = { coordinate -> -f(coordinate, y) },
                    fixed = y,
                    interval = doubleArrayOf(minimumX, maximumX),
                    direction = SearchDirection.HORIZONTAL,
                    state = state,
                    level = MAX_SEARCH_LEVEL,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            y += delta
        }

        return GMResult.Ok(
            ImplicitPlotResult(
                dataX = state.dataX.toDoubleArray(),
                dataY = state.dataY.toDoubleArray(),
                componentCount = state.componentCount,
            ),
        )
    }

    // JSXGraph 1.13.3: src/math/implicitplot.js -> searchLine.
    private fun searchLine(
        minimizationFunction: (Double) -> Double,
        maximizationFunction: (Double) -> Double,
        fixed: Double,
        interval: DoubleArray,
        direction: SearchDirection,
        state: ImplicitSearchState,
        level: Int,
    ): GMResult<Unit, ImplicitPlotError> {
        val minimumParameter = when (
            val result = Numerics.fminbr(
                function = minimizationFunction,
                interval = interval,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                ImplicitPlotError.Numerics("searchLine.fminbr.minimum", result.error),
            )
        }
        val minimumValue = minimizationFunction(minimumParameter)
        val maximumParameter = when (
            val result = Numerics.fminbr(
                function = maximizationFunction,
                interval = interval,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                ImplicitPlotError.Numerics("searchLine.fminbr.maximum", result.error),
            )
        }
        val maximumValue = minimizationFunction(maximumParameter)

        if (
            minimumValue >= config.tolU0 ||
            maximumValue <= -config.tolU0
        ) {
            return GMResult.Ok(Unit)
        }

        val start = min(minimumParameter, maximumParameter)
        val end = max(minimumParameter, maximumParameter)
        val root = when (
            val result = Numerics.fzero(
                function = minimizationFunction,
                interval = doubleArrayOf(start, end),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                ImplicitPlotError.Numerics("searchLine.fzero", result.error),
            )
        }
        val rootValue = minimizationFunction(root)
        if (
            abs(rootValue) >
            max((maximumValue - minimumValue) * Mat.eps, 0.001)
        ) {
            return GMResult.Ok(Unit)
        }

        val point: DoubleArray
        var delta: Double
        if (direction == SearchDirection.VERTICAL) {
            point = doubleArrayOf(1.0, fixed, root)
            delta = config.resolutionInner / config.unitY
        } else {
            point = doubleArrayOf(1.0, root, fixed)
            delta = config.resolutionInner / config.unitX
        }
        delta *= 1.0 + Mat.eps

        if (
            !curveContainsPoint(
                point = point,
                dataX = state.dataX,
                dataY = state.dataY,
                tolerance = delta * 2.0,
                epsilon = config.qdtBox,
            )
        ) {
            val component = traceComponent(point)
            if (component.first.isNotEmpty()) {
                val separatorCount = if (state.componentCount > 0) 1 else 0
                val requested =
                    state.dataX.size.toLong() +
                        separatorCount +
                        component.first.size
                if (requested > maximumPointCount) {
                    return GMResult.Err(
                        ImplicitPlotError.PointLimitExceeded(
                            requested = requested,
                            maximum = maximumPointCount,
                        ),
                    )
                }
                if (separatorCount > 0) {
                    state.dataX += Double.NaN
                    state.dataY += Double.NaN
                }

                val offset = state.dataX.size
                for (index in 1 until component.first.size) {
                    qdt.insertItem(
                        ImplicitPlotSegment(
                            xlb = min(
                                component.first[index - 1],
                                component.first[index],
                            ),
                            xub = max(
                                component.first[index - 1],
                                component.first[index],
                            ),
                            ylb = min(
                                component.second[index - 1],
                                component.second[index],
                            ),
                            yub = max(
                                component.second[index - 1],
                                component.second[index],
                            ),
                            index1 = offset + index - 1,
                            index2 = offset + index,
                            component = state.componentCount,
                        ),
                    )
                }
                state.componentCount += 1
                state.dataX += component.first.toList()
                state.dataY += component.second.toList()
            }
        }

        val intervalStart = interval[0]
        val intervalEnd = interval[1]
        var subdivision = root - delta * 0.01
        if (subdivision - intervalStart > delta && level > 0) {
            when (
                val result = searchLine(
                    minimizationFunction = minimizationFunction,
                    maximizationFunction = maximizationFunction,
                    fixed = fixed,
                    interval = doubleArrayOf(intervalStart, subdivision),
                    direction = direction,
                    state = state,
                    level = level - 1,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        subdivision = root + delta * 0.01
        if (intervalEnd - subdivision > delta && level > 0) {
            when (
                val result = searchLine(
                    minimizationFunction = minimizationFunction,
                    maximizationFunction = maximizationFunction,
                    fixed = fixed,
                    interval = doubleArrayOf(subdivision, intervalEnd),
                    direction = direction,
                    state = state,
                    level = level - 1,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/math/implicitplot.js -> curveContainsPoint.
    private fun curveContainsPoint(
        point: DoubleArray,
        dataX: List<Double>,
        dataY: List<Double>,
        tolerance: Double,
        epsilon: Double,
    ): Boolean {
        val x = point[1]
        val y = point[2]
        val hits = qdt.find(
            doubleArrayOf(
                x - epsilon,
                y + epsilon,
                x + epsilon,
                y - epsilon,
            ),
        )
        for (hit in hits) {
            val distance = Geometry.distPointSegment(
                point,
                doubleArrayOf(
                    1.0,
                    dataX[hit.index1],
                    dataY[hit.index1],
                ),
                doubleArrayOf(
                    1.0,
                    dataX[hit.index2],
                    dataY[hit.index2],
                ),
            )
            if (distance < tolerance) {
                return true
            }
        }
        return false
    }

    // JSXGraph 1.13.3: src/math/implicitplot.js -> traceComponent.
    internal fun traceComponent(
        initialPoint: DoubleArray,
    ): Pair<DoubleArray, DoubleArray> {
        var dataX = doubleArrayOf()
        var dataY = doubleArrayOf()
        val forward = tracing(initialPoint, 1)
        if (forward != null) {
            dataX = forward.pathX
            dataY = forward.pathY
        }

        if (forward?.loopClosed != true) {
            val backward = tracing(initialPoint, -1)
            if (backward != null) {
                dataX =
                    backward.pathX.reversedArray() +
                        dataX.copyOfRange(
                            fromIndex = min(1, dataX.size),
                            toIndex = dataX.size,
                        )
                dataY =
                    backward.pathY.reversedArray() +
                        dataY.copyOfRange(
                            fromIndex = min(1, dataY.size),
                            toIndex = dataY.size,
                        )
            }
        }

        if (dataX.isNotEmpty() && dataX.size < 6) {
            dataX += dataX.last()
            dataY += dataY.last()
        }
        return dataX to dataY
    }

    // JSXGraph 1.13.3: src/math/implicitplot.js -> tracing.
    internal fun tracing(
        initialPoint: DoubleArray,
        direction: Int,
    ): ImplicitTraceResult? {
        if (initialPoint.size < 3) {
            return null
        }
        var u = initialPoint.copyOfRange(1, 3)
        var v = u.copyOf()
        val pathX = mutableListOf(u[0])
        val pathY = mutableListOf(u[1])
        var tangentU = tangent(u) ?: return null
        var initialTangent = tangentU.copyOf()
        var a = doubleArrayOf(dfx(u[0], u[1]), dfy(u[0], u[1]))
        var omega = direction.toDouble()
        var stepWidth = config.hInitial
        var steps = 0
        var pointAdded = false
        var cuspOrBifurcation = false
        var loopClosed = false
        val gosperTable = mutableListOf<Int>()
        val quasiNewton = false

        do {
            val currentTangent =
                if (quasiNewton) tangentA(a) else tangent(u)
            if (currentTangent == null) {
                u = v.copyOf()
                pathX += u[0]
                pathY += u[1]
                break
            }
            tangentU = currentTangent

            when (pathX.size) {
                1 -> initialTangent = tangentU.copyOf()
                2 -> gosperTable += pathX.size - 1
                else -> if (pointAdded && !cuspOrBifurcation) {
                    val initialDistance = Geometry.distPointSegment(
                        doubleArrayOf(1.0, u[0], u[1]),
                        doubleArrayOf(1.0, pathX[0], pathY[0]),
                        doubleArrayOf(1.0, pathX[1], pathY[1]),
                    )
                    if (
                        initialDistance < config.loopDist * stepWidth &&
                        Mat.innerProduct(tangentU, initialTangent, 2) >
                        config.loopDir
                    ) {
                        u = initialPoint.copyOfRange(1, 3)
                        pathX += u[0]
                        pathY += u[1]
                        loopClosed = true
                        break
                    }

                    if (config.loopDetection) {
                        val n = pathX.size - 1
                        val maximumLevel = floor(Mat.log2(n.toDouble())).toInt()
                        var loopMatch = false
                        for (level in 0..maximumLevel) {
                            val tableIndex =
                                gosperTable.getOrNull(level) ?: continue
                            val distance = Geometry.distPointSegment(
                                doubleArrayOf(1.0, u[0], u[1]),
                                doubleArrayOf(
                                    1.0,
                                    pathX[tableIndex - 1],
                                    pathY[tableIndex - 1],
                                ),
                                doubleArrayOf(
                                    1.0,
                                    pathX[tableIndex],
                                    pathY[tableIndex],
                                ),
                            )
                            if (distance < config.loopDist * stepWidth) {
                                val tangentV = tangent(
                                    doubleArrayOf(
                                        pathX[tableIndex],
                                        pathY[tableIndex],
                                    ),
                                )
                                if (
                                    tangentV != null &&
                                    Mat.innerProduct(tangentU, tangentV, 2) >
                                    config.loopDir
                                ) {
                                    loopMatch = true
                                    break
                                }
                            }
                        }
                        if (loopMatch) {
                            loopClosed = true
                            break
                        }

                        var exponent = 0
                        var divisor = 1L
                        while (
                            exponent < 100 &&
                            (n + 1).toLong() % divisor == 0L
                        ) {
                            divisor *= 2
                            exponent += 1
                        }
                        while (gosperTable.size <= exponent) {
                            gosperTable += n
                        }
                        gosperTable[exponent] = n
                    }
                }
            }

            v[0] = u[0] + stepWidth * omega * tangentU[0]
            v[1] = u[1] + stepWidth * omega * tangentU[1]
            var predictor = v.copyOf()
            if (quasiNewton) {
                a = updateA(a, u, v)
                predictor = v.copyOf()
            }

            var iteration = 0
            var firstCorrection = Double.NaN
            var secondCorrection = Double.NaN
            do {
                val gradient =
                    if (quasiNewton) {
                        a
                    } else {
                        doubleArrayOf(dfx(v[0], v[1]), dfy(v[0], v[1]))
                    }
                val denominator =
                    gradient[0] * gradient[0] +
                        gradient[1] * gradient[1]
                val correction = f(v[0], v[1]) / denominator
                val correctionNorm =
                    abs(correction) * sqrt(denominator)
                if (iteration == 0) {
                    firstCorrection = correctionNorm
                } else if (iteration == 1) {
                    secondCorrection = correctionNorm
                }
                v[0] -= gradient[0] * correction
                v[1] -= gradient[1] * correction
                iteration += 1
            } while (
                iteration < MAX_NEWTON_STEPS &&
                abs(f(v[0], v[1])) > config.tolNewton
            )

            val delta = firstCorrection
            val kappa =
                if (iteration > 1) {
                    secondCorrection / firstCorrection
                } else {
                    0.0
                }
            val tangentV =
                if (quasiNewton) {
                    a = updateA(a, predictor, v)
                    tangentA(a)
                } else {
                    tangent(v) ?: doubleArrayOf(Double.NaN, Double.NaN)
                }
            var tangentDirection = Mat.innerProduct(tangentU, tangentV, 2)
            tangentDirection = max(-1.0, min(1.0, tangentDirection))
            var angle = acos(tangentDirection)

            cuspOrBifurcation = false
            val progress = Geometry.distance(u, v, 2)
            if (progress < config.tolProgress) {
                u = v.copyOf()
                pathX += u[0]
                pathY += u[1]
                break
            } else if (tangentDirection < 0.0) {
                if (stepWidth <= config.hCritical) {
                    cuspOrBifurcation = true
                    if (isBifurcation(u, config.tolCusp)) {
                        omega *= -1.0
                        angle = 0.0
                    } else {
                        u = v.copyOf()
                        pathX += u[0]
                        pathY += u[1]
                        break
                    }
                }
            }

            if (!cuspOrBifurcation) {
                var factor = max(
                    sqrt(kappa / config.kappa0),
                    max(
                        sqrt(delta / config.delta0),
                        angle / config.alpha0,
                    ),
                )
                if (factor.isNaN()) {
                    factor = 1.0
                }
                factor = max(min(factor, 2.0), 0.5)
                stepWidth /= factor
                stepWidth = min(config.hMax, stepWidth)
                if (factor >= 2.0) {
                    steps += 1
                    if (steps >= 3.0 * config.maxSteps) {
                        break
                    }
                    pointAdded = false
                    continue
                }
            }

            u = v.copyOf()
            pathX += u[0]
            pathY += u[1]
            pointAdded = true
            steps += 1
        } while (
            steps < config.maxSteps &&
            u[0] >= boundingBox[0] &&
            u[1] <= boundingBox[1] &&
            u[0] <= boundingBox[2] &&
            u[1] >= boundingBox[3]
        )

        clipLastPoint(pathX, pathY, u)
        return ImplicitTraceResult(
            pathX = pathX.toDoubleArray(),
            pathY = pathY.toDoubleArray(),
            loopClosed = loopClosed,
        )
    }

    // JSXGraph 1.13.3: src/math/implicitplot.js -> isBifurcation.
    internal fun isBifurcation(
        point: DoubleArray,
        tolerance: Double,
    ): Boolean {
        val h = Mat.eps * Mat.eps * 100.0
        val x = point[0]
        val y = point[1]
        val a = 0.5 * (dfx(x + h, y) - dfx(x - h, y)) / h
        val b = 0.5 * (dfx(x, y + h) - dfx(x, y - h)) / h
        val c = 0.5 * (dfy(x + h, y) - dfy(x - h, y)) / h
        val d = 0.5 * (dfy(x, y + h) - dfy(x, y - h)) / h
        val trace = a + d
        val discriminant = trace * trace - 4.0 * (a * d - b * c)
        val firstEigenvalue = 0.5 * (trace + sqrt(discriminant))
        val secondEigenvalue = 0.5 * (trace - sqrt(discriminant))
        return abs(firstEigenvalue) > tolerance &&
            abs(secondEigenvalue) > tolerance
    }

    // JSXGraph 1.13.3: src/math/implicitplot.js -> handleCriticalPoint.
    internal fun handleCriticalPoint(
        point: DoubleArray,
        tangent: DoubleArray,
        radius: Double,
        omega: Double,
    ): DoubleArray {
        val angle = atan2(omega * tangent[1], omega * tangent[0])
        val root = Numerics.root(
            function = { parameter ->
                val x = point[0] + radius * cos(parameter)
                val y = point[1] + radius * sin(parameter)
                f(x, y)
            },
            initialValue = angle,
        )
        return doubleArrayOf(
            point[0] + radius * cos(root),
            point[1] + radius * sin(root),
        )
    }

    // JSXGraph 1.13.3: src/math/implicitplot.js -> updateA.
    internal fun updateA(
        initialA: DoubleArray,
        initialPoint: DoubleArray,
        finalPoint: DoubleArray,
    ): DoubleArray {
        val a = initialA
        val displacement = doubleArrayOf(
            finalPoint[0] - initialPoint[0],
            finalPoint[1] - initialPoint[1],
        )
        val functionDifference =
            f(finalPoint[0], finalPoint[1]) -
                f(initialPoint[0], initialPoint[1])
        val denominator =
            displacement[0] * displacement[0] +
                displacement[1] * displacement[1]
        var numerator =
            functionDifference -
                (a[0] * displacement[0] + a[1] * displacement[1])
        numerator /= denominator
        a[0] += numerator * displacement[0]
        a[1] += numerator * displacement[1]
        return a
    }

    // JSXGraph 1.13.3: src/math/implicitplot.js -> tangent_A.
    internal fun tangentA(a: DoubleArray): DoubleArray {
        val tangent = doubleArrayOf(-a[1], a[0])
        val norm = Mat.norm(tangent, 2)
        return doubleArrayOf(tangent[0] / norm, tangent[1] / norm)
    }

    // JSXGraph 1.13.3: src/math/implicitplot.js -> tangent.
    internal fun tangent(point: DoubleArray): DoubleArray? {
        val tangent = doubleArrayOf(
            -dfy(point[0], point[1]),
            dfx(point[0], point[1]),
        )
        val norm = Mat.norm(tangent, 2)
        if (norm < Mat.eps * Mat.eps) {
            return null
        }
        return doubleArrayOf(tangent[0] / norm, tangent[1] / norm)
    }

    private fun clipLastPoint(
        pathX: MutableList<Double>,
        pathY: MutableList<Double>,
        point: DoubleArray,
    ) {
        if (pathX.size < 2 || pathY.size < 2) {
            return
        }
        val lastPoint = doubleArrayOf(
            pathX[pathX.lastIndex - 1],
            pathY[pathY.lastIndex - 1],
        )
        if (point[0] < boundingBox[0]) {
            if (point[0] != lastPoint[0]) {
                val parameter =
                    (boundingBox[0] - lastPoint[0]) /
                        (point[0] - lastPoint[0])
                if (point[1] != lastPoint[1]) {
                    point[1] =
                        lastPoint[1] +
                            parameter * (point[1] - lastPoint[1])
                }
            }
            point[0] = boundingBox[0]
        }
        if (point[0] > boundingBox[2]) {
            if (point[0] != lastPoint[0]) {
                val parameter =
                    (boundingBox[2] - lastPoint[0]) /
                        (point[0] - lastPoint[0])
                if (point[1] != lastPoint[1]) {
                    point[1] =
                        lastPoint[1] +
                            parameter * (point[1] - lastPoint[1])
                }
            }
            point[0] = boundingBox[2]
        }
        if (point[1] < boundingBox[3]) {
            if (point[1] != lastPoint[1]) {
                val parameter =
                    (boundingBox[3] - lastPoint[1]) /
                        (point[1] - lastPoint[1])
                if (point[0] != lastPoint[0]) {
                    point[0] =
                        lastPoint[0] +
                            parameter * (point[0] - lastPoint[0])
                }
            }
            point[1] = boundingBox[3]
        }
        if (point[1] > boundingBox[1]) {
            if (point[1] != lastPoint[1]) {
                val parameter =
                    (boundingBox[1] - lastPoint[1]) /
                        (point[1] - lastPoint[1])
                if (point[0] != lastPoint[0]) {
                    point[0] =
                        lastPoint[0] +
                            parameter * (point[0] - lastPoint[0])
                }
            }
            point[1] = boundingBox[1]
        }
        pathX[pathX.lastIndex] = point[0]
        pathY[pathY.lastIndex] = point[1]
    }

    private fun validateInput(): GMResult<Unit, ImplicitPlotError> {
        if (boundingBox.size < 4) {
            return GMResult.Err(
                ImplicitPlotError.InvalidBoundingBox(boundingBox.size),
            )
        }
        val positiveValues = listOf(
            "resolutionOuter" to config.resolutionOuter,
            "resolutionInner" to config.resolutionInner,
            "unitX" to config.unitX,
            "unitY" to config.unitY,
        )
        for ((name, value) in positiveValues) {
            if (!value.isFinite() || value <= 0.0) {
                return GMResult.Err(
                    ImplicitPlotError.InvalidConfiguration(name, value),
                )
            }
        }
        if (!config.maxSteps.isFinite() || config.maxSteps <= 0.0) {
            return GMResult.Err(
                ImplicitPlotError.InvalidConfiguration(
                    name = "maxSteps",
                    value = config.maxSteps,
                ),
            )
        }
        if (maximumPointCount <= 0) {
            return GMResult.Err(
                ImplicitPlotError.InvalidConfiguration(
                    name = "maximumPointCount",
                    value = maximumPointCount.toDouble(),
                ),
            )
        }
        return GMResult.Ok(Unit)
    }

    private enum class SearchDirection {
        VERTICAL,
        HORIZONTAL,
    }

    private companion object {
        const val MAX_SEARCH_LEVEL = 8
        const val MAX_NEWTON_STEPS = 20
    }
}
