# Testing And Visual Parity

## Current Stack

- Common Kotlin tests use `kotlin.test`.
- Core tests run on JVM, iOS Simulator, and Wasm browser targets.
- `jsxgraph-compose` is a pure Compose Multiplatform renderer module.
- `jsxgraph-debug-ui` is a separate comparison dependency.
- The Android sample is a thin launcher for the debug UI.
- The Web sample is a thin Kotlin/Wasm launcher for the debug UI.
- Official JSXGraph `1.13.3` runs from local debug-only assets in an Android
  WebView or same-origin Web iframe.
- The native axis renderer uses bundled Arimo for the upstream Arial-compatible
  default instead of the device theme font.
- Construction-document parity cases use the production
  `JsxGraphEngine.createSession -> Board -> JsxGraphScene` path. The official
  adapter converts each object in the same ordered source to
  `board.create(type, parents, attributes)`.
- A strict debug-only JessieCode envelope sends the same bounded source and
  Board options to native `JsxGraphJessieCodeSession.execute` and official
  `board.jc.parse`.
- Production artifacts do not use WebView or a JavaScript engine.

The repository does not use dependency injection, mocking, a database, or
navigation. Adding frameworks for those concerns would not improve the current
test surface.

## Commands

Use JDK 17 or newer.

```bash
node tools/stability/generate-production-corpus.mjs
./gradlew :jsxgraph-core:allTests
./gradlew :jsxgraph-compose:allTests :jsxgraph-compose:assemble
./gradlew :jsxgraph-debug-ui:allTests :jsxgraph-debug-ui:assemble
./gradlew verifyPublicationCoordinates
./gradlew \
  :jsxgraph-core:publishAllPublicationsToBuildRepository \
  :jsxgraph-compose:publishAllPublicationsToBuildRepository
bash tools/verify-publication-archives.sh
./gradlew \
  :sample:androidApp:assembleDebug \
  :sample:androidApp:assembleRelease \
  :sample:desktopApp:createDistributable \
  :sample:webApp:wasmJsBrowserDistribution
```

Capture the JSXGraph `1.13.3` dynamic 2D transformation lifecycle used by the
core assertions:

```bash
node tools/upstream-fixtures/transformation-lifecycle.mjs
```

The fixture covers number, JessieCode, and function-valued parameters; dynamic
matrices; Point/Line-backed rotation and reflection; transformed Points;
`bindTo`; transformed drag preimages; independent static `meltTo` clones; and
first-match transform removal. It also records the upstream nonnumeric
classification of array-centered rotations and array-backed matrices. Native
JessieCode and construction documents expose `transform` and transformed Point
creation from one transform or a chain. The focused `transformed_points`
same-source fixture remains outside the 30-case Stable evidence count pending
independent production qualification.

Capture every JSXGraph `1.13.3` 4x4 transformation form:

```bash
node tools/upstream-fixtures/transformation-3d.mjs
```

The fixture covers translate, three- and four-parameter scale, axis and
arbitrary-axis rotations, array- and Point-centered rotations, affine and
matrix forms, scalar/vector/matrix dynamic reevaluation, homogeneous and
zero-length normals, permissive extra rotation parameters, and the observed
six-evaluator/16-read `generic` defect. Core tests compare those matrices and
transformed homogeneous coordinates directly.

Capture the JSXGraph `1.13.3` View3D/Point3D lifecycle built on that kernel:

```bash
node tools/upstream-fixtures/view3d-point3d.mjs
```

The fixture covers parallel and central camera matrices, forward and inverse
projection helpers, cube clipping, numeric, homogeneous, array-function, and
scalar-function Point3D coordinates, transformed Point3D binding and
`applyOnce`, Line3D, Plane3D, direct and Plane-owned Mesh3D, Axis3D, Ticks3D,
explicit Axes3D, automatic `view.defaultAxes`, direct and transformed
Polyhedron3D/Face3D, 2D proxy movement, registration, removal, and malformed
parent forms. Its automatic-axes
snapshot records the official `none`, `border`, and `center` role lists,
non-null member counts, View registration, default tick-label counts, and the
hidden center `O` Intersection's coordinates and dependency metadata. Its Polyhedron3D
snapshot records three faces over four vertex keys, Point-backed, function,
and homogeneous vertices, a four-point closed triangular proxy with green
`0.5` fill and `4px` stroke, an unclosed two-point proxy, transformed
coordinates `[1, 3, -1, 6]`, and dynamic base/transformed updates.

Capture the official Polygon3D open-vertex/closed-proxy structure, dynamic
coordinates, nested styles, centroid depth, and transformed-creator failure:

```bash
node tools/upstream-fixtures/polygon3d.mjs
```

The direct fixture records object type `46`, four open 3D vertices, five
closed proxy vertices, four proxy borders, dynamic x-coordinate and depth
updates, and nested fill/border styles. JSXGraph `1.13.3` throws while
registering the first transformed Point3D; the exact failure and the Kotlin
safety adaptation are recorded in `translation-deviations.md`.

Capture the official Curve3D component/vector/discrete/transformed forms,
sampling, projected proxy arrays, and dynamic function update:

```bash
node tools/upstream-fixtures/curve3d.mjs
```

The fixture records object type `36`, the exact inclusive
`numberPointsHigh` loop, homogeneous 3D samples, matrix transposition,
ordinary Curve proxy coordinates, transformed-parent metadata, and live
function reevaluation.

Capture the official Circle3D center/normal/radius forms, frame updates,
ordinary Curve proxy, negative-radius normalization, and hidden invalid
geometry:

```bash
node tools/upstream-fixtures/circle3d.mjs
```

The fixture records object type `41`, numeric and function-valued normal and
radius parents, absolute-value radius behavior, dynamic frame
recalculation, the owned Curve3D parent, and `NaN` proxy coordinates after an
invalid dynamic radius.

Capture the official VectorField3D component/array-function forms, dynamic
scale and arrow settings, inclusive X/Y/Z meshes, zero-field suppression, and
default style:

```bash
node tools/upstream-fixtures/vectorfield3d.mjs
```

The fixture records Curve3D object type `36`, class `8`, four vectors as 28
points with arrows, 12 points without arrows, an empty zero field, default
`5px`/`π/8` arrowheads, `1px` stroke, and layer `12`.

Capture the official IntersectionCircle3D Plane/Sphere and Sphere/Sphere
forms, dynamic recomputation, hidden owned center, and parent dependencies:

```bash
node tools/upstream-fixtures/intersectioncircle3d.mjs
```

The fixture records object type `42`, both supported dispatch orders,
canonical parent metadata, owned-center visibility, and proxy hiding after
two dynamic spheres become disjoint.

Capture the official IntersectionLine3D Plane/Plane form, clipped endpoints,
hidden owned points, canonical parents, and creation-time snapshot behavior:

```bash
node tools/upstream-fixtures/intersectionline3d.mjs
```

The fixture records object type `39`, exact View3D bounding-box endpoints,
hidden helper visibility, parent dependencies, and the JSXGraph `1.13.3`
behavior where later Plane updates do not recompute the created endpoints.

Capture the official Sphere3D Point/Point and Point/radius forms, dynamic
absolute radius, parallel Circle proxy, central Ellipse proxy, auxiliary
Points, analytic coordinate projection, and default radial-gradient
parameters:

```bash
node tools/upstream-fixtures/sphere3d.mjs
```

The fixture records object type `40`, `pointRadius` and `twoPoints` methods,
live radius updates, Circle and Curve/Ellipse proxy types, the complete
default radial-gradient values, the three central
projection auxiliary Points, parent metadata, and the projected coordinate
and parameter values. It also switches one View3D from parallel to central
and back through `setAttribute`, recording proxy and auxiliary IDs, direct
children, inherited proxies, Board object counts, and old-object removal.
The browser produces the complete JSON evidence before the local sandbox
rejects Chrome Crashpad shutdown access.

Capture the official Surface3D vector/component functions, FunctionGraph3D
wrapper, dynamic ranges, wireframe separators, and rectangle topology:

```bash
node tools/upstream-fixtures/surface3d.mjs
```

The fixture records object type `37`, the `parametricsurface3d` and
`functiongraph3d` element types, dynamic two-parameter reevaluation, exact
row/column Curve proxy arrays with NaN separators, and a `3 x 3` rectangle
grid with four faces.

Capture the official COBYLA optimizer and Curve3D/Surface3D parametric
projection results:

```bash
node tools/upstream-fixtures/nlp-parametric.mjs
```

The fixture records normal status `0`, the exact unconstrained result
`[0.9999998880066555, -1.999999990526853]` after 66 evaluations, and the
exact constrained result `[0.49999991923923603, 0.5000000807607639]` after
59 evaluations. It also records the Curve3D projection parameter
`1.4666860883614186` and point
`[1, 0.10392226619847204, 0.9945854224691683, 0.4668606818536723]`, plus the
Surface3D parameters `[0.7333332582827766, -0.4666667008424886]` and point
`[1, 0.7333332582827766, -0.4666667008424886, 0.266666557440288]`. The
browser produces the complete JSON evidence before the local sandbox rejects
Chrome Crashpad shutdown access.

Capture the official Plane3D rectangle/triangle tiling, color-array, shader,
colormap, and Axes3D rear-plane defaults:

```bash
node tools/upstream-fixtures/plane3d-surfaces.mjs
```

These fixtures are the official source of truth for this bounded Kotlin
translation. Global View3D `depthOrder`/layer configuration, camera controls,
gliders, animations, Stable qualification, and the complete 3D API remain
outside this slice.

Capture the official direct-function Point lifecycle and the observed
Midpoint function-parent rejection:

```bash
node tools/upstream-fixtures/point-function-coordinates.mjs
```

Capture the official ParallelPoint/Parallel coordinates, parent ordering,
dependency graph, helper ownership/removal, failure, and degenerate behavior:

```bash
node tools/upstream-fixtures/parallel-constructions.mjs
```

Capture the official Arrow/ArrowParallel defaults, static head types,
dependency updates, helper ownership/removal, explicit disabled heads, and
unsupported/non-finite parent behavior:

```bash
node tools/upstream-fixtures/line-arrows.mjs
```

Capture the official Bisector/Incenter/Incircle coordinates, parent ordering,
dependency graph, hidden-helper ownership/removal, failure, and degenerate
behavior:

