/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/surface3d.js -> Surface3D /
 * createParametricSurface3D / createFunctiongraph3D,
 * src/math/tiling.js -> triangulation / rectangulation
 * Copyright 2005-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.Parametric3DEvaluator
import com.swithun.jsxgraph.core.math.ParametricProjectionError
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal sealed interface Surface3DDynamicError {
    data class Rejected(
        val reason: String,
    ) : Surface3DDynamicError
}

internal fun interface Surface3DScalarEvaluator {
    fun evaluate(
        parameterU: Double,
        parameterV: Double,
    ): GMResult<Double, Surface3DDynamicError>
}

internal fun interface Surface3DArrayEvaluator {
    fun evaluate(
        parameterU: Double,
        parameterV: Double,
    ): GMResult<DoubleArray, Surface3DDynamicError>
}

internal sealed interface Surface3DSource {
    data class Function(
        val evaluator: Surface3DArrayEvaluator,
    ) : Surface3DSource

    data class Components(
        val x: Surface3DScalarEvaluator,
        val y: Surface3DScalarEvaluator,
        val z: Surface3DScalarEvaluator,
    ) : Surface3DSource

    data class Transformed(
        val base: Surface3D,
    ) : Surface3DSource
}

internal data class Surface3DAttributes(
    val surfaceType: String = "wireframe",
    val tiling: String = "rectangle",
    val stepsU: Int = 30,
    val stepsV: Int = 30,
    val fillColorArray: List<String> = listOf("white", "#0072b2"),
    val faceAttributes: Face3DAttributes = Face3DAttributes(
        strokeWidth = 0.1,
        fillOpacity = 0.8,
        shader = Face3DShaderAttributes(
            minimumLightness = 55.0,
        ),
    ),
    val colormap: Plane3DColormapAttributes =
        Plane3DColormapAttributes(),
)

internal sealed interface Surface3DError {
    data class InvalidRangeCount(
        val rangeIndex: Int,
        val count: Int,
    ) : Surface3DError

    data class RangeEvaluation(
        val rangeIndex: Int,
        val coordinateIndex: Int,
        val error: Line3DDynamicError,
    ) : Surface3DError

    data class CoordinateEvaluation(
        val coordinateIndex: Int?,
        val parameterU: Double,
        val parameterV: Double,
        val error: Surface3DDynamicError,
    ) : Surface3DError

    data class InvalidCoordinateCount(
        val count: Int,
    ) : Surface3DError

    data class InvalidSurfaceType(
        val value: String,
    ) : Surface3DError

    data class InvalidTiling(
        val value: String,
    ) : Surface3DError

    data class InvalidSteps(
        val axis: String,
        val count: Int,
        val minimum: Int,
    ) : Surface3DError

    data class VertexLimitExceeded(
        val count: Long,
        val maximum: Int,
    ) : Surface3DError

    data class FaceLimitExceeded(
        val count: Long,
        val maximum: Int,
    ) : Surface3DError

    data class CurvePointLimitExceeded(
        val count: Long,
        val maximum: Int,
    ) : Surface3DError

    data class ParentViewMismatch(
        val parentIndex: Int,
    ) : Surface3DError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : Surface3DError

    data class InvalidTransformationCount(
        val count: Int,
    ) : Surface3DError

    data class InvalidBaseElement(
        val baseElementId: String?,
    ) : Surface3DError

    data class TransformationEvaluation(
        val transformationIndex: Int,
        val error: TransformationError,
    ) : Surface3DError

    data class Registration(
        val error: BoardError,
    ) : Surface3DError

    data class ProxyCurveFactory(
        val error: CurveError,
    ) : Surface3DError

    data class Polyhedron3DFactory(
        val error: Polyhedron3DError,
    ) : Surface3DError

    data class ParametricProjection(
        val error: ParametricProjectionError<Surface3DError>,
    ) : Surface3DError
}

/**
 * JSXGraph parametric Surface3D with either a wireframe Curve proxy or a
 * Polyhedron3D made from the upstream tiling.
 */
