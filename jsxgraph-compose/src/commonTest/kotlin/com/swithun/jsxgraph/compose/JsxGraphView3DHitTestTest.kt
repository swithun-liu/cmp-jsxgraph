/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.geometry.Offset
import com.swithun.jsxgraph.core.JsxGraphBoundingBox
import com.swithun.jsxgraph.core.JsxGraphPoint2D
import com.swithun.jsxgraph.core.JsxGraphScene
import com.swithun.jsxgraph.core.JsxGraphView3D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsxGraphView3DHitTestTest {
    @Test
    fun hitTestingUsesViewBoundsAndReverseCreationOrder() {
        val scene = scene(
            view(id = "first", lowerLeftX = -4.0),
            view(id = "last", lowerLeftX = -2.0),
        )

        assertEquals(
            "last",
            view3DAt(scene, Offset(0.0f, 0.0f))?.id,
        )
        assertEquals(
            "first",
            view3DAt(scene, Offset(-3.0f, 0.0f))?.id,
        )
        assertNull(view3DAt(scene, Offset(5.0f, 0.0f)))
    }

    @Test
    fun disabledPointerNavigationIsNotHit() {
        val scene = scene(
            view(
                id = "disabled",
                lowerLeftX = -4.0,
                pointerNavigationEnabled = false,
            ),
        )

        assertNull(view3DAt(scene, Offset(0.0f, 0.0f)))
    }

    private fun scene(
        vararg views: JsxGraphView3D,
    ): JsxGraphScene =
        JsxGraphScene(
            boundingBox = JsxGraphBoundingBox(-5.0, 4.0, 5.0, -4.0),
            axis = false,
            grid = false,
            keepAspectRatio = false,
            elements = emptyList(),
            views3D = views.toList(),
        )

    private fun view(
        id: String,
        lowerLeftX: Double,
        pointerNavigationEnabled: Boolean = true,
    ): JsxGraphView3D =
        JsxGraphView3D(
            id = id,
            lowerLeftCorner = JsxGraphPoint2D(lowerLeftX, -3.0),
            size = JsxGraphPoint2D(4.0, 6.0),
            azimuth = 1.0,
            elevation = 0.3,
            bank = 0.0,
            cameraDistance = 1.01,
            trackballEnabled = false,
            pointerNavigationEnabled = pointerNavigationEnabled,
            keyboardNavigationEnabled = true,
        )
}
