/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/slider.js -> createSlider
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.utils.JsMath
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.abs

internal data class SliderElementAttributes(
    val id: String = "",
    val name: String? = "",
    val needsRegularUpdate: Boolean? = null,
)

internal data class SliderPointAttributes(
    val id: String = "",
    val name: String? = "",
    val needsRegularUpdate: Boolean = false,
    val fixed: Boolean = true,
)

internal data class SliderAttributes(
    val id: String = "",
    val name: String? = "",
    val needsRegularUpdate: Boolean = true,
    val fixed: Boolean = false,
    val snapWidth: Double = -1.0,
    val snapValues: DoubleArray = doubleArrayOf(),
    val snapValueDistance: Double = 0.0,
    val withTicks: Boolean = true,
    val withLabel: Boolean = true,
    val digits: Int = 2,
    val precision: Int = 2,
    val suffixLabel: String? = null,
    val unitLabel: String? = null,
    val postLabel: String? = null,
    val point1: SliderPointAttributes = SliderPointAttributes(),
    val point2: SliderPointAttributes = SliderPointAttributes(),
    val baseline: SliderElementAttributes = SliderElementAttributes(),
    val highline: SliderElementAttributes = SliderElementAttributes(),
    val ticks: SliderElementAttributes = SliderElementAttributes(),
    val label: SliderElementAttributes = SliderElementAttributes(),
    val ticksAttributes: TicksAttributes = DEFAULT_TICKS_ATTRIBUTES,
) {
    internal companion object {
        val DEFAULT_TICKS_ATTRIBUTES = TicksAttributes(
            drawZero = true,
            insertTicks = true,
            minTicksDistance = 30.0,
            minorHeight = 4.0,
            majorHeight = 5.0,
            tickEndings = doubleArrayOf(0.0, 1.0),
            majorTickEndings = doubleArrayOf(0.0, 1.0),
            minorTicks = 0,
            digits = 2,
            includeBoundaries = true,
            ticksDistance = 1.0,
            drawLabels = false,
            labelOffset = doubleArrayOf(-4.0, -14.0),
        )
    }
}

internal sealed interface SliderError {
    data class InvalidCoordinateCount(
        val parent: String,
        val count: Int,
    ) : SliderError

    data class NonFiniteParent(
        val parent: String,
        val index: Int,
        val value: Double,
    ) : SliderError

    data class PointFactory(
        val role: String,
        val error: PointError,
    ) : SliderError

    data class LineFactory(
        val role: String,
        val error: LineError,
    ) : SliderError

    data class GliderFactory(
        val error: GliderError,
    ) : SliderError

    data class TextFactory(
        val error: TextError,
    ) : SliderError

    data class TicksFactory(
        val error: TicksError,
    ) : SliderError
}

/**
 * JXG.Slider's Point/Segment/Ticks/Text composition.
 *
 * Baseline pointer events, locale formatting, function-valued visual
 * properties, and browser-only label backends remain outside this slice.
 */
