# Translation Deviations

This file records intentional behavior differences from JSXGraph `1.13.3`.
Every difference must be explicit, narrow, and covered by tests where
practical.

## Unsupported

- `src/unused/symbolic.js`: Symbolic/CAS operations and symbolic locus
  derivation are not implemented.
- `src/math/numerics.js -> glomin`: not imported because its embedded source
  notice identifies the algorithm as GNU LGPL code. This repository requires
  verified MIT-compatible provenance.

## Kotlin Runtime Adaptations

- `JsxGraphEngine` accepts a bounded JSON construction document containing
  `boundingBox`, Board flags, and ordered
  `objects[{id,type,parents,attributes}]`. This is a serialization of
  `Board.create(type, parents, attributes)`, not an upstream JSXGraph file
  reader format. The current production subset creates Point, Line, Arrow,
  Segment with an optional numeric or JessieCode-string fixed-length parent,
  Circle, Midpoint, OrthogonalProjection, PerpendicularPoint, Perpendicular,
  PerpendicularSegment, ParallelPoint, Parallel, ArrowParallel, Curve,
  FunctionGraph, Plot, StepFunction, Derivative, Spline, CardinalSpline,
  Polygon, PolygonalChain, Parallelogram, RegularPolygon, RadicalAxis,
  PolePoint, Circle/Point,
  Line/Point, and
  Curve/Point Tangent, Polar, Circle/Point TangentTo and PolarLine, Ellipse,
  Hyperbola, Parabola, Text, Arc,
  CircumcircleArc, MinorArc, MajorArc, Sector, CircumcircleSector, MinorSector,
  MajorSector, Angle, NonreflexAngle, and ReflexAngle through the translated
  native registry and then snapshots the resulting Board elements into a
  platform-independent scene. Unsupported element types, fields, attributes,
  point faces, labels, Curve/Arc/Sector arrows, and plotting modes
  return `JsxGraphDocumentError` instead of being ignored.
- Construction documents are limited by source length, JSON depth, JSON value
  count, object count, points per Curve, vertices per Polygon, and characters
  per Text. JSON and factory failures are converted to `GMResult.Err`; object
  IDs are required and duplicate IDs are rejected. Colors currently accept CSS
  hex forms plus a small named-color subset. Top-level
  Point/Line/Arrow/Segment/Circle/Midpoint/OrthogonalProjection/
  PerpendicularPoint/Perpendicular/PerpendicularSegment/ParallelPoint/Parallel/
  ArrowParallel/Curve/CurveIntersection/CurveUnion/CurveDifference/
  StepFunction/Derivative/Spline/CardinalSpline/Polygon/PolygonalChain/
  Parallelogram/RegularPolygon/RadicalAxis/PolePoint/
  Circle-Line-or-Curve-Point Tangent-Polar/
  Circle-Point TangentTo/PolarLine/Ellipse/Hyperbola/Parabola/
  Text/Arc/Arc-composition/
  Sector/Sector-composition/
  Angle defaults match the translated JSXGraph `1.13.3` subset; helper Points
  created from
  coordinate-array parents remain in the internal Board. Polygon-owned helper
  Points are represented as Polygon scene sub-elements; other helpers are not
  emitted as top-level source elements.
- `bisectorlines` is exposed through native JessieCode but rejected explicitly
  by the JSON construction-document path. The current document contract maps
  each requested object to one registered geometry element and one scene
  identity, while JSXGraph returns a non-Board `Composition` containing two
  independently registered Lines. Kotlin does not silently flatten that
  compound result or invent a document serialization for it.
- Supported construction-document and scene elements accept a finite
  non-negative integer `layer`, defaulted from `Options.layer`. Compose Canvas
  sorts supported render items by `(layer, creation position)`, including Grid
  at layer `1`, Axis at layer `2`, ordinary top-level elements, and independent
  Polygon fill, border, and implicit-vertex items. This preserves the observed
  Point-over-Arc and Polygon sub-element order. Nested Polygon
  `vertices`/`borders` visual attributes, runtime layer mutation, custom
  Grid/Axis/Ticks layers, traces, images, and untranslated renderer objects
  remain pending; this is not a claim of complete renderer-layer parity.
- Supported Point, Line, Circle, Curve, and Polygon-border strokes accept the
  integer `dash` values `0..7` from
  `src/renderer/abstract.js -> dashArray`. `dashScale: true` scales every
  interval by `strokeWidth / 2`; the platform-independent scene stores the
  resolved CSS-pixel lengths. Compose maps those lengths to dp and replaces
  only the zero-length segment in pattern 7 with Skia's minimum positive
  `0.001` pixel interval. Dynamic dash mutation and untranslated renderer
  objects remain pending.
- `JsxGraphEngine.createSession` retains the translated Board for production
  interaction while `parse` remains the immutable one-shot API. The current
  interaction subset moves visible, free, non-fixed top-level Points with
  upstream Point hit tolerance and reverse creation-order priority. Each move
  runs the Board update lifecycle and snapshots all dependent source elements.
  State capture/reset/restore is explicit and a move or restore that produces
  invalid scene geometry is rolled back atomically. Point events, hover
  styling, multi-pointer gestures, keyboard movement, object dragging, pan,
  zoom, snapping, gliders, groups, and persistent transformations remain
  pending.
- `Intersection` currently translates
  `src/base/point.js -> createIntersectionPoint` and
  `src/math/geometry.js -> intersectionFunction` for Line/Segment/Circle,
  discrete and continuous Curve, Arc, Sector, Polygon/Line, and Polygon/path
  pairs.
  Curve/Curve and Curve/Circle use the translated segment or cubic-Bezier
  traversal; continuous Curve/Curve uses the upstream 20-by-20 damped-Newton
  search, and continuous Curve/Line uses discrete selection followed by
  scalar-root refinement. Polygon intersections with Circle, Curve, Arc,
  Sector, and Polygon use the translated `src/math/clip.js -> _getPath`,
  `findIntersections`, and phase-one sorting path. Crossing, touching,
  collinear-overlap endpoints, reverse parent order, and out-of-range results
  follow the upstream ordering. Numeric or JessieCode-function branch indices,
  `alwaysIntersect` Segment/ray clipping, the upstream first-parent-only Arc
  clipping rule, ideal/non-real results, and dependency updates are preserved.
  `OtherIntersection` covers the translated Curve/Circle/Line
  combinations, one Point or an array of excluded Points, and numeric
  `precision`; excluded Points remain parent metadata without owning child
  links. A non-real intersection remains in `JsxGraphScene` with
  `isReal: false` and is suppressed by Compose drawing and hit testing.
  For Polygon/path input, JSXGraph `1.13.3` throws a property-access error for
  an in-range fractional index; Kotlin returns
  `GMResult.Err(InvalidIntersectionIndex)` instead. The same upstream release
  leaves an existing Polygon/path Intersection Point stale after a defining
  Polygon vertex moves, even though a direct `Geometry.meetPathPath` call
  returns the updated coordinate. Kotlin keeps the dependency edge and
  recomputes the Point. Dynamic `alwaysIntersect`/`precision` visual-property
  forms remain unsupported. Ellipse, Hyperbola, and Parabola construction are
  translated, but
  Intersection and OtherIntersection reject an `OBJECT_TYPE_CONIC` parent
  with `UnsupportedConic` until that dispatch has dedicated numerical,
  lifecycle, resource-limit, and parity evidence.
- CurveIntersection, CurveUnion, and CurveDifference translate
  `src/base/curve.js` creator wrappers and the complete GeometryElement path
  form of `src/math/clip.js -> greinerHormann`. The mutable linked topology is
  isolated from the immutable `Clip.findIntersections` ordering used by
  Intersection Points. Degenerate touching/bouncing chains, fully degenerate
  paths, entry/exit marking, empty and containment cases, multi-component
  separators, and tracing follow the upstream algorithm; invalid topology and
  bounded traversal failures return `GMResult.Err` instead of propagating a
  JavaScript exception or looping indefinitely. The output Curve has no
  explicit parents or source-child links, matching JSXGraph `1.13.3`, and
  recomputes through the regular Board update pass. Supported operands are
  Circle, Curve, Arc, Sector, and Polygon GeometryElements. The upstream raw
  Point/Coords/coordinate-array `_getPath` forms remain unsupported.
- `JsxGraphJessieCode.parse` and `createSession` expose only the translated
  JessieCode grammar and native creator subset. They do not evaluate arbitrary
  JavaScript, load modules, expose a DOM, or return internal AST/runtime
  values. The stateful session retains globals, functions, closures, selected
  Board state, created elements, and bounded source history, then returns
  portable scene snapshots. Parse, runtime, resource, and scene failures are
  mapped to public `JsxGraphJessieCodeError` values with source ranges where
  available. This preview API is outside the current Stable
  construction-document contract.
- The translated Curve subset accepts two numeric arrays for a discrete data
  plot, four number/string terms for an explicit-domain parametric curve,
  three number/string terms for FunctionGraph/Plot, or two retained terms for
  StepFunction. Matching `src/base/curve.js -> generateTerm`, a JessieCode
  string x-term retains `curveType=functiongraph` even in the four-parent
  `curve(xTerm,yTerm,min,max)` form; direct function-valued x-terms retain
  `curveType=parameter`. StepFunction follows
  `src/base/curve.js -> createStepfunction.updateDataArray`: each regular Board
  update rebuilds its data arrays from the current X-term length and emits
  zero points for empty input or `2n - 1` points otherwise. Missing Y entries
  become non-finite scene path breaks. JSON input accepts numeric arrays;
  native JessieCode also preserves mutable array identity and JavaScript
  function `length`/indexed-property behavior without invoking the functions.
  Its expanded point count is bounded before construction. Continuous curves
  require `doAdvancedPlot: false` and use
  `src/math/plot.js -> updateParametricCurveNaive`, including its right-open
  sampling interval. The default and maximum translated sample counts are
  1,600 and 10,000. Non-finite sampled points become explicit scene path
  breaks. Boolean-composition Curves accept fill styling because their
  Greiner-Hormann output is a closed path. Adaptive plot versions, omitted
  domains, function-valued and mixed-array terms, transformations, polar
  curves, cubic Bezier paths, ordinary Curve fills, non-round caps, arrows,
  labels, and hit testing return structured unsupported or creation errors
  until their upstream slices are translated.