```bash
node tools/upstream-fixtures/triangle-centers.mjs
```

Capture the official Intersection/OtherIntersection branch ordering,
Line/Segment/Circle coordinates, finite clipping, dynamic indices, ideal and
non-real results, dependency/removal metadata, and failure behavior:

```bash
node tools/upstream-fixtures/intersection-points.mjs
```

Capture the official Curve/Arc/Sector/Polygon intersection dispatch, arbitrary
indices, first-parent Arc clipping asymmetry, and OtherIntersection selection:

```bash
node tools/upstream-fixtures/intersection-curves-paths.mjs
```

Capture the official CurveIntersection, CurveUnion, and CurveDifference
coordinates, empty/containment and degenerate cases, multi-component output,
creator lifecycle, parent updates, and invalid arity:

```bash
node tools/upstream-fixtures/curve-boolean-clipping.mjs
```

Capture the official StepFunction expansion, missing-Y behavior, retained
source-array mutation, zero-arity function parents, and invalid arity:

```bash
node tools/upstream-fixtures/stepfunction.mjs
```

Capture the official PolygonalChain border order, open vertex list,
measurement behavior, Point updates, helper ownership/removal, `withLines`,
degenerate arity, and invalid-parent behavior:

```bash
node tools/upstream-fixtures/polygonal-chain.mjs
```

Capture the official Parallelogram vertex and border order, exposed
ParallelPoint, forced draggable/fixed state, parent updates, helper
ownership/removal, coordinate-parent lifecycle, and failure leakage:

```bash
node tools/upstream-fixtures/parallelogram.mjs
```

Capture both official RegularPolygon parent forms, chained transforms,
fractional numeric counts, generated CAS Point metadata, `vertices.ids`,
updates, ownership/removal, invalid parents, and duplicate-ID behavior:

```bash
node tools/upstream-fixtures/regular-polygon.mjs
```

Capture the official Circumcenter, CircumcircleMidpoint alias, and
Circumcircle metadata, dependency graph, helper ownership/removal, failure,
degenerate behavior, and frozen-center update semantics:

```bash
node tools/upstream-fixtures/circumcircle-creators.mjs
```

Capture the official Point branches of Reflection and MirrorElement plus
MirrorPoint coordinates, transformation metadata, asymmetric dependency and
removal behavior, frozen updates, rejected coordinate parents, and duplicate
ID behavior:

```bash
node tools/upstream-fixtures/point-reflections.mjs
```

Capture the official two-Line angle bisectors and generic Composition
membership, metadata, forwarded visual mutation, update/removal lifecycle,
nested attributes, failure, and degenerate behavior:

```bash
node tools/upstream-fixtures/bisector-lines.mjs
```

Capture the official Semicircle, CircumcircleArc, MinorArc, and MajorArc
construction, helper ownership/removal, dependency update, failure, and
degenerate behavior:

```bash
node tools/upstream-fixtures/arc-compositions.mjs
```

Capture the official CircumcircleSector, MinorSector, MajorSector,
NonreflexAngle, and ReflexAngle construction, helper ownership/removal,
dependency update, forced selection/`Value` behavior, failure, and degenerate
behavior:

```bash
node tools/upstream-fixtures/sector-compositions.mjs
```

Capture the official Circle/Point Tangent, Polar alias, and PolarLine wrapper
coefficients, aliases, parent order, child/removal metadata, update behavior,
degenerate geometry, and failure behavior:

```bash
node tools/upstream-fixtures/tangent-polar-circle.mjs
```

Capture the official Line/Point Tangent and Polar endpoint sharing, parent and
dependency metadata, removal behavior, degenerate geometry, ignored nested
Point identity, and cross-Board behavior:

```bash
node tools/upstream-fixtures/tangent-line.mjs
```

Capture the official Curve/Point Tangent and Polar continuous/discrete
projection, derivative coefficients, string-term Curve classification,
helper ownership/removal, degenerate geometry, failure rollback, and
cross-Board behavior:

```bash
node tools/upstream-fixtures/tangent-curve.mjs
```

Capture the official Circle-only TangentTo polar/intersection/tangent
composition, numeric branch truthiness, nested attributes, update/removal
lifecycle, degenerate geometry, Conic boundary, duplicate IDs, and staged
failure behavior:

```bash
node tools/upstream-fixtures/tangent-to.mjs
```

Capture the official Ellipse Point/numeric/function major-axis forms,
parameter domains, center/foci/quadratic-form metadata, updates,
ownership/removal, duplicate IDs, partial-construction leakage, degenerate
arithmetic, and Conic-interoperation boundaries:

```bash
node tools/upstream-fixtures/ellipse.mjs
```

Capture the official Hyperbola Point/numeric/function major-axis forms,
default and explicit parameter domains, center/foci/quadratic-form metadata,
updates, ownership/removal, duplicate IDs, invalid parents, and degenerate
arithmetic:

```bash
node tools/upstream-fixtures/hyperbola.mjs
```

Capture the official Parabola focus/Line and coordinate-parent forms,
parameter domains, center/quadratic-form metadata, updates, ownership/removal,
duplicate IDs, invalid parents, and degenerate or ideal directrix arithmetic:

```bash
node tools/upstream-fixtures/parabola.mjs
```

Capture the official Derivative function/parametric/data-Plot sampling,
central-difference values, updates, parent metadata, source-removal survival,
and invalid-parent behavior:

```bash
node tools/upstream-fixtures/derivative.mjs
```

Capture the official Line/Circle/Curve Normal coefficients, ideal and hidden
helper identities, parent order, dependency/removal lifecycle, updates,
coordinate-array stack overflow, and invalid-parent behavior:

```bash
node tools/upstream-fixtures/normal.mjs
```

Capture the official Spline/CardinalSpline parent normalization, interpolation,
dynamic updates and tension, generated-Point ownership, removal lifecycle,
type fallback, and invalid-parent behavior:

```bash
node tools/upstream-fixtures/spline-creators.mjs
```

Capture the official RiemannSum approximation modes, single/between-function
geometry, `Value()`, default fill, dynamic rectangle count/type/bounds,
dependency removal, and invalid-parent side effects:

```bash
node tools/upstream-fixtures/riemannsum.mjs
```

Capture the official BoxPlot vertical/horizontal paths, dynamic
quantile/axis/width terms, `smallWidth`, every outlier face and CSS-pixel size,
dependency removal, and malformed-parent side effects:

```bash
node tools/upstream-fixtures/boxplot.mjs
```

Capture the official Comb defaults, exact tooth arrays, dynamic attributes,
coordinate helper lifecycle, zero-length behavior, and invalid-parent side
effects:

```bash
node tools/upstream-fixtures/comb.mjs
```

Capture the official Inequality Line/FunctionGraph geometry, dynamic
`inverse`, segmented non-finite runs, defaults, parent metadata, source
removal, ignored extra parents, and invalid-source behavior:

```bash
node tools/upstream-fixtures/inequality.mjs
```

Capture the official VectorField function forms, dynamic mesh/scale/arrow
attributes, fractional/zero/negative steps, path arrays, parent relations,
removal behavior, and invalid parents:

```bash
node tools/upstream-fixtures/vectorfield.mjs
```

Capture the official SlopeField scalar normalization, disabled-arrow default,
dynamic mesh/scale/arrow attributes, `setF` return value, non-finite
arithmetic, and invalid parents:

```bash
node tools/upstream-fixtures/slopefield.mjs
```

Capture and audit the current Web parity corpus:

```bash
npm ci --prefix tools/visual-parity
./gradlew :sample:webApp:wasmJsBrowserDistribution
python3 -m http.server 8093 \
  --directory sample/webApp/build/dist/wasmJs/productionExecutable

# Run these in another shell.
BASE_URL=http://127.0.0.1:8093/ \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/web-parity/current \
  npm --prefix tools/visual-parity run audit
```

Replay the production Point drag against both renderers:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/web-interaction/current \
  PARITY_CASE_IDS=baseline_geometry \
  INTERACTION_TRACE=baseline_point_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/web-interaction/current \
  PARITY_CASE_IDS=baseline_geometry \
  npm --prefix tools/visual-parity run audit
```

Replay the focused function-valued Transformation driver drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/transformed-points-interaction/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
PARITY_CASE_IDS=transformed_points \
INTERACTION_TRACE=transformed_point_driver \
npm --prefix tools/visual-parity run capture

INPUT_DIR=captures/local/transformed-points-interaction/desktop \
PARITY_CASE_IDS=transformed_points \
npm --prefix tools/visual-parity run audit
```

Replay the focused Point3D proxy drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/point3d-projection-drag/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
PARITY_CASE_IDS=point3d_projection \
INTERACTION_TRACE=point3d_proxy_drag \
npm --prefix tools/visual-parity run capture

INPUT_DIR=captures/local/point3d-projection-drag/desktop \
PARITY_CASE_IDS=point3d_projection \
npm --prefix tools/visual-parity run audit
```

Replay the focused function-coordinate Point driver drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/function-coordinate-interaction/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
PARITY_CASE_IDS=function_coordinate_points \
INTERACTION_TRACE=function_coordinate_driver \
npm --prefix tools/visual-parity run capture

INPUT_DIR=captures/local/function-coordinate-interaction/desktop \
PARITY_CASE_IDS=function_coordinate_points \
npm --prefix tools/visual-parity run audit
```

Replay the focused Parallel parent drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/parallel-constructions-interaction/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
PARITY_CASE_IDS=parallel_constructions \
INTERACTION_TRACE=parallel_parent_drag \
npm --prefix tools/visual-parity run capture

INPUT_DIR=captures/local/parallel-constructions-interaction/desktop \
PARITY_CASE_IDS=parallel_constructions \
npm --prefix tools/visual-parity run audit
```

Replay the focused ArrowParallel parent drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/line-arrows-interaction/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
MIN_CAPTURE_BYTES=5000 \
PARITY_CASE_IDS=line_arrows \
INTERACTION_TRACE=line_arrow_parent_drag \
npm --prefix tools/visual-parity run capture

INPUT_DIR=captures/local/line-arrows-interaction/desktop \
MIN_CAPTURE_BYTES=5000 \
MIN_BOARD_SSIM=0.90 \
PARITY_CASE_IDS=line_arrows \
npm --prefix tools/visual-parity run audit
```

