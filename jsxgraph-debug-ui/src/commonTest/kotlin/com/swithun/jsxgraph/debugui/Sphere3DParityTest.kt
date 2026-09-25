package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphFillGradient
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
        assertTrue("radial-gradient" in parityCase.features)

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
            JsxGraphColor(255, 255, 255),
            spheres.getValue("parallelSphere").style.fillColor,
        )
        assertEquals(
            JsxGraphColor(255, 244, 214),
            spheres.getValue("centralSphere").style.fillColor,
        )
        assertEquals(
            JsxGraphFillGradient.Radial(
                secondColor = JsxGraphColor(0, 255, 128),
                secondOpacity = 1.0,
                startOffset = 0.0,
                endOffset = 1.0,
                centerX = 0.5,
                centerY = 0.5,
                radius = 0.5,
                focalX = 0.7,
                focalY = 0.3,
                focalRadius = 0.0,
            ),
            spheres.getValue("parallelSphere").style.fillGradient,
        )
        assertEquals(
            JsxGraphColor(230, 159, 0),
            assertIs<JsxGraphFillGradient.Radial>(
                spheres.getValue("centralSphere").style.fillGradient,
            ).secondColor,
        )
        assertEquals(
            0.4,
            spheres.getValue("parallelSphere").style.fillOpacity,
        )
        assertEquals(
            0.55,
            spheres.getValue("centralSphere").style.fillOpacity,
        )
        assertTrue(spheres.values.all { sphere -> sphere.style.layer == 12 })
    }
}
