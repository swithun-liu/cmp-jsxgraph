/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/qdt.js -> Quadtree.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult

internal interface QuadtreePoint {
    val quadtreeX: Double
    val quadtreeY: Double
}

internal sealed interface QuadtreeError {
    data class InvalidBoundingBox(
        val boundingBox: DoubleArray,
    ) : QuadtreeError

    data class InvalidCapacity(
        val capacity: Int,
    ) : QuadtreeError

    data class InvalidMaximumDepth(
        val maximumDepth: Int,
    ) : QuadtreeError

    data class MaximumDepthExceeded(
        val maximumDepth: Int,
        val x: Double,
        val y: Double,
    ) : QuadtreeError
}

/**
 * Bounded translation of `JXG.Math.Quadtree`.
 *
 * Existing points stay in their current node when it is subdivided, matching
 * JSXGraph 1.13.3. The depth guard is the Kotlin safety boundary for
 * coincident-point inputs that can otherwise subdivide without a bound.
 */
internal class Quadtree<T : QuadtreePoint> private constructor(
    boundingBox: DoubleArray,
    internal val capacity: Int,
    private val maximumDepth: Int,
    private val depth: Int,
    internal val parent: Quadtree<T>?,
) {
    internal val points = mutableListOf<T>()

    private val xlb = boundingBox[0]
    private val yub = boundingBox[1]
    private val xub = boundingBox[2]
    private val ylb = boundingBox[3]

    private var northWest: Quadtree<T>? = null
    private var northEast: Quadtree<T>? = null
    private var southEast: Quadtree<T>? = null
    private var southWest: Quadtree<T>? = null

    // JSXGraph 1.13.3: src/math/qdt.js -> contains.
    internal fun contains(
        x: Double,
        y: Double,
    ): Boolean = xlb < x && x <= xub && ylb < y && y <= yub

    // JSXGraph 1.13.3: src/math/qdt.js -> insert.
    internal fun insert(point: T): GMResult<Boolean, QuadtreeError> {
        val x = point.quadtreeX
        val y = point.quadtreeY
        if (!contains(x, y)) {
            return GMResult.Ok(false)
        }
        if (points.size < capacity && northWest == null) {
            points += point
            return GMResult.Ok(true)
        }
        if (northWest == null) {
            if (depth >= maximumDepth) {
                return GMResult.Err(
                    QuadtreeError.MaximumDepthExceeded(
                        maximumDepth = maximumDepth,
                        x = x,
                        y = y,
                    ),
                )
            }
            subdivide()
        }

        for (
            child in listOfNotNull(
                northWest,
                northEast,
                southEast,
                southWest,
            )
        ) {
            when (val result = child.insert(point)) {
                is GMResult.Ok -> if (result.value) return result
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(false)
    }

    // JSXGraph 1.13.3: src/math/qdt.js -> _query / query.
    internal fun query(
        x: Double,
        y: Double,
    ): Quadtree<T>? {
        if (!contains(x, y)) {
            return null
        }
        if (northWest == null) {
            return this
        }
        return northWest?.query(x, y)
            ?: northEast?.query(x, y)
            ?: southEast?.query(x, y)
            ?: southWest?.query(x, y)
    }

    // JSXGraph 1.13.3: src/math/qdt.js -> hasPoint.
    internal fun hasPoint(
        x: Double,
        y: Double,
        tolerance: Double,
    ): Boolean {
        if (!contains(x, y)) {
            return false
        }
        for (point in points) {
            if (
                Mat.hypot(
                    x - point.quadtreeX,
                    y - point.quadtreeY,
                ) < tolerance
            ) {
                return true
            }
        }
        return northWest?.hasPoint(x, y, tolerance) == true ||
            northEast?.hasPoint(x, y, tolerance) == true ||
            southEast?.hasPoint(x, y, tolerance) == true ||
            southWest?.hasPoint(x, y, tolerance) == true
    }

    // JSXGraph 1.13.3: src/math/qdt.js -> getAllPoints.
    internal fun getAllPoints(): List<T> = buildList {
        getAllPointsRecursive(this)
    }

    // JSXGraph 1.13.3: src/math/qdt.js -> getAllPointsRecursive.
    private fun getAllPointsRecursive(destination: MutableList<T>) {
        destination += points
        northWest?.getAllPointsRecursive(destination)
        northEast?.getAllPointsRecursive(destination)
        southEast?.getAllPointsRecursive(destination)
        southWest?.getAllPointsRecursive(destination)
    }

    // JSXGraph 1.13.3: src/math/qdt.js -> subdivide.
    private fun subdivide() {
        val centerX = xlb + (xub - xlb) * 0.5
        val centerY = ylb + (yub - ylb) * 0.5
        northWest = child(
            doubleArrayOf(xlb, yub, centerX, centerY),
        )
        northEast = child(
            doubleArrayOf(centerX, yub, xub, centerY),
        )
        southEast = child(
            doubleArrayOf(xlb, centerY, centerX, ylb),
        )
        southWest = child(
            doubleArrayOf(centerX, centerY, xub, ylb),
        )
    }

    private fun child(boundingBox: DoubleArray): Quadtree<T> =
        Quadtree(
            boundingBox = boundingBox,
            capacity = capacity,
            maximumDepth = maximumDepth,
            depth = depth + 1,
            parent = this,
        )

    companion object {
        internal const val DEFAULT_CAPACITY = 10
        internal const val DEFAULT_MAXIMUM_DEPTH = 64

        internal fun <T : QuadtreePoint> create(
            boundingBox: DoubleArray,
            capacity: Int = DEFAULT_CAPACITY,
            maximumDepth: Int = DEFAULT_MAXIMUM_DEPTH,
        ): GMResult<Quadtree<T>, QuadtreeError> {
            if (
                boundingBox.size != 4 ||
                boundingBox.any { !it.isFinite() } ||
                boundingBox[0] >= boundingBox[2] ||
                boundingBox[3] >= boundingBox[1]
            ) {
                return GMResult.Err(
                    QuadtreeError.InvalidBoundingBox(
                        boundingBox.copyOf(),
                    ),
                )
            }
            if (capacity <= 0) {
                return GMResult.Err(
                    QuadtreeError.InvalidCapacity(capacity),
                )
            }
            if (maximumDepth <= 0) {
                return GMResult.Err(
                    QuadtreeError.InvalidMaximumDepth(maximumDepth),
                )
            }
            return GMResult.Ok(
                Quadtree(
                    boundingBox = boundingBox.copyOf(),
                    capacity = capacity,
                    maximumDepth = maximumDepth,
                    depth = 0,
                    parent = null,
                ),
            )
        }
    }
}
