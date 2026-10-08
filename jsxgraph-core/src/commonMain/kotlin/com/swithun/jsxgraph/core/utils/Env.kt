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

    data class InvalidOffsetHierarchy(
        val reason: String,
    ) : EnvError

    data class EventTargetFailure(
        val operation: String,
        val type: String,
        val message: String?,
    ) : EnvError

    data class TimedChunkFailure(
        val processedCount: Int,
        val message: String?,
    ) : EnvError

    data class InvalidFullscreenDimensions(
        val wrapWidth: Double,
        val wrapHeight: Double,
        val innerWidth: Double,
        val innerHeight: Double,
        val scale: Double,
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

internal data class EnvRuntimeCapabilities(
    val supportsEs6: Boolean = true,
    val supportsVml: Boolean = false,
    val supportsSvg: Boolean = false,
    val supportsCanvas: Boolean = true,
    val isNode: Boolean = false,
    val isWebWorker: Boolean = false,
    val supportsPointerEvents: Boolean = true,
    val isTouchDevice: Boolean = false,
    val isWebkitApple: Boolean = false,
    val isMetroApp: Boolean = false,
)

internal data class EnvEventOptions(
    val capture: Boolean = false,
    val passive: Boolean? = null,
)

internal fun interface EnvEventHandler {
    fun invoke(event: Any?): Any?
}

internal sealed interface EnvEventTargetError {
    data class Rejected(
        val message: String?,
    ) : EnvEventTargetError
}

internal interface EnvEventTarget {
    fun addEventListener(
        type: String,
        listener: EnvEventHandler,
        options: EnvEventOptions,
    ): GMResult<Unit, EnvEventTargetError>

    fun removeEventListener(
        type: String,
        listener: EnvEventHandler,
        options: EnvEventOptions,
    ): GMResult<Unit, EnvEventTargetError>
}

internal class EnvEventOwner {
    internal val registrations =
        linkedMapOf<String, MutableList<EnvEventRegistration>>()
}

internal data class EnvEventRegistration(
    val target: EnvEventTarget?,
    val type: String,
    val origin: EnvEventHandler,
    val listener: EnvEventHandler,
    val options: EnvEventOptions,
)

internal data class EnvOffsetNode(
    val offsetLeft: Double = 0.0,
    val offsetTop: Double = 0.0,
    val scrollLeft: Double = 0.0,
    val scrollTop: Double = 0.0,
    val clientLeft: Double = 0.0,
    val clientTop: Double = 0.0,
    val styles: Map<String, String?> = emptyMap(),
    val offsetParent: EnvOffsetNode? = null,
    val parentNode: EnvOffsetNode? = null,
)

internal fun interface EnvTimeoutScheduler {
    fun schedule(
        delayMilliseconds: Long,
        task: () -> Unit,
    )
}

internal fun interface EnvClock {
    fun nowMilliseconds(): Long
}

internal data class EnvFullscreenLayout(
    val width: Double,
    val height: Double,
    val scale: Double,
    val verticalShift: Double,
    val transform: String,
)

/**
 * Platform-neutral kernels from `JXG.Env`.
 *
 * DOM lookup and mutation stay in platform adapters. Capability probes,
 * event registration, offset walking, timers, and fullscreen geometry keep
 * their upstream semantics through explicit commonMain contracts.
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

    // JSXGraph 1.13.3: src/utils/env.js -> supportsES6.
    fun supportsES6(
        capabilities: EnvRuntimeCapabilities = EnvRuntimeCapabilities(),
    ): Boolean = capabilities.supportsEs6

    // JSXGraph 1.13.3: src/utils/env.js -> supportsVML.
    fun supportsVML(
        capabilities: EnvRuntimeCapabilities = EnvRuntimeCapabilities(),
    ): Boolean = capabilities.supportsVml

    // JSXGraph 1.13.3: src/utils/env.js -> supportsSVG.
    fun supportsSVG(
        capabilities: EnvRuntimeCapabilities = EnvRuntimeCapabilities(),
    ): Boolean = capabilities.supportsSvg

    // JSXGraph 1.13.3: src/utils/env.js -> supportsCanvas.
    fun supportsCanvas(
        capabilities: EnvRuntimeCapabilities = EnvRuntimeCapabilities(),
    ): Boolean = capabilities.supportsCanvas

    // JSXGraph 1.13.3: src/utils/env.js -> isNode.
    fun isNode(
        capabilities: EnvRuntimeCapabilities = EnvRuntimeCapabilities(),
    ): Boolean = capabilities.isNode

    // JSXGraph 1.13.3: src/utils/env.js -> isWebWorker.
    fun isWebWorker(
        capabilities: EnvRuntimeCapabilities = EnvRuntimeCapabilities(),
    ): Boolean = capabilities.isWebWorker

    // JSXGraph 1.13.3: src/utils/env.js -> supportsPointerEvents.
    fun supportsPointerEvents(
        capabilities: EnvRuntimeCapabilities = EnvRuntimeCapabilities(),
    ): Boolean = capabilities.supportsPointerEvents

    // JSXGraph 1.13.3: src/utils/env.js -> isTouchDevice.
    fun isTouchDevice(
        capabilities: EnvRuntimeCapabilities = EnvRuntimeCapabilities(),
    ): Boolean = capabilities.isTouchDevice

    // JSXGraph 1.13.3: src/utils/env.js -> isWebkitApple.
    fun isWebkitApple(
        capabilities: EnvRuntimeCapabilities = EnvRuntimeCapabilities(),
    ): Boolean = capabilities.isWebkitApple

    // JSXGraph 1.13.3: src/utils/env.js -> isMetroApp.
    fun isMetroApp(
        capabilities: EnvRuntimeCapabilities = EnvRuntimeCapabilities(),
    ): Boolean = capabilities.isMetroApp

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

    // JSXGraph 1.13.3: src/utils/env.js -> addEvent.
    fun addEvent(
        target: EnvEventTarget?,
        type: String,
        handler: EnvEventHandler,
        owner: EnvEventOwner,
        options: EnvEventOptions = EnvEventOptions(),
    ): GMResult<EnvEventRegistration, EnvError> {
        val listener = EnvEventHandler { event -> handler.invoke(event) }
        val registration = EnvEventRegistration(
            target = target,
            type = type,
            origin = handler,
            listener = listener,
            options = options,
        )
        owner.registrations.getOrPut(type, ::mutableListOf).add(registration)
        if (target == null) {
            return GMResult.Ok(registration)
        }
        return when (
            val added = target.addEventListener(type, listener, options)
        ) {
            is GMResult.Ok -> GMResult.Ok(registration)
            is GMResult.Err -> {
                owner.registrations[type]?.remove(registration)
                GMResult.Err(
                    EnvError.EventTargetFailure(
                        operation = "addEventListener",
                        type = type,
                        message = added.error.message(),
                    ),
                )
            }
        }
    }

    // JSXGraph 1.13.3: src/utils/env.js -> removeEvent.
    fun removeEvent(
        target: EnvEventTarget?,
        type: String,
        handler: EnvEventHandler,
        owner: EnvEventOwner,
    ): GMResult<Boolean, EnvError> {
        val registrations = owner.registrations[type]
            ?: return GMResult.Ok(false)
        val index = registrations.indexOfFirst { registration ->
            registration.origin === handler
        }
        if (index < 0) {
            return GMResult.Ok(false)
        }
        val registration = registrations[index]
        val registeredTarget = registration.target ?: target
        if (registeredTarget != null) {
            when (
                val removed = registeredTarget.removeEventListener(
                    type = type,
                    listener = registration.listener,
                    options = registration.options,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> {
                    return GMResult.Err(
                        EnvError.EventTargetFailure(
                            operation = "removeEventListener",
                            type = type,
                            message = removed.error.message(),
                        ),
                    )
                }
            }
        }
        registrations.removeAt(index)
        if (registrations.isEmpty()) {
            owner.registrations.remove(type)
        }
        return GMResult.Ok(true)
    }

    // JSXGraph 1.13.3: src/utils/env.js -> removeAllEvents.
    fun removeAllEvents(
        target: EnvEventTarget?,
        type: String,
        owner: EnvEventOwner,
    ): GMResult<Int, EnvError> {
        val handlers = owner.registrations[type]
            ?.map(EnvEventRegistration::origin)
            ?.asReversed()
            .orEmpty()
        var removedCount = 0
        for (handler in handlers) {
            when (val removed = removeEvent(target, type, handler, owner)) {
                is GMResult.Ok -> {
                    if (removed.value) {
                        removedCount += 1
                    }
                }
                is GMResult.Err -> return removed
            }
        }
        return GMResult.Ok(removedCount)
    }

    // JSXGraph 1.13.3: src/utils/env.js -> getOffset.
    fun getOffset(
        node: EnvOffsetNode,
        maximumHierarchyDepth: Int = 1_024,
    ): GMResult<DoubleArray, EnvError> {
        var offsetNode: EnvOffsetNode? = node
        var parentNode: EnvOffsetNode? = node
        var left = node.offsetLeft - node.scrollLeft
        var top = node.offsetTop - node.scrollTop
        var traversed = 0

        val initial = doubleArrayOf(left, top)
        when (val transformed = getCssTransform(initial, node.styles)) {
            is GMResult.Ok -> {
                left = transformed.value[0]
                top = transformed.value[1]
            }
            is GMResult.Err -> return transformed
        }

        offsetNode = offsetNode?.offsetParent
        while (offsetNode != null) {
            traversed += 1
            if (traversed > maximumHierarchyDepth) {
                return GMResult.Err(
                    EnvError.InvalidOffsetHierarchy(
                        "Offset hierarchy exceeds $maximumHierarchyDepth nodes.",
                    ),
                )
            }
            left += offsetNode.offsetLeft
            top += offsetNode.offsetTop
            if (offsetNode.offsetParent != null) {
                left += offsetNode.clientLeft - offsetNode.scrollLeft
                top += offsetNode.clientTop - offsetNode.scrollTop
            }

            val offsetPosition = doubleArrayOf(left, top)
            when (
                val transformed =
                    getCssTransform(offsetPosition, offsetNode.styles)
            ) {
                is GMResult.Ok -> {
                    left = transformed.value[0]
                    top = transformed.value[1]
                }
                is GMResult.Err -> return transformed
            }

            parentNode = parentNode?.parentNode
            while (parentNode !== offsetNode) {
                traversed += 1
                if (
                    parentNode == null ||
                    traversed > maximumHierarchyDepth
                ) {
                    return GMResult.Err(
                        EnvError.InvalidOffsetHierarchy(
                            "parentNode chain does not reach offsetParent.",
                        ),
                    )
                }
                left += parentNode.clientLeft - parentNode.scrollLeft
                top += parentNode.clientTop - parentNode.scrollTop
                val parentPosition = doubleArrayOf(left, top)
                when (
                    val transformed =
                        getCssTransform(parentPosition, parentNode.styles)
                ) {
                    is GMResult.Ok -> {
                        left = transformed.value[0]
                        top = transformed.value[1]
                    }
                    is GMResult.Err -> return transformed
                }
                parentNode = parentNode.parentNode
            }
            offsetNode = offsetNode.offsetParent
        }
        return GMResult.Ok(doubleArrayOf(left, top))
    }

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

    // JSXGraph 1.13.3: src/utils/env.js -> timedChunk.
    fun <T> timedChunk(
        items: List<T>,
        process: (T) -> Unit,
        scheduler: EnvTimeoutScheduler,
        clock: EnvClock,
        callback: (GMResult<List<T>, EnvError>) -> Unit,
        chunkBudgetMilliseconds: Long = 300L,
    ) {
        val todo = ArrayDeque(items)
        var processedCount = 0
        lateinit var timerFunction: () -> Unit
        timerFunction = timer@{
            val start = clock.nowMilliseconds()
            try {
                do {
                    if (todo.isNotEmpty()) {
                        val item = todo.removeFirst()
                        process(item)
                        processedCount += 1
                    }
                } while (
                    todo.isNotEmpty() &&
                    clock.nowMilliseconds() - start <
                    chunkBudgetMilliseconds
                )
            } catch (cause: Exception) {
                callback(
                    GMResult.Err(
                        EnvError.TimedChunkFailure(
                            processedCount = processedCount,
                            message = cause.message,
                        ),
                    ),
                )
                return@timer
            }
            if (todo.isNotEmpty()) {
                scheduler.schedule(1L, timerFunction)
            } else {
                callback(GMResult.Ok(items))
            }
        }
        scheduler.schedule(1L, timerFunction)
    }

    // JSXGraph 1.13.3: src/utils/env.js -> scaleJSXGraphDiv.
    fun scaleJSXGraphDiv(
        wrap: EnvDimensions,
        inner: EnvDimensions,
        scale: Double,
        originalTransform: String = "",
    ): GMResult<EnvFullscreenLayout, EnvError> {
        if (
            !wrap.width.isFinite() ||
            !wrap.height.isFinite() ||
            !inner.width.isFinite() ||
            !inner.height.isFinite() ||
            !scale.isFinite() ||
            wrap.width <= 0.0 ||
            wrap.height <= 0.0 ||
            inner.width <= 0.0 ||
            inner.height <= 0.0 ||
            scale <= 0.0
        ) {
            return GMResult.Err(
                EnvError.InvalidFullscreenDimensions(
                    wrapWidth = wrap.width,
                    wrapHeight = wrap.height,
                    innerWidth = inner.width,
                    innerHeight = inner.height,
                    scale = scale,
                ),
            )
        }

        var width = inner.width
        var height = inner.height
        val ratio = height / width
        if (width > wrap.width * scale) {
            width = wrap.width * scale
            height = width * ratio
        }
        if (height > wrap.height * scale) {
            height = wrap.height * scale
            width = height / ratio
        }
        val layoutScale = if (ratio < wrap.height / wrap.width) {
            wrap.width * scale / width
        } else {
            wrap.height * scale / height
        }
        val verticalShift = (wrap.height - height) * 0.5
        val separator =
            if (originalTransform.isEmpty()) "" else " "
        val transform =
            originalTransform + separator +
                "matrix($layoutScale,0,0,$layoutScale,0,$verticalShift)"
        return GMResult.Ok(
            EnvFullscreenLayout(
                width = width,
                height = height,
                scale = layoutScale,
                verticalShift = verticalShift,
                transform = transform,
            ),
        )
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

    private fun EnvEventTargetError.message(): String? =
        when (this) {
            is EnvEventTargetError.Rejected -> message
        }

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
