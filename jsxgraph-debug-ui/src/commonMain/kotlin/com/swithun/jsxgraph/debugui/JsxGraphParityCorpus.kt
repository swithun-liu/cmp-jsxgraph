/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.debugui.generated.productionCorpusCases

enum class JsxGraphParitySuite(
    val label: String,
) {
    Production("Production corpus"),
    Focused("Focused fixture"),
    Custom("Custom source"),
}

data class JsxGraphParityCase(
    val id: String,
    val title: String,
    val scenario: String,
    val source: String,
    val features: Set<String>,
    val suite: JsxGraphParitySuite,
)

object JsxGraphParityCorpus {
    const val DEFAULT_CASE_ID: String = "prod_geometry_segment_network"

    val focusedCases: List<JsxGraphParityCase> = listOf(
        JsxGraphParityCase(
            id = "baseline_geometry",
            title = "Baseline geometry",
            scenario = "Core point, line, and circle geometry on an aspect-locked board.",
            source = DEFAULT_PARITY_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "point",
                "line",
                "circle",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "finite_segment",
            title = "Finite segment",
            scenario = "A bounded segment contrasts finite and infinite line rendering.",
            source = FINITE_SEGMENT_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "point",
                "line",
                "circle",
                "segment",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "fixed_length_segment",
            title = "Fixed-length segment",
            scenario = "A fixed anchor forces a free endpoint to the requested segment length.",
            source = FIXED_LENGTH_SEGMENT_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "point",
                "segment",
                "fixed-length-segment",
                "fixed-point",
                "free-point",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "coordinate_parents",
            title = "Coordinate parents",
            scenario = "Coordinate arrays drive line and circle construction without Point elements.",
            source = COORDINATE_PARENTS_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "line",
                "circle",
                "coordinate-parent",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "shifted_geometry",
            title = "Shifted geometry",
            scenario = "Element references update geometry inside a shifted coordinate viewport.",
            source = SHIFTED_GEOMETRY_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "point",
                "line",
                "circle",
                "element-parent",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "curves",
            title = "Curves",
            scenario = "Function, sampled data, and parametric curves share one comparison board.",
            source = CURVES_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "curve",
                "functiongraph",
                "data-plot",
                "parametric-curve",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "step_functions",
            title = "Step functions",
            scenario = "Static arrays exercise rising, falling, repeated, and mismatched step data.",
            source = STEP_FUNCTIONS_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "curve",
                "stepfunction",
                "data-plot",
                "path-break",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "polygons",
            title = "Polygons",
            scenario = "Filled polygons exercise implicit vertices, borders, and winding.",
            source = POLYGONS_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "polygon",
                "fill",
                "border",
                "implicit-vertices",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "polygonal_chains",
            title = "Polygonal chains",
            scenario = "An open PolygonalChain preserves parent order and updates its unclosed final segment.",
            source = POLYGONAL_CHAINS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "polygonalchain",
                "open-border",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "parallelograms",
            title = "Parallelograms",
            scenario = "Three draggable source points drive a filled Parallelogram and its exposed ParallelPoint.",
            source = PARALLELOGRAMS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "polygon",
                "parallelogram",
                "parallelpoint",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "regular_polygons",
            title = "Regular polygons",
            scenario = "Two draggable source points generate a regular pentagon through chained rotations.",
            source = REGULAR_POLYGONS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "polygon",
                "regularpolygon",
                "implicit-vertices",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "radical_axis",
            title = "Radical axis",
            scenario = "Two draggable circles drive a constrained RadicalAxis through their changing powers.",
            source = RADICAL_AXIS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "circle",
                "line",
                "radicalaxis",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "pole_point",
            title = "Circle-line pole point",
            scenario = "A draggable Circle and Line drive their constrained PolePoint.",
            source = POLE_POINT_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "circle",
                "line",
                "polepoint",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "tangent_polar_circle",
            title = "Circle tangents and polars",
            scenario = "A draggable Circle drives Tangent, Polar, and PolarLine coefficient constructions.",
            source = TANGENT_POLAR_CIRCLE_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "circle",
                "line",
                "tangent",
                "polar",
                "polarline",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "tangent_to_circle",
            title = "Two tangents from one point",
            scenario = "A draggable external Point drives both indexed TangentTo constructions and their exposed polar and contact Points.",
            source = TANGENT_TO_CIRCLE_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "circle",
                "line",
                "tangentto",
                "intersection",
                "dash",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "tangent_line",
            title = "Line tangents and polar alias",
            scenario = "A draggable Line shares its endpoints with Tangent and Polar range variants.",
            source = TANGENT_LINE_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "line",
                "tangent",
                "polar",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "tangent_curve",
            title = "Curve tangents and polar alias",
            scenario = "Function, parametric, and data Curves drive Tangent and Polar constructions.",
            source = TANGENT_CURVE_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "curve",
                "functiongraph",
                "line",
                "tangent",
                "polar",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "normal_constructions",
            title = "Line, circle, and curve normals",
            scenario = "Line, Circle, FunctionGraph, parametric Curve, and data Plot parents drive Normal constructions.",
            source = NORMAL_CONSTRUCTIONS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "line",
                "circle",
                "curve",
                "functiongraph",
                "normal",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "derivative_curve",
            title = "Curve derivative",
            scenario = "A draggable coefficient updates a FunctionGraph and its numerical Derivative.",
            source = DERIVATIVE_CURVE_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "curve",
                "functiongraph",
                "derivative",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "spline_curves",
            title = "Natural and cardinal splines",
            scenario = "Natural and cardinal splines track draggable interpolation points and dynamic tension.",
            source = SPLINE_CURVES_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "curve",
                "spline",
                "cardinalspline",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "riemann_sums",
            title = "Riemann sums",
            scenario = "Single-function and between-function sums render filled bars and track a dynamic rectangle count.",
            source = RIEMANN_SUMS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "curve",
                "riemannsum",
                "fill",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "box_plots",
            title = "Box plots",
            scenario = "Vertical, horizontal, and dynamic BoxPlots compare fill, whiskers, and viewport-sized outliers.",
            source = BOX_PLOTS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "curve",
                "boxplot",
                "fill",
                "path-break",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "combs",
            title = "Comb curves",
            scenario = "Default, reversed, and function-configured Combs compare tooth geometry and driver updates.",
            source = COMBS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "curve",
                "comb",
                "path-break",
                "function-attribute",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "inequalities",
            title = "Line and function inequalities",
            scenario = "Line and segmented FunctionGraph inequalities compare filled regions, parent updates, and dynamic inverse.",
            source = INEQUALITIES_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "line",
                "functiongraph",
                "curve",
                "inequality",
                "fill",
                "path-break",
                "function-attribute",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "vector_fields",
            title = "Vector fields",
            scenario = "Array-returning and component VectorFields compare mesh geometry, pixel-sized arrowheads, and driver updates.",
            source = VECTOR_FIELDS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "curve",
                "vectorfield",
                "path-break",
                "function-parent",
                "function-attribute",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "slope_fields",
            title = "Slope fields",
            scenario = "String and function SlopeFields compare normalized directions, default arrow suppression, and driver updates.",
            source = SLOPE_FIELDS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "curve",
                "slopefield",
                "path-break",
                "function-parent",
                "function-attribute",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "ellipses",
            title = "Ellipses",
            scenario = "A three-Point Ellipse and a numeric-major-axis elliptical arc compare conic geometry and parent updates.",
            source = ELLIPSES_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "curve",
                "ellipse",
                "conic",
                "numeric-parent",
                "parameter-domain",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "hyperbolas",
            title = "Hyperbolas",
            scenario = "A three-Point Hyperbola and a numeric-major-axis branch compare conic geometry and parent updates.",
            source = HYPERBOLAS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "curve",
                "hyperbola",
                "conic",
                "numeric-parent",
                "parameter-domain",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "parabolas",
            title = "Parabolas",
            scenario = "Point-Line and coordinate-parent Parabolas compare directrix geometry, parameter domains, and focus updates.",
            source = PARABOLAS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "line",
                "curve",
                "parabola",
                "conic",
                "coordinate-parent",
                "parameter-domain",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "text",
            title = "Text",
            scenario = "Static and dynamic labels exercise anchors, colors, and type sizing.",
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
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "circular_regions",
            title = "Arcs and sectors",
            scenario = "Arc, sector, and angle regions compare orientation and fill behavior.",
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
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "arc_direction_point",
            title = "Direction-point arc",
            scenario = "A fourth point selects which circular path connects the two arc endpoints.",
            source = ARC_DIRECTION_POINT_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "point",
                "arc",
                "direction-point",
                "cubic-bezier",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "arc_compositions",
            title = "Arc compositions",
            scenario = "One draggable triangle drives Semicircle, CircumcircleArc, MinorArc, and MajorArc.",
            source = ARC_COMPOSITIONS_SOURCE,
            features = setOf(
                "construction-document",
                "point",
                "arc",
                "semicircle",
                "circumcirclearc",
                "minorarc",
                "majorarc",
                "midpoint",
                "circumcenter",
                "direction-point",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "circumcircle_creators",
            title = "Circumcircle creators",
            scenario = "One draggable triangle drives Circumcenter, its alias, and Circumcircle.",
            source = CIRCUMCIRCLE_CREATORS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "circle",
                "circumcenter",
                "circumcirclemidpoint",
                "circumcircle",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "point_reflections",
            title = "Point reflections",
            scenario = "One draggable point drives line reflection, mirror element, and mirror-point alias.",
            source = POINT_REFLECTIONS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "line",
                "reflection",
                "mirrorelement",
                "mirrorpoint",
                "transform",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "bisector_lines",
            title = "Two-line angle bisectors",
            scenario = "Two draggable source lines drive both nested outputs of one BisectorLines composition.",
            source = BISECTOR_LINES_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "line",
                "bisectorlines",
                "composition",
                "nested-attributes",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "sector_compositions",
            title = "Sector compositions",
            scenario = "One draggable triangle drives CircumcircleSector, MinorSector, MajorSector, NonreflexAngle, and ReflexAngle.",
            source = SECTOR_COMPOSITIONS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "sector",
                "angle",
                "circumcirclesector",
                "minorsector",
                "majorsector",
                "nonreflexangle",
                "reflexangle",
                "circumcenter",
                "direction-point",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "jessiecode_native_source",
            title = "Native JessieCode source",
            scenario = "One bounded JessieCode source creates matching official and native geometry.",
            source = JESSIECODE_NATIVE_SOURCE,
            features = setOf(
                "jessiecode",
                "use",
                "point",
                "segment",
                "circle",
                "visual-attributes",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "function_circle_radius",
            title = "Function circle radius",
            scenario = "A native JessieCode function drives a nonnegative circle radius from a Point.",
            source = FUNCTION_CIRCLE_RADIUS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "circle",
                "function-radius",
                "nonnegative-circle-radius",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "transformed_points",
            title = "Transformed points",
            scenario = "Scalar, function, Point, Line, and chained transforms drive native and official Points.",
            source = TRANSFORMED_POINTS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "line",
                "transform",
                "transformed-point",
                "function-transform",
                "transformation-chain",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "point3d_projection",
            title = "Point3D projection",
            scenario = "A parallel View3D projects free, homogeneous, and transformed Point3D elements through their 2D proxies.",
            source = POINT3D_PROJECTION_SOURCE,
            features = setOf(
                "construction-document",
                "view3d",
                "point3d",
                "transform3d",
                "parallel-projection",
                "homogeneous-coordinate",
                "transformed-point",
                "proxy-point",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "spatial_lines_planes",
            title = "Spatial lines and planes",
            scenario = "A parallel View3D projects bounded Line3D, Plane3D outline and Mesh3D wireframe, and Axis3D proxies.",
            source = SPATIAL_LINES_PLANES_SOURCE,
            features = setOf(
                "construction-document",
                "view3d",
                "line3d",
                "plane3d",
                "mesh3d",
                "axis3d",
                "wireframe",
                "parallel-projection",
                "proxy-segment",
                "proxy-curve",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "plane3d_surfaces",
            title = "Plane3D surfaces",
            scenario = "Finite planes use rectangle color arrays, triangle shading, and height colormaps.",
            source = PLANE_3D_SURFACES_SOURCE,
            features = setOf(
                "construction-document",
                "view3d",
                "plane3d",
                "polyhedron3d",
                "face3d",
                "rectangle-tiling",
                "triangle-tiling",
                "color-array",
                "shader",
                "colormap",
                "parallel-projection",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "polyhedron3d_faces",
            title = "Polyhedron3D faces",
            scenario = "A parallel View3D projects a six-face Polyhedron3D with cyclic colors, a per-face override, and depth ordering.",
            source = POLYHEDRON_3D_FACES_SOURCE,
            features = setOf(
                "jessiecode",
                "view3d",
                "polyhedron3d",
                "face3d",
                "parallel-projection",
                "proxy-curve",
                "per-face-attributes",
                "depth-order",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "polygon3d_projection",
            title = "Polygon3D projection",
            scenario = "A parallel View3D projects coordinate-owned and Point3D-backed polygons through ordinary Polygon proxies.",
            source = POLYGON_3D_PROJECTION_SOURCE,
            features = setOf(
                "jessiecode",
                "view3d",
                "point3d",
                "polygon3d",
                "parallel-projection",
                "proxy-polygon",
                "owned-vertices",
                "nested-vertex-style",
                "nested-border-style",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "view3d_default_axes",
            title = "View3D default axes",
            scenario = "A parallel View3D factory creates border axes, Ticks3D curves, and numeric labels.",
            source = VIEW3D_DEFAULT_AXES_SOURCE,
            features = setOf(
                "construction-document",
                "view3d",
                "default-axes",
                "axes3d",
                "ticks3d",
                "text3d",
                "parallel-projection",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "view3d_center_axes",
            title = "View3D center axes",
            scenario = "A parallel View3D factory creates centered axes and the hidden official origin Intersection.",
            source = VIEW3D_CENTER_AXES_SOURCE,
            features = setOf(
                "construction-document",
                "view3d",
                "default-axes",
                "axes3d",
                "center-origin",
                "intersection",
                "parallel-projection",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "function_coordinate_points",
            title = "Function coordinate points",
            scenario = "Array, scalar, and homogeneous JessieCode functions constrain Points from one draggable driver.",
            source = FUNCTION_COORDINATE_POINTS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "segment",
                "function-coordinate",
                "coordinate-array-function",
                "homogeneous-coordinate",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "parallel_constructions",
            title = "Parallel constructions",
            scenario = "A draggable parent drives a ParallelPoint plus finite and ideal Parallel forms.",
            source = PARALLEL_CONSTRUCTIONS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "line",
                "parallelpoint",
                "parallel",
                "finite-parallel",
                "ideal-point",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "line_arrows",
            title = "Line arrows",
            scenario = "Arrow types one through seven plus finite and ideal ArrowParallel forms share static and drag parity.",
            source = LINE_ARROWS_SOURCE,
            features = setOf(
                "construction-document",
                "point",
                "line",
                "arrow",
                "arrowparallel",
                "first-arrow",
                "last-arrow",
                "arrow-types",
                "cubic-bezier",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "triangle_centers",
            title = "Triangle centers",
            scenario = "Three draggable vertices drive a Bisector, Incenter, and Incircle.",
            source = TRIANGLE_CENTERS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "segment",
                "bisector",
                "incenter",
                "incircle",
                "triangle-center",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "intersection_points",
            title = "Intersection points",
            scenario = "A draggable circle drives indexed, other, extended, and clipped Line intersections.",
            source = INTERSECTION_POINTS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "line",
                "segment",
                "circle",
                "intersection",
                "otherintersection",
                "always-intersect",
                "non-real-point",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "intersection_paths",
            title = "Path intersections",
            scenario = "Curve, Arc, Sector, and Polygon paths share indexed and excluded intersections.",
            source = INTERSECTION_PATHS_SOURCE,
            features = setOf(
                "jessiecode",
                "curve",
                "arc",
                "sector",
                "polygon",
                "intersection",
                "otherintersection",
                "path-intersection",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "polygon_path_intersections",
            title = "Polygon path intersections",
            scenario = "A draggable Polygon intersects a Circle and a second Polygon in both parent orders.",
            source = POLYGON_PATH_INTERSECTIONS_SOURCE,
            features = setOf(
                "jessiecode",
                "point",
                "circle",
                "polygon",
                "intersection",
                "path-intersection",
                "reverse-parent-order",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
        JsxGraphParityCase(
            id = "curve_boolean_clipping",
            title = "Curve boolean clipping",
            scenario = "Intersection, Union, and Difference render closed paths while a draggable Polygon vertex updates the union.",
            source = CURVE_BOOLEAN_CLIPPING_SOURCE,
            features = setOf(
                "axis",
                "grid",
                "jessiecode",
                "curve",
                "polygon",
                "curveintersection",
                "curveunion",
                "curvedifference",
                "greiner-hormann",
                "fill",
                "dependency-update",
            ),
            suite = JsxGraphParitySuite.Focused,
        ),
    )

    private val productionCases: List<JsxGraphParityCase> =
        productionCorpusCases.map { productionCase ->
            JsxGraphParityCase(
                id = productionCase.id,
                title = productionCase.title,
                scenario = productionCase.scenario,
                source = productionCase.source,
                features = productionCase.features,
                suite = JsxGraphParitySuite.Production,
            )
        }

    val cases: List<JsxGraphParityCase> = productionCases + focusedCases

    val productionCaseCount: Int = productionCases.size

    val focusedCaseCount: Int = focusedCases.size

    fun find(caseId: String): GMResult<JsxGraphParityCase, String> =
        cases
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
      "type": "segment",
      "parents": ["A", "B"],
      "attributes": {
        "name": "",
        "withLabel": false,
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

private const val FIXED_LENGTH_SEGMENT_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "anchor",
      "type": "point",
      "parents": [-3, -1],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 5,
        "strokeColor": "#314652",
        "fillColor": "#FFFFFF",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "endpoint",
      "type": "point",
      "parents": [-1, 1],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 6,
        "strokeColor": "#B44335",
        "fillColor": "#F4D44D",
        "strokeWidth": 2,
        "fixed": false,
        "highlight": false
      }
    },
    {
      "id": "fixedSegment",
      "type": "segment",
      "parents": ["anchor", "endpoint", -4],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#167C73",
        "strokeWidth": 4,
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

private const val STEP_FUNCTIONS_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "risingSteps",
      "type": "stepfunction",
      "parents": [
        [-5, -3, -1, 1, 3, 5],
        [-3, -1, 2, 0, 3, 1]
      ],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#246BCE",
        "strokeWidth": 4,
        "fillColor": "none",
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "fallingSteps",
      "type": "stepfunction",
      "parents": [
        [-5, -2, 0, 2, 5],
        [4, 2.5, 0.5, -1.5, -3.5]
      ],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#D9553F",
        "strokeWidth": 2.5,
        "fillColor": "none",
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "mismatchedSteps",
      "type": "stepfunction",
      "parents": [
        [-5, -3.5, -2, -0.5],
        [4.5, 3.5]
      ],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#16877A",
        "strokeWidth": 3,
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

private const val POLYGONAL_CHAINS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 5, 6, -5],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(-5, -3) << id: \"A\", name: \"\", withLabel: false, size: 5, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", fixed: true, highlight: false >>;\nB = point(-3, 3) << id: \"B\", name: \"\", withLabel: false, size: 5, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", fixed: true, highlight: false >>;\nC = point(0, -1) << id: \"C\", name: \"\", withLabel: false, size: 5, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", fixed: true, highlight: false >>;\nD = point(4, 3) << id: \"D\", name: \"\", withLabel: false, size: 7, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nchain = polygonalchain(A, B, C, D) << id: \"chain\", name: \"\", withLabel: false, fixed: true, highlight: false >>;"
}
"""

private const val PARALLELOGRAMS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-7, 7, 7, -7],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(-4, -2) << id: \"A\", name: \"\", withLabel: false, size: 5, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", fixed: true, highlight: false >>;\nB = point(-1, 2) << id: \"B\", name: \"\", withLabel: false, size: 5, strokeColor: \"#246BCE\", fillColor: \"#FCFDFE\", fixed: true, highlight: false >>;\nC = point(2, -3) << id: \"C\", name: \"\", withLabel: false, size: 7, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nP = parallelogram(A, B, C) <<\n  id: \"P\", name: \"\", withLabel: false,\n  fillColor: \"#009E73\", fillOpacity: 0.24,\n  fixed: true, highlight: false,\n  parallelPoint: <<\n    id: \"helper\", name: \"\", withLabel: false,\n    size: 7, strokeColor: \"#7B4EA3\", fillColor: \"#7B4EA3\",\n    strokeWidth: 2, fixed: true, highlight: false\n  >>\n>>;"
}
"""

private const val REGULAR_POLYGONS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-7, 6, 7, -6],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(-3, -2) <<\n  id: \"A\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nB = point(0, -2) <<\n  id: \"B\", name: \"\", withLabel: false,\n  size: 7, strokeColor: \"#B44335\", fillColor: \"#F4D44D\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nregular = regularpolygon(A, B, 5) <<\n  id: \"regular\", name: \"\", withLabel: false,\n  fillColor: \"#009E73\", fillOpacity: 0.24,\n  fixed: true, highlight: false,\n  vertices: <<\n    ids: [\"C\", \"D\", \"E\"], name: \"\", withLabel: false,\n    size: 6, strokeColor: \"#7B4EA3\", fillColor: \"#7B4EA3\",\n    strokeWidth: 2, fixed: true, highlight: false\n  >>\n>>;"
}
"""

private const val RADICAL_AXIS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-8, 6, 8, -6],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\ncenter1 = point(-3, -1) << id: \"center1\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", fixed: false, highlight: false >>;\nradius1 = point(-1, -1) << id: \"radius1\", name: \"\", withLabel: false, size: 7, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\ncircle1 = circle(center1, radius1) << id: \"circle1\", name: \"\", withLabel: false, strokeColor: \"#009E73\", strokeWidth: 3, fillColor: \"#009E73\", fillOpacity: 0.08, fixed: true, highlight: false >>;\ncenter2 = point(2, 2) << id: \"center2\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", fixed: false, highlight: false >>;\nradius2 = point(5, 2) << id: \"radius2\", name: \"\", withLabel: false, size: 6, strokeColor: \"#246BCE\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\ncircle2 = circle(center2, radius2) << id: \"circle2\", name: \"\", withLabel: false, strokeColor: \"#0072B2\", strokeWidth: 3, fillColor: \"#0072B2\", fillOpacity: 0.08, fixed: true, highlight: false >>;\naxis = radicalaxis(circle1, circle2) << id: \"axis\", name: \"\", withLabel: false, strokeColor: \"#7B4EA3\", strokeWidth: 4, straightFirst: true, straightLast: true, fixed: true, highlight: false, point1: << id: \"axisPoint1\", name: \"\", withLabel: false >>, point2: << id: \"axisPoint2\", name: \"\", withLabel: false >> >>;"
}
"""

private const val POLE_POINT_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-8, 6, 8, -6],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\ncenter = point(1, 1) << id: \"center\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nradiusPoint = point(3, 1) << id: \"radiusPoint\", name: \"\", withLabel: false, size: 7, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nsourceCircle = circle(center, radiusPoint) << id: \"sourceCircle\", name: \"\", withLabel: false, strokeColor: \"#009E73\", strokeWidth: 3, fillColor: \"#009E73\", fillOpacity: 0.08, fixed: true, highlight: false >>;\nlinePoint1 = point(-1, 4) << id: \"linePoint1\", name: \"\", withLabel: false, size: 6, strokeColor: \"#246BCE\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nlinePoint2 = point(4, -1) << id: \"linePoint2\", name: \"\", withLabel: false, size: 6, strokeColor: \"#D55E00\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nsourceLine = line(linePoint1, linePoint2) << id: \"sourceLine\", name: \"\", withLabel: false, strokeColor: \"#6F7780\", strokeWidth: 3, fixed: true, highlight: false >>;\npole = polepoint(sourceCircle, sourceLine) << id: \"pole\", name: \"\", withLabel: false, size: 9, strokeColor: \"#7B4EA3\", fillColor: \"#7B4EA3\", strokeWidth: 3, fixed: true, highlight: false >>;"
}
"""

private const val TANGENT_POLAR_CIRCLE_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-8, 6, 8, -6],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\ncenter = point(-1, 0) << id: \"center\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nradiusPoint = point(2, 0) << id: \"radiusPoint\", name: \"\", withLabel: false, size: 7, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nsourceCircle = circle(center, radiusPoint) << id: \"sourceCircle\", name: \"\", withLabel: false, strokeColor: \"#49545D\", strokeWidth: 3, fillColor: \"#49545D\", fillOpacity: 0.06, fixed: true, highlight: false >>;\nonCirclePoint = point(2, 0) << id: \"onCirclePoint\", name: \"\", withLabel: false, size: 6, strokeColor: \"#0072B2\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\noffCirclePoint = point(4, 3) << id: \"offCirclePoint\", name: \"\", withLabel: false, size: 6, strokeColor: \"#D55E00\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nreversePoint = point(-4, 2) << id: \"reversePoint\", name: \"\", withLabel: false, size: 6, strokeColor: \"#009E73\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\ntangentLine = tangent(sourceCircle, onCirclePoint) << id: \"tangentLine\", name: \"\", withLabel: false, strokeColor: \"#0072B2\", strokeWidth: 4, fixed: true, highlight: false, point1: << id: \"tangentPoint1\", name: \"\", withLabel: false >>, point2: << id: \"tangentPoint2\", name: \"\", withLabel: false >> >>;\npolarAlias = polar(offCirclePoint, sourceCircle) << id: \"polarAlias\", name: \"\", withLabel: false, strokeColor: \"#D55E00\", strokeWidth: 4, fixed: true, highlight: false, point1: << id: \"polarPoint1\", name: \"\", withLabel: false >>, point2: << id: \"polarPoint2\", name: \"\", withLabel: false >> >>;\npolarLine = polarline(reversePoint, sourceCircle) << id: \"polarLine\", name: \"\", withLabel: false, strokeColor: \"#009E73\", strokeWidth: 4, fixed: true, highlight: false, point1: << id: \"polarLinePoint1\", name: \"\", withLabel: false >>, point2: << id: \"polarLinePoint2\", name: \"\", withLabel: false >> >>;"
}
"""

private const val TANGENT_TO_CIRCLE_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-8, 6, 8, -6],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\ncenter = point(1, 1) << id: \"center\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\nradiusPoint = point(4, 1) << id: \"radiusPoint\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\nsourceCircle = circle(center, radiusPoint) << id: \"sourceCircle\", name: \"\", withLabel: false, strokeColor: \"#49545D\", strokeWidth: 3, fillColor: \"#49545D\", fillOpacity: 0.06, fixed: true, highlight: false >>;\nsourcePoint = point(5, 4) << id: \"sourcePoint\", name: \"\", withLabel: false, size: 8, strokeColor: \"#7B4EA3\", fillColor: \"#F4D44D\", strokeWidth: 3, fixed: false, highlight: false >>;\nfirstTangent = tangentto(sourceCircle, sourcePoint, 0) << id: \"firstTangent\", name: \"\", withLabel: false, strokeColor: \"#0072B2\", strokeWidth: 5, fixed: true, highlight: false, point1: << id: \"firstTangentPoint1\", name: \"\", withLabel: false >>, point2: << id: \"firstTangentPoint2\", name: \"\", withLabel: false >>, polar: << id: \"firstPolar\", name: \"\", withLabel: false, visible: true, strokeColor: \"#6F7780\", strokeWidth: 2, dash: 3, point1: << id: \"firstPolarPoint1\", name: \"\", withLabel: false >>, point2: << id: \"firstPolarPoint2\", name: \"\", withLabel: false >> >>, point: << id: \"firstIntersection\", name: \"\", withLabel: false, visible: true, size: 7, strokeColor: \"#0072B2\", fillColor: \"#FCFDFE\", strokeWidth: 3, fixed: true >> >>;\nsecondTangent = tangentto(sourceCircle, sourcePoint, 1) << id: \"secondTangent\", name: \"\", withLabel: false, strokeColor: \"#D55E00\", strokeWidth: 5, fixed: true, highlight: false, point1: << id: \"secondTangentPoint1\", name: \"\", withLabel: false >>, point2: << id: \"secondTangentPoint2\", name: \"\", withLabel: false >>, polar: << id: \"secondPolar\", name: \"\", withLabel: false, visible: false, point1: << id: \"secondPolarPoint1\", name: \"\", withLabel: false >>, point2: << id: \"secondPolarPoint2\", name: \"\", withLabel: false >> >>, point: << id: \"secondIntersection\", name: \"\", withLabel: false, visible: true, size: 7, strokeColor: \"#D55E00\", fillColor: \"#FCFDFE\", strokeWidth: 3, fixed: true >> >>;"
}
"""

private const val TANGENT_LINE_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-8, 6, 8, -6],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(-4, -2) << id: \"A\", name: \"\", withLabel: false, size: 7, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nB = point(3, 2) << id: \"B\", name: \"\", withLabel: false, size: 7, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nsourceLine = line(A, B) << id: \"sourceLine\", name: \"\", withLabel: false, strokeColor: \"#6F7780\", strokeWidth: 8, fixed: true, highlight: false >>;\nP = point(-1, 4) << id: \"P\", name: \"\", withLabel: false, size: 6, strokeColor: \"#7B4EA3\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\ntangentForward = tangent(sourceLine, P) << id: \"tangentForward\", name: \"\", withLabel: false, strokeColor: \"#0072B2\", strokeWidth: 6, straightFirst: false, straightLast: true, fixed: true, highlight: false, point1: << id: \"A\", name: \"ignored\" >>, point2: << id: \"B\", name: \"ignored\" >> >>;\npolarReverse = polar(P, sourceLine) << id: \"polarReverse\", name: \"\", withLabel: false, strokeColor: \"#D55E00\", strokeWidth: 4, straightFirst: true, straightLast: false, fixed: true, highlight: false >>;\ntangentSegment = tangent(P, sourceLine) << id: \"tangentSegment\", name: \"\", withLabel: false, strokeColor: \"#009E73\", strokeWidth: 2, straightFirst: false, straightLast: false, fixed: true, highlight: false >>;"
}
"""

private const val TANGENT_CURVE_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-8, 6, 8, -6],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nfunctionCurve = functiongraph(\"0.35 * (x + 4) * (x + 4) - 3\", -7, -1) << id: \"functionCurve\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#6F7780\", strokeWidth: 3, fixed: true, highlight: false >>;\nfunctionPoint = point(-5.5, 2.5) << id: \"functionPoint\", name: \"\", withLabel: false, size: 7, strokeColor: \"#0072B2\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nfunctionTangent = tangent(functionCurve, functionPoint) << id: \"functionTangent\", name: \"\", withLabel: false, strokeColor: \"#0072B2\", strokeWidth: 5, straightFirst: true, straightLast: true, fixed: true, highlight: false, point1: << id: \"functionTangentPoint1\", name: \"\", withLabel: false >>, point2: << id: \"functionTangentPoint2\", name: \"\", withLabel: false >> >>;\nparametricCurve = curve(\"2 * cos(x) + 3.5\", \"1.4 * sin(x) - 1\", 0, 6.283185307179586) << id: \"parametricCurve\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#6F7780\", strokeWidth: 3, fixed: true, highlight: false >>;\nparametricPoint = point(6, 1.2) << id: \"parametricPoint\", name: \"\", withLabel: false, size: 7, strokeColor: \"#D55E00\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nparametricPolar = polar(parametricPoint, parametricCurve) << id: \"parametricPolar\", name: \"\", withLabel: false, strokeColor: \"#D55E00\", strokeWidth: 5, straightFirst: false, straightLast: true, fixed: true, highlight: false, point1: << id: \"parametricPolarPoint1\", name: \"\", withLabel: false >>, point2: << id: \"parametricPolarPoint2\", name: \"\", withLabel: false >> >>;\nplotCurve = curve([-3, -1, 1, 3, 6], [4, 1, 4, 2, 4]) << id: \"plotCurve\", name: \"\", withLabel: false, strokeColor: \"#16877A\", strokeWidth: 4, fixed: true, highlight: false >>;\nplotPoint = point(0, 3.5) << id: \"plotPoint\", name: \"\", withLabel: false, size: 8, strokeColor: \"#7B4EA3\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nplotTangent = tangent(plotCurve, plotPoint) << id: \"plotTangent\", name: \"\", withLabel: false, strokeColor: \"#7B4EA3\", strokeWidth: 6, straightFirst: false, straightLast: false, fixed: true, highlight: false, point1: << id: \"plotTangentPoint1\", name: \"\", withLabel: false >>, point2: << id: \"plotTangentPoint2\", name: \"\", withLabel: false >> >>;"
}
"""

private const val NORMAL_CONSTRUCTIONS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-10, 7, 10, -7],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(-9, 5) << id: \"A\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nB = point(-6, 3) << id: \"B\", name: \"\", withLabel: false, size: 7, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nsourceLine = line(A, B) << id: \"sourceLine\", name: \"\", withLabel: false, strokeColor: \"#6F7780\", strokeWidth: 3, fixed: true, highlight: false >>;\nlinePoint = point(-8, 2) << id: \"linePoint\", name: \"\", withLabel: false, size: 6, strokeColor: \"#0072B2\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nlineNormal = normal(sourceLine, linePoint) << id: \"lineNormal\", name: \"\", withLabel: false, strokeColor: \"#0072B2\", strokeWidth: 4, fixed: true, highlight: false, point: << id: \"lineNormalPoint\", name: \"\", withLabel: false >> >>;\ncenter = point(-2, 4) << id: \"center\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nradiusPoint = point(-0.5, 4) << id: \"radiusPoint\", name: \"\", withLabel: false, size: 6, strokeColor: \"#D55E00\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nsourceCircle = circle(center, radiusPoint) << id: \"sourceCircle\", name: \"\", withLabel: false, strokeColor: \"#6F7780\", strokeWidth: 3, fillColor: \"#6F7780\", fillOpacity: 0.05, fixed: true, highlight: false >>;\ncirclePoint = point(-1, 5) << id: \"circlePoint\", name: \"\", withLabel: false, size: 6, strokeColor: \"#D55E00\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\ncircleNormal = normal(circlePoint, sourceCircle) << id: \"circleNormal\", name: \"\", withLabel: false, strokeColor: \"#D55E00\", strokeWidth: 4, fixed: true, highlight: false >>;\nfunctionCurve = functiongraph(\"0.3 * (x - 5) * (x - 5) - 5\", 2, 8) << id: \"functionCurve\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#6F7780\", strokeWidth: 3, fixed: true, highlight: false >>;\nfunctionPoint = point(3, -3) << id: \"functionPoint\", name: \"\", withLabel: false, size: 7, strokeColor: \"#009E73\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nfunctionNormal = normal(functionCurve, functionPoint) << id: \"functionNormal\", name: \"\", withLabel: false, strokeColor: \"#009E73\", strokeWidth: 4, fixed: true, highlight: false, point1: << id: \"functionNormalPoint1\", name: \"\", withLabel: false >>, point2: << id: \"functionNormalPoint2\", name: \"\", withLabel: false >> >>;\nparameterCurve = curve(\"1.8 * cos(x) - 6\", \"sin(x) - 3\", 0, 6.283185307179586) << id: \"parameterCurve\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#6F7780\", strokeWidth: 3, fixed: true, highlight: false >>;\nparameterPoint = point(-8, -2) << id: \"parameterPoint\", name: \"\", withLabel: false, size: 7, strokeColor: \"#7B4EA3\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nparameterNormal = normal(parameterPoint, parameterCurve) << id: \"parameterNormal\", name: \"\", withLabel: false, strokeColor: \"#7B4EA3\", strokeWidth: 4, fixed: true, highlight: false, point1: << id: \"parameterNormalPoint1\", name: \"\", withLabel: false >>, point2: << id: \"parameterNormalPoint2\", name: \"\", withLabel: false >> >>;\nplotCurve = curve([-1, 1, 3], [-4, -2, -4]) << id: \"plotCurve\", name: \"\", withLabel: false, strokeColor: \"#16877A\", strokeWidth: 3, fixed: true, highlight: false >>;\nplotPoint = point(1, -1) << id: \"plotPoint\", name: \"\", withLabel: false, size: 7, strokeColor: \"#C65D21\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nplotNormal = normal(plotCurve, plotPoint) << id: \"plotNormal\", name: \"\", withLabel: false, strokeColor: \"#C65D21\", strokeWidth: 4, straightFirst: false, straightLast: true, fixed: true, highlight: false, point1: << id: \"plotNormalPoint1\", name: \"\", withLabel: false >>, point2: << id: \"plotNormalPoint2\", name: \"\", withLabel: false >> >>;"
}
"""

private const val DERIVATIVE_CURVE_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-8, 6, 8, -6],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(0.25, 5) << id: \"A\", name: \"\", withLabel: false, size: 7, strokeColor: \"#49545D\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nsourceCurve = functiongraph(\"A.X() * (x + 2) * (x + 2) - 2.5\", -7, 3) << id: \"sourceCurve\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#6F7780\", strokeWidth: 4, fixed: true, highlight: false >>;\nderivativeCurve = derivative(sourceCurve) << id: \"derivativeCurve\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#D55E00\", strokeWidth: 6, fixed: true, highlight: false >>;"
}
"""

private const val SPLINE_CURVES_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-10, 7, 10, -7],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(-8, 1) << id: \"A\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nB = point(-6, 4) << id: \"B\", name: \"\", withLabel: false, size: 7, strokeColor: \"#0072B2\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nC = point(-4, -2) << id: \"C\", name: \"\", withLabel: false, size: 7, strokeColor: \"#D55E00\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nD = point(-1, 2) << id: \"D\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nnaturalSpline = spline(A, B, C, D) << id: \"naturalSpline\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#0072B2\", strokeWidth: 5, fixed: true, highlight: false >>;\nE = point(1, -1) << id: \"E\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nF = point(3, 3) << id: \"F\", name: \"\", withLabel: false, size: 7, strokeColor: \"#009E73\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nG = point(6, -2) << id: \"G\", name: \"\", withLabel: false, size: 7, strokeColor: \"#D55E00\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\nH = point(9, 2) << id: \"H\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: false, highlight: false >>;\ntension = point(0.35, -5.5) << id: \"tension\", name: \"\", withLabel: false, size: 7, strokeColor: \"#7B4EA3\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\ncardinalSpline = cardinalspline([E, F, G, H], function () { return tension.X(); }, \"centripetal\") << id: \"cardinalSpline\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#009E73\", strokeWidth: 5, fixed: true, highlight: false >>;"
}
"""

private const val RIEMANN_SUMS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-8, 7, 8, -7],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nupperLeft = function (x) { return 0.3 * (x + 4) * (x + 4) + 1; };\nsingle = riemannsum(upperLeft, 5, \"middle\", -7, -1) << id: \"single\", name: \"\", withLabel: false, strokeColor: \"#0072B2\", strokeWidth: 3, fillColor: \"#F0E442\", fillOpacity: 0.45, fixed: true, highlight: false >>;\nbars = point(4, -6) << id: \"bars\", name: \"\", withLabel: false, size: 7, strokeColor: \"#7B4EA3\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nlowerRight = function (x) { return -1.5 + 0.15 * x; };\nupperRight = function (x) { return 3.5 - 0.18 * (x - 4) * (x - 4); };\nbetween = riemannsum([lowerRight, upperRight], \"bars.X()\", \"trapezoidal\", 1, 7) << id: \"between\", name: \"\", withLabel: false, strokeColor: \"#009E73\", strokeWidth: 3, fillColor: \"#56B4E9\", fillOpacity: 0.35, fixed: true, highlight: false >>;"
}
"""

private const val BOX_PLOTS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-10, 7, 10, -7],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nstaticVertical = boxplot([-4, -2, 0, 2, 4, [-5.5, 5.5]], -6, 3) << id: \"staticVertical\", name: \"\", withLabel: false, strokeColor: \"#0072B2\", fillColor: \"#56B4E9\", fillOpacity: 0.3, strokeWidth: 3, outlier: << face: \"circle\", size: 6 >>, fixed: true, highlight: false >>;\nstaticHorizontal = boxplot([-4, -2, 0, 2, 4, [-6, 6]], 0, 2) << id: \"staticHorizontal\", name: \"\", withLabel: false, dir: \"horizontal\", smallWidth: 0.35, strokeColor: \"#009E73\", fillColor: \"#009E73\", fillOpacity: 0.2, strokeWidth: 3, outlier: << face: \"square\", size: 5 >>, fixed: true, highlight: false >>;\ndriver = point(5, -5.5) << id: \"driver\", name: \"\", withLabel: false, size: 7, strokeColor: \"#7B4EA3\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\ndynamicBox = boxplot([\"driver.X() - 9\", \"driver.X() - 7\", \"driver.X() - 5\", \"driver.X() - 3\", \"driver.X() - 1\", [-5.5, 5.5]], \"driver.X()\", \"0.5 * driver.X()\") << id: \"dynamicBox\", name: \"\", withLabel: false, smallWidth: 0.7, strokeColor: \"#D55E00\", fillColor: \"#E69F00\", fillOpacity: 0.25, strokeWidth: 3, outlier: << face: \"plus\", size: 7 >>, fixed: true, highlight: false >>;"
}
"""

private const val COMBS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-10, 7, 10, -7],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nstaticComb = comb([-8, 4], [-2, 4]) << id: \"staticComb\", name: \"\", withLabel: false, frequency: 0.75, width: 0.9, angle: 1.0471975511965976, fixed: true, highlight: false >>;\nreverseComb = comb([1, 4], [8, 4]) << id: \"reverseComb\", name: \"\", withLabel: false, frequency: 1, width: 1, angle: 0.7853981633974483, reverse: true, strokeColor: \"#D55E00\", strokeWidth: 4, fixed: true, highlight: false >>;\nA = point(-7, -2.5) << id: \"A\", name: \"\", withLabel: false, size: 5, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\nB = point(7, -2.5) << id: \"B\", name: \"\", withLabel: false, size: 5, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\ndriver = point(4, -5.8) << id: \"driver\", name: \"\", withLabel: false, size: 7, strokeColor: \"#7B4EA3\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\ndynamicComb = comb(A, B) << id: \"dynamicComb\", name: \"\", withLabel: false, frequency: function () { return 0.6 + 0.1 * driver.X(); }, width: function () { return 0.4 + 0.1 * driver.X(); }, angle: function () { return 0.6 + 0.05 * driver.X(); }, reverse: function () { return driver.X() > 5; }, strokeColor: \"#009E73\", strokeWidth: 4, fixed: true, highlight: false >>;"
}
"""

private const val INEQUALITIES_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-10, 7, 10, -7],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nlineA = point(-8, -3) << id: \"lineA\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\nlineB = point(-3, 3) << id: \"lineB\", name: \"\", withLabel: false, size: 7, strokeColor: \"#0072B2\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nsourceLine = line(lineA, lineB) << id: \"sourceLine\", name: \"\", withLabel: false, strokeColor: \"#0072B2\", strokeWidth: 4, layer: 7, fixed: true, highlight: false >>;\nlineRegion = inequality(sourceLine) << id: \"lineRegion\", name: \"\", withLabel: false, fillColor: \"#56B4E9\", fillOpacity: 0.24, layer: 4, fixed: true, highlight: false >>;\nfunctionDriver = point(1, -5.8) << id: \"functionDriver\", name: \"\", withLabel: false, size: 7, strokeColor: \"#7B4EA3\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nsourceFunction = functiongraph(\"x == 4 ? 0 / 0 : 0.25 * functionDriver.X() * (x - 4) * (x - 4) - 2\", 0, 8) << id: \"sourceFunction\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#D55E00\", strokeWidth: 4, layer: 7, fixed: true, highlight: false >>;\nfunctionRegion = inequality(sourceFunction) << id: \"functionRegion\", name: \"\", withLabel: false, inverse: function () { return functionDriver.X() > 1.5; }, fillColor: \"#E69F00\", fillOpacity: 0.28, layer: 4, fixed: true, highlight: false >>;"
}
"""

