/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/color.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class RgbColor(
    val red: Double,
    val green: Double,
    val blue: Double,
) {
    fun toList(): List<Double> = listOf(red, green, blue)
}

data class RgbaColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int,
)

data class RgboColor(
    val rgb: String,
    val opacity: Double,
)

sealed interface ColorError {
    data class InvalidColor(
        val value: String,
    ) : ColorError

    data class InvalidChannelCount(
        val actual: Int,
    ) : ColorError
}

object Color {
    // JSXGraph 1.13.3: src/utils/color.js -> rgbParser.
    fun rgbParser(color: String): GMResult<RgbColor, ColorError> {
        var colorString =
            if (color.startsWith("#")) {
                color.drop(1).take(6)
            } else {
                color
            }
        colorString = colorString.replace(" ", "").lowercase()
        colorString = SIMPLE_COLORS[colorString] ?: colorString

        val channels = parseFunctionalColor(colorString)
            ?: parseHexColor(colorString)
            ?: return GMResult.Err(ColorError.InvalidColor(color))
        return GMResult.Ok(
            RgbColor(
                red = channels[0].coerceIn(0, 255).toDouble(),
                green = channels[1].coerceIn(0, 255).toDouble(),
                blue = channels[2].coerceIn(0, 255).toDouble(),
            ),
        )
    }

    // JSXGraph 1.13.3: src/utils/color.js -> rgbParser array branch.
    fun rgbParser(
        values: List<Number>,
    ): GMResult<RgbColor, ColorError> {
        if (values.size < 3) {
            return GMResult.Err(
                ColorError.InvalidChannelCount(values.size),
            )
        }
        val channels = values.take(3).map(Number::toDouble)
        val hasFractionalText = channels.any { channel ->
            jsNumberToString(channel).contains('.')
        }
        val normalized =
            hasFractionalText && channels.all { it in 0.0..1.0 }
        return GMResult.Ok(
            RgbColor(
                red =
                    if (normalized) ceil(channels[0] * 255.0) else channels[0],
                green =
                    if (normalized) ceil(channels[1] * 255.0) else channels[1],
                blue =
                    if (normalized) ceil(channels[2] * 255.0) else channels[2],
            ),
        )
    }

    fun rgbParser(
        red: Number,
        green: Number,
        blue: Number,
    ): GMResult<RgbColor, ColorError> =
        rgbParser(listOf(red, green, blue))

    // Pure-KMP adapter for the renderer-facing CSS subset.
    fun parseCssColor(
        color: String,
    ): GMResult<RgbaColor, ColorError> {
        val compact = color.replace(" ", "").lowercase()
        if (compact == "none" || compact == "transparent") {
            return GMResult.Ok(RgbaColor(0, 0, 0, 0))
        }

        parseAlphaHex(compact)?.let { return GMResult.Ok(it) }

        RGBA_CSS_REGEX.matchEntire(compact)?.let { match ->
            val alpha = match.groupValues[4].toDoubleOrNull()
                ?: return GMResult.Err(ColorError.InvalidColor(color))
            return GMResult.Ok(
                RgbaColor(
                    red = match.groupValues[1].toInt().coerceIn(0, 255),
                    green = match.groupValues[2].toInt().coerceIn(0, 255),
                    blue = match.groupValues[3].toInt().coerceIn(0, 255),
                    alpha = JsMath.round(
                        alpha.coerceIn(0.0, 1.0) * 255.0,
                    ).toInt(),
                ),
            )
        }

        val parserInput = when {
            compact.startsWith("#") -> color
            compact.startsWith("rgb(") -> color
            compact in SIMPLE_COLORS -> color
            compact in CSS_COLOR_ALIASES -> CSS_COLOR_ALIASES.getValue(compact)
            else -> return GMResult.Err(ColorError.InvalidColor(color))
        }
        return when (val parsed = rgbParser(parserInput)) {
            is GMResult.Ok -> GMResult.Ok(
                RgbaColor(
                    red = parsed.value.red.toInt(),
                    green = parsed.value.green.toInt(),
                    blue = parsed.value.blue.toInt(),
                    alpha = 255,
                ),
            )
            is GMResult.Err -> parsed
        }
    }

    // JSXGraph 1.13.3: src/utils/color.js -> isColor.
    // Browser-only CSS validation is intentionally narrowed to rgbParser's
    // portable syntax plus the CSS transparent keyword.
    fun isColor(color: String): Boolean =
        color.replace(" ", "").lowercase() != "none" &&
            parseCssColor(color) is GMResult.Ok

