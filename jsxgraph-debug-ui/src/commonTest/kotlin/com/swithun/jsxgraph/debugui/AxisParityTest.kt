package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AxisParityTest {
    @Test
    fun focusedCaseProjectsAxisPositioningAndGeneratedTicks() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("axis_2d"),
        ).value
        assertTrue("fixed-axis" in parityCase.features)
        assertTrue("sticky-axis" in parityCase.features)
        assertTrue("ticks-auto-position" in parityCase.features)

        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val scene =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession).scene
        val axes = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Line>()
            .associateBy(JsxGraphSceneElement.Line::id)
        val ticks = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Ticks>()
            .associateBy(JsxGraphSceneElement.Ticks::id)

        assertEquals(
            setOf("horizontal", "vertical", "sticky"),
            axes.keys,
        )
        assertEquals(
            setOf(
                "horizontalTicks",
                "verticalTicks",
                "stickyTicks",
            ),
            ticks.filterValues { it.style.visible }.keys,
        )
        assertEquals(
            listOf(-4.0, -2.0, 0.0, 2.0, 4.0),
            ticks.getValue("horizontalTicks").definition.fixedTicks,
        )
        assertEquals(
            null,
            ticks.getValue("verticalTicks").definition.fixedTicks,
        )
        assertEquals("fixed", axes.getValue("horizontal").axis?.position)
        assertEquals("fixed", axes.getValue("vertical").axis?.position)
        assertEquals("sticky", axes.getValue("sticky").axis?.position)
        assertTrue(ticks.getValue("stickyTicks").style.visible)
    }
}
