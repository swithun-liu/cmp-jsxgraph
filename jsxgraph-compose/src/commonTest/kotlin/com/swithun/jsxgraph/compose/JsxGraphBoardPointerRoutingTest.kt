/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.IntSize
import com.swithun.jsxgraph.core.JsxGraphBoardPointerInteraction
import com.swithun.jsxgraph.core.JsxGraphPoint2D
import com.swithun.jsxgraph.core.JsxGraphPointerDevice
import com.swithun.jsxgraph.core.JsxGraphViewportSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsxGraphBoardPointerRoutingTest {
    @Test
    fun moveUsesViewportLocalPixelsAndModifiers() {
        assertEquals(
            JsxGraphBoardPointerInteraction.Move(
                position = JsxGraphPoint2D(60.25, 40.125),
                viewportSize = JsxGraphViewportSize(320.0, 240.0),
                timestampMilliseconds = 125,
                shiftKey = true,
                controlKey = true,
            ),
            boardPointerInteractionForEvent(
                eventType = PointerEventType.Move,
                position = Offset(120.5f, 80.25f),
                viewportSize = IntSize(640, 480),
                density = 2.0f,
                timestampMilliseconds = 125,
                pointerPressed = false,
                shiftKey = true,
                controlKey = true,
            ),
        )
    }

    @Test
    fun pressedMoveDoesNotCompeteWithDragAndExitAlwaysLeaves() {
        assertNull(
            boardPointerInteractionForEvent(
                eventType = PointerEventType.Move,
                position = Offset(120.0f, 80.0f),
                viewportSize = IntSize(640, 480),
                density = 2.0f,
                timestampMilliseconds = 125,
                pointerPressed = true,
            ),
        )
        assertEquals(
            JsxGraphBoardPointerInteraction.Leave,
            boardPointerInteractionForEvent(
                eventType = PointerEventType.Exit,
                position = null,
                viewportSize = IntSize(640, 480),
                density = 2.0f,
                timestampMilliseconds = 150,
                pointerPressed = false,
            ),
        )
        assertNull(
            boardPointerInteractionForEvent(
                eventType = PointerEventType.Press,
                position = Offset.Zero,
                viewportSize = IntSize(640, 480),
                density = 2.0f,
                timestampMilliseconds = 175,
                pointerPressed = true,
            ),
        )
    }

    @Test
    fun touchLifecyclePreservesPointerIdentityAndViewportUnits() {
        assertEquals(
            listOf(
                JsxGraphBoardPointerInteraction.Down(
                    pointerId = 2,
                    position = JsxGraphPoint2D(150.0, 100.0),
                    viewportSize = JsxGraphViewportSize(320.0, 240.0),
                    timestampMilliseconds = 110,
                ),
            ),
            boardPointerInteractionsForEvent(
                eventType = PointerEventType.Press,
                changes = listOf(
                    touchChange(
                        pointerId = 1,
                        x = 200.0f,
                        y = 200.0f,
                        timestampMilliseconds = 100,
                        pressed = true,
                        previousPressed = true,
                    ),
                    touchChange(
                        pointerId = 2,
                        x = 300.0f,
                        y = 200.0f,
                        timestampMilliseconds = 110,
                        pressed = true,
                        previousPressed = false,
                    ),
                ),
                viewportSize = IntSize(640, 480),
                density = 2.0f,
            ),
        )

        assertEquals(
            listOf(
                JsxGraphBoardPointerInteraction.Move(
                    pointerId = 1,
                    position = JsxGraphPoint2D(110.0, 105.0),
                    viewportSize = JsxGraphViewportSize(320.0, 240.0),
                    timestampMilliseconds = 120,
                    device = JsxGraphPointerDevice.TOUCH,
                ),
                JsxGraphBoardPointerInteraction.Move(
                    pointerId = 2,
                    position = JsxGraphPoint2D(160.0, 95.0),
                    viewportSize = JsxGraphViewportSize(320.0, 240.0),
                    timestampMilliseconds = 121,
                    device = JsxGraphPointerDevice.TOUCH,
                ),
            ),
            boardPointerInteractionsForEvent(
                eventType = PointerEventType.Move,
                changes = listOf(
                    touchChange(
                        pointerId = 1,
                        x = 220.0f,
                        y = 210.0f,
                        timestampMilliseconds = 120,
                        pressed = true,
                        previousPressed = true,
                    ),
                    touchChange(
                        pointerId = 2,
                        x = 320.0f,
                        y = 190.0f,
                        timestampMilliseconds = 121,
                        pressed = true,
                        previousPressed = true,
                    ),
                ),
                viewportSize = IntSize(640, 480),
                density = 2.0f,
            ),
        )

        assertEquals(
            listOf(
                JsxGraphBoardPointerInteraction.Up(
                    pointerId = 1,
                    position = JsxGraphPoint2D(110.0, 105.0),
                    viewportSize = JsxGraphViewportSize(320.0, 240.0),
                    timestampMilliseconds = 130,
                ),
            ),
            boardPointerInteractionsForEvent(
                eventType = PointerEventType.Release,
                changes = listOf(
                    touchChange(
                        pointerId = 1,
                        x = 220.0f,
                        y = 210.0f,
                        timestampMilliseconds = 130,
                        pressed = false,
                        previousPressed = true,
                    ),
                    touchChange(
                        pointerId = 2,
                        x = 320.0f,
                        y = 190.0f,
                        timestampMilliseconds = 130,
                        pressed = true,
                        previousPressed = true,
                    ),
                ),
                viewportSize = IntSize(640, 480),
                density = 2.0f,
            ),
        )
    }

    private fun touchChange(
        pointerId: Long,
        x: Float,
        y: Float,
        timestampMilliseconds: Long,
        pressed: Boolean,
        previousPressed: Boolean,
    ): BoardPointerEventChange =
        BoardPointerEventChange(
            pointerId = pointerId,
            position = Offset(x, y),
            timestampMilliseconds = timestampMilliseconds,
            pressed = pressed,
            previousPressed = previousPressed,
            device = JsxGraphPointerDevice.TOUCH,
        )
}
