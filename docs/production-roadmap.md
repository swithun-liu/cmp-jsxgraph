# Production Roadmap

## Scope

- Upstream baseline: JSXGraph `1.13.3`
- Runtime: pure Kotlin Multiplatform
- Rendering: Compose Multiplatform Canvas
- Platforms: Android, iOS, JVM/Desktop, Wasm
- Explicit exclusion: `src/unused/symbolic.js`

The upstream baseline contains 104 JavaScript source files outside
`src/unused`. Completion is tracked by source symbol and behavior, not by raw
line count.

## Current Release Decision

CMP JSXGraph `0.1.0` is **Stable for the documented support scope**:
Point, Line, Segment with an optional numeric or JessieCode-string
fixed-length parent, Circle including the three-Point circumcircle form,
Midpoint from two Points or one Line, OrthogonalProjection,
PerpendicularPoint, Perpendicular, and PerpendicularSegment from one Point and
one Line, Curve, FunctionGraph, Plot, Polygon, Text, three-point Arc,
four-point Arc with `useDirection`, Sector, Angle, dynamic Segment length,
Midpoint and orthogonal-construction dependency updates, string-radius Circle
updates with `nonnegativeOnly`, and free Point interaction through the bounded
construction-document API. The qualification evidence is recorded in
[`stability-report.md`](stability-report.md).

This promotion does not close the translation roadmap. Remaining JSXGraph
element families, mutation APIs, interaction modes, and renderer features stay
outside the Stable contract until their own source-mapped implementation and
qualification batches pass.

## Post-0.1.0 Translation Progress

- Coordinate systems and transforms: the numeric and dynamic 2D kernel from
  `src/base/transformation.js` is translated, including number, JessieCode,
  structured function, live Point/Line, and dynamic matrix parameters;
  one-time application; static cloning/composition; `bindTo`; static
  `meltTo`; transformed-position preimages; the native JessieCode `transform`
  creator; construction-document transform IDs; and transformed-Point
  creation from one transform or a chain. The 4x4 `setMatrix3D` kernel covers
  translate, three- and four-parameter scale, rotate/rotateX/rotateY/rotateZ,
  affine, affinematrix, generic, and matrix forms with scalar, vector,
  JessieCode, and structured dynamic evaluation.
- View3D, Point3D, Line3D, Plane3D wireframe, Mesh3D, Axis3D, Face3D, and
  Polyhedron3D: the bounded lifecycle from `src/3d/element3d.js`,
  `src/3d/view3d.js`, `src/3d/point3d.js`, `src/3d/linspace3d.js`,
  `src/3d/box3d.js`, `src/3d/face3d.js`, `src/3d/polyhedron3d.js`,
  `src/base/transformation.js`, and `src/options3d.js` is translated. It
  covers View3D registration and selection, parallel and central camera
  matrices, forward/inverse projection helpers, cube bounds, dynamic 3D
  coordinates/vectors/ranges, ordinary 2D proxies, Line3D and Plane3D
  transforms, finite plane outlines and Mesh3D wireframes, fully infinite
  plane/box clipping, projected Text3D, Ticks3D curves and labels, automatic
  `border`/`none` View3D axes, explicit Axes3D scene expansion, direct and
  transformed Polyhedron3D creators, Point-backed/function-valued/homogeneous
  vertices, cyclic and per-face styles, HSL Face3D shaders, local ascending
  face-depth ordering, `transform3d` binding, updates, `applyOnce`, removal,
  JSON and JessieCode entry points, resource limits, rollback, and structured
  failures. Official lifecycle fixtures and focused JVM tests pass. Point3D
  has focused static plus proxy-drag visual parity; Line3D, finite Plane3D
  with its visible Mesh3D, Axis3D, automatic border axes, and Polyhedron3D
  have focused Desktop/Compact static parity.
