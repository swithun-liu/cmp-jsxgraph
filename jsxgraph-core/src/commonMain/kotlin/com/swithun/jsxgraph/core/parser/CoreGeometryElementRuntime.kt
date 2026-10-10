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
import com.swithun.jsxgraph.core.base.AngleRadius
import com.swithun.jsxgraph.core.base.AngleRadiusFunction
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.CircleRadiusFunction
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.CoordsElementAnimationError
import com.swithun.jsxgraph.core.base.CoordsElementMoveOptions
import com.swithun.jsxgraph.core.base.CoordsElementPathFunction
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.CurveGeneratedTerm
import com.swithun.jsxgraph.core.base.ForeignObject
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.GeometryElementEvaluationError
import com.swithun.jsxgraph.core.base.GeometryElementPropertyResultEvaluator
import com.swithun.jsxgraph.core.base.Glider
import com.swithun.jsxgraph.core.base.HtmlControlDefinition
import com.swithun.jsxgraph.core.base.Image
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Polyhedron3D
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.Slider
import com.swithun.jsxgraph.core.base.Sphere3D
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.base.Ticks
import com.swithun.jsxgraph.core.base.Transformation
import com.swithun.jsxgraph.core.base.TransformationDynamicParameter
import com.swithun.jsxgraph.core.base.TransformationDynamicParameterError
import com.swithun.jsxgraph.core.base.Turtle
import com.swithun.jsxgraph.core.base.TurtleError
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.utils.EventHandler
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.abs

/**
 * The methodMap subset backed by geometry classes translated so far.
 */
internal object CoreGeometryElementRuntime : JessieCodeElementRuntime {
    private data class ElementEventHandlerKey(
        val element: GeometryElement,
        val event: String,
        val function: JessieCodeRuntimeValue.FunctionValue,
    )

    private class ElementEventHandlerState(
        val handler: EventHandler,
        var error: JessieCodeRuntimeError? = null,
    )

    private val elementEventHandlers =
        mutableMapOf<ElementEventHandlerKey, ElementEventHandlerState>()

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
        resolveSphere3DProperty(element, property, location)?.let {
            return it
        }
        resolvePolyhedron3DProperty(element, property, location)?.let {
            return it
        }
        resolveGeometryProperty(element, property, location)?.let {
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
        resolveCurveProperty(element, property, location)?.let {
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
        resolveVisualProperty(element, property)?.let {
            return it
        }
        return unavailable(element, property, location)
    }

    // JSXGraph 1.13.3: src/3d/sphere3d.js -> methodMap.
    private fun resolveSphere3DProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val sphere = element as? Sphere3D ?: return null
        return when (property) {
            "center" -> elementReference(sphere.center)
            "point2" -> sphere.point2?.let(::elementReference)
                ?: unavailable(sphere, property, location)
            "Radius" -> numberFunction("Radius", sphere::Radius)
            else -> null
        }
    }

    // JSXGraph 1.13.3: src/3d/polyhedron3d.js -> methodMap.
    private fun resolvePolyhedron3DProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val polyhedron = element as? Polyhedron3D ?: return null
        return when (property) {
            "setParents" -> function("setParents") {
                    arguments,
                    callLocation,
                ->
                val values = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ArrayValue
                    )?.values ?: arguments
                val parents = mutableListOf<GeometryElement>()
                for ((index, value) in values.withIndex()) {
                    val parent = (
                        value as?
                            JessieCodeRuntimeValue.ElementReference
                        )?.element ?: return@function invalidArgumentType(
                        functionName = "setParents",
                        argumentIndex = index,
                        expected = "element",
                        actual = value,
                        location = callLocation,
                    )
                    parents += parent
                }
                polyhedron.setParents(parents)
                elementReference(polyhedron)
            }
            "addTransform", "addTransformation", "trans" ->
                function("addTransform") {
                    arguments,
                    callLocation,
                ->
                val source = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ElementReference
                    )?.element as? Polyhedron3D
                    ?: return@function invalidArgumentType(
                        functionName = "addTransform",
                        argumentIndex = 0,
                        expected = "polyhedron3d",
                        actual = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                val transformations = when (
                    val result = elementTransformations(
                        functionName = "addTransform",
                        value = arguments.getOrNull(1)
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@function result
                }
                polyhedronMutation(
                    polyhedron = polyhedron,
                    method = "addTransform",
                    location = callLocation,
                    result = polyhedron.addTransform(
                        element = source,
                        transformations = transformations,
                    ),
                )
            }
            "removeTransform" -> function("removeTransform") {
                    arguments,
                    callLocation,
                ->
                val transformations = when (
                    val result = elementTransformations(
                        functionName = "removeTransform",
                        value = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@function result
                }
                polyhedronMutation(
                    polyhedron = polyhedron,
                    method = "removeTransform",
                    location = callLocation,
                    result =
                        polyhedron.removeTransform(transformations),
                )
            }
            "clearTransforms" -> function("clearTransforms") {
                    _,
                    callLocation,
                ->
                polyhedronMutation(
                    polyhedron = polyhedron,
                    method = "clearTransforms",
                    location = callLocation,
                    result = polyhedron.clearTransforms(),
                )
            }
            else -> null
        }
    }

