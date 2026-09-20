/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsxGraphLayerTest {
    @Test
    fun defaultLayersMatchUpstreamElementTypes() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    objectSource("P1", "point", "[-3,0]"),
                    objectSource("P2", "point", "[0,2]"),
                    objectSource("P3", "point", "[3,0]"),
                    objectSource("line", "line", """["P1","P2"]"""),
                    objectSource("circle", "circle", """["P1",2]"""),
                    objectSource("arc", "arc", """["P1","P2","P3"]"""),
                    objectSource("sector", "sector", """["P1","P2","P3"]"""),
                    objectSource("angle", "angle", """["P1","P2","P3"]"""),
                    objectSource(
                        "curve",
                        "curve",
                        """[[-2,0,2],[0,1,0]]""",
                    ),
                    objectSource(
                        "polygon",
                        "polygon",
                        """["P1","P2","P3"]""",
                    ),
                    objectSource("text", "text", """[0,0,"layer"]"""),
                ),
            ),
        ).value

        assertEquals(
            mapOf(
                "P1" to 9,
                "P2" to 9,
                "P3" to 9,
                "line" to 7,
                "circle" to 6,
                "arc" to 8,
                "sector" to 3,
                "angle" to 3,
                "curve" to 5,
                "polygon" to 3,
                "text" to 9,
            ),
            scene.elements.associate { element ->
                element.id to element.style.layer
            },
        )
    }

    @Test
    fun explicitLayerOverridesTheElementDefault() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                document(
                    objectSource(
                        id = "point",
                        type = "point",
                        parents = "[0,0]",
                        extraAttributes = ""","layer":4""",
                    ),
                ),
            ),
        ).value

        assertEquals(4, scene.elements.single().style.layer)
    }

    @Test
    fun invalidLayerReturnsStructuredAttributeError() {
        for (layer in listOf("-1", "2.5")) {
            val result = assertIs<GMResult.Err<JsxGraphDocumentError>>(
                JsxGraphEngine.parse(
                    document(
                        objectSource(
                            id = "point",
                            type = "point",
                            parents = "[0,0]",
                            extraAttributes = ""","layer":$layer""",
                        ),
                    ),
                ),
            )
            val error =
                assertIs<JsxGraphDocumentError.InvalidAttribute>(result.error)
            assertEquals("layer", error.attribute)
        }
    }

    private fun document(
        vararg objects: String,
    ): String =
        """
        {
          "boundingBox": [-5,5,5,-5],
          "objects": [${objects.joinToString(",")}]
        }
        """.trimIndent()

    private fun objectSource(
        id: String,
        type: String,
        parents: String,
        extraAttributes: String = "",
    ): String =
        """
        {
          "id":"$id",
          "type":"$type",
          "parents":$parents,
          "attributes":{
            "name":"",
            "withLabel":false
            $extraAttributes
          }
        }
        """.trimIndent()
}
