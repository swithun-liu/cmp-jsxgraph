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

enum class PerpendicularPointRole {
    FIRST_LINE_POINT,
    SECOND_LINE_POINT,
    OTHER,
}

data class PerpendicularResult(
    val point: DoubleArray,
    val change: Boolean,
)

data class HullPoint(
    val index: Int,
    val coordinates: DoubleArray,
)

object Geometry {
    private const val AKL_TOUSSAINT_THRESHOLD = 1024

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

    // JSXGraph: src/math/geometry.js -> angleBisector
    fun angleBisector(
        first: DoubleArray,
        vertex: DoubleArray,
        third: DoubleArray,
    ): DoubleArray {
        if (vertex.valueOrNaN(0) == 0.0) {
            return doubleArrayOf(
                1.0,
                (first.valueOrNaN(1) + third.valueOrNaN(1)) * 0.5,
                (first.valueOrNaN(2) + third.valueOrNaN(2)) * 0.5,
            )
        }

        val firstAngle = atan2(
            first.valueOrNaN(2) - vertex.valueOrNaN(2),
            first.valueOrNaN(1) - vertex.valueOrNaN(1),
        )
        val thirdAngle = atan2(
            third.valueOrNaN(2) - vertex.valueOrNaN(2),
            third.valueOrNaN(1) - vertex.valueOrNaN(1),
        )
        var angle = (firstAngle + thirdAngle) * 0.5
        if (firstAngle > thirdAngle) {
            angle += PI
        }
        return doubleArrayOf(
            1.0,
            kotlin.math.cos(angle) + vertex.valueOrNaN(1),
            kotlin.math.sin(angle) + vertex.valueOrNaN(2),
        )
    }

    // JSXGraph: src/math/geometry.js -> reflection
    fun reflection(
        lineFirst: DoubleArray,
        lineSecond: DoubleArray,
        point: DoubleArray,
    ): DoubleArray {
        val horizontalDirection =
            lineSecond.valueOrNaN(1) - lineFirst.valueOrNaN(1)
        val verticalDirection =
            lineSecond.valueOrNaN(2) - lineFirst.valueOrNaN(2)
        val horizontalOffset =
            point.valueOrNaN(1) - lineFirst.valueOrNaN(1)
        val verticalOffset =
            point.valueOrNaN(2) - lineFirst.valueOrNaN(2)
        val scale =
            (
                horizontalDirection * verticalOffset -
                    verticalDirection * horizontalOffset
            ) /
                (
                    horizontalDirection * horizontalDirection +
                        verticalDirection * verticalDirection
                )
        return doubleArrayOf(
            1.0,
            point.valueOrNaN(1) + 2.0 * scale * verticalDirection,
            point.valueOrNaN(2) - 2.0 * scale * horizontalDirection,
        )
    }

    // JSXGraph: src/math/geometry.js -> rotation
    fun rotation(
        center: DoubleArray,
        point: DoubleArray,
        angle: Double,
    ): DoubleArray {
        val horizontal = point.valueOrNaN(1) - center.valueOrNaN(1)
        val vertical = point.valueOrNaN(2) - center.valueOrNaN(2)
        val cosine = kotlin.math.cos(angle)
        val sine = kotlin.math.sin(angle)
        return doubleArrayOf(
            1.0,
            horizontal * cosine - vertical * sine + center.valueOrNaN(1),
            horizontal * sine + vertical * cosine + center.valueOrNaN(2),
        )
    }