private const val VECTOR_FIELDS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-10, 7, 10, -7],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nstaticField = vectorfield(function (x, y) { return [0.22 * y, -0.22 * (x + 5)]; }, [-9, 4, -1], [-4, 4, 4]) << id: \"staticField\", name: \"\", withLabel: false, scale: 1, arrowHead: << enabled: false >>, strokeColor: \"#009E73\", strokeWidth: 2, fixed: true, highlight: false >>;\ndriver = point(3, -5.8) << id: \"driver\", name: \"\", withLabel: false, size: 7, strokeColor: \"#7B4EA3\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\ndynamicField = vectorfield([function (x, y) { return 0.18 * driver.X() * y; }, function (x, y) { return -0.18 * (x - 5); }], [1, function () { return driver.X() < 5 ? 4 : 8; }, 9], [-4, 4, 4]) << id: \"dynamicField\", name: \"\", withLabel: false, scale: function () { return 0.35 + 0.05 * driver.X(); }, arrowHead: << enabled: function () { return driver.X() < 5; }, size: 7, angle: 0.39269908169872414 >>, strokeColor: \"#D55E00\", strokeWidth: 2, fixed: true, highlight: false >>;"
}
"""

private const val SLOPE_FIELDS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-10, 7, 10, -7],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nstaticField = slopefield(\"0.3 * x - 0.15 * y\", [-9, 4, -1], [-4, 4, 4]) << id: \"staticField\", name: \"\", withLabel: false, scale: 0.8, strokeColor: \"#009E73\", strokeWidth: 2, fixed: true, highlight: false >>;\ndriver = point(3, -5.8) << id: \"driver\", name: \"\", withLabel: false, size: 7, strokeColor: \"#7B4EA3\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\ndynamicField = slopefield(function (x, y) { return 0.12 * driver.X() * (x - 5) - 0.2 * y; }, [1, function () { return driver.X() < 5 ? 4 : 8; }, 9], [-4, 4, 4]) << id: \"dynamicField\", name: \"\", withLabel: false, scale: function () { return 0.45 + 0.05 * driver.X(); }, arrowHead: << enabled: function () { return driver.X() < 5; }, size: 7, angle: 0.39269908169872414 >>, strokeColor: \"#D55E00\", strokeWidth: 2, fixed: true, highlight: false >>;"
}
"""