internal class Slider private constructor(
    board: Board,
    coordinates: DoubleArray,
    line: Line,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    fixed: Boolean,
    minimum: Double,
    maximum: Double,
    snapWidth: Double,
    snapValues: DoubleArray,
    snapValueDistance: Double,
    internal val point1: Point,
    internal val point2: Point,
    internal val baseline: Line,
    private val digits: Int,
    private val precision: Int,
    private val suffixLabel: String?,
    private val unitLabel: String?,
    private val postLabel: String?,
) : Glider(
    board = board,
    coordinates = coordinates,
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
    fixed = fixed,
    line = line,
    snapWidth = snapWidth,
    snapValues = snapValues,
    snapValueDistance = snapValueDistance,
) {
    internal lateinit var highline: Line
    internal var sliderTicks: Ticks? = null
    internal var label: Text? = null
    internal val subs = linkedMapOf<String, GeometryElement>()
    internal val inherits = mutableListOf<GeometryElement>()

    init {
        sliderMinimum = minimum
        sliderMaximum = maximum
        elType = SLIDER_ELEMENT_TYPE
    }

    // JSXGraph: src/element/slider.js -> Value.
    internal fun Value(): Double {
        val minimum = sliderMinimum ?: Double.NaN
        val maximum = sliderMaximum ?: Double.NaN
        val relativePosition = position ?: Double.NaN
        val difference = maximum - minimum
        return if (snapWidth == -1.0) {
            relativePosition * difference + minimum
        } else {
            JsMath.round(relativePosition * difference / snapWidth) *
                snapWidth +
                minimum
        }
    }

    // JSXGraph: src/element/slider.js -> setValue.
    internal fun setValue(value: Double): Slider {
        val minimum = sliderMinimum ?: Double.NaN
        val maximum = sliderMaximum ?: Double.NaN
        val difference = maximum - minimum
        position =
            if (abs(difference) > Mat.eps) {
                ((value - minimum) / difference).coerceIn(0.0, 1.0)
            } else {
                0.0
            }
        return this
    }

    // JSXGraph: src/element/slider.js -> setMin.
    internal fun setMin(value: Double): Slider {
        sliderMinimum = value
        return this
    }

    // JSXGraph: src/element/slider.js -> setMax.
    internal fun setMax(value: Double): Slider {
        sliderMaximum = value
        return this
    }

    override fun update(fromParent: Boolean): Slider {
        super.update(fromParent)
        updateLabel()
        return this
    }

    // JSXGraph: src/element/slider.js -> generated label functions.
    private fun updateLabel() {
        val text = label ?: return
        text.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(
                (point2.X() - point1.X()) * 0.05 + point2.X(),
                (point2.Y() - point1.Y()) * 0.05 + point2.Y(),
            ),
        )
        text.setText(labelText())
    }

    private fun labelText(): String {
        val prefix = suffixLabel
            ?: name.takeIf(String::isNotEmpty)?.let { "$it = " }
            ?: ""
        val effectiveDigits =
            if (digits == 2 && precision != 2) precision else digits
        return prefix +
            JsNumberFormat.fixed(Value(), effectiveDigits) +
            (unitLabel ?: "") +
            (postLabel ?: "")
    }

    // JSXGraph: src/element/slider.js -> p3.remove.
    override fun remove(): GeometryElement {
        board.removeObjects(
            listOfNotNull(
                label,
                sliderTicks,
                if (this::highline.isInitialized) highline else null,
                baseline,
                point2,
                point1,
            ),
        )
        return this
    }

    internal companion object {
        private const val SLIDER_ELEMENT_TYPE = "slider"

        // JSXGraph: src/element/slider.js -> createSlider.
        fun create(
            board: Board,
            startCoordinates: DoubleArray,
            endCoordinates: DoubleArray,
            range: DoubleArray,
            attributes: SliderAttributes = SliderAttributes(),
        ): GMResult<Slider, SliderError> {
            validateCoordinates("start", startCoordinates, 2)?.let {
                return GMResult.Err(it)
            }
            validateCoordinates("end", endCoordinates, 2)?.let {
                return GMResult.Err(it)
            }
            validateCoordinates("range", range, 3)?.let {
                return GMResult.Err(it)
            }

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
                    SliderError.PointFactory("point1", result.error),
                )
            }
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
                    board.removeObject(point1)
                    return GMResult.Err(
                        SliderError.PointFactory("point2", result.error),
                    )
                }
            }
            val baseline = when (
                val result = Line.createSegment(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    id = attributes.baseline.id,
                    name = attributes.baseline.name,
                    needsRegularUpdate =
                        attributes.baseline.needsRegularUpdate ?: false,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    board.removeObjects(listOf(point2, point1))
                    return GMResult.Err(
                        SliderError.LineFactory("baseline", result.error),
                    )
                }
            }
            baseline.updateStdform()

            val minimum = range[0]
            val start = range[1]
            val maximum = range[2]
            val snappedStart =
                if (attributes.snapWidth == -1.0) {
                    start
                } else {
                    JsMath.round(
                        (start - minimum) / attributes.snapWidth,
                    ) * attributes.snapWidth + minimum
                }
            val startX =
                point1.X() +
                    (point2.X() - point1.X()) *
                    (snappedStart - minimum) /
                    (maximum - minimum)
            val startY =
                point1.Y() +
                    (point2.Y() - point1.Y()) *
                    (snappedStart - minimum) /
                    (maximum - minimum)
            val slider = Slider(
                board = board,
                coordinates = doubleArrayOf(startX, startY),
                line = baseline,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                fixed = attributes.fixed,
                minimum = minimum,
                maximum = maximum,
                snapWidth = attributes.snapWidth,
                snapValues = attributes.snapValues.copyOf(),
                snapValueDistance = attributes.snapValueDistance,
                point1 = point1,
                point2 = point2,
                baseline = baseline,
                digits = attributes.digits,
                precision = attributes.precision,
                suffixLabel = attributes.suffixLabel,
                unitLabel = attributes.unitLabel,
                postLabel = attributes.postLabel,
            )
            when (val result = Glider.register(slider)) {
                is GMResult.Err -> {
                    board.removeObjects(listOf(baseline, point2, point1))
                    return GMResult.Err(
                        SliderError.GliderFactory(result.error),
                    )
                }
                is GMResult.Ok -> Unit
            }

            val highline = when (
                val result = Line.createSegment(
                    board = board,
                    point1 = point1,
                    point2 = slider,
                    id = attributes.highline.id,
                    name = attributes.highline.name,
                    needsRegularUpdate =
                        attributes.highline.needsRegularUpdate ?: true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    board.removeObject(slider)
                    return GMResult.Err(
                        SliderError.LineFactory("highline", result.error),
                    )
                }
            }
            slider.highline = highline

            if (attributes.withLabel) {
                val label = when (
                    val result = Text.create(
                        board = board,
                        coordinates = doubleArrayOf(
                            (point2.X() - point1.X()) * 0.05 + point2.X(),
                            (point2.Y() - point1.Y()) * 0.05 + point2.Y(),
                        ),
                        content = slider.labelText(),
                        id = attributes.label.id,
                        name = attributes.label.name,
                        needsRegularUpdate =
                            attributes.label.needsRegularUpdate ?: true,
                        parse = false,
                        digits = attributes.digits,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        board.removeObject(slider)
                        return GMResult.Err(
                            SliderError.TextFactory(result.error),
                        )
                    }
                }
                slider.label = label
                slider.inherits += label
                label.addParents(listOf(slider))
                slider.addChild(label)
            }

            if (attributes.withTicks) {
                val ticks = when (
                    val result = Ticks.create(
                        board = board,
                        parent = baseline,
                        source = TicksSource.Equidistant,
                        attributes = attributes.ticksAttributes,
                        id = attributes.ticks.id,
                        name = attributes.ticks.name,
                        needsRegularUpdate =
                            attributes.ticks.needsRegularUpdate ?: false,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        board.removeObject(slider)
                        return GMResult.Err(
                            SliderError.TicksFactory(result.error),
                        )
                    }
                }
                slider.sliderTicks = ticks
            }

            point1.dump = false
            point2.dump = false
            baseline.dump = false
            highline.dump = false
            slider.label?.dump = false
            slider.sliderTicks?.dump = false

            slider.subs["point1"] = point1
            slider.subs["point2"] = point2
            slider.subs["baseLine"] = baseline
            slider.subs["highLine"] = highline
            slider.sliderTicks?.let { slider.subs["ticks"] = it }
            slider.inherits += listOf(point1, point2, baseline, highline)
            slider.sliderTicks?.let(slider.inherits::add)
            slider.prepareUpdate().update()
            return GMResult.Ok(slider)
        }

        private fun validateCoordinates(
            parent: String,
            values: DoubleArray,
            count: Int,
        ): SliderError? {
            if (values.size != count) {
                return SliderError.InvalidCoordinateCount(
                    parent = parent,
                    count = values.size,
                )
            }
            values.forEachIndexed { index, value ->
                if (!value.isFinite()) {
                    return SliderError.NonFiniteParent(
                        parent = parent,
                        index = index,
                        value = value,
                    )
                }
            }
            return null
        }
    }
}
