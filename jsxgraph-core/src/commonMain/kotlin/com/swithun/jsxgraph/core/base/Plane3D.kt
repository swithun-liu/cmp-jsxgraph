/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/linspace3d.js -> Plane3D / createPlane3D
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal sealed interface Plane3DDirectionSource {
    data class Points(
        val point1: Point3D,
        val point2: Point3D,
    ) : Plane3DDirectionSource

    data class Line(
        val line: Line3D,
    ) : Plane3DDirectionSource

    data class Values(
        val values: List<Line3DCoordinateValue>,
    ) : Plane3DDirectionSource

    data class Function(
        val evaluator: Line3DArrayEvaluator,
    ) : Plane3DDirectionSource
}

internal data class Plane3DColormapAttributes(
    val minimumHeight: Double = -5.0,
    val minimumHue: Double = 190.0,
    val maximumHeight: Double = 5.0,
    val maximumHue: Double = 0.0,
    val saturation: Double = 0.9,
    val value: Double = 0.9,
)

internal data class Plane3DSurfaceAttributes(
    val tiling: String = "rectangle",
    val stepsU: Int = 6,
    val stepsV: Int = 6,
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
) {
    internal companion object {
        // JSXGraph: src/options3d.js -> axes3d.{x,y,z}Plane{Rear,Front}.
        internal fun axes3DDefaults(
            visible: Boolean,
        ): Plane3DSurfaceAttributes =
            Plane3DSurfaceAttributes(
                stepsU = 10,
                stepsV = 10,
                fillColorArray = listOf("#e7e7e7"),
                faceAttributes = Face3DAttributes(
                    visible = visible,
                    strokeColor = "#cccccc",
                    strokeWidth = 0.5,
                    strokeOpacity = 0.7,
                    fillOpacity = 0.3,
                    shader = Face3DShaderAttributes(
                        enabled = true,
                        fixed = true,
                        type = "zIndex",
                        hue = 0.0,
                        saturation = 0.0,
                        minimumLightness = 65.0,
                        maximumLightness = 98.0,
                    ),
                ),
            )
    }
}

internal sealed interface Plane3DError {
    data class ParentViewMismatch(
        val parentIndex: Int,
    ) : Plane3DError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : Plane3DError

    data class InvalidDirectionCount(
        val directionIndex: Int,
        val count: Int,
    ) : Plane3DError

    data class InvalidRangeCount(
        val rangeIndex: Int,
        val count: Int,
    ) : Plane3DError

    data class DirectionEvaluation(
        val directionIndex: Int,
        val coordinateIndex: Int?,
        val error: Line3DDynamicError,
    ) : Plane3DError

    data class RangeEvaluation(
        val rangeIndex: Int,
        val coordinateIndex: Int,
        val error: Line3DDynamicError,
    ) : Plane3DError

    data class InvalidTransformationCount(
        val count: Int,
    ) : Plane3DError

    data class InvalidBaseElement(
        val baseElementId: String?,
    ) : Plane3DError

    data class TransformationEvaluation(
        val transformationIndex: Int,
        val vectorIndex: Int,
        val error: TransformationError,
    ) : Plane3DError

    data class Registration(
        val error: BoardError,
    ) : Plane3DError

    data class PointFactory(
        val error: Point3DError,
    ) : Plane3DError

    data class ProxyCurveFactory(
        val error: CurveError,
    ) : Plane3DError

    data class Mesh3DFactory(
        val error: Mesh3DError,
    ) : Plane3DError

    data class InvalidSurfaceSteps(
        val axis: String,
        val count: Int,
    ) : Plane3DError

    data class SurfaceVertexLimitExceeded(
        val count: Long,
        val maximum: Int,
    ) : Plane3DError

    data class SurfaceFaceLimitExceeded(
        val count: Long,
        val maximum: Int,
    ) : Plane3DError

    data class Polyhedron3DFactory(
        val error: Polyhedron3DError,
    ) : Plane3DError
}

/**
 * Faithful Plane3D lifecycle translated from JSXGraph 1.13.3.
 *
 * The plane owns an ordinary 2D curve proxy. Finite planes project their four
 * range corners. Fully infinite planes use the same Hesse plane intersection
 * construction as View3D.intersectionPlanePlane to clip against the six faces
 * of the View3D bounding box.
 */
