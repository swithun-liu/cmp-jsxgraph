/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/coordselement.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionFunction
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
}

/**
 * Initial coordinate-access slice of JXG.CoordsElement.
 *
 * JessieCode coordinate constraints and their update lifecycle are present.
 * Function, slider, single-array, glider, persistent transformation,
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
    coordinateFunctions: List<JessieCodeExpressionFunction> = emptyList(),
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type = type,
    elementClass = elementClass,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal var coordinateFunctions:
        List<JessieCodeExpressionFunction> = coordinateFunctions
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

    init {
        isDraggable = coordinateFunctions.isEmpty()
    }

    internal val isReal: Boolean
        get() = coords.isReal()

    // JSXGraph: src/base/coordselement.js -> X.
    internal fun X(): Double = coords.usrCoords[1]

    // JSXGraph: src/base/coordselement.js -> Y.
    internal fun Y(): Double = coords.usrCoords[2]

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
        functions: List<JessieCodeExpressionFunction>,
    ): GMResult<DoubleArray, CoordinateConstraintError> {
        if (functions.isEmpty()) {
            return GMResult.Ok(coords.usrCoords.copyOf())
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
        functions: List<JessieCodeExpressionFunction>,
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
    internal open fun updateTransform(fromParent: Boolean): CoordsElement = this

    // JSXGraph: src/base/coordselement.js -> updateCoords
    internal fun updateCoords(fromParent: Boolean = false): CoordsElement {
        if (!needsUpdate) {
            return this
        }

        /*
         * This is the free-element path. Frozen visual properties, glider
         * projection, and transformations are added with their owner models.
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
        coords.setCoordinates(method, coordinates)
        handleSnapToGrid()
        handleSnapToPoints()
        handleAttractors()

        actualCoords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = coords.usrCoords,
        )

        /*
         * relativeCoords and transformation preimages are intentionally not
         * represented until their owner models are translated.
         */
        prepareUpdate()
        update()
        return this
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
            is JessieCodeRuntimeValue.ElementReference -> "element"
        }
}
