/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/geometry.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.CoordsElement
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

enum class ArcSelection {
    AUTO,
    MINOR,
    MAJOR,
}

enum class ArcOrientation {
    COUNTERCLOCKWISE,
    CLOCKWISE,
}

data class PerpendicularResult(
    val point: DoubleArray,
    val change: Boolean,
)

data class HullPoint(
    val index: Int,
    val coordinates: DoubleArray,
)

data class ProjectionResult(
    val point: DoubleArray,
    val parameter: Double,
)

data class BezierArcResult(
    val xCoordinates: DoubleArray,
    val yCoordinates: DoubleArray,
)

data class Circle3DIntersection(
    val center: DoubleArray,
    val normal: DoubleArray,
    val radius: Double,
)

internal class ReuleauxPolygonInterpolation internal constructor(
    private val points: List<CoordsElement>,
    private val vertexCount: Int,
) {
    private val period = 2.0 * PI
    private val arcLength = period / vertexCount
    private val diagonalIndex = (vertexCount - 1) / 2
    private var radius = 0.0
    private var beta = Double.NaN

    internal val start: Double = 0.0
    internal val end: Double = period

    internal fun x(
        parameter: Double,
        suspendedUpdate: Boolean = false,
    ): Double = evaluate(
        parameter = parameter,
        suspendedUpdate = suspendedUpdate,
        coordinate = CoordsElement::X,
        trigonometricFunction = { value -> kotlin.math.cos(value) },
    )

    internal fun y(
        parameter: Double,
        suspendedUpdate: Boolean = false,
    ): Double = evaluate(
        parameter = parameter,
        suspendedUpdate = suspendedUpdate,
        coordinate = CoordsElement::Y,
        trigonometricFunction = { value -> kotlin.math.sin(value) },
    )

    private fun evaluate(
        parameter: Double,
        suspendedUpdate: Boolean,
        coordinate: (CoordsElement) -> Double,
        trigonometricFunction: (Double) -> Double,
    ): Double {
        if (!suspendedUpdate) {
            radius = points[0].Dist(points[diagonalIndex])
            beta = Geometry.rad(
                doubleArrayOf(points[0].X() + 1.0, points[0].Y()),
                doubleArrayOf(points[0].X(), points[0].Y()),
                doubleArrayOf(
                    points[diagonalIndex % vertexCount].X(),
                    points[diagonalIndex % vertexCount].Y(),
                ),
            )
        }

        var localParameter =
            ((parameter % period) + period) % period
        val segmentValue = localParameter / arcLength
        if (segmentValue.isNaN()) {
            return segmentValue
        }
        val segmentIndex =
            kotlin.math.floor(segmentValue).toInt() % vertexCount
        localParameter =
            localParameter * 0.5 +
                segmentIndex * arcLength * 0.5 +
                beta
        return coordinate(points[segmentIndex]) +
            radius * trigonometricFunction(localParameter)
    }
}

internal data class DiscreteCurve2D(
    val points: List<DoubleArray>,
    val bezierDegree: Int,
    val isSector: Boolean = false,
)

internal enum class ContinuousCurveType {
    PARAMETER,
    POLAR,
    FUNCTION_GRAPH,
}

internal data class ContinuousCurve2D(
    val curve: ParametricCurve2D,
    val minimumParameter: Double,
    val maximumParameter: Double,
    val type: ContinuousCurveType,
)

sealed interface GeometryError {
    data class InvalidPolygonPointCount(val pointCount: Int) : GeometryError

    data class InvalidBezierCurveDegrees(
        val firstDegree: Int,
        val secondDegree: Int,
    ) : GeometryError

    data class InvalidDiscreteCurveDegree(val degree: Int) : GeometryError

    data class InvalidDiscreteCurvePointCount(
        val pointCount: Int,
        val degree: Int,
    ) : GeometryError

    data class InvalidContinuousCurveDomain(
        val minimum: Double,
        val maximum: Double,
    ) : GeometryError

    data class InvalidIntersectionIndex(val index: Int) : GeometryError

    data class InvalidReuleauxVertexCount(val vertexCount: Int) : GeometryError

    data class InvalidReuleauxPointCount(
        val pointCount: Int,
        val vertexCount: Int,
    ) : GeometryError

    data class NumericalProjectionFailure(
        val cause: NumericsError,
    ) : GeometryError

    data object PolygonProjectionUnavailable : GeometryError
}

object Geometry {
    private const val AKL_TOUSSAINT_THRESHOLD = 1024
    private const val BEZIER_SUBDIVISION_MAX_LEVEL = 5

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

    // JSXGraph: src/math/geometry.js -> coordsOnArc
    fun coordsOnArc(
        radiusPoint: DoubleArray,
        center: DoubleArray,
        anglePoint: DoubleArray,
        coordinates: DoubleArray,
        selection: ArcSelection = ArcSelection.AUTO,
        orientation: ArcOrientation = ArcOrientation.COUNTERCLOCKWISE,
    ): Boolean {
        var angle = rad(radiusPoint, center, coordinates)
        var minimumAngle = 0.0
        var maximumAngle = rad(radiusPoint, center, anglePoint)

        if (orientation == ArcOrientation.CLOCKWISE) {
            angle = 2.0 * PI - angle
            maximumAngle = 2.0 * PI - maximumAngle
        }
        if (
            (
                selection == ArcSelection.MINOR &&
                    maximumAngle > PI
            ) ||
            (
                selection == ArcSelection.MAJOR &&
                    maximumAngle < PI
            )
        ) {
            minimumAngle = maximumAngle
            maximumAngle = 2.0 * PI
        }
        return angle >= minimumAngle && angle <= maximumAngle
    }