Replay the focused triangle-center parent drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/triangle-centers-interaction/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
PARITY_CASE_IDS=triangle_centers \
INTERACTION_TRACE=triangle_center_parent_drag \
npm --prefix tools/visual-parity run capture

INPUT_DIR=captures/local/triangle-centers-interaction/desktop \
PARITY_CASE_IDS=triangle_centers \
npm --prefix tools/visual-parity run audit
```

Replay the focused Intersection Circle-parent drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/intersection-points/interaction-desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
PARITY_CASE_IDS=intersection_points \
INTERACTION_TRACE=intersection_circle_parent_drag \
npm --prefix tools/visual-parity run capture

INPUT_DIR=captures/local/intersection-points/interaction-desktop \
PARITY_CASE_IDS=intersection_points \
npm --prefix tools/visual-parity run audit
```

Replay the focused Polygon path-intersection parent drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/polygon-path-intersections/interaction-desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
MIN_CAPTURE_BYTES=8000 \
PARITY_CASE_IDS=polygon_path_intersections \
INTERACTION_TRACE=polygon_path_parent_drag \
npm --prefix tools/visual-parity run capture

INPUT_DIR=captures/local/polygon-path-intersections/interaction-desktop \
MIN_CAPTURE_BYTES=8000 \
MIN_BOARD_SSIM=0.90 \
PARITY_CASE_IDS=polygon_path_intersections \
npm --prefix tools/visual-parity run audit
```

Replay the focused PolygonalChain final-Point drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/polygonal-chains/interaction-desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=polygonal_chains \
  INTERACTION_TRACE=polygonal_chain_parent_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/polygonal-chains/interaction-desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.90 \
  PARITY_CASE_IDS=polygonal_chains \
  npm --prefix tools/visual-parity run audit
```

Replay the focused Parallelogram parent drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/parallelograms/interaction-desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=parallelograms \
  INTERACTION_TRACE=parallelogram_parent_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/parallelograms/interaction-desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.90 \
  PARITY_CASE_IDS=parallelograms \
  npm --prefix tools/visual-parity run audit
```

Replay the focused RegularPolygon parent drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/regular-polygons/interaction-desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=regular_polygons \
  INTERACTION_TRACE=regular_polygon_parent_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/regular-polygons/interaction-desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.90 \
  PARITY_CASE_IDS=regular_polygons \
  npm --prefix tools/visual-parity run audit
```

Replay the focused RadicalAxis radius-Point drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/radical-axis/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=radical_axis \
  INTERACTION_TRACE=radical_axis_parent_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/radical-axis/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=radical_axis \
  npm --prefix tools/visual-parity run audit
```

Replay the focused PolePoint Circle/Line parent drag sequence:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/pole-point/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=pole_point \
  INTERACTION_TRACE=pole_point_parent_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/pole-point/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=pole_point \
  npm --prefix tools/visual-parity run audit
```

Replay the focused Circle/Point Tangent, Polar, and PolarLine radius-Point
drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/tangent-polar/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=tangent_polar_circle \
  INTERACTION_TRACE=tangent_polar_circle_parent_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/tangent-polar/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=tangent_polar_circle \
  npm --prefix tools/visual-parity run audit
```

Capture the focused Circle-only TangentTo case and replay its source-Point
drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/tangent-to/static/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=tangent_to_circle \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/tangent-to/static/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=tangent_to_circle \
  npm --prefix tools/visual-parity run audit

BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/tangent-to/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=tangent_to_circle \
  INTERACTION_TRACE=tangent_to_point_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/tangent-to/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=tangent_to_circle \
  npm --prefix tools/visual-parity run audit
```

Capture the focused Ellipse case and replay its point-on-Ellipse drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/ellipse/static/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=ellipses \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/ellipse/static/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=ellipses \
  npm --prefix tools/visual-parity run audit

BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/ellipse/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=ellipses \
  INTERACTION_TRACE=ellipse_point_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/ellipse/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=ellipses \
  npm --prefix tools/visual-parity run audit
```

Capture the focused Hyperbola branches and replay their shared
point-on-Hyperbola drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/hyperbola/static/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=hyperbolas \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/hyperbola/static/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=hyperbolas \
  npm --prefix tools/visual-parity run audit

BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/hyperbola/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=hyperbolas \
  INTERACTION_TRACE=hyperbola_point_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/hyperbola/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=hyperbolas \
  npm --prefix tools/visual-parity run audit
```

Capture the focused Parabola branches and replay the registered focus drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/parabola/static/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=parabolas \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/parabola/static/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=parabolas \
  npm --prefix tools/visual-parity run audit

BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/parabola/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=parabolas \
  INTERACTION_TRACE=parabola_focus_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/parabola/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=parabolas \
  npm --prefix tools/visual-parity run audit
```

Replay the focused Line/Point Tangent and Polar source-endpoint drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/tangent-line/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=tangent_line \
  INTERACTION_TRACE=tangent_line_parent_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/tangent-line/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=tangent_line \
  npm --prefix tools/visual-parity run audit
```

Replay the focused Curve/Point Tangent and Polar Point drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/tangent-curve/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=tangent_curve \
  INTERACTION_TRACE=tangent_curve_point_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/tangent-curve/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=tangent_curve \
  npm --prefix tools/visual-parity run audit
```

Replay the focused Derivative coefficient-Point drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/derivative/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=derivative_curve \
  INTERACTION_TRACE=derivative_curve_coefficient_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/derivative/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=derivative_curve \
  npm --prefix tools/visual-parity run audit
```

Replay the focused Normal Line-parent drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/normal/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=normal_constructions \
  INTERACTION_TRACE=normal_constructions_parent_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/normal/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=normal_constructions \
  npm --prefix tools/visual-parity run audit
```

Replay the focused CardinalSpline tension-Point drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/spline-curves/interaction-desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=spline_curves \
  INTERACTION_TRACE=spline_curves_tension_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/spline-curves/interaction-desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=spline_curves \
  npm --prefix tools/visual-parity run audit
```

Replay the focused RiemannSum rectangle-count drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/riemann-sums/interaction-desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=riemann_sums \
  INTERACTION_TRACE=riemann_sums_bar_count_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/riemann-sums/interaction-desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.93 \
  PARITY_CASE_IDS=riemann_sums \
  npm --prefix tools/visual-parity run audit
```

Replay the focused BoxPlot driver drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/box-plots/interaction-desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=box_plots \
  INTERACTION_TRACE=box_plots_driver_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/box-plots/interaction-desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.90 \
  PARITY_CASE_IDS=box_plots \
  npm --prefix tools/visual-parity run audit
```

Replay the focused Comb driver drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/combs/interaction-desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=combs \
  INTERACTION_TRACE=comb_driver_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/combs/interaction-desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.90 \
  PARITY_CASE_IDS=combs \
  npm --prefix tools/visual-parity run audit
```

Replay the focused Inequality Line-parent and FunctionGraph-driver drags:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/inequalities/line-interaction-desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=inequalities \
  INTERACTION_TRACE=inequality_line_parent_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/inequalities/line-interaction-desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.90 \
  PARITY_CASE_IDS=inequalities \
  npm --prefix tools/visual-parity run audit

BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/inequalities/function-interaction-desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=inequalities \
  INTERACTION_TRACE=inequality_function_driver_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/inequalities/function-interaction-desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.90 \
  PARITY_CASE_IDS=inequalities \
  npm --prefix tools/visual-parity run audit
```

Replay the focused VectorField driver drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/vector-fields/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=vector_fields \
  INTERACTION_TRACE=vector_field_driver_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/vector-fields/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.90 \
  PARITY_CASE_IDS=vector_fields \
  npm --prefix tools/visual-parity run audit
```

Replay the focused SlopeField driver drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/slope-fields/interaction/desktop \
  VIEWPORT_WIDTH=1200 \
  VIEWPORT_HEIGHT=900 \
  MIN_CAPTURE_BYTES=5000 \
  PARITY_CASE_IDS=slope_fields \
  INTERACTION_TRACE=slope_field_driver_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/slope-fields/interaction/desktop \
  MIN_CAPTURE_BYTES=5000 \
  MIN_BOARD_SSIM=0.90 \
  PARITY_CASE_IDS=slope_fields \
  npm --prefix tools/visual-parity run audit
```

Replay the focused Curve Boolean parent drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/curve-boolean/interaction/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
MIN_CAPTURE_BYTES=8000 \
PARITY_CASE_IDS=curve_boolean_clipping \
INTERACTION_TRACE=curve_boolean_parent_drag \
npm --prefix tools/visual-parity run capture

INPUT_DIR=captures/local/curve-boolean/interaction/desktop \
MIN_CAPTURE_BYTES=8000 \
MIN_BOARD_SSIM=0.90 \
PARITY_CASE_IDS=curve_boolean_clipping \
npm --prefix tools/visual-parity run audit
```

Replay the focused Arc-composition parent drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/arc-compositions-interaction/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
PARITY_CASE_IDS=arc_compositions \
INTERACTION_TRACE=arc_composition_parent_drag \
npm --prefix tools/visual-parity run capture

INPUT_DIR=captures/local/arc-compositions-interaction/desktop \
MIN_BOARD_SSIM=0.90 \
PARITY_CASE_IDS=arc_compositions \
npm --prefix tools/visual-parity run audit
```

Replay the focused Sector-composition parent drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/sector-compositions/interaction/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
MIN_CAPTURE_BYTES=8000 \
PARITY_CASE_IDS=sector_compositions \
INTERACTION_TRACE=sector_composition_parent_drag \
npm --prefix tools/visual-parity run capture

INPUT_DIR=captures/local/sector-compositions/interaction/desktop \
MIN_CAPTURE_BYTES=8000 \
MIN_BOARD_SSIM=0.90 \
PARITY_CASE_IDS=sector_compositions \
npm --prefix tools/visual-parity run audit
```

Replay the focused BisectorLines source-Line drag:

```bash
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/bisector-lines/interaction/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
MIN_CAPTURE_BYTES=8000 \
PARITY_CASE_IDS=bisector_lines \
INTERACTION_TRACE=bisector_lines_parent_drag \
npm --prefix tools/visual-parity run capture

