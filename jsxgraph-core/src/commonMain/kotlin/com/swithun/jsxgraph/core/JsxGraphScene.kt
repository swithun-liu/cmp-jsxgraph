/*
 * Kotlin translation support for JSXGraph.
 * Upstream: src/base/board.js -> create,
 * src/base/element.js -> visual properties,
 * src/base/point.js, src/base/line.js, src/base/circle.js,
 * src/base/curve.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

/**
 * Platform-independent output of the translated Board and element factories.
 *
 * Compose consumes this model without exposing the still-incomplete mutable
 * Board implementation as public API.
 */
data class JsxGraphScene(
    val boundingBox: JsxGraphBoundingBox,
    val axis: Boolean,
    val grid: Boolean,
    val keepAspectRatio: Boolean,
    val elements: List<JsxGraphSceneElement>,
)

data class JsxGraphBoundingBox(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
)

data class JsxGraphPoint2D(
    val x: Double,
    val y: Double,
)

data class JsxGraphColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255,
) {
    companion object {
        val Transparent = JsxGraphColor(0, 0, 0, 0)
    }
}

data class JsxGraphElementStyle(
    val visible: Boolean,
    val strokeColor: JsxGraphColor,
    val fillColor: JsxGraphColor,
    val strokeWidth: Double,
    val strokeOpacity: Double,
    val fillOpacity: Double,
)

sealed interface JsxGraphSceneElement {
    val id: String
    val name: String
    val style: JsxGraphElementStyle

    data class Point(
        override val id: String,
        override val name: String,
        override val style: JsxGraphElementStyle,
        val coordinates: JsxGraphPoint2D,
        val size: Double,
        val face: String,
    ) : JsxGraphSceneElement

    data class Line(
        override val id: String,
        override val name: String,
        override val style: JsxGraphElementStyle,
        val point1: JsxGraphPoint2D,
        val point2: JsxGraphPoint2D,
        val straightFirst: Boolean,
        val straightLast: Boolean,
    ) : JsxGraphSceneElement

    data class Circle(
        override val id: String,
        override val name: String,
        override val style: JsxGraphElementStyle,
        val center: JsxGraphPoint2D,
        val radius: Double,
    ) : JsxGraphSceneElement

    data class Curve(
        override val id: String,
        override val name: String,
        override val style: JsxGraphElementStyle,
        val points: List<JsxGraphPoint2D?>,
        val bezierDegree: Int,
        val lineCap: String,
    ) : JsxGraphSceneElement
}
