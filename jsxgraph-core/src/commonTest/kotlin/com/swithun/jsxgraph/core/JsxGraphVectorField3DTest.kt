package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class JsxGraphVectorField3DTest {
    @Test
    fun jessieCodeCreatesDynamicFieldAsCurve3D() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    containerId = "vectorfield3d",
                    boundingBox =
                        JsxGraphBoundingBox(-6.0, 5.0, 6.0, -5.0),
                ),
            ),
        ).value
        val initial = jessieScene(
            session.execute(
                """
                driver = point(1, 0) <<
                    id: "driver", name: "", withLabel: false
                >>;
                view = view3d(
                    [-5, -4],
                    [10, 8],
                    [[-5, 5], [-4, 4], [-3, 5]]
                ) <<
                    id: "view", name: "", projection: "parallel",
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                vectorfield3d(
                    view,
                    function (x, y, z) {
                        return [driver.X(), 0, z];
                    },
                    [
                        0,
                        function () { return driver.X(); },
                        1
                    ],
                    [0, 0, 0],
                    [0, 1, 1]
                ) <<
                    id: "field", name: "", withLabel: false,
                    scale: function () {
                        return driver.X() < 2 ? 0.5 : 1;
                    },
                    arrowHead: <<
                        enabled: function () {
                            return driver.X() < 2;
                        },
                        size: 5,
                        angle: PI * 0.125
                    >>
                >>;
                """.trimIndent(),
            ),
        )
        val initialField = curve(initial, "field")
        val initialVectorField = assertIs<JsxGraphVectorField3D>(
            initialField.vectorField3D,
        )

        assertEquals(4, initialVectorField.vectors.size)
        assertTrue(initialVectorField.arrowEnabled)
        assertEquals(5.0, initialVectorField.arrowSize)
        assertEquals(1.0, initialField.style.strokeWidth)
        assertEquals(12, initialField.style.layer)
        assertEquals(
            28,
            initialField.resolvePoints(
                cssPixelsPerUnitX = 53.333333333333336,
                cssPixelsPerUnitY = 48.0,
            ).size,
        )

        val updated = interactionScene(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(2.0, 0.0),
            ),
        )
        val updatedField = curve(updated, "field")
        val updatedVectorField = assertIs<JsxGraphVectorField3D>(
            updatedField.vectorField3D,
        )
        assertEquals(6, updatedVectorField.vectors.size)
        assertFalse(updatedVectorField.arrowEnabled)
        assertEquals(
            18,
            updatedField.resolvePoints(
                cssPixelsPerUnitX = 53.333333333333336,
                cssPixelsPerUnitY = 48.0,
            ).size,
        )
        assertNotEquals(initialField.points, updatedField.points)
    }

    @Test
    fun dynamicPointLimitRollsBackInteraction() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                limits = JsxGraphJessieCodeLimits(
                    maxCurvePoints = 250,
                ),
            ),
        ).value
        val initial = jessieScene(
            session.execute(
                """
                driver = point(1, 0) <<
                    id: "driver", name: "", withLabel: false
                >>;
                view = view3d(
                    [-5, -4],
                    [10, 8],
                    [[-5, 5], [-4, 4], [-3, 5]]
                ) <<
                    id: "view", name: "", projection: "parallel",
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                vectorfield3d(
                    view,
                    ["1", "0", "z"],
                    [
                        0,
                        function () { return driver.X(); },
                        1
                    ],
                    [0, 0, 0],
                    [0, 1, 1]
                ) <<
                    id: "field", name: "", withLabel: false,
                    arrowHead: <<
                        enabled: function () {
                            return driver.X() >= 2;
                        }
                    >>
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(12, curve(initial, "field").points.size)

        val limit = assertIs<
            GMResult.Err<JsxGraphInteractionError.ResourceLimitExceeded>,
            >(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(30.0, 0.0),
            ),
        ).error
        assertEquals("curve point count", limit.resource)
        assertEquals(250, limit.limit)
        assertEquals(434, limit.requestedSize)
        assertEquals(
            JsxGraphPoint2D(1.0, 0.0),
            session.scene.elements
                .filterIsInstance<JsxGraphSceneElement.Point>()
                .single { it.id == "driver" }
                .coordinates,
        )
        assertEquals(12, curve(session.scene, "field").points.size)
    }

    @Test
    fun jsonCreatesArrayExpressionFieldAndResolvesViewportArrowGeometry() {
        val result = JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id": "field",
                      "type": "vectorfield3d",
                      "parents": [
                        "view",
                        "[1, 0, z]",
                        [0, 1, 1],
                        [0, 0, 0],
                        [0, 1, 1]
                      ],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "strokeColor": "#D55E00",
                        "arrowHead": {
                          "enabled": true,
                          "size": 8,
                          "angle": 0
                        }
                      }
                    }
                    """.trimIndent(),
                ),
            )
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value
        val field = curve(scene, "field")
        val vectorField = assertIs<JsxGraphVectorField3D>(
            field.vectorField3D,
        )
        assertEquals(4, vectorField.vectors.size)
        assertEquals(JsxGraphColor(213, 94, 0), field.style.strokeColor)
        assertEquals(28, field.resolvePoints(40.0, 20.0).size)
        assertEquals(28, field.resolvePoints(80.0, 40.0).size)
        assertEquals(
            null,
            field.resolvePoints(40.0, 20.0)[2],
        )
    }

    @Test
    fun documentPreflightsPointLimitAndRejectsMalformedInputs() {
        val pointLimit = assertIs<
            GMResult.Err<JsxGraphDocumentError.CurvePointLimitExceeded>,
            >(
            JsxGraphEngine.parse(
                source = document(FIELD_JSON),
                limits = JsxGraphEngineLimits(maxCurvePoints = 27),
            ),
        ).error
        assertEquals("field", pointLimit.id)
        assertEquals(28, pointLimit.actual)

        val malformed = assertIs<GMResult.Err<JsxGraphDocumentError>>(
            JsxGraphEngine.parse(
                document(
                    FIELD_JSON.replace(
                        "\"parents\": [\"view\", [\"1\", \"0\", \"z\"], " +
                            "[0, 1, 1], [0, 0, 0], [0, 1, 1]]",
                        "\"parents\": [\"view\", [\"1\", \"0\", \"z\"], " +
                            "[0, 1, 1], [0, 0, 0], [0, 1]]",
                    ),
                ),
            ),
        ).error
        assertTrue(malformed.toString().contains("vectorfield3d"))

        val unsupportedArrow = assertIs<
            GMResult.Err<JsxGraphDocumentError.UnsupportedAttribute>,
            >(
            JsxGraphEngine.parse(
                document(
                    FIELD_JSON.replace(
                        "\"arrowHead\": {\"enabled\": true}",
                        "\"arrowHead\": {\"enabled\": true, \"type\": 2}",
                    ),
                ),
            ),
        ).error
        assertEquals("arrowhead.type", unsupportedArrow.attribute)
    }

    private fun interactionScene(
        result: GMResult<JsxGraphScene, JsxGraphInteractionError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value

    private fun jessieScene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value

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

        const val FIELD_JSON =
            """
            {
              "id": "field",
              "type": "vectorfield3d",
              "parents": ["view", ["1", "0", "z"], [0, 1, 1], [0, 0, 0], [0, 1, 1]],
              "attributes": {
                "name": "",
                "withLabel": false,
                "arrowHead": {"enabled": true}
              }
            }
            """
    }
}
