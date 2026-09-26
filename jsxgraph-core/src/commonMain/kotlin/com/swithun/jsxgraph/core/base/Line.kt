/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/line.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.RandomSource
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.random.Random

internal sealed interface LineError {
    data class UnsupportedAngleUnit(val unit: String) : LineError

    data class ParentBoardMismatch(val parentIndex: Int) : LineError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : LineError

    data class FixedLengthExpressionCompile(
        val error: JessieCodeExpressionCompileError,
    ) : LineError

    data class FixedLengthExpressionEvaluation(
        val error: JessieCodeRuntimeError,
    ) : LineError

    data class NonNumericFixedLengthExpression(
        val actualType: String,
    ) : LineError

    data class FixedLengthFunctionEvaluation(
        val error: JessieCodeRuntimeError,
    ) : LineError

    data class NonNumericFixedLengthFunction(
        val actualType: String,
    ) : LineError

    data class Registration(val error: BoardError) : LineError
}

internal data class LineFixedLengthFunction(
    val function: JessieCodeRuntimeValue.FunctionValue,
    val location: JessieCodeAstLocation,
)

/**
 * Initial translated slice of JXG.Line.
 *
 * This slice covers two-Point lines, dependency links, standard form,
 * coordinate-derived numeric queries, parametric coordinates, and the dynamic
 * fixed-length Segment lifecycle. The native creator registry supplies
 * coordinate, coefficient, Arrow, and construction wrappers around this
 * class. Ticks, dynamic visual mutation, and hit testing remain untranslated;
 * Compose owns the translated static Line-arrow rendering.
 */
