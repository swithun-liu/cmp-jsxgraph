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

data class SegmentIntersection(
    val point: DoubleArray,
    val firstParameter: Double,
    val secondParameter: Double,
)

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

    // JSXGraph: src/math/geometry.js -> meetLineLine
    fun meetLineLine(
        firstLine: DoubleArray,
        secondLine: DoubleArray,
    ): DoubleArray {
        val result = if (
            (firstLine.valueOrNaN(5) + secondLine.valueOrNaN(5)).isNaN()
        ) {
            doubleArrayOf(0.0, 0.0, 0.0)
        } else {
            crossProduct(firstLine, secondLine)
        }
        if (abs(result[0]) < 1.0e-14) {
            result[0] = 0.0
        }
        return result
    }

    // JSXGraph: src/math/geometry.js -> meetLineCircle
    fun meetLineCircle(
        line: DoubleArray,
        circle: DoubleArray,
        intersectionIndex: Int,
    ): DoubleArray {
        if (circle.valueOrNaN(4) < Mat.eps) {
            val centerX = circle.valueOrNaN(6)
            val centerY = circle.valueOrNaN(7)
            val lineValue =
                line.valueOrNaN(0) +
                    centerX * line.valueOrNaN(1) +
                    centerY * line.valueOrNaN(2)
            return if (abs(lineValue) < Mat.eps) {
                doubleArrayOf(1.0, centerX, centerY)
            } else {
                doubleArrayOf(1.0, Double.NaN, Double.NaN)
            }
        }

        val constant = circle.valueOrNaN(0)
        val horizontal = circle.valueOrNaN(1)
        val vertical = circle.valueOrNaN(2)
        val quadratic = circle.valueOrNaN(3)
        val lineConstant = line.valueOrNaN(0)
        val lineHorizontal = line.valueOrNaN(1)
        val lineVertical = line.valueOrNaN(2)

        val linear =
            horizontal * lineVertical - vertical * lineHorizontal
        val scalar =
            quadratic * lineConstant * lineConstant -
                (
                    horizontal * lineHorizontal +
                        vertical * lineVertical
                ) * lineConstant +
                constant
        var discriminant = linear * linear - 4.0 * quadratic * scalar
        if (discriminant > -Mat.eps * Mat.eps) {
            discriminant = sqrt(abs(discriminant))
            val parameter = if (intersectionIndex == 0) {
                (-linear + discriminant) / (2.0 * quadratic)
            } else {
                (-linear - discriminant) / (2.0 * quadratic)
            }
            return doubleArrayOf(
                1.0,
                -parameter * -lineVertical - lineConstant * lineHorizontal,
                -parameter * lineHorizontal - lineConstant * lineVertical,
            )
        }
        return doubleArrayOf(0.0, 0.0, 0.0)
    }

    // JSXGraph: src/math/geometry.js -> meetCircleCircle
    fun meetCircleCircle(
        firstCircle: DoubleArray,
        secondCircle: DoubleArray,
        intersectionIndex: Int,
    ): DoubleArray {
        if (firstCircle.valueOrNaN(4) < Mat.eps) {
            val center = doubleArrayOf(
                firstCircle.valueOrNaN(6),
                firstCircle.valueOrNaN(7),
            )
            val otherCenter = doubleArrayOf(
                secondCircle.valueOrNaN(6),
                secondCircle.valueOrNaN(7),
            )
            return if (
                abs(distance(center, otherCenter) - secondCircle.valueOrNaN(5)) <
                Mat.eps
            ) {
                doubleArrayOf(1.0, center[0], center[1])
            } else {
                doubleArrayOf(0.0, 0.0, 0.0)
            }
        }
        if (secondCircle.valueOrNaN(4) < Mat.eps) {
            val center = doubleArrayOf(
                secondCircle.valueOrNaN(6),
                secondCircle.valueOrNaN(7),
            )
            val otherCenter = doubleArrayOf(
                firstCircle.valueOrNaN(6),
                firstCircle.valueOrNaN(7),
            )
            return if (
                abs(distance(center, otherCenter) - firstCircle.valueOrNaN(5)) <
                Mat.eps
            ) {
                doubleArrayOf(1.0, center[0], center[1])
            } else {
                doubleArrayOf(0.0, 0.0, 0.0)
            }
        }

        val radicalAxis = doubleArrayOf(
            secondCircle.valueOrNaN(3) * firstCircle.valueOrNaN(0) -
                firstCircle.valueOrNaN(3) * secondCircle.valueOrNaN(0),
            secondCircle.valueOrNaN(3) * firstCircle.valueOrNaN(1) -
                firstCircle.valueOrNaN(3) * secondCircle.valueOrNaN(1),
            secondCircle.valueOrNaN(3) * firstCircle.valueOrNaN(2) -
                firstCircle.valueOrNaN(3) * secondCircle.valueOrNaN(2),
            0.0,
            1.0,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
        )
        Mat.normalize(radicalAxis)
        return meetLineCircle(radicalAxis, firstCircle, intersectionIndex)
    }

    // JSXGraph: src/math/geometry.js -> meetSegmentSegment
    fun meetSegmentSegment(
        firstStart: DoubleArray,
        firstEnd: DoubleArray,
        secondStart: DoubleArray,
        secondEnd: DoubleArray,
    ): SegmentIntersection {
        val firstLine = crossProduct(firstStart, firstEnd)
        val secondLine = crossProduct(secondStart, secondEnd)
        val point = crossProduct(firstLine, secondLine)
        if (abs(point[0]) < Mat.eps) {
            return SegmentIntersection(
                point = point,
                firstParameter = Double.POSITIVE_INFINITY,
                secondParameter = Double.POSITIVE_INFINITY,
            )
        }

        val homogeneous = point[0]
        point[1] /= homogeneous
        point[2] /= homogeneous
        point[0] /= homogeneous

        var coordinateIndex =
            if (
                abs(
                    firstEnd.valueOrNaN(1) -
                        firstEnd.valueOrNaN(0) * firstStart.valueOrNaN(1),
                ) < Mat.eps
            ) {
                2
            } else {
                1
            }
        var startValue =
            firstStart.valueOrNaN(coordinateIndex) / firstStart.valueOrNaN(0)
        val firstParameter =
            (point[coordinateIndex] - startValue) /
                if (firstEnd.valueOrNaN(0) != 0.0) {
                    firstEnd.valueOrNaN(coordinateIndex) /
                        firstEnd.valueOrNaN(0) -
                        startValue
                } else {
                    firstEnd.valueOrNaN(coordinateIndex)
                }

        coordinateIndex =
            if (
                abs(
                    secondEnd.valueOrNaN(1) -
                        secondEnd.valueOrNaN(0) * secondStart.valueOrNaN(1),
                ) < Mat.eps
            ) {
                2
            } else {
                1
            }
        startValue =
            secondStart.valueOrNaN(coordinateIndex) / secondStart.valueOrNaN(0)
        val secondParameter =
            (point[coordinateIndex] - startValue) /
                if (secondEnd.valueOrNaN(0) != 0.0) {
                    secondEnd.valueOrNaN(coordinateIndex) /
                        secondEnd.valueOrNaN(0) -
                        startValue
                } else {
                    secondEnd.valueOrNaN(coordinateIndex)
                }
        return SegmentIntersection(point, firstParameter, secondParameter)
    }

    private fun clampInfiniteDifference(value: Double): Double = when (value) {
        Double.POSITIVE_INFINITY -> 1_000_000.0
        Double.NEGATIVE_INFINITY -> -1_000_000.0
        else -> value
    }

    private fun crossProduct(
        first: DoubleArray,
        second: DoubleArray,
    ): DoubleArray = doubleArrayOf(
        first.valueOrNaN(1) * second.valueOrNaN(2) -
            first.valueOrNaN(2) * second.valueOrNaN(1),
        first.valueOrNaN(2) * second.valueOrNaN(0) -
            first.valueOrNaN(0) * second.valueOrNaN(2),
        first.valueOrNaN(0) * second.valueOrNaN(1) -
            first.valueOrNaN(1) * second.valueOrNaN(0),
    )

    private fun DoubleArray.valueOrNaN(index: Int): Double =
        if (index in indices) this[index] else Double.NaN
}
