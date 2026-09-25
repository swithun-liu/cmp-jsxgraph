package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Circle3DParityTest {
    @Test
    fun focusedCaseProjectsAllCircle3DParentForms() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("circle3d_projection"),
        ).value
        assertTrue("circle3d" in parityCase.features)
        assertTrue("proxy-curve" in parityCase.features)

        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val scene =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession).scene
        val targetIds = setOf("xy", "yz", "tilted")
        val circles = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .filter { curve -> curve.id in targetIds }
            .associateBy(JsxGraphSceneElement.Curve::id)

        assertEquals(targetIds, circles.keys)
        assertEquals(160, circles.getValue("xy").points.size)
        assertEquals(160, circles.getValue("yz").points.size)
        assertEquals(160, circles.getValue("tilted").points.size)
        assertEquals(
            JsxGraphColor(0, 114, 178),
            circles.getValue("xy").style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            circles.getValue("yz").style.strokeColor,
        )
        assertTrue(
            circles.getValue("yz").style.strokeDashPattern.isNotEmpty(),
        )
    }
}
