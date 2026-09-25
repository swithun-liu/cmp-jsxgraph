/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/box3d.js -> createMesh3D
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.ceil
import kotlin.math.floor

internal sealed interface Mesh3DPointSource {
    data class Point(
        val point: Point3D,
    ) : Mesh3DPointSource

    data class Values(
        val values: List<Line3DCoordinateValue>,
    ) : Mesh3DPointSource

    data class Function(
        val evaluator: Line3DArrayEvaluator,
    ) : Mesh3DPointSource
}

internal sealed interface Mesh3DVectorSource {
    data class Values(
        val values: List<Line3DCoordinateValue>,
    ) : Mesh3DVectorSource

    data class Function(
        val evaluator: Line3DArrayEvaluator,
    ) : Mesh3DVectorSource
}

internal sealed interface Mesh3DError {
    data object ParentViewMismatch : Mesh3DError

    data class ParentNotRegistered(
        val id: String,
    ) : Mesh3DError

    data class InvalidCoordinateCount(
        val source: String,
        val count: Int,
    ) : Mesh3DError

    data class CoordinateEvaluation(
        val source: String,
        val coordinateIndex: Int?,
        val error: Line3DDynamicError,
    ) : Mesh3DError

    data class InvalidRangeCount(
        val rangeIndex: Int,
        val count: Int,
    ) : Mesh3DError

    data class InvalidStepWidth(
        val axis: String,
        val value: Double,
    ) : Mesh3DError

    data class InvalidStepCount(
        val axis: String,
        val value: Double,
    ) : Mesh3DError

    data class SampleLimitExceeded(
        val count: Long,
        val maximum: Int,
    ) : Mesh3DError

    data class CurveFactory(
        val error: CurveError,
    ) : Mesh3DError
}

/**
 * JSXGraph Mesh3D is an ordinary Curve with a 3D-aware data updater.
 */
