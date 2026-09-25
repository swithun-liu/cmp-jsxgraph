# Production Code Readiness

This document defines the code-level evidence required for CMP JSXGraph to be
rated Stable. A successful build or a single visual demo is not sufficient.

## Current State

- JSXGraph compatibility baseline: `1.13.3`
- Native production modules: `jsxgraph-core` and `jsxgraph-compose`
- Runtime implementation: Kotlin Multiplatform parser, translated Board and
  geometry model, scene snapshots, and Compose Canvas rendering
- Official JSXGraph usage: isolated in `jsxgraph-debug-ui`
- Release status: **Stable for the documented support scope**

The Stable scope is the bounded construction-document API for Point, Line,
Segment with an optional numeric or JessieCode-string fixed-length parent,
Circle, Midpoint, OrthogonalProjection, PerpendicularPoint, Perpendicular,
PerpendicularSegment, Curve, FunctionGraph, Plot, Polygon, Text, three-Point
circumcircles, three-point Arc, four-point Arc with `useDirection`, Sector, and
Angle, including free Point drag, dependent updates, `nonnegativeOnly` dynamic
string Circle radii, dynamic Segment length updates, fixed-length endpoint
coupling, Midpoint and orthogonal-construction parent updates, and
interaction-state capture/restore. The remaining JSXGraph API and element
families are not included.