    // JSXGraph: src/math/geometry.js -> _bezierSplit
    internal fun bezierSplit(
        curve: List<DoubleArray>,
    ): List<List<DoubleArray>> {
        val firstMidpoint = midpoint(curve[0], curve[1])
        val secondMidpoint = midpoint(curve[1], curve[2])
        val thirdMidpoint = midpoint(curve[2], curve[3])
        val firstQuarter = midpoint(firstMidpoint, secondMidpoint)
        val thirdQuarter = midpoint(secondMidpoint, thirdMidpoint)
        val center = midpoint(firstQuarter, thirdQuarter)
        return listOf(
            listOf(curve[0], firstMidpoint, firstQuarter, center),
            listOf(center, thirdQuarter, thirdMidpoint, curve[3]),
        )
    }

    // JSXGraph: src/math/geometry.js -> _bezierBbox
    internal fun bezierBoundingBox(curve: List<DoubleArray>): DoubleArray =
        if (curve.size == 4) {
            doubleArrayOf(
                minOf(curve[0][0], curve[1][0], curve[2][0], curve[3][0]),
                maxOf(curve[0][1], curve[1][1], curve[2][1], curve[3][1]),
                maxOf(curve[0][0], curve[1][0], curve[2][0], curve[3][0]),
                minOf(curve[0][1], curve[1][1], curve[2][1], curve[3][1]),
            )
        } else {
            doubleArrayOf(
                minOf(curve[0][0], curve[1][0]),
                maxOf(curve[0][1], curve[1][1]),
                maxOf(curve[0][0], curve[1][0]),
                minOf(curve[0][1], curve[1][1]),
            )
        }

    // JSXGraph: src/math/geometry.js -> _bezierOverlap
    internal fun bezierOverlap(
        firstBoundingBox: DoubleArray,
        secondBoundingBox: DoubleArray,
    ): Boolean =
        firstBoundingBox[2] >= secondBoundingBox[0] &&
            firstBoundingBox[0] <= secondBoundingBox[2] &&
            firstBoundingBox[1] >= secondBoundingBox[3] &&
            firstBoundingBox[3] <= secondBoundingBox[1]

    // JSXGraph: src/math/geometry.js -> _bezierListConcat
    private fun bezierListConcat(
        destination: MutableList<SegmentIntersection>,
        additions: List<SegmentIntersection>,
        firstOffset: Double,
        secondOffset: Double? = null,
    ) {
        val start = if (
            destination.isNotEmpty() &&
            additions.isNotEmpty() &&
            (
                (
                    destination.last().firstParameter == 1.0 &&
                        additions.first().firstParameter == 0.0
                ) ||
                    (
                        secondOffset != null &&
                            destination.last().secondParameter == 1.0 &&
                            additions.first().secondParameter == 0.0
                    )
            )
        ) {
            1
        } else {
            0
        }

        for (index in start until additions.size) {
            val addition = additions[index]
            destination += SegmentIntersection(
                point = addition.point,
                firstParameter = addition.firstParameter * 0.5 + firstOffset,
                secondParameter = if (secondOffset != null) {
                    addition.secondParameter * 0.5 + secondOffset
                } else {
                    addition.secondParameter
                },
            )
        }
    }

    // JSXGraph: src/math/geometry.js -> _bezierMeetSubdivision
    private fun bezierMeetSubdivision(
        red: List<DoubleArray>,
        blue: List<DoubleArray>,
        level: Int,
    ): List<SegmentIntersection> {
        if (
            !bezierOverlap(
                bezierBoundingBox(blue),
                bezierBoundingBox(red),
            )
        ) {
            return emptyList()
        }

        if (level < BEZIER_SUBDIVISION_MAX_LEVEL) {
            val redSplit = bezierSplit(red)
            val blueSplit = bezierSplit(blue)
            val intersections = mutableListOf<SegmentIntersection>()
            bezierListConcat(
                intersections,
                bezierMeetSubdivision(redSplit[0], blueSplit[0], level + 1),
                firstOffset = 0.0,
                secondOffset = 0.0,
            )
            bezierListConcat(
                intersections,
                bezierMeetSubdivision(redSplit[0], blueSplit[1], level + 1),
                firstOffset = 0.0,
                secondOffset = 0.5,
            )
            bezierListConcat(
                intersections,
                bezierMeetSubdivision(redSplit[1], blueSplit[0], level + 1),
                firstOffset = 0.5,
                secondOffset = 0.0,
            )
            bezierListConcat(
                intersections,
                bezierMeetSubdivision(redSplit[1], blueSplit[1], level + 1),
                firstOffset = 0.5,
                secondOffset = 0.5,
            )
            return intersections
        }

        val intersection = meetSegmentSegment(
            doubleArrayOf(1.0, red[0][0], red[0][1]),
            doubleArrayOf(1.0, red[3][0], red[3][1]),
            doubleArrayOf(1.0, blue[0][0], blue[0][1]),
            doubleArrayOf(1.0, blue[3][0], blue[3][1]),
        )
        return if (
            intersection.firstParameter >= 0.0 &&
            intersection.secondParameter >= 0.0 &&
            intersection.firstParameter <= 1.0 &&
            intersection.secondParameter <= 1.0
        ) {
            listOf(intersection)
        } else {
            emptyList()
        }
    }

