/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ColorTest {
    @Test
    fun parserMatchesOfficialStringAndArrayFixture() {
        assertRgb("CornflowerBlue", 100.0, 149.0, 237.0)
        assertRgb(" light goldenrod yellow ", 250.0, 250.0, 210.0)
        assertRgb("rgb(12, 132, 233)", 12.0, 132.0, 233.0)
        assertRgb("rgba(123, 234, 45, 0.5)", 123.0, 234.0, 45.0)
        assertRgb("#fb0", 255.0, 187.0, 0.0)
        assertRgb("336699", 51.0, 102.0, 153.0)
        assertRgb("#112233aa", 17.0, 34.0, 51.0)
        assertRgb("rgb(999, 2, 3)", 255.0, 2.0, 3.0)
        assertEquals(
            RgbColor(128.0, 255.0, 0.0),
            ok(Color.rgbParser(listOf(0.5, 1, 0))),
        )
        assertEquals(
            RgbColor(0.5, 2.0, 0.0),
            ok(Color.rgbParser(listOf(0.5, 2, 0))),
        )
        assertEquals(
            RgbColor(12.0, 34.0, 56.0),
            ok(Color.rgbParser(12, 34, 56)),
        )
        assertIs<GMResult.Err<ColorError.InvalidColor>>(
            Color.rgbParser("not-a-color"),
        )
        assertIs<GMResult.Err<ColorError.InvalidChannelCount>>(
            Color.rgbParser(listOf(1, 2)),
        )
    }

    @Test
    fun formattingAndColorSpacesMatchOfficialFixture() {
        assertEquals("rgb(51, 102, 153)", ok(Color.rgb2css("#336699")))
        assertEquals("#336699", ok(Color.rgb2hex("rgb(51, 102, 153)")))
        assertEquals("rgb(51, 102, 153)", ok(Color.hex2rgb("#369")))

        val hsl = Color.hsv2hsl(210.0, 2.0 / 3.0, 0.6)
        assertClose(210.0, hsl[0])
        assertClose(0.5, hsl[1])
        assertClose(0.4, hsl[2])
        assertEquals("#336699", Color.hsv2rgb(210.0, 2.0 / 3.0, 0.6))
        assertEquals("#ff0080", Color.hsv2rgb(-30.0, 1.0, 1.0))
        assertEquals("#808080", Color.hsv2rgb(0.0, 0.0, 0.5))
        assertEquals("#ffffff", Color.hsv2rgb(10.0, 0.0, 0.5))

        val hsv = ok(Color.rgb2hsv("#336699"))
        assertClose(210.0, hsv[0])
        assertClose(2.0 / 3.0, hsv[1])
        assertClose(0.6, hsv[2])
    }

    @Test
    fun lmsOpacityAndPortableCssAdapterMatchOfficialFixture() {
        val lms = ok(Color.rgb2LMS("#336699"))
        assertClose(1.2102396519873293, lms[0])
        assertClose(1.0808654907499453, lms[1])
        assertClose(0.8914065350444567, lms[2])
        assertEquals(RgbColor(50.0, 102.0, 153.0), Color.LMS2rgb(
            long = lms[0],
            medium = lms[1],
            short = lms[2],
        ))

        val split = ok(Color.rgba2rgbo("#11223380"))
        assertEquals("#112233", split.rgb)
        assertClose(0.5019607843137255, split.opacity)
        assertEquals(RgboColor("#112233", 1.0), ok(
            Color.rgba2rgbo("#112233"),
        ))
        assertEquals("#11223380", ok(Color.rgbo2rgba("#112233", 0.5)))
        assertEquals("none", ok(Color.rgbo2rgba("none", 0.5)))
        assertEquals(
            "transparent",
            ok(Color.rgbo2rgba("transparent", 0.5)),
        )

        assertEquals(
            RgbaColor(17, 34, 51, 128),
            ok(Color.parseCssColor("#11223380")),
        )
        assertEquals(
            RgbaColor(123, 234, 45, 128),
            ok(Color.parseCssColor("rgba(123, 234, 45, 0.5)")),
        )
        assertEquals(
            RgbaColor(123, 234, 45, 64),
            ok(Color.parseCssColor("rgba(123, 234, 45, 0.25)")),
        )
        assertEquals(
            RgbaColor(128, 128, 128, 255),
            ok(Color.parseCssColor("grey")),
        )
        assertEquals(
            RgbaColor(0, 0, 0, 0),
            ok(Color.parseCssColor("transparent")),
        )
        assertIs<GMResult.Err<ColorError.InvalidColor>>(
            Color.parseCssColor("336699"),
        )
    }

    @Test
    fun transformsAndPaletteMatchOfficialFixture() {
        assertEquals("#5C5C5C", ok(Color.rgb2bw("#336699")))
        assertEquals("none", ok(Color.rgb2bw("none")))
        assertEquals(
            "#505E98",
            ok(Color.rgb2cb("#336699", "protanopia")),
        )
        assertEquals(
            "#47569A",
            ok(Color.rgb2cb("#336699", "deuteranopia")),
        )
        assertEquals(
            "#326791",
            ok(Color.rgb2cb("#336699", "tritanopia")),
        )
        assertEquals("#6699cc", ok(Color.shadeColor("#336699", 0.2)))
        assertEquals("#6699cc", ok(Color.lightenColor("#336699", 0.2)))
        assertEquals("#003366", ok(Color.darkenColor("#336699", 0.2)))
        assertEquals(
            "#7f007f",
            ok(Color.mixColor("#ff0000", "#0000ff")),
        )
        assertEquals(
            "#3f00bf",
            ok(Color.mixColor("#ff0000", "#0000ff", 0.25)),
        )
        assertEquals("#11223366", ok(Color.autoHighlight("#112233")))
        assertEquals("#1122333a", ok(Color.autoHighlight("#11223320")))
        assertEquals("red", ok(Color.autoHighlight("red")))
        assertEquals("#000000", ok(Color.contrast("#ffffff")))
        assertEquals("#ffffff", ok(Color.contrast("#000000")))
        assertEquals("#ffffff", ok(Color.contrast("#0088cc")))
        assertEquals("#ffffff", ok(Color.contrast("#808080", threshold = 0.0)))
        assertEquals(
            "#ffffff",
            ok(Color.contrast("#808080", threshold = Double.NaN)),
        )
        assertEquals(
            "#000000",
            ok(
                Color.contrast(
                    hexColor = "#ffffff",
                    darkColor = "",
                    lightColor = "",
                ),
            ),
        )
        assertEquals(
            "#111111",
            ok(
                Color.contrast(
                    hexColor = "#808080",
                    darkColor = "#111111",
                    lightColor = "#eeeeee",
                    threshold = 2.0,
                ),
            ),
        )
        assertEquals("#E69F00", Color.paletteWong["orange"])
        assertEquals("#D55E00", Color.palette["red"])
    }

    private fun assertRgb(
        source: String,
        red: Double,
        green: Double,
        blue: Double,
    ) {
        assertEquals(
            RgbColor(red, green, blue),
            ok(Color.rgbParser(source)),
        )
    }

    private fun assertClose(
        expected: Double,
        actual: Double,
        tolerance: Double = 1e-12,
    ) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "Expected $expected, actual $actual",
        )
    }

    private fun <T> ok(result: GMResult<T, ColorError>): T =
        assertIs<GMResult.Ok<T>>(result).value
}