    // JSXGraph: src/math/geometry.js -> perpendicular
    fun perpendicular(
        lineFirst: DoubleArray,
        lineSecond: DoubleArray,
        point: DoubleArray,
        pointRole: PerpendicularPointRole = PerpendicularPointRole.OTHER,
    ): PerpendicularResult {
        val line = normalizedLine(lineFirst, lineSecond)
        var horizontal: Double
        var vertical: Double
        var homogeneous: Double
        var change: Boolean

        when (pointRole) {
            PerpendicularPointRole.FIRST_LINE_POINT -> {
                horizontal =
                    lineFirst.valueOrNaN(1) +
                        lineSecond.valueOrNaN(2) -
                        lineFirst.valueOrNaN(2)
                vertical =
                    lineFirst.valueOrNaN(2) -
                        lineSecond.valueOrNaN(1) +
                        lineFirst.valueOrNaN(1)
                homogeneous =
                    lineFirst.valueOrNaN(0) * lineSecond.valueOrNaN(0)
                if (abs(homogeneous) < Mat.eps) {
                    horizontal = lineSecond.valueOrNaN(2)
                    vertical = -lineSecond.valueOrNaN(1)
                }
                change = true
            }

            PerpendicularPointRole.SECOND_LINE_POINT -> {
                horizontal =
                    lineSecond.valueOrNaN(1) +
                        lineFirst.valueOrNaN(2) -
                        lineSecond.valueOrNaN(2)
                vertical =
                    lineSecond.valueOrNaN(2) -
                        lineFirst.valueOrNaN(1) +
                        lineSecond.valueOrNaN(1)
                homogeneous =
                    lineFirst.valueOrNaN(0) * lineSecond.valueOrNaN(0)
                if (abs(homogeneous) < Mat.eps) {
                    horizontal = lineFirst.valueOrNaN(2)
                    vertical = -lineFirst.valueOrNaN(1)
                }
                change = false
            }

            PerpendicularPointRole.OTHER -> {
                val lineValue =
                    point.valueOrNaN(0) * line[0] +
                        point.valueOrNaN(1) * line[1] +
                        point.valueOrNaN(2) * line[2]
                if (abs(lineValue) < Mat.eps) {
                    horizontal =
                        point.valueOrNaN(1) +
                            lineSecond.valueOrNaN(2) -
                            point.valueOrNaN(2)
                    vertical =
                        point.valueOrNaN(2) -
                            lineSecond.valueOrNaN(1) +
                            point.valueOrNaN(1)
                    homogeneous = lineSecond.valueOrNaN(0)
                    if (abs(homogeneous) < Mat.eps) {
                        horizontal = lineSecond.valueOrNaN(2)
                        vertical = -lineSecond.valueOrNaN(1)
                    }

                    change = true
                    if (
                        abs(homogeneous) > Mat.eps &&
                        abs(horizontal - point.valueOrNaN(1)) < Mat.eps &&
                        abs(vertical - point.valueOrNaN(2)) < Mat.eps
                    ) {
                        horizontal =
                            point.valueOrNaN(1) +
                                lineFirst.valueOrNaN(2) -
                                point.valueOrNaN(2)
                        vertical =
                            point.valueOrNaN(2) -
                                lineFirst.valueOrNaN(1) +
                                point.valueOrNaN(1)
                        change = false
                    }
                } else {
                    var perpendicularLine =
                        crossProduct(
                            doubleArrayOf(0.0, line[1], line[2]),
                            point,
                        )
                    perpendicularLine = crossProduct(perpendicularLine, line)
                    return PerpendicularResult(
                        point = normalizeHomogeneous(perpendicularLine),
                        change = true,
                    )
                }
            }
        }
        return PerpendicularResult(
            point = normalizeHomogeneous(
                doubleArrayOf(homogeneous, horizontal, vertical),
            ),
            change = change,
        )
    }

