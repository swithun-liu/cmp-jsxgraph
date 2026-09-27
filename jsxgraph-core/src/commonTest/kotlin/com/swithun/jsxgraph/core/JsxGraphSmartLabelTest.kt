/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.SmartLabel
import com.swithun.jsxgraph.core.base.SmartLabelAttributes
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.parser.JessieCodeCallable
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class JsxGraphSmartLabelTest {
    @Test
    fun rendersAllSupportedParentKindsAndTracksGeometry() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                p = point(-5, 3) <<
                    id: "point", name: "", withLabel: false
                >>;
                a = point(-2, 4) <<
                    id: "lineStart", name: "", withLabel: false
                >>;
                b = point(4, 1) <<
                    id: "lineEnd", name: "", withLabel: false
                >>;
                line = segment(a, b) <<
                    id: "line", name: "", withLabel: false
                >>;
                center = point(-3, -1) <<
                    id: "center", name: "", withLabel: false
                >>;
                edge = point(-1, -1) <<
                    id: "edge", name: "", withLabel: false
                >>;
                circle = circle(center, edge) <<
                    id: "circle", name: "", withLabel: false
                >>;
                polygon = polygon(
                    [1, -1], [5, -1], [4, -4], [1, -3]
                ) <<
                    id: "polygon", name: "", withLabel: false
                >>;
                angle = angle([2, 5], [2, 2], [5, 2]) <<
                    id: "angle", name: "", withLabel: false,
                    radius: 1.25
                >>;
                pointLabel = smartlabel(p) <<
                    id: "pointLabel", name: "",
                    prefix: "P=", baseUnit: "cm", digits: 1,
                    useMathJax: false
                >>;
                lineLength = smartlabel(line) <<
                    id: "lineLength", name: "",
                    prefix: "L=", baseUnit: "m",
                    orientation: "parallel", useMathJax: false
                >>;
                lineSlope = smartlabel(line) <<
                    id: "lineSlope", name: "",
                    measure: "slope", prefix: "s=",
                    orientation: "orthogonal-inverted",
                    useMathJax: false
                >>;
                circleArea = smartlabel(circle) <<
                    id: "circleArea", name: "",
                    measure: "area", prefix: "A=",
                    units: << dim2: "sq" >>,
                    cssClass: "smart-label-outline smart-label-circle",
                    useMathJax: false
                >>;
                polygonArea = smartlabel(polygon) <<
                    id: "polygonArea", name: "",
                    prefix: "A=", baseUnit: "m",
                    useMathJax: false
                >>;
                angleDegrees = smartlabel(angle) <<
                    id: "angleDegrees", name: "",
                    prefix: "a=", useMathJax: false
                >>;
                """.trimIndent(),
            ),
        )

        assertText(initial, "pointLabel", "P=-5.0 cm / 3.0 cm")
        assertText(initial, "lineLength", "L=6.71m")
        assertText(initial, "lineSlope", "s=-0.50")
        assertText(initial, "circleArea", "A=12.57sq")
        assertText(initial, "polygonArea", "A=9.00m<sup>2</sup>")
        assertText(initial, "angleDegrees", "a=270.00")
        assertEquals(
            JsxGraphPoint2D(1.0, 2.5),
            text(initial, "lineLength").coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(2.5, 1.75),
            text(initial, "lineSlope").coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(-3.0, 0.0),
            text(initial, "circleArea").coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(3.0, -2.5),
            text(initial, "polygonArea").coordinates,
        )
        assertEquals(
            333.434948822922,
            assertNotNull(
                text(initial, "lineLength").smartLabel,
            ).rotationDegrees,
            absoluteTolerance = 1.0e-9,
        )
        assertIs<JsxGraphSmartLabelVisibility.Line>(
            text(initial, "lineLength").smartLabel?.visibility,
        )
        assertEquals(
            JsxGraphSmartLabelBoxKind.OUTLINE,
            text(initial, "circleArea").smartLabel?.boxKind,
        )
        assertEquals(
            JsxGraphSmartLabelParentKind.CIRCLE,
            text(initial, "circleArea").smartLabel?.parentKind,
        )
        val angleLabel = text(initial, "angleDegrees")
        assertEquals(
            2.0 - 1.25 / kotlin.math.sqrt(2.0),
            angleLabel.coordinates.x,
            absoluteTolerance = 1.0e-9,
        )
        assertEquals(
            2.0 - 1.25 / kotlin.math.sqrt(2.0),
            angleLabel.coordinates.y,
            absoluteTolerance = 1.0e-9,
        )
        assertEquals(
            -12.0 / kotlin.math.sqrt(2.0),
            angleLabel.screenOffset.x,
            absoluteTolerance = 1.0e-9,
        )
        assertEquals(
            12.0 / kotlin.math.sqrt(2.0),
            angleLabel.screenOffset.y,
            absoluteTolerance = 1.0e-9,
        )

        val moved = interactionScene(
            session.movePoint(
                id = "lineEnd",
                coordinates = JsxGraphPoint2D(5.0, -2.0),
            ),
        )
        assertText(moved, "lineLength", "L=9.22m")
        assertText(moved, "lineSlope", "s=-0.86")
        assertEquals(
            JsxGraphPoint2D(1.5, 1.0),
            text(moved, "lineLength").coordinates,
        )
    }

    @Test
    fun exposesValueDimensionUnitAndParentMethodMap() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val scene = scene(
            session.execute(
                """
                p = point(2, 3) <<
                    id: "point", name: "", withLabel: false
                >>;
                label = smartlabel(p) <<
                    id: "label", name: "",
                    baseUnit: "cm", useMathJax: false
                >>;
                probe = point(
                    function() { return label.Value()[0]; },
                    function() {
                        return label.Dimension() +
                            IfThen(label.Unit() == "cm", 1, 0) +
                            IfThen(label.parent == p, 1, 0) +
                            IfThen(label.parentObject == p, 1, 0);
                    }
                ) <<
                    id: "probe", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            JsxGraphPoint2D(2.0, 4.0),
            assertIs<JsxGraphSceneElement.Point>(
                scene.elements.single { it.id == "probe" },
            ).coordinates,
        )
    }

    @Test
    fun defersPointFunctionMeasureUntilDynamicTextNeedsIt() {
        val board = Board(
            originX = 250.0,
            originY = 250.0,
            unitX = 50.0,
            unitY = 50.0,
            id = "board",
        )
        val point = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(2.0, 3.0),
                id = "point",
                name = "",
            ),
        ).value
        var measureCalls = 0
        val measure = JessieCodeRuntimeValue.FunctionValue(
            name = "function",
            callable = JessieCodeCallable { _, _ ->
                measureCalls += 1
                GMResult.Ok(
                    JessieCodeRuntimeValue.StringValue("coords"),
                )
            },
        )

        val label = assertIs<GMResult.Ok<Text>>(
            SmartLabel.create(
                board = board,
                parent = point,
                userText =
                    JessieCodeRuntimeValue.StringValue("manual"),
                attributes = SmartLabelAttributes(
                    id = "label",
                    name = "",
                    values = mapOf(
                        "measure" to measure,
                        "usemathjax" to
                            JessieCodeRuntimeValue.BooleanValue(false),
                    ),
                ),
            ),
        ).value

        assertEquals("manual", label.plaintext)
        assertEquals(0, measureCalls)
    }

    @Test
    fun matchesTolerantFormattingAndColumnDirectionSemantics() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val scene = scene(
            session.execute(
                """
                p = point(2, 3) <<
                    id: "point", name: "", withLabel: false
                >>;
                column = smartlabel(p) <<
                    id: "column", name: "",
                    measure: function() { return "coords"; },
                    prefix: "P=", baseUnit: "cm",
                    units: "ignored", formatValue: "ignored",
                    dir: "column", useMathJax: false
                >>;
                invalidDirection = smartlabel(p) <<
                    id: "invalidDirection", name: "",
                    dir: "diagonal", useMathJax: false
                >>;
                """.trimIndent(),
            ),
        )

        assertText(
            scene,
            "column",
            "P=2.00 cm&lt;br /&gt;3.00 cm",
        )
        assertText(scene, "invalidDirection", "")
    }

    @Test
    fun rejectsUnsupportedParentsAndMeasuresAtomically() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute(
                """
                p1 = point(0, 0) <<
                    id: "p1", name: "", withLabel: false
                >>;
                p2 = point(1, 0) <<
                    id: "p2", name: "", withLabel: false
                >>;
                line = segment(p1, p2) <<
                    id: "line", name: "", withLabel: false
                >>;
                plain = text(0, 1, "plain") <<
                    id: "plain", name: ""
                >>;
                """.trimIndent(),
            ),
        )
        val before = session.scene.elements.map { it.id }

        for (source in listOf(
            "smartlabel();",
            "smartlabel(plain) << id: \"badParent\" >>;",
            """
            smartlabel(line) <<
                id: "badMeasure", measure: "area",
                useMathJax: false
            >>;
            """.trimIndent(),
            """
            smartlabel(line) <<
                id: "dynamicMeasure",
                measure: function() { return "length"; },
                useMathJax: false
            >>;
            """.trimIndent(),
        )) {
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                session.execute(source),
            )
            assertEquals(before, session.scene.elements.map { it.id })
        }
    }

    @Test
    fun requiresExplicitlyDisabledMathTypesettingForSceneOutput() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val result = session.execute(
            """
            p = point(1, 2) <<
                id: "point", name: "", withLabel: false
            >>;
            smartlabel(p) << id: "label", name: "" >>;
            """.trimIndent(),
        )

        val error = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(result)
        assertNotNull(error.error)
    }

    private fun assertText(
        scene: JsxGraphScene,
        id: String,
        expected: String,
    ) {
        assertEquals(expected, text(scene, id).content)
    }

    private fun text(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Text =
        assertIs(scene.elements.single { it.id == id })

    private fun scene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
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
