/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/measure.js -> createTapemeasure
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.utils.JsNumberFormat

internal data class TapemeasurePointAttributes(
    val id: String = "",
    val name: String? = "",
    val needsRegularUpdate: Boolean = true,
    val fixed: Boolean = false,
)

internal data class TapemeasureElementAttributes(
    val id: String = "",
    val name: String? = null,
    val needsRegularUpdate: Boolean = true,
)

internal data class TapemeasureAttributes(
    val id: String = "",
    val name: String? = null,
    val needsRegularUpdate: Boolean = true,
    val withTicks: Boolean = true,
    val withLabel: Boolean = true,
    val point1: TapemeasurePointAttributes =
        TapemeasurePointAttributes(),
    val point2: TapemeasurePointAttributes =
        TapemeasurePointAttributes(),
    val ticks: TapemeasureElementAttributes =
        TapemeasureElementAttributes(),
    val label: TapemeasureElementAttributes =
        TapemeasureElementAttributes(),
    val labelDigits: Int = 2,
    val ticksAttributes: TicksAttributes = DEFAULT_TICKS_ATTRIBUTES,
) {
    internal companion object {
        val DEFAULT_TICKS_ATTRIBUTES = TicksAttributes(
            drawZero = true,
            insertTicks = true,
            minTicksDistance = 10.0,
            minorHeight = 8.0,
            majorHeight = 16.0,
            tickEndings = doubleArrayOf(0.0, 1.0),
            majorTickEndings = doubleArrayOf(0.0, 1.0),
            minorTicks = 4,
            ticksDistance = 0.1,
            drawLabels = false,
            labelOffset = doubleArrayOf(0.0, -10.0),
            labelAnchorX = "middle",
            labelAnchorY = "top",
        )
    }
}

internal sealed interface TapemeasureError {
    data class InvalidCoordinateCount(
        val parent: String,
        val count: Int,
    ) : TapemeasureError

    data class NonFiniteParent(
        val parent: String,
        val index: Int,
        val value: Double,
    ) : TapemeasureError

    data class PointFactory(
        val role: String,
        val error: PointError,
    ) : TapemeasureError

    data class LineFactory(
        val error: LineError,
    ) : TapemeasureError

    data class TextFactory(
        val error: TextError,
    ) : TapemeasureError

    data class TicksFactory(
        val error: TicksError,
    ) : TapemeasureError
}

/**
 * State dynamically attached to the Segment returned by createTapemeasure.
 *
 * JSXGraph extends the Segment instance directly. Kotlin keeps the same
 * ownership graph explicit so the translated update and removal hooks remain
 * localized to Line.
 */
internal class TapemeasureDefinition(
    internal val line: Line,
    internal val point1: Point,
    internal val point2: Point,
    internal val label: Text?,
    internal val tapeTicks: Ticks?,
    private val labelPrefix: String,
    private val labelDigits: Int,
) {
    internal var labelUpdateError: TextError? = null
        private set

    // JSXGraph 1.13.3: src/element/measure.js -> li.Value.
    @Suppress("FunctionName")
    internal fun Value(): Double = point1.Dist(point2)

    // JSXGraph 1.13.3: src/element/measure.js -> li.label.setText.
    internal fun updateLabel() {
        val text = label ?: return
        text.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(
                (point1.X() + point2.X()) * 0.5,
                (point1.Y() + point2.Y()) * 0.5,
            ),
        )
        when (
            val result = text.setText(
                labelPrefix + JsNumberFormat.fixed(Value(), labelDigits),
            )
        ) {
            is GMResult.Ok -> labelUpdateError = null
            is GMResult.Err -> labelUpdateError = result.error
        }
    }

    // JSXGraph 1.13.3: src/element/measure.js -> li.remove.
    internal fun remove() {
        line.board.removeObjects(
            listOfNotNull(tapeTicks, point2, point1),
        )
    }
}

internal object Tapemeasure {
    private const val ELEMENT_TYPE = "tapemeasure"

