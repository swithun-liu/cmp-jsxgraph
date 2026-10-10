/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/options.js -> JXG.Validator
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.abs
import kotlin.math.round

internal sealed interface OptionsValidatorEntry {
    data object Unvalidated : OptionsValidatorEntry

    fun interface Predicate : OptionsValidatorEntry {
        fun validate(value: Any?): Boolean
    }
}

/**
 * Pure-KMP value predicates used by the upstream options validator.
 *
 * Renderer-name validation intentionally mirrors JSXGraph input semantics.
 * It does not imply that the VML or SVG backends exist in production.
 */
internal object OptionsValidator {
    // JSXGraph 1.13.3: src/options.js -> JXG.Validator.
    val entries: Map<String, OptionsValidatorEntry> = linkedMapOf(
        "attractordistance" to predicate(::validateNotNegative),
        "color" to predicate(::validateColor),
        "display" to predicate(::validateDisplay),
        "doadvancedplot" to OptionsValidatorEntry.Unvalidated,
        "draft" to OptionsValidatorEntry.Unvalidated,
        "drawlabels" to OptionsValidatorEntry.Unvalidated,
        "drawzero" to OptionsValidatorEntry.Unvalidated,
        "face" to predicate(::validatePointFace),
        "factor" to predicate(::validateNumber),
        "fillcolor" to predicate(::validateColor),
        "fillopacity" to predicate(::validateNumber),
        "firstarrow" to OptionsValidatorEntry.Unvalidated,
        "fontsize" to predicate(::validateInteger),
        "dash" to predicate(::validateInteger),
        "gridx" to predicate(::validateNumber),
        "gridy" to predicate(::validateNumber),
        "hasgrid" to OptionsValidatorEntry.Unvalidated,
        "highlightfillcolor" to predicate(::validateColor),
        "highlightfillopacity" to predicate(::validateNumber),
        "highlightstrokecolor" to predicate(::validateColor),
        "highlightstrokeopacity" to predicate(::validateNumber),
        "insertticks" to OptionsValidatorEntry.Unvalidated,
        "lastarrow" to OptionsValidatorEntry.Unvalidated,
        "layer" to predicate(::validateNotNegativeInteger),
        "majorheight" to predicate(::validateInteger),
        "minorheight" to predicate(::validateInteger),
        "minorticks" to predicate(::validateNotNegative),
        "minticksdistance" to predicate(::validatePositiveInteger),
        "numberpointshigh" to predicate(::validatePositiveInteger),
        "numberpointslow" to predicate(::validatePositiveInteger),
        "opacity" to predicate(::validateNumber),
        "radius" to predicate(::validateNumber),
        "rdpsmoothing" to OptionsValidatorEntry.Unvalidated,
        "renderer" to predicate(::validateRenderer),
        "right" to predicate(::validatePixel),
        "showcopyright" to OptionsValidatorEntry.Unvalidated,
        "showinfobox" to OptionsValidatorEntry.Unvalidated,
        "shownavigation" to OptionsValidatorEntry.Unvalidated,
        "size" to predicate(::validateNotNegative),
        "snapsizex" to predicate(::validatePositive),
        "snapsizey" to predicate(::validatePositive),
        "snapwidth" to predicate(::validateNumber),
        "snaptogrid" to OptionsValidatorEntry.Unvalidated,
        "snatchdistance" to predicate(::validateNotNegative),
        "straightfirst" to OptionsValidatorEntry.Unvalidated,
        "straightlast" to OptionsValidatorEntry.Unvalidated,
        "stretch" to OptionsValidatorEntry.Unvalidated,
        "strokecolor" to predicate(::validateColor),
        "strokeopacity" to predicate(::validateNumber),
        "strokewidth" to predicate(::validateNotNegative),
        "takefirst" to OptionsValidatorEntry.Unvalidated,
        "takesizefromfile" to OptionsValidatorEntry.Unvalidated,
        "to10" to OptionsValidatorEntry.Unvalidated,
        "toorigin" to OptionsValidatorEntry.Unvalidated,
        "translateto10" to OptionsValidatorEntry.Unvalidated,
        "translatetoorigin" to OptionsValidatorEntry.Unvalidated,
        "useasciimathml" to OptionsValidatorEntry.Unvalidated,
        "usedirection" to OptionsValidatorEntry.Unvalidated,
        "usemathjax" to OptionsValidatorEntry.Unvalidated,
        "withlabel" to OptionsValidatorEntry.Unvalidated,
        "withticks" to OptionsValidatorEntry.Unvalidated,
        "zoom" to OptionsValidatorEntry.Unvalidated,
    )

