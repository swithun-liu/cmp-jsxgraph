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
    fun curveDocumentsUseTranslatedFactoriesAndPreservePathBreaks() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-3, 5, 3, -1],
                  "objects": [
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [0, 2],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "visible": false
                      }
                    },
                    {
                      "id": "graph",
                      "type": "functiongraph",
                      "parents": ["A.Y() * x * x", -2, 2],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "doAdvancedPlot": false,
                        "numberPointsHigh": 4
                      }
                    },
                    {
                      "id": "data",
                      "type": "curve",
                      "parents": [[-2, -1, 0], [1, 0]],
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

        val graph = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements[1],
        )
        assertEquals(1.0, graph.style.strokeWidth)
        assertEquals("round", graph.lineCap)
        assertEquals(
            listOf(
                JsxGraphPoint2D(-2.0, 8.0),
                JsxGraphPoint2D(-1.0, 2.0),
                JsxGraphPoint2D(0.0, 0.0),
                JsxGraphPoint2D(1.0, 2.0),
            ),
            graph.points,
        )

        val data = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements[2],
        )
        assertEquals(
            listOf(
                JsxGraphPoint2D(-2.0, 1.0),
                JsxGraphPoint2D(-1.0, 0.0),
                null,
            ),
            data.points,
        )
    }

    @Test
    fun polygonDocumentsPreserveFillBordersAndImplicitVertices() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-1, 4, 5, -1],
                  "objects": [
                    {
                      "id": "triangle",
                      "type": "polygon",
                      "parents": [[0, 0], [4, 0], [0, 3]],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "strokeWidth": 7,
                        "strokeOpacity": 0.4
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val polygon = assertIs<JsxGraphSceneElement.Polygon>(
            scene.elements.single(),
        )
        assertEquals(
            listOf(
                JsxGraphPoint2D(0.0, 0.0),
                JsxGraphPoint2D(4.0, 0.0),
                JsxGraphPoint2D(0.0, 3.0),
            ),
            polygon.vertices,
        )
        assertEquals(3, polygon.implicitVertices.size)
        assertTrue(polygon.withLines)
        assertEquals(
            JsxGraphColor(240, 228, 66),
            polygon.style.fillColor,
        )
        assertEquals(0.3, polygon.style.fillOpacity)
        assertEquals(JsxGraphColor(0, 114, 178), polygon.style.strokeColor)
        assertEquals(0.4, polygon.style.strokeOpacity)
        assertEquals(7.0, polygon.style.strokeWidth)
        assertEquals(
            JsxGraphColor(0, 114, 178),
            polygon.borderStyle.strokeColor,
        )
        assertEquals(1.0, polygon.borderStyle.strokeOpacity)
        assertEquals(1.0, polygon.borderStyle.strokeWidth)
        assertTrue(
            polygon.implicitVertices.all {
                it.style.fillColor == JsxGraphColor(213, 94, 0) &&
                    it.size == 3.0
            },
        )
    }

    @Test
    fun textDocumentsPreserveContentStyleAndAnchors() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "title",
                      "type": "text",
                      "parents": [-2, 3, "Plain text"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "strokeColor": "#16877A",
                        "strokeOpacity": 0.75,
                        "fontSize": 18,
                        "fontUnit": "px",
                        "anchorX": "middle",
                        "anchorY": "top",
                        "display": "internal",
                        "parse": false
                      }
                    },
                    {
                      "id": "value",
                      "type": "text",
                      "parents": [1, -2, 3.14159],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "formatNumber": true,
                        "digits": 2
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val title = assertIs<JsxGraphSceneElement.Text>(scene.elements[0])
        assertEquals(JsxGraphPoint2D(-2.0, 3.0), title.coordinates)
        assertEquals("Plain text", title.content)
        assertEquals(18.0, title.fontSize)
        assertEquals("middle", title.anchorX)
        assertEquals("top", title.anchorY)
        assertEquals(JsxGraphColor(22, 135, 122), title.style.strokeColor)
        assertEquals(0.75, title.style.strokeOpacity)

        val value = assertIs<JsxGraphSceneElement.Text>(scene.elements[1])
        assertEquals("3.14", value.content)
        assertEquals(12.0, value.fontSize)
        assertEquals("left", value.anchorX)
        assertEquals("middle", value.anchorY)
        assertEquals(JsxGraphColor(0, 0, 0), value.style.strokeColor)
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
                    """{"id":"a","type":"angle","parents":[0,0,1]}""",
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
        assertIs<JsxGraphDocumentError.ElementCreation>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"t",
                      "type":"text",
                      "parents":[0,0,"x_{1}"],
                      "attributes":{
                        "name":"",
                        "withLabel":false
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
                      "id":"t",
                      "type":"text",
                      "parents":[0,0,"rotated"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "parse":false,
                        "rotate":30
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.ElementCreation>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"f",
                      "type":"functiongraph",
                      "parents":["x * x",-2,2],
                      "attributes":{
                        "name":"",
                        "withLabel":false
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
        assertIs<JsxGraphDocumentError.InvalidLimits>(
            assertError(
                source = """{"boundingBox":[-1,1,1,-1],"objects":[]}""",
                limits = JsxGraphEngineLimits(maxCurvePoints = 0),
            ),
        )
        assertIs<JsxGraphDocumentError.InvalidLimits>(
            assertError(
                source = """{"boundingBox":[-1,1,1,-1],"objects":[]}""",
                limits = JsxGraphEngineLimits(maxPolygonVertices = 0),
            ),
        )
        assertIs<JsxGraphDocumentError.InvalidLimits>(
            assertError(
                source = """{"boundingBox":[-1,1,1,-1],"objects":[]}""",
                limits = JsxGraphEngineLimits(maxTextLength = 0),
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
        assertIs<JsxGraphDocumentError.CurvePointLimitExceeded>(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"f",
                      "type":"functiongraph",
                      "parents":["x",-1,1],
                      "attributes":{
                        "doAdvancedPlot":false,
                        "numberPointsHigh":4
                      }
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 3),
            ),
        )
        assertIs<JsxGraphDocumentError.PolygonVertexLimitExceeded>(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"polygon",
                      "type":"polygon",
                      "parents":[[0,0],[1,0],[1,1],[0,1]]
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxPolygonVertices = 3),
            ),
        )
        assertIs<JsxGraphDocumentError.TextLengthLimitExceeded>(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"text",
                      "type":"text",
                      "parents":[0,0,"four"]
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxTextLength = 3),
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
