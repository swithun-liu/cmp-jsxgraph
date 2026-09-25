package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IntersectionCircle3DParityTest {
    @Test
    fun focusedCaseProjectsPlaneAndSphereIntersections() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find(
                "intersectioncircle3d_projection",
            ),
        ).value
        assertTrue("intersectioncircle3d" in parityCase.features)
        assertTrue("plane-sphere" in parityCase.features)
        assertTrue("sphere-sphere" in parityCase.features)

        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val scene =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession).scene
        val circles = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .filter { curve ->
                curve.id in setOf("planeCircle", "sphereCircle")
            }
            .associateBy(JsxGraphSceneElement.Curve::id)

        assertEquals(setOf("planeCircle", "sphereCircle"), circles.keys)
        assertTrue(circles.values.all { circle ->
            circle.points.size == 160 &&
                circle.style.strokeWidth == 4.0 &&
                circle.style.layer == 12
        })
        assertEquals(
            JsxGraphColor(0, 114, 178),
            circles.getValue("planeCircle").style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            circles.getValue("sphereCircle").style.strokeColor,
        )
    }
}
