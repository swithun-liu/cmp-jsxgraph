/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.debugui.generated.productionCorpusCases

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
        JsxGraphParityCase(
            id = "curves",
            title = "Curves",
            source = CURVES_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "curve",
                "functiongraph",
                "data-plot",
                "parametric-curve",
            ),
        ),
        JsxGraphParityCase(
            id = "polygons",
            title = "Polygons",
            source = POLYGONS_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "polygon",
                "fill",
                "border",
                "implicit-vertices",
            ),
        ),
        JsxGraphParityCase(
            id = "text",
            title = "Text",
            source = TEXT_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "text",
                "font-size",
                "color",
                "anchors",
                "dynamic-content",
            ),
        ),
        JsxGraphParityCase(
            id = "circular_regions",
            title = "Arcs and sectors",
            source = CIRCULAR_REGIONS_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "arc",
                "sector",
                "angle",
                "cubic-bezier",
                "fill",
                "orientation",
            ),
        ),
    )

    private val productionCases: List<JsxGraphParityCase> =
        productionCorpusCases.map { productionCase ->
            JsxGraphParityCase(
                id = productionCase.id,
                title = productionCase.title,
                source = productionCase.source,
                features = productionCase.features,
            )
        }

    fun find(caseId: String): GMResult<JsxGraphParityCase, String> =
        (cases + productionCases)
            .firstOrNull { parityCase -> parityCase.id == caseId }
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
        "fixed": false,
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

private const val CURVES_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "parabola",
      "type": "functiongraph",
      "parents": ["0.16 * x * x - 2.5", -6, 6],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#D9553F",
        "strokeWidth": 2.5,
        "fillColor": "none",
        "doAdvancedPlot": false,
        "numberPointsHigh": 600,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "parametricCircle",
      "type": "curve",
      "parents": [
        "2 * cos(x) + 0.5",
        "2 * sin(x) + 0.6",
        0,
        6.283185307179586
      ],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#16877A",
        "strokeWidth": 2.5,
        "fillColor": "none",
        "doAdvancedPlot": false,
        "numberPointsHigh": 600,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "dataPlot",
      "type": "curve",
      "parents": [
        [-5, -3.5, -2, -0.5, 1, 2.5, 4, 5.5],
        [2.8, 1.5, 2.2, 0.8, 1.4, 0.2, 1.1, -0.4]
      ],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#246BCE",
        "strokeWidth": 2,
        "fillColor": "none",
        "fixed": true,
        "highlight": false
      }
    }
  ]
}
"""

private const val POLYGONS_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "quadrilateral",
      "type": "polygon",
      "parents": [[-5, -2], [-3.5, 3], [-0.5, 1.8], [-1.2, -2.8]],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#0072B2",
        "strokeWidth": 2,
        "fillColor": "#F0E442",
        "fillOpacity": 0.3,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "borderlessTriangle",
      "type": "polygon",
      "parents": [[1, -2.5], [3.2, 3], [5.2, -1.3]],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#16877A",
        "strokeWidth": 2,
        "fillColor": "#16877A",
        "fillOpacity": 0.25,
        "withLines": false,
        "fixed": true,
        "highlight": false
      }
    }
  ]
}
"""

