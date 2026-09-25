package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Polygon3DParityTest {
    @Test
    fun focusedCaseProjectsOwnedAndReferencedVertices() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("polygon3d_projection"),
        ).value
        assertTrue("polygon3d" in parityCase.features)
        assertTrue("proxy-polygon" in parityCase.features)

        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val scene =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession).scene
        val polygons = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Polygon>()
            .associateBy(JsxGraphSceneElement.Polygon::id)
        val owned = polygons.getValue("owned")
        val referenced = polygons.getValue("referenced")

        assertEquals(4, owned.vertices.size)
        assertEquals(4, owned.implicitVertices.size)
        assertEquals(JsxGraphColor(240, 228, 66), owned.style.fillColor)
        assertEquals(JsxGraphColor(73, 84, 93), owned.borderStyle.strokeColor)
        assertEquals(3.0, owned.borderStyle.strokeWidth)
        assertTrue(owned.implicitVertices.all { it.size == 5.0 })

        assertEquals(3, referenced.vertices.size)
        assertTrue(referenced.implicitVertices.isEmpty())
        assertEquals(
            JsxGraphColor(86, 180, 233),
            referenced.style.fillColor,
        )
        assertEquals(
            JsxGraphColor(22, 135, 122),
            referenced.borderStyle.strokeColor,
        )
    }
}
