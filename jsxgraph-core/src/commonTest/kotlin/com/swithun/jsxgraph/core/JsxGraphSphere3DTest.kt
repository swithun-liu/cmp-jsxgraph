package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsxGraphSphere3DTest {
    @Test
    fun jsonCreatesParallelSphereWithOfficialDefaults() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    projection = "parallel",
                    objects =
                        """
                        {
                          "id": "sphere",
                          "type": "sphere3d",
                          "parents": ["view", [1, 2, 0], -2],
                          "attributes": {"name": ""}
                        }
                        """.trimIndent(),
                ),
            ),
        ).value

        val sphere = curve(scene, "sphere")
        assertEquals(13, sphere.points.size)
        assertEquals(JsxGraphColor(0, 255, 128), sphere.style.strokeColor)
        assertEquals(JsxGraphColor(255, 255, 255), sphere.style.fillColor)
        assertEquals(1.0, sphere.style.strokeWidth)
        assertEquals(0.4, sphere.style.fillOpacity)
        assertEquals(12, sphere.style.layer)
    }

    @Test
    fun jsonCreatesCentralEllipseFromTwoPoints() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    projection = "central",
                    objects =
                        """
                        {
                          "id": "center",
                          "type": "point3d",
                          "parents": ["view", [0, 0, 0]],
                          "attributes": {
                            "name": "",
                            "withLabel": false,
                            "visible": false
                          }
                        },
                        {
                          "id": "surfacePoint",
                          "type": "point3d",
                          "parents": ["view", [1.5, 0, 0]],
                          "attributes": {
                            "name": "",
                            "withLabel": false,
                            "visible": false
                          }
                        },
                        {
                          "id": "sphere",
                          "type": "sphere3d",
                          "parents": ["view", "center", "surfacePoint"],
                          "attributes": {
                            "name": "",
                            "numberPointsHigh": 17,
                            "gradient": "none"
                          }
                        }
                        """.trimIndent(),
                ),
            ),
        ).value

        val sphere = curve(scene, "sphere")
        assertEquals(17, sphere.points.size)
        assertTrue(sphere.points.all { point ->
            val coordinate = assertNotNull(point)
            coordinate.x.isFinite() && coordinate.y.isFinite()
        })
    }

    @Test
    fun jessieCodeUpdatesDynamicCenterAndRadius() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute(
                """
                view = view3d(
                    [-5, -4],
                    [10, 8],
                    [[-5, 5], [-4, 4], [-3, 5]]
                ) <<
                    id: "view", name: "", projection: "parallel",
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                x = -2;
                radius = 1;
                sphere3d(
                    view,
                    function() { return [x, 0, 0]; },
                    function() { return radius; }
                ) <<
                    id: "sphere", name: "", withLabel: false,
                    fillColor: "#56B4E9", fillOpacity: 0.4,
                    strokeColor: "#009E73", strokeWidth: 3,
                    gradient: "none"
                >>;
                """.trimIndent(),
            ),
        ).value
        val initialSphere = curve(initial, "sphere")

        val updated = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute(
                """
                x = 2;
                radius = -2;
                """.trimIndent(),
            ),
        ).value
        val updatedSphere = curve(updated, "sphere")

        assertEquals(13, updatedSphere.points.size)
        assertNotEquals(initialSphere.points, updatedSphere.points)
    }

    @Test
    fun stringRadiusAndFunctionPointAreSupported() {
        val result = JsxGraphJessieCode.parse(
                """
                view = view3d(
                    [-5, -4],
                    [10, 8],
                    [[-5, 5], [-4, 4], [-3, 5]]
                ) <<
                    id: "view", name: "", projection: "parallel",
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                center = point3d(view, [0, 0, 0]) <<
                    id: "center", name: "", visible: false
                >>;
                sphere3d(view, center, "2 + 1") <<
                    id: "radiusSphere", name: "",
                    gradient: "none"
                >>;
                sphere3d(
                    view,
                    center,
                    function() { return [0, 2, 0]; }
                ) <<
                    id: "pointSphere", name: "",
                    gradient: "none"
                >>;
                """.trimIndent(),
            )
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value

        assertEquals(2, scene.elements.count { it.id.endsWith("Sphere") })
        assertNotEquals(
            curve(scene, "radiusSphere").points,
            curve(scene, "pointSphere").points,
        )
    }

    @Test
    fun malformedParentAndCurvePointLimitAreStructured() {
        val malformed = assertIs<GMResult.Err<JsxGraphDocumentError>>(
            JsxGraphEngine.parse(
                document(
                    projection = "parallel",
                    objects =
                        """
                        {
                          "id": "sphere",
                          "type": "sphere3d",
                          "parents": ["view", [0, 0, 0], [0, 1]],
                          "attributes": {"name": ""}
                        }
                        """.trimIndent(),
                ),
            ),
        ).error
        assertIs<JsxGraphDocumentError.ElementCreation>(malformed)

        val limit = assertIs<
            GMResult.Err<JsxGraphDocumentError.CurvePointLimitExceeded>
            >(
            JsxGraphEngine.parse(
                source = document(
                    projection = "central",
                    objects =
                        """
                        {
                          "id": "sphere",
                          "type": "sphere3d",
                          "parents": ["view", [0, 0, 0], 1],
                          "attributes": {
                            "name": "",
                            "numberPointsHigh": 9
                          }
                        }
                        """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 8),
            ),
        ).error
        assertEquals(9, limit.actual)
    }

    private fun curve(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Curve =
        scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single { it.id == id }

    private fun document(
        projection: String,
        objects: String,
    ): String =
        """
        {
          "boundingBox": [-6, 5, 6, -5],
          "objects": [
            {
              "id": "view",
              "type": "view3d",
              "parents": [
                [-5, -4],
                [10, 8],
                [[-5, 5], [-4, 4], [-3, 5]]
              ],
              "attributes": {
                "name": "",
                "projection": "$projection",
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
