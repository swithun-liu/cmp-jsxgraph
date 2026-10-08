/*
 * Kotlin translation of JSXGraph.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.renderer

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphArrowHead
import com.swithun.jsxgraph.core.JsxGraphBoundingBox
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphElementStyle
import com.swithun.jsxgraph.core.JsxGraphFillGradient
import com.swithun.jsxgraph.core.JsxGraphPoint2D
import com.swithun.jsxgraph.core.JsxGraphRendererAdapter
import com.swithun.jsxgraph.core.JsxGraphScene
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import com.swithun.jsxgraph.core.JsxGraphShadow
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.resolveVisualStyle
import com.swithun.jsxgraph.core.utils.EnvDimensions
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

internal sealed interface NativeRendererError {
    data class InvalidDimensions(
        val width: Double,
        val height: Double,
    ) : NativeRendererError

    data class InvalidNumber(
        val operation: String,
        val value: Double,
    ) : NativeRendererError

    data class InvalidPrimitive(
        val primitive: String,
    ) : NativeRendererError

    data class MissingNode(
        val id: String,
    ) : NativeRendererError

    data class RemovedNode(
        val id: String,
    ) : NativeRendererError

    data class InvalidTouchpointIndex(
        val index: Int,
    ) : NativeRendererError

    data class UnsupportedCapability(
        val renderer: String,
        val capability: String,
    ) : NativeRendererError

    data object RendererFreed : NativeRendererError
}

internal data class NativeRendererBounds(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
)

internal sealed interface NativeRendererGeometry {
    data object Empty : NativeRendererGeometry

    data class Point(
        val position: JsxGraphPoint2D,
        val size: Double,
        val face: String,
    ) : NativeRendererGeometry

    data class Line(
        val point1: JsxGraphPoint2D,
        val point2: JsxGraphPoint2D,
        val straightFirst: Boolean,
        val straightLast: Boolean,
    ) : NativeRendererGeometry

    data class Ellipse(
        val center: JsxGraphPoint2D,
        val radiusX: Double,
        val radiusY: Double,
    ) : NativeRendererGeometry

    data class Rect(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
    ) : NativeRendererGeometry

    data class Path(
        val subpaths: List<List<JsxGraphPoint2D>>,
        val bezierDegree: Int,
        val handDrawing: Boolean,
    ) : NativeRendererGeometry

    data class Polygon(
        val vertices: List<JsxGraphPoint2D>,
        val closed: Boolean,
    ) : NativeRendererGeometry

    data class Text(
        val position: JsxGraphPoint2D,
        val content: String,
        val fontSize: Double,
        val anchorX: String,
        val anchorY: String,
    ) : NativeRendererGeometry

    data class Image(
        val source: String,
        val anchor: JsxGraphPoint2D,
        val widthVector: JsxGraphPoint2D,
        val heightVector: JsxGraphPoint2D,
    ) : NativeRendererGeometry

    data class ForeignObject(
        val content: String,
        val anchor: JsxGraphPoint2D,
    ) : NativeRendererGeometry
}

internal data class NativeRendererVisualState(
    val visible: Boolean = true,
    val strokeColor: JsxGraphColor = JsxGraphColor.Transparent,
    val fillColor: JsxGraphColor = JsxGraphColor.Transparent,
    val strokeWidth: Double = 0.0,
    val strokeOpacity: Double = 1.0,
    val fillOpacity: Double = 1.0,
    val layer: Int = 0,
    val dashPattern: List<Double> = emptyList(),
    val lineCap: String = "round",
    val clipToViewport: Boolean = true,
    val cssClass: String = "",
    val aria: Map<String, String> = emptyMap(),
    val tabindex: Int? = null,
    val buffering: String = "auto",
    val transitionDuration: Double = 0.0,
    val transitionProperties: List<String> = emptyList(),
    val shadow: JsxGraphShadow = JsxGraphShadow(),
    val gradient: JsxGraphFillGradient? = null,
    val draft: Boolean = false,
    val highlighted: Boolean = false,
)

internal data class NativeRendererTouchpoint(
    val position: JsxGraphPoint2D = JsxGraphPoint2D(0.0, 0.0),
    val visible: Boolean = false,
)

internal data class NativeRendererNodeSnapshot(
    val id: String,
    val primitive: String,
    val geometry: NativeRendererGeometry,
    val visual: NativeRendererVisualState,
    val properties: Map<String, Any?>,
    val firstArrow: JsxGraphArrowHead?,
    val lastArrow: JsxGraphArrowHead?,
    val transform: Array<DoubleArray>?,
)

internal data class NativeRendererSnapshot(
    val type: String,
    val containerId: String,
    val width: Double,
    val height: Double,
    val nodes: List<NativeRendererNodeSnapshot>,
    val touchpoints: List<NativeRendererTouchpoint>,
    val navigationAttributes: Map<String, Any?>,
    val redrawSuspended: Boolean,
)

internal class NativeRendererNode(
    internal val id: String,
    internal var primitive: String,
) {
    internal var geometry: NativeRendererGeometry =
        NativeRendererGeometry.Empty
    internal var visual: NativeRendererVisualState =
        NativeRendererVisualState()
    internal val properties = linkedMapOf<String, Any?>()
    internal var firstArrow: JsxGraphArrowHead? = null
    internal var lastArrow: JsxGraphArrowHead? = null
    internal var transform: Array<DoubleArray>? = null
    internal var removed: Boolean = false

    internal fun snapshot(): NativeRendererNodeSnapshot =
        NativeRendererNodeSnapshot(
            id = id,
            primitive = primitive,
            geometry = geometry,
            visual = visual,
            properties = properties.toMap(),
            firstArrow = firstArrow,
            lastArrow = lastArrow,
            transform = transform?.map(DoubleArray::copyOf)?.toTypedArray(),
        )
}

/**
 * Platform-neutral renderer state shared by the Compose Canvas backends.
 *
 * Browser DOM nodes are translated to [NativeRendererNode] records. Actual
 * drawing remains in jsxgraph-compose, while this class preserves JSXGraph's
 * primitive lifecycle and renderer mutation semantics without a JS runtime.
 */
