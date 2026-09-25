package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Polyhedron3DParityTest {
    @Test
    fun focusedCaseExpandsAllFacesWithOfficialAttributePrecedence() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("polyhedron3d_faces"),
        ).value
        assertTrue("polyhedron3d" in parityCase.features)
        assertTrue("face3d" in parityCase.features)

        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val scene =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession).scene
        val colors = setOf(
            JsxGraphColor(0, 158, 115),
            JsxGraphColor(86, 180, 233),
            JsxGraphColor(230, 159, 0),
        )
        val faces = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .filter { it.style.fillColor in colors }

        assertEquals(6, faces.size)
        assertEquals(
            1,
            faces.count {
                it.style.fillColor == JsxGraphColor(0, 158, 115)
            },
        )
        assertEquals(
            2,
            faces.count {
                it.style.fillColor == JsxGraphColor(86, 180, 233)
            },
        )
        assertEquals(
            3,
            faces.count {
                it.style.fillColor == JsxGraphColor(230, 159, 0)
            },
        )
        assertTrue(faces.all { it.points.size == 5 })
        assertTrue(faces.all { it.style.layer == 12 })
    }
}
