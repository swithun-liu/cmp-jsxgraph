/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/foreignobject.js -> ForeignObject, update, updateSize,
 * setSize, createForeignObject;
 * src/base/coordselement.js -> CoordsElement.create
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.abs

internal sealed interface ForeignObjectError {
    data class InvalidCoordinateCount(
        val count: Int,
    ) : ForeignObjectError

    data class InvalidSizeCount(
        val count: Int,
    ) : ForeignObjectError

    data class CoordinateEvaluation(
        val error: CoordinateConstraintError,
    ) : ForeignObjectError

    data class CoordinateExpressionCompile(
        val coordinateIndex: Int,
        val error: JessieCodeExpressionCompileError,
    ) : ForeignObjectError

    data class SizeEvaluation(
        val sizeIndex: Int,
        val error: JessieCodeRuntimeError,
    ) : ForeignObjectError

    data class SizeExpressionCompile(
        val sizeIndex: Int,
        val error: JessieCodeExpressionCompileError,
    ) : ForeignObjectError

    data class SizeResult(
        val sizeIndex: Int,
        val actualType: String,
    ) : ForeignObjectError

    data class Registration(
        val error: BoardError,
    ) : ForeignObjectError
}

/**
 * Platform-independent JXG.ForeignObject state.
 *
 * HTML is retained as opaque content. Intrinsic DOM measurement and HTML
 * execution are renderer capabilities and are deliberately not emulated in
 * the pure Kotlin core.
 */
