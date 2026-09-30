/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/env.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult

internal sealed interface EnvError {
    data class InvalidPositionCoordinateCount(
        val count: Int,
    ) : EnvError

    data class InvalidTouchIndex(
        val index: Int,
        val size: Int,
    ) : EnvError
}

internal data class EnvPointer(
    val clientX: Double?,
    val clientY: Double?,
)

internal data class EnvEvent(
    val touches: List<EnvPointer?>? = null,
    val changedTouches: List<EnvPointer?>? = null,
    val pointerId: Any? = DumpUndefined,
    val isPrimary: Boolean = false,
    val clientX: Double? = null,
    val clientY: Double? = null,
)

internal data class EnvDimensions(
    val width: Double,
    val height: Double,
)

/**
 * Platform-neutral kernels from `JXG.Env`.
 *
 * DOM lookup, browser capability probes, event registration, timers, and
 * fullscreen mutation stay in platform adapters.
 */
internal object Env {
    // JSXGraph 1.13.3: src/utils/env.js -> maxScreenCoord.
    const val MAX_SCREEN_COORD: Int = 16_777_215

    // JSXGraph 1.13.3: src/utils/env.js -> isTouchEvent.
    fun isTouchEvent(event: EnvEvent): Boolean =
        event.touches != null

    // JSXGraph 1.13.3: src/utils/env.js -> isPointerEvent.
    fun isPointerEvent(event: EnvEvent): Boolean =
        Type.exists(event.pointerId)

    // JSXGraph 1.13.3: src/utils/env.js -> isMouseEvent.
    fun isMouseEvent(event: EnvEvent): Boolean =
        !isTouchEvent(event) && !isPointerEvent(event)

    // JSXGraph 1.13.3: src/utils/env.js -> getNumberOfTouchPoints.
    fun getNumberOfTouchPoints(event: EnvEvent): Int =
        event.touches?.size ?: -1

    // JSXGraph 1.13.3: src/utils/env.js -> isFirstTouch.
    fun isFirstTouch(event: EnvEvent): Boolean =
        if (isPointerEvent(event)) {
            event.isPrimary
        } else {
            getNumberOfTouchPoints(event) == 1
        }

    // JSXGraph 1.13.3: src/utils/env.js -> getPosition.
    fun getPosition(
        event: EnvEvent,
        index: Int? = null,
    ): GMResult<DoubleArray, EnvError> {
        var selected = EnvPointer(
            clientX = event.clientX,
            clientY = event.clientY,
        )
        var eventTouches = event.touches
        if (eventTouches != null && eventTouches.isEmpty()) {
            eventTouches = event.changedTouches
        }
        if (index != null && eventTouches != null) {
            selected = if (index == -1) {
                eventTouches.firstOrNull { touch -> touch != null }
                    ?: EnvPointer(null, null)
            } else {
                if (index !in eventTouches.indices) {
                    return GMResult.Err(
                        EnvError.InvalidTouchIndex(
                            index = index,
                            size = eventTouches.size,
                        ),
                    )
                }
                eventTouches[index] ?: EnvPointer(null, null)
            }
        }
        var x = 0.0
        var y = 0.0
        if (isTruthyNumber(selected.clientX)) {
            x = selected.clientX ?: Double.NaN
            y = selected.clientY ?: Double.NaN
        }
        return GMResult.Ok(doubleArrayOf(x, y))
    }

    // JSXGraph 1.13.3: src/utils/env.js -> isAndroid.
    fun isAndroid(userAgent: String?): Boolean =
        userAgent?.lowercase()?.contains("android") == true

    // JSXGraph 1.13.3: src/utils/env.js -> isWebkitAndroid.
    fun isWebkitAndroid(userAgent: String?): Boolean =
        isAndroid(userAgent) &&
            userAgent?.contains(" AppleWebKit/") == true

    // JSXGraph 1.13.3: src/utils/env.js -> isApple.
    fun isApple(userAgent: String?): Boolean =
        userAgent?.contains("iPad") == true ||
            userAgent?.contains("iPhone") == true

    // JSXGraph 1.13.3: src/utils/env.js -> isMozilla.
    fun isMozilla(userAgent: String?): Boolean {
        val normalized = userAgent?.lowercase() ?: return false
        return "mozilla" in normalized && "apple" !in normalized
    }

    // JSXGraph 1.13.3: src/utils/env.js -> isFirefoxOS.
    fun isFirefoxOS(userAgent: String?): Boolean {
        val normalized = userAgent?.lowercase() ?: return false
        return "android" !in normalized &&
            "apple" !in normalized &&
            "mobile" in normalized &&
            "mozilla" in normalized
    }

    // JSXGraph 1.13.3: src/utils/env.js -> isDesktop.
    fun isDesktop(): Boolean = true

    // JSXGraph 1.13.3: src/utils/env.js -> isMobile.
    fun isMobile(): Boolean = true

    // JSXGraph 1.13.3: src/utils/env.js -> getDimensions non-browser path.
    fun defaultDimensions(): EnvDimensions =
        EnvDimensions(width = 500.0, height = 500.0)

