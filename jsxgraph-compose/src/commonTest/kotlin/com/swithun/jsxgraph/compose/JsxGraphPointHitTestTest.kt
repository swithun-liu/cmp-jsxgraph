/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.geometry.Offset
import com.swithun.jsxgraph.core.JsxGraphBoundingBox
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphElementStyle
import com.swithun.jsxgraph.core.JsxGraphPoint2D
import com.swithun.jsxgraph.core.JsxGraphScene
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsxGraphPointHitTestTest {
    @Test
    fun hitTestingUsesUpstreamPointToleranceAndReverseCreationOrder() {
        val scene = scene(
            point(id = "first"),
            point(id = "last"),
        )

        assertEquals(
            "last",
            draggablePointAt(
                scene = scene,
                position = Offset(71.0f, 50.0f),
                width = 120.0f,
                height = 100.0f,
                density = 2.0f,
            )?.id,
        )
        assertNull(
            draggablePointAt(
                scene = scene,
                position = Offset(73.0f, 50.0f),
                width = 120.0f,
                height = 100.0f,
                density = 2.0f,
            ),
        )
    }

    @Test
    fun hiddenFixedAndNonRealPointsAreNotInteractive() {
        val scene = scene(
            point(id = "fixed", draggable = false),
            point(id = "hidden", visible = false),
            point(id = "non-real", isReal = false),
        )

        assertNull(
            draggablePointAt(
                scene = scene,
                position = Offset(60.0f, 50.0f),
                width = 120.0f,
                height = 100.0f,
                density = 1.0f,
            ),
        )
    }

    private fun scene(
        vararg points: JsxGraphSceneElement.Point,
    ): JsxGraphScene =
        JsxGraphScene(
            boundingBox = JsxGraphBoundingBox(
                left = -6.0,
                top = 5.0,
                right = 6.0,
                bottom = -5.0,
            ),
            axis = false,
            grid = false,
            keepAspectRatio = true,
            elements = points.toList(),
        )

    private fun point(
        id: String,
        draggable: Boolean = true,
        visible: Boolean = true,
        isReal: Boolean = true,
    ): JsxGraphSceneElement.Point =
        JsxGraphSceneElement.Point(
            id = id,
            name = "",
            style = JsxGraphElementStyle(
                visible = visible,
                strokeColor = JsxGraphColor(0, 0, 0),
                fillColor = JsxGraphColor(0, 0, 0),
                strokeWidth = 2.0,
                strokeOpacity = 1.0,
                fillOpacity = 1.0,
            ),
            coordinates = JsxGraphPoint2D(0.0, 0.0),
            size = 3.0,
            face = "o",
            draggable = draggable,
            isReal = isReal,
        )
}
