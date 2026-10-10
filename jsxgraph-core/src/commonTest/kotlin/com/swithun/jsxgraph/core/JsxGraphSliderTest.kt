/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphSliderTest {
    @Test
    fun jessieCodeSliderExpandsToRenderableHelperElements() {
        val scene = jessieScene(
            JsxGraphJessieCode.parse(
                """
                slider(
                    [-4, -1],
                    [4, 3],
                    [-10, 3, 10]
                ) <<
                    id: "slider", name: "s",
                    point1: << id: "sliderStart" >>,
                    point2: << id: "sliderEnd" >>,
                    baseline: <<
                        id: "baseline",
                        strokeColor: "#336699"
                    >>,
                    highline: <<
                        id: "highline",
                        strokeColor: "#cc3344"
                    >>,
                    ticks: << id: "sliderTicks" >>,
                    label: << id: "sliderLabel" >>
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            listOf(
                "sliderStart",
                "sliderEnd",
                "baseline",
                "slider",
                "highline",
                "sliderLabel",
                "sliderTicks",
            ),
            scene.elements.map(JsxGraphSceneElement::id),
        )
        assertFalse(point(scene, "sliderStart").style.visible)
        assertFalse(point(scene, "sliderEnd").style.visible)
        assertEquals(
            JsxGraphColor(51, 102, 153),
            line(scene, "baseline").style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(204, 51, 68),
            line(scene, "highline").style.strokeColor,
        )
        assertEquals(3.0, line(scene, "highline").style.strokeWidth)
        assertPointCoordinates(
            JsxGraphPoint2D(1.2, 1.6),
            point(scene, "slider").coordinates,
        )
        assertTrue(point(scene, "slider").draggable)
        assertEquals(
            "s = 3.00",
            assertIs<JsxGraphSceneElement.Text>(
                scene.elements.single { it.id == "sliderLabel" },
            ).content,
        )
        assertFalse(
            assertIs<JsxGraphSceneElement.Ticks>(
                scene.elements.single { it.id == "sliderTicks" },
            ).definition.drawLabels,
        )
    }

    @Test
    fun sliderTickLabelsUseValueRangeInDocumentAndJessieCodeScenes() {
        val documentScene = documentScene(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [{
                    "id": "slider",
                    "type": "slider",
                    "parents": [[-4, 0], [4, 0], [-10, 0, 10]],
                    "attributes": {
                      "name": "",
                      "withLabel": false,
                      "ticks": {
                        "id": "sliderTicks",
                        "drawLabels": true,
                        "insertTicks": false,
                        "ticksDistance": 2
                      }
                    }
                  }]
                }
                """.trimIndent(),
            ),
        )
        val jessieScene = jessieScene(
            JsxGraphJessieCode.parse(
                """
                slider(
                    [-4, 0],
                    [4, 0],
                    [-10, 0, 10]
                ) <<
                    id: "slider", name: "", withLabel: false,
                    ticks: <<
                        id: "sliderTicks",
                        drawLabels: true,
                        insertTicks: false,
                        ticksDistance: 2
                    >>
                >>;
                """.trimIndent(),
            ),
        )

        for (scene in listOf(documentScene, jessieScene)) {
            val definition = assertIs<JsxGraphSceneElement.Ticks>(
                scene.elements.single { it.id == "sliderTicks" },
            ).definition
            assertEquals(
                JsxGraphSliderTicksValueRange(
                    minimum = -10.0,
                    maximum = 10.0,
                ),
                definition.sliderValueRange,
            )
            val resolved = definition.resolve(
                visibleLeft = -5.0,
                visibleTop = 5.0,
                visibleRight = 5.0,
                visibleBottom = -5.0,
                cssPixelsPerUnitX = 50.0,
                cssPixelsPerUnitY = 50.0,
            )
            assertEquals(
                listOf("−10", "−5", "0", "5", "10"),
                resolved.labels.map(JsxGraphTickLabel::content),
            )
        }
    }

    @Test
    fun sliderInteractionProjectsOntoBaselineAndUpdatesDependents() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = jessieScene(
            session.execute(
                """
                s = slider(
                    [-4, 0],
                    [4, 0],
                    [0, 2.6, 10]
                ) <<
                    id: "slider", name: "",
                    withLabel: false, withTicks: false,
                    snapWidth: 2,
                    snapValues: [1, 7, 9],
                    snapValueDistance: 0.6
                >>;
                dependent = point(
                    function () { return s.X(); },
                    function () { return s.Value(); }
                ) <<
                    id: "dependent", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-2.4, 0.0),
            point(initial, "slider").coordinates,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-2.4, 2.0),
            point(initial, "dependent").coordinates,
        )

        val moved = interactionScene(
            session.movePoint(
                id = "slider",
                coordinates = JsxGraphPoint2D(1.44, 2.0),
            ),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(1.44, 0.0),
            point(moved, "slider").coordinates,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(1.44, 8.0),
            point(moved, "dependent").coordinates,
        )
    }

    @Test
    fun constructionDocumentSupportsSliderAndCountsItsHelpers() {
        val scene = documentScene(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "slider",
                      "type": "slider",
                      "parents": [[-3, 1], [3, 1], [0, 4, 10]],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "withTicks": false,
                        "point1": {"id": "start"},
                        "point2": {"id": "end"},
                        "baseline": {"id": "baseline"},
                        "highline": {"id": "highline"}
                      }
                    }
                  ]
                }
                """.trimIndent(),
                limits = JsxGraphEngineLimits(maxObjects = 5),
            ),
        )
        assertEquals(
            listOf("start", "end", "baseline", "slider", "highline"),
            scene.elements.map(JsxGraphSceneElement::id),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-0.6, 1.0),
            point(scene, "slider").coordinates,
        )

        val limited = assertIs<
            GMResult.Err<JsxGraphDocumentError.ObjectLimitExceeded>,
            >(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [{
                    "id": "slider",
                    "type": "slider",
                    "parents": [[-3, 1], [3, 1], [0, 4, 10]],
                    "attributes": {
                      "name": "",
                      "withLabel": false,
                      "withTicks": false
                    }
                  }]
                }
                """.trimIndent(),
                limits = JsxGraphEngineLimits(maxObjects = 4),
            ),
        ).error
        assertEquals(5, limited.actual)
    }

    @Test
    fun constructionSessionRoutesBaselinePointerUpToSlider() {
        val movable = sliderSession()

        clickBaseline(movable, x = 2.0, y = 0.0)

        assertPointCoordinates(
            JsxGraphPoint2D(2.0, 0.0),
            point(movable.scene, "slider").coordinates,
        )

        val disabled = sliderSession(
            additionalAttributes = "\"moveOnUp\": false,",
        )

        clickBaseline(disabled, x = 2.0, y = 0.0)

        assertPointCoordinates(
            JsxGraphPoint2D(-1.8, 0.0),
            point(disabled.scene, "slider").coordinates,
        )

        val fixed = sliderSession(
            additionalAttributes = "\"fixed\": true,",
        )

        clickBaseline(fixed, x = 2.0, y = 0.0)

        assertPointCoordinates(
            JsxGraphPoint2D(-1.8, 0.0),
            point(fixed.scene, "slider").coordinates,
        )
    }

    private fun sliderSession(
        additionalAttributes: String = "",
    ): JsxGraphSession {
        val result = JsxGraphEngine.createSession(
            """
            {
              "boundingBox": [-5, 5, 5, -5],
              "objects": [{
                "id": "slider",
                "type": "slider",
                "parents": [[-3, 0], [3, 0], [0, 2, 10]],
                "attributes": {
                  $additionalAttributes
                  "name": "",
                  "withLabel": false,
                  "withTicks": false,
                  "baseline": {"id": "baseline"},
                  "highline": {"id": "highline"}
                }
              }]
            }
            """.trimIndent(),
        )
        return assertIs<GMResult.Ok<JsxGraphSession>>(
            result,
            result.toString(),
        ).value
    }

    private fun clickBaseline(
        session: JsxGraphSession,
        x: Double,
        y: Double,
    ) {
        val viewport = JsxGraphViewportSize(500.0, 500.0)
        val screen = JsxGraphPoint2D(
            x = 250.0 + x * 50.0,
            y = 250.0 - y * 50.0,
        )
        interactionScene(
            session.interactPointer(
                JsxGraphBoardPointerInteraction.Down(
                    pointerId = 1L,
                    position = screen,
                    viewportSize = viewport,
                    timestampMilliseconds = 100L,
                    device = JsxGraphPointerDevice.MOUSE,
                ),
            ),
        )
        interactionScene(
            session.interactPointer(
                JsxGraphBoardPointerInteraction.Up(
                    pointerId = 1L,
                    position = screen,
                    viewportSize = viewport,
                    timestampMilliseconds = 120L,
                    device = JsxGraphPointerDevice.MOUSE,
                ),
            ),
        )
    }

    private fun point(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Point =
        assertIs(scene.elements.single { it.id == id })

    private fun line(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Line =
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
        assertIs<GMResult.Ok<JsxGraphScene>>(result).value

    private fun assertPointCoordinates(
        expected: JsxGraphPoint2D,
        actual: JsxGraphPoint2D,
    ) {
        assertEquals(expected.x, actual.x, absoluteTolerance = 1.0e-12)
        assertEquals(expected.y, actual.y, absoluteTolerance = 1.0e-12)
    }
}
