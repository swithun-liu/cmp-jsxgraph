/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/composition.js -> createBisector / createIncenter /
 * createIncircle
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.sqrt

internal sealed interface TriangleCenterConstructionError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : TriangleCenterConstructionError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : TriangleCenterConstructionError

    data class OwnedPointNotParent(
        val id: String,
    ) : TriangleCenterConstructionError

    data class Registration(
        val error: BoardError,
    ) : TriangleCenterConstructionError
}

/**
 * The hidden constrained Point used as the second point of a Bisector.
 *
 * Its geometric parents are intentionally absent from [parents]. JSXGraph's
 * coordinate function establishes update dependencies through `addChild`
 * while `createBisector` only assigns the three source parents to the Line.
 */
internal class AngleBisectorPoint private constructor(
    board: Board,
    internal val point1: Point,
    internal val vertex: Point,
    internal val point3: Point,
    internal val ownedPoints: Set<Point>,
) : Point(
    board = board,
    coordinates = bisectorCoordinates(point1, vertex, point3),
    name = "",
    needsRegularUpdate = true,
    fixed = true,
) {
    init {
        type = Const.OBJECT_TYPE_CAS
        isDraggable = false
    }

    // JSXGraph: src/element/composition.js -> createBisector constraint
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = bisectorCoordinates(point1, vertex, point3),
        )
        return this
    }

    internal companion object {
        private const val POINT_ID_PREFIX = "P"

        fun create(
            board: Board,
            point1: Point,
            vertex: Point,
            point3: Point,
            ownedPoints: Set<Point>,
        ): GMResult<AngleBisectorPoint, TriangleCenterConstructionError> {
            val parents = listOf(point1, vertex, point3)
            validateTriangleParents(
                board = board,
                parents = parents,
                ownedPoints = ownedPoints,
            )?.let {
                return GMResult.Err(it)
            }

            val helper = AngleBisectorPoint(
                board = board,
                point1 = point1,
                vertex = vertex,
                point3 = point3,
                ownedPoints = ownedPoints,
            )
            return when (
                val registration = board.setId(helper, POINT_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    for (parent in parents) {
                        if (parent in ownedPoints) {
                            helper.addChild(parent)
                        } else {
                            parent.addChild(helper)
                        }
                    }
                    helper.update()
                    GMResult.Ok(helper)
                }

                is GMResult.Err -> GMResult.Err(
                    TriangleCenterConstructionError.Registration(
                        registration.error,
                    ),
                )
            }
        }

        private fun bisectorCoordinates(
            point1: Point,
            vertex: Point,
            point3: Point,
        ): DoubleArray =
            Geometry.angleBisector(
                first = point1.coords.usrCoords,
                vertex = vertex.coords.usrCoords,
                third = point3.coords.usrCoords,
            )
    }
}

/**
 * A Line through the angle vertex and its hidden [point].
 */