    // JSXGraph: src/math/geometry.js -> _bezierLineMeetSubdivision
    private fun bezierLineMeetSubdivision(
        red: List<DoubleArray>,
        blue: List<DoubleArray>,
        level: Int,
        testSegment: Boolean,
    ): List<SegmentIntersection> {
        if (
            testSegment &&
            !bezierOverlap(
                bezierBoundingBox(red),
                bezierBoundingBox(blue),
            )
        ) {
            return emptyList()
        }

        if (level < BEZIER_SUBDIVISION_MAX_LEVEL) {
            val redSplit = bezierSplit(red)
            val intersections = mutableListOf<SegmentIntersection>()
            // JSXGraph omits testSegment in recursive calls, making them line tests.
            bezierListConcat(
                intersections,
                bezierLineMeetSubdivision(
                    redSplit[0],
                    blue,
                    level + 1,
                    testSegment = false,
                ),
                firstOffset = 0.0,
            )
            bezierListConcat(
                intersections,
                bezierLineMeetSubdivision(
                    redSplit[1],
                    blue,
                    level + 1,
                    testSegment = false,
                ),
                firstOffset = 0.5,
            )
            return intersections
        }

        val intersection = meetSegmentSegment(
            doubleArrayOf(1.0, red[0][0], red[0][1]),
            doubleArrayOf(1.0, red[3][0], red[3][1]),
            doubleArrayOf(1.0, blue[0][0], blue[0][1]),
            doubleArrayOf(1.0, blue[1][0], blue[1][1]),
        )
        return if (
            intersection.firstParameter >= 0.0 &&
            intersection.firstParameter <= 1.0 &&
            (
                !testSegment ||
                    (
                        intersection.secondParameter >= 0.0 &&
                            intersection.secondParameter <= 1.0
                    )
            )
        ) {
            listOf(intersection)
        } else {
            emptyList()
        }
    }

    // JSXGraph: src/math/geometry.js -> meetBeziersegmentBeziersegment
    internal fun meetBeziersegmentBeziersegment(
        red: List<DoubleArray>,
        blue: List<DoubleArray>,
        testSegment: Boolean = false,
    ): List<SegmentIntersection> {
        val intersections = if (red.size == 4 && blue.size == 4) {
            bezierMeetSubdivision(red, blue, level = 0)
        } else {
            bezierLineMeetSubdivision(
                red,
                blue,
                level = 0,
                testSegment = testSegment,
            )
        }.sortedWith { first, second ->
            val difference =
                (first.firstParameter - second.firstParameter) * 10_000_000.0 +
                    (first.secondParameter - second.secondParameter)
            when {
                difference < 0.0 -> -1
                difference > 0.0 -> 1
                else -> 0
            }
        }

        return intersections.filterIndexed { index, intersection ->
            index == 0 ||
                intersection.firstParameter !=
                intersections[index - 1].firstParameter ||
                intersection.secondParameter !=
                intersections[index - 1].secondParameter
        }
    }

    // JSXGraph: src/math/geometry.js -> meetBezierCurveRedBlueSegments
    internal fun meetBezierCurveRedBlueSegments(
        initialRed: DiscreteCurve2D,
        initialBlue: DiscreteCurve2D,
        intersectionIndex: Int,
    ): GMResult<DoubleArray, GeometryError> {
        if (intersectionIndex < 0) {
            return GMResult.Err(
                GeometryError.InvalidIntersectionIndex(intersectionIndex),
            )
        }
        if (
            !(
                (
                    initialRed.bezierDegree == 3 &&
                        initialBlue.bezierDegree in setOf(1, 3)
                ) ||
                    (
                        initialRed.bezierDegree == 1 &&
                            initialBlue.bezierDegree == 3
                    )
            )
        ) {
            return GMResult.Err(
                GeometryError.InvalidBezierCurveDegrees(
                    initialRed.bezierDegree,
                    initialBlue.bezierDegree,
                ),
            )
        }
        if (
            initialBlue.points.size < initialBlue.bezierDegree + 1 ||
            initialRed.points.size < initialRed.bezierDegree + 1
        ) {
            return GMResult.Ok(doubleArrayOf(0.0, Double.NaN, Double.NaN))
        }

        var red = initialRed
        var blue = initialBlue
        if (red.bezierDegree == 1 && blue.bezierDegree == 3) {
            val swap = red
            red = blue
            blue = swap
        }

        var redStart = 0
        var blueStart = 0
        var redLength = red.points.size - red.bezierDegree
        var blueLength = blue.points.size - blue.bezierDegree
        if (red.isSector) {
            redStart = 3
            redLength -= 3
        }
        if (blue.isSector) {
            blueStart = 3
            blueLength -= 3
        }

        val intersections = mutableListOf<SegmentIntersection>()
        var redIndex = redStart
        while (redIndex < redLength) {
            val redSegment = mutableListOf(
                red.points[redIndex].sliceArray(1..2),
                red.points[redIndex + 1].sliceArray(1..2),
            )
            if (red.bezierDegree == 3) {
                redSegment += red.points[redIndex + 2].sliceArray(1..2)
                redSegment += red.points[redIndex + 3].sliceArray(1..2)
            }
            val redBoundingBox = bezierBoundingBox(redSegment)

            var blueIndex = blueStart
            while (blueIndex < blueLength) {
                val blueSegment = mutableListOf(
                    blue.points[blueIndex].sliceArray(1..2),
                    blue.points[blueIndex + 1].sliceArray(1..2),
                )
                if (blue.bezierDegree == 3) {
                    blueSegment +=
                        blue.points[blueIndex + 2].sliceArray(1..2)
                    blueSegment +=
                        blue.points[blueIndex + 3].sliceArray(1..2)
                }

                if (
                    bezierOverlap(
                        redBoundingBox,
                        bezierBoundingBox(blueSegment),
                    )
                ) {
                    val segmentIntersections =
                        meetBeziersegmentBeziersegment(
                            redSegment,
                            blueSegment,
                        )
                    for (intersection in segmentIntersections) {
                        if (
                            intersection.firstParameter < -Mat.eps ||
                            intersection.firstParameter > 1.0 + Mat.eps ||
                            intersection.secondParameter < -Mat.eps ||
                            intersection.secondParameter > 1.0 + Mat.eps
                        ) {
                            continue
                        }
                        intersections += intersection
                    }
                    if (intersections.size > intersectionIndex) {
                        return GMResult.Ok(
                            intersections[intersectionIndex].point,
                        )
                    }
                }
                blueIndex += blue.bezierDegree
            }
            redIndex += red.bezierDegree
        }

        return if (intersections.size > intersectionIndex) {
            GMResult.Ok(intersections[intersectionIndex].point)
        } else {
            GMResult.Ok(doubleArrayOf(0.0, Double.NaN, Double.NaN))
        }
    }

