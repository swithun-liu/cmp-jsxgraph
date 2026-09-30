/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphGroupTest {
    @Test
    fun groupIsNonRenderedAndMovesExistingScenePoints() {
        val result = JsxGraphJessieCode.parse(
            """
            A = point(0, 0) << id: "A", name: "", withLabel: false >>;
            B = point(2, 0) << id: "B", name: "", withLabel: false >>;
            g = group(A, B) << id: "group", name: "" >>;
            A.moveTo([1, 1]);
            """.trimIndent(),
        )
        assertTrue(result is GMResult.Ok, result.toString())
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(result).value

        assertEquals(2, scene.elements.size)
        val points = scene.elements
            .map { assertIs<JsxGraphSceneElement.Point>(it) }
            .associateBy(JsxGraphSceneElement.Point::id)
        assertEquals(
            JsxGraphPoint2D(1.0, 1.0),
            points.getValue("A").coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(3.0, 1.0),
            points.getValue("B").coordinates,
        )
    }

    @Test
    fun groupDoesNotConsumeSceneObjectBudget() {
        val result = JsxGraphJessieCode.parse(
            source =
                """
                A = point(0, 0) << id: "A", name: "", withLabel: false >>;
                group(A) << id: "group", name: "" >>;
                """.trimIndent(),
            limits = JsxGraphJessieCodeLimits(maxObjects = 1),
        )

        assertTrue(result is GMResult.Ok, result.toString())
        assertEquals(
            1,
            assertIs<GMResult.Ok<JsxGraphScene>>(result)
                .value.elements.size,
        )
    }

    @Test
    fun constructionDocumentAcceptsGroupAsNonSceneObject() {
        val result = JsxGraphEngine.parse(
            """
            {
              "schemaVersion": 1,
              "boundingBox": [-5, 5, 5, -5],
              "axis": false,
              "grid": false,
              "keepAspectRatio": true,
              "objects": [
                {
                  "id": "A",
                  "type": "point",
                  "parents": [0, 0],
                  "attributes": {"name": "", "withLabel": false}
                },
                {
                  "id": "group",
                  "type": "group",
                  "parents": ["A"],
                  "attributes": {"name": ""}
                }
              ]
            }
            """.trimIndent(),
        )

        assertTrue(result is GMResult.Ok, result.toString())
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(result).value
        assertEquals(listOf("A"), scene.elements.map { it.id })
    }
}