internal class Plane3D private constructor(
    view: View3D,
    internal val point: Point3D,
    private val direction1Source: Plane3DDirectionSource,
    private val direction2Source: Plane3DDirectionSource,
    private val rangeUSource: MutableList<Line3DCoordinateValue>,
    private val rangeVSource: MutableList<Line3DCoordinateValue>,
    internal val planeType: String,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    internal val isFixed: Boolean,
) : GeometryElement3D(
    view = view,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_PLANE3D,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal var vec1: DoubleArray = DoubleArray(4)
        private set
    internal var vec2: DoubleArray = DoubleArray(4)
        private set
    internal var evaluatedRangeU: DoubleArray =
        doubleArrayOf(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        private set
    internal var evaluatedRangeV: DoubleArray =
        doubleArrayOf(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        private set
    internal var normal: DoubleArray = DoubleArray(4)
        private set
    internal var d: Double = 0.0
        private set
    internal var dataX: DoubleArray = DoubleArray(0)
        private set
    internal var dataY: DoubleArray = DoubleArray(0)
        private set
    internal lateinit var outline2D: Curve
        private set
    internal var mesh3D: Curve? = null
        private set
    internal var surface3D: Polyhedron3D? = null
        private set
    internal var directionEvaluationError: Plane3DError? = null
        private set
    internal var rangeEvaluationError: Plane3DError? = null
        private set
    internal var transformationEvaluationError: Plane3DError? = null
        private set
    private var usesSurfaceMapping: Boolean = false

    init {
        elType = PLANE_3D_ELEMENT_TYPE
    }

    // JSXGraph: src/3d/linspace3d.js -> Plane3D.F.
    internal fun F(
        u: Double,
        v: Double,
    ): DoubleArray {
        if (usesSurfaceMapping) {
            return DoubleArray(4) { index ->
                point.coords[index] + u * vec1[index] + v * vec2[index]
            }
        }
        val first = normalizedDirection(vec1)
        val second = normalizedDirection(vec2)
        return doubleArrayOf(
            1.0,
            point.X() + u * first[0] + v * second[0],
            point.Y() + u * first[1] + v * second[1],
            point.Z() + u * first[2] + v * second[2],
        )
    }

    @Suppress("FunctionName")
    internal fun X(
        u: Double,
        v: Double,
    ): Double = F(u, v)[1]

    @Suppress("FunctionName")
    internal fun Y(
        u: Double,
        v: Double,
    ): Double = F(u, v)[2]

    @Suppress("FunctionName")
    internal fun Z(
        u: Double,
        v: Double,
    ): Double = F(u, v)[3]

    // JSXGraph: src/3d/linspace3d.js -> Plane3D.updateCoords.
    internal fun updateCoords(): Plane3D {
        val first = evaluateDirection(
            source = direction1Source,
            directionIndex = 0,
        )
        val second = evaluateDirection(
            source = direction2Source,
            directionIndex = 1,
        )
        if (first is GMResult.Ok && second is GMResult.Ok) {
            vec1 = first.value
            vec2 = second.value
            directionEvaluationError = null
        } else {
            vec1 = DoubleArray(4) { Double.NaN }
            vec2 = DoubleArray(4) { Double.NaN }
            directionEvaluationError = when {
                first is GMResult.Err -> first.error
                second is GMResult.Err -> second.error
                else -> null
            }
        }

        val firstRange = evaluateRange(
            source = rangeUSource,
            rangeIndex = 0,
        )
        val secondRange = evaluateRange(
            source = rangeVSource,
            rangeIndex = 1,
        )
        if (firstRange is GMResult.Ok && secondRange is GMResult.Ok) {
            evaluatedRangeU = firstRange.value
            evaluatedRangeV = secondRange.value
            rangeEvaluationError = null
        } else {
            evaluatedRangeU = DoubleArray(2) { Double.NaN }
            evaluatedRangeV = DoubleArray(2) { Double.NaN }
            rangeEvaluationError = when {
                firstRange is GMResult.Err -> firstRange.error
                secondRange is GMResult.Err -> secondRange.error
                else -> null
            }
        }
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Plane3D.updateNormal.
    internal fun updateNormal(): Plane3D {
        val affineNormal = Mat.crossProduct(
            vec1.copyOfRange(1, 4),
            vec2.copyOfRange(1, 4),
        )
        val length = Mat.norm(affineNormal)
        if (abs(length) > Mat.eps * Mat.eps) {
            for (index in affineNormal.indices) {
                affineNormal[index] /= length
            }
        }
        normal = doubleArrayOf(
            0.0,
            affineNormal[0],
            affineNormal[1],
            affineNormal[2],
        )
        d = Mat.innerProduct(point.coords, normal, 4)
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Plane3D.updateDataArray.
    internal fun updateDataArray(): Plane3D {
        updateNormal()
        val projected =
            if (isFullyInfinite()) {
                infiniteOutline()
            } else {
                boundedOutline()
            }
        dataX = DoubleArray(projected.size) { projected[it][1] }
        dataY = DoubleArray(projected.size) { projected[it][2] }
        if (this::outline2D.isInitialized && !usesSurfaceMapping) {
            outline2D.replaceData(dataX, dataY)
        }
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Plane3D.addTransform.
    internal fun addTransform(
        element: Plane3D,
        transformation: Transformation,
    ): Plane3D = addTransform(element, listOf(transformation))

    internal fun addTransform(
        element: Plane3D,
        newTransformations: Iterable<Transformation>,
    ): Plane3D {
        addTransformGeneric(element, newTransformations)
        point.addTransform(element.point, newTransformations)
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Plane3D.removeTransform.
    internal fun removeTransform(
        transformation: Transformation,
    ): Plane3D {
        removeTransformGeneric(listOf(transformation))
        point.removeTransform(transformation)
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Plane3D.clearTransforms.
    internal fun clearTransforms(): Plane3D {
        clearTransformsGeneric()
        point.clearTransforms()
        transformationEvaluationError = null
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Plane3D.updateTransform.
    internal fun updateTransform(): Plane3D {
        if (transformations.isEmpty() || baseElement == null) {
            transformationEvaluationError = null
            return this
        }
        val basePlane = baseElement as? Plane3D
        if (basePlane == null) {
            vec1 = DoubleArray(4) { Double.NaN }
            vec2 = DoubleArray(4) { Double.NaN }
            transformationEvaluationError =
                Plane3DError.InvalidBaseElement(baseElement?.id)
            return this
        }
        var first =
            if (basePlane === this) vec1.copyOf() else basePlane.vec1.copyOf()
        var second =
            if (basePlane === this) vec2.copyOf() else basePlane.vec2.copyOf()
        for ((index, transformation) in transformations.withIndex()) {
            first = when (val result = transformation.applyResult(first)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    vec1 = DoubleArray(4) { Double.NaN }
                    vec2 = DoubleArray(4) { Double.NaN }
                    transformationEvaluationError =
                        Plane3DError.TransformationEvaluation(
                            transformationIndex = index,
                            vectorIndex = 0,
                            error = result.error,
                        )
                    return this
                }
            }
            second = when (val result = transformation.applyResult(second)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    vec1 = DoubleArray(4) { Double.NaN }
                    vec2 = DoubleArray(4) { Double.NaN }
                    transformationEvaluationError =
                        Plane3DError.TransformationEvaluation(
                            transformationIndex = index,
                            vectorIndex = 1,
                            error = result.error,
                        )
                    return this
                }
            }
        }
        vec1 = first
        vec2 = second
        transformationEvaluationError = null
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Plane3D.update.
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        updateCoords()
        if (
            directionEvaluationError == null &&
            rangeEvaluationError == null
        ) {
            updateTransform()
        }
        if (transformationEvaluationError == null) {
            updateDataArray()
        } else {
            dataX = DoubleArray(0)
            dataY = DoubleArray(0)
            if (this::outline2D.isInitialized) {
                outline2D.replaceData(dataX, dataY)
            }
        }
        return this
    }

    override fun updateRenderer(): GeometryElement {
        needsUpdate = false
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Plane3D.projectCoords,
    // src/math/geometry.js -> projectCoordsToParametric.
    internal fun projectCoords(
        coordinates: DoubleArray,
        parameters: DoubleArray,
    ): DoubleArray {
        val affine =
            if (coordinates.size == 4) {
                coordinates.copyOfRange(1, 4)
            } else {
                coordinates.copyOf(3)
            }
        val difference = doubleArrayOf(
            affine[0] - point.X(),
            affine[1] - point.Y(),
            affine[2] - point.Z(),
        )
        val first = parameterDirection(vec1)
        val second = parameterDirection(vec2)
        val firstFirst = Mat.innerProduct(first, first, 3)
        val firstSecond = Mat.innerProduct(first, second, 3)
        val secondSecond = Mat.innerProduct(second, second, 3)
        val determinant =
            firstFirst * secondSecond - firstSecond * firstSecond
        if (abs(determinant) <= Mat.eps * Mat.eps) {
            if (parameters.isNotEmpty()) {
                parameters[0] = Double.NaN
            }
            if (parameters.size > 1) {
                parameters[1] = Double.NaN
            }
            return DoubleArray(4) { Double.NaN }
        }
        val firstDifference = Mat.innerProduct(first, difference, 3)
        val secondDifference = Mat.innerProduct(second, difference, 3)
        val u = clamp(
            (
                firstDifference * secondSecond -
                    secondDifference * firstSecond
                ) / determinant,
            evaluatedRangeU,
        )
        val v = clamp(
            (
                secondDifference * firstFirst -
                    firstDifference * firstSecond
                ) / determinant,
            evaluatedRangeV,
        )
        if (parameters.isNotEmpty()) {
            parameters[0] = u
        }
        if (parameters.size > 1) {
            parameters[1] = v
        }
        return F(u, v)
    }

    private fun parameterDirection(direction: DoubleArray): DoubleArray =
        if (usesSurfaceMapping) {
            direction.copyOfRange(1, 4)
        } else {
            normalizedDirection(direction).copyOfRange(0, 3)
        }

    private fun evaluateDirection(
        source: Plane3DDirectionSource,
        directionIndex: Int,
    ): GMResult<DoubleArray, Plane3DError> =
        when (source) {
            is Plane3DDirectionSource.Points -> GMResult.Ok(
                doubleArrayOf(
                    0.0,
                    source.point2.X() - source.point1.X(),
                    source.point2.Y() - source.point1.Y(),
                    source.point2.Z() - source.point1.Z(),
                ),
            )
            is Plane3DDirectionSource.Line ->
                GMResult.Ok(source.line.vec.copyOf())
            is Plane3DDirectionSource.Function -> {
                val evaluated = when (val result = source.evaluator.evaluate()) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        Plane3DError.DirectionEvaluation(
                            directionIndex = directionIndex,
                            coordinateIndex = null,
                            error = result.error,
                        ),
                    )
                }
                homogeneousDirection(evaluated, directionIndex)
            }
            is Plane3DDirectionSource.Values -> {
                if (source.values.size !in setOf(3, 4)) {
                    return GMResult.Err(
                        Plane3DError.InvalidDirectionCount(
                            directionIndex = directionIndex,
                            count = source.values.size,
                        ),
                    )
                }
                val direction = DoubleArray(4)
                val offset = if (source.values.size == 3) 1 else 0
                for ((index, value) in source.values.withIndex()) {
                    direction[offset + index] = when (value) {
                        is Line3DCoordinateValue.Numeric -> value.value
                        is Line3DCoordinateValue.Dynamic -> when (
                            val result = value.evaluator.evaluate()
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return GMResult.Err(
                                Plane3DError.DirectionEvaluation(
                                    directionIndex = directionIndex,
                                    coordinateIndex = index,
                                    error = result.error,
                                ),
                            )
                        }
                    }
                }
                GMResult.Ok(direction)
            }
        }

    private fun homogeneousDirection(
        direction: DoubleArray,
        directionIndex: Int,
    ): GMResult<DoubleArray, Plane3DError> =
        when (direction.size) {
            3 -> GMResult.Ok(
                doubleArrayOf(
                    0.0,
                    direction[0],
                    direction[1],
                    direction[2],
                ),
            )
            4 -> GMResult.Ok(direction.copyOf())
            else -> GMResult.Err(
                Plane3DError.InvalidDirectionCount(
                    directionIndex = directionIndex,
                    count = direction.size,
                ),
            )
        }

    private fun evaluateRange(
        source: List<Line3DCoordinateValue>,
        rangeIndex: Int,
    ): GMResult<DoubleArray, Plane3DError> {
        if (source.size != 2) {
            return GMResult.Err(
                Plane3DError.InvalidRangeCount(
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
                        Plane3DError.RangeEvaluation(
                            rangeIndex = rangeIndex,
                            coordinateIndex = index,
                            error = evaluated.error,
                        ),
                    )
                }
            }
        }
        return GMResult.Ok(result)
    }

    private fun boundedOutline(): List<DoubleArray> {
        val first = normalizedDirection(vec1)
        val second = normalizedDirection(vec2)
        val parameters = arrayOf(
            doubleArrayOf(evaluatedRangeU[0], evaluatedRangeV[0]),
            doubleArrayOf(evaluatedRangeU[1], evaluatedRangeV[0]),
            doubleArrayOf(evaluatedRangeU[1], evaluatedRangeV[1]),
            doubleArrayOf(evaluatedRangeU[0], evaluatedRangeV[1]),
        )
        val projected = parameters.map { parameter ->
            val coordinates = DoubleArray(4) { index ->
                point.coords[index] +
                    parameter[0] * first[index] +
                    parameter[1] * second[index]
            }
            view.project3DTo2D(coordinates)
        }.toMutableList()
        projected.firstOrNull()?.let { projected += it.copyOf() }
        return projected
    }

    private fun normalizedDirection(direction: DoubleArray): DoubleArray {
        val normalized = direction.copyOf()
        val length = Mat.norm(normalized, 4)
        for (index in 1 until 4) {
            normalized[index] /= length
        }
        return normalized
    }

    private fun infiniteOutline(): List<DoubleArray> {
        val segments = mutableListOf<Array<DoubleArray>>()
        for (dimension in 0 until 3) {
            addBoxIntersection(
                segments = segments,
                dimension = dimension,
                coordinate = view.bbox3D[dimension][0],
            )
            addBoxIntersection(
                segments = segments,
                dimension = dimension,
                coordinate = view.bbox3D[dimension][1],
            )
        }
        if (segments.isEmpty()) {
            return emptyList()
        }

        val projected = mutableListOf<DoubleArray>()
        val first = 0
        var position = first
        var endpointIndex = 0
        var steps = 0
        do {
            val endpoint = segments[position][endpointIndex]
            projected += view.project3DTo2D(endpoint)
            endpointIndex = (endpointIndex + 1) % 2
            val next = segments[position][endpointIndex]
            val previousPosition = position
            for (candidateIndex in segments.indices) {
                if (
                    candidateIndex != position &&
                    distance(next, segments[candidateIndex][0]) <
                    INTERSECTION_EPSILON
                ) {
                    position = candidateIndex
                    endpointIndex = 0
                    break
                }
                if (
                    candidateIndex != position &&
                    distance(next, segments[candidateIndex][1]) <
                    INTERSECTION_EPSILON
                ) {
                    position = candidateIndex
                    endpointIndex = 1
                    break
                }
            }
            if (position == previousPosition) {
                break
            }
            steps += 1
        } while (position != first && steps <= segments.size)

        projected.firstOrNull()?.let { projected += it.copyOf() }
        return projected
    }

    private fun addBoxIntersection(
        segments: MutableList<Array<DoubleArray>>,
        dimension: Int,
        coordinate: Double,
    ) {
        val frame = cubeFaceFrame(dimension, coordinate)
        val intersection = view.intersectionPlanePlane(
            firstNormal = normal,
            firstDistance = d,
            firstVector1 = vec1,
            firstVector2 = vec2,
            secondNormal = frame.normal,
            secondDistance = frame.distance,
            secondVector1 = frame.vector1,
            secondVector2 = frame.vector2,
        )
        val first = intersection[0] ?: return
        val second = intersection[1] ?: return
        val duplicate = segments.any { existing ->
            (
                distance(first, existing[0]) < INTERSECTION_EPSILON &&
                    distance(second, existing[1]) < INTERSECTION_EPSILON
                ) ||
                (
                    distance(first, existing[1]) < INTERSECTION_EPSILON &&
                        distance(second, existing[0]) < INTERSECTION_EPSILON
                    )
        }
        if (!duplicate) {
            segments += arrayOf(first, second)
        }
    }

    private fun cubeFaceFrame(
        dimension: Int,
        coordinate: Double,
    ): CubeFaceFrame =
        when (dimension) {
            0 -> CubeFaceFrame(
                normal = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
                distance = coordinate,
                vector1 = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
                vector2 = doubleArrayOf(0.0, 0.0, 0.0, 1.0),
            )
            1 -> CubeFaceFrame(
                normal = doubleArrayOf(0.0, 0.0, -1.0, 0.0),
                distance = -coordinate,
                vector1 = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
                vector2 = doubleArrayOf(0.0, 0.0, 0.0, 1.0),
            )
            else -> CubeFaceFrame(
                normal = doubleArrayOf(0.0, 0.0, 0.0, 1.0),
                distance = coordinate,
                vector1 = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
                vector2 = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
            )
        }

    private fun isFullyInfinite(): Boolean =
        evaluatedRangeU[0] == Double.NEGATIVE_INFINITY &&
            evaluatedRangeU[1] == Double.POSITIVE_INFINITY &&
            evaluatedRangeV[0] == Double.NEGATIVE_INFINITY &&
            evaluatedRangeV[1] == Double.POSITIVE_INFINITY

    private fun clamp(
        value: Double,
        range: DoubleArray,
    ): Double = min(max(value, range[0]), range[1])

    private fun distance(
        first: DoubleArray,
        second: DoubleArray,
    ): Double {
        var sum = 0.0
        for (index in 0 until min(first.size, second.size)) {
            val difference = first[index] - second[index]
            sum += difference * difference
        }
        return sqrt(sum)
    }

    private data class CubeFaceFrame(
        val normal: DoubleArray,
        val distance: Double,
        val vector1: DoubleArray,
        val vector2: DoubleArray,
    )

    internal companion object {
        private const val PLANE_3D_ID_PREFIX = "plane3d"
        private const val PLANE_3D_ELEMENT_TYPE = "plane3d"
        private const val INTERSECTION_EPSILON = 1.0e-12

        // JSXGraph: src/3d/linspace3d.js -> createPlane3D,
        // point-direction-direction-range-range form.
        internal fun create(
            view: View3D,
            point: Point3D,
            direction1Source: Plane3DDirectionSource,
            direction2Source: Plane3DDirectionSource,
            rangeUSource: List<Line3DCoordinateValue> = defaultRange(),
            rangeVSource: List<Line3DCoordinateValue> = defaultRange(),
            ownsPoint: Boolean = false,
            dependencies: Iterable<GeometryElement> = emptyList(),
            planeType: String = DEFAULT_PLANE_TYPE,
            meshStepWidthU: Double = 1.0,
            meshStepWidthV: Double = 1.0,
            surfaceAttributes: Plane3DSurfaceAttributes =
                Plane3DSurfaceAttributes(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = true,
        ): GMResult<Plane3D, Plane3DError> =
            createInternal(
                view = view,
                point = point,
                direction1Source = direction1Source,
                direction2Source = direction2Source,
                rangeUSource = rangeUSource,
                rangeVSource = rangeVSource,
                definingPoints = listOf(point),
                ownedPoints = if (ownsPoint) listOf(point) else emptyList(),
                dependencies = dependencies,
                planeType = planeType,
                meshStepWidthU = meshStepWidthU,
                meshStepWidthV = meshStepWidthV,
                surfaceAttributes = surfaceAttributes,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )

        // JSXGraph: src/3d/linspace3d.js -> createPlane3D,
        // three-point form.
        internal fun create(
            view: View3D,
            point1: Point3D,
            point2: Point3D,
            point3: Point3D,
            ownsPoint1: Boolean = false,
            ownsPoint2: Boolean = false,
            ownsPoint3: Boolean = false,
            rangeUSource: List<Line3DCoordinateValue> = defaultRange(),
            rangeVSource: List<Line3DCoordinateValue> = defaultRange(),
            dependencies: Iterable<GeometryElement> = emptyList(),
            planeType: String = DEFAULT_PLANE_TYPE,
            meshStepWidthU: Double = 1.0,
            meshStepWidthV: Double = 1.0,
            surfaceAttributes: Plane3DSurfaceAttributes =
                Plane3DSurfaceAttributes(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = true,
        ): GMResult<Plane3D, Plane3DError> =
            createInternal(
                view = view,
                point = point1,
                direction1Source =
                    Plane3DDirectionSource.Points(point1, point2),
                direction2Source =
                    Plane3DDirectionSource.Points(point1, point3),
                rangeUSource = rangeUSource,
                rangeVSource = rangeVSource,
                definingPoints = listOf(point1, point2, point3),
                ownedPoints = buildList {
                    if (ownsPoint1) add(point1)
                    if (ownsPoint2 && point2 !== point1) add(point2)
                    if (
                        ownsPoint3 &&
                        point3 !== point1 &&
                        point3 !== point2
                    ) {
                        add(point3)
                    }
                },
                dependencies = dependencies,
                planeType = planeType,
                meshStepWidthU = meshStepWidthU,
                meshStepWidthV = meshStepWidthV,
                surfaceAttributes = surfaceAttributes,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )

        // JSXGraph: src/3d/linspace3d.js -> createPlane3D,
        // transformed Plane3D form.
        internal fun create(
            view: View3D,
            basePlane: Plane3D,
            transformations: List<Transformation>,
            rangeUSource: List<Line3DCoordinateValue> = defaultRange(),
            rangeVSource: List<Line3DCoordinateValue> = defaultRange(),
            dependencies: Iterable<GeometryElement> = emptyList(),
            planeType: String = DEFAULT_PLANE_TYPE,
            meshStepWidthU: Double = 1.0,
            meshStepWidthV: Double = 1.0,
            surfaceAttributes: Plane3DSurfaceAttributes =
                Plane3DSurfaceAttributes(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = true,
        ): GMResult<Plane3D, Plane3DError> {
            if (basePlane.view !== view) {
                return GMResult.Err(
                    Plane3DError.ParentViewMismatch(parentIndex = 0),
                )
            }
            if (transformations.isEmpty()) {
                return GMResult.Err(
                    Plane3DError.InvalidTransformationCount(0),
                )
            }
            val point = when (
                val result = Point3D.create(
                    view = view,
                    coordinates = doubleArrayOf(0.0, 0.0, 0.0, 0.0),
                    name = "",
                    fixed = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    Plane3DError.PointFactory(result.error),
                )
            }
            val plane = when (
                val result = create(
                    view = view,
                    point = point,
                    direction1Source = Plane3DDirectionSource.Values(
                        listOf(
                            Line3DCoordinateValue.Numeric(0.0),
                            Line3DCoordinateValue.Numeric(0.0001),
                            Line3DCoordinateValue.Numeric(0.0),
                            Line3DCoordinateValue.Numeric(0.0),
                        ),
                    ),
                    direction2Source = Plane3DDirectionSource.Values(
                        listOf(
                            Line3DCoordinateValue.Numeric(0.0),
                            Line3DCoordinateValue.Numeric(0.0),
                            Line3DCoordinateValue.Numeric(0.0001),
                            Line3DCoordinateValue.Numeric(0.0),
                        ),
                    ),
                    rangeUSource = rangeUSource,
                    rangeVSource = rangeVSource,
                    ownsPoint = true,
                    dependencies = dependencies,
                    planeType = planeType,
                    meshStepWidthU = meshStepWidthU,
                    meshStepWidthV = meshStepWidthV,
                    surfaceAttributes = surfaceAttributes,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                    fixed = fixed,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    view.board.removeObject(point)
                    return result
                }
            }
            plane.addTransform(basePlane, transformations)
            plane.addParents(listOf(basePlane))
            plane.prepareUpdate().update()
            plane.transformationEvaluationError?.let { error ->
                view.board.removeObject(plane)
                return GMResult.Err(error)
            }
            return GMResult.Ok(plane)
        }

        private fun createInternal(
            view: View3D,
            point: Point3D,
            direction1Source: Plane3DDirectionSource,
            direction2Source: Plane3DDirectionSource,
            rangeUSource: List<Line3DCoordinateValue>,
            rangeVSource: List<Line3DCoordinateValue>,
            definingPoints: List<Point3D>,
            ownedPoints: List<Point3D>,
            dependencies: Iterable<GeometryElement>,
            planeType: String,
            meshStepWidthU: Double,
            meshStepWidthV: Double,
            surfaceAttributes: Plane3DSurfaceAttributes,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
            fixed: Boolean,
        ): GMResult<Plane3D, Plane3DError> {
            for ((index, definingPoint) in definingPoints.withIndex()) {
                validatePoint(view, definingPoint, index)?.let { error ->
                    cleanupOwnedPoints(view, ownedPoints)
                    return GMResult.Err(error)
                }
            }
            validateDirection(view, direction1Source, 0)?.let { error ->
                cleanupOwnedPoints(view, ownedPoints)
                return GMResult.Err(error)
            }
            validateDirection(view, direction2Source, 1)?.let { error ->
                cleanupOwnedPoints(view, ownedPoints)
                return GMResult.Err(error)
            }
            if (rangeUSource.size != 2) {
                cleanupOwnedPoints(view, ownedPoints)
                return GMResult.Err(
                    Plane3DError.InvalidRangeCount(0, rangeUSource.size),
                )
            }
            if (rangeVSource.size != 2) {
                cleanupOwnedPoints(view, ownedPoints)
                return GMResult.Err(
                    Plane3DError.InvalidRangeCount(1, rangeVSource.size),
                )
            }
            val plane = Plane3D(
                view = view,
                point = point,
                direction1Source = direction1Source,
                direction2Source = direction2Source,
                rangeUSource = rangeUSource.toMutableList(),
                rangeVSource = rangeVSource.toMutableList(),
                planeType = planeType,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                isFixed = fixed,
            )
            plane.updateCoords()
            plane.directionEvaluationError?.let { error ->
                cleanupOwnedPoints(view, ownedPoints)
                return GMResult.Err(error)
            }
            plane.rangeEvaluationError?.let { error ->
                cleanupOwnedPoints(view, ownedPoints)
                return GMResult.Err(error)
            }
            plane.usesSurfaceMapping =
                planeType != WIREFRAME_PLANE_TYPE &&
                    plane.evaluatedRangeU.all(Double::isFinite) &&
                    plane.evaluatedRangeV.all(Double::isFinite)
            plane.updateDataArray()
            when (
                val registration =
                    view.board.setId(plane, PLANE_3D_ID_PREFIX)
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> {
                    cleanupOwnedPoints(view, ownedPoints)
                    return GMResult.Err(
                        Plane3DError.Registration(registration.error),
                    )
                }
            }
            plane.registerInView()
            // createPlane3D only links the defining point. Point2/point3 and
            // Line3D direction parents are read dynamically but are not added
            // to the upstream parent list.
            point.addChild(plane)
            for (dependency in dependencies) {
                dependency.addChild(plane)
            }
            val outline = when (
                val result = Curve.createData(
                    board = view.board,
                    dataX =
                        if (plane.usesSurfaceMapping) {
                            DoubleArray(0)
                        } else {
                            plane.dataX
                        },
                    dataY =
                        if (plane.usesSurfaceMapping) {
                            DoubleArray(0)
                        } else {
                            plane.dataY
                        },
                    name = "",
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    view.board.removeObject(plane)
                    return GMResult.Err(
                        Plane3DError.ProxyCurveFactory(result.error),
                    )
                }
            }
            outline.dump = false
            outline.isDraggable = false
            outline.setParents(listOf(plane))
            plane.outline2D = outline
            plane.element2D = outline
            plane.addChild(outline)
            if (
                planeType == WIREFRAME_PLANE_TYPE &&
                plane.evaluatedRangeU.all(Double::isFinite) &&
                plane.evaluatedRangeV.all(Double::isFinite)
            ) {
                val mesh = when (
                    val result = Mesh3D.create(
                        view = view,
                        pointSource = Mesh3DPointSource.Function(
                            Line3DArrayEvaluator {
                                GMResult.Ok(plane.point.coords.copyOf())
                            },
                        ),
                        direction1Source = Mesh3DVectorSource.Function(
                            Line3DArrayEvaluator {
                                GMResult.Ok(plane.vec1.copyOf())
                            },
                        ),
                        direction2Source = Mesh3DVectorSource.Function(
                            Line3DArrayEvaluator {
                                GMResult.Ok(plane.vec2.copyOf())
                            },
                        ),
                        rangeUSource = planeRangeSource(
                            plane = plane,
                            rangeIndex = 0,
                        ),
                        rangeVSource = planeRangeSource(
                            plane = plane,
                            rangeIndex = 1,
                        ),
                        stepWidthU = meshStepWidthU,
                        stepWidthV = meshStepWidthV,
                        name = "",
                        needsRegularUpdate = needsRegularUpdate,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        view.board.removeObject(plane)
                        cleanupOwnedPoints(view, ownedPoints)
                        return GMResult.Err(
                            Plane3DError.Mesh3DFactory(result.error),
                        )
                    }
                }
                mesh.dump = false
                mesh.setParents(listOf(plane))
                plane.mesh3D = mesh
                plane.addChild(mesh)
                outline.inherits += mesh
            } else if (plane.usesSurfaceMapping) {
                val definition = when (
                    val result = planeSurfaceDefinition(
                        plane = plane,
                        attributes = surfaceAttributes,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        view.board.removeObject(plane)
                        cleanupOwnedPoints(view, ownedPoints)
                        return result
                    }
                }
                val faceInputs = definition.second.mapIndexed {
                        faceNumber,
                        vertexKeys,
                    ->
                    Polyhedron3DFaceInput(
                        vertexKeys = vertexKeys,
                        attributes = planeSurfaceFaceAttributes(
                            planeType = planeType,
                            surfaceAttributes = surfaceAttributes,
                            faceNumber = faceNumber,
                        ),
                    )
                }
                val surface = when (
                    val result = Polyhedron3D.create(
                        view = view,
                        vertices = definition.first,
                        faceInputs = faceInputs,
                        dependencies = listOf(plane),
                        name = "",
                        needsRegularUpdate = needsRegularUpdate,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        view.board.removeObject(plane)
                        cleanupOwnedPoints(view, ownedPoints)
                        return GMResult.Err(
                            Plane3DError.Polyhedron3DFactory(result.error),
                        )
                    }
                }
                surface.setParents(listOf(plane))
                plane.surface3D = surface
            }
            plane.isDraggable = !fixed
            plane.prepareUpdate().update()
            outline.prepareUpdate().update()
            return GMResult.Ok(plane)
        }

        // JSXGraph: src/math/tiling.js -> triangulation /
        // rectangulation, as used by src/3d/linspace3d.js ->
        // createPlane3D.
        private fun planeSurfaceDefinition(
            plane: Plane3D,
            attributes: Plane3DSurfaceAttributes,
        ): GMResult<
            Pair<
                LinkedHashMap<String, Polyhedron3DVertexSource>,
                List<List<String>>,
                >,
            Plane3DError,
            > {
            if (attributes.stepsU <= 0) {
                return GMResult.Err(
                    Plane3DError.InvalidSurfaceSteps(
                        axis = "u",
                        count = attributes.stepsU,
                    ),
                )
            }
            if (attributes.stepsV <= 0) {
                return GMResult.Err(
                    Plane3DError.InvalidSurfaceSteps(
                        axis = "v",
                        count = attributes.stepsV,
                    ),
                )
            }
            val triangular =
                attributes.tiling.lowercase() == TRIANGLE_TILING
            val oddRows = (attributes.stepsV.toLong() + 1L) / 2L
            val rowCount = attributes.stepsV.toLong() + 1L
            val vertexCount =
                rowCount * (attributes.stepsU.toLong() + 1L) +
                    if (triangular) oddRows else 0L
            val faceCount =
                if (triangular) {
                    val oddFaceRows =
                        (attributes.stepsV.toLong() + 1L) / 2L
                    val evenFaceRows =
                        attributes.stepsV.toLong() / 2L
                    oddFaceRows *
                        (2L * (attributes.stepsU.toLong() + 1L)) +
                        evenFaceRows *
                        (2L * attributes.stepsU.toLong() + 1L)
                } else {
                    attributes.stepsU.toLong() *
                        attributes.stepsV.toLong()
                }
            if (vertexCount > Polyhedron3D.MAX_VERTEX_COUNT) {
                return GMResult.Err(
                    Plane3DError.SurfaceVertexLimitExceeded(
                        count = vertexCount,
                        maximum = Polyhedron3D.MAX_VERTEX_COUNT,
                    ),
                )
            }
            if (faceCount > Polyhedron3D.MAX_FACE_COUNT) {
                return GMResult.Err(
                    Plane3DError.SurfaceFaceLimitExceeded(
                        count = faceCount,
                        maximum = Polyhedron3D.MAX_FACE_COUNT,
                    ),
                )
            }
            return if (triangular) {
                triangularSurfaceDefinition(plane, attributes)
            } else {
                rectangularSurfaceDefinition(plane, attributes)
            }
        }

        private fun rectangularSurfaceDefinition(
            plane: Plane3D,
            attributes: Plane3DSurfaceAttributes,
        ): GMResult<
            Pair<
                LinkedHashMap<String, Polyhedron3DVertexSource>,
                List<List<String>>,
                >,
            Plane3DError,
            > {
            val vertices =
                linkedMapOf<String, Polyhedron3DVertexSource>()
            val faces = mutableListOf<List<String>>()
            for (row in 0..attributes.stepsV) {
                for (column in 0..attributes.stepsU) {
                    val index = vertices.size
                    vertices[index.toString()] = planeSurfaceVertex(
                        plane = plane,
                        column = column.toDouble(),
                        row = row,
                        stepsU = attributes.stepsU,
                        stepsV = attributes.stepsV,
                    )
                    if (column > 0 && row > 0) {
                        val last = vertices.size - 1
                        faces += listOf(
                            (last - 1).toString(),
                            last.toString(),
                            (last - 1 - attributes.stepsU).toString(),
                            (last - 2 - attributes.stepsU).toString(),
                        )
                    }
                }
            }
            return GMResult.Ok(vertices to faces)
        }

        private fun triangularSurfaceDefinition(
            plane: Plane3D,
            attributes: Plane3DSurfaceAttributes,
        ): GMResult<
            Pair<
                LinkedHashMap<String, Polyhedron3DVertexSource>,
                List<List<String>>,
                >,
            Plane3DError,
            > {
            val vertices =
                linkedMapOf<String, Polyhedron3DVertexSource>()
            val faces = mutableListOf<List<String>>()
            for (row in 0..attributes.stepsV) {
                val lastColumn =
                    if (row % 2 == 0) {
                        attributes.stepsU
                    } else {
                        attributes.stepsU + 1
                    }
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
                    vertices[vertices.size.toString()] =
                        planeSurfaceVertex(
                            plane = plane,
                            column = shiftedColumn,
                            row = row,
                            stepsU = attributes.stepsU,
                            stepsV = attributes.stepsV,
                        )
                    if (row > 0) {
                        val last = vertices.size - 1
                        if (row % 2 == 1) {
                            if (column > 0) {
                                val first = listOf(
                                    (last - 1).toString(),
                                    last.toString(),
                                    (
                                        last -
                                            2 -
                                            attributes.stepsU
                                        ).toString(),
                                )
                                faces += first
                                faces +=
                                    if (column < lastColumn) {
                                        listOf(
                                            last.toString(),
                                            (
                                                last -
                                                    1 -
                                                    attributes.stepsU
                                                ).toString(),
                                            (
                                                last -
                                                    2 -
                                                    attributes.stepsU
                                                ).toString(),
                                        )
                                    } else {
                                        first
                                    }
                            }
                        } else {
                            if (column > 0) {
                                faces += listOf(
                                    last.toString(),
                                    (
                                        last -
                                            2 -
                                            attributes.stepsU
                                        ).toString(),
                                    (last - 1).toString(),
                                )
                            }
                            faces += listOf(
                                last.toString(),
                                (
                                    last -
                                        1 -
                                        attributes.stepsU
                                    ).toString(),
                                (
                                    last -
                                        2 -
                                        attributes.stepsU
                                    ).toString(),
                            )
                        }
                    }
                }
            }
            return GMResult.Ok(vertices to faces)
        }

        private fun planeSurfaceVertex(
            plane: Plane3D,
            column: Double,
            row: Int,
            stepsU: Int,
            stepsV: Int,
        ): Polyhedron3DVertexSource =
            Polyhedron3DVertexSource.Function(
                Line3DArrayEvaluator {
                    val u =
                        plane.evaluatedRangeU[0] +
                            column *
                            (
                                plane.evaluatedRangeU[1] -
                                    plane.evaluatedRangeU[0]
                                ) / stepsU
                    val v =
                        plane.evaluatedRangeV[0] +
                            row *
                            (
                                plane.evaluatedRangeV[1] -
                                    plane.evaluatedRangeV[0]
                                ) / stepsV
                    GMResult.Ok(plane.F(u, v).copyOfRange(1, 4))
                },
            )

        private fun planeSurfaceFaceAttributes(
            planeType: String,
            surfaceAttributes: Plane3DSurfaceAttributes,
            faceNumber: Int,
        ): Face3DAttributes {
            val base = surfaceAttributes.faceAttributes
            if (planeType.lowercase() == COLORMAP_PLANE_TYPE) {
                val colormap = surfaceAttributes.colormap
                return base.copy(
                    shader = base.shader.copy(enabled = false),
                    fillColorEvaluator = Face3DFillColorEvaluator { face ->
                        var height = 0.0
                        val keys =
                            face.polyhedron.faceKeys[face.faceNumber]
                        for (key in keys) {
                            height += face.polyhedron.coords[key]?.get(3)
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
            val fillColors = surfaceAttributes.fillColorArray
            return base.copy(
                fillColor =
                    if (fillColors.isEmpty()) {
                        base.fillColor
                    } else {
                        fillColors[faceNumber % fillColors.size]
                    },
                shader = base.shader.copy(
                    enabled =
                        planeType.lowercase() == SHADER_PLANE_TYPE,
                ),
                fillColorEvaluator = null,
            )
        }

        private fun validatePoint(
            view: View3D,
            point: Point3D,
            parentIndex: Int,
        ): Plane3DError? =
            when {
                point.view !== view ->
                    Plane3DError.ParentViewMismatch(parentIndex)
                view.board.elementById(point.id) !== point ->
                    Plane3DError.ParentNotRegistered(
                        parentIndex = parentIndex,
                        id = point.id,
                    )
                else -> null
            }

        private fun validateDirection(
            view: View3D,
            source: Plane3DDirectionSource,
            directionIndex: Int,
        ): Plane3DError? =
            when (source) {
                is Plane3DDirectionSource.Points -> null
                is Plane3DDirectionSource.Line ->
                    if (source.line.view !== view) {
                        Plane3DError.ParentViewMismatch(directionIndex + 1)
                    } else if (
                        view.board.elementById(source.line.id) !== source.line
                    ) {
                        Plane3DError.ParentNotRegistered(
                            parentIndex = directionIndex + 1,
                            id = source.line.id,
                        )
                    } else {
                        null
                    }
                is Plane3DDirectionSource.Values ->
                    if (source.values.size !in setOf(3, 4)) {
                        Plane3DError.InvalidDirectionCount(
                            directionIndex = directionIndex,
                            count = source.values.size,
                        )
                    } else {
                        null
                    }
                is Plane3DDirectionSource.Function -> null
            }

        private fun cleanupOwnedPoints(
            view: View3D,
            ownedPoints: List<Point3D>,
        ) {
            for (ownedPoint in ownedPoints.distinct()) {
                view.board.removeObject(ownedPoint)
            }
        }

        private fun planeRangeSource(
            plane: Plane3D,
            rangeIndex: Int,
        ): List<Line3DCoordinateValue> =
            List(2) { coordinateIndex ->
                Line3DCoordinateValue.Dynamic(
                    Line3DScalarEvaluator {
                        val range =
                            if (rangeIndex == 0) {
                                plane.evaluatedRangeU
                            } else {
                                plane.evaluatedRangeV
                            }
                        GMResult.Ok(range[coordinateIndex])
                    },
                )
            }

        private fun defaultRange(): List<Line3DCoordinateValue> =
            listOf(
                Line3DCoordinateValue.Numeric(Double.NEGATIVE_INFINITY),
                Line3DCoordinateValue.Numeric(Double.POSITIVE_INFINITY),
            )

        private const val DEFAULT_PLANE_TYPE = "shader"
        private const val WIREFRAME_PLANE_TYPE = "wireframe"
        private const val SHADER_PLANE_TYPE = "shader"
        private const val COLORMAP_PLANE_TYPE = "colormap"
        private const val TRIANGLE_TILING = "triangle"
    }
}
