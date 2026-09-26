/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphElementStyle
import com.swithun.jsxgraph.core.JsxGraphPoint2D
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsxGraphImageTest {
    @Test
    fun dataUriResolverReportsUnsupportedMalformedAndOversizedSources() {
        assertIs<JsxGraphImageResolveError.UnsupportedSource>(
            assertIs<GMResult.Err<*>>(
                JsxGraphDataUriImageResolver().resolve("asset:checker"),
            ).error,
        )
        assertIs<JsxGraphImageResolveError.InvalidDataUri>(
            assertIs<GMResult.Err<*>>(
                JsxGraphDataUriImageResolver().resolve(
                    "data:image/png;base64,not-base64!",
                ),
            ).error,
        )
        assertIs<JsxGraphImageResolveError.EncodedDataTooLarge>(
            assertIs<GMResult.Err<*>>(
                JsxGraphDataUriImageResolver(maxDecodedBytes = 1)
                    .resolve(ONE_PIXEL_PNG),
            ).error,
        )
        assertIs<JsxGraphImageResolveError.InvalidConfiguration>(
            assertIs<GMResult.Err<*>>(
                JsxGraphDataUriImageResolver(maxDecodedBytes = -1)
                    .resolve(ONE_PIXEL_PNG),
            ).error,
        )
    }

    @Test
    fun screenGeometryMapsBitmapTopLeftAndBasisVectors() {
        val image = JsxGraphSceneElement.Image(
            id = "image",
            name = "",
            style = style(),
            source = "asset:checker",
            anchor = JsxGraphPoint2D(-2.0, -1.0),
            widthVector = JsxGraphPoint2D(3.0, 1.0),
            heightVector = JsxGraphPoint2D(-1.0, 2.0),
            userWidth = 3.0,
            userHeight = 2.0,
        )
        val geometry = imageScreenGeometry(
            image = image,
            metrics = BoardMetrics(
                width = 500.0f,
                height = 500.0f,
                requestedLeft = -5.0f,
                requestedTop = 5.0f,
                requestedRight = 5.0f,
                requestedBottom = -5.0f,
                keepAspectRatio = false,
            ),
        )

        assertEquals(100.0f, geometry.topLeft.x)
        assertEquals(200.0f, geometry.topLeft.y)
        assertEquals(150.0f, geometry.horizontal.x)
        assertEquals(-50.0f, geometry.horizontal.y)
        assertEquals(50.0f, geometry.vertical.x)
        assertEquals(100.0f, geometry.vertical.y)
    }

    private fun style(): JsxGraphElementStyle =
        JsxGraphElementStyle(
            visible = true,
            strokeColor = JsxGraphColor.Transparent,
            fillColor = JsxGraphColor.Transparent,
            strokeWidth = 0.0,
            strokeOpacity = 1.0,
            fillOpacity = 1.0,
        )

    private companion object {
        const val ONE_PIXEL_PNG =
            "data:image/png;base64," +
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwC" +
                "AAAAC0lEQVR42mP8/x8AAusB9Y9WlYoAAAAASUVORK5CYII="
    }
}