internal class Surface3D private constructor(
    view: View3D,
    private val source: Surface3DSource,
    private val rangeUSource: List<Line3DCoordinateValue>,
    private val rangeVSource: List<Line3DCoordinateValue>,
    internal val surfaceAttributes: Surface3DAttributes,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    functionGraph: Boolean,
) : GeometryElement3D(
    view = view,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_SURFACE3D,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal val points = mutableListOf<MutableList<DoubleArray>>()
    internal var evaluatedRangeU = DoubleArray(0)
        private set
    internal var evaluatedRangeV = DoubleArray(0)
        private set
    internal lateinit var curve2D: Curve
        private set
    internal var polyhedron: Polyhedron3D? = null
        private set
    internal val inherits = mutableListOf<GeometryElement>()
    internal var evaluationError: Surface3DError? = null
        private set
    internal val isFunctionGraph: Boolean = functionGraph

    init {
        elType = if (functionGraph) {
            FUNCTION_GRAPH_3D_ELEMENT_TYPE
        } else {
            SURFACE_3D_ELEMENT_TYPE
        }
        isDraggable = true
    }

    // JSXGraph: src/3d/surface3d.js -> updateWireframe.
    internal fun updateWireframeResult():
        GMResult<Surface3D, Surface3DError> {
        if (surfaceAttributes.surfaceType != WIREFRAME_TYPE) {
            return GMResult.Ok(this)
        }
        val rangeU = when (val result = evaluateRange(rangeUSource, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val rangeV = when (val result = evaluateRange(rangeVSource, 1)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        evaluatedRangeU = rangeU
        evaluatedRangeV = rangeV
        points.clear()

        val stepsU = maxOf(surfaceAttributes.stepsU, 1)
        val stepsV = maxOf(surfaceAttributes.stepsV, 1)
        val deltaU = (rangeU[1] - rangeU[0]) / stepsU
        val deltaV = (rangeV[1] - rangeV[0]) / stepsV
        var parameterU = rangeU[0]
        for (indexU in 0..stepsU) {
            val row = mutableListOf<DoubleArray>()
            var parameterV = rangeV[0]
            for (indexV in 0..stepsV) {
                val coordinates = when (
                    val result = evalFResult(parameterU, parameterV)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                row += coordinates
                parameterV += deltaV
            }
            points += row
            parameterU += deltaU
        }
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/surface3d.js -> updateCoords.
    internal fun updateCoordsResult(): GMResult<Surface3D, Surface3DError> =
        if (source is Surface3DSource.Transformed) {
            updateTransformResult()
        } else {
            updateRangesResult().let { result ->
                when (result) {
                    is GMResult.Ok -> updateWireframeResult()
                    is GMResult.Err -> result
                }
            }
        }

    // JSXGraph: src/3d/surface3d.js -> evalF / F / X / Y / Z.
    internal fun evalFResult(
        parameterU: Double,
        parameterV: Double,
    ): GMResult<DoubleArray, Surface3DError> {
        var coordinates =
            if (transformations.isEmpty() || baseElement == null) {
                when (
                    val result = evaluateSource(parameterU, parameterV)
                ) {
                    is GMResult.Ok ->
                        doubleArrayOf(
                            1.0,
                            result.value[0],
                            result.value[1],
                            result.value[2],
                        )
                    is GMResult.Err -> return result
                }
            } else {
                val baseSurface = baseElement as? Surface3D
                    ?: return GMResult.Err(
                        Surface3DError.InvalidBaseElement(baseElement?.id),
                    )
                if (baseSurface === this) {
                    when (
                        val result = evaluateSource(parameterU, parameterV)
                    ) {
                        is GMResult.Ok ->
                            doubleArrayOf(
                                1.0,
                                result.value[0],
                                result.value[1],
                                result.value[2],
                            )
                        is GMResult.Err -> return result
                    }
                } else {
                    when (
                        val result =
                            baseSurface.evalFResult(parameterU, parameterV)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                }
            }
        if (transformations.isEmpty() || baseElement == null) {
            return GMResult.Ok(coordinates)
        }
        for ((index, transformation) in transformations.withIndex()) {
            when (val result = transformation.updateResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Surface3DError.TransformationEvaluation(
                        transformationIndex = index,
                        error = result.error,
                    ),
                )
            }
            coordinates = Mat.matVecMult(
                transformation.matrix,
                coordinates,
            )
        }
        return GMResult.Ok(coordinates)
    }

    @Suppress("FunctionName")
    internal fun F(
        parameterU: Double,
        parameterV: Double,
    ): DoubleArray =
        when (val result = evalFResult(parameterU, parameterV)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> DoubleArray(4) { Double.NaN }
        }

    @Suppress("FunctionName")
    internal fun X(parameterU: Double, parameterV: Double): Double =
        F(parameterU, parameterV)[1]

    @Suppress("FunctionName")
    internal fun Y(parameterU: Double, parameterV: Double): Double =
        F(parameterU, parameterV)[2]

    @Suppress("FunctionName")
    internal fun Z(parameterU: Double, parameterV: Double): Double =
        F(parameterU, parameterV)[3]

    // JSXGraph: src/3d/surface3d.js -> updateDataArray2D.
    internal fun updateDataArray2D(): CurveDataUpdate {
        val x = mutableListOf<Double>()
        val y = mutableListOf<Double>()
        val rowCount = points.size
        if (rowCount != 0) {
            val columnCount = points[0].size
            for (row in 0 until rowCount) {
                if (surfaceAttributes.stepsU > 0) {
                    for (column in 0 until columnCount) {
                        val projected =
                            view.project3DTo2D(points[row][column])
                        x += projected[1]
                        y += projected[2]
                    }
                }
                x += Double.NaN
                y += Double.NaN
            }
            for (column in 0 until columnCount) {
                if (surfaceAttributes.stepsV > 0) {
                    for (row in 0 until rowCount) {
                        val projected =
                            view.project3DTo2D(points[row][column])
                        x += projected[1]
                        y += projected[2]
                    }
                }
                x += Double.NaN
                y += Double.NaN
            }
        }
        return CurveDataUpdate(x.toDoubleArray(), y.toDoubleArray())
    }

    // JSXGraph: src/3d/surface3d.js -> addTransform.
    internal fun addTransform(
        element: Surface3D,
        transformation: Transformation,
    ): Surface3D = addTransform(element, listOf(transformation))

    internal fun addTransform(
        element: Surface3D,
        newTransformations: Iterable<Transformation>,
    ): Surface3D {
        addTransformGeneric(element, newTransformations)
        return this
    }

    // JSXGraph: src/3d/surface3d.js -> removeTransform.
    internal fun removeTransform(
        transformation: Transformation,
    ): Surface3D {
        removeTransformGeneric(listOf(transformation))
        return this
    }

    // JSXGraph: src/3d/surface3d.js -> clearTransforms.
    internal fun clearTransforms(): Surface3D {
        clearTransformsGeneric()
        evaluationError = null
        return this
    }

    // JSXGraph: src/3d/surface3d.js -> updateTransform.
    internal fun updateTransformResult():
        GMResult<Surface3D, Surface3DError> {
        if (
            transformations.isEmpty() ||
            baseElement == null ||
            source !is Surface3DSource.Transformed
        ) {
            return GMResult.Ok(this)
        }
        val baseSurface = baseElement as? Surface3D
            ?: return GMResult.Err(
                Surface3DError.InvalidBaseElement(baseElement?.id),
            )
        for ((index, transformation) in transformations.withIndex()) {
            when (val result = transformation.updateResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Surface3DError.TransformationEvaluation(
                        transformationIndex = index,
                        error = result.error,
                    ),
                )
            }
        }
        evaluatedRangeU = baseSurface.evaluatedRangeU.copyOf()
        evaluatedRangeV = baseSurface.evaluatedRangeV.copyOf()
        points.clear()
        for (baseRow in baseSurface.points) {
            val row = mutableListOf<DoubleArray>()
            for (basePoint in baseRow) {
                var coordinates = basePoint.copyOf()
                for (transformation in transformations) {
                    coordinates = Mat.matVecMult(
                        transformation.matrix,
                        coordinates,
                    )
                }
                row += coordinates
            }
            points += row
        }
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/surface3d.js -> update.
    override fun update(fromParent: Boolean): Surface3D {
        if (!needsUpdate) {
            return this
        }
        when (val result = updateCoordsResult()) {
            is GMResult.Ok -> {
                evaluationError = null
                if (this::curve2D.isInitialized) {
                    val data =
                        if (
                            surfaceAttributes.surfaceType == WIREFRAME_TYPE
                        ) {
                            updateDataArray2D()
                        } else {
                            CurveDataUpdate(
                                DoubleArray(0),
                                DoubleArray(0),
                            )
                        }
                    curve2D.replaceData(data.x, data.y)
                }
            }
            is GMResult.Err -> {
                evaluationError = result.error
                points.clear()
                if (this::curve2D.isInitialized) {
                    curve2D.replaceData(DoubleArray(0), DoubleArray(0))
                }
            }
        }
        return this
    }

    override fun updateRenderer(): Surface3D {
        needsUpdate = false
        return this
    }

    // JSXGraph: src/3d/surface3d.js -> projectCoords;
    // src/math/geometry.js -> projectCoordsToParametric.
    internal fun projectCoords(
        coordinates: DoubleArray,
        parameters: MutableList<Double>,
    ): GMResult<DoubleArray, Surface3DError> {
        val rangeU = when (val result = evaluateRange(rangeUSource, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val rangeV = when (val result = evaluateRange(rangeVSource, 1)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Geometry.projectCoordsToParametric(
                coordinates = coordinates,
                evaluator = Parametric3DEvaluator { values ->
                    evalFResult(values[0], values[1])
                },
                dimension = 2,
                parameters = parameters,
                rangeU = rangeU,
                rangeV = rangeV,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                Surface3DError.ParametricProjection(result.error),
            )
        }
    }

    override fun remove(): GeometryElement {
        points.clear()
        inherits.clear()
        polyhedron = null
        return super.remove()
    }

    private fun updateRangesResult():
        GMResult<Surface3D, Surface3DError> {
        val rangeU = when (val result = evaluateRange(rangeUSource, 0)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val rangeV = when (val result = evaluateRange(rangeVSource, 1)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        evaluatedRangeU = rangeU
        evaluatedRangeV = rangeV
        return GMResult.Ok(this)
    }

    private fun evaluateRange(
        source: List<Line3DCoordinateValue>,
        rangeIndex: Int,
    ): GMResult<DoubleArray, Surface3DError> {
        if (source.size != 2) {
            return GMResult.Err(
                Surface3DError.InvalidRangeCount(
                    rangeIndex = rangeIndex,
                    count = source.size,
                ),
            )
        }
        val range = DoubleArray(2)
        for ((coordinateIndex, value) in source.withIndex()) {
            range[coordinateIndex] = when (value) {
                is Line3DCoordinateValue.Numeric -> value.value
                is Line3DCoordinateValue.Dynamic -> when (
                    val result = value.evaluator.evaluate()
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        Surface3DError.RangeEvaluation(
                            rangeIndex = rangeIndex,
                            coordinateIndex = coordinateIndex,
                            error = result.error,
                        ),
                    )
                }
            }
        }
        return GMResult.Ok(range)
    }

    private fun evaluateSource(
        parameterU: Double,
        parameterV: Double,
    ): GMResult<DoubleArray, Surface3DError> =
        when (val currentSource = source) {
            is Surface3DSource.Function -> when (
                val result =
                    currentSource.evaluator.evaluate(parameterU, parameterV)
            ) {
                is GMResult.Ok ->
                    if (result.value.size == 3) {
                        GMResult.Ok(result.value.copyOf())
                    } else {
                        GMResult.Err(
                            Surface3DError.InvalidCoordinateCount(
                                result.value.size,
                            ),
                        )
                    }
                is GMResult.Err -> GMResult.Err(
                    Surface3DError.CoordinateEvaluation(
                        coordinateIndex = null,
                        parameterU = parameterU,
                        parameterV = parameterV,
                        error = result.error,
                    ),
                )
            }
            is Surface3DSource.Components -> {
                val coordinates = DoubleArray(3)
                val evaluators = listOf(
                    currentSource.x,
                    currentSource.y,
                    currentSource.z,
                )
                for ((index, evaluator) in evaluators.withIndex()) {
                    coordinates[index] = when (
                        val result =
                            evaluator.evaluate(parameterU, parameterV)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return GMResult.Err(
                            Surface3DError.CoordinateEvaluation(
                                coordinateIndex = index,
                                parameterU = parameterU,
                                parameterV = parameterV,
                                error = result.error,
                            ),
                        )
                    }
                }
                GMResult.Ok(coordinates)
            }
            is Surface3DSource.Transformed ->
                when (
                    val result =
                        currentSource.base.evalFResult(parameterU, parameterV)
                ) {
                    is GMResult.Ok ->
                        GMResult.Ok(result.value.copyOfRange(1, 4))
                    is GMResult.Err -> result
                }
        }

    internal companion object {
        internal const val DEFAULT_STEPS_U: Int = 30
        internal const val DEFAULT_STEPS_V: Int = 30
        internal const val MAX_STEPS: Int = Polyhedron3D.MAX_VERTEX_COUNT
        private const val SURFACE_3D_ID_PREFIX = "surface3d"
        private const val SURFACE_3D_ELEMENT_TYPE = "parametricsurface3d"
        private const val FUNCTION_GRAPH_3D_ELEMENT_TYPE = "functiongraph3d"
        private const val WIREFRAME_TYPE = "wireframe"
        private const val COLORMAP_TYPE = "colormap"
        private const val SHADER_TYPE = "shader"
        private const val COLOR_ARRAY_TYPE = "colorarray"
        private const val TRIANGLE_TILING = "triangle"
        private const val RECTANGLE_TILING = "rectangle"

        // JSXGraph: src/3d/surface3d.js ->
        // createParametricSurface3D / createFunctiongraph3D.
        internal fun create(
            view: View3D,
            source: Surface3DSource,
            rangeUSource: List<Line3DCoordinateValue>,
            rangeVSource: List<Line3DCoordinateValue>,
            attributes: Surface3DAttributes = Surface3DAttributes(),
            dependencies: Iterable<GeometryElement> = emptyList(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            functionGraph: Boolean = false,
        ): GMResult<Surface3D, Surface3DError> =
            createInternal(
                view = view,
                source = source,
                rangeUSource = rangeUSource,
                rangeVSource = rangeVSource,
                attributes = normalizedAttributes(attributes),
                dependencies = dependencies,
                baseSurface = null,
                transformations = emptyList(),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                functionGraph = functionGraph,
            )

        // JSXGraph: src/3d/surface3d.js ->
        // createParametricSurface3D transformed form.
        internal fun create(
            view: View3D,
            baseSurface: Surface3D,
            transformations: List<Transformation>,
            attributes: Surface3DAttributes =
                baseSurface.surfaceAttributes,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Surface3D, Surface3DError> {
            if (baseSurface.view !== view) {
                return GMResult.Err(
                    Surface3DError.ParentViewMismatch(parentIndex = 0),
                )
            }
            if (view.board.elementById(baseSurface.id) !== baseSurface) {
                return GMResult.Err(
                    Surface3DError.ParentNotRegistered(
                        parentIndex = 0,
                        id = baseSurface.id,
                    ),
                )
            }
            if (transformations.isEmpty()) {
                return GMResult.Err(
                    Surface3DError.InvalidTransformationCount(0),
                )
            }
            return createInternal(
                view = view,
                source = Surface3DSource.Transformed(baseSurface),
                rangeUSource = baseSurface.rangeUSource,
                rangeVSource = baseSurface.rangeVSource,
                attributes = normalizedAttributes(attributes),
                dependencies = emptyList(),
                baseSurface = baseSurface,
                transformations = transformations,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                functionGraph = false,
            )
        }

        private fun createInternal(
            view: View3D,
            source: Surface3DSource,
            rangeUSource: List<Line3DCoordinateValue>,
            rangeVSource: List<Line3DCoordinateValue>,
            attributes: Surface3DAttributes,
            dependencies: Iterable<GeometryElement>,
            baseSurface: Surface3D?,
            transformations: List<Transformation>,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
            functionGraph: Boolean,
        ): GMResult<Surface3D, Surface3DError> {
            validateAttributes(attributes)?.let {
                return GMResult.Err(it)
            }
            if (rangeUSource.size != 2) {
                return GMResult.Err(
                    Surface3DError.InvalidRangeCount(0, rangeUSource.size),
                )
            }
            if (rangeVSource.size != 2) {
                return GMResult.Err(
                    Surface3DError.InvalidRangeCount(1, rangeVSource.size),
                )
            }
            if (id.isNotEmpty() && view.board.elementById(id) != null) {
                return GMResult.Err(
                    Surface3DError.Registration(
                        BoardError.DuplicateElementId(id),
                    ),
                )
            }
            val surface = Surface3D(
                view = view,
                source = source,
                rangeUSource = rangeUSource,
                rangeVSource = rangeVSource,
                surfaceAttributes = attributes,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                functionGraph = functionGraph,
            )
            if (baseSurface != null) {
                surface.addTransform(baseSurface, transformations)
            }
            when (val result = surface.updateCoordsResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (
                val registration =
                    view.board.setId(surface, SURFACE_3D_ID_PREFIX)
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Surface3DError.Registration(registration.error),
                )
            }
            surface.registerInView()
            val projected =
                if (attributes.surfaceType == WIREFRAME_TYPE) {
                    surface.updateDataArray2D()
                } else {
                    CurveDataUpdate(DoubleArray(0), DoubleArray(0))
                }
            val proxy = when (
                val result = Curve.createData(
                    board = view.board,
                    dataX = projected.x,
                    dataY = projected.y,
                    name = surface.name,
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    view.board.removeObject(surface)
                    return GMResult.Err(
                        Surface3DError.ProxyCurveFactory(result.error),
                    )
                }
            }
            proxy.dump = false
            proxy.isDraggable = false
            proxy.setParents(listOf(surface))
            surface.curve2D = proxy
            surface.element2D = proxy
            surface.addChild(proxy)
            surface.inherits += proxy

            if (attributes.surfaceType != WIREFRAME_TYPE) {
                val definition = when (
                    val result = surfaceDefinition(surface, attributes)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        view.board.removeObject(surface)
                        return result
                    }
                }
                val faceInputs = definition.second.mapIndexed {
                        faceNumber,
                        vertexKeys,
                    ->
                    Polyhedron3DFaceInput(
                        vertexKeys = vertexKeys,
                        attributes = surfaceFaceAttributes(
                            attributes = attributes,
                            faceNumber = faceNumber,
                        ),
                    )
                }
                val polyhedron = when (
                    val result = Polyhedron3D.create(
                        view = view,
                        vertices = definition.first,
                        faceInputs = faceInputs,
                        dependencies = listOf(surface),
                        name = "",
                        needsRegularUpdate = needsRegularUpdate,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        view.board.removeObject(surface)
                        return GMResult.Err(
                            Surface3DError.Polyhedron3DFactory(result.error),
                        )
                    }
                }
                polyhedron.setParents(listOf(surface))
                surface.polyhedron = polyhedron
                surface.inherits += polyhedron
            }
            if (baseSurface != null) {
                surface.setParents(listOf(baseSurface))
                baseSurface.addChild(surface)
            }
            for (dependency in dependencies.distinctBy(GeometryElement::id)) {
                dependency.addChild(surface)
            }
            surface.prepareUpdate().update()
            return GMResult.Ok(surface)
        }

        private fun normalizedAttributes(
            attributes: Surface3DAttributes,
        ): Surface3DAttributes =
            attributes.copy(
                surfaceType = attributes.surfaceType.lowercase(),
                tiling = attributes.tiling.lowercase(),
            )

        private fun validateAttributes(
            attributes: Surface3DAttributes,
        ): Surface3DError? {
            if (
                attributes.surfaceType !in
                setOf(
                    WIREFRAME_TYPE,
                    COLORMAP_TYPE,
                    SHADER_TYPE,
                    COLOR_ARRAY_TYPE,
                )
            ) {
                return Surface3DError.InvalidSurfaceType(
                    attributes.surfaceType,
                )
            }
            if (
                attributes.tiling !in
                setOf(TRIANGLE_TILING, RECTANGLE_TILING)
            ) {
                return Surface3DError.InvalidTiling(attributes.tiling)
            }
            val minimumU = if (
                attributes.surfaceType == WIREFRAME_TYPE
            ) {
                0
            } else {
                1
            }
            val minimumV = if (
                attributes.surfaceType == WIREFRAME_TYPE ||
                attributes.tiling == TRIANGLE_TILING
            ) {
                0
            } else {
                1
            }
            if (attributes.stepsU !in minimumU..MAX_STEPS) {
                return Surface3DError.InvalidSteps(
                    axis = "u",
                    count = attributes.stepsU,
                    minimum = minimumU,
                )
            }
            if (attributes.stepsV !in minimumV..MAX_STEPS) {
                return Surface3DError.InvalidSteps(
                    axis = "v",
                    count = attributes.stepsV,
                    minimum = minimumV,
                )
            }
            if (attributes.surfaceType == WIREFRAME_TYPE) {
                val rowCount = maxOf(attributes.stepsU, 1).toLong() + 1L
                val columnCount =
                    maxOf(attributes.stepsV, 1).toLong() + 1L
                val projectedPointCount =
                    (
                        if (attributes.stepsU > 0) {
                            rowCount * columnCount
                        } else {
                            0L
                        }
                        ) +
                        (
                            if (attributes.stepsV > 0) {
                                rowCount * columnCount
                            } else {
                                0L
                            }
                            ) +
                        rowCount +
                        columnCount
                if (projectedPointCount > Curve.MAX_SAMPLE_COUNT) {
                    return Surface3DError.CurvePointLimitExceeded(
                        count = projectedPointCount,
                        maximum = Curve.MAX_SAMPLE_COUNT,
                    )
                }
            }
            return null
        }

        private fun surfaceDefinition(
            surface: Surface3D,
            attributes: Surface3DAttributes,
        ): GMResult<
            Pair<
                LinkedHashMap<String, Polyhedron3DVertexSource>,
                List<List<String>>,
                >,
            Surface3DError,
            > {
            val stepsV = when {
                attributes.tiling != TRIANGLE_TILING ->
                    attributes.stepsV
                attributes.stepsV != 0 -> attributes.stepsV
                else -> equilateralStepsV(surface, attributes.stepsU)
            }
            val triangular = attributes.tiling == TRIANGLE_TILING
            val oddRows = (stepsV.toLong() + 1L) / 2L
            val rowCount = stepsV.toLong() + 1L
            val vertexCount =
                rowCount * (attributes.stepsU.toLong() + 1L) +
                    if (triangular) oddRows else 0L
            val faceCount =
                if (triangular) {
                    val oddFaceRows = (stepsV.toLong() + 1L) / 2L
                    val evenFaceRows = stepsV.toLong() / 2L
                    oddFaceRows *
                        (2L * (attributes.stepsU.toLong() + 1L)) +
                        evenFaceRows *
                        (2L * attributes.stepsU.toLong() + 1L)
                } else {
                    attributes.stepsU.toLong() * stepsV.toLong()
                }
            if (vertexCount > Polyhedron3D.MAX_VERTEX_COUNT) {
                return GMResult.Err(
                    Surface3DError.VertexLimitExceeded(
                        count = vertexCount,
                        maximum = Polyhedron3D.MAX_VERTEX_COUNT,
                    ),
                )
            }
            if (faceCount > Polyhedron3D.MAX_FACE_COUNT) {
                return GMResult.Err(
                    Surface3DError.FaceLimitExceeded(
                        count = faceCount,
                        maximum = Polyhedron3D.MAX_FACE_COUNT,
                    ),
                )
            }
            return if (triangular) {
                triangularSurfaceDefinition(
                    surface = surface,
                    stepsU = attributes.stepsU,
                    stepsV = stepsV,
                )
            } else {
                rectangularSurfaceDefinition(
                    surface = surface,
                    stepsU = attributes.stepsU,
                    stepsV = stepsV,
                )
            }
        }

        private fun equilateralStepsV(
            surface: Surface3D,
            stepsU: Int,
        ): Int {
            val deltaU =
                (
                    surface.evaluatedRangeU[1] -
                        surface.evaluatedRangeU[0]
                    ) / stepsU
            val deltaV = deltaU * sqrt(3.0) / 2.0
            return (
                abs(
                    surface.evaluatedRangeV[1] -
                        surface.evaluatedRangeV[0],
                ) / deltaV
                ).roundToInt()
        }

        private fun rectangularSurfaceDefinition(
            surface: Surface3D,
            stepsU: Int,
            stepsV: Int,
        ): GMResult<
            Pair<
                LinkedHashMap<String, Polyhedron3DVertexSource>,
                List<List<String>>,
                >,
            Surface3DError,
            > {
            val vertices =
                linkedMapOf<String, Polyhedron3DVertexSource>()
            val faces = mutableListOf<List<String>>()
            for (row in 0..stepsV) {
                for (column in 0..stepsU) {
                    vertices[vertices.size.toString()] = surfaceVertex(
                        surface = surface,
                        column = column.toDouble(),
                        row = row,
                        stepsU = stepsU,
                        stepsV = stepsV,
                    )
                    if (column > 0 && row > 0) {
                        val last = vertices.size - 1
                        faces += listOf(
                            (last - 1).toString(),
                            last.toString(),
                            (last - 1 - stepsU).toString(),
                            (last - 2 - stepsU).toString(),
                        )
                    }
                }
            }
            return GMResult.Ok(vertices to faces)
        }

        private fun triangularSurfaceDefinition(
            surface: Surface3D,
            stepsU: Int,
            stepsV: Int,
        ): GMResult<
            Pair<
                LinkedHashMap<String, Polyhedron3DVertexSource>,
                List<List<String>>,
                >,
            Surface3DError,
            > {
            val vertices =
                linkedMapOf<String, Polyhedron3DVertexSource>()
            val faces = mutableListOf<List<String>>()
            for (row in 0..stepsV) {
                val lastColumn =
                    if (row % 2 == 0) stepsU else stepsU + 1
                for (column in 0..lastColumn) {
                    val shiftedColumn =
                        if (row % 2 == 1) {
                            when {
                                column == lastColumn ->
                                    (column - 1).toDouble()
                                column > 0 -> column - 0.5
                                else -> column.toDouble()
                            }
                        } else {
                            column.toDouble()
                        }
                    vertices[vertices.size.toString()] = surfaceVertex(
                        surface = surface,
                        column = shiftedColumn,
                        row = row,
                        stepsU = stepsU,
                        stepsV = stepsV,
                    )
                    if (row > 0) {
                        val last = vertices.size - 1
                        if (row % 2 == 1) {
                            if (column > 0) {
                                val first = listOf(
                                    (last - 1).toString(),
                                    last.toString(),
                                    (last - 2 - stepsU).toString(),
                                )
                                faces += first
                                faces +=
                                    if (column < lastColumn) {
                                        listOf(
                                            last.toString(),
                                            (last - 1 - stepsU).toString(),
                                            (last - 2 - stepsU).toString(),
                                        )
                                    } else {
                                        first
                                    }
                            }
                        } else {
                            if (column > 0) {
                                faces += listOf(
                                    last.toString(),
                                    (last - 2 - stepsU).toString(),
                                    (last - 1).toString(),
                                )
                            }
                            faces += listOf(
                                last.toString(),
                                (last - 1 - stepsU).toString(),
                                (last - 2 - stepsU).toString(),
                            )
                        }
                    }
                }
            }
            return GMResult.Ok(vertices to faces)
        }

        private fun surfaceVertex(
            surface: Surface3D,
            column: Double,
            row: Int,
            stepsU: Int,
            stepsV: Int,
        ): Polyhedron3DVertexSource =
            Polyhedron3DVertexSource.Function(
                Line3DArrayEvaluator {
                    val u =
                        surface.evaluatedRangeU[0] +
                            column *
                            (
                                surface.evaluatedRangeU[1] -
                                    surface.evaluatedRangeU[0]
                                ) / stepsU
                    val v =
                        surface.evaluatedRangeV[0] +
                            row *
                            (
                                surface.evaluatedRangeV[1] -
                                    surface.evaluatedRangeV[0]
                                ) / stepsV
                    when (val result = surface.evalFResult(u, v)) {
                        is GMResult.Ok ->
                            GMResult.Ok(result.value.copyOfRange(1, 4))
                        is GMResult.Err -> GMResult.Err(
                            Line3DDynamicError.Rejected(
                                result.error.toString(),
                            ),
                        )
                    }
                },
            )

        private fun surfaceFaceAttributes(
            attributes: Surface3DAttributes,
            faceNumber: Int,
        ): Face3DAttributes {
            val base = attributes.faceAttributes
            if (attributes.surfaceType == COLORMAP_TYPE) {
                val colormap = attributes.colormap
                return base.copy(
                    shader = base.shader.copy(enabled = false),
                    fillColorEvaluator = Face3DFillColorEvaluator { face ->
                        var height = 0.0
                        val keys =
                            face.polyhedron.faceKeys[face.faceNumber]
                        for (key in keys) {
                            height +=
                                face.polyhedron.coords[key]?.get(3)
                                    ?: Double.NaN
                        }
                        if (keys.isNotEmpty()) {
                            height /= keys.size
                        }
                        val hue =
                            colormap.minimumHue +
                                (
                                    height - colormap.minimumHeight
                                    ) * (
                                    colormap.maximumHue -
                                        colormap.minimumHue
                                    ) / (
                                    colormap.maximumHeight -
                                        colormap.minimumHeight
                                    )
                        if (hue.isFinite()) {
                            Face3DColor.hsvToHex(
                                hue = hue,
                                saturation = colormap.saturation,
                                value = colormap.value,
                            )
                        } else {
                            base.fillColor
                        }
                    },
                )
            }
            val fillColors = attributes.fillColorArray
            return base.copy(
                fillColor =
                    if (fillColors.isEmpty()) {
                        base.fillColor
                    } else {
                        fillColors[faceNumber % fillColors.size]
                    },
                shader = base.shader.copy(
                    enabled = attributes.surfaceType == SHADER_TYPE,
                ),
                fillColorEvaluator = null,
            )
        }
    }
}