- The translated Polygon subset accepts registered Point references or
  coordinate arrays, closes the vertex list, creates Segment borders in the
  upstream storage and Board-creation orders, and preserves
  existing-versus-owned Point dependencies. `createPolygonalChain` follows
  the upstream wrapper: it removes the duplicate closing vertex and final
  closing Segment, keeps `Area`'s implicit closure, makes `Perimeter`/`L`
  measure only adjacent vertices, and preserves the upstream bounds behavior
  that omits the final chain vertex. Compose renders even-odd fills, default
  border styling, owned helper Points, and an open chain border; a
  PolygonalChain fill still closes geometrically when explicitly enabled,
  matching the official Canvas renderer. Top-level Polygon stroke properties
  are retained on the Polygon scene but do not override its Segment borders,
  matching JSXGraph's separate `polygon.borders` options. Nested
  `vertices`/`borders` attributes, transformations, mutable vertex lists,
  labels, and hit testing remain pending. Polygons can be inputs to translated
  CurveIntersection/CurveUnion/CurveDifference creators. Construction failures
  roll back materialized helper Points and borders instead of leaving
  partially registered elements.
- `createParallelogram` follows
  `src/base/polygon.js -> JXG.createParallelogram`: three
  Point/reference/coordinate parents create a constrained ParallelPoint and
  then a Polygon with vertices
  `[point1, point2, parallelPoint, point3, point1]`. The helper is exposed as
  `parallelPoint`, both output and helper are draggable, and helper `fixed` is
  forced to `false` after nested `parallelpoint` attributes are applied.
  Segment borders retain the official storage order while Board registration
  retains helper-before-borders-before-Polygon creation order. Removing the
  Parallelogram removes the Polygon and borders but leaves the helper
  registered; coordinate-created source Points are owned by that helper and
  are removed only when it is removed. JSXGraph `1.13.3` materializes and
  retains temporary Points when final arity or a later parent is invalid.
  Kotlin validates arity first and atomically removes any temporary Points or
  helper created before a structured factory failure.
- `createRegularPolygon` follows
  `src/base/polygon.js -> JXG.createRegularPolygon`. The numeric form applies
  the upstream angle
  `PI * (2 - (n - 2) / n)` around each preceding vertex and transforms the
  vertex two positions back; JavaScript's `i < n` loop is preserved as
  `ceil(n)` output vertices for finite non-integer `n`. Generated Points keep
  `OBJECT_TYPE_CAS`, a complete nested `vertices.ids` list, and forced
  draggable/non-fixed state. The existing-Point form appends the same
  transforms directly to supplied Points without changing their type,
  parent list, or post-removal transform binding. Coordinate-created starting
  Points remain Polygon-owned, while generated helpers survive Polygon
  removal. Kotlin rejects non-finite numeric counts to prevent the upstream
  positive-infinity loop, rejects duplicate IDs instead of overwriting the
  Board map, and atomically restores existing transforms or removes temporary
  Points on failure. Generated Point labels and complete nested
  `vertices`/`borders` visual semantics remain pending.
- `createRadicalAxis` follows
  `src/base/line.js -> JXG.createRadicalAxis`. Two registered Circles feed the
  exact transposed standard-form matrix product, followed by the upstream
  one-function Line formulas for two constrained homogeneous helper Points.
  The Line is non-draggable, replaces its public parents with the Circle IDs,
  and keeps both helper and Circle dependency links. Helpers survive successful
  Line or Circle removal, matching JSXGraph. Repeated, concentric, and
  identical Circles preserve zero helper coordinates and NaN Line standard
  forms. JSXGraph `1.13.3` overwrites the Board registry for a duplicate output
  ID and leaves duplicate objects; Kotlin preserves the project-wide unique-ID
  invariant, returns `GMResult.Err`, and removes both newly created helpers
  atomically. Nested helper styling and labels remain unsupported in the
  public scene bridge; `id`, `name`, `needsRegularUpdate`, and
  `withLabel: false` are accepted.
- `createEllipse` follows `src/element/conic.js -> JXG.createEllipse`.
  Point/reference/coordinate foci are accepted with either a Point on the
  Ellipse or a numeric/function-valued major-axis parent. The output remains a
  parameter Curve with `OBJECT_TYPE_CONIC`, a constrained midpoint/center,
  foci, `majorAxis`, `subs`, `inherits`, and a live `quadraticform`; optional
  parameter-domain parents are currently numeric-only in native JessieCode.
  The point-on-Ellipse and major-axis forms preserve parent updates,
  coordinate-helper ownership, center/output removal behavior, and upstream
  `Double` propagation for short, zero, negative, non-finite, and coincident
  inputs. Sampling deliberately uses the existing fixed right-open naive
  Curve path with an explicit bounded count; adaptive plotting remains
  pending.
  JSXGraph `1.13.3` may overwrite a Board registry entry for a duplicate
  Ellipse or nested center ID and can retain helpers after a later creator
  failure. Kotlin rejects duplicate IDs and removes newly materialized focus,
  point-on-Ellipse, and center helpers atomically. Invalid function results,
  unsupported parent forms, sample overflow, and scene limits are structured
  failures. Tangent, Polar, PolarLine, PolePoint, TangentTo, Normal,
  Intersection, and OtherIntersection explicitly reject Conic parents until
  those interoperation paths receive their own source-mapped tests and parity
  evidence.
- `createHyperbola` follows
  `src/element/conic.js -> JXG.createHyperbola`.
  Point/reference/coordinate foci are accepted with either a Point on the
  Hyperbola or a numeric/function-valued major-axis parent. The output remains
  a parameter Curve with `OBJECT_TYPE_CONIC`, a constrained midpoint/center,
  foci, `majorAxis`, `subs`, `inherits`, and a live `quadraticform`; optional
  parameter-domain parents are currently numeric-only in native JessieCode,
  and omitted domains preserve the official `±1.0001π` values. Point and
  major-axis forms preserve parent updates, coordinate-helper ownership,
  center/output removal behavior, and upstream `Double` propagation for
  short, equal-focal-distance, zero, negative, non-finite, and coincident
  inputs. Sampling uses the existing fixed right-open naive Curve path with a
  bounded count; adaptive plotting remains pending.
  Kotlin rejects duplicate output or nested center IDs and atomically removes
  newly materialized focus, point-on-Hyperbola, and center helpers. Invalid
  function results, unsupported parent forms, sample overflow, and scene
  limits are structured failures. Tangent, Polar, PolarLine, PolePoint,
  TangentTo, Normal, Intersection, and OtherIntersection continue to reject
  Hyperbola/Conic parents until those interoperation paths receive dedicated
  source-mapped tests and parity evidence.
- `createParabola` follows
  `src/element/conic.js -> JXG.createParabola`.
  The focus accepts a Point/reference, a function returning a Point, or
  coordinates. The directrix accepts a registered Line or an implicit Line
  described by exactly two Point/reference/coordinate terms. The output is a
  parameter Curve with `OBJECT_TYPE_CONIC`; the projection of the focus onto
  the directrix is exposed as `center`, `midpoint`, and `subs.center`, while
  the focus and directrix retain update dependencies and the live
  `quadraticform`. Optional parameter-domain parents are numeric-only, with
  omitted values preserving the official `0..2π` domain and its `π/2`
  non-finite singularity. Degenerate and ideal directrices preserve upstream
  `Double` propagation. Sampling uses the existing fixed right-open naive
  Curve path with a bounded count; adaptive plotting remains pending.
  For an implicit directrix, nested `line` attributes currently support only
  `id`, `name`, and `needsRegularUpdate`; the helper Line is not emitted as a
  top-level source element, and construction-document `line.visible: true`
  is rejected. JSXGraph `1.13.3` can overwrite a duplicate Parabola registry
  ID after materializing coordinate helpers. Kotlin rejects duplicate output,
  center, or implicit-Line IDs and removes newly created focus, directrix
  endpoints, directrix, and center helpers atomically on failure. Tangent,
  Polar, PolarLine, PolePoint, TangentTo, Normal, Intersection, and
  OtherIntersection continue to reject Parabola/Conic parents until those
  interoperation paths receive dedicated source-mapped tests and parity
  evidence.
- `createPolePoint` follows
  `src/base/point.js -> JXG.createPolePoint` for the translated Circle/Line
  parent combination. Parent input order is accepted in both directions and
  canonicalized to Circle then Line, matching the upstream element metadata.
  The output Point evaluates
  `Numerics.det(circle.quadraticform, line.stdform)` through one constrained
  homogeneous coordinate function and keeps direct child links from both
  parents. Direct output removal detaches those links; removing either parent
  recursively removes the PolePoint. A non-finite Line preserves the official
  NaN Point result. Cross-Board, unregistered, invalid-parent, and duplicate-ID
  failures return `GMResult.Err` without partial registration. Ellipse,
  Hyperbola, and Parabola construction are translated, but the upstream
  Conic/Line forms remain explicitly unsupported pending dedicated
  interoperation evidence.
