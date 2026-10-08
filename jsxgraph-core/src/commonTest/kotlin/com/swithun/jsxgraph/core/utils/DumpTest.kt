/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Point
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

    @Test
    fun markersAndBoundingBoxMethodsMatchBoardTraversal() {
        val board = Board(
            originX = 100.0,
            originY = 100.0,
            unitX = 20.0,
            unitY = 20.0,
            canvasWidth = 200.0,
            canvasHeight = 200.0,
        )
        val first = assertIs<GMResult.Ok<Point>>(
            Point.create(board, doubleArrayOf(1.0, 2.0)),
        ).value
        val second = assertIs<GMResult.Ok<Point>>(
            Point.create(board, doubleArrayOf(3.0, 4.0)),
        ).value

        Dump.addMarkers(
            board,
            markers = listOf("dumped", "anonymous"),
            values = listOf(false, true, "ignored"),
        )
        assertEquals(false, first.dumpMarkers["dumped"])
        assertEquals(true, second.dumpMarkers["anonymous"])
        Dump.deleteMarkers(board, "dumped")
        assertEquals(null, first.dumpMarkers["dumped"])
        assertEquals(true, first.dumpMarkers["anonymous"])

        val methods = mutableListOf<DumpMethodCall>()
        assertEquals(
            methods,
            Dump.setBoundingBox(methods, board, "\$board"),
        )
        assertEquals(
            DumpMethodCall(
                obj = "\$board",
                method = "setBoundingBox",
                params = listOf(
                    listOf(-5.0, 5.0, 5.0, -5.0),
                    false,
                ),
            ),
            methods.single(),
        )
    }

    @Test
    fun attributeMinimizationAndPreparationMatchOfficialRules() {
        val minimized = assertIs<GMResult.Ok<Map<String, Any?>>>(
            Dump.minimizeObject(
                instance = linkedMapOf(
                    "strokecolor" to "blue",
                    "dash" to listOf(1, 2),
                    "size" to 4,
                    "custom" to true,
                ),
                elementDefaults = mapOf(
                    "strokeColor" to "blue",
                    "dash" to listOf(1, 2),
                ),
                defaults = listOf(mapOf("size" to 4)),
            ),
        ).value
        // Upstream's deleteAll flag only follows default properties. If every
        // default matches, unknown properties are discarded with the object.
        assertEquals(emptyMap(), minimized)

        val prepared = assertIs<GMResult.Ok<Map<String, Any?>>>(
            Dump.prepareAttributes(
                snapshot = DumpElementSnapshot(
                    id = "p1",
                    name = "A",
                    elType = "point",
                    attributes = linkedMapOf(
                        "strokecolor" to "blue",
                        "size" to 4,
                        "layer" to 9,
                        "draft" to false,
                        "label" to mapOf("fontsize" to 12),
                    ),
                    subs = mapOf(
                        "helper" to DumpElementSnapshot(
                            id = "h1",
                            name = "H",
                            elType = "point",
                            attributes = mapOf(
                                "strokecolor" to "red",
                                "size" to 4,
                            ),
                        ),
                    ),
                ),
                options = mapOf(
                    "elements" to mapOf("strokeColor" to "blue"),
                    "point" to mapOf("size" to 4),
                    "label" to mapOf("fontSize" to 12),
                    "layer" to mapOf("point" to 9),
                ),
            ),
        ).value

        assertEquals("p1", prepared["id"])
        assertEquals("A", prepared["name"])
        assertEquals(null, prepared["strokecolor"])
        assertEquals(null, prepared["size"])
        assertEquals(null, prepared["layer"])
        assertEquals(null, prepared["draft"])
        assertEquals(null, prepared["label"])
        assertEquals(
            mapOf(
                "strokecolor" to "red",
                "id" to "h1",
                "name" to "H",
            ),
            prepared["helper"],
        )
    }

    private fun ok(
        result: GMResult<String, DumpError>,
    ): String = assertIs<GMResult.Ok<String>>(result).value
}