internal open class Line internal constructor(
    board: Board,
    internal var point1: Point,
    internal var point2: Point,
    id: String = "",
    name: String? = null,
    needsRegularUpdate: Boolean = true,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_LINE,
    elementClass = Const.OBJECT_CLASS_LINE,
    needsRegularUpdate = needsRegularUpdate,
) {
    // JSXGraph: src/options.js -> line.straightFirst / straightLast;
    // src/base/line.js -> createSegment / createArrow.
    internal var straightFirst: Boolean = true
        private set
    internal var straightLast: Boolean = true
        private set
    // JSXGraph: src/base/line.js -> Line constructor / createLine.
    internal var constrained: Boolean = false
    // JSXGraph: src/base/line.js -> createTangent.
    internal var glider: Point? = null
    internal val inherits = mutableListOf<GeometryElement>()
    // JSXGraph: src/base/line.js -> createNormal Line/Point branch.
    internal var normalPoint: Point? = null
    // JSXGraph: src/base/line.js -> createTangentTo.
    internal var tangentToPoint: IntersectionPoint? = null
    internal var tangentToPolar: Line? = null
    internal val subs = linkedMapOf<String, GeometryElement>()
    // JSXGraph 1.13.3: src/base/line.js -> createAxis.
    internal var axisDefinition: AxisDefinition? = null
    internal var defaultTicks: Ticks? = null

    internal var hasFixedLength: Boolean = false
        private set
    internal var nonnegativeOnly: Boolean = false
        private set
    internal var fixedLengthEvaluationError: LineError? = null
        private set

    private var fixedLengthValue: Double? = null
    private var updateFixedLength: JessieCodeExpressionFunction? = null
    private var fixedLengthFunction: LineFixedLengthFunction? = null
    private var fixedLengthOldCoords: Array<Coords>? = null
    private var fixedLengthRandomSource: RandomSource = DEFAULT_RANDOM_SOURCE
    private var pendingFixedLengthValue: Double? = null
    private var useExternalFixedLengthFunction = false

    init {
        elType = LINE_ELEMENT_TYPE
    }

    // JSXGraph: src/base/line.js -> update
    override fun update(fromParent: Boolean): Line {
        if (!needsUpdate) {
            return this
        }
        updateAxisPosition()
        updateSegmentFixedLength()
        updateStdform()
        return this
    }

    // JSXGraph 1.13.3: src/base/line.js -> createAxis.update.
    internal fun updateAxisPosition(): Line {
        val definition = axisDefinition ?: return this
        val resolved = definition.resolve(
            currentPoint1 = point1.coords.usrCoords,
            currentPoint2 = point2.coords.usrCoords,
            boundingBox = board.getBoundingBox(),
            pixelsPerUnitX = board.unitX,
            pixelsPerUnitY = board.unitY,
        )
        point1.setPositionDirectly(
            Const.COORDS_BY_USER,
            resolved.point1,
        )
        point2.setPositionDirectly(
            Const.COORDS_BY_USER,
            resolved.point2,
        )
        defaultTicks?.needsUpdate = true
        return this
    }

    // JSXGraph: src/base/line.js -> updateSegmentFixedLength
    internal fun updateSegmentFixedLength(): Line {
        val oldCoords = fixedLengthOldCoords ?: return this
        if (!hasFixedLength) {
            return this
        }

        var distance = point1.Dist(point2)
        val newDistance = when (val result = fixedLengthResult()) {
            is GMResult.Ok -> {
                fixedLengthEvaluationError = null
                normalizeFixedLength(result.value)
            }
            is GMResult.Err -> {
                fixedLengthEvaluationError = result.error
                return this
            }
        }
        val distance1 = oldCoords[0].distance(
            Const.COORDS_BY_USER,
            point1.coords,
        )
        val distance2 = oldCoords[1].distance(
            Const.COORDS_BY_USER,
            point2.coords,
        )

        if (
            distance1 > Mat.eps ||
            distance2 > Mat.eps ||
            distance != newDistance
        ) {
            val drag1 =
                point1.isDraggable &&
                    point1.type != Const.OBJECT_TYPE_GLIDER &&
                    !point1.isFixed
            val drag2 =
                point2.isDraggable &&
                    point2.type != Const.OBJECT_TYPE_GLIDER &&
                    !point2.isFixed

            if (distance > Mat.eps) {
                if (
                    (distance1 > distance2 && drag2) ||
                    (distance1 <= distance2 && drag2 && !drag1)
                ) {
                    point2.setPositionDirectly(
                        Const.COORDS_BY_USER,
                        doubleArrayOf(
                            point1.X() +
                                (point2.X() - point1.X()) *
                                newDistance / distance,
                            point1.Y() +
                                (point2.Y() - point1.Y()) *
                                newDistance / distance,
                        ),
                    )
                    point2.fullUpdate()
                } else if (
                    (distance1 <= distance2 && drag1) ||
                    (distance1 > distance2 && drag1 && !drag2)
                ) {
                    point1.setPositionDirectly(
                        Const.COORDS_BY_USER,
                        doubleArrayOf(
                            point2.X() +
                                (point1.X() - point2.X()) *
                                newDistance / distance,
                            point2.Y() +
                                (point1.Y() - point2.Y()) *
                                newDistance / distance,
                        ),
                    )
                    point1.fullUpdate()
                }
            } else {
                val x = fixedLengthRandomSource.nextDouble() - 0.5
                val y = fixedLengthRandomSource.nextDouble() - 0.5
                distance = Mat.hypot(x, y)

                if (drag2) {
                    point2.setPositionDirectly(
                        Const.COORDS_BY_USER,
                        doubleArrayOf(
                            point1.X() + x * newDistance / distance,
                            point1.Y() + y * newDistance / distance,
                        ),
                    )
                    point2.fullUpdate()
                } else if (drag1) {
                    point1.setPositionDirectly(
                        Const.COORDS_BY_USER,
                        doubleArrayOf(
                            point2.X() + x * newDistance / distance,
                            point2.Y() + y * newDistance / distance,
                        ),
                    )
                    point1.fullUpdate()
                }
            }

            oldCoords[0].setCoordinates(
                Const.COORDS_BY_USER,
                point1.coords.usrCoords,
            )
            oldCoords[1].setCoordinates(
                Const.COORDS_BY_USER,
                point2.coords.usrCoords,
            )
        }
        return this
    }

    // JSXGraph: src/base/line.js -> setFixedLength
    internal fun setFixedLength(length: Double): Line {
        if (!hasFixedLength) {
            return this
        }
        configureFixedLength(
            value = length,
            initialValue = length,
        )
        board.update()
        return this
    }

    // JSXGraph: src/base/line.js -> setFixedLength / Type.createFunction
    internal fun setFixedLength(
        lengthExpression: String,
    ): GMResult<Line, LineError> {
        if (!hasFixedLength) {
            return GMResult.Ok(this)
        }
        val expression = when (
            val result = JessieCodeExpressionFunction.compile(
                source = lengthExpression,
                board = board,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                LineError.FixedLengthExpressionCompile(result.error),
            )
        }
        val initialValue = when (
            val result = evaluateFixedLengthExpression(expression)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        configureFixedLength(
            expression = expression,
            initialValue = initialValue,
        )
        addParentsFromJCFunctions(listOf(expression))
        board.update()
        return GMResult.Ok(this)
    }

    // JSXGraph: src/base/line.js -> setFixedLength / Type.createFunction
    internal fun setFixedLength(
        lengthFunction: JessieCodeRuntimeValue.FunctionValue,
        location: JessieCodeAstLocation,
    ): GMResult<Line, LineError> {
        if (!hasFixedLength) {
            return GMResult.Ok(this)
        }
        for (dependency in lengthFunction.dependencies.values) {
            validateParent(board, dependency, parentIndex = 2)?.let {
                return GMResult.Err(it)
            }
        }
        val source = LineFixedLengthFunction(
            function = lengthFunction,
            location = location,
        )
        val initialValue = when (
            val result = evaluateFixedLengthFunction(
                fixedLengthFunction = source,
                external = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        configureFixedLength(
            function = source,
            initialValue = initialValue,
            useExternalFunction = true,
        )
        for (dependency in lengthFunction.dependencies.values) {
            dependency.addChild(this)
        }
        board.update()
        return GMResult.Ok(this)
    }

    private fun configureFixedLength(
        value: Double? = null,
        expression: JessieCodeExpressionFunction? = null,
        function: LineFixedLengthFunction? = null,
        initialValue: Double,
        useExternalFunction: Boolean = false,
    ) {
        fixedLengthValue = value
        updateFixedLength = expression
        fixedLengthFunction = function
        pendingFixedLengthValue = initialValue
        useExternalFixedLengthFunction = useExternalFunction
        fixedLengthEvaluationError = null
    }

    private fun fixedLengthResult(): GMResult<Double, LineError> {
        pendingFixedLengthValue?.let {
            pendingFixedLengthValue = null
            return GMResult.Ok(it)
        }
        updateFixedLength?.let {
            return evaluateFixedLengthExpression(it)
        }
        fixedLengthFunction?.let {
            return evaluateFixedLengthFunction(
                fixedLengthFunction = it,
                external = useExternalFixedLengthFunction,
            )
        }
        return GMResult.Ok(fixedLengthValue ?: Double.NaN)
    }

    private fun normalizeFixedLength(value: Double): Double =
        if (nonnegativeOnly) {
            maxOf(0.0, value)
        } else {
            abs(value)
        }

    // JSXGraph: src/base/line.js -> updateStdform
    internal fun updateStdform(): Line {
        val value = Mat.crossProduct(
            point1.coords.usrCoords,
            point2.coords.usrCoords,
        )
        stdform[0] = value[0]
        stdform[1] = value[1]
        stdform[2] = value[2]
        stdform[3] = 0.0
        normalize()
        return this
    }

    internal fun configureVisibleRange(
        straightFirst: Boolean,
        straightLast: Boolean,
    ): Line {
        this.straightFirst = straightFirst
        this.straightLast = straightLast
        return this
    }

    // JSXGraph: src/base/line.js -> getRise
    internal fun getRise(): Double =
        if (abs(stdform[2]) >= Mat.eps) {
            -stdform[0] / stdform[2]
        } else {
            Double.POSITIVE_INFINITY
        }

    // JSXGraph: src/base/line.js -> Slope
    internal fun Slope(): Double =
        if (abs(stdform[2]) >= Mat.eps) {
            -stdform[1] / stdform[2]
        } else {
            Double.POSITIVE_INFINITY
        }

    // JSXGraph: src/base/line.js -> getAngle
    internal fun getAngle(): Double = atan2(-stdform[1], stdform[2])

    // JSXGraph: src/base/line.js -> getAngle
    internal fun getAngle(unit: String): GMResult<Double, LineError> {
        val radians = getAngle()
        if (unit.isEmpty()) {
            return GMResult.Ok(radians)
        }
        val normalizedUnit = unit.lowercase()

        return when {
            normalizedUnit.startsWith("rad") -> GMResult.Ok(radians)
            normalizedUnit.startsWith("deg") -> GMResult.Ok(radians * 180.0 / PI)
            normalizedUnit.startsWith("sem") -> GMResult.Ok(radians / PI)
            normalizedUnit.startsWith("cir") -> GMResult.Ok(radians * 0.5 / PI)
            else -> GMResult.Err(LineError.UnsupportedAngleUnit(unit))
        }
    }

    // JSXGraph: src/base/line.js -> Direction
    internal fun Direction(): DoubleArray {
        val coordinates1 = point1.coords.usrCoords
        val coordinates2 = point2.coords.usrCoords

        if (coordinates2[0] == 0.0 && coordinates1[0] != 0.0) {
            return coordinates2.copyOfRange(1, coordinates2.size)
        }
        if (coordinates1[0] == 0.0 && coordinates2[0] != 0.0) {
            return doubleArrayOf(-coordinates1[1], -coordinates1[2])
        }
        return doubleArrayOf(
            coordinates2[1] - coordinates1[1],
            coordinates2[2] - coordinates1[2],
        )
    }

    // JSXGraph: src/base/line.js -> isVertical
    internal fun isVertical(): Boolean {
        val direction = Direction()
        return direction[0] == 0.0 && direction[1] != 0.0
    }

    // JSXGraph: src/base/line.js -> isHorizontal
    internal fun isHorizontal(): Boolean {
        val direction = Direction()
        return direction[1] == 0.0 && direction[0] != 0.0
    }

    // JSXGraph: src/base/line.js -> X
    internal fun X(t: Double): Double {
        val coordinates1 = point1.coords.usrCoords
        val coordinates2 = point2.coords.usrCoords
        val b = stdform[2]

        return if (coordinates1[0] != 0.0) {
            if (coordinates2[0] != 0.0) {
                coordinates1[1] + (coordinates2[1] - coordinates1[1]) * t
            } else {
                coordinates1[1] + b * IDEAL_POINT_SCALE * t
            }
        } else {
            // Preserve JSXGraph's nested condition verbatim for update parity.
            if (coordinates1[0] != 0.0) {
                coordinates2[1] - (coordinates1[1] - coordinates2[1]) * t
            } else {
                coordinates2[1] + b * IDEAL_POINT_SCALE * t
            }
        }
    }

    // JSXGraph: src/base/line.js -> Y
    internal fun Y(t: Double): Double {
        val coordinates1 = point1.coords.usrCoords
        val coordinates2 = point2.coords.usrCoords
        val a = stdform[1]

        return if (coordinates1[0] != 0.0) {
            if (coordinates2[0] != 0.0) {
                coordinates1[2] + (coordinates2[2] - coordinates1[2]) * t
            } else {
                coordinates1[2] - a * IDEAL_POINT_SCALE * t
            }
        } else {
            // Preserve JSXGraph's nested condition verbatim for update parity.
            if (coordinates1[0] != 0.0) {
                coordinates2[2] - (coordinates1[2] - coordinates2[2]) * t
            } else {
                coordinates2[2] - a * IDEAL_POINT_SCALE * t
            }
        }
    }

    // JSXGraph: src/base/line.js -> Z
    internal fun Z(t: Double): Double {
        val coordinates1 = point1.coords.usrCoords
        val coordinates2 = point2.coords.usrCoords
        return if (t == 1.0 && coordinates1[0] * coordinates2[0] == 0.0) {
            0.0
        } else {
            1.0
        }
    }

    // JSXGraph: src/base/line.js -> Ft
    internal fun Ft(t: Double): DoubleArray {
        val coordinates = doubleArrayOf(Z(t), X(t), Y(t))
        coordinates[1] /= coordinates[0]
        coordinates[2] /= coordinates[0]
        coordinates[0] /= coordinates[0]
        return coordinates
    }

    // JSXGraph: src/base/line.js -> L
    internal fun L(): Double = point1.Dist(point2)

    // JSXGraph: src/base/line.js -> minX
    internal fun minX(): Double = 0.0

    // JSXGraph: src/base/line.js -> maxX
    internal fun maxX(): Double = 1.0

    // JSXGraph: src/base/line.js -> bounds
    internal fun bounds(): DoubleArray {
        val coordinates1 = point1.coords.usrCoords
        val coordinates2 = point2.coords.usrCoords
        return doubleArrayOf(
            minOf(coordinates1[1], coordinates2[1]),
            maxOf(coordinates1[2], coordinates2[2]),
            maxOf(coordinates1[1], coordinates2[1]),
            minOf(coordinates1[2], coordinates2[2]),
        )
    }

    internal companion object {
        private const val IDEAL_POINT_SCALE = 1.0e5
        private const val LINE_ID_PREFIX = "L"
        private const val LINE_ELEMENT_TYPE = "line"
        private const val SEGMENT_ELEMENT_TYPE = "segment"
        private val DEFAULT_RANDOM_SOURCE =
            RandomSource { Random.nextDouble() }

        // JSXGraph: src/base/line.js -> createLine / Line constructor
        fun create(
            board: Board,
            point1: Point,
            point2: Point,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Line, LineError> {
            validateParent(board, point1, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParent(board, point2, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }

            val line = Line(
                board = board,
                point1 = point1,
                point2 = point2,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (val registration = board.setId(line, LINE_ID_PREFIX)) {
                is GMResult.Ok -> {
                    point1.addChild(line)
                    point2.addChild(line)
                    line.inherits += point1
                    line.inherits += point2
                    line.setParents(listOf(point1, point2))
                    line.isDraggable = true
                    line.updateStdform()
                    GMResult.Ok(line)
                }

                is GMResult.Err -> GMResult.Err(
                    LineError.Registration(registration.error),
                )
            }
        }

        // JSXGraph: src/base/line.js -> createSegment
        fun createSegment(
            board: Board,
            point1: Point,
            point2: Point,
            fixedLength: Double? = null,
            nonnegativeOnly: Boolean = false,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            randomSource: RandomSource = DEFAULT_RANDOM_SOURCE,
        ): GMResult<Line, LineError> =
            when (
                val result = create(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Err -> result
                is GMResult.Ok -> {
                    val segment = result.value
                    segment.configureSegment(
                        fixedLength = fixedLength,
                        nonnegativeOnly = nonnegativeOnly,
                        randomSource = randomSource,
                    )
                    if (fixedLength != null) {
                        segment.setFixedLength(fixedLength)
                    }
                    GMResult.Ok(segment)
                }
            }

        // JSXGraph: src/base/line.js -> createSegment / setFixedLength
        fun createSegment(
            board: Board,
            point1: Point,
            point2: Point,
            fixedLengthExpression: String,
            nonnegativeOnly: Boolean = false,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            randomSource: RandomSource = DEFAULT_RANDOM_SOURCE,
        ): GMResult<Line, LineError> {
            validateParent(board, point1, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParent(board, point2, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }
            val expression = when (
                val result = JessieCodeExpressionFunction.compile(
                    source = fixedLengthExpression,
                    board = board,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    LineError.FixedLengthExpressionCompile(result.error),
                )
            }
            val initialValue = when (
                val result = evaluateFixedLengthExpression(expression)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return when (
                val result = create(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Err -> result
                is GMResult.Ok -> {
                    val segment = result.value
                    segment.configureSegment(
                        fixedLength = initialValue,
                        nonnegativeOnly = nonnegativeOnly,
                        randomSource = randomSource,
                    )
                    segment.configureFixedLength(
                        expression = expression,
                        initialValue = initialValue,
                    )
                    segment.addParentsFromJCFunctions(listOf(expression))
                    board.update()
                    GMResult.Ok(segment)
                }
            }
        }

        // JSXGraph: src/base/line.js -> createSegment / setFixedLength
        fun createSegment(
            board: Board,
            point1: Point,
            point2: Point,
            fixedLengthFunction: JessieCodeRuntimeValue.FunctionValue,
            fixedLengthFunctionLocation: JessieCodeAstLocation,
            nonnegativeOnly: Boolean = false,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            randomSource: RandomSource = DEFAULT_RANDOM_SOURCE,
        ): GMResult<Line, LineError> {
            validateParent(board, point1, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParent(board, point2, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }
            for (dependency in fixedLengthFunction.dependencies.values) {
                validateParent(board, dependency, parentIndex = 2)?.let {
                    return GMResult.Err(it)
                }
            }
            val source = LineFixedLengthFunction(
                function = fixedLengthFunction,
                location = fixedLengthFunctionLocation,
            )
            val initialValue = when (
                val result = evaluateFixedLengthFunction(
                    fixedLengthFunction = source,
                    external = false,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return when (
                val result = create(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Err -> result
                is GMResult.Ok -> {
                    val segment = result.value
                    segment.configureSegment(
                        fixedLength = initialValue,
                        nonnegativeOnly = nonnegativeOnly,
                        randomSource = randomSource,
                    )
                    segment.configureFixedLength(
                        function = source,
                        initialValue = initialValue,
                        useExternalFunction = true,
                    )
                    for (
                        dependency in
                        fixedLengthFunction.dependencies.values
                    ) {
                        dependency.addChild(segment)
                    }
                    board.update()
                    GMResult.Ok(segment)
                }
            }
        }

        private fun Line.configureSegment(
            fixedLength: Double?,
            nonnegativeOnly: Boolean,
            randomSource: RandomSource,
        ) {
            elType = SEGMENT_ELEMENT_TYPE
            configureVisibleRange(
                straightFirst = false,
                straightLast = false,
            )
            if (fixedLength == null) {
                return
            }
            hasFixedLength = true
            this.nonnegativeOnly = nonnegativeOnly
            fixedLengthRandomSource = randomSource
            fixedLengthOldCoords = arrayOf(
                Coords(
                    method = Const.COORDS_BY_USER,
                    coordinates = point1.Coords(),
                    board = board,
                ),
                Coords(
                    method = Const.COORDS_BY_USER,
                    coordinates = point2.Coords(),
                    board = board,
                ),
            )
        }

        private fun validateParent(
            board: Board,
            element: GeometryElement,
            parentIndex: Int,
        ): LineError? {
            if (element.board !== board) {
                return LineError.ParentBoardMismatch(parentIndex)
            }
            if (board.elementById(element.id) !== element) {
                return LineError.ParentNotRegistered(
                    parentIndex,
                    element.id,
                )
            }
            return null
        }

        private fun evaluateFixedLengthExpression(
            expression: JessieCodeExpressionFunction,
        ): GMResult<Double, LineError> =
            when (val result = expression.evaluate()) {
                is GMResult.Err -> GMResult.Err(
                    LineError.FixedLengthExpressionEvaluation(
                        result.error,
                    ),
                )
                is GMResult.Ok -> {
                    val value = result.value
                    if (value is JessieCodeRuntimeValue.NumberValue) {
                        GMResult.Ok(value.value)
                    } else {
                        GMResult.Err(
                            LineError.NonNumericFixedLengthExpression(
                                actualType = runtimeType(value),
                            ),
                        )
                    }
                }
            }

        private fun evaluateFixedLengthFunction(
            fixedLengthFunction: LineFixedLengthFunction,
            external: Boolean,
        ): GMResult<Double, LineError> {
            val callable =
                if (external) {
                    fixedLengthFunction.function.externalCallable
                } else {
                    fixedLengthFunction.function.callable
                }
            return when (
                val result = callable.call(
                    arguments = emptyList(),
                    location = fixedLengthFunction.location,
                )
            ) {
                is GMResult.Err -> GMResult.Err(
                    LineError.FixedLengthFunctionEvaluation(result.error),
                )
                is GMResult.Ok -> {
                    val value = result.value
                    if (value is JessieCodeRuntimeValue.NumberValue) {
                        GMResult.Ok(value.value)
                    } else {
                        GMResult.Err(
                            LineError.NonNumericFixedLengthFunction(
                                actualType = runtimeType(value),
                            ),
                        )
                    }
                }
            }
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
    }
}