    // JSXGraph 1.13.3: src/utils/color.js -> rgb2css.
    fun rgb2css(color: String): GMResult<String, ColorError> =
        formatCss(rgbParser(color))

    fun rgb2css(values: List<Number>): GMResult<String, ColorError> =
        formatCss(rgbParser(values))

    fun rgb2css(
        red: Number,
        green: Number,
        blue: Number,
    ): GMResult<String, ColorError> =
        formatCss(rgbParser(red, green, blue))

    // JSXGraph 1.13.3: src/utils/color.js -> rgb2hex.
    fun rgb2hex(color: String): GMResult<String, ColorError> =
        formatHex(rgbParser(color))

    fun rgb2hex(values: List<Number>): GMResult<String, ColorError> =
        formatHex(rgbParser(values))

    fun rgb2hex(
        red: Number,
        green: Number,
        blue: Number,
    ): GMResult<String, ColorError> =
        formatHex(rgbParser(red, green, blue))

    // JSXGraph 1.13.3: src/utils/color.js -> hex2rgb.
    fun hex2rgb(hex: String): GMResult<String, ColorError> =
        rgb2css(hex)

    // JSXGraph 1.13.3: src/utils/color.js -> hsv2hsl.
    fun hsv2hsl(
        hue: Double,
        saturation: Double,
        value: Double,
    ): List<Double> {
        val lightness = value * (1.0 - saturation * 0.5)
        val hslSaturation =
            if (lightness == 0.0 || lightness == 1.0) {
                0.0
            } else {
                (value - lightness) / min(lightness, 1.0 - lightness)
            }
        return listOf(hue, hslSaturation, lightness)
    }

    // JSXGraph 1.13.3: src/utils/color.js -> hsv2rgb.
    fun hsv2rgb(
        hue: Double,
        saturation: Double,
        value: Double,
    ): String {
        val normalizedHue = ((hue % 360.0) + 360.0) % 360.0
        val channels =
            if (saturation == 0.0) {
                if (normalizedHue.isNaN() || normalizedHue < Mat.eps) {
                    Triple(value, value, value)
                } else {
                    return "#ffffff"
                }
            } else {
                val hueSection = normalizedHue / 60.0
                val index = floor(hueSection).toInt()
                val fraction = hueSection - index
                val p = value * (1.0 - saturation)
                val q = value * (1.0 - saturation * fraction)
                val t = value * (1.0 - saturation * (1.0 - fraction))
                when (index) {
                    0 -> Triple(value, t, p)
                    1 -> Triple(q, value, p)
                    2 -> Triple(p, value, t)
                    3 -> Triple(p, q, value)
                    4 -> Triple(t, p, value)
                    5 -> Triple(value, p, q)
                    else -> Triple(Double.NaN, Double.NaN, Double.NaN)
                }
            }

        return buildString {
            append('#')
            append(hsvChannelToHex(channels.first))
            append(hsvChannelToHex(channels.second))
            append(hsvChannelToHex(channels.third))
        }
    }

    // JSXGraph 1.13.3: src/utils/color.js -> rgb2hsv.
    fun rgb2hsv(color: String): GMResult<List<Double>, ColorError> =
        when (val parsed = rgbParser(color)) {
            is GMResult.Ok -> GMResult.Ok(rgbToHsv(parsed.value))
            is GMResult.Err -> parsed
        }

    fun rgb2hsv(values: List<Number>): GMResult<List<Double>, ColorError> =
        when (val parsed = rgbParser(values)) {
            is GMResult.Ok -> GMResult.Ok(rgbToHsv(parsed.value))
            is GMResult.Err -> parsed
        }

    fun rgb2hsv(
        red: Number,
        green: Number,
        blue: Number,
    ): GMResult<List<Double>, ColorError> =
        rgb2hsv(listOf(red, green, blue))

    // JSXGraph 1.13.3: src/utils/color.js -> rgb2LMS.
    fun rgb2LMS(color: String): GMResult<List<Double>, ColorError> =
        when (val parsed = rgbParser(color)) {
            is GMResult.Ok -> GMResult.Ok(rgbToLms(parsed.value))
            is GMResult.Err -> parsed
        }

