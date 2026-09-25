package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        assertEquals(
            JsxGraphFillGradient.Radial(
                secondColor = JsxGraphColor(0, 255, 128),
                secondOpacity = 1.0,
                startOffset = 0.0,
                endOffset = 1.0,
                centerX = 0.5,
                centerY = 0.5,
                radius = 0.5,
                focalX = 0.7,
                focalY = 0.3,
                focalRadius = 0.0,
            ),
            sphere.style.fillGradient,
        )
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
        assertNull(sphere.style.fillGradient)
        assertTrue(sphere.points.all { point ->
            val coordinate = assertNotNull(point)
            coordinate.x.isFinite() && coordinate.y.isFinite()
        })
    }

    @Test
    fun jsonParsesCustomLinearAndRadialGradients() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    projection = "parallel",
                    objects =
                        """
                        {
                          "id": "linear",
                          "type": "sphere3d",
                          "parents": ["view", [-2, 0, 0], 1],
                          "attributes": {
                            "name": "",
                            "gradient": "linear",
                            "gradientSecondColor": "#123456",
                            "gradientSecondOpacity": 0.6,
                            "gradientStartOffset": 0.1,
                            "gradientEndOffset": 0.9,
                            "gradientAngle": 1.25
                          }
                        },
                        {
                          "id": "radial",
                          "type": "sphere3d",
                          "parents": ["view", [2, 0, 0], 1],
                          "attributes": {
                            "name": "",
                            "gradient": "radial",
                            "gradientSecondColor": "#abcdef",
                            "gradientSecondOpacity": 0.7,
                            "gradientStartOffset": 0.2,
                            "gradientEndOffset": 0.8,
                            "gradientCX": 0.4,
                            "gradientCY": 0.6,
                            "gradientR": 0.45,
                            "gradientFX": 0.2,
                            "gradientFY": 0.3,
                            "gradientFR": 0.1
                          }
                        }
                        """.trimIndent(),
                ),
            ),
        ).value

        assertEquals(
            JsxGraphFillGradient.Linear(
                secondColor = JsxGraphColor(18, 52, 86),
                secondOpacity = 0.6,
                startOffset = 0.1,
                endOffset = 0.9,
                angle = 1.25,
            ),
            curve(scene, "linear").style.fillGradient,
        )
        assertEquals(
            JsxGraphFillGradient.Radial(
                secondColor = JsxGraphColor(171, 205, 239),
                secondOpacity = 0.7,
                startOffset = 0.2,
                endOffset = 0.8,
                centerX = 0.4,
                centerY = 0.6,
                radius = 0.45,
                focalX = 0.2,
                focalY = 0.3,
                focalRadius = 0.1,
            ),
            curve(scene, "radial").style.fillGradient,
        )
    }

    @Test
    fun invalidGradientTypeAndRangeAreStructured() {
        val typeResult = JsxGraphEngine.parse(
            document(
                projection = "parallel",
                objects =
                    """
                    {
                      "id": "sphere",
                      "type": "sphere3d",
                      "parents": ["view", [0, 0, 0], 1],
                      "attributes": {
                        "name": "",
                        "gradient": "conic"
                      }
                    }
                    """.trimIndent(),
            ),
        )
        val type = assertIs<
            JsxGraphDocumentError.UnsupportedAttributeValue
            >(
            assertIs<GMResult.Err<JsxGraphDocumentError>>(typeResult).error,
        )
        assertEquals("gradient", type.attribute)
        assertEquals("conic", type.value)

        val rangeResult = JsxGraphEngine.parse(
            document(
                projection = "parallel",
                objects =
                    """
                    {
                      "id": "sphere",
                      "type": "sphere3d",
                      "parents": ["view", [0, 0, 0], 1],
                      "attributes": {
                        "name": "",
                        "gradient": "radial",
                        "gradientFX": 1.1
                      }
                    }
                    """.trimIndent(),
            ),
        )
        val range = assertIs<JsxGraphDocumentError.InvalidAttribute>(
            assertIs<GMResult.Err<JsxGraphDocumentError>>(rangeResult).error,
        )
        assertEquals("gradientfx", range.attribute)
        assertEquals("a finite number in 0.0..1.0", range.expected)
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