private const val ELLIPSES_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-10, 7, 10, -7],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nF1 = point(-7, 1) << id: \"F1\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\nF2 = point(-3, 1) << id: \"F2\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\nC = point(-5, 4) << id: \"C\", name: \"\", withLabel: false, size: 8, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", strokeWidth: 3, fixed: false, highlight: false >>;\npointEllipse = ellipse(F1, F2, C) << id: \"pointEllipse\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 192, strokeColor: \"#16877A\", strokeWidth: 5, fixed: true, highlight: false >>;\nN1 = point(2, -1) << id: \"N1\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\nN2 = point(6, -1) << id: \"N2\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\nnumericArc = ellipse(N1, N2, 6, 0, PI) << id: \"numericArc\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#D55E00\", strokeWidth: 4, fixed: true, highlight: false >>;"
}
"""

private const val HYPERBOLAS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-10, 7, 10, -7],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nF1 = point(-8, 1) << id: \"F1\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\nF2 = point(-2, 1) << id: \"F2\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\nC = point(0, 3) << id: \"C\", name: \"\", withLabel: false, size: 8, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", strokeWidth: 3, fixed: false, highlight: false >>;\npointLeft = hyperbola(F1, F2, C, -2.2, 2.2) << id: \"pointLeft\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#16877A\", strokeWidth: 5, fixed: true, highlight: false >>;\npointRight = hyperbola(F1, F2, C, 2.9, 3.4) << id: \"pointRight\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#16877A\", strokeWidth: 5, fixed: true, highlight: false >>;\nN1 = point(2, -2) << id: \"N1\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\nN2 = point(8, -2) << id: \"N2\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\nnumericBranch = hyperbola(N1, N2, 4, -1.2, 1.2) << id: \"numericBranch\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#D55E00\", strokeWidth: 4, fixed: true, highlight: false >>;"
}
"""

