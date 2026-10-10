/*
 * Kotlin translation support for JSXGraph.
 * Upstream: src/base/chart.js -> createChart table branch;
 * src/parser/datasource.js -> loadFromTable.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

/**
 * Host-neutral snapshot of the text cells in one HTML-style chart table.
 *
 * JSXGraph reads `innerText` from every table cell before DataSource performs
 * its numeric normalization, so the portable boundary intentionally exposes
 * strings rather than DOM nodes or platform table objects.
 */
data class JsxGraphChartTableSnapshot(
    val rows: List<List<String>>,
)

sealed interface JsxGraphChartTableError {
    val message: String

    data class NotFound(
        val tableId: String,
    ) : JsxGraphChartTableError {
        override val message: String =
            "Chart table '$tableId' was not found."
    }

    data class Rejected(
        override val message: String,
    ) : JsxGraphChartTableError

    data class CallbackFailed(
        override val message: String,
    ) : JsxGraphChartTableError
}

/**
 * Resolves the string parent accepted by JSXGraph's table-backed Chart path.
 *
 * Hosts may bridge this to a UI table, database result, or another immutable
 * data source without exposing that host API to common code.
 */
fun interface JsxGraphChartTableProvider {
    fun load(
        tableId: String,
    ): GMResult<JsxGraphChartTableSnapshot, JsxGraphChartTableError>
}
