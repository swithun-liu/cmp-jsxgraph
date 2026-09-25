package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphEngine3DTest {
    @Test
    fun documentView3DAutomaticallyExpandsBorderAxes() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 8, 8, -8],
                  "objects": [
                    {
                      "id": "view",
                      "type": "view3d",
                      "parents": [
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                      ],
                      "attributes": {
                        "name": "",
                        "projection": "parallel",
                        "axesPosition": "border",
                        "xAxisBorder": {
                          "ticks3d": {"drawLabels": false}
                        },
                        "yAxisBorder": {
                          "ticks3d": {"drawLabels": false}
                        },
                        "zAxisBorder": {
                          "ticks3d": {"drawLabels": false}
                        },
                        "xPlaneRear": {"visible": false},
                        "yPlaneRear": {"visible": false},
                        "zPlaneRear": {"visible": false}
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        assertEquals(27, scene.elements.size)
        assertEquals(
            3,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Curve>()
                .count { it.ticks3D != null },
        )
        assertEquals(
            15,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Line>()
                .size,
        )
        assertEquals(
            3,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Curve>()
                .count { it.style.strokeColor == JsxGraphColor(154, 154, 154) },
        )
    }

    @Test
    fun documentCreatesProjectedText3D() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 8, 8, -8],
                  "objects": [
                    {
                      "id": "view",
                      "type": "view3d",
                      "parents": [
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                      ],
                      "attributes": {
                        "name": "",
                        "projection": "parallel",
                        "az": {"slider": {"start": 1}},
                        "el": {"slider": {"start": 0.3}},
                        "bank": {"slider": {"start": 0}},
                        $HIDDEN_DEFAULT_AXES_JSON
                      }
                    },
                    {
                      "id": "label",
                      "type": "text3d",
                      "parents": ["view", [-2, 1.519615242270663, -1], "-2"],
                      "attributes": {
                        "name": "",
                        "anchorX": "middle",
                        "anchorY": "middle"
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val label = assertIs<JsxGraphSceneElement.Text>(
            scene.elements.single { it.id == "label" },
        )
        assertEquals("label", label.id)
        assertEquals("-2", label.content)
        assertPoint(
            expectedX = 0.22641937518774236,
            expectedY = -2.233304675143941,
            actual = label.coordinates,
        )
    }

    @Test
    fun ticks3DLabelsUseViewportResolvedOfficialPositions() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 8, 8, -8],
                  "objects": [
                    {
                      "id": "view",
                      "type": "view3d",
                      "parents": [
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                      ],
                      "attributes": {
                        "name": "",
                        "projection": "parallel",
                        "az": {"slider": {"start": 1}},
                        "el": {"slider": {"start": 0.3}},
                        "bank": {"slider": {"start": 0}},
                        $HIDDEN_DEFAULT_AXES_JSON
                      }
                    },
                    {
                      "id": "ticks",
                      "type": "ticks3d",
                      "parents": [
                        "view",
                        [-2, 1, -1],
                        [2, 0, 0],
                        4,
                        [0, 1, 0]
                      ],
                      "attributes": {
                        "name": "",
                        "ticksDistance": 2,
                        "majorHeight": 12,
                        "tickEndings": [0.25, 0.75],
                        "drawLabels": true,
                        "label": {
                          "anchorX": "middle",
                          "anchorY": "middle"
                        }
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val ticks = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements.single { it.id == "ticks" },
        )
        val resolvedTicks = ticks.resolvePoints(40.0, 30.0)
        assertPoint(
            expectedX = -0.18575328186789075,
            expectedY = -2.1648706494826797,
            actual = requireNotNull(resolvedTicks[0]),
        )
        val labels =
            scene.elements.filterIsInstance<JsxGraphSceneElement.Text>()
        assertEquals(listOf("-2", "0", "2"), labels.map { it.content })
        val resolvedLabel = requireNotNull(
            labels[0].ticks3DLabel?.resolvePosition(40.0, 30.0),
        )
        assertPoint(
            expectedX = 0.22641937518774236,
            expectedY = -2.233304675143941,
            actual = resolvedLabel,
        )
    }

    @Test
    fun documentCreatesTicks3DBrokenCurve() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 8, 8, -8],
                  "objects": [
                    {
                      "id": "view",
                      "type": "view3d",
                      "parents": [
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                      ],
                      "attributes": {
                        "name": "",
                        "projection": "parallel",
                        "az": {"slider": {"start": 1}},
                        "el": {"slider": {"start": 0.3}},
                        "bank": {"slider": {"start": 0}},
                        $HIDDEN_DEFAULT_AXES_JSON
                      }
                    },
                    {
                      "id": "ticks",
                      "type": "ticks3d",
                      "parents": [
                        "view",
                        [-2, 1, -1],
                        [2, 0, 0],
                        4,
                        [0, 1, 0]
                      ],
                      "attributes": {
                        "name": "",
                        "ticksDistance": 2,
                        "majorHeight": 12,
                        "tickEndings": [0.25, 0.75],
                        "drawLabels": false
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val ticks = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements.single { it.id == "ticks" },
        )
        assertEquals("ticks", ticks.id)
        assertEquals(9, ticks.points.size)
        assertEquals(null, ticks.points[2])
        assertEquals(null, ticks.points[5])
        assertEquals(null, ticks.points[8])
        val resolved = ticks.resolvePoints(
            cssPixelsPerUnitX = 40.0,
            cssPixelsPerUnitY = 30.0,
        )
        assertPoint(
            expectedX = -0.18575328186789075,
            expectedY = -2.1648706494826797,
            actual = requireNotNull(resolved[0]),
        )
        assertPoint(
            expectedX = 0.04977395073532809,
            expectedY = -2.203975807003401,
            actual = requireNotNull(resolved[1]),
        )
    }

    @Test
    fun documentCreatesProjectedPlane3DWireframe() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                finitePlane3DSource(
                    mesh3D = """{"visible": false}""",
                ),
            ),
        ).value

        val plane = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements.single { it.id == "plane" },
        )
        assertEquals("plane", plane.id)
        assertEquals(5, plane.points.size)
        assertPoint(
            expectedX = -0.36429513147350645,
            expectedY = -0.8348490315008023,
            actual = requireNotNull(plane.points[0]),
        )
        assertPoint(
            expectedX = -1.10481259616156,
            expectedY = -0.5205842898903073,
            actual = requireNotNull(plane.points[2]),
        )
        assertEquals(JsxGraphColor(187, 187, 187), plane.style.fillColor)
        assertEquals(0.3, plane.style.fillOpacity)
        val mesh = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single { it.points.size == 58 }
        assertFalse(mesh.style.visible)
        assertEquals(JsxGraphColor(154, 154, 154), mesh.style.strokeColor)
        assertEquals(0.6, mesh.style.strokeOpacity)
        assertEquals(12, mesh.style.layer)
    }

    @Test
    fun finitePlane3DWireframeMeshInheritsPlaneVisibility() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(finitePlane3DSource(mesh3D = null)),
        ).value

        val mesh = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single { it.points.size == 58 }
        assertTrue(mesh.style.visible)
    }

    @Test
    fun documentCreatesDirectMesh3DAndEnforcesItsPointLimit() {
        val source =
            """
            {
              "boundingBox": [-8, 8, 8, -8],
              "objects": [
                {
                  "id": "view",
                  "type": "view3d",
                  "parents": [
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                  ],
                  "attributes": {
                    "name": "",
                    "projection": "parallel",
                    $HIDDEN_DEFAULT_AXES_JSON
                  }
                },
                {
                  "id": "mesh",
                  "type": "mesh3d",
                  "parents": [
                    "view",
                    [1, 1, 2, 2],
                    [0, 1, 0, 0],
                    [0, 0, 1, 1],
                    [-2, 3],
                    [-1, 2]
                  ],
                  "attributes": {
                    "name": "",
                    "stepWidthU": 2
                  }
                }
              ]
            }
            """.trimIndent()
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(source),
        ).value
        val mesh = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements.single { it.id == "mesh" },
        )
        assertEquals(31, mesh.points.size)
        assertEquals(JsxGraphColor(154, 154, 154), mesh.style.strokeColor)
        assertEquals(0.6, mesh.style.strokeOpacity)
        assertEquals(12, mesh.style.layer)

        val error = assertIs<
            GMResult.Err<JsxGraphDocumentError.CurvePointLimitExceeded>
            >(
            JsxGraphEngine.parse(
                source,
                limits = JsxGraphEngineLimits(maxCurvePoints = 30),
            ),
        ).error
        assertEquals("mesh", error.id)
        assertEquals(31, error.actual)
    }

    @Test
    fun documentCreatesProjectedLine3DWithOfficialDefaults() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 8, 8, -8],
                  "objects": [
                    {
                      "id": "view",
                      "type": "view3d",
                      "parents": [
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                      ],
                      "attributes": {
                        "name": "",
                        "projection": "parallel",
                        "az": {"slider": {"start": 1}},
                        "el": {"slider": {"start": 0.3}},
                        "bank": {"slider": {"start": 0}},
                        $HIDDEN_DEFAULT_AXES_JSON
                      }
                    },
                    {
                      "id": "line",
                      "type": "line3d",
                      "parents": [
                        "view",
                        [1, 2, 2],
                        [2, -1, 3]
                      ],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "straightFirst": true,
                        "straightLast": true
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val line = assertIs<JsxGraphSceneElement.Line>(
            scene.elements.single { it.id == "line" },
        )
        assertEquals("line", line.id)
        assertPoint(
            expectedX = 2.5450641996710544,
            expectedY = -1.9063945685192492,
            actual = line.point1,
        )
        assertPoint(
            expectedX = -5.709235568048273,
            expectedY = 0.8878474942970511,
            actual = line.point2,
        )
        assertEquals(JsxGraphColor(0, 0, 0), line.style.strokeColor)
        assertEquals(1.0, line.style.strokeWidth)
        assertEquals(12, line.style.layer)
    }

    @Test
    fun documentCreatesProjectedPoint3DAndTransformedPoint() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(SOURCE),
        ).value

        val source = assertIs<JsxGraphSceneElement.Point>(
            scene.elements.single { it.id == "source" },
        )
        assertEquals("source", source.id)
        assertPoint(
            expectedX = -0.7566557074166769,
            expectedY = -0.7886977433927291,
            actual = source.coordinates,
        )
        assertEquals(JsxGraphColor(255, 255, 0), source.style.fillColor)
        assertEquals(0.0, source.style.strokeWidth)
        assertEquals(13, source.style.layer)
        assertEquals(4.0, source.size)
        assertTrue(source.draggable)

        val transformed = assertIs<JsxGraphSceneElement.Point>(
            scene.elements.single { it.id == "transformed" },
        )
        assertEquals("transformed", transformed.id)
        assertPoint(
            expectedX = -3.669509900873932,
            expectedY = 1.9000326916012957,
            actual = transformed.coordinates,
        )
    }

    @Test
    fun point3DSessionMovesProjectedProxyAndRestoresState() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(SOURCE),
        ).value
        val initial = session.captureInteractionState()
        assertEquals(setOf("source"), initial.pointCoordinates.keys)

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "source",
                coordinates = JsxGraphPoint2D(0.0, 0.0),
            ),
        ).value
        assertPoint(
            expectedX = 0.0,
            expectedY = 0.0,
            actual = assertIs<JsxGraphSceneElement.Point>(
                moved.elements.single { it.id == "source" },
            ).coordinates,
        )

        val restored = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.restoreInteractionState(initial),
        ).value
        assertPoint(
            expectedX = -0.7566557074166769,
            expectedY = -0.7886977433927291,
            actual = assertIs<JsxGraphSceneElement.Point>(
                restored.elements.single { it.id == "source" },
            ).coordinates,
        )
    }

    @Test
    fun defaultCenterAxesReturnExplicitDocumentGap() {
        val result = JsxGraphEngine.parse(
            """
            {
              "boundingBox": [-8, 8, 8, -8],
              "objects": [
                {
                  "id": "view",
                  "type": "view3d",
                  "parents": [
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                  ],
                  "attributes": {"name": "", "projection": "parallel"}
                }
              ]
            }
            """.trimIndent(),
        )

        val error = assertIs<
            GMResult.Err<JsxGraphDocumentError.ElementCreation>
            >(result).error
        assertEquals("view", error.id)
        assertEquals("view3d", error.type)
        assertTrue(error.reason.contains("center-origin"))
    }

    @Test
    fun automaticBorderAxesConsumeTheirFullDocumentObjectBudget() {
        val result = JsxGraphEngine.parse(
            source =
                """
                {
                  "boundingBox": [-8, 8, 8, -8],
                  "objects": [
                    {
                      "id": "view",
                      "type": "view3d",
                      "parents": [
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                      ],
                      "attributes": {
                        "name": "",
                        "projection": "parallel",
                        "axesPosition": "border"
                      }
                    }
                  ]
                }
                """.trimIndent(),
            limits = JsxGraphEngineLimits(maxObjects = 23),
        )

        assertEquals(
            JsxGraphDocumentError.ObjectLimitExceeded(
                limit = 23,
                actual = 60,
            ),
            assertIs<GMResult.Err<JsxGraphDocumentError>>(result).error,
        )
    }

    @Test
    fun ticks3DAndText3DRespectDocumentResourceLimits() {
        val ticksSource =
            """
            {
              "boundingBox": [-8, 8, 8, -8],
              "objects": [
                {
                  "id": "view",
                  "type": "view3d",
                  "parents": [
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                  ],
                  "attributes": {
                    "name": "",
                    "projection": "parallel",
                    $HIDDEN_DEFAULT_AXES_JSON
                  }
                },
                {
                  "id": "ticks",
                  "type": "ticks3d",
                  "parents": [
                    "view",
                    [-2, 1, -1],
                    [2, 0, 0],
                    4,
                    [0, 1, 0]
                  ],
                  "attributes": {
                    "name": "",
                    "ticksDistance": 2,
                    "drawLabels": false
                  }
                }
              ]
            }
            """.trimIndent()
        val ticksError = assertIs<
            GMResult.Err<JsxGraphDocumentError.CurvePointLimitExceeded>
            >(
            JsxGraphEngine.parse(
                ticksSource,
                limits = JsxGraphEngineLimits(maxCurvePoints = 8),
            ),
        ).error
        assertEquals("ticks", ticksError.id)
        assertEquals(9, ticksError.actual)

        val textSource =
            """
            {
              "boundingBox": [-8, 8, 8, -8],
              "objects": [
                {
                  "id": "view",
                  "type": "view3d",
                  "parents": [
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                  ],
                  "attributes": {
                    "name": "",
                    "projection": "parallel",
                    $HIDDEN_DEFAULT_AXES_JSON
                  }
                },
                {
                  "id": "label",
                  "type": "text3d",
                  "parents": ["view", [0, 0, 0], "long"],
                  "attributes": {"name": ""}
                }
              ]
            }
            """.trimIndent()
        val textError = assertIs<
            GMResult.Err<JsxGraphDocumentError.TextLengthLimitExceeded>
            >(
            JsxGraphEngine.parse(
                textSource,
                limits = JsxGraphEngineLimits(maxTextLength = 3),
            ),
        ).error
        assertEquals("label", textError.id)
        assertEquals(4, textError.actual)
    }

    @Test
    fun malformedPoint3DReturnsStructuredDocumentError() {
        val result = JsxGraphEngine.parse(
            """
            {
              "boundingBox": [-8, 8, 8, -8],
              "objects": [
                {
                  "id": "point",
                  "type": "point3d",
                  "parents": [1, 2, 3],
                  "attributes": {"name": "", "withLabel": false}
                }
              ]
            }
            """.trimIndent(),
        )

        val error = assertIs<
            GMResult.Err<JsxGraphDocumentError.ElementCreation>
            >(result).error
        assertEquals("point3d", error.type)
        assertTrue(error.reason.contains("UnsupportedParents"))
    }

    @Test
    fun malformedLine3DReturnsStructuredDocumentError() {
        val result = JsxGraphEngine.parse(
            """
            {
              "boundingBox": [-8, 8, 8, -8],
              "objects": [
                {
                  "id": "view",
                  "type": "view3d",
                  "parents": [
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                  ],
                  "attributes": {
                    "name": "",
                    "projection": "parallel",
                    $HIDDEN_DEFAULT_AXES_JSON
                  }
                },
                {
                  "id": "line",
                  "type": "line3d",
                  "parents": ["view", [0, 0, 0], [1, 2], [-1, 1]],
                  "attributes": {"name": "", "withLabel": false}
                }
              ]
            }
            """.trimIndent(),
        )

        val error = assertIs<
            GMResult.Err<JsxGraphDocumentError.ElementCreation>
            >(result).error
        assertEquals("line3d", error.type)
        assertTrue(error.reason.contains("InvalidAttributeType"))
    }

    private fun assertPoint(
        expectedX: Double,
        expectedY: Double,
        actual: JsxGraphPoint2D,
    ) {
        assertEquals(expectedX, actual.x, TOLERANCE)
        assertEquals(expectedY, actual.y, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1.0e-12

        val HIDDEN_DEFAULT_AXES_JSON =
            """
            "axesPosition": "none",
            "xPlaneRear": {"visible": false},
            "yPlaneRear": {"visible": false},
            "zPlaneRear": {"visible": false}
            """.trimIndent()

        fun finitePlane3DSource(
            mesh3D: String?,
        ): String {
            val mesh3DAttribute =
                mesh3D?.let { ""","mesh3d": $it""" }.orEmpty()
            return """
                {
                  "boundingBox": [-8, 8, 8, -8],
                  "objects": [
                    {
                      "id": "view",
                      "type": "view3d",
                      "parents": [
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                      ],
                      "attributes": {
                        "name": "",
                        "projection": "parallel",
                        "az": {"slider": {"start": 1}},
                        "el": {"slider": {"start": 0.3}},
                        "bank": {"slider": {"start": 0}},
                        $HIDDEN_DEFAULT_AXES_JSON
                      }
                    },
                    {
                      "id": "plane",
                      "type": "plane3d",
                      "parents": [
                        "view",
                        [1, 2, 2],
                        [1, 0, 0],
                        [0, 1, 1],
                        [-2, 3],
                        [-1, 2]
                      ],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "type": "wireframe"
                        $mesh3DAttribute
                      }
                    }
                  ]
                }
                """.trimIndent()
        }

        val SOURCE =
            """
            {
              "boundingBox": [-8, 8, 8, -8],
              "objects": [
                {
                  "id": "view",
                  "type": "view3d",
                  "parents": [
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                  ],
                  "attributes": {
                    "name": "",
                    "projection": "parallel",
                    "az": {"slider": {"start": 1}},
                    "el": {"slider": {"start": 0.3}},
                    "bank": {"slider": {"start": 0}},
                    $HIDDEN_DEFAULT_AXES_JSON
                  }
                },
                {
                  "id": "source",
                  "type": "point3d",
                  "parents": ["view", 1, 2, 2],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "size": 4
                  }
                },
                {
                  "id": "translation",
                  "type": "transform3d",
                  "parents": ["view", 2, -3, 4],
                  "attributes": {"type": "translate"}
                },
                {
                  "id": "transformed",
                  "type": "point3d",
                  "parents": ["view", "source", "translation"],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "fixed": true
                  }
                }
              ]
            }
            """.trimIndent()
    }
}