    // JSXGraph: src/math/geometry.js -> bezierSegmentEval
    internal fun bezierSegmentEval(
        parameter: Double,
        curve: List<DoubleArray>,
    ): DoubleArray {
        val inverseParameter = 1.0 - parameter
        val firstWeight =
            inverseParameter * inverseParameter * inverseParameter
        val firstControlWeight =
            3.0 * parameter * inverseParameter * inverseParameter
        val secondControlWeight =
            3.0 * parameter * parameter * inverseParameter
        val secondWeight = parameter * parameter * parameter
        return doubleArrayOf(
            1.0,
            firstWeight * curve[0][0] +
                firstControlWeight * curve[1][0] +
                secondControlWeight * curve[2][0] +
                secondWeight * curve[3][0],
            firstWeight * curve[0][1] +
                firstControlWeight * curve[1][1] +
                secondControlWeight * curve[2][1] +
                secondWeight * curve[3][1],
        )
    }

    // JSXGraph: src/math/geometry.js -> bezierArc
    internal fun bezierArc(
        first: DoubleArray,
        center: DoubleArray,
        third: DoubleArray,
        withLegs: Boolean,
        sign: Double,
    ): BezierArcResult {
        val radius = distance(center, first)
        val centerX = center[1] / center[0]
        val centerY = center[2] / center[0]
        var remainingAngle = rad(
            first.sliceArray(1..2),
            center.sliceArray(1..2),
            third.sliceArray(1..2),
        )
        if (sign == -1.0) {
            remainingAngle = 2.0 * PI - remainingAngle
        }
        val angleStep = remainingAngle / 4.0

        var firstControlPoint = first
        firstControlPoint[1] /= firstControlPoint[0]
        firstControlPoint[2] /= firstControlPoint[0]
        firstControlPoint[0] /= firstControlPoint[0]
        var fourthControlPoint = firstControlPoint.copyOf()

        val xCoordinates = mutableListOf<Double>()
        val yCoordinates = mutableListOf<Double>()
        if (withLegs) {
            xCoordinates += listOf(
                centerX,
                centerX + 0.333 * (firstControlPoint[1] - centerX),
                centerX + 0.666 * (firstControlPoint[1] - centerX),
                firstControlPoint[1],
            )
            yCoordinates += listOf(
                centerY,
                centerY + 0.333 * (firstControlPoint[2] - centerY),
                centerY + 0.666 * (firstControlPoint[2] - centerY),
                firstControlPoint[2],
            )
        } else {
            xCoordinates += firstControlPoint[1]
            yCoordinates += firstControlPoint[2]
        }

        while (remainingAngle > Mat.eps) {
            val angle = if (remainingAngle > angleStep) {
                remainingAngle -= angleStep
                angleStep
            } else {
                val finalAngle = remainingAngle
                remainingAngle = 0.0
                finalAngle
            }
            val cosine = kotlin.math.cos(sign * angle)
            val sine = kotlin.math.sin(sign * angle)
            val rotation = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(
                    centerX * (1.0 - cosine) + centerY * sine,
                    cosine,
                    -sine,
                ),
                doubleArrayOf(
                    centerY * (1.0 - cosine) - centerX * sine,
                    sine,
                    cosine,
                ),
            )
            val rotated = Mat.matVecMult(rotation, firstControlPoint)
            fourthControlPoint = doubleArrayOf(
                rotated[0] / rotated[0],
                rotated[1] / rotated[0],
                rotated[2] / rotated[0],
            )

            val firstX = firstControlPoint[1] - centerX
            val firstY = firstControlPoint[2] - centerY
            val fourthX = fourthControlPoint[1] - centerX
            val fourthY = fourthControlPoint[2] - centerY
            val diagonal =
                Mat.hypot(firstX + fourthX, firstY + fourthY)
            val scale = if (abs(fourthY - firstY) > Mat.eps) {
                (
                    (
                        (firstX + fourthX) *
                            (radius / diagonal - 0.5)
                    ) /
                        (fourthY - firstY) *
                        8.0
                ) / 3.0
            } else {
                (
                    (
                        (firstY + fourthY) *
                            (radius / diagonal - 0.5)
                    ) /
                        (firstX - fourthX) *
                        8.0
                ) / 3.0
            }

            val secondControlPoint = doubleArrayOf(
                1.0,
                firstControlPoint[1] - scale * firstY,
                firstControlPoint[2] + scale * firstX,
            )
            val thirdControlPoint = doubleArrayOf(
                1.0,
                fourthControlPoint[1] + scale * fourthY,
                fourthControlPoint[2] - scale * fourthX,
            )
            xCoordinates += listOf(
                secondControlPoint[1],
                thirdControlPoint[1],
                fourthControlPoint[1],
            )
            yCoordinates += listOf(
                secondControlPoint[2],
                thirdControlPoint[2],
                fourthControlPoint[2],
            )
            firstControlPoint = fourthControlPoint.copyOf()
        }

