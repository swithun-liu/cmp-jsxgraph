package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class JsxGraphPolygon3DTest {
    @Test
    fun jsonCreatesPolygonWithProjectedVerticesAndNestedStyles() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id": "polygon",
                      "type": "polygon3d",
                      "parents": [
                        "view",
                        [-3, -2, 0],
                        [3, -2, 1],
                        [2, 3, 2],
                        [-2, 3, 1]
                      ],
                      "attributes": {
                        "name": "",
                        "fillColor": "#F0E442",
                        "fillOpacity": 0.35,
                        "layer": 12,
                        "vertices": {
                          "name": "",
                          "withLabel": false,
                          "size": 5,
                          "strokeColor": "none",
                          "fillColor": "#FFFFFF",
                          "fixed": true
                        },
                        "borders": {
                          "strokeColor": "#49545D",
                          "strokeWidth": 3,
                          "layer": 11
                        }
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val polygon = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Polygon>()
            .single { it.id == "polygon" }
        assertEquals(4, polygon.vertices.size)
        assertEquals(4, polygon.implicitVertices.size)
        assertEquals(JsxGraphColor(240, 228, 66), polygon.style.fillColor)
        assertEquals(0.35, polygon.style.fillOpacity)
        assertEquals(12, polygon.style.layer)
        assertEquals(JsxGraphColor(73, 84, 93), polygon.borderStyle.strokeColor)
        assertEquals(3.0, polygon.borderStyle.strokeWidth)
        assertEquals(11, polygon.borderStyle.layer)
        assertTrue(polygon.implicitVertices.all {
            it.style.fillColor == JsxGraphColor(255, 255, 255) &&
                it.style.strokeColor == JsxGraphColor.Transparent &&
                it.size == 5.0 &&
                !it.draggable
        })
    }

    @Test
    fun jsonTransformedPolygonPreservesUpstreamDroppedFinalVertex() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id": "base",
                      "type": "polygon3d",
                      "parents": [
                        "view",
                        [-2, -1, 0],
                        [2, -1, 0],
                        [2, 3, 0],
                        [-2, 3, 0]
                      ],
                      "attributes": {
                        "name": "",
                        "fillColor": "red",
                        "vertices": {
                          "name": "",
                          "withLabel": false,
                          "visible": false
                        }
                      }
                    },
                    {
                      "id": "translation",
                      "type": "transform3d",
                      "parents": ["view", 1, 2, 3],
                      "attributes": {"type": "translate"}
                    },
                    {
                      "id": "moved",
                      "type": "polygon3d",
                      "parents": ["view", "base", "translation"],
                      "attributes": {
                        "name": "",
                        "fillColor": "blue",
                        "vertices": {
                          "name": "",
                          "withLabel": false,
                          "visible": false
                        }
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val polygons = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Polygon>()
            .associateBy(JsxGraphSceneElement.Polygon::id)
        val base = polygons.getValue("base")
        val moved = polygons.getValue("moved")
        assertEquals(4, base.vertices.size)
        assertEquals(3, moved.vertices.size)
        assertNotEquals(base.vertices.first(), moved.vertices.first())
    }

    @Test
    fun jessieCodeTracksPointAndFunctionVerticesAcrossSessionUpdates() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) <<
                    id: "view", name: "", projection: "parallel",
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                anchor = point3d(view, [-2, -1, 0]) <<
                    id: "anchor", name: "", withLabel: false
                >>;
                x = 2;
                polygon = polygon3d(
                    view,
                    anchor,
                    [x, -1, 0],
                    function() { return [x, 3, 1]; }
                ) <<
                    id: "polygon", name: "",
                    fillColor: "#F0E442", fillOpacity: 0.35,
                    vertices: <<
                        name: "", withLabel: false, visible: false
                    >>,
                    borders: <<
                        strokeColor: "#49545D", strokeWidth: 2
                    >>
                >>;
                """.trimIndent(),
            ),
        ).value
        val initialPolygon = initial.elements
            .filterIsInstance<JsxGraphSceneElement.Polygon>()
            .single { it.id == "polygon" }
        assertEquals(3, initialPolygon.vertices.size)
        assertEquals(2, initialPolygon.implicitVertices.size)

        val updated = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute("x = 4;"),
        ).value
        val updatedPolygon = updated.elements
            .filterIsInstance<JsxGraphSceneElement.Polygon>()
            .single { it.id == "polygon" }
        assertNotEquals(
            initialPolygon.vertices.drop(1),
            updatedPolygon.vertices.drop(1),
        )
    }

    @Test
    fun jsonValidatesTransformedVertexCountAfterResolvingBaseReference() {
        assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                source = document(
                    """
                    {
                      "id": "base",
                      "type": "polygon3d",
                      "parents": ["view", [0, 0, 0]],
                      "attributes": {"name": ""}
                    },
                    {
                      "id": "translation",
                      "type": "transform3d",
                      "parents": ["view", 1, 2, 3],
                      "attributes": {"type": "translate"}
                    },
                    {
                      "id": "moved",
                      "type": "polygon3d",
                      "parents": ["view", "base", "translation"],
                      "attributes": {"name": ""}
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxPolygonVertices = 1),
            ),
        )
    }

    @Test
    fun jsonAndJessieCodeEnforcePolygonVertexLimit() {
        val jsonError = assertIs<
            GMResult.Err<JsxGraphDocumentError.PolygonVertexLimitExceeded>
            >(
            JsxGraphEngine.parse(
                source = document(
                    """
                    {
                      "id": "polygon",
                      "type": "polygon3d",
                      "parents": [
                        "view",
                        [0, 0, 0],
                        [1, 0, 0],
                        [0, 1, 0]
                      ],
                      "attributes": {"name": ""}
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxPolygonVertices = 2),
            ),
        ).error
        assertEquals(3, jsonError.actual)

        val jessieError = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source =
                    """
                    view = view3d(
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                    ) <<
                        id: "view", name: "", projection: "parallel",
                        $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                    >>;
                    polygon3d(
                        view,
                        [0, 0, 0],
                        [1, 0, 0],
                        [0, 1, 0]
                    ) << id: "polygon", name: "" >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxPolygonVertices = 2),
            ),
        ).error
        val limit = assertIs<
            JsxGraphJessieCodeError.ResourceLimitExceeded
            >(jessieError)
        assertEquals("polygon vertex count", limit.resource)
        assertEquals(3L, limit.requestedSize)
    }

    private fun document(objects: String): String =
        """
        {
          "boundingBox": [-8, 8, 8, -8],
          "objects": [
            {
              "id": "view",
              "type": "view3d",
              "parents": [
                [-5, -4],
                [8, 7],
                [[-5, 5], [-4, 6], [-3, 7]]
              ],
              "attributes": {
                "name": "",
                "projection": "parallel",
                "axesPosition": "none",
                "xPlaneRear": {"visible": false, "type": "wireframe"},
                "yPlaneRear": {"visible": false, "type": "wireframe"},
                "zPlaneRear": {"visible": false, "type": "wireframe"},
                "depthOrder": {"enabled": false},
                "az": {"slider": {"visible": false, "start": 1}},
                "el": {"slider": {"visible": false, "start": 0.3}},
                "bank": {"slider": {"visible": false, "start": 0}}
              }
            },
            $objects
          ]
        }
        """.trimIndent()

    private companion object {
        const val HIDDEN_DEFAULT_AXES_ATTRIBUTES =
            """
            axesPosition: "none",
            xPlaneRear: << visible: false, type: "wireframe" >>,
            yPlaneRear: << visible: false, type: "wireframe" >>,
            zPlaneRear: << visible: false, type: "wireframe" >>,
            depthOrder: << enabled: false >>,
            az: << slider: << visible: false, start: 1 >> >>,
            el: << slider: << visible: false, start: 0.3 >> >>,
            bank: << slider: << visible: false, start: 0 >> >>
            """
    }
}
