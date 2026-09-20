/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.generated.ProductionCorpusCase
import com.swithun.jsxgraph.core.generated.productionCorpusCases
import com.swithun.jsxgraph.core.generated.requiredProductionFeatures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ProductionCorpusTest {
    @Test
    fun rendersEveryIndependentProductionCaseWithFiniteGeometry() {
        assertEquals(EXPECTED_CASE_COUNT, productionCorpusCases.size)
        assertEquals(
            EXPECTED_FEATURE_COUNT,
            requiredProductionFeatures.size,
        )
        assertEquals(
            productionCorpusCases.size,
            productionCorpusCases.map(ProductionCorpusCase::id).toSet().size,
        )
        assertEquals(
            productionCorpusCases.size,
            productionCorpusCases.map(ProductionCorpusCase::source).toSet().size,
        )
        assertEquals(
            requiredProductionFeatures,
            productionCorpusCases.flatMap(ProductionCorpusCase::features).toSet(),
        )

        for (case in productionCorpusCases) {
            val scene = render(case)
            assertFiniteScene(case.id, scene)
            assertEquals(
                case.expectedElementIds,
                scene.elements.map(JsxGraphSceneElement::id).toSet(),
                "${case.id} emitted unexpected source elements",
            )
            val renderedTexts = scene.elements
                .filterIsInstance<JsxGraphSceneElement.Text>()
                .map(JsxGraphSceneElement.Text::content)
            for (expectedText in case.expectedTexts) {
                assertTrue(
                    renderedTexts.any { content -> expectedText in content },
                    "${case.id} did not render '$expectedText': $renderedTexts",
                )
            }
        }
    }

    @Test
    fun rendersIndependentProductionCorpusDeterministically() {
        for (case in productionCorpusCases) {
            assertEquals(
                expected = render(case),
                actual = render(case),
                message = "${case.id} produced a non-deterministic scene",
            )
        }
    }

    @Test
    fun replaysProductionInteractionsDeterministically() {
        val interactionCases = productionCorpusCases.filter { case ->
            case.interactionPointId != null
        }
        assertEquals(8, interactionCases.size)

        for (case in interactionCases) {
            val initialSession = createSession(case)
            val initialScene = initialSession.scene
            val pointId = assertIs<String>(case.interactionPointId)
            val target = JsxGraphPoint2D(
                x = assertIs<Double>(case.interactionTargetX),
                y = assertIs<Double>(case.interactionTargetY),
            )
            val movedScene = assertIs<GMResult.Ok<JsxGraphScene>>(
                initialSession.movePoint(pointId, target),
                "${case.id} failed to move $pointId",
            ).value
            assertNotEquals(initialScene, movedScene)
            assertEquals(
                target,
                initialSession.captureInteractionState()
                    .pointCoordinates[pointId],
            )

            val restoredSession = createSession(case)
            val restoredScene = assertIs<GMResult.Ok<JsxGraphScene>>(
                restoredSession.restoreInteractionState(
                    initialSession.captureInteractionState(),
                ),
                "${case.id} failed to restore interaction state",
            ).value
            assertEquals(movedScene, restoredScene)
            assertEquals(
                initialScene,
                assertIs<GMResult.Ok<JsxGraphScene>>(
                    initialSession.resetInteractionState(),
                ).value,
            )
        }
    }

    private fun render(case: ProductionCorpusCase): JsxGraphScene {
        val result = JsxGraphEngine.parse(case.source)
        return assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            "${case.id} failed with " +
                (result as? GMResult.Err)?.error,
        ).value
    }

    private fun createSession(case: ProductionCorpusCase): JsxGraphSession =
        assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(case.source),
            "${case.id} failed to create a production session",
        ).value

    private fun assertFiniteScene(
        caseId: String,
        scene: JsxGraphScene,
    ) {
        val bounds = scene.boundingBox
        assertTrue(
            bounds.left.isFinite() &&
                bounds.top.isFinite() &&
                bounds.right.isFinite() &&
                bounds.bottom.isFinite() &&
                bounds.left < bounds.right &&
                bounds.bottom < bounds.top,
            "$caseId has invalid board bounds $bounds",
        )
        assertTrue(
            scene.elements.isNotEmpty(),
            "$caseId produced an empty scene",
        )
        for (element in scene.elements) {
            assertFiniteStyle(caseId, element)
            when (element) {
                is JsxGraphSceneElement.Point ->
                    assertFinitePoint(caseId, element.id, element.coordinates)
                is JsxGraphSceneElement.Line -> {
                    assertFinitePoint(caseId, element.id, element.point1)
                    assertFinitePoint(caseId, element.id, element.point2)
                    assertNotEquals(
                        element.point1,
                        element.point2,
                        "$caseId/${element.id} collapsed to one point",
                    )
                }
                is JsxGraphSceneElement.Circle -> {
                    assertFinitePoint(caseId, element.id, element.center)
                    assertTrue(
                        element.radius.isFinite() && element.radius >= 0.0,
                        "$caseId/${element.id} has invalid radius ${element.radius}",
                    )
                }
                is JsxGraphSceneElement.Curve -> {
                    assertTrue(
                        element.bezierDegree == 1 || element.bezierDegree == 3,
                        "$caseId/${element.id} has invalid degree " +
                            element.bezierDegree,
                    )
                    assertTrue(
                        element.points.any { point -> point != null },
                        "$caseId/${element.id} has no finite path points",
                    )
                    element.points.filterNotNull().forEach { point ->
                        assertFinitePoint(caseId, element.id, point)
                    }
                }
                is JsxGraphSceneElement.Polygon -> {
                    assertTrue(
                        element.vertices.size >= 3,
                        "$caseId/${element.id} has fewer than three vertices",
                    )
                    element.vertices.forEach { point ->
                        assertFinitePoint(caseId, element.id, point)
                    }
                    element.implicitVertices.forEach { point ->
                        assertFinitePoint(caseId, point.id, point.coordinates)
                    }
                }
                is JsxGraphSceneElement.Text -> {
                    assertFinitePoint(caseId, element.id, element.coordinates)
                    assertTrue(
                        element.fontSize.isFinite() && element.fontSize >= 0.0,
                        "$caseId/${element.id} has invalid font size",
                    )
                    assertTrue(
                        element.content.isNotEmpty(),
                        "$caseId/${element.id} has empty text",
                    )
                }
            }
        }
    }

    private fun assertFiniteStyle(
        caseId: String,
        element: JsxGraphSceneElement,
    ) {
        val style = element.style
        assertTrue(
            style.strokeWidth.isFinite() && style.strokeWidth >= 0.0,
            "$caseId/${element.id} has invalid stroke width",
        )
        assertTrue(
            style.strokeOpacity.isFinite() &&
                style.strokeOpacity in 0.0..1.0 &&
                style.fillOpacity.isFinite() &&
                style.fillOpacity in 0.0..1.0,
            "$caseId/${element.id} has invalid opacity",
        )
    }

    private fun assertFinitePoint(
        caseId: String,
        elementId: String,
        point: JsxGraphPoint2D,
    ) {
        assertTrue(
            point.x.isFinite() && point.y.isFinite(),
            "$caseId/$elementId has invalid point $point",
        )
    }

    private companion object {
        const val EXPECTED_CASE_COUNT = 30
        const val EXPECTED_FEATURE_COUNT = 55
    }
}
