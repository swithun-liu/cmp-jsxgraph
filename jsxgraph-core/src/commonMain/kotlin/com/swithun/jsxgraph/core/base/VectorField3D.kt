/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/curve3d.js -> createVectorfield3D
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

internal sealed interface Curve3DVectorFieldFunction {
    fun evaluate(
        x: Double,
        y: Double,
        z: Double,
    ): GMResult<DoubleArray, CurveError>
}

internal class Curve3DVectorFieldComponentFunction(
    private val xTerm: JessieCodeCoordinateFunction,
    private val yTerm: JessieCodeCoordinateFunction,
    private val zTerm: JessieCodeCoordinateFunction,
) : Curve3DVectorFieldFunction {
    override fun evaluate(
        x: Double,
        y: Double,
        z: Double,
    ): GMResult<DoubleArray, CurveError> {
        val arguments = listOf(
            JessieCodeRuntimeValue.NumberValue(x),
            JessieCodeRuntimeValue.NumberValue(y),
            JessieCodeRuntimeValue.NumberValue(z),
        )
        val coordinates = listOf(xTerm, yTerm, zTerm)
        val values = DoubleArray(3)
        for ((index, term) in coordinates.withIndex()) {
            when (
                val result = evaluateVectorField3DNumber(
                    termName = "vectorfield3d.F[$index]",
                    term = term,
                    arguments = arguments,
                )
            ) {
                is GMResult.Ok -> values[index] = result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(values)
    }
}

internal class Curve3DVectorFieldArrayFunction(
    private val term: JessieCodeCoordinateFunction,
) : Curve3DVectorFieldFunction {
    override fun evaluate(
        x: Double,
        y: Double,
        z: Double,
    ): GMResult<DoubleArray, CurveError> {
        val result = when (
            val evaluated = term.evaluate(
                listOf(
                    JessieCodeRuntimeValue.NumberValue(x),
                    JessieCodeRuntimeValue.NumberValue(y),
                    JessieCodeRuntimeValue.NumberValue(z),
                ),
            )
        ) {
            is GMResult.Ok -> evaluated.value
            is GMResult.Err -> return GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = "vectorfield3d.F",
                    error = evaluated.error,
                ),
            )
        }
        val values = (
            result as? JessieCodeRuntimeValue.ArrayValue
            )?.values
        if (values == null || values.size < 3) {
            return GMResult.Err(
                CurveError.NonNumericExpression(
                    term = "vectorfield3d.F",
                    actualType = vectorField3DRuntimeType(result),
                ),
            )
        }
        val coordinates = DoubleArray(3)
        for (index in coordinates.indices) {
            val value = values[index] as?
                JessieCodeRuntimeValue.NumberValue
                ?: return GMResult.Err(
                    CurveError.NonNumericExpression(
                        term = "vectorfield3d.F[$index]",
                        actualType =
                            vectorField3DRuntimeType(values[index]),
                    ),
                )
            coordinates[index] = value.value
        }
        return GMResult.Ok(coordinates)
    }
}

internal data class Curve3DVectorFieldVectorSnapshot(
    val start: DoubleArray,
    val vector: DoubleArray,
    val scaledNorm: Double,
)

internal data class Curve3DVectorFieldSnapshot(
    val vectors: List<Curve3DVectorFieldVectorSnapshot>,
    val arrowEnabled: Boolean,
    val arrowSize: Double,
    val arrowAngle: Double,
)

internal data class Curve3DVectorFieldDefinition(
    val field: Curve3DVectorFieldFunction,
    val xData: List<JessieCodeCoordinateFunction>,
    val yData: List<JessieCodeCoordinateFunction>,
    val zData: List<JessieCodeCoordinateFunction>,
    val scaleTerm: JessieCodeCoordinateFunction,
    val arrowEnabledTerm: JessieCodeCoordinateFunction,
    val arrowSizeTerm: JessieCodeCoordinateFunction,
    val arrowAngleTerm: JessieCodeCoordinateFunction,
    var requestedPointCount: Long = 0L,
    var snapshot: Curve3DVectorFieldSnapshot? = null,
)

internal data class Curve3DVectorFieldUpdate(
    val points: List<DoubleArray>,
    val requestedPointCount: Long,
    val snapshot: Curve3DVectorFieldSnapshot,
)

