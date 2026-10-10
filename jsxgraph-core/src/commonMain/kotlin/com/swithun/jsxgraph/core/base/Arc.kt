/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/arc.js -> createArc, updateDataArray, Radius, Value
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.ArcOrientation
import com.swithun.jsxgraph.core.math.ArcSelection
import com.swithun.jsxgraph.core.math.Geometry
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min

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

    data class InvalidDirectionPoint(
        val useDirection: Boolean,
        val hasDirectionPoint: Boolean,
    ) : ArcError

    data class OwnedPointNotParent(
        val id: String,
    ) : ArcError

    data class MidpointFactory(
        val error: MidpointError,
    ) : ArcError

    data class CircumcenterFactory(
        val error: CircumcenterError,
    ) : ArcError

    data class Registration(
        val error: BoardError,
    ) : ArcError
}

/**
 * Translated three-point and direction-point JXG.Arc subset.
 *
 * The upstream factory creates a Curve and replaces updateDataArray with the
 * arc implementation. This Kotlin type keeps that lifecycle explicit while
 * preserving the same cubic Bezier data and dependency graph.
 */
internal class Arc private constructor(
    board: Board,
    internal val center: Point,
    radiuspoint: Point,
    anglepoint: Point,
    internal val directionpoint: Point?,
    internal val useDirection: Boolean,
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
), CurveTransformationSource {
    internal var radiuspoint: Point = radiuspoint
        private set
    internal var anglepoint: Point = anglepoint
        private set
    internal val point2: Point = radiuspoint
    internal val point3: Point = anglepoint
    internal val points = mutableListOf<Coords>()
    internal var numberPoints: Int = 0
        private set
    internal val bezierDegree: Int = 3
    override val transformationElement: GeometryElement
        get() = this
    override val transformationPoints: List<Coords>
        get() = points
    override val transformationPointCount: Int
        get() = numberPoints
    override val transformationBezierDegree: Int
        get() = bezierDegree
    internal var midpoint: MidpointPoint? = null
        private set
    internal val subs = linkedMapOf<String, GeometryElement>()
    internal override val inherits = mutableListOf<GeometryElement>()
    // JSXGraph 1.13.3: src/base/element.js -> visProp.firstarrow /
    // visProp.lastarrow; src/reader/geonext.js -> readNode arc branch.
    internal var firstArrowEnabled: Boolean = false
        private set
    internal var lastArrowEnabled: Boolean = false
        private set

    init {
        elType = ARC_ELEMENT_TYPE
        isDraggable = true
    }

    internal override fun dumpSubElements():
        Map<String, GeometryElement> = subs

    internal fun configureVisualArrows(
        firstArrow: Boolean,
        lastArrow: Boolean,
    ): Arc {
        firstArrowEnabled = firstArrow
        lastArrowEnabled = lastArrow
        return this
    }

    // JSXGraph: src/element/arc.js -> updateDataArray
    override fun update(fromParent: Boolean): Arc {
        if (!needsUpdate) {
            return this
        }
        val currentRadiuspoint = radiuspoint
        val currentAnglepoint = anglepoint
        val phi = Geometry.rad(
            currentRadiuspoint.Coords(),
            center.Coords(),
            currentAnglepoint.Coords(),
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
        updateDirectionPoints()
        val arc = Geometry.bezierArc(
            first = currentRadiuspoint.coords.usrCoords.copyOf(),
            center = center.coords.usrCoords.copyOf(),
            third = currentAnglepoint.coords.usrCoords.copyOf(),
            withLegs = false,
            sign = sign,
        )
        setData(arc.xCoordinates, arc.yCoordinates)
        updateStdform()
        updateQuadraticform()
        return this
    }

    private fun updateDirectionPoints() {
        if (!useDirection) {
            return
        }
        val direction = directionpoint ?: return
        val p0c = point2.coords.usrCoords
        val p1c = direction.coords.usrCoords
        val p2c = point3.coords.usrCoords
        val determinant =
            (p0c[1] - p2c[1]) * (p0c[2] - p1c[2]) -
                (p0c[2] - p2c[2]) * (p0c[1] - p1c[1])

        // JSXGraph: src/element/arc.js -> updateDataArray useDirection branch
        if (determinant < 0.0) {
            radiuspoint = point2
            anglepoint = point3
        } else {
            radiuspoint = point3
            anglepoint = point2
        }
    }

    // JSXGraph: src/element/arc.js -> Radius
    @Suppress("FunctionName")
    internal fun Radius(): Double = radiuspoint.Dist(center)

    // JSXGraph: src/element/arc.js -> Circle.prototype.updateStdform
    private fun updateStdform() {
        stdform[3] = 0.5
        stdform[4] = Radius()
        stdform[1] = -center.coords.usrCoords[1]
        stdform[2] = -center.coords.usrCoords[2]
        if (!stdform[4].isFinite()) {
            stdform[0] = -(
                stdform[1] * point2.coords.usrCoords[1] +
                    stdform[2] * point2.coords.usrCoords[2]
                )
        }
        normalize()
    }

    // JSXGraph: src/element/arc.js ->
    // Circle.prototype.updateQuadraticform
    private fun updateQuadraticform() {
        val centerX = center.X()
        val centerY = center.Y()
        val radius = Radius()
        quadraticform = arrayOf(
            doubleArrayOf(
                centerX * centerX + centerY * centerY - radius * radius,
                -centerX,
                -centerY,
            ),
            doubleArrayOf(-centerX, 1.0, 0.0),
            doubleArrayOf(-centerY, 0.0, 1.0),
        )
    }

    // JSXGraph: src/element/arc.js -> getRadius
    internal fun getRadius(): Double = Radius()

    // JSXGraph: src/element/arc.js -> Value
    @Suppress("FunctionName")
    internal fun Value(
        unit: String = "length",
        radiansOverride: Double? = null,
    ): Double {
        var radians = radiansOverride ?: Geometry.rad(
            radiuspoint.Coords(),
            center.Coords(),
            anglepoint.Coords(),
        ).let { value ->
            if (orientation == ORIENTATION_CLOCKWISE) {
                2.0 * PI - value
            } else {
                value
            }
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

    // JSXGraph: src/element/arc.js -> L
    @Suppress("FunctionName")
    internal fun L(): Double = Value("length")

    // Arc is an upstream Curve with cubic data-array interpolation.
    internal fun minX(): Double = 0.0

    internal fun maxX(): Double =
        (numberPoints - 1).toDouble() / bezierDegree

    // JSXGraph: src/element/arc.js -> hasPoint
    internal fun hasPoint(
        screenX: Double,
        screenY: Double,
        precision: Double = DEFAULT_HIT_PRECISION,
        hasInnerPoints: Boolean =
            evalVisProp("hasinnerpoints") == true,
    ): Boolean {
        if (hasInnerPoints) {
            return hasPointSector(screenX, screenY)
        }
        val checkPoint = Coords(
            method = Const.COORDS_BY_SCREEN,
            coordinates = doubleArrayOf(screenX, screenY),
            board = board,
            emitter = false,
        )
        val distance = center.coords.distance(
            Const.COORDS_BY_USER,
            checkPoint,
        )
        val userTolerance =
            precision / min(abs(board.unitX), abs(board.unitY))
        return abs(distance - Radius()) < userTolerance &&
            containsArcCoordinates(checkPoint.usrCoords)
    }

    // JSXGraph: src/element/arc.js -> hasPointSector
    internal fun hasPointSector(
        screenX: Double,
        screenY: Double,
    ): Boolean {
        val checkPoint = Coords(
            method = Const.COORDS_BY_SCREEN,
            coordinates = doubleArrayOf(screenX, screenY),
            board = board,
            emitter = false,
        )
        val distance = center.coords.distance(
            Const.COORDS_BY_USER,
            checkPoint,
        )
        return distance < Radius() &&
            containsArcCoordinates(checkPoint.usrCoords)
    }

    // JSXGraph: src/element/arc.js -> getTextAnchor
    internal override fun getTextAnchor(): Coords = center.coords

    // JSXGraph: src/element/arc.js -> getLabelAnchor
    internal override fun getLabelAnchor(): Coords =
        getLabelAnchor(
            position =
                (elementLabel?.evalVisProp("position") as? String)
                    ?: (evalVisProp("label.position") as? String)
                    ?: "ulft",
            distance =
                (elementLabel?.evalVisProp("distance") as? Number)
                    ?.toDouble()
                    ?: (evalVisProp("label.distance") as? Number)
                        ?.toDouble()
                    ?: 0.0,
            labelSize =
                elementLabel?.size ?: doubleArrayOf(0.0, 0.0),
        )

    internal fun getLabelAnchor(
        position: String,
        distance: Double = 0.0,
        labelSize: DoubleArray = doubleArrayOf(0.0, 0.0),
    ): Coords {
        val anchor = arcSectorLabelAnchor(
            board = board,
            center = center,
            radiusPoint = radiuspoint,
            anglePoint = anglepoint,
            selection = selection,
            orientation = orientation,
            position = position,
            distance = distance,
            labelSize = labelSize,
            points = points,
            bezierDegree = bezierDegree,
            offsetPixels = 10.0,
        )
        if ("right" !in position && "left" !in position) {
            val centerCoordinates = center.coords.usrCoords
            elementLabel?.setAttribute(
                mapOf(
                    "position" to Geometry.calcLabelQuadrant(
                        Geometry.rad(
                            doubleArrayOf(1.0, 0.0),
                            doubleArrayOf(0.0, 0.0),
                            doubleArrayOf(
                                anchor.usrCoords[1] -
                                    centerCoordinates[1],
                                anchor.usrCoords[2] -
                                    centerCoordinates[2],
                            ),
                        ),
                    ),
                ),
            )
        }
        return anchor
    }

    private fun containsArcCoordinates(
        coordinates: DoubleArray,
    ): Boolean =
        Geometry.coordsOnArc(
            radiusPoint = radiuspoint.coords.usrCoords,
            center = center.coords.usrCoords,
            anglePoint = anglepoint.coords.usrCoords,
            coordinates = coordinates,
            selection = when (selection) {
                SELECTION_MINOR -> ArcSelection.MINOR
                SELECTION_MAJOR -> ArcSelection.MAJOR
                else -> ArcSelection.AUTO
            },
            orientation =
                if (orientation == ORIENTATION_CLOCKWISE) {
                    ArcOrientation.CLOCKWISE
                } else {
                    ArcOrientation.COUNTERCLOCKWISE
                },
        )

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
        private const val DEFAULT_HIT_PRECISION = 4.0
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
            directionpoint: Point? = null,
            useDirection: Boolean = false,
            selection: String = SELECTION_AUTO,
            orientation: String = ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Arc, ArcError> {
            if (useDirection != (directionpoint != null)) {
                return GMResult.Err(
                    ArcError.InvalidDirectionPoint(
                        useDirection = useDirection,
                        hasDirectionPoint = directionpoint != null,
                    ),
                )
            }
            val parents = buildList {
                add(center)
                add(radiuspoint)
                add(anglepoint)
                directionpoint?.let(::add)
            }
            val dependencyPoints = listOf(center, radiuspoint, anglepoint)
            for ((index, parent) in parents.withIndex()) {
                validateParent(board, parent, index)?.let {
                    return GMResult.Err(it)
                }
            }
            if (!ownedPoints.all(dependencyPoints::contains)) {
                val invalid = ownedPoints.first { it !in dependencyPoints }
                return GMResult.Err(
                    ArcError.OwnedPointNotParent(invalid.id),
                )
            }
            validateOptions(selection, orientation)?.let {
                return GMResult.Err(it)
            }
            val normalizedSelection = selection.lowercase()
            val normalizedOrientation = orientation.lowercase()

            val arc = Arc(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                directionpoint = directionpoint,
                useDirection = useDirection,
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
                    // JSXGraph only links the three geometric points. The
                    // fourth point selects direction but is not a direct child
                    // dependency of the Arc.
                    for (parent in dependencyPoints) {
                        if (parent in ownedPoints) {
                            arc.addChild(parent)
                        } else {
                            parent.addChild(arc)
                        }
                    }
                    arc.setParents(parents)
                    arc.update()
                    // Upstream board.create performs another regular update
                    // after createArc's prepareUpdate().update(). Preserve that
                    // initial direction-selected path before returning.
                    if (useDirection) {
                        arc.prepareUpdate()
                        arc.update()
                    }
                    GMResult.Ok(arc)
                }
                is GMResult.Err -> GMResult.Err(
                    ArcError.Registration(registration.error),
                )
            }
        }

        // JSXGraph: src/element/arc.js -> createSemicircle
        internal fun createSemicircle(
            board: Board,
            point1: Point,
            point2: Point,
            selection: String = SELECTION_AUTO,
            orientation: String = ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Arc, ArcError> {
            val parents = listOf(point1, point2)
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
                val result = MidpointPoint.create(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    ownedPoints = ownedPoints,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    ArcError.MidpointFactory(result.error),
                )
            }
            helper.dump = false

            return when (
                val result = create(
                    board = board,
                    center = helper,
                    radiuspoint = point2,
                    anglepoint = point1,
                    selection = selection,
                    orientation = orientation,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> {
                    val arc = result.value
                    arc.elType = SEMICIRCLE_ELEMENT_TYPE
                    arc.setParents(parents)
                    arc.midpoint = helper
                    arc.subs[MIDPOINT_SUB_ELEMENT] = helper
                    arc.inherits += helper
                    GMResult.Ok(arc)
                }

                is GMResult.Err -> {
                    board.removeObject(helper)
                    result
                }
            }
        }

        // JSXGraph: src/element/arc.js -> createCircumcircleArc
        internal fun createCircumcircleArc(
            board: Board,
            point1: Point,
            point2: Point,
            point3: Point,
            selection: String = SELECTION_AUTO,
            orientation: String = ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Arc, ArcError> {
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
                    ArcError.CircumcenterFactory(result.error),
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
                    val arc = result.value
                    arc.elType = CIRCUMCIRCLE_ARC_ELEMENT_TYPE
                    arc.setParents(parents)
                    arc.subs[CENTER_SUB_ELEMENT] = helper
                    arc.inherits += helper
                    GMResult.Ok(arc)
                }

                is GMResult.Err -> {
                    board.removeObject(helper)
                    result
                }
            }
        }

        // JSXGraph: src/element/arc.js -> createMinorArc
        internal fun createMinorArc(
            board: Board,
            center: Point,
            radiuspoint: Point,
            anglepoint: Point,
            directionpoint: Point? = null,
            useDirection: Boolean = false,
            orientation: String = ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Arc, ArcError> =
            create(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                directionpoint = directionpoint,
                useDirection = useDirection,
                selection = SELECTION_MINOR,
                orientation = orientation,
                ownedPoints = ownedPoints,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        // JSXGraph: src/element/arc.js -> createMajorArc
        internal fun createMajorArc(
            board: Board,
            center: Point,
            radiuspoint: Point,
            anglepoint: Point,
            directionpoint: Point? = null,
            useDirection: Boolean = false,
            orientation: String = ORIENTATION_COUNTERCLOCKWISE,
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Arc, ArcError> =
            create(
                board = board,
                center = center,
                radiuspoint = radiuspoint,
                anglepoint = anglepoint,
                directionpoint = directionpoint,
                useDirection = useDirection,
                selection = SELECTION_MAJOR,
                orientation = orientation,
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
        ): ArcError? {
            for ((index, parent) in parents.withIndex()) {
                validateParent(board, parent, index)?.let {
                    return it
                }
            }
            if (!ownedPoints.all(parents::contains)) {
                val invalid = ownedPoints.first { it !in parents }
                return ArcError.OwnedPointNotParent(invalid.id)
            }
            validateOptions(selection, orientation)?.let {
                return it
            }
            return if (id.isNotEmpty() && board.elementById(id) != null) {
                ArcError.Registration(BoardError.DuplicateElementId(id))
            } else {
                null
            }
        }

        private fun validateOptions(
            selection: String,
            orientation: String,
        ): ArcError? {
            if (selection.lowercase() !in SELECTIONS) {
                return ArcError.InvalidSelection(selection)
            }
            if (orientation.lowercase() !in ORIENTATIONS) {
                return ArcError.InvalidOrientation(orientation)
            }
            return null
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

        private const val SEMICIRCLE_ELEMENT_TYPE = "semicircle"
        private const val CIRCUMCIRCLE_ARC_ELEMENT_TYPE = "circumcirclearc"
        private const val MIDPOINT_SUB_ELEMENT = "midpoint"
        private const val CENTER_SUB_ELEMENT = "center"
    }
}
