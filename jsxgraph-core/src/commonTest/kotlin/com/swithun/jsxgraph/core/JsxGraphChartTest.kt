/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphChartTest {
    @Test
    fun linePointSplineAndFitStylesExpandToNativeSceneElements() {
        val scene = scene(
            """
            chart([1, 2, 4], [3, -1, 2]) <<
                chartStyle: "line, point",
                name: "",
                strokeColor: "#123456"
            >>;
            chart([1, 2, 4], [3, -1, 2]) <<
                chartStyle: "spline",
                name: ""
            >>;
            chart([1, 2, 3], [2, 5, 10]) <<
                chartStyle: "fit",
                degree: 2,
                name: ""
            >>;
            """.trimIndent(),
        )

        assertEquals(
            3,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Curve>().size,
        )
        val points =
            scene.elements.filterIsInstance<JsxGraphSceneElement.Point>()
        assertEquals(3, points.size)
        assertEquals(
            listOf(
                JsxGraphPoint2D(1.0, 3.0),
                JsxGraphPoint2D(2.0, -1.0),
                JsxGraphPoint2D(4.0, 2.0),
            ),
            points.map(JsxGraphSceneElement.Point::coordinates),
        )
    }

    @Test
    fun barAndPieStylesPreserveGeometryLabelsAndColors() {
        val scene = scene(
            """
            chart([1, 3, 6], [2, -1, 4]) <<
                chartStyle: "bar",
                name: "",
                colors: ["#ff0000", "#00ff00"],
                labels: ["A", "B", "C"]
            >>;
            chart(2, 3, 5) <<
                chartStyle: "pie",
                name: "",
                center: [1, -1],
                radius: 3,
                labels: ["P", "Q", "R"],
                colors: ["#ff0000", "#00ff00", "#0000ff"]
            >>;
            """.trimIndent(),
        )

        val polygons =
            scene.elements.filterIsInstance<JsxGraphSceneElement.Polygon>()
        assertEquals(3, polygons.size)
        assertTrue(polygons.all { it.vertices.size == 4 })
        assertEquals(
            6,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Text>().size,
        )
        val sectors =
            scene.elements.filterIsInstance<JsxGraphSceneElement.Curve>()
                .filter { it.name in setOf("P", "Q", "R") }
        assertEquals(3, sectors.size)
    }

    @Test
    fun radarAndLegendExpandOnlyVisibleChildren() {
        val scene = scene(
            """
            chart([2, 4, 3], [4, 1, 5], [3, 2, 4]) <<
                chartStyle: "radar",
                name: "",
                paramArray: ["A", "B", "C"],
                labelArray: ["first", "second", "third"],
                colors: ["#ff0000", "#0000ff"],
                radius: 4,
                start: 0,
                showCircles: true,
                circleLabelArray: [0, 50, 100],
                legendPosition: "right"
            >>;
            legend(-7, 7) <<
                labels: ["one", "two"],
                colors: ["#ff0000", "#0000ff"]
            >>;
            """.trimIndent(),
        )

        assertEquals(
            5,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Line>().size,
        )
        assertEquals(
            3,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Polygon>().size,
        )
        assertEquals(
            3,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Circle>().size,
        )
        assertEquals(
            0,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Point>().size,
        )
        assertEquals(
            8,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Text>().size,
        )
    }

    @Test
    fun constructionDocumentUsesTheSameCompositeExpansion() {
        val result = JsxGraphEngine.parse(
            """
            {
              "schemaVersion": 1,
              "boundingBox": [-5, 5, 8, -5],
              "axis": false,
              "grid": false,
              "keepAspectRatio": true,
              "objects": [
                {
                  "id": "bars",
                  "type": "chart",
                  "parents": [[1, 2], [3, 4]],
                  "attributes": {
                    "chartStyle": "bar",
                    "colors": ["#ff0000", "#00ff00"]
                  }
                },
                {
                  "id": "legend",
                  "type": "legend",
                  "parents": [5, 4],
                  "attributes": {
                    "labels": ["A", "B"],
                    "colors": ["#ff0000", "#00ff00"]
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value
        assertEquals(
            2,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Polygon>().size,
        )
        assertEquals(
            2,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Line>().size,
        )
    }

    @Test
    fun invalidDataAndRadarConfigurationReturnStructuredFailures() {
        for (
            source in listOf(
                "chart() << chartStyle: \"line\" >>;",
                "chart([]) << chartStyle: \"line\" >>;",
                """
                chart([1, 2], [3, 4]) << chartStyle: "radar" >>;
                """.trimIndent(),
                """
                chart([1, 2]) <<
                    chartStyle: "radar",
                    paramArray: ["A", "B", "C"]
                >>;
                """.trimIndent(),
            )
        ) {
            val result = JsxGraphJessieCode.parse(source)
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                result,
                result.toString(),
            )
        }
    }

    @Test
    fun compositeSceneCountIsPreflightedWithoutPollutingPersistentBoard() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                limits = JsxGraphJessieCodeLimits(maxObjects = 1),
            ),
        ).value

        val error = assertIs<
            GMResult.Err<JsxGraphJessieCodeError.ResourceLimitExceeded>
            >(
            session.execute(
                """
                chart([1, 2]) <<
                    chartStyle: "point",
                    id: "shared",
                    name: ""
                >>;
                """.trimIndent(),
                storeSource = false,
            ),
        ).error
        assertEquals(2, error.requestedSize)

        val recovered = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute(
                """
                point(0, 0) << id: "shared", name: "" >>;
                """.trimIndent(),
                storeSource = false,
            ),
        ).value
        assertEquals(
            listOf("shared"),
            recovered.elements
                .filterIsInstance<JsxGraphSceneElement.Point>()
                .map(JsxGraphSceneElement.Point::id),
        )

        val tableSession = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    runtimeOptions = JsxGraphRuntimeOptions(
                        chartTableProvider =
                            JsxGraphChartTableProvider {
                                GMResult.Ok(
                                    JsxGraphChartTableSnapshot(
                                        listOf(listOf("1", "2")),
                                    ),
                                )
                            },
                    ),
                ),
                limits = JsxGraphJessieCodeLimits(maxObjects = 1),
            ),
        ).value
        val tableError = assertIs<
            GMResult.Err<JsxGraphJessieCodeError.ResourceLimitExceeded>
            >(
            tableSession.execute(
                """
                chart("scores") <<
                    chartStyle: "point",
                    id: "shared",
                    name: ""
                >>;
                """.trimIndent(),
                storeSource = false,
            ),
        ).error
        assertEquals(2, tableError.requestedSize)
        assertIs<GMResult.Ok<JsxGraphScene>>(
            tableSession.execute(
                """point(0, 0) << id: "shared", name: "" >>;""",
                storeSource = false,
            ),
        )

        val constructionError = assertIs<
            GMResult.Err<JsxGraphDocumentError.ObjectLimitExceeded>
            >(
            JsxGraphEngine.parse(
                """
                {
                  "schemaVersion": 1,
                  "boundingBox": [-5, 5, 5, -5],
                  "axis": false,
                  "grid": false,
                  "keepAspectRatio": false,
                  "objects": [
                    {
                      "id": "bars",
                      "type": "chart",
                      "parents": [[1, 2], [3, 4]],
                      "attributes": {"chartStyle": "bar"}
                    }
                  ]
                }
                """.trimIndent(),
                limits = JsxGraphEngineLimits(maxObjects = 1),
            ),
        ).error
        assertEquals(2, constructionError.actual)
    }

    @Test
    fun zeroChildChartAndLegendRemainValidCompositeResults() {
        val scene = scene(
            """
            chart([1, 2]) <<
                chartStyle: "unknown",
                name: ""
            >>;
            legend(0, 0) <<
                labels: [],
                colors: ["#ff0000"]
            >>;
            """.trimIndent(),
        )

        assertTrue(scene.elements.isEmpty())
    }

    @Test
    fun pieLabelCoordinatesTrackFunctionValuedData() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute(
                """
                value = 3;
                chart(1, function() { return value; }) <<
                    chartStyle: "pie",
                    name: "",
                    radius: 4,
                    labels: ["A", "B"]
                >>;
                """.trimIndent(),
            ),
        ).value
        val before = initial.elements
            .filterIsInstance<JsxGraphSceneElement.Text>()
            .single { it.content == "A" }
            .coordinates

        val updated = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute("value = 1;", storeSource = false),
        ).value
        val after = updated.elements
            .filterIsInstance<JsxGraphSceneElement.Text>()
            .single { it.content == "A" }
            .coordinates

        assertTrue(before != after)
    }

    @Test
    fun pieHighlightBySizeMatchesHoverIdempotencyAndLeaveScaling() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initialResult = session.execute(
                """
                chart(1, 1) <<
                    chartStyle: "pie",
                    name: "",
                    center: [0, 0],
                    radius: 2,
                    labels: ["P", "Q"],
                    highlightOnSector: true,
                    highlightBySize: true
                >>;
                """.trimIndent(),
            )
        val initial = assertIs<GMResult.Ok<JsxGraphScene>>(
            initialResult,
            initialResult.toString(),
        ).value
        assertEquals(2.0, startRadius(initial, "Q"), 1e-12)
        assertEquals(12.0, chartLabel(initial, "Q").fontSize, 1e-12)
        val hoverPosition = sectorInteriorScreenPosition(initial, "P")

        val highlighted = movePointer(
            session,
            hoverPosition.x,
            hoverPosition.y,
            100,
        )
        assertTrue(
            chartSector(highlighted, "Q").style.highlighted,
            highlighted.elements
                .filterIsInstance<JsxGraphSceneElement.Curve>()
                .map { it.name to it.style.highlighted }
                .toString(),
        )
        assertEquals(2.2, startRadius(highlighted, "Q"), 1e-12)
        assertEquals(24.0, chartLabel(highlighted, "Q").fontSize, 1e-12)

        val repeated = movePointer(
            session,
            hoverPosition.x,
            hoverPosition.y,
            150,
        )
        val repeatedRadius = 2.2 * 0.9090909 * 1.1
        assertEquals(
            repeatedRadius,
            startRadius(repeated, "Q"),
            1e-12,
        )
        assertEquals(24.0, chartLabel(repeated, "Q").fontSize, 1e-12)

        val left = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.interactPointer(JsxGraphBoardPointerInteraction.Leave),
        ).value
        assertTrue(!chartSector(left, "Q").style.highlighted)
        assertEquals(
            repeatedRadius * 0.9090909,
            startRadius(left, "Q"),
            1e-12,
        )
        assertEquals(12.0, chartLabel(left, "Q").fontSize, 1e-12)
    }

    @Test
    fun tableProviderPreservesHeadersRowsAndGroupedBarExpansion() {
        val runtimeOptions = JsxGraphRuntimeOptions(
            chartTableProvider = JsxGraphChartTableProvider { tableId ->
                assertEquals("scores", tableId)
                GMResult.Ok(
                    JsxGraphChartTableSnapshot(
                        rows = listOf(
                            listOf("row", "Q1", "Q2", "Q3"),
                            listOf("alpha", "1", "2", "3"),
                            listOf("beta", "4", "5", "6"),
                        ),
                    ),
                )
            },
        )
        val lineResult = JsxGraphJessieCode.parse(
                """
                chart("scores") <<
                    chartStyle: "line",
                    withHeaders: true,
                    rows: ["beta"]
                >>;
                """.trimIndent(),
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    runtimeOptions = runtimeOptions,
                ),
            )
        val line = assertIs<GMResult.Ok<JsxGraphScene>>(
            lineResult,
            lineResult.toString(),
        ).value.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single()
        assertEquals("Q1", line.name)
        assertEquals(
            listOf(
                JsxGraphPoint2D(1.0, 4.0),
                JsxGraphPoint2D(2.0, 5.0),
                JsxGraphPoint2D(3.0, 6.0),
            ),
            line.points,
        )

        val barsResult = JsxGraphJessieCode.parse(
                """
                chart("scores") <<
                    chartStyle: "bar",
                    withHeaders: true
                >>;
                """.trimIndent(),
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    runtimeOptions = runtimeOptions,
                ),
            )
        val bars = assertIs<GMResult.Ok<JsxGraphScene>>(
            barsResult,
            barsResult.toString(),
        ).value.elements
            .filterIsInstance<JsxGraphSceneElement.Polygon>()
        assertEquals(6, bars.size)
        val firstSeriesX =
            bars[0].vertices.map(JsxGraphPoint2D::x).distinct()
        assertEquals(0.6, firstSeriesX[0], absoluteTolerance = 1e-12)
        assertEquals(1.0, firstSeriesX[1], absoluteTolerance = 1e-12)
        val secondSeriesX =
            bars[3].vertices.map(JsxGraphPoint2D::x).distinct()
        assertEquals(1.0, secondSeriesX[0], absoluteTolerance = 1e-12)
        assertEquals(1.4, secondSeriesX[1], absoluteTolerance = 1e-12)
    }

    @Test
    fun tableProviderRefreshesVisibleChildrenInTheCurrentUpdate() {
        var rows = listOf(listOf("1", "2"))
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    runtimeOptions = JsxGraphRuntimeOptions(
                        chartTableProvider =
                            JsxGraphChartTableProvider {
                                GMResult.Ok(
                                    JsxGraphChartTableSnapshot(rows),
                                )
                            },
                    ),
                ),
            ),
        ).value
        val initialResult = session.execute(
                """
                chart("scores") <<
                    chartStyle: "line",
                    name: ""
                >>;
                """.trimIndent(),
            )
        val initial = assertIs<GMResult.Ok<JsxGraphScene>>(
            initialResult,
            initialResult.toString(),
        ).value
        assertEquals(
            listOf(
                JsxGraphPoint2D(1.0, 1.0),
                JsxGraphPoint2D(2.0, 2.0),
            ),
            initial.elements
                .filterIsInstance<JsxGraphSceneElement.Curve>()
                .single()
                .points,
        )

        rows = listOf(listOf("5", "7"))
        val updated = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute("tick = 1;", storeSource = false),
        ).value
        assertEquals(
            listOf(
                JsxGraphPoint2D(1.0, 5.0),
                JsxGraphPoint2D(2.0, 7.0),
            ),
            updated.elements
                .filterIsInstance<JsxGraphSceneElement.Curve>()
                .single()
                .points,
        )
    }

    @Test
    fun tableProviderFailuresAndShapeChangesAreStructuredAndRecoverable() {
        val missingProvider = assertIs<
            GMResult.Err<JsxGraphJessieCodeError.Runtime>
            >(
            JsxGraphJessieCode.parse("""chart("missing");"""),
        ).error
        assertTrue(missingProvider.reason.contains("provider is unavailable"))

        val callbackFailure = assertIs<
            GMResult.Err<JsxGraphJessieCodeError.Runtime>
            >(
            JsxGraphJessieCode.parse(
                """chart("broken");""",
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    runtimeOptions = JsxGraphRuntimeOptions(
                        chartTableProvider =
                            JsxGraphChartTableProvider {
                                throw IllegalStateException("table boom")
                            },
                    ),
                ),
            ),
        ).error
        assertTrue(callbackFailure.reason.contains("table boom"))

        var rows = listOf(listOf("1", "2"))
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    runtimeOptions = JsxGraphRuntimeOptions(
                        chartTableProvider =
                            JsxGraphChartTableProvider {
                                GMResult.Ok(
                                    JsxGraphChartTableSnapshot(rows),
                                )
                            },
                    ),
                ),
            ),
        ).value
        val creationResult = session.execute(
                """chart("scores") << name: "" >>;""",
            )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            creationResult,
            creationResult.toString(),
        )

        rows = listOf(listOf("1", "2", "3"))
        val shapeError = assertIs<
            GMResult.Err<JsxGraphJessieCodeError.Scene>
            >(
            session.execute("tick = 1;", storeSource = false),
        ).error.error
        assertIs<JsxGraphDocumentError.ChartDataUpdate>(shapeError)
        assertTrue(shapeError.reason.contains("width from 2 to 3"))

        rows = listOf(listOf("8", "9"))
        val recovered = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute("tick = 2;", storeSource = false),
        ).value
        assertEquals(
            JsxGraphPoint2D(1.0, 8.0),
            recovered.elements
                .filterIsInstance<JsxGraphSceneElement.Curve>()
                .single()
                .points
                .first(),
        )
    }

    private fun scene(source: String): JsxGraphScene {
        val result = JsxGraphJessieCode.parse(source)
        return assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value
    }

    private fun movePointer(
        session: JsxGraphJessieCodeSession,
        x: Double,
        y: Double,
        timestampMilliseconds: Long,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.interactPointer(
                JsxGraphBoardPointerInteraction.Move(
                    position = JsxGraphPoint2D(x, y),
                    viewportSize = JsxGraphViewportSize(500.0, 500.0),
                    timestampMilliseconds = timestampMilliseconds,
                ),
            ),
        ).value

    private fun chartSector(
        scene: JsxGraphScene,
        name: String,
    ): JsxGraphSceneElement.Curve =
        scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single { it.name == name }

    private fun chartLabel(
        scene: JsxGraphScene,
        content: String,
    ): JsxGraphSceneElement.Text =
        scene.elements
            .filterIsInstance<JsxGraphSceneElement.Text>()
            .single { it.content == content }

    private fun startRadius(
        scene: JsxGraphScene,
        sectorName: String,
    ): Double {
        val point = assertIs<JsxGraphPoint2D>(
            chartSector(scene, sectorName).points[3],
        )
        return sqrt(point.x * point.x + point.y * point.y)
    }

    private fun sectorInteriorScreenPosition(
        scene: JsxGraphScene,
        sectorName: String,
    ): JsxGraphPoint2D {
        val arcMidpoint = assertIs<JsxGraphPoint2D>(
            chartSector(scene, sectorName).points[9],
        )
        val userX = arcMidpoint.x * 0.5
        val userY = arcMidpoint.y * 0.5
        return JsxGraphPoint2D(
            x = (
                (userX - scene.boundingBox.left) /
                    (scene.boundingBox.right - scene.boundingBox.left)
                ) * 500.0,
            y = (
                (scene.boundingBox.top - userY) /
                    (scene.boundingBox.top - scene.boundingBox.bottom)
                ) * 500.0,
        )
    }
}
