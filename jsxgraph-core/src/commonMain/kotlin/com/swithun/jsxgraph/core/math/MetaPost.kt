/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/metapost.js -> Metapost
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * MetaPost/Hobby translation lineage includes the PyX port by Michael
 * Schindler, the JavaScript port by Vlad-X, and JSXGraph adaptations by
 * Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal data class MetaPostPoint(
    val x: Double,
    val y: Double,
)

internal data class MetaPostControlPair(
    val left: Double?,
    val right: Double?,
)

internal data class MetaPostPointControl(
    val index: Int,
    val type: String? = null,
    val curl: Double? = null,
    val direction: MetaPostControlPair? = null,
    val tension: MetaPostControlPair? = null,
)

internal data class MetaPostControls(
    val tension: Double = 1.0,
    val isClosed: Boolean = false,
    val pointControls: List<MetaPostPointControl> = emptyList(),
)

internal data class MetaPostCurveData(
    val x: DoubleArray,
    val y: DoubleArray,
)

internal object MetaPost {
    private const val MP_ENDPOINT = 0
    private const val MP_EXPLICIT = 1
    private const val MP_GIVEN = 2
    private const val MP_CURL = 3
    private const val MP_OPEN = 4
    private const val MP_END_CYCLE = 5

    private const val UNITY = 1.0
    private const val FRACTION_ONE = 1.0
    private const val FRACTION_THREE = 3.0
    private const val ONE_EIGHTY_DEG = PI
    private const val THREE_SIXTY_DEG = 2.0 * PI
    private const val EPS_SQ = 1.0e-10

    private class Knot(
        val x: Double,
        val y: Double,
        tension: Double,
    ) {
        var ltype: Int = MP_OPEN
        var rtype: Int = MP_OPEN
        var lx: Double = 0.0
        var rx: Double = 0.0
        var ly: Double = tension
        var ry: Double = tension
        lateinit var next: Knot

        fun leftCurl(): Double = jsOr(lx, 0.0)

        fun rightCurl(): Double = jsOr(rx, 0.0)

        fun leftTension(): Double = jsOr(ly, 1.0)

        fun rightTension(): Double = jsOr(ry, 1.0)

        fun setRightCurl(value: Double) {
            rx = jsOr(value, 0.0)
        }

        fun setLeftCurl(value: Double) {
            lx = jsOr(value, 0.0)
        }

        fun setRightGiven(value: Double) {
            setRightCurl(value)
        }

        fun setLeftGiven(value: Double) {
            setLeftCurl(value)
        }

        fun rightGiven(): Double = rightCurl()

        fun leftGiven(): Double = leftCurl()

        private fun jsOr(
            value: Double,
            fallback: Double,
        ): Double =
            if (value == 0.0 || value.isNaN()) fallback else value
    }

    // JSXGraph 1.13.3: src/math/metapost.js -> curve.
    internal fun curve(
        pointList: List<MetaPostPoint>,
        controls: MetaPostControls = MetaPostControls(),
    ): MetaPostCurveData {
        val knots = makeKnots(pointList, controls.tension)
        val length = knots.size
        if (!controls.isClosed) {
            knots[0].ltype = MP_ENDPOINT
            knots[0].rtype = MP_CURL
            knots[length - 1].rtype = MP_ENDPOINT
            knots[length - 1].ltype = MP_CURL
        }

        for (control in controls.pointControls) {
            if (control.index in knots.indices) {
                val knot = knots[control.index]
                if (control.type == "curl") {
                    val curl = control.curl ?: 0.0
                    when (control.index) {
                        0 -> {
                            knot.rtype = MP_CURL
                            knot.setRightCurl(curl)
                        }
                        length - 1 -> {
                            knot.ltype = MP_CURL
                            knot.setLeftCurl(curl)
                        }
                        else -> {
                            knot.ltype = MP_CURL
                            knot.rtype = MP_CURL
                            knot.lx = curl
                            knot.rx = curl
                        }
                    }
                }
                control.direction?.let { direction ->
                    direction.left?.let { value ->
                        knot.lx = value * PI / 180.0
                        knot.ltype = MP_GIVEN
                    }
                    direction.right?.let { value ->
                        knot.rx = value * PI / 180.0
                        knot.rtype = MP_GIVEN
                    }
                }
                control.tension?.let { tension ->
                    tension.left?.let { knot.ly = it }
                    tension.right?.let { knot.ry = it }
                }
            }
        }

        makeChoices(knots)

        val coordinateCount =
            if (controls.isClosed) length * 3 + 1
            else (length - 1) * 3 + 1
        val x = DoubleArray(coordinateCount)
        val y = DoubleArray(coordinateCount)
        var outputIndex = 0
        for (index in 0 until length - 1) {
            x[outputIndex] = knots[index].x
            y[outputIndex++] = knots[index].y
            x[outputIndex] = knots[index].rx
            y[outputIndex++] = knots[index].ry
            x[outputIndex] = knots[index + 1].lx
            y[outputIndex++] = knots[index + 1].ly
        }
        x[outputIndex] = knots[length - 1].x
        y[outputIndex++] = knots[length - 1].y
        if (controls.isClosed) {
            x[outputIndex] = knots[length - 1].rx
            y[outputIndex++] = knots[length - 1].ry
            x[outputIndex] = knots[0].lx
            y[outputIndex++] = knots[0].ly
            x[outputIndex] = knots[0].x
            y[outputIndex] = knots[0].y
        }
        return MetaPostCurveData(x = x, y = y)
    }