    fun validateProperty(
        name: String,
        value: Any?,
    ): Boolean? =
        when (val entry = entries[name.lowercase()]) {
            null -> null
            OptionsValidatorEntry.Unvalidated -> true
            is OptionsValidatorEntry.Predicate -> entry.validate(value)
        }

    // JSXGraph 1.13.3: src/options.js -> validatePixel.
    fun validatePixel(value: Any?): Boolean =
        value is String && PIXEL_VALUE.matches(value)

    // JSXGraph 1.13.3: src/options.js -> validateDisplay.
    fun validateDisplay(value: Any?): Boolean =
        value == "html" || value == "internal"

    // JSXGraph 1.13.3: src/options.js -> validateColor.
    fun validateColor(value: Any?): Boolean = value is String

    // JSXGraph 1.13.3: src/options.js -> validatePointFace.
    fun validatePointFace(value: Any?): Boolean =
        value is String && value in POINT_FACES

    // JSXGraph 1.13.3: src/options.js -> validateNumber.
    fun validateNumber(value: Any?): Boolean =
        when (value) {
            is Number -> !value.toDouble().isNaN()
            is String -> {
                val number = value.toDoubleOrNull()
                number != null &&
                    !number.isNaN() &&
                    JsNumberFormat.compact(number) == value
            }
            else -> false
        }

    // JSXGraph 1.13.3: src/options.js -> validateInteger.
    fun validateInteger(value: Any?): Boolean {
        val number = jsNumber(value) ?: return false
        return abs(number - round(number)) < Mat.eps
    }

    // JSXGraph 1.13.3: src/options.js -> validateNotNegativeInteger.
    fun validateNotNegativeInteger(value: Any?): Boolean =
        validateInteger(value) &&
            (jsNumber(value)?.let { it >= 0.0 } == true)

    // JSXGraph 1.13.3: src/options.js -> validatePositiveInteger.
    fun validatePositiveInteger(value: Any?): Boolean =
        validateInteger(value) &&
            (jsNumber(value)?.let { it > 0.0 } == true)

    // JSXGraph 1.13.3: src/options.js -> validateRenderer.
    fun validateRenderer(value: Any?): Boolean =
        value in setOf("vml", "svg", "canvas", "no")

    // JSXGraph 1.13.3: src/options.js -> validatePositive.
    fun validatePositive(value: Any?): Boolean =
        jsNumber(value)?.let { it > 0.0 } == true

    // JSXGraph 1.13.3: src/options.js -> validateNotNegative.
    fun validateNotNegative(value: Any?): Boolean =
        jsNumber(value)?.let { it >= 0.0 } == true

    private fun jsNumber(value: Any?): Double? =
        when (value) {
            null -> 0.0
            false -> 0.0
            true -> 1.0
            is Number -> value.toDouble()
            is String -> jsStringNumber(value)
            is List<*> -> when (value.size) {
                0 -> 0.0
                1 -> jsArrayItemString(value[0]).let(::jsStringNumber)
                else -> null
            }
            is Array<*> -> jsNumber(value.asList())
            else -> null
        }

    private fun jsArrayItemString(value: Any?): String =
        when (value) {
            null -> ""
            is String -> value
            is Number, is Boolean -> value.toString()
            else -> ""
        }

    private fun jsStringNumber(value: String): Double? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return 0.0
        }
        val sign = when {
            trimmed.startsWith("-") -> -1.0
            else -> 1.0
        }
        val unsigned = trimmed.removePrefix("+").removePrefix("-")
        val radixValue = when {
            unsigned.startsWith("0x", ignoreCase = true) ->
                unsigned.drop(2).toLongOrNull(16)?.toDouble()
            unsigned.startsWith("0b", ignoreCase = true) ->
                unsigned.drop(2).toLongOrNull(2)?.toDouble()
            unsigned.startsWith("0o", ignoreCase = true) ->
                unsigned.drop(2).toLongOrNull(8)?.toDouble()
            else -> null
        }
        return radixValue?.times(sign) ?: trimmed.toDoubleOrNull()
    }

    private fun predicate(
        validator: (Any?) -> Boolean,
    ): OptionsValidatorEntry =
        OptionsValidatorEntry.Predicate(validator)

    private val PIXEL_VALUE = Regex("^[0-9]+px$")
    private val POINT_FACES = setOf(
        "cross",
        "x",
        "circle",
        "o",
        "square",
        "[]",
        "plus",
        "+",
        "divide",
        "|",
        "minus",
        "-",
        "diamond",
        "<>",
        "diamond2",
        "<<>>",
        "triangleup",
        "A",
        "a",
        "^",
        "triangledown",
        "v",
        "triangleleft",
        "<",
        "triangleright",
        ">",
    )
}