- Still pending around transforms and 3D: transformed Text/Image and general
  element rendering, dynamic `meltTo` beyond the documented JSXGraph `1.13.3`
  null-clone defect, Plane3D shader/colormap/color-array surface modes,
  global View3D
  `depthOrder`/layer configuration, camera controls, Point3D gliders and
  animations, Stable qualification, and the remaining 3D APIs.
  The upstream 3D `generic`
  six-evaluator/16-read defect is preserved as a structured evaluation error.
- Function-coordinate Points: native JessieCode now supports mixed
  number/string/function coordinate terms and one function returning a
  numeric coordinate array, with fresh evaluation budgets, persistent Board
  updates, structured failures, and same-source static/interaction parity.
  The documented function-parent route through `createMidpoint` remains
  rejected because JSXGraph `1.13.3` itself loses the function in its leading
  `board.select` pass.
- StepFunction: `src/base/curve.js -> createStepfunction` now retains source
  arrays, rebuilds output during regular Board updates, preserves empty,
  mismatched-Y, and function-arity behavior, and enforces expanded point
  limits through JSON and JessieCode entry points. Core, creator, persistent
  session, document, official fixture, and Desktop/Compact same-source gates
  pass. It remains outside the Stable corpus until independent production
  qualification is added.
- PolygonalChain: `src/base/polygon.js -> createPolygonalChain` now follows
  the official Polygon wrapper and removes only the duplicated closing vertex
  and final closing Segment. Border storage order, registered/helper Point
  ownership and removal, `Area`/`Perimeter`/`L`/bounds behavior,
  JSON/JessieCode limits, open Compose rendering, and focused static plus
  parent-drag parity pass. Nested Polygon styles, mutable vertex APIs,
  transformed chains, labels, hit testing, independent production cases, and
  Stable qualification remain pending.
- Parallelogram: `src/base/polygon.js -> createParallelogram` now composes the
  translated ParallelPoint and Polygon factories with the official vertex,
  border, and Board creation order. It preserves the exposed helper identity,
  forced draggable/non-fixed helper state, source updates, surviving-helper
  removal behavior, coordinate-Point ownership, nested `parallelpoint`
  attributes, JSON/JessieCode limits, and atomic rollback. Core, creator,
  document/session, official fixture, and focused static plus parent-drag
  parity gates pass. Mutable Polygon APIs, labels, hit testing, an independent
  production case, and Stable qualification remain pending.
- RegularPolygon: `src/base/polygon.js -> createRegularPolygon` now covers
  both `[point1, point2, n]` generation and the existing `n`-Point form. The
  translation preserves the original rotation angle, non-integer
  `ceil(n)` loop behavior, generated CAS Point identity, complete
  `vertices.ids` mapping, forced draggable/non-fixed helpers, source updates,
  Point/Polygon ownership, removal behavior, and Board creation order.
  JSON/JessieCode vertex limits and atomic rollback prevent unbounded or
  partial construction. Core, creator, document/session, official fixture,
  and focused static plus parent-drag parity gates pass. Labels, complete
  nested Polygon styling, an independent production case, and Stable
  qualification remain pending.
- RadicalAxis: `src/base/line.js -> createRadicalAxis` now covers two
  registered Circle parents, the exact standard-form coefficient closure,
  one-function constrained helper Points, parent/dependency rebinding,
  dynamic Circle updates, successful-removal helper survival, repeated and
  degenerate Circle arithmetic, nested helper identity, native JessieCode and
  construction-document entry points, and atomic duplicate-ID rollback. Core,
  creator, document/session, official fixture, and focused static plus
  radius-Point-drag Desktop/Compact parity gates pass. Nested helper styling,
  labels, an independent production case, and Stable qualification remain
  pending.
- PolePoint: `src/base/point.js -> createPolePoint` now covers registered
  Circle/Line parents in either order, canonical parent metadata, the exact
  determinant coordinate closure, constrained dependency updates, direct and
  parent removal, non-finite degenerate arithmetic, native JessieCode and
  construction-document entry points, and structured atomic rollback. Core,
  creator, document/session, official fixture, and focused static plus
  four-parent-drag Desktop/Compact parity gates pass. Ellipse, Hyperbola, and
  Parabola construction are translated, but their Conic/Line PolePoint forms,
  an independent production case, and Stable qualification remain pending.
