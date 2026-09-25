package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class JsxGraphPolyhedron3DTest {
    @Test
    fun jsonExpandsFacesWithCyclicAndPerFaceAttributes() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id": "solid",
                      "type": "polyhedron3d",
                      "parents": [
                        "view",
                        [
                          [-2, -1, 0],
                          [2, -1, 0],
                          [2, 3, 0],
                          [-2, 3, 1]
                        ],
                        [
                          [
                            [0, 1, 2],
                            {
                              "fillColor": "green",
                              "fillOpacity": 0.5,
                              "strokeWidth": 4
                            }
                          ],
                          [0, 2, 3]
                        ]
                      ],
                      "attributes": {
                        "name": "",
                        "fillColorArray": ["red", "blue"],
                        "fillOpacity": 0.6,
                        "layer": 12,
                        "shader": {"enabled": false}
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val faces = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .filter {
                it.style.fillColor in setOf(
                    JsxGraphColor(0, 128, 0),
                    JsxGraphColor(0, 0, 255),
                )
            }
        assertEquals(2, faces.size)
        val green = faces.single {
            it.style.fillColor == JsxGraphColor(0, 128, 0)
        }
        val blue = faces.single {
            it.style.fillColor == JsxGraphColor(0, 0, 255)
        }
        assertEquals(4, green.points.size)
        assertEquals(4.0, green.style.strokeWidth)
        assertEquals(0.5, green.style.fillOpacity)
        assertEquals(4, blue.points.size)
        assertEquals(1.0, blue.style.strokeWidth)
        assertEquals(0.6, blue.style.fillOpacity)
        assertTrue(faces.all { it.style.layer == 12 })
    }

    @Test
    fun jsonSupportsTransformedPolyhedronParents() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id": "base",
                      "type": "polyhedron3d",
                      "parents": [
                        "view",
                        [[0, 0, 0], [1, 0, 0], [0, 1, 0]],
                        [[0, 1, 2]]
                      ],
                      "attributes": {
                        "name": "",
                        "fillColorArray": ["red"]
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
                      "type": "polyhedron3d",
                      "parents": ["view", "base", "translation"],
                      "attributes": {
                        "name": "",
                        "fillColorArray": ["blue"]
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val base = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single { it.style.fillColor == JsxGraphColor(255, 0, 0) }
        val moved = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single { it.style.fillColor == JsxGraphColor(0, 0, 255) }
        assertEquals(4, base.points.size)
        assertEquals(4, moved.points.size)
        assertNotEquals(base.points.first(), moved.points.first())
    }

    @Test
    fun jsonAppliesFaceCountsToAllPublicResourceLimits() {
        val source = document(
            """
            {
              "id": "solid",
              "type": "polyhedron3d",
              "parents": [
                "view",
                [[0, 0, 0], [1, 0, 0], [0, 1, 0]],
                [[0, 1, 2], [0, 2]]
              ],
              "attributes": {"name": ""}
            }
            """.trimIndent(),
        )

        val objectsError = assertIs<
            GMResult.Err<JsxGraphDocumentError.ObjectLimitExceeded>
            >(
            JsxGraphEngine.parse(
                source,
                JsxGraphEngineLimits(maxObjects = 22),
            ),
        ).error
        assertEquals(23, objectsError.actual)

        val curveError = assertIs<
            GMResult.Err<JsxGraphDocumentError.CurvePointLimitExceeded>
            >(
            JsxGraphEngine.parse(
                source,
                JsxGraphEngineLimits(maxCurvePoints = 3),
            ),
        ).error
        assertEquals(4, curveError.actual)

        val polygonError = assertIs<
            GMResult.Err<JsxGraphDocumentError.PolygonVertexLimitExceeded>
            >(
            JsxGraphEngine.parse(
                source,
                JsxGraphEngineLimits(maxPolygonVertices = 2),
            ),
        ).error
        assertEquals(3, polygonError.actual)
    }

    @Test
    fun jessieCodeSupportsObjectVerticesFunctionsAndTransforms() {
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
                    id: "view",
                    name: "",
                    projection: "parallel",
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                driver = point3d(view, [-2, -1, 0]) <<
                    id: "driver",
                    name: "",
                    withLabel: false
                >>;
                x = 2;
                solid = polyhedron3d(
                    view,
                    <<
                        a: driver,
                        b: [function() { return x; }, -1, 0],
                        c: function() { return [x, 3, 0]; }
                    >>,
                    [
                        [
                            ["a", "b", "c"],
                            << fillColor: "green", strokeWidth: 3 >>
                        ],
                        ["a", "c"]
                    ]
                ) <<
                    id: "solid",
                    name: "",
                    fillColorArray: ["red", "blue"]
                >>;
                translation = transform3d(view, 1, 2, 3) <<
                    type: "translate"
                >>;
                moved = polyhedron3d(view, solid, translation) <<
                    id: "moved",
                    name: "",
                    fillColorArray: ["yellow"]
                >>;
                """.trimIndent(),
            ),
        ).value
        val initialGreen = initial.facesWithColor(
            JsxGraphColor(0, 128, 0),
        ).single()
        assertEquals(4, initialGreen.points.size)
        assertEquals(3.0, initialGreen.style.strokeWidth)
        assertEquals(
            2,
            initial.facesWithColor(JsxGraphColor(255, 255, 0)).size,
        )

        val updated = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute("x = 4;"),
        ).value
        val updatedGreen = updated.facesWithColor(
            JsxGraphColor(0, 128, 0),
        ).single()
        assertNotEquals(initialGreen.points, updatedGreen.points)
    }

    @Test
    fun jessieCodeCountsClosedFaceCurvePoints() {
        val error = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source =
                    """
                    view = view3d(
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                    ) <<
                        id: "view",
                        name: "",
                        projection: "parallel",
                        $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                    >>;
                    polyhedron3d(
                        view,
                        [[0, 0, 0], [1, 0, 0], [0, 1, 0]],
                        [[0, 1, 2]]
                    ) << id: "solid", name: "" >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxCurvePoints = 3),
            ),
        ).error
        val limit =
            assertIs<JsxGraphJessieCodeError.ResourceLimitExceeded>(error)
        assertEquals("curve point count", limit.resource)
        assertEquals(4, limit.requestedSize)
    }

    @Test
    fun jsonAppliesFixedLightnessShaderAsExactColor() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id": "solid",
                      "type": "polyhedron3d",
                      "parents": [
                        "view",
                        [[0, 0, 0], [1, 0, 0], [0, 1, 0]],
                        [[0, 1, 2]]
                      ],
                      "attributes": {
                        "name": "",
                        "fillColor": "blue",
                        "shader": {
                          "enabled": true,
                          "hue": 0,
                          "saturation": 100,
                          "minLightness": 50,
                          "maxLightness": 50
                        }
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val faceColors = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .map { it.style.fillColor }
            .filter {
                it == JsxGraphColor(255, 0, 0) ||
                    it == JsxGraphColor(0, 0, 255)
            }
        assertEquals(
            listOf(JsxGraphColor(255, 0, 0)),
            faceColors,
        )
    }

    @Test
    fun jsonOrdersFacesWithinPolyhedronByAscendingDepth() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id": "solid",
                      "type": "polyhedron3d",
                      "parents": [
                        "view",
                        [
                          [0, 0, 1],
                          [1, 0, 1],
                          [0, 1, 1],
                          [0, 0, -1],
                          [1, 0, -1],
                          [0, 1, -1]
                        ],
                        [
                          [[0, 1, 2], {"fillColor": "red"}],
                          [[3, 4, 5], {"fillColor": "blue"}]
                        ]
                      ],
                      "attributes": {"name": ""}
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val faceColors = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .map { it.style.fillColor }
            .filter {
                it == JsxGraphColor(255, 0, 0) ||
                    it == JsxGraphColor(0, 0, 255)
            }
        assertEquals(
            listOf(
                JsxGraphColor(0, 0, 255),
                JsxGraphColor(255, 0, 0),
            ),
            faceColors,
        )
    }

    private fun JsxGraphScene.facesWithColor(
        color: JsxGraphColor,
    ): List<JsxGraphSceneElement.Curve> =
        elements.filterIsInstance<JsxGraphSceneElement.Curve>()
            .filter { it.style.fillColor == color }

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
                "az": {"slider": {"start": 1}},
                "el": {"slider": {"start": 0.3}},
                "bank": {"slider": {"start": 0}},
                $HIDDEN_DEFAULT_AXES_JSON
              }
            },
            $objects
          ]
        }
        """.trimIndent()

    private companion object {
        val HIDDEN_DEFAULT_AXES_JSON =
            """
            "axesPosition": "none",
            "xPlaneRear": {"visible": false},
            "yPlaneRear": {"visible": false},
            "zPlaneRear": {"visible": false}
            """.trimIndent()

        val HIDDEN_DEFAULT_AXES_ATTRIBUTES =
            """
            axesPosition: "none",
            xPlaneRear: << visible: false >>,
            yPlaneRear: << visible: false >>,
            zPlaneRear: << visible: false >>
            """.trimIndent()
    }
}
