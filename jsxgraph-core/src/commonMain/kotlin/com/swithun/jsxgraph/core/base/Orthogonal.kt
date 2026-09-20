/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/composition.js -> createOrthogonalProjection /
 * createPerpendicular / createPerpendicularPoint /
 * createPerpendicularSegment
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.PerpendicularPointRole

internal sealed interface OrthogonalConstructionError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : OrthogonalConstructionError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : OrthogonalConstructionError

    data class OwnedPointNotParent(
        val id: String,
    ) : OrthogonalConstructionError

    data class HelperPoint(
        val error: PointError,
    ) : OrthogonalConstructionError

    data class Registration(
        val error: BoardError,
    ) : OrthogonalConstructionError
}

internal enum class OrthogonalPointKind(
    internal val elementType: String,
) {
    ORTHOGONAL_PROJECTION("orthogonalprojection"),
    PERPENDICULAR_POINT("perpendicularpoint"),
}

/**
 * Constrained Point used by the two JSXGraph orthogonal-point factories.
 */
internal class OrthogonalPoint private constructor(
    board: Board,
    internal val sourcePoint: Point,
    internal val sourceLine: Line,
    internal val kind: OrthogonalPointKind,
    internal val ownsSourcePoint: Boolean,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    fixed: Boolean,
) : Point(
    board = board,
    coordinates = coordinates(kind, sourcePoint, sourceLine),
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
    fixed = fixed,
) {
    init {
        type = Const.OBJECT_TYPE_CAS
        elType = kind.elementType
        isDraggable = false
    }

    // JSXGraph: src/element/composition.js ->
    // createOrthogonalProjection / createPerpendicularPoint constraints
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = coordinates(kind, sourcePoint, sourceLine),
        )
        return this
    }

    internal companion object {
        private const val POINT_ID_PREFIX = "P"

        fun create(
            board: Board,
            point: Point,
            line: Line,
            kind: OrthogonalPointKind,
            ownsPoint: Boolean = false,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<OrthogonalPoint, OrthogonalConstructionError> {
            validateParent(board, point, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParent(board, line, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }
            if (ownsPoint && point.board !== board) {
                return GMResult.Err(
                    OrthogonalConstructionError.OwnedPointNotParent(point.id),
                )
            }

            val output = OrthogonalPoint(
                board = board,
                sourcePoint = point,
                sourceLine = line,
                kind = kind,
                ownsSourcePoint = ownsPoint,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )
            output.baseElement = output
            return when (
                val registration = board.setId(output, POINT_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    if (ownsPoint) {
                        output.addChild(point)
                    } else {
                        point.addChild(output)
                    }
                    line.addChild(output)
                    if (kind == OrthogonalPointKind.ORTHOGONAL_PROJECTION) {
                        // JSXGraph 1.13.3 intentionally stores the output's
                        // own id instead of the source line id.
                        output.setParents(listOf(point, output))
                    } else {
                        output.setParents(listOf(point, line))
                    }
                    output.update()
                    GMResult.Ok(output)
                }

                is GMResult.Err -> GMResult.Err(
                    OrthogonalConstructionError.Registration(
                        registration.error,
                    ),
                )
            }
        }

        private fun coordinates(
            kind: OrthogonalPointKind,
            point: Point,
            line: Line,
        ): DoubleArray =
            when (kind) {
                OrthogonalPointKind.ORTHOGONAL_PROJECTION ->
                    Geometry.projectPointToLine(
                        point = point.coords.usrCoords,
                        line = line.stdform,
                    )
                OrthogonalPointKind.PERPENDICULAR_POINT ->
                    perpendicular(line, point).point
            }
    }
}

/**
 * Infinite constrained line created by JSXGraph's `perpendicular` factory.
 */
internal class PerpendicularLine private constructor(
    board: Board,
    point1: Point,
    point2: Point,
    internal val sourceLine: Line,
    internal val sourcePoint: Point,
    internal val ownsSourcePoint: Boolean,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : Line(
    board = board,
    point1 = point1,
    point2 = point2,
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
) {
    init {
        elType = PERPENDICULAR_ELEMENT_TYPE
        isDraggable = false
    }

    // JSXGraph: src/element/composition.js -> createPerpendicular
    // and src/base/line.js -> createLine([a, b, c])
    override fun update(fromParent: Boolean): PerpendicularLine {
        if (!needsUpdate) {
            return this
        }
        val endpoints = perpendicularLineEndpoints(sourceLine, sourcePoint)
        point1.coords.setCoordinates(
            Const.COORDS_BY_USER,
            endpoints.first,
        )
        point2.coords.setCoordinates(
            Const.COORDS_BY_USER,
            endpoints.second,
        )
        updateStdform()
        return this
    }

    internal companion object {
        private const val LINE_ID_PREFIX = "L"
        private const val PERPENDICULAR_ELEMENT_TYPE = "perpendicular"

        fun create(
            board: Board,
            line: Line,
            point: Point,
            ownsPoint: Boolean = false,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<PerpendicularLine, OrthogonalConstructionError> {
            validateParent(board, line, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParent(board, point, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }
            if (ownsPoint && point.board !== board) {
                return GMResult.Err(
                    OrthogonalConstructionError.OwnedPointNotParent(point.id),
                )
            }

            val endpoints = perpendicularLineEndpoints(line, point)
            val point1 = when (
                val result = createHelperPoint(board, endpoints.first)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val point2 = when (
                val result = createHelperPoint(board, endpoints.second)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    board.removeObject(point1)
                    return result
                }
            }
            val output = PerpendicularLine(
                board = board,
                point1 = point1,
                point2 = point2,
                sourceLine = line,
                sourcePoint = point,
                ownsSourcePoint = ownsPoint,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val registration = board.setId(output, LINE_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    output.addChild(point1)
                    output.addChild(point2)
                    if (ownsPoint) {
                        output.addChild(point)
                    } else {
                        point.addChild(output)
                    }
                    line.addChild(output)
                    output.setParents(listOf(line, point))
                    output.update()
                    GMResult.Ok(output)
                }

                is GMResult.Err -> {
                    board.removeObjects(listOf(point1, point2))
                    GMResult.Err(
                        OrthogonalConstructionError.Registration(
                            registration.error,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Finite constrained segment returned by `perpendicularsegment`.
 *
 * The helper point deliberately survives removal of this line, matching
 * JSXGraph 1.13.3's `subs` / `inherits` ownership behavior.
 */
internal class PerpendicularSegmentLine private constructor(
    board: Board,
    point1: Point,
    point2: Point,
    internal val sourceLine: Line,
    internal val sourcePoint: Point,
    internal val point: OrthogonalPoint,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : Line(
    board = board,
    point1 = point1,
    point2 = point2,
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
) {
    init {
        elType = PERPENDICULAR_SEGMENT_ELEMENT_TYPE
        isDraggable = false
    }

    // JSXGraph: src/element/composition.js ->
    // createPerpendicularSegment constrained endpoint function
    override fun update(fromParent: Boolean): PerpendicularSegmentLine {
        if (!needsUpdate) {
            return this
        }
        val change = perpendicular(sourceLine, sourcePoint).change
        if (change) {
            point1 = point
            point2 = sourcePoint
        } else {
            point1 = sourcePoint
            point2 = point
        }
        updateStdform()
        return this
    }

    internal companion object {
        private const val LINE_ID_PREFIX = "L"
        private const val PERPENDICULAR_SEGMENT_ELEMENT_TYPE =
            "perpendicularsegment"

        fun create(
            board: Board,
            line: Line,
            sourcePoint: Point,
            ownsSourcePoint: Boolean = false,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<PerpendicularSegmentLine, OrthogonalConstructionError> {
            validateParent(board, line, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParent(board, sourcePoint, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }
            if (ownsSourcePoint && sourcePoint.board !== board) {
                return GMResult.Err(
                    OrthogonalConstructionError.OwnedPointNotParent(
                        sourcePoint.id,
                    ),
                )
            }

            val helper = when (
                val result = OrthogonalPoint.create(
                    board = board,
                    point = sourcePoint,
                    line = line,
                    kind = OrthogonalPointKind.PERPENDICULAR_POINT,
                    ownsPoint = ownsSourcePoint,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val change = perpendicular(line, sourcePoint).change
            val output = PerpendicularSegmentLine(
                board = board,
                point1 = if (change) helper else sourcePoint,
                point2 = if (change) sourcePoint else helper,
                sourceLine = line,
                sourcePoint = sourcePoint,
                point = helper,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val registration = board.setId(output, LINE_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    helper.addChild(output)
                    sourcePoint.addChild(output)
                    line.addChild(output)
                    output.setParents(listOf(sourcePoint, line))
                    output.update()
                    GMResult.Ok(output)
                }

                is GMResult.Err -> {
                    board.removeObject(helper)
                    GMResult.Err(
                        OrthogonalConstructionError.Registration(
                            registration.error,
                        ),
                    )
                }
            }
        }
    }
}

private fun validateParent(
    board: Board,
    element: GeometryElement,
    parentIndex: Int,
): OrthogonalConstructionError? {
    if (element.board !== board) {
        return OrthogonalConstructionError.ParentBoardMismatch(parentIndex)
    }
    if (board.elementById(element.id) !== element) {
        return OrthogonalConstructionError.ParentNotRegistered(
            parentIndex = parentIndex,
            id = element.id,
        )
    }
    return null
}

private fun perpendicular(
    line: Line,
    point: Point,
) = Geometry.perpendicular(
    lineFirst = line.point1.coords.usrCoords,
    lineSecond = line.point2.coords.usrCoords,
    point = point.coords.usrCoords,
    pointRole = when (point) {
        line.point1 -> PerpendicularPointRole.FIRST_LINE_POINT
        line.point2 -> PerpendicularPointRole.SECOND_LINE_POINT
        else -> PerpendicularPointRole.OTHER
    },
    lineStandardForm = line.stdform,
)

// JSXGraph: src/element/composition.js -> createPerpendicular coefficient
// functions and src/base/line.js -> createLine([a, b, c]).
private fun perpendicularLineEndpoints(
    line: Line,
    point: Point,
): Pair<DoubleArray, DoubleArray> {
    val pointCoordinates = point.coords.usrCoords
    val constant =
        line.stdform[2] * pointCoordinates[1] -
            line.stdform[1] * pointCoordinates[2]
    val horizontal = -line.stdform[2] * pointCoordinates[0]
    val vertical = line.stdform[1] * pointCoordinates[0]
    val homogeneous = vertical * vertical + horizontal * horizontal
    return Pair(
        doubleArrayOf(
            homogeneous * 0.5,
            (vertical - horizontal * constant + vertical) * 0.5,
            (-horizontal - vertical * constant - horizontal) * 0.5,
        ),
        doubleArrayOf(
            homogeneous,
            -horizontal * constant + vertical,
            -vertical * constant - horizontal,
        ),
    )
}

private fun createHelperPoint(
    board: Board,
    coordinates: DoubleArray,
): GMResult<Point, OrthogonalConstructionError> =
    when (
        val result = Point.create(
            board = board,
            coordinates = coordinates,
            name = "",
        )
    ) {
        is GMResult.Err -> GMResult.Err(
            OrthogonalConstructionError.HelperPoint(result.error),
        )
        is GMResult.Ok -> {
            result.value.type = Const.OBJECT_TYPE_CAS
            result.value.isDraggable = false
            GMResult.Ok(result.value)
        }
    }
