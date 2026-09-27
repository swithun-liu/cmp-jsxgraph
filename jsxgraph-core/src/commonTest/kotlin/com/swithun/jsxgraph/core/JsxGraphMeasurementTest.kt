/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsxGraphMeasurementTest {
    @Test
    fun jessieCodeMeasurementsComposeAndTrackGeometry() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = jessieScene(
            session.execute(
                """
                p1 = point(1, 1) <<
                    id: "p1", name: "", withLabel: false
                >>;
                p2 = point(1, 3) <<
                    id: "p2", name: "", withLabel: false
                >>;
                c = circle(p1, p2) <<
                    id: "circle", name: "", withLabel: false
                >>;
                seg = segment([-2, -3], [-2, 3]) <<
                    id: "segment", name: "", withLabel: false
                >>;
                s = slider([-4, 4], [-1.5, 4], [-10, 1, 10]) <<
                    id: "slider", name: "",
                    withLabel: false, withTicks: false
                >>;
                m1 = measurement(-6, -2, ["Radius", c]) <<
                    id: "radius", name: "",
                    prefix: "r=", baseUnit: "cm", digits: 3
                >>;
                m2 = measurement(-6, -4, ["L", seg]) <<
                    id: "length", name: "",
                    prefix: "l=", units: << dim1: " meter" >>,
                    digits: "auto"
                >>;
                m3 = measurement(-6, -6, ["V", s]) <<
                    id: "sliderValue", name: "",
                    prefix: "s=", baseUnit: "cm", dim: 1
                >>;
                total = measurement(
                    1, -4, ["+", ["V", m1], ["V", m2], ["V", m3]]
                ) <<
                    id: "total", name: "",
                    prefix: "sum=", suffix: "!",
                    baseUnit: "cm", digits: 2
                >>;
                probe = point(
                    function () { return total.Value(); },
                    function () { return total.Dimension(); }
                ) <<
                    id: "probe", name: "", withLabel: false
                >>;
                apiProbe = point(
                    function () {
                        return
                            IfThen(total.Unit(2) == "cm^{2}", 1, 0) +
                            IfThen(total.Method() == "+", 1, 0) +
                            IfThen(total.Term()[0] == "+", 1, 0) +
                            IfThen(total.toPrefix()[0] == "+", 1, 0);
                    },
                    function () { return total.Parents().length; }
                ) <<
                    id: "apiProbe", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals("r=2.000cm", text(initial, "radius").content)
        assertEquals("l=6.00 meter", text(initial, "length").content)
        assertEquals("s=1.00cm", text(initial, "sliderValue").content)
        assertEquals("sum=9.00cm!", text(initial, "total").content)
        assertEquals(
            JsxGraphPoint2D(9.0, 1.0),
            point(initial, "probe").coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(4.0, 3.0),
            point(initial, "apiProbe").coordinates,
        )

        val moved = interactionScene(
            session.movePoint(
                id = "p2",
                coordinates = JsxGraphPoint2D(1.0, 5.0),
            ),
        )
        assertEquals("r=4.000cm", text(moved, "radius").content)
        assertEquals("sum=11.00cm!", text(moved, "total").content)
        assertEquals(
            JsxGraphPoint2D(11.0, 1.0),
            point(moved, "probe").coordinates,
        )
    }

    @Test
    fun constructionDocumentResolvesPrefixElementIds() {
        val source =
            """
            {
              "boundingBox": [-5, 5, 5, -5],
              "objects": [
                {
                  "id": "center",
                  "type": "point",
                  "parents": [0, 0],
                  "attributes": {"name": "", "withLabel": false}
                },
                {
                  "id": "radiusPoint",
                  "type": "point",
                  "parents": [3, 0],
                  "attributes": {"name": "", "withLabel": false}
                },
                {
                  "id": "circle",
                  "type": "circle",
                  "parents": ["center", "radiusPoint"],
                  "attributes": {"name": "", "withLabel": false}
                },
                {
                  "id": "measurement",
                  "type": "measurement",
                  "parents": [-3, -2, ["Area", "circle"]],
                  "attributes": {
                    "name": "",
                    "prefix": "A=",
                    "baseUnit": " cm",
                    "digits": 2
                  }
                }
              ]
            }
            """.trimIndent()
        val scene = documentScene(JsxGraphEngine.parse(source))

        assertEquals(
            "A=${fixed(9.0 * kotlin.math.PI, 2)} cm<sup>2</sup>",
            text(scene, "measurement").content,
        )
    }

    private fun fixed(
        value: Double,
        digits: Int,
    ): String =
        com.swithun.jsxgraph.core.utils.JsNumberFormat.fixed(value, digits)

    private fun point(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Point =
        assertIs(scene.elements.single { it.id == id })

    private fun text(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Text =
        assertIs(scene.elements.single { it.id == id })

    private fun jessieScene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value

    private fun documentScene(
        result: GMResult<JsxGraphScene, JsxGraphDocumentError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value

    private fun interactionScene(
        result: GMResult<JsxGraphScene, JsxGraphInteractionError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value
}
