/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.renderer

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphArrowHead
import com.swithun.jsxgraph.core.JsxGraphBoundingBox
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphDraftStyle
import com.swithun.jsxgraph.core.JsxGraphElementStyle
import com.swithun.jsxgraph.core.JsxGraphPoint2D
import com.swithun.jsxgraph.core.JsxGraphScene
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import com.swithun.jsxgraph.core.JsxGraphShadow
import com.swithun.jsxgraph.core.utils.EnvDimensions
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NativeRendererTest {
    @Test
    fun renderBuildsLayeredPrimitiveSnapshot() {
        val renderer = CanvasRenderer(
            containerId = "box",
            dimensions = EnvDimensions(640.0, 480.0),
        )
        val scene = JsxGraphScene(
            boundingBox = JsxGraphBoundingBox(-5.0, 5.0, 5.0, -5.0),
            axis = false,
            grid = false,
            keepAspectRatio = false,
            elements = listOf(
                JsxGraphSceneElement.Point(
                    id = "point",
                    name = "A",
                    style = style(layer = 9),
                    coordinates = JsxGraphPoint2D(1.0, 2.0),
                    size = 4.0,
                    face = "cross",
                ),
                JsxGraphSceneElement.Line(
                    id = "line",
                    name = "l",
                    style = style(layer = 7),
                    point1 = JsxGraphPoint2D(-1.0, 0.0),
                    point2 = JsxGraphPoint2D(2.0, 0.0),
                    straightFirst = false,
                    straightLast = false,
                    firstArrow = JsxGraphArrowHead(
                        type = 2,
                        size = 6.0,
                        highlightSize = null,
                    ),
                    lastArrow = JsxGraphArrowHead(
                        type = 7,
                        size = 10.0,
                        highlightSize = 12.0,
                    ),
                ),
                JsxGraphSceneElement.Curve(
                    id = "curve",
                    name = "c",
                    style = style(layer = 5),
                    points = listOf(
                        JsxGraphPoint2D(0.0, 0.0),
                        JsxGraphPoint2D(1.0, 1.0),
                        null,
                        JsxGraphPoint2D(2.0, 2.0),
                    ),
                    bezierDegree = 1,
                    lineCap = "square",
                    firstArrow = JsxGraphArrowHead(
                        type = 4,
                        size = 5.0,
                        highlightSize = null,
                    ),
                    lastArrow = JsxGraphArrowHead(
                        type = 6,
                        size = 7.0,
                        highlightSize = 8.0,
                    ),
                ),
            ),
        )

        val snapshot = assertIs<
            GMResult.Ok<NativeRendererSnapshot>
            >(renderer.render(scene)).value

        assertEquals(listOf("curve", "line", "point"), snapshot.nodes.map { it.id })
        assertEquals("square", snapshot.nodes[0].visual.lineCap)
        assertEquals(
            listOf(2, 1),
            (
                snapshot.nodes[0].geometry as NativeRendererGeometry.Path
                ).subpaths.map(List<*>::size),
        )
        assertEquals(4, snapshot.nodes[0].firstArrow?.type)
        assertEquals(6, snapshot.nodes[0].lastArrow?.type)
        assertEquals(2, snapshot.nodes[1].firstArrow?.type)
        assertEquals(7, snapshot.nodes[1].lastArrow?.type)
        assertEquals("path", snapshot.nodes[2].primitive)
        assertFalse(snapshot.redrawSuspended)
    }

    @Test
    fun primitiveLifecycleRestoresOriginalLayerPosition() {
        val renderer = SvgRenderer(
            containerId = "box",
            dimensions = EnvDimensions(100.0, 80.0),
        )
        val first = assertIs<GMResult.Ok<NativeRendererNode>>(
            renderer.createPrim("line", "first"),
        ).value
        val second = assertIs<GMResult.Ok<NativeRendererNode>>(
            renderer.createPrim("line", "second"),
        ).value
        assertIs<GMResult.Ok<NativeRendererNode>>(
            renderer.appendChildPrim(first, 4),
        )
        assertIs<GMResult.Ok<NativeRendererNode>>(
            renderer.appendChildPrim(second, 4),
        )

        val restore = assertIs<
            GMResult.Ok<() -> GMResult<Unit, NativeRendererError>>
            >(renderer.removeToInsertLater(first)).value
        assertEquals(listOf("second"), renderer.snapshot().nodes.map { it.id })
        assertIs<GMResult.Ok<Unit>>(restore())
        assertEquals(
            listOf("first", "second"),
            renderer.snapshot().nodes.map { it.id },
        )

        assertIs<GMResult.Ok<Unit>>(renderer.setLayer(first, 8))
        assertEquals(
            listOf("second", "first"),
            renderer.snapshot().nodes.map { it.id },
        )
        assertIs<GMResult.Ok<Unit>>(renderer.remove(first))
        assertNull(renderer.getElementById("first"))
        assertIs<GMResult.Err<NativeRendererError.RemovedNode>>(
            renderer.show(first),
        )
    }

    @Test
    fun visualAndTouchpointMutationsAreExplicitState() {
        val renderer = CanvasRenderer(
            containerId = "box",
            dimensions = EnvDimensions(100.0, 80.0),
        )
        val node = assertIs<GMResult.Ok<NativeRendererNode>>(
            renderer.createPrim("path", "p"),
        ).value
        assertIs<GMResult.Ok<NativeRendererNode>>(
            renderer.appendChildPrim(node, 0),
        )

        assertIs<GMResult.Ok<Unit>>(
            renderer.setObjectStrokeWidth(node, 4.0),
        )
        assertIs<GMResult.Ok<Unit>>(
            renderer.setDashStyle(node, dash = 2, dashScale = true),
        )
        assertContentEquals(
            listOf(10.0, 10.0),
            node.visual.dashPattern,
        )
        assertIs<GMResult.Ok<Unit>>(
            renderer.setARIA(node, mapOf("label" to "point A")),
        )
        assertIs<GMResult.Ok<Unit>>(renderer.setTabindex(node, 3))
        assertEquals("point A", node.visual.aria["label"])
        assertEquals(3, node.visual.tabindex)

        assertIs<GMResult.Ok<Unit>>(renderer.createTouchpoints(2))
        assertIs<GMResult.Ok<Unit>>(
            renderer.updateTouchpoint(1, JsxGraphPoint2D(4.0, 7.0)),
        )
        assertIs<GMResult.Ok<Unit>>(renderer.showTouchpoint(1))
        assertTrue(renderer.snapshot().touchpoints[1].visible)
        assertEquals(
            JsxGraphPoint2D(4.0, 7.0),
            renderer.snapshot().touchpoints[1].position,
        )
        assertIs<GMResult.Err<NativeRendererError.InvalidTouchpointIndex>>(
            renderer.hideTouchpoint(2),
        )
    }

    @Test
    fun renderResolvesHighlightDraftAndPortableMetadata() {
        val renderer = SvgRenderer(
            containerId = "box",
            dimensions = EnvDimensions(100.0, 80.0),
        )
        val highlightedStyle = style(layer = 2).copy(
            highlighted = true,
            highlightStrokeColor = JsxGraphColor(1, 2, 3),
            highlightFillColor = JsxGraphColor(4, 5, 6),
            highlightStrokeWidth = 7.0,
            lineCap = "square",
            clipToViewport = false,
            shadow = JsxGraphShadow(enabled = true, blur = 9.0),
            cssClass = "normal",
            highlightCssClass = "hot",
            aria = mapOf("label" to "A"),
            tabIndex = 4,
            transitionDurationMillis = 250.0,
            transitionProperties = listOf("stroke"),
        )
        val highlighted = assertIs<GMResult.Ok<NativeRendererNode>>(
            renderer.drawPoint(
                JsxGraphSceneElement.Point(
                    id = "highlighted",
                    name = "",
                    style = highlightedStyle,
                    coordinates = JsxGraphPoint2D(0.0, 0.0),
                    size = 3.0,
                    face = "o",
                ),
            ),
        ).value.visual

        assertEquals(JsxGraphColor(1, 2, 3), highlighted.strokeColor)
        assertEquals(JsxGraphColor(4, 5, 6), highlighted.fillColor)
        assertEquals(7.0, highlighted.strokeWidth)
        assertEquals("square", highlighted.lineCap)
        assertFalse(highlighted.clipToViewport)
        assertTrue(highlighted.shadow.enabled)
        assertEquals("hot", highlighted.cssClass)
        assertEquals(mapOf("label" to "A"), highlighted.aria)
        assertEquals(4, highlighted.tabindex)
        assertEquals(250.0, highlighted.transitionDuration)
        assertTrue(highlighted.highlighted)

        val draft = assertIs<GMResult.Ok<NativeRendererNode>>(
            renderer.drawPoint(
                JsxGraphSceneElement.Point(
                    id = "draft",
                    name = "",
                    style = highlightedStyle.copy(
                        draft = JsxGraphDraftStyle(
                            enabled = true,
                            strokeColor = JsxGraphColor(10, 20, 30),
                            fillColor = JsxGraphColor(40, 50, 60),
                            strokeOpacity = 0.5,
                            fillOpacity = 0.25,
                            strokeWidth = 3.0,
                        ),
                    ),
                    coordinates = JsxGraphPoint2D(1.0, 1.0),
                    size = 3.0,
                    face = "o",
                ),
            ),
        ).value.visual
        assertEquals(JsxGraphColor(10, 20, 30), draft.strokeColor)
        assertEquals(JsxGraphColor(40, 50, 60), draft.fillColor)
        assertEquals(3.0, draft.strokeWidth)
        assertTrue(draft.draft)
        assertFalse(draft.highlighted)
    }

    @Test
    fun canvasGeometryHelpersFollowUpstreamEquations() {
        val renderer = CanvasRenderer(
            containerId = "box",
            dimensions = EnvDimensions(100.0, 80.0),
        )

        val rotated = renderer._rotatePoint(PI * 0.5, 2.0, 0.0)
        assertEquals(0.0, rotated.x, absoluteTolerance = 1e-12)
        assertEquals(2.0, rotated.y, absoluteTolerance = 1e-12)

        val translated = renderer._translateShape(
            shape = listOf(JsxGraphPoint2D(1.0, 2.0)),
            x = 3.0,
            y = -4.0,
        )
        assertEquals(listOf(JsxGraphPoint2D(4.0, -2.0)), translated)

        val gradient = renderer.updateGradientAngle(
            bounds = NativeRendererBounds(10.0, 20.0, 110.0, 70.0),
            radians = 0.0,
        )
        assertEquals(JsxGraphPoint2D(10.0, 20.0), gradient.first)
        assertEquals(JsxGraphPoint2D(110.0, 20.0), gradient.second)
    }

    @Test
    fun matrixCompositionMatchesAbstractRendererOrder() {
        val renderer = SvgRenderer(
            containerId = "box",
            dimensions = EnvDimensions(100.0, 80.0),
        )
        val translation = arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(3.0, 1.0, 0.0),
            doubleArrayOf(4.0, 0.0, 1.0),
        )
        val result = assertIs<GMResult.Ok<Array<DoubleArray>>>(
            renderer.joinTransforms(
                originX = 0.0,
                originY = 0.0,
                unitX = 1.0,
                unitY = 1.0,
                transformations = listOf(translation),
            ),
        ).value

        assertContentEquals(doubleArrayOf(1.0, 0.0, 0.0), result[0])
        assertContentEquals(doubleArrayOf(3.0, 1.0, 0.0), result[1])
        assertContentEquals(doubleArrayOf(-4.0, 0.0, 1.0), result[2])
    }

    @Test
    fun browserOnlyOperationsAndFreedRendererReturnErrors() {
        val renderer = SvgRenderer(
            containerId = "box",
            dimensions = EnvDimensions(100.0, 80.0),
        )

        assertIs<
            GMResult.Err<NativeRendererError.UnsupportedCapability>
            >(renderer.dumpToDataURI(ignoreTexts = false))
        assertIs<
            GMResult.Err<NativeRendererError.UnsupportedCapability>
            >(renderer._getValuesOfDOMElements())

        renderer.free()
        assertIs<GMResult.Err<NativeRendererError.RendererFreed>>(
            renderer.createPrim("path", "after-free"),
        )
        assertIs<GMResult.Err<NativeRendererError.RendererFreed>>(
            renderer.resize(200.0, 100.0),
        )
    }

    private fun style(layer: Int): JsxGraphElementStyle =
        JsxGraphElementStyle(
            visible = true,
            strokeColor = JsxGraphColor(20, 40, 60),
            fillColor = JsxGraphColor(80, 100, 120),
            strokeWidth = 2.0,
            strokeOpacity = 0.8,
            fillOpacity = 0.4,
            layer = layer,
            strokeDashPattern = listOf(5.0, 5.0),
        )
}
