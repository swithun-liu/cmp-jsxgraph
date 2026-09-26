/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GeneratedStressTest {
    @Test
    fun generatedCorpusIsDeterministicAndUnique() {
        val first = generatedSources()
        val second = generatedSources()

        assertEquals(STRESS_CASE_COUNT, first.size)
        assertEquals(first, second)
        assertEquals(first.size, first.toSet().size)
    }

    @Test
    fun parsesGeneratedCorpusWithBoundedFiniteScenes() {
        generatedSources().forEachIndexed { index, source ->
            val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
                JsxGraphEngine.parse(source),
                "Generated stress case $index did not parse",
            ).value
            assertTrue(
                scene.elements.size in 1..MAX_SCENE_ELEMENTS,
                "Generated stress case $index emitted " +
                    "${scene.elements.size} elements",
            )
            assertFiniteScene(index, scene)
        }
    }

    @Test
    fun updatesEveryGeneratedInteractionCase() {
        generatedSources()
            .filterIndexed { index, _ -> index % SHAPE_COUNT == 7 }
            .forEachIndexed { interactionIndex, source ->
                val session = assertIs<GMResult.Ok<JsxGraphSession>>(
                    JsxGraphEngine.createSession(source),
                ).value
                val initial = session.scene
                val target = JsxGraphPoint2D(
                    x = -3.5 + interactionIndex % 8,
                    y = 3.5 - interactionIndex % 7,
                )
                val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
                    session.movePoint("driver", target),
                ).value
                assertNotEquals(initial, moved)
                assertEquals(
                    target,
                    session.captureInteractionState()
                        .pointCoordinates["driver"],
                )
                assertFiniteScene(interactionIndex, moved)
            }
    }

    private fun generatedSources(): List<String> {
        val random = DeterministicRandom(SEED)
        return List(STRESS_CASE_COUNT) { index ->
            val objects = when (index % SHAPE_COUNT) {
                0 -> geometryObjects(index, random)
                1 -> functionGraphObjects(index, random)
                2 -> parametricObjects(index, random)
                3 -> dataPlotObjects(index, random)
                4 -> polygonObjects(index, random)
                5 -> textObjects(index, random)
                6 -> circularObjects(index, random)
                else -> interactionObjects(index, random)
            }
            constructionDocument(
                index = index,
                objects = objects,
                keepAspectRatio = index % 3 != 0,
            )
        }
    }

    private fun geometryObjects(
        index: Int,
        random: DeterministicRandom,
    ): List<JsonObject> {
        val first = randomPoint(random, leftHalf = true)
        val second = randomPoint(random, leftHalf = false)
        val center = randomPoint(random)
        val radius = random.number(0.6, 2.8)
        return listOf(
            pointObject("pointA", first, fixed = true, colorIndex = index),
            pointObject("pointB", second, fixed = true, colorIndex = index + 1),
            sourceObject(
                id = "line",
                type = "line",
                parents = jsonArray("pointA", "pointB"),
                attributes = commonAttributes(
                    colorIndex = index + 2,
                    extras = mapOf(
                        "straightFirst" to JsonPrimitive(index % 2 == 0),
                        "straightLast" to JsonPrimitive(index % 3 == 0),
                    ),
                ),
            ),
            sourceObject(
                id = "circle",
                type = "circle",
                parents = JsonArray(
                    listOf(pointArray(center), JsonPrimitive(radius)),
                ),
                attributes = commonAttributes(
                    colorIndex = index + 3,
                    fill = true,
                ),
            ),
        )
    }

    private fun functionGraphObjects(
        index: Int,
        random: DeterministicRandom,
    ): List<JsonObject> {
        val coefficient = random.number(0.05, 0.28)
        val offset = random.number(-2.5, 2.5)
        val expression = "$coefficient * x * x + $offset"
        return listOf(
            curveObject(
                id = "function",
                type = "functiongraph",
                parents = jsonArray(expression, -6, 6),
                colorIndex = index,
                pointCount = 96 + index % 96,
            ),
        )
    }

    private fun parametricObjects(
        index: Int,
        random: DeterministicRandom,
    ): List<JsonObject> {
        val radiusX = random.number(1.0, 4.5)
        val radiusY = random.number(0.8, 3.4)
        val offsetX = random.number(-1.0, 1.0)
        val offsetY = random.number(-1.0, 1.0)
        return listOf(
            curveObject(
                id = "parametric",
                type = "curve",
                parents = jsonArray(
                    "$radiusX * cos(x) + $offsetX",
                    "$radiusY * sin(x) + $offsetY",
                    0,
                    2 * PI,
                ),
                colorIndex = index,
                pointCount = 120 + index % 80,
            ),
        )
    }

    private fun dataPlotObjects(
        index: Int,
        random: DeterministicRandom,
    ): List<JsonObject> {
        val xValues = List(12) { pointIndex ->
            rounded(-5.5 + pointIndex)
        }
        val yValues = List(12) {
            random.number(-3.8, 3.8)
        }
        return listOf(
            curveObject(
                id = "data",
                type = "curve",
                parents = JsonArray(
                    listOf(
                        JsonArray(xValues.map(::JsonPrimitive)),
                        JsonArray(yValues.map(::JsonPrimitive)),
                    ),
                ),
                colorIndex = index,
                pointCount = 120,
            ),
        )
    }

    private fun polygonObjects(
        index: Int,
        random: DeterministicRandom,
    ): List<JsonObject> {
        val vertexCount = 3 + index % 6
        val centerX = random.number(-0.8, 0.8)
        val centerY = random.number(-0.8, 0.8)
        val vertices = List(vertexCount) { vertexIndex ->
            val angle = 2.0 * PI * vertexIndex / vertexCount
            val radius = random.number(2.0, 4.5)
            pointArray(
                JsxGraphPoint2D(
                    x = rounded(centerX + cos(angle) * radius),
                    y = rounded(centerY + sin(angle) * radius),
                ),
            )
        }
        return listOf(
            sourceObject(
                id = "polygon",
                type = "polygon",
                parents = JsonArray(vertices),
                attributes = commonAttributes(
                    colorIndex = index,
                    fill = true,
                    extras = mapOf(
                        "withLines" to JsonPrimitive(index % 4 != 0),
                    ),
                ),
            ),
        )
    }

    private fun textObjects(
        index: Int,
        random: DeterministicRandom,
    ): List<JsonObject> {
        val coordinate = randomPoint(random)
        val anchorX = listOf("left", "middle", "right")[index % 3]
        val anchorY = listOf("top", "middle", "bottom")[index % 3]
        val content = "Stress label $index"
        return listOf(
            sourceObject(
                id = "label",
                type = "text",
                parents = JsonArray(
                    listOf(
                        JsonPrimitive(coordinate.x),
                        JsonPrimitive(coordinate.y),
                        JsonPrimitive(content),
                    ),
                ),
                attributes = commonAttributes(
                    colorIndex = index,
                    extras = mapOf(
                        "fontSize" to JsonPrimitive(12 + index % 18),
                        "fontUnit" to JsonPrimitive("px"),
                        "display" to JsonPrimitive("internal"),
                        "parse" to JsonPrimitive(false),
                        "anchorX" to JsonPrimitive(anchorX),
                        "anchorY" to JsonPrimitive(anchorY),
                    ),
                ),
            ),
        )
    }

    private fun circularObjects(
        index: Int,
        random: DeterministicRandom,
    ): List<JsonObject> {
        val center = randomPoint(random)
        val radius = random.number(1.0, 2.5)
        val startAngle = random.number(0.0, PI)
        val sweep = random.number(0.45, 2.4)
        val radiusPoint = JsxGraphPoint2D(
            x = rounded(center.x + cos(startAngle) * radius),
            y = rounded(center.y + sin(startAngle) * radius),
        )
        val anglePoint = JsxGraphPoint2D(
            x = rounded(center.x + cos(startAngle + sweep) * radius),
            y = rounded(center.y + sin(startAngle + sweep) * radius),
        )
        val circularType = when (index % 3) {
            0 -> "arc"
            1 -> "sector"
            else -> "angle"
        }
        val attributes = commonAttributes(
            colorIndex = index,
            fill = circularType != "arc",
            extras = buildMap {
                put("selection", JsonPrimitive("minor"))
                put("orientation", JsonPrimitive("counterclockwise"))
                if (circularType == "arc") {
                    put("fillColor", JsonPrimitive("none"))
                }
                if (circularType == "angle") {
                    put("type", JsonPrimitive("sector"))
                    put("orthoType", JsonPrimitive("sector"))
                    put("radius", JsonPrimitive(1.0 + index % 3))
                }
            },
        )
        return listOf(
            pointObject("center", center, fixed = true, visible = false),
            pointObject("radius", radiusPoint, fixed = true, visible = false),
            pointObject("angle", anglePoint, fixed = true, visible = false),
            sourceObject(
                id = "region",
                type = circularType,
                parents = jsonArray("center", "radius", "angle"),
                attributes = attributes,
            ),
        )
    }

    private fun interactionObjects(
        index: Int,
        random: DeterministicRandom,
    ): List<JsonObject> {
        val driver = randomPoint(random, leftHalf = true)
        val anchor = randomPoint(random, leftHalf = false)
        return listOf(
            pointObject(
                id = "driver",
                coordinates = driver,
                fixed = false,
                colorIndex = index,
            ),
            pointObject(
                id = "anchor",
                coordinates = anchor,
                fixed = true,
                colorIndex = index + 1,
            ),
            sourceObject(
                id = "dependentLine",
                type = "line",
                parents = jsonArray("driver", "anchor"),
                attributes = commonAttributes(
                    colorIndex = index + 2,
                    extras = mapOf(
                        "straightFirst" to JsonPrimitive(false),
                        "straightLast" to JsonPrimitive(false),
                    ),
                ),
            ),
            sourceObject(
                id = "dependentCircle",
                type = "circle",
                parents = jsonArray("driver", random.number(0.7, 2.3)),
                attributes = commonAttributes(
                    colorIndex = index + 3,
                    fill = true,
                ),
            ),
        )
    }

    private fun constructionDocument(
        index: Int,
        objects: List<JsonObject>,
        keepAspectRatio: Boolean,
    ): String =
        buildJsonObject {
            put("schemaVersion", 1)
            putJsonArray("boundingBox") {
                add(JsonPrimitive(-6.0 - index % 3))
                add(JsonPrimitive(5.0 + index % 2))
                add(JsonPrimitive(6.0 + index % 4))
                add(JsonPrimitive(-5.0 - index % 2))
            }
            put("axis", true)
            put("grid", index % 5 != 0)
            put("keepAspectRatio", keepAspectRatio)
            put("objects", JsonArray(objects))
        }.toString()

    private fun pointObject(
        id: String,
        coordinates: JsxGraphPoint2D,
        fixed: Boolean,
        visible: Boolean = true,
        colorIndex: Int = 0,
    ): JsonObject =
        sourceObject(
            id = id,
            type = "point",
            parents = pointArray(coordinates),
            attributes = commonAttributes(
                colorIndex = colorIndex,
                extras = mapOf(
                    "fixed" to JsonPrimitive(fixed),
                    "visible" to JsonPrimitive(visible),
                    "size" to JsonPrimitive(3 + colorIndex % 5),
                ),
            ),
        )

    private fun curveObject(
        id: String,
        type: String,
        parents: JsonArray,
        colorIndex: Int,
        pointCount: Int,
    ): JsonObject =
        sourceObject(
            id = id,
            type = type,
            parents = parents,
            attributes = commonAttributes(
                colorIndex = colorIndex,
                extras = mapOf(
                    "fillColor" to JsonPrimitive("none"),
                    "doAdvancedPlot" to JsonPrimitive(false),
                    "numberPointsHigh" to JsonPrimitive(pointCount),
                ),
            ),
        )

    private fun sourceObject(
        id: String,
        type: String,
        parents: JsonArray,
        attributes: JsonObject,
    ): JsonObject =
        buildJsonObject {
            put("id", id)
            put("type", type)
            put("parents", parents)
            put("attributes", attributes)
        }

    private fun commonAttributes(
        colorIndex: Int,
        fill: Boolean = false,
        extras: Map<String, JsonPrimitive> = emptyMap(),
    ): JsonObject =
        buildJsonObject {
            put("name", "")
            put("withLabel", false)
            put("fixed", true)
            put("highlight", false)
            put("strokeColor", COLORS[colorIndex.mod(COLORS.size)])
            put("strokeWidth", 1.0 + colorIndex.mod(4) * 0.75)
            put(
                "fillColor",
                if (fill) COLORS[(colorIndex + 1).mod(COLORS.size)] else "none",
            )
            put("fillOpacity", if (fill) 0.2 + colorIndex.mod(3) * 0.1 else 1.0)
            for ((name, value) in extras) {
                put(name, value)
            }
        }

    private fun randomPoint(
        random: DeterministicRandom,
        leftHalf: Boolean? = null,
    ): JsxGraphPoint2D {
        val x = when (leftHalf) {
            true -> random.number(-5.2, -0.8)
            false -> random.number(0.8, 5.2)
            null -> random.number(-4.5, 4.5)
        }
        return JsxGraphPoint2D(
            x = x,
            y = random.number(-3.8, 3.8),
        )
    }

    private fun pointArray(point: JsxGraphPoint2D): JsonArray =
        JsonArray(
            listOf(
                JsonPrimitive(point.x),
                JsonPrimitive(point.y),
            ),
        )

    private fun jsonArray(vararg values: Any): JsonArray =
        buildJsonArray {
            for (value in values) {
                add(
                    when (value) {
                        is String -> JsonPrimitive(value)
                        is Int -> JsonPrimitive(value)
                        is Double -> JsonPrimitive(value)
                        else -> error("Unsupported test JSON value")
                    },
                )
            }
        }

    private fun assertFiniteScene(
        caseIndex: Int,
        scene: JsxGraphScene,
    ) {
        for (element in scene.elements) {
            assertTrue(
                element.style.strokeWidth.isFinite() &&
                    element.style.strokeWidth >= 0.0,
                "Stress case $caseIndex/${element.id} has invalid style",
            )
            when (element) {
                is JsxGraphSceneElement.Point ->
                    assertFinitePoint(caseIndex, element.id, element.coordinates)
                is JsxGraphSceneElement.Line -> {
                    assertFinitePoint(caseIndex, element.id, element.point1)
                    assertFinitePoint(caseIndex, element.id, element.point2)
                }
                is JsxGraphSceneElement.Ticks -> {
                    val bounds = scene.boundingBox
                    val resolved = element.definition.resolve(
                        visibleLeft = bounds.left,
                        visibleTop = bounds.top,
                        visibleRight = bounds.right,
                        visibleBottom = bounds.bottom,
                        cssPixelsPerUnitX = 50.0,
                        cssPixelsPerUnitY = 50.0,
                    )
                    resolved.paths.flatMap { it.points }.forEach { point ->
                        assertFinitePoint(caseIndex, element.id, point)
                    }
                    resolved.labels.forEach { label ->
                        assertFinitePoint(caseIndex, element.id, label.position)
                    }
                }
                is JsxGraphSceneElement.Circle -> {
                    assertFinitePoint(caseIndex, element.id, element.center)
                    assertTrue(element.radius.isFinite() && element.radius >= 0.0)
                }
                is JsxGraphSceneElement.Curve ->
                    element.points.filterNotNull().forEach { point ->
                        assertFinitePoint(caseIndex, element.id, point)
                    }
                is JsxGraphSceneElement.Polygon ->
                    element.vertices.forEach { point ->
                        assertFinitePoint(caseIndex, element.id, point)
                    }
                is JsxGraphSceneElement.Text ->
                    assertFinitePoint(caseIndex, element.id, element.coordinates)
                is JsxGraphSceneElement.Image -> {
                    assertFinitePoint(caseIndex, element.id, element.anchor)
                    assertFinitePoint(caseIndex, element.id, element.widthVector)
                    assertFinitePoint(caseIndex, element.id, element.heightVector)
                    assertTrue(
                        element.userWidth.isFinite() &&
                            element.userHeight.isFinite(),
                    )
                }
            }
        }
    }

    private fun assertFinitePoint(
        caseIndex: Int,
        elementId: String,
        point: JsxGraphPoint2D,
    ) {
        assertTrue(
            point.x.isFinite() && point.y.isFinite(),
            "Stress case $caseIndex/$elementId has invalid point $point",
        )
    }

    private fun rounded(value: Double): Double =
        round(value * 1_000.0) / 1_000.0

    private class DeterministicRandom(
        seed: UInt,
    ) {
        private var state = seed

        fun number(
            minimum: Double,
            maximum: Double,
        ): Double {
            state = state * 1_664_525u + 1_013_904_223u
            val fraction = (state shr 8).toDouble() / 0xFFFFFFu.toDouble()
            return round(
                (minimum + (maximum - minimum) * fraction) * 1_000.0,
            ) / 1_000.0
        }
    }

    private companion object {
        const val STRESS_CASE_COUNT = 512
        const val SHAPE_COUNT = 8
        const val MAX_SCENE_ELEMENTS = 32
        val SEED = 0x4A535847u
        val COLORS = listOf(
            "#1F5A94",
            "#167C73",
            "#B44335",
            "#9A4E1F",
            "#6C737A",
        )
    }
}
