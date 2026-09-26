/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphGridTest {
    @Test
    fun documentAndJessieCodeExposeMajorAndMinorGridCurves() {
        val document = scene(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "grid",
                      "type": "grid",
                      "parents": [],
                      "attributes": {"theme": 5}
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )
        val jessieCode = jessieScene(
            JsxGraphJessieCode.parse(
                """grid() << id: "grid", theme: 5 >>;""",
            ),
        )

        assertEquals(document, jessieCode)
        assertEquals(listOf("grid", "grid_minor"), document.elements.map { it.id })
        val major = curve(document, "grid")
        val minor = curve(document, "grid_minor")
        assertEquals(JsxGraphGridRole.Major, major.grid?.role)
        assertEquals(JsxGraphGridRole.Minor, minor.grid?.role)
        assertEquals(1, major.style.layer)
        assertEquals(JsxGraphColor(192, 192, 192), major.style.strokeColor)
        assertEquals(1.0, major.style.strokeOpacity)
        assertEquals(0.5, minor.style.strokeOpacity)

        val majorResolved = resolve(major)
        val minorResolved = resolve(minor)
        assertEquals(486, majorResolved.points.size)
        assertEquals(4320, minorResolved.points.size)
        assertEquals(1, majorResolved.bezierDegree)
        assertEquals(null, majorResolved.pointStrokeWidth)
        assertEquals(3.0, minorResolved.pointStrokeWidth)
    }

    @Test
    fun unitsAndForceSquareMatchOfficialFixture() {
        val scene = jessieScene(
            JsxGraphJessieCode.parse(
                """
                grid() <<
                    id: "grid",
                    majorStep: ["0.2fr", "100px"],
                    forceSquare: "max",
                    major: <<
                        face: "plus",
                        size: "80%"
                    >>,
                    minorElements: 0
                >>;
                """.trimIndent(),
            ),
        )
        val major = curve(scene, "grid")
        val resolved = resolve(major)

        assertEquals(150, resolved.points.size)
        assertEquals(1, resolved.bezierDegree)
        assertEquals("round", resolved.lineCap)
    }

    @Test
    fun nestedMajorAndMinorIdentityMatchesOfficialFixture() {
        val scene = jessieScene(
            JsxGraphJessieCode.parse(
                """
                grid() <<
                    id: "outerId",
                    name: "outerName",
                    major: << id: "majorId", name: "majorName" >>,
                    minor: << id: "minorId", name: "minorName" >>
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            listOf("majorId", "minorId"),
            scene.elements.map(JsxGraphSceneElement::id),
        )
        assertEquals(
            listOf("majorName", "minorName"),
            scene.elements.map(JsxGraphSceneElement::name),
        )
    }

    @Test
    fun parentAxesDriveAutoStepAndMinorElementCounts() {
        val scene = jessieScene(
            JsxGraphJessieCode.parse(
                """
                xAxis = axis([0, 0], [1, 0]) <<
                    id: "xAxis",
                    ticks: <<
                        id: "xTicks",
                        ticksDistance: 2,
                        minorTicks: 1,
                        insertTicks: false
                    >>
                >>;
                yAxis = axis([0, 0], [0, 1]) <<
                    id: "yAxis",
                    ticks: <<
                        id: "yTicks",
                        ticksDistance: 2.5,
                        minorTicks: 3,
                        insertTicks: false
                    >>
                >>;
                grid(xAxis, yAxis) <<
                    id: "grid",
                    majorStep: "auto",
                    minorElements: "auto"
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(6, scene.elements.size)
        assertEquals(24, resolve(curve(scene, "grid")).points.size)
        assertEquals(144, resolve(curve(scene, "grid_minor")).points.size)
    }

    @Test
    fun themesMatchOfficialMajorAndMinorPointCounts() {
        val expected = listOf(
            0 to (54 to 0),
            1 to (54 to 0),
            2 to (54 to 2700),
            3 to (54 to 180),
            4 to (54 to 5400),
            5 to (486 to 4320),
            6 to (1134 to 4320),
        )

        for ((theme, counts) in expected) {
            val scene = jessieScene(
                JsxGraphJessieCode.parse(
                    """grid() << id: "grid", theme: $theme >>;""",
                ),
            )
            assertEquals(
                counts.first,
                resolve(curve(scene, "grid")).points.size,
                "theme $theme major",
            )
            assertEquals(
                counts.second,
                resolve(curve(scene, "grid_minor")).points.size,
                "theme $theme minor",
            )
        }
    }

    @Test
    fun themeAttributesOverrideConflictingUserGridAttributes() {
        val scene = jessieScene(
            JsxGraphJessieCode.parse(
                """
                grid() <<
                    id: "grid",
                    theme: 6,
                    major: <<
                        face: "plus",
                        size: 20,
                        fillColor: "#F0E442"
                    >>,
                    minor: << face: "plus", size: "58%" >>
                >>;
                """.trimIndent(),
            ),
        )

        val major = requireNotNull(curve(scene, "grid").grid)
        val minor = requireNotNull(curve(scene, "grid_minor").grid)
        assertEquals("circle", major.major.face)
        assertEquals(
            JsxGraphGridLength.Pixels(8.0),
            major.major.size.x,
        )
        assertEquals("point", minor.minor.face)
        assertEquals(
            JsxGraphGridLength.Pixels(3.0),
            minor.minor.size.x,
        )
    }

    @Test
    fun boundaryAndDrawZeroCasesMatchOfficialPointCounts() {
        val excluded = jessieScene(
            JsxGraphJessieCode.parse(
                """
                grid() <<
                    id: "grid",
                    majorStep: 1,
                    includeBoundaries: false,
                    major: << face: "plus", size: 10, drawZero: false >>,
                    minorElements: 1,
                    minor: << face: "point", drawZero: false >>
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(384, resolve(curve(excluded, "grid")).points.size)
        assertEquals(780, resolve(curve(excluded, "grid_minor")).points.size)

        val included = jessieScene(
            JsxGraphJessieCode.parse(
                """
                grid() <<
                    id: "grid",
                    majorStep: 1,
                    includeBoundaries: true,
                    major: <<
                        face: "square",
                        size: 10,
                        drawZero: << origin: false, x: true, y: false >>
                    >>,
                    minorElements: 1,
                    minor: <<
                        face: "circle",
                        size: 4,
                        drawZero: << x: false, y: true >>
                    >>
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(660, resolve(curve(included, "grid")).points.size)
        assertEquals(4480, resolve(curve(included, "grid_minor")).points.size)
    }

    @Test
    fun sessionRetainsGridDefinitionsAcrossExecutions() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = jessieScene(
            session.execute(
                """
                grid() <<
                    id: "grid",
                    majorStep: ["50px", "25%"],
                    minorElements: [1, 2]
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(36, resolve(curve(initial, "grid")).points.size)
        assertEquals(240, resolve(curve(initial, "grid_minor")).points.size)

        val refreshed = jessieScene(session.execute("1 + 1;"))
        assertEquals(
            resolve(curve(initial, "grid")),
            resolve(curve(refreshed, "grid")),
        )
    }

    @Test
    fun gridExpansionHonorsObjectLimitsAndRejectsMalformedAttributes() {
        val documentError = assertIs<
            GMResult.Err<JsxGraphDocumentError.ObjectLimitExceeded>,
            >(
            JsxGraphEngine.parse(
                source =
                    """
                    {
                      "boundingBox": [-5, 5, 5, -5],
                      "objects": [
                        {
                          "id": "grid",
                          "type": "grid",
                          "parents": []
                        }
                      ]
                    }
                    """.trimIndent(),
                limits = JsxGraphEngineLimits(maxObjects = 1),
            ),
        ).error
        assertEquals(2, documentError.actual)

        val jessieError = assertIs<
            GMResult.Err<JsxGraphJessieCodeError.ResourceLimitExceeded>,
            >(
            JsxGraphJessieCode.parse(
                """grid() << id: "grid" >>;""",
                limits = JsxGraphJessieCodeLimits(maxObjects = 1),
            ),
        ).error
        assertEquals(2, jessieError.requestedSize)

        val invalid = assertIs<GMResult.Err<JsxGraphJessieCodeError.Runtime>>(
            JsxGraphJessieCode.parse(
                """grid() << majorStep: 0 >>;""",
            ),
        ).error
        assertTrue("majorstep" in invalid.reason.lowercase())
    }

    @Test
    fun denseGridFailsAsStructuredCurvePointLimit() {
        val source =
            """
            {
              "boundingBox": [-5, 5, 5, -5],
              "objects": [
                {
                  "id": "dense",
                  "type": "grid",
                  "parents": [],
                  "attributes": {
                    "majorStep": 0.002,
                    "major": {"face": "plus"}
                  }
                }
              ]
            }
            """.trimIndent()
        val documentError = assertIs<
            GMResult.Err<JsxGraphDocumentError.CurvePointLimitExceeded>,
            >(
            JsxGraphEngine.parse(
                source = source,
                limits = JsxGraphEngineLimits(maxCurvePoints = 100),
            ),
        ).error
        assertEquals(100, documentError.limit)
        assertTrue(documentError.actual > documentError.limit)

        val jessieError = assertIs<
            GMResult.Err<JsxGraphJessieCodeError.ResourceLimitExceeded>,
            >(
            JsxGraphJessieCode.parse(
                """
                grid() <<
                    id: "dense",
                    majorStep: 0.002,
                    major: << face: "plus" >>
                >>;
                """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxCurvePoints = 100),
            ),
        ).error
        assertEquals("curve point count", jessieError.resource)
        assertEquals(100, jessieError.limit)
        assertTrue(jessieError.requestedSize > jessieError.limit)
    }

    private fun resolve(curve: JsxGraphSceneElement.Curve): JsxGraphResolvedGrid =
        assertIs<GMResult.Ok<JsxGraphResolvedGrid>>(
            requireNotNull(curve.grid).resolve(
                visibleLeft = -5.0,
                visibleTop = 5.0,
                visibleRight = 5.0,
                visibleBottom = -5.0,
                cssPixelsPerUnitX = 50.0,
                cssPixelsPerUnitY = 50.0,
            ),
        ).value

    private fun curve(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Curve =
        assertIs(scene.elements.single { it.id == id })

    private fun scene(
        result: GMResult<JsxGraphScene, JsxGraphDocumentError>,
    ): JsxGraphScene = assertIs<GMResult.Ok<JsxGraphScene>>(result).value

    private fun jessieScene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene = assertIs<GMResult.Ok<JsxGraphScene>>(result).value
}
