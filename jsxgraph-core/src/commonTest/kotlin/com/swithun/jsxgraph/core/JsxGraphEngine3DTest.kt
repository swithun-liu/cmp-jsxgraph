package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphEngine3DTest {
    @Test
    fun documentCreatesProjectedPoint3DAndTransformedPoint() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(SOURCE),
        ).value

        assertEquals(2, scene.elements.size)
        val source = assertIs<JsxGraphSceneElement.Point>(
            scene.elements[0],
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
            scene.elements[1],
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
                moved.elements[0],
            ).coordinates,
        )

        val restored = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.restoreInteractionState(initial),
        ).value
        assertPoint(
            expectedX = -0.7566557074166769,
            expectedY = -0.7886977433927291,
            actual = assertIs<JsxGraphSceneElement.Point>(
                restored.elements[0],
            ).coordinates,
        )
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
                    "bank": {"slider": {"start": 0}}
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
