/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/linspace3d.js -> Line3D / createLine3D
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

internal sealed interface Line3DDynamicError {
    data class Rejected(
        val reason: String,
    ) : Line3DDynamicError
}

internal fun interface Line3DScalarEvaluator {
    fun evaluate(): GMResult<Double, Line3DDynamicError>
}

internal fun interface Line3DArrayEvaluator {
    fun evaluate(): GMResult<DoubleArray, Line3DDynamicError>
}

internal sealed interface Line3DCoordinateValue {
    data class Numeric(
        val value: Double,
    ) : Line3DCoordinateValue

    data class Dynamic(
        val evaluator: Line3DScalarEvaluator,
    ) : Line3DCoordinateValue
}

internal sealed interface Line3DDirectionSource {
    data class Points(
        val point1: Point3D,
        val point2: Point3D,
    ) : Line3DDirectionSource

    data class Line(
        val line: Line3D,
    ) : Line3DDirectionSource

    data class Values(
        val values: List<Line3DCoordinateValue>,
    ) : Line3DDirectionSource

    data class Function(
        val evaluator: Line3DArrayEvaluator,
    ) : Line3DDirectionSource
}

internal sealed interface Line3DError {
    data class ParentViewMismatch(
        val parentIndex: Int,
    ) : Line3DError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : Line3DError

    data class InvalidDirectionCount(
        val count: Int,
    ) : Line3DError

    data class InvalidRangeCount(
        val count: Int,
    ) : Line3DError

    data class DirectionEvaluation(
        val coordinateIndex: Int?,
        val error: Line3DDynamicError,
    ) : Line3DError

    data class RangeEvaluation(
        val rangeIndex: Int,
        val error: Line3DDynamicError,
    ) : Line3DError

    data class InvalidTransformationCount(
        val count: Int,
    ) : Line3DError

    data class InvalidBaseElement(
        val baseElementId: String?,
    ) : Line3DError

    data class TransformationEvaluation(
        val transformationIndex: Int,
        val error: TransformationError,
    ) : Line3DError

    data class Registration(
        val error: BoardError,
    ) : Line3DError

    data class EndpointFactory(
        val endpointIndex: Int,
        val error: Point3DError,
    ) : Line3DError

    data class ProxyLineFactory(
        val error: LineError,
    ) : Line3DError
}

/**
 * Faithful Line3D lifecycle translated from JSXGraph 1.13.3.
 *
 * The 3D line owns two projected Point3D endpoints and an ordinary 2D segment
 * proxy. Two-point lines keep their defining points separate from those
 * visible endpoints because straightFirst/straightLast can extend the proxy
 * to the View3D bounding box.
 */
