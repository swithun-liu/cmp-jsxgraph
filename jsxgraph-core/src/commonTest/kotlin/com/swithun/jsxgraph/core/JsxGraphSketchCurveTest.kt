/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphSketchCurveTest {
    @Test
    fun jessieCodeProducesOfficialEmptySketchCurveScene() {
        val result = JsxGraphJessieCode.parse(
            """
            sketchcurve() <<
                id: "sketch",
                name: "",
                withLabel: false
            >>;
            """.trimIndent(),
        )
        assertTrue(result is GMResult.Ok, result.toString())
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(result).value

        val curve = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements.single(),
        )
        assertEquals("sketch", curve.id)
        assertTrue(curve.points.isEmpty())
        assertEquals(1, curve.bezierDegree)
        assertEquals("round", curve.lineCap)
        assertEquals(
            JsxGraphColor(red = 213, green = 94, blue = 0),
            curve.style.strokeColor,
        )
        assertEquals(1.0, curve.style.strokeWidth)
        assertTrue(curve.style.visible)
    }
}
