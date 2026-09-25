package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class JsxGraphIntersectionCircle3DTest {
    @Test
    fun jessieCodeSphereIntersectionUpdatesAndHidesWhenDisjoint() {
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
                leftX = -1;
                rightX = 1;
                left = sphere3d(
                    view,
                    function() { return [leftX, 0, 0]; },
                    2
                ) <<
                    id: "left", name: "", visible: false,
                    numberPointsHigh: 4, gradient: "none"
                >>;
                right = sphere3d(
                    view,
                    function() { return [rightX, 0, 0]; },
                    2
                ) <<
                    id: "right", name: "", visible: false,
                    numberPointsHigh: 4, gradient: "none"
                >>;
                intersectioncircle3d(view, left, right) <<
                    id: "intersection", name: "", withLabel: false,
                    numberPointsHigh: 17,
                    strokeColor: "#D55E00", strokeWidth: 4
                >>;
                """.trimIndent(),
            ),
        ).value
        val initialCircle = curve(initial, "intersection")
        assertEquals(16, initialCircle.points.size)
        assertEquals(true, initialCircle.style.visible)
        assertEquals(JsxGraphColor(213, 94, 0), initialCircle.style.strokeColor)
        assertEquals(4.0, initialCircle.style.strokeWidth)

        val updated = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute("rightX = 4;"),
        ).value
        val updatedCircle = curve(updated, "intersection")

        assertEquals(false, updatedCircle.style.visible)
        assertNotEquals(initialCircle.points, updatedCircle.points)
    }

    @Test
    fun jessieCodeCreatesPlaneSphereIntersectionInEitherOrder() {
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
                plane = plane3d(
                    view,
                    [0, 0, 1],
                    [1, 0, 0],
                    [0, 1, 0],
                    [-3, 3],
                    [-3, 3]
                ) <<
                    id: "plane", name: "", visible: false,
                    type: "wireframe"
                >>;
                sphere = sphere3d(view, [0, 0, 0], 2) <<
                    id: "sphere", name: "", visible: false,
                    numberPointsHigh: 4, gradient: "none"
                >>;
                intersectioncircle3d(view, plane, sphere) <<
                    id: "forward", name: "", withLabel: false,
                    numberPointsHigh: 17
                >>;
                intersectioncircle3d(view, sphere, plane) <<
                    id: "reverse", name: "", withLabel: false,
                    numberPointsHigh: 17
                >>;
                """.trimIndent(),
            ),
        ).value

        val forward = curve(scene, "forward")
        val reverse = curve(scene, "reverse")
        assertEquals(16, forward.points.size)
        assertEquals(forward.points, reverse.points)
    }

    @Test
    fun jsonCurvePointLimitIncludesIntersectionCircle() {
        val error = assertIs<
            GMResult.Err<JsxGraphDocumentError.CurvePointLimitExceeded>
            >(
            JsxGraphEngine.parse(
                source = document(),
                limits = JsxGraphEngineLimits(maxCurvePoints = 8),
            ),
        ).error

        assertEquals(3, error.objectIndex)
        assertEquals("intersection", error.id)
        assertEquals(9, error.actual)
    }

    private fun curve(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Curve =
        scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single { it.id == id }

    private fun document(): String =
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
            {
              "id": "left",
              "type": "sphere3d",
              "parents": ["view", [-1, 0, 0], 2],
              "attributes": {
                "name": "",
                "visible": false,
                "numberPointsHigh": 4
              }
            },
            {
              "id": "right",
              "type": "sphere3d",
              "parents": ["view", [1, 0, 0], 2],
              "attributes": {
                "name": "",
                "visible": false,
                "numberPointsHigh": 4
              }
            },
            {
              "id": "intersection",
              "type": "intersectioncircle3d",
              "parents": ["view", "left", "right"],
              "attributes": {
                "name": "",
                "numberPointsHigh": 9
              }
            }
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
