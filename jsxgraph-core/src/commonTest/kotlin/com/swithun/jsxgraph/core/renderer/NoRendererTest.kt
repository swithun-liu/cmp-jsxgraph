/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.renderer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NoRendererTest {
    @Test
    fun metadataAndReturnValuesMatchUpstream() {
        val renderer = NoRenderer()

        assertEquals(false, renderer.enhancedRendering)
        assertEquals("no", renderer.type)
        assertNull(renderer.getElementById("missing"))
        assertEquals(Unit, renderer.removeToInsertLater().invoke())
    }

    @Test
    fun representativeRenderingOperationsHaveNoEffects() {
        val renderer = NoRenderer()
        val marker = Any()

        assertEquals(Unit, renderer.drawPoint(marker))
        assertEquals(Unit, renderer.updateLine(marker))
        assertEquals(Unit, renderer.drawCurve(marker))
        assertEquals(Unit, renderer.updateEllipse(marker))
        assertEquals(Unit, renderer.drawPolygon(marker))
        assertEquals(Unit, renderer.drawText(marker))
        assertEquals(Unit, renderer.drawImage(marker))
        assertEquals(Unit, renderer.setObjectStrokeWidth(marker, 2.0))
        assertEquals(Unit, renderer.suspendRedraw())
        assertEquals(Unit, renderer.unsuspendRedraw())
    }
}
