/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/renderer/no.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.renderer

import com.swithun.jsxgraph.core.JsxGraphRendererAdapter
import com.swithun.jsxgraph.core.base.Board

/**
 * Renderer implementation for environments that intentionally draw nothing.
 *
 * Parameters stay opaque because the production renderer boundary is the
 * platform-independent scene model rather than JSXGraph DOM nodes.
 */
// JSXGraph 1.13.3: src/renderer/no.js -> NoRenderer.
internal class NoRenderer : JsxGraphRendererAdapter {
    internal val enhancedRendering: Boolean = false
    override val type: String = "no"

    override fun clear() = Unit

    override fun free() = Unit

    override fun drawNavigationBar(
        board: Board,
        attributes: Map<String, Any?>,
    ) = Unit

    // JSXGraph 1.13.3: src/renderer/no.js ->
    // drawPoint, updatePoint, changePointStyle, drawLine, updateLine,
    // drawTicks, updateTicks, drawCurve, updateCurve, drawEllipse,
    // updateEllipse, drawPolygon, updatePolygon.
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

    // JSXGraph 1.13.3: src/renderer/no.js ->
    // displayCopyright, drawInternalText, updateInternalText, drawText,
    // updateText, updateTextStyle, updateInternalTextStyle.
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

    // JSXGraph 1.13.3: src/renderer/no.js ->
    // drawImage, updateImage, updateImageURL, appendChildPrim,
    // appendNodesToElement, remove, makeArrows.
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

    // JSXGraph 1.13.3: src/renderer/no.js ->
    // updateEllipsePrim, updateLinePrim, updatePathPrim,
    // updatePathStringPoint, updatePathStringPrim,
    // updatePathStringBezierPrim, updatePolygonPrim, updateRectPrim,
    // setPropertyPrim.
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

    // JSXGraph 1.13.3: src/renderer/no.js ->
    // show, hide, setBuffering, setDashStyle, setDraft, removeDraft,
    // setGradient, updateGradient, setObjectTransition.
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

    // JSXGraph 1.13.3: src/renderer/no.js ->
    // setObjectFillColor, setObjectStrokeColor, setObjectStrokeWidth,
    // setShadow, highlight, noHighlight.
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

    // JSXGraph 1.13.3: src/renderer/no.js ->
    // suspendRedraw, unsuspendRedraw, drawNavigationBar, getElementById,
    // resize, removeToInsertLater.
    internal fun suspendRedraw() = Unit
    internal fun unsuspendRedraw() = Unit
    internal fun drawNavigationBar(board: Any?) = Unit
    internal fun getElementById(id: String): Nothing? = null
    internal fun resize(width: Double, height: Double) = Unit
    internal fun removeToInsertLater(): () -> Unit = {}
}