        if (withLegs) {
            xCoordinates += listOf(
                fourthControlPoint[1] +
                    0.333 * (centerX - fourthControlPoint[1]),
                fourthControlPoint[1] +
                    0.666 * (centerX - fourthControlPoint[1]),
                centerX,
            )
            yCoordinates += listOf(
                fourthControlPoint[2] +
                    0.333 * (centerY - fourthControlPoint[2]),
                fourthControlPoint[2] +
                    0.666 * (centerY - fourthControlPoint[2]),
                centerY,
            )
        }
        return BezierArcResult(
            xCoordinates = xCoordinates.toDoubleArray(),
            yCoordinates = yCoordinates.toDoubleArray(),
        )
    }

    // JSXGraph: src/math/geometry.js -> projectPointToCircle
    fun projectPointToCircle(
        point: DoubleArray,
        center: DoubleArray,
        radius: Double,
    ): DoubleArray {
        val weightDifference = point[0] - center[0]
        var centerDistance = if (
            weightDifference * weightDifference > Mat.eps * Mat.eps
        ) {
            Double.POSITIVE_INFINITY
        } else {
            Mat.hypot(
                point[1] - center[1],
                point[2] - center[2],
            )
        }
        if (abs(centerDistance) < Mat.eps) {
            centerDistance = Mat.eps
        }

        val factor = radius / centerDistance
        return doubleArrayOf(
            1.0,
            center[1] + factor * (point[1] - center[1]),
            center[2] + factor * (point[2] - center[2]),
        )
    }

    // JSXGraph: src/math/geometry.js -> projectPointToLine
    fun projectPointToLine(
        point: DoubleArray,
        line: DoubleArray,
    ): DoubleArray {
        val direction = doubleArrayOf(0.0, line[1], line[2])
        val perpendicular = Mat.crossProduct(direction, point)
        val projection = Mat.crossProduct(perpendicular, line)
        if (abs(projection[0]) > Mat.eps) {
            projection[1] /= projection[0]
            projection[2] /= projection[0]
            projection[0] = 1.0
        }
        return projection
    }

    // JSXGraph: src/math/geometry.js -> meetCurveRedBlueSegments
    internal fun meetCurveRedBlueSegments(
        red: List<DoubleArray>,
        blue: List<DoubleArray>,
        intersectionIndex: Int,
    ): DoubleArray {
        if (blue.size <= 1 || red.size <= 1) {
            return doubleArrayOf(0.0, Double.NaN, Double.NaN)
        }

        var foundIndex = 0
        for (redIndex in 1 until red.size) {
            val redStart = red[redIndex - 1]
            val redEnd = red[redIndex]
            val minimumRedX = minOf(redStart[1], redEnd[1])
            val maximumRedX = maxOf(redStart[1], redEnd[1])
            var blueEnd = blue[0]

            for (blueIndex in 1 until blue.size) {
                val blueStart = blueEnd
                blueEnd = blue[blueIndex]
                if (
                    minOf(blueStart[1], blueEnd[1]) < maximumRedX &&
                    maxOf(blueStart[1], blueEnd[1]) > minimumRedX
                ) {
                    val intersection = meetSegmentSegment(
                        redStart,
                        redEnd,
                        blueStart,
                        blueEnd,
                    )
                    if (
                        intersection.firstParameter >= 0.0 &&
                        intersection.secondParameter >= 0.0 &&
                        (
                            (
                                intersection.firstParameter < 1.0 &&
                                    intersection.secondParameter < 1.0
                            ) ||
                                (
                                    redIndex == red.lastIndex &&
                                        intersection.firstParameter == 1.0
                                ) ||
                                (
                                    blueIndex == blue.lastIndex &&
                                        intersection.secondParameter == 1.0
                                )
                        )
                    ) {
                        if (foundIndex == intersectionIndex) {
                            return intersection.point
                        }
                        foundIndex += 1
                    }
                }
            }
        }
        return doubleArrayOf(0.0, Double.NaN, Double.NaN)
    }

    // JSXGraph: src/math/geometry.js -> projectCoordsToPolygon
    internal fun projectCoordsToPolygon(
        point: DoubleArray,
        vertices: List<DoubleArray>,
    ): GMResult<DoubleArray, GeometryError> {
        if (vertices.size < 2) {
            return GMResult.Err(
                GeometryError.InvalidPolygonPointCount(vertices.size),
            )
        }

        var bestDistance = Double.POSITIVE_INFINITY
        var bestProjection: DoubleArray? = null
        for (index in 0 until vertices.lastIndex) {
            val projection = projectCoordsToSegment(
                point,
                vertices[index],
                vertices[index + 1],
            )
            val candidate = when {
                projection.parameter in 0.0..1.0 -> projection.point
                projection.parameter < 0.0 -> vertices[index]
                projection.parameter > 1.0 -> vertices[index + 1]
                else -> continue
            }
            val candidateDistance = distance(candidate, point, 3)
            if (candidateDistance < bestDistance) {
                bestProjection = candidate.copyOf()
                bestDistance = candidateDistance
            }
        }
        return if (bestProjection != null) {
            GMResult.Ok(bestProjection)
        } else {
            GMResult.Err(GeometryError.PolygonProjectionUnavailable)
        }
    }

    // JSXGraph: src/math/geometry.js -> projectCoordsToSegment
    fun projectCoordsToSegment(
        point: DoubleArray,
        first: DoubleArray,
        second: DoubleArray,
    ): ProjectionResult {
        val directionX = second.valueOrNaN(1) - first.valueOrNaN(1)
        val directionY = second.valueOrNaN(2) - first.valueOrNaN(2)
        if (abs(directionX) < Mat.eps && abs(directionY) < Mat.eps) {
            return ProjectionResult(point = first, parameter = 0.0)
        }

        val offsetX = point.valueOrNaN(1) - first.valueOrNaN(1)
        val offsetY = point.valueOrNaN(2) - first.valueOrNaN(2)
        val parameter =
            (offsetX * directionX + offsetY * directionY) /
                (directionX * directionX + directionY * directionY)
        return ProjectionResult(
            point = doubleArrayOf(
                1.0,
                parameter * directionX + first.valueOrNaN(1),
                parameter * directionY + first.valueOrNaN(2),
            ),
            parameter = parameter,
        )
    }

    // JSXGraph: src/math/geometry.js -> projectCoordsToBeziersegment
    internal fun projectCoordsToBeziersegment(
        point: DoubleArray,
        curve: ParametricCurve2D,
        start: Double,
    ): GMResult<ProjectionResult, NumericsError> {
        val minimum = when (
            val result = Numerics.fminbr(
                function = { parameter ->
                    val x = curve.x(start + parameter) - point.valueOrNaN(1)
                    val y = curve.y(start + parameter) - point.valueOrNaN(2)
                    x * x + y * y
                },
                interval = doubleArrayOf(0.0, 1.0),
            )
        ) {
            is GMResult.Err -> return result
            is GMResult.Ok -> result.value
        }
        return GMResult.Ok(
            ProjectionResult(
                point = doubleArrayOf(
                    1.0,
                    curve.x(minimum + start),
                    curve.y(minimum + start),
                ),
                parameter = minimum,
            ),
        )
    }

    // JSXGraph: src/math/geometry.js -> projectCoordsToCurve (plot branch)
    internal fun projectCoordsToCurve(
        point: DoubleArray,
        curve: DiscreteCurve2D,
    ): GMResult<ProjectionResult, GeometryError> {
        if (curve.bezierDegree != 1 && curve.bezierDegree != 3) {
            return GMResult.Err(
                GeometryError.InvalidDiscreteCurveDegree(
                    curve.bezierDegree,
                ),
            )
        }
        if (
            curve.bezierDegree == 3 &&
            curve.points.size > 1 &&
            (curve.points.size - 1) % curve.bezierDegree != 0
        ) {
            return GMResult.Err(
                GeometryError.InvalidDiscreteCurvePointCount(
                    pointCount = curve.points.size,
                    degree = curve.bezierDegree,
                ),
            )
        }
        if (curve.points.isEmpty()) {
            return GMResult.Ok(
                ProjectionResult(
                    point = doubleArrayOf(0.0, 1.0, 1.0),
                    parameter = 0.0,
                ),
            )
        }

        var bestPoint = curve.points[0].copyOf()
        var bestParameter = 0.0
        var bestDistance = Double.POSITIVE_INFINITY
        if (curve.points.size <= 1) {
            return GMResult.Ok(
                ProjectionResult(bestPoint, bestParameter),
            )
        }

        var pointIndex = 0
        var bezierSegmentIndex = 0
        while (pointIndex < curve.points.lastIndex) {
            val projection = if (curve.bezierDegree == 3) {
                val segment = listOf(
                    curve.points[pointIndex].sliceArray(1..2),
                    curve.points[pointIndex + 1].sliceArray(1..2),
                    curve.points[pointIndex + 2].sliceArray(1..2),
                    curve.points[pointIndex + 3].sliceArray(1..2),
                )
                val segmentIndex = bezierSegmentIndex.toDouble()
                val parametricCurve = ParametricCurve2D(
                    x = { parameter ->
                        bezierSegmentEval(
                            parameter - segmentIndex,
                            segment,
                        )[1]
                    },
                    y = { parameter ->
                        bezierSegmentEval(
                            parameter - segmentIndex,
                            segment,
                        )[2]
                    },
                )
                when (
                    val result = projectCoordsToBeziersegment(
                        point = point,
                        curve = parametricCurve,
                        start = segmentIndex,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        return GMResult.Err(
                            GeometryError.NumericalProjectionFailure(
                                result.error,
                            ),
                        )
                    }
                }
            } else {
                projectCoordsToSegment(
                    point = point,
                    first = curve.points[pointIndex],
                    second = curve.points[pointIndex + 1],
                )
            }

            val candidate = when {
                projection.parameter in 0.0..1.0 ->
                    ProjectionResult(
                        point = projection.point,
                        parameter = pointIndex + projection.parameter,
                    )

                projection.parameter < 0.0 ->
                    ProjectionResult(
                        point = curve.points[pointIndex],
                        parameter = pointIndex.toDouble(),
                    )

                projection.parameter > 1.0 &&
                    pointIndex == curve.points.lastIndex - 1 ->
                    ProjectionResult(
                        point = curve.points[pointIndex + 1],
                        parameter = curve.points.lastIndex.toDouble(),
                    )

                else -> null
            }
            if (candidate != null) {
                val candidateDistance = distance(candidate.point, point)
                if (candidateDistance < bestDistance) {
                    bestPoint = candidate.point.copyOf()
                    bestParameter = candidate.parameter
                    bestDistance = candidateDistance
                }
            }

            if (curve.bezierDegree == 3) {
                bezierSegmentIndex += 1
                pointIndex += 3
            } else {
                pointIndex += 1
            }
        }
        return GMResult.Ok(
            ProjectionResult(
                point = bestPoint,
                parameter = bestParameter,
            ),
        )
    }

    // JSXGraph: src/math/geometry.js -> projectCoordsToCurve (continuous branch)
    internal fun projectCoordsToCurve(
        horizontal: Double,
        vertical: Double,
        initialParameter: Double,
        continuousCurve: ContinuousCurve2D,
    ): GMResult<ProjectionResult, GeometryError> {
        val curve = continuousCurve.curve
        val globalMinimum = continuousCurve.minimumParameter
        val globalMaximum = continuousCurve.maximumParameter
        if (
            !globalMinimum.isFinite() ||
            !globalMaximum.isFinite() ||
            globalMinimum > globalMaximum
        ) {
            return GMResult.Err(
                GeometryError.InvalidContinuousCurveDomain(
                    minimum = globalMinimum,
                    maximum = globalMaximum,
                ),
            )
        }

        var minimum = globalMinimum
        var maximum = globalMaximum
        if (continuousCurve.type == ContinuousCurveType.FUNCTION_GRAPH) {
            val verticalDifference =
                abs(vertical - curve.y(horizontal))
            if (!verticalDifference.isNaN()) {
                minimum = horizontal - verticalDifference
                maximum = horizontal + verticalDifference
            }
        }

        val distanceSquared = { parameter: Double ->
            if (parameter < globalMinimum || parameter > globalMaximum) {
                Double.POSITIVE_INFINITY
            } else {
                val horizontalDifference =
                    horizontal - curve.x(parameter)
                val verticalDifference =
                    vertical - curve.y(parameter)
                horizontalDifference * horizontalDifference +
                    verticalDifference * verticalDifference
            }
        }

        var parameter = initialParameter
        var bestDistance = distanceSquared(parameter)
        val steps = 50
        val step = (maximum - minimum) / steps
        var candidate = minimum
        repeat(steps) {
            val candidateDistance = distanceSquared(candidate)
            if (
                candidateDistance < bestDistance ||
                bestDistance == Double.POSITIVE_INFINITY ||
                bestDistance.isNaN()
            ) {
                parameter = candidate
                bestDistance = candidateDistance
            }
            candidate += step
        }

        var lowerStep = step
        var iteration = 0
        while (
            iteration < 20 &&
            distanceSquared(parameter - lowerStep).isNaN()
        ) {
            lowerStep *= 0.5
            iteration += 1
        }
        if (distanceSquared(parameter - lowerStep).isNaN()) {
            lowerStep = 0.0
        }

        var upperStep = step
        iteration = 0
        while (
            iteration < 20 &&
            distanceSquared(parameter + upperStep).isNaN()
        ) {
            upperStep *= 0.5
            iteration += 1
        }
        if (distanceSquared(parameter + upperStep).isNaN()) {
            upperStep = 0.0
        }

        parameter = when (
            val result = Numerics.fminbr(
                function = distanceSquared,
                interval = doubleArrayOf(
                    maxOf(parameter - lowerStep, minimum),
                    minOf(parameter + upperStep, maximum),
                ),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    GeometryError.NumericalProjectionFailure(result.error),
                )
            }
        }
        parameter = maxOf(parameter, globalMinimum)
        parameter = minOf(parameter, globalMaximum)
        return GMResult.Ok(
            ProjectionResult(
                point = doubleArrayOf(
                    1.0,
                    curve.x(parameter),
                    curve.y(parameter),
                ),
                parameter = parameter,
            ),
        )
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

    // JSXGraph: src/math/geometry.js -> meet
    fun meet(
        firstStandardForm: DoubleArray,
        secondStandardForm: DoubleArray,
        intersectionIndex: Int,
    ): DoubleArray {
        val firstIsLine =
            abs(firstStandardForm.valueOrNaN(3)) < Mat.eps
        val secondIsLine =
            abs(secondStandardForm.valueOrNaN(3)) < Mat.eps
        return when {
            firstIsLine && secondIsLine ->
                meetLineLine(firstStandardForm, secondStandardForm)

            !firstIsLine && secondIsLine ->
                meetLineCircle(
                    secondStandardForm,
                    firstStandardForm,
                    intersectionIndex,
                )

            firstIsLine && !secondIsLine ->
                meetLineCircle(
                    firstStandardForm,
                    secondStandardForm,
                    intersectionIndex,
                )

            else ->
                meetCircleCircle(
                    firstStandardForm,
                    secondStandardForm,
                    intersectionIndex,
                )
        }
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

    // JSXGraph: src/math/geometry.js -> meet3Planes
    fun meet3Planes(
        firstNormal: DoubleArray,
        firstDistance: Double,
        secondNormal: DoubleArray,
        secondDistance: Double,
        thirdNormal: DoubleArray,
        thirdDistance: Double,
    ): DoubleArray {
        val thirdFirstCross = Mat.crossProduct(
            thirdNormal.copyOfRange(1, 4),
            firstNormal.copyOfRange(1, 4),
        )
        val firstSecondCross = Mat.crossProduct(
            firstNormal.copyOfRange(1, 4),
            secondNormal.copyOfRange(1, 4),
        )
        val secondThirdCross = Mat.crossProduct(
            secondNormal.copyOfRange(1, 4),
            thirdNormal.copyOfRange(1, 4),
        )
        val denominator = Mat.innerProduct(
            firstNormal.copyOfRange(1, 4),
            secondThirdCross,
            3,
        )
        return doubleArrayOf(
            1.0,
            (
                firstDistance * secondThirdCross[0] +
                    secondDistance * thirdFirstCross[0] +
                    thirdDistance * firstSecondCross[0]
            ) / denominator,
            (
                firstDistance * secondThirdCross[1] +
                    secondDistance * thirdFirstCross[1] +
                    thirdDistance * firstSecondCross[1]
            ) / denominator,
            (
                firstDistance * secondThirdCross[2] +
                    secondDistance * thirdFirstCross[2] +
                    thirdDistance * firstSecondCross[2]
            ) / denominator,
        )
    }

    // JSXGraph: src/math/geometry.js -> meetPlanePlane
    fun meetPlanePlane(
        firstVector: DoubleArray,
        secondVector: DoubleArray,
        thirdVector: DoubleArray,
        fourthVector: DoubleArray,
    ): DoubleArray {
        val firstNormal = Mat.crossProduct(
            firstVector.copyOfRange(1, 4),
            secondVector.copyOfRange(1, 4),
        )
        val secondNormal = Mat.crossProduct(
            thirdVector.copyOfRange(1, 4),
            fourthVector.copyOfRange(1, 4),
        )
        val direction = Mat.crossProduct(firstNormal, secondNormal)
        return doubleArrayOf(
            0.0,
            direction[0],
            direction[1],
            direction[2],
        )
    }

    // JSXGraph: src/math/geometry.js -> meetPlaneSphere
    fun meetPlaneSphere(
        planeNormal: DoubleArray,
        planeDistance: Double,
        sphereCenter: DoubleArray,
        sphereRadius: Double,
    ): Circle3DIntersection {
        val signedDistance =
            Mat.innerProduct(planeNormal, sphereCenter, 4) -
                planeDistance
        return Circle3DIntersection(
            center = Mat.axpy(
                scalar = -signedDistance,
                x = planeNormal,
                y = sphereCenter,
            ),
            normal = planeNormal,
            radius = sqrt(
                sphereRadius * sphereRadius -
                    signedDistance * signedDistance,
            ),
        )
    }

    // JSXGraph: src/math/geometry.js -> meetSphereSphere
    fun meetSphereSphere(
        firstCenter: DoubleArray,
        firstRadius: Double,
        secondCenter: DoubleArray,
        secondRadius: Double,
    ): Circle3DIntersection {
        val centerDistance = point3DDistance(firstCenter, secondCenter)
        val skew =
            (firstRadius - secondRadius) *
                (firstRadius + secondRadius) /
                (centerDistance * centerDistance)
        val center = doubleArrayOf(
            1.0,
            0.5 *
                (
                    (1.0 - skew) * firstCenter[1] +
                        (1.0 + skew) * secondCenter[1]
                ),
            0.5 *
                (
                    (1.0 - skew) * firstCenter[2] +
                        (1.0 + skew) * secondCenter[2]
                ),
            0.5 *
                (
                    (1.0 - skew) * firstCenter[3] +
                        (1.0 + skew) * secondCenter[3]
                ),
        )
        val radiusSquared =
            0.5 *
                (
                    firstRadius * firstRadius +
                        secondRadius * secondRadius -
                        0.5 *
                        centerDistance *
                        centerDistance *
                        (1.0 + skew * skew)
                )
        return Circle3DIntersection(
            center = center,
            normal = Statistics.subtract(secondCenter, firstCenter),
            radius = sqrt(radiusSquared),
        )
    }

    // JSXGraph: src/math/geometry.js -> project3DTo3DPlane
    fun project3DTo3DPlane(
        point: DoubleArray,
        normal: DoubleArray,
        foot: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0),
    ): DoubleArray {
        val normalLength = Mat.norm(normal)
        val pointDistance = Mat.innerProduct(point, normal, 3)
        val footDistance = Mat.innerProduct(foot, normal, 3)
        val parameter = (pointDistance - footDistance) / normalLength
        return Mat.axpy(-parameter, normal, point)
    }

    // JSXGraph: src/math/geometry.js -> getPlaneBounds
    fun getPlaneBounds(
        firstVector: DoubleArray,
        secondVector: DoubleArray,
        point: DoubleArray,
        start: Double,
        end: Double,
    ): GMResult<DoubleArray?, NumericsError> {
        if (firstVector[2] + secondVector[0] == 0.0) {
            return GMResult.Ok(null)
        }

        val matrix = arrayOf(
            doubleArrayOf(firstVector[0], secondVector[0]),
            doubleArrayOf(firstVector[1], secondVector[1]),
        )
        val startSolution = when (
            val result = Numerics.Gauss(
                matrix,
                doubleArrayOf(start - point[0], start - point[1]),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val endSolution = when (
            val result = Numerics.Gauss(
                matrix,
                doubleArrayOf(end - point[0], end - point[1]),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            doubleArrayOf(
                startSolution[0],
                endSolution[0],
                startSolution[1],
                endSolution[1],
            ),
        )
    }

    // JSXGraph: src/math/geometry.js -> reuleauxPolygon
    internal fun reuleauxPolygon(
        points: List<CoordsElement>,
        vertexCount: Int,
    ): GMResult<ReuleauxPolygonInterpolation, GeometryError> {
        if (vertexCount <= 0 || vertexCount % 2 == 0) {
            return GMResult.Err(
                GeometryError.InvalidReuleauxVertexCount(vertexCount),
            )
        }
        if (points.size < vertexCount) {
            return GMResult.Err(
                GeometryError.InvalidReuleauxPointCount(
                    pointCount = points.size,
                    vertexCount = vertexCount,
                ),
            )
        }
        return GMResult.Ok(
            ReuleauxPolygonInterpolation(points, vertexCount),
        )
    }

    private fun point3DDistance(
        first: DoubleArray,
        second: DoubleArray,
    ): Double =
        if (
            first[0] * first[0] > 1.0e-12 &&
            second[0] * second[0] > 1.0e-12
        ) {
            Mat.hypot(
                second[1] - first[1],
                second[2] - first[2],
                second[3] - first[3],
            )
        } else {
            Double.POSITIVE_INFINITY
        }

    private fun midpoint(
        first: DoubleArray,
        second: DoubleArray,
    ): DoubleArray = doubleArrayOf(
        (first[0] + second[0]) * 0.5,
        (first[1] + second[1]) * 0.5,
    )

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
