/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/composition.js -> createIntegral
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.utils.JsNumberFormat

internal sealed interface IntegralBoundary {
    data class Fixed(
        val value: Double,
    ) : IntegralBoundary

    data class Dynamic(
        val term: JessieCodeCoordinateFunction,
    ) : IntegralBoundary
}

internal data class IntegralElementAttributes(
    val id: String = "",
    val name: String? = "",
    val needsRegularUpdate: Boolean = true,
    val fixed: Boolean = false,
)

internal data class IntegralLabelAttributes(
    val id: String = "",
    val name: String? = "",
    val needsRegularUpdate: Boolean = true,
    val digits: Int = 4,
    val offset: DoubleArray = doubleArrayOf(10.0, 10.0),
)

internal data class IntegralAttributes(
    val id: String = "",
    val name: String? = "",
    val needsRegularUpdate: Boolean = true,
    val axis: String = "x",
    val withLabel: Boolean = true,
    val curveLeft: IntegralElementAttributes =
        IntegralElementAttributes(),
    val baseLeft: IntegralElementAttributes =
        IntegralElementAttributes(name = ""),
    val curveRight: IntegralElementAttributes =
        IntegralElementAttributes(),
    val baseRight: IntegralElementAttributes =
        IntegralElementAttributes(name = ""),
    val label: IntegralLabelAttributes = IntegralLabelAttributes(),
)

internal sealed interface IntegralError {
    data object SourceBoardMismatch : IntegralError

    data class SourceNotRegistered(
        val id: String,
    ) : IntegralError

    data class DuplicateElementId(
        val id: String,
    ) : IntegralError

    data class BoundaryEvaluation(
        val role: String,
        val error: JessieCodeRuntimeError,
    ) : IntegralError

    data class BoundaryNonNumeric(
        val role: String,
        val actualType: String,
    ) : IntegralError

    data class GliderFactory(
        val role: String,
        val error: GliderError,
    ) : IntegralError

    data class PointFactory(
        val role: String,
        val error: PointError,
    ) : IntegralError

    data class CurveFactory(
        val error: CurveError,
    ) : IntegralError

    data class TextFactory(
        val error: TextError,
    ) : IntegralError
}

