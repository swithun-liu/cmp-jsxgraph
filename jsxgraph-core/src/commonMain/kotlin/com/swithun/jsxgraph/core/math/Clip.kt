/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/clip.js -> _getPath, _addToList, _isSeparator,
 * _noOverlap, _inbetween, sortIntersections, findIntersections;
 * src/math/geometry.js -> meetPathPath
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.Sector
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal sealed interface ClipError {
    data class UnsupportedPathElement(
        val elementType: String,
    ) : ClipError

    data class InvalidIntersectionIndex(
        val index: Double,
        val intersectionCount: Int,
    ) : ClipError

    data class InvalidTopology(
        val phase: String,
        val detail: String,
    ) : ClipError

    data class TraversalLimitExceeded(
        val phase: String,
        val limit: Int,
    ) : ClipError
}

internal data class ClipPathNode(
    val coordinates: DoubleArray,
    val position: Int,
)

internal enum class ClipIntersectionType {
    CROSSING,
    TOUCHING,
}

internal data class ClipIntersection(
    val coordinates: DoubleArray,
    val subjectSegmentIndex: Int,
    val clipSegmentIndex: Int,
    val subjectAlpha: Double,
    val clipAlpha: Double,
    val type: ClipIntersectionType,
)

/**
 * The translated path extraction and intersection portion of `JXG.Math.Clip`.
 *
 * Boolean entry/exit classification and path tracing live in
 * [BooleanClip] so the mutable Greiner-Hormann topology stays isolated from
 * the immutable ordering used by [meetPathPath].
 */
internal object Clip {
    private val duplicateEpsilon = Mat.eps * Mat.eps
    private val boundingBoxEpsilon = sqrt(Mat.eps)
    private val intersectionEpsilon = Mat.eps * 100.0

    // JSXGraph: src/math/geometry.js -> meetPathPath.
    internal fun meetPathPath(
        first: GeometryElement,
        second: GeometryElement,
        intersectionIndex: Double,
        rawIndexIsFunction: Boolean = false,
    ): GMResult<DoubleArray, ClipError> {
        val subject = when (val result = getPath(first)) {
            is GMResult.Ok -> result.value.toMutableList()
            is GMResult.Err -> return result
        }
        removeClosingDuplicate(subject, Mat.eps)

        val clip = when (val result = getPath(second)) {
            is GMResult.Ok -> result.value.toMutableList()
            is GMResult.Err -> return result
        }
        removeClosingDuplicate(clip, duplicateEpsilon)

        if (
            (!rawIndexIsFunction && intersectionIndex < 0.0) ||
            subject.isEmpty() ||
            clip.isEmpty()
        ) {
            return GMResult.Ok(idealCoordinates())
        }

        val intersections = findIntersections(subject, clip)
        if (intersectionIndex < intersections.size.toDouble()) {
            val integralIndex = intersectionIndex.toInt()
            if (
                !intersectionIndex.isFinite() ||
                integralIndex.toDouble() != intersectionIndex ||
                integralIndex !in intersections.indices
            ) {
                return GMResult.Err(
                    ClipError.InvalidIntersectionIndex(
                        index = intersectionIndex,
                        intersectionCount = intersections.size,
                    ),
                )
            }
            return GMResult.Ok(
                intersections[integralIndex].coordinates.copyOf(),
            )
        }
        return GMResult.Ok(idealCoordinates())
    }

    // JSXGraph: src/math/clip.js -> greinerHormann / intersection / union /
    // difference.
    internal fun booleanOperation(
        subject: GeometryElement,
        clip: GeometryElement,
        operation: ClipBooleanOperation,
    ): GMResult<ClipBooleanResult, ClipError> =
        BooleanClip.greinerHormann(subject, clip, operation)

