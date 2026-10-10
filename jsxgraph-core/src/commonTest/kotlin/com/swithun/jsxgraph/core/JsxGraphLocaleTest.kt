/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JsxGraphLocaleTest {
    @Test
    fun publicJessieCodeEntryInjectsNormalizedLocaleFormatting() {
        val requests = mutableListOf<JsxGraphLocaleNumberFormatRequest>()
        val formatter = JsxGraphLocaleNumberFormatter { request ->
            requests += request
            GMResult.Ok("localized")
        }

        val result = JsxGraphJessieCode.parse(
            source =
                """
                text(0, 0, -12.3456) <<
                    formatNumber: true,
                    digits: 3,
                    intl: <<
                        enabled: true,
                        locale: "de-DE",
                        options: <<
                            unitDisplay: "narrow",
                            maximumFractionDigits: 1
                        >>
                    >>
                >>;
                """.trimIndent(),
            boardOptions = JsxGraphJessieCodeBoardOptions(
                runtimeOptions = JsxGraphRuntimeOptions(
                    localeNumberFormatter = formatter,
                ),
            ),
        )

        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value
        assertEquals(
            "localized",
            scene.elements
                .filterIsInstance<JsxGraphSceneElement.Text>()
                .single()
                .content,
        )
        val request = requests.single()
        assertEquals(-12.3456, request.value)
        assertEquals("de-DE", request.locale)
        assertEquals(
            JsxGraphLocaleOptionValue.NumberValue(1.0),
            request.options["maximumFractionDigits"],
        )
        assertEquals(
            JsxGraphLocaleOptionValue.StringValue("narrow"),
            request.options["unitDisplay"],
        )
    }

    @Test
    fun inheritedBoardLocaleAndMissingFormatterUseUpstreamFallback() {
        val board = board()
        board.setAttribute(
            mapOf(
                "intl" to mapOf(
                    "enabled" to true,
                    "locale" to "fr-FR",
                ),
            ),
        )
        val text = text(board)

        assertEquals(true, text.useLocale())
        assertEquals(12.5, text.formatNumberLocale(12.5, 4))
        assertEquals(null, text.localeNumberFormatError)
    }

    @Test
    fun formatterFailureIsStructuredAndFormatMethodFallsBack() {
        val expected =
            JsxGraphLocaleNumberFormatError.Rejected("unsupported locale")
        val board = board(
            formatter = JsxGraphLocaleNumberFormatter {
                GMResult.Err(expected)
            },
        )
        val text = text(board)
        text.setAttribute(
            mapOf(
                "intl" to mapOf(
                    "enabled" to true,
                    "locale" to "x-test",
                ),
            ),
        )

        val result = text.formatNumberLocaleResult(7.25, 2)
        assertSame(expected, assertIs<GMResult.Err<*>>(result).error)
        assertSame(expected, text.localeNumberFormatError)
        assertEquals(7.25, text.formatNumberLocale(7.25, 2))
    }

    @Test
    fun thrownFormatterExceptionIsContainedAtTheBoardBoundary() {
        val board = board(
            formatter = JsxGraphLocaleNumberFormatter {
                throw IllegalStateException("formatter failed")
            },
        )
        val text = text(board)
        text.setAttribute(
            mapOf("intl" to mapOf("enabled" to true)),
        )

        val error = assertIs<
            GMResult.Err<JsxGraphLocaleNumberFormatError.CallbackFailed>
            >(text.formatNumberLocaleResult(1.0)).error
        assertEquals("formatter failed", error.message)
    }

    @Test
    fun richTextCreatorsUseTheirUpstreamLocaleOwner() {
        val requests = mutableListOf<JsxGraphLocaleNumberFormatRequest>()
        val formatter = JsxGraphLocaleNumberFormatter { request ->
            requests += request
            val digits = (
                request.options["maximumFractionDigits"] as?
                    JsxGraphLocaleOptionValue.NumberValue
                )?.value?.toInt()
            GMResult.Ok("${request.locale}:$digits")
        }

        val result = JsxGraphJessieCode.parse(
            source =
                """
                text(0, 4, "v=<value>2.5</value>") <<
                    id: "dynamicText", name: "", digits: 2,
                    intl: << enabled: true, locale: "text" >>
                >>;
                measurement(0, 3, ["+", 1, 2]) <<
                    id: "measurement", name: "",
                    prefix: "m=", digits: 3,
                    intl: << enabled: true, locale: "measurement" >>
                >>;
                p = point(1.25, 2.75) <<
                    id: "point", name: "", withLabel: false
                >>;
                smartlabel(p) <<
                    id: "smart", name: "",
                    prefix: "p=", baseUnit: "cm", digits: 1,
                    useMathJax: false,
                    intl: << enabled: true, locale: "smart" >>
                >>;
                a = point(-3, -1) <<
                    id: "a", name: "", withLabel: false
                >>;
                b = point(3, 2) <<
                    id: "b", name: "", withLabel: false
                >>;
                source = segment(a, b) <<
                    id: "source", name: "", withLabel: false
                >>;
                g = glider(0, 0.5, source) <<
                    id: "sourceGlider", name: "", withLabel: false
                >>;
                slopetriangle(source, g) <<
                    id: "triangle", name: "",
                    digits: 3, prefix: "s=",
                    basepoint: << id: "slopeBase", name: "" >>,
                    baseline: << id: "slopeLine", name: "" >>,
                    glider: << id: "slopeGlider", name: "" >>,
                    toppoint: << id: "slopeTop", name: "" >>,
                    label: <<
                        intl: << enabled: true, locale: "slope" >>
                    >>
                >>;
                tapemeasure([0, -2], [3, 2]) <<
                    id: "tape", name: "d", withTicks: false,
                    label: <<
                        digits: 3,
                        intl: << enabled: true, locale: "tape" >>
                    >>
                >>;
                f = functiongraph("x + 1", -5, 5) <<
                    id: "function", name: "",
                    doAdvancedPlot: false, numberPointsHigh: 64
                >>;
                integral([-1, 1], f) <<
                    id: "integral", name: "",
                    curveLeft: << id: "integralLeft", name: "" >>,
                    baseLeft: << id: "integralLeftBase", name: "" >>,
                    curveRight: << id: "integralRight", name: "" >>,
                    baseRight: << id: "integralRightBase", name: "" >>,
                    label: <<
                        id: "integralLabel", digits: 4,
                        intl: << enabled: true, locale: "integral" >>
                    >>
                >>;
                """.trimIndent(),
            boardOptions = JsxGraphJessieCodeBoardOptions(
                runtimeOptions = JsxGraphRuntimeOptions(
                    localeNumberFormatter = formatter,
                ),
            ),
        )

        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value
        val texts = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Text>()
        assertEquals(
            "v=text:2",
            texts.single { it.id == "dynamicText" }.content,
        )
        assertEquals(
            "m=measurement:2",
            texts.single { it.id == "measurement" }.content,
        )
        assertEquals(
            "p=smart:2 cm / smart:2 cm",
            texts.single { it.id == "smart" }.content,
        )
        assertTrue(texts.any { it.content == "s=slope:2" })
        assertEquals(
            "d = tape:2",
            texts.single { it.id == "tapeLabel" }.content,
        )
        assertEquals(
            "\u222b = integral:2",
            texts.single { it.id == "integralLabel" }.content,
        )
        assertTrue(
            listOf(
                "text",
                "measurement",
                "smart",
                "slope",
                "tape",
                "integral",
            ).all { locale ->
                requests.any { it.locale == locale }
            },
        )
    }

    private fun board(
        formatter: JsxGraphLocaleNumberFormatter? = null,
    ): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        localeNumberFormatter = formatter,
    )

    private fun text(board: Board): Text =
        assertIs<GMResult.Ok<Text>>(
            Text.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                content = "",
                name = "",
            ),
        ).value
}