    // JSXGraph 1.13.3: src/math/metapost.js -> makeknots.
    private fun makeKnots(
        points: List<MetaPostPoint>,
        tension: Double,
    ): List<Knot> {
        val knots = points.map { point ->
            Knot(
                x = point.x,
                y = point.y,
                tension = tension,
            )
        }
        for (index in knots.indices) {
            knots[index].next = knots.getOrElse(index + 1) { knots[index] }
        }
        knots[knots.lastIndex].next = knots[0]
        return knots
    }

    // JSXGraph 1.13.3: src/math/metapost.js -> make_choices.
    private fun makeChoices(knots: List<Knot>) {
        var point = knots[0]
        do {
            val next = point.next
            if (
                point.rtype > MP_EXPLICIT &&
                (point.x - next.x) * (point.x - next.x) +
                    (point.y - next.y) * (point.y - next.y) < EPS_SQ
            ) {
                point.rtype = MP_EXPLICIT
                if (point.ltype == MP_OPEN) {
                    point.ltype = MP_CURL
                    point.setLeftCurl(UNITY)
                }
                next.ltype = MP_EXPLICIT
                if (next.rtype == MP_OPEN) {
                    next.rtype = MP_CURL
                    next.setRightCurl(UNITY)
                }
                point.rx = point.x
                next.lx = point.x
                point.ry = point.y
                next.ly = point.y
            }
            point = next
        } while (point !== knots[0])

        var breakpoint = knots[0]
        while (
            breakpoint.ltype == MP_OPEN &&
            breakpoint.rtype == MP_OPEN
        ) {
            breakpoint = breakpoint.next
            if (breakpoint === knots[0]) {
                breakpoint.ltype = MP_END_CYCLE
                break
            }
        }

        point = breakpoint
        while (true) {
            var next = point.next
            if (point.rtype >= MP_GIVEN) {
                while (
                    next.ltype == MP_OPEN &&
                    next.rtype == MP_OPEN
                ) {
                    next = next.next
                }

                var index = 0
                var segment = point
                var pathLength = knots.size
                val deltaX = mutableListOf<Double>()
                val deltaY = mutableListOf<Double>()
                val delta = mutableListOf<Double>()
                val psi = mutableListOf(Double.NaN)
                while (true) {
                    val target = segment.next
                    deltaX += target.x - segment.x
                    deltaY += target.y - segment.y
                    delta += Mat.hypot(deltaX[index], deltaY[index])
                    if (index > 0) {
                        val sine = deltaY[index - 1] / delta[index - 1]
                        val cosine = deltaX[index - 1] / delta[index - 1]
                        psi += atan2(
                            deltaY[index] * cosine -
                                deltaX[index] * sine,
                            deltaX[index] * cosine +
                                deltaY[index] * sine,
                        )
                    }
                    index += 1
                    segment = target
                    if (segment === next) {
                        pathLength = index
                    }
                    if (
                        index >= pathLength &&
                        segment.ltype != MP_END_CYCLE
                    ) {
                        break
                    }
                }
                psi += if (index == pathLength) 0.0 else psi[1]

                if (next.ltype == MP_OPEN) {
                    val deltaControlX = next.rx - next.x
                    val deltaControlY = next.ry - next.y
                    if (
                        deltaControlX * deltaControlX +
                        deltaControlY * deltaControlY < EPS_SQ
                    ) {
                        next.ltype = MP_CURL
                        next.setLeftCurl(UNITY)
                    } else {
                        next.ltype = MP_GIVEN
                        next.setLeftGiven(
                            atan2(deltaControlY, deltaControlX),
                        )
                    }
                }
                if (
                    point.rtype == MP_OPEN &&
                    point.ltype == MP_EXPLICIT
                ) {
                    val deltaControlX = point.x - point.lx
                    val deltaControlY = point.y - point.ly
                    if (
                        deltaControlX * deltaControlX +
                        deltaControlY * deltaControlY < EPS_SQ
                    ) {
                        point.rtype = MP_CURL
                        point.setRightCurl(UNITY)
                    } else {
                        point.rtype = MP_GIVEN
                        point.setRightGiven(
                            atan2(deltaControlY, deltaControlX),
                        )
                    }
                }
                solveChoices(
                    point = point,
                    endpoint = next,
                    pathLength = pathLength,
                    deltaX = deltaX,
                    deltaY = deltaY,
                    delta = delta,
                    psi = psi,
                )
            } else if (point.rtype == MP_ENDPOINT) {
                point.rx = point.x
                point.ry = point.y
                next.lx = next.x
                next.ly = next.y
            }
            point = next
            if (point === breakpoint) {
                break
            }
        }
    }

