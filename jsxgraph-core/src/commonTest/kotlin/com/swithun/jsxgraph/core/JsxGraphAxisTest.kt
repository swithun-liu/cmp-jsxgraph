/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsxGraphAxisTest {
    @Test
    fun documentAndJessieCodeExposeAxisAndGeneratedTicks() {
        val document = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "axis",
                      "type": "axis",
                      "parents": [[0, 0], [1, 0]],
                      "attributes": {
                        "position": "fixed",
                        "anchor": "right",
                        "anchorDist": "25px",
                        "ticksAutoPos": true,
                        "ticks": {
                          "id": "axisTicks",
                          "ticksDistance": 2,
                          "minorTicks": 0,
                          "drawZero": true,
                          "insertTicks": false
                        }
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value
        val jessieCode = scene(
            JsxGraphJessieCode.parse(
                """
                axis([0, 0], [1, 0]) <<
                    id: "axis",
                    position: "fixed",
                    anchor: "right",
                    anchorDist: "25px",
                    ticksAutoPos: true,
                    ticks: <<
                        id: "axisTicks",
                        ticksDistance: 2,
                        minorTicks: 0,
                        drawZero: true,
                        insertTicks: false
                    >>
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(document, jessieCode)
        assertEquals(2, document.elements.size)
        val axis = sceneLine(document, "axis")
        assertEquals(
            JsxGraphElementStyle(
                visible = true,
                strokeColor = JsxGraphColor(102, 102, 102),
                fillColor = JsxGraphColor.Transparent,
                strokeWidth = 1.0,
                strokeOpacity = 1.0,
                fillOpacity = 1.0,
                layer = 2,
            ),
            axis.style,
        )
        assertEquals(
            JsxGraphArrowHead(
                type = 1,
                size = 8.0,
                highlightSize = 8.0,
            ),
            axis.lastArrow,
        )
        val resolvedAxis = assertNotNull(axis.axis).resolvePoints(
            currentPoint1 = axis.point1,
            currentPoint2 = axis.point2,
            visibleLeft = -5.0,
            visibleTop = 5.0,
            visibleRight = 5.0,
            visibleBottom = -5.0,
            cssPixelsPerUnitX = 50.0,
            cssPixelsPerUnitY = 50.0,
        )
        assertPoint(0.0, -4.5, resolvedAxis.first)
        assertPoint(1.0, -4.5, resolvedAxis.second)

        val ticks = sceneTicks(document, "axisTicks")
        assertEquals(
            JsxGraphElementStyle(
                visible = true,
                strokeColor = JsxGraphColor(102, 102, 102),
                fillColor = JsxGraphColor.Transparent,
                strokeWidth = 1.0,
                strokeOpacity = 0.25,
                fillOpacity = 1.0,
                layer = 2,
            ),
            ticks.style,
        )
        assertEquals(null, ticks.definition.fixedTicks)
        assertTrue(ticks.definition.drawLabels)
        assertEquals(false, ticks.definition.insertTicks)
        assertEquals(-1.0, ticks.definition.majorHeight)
        val resolvedTicks = ticks.definition.resolve(
            visibleLeft = -5.0,
            visibleTop = 5.0,
            visibleRight = 5.0,
            visibleBottom = -5.0,
            cssPixelsPerUnitX = 50.0,
            cssPixelsPerUnitY = 50.0,
        )
        assertEquals(
            listOf(0.0, 2.0, 4.0, -2.0, -4.0),
            resolvedTicks.paths.map { path -> path.points[1].x },
        )
        assertTrue(
            resolvedTicks.paths.all { path ->
                path.points.map(JsxGraphPoint2D::y).toSet() ==
                    setOf(-5.0, 5.0)
            },
        )
    }

    @Test
    fun verticalAxisAutoPositionsLabelsTowardTheBoardEdge() {
        val scene = scene(
            JsxGraphJessieCode.parse(
                """
                axis([0, 0], [0, 1]) <<
                    id: "axis",
                    position: "fixed",
                    anchor: "right",
                    anchorDist: "10%",
                    ticksAutoPos: true,
                    ticks: <<
                        id: "axisTicks",
                        ticksDistance: 10,
                        minorTicks: 0,
                        drawZero: true,
                        insertTicks: false,
                        label: <<
                            anchorX: "right",
                            offset: [6, 0]
                        >>
                    >>
                >>;
                """.trimIndent(),
            ),
        )
        val resolved = sceneTicks(scene, "axisTicks").definition.resolve(
            visibleLeft = -5.0,
            visibleTop = 5.0,
            visibleRight = 5.0,
            visibleBottom = -5.0,
            cssPixelsPerUnitX = 50.0,
            cssPixelsPerUnitY = 50.0,
        )

        val labelStyle = assertNotNull(resolved.labelStyle)
        assertEquals("left", labelStyle.anchorX)
        assertEquals(6.0, labelStyle.offsetX)
        assertEquals(4.12, resolved.labels.single().position.x)
    }

    @Test
    fun sessionRefreshesDynamicAxisEndpointsAndGeneratedTicks() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                driver = point(0, 0) <<
                    id: "driver", name: "", withLabel: false
                >>;
                start = point(function () {
                    return [-2, driver.Y()];
                }) <<
                    id: "start", name: "", withLabel: false,
                    visible: false
                >>;
                end = point(function () {
                    return [2, driver.Y()];
                }) <<
                    id: "end", name: "", withLabel: false,
                    visible: false
                >>;
                axis(start, end) <<
                    id: "axis",
                    ticks: <<
                        id: "axisTicks",
                        ticksDistance: 2,
                        insertTicks: false,
                        minorTicks: 0,
                        drawZero: true,
                        majorHeight: 12
                    >>
                >>;
                """.trimIndent(),
            ),
        )
        assertPoint(0.0, 0.0, scenePoint(initial, "driver").coordinates)
        assertEquals(0.0, sceneLine(initial, "axis").point1.y)
        assertEquals(
            0.0,
            assertIs<JsxGraphTicksParent2D.Line>(
                sceneTicks(initial, "axisTicks").definition.parent,
            ).point1.y,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(0.0, 2.0),
            ),
        ).value

        assertPoint(0.0, 2.0, scenePoint(moved, "driver").coordinates)
        assertEquals(2.0, sceneLine(moved, "axis").point1.y)
        val movedTicks = sceneTicks(moved, "axisTicks")
        assertEquals(
            2.0,
            assertIs<JsxGraphTicksParent2D.Line>(
                movedTicks.definition.parent,
            ).point1.y,
        )
        assertTrue(
            movedTicks.definition.resolve(
                visibleLeft = -5.0,
                visibleTop = 5.0,
                visibleRight = 5.0,
                visibleBottom = -5.0,
                cssPixelsPerUnitX = 50.0,
                cssPixelsPerUnitY = 50.0,
            ).paths.all { path ->
                path.points.map(JsxGraphPoint2D::y).average() == 2.0
            },
        )
    }

    @Test
    fun axisExpansionAndFixedTicksHonorResourceLimits() {
        val documentLimit = assertIs<
            GMResult.Err<JsxGraphDocumentError.ObjectLimitExceeded>,
            >(
            JsxGraphEngine.parse(
                source =
                    """
                    {
                      "boundingBox": [-5, 5, 5, -5],
                      "objects": [
                        {
                          "id": "axis",
                          "type": "axis",
                          "parents": [[0, 0], [1, 0]]
                        }
                      ]
                    }
                    """.trimIndent(),
                limits = JsxGraphEngineLimits(maxObjects = 1),
            ),
        ).error
        assertEquals(1, documentLimit.limit)
        assertEquals(2, documentLimit.actual)

        val jessieLimit = assertIs<
            GMResult.Err<JsxGraphJessieCodeError.ResourceLimitExceeded>,
            >(
            JsxGraphJessieCode.parse(
                """axis([0, 0], [1, 0]) << id: "axis" >>;""",
                limits = JsxGraphJessieCodeLimits(maxObjects = 1),
            ),
        ).error
        assertEquals("created element count", jessieLimit.resource)
        assertEquals(1, jessieLimit.limit)
        assertEquals(2, jessieLimit.requestedSize)

        val fixedTicks = (0..2048).joinToString(",")
        val tickLimit = assertIs<
            GMResult.Err<JsxGraphJessieCodeError.ResourceLimitExceeded>,
            >(
            JsxGraphJessieCode.parse(
                """
                axis([0, 0], [1, 0]) <<
                    id: "axis",
                    ticks: <<
                        ticks: [$fixedTicks],
                        ticksDistance: undefined
                    >>
                >>;
                """.trimIndent(),
            ),
        ).error
        assertEquals("tick count", tickLimit.resource)
        assertEquals(2048, tickLimit.limit)
        assertEquals(2049, tickLimit.requestedSize)

        val ignoredTicks = scene(
            JsxGraphJessieCode.parse(
                """
                axis([0, 0], [1, 0]) <<
                    id: "axis",
                    ticks: <<
                        id: "axisTicks",
                        ticks: [$fixedTicks]
                    >>
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(
            null,
            sceneTicks(ignoredTicks, "axisTicks").definition.fixedTicks,
        )
    }

    private fun scene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene = assertIs<GMResult.Ok<JsxGraphScene>>(result).value

    private fun scenePoint(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Point =
        assertIs(scene.elements.single { it.id == id })

    private fun sceneLine(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Line =
        assertIs(scene.elements.single { it.id == id })

    private fun sceneTicks(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Ticks =
        assertIs(scene.elements.single { it.id == id })

    private fun assertPoint(
        expectedX: Double,
        expectedY: Double,
        actual: JsxGraphPoint2D,
    ) {
        assertEquals(
            expectedX,
            actual.x,
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            expectedY,
            actual.y,
            absoluteTolerance = TOLERANCE,
        )
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