private const val TEXT_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "leftMiddle",
      "type": "text",
      "parents": [-5, 3, "Left / middle"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "fontSize": 18,
        "fontUnit": "px",
        "strokeColor": "#1D252C",
        "strokeOpacity": 1,
        "anchorX": "left",
        "anchorY": "middle",
        "display": "internal",
        "parse": false,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "centerTop",
      "type": "text",
      "parents": [0, 2, "Centered above"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "fontSize": 24,
        "fontUnit": "px",
        "strokeColor": "#0072B2",
        "strokeOpacity": 0.8,
        "anchorX": "middle",
        "anchorY": "bottom",
        "display": "internal",
        "parse": false,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "rightBottom",
      "type": "text",
      "parents": [5, -2.5, "Right / bottom"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "fontSize": 20,
        "fontUnit": "px",
        "strokeColor": "#D9553F",
        "strokeOpacity": 1,
        "anchorX": "right",
        "anchorY": "top",
        "display": "internal",
        "parse": false,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "number",
      "type": "text",
      "parents": [0, -3.5, 3.141592653589793],
      "attributes": {
        "name": "",
        "withLabel": false,
        "fontSize": 16,
        "fontUnit": "px",
        "strokeColor": "#16877A",
        "anchorX": "middle",
        "anchorY": "middle",
        "display": "internal",
        "parse": false,
        "formatNumber": true,
        "digits": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "driver",
      "type": "point",
      "parents": [2, -1],
      "attributes": {
        "name": "A",
        "withLabel": false,
        "visible": false,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "dynamic",
      "type": "text",
      "parents": [0, -1.5, "A.x = <value>X(A)</value>"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "fontSize": 16,
        "fontUnit": "px",
        "strokeColor": "#6F7780",
        "anchorX": "middle",
        "anchorY": "middle",
        "display": "internal",
        "parse": true,
        "digits": 1,
        "fixed": true,
        "highlight": false
      }
    }
  ]
}
"""

private const val CIRCULAR_REGIONS_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "arcCenter",
      "type": "point",
      "parents": [-3, 1],
      "attributes": {"name": "", "withLabel": false, "visible": false}
    },
    {
      "id": "arcRadius",
      "type": "point",
      "parents": [-1, 1],
      "attributes": {"name": "", "withLabel": false, "visible": false}
    },
    {
      "id": "arcAngle",
      "type": "point",
      "parents": [-2.6, 3.2],
      "attributes": {"name": "", "withLabel": false, "visible": false}
    },
    {
      "id": "arc",
      "type": "arc",
      "parents": ["arcCenter", "arcRadius", "arcAngle"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "selection": "minor",
        "orientation": "counterclockwise",
        "strokeColor": "#D9553F",
        "fillColor": "none",
        "strokeWidth": 3,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "sectorCenter",
      "type": "point",
      "parents": [2.5, 1.5],
      "attributes": {"name": "", "withLabel": false, "visible": false}
    },
    {
      "id": "sectorRadius",
      "type": "point",
      "parents": [4, 1.5],
      "attributes": {"name": "", "withLabel": false, "visible": false}
    },
    {
      "id": "sectorAngle",
      "type": "point",
      "parents": [2.2, 3.2],
      "attributes": {"name": "", "withLabel": false, "visible": false}
    },
    {
      "id": "sector",
      "type": "sector",
      "parents": ["sectorCenter", "sectorRadius", "sectorAngle"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "selection": "minor",
        "orientation": "counterclockwise",
        "strokeColor": "#0072B2",
        "fillColor": "#F0E442",
        "fillOpacity": 0.35,
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "angleFirst",
      "type": "point",
      "parents": [2, -2],
      "attributes": {"name": "", "withLabel": false, "visible": false}
    },
    {
      "id": "angleVertex",
      "type": "point",
      "parents": [0, -2],
      "attributes": {"name": "", "withLabel": false, "visible": false}
    },
    {
      "id": "angleThird",
      "type": "point",
      "parents": [1, -0.5],
      "attributes": {"name": "", "withLabel": false, "visible": false}
    },
    {
      "id": "angle",
      "type": "angle",
      "parents": ["angleFirst", "angleVertex", "angleThird"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "radius": 1.3,
        "type": "sector",
        "orthoType": "sector",
        "selection": "minor",
        "orientation": "counterclockwise",
        "strokeColor": "#E69F00",
        "fillColor": "#E69F00",
        "fillOpacity": 0.3,
        "strokeWidth": 1,
        "fixed": true,
        "highlight": false
      }
    }
  ]
}
"""
