/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphImageTest {
    @Test
    fun documentAndJessieCodeExposeEquivalentStaticImageGeometry() {
        val document = scene(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "image",
                      "type": "image",
                      "parents": ["asset:checker", [-3, -2], [3, 4]],
                      "attributes": {
                        "name": "",
                        "fillOpacity": 0.4
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )
        val jessieCode = jessieScene(
            JsxGraphJessieCode.parse(
                """
                image("asset:checker", [-3, -2], [3, 4]) <<
                    id: "image",
                    name: "",
                    fillOpacity: 0.4
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(document, jessieCode)
        val image = image(document)
        assertEquals("asset:checker", image.source)
        assertEquals(JsxGraphPoint2D(-3.0, -2.0), image.anchor)
        assertEquals(JsxGraphPoint2D(3.0, 0.0), image.widthVector)
        assertEquals(JsxGraphPoint2D(0.0, 4.0), image.heightVector)
        assertEquals(3.0, image.userWidth)
        assertEquals(4.0, image.userHeight)
        assertEquals(0.4, image.style.fillOpacity)
        assertEquals(0, image.style.layer)
    }

    @Test
    fun dynamicUrlCoordinatesAndSizeUpdateWithTheBoard() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = jessieScene(
            session.execute(
                """
                driver = point(-2, -1) <<
                    id: "driver", name: "", withLabel: false
                >>;
                Picture = image(
                    function () {
                        return driver.X() < 0 ?
                            "asset:first" : "asset:second";
                    },
                    [
                        function () { return driver.X(); },
                        function () { return driver.Y(); }
                    ],
                    [
                        function () { return driver.X() + 4; },
                        function () { return driver.Y() + 3; }
                    ]
                ) << id: "picture", name: "" >>;
                """.trimIndent(),
            ),
        )

        assertImage(
            image = image(initial),
            source = "asset:first",
            anchor = JsxGraphPoint2D(-2.0, -1.0),
            width = 2.0,
            height = 2.0,
        )

        val moved = interactionScene(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(1.0, 2.0),
            ),
        )
        assertImage(
            image = image(moved),
            source = "asset:second",
            anchor = JsxGraphPoint2D(1.0, 2.0),
            width = 5.0,
            height = 5.0,
        )

        val changed = jessieScene(
            session.execute(
                """
                Picture.setSize(4, 1.5);
                sizeProbe = point(Picture.W(), Picture.H()) <<
                    id: "sizeProbe", name: "", visible: false
                >>;
                """.trimIndent(),
            ),
        )
        assertImage(
            image = image(changed),
            source = "asset:second",
            anchor = JsxGraphPoint2D(1.0, 2.0),
            width = 4.0,
            height = 1.5,
        )
        assertEquals(
            JsxGraphPoint2D(4.0, 1.5),
            assertIs<JsxGraphSceneElement.Point>(
                changed.elements.last(),
            ).coordinates,
        )
    }

    @Test
    fun negativeSizeUsesAbsoluteRenderVectorsButPreservesUserSize() {
        val image = image(
            jessieScene(
                JsxGraphJessieCode.parse(
                    """
                    image("asset:checker", [1, 2], [-2, -3]) <<
                        id: "negative", name: ""
                    >>;
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(-2.0, image.userWidth)
        assertEquals(-3.0, image.userHeight)
        assertEquals(JsxGraphPoint2D(2.0, 0.0), image.widthVector)
        assertEquals(JsxGraphPoint2D(0.0, 3.0), image.heightVector)
    }

    @Test
    fun rotateAttributeMatchesOfficialFiveTransformGeometry() {
        val image = image(
            jessieScene(
                JsxGraphJessieCode.parse(
                    """
                    image("asset:checker", [-1, -2], [3, 2]) <<
                        id: "rotated", name: "", rotate: 30
                    >>;
                    """.trimIndent(),
                ),
            ),
        )

        assertPointClose(JsxGraphPoint2D(-1.0, -2.0), image.anchor)
        assertPointClose(
            JsxGraphPoint2D(2.598076211353316, 1.5),
            image.widthVector,
        )
        assertPointClose(
            JsxGraphPoint2D(-1.0, 1.7320508075688772),
            image.heightVector,
        )
    }

    @Test
    fun invalidParentsAndDynamicResultsAreStructuredFailures() {
        assertIs<GMResult.Err<JsxGraphDocumentError>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "bad",
                      "type": "image",
                      "parents": ["asset:checker", [0], [1, 1]]
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )
        val invalidSize = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                """
                image(
                    "asset:checker",
                    [0, 0],
                    [function () { return "wide"; }, 1]
                ) << id: "bad", name: "" >>;
                """.trimIndent(),
            ),
        )
        assertTrue(invalidSize.error.message.contains("SizeResult"))
    }

    @Test
    fun sourceLengthLimitsRejectStaticAndDynamicValues() {
        val documentLimit = assertIs<
            GMResult.Err<
                JsxGraphDocumentError.ImageSourceLengthLimitExceeded,
                >,
            >(
            JsxGraphEngine.parse(
                source =
                    """
                    {
                      "boundingBox": [-5, 5, 5, -5],
                      "objects": [
                        {
                          "id": "image",
                          "type": "image",
                          "parents": ["asset:too-long", [0, 0], [1, 1]]
                        }
                      ]
                    }
                    """.trimIndent(),
                limits = JsxGraphEngineLimits(maxImageSourceLength = 5),
            ),
        ).error
        assertEquals("image", documentLimit.id)
        assertEquals(5, documentLimit.limit)
        assertEquals("asset:too-long".length, documentLimit.actual)

        val jessieLimit = assertIs<
            JsxGraphJessieCodeError.ResourceLimitExceeded,
            >(
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    source =
                        """
                        image("asset:too-long", [0, 0], [1, 1]) <<
                            id: "image", name: ""
                        >>;
                        """.trimIndent(),
                    limits = JsxGraphJessieCodeLimits(
                        maxImageSourceLength = 5,
                    ),
                ),
            ).error,
        )
        assertEquals("image source length", jessieLimit.resource)
        assertEquals("asset:too-long".length.toLong(), jessieLimit.requestedSize)

        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                limits = JsxGraphJessieCodeLimits(
                    maxImageSourceLength = 12,
                ),
            ),
        ).value
        val initial = jessieScene(
            session.execute(
                """
                driver = point(-1, 0) <<
                    id: "driver", name: "", withLabel: false
                >>;
                image(
                    function () {
                        return driver.X() < 0 ?
                            "short" : "this-source-is-too-long";
                    },
                    [0, 0],
                    [1, 1]
                ) << id: "image", name: "" >>;
                """.trimIndent(),
            ),
        )
        assertEquals("short", image(initial).source)

        val moveError = assertIs<
            GMResult.Err<JsxGraphInteractionError.SceneUpdate>,
            >(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(1.0, 0.0),
            ),
        ).error
        val dynamicLimit = assertIs<
            JsxGraphDocumentError.ImageSourceLengthLimitExceeded,
            >(moveError.error)
        assertEquals("image", dynamicLimit.id)
        assertEquals(12, dynamicLimit.limit)
        assertEquals("this-source-is-too-long".length, dynamicLimit.actual)
        assertEquals("short", image(session.scene).source)
    }

    private fun assertImage(
        image: JsxGraphSceneElement.Image,
        source: String,
        anchor: JsxGraphPoint2D,
        width: Double,
        height: Double,
    ) {
        assertEquals(source, image.source)
        assertEquals(anchor, image.anchor)
        assertEquals(width, image.userWidth)
        assertEquals(height, image.userHeight)
        assertEquals(JsxGraphPoint2D(width, 0.0), image.widthVector)
        assertEquals(JsxGraphPoint2D(0.0, height), image.heightVector)
    }

    private fun assertPointClose(
        expected: JsxGraphPoint2D,
        actual: JsxGraphPoint2D,
    ) {
        assertEquals(expected.x, actual.x, 1.0e-12)
        assertEquals(expected.y, actual.y, 1.0e-12)
    }

    private fun image(scene: JsxGraphScene): JsxGraphSceneElement.Image =
        assertIs<JsxGraphSceneElement.Image>(
            scene.elements.first { it is JsxGraphSceneElement.Image },
        )

    private fun scene(
        result: GMResult<JsxGraphScene, JsxGraphDocumentError>,
    ): JsxGraphScene = assertIs<GMResult.Ok<JsxGraphScene>>(result).value

    private fun jessieScene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value

    private fun interactionScene(
        result: GMResult<JsxGraphScene, JsxGraphInteractionError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value
}
