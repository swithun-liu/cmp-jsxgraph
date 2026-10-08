/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.geometry.Offset
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphElementStyle
import com.swithun.jsxgraph.core.JsxGraphShadow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RendererVisualStyleTest {
    @Test
    fun shadowPaintMatchesSvgBlendOpacityAndCssPixelScaling() {
        val paint = style().copy(
            shadow = JsxGraphShadow(
                enabled = true,
                color = JsxGraphColor(10, 20, 30),
                opacity = 0.5,
                blur = 5.0,
                blend = 0.5,
                offsetX = 3.0,
                offsetY = 4.0,
            ),
        ).shadowPaint(
            sourceColor = JsxGraphColor(100, 110, 120),
            sourceOpacity = 0.8,
            density = 2.0f,
        )

        requireNotNull(paint)
        assertEquals(60f / 255f, paint.color.red, COLOR_TOLERANCE)
        assertEquals(75f / 255f, paint.color.green, COLOR_TOLERANCE)
        assertEquals(90f / 255f, paint.color.blue, COLOR_TOLERANCE)
        assertEquals(0.4f, paint.color.alpha, COLOR_TOLERANCE)
        assertEquals(Offset(6.0f, 8.0f), paint.offset)
        assertEquals(10.0f, paint.blurRadius)
    }

    @Test
    fun disabledShadowAndPointFaceAliasesAreStable() {
        assertNull(
            style().shadowPaint(
                sourceColor = JsxGraphColor(1, 2, 3),
                sourceOpacity = 1.0,
                density = 1.0f,
            ),
        )
        assertEquals("o", normalizePointFace("circle"))
        assertEquals("[]", normalizePointFace("square"))
        assertEquals("^", normalizePointFace("A"))
        assertEquals("<<>>", normalizePointFace("diamond2"))
    }

    private fun style(): JsxGraphElementStyle =
        JsxGraphElementStyle(
            visible = true,
            strokeColor = JsxGraphColor(0, 0, 0),
            fillColor = JsxGraphColor.Transparent,
            strokeWidth = 1.0,
            strokeOpacity = 1.0,
            fillOpacity = 1.0,
        )

    private companion object {
        const val COLOR_TOLERANCE = 1.0f / 255.0f + 1.0e-6f
    }
}
