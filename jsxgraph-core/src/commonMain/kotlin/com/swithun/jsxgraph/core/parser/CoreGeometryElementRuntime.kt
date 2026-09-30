/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> resolveProperty and the methodMap
 * declarations in src/base/coordselement.js, line.js, circle.js, curve.js,
 * polygon.js, text.js, image.js, and element/slider.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.ForeignObject
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Glider
import com.swithun.jsxgraph.core.base.HtmlControlDefinition
import com.swithun.jsxgraph.core.base.Image
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.Slider
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.base.TransformationDynamicParameter
import com.swithun.jsxgraph.core.base.TransformationDynamicParameterError
import com.swithun.jsxgraph.core.base.Turtle
import com.swithun.jsxgraph.core.base.TurtleError
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.abs

/**
 * The methodMap subset backed by geometry classes translated so far.
 */
internal object CoreGeometryElementRuntime : JessieCodeElementRuntime {
    override fun valueOf(
        element: GeometryElement,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        when (element) {
            is Polygon ->
                element.slopeTriangleDefinition?.let {
                    number(it.Slope())
                } ?: GMResult.Err(
                    JessieCodeRuntimeError.ElementValueUnavailable(
                        elementId = element.id,
                        location = location,
                    ),
                )
            is Slider -> number(element.Value())
            is Text ->
                when (val control = element.htmlControlDefinition) {
                    is HtmlControlDefinition.Checkbox ->
                        GMResult.Ok(
                            JessieCodeRuntimeValue.BooleanValue(
                                control.checked,
                            ),
                        )
                    is HtmlControlDefinition.Input ->
                        GMResult.Ok(
                            JessieCodeRuntimeValue.StringValue(control.value),
                        )
                    is HtmlControlDefinition.Slider ->
                        number(control.value)
                    is HtmlControlDefinition.Button,
                    null,
                    -> element.smartLabelDefinition?.let { definition ->
                        measurementValue(
                            element = element,
                            method = "Value",
                            location = location,
                            result = definition.Value(),
                        )
                    } ?: element.measurementDefinition?.let { definition ->
                        measurementValue(
                            element = element,
                            method = "Value",
                            location = location,
                            result = definition.Value(),
                        )
                    } ?: GMResult.Err(
                        JessieCodeRuntimeError.ElementValueUnavailable(
                            elementId = element.id,
                            location = location,
                        ),
                    )
                }
            is Line ->
                element.tapemeasureDefinition?.let {
                    number(it.Value())
                } ?: GMResult.Err(
                    JessieCodeRuntimeError.ElementValueUnavailable(
                        elementId = element.id,
                        location = location,
                    ),
                )
            is Arc -> number(element.Value())
            is Sector -> number(element.Value())
            is Curve ->
                if (element.isRiemannSum || element.isIntegral) {
                    number(element.Value())
                } else {
                    GMResult.Err(
                        JessieCodeRuntimeError.ElementValueUnavailable(
                            elementId = element.id,
                            location = location,
                        ),
                    )
                }
            else -> GMResult.Err(
                JessieCodeRuntimeError.ElementValueUnavailable(
                    elementId = element.id,
                    location = location,
                ),
            )
        }

    override fun resolveProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        resolveGeometryProperty(element, property)?.let {
            return it
        }
        resolveTurtleProperty(element, property, location)?.let {
            return it
        }
        resolveSliderProperty(element, property)?.let {
            return it
        }
        resolveGliderProperty(element, property)?.let {
            return it
        }
        resolveCurveProperty(element, property)?.let {
            return it
        }
        resolveSlopeTriangleProperty(element, property, location)?.let {
            return it
        }
        resolvePolygonProperty(element, property, location)?.let {
            return it
        }
        resolveArcSectorProperty(element, property, location)?.let {
            return it
        }
        resolveCircleProperty(element, property, location)?.let {
            return it
        }
        resolveLineProperty(element, property, location)?.let {
            return it
        }
        resolveMeasurementProperty(element, property, location)?.let {
            return it
        }
        resolveSmartLabelProperty(element, property, location)?.let {
            return it
        }
        resolveHtmlControlProperty(element, property, location)?.let {
            return it
        }
        resolveTextProperty(element, property, location)?.let {
            return it
        }
        resolveForeignObjectProperty(element, property, location)?.let {
            return it
        }
        resolveImageProperty(element, property, location)?.let {
            return it
        }
        resolveCoordsProperty(element, property, location)?.let {
            return it
        }
        return unavailable(element, property, location)
    }

