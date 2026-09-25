/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphElementStyle
import com.swithun.jsxgraph.core.JsxGraphFillGradient
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class FillGradientGeometryTest {
    @Test
    fun linearGeometryMatchesSvgNormalizedAngle() {
        val bounds = Rect(
            left = 10.0f,
            top = 20.0f,
            right = 110.0f,
            bottom = 220.0f,
        )

        assertEquals(
            LinearGradientGeometry(
                start = Offset(10.0f, 20.0f),
                end = Offset(110.0f, 20.0f),
            ),
            linearGradientGeometry(bounds, angle = 0.0),
        )
        val vertical = linearGradientGeometry(bounds, angle = PI * 0.5)
        assertEquals(10.0f, vertical.start.x, absoluteTolerance = 1.0e-4f)
        assertEquals(20.0f, vertical.start.y, absoluteTolerance = 1.0e-4f)
        assertEquals(10.0f, vertical.end.x, absoluteTolerance = 1.0e-4f)
        assertEquals(220.0f, vertical.end.y, absoluteTolerance = 1.0e-4f)
    }

    @Test
    fun radialContoursInterpolateSvgObjectBoundingBoxCircles() {
        val gradient = radialGradient()
        val bounds = Rect(
            left = 0.0f,
            top = 0.0f,
            right = 200.0f,
            bottom = 100.0f,
        )

        assertContourEquals(
            RadialGradientContour(
                center = Offset(140.0f, 30.0f),
                radiusX = 0.0f,
                radiusY = 0.0f,
            ),
            radialGradientContour(bounds, gradient, progress = 0.0f),
        )
        assertContourEquals(
            RadialGradientContour(
                center = Offset(120.0f, 40.0f),
                radiusX = 50.0f,
                radiusY = 25.0f,
            ),
            radialGradientContour(bounds, gradient, progress = 0.5f),
        )
        assertContourEquals(
            RadialGradientContour(
                center = Offset(100.0f, 50.0f),
                radiusX = 100.0f,
                radiusY = 50.0f,
            ),
            radialGradientContour(bounds, gradient, progress = 1.0f),
        )
    }

    @Test
    fun gradientColorsApplySvgStopAndElementOpacity() {
        val gradient = radialGradient(
            secondOpacity = 0.5,
            startOffset = 0.25,
            endOffset = 0.75,
        )
        val style = style(fillOpacity = 0.4)
        val stops = gradientStopColors(style, gradient)

        assertEquals(0.16f, stops.first.alpha, absoluteTolerance = COLOR_TOLERANCE)
        assertEquals(0.2f, stops.second.alpha, absoluteTolerance = COLOR_TOLERANCE)
        assertEquals(
            stops.first,
            gradientColorAt(style, gradient, position = 0.0f),
        )
        val midpoint = gradientColorAt(style, gradient, position = 0.5f)
        assertEquals(0.5f, midpoint.red, absoluteTolerance = COLOR_TOLERANCE)
        assertEquals(1.0f, midpoint.green, absoluteTolerance = COLOR_TOLERANCE)
        assertEquals(0.5f, midpoint.blue, absoluteTolerance = COLOR_TOLERANCE)
        assertEquals(0.18f, midpoint.alpha, absoluteTolerance = COLOR_TOLERANCE)
        assertEquals(
            stops.second,
            gradientColorAt(style, gradient, position = 1.0f),
        )
    }

    @Test
    fun radialRasterResolutionIsBounded() {
        assertEquals(
            256,
            radialGradientStepCount(Rect(0.0f, 0.0f, 40.0f, 50.0f)),
        )
        assertEquals(
            640,
            radialGradientStepCount(Rect(0.0f, 0.0f, 640.0f, 300.0f)),
        )
        assertEquals(
            1024,
            radialGradientStepCount(Rect(0.0f, 0.0f, 2_000.0f, 300.0f)),
        )
    }

    private fun radialGradient(
        secondOpacity: Double = 1.0,
        startOffset: Double = 0.0,
        endOffset: Double = 1.0,
    ): JsxGraphFillGradient.Radial =
        JsxGraphFillGradient.Radial(
            secondColor = JsxGraphColor(0, 255, 0),
            secondOpacity = secondOpacity,
            startOffset = startOffset,
            endOffset = endOffset,
            centerX = 0.5,
            centerY = 0.5,
            radius = 0.5,
            focalX = 0.7,
            focalY = 0.3,
            focalRadius = 0.0,
        )

    private fun style(
        fillOpacity: Double,
    ): JsxGraphElementStyle =
        JsxGraphElementStyle(
            visible = true,
            strokeColor = JsxGraphColor.Transparent,
            fillColor = JsxGraphColor(255, 255, 255),
            strokeWidth = 0.0,
            strokeOpacity = 0.0,
            fillOpacity = fillOpacity,
        )

    private fun assertContourEquals(
        expected: RadialGradientContour,
        actual: RadialGradientContour,
    ) {
        assertEquals(
            expected.center.x,
            actual.center.x,
            absoluteTolerance = 1.0e-4f,
        )
        assertEquals(
            expected.center.y,
            actual.center.y,
            absoluteTolerance = 1.0e-4f,
        )
        assertEquals(
            expected.radiusX,
            actual.radiusX,
            absoluteTolerance = 1.0e-4f,
        )
        assertEquals(
            expected.radiusY,
            actual.radiusY,
            absoluteTolerance = 1.0e-4f,
        )
    }

    private companion object {
        const val COLOR_TOLERANCE = 1.0f / 255.0f + 1.0e-6f
    }
}