private const val PARABOLAS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-10, 7, 10, -7],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\ndirectrixPoint1 = point(-7, 6) << id: \"directrixPoint1\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\ndirectrixPoint2 = point(-7, -6) << id: \"directrixPoint2\", name: \"\", withLabel: false, size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", strokeWidth: 2, fixed: true, highlight: false >>;\ndirectrix = line(directrixPoint1, directrixPoint2) << id: \"directrix\", name: \"\", withLabel: false, strokeColor: \"#6F7780\", strokeWidth: 3, dash: 2, fixed: true, highlight: false >>;\nfocus = point(-4, 1) << id: \"focus\", name: \"\", withLabel: false, size: 8, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", strokeWidth: 3, fixed: false, highlight: false >>;\npointParabola = parabola(focus, directrix, -1.2, 1.2) << id: \"pointParabola\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 192, strokeColor: \"#16877A\", strokeWidth: 5, fixed: true, highlight: false >>;\ncoordinateParabola = parabola([4, -1], [[7, -6], [7, 6]], -1.2, 1.2) << id: \"coordinateParabola\", name: \"\", withLabel: false, doAdvancedPlot: false, numberPointsHigh: 128, strokeColor: \"#D55E00\", strokeWidth: 4, fixed: true, highlight: false >>;"
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

