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
            ),
        ),
        JsxGraphParityCase(
            id = "finite_segment",
            title = "Finite segment",
            source = FINITE_SEGMENT_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "point",
                "line",
                "circle",
                "segment",
            ),
        ),
        JsxGraphParityCase(
            id = "coordinate_parents",
            title = "Coordinate parents",
            source = COORDINATE_PARENTS_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "line",
                "circle",
                "coordinate-parent",
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
                "element-parent",
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
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "A",
      "type": "point",
      "parents": [-4, -2],
      "attributes": {
        "name": "",
        "withLabel": false,
        "face": "o",
        "size": 3,
        "strokeColor": "#6F7780",
        "fillColor": "#6F7780",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "B",
      "type": "point",
      "parents": [3.2, 2.1],
      "attributes": {
        "name": "",
        "withLabel": false,
        "face": "o",
        "size": 6,
        "strokeColor": "#E0A11A",
        "fillColor": "#E0A11A",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "lineAB",
      "type": "line",
      "parents": ["A", "B"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#49545D",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "circle",
      "type": "circle",
      "parents": [[0.5, 0.6], 2.35],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#16877A",
        "fillColor": "none",
        "strokeWidth": 2.5,
        "fixed": true,
        "highlight": false,
        "center": {
          "visible": false
        }
      }
    }
  ]
}
"""

private const val FINITE_SEGMENT_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "A",
      "type": "point",
      "parents": [-4, 2],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 4,
        "strokeColor": "#246BCE",
        "fillColor": "#FCFDFE",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "B",
      "type": "point",
      "parents": [4, 2],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 4,
        "strokeColor": "#246BCE",
        "fillColor": "#FCFDFE",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "segmentAB",
      "type": "line",
      "parents": ["A", "B"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "straightFirst": false,
        "straightLast": false,
        "strokeColor": "#246BCE",
        "strokeWidth": 3,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "circle",
      "type": "circle",
      "parents": [[0, 0], 2],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#16877A",
        "fillColor": "none",
        "strokeWidth": 2.5,
        "fixed": true,
        "highlight": false
      }
    }
  ]
}
"""

private const val COORDINATE_PARENTS_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "axis": true,
  "grid": true,
  "keepAspectRatio": false,
  "objects": [
    {
      "id": "line",
      "type": "line",
      "parents": [[-4, 4], [4, 4]],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#D9553F",
        "strokeWidth": 2.5,
        "fixed": true,
        "highlight": false,
        "point1": {
          "visible": false
        },
        "point2": {
          "visible": false
        }
      }
    },
    {
      "id": "circle",
      "type": "circle",
      "parents": [[0, -1], 1.5],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#16877A",
        "fillColor": "#16877A22",
        "strokeWidth": 2.5,
        "fixed": true,
        "highlight": false,
        "center": {
          "visible": false
        }
      }
    }
  ]
}
"""

private const val SHIFTED_GEOMETRY_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-8, 6, 4, -4],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "A",
      "type": "point",
      "parents": [-7, 3],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 4,
        "strokeColor": "#6F7780",
        "fillColor": "#6F7780",
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "B",
      "type": "point",
      "parents": [3, -2.5],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 6,
        "strokeColor": "#E0A11A",
        "fillColor": "#E0A11A",
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "C",
      "type": "point",
      "parents": [-2, 0],
      "attributes": {
        "name": "",
        "withLabel": false,
        "visible": false,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "lineAB",
      "type": "line",
      "parents": ["A", "B"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#49545D",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "circleC",
      "type": "circle",
      "parents": ["C", 2.5],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#16877A",
        "fillColor": "none",
        "strokeWidth": 2.5,
        "fixed": true,
        "highlight": false
      }
    }
  ]
}
"""
