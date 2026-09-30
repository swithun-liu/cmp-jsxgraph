/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/button.js, checkbox.js, input.js;
 * src/base/text.js -> createHTMLSlider
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue

internal sealed interface HtmlControlDefinition {
    val disabled: Boolean

    data class Button(
        override val disabled: Boolean,
        val handler: HtmlButtonHandler?,
    ) : HtmlControlDefinition

    data class Checkbox(
        override val disabled: Boolean,
        var checked: Boolean,
    ) : HtmlControlDefinition

    data class Input(
        override val disabled: Boolean,
        val maxLength: Int,
        var value: String,
    ) : HtmlControlDefinition

    data class Slider(
        val minimum: Double,
        val maximum: Double,
        val step: Double,
        val widthRange: Double,
        val widthOut: Double,
        val withLabel: Boolean,
        var value: Double,
    ) : HtmlControlDefinition {
        override val disabled: Boolean = false
    }
}

internal sealed interface HtmlButtonHandler {
    data class JessieCode(
        val source: String,
    ) : HtmlButtonHandler

    data class Function(
        val value: JessieCodeRuntimeValue.FunctionValue,
        val location: JessieCodeAstLocation,
    ) : HtmlButtonHandler
}