private const val ARC_DIRECTION_POINT_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-4, 4, 4, -4],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "center",
      "type": "point",
      "parents": [0, 0],
      "attributes": {
        "name": "",
        "withLabel": false,
        "face": "o",
        "size": 3,
        "strokeColor": "#6F7780",
        "fillColor": "#6F7780",
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "start",
      "type": "point",
      "parents": [2.6, 0],
      "attributes": {
        "name": "",
        "withLabel": false,
        "face": "o",
        "size": 4,
        "strokeColor": "#246BCE",
        "fillColor": "#FCFDFE",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "end",
      "type": "point",
      "parents": [0, 2.6],
      "attributes": {
        "name": "",
        "withLabel": false,
        "face": "o",
        "size": 4,
        "strokeColor": "#D9553F",
        "fillColor": "#FCFDFE",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "direction",
      "type": "point",
      "parents": [0, -2.6],
      "attributes": {
        "name": "",
        "withLabel": false,
        "face": "o",
        "size": 4,
        "strokeColor": "#E69F00",
        "fillColor": "#E69F00",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "directionArc",
      "type": "arc",
      "parents": ["center", "start", "end", "direction"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "useDirection": true,
        "selection": "auto",
        "orientation": "counterclockwise",
        "strokeColor": "#16877A",
        "fillColor": "none",
        "strokeWidth": 4,
        "fixed": true,
        "highlight": false
      }
    }
  ]
}
"""

private const val ARC_COMPOSITIONS_SOURCE: String = """
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
      "parents": [-4, -1],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 6,
        "strokeColor": "#49545D",
        "fillColor": "#FCFDFE",
        "strokeWidth": 2,
        "fixed": false,
        "highlight": false
      }
    },
    {
      "id": "B",
      "type": "point",
      "parents": [1, 4],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 7,
        "strokeColor": "#9A4E1F",
        "fillColor": "#F4D44D",
        "strokeWidth": 2,
        "fixed": false,
        "highlight": false
      }
    },
    {
      "id": "C",
      "type": "point",
      "parents": [5, -2],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 6,
        "strokeColor": "#246BCE",
        "fillColor": "#FCFDFE",
        "strokeWidth": 2,
        "fixed": false,
        "highlight": false
      }
    },
    {
      "id": "semicircleArc",
      "type": "semicircle",
      "parents": ["A", "B"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "selection": "major",
        "strokeColor": "#D55E00",
        "fillColor": "none",
        "strokeWidth": 8,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "circumcircleRoute",
      "type": "circumcirclearc",
      "parents": ["A", "B", "C"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "useDirection": false,
        "strokeColor": "#0072B2",
        "fillColor": "none",
        "strokeWidth": 6,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "minorRoute",
      "type": "minorarc",
      "parents": ["A", "B", "C"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "selection": "major",
        "strokeColor": "#009E73",
        "fillColor": "none",
        "strokeWidth": 4,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "majorRoute",
      "type": "majorarc",
      "parents": ["A", "B", "C"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "selection": "minor",
        "strokeColor": "#CC79A7",
        "fillColor": "none",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    }
  ]
}
"""

private const val CIRCUMCIRCLE_CREATORS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 5, 6, -5],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(-4, -2) <<\n  id: \"A\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nB = point(0, 4) <<\n  id: \"B\", name: \"\", withLabel: false,\n  size: 7, strokeColor: \"#9A4E1F\", fillColor: \"#F4D44D\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nC = point(4, -3) <<\n  id: \"C\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#246BCE\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\ncenter = circumcenter(A, B, C) <<\n  id: \"center\", name: \"\", withLabel: false,\n  size: 10, strokeColor: \"#7B4EA3\", fillColor: \"#FCFDFE\",\n  strokeWidth: 3, fixed: true, highlight: false\n>>;\nalias = circumcirclemidpoint(A, B, C) <<\n  id: \"alias\", name: \"\", withLabel: false,\n  size: 4, strokeColor: \"#E69F00\", fillColor: \"#E69F00\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\noutput = circumcircle(A, B, C) <<\n  id: \"output\", name: \"\", withLabel: false,\n  strokeColor: \"#16877A\", fillColor: \"#16877A\",\n  fillOpacity: 0.08, strokeWidth: 4,\n  fixed: true, highlight: false\n>>;"
}
"""

private const val POINT_REFLECTIONS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 6, 6, -6],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nsource = point(-3, 1) <<\n  id: \"source\", name: \"\", withLabel: false,\n  size: 7, strokeColor: \"#246BCE\", fillColor: \"#246BCE\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nmirror = point(0, -1) <<\n  id: \"mirror\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#16877A\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\naxisFirst = point(0, -5) <<\n  id: \"axisFirst\", name: \"\", withLabel: false,\n  visible: false, fixed: true, highlight: false\n>>;\naxisSecond = point(0, 5) <<\n  id: \"axisSecond\", name: \"\", withLabel: false,\n  visible: false, fixed: true, highlight: false\n>>;\naxis = line(axisFirst, axisSecond) <<\n  id: \"axis\", name: \"\", withLabel: false,\n  strokeColor: \"#6F7780\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;\nreflected = reflection(source, axis) <<\n  id: \"reflected\", name: \"\", withLabel: false,\n  size: 7, strokeColor: \"#D9553F\", fillColor: \"#D9553F\",\n  strokeWidth: 2, highlight: false\n>>;\nmirrored = mirrorelement(source, mirror) <<\n  id: \"mirrored\", name: \"\", withLabel: false,\n  size: 10, strokeColor: \"#7B4EA3\", fillColor: \"#FCFDFE\",\n  strokeWidth: 3, highlight: false\n>>;\nalias = mirrorpoint(source, mirror) <<\n  id: \"alias\", name: \"\", withLabel: false,\n  size: 4, strokeColor: \"#E69F00\", fillColor: \"#E69F00\",\n  strokeWidth: 2, highlight: false\n>>;"
}
"""

private const val BISECTOR_LINES_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-8, 6, 8, -6],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(-4, -1) <<\n  id: \"A\", name: \"\", withLabel: false,\n  size: 7, strokeColor: \"#246BCE\", fillColor: \"#246BCE\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nB = point(2, 3) <<\n  id: \"B\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nC = point(-3, 4) <<\n  id: \"C\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#16877A\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nD = point(4, -2) <<\n  id: \"D\", name: \"\", withLabel: false,\n  size: 7, strokeColor: \"#D9553F\", fillColor: \"#F4D44D\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nfirst = line(A, B) <<\n  id: \"first\", name: \"\", withLabel: false,\n  strokeColor: \"#6F7780\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;\nsecond = line(C, D) <<\n  id: \"second\", name: \"\", withLabel: false,\n  strokeColor: \"#9A4E1F\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;\npair = bisectorlines(first, second) <<\n  layer: 5,\n  line1: <<\n    id: \"bisectorFirst\", name: \"\", withLabel: false,\n    strokeColor: \"#1565C0\", strokeWidth: 6,\n    fixed: true, highlight: false\n  >>,\n  line2: <<\n    id: \"bisectorSecond\", name: \"\", withLabel: false,\n    strokeColor: \"#C62828\", strokeWidth: 3,\n    fixed: true, highlight: false\n  >>\n>>;"
}
"""

private const val SECTOR_COMPOSITIONS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 5, 6, -5],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(-4, -2) <<\n  id: \"A\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\nB = point(0, 4) <<\n  id: \"B\", name: \"\", withLabel: false,\n  size: 7, strokeColor: \"#9A4E1F\", fillColor: \"#F4D44D\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nC = point(4, -3) <<\n  id: \"C\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#246BCE\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\ncircumRegion = circumcirclesector(A, B, C) <<\n  id: \"circumRegion\", name: \"\", withLabel: false,\n  strokeColor: \"#0072B2\", fillColor: \"#0072B2\",\n  fillOpacity: 0.12, strokeWidth: 7,\n  fixed: true, highlight: false\n>>;\nminorRegion = minorsector(A, B, C) <<\n  id: \"minorRegion\", name: \"\", withLabel: false,\n  selection: \"major\",\n  strokeColor: \"#009E73\", fillColor: \"#009E73\",\n  fillOpacity: 0.16, strokeWidth: 5,\n  fixed: true, highlight: false\n>>;\nmajorRegion = majorsector(A, B, C) <<\n  id: \"majorRegion\", name: \"\", withLabel: false,\n  selection: \"minor\",\n  strokeColor: \"#CC79A7\", fillColor: \"none\",\n  strokeWidth: 3, fixed: true, highlight: false\n>>;\nnonreflexRegion = nonreflexangle(A, B, C) <<\n  id: \"nonreflexRegion\", name: \"\", withLabel: false,\n  radius: 1.35, type: \"sector\", orthoType: \"sector\",\n  strokeColor: \"#E69F00\", fillColor: \"#F4D44D\",\n  fillOpacity: 0.34, strokeWidth: 4,\n  fixed: true, highlight: false\n>>;\nreflexRegion = reflexangle(A, B, C) <<\n  id: \"reflexRegion\", name: \"\", withLabel: false,\n  radius: 2.15, type: \"sector\", orthoType: \"sector\",\n  strokeColor: \"#D55E00\", fillColor: \"none\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;"
}
"""

private const val JESSIECODE_NATIVE_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 5, 6, -5],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nsourcePoint = point(-3, -1) <<\n  id: \"sourcePoint\", name: \"\", withLabel: false,\n  size: 4, strokeColor: \"#6F7780\", fillColor: \"#6F7780\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\ntargetPoint = point(2.5, 2) <<\n  id: \"targetPoint\", name: \"\", withLabel: false,\n  size: 4, strokeColor: \"#D9553F\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\nroute = segment(sourcePoint, targetPoint) <<\n  id: \"route\", name: \"\", withLabel: false,\n  strokeColor: \"#246BCE\", strokeWidth: 3,\n  fixed: true, highlight: false\n>>;\nregion = circle(sourcePoint, 2.25) <<\n  id: \"region\", name: \"\", withLabel: false,\n  strokeColor: \"#16877A\", fillColor: \"#16877A\",\n  fillOpacity: 0.12, strokeWidth: 3,\n  fixed: true, highlight: false\n>>;"
}
"""

private const val FUNCTION_CIRCLE_RADIUS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 5, 6, -5],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\ncenter = point(0, 0) <<\n  id: \"center\", name: \"\", withLabel: false,\n  size: 3, strokeColor: \"#6F7780\", fillColor: \"#6F7780\",\n  fixed: true, highlight: false\n>>;\ndriver = point(3, 0) <<\n  id: \"driver\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#B44335\", fillColor: \"#F4D44D\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\ndynamicCircle = circle(\n  center,\n  function () { return driver.X() - 1; }\n) <<\n  id: \"dynamicCircle\", name: \"\", withLabel: false,\n  nonnegativeOnly: true,\n  strokeColor: \"#16877A\", fillColor: \"#16877A\",\n  fillOpacity: 0.14, strokeWidth: 3,\n  fixed: true, highlight: false\n>>;"
}
"""

private const val TRANSFORMED_POINTS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 5, 6, -5],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nbase = point(-2, 1) <<\n  id: \"base\", name: \"\", withLabel: false,\n  size: 5, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\norigin = point(0, 0) <<\n  id: \"origin\", name: \"\", withLabel: false,\n  visible: false, fixed: true, highlight: false\n>>;\naxisPoint = point(0, 3) <<\n  id: \"axisPoint\", name: \"\", withLabel: false,\n  visible: false, fixed: true, highlight: false\n>>;\ndriver = point(2, 0) <<\n  id: \"driver\", name: \"\", withLabel: false,\n  size: 5, strokeColor: \"#D9553F\", fillColor: \"#F4D44D\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\naxis = line(origin, axisPoint) <<\n  id: \"axis\", name: \"\", withLabel: false,\n  strokeColor: \"#6F7780\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;\nscale = transform(1.4, 0.8) << type: \"scale\" >>;\nrotate = transform(PI / 5, origin) << type: \"rotate\" >>;\nchained = point(base, [scale, rotate]) <<\n  id: \"chained\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#246BCE\", fillColor: \"#246BCE\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\nshift = transform(\n  function () { return driver.X() / 2; }, -1\n) << type: \"translate\" >>;\nshifted = point(base, shift) <<\n  id: \"shifted\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#16877A\", fillColor: \"#16877A\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\nreflection = transform(axis) << type: \"reflect\" >>;\nmirrored = point(shifted, reflection) <<\n  id: \"mirrored\", name: \"\", withLabel: false,\n  size: 7, strokeColor: \"#B44335\", fillColor: \"#B44335\",\n  strokeWidth: 3, fixed: true, highlight: false\n>>;"
}
"""