    // JSXGraph: src/math/geometry.js -> circumcenter
    fun circumcenter(
        first: DoubleArray,
        second: DoubleArray,
        third: DoubleArray,
    ): DoubleArray {
        var direction = doubleArrayOf(
            second.valueOrNaN(0) - first.valueOrNaN(0),
            -second.valueOrNaN(2) + first.valueOrNaN(2),
            second.valueOrNaN(1) - first.valueOrNaN(1),
        )
        var midpoint = doubleArrayOf(
            (first.valueOrNaN(0) + second.valueOrNaN(0)) * 0.5,
            (first.valueOrNaN(1) + second.valueOrNaN(1)) * 0.5,
            (first.valueOrNaN(2) + second.valueOrNaN(2)) * 0.5,
        )
        val firstBisector = crossProduct(direction, midpoint)

        direction = doubleArrayOf(
            third.valueOrNaN(0) - second.valueOrNaN(0),
            -third.valueOrNaN(2) + second.valueOrNaN(2),
            third.valueOrNaN(1) - second.valueOrNaN(1),
        )
        midpoint = doubleArrayOf(
            (second.valueOrNaN(0) + third.valueOrNaN(0)) * 0.5,
            (second.valueOrNaN(1) + third.valueOrNaN(1)) * 0.5,
            (second.valueOrNaN(2) + third.valueOrNaN(2)) * 0.5,
        )
        val secondBisector = crossProduct(direction, midpoint)
        return normalizeHomogeneous(crossProduct(firstBisector, secondBisector))
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

    // JSXGraph: src/math/geometry.js -> sortVertices
    fun sortVertices(points: List<DoubleArray>): List<DoubleArray> {
        if (points.size <= 1) {
            return points.toList()
        }

        val sorted = points.toMutableList()
        val first = sorted[0]
        var lastPoint: DoubleArray? = null
        while (
            sorted.size > 1 &&
            first.valueOrNaN(0) == sorted.last().valueOrNaN(0) &&
            first.valueOrNaN(1) == sorted.last().valueOrNaN(1) &&
            first.valueOrNaN(2) == sorted.last().valueOrNaN(2)
        ) {
            lastPoint = sorted.removeAt(sorted.lastIndex)
        }

        val origin = sorted[0]
        sorted.sortWith { firstPoint, secondPoint ->
            val firstAngle =
                if (
                    firstPoint.valueOrNaN(2) == origin.valueOrNaN(2) &&
                    firstPoint.valueOrNaN(1) == origin.valueOrNaN(1)
                ) {
                    Double.NEGATIVE_INFINITY
                } else {
                    atan2(
                        firstPoint.valueOrNaN(2) - origin.valueOrNaN(2),
                        firstPoint.valueOrNaN(1) - origin.valueOrNaN(1),
                    )
                }
            val secondAngle =
                if (
                    secondPoint.valueOrNaN(2) == origin.valueOrNaN(2) &&
                    secondPoint.valueOrNaN(1) == origin.valueOrNaN(1)
                ) {
                    Double.NEGATIVE_INFINITY
                } else {
                    atan2(
                        secondPoint.valueOrNaN(2) - origin.valueOrNaN(2),
                        secondPoint.valueOrNaN(1) - origin.valueOrNaN(1),
                    )
                }
            when {
                firstAngle < secondAngle -> -1
                firstAngle > secondAngle -> 1
                else -> 0
            }
        }
        if (lastPoint != null) {
            sorted += lastPoint
        }
        return sorted
    }

    // JSXGraph: src/math/geometry.js -> signedPolygon
    fun signedPolygon(
        points: List<DoubleArray>,
        sort: Boolean = true,
    ): Double {
        if (points.isEmpty()) {
            return 0.0
        }

        val polygon = if (!sort) {
            sortVertices(points).toMutableList()
        } else {
            points.toMutableList().also { it.add(0, points.last()) }
        }
        var area = 0.0
        for (index in 1 until polygon.size) {
            area +=
                polygon[index - 1].valueOrNaN(1) *
                polygon[index].valueOrNaN(2) -
                polygon[index].valueOrNaN(1) *
                polygon[index - 1].valueOrNaN(2)
        }
        return area * 0.5
    }

    // JSXGraph: src/math/geometry.js -> GrahamScan
    @Suppress("FunctionName")
    fun GrahamScan(points: List<DoubleArray>): List<HullPoint> {
        if (points.isEmpty()) {
            return emptyList()
        }

        val pointCount = points.size
        var minimumXIndex = 0
        var maximumXIndex = 0
        var minimumYIndex = 0
        var maximumYIndex = 0
        var minimumXMinusYIndex = 0
        var maximumXMinusYIndex = 0
        var minimumXPlusYIndex = 0
        var maximumXPlusYIndex = 0

        if (pointCount > AKL_TOUSSAINT_THRESHOLD) {
            var minimumX = points[0].valueOrNaN(1)
            var maximumX = minimumX
            var minimumY = points[0].valueOrNaN(2)
            var maximumY = minimumY
            var minimumXMinusY = minimumX - minimumY
            var maximumXMinusY = minimumXMinusY
            var minimumXPlusY = minimumX + minimumY
            var maximumXPlusY = minimumXPlusY

            for (index in 1 until pointCount) {
                var value = points[index].valueOrNaN(1)
                if (value < minimumX) {
                    minimumX = value
                    minimumXIndex = index
                } else if (value > maximumX) {
                    maximumX = value
                    maximumXIndex = index
                }

                value = points[index].valueOrNaN(2)
                if (value < minimumY) {
                    minimumY = value
                    minimumYIndex = index
                } else if (value > maximumY) {
                    maximumY = value
                    maximumYIndex = index
                }

                value =
                    points[index].valueOrNaN(1) -
                        points[index].valueOrNaN(2)
                if (value < minimumXMinusY) {
                    minimumXMinusY = value
                    minimumXMinusYIndex = index
                } else if (value > maximumXMinusY) {
                    maximumXMinusY = value
                    maximumXMinusYIndex = index
                }

                value =
                    points[index].valueOrNaN(1) +
                        points[index].valueOrNaN(2)
                if (value < minimumXPlusY) {
                    minimumXPlusY = value
                    minimumXPlusYIndex = index
                } else if (value > maximumXPlusY) {
                    maximumXPlusY = value
                    maximumXPlusYIndex = index
                }
            }
        }

        val epsilon = Mat.eps * Mat.eps
        val extremeIndices = setOf(
            minimumXIndex,
            maximumXIndex,
            minimumYIndex,
            maximumYIndex,
            minimumXPlusYIndex,
            minimumXMinusYIndex,
            maximumXPlusYIndex,
            maximumXMinusYIndex,
        )
        val candidates = ArrayList<HullPoint>(pointCount)
        for (index in points.indices) {
            val point = points[index]
            if (
                pointCount <= AKL_TOUSSAINT_THRESHOLD ||
                index in extremeIndices ||
                (
                    minimumXIndex != minimumXMinusYIndex &&
                        signedTriangle(
                            points[minimumXIndex],
                            points[minimumXMinusYIndex],
                            point,
                        ) >= -epsilon
                ) ||
                (
                    minimumXMinusYIndex != maximumYIndex &&
                        signedTriangle(
                            points[minimumXMinusYIndex],
                            points[maximumYIndex],
                            point,
                        ) >= -epsilon
                ) ||
                (
                    maximumYIndex != maximumXPlusYIndex &&
                        signedTriangle(
                            points[maximumYIndex],
                            points[maximumXPlusYIndex],
                            point,
                        ) >= -epsilon
                ) ||
                (
                    maximumXPlusYIndex != maximumXIndex &&
                        signedTriangle(
                            points[maximumXPlusYIndex],
                            points[maximumXIndex],
                            point,
                        ) >= -epsilon
                ) ||
                (
                    maximumXIndex != maximumXMinusYIndex &&
                        signedTriangle(
                            points[maximumXIndex],
                            points[maximumXMinusYIndex],
                            point,
                        ) >= -epsilon
                ) ||
                (
                    maximumXMinusYIndex != minimumYIndex &&
                        signedTriangle(
                            points[maximumXMinusYIndex],
                            points[minimumYIndex],
                            point,
                        ) >= -epsilon
                ) ||
                (
                    minimumYIndex != minimumXPlusYIndex &&
                        signedTriangle(
                            points[minimumYIndex],
                            points[minimumXPlusYIndex],
                            point,
                        ) >= -epsilon
                ) ||
                (
                    minimumXPlusYIndex != minimumXIndex &&
                        signedTriangle(
                            points[minimumXPlusYIndex],
                            points[minimumXIndex],
                            point,
                        ) >= -epsilon
                )
            ) {
                candidates += HullPoint(index = index, coordinates = point)
            }
        }

        var lowestIndex = 0
        var lowestX = candidates[0].coordinates.valueOrNaN(1)
        var lowestY = candidates[0].coordinates.valueOrNaN(2)
        for (index in 1 until candidates.size) {
            val coordinates = candidates[index].coordinates
            if (
                coordinates.valueOrNaN(2) < lowestY ||
                (
                    coordinates.valueOrNaN(2) == lowestY &&
                        coordinates.valueOrNaN(1) < lowestX
                )
            ) {
                lowestX = coordinates.valueOrNaN(1)
                lowestY = coordinates.valueOrNaN(2)
                lowestIndex = index
            }
        }
        val swap = candidates[0]
        candidates[0] = candidates[lowestIndex]
        candidates[lowestIndex] = swap

        val origin = candidates[0].coordinates
        candidates.sortWith { firstPoint, secondPoint ->
            val orientation = signedTriangle(
                origin,
                firstPoint.coordinates,
                secondPoint.coordinates,
            )
            if (orientation == 0.0) {
                val firstDistance = Mat.hypot(
                    firstPoint.coordinates.valueOrNaN(1) -
                        origin.valueOrNaN(1),
                    firstPoint.coordinates.valueOrNaN(2) -
                        origin.valueOrNaN(2),
                )
                val secondDistance = Mat.hypot(
                    secondPoint.coordinates.valueOrNaN(1) -
                        origin.valueOrNaN(1),
                    secondPoint.coordinates.valueOrNaN(2) -
                        origin.valueOrNaN(2),
                )
                when {
                    firstDistance < secondDistance -> -1
                    firstDistance > secondDistance -> 1
                    else -> 0
                }
            } else {
                when {
                    orientation > 0.0 -> -1
                    orientation < 0.0 -> 1
                    else -> 0
                }
            }
        }

        val hull = ArrayList<HullPoint>()
        for (candidate in candidates) {
            while (
                hull.size > 1 &&
                signedTriangle(
                    hull[hull.lastIndex - 1].coordinates,
                    hull[hull.lastIndex].coordinates,
                    candidate.coordinates,
                ) <= 0.0
            ) {
                hull.removeAt(hull.lastIndex)
            }
            hull += candidate
        }
        return hull
    }

    // JSXGraph: src/math/geometry.js -> convexHull
    fun convexHull(points: List<DoubleArray>): List<DoubleArray> =
        GrahamScan(points).map(HullPoint::coordinates)

    // JSXGraph: src/math/geometry.js -> isConvex
    fun isConvex(points: List<DoubleArray>): Boolean {
        if (points.size < 3) {
            return true
        }

        val epsilon = Mat.eps * Mat.eps
        var orientation: Int? = null
        var oldX = points[points.lastIndex - 1].valueOrNaN(1)
        var oldY = points[points.lastIndex - 1].valueOrNaN(2)
        var newX = points.last().valueOrNaN(1)
        var newY = points.last().valueOrNaN(2)
        var newDirection = atan2(newY - oldY, newX - oldX)
        var angleSum = 0.0

        for (point in points) {
            oldX = newX
            oldY = newY
            val oldDirection = newDirection
            newX = point.valueOrNaN(1)
            newY = point.valueOrNaN(2)
            if (oldX == newX && oldY == newY) {
                continue
            }

            newDirection = atan2(newY - oldY, newX - oldX)
            var angle = newDirection - oldDirection
            if (angle <= -PI) {
                angle += 2.0 * PI
            } else if (angle > PI) {
                angle -= 2.0 * PI
            }

            if (orientation == null) {
                if (angle == 0.0) {
                    continue
                }
                orientation = if (angle > 0.0) 1 else -1
            } else if (orientation * angle < -epsilon) {
                return false
            }
            angleSum += angle
        }

        return abs(angleSum / (2.0 * PI)) - 1.0 < epsilon
    }

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

    // JSXGraph: src/math/geometry.js -> windingNumber
    fun windingNumber(
        point: DoubleArray,
        path: List<DoubleArray>,
        doNotClosePath: Boolean = false,
    ): Int {
        if (path.isEmpty()) {
            return 0
        }

        val x = point.valueOrNaN(1)
        val y = point.valueOrNaN(2)
        if (x.isNaN() || y.isNaN()) {
            return 1
        }

        val first = path[0]
        if (first.valueOrNaN(1) == x && first.valueOrNaN(2) == y) {
            return 1
        }

        val edgeCount = path.size - if (doNotClosePath) 1 else 0
        var windingNumber = 0
        for (index in 0 until edgeCount) {
            val edgeStart = path[index]
            val edgeEnd = path[(index + 1) % path.size]
            if (
                edgeStart.valueOrNaN(0) == 0.0 ||
                edgeEnd.valueOrNaN(0) == 0.0 ||
                edgeStart.valueOrNaN(1).isNaN() ||
                edgeEnd.valueOrNaN(1).isNaN() ||
                edgeStart.valueOrNaN(2).isNaN() ||
                edgeEnd.valueOrNaN(2).isNaN()
            ) {
                continue
            }

            if (edgeEnd.valueOrNaN(2) == y) {
                if (edgeEnd.valueOrNaN(1) == x) {
                    return 1
                }
                if (
                    edgeStart.valueOrNaN(2) == y &&
                    (edgeEnd.valueOrNaN(1) > x) ==
                    (edgeStart.valueOrNaN(1) < x)
                ) {
                    return 0
                }
            }

            if (
                (edgeStart.valueOrNaN(2) < y) !=
                (edgeEnd.valueOrNaN(2) < y)
            ) {
                val sign =
                    2 *
                    (if (edgeEnd.valueOrNaN(2) > edgeStart.valueOrNaN(2)) 1 else 0) -
                    1
                if (edgeStart.valueOrNaN(1) >= x) {
                    if (edgeEnd.valueOrNaN(1) > x) {
                        windingNumber += sign
                    } else {
                        val determinant = det3p(edgeStart, edgeEnd, point)
                        if (determinant == 0.0) {
                            return 0
                        }
                        if (
                            (determinant > Mat.eps) ==
                            (edgeEnd.valueOrNaN(2) > edgeStart.valueOrNaN(2))
                        ) {
                            windingNumber += sign
                        }
                    }
                } else if (edgeEnd.valueOrNaN(1) > x) {
                    val determinant = det3p(edgeStart, edgeEnd, point)
                    if (
                        (determinant > Mat.eps) ==
                        (edgeEnd.valueOrNaN(2) > edgeStart.valueOrNaN(2))
                    ) {
                        windingNumber += sign
                    }
                }
            }
        }
        return windingNumber
    }

    // JSXGraph: src/math/geometry.js -> pnpoly
    fun pnpoly(
        screenX: Double,
        screenY: Double,
        closedPath: List<DoubleArray>,
    ): Boolean {
        var isInside = false
        var previousIndex = closedPath.size - 2
        for (index in 0 until closedPath.size - 1) {
            val current = closedPath[index]
            val previous = closedPath[previousIndex]
            if (
                (current.valueOrNaN(2) > screenY) !=
                (previous.valueOrNaN(2) > screenY) &&
                screenX <
                (
                    (previous.valueOrNaN(1) - current.valueOrNaN(1)) *
                        (screenY - current.valueOrNaN(2))
                ) /
                (previous.valueOrNaN(2) - current.valueOrNaN(2)) +
                current.valueOrNaN(1)
            ) {
                isInside = !isInside
            }
            previousIndex = index
        }
        return isInside
    }

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
        return normalizeHomogeneous(result)
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

    private fun normalizedLine(
        first: DoubleArray,
        second: DoubleArray,
    ): DoubleArray {
        val crossProduct = crossProduct(first, second)
        val norm = Mat.hypot(crossProduct[1], crossProduct[2])
        if (norm != 0.0) {
            crossProduct[0] /= norm
            crossProduct[1] /= norm
            crossProduct[2] /= norm
        }
        return crossProduct
    }

    private fun normalizeHomogeneous(coordinates: DoubleArray): DoubleArray {
        if (coordinates[0] != 0.0 && coordinates[0] != 1.0) {
            coordinates[1] /= coordinates[0]
            coordinates[2] /= coordinates[0]
            coordinates[0] /= coordinates[0]
        }
        return coordinates
    }

    private fun DoubleArray.valueOrNaN(index: Int): Double =
        if (index in indices) this[index] else Double.NaN
}