The public `JsxGraphJessieCode` one-shot and stateful source APIs are a
resource-bounded preview. They execute only the translated JessieCode subset,
never arbitrary JavaScript, and are not included in the current Stable rating.
Post-`0.1.0` construction-document and JessieCode preview work includes
ParallelPoint/Parallel and Bisector/Incenter/Incircle with source-mapped tests
and focused same-source parity. Static Arrow/ArrowParallel creation and Canvas
head types `1..7` now have source-mapped creator, geometry, and focused
static/parent-drag parity evidence. Dynamic/highlight and non-Line arrows
remain pending. The non-Board Composition subset and
JessieCode-only BisectorLines creator now have source-mapped construction,
update, removal, nested-attribute, and focused parity evidence; the JSON
document path continues to reject that compound return explicitly.
Ordered construction documents now expose 2D `transform` IDs to subsequent
transformed Points. The source-mapped 4x4 Transformation kernel covers every
JSXGraph `1.13.3` 3D matrix form with official numerical fixture evidence;
the bounded `View3D`, `Point3D`, `Line3D`, Plane3D wireframe, `Mesh3D`,
`Axis3D`, `Face3D`, `Polyhedron3D`, and `transform3d` lifecycle now has
source-mapped construction, projection, 2D proxy rendering, transformation
binding, update/removal, structured-error, official-fixture, and focused JVM
evidence. Point3D additionally has drag and `applyOnce` parity evidence;
Line3D, finite Plane3D outlines with visible Mesh3D wireframes, Axis3D, and
Polyhedron3D have focused Desktop/Compact static parity evidence. Text3D,
Ticks3D, explicit Axes3D scene expansion, and automatic `border`/`none`
View3D axes now have bounded JSON/JessieCode creation and resource accounting.
The Polyhedron3D slice accepts direct and transformed creators, Point-backed,
function-valued, and homogeneous vertices, cyclic `fillColorArray` plus
per-face overrides, HSL Face3D shaders, ordinary Curve proxies, and ascending
face `zIndex` ordering within each Polyhedron. This 3D preview remains outside
the qualified Stable scope. The centered Axes3D origin helper, Plane3D
shader/colormap/color-array surface modes, global View3D `depthOrder`/layer
configuration, camera controls, Point3D gliders and animations, and the
remaining 3D APIs are still pending.
Semicircle, CircumcircleArc, MinorArc, and MajorArc now have the same
source-mapped construction, update, lifecycle, and focused parity evidence.
Intersection and OtherIntersection now have source-mapped Line/Segment/Circle,
Curve, Arc, Sector, Polygon/Line, and Polygon/path dispatch, including finite
clipping, arbitrary numeric indices, ideal/non-real outputs, dependency
updates, and focused same-source parity evidence. The translated
`Clip.findIntersections` phase covers Circle, Curve, Arc, Sector, and Polygon
paths. Translated Conic Intersection and OtherIntersection dispatch remains
explicitly unsupported pending dedicated numerical, lifecycle, resource, and
parity evidence. CurveIntersection, CurveUnion, and CurveDifference now cover the
Greiner-Hormann degenerate classification, entry/exit, empty/containment,
multi-component, and tracing chain, regular Board updates, structured
topology failures, and focused static/parent-drag parity. Raw coordinate-array
path inputs and Stable qualification remain pending.
StepFunction now has source-mapped `updateDataArray` expansion, retained
JessieCode array identity, missing-Y path breaks, JSON/JessieCode resource
checks on expanded output size, and focused Desktop/Compact same-source
parity. It remains outside the independent Stable corpus.
PolygonalChain now has source-mapped wrapper construction, official border
ordering, open vertices and Segment border rendering, explicit/helper Point
ownership and removal, JSON/JessieCode vertex limits, and focused static plus
parent-drag Desktop/Compact parity. It remains outside the independent Stable
corpus.
Parallelogram now has source-mapped ParallelPoint-plus-Polygon construction,
official vertex/border/Board order, exposed helper identity, forced
draggable/non-fixed helper state, parent updates, helper and coordinate-Point
ownership/removal, bounded JSON/JessieCode paths, atomic rollback, and focused
static plus parent-drag Desktop/Compact parity. It remains outside the
independent Stable corpus.
RegularPolygon now has source-mapped numeric-count and existing-Point forms,
chained rotation transforms, fractional-count behavior, CAS helper identity,
complete `vertices.ids` mapping, forced draggable/non-fixed helpers, parent
updates, coordinate/helper ownership and removal, bounded JSON/JessieCode
paths, atomic rollback, and focused static plus parent-drag Desktop/Compact
parity. It remains outside the independent Stable corpus.
RadicalAxis now has source-mapped two-Circle coefficient construction,
one-function constrained helper Points, Circle parent/dependency updates,
official removal and degenerate arithmetic, nested helper identity,
bounded JSON/JessieCode paths, atomic duplicate-ID rollback, and focused
static plus radius-Point-drag Desktop/Compact parity. It remains outside the
independent Stable corpus.
PolePoint now has source-mapped Circle/Line construction in either parent
order, canonical parent metadata, the exact determinant coordinate closure,
dependency and removal behavior, non-finite degenerate arithmetic, bounded
JSON/JessieCode paths, structured atomic rollback, and focused static plus
four-parent-drag Desktop/Compact parity. Ellipse, Hyperbola, and Parabola
construction are translated, but their Conic/Line PolePoint forms remain
explicitly unsupported pending dedicated interop evidence. The translated
subset remains outside the independent Stable corpus.
The Circle/Point, Line/Point, and Curve/Point branches of Tangent now have
source-mapped construction, the registered Polar alias, and the Circle-only
PolarLine wrapper. Circle/Point preserves exact hidden one-function Line
endpoints, Point-owned removal behavior, and center/zero-radius arithmetic.
Line/Point preserves direct source-endpoint reuse, unconstrained/draggable
state, metadata/dependency asymmetry, endpoint-owned removal, and ignored
nested helper identity. Curve/Point preserves FunctionGraph X-parameter
derivatives, nearest-parameter projection for direct function-valued
parametric Curves, nearest-segment projection for data Plots, upstream
FunctionGraph classification for JessieCode string x-terms, hidden helpers,
and Point-only ownership. All three branches have bounded JSON/JessieCode
paths, structured atomic failure handling, and focused static plus parent-drag
Desktop/Compact parity. Ellipse/Hyperbola/Parabola Tangent/Polar and PolarLine
Conic forms, Turtle, and one-parent Glider branches remain explicitly
unsupported, and this slice remains outside the independent Stable corpus.
TangentTo now has source-mapped Circle-only
`polar -> intersection -> tangent` composition, numeric truthiness for
selecting either contact Point, exposed polar/contact identities, nested
attributes, dependency and removal behavior, non-real/degenerate arithmetic,
bounded three-object JSON/JessieCode scene expansion, and atomic rollback.
Focused static and source-Point-drag Desktop/Compact parity passes. Ellipse,
Hyperbola, and Parabola construction are translated, but their TangentTo Conic
branches remain explicitly
unsupported pending dedicated interop evidence. This slice remains outside
the independent Stable corpus.
Ellipse now has source-mapped `src/element/conic.js -> createEllipse`
construction from Point/reference/coordinate foci and either a Point on the
Ellipse or a numeric/function-valued major axis. It preserves optional numeric
domains, constrained center creation, foci/center/midpoint/major-axis metadata,
quadratic-form and parent updates, helper ownership/removal, and degenerate
JavaScript `Double` arithmetic. JSON/JessieCode limits, native session
updates, duplicate-ID rejection, structured failures, atomic rollback, and
focused static plus point-parent-drag Desktop/Compact parity pass. Adaptive
plotting and independent Stable qualification remain pending. Tangent, Polar,
PolarLine, PolePoint, TangentTo, Normal, Intersection, and OtherIntersection
Conic forms remain explicitly unsupported until their own source-mapped
translation and evidence are complete.
Hyperbola now has source-mapped
`src/element/conic.js -> createHyperbola` construction from
Point/reference/coordinate foci and either a Point on the Hyperbola or a
numeric/function-valued major axis. It preserves optional numeric domains and
the official default domain, constrained center creation,
foci/center/midpoint/major-axis metadata, quadratic-form and parent updates,
helper ownership/removal, and degenerate JavaScript `Double` arithmetic.
JSON/JessieCode limits, native session updates, duplicate-ID rejection,
structured failures, atomic rollback, and focused static plus
point-parent-drag Desktop/Compact parity pass. Adaptive plotting and
independent Stable qualification remain pending. Tangent, Polar, PolarLine,
PolePoint, TangentTo, Normal, Intersection, and OtherIntersection Conic forms
remain explicitly unsupported until their own source-mapped translation and
evidence are complete.
Parabola now has source-mapped
`src/element/conic.js -> createParabola` construction from a
Point/reference/function-returning-Point or coordinate focus and either a
registered Line or an implicit two-Point directrix. It preserves optional
numeric domains and the official default domain, constrained
focus-projection `center`/`midpoint` metadata, quadratic-form and
focus/directrix updates, helper ownership/removal, and degenerate JavaScript
`Double` arithmetic. JSON/JessieCode limits, native session updates,
duplicate-ID rejection, structured failures, atomic rollback, and focused
static plus focus-drag Desktop/Compact parity pass. The visual fixture uses a
finite domain around the visible branch; Core and official fixture tests
retain coverage of the default-domain singularity. Adaptive plotting, richer
implicit-Line attributes, and independent Stable qualification remain
pending. Tangent, Polar, PolarLine, PolePoint, TangentTo, Normal,
Intersection, and OtherIntersection Conic forms remain explicitly unsupported
until their own source-mapped translation and evidence are complete.
Derivative now has source-mapped `createDerivative` construction over
`Numerics.D`, FunctionGraph/parametric/data-Plot coverage, linear array
interpolation and viewport-padded data domains, infinite-slope preservation,
regular Board recomputation, metadata-only source parents, source-removal
survival, bounded JSON/JessieCode paths, structured atomic failures, and
focused static plus coefficient-Point-drag Desktop/Compact parity. It remains
outside the independent Stable corpus.
Normal now has source-mapped Line/Point, Circle/Point, and Curve/Point
construction in either parent order. It preserves ideal or hidden helper
identity, `point`/`subs`/`inherits` metadata, FunctionGraph derivatives,
parametric and data-Plot projection, explicit-parent ownership, removal
behavior, bounded JSON/JessieCode paths, and structured atomic rollback.
Focused static plus parent-drag Desktop/Compact parity passes. Coordinate-array
Point inputs are explicitly rejected instead of reproducing the JSXGraph
`1.13.3` stack overflow; Glider, Turtle, transformed-Curve, degree-three
Plot/Bezier, and translated Conic branches remain unsupported. Those branches
require dedicated interop evidence rather than implicit use of the generic
Curve path. This slice remains outside the independent Stable corpus.
Spline and CardinalSpline now have source-mapped natural-cubic and cardinal
interpolation, dynamic Point and tension updates, Point ownership/removal
semantics, coordinate and parallel-array modes, bounded JSON/JessieCode paths,
structured failures, and focused static plus tension-drag Desktop/Compact
parity. Fewer than two interpolation points are rejected before registration
instead of producing delayed invalid geometry. This slice remains outside the
independent Stable corpus.
RiemannSum now has source-mapped single-function and between-function
construction over `Numerics.riemann`, dynamic rectangle count/type/bounds,
cached `Value()`, default and explicit fill, dependency/removal behavior,
bounded `5n`/`34n`/`63n` output, structured atomic failures, and focused
static plus bar-count-drag Desktop/Compact parity. This slice remains outside
the independent Stable corpus.
BoxPlot now has source-mapped dynamic quantile, axis, and width terms,
vertical/horizontal geometry, `smallWidth`, all documented outlier faces,
viewport CSS-pixel outlier sizing, official blue fill defaults,
dependency/removal behavior, exact output bounds, structured atomic failures,
and focused static plus driver-drag Desktop/Compact parity. This slice remains
outside the independent Stable corpus.
Comb now has source-mapped Point/reference/coordinate endpoint construction,
exact upstream tooth geometry and path breaks, numeric/function-valued
frequency/width/angle/reverse terms, hidden helper lifecycle, default blue
stroke, bounded output, structured failures, and focused static plus
driver-drag Desktop/Compact parity. Atomic helper cleanup intentionally avoids
the upstream invalid-second-parent leak. This slice remains outside the
independent Stable corpus.
Inequality now has source-mapped Line and FunctionGraph region construction,
Board-bound-aware half-plane expansion, segmented closure across non-finite
FunctionGraph runs, dynamic `inverse`, official fill defaults,
metadata-only parent/removal behavior, bounded output, structured failures,
and focused static plus two independent interaction traces at Desktop and
Compact sizes. This slice remains outside the independent Stable corpus.
VectorField now has source-mapped component and array-returning function
forms, dynamic mesh/scale/arrow attributes, exact inclusive step and `NaN`
path-break behavior, metadata-only closure dependencies, bounded output,
structured atomic failures, and viewport CSS-pixel arrow sizing. Focused
static and driver-drag parity passes at Desktop and Compact sizes. SlopeField
now wraps that path with one scalar function/expression, the exact upstream
unit-direction normalization, disabled-arrow default, non-finite arithmetic,
bounded output, and structured atomic failures. Its focused static and
driver-drag parity also passes at Desktop and Compact sizes. Runtime `setF`,
independent production qualification, and Stable promotion remain pending, so
both slices are outside the Stable corpus.
Supported Canvas render items also follow the translated default/explicit
layer order, including independent Polygon sub-elements. These additions do
not expand the independent 30-case Stable corpus or this document's release
rating.
Supported Point, Line, Circle, Curve, and Polygon-border strokes now also
apply the seven upstream Canvas dash patterns and `dashScale` semantics.
Compose converts the resolved CSS-pixel intervals to dp and adapts only the
zero-length dotted interval to Skia's positive-interval requirement. This
renderer addition likewise does not expand the independent Stable corpus.