    // JSXGraph: src/math/clip.js -> _getPath.
    internal fun getPath(
        element: GeometryElement,
    ): GMResult<List<ClipPathNode>, ClipError> {
        val path = mutableListOf<ClipPathNode>()
        when (element) {
            is Arc -> addArcPath(
                path = path,
                center = element.center.coords.usrCoords,
                radiusPoint = element.radiuspoint.coords.usrCoords,
                anglePoint = element.anglepoint.coords.usrCoords,
                radius = element.Radius(),
                includeCenter = false,
            )

            is Sector -> {
                if (element.type == Const.OBJECT_TYPE_SECTOR) {
                    addArcPath(
                        path = path,
                        center = element.center.coords.usrCoords,
                        radiusPoint =
                            element.radiuspoint.coords.usrCoords,
                        anglePoint =
                            element.anglepoint.coords.usrCoords,
                        radius = element.Radius(),
                        includeCenter = true,
                    )
                } else {
                    addCoordinatePath(
                        path = path,
                        coordinates = element.points.map { it.usrCoords },
                    )
                }
            }

            is Curve -> addCoordinatePath(
                path = path,
                coordinates = element.points.map { it.usrCoords },
            )

            is Polygon -> addCoordinatePath(
                path = path,
                coordinates =
                    element.vertices.map { it.coords.usrCoords },
            )

            is Circle -> addCirclePath(
                path = path,
                center = element.center.coords.usrCoords,
                radius = element.Radius(),
            )

            else -> return GMResult.Err(
                ClipError.UnsupportedPathElement(element.elType),
            )
        }
        return GMResult.Ok(path)
    }

    // JSXGraph: src/math/clip.js -> findIntersections.
    internal fun findIntersections(
        subject: List<ClipPathNode>,
        clip: List<ClipPathNode>,
    ): List<ClipIntersection> {
        val subjectCrossings =
            List(subject.size) { mutableListOf<ClipIntersection>() }
        var subjectHasMultipleComponents = false
        var clipHasMultipleComponents = false

        for (subjectIndex in subject.indices) {
            val subjectNextIndex = (subjectIndex + 1) % subject.size
            if (
                isSeparator(subject[subjectIndex]) ||
                isSeparator(subject[subjectNextIndex])
            ) {
                subjectHasMultipleComponents = true
                continue
            }
            if (
                subjectHasMultipleComponents &&
                subjectIndex == subject.lastIndex
            ) {
                break
            }

            val subjectStart = subject[subjectIndex].coordinates
            val subjectEnd = subject[subjectNextIndex].coordinates
            for (clipIndex in clip.indices) {
                val clipNextIndex = (clipIndex + 1) % clip.size
                if (
                    isSeparator(clip[clipIndex]) ||
                    isSeparator(clip[clipNextIndex])
                ) {
                    clipHasMultipleComponents = true
                    continue
                }
                if (
                    clipHasMultipleComponents &&
                    clipIndex == clip.lastIndex
                ) {
                    break
                }

                val clipStart = clip[clipIndex].coordinates
                val clipEnd = clip[clipNextIndex].coordinates
                if (
                    noOverlap(
                        firstStart = subjectStart,
                        firstEnd = subjectEnd,
                        secondStart = clipStart,
                        secondEnd = clipEnd,
                    )
                ) {
                    continue
                }

                val result = Geometry.meetSegmentSegment(
                    firstStart = subjectStart,
                    firstEnd = subjectEnd,
                    secondStart = clipStart,
                    secondEnd = clipEnd,
                )
                val subjectLength =
                    Geometry.distance(subjectStart, subjectEnd, 3)
                val clipLength =
                    Geometry.distance(clipStart, clipEnd, 3)
                val isCollinear =
                    result.firstParameter ==
                    Double.POSITIVE_INFINITY &&
                        result.secondParameter ==
                        Double.POSITIVE_INFINITY &&
                        Mat.norm(result.point, 3) <
                        intersectionEpsilon
                val isRegularIntersection =
                    result.firstParameter * subjectLength >
                    -intersectionEpsilon &&
                        result.firstParameter <
                        1.0 -
                        intersectionEpsilon / subjectLength &&
                        result.secondParameter * clipLength >
                        -intersectionEpsilon &&
                        result.secondParameter <
                        1.0 -
                        intersectionEpsilon / clipLength
                if (!isRegularIntersection && !isCollinear) {
                    continue
                }

                var coordinates = result.point
                var subjectAlpha = result.firstParameter
                var clipAlpha = result.secondParameter
                var type = ClipIntersectionType.CROSSING

                if (
                    abs(subjectAlpha) * subjectLength <
                    intersectionEpsilon ||
                    abs(clipAlpha) * clipLength <
                    intersectionEpsilon
                ) {
                    type = ClipIntersectionType.TOUCHING
                    if (
                        abs(subjectAlpha) * subjectLength <
                        intersectionEpsilon
                    ) {
                        subjectAlpha = 0.0
                    }
                    if (
                        abs(clipAlpha) * clipLength <
                        intersectionEpsilon
                    ) {
                        clipAlpha = 0.0
                    }
                    coordinates =
                        if (subjectAlpha == 0.0) {
                            subjectStart
                        } else {
                            clipStart
                        }
                } else if (isCollinear) {
                    val subjectStartOnClip = inBetween(
                        subjectStart,
                        clipStart,
                        clipEnd,
                    )
                    if (
                        subjectStartOnClip >= 0.0 &&
                        subjectStartOnClip < 1.0
                    ) {
                        subjectCrossings[subjectIndex] +=
                            ClipIntersection(
                                coordinates = subjectStart.copyOf(),
                                subjectSegmentIndex = subjectIndex,
                                clipSegmentIndex = clipIndex,
                                subjectAlpha = 0.0,
                                clipAlpha = subjectStartOnClip,
                                type = ClipIntersectionType.TOUCHING,
                            )
                    }

                    val clipStartOnSubject = inBetween(
                        clipStart,
                        subjectStart,
                        subjectEnd,
                    )
                    if (
                        Geometry.distance(
                            subjectStart,
                            clipStart,
                            3,
                        ) > intersectionEpsilon &&
                        clipStartOnSubject >= 0.0 &&
                        clipStartOnSubject < 1.0
                    ) {
                        subjectCrossings[subjectIndex] +=
                            ClipIntersection(
                                coordinates = clipStart.copyOf(),
                                subjectSegmentIndex = subjectIndex,
                                clipSegmentIndex = clipIndex,
                                subjectAlpha = clipStartOnSubject,
                                clipAlpha = 0.0,
                                type = ClipIntersectionType.TOUCHING,
                            )
                    }
                    continue
                }

                subjectCrossings[subjectIndex] += ClipIntersection(
                    coordinates = coordinates.copyOf(),
                    subjectSegmentIndex = subjectIndex,
                    clipSegmentIndex = clipIndex,
                    subjectAlpha = subjectAlpha,
                    clipAlpha = clipAlpha,
                    type = type,
                )
            }
        }

        return subjectCrossings.flatMap(::sortIntersections)
    }

