package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class JsxGraphCircle3DTest {
    @Test
    fun jsonCreatesProjectedCircleWithOfficialDefaults() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id": "circle",
                      "type": "circle3d",
                      "parents": [
                        "view",
                        [1, 2, 0],
                        [0, 0, 1],
                        -2
                      ],
                      "attributes": {
                        "name": "",
                        "strokeColor": "#0072B2",
                        "strokeWidth": 3,
                        "numberPointsHigh": 9
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val circle = curve(scene, "circle")
        assertEquals(9, circle.points.size)
        assertEquals(JsxGraphColor(0, 114, 178), circle.style.strokeColor)
        assertEquals(3.0, circle.style.strokeWidth)
        assertEquals(12, circle.style.layer)
    }

    @Test
    fun jessieCodeUpdatesDynamicCenterNormalAndRadius() {
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
                normalY = 0;
                normalZ = 1;
                normal = function() {
                    return [0, 0, normalY, normalZ];
                };
                circle3d(
                    view,
                    function() { return [x, 0, 0]; },
                    normal,
                    function() { return radius; }
                ) <<
                    id: "circle", name: "", withLabel: false,
                    numberPointsHigh: 17,
                    strokeColor: "#009E73", strokeWidth: 3
                >>;
                """.trimIndent(),
            ),
        ).value
        val initialCircle = curve(initial, "circle")
        assertEquals(16, initialCircle.points.size)

        val updated = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute(
                """
                x = 2;
                radius = -2;
                normalY = 1;
                normalZ = 0;
                """.trimIndent(),
            ),
        ).value
        val updatedCircle = curve(updated, "circle")
        assertEquals(16, updatedCircle.points.size)
        assertNotEquals(initialCircle.points, updatedCircle.points)
    }

    @Test
    fun nanRadiusHidesCircleWithoutRejectingDocument() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphJessieCode.parse(
                """
                view = view3d(
                    [-5, -4],
                    [10, 8],
                    [[-5, 5], [-4, 4], [-3, 5]]
                ) <<
                    id: "view", name: "", projection: "parallel",
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                circle3d(view, [0, 0, 0], [0, 0, 1], NaN) <<
                    id: "circle", name: "", withLabel: false,
                    numberPointsHigh: 9
                >>;
                """.trimIndent(),
            ),
        ).value

        assertEquals(false, curve(scene, "circle").style.visible)
    }

    @Test
    fun malformedNormalAndCurvePointLimitAreStructured() {
        val malformed = assertIs<GMResult.Err<JsxGraphDocumentError>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id": "circle",
                      "type": "circle3d",
                      "parents": ["view", [0, 0, 0], [0, 1], 1],
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
                    """
                    {
                      "id": "circle",
                      "type": "circle3d",
                      "parents": ["view", [0, 0, 0], [0, 0, 1], 1],
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
    }
}
