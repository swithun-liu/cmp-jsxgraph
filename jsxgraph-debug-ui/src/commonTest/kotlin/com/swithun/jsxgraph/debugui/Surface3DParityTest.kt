package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Surface3DParityTest {
    @Test
    fun focusedCaseProjectsWireframeAndShadedTriangleFaces() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("surface3d_projection"),
        ).value
        assertTrue("surface3d" in parityCase.features)
        assertTrue("functiongraph3d" in parityCase.features)
        assertTrue("shader" in parityCase.features)

        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val scene =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession).scene
        val curves = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
        val wire = curves.single { curve -> curve.id == "wire" }
        val filled = curves.single { curve -> curve.id == "filled" }
        val faces = curves.filter { curve ->
            curve.id != "filled" &&
                curve.points.size == 4 &&
                curve.style.strokeWidth == 0.4 &&
                curve.style.fillOpacity == 0.86
        }

        assertEquals(199, wire.points.size)
        assertEquals(
            JsxGraphColor(0, 114, 178),
            wire.style.strokeColor,
        )
        assertTrue(wire.style.visible)
        assertTrue(filled.points.isEmpty())
        assertTrue(!filled.style.visible)
        assertEquals(93, faces.size)
        assertTrue(faces.all { face -> face.style.layer == 12 })
        assertTrue(
            faces.map { face -> face.style.fillColor }.distinct().size > 1,
        )
    }
}
