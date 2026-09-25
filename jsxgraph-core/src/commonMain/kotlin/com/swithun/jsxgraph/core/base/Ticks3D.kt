/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/ticks3d.js -> createTicks3D / updateDataArray / drawLabels
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.abs
import kotlin.math.sqrt

internal sealed interface Ticks3DPointSource {
    data class Values(
        val values: List<Line3DCoordinateValue>,
    ) : Ticks3DPointSource

    /**
     * JSXGraph's function parent returns homogeneous coordinates and
     * createTicks3D drops the leading coordinate with `slice(1)`.
     */
    data class Function(
        val evaluator: Line3DArrayEvaluator,
    ) : Ticks3DPointSource
}

internal sealed interface Ticks3DError {
    data class InvalidPointCount(
        val count: Int,
        val functionResult: Boolean,
    ) : Ticks3DError

    data class InvalidDirectionCount(
        val directionIndex: Int,
        val count: Int,
    ) : Ticks3DError

    data class InvalidTickEndingsCount(
        val count: Int,
    ) : Ticks3DError

    data class DynamicEvaluation(
        val component: String,
        val error: Line3DDynamicError,
    ) : Ticks3DError

    data class InvalidTicksDistance(
        val value: Double,
    ) : Ticks3DError

    data class TickCountLimitExceeded(
        val limit: Int,
        val requestedSize: Long,
    ) : Ticks3DError

    data class CurveFactory(
        val error: CurveError,
    ) : Ticks3DError

    data class LabelFactory(
        val labelIndex: Int,
        val error: Text3DError,
    ) : Ticks3DError
}

internal data class Ticks3DLabel(
    val coordinates3D: DoubleArray,
    val coordinates2D: DoubleArray,
    val value: Double,
)

