/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphHatchTest {
    @Test
    fun documentAndJessieCodeMatchOfficialDefaultsAndHashAlias() {
        val document = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "line",
                      "type": "segment",
                      "parents": [[-4, 2], [4, 2]],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "hatch",
                      "type": "hatch",
                      "parents": ["line", 3],
                      "attributes": {"name": ""}
                    },
                    {
                      "id": "alias",
                      "type": "hash",
                      "parents": ["line", 2.5],
                      "attributes": {"name": ""}
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value
        val jessieCode = scene(
            JsxGraphJessieCode.parse(
                """
                line = segment([-4, 2], [4, 2]) <<
                    id: "line", name: "", withLabel: false
                >>;
                hatch(line, 3) << id: "hatch", name: "" >>;
                hash(line, 2.5) << id: "alias", name: "" >>;
                """.trimIndent(),
            ),
        )

        assertEquals(document, jessieCode)
        val hatch = sceneTicks(document, "hatch")
        assertEquals(
            JsxGraphElementStyle(
                visible = true,
                strokeColor = JsxGraphColor(0, 114, 178),
                fillColor = JsxGraphColor.Transparent,
                strokeWidth = 2.0,
                strokeOpacity = 1.0,
                fillOpacity = 1.0,
                layer = 2,
            ),
            hatch.style,
        )
        assertContentEquals(
            listOf(-0.2, 0.0, 0.2),
            hatch.definition.fixedTicks,
        )
        assertEquals("middle", hatch.definition.anchor)
        assertTrue(hatch.definition.drawZero)
        assertEquals(20.0, hatch.definition.majorHeight)
        assertEquals(0.2, hatch.definition.ticksDistance)
        assertEquals(
            listOf(-0.2, 0.0, 0.2),
            resolveTicks(hatch).paths.map { it.points[1].x },
        )

        assertContentEquals(
            listOf(
                -0.15000000000000002,
                0.04999999999999999,
                0.25,
            ),
            sceneTicks(document, "alias").definition.fixedTicks,
        )
    }

    @Test
    fun hatchTracksParentMovementThroughPersistentSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    containerId = "hatch",
                    boundingBox =
                        JsxGraphBoundingBox(-5.0, 5.0, 5.0, -5.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(-4, 2) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(4, 2) <<
                    id: "B", name: "", withLabel: false
                >>;
                line = segment(A, B) <<
                    id: "line", name: "", withLabel: false
                >>;
                hatch(line, 3) << id: "hatch", name: "" >>;
                """.trimIndent(),
            ),
        )
        assertEquals(
            JsxGraphPoint2D(0.0, 2.0),
            resolveTicks(sceneTicks(initial, "hatch")).paths[1].points[1],
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "B",
                coordinates = JsxGraphPoint2D(4.0, 4.0),
            ),
        ).value
        val paths = resolveTicks(sceneTicks(moved, "hatch")).paths
        assertEquals(3, paths.size)
        assertPoint(
            expectedX = 0.0,
            expectedY = 3.0,
            actual = paths[1].points[1],
        )
        assertPoint(
            expectedX = -0.19402850002906638,
            expectedY = 2.9514928749927334,
            actual = paths[0].points[1],
        )
    }

    @Test
    fun hatchCountLimitsAreEnforcedBeforeEitherPublicFactoryRuns() {
        val documentError = assertIs<GMResult.Err<JsxGraphDocumentError>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "line",
                      "type": "segment",
                      "parents": [[-2, 0], [2, 0]],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "hatch",
                      "type": "hatch",
                      "parents": ["line", 2049],
                      "attributes": {"name": ""}
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).error
        val documentLimit =
            assertIs<JsxGraphDocumentError.TickCountLimitExceeded>(
                documentError,
            )
        assertEquals(2048, documentLimit.limit)
        assertEquals(2049, documentLimit.actual)

        val jessieError = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                """
                line = segment([-2, 0], [2, 0]) <<
                    id: "line", name: "", withLabel: false
                >>;
                hash(line, 2048.5) << id: "hash", name: "" >>;
                """.trimIndent(),
            ),
        ).error
        val jessieLimit =
            assertIs<JsxGraphJessieCodeError.ResourceLimitExceeded>(
                jessieError,
            )
        assertEquals("tick count", jessieLimit.resource)
        assertEquals(2048, jessieLimit.limit)
        assertEquals(2049, jessieLimit.requestedSize)
    }

    private fun scene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene = assertIs<GMResult.Ok<JsxGraphScene>>(result).value

    private fun sceneTicks(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Ticks =
        assertIs(scene.elements.single { it.id == id })

    private fun resolveTicks(
        ticks: JsxGraphSceneElement.Ticks,
    ): JsxGraphResolvedTicks =
        ticks.definition.resolve(
            visibleLeft = -5.0,
            visibleTop = 5.0,
            visibleRight = 5.0,
            visibleBottom = -5.0,
            cssPixelsPerUnitX = 50.0,
            cssPixelsPerUnitY = 50.0,
        )

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
