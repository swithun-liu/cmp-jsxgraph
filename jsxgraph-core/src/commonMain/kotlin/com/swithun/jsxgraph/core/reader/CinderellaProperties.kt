/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/cinderella.js -> calculateColor,
 * readPointProperties, readCircleProperties, and readLineProperties.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult

internal data class CinderellaPropertyLimits(
    val maxLines: Int = 1_000_000,
    val maxAppearanceValues: Int = 1_000,
)

internal sealed interface CinderellaPropertyError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : CinderellaPropertyError

    data class LineLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : CinderellaPropertyError

    data class InvalidStartIndex(
        val index: Int,
        val lineCount: Int,
    ) : CinderellaPropertyError

    data class PropertyNotFound(
        val property: String,
        val afterIndex: Int,
    ) : CinderellaPropertyError

    data class MalformedProperty(
        val property: String,
        val lineIndex: Int,
        val line: String,
    ) : CinderellaPropertyError

    data class AppearanceLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : CinderellaPropertyError
}

internal data class CinderellaAppearance(
    val values: List<String>,
)

internal data class CinderellaPointProperties(
    val appearance: CinderellaAppearance,
    val nextIndex: Int,
    val border: String,
    val labelColor: String,
)

internal data class CinderellaCircleProperties(
    val appearance: CinderellaAppearance,
    val filling: String,
    val fillOpacity: Double,
    val nextIndex: Int,
)

internal data class CinderellaLineProperties(
    val appearance: CinderellaAppearance,
    val dashing: Int,
    val nextIndex: Int,
)

internal object CinderellaProperties {
    // JSXGraph 1.13.3: src/reader/cinderella.js -> calculateColor.
    internal fun calculateColor(colorNumber: String): String =
        when (jsParseInt(colorNumber)) {
            0 -> "white"
            1 -> "black"
            2 -> "red"
            3 -> "blue"
            4 -> "green"
            5 -> "yellow"
            6 -> "#ffafaf"
            7 -> "cyan"
            8 -> "#ffc800"
            9 -> "#199e4e"
            10 -> "#b75500"
            11 -> "#7700b7"
            12 -> "#ff7f00"
            13 -> "#03a7bc"
            14 -> "#c10000"
            15 -> "#808080"
            16 -> "#ff4a4a"
            17 -> "#faff9e"
            18 -> "#b6ffaa"
            19 -> "#82f2ff"
            20 -> "#d4a3ff"
            21 -> "#ffbd77"
            else -> "black"
        }

