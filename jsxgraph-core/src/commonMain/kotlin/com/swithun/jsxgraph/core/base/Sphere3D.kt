/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/sphere3d.js -> Sphere3D / createSphere3D
 * Copyright 2008-2026 Matthias Ehmann, Aaron Fenyes, Carsten Miller,
 * Andreas Walter, and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal enum class Sphere3DMethod {
    TwoPoints,
    PointRadius,
}

internal sealed interface Sphere3DError {
    data class ParentViewMismatch(
        val parentIndex: Int,
    ) : Sphere3DError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : Sphere3DError

    data class MissingPoint2(
        val method: Sphere3DMethod,
    ) : Sphere3DError

    data class MissingRadius(
        val method: Sphere3DMethod,
    ) : Sphere3DError

    data class RadiusEvaluation(
        val error: Line3DDynamicError,
    ) : Sphere3DError

    data class InvalidSampleCount(
        val count: Int,
        val maximum: Int,
    ) : Sphere3DError

    data class InvalidCoordinateCount(
        val count: Int,
    ) : Sphere3DError

    data class InvalidParameterCount(
        val count: Int,
    ) : Sphere3DError

    data class Registration(
        val error: BoardError,
    ) : Sphere3DError

    data class ProjectionPointFactory(
        val error: PointError,
    ) : Sphere3DError

    data class ParallelProjectionFactory(
        val error: CircleError,
    ) : Sphere3DError

    data class CentralProjectionFactory(
        val error: EllipseError,
    ) : Sphere3DError

    data object MissingCentralProjectionCenter : Sphere3DError
}

/**
 * Source-mapped Sphere3D whose ordinary 2D proxy follows the active View3D
 * projection: Circle for parallel projection and Ellipse for central
 * projection.
 */
