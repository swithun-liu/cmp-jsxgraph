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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsxGraphSlopeTriangleTest {
    @Test
    fun jessieCodeSlopeTriangleExpandsHelpersBordersAndLabel() {
        val scene = jessieScene(
            JsxGraphJessieCode.parse(
                """
                A = point(-3, -1) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(3, 2) <<
                    id: "B", name: "", withLabel: false
                >>;
                source = segment(A, B) <<
                    id: "source", name: "", withLabel: false
                >>;
                g = glider(0, 0.5, source) <<
                    id: "sourceGlider", name: "", withLabel: false
                >>;
                t = tangent(source, g) <<
                    id: "tangent", name: "", withLabel: false
                >>;
                slopetriangle(t) <<
                    id: "triangle", name: "",
                    digits: 3, prefix: "m=", suffix: "!",
                    fillColor: "#ff0000", fillOpacity: 0.4,
                    basepoint: << id: "base", name: "" >>,
                    baseline: << id: "baseline", name: "" >>,
                    glider: << id: "helper", name: "" >>,
                    toppoint: << id: "top", name: "" >>
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(14, scene.elements.size)
        assertEquals(
            listOf(
                "A",
                "B",
                "source",
                "sourceGlider",
                "tangent",
                "base",
                "baseline",
                "helper",
                "top",
            ),
            scene.elements.take(9).map(JsxGraphSceneElement::id),
        )
        val triangleIndex = scene.elements.indexOfFirst { it.id == "triangle" }
        assertEquals(12, triangleIndex)
        val triangle = assertIs<JsxGraphSceneElement.Polygon>(
            scene.elements[triangleIndex],
        )
        assertFalse(triangle.withLines)
        assertEquals(JsxGraphColor(255, 0, 0), triangle.style.fillColor)
        assertEquals(0.4, triangle.style.fillOpacity)
        assertEquals(
            listOf(
                JsxGraphPoint2D(0.0, 0.5),
                JsxGraphPoint2D(1.0, 0.5),
                JsxGraphPoint2D(1.0, 1.0),
            ),
            triangle.vertices,
        )

        assertFalse(point(scene, "base").style.visible)
        assertFalse(line(scene, "baseline").style.visible)
        assertFalse(point(scene, "helper").style.visible)
        assertFalse(point(scene, "top").style.visible)

        val borders = scene.elements
            .subList(9, 12)
            .map { assertIs<JsxGraphSceneElement.Line>(it) }
        assertNotNull(borders[0].lastArrow)
        assertNull(borders[1].lastArrow)
        assertNotNull(borders[2].lastArrow)
        assertTrue(borders.all { !it.straightFirst && !it.straightLast })

        val label = assertIs<JsxGraphSceneElement.Text>(
            scene.elements[13],
        )
        assertEquals("m=0.500!", label.content)
        assertEquals(JsxGraphPoint2D(1.0, 0.75), label.coordinates)
        assertEquals(JsxGraphPoint2D(10.0, 0.0), label.screenOffset)
    }

    @Test
    fun sourceGliderInteractionUpdatesTriangleAndLabel() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    boundingBox =
                        JsxGraphBoundingBox(-5.0, 5.0, 5.0, -5.0),
                ),
            ),
        ).value
        val initial = jessieScene(
            session.execute(
                """
                A = point(-3, -1) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(3, 2) <<
                    id: "B", name: "", withLabel: false
                >>;
                source = segment(A, B) <<
                    id: "source", name: "", withLabel: false
                >>;
                g = glider(0, 0.5, source) <<
                    id: "sourceGlider", name: "", withLabel: false
                >>;
                slopetriangle(source, g) <<
                    id: "triangle", name: "",
                    prefix: "m=",
                    basepoint: << id: "base", name: "" >>,
                    baseline: << id: "baseline", name: "" >>,
                    glider: << id: "helper", name: "" >>,
                    toppoint: << id: "top", name: "" >>
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals("m=0.50", text(initial).content)

        val moved = interactionScene(
            session.movePoint(
                id = "B",
                coordinates = JsxGraphPoint2D(3.0, 5.0),
            ),
        )
        assertEquals("m=1.00", text(moved).content)
        assertEquals(
            JsxGraphPoint2D(1.0, 3.0),
            polygon(moved).vertices[2],
        )
    }

    @Test
    fun documentCountsNineLinePointOutputsAndTenGliderOutputs() {
        val document =
            """
            {
              "boundingBox": [-5, 5, 5, -5],
              "objects": [
                {
                  "id": "A",
                  "type": "point",
                  "parents": [-3, -1],
                  "attributes": {"name": "", "withLabel": false}
                },
                {
                  "id": "B",
                  "type": "point",
                  "parents": [3, 2],
                  "attributes": {"name": "", "withLabel": false}
                },
                {
                  "id": "source",
                  "type": "segment",
                  "parents": ["A", "B"],
                  "attributes": {"name": "", "withLabel": false}
                },
                {
                  "id": "sourceGlider",
                  "type": "glider",
                  "parents": [0, 0.5, "source"],
                  "attributes": {"name": "", "withLabel": false}
                },
                {
                  "id": "triangle",
                  "type": "slopetriangle",
                  "parents": ["source", "sourceGlider"],
                  "attributes": {
                    "name": "",
                    "basepoint": {"id": "base", "name": ""},
                    "baseline": {"id": "baseline", "name": ""},
                    "glider": {"id": "helper", "name": ""},
                    "toppoint": {"id": "top", "name": ""}
                  }
                }
              ]
            }
            """.trimIndent()
        val scene = documentScene(
            JsxGraphEngine.parse(
                document,
                limits = JsxGraphEngineLimits(maxObjects = 13),
            ),
        )
        assertEquals(13, scene.elements.size)

        val limited = assertIs<
            GMResult.Err<JsxGraphDocumentError.ObjectLimitExceeded>,
            >(
            JsxGraphEngine.parse(
                document,
                limits = JsxGraphEngineLimits(maxObjects = 12),
            ),
        ).error
        assertEquals(13, limited.actual)

        val gliderLimit = assertIs<
            GMResult.Err<JsxGraphJessieCodeError.ResourceLimitExceeded>,
            >(
            JsxGraphJessieCode.parse(
                """
                A = point(-3, -1);
                B = point(3, 2);
                source = segment(A, B);
                g = glider(0, 0.5, source);
                slopetriangle(g);
                """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxObjects = 13),
            ),
        ).error
        assertEquals("created element count", gliderLimit.resource)
        assertEquals(14, gliderLimit.requestedSize)
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

    private fun polygon(
        scene: JsxGraphScene,
    ): JsxGraphSceneElement.Polygon =
        assertIs(scene.elements.single { it.id == "triangle" })

    private fun text(
        scene: JsxGraphScene,
    ): JsxGraphSceneElement.Text =
        assertIs(scene.elements.single { it is JsxGraphSceneElement.Text })

    private fun jessieScene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(result).value

    private fun documentScene(
        result: GMResult<JsxGraphScene, JsxGraphDocumentError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(result).value

    private fun interactionScene(
        result: GMResult<JsxGraphScene, JsxGraphInteractionError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(result).value
}