- The translated Circle/Point, Line/Point, and Curve/Point branches of
  `createTangent` follow `src/base/line.js -> JXG.createTangent`, including the
  registered `polar` alias; `createPolarLine` remains restricted to
  Circle/Point. The exact
  `Mat.matVecMult(circle.quadraticform, point.coords.usrCoords)` closure feeds
  the upstream one-function Line formulas for two constrained homogeneous
  helper Points. `tangent` and `polar` keep the supplied parent order and
  `elType=tangent`; `polarline` canonicalizes metadata to Circle then Point
  and changes only `elType`. Matching JSXGraph, only the Point directly owns
  the output child link: removing the Point removes the Line, while removing
  the Circle leaves it registered. Direct Line removal leaves both implicit
  helper Points registered. Center and zero-radius inputs preserve upstream
  zero/NaN or finite degenerate arithmetic. Cross-Board and unregistered
  parents return `GMResult.Err`; unlike JSXGraph's permissive cross-Board
  closure and duplicate-registry overwrite, Kotlin enforces Board ownership
  and unique IDs and atomically removes temporary helpers on failure.
- The Line/Point Tangent branch calls the translated two-Point Line factory
  with the source Line's existing endpoints. The output is therefore
  unconstrained and draggable, shares both endpoint objects and their
  `inherits`/ancestor links, ignores nested `point1`/`point2` identity, and
  changes geometry only when those endpoints move. Matching upstream, neither
  the source Line nor parameter Point receives a direct child link even though
  both remain in the public `parents` metadata. Removing either leaves the
  Tangent registered; removing either shared endpoint removes both source and
  Tangent Lines. Official cross-Board construction with a foreign parameter
  Point succeeds while a foreign source Line fails with a `TypeError`; Kotlin
  deliberately rejects both as `ParentBoardMismatch`.
- The Curve/Point Tangent branch follows all three translated Curve modes.
  FunctionGraph evaluates numerical derivatives at `point.X()`. A direct
  function-valued parametric Curve first projects the Point through
  `Geometry.projectCoordsToCurve` and differentiates at the nearest
  parameter. A data Plot projects to the nearest degree-one segment and uses
  that segment's homogeneous cross product. The output uses two hidden
  constrained helper Points and preserves the supplied parent order, while
  only the parameter Point owns the direct Tangent child edge. Removing the
  Tangent or Curve leaves the helpers and the remaining objects registered;
  removing the Point removes the Tangent but leaves the helpers. Duplicate
  Plot points preserve zero-helper/NaN-Line arithmetic. A one-point Plot,
  projection failure, cross-Board parent, or duplicate ID returns a structured
  error with atomic rollback. Ellipse, Hyperbola, and Parabola Tangent/Polar
  and PolarLine Conic forms remain explicitly unsupported pending dedicated
  evidence, as do Turtle and one-parent Glider branches.
- The Circle branch of `src/base/line.js -> JXG.createTangentTo` is translated
  as the same three-stage construction: create the external Point's polar,
  intersect that Line with the Circle, then create the Tangent at the selected
  contact Point. An omitted, zero, or `NaN` numeric third parent selects branch
  zero; every other `Double`, including either infinity, selects branch one,
  matching the upstream truthiness test. The returned Line exposes the
  registered polar and contact Point and preserves the observed dependency,
  update, removal, non-real, and degenerate behavior. Nested tangent/polar
  helper identities and contact
  Point identity/fixed state are retained. Public scene generation expands one
  `tangentto` source object into the polar Line, contact Point, and tangent
  Line, and charges all three against object limits. Kotlin rejects
  cross-Board, unregistered, duplicate-ID, and partial-stage failures with
  structured atomic rollback. Ellipse, Hyperbola, and Parabola construction
  are translated, but the upstream TangentTo Conic branch remains explicitly
  unsupported pending dedicated interoperation evidence.
- `createDerivative` preserves the upstream runtime construction:
  `X(t)` delegates to the source Curve and `Y(t)` divides the central
  `Numerics.D` result for `Y` by that for `X`. The output remains
  `curveType=parameter`, captures the source domain, and records only public
  parent metadata; it intentionally does not add a source child edge, so
  removing the source leaves the Derivative registered with its retained
  source reference. Data Plots use the translated
  `interpolationFunctionFromArray` behavior and a viewport-padded default
  domain. Kotlin accepts exactly one registered Curve parent and returns a
  structured error for all invalid forms; JSXGraph `1.13.3` instead exposes
  inconsistent `TypeError`/generic `Error` paths because its guard combines
  `parents.length !== 1` with `parents[0].class`. Adaptive plotting remains
  unsupported, so public sources must continue to request
  `doAdvancedPlot: false`.
- `createSpline` and `createCardinalSpline` preserve the upstream natural and
  cardinal interpolation functions, dynamic Point/tension updates, and
  distinct parent lifecycles. Spline's Point parents are metadata only;
  existing CardinalSpline Points own the Curve, while coordinate-generated
  Points are Curve children. The Spline coordinate-pair parent branch
  deliberately retains the upstream nested-loop behavior that replays the
  complete parent list for every pair, including its duplicated points and
  resulting non-finite interpolation. Fewer than two interpolation points are
  rejected as `InvalidInterpolationPointCount` before registration instead of
  allowing a later invalid Curve. Coordinate functions, dynamic tension,
  `createPoints`, `isArrayOfCoordinates`, uniform/centripetal/chordal
  parameterization, and bounded sample counts are translated; adaptive
  plotting remains unsupported.
- `createRiemannsum` accepts one numeric function or an ordered lower/upper
  pair, evaluates dynamic rectangle count/type/interval terms on every regular
  update, and exposes the cached signed area through `Value()` and `V()`.
  Construction-document function expressions are strings as required by the
  repository's serialization contract; native JessieCode uses actual function
  values. Output is bounded using the exact ordinary/Simpson point counts
  (`5n`, `34n`, or `63n`). A nonnumeric/throwing function or an oversized
  dynamic count returns a structured error and does not retain a partially
  registered Curve; JSXGraph `1.13.3` can leave that Curve registered after
  throwing during its first `updateDataArray`. Numeric approximation types
  retain the upstream unknown-type fallback. Ordinary Curve fills remain
  unsupported; fill is enabled only for Boolean-composition, RiemannSum,
  BoxPlot, and Inequality Curves.
- `createBoxPlot` accepts at least five numeric/string/function quantile terms,
  dynamic numeric/string/function axis and width terms, and an optional sixth
  array-valued outlier term. It preserves the upstream 19-point body/whisker
  path, vertical-versus-other-direction transposition, `smallWidth`, all
  documented outlier face aliases, unknown-face circle fallback, blue fill
  defaults, and JessieCode dependency/removal behavior. The public scene keeps
  BoxPlot source geometry so Compose can convert `outlier.size` from CSS pixels
  using the actual viewport `unitX`/`unitY`, rather than freezing the size at
  construction time. Output growth is bounded as `19`, `20 + 3n`, `20 + 6n`,
  or `20 + 19n` according to face and outlier count. JSXGraph `1.13.3` can
  register a partial Curve before failing on non-array quantiles, object terms,
  or initial evaluation; Kotlin validates and evaluates first, then returns a
  structured error atomically. Function-valued visual attributes remain
  outside the current public source contract.
- `createComb` accepts two existing Point references/names or coordinate
  arrays and preserves the upstream tooth loop, including cumulative
  floating-point stepping, final-tooth clipping, and `NaN` path breaks.
  `frequency`, `width`, and `angle` accept numbers or native JessieCode
  functions; `reverse` accepts a Boolean or function. Function-returning
  Point/coordinate parents remain unsupported. Coordinate parents create
  hidden, non-fixed helpers with nested `point1`/`point2` identity and fixed
  settings; successful helpers survive Curve removal as upstream does.
  JSXGraph `1.13.3` leaks the first helper when the second parent is invalid,
  while Kotlin validates both parents first and cleans all helpers after any
  later failure. Non-positive/non-finite frequency returns
  `InvalidCombFrequency` instead of entering the upstream unbounded loop, and
  static/dynamic tooth growth is capped before allocation.
- `createInequality` accepts the first registered Line or FunctionGraph parent
  and ignores extra parents, matching JSXGraph `1.13.3`. Line inputs preserve
  the five-point expanded half-plane polygon derived from the current Board
  bounds. FunctionGraph inputs preserve each finite run, its original
  `minX`/`maxX` closure, and `NaN` separators. `inverse` accepts a Boolean or
  native JessieCode function and is reevaluated during regular updates.
  Parents are metadata only and do not own a child edge, so source removal
  leaves the Inequality registered. Unsupported source types, non-Boolean
  `inverse`, and output above the configured point limit return structured
  errors. The Kotlin intrinsic cap additionally prevents an unbounded dynamic
  allocation before the public engine applies its configured limit.
- `createVectorField` accepts the upstream component-function and
  array-returning-function forms, including JessieCode strings, dynamic
  three-term meshes, scale, and nested arrow enable/size/angle. Inclusive
  fractional, zero, and negative step behavior, `NaN` path breaks, and
  arrowheads only for nonzero vectors follow JSXGraph `1.13.3`. Direct
  function references do not create geometry child edges, so removing a Point
  referenced only by a closure leaves the VectorField registered. Static and
  dynamic output growth is capped before allocation. Because the public scene
  is viewport-independent, it retains the resolved vectors and arrow settings;
  Compose applies the upstream CSS-pixel arrow size using its actual
  `unitX`/`unitY`. Unsupported nested arrow fields remain visible to the
  structured scene-attribute validator.
- `createSlopeField` accepts one scalar JessieCode string or native function
  and wraps the VectorField path with the exact upstream
  `[1 / sqrt(1 + z * z), z / sqrt(1 + z * z)]` normalization. This preserves
  `NaN` and infinite-slope arithmetic instead of substituting a vertical
  vector. Arrowheads default to disabled; explicit dynamic arrow, scale, and
  mesh terms use the same bounded, atomic behavior as VectorField. Function
  dependencies remain metadata-only. The public runtime does not yet expose
  either field type's `setF` mutation method.
