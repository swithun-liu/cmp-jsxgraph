/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class TextAnchorTest {
    @Test
    fun anchorOffsetsMatchCanvasTextAlignment() {
        val anchor = Offset(100.0f, 80.0f)

        assertEquals(
            Offset(100.0f, 70.0f),
            textTopLeft(
                anchor = anchor,
                width = 40.0f,
                height = 20.0f,
                anchorX = "left",
                anchorY = "middle",
            ),
        )
        assertEquals(
            Offset(80.0f, 80.0f),
            textTopLeft(
                anchor = anchor,
                width = 40.0f,
                height = 20.0f,
                anchorX = "middle",
                anchorY = "top",
            ),
        )
        assertEquals(
            Offset(60.0f, 60.0f),
            textTopLeft(
                anchor = anchor,
                width = 40.0f,
                height = 20.0f,
                anchorX = "right",
                anchorY = "bottom",
            ),
        )
    }
}
