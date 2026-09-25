/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/circle3d.js -> Circle3D / createCircle3D
 * Copyright 2008-2026 Matthias Ehmann, Aaron Fenyes, Carsten Miller,
 * Andreas Walter, and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal sealed interface Circle3DNormalSource {
    data class Values(
        val values: List<Line3DCoordinateValue>,
    ) : Circle3DNormalSource

    data class Function(
        val evaluator: Line3DArrayEvaluator,
    ) : Circle3DNormalSource
}

internal sealed interface Circle3DError {
    data class ParentViewMismatch(
        val parentIndex: Int,
    ) : Circle3DError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : Circle3DError

    data class InvalidNormalCount(
        val count: Int,
    ) : Circle3DError

    data class NormalEvaluation(
        val coordinateIndex: Int?,
        val error: Line3DDynamicError,
    ) : Circle3DError

    data class RadiusEvaluation(
        val error: Line3DDynamicError,
    ) : Circle3DError

    data class Registration(
        val error: BoardError,
    ) : Circle3DError

    data class Curve3DFactory(
        val error: Curve3DError,
    ) : Circle3DError
}

/**
 * A source-mapped Circle3D backed by the ordinary Curve proxy owned by its
 * underlying Curve3D.
 */
internal class Circle3D private constructor(
    view: View3D,
    internal val center: Point3D,
    private val normalSource: Circle3DNormalSource,
    private var radiusSource: Line3DCoordinateValue,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : GeometryElement3D(
    view = view,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_CIRCLE3D,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal var normal: DoubleArray = DoubleArray(4)
        private set
    internal var frame1: DoubleArray = DoubleArray(4)
        private set
    internal var frame2: DoubleArray = DoubleArray(4)
        private set
    internal lateinit var curve: Curve3D
        private set
    internal var evaluationError: Circle3DError? = null
        private set

    init {
        elType = CIRCLE_3D_ELEMENT_TYPE
    }

    // JSXGraph: src/3d/circle3d.js -> Radius.
    @Suppress("FunctionName")
    internal fun Radius(): Double =
        when (val result = evaluateRadius()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> Double.NaN
        }

    // JSXGraph: src/3d/circle3d.js -> setRadius.
    internal fun setRadius(
        source: Line3DCoordinateValue,
        dependencies: Iterable<GeometryElement> = emptyList(),
    ): GMResult<Circle3D, Circle3DError> {
        radiusSource = source
        for (dependency in dependencies.distinctBy(GeometryElement::id)) {
            dependency.addChild(this)
        }
        when (val result = evaluateRadius()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        board.update()
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/circle3d.js -> normalizeFrame.
    internal fun normalizeFrame(): Circle3D {
        val length1 = Mat.norm(frame1)
        val length2 = Mat.norm(frame2)
        for (index in 0 until 4) {
            frame1[index] /= length1
            frame2[index] /= length2
        }
        return this
    }

    // JSXGraph: src/3d/circle3d.js -> updateNormal.
    internal fun updateNormalResult(): GMResult<Circle3D, Circle3DError> {
        val evaluated = when (val result = evaluateNormal()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        normal = evaluated
        val length = Mat.norm(normal)
        if (abs(length) > Mat.eps * Mat.eps) {
            for (index in 0 until 4) {
                normal[index] /= length
            }
        }
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/circle3d.js -> updateFrame.
    internal fun updateFrame(): Circle3D {
        frame1 = Mat.crossProduct(
            frame2.copyOfRange(1, 4),
            normal.copyOfRange(1, 4),
        ).withLeadingZero()
        frame2 = Mat.crossProduct(
            normal.copyOfRange(1, 4),
            frame1.copyOfRange(1, 4),
        ).withLeadingZero()
        return normalizeFrame()
    }

    // JSXGraph: src/3d/circle3d.js -> update.
    override fun update(fromParent: Boolean): Circle3D {
        if (!needsUpdate) {
            return this
        }
        when (val normalResult = updateNormalResult()) {
            is GMResult.Ok -> {
                updateFrame()
                evaluationError = when (val radiusResult = evaluateRadius()) {
                    is GMResult.Ok -> null
                    is GMResult.Err -> radiusResult.error
                }
            }
            is GMResult.Err -> evaluationError = normalResult.error
        }
        return this
    }

    override fun updateRenderer(): Circle3D {
        needsUpdate = false
        return this
    }

    // JSXGraph: src/3d/circle3d.js -> projectCoords.
    internal fun projectCoords(
        coordinates: DoubleArray,
        parameters: DoubleArray,
    ): GMResult<DoubleArray, Curve3DError> =
        curve.projectCoords(coordinates, parameters)

    override fun remove(): GeometryElement {
        return super.remove()
    }

    private fun pointAtResult(
        parameter: Double,
    ): GMResult<DoubleArray, Curve3DDynamicError> {
        val radius = when (val result = evaluateRadius()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                Curve3DDynamicError.Rejected(result.error.toString()),
            )
        }
        val sine = sin(parameter)
        val cosine = cos(parameter)
        return GMResult.Ok(
            doubleArrayOf(
                center.coords[1] +
                    radius * (
                        cosine * frame1[1] +
                            sine * frame2[1]
                        ),
                center.coords[2] +
                    radius * (
                        cosine * frame1[2] +
                            sine * frame2[2]
                        ),
                center.coords[3] +
                    radius * (
                        cosine * frame1[3] +
                            sine * frame2[3]
                        ),
            ),
        )
    }

    private fun evaluateRadius(): GMResult<Double, Circle3DError> =
        when (val source = radiusSource) {
            is Line3DCoordinateValue.Numeric ->
                GMResult.Ok(abs(source.value))
            is Line3DCoordinateValue.Dynamic ->
                when (val result = source.evaluator.evaluate()) {
                    is GMResult.Ok -> GMResult.Ok(abs(result.value))
                    is GMResult.Err -> GMResult.Err(
                        Circle3DError.RadiusEvaluation(result.error),
                    )
                }
        }

    private fun evaluateNormal(): GMResult<DoubleArray, Circle3DError> =
        when (val source = normalSource) {
            is Circle3DNormalSource.Function -> {
                val evaluated = when (
                    val result = source.evaluator.evaluate()
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        Circle3DError.NormalEvaluation(
                            coordinateIndex = null,
                            error = result.error,
                        ),
                    )
                }
                if (evaluated.size == 4) {
                    GMResult.Ok(evaluated.copyOf())
                } else {
                    GMResult.Err(
                        Circle3DError.InvalidNormalCount(evaluated.size),
                    )
                }
            }
            is Circle3DNormalSource.Values -> {
                if (source.values.size !in setOf(3, 4)) {
                    return GMResult.Err(
                        Circle3DError.InvalidNormalCount(
                            source.values.size,
                        ),
                    )
                }
                val result = DoubleArray(4)
                val offset =
                    if (source.values.size == 3) {
                        result[0] = 0.0
                        1
                    } else {
                        0
                    }
                for ((index, value) in source.values.withIndex()) {
                    result[offset + index] = when (value) {
                        is Line3DCoordinateValue.Numeric -> value.value
                        is Line3DCoordinateValue.Dynamic -> when (
                            val evaluated = value.evaluator.evaluate()
                        ) {
                            is GMResult.Ok -> evaluated.value
                            is GMResult.Err -> return GMResult.Err(
                                Circle3DError.NormalEvaluation(
                                    coordinateIndex = index,
                                    error = evaluated.error,
                                ),
                            )
                        }
                    }
                }
                GMResult.Ok(result)
            }
        }

    private fun initializeFrame(): Circle3D {
        frame1 = Mat.crossProduct(
            normal.copyOfRange(1, 4),
            doubleArrayOf(1.0, 0.0, 0.0),
        ).withLeadingZero()
        val alternateFrame = Mat.crossProduct(
            normal.copyOfRange(1, 4),
            doubleArrayOf(-0.5, 0.8660254037844386, 0.0),
        ).withLeadingZero()
        if (Mat.norm(alternateFrame) > Mat.norm(frame1)) {
            frame1 = alternateFrame
        }
        frame2 = Mat.crossProduct(
            normal.copyOfRange(1, 4),
            frame1.copyOfRange(1, 4),
        ).withLeadingZero()
        return normalizeFrame()
    }

    private fun DoubleArray.withLeadingZero(): DoubleArray =
        doubleArrayOf(0.0, this[0], this[1], this[2])

    internal companion object {
        internal const val DEFAULT_SAMPLE_COUNT: Int =
            Curve3D.DEFAULT_SAMPLE_COUNT
        internal const val MAX_SAMPLE_COUNT: Int =
            Curve3D.MAX_SAMPLE_COUNT
        private const val CIRCLE_3D_ID_PREFIX = "circle3d"
        private const val CIRCLE_3D_ELEMENT_TYPE = "circle3d"

        // JSXGraph: src/3d/circle3d.js -> createCircle3D.
        internal fun create(
            view: View3D,
            center: Point3D,
            normalSource: Circle3DNormalSource,
            radiusSource: Line3DCoordinateValue,
            ownsCenter: Boolean = false,
            radiusDependencies: Iterable<GeometryElement> = emptyList(),
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Circle3D, Circle3DError> {
            if (center.view !== view) {
                return GMResult.Err(
                    Circle3DError.ParentViewMismatch(parentIndex = 0),
                )
            }
            if (view.board.elementById(center.id) !== center) {
                return GMResult.Err(
                    Circle3DError.ParentNotRegistered(
                        parentIndex = 0,
                        id = center.id,
                    ),
                )
            }
            if (sampleCount !in 1..MAX_SAMPLE_COUNT) {
                return GMResult.Err(
                    Circle3DError.Curve3DFactory(
                        Curve3DError.InvalidSampleCount(
                            count = sampleCount,
                            maximum = MAX_SAMPLE_COUNT,
                        ),
                    ),
                )
            }
            if (id.isNotEmpty() && view.board.elementById(id) != null) {
                return GMResult.Err(
                    Circle3DError.Registration(
                        BoardError.DuplicateElementId(id),
                    ),
                )
            }
            val circle = Circle3D(
                view = view,
                center = center,
                normalSource = normalSource,
                radiusSource = radiusSource,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            when (val result = circle.updateNormalResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (val result = circle.evaluateRadius()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            circle.initializeFrame()
            when (
                val registration =
                    view.board.setId(circle, CIRCLE_3D_ID_PREFIX)
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Circle3DError.Registration(registration.error),
                )
            }
            circle.registerInView()
            val curve = when (
                val result = Curve3D.create(
                    view = view,
                    source = Curve3DSource.Function(
                        Curve3DArrayEvaluator(circle::pointAtResult),
                    ),
                    rangeSource = listOf(
                        Line3DCoordinateValue.Numeric(0.0),
                        Line3DCoordinateValue.Numeric(2.0 * PI),
                    ),
                    sampleCount = sampleCount,
                    dependencies = listOf(circle),
                    name = "",
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    view.board.removeObject(circle)
                    return GMResult.Err(
                        Circle3DError.Curve3DFactory(result.error),
                    )
                }
            }
            circle.curve = curve
            circle.element2D = curve.curve2D
            curve.setParents(listOf(circle))
            circle.addChild(curve)
            if (ownsCenter) {
                circle.addChild(center)
            } else {
                center.addChild(circle)
            }
            for (
                dependency in
                radiusDependencies.distinctBy(GeometryElement::id)
            ) {
                dependency.addChild(circle)
            }
            circle.update()
            return GMResult.Ok(circle)
        }
    }
}