- `createNormal` translates registered Line/Point, Circle/Point, and
  Curve/Point parents in either order. The Line branch preserves the ideal
  direction helper, `point` and `subs.point` access, and the duplicated helper
  entry in `inherits`; the Circle branch reuses its midpoint and the supplied
  Point. FunctionGraph, true parametric Curve, and degree-one data Plot
  branches use the upstream derivative or nearest-projection coefficients and
  two hidden constrained endpoints. Both explicit parents own the output child
  edge, so removing either parent removes the Normal, while removing the
  Normal leaves its hidden helpers registered. JSXGraph `1.13.3` accepts a
  coordinate-array Point far enough to create cyclic helper ownership and then
  throws `RangeError: Maximum call stack size exceeded`; Kotlin rejects that
  form as a structured unsupported-parent result. Cross-Board, unregistered,
  one-point Plot, unsupported-degree, projection, and duplicate-ID failures
  are also structured and roll back new helpers atomically. Glider, Turtle,
  transformed-Curve, degree-three Plot/Bezier, and
  Ellipse/Hyperbola/Parabola Conic branches remain unsupported. The Conic
  branch requires dedicated
  interoperation evidence rather than implicitly entering the generic Curve
  path.
- The translated Arc and Sector subsets accept three registered Point
  references or coordinate arrays and preserve existing-versus-owned Point
  dependencies. Arc additionally accepts four Point parents when
  `useDirection` is `true`; the fourth Point participates in dependency
  updates and selects the endpoint order with the upstream determinant.
  They support `auto`/`minor`/`major` selection, clockwise and
  counterclockwise orientation, and the upstream four-segment cubic Bezier
  approximation. Angle supports the three-point form, parent reordering,
  numeric or `auto` radius, and `sector` display. Two-line Sector/Angle forms,
  Sector/Angle `useDirection`, visible/styled sub-elements, labels,
  transformations, arrows, hit testing, Angle mutation, and
  `square`/`sectordot`/`none` display are rejected or remain unavailable. A
  right Angle whose effective `orthoType` is not `sector` is rejected instead
  of silently drawing a sector.
- The translated Arc-composition subset follows `src/element/arc.js`
  `createSemicircle`, `createCircumcircleArc`, `createMinorArc`, and
  `createMajorArc`. Semicircle accepts two Point/reference/coordinate parents
  and owns a hidden Midpoint center. CircumcircleArc accepts three such parents,
  owns a hidden Circumcenter, and passes the third source Point through the
  direction-point branch. MinorArc and MajorArc preserve the source center and
  endpoints while forcing the corresponding selection mode. Dependency
  updates, helper ownership/removal, registration rollback, and upstream
  degenerate arithmetic are translated. The composition-specific nested
  attribute groups and the remaining Arc mutation/rendering surface remain
  pending. This focused subset is outside the `0.1.0` Stable corpus.
- The translated Sector-composition subset follows `src/element/sector.js`
  `createCircumcircleSector`, `createMinorSector`, `createMajorSector`,
  `createNonreflexAngle`, and `createReflexAngle`. CircumcircleSector accepts
  three Point/reference/coordinate parents, owns a hidden Circumcenter, and
  passes the third source Point through the four-parent direction-selection
  path. MinorSector and MajorSector preserve the source center and endpoints
  while forcing the corresponding selection mode. NonreflexAngle and
  ReflexAngle force minor/major selection and expose the upstream
  radian-default, unit-aware `Value()` behavior. Dependency updates, helper
  ownership/removal, registration rollback, and upstream degenerate
  arithmetic are translated. Composition-specific nested attribute groups and
  the remaining Sector/Angle mutation and rendering surface remain pending.
  This focused subset is outside the `0.1.0` Stable corpus.
- The translated `JXG.Composition` subset is a non-Board container with
  role-based membership, ID/name selection, ordered objects, `subs`, type
  access, generated update lifecycle, parent forwarding, member removal, and
  Board-driven deletion of its registered members. Generic group/filter
  selection and generated forwarding for untranslated element APIs remain
  pending. In particular, upstream `Composition.setAttribute` forwards a
  visual mutation to every member; Kotlin returns no such callable property
  until the mutable visual-property model exists.
- Native `bisectorlines` follows
  `src/element/composition.js -> createAngularBisectorsOfTwoLines`: two
  registered output Lines are backed by four hidden constrained Points and
  carry the two source Lines as parent metadata without adding source-child
  links. Only nested `line1` and `line2` creator attributes style the outputs.
  An observed top-level `layer: 5` remains on the Composition attributes and
  both outputs retain the Line default layer `7`, which Kotlin preserves.
  JSXGraph `1.13.3` silently accepts duplicate nested output IDs and leaves an
  ambiguous Board registry; Kotlin instead returns the existing structured
  `DuplicateElementId` failure and atomically removes all partially created
  helpers and outputs. Collapsed source Lines preserve upstream `NaN`
  propagation. This focused subset remains outside the `0.1.0` Stable corpus.
- The translated Text subset accepts numeric or string coordinates, static
  string/number content, and numeric JessieCode expressions inside upstream
  `<value>...</value>` tags. It preserves the upstream short-math expansion,
  dependency updates, `digits` formatting, `setText`, font size, stroke
  color/opacity, and left/middle/right plus top/middle/bottom anchors. Only
  `px` font units are accepted. Function-valued content, nonnumeric value-tag
  results, rich-text subscript/superscript and GEONExT conversion,
  MathJax/KaTeX/ASCIIMath, fractions, element anchors, nonzero rotation,
  measured bounds, and hit testing return structured errors or remain
  unavailable. Compose always draws text on Canvas; accepted `html` and
  `internal` display modes are equivalent for this plain-text subset.
- JessieCode tokenization and the translated expression parser return
  `GMResult.Err` when configured source-length, token-count, AST-node, or
  AST-depth limits are exceeded. The upstream generated lexer and Jison parser
  have no resource limits. A separate parser-nesting limit is capped at 64 so
  malformed recursive syntax is rejected before exhausting the browser Wasm
  stack.
- The translated parser currently accepts an empty program or an expression
  statement list, including empty statements, blocks, `if`/`else`, and
  `while`/`do`/`for` loops. Its expressions include right-associative
  assignment, conditionals, literals, variables, arrays, objects, calls,
  properties, indexes, function/map expressions, and unary/binary precedence.
  Successful supported input preserves the upstream AST node/value/child
  shape, `isMath` flags, dangling-else behavior, loop evaluation order,
  function parameter arrays, creator attribute lists, and generated-action
  locations, including the deprecated multi-Board `use` statement.
- JSXGraph `1.13.3` stores string and numeric object-literal property AST nodes
  directly as JavaScript object keys. JavaScript coerces each of those nodes to
  `"[object Object]"`, so such keys collide while identifier keys behave
  normally. The Kotlin runtime preserves that observable behavior instead of
  normalizing literal property names.
- JessieCode evaluation returns structured `GMResult.Err` values for malformed
  ASTs, invalid assignment targets, unsupported operand combinations,
  unavailable element properties or values, and non-callable values instead
  of propagating JavaScript exceptions. Evaluation is capped by node-step,
  depth, and collection-size limits; the maximum configurable recursive depth
  is 64 to stay below the browser Wasm stack limit.
- Assignment preserves upstream right associativity and resolves a property or
  index receiver before evaluating the right-hand value. Arrays and objects
  retain mutable identity. Kotlin materializes JavaScript sparse-array holes
  as `UndefinedValue`, preserving observable translated indexing and `length`
  behavior while using bounded storage. Element targets delegate to
  `JessieCodeElementRuntime.assignProperty`; unsupported writes return
  `ElementPropertyAssignmentUnavailable`.
- The translated `setProp` subset supports exact uppercase Point `X`/`Y`
  assignment from numbers or strings, plus case-insensitive element `name`
  and `needsRegularUpdate` attributes. Numeric coordinates on free Points use
  `setPosition`; string coordinates and writes to already constrained Points
  atomically compile and evaluate replacement coordinate functions before
  changing dependency ownership. Kotlin stores numeric constraint origins as
  JessieCode number sources. Function-valued coordinate writes, non-string
  `name` values, Text coordinates, method-mapped fields, and visual-property
  fallback remain pending and return structured errors instead of propagating
  JavaScript exceptions or silently accepting an unsupported property.
- Standalone `JessieCodeEvaluator.evaluate` calls retain isolated locals.
  `JessieCodeSession` explicitly opts into the upstream persistent-instance
  behavior: globals, function scopes, nested closure chains, selected Board,
  and stored source survive across parse calls. Evaluation step and depth
  budgets reset at each top-level session call, and stored source has an
  explicit length limit instead of growing without a bound.
- The deprecated `use IDENTIFIER` statement switches all subsequent Board
  lookup, `$board`, deletion, function-dependency, and creator operations
  within the evaluator invocation. Since KMP has no global DOM container
  registry, callers expose the same lookup explicitly through
  `boardsByContainer`. An unknown entry returns `BoardNotFound`; an explicit
  `JessieCodeSession` retains the selected Board for later parse calls.
- Loops use the evaluator's existing node-step limit as their execution budget.
  JSXGraph has no corresponding bound and can run an infinite loop.
- `return` preserves the JSXGraph `1.13.3` interpreter behavior: it evaluates
  to its child (or numeric zero for bare `return;`) but does not terminate an
  enclosing statement list, including inside a translated function body.
- Function and map values preserve the interpreter's reusable mutable scope
  per function definition. Parameters overwrite that same scope on each call,
  missing parameters receive `UndefinedValue`, duplicate names are written in
  order, and nested closures therefore observe the latest captured parameter
  values just as in JSXGraph `1.13.3`. Recursive calls share the evaluator's
  depth and step budgets. Invalid map bodies return `InvalidMapBody` instead
  of throwing the upstream runtime exception.
