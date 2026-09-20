/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/coordselement.js -> addConstraint
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.GeometryElement

internal interface JessieCodeCoordinateFunction {
    val origin: String?

    val dependencies: Map<String, GeometryElement>

    val returnsCoordinateArray: Boolean
        get() = false

    fun evaluate(
        arguments: List<JessieCodeRuntimeValue> = emptyList(),
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>
}

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

internal class JessieCodeRuntimeCoordinateFunction(
    internal val function: JessieCodeRuntimeValue.FunctionValue,
    private val location: JessieCodeAstLocation,
    override val returnsCoordinateArray: Boolean,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null

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