- Tangent/Polar: the Circle/Point, Line/Point, and Curve/Point branches of
  `src/base/line.js -> createTangent` plus its registered `polar` alias are
  translated; `createPolarLine` remains Circle/Point-only. Circle/Point
  preserves the exact quadratic-form coefficient closure, hidden constrained
  Line endpoints, Point-owned removal edge, degenerate arithmetic, and nested
  helper identity. Line/Point preserves source-endpoint reuse,
  unconstrained/draggable state, supplied parent metadata, endpoint-owned
  dependency/removal, and ignored nested helper identity. Curve/Point
  preserves FunctionGraph X-parameter derivatives, true parametric nearest
  projection, data-Plot nearest-segment selection, JessieCode string-term
  FunctionGraph classification, hidden helpers, and Point-only ownership.
  Native JessieCode, construction-document, core lifecycle, official fixture,
  and focused static plus parent-drag Desktop/Compact parity gates pass.
  Ellipse/Hyperbola/Parabola Tangent/Polar and PolarLine Conic forms, Turtle
  and one-parent Glider branches, an independent production case, and Stable
  qualification remain pending.
- TangentTo: the Circle branch of
  `src/base/line.js -> createTangentTo` now composes the source Point's polar,
  one polar/Circle Intersection selected by upstream numeric truthiness, and
  the Tangent at that contact Point. It preserves the exposed polar/contact
  identities, nested Line/Point attributes, dependencies, removal lifecycle,
  non-real and degenerate arithmetic, and three-object scene/resource
  accounting. Native JessieCode, construction-document, Core lifecycle,
  official fixture, and focused static plus source-Point-drag
  Desktop/Compact parity gates pass. Ellipse, Hyperbola, and Parabola
  construction are translated, but their TangentTo Conic branches, an
  independent production case, and Stable qualification remain pending.
- Ellipse: `src/element/conic.js -> createEllipse` now covers
  Point/reference/coordinate foci with either a Point on the Ellipse or a
  numeric/function-valued major axis, optional numeric parameter domains,
  constrained center creation, `foci`/`center`/`midpoint`/`majorAxis`
  metadata, quadratic-form updates, JavaScript `Double` behavior for
  degenerate axes, parent/helper ownership, and removal lifecycle. Bounded
  JSON/JessieCode entry points, native session updates, duplicate-ID
  rejection, resource checks, and atomic helper rollback are covered. Core,
  creator, document/session, method-map, official fixture, and focused static
  plus point-parent-drag Desktop/Compact parity gates pass. Adaptive plotting,
  an independent production case, and Stable qualification remain pending.
  Tangent, Polar, PolarLine, PolePoint, TangentTo, Normal, Intersection, and
  OtherIntersection Conic forms remain explicitly unsupported until their
  dedicated source-mapped numerical, lifecycle, resource, and parity evidence
  is complete.
- Hyperbola: `src/element/conic.js -> createHyperbola` now covers
  Point/reference/coordinate foci with either a Point on the Hyperbola or a
  numeric/function-valued major axis, optional numeric parameter domains and
  the official `±1.0001π` defaults, constrained center creation,
  `foci`/`center`/`midpoint`/`majorAxis` metadata, quadratic-form updates,
  JavaScript `Double` behavior for degenerate axes, parent/helper ownership,
  and removal lifecycle. Bounded JSON/JessieCode entry points, native session
  updates, duplicate-ID rejection, resource checks, and atomic helper
  rollback are covered. Core, creator, document/session, method-map, official
  fixture, and focused static plus point-parent-drag Desktop/Compact parity
  gates pass. Adaptive plotting, an independent production case, and Stable
  qualification remain pending. Tangent, Polar, PolarLine, PolePoint,
  TangentTo, Normal, Intersection, and OtherIntersection Conic forms remain
  explicitly unsupported until their dedicated source-mapped numerical,
  lifecycle, resource, and parity evidence is complete.