- Creator attribute expressions are evaluated before parent expressions and
  merged recursively from left to right with lower-case keys, matching
  `Type.deepCopy(..., true)`. As upstream does, an assignment target becomes
  the creator's implicit `name` when neither `name` nor `id` is supplied.
  Kotlin exposes custom creators through the explicit `JessieCodeCreator`
  adapter and gives them precedence over the native `point`, `line`, `arrow`,
  `segment`, `circle`, `ellipse`, `hyperbola`, `parabola`, `circumcenter`,
  `circumcirclemidpoint`, `circumcircle`, `midpoint`,
  `reflection`, `mirrorelement`, `mirrorpoint`, `orthogonalprojection`,
  `perpendicularpoint`, `perpendicular`, `perpendicularsegment`,
  `parallelpoint`, `parallel`, `arrowparallel`, `bisectorlines`, `bisector`,
  `incenter`, `incircle`, `curve`, `functiongraph`, `plot`, `stepfunction`,
  `derivative`, `spline`, `cardinalspline`, `riemannsum`, `boxplot`, `comb`,
  `inequality`, `vectorfield`, `slopefield`,
  `polygon`, `polygonalchain`, `parallelogram`, `regularpolygon`,
  `radicalaxis`, `polepoint`, `tangent`, `polar`, `tangentto`, `polarline`,
  `text`, `arc`,
  `semicircle`,
  `circumcirclearc`, `minorarc`, `majorarc`, `sector`, `circumcirclesector`,
  `minorsector`, `majorsector`, `angle`, `nonreflexangle`, and `reflexangle`
  registry. Attributes on an ordinary function return
  `UnexpectedCreatorAttributes` instead of throwing, and attribute
  nesting/collection growth shares the evaluator resource limits.
- Native JessieCode creators currently apply `id`, `name`, and
  `needsRegularUpdate`; Curve creators additionally consume their translated
  plotting attributes, Polygon consumes `withLines`, Text consumes `parse`,
  `formatNumber`, and `digits`, Arc/Sector consume `selection` and
  `orientation`, Arc additionally consumes `useDirection`, Angle also
  consumes `radius`. Arc and Sector compositions consume the translated
  common/shape attributes while forcing their upstream composition semantics.
  Parallelogram consumes nested `parallelpoint` identity and Point styling,
  while preserving the upstream forced draggable/non-fixed helper state.
  RegularPolygon consumes nested `vertices` identity, `ids`, and translated
  Point styling while likewise forcing generated helpers draggable/non-fixed.
  RadicalAxis consumes nested `point1`/`point2` identity and regular-update
  attributes while retaining hidden constrained helper Points.
  Ellipse, Hyperbola, and Parabola consume nested `center` identity, fixed
  state, and regular-update attributes plus their translated Curve
  sampling/style fields. Parabola additionally consumes nested implicit-Line
  identity and regular-update attributes; its focus uses the existing nested
  `foci` Point attributes.
  Circle/Point and Curve/Point Tangent and Polar, plus PolarLine, consume the
  same nested helper identity and regular-update attributes for their hidden
  constrained endpoints. Line/Point Tangent and Polar ignore those nested
  attributes because they reuse the source Line endpoints, matching upstream.
  TangentTo consumes the top-level tangent endpoint identities, nested polar
  Line and endpoint identities, and the contact Point identity/fixed state.
  BoxPlot consumes `dir`, `smallWidth`, and nested outlier `face`/`size`.
  Comb consumes numeric/function-valued `frequency`, `width`, `angle`, and
  `reverse`, plus nested hidden `point1`/`point2` identity and fixed settings.
  Inequality consumes Boolean/function-valued `inverse`.
  VectorField and SlopeField consume numeric/function-valued `scale` and
  nested `arrowhead.enabled`/`size`/`angle`.
  The public scene bridge snapshots bounded literal creator attributes and
  applies the same supported top-level style, visibility, line-end, Point,
  static Line arrow-head, Curve, Polygon, Text, Arc, Sector, and Angle fields
  as the construction-document path.
  Function, Board, element-reference, cyclic, and over-depth attribute values
  fail explicitly. Nested element styling and later visual-property mutation
  remain pending. Invalid supported attribute types, unavailable Boards,
  unsupported parent combinations, and native factory failures return
  structured errors instead of throwing.
- The debug-only official JSXGraph iframe allows CSP `unsafe-eval` because
  upstream JessieCode compiles string curve expressions through JavaScript
  evaluation. That permission is confined to the comparison renderer;
  `jsxgraph-core` and `jsxgraph-compose` contain no JavaScript engine or
  WebView dependency. The visual capture harness treats upstream
  `error compiling function` console messages as failures.
- The comparison workbench accepts a strict debug-only JSON envelope for a
  JessieCode case. The same raw `source` and Board options drive native
  `JsxGraphJessieCodeSession.execute` and official `board.jc.parse`; unknown
  fields, arbitrary-JavaScript input kinds, invalid bounds, and non-Boolean
  Board flags are rejected. This envelope is not accepted by either production
  source API.
- The deprecated `delete` statement removes a resolved geometry element
  through the translated `Board.removeObject` lifecycle and returns
  `UndefinedValue`. Kotlin does not emit the upstream deprecation warning
  because the core runtime has no logging side channel.
- JessieCode geometry values cross the interpreter through
  `JessieCodeElementRuntime`. This preserves board object identity while the
  full upstream `methodMap`, visual-property, and generic `Value()` contracts
  are still untranslated.
- Static JessieCode dependency discovery preserves the upstream reverse child
  traversal and direct-name / `$()` / `$value()` lookup behavior. It uses an
  explicit traversal stack and returns `GMResult.Err(MissingExplicitElement)`
  for an unknown literal `$()` or `$value()` ID; upstream stores an
  `undefined` dependency and fails later when that dependency is attached.
- JessieCode name replacement preserves stable element-ID calls, slider value
  calls, predefined constants, and reverse child traversal. `replaceIDs`
  restores the current non-empty board name, including names changed after
  compilation, while preserving the stable call for unnamed or removed
  elements. Both directions return structured node/depth-limit errors instead
  of traversing without resource limits. A malformed node carrying the
  internal `replaced` marker returns `InvalidReplacedNode`; upstream throws
  while indexing the assumed replacement-call shape. A direct variable
  assignment target remains local even when a Board element has the same
  name; property and index receivers are still replaced. Static dependency
  discovery preserves the upstream behavior of also recording a same-named
  Board element for a direct variable target.
- `JessieCodeExpressionFunction` covers the string branch of
  `Type.createFunction`, including argument binding, stable references, and
  dependency metadata. It accepts at most one expression statement, including
  a single assignment expression; a multi-statement source returns
  `MultipleStatements`, while a block, branch, loop, return, or delete
  statement returns `UnsupportedStatement`. Direct Kotlin number, array, and
  function adapters are deferred until a translated caller needs those parent
  forms.
- The core element runtime exposes the translated `methodMap` subset for
  coordinate elements, lines, circles, polygons, Text, and common element
  names, plus the bounded writable subset described above. `Bounds` and
  `addChild` preserve the translated element return values. `move` and
  `moveTo` accept numeric two- or three-coordinate arrays when the duration is
  omitted or zero, Point `addConstraint` accepts arrays of number/string
  terms, and Text `setText` atomically compiles supported replacement content.
  Nonzero movement durations return `ElementMethodUnavailable` until the
  animation scheduler exists; function-valued constraints, remaining mutating
  methods, visual-property fallback, generic `Value()`, and untranslated
  element classes return structured unavailable-property/value errors.
- The translated JessieCode built-ins now include coordinate access,
  Line/Circle/Polygon measurements, names, angles, binomial/GCD, `randint`,
  `IfThen`, recursive `eval`, and `remove`. `V`/`Value` delegates to
  `JessieCodeElementRuntime` until Slider/Glider exists. Area and perimeter
  accept Circle and Polygon. `randint` uses an injectable `RandomSource`; its
  default remains nondeterministic.
- JSXGraph `1.13.3` registers `Mat.lcm` and `Mat.ratpow` as unbound built-ins.
  Their ordinary nonzero paths therefore throw `this.gcd is not a function`;
  Kotlin preserves that observable defect as `BuiltInInvocationFailure`.
  The upstream early-return cases (`lcm` with a zero product, `ratpow` with a
  zero numerator or denominator) remain available. Recursive `eval` adds the
  existing step, depth, and collection-size limits and reports cyclic arrays
  as a structured failure instead of exhausting the runtime stack.
  Unsupported built-ins `import`, `$log`, and `D` remain pending.
- Parser errors retain both the offending token location and the previous
  shifted-token location used by Jison's error hash. Expected-token lists
  describe the translated grammar subset rather than the complete generated
  LALR state.
- JessieCode's recognized token stream, one-based lines, zero-based columns,
  `INVALID` fallback tokens, and rule-order quirks are preserved, including
  JSXGraph `1.13.3` splitting `!=` into `!` and `=`.
- Numeric vectors and matrices use `DoubleArray` and `Array<DoubleArray>`.
  Malformed dimensions are outside the internal contract and may fail
  differently from malformed JavaScript arrays.
- `Transformation` covers the numeric and dynamic 2D kernel and the 4x4
  `setMatrix3D` kernel. Unsupported type names, wrong parameter counts,
  malformed matrix shapes or vector dimensions, malformed reflection
  coordinates, JessieCode compile/evaluation failures, nonnumeric expression
  results, and rejected structured function parameters return `GMResult.Err`
  instead of throwing or failing while indexing. `bindTo`, static `meltTo`,
  coordinate-element transformation lists, transformed-position preimages,
  the dedicated JessieCode Transformation runtime reference, the native and
  construction-document `transform` creator paths, and transformed-Point
  creation are translated.
- JSXGraph `1.13.3` returns `null` from `Transformation.clone()` for dynamic
  matrices, but `meltTo()` still appends that value and fails on the next
  element update. Kotlin returns
  `TransformationError.DynamicMeltUnsupported` without mutating the target.
  Dynamic transforms remain available through `bindTo`, which is the working
  upstream route. Array-backed `affinematrix`/`matrix` transforms and
  array-centered rotations retain the upstream nonnumeric classification even
  when all entries are numbers. Transformed Text/Image rendering remains
  pending.
