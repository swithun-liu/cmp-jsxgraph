/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DataSourceTest {
    @Test
    fun embeddedHeadersAndCellCoercionMatchOfficialFixture() {
        val source = DataSource()
        assertIs<GMResult.Ok<DataSource>>(
            source.loadFromArray(
                table = listOf(
                    listOf("", "A", "B"),
                    listOf("r1", "1", "1.0"),
                    listOf("r2", "-", "2x"),
                    listOf("r3", 3, true),
                    listOf("short"),
                ),
                columnHeader = DataSourceHeader.Embedded,
                rowHeader = DataSourceHeader.Embedded,
            ),
        )

        assertEquals(listOf("A", "B"), source.columnHeaders)
        assertEquals(
            listOf("r1", "r2", "r3", "short"),
            source.rowHeaders,
        )
        assertEquals(1.0, source.data[0][0])
        assertEquals("1.0", source.data[0][1])
        assertTrue((source.data[1][0] as Double).isNaN())
        assertEquals("2x", source.data[1][1])
        assertEquals(3, source.data[2][0])
        assertEquals(true, source.data[2][1])
        assertEquals(emptyList(), source.data[3])

        val columnA = source.getColumn("A")
        assertEquals(3, columnA.size)
        assertEquals(1.0, columnA[0])
        assertTrue(columnA[1].isNaN())
        assertEquals(3.0, columnA[2])

        val columnB = source.getColumn("B")
        assertEquals(3, columnB.size)
        assertEquals(1.0, columnB[0])
        assertEquals(2.0, columnB[1])
        assertTrue(columnB[2].isNaN())
        assertEquals(emptyList(), source.getColumn("missing"))

        val row = assertIs<GMResult.Ok<List<Any?>>>(
            source.getRow("r2"),
        ).value
        assertTrue((row[0] as Double).isNaN())
        assertEquals("2x", row[1])
    }

    @Test
    fun explicitHeadersTableAdapterAndFailuresAreStructured() {
        val source = DataSource()
        val loaded = source.loadFromTable(
            tableId = "table",
            tableProvider = { id ->
                if (id == "table") {
                    listOf(
                        listOf("1", "2"),
                        listOf("3", "4"),
                    )
                } else {
                    null
                }
            },
            columnHeader = DataSourceHeader.Explicit(listOf("X", "Y")),
            rowHeader =
                DataSourceHeader.Explicit(listOf("first", "second")),
        )

        assertIs<GMResult.Ok<DataSource>>(loaded)
        assertEquals(listOf("X", "Y"), source.columnHeaders)
        assertEquals(listOf("first", "second"), source.rowHeaders)
        assertEquals(
            listOf(listOf(1.0, 2.0), listOf(3.0, 4.0)),
            source.data,
        )
        assertIs<GMResult.Err<DataSourceError.TableNotFound>>(
            source.loadFromTable(
                tableId = "missing",
                tableProvider = { null },
            ),
        )
        assertIs<GMResult.Err<DataSourceError.UnknownRow>>(
            source.getRow("missing"),
        )
        assertIs<GMResult.Err<DataSourceError.RowIndexOutOfBounds>>(
            source.getRow(2),
        )
        assertIs<GMResult.Err<DataSourceError.UnsupportedOperation>>(
            source.addColumn(),
        )
        assertIs<GMResult.Err<DataSourceError.UnsupportedOperation>>(
            source.addRow(),
        )
    }

    @Test
    fun canonicalJavaScriptNumberSpellingsAreConvertedExactly() {
        val source = DataSource()
        assertIs<GMResult.Ok<DataSource>>(
            source.loadFromArray(
                table = listOf(
                    listOf(
                        "-0",
                        "1e-7",
                        "1e+21",
                        "1e21",
                        "01",
                        "0.000001",
                        "1e-6",
                        "100000000000000000000",
                    ),
                ),
            ),
        )

        assertEquals("-0", source.data[0][0])
        assertEquals(1.0e-7, source.data[0][1])
        assertEquals(1.0e21, source.data[0][2])
        assertEquals("1e21", source.data[0][3])
        assertEquals("01", source.data[0][4])
        assertEquals(1.0e-6, source.data[0][5])
        assertEquals("1e-6", source.data[0][6])
        assertEquals(1.0e20, source.data[0][7])
    }

    @Test
    fun emptyEmbeddedColumnHeaderFailsWithoutReplacingExistingState() {
        val source = DataSource()
        assertIs<GMResult.Ok<DataSource>>(
            source.loadFromArray(
                table = listOf(listOf("1")),
                columnHeader =
                    DataSourceHeader.Explicit(listOf("existing")),
            ),
        )

        assertIs<GMResult.Err<DataSourceError.MissingColumnHeaderRow>>(
            source.loadFromArray(
                table = emptyList(),
                columnHeader = DataSourceHeader.Embedded,
            ),
        )
        assertEquals(listOf("existing"), source.columnHeaders)
        assertEquals(listOf(listOf(1.0)), source.data)
    }
}
