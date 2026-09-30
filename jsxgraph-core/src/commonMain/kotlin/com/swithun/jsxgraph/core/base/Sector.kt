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
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

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

    data class RadiusExpressionCompile(
        val error: JessieCodeExpressionCompileError,
    ) : SectorError

    data class RadiusExpressionEvaluation(
        val error: JessieCodeRuntimeError,
    ) : SectorError

    data class RadiusFunctionEvaluation(
        val error: JessieCodeRuntimeError,
    ) : SectorError

    data class NonNumericRadius(
        val actualType: String,
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

    data class AngleDotFactory(
        val error: PointError,
    ) : SectorError

    data class InvalidAngleDisplayType(
        val attribute: String,
        val value: String,
    ) : SectorError

    data class InvalidOrthoSensitivity(
        val value: Double,
    ) : SectorError

    data class InvalidFixedAngle(
        val value: Double,
    ) : SectorError

    data class AngleTransformation(
        val error: TransformationError,
    ) : SectorError

    data class AnglePointTransformation(
        val error: CoordinateTransformationError,
    ) : SectorError
}

internal sealed interface AngleRadius {
    data object Auto : AngleRadius

    data class Fixed(
        val value: Double,
    ) : AngleRadius

    data class Expression(
        val expression: JessieCodeExpressionFunction,
    ) : AngleRadius

    data class Function(
        val source: AngleRadiusFunction,
    ) : AngleRadius
}

internal data class AngleRadiusFunction(
    val function: JessieCodeRuntimeValue.FunctionValue,
    val location: JessieCodeAstLocation,
)

internal sealed interface SectorDirection {
    data class Sign(
        val value: Double,
    ) : SectorDirection

    data class Coordinates(
        val value: DoubleArray,
    ) : SectorDirection
}

internal enum class AngleValueMode {
    DEFAULT,
    NONREFLEX,
    REFLEX,
}

internal data class AngleDisplayAttributes(
    val type: String = "sector",
    val orthoType: String = "square",
    val orthoSensitivity: Double = 1.0,
    val dotId: String = "",
    val dotName: String? = "",
    val dotNeedsRegularUpdate: Boolean = true,
)

private data class TwoLineSectorDefinition(
    val line1: Line,
    val line2: Line,
    val direction1: Double,
    val direction2: Double,
    val radius: AngleRadius,
)

