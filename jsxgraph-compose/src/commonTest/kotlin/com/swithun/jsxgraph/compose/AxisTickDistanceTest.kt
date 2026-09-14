package com.swithun.jsxgraph.compose

import kotlin.test.Test
import kotlin.test.assertEquals

class AxisTickDistanceTest {
    @Test
    fun followsJsxGraphAutomaticMajorTickDistance() {
        assertEquals(
            2.0f,
            jsxGraphMajorTickDistance(
                visibleDistance = 15.34f,
                cssPixelsPerUnit = 24.25f,
            ),
        )
        assertEquals(
            1.0f,
            jsxGraphMajorTickDistance(
                visibleDistance = 12.0f,
                cssPixelsPerUnit = 27.67f,
            ),
        )
        assertEquals(
            2.0f,
            jsxGraphMajorTickDistance(
                visibleDistance = 10.0f,
                cssPixelsPerUnit = 24.25f,
            ),
        )
        assertEquals(
            2.0f,
            jsxGraphMajorTickDistance(
                visibleDistance = 30.0f,
                cssPixelsPerUnit = 30.0f,
            ),
        )
    }
}