- The bounded
  `GeometryElement3D`/`View3D`/`Point3D`/`Line3D`/`Plane3D`/`Curve3D`/
  `Circle3D`/`Face3D`/`Polygon3D`/`Polyhedron3D` lifecycle and the
  ordinary-Curve Mesh3D factory follow
  `src/3d/element3d.js`, `src/3d/view3d.js`, `src/3d/point3d.js`,
  `src/3d/linspace3d.js`, `src/3d/curve3d.js`, `src/3d/circle3d.js`,
  `src/3d/surface3d.js`, `src/3d/box3d.js`, `src/3d/polygon3d.js`,
  `src/3d/face3d.js`, `src/3d/polyhedron3d.js`, and `src/math/tiling.js`.
  Kotlin preserves parallel and central projection, numeric and homogeneous
  coordinates, function reevaluation, 3D transformation binding, `applyOnce`,
  ordinary 2D Point/Segment/Curve proxies, cube clamping, Line3D endpoint
  clipping, Plane3D Hesse normals and finite outlines, fully infinite
  plane/box clipping, registration, dependency updates, and removal. Malformed
  View3D dimensions, malformed 3D parents, duplicate IDs, and
  coordinate/transformation evaluation failures cross public boundaries as
  `GMResult.Err` instead of throwing from array indexing or dynamic JavaScript
  calls.
- JSXGraph renders and drags Point3D through an ordinary Point proxy. Kotlin
  keeps the same owner/proxy lifecycle but materializes the proxy coordinates
  directly into the platform-independent scene, so Compose requires no 3D
  renderer or platform-specific runtime. Line3D uses projected Point3D
  endpoints and a Segment proxy. Plane3D materializes its source-mapped
  outline Curve and, for finite wireframe ranges, owns the upstream Mesh3D
  Curve. Finite non-wireframe Plane3D modes replace the outline data with an
  owned Polyhedron3D using the exact upstream rectangle or triangle tiling;
  cyclic color arrays, HSL shaders, height colormaps, dynamic vertices, and
  Axes3D rear-plane defaults are preserved. Direct and Plane-owned Mesh3D
  preserve 3D/4D point input, normalized directions, dynamic ranges, both
  sampled line families, NaN path separators, and official default style.
  Kotlin rejects non-positive/non-finite step widths and grids above the
  bounded Curve point, surface vertex, or surface face budgets before entering
  the sampling loops; upstream JavaScript can instead loop indefinitely or
  exhaust memory.
- Face3D preserves the shared Polyhedron vertex maps, face closure rule,
  normals, projected coordinates, averaged `zIndex`, cyclic
  `fillColorArray`, per-face overrides, HSL angle/depth shaders, and ordinary
  Curve proxy lifecycle from `src/3d/face3d.js`. Polyhedron3D preserves direct
  and transformed parent forms, Point-backed, function-valued, and
  homogeneous vertices, face ownership, update and transform forwarding,
  removal, and `toSTL` output from `src/3d/polyhedron3d.js`. Kotlin adds
  bounded vertex/face/curve-point checks and atomic rollback. Scene assembly
  sorts contiguous faces within each Polyhedron by ascending local `zIndex`;
  global View3D `depthOrder` and layer redistribution remain pending.
- Circle3D preserves its Point3D center, three-/four-value and array-function
  normals, numeric and function-valued absolute radii, normalized normal,
  orthogonal frame construction, exact `0..2π` Curve3D sampling, and ordinary
  Curve proxy from `src/3d/circle3d.js`. Dynamic center, normal, and radius
  changes recompute the frame and sampled proxy through the regular Board
  update pass. Kotlin reports malformed normals and rejected dynamic values
  through `GMResult.Err`; update-time failures expose `NaN` proxy coordinates
  instead of throwing through scene construction. Parametric projection
  delegates to the source-mapped Curve3D/COBYLA path.
- IntersectionCircle3D preserves
  `src/3d/circle3d.js -> createIntersectionCircle3D` and
  `src/math/geometry.js -> intersectionFunction3D` dispatch for Plane/Sphere
  in either order and Sphere/Sphere. It owns the hidden dynamic center,
  reuses Circle3D's Curve3D/Curve proxy, preserves canonical parent metadata
  and dependency edges, and hides disjoint results through the upstream
  `NaN` radius. Kotlin rejects unsupported parent pairs and rolls back helper
  creation through `GMResult.Err`.
- Surface3D preserves vector-function and three-component-function sources,
  FunctionGraph3D's scalar-to-vector wrapper, dynamic parameter ranges,
  transformed surfaces, exact row/column wireframe sampling with NaN
  separators, and the rectangle/triangle topology from `src/math/tiling.js`.
  Filled modes reuse Polyhedron3D and Face3D for cyclic color arrays, HSL
  shaders, height colormaps, and local depth ordering. Kotlin bounds steps,
  generated curve points, vertices, faces, and face vertices before
  allocation, returns factory and evaluation failures through `GMResult.Err`,
  and translates `projectCoords` through the shared source-mapped COBYLA
  optimizer.
- Polygon3D preserves direct existing-Point3D, coordinate-array,
  scalar-function, and array-function vertices, open 3D vertex storage, the
  closed ordinary Polygon proxy, generated-versus-external ownership, nested
  `vertices`/`borders` styles, centroid depth, updates, and removal from
  `src/3d/polygon3d.js`. The transformed branch intentionally preserves the
  upstream `base.vertices.length - 1` loop, including its dropped final base
  vertex. JSXGraph `1.13.3` then fails while registering the first transformed
  Point3D with
  `TypeError: Cannot set properties of undefined (setting 'jxgBoard1point3d219')`.
  Kotlin does not reproduce that partial-Board corruption: it returns a usable
  transformed Polygon3D when construction succeeds and reports validation or
  factory failures through `GMResult.Err`, with atomic cleanup of generated
  Point3D and Polygon proxy elements.
- `src/3d/box3d.js` `createAxis3D` is translated as the upstream Line3D
  wrapper. The core `Axes3D` composition creates the three main axes where
  applicable, six axis planes, and twelve face axes for `center`, `border`,
  and `none`; `border` also creates its three Ticks3D curves and numeric
  Text3D labels. View3D factory creation owns this composition and removes it
  with the View. JSON and JessieCode expand explicit Axes3D and automatic
  `center`/`border`/`none` axes into scene elements, including the official
  visible 10-by-10 rear shader planes. The centered `O`
  preserves the JSXGraph `1.13.3` fallback through the Line3D default
  `stdform`: it is a hidden, non-real `intersection` at homogeneous
  `[0,0,0]`, parented by the x/y Line3D members.
- JSXGraph `1.13.3` leaves both
  `Type.copyMethodMap(JXG.View3D, { /* TODO */ })` and
  `Type.copyMethodMap(JXG.Point3D, { /* TODO */ })` empty. Kotlin therefore
  registers the upstream `view3d`, `point3d`, `line3d`, `plane3d` wireframe
  and finite surface, `curve3d`, `circle3d`, `intersectioncircle3d`,
  `sphere3d`, `surface3d`,
  `functiongraph3d`, `mesh3d`, `axis3d`, `polygon3d`, `polyhedron3d`, and
  `transform3d` creator routes but
  does not invent JessieCode `view.create(...)` or Point3D `X`/`Y`/`Z`
  property access that the baseline does not expose. Camera controls, Point3D
  gliders and animations, and the remaining 3D APIs are still pending.
- JSXGraph `1.13.3` requires four `affine` parameters but calls
  `Type.createEvalFunction` with a count of nine, which fails while reading the
  fifth missing parameter. Kotlin implements the documented 2x2 affine matrix
  from the four entries that the upstream `update` function reads.
- JSXGraph `1.13.3` creates six evaluators for the 16-parameter 3D `generic`
  transform and then reads all 16. Kotlin returns
  `TransformationError.UpstreamEvaluationDefect` and keeps the previous
  matrix atomic instead of exposing the upstream partially written matrix.
- JSXGraph `1.13.3` accepts extra 3D `rotate` and `rotateX/Y/Z` parameters.
  When the delegated `rotate` parent count is not exactly three, its center
  branch is skipped. Kotlin preserves that permissive count and ignored-center
  behavior.
- `EventEmitter` passes the registered context as an explicit callback
  argument because Kotlin has no dynamic JavaScript `this`.
- `Board.setId` returns `GMResult.Err(DuplicateElementId)` for an explicitly
  duplicated id instead of silently replacing the previous `objects` entry.
  Generated-id collisions use deterministic increasing suffixes instead of
  JSXGraph's random suffix, while preserving uniqueness and creation order.
- Automatic element names and `elementsByName` registration are finalized
  after a successful `Board.setId` call instead of in the raw element
  constructor. The public factory is not translated yet; this ordering avoids
  leaving an orphaned name when Kotlin rejects a duplicate explicit id.
- The translated `Board.select` overloads cover direct elements and ID/name
  strings. Unknown and empty strings return `null` instead of the unchanged
  input string. Group lookup and function/object filter `Composition` results
  remain pending with those untranslated models.
- `CoordsElement.setPositionDirectly` covers free and persistently transformed
  elements, including inverse-composite preimage recovery. A singular
  composite returns
  `CoordinateTransformationError.NonInvertibleCompositeMatrix` from the
  explicit result API instead of failing in matrix-vector multiplication.
  Relative coordinates remain pending. Snap-to-grid, snap-to-point, and
  attractor calls are lifecycle hooks with no-op defaults until the
  visual-property and attractor models are translated.
