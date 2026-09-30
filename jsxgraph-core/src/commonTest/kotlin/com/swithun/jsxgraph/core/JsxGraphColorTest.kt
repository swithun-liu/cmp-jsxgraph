/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsxGraphColorTest {
    @Test
    fun documentColorsUseTranslatedCssParserWithoutRegressingAlpha() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "axis": false,
                  "grid": false,
                  "keepAspectRatio": true,
                  "objects": [
                    {
                      "id": "named",
                      "type": "point",
                      "parents": [-3, 0],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "fillColor": "cornflowerblue"
                      }
                    },
                    {
                      "id": "rgb",
                      "type": "point",
                      "parents": [-1, 0],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "fillColor": "rgb(12, 132, 233)"
                      }
                    },
                    {
                      "id": "rgba",
                      "type": "point",
                      "parents": [1, 0],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "fillColor": "rgba(123, 234, 45, 0.5)"
                      }
                    },
                    {
                      "id": "hexAlpha",
                      "type": "point",
                      "parents": [3, 0],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "fillColor": "#1234"
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val points = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Point>()
            .associateBy(JsxGraphSceneElement.Point::id)
        assertEquals(
            JsxGraphColor(100, 149, 237),
            points.getValue("named").style.fillColor,
        )
        assertEquals(
            JsxGraphColor(12, 132, 233),
            points.getValue("rgb").style.fillColor,
        )
        assertEquals(
            JsxGraphColor(123, 234, 45, 128),
            points.getValue("rgba").style.fillColor,
        )
        assertEquals(
            JsxGraphColor(17, 34, 51, 68),
            points.getValue("hexAlpha").style.fillColor,
        )
    }
}
