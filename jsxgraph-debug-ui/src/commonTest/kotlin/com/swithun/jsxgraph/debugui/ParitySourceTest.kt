package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.compose.GeometryPlaygroundScene
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParitySourceTest {
    @Test
    fun defaultSourceProducesTheNativeScene() {
        val scene = assertIs<GMResult.Ok<GeometryPlaygroundScene>>(
            parseParitySource(DEFAULT_PARITY_SOURCE),
        ).value
        assertEquals(-4.0f, scene.fixedPoint.x)
        assertEquals(-2.0f, scene.fixedPoint.y)
        assertEquals(3.2f, scene.controlPoint.x)
        assertEquals(2.1f, scene.controlPoint.y)
        assertEquals(2.35f, scene.circleRadius)
        assertEquals(2.0f, scene.sineAmplitude)
        assertEquals(0.16f, scene.parabolaQuadratic)
    }

    @Test
    fun malformedAndUnsupportedSourcesReturnExplicitErrors() {
        assertIs<GMResult.Err<String>>(parseParitySource("not-json"))
        assertIs<GMResult.Err<String>>(
            parseParitySource(
                """
                {
                  "schemaVersion": 2,
                  "boundingBox": [-6, 5, 6, -5]
                }
                """.trimIndent(),
            ),
        )
        assertIs<GMResult.Err<String>>(
            parseParitySource(
                DEFAULT_PARITY_SOURCE.replace(
                    "\"radius\": 2.35",
                    "\"radius\": 0",
                ),
            ),
        )
        assertIs<GMResult.Err<String>>(
            parseParitySource(
                DEFAULT_PARITY_SOURCE.replace(
                    "\"amplitude\": 2",
                    "\"amplitude\": 1e300",
                ),
            ),
        )
    }
}