- Parabola: `src/element/conic.js -> createParabola` now covers a
  Point/reference/function-returning-Point or coordinate focus and either a
  registered Line or an implicit two-Point directrix. The translation
  preserves optional numeric parameter domains and official `0..2π` defaults,
  constrained focus-projection `center`/`midpoint` metadata, directrix/focus
  dependencies, quadratic-form updates, degenerate and ideal-directrix
  arithmetic, helper ownership, and removal lifecycle. Bounded JSON/JessieCode
  entry points, native session updates, duplicate-ID rejection, resource
  checks, and atomic helper rollback are covered. Core, creator,
  document/session, method-map, official fixture, and focused static plus
  focus-drag Desktop/Compact parity gates pass. Adaptive plotting, richer
  implicit-Line attributes, an independent production case, and Stable
  qualification remain pending. Tangent, Polar, PolarLine, PolePoint,
  TangentTo, Normal, Intersection, and OtherIntersection Conic forms remain
  explicitly unsupported until their dedicated source-mapped numerical,
  lifecycle, resource, and parity evidence is complete.
- Derivative: `src/base/curve.js -> createDerivative` now creates a
  `curveType=parameter` Curve from `X(t)` and `Numerics.D(Y)(t) /
  Numerics.D(X)(t)`, with the source domain values captured at creation.
  FunctionGraph, parametric, and linearly interpolated data-Plot
  parents are covered, including the viewport-padded data domain, infinite
  vertical slopes, regular Board recomputation, metadata-only parent
  registration, source-removal survival, bounded JSON/JessieCode entry points,
  and structured atomic failures. Core, creator, document/session, official
  fixture, and focused static plus coefficient-Point-drag Desktop/Compact
  parity gates pass. Adaptive plotting, transformed Curves, an independent
  production case, and Stable qualification remain pending.
- Normal: the Line/Point, Circle/Point, and Curve/Point branches of
  `src/base/line.js -> createNormal` now accept registered parents in either
  order. The Line branch preserves its ideal helper plus `point`, `subs`, and
  duplicated `inherits` metadata; the Circle branch reuses the midpoint and
  supplied Point; FunctionGraph, true parametric Curve, and degree-one data
  Plot branches preserve derivative or nearest-projection coefficients and
  hidden constrained endpoints. Explicit-parent dependencies, removal
  behavior, nested helper identities, bounded JSON/JessieCode entry points,
  and structured atomic rollback are covered. Core, creator, document/session,
  official fixture, and focused static plus parent-drag Desktop/Compact parity
  gates pass. Coordinate-array Points are rejected instead of reproducing the
  upstream `1.13.3` stack overflow. Glider, Turtle, transformed-Curve,
  degree-three Plot/Bezier forms, translated Conic interoperation, an independent
  production case, and Stable qualification remain pending.
- Spline/CardinalSpline: `src/base/curve.js -> createSpline` and
  `createCardinalSpline` now cover natural-cubic interpolation from Points,
  coordinate pairs, parallel X/Y arrays, and coordinate functions, plus
  cardinal interpolation from existing or generated Points with dynamic
  tension and uniform, centripetal, or chordal parameterization. Sorted-knot
  updates, metadata-only Spline parents, CardinalSpline generated-Point
  ownership, `createPoints`/`isArrayOfCoordinates`, bounded JSON/JessieCode
  paths, structured failures, and atomic rollback are covered. Core, creator,
  document/session, official fixture, and focused static plus tension-drag
  Desktop/Compact parity gates pass. Adaptive plotting, ordinary Curve fill,
  an independent production case, and Stable qualification remain pending.