    // JSXGraph 1.13.3: src/math/metapost.js -> mp_solve_choices.
    private fun solveChoices(
        point: Knot,
        endpoint: Knot,
        pathLength: Int,
        deltaX: List<Double>,
        deltaY: List<Double>,
        delta: List<Double>,
        psi: List<Double>,
    ) {
        val arrayLength = delta.size + 1
        val uu = DoubleArray(arrayLength)
        val ww = DoubleArray(arrayLength)
        val vv = DoubleArray(arrayLength)
        val theta = DoubleArray(arrayLength)
        var index = 0
        var segment = point
        var previous = point
        while (true) {
            val target = segment.next
            if (index == 0) {
                when (segment.rtype) {
                    MP_GIVEN -> {
                        if (target.ltype == MP_GIVEN) {
                            val angle = atan2(deltaY[0], deltaX[0])
                            val (ct, st) = sinCos(
                                point.rightGiven() - angle,
                            )
                            val (cf, sf) = sinCos(
                                endpoint.leftGiven() - angle,
                            )
                            setControls(
                                point = point,
                                endpoint = endpoint,
                                deltaX = deltaX[0],
                                deltaY = deltaY[0],
                                st = st,
                                ct = ct,
                                sf = -sf,
                                cf = cf,
                            )
                            return
                        }
                        vv[0] = reduceAngle(
                            segment.rightGiven() -
                                atan2(deltaY[0], deltaX[0]),
                        )
                        uu[0] = 0.0
                        ww[0] = 0.0
                    }
                    MP_CURL -> {
                        if (target.ltype == MP_CURL) {
                            point.rtype = MP_EXPLICIT
                            endpoint.ltype = MP_EXPLICIT
                            val leftTension =
                                abs(endpoint.leftTension())
                            val rightTension =
                                abs(point.rightTension())
                            var factor = UNITY / (3.0 * rightTension)
                            point.rx = point.x + deltaX[0] * factor
                            point.ry = point.y + deltaY[0] * factor
                            factor = UNITY / (3.0 * leftTension)
                            endpoint.lx =
                                endpoint.x - deltaX[0] * factor
                            endpoint.ly =
                                endpoint.y - deltaY[0] * factor
                            return
                        }
                        val curl = segment.rightCurl()
                        val leftTension = abs(target.leftTension())
                        val rightTension = abs(segment.rightTension())
                        uu[0] = curlRatio(
                            gamma = curl,
                            aTension = rightTension,
                            bTension = leftTension,
                        )
                        vv[0] = -psi[1] * uu[0]
                        ww[0] = 0.0
                    }
                    MP_OPEN -> {
                        uu[0] = 0.0
                        vv[0] = 0.0
                        ww[0] = FRACTION_ONE
                    }
                }
            } else if (
                segment.ltype == MP_END_CYCLE ||
                segment.ltype == MP_OPEN
            ) {
                val aa =
                    UNITY /
                        (
                            3.0 * abs(previous.rightTension()) -
                                UNITY
                            )
                var dd =
                    delta[index] *
                        (
                            FRACTION_THREE -
                                UNITY / abs(previous.rightTension())
                            )
                val bb =
                    UNITY /
                        (
                            3.0 * abs(target.leftTension()) -
                                UNITY
                            )
                var ee =
                    delta[index - 1] *
                        (
                            FRACTION_THREE -
                                UNITY / abs(target.leftTension())
                            )
                val cc = FRACTION_ONE - uu[index - 1] * aa
                dd *= cc
                val leftTension = abs(segment.leftTension())
                val rightTension = abs(segment.rightTension())
                if (leftTension < rightTension) {
                    dd *= (leftTension / rightTension).pow(2)
                } else if (leftTension > rightTension) {
                    ee *= (rightTension / leftTension).pow(2)
                }
                var factor = ee / (ee + dd)
                uu[index] = factor * bb
                var accumulator = -psi[index + 1] * uu[index]
                if (previous.rtype == MP_CURL) {
                    ww[index] = 0.0
                    vv[index] =
                        accumulator -
                            psi[1] * (FRACTION_ONE - factor)
                } else {
                    factor = (FRACTION_ONE - factor) / cc
                    accumulator -= psi[index] * factor
                    factor *= aa
                    vv[index] =
                        accumulator - vv[index - 1] * factor
                    ww[index] = -ww[index - 1] * factor
                }
                if (segment.ltype == MP_END_CYCLE) {
                    var cycleA = 0.0
                    var cycleB = FRACTION_ONE
                    while (true) {
                        index -= 1
                        if (index == 0) {
                            index = pathLength
                        }
                        cycleA = vv[index] - cycleA * uu[index]
                        cycleB = ww[index] - cycleB * uu[index]
                        if (index == pathLength) {
                            break
                        }
                    }
                    cycleA /= FRACTION_ONE - cycleB
                    theta[pathLength] = cycleA
                    vv[0] = cycleA
                    for (
                        cycleIndex in 1 until pathLength
                    ) {
                        vv[cycleIndex] +=
                            cycleA * ww[cycleIndex]
                    }
                    break
                }
            } else {
                if (segment.ltype == MP_CURL) {
                    val curl = segment.leftCurl()
                    val leftTension = abs(segment.leftTension())
                    val rightTension =
                        abs(previous.rightTension())
                    val factor = curlRatio(
                        gamma = curl,
                        aTension = leftTension,
                        bTension = rightTension,
                    )
                    theta[pathLength] =
                        -(vv[pathLength - 1] * factor) /
                        (
                            FRACTION_ONE -
                                factor * uu[pathLength - 1]
                            )
                    break
                }
                if (segment.ltype == MP_GIVEN) {
                    theta[pathLength] = reduceAngle(
                        segment.leftGiven() -
                            atan2(
                                deltaY[pathLength - 1],
                                deltaX[pathLength - 1],
                            ),
                    )
                    break
                }
            }
            previous = segment
            segment = target
            index += 1
        }

        for (backwardIndex in pathLength - 1 downTo 0) {
            theta[backwardIndex] =
                vv[backwardIndex] -
                    theta[backwardIndex + 1] * uu[backwardIndex]
        }

        segment = point
        index = 0
        while (true) {
            val target = segment.next
            val (ct, st) = sinCos(theta[index])
            val (cf, sf) = sinCos(
                -psi[index + 1] - theta[index + 1],
            )
            setControls(
                point = segment,
                endpoint = target,
                deltaX = deltaX[index],
                deltaY = deltaY[index],
                st = st,
                ct = ct,
                sf = sf,
                cf = cf,
            )
            index += 1
            segment = target
            if (index == pathLength) {
                break
            }
        }
    }

