/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/coordselement.js -> addConstraint
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Coords
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Slider

internal interface JessieCodeCoordinateFunction {
    val origin: String?

    val dependencies: Map<String, GeometryElement>

    val coordinateParents: List<GeometryElement>
        get() = emptyList()

    val returnsCoordinateArray: Boolean
        get() = false

    fun evaluateCoordsObject():
        GMResult<Coords?, JessieCodeRuntimeError> =
        GMResult.Ok(null)

    fun evaluate(
        arguments: List<JessieCodeRuntimeValue> = emptyList(),
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>
}

internal interface JessieCodeSourceBackedFunction {
    val sourceExpression: String?
    val javaScriptSourceExpression: String?
}

// JSXGraph 1.13.3: src/base/coordselement.js ->
// addConstraint closure makeConstFunction.
internal class JessieCodeNumericCoordinateFunction(
    private val value: Double,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Ok(JessieCodeRuntimeValue.NumberValue(value))
}

// JSXGraph 1.13.3: src/base/coordselement.js -> addConstraint,
// makeSliderFunction.
internal class JessieCodeSliderCoordinateFunction(
    private val slider: Slider,
) : JessieCodeCoordinateFunction {
    override val origin: String? = slider.id
    override val dependencies: Map<String, GeometryElement> =
        mapOf(slider.id to slider)
    override val coordinateParents: List<GeometryElement> =
        listOf(slider)

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Ok(
            JessieCodeRuntimeValue.NumberValue(slider.Value()),
        )
}

internal class JessieCodeConstantCoordinateFunction(
    private val value: JessieCodeRuntimeValue,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Ok(value)
}

// JSXGraph 1.13.3: src/base/coordselement.js -> addConstraint,
// one-function Coords-object result branch.
internal class JessieCodeCoordsCoordinateFunction(
    override val dependencies: Map<String, GeometryElement> = emptyMap(),
    private val provider:
        () -> GMResult<Coords, JessieCodeRuntimeError>,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null

    override fun evaluateCoordsObject():
        GMResult<Coords?, JessieCodeRuntimeError> =
        when (val result = provider()) {
            is GMResult.Ok -> GMResult.Ok(result.value)
            is GMResult.Err -> result
        }

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
}

internal class JessieCodeRuntimeCoordinateFunction(
    internal val function: JessieCodeRuntimeValue.FunctionValue,
    private val location: JessieCodeAstLocation,
    override val returnsCoordinateArray: Boolean,
) : JessieCodeCoordinateFunction, JessieCodeSourceBackedFunction {
    override val origin: String? = function.sourceExpression
    override val sourceExpression: String? = function.sourceExpression
    override val javaScriptSourceExpression: String? =
        function.javaScriptSourceExpression

    /*
     * JSXGraph only calls addParentsFromJCFunctions for string constraints.
     * Direct JavaScript functions remain regular-update constraints without
     * geometric parent entries.
     */
    override val dependencies: Map<String, GeometryElement> = emptyMap()

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        function.externalCallable.call(
            arguments = arguments,
            location = location,
        )
}
