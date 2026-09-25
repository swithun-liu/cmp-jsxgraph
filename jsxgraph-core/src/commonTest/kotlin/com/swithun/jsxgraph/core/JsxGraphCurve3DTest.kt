package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class JsxGraphCurve3DTest {
    @Test
    fun jsonCreatesDiscreteAndTransformedCurves() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    """
                    {
                      "id": "base",
                      "type": "curve3d",
                      "parents": [
                        "view",
                        [[-2, -1, 0], [0, 2, 1], [2, -1, 2]]
                      ],
                      "attributes": {
                        "name": "",
                        "strokeColor": "#0072B2",
                        "strokeWidth": 3
                      }
                    },
                    {
                      "id": "translation",
                      "type": "transform3d",
                      "parents": ["view", 1, 0, 2],
                      "attributes": {"type": "translate"}
                    },
                    {
                      "id": "moved",
                      "type": "curve3d",
                      "parents": ["view", "base", "translation"],
                      "attributes": {
                        "name": "",
                        "strokeColor": "#D55E00",
                        "strokeWidth": 2
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val curves = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .associateBy(JsxGraphSceneElement.Curve::id)
        assertEquals(3, curves.getValue("base").points.size)
        assertEquals(3, curves.getValue("moved").points.size)
        assertEquals(
            JsxGraphColor(0, 114, 178),
            curves.getValue("base").style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            curves.getValue("moved").style.strokeColor,
        )
        assertNotEquals(
            curves.getValue("base").points,
            curves.getValue("moved").points,
        )
    }

    @Test
    fun jessieCodeSamplesComponentFunctionsAndTracksSessionState() {
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
                helix = curve3d(
                    view,
                    function(u) { return cos(u); },
                    function(u) { return sin(u); },
                    function(u) { return height * u / PI; },
                    [0, 2 * PI]
                ) <<
                    id: "helix", name: "", withLabel: false,
                    numberPointsHigh: 9, doAdvancedPlot: false,
                    strokeColor: "#009E73", strokeWidth: 3
                >>;
                """.trimIndent(),
            ),
        ).value
        val initialCurve = curve(initial, "helix")
        assertEquals(9, initialCurve.points.size)

        val updated = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute("height = 2;"),
        ).value
        val updatedCurve = curve(updated, "helix")
        assertEquals(9, updatedCurve.points.size)
        assertNotEquals(initialCurve.points, updatedCurve.points)
    }

    @Test
    fun vectorFunctionUsesConfiguredSampleCount() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphJessieCode.parse(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) <<
                    id: "view", name: "", projection: "parallel",
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                curve3d(
                    view,
                    function(u) { return [u, u * u, -u]; },
                    [-2, 2]
                ) <<
                    id: "curve", name: "", withLabel: false,
                    numberPointsHigh: 5
                >>;
                """.trimIndent(),
            ),
        ).value

        assertEquals(5, curve(scene, "curve").points.size)
    }

    @Test
    fun jsonAndJessieCodeEnforceCurvePointLimit() {
        val jsonError = assertIs<
            GMResult.Err<JsxGraphDocumentError.CurvePointLimitExceeded>
            >(
            JsxGraphEngine.parse(
                source = document(
                    """
                    {
                      "id": "curve",
                      "type": "curve3d",
                      "parents": [
                        "view",
                        [[0, 0, 0], [1, 1, 1], [2, 0, 2]]
                      ],
                      "attributes": {"name": ""}
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 2),
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
                    curve3d(
                        view,
                        function(u) { return [u, u, u]; },
                        [0, 1]
                    ) <<
                        id: "curve", name: "",
                        numberPointsHigh: 5
                    >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxCurvePoints = 4),
            ),
        ).error
        val limit = assertIs<
            JsxGraphJessieCodeError.ResourceLimitExceeded
            >(jessieError)
        assertEquals("curve point count", limit.resource)
        assertEquals(5L, limit.requestedSize)
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
