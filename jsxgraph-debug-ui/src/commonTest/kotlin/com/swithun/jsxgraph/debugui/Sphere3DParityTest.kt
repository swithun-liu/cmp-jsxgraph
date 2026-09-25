package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Sphere3DParityTest {
    @Test
    fun focusedCaseProjectsParallelCircleAndCentralEllipse() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("sphere3d_projection"),
        ).value
        assertTrue("sphere3d" in parityCase.features)
        assertTrue("proxy-circle" in parityCase.features)
        assertTrue("proxy-ellipse" in parityCase.features)

        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val scene =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession).scene
        val spheres = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .filter { curve ->
                curve.id in setOf("parallelSphere", "centralSphere")
            }
            .associateBy(JsxGraphSceneElement.Curve::id)

        assertEquals(
            setOf("parallelSphere", "centralSphere"),
            spheres.keys,
        )
        assertEquals(13, spheres.getValue("parallelSphere").points.size)
        assertEquals(160, spheres.getValue("centralSphere").points.size)
        assertEquals(
            JsxGraphColor(86, 180, 233),
            spheres.getValue("parallelSphere").style.fillColor,
        )
        assertEquals(
            JsxGraphColor(230, 159, 0),
            spheres.getValue("centralSphere").style.fillColor,
        )
        assertTrue(spheres.values.all { sphere ->
            sphere.style.fillOpacity == 0.38 &&
                sphere.style.layer == 12
        })
    }
}
