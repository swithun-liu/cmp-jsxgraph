package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphScene
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParitySourceTest {
    @Test
    fun parityCorpusHasUniqueResolvableCases() {
        val cases = JsxGraphParityCorpus.cases
        assertEquals(cases.size, cases.map { parityCase -> parityCase.id }.distinct().size)
        assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find(JsxGraphParityCorpus.DEFAULT_CASE_ID),
        )
        assertIs<GMResult.Err<String>>(
            JsxGraphParityCorpus.find("missing_case"),
        )
    }

    @Test
    fun defaultSourceProducesTheNativeScene() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            parseParitySource(DEFAULT_PARITY_SOURCE),
        ).value
        assertEquals(4, scene.elements.size)
        val fixedPoint = assertIs<JsxGraphSceneElement.Point>(
            scene.elements[0],
        )
        assertEquals(-4.0, fixedPoint.coordinates.x)
        assertEquals(-2.0, fixedPoint.coordinates.y)
        val controlPoint = assertIs<JsxGraphSceneElement.Point>(
            scene.elements[1],
        )
        assertEquals(3.2, controlPoint.coordinates.x)
        assertEquals(2.1, controlPoint.coordinates.y)
        val circle = assertIs<JsxGraphSceneElement.Circle>(
            scene.elements[3],
        )
        assertEquals(2.35, circle.radius)
    }

    @Test
    fun everyCorpusSourceProducesANativeScene() {
        for (parityCase in JsxGraphParityCorpus.cases) {
            assertIs<GMResult.Ok<JsxGraphScene>>(
                parseParitySource(parityCase.source),
                parityCase.id,
            )
        }
    }

    @Test
    fun textParitySourceEvaluatesDynamicContent() {
        val textCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("text"),
        ).value
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            parseParitySource(textCase.source),
        ).value
        val texts = scene.elements.filterIsInstance<JsxGraphSceneElement.Text>()

        assertEquals(5, texts.size)
        assertEquals("A.x = 2.0", texts.last().content)
    }

    @Test
    fun malformedAndUnsupportedSourcesReturnExplicitErrors() {
        assertIs<GMResult.Err<String>>(parseParitySource("not-json"))
        assertIs<GMResult.Err<String>>(
            parseParitySource(
                """
                {
                  "schemaVersion": 2,
                  "boundingBox": [-6, 5, 6, -5],
                  "objects": []
                }
                """.trimIndent(),
            ),
        )
        assertIs<GMResult.Err<String>>(
            parseParitySource(
                DEFAULT_PARITY_SOURCE.replace(
                    "\"parents\": [[0.5, 0.6], 2.35]",
                    "\"parents\": [[0.5, 0.6], \"missing\"]",
                ),
            ),
        )
        assertIs<GMResult.Err<String>>(
            parseParitySource(
                DEFAULT_PARITY_SOURCE.replace(
                    "\"strokeWidth\": 2.5",
                    "\"strokeWidth\": -1",
                ),
            ),
        )
    }
}
