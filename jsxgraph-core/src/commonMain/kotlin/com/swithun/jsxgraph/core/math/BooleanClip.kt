/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/clip.js -> makeDoublyLinkedList, Vertex,
 * sortIntersections, findIntersections, _getPosition,
 * _classifyDegenerateIntersections, _handleIntersectionChains,
 * _handleFullyDegenerateCase, _getStatus, markEntryExit, _stayOnPath,
 * _addVertex, tracing, isEmptyCase, _getCoordsArrays,
 * handleEmptyIntersection, _countCrossingIntersections, greinerHormann,
 * intersection, union, difference.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.GeometryElement
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

internal enum class ClipBooleanOperation {
    INTERSECTION,
    UNION,
    DIFFERENCE,
}

internal data class ClipBooleanResult(
    val x: DoubleArray,
    val y: DoubleArray,
)

private enum class BooleanIntersectionType {
    CROSSING,
    TOUCHING,
    BOUNCING,
    DELAYED_CROSSING,
    DELAYED_BOUNCING,
}

private enum class BooleanPathStatus {
    ENTRY,
    EXIT,
}

private enum class BooleanSide {
    ON,
    LEFT,
    RIGHT,
}

private data class BooleanIntersectionData(
    var alpha: Double,
    val path: MutableList<BooleanClipVertex>,
    val pathName: String,
    var done: Boolean = false,
    var type: BooleanIntersectionType,
    var index: Int = 0,
)

private class BooleanClipVertex(
    val coordinates: DoubleArray,
    val position: Int,
) {
    var data: BooleanIntersectionData? = null
    var neighbour: BooleanClipVertex? = null
    var entryExit: BooleanPathStatus? = null
    var delayedStatus: Pair<BooleanSide, BooleanSide>? = null
    var next: BooleanClipVertex = this
    var previous: BooleanClipVertex = this
    var end: Boolean = false
    var starter: Boolean = false
    var tours: Int? = null

    val intersection: Boolean
        get() = data != null
}

/**
 * Mutable Greiner-Hormann topology translated from JSXGraph 1.13.3.
 *
 * This is separate from [Clip.findIntersections], whose immutable result order
 * is part of the existing path-intersection API.
 */
internal object BooleanClip {
    private const val CLASSIFICATION_INTERSECTION_LIMIT = 1_000
    private const val CHAIN_INTERSECTION_LIMIT = 1_000
    private const val MARK_TRAVERSAL_LIMIT = 10_000
    private const val TRACE_TRAVERSAL_LIMIT = 10_000
    private const val INTERSECTION_LIMIT = 10_000

    private val duplicateEpsilon = Mat.eps * Mat.eps
    private val boundingBoxEpsilon = sqrt(Mat.eps)
    private val intersectionEpsilon = Mat.eps * 100.0

