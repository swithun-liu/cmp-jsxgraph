/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsxGraphCurveGliderTest {
    @Test
    fun jessieCodeRendersFunctionGraphAndDataPlotGliders() {
        val scene = jessieScene(
            JsxGraphJessieCode.parse(
                """
                graph = functiongraph(
                    "1 + 0.25 * x * x", -4, 4
                ) <<
                    id: "graph", name: "", withLabel: false,
                    doAdvancedPlot: false, numberPointsHigh: 64
                >>;
                functionGlider = glider(1.5, 4, graph) <<
                    id: "functionGlider", name: "", withLabel: false
                >>;
                plot = curve(
                    [-4, -1, 2, 4],
                    [-2, 2, -1, 2]
                ) << id: "plot", name: "", withLabel: false >>;
                plotGlider = glider(0, 0, plot) <<
                    id: "plotGlider", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            listOf("graph", "functionGlider", "plot", "plotGlider"),
            scene.elements.map(JsxGraphSceneElement::id),
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(
                x = 2.8622543136990153,
                y = 3.048124939072155,
            ),
            actual = point(scene, "functionGlider").coordinates,
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(0.5, 0.5),
            actual = point(scene, "plotGlider").coordinates,
        )
    }

    @Test
    fun sessionMovesCurveGliderAndTracksFunctionGraphDependency() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    boundingBox =
                        JsxGraphBoundingBox(-5.0, 6.0, 5.0, -4.0),
                ),
            ),
        ).value
        jessieScene(
            session.execute(
                """
                driver = point(0, 1) <<
                    id: "driver", name: "", withLabel: false
                >>;
                graph = functiongraph(
                    "driver.Y() + 0.25 * x * x", -4, 4
                ) <<
                    id: "graph", name: "", withLabel: false,
                    doAdvancedPlot: false, numberPointsHigh: 64
                >>;
                glider(1.5, 4, graph) <<
                    id: "glider", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )

        val parentMoved = interactionScene(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(0.0, 2.0),
            ),
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(
                x = 2.8622543136990153,
                y = 4.048124939072155,
            ),
            actual = point(parentMoved, "glider").coordinates,
        )

        val gliderMoved = interactionScene(
            session.movePoint(
                id = "glider",
                coordinates = JsxGraphPoint2D(3.0, 0.0),
            ),
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(
                x = 1.34718621971269,
                y = 2.4537276776459422,
            ),
            actual = point(gliderMoved, "glider").coordinates,
        )
    }

    @Test
    fun constructionDocumentResolvesCurveGliderParent() {
        val scene = documentScene(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "plot",
                      "type": "curve",
                      "parents": [[-4, -1, 2, 4], [-2, 2, -1, 2]],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "glider",
                      "type": "glider",
                      "parents": [0, 0, "plot"],
                      "attributes": {"name": "", "withLabel": false}
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals(
            listOf("plot", "glider"),
            scene.elements.map(JsxGraphSceneElement::id),
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(0.5, 0.5),
            actual = point(scene, "glider").coordinates,
        )
    }

    @Test
    fun persistentJessieCodeSessionExposesCurveGliderPositionMethod() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        jessieScene(
            session.execute(
                """
                graph = functiongraph(
                    "1 + 0.25 * x * x", -4, 4
                ) <<
                    id: "graph", name: "", withLabel: false,
                    doAdvancedPlot: false, numberPointsHigh: 64
                >>;
                g = glider(1.5, 4, graph) <<
                    id: "glider", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )

        val positioned = jessieScene(
            session.execute("g.setGliderPosition(1);"),
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(1.0, 1.25),
            actual = point(positioned, "glider").coordinates,
        )
    }

    private fun point(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Point =
        assertIs(scene.elements.single { it.id == id })

    private fun assertPointCoordinates(
        expected: JsxGraphPoint2D,
        actual: JsxGraphPoint2D,
    ) {
        assertEquals(
            expected.x,
            actual.x,
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            expected.y,
            actual.y,
            absoluteTolerance = TOLERANCE,
        )
    }

    private fun jessieScene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(result, result.toString()).value

    private fun documentScene(
        result: GMResult<JsxGraphScene, JsxGraphDocumentError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(result, result.toString()).value

    private fun interactionScene(
        result: GMResult<JsxGraphScene, JsxGraphInteractionError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(result, result.toString()).value

    private companion object {
        const val TOLERANCE = 1.0e-6
    }
}
