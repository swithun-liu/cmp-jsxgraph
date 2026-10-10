/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.geometry.Offset
import com.swithun.jsxgraph.core.JsxGraphAffineTransform
import kotlin.test.Test
import kotlin.test.assertEquals

class TextAnchorTest {
    @Test
    fun anchorOffsetsMatchCanvasTextAlignment() {
        val anchor = Offset(100.0f, 80.0f)

        assertEquals(
            Offset(100.0f, 70.0f),
            textTopLeft(
                anchor = anchor,
                width = 40.0f,
                height = 20.0f,
                anchorX = "left",
                anchorY = "middle",
            ),
        )
        assertEquals(
            Offset(80.0f, 80.0f),
            textTopLeft(
                anchor = anchor,
                width = 40.0f,
                height = 20.0f,
                anchorX = "middle",
                anchorY = "top",
            ),
        )
        assertEquals(
            Offset(60.0f, 60.0f),
            textTopLeft(
                anchor = anchor,
                width = 40.0f,
                height = 20.0f,
                anchorX = "right",
                anchorY = "bottom",
            ),
        )
    }

    @Test
    fun htmlControlTransformOriginMatchesTextAnchor() {
        val topLeft = textTransformOrigin("left", "top")
        assertEquals(0.0f, topLeft.pivotFractionX)
        assertEquals(0.0f, topLeft.pivotFractionY)

        val middle = textTransformOrigin("middle", "middle")
        assertEquals(0.5f, middle.pivotFractionX)
        assertEquals(0.5f, middle.pivotFractionY)

        val bottomRight = textTransformOrigin("right", "bottom")
        assertEquals(1.0f, bottomRight.pivotFractionX)
        assertEquals(1.0f, bottomRight.pivotFractionY)
    }

    @Test
    fun affineTransformResolvesAcrossViewportAndUsesComposeMatrixOrder() {
        val source = JsxGraphAffineTransform(
            a = 1.1200317547305485,
            b = -0.560048094716167,
            c = 0.15849364905389024,
            d = 0.774519052838329,
            e = -6.076859021798441,
            f = 215.218313001771,
            sourceViewportWidth = 640.0,
            sourceViewportHeight = 480.0,
        )
        val metrics = BoardMetrics(
            width = 1280.0f,
            height = 720.0f,
            requestedLeft = -8.0f,
            requestedTop = 6.0f,
            requestedRight = 8.0f,
            requestedBottom = -6.0f,
            keepAspectRatio = false,
        )
        val resolved = source.resolveFor(metrics)

        assertEquals(source.a, resolved.a, absoluteTolerance = 1.0e-12)
        assertEquals(
            0.75 * source.b,
            resolved.b,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            4.0 / 3.0 * source.c,
            resolved.c,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(source.d, resolved.d, absoluteTolerance = 1.0e-12)
        assertEquals(2.0 * source.e, resolved.e, absoluteTolerance = 1.0e-12)
        assertEquals(1.5 * source.f, resolved.f, absoluteTolerance = 1.0e-12)

        val origin = Offset(100.0f, 50.0f)
        val matrix = resolved.toComposeMatrix(localOrigin = origin)
        val localPoint = Offset(7.0f, 11.0f)
        val mapped = matrix.map(localPoint)
        assertEquals(
            (
                resolved.a * (origin.x + localPoint.x) +
                    resolved.c * (origin.y + localPoint.y) +
                    resolved.e -
                    origin.x
                ).toFloat(),
            mapped.x,
            absoluteTolerance = 1.0e-4f,
        )
        assertEquals(
            (
                resolved.b * (origin.x + localPoint.x) +
                    resolved.d * (origin.y + localPoint.y) +
                    resolved.f -
                    origin.y
                ).toFloat(),
            mapped.y,
            absoluteTolerance = 1.0e-4f,
        )
    }
}
