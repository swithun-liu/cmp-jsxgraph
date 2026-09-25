package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IntersectionLine3DParityTest {
    @Test
    fun focusedCaseProjectsPlaneIntersections() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find(
                "intersectionline3d_projection",
            ),
        ).value
        assertTrue("intersectionline3d" in parityCase.features)
        assertTrue("plane-plane" in parityCase.features)
        assertTrue("owned-points" in parityCase.features)

        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val scene =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession).scene
        val lines = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Line>()
            .filter { line ->
                line.id in setOf("verticalLine", "diagonalLine")
            }
            .associateBy(JsxGraphSceneElement.Line::id)

        assertEquals(setOf("verticalLine", "diagonalLine"), lines.keys)
        assertTrue(lines.values.all { line ->
            line.point1 != line.point2 &&
                line.style.strokeWidth == 4.0 &&
                line.style.layer == 12
        })
        assertEquals(
            JsxGraphColor(0, 114, 178),
            lines.getValue("verticalLine").style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            lines.getValue("diagonalLine").style.strokeColor,
        )
        assertNotEquals(
            lines.getValue("verticalLine").point1,
            lines.getValue("diagonalLine").point1,
        )
    }
}
