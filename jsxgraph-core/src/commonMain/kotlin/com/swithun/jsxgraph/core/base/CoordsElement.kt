/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/coordselement.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.ContinuousCurve2D
import com.swithun.jsxgraph.core.math.ContinuousCurveType
import com.swithun.jsxgraph.core.math.DiscreteCurve2D
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.NumericsPoint2D
import com.swithun.jsxgraph.core.math.ParametricCurve2D
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin

internal sealed interface CoordinateConstraintError {
    data class UnsupportedFunctionCount(
        val count: Int,
    ) : CoordinateConstraintError

    data class Evaluation(
        val coordinateIndex: Int,
        val error: JessieCodeRuntimeError,
    ) : CoordinateConstraintError

    data class NonNumericResult(
        val coordinateIndex: Int,
        val actualType: String,
    ) : CoordinateConstraintError

    data class CoordinateArrayResultExpected(
        val actualType: String,
    ) : CoordinateConstraintError
}

internal sealed interface CoordinateTransformationError {
    data class InvalidBaseElement(
        val baseElementId: String?,
    ) : CoordinateTransformationError

    data class Evaluation(
        val transformationIndex: Int,
        val error: TransformationError,
    ) : CoordinateTransformationError

    data class NonInvertibleCompositeMatrix(
        val transformationCount: Int,
    ) : CoordinateTransformationError
}

internal sealed interface CoordsElementAnimationError {
    data class InvalidCoordinateCount(
        val count: Int,
    ) : CoordsElementAnimationError

    data class InvalidDuration(
        val milliseconds: Double,
    ) : CoordsElementAnimationError

    data class InvalidRepeatCount(
        val count: Int,
    ) : CoordsElementAnimationError

    data object EmptyPath : CoordsElementAnimationError

    data class InvalidStepCount(
        val count: Int,
    ) : CoordsElementAnimationError

    data class UnsupportedSlideObject(
        val elementType: String,
    ) : CoordsElementAnimationError

    data object GliderRequired : CoordsElementAnimationError

    data object UnboundedSynchronousAnimation :
        CoordsElementAnimationError

    data class Position(
        val error: CoordinateTransformationError,
    ) : CoordsElementAnimationError

    data class Callback(
        val message: String,
    ) : CoordsElementAnimationError
}

internal data class CoordsElementMoveOptions(
    val callback: (() -> Unit)? = null,
    val effect: String? = null,
    val interpolate: Boolean = true,
    val repeat: Int = 1,
)

/**
 * Initial coordinate-access slice of JXG.CoordsElement.
 *
 * JessieCode string and function coordinate constraints and the persistent
 * 2D transformation lifecycle and the line-backed Glider update hook are
 * present. Coords-object, remaining Glider hosts, animation, and renderer
 * behavior remain in the untranslated element model.
 * This class stays internal until those lifecycle contracts are available.
 */
