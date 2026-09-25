/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/curve3d.js -> Curve3D / createCurve3D
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.Parametric3DEvaluator
import com.swithun.jsxgraph.core.math.ParametricProjectionError
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction

internal sealed interface Curve3DDynamicError {
    data class Rejected(
        val reason: String,
    ) : Curve3DDynamicError
}

internal fun interface Curve3DScalarEvaluator {
    fun evaluate(parameter: Double): GMResult<Double, Curve3DDynamicError>
}

internal fun interface Curve3DArrayEvaluator {
    fun evaluate(parameter: Double): GMResult<DoubleArray, Curve3DDynamicError>
}

internal sealed interface Curve3DSource {
    data class Function(
        val evaluator: Curve3DArrayEvaluator,
    ) : Curve3DSource

    data class Components(
        val x: Curve3DScalarEvaluator,
        val y: Curve3DScalarEvaluator,
        val z: Curve3DScalarEvaluator,
    ) : Curve3DSource

    data class Arrays(
        val x: DoubleArray,
        val y: DoubleArray,
        val z: DoubleArray,
    ) : Curve3DSource

    data class Transformed(
        val base: Curve3D,
    ) : Curve3DSource
}

internal sealed interface Curve3DError {
    data class InvalidSampleCount(
        val count: Int,
        val maximum: Int,
    ) : Curve3DError

    data class InvalidRangeCount(
        val count: Int,
    ) : Curve3DError

    data class RangeEvaluation(
        val rangeIndex: Int,
        val error: Line3DDynamicError,
    ) : Curve3DError

    data class CoordinateEvaluation(
        val coordinateIndex: Int?,
        val parameter: Double,
        val error: Curve3DDynamicError,
    ) : Curve3DError

    data class InvalidCoordinateCount(
        val count: Int,
    ) : Curve3DError

    data class ParentViewMismatch(
        val parentIndex: Int,
    ) : Curve3DError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : Curve3DError

    data class InvalidTransformationCount(
        val count: Int,
    ) : Curve3DError

    data class InvalidBaseElement(
        val baseElementId: String?,
    ) : Curve3DError

    data class TransformationEvaluation(
        val transformationIndex: Int,
        val error: TransformationError,
    ) : Curve3DError

    data class Registration(
        val error: BoardError,
    ) : Curve3DError

    data class ProxyCurveFactory(
        val error: CurveError,
    ) : Curve3DError

    data class VectorField(
        val error: CurveError,
    ) : Curve3DError

    data class ParametricProjection(
        val error: ParametricProjectionError<Curve3DError>,
    ) : Curve3DError
}

/**
 * JSXGraph Curve3D with the ordinary Curve proxy used by the portable scene.
 */
