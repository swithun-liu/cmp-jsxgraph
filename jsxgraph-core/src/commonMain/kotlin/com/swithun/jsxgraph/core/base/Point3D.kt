/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/point3d.js
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.abs

internal sealed interface Point3DDynamicError {
    data class Rejected(
        val reason: String,
    ) : Point3DDynamicError
}

internal fun interface Point3DScalarEvaluator {
    fun evaluate(): GMResult<Double, Point3DDynamicError>
}

internal fun interface Point3DArrayEvaluator {
    fun evaluate(): GMResult<DoubleArray, Point3DDynamicError>
}

internal sealed interface Point3DCoordinateValue {
    data class Numeric(
        val value: Double,
    ) : Point3DCoordinateValue

    data class Dynamic(
        val evaluator: Point3DScalarEvaluator,
    ) : Point3DCoordinateValue
}

internal sealed interface Point3DCoordinateSource {
    data class Values(
        val values: List<Point3DCoordinateValue>,
    ) : Point3DCoordinateSource

    data class Function(
        val evaluator: Point3DArrayEvaluator,
    ) : Point3DCoordinateSource
}

internal sealed interface Point3DError {
    data class InvalidCoordinateCount(
        val count: Int,
    ) : Point3DError

    data class CoordinateEvaluation(
        val coordinateIndex: Int?,
        val error: Point3DDynamicError,
    ) : Point3DError

    data class InvalidTransformationCount(
        val count: Int,
    ) : Point3DError

    data class InvalidBaseElement(
        val baseElementId: String?,
    ) : Point3DError

    data class TransformationEvaluation(
        val transformationIndex: Int,
        val error: TransformationError,
    ) : Point3DError

    data class Registration(
        val error: BoardError,
    ) : Point3DError

    data class ProxyPointFactory(
        val error: PointError,
    ) : Point3DError
}

/**
 * Translated Point3D lifecycle with the same ordinary Point proxy used by
 * JSXGraph for rendering and pointer interaction.
 */
