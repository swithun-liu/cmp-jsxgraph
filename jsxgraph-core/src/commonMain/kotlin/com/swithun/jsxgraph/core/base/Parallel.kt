/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/composition.js -> createParallelPoint / createParallel
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat

internal sealed interface ParallelConstructionError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : ParallelConstructionError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : ParallelConstructionError

    data class OwnedPointNotParent(
        val id: String,
    ) : ParallelConstructionError

    data class HelperPoint(
        val error: PointError,
    ) : ParallelConstructionError

    data class Registration(
        val error: BoardError,
    ) : ParallelConstructionError
}

/**
 * Constrained fourth point of the parallelogram defined by three Points.
 *
 * Coordinate-array parents are helper Points created by `providePoints`.
 * JSXGraph makes those helpers children of the output so removing the output
 * recursively removes them.
 */
internal class ParallelPoint private constructor(
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
    coordinates = parallelCoordinates(point1, point2, point3),
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
    fixed = fixed,
) {
    init {
        type = Const.OBJECT_TYPE_CAS
        elType = PARALLEL_POINT_ELEMENT_TYPE
        isDraggable = false
    }

    // JSXGraph: src/element/composition.js -> createParallelPoint constraints
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = parallelCoordinates(point1, point2, point3),
        )
        return this
    }

    internal companion object {
        private const val POINT_ID_PREFIX = "P"
        private const val PARALLEL_POINT_ELEMENT_TYPE = "parallelpoint"

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
        ): GMResult<ParallelPoint, ParallelConstructionError> {
            val parents = listOf(point1, point2, point3)
            for ((index, parent) in parents.withIndex()) {
                validateParallelParent(board, parent, index)?.let {
                    return GMResult.Err(it)
                }
            }
            if (!ownedPoints.all(parents::contains)) {
                val invalid = ownedPoints.first { it !in parents }
                return GMResult.Err(
                    ParallelConstructionError.OwnedPointNotParent(invalid.id),
                )
            }

            val output = ParallelPoint(
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
            output.baseElement = output
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
                    ParallelConstructionError.Registration(
                        registration.error,
                    ),
                )
            }
        }

        private fun parallelCoordinates(
            point1: Point,
            point2: Point,
            point3: Point,
        ): DoubleArray =
            doubleArrayOf(
                point3.X() + point2.X() - point1.X(),
                point3.Y() + point2.Y() - point1.Y(),
            )
    }
}

/**
 * Dynamic ideal Point used when `parallel` receives a Line parent.
 *
 * The source Line intentionally has no child relationship to this helper.
 * JSXGraph evaluates its coordinate function during each board update and
 * leaves the helper registered after the output line is removed.
 */
internal class ParallelDirectionPoint private constructor(
    board: Board,
    internal val sourceLine: Line,
) : Point(
    board = board,
    coordinates = direction(sourceLine),
    name = "",
    needsRegularUpdate = true,
) {
    init {
        type = Const.OBJECT_TYPE_CAS
        isDraggable = true
    }

    // JSXGraph: src/element/composition.js -> createParallel ideal point
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = direction(sourceLine),
        )
        return this
    }

    internal companion object {
        private const val POINT_ID_PREFIX = "P"

        fun create(
            board: Board,
            sourceLine: Line,
        ): GMResult<ParallelDirectionPoint, ParallelConstructionError> {
            validateParallelParent(
                board = board,
                element = sourceLine,
                parentIndex = 0,
            )?.let {
                return GMResult.Err(it)
            }
            val helper = ParallelDirectionPoint(board, sourceLine)
            helper.baseElement = helper
            return when (
                val registration = board.setId(helper, POINT_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    helper.update()
                    GMResult.Ok(helper)
                }

                is GMResult.Err -> GMResult.Err(
                    ParallelConstructionError.Registration(
                        registration.error,
                    ),
                )
            }
        }

        private fun direction(line: Line): DoubleArray =
            Mat.crossProduct(
                doubleArrayOf(1.0, 0.0, 0.0),
                line.stdform,
            )
    }
}

/**
 * Line through [throughPoint] and the translated finite or ideal [point].
 *
 * The helper is a parent, not a child, so it survives removal of this line as
 * it does in JSXGraph's `subs.point` / `inherits` lifecycle.
 */