// JSXGraph 1.13.3: src/renderer/abstract.js ->
// AbstractRenderer, _updateVisual, _getHighlighted, drawPoint, updatePoint,
// changePointStyle, drawLine, updateLine, drawCurve, updateCurve.
// JSXGraph 1.13.3: src/renderer/abstract.js ->
// updatePathWithArrowHeads, updateLineWithEndings, updatePath,
// handleTouchpoints, setArrowSize, setLineCap, drawTicks, updateTicks.
// JSXGraph 1.13.3: src/renderer/abstract.js ->
// drawEllipse, updateEllipse, displayCopyright, displayLogo,
// drawInternalText, updateInternalText, drawText, updateClipPath.
// JSXGraph 1.13.3: src/renderer/abstract.js ->
// updateText, updateTextStyle, updateInternalTextStyle, drawImage,
// updateImage, joinTransforms, transformRect, updateImageURL.
// JSXGraph 1.13.3: src/renderer/abstract.js ->
// updateImageStyle, drawForeignObject, updateForeignObject,
// appendChildPrim, appendNodesToElement, createPrim, remove, makeArrows.
// JSXGraph 1.13.3: src/renderer/abstract.js ->
// _setArrowWidth, updateEllipsePrim, updateLinePrim, updatePathPrim,
// updatePathStringPrim, updatePathStringBezierPrim, updateRectPrim.
// JSXGraph 1.13.3: src/renderer/abstract.js ->
// display, hide, highlight, noHighlight, removeDraft, setARIA,
// setBuffering, setClipPath, setCssClass, setDashStyle, setDraft.
// JSXGraph 1.13.3: src/renderer/abstract.js ->
// setGradient, setLayer, setObjectFillColor, setObjectStrokeColor,
// setObjectStrokeWidth, setObjectTransition, setPropertyPrim, setShadow.
// JSXGraph 1.13.3: src/renderer/abstract.js ->
// setTabindex, show, updateGradient, suspendRedraw, unsuspendRedraw,
// drawNavigationBar, getElementById, removeToInsertLater, resize.
// JSXGraph 1.13.3: src/renderer/abstract.js ->
// createTouchpoints, showTouchpoint, hideTouchpoint, updateTouchpoint,
// dumpToDataURI, dumpToCanvas, screenshot.
internal open class AbstractRenderer(
    final override val type: String,
    internal val containerId: String,
    dimensions: EnvDimensions,
) : JsxGraphRendererAdapter {
    internal var enhancedRendering: Boolean = true
    internal var supportsForeignObject: Boolean = true
        private set

    internal val dashArray: List<List<Double>> = listOf(
        listOf(2.0, 2.0),
        listOf(5.0, 5.0),
        listOf(10.0, 10.0),
        listOf(20.0, 20.0),
        listOf(20.0, 10.0, 10.0, 10.0),
        listOf(20.0, 5.0, 10.0, 5.0),
        listOf(0.0, 5.0),
    )

    private val nodes = linkedMapOf<String, NativeRendererNode>()
    private val layerOrder = linkedMapOf<Int, MutableList<String>>()
    private val touchpoints = mutableListOf<NativeRendererTouchpoint>()
    private var width = dimensions.width
    private var height = dimensions.height
    private var navigationAttributes: Map<String, Any?> = emptyMap()
    private var redrawSuspended = false
    private var freed = false
    private var generatedNodeId = 0L

    internal fun render(
        scene: JsxGraphScene,
    ): GMResult<NativeRendererSnapshot, NativeRendererError> {
        if (freed) {
            return GMResult.Err(NativeRendererError.RendererFreed)
        }
        clear()
        suspendRedraw()
        for (
            element in scene.elements
                .withIndex()
                .sortedWith(
                    compareBy<IndexedValue<JsxGraphSceneElement>>(
                        { indexed -> indexed.value.style.layer },
                        { indexed -> indexed.index },
                    ),
                )
                .map(IndexedValue<JsxGraphSceneElement>::value)
        ) {
            val result = when (element) {
                is JsxGraphSceneElement.Point -> drawPoint(element)
                is JsxGraphSceneElement.Line -> drawLine(element)
                is JsxGraphSceneElement.Ticks -> drawTicks(element)
                is JsxGraphSceneElement.Circle -> drawEllipse(element)
                is JsxGraphSceneElement.Curve -> drawCurve(element)
                is JsxGraphSceneElement.Polygon -> drawPolygon(element)
                is JsxGraphSceneElement.Text -> drawText(element)
                is JsxGraphSceneElement.Image -> drawImage(element)
                is JsxGraphSceneElement.ForeignObject ->
                    drawForeignObject(element)
            }
            if (result is GMResult.Err) {
                redrawSuspended = false
                return result
            }
        }
        unsuspendRedraw()
        return GMResult.Ok(snapshot())
    }

    internal fun snapshot(): NativeRendererSnapshot =
        NativeRendererSnapshot(
            type = type,
            containerId = containerId,
            width = width,
            height = height,
            nodes = layerOrder.entries
                .sortedBy { it.key }
                .map { it.value }
                .flatten()
                .mapNotNull { id -> nodes[id] }
                .filterNot(NativeRendererNode::removed)
                .map(NativeRendererNode::snapshot),
            touchpoints = touchpoints.toList(),
            navigationAttributes = navigationAttributes,
            redrawSuspended = redrawSuspended,
        )

    internal fun _updateVisual(
        node: NativeRendererNode,
        style: JsxGraphElementStyle,
        lineCap: String = style.lineCap,
        not: Set<String> = emptySet(),
        enhanced: Boolean = false,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            if (!enhancedRendering && !enhanced) {
                return@mutate
            }
            val resolved = style.resolveVisualStyle()
            val current = it.visual
            it.visual = current.copy(
                visible = style.visible,
                strokeColor =
                    if ("stroke" in not) current.strokeColor
                    else resolved.strokeColor,
                fillColor =
                    if ("fill" in not) current.fillColor
                    else resolved.fillColor,
                strokeWidth =
                    if ("stroke" in not) current.strokeWidth
                    else resolved.strokeWidth,
                strokeOpacity =
                    if ("stroke" in not) current.strokeOpacity
                    else resolved.strokeOpacity,
                fillOpacity =
                    if ("fill" in not) current.fillOpacity
                    else resolved.fillOpacity,
                layer = style.layer,
                dashPattern =
                    if ("dash" in not) current.dashPattern
                    else style.strokeDashPattern,
                lineCap = normalizeLineCap(lineCap),
                clipToViewport = style.clipToViewport,
                cssClass = resolved.cssClass,
                aria = style.aria,
                tabindex = if (style.visible) style.tabIndex else null,
                transitionDuration = style.transitionDurationMillis,
                transitionProperties = style.transitionProperties,
                shadow =
                    if ("shadow" in not) current.shadow
                    else style.shadow,
                gradient =
                    if ("gradient" in not) current.gradient
                    else style.fillGradient,
                draft = resolved.isDraft,
                highlighted = resolved.isHighlighted,
            )
        }

    internal fun _getHighlighted(
        node: NativeRendererNode,
    ): String = if (node.visual.highlighted) "highlight" else ""

    internal fun drawPoint(
        element: JsxGraphSceneElement.Point,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        val primitive = when (normalizePointFace(element.face)) {
            "o" -> "ellipse"
            "[]" -> "rect"
            else -> "path"
        }
        val node = when (val result = createAndAppend(
            primitive = primitive,
            id = element.id,
            layer = element.style.layer,
        )) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return updatePoint(node, element)
    }

    internal fun updatePoint(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.Point,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        val size = element.size
        if (!size.isFinite()) {
            return GMResult.Err(
                NativeRendererError.InvalidNumber("updatePoint", size),
            )
        }
        node.primitive = when (normalizePointFace(element.face)) {
            "o" -> "ellipse"
            "[]" -> "rect"
            else -> "path"
        }
        node.geometry = NativeRendererGeometry.Point(
            position = element.coordinates,
            size = size,
            face = normalizePointFace(element.face),
        )
        return applyVisual(node, element.style)
    }

    internal fun changePointStyle(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.Point,
    ): GMResult<NativeRendererNode, NativeRendererError> =
        updatePoint(node, element)

    internal fun drawLine(
        element: JsxGraphSceneElement.Line,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        val node = when (val result = createAndAppend(
            primitive = "line",
            id = element.id,
            layer = element.style.layer,
        )) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return updateLine(node, element)
    }

    internal fun updateLine(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.Line,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        node.geometry = NativeRendererGeometry.Line(
            point1 = element.point1,
            point2 = element.point2,
            straightFirst = element.straightFirst,
            straightLast = element.straightLast,
        )
        val visual = applyVisual(node, element.style)
        if (visual is GMResult.Err) {
            return visual
        }
        updatePathWithArrowHeads(node, element)
        return GMResult.Ok(node)
    }

    internal fun drawCurve(
        element: JsxGraphSceneElement.Curve,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        val node = when (val result = createAndAppend(
            primitive = "path",
            id = element.id,
            layer = element.style.layer,
        )) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return updateCurve(node, element)
    }

    internal fun updateCurve(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.Curve,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        node.geometry = NativeRendererGeometry.Path(
            subpaths = updatePathStringPrim(element),
            bezierDegree = element.bezierDegree,
            handDrawing = false,
        )
        val visual = applyVisual(
            node = node,
            style = element.style,
            lineCap = element.lineCap,
        )
        if (visual is GMResult.Err) {
            return visual
        }
        updatePathWithArrowHeads(node, element)
        return GMResult.Ok(node)
    }

    internal fun updatePathWithArrowHeads(
        node: NativeRendererNode,
        element: JsxGraphSceneElement,
    ): GMResult<Unit, NativeRendererError> {
        when (element) {
            is JsxGraphSceneElement.Line -> {
                node.firstArrow = element.firstArrow
                node.lastArrow = element.lastArrow
            }
            is JsxGraphSceneElement.Curve -> {
                node.firstArrow = element.firstArrow
                node.lastArrow = element.lastArrow
            }
            else -> Unit
        }
        return GMResult.Ok(Unit)
    }

    internal fun updateLineWithEndings(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.Line,
    ): GMResult<NativeRendererNode, NativeRendererError> =
        updateLine(node, element)

    internal fun updatePath(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.Curve,
    ): GMResult<NativeRendererNode, NativeRendererError> =
        updateCurve(node, element)

    internal fun handleTouchpoints(
        point1: JsxGraphPoint2D,
        point2: JsxGraphPoint2D,
        firstDistance: Double,
        lastDistance: Double,
    ): GMResult<Pair<JsxGraphPoint2D, JsxGraphPoint2D>, NativeRendererError> =
        shortenLine(
            point1 = point1,
            point2 = point2,
            firstDistance = firstDistance,
            lastDistance = lastDistance,
            operation = "handleTouchpoints",
        )

    internal fun setArrowSize(
        node: NativeRendererNode,
        firstArrow: JsxGraphArrowHead?,
        lastArrow: JsxGraphArrowHead?,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.firstArrow = firstArrow
            it.lastArrow = lastArrow
        }

    internal fun setLineCap(
        node: NativeRendererNode,
        lineCap: String,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.visual = it.visual.copy(lineCap = normalizeLineCap(lineCap))
        }

    internal fun drawTicks(
        element: JsxGraphSceneElement.Ticks,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        val node = when (val result = createAndAppend(
            primitive = "path",
            id = element.id,
            layer = element.style.layer,
        )) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return updateTicks(node, element)
    }

    internal fun updateTicks(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.Ticks,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        node.properties["ticks"] = element.definition
        return applyVisual(node, element.style)
    }

    internal fun drawEllipse(
        element: JsxGraphSceneElement.Circle,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        val node = when (val result = createAndAppend(
            primitive = "ellipse",
            id = element.id,
            layer = element.style.layer,
        )) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return updateEllipse(node, element)
    }

    internal fun updateEllipse(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.Circle,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        if (!element.radius.isFinite()) {
            return GMResult.Err(
                NativeRendererError.InvalidNumber(
                    "updateEllipse",
                    element.radius,
                ),
            )
        }
        node.geometry = NativeRendererGeometry.Ellipse(
            center = element.center,
            radiusX = abs(element.radius),
            radiusY = abs(element.radius),
        )
        return applyVisual(node, element.style)
    }

    internal fun drawPolygon(
        element: JsxGraphSceneElement.Polygon,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        val node = when (val result = createAndAppend(
            primitive = "polygon",
            id = element.id,
            layer = element.style.layer,
        )) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return updatePolygon(node, element)
    }

    internal fun updatePolygon(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.Polygon,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        node.geometry = NativeRendererGeometry.Polygon(
            vertices = element.vertices,
            closed = element.isClosed,
        )
        return applyVisual(node, element.style, not = setOf("stroke", "dash"))
    }

    internal fun displayCopyright(
        text: String,
        fontSize: Double,
    ): GMResult<NativeRendererNode, NativeRendererError> =
        drawOverlayText("copyright", text, fontSize)

    internal fun displayLogo(
        source: String,
        fontSize: Double,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        if (!fontSize.isFinite()) {
            return GMResult.Err(
                NativeRendererError.InvalidNumber(
                    "displayLogo",
                    fontSize,
                ),
            )
        }
        val node = when (val result = createAndAppend(
            primitive = "image",
            id = generatedId("logo"),
            layer = Int.MAX_VALUE,
        )) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        node.geometry = NativeRendererGeometry.Image(
            source = source,
            anchor = JsxGraphPoint2D(4.0, 6.0),
            widthVector = JsxGraphPoint2D(1.5 * fontSize, 0.0),
            heightVector = JsxGraphPoint2D(0.0, 1.5 * fontSize),
        )
        return GMResult.Ok(node)
    }

    internal fun drawInternalText(
        element: JsxGraphSceneElement.Text,
    ): GMResult<NativeRendererNode, NativeRendererError> =
        drawText(element)

    internal fun updateInternalText(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.Text,
    ): GMResult<NativeRendererNode, NativeRendererError> =
        updateText(node, element)

    internal fun drawText(
        element: JsxGraphSceneElement.Text,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        val node = when (val result = createAndAppend(
            primitive = "text",
            id = element.id,
            layer = element.style.layer,
        )) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return updateText(node, element)
    }

    internal fun updateClipPath(
        node: NativeRendererNode,
        clip: Boolean,
    ): GMResult<Unit, NativeRendererError> = setClipPath(node, clip)

    internal fun updateText(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.Text,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        node.geometry = NativeRendererGeometry.Text(
            position = element.coordinates,
            content = element.content,
            fontSize = element.fontSize,
            anchorX = element.anchorX,
            anchorY = element.anchorY,
        )
        return updateTextStyle(node, element.style)
    }

    internal fun updateTextStyle(
        node: NativeRendererNode,
        style: JsxGraphElementStyle,
    ): GMResult<NativeRendererNode, NativeRendererError> =
        applyVisual(node, style)

    internal fun updateInternalTextStyle(
        node: NativeRendererNode,
        strokeColor: JsxGraphColor,
        strokeOpacity: Double,
    ): GMResult<Unit, NativeRendererError> =
        setObjectStrokeColor(node, strokeColor, strokeOpacity)

    internal fun drawImage(
        element: JsxGraphSceneElement.Image,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        val node = when (val result = createAndAppend(
            primitive = "image",
            id = element.id,
            layer = element.style.layer,
        )) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return updateImage(node, element)
    }

    internal fun updateImage(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.Image,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        node.geometry = NativeRendererGeometry.Image(
            source = element.source,
            anchor = element.anchor,
            widthVector = element.widthVector,
            heightVector = element.heightVector,
        )
        return applyVisual(node, element.style, not = setOf("stroke", "dash"))
    }

    internal fun joinTransforms(
        originX: Double,
        originY: Double,
        unitX: Double,
        unitY: Double,
        transformations: List<Array<DoubleArray>>,
    ): GMResult<Array<DoubleArray>, NativeRendererError> {
        val values = listOf(originX, originY, unitX, unitY)
        values.firstOrNull { !it.isFinite() }?.let { invalid ->
            return GMResult.Err(
                NativeRendererError.InvalidNumber(
                    operation = "joinTransforms",
                    value = invalid,
                ),
            )
        }
        if (unitX == 0.0 || unitY == 0.0) {
            return GMResult.Err(
                NativeRendererError.InvalidNumber(
                    operation = "joinTransforms",
                    value = 0.0,
                ),
            )
        }
        var matrix = arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(-originX / unitX, 1.0 / unitX, 0.0),
            doubleArrayOf(originY / unitY, 0.0, -1.0 / unitY),
        )
        for (transformation in transformations) {
            if (
                transformation.size != 3 ||
                transformation.any { row -> row.size != 3 }
            ) {
                return GMResult.Err(
                    NativeRendererError.UnsupportedCapability(
                        renderer = type,
                        capability = "non-3x3 transformation",
                    ),
                )
            }
            matrix = Mat.matMatMult(transformation, matrix)
        }
        matrix = Mat.matMatMult(
            arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(originX, unitX, 0.0),
                doubleArrayOf(originY, 0.0, -unitY),
            ),
            matrix,
        )
        return GMResult.Ok(matrix)
    }

    internal fun transformRect(
        node: NativeRendererNode,
        transformation: Array<DoubleArray>,
    ): GMResult<Unit, NativeRendererError> {
        if (
            transformation.size != 3 ||
            transformation.any { row -> row.size != 3 }
        ) {
            return GMResult.Err(
                NativeRendererError.UnsupportedCapability(
                    renderer = type,
                    capability = "non-3x3 transformation",
                ),
            )
        }
        return mutate(node) {
            it.transform =
                transformation.map(DoubleArray::copyOf).toTypedArray()
        }
    }

    internal fun updateImageURL(
        node: NativeRendererNode,
        source: String,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            val image = it.geometry as? NativeRendererGeometry.Image
            if (image != null) {
                it.geometry = image.copy(source = source)
            } else {
                it.properties["source"] = source
            }
        }

    internal fun updateImageStyle(
        node: NativeRendererNode,
        style: JsxGraphElementStyle,
    ): GMResult<NativeRendererNode, NativeRendererError> =
        applyVisual(node, style, not = setOf("stroke", "dash"))

    internal fun drawForeignObject(
        element: JsxGraphSceneElement.ForeignObject,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        if (!supportsForeignObject) {
            return GMResult.Err(
                NativeRendererError.UnsupportedCapability(
                    renderer = type,
                    capability = "foreignObject",
                ),
            )
        }
        val node = when (val result = createAndAppend(
            primitive = "foreignObject",
            id = element.id,
            layer = element.style.layer,
        )) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return updateForeignObject(node, element)
    }

    internal fun updateForeignObject(
        node: NativeRendererNode,
        element: JsxGraphSceneElement.ForeignObject,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        node.geometry = NativeRendererGeometry.ForeignObject(
            content = element.content,
            anchor = element.anchor,
        )
        node.properties["size"] = element.size
        node.properties["evaluateOnlyOnce"] = element.evaluateOnlyOnce
        return applyVisual(node, element.style, not = setOf("stroke", "dash"))
    }

    internal fun appendChildPrim(
        node: NativeRendererNode,
        level: Int,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        if (freed) {
            return GMResult.Err(NativeRendererError.RendererFreed)
        }
        val effectiveLevel = max(0, level)
        nodes[node.id]?.let { old ->
            layerOrder[old.visual.layer]?.remove(old.id)
        }
        node.removed = false
        node.visual = node.visual.copy(layer = effectiveLevel)
        nodes[node.id] = node
        layerOrder.getOrPut(effectiveLevel) { mutableListOf() }
            .apply {
                remove(node.id)
                add(node.id)
            }
        return GMResult.Ok(node)
    }

    internal fun appendNodesToElement(
        node: NativeRendererNode,
        type: String,
    ): GMResult<Unit, NativeRendererError> =
        setPropertyPrim(node, "nodeType", type)

    internal fun createPrim(
        primitive: String,
        id: String,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        if (freed) {
            return GMResult.Err(NativeRendererError.RendererFreed)
        }
        if (primitive !in SUPPORTED_PRIMITIVES) {
            return GMResult.Err(
                NativeRendererError.InvalidPrimitive(primitive),
            )
        }
        return GMResult.Ok(NativeRendererNode(id, primitive))
    }

    internal fun remove(
        node: NativeRendererNode,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.removed = true
            nodes.remove(it.id)
            layerOrder[it.visual.layer]?.remove(it.id)
        }

    internal fun makeArrows(
        node: NativeRendererNode,
        firstArrow: JsxGraphArrowHead?,
        lastArrow: JsxGraphArrowHead?,
    ): GMResult<Unit, NativeRendererError> =
        setArrowSize(node, firstArrow, lastArrow)

    internal fun _setArrowWidth(
        node: NativeRendererNode,
        firstWidth: Double?,
        lastWidth: Double?,
    ): GMResult<Unit, NativeRendererError> {
        listOfNotNull(firstWidth, lastWidth)
            .firstOrNull { !it.isFinite() || it < 0.0 }
            ?.let { invalid ->
                return GMResult.Err(
                    NativeRendererError.InvalidNumber(
                        operation = "_setArrowWidth",
                        value = invalid,
                    ),
                )
            }
        return mutate(node) {
            it.properties["firstArrowWidth"] = firstWidth
            it.properties["lastArrowWidth"] = lastWidth
        }
    }

    internal fun updateEllipsePrim(
        node: NativeRendererNode,
        x: Double,
        y: Double,
        radiusX: Double,
        radiusY: Double,
    ): GMResult<Unit, NativeRendererError> {
        val invalid = listOf(x, y, radiusX, radiusY)
            .firstOrNull { !it.isFinite() }
        if (invalid != null) {
            return GMResult.Err(
                NativeRendererError.InvalidNumber(
                    operation = "updateEllipsePrim",
                    value = invalid,
                ),
            )
        }
        return mutate(node) {
            it.geometry = NativeRendererGeometry.Ellipse(
                center = JsxGraphPoint2D(x, y),
                radiusX = abs(radiusX),
                radiusY = abs(radiusY),
            )
        }
    }

    internal fun updateLinePrim(
        node: NativeRendererNode,
        point1X: Double,
        point1Y: Double,
        point2X: Double,
        point2Y: Double,
    ): GMResult<Unit, NativeRendererError> {
        val invalid = listOf(point1X, point1Y, point2X, point2Y)
            .firstOrNull { !it.isFinite() }
        if (invalid != null) {
            return GMResult.Err(
                NativeRendererError.InvalidNumber(
                    operation = "updateLinePrim",
                    value = invalid,
                ),
            )
        }
        return mutate(node) {
            it.geometry = NativeRendererGeometry.Line(
                point1 = JsxGraphPoint2D(point1X, point1Y),
                point2 = JsxGraphPoint2D(point2X, point2Y),
                straightFirst = false,
                straightLast = false,
            )
        }
    }

    internal fun updatePathPrim(
        node: NativeRendererNode,
        subpaths: List<List<JsxGraphPoint2D>>,
        bezierDegree: Int,
        handDrawing: Boolean = false,
    ): GMResult<Unit, NativeRendererError> {
        if (bezierDegree !in setOf(1, 3)) {
            return GMResult.Err(
                NativeRendererError.UnsupportedCapability(
                    renderer = type,
                    capability = "Bezier degree $bezierDegree",
                ),
            )
        }
        return mutate(node) {
            it.geometry = NativeRendererGeometry.Path(
                subpaths = subpaths,
                bezierDegree = bezierDegree,
                handDrawing = handDrawing,
            )
        }
    }

    internal fun updatePathStringPoint(
        position: JsxGraphPoint2D,
        size: Double,
        type: String,
    ): List<List<JsxGraphPoint2D>> {
        val x = position.x
        val y = position.y
        val sqrt32 = size * sqrt(3.0) * 0.5
        val half = size * 0.5
        return when (normalizePointFace(type)) {
            "x" -> listOf(
                listOf(
                    JsxGraphPoint2D(x - size, y - size),
                    JsxGraphPoint2D(x + size, y + size),
                ),
                listOf(
                    JsxGraphPoint2D(x + size, y - size),
                    JsxGraphPoint2D(x - size, y + size),
                ),
            )
            "+" -> listOf(
                listOf(
                    JsxGraphPoint2D(x - size, y),
                    JsxGraphPoint2D(x + size, y),
                ),
                listOf(
                    JsxGraphPoint2D(x, y - size),
                    JsxGraphPoint2D(x, y + size),
                ),
            )
            "|" -> listOf(
                listOf(
                    JsxGraphPoint2D(x, y - size),
                    JsxGraphPoint2D(x, y + size),
                ),
            )
            "-" -> listOf(
                listOf(
                    JsxGraphPoint2D(x - size, y),
                    JsxGraphPoint2D(x + size, y),
                ),
            )
            "<>", "<<>>" -> {
                val radius =
                    if (normalizePointFace(type) == "<<>>") size * 1.41
                    else size
                listOf(
                    listOf(
                        JsxGraphPoint2D(x - radius, y),
                        JsxGraphPoint2D(x, y + radius),
                        JsxGraphPoint2D(x + radius, y),
                        JsxGraphPoint2D(x, y - radius),
                        JsxGraphPoint2D(x - radius, y),
                    ),
                )
            }
            "^" -> listOf(
                listOf(
                    JsxGraphPoint2D(x, y - size),
                    JsxGraphPoint2D(x - sqrt32, y + half),
                    JsxGraphPoint2D(x + sqrt32, y + half),
                    JsxGraphPoint2D(x, y - size),
                ),
            )
            "v" -> listOf(
                listOf(
                    JsxGraphPoint2D(x, y + size),
                    JsxGraphPoint2D(x - sqrt32, y - half),
                    JsxGraphPoint2D(x + sqrt32, y - half),
                    JsxGraphPoint2D(x, y + size),
                ),
            )
            ">" -> listOf(
                listOf(
                    JsxGraphPoint2D(x + size, y),
                    JsxGraphPoint2D(x - half, y - sqrt32),
                    JsxGraphPoint2D(x - half, y + sqrt32),
                    JsxGraphPoint2D(x + size, y),
                ),
            )
            "<" -> listOf(
                listOf(
                    JsxGraphPoint2D(x - size, y),
                    JsxGraphPoint2D(x + half, y - sqrt32),
                    JsxGraphPoint2D(x + half, y + sqrt32),
                    JsxGraphPoint2D(x - size, y),
                ),
            )
            else -> emptyList()
        }
    }

    internal fun updatePathStringPrim(
        element: JsxGraphSceneElement.Curve,
    ): List<List<JsxGraphPoint2D>> = splitSubpaths(element.points)

    internal fun updatePathStringBezierPrim(
        element: JsxGraphSceneElement.Curve,
    ): List<List<JsxGraphPoint2D>> = splitSubpaths(element.points)

    internal fun updatePolygonPrim(
        node: NativeRendererNode,
        vertices: List<JsxGraphPoint2D>,
        closed: Boolean,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.geometry = NativeRendererGeometry.Polygon(vertices, closed)
        }

    internal fun updateRectPrim(
        node: NativeRendererNode,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
    ): GMResult<Unit, NativeRendererError> {
        val invalid = listOf(x, y, width, height)
            .firstOrNull { !it.isFinite() }
        if (invalid != null) {
            return GMResult.Err(
                NativeRendererError.InvalidNumber(
                    operation = "updateRectPrim",
                    value = invalid,
                ),
            )
        }
        return mutate(node) {
            it.geometry = NativeRendererGeometry.Rect(
                x = x,
                y = y,
                width = width,
                height = height,
            )
        }
    }

    internal fun display(
        node: NativeRendererNode,
        value: Boolean,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.visual = it.visual.copy(visible = value)
        }

    internal fun hide(
        node: NativeRendererNode,
    ): GMResult<Unit, NativeRendererError> = display(node, false)

    internal fun highlight(
        node: NativeRendererNode,
        style: JsxGraphElementStyle? = null,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.visual = (
                style
                    ?.copy(highlighted = true)
                    ?.let(::visualState)
                    ?: it.visual
                )
                .copy(highlighted = true)
        }

    internal fun noHighlight(
        node: NativeRendererNode,
        style: JsxGraphElementStyle? = null,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.visual = (
                style
                    ?.copy(highlighted = false)
                    ?.let(::visualState)
                    ?: it.visual
                )
                .copy(highlighted = false)
        }

    internal fun removeDraft(
        node: NativeRendererNode,
        style: JsxGraphElementStyle,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.visual = visualState(
                style.copy(
                    draft = style.draft.copy(enabled = false),
                ),
            )
        }

    internal fun setARIA(
        node: NativeRendererNode,
        aria: Map<String, String>,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.visual = it.visual.copy(aria = aria)
        }

    internal fun setBuffering(
        node: NativeRendererNode,
        buffering: String,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.visual = it.visual.copy(buffering = buffering)
        }

    internal fun setClipPath(
        node: NativeRendererNode,
        clip: Boolean,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.visual = it.visual.copy(clipToViewport = clip)
        }

    internal fun setCssClass(
        node: NativeRendererNode,
        cssClass: String,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.visual = it.visual.copy(cssClass = cssClass)
        }

    internal fun setDashStyle(
        node: NativeRendererNode,
        dash: Int,
        dashScale: Boolean,
    ): GMResult<Unit, NativeRendererError> {
        if (dash !in 0..dashArray.size) {
            return GMResult.Err(
                NativeRendererError.UnsupportedCapability(
                    renderer = type,
                    capability = "dash style $dash",
                ),
            )
        }
        val scale = if (dashScale) node.visual.strokeWidth * 0.5 else 1.0
        return mutate(node) {
            it.visual = it.visual.copy(
                dashPattern =
                    if (dash == 0) emptyList()
                    else dashArray[dash - 1].map { value -> value * scale },
            )
        }
    }

    internal fun setDraft(
        node: NativeRendererNode,
        color: JsxGraphColor,
        opacity: Double,
        strokeWidth: Double,
    ): GMResult<Unit, NativeRendererError> {
        val valid = validateOpacity(opacity, "setDraft")
        if (valid is GMResult.Err) {
            return valid
        }
        return mutate(node) {
            it.visual = it.visual.copy(
                strokeColor = color,
                fillColor =
                    if (it.primitive in FILLABLE_PRIMITIVES) color
                    else JsxGraphColor.Transparent,
                strokeOpacity = opacity,
                fillOpacity = opacity,
                strokeWidth = strokeWidth,
                draft = true,
            )
        }
    }

    internal fun setGradient(
        node: NativeRendererNode,
        gradient: JsxGraphFillGradient?,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.visual = it.visual.copy(gradient = gradient)
        }

    internal fun setLayer(
        node: NativeRendererNode,
        level: Int,
    ): GMResult<Unit, NativeRendererError> {
        val active = activeNode(node)
        if (active is GMResult.Err) {
            return active
        }
        layerOrder[node.visual.layer]?.remove(node.id)
        node.visual = node.visual.copy(layer = max(0, level))
        layerOrder.getOrPut(node.visual.layer) { mutableListOf() }
            .apply {
                remove(node.id)
                add(node.id)
            }
        return GMResult.Ok(Unit)
    }

    internal fun setObjectFillColor(
        node: NativeRendererNode,
        color: JsxGraphColor,
        opacity: Double,
    ): GMResult<Unit, NativeRendererError> {
        val valid = validateOpacity(opacity, "setObjectFillColor")
        if (valid is GMResult.Err) {
            return valid
        }
        return mutate(node) {
            it.visual = it.visual.copy(
                fillColor = color,
                fillOpacity = opacity,
            )
        }
    }

    internal fun setObjectStrokeColor(
        node: NativeRendererNode,
        color: JsxGraphColor,
        opacity: Double,
    ): GMResult<Unit, NativeRendererError> {
        val valid = validateOpacity(opacity, "setObjectStrokeColor")
        if (valid is GMResult.Err) {
            return valid
        }
        return mutate(node) {
            it.visual = it.visual.copy(
                strokeColor = color,
                strokeOpacity = opacity,
            )
        }
    }

    internal fun setObjectStrokeWidth(
        node: NativeRendererNode,
        width: Double,
    ): GMResult<Unit, NativeRendererError> {
        if (!width.isFinite() || width < 0.0) {
            return GMResult.Err(
                NativeRendererError.InvalidNumber(
                    operation = "setObjectStrokeWidth",
                    value = width,
                ),
            )
        }
        return mutate(node) {
            it.visual = it.visual.copy(strokeWidth = width)
        }
    }

    internal fun setObjectTransition(
        node: NativeRendererNode,
        duration: Double,
        properties: List<String>,
    ): GMResult<Unit, NativeRendererError> {
        if (!duration.isFinite() || duration < 0.0) {
            return GMResult.Err(
                NativeRendererError.InvalidNumber(
                    operation = "setObjectTransition",
                    value = duration,
                ),
            )
        }
        return mutate(node) {
            it.visual = it.visual.copy(
                transitionDuration = duration,
                transitionProperties = properties,
            )
        }
    }

    internal fun setPropertyPrim(
        node: NativeRendererNode,
        key: String,
        value: Any?,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            if (value == null) {
                it.properties.remove(key)
            } else {
                it.properties[key] = value
            }
        }

    internal fun setShadow(
        node: NativeRendererNode,
        shadow: JsxGraphShadow,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.visual = it.visual.copy(shadow = shadow)
        }

    internal fun setTabindex(
        node: NativeRendererNode,
        tabindex: Int?,
    ): GMResult<Unit, NativeRendererError> =
        mutate(node) {
            it.visual = it.visual.copy(
                tabindex = if (it.visual.visible) tabindex else null,
            )
        }

    internal fun show(
        node: NativeRendererNode,
    ): GMResult<Unit, NativeRendererError> = display(node, true)

    internal fun updateGradient(
        node: NativeRendererNode,
        gradient: JsxGraphFillGradient?,
    ): GMResult<Unit, NativeRendererError> = setGradient(node, gradient)

    internal fun suspendRedraw(): GMResult<Unit, NativeRendererError> {
        if (freed) {
            return GMResult.Err(NativeRendererError.RendererFreed)
        }
        redrawSuspended = true
        return GMResult.Ok(Unit)
    }

    internal fun unsuspendRedraw(): GMResult<Unit, NativeRendererError> {
        if (freed) {
            return GMResult.Err(NativeRendererError.RendererFreed)
        }
        redrawSuspended = false
        return GMResult.Ok(Unit)
    }

    override fun drawNavigationBar(
        board: Board,
        attributes: Map<String, Any?>,
    ) {
        navigationAttributes = attributes.toMap()
    }

    internal fun getElementById(id: String): NativeRendererNode? =
        nodes[id]?.takeUnless(NativeRendererNode::removed)

    internal fun removeToInsertLater(
        node: NativeRendererNode,
    ): GMResult<() -> GMResult<Unit, NativeRendererError>, NativeRendererError> {
        val active = activeNode(node)
        if (active is GMResult.Err) {
            return active
        }
        val level = node.visual.layer
        val position = layerOrder[level]?.indexOf(node.id) ?: -1
        nodes.remove(node.id)
        layerOrder[level]?.remove(node.id)
        node.removed = true
        return GMResult.Ok {
            if (freed) {
                GMResult.Err(NativeRendererError.RendererFreed)
            } else {
                node.removed = false
                nodes[node.id] = node
                val ids = layerOrder.getOrPut(level) { mutableListOf() }
                val insertion = position.coerceIn(0, ids.size)
                ids.add(insertion, node.id)
                GMResult.Ok(Unit)
            }
        }
    }

    internal fun resize(
        width: Double,
        height: Double,
    ): GMResult<Unit, NativeRendererError> {
        if (freed) {
            return GMResult.Err(NativeRendererError.RendererFreed)
        }
        if (
            !width.isFinite() ||
            !height.isFinite() ||
            width <= 0.0 ||
            height <= 0.0
        ) {
            return GMResult.Err(
                NativeRendererError.InvalidDimensions(width, height),
            )
        }
        this.width = width
        this.height = height
        return GMResult.Ok(Unit)
    }

    internal fun createTouchpoints(
        count: Int,
    ): GMResult<Unit, NativeRendererError> {
        if (freed) {
            return GMResult.Err(NativeRendererError.RendererFreed)
        }
        if (count < 0) {
            return GMResult.Err(
                NativeRendererError.InvalidTouchpointIndex(count),
            )
        }
        touchpoints.clear()
        repeat(count) {
            touchpoints += NativeRendererTouchpoint()
        }
        return GMResult.Ok(Unit)
    }

    internal fun showTouchpoint(
        index: Int,
    ): GMResult<Unit, NativeRendererError> =
        updateTouchpointVisibility(index, true)

    internal fun hideTouchpoint(
        index: Int,
    ): GMResult<Unit, NativeRendererError> =
        updateTouchpointVisibility(index, false)

    internal fun updateTouchpoint(
        index: Int,
        position: JsxGraphPoint2D,
    ): GMResult<Unit, NativeRendererError> {
        if (freed) {
            return GMResult.Err(NativeRendererError.RendererFreed)
        }
        if (index !in touchpoints.indices) {
            return GMResult.Err(
                NativeRendererError.InvalidTouchpointIndex(index),
            )
        }
        touchpoints[index] = touchpoints[index].copy(position = position)
        return GMResult.Ok(Unit)
    }

    internal fun dumpToDataURI(
        ignoreTexts: Boolean,
    ): GMResult<String, NativeRendererError> {
        @Suppress("UNUSED_VARIABLE")
        val requestedTextPolicy = ignoreTexts
        return unsupported("SVG data URI export")
    }

    internal fun dumpToCanvas(
        canvasId: String,
        width: Double,
        height: Double,
        ignoreTexts: Boolean,
    ): GMResult<Unit, NativeRendererError> {
        @Suppress("UNUSED_VARIABLE")
        val request = listOf(canvasId, width, height, ignoreTexts)
        return unsupported("browser canvas export")
    }

    internal fun screenshot(
        board: Board,
    ): GMResult<Unit, NativeRendererError> {
        @Suppress("UNUSED_VARIABLE")
        val requestedBoard = board
        return unsupported("browser screenshot UI")
    }

    override fun clear() {
        nodes.clear()
        layerOrder.clear()
    }

    override fun free() {
        clear()
        touchpoints.clear()
        navigationAttributes = emptyMap()
        redrawSuspended = false
        freed = true
    }

    private fun createAndAppend(
        primitive: String,
        id: String,
        layer: Int,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        val node = when (val result = createPrim(primitive, id)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return appendChildPrim(node, layer)
    }

    private fun applyVisual(
        node: NativeRendererNode,
        style: JsxGraphElementStyle,
        lineCap: String = style.lineCap,
        not: Set<String> = emptySet(),
    ): GMResult<NativeRendererNode, NativeRendererError> {
        return when (
            val result = _updateVisual(
                node = node,
                style = style,
                lineCap = lineCap,
                not = not,
                enhanced = true,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(node)
            is GMResult.Err -> result
        }
    }

    private fun visualState(
        style: JsxGraphElementStyle,
    ): NativeRendererVisualState {
        val resolved = style.resolveVisualStyle()
        return NativeRendererVisualState(
            visible = style.visible,
            strokeColor = resolved.strokeColor,
            fillColor = resolved.fillColor,
            strokeWidth = resolved.strokeWidth,
            strokeOpacity = resolved.strokeOpacity,
            fillOpacity = resolved.fillOpacity,
            layer = style.layer,
            dashPattern = style.strokeDashPattern,
            lineCap = normalizeLineCap(style.lineCap),
            clipToViewport = style.clipToViewport,
            cssClass = resolved.cssClass,
            aria = style.aria,
            tabindex = if (style.visible) style.tabIndex else null,
            transitionDuration = style.transitionDurationMillis,
            transitionProperties = style.transitionProperties,
            shadow = style.shadow,
            gradient = style.fillGradient,
            draft = resolved.isDraft,
            highlighted = resolved.isHighlighted,
        )
    }

    private fun activeNode(
        node: NativeRendererNode,
    ): GMResult<NativeRendererNode, NativeRendererError> =
        when {
            freed -> GMResult.Err(NativeRendererError.RendererFreed)
            node.removed -> GMResult.Err(
                NativeRendererError.RemovedNode(node.id),
            )
            nodes[node.id] !== node -> GMResult.Err(
                NativeRendererError.MissingNode(node.id),
            )
            else -> GMResult.Ok(node)
        }

    private inline fun mutate(
        node: NativeRendererNode,
        mutation: (NativeRendererNode) -> Unit,
    ): GMResult<Unit, NativeRendererError> {
        val active = activeNode(node)
        if (active is GMResult.Err) {
            return active
        }
        mutation(node)
        return GMResult.Ok(Unit)
    }

    private fun validateOpacity(
        value: Double,
        operation: String,
    ): GMResult<Unit, NativeRendererError> =
        if (value.isFinite() && value in 0.0..1.0) {
            GMResult.Ok(Unit)
        } else {
            GMResult.Err(
                NativeRendererError.InvalidNumber(operation, value),
            )
        }

    private fun updateTouchpointVisibility(
        index: Int,
        visible: Boolean,
    ): GMResult<Unit, NativeRendererError> {
        if (freed) {
            return GMResult.Err(NativeRendererError.RendererFreed)
        }
        if (index !in touchpoints.indices) {
            return GMResult.Err(
                NativeRendererError.InvalidTouchpointIndex(index),
            )
        }
        touchpoints[index] = touchpoints[index].copy(visible = visible)
        return GMResult.Ok(Unit)
    }

    private fun drawOverlayText(
        prefix: String,
        text: String,
        fontSize: Double,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        if (!fontSize.isFinite()) {
            return GMResult.Err(
                NativeRendererError.InvalidNumber(
                    operation = "displayCopyright",
                    value = fontSize,
                ),
            )
        }
        val node = when (val result = createAndAppend(
            primitive = "text",
            id = generatedId(prefix),
            layer = Int.MAX_VALUE,
        )) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        node.geometry = NativeRendererGeometry.Text(
            position = JsxGraphPoint2D(
                x = 4.0 + 1.8 * fontSize,
                y = 6.0 + fontSize,
            ),
            content = text,
            fontSize = fontSize,
            anchorX = "left",
            anchorY = "top",
        )
        node.visual = node.visual.copy(fillOpacity = 0.2)
        return GMResult.Ok(node)
    }

    private fun generatedId(prefix: String): String {
        generatedNodeId += 1
        return "$prefix-$generatedNodeId"
    }

    private fun <T> unsupported(
        capability: String,
    ): GMResult<T, NativeRendererError> =
        GMResult.Err(
            NativeRendererError.UnsupportedCapability(type, capability),
        )

    private fun shortenLine(
        point1: JsxGraphPoint2D,
        point2: JsxGraphPoint2D,
        firstDistance: Double,
        lastDistance: Double,
        operation: String,
    ): GMResult<Pair<JsxGraphPoint2D, JsxGraphPoint2D>, NativeRendererError> {
        val values = listOf(
            point1.x,
            point1.y,
            point2.x,
            point2.y,
            firstDistance,
            lastDistance,
        )
        values.firstOrNull { !it.isFinite() }?.let { invalid ->
            return GMResult.Err(
                NativeRendererError.InvalidNumber(operation, invalid),
            )
        }
        val deltaX = point2.x - point1.x
        val deltaY = point2.y - point1.y
        val distance = kotlin.math.hypot(deltaX, deltaY)
        if (distance == 0.0) {
            return GMResult.Ok(point1 to point2)
        }
        return GMResult.Ok(
            JsxGraphPoint2D(
                x = point1.x + deltaX * firstDistance / distance,
                y = point1.y + deltaY * firstDistance / distance,
            ) to JsxGraphPoint2D(
                x = point2.x - deltaX * lastDistance / distance,
                y = point2.y - deltaY * lastDistance / distance,
            ),
        )
    }

    private fun splitSubpaths(
        points: List<JsxGraphPoint2D?>,
    ): List<List<JsxGraphPoint2D>> {
        val result = mutableListOf<List<JsxGraphPoint2D>>()
        var current = mutableListOf<JsxGraphPoint2D>()
        for (point in points) {
            if (point == null || !point.x.isFinite() || !point.y.isFinite()) {
                if (current.isNotEmpty()) {
                    result += current
                    current = mutableListOf()
                }
            } else {
                current += point
            }
        }
        if (current.isNotEmpty()) {
            result += current
        }
        return result
    }

    private fun normalizePointFace(face: String): String =
        when (face.lowercase()) {
            "circle", "o" -> "o"
            "square", "[]" -> "[]"
            "cross", "x" -> "x"
            "plus", "+" -> "+"
            "diamond", "<>" -> "<>"
            "diamond2", "<<>>" -> "<<>>"
            "triangleup", "a", "^" -> "^"
            "triangledown", "v" -> "v"
            "triangleright", ">" -> ">"
            "triangleleft", "<" -> "<"
            "|" -> "|"
            "-" -> "-"
            else -> face
        }

    private fun normalizeLineCap(lineCap: String): String =
        lineCap.takeIf { it in setOf("butt", "round", "square") } ?: "round"

    private companion object {
        val SUPPORTED_PRIMITIVES = setOf(
            "ellipse",
            "foreignObject",
            "image",
            "line",
            "linearGradient",
            "marker",
            "path",
            "polygon",
            "radialGradient",
            "rect",
            "stop",
            "text",
        )
        val FILLABLE_PRIMITIVES = setOf(
            "ellipse",
            "path",
            "polygon",
            "rect",
            "text",
        )
    }
}

/**
 * Canvas-flavoured native renderer.
 *
 * Geometry is retained as vectors and consumed by Compose Canvas instead of
 * an HTML CanvasRenderingContext2D.
 */
// JSXGraph 1.13.3: src/renderer/canvas.js ->
// CanvasRenderer, _fill, _rotatePoint, _rotateShape, updateGradientAngle,
// updateGradientCircle, updateGradient, _setColor, _translateShape.
// JSXGraph 1.13.3: src/renderer/canvas.js ->
// drawPoint, updatePoint, drawLine, updateLine, drawTicks, updateTicks,
// drawCurve, updateCurve, drawEllipse, updateEllipse.
// JSXGraph 1.13.3: src/renderer/canvas.js ->
// displayCopyright, displayLogo, updateInternalText, setObjectStrokeColor,
// drawImage, updateImageURL, remove, updatePathStringPrim.
// JSXGraph 1.13.3: src/renderer/canvas.js ->
// updatePathStringBezierPrim, display, show, hide, setGradient, setShadow,
// highlight, noHighlight, suspendRedraw, unsuspendRedraw.
// JSXGraph 1.13.3: src/renderer/canvas.js ->
// resize, removeToInsertLater.
internal class CanvasRenderer(
    containerId: String,
    dimensions: EnvDimensions,
) : AbstractRenderer("canvas", containerId, dimensions) {
    internal fun _rotatePoint(
        angle: Double,
        x: Double,
        y: Double,
    ): JsxGraphPoint2D =
        JsxGraphPoint2D(
            x = x * cos(angle) - y * sin(angle),
            y = x * sin(angle) + y * cos(angle),
        )

    internal fun _rotateShape(
        shape: List<JsxGraphPoint2D>,
        angle: Double,
    ): List<JsxGraphPoint2D> =
        shape.map { point -> _rotatePoint(angle, point.x, point.y) }

    internal fun _translateShape(
        shape: List<JsxGraphPoint2D>,
        x: Double,
        y: Double,
    ): List<JsxGraphPoint2D> =
        shape.map { point ->
            JsxGraphPoint2D(point.x + x, point.y + y)
        }

    internal fun updateGradientAngle(
        bounds: NativeRendererBounds,
        radians: Double,
    ): Pair<JsxGraphPoint2D, JsxGraphPoint2D> {
        val cosine = cos(-radians)
        val sine = sin(-radians)
        val factor = 1.0 / max(abs(cosine), abs(sine))
        val x1 = if (cosine >= 0.0) 0.0 else -cosine * factor
        val x2 = if (cosine >= 0.0) cosine * factor else 0.0
        val y1 = if (sine >= 0.0) 0.0 else -sine * factor
        val y2 = if (sine >= 0.0) sine * factor else 0.0
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        return JsxGraphPoint2D(
            x = bounds.left + width * x1,
            y = bounds.top + height * y1,
        ) to JsxGraphPoint2D(
            x = bounds.left + width * x2,
            y = bounds.top + height * y2,
        )
    }

    internal fun updateGradientCircle(
        bounds: NativeRendererBounds,
        centerX: Double,
        centerY: Double,
        radius: Double,
        focalX: Double,
        focalY: Double,
        focalRadius: Double,
    ): Pair<NativeRendererGeometry.Ellipse, NativeRendererGeometry.Ellipse> {
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        val scale = (abs(width) + abs(height)) * 0.5
        val focal = NativeRendererGeometry.Ellipse(
            center = JsxGraphPoint2D(
                bounds.left + width * focalX,
                bounds.top + height * focalY,
            ),
            radiusX = focalRadius * scale,
            radiusY = focalRadius * scale,
        )
        val outer = NativeRendererGeometry.Ellipse(
            center = JsxGraphPoint2D(
                bounds.left + width * centerX,
                bounds.top + height * centerY,
            ),
            radiusX = radius * scale,
            radiusY = radius * scale,
        )
        return focal to outer
    }

    internal fun _fill(
        node: NativeRendererNode,
    ): Boolean =
        node.visual.fillOpacity > 0.0 &&
            node.visual.fillColor.alpha > 0

    internal fun _setColor(
        node: NativeRendererNode,
        type: String,
    ): Pair<JsxGraphColor, Double>? =
        when (type) {
            "fill" ->
                node.visual.fillColor
                    .takeIf { color -> color.alpha > 0 }
                    ?.let { color -> color to node.visual.fillOpacity }
            "stroke" ->
                node.visual.strokeColor
                    .takeIf { color -> color.alpha > 0 }
                    ?.let { color -> color to node.visual.strokeOpacity }
            else -> null
        }
}

/**
 * SVG-flavoured state renderer.
 *
 * It preserves SVG primitive semantics but deliberately does not serialize a
 * browser DOM. The Compose renderer consumes the same node state.
 */
// JSXGraph 1.13.3: src/renderer/svg.js ->
// SVGRenderer, _createArrowHead, _setArrowColor, _setArrowWidth,
// updateTicks, displayCopyright, displayLogo, drawInternalText.
// JSXGraph 1.13.3: src/renderer/svg.js ->
// updateInternalText, updateInternalTextStyle, drawImage, transformRect,
// updateImageURL, updateImageStyle, drawForeignObject, updateForeignObject.
// JSXGraph 1.13.3: src/renderer/svg.js ->
// appendChildPrim, createPrim, remove, setLayer, makeArrows,
// updateEllipsePrim, updateLinePrim, updatePathPrim, updatePathStringPoint.
// JSXGraph 1.13.3: src/renderer/svg.js ->
// updatePathStringPrim, updatePathStringBezierPrim, updatePolygonPrim,
// updateRectPrim, _setAttribute, display, hide, setARIA, setBuffering.
// JSXGraph 1.13.3: src/renderer/svg.js ->
// setDashStyle, setGradient, setLineCap, setObjectFillColor,
// setObjectStrokeColor, setObjectStrokeWidth, setObjectTransition.
// JSXGraph 1.13.3: src/renderer/svg.js ->
// setShadow, setTabindex, setPropertyPrim, show, suspendRedraw,
// unsuspendRedraw, resize, createTouchpoints, showTouchpoint.
// JSXGraph 1.13.3: src/renderer/svg.js ->
// hideTouchpoint, updateTouchpoint, _getValuesOfDOMElements,
// _getImgDataURL, dumpToDataURI, dumpToCanvas, screenshot.
internal class SvgRenderer(
    containerId: String,
    dimensions: EnvDimensions,
) : AbstractRenderer("svg", containerId, dimensions) {
    internal fun _createArrowHead(
        elementId: String,
        appendix: String,
        type: Int,
    ): GMResult<NativeRendererNode, NativeRendererError> {
        val node = when (
            val result = createPrim(
                primitive = "marker",
                id = "${elementId}Triangle$appendix$type",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        node.properties["arrowType"] = type
        node.properties["appendix"] = appendix
        node.properties["fill"] =
            if (type == 7) "none" else "context-stroke"
        node.properties["stroke"] = "context-stroke"
        node.properties["orient"] = "auto"
        node.properties["markerUnits"] = "strokeWidth"
        return appendChildPrim(node, Int.MAX_VALUE)
    }

    internal fun _setArrowColor(
        node: NativeRendererNode,
        color: JsxGraphColor,
        opacity: Double,
        type: Int,
    ): GMResult<Unit, NativeRendererError> {
        val result =
            if (type == 7) {
                setObjectStrokeColor(node, color, opacity)
            } else {
                setObjectFillColor(node, color, opacity)
            }
        return result
    }

    internal fun _setAttribute(
        node: NativeRendererNode,
        key: String,
        value: Any?,
    ): GMResult<Unit, NativeRendererError> =
        setPropertyPrim(node, key, value)

    internal fun _getValuesOfDOMElements():
        GMResult<Nothing, NativeRendererError> =
        GMResult.Err(
            NativeRendererError.UnsupportedCapability(
                renderer = type,
                capability = "browser DOM value extraction",
            ),
        )

    internal fun _getImgDataURL():
        GMResult<Nothing, NativeRendererError> =
        GMResult.Err(
            NativeRendererError.UnsupportedCapability(
                renderer = type,
                capability = "browser image data URL export",
            ),
        )
}