    // JSXGraph 1.13.3: src/utils/env.js -> getStyle inline-style path.
    fun getStyle(
        styles: Map<String, String?>,
        styleName: String,
    ): String? =
        styles[styleName.toLowerCamelCase()]

    // JSXGraph 1.13.3: src/utils/env.js -> getProp.
    fun getProp(
        styles: Map<String, String?>,
        styleName: String,
    ): Int =
        parseInt(getStyle(styles, styleName))

    // JSXGraph 1.13.3: src/utils/env.js -> getCSSTransform.
    fun getCssTransform(
        position: DoubleArray,
        styles: Map<String, String?>,
    ): GMResult<DoubleArray, EnvError> {
        if (position.size < 2) {
            return GMResult.Err(
                EnvError.InvalidPositionCoordinateCount(position.size),
            )
        }
        val transform = firstTransform(styles)
        if (transform.isNotEmpty()) {
            val values = transformValues(transform)
            when {
                transform.startsWith("matrix") -> {
                    position[0] += values.getOrNaN(4)
                    position[1] += values.getOrNaN(5)
                }
                transform.startsWith("translateX") ->
                    position[0] += values.getOrNaN(0)
                transform.startsWith("translateY") ->
                    position[1] += values.getOrNaN(0)
                transform.startsWith("translate") -> {
                    position[0] += values.getOrNaN(0)
                    position[1] += values.getOrNaN(1)
                }
            }
        }
        val zoom = styles["zoom"]
        if (zoom != null && zoom.isNotEmpty()) {
            val factor = parseFloat(zoom)
            position[0] *= factor
            position[1] *= factor
        }
        return GMResult.Ok(position)
    }

    // JSXGraph 1.13.3: src/utils/env.js -> getCSSTransformMatrix.
    fun getCssTransformMatrix(
        styles: Map<String, String?>,
    ): Array<DoubleArray> {
        val matrix = arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0),
        )
        val transform = firstTransform(styles)
        if (transform.isNotEmpty()) {
            val values = transformValues(transform)
            when {
                transform.startsWith("matrix") -> {
                    matrix[1][1] = values.getOrNaN(0)
                    matrix[1][2] = values.getOrNaN(1)
                    matrix[2][1] = values.getOrNaN(2)
                    matrix[2][2] = values.getOrNaN(3)
                }
                transform.startsWith("scaleX") ->
                    matrix[1][1] = values.getOrNaN(0)
                transform.startsWith("scaleY") ->
                    matrix[2][2] = values.getOrNaN(0)
                transform.startsWith("scale") -> {
                    matrix[1][1] = values.getOrNaN(0)
                    matrix[2][2] = values.getOrNaN(1)
                }
            }
        }
        val zoom = styles["zoom"]
        if (zoom != null && zoom.isNotEmpty()) {
            val factor = parseFloat(zoom)
            matrix[1][1] *= factor
            matrix[2][2] *= factor
        }
        return matrix
    }

    private fun firstTransform(
        styles: Map<String, String?>,
    ): String {
        for (property in TRANSFORM_PROPERTIES) {
            val value = styles[property]
            if (value != null) {
                return value
            }
        }
        return ""
    }

    private fun transformValues(transform: String): List<Double> {
        val start = transform.indexOf('(')
        if (start <= 0) {
            return emptyList()
        }
        return transform
            .substring(start + 1, transform.length - 1)
            .split(',')
            .map(::parseFloat)
    }

    private fun List<Double>.getOrNaN(index: Int): Double =
        getOrNull(index) ?: Double.NaN

    private fun String.toLowerCamelCase(): String =
        HYPHENATED_PROPERTY.replace(this) { match ->
            match.groupValues[1].uppercase()
        }

    private fun parseInt(value: String?): Int {
        val normalized = value?.trimStart() ?: return 0
        val prefix = INTEGER_PREFIX.find(normalized)?.value ?: return 0
        return prefix.toIntOrNull() ?: 0
    }

    private fun parseFloat(value: String): Double {
        val normalized = value.trimStart()
        val prefix = FLOAT_PREFIX.find(normalized)?.value
            ?: return Double.NaN
        return when (prefix) {
            "Infinity",
            "+Infinity",
            -> Double.POSITIVE_INFINITY
            "-Infinity" -> Double.NEGATIVE_INFINITY
            else -> prefix.toDoubleOrNull() ?: Double.NaN
        }
    }

    private fun isTruthyNumber(value: Double?): Boolean =
        value != null && value != 0.0 && !value.isNaN()

    private val TRANSFORM_PROPERTIES = listOf(
        "transform",
        "webkitTransform",
        "MozTransform",
        "msTransform",
        "oTransform",
    )
    private val HYPHENATED_PROPERTY = Regex("-([a-z0-9])", RegexOption.IGNORE_CASE)
    private val INTEGER_PREFIX = Regex("^[+-]?\\d+")
    private val FLOAT_PREFIX = Regex(
        "^[+-]?(?:Infinity|(?:(?:\\d+\\.?\\d*)|(?:\\.\\d+))" +
            "(?:[eE][+-]?\\d+)?)",
    )
}