internal class Sphere3D private constructor(
    view: View3D,
    internal val method: Sphere3DMethod,
    internal val center: Point3D,
    internal val point2: Point3D?,
    private var radiusSource: Line3DCoordinateValue?,
    private val sampleCount: Int,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : GeometryElement3D(
    view = view,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_SPHERE3D,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal val points = mutableListOf<Point>()
    internal val aux2D = mutableListOf<Point>()
    internal val inherits = mutableListOf<GeometryElement>()
    internal var projectionType: String = view.projectionType
        private set
    internal var evaluationError: Sphere3DError? = null
        private set

    private val projectionElements = mutableListOf<GeometryElement>()

    init {
        elType = SPHERE_3D_ELEMENT_TYPE
    }

    // JSXGraph: src/3d/sphere3d.js -> X / Y / Z.
    @Suppress("UNUSED_PARAMETER")
    internal fun X(u: Double, v: Double): Double =
        Radius() * sin(u) * cos(v)

    internal fun Y(u: Double, v: Double): Double =
        Radius() * sin(u) * sin(v)

    @Suppress("UNUSED_PARAMETER")
    internal fun Z(u: Double, v: Double): Double =
        Radius() * cos(u)

    internal val rangeU: DoubleArray =
        doubleArrayOf(0.0, 2.0 * PI)
    internal val rangeV: DoubleArray =
        doubleArrayOf(0.0, PI)

    // JSXGraph: src/3d/sphere3d.js -> setRadius.
    internal fun setRadius(
        source: Line3DCoordinateValue,
        dependencies: Iterable<GeometryElement> = emptyList(),
    ): GMResult<Sphere3D, Sphere3DError> {
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

    // JSXGraph: src/3d/sphere3d.js -> Radius.
    @Suppress("FunctionName")
    internal fun Radius(): Double =
        when (val result = evaluateRadius()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> Double.NaN
        }

    // JSXGraph: src/3d/sphere3d.js -> focusFn.
    internal fun focusCoordinates(sign: Double): DoubleArray {
        val cameraDirection = view.boxToCam[3]
        val radius = Radius()
        return view.project3DTo2D(
            doubleArrayOf(
                center.X() + sign * radius * cameraDirection[1],
                center.Y() + sign * radius * cameraDirection[2],
                center.Z() + sign * radius * cameraDirection[3],
            ),
        )
    }

    // JSXGraph: src/3d/sphere3d.js -> innerVertexFn.
    internal fun innerVertexCoordinates(): DoubleArray {
        val focalCenter = view.worldToFocal(center.coords, homogeneous = false)
        val distanceOffAxis = Mat.hypot(
            focalCenter[0],
            focalCenter[1],
        )
        val camera = view.boxToCam
        val radius = Radius()
        val angleOffAxis = atan(-distanceOffAxis / focalCenter[2])
        val steepness = acos(radius / Mat.norm(focalCenter))
        val lean = angleOffAxis + steepness
        val cosineLean = cos(lean)
        val sineLean = sin(lean)
        val inward =
            if (distanceOffAxis > OFF_AXIS_EPSILON) {
                doubleArrayOf(
                    -(
                        focalCenter[0] * camera[1][1] +
                            focalCenter[1] * camera[2][1]
                        ) / distanceOffAxis,
                    -(
                        focalCenter[0] * camera[1][2] +
                            focalCenter[1] * camera[2][2]
                        ) / distanceOffAxis,
                    -(
                        focalCenter[0] * camera[1][3] +
                            focalCenter[1] * camera[2][3]
                        ) / distanceOffAxis,
                )
            } else {
                doubleArrayOf(
                    camera[1][1],
                    camera[1][2],
                    camera[1][3],
                )
            }
        return view.project3DTo2D(
            doubleArrayOf(
                center.X() +
                    radius * (
                        sineLean * inward[0] +
                            cosineLean * camera[3][1]
                        ),
                center.Y() +
                    radius * (
                        sineLean * inward[1] +
                            cosineLean * camera[3][2]
                        ),
                center.Z() +
                    radius * (
                        sineLean * inward[2] +
                            cosineLean * camera[3][3]
                        ),
            ),
        )
    }

    // JSXGraph: src/3d/sphere3d.js -> rebuildProjection.
    internal fun rebuildProjection():
        GMResult<Sphere3D, Sphere3DError> {
        clearProjection()
        projectionType = view.projectionType
        val result =
            if (projectionType == CENTRAL_PROJECTION) {
                buildCentralProjection()
            } else {
                buildParallelProjection()
            }
        return when (result) {
            is GMResult.Ok -> {
                evaluationError = null
                GMResult.Ok(this)
            }
            is GMResult.Err -> {
                clearProjection()
                evaluationError = result.error
                result
            }
        }
    }

    // JSXGraph: src/3d/sphere3d.js -> projectCoords.
    internal fun projectCoords(
        coordinates: DoubleArray,
        parameters: DoubleArray,
    ): GMResult<DoubleArray, Sphere3DError> {
        if (coordinates.size != 3) {
            return GMResult.Err(
                Sphere3DError.InvalidCoordinateCount(coordinates.size),
            )
        }
        if (parameters.size < 2) {
            return GMResult.Err(
                Sphere3DError.InvalidParameterCount(parameters.size),
            )
        }
        val radius = Radius()
        val point = doubleArrayOf(
            1.0,
            coordinates[0],
            coordinates[1],
            coordinates[2],
        )
        val distance = Geometry.distance(center.coords, point, 4)
        val vector = DoubleArray(4) { point[it] - center.coords[it] }
        if (distance == 0.0) {
            parameters[0] = 0.0
            parameters[1] = 0.0
            return GMResult.Ok(doubleArrayOf(1.0, radius, 0.0, 0.0))
        }
        if (radius == 0.0) {
            parameters[0] = 0.0
            parameters[1] = 0.0
            return GMResult.Ok(center.coords.copyOf())
        }

        val scale = radius / distance
        vector[0] = 1.0
        vector[1] *= scale
        vector[2] *= scale
        vector[3] *= scale
        parameters[1] = atan2(vector[2], vector[1])
        if (parameters[1] < 0.0) {
            parameters[1] += PI
        }
        parameters[0] =
            if (parameters[1] != 0.0) {
                atan2(
                    vector[2],
                    vector[3] * sin(parameters[1]),
                )
            } else {
                atan2(
                    vector[1],
                    vector[3] * cos(parameters[1]),
                )
            }
        if (parameters[0] < 0.0) {
            parameters[0] += 2.0 * PI
        }
        return GMResult.Ok(vector)
    }

    // JSXGraph: src/3d/sphere3d.js -> update.
    override fun update(fromParent: Boolean): Sphere3D {
        if (!needsUpdate) {
            return this
        }
        if (projectionType != view.projectionType) {
            rebuildProjection()
            return this
        }
        evaluationError = when (val result = evaluateRadius()) {
            is GMResult.Ok -> null
            is GMResult.Err -> result.error
        }
        return this
    }

    override fun updateRenderer(): Sphere3D {
        needsUpdate = false
        return this
    }

    private fun evaluateRadius(): GMResult<Double, Sphere3DError> =
        when (method) {
            Sphere3DMethod.TwoPoints -> {
                val circumferencePoint = point2
                    ?: return GMResult.Err(
                        Sphere3DError.MissingPoint2(method),
                    )
                GMResult.Ok(
                    if (
                        center.testIfFinite() &&
                        circumferencePoint.testIfFinite()
                    ) {
                        center.distance(circumferencePoint)
                    } else {
                        Double.NaN
                    },
                )
            }
            Sphere3DMethod.PointRadius -> {
                when (val source = radiusSource) {
                    null -> GMResult.Err(
                        Sphere3DError.MissingRadius(method),
                    )
                    is Line3DCoordinateValue.Numeric ->
                        GMResult.Ok(abs(source.value))
                    is Line3DCoordinateValue.Dynamic ->
                        when (val result = source.evaluator.evaluate()) {
                            is GMResult.Ok ->
                                GMResult.Ok(abs(result.value))
                            is GMResult.Err -> GMResult.Err(
                                Sphere3DError.RadiusEvaluation(
                                    result.error,
                                ),
                            )
                        }
                }
            }
        }

    private fun buildParallelProjection():
        GMResult<Sphere3D, Sphere3DError> {
        val boxSize = view.bbox3D[0][1] - view.bbox3D[0][0]
        val proxy = when (
            val result = Circle.create(
                board = board,
                center = center.point2D,
                radiusFunction = {
                    Radius() * view.size[0] / boxSize
                },
                name = "",
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                Sphere3DError.ParallelProjectionFactory(result.error),
            )
        }
        proxy.dump = false
        element2D = proxy
        projectionElements += proxy
        inherits += proxy
        addChild(proxy)
        return GMResult.Ok(this)
    }

    private fun buildCentralProjection():
        GMResult<Sphere3D, Sphere3DError> {
        val createdPoints = mutableListOf<Point>()
        val suppliers = listOf<() -> DoubleArray>(
            { focusCoordinates(-1.0) },
            { focusCoordinates(1.0) },
            ::innerVertexCoordinates,
        )
        for (supplier in suppliers) {
            val point = when (
                val result = Point.createConstrained(
                    board = board,
                    coordinateFunctions = listOf(
                        SphereProjectionCoordinateFunction(
                            supplier = supplier,
                            coordinateIndex = 1,
                        ),
                        SphereProjectionCoordinateFunction(
                            supplier = supplier,
                            coordinateIndex = 2,
                        ),
                    ),
                    name = "",
                    needsRegularUpdate = needsRegularUpdate,
                    fixed = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    board.removeObjects(createdPoints)
                    return GMResult.Err(
                        Sphere3DError.ProjectionPointFactory(result.error),
                    )
                }
            }
            point.dump = false
            createdPoints += point
        }
        val ellipse = when (
            val result = Ellipse.create(
                board = board,
                focus1 = createdPoints[0],
                focus2 = createdPoints[1],
                pointOnEllipse = createdPoints[2],
                parentlessPoints = createdPoints.toSet(),
                sampleCount = sampleCount,
                name = "",
                needsRegularUpdate = needsRegularUpdate,
                centerName = "",
                centerNeedsRegularUpdate = needsRegularUpdate,
                centerFixed = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObjects(createdPoints)
                return GMResult.Err(
                    Sphere3DError.CentralProjectionFactory(result.error),
                )
            }
        }
        val ellipseCenter = ellipse.subs["center"] as? Point
        if (ellipseCenter == null) {
            board.removeObject(ellipse)
            board.removeObjects(createdPoints)
            return GMResult.Err(
                Sphere3DError.MissingCentralProjectionCenter,
            )
        }
        ellipse.dump = false
        ellipseCenter.dump = false
        points += createdPoints
        aux2D += createdPoints
        element2D = ellipse
        projectionElements +=
            listOf(ellipse, ellipseCenter) + createdPoints
        inherits += ellipse
        for (projectionElement in projectionElements) {
            addChild(projectionElement)
        }
        return GMResult.Ok(this)
    }

    private fun clearProjection() {
        board.removeObjects(projectionElements)
        projectionElements.clear()
        points.clear()
        aux2D.clear()
        inherits.clear()
        element2D = null
    }

    internal companion object {
        internal const val DEFAULT_SAMPLE_COUNT: Int =
            Curve.DEFAULT_SAMPLE_COUNT
        internal const val MAX_SAMPLE_COUNT: Int =
            Curve.MAX_SAMPLE_COUNT
        private const val SPHERE_3D_ID_PREFIX = "sphere3d"
        private const val SPHERE_3D_ELEMENT_TYPE = "sphere3d"
        private const val CENTRAL_PROJECTION = "central"
        private const val OFF_AXIS_EPSILON = 1.0e-8

        // JSXGraph: src/3d/sphere3d.js -> createSphere3D.
        internal fun create(
            view: View3D,
            center: Point3D,
            point2: Point3D? = null,
            radiusSource: Line3DCoordinateValue? = null,
            ownsCenter: Boolean = false,
            ownsPoint2: Boolean = false,
            radiusDependencies: Iterable<GeometryElement> = emptyList(),
            sampleCount: Int = DEFAULT_SAMPLE_COUNT,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Sphere3D, Sphere3DError> {
            validateParent(view, center, 0)?.let {
                return GMResult.Err(it)
            }
            point2?.let { point ->
                validateParent(view, point, 1)?.let {
                    return GMResult.Err(it)
                }
            }
            val method =
                if (point2 != null) {
                    Sphere3DMethod.TwoPoints
                } else {
                    Sphere3DMethod.PointRadius
                }
            if (
                method == Sphere3DMethod.PointRadius &&
                radiusSource == null
            ) {
                return GMResult.Err(Sphere3DError.MissingRadius(method))
            }
            if (sampleCount !in 1..MAX_SAMPLE_COUNT) {
                return GMResult.Err(
                    Sphere3DError.InvalidSampleCount(
                        count = sampleCount,
                        maximum = MAX_SAMPLE_COUNT,
                    ),
                )
            }
            if (id.isNotEmpty() && view.board.elementById(id) != null) {
                return GMResult.Err(
                    Sphere3DError.Registration(
                        BoardError.DuplicateElementId(id),
                    ),
                )
            }
            val sphere = Sphere3D(
                view = view,
                method = method,
                center = center,
                point2 = point2,
                radiusSource = radiusSource,
                sampleCount = sampleCount,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            when (val result = sphere.evaluateRadius()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (
                val registration =
                    view.board.setId(sphere, SPHERE_3D_ID_PREFIX)
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Sphere3DError.Registration(registration.error),
                )
            }
            sphere.registerInView()
            if (ownsCenter) {
                sphere.addChild(center)
            } else {
                center.addChild(sphere)
            }
            point2?.let { point ->
                if (ownsPoint2) {
                    sphere.addChild(point)
                } else {
                    point.addChild(sphere)
                }
            }
            for (
                dependency in
                radiusDependencies.distinctBy(GeometryElement::id)
            ) {
                dependency.addChild(sphere)
            }
            return when (val result = sphere.rebuildProjection()) {
                is GMResult.Ok -> {
                    sphere.update()
                    result
                }
                is GMResult.Err -> {
                    view.board.removeObject(sphere)
                    result
                }
            }
        }

        private fun validateParent(
            view: View3D,
            point: Point3D,
            parentIndex: Int,
        ): Sphere3DError? {
            if (point.view !== view) {
                return Sphere3DError.ParentViewMismatch(parentIndex)
            }
            if (view.board.elementById(point.id) !== point) {
                return Sphere3DError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = point.id,
                )
            }
            return null
        }
    }
}

private class SphereProjectionCoordinateFunction(
    private val supplier: () -> DoubleArray,
    private val coordinateIndex: Int,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Ok(
            JessieCodeRuntimeValue.NumberValue(
                supplier()[coordinateIndex],
            ),
        )
}
