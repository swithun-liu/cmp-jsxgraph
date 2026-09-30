/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphForeignObjectTest {
    @Test
    fun documentAndAliasExposeEquivalentOpaqueHtmlGeometry() {
        val document = scene(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "overlay",
                      "type": "foreignobject",
                      "parents": ["<b>content</b>", [-3, -2], [3, 2]],
                      "attributes": {
                        "name": "",
                        "evaluateOnlyOnce": true,
                        "layer": 4
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )
        val jessieCode = jessieScene(
            JsxGraphJessieCode.parse(
                """
                fo("<b>content</b>", [-3, -2], [3, 2]) <<
                    id: "overlay",
                    name: "",
                    evaluateOnlyOnce: true,
                    layer: 4
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(document, jessieCode)
        val foreignObject = foreignObject(document)
        assertEquals("<b>content</b>", foreignObject.content)
        assertEquals(JsxGraphPoint2D(-3.0, -2.0), foreignObject.anchor)
        assertEquals(
            JsxGraphForeignObjectSize.UserSpace(3.0, 2.0),
            foreignObject.size,
        )
        assertEquals(true, foreignObject.evaluateOnlyOnce)
        assertEquals(4, foreignObject.style.layer)
        assertEquals(
            listOf(
                JsxGraphUnsupportedRenderFeature(
                    elementId = "overlay",
                    elementType = "foreignobject",
                    capability =
                        JsxGraphUnsupportedRenderCapability.ARBITRARY_HTML,
                    reason =
                        "Pure Compose Canvas cannot execute SVG " +
                            "foreignObject HTML content.",
                ),
            ),
            document.unsupportedRenderFeatures,
        )
    }

    @Test
    fun omittedSizeRemainsExplicitlyContentIntrinsic() {
        val scene = jessieScene(
            JsxGraphJessieCode.parse(
                """
                foreignobject("<div>intrinsic</div>", [1, 2]) <<
                    id: "intrinsic", name: ""
                >>;
                """.trimIndent(),
            ),
        )

        assertIs<JsxGraphForeignObjectSize.ContentIntrinsic>(
            foreignObject(scene).size,
        )
        assertEquals(1, scene.unsupportedRenderFeatures.size)
    }

    @Test
    fun dynamicCoordinatesAndSizeUpdateWithoutExecutingHtml() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = jessieScene(
            session.execute(
                """
                driver = point(-2, -1) <<
                    id: "driver", name: "", withLabel: false
                >>;
                overlay = foreignobject(
                    "<span>dynamic</span>",
                    [
                        function () { return driver.X(); },
                        function () { return driver.Y(); }
                    ],
                    [
                        function () { return driver.X() + 4; },
                        function () { return driver.Y() + 3; }
                    ]
                ) <<
                    id: "overlay",
                    name: "",
                    needsRegularUpdate: true
                >>;
                """.trimIndent(),
            ),
        )
        assertForeignObject(
            foreignObject = foreignObject(initial),
            anchor = JsxGraphPoint2D(-2.0, -1.0),
            width = 2.0,
            height = 2.0,
        )

        val moved = interactionScene(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(1.0, 2.0),
            ),
        )
        assertForeignObject(
            foreignObject = foreignObject(moved),
            anchor = JsxGraphPoint2D(1.0, 2.0),
            width = 5.0,
            height = 5.0,
        )

        val resized = jessieScene(
            session.execute("overlay.setSize(4, 1.5);"),
        )
        assertForeignObject(
            foreignObject = foreignObject(resized),
            anchor = JsxGraphPoint2D(1.0, 2.0),
            width = 4.0,
            height = 1.5,
        )
    }

    @Test
    fun functionContentAndMalformedSizeFailStructurally() {
        for (
            source in listOf(
                """
                foreignobject(
                    function () { return "<b>function</b>"; },
                    [0, 0],
                    [1, 1]
                ) << id: "bad", name: "" >>;
                """.trimIndent(),
                """
                fo("<b>bad</b>", [0, 0], [1]) <<
                    id: "bad", name: ""
                >>;
                """.trimIndent(),
            )
        ) {
            val failure = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(source),
            )
            assertTrue(failure.error.message.contains("UnsupportedParents"))
        }
    }

    private fun assertForeignObject(
        foreignObject: JsxGraphSceneElement.ForeignObject,
        anchor: JsxGraphPoint2D,
        width: Double,
        height: Double,
    ) {
        assertEquals(anchor, foreignObject.anchor)
        assertEquals(
            JsxGraphForeignObjectSize.UserSpace(width, height),
            foreignObject.size,
        )
        assertEquals("<span>dynamic</span>", foreignObject.content)
    }

    private fun foreignObject(
        scene: JsxGraphScene,
    ): JsxGraphSceneElement.ForeignObject =
        assertIs<JsxGraphSceneElement.ForeignObject>(
            scene.elements.first {
                it is JsxGraphSceneElement.ForeignObject
            },
        )

    private fun scene(
        result: GMResult<JsxGraphScene, JsxGraphDocumentError>,
    ): JsxGraphScene = assertIs<GMResult.Ok<JsxGraphScene>>(result).value

    private fun jessieScene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value

    private fun interactionScene(
        result: GMResult<JsxGraphScene, JsxGraphInteractionError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value
}
