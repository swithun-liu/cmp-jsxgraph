/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsxGraphThemesTest {
    @Test
    fun builtInThemeDataMatchesOfficialFixtures() {
        assertEquals(
            ThemeFingerprint(98, "ea5e57bccf3f9afa"),
            fingerprint(JsxGraphThemes.darkOptionsPatch),
        )
        assertEquals(
            ThemeFingerprint(163, "dbb716ad98578b1c"),
            fingerprint(JsxGraphThemes.guiOptionsPatch),
        )
        assertEquals(
            ThemeFingerprint(207, "115f422c6fa770e8"),
            fingerprint(JsxGraphThemes.monoThin),
        )
    }

    @Test
    fun monoThinThemeAppliesBeforeExplicitElementAttributes() {
        val result = JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "theme": "mono_thin",
                  "objects": [
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [1, 2],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "B",
                      "type": "point",
                      "parents": [3, 1],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "fillColor": "#ff0000"
                      }
                    },
                    {
                      "id": "line",
                      "type": "line",
                      "parents": ["A", "B"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "straightFirst": false,
                        "straightLast": false
                      }
                    }
                  ]
                }
                """.trimIndent(),
            )
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value

        val first = assertIs<JsxGraphSceneElement.Point>(scene.elements[0])
        assertEquals(0.75, first.size)
        assertEquals(JsxGraphColor(0, 0, 0), first.style.strokeColor)
        assertEquals(JsxGraphColor(0, 0, 0), first.style.fillColor)
        assertEquals(0.6, first.style.strokeOpacity)

        val second = assertIs<JsxGraphSceneElement.Point>(scene.elements[1])
        assertEquals(JsxGraphColor(255, 0, 0), second.style.fillColor)

        val line = assertIs<JsxGraphSceneElement.Line>(scene.elements[2])
        assertEquals(0.75, line.style.strokeWidth)
        assertEquals(JsxGraphColor(0, 0, 0), line.style.strokeColor)
        assertEquals(0.6, line.style.strokeOpacity)
    }

    @Test
    fun monoThinThemeAlsoAppliesToJessieCodeBoards() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphJessieCode.parse(
                source = "A = point(1, 2) <<name: '', withLabel: false>>;",
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    theme = "mono_thin",
                ),
            ),
        ).value
        val point = assertIs<JsxGraphSceneElement.Point>(scene.elements.single())

        assertEquals(0.75, point.size)
        assertEquals(JsxGraphColor(0, 0, 0), point.style.strokeColor)
        assertEquals(JsxGraphColor(0, 0, 0), point.style.fillColor)
    }

    private fun fingerprint(source: JsonObject): ThemeFingerprint {
        val leaves = mutableListOf<String>()

        fun flatten(value: JsonElement, path: String) {
            when (value) {
                is JsonObject ->
                    for (
                        (key, child) in
                        value.entries.sortedBy { it.key }
                    ) {
                        flatten(
                            child,
                            if (path.isEmpty()) key else "$path.$key",
                        )
                    }
                is JsonArray ->
                    for ((index, child) in value.withIndex()) {
                        flatten(child, "$path[$index]")
                    }
                is JsonPrimitive -> leaves += "$path=$value"
            }
        }

        flatten(source, "")
        var hash = 0xcbf29ce484222325uL
        for (character in leaves.joinToString("\n")) {
            hash = (hash xor character.code.toULong()) * 0x100000001b3uL
        }
        return ThemeFingerprint(
            leaves = leaves.size,
            hash = hash.toString(16).padStart(16, '0'),
        )
    }

    private data class ThemeFingerprint(
        val leaves: Int,
        val hash: String,
    )
}
