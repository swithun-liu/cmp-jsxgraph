/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/turtle.js -> Turtle, init, forward, back, right, left,
 * penUp, penDown, clean, clearScreen, setPos, setPenSize, setPenColor,
 * getPenAttribute, setHighlightPenColor, showTurtle, hideTurtle, home,
 * pushTurtle, popTurtle, lookTo, moveTo, evalAt, X, Y, Z, minX, maxX
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal sealed interface TurtleError {
    data class InvalidCoordinate(
        val coordinate: String,
        val value: Double,
    ) : TurtleError

    data class InvalidPositionCount(
        val count: Int,
    ) : TurtleError

    data object EmptyStack : TurtleError

    data class CurveFactory(
        val error: CurveError,
    ) : TurtleError

    data class PointFactory(
        val role: String,
        val error: PointError,
    ) : TurtleError

    data class LineFactory(
        val error: LineError,
    ) : TurtleError
}

internal data class TurtlePenAttributes(
    val strokeWidth: Double = 1.0,
    val strokeColor: String = "#000000",
    val highlightStrokeColor: String = "#c3d9ff",
)

/**
 * Stateful translation of JXG.Turtle.
 *
 * Like JSXGraph 1.13.3, the Turtle itself is not registered in
 * [Board.objects]. Its generated Curve, Point, and Line objects are.
 */
