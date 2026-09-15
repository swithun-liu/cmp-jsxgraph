/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/arc.js -> createArc, updateDataArray, Radius, Value
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import kotlin.math.PI

internal sealed interface ArcError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : ArcError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : ArcError

    data class InvalidSelection(
        val selection: String,
    ) : ArcError

    data class InvalidOrientation(
        val orientation: String,
    ) : ArcError

    data class Registration(
        val error: BoardError,
    ) : ArcError
}

/**
 * Translated three-point JXG.Arc subset.
 *
 * The upstream factory creates a Curve and replaces updateDataArray with the
 * arc implementation. This Kotlin type keeps that lifecycle explicit while
 * preserving the same cubic Bezier data and dependency graph.
 */
internal class Arc private constructor(
    board: Board,
    internal val center: Point,
    internal val radiuspoint: Point,
    internal val anglepoint: Point,
    internal val selection: String,
    internal val orientation: String,
    internal val ownedPoints: Set<Point>,
    id: String = "",
    name: String? = null,
    needsRegularUpdate: Boolean = true,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_ARC,
    elementClass = Const.OBJECT_CLASS_CURVE,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal val point2: Point = radiuspoint
    internal val point3: Point = anglepoint
    internal val points = mutableListOf<Coords>()
    internal var numberPoints: Int = 0
        private set
    internal val bezierDegree: Int = 3

    init {
        elType = ARC_ELEMENT_TYPE
        isDraggable = true
    }

    // JSXGraph: src/element/arc.js -> updateDataArray
    override fun update(fromParent: Boolean): Arc {
        if (!needsUpdate) {
            return this
        }
        val phi = Geometry.rad(
            radiuspoint.Coords(),
            center.Coords(),
            anglepoint.Coords(),
        )
        val sign =
            if (
                (selection == SELECTION_MINOR && phi > PI) ||
                (selection == SELECTION_MAJOR && phi < PI) ||
                (
                    selection == SELECTION_AUTO &&
                        orientation == ORIENTATION_CLOCKWISE
                    )
            ) {
                -1.0
            } else {
                1.0
            }
        val arc = Geometry.bezierArc(
            first = radiuspoint.coords.usrCoords.copyOf(),
            center = center.coords.usrCoords.copyOf(),
            third = anglepoint.coords.usrCoords.copyOf(),
            withLegs = false,
            sign = sign,
        )
        setData(arc.xCoordinates, arc.yCoordinates)
        return this
    }

    // JSXGraph: src/element/arc.js -> Radius
    @Suppress("FunctionName")
    internal fun Radius(): Double = radiuspoint.Dist(center)

    // JSXGraph: src/element/arc.js -> Value
    @Suppress("FunctionName")
    internal fun Value(unit: String = "length"): Double {
        var radians = Geometry.rad(
            radiuspoint.Coords(),
            center.Coords(),
            anglepoint.Coords(),
        )
        if (orientation == ORIENTATION_CLOCKWISE) {
            radians = 2.0 * PI - radians
        }
        return when {
            unit.isEmpty() || unit.lowercase().startsWith("len") ->
                radians * Radius()
            unit.lowercase().startsWith("rad") -> radians
            unit.lowercase().startsWith("deg") -> radians * 180.0 / PI
            unit.lowercase().startsWith("sem") -> radians / PI
            unit.lowercase().startsWith("cir") -> radians * 0.5 / PI
            else -> Double.NaN
        }
    }

    private fun setData(
        dataX: DoubleArray,
        dataY: DoubleArray,
    ) {
        points.clear()
        val count = minOf(dataX.size, dataY.size)
        for (index in 0 until count) {
            points += Coords(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(dataX[index], dataY[index]),
                board = board,
            )
        }
        numberPoints = count
    }

    internal companion object {
        internal const val SELECTION_AUTO = "auto"
        internal const val SELECTION_MINOR = "minor"
        internal const val SELECTION_MAJOR = "major"
        internal const val ORIENTATION_COUNTERCLOCKWISE = "counterclockwise"
        internal const val ORIENTATION_CLOCKWISE = "clockwise"

        private const val ARC_ID_PREFIX = "G"
        private const val ARC_ELEMENT_TYPE = "arc"
        private val SELECTIONS =
            setOf(SELECTION_AUTO, SELECTION_MINOR, SELECTION_MAJOR)
        private val ORIENTATIONS =
            setOf(ORIENTATION_COUNTERCLOCKWISE, ORIENTATION_CLOCKWISE)

        // JSXGraph: src/element/arc.js -> createArc
        internal fun create(
            board: Board,
            center: Point,
            radiuspoint: Point,
            anglepoint: Point,
            selection: String = SELECTION_AUTO,
            orientation: String = ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Arc, ArcError> {
            val parents = listOf(center, radiuspoint, anglepoint)
            for ((index, parent) in parents.withIndex()) {
                validateParent(board, parent, index)?.let {
                    return GMResult.Err(it)
                }
            }
            val normalizedSelection = selection.lowercase()
            if (normalizedSelection !in SELECTIONS) {
                return GMResult.Err(
                    ArcError.InvalidSelection(selection),
                )
            }
            val normalizedOrientation = orientation.lowercase()
            if (normalizedOrientation !in ORIENTATIONS) {
                return GMResult.Err(
                    ArcError.InvalidOrientation(orientation),
                )
            }

            val arc = Arc(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                selection = normalizedSelection,
                orientation = normalizedOrientation,
                ownedPoints = ownedPoints,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val registration = board.setId(arc, ARC_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    for (parent in parents) {
                        if (parent in ownedPoints) {
                            arc.addChild(parent)
                        } else {
                            parent.addChild(arc)
                        }
                    }
                    arc.setParents(parents)
                    arc.update()
                    GMResult.Ok(arc)
                }
                is GMResult.Err -> GMResult.Err(
                    ArcError.Registration(registration.error),
                )
            }
        }

        private fun validateParent(
            board: Board,
            point: Point,
            parentIndex: Int,
        ): ArcError? {
            if (point.board !== board) {
                return ArcError.ParentBoardMismatch(parentIndex)
            }
            if (board.elementById(point.id) !== point) {
                return ArcError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = point.id,
                )
            }
            return null
        }
    }
}