- `Point.create` accepts numeric free-point coordinates, a list of at least
  two JessieCode string coordinate expressions, mixed
  numeric/string/function terms, or one function returning a numeric
  coordinate array. Runtime functions are called without arguments with a
  fresh evaluation budget on each update, matching the translated closure
  lifecycle. String-expression compilation, first evaluation, array-shape,
  and numeric validation return `GMResult.Err` before registration. Later
  failures are exposed through
  `coordinateConstraintResult()` and `coordinateEvaluationError`; the regular
  update path writes `NaN` coordinate values instead of throwing or retaining
  stale geometry. The native JessieCode creator also accepts a CoordsElement
  plus one Transformation or a nonempty Transformation array. JSXGraph
  `1.13.3` accepts nonnumeric members from a function-returned JavaScript
  array and can corrupt coordinate storage, or throws a `TypeError` when the
  single function returns a scalar. Kotlin requires numeric array members and
  returns a structured failure instead. Slider and Coords-object function
  results remain pending.
- `Point.isOn` currently supports translated `Point`, ordinary `Line`, and
  circle-boundary targets. Segment clipping, circle interior hits, curves,
  polygons, and turtles remain pending on their element and visual-property
  models.
- `Line.create` currently accepts two already registered `Point` instances from
  the same `Board`. The native JessieCode creator additionally resolves Point
  names/IDs, creates unnamed helper Points from coordinate arrays, and
  translates the numeric three-standard-form-coordinate branch. Function
  parents and transformations remain pending. Factory and creator failures use
  `GMResult.Err`; JSXGraph throws for unsupported parent values.
- `createArrow` reuses the translated Line parent forms, forces visible
  `straightFirst` and `straightLast` to `false`, and preserves the upstream
  vector type and `arrow` identity. `createArrowParallel` reuses the translated
  three-Point and Line/Point Parallel forms with the same forced flags and
  `arrowparallel` identity. A missing final head defaults to type `1`, size
  `6`, and highlight size `6`; explicit `lastArrow: false` remains disabled,
  matching the observed JSXGraph `1.13.3` normalized-attribute behavior.
  Static `firstArrow` and `lastArrow` values accept Booleans or objects with
  finite non-negative `size`/`highlightSize` and integer `type` in `1..7`.
  Compose translates `getArrowHeadData`, `getPositionArrowHead`, and Canvas
  `drawArrows`, including per-type endpoint shortening, minimum-length
  behavior, cubic paths for types `4..7`, the four-pixel infinite-line inset,
  filled types `1..6`, and open type `7` with its fixed effective size.
  Dynamic attribute functions, highlight-state rendering, touch-point
  adjustment, non-Canvas renderer variants, and Curve/Arc/Sector arrows remain
  pending. JSXGraph's two-Point `createArrowParallel` dispatch creates a
  registered object with non-finite geometry; Kotlin returns structured
  `UnsupportedParents` instead of exposing an unusable scene element.
- `createSegment` reuses the translated two-Point Line construction, assigns
  the upstream `segment` element type, and unconditionally forces
  `straightFirst` and `straightLast` to `false`, including when input
  attributes request `true`. Registered Point and coordinate-array parents are
  supported. An optional numeric, JessieCode-string, or JessieCode-function
  third parent enables the translated `setFixedLength` and
  `updateSegmentFixedLength` lifecycle. It preserves upstream endpoint
  ownership, fixed-endpoint fallback, absolute negative lengths,
  `nonnegativeOnly`, random-direction recovery for coincident endpoints,
  string/function dependency updates, and a fresh evaluation budget for each
  external function call; tests inject the random source for determinism.
  Compilation, initial/runtime evaluation, missing dependency, and nonnumeric
  result failures are structured. Initial factory failure atomically removes
  owned coordinate/coefficient helper Points but never removes existing
  Points. A failed production-session scene update restores every Point,
  including an endpoint moved indirectly by the length constraint. JSXGraph
  `1.13.3` instead throws when the fixture function throws after retaining
  three partially created objects, and propagates `NaN` geometry when the
  function returns a nonnumeric value.
- `Line.getAngle(String)` returns `GMResult.Err(UnsupportedAngleUnit)` for an
  unknown unit. JSXGraph returns JavaScript `undefined`; valid unit prefixes
  and the no-unit radians result retain upstream behavior.
- `Circle.create` currently accepts an already registered center plus an
  already registered circumference `Point`, a fixed numeric or JessieCode
  string radius, an already registered `Line`, or an already registered source
  `Circle` from the same `Board`. Three registered or coordinate-array Points
  create the upstream constrained circumcenter sub-element and a dependent
  circumcircle; the implicit center remains registered for update/removal
  ordering but is excluded from source scene snapshots. String-radius
  compilation, first evaluation, and numeric validation return `GMResult.Err`
  before registration; later evaluation failures are available through
  `radiusResult()` and `radiusEvaluationError`, while the legacy numeric
  `Radius()` path returns `NaN`. JSXGraph can instead throw during expression
  execution or propagate JavaScript coercion. The native JessieCode creator
  resolves names/IDs, creates unnamed helper Points from coordinate arrays,
  and accepts the translated radius forms in either upstream order. A genuine
  JessieCode function radius is called without arguments, receives a fresh
  evaluation budget on every external Circle update, and contributes its
  statically discovered element dependencies without becoming a geometric
  parent. `nonnegativeOnly` clamps numeric, string, and function results to
  zero before the Circle is updated. Initial invocation, dependency,
  registration, and nonnumeric-result failures return structured errors;
  helper Points created for a failed factory call are removed atomically.
  Transformations remain pending.
- Explicit `createCircumcenter`, its `circumcirclemidpoint` alias, and
  `createCircumcircle` accept three Point/reference/coordinate parents through
  construction documents and native JessieCode. Public centers keep the
  upstream `circumcenter` element type; Circumcircle owns a hidden registered
  center, exposes it through `subs.center`, and preserves the upstream
  `[center, firstPoint, circle]` inheritance list. Existing parents own the
  dependencies, coordinate helpers are owned by the constrained center, and
  removal or failed registration follows the source-controlled lifecycle.
  Degenerate ideal/`NaN` arithmetic is preserved. The same-source static and
  parent-drag fixture passes at both viewports, but these explicit creators
  remain outside the `0.1.0` Stable corpus.
- `createReflection` currently translates its existing-Point plus Line branch;
  `createMirrorElement` translates its existing-Point plus Point branch, and
  `createMirrorPoint` reuses that branch with the upstream alias `elType`.
  Outputs use live reflect/rotate transformations, remain non-draggable, and
  preserve the upstream asymmetric dependency: the source Point is metadata
  only, while the Line or mirror center owns the output and controls recursive
  removal. `needsRegularUpdate: false` freezes the output after its initial
  update. Although the API documentation suggests Point-like coordinate
  inputs, JSXGraph `1.13.3` rejects coordinate arrays after `board.select`;
  Kotlin preserves that rejection. Reflection and MirrorElement branches for
  Line, Curve, Polygon, Circle, Arc, and Sector remain explicitly unsupported.
  Same-source static and source-drag parity passes at both viewports, but the
  Point branches remain outside the `0.1.0` Stable corpus.
- `createMidpoint` accepts two registered Point, Point name/ID, or
  coordinate-array parents, or one registered Line parent whose endpoints are
  reused. Its homogeneous arithmetic matches the source-controlled official
  fixture: moving either parent updates the result, an ideal parent produces
  `[1, NaN, NaN]`, and a `NaN` in one summed coordinate does not discard the
  other finite coordinate. Existing parents own the Midpoint dependency and
  survive its removal; coordinate helper Points are owned children and are
  removed recursively with it. Registration failure removes all helpers
  atomically. Although `Type.isPointType` and `Type.providePoints` document a
  function returning a coordinate array, JSXGraph `1.13.3`
  `createMidpoint` first replaces every parent with `board.select(parent)`;
  the function form then throws the standard unsupported-parent error. Native
  JessieCode preserves that observed failure as `UnsupportedParents`. The
  official fixture also records that applying one explicit Midpoint `id` to
  coordinate-array parents propagates that `id` into upstream helper
  attributes and corrupts the official Board registry; the production visual
  case therefore uses existing Point and Line parents, while helper ownership
  and rollback remain covered by native tests.
- `createOrthogonalProjection`, `createPerpendicularPoint`,
  `createPerpendicular`, and `createPerpendicularSegment` accept one registered
  Point, Point name/ID, or coordinate-array parent and one registered Line
  parent in either order. The translated factories preserve the upstream
  projection versus endpoint-sensitive perpendicular branches, dependency
  edges, parent metadata, helper ownership, perpendicular standard-form
  coefficients, and dynamic PerpendicularSegment endpoint order. The
  OrthogonalProjection parent list deliberately stores the output Point's own
  ID instead of the source Line ID because JSXGraph `1.13.3`
  `createOrthogonalProjection` does the same. Registration and unsupported
  parent failures return structured errors and remove owned helper Points
  atomically. Symbolic `generatePolynomial` methods remain excluded with the
  rest of `src/unused/symbolic.js`-dependent CAS behavior.
- `createParallelPoint` accepts three registered Point, Point name/ID, or
  coordinate-array parents, or one Line and one Point in either order. It
  preserves the upstream homogeneous `c + b - a` constraint, parent metadata,
  dependency updates, and existing-versus-owned Point lifecycle.
  `createParallel` accepts the same parent forms. Three Points create a finite
  ParallelPoint endpoint and preserve caller-supplied `straightFirst` and
  `straightLast`; a Line and Point create the normalized upstream ideal
  direction Point. The source Line intentionally does not own that helper, and
  removing the Parallel leaves the ideal helper registered as JSXGraph
  `1.13.3` does. Compose scene snapshots adapt the ideal endpoint to a finite
  directional endpoint because the platform-independent renderer has no
  homogeneous Point representation. Degenerate geometry follows the Board
  arithmetic, while a production scene containing non-finite Line geometry is
  rejected structurally. Registration and unsupported-parent failures roll
  back newly materialized helpers atomically.