internal object Integral {
    // JSXGraph 1.13.3:
    // src/element/composition.js -> createIntegral.
    internal fun create(
        board: Board,
        interval: Pair<IntegralBoundary, IntegralBoundary>,
        source: Curve,
        attributes: IntegralAttributes = IntegralAttributes(),
    ): GMResult<Curve, IntegralError> {
        if (source.board !== board) {
            return GMResult.Err(IntegralError.SourceBoardMismatch)
        }
        if (board.elementById(source.id) !== source) {
            return GMResult.Err(
                IntegralError.SourceNotRegistered(source.id),
            )
        }
        requestedIds(attributes)
            .filter(String::isNotEmpty)
            .groupingBy { it }
            .eachCount()
            .entries
            .firstOrNull { (id, count) ->
                count > 1 || board.elementById(id) != null
            }
            ?.let { return GMResult.Err(IntegralError.DuplicateElementId(it.key)) }

        val start = when (
            val result = evaluateBoundary("curveLeft", interval.first)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val end = when (
            val result = evaluateBoundary("curveRight", interval.second)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val created = mutableListOf<GeometryElement>()

        val curveLeft = when (
            val result = createCurvePoint(
                board = board,
                source = source,
                boundary = interval.first,
                initialValue = start,
                role = "curveLeft",
                attributes = attributes.curveLeft,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        created += curveLeft

        val baseLeft = when (
            val result = createBasePoint(
                board = board,
                curvePoint = curveLeft,
                axis = attributes.axis,
                role = "baseLeft",
                attributes = attributes.baseLeft,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollback(board, created)
                return result
            }
        }
        created += baseLeft

        val curveRight = when (
            val result = createCurvePoint(
                board = board,
                source = source,
                boundary = interval.second,
                initialValue = end,
                role = "curveRight",
                attributes = attributes.curveRight,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollback(board, created)
                return result
            }
        }
        created += curveRight

        val baseRight = when (
            val result = createBasePoint(
                board = board,
                curvePoint = curveRight,
                axis = attributes.axis,
                role = "baseRight",
                attributes = attributes.baseRight,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollback(board, created)
                return result
            }
        }
        created += baseRight

        val definition = CurveIntegralDefinition(
            source = source,
            curveLeft = curveLeft,
            curveLeftDynamic = interval.first is IntegralBoundary.Dynamic,
            baseLeft = baseLeft,
            curveRight = curveRight,
            curveRightDynamic = interval.second is IntegralBoundary.Dynamic,
            baseRight = baseRight,
            axis = attributes.axis,
            labelDigits = attributes.label.digits,
        )
        val integral = when (
            val result = Curve.createIntegral(
                board = board,
                definition = definition,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollback(board, created)
                return GMResult.Err(IntegralError.CurveFactory(result.error))
            }
        }
        created += integral

        integral.setParents(listOf(source))
        integral.subs["curveLeft"] = curveLeft
        integral.subs["baseLeft"] = baseLeft
        integral.subs["curveRight"] = curveRight
        integral.subs["baseRight"] = baseRight
        integral.inherits += listOf(
            curveLeft,
            baseLeft,
            curveRight,
            baseRight,
        )
        for (helper in integral.inherits) {
            helper.addChild(integral)
        }

        if (attributes.withLabel && attributes.axis != "y") {
            val label = when (
                val result = createLabel(
                    board = board,
                    integral = integral,
                    attributes = attributes.label,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    rollback(board, created)
                    return result
                }
            }
            definition.label = label
            integral.subs["label"] = label
            integral.inherits += label
            curveLeft.addChild(label)
            curveRight.addChild(label)
            integral.prepareUpdate().update()
        }
        return GMResult.Ok(integral)
    }

    private fun createCurvePoint(
        board: Board,
        source: Curve,
        boundary: IntegralBoundary,
        initialValue: Double,
        role: String,
        attributes: IntegralElementAttributes,
    ): GMResult<Glider, IntegralError> =
        when (
            val result = Glider.create(
                board = board,
                coordinates = doubleArrayOf(
                    initialValue,
                    source.Y(initialValue),
                ),
                slideObject = source,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                fixed = attributes.fixed,
                coordinateConstraint =
                    (boundary as? IntegralBoundary.Dynamic)?.let {
                        IntegralCurveCoordinateFunction(
                            boundary = it.term,
                            source = source,
                        )
                    },
            )
        ) {
            is GMResult.Ok -> {
                result.value.dump = false
                GMResult.Ok(result.value)
            }
            is GMResult.Err -> GMResult.Err(
                IntegralError.GliderFactory(
                    role = role,
                    error = result.error,
                ),
            )
        }

    private fun createBasePoint(
        board: Board,
        curvePoint: Glider,
        axis: String,
        role: String,
        attributes: IntegralElementAttributes,
    ): GMResult<Point, IntegralError> =
        when (
            val result = Point.createConstrained(
                board = board,
                coordinateFunctions = listOf(
                    IntegralBaseCoordinateFunction(curvePoint, axis),
                ),
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                fixed = attributes.fixed,
            )
        ) {
            is GMResult.Ok -> {
                result.value.dump = false
                GMResult.Ok(result.value)
            }
            is GMResult.Err -> GMResult.Err(
                IntegralError.PointFactory(
                    role = role,
                    error = result.error,
                ),
            )
        }

    private fun createLabel(
        board: Board,
        integral: Curve,
        attributes: IntegralLabelAttributes,
    ): GMResult<Text, IntegralError> {
        val definition = integral.integralDefinition
            ?: return GMResult.Err(
                IntegralError.CurveFactory(
                    CurveError.InvalidInterpolationPointCount(
                        creator = "integral",
                        count = 0,
                        minimum = 1,
                    ),
                ),
            )
        val position = labelPosition(board, definition.curveRight)
        val label = when (
            val result = Text.create(
                board = board,
                coordinates = doubleArrayOf(position.first, position.second),
                content = integralLabel(integral.Value(), attributes.digits),
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                parse = false,
                digits = attributes.digits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                IntegralError.TextFactory(result.error),
            )
        }
        label.dump = false
        label.screenOffset = attributes.offset.copyOf()
        return GMResult.Ok(label)
    }

    private fun labelPosition(
        board: Board,
        curveRight: Glider,
    ): Pair<Double, Double> {
        val boundingBox = board.getBoundingBox()
        val dx = (boundingBox[2] - boundingBox[0]) * 0.1
        val dy = (boundingBox[1] - boundingBox[3]) * 0.1
        val curveX = curveRight.X()
        val x = when {
            curveX < boundingBox[0] -> boundingBox[0] + dx
            curveX > boundingBox[2] -> boundingBox[2] - dx
            else -> curveX
        }
        val curveY = curveRight.Y()
        val y = when {
            curveY > boundingBox[1] -> boundingBox[1] - dy
            curveY < boundingBox[3] -> boundingBox[3] + dy
            else -> curveY
        }
        return x to y
    }

    private fun integralLabel(
        value: Double,
        digits: Int,
    ): String = "\u222b = ${JsNumberFormat.fixed(value, digits)}"

    private fun evaluateBoundary(
        role: String,
        boundary: IntegralBoundary,
    ): GMResult<Double, IntegralError> {
        if (boundary is IntegralBoundary.Fixed) {
            return GMResult.Ok(boundary.value)
        }
        val value = when (
            val result = (boundary as IntegralBoundary.Dynamic).term.evaluate()
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                IntegralError.BoundaryEvaluation(role, result.error),
            )
        }
        return if (value is JessieCodeRuntimeValue.NumberValue) {
            GMResult.Ok(value.value)
        } else {
            GMResult.Err(
                IntegralError.BoundaryNonNumeric(
                    role = role,
                    actualType = runtimeType(value),
                ),
            )
        }
    }

    private fun requestedIds(
        attributes: IntegralAttributes,
    ): List<String> = buildList {
        add(attributes.id)
        add(attributes.curveLeft.id)
        add(attributes.baseLeft.id)
        add(attributes.curveRight.id)
        add(attributes.baseRight.id)
        if (attributes.withLabel && attributes.axis != "y") {
            add(attributes.label.id)
        }
    }

    private fun rollback(
        board: Board,
        created: List<GeometryElement>,
    ) {
        board.removeObjects(created.asReversed())
    }
}

private class IntegralCurveCoordinateFunction(
    private val boundary: JessieCodeCoordinateFunction,
    private val source: Curve,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> =
        boundary.dependencies
    override val returnsCoordinateArray: Boolean = true

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        when (val result = boundary.evaluate()) {
            is GMResult.Err -> result
            is GMResult.Ok -> {
                val value = result.value
                if (value is JessieCodeRuntimeValue.NumberValue) {
                    GMResult.Ok(
                        JessieCodeRuntimeValue.ArrayValue(
                            listOf(
                                value,
                                JessieCodeRuntimeValue.NumberValue(
                                    source.Y(value.value),
                                ),
                            ),
                        ),
                    )
                } else {
                    GMResult.Ok(
                        JessieCodeRuntimeValue.ArrayValue(
                            listOf(value, value),
                        ),
                    )
                }
            }
        }
}

private class IntegralBaseCoordinateFunction(
    private val curvePoint: Glider,
    private val axis: String,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()
    override val returnsCoordinateArray: Boolean = true

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Ok(
            JessieCodeRuntimeValue.ArrayValue(
                if (axis == "y") {
                    listOf(
                        JessieCodeRuntimeValue.NumberValue(0.0),
                        JessieCodeRuntimeValue.NumberValue(curvePoint.Y()),
                    )
                } else {
                    listOf(
                        JessieCodeRuntimeValue.NumberValue(curvePoint.X()),
                        JessieCodeRuntimeValue.NumberValue(0.0),
                    )
                },
            ),
        )
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
        is JessieCodeRuntimeValue.CompositionReference -> "composition"
        is JessieCodeRuntimeValue.ElementReference -> "element"
    }