internal class BisectorLine private constructor(
    board: Board,
    internal val sourcePoint1: Point,
    internal val vertex: Point,
    internal val sourcePoint3: Point,
    internal val point: AngleBisectorPoint,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : Line(
    board = board,
    point1 = vertex,
    point2 = point,
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
) {
    init {
        elType = BISECTOR_ELEMENT_TYPE
        isDraggable = true
    }

    internal companion object {
        private const val LINE_ID_PREFIX = "L"
        private const val BISECTOR_ELEMENT_TYPE = "bisector"

        fun create(
            board: Board,
            point1: Point,
            vertex: Point,
            point3: Point,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<BisectorLine, TriangleCenterConstructionError> {
            val parents = listOf(point1, vertex, point3)
            validateTriangleParents(
                board = board,
                parents = parents,
                ownedPoints = ownedPoints,
            )?.let {
                return GMResult.Err(it)
            }
            duplicateId(board, id)?.let {
                return GMResult.Err(it)
            }

            val helper = when (
                val result = AngleBisectorPoint.create(
                    board = board,
                    point1 = point1,
                    vertex = vertex,
                    point3 = point3,
                    ownedPoints = ownedPoints,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val output = BisectorLine(
                board = board,
                sourcePoint1 = point1,
                vertex = vertex,
                sourcePoint3 = point3,
                point = helper,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val registration = board.setId(output, LINE_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    vertex.addChild(output)
                    helper.addChild(output)
                    output.setParents(parents)
                    output.updateStdform()
                    GMResult.Ok(output)
                }

                is GMResult.Err -> {
                    board.removeObject(helper)
                    GMResult.Err(
                        TriangleCenterConstructionError.Registration(
                            registration.error,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * The side-length weighted center of a triangle's incircle.
 */
internal class IncenterPoint private constructor(
    board: Board,
    internal val point1: Point,
    internal val point2: Point,
    internal val point3: Point,
    internal val ownedPoints: Set<Point>,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    fixed: Boolean,
) : Point(
    board = board,
    coordinates = incenterCoordinates(point1, point2, point3),
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
    fixed = fixed,
) {
    init {
        type = Const.OBJECT_TYPE_CAS
        elType = INCENTER_ELEMENT_TYPE
        isDraggable = false
    }

    // JSXGraph: src/element/composition.js -> createIncenter constraint
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = incenterCoordinates(point1, point2, point3),
        )
        return this
    }

    internal companion object {
        private const val POINT_ID_PREFIX = "P"
        private const val INCENTER_ELEMENT_TYPE = "incenter"

        fun create(
            board: Board,
            point1: Point,
            point2: Point,
            point3: Point,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<IncenterPoint, TriangleCenterConstructionError> {
            val parents = listOf(point1, point2, point3)
            validateTriangleParents(
                board = board,
                parents = parents,
                ownedPoints = ownedPoints,
            )?.let {
                return GMResult.Err(it)
            }

            val output = IncenterPoint(
                board = board,
                point1 = point1,
                point2 = point2,
                point3 = point3,
                ownedPoints = ownedPoints,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )
            return when (
                val registration = board.setId(output, POINT_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    for (parent in parents) {
                        if (parent in ownedPoints) {
                            output.addChild(parent)
                        } else {
                            parent.addChild(output)
                        }
                    }
                    output.setParents(parents)
                    output.update()
                    GMResult.Ok(output)
                }

                is GMResult.Err -> GMResult.Err(
                    TriangleCenterConstructionError.Registration(
                        registration.error,
                    ),
                )
            }
        }

        internal fun incenterCoordinates(
            point1: Point,
            point2: Point,
            point3: Point,
        ): DoubleArray {
            val a = Mat.hypot(
                point2.X() - point3.X(),
                point2.Y() - point3.Y(),
            )
            val b = Mat.hypot(
                point1.X() - point3.X(),
                point1.Y() - point3.Y(),
            )
            val c = Mat.hypot(
                point2.X() - point1.X(),
                point2.Y() - point1.Y(),
            )
            val perimeter = a + b + c
            return doubleArrayOf(
                (
                    a * point1.X() +
                        b * point2.X() +
                        c * point3.X()
                ) / perimeter,
                (
                    a * point1.Y() +
                        b * point2.Y() +
                        c * point3.Y()
                ) / perimeter,
            )
        }
    }
}

/**
 * A Circle centered at a hidden [IncenterPoint] with Heron's inradius.
 */
internal class IncircleCircle private constructor(
    board: Board,
    center: IncenterPoint,
    internal val sourcePoint1: Point,
    internal val sourcePoint2: Point,
    internal val sourcePoint3: Point,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : Circle(
    board = board,
    method = POINT_RADIUS_METHOD,
    center = center,
    nativeRadiusFunction = {
        incircleRadius(sourcePoint1, sourcePoint2, sourcePoint3)
    },
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
) {
    init {
        elType = INCIRCLE_ELEMENT_TYPE
        isDraggable = true
    }

    internal companion object {
        private const val CIRCLE_ID_PREFIX = "C"
        private const val POINT_RADIUS_METHOD = "pointRadius"
        private const val INCIRCLE_ELEMENT_TYPE = "incircle"

        fun create(
            board: Board,
            point1: Point,
            point2: Point,
            point3: Point,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<IncircleCircle, TriangleCenterConstructionError> {
            val parents = listOf(point1, point2, point3)
            validateTriangleParents(
                board = board,
                parents = parents,
                ownedPoints = ownedPoints,
            )?.let {
                return GMResult.Err(it)
            }
            duplicateId(board, id)?.let {
                return GMResult.Err(it)
            }

            val center = when (
                val result = IncenterPoint.create(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    point3 = point3,
                    ownedPoints = ownedPoints,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val output = IncircleCircle(
                board = board,
                center = center,
                sourcePoint1 = point1,
                sourcePoint2 = point2,
                sourcePoint3 = point3,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val registration = board.setId(output, CIRCLE_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    center.addChild(output)
                    for (parent in parents) {
                        parent.addChild(output)
                    }
                    output.setParents(parents)
                    output.update()
                    GMResult.Ok(output)
                }

                is GMResult.Err -> {
                    board.removeObject(center)
                    GMResult.Err(
                        TriangleCenterConstructionError.Registration(
                            registration.error,
                        ),
                    )
                }
            }
        }

        // JSXGraph: src/element/composition.js -> createIncircle radius
        private fun incircleRadius(
            point1: Point,
            point2: Point,
            point3: Point,
        ): Double {
            val a = Mat.hypot(
                point2.X() - point3.X(),
                point2.Y() - point3.Y(),
            )
            val b = Mat.hypot(
                point1.X() - point3.X(),
                point1.Y() - point3.Y(),
            )
            val c = Mat.hypot(
                point2.X() - point1.X(),
                point2.Y() - point1.Y(),
            )
            val semiperimeter = (a + b + c) * 0.5
            return sqrt(
                (
                    (semiperimeter - a) *
                        (semiperimeter - b) *
                        (semiperimeter - c)
                ) / semiperimeter,
            )
        }
    }
}

private fun validateTriangleParents(
    board: Board,
    parents: List<Point>,
    ownedPoints: Set<Point>,
): TriangleCenterConstructionError? {
    for ((index, parent) in parents.withIndex()) {
        if (parent.board !== board) {
            return TriangleCenterConstructionError.ParentBoardMismatch(index)
        }
        if (board.elementById(parent.id) !== parent) {
            return TriangleCenterConstructionError.ParentNotRegistered(
                parentIndex = index,
                id = parent.id,
            )
        }
    }
    if (!ownedPoints.all(parents::contains)) {
        val invalid = ownedPoints.first { it !in parents }
        return TriangleCenterConstructionError.OwnedPointNotParent(
            invalid.id,
        )
    }
    return null
}

private fun duplicateId(
    board: Board,
    id: String,
): TriangleCenterConstructionError? =
    if (id.isNotEmpty() && board.elementById(id) != null) {
        TriangleCenterConstructionError.Registration(
            BoardError.DuplicateElementId(id),
        )
    } else {
        null
    }
