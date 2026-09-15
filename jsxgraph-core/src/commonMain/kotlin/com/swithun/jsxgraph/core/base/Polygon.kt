/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/polygon.js -> Polygon, Area, Perimeter, boundingBox,
 * createPolygon
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
}

/**
 * Initial translated slice of JXG.Polygon.
 *
 * The polygon keeps the upstream closed vertex list and creates Segment
 * children when withLines is enabled. Transformations, mutable vertex lists,
 * polygon clipping, labels, and hit testing remain untranslated.
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
    override fun update(fromParent: Boolean): Polygon = this

    override fun updateRenderer(): Polygon {
        needsUpdate = false
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

    internal companion object {
        private const val POLYGON_ID_PREFIX = "Py"
        private const val POLYGON_ELEMENT_TYPE = "polygon"

        // JSXGraph: src/base/polygon.js -> createPolygon / Polygon constructor
        internal fun create(
            board: Board,
            vertices: List<Point>,
            ownedVertices: Set<Point> = emptySet(),
            withLines: Boolean = true,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
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
                for (borderIndex in 0 until borderCount) {
                    val firstIndex = (borderIndex + 1) % borderCount
                    val border = when (
                        val result = Line.create(
                            board = board,
                            point1 = polygon.vertices[firstIndex],
                            point2 = polygon.vertices[firstIndex + 1],
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> {
                            board.removeObjects(polygon.borders)
                            return GMResult.Err(
                                PolygonError.BorderCreation(
                                    borderIndex = borderIndex,
                                    error = result.error,
                                ),
                            )
                        }
                    }
                    polygon.borders += border
                }
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
