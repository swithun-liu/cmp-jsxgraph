/*
 * Kotlin translation support for JSXGraph curve-class elements.
 * Upstream: src/base/curve.js, src/element/arc.js,
 * src/element/sector.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

internal data class PlotCurveGeometry(
    val points: List<DoubleArray>,
    val numberPoints: Int,
    val bezierDegree: Int,
    val hasSectorLegs: Boolean,
)

internal fun GeometryElement.toPlotCurveGeometry(): PlotCurveGeometry? =
    when (this) {
        is Curve ->
            if (curveType == "plot") {
                PlotCurveGeometry(
                    points = points.map { it.usrCoords.copyOf() },
                    numberPoints = numberPoints,
                    bezierDegree = bezierDegree,
                    hasSectorLegs = false,
                )
            } else {
                null
            }

        is Arc -> PlotCurveGeometry(
            points = points.map { it.usrCoords.copyOf() },
            numberPoints = numberPoints,
            bezierDegree = bezierDegree,
            hasSectorLegs = false,
        )

        is Sector -> PlotCurveGeometry(
            points = points.map { it.usrCoords.copyOf() },
            numberPoints = numberPoints,
            bezierDegree = bezierDegree,
            // JSXGraph: src/base/line.js -> getCurveTangentDir /
            // getCurveNormalDir check c.elType === "sector".
            hasSectorLegs = elType == "sector",
        )

        else -> null
    }