    // JSXGraph: src/math/clip.js -> sortIntersections.
    private fun sortIntersections(
        crossings: List<ClipIntersection>,
    ): List<ClipIntersection> {
        val sorted = mutableListOf<ClipIntersection>()
        for (intersection in crossings) {
            val insertionIndex = sorted.indexOfFirst { existing ->
                !(intersection.subjectAlpha > existing.subjectAlpha)
            }
            if (insertionIndex < 0) {
                sorted += intersection
            } else {
                sorted.add(insertionIndex, intersection)
            }
        }
        return sorted
    }

    // JSXGraph: src/math/clip.js -> _addToList.
    private fun addToList(
        path: MutableList<ClipPathNode>,
        coordinates: DoubleArray,
        position: Int,
    ) {
        val previous = path.lastOrNull()?.coordinates
        if (
            previous != null &&
            abs(previous[0] - coordinates[0]) < duplicateEpsilon &&
            abs(previous[1] - coordinates[1]) < duplicateEpsilon &&
            abs(previous[2] - coordinates[2]) < duplicateEpsilon
        ) {
            return
        }
        path += ClipPathNode(
            coordinates = coordinates.copyOf(),
            position = position,
        )
    }

    private fun addCoordinatePath(
        path: MutableList<ClipPathNode>,
        coordinates: List<DoubleArray>,
    ) {
        for ((index, coordinate) in coordinates.withIndex()) {
            addToList(path, coordinate, index)
        }
    }