// JSXGraph 1.13.3:
// src/3d/curve3d.js -> createVectorfield3D.updateDataArray.
internal fun updateVectorField3D(
    definition: Curve3DVectorFieldDefinition,
    board: Board,
): GMResult<Curve3DVectorFieldUpdate, CurveError> {
    val meshes = listOf(
        "x" to definition.xData,
        "y" to definition.yData,
        "z" to definition.zData,
    )
    val evaluatedMeshes = mutableListOf<DoubleArray>()
    for ((axis, terms) in meshes) {
        val values = DoubleArray(3)
        for (index in values.indices) {
            when (
                val result = evaluateVectorField3DNumber(
                    termName = "vectorfield3d.${axis}Data[$index]",
                    term = terms[index],
                )
            ) {
                is GMResult.Ok -> values[index] = result.value
                is GMResult.Err -> return result
            }
        }
        evaluatedMeshes += values
    }
    val scale = when (
        val result = evaluateVectorField3DNumber(
            termName = "vectorfield3d.scale",
            term = definition.scaleTerm,
        )
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> return result
    }
    val arrowEnabled = when (
        val result = definition.arrowEnabledTerm.evaluate()
    ) {
        is GMResult.Err -> return GMResult.Err(
            CurveError.ExpressionEvaluation(
                term = "vectorfield3d.arrowhead.enabled",
                error = result.error,
            ),
        )
        is GMResult.Ok -> {
            val value = result.value
            if (value is JessieCodeRuntimeValue.BooleanValue) {
                value.value
            } else {
                return GMResult.Err(
                    CurveError.NonNumericExpression(
                        term = "vectorfield3d.arrowhead.enabled",
                        actualType = vectorField3DRuntimeType(value),
                    ),
                )
            }
        }
    }
    val arrowSize =
        if (arrowEnabled) {
            when (
                val result = evaluateVectorField3DNumber(
                    termName = "vectorfield3d.arrowhead.size",
                    term = definition.arrowSizeTerm,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        } else {
            Curve3D.VECTOR_FIELD_DEFAULT_ARROW_SIZE
        }
    val arrowAngle =
        if (arrowEnabled) {
            when (
                val result = evaluateVectorField3DNumber(
                    termName = "vectorfield3d.arrowhead.angle",
                    term = definition.arrowAngleTerm,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        } else {
            Curve3D.VECTOR_FIELD_DEFAULT_ARROW_ANGLE
        }
    val pointCount = Curve3D.vectorFieldPointCount(
        xSteps = evaluatedMeshes[0][1],
        ySteps = evaluatedMeshes[1][1],
        zSteps = evaluatedMeshes[2][1],
        arrowEnabled = arrowEnabled,
    )
    definition.requestedPointCount = pointCount
    if (pointCount > Curve3D.MAX_SAMPLE_COUNT) {
        return GMResult.Err(
            CurveError.InvalidSampleCount(
                count = pointCount.coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt(),
                maximum = Curve3D.MAX_SAMPLE_COUNT,
            ),
        )
    }

    val counts = evaluatedMeshes.map { vectorField3DAxisCount(it[1]) }
    if (counts.any { it == 0L }) {
        val snapshot = Curve3DVectorFieldSnapshot(
            vectors = emptyList(),
            arrowEnabled = arrowEnabled,
            arrowSize = arrowSize,
            arrowAngle = arrowAngle,
        )
        definition.snapshot = snapshot
        return GMResult.Ok(
            Curve3DVectorFieldUpdate(
                points = emptyList(),
                requestedPointCount = pointCount,
                snapshot = snapshot,
            ),
        )
    }
    val starts = evaluatedMeshes.map { it[0] }
    val deltas = evaluatedMeshes.map {
        (it[2] - it[0]) / it[1]
    }
    val points = ArrayList<DoubleArray>(pointCount.toInt())
    val vectors = mutableListOf<Curve3DVectorFieldVectorSnapshot>()
    val arrowLegs = doubleArrayOf(
        arrowSize / board.unitX,
        arrowSize / board.unitY,
        arrowSize / sqrt(board.unitX * board.unitY),
    )

    var x = starts[0]
    repeat(counts[0].toInt()) {
        var y = starts[1]
        repeat(counts[1].toInt()) {
            var z = starts[2]
            repeat(counts[2].toInt()) zLoop@ {
                val original = when (
                    val result = definition.field.evaluate(x, y, z)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val norm = sqrt(
                    original[0] * original[0] +
                        original[1] * original[1] +
                        original[2] * original[2],
                )
                if (norm < JS_NUMBER_EPSILON) {
                    z += deltas[2]
                    return@zLoop
                }
                val vector = DoubleArray(3) { original[it] * scale }
                val start = doubleArrayOf(x, y, z)
                val end = DoubleArray(3) { start[it] + vector[it] }
                val scaledNorm = norm * scale
                vectors += Curve3DVectorFieldVectorSnapshot(
                    start = start,
                    vector = vector,
                    scaledNorm = scaledNorm,
                )
                points += homogeneous(start)
                points += homogeneous(end)
                points += NAN_POINT_3D.copyOf()

                if (arrowEnabled) {
                    val phi = atan2(vector[1], vector[0])
                    val theta = asin(vector[2] / scaledNorm)
                    val theta1 = theta - arrowAngle
                    val theta2 = theta + arrowAngle
                    points += homogeneous(
                        doubleArrayOf(
                            end[0] -
                                arrowLegs[0] * cos(phi) * cos(theta1),
                            end[1] -
                                arrowLegs[1] * sin(phi) * cos(theta1),
                            end[2] - arrowLegs[2] * sin(theta2),
                        ),
                    )
                    points += homogeneous(end)
                    points += homogeneous(
                        doubleArrayOf(
                            end[0] -
                                arrowLegs[0] * cos(phi) * cos(theta2),
                            end[1] -
                                arrowLegs[1] * sin(phi) * cos(theta2),
                            end[2] - arrowLegs[2] * sin(theta1),
                        ),
                    )
                    points += NAN_POINT_3D.copyOf()
                }
                z += deltas[2]
            }
            y += deltas[1]
        }
        x += deltas[0]
    }
    val snapshot = Curve3DVectorFieldSnapshot(
        vectors = vectors,
        arrowEnabled = arrowEnabled,
        arrowSize = arrowSize,
        arrowAngle = arrowAngle,
    )
    definition.snapshot = snapshot
    return GMResult.Ok(
        Curve3DVectorFieldUpdate(
            points = points,
            requestedPointCount = pointCount,
            snapshot = snapshot,
        ),
    )
}

internal fun vectorField3DPointCount(
    xSteps: Double,
    ySteps: Double,
    zSteps: Double,
    arrowEnabled: Boolean,
): Long {
    val counts = listOf(xSteps, ySteps, zSteps)
        .map(::vectorField3DAxisCount)
    if (counts.any { it == 0L }) {
        return 0L
    }
    var vectorCount = 1L
    for (count in counts) {
        if (vectorCount > Long.MAX_VALUE / count) {
            return Long.MAX_VALUE
        }
        vectorCount *= count
    }
    val pointsPerVector =
        VECTOR_FIELD_3D_BODY_POINT_COUNT +
            if (arrowEnabled) {
                VECTOR_FIELD_3D_ARROW_POINT_COUNT
            } else {
                0L
            }
    return if (vectorCount > Long.MAX_VALUE / pointsPerVector) {
        Long.MAX_VALUE
    } else {
        vectorCount * pointsPerVector
    }
}

private fun vectorField3DAxisCount(steps: Double): Long =
    when {
        steps.isNaN() || steps < 0.0 -> 0L
        !steps.isFinite() ||
            steps >= Long.MAX_VALUE.toDouble() -> Long.MAX_VALUE
        else -> floor(steps).toLong() + 1L
    }

private fun evaluateVectorField3DNumber(
    termName: String,
    term: JessieCodeCoordinateFunction,
    arguments: List<JessieCodeRuntimeValue> = emptyList(),
): GMResult<Double, CurveError> =
    when (val result = term.evaluate(arguments)) {
        is GMResult.Err -> GMResult.Err(
            CurveError.ExpressionEvaluation(
                term = termName,
                error = result.error,
            ),
        )
        is GMResult.Ok -> {
            val number = result.value as?
                JessieCodeRuntimeValue.NumberValue
            if (number == null) {
                GMResult.Err(
                    CurveError.NonNumericExpression(
                        term = termName,
                        actualType =
                            vectorField3DRuntimeType(result.value),
                    ),
                )
            } else {
                GMResult.Ok(number.value)
            }
        }
    }

private fun vectorField3DRuntimeType(
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
        is JessieCodeRuntimeValue.CompositionReference -> "composition"
        is JessieCodeRuntimeValue.ElementReference -> "element"
    }

private fun homogeneous(coordinates: DoubleArray): DoubleArray =
    doubleArrayOf(
        1.0,
        coordinates[0],
        coordinates[1],
        coordinates[2],
    )

private const val JS_NUMBER_EPSILON = 2.220446049250313e-16
private const val VECTOR_FIELD_3D_BODY_POINT_COUNT = 3L
private const val VECTOR_FIELD_3D_ARROW_POINT_COUNT = 4L
private val NAN_POINT_3D =
    doubleArrayOf(1.0, Double.NaN, Double.NaN, Double.NaN)