internal class ParallelLine private constructor(
    board: Board,
    internal val throughPoint: Point,
    internal val point: Point,
    internal val sourceLine: Line?,
    internal val sourcePoints: List<Point>,
    internal val ownsThroughPoint: Boolean,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : Line(
    board = board,
    point1 = throughPoint,
    point2 = point,
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
) {
    init {
        elType = PARALLEL_ELEMENT_TYPE
        isDraggable = true
    }

    internal companion object {
        private const val LINE_ID_PREFIX = "L"
        private const val PARALLEL_ELEMENT_TYPE = "parallel"

        fun create(
            board: Board,
            point1: Point,
            point2: Point,
            throughPoint: Point,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<ParallelLine, ParallelConstructionError> {
            val parents = listOf(point1, point2, throughPoint)
            for ((index, parent) in parents.withIndex()) {
                validateParallelParent(board, parent, index)?.let {
                    return GMResult.Err(it)
                }
            }
            if (!ownedPoints.all(parents::contains)) {
                val invalid = ownedPoints.first { it !in parents }
                return GMResult.Err(
                    ParallelConstructionError.OwnedPointNotParent(invalid.id),
                )
            }

            val helper = when (
                val result = ParallelPoint.create(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    point3 = throughPoint,
                    ownedPoints = ownedPoints,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            helper.isDraggable = true
            return register(
                board = board,
                throughPoint = throughPoint,
                helper = helper,
                sourceLine = null,
                sourcePoints = listOf(point1, point2),
                // createParallelPoint consumes each `_is_new` marker before
                // createLine receives the finite through Point.
                ownsThroughPoint = false,
                parentMetadata = parents,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                removeHelperOnFailure = true,
            )
        }

        fun create(
            board: Board,
            sourceLine: Line,
            throughPoint: Point,
            ownsThroughPoint: Boolean = false,
            parentMetadata: List<GeometryElement> =
                listOf(sourceLine, throughPoint),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<ParallelLine, ParallelConstructionError> {
            validateParallelParent(board, sourceLine, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParallelParent(board, throughPoint, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }
            if (
                parentMetadata.size != 2 ||
                parentMetadata.toSet() != setOf(sourceLine, throughPoint)
            ) {
                val invalid = parentMetadata.firstOrNull {
                    it !== sourceLine && it !== throughPoint
                }
                return GMResult.Err(
                    ParallelConstructionError.OwnedPointNotParent(
                        invalid?.id ?: throughPoint.id,
                    ),
                )
            }

            val helper = when (
                val result = ParallelDirectionPoint.create(board, sourceLine)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return register(
                board = board,
                throughPoint = throughPoint,
                helper = helper,
                sourceLine = sourceLine,
                sourcePoints = emptyList(),
                ownsThroughPoint = ownsThroughPoint,
                parentMetadata = parentMetadata,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                removeHelperOnFailure = true,
            )
        }

        private fun register(
            board: Board,
            throughPoint: Point,
            helper: Point,
            sourceLine: Line?,
            sourcePoints: List<Point>,
            ownsThroughPoint: Boolean,
            parentMetadata: List<GeometryElement>,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
            removeHelperOnFailure: Boolean,
        ): GMResult<ParallelLine, ParallelConstructionError> {
            val output = ParallelLine(
                board = board,
                throughPoint = throughPoint,
                point = helper,
                sourceLine = sourceLine,
                sourcePoints = sourcePoints,
                ownsThroughPoint = ownsThroughPoint,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val registration = board.setId(output, LINE_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    if (ownsThroughPoint) {
                        output.addChild(throughPoint)
                    } else {
                        throughPoint.addChild(output)
                    }
                    helper.addChild(output)
                    output.setParents(parentMetadata)
                    output.updateStdform()
                    GMResult.Ok(output)
                }

                is GMResult.Err -> {
                    if (removeHelperOnFailure) {
                        board.removeObject(helper)
                    }
                    GMResult.Err(
                        ParallelConstructionError.Registration(
                            registration.error,
                        ),
                    )
                }
            }
        }
    }
}

private fun validateParallelParent(
    board: Board,
    element: GeometryElement,
    parentIndex: Int,
): ParallelConstructionError? {
    if (element.board !== board) {
        return ParallelConstructionError.ParentBoardMismatch(parentIndex)
    }
    if (board.elementById(element.id) !== element) {
        return ParallelConstructionError.ParentNotRegistered(
            parentIndex = parentIndex,
            id = element.id,
        )
    }
    return null
}
