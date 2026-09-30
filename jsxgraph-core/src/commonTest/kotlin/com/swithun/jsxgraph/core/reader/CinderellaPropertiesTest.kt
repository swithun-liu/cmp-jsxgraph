/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CinderellaPropertiesTest {
    @Test
    fun colorTableAndParseIntPrefixMatchOfficialFixture() {
        assertEquals(
            listOf(
                "white",
                "red",
                "#ffafaf",
                "#c10000",
                "#ffbd77",
                "black",
                "black",
            ),
            listOf("0", "2tail", "6", "14", "21", "22", "bad").map(
                CinderellaProperties::calculateColor,
            ),
        )
    }

    @Test
    fun pointPropertiesMatchOfficialFixture() {
        assertEquals(
            CinderellaPointProperties(
                appearance = CinderellaAppearance(
                    listOf(
                        "red",
                        "5",
                        "1",
                        "0",
                        "0",
                        "9",
                        "true",
                        "false",
                    ),
                ),
                nextIndex = 3,
                border = "black",
                labelColor = "black",
            ),
            point(POINT_WITH_BORDER),
        )
        assertEquals(
            CinderellaPointProperties(
                appearance = CinderellaAppearance(
                    listOf(
                        "yellow",
                        "12",
                        "1",
                        "0",
                        "0",
                        "9",
                        "true",
                        "false",
                    ),
                ),
                nextIndex = 3,
                border = "none",
                labelColor = "yellow",
            ),
            point(POINT_WITHOUT_BORDER),
        )
    }

    @Test
    fun circleAndLinePropertiesMatchOfficialFixture() {
        assertEquals(
            CinderellaCircleProperties(
                appearance = CinderellaAppearance(
                    listOf(
                        "#c10000",
                        "5",
                        "2",
                        "0",
                        "0",
                        "9",
                        "false",
                        "false",
                    ),
                ),
                filling = "yellow",
                fillOpacity = 0.4,
                nextIndex = 3,
            ),
            circle(CIRCLE_VISIBILITY),
        )
        assertEquals(
            CinderellaCircleProperties(
                appearance = CinderellaAppearance(
                    listOf(
                        "#7700b7",
                        "5",
                        "4",
                        "0",
                        "0",
                        "9",
                        "false",
                        "false",
                    ),
                ),
                filling = "green",
                fillOpacity = 0.25,
                nextIndex = 3,
            ),
            circle(CIRCLE_ALPHA),
        )
        assertEquals(
            CinderellaLineProperties(
                appearance = CinderellaAppearance(
                    listOf(
                        "#199e4e",
                        "5",
                        "5",
                        "0",
                        "0",
                        "9",
                        "true",
                        "false",
                    ),
                ),
                dashing = 3,
                nextIndex = 2,
            ),
            line(DASHED_LINE),
        )
        assertEquals(
            CinderellaLineProperties(
                appearance = CinderellaAppearance(
                    listOf(
                        "#7700b7",
                        "5",
                        "2",
                        "0",
                        "0",
                        "9",
                        "true",
                        "false",
                    ),
                ),
                dashing = 0,
                nextIndex = 2,
            ),
            line(SOLID_LINE),
        )
    }

    @Test
    fun malformedPropertiesAndLimitsAreStructured() {
        assertIs<
            GMResult.Err<CinderellaPropertyError.PropertyNotFound>
        >(
            CinderellaProperties.readPointProperties(
                dataLines = listOf("definition"),
                startIndex = 0,
            ),
        )
        assertIs<
            GMResult.Err<CinderellaPropertyError.MalformedProperty>
        >(
            CinderellaProperties.readLineProperties(
                dataLines = listOf(
                    "definition",
                    "setAppearance(1, -2)",
                    "linedashing false",
                ),
                startIndex = 0,
            ),
        )
        assertIs<
            GMResult.Err<CinderellaPropertyError.InvalidStartIndex>
        >(
            CinderellaProperties.readCircleProperties(
                dataLines = emptyList(),
                startIndex = 0,
            ),
        )
        assertIs<
            GMResult.Err<CinderellaPropertyError.LineLimitExceeded>
        >(
            CinderellaProperties.readPointProperties(
                dataLines = POINT_WITH_BORDER,
                startIndex = 0,
                limits = CinderellaPropertyLimits(maxLines = 1),
            ),
        )
        assertIs<
            GMResult.Err<CinderellaPropertyError.AppearanceLimitExceeded>
        >(
            CinderellaProperties.readPointProperties(
                dataLines = POINT_WITH_BORDER,
                startIndex = 0,
                limits = CinderellaPropertyLimits(
                    maxAppearanceValues = 1,
                ),
            ),
        )
        assertIs<GMResult.Err<CinderellaPropertyError.InvalidLimits>>(
            CinderellaProperties.readLineProperties(
                dataLines = SOLID_LINE,
                startIndex = 0,
                limits = CinderellaPropertyLimits(maxLines = -1),
            ),
        )
    }

    private fun point(
        lines: List<String>,
    ): CinderellaPointProperties =
        assertIs<GMResult.Ok<CinderellaPointProperties>>(
            CinderellaProperties.readPointProperties(lines, 0),
        ).value

    private fun circle(
        lines: List<String>,
    ): CinderellaCircleProperties =
        assertIs<GMResult.Ok<CinderellaCircleProperties>>(
            CinderellaProperties.readCircleProperties(lines, 0),
        ).value

    private fun line(
        lines: List<String>,
    ): CinderellaLineProperties =
        assertIs<GMResult.Ok<CinderellaLineProperties>>(
            CinderellaProperties.readLineProperties(lines, 0),
        ).value

    private companion object {
        val POINT_WITH_BORDER = listOf(
            "(\"A\"):=FreePoint([]);",
            "\"A\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"A\".setAttribute(\"color\",\"2\");",
            "\"A\".setAttribute(\"pointborder\",\"true\");",
        )
        val POINT_WITHOUT_BORDER = listOf(
            "(\"B\"):=FreePoint([]);",
            "\"B\".setAppearance(5,12,1,0,0,9,true,false);",
            "\"B\".noPBorder();",
            "\"B\".setAttribute(\"pointborder\",\"false\");",
        )
        val CIRCLE_VISIBILITY = listOf(
            "(\"C0\"):=CircleMP(\"A\",\"B\");",
            "\"C0\".setAppearance(14,5,2,0,0,9,false,false);",
            "\"C0\".setAttribute(\"colorfill\",\"5\");",
            "\"C0\".setAttribute(\"visibilityfill\",\"4\");",
        )
        val CIRCLE_ALPHA = listOf(
            "(\"C1\"):=CircleMP(\"A\",\"B\");",
            "\"C1\".setAppearance(11,5,4,0,0,9,false,false);",
            "\"C1\".setAttribute(\"colorfill\",\"4\");",
            "\"C1\".setAttribute(\"fillalpha\",\"0.25\");",
        )
        val DASHED_LINE = listOf(
            "(\"a\"):=Join(\"A\",\"B\");",
            "\"a\".setAppearance(9,5,5,0,0,9,true,false);",
            "\"a\".setAttribute(\"linedashing\",\"true\");",
        )
        val SOLID_LINE = listOf(
            "(\"b\"):=Join(\"A\",\"B\");",
            "\"b\".setAppearance(11,5,2,0,0,9,true,false);",
            "\"b\".setAttribute(\"linedashing\",\"false\");",
        )
    }
}
