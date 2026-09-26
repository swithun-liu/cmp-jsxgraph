/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/line.js -> createAxis
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult

internal sealed interface AxisError {
    data class LineFactory(val error: LineError) : AxisError

    data class TicksFactory(val error: TicksError) : AxisError
}

internal sealed interface AxisDistance {
    fun resolve(
        relativeTo: Double,
        pixelsToUser: Double,
    ): Double

    data class User(val value: Double) : AxisDistance {
        override fun resolve(
            relativeTo: Double,
            pixelsToUser: Double,
        ): Double = value
    }

    data class Percent(val value: Double) : AxisDistance {
        override fun resolve(
            relativeTo: Double,
            pixelsToUser: Double,
        ): Double = value * relativeTo * 0.01
    }

    data class Fraction(val value: Double) : AxisDistance {
        override fun resolve(
            relativeTo: Double,
            pixelsToUser: Double,
        ): Double = value * relativeTo
    }

    data class Pixels(val value: Double) : AxisDistance {
        override fun resolve(
            relativeTo: Double,
            pixelsToUser: Double,
        ): Double = value * pixelsToUser
    }
}

internal data class AxisAttributes(
    val position: String = "static",
    val anchor: String = "",
    val anchorDist: AxisDistance = AxisDistance.Percent(10.0),
    val ticksAutoPos: Boolean = false,
    val ticksAutoPosThreshold: AxisDistance =
        AxisDistance.Percent(5.0),
)

internal fun defaultAxisTicksAttributes(): TicksAttributes =
    TicksAttributes(
        drawZero = false,
        insertTicks = true,
        minTicksDistance = 5.0,
        minorHeight = 10.0,
        majorHeight = -1.0,
        tickEndings = doubleArrayOf(0.0, 1.0),
        majorTickEndings = doubleArrayOf(1.0, 1.0),
        minorTicks = 4,
        ticksDistance = 1.0,
        drawLabels = true,
        labelOffset = doubleArrayOf(4.0, -9.0),
    )

internal data class AxisResolvedPoints(
    val point1: DoubleArray,
    val point2: DoubleArray,
)

internal data class AxisDefinition(
    val originalPoint1: DoubleArray,
    val originalPoint2: DoubleArray,
    val attributes: AxisAttributes,
) {
    // JSXGraph 1.13.3: src/base/line.js -> createAxis.update;
    // src/base/board.js -> getPointLoc.
    fun resolve(
        currentPoint1: DoubleArray,
        currentPoint2: DoubleArray,
        boundingBox: DoubleArray,
        pixelsPerUnitX: Double,
        pixelsPerUnitY: Double,
    ): AxisResolvedPoints {
        val directionX = currentPoint2[1] - currentPoint1[1]
        val directionY = currentPoint2[2] - currentPoint1[2]
        val horizontal = directionY == 0.0 && directionX != 0.0
        val vertical = directionX == 0.0 && directionY != 0.0
        if (
            attributes.position == "static" ||
            (!horizontal && !vertical)
        ) {
            return AxisResolvedPoints(
                currentPoint1.copyOf(),
                currentPoint2.copyOf(),
            )
        }

        val anchor = attributes.anchor
        val left = "left" in anchor
        val right = "right" in anchor
        val distance = when {
            horizontal -> attributes.anchorDist.resolve(
                relativeTo = kotlin.math.abs(
                    boundingBox[1] - boundingBox[3],
                ),
                // Preserve createAxis' horizontal unitX conversion.
                pixelsToUser = 1.0 / pixelsPerUnitX,
            )
            vertical -> attributes.anchorDist.resolve(
                relativeTo = kotlin.math.abs(
                    boundingBox[0] - boundingBox[2],
                ),
                // Preserve createAxis' vertical unitY conversion.
                pixelsToUser = 1.0 / pixelsPerUnitY,
            )
            else -> 0.0
        }
        val originalLocation = pointLocation(
            position = originalPoint1,
            margin = distance,
            boundingBox = boundingBox,
        )
        var point1 = currentPoint1.copyOf()
        var point2 = currentPoint2.copyOf()

        fun restoreOriginal() {
            point1 = originalPoint1.copyOf()
            point2 = originalPoint2.copyOf()
        }

        when (attributes.position) {
            "fixed" -> {
                when {
                    horizontal &&
                        (
                            directionX > 0.0 && right ||
                                directionX < 0.0 && left
                            ) -> {
                        point1[2] = boundingBox[3] + distance
                        point2[2] = boundingBox[3] + distance
                    }
                    horizontal &&
                        (
                            directionX > 0.0 && left ||
                                directionX < 0.0 && right
                            ) -> {
                        point1[2] = boundingBox[1] - distance
                        point2[2] = boundingBox[1] - distance
                    }
                    vertical &&
                        (
                            directionY > 0.0 && left ||
                                directionY < 0.0 && right
                            ) -> {
                        point1[1] = boundingBox[0] + distance
                        point2[1] = boundingBox[0] + distance
                    }
                    vertical &&
                        (
                            directionY > 0.0 && right ||
                                directionY < 0.0 && left
                            ) -> {
                        point1[1] = boundingBox[2] - distance
                        point2[1] = boundingBox[2] - distance
                    }
                    else -> restoreOriginal()
                }
            }
            "sticky" -> {
                when {
                    horizontal &&
                        originalLocation[1] < 0 &&
                        (
                            directionX > 0.0 && right ||
                                directionX < 0.0 && left
                            ) -> {
                        point1[2] = boundingBox[3] + distance
                        point2[2] = boundingBox[3] + distance
                    }
                    horizontal &&
                        originalLocation[1] > 0 &&
                        (
                            directionX > 0.0 && left ||
                                directionX < 0.0 && right
                            ) -> {
                        point1[2] = boundingBox[1] - distance
                        point2[2] = boundingBox[1] - distance
                    }
                    vertical &&
                        originalLocation[0] < 0 &&
                        (
                            directionY > 0.0 && left ||
                                directionY < 0.0 && right
                            ) -> {
                        point1[1] = boundingBox[0] + distance
                        point2[1] = boundingBox[0] + distance
                    }
                    vertical &&
                        originalLocation[0] > 0 &&
                        (
                            directionY > 0.0 && right ||
                                directionY < 0.0 && left
                            ) -> {
                        point1[1] = boundingBox[2] - distance
                        point2[1] = boundingBox[2] - distance
                    }
                    else -> restoreOriginal()
                }
            }
        }
        return AxisResolvedPoints(point1, point2)
    }

    private fun pointLocation(
        position: DoubleArray,
        margin: Double,
        boundingBox: DoubleArray,
    ): IntArray {
        val result = IntArray(2)
        if (position[1] > boundingBox[2] - margin) {
            result[0] = 1
        }
        if (position[1] < boundingBox[0] + margin) {
            result[0] = -1
        }
        if (position[2] > boundingBox[1] - margin) {
            result[1] = 1
        }
        if (position[2] < boundingBox[3] + margin) {
            result[1] = -1
        }
        return result
    }
}

