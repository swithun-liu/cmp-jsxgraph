/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsxGraphTurtleTest {
    @Test
    fun jessieCodeExpandsCurrentTurtleObjectsAndPenStyles() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                t = turtle([1, 2], 30) <<
                    id: "t", name: "",
                    strokeWidth: 2,
                    strokeColor: "#123456",
                    highlightStrokeColor: "#abcdef"
                >>;
                t.fd(4);
                """.trimIndent(),
            ),
        )

        assertEquals(4, initial.elements.size)
        assertTrue(initial.elements.none { it.id == "t" })
        val initialCurve = initial.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single()
        assertEquals(
            listOf(
                JsxGraphPoint2D(1.0, 2.0),
                JsxGraphPoint2D(4.464101615137755, 4.0),
            ),
            initialCurve.points,
        )
        assertEquals(JsxGraphColor(0x12, 0x34, 0x56), initialCurve.style.strokeColor)
        assertEquals(2.0, initialCurve.style.strokeWidth)
        assertTrue(
            initial.elements
                .filterIsInstance<JsxGraphSceneElement.Point>()
                .all { !it.style.visible },
        )
        val initialArrow = initial.elements
            .filterIsInstance<JsxGraphSceneElement.Line>()
            .single()
        assertTrue(initialArrow.style.visible)
        assertFalse(initialArrow.straightFirst)
        assertFalse(initialArrow.straightLast)
        assertNotNull(initialArrow.lastArrow)

        val hidden = scene(
            session.execute(
                """
                t.penColor("#654321");
                t.forward(1);
                t.hide();
                """.trimIndent(),
            ),
        )
        val hiddenCurves = hidden.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
        assertEquals(2, hiddenCurves.size)
        assertEquals(
            JsxGraphColor(0x65, 0x43, 0x21),
            hiddenCurves.last().style.strokeColor,
        )
        assertFalse(
            hidden.elements
                .filterIsInstance<JsxGraphSceneElement.Line>()
                .single()
                .style
                .visible,
        )

        val shown = scene(session.execute("t.st();"))
        assertEquals(3, shown.elements.count { it is JsxGraphSceneElement.Curve })
        assertTrue(
            shown.elements
                .filterIsInstance<JsxGraphSceneElement.Line>()
                .single()
                .style
                .visible,
        )

        val cleared = scene(session.execute("t.cs();"))
        assertEquals(4, cleared.elements.size)
        assertEquals(
            1,
            cleared.elements.count { it is JsxGraphSceneElement.Curve },
        )
        assertFalse(
            cleared.elements
                .filterIsInstance<JsxGraphSceneElement.Line>()
                .single()
                .style
                .visible,
        )
    }

    @Test
    fun dynamicTurtleObjectsAndPointsRespectSessionLimits() {
        val objectSession = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                limits = JsxGraphJessieCodeLimits(maxObjects = 4),
            ),
        ).value
        scene(
            objectSession.execute(
                "t = turtle() << id: \"t\", name: \"\" >>;",
            ),
        )
        val objectLimit =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                objectSession.execute("t.penDown();"),
            ).error
        val objectError =
            assertIs<JsxGraphJessieCodeError.ResourceLimitExceeded>(
                objectLimit,
            )
        assertEquals("created element count", objectError.resource)
        assertEquals(5, objectError.requestedSize)

        val pointSession = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                limits = JsxGraphJessieCodeLimits(maxCurvePoints = 2),
            ),
        ).value
        val pointLimit =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                pointSession.execute(
                    """
                    t = turtle() << id: "t", name: "" >>;
                    t.fd(1);
                    t.fd(1);
                    """.trimIndent(),
                ),
            ).error
        val pointError =
            assertIs<JsxGraphJessieCodeError.ResourceLimitExceeded>(
                pointLimit,
            )
        assertEquals("curve point count", pointError.resource)
        assertEquals(3, pointError.requestedSize)
    }

    private fun scene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value
}
