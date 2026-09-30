/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphHtmlControlTest {
    @Test
    fun textBackedControlsExposeOfficialStateAndDynamicLabels() {
        val session = session()
        val initial = scene(
            session.execute(
                """
                driver = point(-1, 4) <<
                    id: "driver", name: "", withLabel: false
                >>;
                button(
                    -3,
                    3,
                    function () {
                        return driver.X() < 0 ? "first" : "second";
                    },
                    function () { return 0; }
                ) << id: "button", name: "", disabled: true >>;
                checkbox(
                    -3,
                    2,
                    function () {
                        return driver.X() < 0 ? "first" : "second";
                    }
                ) << id: "checkbox", name: "", checked: true >>;
                input(
                    -3,
                    1,
                    "initial",
                    function () {
                        return driver.X() < 0 ? "first" : "second";
                    }
                ) << id: "input", name: "", maxlength: 7 >>;
                htmlslider([ -3, 0 ], [ -2, 1.5, 8 ]) <<
                    id: "slider",
                    name: "alpha",
                    step: 0.25,
                    widthRange: 140,
                    widthOut: 48,
                    withLabel: true
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            JsxGraphHtmlControl.Button(disabled = true),
            control(initial, "button").htmlControl,
        )
        assertEquals("first", control(initial, "button").content)
        assertEquals(
            JsxGraphHtmlControl.Checkbox(
                checked = true,
                disabled = false,
            ),
            control(initial, "checkbox").htmlControl,
        )
        assertEquals(
            JsxGraphHtmlControl.Input(
                value = "initial",
                maxLength = 7,
                disabled = false,
            ),
            control(initial, "input").htmlControl,
        )
        assertEquals(
            JsxGraphHtmlControl.Slider(
                minimum = -2.0,
                maximum = 8.0,
                value = 1.5,
                step = 0.25,
                widthRange = 140.0,
                widthOut = 48.0,
                withLabel = true,
            ),
            control(initial, "slider").htmlControl,
        )
        assertEquals("alpha=", control(initial, "slider").content)

        val updated = scene(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(1.0, 4.0),
            ),
        )
        for (id in listOf("button", "checkbox", "input")) {
            assertEquals("second", control(updated, id).content)
        }
    }

    @Test
    fun sessionInteractionsUpdateControlsAndRunBothButtonHandlers() {
        val session = session()
        scene(
            session.execute(
                """
                p = point(1, 1) <<
                    id: "p", name: "", withLabel: false
                >>;
                functionButton = button(
                    -3,
                    3,
                    "Function",
                    function () { p.X = p.X() + 1; }
                ) << id: "functionButton", name: "" >>;
                stringButton = button(
                    -3,
                    2,
                    "String",
                    "${'$'}('p').Y = ${'$'}('p').Y() + 2;"
                ) << id: "stringButton", name: "" >>;
                checkbox(-3, 1, "Check") <<
                    id: "checkbox", name: ""
                >>;
                input(-3, 0, "a", "Input") <<
                    id: "input", name: "", maxlength: 7
                >>;
                htmlslider([ -3, -1 ], [ 0, 1, 5 ]) <<
                    id: "slider", name: "", step: 0.5
                >>;
                """.trimIndent(),
            ),
        )

        scene(session.changeCheckbox("checkbox", checked = true))
        scene(session.changeInput("input", "changed"))
        scene(session.changeSlider("slider", 4.5))
        scene(session.clickButton("functionButton"))
        val updated = scene(session.clickButton("stringButton"))

        assertEquals(
            JsxGraphHtmlControl.Checkbox(
                checked = true,
                disabled = false,
            ),
            control(updated, "checkbox").htmlControl,
        )
        assertEquals(
            "changed",
            assertIs<JsxGraphHtmlControl.Input>(
                control(updated, "input").htmlControl,
            ).value,
        )
        assertEquals(
            4.5,
            assertIs<JsxGraphHtmlControl.Slider>(
                control(updated, "slider").htmlControl,
            ).value,
        )
        val point = assertIs<JsxGraphSceneElement.Point>(
            updated.elements.single { it.id == "p" },
        )
        assertEquals(JsxGraphPoint2D(2.0, 3.0), point.coordinates)
    }

    @Test
    fun constructionDocumentCreatesStaticControlsThroughTheSameCreators() {
        val result =
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "button",
                      "type": "button",
                      "parents": [0, 3, "Run"],
                      "attributes": {"name": ""}
                    },
                    {
                      "id": "checkbox",
                      "type": "checkbox",
                      "parents": [0, 2, "Check"],
                      "attributes": {"name": "", "checked": true}
                    },
                    {
                      "id": "input",
                      "type": "input",
                      "parents": [0, 1, "value", "Label"],
                      "attributes": {"name": "", "maxlength": 8}
                    },
                    {
                      "id": "slider",
                      "type": "htmlslider",
                      "parents": [[0, 0], [0, 2, 5]],
                      "attributes": {"name": ""}
                    }
                  ]
                }
                """.trimIndent(),
            )
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            result,
            result.toString(),
        ).value
        val scene = session.scene

        assertIs<JsxGraphHtmlControl.Button>(
            control(scene, "button").htmlControl,
        )
        assertEquals(
            true,
            assertIs<JsxGraphHtmlControl.Checkbox>(
                control(scene, "checkbox").htmlControl,
            ).checked,
        )
        assertEquals(
            "value",
            assertIs<JsxGraphHtmlControl.Input>(
                control(scene, "input").htmlControl,
            ).value,
        )
        assertEquals(
            2.0,
            assertIs<JsxGraphHtmlControl.Slider>(
                control(scene, "slider").htmlControl,
            ).value,
        )
        val updated = scene(
            session.interactControl(
                JsxGraphControlInteraction.ChangeCheckbox(
                    id = "checkbox",
                    checked = false,
                ),
            ),
        )
        assertEquals(
            false,
            assertIs<JsxGraphHtmlControl.Checkbox>(
                control(updated, "checkbox").htmlControl,
            ).checked,
        )
        scene(
            session.interactControl(
                JsxGraphControlInteraction.ClickButton("button"),
            ),
        )
    }

    @Test
    fun omittedLabelsRenderAsOfficialUndefinedText() {
        val scene = scene(
            JsxGraphJessieCode.parse(
                """
                button(0, 2) << id: "button", name: "" >>;
                checkbox(0, 1) << id: "checkbox", name: "" >>;
                input(0, 0, "value") << id: "input", name: "" >>;
                """.trimIndent(),
            ),
        )

        for (id in listOf("button", "checkbox", "input")) {
            assertEquals("undefined", control(scene, id).content)
        }
        assertEquals(
            "value",
            assertIs<JsxGraphHtmlControl.Input>(
                control(scene, "input").htmlControl,
            ).value,
        )
    }

    @Test
    fun valueMethodsAndInvalidInteractionsFailStructurally() {
        val session = session()
        val scene = scene(
            session.execute(
                """
                checkbox(0, 3, "Disabled") <<
                    id: "disabled", name: "", disabled: true
                >>;
                checkbox(0, 2, "Check") <<
                    id: "check", name: "", checked: true
                >>;
                field = input(0, 1, "old", "Input") <<
                    id: "field", name: "", maxlength: 4
                >>;
                range = htmlslider([0, 0], [0, 2, 5]) <<
                    id: "range", name: ""
                >>;
                field.set("new");
                probe = point(range.Value(), check.Value() ? 1 : 0) <<
                    id: "probe", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            "new",
            assertIs<JsxGraphHtmlControl.Input>(
                control(scene, "field").htmlControl,
            ).value,
        )
        assertEquals(
            JsxGraphPoint2D(2.0, 1.0),
            assertIs<JsxGraphSceneElement.Point>(
                scene.elements.single { it.id == "probe" },
            ).coordinates,
        )
        assertIs<GMResult.Err<JsxGraphInteractionError.ControlDisabled>>(
            session.changeCheckbox("disabled", checked = true),
        )
        assertIs<GMResult.Err<JsxGraphInteractionError.InvalidControlValue>>(
            session.changeInput("field", "too-long"),
        )
        assertIs<GMResult.Err<JsxGraphInteractionError.InvalidControlValue>>(
            session.changeSlider("range", 6.0),
        )
        assertIs<
            GMResult.Err<JsxGraphInteractionError.UnexpectedControlType>,
            >(session.clickButton("check"))
        assertIs<GMResult.Err<JsxGraphInteractionError.UnknownControl>>(
            session.clickButton("missing"),
        )

        for (
            source in listOf(
                "htmlslider([0, 0], [0, 1]);",
                "htmlslider([0, 0], [2, 1, 0]);",
            )
        ) {
            val failure = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(source),
            )
            assertTrue(
                failure.error.message.contains("UnsupportedParents") ||
                    failure.error.message.contains("InvalidControlValue"),
                failure.error.message,
            )
        }
    }

    private fun session(): JsxGraphJessieCodeSession =
        assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value

    private fun scene(
        result: GMResult<JsxGraphScene, *>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value

    private fun control(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Text =
        assertIs<JsxGraphSceneElement.Text>(
            scene.elements.single { it.id == id },
        )
}
