/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphGrid2D
import com.swithun.jsxgraph.core.JsxGraphGridForceSquare
import com.swithun.jsxgraph.core.JsxGraphGridRole
import com.swithun.jsxgraph.core.JsxGraphResolvedGrid
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GridParityTest {
    @Test
    fun focusedCaseProjectsAxisDrivenMajorAndMinorGridCurves() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("grid_2d"),
        ).value
        assertTrue("axis-parent" in parityCase.features)
        assertTrue("major-grid" in parityCase.features)
        assertTrue("minor-grid" in parityCase.features)

        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val scene =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession).scene
        val grids = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .filter { curve -> curve.grid != null }
            .associateBy(JsxGraphSceneElement.Curve::id)

        assertEquals(setOf("mesh", "meshMinor"), grids.keys)
        val major = grids.getValue("mesh")
        val minor = grids.getValue("meshMinor")
        assertEquals(JsxGraphGridRole.Major, major.grid?.role)
        assertEquals(JsxGraphGridRole.Minor, minor.grid?.role)
        assertEquals(
            JsxGraphGridForceSquare.None,
            major.grid?.forceSquare,
        )
        assertEquals(2.0, major.grid?.parentMajorStep?.x)
        assertEquals(2.0, major.grid?.parentMajorStep?.y)
        assertEquals(1.0, minor.grid?.parentMinorElements?.x)
        assertEquals(2.0, minor.grid?.parentMinorElements?.y)
        assertEquals(JsxGraphColor(213, 94, 0), major.style.strokeColor)
        assertEquals(JsxGraphColor(192, 192, 192), major.style.fillColor)
        assertEquals(1.0, major.style.fillOpacity)
        assertEquals(JsxGraphColor(0, 114, 178), minor.style.strokeColor)
        assertEquals(0.65, minor.style.strokeOpacity)
        assertTrue(resolve(requireNotNull(major.grid)).points.isNotEmpty())
        assertTrue(resolve(requireNotNull(minor.grid)).points.isNotEmpty())
    }

    private fun resolve(grid: JsxGraphGrid2D): JsxGraphResolvedGrid =
        assertIs<GMResult.Ok<JsxGraphResolvedGrid>>(
            grid.resolve(
                visibleLeft = -6.0,
                visibleTop = 5.0,
                visibleRight = 6.0,
                visibleBottom = -5.0,
                cssPixelsPerUnitX = 50.0,
                cssPixelsPerUnitY = 50.0,
            ),
        ).value
}