internal class Ticks3DDefinition(
    internal val view: View3D,
    private val pointSource: Ticks3DPointSource,
    private val direction1: List<Line3DCoordinateValue>,
    internal val length: Double,
    private val direction2: List<Line3DCoordinateValue>,
    internal val ticksDistance: Double,
    internal val tickEndings: DoubleArray,
    internal val majorHeight: Double,
    internal val drawLabels: Boolean,
    private val maximumTickCount: Int,
) {
    internal var labels: List<Ticks3DLabel> = emptyList()
        private set
    internal val labelElements = mutableListOf<Text3D>()
    internal var tickBases3D: List<DoubleArray> = emptyList()
        private set
    internal var normalizedDirection2: DoubleArray = DoubleArray(3)
        private set
    private var labelsInitialized: Boolean = false

    // JSXGraph: src/3d/ticks3d.js -> updateDataArray / drawLabels.
    internal fun updateDataArray():
        GMResult<CurveDataUpdate, Ticks3DError> {
        val point = when (val result = evaluatePoint()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val firstDirection = when (
            val result = evaluateDirection(direction1, directionIndex = 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val secondDirection = when (
            val result = evaluateDirection(direction2, directionIndex = 1)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (!ticksDistance.isFinite() || ticksDistance < 0.0) {
            return GMResult.Err(
                Ticks3DError.InvalidTicksDistance(ticksDistance),
            )
        }
        if (abs(ticksDistance) < Mat.eps || length < 0.0) {
            tickBases3D = emptyList()
            normalizedDirection2 = secondDirection.copyOf()
            if (!labelsInitialized) {
                labels = emptyList()
                labelsInitialized = true
            }
            return GMResult.Ok(
                CurveDataUpdate(
                    x = DoubleArray(0),
                    y = DoubleArray(0),
                ),
            )
        }

        val requestedTickCount = Ticks3D.tickCount(
            length = length,
            step = ticksDistance,
        )
        if (requestedTickCount > maximumTickCount.toLong()) {
            return GMResult.Err(
                Ticks3DError.TickCountLimitExceeded(
                    limit = maximumTickCount,
                    requestedSize = requestedTickCount,
                ),
            )
        }

        val unitScale = sqrt(view.board.unitX * view.board.unitY)
        val normalizedMajorHeight = majorHeight / unitScale
        val startOffset = normalizedMajorHeight * -tickEndings[0]
        val endOffset = normalizedMajorHeight * tickEndings[1]
        val labelOffset =
            normalizedMajorHeight * tickEndings[1] * 2.0
        val x = mutableListOf<Double>()
        val y = mutableListOf<Double>()
        val updatedTickBases = mutableListOf<DoubleArray>()
        val updatedLabels = mutableListOf<Ticks3DLabel>()
        var position = 0.0
        var count = 0
        while (position <= length) {
            if (count >= maximumTickCount) {
                return GMResult.Err(
                    Ticks3DError.TickCountLimitExceeded(
                        limit = maximumTickCount,
                        requestedSize = count.toLong() + 1L,
                    ),
                )
            }
            val start = pointAt(
                point = point,
                direction1 = firstDirection,
                direction2 = secondDirection,
                position = position,
                offset = startOffset,
            )
            val end = pointAt(
                point = point,
                direction1 = firstDirection,
                direction2 = secondDirection,
                position = position,
                offset = endOffset,
            )
            val projectedStart = view.project3DTo2D(start)
            val projectedEnd = view.project3DTo2D(end)
            updatedTickBases += pointAt(
                point = point,
                direction1 = firstDirection,
                direction2 = secondDirection,
                position = position,
                offset = 0.0,
            )
            x += projectedStart[1]
            y += projectedStart[2]
            x += projectedEnd[1]
            y += projectedEnd[2]
            x += Double.NaN
            y += Double.NaN

            if (drawLabels && !labelsInitialized) {
                val labelPosition = pointAt(
                    point = point,
                    direction1 = firstDirection,
                    direction2 = secondDirection,
                    position = position,
                    offset = labelOffset,
                )
                updatedLabels += Ticks3DLabel(
                    coordinates3D =
                        doubleArrayOf(1.0) + labelPosition,
                    coordinates2D = view.project3DTo2D(labelPosition),
                    value = labelValue(
                        point = point,
                        direction = firstDirection,
                        position = position,
                    ),
                )
            }
            count += 1
            position += ticksDistance
        }
        tickBases3D = updatedTickBases
        normalizedDirection2 = secondDirection.copyOf()
        if (!labelsInitialized) {
            labels = updatedLabels
            labelsInitialized = true
        }
        return GMResult.Ok(
            CurveDataUpdate(
                x = x.toDoubleArray(),
                y = y.toDoubleArray(),
            ),
        )
    }

    private fun evaluatePoint(): GMResult<DoubleArray, Ticks3DError> =
        when (val source = pointSource) {
            is Ticks3DPointSource.Values -> {
                if (source.values.size != 3) {
                    GMResult.Err(
                        Ticks3DError.InvalidPointCount(
                            count = source.values.size,
                            functionResult = false,
                        ),
                    )
                } else {
                    evaluateValues(source.values, "point")
                }
            }
            is Ticks3DPointSource.Function -> when (
                val result = source.evaluator.evaluate()
            ) {
                is GMResult.Err -> GMResult.Err(
                    Ticks3DError.DynamicEvaluation(
                        component = "point",
                        error = result.error,
                    ),
                )
                is GMResult.Ok -> {
                    if (result.value.size != 4) {
                        GMResult.Err(
                            Ticks3DError.InvalidPointCount(
                                count = result.value.size,
                                functionResult = true,
                            ),
                        )
                    } else {
                        GMResult.Ok(result.value.copyOfRange(1, 4))
                    }
                }
            }
        }

    private fun evaluateDirection(
        source: List<Line3DCoordinateValue>,
        directionIndex: Int,
    ): GMResult<DoubleArray, Ticks3DError> {
        if (source.size != 3) {
            return GMResult.Err(
                Ticks3DError.InvalidDirectionCount(
                    directionIndex = directionIndex,
                    count = source.size,
                ),
            )
        }
        val values = when (
            val result = evaluateValues(
                source = source,
                component = "direction${directionIndex + 1}",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val length = Mat.norm(values, 3)
        for (index in values.indices) {
            values[index] /= length
        }
        return GMResult.Ok(values)
    }

    private fun evaluateValues(
        source: List<Line3DCoordinateValue>,
        component: String,
    ): GMResult<DoubleArray, Ticks3DError> {
        val values = DoubleArray(source.size)
        for ((index, value) in source.withIndex()) {
            values[index] = when (value) {
                is Line3DCoordinateValue.Numeric -> value.value
                is Line3DCoordinateValue.Dynamic -> when (
                    val result = value.evaluator.evaluate()
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        Ticks3DError.DynamicEvaluation(
                            component = "$component[$index]",
                            error = result.error,
                        ),
                    )
                }
            }
        }
        return GMResult.Ok(values)
    }

    private fun pointAt(
        point: DoubleArray,
        direction1: DoubleArray,
        direction2: DoubleArray,
        position: Double,
        offset: Double,
    ): DoubleArray =
        DoubleArray(3) { index ->
            point[index] +
                position * direction1[index] +
                offset * direction2[index]
        }

    private fun labelValue(
        point: DoubleArray,
        direction: DoubleArray,
        position: Double,
    ): Double {
        var value = Double.NaN
        for (index in direction.indices) {
            if (direction[index] != 0.0) {
                value = point[index] + position * direction[index]
            }
        }
        return value
    }

}

internal object Ticks3D {
    internal const val DEFAULT_MAXIMUM_TICK_COUNT = 100_000

    internal fun tickCount(
        length: Double,
        step: Double,
    ): Long {
        if (abs(step) < Mat.eps || length < 0.0) {
            return 0L
        }
        if (!length.isFinite() || !step.isFinite()) {
            return Long.MAX_VALUE
        }
        val ratio = length / step
        if (!ratio.isFinite() || ratio >= Long.MAX_VALUE.toDouble()) {
            return Long.MAX_VALUE
        }
        return ratio.toLong() + 1L
    }

    internal fun curvePointCount(
        length: Double,
        step: Double,
    ): Long {
        val ticks = tickCount(length, step)
        return if (ticks > Long.MAX_VALUE / POINTS_PER_TICK) {
            Long.MAX_VALUE
        } else {
            ticks * POINTS_PER_TICK
        }
    }

    // JSXGraph: src/3d/ticks3d.js -> createTicks3D.
    internal fun create(
        view: View3D,
        pointSource: Ticks3DPointSource,
        direction1: List<Line3DCoordinateValue>,
        length: Double,
        direction2: List<Line3DCoordinateValue>,
        ticksDistance: Double = 1.0,
        tickEndings: DoubleArray = doubleArrayOf(0.0, 1.0),
        majorHeight: Double = 10.0,
        drawLabels: Boolean = true,
        dependencies: Iterable<GeometryElement> = emptyList(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        maximumTickCount: Int = DEFAULT_MAXIMUM_TICK_COUNT,
    ): GMResult<Curve, Ticks3DError> {
        if (
            pointSource is Ticks3DPointSource.Values &&
            pointSource.values.size != 3
        ) {
            return GMResult.Err(
                Ticks3DError.InvalidPointCount(
                    count = pointSource.values.size,
                    functionResult = false,
                ),
            )
        }
        if (direction1.size != 3) {
            return GMResult.Err(
                Ticks3DError.InvalidDirectionCount(0, direction1.size),
            )
        }
        if (direction2.size != 3) {
            return GMResult.Err(
                Ticks3DError.InvalidDirectionCount(1, direction2.size),
            )
        }
        if (tickEndings.size != 2) {
            return GMResult.Err(
                Ticks3DError.InvalidTickEndingsCount(tickEndings.size),
            )
        }
        if (!ticksDistance.isFinite() || ticksDistance < 0.0) {
            return GMResult.Err(
                Ticks3DError.InvalidTicksDistance(ticksDistance),
            )
        }
        val definition = Ticks3DDefinition(
            view = view,
            pointSource = pointSource,
            direction1 = direction1,
            length = length,
            direction2 = direction2,
            ticksDistance = ticksDistance,
            tickEndings = tickEndings.copyOf(),
            majorHeight = majorHeight,
            drawLabels = drawLabels,
            maximumTickCount = maximumTickCount,
        )
        val curve = when (
            val result = Curve.createData(
                board = view.board,
                dataX = DoubleArray(0),
                dataY = DoubleArray(0),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                Ticks3DError.CurveFactory(result.error),
            )
        }
        curve.setDataUpdater(
            updater = CurveDataUpdater {
                when (val result = definition.updateDataArray()) {
                    is GMResult.Ok -> result
                    is GMResult.Err -> GMResult.Err(
                        CurveDataUpdateError.Ticks3D(result.error),
                    )
                }
            },
            ticks3D = definition,
        )
        val uniqueDependencies = dependencies.distinct()
        curve.setParents(uniqueDependencies)
        for (dependency in uniqueDependencies) {
            dependency.addChild(curve)
        }
        curve.prepareUpdate().update()
        val error = (
            curve.evaluationError as?
                CurveError.DataUpdate
            )?.error as? CurveDataUpdateError.Ticks3D
        if (error != null) {
            view.board.removeObject(curve)
            return GMResult.Err(error.error)
        }
        for ((index, label) in definition.labels.withIndex()) {
            val labelElement = when (
                val result = Text3D.create(
                    view = view,
                    coordinateSource =
                        Point3DCoordinateSource.Values(
                            label.coordinates3D
                                .copyOfRange(1, 4)
                                .map(
                                    Point3DCoordinateValue::Numeric,
                                ),
                        ),
                    content = JsNumberFormat.compact(label.value),
                    name = "",
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    view.board.removeObjects(
                        definition.labelElements.asReversed(),
                    )
                    view.board.removeObject(curve)
                    return GMResult.Err(
                        Ticks3DError.LabelFactory(
                            labelIndex = index,
                            error = result.error,
                        ),
                    )
                }
            }
            definition.labelElements += labelElement
        }
        return GMResult.Ok(curve)
    }

    private const val POINTS_PER_TICK = 3L
}
