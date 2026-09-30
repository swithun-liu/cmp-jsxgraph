/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DumpTest {
    @Test
    fun scalarAndNestedSerializationMatchesOfficialFixture() {
        assertEquals("\"plain\"", Dump.str("plain"))
        assertEquals(
            "\"function (x) { return x; }\"",
            Dump.str("function (x) { return x; }"),
        )
        assertEquals(2, Dump.str(2))
        assertNull(Dump.str(null))

        val nested = linkedMapOf<String, Any?>(
            "text" to "a'b\"c\\d\nx",
            "integer" to 1,
            "decimal" to 1.25,
            "boolean" to true,
            "nil" to null,
            "array" to listOf(1, "x", null, false),
            "nested" to linkedMapOf("z" to 2),
        )
        assertEquals(
            "<<text: 'a\\'b\\\"c\\\\d\nx', integer: 1, " +
                "decimal: 1.25, boolean: true, nil: null, " +
                "array: [1,'x',null,false], nested: <<z: 2>> >> ",
            ok(Dump.toJCAN(nested)),
        )
        assertEquals("[]", ok(Dump.toJCAN(emptyList<Any?>())))
        assertEquals("<<>> ", ok(Dump.toJCAN(emptyMap<String, Any?>())))
        assertEquals("NaN", ok(Dump.toJCAN(Double.NaN)))
        assertEquals("Infinity", ok(Dump.toJCAN(Double.POSITIVE_INFINITY)))
        assertEquals("-Infinity", ok(Dump.toJCAN(Double.NEGATIVE_INFINITY)))
        assertEquals("0", ok(Dump.toJCAN(-0.0)))
        assertEquals("undefined", ok(Dump.toJCAN(DumpUndefined)))
    }

    @Test
    fun propertyAndParameterOrderMatchesOfficialFixture() {
        val properties = linkedMapOf<String, Any?>()
        properties["beta"] = 1
        properties["10"] = 2
        properties["2"] = 3
        properties["alpha"] = 4

        assertEquals(
            "<<2: 3, 10: 2, beta: 1, alpha: 4>> ",
            ok(Dump.toJCAN(properties)),
        )
        assertEquals(
            "1, 'x', null, [true,2]",
            ok(
                Dump.arrayToParamStr(
                    listOf(1, "x", null, listOf(true, 2)),
                ),
            ),
        )
    }

    @Test
    fun kotlinxJsonValuesUseTheSameJCANPath() {
        val value = Json.parseToJsonElement(
            """{"text":"x","number":1.0,"items":[true,null]}""",
        )
        assertEquals(
            "<<text: 'x', number: 1, items: [true,null]>> ",
            ok(Dump.toJCAN(value)),
        )
    }

    @Test
    fun invalidContainersAndResourceLimitsAreStructured() {
        assertIs<GMResult.Err<DumpError.InvalidLimit>>(
            Dump.toJCAN(
                value = null,
                limits = DumpLimits(maxDepth = -1),
            ),
        )
        assertIs<GMResult.Err<DumpError.DepthLimitExceeded>>(
            Dump.toJCAN(
                value = listOf(1),
                limits = DumpLimits(maxDepth = 0),
            ),
        )
        assertIs<GMResult.Err<DumpError.ValueLimitExceeded>>(
            Dump.toJCAN(
                value = listOf(1),
                limits = DumpLimits(maxValues = 1),
            ),
        )
        assertIs<GMResult.Err<DumpError.OutputLimitExceeded>>(
            Dump.toJCAN(
                value = emptyList<Any?>(),
                limits = DumpLimits(maxOutputLength = 1),
            ),
        )
        assertIs<GMResult.Err<DumpError.InvalidObjectKey>>(
            Dump.toJCAN(mapOf(1 to "value")),
        )
        assertIs<GMResult.Err<DumpError.UnsupportedValue>>(
            Dump.toJCAN(Unit),
        )
    }

    private fun ok(
        result: GMResult<String, DumpError>,
    ): String = assertIs<GMResult.Ok<String>>(result).value
}