    // JSXGraph 1.13.3: src/math/metapost.js -> mp_n_sin_cos.
    private fun sinCos(value: Double): Pair<Double, Double> =
        cos(value) to sin(value)

    // JSXGraph 1.13.3: src/math/metapost.js -> mp_set_controls.
    private fun setControls(
        point: Knot,
        endpoint: Knot,
        deltaX: Double,
        deltaY: Double,
        st: Double,
        ct: Double,
        sf: Double,
        cf: Double,
    ) {
        val leftTension = abs(endpoint.leftTension())
        val rightTension = abs(point.rightTension())
        var rightVelocity = velocity(
            st = st,
            ct = ct,
            sf = sf,
            cf = cf,
            tension = rightTension,
        )
        var leftVelocity = velocity(
            st = sf,
            ct = cf,
            sf = st,
            cf = ct,
            tension = leftTension,
        )
        if (
            point.rightTension() < 0.0 ||
            endpoint.leftTension() < 0.0
        ) {
            if (
                (st >= 0.0 && sf >= 0.0) ||
                (st <= 0.0 && sf <= 0.0)
            ) {
                var sine = abs(st) * cf + abs(sf) * ct
                if (sine > 0.0) {
                    sine *= 1.00024414062
                    if (
                        point.rightTension() < 0.0 &&
                        compareProducts(
                            abs(sf),
                            FRACTION_ONE,
                            rightVelocity,
                            sine,
                        ) < 0
                    ) {
                        rightVelocity = abs(sf) / sine
                    }
                    if (
                        endpoint.leftTension() < 0.0 &&
                        compareProducts(
                            abs(st),
                            FRACTION_ONE,
                            leftVelocity,
                            sine,
                        ) < 0
                    ) {
                        leftVelocity = abs(st) / sine
                    }
                }
            }
        }
        point.rx =
            point.x +
                (deltaX * ct - deltaY * st) * rightVelocity
        point.ry =
            point.y +
                (deltaY * ct + deltaX * st) * rightVelocity
        endpoint.lx =
            endpoint.x -
                (deltaX * cf + deltaY * sf) * leftVelocity
        endpoint.ly =
            endpoint.y -
                (deltaY * cf - deltaX * sf) * leftVelocity
        point.rtype = MP_EXPLICIT
        endpoint.ltype = MP_EXPLICIT
    }