private const val POINT3D_PROJECTION_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "view",
      "type": "view3d",
      "parents": [
        [-5, -4],
        [8, 7],
        [[-5, 5], [-4, 6], [-3, 7]]
      ],
      "attributes": {
        "name": "",
        "projection": "parallel",
        "axesPosition": "none",
        "xPlaneRear": {"visible": false, "type": "wireframe"},
        "yPlaneRear": {"visible": false, "type": "wireframe"},
        "zPlaneRear": {"visible": false, "type": "wireframe"},
        "depthOrder": {"enabled": false},
        "az": {"slider": {"visible": false, "start": 1}},
        "el": {"slider": {"visible": false, "start": 0.3}},
        "bank": {"slider": {"visible": false, "start": 0}}
      }
    },
    {
      "id": "source3d",
      "type": "point3d",
      "parents": ["view", 1, 2, 2],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 8,
        "strokeColor": "#D9553F",
        "fillColor": "none",
        "strokeWidth": 3,
        "fixed": false,
        "highlight": false
      }
    },
    {
      "id": "homogeneous3d",
      "type": "point3d",
      "parents": ["view", 2, 4, 6, 8],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 7,
        "strokeColor": "#246BCE",
        "fillColor": "none",
        "strokeWidth": 3,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "translation3d",
      "type": "transform3d",
      "parents": ["view", 2, -3, 4],
      "attributes": {"type": "translate"}
    },
    {
      "id": "transformed3d",
      "type": "point3d",
      "parents": ["view", "source3d", "translation3d"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 9,
        "strokeColor": "#16877A",
        "fillColor": "none",
        "strokeWidth": 3,
        "fixed": true,
        "highlight": false
      }
    }
  ]
}
"""

private const val SPATIAL_LINES_PLANES_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "view",
      "type": "view3d",
      "parents": [
        [-5, -4],
        [8, 7],
        [[-5, 5], [-4, 6], [-3, 7]]
      ],
      "attributes": {
        "name": "",
        "projection": "parallel",
        "axesPosition": "none",
        "xPlaneRear": {"visible": false, "type": "wireframe"},
        "yPlaneRear": {"visible": false, "type": "wireframe"},
        "zPlaneRear": {"visible": false, "type": "wireframe"},
        "depthOrder": {"enabled": false},
        "az": {"slider": {"visible": false, "start": 1}},
        "el": {"slider": {"visible": false, "start": 0.3}},
        "bank": {"slider": {"visible": false, "start": 0}}
      }
    },
    {
      "id": "plane",
      "type": "plane3d",
      "parents": [
        "view",
        [1, 2, 2],
        [1, 0, 0],
        [0, 1, 1],
        [-2, 3],
        [-1, 2]
      ],
      "attributes": {
        "name": "",
        "withLabel": false,
        "type": "wireframe",
        "strokeColor": "#16877A",
        "strokeWidth": 3,
        "fillColor": "none",
        "fillOpacity": 0,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "line",
      "type": "line3d",
      "parents": [
        "view",
        [1, 2, 2],
        [2, -1, 3]
      ],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#D9553F",
        "strokeWidth": 4,
        "straightFirst": true,
        "straightLast": true,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "axis",
      "type": "axis3d",
      "parents": [
        "view",
        [-5, 6, -3],
        [5, 6, -3]
      ],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#246BCE",
        "strokeWidth": 3,
        "fixed": true,
        "highlight": false
      }
    }
  ]
}
"""

private const val PLANE_3D_SURFACES_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-6, 5, 6, -5],
  "axis": false,
  "grid": false,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "view",
      "type": "view3d",
      "parents": [
        [-5, -4],
        [8, 7],
        [[-5, 5], [-4, 6], [-3, 7]]
      ],
      "attributes": {
        "name": "",
        "projection": "parallel",
        "axesPosition": "none",
        "xPlaneRear": {"visible": false, "type": "wireframe"},
        "yPlaneRear": {"visible": false, "type": "wireframe"},
        "zPlaneRear": {"visible": false, "type": "wireframe"},
        "depthOrder": {"enabled": true},
        "az": {"slider": {"visible": false, "start": 1}},
        "el": {"slider": {"visible": false, "start": 0.3}},
        "bank": {"slider": {"visible": false, "start": 0}}
      }
    },
    {
      "id": "colors",
      "type": "plane3d",
      "parents": [
        "view",
        [0, 0, -2],
        [1, 0, 0],
        [0, 1, 0],
        [-4, -0.5],
        [-2, 2]
      ],
      "attributes": {
        "name": "",
        "withLabel": false,
        "type": "colorarray",
        "stepsU": 3,
        "stepsV": 2,
        "polyhedron": {
          "strokeColor": "#49545D",
          "strokeWidth": 0.6,
          "fillOpacity": 0.82,
          "fillColorArray": ["#56B4E9", "#E69F00"]
        }
      }
    },
    {
      "id": "shader",
      "type": "plane3d",
      "parents": [
        "view",
        [0, 0, 0],
        [1, 0, 0],
        [0, 1, 0],
        [-1.5, 1.5],
        [-2, 2]
      ],
      "attributes": {
        "name": "",
        "withLabel": false,
        "type": "shader",
        "tiling": "triangle",
        "stepsU": 3,
        "stepsV": 3,
        "polyhedron": {
          "strokeColor": "#49545D",
          "strokeWidth": 0.5,
          "fillOpacity": 0.82,
          "fillColorArray": ["#D9553F"],
          "shader": {
            "hue": 145,
            "saturation": 65,
            "minLightness": 38,
            "maxLightness": 72,
            "light": {"dir": 0}
          }
        }
      }
    },
    {
      "id": "colormap",
      "type": "plane3d",
      "parents": [
        "view",
        [3, 0, 0],
        [0, 1, 0],
        [0, 0, 1],
        [-2, 2],
        [-2, 2]
      ],
      "attributes": {
        "name": "",
        "withLabel": false,
        "type": "colormap",
        "stepsU": 2,
        "stepsV": 3,
        "polyhedron": {
          "strokeColor": "#49545D",
          "strokeWidth": 0.6,
          "fillOpacity": 0.82
        },
        "colormap": {
          "min": [-2, 220],
          "max": [2, 0],
          "s": 0.78,
          "v": 0.92
        }
      }
    }
  ]
}
"""

private const val POLYHEDRON_3D_FACES_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 5, 6, -5],
    "axis": false,
    "grid": false,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nview = view3d(\n  [-5, -4],\n  [8, 7],\n  [[-5, 5], [-4, 6], [-3, 7]]\n) <<\n  id: \"view\", name: \"\", projection: \"parallel\",\n  axesPosition: \"none\",\n  xPlaneRear: << visible: false, type: \"wireframe\" >>,\n  yPlaneRear: << visible: false, type: \"wireframe\" >>,\n  zPlaneRear: << visible: false, type: \"wireframe\" >>,\n  depthOrder: << enabled: true >>,\n  az: << slider: << visible: false, start: 1 >> >>,\n  el: << slider: << visible: false, start: 0.3 >> >>,\n  bank: << slider: << visible: false, start: 0 >> >>\n>>;\nsolid = polyhedron3d(\n  view,\n  [\n    [-3, -3, -3], [3, -3, -3],\n    [3, 3, -3], [-3, 3, -3],\n    [-3, -3, 3], [3, -3, 3],\n    [3, 3, 3], [-3, 3, 3]\n  ],\n  [\n    [[0, 1, 2, 3], << fillColor: \"#009E73\", fillOpacity: 0.62 >>],\n    [0, 1, 5, 4],\n    [1, 2, 6, 5],\n    [2, 3, 7, 6],\n    [3, 0, 4, 7],\n    [4, 5, 6, 7]\n  ]\n) <<\n  name: \"\", fillColorArray: [\"#56B4E9\", \"#E69F00\"],\n  strokeColor: \"#49545D\", strokeWidth: 2,\n  fillOpacity: 0.48, layer: 12,\n  fixed: true, highlight: false,\n  shader: << enabled: false >>\n>>;"
}
"""

private const val POLYGON_3D_PROJECTION_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 5, 6, -5],
    "axis": false,
    "grid": false,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nview = view3d(\n  [-5, -4],\n  [8, 7],\n  [[-5, 5], [-4, 6], [-3, 7]]\n) <<\n  id: \"view\", name: \"\", projection: \"parallel\",\n  axesPosition: \"none\",\n  xPlaneRear: << visible: false, type: \"wireframe\" >>,\n  yPlaneRear: << visible: false, type: \"wireframe\" >>,\n  zPlaneRear: << visible: false, type: \"wireframe\" >>,\n  depthOrder: << enabled: true >>,\n  az: << slider: << visible: false, start: 1 >> >>,\n  el: << slider: << visible: false, start: 0.3 >> >>,\n  bank: << slider: << visible: false, start: 0 >> >>\n>>;\nowned = polygon3d(\n  view,\n  [-4, -2, -1], [0, -3, 1], [1, 1, 2], [-3, 2, 0]\n) <<\n  id: \"owned\", name: \"\",\n  fillColor: \"#F0E442\", fillOpacity: 0.42, layer: 12,\n  vertices: <<\n    name: \"\", withLabel: false, size: 5,\n    strokeColor: \"#D9553F\", fillColor: \"#FFFFFF\",\n    strokeWidth: 2, fixed: true\n  >>,\n  borders: <<\n    strokeColor: \"#49545D\", strokeWidth: 3, layer: 11\n  >>\n>>;\na = point3d(view, [1, -2, -1]) <<\n  id: \"a\", name: \"\", withLabel: false,\n  size: 5, strokeColor: \"#246BCE\", fillColor: \"#FFFFFF\",\n  strokeWidth: 2, fixed: true\n>>;\nb = point3d(view, [4, -1, 0]) <<\n  id: \"b\", name: \"\", withLabel: false,\n  size: 5, strokeColor: \"#246BCE\", fillColor: \"#FFFFFF\",\n  strokeWidth: 2, fixed: true\n>>;\nc = point3d(view, [3, 3, 2]) <<\n  id: \"c\", name: \"\", withLabel: false,\n  size: 5, strokeColor: \"#246BCE\", fillColor: \"#FFFFFF\",\n  strokeWidth: 2, fixed: true\n>>;\nreferenced = polygon3d(view, a, b, c) <<\n  id: \"referenced\", name: \"\",\n  fillColor: \"#56B4E9\", fillOpacity: 0.34, layer: 12,\n  borders: <<\n    strokeColor: \"#16877A\", strokeWidth: 3, layer: 11\n  >>\n>>;"
}
"""

private const val VIEW3D_DEFAULT_AXES_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-8, 8, 8, -8],
  "axis": false,
  "grid": false,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "view",
      "type": "view3d",
      "parents": [
        [-5, -4],
        [8, 7],
        [[-5, 5], [-4, 6], [-3, 7]]
      ],
      "attributes": {
        "name": "",
        "projection": "parallel",
        "axesPosition": "border",
        "xAxisBorder": {
          "name": "",
          "withLabel": false,
          "strokeColor": "#D9553F",
          "strokeWidth": 2,
          "ticks3d": {
            "strokeColor": "#D9553F",
            "drawLabels": true,
            "label": {
              "anchorX": "middle",
              "anchorY": "middle"
            }
          }
        },
        "yAxisBorder": {
          "name": "",
          "withLabel": false,
          "strokeColor": "#16877A",
          "strokeWidth": 2,
          "ticks3d": {
            "strokeColor": "#16877A",
            "drawLabels": true,
            "label": {
              "anchorX": "middle",
              "anchorY": "middle"
            }
          }
        },
        "zAxisBorder": {
          "name": "",
          "withLabel": false,
          "strokeColor": "#246BCE",
          "strokeWidth": 2,
          "ticks3d": {
            "strokeColor": "#246BCE",
            "drawLabels": true,
            "label": {
              "anchorX": "middle",
              "anchorY": "middle"
            }
          }
        },
        "xPlaneRear": {"visible": false, "type": "wireframe"},
        "xPlaneFront": {"visible": false},
        "yPlaneRear": {"visible": false, "type": "wireframe"},
        "yPlaneFront": {"visible": false},
        "zPlaneRear": {"visible": false, "type": "wireframe"},
        "zPlaneFront": {"visible": false},
        "depthOrder": {"enabled": false},
        "az": {"slider": {"visible": false, "start": 1}},
        "el": {"slider": {"visible": false, "start": 0.3}},
        "bank": {"slider": {"visible": false, "start": 0}}
      }
    }
  ]
}
"""

