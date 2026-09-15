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
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Line

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