    // JSXGraph: src/math/clip.js -> greinerHormann.
    internal fun greinerHormann(
        subjectElement: GeometryElement,
        clipElement: GeometryElement,
        operation: ClipBooleanOperation,
    ): GMResult<ClipBooleanResult, ClipError> {
        val subject = when (val result = mutablePath(subjectElement)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        removeClosingDuplicate(subject, Mat.eps)

        val clip = when (val result = mutablePath(clipElement)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        removeClosingDuplicate(clip, duplicateEpsilon)

        if (isEmptyCase(subject, clip, operation)) {
            return GMResult.Ok(emptyResult())
        }

        val subjectStarters = makeDoublyLinkedList(subject)
        val clipStarters = makeDoublyLinkedList(clip)
        val subjectIntersections = when (
            val result = findIntersections(subject, clip)
        ) {
            is GMResult.Ok -> result.value.first
            is GMResult.Err -> return result
        }

        handleFullyDegenerateCase(subject, clip)
        when (val result = markEntryExit(subject, clip, subjectStarters)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        when (val result = markEntryExit(clip, subject, clipStarters)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }

        if (countCrossingIntersections(subjectIntersections) == 0) {
            return handleEmptyIntersection(subject, clip, operation)
        }
        return tracing(subject, subjectIntersections, operation)
    }

    private fun mutablePath(
        element: GeometryElement,
    ): GMResult<MutableList<BooleanClipVertex>, ClipError> =
        when (val result = Clip.getPath(element)) {
            is GMResult.Ok -> GMResult.Ok(
                result.value.mapTo(mutableListOf()) { node ->
                    BooleanClipVertex(
                        coordinates = node.coordinates.copyOf(),
                        position = node.position,
                    )
                },
            )
            is GMResult.Err -> result
        }

    // JSXGraph: src/math/clip.js -> makeDoublyLinkedList.
    private fun makeDoublyLinkedList(
        path: MutableList<BooleanClipVertex>,
    ): List<Int> {
        var first: Int? = null
        val components = mutableListOf<Int>()
        val size = path.size
        for (index in path.indices) {
            val current = path[index]
            if (isSeparator(current)) {
                current.next = path[(index + 1) % size]
                current.previous = path[(size + index - 1) % size]
                continue
            }

            val firstIndex = first ?: index.also {
                first = it
                components += it
            }
            if (
                isSeparator(path[(index + 1) % size]) ||
                index == path.lastIndex
            ) {
                current.next = path[firstIndex]
                path[firstIndex].previous = current
                current.end = true
                first = null
            } else {
                current.next = path[(index + 1) % size]
                path[firstIndex].previous = current
            }
            if (!isSeparator(path[(size + index - 1) % size])) {
                current.previous = path[(size + index - 1) % size]
            }
        }
        return components
    }

    // JSXGraph: src/math/clip.js -> findIntersections.
    private fun findIntersections(
        subject: MutableList<BooleanClipVertex>,
        clip: MutableList<BooleanClipVertex>,
    ): GMResult<
        Pair<List<BooleanClipVertex>, List<BooleanClipVertex>>,
        ClipError,
        > {
        val subjectCrossings =
            List(subject.size) { mutableListOf<BooleanClipVertex>() }
        val clipCrossings =
            List(clip.size) { mutableListOf<BooleanClipVertex>() }
        var subjectHasMultipleComponents = false
        var clipHasMultipleComponents = false
        var intersectionCount = 0

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
                        subjectStart,
                        subjectEnd,
                        clipStart,
                        clipEnd,
                    )
                ) {
                    continue
                }

                val intersection = Geometry.meetSegmentSegment(
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
                    intersection.firstParameter ==
                    Double.POSITIVE_INFINITY &&
                        intersection.secondParameter ==
                        Double.POSITIVE_INFINITY &&
                        Mat.norm(intersection.point, 3) <
                        intersectionEpsilon
                val isRegularIntersection =
                    intersection.firstParameter * subjectLength >
                    -intersectionEpsilon &&
                        intersection.firstParameter <
                        1.0 -
                        intersectionEpsilon / subjectLength &&
                        intersection.secondParameter * clipLength >
                        -intersectionEpsilon &&
                        intersection.secondParameter <
                        1.0 -
                        intersectionEpsilon / clipLength
                if (!isRegularIntersection && !isCollinear) {
                    continue
                }

                var coordinates = intersection.point.copyOf()
                var subjectAlpha = intersection.firstParameter
                var clipAlpha = intersection.secondParameter
                var type = BooleanIntersectionType.CROSSING
                if (
                    abs(subjectAlpha) * subjectLength <
                    intersectionEpsilon ||
                    abs(clipAlpha) * clipLength <
                    intersectionEpsilon
                ) {
                    type = BooleanIntersectionType.TOUCHING
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
                    coordinates = (
                        if (subjectAlpha == 0.0) {
                            subjectStart
                        } else {
                            clipStart
                        }
                    ).copyOf()
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
                        addIntersectionPair(
                            coordinates = subjectStart.copyOf(),
                            subjectIndex = subjectIndex,
                            clipIndex = clipIndex,
                            subjectAlpha = 0.0,
                            clipAlpha = subjectStartOnClip,
                            subject = subject,
                            clip = clip,
                            subjectCrossings = subjectCrossings,
                            clipCrossings = clipCrossings,
                            type = BooleanIntersectionType.TOUCHING,
                        )
                        intersectionCount++
                    }

                    val clipStartOnSubject = inBetween(
                        clipStart,
                        subjectStart,
                        subjectEnd,
                    )
                    if (
                        Geometry.distance(subjectStart, clipStart, 3) >
                        intersectionEpsilon &&
                        clipStartOnSubject >= 0.0 &&
                        clipStartOnSubject < 1.0
                    ) {
                        addIntersectionPair(
                            coordinates = clipStart.copyOf(),
                            subjectIndex = subjectIndex,
                            clipIndex = clipIndex,
                            subjectAlpha = clipStartOnSubject,
                            clipAlpha = 0.0,
                            subject = subject,
                            clip = clip,
                            subjectCrossings = subjectCrossings,
                            clipCrossings = clipCrossings,
                            type = BooleanIntersectionType.TOUCHING,
                        )
                        intersectionCount++
                    }
                    if (intersectionCount > INTERSECTION_LIMIT) {
                        return traversalLimit(
                            phase = "findIntersections",
                            limit = INTERSECTION_LIMIT,
                        )
                    }
                    continue
                }

                addIntersectionPair(
                    coordinates = coordinates,
                    subjectIndex = subjectIndex,
                    clipIndex = clipIndex,
                    subjectAlpha = subjectAlpha,
                    clipAlpha = clipAlpha,
                    subject = subject,
                    clip = clip,
                    subjectCrossings = subjectCrossings,
                    clipCrossings = clipCrossings,
                    type = type,
                )
                intersectionCount++
                if (intersectionCount > INTERSECTION_LIMIT) {
                    return traversalLimit(
                        phase = "findIntersections",
                        limit = INTERSECTION_LIMIT,
                    )
                }
            }
        }