## Expected Behavior

Stable means supported input can be parsed, rendered, and interacted with in a
production application without WebView or a JavaScript engine in the
production path. Results are deterministic, resource use is bounded, expected
failures use `GMResult`, unsupported behavior is documented, and no known
high-severity defect remains in the supported contract.

## Promotion Gates

| Gate | Requirement | Current evidence | Status |
| --- | --- | --- | --- |
| Visual fidelity | No blocked mismatch in the independent production corpus | 30 Native/Official pairs at Desktop and Compact; 60/60 pass SSIM `0.93` | Passing |
| Capability coverage | Every declared production capability appears in an independent case | 55/55 capability points across 30 scenarios | Passing |
| Determinism | Repeated parsing returns the same complete scene | 30/30 scene replays | Passing |
| Interaction replay | Move, capture, restore, and reset are repeatable | 8/8 production interaction scenarios plus 64 generated updates | Passing |
| Parser/render robustness | Deterministic generated inputs stay finite and bounded | 512 unique Native stress inputs | Passing |
| Core throughput | 900 warmed renders complete within 45s and P95 is at most 500ms | Local baseline: 292ms total, 1.649ms P95 | Passing |
| Core retained heap | The same soak retains at most 64MiB after forced GC | Local baseline: 0 retained bytes | Passing |
| Runtime matrix | Android, iOS Simulator, Desktop, and Web reach the final shared load case | All four platforms completed the 30-case traversal | Passing locally |
| Build matrix | Production modules and samples compile on supported targets | Android debug/release, Desktop, Web, iOS device/simulator targets | Passing locally |
| Publication | Production coordinates and MIT POM metadata are exact | `com.swithun:jsxgraph-core:0.1.0` and `com.swithun:jsxgraph-compose:0.1.0`; 14 POMs verified | Passing |
| Public-source safety | Published source and artifacts contain no private endpoint, credential material, or developer-machine path | Tracked/untracked repository scan, 38-archive publication scan, normalized KLIB source metadata, and Android debug/release permission audit | Passing |