- RiemannSum: `src/base/curve.js -> createRiemannsum` now composes the
  translated `Numerics.riemann` geometry for one function or an ordered
  lower/upper pair. Numeric, string, and function-valued rectangle counts,
  string/function approximation types, dynamic interval bounds, `Value()`,
  metadata-free parents, default/custom fill, regular recomputation, and
  dependency removal are covered. Static and dynamic output growth is bounded;
  evaluation failures are structured and atomic instead of retaining the
  upstream partial Curve. Core, creator, method-map, document/session,
  official fixture, and focused static plus bar-count-drag Desktop/Compact
  parity gates pass. An independent production case and Stable qualification
  remain pending.
- BoxPlot: `src/base/curve.js -> createBoxPlot` now covers dynamic five-number
  terms, optional outlier arrays, axis and width terms, vertical/horizontal
  direction, `smallWidth`, every documented outlier face alias, unknown-face
  circle fallback, and official blue fill defaults. Compose resolves outlier
  sizes from the actual CSS-pixel viewport scale. Exact point-growth bounds,
  structured atomic failures, dependency/removal lifecycle, JSON/JessieCode
  entry points, the official fixture, and focused static plus driver-drag
  Desktop/Compact parity gates pass. Function-valued visual attributes, an
  independent production case, and Stable qualification remain pending.
- Comb: `src/element/comb.js -> createComb` now covers Point/reference and
  coordinate-array endpoints, exact upstream clipping and `NaN` tooth breaks,
  numeric/function-valued frequency, width, angle, and reverse attributes,
  hidden endpoint identity/fixed defaults and removal survival, bounded
  static/dynamic output, and structured atomic rollback. Core, creator,
  document/session, official fixture, and focused static plus driver-drag
  Desktop/Compact parity gates pass. Function-returning Point/coordinate
  parents, an independent production case, and Stable qualification remain
  pending.
- Inequality: `src/element/composition.js -> createInequality` now covers Line
  and FunctionGraph sources, the exact expanded half-plane and segmented
  closure algorithms, Board bounds, non-finite FunctionGraph runs, dynamic
  `inverse`, official defaults, metadata-only parents and source-removal
  survival, bounded output, and structured failures. Core, creator,
  document/session, official fixture, focused static parity, Line-parent drag,
  and FunctionGraph-driver/inverse drag pass at Desktop and Compact sizes.
  Additional source kinds, an independent production case, and Stable
  qualification remain pending.
- VectorField: `src/element/vectorfield.js -> createVectorField` now covers
  two component functions/expressions or one array-returning function/
  expression, dynamic three-term meshes, scale, and nested arrow enable/size/
  angle attributes. It preserves inclusive fractional/zero/negative step
  loops, `NaN` path breaks, nonzero-vector arrows, metadata-only function
  dependencies, source-removal survival, viewport CSS-pixel arrow sizing,
  bounded output, and structured atomic failure. Core, creator,
  document/session, official fixture, and focused static plus driver-drag
  Desktop/Compact parity gates pass. An independent production case and Stable
  qualification remain pending.
- SlopeField: `src/element/vectorfield.js -> createSlopeField` now wraps the
  translated VectorField path for one scalar function/expression. It preserves
  the exact unit-direction normalization, non-finite arithmetic, disabled
  arrowhead default, dynamic mesh/scale/arrow evaluation, metadata-only
  closure dependencies, bounded output, and structured atomic failure. Core,
  creator, document/session, official fixture, and focused static plus
  driver-drag Desktop/Compact parity gates pass. Runtime `setF`, an independent
  production case, and Stable qualification remain pending.
- Circumcircle creators: `createCircumcenter`, the
  `circumcirclemidpoint` alias, and `createCircumcircle` now cover
  Point/reference/coordinate parents, public and hidden center metadata,
  `subs`/`inherits`, dependency updates, helper ownership/removal, degenerate
  arithmetic, atomic rollback, native JessieCode and construction-document
  entry points, and same-source static/interaction parity. They remain outside
  the Stable corpus until an independent production case and the full
  qualification gates are added.
