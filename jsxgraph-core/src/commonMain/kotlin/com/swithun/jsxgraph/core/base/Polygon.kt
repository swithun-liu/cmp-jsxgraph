/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/polygon.js -> Polygon, Area, Perimeter, boundingBox,
 * createPolygon, createPolygonalChain
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import kotlin.math.abs

internal sealed interface PolygonError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : PolygonError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : PolygonError

    data class Registration(
        val error: BoardError,
    ) : PolygonError

    data class BorderCreation(
        val borderIndex: Int,
        val error: LineError,
    ) : PolygonError

    data class InvalidInsertionIndex(
        val index: Int,
        val minimum: Int,
        val maximum: Int,
    ) : PolygonError
}

internal data class PolygonTraceSnapshot(
    val vertices: List<DoubleArray>,
)

/**
 * Translated JXG.Polygon lifecycle.
 *
 * The polygon keeps the upstream closed vertex list and creates Segment
 * children when withLines is enabled. It supports transforms, mutable vertex
 * insertion/removal, nested vertex/border styles, labels, traces, movement,
 * and fill/border hit testing.
 */
internal class Polygon private constructor(
    board: Board,
    vertices: List<Point>,
    internal val ownedVertices: Set<Point>,
    internal val withLines: Boolean,
    id: String = "",
    name: String? = null,
    needsRegularUpdate: Boolean = true,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_POLYGON,
    elementClass = Const.OBJECT_CLASS_AREA,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal val vertices = vertices.toMutableList()
    internal val borders = mutableListOf<Line>()
    internal var implicitVertices: List<Point> = ownedVertices.toList()
    // JSXGraph 1.13.3: src/options.js -> Polygon.hasInnerPoints.
    internal var hasInnerPoints: Boolean = false
    internal val subs = linkedMapOf<String, GeometryElement>()
    internal override val inherits = mutableListOf<GeometryElement>()
    internal val traceSnapshots = mutableListOf<PolygonTraceSnapshot>()
    // JSXGraph: src/base/polygon.js -> createParallelogram.parallelPoint
    internal var parallelPoint: ParallelPoint? = null
    // JSXGraph: src/element/slopetriangle.js -> createSlopeTriangle.
    internal var slopeTriangleDefinition: SlopeTriangleDefinition? = null

    init {
        if (
            this.vertices.isNotEmpty() &&
            this.vertices.last().id != this.vertices.first().id
        ) {
            this.vertices += this.vertices.first()
        }
        elType = POLYGON_ELEMENT_TYPE
        isDraggable = true
    }

    // JSXGraph: src/base/polygon.js -> update
    override fun update(fromParent: Boolean): Polygon {
        slopeTriangleDefinition?.updateLabel()
        if (getAttribute("trace") == true) {
            cloneToBackground()
        }
        return this
    }

    override fun updateRenderer(): Polygon {
        needsUpdate = false
        return this
    }

    // JSXGraph 1.13.3: src/base/polygon.js -> remove;
    // src/element/slopetriangle.js -> removeSlopeTriangle.
    override fun remove(): GeometryElement {
        slopeTriangleDefinition?.remove()
        board.removeObjects(borders.toList())
        borders.clear()
        return this
    }

    // JSXGraph 1.13.3: src/base/polygon.js -> pnpoly.
    internal fun pnpoly(
        horizontal: Double,
        vertical: Double,
        coordinateType: Int = Const.COORDS_BY_SCREEN,
    ): Boolean {
        val coordinates =
            if (coordinateType == Const.COORDS_BY_USER) {
                doubleArrayOf(horizontal, vertical)
            } else {
                Coords(
                    method = Const.COORDS_BY_SCREEN,
                    coordinates = doubleArrayOf(horizontal, vertical),
                    board = board,
                    emitter = false,
                ).usrCoords.copyOfRange(1, 3)
            }
        return Geometry.pnpoly(
            screenX = coordinates[0],
            screenY = coordinates[1],
            closedPath = vertices.map { it.coords.usrCoords },
        )
    }

    // JSXGraph 1.13.3: src/base/polygon.js -> hasPoint.
    internal fun hasPoint(
        screenX: Double,
        screenY: Double,
        precision: Double = DEFAULT_HIT_PRECISION,
    ): Boolean {
        if (hasInnerPoints && pnpoly(screenX, screenY)) {
            return true
        }
        return borders.any {
            it.hasPoint(screenX, screenY, precision)
        }
    }

    // JSXGraph 1.13.3: src/base/polygon.js -> getTextAnchor /
    // getLabelAnchor.
    internal override fun getTextAnchor(): Coords {
        if (vertices.isEmpty()) {
            return Coords(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(1.0, 0.0, 0.0),
                board = board,
            )
        }
        var minimumX = vertices[0].X()
        var maximumX = minimumX
        var maximumY = vertices[0].Y()
        var minimumY = maximumY
        for (vertex in vertices) {
            minimumX = minOf(minimumX, vertex.X())
            maximumX = maxOf(maximumX, vertex.X())
            maximumY = maxOf(maximumY, vertex.Y())
            minimumY = minOf(minimumY, vertex.Y())
        }
        return Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(
                (minimumX + maximumX) * 0.5,
                (maximumY + minimumY) * 0.5,
            ),
            board = board,
        )
    }

    internal override fun getLabelAnchor(): Coords = getTextAnchor()

    // JSXGraph 1.13.3: src/base/polygon.js -> cloneToBackground.
    internal override fun cloneToBackground(): Polygon {
        traceSnapshots += PolygonTraceSnapshot(
            vertices = vertices.map {
                it.coords.usrCoords.copyOf()
            },
        )
        return this
    }

    // JSXGraph 1.13.3: src/base/polygon.js -> hideElement.
    internal fun hideElement(borderless: Boolean = false): Polygon {
        setAttribute(mapOf("visible" to false))
        if (!borderless) {
            for (border in borders) {
                border.setAttribute(mapOf("visible" to false))
            }
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/polygon.js -> showElement.
    internal fun showElement(borderless: Boolean = false): Polygon {
        setAttribute(mapOf("visible" to true))
        if (!borderless) {
            for (border in borders) {
                border.setAttribute(mapOf("visible" to true))
            }
        }
        return this
    }

    // JSXGraph: src/base/polygon.js -> Area
    @Suppress("FunctionName")
    internal fun Area(): Double =
        abs(
            Geometry.signedPolygon(
                points = vertices.map { it.coords.usrCoords },
                sort = true,
            ),
        )

    // JSXGraph: src/base/polygon.js -> Perimeter / L
    @Suppress("FunctionName")
    internal fun Perimeter(): Double {
        var perimeter = 0.0
        for (index in 1 until vertices.size) {
            perimeter += vertices[index].Dist(vertices[index - 1])
        }
        return perimeter
    }

    @Suppress("FunctionName")
    internal fun L(): Double = Perimeter()

    // JSXGraph: src/base/polygon.js -> boundingBox / bounds
    internal fun bounds(): DoubleArray {
        val vertexCount = vertices.size - 1
        if (vertexCount <= 0) {
            return doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        }

        var minimumX = vertices[0].X()
        var maximumX = minimumX
        var maximumY = vertices[0].Y()
        var minimumY = maximumY
        for (index in 1 until vertexCount) {
            val x = vertices[index].X()
            val y = vertices[index].Y()
            minimumX = minOf(minimumX, x)
            maximumX = maxOf(maximumX, x)
            maximumY = maxOf(maximumY, y)
            minimumY = minOf(minimumY, y)
        }
        return doubleArrayOf(
            minimumX,
            maximumY,
            maximumX,
            minimumY,
        )
    }

    internal fun boundingBox(): DoubleArray = bounds()

    // JSXGraph 1.13.3: src/base/polygon.js -> findPoint.
    internal fun findPoint(point: Point): Int =
        vertices.indexOfFirst { it.id == point.id }

    // JSXGraph 1.13.3: src/base/polygon.js -> addPoints.
    internal fun addPoints(
        points: List<Point>,
    ): GMResult<Polygon, PolygonError> {
        val insertionIndex =
            if (elType == POLYGONAL_CHAIN_ELEMENT_TYPE) {
                vertices.lastIndex
            } else {
                vertices.size - 2
            }
        return insertPoints(insertionIndex, points)
    }

    // JSXGraph 1.13.3: src/base/polygon.js -> insertPoints.
    internal fun insertPoints(
        index: Int,
        points: List<Point>,
    ): GMResult<Polygon, PolygonError> {
        if (points.isEmpty()) {
            return GMResult.Ok(this)
        }
        val last =
            if (elType == POLYGON_ELEMENT_TYPE) {
                vertices.size - 2
            } else {
                vertices.lastIndex
            }
        if (index < -1 || index > last) {
            return GMResult.Err(
                PolygonError.InvalidInsertionIndex(
                    index = index,
                    minimum = -1,
                    maximum = last,
                ),
            )
        }
        for ((offset, point) in points.withIndex()) {
            validateParent(board, point, index + offset + 1)?.let {
                return GMResult.Err(it)
            }
        }

        val openVertices = openVertices()
        openVertices.addAll(index + 1, points)
        replaceVertices(openVertices)
        return when (val result = rebuildBorders()) {
            is GMResult.Ok -> {
                board.update()
                GMResult.Ok(this)
            }
            is GMResult.Err -> result
        }
    }

    // JSXGraph 1.13.3: src/base/polygon.js -> removePoints.
    internal fun removePoints(
        indices: Iterable<Int>,
    ): GMResult<Polygon, PolygonError> {
        val openVertices = openVertices()
        val validIndices = indices
            .filter { it in openVertices.indices }
            .distinct()
            .sortedDescending()
        if (validIndices.isEmpty()) {
            return GMResult.Ok(this)
        }
        for (index in validIndices) {
            val removed = openVertices.removeAt(index)
            removed.removeChild(this)
        }
        replaceVertices(openVertices)
        return when (val result = rebuildBorders()) {
            is GMResult.Ok -> {
                board.update()
                GMResult.Ok(this)
            }
            is GMResult.Err -> result
        }
    }

    internal fun removePoints(
        points: List<Point>,
    ): GMResult<Polygon, PolygonError> =
        removePoints(points.map(::findPoint))

    // JSXGraph 1.13.3: src/base/polygon.js -> getParents.
    internal fun parentData(): List<String> =
        openVertices().map(GeometryElement::id)

    internal override fun dumpParents():
        GMResult<List<Any?>, GeometryElementDumpError> =
        GMResult.Ok(parentData())

    internal override fun dumpSubElements():
        Map<String, GeometryElement> = subs

    // JSXGraph 1.13.3: src/base/polygon.js -> getAttributes.
    override fun getAttributes(): Map<String, Any?> {
        val attributes = super.getAttributes().toMutableMap()
        if (withLines) {
            val lines = (
                attributes["lines"] as? Map<*, *>
                )?.entries?.associateTo(linkedMapOf()) {
                it.key.toString() to it.value
            } ?: linkedMapOf()
            attributes["lines"] = lines.apply {
                this["ids"] = borders.map { it.id }
                this["colors"] = borders.map {
                    it.getAttribute("strokecolor")
                }
            }
        }
        return attributes
    }

    // JSXGraph 1.13.3: src/base/polygon.js -> snapToGrid.
    internal override fun snapToGrid(): Polygon {
        for (vertex in openVertices()) {
            vertex.handleSnapToGrid()
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/polygon.js -> setPositionDirectly.
    internal fun setPositionDirectly(
        method: Int,
        coordinates: DoubleArray,
        oldCoordinates: DoubleArray,
    ): Polygon {
        val movableVertices = openVertices()
        if (movableVertices.any { !it.isDraggable }) {
            return this
        }
        val current = Coords(
            method = method,
            coordinates = coordinates,
            board = board,
            emitter = false,
        ).usrCoords
        val previous = Coords(
            method = method,
            coordinates = oldCoordinates,
            board = board,
            emitter = false,
        ).usrCoords
        val deltaX = current[1] - previous[1]
        val deltaY = current[2] - previous[2]
        for (vertex in movableVertices) {
            vertex.setPositionDirectly(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(
                    vertex.X() + deltaX,
                    vertex.Y() + deltaY,
                ),
            )
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/polygon.js -> sutherlandHodgman.
    internal fun sutherlandHodgman(
        polygon: Polygon,
    ): List<DoubleArray> {
        val clip = Geometry.sortVertices(
            vertices.map { it.coords.usrCoords },
        )
        val subject = Geometry.sortVertices(
            polygon.vertices.map { it.coords.usrCoords },
        )
        val clipCount = (clip.size - 1).coerceAtLeast(0)
        val subjectCount = (subject.size - 1).coerceAtLeast(0)
        var output = subject.take(subjectCount).map(DoubleArray::copyOf)
        if (clipCount == 0 || output.isEmpty()) {
            return emptyList()
        }

        // JSXGraph 1.13.3: src/base/polygon.js ->
        // sutherlandHodgman closure isInside.
        fun isInside(
            first: DoubleArray,
            second: DoubleArray,
            point: DoubleArray,
        ): Boolean =
            (second[1] - first[1]) * (point[2] - first[2]) -
                (second[2] - first[2]) * (point[1] - first[1]) >= 0.0

        for (clipIndex in 0 until clipCount) {
            val input = output
            output = emptyList()
            if (input.isEmpty()) {
                break
            }
            val nextOutput = mutableListOf<DoubleArray>()
            var start = input.last()
            for (end in input) {
                val endInside = isInside(
                    clip[clipIndex],
                    clip[clipIndex + 1],
                    end,
                )
                val startInside = isInside(
                    clip[clipIndex],
                    clip[clipIndex + 1],
                    start,
                )
                if (endInside) {
                    if (!startInside) {
                        nextOutput += normalizedIntersection(
                            start,
                            end,
                            clip[clipIndex],
                            clip[clipIndex + 1],
                        )
                    }
                    nextOutput += end
                } else if (startInside) {
                    nextOutput += normalizedIntersection(
                        start,
                        end,
                        clip[clipIndex],
                        clip[clipIndex + 1],
                    )
                }
                start = end
            }
            output = nextOutput
        }
        return output
    }

    // JSXGraph 1.13.3: src/base/polygon.js -> intersect.
    internal fun intersect(polygon: Polygon): List<DoubleArray> =
        sutherlandHodgman(polygon)

    private fun normalizedIntersection(
        firstStart: DoubleArray,
        firstEnd: DoubleArray,
        secondStart: DoubleArray,
        secondEnd: DoubleArray,
    ): DoubleArray {
        val point = Geometry.meetSegmentSegment(
            firstStart = firstStart,
            firstEnd = firstEnd,
            secondStart = secondStart,
            secondEnd = secondEnd,
        ).point.copyOf()
        point[1] /= point[0]
        point[2] /= point[0]
        point[0] = 1.0
        return point
    }

    private fun openVertices(): MutableList<Point> =
        if (
            elType == POLYGON_ELEMENT_TYPE &&
            vertices.size > 1 &&
            vertices.first().id == vertices.last().id
        ) {
            vertices.dropLast(1).toMutableList()
        } else {
            vertices.toMutableList()
        }

    private fun replaceVertices(openVertices: List<Point>) {
        vertices.clear()
        vertices += openVertices
        if (
            elType == POLYGON_ELEMENT_TYPE &&
            vertices.isNotEmpty()
        ) {
            vertices += vertices.first()
        }
        for (vertex in openVertices) {
            if (vertex !in ownedVertices) {
                vertex.addChild(this)
            }
        }
        setParents(openVertices)
    }

    private fun rebuildBorders(): GMResult<Unit, PolygonError> {
        if (!withLines) {
            borders.clear()
            return GMResult.Ok(Unit)
        }
        board.removeObjects(borders.toList())
        borders.clear()
        val edgeCount = (vertices.size - 1).coerceAtLeast(0)
        for (index in 0 until edgeCount) {
            val border = when (
                val result = Line.createSegment(
                    board = board,
                    point1 = vertices[index],
                    point2 = vertices[index + 1],
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    PolygonError.BorderCreation(index, result.error),
                )
            }
            borders += border
            addChild(border)
        }
        inherits.clear()
        inherits += openVertices()
        inherits += borders
        return GMResult.Ok(Unit)
    }

    internal companion object {
        private const val POLYGON_ID_PREFIX = "Py"
        private const val POLYGON_ELEMENT_TYPE = "polygon"
        private const val POLYGONAL_CHAIN_ELEMENT_TYPE = "polygonalchain"
        private const val DEFAULT_HIT_PRECISION = 4.0

        // JSXGraph: src/base/polygon.js -> createPolygon / Polygon constructor
        internal fun create(
            board: Board,
            vertices: List<Point>,
            ownedVertices: Set<Point> = emptySet(),
            withLines: Boolean = true,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            borderIds: List<String> = emptyList(),
            borderNames: List<String> = emptyList(),
        ): GMResult<Polygon, PolygonError> {
            for ((index, vertex) in vertices.withIndex()) {
                validateParent(board, vertex, index)?.let {
                    return GMResult.Err(it)
                }
            }
            if (id.isNotEmpty() && board.elementById(id) != null) {
                return GMResult.Err(
                    PolygonError.Registration(
                        BoardError.DuplicateElementId(id),
                    ),
                )
            }

            val polygon = Polygon(
                board = board,
                vertices = vertices,
                ownedVertices = ownedVertices,
                withLines = withLines,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            if (withLines) {
                val borderCount = polygon.vertices.size - 1
                val createdBorders = mutableListOf<Line>()
                val orderedBorders =
                    MutableList<Line?>(borderCount.coerceAtLeast(0)) { null }
                for (borderIndex in 0 until borderCount) {
                    val firstIndex = (borderIndex + 1) % borderCount
                    val border = when (
                        val result = Line.createSegment(
                            board = board,
                            point1 = polygon.vertices[firstIndex],
                            point2 = polygon.vertices[firstIndex + 1],
                            id = borderIds.getOrNull(firstIndex).orEmpty(),
                            name = borderNames.getOrNull(firstIndex),
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> {
                            board.removeObjects(createdBorders)
                            return GMResult.Err(
                                PolygonError.BorderCreation(
                                    borderIndex = borderIndex,
                                    error = result.error,
                                ),
                            )
                        }
                    }
                    createdBorders += border
                    orderedBorders[firstIndex] = border
                }
                polygon.borders += orderedBorders.filterNotNull()
            }

            when (val registration = board.setId(polygon, POLYGON_ID_PREFIX)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> {
                    board.removeObjects(polygon.borders)
                    return GMResult.Err(
                        PolygonError.Registration(registration.error),
                    )
                }
            }
            for (border in polygon.borders) {
                polygon.addChild(border)
            }
            for (vertex in polygon.vertices.dropLast(1)) {
                if (vertex in ownedVertices) {
                    polygon.addChild(vertex)
                } else {
                    vertex.addChild(polygon)
                }
            }
            return GMResult.Ok(polygon)
        }

        // JSXGraph: src/base/polygon.js -> createPolygonalChain
        internal fun createPolygonalChain(
            board: Board,
            vertices: List<Point>,
            ownedVertices: Set<Point> = emptySet(),
            withLines: Boolean = true,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Polygon, PolygonError> =
            when (
                val result = create(
                    board = board,
                    vertices = vertices,
                    ownedVertices = ownedVertices,
                    withLines = withLines,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Err -> result
                is GMResult.Ok -> {
                    val chain = result.value
                    chain.elType = POLYGONAL_CHAIN_ELEMENT_TYPE

                    // Array.pop() is a no-op for empty arrays in the upstream
                    // wrapper, as is removing an absent closing border.
                    if (chain.vertices.isNotEmpty()) {
                        chain.vertices.removeAt(chain.vertices.lastIndex)
                    }
                    chain.borders.lastOrNull()?.let { closingBorder ->
                        board.removeObject(closingBorder)
                        chain.borders.removeAt(chain.borders.lastIndex)
                    }
                    GMResult.Ok(chain)
                }
            }

        private fun validateParent(
            board: Board,
            point: Point,
            parentIndex: Int,
        ): PolygonError? {
            if (point.board !== board) {
                return PolygonError.ParentBoardMismatch(parentIndex)
            }
            if (board.elementById(point.id) !== point) {
                return PolygonError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = point.id,
                )
            }
            return null
        }
    }
}
