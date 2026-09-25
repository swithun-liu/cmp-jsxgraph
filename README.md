# CMP JSXGraph

[![Quality Gate](https://github.com/swithun-liu/cmp-jsxgraph/actions/workflows/quality.yml/badge.svg?branch=main)](https://github.com/swithun-liu/cmp-jsxgraph/actions/workflows/quality.yml)
[![Web Demo](https://github.com/swithun-liu/cmp-jsxgraph/actions/workflows/deploy-pages.yml/badge.svg?branch=main)](https://github.com/swithun-liu/cmp-jsxgraph/actions/workflows/deploy-pages.yml)
[![Visual Parity](https://github.com/swithun-liu/cmp-jsxgraph/actions/workflows/visual-parity.yml/badge.svg)](https://github.com/swithun-liu/cmp-jsxgraph/actions/workflows/visual-parity.yml)
[![JSXGraph 1.13.3](https://img.shields.io/badge/JSXGraph-1.13.3-246BCE)](https://jsxgraph.org/)
[![MIT License](https://img.shields.io/badge/license-MIT-16877A)](LICENSE)

Stable native JSXGraph rendering for the documented Kotlin and Compose
Multiplatform support scope.

**[Open the live Kotlin/Wasm case workbench](https://swithun-liu.github.io/cmp-jsxgraph/)**
to browse 84 source-controlled cases: 30 independent production scenarios and
54 focused regression fixtures. Use the case picker or previous/next controls,
then switch the same source between Source, official JSXGraph `1.13.3`, and
native Compose Canvas rendering. Case selection is reflected in the URL for
direct links and reloads. The separate
**[translation roadmap](https://swithun-liu.github.io/cmp-jsxgraph/?roadmap=true)**
tracks the remaining upstream work.

Open the
**[30-case Stable load screen](https://swithun-liu.github.io/cmp-jsxgraph/?load=true)**
to scroll the independent production corpus through the native renderer.
The evidence behind the release decision is in the
**[Stable report](docs/stability-report.md)** and
**[production-readiness contract](docs/production-readiness.md)**.

The workbench opens on the production
[segment network](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=prod_geometry_segment_network).
Focused direct links remain available for cases such as
[baseline geometry](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=baseline_geometry),
[curves](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=curves), and
[Step functions](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=step_functions),
[function-coordinate Points](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=function_coordinate_points),
[Point3D projection](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=point3d_projection),
[spatial lines and planes](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=spatial_lines_planes),
[Plane3D surfaces](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=plane3d_surfaces),
[Polyhedron3D faces](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=polyhedron3d_faces),
[View3D default axes](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=view3d_default_axes),
[View3D center axes](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=view3d_center_axes),
[parallel constructions](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=parallel_constructions),
[Line arrows](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=line_arrows),
[triangle centers](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=triangle_centers),
[Intersection points](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=intersection_points),
[path intersections](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=intersection_paths),
[Polygon path intersections](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=polygon_path_intersections),
[Polygonal chains](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=polygonal_chains),
[Parallelograms](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=parallelograms),
[Regular polygons](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=regular_polygons),
[Radical axis](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=radical_axis),
[Circle-line pole point](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=pole_point),
[Circle tangents and polars](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=tangent_polar_circle),
[two tangents from one Point](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=tangent_to_circle),
[Ellipses](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=ellipses),
[Hyperbolas](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=hyperbolas),
[Parabolas](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=parabolas),
[Line tangents and polar alias](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=tangent_line),
[Curve tangents and polar alias](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=tangent_curve),
[Curve derivative](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=derivative_curve),
[Line, Circle, and Curve normals](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=normal_constructions),
[natural and cardinal splines](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=spline_curves),
[Riemann sums](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=riemann_sums),
[Box plots](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=box_plots),
[Comb curves](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=combs),
[Line and function inequalities](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=inequalities),
[Vector fields](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=vector_fields),
[Slope fields](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=slope_fields),
[Curve Boolean clipping](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=curve_boolean_clipping),
[Arc compositions](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=arc_compositions),
[Circumcircle creators](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=circumcircle_creators),
[Point reflections](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=point_reflections),
[two-line angle bisectors](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=bisector_lines),
and
[Sector compositions](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=sector_compositions).
The
[native JessieCode source](https://swithun-liu.github.io/cmp-jsxgraph/?caseId=jessiecode_native_source)
fixture sends one debug-only source envelope to both the official
`board.jc.parse` path and the native interpreter.

## Status

> [!IMPORTANT]
> **CMP JSXGraph is Stable for its documented JSXGraph `1.13.3` support
> scope.** This rating covers the construction-document, render-scene, and
> Point-interaction contracts described below. It is not a claim of complete
> JSXGraph API or element coverage.

The release decision is based on repository-controlled evidence:

| Evidence | Result |
| --- | ---: |
| Independent production scenarios | 30 |
| Declared capability coverage | 55/55 |
| Deterministic scene replay | 30/30 |
| Deterministic interaction replay | 8/8 |
| Separate generated Native stress inputs | 512 |
| Generated interaction updates | 64 |
| Native/Official visual pairs | 60 across Desktop and Compact |
| Lowest Desktop / Compact SSIM | 0.981779 / 0.961778 |
| JVM production soak | 900 renders, 1.649ms P95 |
| Runtime load matrix | Android, iOS, Desktop, Web passed |

The current compatibility baseline is JSXGraph `1.13.3`.

Implemented translation slices:

- coordinate constants from `src/base/constants.js`;
- core numeric, matrix, combinatoric, special and probability functions from
  `src/math/math.js` and `src/math/probfuncs.js`;
- complex arithmetic from `src/math/complex.js`;
- statistics, random distributions, histogram generation and vectorized
  arithmetic from `src/math/statistics.js`;
- linear systems, determinants, symmetric eigensystems, fixed and adaptive
  integration, natural cubic splines, scalar and multidimensional root
  finding, complex polynomial roots, domain search, minimization, and
  Runge-Kutta ODE solvers from `src/math/numerics.js`;
- foundational angles, distances, orientation, transformations,
  perpendicular/circumcenter constructions, and analytic intersections from
  `src/math/geometry.js`;
- JessieCode lexical analysis, the first statement/expression AST/parser
  slices, and their interpreter from `src/parser/jessiecode.js`, including
  statement lists, blocks, `if`/`else`, `while`/`do`/`for`, assignment,
  return/delete statements, multi-Board `use`, literals, variables, mutable
  arrays and objects, function/map expressions, nested closures, calls,
  creator attribute lists with recursive merge semantics, properties,
  indexes, conditionals,
  JavaScript-compatible coercion, core math functions, coordinate and geometry
  measurement built-ins, angle/combinatoric helpers, element naming and
  removal, `IfThen`, recursive `eval`, injectable `randint`, static dependency
  discovery, source locations, stable name-to-ID replacement and current-name
  restoration, reusable expression functions, persistent JessieCode sessions,
  assignment-LHS creator naming, native Point, Line, Arrow, Segment, Circle,
  Ellipse, Hyperbola, Parabola,
  including three-Point circumcircles, Circumcenter, CircumcircleMidpoint,
  Circumcircle,
  Point Reflection, MirrorElement's Point branch, MirrorPoint, Midpoint,
  ParallelPoint, Parallel, ArrowParallel, BisectorLines, Bisector, Incenter,
  Incircle, Intersection, OtherIntersection, Curve, CurveIntersection,
  CurveUnion, CurveDifference, FunctionGraph, Plot, StepFunction, Derivative,
  Spline, CardinalSpline, RiemannSum, BoxPlot, Comb, Inequality, VectorField,
  SlopeField,
  Polygon, PolygonalChain, Parallelogram,
  RegularPolygon, RadicalAxis, PolePoint, Circle/Point,
  Line/Point, and Curve/Point Tangent, Polar, Circle/Point TangentTo,
  PolarLine, Text, Arc,
  Sector, Angle, Semicircle,
  CircumcircleArc, MinorArc, MajorArc,
  CircumcircleSector, MinorSector, MajorSector, NonreflexAngle, ReflexAngle,
  2D Transformation creation through JessieCode or construction documents,
  transformed Points from one Transformation or a chain, and the 4x4
  Transformation kernel for every JSXGraph `1.13.3` 3D matrix form; the
  source-mapped `View3D`, `Point3D`, `Line3D`, Plane3D wireframe and finite
  surfaces, `Mesh3D`,
  `Axis3D`, `Face3D`, `Polyhedron3D`, and `transform3d` lifecycle from
  `src/3d/element3d.js`,
  `src/3d/view3d.js`, `src/3d/point3d.js`, `src/3d/linspace3d.js`,
  `src/3d/box3d.js`, `src/3d/face3d.js`, `src/3d/polyhedron3d.js`, and
  `src/base/transformation.js`, including parallel and central projection,
  numeric, homogeneous, and function-valued coordinates, 2D proxy rendering
  and drag projection, transformed 3D updates, finite plane outlines and
  visible `Mesh3D` wireframes, rectangle/triangle surface tiling, cyclic
  color arrays, HSL shaders, height colormaps, infinite plane/box clipping,
  projected Text3D,
  Ticks3D curves and labels, automatic `border`/`none` View3D axes, explicit
  Axes3D scene expansion, direct and transformed Polyhedron3D creation,
  cyclic/per-face Face3D styling, per-Polyhedron depth ordering,
  `applyOnce`, dependency updates, removal, resource limits, and structured
  failures;
  Point coordinates from scalar
  JessieCode functions or one function returning a coordinate array; the
  translated Point/Line/Circle/Polygon/Text `methodMap` subset, Point `X`/`Y`
  assignment,
  element names, bounds, child links, immediate movement, coordinate
  constraints, static and `<value>` Text content, regular-update assignment,
  and resource limits;
- Circle radii defined by JessieCode strings or JessieCode function values,
  including functional update dependencies that remain separate from
  geometric parents and `nonnegativeOnly` clamping;
- Point coordinates defined by two or more JessieCode strings/functions or by
  one JessieCode function returning an array, including Euclidean,
  homogeneous, and persistent session updates;
- event emitter behavior from `src/utils/event.js`;
- homogeneous user/screen coordinate conversion from `src/base/coords.js`;
- a bounded production construction-document path for `boundingBox` and
  ordered `objects[]`, currently creating Point, Line, Arrow, Segment with an
  optional numeric or JessieCode-string fixed-length parent, Circle including
  the three-Point circumcircle form, Ellipse, Hyperbola, Parabola,
  Circumcenter,
  CircumcircleMidpoint,
  Circumcircle, Point Reflection, MirrorElement's Point branch, MirrorPoint,
  Midpoint from two Points or one Line, OrthogonalProjection,
  PerpendicularPoint, Perpendicular, and PerpendicularSegment from one Point
  and one Line, ParallelPoint from three Points, finite or ideal Parallel
  and ArrowParallel lines, and three-Point Bisector, Incenter, and Incircle
  constructions, plus
  Semicircle, CircumcircleArc, MinorArc, and MajorArc compositions and
  CircumcircleSector, MinorSector, MajorSector, NonreflexAngle, and
  ReflexAngle compositions,
  Curve, CurveIntersection, CurveUnion, CurveDifference, FunctionGraph, Plot,
  StepFunction, Derivative, Spline, CardinalSpline, RiemannSum, BoxPlot, Comb,
  Inequality, VectorField, SlopeField, Polygon,
  PolygonalChain, Parallelogram, RegularPolygon,
  RadicalAxis, PolePoint, Circle/Point, Line/Point, and Curve/Point Tangent,
  Polar, Circle/Point TangentTo, PolarLine, Text, Arc, Sector, and Angle
  elements through the translated Board and native creator registry;
- a platform-independent Point/Line/Segment/Circle/Curve/Polygon/Text/Arc/
  Sector/Angle render scene
  consumed by Compose, including discrete data plots, right-open naive sampling
  for explicit-domain function and parametric curves, retained-array
  StepFunction expansion, viewport-sized BoxPlot outliers and VectorField/
  SlopeField arrowheads, filled Line and FunctionGraph inequalities, cubic
  Bezier arcs,
  filled sectors, fixed-radius angles, filled/bordered polygons, open
  polygonal chains, and anchored Canvas text using a bundled
  Arial-compatible font; static Line arrowheads
  cover upstream Canvas types `1..7`, endpoint shortening, filled and open
  heads, finite ends, and ideal-endpoint clipping; supported render items
  follow upstream `Options.layer` defaults and explicit layer overrides, with
  Polygon fill, borders, and implicit vertices ordered independently; the
  seven upstream Canvas dash patterns and `dashScale` stroke-width scaling
  apply to Point, Line, Circle, Curve, and Polygon-border strokes;
- a production `JsxGraphSession` that retains translated Board state, moves
  free non-fixed Points, updates dependent geometry, captures/restores
  interaction state, and rolls back moves that would make the scene invalid;
- `JsxGraphBoard`, a Compose Canvas surface with upstream-compatible Point hit
  tolerance, pointer capture, drag-offset preservation, and explicit
  interaction errors;
- an interactive Compose geometry playground backed by the translated
  line-circle intersection math;
- a separate `jsxgraph-debug-ui` comparison dependency with Source, official
  JSXGraph `1.13.3`, and native Compose previews.

The construction-document, scene-rendering, and Point-interaction slice
described by the 30-case production corpus is Stable. The public native
JessieCode source/session API is a bounded preview and is not yet part of that
Stable contract. The remaining JessieCode creator registry, visual-property
and function-valued element mutation, Slider/Glider-backed built-ins,
`import`/`$log`/`D`, the complete element `methodMap`, the remaining
construction-document element types and attributes, and the complete element
renderer are not yet translated. The current 3D preview does not yet include
global View3D `depthOrder`/layer configuration, camera controls, gliders,
animations, Stable qualification, or the complete 3D APIs.

Symbolic algebra (`src/unused/symbolic.js`) is intentionally out of scope for
the initial implementation.

## Translation Model

Kotlin implementations retain links to their upstream source files and
function names. This makes upstream JSXGraph changes reviewable as explicit
translation diffs instead of independent reimplementations.

See [docs/translation-status.md](docs/translation-status.md) for the exact
upstream commit and source-to-Kotlin mapping. Intentional edge-case
differences are tracked in
[docs/translation-deviations.md](docs/translation-deviations.md).
Production milestones and acceptance gates are tracked in
[docs/production-roadmap.md](docs/production-roadmap.md). The current release
evidence is recorded in [docs/stability-report.md](docs/stability-report.md).

The intended runtime pipeline is:

```text
JSXGraph input
    -> Kotlin parser and JessieCode runtime
    -> translated Board, geometry, dependency and update engines
    -> platform-independent render model
    -> Compose Canvas
```

The runtime does not use WebView or an embedded JavaScript engine.

## Construction Source

The first production source contract maps directly to ordered
`Board.create(type, parents, attributes)` calls:

```json
{
  "schemaVersion": 1,
  "boundingBox": [-5, 5, 5, -5],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "A",
      "type": "point",
      "parents": [1, 2],
      "attributes": {"name": "", "withLabel": false}
    },
    {
      "id": "lineA",
      "type": "line",
      "parents": ["A", [3, -1]],
      "attributes": {"name": "", "withLabel": false}
    }
  ]
}
```

`JsxGraphEngine.parse(source)` returns
`GMResult<JsxGraphScene, JsxGraphDocumentError>`. Current accepted object
types are `transform`, `view3d`, `transform3d`, `point3d`, `point`, `line`,
`arrow`, `segment`, `circle`, `curve`,
`ellipse`, `hyperbola`, `parabola`, `curveintersection`, `curveunion`,
`curvedifference`,
`functiongraph`, `plot`,
`stepfunction`, `derivative`, `spline`, `cardinalspline`, `riemannsum`,
`boxplot`, `comb`, `inequality`,
`normal`,
`circumcenter`, `circumcirclemidpoint`, `circumcircle`, `reflection`,
`mirrorelement`, `mirrorpoint`, `midpoint`, `orthogonalprojection`,
`perpendicularpoint`, `perpendicular`, `perpendicularsegment`,
`parallelpoint`, `parallel`, `arrowparallel`, `bisector`, `incenter`,
`incircle`, `polygon`, `polygonalchain`, `parallelogram`, `regularpolygon`,
`radicalaxis`, `polepoint`, `tangent`, `polar`, `tangentto`, `polarline`,
`text`, `arc`, `semicircle`, `circumcirclearc`, `minorarc`, `majorarc`,
`sector`, `circumcirclesector`, `minorsector`, `majorsector`, `angle`,
`nonreflexangle`, and `reflexangle`.
Continuous curves currently require `doAdvancedPlot: false`; Polygon,
PolygonalChain, Parallelogram, and RegularPolygon currently support Point or
coordinate-array vertices,
`withLines`, top-level fill styling, and default border/vertex styles.
PolygonalChain preserves the upstream open vertex list, open Segment border,
transparent default fill, length semantics, and helper ownership. Text
supports static strings/numbers,
dynamic `<value>` JessieCode terms, constrained coordinates, number formatting,
font size, color/opacity, and horizontal/vertical anchors. The Stable Arc and
Sector subset supports three Point or coordinate-array parents,
minor/major/auto selection, both orientations, and cubic Bezier rendering. Arc
additionally supports the four-point `useDirection` form with explicit
direction-point dependency updates as part of the Stable corpus. Angle
supports the three-point form with numeric or `auto` radius and sector display.
Unsupported types, parent forms, rich-text features, plotting modes, nested
styles, and visual attributes fail explicitly instead of being omitted from
the native render.

Supported elements accept a finite non-negative integer `layer`. Compose
orders Grid, Axis, top-level elements, and Polygon fill/borders/implicit
vertices by the JSXGraph `1.13.3` default or explicit layer, then by creation
order. Nested Polygon `vertices`/`borders` styling, dynamic layer mutation, and
the remaining Axis/Ticks/Grid option surface are not yet translated.

Circle supports Point, numeric, JessieCode string, Line, Circle, and
three-Point circumcircle radius forms. The native JessieCode creator also
accepts a genuine JessieCode function as the radius parent in either upstream
order. Its discovered element dependencies update the Circle after Point
movement, and `nonnegativeOnly: true` clamps a negative result to zero.
Compile, invocation, and nonnumeric-result failures remain structured.

Circumcenter and its `circumcirclemidpoint` alias accept three
Point/reference/coordinate parents. Circumcircle wraps the same constrained
center as a hidden registered sub-element and preserves upstream `subs`,
`inherits`, dependency, helper ownership/removal, degenerate arithmetic, and
atomic rollback behavior. The explicit creators are available in the
construction-document and native JessieCode preview APIs with same-source
static and parent-drag parity, but remain outside the 30-case Stable corpus.

Reflection accepts an existing Point and Line; MirrorElement's translated
branch and MirrorPoint accept two existing Points. They preserve the upstream
dynamic reflect/rotate transform, non-draggable output, parent metadata, and
the intentionally asymmetric removal dependency on the Line or mirror center.
Coordinate-array parents remain rejected because JSXGraph `1.13.3` rejects
them in these creator paths despite broader documentation. The remaining
Line/Curve/Polygon/Circle Reflection and MirrorElement branches are explicitly
unsupported. The Point branches have same-source static and source-drag
parity but remain outside the 30-case Stable corpus.

Segment supports two Point/reference/coordinate parents and an optional
numeric, JessieCode-string, or JessieCode-function third parent for fixed
length. Fixed-length updates preserve the upstream endpoint ownership,
negative-length and `nonnegativeOnly` rules, coincident-point random-direction
behavior, and discovered functional dependencies. Compile, invocation, and
nonnumeric-result failures remain structured; helper Points created by a
failed native JessieCode factory call are removed atomically.

Midpoint supports two Point/reference/coordinate parents or one Line
reference. It follows either endpoint after Board updates, preserves
existing-parent ownership, and recursively removes coordinate helper Points
with the Midpoint. JSXGraph `1.13.3` documents a function returning a
coordinate array as a generic Point parent, but `createMidpoint` first applies
`board.select` and rejects that form; native JessieCode preserves the observed
failure explicitly.

OrthogonalProjection, PerpendicularPoint, Perpendicular, and
PerpendicularSegment accept one Point/reference/coordinate parent and one Line
reference in either order. They preserve the upstream projection,
perpendicular-coefficient, endpoint-order, dependency, and helper ownership
semantics as their defining Point or Line moves.

ParallelPoint accepts three Point/reference/coordinate parents or one Line and
one Point in either order. Parallel accepts the same forms: three Points produce
a finite helper endpoint that can be shortened with `straightFirst` and
`straightLast`, while a Line and Point produce the upstream ideal helper point.
The translated lifecycle preserves dependency updates, parent-order metadata,
helper ownership, and helper survival after removing a Line-backed Parallel.
These forms are available in the construction-document and native JessieCode
preview APIs but remain outside the 30-case Stable corpus.

Intersection and OtherIntersection map to
`src/base/point.js -> createIntersectionPoint` /
`createOtherIntersectionPoint` and
`src/math/geometry.js -> intersectionFunction` /
`otherIntersectionFunction`. The translated dispatch covers Line/Segment/
Circle, discrete and continuous Curve, Arc, Sector, Polygon/Line, and
Polygon/path pairs. `Clip.findIntersections` now covers Polygon intersections
with Circle, Curve, Arc, Sector, and Polygon paths, including ordered
crossings, touching and collinear overlap endpoints, reverse parent order,
ideal/non-real results, index validation, and dependency updates. The APIs
remain available through construction documents and native JessieCode.
Ellipse, Hyperbola, and Parabola construction are translated, but Conic
Intersection and OtherIntersection dispatch remains explicitly unsupported
pending its own source-mapped numerical, lifecycle, resource-limit, and parity
evidence.
CurveIntersection, CurveUnion, and CurveDifference translate the complete
`src/math/clip.js` Greiner-Hormann chain for Curve, Polygon, Circle, Arc, and
Sector paths, including degenerate intersection classification, entry/exit
marking, empty/containment cases, multi-component output, tracing, regular
Board updates, structured topology failures, and filled closed-path rendering.
They are available through construction documents and native JessieCode.
Dynamic `alwaysIntersect`/`precision` visual properties remain pending. All
four focused same-source fixtures remain outside the 30-case Stable corpus.

StepFunction translates `src/base/curve.js -> createStepfunction`. It retains
the two source terms, rebuilds `dataX`/`dataY` on each regular Board update,
and emits `0` or `2n - 1` points from the X-term length, including upstream
missing-Y path breaks. JSON construction documents accept numeric arrays;
native JessieCode additionally preserves array identity and JavaScript
function-arity behavior. Its focused static parity case remains outside the
30-case Stable corpus.

PolygonalChain translates
`src/base/polygon.js -> createPolygonalChain` by creating the ordinary Polygon
first, then removing its duplicated closing vertex and final closing Segment.
The translated Board preserves official border order, explicit versus
coordinate-created Point ownership, `Area`/`Perimeter`/`L`/bounds behavior,
resource limits, and parent updates. Compose closes only the optional fill
primitive while leaving the Segment border open. Its focused static and
parent-drag parity evidence remains outside the 30-case Stable corpus.

Parallelogram translates `src/base/polygon.js -> createParallelogram` by
creating a constrained `ParallelPoint` from three Point/reference/coordinate
parents and then a Polygon ordered as
`[point1, point2, parallelPoint, point3]`. It exposes the helper through
`parallelPoint`, forces the output and helper draggable and the helper
non-fixed, and preserves the official border storage and Board creation order.
Removing the Parallelogram removes only its Polygon and borders; the helper
survives, and coordinate-created source Points remain owned by that helper.
Factory failures return structured errors and remove temporary Points
atomically. Its focused static and parent-drag parity evidence remains outside
the 30-case Stable corpus.

RegularPolygon translates
`src/base/polygon.js -> createRegularPolygon` in both official forms. Two
Points plus numeric `n` generate `ceil(n) - 2` CAS Point helpers through the
same chained rotations, including the original non-integer-angle behavior;
an existing Point list receives those rotations in place. Generated helpers
honor a complete nested `vertices.ids` list, are forced draggable/non-fixed,
survive Polygon removal, and update from either source Point. Coordinate
parents retain the official Polygon ownership, while structured failures roll
back temporary Points and existing-Point transforms atomically. Non-finite
counts are rejected explicitly to avoid the upstream positive-infinity loop.
Its focused static and parent-drag parity evidence remains outside the 30-case
Stable corpus.

RadicalAxis translates `src/base/line.js -> createRadicalAxis`. Two registered
Circles drive the official standard-form coefficient closure and its two
implicit constrained CAS Point endpoints. The output Line is non-draggable,
retains nested `point1`/`point2` identity, exposes the two Circles as parents,
updates through Circle dependencies, and leaves its helper Points registered
after output or parent removal. Degenerate concentric, identical, and repeated
Circle forms preserve official zero-helper/NaN-line arithmetic. Kotlin rejects
duplicate IDs with a structured error and atomically removes newly created
helpers instead of overwriting the Board registry as JSXGraph does. Its
focused static and radius-Point-drag Desktop/Compact parity evidence remains
outside the 30-case Stable corpus.

PolePoint translates `src/base/point.js -> createPolePoint` for the currently
translated Circle/Line parent pair in either input order. The factory
canonicalizes public parents to Circle then Line and evaluates the official
`Numerics.det(circle.quadraticform, line.stdform)` homogeneous coordinate
closure on each Board update. The constrained output follows both parent
dependency graphs, preserves official direct and parent-removal behavior, and
keeps non-finite degenerate arithmetic. Invalid, cross-Board, unregistered,
and duplicate-ID inputs return structured errors with atomic rollback.
Ellipse, Hyperbola, and Parabola construction are translated, but their
Conic/Line PolePoint forms remain explicitly unsupported pending dedicated
source-mapped interop and parity evidence. Its
focused static and four-parent-drag Desktop/Compact parity evidence remains
outside the 30-case Stable corpus.

Tangent translates the Circle/Point, Line/Point, and Curve/Point branches of
`src/base/line.js -> createTangent`, plus the registered `polar` alias and
Circle-only `createPolarLine` wrapper. The Circle branch uses the exact
quadratic-form matrix-vector product and two implicit constrained CAS Point
endpoints. The Line branch instead reuses the source Line endpoints, ignores
nested helper identity, remains draggable/unconstrained, and does not attach a
child edge to either the source Line or parameter Point. Its endpoint Points
own the actual dependency and removal edges. For continuous Curves, a
FunctionGraph differentiates at the Point's X coordinate, while a true
function-valued parametric Curve first projects the Point to its nearest
parameter. JessieCode string x-terms retain upstream `functiongraph`
classification even in the four-parent `curve(x,y,min,max)` form. A data Plot
projects to the nearest degree-one segment and uses that segment's cross
product. Curve Tangents use two hidden constrained helpers and only the Point
owns the direct removal edge. `tangent` and `polar` preserve input parent order
and `elType=tangent`; `polarline` canonicalizes its parents to Circle then
Point and changes only `elType`. Invalid, cross-Board, unregistered, one-point
Plot, and duplicate-ID inputs return structured errors with atomic rollback.
Ellipse, Hyperbola, and Parabola construction are translated, but their
Tangent/Polar and PolarLine Conic forms remain explicitly unsupported pending
dedicated interop evidence.
Turtle and one-parent Glider branches also remain unsupported.
Focused static and parent-drag Desktop/Compact parity evidence remains outside
the 30-case Stable corpus.

TangentTo translates the Circle branch of
`src/base/line.js -> createTangentTo`. It first creates the source Point's
polar, selects one polar/Circle intersection using upstream numeric truthiness
(`0` and `NaN` select the first branch; every other numeric value, including
either infinity, selects the second), then creates the tangent at that contact
Point. The returned Line exposes its registered polar and contact Point,
preserves nested identities, visibility, fixed state, straight-end settings,
dependencies, removal behavior,
non-real/degenerate arithmetic, and the official default `dash: 3` polar
style. Public JessieCode and construction-document scenes expand one
`tangentto` request into the polar Line, contact Point, and tangent Line, so
resource limits charge three scene objects. Duplicate IDs and partial-stage
failures roll back atomically. Ellipse, Hyperbola, and Parabola construction
are translated, but their TangentTo Conic branches remain explicitly
unsupported pending dedicated interop evidence. Focused static and source-Point-drag
Desktop/Compact parity evidence remains outside the 30-case Stable corpus.

Ellipse translates `src/element/conic.js -> createEllipse`. It supports
Point/reference/coordinate foci with either a Point on the Ellipse or a
numeric/function-valued major axis, plus optional numeric parameter domains.
The translated Curve preserves Conic type, center/midpoint, foci, `majorAxis`,
`quadraticform`, parent updates, helper ownership/removal, nested center
identity, and JavaScript `Double` behavior for degenerate axes. Native
JessieCode and construction-document paths enforce sample/object limits and
return structured failures with atomic helper rollback. Focused static and
point-parent-drag Desktop/Compact parity passes, but this slice remains
outside the 30-case Stable corpus. Conic interoperability with Tangent, Polar,
PolarLine, PolePoint, TangentTo, Normal, Intersection, and OtherIntersection
remains explicitly unsupported pending dedicated translation and evidence.

Hyperbola translates `src/element/conic.js -> createHyperbola`. It supports
Point/reference/coordinate foci with either a Point on the Hyperbola or a
numeric/function-valued major axis, plus optional numeric parameter domains.
The translated Curve preserves Conic type, center/midpoint, foci, `majorAxis`,
`quadraticform`, parent updates, helper ownership/removal, nested center
identity, and JavaScript `Double` behavior for degenerate axes. Native
JessieCode and construction-document paths enforce sample/object limits and
return structured failures with atomic helper rollback. Focused static and
point-parent-drag Desktop/Compact parity passes, but this slice remains
outside the 30-case Stable corpus. Adaptive plotting and all Conic
interoperation remain explicitly unsupported pending dedicated translation
and evidence.

Parabola translates `src/element/conic.js -> createParabola`. It supports a
Point/reference/function-returning-Point or coordinate focus and either a
registered Line or an implicit two-Point directrix, plus optional numeric
parameter domains. The translated Curve preserves Conic type, the constrained
focus projection exposed as `center`/`midpoint`, focus/directrix dependencies,
`quadraticform`, parent updates, helper ownership/removal, and JavaScript
`Double` behavior at the default-domain singularity and degenerate
directrices. Native JessieCode and construction-document paths enforce
sample/object limits, unique IDs, and atomic helper rollback; nested implicit
Line metadata is limited to identity and regular-update fields. Focused static
and focus-drag Desktop/Compact parity passes, but this slice remains outside
the 30-case Stable corpus. Adaptive plotting and all Conic interoperation
remain explicitly unsupported pending dedicated translation and evidence.

Derivative translates `src/base/curve.js -> createDerivative`. It reuses
`Numerics.D` for the source Curve's `X` and `Y` functions, renders
`Y'(t) / X'(t)` against `X(t)`, inherits the source domain, and preserves the
official `curveType=parameter` and metadata-only parent relation. FunctionGraph,
parametric, and linearly interpolated data-Plot parents are supported, including
the viewport-padded default data domain, regular Board recomputation, and the
official behavior where removing the source does not recursively remove the
Derivative. Invalid parents, duplicate IDs, and point-count overflow return
structured errors. Focused static and coefficient-Point-drag Desktop/Compact
parity evidence remains outside the 30-case Stable corpus.

Normal translates the Line/Point, Circle/Point, and Curve/Point branches of
`src/base/line.js -> createNormal` in either parent order. The Line branch
reuses the supplied Point and creates the official ideal direction helper,
including its `point`, `subs.point`, and duplicated `inherits` identity. The
Circle branch reuses the Circle midpoint and supplied Point. FunctionGraph,
true parametric Curve, and degree-one data Plot branches use the upstream
derivative or nearest-projection formulas and two hidden constrained endpoints.
Both explicit parents own the output removal edge, while removing the output
leaves hidden helpers registered. Invalid, cross-Board, unregistered,
one-point Plot, unsupported-degree, and duplicate-ID inputs return structured
errors with atomic rollback. The upstream coordinate-array Point form overflows
the JavaScript stack in `1.13.3`; Kotlin rejects it explicitly. Glider, Turtle,
transformed-Curve, degree-three Plot/Bezier, and translated Conic branches
remain unsupported. Those branches require dedicated interop evidence rather
than using the generic Curve path implicitly.
Focused static and parent-drag Desktop/Compact parity evidence remains outside
the 30-case Stable corpus.

Spline and CardinalSpline translate `src/base/curve.js -> createSpline` and
`createCardinalSpline` plus their `src/math/numerics.js` interpolation
functions. Natural Spline accepts registered Points, coordinate pairs,
parallel X/Y arrays, and coordinate functions, sorts knots on every update,
and preserves the upstream metadata-only Point-parent lifecycle.
CardinalSpline accepts existing or generated Points, dynamic tension, uniform,
centripetal, and chordal parameterization, and the upstream `createPoints` and
`isArrayOfCoordinates` modes. Both creators have bounded JSON/JessieCode paths,
structured evaluation failures, official lifecycle fixtures, and focused
static plus tension-drag Desktop/Compact parity. Kotlin rejects fewer than two
interpolation points instead of allowing the upstream delayed invalid
geometry. This slice remains outside the 30-case Stable corpus.

RiemannSum translates `src/base/curve.js -> createRiemannsum` over the
existing `Numerics.riemann` port. It accepts one function or an ordered
lower/upper function pair, numeric/string/function rectangle counts,
string/function approximation types, optional dynamic interval bounds, and
all upstream left/right/middle/lower/upper/random/Simpson/trapezoidal modes.
The Curve preserves `curveType=plot`, `Value()`, regular recomputation, default
yellow fill at opacity `0.3`, and dependency/removal behavior. Output point
growth is bounded before or immediately after dynamic updates, and failed
functions return structured errors without retaining the upstream partial
Curve. Focused static and bar-count-drag Desktop/Compact parity evidence
remains outside the 30-case Stable corpus.

BoxPlot translates `src/base/curve.js -> createBoxPlot` and reuses the
translated Statistics five-number summary. It accepts dynamic quantile,
axis, and width terms, vertical or horizontal direction, `smallWidth`, and
the upstream circle/cross/square/diamond/diamond2/plus/minus/divide outlier
faces. Its Curve keeps the 19-point whisker/body path, blue fill defaults,
dependency/removal behavior, and viewport CSS-pixel outlier sizing. Static
and dynamic output growth is bounded, malformed terms fail atomically, and
focused static plus driver-drag Desktop/Compact parity evidence remains
outside the 30-case Stable corpus.

Comb translates `src/element/comb.js -> createComb` into an ordinary
degree-one Plot Curve with `NaN` path breaks. It accepts Point references or
coordinate-array endpoints plus numeric or function-valued `frequency`,
`width`, `angle`, and `reverse` attributes. Coordinate endpoints become
hidden, non-fixed helper Points that survive Curve removal, matching upstream;
creation failures clean them atomically instead of retaining JSXGraph's
observed partial helper. Non-positive/non-finite frequency and point growth
above the configured limit fail structurally. Focused static plus driver-drag
Desktop/Compact parity evidence remains outside the 30-case Stable corpus.

Inequality translates `src/element/composition.js -> createInequality` for
Line and FunctionGraph sources. It preserves the five-point expanded
half-plane polygon for Lines, segmented FunctionGraph closure across
non-finite breaks, Board-bound-dependent geometry, default transparent stroke
and orange fill at opacity `0.2`, dynamic `inverse`, metadata-only parents,
and source-removal survival. Extra parents are ignored as upstream does;
unsupported source kinds and excessive static or dynamic point growth return
structured errors. Focused static, Line-parent-drag, and
FunctionGraph-driver/inverse-drag Desktop/Compact parity evidence remains
outside the 30-case Stable corpus.

VectorField translates
`src/element/vectorfield.js -> createVectorField`. It accepts either two
component functions/expressions or one function/expression returning a
two-value array, plus dynamic three-term X/Y meshes, `scale`, and nested
`arrowHead.enabled/size/angle`. The exact inclusive loop, fractional/zero/
negative step behavior, `NaN` path breaks, nonzero-vector arrow rule, and
metadata-only function dependencies are preserved. Static and dynamic output
growth is bounded, failures are structured and atomic, and Compose resolves
arrow size from the actual viewport CSS-pixel scale. Focused static plus
driver-drag Desktop/Compact parity evidence remains outside the 30-case Stable
corpus.

SlopeField translates
`src/element/vectorfield.js -> createSlopeField` as the upstream wrapper over
VectorField. It accepts one scalar function or expression, preserves the
exact `[1 / sqrt(1 + z * z), z / sqrt(1 + z * z)]` normalization and
non-finite arithmetic, defaults arrowheads to disabled, and reuses the
dynamic mesh, scale, viewport arrow, bounded-output, and rollback behavior.
Focused static plus driver-drag Desktop/Compact parity evidence remains
outside the 30-case Stable corpus. Runtime `setF` mutation remains pending
with the broader Curve mutation API.

Arrow accepts the translated two-Point Line parent forms. ArrowParallel wraps
the three-Point and Line/Point Parallel forms. Both force their visible
`straightFirst` and `straightLast` attributes to `false`, retain upstream
vector identity, default to a type-1 final head of size `6`, preserve explicit
`lastArrow: false`, and accept static Boolean or object-valued
`firstArrow`/`lastArrow` settings for types `1..7`. Compose translates the
Canvas endpoint shortening and filled/open head paths, including ideal
endpoints and the type-7 fixed effective size. Dynamic arrow attributes,
highlight-state sizes, Curve/Arc/Sector arrows, and the remaining renderer
surface are pending. The static and parent-drag focused parity fixture remains
outside the 30-case Stable corpus.

Bisector, Incenter, and Incircle accept three Point/reference/coordinate
parents. They preserve the JSXGraph `1.13.3` angle-bisector and weighted
incenter formulas, dependency updates, parent metadata, hidden helper
ownership, removal behavior, and atomic registration rollback. Incircle uses
the constrained Incenter as its hidden center and updates its radius from the
three side lengths. These forms are available in the construction-document
and native JessieCode preview APIs and have same-source static and parent-drag
parity, but remain outside the 30-case Stable corpus.

BisectorLines accepts two existing Line references through native JessieCode
and returns a non-Board-registered `Composition` whose addressable `line1` and
`line2` members are ordinary registered Lines. It preserves the normalized
coefficient sum/difference formulas, four hidden constrained helper Points,
source-parent metadata, dependency updates, nested output-line attributes,
selection/removal behavior, degenerate `NaN` propagation, and atomic rollback.
The top-level creator `layer` does not flow into the nested Lines, matching
JSXGraph `1.13.3`; composition-wide visual mutation remains unavailable until
the mutable visual-property model is translated. JSON construction documents
reject this compound return type explicitly. The focused static and parent-drag
parity fixture remains outside the 30-case Stable corpus.

Semicircle accepts two Point/reference/coordinate parents and owns a hidden
Midpoint center. CircumcircleArc, MinorArc, and MajorArc accept three such
parents; CircumcircleArc owns a hidden Circumcenter and uses the third source
Point as the direction selector, while MinorArc and MajorArc force their
respective selection modes. Their dependencies, helper ownership/removal,
degenerate behavior, native JessieCode and construction-document entry points,
and same-source static/parent-drag parity are translated. These compositions
remain outside the 30-case Stable corpus.

CircumcircleSector accepts three Point/reference/coordinate parents, owns a
hidden Circumcenter, and passes the third source Point through the four-parent
direction-selection path. MinorSector and MajorSector preserve the supplied
center and endpoints while forcing their respective selection modes.
NonreflexAngle and ReflexAngle force minor/major selection and preserve the
upstream radian-default, unit-aware `Value()` behavior. Their dependencies, helper
ownership/removal, degenerate behavior, native JessieCode and
construction-document entry points, and same-source static/parent-drag parity
are translated. These compositions remain outside the 30-case Stable corpus.

For interactive rendering, `JsxGraphEngine.createSession(source)` returns a
`JsxGraphSession`. Pass that session to `JsxGraphBoard`; use
`captureInteractionState()` and `restoreInteractionState(...)` to preserve
free Point positions across screen or process recreation. Fixed, constrained,
hidden, and non-Point elements are not draggable.

## Native JessieCode

`JsxGraphJessieCode.parse(source)` evaluates the translated, bounded
JessieCode subset and returns a `JsxGraphScene`.
`JsxGraphJessieCode.createSession()` retains globals, functions, closures,
selected Board state, created elements, and stored source across
`execute(...)` calls and exposes `movePoint(...)` for native interaction.
`JsxGraphJessieCodeLimits` bounds parsing, evaluation, stored source,
collections, scene objects, curve points, polygon vertices, and text.

This API does not execute arbitrary JavaScript and does not expose a DOM,
browser runtime, internal AST, or interpreter values. Unsupported syntax,
creators, parent forms, attributes, properties, and methods return
`GMResult.Err<JsxGraphJessieCodeError>`. The JSON JessieCode envelope used by
the case workbench is a debug-only parity transport, not a production input
format.

## Build

JDK 17 or newer is required.

```bash
./gradlew :jsxgraph-core:allTests
./gradlew :jsxgraph-compose:allTests :jsxgraph-debug-ui:allTests
./gradlew verifyPublicationCoordinates
./gradlew \
  :sample:androidApp:assembleDebug \
  :sample:androidApp:assembleRelease \
  :sample:desktopApp:createDistributable \
  :sample:webApp:wasmJsBrowserDistribution
ANDROID_SERIAL=<device-serial> ./tools/capture-android-parity.sh
```

See [`docs/testing.md`](docs/testing.md) for the screenshot parity and stable
gates.

## Coordinates

The project group and package namespace include the author name:

- package root: `com.swithun.jsxgraph`
- core artifact: `com.swithun:jsxgraph-core:0.1.0`
- Compose artifact: `com.swithun:jsxgraph-compose:0.1.0`

`jsxgraph-debug-ui` is intentionally excluded from the production publication
set. The first public artifact repository release has not been uploaded yet;
until then, consume the repository modules directly or publish them to a local
Maven repository.

## License

CMP JSXGraph is released under the MIT License. JSXGraph is used under its
MIT license option. See [LICENSE](LICENSE) and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