private const val VIEW3D_CENTER_AXES_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-8, 8, 8, -8],
  "axis": false,
  "grid": false,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "view",
      "type": "view3d",
      "parents": [
        [-5, -4],
        [8, 7],
        [[-5, 5], [-4, 6], [-3, 7]]
      ],
      "attributes": {
        "name": "",
        "projection": "parallel",
        "axesPosition": "center",
        "xAxis": {
          "name": "",
          "withLabel": false,
          "strokeColor": "#D9553F",
          "strokeWidth": 3
        },
        "yAxis": {
          "name": "",
          "withLabel": false,
          "strokeColor": "#16877A",
          "strokeWidth": 3
        },
        "zAxis": {
          "name": "",
          "withLabel": false,
          "strokeColor": "#246BCE",
          "strokeWidth": 3
        },
        "O": {
          "name": "ignored",
          "visible": true,
          "withLabel": true
        },
        "xPlaneRear": {"visible": false, "type": "wireframe"},
        "xPlaneFront": {"visible": false},
        "yPlaneRear": {"visible": false, "type": "wireframe"},
        "yPlaneFront": {"visible": false},
        "zPlaneRear": {"visible": false, "type": "wireframe"},
        "zPlaneFront": {"visible": false},
        "depthOrder": {"enabled": false},
        "az": {"slider": {"visible": false, "start": 1}},
        "el": {"slider": {"visible": false, "start": 0.3}},
        "bank": {"slider": {"visible": false, "start": 0}}
      }
    }
  ]
}
"""

private const val FUNCTION_COORDINATE_POINTS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 5, 6, -5],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\ndriver = point(-3, -2) <<\n  id: \"driver\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#B44335\", fillColor: \"#F4D44D\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\narrayPoint = point(\n  function () {\n    return [driver.X() + 2, driver.Y() + 2];\n  }\n) <<\n  id: \"arrayPoint\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#246BCE\", fillColor: \"#246BCE\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\nmixedPoint = point(\n  function () { return driver.X() + 5; },\n  function () { return driver.Y() - 1; }\n) <<\n  id: \"mixedPoint\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#16877A\", fillColor: \"#16877A\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\nhomogeneousPoint = point(\n  function () {\n    return [\n      2,\n      (driver.X() - 1) * 2,\n      (driver.Y() + 3) * 2\n    ];\n  }\n) <<\n  id: \"homogeneousPoint\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#7B4EA3\", fillColor: \"#7B4EA3\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\narrayRoute = segment(driver, arrayPoint) <<\n  id: \"arrayRoute\", name: \"\", withLabel: false,\n  strokeColor: \"#246BCE\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;\nmixedRoute = segment(driver, mixedPoint) <<\n  id: \"mixedRoute\", name: \"\", withLabel: false,\n  strokeColor: \"#16877A\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;\nhomogeneousRoute = segment(driver, homogeneousPoint) <<\n  id: \"homogeneousRoute\", name: \"\", withLabel: false,\n  strokeColor: \"#7B4EA3\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;"
}
"""

