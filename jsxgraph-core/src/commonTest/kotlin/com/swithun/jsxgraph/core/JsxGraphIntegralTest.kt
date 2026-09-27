/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphIntegralTest {
    @Test
    fun constructionDocumentExpandsIntegralAndUpdatesDynamicBoundary() {
        val document =
            """
            {
              "boundingBox": [-5, 8, 5, -5],
              "objects": [
                {
                  "id": "A",
                  "type": "point",
                  "parents": [-2, 0],
                  "attributes": {"name": "", "withLabel": false}
                },
                {
                  "id": "source",
                  "type": "functiongraph",
                  "parents": ["x * x - 2", -5, 5],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "doAdvancedPlot": false,
                    "numberPointsHigh": 256
                  }
                },
                {
                  "id": "integral",
                  "type": "integral",
                  "parents": [["A.X()", 3], "source"],
                  "attributes": {
                    "name": "",
                    "fillColor": "#336699",
                    "fillOpacity": 0.45,
                    "curveLeft": {
                      "id": "curveLeft",
                      "name": "",
                      "visible": true
                    },
                    "baseLeft": {"id": "baseLeft", "name": ""},
                    "curveRight": {
                      "id": "curveRight",
                      "name": "",
                      "fillColor": "#cc3344"
                    },
                    "baseRight": {"id": "baseRight", "name": ""},
                    "label": {
                      "id": "integralLabel",
                      "name": "",
                      "fontSize": 18,
                      "digits": 4,
                      "offset": [12, -8]
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                source = document,
                limits = JsxGraphEngineLimits(maxObjects = 8),
            ),
        ).value

        assertEquals(
            listOf(
                "A",
                "source",
                "curveLeft",
                "baseLeft",
                "curveRight",
                "baseRight",
                "integral",
                "integralLabel",
            ),
            session.scene.elements.map(JsxGraphSceneElement::id),
        )
        assertFalse(point(session.scene, "curveLeft").style.visible)
        assertFalse(point(session.scene, "curveLeft").draggable)
        assertFalse(point(session.scene, "baseLeft").style.visible)
        assertTrue(point(session.scene, "curveRight").style.visible)
        assertTrue(point(session.scene, "curveRight").draggable)
        assertEquals(
            JsxGraphColor(204, 51, 68),
            point(session.scene, "curveRight").style.fillColor,
        )

        val integral = curve(session.scene, "integral")
        assertEquals(JsxGraphColor(51, 102, 153), integral.style.fillColor)
        assertEquals(0.45, integral.style.fillOpacity)
        assertEquals(0.0, integral.style.strokeWidth)
        assertEquals(0.0, integral.style.strokeOpacity)
        assertEquals(integral.points.first(), integral.points.last())
        assertEquals(JsxGraphPoint2D(-2.0, 0.0), integral.points.first())

        val label = text(session.scene, "integralLabel")
        assertEquals("\u222b = 1.6667", label.content)
        assertEquals(18.0, label.fontSize)
        assertEquals(JsxGraphPoint2D(3.0, 7.0), label.coordinates)
        assertEquals(JsxGraphPoint2D(12.0, -8.0), label.screenOffset)

        val moved = interactionScene(
            session.movePoint(
                id = "A",
                coordinates = JsxGraphPoint2D(-1.0, 0.0),
            ),
        )
        assertEquals(
            JsxGraphPoint2D(-1.0, -1.0),
            point(moved, "curveLeft").coordinates,
        )
        assertEquals(JsxGraphPoint2D(-1.0, 0.0), curve(moved, "integral").points.first())
        assertEquals("\u222b = 1.3333", text(moved, "integralLabel").content)

        val limited = assertIs<
            GMResult.Err<JsxGraphDocumentError.ObjectLimitExceeded>,
            >(
            JsxGraphEngine.parse(
                source = document,
                limits = JsxGraphEngineLimits(maxObjects = 7),
            ),
        ).error
        assertEquals(8, limited.actual)
    }

    @Test
    fun jessieCodeIntegralSupportsFixedBoundsReverseOrderAndYAxis() {
        val scene = jessieScene(
            JsxGraphJessieCode.parse(
                """
                f = functiongraph("x + 1", -5, 5) <<
                    id: "source", name: "",
                    doAdvancedPlot: false, numberPointsHigh: 128
                >>;
                integral(f, [3, -2]) <<
                    id: "reverse", name: "", withLabel: false,
                    curveLeft: << id: "left", name: "" >>,
                    baseLeft: << id: "leftBase", name: "" >>,
                    curveRight: << id: "right", name: "" >>,
                    baseRight: << id: "rightBase", name: "" >>
                >>;
                """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxObjects = 6),
            ),
        )

        assertEquals(
            listOf(
                "source",
                "left",
                "leftBase",
                "right",
                "rightBase",
                "reverse",
            ),
            scene.elements.map(JsxGraphSceneElement::id),
        )
        assertTrue(point(scene, "left").style.visible)
        assertTrue(point(scene, "left").draggable)
        assertTrue(point(scene, "right").style.visible)
        assertTrue(point(scene, "right").draggable)
        assertEquals(
            JsxGraphPoint2D(-2.0, 0.0),
            curve(scene, "reverse").points.first(),
        )

        val yAxis = jessieScene(
            JsxGraphJessieCode.parse(
                """
                c = curve(
                    "x * x - 1",
                    "x",
                    -2,
                    2
                ) <<
                    id: "source", name: "",
                    doAdvancedPlot: false, numberPointsHigh: 128
                >>;
                integral([-1.5, 1.5], c) <<
                    id: "vertical", name: "",
                    axis: "y", withLabel: true,
                    curveLeft: << id: "left", name: "" >>,
                    baseLeft: << id: "leftBase", name: "" >>,
                    curveRight: << id: "right", name: "" >>,
                    baseRight: << id: "rightBase", name: "" >>
                >>;
                """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxObjects = 6),
            ),
        )
        assertEquals(6, yAxis.elements.size)
        assertTrue(yAxis.elements.none { it is JsxGraphSceneElement.Text })
        assertEquals(
            0.0,
            point(yAxis, "leftBase").coordinates.x,
        )
        assertEquals(
            0.0,
            point(yAxis, "rightBase").coordinates.x,
        )

        val limitError = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                """
                f = functiongraph("x + 1", -5, 5) <<
                    id: "source", name: "",
                    doAdvancedPlot: false, numberPointsHigh: 64
                >>;
                integral([-2, 3], f) <<
                    id: "integral", name: "", withLabel: false
                >>;
                """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxObjects = 5),
            ),
        ).error
        assertTrue(
            limitError is JsxGraphJessieCodeError.ResourceLimitExceeded,
            limitError.toString(),
        )
        val limited = limitError
        assertEquals("created element count", limited.resource)
        assertEquals(6, limited.requestedSize)
    }

    private fun point(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Point =
        assertIs(scene.elements.single { it.id == id })

    private fun curve(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Curve =
        assertIs(scene.elements.single { it.id == id })

    private fun text(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Text =
        assertIs(scene.elements.single { it.id == id })

    private fun jessieScene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(result).value

    private fun interactionScene(
        result: GMResult<JsxGraphScene, JsxGraphInteractionError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(result).value
}