internal object Axis {
    // JSXGraph 1.13.3: src/base/line.js -> JXG.createAxis.
    fun create(
        board: Board,
        point1: Point,
        point2: Point,
        attributes: AxisAttributes = AxisAttributes(),
        ticksSource: TicksSource = TicksSource.Equidistant,
        ticksAttributes: TicksAttributes = defaultAxisTicksAttributes(),
        id: String = "",
        name: String? = "",
        needsRegularUpdate: Boolean = false,
        straightFirst: Boolean = true,
        straightLast: Boolean = true,
        ticksId: String = "",
        ticksName: String? = null,
        ticksNeedsRegularUpdate: Boolean = false,
    ): GMResult<Line, AxisError> {
        val line = when (
            val result = Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err ->
                return GMResult.Err(AxisError.LineFactory(result.error))
        }
        val previousAncestorTypes = line.ancestors.values.associateWith {
            ancestor -> ancestor.type
        }
        val point1Draggable = point1.isDraggable
        val point2Draggable = point2.isDraggable

        line.type = Const.OBJECT_TYPE_AXIS
        line.elType = "axis"
        line.isDraggable = false
        point1.isDraggable = false
        point2.isDraggable = false
        line.axisDefinition = AxisDefinition(
            originalPoint1 = point1.coords.usrCoords.copyOf(),
            originalPoint2 = point2.coords.usrCoords.copyOf(),
            attributes = attributes,
        )
        line.configureVisibleRange(
            straightFirst = straightFirst,
            straightLast = straightLast,
        )
        for (ancestor in line.ancestors.values) {
            ancestor.type = Const.OBJECT_TYPE_AXISPOINT
        }

        val ticks = when (
            val result = Ticks.create(
                board = board,
                parent = line,
                source = ticksSource,
                attributes = ticksAttributes,
                id = ticksId,
                name = ticksName,
                needsRegularUpdate = ticksNeedsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                previousAncestorTypes.forEach { (ancestor, type) ->
                    ancestor.type = type
                }
                point1.isDraggable = point1Draggable
                point2.isDraggable = point2Draggable
                board.removeObject(line)
                return GMResult.Err(AxisError.TicksFactory(result.error))
            }
        }
        ticks.dump = false
        line.defaultTicks = ticks
        line.subs["ticks"] = ticks
        line.inherits += ticks
        line.updateAxisPosition()
        line.updateStdform()
        return GMResult.Ok(line)
    }
}