internal open class CoordsElement(
    board: Board,
    coordinates: DoubleArray = doubleArrayOf(1.0, 0.0, 0.0),
    id: String = "",
    name: String? = null,
    type: Int = 0,
    elementClass: Int = Const.OBJECT_CLASS_OTHER,
    needsRegularUpdate: Boolean = true,
    coordinateFunctions: List<JessieCodeCoordinateFunction> = emptyList(),
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type = type,
    elementClass = elementClass,
    needsRegularUpdate = needsRegularUpdate,
),
    NumericsPoint2D {
    internal var coordinateFunctions:
        List<JessieCodeCoordinateFunction> = coordinateFunctions
        private set

    // JSXGraph: src/base/coordselement.js -> CoordsElement constructor.
    internal val coords = Coords(
        method = Const.COORDS_BY_USER,
        coordinates = coordinates,
        board = board,
    )

    internal val initialCoords = Coords(
        method = Const.COORDS_BY_USER,
        coordinates = coordinates,
        board = board,
    )

    internal val actualCoords = Coords(
        method = Const.COORDS_BY_USER,
        coordinates = coordinates,
        board = board,
    )

    internal var position: Double? = null
    internal var isConstrained: Boolean = coordinateFunctions.isNotEmpty()
    internal var onPolygon: Boolean = false
    internal var slideObject: GeometryElement? = null
    internal val slideObjects = mutableListOf<GeometryElement>()
    internal var needsUpdateFromParent: Boolean = true
    // JSXGraph 1.13.3: src/base/coordselement.js -> groups.
    internal val groups = mutableListOf<String>()
    internal var Xjc: String? = null
    internal var Yjc: String? = null
    internal var coordinateEvaluationError: CoordinateConstraintError? = null
        private set
    internal var transformationEvaluationError:
        CoordinateTransformationError? = null
        private set
    private val movePath = ArrayDeque<DoubleArray>()
    private var moveCallback: (() -> Unit)? = null
    private var movementScheduled: Boolean = false
    private var gliderAnimationRunning: Boolean = false
    private var gliderAnimationStep: Int = 0
    private var gliderAnimationRounds: Int = 0
    internal var movementError: CoordsElementAnimationError? = null
        private set

    internal fun reportMovementError(
        error: CoordsElementAnimationError,
    ) {
        movementError = error
    }

    protected fun setTransformationEvaluationError(
        error: CoordinateTransformationError,
    ) {
        transformationEvaluationError = error
    }

    protected fun clearTransformationEvaluationError() {
        transformationEvaluationError = null
    }

    init {
        isDraggable = coordinateFunctions.isEmpty()
    }

    internal open val isReal: Boolean
        get() = coords.isReal()

    // JSXGraph: src/base/coordselement.js -> X.
    override fun X(): Double = coords.usrCoords[1]

    // JSXGraph: src/base/coordselement.js -> Y.
    override fun Y(): Double = coords.usrCoords[2]

    // JSXGraph: src/base/coordselement.js -> Z.
    internal fun Z(): Double = coords.usrCoords[0]

    // JSXGraph: src/base/coordselement.js -> Coords.
    internal fun Coords(withZ: Boolean = false): DoubleArray =
        if (withZ) {
            coords.usrCoords.copyOf()
        } else {
            coords.usrCoords.copyOfRange(1, coords.usrCoords.size)
        }

    // JSXGraph: src/base/coordselement.js -> XEval, YEval, ZEval.
    internal fun XEval(): Double = coords.usrCoords[1]

    internal fun YEval(): Double = coords.usrCoords[2]

    internal fun ZEval(): Double = coords.usrCoords[0]

    // JSXGraph: src/base/coordselement.js -> Dist.
    internal fun Dist(other: CoordsElement): Double =
        if (isReal && other.isReal) {
            coords.distance(Const.COORDS_BY_USER, other.coords)
        } else {
            Double.NaN
        }

    // JSXGraph: src/base/coordselement.js -> addConstraint / updateConstraint
    internal fun coordinateConstraintResult(): GMResult<
        DoubleArray,
        CoordinateConstraintError,
        > = coordinateConstraintResult(coordinateFunctions)

    internal fun coordinateConstraintResult(
        functions: List<JessieCodeCoordinateFunction>,
    ): GMResult<DoubleArray, CoordinateConstraintError> {
        if (functions.isEmpty()) {
            return GMResult.Ok(coords.usrCoords.copyOf())
        }
        if (
            functions.size == 1 &&
            functions[0].returnsCoordinateArray
        ) {
            val value = when (val result = functions[0].evaluate()) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    CoordinateConstraintError.Evaluation(
                        coordinateIndex = 0,
                        error = result.error,
                    ),
                )
            }
            val array = value as? JessieCodeRuntimeValue.ArrayValue
                ?: return GMResult.Err(
                    CoordinateConstraintError
                        .CoordinateArrayResultExpected(
                            actualType = runtimeType(value),
                        ),
                )
            val coordinates = DoubleArray(array.values.size)
            for ((index, coordinate) in array.values.withIndex()) {
                val number = coordinate as?
                    JessieCodeRuntimeValue.NumberValue
                    ?: return GMResult.Err(
                        CoordinateConstraintError.NonNumericResult(
                            coordinateIndex = index,
                            actualType = runtimeType(coordinate),
                        ),
                    )
                coordinates[index] = number.value
            }
            return GMResult.Ok(coordinates)
        }
        if (functions.size < 2) {
            return GMResult.Err(
                CoordinateConstraintError.UnsupportedFunctionCount(
                    functions.size,
                ),
            )
        }

        val coordinateCount =
            if (functions.size == 2) 2 else 3
        val values = DoubleArray(coordinateCount)
        for (index in 0 until coordinateCount) {
            when (val result = functions[index].evaluate()) {
                is GMResult.Err -> return GMResult.Err(
                    CoordinateConstraintError.Evaluation(
                        coordinateIndex = index,
                        error = result.error,
                    ),
                )
                is GMResult.Ok -> {
                    val value = result.value
                    if (value !is JessieCodeRuntimeValue.NumberValue) {
                        return GMResult.Err(
                            CoordinateConstraintError.NonNumericResult(
                                coordinateIndex = index,
                                actualType = runtimeType(value),
                            ),
                        )
                    }
                    values[index] = value.value
                }
            }
        }
        return GMResult.Ok(values)
    }

    // JSXGraph: src/base/coordselement.js -> addConstraint
    internal fun replaceCoordinateFunctions(
        functions: List<JessieCodeCoordinateFunction>,
    ): CoordsElement {
        val oldDependencies = coordinateFunctions
            .flatMap { it.dependencies.values }
            .associateBy { it.id }
        val newDependencies = functions
            .flatMap { it.dependencies.values }
            .associateBy { it.id }
        for ((id, dependency) in oldDependencies) {
            if (id !in newDependencies) {
                dependency.removeChild(this)
            }
        }

        coordinateFunctions = functions
        isConstrained = functions.isNotEmpty()
        isDraggable = functions.isEmpty()
        addParentsFromJCFunctions(functions)
        return this
    }

    // JSXGraph: src/base/coordselement.js -> updateConstraint
    internal open fun updateConstraint(): CoordsElement {
        if (coordinateFunctions.isEmpty()) {
            return this
        }

        when (val result = coordinateConstraintResult()) {
            is GMResult.Ok -> applyCoordinateConstraint(result.value)
            is GMResult.Err -> {
                coordinateEvaluationError = result.error
                coords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = DoubleArray(
                        if (coordinateFunctions.size == 2) 2 else 3,
                    ) { Double.NaN },
                )
            }
        }
        return this
    }

    internal fun applyCoordinateConstraint(
        coordinates: DoubleArray,
    ): CoordsElement {
        coordinateEvaluationError = null
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = coordinates,
        )
        return this
    }

    // JSXGraph: src/base/coordselement.js -> updateTransform
    internal open fun updateTransform(fromParent: Boolean): CoordsElement {
        if (transformations.isEmpty()) {
            transformationEvaluationError = null
            return this
        }
        if (baseElement == null) {
            baseElement = this
        }

        when (val result = transformedCoordinatesResult()) {
            is GMResult.Ok -> {
                actualCoords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = result.value,
                )
                transformationEvaluationError = null
            }
            is GMResult.Err -> {
                actualCoords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = DoubleArray(3) { Double.NaN },
                )
                transformationEvaluationError = result.error
            }
        }
        return this
    }

    internal fun transformedCoordinatesResult(): GMResult<
        DoubleArray,
        CoordinateTransformationError,
        > {
        val source = baseElement as? CoordsElement
            ?: return GMResult.Err(
                CoordinateTransformationError.InvalidBaseElement(
                    baseElementId = baseElement?.id,
                ),
            )
        var coordinates =
            if (source === this) {
                initialCoords.usrCoords.copyOf()
            } else {
                source.coords.usrCoords.copyOf()
            }
        for ((index, transformation) in transformations.withIndex()) {
            when (val result = transformation.applyResult(coordinates)) {
                is GMResult.Ok -> coordinates = result.value
                is GMResult.Err -> return GMResult.Err(
                    CoordinateTransformationError.Evaluation(
                        transformationIndex = index,
                        error = result.error,
                    ),
                )
            }
        }
        return GMResult.Ok(coordinates)
    }

    // JSXGraph: src/base/coordselement.js -> addTransform
    internal fun addTransform(
        element: GeometryElement,
        transformation: Transformation,
    ): CoordsElement = addTransform(element, listOf(transformation))

    internal fun addTransform(
        element: GeometryElement,
        newTransformations: Iterable<Transformation>,
    ): CoordsElement {
        if (transformations.isEmpty()) {
            baseElement = element
        }
        transformations += newTransformations
        return this
    }

    // JSXGraph: src/base/coordselement.js -> removeTransform
    internal fun removeTransform(
        transformation: Transformation,
    ): CoordsElement = removeTransform(listOf(transformation))

    internal fun removeTransform(
        removedTransformations: Iterable<Transformation>,
    ): CoordsElement {
        for (transformation in removedTransformations) {
            transformations.remove(transformation)
        }
        if (transformations.isEmpty()) {
            baseElement = null
        }
        return this
    }

    // JSXGraph: src/base/coordselement.js -> clearTransforms
    internal fun clearTransforms(): CoordsElement {
        transformations.clear()
        baseElement = null
        transformationEvaluationError = null
        return this
    }

    // JSXGraph: src/base/coordselement.js -> updateCoords
    internal fun updateCoords(fromParent: Boolean = false): CoordsElement {
        if (!needsUpdate) {
            return this
        }

        // Glider overrides update() and preserves this shared free-element path.
        updateConstraint()
        updateTransform(fromParent)
        return this
    }

    // JSXGraph 1.13.3: src/base/element.js -> getSnapSizes.
    internal fun getSnapSizes(): DoubleArray =
        doubleArrayOf(
            numericVisualProperty("snapsizex", 1.0),
            numericVisualProperty("snapsizey", 1.0),
        )

    // JSXGraph 1.13.3: src/base/coordselement.js -> snapToGrid;
    // src/base/element.js -> handleSnapToGrid.
    internal fun snapToGrid(force: Boolean = false): CoordsElement =
        if (force) {
            handleSnapToGrid(force = true, fromParent = false)
        } else {
            handleSnapToGrid()
        }

    internal open fun handleSnapToGrid(): CoordsElement =
        handleSnapToGrid(force = false, fromParent = false)

    internal fun handleSnapToGrid(
        force: Boolean,
        fromParent: Boolean = false,
    ): CoordsElement {
        if (
            (this is Point && isFixed) ||
            booleanVisualProperty("fixed", false)
        ) {
            return this
        }

        val attractToGrid =
            booleanVisualProperty("attracttogrid", false)
        if (
            !force &&
            !attractToGrid &&
            !booleanVisualProperty("snaptogrid", false)
        ) {
            return this
        }
        val snapSizes = getSnapSizes()
        val snapSizeX = snapSizes[0]
        val snapSizeY = snapSizes[1]
        if (snapSizeX <= 0.0 || snapSizeY <= 0.0) {
            return this
        }

        var horizontal = round(X() / snapSizeX) * snapSizeX
        var vertical = round(Y() / snapSizeY) * snapSizeY
        if (attractToGrid) {
            val rounded = Coords(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(horizontal, vertical),
                board = board,
                emitter = false,
            )
            val coordinateType =
                if (
                    stringVisualProperty(
                        "attractorunit",
                        DEFAULT_ATTRACTOR_UNIT,
                    ) == SCREEN_ATTRACTOR_UNIT
                ) {
                    Const.COORDS_BY_SCREEN
                } else {
                    Const.COORDS_BY_USER
                }
            if (
                rounded.distance(coordinateType, coords) >=
                numericVisualProperty("attractordistance", 0.0)
            ) {
                return this
            }
        }

        if (!fromParent) {
            val boundingBox = board.getBoundingBox()
            val minimumX = minOf(boundingBox[0], boundingBox[2])
            val maximumX = maxOf(boundingBox[0], boundingBox[2])
            val minimumY = minOf(boundingBox[1], boundingBox[3])
            val maximumY = maxOf(boundingBox[1], boundingBox[3])
            horizontal = keepRoundedCoordinateInside(
                value = horizontal,
                minimum = minimumX,
                maximum = maximumX,
                step = snapSizeX,
            )
            vertical = keepRoundedCoordinateInside(
                value = vertical,
                minimum = minimumY,
                maximum = maximumY,
                step = snapSizeY,
            )
        }
        coords.setCoordinates(
            Const.COORDS_BY_USER,
            doubleArrayOf(horizontal, vertical),
        )
        return this
    }

    // JSXGraph 1.13.3: src/base/coordselement.js ->
    // handleSnapToPoints / snapToPoints.
    internal fun snapToPoints(force: Boolean = false): CoordsElement =
        if (force) {
            handleSnapToPoints(force = true)
        } else {
            handleSnapToPoints()
        }

    internal open fun handleSnapToPoints(): CoordsElement =
        handleSnapToPoints(force = false)

    internal fun handleSnapToPoints(
        force: Boolean,
    ): CoordsElement {
        if (
            !force &&
            !booleanVisualProperty("snaptopoints", false)
        ) {
            return this
        }
        val ignored = ignoredSnapElements()
        val coordinateType =
            if (
                stringVisualProperty(
                    "attractorunit",
                    DEFAULT_ATTRACTOR_UNIT,
                ) == SCREEN_ATTRACTOR_UNIT
            ) {
                Const.COORDS_BY_SCREEN
            } else {
                Const.COORDS_BY_USER
            }
        val attractorDistance =
            numericVisualProperty("attractordistance", 0.0)
        var closestDistance = Double.POSITIVE_INFINITY
        var closestCoordinates: DoubleArray? = null
        for (candidate in board.objectsList) {
            if (
                candidate === this ||
                candidate !is Point ||
                candidate in ignored ||
                candidate.getAttribute("visible") == false
            ) {
                continue
            }
            val projected = Coords(
                method = Const.COORDS_BY_USER,
                coordinates = Geometry.projectPointToPoint(
                    point = coords.usrCoords,
                    destination = candidate.coords.usrCoords,
                ),
                board = board,
                emitter = false,
            )
            val distance = projected.distance(coordinateType, coords)
            if (
                distance < attractorDistance &&
                distance < closestDistance
            ) {
                closestDistance = distance
                closestCoordinates = projected.usrCoords
            }
        }
        closestCoordinates?.let {
            coords.setCoordinates(Const.COORDS_BY_USER, it)
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/coordselement.js -> handleAttractors.
    internal open fun handleAttractors(): CoordsElement {
        val attractorDistance =
            numericVisualProperty("attractordistance", 0.0)
        if (attractorDistance == 0.0) {
            return this
        }
        val coordinateType =
            if (
                stringVisualProperty(
                    "attractorunit",
                    DEFAULT_ATTRACTOR_UNIT,
                ) == SCREEN_ATTRACTOR_UNIT
            ) {
                Const.COORDS_BY_SCREEN
            } else {
                Const.COORDS_BY_USER
            }
        val snatchDistance =
            numericVisualProperty("snatchdistance", 0.0)
        for (attractor in attractorElements()) {
            if (attractor === this) {
                continue
            }
            val projection = projectToAttractor(attractor) ?: continue
            val projected = Coords(
                method = Const.COORDS_BY_USER,
                coordinates = projection,
                board = board,
                emitter = false,
            )
            val distance = projected.distance(coordinateType, coords)
            if (distance < attractorDistance) {
                coords.setCoordinates(
                    Const.COORDS_BY_USER,
                    projected.usrCoords,
                )
                bindToAttractor(attractor)
                break
            }
            if (
                distance >= snatchDistance &&
                slideObject === attractor &&
                this !is Glider
            ) {
                popSlideObject()
            }
        }
        return this
    }

    // JSXGraph: src/base/coordselement.js -> setPositionDirectly
    internal fun setPositionDirectly(
        method: Int,
        coordinates: DoubleArray,
    ): CoordsElement {
        setPositionDirectlyResult(method, coordinates)
        return this
    }

    internal fun setPositionDirectlyResult(
        method: Int,
        coordinates: DoubleArray,
    ): GMResult<CoordsElement, CoordinateTransformationError> {
        coords.setCoordinates(method, coordinates)
        handleSnapToGrid()
        handleSnapToPoints()
        handleAttractors()

        actualCoords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = coords.usrCoords,
        )

        var preimageError: CoordinateTransformationError? = null
        // JSXGraph: src/base/coordselement.js -> setPositionDirectly
        // Determine the preimage before all persistent transformations.
        if (transformations.isNotEmpty()) {
            var composite = Mat.identity(3)
            for (transformation in transformations) {
                composite = Mat.matMatMult(
                    transformation.matrix,
                    composite,
                )
            }
            val inverse = Mat.inverse(composite)
            if (inverse.isEmpty()) {
                preimageError =
                    CoordinateTransformationError
                        .NonInvertibleCompositeMatrix(
                            transformationCount = transformations.size,
                        )
                transformationEvaluationError = preimageError
            } else {
                val preimage = Mat.matVecMult(
                    inverse,
                    coords.usrCoords,
                )
                initialCoords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = preimage,
                )
                if (elementClass != Const.OBJECT_CLASS_POINT) {
                    coords.setCoordinates(
                        coordType = Const.COORDS_BY_USER,
                        coordinates = preimage,
                    )
                }
                transformationEvaluationError = null
            }
        }
        prepareUpdate()
        // JSXGraph's omitted update argument means a direct manipulation,
        // while Board.update passes true for parent-driven updates.
        update(fromParent = false)
        return if (preimageError == null) {
            GMResult.Ok(this)
        } else {
            transformationEvaluationError = preimageError
            GMResult.Err(preimageError)
        }
    }

    // JSXGraph: src/base/coordselement.js -> setPosition
    override fun setPosition(
        method: Int,
        coordinates: DoubleArray,
    ): CoordsElement = setPositionDirectly(method, coordinates)

    // JSXGraph 1.13.3: src/base/coordselement.js ->
    // setPositionByTransform.
    internal fun setPositionByTransform(
        method: Int,
        translation: DoubleArray,
    ): CoordsElement {
        setPositionByTransformResult(method, translation)
        return this
    }

    internal fun setPositionByTransformResult(
        method: Int,
        translation: DoubleArray,
    ): GMResult<CoordsElement, CoordinateTransformationError> {
        val translated = Coords(
            method = method,
            coordinates = translation,
            board = board,
            emitter = false,
        )
        val transformation = when (
            val result = Transformation.create(
                type = "translate",
                parameters = translated.usrCoords.copyOfRange(1, 3),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                CoordinateTransformationError.Evaluation(
                    transformationIndex = transformations.size,
                    error = result.error,
                ),
            )
        }
        val last = transformations.lastOrNull()
        if (last != null && last.isNumericMatrix) {
            when (val result = last.melt(transformation)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    CoordinateTransformationError.Evaluation(
                        transformationIndex =
                            transformations.lastIndex,
                        error = result.error,
                    ),
                )
            }
        } else {
            addTransform(this, transformation)
        }
        prepareUpdate()
        update()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/coordselement.js -> popSlideObject.
    internal fun popSlideObject(): CoordsElement {
        val previous = slideObjects.removeLastOrNull() ?: return this
        previous.removeChild(this)
        slideObject = slideObjects.lastOrNull()
        if (slideObject == null) {
            type = originalType
            elType = when {
                type == Const.OBJECT_TYPE_POINT -> "point"
                elementClass == Const.OBJECT_CLASS_TEXT -> "text"
                type == Const.OBJECT_TYPE_IMAGE -> "image"
                type == Const.OBJECT_TYPE_FOREIGNOBJECT ->
                    "foreignobject"
                else -> elType
            }
            onPolygon = false
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/coordselement.js -> moveTo.
    internal fun moveTo(
        where: DoubleArray,
        timeMilliseconds: Double? = null,
        options: CoordsElementMoveOptions =
            CoordsElementMoveOptions(),
    ): GMResult<CoordsElement, CoordsElementAnimationError> {
        if (where.size !in setOf(2, 3)) {
            return GMResult.Err(
                CoordsElementAnimationError.InvalidCoordinateCount(
                    where.size,
                ),
            )
        }
        val duration = timeMilliseconds ?: 0.0
        if (!duration.isFinite() || duration < 0.0) {
            return GMResult.Err(
                CoordsElementAnimationError.InvalidDuration(duration),
            )
        }
        val target = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = where,
            board = board,
            emitter = false,
        ).usrCoords
        if (
            duration == 0.0 ||
            abs(target[0] - coords.usrCoords[0]) > Mat.eps
        ) {
            return setAnimatedPosition(target, options.callback)
        }

        val deltaX = target[1] - X()
        val deltaY = target[2] - Y()
        if (
            options.callback == null &&
            abs(deltaX) < Mat.eps &&
            abs(deltaY) < Mat.eps
        ) {
            return GMResult.Ok(this)
        }
        val steps = maxOf(
            1,
            ceil(duration / board.animationDelay).toInt(),
        )
        val path = List(steps + 1) { index ->
            val progress = animationProgress(
                step = index,
                steps = steps,
                effect = options.effect,
            )
            doubleArrayOf(
                target[0],
                X() + deltaX * progress,
                Y() + deltaY * progress,
            )
        }
        return scheduleMovement(path, duration, options.callback)
    }

    // JSXGraph 1.13.3: src/base/coordselement.js -> moveAlong.
    internal fun moveAlong(
        path: List<DoubleArray>,
        timeMilliseconds: Double,
        options: CoordsElementMoveOptions =
            CoordsElementMoveOptions(),
    ): GMResult<CoordsElement, CoordsElementAnimationError> {
        if (path.isEmpty()) {
            return GMResult.Err(CoordsElementAnimationError.EmptyPath)
        }
        if (!timeMilliseconds.isFinite() || timeMilliseconds < 0.0) {
            return GMResult.Err(
                CoordsElementAnimationError.InvalidDuration(
                    timeMilliseconds,
                ),
            )
        }
        for (coordinates in path) {
            if (coordinates.size !in setOf(2, 3)) {
                return GMResult.Err(
                    CoordsElementAnimationError.InvalidCoordinateCount(
                        coordinates.size,
                    ),
                )
            }
        }
        if (timeMilliseconds == 0.0) {
            return setAnimatedPosition(path.last(), options.callback)
        }
        val steps = maxOf(
            1,
            ceil(timeMilliseconds / board.animationDelay).toInt(),
        )
        val normalizedPath = path.map {
            Coords(
                method = Const.COORDS_BY_USER,
                coordinates = it,
                board = board,
                emitter = false,
            ).usrCoords
        }
        val interpolated =
            if (options.interpolate && normalizedPath.size > 1) {
                polynomialPath(normalizedPath, steps)
            } else {
                linearPath(normalizedPath, steps)
            }
        return scheduleMovement(
            interpolated,
            timeMilliseconds,
            options.callback,
        )
    }

    // JSXGraph 1.13.3: src/base/coordselement.js -> visit.
    internal fun visit(
        where: DoubleArray,
        timeMilliseconds: Double,
        options: CoordsElementMoveOptions =
            CoordsElementMoveOptions(),
    ): GMResult<CoordsElement, CoordsElementAnimationError> {
        if (where.size !in setOf(2, 3)) {
            return GMResult.Err(
                CoordsElementAnimationError.InvalidCoordinateCount(
                    where.size,
                ),
            )
        }
        if (!timeMilliseconds.isFinite() || timeMilliseconds < 0.0) {
            return GMResult.Err(
                CoordsElementAnimationError.InvalidDuration(
                    timeMilliseconds,
                ),
            )
        }
        if (options.repeat < 1) {
            return GMResult.Err(
                CoordsElementAnimationError.InvalidRepeatCount(
                    options.repeat,
                ),
            )
        }
        val start = coords.usrCoords.copyOf()
        val target = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = where,
            board = board,
            emitter = false,
        ).usrCoords
        if (timeMilliseconds == 0.0) {
            return setAnimatedPosition(start, options.callback)
        }
        val steps = maxOf(
            2,
            ceil(
                timeMilliseconds /
                    (board.animationDelay * options.repeat),
            ).toInt(),
        )
        val oneRound = List(steps + 1) { index ->
            val progress =
                if (index <= steps / 2.0) {
                    2.0 * index / steps
                } else {
                    2.0 * (steps - index) / steps
                }
            val eased = animationProgress(
                progress = progress,
                effect = options.effect,
            )
            doubleArrayOf(
                target[0],
                start[1] + (target[1] - start[1]) * eased,
                start[2] + (target[2] - start[2]) * eased,
            )
        }
        val path = buildList {
            repeat(options.repeat) {
                addAll(oneRound)
            }
        }
        return scheduleMovement(
            path,
            timeMilliseconds,
            options.callback,
        )
    }

    // JSXGraph 1.13.3: src/base/coordselement.js ->
    // startAnimation / stopAnimation / _anim.
    internal fun startAnimation(
        direction: Double,
        stepCount: Int,
        delayMilliseconds: Double = 250.0,
        maxRounds: Int = -1,
    ): GMResult<CoordsElement, CoordsElementAnimationError> {
        if (type != Const.OBJECT_TYPE_GLIDER || slideObject == null) {
            return GMResult.Err(
                CoordsElementAnimationError.GliderRequired,
            )
        }
        if (stepCount < 1) {
            return GMResult.Err(
                CoordsElementAnimationError.InvalidStepCount(stepCount),
            )
        }
        if (
            !delayMilliseconds.isFinite() ||
            delayMilliseconds < 0.0
        ) {
            return GMResult.Err(
                CoordsElementAnimationError.InvalidDuration(
                    delayMilliseconds,
                ),
            )
        }
        if (maxRounds == 0 || gliderAnimationRunning) {
            return GMResult.Ok(this)
        }
        if (
            maxRounds < 0 &&
            board.animationScheduler.runsSynchronously
        ) {
            return GMResult.Err(
                CoordsElementAnimationError
                    .UnboundedSynchronousAnimation,
            )
        }
        gliderAnimationRunning = true
        gliderAnimationStep = 0
        gliderAnimationRounds = 0
        movementError = null
        board.animationScheduler.schedule(delayMilliseconds) {
            advanceGliderAnimation(
                direction = direction,
                stepCount = stepCount,
                maxRounds = maxRounds,
            )
        }
        val error = movementError
        return if (error == null) {
            GMResult.Ok(this)
        } else {
            GMResult.Err(error)
        }
    }

    internal fun stopAnimation(): CoordsElement {
        gliderAnimationRunning = false
        return this
    }

    // JSXGraph 1.13.3: src/base/coordselement.js ->
    // updateRendererGeneric. The renderer callback is supplied by the
    // platform-neutral scene bridge instead of a DOM renderer method name.
    internal fun updateRendererGeneric(
        rendererMethod: (CoordsElement) -> Unit,
    ): CoordsElement {
        if (!needsUpdate) {
            return this
        }
        var visible =
            visualPropertiesCalculated["visible"] as? Boolean ?: false
        if (visible && !isReal) {
            updateVisibility(false)
            visible =
                visualPropertiesCalculated["visible"] as? Boolean ?: false
        }
        if (visible) {
            rendererMethod(this)
        }
        val label = elementLabel
        if (
            hasLabel &&
            visible &&
            label != null &&
            label.visualPropertiesCalculated["visible"] == true &&
            isReal
        ) {
            label.update()
            label.updateRenderer()
        }
        setDisplayRendNode()
        needsUpdate = false
        return this
    }

    // Promise adapters in JavaScript become callback-preserving result
    // adapters in common Kotlin.
    // JSXGraph 1.13.3: src/base/coordselement.js -> moveAlongES6.
    internal fun moveAlongES6(
        path: List<DoubleArray>,
        timeMilliseconds: Double,
        options: CoordsElementMoveOptions =
            CoordsElementMoveOptions(),
    ): GMResult<CoordsElement, CoordsElementAnimationError> =
        moveAlong(path, timeMilliseconds, options)

    // JSXGraph 1.13.3: src/base/coordselement.js -> moveToES6.
    internal fun moveToES6(
        where: DoubleArray,
        timeMilliseconds: Double,
        options: CoordsElementMoveOptions =
            CoordsElementMoveOptions(),
    ): GMResult<CoordsElement, CoordsElementAnimationError> =
        moveTo(where, timeMilliseconds, options)

    // JSXGraph 1.13.3: src/base/coordselement.js -> visitES6.
    internal fun visitES6(
        where: DoubleArray,
        timeMilliseconds: Double,
        options: CoordsElementMoveOptions =
            CoordsElementMoveOptions(),
    ): GMResult<CoordsElement, CoordsElementAnimationError> =
        visit(where, timeMilliseconds, options)

    // JSXGraph 1.13.3: src/base/coordselement.js ->
    // getTextAnchor / getLabelAnchor / getParents.
    internal override fun getTextAnchor(): Coords = coords

    internal override fun getLabelAnchor(): Coords = coords

    internal fun parentData(): List<Any> {
        val currentSlideObject = slideObject
        return when {
            type == Const.OBJECT_TYPE_GLIDER &&
                currentSlideObject != null ->
                listOf(X(), Y(), currentSlideObject.id)
            parents.isNotEmpty() -> parents.toList()
            else -> listOf(Z(), X(), Y())
        }
    }

    private fun scheduleMovement(
        path: List<DoubleArray>,
        timeMilliseconds: Double,
        callback: (() -> Unit)?,
    ): GMResult<CoordsElement, CoordsElementAnimationError> {
        movePath.clear()
        movePath.addAll(path.map(DoubleArray::copyOf))
        moveCallback = callback
        movementError = null
        if (movementScheduled) {
            return GMResult.Ok(this)
        }
        movementScheduled = true
        val stepTime = timeMilliseconds / maxOf(path.size, 1)
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
            when (
                val result = setPositionDirectlyResult(
                    Const.COORDS_BY_USER,
                    next,
                )
            ) {
                is GMResult.Ok -> board.update(this)
                is GMResult.Err -> {
                    movementError =
                        CoordsElementAnimationError.Position(result.error)
                    movePath.clear()
                }
            }
        }
        if (movePath.isNotEmpty()) {
            return true
        }
        movementScheduled = false
        runAnimationCallback()
        return false
    }

    private fun setAnimatedPosition(
        coordinates: DoubleArray,
        callback: (() -> Unit)?,
    ): GMResult<CoordsElement, CoordsElementAnimationError> {
        return when (
            val result = setPositionDirectlyResult(
                Const.COORDS_BY_USER,
                coordinates,
            )
        ) {
            is GMResult.Err -> GMResult.Err(
                CoordsElementAnimationError.Position(result.error),
            )
            is GMResult.Ok -> {
                board.update(this)
                moveCallback = callback
                movementError = null
                runAnimationCallback()
                val error = movementError
                if (error == null) {
                    GMResult.Ok(this)
                } else {
                    GMResult.Err(error)
                }
            }
        }
    }

    private fun runAnimationCallback() {
        val callback = moveCallback
        moveCallback = null
        if (callback != null) {
            try {
                callback()
            } catch (exception: Exception) {
                movementError = CoordsElementAnimationError.Callback(
                    exception.message
                        ?: exception::class.simpleName.orEmpty(),
                )
            }
        }
    }

    private fun polynomialPath(
        path: List<DoubleArray>,
        steps: Int,
    ): List<DoubleArray> {
        val points = path.map {
            CoordsElement(
                board = board,
                coordinates = it,
            )
        }
        val neville =
            com.swithun.jsxgraph.core.math.Numerics.Neville(points)
        return List(steps + 1) { index ->
            val parameter =
                index.toDouble() / steps * neville.end()
            doubleArrayOf(
                1.0,
                neville.x(parameter),
                neville.y(parameter, suspendedUpdate = true),
            )
        }
    }

    private fun linearPath(
        path: List<DoubleArray>,
        steps: Int,
    ): List<DoubleArray> {
        if (path.size == 1) {
            return listOf(path[0].copyOf())
        }
        return List(steps + 1) { index ->
            val location =
                index.toDouble() / steps * path.lastIndex
            val segment = minOf(location.toInt(), path.lastIndex - 1)
            val fraction = location - segment
            doubleArrayOf(
                1.0,
                (1.0 - fraction) * path[segment][1] +
                    fraction * path[segment + 1][1],
                (1.0 - fraction) * path[segment][2] +
                    fraction * path[segment + 1][2],
            )
        }
    }

    private fun animationProgress(
        step: Int,
        steps: Int,
        effect: String?,
    ): Double =
        animationProgress(
            progress = step.toDouble() / steps,
            effect = effect,
        )

    private fun animationProgress(
        progress: Double,
        effect: String?,
    ): Double =
        when (effect) {
            "<>" -> sin(progress * PI * 0.5).pow(2)
            "<" -> progress.pow(3)
            ">" -> 1.0 - (1.0 - progress).pow(3)
            "==", "--", null -> progress
            else -> progress
        }

    private fun advanceGliderAnimation(
        direction: Double,
        stepCount: Int,
        maxRounds: Int,
    ): Boolean {
        if (!gliderAnimationRunning) {
            return false
        }
        gliderAnimationStep += 1
        if (gliderAnimationStep > stepCount) {
            gliderAnimationStep = 0
            gliderAnimationRounds += 1
            if (
                maxRounds > 0 &&
                gliderAnimationRounds >= maxRounds
            ) {
                gliderAnimationRounds = 0
                stopAnimation()
                return false
            }
        }
        val slide = slideObject
        val next = when (slide) {
            is Line -> {
                val first =
                    if (direction > 0.0) slide.point1 else slide.point2
                val second =
                    if (direction > 0.0) slide.point2 else slide.point1
                val fraction =
                    gliderAnimationStep.toDouble() / stepCount
                doubleArrayOf(
                    1.0,
                    first.X() + (second.X() - first.X()) * fraction,
                    first.Y() + (second.Y() - first.Y()) * fraction,
                )
            }
            is Curve -> {
                val minimum = slide.minX()
                val maximum = slide.maxX()
                val fraction =
                    gliderAnimationStep.toDouble() / stepCount
                val parameter =
                    if (direction > 0.0) {
                        minimum + (maximum - minimum) * fraction
                    } else {
                        maximum - (maximum - minimum) * fraction
                    }
                position = parameter
                doubleArrayOf(
                    1.0,
                    slide.X(parameter),
                    slide.Y(parameter),
                )
            }
            is Circle -> {
                val fraction =
                    gliderAnimationStep.toDouble() / stepCount
                val angle =
                    if (direction < 0.0) {
                        2.0 * PI * fraction
                    } else {
                        2.0 * PI * (1.0 - fraction)
                    }
                doubleArrayOf(
                    1.0,
                    slide.center.X() + slide.Radius() * cos(angle),
                    slide.center.Y() + slide.Radius() * sin(angle),
                )
            }
            else -> {
                movementError =
                    CoordsElementAnimationError.UnsupportedSlideObject(
                        slide?.elType.orEmpty(),
                    )
                stopAnimation()
                return false
            }
        }
        coords.setCoordinates(Const.COORDS_BY_USER, next)
        board.update(this)
        return gliderAnimationRunning
    }

    private fun bindToAttractor(
        attractor: GeometryElement,
    ) {
        if (slideObject === attractor) {
            return
        }
        if (this is Glider) {
            slideElement = attractor
        }
        slideObject = attractor
        if (attractor !in slideObjects) {
            slideObjects += attractor
        }
        addParents(listOf(attractor))
        attractor.addChild(this)
        type = Const.OBJECT_TYPE_GLIDER
        elType = "glider"
        isDraggable = true
    }

    private fun projectToAttractor(
        attractor: GeometryElement,
    ): DoubleArray? =
        when (attractor) {
            is Point -> attractor.coords.usrCoords.copyOf()
            is Line -> {
                val projection = Geometry.projectCoordsToSegment(
                    point = coords.usrCoords,
                    first = attractor.point1.coords.usrCoords,
                    second = attractor.point2.coords.usrCoords,
                )
                when {
                    !attractor.straightFirst &&
                        projection.parameter < 0.0 ->
                        attractor.point1.coords.usrCoords.copyOf()
                    !attractor.straightLast &&
                        projection.parameter > 1.0 ->
                        attractor.point2.coords.usrCoords.copyOf()
                    else -> projection.point
                }
            }
            is Circle -> Geometry.projectPointToCircle(
                point = coords.usrCoords,
                center = attractor.center.coords.usrCoords,
                radius = attractor.Radius(),
            )
            is Curve -> projectToCurve(attractor)
            is Polygon -> when (
                val result = Geometry.projectCoordsToPolygon(
                    point = coords.usrCoords,
                    vertices = attractor.vertices.map {
                        it.coords.usrCoords
                    },
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> null
            }
            else -> null
        }

    private fun projectToCurve(
        curve: Curve,
    ): DoubleArray? {
        val projection =
            if (curve.curveType == "plot") {
                val rawX = curve.dataX
                val rawY = curve.dataY
                val points =
                    if (rawX != null) {
                        List(minOf(curve.numberPoints, rawX.size)) {
                                index,
                            ->
                            doubleArrayOf(
                                1.0,
                                rawX[index],
                                rawY?.getOrNull(index) ?: Double.NaN,
                            )
                        }
                    } else {
                        curve.points
                            .take(curve.numberPoints)
                            .map { it.usrCoords.copyOf() }
                    }
                Geometry.projectCoordsToCurve(
                    point = coords.usrCoords,
                    curve = DiscreteCurve2D(
                        points = points,
                        bezierDegree = curve.bezierDegree,
                    ),
                )
            } else {
                Geometry.projectCoordsToCurve(
                    horizontal = X(),
                    vertical = Y(),
                    initialParameter = position ?: 0.0,
                    continuousCurve = ContinuousCurve2D(
                        curve = ParametricCurve2D(
                            x = curve::X,
                            y = curve::Y,
                        ),
                        minimumParameter = curve.minX(),
                        maximumParameter = curve.maxX(),
                        type = ContinuousCurveType.PARAMETER,
                    ),
                )
            }
        return when (projection) {
            is GMResult.Ok -> {
                position = projection.value.parameter
                projection.value.point
            }
            is GMResult.Err -> null
        }
    }

    private fun ignoredSnapElements(): Set<GeometryElement> {
        val raw = getAttribute("ignoredsnaptopoints") as? List<*>
            ?: return emptySet()
        return raw.mapNotNull {
            when (it) {
                is GeometryElement -> board.select(it)
                is String -> board.select(it)
                else -> null
            }
        }.toSet()
    }

    private fun attractorElements(): List<GeometryElement> {
        val raw = getAttribute("attractors") as? List<*>
            ?: return emptyList()
        return raw.mapNotNull {
            when (it) {
                is GeometryElement -> board.select(it)
                is String -> board.select(it)
                else -> null
            }
        }
    }

    private fun keepRoundedCoordinateInside(
        value: Double,
        minimum: Double,
        maximum: Double,
        step: Double,
    ): Double =
        when {
            value < minimum && value > minimum - step -> value + step
            value > maximum && value < maximum + step -> value - step
            else -> value
        }

    private fun booleanVisualProperty(
        key: String,
        default: Boolean,
    ): Boolean = getAttribute(key) as? Boolean ?: default

    private fun numericVisualProperty(
        key: String,
        default: Double,
    ): Double = (getAttribute(key) as? Number)?.toDouble() ?: default

    private fun stringVisualProperty(
        key: String,
        default: String,
    ): String = getAttribute(key) as? String ?: default

    private fun runtimeType(
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
            is JessieCodeRuntimeValue.CompositionReference ->
                "composition"
            is JessieCodeRuntimeValue.ElementReference -> "element"
        }

    private companion object {
        const val DEFAULT_ATTRACTOR_UNIT = "user"
        const val SCREEN_ATTRACTOR_UNIT = "screen"
    }
}
