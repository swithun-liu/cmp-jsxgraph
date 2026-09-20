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

class JsxGraphLayerOrderTest {
    @Test
    fun renderOrderUsesLayerThenCreationOrder() {
        val scene = scene(
            point(id = "high-first", layer = 9),
            point(id = "low", layer = 3),
            point(id = "middle", layer = 8),
            point(id = "high-last", layer = 9),
        )

        assertEquals(
            listOf("low", "middle", "high-first", "high-last"),
            scene.renderOrderedElements().map(JsxGraphSceneElement::id),
        )
    }

    @Test
    fun renderItemsInterleaveScaffoldingAndPolygonPartsByLayer() {
        val polygon = JsxGraphSceneElement.Polygon(
            id = "polygon",
            name = "",
            style = style(layer = 5),
            vertices = listOf(
                JsxGraphPoint2D(-1.0, -1.0),
                JsxGraphPoint2D(1.0, -1.0),
                JsxGraphPoint2D(0.0, 1.0),
            ),
            implicitVertices = listOf(
                point(id = "vertex-first", layer = 9),
                point(id = "vertex-last", layer = 9),
            ),
            borderStyle = style(layer = 5),
            withLines = true,
        )
        val scene = scene(
            point(id = "below-grid", layer = 0),
            point(id = "between-axis-and-polygon", layer = 4),
            polygon,
            point(id = "above-vertices", layer = 10),
            axis = true,
            grid = true,
        )

        assertEquals(
            listOf(
                "below-grid",
                "grid",
                "axis",
                "between-axis-and-polygon",
                "polygon:border",
                "polygon:fill",
                "polygon:vertex-first",
                "polygon:vertex-last",
                "above-vertices",
            ),
            scene.renderOrderedItems().map { item ->
                when (item) {
                    is JsxGraphSceneRenderItem.Grid -> "grid"
                    is JsxGraphSceneRenderItem.Axis -> "axis"
                    is JsxGraphSceneRenderItem.Element -> item.element.id
                    is JsxGraphSceneRenderItem.PolygonFill ->
                        "${item.polygon.id}:fill"
                    is JsxGraphSceneRenderItem.PolygonBorder ->
                        "${item.polygon.id}:border"
                    is JsxGraphSceneRenderItem.PolygonVertex ->
                        "${item.polygon.id}:${item.vertex.id}"
                }
            },
        )
    }

    @Test
    fun hitTestingPrioritizesHigherLayerBeforeLaterCreation() {
        val scene = scene(
            point(id = "higher", layer = 9),
            point(id = "later-but-lower", layer = 8),
        )

        assertEquals(
            "higher",
            draggablePointAt(
                scene = scene,
                position = Offset(60.0f, 50.0f),
                width = 120.0f,
                height = 100.0f,
                density = 1.0f,
            )?.id,
        )
    }

    private fun scene(
        vararg elements: JsxGraphSceneElement,
        axis: Boolean = false,
        grid: Boolean = false,
    ): JsxGraphScene =
        JsxGraphScene(
            boundingBox = JsxGraphBoundingBox(
                left = -6.0,
                top = 5.0,
                right = 6.0,
                bottom = -5.0,
            ),
            axis = axis,
            grid = grid,
            keepAspectRatio = true,
            elements = elements.toList(),
        )

    private fun point(
        id: String,
        layer: Int,
    ): JsxGraphSceneElement.Point =
        JsxGraphSceneElement.Point(
            id = id,
            name = "",
            style = style(layer),
            coordinates = JsxGraphPoint2D(0.0, 0.0),
            size = 3.0,
            face = "o",
            draggable = true,
        )

    private fun style(
        layer: Int,
    ): JsxGraphElementStyle =
        JsxGraphElementStyle(
            visible = true,
            strokeColor = JsxGraphColor(0, 0, 0),
            fillColor = JsxGraphColor(0, 0, 0),
            strokeWidth = 2.0,
            strokeOpacity = 1.0,
            fillOpacity = 1.0,
            layer = layer,
        )
}