/**
 * Translated three-point, direction-point, and two-line
 * JXG.Sector/JXG.Angle subset.
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
    private val angleDisplayAttributes: AngleDisplayAttributes?,
    private val sourceParents: List<GeometryElement>,
    private val twoLineDefinition: TwoLineSectorDefinition?,
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
    internal var bezierDegree: Int = 3
        private set
    internal val isAngle: Boolean
        get() = angleRadius != null
    internal val isTwoLine: Boolean
        get() = twoLineDefinition != null
    internal val line1: Line?
        get() = twoLineDefinition?.line1
    internal val line2: Line?
        get() = twoLineDefinition?.line2
    internal val usesAutoRadius: Boolean
        get() = angleRadius == AngleRadius.Auto
    internal var radiusEvaluationError: SectorError? = null
        private set
    internal var activeAngleDisplayType: String = ANGLE_TYPE_SECTOR
        private set
    internal var dot: Point? = null
        private set
    internal var dotVisible: Boolean = false
        private set
    internal var hasFixedAngle: Boolean = false
        private set
    private var fixedAngleTransformation: Transformation? = null
    private var useExternalRadiusFunction: Boolean = false

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
        if (isTwoLine) {
            updateTwoLinePoints()
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

        if (isAngle) {
            activeAngleDisplayType = effectiveAngleDisplayType()
            when (activeAngleDisplayType) {
                ANGLE_TYPE_NONE -> {
                    bezierDegree = 1
                    setData(
                        xCoordinates = doubleArrayOf(Double.NaN),
                        yCoordinates = doubleArrayOf(Double.NaN),
                    )
                }
                ANGLE_TYPE_SQUARE -> updateSquareData()
                ANGLE_TYPE_SECTOR,
                ANGLE_TYPE_SECTOR_DOT,
                -> updateSectorData()
            }
            updateAngleDot()
            return this
        }

        updateSectorData()
        return this
    }

    // JSXGraph 1.13.3: src/element/sector.js ->
    // createSector two-line updateDataArray.
    private fun updateTwoLinePoints() {
        val definition = twoLineDefinition ?: return
        val intersection = lineIntersection(
            line1 = definition.line1,
            line2 = definition.line2,
            normalize = true,
        )
        val firstRadius = definition.direction1 * Radius()
        val thirdRadius = definition.direction2 * Radius()
        val first = doubleArrayOf(
            intersection[0],
            intersection[1] +
                firstRadius * definition.line1.stdform[2],
            intersection[2] -
                firstRadius * definition.line1.stdform[1],
        )
        val third = doubleArrayOf(
            intersection[0],
            intersection[1] +
                thirdRadius * definition.line2.stdform[2],
            intersection[2] -
                thirdRadius * definition.line2.stdform[1],
        )
        point2.coords.setCoordinates(Const.COORDS_BY_USER, first)
        point1.coords.setCoordinates(Const.COORDS_BY_USER, intersection)
        point3.coords.setCoordinates(Const.COORDS_BY_USER, third)
    }

    private fun updateSectorData() {
        val sign = if (isTwoLine) 1.0 else directionSign()
        var first = point2.coords.usrCoords.copyOf()
        val vertex = point1.coords.usrCoords.copyOf()
        var third = point3.coords.usrCoords.copyOf()
        if (isAngle && !isTwoLine) {
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
        bezierDegree = 3
        setData(sector.xCoordinates, sector.yCoordinates)
    }

    // JSXGraph 1.13.3: src/element/sector.js ->
    // createAngle.updateDataArraySquare.
    private fun updateSquareData() {
        val vertex = point1.coords.usrCoords
        val radius = Radius()
        val firstDistance = point1.Dist(point2)
        val thirdDistance = point1.Dist(point3)
        val firstX =
            vertex[1] + (point2.X() - vertex[1]) * radius / firstDistance
        val firstY =
            vertex[2] + (point2.Y() - vertex[2]) * radius / firstDistance
        val thirdX =
            vertex[1] + (point3.X() - vertex[1]) * radius / thirdDistance
        val thirdY =
            vertex[2] + (point3.Y() - vertex[2]) * radius / thirdDistance
        bezierDegree = 1
        setData(
            xCoordinates = doubleArrayOf(
                vertex[1],
                firstX,
                firstX + thirdX - vertex[1],
                thirdX,
                vertex[1],
            ),
            yCoordinates = doubleArrayOf(
                vertex[2],
                firstY,
                firstY + thirdY - vertex[2],
                thirdY,
                vertex[2],
            ),
        )
    }

    private fun effectiveAngleDisplayType(): String {
        val attributes = angleDisplayAttributes
            ?: return ANGLE_TYPE_SECTOR
        var degrees = Geometry.trueAngle(
            point2.Coords(),
            point1.Coords(),
            point3.Coords(),
        )
        if (
            (selection == Arc.SELECTION_MINOR && degrees > 180.0) ||
            (selection == Arc.SELECTION_MAJOR && degrees < 180.0) ||
            (
                selection == Arc.SELECTION_AUTO &&
                    orientation == Arc.ORIENTATION_CLOCKWISE
                )
        ) {
            degrees = 360.0 - degrees
        }
        return if (
            abs(degrees - 90.0) <
            attributes.orthoSensitivity + com.swithun.jsxgraph.core.math.Mat.eps
        ) {
            attributes.orthoType
        } else {
            attributes.type
        }
    }

    // JSXGraph 1.13.3: src/element/sector.js -> createAngle.dot.
    private fun updateAngleDot() {
        val helper = dot ?: return
        dotVisible = activeAngleDisplayType == ANGLE_TYPE_SECTOR_DOT
        if (!dotVisible) {
            helper.setPositionDirectly(
                Const.COORDS_BY_USER,
                doubleArrayOf(0.0, 0.0),
            )
            return
        }
        val vertex = point1.coords.usrCoords
        val radius = Radius()
        val distance = point1.Dist(point2)
        var angle = Geometry.rad(
            point2.Coords(),
            point1.Coords(),
            point3.Coords(),
        )
        if (
            (selection == Arc.SELECTION_MINOR && angle > PI) ||
            (selection == Arc.SELECTION_MAJOR && angle < PI) ||
            (
                selection == Arc.SELECTION_AUTO &&
                    orientation == Arc.ORIENTATION_CLOCKWISE
                )
        ) {
            angle = -(2.0 * PI - angle)
        }
        angle *= 0.5
        val sourceX = (point2.X() - vertex[1]) * radius / distance
        val sourceY = (point2.Y() - vertex[2]) * radius / distance
        helper.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(
                vertex[1] +
                    0.5 * (cos(angle) * sourceX - sin(angle) * sourceY),
                vertex[2] +
                    0.5 * (sin(angle) * sourceX + cos(angle) * sourceY),
            ),
        )
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
        when (
            val radius = twoLineDefinition?.radius ?: angleRadius
        ) {
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
            is AngleRadius.Expression,
            is AngleRadius.Function,
            -> when (val result = evaluateRadius(radius, external = true)) {
                is GMResult.Ok -> {
                    radiusEvaluationError = null
                    when (val value = result.value) {
                        EvaluatedRadius.Auto -> autoRadius()
                        is EvaluatedRadius.Fixed -> value.value
                    }
                }
                is GMResult.Err -> {
                    radiusEvaluationError = result.error
                    Double.NaN
                }
            }
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
        val pointDistance =
            if (isTwoLine) {
                Double.POSITIVE_INFINITY
            } else {
                center.Dist(point2) * 0.3333
            }
        val maximum = 50.0 / board.unitX
        return max(minimum, min(pointDistance, maximum))
    }

    // JSXGraph 1.13.3: src/element/sector.js ->
    // createAngle.setAngle / free.
    internal fun setAngle(
        angle: Double,
    ): GMResult<Sector, SectorError> {
        if (!isAngle) {
            return GMResult.Err(
                SectorError.InvalidAngleDisplayType("setAngle", "sector"),
            )
        }
        if (isTwoLine) {
            return GMResult.Ok(this)
        }
        if (!angle.isFinite()) {
            return GMResult.Err(SectorError.InvalidFixedAngle(angle))
        }
        val oriented =
            if (orientation == Arc.ORIENTATION_CLOCKWISE) {
                2.0 * PI - angle
            } else {
                angle
            }
        return setAngleParameter(TransformationParameter.Numeric(oriented))
    }

    internal fun setAngle(
        evaluator: TransformationDynamicParameter,
    ): GMResult<Sector, SectorError> {
        if (!isAngle) {
            return GMResult.Err(
                SectorError.InvalidAngleDisplayType("setAngle", "sector"),
            )
        }
        if (isTwoLine) {
            return GMResult.Ok(this)
        }
        val oriented = TransformationDynamicParameter {
            when (val result = evaluator.evaluate()) {
                is GMResult.Ok -> {
                    if (!result.value.isFinite()) {
                        GMResult.Err(
                            TransformationDynamicParameterError.Rejected(
                                "Angle is not finite: ${result.value}",
                            ),
                        )
                    } else {
                        GMResult.Ok(
                            if (
                                orientation ==
                                Arc.ORIENTATION_CLOCKWISE
                            ) {
                                2.0 * PI - result.value
                            } else {
                                result.value
                            },
                        )
                    }
                }
                is GMResult.Err -> result
            }
        }
        return setAngleParameter(TransformationParameter.Dynamic(oriented))
    }

    private fun setAngleParameter(
        angle: TransformationParameter,
    ): GMResult<Sector, SectorError> {
        if (!isAngle) {
            return GMResult.Err(
                SectorError.InvalidAngleDisplayType("setAngle", "sector"),
            )
        }
        if (
            hasFixedAngle ||
            !anglepoint.isDraggable ||
            anglepoint.isFixed
        ) {
            return GMResult.Ok(this)
        }
        val transformation = when (
            val result = Transformation.createRotation(
                board = board,
                angle = angle,
                center = center,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                SectorError.AngleTransformation(result.error),
            )
        }
        val previousCoordinates = anglepoint.Coords(withZ = true)
        anglepoint.addTransform(radiuspoint, transformation)
        anglepoint.setParents(listOf(radiuspoint))
        anglepoint.isDraggable = false
        anglepoint.fullUpdate()
        anglepoint.transformationEvaluationError?.let { error ->
            anglepoint.removeTransform(transformation)
            anglepoint.setParents(emptyList())
            anglepoint.isDraggable = true
            anglepoint.setPositionDirectly(
                Const.COORDS_BY_USER,
                previousCoordinates,
            )
            return GMResult.Err(
                SectorError.AnglePointTransformation(error),
            )
        }
        fixedAngleTransformation = transformation
        hasFixedAngle = true
        board.update()
        return GMResult.Ok(this)
    }

    internal fun free(): Sector {
        if (isTwoLine) {
            return this
        }
        fixedAngleTransformation?.let(anglepoint::removeTransform)
        fixedAngleTransformation = null
        anglepoint.setParents(emptyList())
        anglepoint.isDraggable = true
        hasFixedAngle = false
        board.update()
        return this
    }

    override fun remove(): GeometryElement {
        dot?.let(board::removeObject)
        dot = null
        return this
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
        private const val ANGLE_TYPE_SECTOR = "sector"
        private const val ANGLE_TYPE_SECTOR_DOT = "sectordot"
        private const val ANGLE_TYPE_SQUARE = "square"
        private const val ANGLE_TYPE_NONE = "none"
        private val ANGLE_DISPLAY_TYPES = setOf(
            ANGLE_TYPE_SECTOR,
            ANGLE_TYPE_SECTOR_DOT,
            ANGLE_TYPE_SQUARE,
            ANGLE_TYPE_NONE,
        )
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
                angleDisplayAttributes = null,
                sourceParents = buildList {
                    add(center)
                    add(radiuspoint)
                    add(anglepoint)
                    directionpoint?.let(::add)
                },
                twoLineDefinition = null,
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
            displayAttributes: AngleDisplayAttributes =
                AngleDisplayAttributes(),
            ownedPoints: Set<Point> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Sector, SectorError> {
            val normalizedDisplay = when (
                val result = normalizeAngleDisplay(displayAttributes)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
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
                angleDisplayAttributes = normalizedDisplay,
                sourceParents = listOf(first, vertex, third),
                twoLineDefinition = null,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        }

        // JSXGraph 1.13.3: src/element/sector.js ->
        // createSector two-line branch.
        internal fun createFromLines(
            board: Board,
            line1: Line,
            line2: Line,
            direction1: SectorDirection,
            direction2: SectorDirection,
            radius: AngleRadius,
            selection: String = Arc.SELECTION_AUTO,
            orientation: String = Arc.ORIENTATION_COUNTERCLOCKWISE,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Sector, SectorError> =
            createFromLinesInternal(
                board = board,
                line1 = line1,
                line2 = line2,
                direction1 = direction1,
                direction2 = direction2,
                radius = radius,
                selection = selection,
                orientation = orientation,
                angleRadius = null,
                angleValueMode = AngleValueMode.DEFAULT,
                angleDisplayAttributes = null,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        // JSXGraph 1.13.3: src/element/sector.js ->
        // createAngle two-line branch.
        internal fun createAngleFromLines(
            board: Board,
            line1: Line,
            line2: Line,
            direction1: SectorDirection,
            direction2: SectorDirection,
            radius: AngleRadius = AngleRadius.Auto,
            selection: String = Arc.SELECTION_AUTO,
            orientation: String = Arc.ORIENTATION_COUNTERCLOCKWISE,
            valueMode: AngleValueMode = AngleValueMode.DEFAULT,
            displayAttributes: AngleDisplayAttributes =
                AngleDisplayAttributes(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Sector, SectorError> {
            val normalizedDisplay = when (
                val result = normalizeAngleDisplay(displayAttributes)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return createFromLinesInternal(
                board = board,
                line1 = line1,
                line2 = line2,
                direction1 = direction1,
                direction2 = direction2,
                radius = radius,
                selection = selection,
                orientation = orientation,
                angleRadius = radius,
                angleValueMode = valueMode,
                angleDisplayAttributes = normalizedDisplay,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        }

        private fun createFromLinesInternal(
            board: Board,
            line1: Line,
            line2: Line,
            direction1: SectorDirection,
            direction2: SectorDirection,
            radius: AngleRadius,
            selection: String,
            orientation: String,
            angleRadius: AngleRadius?,
            angleValueMode: AngleValueMode,
            angleDisplayAttributes: AngleDisplayAttributes?,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
        ): GMResult<Sector, SectorError> {
            val intersection = lineIntersection(
                line1 = line1,
                line2 = line2,
                normalize = false,
            )
            val definition = TwoLineSectorDefinition(
                line1 = line1,
                line2 = line2,
                direction1 = directionSign(
                    line = line1,
                    intersection = intersection,
                    direction = direction1,
                ),
                direction2 = directionSign(
                    line = line2,
                    intersection = intersection,
                    direction = direction2,
                ),
                radius = radius,
            )
            return createInternal(
                board = board,
                center = virtualPoint(board),
                radiuspoint = virtualPoint(board),
                anglepoint = virtualPoint(board),
                directionpoint = null,
                useDirection = false,
                selection = selection,
                orientation = orientation,
                ownedPoints = emptySet(),
                angleRadius = angleRadius,
                angleValueMode = angleValueMode,
                angleDisplayAttributes = angleDisplayAttributes,
                sourceParents = listOf(line1, line2),
                twoLineDefinition = definition,
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
            angleDisplayAttributes: AngleDisplayAttributes?,
            sourceParents: List<GeometryElement>,
            twoLineDefinition: TwoLineSectorDefinition?,
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
            val radius = twoLineDefinition?.radius ?: angleRadius
            if (radius != null) {
                when (val result = evaluateRadius(radius, external = false)) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                for ((index, dependency) in radiusDependencies(radius)
                    .withIndex()
                ) {
                    validateParent(
                        board = board,
                        element = dependency,
                        parentIndex = sourceParents.size + index,
                    )?.let {
                        return GMResult.Err(it)
                    }
                }
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
                angleDisplayAttributes = angleDisplayAttributes,
                sourceParents = sourceParents,
                twoLineDefinition = twoLineDefinition,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val registration = board.setId(sector, CURVE_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    if (twoLineDefinition == null) {
                        val dependencyPoints =
                            listOf(center, radiuspoint, anglepoint)
                        for (parent in dependencyPoints) {
                            if (parent in ownedPoints) {
                                sector.addChild(parent)
                            } else {
                                parent.addChild(sector)
                            }
                        }
                        // JSXGraph links a fourth Sector parent as an
                        // ancestor even when Type.providePoints created it.
                        directionpoint?.addChild(sector)
                    } else {
                        twoLineDefinition.line1.addChild(sector)
                        twoLineDefinition.line2.addChild(sector)
                    }
                    sector.setParents(sourceParents)
                    radius?.let { source ->
                        radiusDependencies(source).forEach { dependency ->
                            dependency.addChild(sector)
                        }
                        if (source is AngleRadius.Function) {
                            sector.useExternalRadiusFunction = true
                        }
                    }
                    if (sector.isAngle) {
                        val display = angleDisplayAttributes
                            ?: AngleDisplayAttributes()
                        val dot = when (
                            val result = Point.create(
                                board = board,
                                coordinates = doubleArrayOf(0.0, 0.0),
                                id = display.dotId,
                                name = display.dotName,
                                needsRegularUpdate =
                                    display.dotNeedsRegularUpdate,
                                fixed = true,
                            )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> {
                                board.removeObject(sector)
                                return GMResult.Err(
                                    SectorError.AngleDotFactory(
                                        result.error,
                                    ),
                                )
                            }
                        }
                        dot.dump = false
                        sector.dot = dot
                        sector.subs["dot"] = dot
                        sourceParents.forEach { parent ->
                            parent.addChild(dot)
                        }
                    }
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
            displayAttributes: AngleDisplayAttributes =
                AngleDisplayAttributes(),
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
                displayAttributes = displayAttributes,
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
            displayAttributes: AngleDisplayAttributes =
                AngleDisplayAttributes(),
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
                displayAttributes = displayAttributes,
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
            element: GeometryElement,
            parentIndex: Int,
        ): SectorError? {
            if (element.board !== board) {
                return SectorError.ParentBoardMismatch(parentIndex)
            }
            if (board.elementById(element.id) !== element) {
                return SectorError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = element.id,
                )
            }
            return null
        }

        private fun normalizeAngleDisplay(
            displayAttributes: AngleDisplayAttributes,
        ): GMResult<AngleDisplayAttributes, SectorError> {
            val normalizedType = displayAttributes.type.lowercase()
            if (normalizedType !in ANGLE_DISPLAY_TYPES) {
                return GMResult.Err(
                    SectorError.InvalidAngleDisplayType(
                        attribute = "type",
                        value = displayAttributes.type,
                    ),
                )
            }
            val normalizedOrthoType =
                displayAttributes.orthoType.lowercase()
            if (normalizedOrthoType !in ANGLE_DISPLAY_TYPES) {
                return GMResult.Err(
                    SectorError.InvalidAngleDisplayType(
                        attribute = "orthoType",
                        value = displayAttributes.orthoType,
                    ),
                )
            }
            if (
                !displayAttributes.orthoSensitivity.isFinite() ||
                displayAttributes.orthoSensitivity < 0.0
            ) {
                return GMResult.Err(
                    SectorError.InvalidOrthoSensitivity(
                        displayAttributes.orthoSensitivity,
                    ),
                )
            }
            return GMResult.Ok(
                displayAttributes.copy(
                    type = normalizedType,
                    orthoType = normalizedOrthoType,
                ),
            )
        }

        private fun virtualPoint(board: Board): Point =
            Point(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                name = "",
                needsRegularUpdate = false,
                fixed = true,
            ).also { point ->
                point.baseElement = point
                point.dump = false
                point.isDraggable = false
            }

        private fun directionSign(
            line: Line,
            intersection: DoubleArray,
            direction: SectorDirection,
        ): Double =
            when (direction) {
                is SectorDirection.Sign ->
                    if (direction.value >= 0.0) 1.0 else -1.0
                is SectorDirection.Coordinates -> {
                    val coordinates =
                        if (direction.value.size == 2) {
                            doubleArrayOf(
                                1.0,
                                direction.value[0],
                                direction.value[1],
                            )
                        } else {
                            DoubleArray(3) { index ->
                                direction.value.getOrElse(index) {
                                    Double.NaN
                                }
                            }
                        }
                    val projection = Geometry.projectPointToLine(
                        point = coordinates,
                        line = line.stdform,
                    )
                    val vector = DoubleArray(3) { index ->
                        projection[index] - intersection[index]
                    }
                    if (
                        Mat.innerProduct(
                            vector,
                            doubleArrayOf(
                                0.0,
                                line.stdform[2],
                                -line.stdform[1],
                            ),
                            3,
                        ) >= 0.0
                    ) {
                        1.0
                    } else {
                        -1.0
                    }
                }
            }

        private fun lineIntersection(
            line1: Line,
            line2: Line,
            normalize: Boolean,
        ): DoubleArray {
            var intersection =
                if (normalize) {
                    Mat.crossProduct(line1.stdform, line2.stdform)
                } else {
                    Geometry.meetLineLine(
                        line1.stdform,
                        line2.stdform,
                    )
                }
            if (
                Geometry.distance(
                    intersection,
                    doubleArrayOf(0.0, 0.0, 0.0),
                    3,
                ) < TWO_LINE_EPSILON
            ) {
                intersection = when {
                    line1.point1.Dist(line2.point1) < TWO_LINE_EPSILON ||
                        line1.point1.Dist(line2.point2) <
                        TWO_LINE_EPSILON ->
                        line1.point1.coords.usrCoords.copyOf()
                    line1.point2.Dist(line2.point1) < TWO_LINE_EPSILON ||
                        line1.point2.Dist(line2.point1) <
                        TWO_LINE_EPSILON ->
                        line1.point2.coords.usrCoords.copyOf()
                    else -> intersection
                }
            }
            if (normalize && abs(intersection[0]) > TWO_LINE_EPSILON) {
                intersection[1] /= intersection[0]
                intersection[2] /= intersection[0]
                intersection[0] /= intersection[0]
            }
            return intersection
        }

        private fun evaluateRadius(
            radius: AngleRadius,
            external: Boolean,
        ): GMResult<EvaluatedRadius, SectorError> {
            val value = when (radius) {
                AngleRadius.Auto ->
                    return GMResult.Ok(EvaluatedRadius.Auto)
                is AngleRadius.Fixed ->
                    return if (radius.value.isFinite()) {
                        GMResult.Ok(EvaluatedRadius.Fixed(radius.value))
                    } else {
                        GMResult.Err(
                            SectorError.InvalidRadius(radius.value),
                        )
                    }
                is AngleRadius.Expression ->
                    when (val result = radius.expression.evaluate()) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return GMResult.Err(
                            SectorError.RadiusExpressionEvaluation(
                                result.error,
                            ),
                        )
                    }
                is AngleRadius.Function -> {
                    val callable =
                        if (external) {
                            radius.source.function.externalCallable
                        } else {
                            radius.source.function.callable
                        }
                    when (
                        val result = callable.call(
                            arguments = emptyList(),
                            location = radius.source.location,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return GMResult.Err(
                            SectorError.RadiusFunctionEvaluation(
                                result.error,
                            ),
                        )
                    }
                }
            }
            return when (value) {
                is JessieCodeRuntimeValue.NumberValue ->
                    if (value.value.isFinite()) {
                        GMResult.Ok(EvaluatedRadius.Fixed(value.value))
                    } else {
                        GMResult.Err(
                            SectorError.InvalidRadius(value.value),
                        )
                    }
                is JessieCodeRuntimeValue.StringValue ->
                    if (value.value.lowercase() == "auto") {
                        GMResult.Ok(EvaluatedRadius.Auto)
                    } else {
                        GMResult.Err(
                            SectorError.NonNumericRadius("string"),
                        )
                    }
                else -> GMResult.Err(
                    SectorError.NonNumericRadius(runtimeType(value)),
                )
            }
        }

        private fun radiusDependencies(
            radius: AngleRadius,
        ): Collection<GeometryElement> =
            when (radius) {
                is AngleRadius.Expression ->
                    radius.expression.dependencies.values
                is AngleRadius.Function ->
                    radius.source.function.dependencies.values
                AngleRadius.Auto,
                is AngleRadius.Fixed,
                -> emptyList()
            }

        private fun runtimeType(
            value: JessieCodeRuntimeValue,
        ): String =
            when (value) {
                JessieCodeRuntimeValue.UndefinedValue -> "undefined"
                JessieCodeRuntimeValue.NullValue -> "null"
                is JessieCodeRuntimeValue.NumberValue -> "number"
                is JessieCodeRuntimeValue.BooleanValue -> "boolean"
                is JessieCodeRuntimeValue.StringValue -> "string"
                is JessieCodeRuntimeValue.ArrayValue -> "array"
                is JessieCodeRuntimeValue.ObjectValue -> "object"
                is JessieCodeRuntimeValue.FunctionValue -> "function"
                is JessieCodeRuntimeValue.BoardReference -> "board"
                is JessieCodeRuntimeValue.TransformationReference ->
                    "transformation"
                is JessieCodeRuntimeValue.CompositionReference ->
                    "composition"
                is JessieCodeRuntimeValue.ElementReference -> "element"
            }

        private sealed interface EvaluatedRadius {
            data object Auto : EvaluatedRadius

            data class Fixed(
                val value: Double,
            ) : EvaluatedRadius
        }

        private const val CIRCUMCIRCLE_SECTOR_ELEMENT_TYPE =
            "circumcirclesector"
        private const val CENTER_SUB_ELEMENT = "center"
        private const val TWO_LINE_EPSILON = 1.0e-14
    }
}