INPUT_DIR=captures/local/bisector-lines/interaction/desktop \
MIN_CAPTURE_BYTES=8000 \
MIN_BOARD_SSIM=0.93 \
PARITY_CASE_IDS=bisector_lines \
npm --prefix tools/visual-parity run audit
```

Replay the production Midpoint parent drag:

```bash
CORPUS_SOURCE=production \
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/midpoint-interaction/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
MIN_CAPTURE_BYTES=8000 \
PARITY_CASE_IDS=prod_geometry_midpoints \
INTERACTION_TRACE=midpoint_parent_drag \
npm --prefix tools/visual-parity run capture

CORPUS_SOURCE=production \
INPUT_DIR=captures/local/midpoint-interaction/desktop \
MIN_CAPTURE_BYTES=8000 \
MIN_BOARD_SSIM=0.93 \
PARITY_CASE_IDS=prod_geometry_midpoints \
npm --prefix tools/visual-parity run audit
```

The scheduled `Visual Parity` workflow runs the same audit at `1200 x 900` and
`390 x 844`, requires nontrivial captures, enforces a provisional board SSIM
floor of `0.90` for the development corpus, repeats `baseline_point_drag`, and
the direction-point Arc, circumcircle, Midpoint, dynamic Circle-radius, and
dynamic Segment-length, orthogonal-parent, and transformed-Point driver drags,
the function-coordinate Point driver drag, the Parallel and ArrowParallel
parent drags, and the triangle-center, Intersection, Arc-composition,
Sector-composition, explicit Circumcircle-creator, and Polygon
path-intersection, PolygonalChain, Parallelogram, and Curve Boolean parent
drags, plus the Point-reflection source, BisectorLines source-Line, and
four-parent PolePoint drags, the shared-radius Circle Tangent/Polar drag, the
targeted TangentTo static case and source-Point drag, and the shared-endpoint
Line Tangent/Polar and Curve Tangent/Polar Point drags,
the two Inequality drags, the VectorField mesh/scale/arrow driver drag, and
the SlopeField normalization/mesh/scale/arrow driver drag, plus the Ellipse
and Hyperbola point-parent drags and the Parabola focus drag,
and uploads the PNG pairs, contact sheets, TSV summaries, and JSON reports as
workflow artifacts. Its independent production-corpus pass uses the Stable
floor of `0.93` at both viewports.

Capture and audit the independent production corpus:

```bash
CORPUS_SOURCE=production \
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/stable-production/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
npm --prefix tools/visual-parity run capture

CORPUS_SOURCE=production \
INPUT_DIR=captures/local/stable-production/desktop \
MIN_BOARD_SSIM=0.93 \
npm --prefix tools/visual-parity run audit
```

Capture the same source as source text, official JSXGraph, and Compose Canvas:

```bash
ANDROID_SERIAL=<device-serial> ./tools/capture-android-parity.sh
ANDROID_SERIAL=<device-serial> ./tools/capture-android-parity-matrix.sh
```

The script writes ignored local evidence to:

```text
captures/local/android-parity/current/
├── summary.tsv
└── baseline_geometry/
    ├── source.png
    ├── official.png
    ├── native.png
    ├── contact-sheet.png
    └── metrics.txt
