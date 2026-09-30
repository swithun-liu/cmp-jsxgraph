/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/bqdt.js -> BoxQuadtree.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

internal interface BoxQuadtreeItem {
    val xlb: Double
    val xub: Double
    val ylb: Double
    val yub: Double
}

internal data class BoxQuadtreeStats(
    val numberItems: Int,
    val depth: Int,
)

internal data class BoxQuadtreePlot(
    val dataX: DoubleArray,
    val dataY: DoubleArray,
)

internal class BoxQuadtree<T : BoxQuadtreeItem>(
    depth: Int,
    private val capacity: Int,
    boundingBox: DoubleArray? = null,
) {
    private val depth = depth - 1
    private val items = mutableListOf<T>()
    private var northWest: BoxQuadtree<T>? = null
    private var northEast: BoxQuadtree<T>? = null
    private var southEast: BoxQuadtree<T>? = null
    private var southWest: BoxQuadtree<T>? = null
    private var boundingBox: DoubleArray? = boundingBox?.copyOf()
    private var centerX: Double? = boundingBox?.let {
        (it[0] + it[2]) * 0.5
    }
    private var centerY: Double? = boundingBox?.let {
        (it[1] + it[3]) * 0.5
    }

    // JSXGraph 1.13.3: src/math/bqdt.js -> insert.
    internal fun insert(newItems: List<T>): BoxQuadtree<T> {
        val bounds = ensureBoundingBox(newItems)
        val centerHorizontal = centerX ?: Double.NaN
        val centerVertical = centerY ?: Double.NaN

        if (depth == 0 || items.size + newItems.size < capacity) {
            items += newItems
            return this
        }

        val northWestItems = mutableListOf<T>()
        val northEastItems = mutableListOf<T>()
        val southEastItems = mutableListOf<T>()
        val southWestItems = mutableListOf<T>()
        for (item in newItems) {
            val inNorthWest =
                item.xlb <= centerHorizontal && item.yub > centerVertical
            val inSouthWest =
                item.xlb <= centerHorizontal && item.ylb <= centerVertical
            val inNorthEast =
                item.xub > centerHorizontal && item.yub > centerVertical
            val inSouthEast =
                item.xub > centerHorizontal && item.ylb <= centerVertical

            if (
                inNorthWest &&
                inNorthEast &&
                inSouthEast &&
                inSouthWest
            ) {
                items += item
            } else {
                if (inNorthWest) northWestItems += item
                if (inSouthWest) southWestItems += item
                if (inNorthEast) northEastItems += item
                if (inSouthEast) southEastItems += item
            }
        }

        subdivide(
            northWestItems = northWestItems,
            southWestItems = southWestItems,
            northEastItems = northEastItems,
            southEastItems = southEastItems,
            left = bounds[0],
            top = bounds[1],
            right = bounds[2],
            bottom = bounds[3],
            centerHorizontal = centerHorizontal,
            centerVertical = centerVertical,
        )
        return this
    }

    // JSXGraph 1.13.3: src/math/bqdt.js -> insertItem.
    internal fun insertItem(item: T): BoxQuadtree<T> {
        val bounds = ensureBoundingBox(listOf(item))
        val centerHorizontal = centerX ?: Double.NaN
        val centerVertical = centerY ?: Double.NaN

        if (depth == 0 || items.size + 1 < capacity) {
            items += item
            return this
        }

        val northWestItems = mutableListOf<T>()
        val northEastItems = mutableListOf<T>()
        val southEastItems = mutableListOf<T>()
        val southWestItems = mutableListOf<T>()
        val inNorthWest =
            item.xlb <= centerHorizontal && item.yub > centerVertical
        val inSouthWest =
            item.xlb <= centerHorizontal && item.ylb <= centerVertical
        val inNorthEast =
            item.xub > centerHorizontal && item.yub > centerVertical
        val inSouthEast =
            item.xub > centerHorizontal && item.ylb <= centerVertical

        if (
            inNorthWest &&
            inNorthEast &&
            inSouthEast &&
            inSouthWest
        ) {
            items += item
        } else {
            if (inNorthWest) northWestItems += item
            if (inSouthWest) southWestItems += item
            if (inNorthEast) northEastItems += item
            if (inSouthEast) southEastItems += item
        }

        subdivide(
            northWestItems = northWestItems,
            southWestItems = southWestItems,
            northEastItems = northEastItems,
            southEastItems = southEastItems,
            left = bounds[0],
            top = bounds[1],
            right = bounds[2],
            bottom = bounds[3],
            centerHorizontal = centerHorizontal,
            centerVertical = centerVertical,
        )
        return this
    }

    // JSXGraph 1.13.3: src/math/bqdt.js -> find.
    internal fun find(box: DoubleArray): List<T> {
        val hits = mutableListOf<T>()
        for (item in items) {
            if (
                box[2] >= item.xlb &&
                box[0] <= item.xub &&
                box[3] <= item.yub &&
                box[1] >= item.ylb
            ) {
                hits += item
            }
        }

        val centerHorizontal = centerX ?: return hits
        val centerVertical = centerY ?: return hits
        if (box[0] <= centerHorizontal && box[1] >= centerVertical) {
            northWest?.let { hits += it.find(box) }
        }
        if (box[0] <= centerHorizontal && box[3] <= centerVertical) {
            southWest?.let { hits += it.find(box) }
        }
        if (box[2] >= centerHorizontal && box[1] >= centerVertical) {
            northEast?.let { hits += it.find(box) }
        }
        if (box[2] >= centerHorizontal && box[3] <= centerVertical) {
            southEast?.let { hits += it.find(box) }
        }
        return hits
    }

    // JSXGraph 1.13.3: src/math/bqdt.js -> analyzeTree.
    internal fun analyzeTree(): BoxQuadtreeStats {
        var numberItems = items.size
        var maximumDepth = 1
        for (child in listOf(northWest, southWest, northEast, southEast)) {
            val stats = child?.analyzeTree() ?: continue
            numberItems += stats.numberItems
            maximumDepth = maxOf(maximumDepth, 1 + stats.depth)
        }
        return BoxQuadtreeStats(
            numberItems = numberItems,
            depth = maximumDepth,
        )
    }

    // JSXGraph 1.13.3: src/math/bqdt.js -> plot.
    internal fun plot(): BoxQuadtreePlot {
        val bounds = boundingBox ?: return BoxQuadtreePlot(
            dataX = doubleArrayOf(),
            dataY = doubleArrayOf(),
        )
        val dataX = mutableListOf(
            bounds[0],
            bounds[2],
            bounds[2],
            bounds[0],
            bounds[0],
            Double.NaN,
        )
        val dataY = mutableListOf(
            bounds[3],
            bounds[3],
            bounds[1],
            bounds[1],
            bounds[3],
            Double.NaN,
        )

        for (child in listOf(northWest, northEast, southEast, southWest)) {
            val childPlot = child?.plot() ?: continue
            dataX += childPlot.dataX.toList()
            dataY += childPlot.dataY.toList()
        }
        return BoxQuadtreePlot(
            dataX = dataX.toDoubleArray(),
            dataY = dataY.toDoubleArray(),
        )
    }

    // JSXGraph 1.13.3: src/math/bqdt.js -> subdivide.
    private fun subdivide(
        northWestItems: List<T>,
        southWestItems: List<T>,
        northEastItems: List<T>,
        southEastItems: List<T>,
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
        centerHorizontal: Double,
        centerVertical: Double,
    ) {
        if (northWestItems.isNotEmpty()) {
            var child = northWest
            if (child == null) {
                child = BoxQuadtree(
                    depth = depth,
                    capacity = capacity,
                    boundingBox = doubleArrayOf(
                        left,
                        top,
                        centerHorizontal,
                        centerVertical,
                    ),
                )
                northWest = child
            }
            child.insert(northWestItems)
        }
        if (southWestItems.isNotEmpty()) {
            var child = southWest
            if (child == null) {
                child = BoxQuadtree(
                    depth = depth,
                    capacity = capacity,
                    boundingBox = doubleArrayOf(
                        left,
                        centerVertical,
                        centerHorizontal,
                        bottom,
                    ),
                )
                southWest = child
            }
            child.insert(southWestItems)
        }
        if (northEastItems.isNotEmpty()) {
            var child = northEast
            if (child == null) {
                child = BoxQuadtree(
                    depth = depth,
                    capacity = capacity,
                    boundingBox = doubleArrayOf(
                        centerHorizontal,
                        top,
                        right,
                        centerVertical,
                    ),
                )
                northEast = child
            }
            child.insert(northEastItems)
        }
        if (southEastItems.isNotEmpty()) {
            var child = southEast
            if (child == null) {
                child = BoxQuadtree(
                    depth = depth,
                    capacity = capacity,
                    boundingBox = doubleArrayOf(
                        centerHorizontal,
                        centerVertical,
                        right,
                        bottom,
                    ),
                )
                southEast = child
            }
            child.insert(southEastItems)
        }
    }

    private fun ensureBoundingBox(newItems: List<T>): DoubleArray {
        boundingBox?.let { return it }

        var left = Double.POSITIVE_INFINITY
        var top = Double.NEGATIVE_INFINITY
        var right = Double.NEGATIVE_INFINITY
        var bottom = Double.POSITIVE_INFINITY
        for (item in newItems) {
            left = minOf(left, item.xlb)
            top = maxOf(top, item.yub)
            right = maxOf(right, item.xub)
            bottom = minOf(bottom, item.ylb)
        }
        val bounds = doubleArrayOf(left, top, right, bottom)
        boundingBox = bounds
        centerX = (left + right) * 0.5
        centerY = (top + bottom) * 0.5
        return bounds
    }
}