        val subjectIntersections = when (
            val result = sortIntersections(subjectCrossings)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        for ((index, vertex) in subjectIntersections.withIndex()) {
            val data = vertex.data
                ?: return invalidTopology(
                    "findIntersections",
                    "subject intersection has no data",
                )
            data.index = index
            val neighbour = vertex.neighbour
                ?: return invalidTopology(
                    "findIntersections",
                    "subject intersection has no neighbour",
                )
            val neighbourData = neighbour.data
                ?: return invalidTopology(
                    "findIntersections",
                    "clip intersection has no data",
                )
            neighbourData.index = index
        }
        val clipIntersections = when (
            val result = sortIntersections(clipCrossings)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(subjectIntersections to clipIntersections)
    }

    private fun addIntersectionPair(
        coordinates: DoubleArray,
        subjectIndex: Int,
        clipIndex: Int,
        subjectAlpha: Double,
        clipAlpha: Double,
        subject: MutableList<BooleanClipVertex>,
        clip: MutableList<BooleanClipVertex>,
        subjectCrossings: List<MutableList<BooleanClipVertex>>,
        clipCrossings: List<MutableList<BooleanClipVertex>>,
        type: BooleanIntersectionType,
    ) {
        val subjectVertex = BooleanClipVertex(
            coordinates = coordinates,
            position = subjectIndex,
        )
        val clipVertex = BooleanClipVertex(
            coordinates = coordinates,
            position = clipIndex,
        )
        subjectVertex.data = BooleanIntersectionData(
            alpha = subjectAlpha,
            path = subject,
            pathName = "S",
            type = type,
        )
        clipVertex.data = BooleanIntersectionData(
            alpha = clipAlpha,
            path = clip,
            pathName = "C",
            type = type,
        )
        subjectVertex.neighbour = clipVertex
        clipVertex.neighbour = subjectVertex
        subjectCrossings[subjectIndex] += subjectVertex
        clipCrossings[clipIndex] += clipVertex
    }

    // JSXGraph: src/math/clip.js -> sortIntersections.
    private fun sortIntersections(
        crossingsBySegment: List<MutableList<BooleanClipVertex>>,
    ): GMResult<List<BooleanClipVertex>, ClipError> {
        val intersections = mutableListOf<BooleanClipVertex>()
        for ((segmentIndex, crossings) in crossingsBySegment.withIndex()) {
            val sorted = mutableListOf<BooleanClipVertex>()
            for (vertex in crossings) {
                val alpha = vertex.data?.alpha
                    ?: return invalidTopology(
                        "sortIntersections",
                        "intersection has no data",
                    )
                val insertionIndex = sorted.indexOfFirst { existing ->
                    val existingAlpha = existing.data?.alpha
                        ?: Double.NaN
                    !(alpha > existingAlpha)
                }
                if (insertionIndex < 0) {
                    sorted += vertex
                } else {
                    sorted.add(insertionIndex, vertex)
                }
            }
            if (sorted.isEmpty()) {
                continue
            }

            var first = sorted.first()
            val firstData = first.data
                ?: return invalidTopology(
                    "sortIntersections",
                    "first intersection has no data",
                )
            val pathNode = firstData.path[first.position]
            val nextNode = pathNode.next
            if (segmentIndex == crossingsBySegment.lastIndex) {
                pathNode.end = false
            }

            if (
                firstData.alpha == 0.0 &&
                firstData.type == BooleanIntersectionType.TOUCHING
            ) {
                pathNode.data = firstData
                pathNode.neighbour = first.neighbour
                val neighbour = pathNode.neighbour
                    ?: return invalidTopology(
                        "sortIntersections",
                        "touching intersection has no neighbour",
                    )
                neighbour.neighbour = pathNode
                pathNode.entryExit = null
                sorted[0] = pathNode
                first = pathNode
            } else {
                first.previous = pathNode
                pathNode.next = first
            }

            for (index in 1..sorted.lastIndex) {
                val current = sorted[index]
                current.previous = sorted[index - 1]
                current.previous.next = current
            }
            val last = sorted.last()
            last.next = nextNode
            nextNode.previous = last
            if (segmentIndex == crossingsBySegment.lastIndex) {
                last.end = true
            }
            intersections += sorted
        }
        return GMResult.Ok(intersections)
    }

    // JSXGraph: src/math/clip.js -> _getPosition.
    private fun getPosition(
        point: DoubleArray,
        first: DoubleArray,
        second: DoubleArray,
        third: DoubleArray,
    ): BooleanSide {
        val firstDeterminant = Geometry.det3p(point, first, second)
        val secondDeterminant = Geometry.det3p(point, second, third)
        val turn = Geometry.det3p(first, second, third)
        if (turn >= 0.0) {
            return if (
                firstDeterminant >= 0.0 &&
                secondDeterminant >= 0.0
            ) {
                BooleanSide.LEFT
            } else {
                BooleanSide.RIGHT
            }
        }
        return if (
            firstDeterminant >= 0.0 ||
            secondDeterminant >= 0.0
        ) {
            BooleanSide.LEFT
        } else {
            BooleanSide.RIGHT
        }
    }

    // JSXGraph: src/math/clip.js -> _classifyDegenerateIntersections.
    private fun classifyDegenerateIntersections(
        start: BooleanClipVertex,
    ): GMResult<Unit, ClipError> {
        var current = start
        var intersectionCount = 0
        start.tours = 0
        while (true) {
            val data = current.data
            if (data?.type == BooleanIntersectionType.TOUCHING) {
                var nextCoordinates = current.next.coordinates
                var previousCoordinates = current.previous.coordinates
                if (
                    Geometry.distance(
                        current.coordinates,
                        nextCoordinates,
                        3,
                    ) < Mat.eps
                ) {
                    nextCoordinates = current.next.next.coordinates
                }
                if (
                    Geometry.distance(
                        current.coordinates,
                        previousCoordinates,
                        3,
                    ) < Mat.eps
                ) {
                    previousCoordinates =
                        current.previous.previous.coordinates
                }

                val neighbour = current.neighbour
                    ?: return invalidTopology(
                        "classifyDegenerateIntersections",
                        "intersection has no neighbour",
                    )
                var neighbourPrevious = neighbour.previous.coordinates
                var neighbourNext = neighbour.next.coordinates
                if (
                    Geometry.distance(
                        neighbour.coordinates,
                        neighbourNext,
                        3,
                    ) < Mat.eps
                ) {
                    neighbourNext = neighbour.next.next.coordinates
                }
                if (
                    Geometry.distance(
                        neighbour.coordinates,
                        neighbourPrevious,
                        3,
                    ) < Mat.eps
                ) {
                    neighbourPrevious =
                        neighbour.previous.previous.coordinates
                }

                var firstDeterminant = Geometry.det3p(
                    current.coordinates,
                    previousCoordinates,
                    neighbourPrevious,
                )
                var secondDeterminant = Geometry.det3p(
                    current.coordinates,
                    nextCoordinates,
                    neighbourNext,
                )
                var thirdDeterminant = Geometry.det3p(
                    current.coordinates,
                    previousCoordinates,
                    neighbourNext,
                )
                var fourthDeterminant = Geometry.det3p(
                    current.coordinates,
                    nextCoordinates,
                    neighbourPrevious,
                )

                if (
                    firstDeterminant == 0.0 &&
                    secondDeterminant == 0.0 &&
                    thirdDeterminant == 0.0 &&
                    fourthDeterminant == 0.0
                ) {
                    current.coordinates[1] *=
                        1.0 + Random.nextDouble() * Mat.eps
                    current.coordinates[2] *=
                        1.0 + Random.nextDouble() * Mat.eps
                    neighbour.coordinates[1] = current.coordinates[1]
                    neighbour.coordinates[2] = current.coordinates[2]
                    firstDeterminant = Geometry.det3p(
                        current.coordinates,
                        previousCoordinates,
                        neighbourPrevious,
                    )
                    secondDeterminant = Geometry.det3p(
                        current.coordinates,
                        nextCoordinates,
                        neighbourNext,
                    )
                    thirdDeterminant = Geometry.det3p(
                        current.coordinates,
                        previousCoordinates,
                        neighbourNext,
                    )
                    fourthDeterminant = Geometry.det3p(
                        current.coordinates,
                        nextCoordinates,
                        neighbourPrevious,
                    )
                }

                val oppositeDirection = when {
                    firstDeterminant == 0.0 ->
                        Geometry.affineRatio(
                            current.coordinates,
                            previousCoordinates,
                            neighbourPrevious,
                        ) < 0.0
                    secondDeterminant == 0.0 ->
                        Geometry.affineRatio(
                            current.coordinates,
                            nextCoordinates,
                            neighbourNext,
                        ) < 0.0
                    thirdDeterminant == 0.0 ->
                        Geometry.affineRatio(
                            current.coordinates,
                            previousCoordinates,
                            neighbourNext,
                        ) > 0.0
                    fourthDeterminant == 0.0 ->
                        Geometry.affineRatio(
                            current.coordinates,
                            nextCoordinates,
                            neighbourPrevious,
                        ) > 0.0
                    else -> false
                }
                if (oppositeDirection) {
                    val coordinates = neighbourPrevious
                    neighbourPrevious = neighbourNext
                    neighbourNext = coordinates
                    val determinant = firstDeterminant
                    firstDeterminant = thirdDeterminant
                    thirdDeterminant = determinant
                    val nextDeterminant = secondDeterminant
                    secondDeterminant = fourthDeterminant
                    fourthDeterminant = nextDeterminant
                }

                when {
                    firstDeterminant == 0.0 &&
                        secondDeterminant == 0.0 -> {
                        current.delayedStatus =
                            BooleanSide.ON to BooleanSide.ON
                    }
                    firstDeterminant == 0.0 -> {
                        current.delayedStatus = BooleanSide.ON to
                            getPosition(
                                nextCoordinates,
                                neighbourPrevious,
                                neighbour.coordinates,
                                neighbourNext,
                            )
                    }
                    secondDeterminant == 0.0 -> {
                        current.delayedStatus =
                            getPosition(
                                previousCoordinates,
                                neighbourPrevious,
                                neighbour.coordinates,
                                neighbourNext,
                            ) to BooleanSide.ON
                    }
                    current.delayedStatus == null -> {
                        data.type =
                            if (
                                getPosition(
                                    previousCoordinates,
                                    neighbourPrevious,
                                    neighbour.coordinates,
                                    neighbourNext,
                                ) !=
                                getPosition(
                                    nextCoordinates,
                                    neighbourPrevious,
                                    neighbour.coordinates,
                                    neighbourNext,
                                )
                            ) {
                                BooleanIntersectionType.CROSSING
                            } else {
                                BooleanIntersectionType.BOUNCING
                            }
                    }
                }
            }

            current.tours = current.tours?.plus(1)
            if (
                (current.tours ?: 0) > 3 ||
                current.end
            ) {
                current.tours = null
                break
            }
            if (current.intersection) {
                intersectionCount++
            }
            if (
                intersectionCount >
                CLASSIFICATION_INTERSECTION_LIMIT
            ) {
                return traversalLimit(
                    phase = "classifyDegenerateIntersections",
                    limit = CLASSIFICATION_INTERSECTION_LIMIT,
                )
            }
            current = current.next
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph: src/math/clip.js -> _handleIntersectionChains.
    private fun handleIntersectionChains(
        start: BooleanClipVertex,
    ): GMResult<Unit, ClipError> {
        var current = start
        var intersectionCount = 0
        var startStatus: BooleanSide? = null
        var chainStart: BooleanClipVertex? = null
        var intersectionChain = false
        var waitForExit = false
        while (true) {
            val data = current.data
            if (data != null) {
                if (data.type == BooleanIntersectionType.TOUCHING) {
                    val delayed = current.delayedStatus
                        ?: return invalidTopology(
                            "handleIntersectionChains",
                            "touching intersection has no delayed status",
                        )
                    if (
                        delayed.first != BooleanSide.ON &&
                        delayed.second == BooleanSide.ON
                    ) {
                        intersectionChain = true
                        chainStart = current
                        startStatus = delayed.first
                    } else if (
                        intersectionChain &&
                        delayed.first == BooleanSide.ON &&
                        delayed.second == BooleanSide.ON
                    ) {
                        data.type = BooleanIntersectionType.BOUNCING
                    } else if (
                        intersectionChain &&
                        delayed.first == BooleanSide.ON &&
                        delayed.second != BooleanSide.ON
                    ) {
                        intersectionChain = false
                        val first = chainStart
                            ?: return invalidTopology(
                                "handleIntersectionChains",
                                "intersection chain has no start",
                            )
                        val firstData = first.data
                            ?: return invalidTopology(
                                "handleIntersectionChains",
                                "intersection chain start has no data",
                            )
                        if (startStatus == delayed.second) {
                            firstData.type =
                                BooleanIntersectionType.DELAYED_BOUNCING
                            data.type =
                                BooleanIntersectionType.DELAYED_BOUNCING
                        } else {
                            firstData.type =
                                BooleanIntersectionType.DELAYED_CROSSING
                            data.type =
                                BooleanIntersectionType.DELAYED_CROSSING
                        }
                    }
                }
                intersectionCount++
            }
            if (current.end) {
                waitForExit = true
            }
            if (waitForExit && !intersectionChain) {
                break
            }
            if (intersectionCount > CHAIN_INTERSECTION_LIMIT) {
                return traversalLimit(
                    phase = "handleIntersectionChains",
                    limit = CHAIN_INTERSECTION_LIMIT,
                )
            }
            current = current.next
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph: src/math/clip.js -> _handleFullyDegenerateCase.
    private fun handleFullyDegenerateCase(
        subject: MutableList<BooleanClipVertex>,
        clip: MutableList<BooleanClipVertex>,
    ) {
        val paths = listOf(subject, clip)
        for (pathIndex in paths.indices) {
            val path = paths[pathIndex]
            if (path.isEmpty() || path.any { !it.intersection }) {
                continue
            }
            val other = paths[(pathIndex + 1) % 2]
            for (index in path.indices) {
                val first = path[index].coordinates
                val second = path[index].next.coordinates
                val midpoint = doubleArrayOf(
                    (first[0] + second[0]) * 0.5,
                    (first[1] + second[1]) * 0.5,
                    (first[2] + second[2]) * 0.5,
                )
                var isOnOtherPath = false
                for (otherIndex in other.indices) {
                    if (
                        abs(
                            Geometry.det3p(
                                other[otherIndex].coordinates,
                                other[
                                    (otherIndex + 1) % other.size
                                ].coordinates,
                                midpoint,
                            ),
                        ) < Mat.eps
                    ) {
                        isOnOtherPath = true
                        break
                    }
                }
                if (!isOnOtherPath) {
                    val node = BooleanClipVertex(
                        coordinates = midpoint,
                        position = index,
                    )
                    val next = path[index].next
                    path[index].next = node
                    node.previous = path[index]
                    node.next = next
                    next.previous = node
                    if (path[index].end) {
                        path[index].end = false
                        node.end = true
                    }
                    break
                }
            }
        }
    }

    // JSXGraph: src/math/clip.js -> _getStatus.
    private fun getStatus(
        start: BooleanClipVertex,
        path: List<BooleanClipVertex>,
    ): Pair<BooleanClipVertex, BooleanPathStatus> {
        var current = start
        while (current.intersection) {
            if (current.end) {
                break
            }
            current = current.next
        }
        val status =
            if (
                Geometry.windingNumber(
                    current.coordinates,
                    path.map { it.coordinates },
                ) == 0
            ) {
                BooleanPathStatus.ENTRY
            } else {
                BooleanPathStatus.EXIT
            }
        return current to status
    }

    // JSXGraph: src/math/clip.js -> markEntryExit.
    private fun markEntryExit(
        path: List<BooleanClipVertex>,
        otherPath: List<BooleanClipVertex>,
        starters: List<Int>,
    ): GMResult<Unit, ClipError> {
        for (startIndex in starters) {
            val start = path.getOrNull(startIndex)
                ?: return invalidTopology(
                    "markEntryExit",
                    "component starter is outside the path",
                )
            when (val result = classifyDegenerateIntersections(start)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (val result = handleIntersectionChains(start)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }

            val initial = getStatus(start, otherPath)
            var current = initial.first
            var status = initial.second
            current.starter = true
            var count = 0
            var chainStart: BooleanClipVertex? = null
            var intersectionChain = 0
            while (true) {
                val data = current.data
                if (data != null) {
                    if (
                        data.type == BooleanIntersectionType.CROSSING &&
                        intersectionChain == 1
                    ) {
                        val first = chainStart
                            ?: return invalidTopology(
                                "markEntryExit",
                                "delayed crossing has no start",
                            )
                        val firstData = first.data
                            ?: return invalidTopology(
                                "markEntryExit",
                                "delayed crossing start has no data",
                            )
                        first.entryExit = status
                        if (status == BooleanPathStatus.EXIT) {
                            firstData.type =
                                BooleanIntersectionType.CROSSING
                        }
                        intersectionChain = 2
                    }

                    if (
                        data.type == BooleanIntersectionType.CROSSING ||
                        data.type ==
                        BooleanIntersectionType.DELAYED_BOUNCING
                    ) {
                        current.entryExit = status
                        status = status.toggle()
                    }

                    if (
                        data.type ==
                        BooleanIntersectionType.DELAYED_CROSSING
                    ) {
                        when (intersectionChain) {
                            0 -> {
                                chainStart = current
                                intersectionChain = 1
                            }
                            1 -> {
                                val first = chainStart
                                    ?: return invalidTopology(
                                        "markEntryExit",
                                        "delayed crossing has no start",
                                    )
                                val firstData = first.data
                                    ?: return invalidTopology(
                                        "markEntryExit",
                                        "delayed crossing start has no data",
                                    )
                                current.entryExit = status
                                first.entryExit = status
                                if (status == BooleanPathStatus.EXIT) {
                                    firstData.type =
                                        BooleanIntersectionType.CROSSING
                                } else {
                                    data.type =
                                        BooleanIntersectionType.CROSSING
                                }
                                status = status.toggle()
                                chainStart = null
                                intersectionChain = 0
                            }
                            2 -> {
                                current.entryExit = status
                                data.type =
                                    BooleanIntersectionType.CROSSING
                                status = status.toggle()
                                chainStart = null
                                intersectionChain = 0
                            }
                        }
                    }
                }

                current = current.next
                if (current.starter) {
                    break
                }
                if (count > MARK_TRAVERSAL_LIMIT) {
                    return traversalLimit(
                        phase = "markEntryExit",
                        limit = MARK_TRAVERSAL_LIMIT,
                    )
                }
                count++
            }
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph: src/math/clip.js -> _stayOnPath.
    private fun stayOnPath(
        vertex: BooleanClipVertex,
        status: BooleanPathStatus?,
    ): Boolean {
        val data = vertex.data
        return if (
            data != null &&
            data.type != BooleanIntersectionType.BOUNCING
        ) {
            status == vertex.entryExit
        } else {
            true
        }
    }

    // JSXGraph: src/math/clip.js -> _addVertex.
    private fun addVertex(
        path: MutableList<DoubleArray>,
        vertex: BooleanClipVertex,
    ): Boolean {
        if (
            !vertex.coordinates[1].isNaN() &&
            !vertex.coordinates[2].isNaN()
        ) {
            path += vertex.coordinates.copyOf()
        }
        val data = vertex.data ?: return false
        if (data.done) {
            return true
        }
        data.done = true
        return false
    }

    // JSXGraph: src/math/clip.js -> tracing.
    private fun tracing(
        subject: MutableList<BooleanClipVertex>,
        subjectIntersections: List<BooleanClipVertex>,
        operation: ClipBooleanOperation,
    ): GMResult<ClipBooleanResult, ClipError> {
        var transitionCount = 0
        var subjectIndex = 0
        val path = mutableListOf<DoubleArray>()
        while (
            subjectIndex < subjectIntersections.size &&
            transitionCount < TRACE_TRAVERSAL_LIMIT
        ) {
            var current = subjectIntersections[subjectIndex]
            val initialData = current.data
                ?: return invalidTopology(
                    "tracing",
                    "subject intersection has no data",
                )
            if (
                initialData.done ||
                initialData.type != BooleanIntersectionType.CROSSING
            ) {
                subjectIndex++
                continue
            }
            if (path.isNotEmpty()) {
                path += separatorCoordinates()
            }

            val start = initialData.index
            var currentPath = subject
            var done = addVertex(path, current)
            var status = current.entryExit
            do {
                if (done) {
                    break
                }
                val goForward =
                    (
                        operation ==
                        ClipBooleanOperation.INTERSECTION &&
                            current.entryExit ==
                            BooleanPathStatus.ENTRY
                        ) ||
                        (
                            operation == ClipBooleanOperation.UNION &&
                                current.entryExit ==
                                BooleanPathStatus.EXIT
                            ) ||
                        (
                            operation ==
                            ClipBooleanOperation.DIFFERENCE &&
                                (
                                    (currentPath === subject) ==
                                    (
                                        current.entryExit ==
                                        BooleanPathStatus.EXIT
                                        )
                                    )
                            )
                var pathHopCount = 0
                do {
                    current =
                        if (goForward) current.next else current.previous
                    done = addVertex(path, current)
                    if (done) {
                        break
                    }
                    pathHopCount++
                    if (pathHopCount > TRACE_TRAVERSAL_LIMIT) {
                        return traversalLimit(
                            phase = "tracingPath",
                            limit = TRACE_TRAVERSAL_LIMIT,
                        )
                    }
                } while (stayOnPath(current, status))
                transitionCount++
                if (done) {
                    break
                }

                current = current.neighbour
                    ?: return invalidTopology(
                        "tracing",
                        "crossing intersection has no neighbour",
                    )
                val currentData = current.data
                    ?: return invalidTopology(
                        "tracing",
                        "neighbour intersection has no data",
                    )
                if (currentData.done) {
                    break
                }
                currentData.done = true
                status = current.entryExit
                currentPath = currentData.path
            } while (
                (
                    current.data
                        ?: return invalidTopology(
                            "tracing",
                            "current intersection has no data",
                        )
                    ).index != start &&
                transitionCount < TRACE_TRAVERSAL_LIMIT
            )

            if (transitionCount >= TRACE_TRAVERSAL_LIMIT) {
                return traversalLimit(
                    phase = "tracing",
                    limit = TRACE_TRAVERSAL_LIMIT,
                )
            }
            subjectIndex++
        }
        return GMResult.Ok(getCoordsArrays(path, doClose = false))
    }

    // JSXGraph: src/math/clip.js -> isEmptyCase.
    private fun isEmptyCase(
        subject: List<BooleanClipVertex>,
        clip: List<BooleanClipVertex>,
        operation: ClipBooleanOperation,
    ): Boolean =
        when (operation) {
            ClipBooleanOperation.INTERSECTION ->
                subject.isEmpty() || clip.isEmpty()
            ClipBooleanOperation.UNION ->
                subject.isEmpty() && clip.isEmpty()
            ClipBooleanOperation.DIFFERENCE -> subject.isEmpty()
        }

    // JSXGraph: src/math/clip.js -> _getCoordsArrays.
    private fun getCoordsArrays(
        path: List<DoubleArray>,
        doClose: Boolean,
    ): ClipBooleanResult {
        val coordinates =
            if (doClose && path.isNotEmpty()) {
                path + listOf(path.first())
            } else {
                path
            }
        return ClipBooleanResult(
            x = coordinates.map { it[1] }.toDoubleArray(),
            y = coordinates.map { it[2] }.toDoubleArray(),
        )
    }

    // JSXGraph: src/math/clip.js -> handleEmptyIntersection.
    private fun handleEmptyIntersection(
        subject: List<BooleanClipVertex>,
        clip: List<BooleanClipVertex>,
        operation: ClipBooleanOperation,
    ): GMResult<ClipBooleanResult, ClipError> {
        var doClose = false
        val path = mutableListOf<DoubleArray>()
        if (subject.isEmpty()) {
            if (operation == ClipBooleanOperation.UNION) {
                path.addAll(clip.map { it.coordinates })
            }
            return GMResult.Ok(getCoordsArrays(path, doClose = true))
        }
        if (clip.isEmpty()) {
            if (operation != ClipBooleanOperation.INTERSECTION) {
                path.addAll(subject.map { it.coordinates })
            }
            return GMResult.Ok(getCoordsArrays(path, doClose = true))
        }

        var subjectPoint = subject.first()
        while (subjectPoint.intersection) {
            subjectPoint = subjectPoint.next
            if (subjectPoint.end) {
                break
            }
        }
        var clipPoint = clip.first()
        while (clipPoint.intersection) {
            clipPoint = clipPoint.next
            if (clipPoint.end) {
                break
            }
        }
        val subjectCoordinates = subject.map { it.coordinates }
        val clipCoordinates = clip.map { it.coordinates }
        if (
            Geometry.windingNumber(
                subjectPoint.coordinates,
                clipCoordinates,
            ) == 0
        ) {
            if (
                Geometry.windingNumber(
                    clipPoint.coordinates,
                    subjectCoordinates,
                ) != 0
            ) {
                if (operation == ClipBooleanOperation.UNION) {
                    path.addAll(subjectCoordinates)
                    path += subjectCoordinates.first()
                } else if (
                    operation == ClipBooleanOperation.DIFFERENCE
                ) {
                    path.addAll(subjectCoordinates)
                    path += subjectCoordinates.first()
                    if (
                        Geometry.signedPolygon(subjectCoordinates) *
                        Geometry.signedPolygon(clipCoordinates) >
                        0.0
                    ) {
                        path.reverse()
                    }
                    path += separatorCoordinates()
                }
                if (
                    operation == ClipBooleanOperation.DIFFERENCE ||
                    operation == ClipBooleanOperation.INTERSECTION
                ) {
                    path.addAll(clipCoordinates)
                    path += clipCoordinates.first()
                    doClose = false
                }
            } else {
                if (operation == ClipBooleanOperation.DIFFERENCE) {
                    path.addAll(subjectCoordinates)
                    doClose = true
                } else if (operation == ClipBooleanOperation.UNION) {
                    path.addAll(subjectCoordinates)
                    path += subjectCoordinates.first()
                    path += separatorCoordinates()
                    path.addAll(clipCoordinates)
                    path += clipCoordinates.first()
                }
            }
        } else {
            if (operation == ClipBooleanOperation.INTERSECTION) {
                path.addAll(subjectCoordinates)
                doClose = true
            } else if (operation == ClipBooleanOperation.UNION) {
                path.addAll(clipCoordinates)
                path += clipCoordinates.first()
            }
        }
        return GMResult.Ok(getCoordsArrays(path, doClose))
    }

    // JSXGraph: src/math/clip.js -> _countCrossingIntersections.
    private fun countCrossingIntersections(
        intersections: List<BooleanClipVertex>,
    ): Int =
        intersections.count { vertex ->
            vertex.data?.type == BooleanIntersectionType.CROSSING
        }

    private fun BooleanPathStatus.toggle(): BooleanPathStatus =
        if (this == BooleanPathStatus.ENTRY) {
            BooleanPathStatus.EXIT
        } else {
            BooleanPathStatus.ENTRY
        }

    private fun removeClosingDuplicate(
        path: MutableList<BooleanClipVertex>,
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
    private fun isSeparator(vertex: BooleanClipVertex): Boolean =
        vertex.coordinates[1].isNaN() &&
            vertex.coordinates[2].isNaN()

    // JSXGraph: src/math/clip.js -> _noOverlap.
    private fun noOverlap(
        firstStart: DoubleArray,
        firstEnd: DoubleArray,
        secondStart: DoubleArray,
        secondEnd: DoubleArray,
    ): Boolean {
        for (index in 0 until 3) {
            val firstMinimum = min(firstStart[index], firstEnd[index])
            val firstMaximum = max(firstStart[index], firstEnd[index])
            val secondMinimum = min(secondStart[index], secondEnd[index])
            val secondMaximum = max(secondStart[index], secondEnd[index])
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
        first: DoubleArray,
        second: DoubleArray,
    ): Double {
        val segmentX = second[1] - first[1]
        val segmentY = second[2] - first[2]
        val pointX = point[1] - first[1]
        val pointY = point[2] - first[2]
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

    private fun separatorCoordinates(): DoubleArray =
        doubleArrayOf(1.0, Double.NaN, Double.NaN)

    private fun emptyResult(): ClipBooleanResult =
        ClipBooleanResult(DoubleArray(0), DoubleArray(0))

    private fun <T> invalidTopology(
        phase: String,
        detail: String,
    ): GMResult<T, ClipError> =
        GMResult.Err(ClipError.InvalidTopology(phase, detail))

    private fun <T> traversalLimit(
        phase: String,
        limit: Int,
    ): GMResult<T, ClipError> =
        GMResult.Err(ClipError.TraversalLimitExceeded(phase, limit))
}
