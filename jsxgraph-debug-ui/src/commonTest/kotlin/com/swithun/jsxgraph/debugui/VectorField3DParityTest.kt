package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import com.swithun.jsxgraph.core.JsxGraphVectorField3D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VectorField3DParityTest {
    @Test
    fun focusedCaseProjectsComponentAndArrayFields() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("vectorfield3d_projection"),
        ).value
        assertTrue("vectorfield3d" in parityCase.features)
        assertTrue("pixel-arrowhead" in parityCase.features)

        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val scene =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession).scene
        val curves = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .filter { it.id in setOf("rotational", "radial") }
            .associateBy(JsxGraphSceneElement.Curve::id)

        val rotational = assertIs<JsxGraphVectorField3D>(
            curves.getValue("rotational").vectorField3D,
        )
        val radial = assertIs<JsxGraphVectorField3D>(
            curves.getValue("radial").vectorField3D,
        )
        assertEquals(36, rotational.vectors.size)
        assertTrue(rotational.arrowEnabled)
        assertEquals(6.0, rotational.arrowSize)
        assertEquals(9, radial.vectors.size)
        assertFalse(radial.arrowEnabled)
        assertEquals(
            JsxGraphColor(0, 114, 178),
            curves.getValue("rotational").style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            curves.getValue("radial").style.strokeColor,
        )
    }
}
