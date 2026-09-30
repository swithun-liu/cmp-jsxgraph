/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/renderer/no.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.renderer

/**
 * Renderer implementation for environments that intentionally draw nothing.
 *
 * Parameters stay opaque because the production renderer boundary is the
 * platform-independent scene model rather than JSXGraph DOM nodes.
 */
internal class NoRenderer {
    internal val enhancedRendering: Boolean = false
    internal val type: String = "no"

    internal fun drawPoint(element: Any?) = Unit
    internal fun updatePoint(element: Any?) = Unit
    internal fun changePointStyle(element: Any?) = Unit
    internal fun drawLine(element: Any?) = Unit
    internal fun updateLine(element: Any?) = Unit
    internal fun drawTicks(element: Any?) = Unit
    internal fun updateTicks(element: Any?) = Unit
    internal fun drawCurve(element: Any?) = Unit
    internal fun updateCurve(element: Any?) = Unit
    internal fun drawEllipse(element: Any?) = Unit
    internal fun updateEllipse(element: Any?) = Unit
    internal fun drawPolygon(element: Any?) = Unit
    internal fun updatePolygon(element: Any?) = Unit

    internal fun displayCopyright(
        text: String,
        fontSize: Double,
    ) = Unit

    internal fun drawInternalText(element: Any?) = Unit
    internal fun updateInternalText(element: Any?) = Unit
    internal fun drawText(element: Any?) = Unit
    internal fun updateText(element: Any?) = Unit

    internal fun updateTextStyle(
        element: Any?,
        doHighlight: Boolean,
    ) = Unit

    internal fun updateInternalTextStyle(
        element: Any?,
        strokeColor: String,
        strokeOpacity: Double,
    ) = Unit

    internal fun drawImage(element: Any?) = Unit
    internal fun updateImage(element: Any?) = Unit
    internal fun updateImageURL(element: Any?) = Unit

    internal fun appendChildPrim(
        node: Any?,
        level: Int,
    ) = Unit

    internal fun appendNodesToElement(
        element: Any?,
        type: String,
    ) = Unit

    internal fun remove(node: Any?) = Unit
    internal fun makeArrows(element: Any?) = Unit

    internal fun updateEllipsePrim(
        node: Any?,
        x: Double,
        y: Double,
        radiusX: Double,
        radiusY: Double,
    ) = Unit

    internal fun updateLinePrim(
        node: Any?,
        point1X: Double,
        point1Y: Double,
        point2X: Double,
        point2Y: Double,
        board: Any?,
    ) = Unit

    internal fun updatePathPrim(
        node: Any?,
        pathString: String,
        board: Any?,
    ) = Unit

    internal fun updatePathStringPoint(
        element: Any?,
        size: Double,
        type: String,
    ) = Unit

    internal fun updatePathStringPrim(element: Any?) = Unit
    internal fun updatePathStringBezierPrim(element: Any?) = Unit

    internal fun updatePolygonPrim(
        node: Any?,
        element: Any?,
    ) = Unit

    internal fun updateRectPrim(
        node: Any?,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
    ) = Unit

    internal fun setPropertyPrim(
        node: Any?,
        key: String,
        value: Any?,
    ) = Unit

    internal fun show(element: Any?) = Unit
    internal fun hide(element: Any?) = Unit

    internal fun setBuffering(
        node: Any?,
        type: String,
    ) = Unit

    internal fun setDashStyle(element: Any?) = Unit
    internal fun setDraft(element: Any?) = Unit
    internal fun removeDraft(element: Any?) = Unit
    internal fun setGradient(element: Any?) = Unit
    internal fun updateGradient(element: Any?) = Unit

    internal fun setObjectTransition(
        element: Any?,
        duration: Double,
    ) = Unit

    internal fun setObjectFillColor(
        element: Any?,
        color: String,
        opacity: Double,
    ) = Unit

    internal fun setObjectStrokeColor(
        element: Any?,
        color: String,
        opacity: Double,
    ) = Unit

    internal fun setObjectStrokeWidth(
        element: Any?,
        width: Double,
    ) = Unit

    internal fun setShadow(element: Any?) = Unit
    internal fun highlight(element: Any?) = Unit
    internal fun noHighlight(element: Any?) = Unit
    internal fun suspendRedraw() = Unit
    internal fun unsuspendRedraw() = Unit
    internal fun drawNavigationBar(board: Any?) = Unit
    internal fun getElementById(id: String): Nothing? = null
    internal fun resize(width: Double, height: Double) = Unit
    internal fun removeToInsertLater(): () -> Unit = {}
}
