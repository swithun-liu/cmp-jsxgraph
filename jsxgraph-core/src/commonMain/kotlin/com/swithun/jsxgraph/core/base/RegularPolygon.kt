/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/polygon.js -> createRegularPolygon
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.math.ceil

internal sealed interface RegularPolygonError {
    data class InvalidNumericParentForm(
        val actualParentCount: Int,
        val vertexCount: Double,
    ) : RegularPolygonError

    data class InvalidVertexCount(
        val vertexCount: Double,
    ) : RegularPolygonError

    data class VertexCountTooLarge(
        val vertexCount: Double,
    ) : RegularPolygonError

    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : RegularPolygonError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : RegularPolygonError

    data class TransformationCreation(
        val vertexIndex: Int,
        val error: TransformationError,
    ) : RegularPolygonError

    data class PointCreation(
        val vertexIndex: Int,
        val error: PointError,
    ) : RegularPolygonError

    data class PolygonCreation(
        val error: PolygonError,
    ) : RegularPolygonError
}

internal object RegularPolygon {
    private const val REGULAR_POLYGON_ELEMENT_TYPE = "regularpolygon"

    // JSXGraph: src/base/polygon.js -> createRegularPolygon,
    // two Points plus numeric n branch.
    internal fun create(
        board: Board,
        point1: Point,
        point2: Point,
        numberOfVertices: Double,
        ownedPoints: Set<Point> = emptySet(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        withLines: Boolean = true,
        vertexIds: List<String> = emptyList(),
        vertexId: String = "",
        vertexName: String? = null,
        vertexNeedsRegularUpdate: Boolean = true,
    ): GMResult<Polygon, RegularPolygonError> {
        if (!numberOfVertices.isFinite() || numberOfVertices < 3.0) {
            return GMResult.Err(
                RegularPolygonError.InvalidVertexCount(numberOfVertices),
            )
        }
        if (numberOfVertices > Int.MAX_VALUE.toDouble()) {
            return GMResult.Err(
                RegularPolygonError.VertexCountTooLarge(numberOfVertices),
            )
        }
        validateParent(board, point1, 0)?.let {
            return GMResult.Err(it)
        }
        validateParent(board, point2, 1)?.let {
            return GMResult.Err(it)
        }

        val vertexCount = ceil(numberOfVertices).toInt()
        val generatedCount = vertexCount - 2
        val completeIdList = vertexIds.size >= generatedCount
        val vertices = mutableListOf(point1, point2)
        val generatedPoints = mutableListOf<Point>()
        val angle = PI * (2.0 - (numberOfVertices - 2.0) / numberOfVertices)

        for (index in 2 until vertexCount) {
            val rotation = when (
                val result = Transformation.createRotation(
                    board = board,
                    angle = TransformationParameter.Numeric(angle),
                    center = vertices[index - 1],
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    board.removeObjects(generatedPoints)
                    return GMResult.Err(
                        RegularPolygonError.TransformationCreation(
                            vertexIndex = index,
                            error = result.error,
                        ),
                    )
                }
            }
            val helperId =
                if (completeIdList) {
                    vertexIds[index - 2]
                } else {
                    vertexId
                }
            val helper = when (
                val result = Point.create(
                    board = board,
                    basePoint = vertices[index - 2],
                    transformations = listOf(rotation),
                    id = helperId,
                    name = vertexName,
                    needsRegularUpdate = vertexNeedsRegularUpdate,
                    fixed = false,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    board.removeObjects(generatedPoints)
                    return GMResult.Err(
                        RegularPolygonError.PointCreation(
                            vertexIndex = index,
                            error = result.error,
                        ),
                    )
                }
            }
            helper.type = Const.OBJECT_TYPE_CAS
            helper.isDraggable = true
            helper.isFixed = false
            helper.fullUpdate()
            generatedPoints += helper
            vertices += helper
        }

        return finish(
            board = board,
            vertices = vertices,
            ownedPoints = ownedPoints,
            generatedPoints = generatedPoints,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            withLines = withLines,
        )
    }

    // JSXGraph: src/base/polygon.js -> createRegularPolygon,
    // existing n-Point branch.
    internal fun create(
        board: Board,
        vertices: List<Point>,
        ownedPoints: Set<Point> = emptySet(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        withLines: Boolean = true,
    ): GMResult<Polygon, RegularPolygonError> {
        for ((index, vertex) in vertices.withIndex()) {
            validateParent(board, vertex, index)?.let {
                return GMResult.Err(it)
            }
        }

        val snapshots = vertices.drop(2).map(::snapshot)
        val angle = PI * (
            2.0 -
                (vertices.size.toDouble() - 2.0) /
                vertices.size.toDouble()
            )
        for (index in 2 until vertices.size) {
            val rotation = when (
                val result = Transformation.createRotation(
                    board = board,
                    angle = TransformationParameter.Numeric(angle),
                    center = vertices[index - 1],
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    restore(snapshots)
                    return GMResult.Err(
                        RegularPolygonError.TransformationCreation(
                            vertexIndex = index,
                            error = result.error,
                        ),
                    )
                }
            }
            vertices[index].addTransform(vertices[index - 2], rotation)
            vertices[index].fullUpdate()
        }

        return when (
            val result = finish(
                board = board,
                vertices = vertices,
                ownedPoints = ownedPoints,
                generatedPoints = emptyList(),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                withLines = withLines,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> {
                restore(snapshots)
                result
            }
        }
    }

    private fun finish(
        board: Board,
        vertices: List<Point>,
        ownedPoints: Set<Point>,
        generatedPoints: List<Point>,
        id: String,
        name: String?,
        needsRegularUpdate: Boolean,
        withLines: Boolean,
    ): GMResult<Polygon, RegularPolygonError> =
        when (
            val result = Polygon.create(
                board = board,
                vertices = vertices,
                ownedVertices = ownedPoints,
                withLines = withLines,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> {
                val polygon = result.value
                polygon.elType = REGULAR_POLYGON_ELEMENT_TYPE
                polygon.implicitVertices =
                    ownedPoints.toList() + generatedPoints
                GMResult.Ok(polygon)
            }
            is GMResult.Err -> {
                board.removeObjects(generatedPoints)
                GMResult.Err(
                    RegularPolygonError.PolygonCreation(result.error),
                )
            }
        }

    private fun validateParent(
        board: Board,
        point: Point,
        parentIndex: Int,
    ): RegularPolygonError? =
        when {
            point.board !== board ->
                RegularPolygonError.ParentBoardMismatch(parentIndex)
            board.elementById(point.id) !== point ->
                RegularPolygonError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = point.id,
                )
            else -> null
        }

    private fun snapshot(point: Point): PointState =
        PointState(
            point = point,
            transformations = point.transformations.toList(),
            baseElement = point.baseElement,
            coordinates = point.coords.usrCoords.copyOf(),
            initialCoordinates = point.initialCoords.usrCoords.copyOf(),
            actualCoordinates = point.actualCoords.usrCoords.copyOf(),
        )

    private fun restore(snapshots: List<PointState>) {
        for (snapshot in snapshots) {
            val point = snapshot.point
            point.transformations.clear()
            point.transformations += snapshot.transformations
            point.baseElement = snapshot.baseElement
            point.coords.setCoordinates(
                Const.COORDS_BY_USER,
                snapshot.coordinates,
            )
            point.initialCoords.setCoordinates(
                Const.COORDS_BY_USER,
                snapshot.initialCoordinates,
            )
            point.actualCoords.setCoordinates(
                Const.COORDS_BY_USER,
                snapshot.actualCoordinates,
            )
        }
    }

    private data class PointState(
        val point: Point,
        val transformations: List<Transformation>,
        val baseElement: GeometryElement?,
        val coordinates: DoubleArray,
        val initialCoordinates: DoubleArray,
        val actualCoordinates: DoubleArray,
    )
}