    private fun polyhedronMutation(
        polyhedron: Polyhedron3D,
        method: String,
        location: JessieCodeAstLocation,
        result: GMResult<Polyhedron3D, *>,
    ): ElementPropertyResult =
        when (result) {
            is GMResult.Ok -> elementReference(polyhedron)
            is GMResult.Err -> GMResult.Err(
                JessieCodeRuntimeError.ElementMethodUnavailable(
                    elementId = polyhedron.id,
                    method = method,
                    reason = result.error.toString(),
                    location = location,
                ),
            )
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
        location: JessieCodeAstLocation,
    ): ElementPropertyResult? {
        val curve = element as? Curve ?: return null
        when (property) {
            "minorGrid" -> return curve.minorGrid
                ?.let(::elementReference)
                ?: unavailable(curve, property, location)
            "majorGrid" -> return curve.majorGrid
                ?.let(::elementReference)
                ?: unavailable(curve, property, location)
            "MinX" -> return numberFunction("minX", curve::minX)
            "MaxX" -> return numberFunction("maxX", curve::maxX)
            "move", "moveTo" -> return function("moveTo") {
                    arguments,
                    callLocation,
                ->
                val target = when (
                    val result = coordinateArray(
                        functionName = "moveTo",
                        value = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@function result
                }
                when (val result = curve.moveTo(target)) {
                    is GMResult.Ok -> elementReference(curve)
                    is GMResult.Err -> GMResult.Err(
                        JessieCodeRuntimeError.ElementMethodUnavailable(
                            elementId = curve.id,
                            method = "moveTo",
                            reason = result.error.toString(),
                            location = callLocation,
                        ),
                    )
                }
            }
            "generateTerm", "setTerm" ->
                return function("generateTerm") {
                        arguments,
                        callLocation,
                    ->
                    curveGenerateTerm(
                        curve = curve,
                        arguments = arguments,
                        location = callLocation,
                    )
                }
            "setF" -> {
                if (!curve.isVectorField) {
                    return null
                }
                return function("setF") { arguments, callLocation ->
                    val value = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue
                    val variableNames = when (
                        val names = arguments.getOrNull(1)
                            ?: JessieCodeRuntimeValue.UndefinedValue
                    ) {
                        JessieCodeRuntimeValue.UndefinedValue -> emptyList()
                        is JessieCodeRuntimeValue.StringValue ->
                            names.value
                                .split(',')
                                .map(String::trim)
                                .filter(String::isNotEmpty)
                        else -> return@function invalidArgumentType(
                            functionName = "setF",
                            argumentIndex = 1,
                            expected = "string",
                            actual = names,
                            location = callLocation,
                        )
                    }
                    val field = when (
                        val result =
                            if (curve.elType == "slopefield") {
                                NativeJessieCodeCreators.slopeFieldFunction(
                                    board = curve.board,
                                    value = value,
                                    location = callLocation,
                                    variableNames = variableNames,
                                )
                            } else {
                                NativeJessieCodeCreators.vectorFieldFunction(
                                    board = curve.board,
                                    value = value,
                                    location = callLocation,
                                    variableNames = variableNames,
                                )
                            }
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@function GMResult.Err(
                            JessieCodeRuntimeError.ElementMethodUnavailable(
                                elementId = curve.id,
                                method = "setF",
                                reason = result.error.toString(),
                                location = callLocation,
                            ),
                        )
                    }
                    when (val result = curve.setF(field)) {
                        is GMResult.Ok ->
                            if (curve.elType == "slopefield") {
                                GMResult.Ok(
                                    JessieCodeRuntimeValue.UndefinedValue,
                                )
                            } else {
                                elementReference(curve)
                            }
                        is GMResult.Err -> GMResult.Err(
                            JessieCodeRuntimeError.ElementMethodUnavailable(
                                elementId = curve.id,
                                method = "setF",
                                reason = result.error.toString(),
                                location = callLocation,
                            ),
                        )
                    }
                }
            }
        }
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

    private fun curveGenerateTerm(
        curve: Curve,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult {
        val variableName = when (
            val value = arguments.firstOrNull()
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> "x"
            is JessieCodeRuntimeValue.StringValue -> value.value
            else -> return invalidArgumentType(
                functionName = "generateTerm",
                argumentIndex = 0,
                expected = "string",
                actual = value,
                location = location,
            )
        }
        val xValue = arguments.getOrNull(1)
            ?: JessieCodeRuntimeValue.UndefinedValue
        val yValue = arguments.getOrNull(2)
            ?: JessieCodeRuntimeValue.UndefinedValue
        val minimumTerm = when (
            val result = optionalCurveTerm(
                curve = curve,
                termName = "minX",
                argumentIndex = 3,
                value = arguments.getOrNull(3)
                    ?: JessieCodeRuntimeValue.UndefinedValue,
                variableNames = emptyList(),
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val maximumTerm = when (
            val result = optionalCurveTerm(
                curve = curve,
                termName = "maxX",
                argumentIndex = 4,
                value = arguments.getOrNull(4)
                    ?: JessieCodeRuntimeValue.UndefinedValue,
                variableNames = emptyList(),
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val generatedTerm = when (xValue) {
            is JessieCodeRuntimeValue.ArrayValue ->
                when (
                    val result = curveDataGeneratedTerm(
                        curve = curve,
                        xValue = xValue,
                        yValue = yValue,
                        variableName = variableName,
                        minimumTerm = minimumTerm,
                        maximumTerm = maximumTerm,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            else -> {
                val xTerm = when (
                    val result = curveTerm(
                        curve = curve,
                        termName = "xterm",
                        argumentIndex = 1,
                        value = xValue,
                        variableNames = listOf(variableName),
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (
                    xValue is JessieCodeRuntimeValue.FunctionValue &&
                    yValue is JessieCodeRuntimeValue.ArrayValue
                ) {
                    if (yValue.values.size < 2) {
                        return invalidArgumentType(
                            functionName = "generateTerm",
                            argumentIndex = 2,
                            expected = "two polar offset terms",
                            actual = yValue,
                            location = location,
                        )
                    }
                    val yTerms = when (
                        val result = curveTerms(
                            curve = curve,
                            termName = "yterm",
                            argumentIndex = 2,
                            values = yValue.values,
                            variableNames = emptyList(),
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    CurveGeneratedTerm.Polar(
                        radiusTerm = xTerm,
                        xOffsetTerm = yTerms[0],
                        yOffsetTerm = yTerms[1],
                        yTerms = yTerms,
                        minimumTerm = minimumTerm,
                        maximumTerm = maximumTerm,
                    )
                } else {
                    val yTerms =
                        (yValue as? JessieCodeRuntimeValue.ArrayValue)
                            ?.let { array ->
                                when (
                                    val result = curveTerms(
                                        curve = curve,
                                        termName = "yterm",
                                        argumentIndex = 2,
                                        values = array.values,
                                        variableNames = emptyList(),
                                        location = location,
                                    )
                                ) {
                                    is GMResult.Ok -> result.value
                                    is GMResult.Err -> return result
                                }
                            }
                    val yTerm =
                        if (yTerms == null) {
                            when (
                                val result = curveTerm(
                                    curve = curve,
                                    termName = "yterm",
                                    argumentIndex = 2,
                                    value = yValue,
                                    variableNames = listOf(variableName),
                                    location = location,
                                )
                            ) {
                                is GMResult.Ok -> result.value
                                is GMResult.Err -> return result
                            }
                        } else {
                            null
                        }
                    CurveGeneratedTerm.Continuous(
                        xTerm = xTerm,
                        yTerm = yTerm,
                        yTerms = yTerms,
                        functionGraph =
                            xValue is JessieCodeRuntimeValue.StringValue,
                        identityXTerm =
                            (xValue as? JessieCodeRuntimeValue.StringValue)
                                ?.value == variableName,
                        minimumTerm = minimumTerm,
                        maximumTerm = maximumTerm,
                    )
                }
            }
        }
        curve.generateTerm(generatedTerm)
        curve.board.update()
        return GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
    }

    private fun curveDataGeneratedTerm(
        curve: Curve,
        xValue: JessieCodeRuntimeValue.ArrayValue,
        yValue: JessieCodeRuntimeValue,
        variableName: String,
        minimumTerm: JessieCodeCoordinateFunction?,
        maximumTerm: JessieCodeCoordinateFunction?,
        location: JessieCodeAstLocation,
    ): GMResult<CurveGeneratedTerm.Data, JessieCodeRuntimeError> {
        val xTerms = mutableListOf<JessieCodeCoordinateFunction>()
        val yTerms = mutableListOf<JessieCodeCoordinateFunction>()
        var continuousYTerm: JessieCodeCoordinateFunction? = null
        if (yValue === JessieCodeRuntimeValue.UndefinedValue) {
            for ((index, pairValue) in xValue.values.withIndex()) {
                val pair = pairValue as?
                    JessieCodeRuntimeValue.ArrayValue
                    ?: return invalidArgumentType(
                        functionName = "generateTerm",
                        argumentIndex = index,
                        expected = "coordinate pair",
                        actual = pairValue,
                        location = location,
                    )
                if (pair.values.size !in 2..3) {
                    return invalidArgumentType(
                        functionName = "generateTerm",
                        argumentIndex = index,
                        expected = "coordinate pair",
                        actual = pairValue,
                        location = location,
                    )
                }
                val offset = if (pair.values.size == 3) 1 else 0
                when (
                    val result = curveTerm(
                        curve = curve,
                        termName = "xterm[$index]",
                        argumentIndex = index,
                        value = pair.values[offset],
                        variableNames = emptyList(),
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> xTerms += result.value
                    is GMResult.Err -> return result
                }
                when (
                    val result = curveTerm(
                        curve = curve,
                        termName = "yterm[$index]",
                        argumentIndex = index,
                        value = pair.values[offset + 1],
                        variableNames = emptyList(),
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> yTerms += result.value
                    is GMResult.Err -> return result
                }
            }
        } else {
            for ((index, value) in xValue.values.withIndex()) {
                when (
                    val result = curveTerm(
                        curve = curve,
                        termName = "xterm[$index]",
                        argumentIndex = index,
                        value = value,
                        variableNames = emptyList(),
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> xTerms += result.value
                    is GMResult.Err -> return result
                }
            }
            if (yValue is JessieCodeRuntimeValue.ArrayValue) {
                for ((index, value) in yValue.values.withIndex()) {
                    when (
                        val result = curveTerm(
                            curve = curve,
                            termName = "yterm[$index]",
                            argumentIndex = index,
                            value = value,
                            variableNames = emptyList(),
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> yTerms += result.value
                        is GMResult.Err -> return result
                    }
                }
            } else {
                continuousYTerm = when (
                    val result = curveTerm(
                        curve = curve,
                        termName = "yterm",
                        argumentIndex = 2,
                        value = yValue,
                        variableNames = listOf(variableName),
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }
        }
        return GMResult.Ok(
            CurveGeneratedTerm.Data(
                xTerms = xTerms,
                yTerms = yTerms.takeIf { it.isNotEmpty() },
                continuousYTerm = continuousYTerm,
                minimumTerm = minimumTerm,
                maximumTerm = maximumTerm,
            ),
        )
    }

    private fun optionalCurveTerm(
        curve: Curve,
        termName: String,
        argumentIndex: Int,
        value: JessieCodeRuntimeValue,
        variableNames: List<String>,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeCoordinateFunction?, JessieCodeRuntimeError> =
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            GMResult.Ok(null)
        } else {
            curveTerm(
                curve = curve,
                termName = termName,
                argumentIndex = argumentIndex,
                value = value,
                variableNames = variableNames,
                location = location,
            )
        }

    private fun curveTerms(
        curve: Curve,
        termName: String,
        argumentIndex: Int,
        values: List<JessieCodeRuntimeValue>,
        variableNames: List<String>,
        location: JessieCodeAstLocation,
    ): GMResult<List<JessieCodeCoordinateFunction>, JessieCodeRuntimeError> {
        val terms = mutableListOf<JessieCodeCoordinateFunction>()
        for ((index, value) in values.withIndex()) {
            when (
                val result = curveTerm(
                    curve = curve,
                    termName = "$termName[$index]",
                    argumentIndex = argumentIndex,
                    value = value,
                    variableNames = variableNames,
                    location = location,
                )
            ) {
                is GMResult.Ok -> terms += result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(terms)
    }

    private fun curveTerm(
        curve: Curve,
        termName: String,
        argumentIndex: Int,
        value: JessieCodeRuntimeValue,
        variableNames: List<String>,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeCoordinateFunction, JessieCodeRuntimeError> =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                GMResult.Ok(
                    JessieCodeNumericCoordinateFunction(value.value),
                )
            is JessieCodeRuntimeValue.StringValue ->
                when (
                    val result = JessieCodeExpressionFunction.compile(
                        source = value.value,
                        board = curve.board,
                        variableNames = variableNames,
                    )
                ) {
                    is GMResult.Ok -> result
                    is GMResult.Err -> GMResult.Err(
                        JessieCodeRuntimeError.ElementMethodUnavailable(
                            elementId = curve.id,
                            method = "generateTerm",
                            reason = "$termName: ${result.error}",
                            location = location,
                        ),
                    )
                }
            is JessieCodeRuntimeValue.FunctionValue ->
                GMResult.Ok(
                    JessieCodeRuntimeCoordinateFunction(
                        function = value,
                        location = location,
                        returnsCoordinateArray = false,
                    ),
                )
            else -> invalidArgumentType(
                functionName = "generateTerm",
                argumentIndex = argumentIndex,
                expected = "number, string, or function",
                actual = value,
                location = location,
            )
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
            "L", "Length" -> numberFunction("L") {
                when (element) {
                    is Arc -> element.L()
                    is Sector -> element.L()
                    else -> Double.NaN
                }
            }
            "A", "Area" ->
                if (element is Sector) {
                    numberFunction("Area", element::Area)
                } else {
                    unavailable(element, property, location)
                }
            "Perimeter" ->
                if (element is Sector) {
                    numberFunction("Perimeter", element::Perimeter)
                } else {
                    unavailable(element, property, location)
                }
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
            "setRadius" ->
                if (element is Sector) {
                    function("setRadius") { arguments, callLocation ->
                        val result = when (
                            val value = arguments.firstOrNull()
                                ?: JessieCodeRuntimeValue.UndefinedValue
                        ) {
                            is JessieCodeRuntimeValue.NumberValue ->
                                element.setRadius(value.value)
                            is JessieCodeRuntimeValue.StringValue ->
                                if (value.value.lowercase() == "auto") {
                                    element.setRadiusAuto()
                                } else {
                                    return@function invalidArgumentType(
                                        functionName = "setRadius",
                                        argumentIndex = 0,
                                        expected = "number, function, or auto",
                                        actual = value,
                                        location = callLocation,
                                    )
                                }
                            is JessieCodeRuntimeValue.FunctionValue ->
                                element.setRadius(
                                    AngleRadius.Function(
                                        AngleRadiusFunction(
                                            function = value,
                                            location = callLocation,
                                        ),
                                    ),
                                )
                            else -> return@function invalidArgumentType(
                                functionName = "setRadius",
                                argumentIndex = 0,
                                expected = "number, function, or auto",
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
                                        method = "setRadius",
                                        reason = result.error.toString(),
                                        location = callLocation,
                                    ),
                            )
                        }
                    }
                } else {
                    unavailable(element, property, location)
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
            else -> null
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
        val attributeValue = when (
            val result = runtimeAttributeValue(
                value = value,
                elementId = element.id,
                property = property,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        element.setAttribute(mapOf(property to attributeValue))
        updateAfterAttributeMutation(element)
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/parser/jessiescript.js ->
    // Board.construct property mutation; src/base/element.js -> setAttribute.
    private fun runtimeAttributeValue(
        value: JessieCodeRuntimeValue,
        elementId: String,
        property: String,
        location: JessieCodeAstLocation,
        depth: Int = 0,
        allowFunction: Boolean = true,
    ): GMResult<Any?, JessieCodeRuntimeError> {
        if (depth > MAX_ATTRIBUTE_VALUE_DEPTH) {
            return GMResult.Err(
                JessieCodeRuntimeError.InvalidElementPropertyValue(
                    elementId = elementId,
                    property = property,
                    expected = "an attribute value with bounded depth",
                    actual = "nested value",
                    location = location,
                ),
            )
        }
        return when (value) {
            JessieCodeRuntimeValue.UndefinedValue,
            JessieCodeRuntimeValue.NullValue,
            -> GMResult.Ok(null)
            is JessieCodeRuntimeValue.NumberValue ->
                GMResult.Ok(value.value)
            is JessieCodeRuntimeValue.BooleanValue ->
                GMResult.Ok(value.value)
            is JessieCodeRuntimeValue.StringValue ->
                GMResult.Ok(value.value)
            is JessieCodeRuntimeValue.ElementReference ->
                GMResult.Ok(value.element)
            is JessieCodeRuntimeValue.BoardReference ->
                GMResult.Ok(value.board)
            is JessieCodeRuntimeValue.TransformationReference ->
                GMResult.Ok(value.transformation)
            is JessieCodeRuntimeValue.CompositionReference ->
                GMResult.Ok(value.composition)
            is JessieCodeRuntimeValue.FunctionValue -> if (!allowFunction) {
                GMResult.Err(
                    JessieCodeRuntimeError.InvalidElementPropertyValue(
                        elementId = elementId,
                        property = property,
                        expected = "a portable non-function result",
                        actual = "function",
                        location = location,
                    ),
                )
            } else {
                GMResult.Ok(
                    GeometryElementPropertyResultEvaluator { element ->
                        when (
                            val evaluated = value.externalCallable.call(
                                arguments = listOf(
                                    JessieCodeRuntimeValue.ElementReference(
                                        element,
                                    ),
                                ),
                                location = location,
                            )
                        ) {
                            is GMResult.Err -> GMResult.Err(
                                GeometryElementEvaluationError.CallbackFailed(
                                    evaluated.error.toString(),
                                ),
                            )
                            is GMResult.Ok -> when (
                                val converted = runtimeAttributeValue(
                                    value = evaluated.value,
                                    elementId = elementId,
                                    property = property,
                                    location = location,
                                    allowFunction = false,
                                )
                            ) {
                                is GMResult.Ok -> converted
                                is GMResult.Err -> GMResult.Err(
                                    GeometryElementEvaluationError
                                        .CallbackFailed(
                                            converted.error.toString(),
                                        ),
                                )
                            }
                        }
                    },
                )
            }
            is JessieCodeRuntimeValue.ArrayValue -> {
                val values = mutableListOf<Any?>()
                for (child in value.values) {
                    when (
                        val result = runtimeAttributeValue(
                            value = child,
                            elementId = elementId,
                            property = property,
                            location = location,
                            depth = depth + 1,
                            allowFunction = allowFunction,
                        )
                    ) {
                        is GMResult.Ok -> values += result.value
                        is GMResult.Err -> return result
                    }
                }
                GMResult.Ok(values)
            }
            is JessieCodeRuntimeValue.ObjectValue -> {
                val values = linkedMapOf<String, Any?>()
                for ((name, child) in value.properties) {
                    when (
                        val result = runtimeAttributeValue(
                            value = child,
                            elementId = elementId,
                            property = "$property.$name",
                            location = location,
                            depth = depth + 1,
                            allowFunction = allowFunction,
                        )
                    ) {
                        is GMResult.Ok -> values[name] = result.value
                        is GMResult.Err -> return result
                    }
                }
                GMResult.Ok(values)
            }
        }
    }

    private fun resolveGeometryProperty(
        element: GeometryElement,
        property: String,
        location: JessieCodeAstLocation,
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
            "label" -> element.elementLabel?.let(::elementReference)
            "setLabel" -> function("setLabel") {
                    arguments,
                    callLocation,
                ->
                val label = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.StringValue
                    )?.value ?: return@function invalidArgumentType(
                    functionName = "setLabel",
                    argumentIndex = 0,
                    expected = "string",
                    actual = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue,
                    location = callLocation,
                )
                element.setLabel(label)
                GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
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
            "addTransform" -> function("addTransform") {
                    arguments,
                    callLocation,
                ->
                val transformations = when (
                    val result = elementTransformations(
                        functionName = "addTransform",
                        value = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@function result
                }
                when (element) {
                    is Circle -> element.addTransform(transformations)
                    is Line -> element.addTransform(transformations)
                    is Curve -> element.addTransform(transformations)
                    is CoordsElement ->
                        element.addTransform(element, transformations)
                    else -> transformations.forEach(element::addTransformBase)
                }
                elementReference(element)
                }
            "removeTransform" -> function("removeTransform") {
                    arguments,
                    callLocation,
                ->
                val transformations = when (
                    val result = elementTransformations(
                        functionName = "removeTransform",
                        value = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@function result
                }
                when (element) {
                    is Circle -> element.removeTransform(transformations)
                    is Line -> element.removeTransform(transformations)
                    is Curve -> element.removeTransform(transformations)
                    is CoordsElement ->
                        element.removeTransform(transformations)
                    else ->
                        transformations.forEach(element::removeTransformBase)
                }
                elementReference(element)
            }
            "clearTransforms" -> function("clearTransforms") { _, _ ->
                when (element) {
                    is Circle -> element.clearTransforms()
                    is Line -> element.clearTransforms()
                    is Curve -> element.clearTransforms()
                    is CoordsElement -> element.clearTransforms()
                    else -> element.clearTransformsBase()
                }
                elementReference(element)
            }
            "setProperty", "setAttribute" ->
                function("setAttribute") { arguments, callLocation ->
                    val attributes = when (
                        val result = elementAttributeArguments(
                            element = element,
                            arguments = arguments,
                            functionName = "setAttribute",
                            location = callLocation,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@function result
                    }
                    element.setAttribute(attributes)
                    updateAfterAttributeMutation(element)
                    elementReference(element)
                }
            "animate" -> function("animate") { arguments, callLocation ->
                val attributes = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ObjectValue
                    ) ?: return@function invalidArgumentType(
                    functionName = "animate",
                    argumentIndex = 0,
                    expected = "object",
                    actual = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue,
                    location = callLocation,
                )
                val targetAttributes = when (
                    val result = elementAttributeObject(
                        element = element,
                        value = attributes,
                        property = "animate",
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@function result
                }
                val duration = (
                    arguments.getOrNull(1) as?
                        JessieCodeRuntimeValue.NumberValue
                    )?.value ?: return@function invalidArgumentType(
                    functionName = "animate",
                    argumentIndex = 1,
                    expected = "number",
                    actual = arguments.getOrNull(1)
                        ?: JessieCodeRuntimeValue.UndefinedValue,
                    location = callLocation,
                )
                val callback = when (
                    val value = arguments.getOrNull(2)
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    JessieCodeRuntimeValue.UndefinedValue -> null
                    is JessieCodeRuntimeValue.ObjectValue -> {
                        when (
                            val raw = value.properties["callback"]
                                ?: JessieCodeRuntimeValue.UndefinedValue
                        ) {
                            JessieCodeRuntimeValue.UndefinedValue -> null
                            is JessieCodeRuntimeValue.FunctionValue -> {
                                {
                                    raw.externalCallable.call(
                                        arguments = emptyList(),
                                        location = callLocation,
                                    )
                                    Unit
                                }
                            }
                            else -> return@function invalidArgumentType(
                                functionName = "animate",
                                argumentIndex = 2,
                                expected =
                                    "options.callback function",
                                actual = raw,
                                location = callLocation,
                            )
                        }
                    }
                    else -> return@function invalidArgumentType(
                        functionName = "animate",
                        argumentIndex = 2,
                        expected = "object or undefined",
                        actual = value,
                        location = callLocation,
                    )
                }
                when (
                    val result = element.animateResult(
                        targetAttributes = targetAttributes,
                        timeMilliseconds = duration,
                        options =
                            com.swithun.jsxgraph.core.base
                                .GeometryElementAnimationOptions(
                                    callback = callback,
                                ),
                    )
                ) {
                    is GMResult.Ok -> elementReference(element)
                    is GMResult.Err -> GMResult.Err(
                        JessieCodeRuntimeError.ElementMethodUnavailable(
                            elementId = element.id,
                            method = "animate",
                            reason = result.error.toString(),
                            location = callLocation,
                        ),
                    )
                }
            }
            "on" -> function("on") { arguments, callLocation ->
                elementOn(element, arguments, callLocation)
            }
            "off" -> function("off") { arguments, callLocation ->
                elementOff(element, arguments, callLocation)
            }
            "trigger" -> function("trigger") {
                    arguments,
                    callLocation,
                ->
                elementTrigger(element, arguments, callLocation)
            }
            "addTicks" -> function("addTicks") {
                    arguments,
                    callLocation,
                ->
                val ticks = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ElementReference
                    )?.element as? Ticks
                    ?: return@function invalidArgumentType(
                        functionName = "addTicks",
                        argumentIndex = 0,
                        expected = "ticks",
                        actual = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                GMResult.Ok(
                    JessieCodeRuntimeValue.StringValue(
                        element.addTicks(ticks),
                    ),
                )
            }
            "removeTicks" -> function("removeTicks") {
                    arguments,
                    callLocation,
                ->
                val ticks = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ElementReference
                    )?.element as? Ticks
                    ?: return@function invalidArgumentType(
                        functionName = "removeTicks",
                        argumentIndex = 0,
                        expected = "ticks",
                        actual = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                elementReference(element.removeTicks(ticks))
            }
            "removeAllTicks" -> function("removeAllTicks") { _, _ ->
                elementReference(element.removeAllTicks())
            }
            else -> null
        }

    private fun elementTransformations(
        functionName: String,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<List<Transformation>, JessieCodeRuntimeError> {
        val values = if (value is JessieCodeRuntimeValue.ArrayValue) {
            value.values
        } else {
            listOf(value)
        }
        val transformations = mutableListOf<Transformation>()
        for ((index, child) in values.withIndex()) {
            val transformation = (
                child as?
                    JessieCodeRuntimeValue.TransformationReference
                )?.transformation ?: return invalidArgumentType(
                functionName = functionName,
                argumentIndex = index,
                expected = "transformation",
                actual = child,
                location = location,
            )
            transformations += transformation
        }
        return GMResult.Ok(transformations)
    }

    private fun elementAttributeArguments(
        element: GeometryElement,
        arguments: List<JessieCodeRuntimeValue>,
        functionName: String,
        location: JessieCodeAstLocation,
    ): GMResult<Map<String, Any?>, JessieCodeRuntimeError> {
        val attributes = linkedMapOf<String, Any?>()
        for ((index, argument) in arguments.withIndex()) {
            when (argument) {
                is JessieCodeRuntimeValue.ObjectValue -> {
                    when (
                        val result = elementAttributeObject(
                            element = element,
                            value = argument,
                            property = functionName,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> attributes.putAll(result.value)
                        is GMResult.Err -> return result
                    }
                }
                is JessieCodeRuntimeValue.StringValue -> {
                    val separator = argument.value.indexOf(':')
                    if (separator < 0) {
                        return invalidArgumentType(
                            functionName = functionName,
                            argumentIndex = index,
                            expected = "key:value string",
                            actual = argument,
                            location = location,
                        )
                    }
                    attributes[
                        argument.value.substring(0, separator).trim()
                    ] = argument.value.substring(separator + 1).trim()
                }
                is JessieCodeRuntimeValue.ArrayValue -> {
                    if (
                        argument.values.size != 2 ||
                        argument.values[0] !is
                            JessieCodeRuntimeValue.StringValue
                    ) {
                        return invalidArgumentType(
                            functionName = functionName,
                            argumentIndex = index,
                            expected = "[string, value] pair",
                            actual = argument,
                            location = location,
                        )
                    }
                    val key = (
                        argument.values[0] as
                            JessieCodeRuntimeValue.StringValue
                        ).value
                    when (
                        val result = runtimeAttributeValue(
                            value = argument.values[1],
                            elementId = element.id,
                            property = key,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> attributes[key] = result.value
                        is GMResult.Err -> return result
                    }
                }
                else -> return invalidArgumentType(
                    functionName = functionName,
                    argumentIndex = index,
                    expected = "object, key:value string, or pair",
                    actual = argument,
                    location = location,
                )
            }
        }
        return GMResult.Ok(attributes)
    }

    // JSXGraph 1.13.3: src/base/element.js -> setAttribute.
    private fun updateAfterAttributeMutation(
        element: GeometryElement,
    ) {
        if (element.needsRegularUpdate) {
            element.board.update(element)
        } else {
            element.board.fullUpdate()
        }
    }

    private fun elementAttributeObject(
        element: GeometryElement,
        value: JessieCodeRuntimeValue.ObjectValue,
        property: String,
        location: JessieCodeAstLocation,
    ): GMResult<Map<String, Any?>, JessieCodeRuntimeError> {
        val attributes = linkedMapOf<String, Any?>()
        for ((key, child) in value.properties) {
            when (
                val result = runtimeAttributeValue(
                    value = child,
                    elementId = element.id,
                    property = "$property.$key",
                    location = location,
                )
            ) {
                is GMResult.Ok -> attributes[key] = result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(attributes)
    }

    private fun elementOn(
        element: GeometryElement,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult {
        val event = (
            arguments.firstOrNull() as?
                JessieCodeRuntimeValue.StringValue
            )?.value ?: return invalidArgumentType(
            functionName = "on",
            argumentIndex = 0,
            expected = "string",
            actual = arguments.firstOrNull()
                ?: JessieCodeRuntimeValue.UndefinedValue,
            location = location,
        )
        val callback = arguments.getOrNull(1) as?
            JessieCodeRuntimeValue.FunctionValue
            ?: return invalidArgumentType(
                functionName = "on",
                argumentIndex = 1,
                expected = "function",
                actual = arguments.getOrNull(1)
                    ?: JessieCodeRuntimeValue.UndefinedValue,
                location = location,
            )
        val key = ElementEventHandlerKey(element, event, callback)
        val state = elementEventHandlers.getOrPut(key) {
            lateinit var handlerState: ElementEventHandlerState
            val handler = EventHandler { _, nativeArguments ->
                val result = callback.externalCallable.call(
                    arguments = nativeArguments.map(::runtimeValue),
                    location = location,
                )
                handlerState.error = when (result) {
                    is GMResult.Ok -> null
                    is GMResult.Err -> result.error
                }
            }
            ElementEventHandlerState(handler).also {
                handlerState = it
            }
        }
        element.on(event, state.handler)
        return elementReference(element)
    }

    private fun elementOff(
        element: GeometryElement,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult {
        val event = (
            arguments.firstOrNull() as?
                JessieCodeRuntimeValue.StringValue
            )?.value ?: return invalidArgumentType(
            functionName = "off",
            argumentIndex = 0,
            expected = "string",
            actual = arguments.firstOrNull()
                ?: JessieCodeRuntimeValue.UndefinedValue,
            location = location,
        )
        val callback = when (
            val value = arguments.getOrNull(1)
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> null
            is JessieCodeRuntimeValue.FunctionValue -> value
            else -> return invalidArgumentType(
                functionName = "off",
                argumentIndex = 1,
                expected = "function or undefined",
                actual = value,
                location = location,
            )
        }
        if (callback == null) {
            element.off(event)
            elementEventHandlers.keys.removeAll {
                it.element === element && it.event == event
            }
        } else {
            val key = ElementEventHandlerKey(element, event, callback)
            elementEventHandlers.remove(key)?.let {
                element.off(event, it.handler)
            }
        }
        return elementReference(element)
    }

    private fun elementTrigger(
        element: GeometryElement,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult {
        val events = when (
            val value = arguments.firstOrNull()
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            is JessieCodeRuntimeValue.StringValue -> listOf(value.value)
            is JessieCodeRuntimeValue.ArrayValue -> {
                val names = mutableListOf<String>()
                for ((index, child) in value.values.withIndex()) {
                    val name = (
                        child as? JessieCodeRuntimeValue.StringValue
                        )?.value ?: return invalidArgumentType(
                        functionName = "trigger",
                        argumentIndex = index,
                        expected = "string event name",
                        actual = child,
                        location = location,
                    )
                    names += name
                }
                names
            }
            else -> return invalidArgumentType(
                functionName = "trigger",
                argumentIndex = 0,
                expected = "string or string array",
                actual = value,
                location = location,
            )
        }
        val states = elementEventHandlers
            .filterKeys {
                it.element === element && it.event in events
            }
            .values
        states.forEach { it.error = null }
        element.trigger(events, arguments.drop(1))
        states.firstNotNullOfOrNull { it.error }?.let {
            return GMResult.Err(it)
        }
        return elementReference(element)
    }

    private fun resolveVisualProperty(
        element: GeometryElement,
        property: String,
    ): ElementPropertyResult? {
        val normalized = normalizedAttributeName(property)
        val attributes = element.getAttributes()
        if (normalized !in attributes) {
            return null
        }
        return nativeRuntimeValue(attributes[normalized])?.let {
            GMResult.Ok(it)
        }
    }

    private fun nativeRuntimeValue(
        value: Any?,
    ): JessieCodeRuntimeValue? =
        when (value) {
            null -> JessieCodeRuntimeValue.NullValue
            is JessieCodeRuntimeValue -> value
            is Boolean -> JessieCodeRuntimeValue.BooleanValue(value)
            is Number ->
                JessieCodeRuntimeValue.NumberValue(value.toDouble())
            is String -> JessieCodeRuntimeValue.StringValue(value)
            is GeometryElement ->
                JessieCodeRuntimeValue.ElementReference(value)
            is Transformation ->
                JessieCodeRuntimeValue.TransformationReference(value)
            is List<*> -> {
                val converted = value.map(::nativeRuntimeValue)
                if (converted.any { it == null }) {
                    null
                } else {
                    JessieCodeRuntimeValue.ArrayValue(
                        converted.filterNotNull(),
                    )
                }
            }
            is DoubleArray ->
                JessieCodeRuntimeValue.ArrayValue(
                    value.map(JessieCodeRuntimeValue::NumberValue),
                )
            is Map<*, *> -> {
                val converted = linkedMapOf<String, JessieCodeRuntimeValue>()
                for ((key, child) in value) {
                    val name = key as? String ?: return null
                    converted[name] =
                        nativeRuntimeValue(child) ?: return null
                }
                JessieCodeRuntimeValue.ObjectValue(converted)
            }
            else -> null
        }

    private fun runtimeValue(value: Any?): JessieCodeRuntimeValue =
        nativeRuntimeValue(value) ?: JessieCodeRuntimeValue.UndefinedValue

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
            "addPoints" -> function("addPoints") {
                    arguments,
                    callLocation,
                ->
                val points = when (
                    val result = polygonPointArguments(
                        polygon = polygon,
                        functionName = "addPoints",
                        arguments = arguments,
                        offset = 0,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@function result
                }
                polygonMutation(
                    polygon = polygon,
                    method = "addPoints",
                    location = callLocation,
                    result = polygon.addPoints(points),
                )
            }
            "insertPoints" -> function("insertPoints") {
                    arguments,
                    callLocation,
                ->
                val indexValue = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.NumberValue
                    )?.value ?: return@function invalidArgumentType(
                    functionName = "insertPoints",
                    argumentIndex = 0,
                    expected = "integer",
                    actual = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue,
                    location = callLocation,
                )
                if (
                    !indexValue.isFinite() ||
                    indexValue % 1.0 != 0.0 ||
                    indexValue < Int.MIN_VALUE.toDouble() ||
                    indexValue > Int.MAX_VALUE.toDouble()
                ) {
                    return@function invalidArgumentType(
                        functionName = "insertPoints",
                        argumentIndex = 0,
                        expected = "integer",
                        actual = arguments.first(),
                        location = callLocation,
                    )
                }
                val points = when (
                    val result = polygonPointArguments(
                        polygon = polygon,
                        functionName = "insertPoints",
                        arguments = arguments,
                        offset = 1,
                        location = callLocation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@function result
                }
                polygonMutation(
                    polygon = polygon,
                    method = "insertPoints",
                    location = callLocation,
                    result = polygon.insertPoints(
                        index = indexValue.toInt(),
                        points = points,
                    ),
                )
            }
            "removePoints" -> function("removePoints") {
                    arguments,
                    callLocation,
                ->
                val indices = mutableListOf<Int>()
                for ((index, argument) in arguments.withIndex()) {
                    when (argument) {
                        is JessieCodeRuntimeValue.ElementReference -> {
                            val point = argument.element as? Point
                                ?: return@function invalidArgumentType(
                                    functionName = "removePoints",
                                    argumentIndex = index,
                                    expected = "point or integer",
                                    actual = argument,
                                    location = callLocation,
                                )
                            indices += polygon.findPoint(point)
                        }
                        is JessieCodeRuntimeValue.NumberValue -> {
                            if (
                                !argument.value.isFinite() ||
                                argument.value % 1.0 != 0.0 ||
                                argument.value < Int.MIN_VALUE.toDouble() ||
                                argument.value > Int.MAX_VALUE.toDouble()
                            ) {
                                return@function invalidArgumentType(
                                    functionName = "removePoints",
                                    argumentIndex = index,
                                    expected = "point or integer",
                                    actual = argument,
                                    location = callLocation,
                                )
                            }
                            indices += argument.value.toInt()
                        }
                        else -> return@function invalidArgumentType(
                            functionName = "removePoints",
                            argumentIndex = index,
                            expected = "point or integer",
                            actual = argument,
                            location = callLocation,
                        )
                    }
                }
                polygonMutation(
                    polygon = polygon,
                    method = "removePoints",
                    location = callLocation,
                    result = polygon.removePoints(indices),
                )
            }
            "Intersect" -> function("intersect") {
                    arguments,
                    callLocation,
                ->
                val other = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.ElementReference
                    )?.element as? Polygon
                    ?: return@function invalidArgumentType(
                        functionName = "intersect",
                        argumentIndex = 0,
                        expected = "polygon",
                        actual = arguments.firstOrNull()
                            ?: JessieCodeRuntimeValue.UndefinedValue,
                        location = callLocation,
                    )
                GMResult.Ok(
                    JessieCodeRuntimeValue.ArrayValue(
                        polygon.intersect(other).map(::array),
                    ),
                )
            }
            else -> null
        }
    }

    private fun polygonPointArguments(
        polygon: Polygon,
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        offset: Int,
        location: JessieCodeAstLocation,
    ): GMResult<List<Point>, JessieCodeRuntimeError> {
        val points = mutableListOf<Point>()
        for (index in offset until arguments.size) {
            val point = (
                arguments[index] as?
                    JessieCodeRuntimeValue.ElementReference
                )?.element as? Point
                ?: return invalidArgumentType(
                    functionName = functionName,
                    argumentIndex = index,
                    expected = "point",
                    actual = arguments[index],
                    location = location,
                )
            if (
                point.board !== polygon.board ||
                polygon.board.select(point) !== point
            ) {
                return GMResult.Err(
                    JessieCodeRuntimeError.ElementMethodUnavailable(
                        elementId = polygon.id,
                        method = functionName,
                        reason =
                            "point ${point.id} is not registered on " +
                                "the polygon board",
                        location = location,
                    ),
                )
            }
            points += point
        }
        return GMResult.Ok(points)
    }

    private fun polygonMutation(
        polygon: Polygon,
        method: String,
        location: JessieCodeAstLocation,
        result: GMResult<Polygon, *>,
    ): ElementPropertyResult =
        when (result) {
            is GMResult.Ok -> elementReference(result.value)
            is GMResult.Err -> GMResult.Err(
                JessieCodeRuntimeError.ElementMethodUnavailable(
                    elementId = polygon.id,
                    method = method,
                    reason = result.error.toString(),
                    location = location,
                ),
            )
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
            else -> null
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
            "setRadius" -> function("setRadius") {
                    arguments,
                    callLocation,
                ->
                val result: GMResult<Circle, *> = when (
                    val value = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    is JessieCodeRuntimeValue.NumberValue ->
                        GMResult.Ok(circle.setRadius(value.value))
                    is JessieCodeRuntimeValue.StringValue ->
                        circle.setRadius(value.value)
                    is JessieCodeRuntimeValue.FunctionValue ->
                        circle.setRadius(
                            CircleRadiusFunction(
                                function = value,
                                location = callLocation,
                            ),
                        )
                    else -> return@function invalidArgumentType(
                        functionName = "setRadius",
                        argumentIndex = 0,
                        expected = "number, string, or function",
                        actual = value,
                        location = callLocation,
                    )
                }
                when (result) {
                    is GMResult.Ok -> elementReference(circle)
                    is GMResult.Err -> GMResult.Err(
                        JessieCodeRuntimeError.ElementMethodUnavailable(
                            elementId = circle.id,
                            method = "setRadius",
                            reason = result.error.toString(),
                            location = callLocation,
                        ),
                    )
                }
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
            else -> null
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
            "setFixedLength" -> function("setFixedLength") {
                    arguments,
                    callLocation,
                ->
                val result: GMResult<Line, *> = when (
                    val value = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    is JessieCodeRuntimeValue.NumberValue ->
                        GMResult.Ok(line.setFixedLength(value.value))
                    is JessieCodeRuntimeValue.StringValue ->
                        line.setFixedLength(value.value)
                    is JessieCodeRuntimeValue.FunctionValue ->
                        line.setFixedLength(value, callLocation)
                    else -> return@function invalidArgumentType(
                        functionName = "setFixedLength",
                        argumentIndex = 0,
                        expected = "number, string, or function",
                        actual = value,
                        location = callLocation,
                    )
                }
                when (result) {
                    is GMResult.Ok -> elementReference(line)
                    is GMResult.Err -> GMResult.Err(
                        JessieCodeRuntimeError.ElementMethodUnavailable(
                            elementId = line.id,
                            method = "setFixedLength",
                            reason = result.error.toString(),
                            location = callLocation,
                        ),
                    )
                }
            }
            "setStraight" -> function("setStraight") {
                    arguments,
                    callLocation,
                ->
                val first = (
                    arguments.firstOrNull() as?
                        JessieCodeRuntimeValue.BooleanValue
                    )?.value ?: return@function invalidArgumentType(
                    functionName = "setStraight",
                    argumentIndex = 0,
                    expected = "boolean",
                    actual = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue,
                    location = callLocation,
                )
                val last = (
                    arguments.getOrNull(1) as?
                        JessieCodeRuntimeValue.BooleanValue
                    )?.value ?: return@function invalidArgumentType(
                    functionName = "setStraight",
                    argumentIndex = 1,
                    expected = "boolean",
                    actual = arguments.getOrNull(1)
                        ?: JessieCodeRuntimeValue.UndefinedValue,
                    location = callLocation,
                )
                elementReference(line.setStraight(first, last))
            }
            else -> null
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
            "move" -> function("setCoords") {
                    arguments,
                    callLocation,
                ->
                val coordinates = when (
                    val first = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    is JessieCodeRuntimeValue.ArrayValue -> when (
                        val result = coordinateArray(
                            functionName = "setCoords",
                            value = first,
                            location = callLocation,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@function result
                    }
                    is JessieCodeRuntimeValue.NumberValue -> {
                        val second = (
                            arguments.getOrNull(1) as?
                                JessieCodeRuntimeValue.NumberValue
                            )?.value ?: return@function invalidArgumentType(
                            functionName = "setCoords",
                            argumentIndex = 1,
                            expected = "number",
                            actual = arguments.getOrNull(1)
                                ?: JessieCodeRuntimeValue.UndefinedValue,
                            location = callLocation,
                        )
                        doubleArrayOf(first.value, second)
                    }
                    else -> return@function invalidArgumentType(
                        functionName = "setCoords",
                        argumentIndex = 0,
                        expected = "coordinate array or number",
                        actual = first,
                        location = callLocation,
                    )
                }
                elementReference(text.setCoords(coordinates))
            }
            "Size" -> function("getSize") { _, _ ->
                GMResult.Ok(array(text.textSize()))
            }
            "setAutoPosition" -> function("setAutoPosition") {
                    _,
                    _,
                ->
                elementReference(text.setAutoPosition())
            }
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
            "glide", "makeGlider" -> function("makeGlider") {
                    arguments,
                    callLocation,
                ->
                val slide = when (
                    val value = arguments.firstOrNull()
                        ?: JessieCodeRuntimeValue.UndefinedValue
                ) {
                    is JessieCodeRuntimeValue.ElementReference ->
                        value.element
                    is JessieCodeRuntimeValue.StringValue ->
                        coordinates.board.select(value.value)
                            ?: return@function GMResult.Err(
                                JessieCodeRuntimeError
                                    .ElementMethodUnavailable(
                                        elementId = coordinates.id,
                                        method = "makeGlider",
                                        reason =
                                            "unknown slide object " +
                                                value.value,
                                        location = callLocation,
                                    ),
                            )
                    else -> return@function invalidArgumentType(
                        functionName = "makeGlider",
                        argumentIndex = 0,
                        expected = "element or string reference",
                        actual = value,
                        location = callLocation,
                    )
                }
                when (val result = coordinates.makeGlider(slide)) {
                    is GMResult.Ok -> elementReference(coordinates)
                    is GMResult.Err -> GMResult.Err(
                        JessieCodeRuntimeError.ElementMethodUnavailable(
                            elementId = coordinates.id,
                            method = "makeGlider",
                            reason = result.error.toString(),
                            location = callLocation,
                        ),
                    )
                }
            }
            "intersect", "makeIntersection" ->
                function("makeIntersection") {
                        arguments,
                        callLocation,
                    ->
                    val point = coordinates as? Point
                        ?: return@function GMResult.Err(
                            JessieCodeRuntimeError
                                .ElementMethodUnavailable(
                                    elementId = coordinates.id,
                                    method = "makeIntersection",
                                    reason =
                                        "only Point can become an " +
                                            "intersection",
                                    location = callLocation,
                                ),
                        )
                    val first = (
                        arguments.firstOrNull() as?
                            JessieCodeRuntimeValue.ElementReference
                        )?.element
                        ?: return@function invalidArgumentType(
                            functionName = "makeIntersection",
                            argumentIndex = 0,
                            expected = "element",
                            actual = arguments.firstOrNull()
                                ?: JessieCodeRuntimeValue.UndefinedValue,
                            location = callLocation,
                        )
                    val second = (
                        arguments.getOrNull(1) as?
                            JessieCodeRuntimeValue.ElementReference
                        )?.element
                        ?: return@function invalidArgumentType(
                            functionName = "makeIntersection",
                            argumentIndex = 1,
                            expected = "element",
                            actual = arguments.getOrNull(1)
                                ?: JessieCodeRuntimeValue.UndefinedValue,
                            location = callLocation,
                        )
                    val firstIndex = optionalNumberArgument(
                        functionName = "makeIntersection",
                        arguments = arguments,
                        index = 2,
                        default = 0.0,
                        location = callLocation,
                    )
                    val secondIndex = optionalNumberArgument(
                        functionName = "makeIntersection",
                        arguments = arguments,
                        index = 3,
                        default = 0.0,
                        location = callLocation,
                    )
                    val firstIndexValue = when (firstIndex) {
                        is GMResult.Ok -> firstIndex.value
                        is GMResult.Err -> return@function firstIndex
                    }
                    val secondIndexValue = when (secondIndex) {
                        is GMResult.Ok -> secondIndex.value
                        is GMResult.Err -> return@function secondIndex
                    }
                    when (
                        val result = point.makeIntersection(
                            first = first,
                            second = second,
                            firstIndex = firstIndexValue,
                            secondIndex = secondIndexValue,
                        )
                    ) {
                        is GMResult.Ok -> elementReference(point)
                        is GMResult.Err -> GMResult.Err(
                            JessieCodeRuntimeError
                                .ElementMethodUnavailable(
                                    elementId = point.id,
                                    method = "makeIntersection",
                                    reason = result.error.toString(),
                                    location = callLocation,
                                ),
                        )
                    }
                }
            "free" -> function("free") { _, _ ->
                elementReference(coordinates.free())
            }
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
                    if (coordinates.type == Const.OBJECT_TYPE_GLIDER) {
                        coordinates.position = value
                        coordinates.board.update()
                    }
                    elementReference(coordinates)
                }
            "onPolygon" -> GMResult.Ok(
                JessieCodeRuntimeValue.BooleanValue(
                    coordinates.onPolygon,
                ),
            )
            "startAnimation" -> function("startAnimation") {
                    arguments,
                    callLocation,
                ->
                val direction = requiredNumberArgument(
                    functionName = "startAnimation",
                    arguments = arguments,
                    index = 0,
                    location = callLocation,
                )
                val steps = requiredNumberArgument(
                    functionName = "startAnimation",
                    arguments = arguments,
                    index = 1,
                    location = callLocation,
                )
                val directionValue = when (direction) {
                    is GMResult.Ok -> direction.value
                    is GMResult.Err -> return@function direction
                }
                val stepsValue = when (steps) {
                    is GMResult.Ok -> steps.value
                    is GMResult.Err -> return@function steps
                }
                if (
                    !stepsValue.isFinite() ||
                    stepsValue % 1.0 != 0.0 ||
                    stepsValue < Int.MIN_VALUE.toDouble() ||
                    stepsValue > Int.MAX_VALUE.toDouble()
                ) {
                    return@function invalidArgumentType(
                        functionName = "startAnimation",
                        argumentIndex = 1,
                        expected = "integer",
                        actual = arguments[1],
                        location = callLocation,
                    )
                }
                val delay = optionalNumberArgument(
                    functionName = "startAnimation",
                    arguments = arguments,
                    index = 2,
                    default = 250.0,
                    location = callLocation,
                )
                val delayValue = when (delay) {
                    is GMResult.Ok -> delay.value
                    is GMResult.Err -> return@function delay
                }
                when (
                    val result = coordinates.startAnimation(
                        direction = directionValue,
                        stepCount = stepsValue.toInt(),
                        delayMilliseconds = delayValue,
                    )
                ) {
                    is GMResult.Ok -> elementReference(coordinates)
                    is GMResult.Err -> GMResult.Err(
                        JessieCodeRuntimeError.ElementMethodUnavailable(
                            elementId = coordinates.id,
                            method = "startAnimation",
                            reason = result.error.toString(),
                            location = callLocation,
                        ),
                    )
                }
            }
            "stopAnimation" -> function("stopAnimation") { _, _ ->
                elementReference(coordinates.stopAnimation())
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
            "moveAlong" -> function("moveAlong") {
                    arguments,
                    callLocation,
                ->
                moveAlong(
                    element = coordinates,
                    arguments = arguments,
                    location = callLocation,
                )
            }
            "visit" -> function("visit") {
                    arguments,
                    callLocation,
                ->
                visit(
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
            else -> null
        }
    }

    private fun requiredNumberArgument(
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        index: Int,
        location: JessieCodeAstLocation,
    ): GMResult<Double, JessieCodeRuntimeError> {
        val value = arguments.getOrNull(index)
            ?: JessieCodeRuntimeValue.UndefinedValue
        val number = (
            value as? JessieCodeRuntimeValue.NumberValue
            )?.value ?: return invalidArgumentType(
            functionName = functionName,
            argumentIndex = index,
            expected = "number",
            actual = value,
            location = location,
        )
        return GMResult.Ok(number)
    }

    private fun optionalNumberArgument(
        functionName: String,
        arguments: List<JessieCodeRuntimeValue>,
        index: Int,
        default: Double,
        location: JessieCodeAstLocation,
    ): GMResult<Double, JessieCodeRuntimeError> =
        when (
            val value = arguments.getOrNull(index)
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> GMResult.Ok(default)
            is JessieCodeRuntimeValue.NumberValue ->
                GMResult.Ok(value.value)
            else -> invalidArgumentType(
                functionName = functionName,
                argumentIndex = index,
                expected = "number or undefined",
                actual = value,
                location = location,
            )
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
        val duration = when (
            val time = arguments.getOrNull(1)
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> null
            is JessieCodeRuntimeValue.NumberValue -> time.value
            else -> return invalidArgumentType(
                functionName = "moveTo",
                argumentIndex = 1,
                expected = "number or undefined",
                actual = time,
                location = location,
            )
        }
        val options = when (
            val result = moveOptions(
                functionName = "moveTo",
                element = element,
                value = arguments.getOrNull(2)
                    ?: JessieCodeRuntimeValue.UndefinedValue,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        return when (
            val result = element.moveTo(
                where = coordinates,
                timeMilliseconds = duration,
                options = options,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(
                JessieCodeRuntimeValue.BoardReference(element.board),
            )
            is GMResult.Err -> GMResult.Err(
                JessieCodeRuntimeError.ElementMethodUnavailable(
                    elementId = element.id,
                    method = "moveTo",
                    reason = result.error.toString(),
                    location = location,
                ),
            )
        }
    }

    private fun moveAlong(
        element: CoordsElement,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult {
        val duration = when (
            val time = arguments.getOrNull(1)
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> 0.0
            is JessieCodeRuntimeValue.NumberValue -> time.value
            else -> return invalidArgumentType(
                functionName = "moveAlong",
                argumentIndex = 1,
                expected = "number or undefined",
                actual = time,
                location = location,
            )
        }
        val options = when (
            val result = moveOptions(
                functionName = "moveAlong",
                element = element,
                value = arguments.getOrNull(2)
                    ?: JessieCodeRuntimeValue.UndefinedValue,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val path = arguments.firstOrNull()
            ?: JessieCodeRuntimeValue.UndefinedValue
        val result = when (path) {
            is JessieCodeRuntimeValue.ArrayValue -> {
                val coordinates = mutableListOf<DoubleArray>()
                for ((index, value) in path.values.withIndex()) {
                    val pointCoordinates = when (value) {
                        is JessieCodeRuntimeValue.ElementReference -> {
                            val point = value.element as? CoordsElement
                                ?: return invalidArgumentType(
                                    functionName = "moveAlong",
                                    argumentIndex = index,
                                    expected =
                                        "coordinate array or point",
                                    actual = value,
                                    location = location,
                                )
                            doubleArrayOf(point.X(), point.Y())
                        }
                        else -> when (
                            val parsed = coordinateArray(
                                functionName = "moveAlong",
                                value = value,
                                location = location,
                            )
                        ) {
                            is GMResult.Ok -> parsed.value
                            is GMResult.Err -> return parsed
                        }
                    }
                    coordinates += pointCoordinates
                }
                element.moveAlong(
                    path = coordinates,
                    timeMilliseconds = duration,
                    options = options,
                )
            }
            is JessieCodeRuntimeValue.FunctionValue ->
                element.moveAlong(
                    path = CoordsElementPathFunction {
                            elapsedMilliseconds,
                        ->
                        when (
                            val evaluated = path.externalCallable.call(
                                arguments = listOf(
                                    JessieCodeRuntimeValue.NumberValue(
                                        elapsedMilliseconds,
                                    ),
                                ),
                                location = location,
                            )
                        ) {
                            is GMResult.Err -> GMResult.Err(
                                CoordsElementAnimationError.PathEvaluation(
                                    evaluated.error.toString(),
                                ),
                            )
                            is GMResult.Ok -> when (val value = evaluated.value) {
                                JessieCodeRuntimeValue.UndefinedValue,
                                JessieCodeRuntimeValue.NullValue,
                                -> GMResult.Ok(null)
                                is JessieCodeRuntimeValue.NumberValue ->
                                    if (value.value.isNaN()) {
                                        GMResult.Ok(null)
                                    } else {
                                        GMResult.Err(
                                            CoordsElementAnimationError
                                                .PathEvaluation(
                                                    "expected coordinates " +
                                                        "or NaN, got number",
                                                ),
                                        )
                                    }
                                is JessieCodeRuntimeValue.ArrayValue ->
                                    when (
                                        val coordinates = coordinateArray(
                                            functionName = "moveAlong",
                                            value = value,
                                            location = location,
                                        )
                                    ) {
                                        is GMResult.Ok ->
                                            GMResult.Ok(coordinates.value)
                                        is GMResult.Err -> GMResult.Err(
                                            CoordsElementAnimationError
                                                .PathEvaluation(
                                                    coordinates.error
                                                        .toString(),
                                                ),
                                        )
                                    }
                                else -> GMResult.Err(
                                    CoordsElementAnimationError.PathEvaluation(
                                        "expected coordinates or NaN, got " +
                                            typeName(value),
                                    ),
                                )
                            }
                        }
                    },
                    timeMilliseconds = duration,
                    options = options,
                )
            else -> return invalidArgumentType(
                functionName = "moveAlong",
                argumentIndex = 0,
                expected = "path array or function",
                actual = path,
                location = location,
            )
        }
        return movementResult(
            element = element,
            method = "moveAlong",
            location = location,
            result = result,
        )
    }

    private fun visit(
        element: CoordsElement,
        arguments: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): ElementPropertyResult {
        val coordinates = when (
            val result = coordinateArray(
                functionName = "visit",
                value = arguments.firstOrNull()
                    ?: JessieCodeRuntimeValue.UndefinedValue,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val duration = (
            arguments.getOrNull(1) as?
                JessieCodeRuntimeValue.NumberValue
            )?.value ?: return invalidArgumentType(
            functionName = "visit",
            argumentIndex = 1,
            expected = "number",
            actual = arguments.getOrNull(1)
                ?: JessieCodeRuntimeValue.UndefinedValue,
            location = location,
        )
        val options = when (
            val result = moveOptions(
                functionName = "visit",
                element = element,
                value = arguments.getOrNull(2)
                    ?: JessieCodeRuntimeValue.UndefinedValue,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return movementResult(
            element = element,
            method = "visit",
            location = location,
            result = element.visit(
                where = coordinates,
                timeMilliseconds = duration,
                options = options,
            ),
        )
    }

    private fun movementResult(
        element: CoordsElement,
        method: String,
        location: JessieCodeAstLocation,
        result: GMResult<CoordsElement, CoordsElementAnimationError>,
    ): ElementPropertyResult =
        when (result) {
            is GMResult.Ok -> GMResult.Ok(
                JessieCodeRuntimeValue.BoardReference(element.board),
            )
            is GMResult.Err -> GMResult.Err(
                JessieCodeRuntimeError.ElementMethodUnavailable(
                    elementId = element.id,
                    method = method,
                    reason = result.error.toString(),
                    location = location,
                ),
            )
        }

    private fun moveOptions(
        functionName: String,
        element: CoordsElement,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<CoordsElementMoveOptions, JessieCodeRuntimeError> {
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(CoordsElementMoveOptions())
        }
        val properties = (
            value as? JessieCodeRuntimeValue.ObjectValue
            )?.properties ?: return invalidArgumentType(
            functionName = functionName,
            argumentIndex = 2,
            expected = "object or undefined",
            actual = value,
            location = location,
        )
        val effect = when (
            val raw = properties["effect"]
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> null
            is JessieCodeRuntimeValue.StringValue -> raw.value
            else -> return invalidArgumentType(
                functionName = functionName,
                argumentIndex = 2,
                expected = "options.effect string",
                actual = raw,
                location = location,
            )
        }
        val callback = when (
            val raw = properties["callback"]
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> null
            is JessieCodeRuntimeValue.FunctionValue -> {
                {
                    when (
                        val callbackResult = raw.externalCallable.call(
                            arguments = emptyList(),
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> element.reportMovementError(
                            CoordsElementAnimationError.Callback(
                                callbackResult.error.toString(),
                            ),
                        )
                    }
                }
            }
            else -> return invalidArgumentType(
                functionName = functionName,
                argumentIndex = 2,
                expected = "options.callback function",
                actual = raw,
                location = location,
            )
        }
        val interpolate = when (
            val raw = properties["interpolate"]
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> true
            is JessieCodeRuntimeValue.BooleanValue -> raw.value
            else -> return invalidArgumentType(
                functionName = functionName,
                argumentIndex = 2,
                expected = "options.interpolate boolean",
                actual = raw,
                location = location,
            )
        }
        val repeat = when (
            val raw = properties["repeat"]
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> 1
            is JessieCodeRuntimeValue.NumberValue -> {
                if (
                    !raw.value.isFinite() ||
                    raw.value % 1.0 != 0.0 ||
                    raw.value < Int.MIN_VALUE.toDouble() ||
                    raw.value > Int.MAX_VALUE.toDouble()
                ) {
                    return invalidArgumentType(
                        functionName = functionName,
                        argumentIndex = 2,
                        expected = "options.repeat integer",
                        actual = raw,
                        location = location,
                    )
                }
                raw.value.toInt()
            }
            else -> return invalidArgumentType(
                functionName = functionName,
                argumentIndex = 2,
                expected = "options.repeat integer",
                actual = raw,
                location = location,
            )
        }
        return GMResult.Ok(
            CoordsElementMoveOptions(
                callback = callback,
                effect = effect,
                interpolate = interpolate,
                repeat = repeat,
            ),
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

    private const val MAX_ATTRIBUTE_VALUE_DEPTH = 64
}

private typealias ElementPropertyResult =
    GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>
