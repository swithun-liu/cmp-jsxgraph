/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphBoardExportTest {
    @Test
    fun liveJessieCodeBoardExportsAllOfficialFormats() {
        val session = jessieSession(
            """
            A = point(1, 2) <<
                id: "A", name: "A", withLabel: false,
                strokeColor: "#123456"
            >>;
            B = point(3, 4) <<
                id: "B", name: "", withLabel: false
            >>;
            s = segment(A, B) <<
                id: "s", name: "", withLabel: false
            >>;
            """.trimIndent(),
        )

        val jessie = export(
            session,
            JsxGraphBoardExportFormat.JessieCode,
        )
        assertTrue("s0 = point(1, 2)" in jessie, jessie)
        assertTrue("s2 = segment(\"A\", \"B\")" in jessie, jessie)
        assertTrue("\$board.setBoundingBox(" in jessie, jessie)

        val javaScript = export(
            session,
            JsxGraphBoardExportFormat.JavaScript,
        )
        assertTrue(
            "board.create(\"point\", [1, 2]," in javaScript,
            javaScript,
        )
        assertTrue(
            "board.create(\"segment\", [\"A\", \"B\"]," in javaScript,
            javaScript,
        )
        assertTrue("board.setBoundingBox(" in javaScript, javaScript)

        val root = Json.parseToJsonElement(
            export(session, JsxGraphBoardExportFormat.Json),
        ).jsonObject
        val elements = root.getValue("elements").jsonArray
        assertEquals(3, elements.size)
        val first = elements[0].jsonObject
        assertEquals("point", first.getValue("type").jsonPrimitive.content)
        assertEquals(
            listOf("1", "2"),
            first.getValue("parents").jsonArray.map {
                it.jsonPrimitive.content
            },
        )
        assertEquals(
            "point",
            first.getValue("properties")
                .jsonObject
                .getValue("elType")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            listOf("s"),
            first.getValue("children").jsonArray.map {
                it.jsonPrimitive.content
            },
        )
        assertEquals(
            listOf("A", "B"),
            elements[2]
                .jsonObject
                .getValue("ancestors")
                .jsonArray
                .map { it.jsonPrimitive.content },
        )
        assertTrue(root.getValue("props").jsonArray.isEmpty())
        assertTrue(root.getValue("methods").jsonArray.isEmpty())
        assertTrue(root.getValue("userLog").jsonArray.isEmpty())
    }

    @Test
    fun noAttributesAndLiveCoordinatesFollowCurrentBoardState() {
        val session = jessieSession(
            """
            A = point(1, 2) <<
                id: "A", name: "A", withLabel: false,
                strokeColor: "#123456"
            >>;
            """.trimIndent(),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("A", JsxGraphPoint2D(7.0, 8.0)),
        )

        val full = export(
            session,
            JsxGraphBoardExportFormat.JessieCode,
        )
        val minimal = export(
            session,
            JsxGraphBoardExportFormat.JessieCode,
            noAttributes = true,
        )
        assertTrue("point(7, 8)" in full, full)
        assertTrue("strokecolor" in full.lowercase(), full)
        assertFalse("strokecolor" in minimal.lowercase(), minimal)
        assertTrue("<<id: 'A', name: 'A'>>" in minimal, minimal)
    }

    @Test
    fun exportLimitsReturnStructuredFailures() {
        val session = jessieSession(
            """
            A = point(1, 2) << id: "A", name: "", withLabel: false >>;
            B = point(3, 4) << id: "B", name: "", withLabel: false >>;
            """.trimIndent(),
        )

        assertIs<
            GMResult.Err<JsxGraphBoardExportError.ElementLimitExceeded>,
            >(
            session.exportBoard(
                format = JsxGraphBoardExportFormat.Json,
                limits = JsxGraphBoardExportLimits(maxElements = 1),
            ),
        )
        assertIs<
            GMResult.Err<JsxGraphBoardExportError.OutputLimitExceeded>,
            >(
            session.exportBoard(
                format = JsxGraphBoardExportFormat.JavaScript,
                limits = JsxGraphBoardExportLimits(maxOutputLength = 8),
            ),
        )
        assertIs<
            GMResult.Err<JsxGraphBoardExportError.InvalidLimit>,
            >(
            session.exportBoard(
                format = JsxGraphBoardExportFormat.Json,
                limits = JsxGraphBoardExportLimits(maxDepth = -1),
            ),
        )
    }

    @Test
    fun functionParentsRetainSourceAcrossOfficialExportFormats() {
        val session = jessieSession(
            """
            f = functiongraph(
                function (x) { return x * x; },
                -2,
                2
            ) << id: "f", name: "" >>;
            """.trimIndent(),
        )

        val jessie = export(
            session,
            JsxGraphBoardExportFormat.JessieCode,
        )
        assertTrue(
            "curve(function (x) { return x; }, function (x)" in jessie,
            jessie,
        )
        assertTrue("return (x^2);" in jessie, jessie)

        val javaScript = export(
            session,
            JsxGraphBoardExportFormat.JavaScript,
        )
        assertTrue(
            "board.create(\"curve\", [function (x) { return x; }, " +
                "function (x)" in
                javaScript,
            javaScript,
        )
        assertTrue(
            "return Math.pow(x, 2);" in javaScript,
            javaScript,
        )

        val json = Json.parseToJsonElement(
            export(session, JsxGraphBoardExportFormat.Json),
        ).jsonObject
        assertEquals(
            "null",
            json.getValue("elements")
                .jsonArray[0]
                .jsonObject
                .getValue("parents")
                .jsonArray[0]
                .toString(),
        )
    }

    @Test
    fun jsonAndLegacySessionsExposeTheSameBoardExportContract() {
        val jsonSession = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "type": "point",
                      "id": "A",
                      "parents": [1, 2],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value
        val legacySession =
            assertIs<GMResult.Ok<JsxGraphJessieScriptSession>>(
                JsxGraphJessieScript.createSession("A(1,2) nolabel"),
            ).value

        assertTrue(
            "\"type\":\"point\"" in export(
                jsonSession,
                JsxGraphBoardExportFormat.Json,
            ),
        )
        assertTrue(
            "point(1, 2)" in export(
                legacySession,
                JsxGraphBoardExportFormat.JessieCode,
            ),
        )
    }

    private fun jessieSession(
        source: String,
    ): JsxGraphJessieCodeSession {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        assertIs<GMResult.Ok<JsxGraphScene>>(session.execute(source))
        return session
    }

    private fun export(
        session: JsxGraphJessieCodeSession,
        format: JsxGraphBoardExportFormat,
        noAttributes: Boolean = false,
    ): String =
        assertIs<GMResult.Ok<String>>(
            session.exportBoard(format, noAttributes),
        ).value

    private fun export(
        session: JsxGraphSession,
        format: JsxGraphBoardExportFormat,
    ): String =
        assertIs<GMResult.Ok<String>>(
            session.exportBoard(format),
        ).value

    private fun export(
        session: JsxGraphJessieScriptSession,
        format: JsxGraphBoardExportFormat,
    ): String =
        assertIs<GMResult.Ok<String>>(
            session.exportBoard(format),
        ).value
}
