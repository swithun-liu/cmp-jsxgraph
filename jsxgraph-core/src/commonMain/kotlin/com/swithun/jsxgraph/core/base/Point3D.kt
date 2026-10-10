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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sin

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

    data class SlideViewMismatch(
        val slideId: String,
    ) : Point3DError

    data class SlideNotRegistered(
        val slideId: String,
    ) : Point3DError

    data class UnsupportedSlide(
        val elementType: String,
    ) : Point3DError

    data class SlideProjection(
        val reason: String,
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

    data class InvalidAnimationDuration(
        val milliseconds: Double,
    ) : Point3DError

    data class InvalidAnimationEffect(
        val effect: String,
    ) : Point3DError

    data class AnimationPosition(
        val error: Point3DError,
    ) : Point3DError

    data class AnimationCallback(
        val message: String,
    ) : Point3DError
}

internal data class Point3DMoveOptions(
    val callback: (() -> Unit)? = null,
    val effect: String? = null,
)

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
    internal val slide: GeometryElement3D?,
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
    internal var position: DoubleArray = doubleArrayOf()
        private set

    private var projectedCoordinates: DoubleArray? = null
    private val movePath = ArrayDeque<DoubleArray>()
    private var moveCallback: (() -> Unit)? = null
    private var movementScheduled: Boolean = false
    internal var movementError: Point3DError? = null
        private set

    internal fun takeMovementError(): Point3DError? {
        val error = movementError
        movementError = null
        return error
    }

    init {
        elType = POINT_3D_ELEMENT_TYPE
    }

    // JSXGraph 1.13.3: src/3d/point3d.js -> X.
    internal fun X(): Double = coords[1]

    // JSXGraph 1.13.3: src/3d/point3d.js -> Y.
    internal fun Y(): Double = coords[2]

    // JSXGraph 1.13.3: src/3d/point3d.js -> Z.
    internal fun Z(): Double = coords[3]

    // JSXGraph 1.13.3: src/3d/point3d.js -> W.
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
            if (slide != null) {
                when (
                    val result = projectScreenToSlide(
                        doubleArrayOf(point2D.X(), point2D.Y()),
                    )
                ) {
                    is GMResult.Ok -> {
                        coords = result.value
                        coordinateEvaluationError = null
                        point2D.coords.setCoordinates(
                            coordType = Const.COORDS_BY_USER,
                            coordinates = view.project3DTo2D(coords),
                        )
                    }
                    is GMResult.Err -> {
                        coordinateEvaluationError = result.error
                    }
                }
            } else {
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
            }
        } else {
            updateCoords()
            if (coordinateEvaluationError == null) {
                updateTransform()
            }
            if (
                coordinateEvaluationError == null &&
                transformationEvaluationError == null
            ) {
                when (val result = projectToSlide()) {
                    is GMResult.Ok -> {
                        coords = result.value
                        coordinateEvaluationError = null
                    }
                    is GMResult.Err -> {
                        coordinateEvaluationError = result.error
                    }
                }
            }
            if (coordinateEvaluationError == null) {
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
        }
        projectedCoordinates = point2D.coords.usrCoords.copyOf()
        return this
    }

    // JSXGraph 1.13.3: src/3d/point3d.js -> updateRenderer.
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

    // JSXGraph 1.13.3: src/3d/point3d.js -> moveTo.
    internal fun moveTo(
        where: DoubleArray,
        timeMilliseconds: Double? = null,
        options: Point3DMoveOptions = Point3DMoveOptions(),
    ): GMResult<Point3D, Point3DError> {
        if (where.size !in setOf(3, 4)) {
            return GMResult.Err(
                Point3DError.InvalidCoordinateCount(where.size),
            )
        }
        val duration = timeMilliseconds ?: 0.0
        if (!duration.isFinite() || duration < 0.0) {
            return GMResult.Err(
                Point3DError.InvalidAnimationDuration(duration),
            )
        }
        val target = if (where.size == 4) {
            val homogeneous = homogeneousCoordinates(where)
            when (homogeneous) {
                is GMResult.Ok -> homogeneous.value
                is GMResult.Err -> return homogeneous
            }
        } else {
            doubleArrayOf(1.0, where[0], where[1], where[2])
        }
        if (duration == 0.0) {
            return when (val result = setPosition(target)) {
                is GMResult.Ok -> {
                    board.update(this)
                    GMResult.Ok(this)
                }
                is GMResult.Err -> result
            }
        }

        val deltaX = coords[1] - target[1]
        val deltaY = coords[2] - target[2]
        val deltaZ = coords[3] - target[3]
        if (
            options.callback == null &&
            abs(deltaX) < Mat.eps &&
            abs(deltaY) < Mat.eps &&
            abs(deltaZ) < Mat.eps
        ) {
            return GMResult.Ok(this)
        }
        val steps = ceil(duration / board.animationDelay).toInt()
        val path = List(steps + 1) { index ->
            val reverseStep = steps - index
            val progress = animationProgress(
                step = reverseStep,
                steps = steps,
                effect = options.effect,
            )
            when (progress) {
                is GMResult.Ok -> doubleArrayOf(
                    target[1] + deltaX * progress.value,
                    target[2] + deltaY * progress.value,
                    target[3] + deltaZ * progress.value,
                )
                is GMResult.Err -> return progress
            }
        }
        return moveAlong(
            traversePath = path,
            timeMilliseconds = duration,
            options = Point3DMoveOptions(callback = options.callback),
        )
    }

    // JSXGraph 1.13.3: src/3d/point3d.js -> moveAlong.
    internal fun moveAlong(
        traversePath: List<DoubleArray>,
        timeMilliseconds: Double,
        options: Point3DMoveOptions = Point3DMoveOptions(),
    ): GMResult<Point3D, Point3DError> {
        if (!timeMilliseconds.isFinite() || timeMilliseconds < 0.0) {
            return GMResult.Err(
                Point3DError.InvalidAnimationDuration(timeMilliseconds),
            )
        }
        for (coordinates in traversePath) {
            if (coordinates.size !in setOf(3, 4)) {
                return GMResult.Err(
                    Point3DError.InvalidCoordinateCount(coordinates.size),
                )
            }
        }
        options.callback?.let { moveCallback = it }
        movePath.clear()
        movePath.addAll(traversePath.map(DoubleArray::copyOf))
        movementError = null
        if (movementScheduled) {
            return GMResult.Ok(this)
        }
        movementScheduled = true
        val stepTime =
            if (traversePath.isEmpty()) {
                timeMilliseconds
            } else {
                timeMilliseconds / traversePath.size
            }
        board.animationScheduler.schedule(stepTime, ::advanceMovement)
        val error = movementError
        return if (error == null) {
            GMResult.Ok(this)
        } else {
            GMResult.Err(error)
        }
    }

    private fun advanceMovement(): Boolean {
        val next = movePath.removeFirstOrNull()
        if (next != null) {
            when (val result = setPosition(next)) {
                is GMResult.Ok -> board.update(this)
                is GMResult.Err -> {
                    movementError =
                        Point3DError.AnimationPosition(result.error)
                    movePath.clear()
                }
            }
        }
        if (movePath.isNotEmpty()) {
            return true
        }
        movementScheduled = false
        val callback = moveCallback
        moveCallback = null
        if (callback != null) {
            try {
                callback()
            } catch (exception: Exception) {
                movementError = Point3DError.AnimationCallback(
                    exception.message
                        ?: exception::class.simpleName.orEmpty(),
                )
            }
        }
        return false
    }

    // JSXGraph 1.13.3: src/3d/point3d.js -> moveTo closure stepFun.
    private fun animationProgress(
        step: Int,
        steps: Int,
        effect: String?,
    ): GMResult<Double, Point3DError> {
        val progress = step.toDouble() / steps
        return when (effect) {
            null, "==" -> GMResult.Ok(progress)
            "<>" -> GMResult.Ok(sin(progress * PI * 0.5).pow(2))
            "<" -> GMResult.Ok(progress.pow(3))
            ">" -> GMResult.Ok(1.0 - (1.0 - progress).pow(3))
            else -> GMResult.Err(
                Point3DError.InvalidAnimationEffect(effect),
            )
        }
    }

    // JSXGraph 1.13.3: src/3d/point3d.js -> __evt__update3D.
    internal fun update3DEvent(oldCoordinates: DoubleArray) = Unit

    internal fun setPositionFrom2D(
        coordinates: DoubleArray,
        vertical: Boolean = false,
    ): GMResult<Point3D, Point3DError> {
        point2D.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = coordinates,
        )
        if (slide != null) {
            when (
                val result = projectScreenToSlide(
                    doubleArrayOf(point2D.X(), point2D.Y()),
                )
            ) {
                is GMResult.Ok -> {
                    coords = result.value
                    coordinateEvaluationError = null
                }
                is GMResult.Err -> {
                    coordinateEvaluationError = result.error
                    return result
                }
            }
        } else {
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
        }
        point2D.coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = view.project3DTo2D(coords),
        )
        projectedCoordinates = point2D.coords.usrCoords.copyOf()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/3d/point3d.js -> update slide projection.
    private fun projectToSlide(): GMResult<DoubleArray, Point3DError> {
        val target = slide ?: return GMResult.Ok(coords)
        val coordinates = doubleArrayOf(1.0, X(), Y(), Z())
        return when (target) {
            is Circle3D -> projectWithMutablePosition { parameters ->
                target.projectCoords(
                    coordinates = coordinates,
                    parameters = parameters,
                )
            }
            is Curve3D -> projectWithMutablePosition { parameters ->
                target.projectCoords(
                    coordinates = coordinates,
                    parameters = parameters,
                )
            }
            is Line3D -> {
                val parameters = positionArray(1)
                val projected = target.projectCoords(
                    coordinates = coordinates,
                    parameters = parameters,
                )
                position = parameters
                GMResult.Ok(projected)
            }
            is Plane3D -> {
                val parameters = positionArray(2)
                val projected = target.projectCoords(
                    coordinates = coordinates,
                    parameters = parameters,
                )
                position = parameters
                GMResult.Ok(projected)
            }
            is Sphere3D -> {
                val parameters = positionArray(2)
                when (
                    val result = target.projectCoords(
                        coordinates = coordinates,
                        parameters = parameters,
                    )
                ) {
                    is GMResult.Ok -> {
                        position = parameters
                        result
                    }
                    is GMResult.Err -> slideProjectionError(result.error)
                }
            }
            is Surface3D -> projectWithMutablePosition { parameters ->
                target.projectCoords(
                    coordinates = coordinates,
                    parameters = parameters,
                )
            }
            else -> GMResult.Err(
                Point3DError.UnsupportedSlide(target.elType),
            )
        }
    }

    // JSXGraph 1.13.3: src/3d/point3d.js -> update drag branch;
    // src/3d/element3d.js -> projectScreenCoords.
    private fun projectScreenToSlide(
        screenCoordinates: DoubleArray,
    ): GMResult<DoubleArray, Point3DError> {
        val target = slide ?: return GMResult.Ok(coords)
        val cyclic = evalVisProp("cyclic") == true
        return when (target) {
            is Circle3D -> projectWithMutablePosition { parameters ->
                target.projectScreenCoords(
                    screenCoordinates = screenCoordinates,
                    parameters = parameters,
                    cyclic = cyclic,
                )
            }
            is Curve3D -> projectWithMutablePosition { parameters ->
                target.projectScreenCoords(
                    screenCoordinates = screenCoordinates,
                    parameters = parameters,
                    cyclic = cyclic,
                )
            }
            is Line3D -> GMResult.Ok(
                target.projectScreenCoords(screenCoordinates),
            )
            is Plane3D -> projectWithMutablePosition { parameters ->
                target.projectScreenCoords(
                    screenCoordinates = screenCoordinates,
                    parameters = parameters,
                    cyclic = cyclic,
                )
            }
            is Sphere3D -> projectWithMutablePosition { parameters ->
                target.projectScreenCoords(
                    screenCoordinates = screenCoordinates,
                    parameters = parameters,
                    cyclic = cyclic,
                )
            }
            is Surface3D -> projectWithMutablePosition { parameters ->
                target.projectScreenCoords(
                    screenCoordinates = screenCoordinates,
                    parameters = parameters,
                    cyclic = cyclic,
                )
            }
            else -> GMResult.Err(
                Point3DError.UnsupportedSlide(target.elType),
            )
        }
    }

    private fun positionArray(minimumSize: Int): DoubleArray =
        DoubleArray(maxOf(position.size, minimumSize)) { index ->
            position.getOrElse(index) { 0.0 }
        }

    private fun <E> projectWithMutablePosition(
        projection: (
            MutableList<Double>,
        ) -> GMResult<DoubleArray, E>,
    ): GMResult<DoubleArray, Point3DError> {
        val parameters = position.toMutableList()
        return when (val result = projection(parameters)) {
            is GMResult.Ok -> {
                position = parameters.toDoubleArray()
                result
            }
            is GMResult.Err -> slideProjectionError(result.error)
        }
    }

    private fun slideProjectionError(
        error: Any?,
    ): GMResult.Err<Point3DError.SlideProjection> =
        GMResult.Err(
            Point3DError.SlideProjection(error.toString()),
        )

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
            slide: GeometryElement3D? = null,
            attributes: Map<String, Any?> = emptyMap(),
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
                slide = slide,
                attributes = attributes,
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
            slide: GeometryElement3D? = null,
            attributes: Map<String, Any?> = emptyMap(),
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
                slide = slide,
                attributes = attributes,
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
            slide: GeometryElement3D? = null,
            attributes: Map<String, Any?> = emptyMap(),
        ): GMResult<Point3D, Point3DError> {
            if (slide != null) {
                if (slide.view !== view) {
                    return GMResult.Err(
                        Point3DError.SlideViewMismatch(slide.id),
                    )
                }
                if (view.board.elementById(slide.id) !== slide) {
                    return GMResult.Err(
                        Point3DError.SlideNotRegistered(slide.id),
                    )
                }
                if (!supportsSlideProjection(slide)) {
                    return GMResult.Err(
                        Point3DError.UnsupportedSlide(slide.elType),
                    )
                }
            }
            val point = Point3D(
                view = view,
                coordinateSource = coordinateSource,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                isFixed = fixed,
                slide = slide,
            )
            point.setCreationAttributes(attributes)
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
            point.configureElement2D(
                proxy = proxy,
                attributes = attributes,
                defaultWithLabel = true,
            )
            proxy.dump = false
            point.point2D = proxy
            point.element2D = proxy
            point.inherits += proxy
            point.isDraggable = proxy.isDraggable
            point.addChild(proxy)
            proxy.setParents(listOf(point))
            point.projectedCoordinates =
                proxy.coords.usrCoords.copyOf()
            for (dependency in dependencies) {
                dependency.addChild(point)
            }
            if (slide != null) {
                slide.addChild(point)
                point.setParents(listOf(slide))
            }
            // JSXGraph 1.13.3: src/base/board.js -> Board.create
            // invokes el.fullUpdate() after createPoint3D returns.
            point.fullUpdate()
            return GMResult.Ok(point)
        }

        private fun supportsSlideProjection(
            slide: GeometryElement3D,
        ): Boolean =
            slide is Circle3D ||
                slide is Curve3D ||
                slide is Line3D ||
                slide is Plane3D ||
                slide is Sphere3D ||
                slide is Surface3D
    }
}