internal object Mesh3D {
    // JSXGraph: src/3d/box3d.js -> createMesh3D.
    internal fun create(
        view: View3D,
        pointSource: Mesh3DPointSource,
        direction1Source: Mesh3DVectorSource,
        direction2Source: Mesh3DVectorSource,
        rangeUSource: List<Line3DCoordinateValue>,
        rangeVSource: List<Line3DCoordinateValue>,
        stepWidthU: Double = DEFAULT_STEP_WIDTH,
        stepWidthV: Double = DEFAULT_STEP_WIDTH,
        dependencies: Iterable<GeometryElement> = emptyList(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
    ): GMResult<Curve, Mesh3DError> {
        validatePoint(view, pointSource)?.let {
            return GMResult.Err(it)
        }
        validateSourceSizes(
            pointSource = pointSource,
            direction1Source = direction1Source,
            direction2Source = direction2Source,
            rangeUSource = rangeUSource,
            rangeVSource = rangeVSource,
        )?.let {
            return GMResult.Err(it)
        }
        validateStepWidth("u", stepWidthU)?.let {
            return GMResult.Err(it)
        }
        validateStepWidth("v", stepWidthV)?.let {
            return GMResult.Err(it)
        }

        val curveDefinition = CurveMesh3DDefinition()
        val updater = CurveDataUpdater {
            when (
                val result = updateData(
                    view = view,
                    pointSource = pointSource,
                    direction1Source = direction1Source,
                    direction2Source = direction2Source,
                    rangeUSource = rangeUSource,
                    rangeVSource = rangeVSource,
                    stepWidthU = stepWidthU,
                    stepWidthV = stepWidthV,
                    definition = curveDefinition,
                )
            ) {
                is GMResult.Ok -> result
                is GMResult.Err -> GMResult.Err(
                    CurveDataUpdateError.Mesh3D(result.error),
                )
            }
        }
        val initialData = when (
            val result = updateData(
                view = view,
                pointSource = pointSource,
                direction1Source = direction1Source,
                direction2Source = direction2Source,
                rangeUSource = rangeUSource,
                rangeVSource = rangeVSource,
                stepWidthU = stepWidthU,
                stepWidthV = stepWidthV,
                definition = curveDefinition,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val curve = when (
            val result = Curve.createData(
                board = view.board,
                dataX = initialData.x,
                dataY = initialData.y,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                Mesh3DError.CurveFactory(result.error),
            )
        }
        curve.setDataUpdater(
            updater = updater,
            mesh3D = curveDefinition,
        )
        val sourceDependencies = buildList {
            if (pointSource is Mesh3DPointSource.Point) {
                add(pointSource.point)
            }
            addAll(dependencies)
        }
        for (dependency in sourceDependencies.distinctBy { it.id }) {
            dependency.addChild(curve)
        }
        return GMResult.Ok(curve)
    }

    private fun updateData(
        view: View3D,
        pointSource: Mesh3DPointSource,
        direction1Source: Mesh3DVectorSource,
        direction2Source: Mesh3DVectorSource,
        rangeUSource: List<Line3DCoordinateValue>,
        rangeVSource: List<Line3DCoordinateValue>,
        stepWidthU: Double,
        stepWidthV: Double,
        definition: CurveMesh3DDefinition,
    ): GMResult<CurveDataUpdate, Mesh3DError> {
        val point = when (
            val result = evaluatePoint(pointSource)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val direction1 = when (
            val result = evaluateVector(direction1Source, "direction1")
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val direction2 = when (
            val result = evaluateVector(direction2Source, "direction2")
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val rangeU = when (
            val result = evaluateRange(rangeUSource, rangeIndex = 0)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val rangeV = when (
            val result = evaluateRange(rangeVSource, rangeIndex = 1)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        normalize(direction1)
        normalize(direction2)
        val intervalU = meshInterval(rangeU, stepWidthU)
        val intervalV = meshInterval(rangeV, stepWidthV)
        validateStepCount("u", intervalU[2])?.let {
            definition.requestedPointCount = Long.MAX_VALUE
            return GMResult.Err(it)
        }
        validateStepCount("v", intervalV[2])?.let {
            definition.requestedPointCount = Long.MAX_VALUE
            return GMResult.Err(it)
        }
        val requestedPointCount = samplePointCount(
            stepsU = intervalU[2],
            stepsV = intervalV[2],
        )
        definition.requestedPointCount = requestedPointCount
        if (requestedPointCount > Curve.MAX_SAMPLE_COUNT) {
            return GMResult.Err(
                Mesh3DError.SampleLimitExceeded(
                    count = requestedPointCount,
                    maximum = Curve.MAX_SAMPLE_COUNT,
                ),
            )
        }
        return GMResult.Ok(
            view.getMesh(
                function = { u, v ->
                    doubleArrayOf(
                        point[0] + u * direction1[0] +
                            v * direction2[0],
                        point[1] + u * direction1[1] +
                            v * direction2[1],
                        point[2] + u * direction1[2] +
                            v * direction2[2],
                    )
                },
                intervalU = intervalU,
                intervalV = intervalV,
            ),
        )
    }

    private fun evaluatePoint(
        source: Mesh3DPointSource,
    ): GMResult<DoubleArray, Mesh3DError> =
        when (source) {
            is Mesh3DPointSource.Point ->
                GMResult.Ok(source.point.coords.copyOfRange(1, 4))
            is Mesh3DPointSource.Values -> evaluateValues(
                values = source.values,
                sourceName = "point",
            )
            is Mesh3DPointSource.Function -> evaluateFunction(
                evaluator = source.evaluator,
                sourceName = "point",
            )
        }

    private fun evaluateVector(
        source: Mesh3DVectorSource,
        sourceName: String,
    ): GMResult<DoubleArray, Mesh3DError> =
        when (source) {
            is Mesh3DVectorSource.Values -> evaluateValues(
                values = source.values,
                sourceName = sourceName,
            )
            is Mesh3DVectorSource.Function -> evaluateFunction(
                evaluator = source.evaluator,
                sourceName = sourceName,
            )
        }

    private fun evaluateValues(
        values: List<Line3DCoordinateValue>,
        sourceName: String,
    ): GMResult<DoubleArray, Mesh3DError> {
        if (values.size !in setOf(3, 4)) {
            return GMResult.Err(
                Mesh3DError.InvalidCoordinateCount(
                    source = sourceName,
                    count = values.size,
                ),
            )
        }
        val evaluated = DoubleArray(values.size)
        for ((index, value) in values.withIndex()) {
            evaluated[index] = when (value) {
                is Line3DCoordinateValue.Numeric -> value.value
                is Line3DCoordinateValue.Dynamic -> when (
                    val result = value.evaluator.evaluate()
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        Mesh3DError.CoordinateEvaluation(
                            source = sourceName,
                            coordinateIndex = index,
                            error = result.error,
                        ),
                    )
                }
            }
        }
        return GMResult.Ok(affineCoordinates(evaluated))
    }

    private fun evaluateFunction(
        evaluator: Line3DArrayEvaluator,
        sourceName: String,
    ): GMResult<DoubleArray, Mesh3DError> =
        when (val result = evaluator.evaluate()) {
            is GMResult.Err -> GMResult.Err(
                Mesh3DError.CoordinateEvaluation(
                    source = sourceName,
                    coordinateIndex = null,
                    error = result.error,
                ),
            )
            is GMResult.Ok -> {
                if (result.value.size !in setOf(3, 4)) {
                    GMResult.Err(
                        Mesh3DError.InvalidCoordinateCount(
                            source = sourceName,
                            count = result.value.size,
                        ),
                    )
                } else {
                    GMResult.Ok(affineCoordinates(result.value))
                }
            }
        }

    private fun evaluateRange(
        source: List<Line3DCoordinateValue>,
        rangeIndex: Int,
    ): GMResult<DoubleArray, Mesh3DError> {
        if (source.size != 2) {
            return GMResult.Err(
                Mesh3DError.InvalidRangeCount(
                    rangeIndex = rangeIndex,
                    count = source.size,
                ),
            )
        }
        val result = DoubleArray(2)
        for ((index, value) in source.withIndex()) {
            result[index] = when (value) {
                is Line3DCoordinateValue.Numeric -> value.value
                is Line3DCoordinateValue.Dynamic -> when (
                    val evaluated = value.evaluator.evaluate()
                ) {
                    is GMResult.Ok -> evaluated.value
                    is GMResult.Err -> return GMResult.Err(
                        Mesh3DError.CoordinateEvaluation(
                            source = "range${rangeIndex + 1}",
                            coordinateIndex = index,
                            error = evaluated.error,
                        ),
                    )
                }
            }
        }
        return GMResult.Ok(result)
    }

    private fun affineCoordinates(coordinates: DoubleArray): DoubleArray =
        if (coordinates.size == 4) {
            coordinates.copyOfRange(1, 4)
        } else {
            coordinates.copyOf()
        }

    private fun normalize(vector: DoubleArray) {
        val length = Mat.norm(vector)
        for (index in vector.indices) {
            vector[index] /= length
        }
    }

    private fun meshInterval(
        range: DoubleArray,
        stepWidth: Double,
    ): DoubleArray =
        doubleArrayOf(
            ceil(range[0]),
            floor(range[1]),
            (ceil(range[1]) - floor(range[0])) / stepWidth,
        )

    internal fun requestedPointCount(
        rangeU: DoubleArray,
        rangeV: DoubleArray,
        stepWidthU: Double,
        stepWidthV: Double,
    ): Long {
        if (
            validateStepWidth("u", stepWidthU) != null ||
            validateStepWidth("v", stepWidthV) != null
        ) {
            return Long.MAX_VALUE
        }
        val intervalU = meshInterval(rangeU, stepWidthU)
        val intervalV = meshInterval(rangeV, stepWidthV)
        if (
            validateStepCount("u", intervalU[2]) != null ||
            validateStepCount("v", intervalV[2]) != null
        ) {
            return Long.MAX_VALUE
        }
        return samplePointCount(intervalU[2], intervalV[2])
    }

    private fun samplePointCount(
        stepsU: Double,
        stepsV: Double,
    ): Long {
        val countU = axisPointCount(stepsU)
        val countV = axisPointCount(stepsV)
        val firstFamily = saturatedMultiply(
            countU,
            saturatedIncrement(countV),
        )
        val secondFamily = saturatedMultiply(
            countV,
            saturatedIncrement(countU),
        )
        return saturatedAdd(firstFamily, secondFamily)
    }

    private fun axisPointCount(steps: Double): Long {
        val floored = floor(steps)
        return if (floored >= Long.MAX_VALUE.toDouble() - 1.0) {
            Long.MAX_VALUE
        } else {
            floored.toLong() + 1L
        }
    }

    private fun saturatedMultiply(
        first: Long,
        second: Long,
    ): Long =
        if (first != 0L && second > Long.MAX_VALUE / first) {
            Long.MAX_VALUE
        } else {
            first * second
        }

    private fun saturatedIncrement(value: Long): Long =
        if (value == Long.MAX_VALUE) Long.MAX_VALUE else value + 1L

    private fun saturatedAdd(
        first: Long,
        second: Long,
    ): Long =
        if (second > Long.MAX_VALUE - first) {
            Long.MAX_VALUE
        } else {
            first + second
        }

    private fun validatePoint(
        view: View3D,
        source: Mesh3DPointSource,
    ): Mesh3DError? {
        val point = (source as? Mesh3DPointSource.Point)?.point ?: return null
        return when {
            point.view !== view -> Mesh3DError.ParentViewMismatch
            view.board.elementById(point.id) !== point ->
                Mesh3DError.ParentNotRegistered(point.id)
            else -> null
        }
    }

    private fun validateSourceSizes(
        pointSource: Mesh3DPointSource,
        direction1Source: Mesh3DVectorSource,
        direction2Source: Mesh3DVectorSource,
        rangeUSource: List<Line3DCoordinateValue>,
        rangeVSource: List<Line3DCoordinateValue>,
    ): Mesh3DError? {
        val pointSize = (pointSource as? Mesh3DPointSource.Values)?.values?.size
        if (pointSize != null && pointSize !in setOf(3, 4)) {
            return Mesh3DError.InvalidCoordinateCount("point", pointSize)
        }
        val direction1Size =
            (direction1Source as? Mesh3DVectorSource.Values)?.values?.size
        if (direction1Size != null && direction1Size !in setOf(3, 4)) {
            return Mesh3DError.InvalidCoordinateCount(
                "direction1",
                direction1Size,
            )
        }
        val direction2Size =
            (direction2Source as? Mesh3DVectorSource.Values)?.values?.size
        if (direction2Size != null && direction2Size !in setOf(3, 4)) {
            return Mesh3DError.InvalidCoordinateCount(
                "direction2",
                direction2Size,
            )
        }
        if (rangeUSource.size != 2) {
            return Mesh3DError.InvalidRangeCount(0, rangeUSource.size)
        }
        if (rangeVSource.size != 2) {
            return Mesh3DError.InvalidRangeCount(1, rangeVSource.size)
        }
        return null
    }

    private fun validateStepWidth(
        axis: String,
        value: Double,
    ): Mesh3DError.InvalidStepWidth? =
        if (!value.isFinite() || value <= 0.0) {
            Mesh3DError.InvalidStepWidth(axis, value)
        } else {
            null
        }

    private fun validateStepCount(
        axis: String,
        value: Double,
    ): Mesh3DError.InvalidStepCount? =
        if (!value.isFinite() || value <= 0.0) {
            Mesh3DError.InvalidStepCount(axis, value)
        } else {
            null
        }

    private const val DEFAULT_STEP_WIDTH = 1.0
}
