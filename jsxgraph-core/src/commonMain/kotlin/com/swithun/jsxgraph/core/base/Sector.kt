/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/sector.js -> createSector, createAngle,
 * createCircumcircleSector, createMinorSector, createMajorSector,
 * createNonreflexAngle, createReflexAngle, updateDataArray,
 * updateDataArraySector, Radius, Value, autoRadius
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import kotlin.math.PI
import kotlin.math.abs
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

    data class InvalidDirectionPoint(
        val useDirection: Boolean,
        val hasDirectionPoint: Boolean,
    ) : SectorError

    data class OwnedPointNotParent(
        val id: String,
    ) : SectorError

    data class CircumcenterFactory(
        val error: CircumcenterError,
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

internal enum class AngleValueMode {
    DEFAULT,
    NONREFLEX,
    REFLEX,
}

/**
 * Translated three-point and direction-point JXG.Sector/JXG.Angle subset.
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
    internal val point4: Point?,
    internal val useDirection: Boolean,
    internal val selection: String,
    internal val orientation: String,
    internal val ownedPoints: Set<Point>,
    private val angleRadius: AngleRadius?,
    private val angleValueMode: AngleValueMode,
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
    internal val directionpoint: Point? = point4
    internal val points = mutableListOf<Coords>()
    internal val subs = linkedMapOf<String, GeometryElement>()
    internal val inherits = mutableListOf<GeometryElement>()
    internal var numberPoints: Int = 0
        private set
    internal val bezierDegree: Int = 3
    internal val isAngle: Boolean
        get() = angleRadius != null
    internal val usesAutoRadius: Boolean
        get() = angleRadius == AngleRadius.Auto

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
        if (
            !point1.coords.isReal() ||
            !point2.coords.isReal() ||
            !point3.coords.isReal()
        ) {
            setData(
                xCoordinates = doubleArrayOf(Double.NaN),
                yCoordinates = doubleArrayOf(Double.NaN),
            )
            return this
        }

        val sign = directionSign()

        var first = point2.coords.usrCoords.copyOf()
        val vertex = point1.coords.usrCoords.copyOf()
        var third = point3.coords.usrCoords.copyOf()
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

        // JSXGraph: src/element/sector.js -> updateDataArray useDirection
        if (useDirection) {
            val direction = point4
            if (direction != null) {
                val firstCoordinates = point2.coords.usrCoords
                val directionCoordinates = direction.coords.usrCoords
                val thirdCoordinates = point3.coords.usrCoords
                val determinant =
                    (
                        firstCoordinates[1] - thirdCoordinates[1]
                    ) * (
                        firstCoordinates[2] - directionCoordinates[2]
                    ) - (
                        firstCoordinates[2] - thirdCoordinates[2]
                    ) * (
                        firstCoordinates[1] - directionCoordinates[1]
                    )
                if (determinant >= 0.0) {
                    first = point3.coords.usrCoords.copyOf()
                    third = point2.coords.usrCoords.copyOf()
                }
            }
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

    // JSXGraph: src/element/sector.js -> updateDataArray /
    // createAngle -> updateDataArraySector.
    internal fun directionSign(): Double {
        val phi = Geometry.rad(
            point2.Coords(),
            point1.Coords(),
            point3.Coords(),
        )
        if (isAngle) {
            return if (
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
        }
        return if (
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
    }

    // JSXGraph: src/element/sector.js -> Radius / createAngle -> Radius
    @Suppress("FunctionName")
    internal fun Radius(): Double =
        when (val radius = angleRadius) {
            null ->
                if (
                    point1.coords.isReal() &&
                    point2.coords.isReal()
                ) {
                    point2.Dist(point1)
                } else {
                    Double.NaN
                }
            AngleRadius.Auto -> autoRadius()
            is AngleRadius.Fixed -> radius.value
        }

    // JSXGraph: src/element/sector.js -> Value,
    // createNonreflexAngle -> Value, createReflexAngle -> Value
    @Suppress("FunctionName")
    internal fun Value(unit: String? = null): Double {
        val rawRadians = Geometry.rad(
            point2.Coords(),
            point1.Coords(),
            point3.Coords(),
        )
        val radians = when (angleValueMode) {
            AngleValueMode.NONREFLEX ->
                if (rawRadians < PI) rawRadians else 2.0 * PI - rawRadians
            AngleValueMode.REFLEX ->
                if (rawRadians >= PI) rawRadians else 2.0 * PI - rawRadians
            AngleValueMode.DEFAULT ->
                if (orientation == Arc.ORIENTATION_CLOCKWISE) {
                    2.0 * PI - rawRadians
                } else {
                    rawRadians
                }
        }
        val normalizedUnit = when {
            unit.isNullOrEmpty() &&
                angleValueMode != AngleValueMode.DEFAULT -> "radians"
            unit.isNullOrEmpty() -> "length"
            else -> unit.lowercase()
        }
        return when {
            normalizedUnit.startsWith("len") ->
                radians * valueArcRadius()
            normalizedUnit.startsWith("rad") -> radians
            normalizedUnit.startsWith("deg") -> radians * 180.0 / PI
            normalizedUnit.startsWith("sem") -> radians / PI
            normalizedUnit.startsWith("cir") -> radians * 0.5 / PI
            else -> Double.NaN
        }
    }

    private fun valueArcRadius(): Double {
        if (!isAngle) {
            return Radius()
        }
        return if (point1.Dist(point2) == 0.0) {
            0.0
        } else {
            abs(Radius())
        }
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
            directionpoint: Point? = null,
            useDirection: Boolean = false,
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
                directionpoint = directionpoint,
                useDirection = useDirection,
                selection = selection,
                orientation = orientation,
                ownedPoints = ownedPoints,
                angleRadius = null,
                angleValueMode = AngleValueMode.DEFAULT,
                sourceParents = buildList {
                    add(center)
                    add(radiuspoint)
                    add(anglepoint)
                    directionpoint?.let(::add)
                },
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
            valueMode: AngleValueMode = AngleValueMode.DEFAULT,
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
                directionpoint = null,
                useDirection = false,
                selection = selection,
                orientation = orientation,
                ownedPoints = ownedPoints,
                angleRadius = radius,
                angleValueMode = valueMode,
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
            directionpoint: Point?,
            useDirection: Boolean,
            selection: String,
            orientation: String,
            ownedPoints: Set<Point>,
            angleRadius: AngleRadius?,
            angleValueMode: AngleValueMode,
            sourceParents: List<Point>,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
        ): GMResult<Sector, SectorError> {
            if (useDirection && directionpoint == null) {
                return GMResult.Err(
                    SectorError.InvalidDirectionPoint(
                        useDirection = true,
                        hasDirectionPoint = false,
                    ),
                )
            }
            for ((index, parent) in sourceParents.withIndex()) {
                validateParent(board, parent, index)?.let {
                    return GMResult.Err(it)
                }
            }
            if (!ownedPoints.all(sourceParents::contains)) {
                val invalid = ownedPoints.first { it !in sourceParents }
                return GMResult.Err(
                    SectorError.OwnedPointNotParent(invalid.id),
                )
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
                point4 = directionpoint,
                useDirection = useDirection,
                selection = normalizedSelection,
                orientation = normalizedOrientation,
                ownedPoints = ownedPoints,
                angleRadius = angleRadius,
                angleValueMode = angleValueMode,
                sourceParents = sourceParents,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val registration = board.setId(sector, CURVE_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    val dependencyPoints =
                        listOf(center, radiuspoint, anglepoint)
                    for (parent in dependencyPoints) {
                        if (parent in ownedPoints) {
                            sector.addChild(parent)
                        } else {
                            parent.addChild(sector)
                        }
                    }
                    // JSXGraph links a fourth Sector parent as an ancestor
                    // even when Type.providePoints created that Point.
                    directionpoint?.addChild(sector)
                    sector.setParents(sourceParents)
                    sector.update()
                    GMResult.Ok(sector)
                }
                is GMResult.Err -> GMResult.Err(
                    SectorError.Registration(registration.error),
                )
            }
        }

        // JSXGraph: src/element/sector.js -> createCircumcircleSector
        internal fun createCircumcircleSector(
            board: Board,
            point1: Point,
            point2: Point,
            point3: Point,
            selection: String = Arc.SELECTION_AUTO,
            orientation: String = Arc.ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Sector, SectorError> {
            val parents = listOf(point1, point2, point3)
            validateComposition(
                board = board,
                parents = parents,
                ownedPoints = ownedPoints,
                id = id,
                selection = selection,
                orientation = orientation,
            )?.let {
                return GMResult.Err(it)
            }

            val helper = when (
                val result = CircumcenterPoint.create(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    point3 = point3,
                    ownedPoints = ownedPoints,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    SectorError.CircumcenterFactory(result.error),
                )
            }
            helper.dump = false

            return when (
                val result = create(
                    board = board,
                    center = helper,
                    radiuspoint = point1,
                    anglepoint = point3,
                    directionpoint = point2,
                    useDirection = true,
                    selection = selection,
                    orientation = orientation,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> {
                    val sector = result.value
                    sector.elType = CIRCUMCIRCLE_SECTOR_ELEMENT_TYPE
                    sector.setParents(parents)
                    sector.subs[CENTER_SUB_ELEMENT] = helper
                    GMResult.Ok(sector)
                }

                is GMResult.Err -> {
                    board.removeObject(helper)
                    result
                }
            }
        }

        // JSXGraph: src/element/sector.js -> createMinorSector
        internal fun createMinorSector(
            board: Board,
            center: Point,
            radiuspoint: Point,
            anglepoint: Point,
            directionpoint: Point? = null,
            useDirection: Boolean = false,
            orientation: String = Arc.ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Sector, SectorError> =
            create(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                directionpoint = directionpoint,
                useDirection = useDirection,
                selection = Arc.SELECTION_MINOR,
                orientation = orientation,
                ownedPoints = ownedPoints,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        // JSXGraph: src/element/sector.js -> createMajorSector
        internal fun createMajorSector(
            board: Board,
            center: Point,
            radiuspoint: Point,
            anglepoint: Point,
            directionpoint: Point? = null,
            useDirection: Boolean = false,
            orientation: String = Arc.ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Sector, SectorError> =
            create(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                directionpoint = directionpoint,
                useDirection = useDirection,
                selection = Arc.SELECTION_MAJOR,
                orientation = orientation,
                ownedPoints = ownedPoints,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        // JSXGraph: src/element/sector.js -> createNonreflexAngle
        internal fun createNonreflexAngle(
            board: Board,
            first: Point,
            vertex: Point,
            third: Point,
            radius: AngleRadius = AngleRadius.Auto,
            orientation: String = Arc.ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Sector, SectorError> =
            createAngle(
                board = board,
                first = first,
                vertex = vertex,
                third = third,
                radius = radius,
                selection = Arc.SELECTION_MINOR,
                orientation = orientation,
                valueMode = AngleValueMode.NONREFLEX,
                ownedPoints = ownedPoints,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        // JSXGraph: src/element/sector.js -> createReflexAngle
        internal fun createReflexAngle(
            board: Board,
            first: Point,
            vertex: Point,
            third: Point,
            radius: AngleRadius = AngleRadius.Auto,
            orientation: String = Arc.ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Sector, SectorError> =
            createAngle(
                board = board,
                first = first,
                vertex = vertex,
                third = third,
                radius = radius,
                selection = Arc.SELECTION_MAJOR,
                orientation = orientation,
                valueMode = AngleValueMode.REFLEX,
                ownedPoints = ownedPoints,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        private fun validateComposition(
            board: Board,
            parents: List<Point>,
            ownedPoints: Set<Point>,
            id: String,
            selection: String,
            orientation: String,
        ): SectorError? {
            for ((index, parent) in parents.withIndex()) {
                validateParent(board, parent, index)?.let {
                    return it
                }
            }
            if (!ownedPoints.all(parents::contains)) {
                val invalid = ownedPoints.first { it !in parents }
                return SectorError.OwnedPointNotParent(invalid.id)
            }
            validateOptions(selection, orientation)?.let {
                return it
            }
            return if (id.isNotEmpty() && board.elementById(id) != null) {
                SectorError.Registration(BoardError.DuplicateElementId(id))
            } else {
                null
            }
        }

        private fun validateOptions(
            selection: String,
            orientation: String,
        ): SectorError? {
            if (selection.lowercase() !in SELECTIONS) {
                return SectorError.InvalidSelection(selection)
            }
            if (orientation.lowercase() !in ORIENTATIONS) {
                return SectorError.InvalidOrientation(orientation)
            }
            return null
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

        private const val CIRCUMCIRCLE_SECTOR_ELEMENT_TYPE =
            "circumcirclesector"
        private const val CENTER_SUB_ELEMENT = "center"
    }
}