    // JSXGraph 1.13.3: src/base/turtle.js -> methodMap.
    private fun resolveTurtleProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val turtle = element as? Turtle ?: return null
        return when (property) {
            "forward", "fd" ->
                turtleNumberMethod(turtle, "forward", turtle::forward)
            "back", "bk" ->
                turtleNumberMethod(turtle, "back", turtle::back)
            "right", "rt" ->
                turtleNumberMethod(turtle, "right") { angle ->
                    turtle.right(angle)
                    GMResult.Ok(turtle)
                }
            "left", "lt" ->
                turtleNumberMethod(turtle, "left") { angle ->
                    turtle.left(angle)
                    GMResult.Ok(turtle)
                }
            "penUp", "pu", "up" -> turtleMutation("penUp", turtle) {
                GMResult.Ok(turtle.penUp())
            }
            "penDown", "pd", "down" ->
                turtleMutation("penDown", turtle, turtle::penDown)
            "clearScreen", "cs" ->
                turtleMutation("clearScreen", turtle, turtle::clearScreen)
            "clean" -> turtleMutation("clean", turtle, turtle::clean)
            "setPos" -> function("setPos") { arguments, callLocation ->
                val result = when (
                    val coordinates = arguments.firstOrNull()
                ) {
                    is JessieCodeRuntimeValue.ArrayValue ->
                        turtleCoordinateArray(
                            functionName = "setPos",
                            value = coordinates,
                            location = callLocation,
                        ).let { parsed ->
                            when (parsed) {
                                is GMResult.Ok ->
                                    turtle.setPos(parsed.value)
                                is GMResult.Err -> return@function parsed
                            }
                        }
                    else -> {
                        val x = turtleNumberArgument(
                            functionName = "setPos",
                            arguments = arguments,
                            index = 0,
                            location = callLocation,
                        )
                        val y = turtleNumberArgument(
                            functionName = "setPos",
                            arguments = arguments,
                            index = 1,
                            location = callLocation,
                        )
                        val xValue = when (x) {
                            is GMResult.Ok -> x.value
                            is GMResult.Err -> return@function x
                        }
                        val yValue = when (y) {
                            is GMResult.Ok -> y.value
                            is GMResult.Err -> return@function y
                        }
                        turtle.setPos(xValue, yValue)
                    }
                }
                turtleMutationResult(
                    turtle = turtle,
                    method = "setPos",
                    location = callLocation,
                    result = result,
                )
            }
            "home" -> turtleMutation("home", turtle, turtle::home)
            "hideTurtle", "ht", "hide" ->
                turtleMutation("hideTurtle", turtle) {
                    GMResult.Ok(turtle.hideTurtle())
                }
            "showTurtle", "st", "show" ->
                turtleMutation(
                    "showTurtle",
                    turtle,
                    turtle::showTurtle,
                )
            "penSize", "setPenSize" ->
                turtleNumberMethod(
                    turtle,
                    "setPenSize",
                    turtle::setPenSize,
                )
            "penColor", "setPenColor" ->
                turtleStringMethod(
                    turtle,
                    "setPenColor",
                    turtle::setPenColor,
                )
            "highlightPenColor", "setHighlightPenColor" ->
                turtleStringMethod(
                    turtle,
                    "setHighlightPenColor",
                    turtle::setHighlightPenColor,
                )
            "getPenColor", "Color" ->
                stringFunction("getPenColor", turtle::getPenColor)
            "getHighlightPenColor", "HighlightColor" ->
                stringFunction(
                    "getHighlightPenColor",
                    turtle::getHighlightPenColor,
                )
            "getPenSize", "Size" ->
                numberFunction("getPenSize", turtle::getPenSize)
            "pushTurtle", "push" ->
                turtleMutation("pushTurtle", turtle) {
                    GMResult.Ok(turtle.pushTurtle())
                }
            "popTurtle", "pop" ->
                turtleMutation("popTurtle", turtle, turtle::popTurtle)
            "lookTo" -> function("lookTo") { arguments, callLocation ->
                val result = when (
                    val target = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    is JessieCodeRuntimeValue.NumberValue ->
                        if (target.value.isFinite()) {
                            GMResult.Ok(turtle.lookTo(target.value))
                        } else {
                            GMResult.Err(
                                TurtleError.InvalidCoordinate(
                                    coordinate = "target",
                                    value = target.value,
                                ),
                            )
                        }
                    is JessieCodeRuntimeValue.ArrayValue ->
                        when (
                            val parsed = turtleCoordinateArray(
                                functionName = "lookTo",
                                value = target,
                                location = callLocation,
                            )
                        ) {
                            is GMResult.Ok -> turtle.lookTo(parsed.value)
                            is GMResult.Err -> return@function parsed
                        }
                    else -> return@function invalidArgumentType(
                        functionName = "lookTo",
                        argumentIndex = 0,
                        expected = "number or coordinate array",
                        actual = target,
                        location = callLocation,
                    )
                }
                turtleMutationResult(
                    turtle = turtle,
                    method = "lookTo",
                    location = callLocation,
                    result = result,
                )
            }
            "pos", "Pos" -> GMResult.Ok(array(turtle.position))
            "moveTo" -> function("moveTo") { arguments, callLocation ->
                val coordinates = when (
                    val result = turtleCoordinateArray(
                        functionName = "moveTo",
                        value = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@function result
                }
                turtleMutationResult(
                    turtle = turtle,
                    method = "moveTo",
                    location = callLocation,
                    result = turtle.moveTo(coordinates),
                )
            }
            "X" -> turtleCoordinateFunction("X", turtle::X)
            "Y" -> turtleCoordinateFunction("Y", turtle::Y)
            "Z" -> turtleCoordinateFunction("Z", turtle::Z)
            "minX" -> numberFunction("minX", turtle::minX)
            "maxX" -> numberFunction("maxX", turtle::maxX)
            else -> null
        }
    }

    private fun turtleMutation(
        method: String,
        turtle: Turtle,
        operation: () -> GMResult<Turtle, TurtleError>,
    ): ElementPropertyResult =
        function(method) { _, location ->
            turtleMutationResult(
                turtle = turtle,
                method = method,
                location = location,
                result = operation(),
            )
        }

    private fun turtleNumberMethod(
        turtle: Turtle,
        method: String,
        operation: (Double) -> GMResult<Turtle, TurtleError>,
    ): ElementPropertyResult =
        function(method) { arguments, location ->
            val value = when (
                val result = turtleNumberArgument(
                    functionName = method,
                    arguments = arguments,
                    index = 0,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return@function result
            }
            val operationResult =
                if (value.isFinite()) {
                    operation(value)
                } else {
                    GMResult.Err(
                        TurtleError.InvalidCoordinate(
                            coordinate = method,
                            value = value,
                        ),
                    )
                }
            turtleMutationResult(
                turtle = turtle,
                method = method,
                location = location,
                result = operationResult,
            )
        }

    private fun turtleStringMethod(
        turtle: Turtle,
        method: String,
        operation: (String) -> GMResult<Turtle, TurtleError>,
    ): ElementPropertyResult =
        function(method) { arguments, location ->
            val value = (
                arguments.firstOrNull() as?
                    JessieCodeRuntimeValue.StringValue
                )?.value ?: return@function invalidArgumentType(
                functionName = method,
                argumentIndex = 0,
                expected = "string",
                actual = arguments.firstOrNull()
                    ?: JessieCodeRuntimeValue.UndefinedValue,
                location = location,
            )
            turtleMutationResult(
                turtle = turtle,
                method = method,
                location = location,
                result = operation(value),
            )
        }

    private fun turtleCoordinateFunction(
        method: String,
        operation: (Double?) -> Double,
    ): ElementPropertyResult =
        function(method) { arguments, location ->
            val value = arguments.firstOrNull()
            val parameter = when (value) {
                null, JessieCodeRuntimeValue.UndefinedValue -> null
                is JessieCodeRuntimeValue.NumberValue -> value.value
                else -> return@function invalidArgumentType(
                    functionName = method,
                    argumentIndex = 0,
                    expected = "number or undefined",
                    actual = value,
                    location = location,
                )
            }
            number(operation(parameter))
        }

    private fun turtleNumberArgument(
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        index: Int,
        location: JessieCodeAstLocation,
    ): GMResult<Double, JessieCodeRuntimeError> {
        val value = arguments.getOrNull(index)
            ?: JessieCodeRuntimeValue.UndefinedValue
        val number = (value as? JessieCodeRuntimeValue.NumberValue)?.value
            ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = index,
                expected = "number",
                actual = value,
                location = location,
            )
        return GMResult.Ok(number)
    }

