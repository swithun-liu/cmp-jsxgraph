/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsxGraphElementOptionsTest {
    @Test
    fun pointFaceAliasesMatchNormalizePointFace() {
        val aliases = listOf(
            "cross" to "x",
            "x" to "x",
            "circle" to "o",
            "o" to "o",
            "square" to "[]",
            "[]" to "[]",
            "plus" to "+",
            "+" to "+",
            "divide" to "|",
            "|" to "|",
            "minus" to "-",
            "-" to "-",
            "diamond" to "<>",
            "<>" to "<>",
            "diamond2" to "<<>>",
            "<<>>" to "<<>>",
            "triangleup" to "^",
            "A" to "^",
            "a" to "^",
            "^" to "^",
            "triangledown" to "v",
            "v" to "v",
            "triangleleft" to "<",
            "<" to "<",
            "triangleright" to ">",
            ">" to ">",
        )
        val objects = aliases.mapIndexed { index, (alias, _) ->
            """
            {
              "id":"point$index",
              "type":"point",
              "parents":[$index,0],
              "attributes":{
                "name":"",
                "withLabel":false,
                "face":"$alias"
              }
            }
            """.trimIndent()
        }.joinToString(",")

        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(document(objects)),
        ).value

        assertEquals(
            aliases.map(Pair<String, String>::second),
            scene.elements
                .map { assertIs<JsxGraphSceneElement.Point>(it).face },
        )
    }

    @Test
    fun unsupportedPointFaceReturnsStructuredError() {
        val result = assertIs<GMResult.Err<JsxGraphDocumentError>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id":"point",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "face":"star"
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        val error = assertIs<
            JsxGraphDocumentError.UnsupportedAttributeValue,
            >(result.error)
        assertEquals("face", error.attribute)
        assertEquals("star", error.value)
    }

    @Test
    fun pointSizeUnitAndZoomFollowRendererSnapshotSemantics() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox":[-5,5,5,-5],
                  "axis":false,
                  "grid":false,
                  "showNavigation":true,
                  "objects":[
                    {
                      "id":"userZoom",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "size":2,
                        "sizeUnit":"user",
                        "zoom":true
                      }
                    },
                    {
                      "id":"screen",
                      "type":"point",
                      "parents":[1,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "size":2,
                        "sizeUnit":"screen",
                        "zoom":false
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val initialUser = point(session.scene, "userZoom")
        val initialScreen = point(session.scene, "screen")
        assertEquals("user", initialUser.sizeUnit)
        assertEquals(1.0, initialUser.zoomScale)
        assertEquals(20.0, initialUser.resolveSize(10.0, 10.0))
        assertEquals(20.0, initialUser.resolveHitSize(10.0, 10.0))
        assertEquals("screen", initialScreen.sizeUnit)
        assertEquals(2.0, initialScreen.resolveSize(10.0, 10.0))

        val zoomed = assertIs<
            GMResult.Ok<JsxGraphNavigationInteractionResult>,
            >(
            session.interactNavigation(JsxGraphNavigationAction.ZOOM_IN),
        ).value.scene
        val zoomedUser = point(zoomed, "userZoom")
        assertEquals(1.25, zoomedUser.zoomScale)
        assertEquals(31.25, zoomedUser.resolveSize(12.5, 12.5))
        assertEquals(25.0, zoomedUser.resolveHitSize(12.5, 12.5))
        assertEquals(1.0, point(zoomed, "screen").zoomScale)
    }

    @Test
    fun invalidPointSizeOptionsReturnStructuredErrors() {
        val invalidUnit = assertIs<GMResult.Err<JsxGraphDocumentError>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id":"point",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "sizeUnit":"world"
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        val unsupported = assertIs<
            JsxGraphDocumentError.UnsupportedAttributeValue,
            >(invalidUnit.error)
        assertEquals("sizeunit", unsupported.attribute)
        assertEquals("world", unsupported.value)

        val invalidZoom = assertIs<
            GMResult.Err<JsxGraphDocumentError.InvalidAttribute>,
            >(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id":"point",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "zoom":1
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).error
        assertEquals("zoom", invalidZoom.attribute)
        assertEquals("a boolean", invalidZoom.expected)
    }

    @Test
    fun textRotationIsPreservedForCanvasAndHtmlRendering() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id":"text",
                      "type":"text",
                      "parents":[1,2,"rotated"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "display":"internal",
                        "rotate":90
                      }
                    },
                    {
                      "id":"button",
                      "type":"button",
                      "parents":[-1,-2,"Run"],
                      "attributes":{
                        "name":"",
                        "rotate":45
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val text = assertIs<JsxGraphSceneElement.Text>(
            scene.elements.single { it.id == "text" },
        )
        assertEquals(90.0, text.rotationDegrees)
        assertEquals(JsxGraphPoint2D(1.0, 2.0), text.coordinates)
        assertIs<JsxGraphAffineTransform>(text.affineTransform)
        val button = assertIs<JsxGraphSceneElement.Text>(
            scene.elements.single { it.id == "button" },
        )
        assertEquals(45.0, button.rotationDegrees)
        assertIs<JsxGraphAffineTransform>(button.affineTransform)
        assertIs<JsxGraphHtmlControl.Button>(button.htmlControl)
    }

    @Test
    fun lineTouchpointDistancesFollowVisibleEndpointStyles() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id":"start",
                      "type":"point",
                      "parents":[-2,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "visible":true,
                        "size":10,
                        "strokeWidth":1
                      }
                    },
                    {
                      "id":"end",
                      "type":"point",
                      "parents":[2,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "visible":true,
                        "size":6,
                        "strokeWidth":2
                      }
                    },
                    {
                      "id":"line",
                      "type":"segment",
                      "parents":["start","end"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "margin":25,
                        "firstArrow":{"type":7},
                        "lastArrow":{"type":7},
                        "touchFirstPoint":true,
                        "touchLastPoint":true
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val line = assertIs<JsxGraphSceneElement.Line>(
            scene.elements.single { it.id == "line" },
        )
        assertEquals(11.0, line.touchFirstPointDistance)
        assertEquals(8.0, line.touchLastPointDistance)
    }

    @Test
    fun curveHandDrawingIsPreservedForCanvasRendering() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id":"curve",
                      "type":"curve",
                      "parents":[[-2,0,2],[-1,1,-1]],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "handDrawing":true,
                        "lineCap":"square",
                        "strokeWidth":2
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val curve = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements.single(),
        )
        assertEquals(true, curve.handDrawing)
        assertEquals(1, curve.bezierDegree)
        assertEquals("square", curve.lineCap)
        assertEquals(3, curve.points.size)
    }

    private fun point(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Point =
        assertIs(
            scene.elements.single { it.id == id },
        )

    private fun document(objects: String): String =
        """
        {
          "boundingBox":[-5,5,5,-5],
          "axis":false,
          "grid":false,
          "objects":[$objects]
        }
        """.trimIndent()
}
