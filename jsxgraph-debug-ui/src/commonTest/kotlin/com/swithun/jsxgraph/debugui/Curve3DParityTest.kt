package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class Curve3DParityTest {
    @Test
    fun focusedCaseProjectsAllCurve3DParentForms() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("curve3d_projection"),
        ).value
        assertTrue("curve3d" in parityCase.features)
        assertTrue("proxy-curve" in parityCase.features)

        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val scene =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession).scene
        val curves = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .associateBy(JsxGraphSceneElement.Curve::id)

        assertEquals(199, curves.getValue("helix").points.size)
        assertEquals(160, curves.getValue("vectorCurve").points.size)
        assertEquals(4, curves.getValue("dataCurve").points.size)
        assertEquals(4, curves.getValue("moved").points.size)
        assertEquals(
            JsxGraphColor(0, 114, 178),
            curves.getValue("helix").style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(204, 121, 167),
            curves.getValue("moved").style.strokeColor,
        )
        assertNotEquals(
            curves.getValue("dataCurve").points,
            curves.getValue("moved").points,
        )
    }
}