- Point reflections: the Point branches of `createReflection` and
  `createMirrorElement`, plus `createMirrorPoint`, now cover dynamic Line
  reflection, Point-centered π rotation, parent metadata, asymmetric
  reflector-owned dependency/removal, frozen regular-update behavior,
  structured failures, native JessieCode and construction-document entry
  points, and same-source static/interaction parity. The remaining
  Line/Curve/Polygon/Circle reflection branches and Stable qualification are
  still pending.
- Parallel constructions: `createParallelPoint` and `createParallel` now cover
  three-Point and Line/Point forms, coordinate helper ownership, finite and
  ideal endpoints, dependency updates, removal behavior, native JessieCode and
  construction-document entry points, and same-source static/interaction
  parity. They remain outside the Stable corpus until an independent
  production case and the full qualification gates are added.
- Line arrows: `createArrow` and `createArrowParallel` now preserve vector
  identity, forced visible line-end flags, upstream defaults, explicit
  disabled heads, and static Boolean/object head attributes. Compose Canvas
  translates head types `1..7`, per-type endpoint shortening, cubic shapes,
  filled/open behavior, ideal-endpoint clipping, and type 7's fixed effective
  size. Core geometry tests plus focused same-source static and parent-drag
  parity pass at both viewports. Dynamic/highlight attributes, Curve/Arc/
  Sector arrows, non-Canvas renderers, independent production cases, and
  Stable qualification remain pending.
- Composition and two-Line angle bisectors: the reusable non-Board
  `JXG.Composition` subset and
  `createAngularBisectorsOfTwoLines` now cover role/ID/name membership,
  ordered output access, lifecycle forwarding, two registered output Lines,
  four hidden constrained helpers, normalized coefficient geometry, nested
  attributes, dependency updates, removal, degenerate arithmetic, atomic
  rollback, native JessieCode access, and same-source static/interaction
  parity. Generic group/filter selection, composition-wide mutable visual
  properties, a JSON compound-result contract, independent production cases,
  and Stable qualification remain pending.
- Triangle centers: `createBisector`, `createIncenter`, and `createIncircle`
  now cover Point/reference/coordinate parents, exact constrained geometry,
  dependency updates, hidden helper ownership and removal, degenerate
  arithmetic, atomic rollback, native JessieCode and construction-document
  entry points, and same-source static/interaction parity. They remain outside
  the Stable corpus until independent production cases and the full
  qualification gates are added.
- Intersection Points: `createIntersectionPoint` and
  `createOtherIntersectionPoint` now cover Line/Segment/Circle, discrete and
  continuous Curve, Arc, Sector, Polygon/Line, and Polygon/path pairs. The
  translation preserves indexed segment/Bézier traversal, continuous
  Newton/root search, Segment/ray and Arc clipping, ordered
  `Clip.findIntersections` crossings and overlap endpoints, the upstream
  first-parent Arc asymmetry, ideal/non-real results, excluded-Point
  selection, dependency updates, native JessieCode and construction-document
  entry points, and same-source static/interaction parity. Translated Conic
  dispatch, dynamic visual-property forms, independent production cases, and
  full qualification remain pending.
- Curve Boolean clipping: `createCurveIntersection`, `createCurveUnion`, and
  `createCurveDifference` now cover Circle/Curve/Arc/Sector/Polygon paths,
  degenerate intersection classification and chains, entry/exit marking,
  empty/containment cases, multi-component output, path tracing, regular Board
  updates, structured topology/traversal failures, native JessieCode and
  construction-document entry points, and same-source static/parent-drag
  parity. Raw coordinate-array paths, independent production cases, and
  Stable qualification remain pending.
- Arc compositions: `createSemicircle`, `createCircumcircleArc`,
  `createMinorArc`, and `createMajorArc` now cover Point/reference/coordinate
  parents, hidden Midpoint/Circumcenter helpers, dependency updates, helper
  ownership/removal, degenerate arithmetic, atomic rollback, native JessieCode
  and construction-document entry points, and same-source static/interaction
  parity. They remain outside the Stable corpus until independent production
  cases and the full qualification gates are added.
