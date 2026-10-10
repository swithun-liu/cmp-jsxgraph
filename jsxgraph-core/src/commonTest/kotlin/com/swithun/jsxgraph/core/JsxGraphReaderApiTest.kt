/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphReaderApiTest {
    @Test
    fun publicFormatsExposeTheRegisteredUpstreamAliases() {
        assertEquals(
            setOf(
                "gxt",
                "geonext",
                "ggb",
                "geogebra",
                "i2g",
                "xml",
                "intergeo",
                "cdy",
                "cindy",
                "cinderella",
                "tracenpoche",
                "txt",
                "graph",
                "digraph",
                "jessiecode",
            ),
            JsxGraphReader.supportedFormats,
        )
    }

    @Test
    fun graphTextLoadsThroughThePublicSceneBoundary() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphReader.parse(
                source =
                    "-5 5 5 -5\ngraph\n2\nA 0 0\nB 1 1\nA B",
                format = "GRAPH",
            ),
        ).value

        assertEquals(
            JsxGraphBoundingBox(-5.0, 5.0, 5.0, -5.0),
            scene.boundingBox,
        )
        assertTrue(scene.keepAspectRatio)
        assertFalse(scene.axis)
        assertFalse(scene.grid)
        assertEquals(3, scene.elements.size)

        val pointA = assertIs<JsxGraphSceneElement.Point>(
            scene.elements[0],
        )
        assertEquals("A", pointA.name)
        assertEquals(JsxGraphPoint2D(0.0, 0.0), pointA.coordinates)
        val pointB = assertIs<JsxGraphSceneElement.Point>(
            scene.elements[1],
        )
        assertEquals("B", pointB.name)
        assertEquals(JsxGraphPoint2D(1.0, 1.0), pointB.coordinates)
        val segment = assertIs<JsxGraphSceneElement.Line>(
            scene.elements[2],
        )
        assertFalse(segment.straightFirst)
        assertFalse(segment.straightLast)
    }

    @Test
    fun jessieCodeKeepsTheOfficialNoReaderCallbackPath() {
        val options = JsxGraphReaderOptions(
            boundingBox = JsxGraphBoundingBox(-2.0, 3.0, 4.0, -5.0),
            axis = true,
            grid = true,
        )
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphReader.parse(
                source = "ignored",
                format = "JESSIECODE",
                options = options,
            ),
        ).value

        assertEquals(
            options.boundingBox.left,
            scene.boundingBox.left,
            1.0e-12,
        )
        assertEquals(
            options.boundingBox.top,
            scene.boundingBox.top,
            1.0e-12,
        )
        assertEquals(
            options.boundingBox.right,
            scene.boundingBox.right,
            1.0e-12,
        )
        assertEquals(
            options.boundingBox.bottom,
            scene.boundingBox.bottom,
            1.0e-12,
        )
        assertTrue(scene.axis)
        assertTrue(scene.grid)
        assertTrue(scene.elements.isEmpty())
    }

    @Test
    fun axisAttributeObjectEnablesTheReaderDefaultAxesModel() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphReader.parse(
                source = "ignored",
                format = "JESSIECODE",
                options = JsxGraphReaderOptions(
                    axis = false,
                    axisAttributes = mapOf(
                        "ticks" to mapOf("ticksDistance" to 2.0),
                    ),
                    defaultAxesAttributes = mapOf(
                        "y" to mapOf(
                            "ticks" to mapOf("minorTicks" to 3),
                        ),
                    ),
                ),
            ),
        ).value

        assertTrue(scene.axis)
        assertTrue(scene.elements.isEmpty())
    }

    @Test
    fun tracenpocheAliasUsesTheSamePublicSceneBoundary() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphReader.parse(
                source = "@figure;A=point(1,2);",
                format = "TRACENPOCHE",
            ),
        ).value

        val point = assertIs<JsxGraphSceneElement.Point>(
            scene.elements.single { it.name == "A" },
        )
        assertEquals(JsxGraphPoint2D(1.0, 2.0), point.coordinates)
    }

    @Test
    fun geogebraDefaultsReachThePublicSceneBoundary() {
        val parsed =
            JsxGraphReader.parse(
                source = """
                <geogebra format="5.0">
                  <euclidianView>
                    <coordSystem
                        xZero="250"
                        yZero="250"
                        scale="50"
                        yscale="50"
                    />
                  </euclidianView>
                  <kernel><decimals val="2"/></kernel>
                  <construction>
                    <element type="point" label="A">
                      <coords x="1" y="2" z="1"/>
                    </element>
                  </construction>
                </geogebra>
            """.trimIndent(),
                format = "GGB",
            )
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            value = parsed,
            message = parsed.toString(),
        ).value

        val point = assertIs<JsxGraphSceneElement.Point>(
            scene.elements.single {
                it is JsxGraphSceneElement.Point && it.name == "A"
            },
        )
        assertEquals(3.0, point.size)
        assertEquals("o", point.face)
        assertEquals(JsxGraphColor(0, 0, 0), point.style.fillColor)
        assertEquals(JsxGraphColor(0, 0, 0), point.style.strokeColor)
        assertEquals(1.0, point.style.fillOpacity)
        assertEquals(1.0, point.style.strokeWidth)
        assertTrue(
            scene.elements.any {
                it is JsxGraphSceneElement.Text && it.content == "A"
            },
        )
    }

    @Test
    fun publicFailuresRemainStructuredAndDoNotExposeInternalBoard() {
        val unknown = assertIs<
            GMResult.Err<JsxGraphReaderError.UnknownFormat>
        >(
            JsxGraphReader.parse(
                source = "data",
                format = "missing",
            ),
        ).error
        assertEquals("missing", unknown.format)

        val domain = assertIs<
            GMResult.Err<JsxGraphReaderError.ReaderFailure>
        >(
            JsxGraphReader.parse(
                source = "-5 5 5 -5\ngraph\n1\nA 0 0\nA missing",
                format = "graph",
            ),
        ).error
        assertEquals(JsxGraphReaderFailureStage.DOMAIN, domain.stage)
        assertEquals("graph", domain.format)

        assertIs<GMResult.Err<JsxGraphReaderError.SourceLengthExceeded>>(
            JsxGraphReader.parse(
                source = "1234",
                format = "graph",
                limits = JsxGraphEngineLimits(maxSourceLength = 3),
            ),
        )
        assertIs<GMResult.Err<JsxGraphReaderError.ObjectLimitExceeded>>(
            JsxGraphReader.parse(
                source =
                    "-5 5 5 -5\ngraph\n2\nA 0 0\nB 1 1\nA B",
                format = "graph",
                limits = JsxGraphEngineLimits(maxObjects = 2),
            ),
        )
        assertIs<GMResult.Err<JsxGraphReaderError.SceneSnapshotFailed>>(
            JsxGraphReader.parse(
                source = "-5 5 5 -5\ngraph\n1\nA invalid 0",
                format = "graph",
            ),
        )
        assertIs<GMResult.Err<JsxGraphReaderError.InvalidConfiguration>>(
            JsxGraphReader.parse(
                source = "",
                format = "graph",
                options = JsxGraphReaderOptions(
                    boundingBox =
                        JsxGraphBoundingBox(1.0, 1.0, 1.0, -1.0),
                ),
            ),
        )
        assertIs<GMResult.Err<JsxGraphReaderError.InvalidConfiguration>>(
            JsxGraphReader.parse(
                source = "",
                format = "graph",
                limits = JsxGraphEngineLimits(maxObjects = 0),
            ),
        )
    }
}
