/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphTapemeasureTest {
    @Test
    fun jessieCodeExpandsHelpersAndUpdatesValueAndLabel() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = jessieScene(
            session.execute(
                """
                t = tapemeasure([1, 2], [4, 6]) <<
                    id: "tape", name: "dist",
                    digits: 4, precision: 5,
                    point1: << id: "start" >>,
                    point2: << id: "end" >>,
                    label: << id: "label", digits: 3 >>,
                    ticks: << id: "ticks" >>
                >>;
                dependent = point(
                    function () { return t.Value(); },
                    -2
                ) <<
                    id: "dependent", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            listOf(
                "start",
                "end",
                "tape",
                "tapeLabel",
                "ticks",
                "dependent",
            ),
            initial.elements.map(JsxGraphSceneElement::id),
        )
        assertTrue(point(initial, "start").style.visible)
        assertTrue(point(initial, "start").draggable)
        assertTrue(point(initial, "end").style.visible)
        assertTrue(point(initial, "end").draggable)

        val tape = line(initial, "tape")
        assertEquals(JsxGraphPoint2D(1.0, 2.0), tape.point1)
        assertEquals(JsxGraphPoint2D(4.0, 6.0), tape.point2)
        assertFalse(tape.straightFirst)
        assertFalse(tape.straightLast)
        assertEquals(2.0, tape.style.strokeWidth)

        val label = text(initial, "tapeLabel")
        assertEquals("dist = 5.000", label.content)
        assertEquals(JsxGraphPoint2D(2.5, 4.0), label.coordinates)
        assertEquals(JsxGraphPoint2D(10.0, -10.0), label.screenOffset)

        val ticks = ticks(initial, "ticks").definition
        assertTrue(ticks.drawZero)
        assertTrue(ticks.insertTicks)
        assertFalse(ticks.drawLabels)
        assertEquals(0.1, ticks.ticksDistance)
        assertEquals(8.0, ticks.minorHeight)
        assertEquals(16.0, ticks.majorHeight)
        assertEquals(4, ticks.minorTicks)
        assertContentEquals(listOf(0.0, 1.0), ticks.tickEndings)
        assertEquals("middle", ticks.labelStyle.anchorX)
        assertEquals("top", ticks.labelStyle.anchorY)
        assertEquals(0.0, ticks.labelStyle.offsetX)
        assertEquals(-10.0, ticks.labelStyle.offsetY)
        assertEquals(
            JsxGraphPoint2D(5.0, -2.0),
            point(initial, "dependent").coordinates,
        )

        val moved = interactionScene(
            session.movePoint(
                id = "end",
                coordinates = JsxGraphPoint2D(7.0, 10.0),
            ),
        )
        assertEquals(
            JsxGraphPoint2D(7.0, 10.0),
            line(moved, "tape").point2,
        )
        assertEquals("dist = 10.000", text(moved, "tapeLabel").content)
        assertEquals(
            JsxGraphPoint2D(10.0, -2.0),
            point(moved, "dependent").coordinates,
        )
    }

    @Test
    fun constructionDocumentSupportsOptionalHelpersAndCountsExpansion() {
        val document =
            """
            {
              "boundingBox": [-5, 5, 5, -5],
              "objects": [{
                "id": "tape",
                "type": "tapemeasure",
                "parents": [[-3, 1], [3, 1]],
                "attributes": {
                  "name": "",
                  "withLabel": false,
                  "withTicks": false,
                  "point1": {"id": "start"},
                  "point2": {"id": "end"}
                }
              }]
            }
            """.trimIndent()
        val scene = documentScene(
            JsxGraphEngine.parse(
                source = document,
                limits = JsxGraphEngineLimits(maxObjects = 3),
            ),
        )

        assertEquals(
            listOf("start", "end", "tape"),
            scene.elements.map(JsxGraphSceneElement::id),
        )
        val tape = line(scene, "tape")
        assertFalse(tape.straightFirst)
        assertFalse(tape.straightLast)
        assertTrue(scene.elements.none { it is JsxGraphSceneElement.Text })
        assertTrue(scene.elements.none { it is JsxGraphSceneElement.Ticks })

        val limited = assertIs<
            GMResult.Err<JsxGraphDocumentError.ObjectLimitExceeded>,
            >(
            JsxGraphEngine.parse(
                source = document,
                limits = JsxGraphEngineLimits(maxObjects = 2),
            ),
        ).error
        assertEquals(3, limited.actual)
    }

    private fun point(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Point =
        assertIs(scene.elements.single { it.id == id })

    private fun line(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Line =
        assertIs(scene.elements.single { it.id == id })

    private fun text(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Text =
        assertIs(scene.elements.single { it.id == id })

    private fun ticks(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Ticks =
        assertIs(scene.elements.single { it.id == id })

    private fun jessieScene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value

    private fun documentScene(
        result: GMResult<JsxGraphScene, JsxGraphDocumentError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value

    private fun interactionScene(
        result: GMResult<JsxGraphScene, JsxGraphInteractionError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(result).value
}
