/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

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

    private fun scene(source: String): JsxGraphScene {
        val result = JsxGraphJessieCode.parse(source)
        return assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value
    }
}