internal class Curve3D private constructor(
    view: View3D,
    private val source: Curve3DSource,
    private val rangeSource: List<Line3DCoordinateValue>,
    private val vectorFieldDefinition: Curve3DVectorFieldDefinition?,
    internal val sampleCount: Int,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : GeometryElement3D(
    view = view,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_CURVE3D,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal val points = mutableListOf<DoubleArray>()
    internal var numberPoints: Int = 0
        private set
    internal var evaluatedRange: DoubleArray = DoubleArray(0)
        private set
    internal lateinit var curve2D: Curve
        private set
    internal val inherits = mutableListOf<GeometryElement>()
    internal var evaluationError: Curve3DError? = null
        private set
    internal val isVectorField3D: Boolean
        get() = vectorFieldDefinition != null

    init {
        elType = CURVE_3D_ELEMENT_TYPE
        isDraggable = true
    }

    // JSXGraph: src/3d/curve3d.js -> updateCoords.
    internal fun updateCoordsResult(): GMResult<Curve3D, Curve3DError> {
        points.clear()
        vectorFieldDefinition?.let { definition ->
            return when (
                val result = updateVectorField3D(
                    definition = definition,
                    board = view.board,
                )
            ) {
                is GMResult.Ok -> {
                    points += result.value.points
                    numberPoints = points.size
                    GMResult.Ok(this)
                }
                is GMResult.Err -> GMResult.Err(
                    Curve3DError.VectorField(result.error),
                )
            }
        }
        when (val currentSource = source) {
            is Curve3DSource.Arrays -> {
                for (index in currentSource.x.indices) {
                    points += doubleArrayOf(
                        1.0,
                        currentSource.x[index],
                        currentSource.y.getOrNull(index) ?: Double.NaN,
                        currentSource.z.getOrNull(index) ?: Double.NaN,
                    )
                }
            }
            is Curve3DSource.Transformed -> Unit
            is Curve3DSource.Function,
            is Curve3DSource.Components,
            -> {
                val range = when (val result = evaluateRange()) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                evaluatedRange = range
                val start = range[0]
                val end = range[1]
                val delta = (end - start) / (sampleCount - 1)
                var parameter = start
                var index = 0
                while (index < sampleCount && parameter <= end) {
                    val coordinates = when (
                        val result = evalFResult(parameter)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    points += doubleArrayOf(
                        1.0,
                        coordinates[0],
                        coordinates[1],
                        coordinates[2],
                    )
                    index += 1
                    parameter += delta
                }
            }
        }
        numberPoints = points.size
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/curve3d.js -> evalF / F / X / Y / Z.
    internal fun evalFResult(
        parameter: Double,
    ): GMResult<DoubleArray, Curve3DError> {
        val coordinates =
            if (transformations.isEmpty() || baseElement == null) {
                evaluateSource(parameter)
            } else {
                val baseCurve = baseElement as? Curve3D
                    ?: return GMResult.Err(
                        Curve3DError.InvalidBaseElement(baseElement?.id),
                    )
                if (baseCurve === this) {
                    evaluateSource(parameter)
                } else {
                    baseCurve.evalFResult(parameter)
                }
            }
        var evaluated = when (coordinates) {
            is GMResult.Ok -> coordinates.value
            is GMResult.Err -> return coordinates
        }
        if (transformations.isEmpty() || baseElement == null) {
            return GMResult.Ok(evaluated)
        }
        for ((index, transformation) in transformations.withIndex()) {
            when (val result = transformation.updateResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Curve3DError.TransformationEvaluation(
                        transformationIndex = index,
                        error = result.error,
                    ),
                )
            }
            evaluated = Mat.matVecMult(
                transformation.matrix,
                doubleArrayOf(
                    1.0,
                    evaluated[0],
                    evaluated[1],
                    evaluated[2],
                ),
            ).copyOfRange(1, 4)
        }
        return GMResult.Ok(evaluated)
    }

    @Suppress("FunctionName")
    internal fun F(parameter: Double): DoubleArray =
        when (val result = evalFResult(parameter)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> DoubleArray(3) { Double.NaN }
        }

    @Suppress("FunctionName")
    internal fun X(parameter: Double): Double = F(parameter)[0]

    @Suppress("FunctionName")
    internal fun Y(parameter: Double): Double = F(parameter)[1]

    @Suppress("FunctionName")
    internal fun Z(parameter: Double): Double = F(parameter)[2]

    // JSXGraph: src/3d/curve3d.js -> updateDataArray2D.
    internal fun updateDataArray2D(): CurveDataUpdate {
        val projected = points.map(view::project3DTo2D)
        return CurveDataUpdate(
            x = DoubleArray(projected.size) { projected[it][1] },
            y = DoubleArray(projected.size) { projected[it][2] },
        )
    }

    // JSXGraph: src/3d/curve3d.js -> addTransform.
    internal fun addTransform(
        element: Curve3D,
        transformation: Transformation,
    ): Curve3D = addTransform(element, listOf(transformation))

    internal fun addTransform(
        element: Curve3D,
        newTransformations: Iterable<Transformation>,
    ): Curve3D {
        addTransformGeneric(element, newTransformations)
        return this
    }

    // JSXGraph: src/3d/curve3d.js -> removeTransform.
    internal fun removeTransform(
        transformation: Transformation,
    ): Curve3D {
        removeTransformGeneric(listOf(transformation))
        return this
    }

    // JSXGraph: src/3d/curve3d.js -> clearTransforms.
    internal fun clearTransforms(): Curve3D {
        clearTransformsGeneric()
        evaluationError = null
        return this
    }

    internal fun requestedPointCount(): Long? =
        vectorFieldDefinition?.requestedPointCount

    internal fun vectorField3DSnapshot(): Curve3DVectorFieldSnapshot? =
        vectorFieldDefinition?.snapshot

    // JSXGraph: src/3d/curve3d.js -> updateTransform.
    internal fun updateTransformResult(): GMResult<Curve3D, Curve3DError> {
        if (
            transformations.isEmpty() ||
            baseElement == null ||
            source !is Curve3DSource.Transformed
        ) {
            return GMResult.Ok(this)
        }
        val baseCurve = baseElement as? Curve3D
            ?: return GMResult.Err(
                Curve3DError.InvalidBaseElement(baseElement?.id),
            )
        for ((index, transformation) in transformations.withIndex()) {
            when (val result = transformation.updateResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Curve3DError.TransformationEvaluation(
                        transformationIndex = index,
                        error = result.error,
                    ),
                )
            }
        }
        points.clear()
        for (basePoint in baseCurve.points) {
            var transformed = basePoint.copyOf()
            for (transformation in transformations) {
                transformed = Mat.matVecMult(
                    transformation.matrix,
                    transformed,
                )
            }
            points += transformed
        }
        numberPoints = baseCurve.numberPoints
        evaluatedRange = baseCurve.evaluatedRange.copyOf()
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/curve3d.js -> update.
    override fun update(fromParent: Boolean): Curve3D {
        if (!needsUpdate) {
            return this
        }
        val result = when (val coordinates = updateCoordsResult()) {
            is GMResult.Ok -> updateTransformResult()
            is GMResult.Err -> coordinates
        }
        when (result) {
            is GMResult.Ok -> {
                evaluationError = null
                if (this::curve2D.isInitialized) {
                    val data = updateDataArray2D()
                    curve2D.replaceData(data.x, data.y)
                }
            }
            is GMResult.Err -> {
                evaluationError = result.error
                points.clear()
                numberPoints = 0
                if (this::curve2D.isInitialized) {
                    curve2D.replaceData(DoubleArray(0), DoubleArray(0))
                }
            }
        }
        return this
    }

    override fun updateRenderer(): Curve3D {
        needsUpdate = false
        return this
    }

    // JSXGraph: src/3d/curve3d.js -> projectCoords;
    // src/math/geometry.js -> projectCoordsToParametric.
    internal fun projectCoords(
        coordinates: DoubleArray,
        parameters: MutableList<Double>,
    ): GMResult<DoubleArray, Curve3DError> {
        val range = when (val result = evaluateRange()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Geometry.projectCoordsToParametric(
                coordinates = coordinates,
                evaluator = Parametric3DEvaluator { values ->
                    evalFResult(values[0])
                },
                dimension = 1,
                parameters = parameters,
                rangeU = range,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                Curve3DError.ParametricProjection(result.error),
            )
        }
    }

    override fun remove(): GeometryElement {
        points.clear()
        inherits.clear()
        return super.remove()
    }

    private fun evaluateSource(
        parameter: Double,
    ): GMResult<DoubleArray, Curve3DError> =
        when (val currentSource = source) {
            is Curve3DSource.Function -> when (
                val result = currentSource.evaluator.evaluate(parameter)
            ) {
                is GMResult.Ok ->
                    if (result.value.size == 3) {
                        GMResult.Ok(result.value.copyOf())
                    } else {
                        GMResult.Err(
                            Curve3DError.InvalidCoordinateCount(
                                result.value.size,
                            ),
                        )
                    }
                is GMResult.Err -> GMResult.Err(
                    Curve3DError.CoordinateEvaluation(
                        coordinateIndex = null,
                        parameter = parameter,
                        error = result.error,
                    ),
                )
            }
            is Curve3DSource.Components -> {
                val values = DoubleArray(3)
                val evaluators = listOf(
                    currentSource.x,
                    currentSource.y,
                    currentSource.z,
                )
                for ((index, evaluator) in evaluators.withIndex()) {
                    values[index] = when (
                        val result = evaluator.evaluate(parameter)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return GMResult.Err(
                            Curve3DError.CoordinateEvaluation(
                                coordinateIndex = index,
                                parameter = parameter,
                                error = result.error,
                            ),
                        )
                    }
                }
                GMResult.Ok(values)
            }
            is Curve3DSource.Arrays -> {
                val index = parameter.toInt()
                GMResult.Ok(
                    doubleArrayOf(
                        currentSource.x.getOrNull(index) ?: Double.NaN,
                        currentSource.y.getOrNull(index) ?: Double.NaN,
                        currentSource.z.getOrNull(index) ?: Double.NaN,
                    ),
                )
            }
            is Curve3DSource.Transformed ->
                currentSource.base.evalFResult(parameter)
        }

    private fun evaluateRange(): GMResult<DoubleArray, Curve3DError> {
        if (rangeSource.size != 2) {
            return GMResult.Err(
                Curve3DError.InvalidRangeCount(rangeSource.size),
            )
        }
        val range = DoubleArray(2)
        for ((index, value) in rangeSource.withIndex()) {
            range[index] = when (value) {
                is Line3DCoordinateValue.Numeric -> value.value
                is Line3DCoordinateValue.Dynamic -> when (
                    val result = value.evaluator.evaluate()
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        Curve3DError.RangeEvaluation(
                            rangeIndex = index,
                            error = result.error,
                        ),
                    )
                }
            }
        }
        return GMResult.Ok(range)
    }

    internal companion object {
        internal const val DEFAULT_SAMPLE_COUNT: Int = 200
        internal const val MAX_SAMPLE_COUNT: Int = Curve.MAX_SAMPLE_COUNT
        private const val CURVE_3D_ID_PREFIX = "curve3d"
        private const val CURVE_3D_ELEMENT_TYPE = "curve3d"

        internal fun create(
            view: View3D,
            source: Curve3DSource,
            rangeSource: List<Line3DCoordinateValue> = emptyList(),
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            dependencies: Iterable<GeometryElement> = emptyList(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve3D, Curve3DError> =
            createInternal(
                view = view,
                source = source,
                rangeSource = rangeSource,
                vectorFieldDefinition = null,
                sampleCount = sampleCount,
                dependencies = dependencies,
                baseCurve = null,
                transformations = emptyList(),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )

        // JSXGraph: src/3d/curve3d.js -> createCurve3D transformed form.
        internal fun create(
            view: View3D,
            baseCurve: Curve3D,
            transformations: List<Transformation>,
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve3D, Curve3DError> {
            if (baseCurve.view !== view) {
                return GMResult.Err(
                    Curve3DError.ParentViewMismatch(parentIndex = 0),
                )
            }
            if (view.board.elementById(baseCurve.id) !== baseCurve) {
                return GMResult.Err(
                    Curve3DError.ParentNotRegistered(
                        parentIndex = 0,
                        id = baseCurve.id,
                    ),
                )
            }
            if (transformations.isEmpty()) {
                return GMResult.Err(
                    Curve3DError.InvalidTransformationCount(0),
                )
            }
            return createInternal(
                view = view,
                source = Curve3DSource.Transformed(baseCurve),
                rangeSource = emptyList(),
                vectorFieldDefinition = null,
                sampleCount = sampleCount,
                dependencies = emptyList(),
                baseCurve = baseCurve,
                transformations = transformations,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        }

        // JSXGraph 1.13.3:
        // src/3d/curve3d.js -> createVectorfield3D.
        internal fun createVectorField(
            view: View3D,
            field: Curve3DVectorFieldFunction,
            xData: List<JessieCodeCoordinateFunction>,
            yData: List<JessieCodeCoordinateFunction>,
            zData: List<JessieCodeCoordinateFunction>,
            scaleTerm: JessieCodeCoordinateFunction,
            arrowEnabledTerm: JessieCodeCoordinateFunction,
            arrowSizeTerm: JessieCodeCoordinateFunction,
            arrowAngleTerm: JessieCodeCoordinateFunction,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Curve3D, Curve3DError> {
            if (xData.size != 3 || yData.size != 3 || zData.size != 3) {
                return GMResult.Err(
                    Curve3DError.VectorField(
                        CurveError.NonNumericExpression(
                            term = "vectorfield3d.mesh",
                            actualType = "invalid length",
                        ),
                    ),
                )
            }
            return createInternal(
                view = view,
                source = Curve3DSource.Arrays(
                    x = doubleArrayOf(),
                    y = doubleArrayOf(),
                    z = doubleArrayOf(),
                ),
                rangeSource = emptyList(),
                vectorFieldDefinition = Curve3DVectorFieldDefinition(
                    field = field,
                    xData = xData,
                    yData = yData,
                    zData = zData,
                    scaleTerm = scaleTerm,
                    arrowEnabledTerm = arrowEnabledTerm,
                    arrowSizeTerm = arrowSizeTerm,
                    arrowAngleTerm = arrowAngleTerm,
                ),
                sampleCount = 1,
                dependencies = emptyList(),
                baseCurve = null,
                transformations = emptyList(),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        }

        private fun createInternal(
            view: View3D,
            source: Curve3DSource,
            rangeSource: List<Line3DCoordinateValue>,
            vectorFieldDefinition: Curve3DVectorFieldDefinition?,
            sampleCount: Int,
            dependencies: Iterable<GeometryElement>,
            baseCurve: Curve3D?,
            transformations: List<Transformation>,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
        ): GMResult<Curve3D, Curve3DError> {
            if (sampleCount !in 1..MAX_SAMPLE_COUNT) {
                return GMResult.Err(
                    Curve3DError.InvalidSampleCount(
                        count = sampleCount,
                        maximum = MAX_SAMPLE_COUNT,
                    ),
                )
            }
            if (
                source !is Curve3DSource.Arrays &&
                source !is Curve3DSource.Transformed &&
                rangeSource.size != 2
            ) {
                return GMResult.Err(
                    Curve3DError.InvalidRangeCount(rangeSource.size),
                )
            }
            if (id.isNotEmpty() && view.board.elementById(id) != null) {
                return GMResult.Err(
                    Curve3DError.Registration(
                        BoardError.DuplicateElementId(id),
                    ),
                )
            }
            val curve = Curve3D(
                view = view,
                source = source,
                rangeSource = rangeSource,
                vectorFieldDefinition = vectorFieldDefinition,
                sampleCount = sampleCount,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            if (baseCurve != null) {
                curve.addTransform(baseCurve, transformations)
            }
            when (val result = curve.updateCoordsResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (val result = curve.updateTransformResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (
                val registration =
                    view.board.setId(curve, CURVE_3D_ID_PREFIX)
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Curve3DError.Registration(registration.error),
                )
            }
            curve.registerInView()
            val projected = curve.updateDataArray2D()
            val proxy = when (
                val result = Curve.createData(
                    board = view.board,
                    dataX = projected.x,
                    dataY = projected.y,
                    name = curve.name,
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    view.board.removeObject(curve)
                    return GMResult.Err(
                        Curve3DError.ProxyCurveFactory(result.error),
                    )
                }
            }
            proxy.dump = false
            proxy.isDraggable = false
            proxy.setParents(listOf(curve))
            curve.curve2D = proxy
            curve.element2D = proxy
            curve.addChild(proxy)
            curve.inherits += proxy

            if (baseCurve != null) {
                curve.setParents(listOf(baseCurve))
                baseCurve.addChild(curve)
            }
            for (dependency in dependencies.distinctBy(GeometryElement::id)) {
                dependency.addChild(curve)
            }
            return GMResult.Ok(curve)
        }

        internal const val VECTOR_FIELD_DEFAULT_SCALE: Double = 1.0
        internal const val VECTOR_FIELD_DEFAULT_ARROW_SIZE: Double = 5.0
        internal const val VECTOR_FIELD_DEFAULT_ARROW_ANGLE: Double =
            kotlin.math.PI * 0.125

        internal fun vectorFieldPointCount(
            xSteps: Double,
            ySteps: Double,
            zSteps: Double,
            arrowEnabled: Boolean,
        ): Long =
            vectorField3DPointCount(
                xSteps = xSteps,
                ySteps = ySteps,
                zSteps = zSteps,
                arrowEnabled = arrowEnabled,
            )
    }
}