internal class Line3D private constructor(
    view: View3D,
    internal val point: Point3D,
    private val directionSource: Line3DDirectionSource,
    private val rangeSource: MutableList<Line3DCoordinateValue>,
    private val twoPointForm: Boolean,
    private val straightFirst: Boolean,
    private val straightLast: Boolean,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    internal val isFixed: Boolean,
) : GeometryElement3D(
    view = view,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_LINE3D,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal var vec: DoubleArray = DoubleArray(4)
        private set
    internal var evaluatedRange: DoubleArray =
        doubleArrayOf(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        private set
    internal lateinit var endpoints: Array<Point3D>
        private set
    internal lateinit var point1: Point3D
        private set
    internal lateinit var point2: Point3D
        private set
    internal lateinit var line2D: Line
        private set
    internal var directionEvaluationError: Line3DError? = null
        private set
    internal var rangeEvaluationError: Line3DError? = null
        private set
    internal var transformationEvaluationError: Line3DError? = null
        private set

    init {
        elType = LINE_3D_ELEMENT_TYPE
    }

    // JSXGraph: src/3d/linspace3d.js -> Line3D.updateCoords.
    internal fun updateCoords(): Line3D {
        when (val result = evaluateDirection()) {
            is GMResult.Ok -> {
                vec = result.value
                directionEvaluationError = null
            }
            is GMResult.Err -> {
                vec = DoubleArray(4) { Double.NaN }
                directionEvaluationError = result.error
            }
        }
        when (val result = evaluateRange()) {
            is GMResult.Ok -> {
                evaluatedRange = result.value
                rangeEvaluationError = null
            }
            is GMResult.Err -> {
                evaluatedRange = DoubleArray(2) { Double.NaN }
                rangeEvaluationError = result.error
            }
        }
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Line3D.getPointCoords.
    internal fun getPointCoords(ratio: Double): DoubleArray {
        var boundedRatio = view.intersectionLineCube(
            point = point.coords,
            direction = vec,
            ratio = ratio,
        )
        if (abs(boundedRatio) == Double.POSITIVE_INFINITY) {
            boundedRatio = 0.0
        }
        return DoubleArray(4) { index ->
            point.coords[index] + vec[index] * boundedRatio
        }
    }

    // JSXGraph: src/3d/linspace3d.js -> Line3D.addTransform.
    internal fun addTransform(
        element: Line3D,
        transformation: Transformation,
    ): Line3D = addTransform(element, listOf(transformation))

    internal fun addTransform(
        element: Line3D,
        newTransformations: Iterable<Transformation>,
    ): Line3D {
        point.addTransform(element.point, newTransformations)
        addTransformGeneric(element, newTransformations)
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Line3D.removeTransform.
    internal fun removeTransform(
        transformation: Transformation,
    ): Line3D {
        point.removeTransform(transformation)
        removeTransformGeneric(listOf(transformation))
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Line3D.clearTransforms.
    internal fun clearTransforms(): Line3D {
        point.clearTransforms()
        clearTransformsGeneric()
        transformationEvaluationError = null
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Line3D.updateTransform.
    internal fun updateTransform(): Line3D {
        if (transformations.isEmpty() || baseElement == null) {
            transformationEvaluationError = null
            return this
        }
        val baseLine = baseElement as? Line3D
        if (baseLine == null) {
            vec = DoubleArray(4) { Double.NaN }
            transformationEvaluationError =
                Line3DError.InvalidBaseElement(baseElement?.id)
            return this
        }
        var transformed =
            if (baseLine === this) {
                vec.copyOf()
            } else {
                baseLine.vec.copyOf()
            }
        for ((index, transformation) in transformations.withIndex()) {
            when (val result = transformation.applyResult(transformed)) {
                is GMResult.Ok -> transformed = result.value
                is GMResult.Err -> {
                    vec = DoubleArray(4) { Double.NaN }
                    transformationEvaluationError =
                        Line3DError.TransformationEvaluation(
                            transformationIndex = index,
                            error = result.error,
                        )
                    return this
                }
            }
        }
        vec = transformed
        transformationEvaluationError = null
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Line3D.update.
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        updateCoords()
        if (directionEvaluationError == null) {
            updateTransform()
        }
        return this
    }

    override fun updateRenderer(): GeometryElement {
        needsUpdate = false
        return this
    }

    // JSXGraph: src/3d/linspace3d.js -> Line3D.projectCoords.
    internal fun projectCoords(
        coordinates: DoubleArray,
        parameters: DoubleArray,
    ): DoubleArray {
        val first = getPointCoords(0.0)
        val second = getPointCoords(1.0)
        val direction = doubleArrayOf(
            second[0] - first[0],
            second[1] - first[1],
            second[2] - first[2],
        )
        val difference = doubleArrayOf(
            coordinates[0] - first[0],
            coordinates[1] - first[1],
            coordinates[2] - first[2],
        )
        val parameter =
            Mat.innerProduct(difference, direction) /
                Mat.innerProduct(direction, direction)
        val clamped = min(
            max(parameter, evaluatedRange[0]),
            evaluatedRange[1],
        )
        parameters[0] = clamped
        return getPointCoords(clamped)
    }

    // JSXGraph: src/3d/linspace3d.js -> Line3D.projectScreenCoords.
    internal fun projectScreenCoords(
        screenCoordinates: DoubleArray,
    ): DoubleArray {
        val firstRatio = finiteProjectionRatio(
            index = 0,
            ratio = evaluatedRange[0],
        )
        val secondRatio = finiteProjectionRatio(
            index = 1,
            ratio = evaluatedRange[1],
        )
        return view.projectScreenToSegment(
            screen = screenCoordinates,
            first = getPointCoords(firstRatio),
            second = getPointCoords(secondRatio),
        )
    }

    // JSXGraph: src/3d/linspace3d.js -> Line3D.updateZIndex.
    internal fun updateZIndex(): Line3D {
        if (!this::endpoints.isInitialized) {
            return this
        }
        val midpoint = doubleArrayOf(
            1.0,
            0.5 * (endpoints[0].X() + endpoints[1].X()),
            0.5 * (endpoints[0].Y() + endpoints[1].Y()),
            0.5 * (endpoints[0].Z() + endpoints[1].Z()),
        )
        zIndex = Mat.innerProduct(
            view.matrix3DRotShift[3],
            midpoint,
        )
        return this
    }

    private fun evaluateDirection(): GMResult<DoubleArray, Line3DError> =
        when (val source = directionSource) {
            is Line3DDirectionSource.Points -> GMResult.Ok(
                doubleArrayOf(
                    0.0,
                    source.point2.X() - source.point1.X(),
                    source.point2.Y() - source.point1.Y(),
                    source.point2.Z() - source.point1.Z(),
                ),
            )
            is Line3DDirectionSource.Line ->
                GMResult.Ok(source.line.vec.copyOf())
            is Line3DDirectionSource.Function -> {
                val evaluated = when (val result = source.evaluator.evaluate()) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        Line3DError.DirectionEvaluation(
                            coordinateIndex = null,
                            error = result.error,
                        ),
                    )
                }
                homogeneousDirection(evaluated)
            }
            is Line3DDirectionSource.Values -> {
                if (source.values.size !in setOf(3, 4)) {
                    return GMResult.Err(
                        Line3DError.InvalidDirectionCount(
                            source.values.size,
                        ),
                    )
                }
                val direction = DoubleArray(4)
                val offset =
                    if (source.values.size == 3) {
                        direction[0] = 0.0
                        1
                    } else {
                        0
                    }
                for ((index, value) in source.values.withIndex()) {
                    direction[offset + index] = when (value) {
                        is Line3DCoordinateValue.Numeric -> value.value
                        is Line3DCoordinateValue.Dynamic -> when (
                            val result = value.evaluator.evaluate()
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return GMResult.Err(
                                Line3DError.DirectionEvaluation(
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

    private fun evaluateRange(): GMResult<DoubleArray, Line3DError> {
        if (rangeSource.size != 2) {
            return GMResult.Err(
                Line3DError.InvalidRangeCount(rangeSource.size),
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
                        Line3DError.RangeEvaluation(
                            rangeIndex = index,
                            error = result.error,
                        ),
                    )
                }
            }
        }
        return GMResult.Ok(range)
    }

    private fun homogeneousDirection(
        direction: DoubleArray,
    ): GMResult<DoubleArray, Line3DError> =
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
                Line3DError.InvalidDirectionCount(direction.size),
            )
        }

    private fun endpointRatio(endpointIndex: Int): Double =
        if (twoPointForm) {
            when (endpointIndex) {
                0 -> if (straightFirst) {
                    Double.NEGATIVE_INFINITY
                } else {
                    0.0
                }
                else -> if (straightLast) {
                    Double.POSITIVE_INFINITY
                } else {
                    1.0
                }
            }
        } else {
            evaluatedRange[endpointIndex]
        }

    private fun finiteProjectionRatio(
        index: Int,
        ratio: Double,
    ): Double {
        val finite = when (ratio) {
            Double.POSITIVE_INFINITY -> PROJECTIVE_RANGE_LIMIT
            Double.NEGATIVE_INFINITY -> -PROJECTIVE_RANGE_LIMIT
            else -> ratio
        }
        if (
            finite != ratio &&
            rangeSource[index] is Line3DCoordinateValue.Numeric
        ) {
            rangeSource[index] = Line3DCoordinateValue.Numeric(finite)
            evaluatedRange[index] = finite
        }
        return finite
    }

    internal companion object {
        private const val LINE_3D_ID_PREFIX = "line3d"
        private const val LINE_3D_ELEMENT_TYPE = "line3d"
        private const val PROJECTIVE_RANGE_LIMIT = 100000.0

        // JSXGraph: src/3d/linspace3d.js -> createLine3D, two-point form.
        internal fun create(
            view: View3D,
            point1: Point3D,
            point2: Point3D,
            ownsPoint1: Boolean = false,
            ownsPoint2: Boolean = false,
            straightFirst: Boolean = false,
            straightLast: Boolean = false,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<Line3D, Line3DError> =
            createInternal(
                view = view,
                point = point1,
                directionSource =
                    Line3DDirectionSource.Points(point1, point2),
                rangeSource = listOf(
                    Line3DCoordinateValue.Numeric(0.0),
                    Line3DCoordinateValue.Numeric(1.0),
                ),
                definingPoints = listOf(point1, point2),
                ownedPoints = buildList {
                    if (ownsPoint1) {
                        add(point1)
                    }
                    if (ownsPoint2 && point2 !== point1) {
                        add(point2)
                    }
                },
                dependencies = emptyList(),
                twoPointForm = true,
                straightFirst = straightFirst,
                straightLast = straightLast,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )

        // JSXGraph: src/3d/linspace3d.js -> createLine3D,
        // point-direction-range form.
        internal fun create(
            view: View3D,
            point: Point3D,
            directionSource: Line3DDirectionSource,
            rangeSource: List<Line3DCoordinateValue> = defaultRange(),
            ownsPoint: Boolean = false,
            dependencies: Iterable<GeometryElement> = emptyList(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<Line3D, Line3DError> =
            createInternal(
                view = view,
                point = point,
                directionSource = directionSource,
                rangeSource = rangeSource.toMutableList(),
                definingPoints = listOf(point),
                ownedPoints =
                    if (ownsPoint) {
                        listOf(point)
                    } else {
                        emptyList()
                    },
                dependencies = dependencies,
                twoPointForm = false,
                straightFirst = false,
                straightLast = false,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )

        // JSXGraph: src/3d/linspace3d.js -> createLine3D,
        // transformed Line3D form.
        internal fun create(
            view: View3D,
            baseLine: Line3D,
            transformations: List<Transformation>,
            rangeSource: List<Line3DCoordinateValue> = defaultRange(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<Line3D, Line3DError> {
            if (baseLine.view !== view) {
                return GMResult.Err(
                    Line3DError.ParentViewMismatch(parentIndex = 0),
                )
            }
            if (transformations.isEmpty()) {
                return GMResult.Err(
                    Line3DError.InvalidTransformationCount(0),
                )
            }
            val point = when (
                val result = Point3D.create(
                    view = view,
                    coordinates = doubleArrayOf(0.0, 0.0, 0.0),
                    name = "",
                    fixed = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    Line3DError.EndpointFactory(
                        endpointIndex = -1,
                        error = result.error,
                    ),
                )
            }
            val line = when (
                val result = create(
                    view = view,
                    point = point,
                    directionSource = Line3DDirectionSource.Values(
                        listOf(
                            Line3DCoordinateValue.Numeric(0.0),
                            Line3DCoordinateValue.Numeric(0.0),
                            Line3DCoordinateValue.Numeric(0.0),
                            Line3DCoordinateValue.Numeric(0.0001),
                        ),
                    ),
                    rangeSource = rangeSource,
                    ownsPoint = true,
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
            line.addTransform(baseLine, transformations)
            line.addParents(listOf(baseLine))
            baseLine.addChild(line)
            line.prepareUpdate().update()
            line.transformationEvaluationError?.let { error ->
                view.board.removeObject(line)
                return GMResult.Err(error)
            }
            return GMResult.Ok(line)
        }

        private fun createInternal(
            view: View3D,
            point: Point3D,
            directionSource: Line3DDirectionSource,
            rangeSource: List<Line3DCoordinateValue>,
            definingPoints: List<Point3D>,
            ownedPoints: List<Point3D>,
            dependencies: Iterable<GeometryElement>,
            twoPointForm: Boolean,
            straightFirst: Boolean,
            straightLast: Boolean,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
            fixed: Boolean,
        ): GMResult<Line3D, Line3DError> {
            for ((index, definingPoint) in definingPoints.withIndex()) {
                validatePoint(view, definingPoint, index)?.let {
                    cleanupOwnedPoints(view, ownedPoints)
                    return GMResult.Err(it)
                }
            }
            validateDirection(view, directionSource)?.let {
                cleanupOwnedPoints(view, ownedPoints)
                return GMResult.Err(it)
            }
            if (rangeSource.size != 2) {
                cleanupOwnedPoints(view, ownedPoints)
                return GMResult.Err(
                    Line3DError.InvalidRangeCount(rangeSource.size),
                )
            }
            var lineReference: Line3D? = null
            val precreatedEndpoints =
                if (twoPointForm) {
                    null
                } else {
                    when (
                        val result = createEndpoints(
                            view = view,
                            needsRegularUpdate = needsRegularUpdate,
                            line = { lineReference },
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> {
                            cleanupOwnedPoints(view, ownedPoints)
                            return result
                        }
                    }
                }
            val line = Line3D(
                view = view,
                point = point,
                directionSource = directionSource,
                rangeSource = rangeSource.toMutableList(),
                twoPointForm = twoPointForm,
                straightFirst = straightFirst,
                straightLast = straightLast,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                isFixed = fixed,
            )
            lineReference = line
            line.updateCoords()
            line.directionEvaluationError?.let {
                cleanupEndpoints(view, precreatedEndpoints)
                cleanupOwnedPoints(view, ownedPoints)
                return GMResult.Err(it)
            }
            line.rangeEvaluationError?.let {
                cleanupEndpoints(view, precreatedEndpoints)
                cleanupOwnedPoints(view, ownedPoints)
                return GMResult.Err(it)
            }
            when (
                val registration =
                    view.board.setId(line, LINE_3D_ID_PREFIX)
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> {
                    cleanupEndpoints(view, precreatedEndpoints)
                    cleanupOwnedPoints(view, ownedPoints)
                    return GMResult.Err(
                        Line3DError.Registration(registration.error),
                    )
                }
            }
            line.registerInView()
            line.addParents(definingPoints)
            for (definingPoint in definingPoints.distinct()) {
                if (definingPoint in ownedPoints) {
                    line.addChild(definingPoint)
                } else {
                    definingPoint.addChild(line)
                }
            }
            val directionLine =
                (directionSource as? Line3DDirectionSource.Line)?.line
            if (directionLine != null) {
                line.addParents(listOf(directionLine))
                directionLine.addChild(line)
            }
            for (dependency in dependencies) {
                dependency.addChild(line)
            }
            val endpointArray =
                precreatedEndpoints ?: when (
                    val result = createEndpoints(
                        view = view,
                        needsRegularUpdate = needsRegularUpdate,
                        line = { line },
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        view.board.removeObject(line)
                        return result
                    }
                }
            for (endpoint in endpointArray) {
                line.addChild(endpoint)
            }
            val firstEndpoint = endpointArray[0]
            val secondEndpoint = endpointArray[1]
            line.endpoints = arrayOf(firstEndpoint, secondEndpoint)
            line.point1 =
                if (twoPointForm) {
                    definingPoints[0]
                } else {
                    firstEndpoint
                }
            line.point2 =
                if (twoPointForm) {
                    definingPoints[1]
                } else {
                    secondEndpoint
                }
            val proxy = when (
                val result = Line.createSegment(
                    board = view.board,
                    point1 = firstEndpoint.point2D,
                    point2 = secondEndpoint.point2D,
                    name = "",
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    view.board.removeObject(line)
                    return GMResult.Err(
                        Line3DError.ProxyLineFactory(result.error),
                    )
                }
            }
            proxy.dump = false
            proxy.isDraggable = false
            proxy.addParents(listOf(line))
            line.line2D = proxy
            line.element2D = proxy
            line.addChild(proxy)
            line.isDraggable = !fixed
            line.prepareUpdate().update()
            for (endpoint in line.endpoints) {
                endpoint.prepareUpdate().update()
            }
            proxy.prepareUpdate().update()
            return GMResult.Ok(line)
        }

        private fun createEndpoints(
            view: View3D,
            needsRegularUpdate: Boolean,
            line: () -> Line3D?,
        ): GMResult<Array<Point3D>, Line3DError> {
            val endpoints = mutableListOf<Point3D>()
            for (index in 0..1) {
                val endpoint = when (
                    val result = Point3D.create(
                        view = view,
                        coordinateSource =
                            Point3DCoordinateSource.Function(
                                Point3DArrayEvaluator {
                                    val owner = line()
                                    GMResult.Ok(
                                        if (owner == null) {
                                            doubleArrayOf(1.0, 0.0, 0.0, 0.0)
                                        } else {
                                            owner.getPointCoords(
                                                owner.endpointRatio(index),
                                            )
                                        },
                                    )
                                },
                            ),
                        name = "",
                        needsRegularUpdate = needsRegularUpdate,
                        fixed = true,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        cleanupEndpoints(view, endpoints.toTypedArray())
                        return GMResult.Err(
                            Line3DError.EndpointFactory(
                                endpointIndex = index,
                                error = result.error,
                            ),
                        )
                    }
                }
                endpoints += endpoint
            }
            return GMResult.Ok(endpoints.toTypedArray())
        }

        private fun validatePoint(
            view: View3D,
            point: Point3D,
            parentIndex: Int,
        ): Line3DError? =
            when {
                point.view !== view ->
                    Line3DError.ParentViewMismatch(parentIndex)
                view.board.elementById(point.id) !== point ->
                    Line3DError.ParentNotRegistered(
                        parentIndex = parentIndex,
                        id = point.id,
                    )
                else -> null
            }

        private fun validateDirection(
            view: View3D,
            source: Line3DDirectionSource,
        ): Line3DError? =
            when (source) {
                is Line3DDirectionSource.Points -> null
                is Line3DDirectionSource.Line ->
                    if (source.line.view !== view) {
                        Line3DError.ParentViewMismatch(parentIndex = 1)
                    } else if (
                        view.board.elementById(source.line.id) !== source.line
                    ) {
                        Line3DError.ParentNotRegistered(
                            parentIndex = 1,
                            id = source.line.id,
                        )
                    } else {
                        null
                    }
                is Line3DDirectionSource.Values ->
                    if (source.values.size !in setOf(3, 4)) {
                        Line3DError.InvalidDirectionCount(source.values.size)
                    } else {
                        null
                    }
                is Line3DDirectionSource.Function -> null
            }

        private fun cleanupOwnedPoints(
            view: View3D,
            ownedPoints: List<Point3D>,
        ) {
            for (ownedPoint in ownedPoints.distinct()) {
                view.board.removeObject(ownedPoint)
            }
        }

        private fun cleanupEndpoints(
            view: View3D,
            endpoints: Array<Point3D>?,
        ) {
            for (endpoint in endpoints.orEmpty()) {
                view.board.removeObject(endpoint)
            }
        }

        private fun defaultRange(): List<Line3DCoordinateValue> =
            listOf(
                Line3DCoordinateValue.Numeric(Double.NEGATIVE_INFINITY),
                Line3DCoordinateValue.Numeric(Double.POSITIVE_INFINITY),
            )
    }
}
