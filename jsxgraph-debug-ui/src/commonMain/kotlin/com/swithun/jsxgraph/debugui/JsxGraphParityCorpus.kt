/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult

data class JsxGraphParityCase(
    val id: String,
    val title: String,
    val source: String,
    val features: Set<String>,
)

object JsxGraphParityCorpus {
    const val DEFAULT_CASE_ID: String = "baseline_geometry"

    val cases: List<JsxGraphParityCase> = listOf(
        JsxGraphParityCase(
            id = DEFAULT_CASE_ID,
            title = "Baseline geometry",
            source = DEFAULT_PARITY_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "point",
                "line",
                "circle",
                "intersection",
                "curve",
            ),
        ),
        JsxGraphParityCase(
            id = "tangent_intersection",
            title = "Tangent intersection",
            source = TANGENT_INTERSECTION_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "point",
                "line",
                "circle",
                "tangent",
                "curve",
            ),
        ),
        JsxGraphParityCase(
            id = "disjoint_intersection",
            title = "Disjoint intersection",
            source = DISJOINT_INTERSECTION_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "point",
                "line",
                "circle",
                "no-intersection",
                "curve",
            ),
        ),
        JsxGraphParityCase(
            id = "shifted_geometry",
            title = "Shifted geometry",
            source = SHIFTED_GEOMETRY_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "point",
                "line",
                "circle",
                "intersection",
                "curve",
            ),
        ),
    )

    fun find(caseId: String): GMResult<JsxGraphParityCase, String> =
        cases.firstOrNull { parityCase -> parityCase.id == caseId }
            ?.let { parityCase -> GMResult.Ok(parityCase) }
            ?: GMResult.Err("Unknown parity case: $caseId")
}

const val DEFAULT_PARITY_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "fixedPoint": [-4, -2],
  "controlPoint": [3.2, 2.1],
  "circle": {
    "center": [0.5, 0.6],
    "radius": 2.35
  },
  "sine": {
    "amplitude": 2,
    "frequency": 0.8
  },
  "parabola": {
    "quadratic": 0.16,
    "constant": -2.5
  }
}
"""

private const val TANGENT_INTERSECTION_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "fixedPoint": [-4, 2],
  "controlPoint": [4, 2],
  "circle": {
    "center": [0, 0],
    "radius": 2
  },
  "sine": {
    "amplitude": 1.25,
    "frequency": 1.3
  },
  "parabola": {
    "quadratic": -0.1,
    "constant": 2.5
  }
}
"""

private const val DISJOINT_INTERSECTION_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "fixedPoint": [-4, 4],
  "controlPoint": [4, 4],
  "circle": {
    "center": [0, -1],
    "radius": 1.5
  },
  "sine": {
    "amplitude": 0.75,
    "frequency": 1.6
  },
  "parabola": {
    "quadratic": 0.08,
    "constant": -3
  }
}
"""

private const val SHIFTED_GEOMETRY_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "fixedPoint": [-5, 1.5],
  "controlPoint": [4.5, -2.5],
  "circle": {
    "center": [-1.5, -0.5],
    "radius": 2.75
  },
  "sine": {
    "amplitude": 2.8,
    "frequency": 0.45
  },
  "parabola": {
    "quadratic": 0.24,
    "constant": -3.2
  }
}
"""
