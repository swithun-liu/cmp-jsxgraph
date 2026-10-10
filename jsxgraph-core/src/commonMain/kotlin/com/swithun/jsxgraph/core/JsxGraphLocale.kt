/*
 * Kotlin translation support for JSXGraph.
 * Upstream: src/base/element.js -> formatNumberLocale / useLocale
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

sealed interface JsxGraphLocaleOptionValue {
    data class StringValue(
        val value: String,
    ) : JsxGraphLocaleOptionValue

    data class NumberValue(
        val value: Double,
    ) : JsxGraphLocaleOptionValue

    data class BooleanValue(
        val value: Boolean,
    ) : JsxGraphLocaleOptionValue
}

data class JsxGraphLocaleNumberFormatRequest(
    val value: Double,
    val locale: String?,
    val options: Map<String, JsxGraphLocaleOptionValue>,
)

sealed interface JsxGraphLocaleNumberFormatError {
    val message: String

    data class Rejected(
        override val message: String,
    ) : JsxGraphLocaleNumberFormatError

    data class InvalidLocale(
        val actualType: String,
    ) : JsxGraphLocaleNumberFormatError {
        override val message: String =
            "Locale must evaluate to a string or null, got $actualType."
    }

    data class InvalidOption(
        val name: String,
        val actualType: String,
    ) : JsxGraphLocaleNumberFormatError {
        override val message: String =
            "Locale option '$name' must evaluate to a string, number, " +
                "boolean, or null, got $actualType."
    }

    data class AttributeEvaluationFailed(
        val attribute: String,
        val reason: String,
    ) : JsxGraphLocaleNumberFormatError {
        override val message: String =
            "Could not evaluate locale attribute '$attribute': $reason."
    }

    data class CallbackFailed(
        override val message: String,
    ) : JsxGraphLocaleNumberFormatError
}

/**
 * Host-neutral counterpart of `Intl.NumberFormat`.
 *
 * Core resolves JSXGraph's locale inheritance and normalized option names.
 * Hosts may implement this contract with their platform locale library.
 */
fun interface JsxGraphLocaleNumberFormatter {
    fun format(
        request: JsxGraphLocaleNumberFormatRequest,
    ): GMResult<String, JsxGraphLocaleNumberFormatError>
}

data class JsxGraphRuntimeOptions(
    val localeNumberFormatter: JsxGraphLocaleNumberFormatter? = null,
    val chartTableProvider: JsxGraphChartTableProvider? = null,
)