private const val PARALLEL_CONSTRUCTIONS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 5, 6, -5],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(-4, 1) <<\n  id: \"A\", name: \"\", withLabel: false,\n  size: 5, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nB = point(-2, 0) <<\n  id: \"B\", name: \"\", withLabel: false,\n  size: 5, strokeColor: \"#246BCE\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nbaseLine = line(A, B) <<\n  id: \"baseLine\", name: \"\", withLabel: false,\n  strokeColor: \"#6F7780\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;\nC = point(3, -3) <<\n  id: \"C\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#B44335\", fillColor: \"#F4D44D\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nparallelPoint = parallelpoint(A, B, C) <<\n  id: \"parallelPoint\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#7B4EA3\", fillColor: \"#7B4EA3\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\nfiniteParallel = parallel(A, B, C) <<\n  id: \"finiteParallel\", name: \"\", withLabel: false,\n  straightFirst: false, straightLast: false,\n  strokeColor: \"#16877A\", strokeWidth: 7,\n  fixed: true, highlight: false\n>>;\nidealParallel = parallel(baseLine, C) <<\n  id: \"idealParallel\", name: \"\", withLabel: false,\n  strokeColor: \"#D9553F\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;"
}
"""

private const val LINE_ARROWS_SOURCE: String = """
{
  "schemaVersion": 1,
  "boundingBox": [-8, 6, 8, -6],
  "axis": false,
  "grid": false,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "arrowType1",
      "type": "arrow",
      "parents": [[-7, 5], [-2.5, 5]],
      "attributes": {
        "name": "",
        "withLabel": false,
        "strokeColor": "#1565C0",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "arrowType2",
      "type": "arrow",
      "parents": [[-7, 3.5], [-2.5, 3.5]],
      "attributes": {
        "name": "",
        "withLabel": false,
        "lastArrow": {"type": 2, "size": 6},
        "strokeColor": "#16877A",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "arrowType3",
      "type": "arrow",
      "parents": [[-7, 2], [-2.5, 2]],
      "attributes": {
        "name": "",
        "withLabel": false,
        "lastArrow": {"type": 3, "size": 6},
        "strokeColor": "#B44335",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "arrowType4",
      "type": "arrow",
      "parents": [[-7, 0.5], [-2.5, 0.5]],
      "attributes": {
        "name": "",
        "withLabel": false,
        "lastArrow": {"type": 4, "size": 6},
        "strokeColor": "#7B4EA3",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "arrowType5",
      "type": "arrow",
      "parents": [[-7, -1], [-2.5, -1]],
      "attributes": {
        "name": "",
        "withLabel": false,
        "lastArrow": {"type": 5, "size": 6},
        "strokeColor": "#D55E00",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "arrowType6",
      "type": "arrow",
      "parents": [[-7, -2.5], [-2.5, -2.5]],
      "attributes": {
        "name": "",
        "withLabel": false,
        "lastArrow": {"type": 6, "size": 6},
        "strokeColor": "#009E73",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "arrowType7",
      "type": "arrow",
      "parents": [[-7, -4], [-2.5, -4]],
      "attributes": {
        "name": "",
        "withLabel": false,
        "lastArrow": {"type": 7, "size": 20},
        "strokeColor": "#CC79A7",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "doubleArrow",
      "type": "arrow",
      "parents": [[0, 4.8], [7, 4.8]],
      "attributes": {
        "name": "",
        "withLabel": false,
        "firstArrow": {"type": 2, "size": 5},
        "lastArrow": {"type": 3, "size": 7},
        "strokeColor": "#1D252C",
        "strokeWidth": 3,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "A",
      "type": "point",
      "parents": [1, -2],
      "attributes": {
        "name": "",
        "withLabel": false,
        "visible": false,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "B",
      "type": "point",
      "parents": [3, -1],
      "attributes": {
        "name": "",
        "withLabel": false,
        "visible": false,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "base",
      "type": "line",
      "parents": ["A", "B"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "visible": false,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "driver",
      "type": "point",
      "parents": [0, -4.8],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 7,
        "strokeColor": "#B44335",
        "fillColor": "#F4D44D",
        "strokeWidth": 2,
        "fixed": false,
        "highlight": false
      }
    },
    {
      "id": "through",
      "type": "point",
      "parents": [0, 1],
      "attributes": {
        "name": "",
        "withLabel": false,
        "size": 6,
        "strokeColor": "#49545D",
        "fillColor": "#FCFDFE",
        "strokeWidth": 2,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "finiteArrowParallel",
      "type": "arrowparallel",
      "parents": ["A", "B", "driver"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "lastArrow": {"type": 4, "size": 7},
        "strokeColor": "#246BCE",
        "strokeWidth": 4,
        "fixed": true,
        "highlight": false
      }
    },
    {
      "id": "idealArrowParallel",
      "type": "arrowparallel",
      "parents": ["base", "through"],
      "attributes": {
        "name": "",
        "withLabel": false,
        "firstArrow": {"type": 2, "size": 5},
        "lastArrow": {"type": 7, "size": 30},
        "strokeColor": "#E69F00",
        "strokeWidth": 3,
        "fixed": true,
        "highlight": false
      }
    }
  ]
}
"""

private const val TRIANGLE_CENTERS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 5, 6, -5],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(-4, -2) <<\n  id: \"A\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nB = point(0, 4) <<\n  id: \"B\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#246BCE\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nC = point(4, -3) <<\n  id: \"C\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#B44335\", fillColor: \"#F4D44D\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nedgeAB = segment(A, B) <<\n  id: \"edgeAB\", name: \"\", withLabel: false,\n  strokeColor: \"#6F7780\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;\nedgeBC = segment(B, C) <<\n  id: \"edgeBC\", name: \"\", withLabel: false,\n  strokeColor: \"#6F7780\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;\nedgeCA = segment(C, A) <<\n  id: \"edgeCA\", name: \"\", withLabel: false,\n  strokeColor: \"#6F7780\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;\nangleBisector = bisector(A, B, C) <<\n  id: \"angleBisector\", name: \"\", withLabel: false,\n  strokeColor: \"#D9553F\", strokeWidth: 3,\n  fixed: true, highlight: false\n>>;\ntriangleIncenter = incenter(A, B, C) <<\n  id: \"triangleIncenter\", name: \"\", withLabel: false,\n  size: 7, strokeColor: \"#7B4EA3\", fillColor: \"#7B4EA3\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\ntriangleIncircle = incircle(A, B, C) <<\n  id: \"triangleIncircle\", name: \"\", withLabel: false,\n  strokeColor: \"#16877A\", strokeWidth: 4,\n  fixed: true, highlight: false\n>>;"
}
"""

private const val INTERSECTION_POINTS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-6, 5, 6, -5],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nO = point(0, 0) <<\n  id: \"O\", name: \"\", withLabel: false,\n  size: 7, strokeColor: \"#B44335\", fillColor: \"#F4D44D\",\n  strokeWidth: 2, fixed: false, highlight: false\n>>;\nC = circle(O, 3) <<\n  id: \"C\", name: \"\", withLabel: false,\n  strokeColor: \"#16877A\", strokeWidth: 3,\n  fillColor: \"#16877A\", fillOpacity: 0.08,\n  fixed: true, highlight: false\n>>;\nlineA = point(-5, 0) <<\n  id: \"lineA\", name: \"\", withLabel: false,\n  visible: false, fixed: true, highlight: false\n>>;\nlineB = point(5, 0) <<\n  id: \"lineB\", name: \"\", withLabel: false,\n  visible: false, fixed: true, highlight: false\n>>;\nmainLine = line(lineA, lineB) <<\n  id: \"mainLine\", name: \"\", withLabel: false,\n  strokeColor: \"#6F7780\", strokeWidth: 2,\n  fixed: true, highlight: false\n>>;\nsegmentA = point(-1, 2) <<\n  id: \"segmentA\", name: \"\", withLabel: false,\n  size: 4, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\nsegmentB = point(1, 2) <<\n  id: \"segmentB\", name: \"\", withLabel: false,\n  size: 4, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\nfiniteSegment = segment(segmentA, segmentB) <<\n  id: \"finiteSegment\", name: \"\", withLabel: false,\n  strokeColor: \"#49545D\", strokeWidth: 5,\n  fixed: true, highlight: false\n>>;\nfirst = intersection(C, mainLine, 0) <<\n  id: \"first\", name: \"\", withLabel: false,\n  size: 7, strokeColor: \"#246BCE\", fillColor: \"#246BCE\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\nother = otherintersection(C, mainLine, first) <<\n  id: \"other\", name: \"\", withLabel: false,\n  size: 7, strokeColor: \"#D9553F\", fillColor: \"#D9553F\",\n  strokeWidth: 2, precision: 0.001,\n  fixed: true, highlight: false\n>>;\nextended = intersection(C, finiteSegment, 0) <<\n  id: \"extended\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#7B4EA3\", fillColor: \"#7B4EA3\",\n  strokeWidth: 2, fixed: true, highlight: false\n>>;\nclipped = intersection(C, finiteSegment, 0) <<\n  id: \"clipped\", name: \"\", withLabel: false,\n  size: 6, strokeColor: \"#E69F00\", fillColor: \"#E69F00\",\n  strokeWidth: 2, alwaysIntersect: false,\n  fixed: true, highlight: false\n>>;"
}
"""

private const val INTERSECTION_PATHS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-8, 6, 8, -6],
    "axis": false,
    "grid": false,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nlineA = point(-7, 2) << id: \"lineA\", name: \"\", withLabel: false, visible: false, fixed: true >>;\nlineB = point(7, 2) << id: \"lineB\", name: \"\", withLabel: false, visible: false, fixed: true >>;\nmainLine = line(lineA, lineB) << id: \"mainLine\", name: \"\", withLabel: false, strokeColor: \"#6F7780\", strokeWidth: 2, fixed: true >>;\nzigzag = curve([-6, -4, -2, 0, 2], [5, 1, 5, 1, 5]) << id: \"zigzag\", name: \"\", withLabel: false, strokeColor: \"#16877A\", strokeWidth: 4, fixed: true >>;\nfirst = intersection(zigzag, mainLine, 0) << id: \"first\", name: \"\", withLabel: false, size: 6, strokeColor: \"#246BCE\", fillColor: \"#246BCE\", fixed: true >>;\nsecond = otherintersection(zigzag, mainLine, first) << id: \"second\", name: \"\", withLabel: false, size: 6, strokeColor: \"#D9553F\", fillColor: \"#D9553F\", fixed: true >>;\nOa = point(-4, -2) << id: \"Oa\", name: \"\", withLabel: false, visible: false, fixed: true >>;\nRa = point(-1, -2) << id: \"Ra\", name: \"\", withLabel: false, size: 4, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", fixed: true >>;\nTa = point(-4, 1) << id: \"Ta\", name: \"\", withLabel: false, size: 5, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", fixed: false >>;\narcPath = arc(Oa, Ra, Ta) << id: \"arcPath\", name: \"\", withLabel: false, strokeColor: \"#7B4EA3\", strokeWidth: 4, fixed: true >>;\narcGuide = curve([-7, -1], [-1, -1]) << id: \"arcGuide\", name: \"\", withLabel: false, strokeColor: \"#A8ADB3\", strokeWidth: 2, fixed: true >>;\narcHit = intersection(arcPath, arcGuide, 0) << id: \"arcHit\", name: \"\", withLabel: false, size: 7, strokeColor: \"#7B4EA3\", fillColor: \"#FCFDFE\", strokeWidth: 3, fixed: true >>;\nOs = point(4, -2) << id: \"Os\", name: \"\", withLabel: false, visible: false, fixed: true >>;\nRs = point(7, -2) << id: \"Rs\", name: \"\", withLabel: false, visible: false, fixed: true >>;\nTs = point(4, 1) << id: \"Ts\", name: \"\", withLabel: false, visible: false, fixed: true >>;\nsectorPath = sector(Os, Rs, Ts) << id: \"sectorPath\", name: \"\", withLabel: false, strokeColor: \"#E69F00\", strokeWidth: 3, fillColor: \"#E69F00\", fillOpacity: 0.12, fixed: true >>;\nsectorGuide = curve([1, 7], [-1, -1]) << id: \"sectorGuide\", name: \"\", withLabel: false, strokeColor: \"#A8ADB3\", strokeWidth: 2, fixed: true >>;\nsectorHit = intersection(sectorPath, sectorGuide, 0) << id: \"sectorHit\", name: \"\", withLabel: false, size: 7, strokeColor: \"#E69F00\", fillColor: \"#FCFDFE\", strokeWidth: 3, fixed: true >>;\nPa = point(-2, -5) << id: \"Pa\", name: \"\", withLabel: false, visible: false, fixed: true >>;\nPb = point(2, -5) << id: \"Pb\", name: \"\", withLabel: false, visible: false, fixed: true >>;\nPc = point(2, -3) << id: \"Pc\", name: \"\", withLabel: false, visible: false, fixed: true >>;\nPd = point(-2, -3) << id: \"Pd\", name: \"\", withLabel: false, visible: false, fixed: true >>;\npolygonPath = polygon(Pa, Pb, Pc, Pd) << id: \"polygonPath\", name: \"\", withLabel: false, strokeColor: \"#49545D\", strokeWidth: 3, fillColor: \"#49545D\", fillOpacity: 0.08, fixed: true >>;\npolygonA = point(-4, -4) << id: \"polygonA\", name: \"\", withLabel: false, visible: false, fixed: true >>;\npolygonB = point(4, -4) << id: \"polygonB\", name: \"\", withLabel: false, visible: false, fixed: true >>;\npolygonLine = line(polygonA, polygonB) << id: \"polygonLine\", name: \"\", withLabel: false, strokeColor: \"#A8ADB3\", strokeWidth: 2, fixed: true >>;\npolygonHit0 = intersection(polygonPath, polygonLine, 0) << id: \"polygonHit0\", name: \"\", withLabel: false, size: 6, strokeColor: \"#009E73\", fillColor: \"#009E73\", fixed: true >>;\npolygonHit1 = intersection(polygonPath, polygonLine, 1) << id: \"polygonHit1\", name: \"\", withLabel: false, size: 6, strokeColor: \"#009E73\", fillColor: \"#009E73\", fixed: true >>;"
}
"""

private const val POLYGON_PATH_INTERSECTIONS_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-5, 5, 5, -5],
    "axis": false,
    "grid": false,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\nA = point(-2, -2) << id: \"A\", name: \"\", withLabel: false, size: 5, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", fixed: true, highlight: false >>;\nB = point(2, -2) << id: \"B\", name: \"\", withLabel: false, size: 5, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", fixed: true, highlight: false >>;\nC = point(2, 2) << id: \"C\", name: \"\", withLabel: false, size: 7, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", fixed: false, highlight: false >>;\nD = point(-2, 2) << id: \"D\", name: \"\", withLabel: false, size: 5, strokeColor: \"#49545D\", fillColor: \"#FCFDFE\", fixed: true, highlight: false >>;\nframe = polygon(A, B, C, D) << id: \"frame\", name: \"\", withLabel: false, strokeColor: \"#49545D\", strokeWidth: 3, fillColor: \"#49545D\", fillOpacity: 0.08, fixed: true, highlight: false >>;\nO = point(0, 0) << id: \"O\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\nring = circle(O, 2.5) << id: \"ring\", name: \"\", withLabel: false, strokeColor: \"#16877A\", strokeWidth: 4, fillColor: \"none\", fixed: true, highlight: false >>;\nDA = point(0, -3) << id: \"DA\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\nDB = point(3, 0) << id: \"DB\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\nDC = point(0, 3) << id: \"DC\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\nDD = point(-3, 0) << id: \"DD\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\ndiamond = polygon(DA, DB, DC, DD) << id: \"diamond\", name: \"\", withLabel: false, strokeColor: \"#7B4EA3\", strokeWidth: 2, fillColor: \"#7B4EA3\", fillOpacity: 0.05, fixed: true, highlight: false >>;\npc0 = intersection(frame, ring, 0) << id: \"pc0\", name: \"\", withLabel: false, size: 5, strokeColor: \"#246BCE\", fillColor: \"#246BCE\", fixed: true, highlight: false >>;\npc2 = intersection(frame, ring, 2) << id: \"pc2\", name: \"\", withLabel: false, size: 5, strokeColor: \"#246BCE\", fillColor: \"#246BCE\", fixed: true, highlight: false >>;\npc4 = intersection(frame, ring, 4) << id: \"pc4\", name: \"\", withLabel: false, size: 5, strokeColor: \"#246BCE\", fillColor: \"#246BCE\", fixed: true, highlight: false >>;\npc6 = intersection(frame, ring, 6) << id: \"pc6\", name: \"\", withLabel: false, size: 5, strokeColor: \"#246BCE\", fillColor: \"#246BCE\", fixed: true, highlight: false >>;\nreverse = intersection(ring, frame, 0) << id: \"reverse\", name: \"\", withLabel: false, size: 7, strokeColor: \"#E69F00\", fillColor: \"#FCFDFE\", strokeWidth: 3, fixed: true, highlight: false >>;\npp0 = intersection(frame, diamond, 0) << id: \"pp0\", name: \"\", withLabel: false, size: 4, strokeColor: \"#D9553F\", fillColor: \"#D9553F\", fixed: true, highlight: false >>;\npp2 = intersection(frame, diamond, 2) << id: \"pp2\", name: \"\", withLabel: false, size: 4, strokeColor: \"#D9553F\", fillColor: \"#D9553F\", fixed: true, highlight: false >>;\npp4 = intersection(frame, diamond, 4) << id: \"pp4\", name: \"\", withLabel: false, size: 4, strokeColor: \"#D9553F\", fillColor: \"#D9553F\", fixed: true, highlight: false >>;\npp6 = intersection(frame, diamond, 6) << id: \"pp6\", name: \"\", withLabel: false, size: 4, strokeColor: \"#D9553F\", fillColor: \"#D9553F\", fixed: true, highlight: false >>;"
}
"""

private const val CURVE_BOOLEAN_CLIPPING_SOURCE: String = """
{
  "schemaVersion": 1,
  "inputKind": "jessiecode",
  "boardOptions": {
    "containerId": "jxgbox",
    "boundingBox": [-9, 5, 9, -5],
    "axis": true,
    "grid": true,
    "keepAspectRatio": true
  },
  "source": "use jxgbox;\niSubject = curve([-8, -4, -4, -8], [-2, -2, 2, 2]) << id: \"iSubject\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\niClip = curve([-6, -2, -2, -6], [-3, -3, 1, 1]) << id: \"iClip\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\niOutput = curveintersection(iSubject, iClip) << id: \"iOutput\", name: \"\", withLabel: false, strokeColor: \"#0072B2\", strokeWidth: 3, fillColor: \"#56B4E9\", fillOpacity: 0.45, fixed: true, highlight: false >>;\nuA = point(-3, -2) << id: \"uA\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\nuB = point(1, -2) << id: \"uB\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\nuC = point(1, 2) << id: \"uC\", name: \"\", withLabel: false, size: 7, strokeColor: \"#B44335\", fillColor: \"#F4D44D\", strokeWidth: 2, fixed: false, highlight: false >>;\nuD = point(-3, 2) << id: \"uD\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\nuSubject = polygon(uA, uB, uC, uD) << id: \"uSubject\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\nuClip = curve([-1, 3, 3, -1], [-3, -3, 1, 1]) << id: \"uClip\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\nuOutput = curveunion(uSubject, uClip) << id: \"uOutput\", name: \"\", withLabel: false, strokeColor: \"#008A72\", strokeWidth: 3, fillColor: \"#009E73\", fillOpacity: 0.32, fixed: true, highlight: false >>;\ndSubject = curve([2, 6, 6, 2], [-2, -2, 2, 2]) << id: \"dSubject\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\ndClip = curve([4, 8, 8, 4], [-3, -3, 1, 1]) << id: \"dClip\", name: \"\", withLabel: false, visible: false, fixed: true, highlight: false >>;\ndOutput = curvedifference(dSubject, dClip) << id: \"dOutput\", name: \"\", withLabel: false, strokeColor: \"#D55E00\", strokeWidth: 3, fillColor: \"#E69F00\", fillOpacity: 0.4, fixed: true, highlight: false >>;"
}
"""