- `createBisector`, `createIncenter`, and `createIncircle` accept three
  registered Point, Point name/ID, or coordinate-array parents. Bisector uses
  the exact `Geometry.angleBisector` constraint and a hidden Point; Incenter
  uses the upstream side-length weighting; Incircle uses a hidden Incenter and
  the upstream semiperimeter-area radius. Their dependencies, parent metadata,
  updates, and degenerate `NaN`/zero-radius arithmetic match JSXGraph `1.13.3`.
  Removing a Bisector or Incircle leaves its hidden helper registered, while
  removing that helper recursively removes the visible construction. A direct
  coordinate-parent Incenter owns and removes its temporary Points; Bisector
  and Incircle removal leaves the surviving helper and its coordinate Points,
  matching the official lifecycle. JSXGraph propagates an explicit output
  `id` to coordinate helper attributes and can overwrite its temporary Board
  registry entries. Native factories deliberately allocate isolated helper
  IDs and return structured errors with atomic rollback instead of reproducing
  that registry corruption.
- `Board.removeObject` resets a removed element's board position to `-1` and
  ignores later attempts to remove the same reference. JSXGraph leaves the
  stale `_pos` value on the removed object, so removing that reference again
  can splice an unrelated element that has moved into the old position.
- Random-distribution functions accept an injectable `RandomSource`. Their
  default behavior still uses the platform random source; injection makes
  algorithm parity tests deterministic.
- `boxplot` and `weightedMean` use `GMResult` for empty-data and
  dimension-mismatch failures instead of returning mixed JavaScript types or
  throwing.
- `Numerics.Gauss` returns `GMResult` for dimension and singular-matrix
  failures instead of throwing.
- Numerical integration, interval root-finding, domain-search, and
  minimization APIs return `GMResult` for invalid intervals, node counts, and
  quadrature orders.
- `Numerics.Newton`, `Numerics.chandrupatla`, and `Numerics.root` accept an
  injectable `RandomSource`. Their default behavior still uses the platform
  random source; injection makes fallback and interpolation parity tests
  deterministic.
- `Numerics.splineDef` and `Numerics.splineEval` return `GMResult` for
  malformed spline data. Out-of-domain evaluation is reported as
  `SplineValueOutOfDomain`; upstream returns `NaN`, including a scalar `NaN`
  when one item in an array request is out of range.
- `Numerics.generalizedDampedNewton` returns `GMResult` for malformed function
  vectors, malformed Jacobians, and singular Jacobians. Upstream can instead
  propagate `undefined`/`NaN` or fail later while indexing these values.
- `Numerics.rungeKutta` returns `GMResult` for invalid intervals, step counts,
  Butcher tableaus, and derivative-vector dimensions. String method names
  retain the upstream fallback to Euler for unknown values.
- Gauss-Kronrod and QAG integration return `GMResult` for invalid intervals,
  workspace limits, and impossible tolerance configurations. QAG exposes the
  three upstream quadrature rules through `GaussKronrodRule`.
- `Numerics.polzeros` returns `GMResult` when explicit initial roots are
  missing. Constant polynomials return an empty root array; JSXGraph `1.13.3`
  throws while constructing automatic initial roots for a nonzero constant.
- `Numerics.RamerDouglasPeucker` and `Numerics.Visvalingam` return `GMResult`
  for invalid simplification parameters or an invalid internal point topology.
  The upstream implementation can recurse indefinitely or fail while indexing
  for those inputs. Ramer-Douglas-Peucker uses an explicit work stack to avoid
  exhausting the Kotlin call stack on adversarial curves.
- `Numerics.generatePolynomialTerm` returns `GMResult` for an unavailable
  coefficient or a precision outside JavaScript's `1..100` range instead of
  propagating `TypeError` or `RangeError`.
- Cardinal and Catmull-Rom spline evaluation returns `NaN` when
  `suspendedUpdate` requests a non-knot value before that coordinate's
  coefficient cache is initialized. JSXGraph `1.13.3` throws `TypeError` while
  indexing the absent cache despite a subsequent missing-coefficient guard.
- Bezier evaluation returns `NaN` for an empty point list or an unavailable
  control point. JSXGraph `1.13.3` throws `TypeError` while indexing the
  missing point.
- B-spline order is an `Int`, matching the documented degree-plus-one
  contract. JavaScript non-integer orders only produce incidental
  `undefined`/`NaN` array-property behavior and are not represented.
- Regression polynomial construction and evaluation use `GMResult` for empty
  or mismatched data, invalid degrees, singular normal equations, and a
  suspended evaluation without compatible cached coefficients. JSXGraph
  `1.13.3` throws or propagates `undefined`/`NaN` for these cases.
- Riemann geometry and sum generation use `GMResult` for non-finite rectangle
  counts and interval bounds. Lower/upper sampling stops when floating-point
  addition can no longer advance; this prevents the upstream loop from
  hanging on zero or subnormal effective widths.
- Curve-intersection Newton wrappers return `GMResult.Err(SingularMatrix)` for
  parallel or singular tangents. JSXGraph `1.13.3` propagates `NaN`
  coordinates and parameters.
- `Geometry.meetBezierCurveRedBlueSegments` uses an internal
  `DiscreteCurve2D` adapter until the full `Curve` element is translated. It
  returns `GMResult.Err` for a negative intersection index or an unsupported
  Bezier-degree pairing; direct upstream calls can fail while indexing or
  recursing for those inputs.
- The discrete `plot` branch of `Geometry.projectCoordsToCurve` also uses
  `DiscreteCurve2D` until the full `Curve` domain and transform lifecycle is
  available. Unsupported degrees and incomplete cubic control-point groups
  return `GMResult.Err` instead of failing while indexing.
- The continuous `parameter`, `polar`, and `functiongraph` projection branch
  uses an internal `ContinuousCurve2D` adapter and returns coordinates before
  `curve.updateTransform`. Non-finite or reversed domains return
  `GMResult.Err`; transform application remains pending on the full element
  lifecycle.
- `Geometry.projectCoordsToPolygon` returns `GMResult.Err` for fewer than two
  vertices or when every edge produces an undefined projection. JSXGraph
  `1.13.3` returns JavaScript `undefined` in those cases.
- `Geometry.getPlaneBounds` returns `GMResult.Err` when either 2D linear solve
  is singular. JSXGraph `1.13.3` throws from `Numerics.Gauss`.
- `Geometry.meetPlaneSphere` and `Geometry.meetSphereSphere` return a numeric
  `Circle3DIntersection` snapshot. `IntersectionCircle3D` supplies the
  element-bound dynamic center/radius and Sphere/Sphere normal wrappers around
  those pure calculations; Plane/Sphere retains the plane normal snapshot
  passed by JSXGraph `1.13.3`.
- Sphere3D's parallel proxy uses an internal Kotlin radius closure instead of
  passing a JavaScript function through `Type.createFunction`; it preserves
  dynamic absolute-radius evaluation and dependency updates without a JS
  runtime. Central-projection focus, inner-vertex, Ellipse, and auxiliary
  Point closures are translated directly.
- Implicit central-projection Points are owned by the Sphere3D and excluded
  from the Ellipse's serialized parent list so Board removal can clean the
  complete proxy atomically. JSXGraph retains the three auxiliary Point IDs
  in the Ellipse proxy's `parents` array.
- During parallel-to-central-to-parallel Sphere3D switching, JSXGraph
  `1.13.3` removes the visible proxies and three auxiliary Points but retains
  stale entries in `sphere.inherits` and leaves the Ellipse's generated center
  registered on the Board. Kotlin clears both as part of the owned projection
  set before building the replacement, while preserving the observable
  Circle/Ellipse switching behavior.
- The scene model currently renders Sphere3D fills as flat colors. It accepts
  `gradient` metadata for source compatibility, but the official radial
  gradient remains explicit follow-up work.
- `Geometry.reuleauxPolygon` accepts a positive odd `Int` vertex count and
  returns `GMResult.Err` for even/non-positive counts or too few points.
  JSXGraph accepts a dynamic number and fails later while indexing for
  incompatible inputs.
- `Geometry.sortVertices` returns an unchanged copy for empty and single-point
  inputs. JSXGraph `1.13.3` throws while repeatedly removing a closing point
  from those inputs.
- The translated geometry primitives accept homogeneous coordinate and
  standard-form arrays directly. JSXGraph's `Point`, `Line`, `Circle`, and
  `Coords` overloads will wrap these functions when the element model is
  translated; `PerpendicularPointRole` preserves identity-dependent branches.
- `Numerics.Romberg` honors its documented default configuration. JSXGraph
  `1.13.3` dereferences `config.eps` when the optional config is omitted.
- `Complex.toString(digits)` uses a common Kotlin fixed-decimal formatter.
  Extremely large values and unsupported digit counts do not reproduce
  JavaScript `Number.toFixed` exceptions byte-for-byte.
- Polynomial term generation uses a common Kotlin significant-digit formatter.
  Extreme precisions can differ from JavaScript `Number.toPrecision` in digits
  beyond the platform's shortest round-trip `Double` representation.
- Native math functions can differ from JavaScript by a few final binary
  digits. Official-reference assertions use narrow numeric tolerances.
- `Nlp.findMinimum` preserves the one-based JSXGraph `1.13.3` COBYLA control
  flow and numeric results, but validates array sizes, trust-region values,
  print level, and evaluation limits before allocation. Callback failures are
  returned as `GMResult.Err(NlpError.Evaluation)` instead of escaping through
  the optimizer.
- Statistics filters `NaN` values before sorting percentile and boxplot data.
  Upstream filters after sorting, which makes results depend on the
  JavaScript engine's sort behavior when the comparator receives `NaN`.
## Safety Guards

The following upstream edge cases can loop indefinitely or recurse without a
base case. The Kotlin translation returns `NaN` or the mathematically defined
result instead:

- `factorial(NaN)` and `factorial(Infinity)` return `NaN`.
- `binomial` returns `NaN` for non-finite inputs.
- `gcd(0, x)` returns `abs(x)` and non-finite inputs return `NaN`.
- `squampow` uses arithmetic halving rather than JavaScript 32-bit bitwise
  coercion for large integer exponents.
