/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/geometry.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sqrt

object Geometry {
    // JSXGraph: src/math/geometry.js -> angle
    fun angle(
        first: DoubleArray,
        vertex: DoubleArray,
        third: DoubleArray,
    ): Double {
        val firstX = first.valueOrNaN(0) - vertex.valueOrNaN(0)
        val firstY = first.valueOrNaN(1) - vertex.valueOrNaN(1)
        val thirdX = third.valueOrNaN(0) - vertex.valueOrNaN(0)
        val thirdY = third.valueOrNaN(1) - vertex.valueOrNaN(1)
        return atan2(
            firstX * thirdY - firstY * thirdX,
            firstX * thirdX + firstY * thirdY,
        )
    }

    // JSXGraph: src/math/geometry.js -> trueAngle
    fun trueAngle(
        first: DoubleArray,
        vertex: DoubleArray,
        third: DoubleArray,
    ): Double = rad(first, vertex, third) * 57.295779513082323

    // JSXGraph: src/math/geometry.js -> rad
    fun rad(
        first: DoubleArray,
        vertex: DoubleArray,
        third: DoubleArray,
    ): Double {
        var result =
            atan2(
                third.valueOrNaN(1) - vertex.valueOrNaN(1),
                third.valueOrNaN(0) - vertex.valueOrNaN(0),
            ) -
                atan2(
                    first.valueOrNaN(1) - vertex.valueOrNaN(1),
                    first.valueOrNaN(0) - vertex.valueOrNaN(0),
                )
        if (result < 0.0) {
            result += 6.2831853071795862
        }
        return result
    }

    // JSXGraph: src/math/geometry.js -> distance
    fun distance(
        first: DoubleArray,
        second: DoubleArray,
        requestedLength: Int? = null,
    ): Double {
        val length = if (requestedLength == null || requestedLength == 0) {
            minOf(first.size, second.size)
        } else {
            requestedLength
        }
        var sum = 0.0
        for (index in 0 until length) {
            val difference = first.valueOrNaN(index) - second.valueOrNaN(index)
            sum += difference * difference
        }
        return sqrt(sum)
    }

    // JSXGraph: src/math/geometry.js -> affineDistance
    fun affineDistance(
        first: DoubleArray,
        second: DoubleArray,
        requestedLength: Int? = null,
    ): Double {
        val result = distance(first, second, requestedLength)
        if (
            result > Mat.eps &&
            (
                abs(first.valueOrNaN(0)) < Mat.eps ||
                    abs(second.valueOrNaN(0)) < Mat.eps
            )
        ) {
            return Double.POSITIVE_INFINITY
        }
        return result
    }

    // JSXGraph: src/math/geometry.js -> affineRatio
    fun affineRatio(
        first: DoubleArray,
        second: DoubleArray,
        third: DoubleArray,
    ): Double {
        val horizontalDifference = second.valueOrNaN(1) - first.valueOrNaN(1)
        return if (abs(horizontalDifference) > Mat.eps) {
            (third.valueOrNaN(1) - first.valueOrNaN(1)) / horizontalDifference
        } else {
            (third.valueOrNaN(2) - first.valueOrNaN(2)) /
                (second.valueOrNaN(2) - first.valueOrNaN(2))
        }
    }

    // JSXGraph: src/math/geometry.js -> signedTriangle
    fun signedTriangle(
        first: DoubleArray,
        second: DoubleArray,
        third: DoubleArray,
    ): Double =
        0.5 *
            (
                (second.valueOrNaN(1) - first.valueOrNaN(1)) *
                    (third.valueOrNaN(2) - first.valueOrNaN(2)) -
                    (second.valueOrNaN(2) - first.valueOrNaN(2)) *
                    (third.valueOrNaN(1) - first.valueOrNaN(1))
            )

    // JSXGraph: src/math/geometry.js -> calcLabelQuadrant
    fun calcLabelQuadrant(inputAngle: Double): String {
        var angle = inputAngle
        if (angle < 0.0) {
            angle += 2.0 * PI
        }
        val quadrant = floor((angle + PI / 8.0) / (PI / 4.0)).toInt() % 8
        return arrayOf("rt", "urt", "top", "ulft", "lft", "llft", "bot", "lrt")[quadrant]
    }