## Production Boundaries

The production dependency graph is:

```text
application
    -> jsxgraph-compose
        -> jsxgraph-core
```

`jsxgraph-debug-ui` is not a production dependency and is excluded from the
production publication set. It alone contains the pinned official JSXGraph
assets and the Android WebView/same-origin Web iframe comparison adapters.
Neither production module depends on WebView, JavaScriptCore, QuickJS, or a
browser JavaScript runtime.

Construction and JessieCode inputs are bounded by source length, JSON depth
and value count, object count, parser depth, evaluation steps, collection
growth, curve output point count, polygon vertex count, and text length. Invalid or
unsupported input returns a structured `GMResult.Err`; it is not silently
replaced with different geometry.

The JSON envelope used to compare one JessieCode source against official
`board.jc.parse` exists only in `jsxgraph-debug-ui`. It is not a production
serialization contract and does not add a JavaScript execution path to either
published module.

## Performance And Runtime Gate

The core soak measures parse, translated Board creation, dependency updates,
and scene generation after three warmup rounds. Canvas/runtime measurements
remain separate because Compose, Skia, device density, and GPU behavior are
platform-specific.

| Platform | Corpus | Latency | Memory | Outcome |
| --- | ---: | --- | --- | --- |
| Android Emulator | 30 | Automatic traversal reached final case | 129.1MiB PSS; 195.9MiB RSS; WebViews 0 | Passed |
| iOS Simulator | 30 | Automatic traversal reached final case | 252.3MiB observed peak; 246.8MiB final host RSS | Passed |
| Desktop JVM | 30 | Process survived 30s beyond automatic traversal | 213.7MiB final RSS | Passed |
| Web | 30 | 542ms first content; 2.110s traversal | 7.8MiB retained JS heap | Passed 15s/96MiB budget |

The Desktop run has machine-readable process evidence but no current
screenshot because macOS screen-recording permission prevented window capture.

## Integration Guidance

A production adopter should:

- handle `JsxGraphDocumentError` and `JsxGraphInteractionError` without retry
  loops;
- when evaluating the preview JessieCode subset, handle
  `JsxGraphJessieCodeError` and keep default resource limits;
- retain the default engine limits unless a larger trusted document requires
  an explicit increase;
- record duration and error category without logging source text;
- preserve a fallback to source text or another safe representation;
- keep `jsxgraph-debug-ui` in debug or audit configurations only.

Application canaries and rollout controls remain useful, but they belong to
the adopting application and are not part of this repository's code-level
rating.

## Stable Rating Rule

Stable applies only while every gate above passes, the latest evidence is
linked from [`stability-report.md`](stability-report.md), and no open
severity-1 correctness, crash, resource, licensing, or data-exposure defect is
known in the supported contract.

All current code-level gates pass for the documented JSXGraph `1.13.3` subset.
CMP JSXGraph is therefore rated **Stable for that support scope**. This does
not imply complete JSXGraph compatibility; unsupported features remain listed
in [`translation-status.md`](translation-status.md) and
[`translation-deviations.md`](translation-deviations.md).
