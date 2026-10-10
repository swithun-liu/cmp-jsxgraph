/*
 * Kotlin translation support for JSXGraph.
 * Upstream: src/options.js -> Board navigation/navbar options,
 * src/renderer/abstract.js -> drawNavigationBar
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

/**
 * One command exposed by JSXGraph's navigation bar.
 *
 * The enum order is not the visual order. [JsxGraphNavigationBar.controls]
 * retains the exact order emitted by JSXGraph 1.13.3.
 */
enum class JsxGraphNavigationAction {
    FULLSCREEN,
    SCREENSHOT,
    RELOAD,
    CLEAR_TRACES,
    ZOOM_OUT,
    ZOOM_RESET,
    ZOOM_IN,
    PAN_LEFT,
    PAN_DOWN,
    PAN_UP,
    PAN_RIGHT,
}

enum class JsxGraphNavigationHostAction {
    FULLSCREEN,
    SCREENSHOT,
    RELOAD,
}

data class JsxGraphNavigationControl(
    val action: JsxGraphNavigationAction,
    val upstreamType: String,
    val symbol: String,
)

/**
 * Portable counterpart of `JXG.Options.navbar`.
 *
 * JSXGraph stores these values as CSS strings. The common model resolves the
 * portable color and pixel subset before Compose consumes it.
 */
data class JsxGraphNavigationBarStyle(
    val strokeColor: JsxGraphColor = JsxGraphColor(51, 51, 51),
    val fillColor: JsxGraphColor = JsxGraphColor.Transparent,
    val highlightFillColor: JsxGraphColor = JsxGraphColor(170, 170, 170),
    val paddingPixels: Double = 2.0,
    val fontSizePixels: Double = 14.0,
    val zIndex: Int = 100,
    val rightPixels: Double = 5.0,
    val bottomPixels: Double = 5.0,
)

data class JsxGraphNavigationBar(
    val controls: List<JsxGraphNavigationControl>,
    val style: JsxGraphNavigationBarStyle = JsxGraphNavigationBarStyle(),
)

data class JsxGraphNavigationInteractionResult(
    val scene: JsxGraphScene,
    val hostAction: JsxGraphNavigationHostAction? = null,
)

internal data class JsxGraphNavigationConfiguration(
    val showNavigation: Boolean,
    val showZoom: Boolean = true,
    val showClearTraces: Boolean = false,
    val showFullscreen: Boolean = false,
    val showScreenshot: Boolean = false,
    val showReload: Boolean = false,
    val fullscreenSymbol: String = "\u26f6",
    val screenshotSymbol: String = "\u2318",
    val style: JsxGraphNavigationBarStyle = JsxGraphNavigationBarStyle(),
)

/**
 * JSXGraph 1.13.3: src/renderer/abstract.js -> drawNavigationBar.
 */
internal fun JsxGraphNavigationConfiguration.toNavigationBar():
    JsxGraphNavigationBar? {
    val controls = buildList {
        if (showFullscreen) {
            add(
                JsxGraphNavigationControl(
                    action = JsxGraphNavigationAction.FULLSCREEN,
                    upstreamType = "fullscreen",
                    symbol = fullscreenSymbol,
                ),
            )
        }
        if (showScreenshot) {
            add(
                JsxGraphNavigationControl(
                    action = JsxGraphNavigationAction.SCREENSHOT,
                    upstreamType = "screenshot",
                    symbol = screenshotSymbol,
                ),
            )
        }
        if (showReload) {
            add(
                JsxGraphNavigationControl(
                    action = JsxGraphNavigationAction.RELOAD,
                    upstreamType = "reload",
                    symbol = "\u21bb",
                ),
            )
        }
        if (showClearTraces) {
            add(
                JsxGraphNavigationControl(
                    action = JsxGraphNavigationAction.CLEAR_TRACES,
                    upstreamType = "cleartraces",
                    symbol = "\u2297",
                ),
            )
        }
        if (showNavigation) {
            if (showZoom) {
                add(
                    JsxGraphNavigationControl(
                        action = JsxGraphNavigationAction.ZOOM_OUT,
                        upstreamType = "out",
                        symbol = "\u2013",
                    ),
                )
                add(
                    JsxGraphNavigationControl(
                        action = JsxGraphNavigationAction.ZOOM_RESET,
                        upstreamType = "100",
                        symbol = "o",
                    ),
                )
                add(
                    JsxGraphNavigationControl(
                        action = JsxGraphNavigationAction.ZOOM_IN,
                        upstreamType = "in",
                        symbol = "+",
                    ),
                )
            }
            add(
                JsxGraphNavigationControl(
                    action = JsxGraphNavigationAction.PAN_LEFT,
                    upstreamType = "left",
                    symbol = "\u2190",
                ),
            )
            add(
                JsxGraphNavigationControl(
                    action = JsxGraphNavigationAction.PAN_DOWN,
                    upstreamType = "down",
                    symbol = "\u2193",
                ),
            )
            add(
                JsxGraphNavigationControl(
                    action = JsxGraphNavigationAction.PAN_UP,
                    upstreamType = "up",
                    symbol = "\u2191",
                ),
            )
            add(
                JsxGraphNavigationControl(
                    action = JsxGraphNavigationAction.PAN_RIGHT,
                    upstreamType = "right",
                    symbol = "\u2192",
                ),
            )
        }
    }
    return controls.takeIf(List<*>::isNotEmpty)?.let {
        JsxGraphNavigationBar(
            controls = it,
            style = style,
        )
    }
}