    // JSXGraph: src/math/geometry.js -> isSameDir
    fun isSameDir(
        firstStart: DoubleArray,
        firstEnd: DoubleArray,
        secondStart: DoubleArray,
        secondEnd: DoubleArray,
    ): Boolean {
        var firstX = firstEnd.valueOrNaN(1) - firstStart.valueOrNaN(1)
        var firstY = firstEnd.valueOrNaN(2) - firstStart.valueOrNaN(2)
        val secondX = secondEnd.valueOrNaN(1) - secondStart.valueOrNaN(1)
        val secondY = secondEnd.valueOrNaN(2) - secondStart.valueOrNaN(2)

        if (abs(firstEnd.valueOrNaN(0)) < Mat.eps) {
            firstX = firstEnd.valueOrNaN(1)
            firstY = firstEnd.valueOrNaN(2)
        }
        if (abs(firstStart.valueOrNaN(0)) < Mat.eps) {
            firstX = -firstStart.valueOrNaN(1)
            firstY = -firstStart.valueOrNaN(2)
        }
        return firstX * secondX + firstY * secondY >= 0.0
    }

    // JSXGraph: src/math/geometry.js -> isSameDirection
    fun isSameDirection(
        start: DoubleArray,
        point: DoubleArray,
        visiblePoint: DoubleArray,
    ): Boolean {
        var directionX = point.valueOrNaN(1) - start.valueOrNaN(1)
        var directionY = point.valueOrNaN(2) - start.valueOrNaN(2)
        var visibleX = visiblePoint.valueOrNaN(1) - start.valueOrNaN(1)
        var visibleY = visiblePoint.valueOrNaN(2) - start.valueOrNaN(2)

        if (abs(directionX) < Mat.eps) directionX = 0.0
        if (abs(directionY) < Mat.eps) directionY = 0.0
        if (abs(visibleX) < Mat.eps) visibleX = 0.0
        if (abs(visibleY) < Mat.eps) visibleY = 0.0

        return (
            (directionX >= 0.0 && visibleX >= 0.0) ||
                (directionX <= 0.0 && visibleX <= 0.0)
        ) &&
            (
                (directionY >= 0.0 && visibleY >= 0.0) ||
                    (directionY <= 0.0 && visibleY <= 0.0)
            )
    }

    // JSXGraph: src/math/geometry.js -> det3p
    fun det3p(
        first: DoubleArray,
        second: DoubleArray,
        point: DoubleArray,
    ): Double =
        (first.valueOrNaN(1) - point.valueOrNaN(1)) *
            (second.valueOrNaN(2) - point.valueOrNaN(2)) -
            (second.valueOrNaN(1) - point.valueOrNaN(1)) *
            (first.valueOrNaN(2) - point.valueOrNaN(2))

    // JSXGraph: src/math/geometry.js -> distPointLine
    fun distPointLine(
        point: DoubleArray,
        line: DoubleArray,
    ): Double {
        var horizontalCoefficient = line.valueOrNaN(1)
        var verticalCoefficient = line.valueOrNaN(2)
        val constant = line.valueOrNaN(0)
        if (abs(horizontalCoefficient) + abs(verticalCoefficient) < Mat.eps) {
            return Double.POSITIVE_INFINITY
        }

        val numerator =
            horizontalCoefficient * point.valueOrNaN(1) +
                verticalCoefficient * point.valueOrNaN(2) +
                constant
        horizontalCoefficient *= horizontalCoefficient
        verticalCoefficient *= verticalCoefficient
        return abs(numerator) / sqrt(horizontalCoefficient + verticalCoefficient)
    }

    // JSXGraph: src/math/geometry.js -> distPointSegment
    fun distPointSegment(
        point: DoubleArray,
        first: DoubleArray,
        second: DoubleArray,
    ): Double {
        var horizontal = clampInfiniteDifference(
            point.valueOrNaN(1) - first.valueOrNaN(1),
        )
        var vertical = clampInfiniteDifference(
            point.valueOrNaN(2) - first.valueOrNaN(2),
        )
        val segmentX = clampInfiniteDifference(
            second.valueOrNaN(1) - first.valueOrNaN(1),
        )
        val segmentY = clampInfiniteDifference(
            second.valueOrNaN(2) - first.valueOrNaN(2),
        )

        val squaredLength = segmentX * segmentX + segmentY * segmentY
        if (squaredLength > Mat.eps * Mat.eps) {
            var ratio = (horizontal * segmentX + vertical * segmentY) / squaredLength
            ratio = ratio.coerceIn(0.0, 1.0)
            horizontal -= ratio * segmentX
            vertical -= ratio * segmentY
        }
        return Mat.hypot(horizontal, vertical)
    }

    private fun clampInfiniteDifference(value: Double): Double = when (value) {
        Double.POSITIVE_INFINITY -> 1_000_000.0
        Double.NEGATIVE_INFINITY -> -1_000_000.0
        else -> value
    }

    private fun DoubleArray.valueOrNaN(index: Int): Double =
        if (index in indices) this[index] else Double.NaN
}