    private fun addArcPath(
        path: MutableList<ClipPathNode>,
        center: DoubleArray,
        radiusPoint: DoubleArray,
        anglePoint: DoubleArray,
        radius: Double,
        includeCenter: Boolean,
    ) {
        val angle = Geometry.rad(
            first = radiusPoint.copyOfRange(1, 3),
            vertex = center.copyOfRange(1, 3),
            third = anglePoint.copyOfRange(1, 3),
        )
        val steps = floor(angle * 180.0 / PI).toInt()
        val radiansPerStep = angle / steps
        val startAngle = atan2(
            radiusPoint[2] - center[2],
            radiusPoint[1] - center[1],
        )

        if (includeCenter) {
            addToList(path, center, 0)
        }
        for (index in 0..steps) {
            addToList(
                path = path,
                coordinates = doubleArrayOf(
                    center[0],
                    center[1] +
                        cos(index * radiansPerStep + startAngle) *
                        radius,
                    center[2] +
                        sin(index * radiansPerStep + startAngle) *
                        radius,
                ),
                position = index + 1,
            )
        }
        if (includeCenter) {
            addToList(path, center, steps + 2)
        }
    }

    private fun addCirclePath(
        path: MutableList<ClipPathNode>,
        center: DoubleArray,
        radius: Double,
    ) {
        val steps = 359
        val radiansPerStep = 2.0 * PI / steps
        for (index in 0..steps) {
            addToList(
                path = path,
                coordinates = doubleArrayOf(
                    center[0],
                    center[1] + cos(index * radiansPerStep) * radius,
                    center[2] + sin(index * radiansPerStep) * radius,
                ),
                position = index,
            )
        }
    }

    private fun removeClosingDuplicate(
        path: MutableList<ClipPathNode>,
        epsilon: Double,
    ) {
        if (
            path.isNotEmpty() &&
            Geometry.distance(
                path.first().coordinates,
                path.last().coordinates,
                3,
            ) < epsilon
        ) {
            path.removeAt(path.lastIndex)
        }
    }

    // JSXGraph: src/math/clip.js -> _isSeparator.
    private fun isSeparator(node: ClipPathNode): Boolean =
        node.coordinates[1].isNaN() &&
            node.coordinates[2].isNaN()

    // JSXGraph: src/math/clip.js -> _noOverlap.
    private fun noOverlap(
        firstStart: DoubleArray,
        firstEnd: DoubleArray,
        secondStart: DoubleArray,
        secondEnd: DoubleArray,
    ): Boolean {
        for (index in 0 until 3) {
            val firstMinimum =
                min(firstStart[index], firstEnd[index])
            val firstMaximum =
                max(firstStart[index], firstEnd[index])
            val secondMinimum =
                min(secondStart[index], secondEnd[index])
            val secondMaximum =
                max(secondStart[index], secondEnd[index])
            if (
                firstMaximum <
                secondMinimum - boundingBoxEpsilon ||
                firstMinimum >
                secondMaximum + boundingBoxEpsilon
            ) {
                return true
            }
        }
        return false
    }

    // JSXGraph: src/math/clip.js -> _inbetween.
    private fun inBetween(
        point: DoubleArray,
        segmentStart: DoubleArray,
        segmentEnd: DoubleArray,
    ): Double {
        val segmentX = segmentEnd[1] - segmentStart[1]
        val segmentY = segmentEnd[2] - segmentStart[2]
        val pointX = point[1] - segmentStart[1]
        val pointY = point[2] - segmentStart[2]
        if (
            segmentX == 0.0 &&
            segmentY == 0.0 &&
            pointX == 0.0 &&
            pointY == 0.0
        ) {
            return 1.0
        }
        var alpha =
            if (
                abs(pointX) < duplicateEpsilon &&
                abs(segmentX) < duplicateEpsilon
            ) {
                pointY / segmentY
            } else {
                pointX / segmentX
            }
        if (abs(alpha) < duplicateEpsilon) {
            alpha = 0.0
        }
        return alpha
    }

    private fun idealCoordinates(): DoubleArray =
        doubleArrayOf(0.0, 0.0, 0.0)
}