internal class Point3D private constructor(
    view: View3D,
    private val coordinateSource: Point3DCoordinateSource,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    internal val isFixed: Boolean,
) : GeometryElement3D(
    view = view,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_POINT3D,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal var coords: DoubleArray = DoubleArray(4)
        private set
    internal var initialCoords: DoubleArray = DoubleArray(4)
        private set
    internal lateinit var point2D: Point
        private set
    internal var coordinateEvaluationError: Point3DError? = null
        private set
    internal var transformationEvaluationError: Point3DError? = null
        private set

    private var projectedCoordinates: DoubleArray? = null

    init {
        elType = POINT_3D_ELEMENT_TYPE
    }

    internal fun X(): Double = coords[1]

    internal fun Y(): Double = coords[2]

    internal fun Z(): Double = coords[3]

    internal fun W(): Double = coords[0]

    // JSXGraph: src/3d/point3d.js -> initCoords.
    private fun initCoords(): GMResult<Point3D, Point3DError> {
        val evaluated = when (
            val result = evaluateCoordinates(
                source = coordinateSource,
                dynamicOnly = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        coords = evaluated
        initialCoords = evaluated.copyOf()
        coordinateEvaluationError = null
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/point3d.js -> updateCoords.
    internal fun updateCoords(): Point3D {
        when (
            val result = evaluateCoordinates(
                source = coordinateSource,
                dynamicOnly = true,
            )
        ) {
            is GMResult.Ok -> {
                coords = result.value
                coordinateEvaluationError = null
            }
            is GMResult.Err -> {
                coords = DoubleArray(4) { Double.NaN }
                coordinateEvaluationError = result.error
            }
        }
        return this
    }

    // JSXGraph: src/3d/point3d.js -> normalizeCoords.
    internal fun normalizeCoords(): Point3D {
        if (abs(coords[0]) > NORMALIZATION_EPSILON) {
            coords[1] /= coords[0]
            coords[2] /= coords[0]
            coords[3] /= coords[0]
            coords[0] = 1.0
        }
        return this
    }

    // JSXGraph: src/3d/point3d.js -> setPosition.
    internal fun setPosition(
        coordinates: DoubleArray,
    ): GMResult<Point3D, Point3DError> {
        if (coordinates.size !in setOf(3, 4)) {
            return GMResult.Err(
                Point3DError.InvalidCoordinateCount(coordinates.size),
            )
        }
        if (coordinates.size == 3) {
            coords[0] = 1.0
            coords[1] = coordinates[0]
            coords[2] = coordinates[1]
            coords[3] = coordinates[2]
        } else {
            coords = coordinates.copyOf()
            normalizeCoords()
        }
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/point3d.js -> addTransform.
    internal fun addTransform(
        element: Point3D,
        transformation: Transformation,
    ): Point3D = addTransform(element, listOf(transformation))

    internal fun addTransform(
        element: Point3D,
        newTransformations: Iterable<Transformation>,
    ): Point3D {
        addTransformGeneric(element, newTransformations)
        return this
    }

    // JSXGraph: src/3d/point3d.js -> removeTransform.
    internal fun removeTransform(
        transformation: Transformation,
    ): Point3D {
        removeTransformGeneric(listOf(transformation))
        return this
    }

    // JSXGraph: src/3d/point3d.js -> clearTransforms.
    internal fun clearTransforms(): Point3D {
        clearTransformsGeneric()
        transformationEvaluationError = null
        return this
    }

    // JSXGraph: src/3d/point3d.js -> updateTransform.
    internal fun updateTransform(): Point3D {
        if (transformations.isEmpty() || baseElement == null) {
            transformationEvaluationError = null
            return this
        }
        val basePoint = baseElement as? Point3D
        if (basePoint == null) {
            transformationEvaluationError =
                Point3DError.InvalidBaseElement(baseElement?.id)
            coords = DoubleArray(4) { Double.NaN }
            return this
        }
        var transformed =
            if (basePoint === this) {
                initialCoords.copyOf()
            } else {
                basePoint.coords.copyOf()
            }
        for ((index, transformation) in transformations.withIndex()) {
            when (val result = transformation.applyResult(transformed)) {
                is GMResult.Ok -> transformed = result.value
                is GMResult.Err -> {
                    transformationEvaluationError =
                        Point3DError.TransformationEvaluation(
                            transformationIndex = index,
                            error = result.error,
                        )
                    coords = DoubleArray(4) { Double.NaN }
                    return this
                }
            }
        }
        coords = transformed
        transformationEvaluationError = null
        return this
    }

    // JSXGraph: src/3d/point3d.js -> update.
    override fun update(fromParent: Boolean): GeometryElement {
        val previousProjection = projectedCoordinates
        val currentProjection = point2D.coords.usrCoords
        val proxyMoved =
            point2D.isDraggable &&
                !isFixed &&
                previousProjection != null &&
                !previousProjection.contentEquals(currentProjection)

        if (proxyMoved) {
            val foot = doubleArrayOf(1.0, 0.0, 0.0, coords[3])
            val coordinates3D = view.project2DTo3DPlane(
                coordinates2D = currentProjection,
                normal = doubleArrayOf(1.0, 0.0, 0.0, 1.0),
                foot = foot,
            )
            if (coordinates3D[0] != 0.0) {
                val (bounded, corrected) =
                    view.project3DToCube(coordinates3D)
                coords = bounded
                if (corrected) {
                    point2D.coords.setCoordinates(
                        coordType = Const.COORDS_BY_USER,
                        coordinates = view.project3DTo2D(coords),
                    )
                }
            }
        } else {
            updateCoords()
            if (coordinateEvaluationError == null) {
                updateTransform()
            }
            point2D.coords.setCoordinates(
                coordType = Const.COORDS_BY_USER,
                coordinates = view.project3DTo2D(coords),
            )
            zIndex = Mat.innerProduct(
                view.matrix3DRotShift[3],
                coords,
            )
            point2D.prepareUpdate().update()
        }
        projectedCoordinates = point2D.coords.usrCoords.copyOf()
        return this
    }

    override fun updateRenderer(): GeometryElement {
        needsUpdate = false
        return this
    }

    // JSXGraph: src/3d/point3d.js -> testIfFinite.
    internal fun testIfFinite(): Boolean =
        abs(coords[0]) > FINITE_EPSILON

    // JSXGraph: src/3d/point3d.js -> distance.
    internal fun distance(other: Point3D): Double =
        if (
            coords[0] * coords[0] > DISTANCE_EPSILON_SQUARED &&
            other.coords[0] * other.coords[0] >
            DISTANCE_EPSILON_SQUARED
        ) {
            Mat.hypot(
                other.coords[1] - coords[1],
                other.coords[2] - coords[2],
                other.coords[3] - coords[3],
            )
        } else {
            Double.POSITIVE_INFINITY
        }

    internal fun setPositionFrom2D(
        coordinates: DoubleArray,
        vertical: Boolean = false,
    ): GMResult<Point3D, Point3DError> {
        point2D.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = coordinates,
        )
        val projected =
            if (vertical) {
                view.project2DTo3DVertical(
                    point2D.coords.usrCoords,
                    coords,
                )
            } else {
                view.project2DTo3DPlane(
                    coordinates2D = point2D.coords.usrCoords,
                    normal = doubleArrayOf(1.0, 0.0, 0.0, 1.0),
                    foot = doubleArrayOf(1.0, 0.0, 0.0, coords[3]),
                )
            }
        if (projected[0] != 0.0) {
            coords = view.project3DToCube(projected).first
        }
        point2D.coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = view.project3DTo2D(coords),
        )
        projectedCoordinates = point2D.coords.usrCoords.copyOf()
        return GMResult.Ok(this)
    }

    private fun evaluateCoordinates(
        source: Point3DCoordinateSource,
        dynamicOnly: Boolean,
    ): GMResult<DoubleArray, Point3DError> =
        when (source) {
            is Point3DCoordinateSource.Function -> {
                val evaluated = when (
                    val result = source.evaluator.evaluate()
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        Point3DError.CoordinateEvaluation(
                            coordinateIndex = null,
                            error = result.error,
                        ),
                    )
                }
                homogeneousCoordinates(evaluated)
            }
            is Point3DCoordinateSource.Values -> {
                if (source.values.size !in setOf(3, 4)) {
                    return GMResult.Err(
                        Point3DError.InvalidCoordinateCount(
                            source.values.size,
                        ),
                    )
                }
                val result =
                    if (dynamicOnly) {
                        coords.copyOf()
                    } else {
                        DoubleArray(4)
                    }
                val offset =
                    if (source.values.size == 3) {
                        result[0] = 1.0
                        1
                    } else {
                        0
                    }
                for ((index, value) in source.values.withIndex()) {
                    if (
                        dynamicOnly &&
                        value is Point3DCoordinateValue.Numeric
                    ) {
                        continue
                    }
                    result[offset + index] = when (value) {
                        is Point3DCoordinateValue.Numeric -> value.value
                        is Point3DCoordinateValue.Dynamic -> when (
                            val evaluated = value.evaluator.evaluate()
                        ) {
                            is GMResult.Ok -> evaluated.value
                            is GMResult.Err -> return GMResult.Err(
                                Point3DError.CoordinateEvaluation(
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

    private fun homogeneousCoordinates(
        coordinates: DoubleArray,
    ): GMResult<DoubleArray, Point3DError> =
        when (coordinates.size) {
            3 -> GMResult.Ok(
                doubleArrayOf(
                    1.0,
                    coordinates[0],
                    coordinates[1],
                    coordinates[2],
                ),
            )
            4 -> GMResult.Ok(coordinates.copyOf())
            else -> GMResult.Err(
                Point3DError.InvalidCoordinateCount(coordinates.size),
            )
        }

    internal companion object {
        private const val POINT_3D_ID_PREFIX = "point3d"
        private const val POINT_3D_ELEMENT_TYPE = "point3d"
        private const val NORMALIZATION_EPSILON = 1.0e-14
        private const val FINITE_EPSILON = 1.0e-12
        private const val DISTANCE_EPSILON_SQUARED = 1.0e-12

        // JSXGraph: src/3d/point3d.js -> createPoint3D / initCoords.
        internal fun create(
            view: View3D,
            coordinates: DoubleArray,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<Point3D, Point3DError> =
            create(
                view = view,
                coordinateSource = Point3DCoordinateSource.Values(
                    coordinates.map(Point3DCoordinateValue::Numeric),
                ),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )

        // JSXGraph: src/3d/point3d.js -> createPoint3D transformation form.
        internal fun create(
            view: View3D,
            basePoint: Point3D,
            transformations: List<Transformation>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<Point3D, Point3DError> {
            if (transformations.isEmpty()) {
                return GMResult.Err(
                    Point3DError.InvalidTransformationCount(0),
                )
            }
            val result = create(
                view = view,
                coordinateSource = Point3DCoordinateSource.Values(
                    listOf(
                        Point3DCoordinateValue.Numeric(0.0),
                        Point3DCoordinateValue.Numeric(0.0),
                        Point3DCoordinateValue.Numeric(0.0),
                    ),
                ),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )
            val point = when (result) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            point.addTransform(basePoint, transformations)
            point.setParents(listOf(basePoint))
            point.point2D.isDraggable = false
            point.isDraggable = false
            point.prepareUpdate().update()
            return GMResult.Ok(point)
        }

        internal fun create(
            view: View3D,
            coordinateSource: Point3DCoordinateSource,
            dependencies: Iterable<GeometryElement> = emptyList(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<Point3D, Point3DError> {
            val point = Point3D(
                view = view,
                coordinateSource = coordinateSource,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                isFixed = fixed,
            )
            when (val initialized = point.initCoords()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return initialized
            }
            when (
                val registration =
                    view.board.setId(point, POINT_3D_ID_PREFIX)
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Point3DError.Registration(registration.error),
                )
            }
            point.registerInView()
            val proxy = when (
                val result = Point.create(
                    board = view.board,
                    coordinates = view.project3DTo2D(point.coords),
                    name = point.name,
                    needsRegularUpdate = needsRegularUpdate,
                    fixed = fixed,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    view.board.removeObject(point)
                    return GMResult.Err(
                        Point3DError.ProxyPointFactory(result.error),
                    )
                }
            }
            proxy.dump = false
            point.point2D = proxy
            point.element2D = proxy
            point.isDraggable = proxy.isDraggable
            point.addChild(proxy)
            proxy.setParents(listOf(point))
            point.projectedCoordinates =
                proxy.coords.usrCoords.copyOf()
            for (dependency in dependencies) {
                dependency.addChild(point)
            }
            return GMResult.Ok(point)
        }
    }
}
