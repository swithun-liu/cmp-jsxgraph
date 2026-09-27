/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneTextMarkupTest {
    @Test
    fun convertsCompleteSuperscriptAndSubscriptMarkupToStyledText() {
        val text = sceneTextAnnotatedString(
            content = "A=12 cm<sup>2</sup>, x<sub>1</sub>",
            fontSize = 12.0,
        )

        assertEquals("A=12 cm2, x1", text.text)
        assertEquals(2, text.spanStyles.size)
        assertEquals(7, text.spanStyles[0].start)
        assertEquals(8, text.spanStyles[0].end)
        assertEquals(
            BaselineShift.Superscript,
            text.spanStyles[0].item.baselineShift,
        )
        assertEquals(9.sp, text.spanStyles[0].item.fontSize)
        assertEquals(11, text.spanStyles[1].start)
        assertEquals(12, text.spanStyles[1].end)
        assertEquals(
            BaselineShift.Subscript,
            text.spanStyles[1].item.baselineShift,
        )
    }

    @Test
    fun convertsExactHtmlLineBreakMarkupToNewline() {
        assertEquals(
            "P=2.00 cm\n3.00 cm",
            sceneTextAnnotatedString(
                content = "P=2.00 cm<br />3.00 cm",
                fontSize = 12.0,
            ).text,
        )
    }

    @Test
    fun preservesSanitizedHtmlMarkupAsLiteralText() {
        assertEquals(
            "P=2.00 cm<br />3.00 cm",
            sceneTextAnnotatedString(
                content = "P=2.00 cm&lt;br /&gt;3.00 cm",
                fontSize = 12.0,
            ).text,
        )
    }

    @Test
    fun preservesUnclosedAndUnsupportedMarkupAsLiteralText() {
        assertEquals(
            "A<sup>2",
            sceneTextAnnotatedString(
                content = "A<sup>2",
                fontSize = 12.0,
            ).text,
        )
        assertEquals(
            "A<strong>2</strong>",
            sceneTextAnnotatedString(
                content = "A<strong>2</strong>",
                fontSize = 12.0,
            ).text,
        )
    }
}