    // JSXGraph 1.13.3: src/math/metapost.js -> mp_curl_ratio.
    private fun curlRatio(
        gamma: Double,
        aTension: Double,
        bTension: Double,
    ): Double {
        val alpha = 1.0 / aTension
        val beta = 1.0 / bTension
        return min(
            4.0,
            (
                (3.0 - alpha) * alpha * alpha * gamma +
                    beta * beta * beta
                ) /
                (
                    alpha * alpha * alpha * gamma +
                        (3.0 - beta) * beta * beta
                    ),
        )
    }

    // JSXGraph 1.13.3: src/math/metapost.js -> mp_ab_vs_cd.
    private fun compareProducts(
        a: Double,
        b: Double,
        c: Double,
        d: Double,
    ): Int =
        when {
            a * b == c * d -> 0
            a * b > c * d -> 1
            else -> -1
        }

    // JSXGraph 1.13.3: src/math/metapost.js -> mp_velocity.
    private fun velocity(
        st: Double,
        ct: Double,
        sf: Double,
        cf: Double,
        tension: Double,
    ): Double =
        min(
            4.0,
            (
                2.0 +
                    sqrt(2.0) *
                    (st - sf / 16.0) *
                    (sf - st / 16.0) *
                    (ct - cf)
                ) /
                (
                    1.5 *
                        tension *
                        (
                            2.0 +
                                (sqrt(5.0) - 1.0) * ct +
                                (3.0 - sqrt(5.0)) * cf
                            )
                    ),
        )

    // JSXGraph 1.13.3: src/math/metapost.js -> reduce_angle.
    private fun reduceAngle(value: Double): Double {
        var result = value
        if (abs(result) > ONE_EIGHTY_DEG) {
            result +=
                if (result > 0.0) -THREE_SIXTY_DEG
                else THREE_SIXTY_DEG
        }
        return result
    }
}