    // JSXGraph 1.13.3: src/element/measure.js -> createTapemeasure.
    internal fun create(
        board: Board,
        startCoordinates: DoubleArray,
        endCoordinates: DoubleArray,
        attributes: TapemeasureAttributes = TapemeasureAttributes(),
    ): GMResult<Line, TapemeasureError> {
        validateCoordinates("start", startCoordinates)?.let {
            return GMResult.Err(it)
        }
        validateCoordinates("end", endCoordinates)?.let {
            return GMResult.Err(it)
        }

        val created = mutableListOf<GeometryElement>()
        val point1 = when (
            val result = Point.create(
                board = board,
                coordinates = startCoordinates,
                id = attributes.point1.id,
                name = attributes.point1.name,
                needsRegularUpdate =
                    attributes.point1.needsRegularUpdate,
                fixed = attributes.point1.fixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                TapemeasureError.PointFactory("point1", result.error),
            )
        }
        created += point1

        val point2 = when (
            val result = Point.create(
                board = board,
                coordinates = endCoordinates,
                id = attributes.point2.id,
                name = attributes.point2.name,
                needsRegularUpdate =
                    attributes.point2.needsRegularUpdate,
                fixed = attributes.point2.fixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollback(board, created)
                return GMResult.Err(
                    TapemeasureError.PointFactory("point2", result.error),
                )
            }
        }
        created += point2

        val line = when (
            val result = Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollback(board, created)
                return GMResult.Err(
                    TapemeasureError.LineFactory(result.error),
                )
            }
        }
        created += line
        line.elType = ELEMENT_TYPE

        val labelPrefix = attributes.name
            ?.takeIf(String::isNotEmpty)
            ?.let { "$it = " }
            ?: ""
        val label = if (attributes.withLabel) {
            when (
                val result = Text.create(
                    board = board,
                    coordinates = midpoint(point1, point2),
                    content = labelPrefix +
                        JsNumberFormat.fixed(
                            point1.Dist(point2),
                            attributes.labelDigits,
                        ),
                    id = "${line.id}Label",
                    name = attributes.label.name,
                    needsRegularUpdate =
                        attributes.label.needsRegularUpdate,
                    parse = false,
                    digits = attributes.labelDigits,
                )
            ) {
                is GMResult.Ok -> result.value.also { text ->
                    text.elType = "label"
                    text.dump = false
                    text.screenOffset = doubleArrayOf(10.0, -10.0)
                    text.addParents(listOf(line))
                    line.addChild(text)
                    created += text
                }
                is GMResult.Err -> {
                    rollback(board, created)
                    return GMResult.Err(
                        TapemeasureError.TextFactory(result.error),
                    )
                }
            }
        } else {
            null
        }

        val tapeTicks = if (attributes.withTicks) {
            when (
                val result = Ticks.create(
                    board = board,
                    parent = line,
                    source = TicksSource.Equidistant,
                    attributes = attributes.ticksAttributes,
                    id = attributes.ticks.id,
                    name = attributes.ticks.name,
                    needsRegularUpdate =
                        attributes.ticks.needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> result.value.also { ticks ->
                    ticks.dump = false
                    created += ticks
                }
                is GMResult.Err -> {
                    rollback(board, created)
                    return GMResult.Err(
                        TapemeasureError.TicksFactory(result.error),
                    )
                }
            }
        } else {
            null
        }

        val definition = TapemeasureDefinition(
            line = line,
            point1 = point1,
            point2 = point2,
            label = label,
            tapeTicks = tapeTicks,
            labelPrefix = labelPrefix,
            labelDigits = attributes.labelDigits,
        )
        line.tapemeasureDefinition = definition
        line.subs["point1"] = point1
        line.subs["point2"] = point2
        tapeTicks?.let(line.inherits::add)
        point1.dump = false
        point2.dump = false
        definition.updateLabel()
        return GMResult.Ok(line)
    }

    private fun midpoint(
        point1: Point,
        point2: Point,
    ): DoubleArray = doubleArrayOf(
        (point1.X() + point2.X()) * 0.5,
        (point1.Y() + point2.Y()) * 0.5,
    )

    private fun validateCoordinates(
        parent: String,
        coordinates: DoubleArray,
    ): TapemeasureError? {
        if (coordinates.size != 2) {
            return TapemeasureError.InvalidCoordinateCount(
                parent = parent,
                count = coordinates.size,
            )
        }
        for ((index, value) in coordinates.withIndex()) {
            if (!value.isFinite()) {
                return TapemeasureError.NonFiniteParent(
                    parent = parent,
                    index = index,
                    value = value,
                )
            }
        }
        return null
    }

    private fun rollback(
        board: Board,
        created: List<GeometryElement>,
    ) {
        board.removeObjects(created.asReversed())
    }
}
