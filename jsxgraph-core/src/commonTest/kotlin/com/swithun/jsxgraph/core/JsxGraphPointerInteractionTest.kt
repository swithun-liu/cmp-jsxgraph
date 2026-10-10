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

class JsxGraphPointerInteractionTest {
    @Test
    fun constructionSessionPublishesHoverAndLeaveSnapshots() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(CONSTRUCTION_SOURCE),
        ).value

        assertFalse(point(session.scene).style.highlighted)
        assertTrue(
            point(
                move(
                    interaction = session::interactPointer,
                    x = 250.0,
                    y = 250.0,
                    timestampMilliseconds = 100,
                ),
            ).style.highlighted,
        )
        assertFalse(
            point(
                move(
                    interaction = session::interactPointer,
                    x = 400.0,
                    y = 400.0,
                    timestampMilliseconds = 150,
                ),
            ).style.highlighted,
        )
        move(
            interaction = session::interactPointer,
            x = 250.0,
            y = 250.0,
            timestampMilliseconds = 200,
        )
        val left = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.interactPointer(
                JsxGraphBoardPointerInteraction.Leave,
            ),
        ).value
        assertFalse(point(left).style.highlighted)
        assertFalse(point(session.scene).style.highlighted)
    }

    @Test
    fun jessieCodeAndLegacySessionsDelegateBoardHover() {
        val jessieCode = assertIs<
            GMResult.Ok<JsxGraphJessieCodeSession>,
            >(
            JsxGraphJessieCode.createSession(),
        ).value
        assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCode.execute(
                """
                A = point(0, 0) <<
                    id: "point",
                    name: "",
                    withLabel: false,
                    size: 8,
                    highlight: true
                >>;
                """.trimIndent(),
            ),
        )

        val highlighted = move(
            interaction = jessieCode::interactPointer,
            x = 250.0,
            y = 250.0,
            timestampMilliseconds = 100,
        )
        assertTrue(point(highlighted).style.highlighted)

        val legacy = assertIs<
            GMResult.Ok<JsxGraphJessieScriptSession>,
            >(
            JsxGraphJessieScript.createSession("A(0,0) nolabel"),
        ).value
        val legacyHighlighted = move(
            interaction = legacy::interactPointer,
            x = 250.0,
            y = 250.0,
            timestampMilliseconds = 100,
        )
        assertTrue(
            legacyHighlighted.elements
                .filterIsInstance<JsxGraphSceneElement.Point>()
                .single()
                .style
                .highlighted,
        )
    }

    @Test
    fun pointerMoveRejectsInvalidCoordinatesAndViewport() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(CONSTRUCTION_SOURCE),
        ).value

        assertIs<
            GMResult.Err<
                JsxGraphInteractionError.NonFiniteBoardPointerCoordinates,
                >,
            >(
            session.interactPointer(
                JsxGraphBoardPointerInteraction.Move(
                    position = JsxGraphPoint2D(Double.NaN, 0.0),
                    viewportSize = VIEWPORT,
                    timestampMilliseconds = 100,
                ),
            ),
        )
        assertIs<
            GMResult.Err<
                JsxGraphInteractionError.InvalidBoardPointerViewport,
                >,
            >(
            session.interactPointer(
                JsxGraphBoardPointerInteraction.Move(
                    position = JsxGraphPoint2D(250.0, 250.0),
                    viewportSize = JsxGraphViewportSize(0.0, 500.0),
                    timestampMilliseconds = 100,
                ),
            ),
        )
        assertFalse(point(session.scene).style.highlighted)
    }

    @Test
    fun constructionSessionPublishesTwoFingerLineTransform() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(MULTI_POINTER_SOURCE),
        ).value

        touch(
            session,
            JsxGraphBoardPointerInteraction.Down(
                pointerId = 1,
                position = JsxGraphPoint2D(150.0, 200.0),
                viewportSize = MULTI_POINTER_VIEWPORT,
                timestampMilliseconds = 100,
            ),
        )
        touch(
            session,
            JsxGraphBoardPointerInteraction.Down(
                pointerId = 2,
                position = JsxGraphPoint2D(350.0, 200.0),
                viewportSize = MULTI_POINTER_VIEWPORT,
                timestampMilliseconds = 101,
            ),
        )
        for ((pointerId, x, y, time) in listOf(
            TouchMove(1, 150.0, 200.0, 110),
            TouchMove(2, 350.0, 200.0, 120),
            TouchMove(1, 200.0, 250.0, 130),
            TouchMove(2, 400.0, 150.0, 140),
        )) {
            touch(
                session,
                JsxGraphBoardPointerInteraction.Move(
                    pointerId = pointerId,
                    position = JsxGraphPoint2D(x, y),
                    viewportSize = MULTI_POINTER_VIEWPORT,
                    timestampMilliseconds = time,
                    device = JsxGraphPointerDevice.TOUCH,
                ),
            )
        }
        val transformed = session.scene.elements
            .filterIsInstance<JsxGraphSceneElement.Line>()
            .single { it.id == "line" }
        assertEquals(0.0, transformed.point1.x, ABSOLUTE_TOLERANCE)
        assertEquals(-0.5, transformed.point1.y, ABSOLUTE_TOLERANCE)
        assertEquals(2.0, transformed.point2.x, ABSOLUTE_TOLERANCE)
        assertEquals(0.5, transformed.point2.y, ABSOLUTE_TOLERANCE)

        touch(
            session,
            JsxGraphBoardPointerInteraction.Up(
                pointerId = 1,
                position = JsxGraphPoint2D(200.0, 250.0),
                viewportSize = MULTI_POINTER_VIEWPORT,
                timestampMilliseconds = 150,
            ),
        )
        touch(
            session,
            JsxGraphBoardPointerInteraction.Up(
                pointerId = 2,
                position = JsxGraphPoint2D(400.0, 150.0),
                viewportSize = MULTI_POINTER_VIEWPORT,
                timestampMilliseconds = 160,
            ),
        )
    }

    @Test
    fun touchDownAndUpRejectInvalidPortableInput() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(CONSTRUCTION_SOURCE),
        ).value

        assertIs<
            GMResult.Err<
                JsxGraphInteractionError.NonFiniteBoardPointerCoordinates,
                >,
            >(
            session.interactPointer(
                JsxGraphBoardPointerInteraction.Down(
                    pointerId = 1,
                    position = JsxGraphPoint2D(Double.NaN, 0.0),
                    viewportSize = VIEWPORT,
                    timestampMilliseconds = 100,
                ),
            ),
        )
        assertIs<
            GMResult.Err<
                JsxGraphInteractionError.InvalidBoardPointerViewport,
                >,
            >(
            session.interactPointer(
                JsxGraphBoardPointerInteraction.Up(
                    pointerId = 1,
                    position = JsxGraphPoint2D(0.0, 0.0),
                    viewportSize = JsxGraphViewportSize(500.0, 0.0),
                    timestampMilliseconds = 110,
                ),
            ),
        )
    }

    private fun touch(
        session: JsxGraphSession,
        interaction: JsxGraphBoardPointerInteraction,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.interactPointer(interaction),
        ).value

    private fun move(
        interaction: (
            JsxGraphBoardPointerInteraction,
        ) -> GMResult<JsxGraphScene, JsxGraphInteractionError>,
        x: Double,
        y: Double,
        timestampMilliseconds: Long,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            interaction(
                JsxGraphBoardPointerInteraction.Move(
                    position = JsxGraphPoint2D(x, y),
                    viewportSize = VIEWPORT,
                    timestampMilliseconds = timestampMilliseconds,
                ),
            ),
        ).value

    private fun point(
        scene: JsxGraphScene,
    ): JsxGraphSceneElement.Point =
        assertIs(
            scene.elements.single { it.id == "point" },
        )

    private companion object {
        private const val ABSOLUTE_TOLERANCE = 1e-12

        private data class TouchMove(
            val pointerId: Long,
            val x: Double,
            val y: Double,
            val timestampMilliseconds: Long,
        )

        val VIEWPORT = JsxGraphViewportSize(500.0, 500.0)
        val MULTI_POINTER_VIEWPORT =
            JsxGraphViewportSize(500.0, 400.0)

        val CONSTRUCTION_SOURCE =
            """
            {
              "boundingBox": [-5, 5, 5, -5],
              "axis": false,
              "grid": false,
              "keepAspectRatio": false,
              "objects": [
                {
                  "id": "point",
                  "type": "point",
                  "parents": [0, 0],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "size": 8,
                    "highlight": true
                  }
                }
              ]
            }
            """.trimIndent()

        val MULTI_POINTER_SOURCE =
            """
            {
              "boundingBox": [-5, 4, 5, -4],
              "axis": false,
              "grid": false,
              "keepAspectRatio": false,
              "objects": [
                {
                  "id": "first",
                  "type": "point",
                  "parents": [-1, 0],
                  "attributes": {
                    "name": "",
                    "withLabel": false
                  }
                },
                {
                  "id": "second",
                  "type": "point",
                  "parents": [1, 0],
                  "attributes": {
                    "name": "",
                    "withLabel": false
                  }
                },
                {
                  "id": "line",
                  "type": "line",
                  "parents": ["first", "second"],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "scalable": true,
                    "rotatable": true
                  }
                }
              ]
            }
            """.trimIndent()
    }
}
