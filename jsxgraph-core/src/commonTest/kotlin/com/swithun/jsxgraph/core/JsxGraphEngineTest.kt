/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphEngineTest {
    @Test
    fun sourceCreatesBoardElementsAndPortableScene() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(REFERENCE_SOURCE),
        ).value

        assertEquals(
            JsxGraphBoundingBox(-6.0, 5.0, 6.0, -5.0),
            scene.boundingBox,
        )
        assertTrue(scene.axis)
        assertTrue(scene.grid)
        assertTrue(scene.keepAspectRatio)
        assertEquals(4, scene.elements.size)

        val pointA = assertIs<JsxGraphSceneElement.Point>(
            scene.elements[0],
        )
        assertEquals("A", pointA.id)
        assertEquals(JsxGraphPoint2D(1.0, 2.0), pointA.coordinates)
        assertEquals(4.0, pointA.size)
        assertEquals(JsxGraphColor(111, 119, 128), pointA.style.fillColor)

        val pointB = assertIs<JsxGraphSceneElement.Point>(
            scene.elements[1],
        )
        assertEquals(JsxGraphPoint2D(3.0, 1.0), pointB.coordinates)

        val line = assertIs<JsxGraphSceneElement.Line>(
            scene.elements[2],
        )
        assertEquals(pointA.coordinates, line.point1)
        assertEquals(pointB.coordinates, line.point2)
        assertEquals(false, line.straightFirst)
        assertEquals(false, line.straightLast)
        assertEquals(2.5, line.style.strokeWidth)

        val circle = assertIs<JsxGraphSceneElement.Circle>(
            scene.elements[3],
        )
        assertEquals(pointA.coordinates, circle.center)
        assertEquals(2.5, circle.radius)
        assertEquals(JsxGraphColor(22, 135, 122), circle.style.strokeColor)
    }

    @Test
    fun coordinateParentsCreateOnlyRequestedSceneElements() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "line",
                      "type": "line",
                      "parents": [[-2, -1], [3, 2]],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "straightFirst": false,
                        "straightLast": false
                      }
                    },
                    {
                      "id": "circle",
                      "type": "circle",
                      "parents": [[1, 1], 2],
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

        assertEquals(2, scene.elements.size)
        assertEquals("line", scene.elements[0].id)
        assertEquals("circle", scene.elements[1].id)
        assertEquals(
            JsxGraphPoint2D(1.0, 1.0),
            assertIs<JsxGraphSceneElement.Circle>(
                scene.elements[1],
            ).center,
        )
    }

    @Test
    fun defaultsMatchTranslatedUpstreamOptions() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [0, 0],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    },
                    {
                      "id": "c",
                      "type": "circle",
                      "parents": ["A", 1],
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

        val point = assertIs<JsxGraphSceneElement.Point>(scene.elements[0])
        assertEquals(JsxGraphColor(213, 94, 0), point.style.strokeColor)
        assertEquals(JsxGraphColor(213, 94, 0), point.style.fillColor)
        assertEquals(3.0, point.size)
        val circle = assertIs<JsxGraphSceneElement.Circle>(scene.elements[1])
        assertEquals(JsxGraphColor(0, 114, 178), circle.style.strokeColor)
        assertEquals(JsxGraphColor.Transparent, circle.style.fillColor)
        assertEquals(2.0, circle.style.strokeWidth)
    }

    @Test
    fun malformedDocumentsReturnStructuredErrors() {
        assertIs<JsxGraphDocumentError.InvalidJson>(
            assertError("{"),
        )
        assertIs<JsxGraphDocumentError.InvalidField>(
            assertError("""{"boundingBox":[0, 1, 0, -1], "objects":[]}"""),
        )
        assertIs<JsxGraphDocumentError.UnsupportedSchemaVersion>(
            assertError(
                """
                {
                  "schemaVersion": 2,
                  "boundingBox": [-1, 1, 1, -1],
                  "objects": []
                }
                """.trimIndent(),
            ),
        )
        assertIs<JsxGraphDocumentError.UnsupportedField>(
            assertError(
                """
                {
                  "boundingBox": [-1, 1, 1, -1],
                  "objects": [],
                  "fallback": true
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun factoryAndRenderingFailuresRemainExplicit() {
        assertIs<JsxGraphDocumentError.DuplicateObjectId>(
            assertError(
                documentWithObjects(
                    """{"id":"A","type":"point","parents":[0,0]}""",
                    """{"id":"A","type":"point","parents":[1,1]}""",
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.UnsupportedElementType>(
            assertError(
                documentWithObjects(
                    """{"id":"p","type":"polygon","parents":[]}""",
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.ElementCreation>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"l",
                      "type":"line",
                      "parents":["missing-a","missing-b"]
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.UnsupportedAttribute>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "shadow":true
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.UnsupportedAttributeValue>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "face":"square"
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.UnsupportedAttributeValue>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"c",
                      "type":"circle",
                      "parents":[[0,0],2],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "center":{"visible":true}
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
    }

    @Test
    fun resourceLimitsRejectInputBeforeConstruction() {
        assertIs<JsxGraphDocumentError.InvalidLimits>(
            assertError(
                source = """{"boundingBox":[-1,1,1,-1],"objects":[]}""",
                limits = JsxGraphEngineLimits(maxJsonDepth = 0),
            ),
        )
        assertIs<JsxGraphDocumentError.SourceLengthExceeded>(
            assertError(
                source = """{"boundingBox":[-1,1,1,-1],"objects":[]}""",
                limits = JsxGraphEngineLimits(maxSourceLength = 8),
            ),
        )
        assertIs<JsxGraphDocumentError.JsonDepthExceeded>(
            assertError(
                source = """{"nested":[[[[]]]]}""",
                limits = JsxGraphEngineLimits(maxJsonDepth = 3),
            ),
        )
        assertIs<JsxGraphDocumentError.JsonValueLimitExceeded>(
            assertError(
                source = """{"boundingBox":[-1,1,1,-1],"objects":[]}""",
                limits = JsxGraphEngineLimits(maxJsonValues = 3),
            ),
        )
        assertIs<JsxGraphDocumentError.ObjectLimitExceeded>(
            assertError(
                source = documentWithObjects(
                    """{"id":"A","type":"point","parents":[0,0]}""",
                    """{"id":"B","type":"point","parents":[1,1]}""",
                ),
                limits = JsxGraphEngineLimits(maxObjects = 1),
            ),
        )
    }

    private fun assertError(
        source: String,
        limits: JsxGraphEngineLimits = JsxGraphEngineLimits(),
    ): JsxGraphDocumentError =
        assertIs<GMResult.Err<JsxGraphDocumentError>>(
            JsxGraphEngine.parse(source, limits),
        ).error

    private fun documentWithObjects(
        vararg objects: String,
    ): String =
        """
        {
          "boundingBox": [-5, 5, 5, -5],
          "objects": [${objects.joinToString(",")}]
        }
        """.trimIndent()

    private companion object {
        val REFERENCE_SOURCE: String = """
            {
              "schemaVersion": 1,
              "boundingBox": [-6, 5, 6, -5],
              "axis": true,
              "grid": true,
              "keepAspectRatio": true,
              "objects": [
                {
                  "id": "A",
                  "type": "point",
                  "parents": [1, 2],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "size": 4,
                    "strokeColor": "#6F7780",
                    "fillColor": "#6F7780"
                  }
                },
                {
                  "id": "B",
                  "type": "point",
                  "parents": ["A.X() + 2", "A.Y() - 1"],
                  "attributes": {
                    "name": "",
                    "withLabel": false
                  }
                },
                {
                  "id": "l",
                  "type": "line",
                  "parents": ["A", "B"],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "strokeWidth": 2.5,
                    "straightFirst": false,
                    "straightLast": false
                  }
                },
                {
                  "id": "c",
                  "type": "circle",
                  "parents": ["A", 2.5],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "strokeColor": "#16877A"
                  }
                }
              ]
            }
        """.trimIndent()
    }
}