internal class ForeignObject private constructor(
    board: Board,
    coordinates: DoubleArray,
    internal val content: String,
    sizeTerms: List<JessieCodeCoordinateFunction>,
    initialSize: DoubleArray?,
    internal val evaluateOnlyOnce: Boolean,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    coordinateFunctions: List<JessieCodeCoordinateFunction>,
) : CoordsElement(
    board = board,
    coordinates = coordinates,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_FOREIGNOBJECT,
    elementClass = Const.OBJECT_CLASS_OTHER,
    needsRegularUpdate = needsRegularUpdate,
    coordinateFunctions = coordinateFunctions,
) {
    private var sizeTerms: List<JessieCodeCoordinateFunction> = sizeTerms

    internal var usrSize: DoubleArray? = initialSize?.copyOf()
        private set
    internal var size: DoubleArray? = initialSize?.let(::pixelSize)
        private set
    internal var sizeEvaluationError: ForeignObjectError? = null
        private set

    internal val usesUserSize: Boolean
        get() = sizeTerms.isNotEmpty()

    init {
        elType = FOREIGN_OBJECT_ELEMENT_TYPE
    }

    internal fun W(): Double? = usrSize?.get(0)

    internal fun H(): Double? = usrSize?.get(1)

    // JSXGraph 1.13.3: src/base/foreignobject.js -> setSize.
    internal fun setSize(
        newSizeTerms: List<JessieCodeCoordinateFunction>,
    ): GMResult<ForeignObject, ForeignObjectError> {
        if (newSizeTerms.size != 2) {
            return GMResult.Err(
                ForeignObjectError.InvalidSizeCount(newSizeTerms.size),
            )
        }
        sizeTerms = newSizeTerms
        addParentsFromJCFunctions(newSizeTerms)
        prepareUpdate()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/foreignobject.js -> update.
    override fun update(fromParent: Boolean): ForeignObject {
        if (!needsUpdate) {
            return this
        }
        updateCoords(fromParent)
        updateSize()
        return this
    }

    // JSXGraph 1.13.3: src/base/foreignobject.js -> updateRenderer.
    override fun updateRenderer(): ForeignObject {
        needsUpdate = false
        return this
    }

    // JSXGraph 1.13.3: src/base/foreignobject.js -> updateSize.
    private fun updateSize() {
        if (!usesUserSize) {
            return
        }
        when (val result = evaluateSize(sizeTerms)) {
            is GMResult.Ok -> {
                usrSize = result.value
                size = pixelSize(result.value)
                sizeEvaluationError = null
            }
            is GMResult.Err -> {
                usrSize = doubleArrayOf(Double.NaN, Double.NaN)
                size = doubleArrayOf(Double.NaN, Double.NaN)
                sizeEvaluationError = result.error
            }
        }
    }

    private fun pixelSize(userSize: DoubleArray): DoubleArray =
        doubleArrayOf(
            abs(userSize[0] * board.unitX),
            abs(userSize[1] * board.unitY),
        )

    internal companion object {
        private const val FOREIGN_OBJECT_ID_PREFIX = "Im"
        private const val FOREIGN_OBJECT_ELEMENT_TYPE = "foreignobject"

        internal fun create(
            board: Board,
            content: String,
            coordinates: DoubleArray,
            sizeTerms: List<JessieCodeCoordinateFunction>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = false,
            evaluateOnlyOnce: Boolean = false,
        ): GMResult<ForeignObject, ForeignObjectError> =
            createResolved(
                board = board,
                content = content,
                coordinates = coordinates,
                coordinateFunctions = emptyList(),
                sizeTerms = sizeTerms,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                evaluateOnlyOnce = evaluateOnlyOnce,
            )

        internal fun createConstrained(
            board: Board,
            content: String,
            coordinateFunctions: List<JessieCodeCoordinateFunction>,
            sizeTerms: List<JessieCodeCoordinateFunction>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = false,
            evaluateOnlyOnce: Boolean = false,
        ): GMResult<ForeignObject, ForeignObjectError> {
            if (
                coordinateFunctions.size !in 1..3 ||
                (
                    coordinateFunctions.size == 1 &&
                        !coordinateFunctions[0].returnsCoordinateArray
                    )
            ) {
                return GMResult.Err(
                    ForeignObjectError.InvalidCoordinateCount(
                        coordinateFunctions.size,
                    ),
                )
            }
            val placeholder =
                if (coordinateFunctions.size <= 2) {
                    doubleArrayOf(0.0, 0.0)
                } else {
                    doubleArrayOf(1.0, 0.0, 0.0)
                }
            return createResolved(
                board = board,
                content = content,
                coordinates = placeholder,
                coordinateFunctions = coordinateFunctions,
                sizeTerms = sizeTerms,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                evaluateOnlyOnce = evaluateOnlyOnce,
            )
        }

        private fun createResolved(
            board: Board,
            content: String,
            coordinates: DoubleArray,
            coordinateFunctions: List<JessieCodeCoordinateFunction>,
            sizeTerms: List<JessieCodeCoordinateFunction>,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
            evaluateOnlyOnce: Boolean,
        ): GMResult<ForeignObject, ForeignObjectError> {
            if (coordinates.size !in 2..3) {
                return GMResult.Err(
                    ForeignObjectError.InvalidCoordinateCount(
                        coordinates.size,
                    ),
                )
            }
            if (sizeTerms.size != 0 && sizeTerms.size != 2) {
                return GMResult.Err(
                    ForeignObjectError.InvalidSizeCount(sizeTerms.size),
                )
            }
            val initialSize =
                if (sizeTerms.isEmpty()) {
                    null
                } else {
                    when (val result = evaluateSize(sizeTerms)) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                }
            val foreignObject = ForeignObject(
                board = board,
                coordinates = coordinates,
                content = content,
                sizeTerms = sizeTerms,
                initialSize = initialSize,
                evaluateOnlyOnce = evaluateOnlyOnce,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                coordinateFunctions = coordinateFunctions,
            )
            if (coordinateFunctions.isEmpty()) {
                foreignObject.baseElement = foreignObject
            } else {
                val initialCoordinates = when (
                    val result = foreignObject.coordinateConstraintResult()
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        ForeignObjectError.CoordinateEvaluation(result.error),
                    )
                }
                if (initialCoordinates.size !in 2..3) {
                    return GMResult.Err(
                        ForeignObjectError.InvalidCoordinateCount(
                            initialCoordinates.size,
                        ),
                    )
                }
                foreignObject.applyCoordinateConstraint(initialCoordinates)
            }
            when (
                val registration = board.setId(
                    foreignObject,
                    FOREIGN_OBJECT_ID_PREFIX,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    ForeignObjectError.Registration(registration.error),
                )
            }
            foreignObject.addParentsFromJCFunctions(
                coordinateFunctions + sizeTerms,
            )
            foreignObject.update()
            return GMResult.Ok(foreignObject)
        }

        private fun evaluateSize(
            terms: List<JessieCodeCoordinateFunction>,
        ): GMResult<DoubleArray, ForeignObjectError> {
            if (terms.size != 2) {
                return GMResult.Err(
                    ForeignObjectError.InvalidSizeCount(terms.size),
                )
            }
            val values = DoubleArray(2)
            for ((index, term) in terms.withIndex()) {
                when (val result = term.evaluate()) {
                    is GMResult.Err -> return GMResult.Err(
                        ForeignObjectError.SizeEvaluation(
                            sizeIndex = index,
                            error = result.error,
                        ),
                    )
                    is GMResult.Ok -> {
                        val value = result.value
                        if (value !is JessieCodeRuntimeValue.NumberValue) {
                            return GMResult.Err(
                                ForeignObjectError.SizeResult(
                                    sizeIndex = index,
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
}
