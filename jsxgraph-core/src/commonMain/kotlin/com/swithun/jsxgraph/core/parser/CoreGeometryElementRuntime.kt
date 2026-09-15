/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> resolveProperty and the methodMap
 * declarations in src/base/coordselement.js, line.js, and circle.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.utils.JsNumberFormat

/**
 * The methodMap subset backed by geometry classes translated so far.
 */
internal object CoreGeometryElementRuntime : JessieCodeElementRuntime {
    override fun valueOf(
        element: GeometryElement,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.ElementValueUnavailable(
                elementId = element.id,
                location = location,
            ),
        )

    override fun resolveProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        resolveGeometryProperty(element, property)?.let {
            return it
        }
        resolveCircleProperty(element, property, location)?.let {
            return it
        }
        resolveLineProperty(element, property, location)?.let {
            return it
        }
        resolveCoordsProperty(element, property, location)?.let {
            return it
        }
        return unavailable(element, property, location)
    }

    override fun assignProperty(
        element: GeometryElement,
        property: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Unit, JessieCodeRuntimeError> {
        if (element is Point && (property == "X" || property == "Y")) {
            return assignPointCoordinate(
                point = element,
                property = property,
                value = value,
                location = location,
            )
        }
        when (normalizedAttributeName(property)) {
            "name" -> return assignName(
                element,
                property,
                value,
                location,
            )
            "needsregularupdate" -> {
                element.needsRegularUpdate = !(
                    value == JessieCodeRuntimeValue.BooleanValue(false) ||
                        value == JessieCodeRuntimeValue.StringValue("false")
                    )
                return GMResult.Ok(Unit)
            }
        }
        return assignmentUnavailable(element, property, location)
    }

    private fun resolveGeometryProperty(
        element: GeometryElement,
        property: String,
    ): ElementPropertyResult? =
        when (property) {
            "name" -> GMResult.Ok(
                JessieCodeRuntimeValue.StringValue(element.name),
            )
            "needsRegularUpdate" -> GMResult.Ok(
                JessieCodeRuntimeValue.BooleanValue(
                    element.needsRegularUpdate,
                ),
            )
            "getName", "Name" -> stringFunction("getName") {
                element.name
            }
            "setName" -> function("setName") {
                    arguments,
                    callLocation,
                ->
                val name = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.StringValue
                    )?.value
                if (name == null) {
                    GMResult.Err(
                        JessieCodeRuntimeError.InvalidArgumentType(
                            functionName = "setName",
                            argumentIndex = 0,
                            expected = "string",
                            actual = typeName(
                                arguments.firstOrNull()
                                    ?: JessieCodeRuntimeValue.UndefinedValue,
                            ),
                            location = callLocation,
                        ),
                    )
                } else {
                    element.setName(
                        name.replace("<", "&lt;")
                            .replace(">", "&gt;"),
                    )
                    GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
                }
            }
            else -> null
        }

    private fun resolveCircleProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val circle = element as? Circle ?: return null
        return when (property) {
            "Area", "area" -> numberFunction("Area") {
                circle.Area()
            }
            "Perimeter", "Circumference" ->
                numberFunction("Perimeter") {
                    circle.Perimeter()
                }
            "radius", "Radius", "getRadius" ->
                numberFunction("Radius") {
                    circle.Radius()
                }
            "Diameter" -> numberFunction("Diameter") {
                circle.Diameter()
            }
            "center" -> GMResult.Ok(
                JessieCodeRuntimeValue.ElementReference(circle.center),
            )
            "line" -> circle.line?.let {
                GMResult.Ok(
                    JessieCodeRuntimeValue.ElementReference(it),
                )
            } ?: unavailable(circle, property, location)
            "point2" -> circle.point2?.let {
                GMResult.Ok(
                    JessieCodeRuntimeValue.ElementReference(it),
                )
            } ?: unavailable(circle, property, location)
            else -> unavailable(circle, property, location)
        }
    }

    private fun resolveLineProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val line = element as? Line ?: return null
        return when (property) {
            "point1" -> GMResult.Ok(
                JessieCodeRuntimeValue.ElementReference(line.point1),
            )
            "point2" -> GMResult.Ok(
                JessieCodeRuntimeValue.ElementReference(line.point2),
            )
            "getSlope", "Slope" -> numberFunction("Slope") {
                line.Slope()
            }
            "Direction" -> function("Direction") { _, _ ->
                GMResult.Ok(array(line.Direction()))
            }
            "getRise", "Rise", "getYIntersect", "YIntersect" ->
                numberFunction("getRise") {
                    line.getRise()
                }
            "getAngle", "Angle" -> function("getAngle") {
                    arguments,
                    _,
                ->
                val unit = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.StringValue
                    )?.value
                if (unit == null) {
                    number(line.getAngle())
                } else {
                    when (val result = line.getAngle(unit)) {
                        is GMResult.Ok -> number(result.value)
                        is GMResult.Err -> GMResult.Ok(
                            JessieCodeRuntimeValue.UndefinedValue,
                        )
                    }
                }
            }
            "L", "length" -> numberFunction("L") {
                line.L()
            }
            else -> unavailable(line, property, location)
        }
    }

    private fun resolveCoordsProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val coordinates = element as? CoordsElement ?: return null
        return when (property) {
            "X" -> numberFunction("X") { coordinates.X() }
            "Y" -> numberFunction("Y") { coordinates.Y() }
            "Coords" -> function("Coords") { arguments, _ ->
                val withZ = arguments.firstOrNull()?.let(::isTruthy)
                    ?: false
                GMResult.Ok(array(coordinates.Coords(withZ)))
            }
            "dist", "Dist" -> function("Dist") {
                    arguments,
                    callLocation,
                ->
                val other = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ElementReference
                    )?.element as? CoordsElement
                if (other == null) {
                    GMResult.Err(
                        JessieCodeRuntimeError.InvalidArgumentType(
                            functionName = "Dist",
                            argumentIndex = 0,
                            expected = "coordinate element",
                            actual = typeName(
                                arguments.firstOrNull()
                                    ?: JessieCodeRuntimeValue
                                        .UndefinedValue,
                            ),
                            location = callLocation,
                        ),
                    )
                } else {
                    number(coordinates.Dist(other))
                }
            }
            else -> unavailable(coordinates, property, location)
        }
    }

    private fun numberFunction(
        name: String,
        value: () -> Double,
    ): ElementPropertyResult =
        GMResult.Ok(
            JessieCodeRuntimeValue.FunctionValue(
                name = name,
                callable = JessieCodeCallable { _, _ ->
                    number(value())
                },
            ),
        )

    private fun stringFunction(
        name: String,
        value: () -> String,
    ): ElementPropertyResult =
        GMResult.Ok(
            JessieCodeRuntimeValue.FunctionValue(
                name = name,
                callable = JessieCodeCallable { _, _ ->
                    GMResult.Ok(
                        JessieCodeRuntimeValue.StringValue(value()),
                    )
                },
            ),
        )

    private fun function(
        name: String,
        callable: JessieCodeCallable,
    ): ElementPropertyResult =
        GMResult.Ok(
            JessieCodeRuntimeValue.FunctionValue(name, callable),
        )

    private fun array(values: DoubleArray): JessieCodeRuntimeValue =
        JessieCodeRuntimeValue.ArrayValue(
            values.map(JessieCodeRuntimeValue::NumberValue),
        )

    private fun number(value: Double): ElementPropertyResult =
        GMResult.Ok(JessieCodeRuntimeValue.NumberValue(value))

    private fun unavailable(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult =
        GMResult.Err(
            JessieCodeRuntimeError.ElementPropertyUnavailable(
                elementId = element.id,
                property = property,
                location = location,
            ),
        )

    private fun assignPointCoordinate(
        point: Point,
        property: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Unit, JessieCodeRuntimeError> {
        if (
            point.isDraggable &&
            value is JessieCodeRuntimeValue.NumberValue
        ) {
            val x = if (property == "X") value.value else point.X()
            val y = if (property == "Y") value.value else point.Y()
            point.setPosition(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(x, y),
            )
            point.board.update()
            return GMResult.Ok(Unit)
        }

        val source = when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                JsNumberFormat.compact(value.value)
            is JessieCodeRuntimeValue.StringValue -> value.value
            else -> return invalidAssignmentValue(
                element = point,
                property = property,
                expected = "number or string",
                actual = value,
                location = location,
            )
        }
        val x = if (property == "X") {
            source
        } else {
            coordinateOrigin(point, coordinateIndex = 0)
        }
        val y = if (property == "Y") {
            source
        } else {
            coordinateOrigin(point, coordinateIndex = 1)
        }
        return when (
            val result = point.replaceCoordinateConstraints(listOf(x, y))
        ) {
            is GMResult.Ok -> {
                point.board.update()
                GMResult.Ok(Unit)
            }
            is GMResult.Err -> GMResult.Err(
                JessieCodeRuntimeError.ElementCoordinateConstraintFailure(
                    elementId = point.id,
                    property = property,
                    error = result.error,
                    location = location,
                ),
            )
        }
    }

    private fun coordinateOrigin(
        point: Point,
        coordinateIndex: Int,
    ): String {
        val functionIndex = when (point.coordinateFunctions.size) {
            2 -> coordinateIndex
            in 3..Int.MAX_VALUE -> coordinateIndex + 1
            else -> -1
        }
        return point.coordinateFunctions.getOrNull(functionIndex)?.origin
            ?: JsNumberFormat.compact(
                if (coordinateIndex == 0) point.X() else point.Y(),
            )
    }

    private fun assignName(
        element: GeometryElement,
        property: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Unit, JessieCodeRuntimeError> {
        val name = (value as? JessieCodeRuntimeValue.StringValue)?.value
            ?: return invalidAssignmentValue(
                element = element,
                property = property,
                expected = "string",
                actual = value,
                location = location,
            )
        element.setName(name)
        return GMResult.Ok(Unit)
    }

    private fun invalidAssignmentValue(
        element: GeometryElement,
        property: String,
        expected: String,
        actual: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Unit, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.InvalidElementPropertyValue(
                elementId = element.id,
                property = property,
                expected = expected,
                actual = typeName(actual),
                location = location,
            ),
        )

    private fun assignmentUnavailable(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): GMResult<Unit, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.ElementPropertyAssignmentUnavailable(
                elementId = element.id,
                property = property,
                location = location,
            ),
        )

    private fun normalizedAttributeName(property: String): String =
        property.filterNot(Char::isWhitespace).lowercase()

    private fun isTruthy(value: JessieCodeRuntimeValue): Boolean =
        when (value) {
            JessieCodeRuntimeValue.NullValue,
            JessieCodeRuntimeValue.UndefinedValue,
            -> false
            is JessieCodeRuntimeValue.BooleanValue -> value.value
            is JessieCodeRuntimeValue.NumberValue ->
                value.value != 0.0 && !value.value.isNaN()
            is JessieCodeRuntimeValue.StringValue ->
                value.value.isNotEmpty()
            else -> true
        }

    private fun typeName(value: JessieCodeRuntimeValue): String =
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

private typealias ElementPropertyResult =
    GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>