internal class Turtle private constructor(
    board: Board,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    initialPenAttributes: TurtlePenAttributes,
    initialArrowVisible: Boolean,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_TURTLE,
    elementClass = Const.OBJECT_CLASS_OTHER,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal var turtleIsHidden: Boolean = false
        private set
    internal var isPenDown: Boolean = true
        private set
    internal var direction: Double = DEFAULT_DIRECTION
        private set
    internal var position: DoubleArray = doubleArrayOf(0.0, 0.0)
        private set
    internal val stack = mutableListOf<DoubleArray>()
    internal val objects = mutableListOf<GeometryElement>()
    internal val subs = linkedMapOf<String, GeometryElement>()
    internal val inherits = mutableListOf<GeometryElement>()
    internal val arrowLength: Double =
        20.0 / Mat.hypot(board.unitX, board.unitY)

    internal lateinit var curve: Curve
        private set
    internal lateinit var turtle: Point
        private set
    internal lateinit var turtle2: Point
        private set
    internal lateinit var arrow: Line
        private set

    internal var arrowVisible: Boolean = initialArrowVisible
        private set
    private var arrowAttributeVisible: Boolean = initialArrowVisible
    private var penAttributes: TurtlePenAttributes = initialPenAttributes

    init {
        elType = TURTLE_ELEMENT_TYPE
    }

    // JSXGraph 1.13.3: src/base/turtle.js -> init.
    private fun initialize(
        x: Double,
        y: Double,
        requestedDirection: Double,
    ): GMResult<Turtle, TurtleError> {
        position = doubleArrayOf(x, y)
        isPenDown = true
        direction = DEFAULT_DIRECTION
        stack.clear()
        objects.clear()
        subs.clear()
        inherits.clear()

        when (val result = createPathCurve()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        turtle = when (
            val result = createHiddenPoint(
                role = "turtle",
                coordinates = position,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollbackObjects()
                return result
            }
        }
        turtle2 = when (
            val result = createHiddenPoint(
                role = "turtle2",
                coordinates = doubleArrayOf(x, y + arrowLength),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollbackObjects()
                return result
            }
        }
        arrow = when (
            val result = Line.createSegment(
                board = board,
                point1 = turtle,
                point2 = turtle2,
                name = "",
                needsRegularUpdate = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollbackObjects()
                return GMResult.Err(TurtleError.LineFactory(result.error))
            }
        }
        objects += arrow
        arrowVisible = arrowAttributeVisible
        subs["arrow"] = arrow
        inherits += arrow
        right(DEFAULT_DIRECTION - requestedDirection)
        board.update()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/turtle.js -> forward.
    internal fun forward(
        length: Double,
    ): GMResult<Turtle, TurtleError> {
        validateFinite("length", length)?.let {
            return GMResult.Err(it)
        }
        if (length == 0.0) {
            return GMResult.Ok(this)
        }
        val radians = direction * PI / 180.0
        val dx = length * cos(radians)
        val dy = length * sin(radians)
        if (!turtleIsHidden) {
            moveHeadBy(dx, dy)
        }
        if (isPenDown && curve.numberPoints >= CURVE_SPLIT_POINT_COUNT) {
            when (val result = createPathCurve()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        position[0] += dx
        position[1] += dy
        if (isPenDown) {
            curve.appendDataPoint(position[0], position[1])
        }
        board.update()
        return GMResult.Ok(this)
    }

    internal fun back(
        length: Double,
    ): GMResult<Turtle, TurtleError> = forward(-length)

    // JSXGraph 1.13.3: src/base/turtle.js -> right / left.
    internal fun right(angle: Double): Turtle {
        direction -= angle
        direction %= FULL_TURN
        if (!turtleIsHidden) {
            updateHeadDirection()
        }
        board.update()
        return this
    }

    internal fun left(angle: Double): Turtle = right(-angle)

    internal fun penUp(): Turtle {
        isPenDown = false
        return this
    }

    internal fun penDown(): GMResult<Turtle, TurtleError> {
        isPenDown = true
        return createPathCurve()
    }

    // JSXGraph 1.13.3: src/base/turtle.js -> clean.
    // The upstream forward splice intentionally leaves alternating adjacent
    // Curve entries in this.objects.
    internal fun clean(): GMResult<Turtle, TurtleError> {
        var index = 0
        while (index < objects.size) {
            val element = objects[index]
            if (element.type == Const.OBJECT_TYPE_CURVE) {
                board.removeObject(element)
                objects.removeAt(index)
            }
            index += 1
        }
        when (val result = createPathCurve()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        board.update()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/turtle.js -> clearScreen.
    internal fun clearScreen(): GMResult<Turtle, TurtleError> {
        board.removeObjects(objects)
        objects.clear()
        return initialize(
            x = 0.0,
            y = 0.0,
            requestedDirection = DEFAULT_DIRECTION,
        )
    }

    // JSXGraph 1.13.3: src/base/turtle.js -> setPos.
    internal fun setPos(
        coordinates: DoubleArray,
    ): GMResult<Turtle, TurtleError> {
        if (coordinates.size < 2) {
            return GMResult.Err(
                TurtleError.InvalidPositionCount(coordinates.size),
            )
        }
        return setPos(coordinates[0], coordinates[1])
    }

    internal fun setPos(
        x: Double,
        y: Double,
    ): GMResult<Turtle, TurtleError> {
        validateFinite("x", x)?.let {
            return GMResult.Err(it)
        }
        validateFinite("y", y)?.let {
            return GMResult.Err(it)
        }
        position = doubleArrayOf(x, y)
        if (!turtleIsHidden) {
            turtle.setPositionDirectly(
                Const.COORDS_BY_USER,
                position,
            )
            updateHeadDirection()
        }
        when (val result = createPathCurve()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        board.update()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/turtle.js -> setPenSize /
    // setPenColor / setHighlightPenColor.
    internal fun setPenSize(
        size: Double,
    ): GMResult<Turtle, TurtleError> {
        validateFinite("size", size)?.let {
            return GMResult.Err(it)
        }
        penAttributes = penAttributes.copy(strokeWidth = size)
        return createPathCurve()
    }

    internal fun setPenColor(
        color: String,
    ): GMResult<Turtle, TurtleError> {
        penAttributes = penAttributes.copy(strokeColor = color)
        return createPathCurve()
    }

    internal fun setHighlightPenColor(
        color: String,
    ): GMResult<Turtle, TurtleError> {
        penAttributes = penAttributes.copy(
            highlightStrokeColor = color,
        )
        return createPathCurve()
    }

    internal fun getPenSize(): Double =
        curve.turtlePenAttributes?.strokeWidth
            ?: penAttributes.strokeWidth

    internal fun getPenColor(): String =
        curve.turtlePenAttributes?.strokeColor
            ?: penAttributes.strokeColor

    internal fun getHighlightPenColor(): String =
        curve.turtlePenAttributes?.highlightStrokeColor
            ?: penAttributes.highlightStrokeColor

    // JSXGraph 1.13.3: src/base/turtle.js -> showTurtle / hideTurtle.
    internal fun showTurtle(): GMResult<Turtle, TurtleError> {
        turtleIsHidden = false
        arrowVisible = true
        arrowAttributeVisible = false
        return when (val result = setPos(position[0], position[1])) {
            is GMResult.Ok -> {
                board.update()
                result
            }
            is GMResult.Err -> result
        }
    }

    internal fun hideTurtle(): Turtle {
        turtleIsHidden = true
        arrowVisible = false
        arrowAttributeVisible = false
        board.update()
        return this
    }

    internal fun home(): GMResult<Turtle, TurtleError> =
        setPos(0.0, 0.0)

    internal fun pushTurtle(): Turtle {
        stack += doubleArrayOf(position[0], position[1], direction)
        return this
    }

    internal fun popTurtle(): GMResult<Turtle, TurtleError> {
        val status = stack.removeLastOrNull()
            ?: return GMResult.Err(TurtleError.EmptyStack)
        position[0] = status[0]
        position[1] = status[1]
        direction = status[2]
        return setPos(position[0], position[1])
    }

    // JSXGraph 1.13.3: src/base/turtle.js -> lookTo.
    internal fun lookTo(
        targetDirection: Double,
    ): Turtle = right(direction - targetDirection)

    internal fun lookTo(
        target: DoubleArray,
    ): GMResult<Turtle, TurtleError> {
        if (target.size < 2) {
            return GMResult.Err(
                TurtleError.InvalidPositionCount(target.size),
            )
        }
        validateFinite("target.x", target[0])?.let {
            return GMResult.Err(it)
        }
        validateFinite("target.y", target[1])?.let {
            return GMResult.Err(it)
        }
        val radians = atan2(
            target[1] - position[1],
            target[0] - position[0],
        )
        right(direction - radians * 180.0 / PI)
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/turtle.js -> moveTo.
    internal fun moveTo(
        target: DoubleArray,
    ): GMResult<Turtle, TurtleError> {
        if (target.size < 2) {
            return GMResult.Err(
                TurtleError.InvalidPositionCount(target.size),
            )
        }
        validateFinite("target.x", target[0])?.let {
            return GMResult.Err(it)
        }
        validateFinite("target.y", target[1])?.let {
            return GMResult.Err(it)
        }
        val dx = target[0] - position[0]
        val dy = target[1] - position[1]
        if (!turtleIsHidden) {
            moveHeadBy(dx, dy)
        }
        if (isPenDown && curve.numberPoints >= CURVE_SPLIT_POINT_COUNT) {
            when (val result = createPathCurve()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        position[0] = target[0]
        position[1] = target[1]
        if (isPenDown) {
            curve.appendDataPoint(position[0], position[1])
        }
        board.update()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/turtle.js -> evalAt / X / Y / Z /
    // minX / maxX.
    internal fun X(parameter: Double? = null): Double =
        if (parameter == null) {
            position[0]
        } else {
            evalAt(parameter, Curve::X, position[0])
        }

    internal fun Y(parameter: Double? = null): Double =
        if (parameter == null) {
            position[1]
        } else {
            evalAt(parameter, Curve::Y, position[1])
        }

    @Suppress("UNUSED_PARAMETER")
    internal fun Z(parameter: Double? = null): Double = 1.0

    internal fun minX(): Double = 0.0

    internal fun maxX(): Double =
        objects.filterIsInstance<Curve>()
            .sumOf(Curve::numberPoints)
            .toDouble()

    private fun evalAt(
        parameter: Double,
        coordinate: (Curve, Double) -> Double,
        fallback: Double,
    ): Double {
        var offset = 0
        for (element in objects) {
            val path = element as? Curve ?: continue
            if (
                offset <= parameter &&
                parameter < offset + path.numberPoints
            ) {
                return coordinate(path, parameter - offset)
            }
            offset += path.numberPoints
        }
        return fallback
    }

    private fun createPathCurve(): GMResult<Turtle, TurtleError> =
        when (
            val result = Curve.createData(
                board = board,
                dataX = doubleArrayOf(position[0]),
                dataY = doubleArrayOf(position[1]),
                name = "",
                needsRegularUpdate = false,
            )
        ) {
            is GMResult.Ok -> {
                curve = result.value
                curve.turtlePenAttributes = penAttributes.copy()
                objects += curve
                GMResult.Ok(this)
            }
            is GMResult.Err -> GMResult.Err(
                TurtleError.CurveFactory(result.error),
            )
        }

    private fun createHiddenPoint(
        role: String,
        coordinates: DoubleArray,
    ): GMResult<Point, TurtleError> =
        when (
            val result = Point.create(
                board = board,
                coordinates = coordinates,
                name = "",
                needsRegularUpdate = false,
                fixed = true,
            )
        ) {
            is GMResult.Ok -> {
                objects += result.value
                GMResult.Ok(result.value)
            }
            is GMResult.Err -> GMResult.Err(
                TurtleError.PointFactory(role, result.error),
            )
        }

    private fun moveHeadBy(
        dx: Double,
        dy: Double,
    ) {
        turtle.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(turtle.X() + dx, turtle.Y() + dy),
        )
        turtle2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(turtle2.X() + dx, turtle2.Y() + dy),
        )
    }

    private fun updateHeadDirection() {
        val radians = direction * PI / 180.0
        turtle2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(
                turtle.X() + arrowLength * cos(radians),
                turtle.Y() + arrowLength * sin(radians),
            ),
        )
    }

    private fun rollbackObjects() {
        board.removeObjects(objects)
        objects.clear()
    }

    private fun validateFinite(
        coordinate: String,
        value: Double,
    ): TurtleError.InvalidCoordinate? =
        if (value.isFinite()) {
            null
        } else {
            TurtleError.InvalidCoordinate(coordinate, value)
        }

    internal companion object {
        private const val TURTLE_ELEMENT_TYPE = "turtle"
        private const val DEFAULT_DIRECTION = 90.0
        private const val FULL_TURN = 360.0
        private const val CURVE_SPLIT_POINT_COUNT = 8192

        // JSXGraph 1.13.3: src/base/turtle.js -> createTurtle / Turtle.
        internal fun create(
            board: Board,
            x: Double = 0.0,
            y: Double = 0.0,
            direction: Double = DEFAULT_DIRECTION,
            penAttributes: TurtlePenAttributes = TurtlePenAttributes(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            arrowVisible: Boolean = true,
        ): GMResult<Turtle, TurtleError> {
            val coordinates = listOf(
                "x" to x,
                "y" to y,
                "direction" to direction,
                "strokeWidth" to penAttributes.strokeWidth,
            )
            for ((coordinate, value) in coordinates) {
                if (!value.isFinite()) {
                    return GMResult.Err(
                        TurtleError.InvalidCoordinate(coordinate, value),
                    )
                }
            }
            val turtle = Turtle(
                board = board,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                initialPenAttributes = penAttributes,
                initialArrowVisible = arrowVisible,
            )
            return turtle.initialize(x, y, direction)
        }
    }
}