    fun rgb2LMS(values: List<Number>): GMResult<List<Double>, ColorError> =
        when (val parsed = rgbParser(values)) {
            is GMResult.Ok -> GMResult.Ok(rgbToLms(parsed.value))
            is GMResult.Err -> parsed
        }

    fun rgb2LMS(
        red: Number,
        green: Number,
        blue: Number,
    ): GMResult<List<Double>, ColorError> =
        rgb2LMS(listOf(red, green, blue))

    // JSXGraph 1.13.3: src/utils/color.js -> LMS2rgb.
    fun LMS2rgb(
        long: Double,
        medium: Double,
        short: Double,
    ): RgbColor {
        val red =
            long * 30.830854 +
                medium * -29.832659 +
                short * 1.610474
        val green =
            long * -6.481468 +
                medium * 17.715578 +
                short * -2.532642
        val blue =
            long * -0.37569 +
                medium * -1.199062 +
                short * 14.273846
        return RgbColor(
            red = lmsLookup(red).toDouble(),
            green = lmsLookup(green).toDouble(),
            blue = lmsLookup(blue).toDouble(),
        )
    }

    // JSXGraph 1.13.3: src/utils/color.js -> rgba2rgbo.
    fun rgba2rgbo(rgba: String): GMResult<RgboColor, ColorError> {
        if (rgba.length != 9 || rgba.firstOrNull() != '#') {
            return GMResult.Ok(RgboColor(rgba, 1.0))
        }
        val alpha = rgba.substring(7, 9).toIntOrNull(16)
            ?: return GMResult.Err(ColorError.InvalidColor(rgba))
        return GMResult.Ok(
            RgboColor(
                rgb = rgba.take(7),
                opacity = alpha / 255.0,
            ),
        )
    }

    // JSXGraph 1.13.3: src/utils/color.js -> rgbo2rgba.
    fun rgbo2rgba(
        rgb: String,
        opacity: Double,
    ): GMResult<String, ColorError> {
        if (rgb == "none" || rgb == "transparent") {
            return GMResult.Ok(rgb)
        }
        val alpha = JsMath.round(opacity * 255.0).toInt()
            .toString(16)
            .padStart(2, '0')
        return when (val hex = rgb2hex(rgb)) {
            is GMResult.Ok -> GMResult.Ok(hex.value + alpha)
            is GMResult.Err -> hex
        }
    }

    // JSXGraph 1.13.3: src/utils/color.js -> rgb2bw.
    fun rgb2bw(color: String): GMResult<String, ColorError> {
        if (color == "none") {
            return GMResult.Ok(color)
        }
        return when (val parsed = rgbParser(color)) {
            is GMResult.Err -> parsed
            is GMResult.Ok -> {
                val channel = floor(
                    0.3 * parsed.value.red +
                        0.59 * parsed.value.green +
                        0.11 * parsed.value.blue,
                ).toInt()
                val hex = channel.toString(16).uppercase().padStart(2, '0')
                GMResult.Ok("#$hex$hex$hex")
            }
        }
    }

