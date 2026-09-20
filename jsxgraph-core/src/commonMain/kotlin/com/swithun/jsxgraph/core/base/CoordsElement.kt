/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/coordselement.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.NumericsPoint2D
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue

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

/**
 * Initial coordinate-access slice of JXG.CoordsElement.
 *
 * JessieCode string and function coordinate constraints and the persistent
 * 2D transformation lifecycle are present. Slider, Coords-object, glider,
 * animation, and renderer behavior remain in the untranslated element model.
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
    internal var Xjc: String? = null
    internal var Yjc: String? = null
    internal var coordinateEvaluationError: CoordinateConstraintError? = null
        private set
    internal var transformationEvaluationError:
        CoordinateTransformationError? = null
        private set

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

        /*
         * This is the free-element path. Frozen visual properties and glider
         * projection are added with their owner models.
         */
        updateConstraint()
        updateTransform(fromParent)
        return this
    }

    /*
     * These hooks preserve setPositionDirectly's upstream call order. Their
     * attribute-driven algorithms are translated with the visual-property and
     * attractor models.
     */
    internal open fun handleSnapToGrid(): CoordsElement = this

    internal open fun handleSnapToPoints(): CoordsElement = this

    internal open fun handleAttractors(): CoordsElement = this

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
        update()
        return if (preimageError == null) {
            GMResult.Ok(this)
        } else {
            transformationEvaluationError = preimageError
            GMResult.Err(preimageError)
        }
    }

    // JSXGraph: src/base/coordselement.js -> setPosition
    internal fun setPosition(
        method: Int,
        coordinates: DoubleArray,
    ): CoordsElement = setPositionDirectly(method, coordinates)

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
}