```

The script reads the board bounds from Compose semantics and compares the
official and native board crops with FFmpeg SSIM. The provisional regression
floor is `0.90` and can be overridden with `MIN_BOARD_SSIM`. This coarse metric
detects large visual regressions; it does not replace contact-sheet review.
Use `PARITY_CASE_IDS` with comma- or space-separated case IDs to select a
corpus subset. Unknown IDs fail explicitly instead of falling back to the
default case.

`JsxGraphParityCorpus` is the debug workbench source of truth for 92 cases:
30 generated production scenarios followed by 62 focused regression
fixtures. A construction document contains `boundingBox` and ordered
`objects[{id,type,parents,attributes}]`; the debug UI does not convert a
separate demo schema into handwritten native geometry. The focused
`jessiecode_native_source`, `function_circle_radius`, `transformed_points`,
`function_coordinate_points`, `parallel_constructions`, `triangle_centers`,
`intersection_points`, `intersection_paths`, `polygon_path_intersections`,
`polygonal_chains`, `parallelograms`, `regular_polygons`, `radical_axis`,
`pole_point`, `tangent_polar_circle`, `tangent_to_circle`, `tangent_line`,
`tangent_curve`, `ellipses`, `hyperbolas`, `parabolas`,
`derivative_curve`, `normal_constructions`, `spline_curves`, `riemann_sums`,
`box_plots`, `combs`, `inequalities`, `vector_fields`, `slope_fields`,
`circumcircle_creators`,
`point_reflections`, `bisector_lines`, `sector_compositions`, and
`curve_boolean_clipping` cases instead use a strict debug envelope around one
raw JessieCode source.
The focused `point3d_projection` case uses the same ordered construction
document on both renderers and projects its Point3D elements through the
ordinary 2D scene consumed by Compose.
The focused `polygon3d_projection` case uses one JessieCode source for a
coordinate-owned quadrilateral and a triangle backed by existing Point3D
elements, including nested vertex and border styles.
The focused `circle3d_projection` case uses one construction document for
numeric and function-valued Circle3D radii and normals through the ordinary
Curve3D/Curve proxy chain.
The focused `vectorfield3d_projection` case uses one JessieCode source for
component and array-returning 3D fields, inclusive meshes, path breaks, and
viewport-sized arrowheads through the ordinary Curve3D/Curve proxy chain.
The focused `intersectioncircle3d_projection` case uses one JessieCode source
for Plane/Sphere and Sphere/Sphere intersection circles with hidden parents
and ordinary Curve3D/Curve proxies.
The focused `intersectionline3d_projection` case uses one JessieCode source
for two Plane/Plane intersections through hidden Point3D definitions and
ordinary Line3D proxies.
The focused `sphere3d_projection` case uses one JessieCode source for
Point/radius Sphere3D instances in parallel and central View3D projections
through ordinary Circle and Ellipse proxies with default and custom
off-center radial gradients.
The focused `surface3d_projection` case uses one JessieCode source for a
FunctionGraph3D wireframe and a Shader-backed triangular ParametricSurface3D
expanded through ordinary Curve and Face3D proxies.
The focused `spatial_lines_planes` case likewise uses one construction
document for bounded Line3D, a finite Plane3D outline with its visible Mesh3D
wireframe, and Axis3D.
The focused `plane3d_surfaces` case uses one construction document for exact
rectangle and triangle tiling, cyclic color-array fills, HSL shading, and
height colormaps.
The focused `polyhedron3d_faces` case uses one JessieCode source for six
Face3D Curve proxies with cyclic colors, a per-face override, and local depth
ordering.
The focused `view3d_default_axes` case uses one View3D construction document
whose factory expands border Axes3D, Ticks3D, and Text3D labels in both
renderers.
The focused `view3d_center_axes` case uses one View3D construction document
whose factory expands three center Axis3D members and preserves the hidden,
non-real `O` Intersection from JSXGraph `1.13.3`.
`arc_compositions` uses the same ordered construction document on both
renderers because the official combined JessieCode form crashes before
producing a Board. A focused case is added only after the native
implementation supports every feature it declares.

The independent Stable corpus is defined in
`tools/stability/production-corpus.mjs`. Its generator validates 30 unique
sources and 55 declared capability points, then emits separate Kotlin copies
for core tests and debug/runtime consumers. CI regenerates both copies and
rejects drift. Finite production edges use the native `segment` creator;
infinite guides remain `line` elements.

The Web audit uses `?audit=true&caseId=<id>&preview=official|native` to render
only the comparison board. This removes the surrounding debug UI from image
metrics while retaining the exact same source lookup and renderer adapters.

Latest same-source workbench evidence (2026-09-25):

`Compact` is the scheduled `390 x 844` profile unless a case paragraph records
a different reviewed viewport; the new BisectorLines evidence uses
`760 x 920` as recorded below.

| Case | Desktop SSIM | Compact SSIM |
|---|---:|---:|
| `baseline_geometry` | 0.987120 | 0.974780 |
| `finite_segment` | 0.987172 | 0.974567 |
| `fixed_length_segment` | 0.986940 | 0.974092 |
| `coordinate_parents` | 0.986543 | 0.978321 |
| `shifted_geometry` | 0.986997 | 0.974236 |
| `curves` | 0.986698 | 0.975236 |
| `step_functions` | 0.987344 | 0.975382 |
| `polygons` | 0.986562 | 0.973171 |
| `polygonal_chains` | 0.986474 | 0.972464 |
| `parallelograms` | 0.985806 | 0.978662 |
| `regular_polygons` | 0.986177 | 0.977920 |
| `radical_axis` | 0.986191 | 0.982487 |
| `pole_point` | 0.985927 | 0.974830 |
| `tangent_polar_circle` | 0.986486 | 0.976391 |
| `tangent_to_circle` | 0.975735 | 0.959045 |
| `ellipses` | 0.985972 | 0.974127 |
| `hyperbolas` | 0.986155 | 0.973306 |
| `parabolas` | 0.986569 | 0.953062 |
| `tangent_line` | 0.986294 | 0.976234 |
| `tangent_curve` | 0.986097 | 0.975965 |
| `derivative_curve` | 0.986226 | 0.976714 |
| `normal_constructions` | 0.985870 | 0.973272 |
| `spline_curves` | 0.985026 | 0.970335 |
| `riemann_sums` | 0.985966 | 0.977326 |
| `box_plots` | 0.987288 | 0.976337 |
| `combs` | 0.986352 | 0.974448 |
| `inequalities` | 0.988722 | 0.973466 |
| `vector_fields` | 0.986692 | 0.974773 |
| `slope_fields` | 0.986516 | 0.974913 |
| `text` | 0.981600 | 0.954662 |
| `circular_regions` | 0.987047 | 0.974687 |
| `arc_direction_point` | 0.986060 | 0.977713 |
| `arc_compositions` | 0.986254 | 0.973086 |
| `circumcircle_creators` | 0.986771 | 0.972397 |
| `point_reflections` | 0.986216 | 0.972032 |
| `bisector_lines` | 0.986575 | 0.983159 |
| `sector_compositions` | 0.985269 | 0.971910 |
| `jessiecode_native_source` | 0.987004 | 0.974865 |
| `function_circle_radius` | 0.987261 | 0.975137 |
| `transformed_points` | 0.986656 | 0.972766 |
| `point3d_projection` | 0.986505 | 0.972805 |
| `polygon3d_projection` | 0.988028 | 0.982365 |
| `circle3d_projection` | 0.988886 | 0.985882 |
| `intersectioncircle3d_projection` | 0.989030 | 0.986025 |
| `intersectionline3d_projection` | 0.989049 | 0.986102 |
| `vectorfield3d_projection` | 0.988321 | 0.984174 |
| `sphere3d_projection` | 0.988514 | 0.985909 |
| `surface3d_projection` | 0.985262 | 0.985297 |
| `spatial_lines_planes` | 0.987328 | 0.975225 |
| `plane3d_surfaces` | 0.987317 | 0.984636 |
| `polyhedron3d_faces` | 0.987380 | 0.984777 |
| `view3d_default_axes` | 0.986921 | 0.977686 |
| `view3d_center_axes` | 0.989085 | 0.986061 |
| `function_coordinate_points` | 0.986724 | 0.973162 |
| `parallel_constructions` | 0.986754 | 0.973271 |
| `line_arrows` | 0.987817 | 0.981850 |
| `triangle_centers` | 0.986836 | 0.974277 |
| `intersection_points` | 0.986706 | 0.973276 |
| `intersection_paths` | 0.987758 | 0.982089 |
| `polygon_path_intersections` | 0.987353 | 0.981156 |
| `curve_boolean_clipping` | 0.986428 | 0.976584 |

All captures passed the nonblank and browser-error checks. These numbers are
evidence for this translated slice only; they do not satisfy the full Stable
Gate. The Curve capture additionally verifies nonempty official SVG paths for
the function graph, parametric curve, and discrete data plot. The capture
harness rejects JSXGraph's `error compiling function` console warning so a
JessieCode CSP failure cannot pass as an empty official curve. The Polygon
capture verifies independent fill and default border styles, `withLines:
false`, and coordinate-array helper vertices.
The StepFunction capture verifies rising/falling steps, repeated X
coordinates, and the missing-Y path break at both viewports. Desktop and
Compact scored `0.987344` and `0.975382`; both contact sheets were manually
reviewed for horizontal/vertical segment placement, break location, clipping,
and blank output. Runtime source-array identity and recomputation are covered
separately by the persistent JessieCode session test. This fixture remains
outside the 30-case Stable corpus.
The PolygonalChain capture verifies the official open Segment border,
transparent default fill, ordered vertices, and visible absence of the
last-to-first edge. Desktop and Compact scored `0.986474` and `0.972464`;
after dragging the final Point from `(4,3)` to `(5,-2)`, they scored
`0.986499` and `0.971890`. All four contact sheets were manually reviewed for
open-edge placement, point order, update propagation, clipping, and blank
output. The fixture remains outside the 30-case Stable corpus.
The Parallelogram capture verifies the official four-edge Polygon order,
translucent fill, exposed and styled `parallelPoint`, and three source Points.
Desktop and Compact scored `0.985806` and `0.978662`; after dragging C from
`(2,-3)` to `(3,1)`, they scored `0.985679` and `0.977151`. All four contact
sheets were manually reviewed for helper visibility, vertex and edge order,
fill, moved geometry, clipping, overlap, and blank output. The fixture remains
outside the 30-case Stable corpus.
The RegularPolygon capture verifies the official two-Point numeric form,
five-edge rotation chain, generated CAS helper identity and styling, and
source Points. Desktop and Compact scored `0.986177` and `0.977920`; after
dragging B from `(0,-2)` to `(1,0)`, they scored `0.986049` and `0.976659`.
All four contact sheets were manually reviewed for helper visibility, vertex
and edge order, fill, moved geometry, clipping, overlap, and blank output.
The fixture remains outside the 30-case Stable corpus.
The RadicalAxis capture verifies two source Circles, hidden constrained
coefficient helpers, and the infinite Line produced by their power
difference. Static Desktop `1200 x 900` and Compact `760 x 920` captures
scored `0.986191` and `0.982487`. After dragging `radius1` from `(-1,-1)` to
`(-1,1)`, the first Circle radius and dependent axis updated with scores of
`0.986249` and `0.982723`. All four contact sheets were manually reviewed for
Circle geometry, helper leakage, axis slope and clipping, moved geometry,
overlap, and blank output. The fixture remains outside the 30-case Stable
corpus.
The PolePoint capture verifies the Circle/Line determinant construction,
canonical constrained Point output, and absence of hidden helper leakage.
Static Desktop `1200 x 900` and Compact `390 x 844` captures scored
`0.985927` and `0.974830`. The interaction trace then moved the Circle center
and radius Point plus both Line Points from the official fixture's initial
coordinates to its moved coordinates; the PolePoint moved from `(5,5)` to
`(2.5,-2.5)` with scores of `0.986075` and `0.963320`. All four contact sheets
were manually reviewed for Circle radius, Line slope, parent and PolePoint
placement, dependency propagation, clipping, overlap, helper leakage, and
blank output. The fixture remains outside the 30-case Stable corpus.
The Circle/Point Tangent capture verifies the exact quadratic-form
coefficient construction for `tangent`, the `polar` alias, and the
`polarline` wrapper, including both parent orders and hidden helper Points.
Static Desktop `1200 x 900` and Compact `390 x 844` captures scored
`0.986486` and `0.976391`. After dragging the shared radius Point from
`(2,0)` to `(1,0)`, all three Lines updated with scores of `0.986356` and
`0.961215`. All four contact sheets were manually reviewed for Circle radius,
line placement, dependency propagation, clipping, overlap, helper leakage,
blank output, and Native/Official alignment. The fixture remains outside the
30-case Stable corpus.
The TangentTo capture verifies the Circle-only
`polar -> intersection -> tangent` composition, both branch indices, exposed
polar/contact identities, nested visibility and fixed state, and the
upstream `dash: 3` polar pattern. Static Desktop `1200 x 900` and Compact
`390 x 844` captures scored `0.975735` and `0.959045`. After dragging the
source Point from `(5,4)` to `(3,-3)`, both contact Points and Tangents
updated with scores of `0.979462` and `0.951020`. All four contact sheets
passed nonblank/browser checks and manual review for two distinct tangents,
two contact Points, dashed polar alignment, dependency propagation, clipping,
overlap, helper leakage, and Native/Official agreement. The fixture remains
outside the 30-case Stable corpus.
The Ellipse capture verifies the three-Point form and a numeric-major-axis
partial domain, including center/foci metadata, Conic sampling, and parent
updates. Static Desktop `1200 x 900` and Compact `390 x 844` captures scored
`0.985972` and `0.974127`. After dragging the point-on-Ellipse parent from
`(-5,4)` to `(-5,5)`, both renderers updated the major axis, center-relative
geometry, and quadratic form with scores of `0.985922` and `0.970544`.
All four contact sheets passed nonblank/browser checks and manual review for
focus/center placement, complete and partial Ellipse geometry, moved-parent
propagation, clipping, overlap, helper leakage, and Native/Official agreement.
This evidence remains outside the 30-case Stable production corpus and does
not change its 30 scenarios or 55 capabilities.
The Hyperbola capture verifies the three-Point form as two continuous
parameter-domain branches and a numeric-major-axis branch, including
center/foci metadata, Conic sampling, and parent updates. Static Desktop
`1200 x 900` and Compact `390 x 844` captures scored `0.986155` and
`0.973306`. After dragging the point-on-Hyperbola parent from `(0,3)` to
`(1,4)`, both renderers updated the major axis, both Point-defined branches,
and quadratic form with scores of `0.985998` and `0.973040`. All four contact
sheets passed nonblank/browser checks and manual review for focus/center
placement, continuous left/right branch geometry, moved-parent propagation,
clipping, overlap, helper leakage, and Native/Official agreement. The default
`±1.0001π` domain and degenerate forms remain covered by the Core and official
fixture tests. This evidence remains outside the 30-case Stable production
corpus and does not change its 30 scenarios or 55 capabilities.
The Parabola capture verifies a registered Point/Line form and a
coordinate-focus/two-Point-directrix form with explicit finite parameter
domains, constrained center metadata, Conic sampling, quadratic-form updates,
and hidden helper ownership. Static Desktop `1200 x 900` and Compact
`390 x 844` captures scored `0.986569` and `0.953062`. After dragging the
registered focus from `(-4,1)` to `(-3,2)`, both renderers updated the
Point-defined Parabola with scores of `0.986538` and `0.952681`. All four
contact sheets passed nonblank/browser checks and manual review for
focus/directrix placement, both Parabola curves, moved-focus propagation,
clipping, overlap, helper leakage, and Native/Official agreement. The visual
fixture uses `[-1.2,1.2]` domains so the official SVG renderer does not emit a
path error at the default-domain `π/2` singularity; the default `0..2π`
domain and resulting non-finite arithmetic remain covered by Core and
official fixture tests. This evidence remains outside the 30-case Stable
production corpus and does not change its 30 scenarios or 55 capabilities.
The Line/Point Tangent capture verifies direct source-endpoint reuse for
`tangent` and the `polar` alias in both parent orders, ignored nested helper
identity, and forward/reverse/finite visible ranges. Static Desktop
`1200 x 900` and Compact `390 x 844` captures scored `0.986294` and
`0.976234`. After dragging the shared second endpoint from `(3,2)` to
`(2,-3)`, the source and all three derived Lines updated with scores of
`0.986283` and `0.976048`. All four contact sheets were manually reviewed for
endpoint coincidence, range direction, color/layer overlap, parameter-Point
independence, clipping, blank output, and Native/Official alignment. The
fixture remains outside the 30-case Stable corpus.
The Curve/Point Tangent capture verifies FunctionGraph differentiation at the
Point X coordinate, the upstream FunctionGraph classification of four-parent
JessieCode string Curves, nearest-segment data-Plot projection, the `polar`
alias, hidden constrained helper Points, and finite Line ranges. Static
Desktop `1200 x 900` and Compact `390 x 844` captures scored `0.986097` and
`0.975965`. After dragging the data-Plot Point from `(0,3.5)` to `(4.5,5)`,
the nearest segment and all dependent geometry updated with scores of
`0.986078` and `0.975876`. All four contact sheets were manually reviewed for
line direction, finite ranges, nearest-segment selection, dependency
propagation, helper leakage, clipping, overlap, blank output, and
Native/Official alignment. The fixture remains outside the 30-case Stable
corpus.
The Derivative capture verifies source `X(t)` delegation,
`Numerics.D(Y)(t) / Numerics.D(X)(t)`, inherited domains, and regular Board
updates from a JessieCode-dependent coefficient Point. Static Desktop
`1200 x 900` and Compact `390 x 844` captures scored `0.986226` and
`0.976714`. After dragging the coefficient Point from `(0.25,5)` to `(0.6,5)`,
both Curves updated with scores of `0.979353` and `0.953834`. All four
canonical contact sheets passed nonblank/browser checks and manual review for
source/derivative alignment, update propagation, clipping, overlap, and
Native/Official agreement. The fixture remains outside the 30-case Stable
corpus.
The Normal capture verifies the Line ideal direction helper, Circle midpoint
reuse, FunctionGraph derivative, true parametric nearest projection, and
degree-one data-Plot nearest-segment branches. Static Desktop `1200 x 900` and
Compact `390 x 844` captures scored `0.985870` and `0.973272`. After dragging
the source Line endpoint from `(-6,3)` to `(-5,1)`, both renderers updated the
source and Normal with scores of `0.985860` and `0.959998`. All four canonical
contact sheets passed nonblank/browser checks and manual review for line
direction, parent updates, hidden-helper leakage, clipping, overlap, blank
output, and Native/Official agreement. The fixture remains outside the 30-case
Stable corpus.
The Spline capture verifies sorted natural-cubic interpolation from existing
Points and CardinalSpline interpolation from coordinate parents with a
dynamic tension Point. Static Desktop `1200 x 900` and Compact `390 x 844`
captures scored `0.985026` and `0.970335`. After dragging the tension Point
from `(0.35,-5.5)` to `(0.8,-5.5)`, both renderers updated the CardinalSpline
and scored `0.985021` and `0.970137`. All four contact sheets passed
nonblank/browser checks and manual review for knot placement, curve shape,
endpoint alignment, tension propagation, clipping, overlap, and
Native/Official agreement. The fixture remains outside the 30-case Stable
corpus.
The RiemannSum capture verifies a single-function midpoint sum and a
between-function trapezoidal sum with closed filled geometry. Static Desktop
`1200 x 900` and Compact `390 x 844` captures scored `0.985966` and
`0.977326`. After dragging the rectangle-count Point from `(4,-6)` to
`(6,-6)`, both renderers changed the second sum from four to six bars and
scored `0.964669` and `0.954787`. All four contact sheets passed
nonblank/browser checks and manual review for bar count, upper/lower
boundaries, closure, fill, clipping, overlap, and Native/Official agreement.
The fixture remains outside the 30-case Stable corpus.
The BoxPlot capture verifies vertical and horizontal Curve paths, closed
filled boxes, `smallWidth`, circle/square/plus outliers sized in viewport CSS
pixels, and dynamic quantile/axis/width terms. Static Desktop `1200 x 900` and
Compact `390 x 844` captures scored `0.987288` and `0.976337`. After dragging
the driver Point from `(5,-5.5)` to `(6.5,-5.5)`, both renderers updated the
dynamic five-number summary, axis, and width and scored `0.987262` and
`0.975597`. All four contact sheets passed nonblank/browser checks and manual
review for whiskers, median lines, fill closure, outlier face and size,
horizontal transposition, clipping, overlap, and Native/Official agreement.
The fixture remains outside the 30-case Stable corpus.
The Comb capture verifies default, reversed, and function-configured tooth
geometry, `NaN` path breaks, official blue defaults, and dynamic
frequency/width/angle/reverse evaluation. Static Desktop `1200 x 900` and
Compact `390 x 844` captures scored `0.986352` and `0.974448`. After dragging
the driver Point from `(4,-5.8)` to `(6.5,-5.8)`, both renderers changed tooth
spacing and width, rotated the teeth, reversed endpoint order, and scored
`0.986473` and `0.974210`. All four contact sheets passed nonblank/browser
checks and manual review for tooth count, baseline placement, direction,
clipping, overlap, and Native/Official agreement. The fixture remains outside
the 30-case Stable corpus.
The Inequality capture verifies the expanded Line half-plane polygon,
FunctionGraph region closure on both sides of a non-finite break, custom fill,
and dynamic `inverse`. Static Desktop `1200 x 900` and Compact `390 x 844`
captures scored `0.988722` and `0.973466`. After dragging the Line endpoint
from `(-3,3)` to `(-2,1)`, both renderers updated the half-plane and scored
`0.989238` and `0.971356`. After independently dragging the FunctionGraph
driver from `(1,-5.8)` to `(2,-5.8)`, both renderers changed the coefficient,
flipped `inverse`, and scored `0.988671` and `0.975376`. All six contact
sheets passed nonblank/browser checks and manual review for fill direction,
source-boundary alignment, segmented closure, clipping, overlap, and
Native/Official agreement. The fixture remains outside the 30-case Stable
corpus.
The VectorField capture verifies both component-function and
array-returning-function forms, dynamic mesh and scale terms, `NaN` path
breaks, and viewport CSS-pixel arrow size. Static Desktop `1200 x 900` and
Compact `390 x 844` captures scored `0.986692` and `0.974773`. After dragging
the driver Point from `(3,-5.8)` to `(6,-5.8)`, both renderers increased the
horizontal mesh count, changed scale, disabled arrowheads, and scored
`0.986709` and `0.975590`. All four contact sheets passed nonblank/browser
checks and manual review for vector placement, arrow size and direction,
mesh count, clipping, overlap, and Native/Official agreement. The fixture
remains outside the 30-case Stable corpus.
The SlopeField capture verifies string and function scalar fields, exact
unit-direction normalization, the disabled-arrow default, and dynamic mesh,
scale, and arrow terms. Static Desktop `1200 x 900` and Compact `390 x 844`
captures scored `0.986516` and `0.974913`. After dragging the driver Point
from `(3,-5.8)` to `(6,-5.8)`, both renderers increased the horizontal mesh
count, changed scale, disabled arrowheads, and scored `0.986548` and
`0.975556`. All four captures passed nonblank/browser checks and review for
direction, length, mesh count, clipping, overlap, and Native/Official
agreement. The fixture remains outside the 30-case Stable corpus.
The Text capture verifies static and numeric content, dynamic
`<value>` JessieCode evaluation, font size, stroke color/opacity, and all
translated horizontal/vertical anchor directions.
The circular-region capture verifies degree-three Bezier paths, Arc
selection/orientation, filled Sector geometry, and fixed-radius Angle
geometry.
The direction-point Arc capture verifies the four-parent `useDirection` path
and its endpoint selection at both viewports. The focused case remains a
regression fixture; the independent `prod_arc_direction_route` case and its
direction-Point interaction trace qualify that behavior for the Stable scope.
The Arc-composition capture verifies Semicircle, CircumcircleArc, MinorArc,
and MajorArc from one draggable source triangle. It also verifies that the
hidden Midpoint/Circumcenter helpers do not leak into the scene and that
default layer `9` Points remain above layer `8` Arc strokes. Static captures
scored `0.986254` on Desktop and `0.973086` on Compact. After moving B from
`(1,4)` to `(0,-4)`, every dependent composition updated and scored
`0.986107` and `0.971485`, respectively. All eight Native/Official images
were manually reviewed for geometry, endpoint ordering, clipping, blank
output, and overlap. This fixture remains outside the 30-case Stable corpus.
The explicit Circumcircle-creator capture verifies `circumcenter`, its
`circumcirclemidpoint` alias, and `circumcircle` from one draggable source
triangle. The two public constrained centers remain coincident with the
Circle's hidden center, while the hidden helper stays out of the scene.
Static captures scored `0.986771` on Desktop and `0.972397` on Compact. After
moving B from `(0,4)` to `(1,2)`, all three constructions updated and scored
`0.986870` and `0.972478`, respectively. All eight Native/Official images were
manually reviewed for center alignment, helper leakage, geometry, clipping,
blank output, and overlap. This fixture remains outside the 30-case Stable
corpus.
The Point-reflection capture verifies a Point reflected across a live Line,
the Point branch of MirrorElement, and the MirrorPoint alias from one
draggable source. The two Point-mirror outputs stay coincident while the Line
reflection remains independent. Static captures scored `0.986216` on Desktop
and `0.972032` on Compact. After moving the source from `(-3,1)` to `(-2,2)`,
all three outputs updated and scored `0.986163` and `0.972043`, respectively.
All eight Native/Official images were manually reviewed for marker
coincidence, geometry, clipping, blank output, and overlap. This fixture
remains outside the 30-case Stable corpus.
The BisectorLines capture verifies both outputs of one non-Board Composition,
their nested line attributes, default layer `7` despite a top-level
`layer: 5`, hidden constrained helper Points, and live source-Line
dependencies. Static captures at `1200 x 900` and `760 x 920` scored
`0.986575` and `0.983159`. After moving the first source endpoint from
`(-4,-1)` to `(-2,-3)`, both outputs updated and scored `0.986360` and
`0.983028`. All eight Native/Official images were reviewed for helper leakage,
geometry, clipping, blank output, and overlap. This fixture remains outside
the 30-case Stable corpus.
The Sector-composition capture verifies CircumcircleSector, MinorSector,
MajorSector, NonreflexAngle, and ReflexAngle from one draggable source
triangle. It also verifies the hidden Circumcenter, direction-Point endpoint
selection, forced minor/major and nonreflex/reflex routes, and Point-over-curve
layer order. Static captures scored `0.985269` on Desktop and `0.971910` on
Compact. After moving B from `(0,4)` to `(0,-4)`, all five dependent
compositions updated and scored `0.985899` and `0.972822`, respectively. All
eight Native/Official images were manually reviewed for helper leakage,
geometry, endpoint ordering, clipping, blank output, and overlap. This fixture
remains outside the 30-case Stable corpus.
The transformed-Point capture verifies scalar and function-valued
transformations, Point- and Line-backed parameters, and a transformation
chain. After dragging its function driver Point from `(2,0)` to `(5,0)`, both
renderers updated the dependent Points and scored `0.986523` on Desktop and
`0.972569` on Compact. It remains a focused regression fixture outside the
production corpus.
The Point3D capture verifies a source-mapped parallel View3D, numeric and
homogeneous Point3D coordinates, a translated `transform3d`, transformed
Point3D binding, and rendering through ordinary 2D proxy Points. Static
captures scored `0.986505` on Desktop and `0.972805` on Compact. After dragging
the free source proxy, both renderers projected the movement back onto the
source Point3D's constant-z plane, updated the transformed Point3D, and scored
`0.986588` and `0.972885`, respectively. All four contact sheets passed manual
review for marker position, dependency updates, clipping, overlap, and blank
output. This remains a focused preview outside the 30-case Stable corpus.
The Polygon3D capture verifies direct coordinate-owned and existing-Point3D
vertex forms, open 3D vertex storage, closed ordinary Polygon proxies, nested
vertex and border styles, and parallel projection. Static captures scored
`0.988028` on Desktop and `0.982365` on Compact. Both contact sheets passed
manual review for projected geometry, closure, fill, borders, vertex
ownership, overlap, clipping, and blank output. The remaining visible
differences are existing Point marker fill and antialiasing differences. This
is a focused preview outside the 30-case Stable corpus.
The Curve3D capture verifies component functions, one vector-valued function,
a discrete point matrix, a transformed parent, exact sampling, and ordinary
Curve proxy projection. Static captures scored `0.988916` on Desktop and
`0.986324` on Compact. Both contact sheets passed manual review for path
shape, transform offset, dash style, clipping, overlap, and blank output.
Curve3D parametric point projection additionally matches the official COBYLA
fixture above. This remains a focused preview outside the 30-case Stable
corpus.
The VectorField3D capture verifies component and array-returning field forms,
inclusive three-axis meshes, scale, zero-vector suppression, `NaN` path
breaks, ordinary Curve3D/Curve proxy identity, and viewport CSS-pixel
arrowheads generated before View3D projection. Desktop `1200 x 900` and
Compact `390 x 844` captures scored `0.988321` and `0.984174`. Both contact
sheets passed manual review for vector placement, arrow direction and size,
color, clipping, overlap, and blank output. This is a focused preview outside
the 30-case Stable corpus.
The Circle3D capture verifies numeric and function-valued radii, numeric and
function-valued normals, dynamic center/normal/radius updates,
negative-radius normalization, frame recomputation, and ordinary
Curve3D/Curve proxy projection. Desktop `1200 x 900` and Compact `390 x 844`
captures scored `0.988886` and `0.985882`. Both contact sheets passed manual
review for circle geometry, projected flattening and tilt, color, dash style,
clipping, overlap, and blank output. Circle3D parametric point projection
delegates to the same tested Curve3D path. This is a focused preview outside
the 30-case Stable corpus.
The IntersectionCircle3D capture verifies Plane/Sphere in either order,
Sphere/Sphere, hidden dynamic center ownership, canonical parent
dependencies, structured rollback, bounded sampling, and disjoint-result
hiding. Desktop `1200 x 900` and Compact `390 x 844` captures scored
`0.989030` and `0.986025`. Both contact sheets passed manual review for
projected geometry, color, dash style, placement, clipping, overlap, and blank
output. The sparse Compact official image used a reviewed 4000-byte nonblank
floor. This is a focused preview outside the 30-case Stable corpus.
The IntersectionLine3D capture verifies two Plane/Plane intersections,
View3D bounding-box clipping, hidden owned Point3D definitions, canonical
parent dependencies, creation-time snapshot semantics, structured rollback,
and ordinary Line3D proxy rendering. Desktop `1200 x 900` and Compact
`390 x 844` captures scored `0.989049` and `0.986102`. Both contact sheets
passed manual review for endpoint placement, line geometry, color, dash
style, clipping, overlap, and blank output. This is a focused preview outside
the 30-case Stable corpus.
The Sphere3D capture verifies dynamic absolute radius evaluation and the
parallel Circle versus central Ellipse projection branches from one source.
The source exercises the official default radial gradient and a custom
off-center radial gradient with second-color and opacity overrides. Desktop
`1200 x 900` and Compact `390 x 844` captures scored `0.988514` and
`0.985909`. Both contact sheets passed manual review for focal direction,
smooth interpolation, geometry, opacity, border, placement, clipping,
overlap, and blank output. The lifecycle fixture and focused JVM tests
additionally verify default and custom static gradient parameters,
`gradient: "none"`, invalid-value errors, parallel-to-central-to-parallel
runtime switching, dynamic projection evaluation, structured evaluation
failure, and complete proxy cleanup. This is a focused preview outside the
30-case Stable corpus.
The same upstream lifecycle fixture now records Line3D two-Point,
point/direction/range, copied-direction, transformed, coordinate-projection,
and screen-projection behavior; Plane3D finite, three-Point, transformed,
normal-form, outline, and parametric-projection behavior; finite Plane-owned
and direct Mesh3D sampling, default style, parent relationships, and
projection; the Axis3D wrapper; and the complete official Axes3D role list,
including its three Ticks3D members. The automatic default-axes snapshot
further proves that official
`none` has 19 role keys and 18 non-null View members, while `border` has 25
role keys, 24 non-null members, 21 direct View registrations, and three
Board-owned Ticks3D members with 11 labels each. Kotlin JVM tests cover the
translated Line3D, Plane3D, Mesh3D, Ticks3D, Text3D, Axes3D, Face3D, and
Polyhedron3D lifecycles.
Official `center` has 22 concrete members and 21 direct View registrations.
Its Board-owned `O` is a hidden, non-real `intersection` with homogeneous
coordinates `[0,0,0]` and the center x/y Line3D members as parents.
The focused `spatial_lines_planes` capture scored `0.987328` on Desktop and
`0.975225` on Compact. Both contact sheets passed manual review for the closed
Plane3D outline, both visible Mesh3D line families, Line3D clipping, Axis3D
arrow direction, layering, overlap, blank output, and viewport clipping.
The focused `plane3d_surfaces` capture verifies six rectangular color-array
faces, the upstream triangular topology, HSL shader colors, height colormap
colors, and an empty ordinary Plane outline. It scored `0.987317` on Desktop
and `0.984636` on Compact; both contact sheets passed manual review for
topology, closure, colors, transparency, borders, depth ordering, overlap,
clipping, and blank output.
The focused `view3d_default_axes` capture verifies automatic border
Axes3D scene expansion, three Ticks3D curves, and 33 labels. It scored
`0.986921` on Desktop and `0.977686` on Compact; both contact sheets passed
manual review for tick endpoints, label order and placement, axis direction,
clipping, overlap, and blank output.
The focused `view3d_center_axes` capture verifies automatic center Axes3D
scene expansion, the three colored axis directions, and the hidden non-real
`O` Intersection. It scored `0.989085` on Desktop and `0.986061` on Compact;
both contact sheets passed manual review for center alignment, arrow
direction, color, clipping, overlap, and blank output.
The focused `polyhedron3d_faces` capture verifies six projected Face3D Curve
proxies, cyclic colors, a per-face override, translucent fills, borders, and
ascending `zIndex` ordering within one Polyhedron. It scored `0.987380` on
Desktop and `0.984777` on Compact; both contact sheets passed manual review
for geometry, face closure, transparent overlays, borders, overlap, clipping,
and blank output. The local ordering test deliberately supplies near/far
faces in reverse and verifies that the scene emits them in ascending depth.
The focused `surface3d_projection` capture verifies the two-parameter
FunctionGraph3D wireframe, NaN-separated row/column paths, triangular tiling,
93 Face3D proxies, HSL Shader colors, translucent fills, and local depth
ordering. It scored `0.985262` on Desktop and `0.985297` on Compact; both
contact sheets passed manual review for geometry, topology, face closure,
shading, transparency, overlap, clipping, and blank output. Surface3D
parametric point projection additionally matches the official COBYLA fixture
above.
Together with the Polygon3D, Curve3D, VectorField3D, Circle3D,
IntersectionCircle3D, IntersectionLine3D, Sphere3D, and Surface3D fixtures,
the development corpus now contains 92 cases while the independently
qualified 30-case Stable corpus remains unchanged.
The function-coordinate Point capture verifies one function returning a
coordinate array, separate scalar coordinate functions, homogeneous
normalization, and non-draggable constrained Points. After dragging the shared
driver from `(-3,-2)` to `(-1,1)`, both renderers updated all three dependent
Points and scored `0.986769` on Desktop and `0.971963` on Compact. The fixture
remains outside the 30-case Stable corpus.
The Parallel capture verifies the three-Point `parallelpoint` and finite
`parallel` forms together with the Line/Point ideal-helper form. After dragging
the shared parent C from `(3,-3)` to `(0,4)`, both renderers moved the
ParallelPoint from `(5,-4)` to `(2,3)`, updated the finite and infinite lines,
and scored `0.986862` on Desktop and `0.972748` on Compact. Static and moved
contact sheets showed no helper-Point leakage, clipping, or endpoint mismatch.
This fixture remains outside the 30-case Stable corpus.
The Line-arrow capture verifies Arrow types `1..7`, default and double-headed
forms, and finite/ideal ArrowParallel lines from one source document. Static
captures scored `0.987817` on Desktop and `0.981850` on Compact. After moving
the shared ArrowParallel parent from `(0,-4.8)` to `(4,-3)`, both renderers
updated the finite and ideal lines and scored `0.987828` and `0.981756`,
respectively. Core and Compose geometry tests additionally cover explicit
disabled heads, Boolean/object attribute validation, per-type endpoint
shortening, short-line behavior, first/last mirroring, cubic control points,
the four-pixel Canvas boundary inset, and type 7's fixed effective size and
open stroke. This fixture remains outside the 30-case Stable corpus.
The triangle-center capture verifies `bisector`, `incenter`, and `incircle`
from the same three draggable source Points. After dragging C from `(4,-3)` to
`(1,4)`, both renderers updated the triangle edges, angle-bisector helper,
weighted Incenter, hidden Incircle center, and radius. The moved captures
scored `0.986532` on Desktop and `0.956464` on Compact; manual contact-sheet
review found no helper leakage, clipping, center displacement, or radius
mismatch. This fixture remains outside the 30-case Stable corpus.
The Intersection capture verifies indexed Circle/Line intersections,
OtherIntersection exclusion, an extended Segment/Circle intersection, and a
clipped non-real Point from one source. Static captures scored `0.986706` on
Desktop and `0.973276` on Compact. After dragging the Circle center from
`(0,0)` to `(0,4)`, both main-line intersections disappeared, the extended
intersection remained, and the clipped result stayed hidden; moved captures
scored `0.987292` and `0.973521`. All four contact sheets were manually
reviewed for branch selection, non-real visibility, clipping, geometry, blank
output, and overlap. This fixture remains outside the 30-case Stable corpus.
The path-intersection capture verifies indexed Curve/Line,
Curve/Curve, Arc/Curve, Sector/Curve, Polygon/Line, and curve-based
OtherIntersection rendering from one source. Static captures scored
`0.987758` on Desktop and `0.982089` on Compact; both Native/Official pairs
passed nonblank and browser-error checks and were reviewed for intersection
placement, hidden non-real outputs, clipping, and overlap. This fixture
remains outside the 30-case Stable corpus.
The Polygon path-intersection capture verifies Polygon/Circle,
Circle/Polygon, and Polygon/Polygon ordering from one source, including
Circle sampling and touching/overlap handling. Static captures scored
`0.987353` on Desktop and `0.981156` on Compact. After dragging one Polygon
vertex from `(2,2)` to `(3,2)`, the captures scored `0.987418` and `0.981052`.
The official `1.13.3` Intersection Points remain at their creation coordinates
after that parent move, while its direct `Geometry.meetPathPath` result and
the Kotlin dependency graph both recompute the moved intersections; this
observed lifecycle difference is documented rather than copied. All four
contact sheets were reviewed for ordering, geometry, clipping, blank output,
and overlap. This fixture remains outside the 30-case Stable corpus.
The Curve Boolean capture verifies CurveIntersection, CurveUnion, and
CurveDifference closed-path geometry and fill from one source. Static captures
scored `0.986428` on Desktop and `0.976584` on Compact. After dragging the
shared Polygon vertex from `(1,2)` to `(0,3)`, the CurveUnion output recomputed
through the regular Board update pass and scored `0.986141` and `0.976463`.
All four contact sheets were reviewed for intersection/union/difference
geometry, closure, fill, overlap, moved-parent propagation, clipping, and blank
output. This fixture remains outside the 30-case Stable corpus.
The JessieCode source capture verifies that one debug envelope drives
official `board.jc.parse` and the native public JessieCode session without
source substitution. Its Point, two-parent Segment, Circle, style, axis, grid, and
aspect-ratio output passed nonblank, browser-error, SSIM, and manual
contact-sheet checks at both viewports. It remains outside the 30-case Stable
corpus.
The function-radius fixture verifies a genuine JessieCode function parent in
either upstream order, discovered Point dependencies, `nonnegativeOnly`,
structured invocation/nonnumeric failures, and atomic helper-Point rollback.
It remains a focused regression fixture rather than a production-corpus case.
The dynamic fixed-length Segment production capture verifies one
JessieCode-string third parent, initial normalization, dependency updates,
finite endpoints, and the same source in both renderers. Core tests
additionally cover numeric and genuine function parents, either endpoint
moving, a fixed endpoint, negative lengths, `nonnegativeOnly`, fresh function
budgets, deterministic injection of the upstream random direction for
coincident endpoints, structured compile/evaluation/nonnumeric failures,
atomic helper-Point cleanup, and all-Point rollback when a dependent scene
update fails.
The source-controlled official `segment-fixed-length.mjs` fixture records
string/function/clamped lengths `3/4/0` initially and `12/13/2` after the
driver moves. It also records that JSXGraph `1.13.3` retains three helper
objects after a throwing function and propagates `NaN` geometry for a
nonnumeric result; Kotlin deliberately returns structured errors and rolls
back owned helpers instead.
The baseline Native preview additionally exposes its amber Point through the
production `JsxGraphSession` interaction path. Core behavior tests verify the
same Point movement, dependent constrained-Point/Line/Circle updates,
interaction-state capture/reset/restore, fixed/constrained rejection, and
atomic rollback when a drag would produce invalid geometry. Compose tests
verify upstream Point hit tolerance, visibility/fixed filtering, and reverse
creation-order priority.

The source-controlled `baseline_point_drag` trace moves the amber Point from
`(3.2, 2.1)` to `(1.1, 0.55)` in both renderers. The latest post-drag
Native/Official full-board SSIM was `0.984721` on Desktop and `0.974036` on
Compact; both profiles completed without browser errors.

Latest independent production evidence (2026-09-17):

| Profile | Cases | Lowest SSIM | Stable floor |
| --- | ---: | ---: | ---: |
| Desktop `1200 x 900` | 30/30 | 0.981779 | 0.93 |
| Compact `390 x 844` | 30/30 | 0.961778 | 0.93 |

The lowest cases are `prod_text_anchor_board` on Desktop and
`prod_text_opacity_caption` on Compact; their remaining differences are font
rasterization. The dynamic fixed-length Segment production case scored
`0.986731` on Desktop and `0.973133` on Compact before movement. After moving
its driver Point from `(3,-2.5)` to `(5,-2.5)`, both renderers extended the
length from `4` to `6` and scored `0.986731` and `0.972964`, respectively.
The four-point
`prod_arc_direction_route` case scored `0.986647` on Desktop and `0.973102`
on Compact before movement, then `0.986688` and `0.973192` after its
direction Point flipped the selected route. `prod_angle_auto_wedge` resolves
the upstream automatic radius rule from the actual viewport CSS-pixel scale.
Its geometry and all other production cases were manually reviewed in the
paged contact sheets linked from
[`stability-report.md`](stability-report.md).
The three-Point `prod_geometry_circumcircle` case scored `0.986780` on
Desktop and `0.973500` on Compact, then `0.986788` and `0.971316` after one
defining Point moved. Both renderers kept the implicit circumcenter hidden.
The `prod_geometry_midpoints` case verifies both two-Point and one-Line parent
forms. It scored `0.986439` on Desktop and `0.972343` on Compact, then
`0.986477` and `0.972654` after moving a defining Point from `(4,2)` to
`(2,4)`. Core and official-fixture tests additionally cover Point name/ID and
coordinate-array parents, ideal and per-coordinate `NaN` propagation, helper
ownership, recursive removal, and failed-registration rollback. A JessieCode
function returning a coordinate array as a Point parent remains explicitly
unsupported.
The `prod_geometry_orthogonal_constructions` case covers
OrthogonalProjection, PerpendicularPoint, Perpendicular, and
PerpendicularSegment in both Point/Line parent orders. It scored `0.986433` on
Desktop and `0.972494` on Compact, then `0.986452` and `0.972272` after moving
the shared driver Point from `(3,-3)` to `(0,4)`. Core and official-fixture
tests additionally cover coordinate-array Point parents, projection versus
perpendicular endpoint branches, dependency ownership, removal behavior,
degenerate Lines, and atomic creator failures.
The clamped dynamic-radius production case scored `0.987324` on Desktop and
`0.975346` on Compact before movement. After moving its driver Point so the
string radius became negative, both renderers clamped the radius to zero and
scored `0.987084` and `0.974247`.

The matrix script captures these logical window profiles and restores the
device's previous size, density, and font scale on success or failure:

- `400 x 400 dp`
- `400 x 500 dp`
- `610 x 500 dp`
- `900 x 1000 dp`
- `400 x 500 dp` at `1.5` font scale

## Comparison Contract

Each parity case must contain one source input. Both the official adapter and
the native renderer consume that exact input. Construction cases contain the
ordered document directly. A JessieCode case contains a strict debug envelope
whose same raw source drives both interpreters. A case is invalid when either
side silently substitutes hard-coded geometry.

The official renderer is isolated in `jsxgraph-debug-ui` and must:

- load the pinned JSXGraph distribution from local assets;
- block network, file URL, content, popup, and mixed-content access;
- report explicit loading, ready, or error state;
- release its WebView and renderer process resources.

The native renderer must not depend on those assets, WebView, or JavaScript.
The JessieCode parity envelope must remain in `jsxgraph-debug-ui`; production
modules expose typed native options and source APIs instead.

## Stable Gate

`stable` requires evidence across the supported corpus, not one successful
sample:

1. The source parses without fallback on both renderers.
2. Official and native captures are nonblank and report ready.
3. Geometry is compared for viewport, axes, object bounds, intersections, and
   relative placement.
4. Text, color, stroke, point, and interaction differences are reviewed.
5. Behavior tests cover source/session replacement, preview-tab retention,
   Point drag dependency updates, and interaction-state restoration.
6. Screen captures cover compact, medium, and expanded widths and representative
   heights, plus 1.5 font scale.
7. Android, iOS Simulator, JVM, and Wasm builds remain green.
8. Every accepted mismatch is documented; unreviewed visual drift fails the
   gate.
9. Independent scene and interaction replay is deterministic.
10. Generated stress and JVM soak tests remain inside their source-controlled
    element, time, P95, and retained-heap limits.
11. Android, iOS, Desktop, and Web run the same 30-case load screen to its
    final case.
12. Production Maven coordinates, MIT POM metadata, source-safety scans,
    publication archive scans, and debug/release APK permission audits pass.

The publication archive gate verifies every production POM and every generated
JAR, AAR, KLIB, and resource ZIP. It rejects debug-ui dependencies, unsafe
archive entries, credential or internal-organization patterns, and absolute
developer-machine paths. Kotlin/Native and Kotlin/Wasm compilation normalizes
KLIB source metadata relative to the repository root before publication.

All gates above pass for the documented Point/Line/Segment with optional
numeric or JessieCode-string fixed length/Circle/Midpoint/
OrthogonalProjection/PerpendicularPoint/Perpendicular/PerpendicularSegment/
Curve/Polygon/Text/Arc/Sector/Angle construction, rendering, and
Point-interaction scope.
That scope is rated **Stable**. Unsupported JSXGraph APIs and element families
remain outside the rating and fail explicitly where they cross the production
document boundary.
