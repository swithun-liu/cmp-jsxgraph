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