    private fun turtleCoordinateArray(
        functionName: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<DoubleArray, JessieCodeRuntimeError> {
        val values = (value as? JessieCodeRuntimeValue.ArrayValue)?.values
            ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = 0,
                expected = "coordinate array",
                actual = value,
                location = location,
            )
        if (values.size < 2) {
            return GMResult.Err(
                JessieCodeRuntimeError.InvalidArgumentCount(
                    functionName = "$functionName coordinate array",
                    expected = "at least 2",
                    actual = values.size,
                    location = location,
                ),
            )
        }
        val coordinates = DoubleArray(2)
        for (index in coordinates.indices) {
            val coordinate = values[index]
            val number = (
                coordinate as? JessieCodeRuntimeValue.NumberValue
                )?.value ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = index,
                expected = "number coordinate",
                actual = coordinate,
                location = location,
            )
            coordinates[index] = number
        }
        return GMResult.Ok(coordinates)
    }

    private fun turtleMutationResult(
        turtle: Turtle,
        method: String,
        location: JessieCodeAstLocation,
        result: GMResult<Turtle, TurtleError>,
    ): ElementPropertyResult =
        when (result) {
            is GMResult.Ok -> elementReference(turtle)
            is GMResult.Err -> GMResult.Err(
                JessieCodeRuntimeError.ElementMethodUnavailable(
                    elementId = turtle.id,
                    method = method,
                    reason = result.error.toString(),
                    location = location,
                ),
            )
        }

    private fun resolveCurveProperty(
        element: GeometryElement,
        property: String,
    ): ElementPropertyResult? {
        val curve = element as? Curve ?: return null
        if (
            curve.isEllipse ||
            curve.isHyperbola ||
            curve.isParabola ||
            curve.isGenericConic
        ) {
            return when (property) {
                "majorAxis" ->
                    if (curve.isEllipse || curve.isHyperbola) {
                        numberFunction("majorAxis", curve::majorAxis)
                    } else {
                        null
                    }
                "center", "midpoint" -> curve.center?.let {
                    GMResult.Ok(
                        JessieCodeRuntimeValue.ElementReference(it),
                    )
                }
                "subs" -> GMResult.Ok(
                    JessieCodeRuntimeValue.ObjectValue(
                        curve.subs.mapValues { (_, child) ->
                            JessieCodeRuntimeValue.ElementReference(child)
                        },
                    ),
                )
                else -> null
            }
        }
        curve.integralDefinition?.let { definition ->
            return when (property) {
                "curveLeft" -> elementReference(definition.curveLeft)
                "baseLeft" -> elementReference(definition.baseLeft)
                "curveRight" -> elementReference(definition.curveRight)
                "baseRight" -> elementReference(definition.baseRight)
                "label" -> definition.label?.let(::elementReference)
                "subs" -> GMResult.Ok(
                    JessieCodeRuntimeValue.ObjectValue(
                        curve.subs.mapValues { (_, child) ->
                            JessieCodeRuntimeValue.ElementReference(child)
                        },
                    ),
                )
                "V", "Value" ->
                    numberFunction("Value", curve::Value)
                else -> null
            }
        }
        if (!curve.isRiemannSum) {
            return null
        }
        return when (property) {
            "V", "Value" -> numberFunction("Value", curve::Value)
            else -> null
        }
    }

    private fun resolveArcSectorProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val radius = when (element) {
            is Arc -> element::Radius
            is Sector -> element::Radius
            else -> return null
        }
        return when (property) {
            "radius", "Radius", "getRadius" ->
                numberFunction("Radius", radius)
            "V", "Value" -> function("Value") {
                    arguments,
                    callLocation,
                ->
                val unit = when (
                    val value = arguments.firstOrNull()
                ) {
                    null,
                    JessieCodeRuntimeValue.UndefinedValue,
                    -> null
                    is JessieCodeRuntimeValue.StringValue -> value.value
                    else -> return@function GMResult.Err(
                        JessieCodeRuntimeError.InvalidArgumentType(
                            functionName = "Value",
                            argumentIndex = 0,
                            expected = "string",
                            actual = typeName(value),
                            location = callLocation,
                        ),
                    )
                }
                if (unit != null && !isArcValueUnit(unit)) {
                    GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
                } else {
                    number(
                        when (element) {
                            is Arc -> element.Value(unit ?: "length")
                            is Sector -> element.Value(unit)
                            else -> Double.NaN
                        },
                    )
                }
            }
            "center", "point1" -> GMResult.Ok(
                JessieCodeRuntimeValue.ElementReference(
                    when (element) {
                        is Arc -> element.center
                        is Sector -> element.center
                        else -> return null
                    },
                ),
            )
            "radiuspoint", "point2" -> GMResult.Ok(
                JessieCodeRuntimeValue.ElementReference(
                    when (element) {
                        is Arc -> element.radiuspoint
                        is Sector -> element.radiuspoint
                        else -> return null
                    },
                ),
            )
            "anglepoint", "point3" -> GMResult.Ok(
                JessieCodeRuntimeValue.ElementReference(
                    when (element) {
                        is Arc -> element.anglepoint
                        is Sector -> element.anglepoint
                        else -> return null
                    },
                ),
            )
            "point" ->
                if (
                    element is Sector &&
                    element.isAngle &&
                    !element.isTwoLine
                ) {
                    elementReference(element.radiuspoint)
                } else {
                    unavailable(element, property, location)
                }
            "pointsquare" ->
                if (
                    element is Sector &&
                    element.isAngle &&
                    !element.isTwoLine
                ) {
                    elementReference(element.anglepoint)
                } else {
                    unavailable(element, property, location)
                }
            "line1" ->
                if (element is Sector && element.isTwoLine) {
                    element.line1?.let(::elementReference)
                        ?: unavailable(element, property, location)
                } else {
                    unavailable(element, property, location)
                }
            "line2" ->
                if (element is Sector && element.isTwoLine) {
                    element.line2?.let(::elementReference)
                        ?: unavailable(element, property, location)
                } else {
                    unavailable(element, property, location)
                }
            "dot" ->
                if (element is Sector && element.isAngle) {
                    element.dot?.let(::elementReference)
                        ?: unavailable(element, property, location)
                } else {
                    unavailable(element, property, location)
                }
            "setAngle" ->
                if (element is Sector && element.isAngle) {
                    function("setAngle") { arguments, callLocation ->
                        if (element.isTwoLine) {
                            return@function GMResult.Ok(
                                JessieCodeRuntimeValue.UndefinedValue,
                            )
                        }
                        val result = when (
                            val value = arguments.firstOrNull()
                                ?: JessieCodeRuntimeValue.UndefinedValue
                        ) {
                            is JessieCodeRuntimeValue.NumberValue ->
                                element.setAngle(value.value)
                            is JessieCodeRuntimeValue.FunctionValue ->
                                element.setAngle(
                                    TransformationDynamicParameter {
                                        when (
                                            val evaluated =
                                                value.externalCallable.call(
                                                    arguments = emptyList(),
                                                    location = callLocation,
                                                )
                                        ) {
                                            is GMResult.Ok -> {
                                                val number =
                                                    evaluated.value as?
                                                        JessieCodeRuntimeValue
                                                            .NumberValue
                                                if (number == null) {
                                                    GMResult.Err(
                                                        TransformationDynamicParameterError
                                                            .Rejected(
                                                                "Expected " +
                                                                    "number, " +
                                                                    "got " +
                                                                    typeName(
                                                                        evaluated
                                                                            .value,
                                                                    ),
                                                            ),
                                                    )
                                                } else {
                                                    GMResult.Ok(number.value)
                                                }
                                            }
                                            is GMResult.Err -> GMResult.Err(
                                                TransformationDynamicParameterError
                                                    .Rejected(
                                                        evaluated.error
                                                            .toString(),
                                                    ),
                                            )
                                        }
                                    },
                                )
                            else -> return@function invalidArgumentType(
                                functionName = "setAngle",
                                argumentIndex = 0,
                                expected = "number or function",
                                actual = value,
                                location = callLocation,
                            )
                        }
                        when (result) {
                            is GMResult.Ok -> elementReference(element)
                            is GMResult.Err -> GMResult.Err(
                                JessieCodeRuntimeError
                                    .ElementMethodUnavailable(
                                        elementId = element.id,
                                        method = "setAngle",
                                        reason = result.error.toString(),
                                        location = callLocation,
                                    ),
                            )
                        }
                    }
                } else {
                    unavailable(element, property, location)
                }
            "free" ->
                if (element is Sector && element.isAngle) {
                    function("free") { _, _ ->
                        if (element.isTwoLine) {
                            GMResult.Ok(
                                JessieCodeRuntimeValue.UndefinedValue,
                            )
                        } else {
                            elementReference(element.free())
                        }
                    }
                } else {
                    unavailable(element, property, location)
                }
            "point4" -> when (element) {
                is Arc -> element.directionpoint
                is Sector -> element.directionpoint
                else -> null
            }?.let {
                GMResult.Ok(JessieCodeRuntimeValue.ElementReference(it))
            } ?: GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
            else -> unavailable(element, property, location)
        }
    }

    private fun isArcValueUnit(unit: String): Boolean {
        val normalized = unit.lowercase()
        return normalized.isEmpty() ||
            normalized.startsWith("len") ||
            normalized.startsWith("rad") ||
            normalized.startsWith("deg") ||
            normalized.startsWith("sem") ||
            normalized.startsWith("cir")
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
            "Bounds" -> function("Bounds") { _, _ ->
                GMResult.Ok(array(bounds(element)))
            }
            "addChild" -> function("addChild") {
                    arguments,
                    callLocation,
                ->
                val child = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ElementReference
                    )?.element
                if (child == null) {
                    GMResult.Err(
                        JessieCodeRuntimeError.InvalidArgumentType(
                            functionName = "addChild",
                            argumentIndex = 0,
                            expected = "element",
                            actual = typeName(
                                arguments.firstOrNull()
                                    ?: JessieCodeRuntimeValue.UndefinedValue,
                            ),
                            location = callLocation,
                        ),
                    )
                } else {
                    element.addChild(child)
                    GMResult.Ok(
                        JessieCodeRuntimeValue.ElementReference(element),
                    )
                }
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

    private fun resolvePolygonProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val polygon = element as? Polygon ?: return null
        return when (property) {
            "vertices" -> GMResult.Ok(elements(polygon.vertices))
            "borders" -> GMResult.Ok(elements(polygon.borders))
            "parallelPoint" -> polygon.parallelPoint?.let {
                GMResult.Ok(
                    JessieCodeRuntimeValue.ElementReference(it),
                )
            } ?: unavailable(polygon, property, location)
            "A", "Area" -> numberFunction("Area") {
                polygon.Area()
            }
            "Perimeter", "L" -> numberFunction("Perimeter") {
                polygon.Perimeter()
            }
            "boundingBox", "BoundingBox" ->
                function("boundingBox") { _, _ ->
                    GMResult.Ok(array(polygon.bounds()))
                }
            else -> unavailable(polygon, property, location)
        }
    }

    // JSXGraph 1.13.3: src/element/slopetriangle.js ->
    // extendInstanceMethodMap.
    private fun resolveSlopeTriangleProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val polygon = element as? Polygon ?: return null
        val definition = polygon.slopeTriangleDefinition ?: return null
        return when (property) {
            "tangent" -> elementReference(definition.tangent)
            "glider" -> elementReference(definition.glider)
            "basepoint" -> elementReference(definition.basePoint)
            "baseline" -> elementReference(definition.baseLine)
            "toppoint" -> elementReference(definition.topPoint)
            "borderHorizontal" ->
                elementReference(definition.borderHorizontal)
            "borderVertical" ->
                elementReference(definition.borderVertical)
            "borderParallel" ->
                elementReference(definition.borderParallel)
            "label" -> elementReference(definition.label)
            "V", "Value", "Slope" ->
                numberFunction("Slope", definition::Slope)
            "DeltaX" ->
                numberFunction("DeltaX", definition::DeltaX)
            "DeltaY" ->
                numberFunction("DeltaY", definition::DeltaY)
            "Direction" ->
                function("Direction") { _, _ ->
                    GMResult.Ok(array(definition.Direction()))
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
                    number(definition.getAngle())
                } else {
                    when (val result = definition.getAngle(unit)) {
                        is GMResult.Ok -> number(result.value)
                        is GMResult.Err -> GMResult.Ok(
                            JessieCodeRuntimeValue.UndefinedValue,
                        )
                    }
                }
            }
            "subs" -> GMResult.Ok(
                JessieCodeRuntimeValue.ObjectValue(
                    polygon.subs.mapValues { (_, child) ->
                        JessieCodeRuntimeValue.ElementReference(child)
                    },
                ),
            )
            else -> unavailable(polygon, property, location)
        }
    }

    private fun resolveCircleProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val circle = element as? Circle ?: return null
        return when (property) {
            "A", "Area", "area" -> numberFunction("Area") {
                circle.Area()
            }
            "Perimeter", "Circumference" ->
                numberFunction("Perimeter") {
                    circle.Perimeter()
                }
            "R", "radius", "Radius", "getRadius" ->
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
        val tapemeasure = line.tapemeasureDefinition
        return when (property) {
            "V", "Value" -> tapemeasure?.let {
                numberFunction("Value", it::Value)
            } ?: unavailable(line, property, location)
            "label" -> tapemeasure?.label?.let(::elementReference)
                ?: unavailable(line, property, location)
            "ticks" -> tapemeasure?.tapeTicks?.let(::elementReference)
                ?: unavailable(line, property, location)
            "point" -> (line.tangentToPoint ?: line.normalPoint)?.let {
                GMResult.Ok(
                    JessieCodeRuntimeValue.ElementReference(it),
                )
            } ?: unavailable(line, property, location)
            "polar" -> line.tangentToPolar?.let {
                GMResult.Ok(
                    JessieCodeRuntimeValue.ElementReference(it),
                )
            } ?: unavailable(line, property, location)
            "point1" -> GMResult.Ok(
                JessieCodeRuntimeValue.ElementReference(line.point1),
            )
            "point2" -> GMResult.Ok(
                JessieCodeRuntimeValue.ElementReference(line.point2),
            )
            "subs" -> GMResult.Ok(
                JessieCodeRuntimeValue.ObjectValue(
                    line.subs.mapValues { (_, child) ->
                        JessieCodeRuntimeValue.ElementReference(child)
                    },
                ),
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
            "L", "Length", "length" -> numberFunction("L") {
                line.L()
            }
            else -> unavailable(line, property, location)
        }
    }

    // JSXGraph 1.13.3: src/element/measure.js ->
    // extendInstanceMethodMap.
    private fun resolveMeasurementProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val text = element as? Text ?: return null
        val definition = text.measurementDefinition ?: return null
        return when (property) {
            "V", "Value" -> function("Value") { _, callLocation ->
                measurementValue(
                    element = text,
                    method = "Value",
                    location = callLocation,
                    result = definition.Value(),
                )
            }
            "Dimension" -> function("Dimension") { _, callLocation ->
                measurementValue(
                    element = text,
                    method = "Dimension",
                    location = callLocation,
                    result = definition.Dimension(),
                )
            }
            "Unit" -> function("Unit") { arguments, callLocation ->
                measurementValue(
                    element = text,
                    method = "Unit",
                    location = callLocation,
                    result = definition.Unit(arguments.firstOrNull()),
                )
            }
            "getTerm", "Term" -> function("getTerm") { _, _ ->
                GMResult.Ok(definition.getTerm())
            }
            "getMethod", "Method" ->
                function("getMethod") { _, callLocation ->
                    when (val result = definition.getMethod()) {
                        is GMResult.Ok -> GMResult.Ok(
                            JessieCodeRuntimeValue.StringValue(result.value),
                        )
                        is GMResult.Err -> measurementFailure(
                            element = text,
                            method = "getMethod",
                            location = callLocation,
                            reason = result.error.toString(),
                        )
                    }
                }
            "toPrefix" -> function("toPrefix") { _, callLocation ->
                measurementValue(
                    element = text,
                    method = "toPrefix",
                    location = callLocation,
                    result = definition.toPrefix(),
                )
            }
            "getParents", "Parents" ->
                function("getParents") { _, callLocation ->
                    when (val result = definition.getParents()) {
                        is GMResult.Ok -> GMResult.Ok(
                            JessieCodeRuntimeValue.ArrayValue(
                                result.value,
                            ),
                        )
                        is GMResult.Err -> measurementFailure(
                            element = text,
                            method = "getParents",
                            location = callLocation,
                            reason = result.error.toString(),
                        )
                    }
                }
            else -> null
        }
    }

    // JSXGraph 1.13.3: src/element/smartlabel.js ->
    // extendInstanceMethodMap.
    private fun resolveSmartLabelProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val text = element as? Text ?: return null
        val definition = text.smartLabelDefinition ?: return null
        return when (property) {
            "V", "Value" -> function("Value") { _, callLocation ->
                measurementValue(
                    element = text,
                    method = "Value",
                    location = callLocation,
                    result = definition.Value(),
                )
            }
            "Dimension" -> function("Dimension") { _, callLocation ->
                measurementValue(
                    element = text,
                    method = "Dimension",
                    location = callLocation,
                    result = definition.Dimension(),
                )
            }
            "Unit" -> function("Unit") { arguments, callLocation ->
                measurementValue(
                    element = text,
                    method = "Unit",
                    location = callLocation,
                    result = definition.Unit(arguments.firstOrNull()),
                )
            }
            "parent", "parentObject" ->
                elementReference(definition.parentObject)
            else -> null
        }
    }

    private fun resolveTextProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val text = element as? Text ?: return null
        return when (property) {
            "setText" -> function("setText") {
                    arguments,
                    callLocation,
                ->
                val content = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.StringValue
                    )?.value
                if (content == null) {
                    GMResult.Err(
                        JessieCodeRuntimeError.InvalidArgumentType(
                            functionName = "setText",
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
                    when (val result = text.setText(content)) {
                        is GMResult.Ok -> GMResult.Ok(
                            JessieCodeRuntimeValue.ElementReference(
                                result.value,
                            ),
                        )
                        is GMResult.Err -> GMResult.Err(
                            JessieCodeRuntimeError.ElementMethodUnavailable(
                                elementId = text.id,
                                method = "setText",
                                reason = result.error.toString(),
                                location = callLocation,
                            ),
                        )
                    }
                }
            }
            else -> null
        }
    }

    // JSXGraph 1.13.3: src/element/checkbox.js -> Value;
    // src/element/input.js -> Value/set;
    // src/base/text.js -> createHTMLSlider/Value.
    private fun resolveHtmlControlProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val text = element as? Text ?: return null
        return when (val control = text.htmlControlDefinition) {
            is HtmlControlDefinition.Checkbox -> when (property) {
                "V", "Value" ->
                    booleanFunction("Value") { control.checked }
                else -> null
            }
            is HtmlControlDefinition.Input -> when (property) {
                "V", "Value" ->
                    stringFunction("Value") { control.value }
                "set" -> function("set") { arguments, callLocation ->
                    val value = (
                        arguments.firstOrNull() as?
                            JessieCodeRuntimeValue.StringValue
                        )?.value
                    if (value == null) {
                        GMResult.Err(
                            JessieCodeRuntimeError.InvalidArgumentType(
                                functionName = "set",
                                argumentIndex = 0,
                                expected = "string",
                                actual = typeName(
                                    arguments.firstOrNull()
                                        ?: JessieCodeRuntimeValue
                                            .UndefinedValue,
                                ),
                                location = callLocation,
                            ),
                        )
                    } else {
                        control.value = value
                        GMResult.Ok(
                            JessieCodeRuntimeValue.ElementReference(text),
                        )
                    }
                }
                else -> null
            }
            is HtmlControlDefinition.Slider -> when (property) {
                "V", "Value" -> numberFunction("Value") { control.value }
                else -> null
            }
            is HtmlControlDefinition.Button,
            null,
            -> null
        }
    }

    // JSXGraph: src/base/image.js -> methodMap / setSize.
    private fun resolveImageProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val image = element as? Image ?: return null
        return when (property) {
            "W", "Width" -> numberFunction("W", image::W)
            "H", "Height" -> numberFunction("H", image::H)
            "setSize" -> function("setSize") {
                    arguments,
                    callLocation,
                ->
                if (arguments.size != 2) {
                    return@function GMResult.Err(
                        JessieCodeRuntimeError.InvalidArgumentCount(
                            functionName = "setSize",
                            expected = "2",
                            actual = arguments.size,
                            location = callLocation,
                        ),
                    )
                }
                val terms = mutableListOf<JessieCodeCoordinateFunction>()
                for ((index, value) in arguments.withIndex()) {
                    when (value) {
                        is JessieCodeRuntimeValue.NumberValue ->
                            terms += JessieCodeNumericCoordinateFunction(
                                value.value,
                            )
                        is JessieCodeRuntimeValue.StringValue -> {
                            when (
                                val result =
                                    JessieCodeExpressionFunction.compile(
                                        source = value.value,
                                        board = image.board,
                                    )
                            ) {
                                is GMResult.Ok -> terms += result.value
                                is GMResult.Err -> return@function GMResult.Err(
                                    JessieCodeRuntimeError
                                        .ElementMethodUnavailable(
                                            elementId = image.id,
                                            method = "setSize",
                                            reason =
                                                "size[$index]: " +
                                                    result.error,
                                            location = callLocation,
                                        ),
                                )
                            }
                        }
                        is JessieCodeRuntimeValue.FunctionValue ->
                            terms += JessieCodeRuntimeCoordinateFunction(
                                function = value,
                                location = callLocation,
                                returnsCoordinateArray = false,
                            )
                        else -> return@function invalidArgumentType(
                            functionName = "setSize",
                            argumentIndex = index,
                            expected = "number, string, or function",
                            actual = value,
                            location = callLocation,
                        )
                    }
                }
                when (val result = image.setSize(terms)) {
                    is GMResult.Ok -> {
                        image.board.update()
                        GMResult.Ok(
                            JessieCodeRuntimeValue.ElementReference(image),
                        )
                    }
                    is GMResult.Err -> GMResult.Err(
                        JessieCodeRuntimeError.ElementMethodUnavailable(
                            elementId = image.id,
                            method = "setSize",
                            reason = result.error.toString(),
                            location = callLocation,
                        ),
                    )
                }
            }
            else -> null
        }
    }

    // JSXGraph 1.13.3: src/base/foreignobject.js -> methodMap / setSize.
    private fun resolveForeignObjectProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val foreignObject = element as? ForeignObject ?: return null
        return when (property) {
            "W", "Width" -> foreignObject.W()?.let { value ->
                numberFunction("W") { value }
            } ?: unavailable(foreignObject, property, location)
            "H", "Height" -> foreignObject.H()?.let { value ->
                numberFunction("H") { value }
            } ?: unavailable(foreignObject, property, location)
            "setSize" -> function("setSize") {
                    arguments,
                    callLocation,
                ->
                if (arguments.size != 2) {
                    return@function GMResult.Err(
                        JessieCodeRuntimeError.InvalidArgumentCount(
                            functionName = "setSize",
                            expected = "2",
                            actual = arguments.size,
                            location = callLocation,
                        ),
                    )
                }
                val terms = mutableListOf<JessieCodeCoordinateFunction>()
                for ((index, value) in arguments.withIndex()) {
                    when (value) {
                        is JessieCodeRuntimeValue.NumberValue ->
                            terms += JessieCodeNumericCoordinateFunction(
                                value.value,
                            )
                        is JessieCodeRuntimeValue.StringValue -> {
                            when (
                                val result =
                                    JessieCodeExpressionFunction.compile(
                                        source = value.value,
                                        board = foreignObject.board,
                                    )
                            ) {
                                is GMResult.Ok -> terms += result.value
                                is GMResult.Err -> return@function GMResult.Err(
                                    JessieCodeRuntimeError
                                        .ElementMethodUnavailable(
                                            elementId = foreignObject.id,
                                            method = "setSize",
                                            reason =
                                                "size[$index]: " +
                                                    result.error,
                                            location = callLocation,
                                        ),
                                )
                            }
                        }
                        is JessieCodeRuntimeValue.FunctionValue ->
                            terms += JessieCodeRuntimeCoordinateFunction(
                                function = value,
                                location = callLocation,
                                returnsCoordinateArray = false,
                            )
                        else -> return@function invalidArgumentType(
                            functionName = "setSize",
                            argumentIndex = index,
                            expected = "number, string, or function",
                            actual = value,
                            location = callLocation,
                        )
                    }
                }
                when (val result = foreignObject.setSize(terms)) {
                    is GMResult.Ok -> {
                        foreignObject.board.update()
                        GMResult.Ok(
                            JessieCodeRuntimeValue.ElementReference(
                                foreignObject,
                            ),
                        )
                    }
                    is GMResult.Err -> GMResult.Err(
                        JessieCodeRuntimeError.ElementMethodUnavailable(
                            elementId = foreignObject.id,
                            method = "setSize",
                            reason = result.error.toString(),
                            location = callLocation,
                        ),
                    )
                }
            }
            else -> null
        }
    }

    // JSXGraph 1.13.3: src/element/slider.js -> instance methodMap.
    private fun resolveSliderProperty(
        element: GeometryElement,
        property: String,
    ): ElementPropertyResult? {
        val slider = element as? Slider ?: return null
        return when (property) {
            "V", "Value" -> numberFunction("Value", slider::Value)
            "setValue" -> sliderNumberMethod(
                slider,
                "setValue",
                slider::setValue,
            )
            "setMin" -> sliderNumberMethod(
                slider,
                "setMin",
                slider::setMin,
            )
            "setMax" -> sliderNumberMethod(
                slider,
                "setMax",
                slider::setMax,
            )
            "smin" -> number(slider.sliderMinimum ?: Double.NaN)
            "smax" -> number(slider.sliderMaximum ?: Double.NaN)
            "point1" -> elementReference(slider.point1)
            "point2" -> elementReference(slider.point2)
            "baseline" -> elementReference(slider.baseline)
            "highline" -> elementReference(slider.highline)
            "ticks" -> slider.sliderTicks?.let(::elementReference)
                ?: GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
            "label" -> slider.label?.let(::elementReference)
                ?: GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
            else -> null
        }
    }

    // JSXGraph 1.13.3: src/base/coordselement.js -> methodMap.
    private fun resolveGliderProperty(
        element: GeometryElement,
        property: String,
    ): ElementPropertyResult? {
        val glider = element as? Glider ?: return null
        return when (property) {
            "setPosition", "setGliderPosition" ->
                function("setGliderPosition") {
                        arguments,
                        callLocation,
                    ->
                    val value = (
                        arguments.firstOrNull() as?
                            JessieCodeRuntimeValue.NumberValue
                        )?.value ?: return@function invalidArgumentType(
                        functionName = "setGliderPosition",
                        argumentIndex = 0,
                        expected = "number",
                        actual = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                    glider.setGliderPosition(value)
                    elementReference(glider)
                }
            else -> null
        }
    }

    private fun sliderNumberMethod(
        slider: Slider,
        methodName: String,
        operation: (Double) -> Slider,
    ): ElementPropertyResult =
        function(methodName) { arguments, callLocation ->
            val value = (
                arguments.firstOrNull() as?
                    JessieCodeRuntimeValue.NumberValue
                )?.value ?: return@function invalidArgumentType(
                functionName = methodName,
                argumentIndex = 0,
                expected = "number",
                actual = arguments.firstOrNull()
                    ?: JessieCodeRuntimeValue.UndefinedValue,
                location = callLocation,
            )
            operation(value)
            elementReference(slider)
        }

    private fun elementReference(
        element: GeometryElement,
    ): ElementPropertyResult =
        GMResult.Ok(JessieCodeRuntimeValue.ElementReference(element))

    private fun measurementValue(
        element: Text,
        method: String,
        location: JessieCodeAstLocation,
        result: GMResult<JessieCodeRuntimeValue, *>,
    ): ElementPropertyResult =
        when (result) {
            is GMResult.Ok -> result
            is GMResult.Err -> measurementFailure(
                element = element,
                method = method,
                location = location,
                reason = result.error.toString(),
            )
        }

    private fun measurementFailure(
        element: Text,
        method: String,
        location: JessieCodeAstLocation,
        reason: String,
    ): ElementPropertyResult =
        GMResult.Err(
            JessieCodeRuntimeError.ElementMethodUnavailable(
                elementId = element.id,
                method = method,
                reason = reason,
                location = location,
            ),
        )

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
            "move", "moveTo" -> function("moveTo") {
                    arguments,
                    callLocation,
                ->
                moveTo(
                    element = coordinates,
                    arguments = arguments,
                    location = callLocation,
                )
            }
            "addConstraint" -> function("addConstraint") {
                    arguments,
                    callLocation,
                ->
                addConstraint(
                    element = coordinates,
                    arguments = arguments,
                    location = callLocation,
                )
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

    private fun booleanFunction(
        name: String,
        value: () -> Boolean,
    ): ElementPropertyResult =
        GMResult.Ok(
            JessieCodeRuntimeValue.FunctionValue(
                name = name,
                callable = JessieCodeCallable { _, _ ->
                    GMResult.Ok(
                        JessieCodeRuntimeValue.BooleanValue(value()),
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

    private fun elements(
        values: List<GeometryElement>,
    ): JessieCodeRuntimeValue =
        JessieCodeRuntimeValue.ArrayValue(
            values.map(JessieCodeRuntimeValue::ElementReference),
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

    private fun moveTo(
        element: CoordsElement,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult {
        val coordinates = when (
            val result = coordinateArray(
                functionName = "moveTo",
                value = arguments.firstOrNull()
                    ?: JessieCodeRuntimeValue.UndefinedValue,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val time = arguments.getOrNull(1)
            ?: JessieCodeRuntimeValue.UndefinedValue
        val immediate = when (time) {
            JessieCodeRuntimeValue.UndefinedValue -> true
            is JessieCodeRuntimeValue.NumberValue ->
                time.value == 0.0 ||
                    (
                        coordinates.size == 3 &&
                            abs(coordinates[0] - element.Z()) > Mat.eps
                        )
            else -> return invalidArgumentType(
                functionName = "moveTo",
                argumentIndex = 1,
                expected = "number or undefined",
                actual = time,
                location = location,
            )
        }
        if (!immediate) {
            return GMResult.Err(
                JessieCodeRuntimeError.ElementMethodUnavailable(
                    elementId = element.id,
                    method = "moveTo",
                    reason = "animated movement is not translated",
                    location = location,
                ),
            )
        }

        element.setPosition(Const.COORDS_BY_USER, coordinates)
        element.board.update(element)
        return GMResult.Ok(
            JessieCodeRuntimeValue.BoardReference(element.board),
        )
    }

    private fun addConstraint(
        element: CoordsElement,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult {
        val point = element as? Point
            ?: return GMResult.Err(
                JessieCodeRuntimeError.ElementMethodUnavailable(
                    elementId = element.id,
                    method = "addConstraint",
                    reason = "only Point constraints are translated",
                    location = location,
                ),
            )
        val values = (
            arguments.firstOrNull() as?
                JessieCodeRuntimeValue.ArrayValue
            )?.values ?: return invalidArgumentType(
            functionName = "addConstraint",
            argumentIndex = 0,
            expected = "array",
            actual = arguments.firstOrNull()
                ?: JessieCodeRuntimeValue.UndefinedValue,
            location = location,
        )
        if (values.size < 2) {
            return GMResult.Err(
                JessieCodeRuntimeError.InvalidArgumentCount(
                    functionName = "addConstraint coordinates",
                    expected = "at least 2",
                    actual = values.size,
                    location = location,
                ),
            )
        }
        val sources = mutableListOf<String>()
        for ((index, value) in values.withIndex()) {
            when (value) {
                is JessieCodeRuntimeValue.NumberValue ->
                    sources += JsNumberFormat.compact(value.value)
                is JessieCodeRuntimeValue.StringValue ->
                    sources += value.value
                else -> return invalidArgumentType(
                    functionName = "addConstraint",
                    argumentIndex = index,
                    expected = "number or string constraint term",
                    actual = value,
                    location = location,
                )
            }
        }
        return when (val result = point.replaceCoordinateConstraints(sources)) {
            is GMResult.Ok -> GMResult.Ok(
                JessieCodeRuntimeValue.ElementReference(point),
            )
            is GMResult.Err -> GMResult.Err(
                JessieCodeRuntimeError.ElementCoordinateConstraintFailure(
                    elementId = point.id,
                    property = "addConstraint",
                    error = result.error,
                    location = location,
                ),
            )
        }
    }

    private fun coordinateArray(
        functionName: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<DoubleArray, JessieCodeRuntimeError> {
        val values = (value as? JessieCodeRuntimeValue.ArrayValue)?.values
            ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = 0,
                expected = "coordinate array",
                actual = value,
                location = location,
            )
        if (values.size !in 2..3) {
            return GMResult.Err(
                JessieCodeRuntimeError.InvalidArgumentCount(
                    functionName = "$functionName coordinate array",
                    expected = "2 or 3",
                    actual = values.size,
                    location = location,
                ),
            )
        }
        val coordinates = DoubleArray(values.size)
        for ((index, coordinate) in values.withIndex()) {
            val number = (
                coordinate as? JessieCodeRuntimeValue.NumberValue
                )?.value ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = index,
                expected = "number coordinate",
                actual = coordinate,
                location = location,
            )
            coordinates[index] = number
        }
        return GMResult.Ok(coordinates)
    }

    private fun invalidArgumentType(
        functionName: String,
        argumentIndex: Int,
        expected: String,
        actual: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult.Err<JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.InvalidArgumentType(
                functionName = functionName,
                argumentIndex = argumentIndex,
                expected = expected,
                actual = typeName(actual),
                location = location,
            ),
        )

    private fun bounds(element: GeometryElement): DoubleArray =
        when (element) {
            is Circle -> element.bounds()
            is Line -> element.bounds()
            is Point -> element.bounds()
            is Polygon -> element.bounds()
            else -> doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        }

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
            is JessieCodeRuntimeValue.TransformationReference ->
                "transformation"
            is JessieCodeRuntimeValue.CompositionReference ->
                "composition"
            is JessieCodeRuntimeValue.ElementReference -> "element"
        }
}

private typealias ElementPropertyResult =
    GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>