    /*
     * JSXGraph 1.13.3: src/reader/cinderella.js ->
     * readPointProperties.
     */
    internal fun readPointProperties(
        dataLines: List<String>,
        startIndex: Int,
        limits: CinderellaPropertyLimits = CinderellaPropertyLimits(),
    ): GMResult<CinderellaPointProperties, CinderellaPropertyError> {
        validateInput(dataLines, startIndex, limits)?.let {
            return GMResult.Err(it)
        }
        val appearanceLine = when (
            val result = findAfter(
                dataLines = dataLines,
                startIndex = startIndex,
                property = SET_APPEARANCE,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val appearance = when (
            val result = readAppearance(
                line = dataLines[appearanceLine],
                lineIndex = appearanceLine,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val borderLine = when (
            val result = findAfter(
                dataLines = dataLines,
                startIndex = appearanceLine,
                property = POINT_BORDER,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val borderDisabled = FALSE_LITERAL in dataLines[borderLine]
        return GMResult.Ok(
            CinderellaPointProperties(
                appearance = appearance,
                nextIndex = borderLine,
                border = if (borderDisabled) "none" else "black",
                labelColor = if (borderDisabled) {
                    appearance.values.first()
                } else {
                    "black"
                },
            ),
        )
    }

    /*
     * JSXGraph 1.13.3: src/reader/cinderella.js ->
     * readCircleProperties.
     */
    internal fun readCircleProperties(
        dataLines: List<String>,
        startIndex: Int,
        limits: CinderellaPropertyLimits = CinderellaPropertyLimits(),
    ): GMResult<CinderellaCircleProperties, CinderellaPropertyError> {
        validateInput(dataLines, startIndex, limits)?.let {
            return GMResult.Err(it)
        }
        val appearanceLine = when (
            val result = findAfter(
                dataLines = dataLines,
                startIndex = startIndex,
                property = SET_APPEARANCE,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val appearance = when (
            val result = readAppearance(
                line = dataLines[appearanceLine],
                lineIndex = appearanceLine,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val colorLine = when (
            val result = findAfter(
                dataLines = dataLines,
                startIndex = appearanceLine,
                property = COLOR_FILL,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val colorNumber = QUOTED_INTEGER
            .find(dataLines[colorLine])
            ?.value
            ?.removeSurrounding("\"")
            ?: return GMResult.Err(
                CinderellaPropertyError.MalformedProperty(
                    property = COLOR_FILL,
                    lineIndex = colorLine,
                    line = dataLines[colorLine],
                ),
            )
        val opacityLine = when (
            val result = findAfter(
                dataLines = dataLines,
                startIndex = colorLine,
                property = FILL_OPACITY,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val opacityText = QUOTED_DECIMAL
            .find(dataLines[opacityLine])
            ?.value
            ?.removeSurrounding("\"")
            ?: return GMResult.Err(
                CinderellaPropertyError.MalformedProperty(
                    property = FILL_OPACITY,
                    lineIndex = opacityLine,
                    line = dataLines[opacityLine],
                ),
            )
        var opacity = jsParseFloat(opacityText)
        if (VISIBILITY_FILL in dataLines[opacityLine]) {
            opacity /= 10.0
        }
        return GMResult.Ok(
            CinderellaCircleProperties(
                appearance = appearance,
                filling = calculateColor(colorNumber),
                fillOpacity = opacity,
                nextIndex = opacityLine,
            ),
        )
    }

    /*
     * JSXGraph 1.13.3: src/reader/cinderella.js ->
     * readLineProperties.
     */
    internal fun readLineProperties(
        dataLines: List<String>,
        startIndex: Int,
        limits: CinderellaPropertyLimits = CinderellaPropertyLimits(),
    ): GMResult<CinderellaLineProperties, CinderellaPropertyError> {
        validateInput(dataLines, startIndex, limits)?.let {
            return GMResult.Err(it)
        }
        val appearanceLine = when (
            val result = findAfter(
                dataLines = dataLines,
                startIndex = startIndex,
                property = SET_APPEARANCE,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val appearance = when (
            val result = readAppearance(
                line = dataLines[appearanceLine],
                lineIndex = appearanceLine,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val dashingLine = when (
            val result = findAfter(
                dataLines = dataLines,
                startIndex = appearanceLine,
                property = LINE_DASHING,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            CinderellaLineProperties(
                appearance = appearance,
                dashing = if (FALSE_LITERAL in dataLines[dashingLine]) 0 else 3,
                nextIndex = dashingLine,
            ),
        )
    }

    private fun readAppearance(
        line: String,
        lineIndex: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<CinderellaAppearance, CinderellaPropertyError> {
        val rawValues = APPEARANCE
            .find(line)
            ?.value
            ?.removeSurrounding("(", ")")
            ?.split(",")
            ?: return GMResult.Err(
                CinderellaPropertyError.MalformedProperty(
                    property = SET_APPEARANCE,
                    lineIndex = lineIndex,
                    line = line,
                ),
            )
        if (rawValues.size > limits.maxAppearanceValues) {
            return GMResult.Err(
                CinderellaPropertyError.AppearanceLimitExceeded(
                    limit = limits.maxAppearanceValues,
                    requested = rawValues.size,
                ),
            )
        }
        val values = rawValues.toMutableList()
        values[0] = calculateColor(values[0])
        return GMResult.Ok(CinderellaAppearance(values))
    }

    private fun findAfter(
        dataLines: List<String>,
        startIndex: Int,
        property: String,
    ): GMResult<Int, CinderellaPropertyError> {
        for (index in startIndex + 1 until dataLines.size) {
            val found = when (property) {
                FILL_OPACITY ->
                    VISIBILITY_FILL in dataLines[index] ||
                        FILL_ALPHA in dataLines[index]
                else -> property in dataLines[index]
            }
            if (found) {
                return GMResult.Ok(index)
            }
        }
        return GMResult.Err(
            CinderellaPropertyError.PropertyNotFound(
                property = property,
                afterIndex = startIndex,
            ),
        )
    }

    private fun validateInput(
        dataLines: List<String>,
        startIndex: Int,
        limits: CinderellaPropertyLimits,
    ): CinderellaPropertyError? {
        val invalidLimit = when {
            limits.maxLines < 0 -> "maxLines" to limits.maxLines
            limits.maxAppearanceValues < 1 ->
                "maxAppearanceValues" to limits.maxAppearanceValues
            else -> null
        }
        if (invalidLimit != null) {
            return CinderellaPropertyError.InvalidLimits(
                name = invalidLimit.first,
                value = invalidLimit.second,
            )
        }
        if (dataLines.size > limits.maxLines) {
            return CinderellaPropertyError.LineLimitExceeded(
                limit = limits.maxLines,
                requested = dataLines.size,
            )
        }
        if (startIndex < -1 || startIndex >= dataLines.size) {
            return CinderellaPropertyError.InvalidStartIndex(
                index = startIndex,
                lineCount = dataLines.size,
            )
        }
        return null
    }

    private fun jsParseInt(value: String): Int? {
        val prefix = INTEGER_PREFIX.find(value.trimStart())?.value
            ?: return null
        return prefix.toIntOrNull()
    }

    private fun jsParseFloat(value: String): Double {
        val prefix = FLOAT_PREFIX.find(value.trimStart())?.value
            ?: return Double.NaN
        return prefix.toDoubleOrNull() ?: Double.NaN
    }

    private const val SET_APPEARANCE = "setAppearance"
    private const val POINT_BORDER = "pointborder"
    private const val COLOR_FILL = "colorfill"
    private const val VISIBILITY_FILL = "visibilityfill"
    private const val FILL_ALPHA = "fillalpha"
    private const val FILL_OPACITY = "visibilityfill|fillalpha"
    private const val LINE_DASHING = "linedashing"
    private const val FALSE_LITERAL = "false"

    private val INTEGER_PREFIX = Regex("^[+-]?\\d+")
    private val FLOAT_PREFIX =
        Regex(
            "^[+-]?(?:(?:\\d+\\.?\\d*|\\.\\d+)" +
                "(?:[eE][+-]?\\d+)?)",
        )
    private val APPEARANCE = Regex("\\([A-Za-z,0-9.]*\\)")
    private val QUOTED_INTEGER = Regex("\"[0-9]*\"")
    private val QUOTED_DECIMAL = Regex("\"[0-9.]*\"")
}