    // JSXGraph 1.13.3: src/utils/color.js -> rgb2cb.
    fun rgb2cb(
        color: String,
        deficiency: String,
    ): GMResult<String, ColorError> {
        if (color == "none") {
            return GMResult.Ok(color)
        }
        val lms = when (val result = rgb2LMS(color)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        var long = lms[0]
        var medium = lms[1]
        var short = lms[2]

        when (deficiency.lowercase()) {
            "protanopia" -> {
                val ratio = short / medium
                long =
                    if (ratio < 0.6903216543277437) {
                        -(
                            0.08277001656812001 * medium +
                                -0.013200141220000003 * short
                            ) / -0.06150039994295001
                    } else {
                        -(
                            -0.07934519995360001 * medium +
                                0.013289415272000003 * short
                            ) / 0.05858939668799999
                    }
            }
            "tritanopia" -> {
                val ratio = medium / long
                short =
                    if (ratio < 0.8349489908460004) {
                        -(
                            -0.00058973116217 * long +
                                0.007690316482 * medium
                            ) / -0.01011703519052
                    } else {
                        -(
                            0.025495080838999994 * long +
                                -0.0422740347 * medium
                            ) / 0.017005316784
                    }
            }
            else -> {
                val ratio = short / long
                medium =
                    if (ratio < 0.5763833686400911) {
                        -(
                            -0.06150039994295001 * long +
                                -0.013200141220000003 * short
                            ) / 0.08277001656812001
                    } else {
                        -(
                            0.05858939668799999 * long +
                                0.013289415272000003 * short
                            ) / -0.07934519995360001
                    }
            }
        }

        val rgb = LMS2rgb(long, medium, short)
        return GMResult.Ok(
            "#" +
                rgb.red.toInt().toString(16).uppercase().padStart(2, '0') +
                rgb.green.toInt().toString(16).uppercase().padStart(2, '0') +
                rgb.blue.toInt().toString(16).uppercase().padStart(2, '0'),
        )
    }

    // JSXGraph 1.13.3: src/utils/color.js -> shadeColor.
    fun shadeColor(
        color: String,
        percent: Double,
    ): GMResult<String, ColorError> {
        val rgb = when (val parsed = rgbParser(color)) {
            is GMResult.Ok -> parsed.value
            is GMResult.Err -> return parsed
        }
        return formatHex(
            GMResult.Ok(
                RgbColor(
                    red =
                        (rgb.red + 255.0 * percent)
                            .toInt()
                            .coerceIn(0, 255)
                            .toDouble(),
                    green =
                        (rgb.green + 255.0 * percent)
                            .toInt()
                            .coerceIn(0, 255)
                            .toDouble(),
                    blue =
                        (rgb.blue + 255.0 * percent)
                            .toInt()
                            .coerceIn(0, 255)
                            .toDouble(),
                ),
            ),
        )
    }

    fun lightenColor(
        color: String,
        percent: Double,
    ): GMResult<String, ColorError> =
        shadeColor(color, percent)

    fun darkenColor(
        color: String,
        percent: Double,
    ): GMResult<String, ColorError> =
        shadeColor(color, -percent)

    // JSXGraph 1.13.3: src/utils/color.js -> mixColor.
    fun mixColor(
        color1: String,
        color2: String,
        percent: Double = 0.5,
    ): GMResult<String, ColorError> {
        val rgb1 = when (val parsed = rgbParser(color1)) {
            is GMResult.Ok -> parsed.value
            is GMResult.Err -> return parsed
        }
        val rgb2 = when (val parsed = rgbParser(color2)) {
            is GMResult.Ok -> parsed.value
            is GMResult.Err -> return parsed
        }
        return formatHex(
            GMResult.Ok(
                RgbColor(
                    red =
                        (
                            rgb1.red * percent +
                                rgb2.red * (1.0 - percent)
                            ).toInt().toDouble(),
                    green =
                        (
                            rgb1.green * percent +
                                rgb2.green * (1.0 - percent)
                            ).toInt().toDouble(),
                    blue =
                        (
                            rgb1.blue * percent +
                                rgb2.blue * (1.0 - percent)
                            ).toInt().toDouble(),
                ),
            ),
        )
    }

    // JSXGraph 1.13.3: src/utils/color.js -> autoHighlight.
    fun autoHighlight(color: String): GMResult<String, ColorError> {
        val split = when (val result = rgba2rgbo(color)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (!color.startsWith("#")) {
            return GMResult.Ok(color)
        }
        val opacity =
            if (split.opacity < 0.3) {
                split.opacity * 1.8
            } else {
                split.opacity * 0.4
            }
        return rgbo2rgba(split.rgb, opacity)
    }

    // JSXGraph 1.13.3: src/utils/color.js -> contrast.
    fun contrast(
        hexColor: String,
        darkColor: String = "#000000",
        lightColor: String = "#ffffff",
        threshold: Double = 7.0,
    ): GMResult<String, ColorError> {
        val effectiveDarkColor =
            if (darkColor.isEmpty()) "#000000" else darkColor
        val effectiveLightColor =
            if (lightColor.isEmpty()) "#ffffff" else lightColor
        val effectiveThreshold =
            if (threshold == 0.0 || threshold.isNaN()) 7.0 else threshold
        val rgb = when (val parsed = rgbParser(hexColor)) {
            is GMResult.Ok -> parsed.value
            is GMResult.Err -> return parsed
        }
        val lightness =
            0.2126 * (rgb.red / 255.0).pow(2.2) +
                0.7152 * (rgb.green / 255.0).pow(2.2) +
                0.0722 * (rgb.blue / 255.0).pow(2.2)
        val contrastRatio = floor((lightness + 0.05) / 0.05) - 1.0
        return GMResult.Ok(
            if (contrastRatio > effectiveThreshold) {
                effectiveDarkColor
            } else {
                effectiveLightColor
            },
        )
    }

    val paletteWong: Map<String, String> = linkedMapOf(
        "black" to "#000000",
        "orange" to "#E69F00",
        "skyblue" to "#56B4E9",
        "bluishgreen" to "#009E73",
        "yellow" to "#F0E442",
        "darkblue" to "#0072B2",
        "vermillion" to "#D55E00",
        "reddishpurple" to "#CC79A7",
        "blue" to "#0072B2",
        "red" to "#D55E00",
        "green" to "#009E73",
        "purple" to "#CC79A7",
        "white" to "#ffffff",
    )

    val palette: Map<String, String>
        get() = paletteWong

    private fun parseFunctionalColor(value: String): IntArray? {
        val match = RGBA_REGEX.matchEntire(value)
            ?: RGB_REGEX.matchEntire(value)
            ?: return null
        return intArrayOf(
            match.groupValues[1].toInt(),
            match.groupValues[2].toInt(),
            match.groupValues[3].toInt(),
        )
    }

    private fun parseHexColor(value: String): IntArray? {
        HEX6_REGEX.matchEntire(value)?.let { match ->
            return intArrayOf(
                parseHexPrefix(match.groupValues[1]) ?: return null,
                parseHexPrefix(match.groupValues[2]) ?: return null,
                parseHexPrefix(match.groupValues[3]) ?: return null,
            )
        }
        HEX3_REGEX.matchEntire(value)?.let { match ->
            return intArrayOf(
                parseHexPrefix(match.groupValues[1].repeat(2))
                    ?: return null,
                parseHexPrefix(match.groupValues[2].repeat(2))
                    ?: return null,
                parseHexPrefix(match.groupValues[3].repeat(2))
                    ?: return null,
            )
        }
        return null
    }

    private fun parseAlphaHex(value: String): RgbaColor? {
        if (!value.startsWith("#")) {
            return null
        }
        val digits = value.drop(1)
        val expanded = when (digits.length) {
            4 -> buildString {
                for (digit in digits) {
                    append(digit)
                    append(digit)
                }
            }
            8 -> digits
            else -> return null
        }
        val channels = expanded.chunked(2).map { channel ->
            channel.toIntOrNull(16) ?: return null
        }
        return RgbaColor(
            red = channels[0],
            green = channels[1],
            blue = channels[2],
            alpha = channels[3],
        )
    }

    private fun formatCss(
        result: GMResult<RgbColor, ColorError>,
    ): GMResult<String, ColorError> =
        when (result) {
            is GMResult.Ok -> GMResult.Ok(
                "rgb(" +
                    jsNumberToString(result.value.red) +
                    ", " +
                    jsNumberToString(result.value.green) +
                    ", " +
                    jsNumberToString(result.value.blue) +
                    ")",
            )
            is GMResult.Err -> result
        }

    private fun formatHex(
        result: GMResult<RgbColor, ColorError>,
    ): GMResult<String, ColorError> =
        when (result) {
            is GMResult.Ok -> GMResult.Ok(
                "#" +
                    result.value.red.toInt()
                        .toString(16)
                        .padStart(2, '0') +
                    result.value.green.toInt()
                        .toString(16)
                        .padStart(2, '0') +
                    result.value.blue.toInt()
                        .toString(16)
                        .padStart(2, '0'),
            )
            is GMResult.Err -> result
        }

    private fun rgbToHsv(rgb: RgbColor): List<Double> {
        val fractionalRed = rgb.red / 255.0
        val fractionalGreen = rgb.green / 255.0
        val fractionalBlue = rgb.blue / 255.0
        val maximum = max(rgb.red, max(rgb.green, rgb.blue))
        val minimum = min(rgb.red, min(rgb.green, rgb.blue))
        val fractionalMaximum = maximum / 255.0
        val fractionalMinimum = minimum / 255.0
        val value = fractionalMaximum
        val saturation =
            if (value > 0.0) {
                (value - fractionalMinimum) / value
            } else {
                0.0
            }
        var hue = 1.0 / (fractionalMaximum - fractionalMinimum)
        if (saturation > 0.0) {
            hue = when (maximum) {
                rgb.red ->
                    (fractionalGreen - fractionalBlue) * hue
                rgb.green ->
                    2.0 + (fractionalBlue - fractionalRed) * hue
                else ->
                    4.0 + (fractionalRed - fractionalGreen) * hue
            }
        }
        hue *= 60.0
        if (hue < 0.0) {
            hue += 360.0
        }
        if (maximum == minimum) {
            hue = 0.0
        }
        return listOf(hue, saturation, value)
    }

    private fun rgbToLms(rgb: RgbColor): List<Double> {
        val red = rgb.red.pow(0.476190476)
        val green = rgb.green.pow(0.476190476)
        val blue = rgb.blue.pow(0.476190476)
        return listOf(
            red * 0.05059983 +
                green * 0.08585369 +
                blue * 0.0095242,
            red * 0.01893033 +
                green * 0.08925308 +
                blue * 0.01370054,
            red * 0.00292202 +
                green * 0.00975732 +
                blue * 0.07145979,
        )
    }

    private fun lmsLookup(value: Double): Int {
        var offset = 127
        var step = 64
        while (step > 0) {
            if (offset.toDouble().pow(0.476190476) > value) {
                offset -= step
            } else {
                if ((offset + 1).toDouble().pow(0.476190476) > value) {
                    return offset
                }
                offset += step
            }
            step /= 2
        }
        if (offset == 254 && 13.994955247 < value) {
            return 255
        }
        return offset
    }

    private fun hsvChannelToHex(channel: Double): String {
        val value = JsMath.round(channel * 255.0)
        if (!value.isFinite()) {
            return "00"
        }
        val hex = value.toInt().toString(16)
        return when (hex.length) {
            1 -> "0$hex"
            2 -> hex
            else -> "00"
        }
    }

    private fun parseHexPrefix(value: String): Int? {
        var parsed = 0
        var count = 0
        for (character in value) {
            val digit = character.digitToIntOrNull(16) ?: break
            parsed = parsed * 16 + digit
            count += 1
        }
        return if (count == 0) null else parsed
    }

    private fun jsNumberToString(value: Double): String =
        if (
            value.isFinite() &&
            value >= Long.MIN_VALUE.toDouble() &&
            value <= Long.MAX_VALUE.toDouble() &&
            value == value.toLong().toDouble()
        ) {
            value.toLong().toString()
        } else {
            value.toString()
        }

    private val RGBA_REGEX = Regex(
        """^rgba\((\d{1,3}),(\d{1,3}),(\d{1,3}),([\d.]{1,3})\)$""",
    )
    private val RGBA_CSS_REGEX = Regex(
        """^rgba\((\d{1,3}),(\d{1,3}),(\d{1,3}),([+-]?(?:\d+(?:\.\d*)?|\.\d+))\)$""",
    )
    private val RGB_REGEX =
        Regex("""^rgb\((\d{1,3}),(\d{1,3}),(\d{1,3})\)$""")
    private val HEX6_REGEX = Regex("""^(\w{2})(\w{2})(\w{2})$""")
    private val HEX3_REGEX = Regex("""^(\w)(\w)(\w)$""")

    private val SIMPLE_COLORS = mapOf(
        "aliceblue" to "f0f8ff",
        "antiquewhite" to "faebd7",
        "aqua" to "00ffff",
        "aquamarine" to "7fffd4",
        "azure" to "f0ffff",
        "beige" to "f5f5dc",
        "bisque" to "ffe4c4",
        "black" to "000000",
        "blanchedalmond" to "ffebcd",
        "blue" to "0000ff",
        "blueviolet" to "8a2be2",
        "brown" to "a52a2a",
        "burlywood" to "deb887",
        "cadetblue" to "5f9ea0",
        "chartreuse" to "7fff00",
        "chocolate" to "d2691e",
        "coral" to "ff7f50",
        "cornflowerblue" to "6495ed",
        "cornsilk" to "fff8dc",
        "crimson" to "dc143c",
        "cyan" to "00ffff",
        "darkblue" to "00008b",
        "darkcyan" to "008b8b",
        "darkgoldenrod" to "b8860b",
        "darkgray" to "a9a9a9",
        "darkgreen" to "006400",
        "darkkhaki" to "bdb76b",
        "darkmagenta" to "8b008b",
        "darkolivegreen" to "556b2f",
        "darkorange" to "ff8c00",
        "darkorchid" to "9932cc",
        "darkred" to "8b0000",
        "darksalmon" to "e9967a",
        "darkseagreen" to "8fbc8f",
        "darkslateblue" to "483d8b",
        "darkslategray" to "2f4f4f",
        "darkturquoise" to "00ced1",
        "darkviolet" to "9400d3",
        "deeppink" to "ff1493",
        "deepskyblue" to "00bfff",
        "dimgray" to "696969",
        "dodgerblue" to "1e90ff",
        "feldspar" to "d19275",
        "firebrick" to "b22222",
        "floralwhite" to "fffaf0",
        "forestgreen" to "228b22",
        "fuchsia" to "ff00ff",
        "gainsboro" to "dcdcdc",
        "ghostwhite" to "f8f8ff",
        "gold" to "ffd700",
        "goldenrod" to "daa520",
        "gray" to "808080",
        "green" to "008000",
        "greenyellow" to "adff2f",
        "honeydew" to "f0fff0",
        "hotpink" to "ff69b4",
        "indianred" to "cd5c5c",
        "indigo" to "4b0082",
        "ivory" to "fffff0",
        "khaki" to "f0e68c",
        "lavender" to "e6e6fa",
        "lavenderblush" to "fff0f5",
        "lawngreen" to "7cfc00",
        "lemonchiffon" to "fffacd",
        "lightblue" to "add8e6",
        "lightcoral" to "f08080",
        "lightcyan" to "e0ffff",
        "lightgoldenrodyellow" to "fafad2",
        "lightgrey" to "d3d3d3",
        "lightgreen" to "90ee90",
        "lightpink" to "ffb6c1",
        "lightsalmon" to "ffa07a",
        "lightseagreen" to "20b2aa",
        "lightskyblue" to "87cefa",
        "lightslateblue" to "8470ff",
        "lightslategray" to "778899",
        "lightsteelblue" to "b0c4de",
        "lightyellow" to "ffffe0",
        "lime" to "00ff00",
        "limegreen" to "32cd32",
        "linen" to "faf0e6",
        "magenta" to "ff00ff",
        "maroon" to "800000",
        "mediumaquamarine" to "66cdaa",
        "mediumblue" to "0000cd",
        "mediumorchid" to "ba55d3",
        "mediumpurple" to "9370d8",
        "mediumseagreen" to "3cb371",
        "mediumslateblue" to "7b68ee",
        "mediumspringgreen" to "00fa9a",
        "mediumturquoise" to "48d1cc",
        "mediumvioletred" to "c71585",
        "midnightblue" to "191970",
        "mintcream" to "f5fffa",
        "mistyrose" to "ffe4e1",
        "moccasin" to "ffe4b5",
        "navajowhite" to "ffdead",
        "navy" to "000080",
        "oldlace" to "fdf5e6",
        "olive" to "808000",
        "olivedrab" to "6b8e23",
        "orange" to "ffa500",
        "orangered" to "ff4500",
        "orchid" to "da70d6",
        "palegoldenrod" to "eee8aa",
        "palegreen" to "98fb98",
        "paleturquoise" to "afeeee",
        "palevioletred" to "d87093",
        "papayawhip" to "ffefd5",
        "peachpuff" to "ffdab9",
        "peru" to "cd853f",
        "pink" to "ffc0cb",
        "plum" to "dda0dd",
        "powderblue" to "b0e0e6",
        "purple" to "800080",
        "red" to "ff0000",
        "rosybrown" to "bc8f8f",
        "royalblue" to "4169e1",
        "saddlebrown" to "8b4513",
        "salmon" to "fa8072",
        "sandybrown" to "f4a460",
        "seagreen" to "2e8b57",
        "seashell" to "fff5ee",
        "sienna" to "a0522d",
        "silver" to "c0c0c0",
        "skyblue" to "87ceeb",
        "slateblue" to "6a5acd",
        "slategray" to "708090",
        "snow" to "fffafa",
        "springgreen" to "00ff7f",
        "steelblue" to "4682b4",
        "tan" to "d2b48c",
        "teal" to "008080",
        "thistle" to "d8bfd8",
        "tomato" to "ff6347",
        "turquoise" to "40e0d0",
        "venetianred" to "ae181e",
        "violet" to "ee82ee",
        "violetred" to "d02090",
        "wheat" to "f5deb3",
        "white" to "ffffff",
        "whitesmoke" to "f5f5f5",
        "yellow" to "ffff00",
        "yellowgreen" to "9acd32",
    )

    private val CSS_COLOR_ALIASES = mapOf(
        "darkgrey" to "darkgray",
        "dimgrey" to "dimgray",
        "grey" to "gray",
        "lightgray" to "lightgrey",
        "lightslategrey" to "lightslategray",
        "slategrey" to "slategray",
    )
}
