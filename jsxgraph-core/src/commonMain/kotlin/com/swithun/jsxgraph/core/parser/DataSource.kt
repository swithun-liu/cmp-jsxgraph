/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/datasource.js -> DataSource.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.utils.Type

internal sealed interface DataSourceHeader {
    data object None : DataSourceHeader

    data object Embedded : DataSourceHeader

    data class Explicit(
        val values: List<Any?>,
    ) : DataSourceHeader
}

internal sealed interface DataSourceError {
    data class MissingColumnHeaderRow(
        val rowCount: Int,
    ) : DataSourceError

    data class TableNotFound(
        val tableId: String,
    ) : DataSourceError

    data class UnknownRow(
        val row: String,
    ) : DataSourceError

    data class RowIndexOutOfBounds(
        val row: Int,
        val rowCount: Int,
    ) : DataSourceError

    data class UnsupportedOperation(
        val operation: String,
    ) : DataSourceError
}

/**
 * Portable `JXG.DataSource` translation.
 *
 * Browser table lookup is supplied by the caller as already extracted rows;
 * production code therefore remains independent of a DOM or JavaScript
 * runtime.
 */
internal class DataSource {
    internal var data: List<List<Any?>> = emptyList()
        private set
    internal var columnHeaders: List<Any?> = emptyList()
        private set
    internal var rowHeaders: List<Any?> = emptyList()
        private set

    // JSXGraph 1.13.3: src/parser/datasource.js -> loadFromArray.
    internal fun loadFromArray(
        table: List<List<Any?>>,
        columnHeader: DataSourceHeader = DataSourceHeader.None,
        rowHeader: DataSourceHeader = DataSourceHeader.None,
    ): GMResult<DataSource, DataSourceError> {
        val nextColumnHeaders =
            (columnHeader as? DataSourceHeader.Explicit)?.values
                ?: if (columnHeader == DataSourceHeader.Embedded) {
                    emptyList()
                } else {
                    columnHeaders
                }
        var nextRowHeaders =
            (rowHeader as? DataSourceHeader.Explicit)?.values
                ?: if (rowHeader == DataSourceHeader.Embedded) {
                    emptyList()
                } else {
                    rowHeaders
                }
        var nextData = table.map { row ->
            row.map(::normalizeCell)
        }

        if (columnHeader == DataSourceHeader.Embedded) {
            val firstRow = nextData.firstOrNull()
                ?: return GMResult.Err(
                    DataSourceError.MissingColumnHeaderRow(
                        rowCount = nextData.size,
                    ),
                )
            columnHeaders = firstRow.drop(1)
            nextData = nextData.drop(1)
        } else {
            columnHeaders = nextColumnHeaders.toList()
        }

        if (rowHeader == DataSourceHeader.Embedded) {
            nextRowHeaders = nextData.map { row -> row.firstOrNull() }
            nextData = nextData.map { row -> row.drop(1) }
        }

        data = nextData.map(List<Any?>::toList)
        rowHeaders = nextRowHeaders.toList()
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/parser/datasource.js -> loadFromTable.
    internal fun loadFromTable(
        tableId: String,
        tableProvider: (String) -> List<List<Any?>>?,
        columnHeader: DataSourceHeader = DataSourceHeader.None,
        rowHeader: DataSourceHeader = DataSourceHeader.None,
    ): GMResult<DataSource, DataSourceError> {
        val table = tableProvider(tableId)
            ?: return GMResult.Err(DataSourceError.TableNotFound(tableId))
        return loadFromArray(
            table = table,
            columnHeader = columnHeader,
            rowHeader = rowHeader,
        )
    }

    // JSXGraph 1.13.3: src/parser/datasource.js -> addColumn.
    internal fun addColumn(): GMResult<Nothing, DataSourceError> =
        GMResult.Err(
            DataSourceError.UnsupportedOperation("addColumn"),
        )

    // JSXGraph 1.13.3: src/parser/datasource.js -> addRow.
    internal fun addRow(): GMResult<Nothing, DataSourceError> =
        GMResult.Err(
            DataSourceError.UnsupportedOperation("addRow"),
        )

    // JSXGraph 1.13.3: src/parser/datasource.js -> getColumn.
    internal fun getColumn(column: Int): List<Double> =
        buildList {
            for (row in data) {
                if (column < row.size) {
                    add(parseFloat(row.getOrNull(column)))
                }
            }
        }

    // JSXGraph 1.13.3: src/parser/datasource.js -> getColumn.
    internal fun getColumn(column: String): List<Double> {
        val index = columnHeaders.indexOfFirst { it == column }
        return if (index >= 0) getColumn(index) else emptyList()
    }

    // JSXGraph 1.13.3: src/parser/datasource.js -> getRow.
    internal fun getRow(
        row: Int,
    ): GMResult<List<Any?>, DataSourceError> {
        if (row !in data.indices) {
            return GMResult.Err(
                DataSourceError.RowIndexOutOfBounds(
                    row = row,
                    rowCount = data.size,
                ),
            )
        }
        return GMResult.Ok(data[row].toList())
    }

    // JSXGraph 1.13.3: src/parser/datasource.js -> getRow.
    internal fun getRow(
        row: String,
    ): GMResult<List<Any?>, DataSourceError> {
        val index = rowHeaders.indexOfFirst { it == row }
        if (index < 0) {
            return GMResult.Err(DataSourceError.UnknownRow(row))
        }
        return getRow(index)
    }

    private fun normalizeCell(cell: Any?): Any? {
        if (cell !is String) {
            return cell
        }
        if (cell == "-") {
            return Double.NaN
        }
        val parsed = parseFloat(cell)
        return if (
            Type.formatJsNumber(parsed) == cell
        ) {
            parsed
        } else {
            cell
        }
    }

    private fun parseFloat(value: Any?): Double =
        when (value) {
            is Number -> value.toDouble()
            is String -> {
                val match = NUMBER_PREFIX.find(value.trimStart())
                match?.value?.toDoubleOrNull() ?: Double.NaN
            }
            else -> Double.NaN
        }

    private companion object {
        val NUMBER_PREFIX = Regex(
            """^[+-]?(?:Infinity|(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?)""",
        )
    }
}
