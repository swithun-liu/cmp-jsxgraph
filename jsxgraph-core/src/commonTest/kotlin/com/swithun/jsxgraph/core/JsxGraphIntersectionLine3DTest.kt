package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class JsxGraphIntersectionLine3DTest {
    @Test
    fun jessieCodeCreatesStyledPlaneIntersectionFromCreationSnapshot() {
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
                horizontal = plane3d(
                    view,
                    [0, 0, 0],
                    [1, 0, 0],
                    [0, 1, 0]
                ) <<
                    id: "horizontal", name: "", visible: false,
                    type: "wireframe"
                >>;
                verticalX = 1;
                vertical = plane3d(
                    view,
                    function() { return [verticalX, 0, 0]; },
                    [0, 1, 0],
                    [0, 0, 1]
                ) <<
                    id: "vertical", name: "", visible: false,
                    type: "wireframe"
                >>;
                intersectionline3d(view, horizontal, vertical) <<
                    id: "intersection", name: "", withLabel: false,
                    strokeColor: "#D55E00", strokeWidth: 4,
                    point1: << id: "helper1", name: "", visible: false >>,
                    point2: << id: "helper2", name: "", visible: false >>
                >>;
                """.trimIndent(),
            ),
        ).value
        val initialLine = line(initial, "intersection")

        assertNotEquals(initialLine.point1, initialLine.point2)
        assertEquals(JsxGraphColor(213, 94, 0), initialLine.style.strokeColor)
        assertEquals(4.0, initialLine.style.strokeWidth)
        assertEquals(false, initialLine.straightFirst)
        assertEquals(false, initialLine.straightLast)

        val updated = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute("verticalX = 2;"),
        ).value
        assertEquals(initialLine, line(updated, "intersection"))
    }

    @Test
    fun jsonDocumentCreatesIntersectionLine3D() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(document()),
        ).value
        val line = line(scene, "intersection")

        assertNotEquals(line.point1, line.point2)
        assertEquals(JsxGraphColor(0, 114, 178), line.style.strokeColor)
        assertEquals(3.0, line.style.strokeWidth)
    }

    private fun line(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Line =
        scene.elements
            .filterIsInstance<JsxGraphSceneElement.Line>()
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
              "id": "horizontal",
              "type": "plane3d",
              "parents": [
                "view",
                [0, 0, 0],
                [1, 0, 0],
                [0, 1, 0]
              ],
              "attributes": {
                "name": "",
                "visible": false,
                "type": "wireframe"
              }
            },
            {
              "id": "vertical",
              "type": "plane3d",
              "parents": [
                "view",
                [1, 0, 0],
                [0, 1, 0],
                [0, 0, 1]
              ],
              "attributes": {
                "name": "",
                "visible": false,
                "type": "wireframe"
              }
            },
            {
              "id": "intersection",
              "type": "intersectionline3d",
              "parents": ["view", "horizontal", "vertical"],
              "attributes": {
                "name": "",
                "withLabel": false,
                "strokeColor": "#0072B2",
                "strokeWidth": 3,
                "point1": {
                  "id": "helper1",
                  "name": "",
                  "visible": false
                },
                "point2": {
                  "id": "helper2",
                  "name": "",
                  "visible": false
                }
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
