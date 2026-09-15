/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/sector.js -> createSector, createAngle,
 * updateDataArray, updateDataArraySector, Radius, autoRadius
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

internal sealed interface SectorError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : SectorError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : SectorError

    data class InvalidSelection(
        val selection: String,
    ) : SectorError

    data class InvalidOrientation(
        val orientation: String,
    ) : SectorError

    data class InvalidRadius(
        val radius: Double,
    ) : SectorError

    data class Registration(
        val error: BoardError,
    ) : SectorError
}

internal sealed interface AngleRadius {
    data object Auto : AngleRadius

    data class Fixed(
        val value: Double,
    ) : AngleRadius
}

/**
 * Translated three-point JXG.Sector and JXG.Angle subset.
 *
 * JSXGraph implements both as Curve instances. The Kotlin translation keeps
 * the shared sector lifecycle in one type and preserves Angle's parent order,
 * radius scaling, object type, and generated-name behavior.
 */
internal class Sector private constructor(
    board: Board,
    internal val point1: Point,
    internal val point2: Point,
    internal val point3: Point,
    internal val selection: String,
    internal val orientation: String,
    internal val ownedPoints: Set<Point>,
    private val angleRadius: AngleRadius?,
    private val sourceParents: List<Point>,
    id: String = "",
    name: String? = null,
    needsRegularUpdate: Boolean = true,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type =
        if (angleRadius == null) {
            Const.OBJECT_TYPE_SECTOR
        } else {
            Const.OBJECT_TYPE_ANGLE
        },
    elementClass = Const.OBJECT_CLASS_CURVE,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal val center: Point = point1
    internal val radiuspoint: Point = point2
    internal val anglepoint: Point = point3
    internal val points = mutableListOf<Coords>()
    internal var numberPoints: Int = 0
        private set
    internal val bezierDegree: Int = 3
    internal val isAngle: Boolean
        get() = angleRadius != null

    init {
        elType = if (isAngle) ANGLE_ELEMENT_TYPE else SECTOR_ELEMENT_TYPE
        isDraggable = true
    }

    // JSXGraph: src/element/sector.js -> updateDataArray /
    // createAngle -> updateDataArraySector
    override fun update(fromParent: Boolean): Sector {
        if (!needsUpdate) {
            return this
        }
        if (!point1.isReal || !point2.isReal || !point3.isReal) {
            setData(
                xCoordinates = doubleArrayOf(Double.NaN),
                yCoordinates = doubleArrayOf(Double.NaN),
            )
            return this
        }

        val phi = Geometry.rad(
            point2.Coords(),
            point1.Coords(),
            point3.Coords(),
        )
        val sign =
            if (isAngle) {
                if (
                    (selection == Arc.SELECTION_MINOR && phi > PI) ||
                    (selection == Arc.SELECTION_MAJOR && phi < PI) ||
                    (
                        selection == Arc.SELECTION_AUTO &&
                            orientation == Arc.ORIENTATION_CLOCKWISE
                        )
                ) {
                    -1.0
                } else {
                    1.0
                }
            } else if (
                (
                    orientation == Arc.ORIENTATION_COUNTERCLOCKWISE &&
                        (
                            (
                                selection == Arc.SELECTION_MINOR &&
                                    phi > PI
                                ) ||
                                (
                                    selection == Arc.SELECTION_MAJOR &&
                                        phi < PI
                                    )
                            )
                    ) ||
                (
                    orientation == Arc.ORIENTATION_CLOCKWISE &&
                        (
                            selection == Arc.SELECTION_AUTO ||
                                (
                                    selection == Arc.SELECTION_MINOR &&
                                        phi > PI
                                    ) ||
                                (
                                    selection == Arc.SELECTION_MAJOR &&
                                        phi < PI
                                    )
                            )
                    )
            ) {
                -1.0
            } else {
                1.0
            }

        val first = point2.coords.usrCoords.copyOf()
        val vertex = point1.coords.usrCoords.copyOf()
        val third = point3.coords.usrCoords.copyOf()
        if (isAngle) {
            val radius = Radius()
            val distance = point1.Dist(point2)
            first[0] = 1.0
            first[1] = vertex[1] + (first[1] - vertex[1]) * radius / distance
            first[2] = vertex[2] + (first[2] - vertex[2]) * radius / distance
            third[0] = 1.0
            third[1] = vertex[1] + (third[1] - vertex[1]) * radius / distance
            third[2] = vertex[2] + (third[2] - vertex[2]) * radius / distance
        }

        val sector = Geometry.bezierArc(
            first = first,
            center = vertex,
            third = third,
            withLegs = true,
            sign = sign,
        )
        setData(sector.xCoordinates, sector.yCoordinates)
        return this
    }

    // JSXGraph: src/element/sector.js -> Radius / createAngle -> Radius
    @Suppress("FunctionName")
    internal fun Radius(): Double =
        when (val radius = angleRadius) {
            null -> point2.Dist(point1)
            AngleRadius.Auto -> autoRadius()
            is AngleRadius.Fixed -> radius.value
        }

    // JSXGraph: src/element/sector.js -> autoRadius
    internal fun autoRadius(): Double {
        val minimum = 20.0 / board.unitX
        val pointDistance = center.Dist(point2) * 0.3333
        val maximum = 50.0 / board.unitX
        return max(minimum, min(pointDistance, maximum))
    }

    private fun setData(
        xCoordinates: DoubleArray,
        yCoordinates: DoubleArray,
    ) {
        points.clear()
        val count = minOf(xCoordinates.size, yCoordinates.size)
        for (index in 0 until count) {
            points += Coords(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(
                    xCoordinates[index],
                    yCoordinates[index],
                ),
                board = board,
            )
        }
        numberPoints = count
    }

    internal companion object {
        private const val CURVE_ID_PREFIX = "G"
        private const val SECTOR_ELEMENT_TYPE = "sector"
        private const val ANGLE_ELEMENT_TYPE = "angle"
        private val SELECTIONS = setOf(
            Arc.SELECTION_AUTO,
            Arc.SELECTION_MINOR,
            Arc.SELECTION_MAJOR,
        )
        private val ORIENTATIONS = setOf(
            Arc.ORIENTATION_COUNTERCLOCKWISE,
            Arc.ORIENTATION_CLOCKWISE,
        )

        // JSXGraph: src/element/sector.js -> createSector three-point branch
        internal fun create(
            board: Board,
            center: Point,
            radiuspoint: Point,
            anglepoint: Point,
            selection: String = Arc.SELECTION_AUTO,
            orientation: String = Arc.ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Sector, SectorError> =
            createInternal(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                selection = selection,
                orientation = orientation,
                ownedPoints = ownedPoints,
                angleRadius = null,
                sourceParents = listOf(center, radiuspoint, anglepoint),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        // JSXGraph: src/element/sector.js -> createAngle three-point branch
        internal fun createAngle(
            board: Board,
            first: Point,
            vertex: Point,
            third: Point,
            radius: AngleRadius = AngleRadius.Auto,
            selection: String = Arc.SELECTION_AUTO,
            orientation: String = Arc.ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Sector, SectorError> {
            if (radius is AngleRadius.Fixed && !radius.value.isFinite()) {
                return GMResult.Err(
                    SectorError.InvalidRadius(radius.value),
                )
            }
            return createInternal(
                board = board,
                center = vertex,
                radiuspoint = first,
                anglepoint = third,
                selection = selection,
                orientation = orientation,
                ownedPoints = ownedPoints,
                angleRadius = radius,
                sourceParents = listOf(first, vertex, third),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        }

        private fun createInternal(
            board: Board,
            center: Point,
            radiuspoint: Point,
            anglepoint: Point,
            selection: String,
            orientation: String,
            ownedPoints: Set<Point>,
            angleRadius: AngleRadius?,
            sourceParents: List<Point>,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
        ): GMResult<Sector, SectorError> {
            for ((index, parent) in sourceParents.withIndex()) {
                validateParent(board, parent, index)?.let {
                    return GMResult.Err(it)
                }
            }
            val normalizedSelection = selection.lowercase()
            if (normalizedSelection !in SELECTIONS) {
                return GMResult.Err(
                    SectorError.InvalidSelection(selection),
                )
            }
            val normalizedOrientation = orientation.lowercase()
            if (normalizedOrientation !in ORIENTATIONS) {
                return GMResult.Err(
                    SectorError.InvalidOrientation(orientation),
                )
            }

            val sector = Sector(
                board = board,
                point1 = center,
                point2 = radiuspoint,
                point3 = anglepoint,
                selection = normalizedSelection,
                orientation = normalizedOrientation,
                ownedPoints = ownedPoints,
                angleRadius = angleRadius,
                sourceParents = sourceParents,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val registration = board.setId(sector, CURVE_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    for (parent in sourceParents) {
                        if (parent in ownedPoints) {
                            sector.addChild(parent)
                        } else {
                            parent.addChild(sector)
                        }
                    }
                    sector.setParents(sourceParents)
                    sector.update()
                    GMResult.Ok(sector)
                }
                is GMResult.Err -> GMResult.Err(
                    SectorError.Registration(registration.error),
                )
            }
        }

        private fun validateParent(
            board: Board,
            point: Point,
            parentIndex: Int,
        ): SectorError? {
            if (point.board !== board) {
                return SectorError.ParentBoardMismatch(parentIndex)
            }
            if (board.elementById(point.id) !== point) {
                return SectorError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = point.id,
                )
            }
            return null
        }
    }
}