- Sector compositions: `createCircumcircleSector`, `createMinorSector`,
  `createMajorSector`, `createNonreflexAngle`, and `createReflexAngle` now
  cover Point/reference/coordinate parents, hidden Circumcenter and
  direction-Point handling, forced selection and radian-default, unit-aware
  Angle `Value`, dependency updates, helper ownership/removal, degenerate
  arithmetic, atomic rollback, native JessieCode and construction-document
  entry points, and same-source static/interaction parity. They remain outside
  the Stable corpus until independent production cases and the full
  qualification gates are added.
- Canvas layer ordering: supported render items now follow JSXGraph
  `Options.layer` defaults and explicit non-negative integer overrides with
  creation order as the tie-breaker. Grid, Axis, top-level elements, and
  Polygon fill/borders/implicit vertices participate independently. Nested
  Polygon styling, dynamic layer mutation, and the remaining renderer objects
  and Board layer options are still pending.
- Canvas dash rendering: supported Point, Line, Circle, Curve, and
  Polygon-border strokes now use all seven
  `src/renderer/abstract.js -> dashArray` patterns. `dashScale` multiplies
  intervals by `strokeWidth / 2` as upstream does; the Compose backend maps
  resolved CSS-pixel intervals to dp and clamps only the zero-length dotted
  interval to Skia's `0.001` minimum. Dynamic style mutation and unsupported
  renderer objects remain pending.

## Implementation Order

### 1. Foundation

- constants, coordinate conversion and event dispatch
- `JXG.Math`, probability functions and complex arithmetic
- shared result/error model
- upstream version and source mapping

Exit gate: common tests pass on JVM, iOS simulator and Wasm; all target
artifacts assemble.

### 2. Geometry And Numerics

- statistics, numerical integration/root finding and interpolation
- core geometry intersections, projections and distances
- curves, clipping, implicit plotting and quadtrees

Exit gate: deterministic fixtures are compared with values produced by the
official JSXGraph baseline, including degenerate and non-finite inputs.

### 3. Parser And Expression Runtime

- JessieCode lexer, parser, AST and evaluator
- JSXGraph construction input parser
- structured `GMResult` errors with source locations
- resource limits for untrusted generated input

Exit gate: official syntax corpus passes without uncaught exceptions, hangs or
unbounded allocations.

### 4. Board And Element Model

- board lifecycle, object registry and dependency graph
- geometry element base classes and coordinate elements
- point, line, circle, curve, polygon, text, axes, ticks and grids
- remaining 2D element factories

Exit gate: every supported upstream 2D factory has construction, update and
serialization tests.

### 5. Rendering And Interaction

- platform-independent render model
- Compose Canvas renderer
- text measurement and image/resource adapters
- hit testing, pointer capture, drag, pan, zoom and keyboard handling

Exit gate: interaction traces and render geometry match the official baseline
within documented platform tolerances.

### 6. Production Qualification

- official example and generated-input compatibility corpus
- golden geometry and screenshot tests
- malformed-input, fuzz and resource-limit tests
- multi-board memory, scroll, lifecycle and long-running soak tests
- API documentation, migration notes and Maven publication metadata

Exit gate: no known critical correctness, crash, leak or licensing issue;
supported and unsupported behavior is published explicitly.

Status: complete for the `0.1.0` documented support scope. The independent
30-case production corpus, 55-point capability coverage, deterministic replay,
512 generated stress inputs, JVM soak, Desktop/Compact visual parity, runtime
load matrix, publication metadata, and public-source safety gates pass.

## Continuous Loop

For each upstream slice:

1. Record the source file and symbols.
2. Capture official reference outputs for representative and edge cases.
3. Translate the implementation without unrelated redesign.
4. Run focused tests, then all common tests and target assembly.
5. Audit the diff for private/internal content and license attribution.
6. Commit and push a reviewable batch.
