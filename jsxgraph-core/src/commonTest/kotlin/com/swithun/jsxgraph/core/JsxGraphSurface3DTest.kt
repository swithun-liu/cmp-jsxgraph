package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class JsxGraphSurface3DTest {
    @Test
    fun jsonFunctionGraphExpressionCreatesWireframe() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id": "graph",
                      "type": "functiongraph3d",
                      "parents": [
                        "view",
                        "sin(x * y)",
                        [-2, 2],
                        [-2, 2]
                      ],
                      "attributes": {
                        "name": "",
                        "stepsU": 2,
                        "stepsV": 2,
                        "strokeColor": "#D55E00",
                        "strokeWidth": 2
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val graph = curve(scene, "graph")
        assertEquals(24, graph.points.size)
        assertEquals(JsxGraphColor(213, 94, 0), graph.style.strokeColor)
        assertTrue(graph.style.visible)
    }

    @Test
    fun jsonParametricComponentsCreateRectangleFaces() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id": "surface",
                      "type": "parametricsurface3d",
                      "parents": [
                        "view",
                        "u",
                        "v",
                        "u + v",
                        [-1, 1],
                        [-1, 1]
                      ],
                      "attributes": {
                        "name": "",
                        "type": "shader",
                        "tiling": "rectangle",
                        "stepsU": 2,
                        "stepsV": 2,
                        "polyhedron": {
                          "strokeWidth": 0.2,
                          "fillOpacity": 0.8,
                          "shader": {
                            "hue": 30,
                            "saturation": 80,
                            "minLightness": 45,
                            "maxLightness": 85
                          }
                        }
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val surface = curve(scene, "surface")
        val faces = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .filter {
                it.id != "surface" &&
                    it.points.size == 5 &&
                    it.style.strokeWidth == 0.2
            }
        assertTrue(!surface.style.visible)
        assertEquals(4, faces.size)
        assertTrue(faces.all { it.style.fillOpacity == 0.8 })
    }

    @Test
    fun jessieCodeDynamicFunctionGraphUpdatesSession() {
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
                height = 1;
                graph = functiongraph3d(
                    view,
                    function(x, y) { return height * (x + y); },
                    [-2, 2],
                    [-2, 2]
                ) <<
                    id: "graph", name: "", withLabel: false,
                    stepsU: 2, stepsV: 2,
                    strokeColor: "#009E73", strokeWidth: 2
                >>;
                """.trimIndent(),
            ),
        ).value
        val initialGraph = curve(initial, "graph")

        val updated = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute("height = 2;"),
        ).value
        val updatedGraph = curve(updated, "graph")

        assertEquals(24, updatedGraph.points.size)
        assertNotEquals(initialGraph.points, updatedGraph.points)
    }

    @Test
    fun jsonAndJessieCodeEnforceWireframePointLimit() {
        val jsonError = assertIs<
            GMResult.Err<JsxGraphDocumentError.CurvePointLimitExceeded>
            >(
            JsxGraphEngine.parse(
                source = document(
                    """
                    {
                      "id": "graph",
                      "type": "functiongraph3d",
                      "parents": ["view", "x + y", [-2, 2], [-2, 2]],
                      "attributes": {
                        "name": "",
                        "stepsU": 3,
                        "stepsV": 3
                      }
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 30),
            ),
        ).error
        assertEquals(40, jsonError.actual)

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
                    functiongraph3d(
                        view,
                        function(x, y) { return x + y; },
                        [-2, 2],
                        [-2, 2]
                    ) <<
                        id: "graph", name: "",
                        stepsU: 3, stepsV: 3
                    >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxCurvePoints = 30),
            ),
        ).error
        val limit = assertIs<
            JsxGraphJessieCodeError.ResourceLimitExceeded
            >(jessieError)
        assertEquals("curve point count", limit.resource)
        assertEquals(40L, limit.requestedSize)
    }

    private fun curve(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Curve =
        scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single { it.id == id }

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
