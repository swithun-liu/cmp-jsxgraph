# Production Roadmap

## Scope

- Upstream baseline: JSXGraph `1.13.3`
- Runtime: pure Kotlin Multiplatform
- Rendering: Compose Multiplatform Canvas
- Platforms: Android, iOS, JVM/Desktop, Wasm
- Explicit exclusion: `src/unused/symbolic.js`

The upstream baseline contains 104 JavaScript source files outside
`src/unused`. Completion is tracked by source symbol and behavior, not by raw
line count. The `2026-10-09` source audit excludes only the legacy VML backend,
scans the remaining 103 files, and classifies all 2,591 source symbols:
2,540 translated, 16 explicitly adapted for pure KMP, 35
platform/CAS/license exclusions, and 0 pending. All 2,556 in-scope symbols,
including all 2,029 API-surface symbols, map to source-specific Kotlin
locations.
The independent
[`portable-behavior-compatibility.json`](portable-behavior-compatibility.json)
audit verifies all 249 portable behavior slices for a weighted score of
`100.00%`; one host-specific slice is excluded from 250 assessed behaviors.
`Numerics.glomin` is separately excluded by the MIT-only license policy.
Neither mapping coverage nor this portable behavior score overrides the
narrower Stable release gates below.

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

This promotion does not close the qualification roadmap. The portable
translation audit has no pending symbols or behavior slices, but capabilities
outside the 30-case Stable contract remain preview APIs until their own
independent production qualification batches pass.

## Post-0.1.0 Translation Progress

- Coordinate systems and transforms: the numeric and dynamic 2D kernel from
  `src/base/transformation.js` is translated, including number, JessieCode,
  structured function, live Point/Line, and dynamic matrix parameters;
  one-time application; static cloning/composition; `bindTo`; static
  `meltTo`; atomic structured containment of the upstream dynamic `meltTo`
  null-clone defect; transformed-position preimages; the native JessieCode
  `transform` creator; construction-document transform IDs; and
  transformed-Point creation from one transform or a chain. The 4x4
  `setMatrix3D` kernel covers
  translate, three- and four-parameter scale, rotate/rotateX/rotateY/rotateZ,
  affine, affinematrix, generic, and matrix forms with scalar, vector,
  JessieCode, and structured dynamic evaluation.
- View3D, Point3D, Line3D, IntersectionLine3D, Plane3D wireframe and finite
  surfaces, Mesh3D, Curve3D, VectorField3D, Circle3D,
  Axis3D, Polygon3D, Face3D, and Polyhedron3D: the bounded lifecycle from
  `src/3d/element3d.js`,
  `src/3d/view3d.js`, `src/3d/point3d.js`, `src/3d/linspace3d.js`,
  `src/3d/box3d.js`, `src/3d/curve3d.js`, `src/3d/surface3d.js`,
  `src/3d/polygon3d.js`, `src/3d/face3d.js`, `src/math/tiling.js`,
  `src/3d/polyhedron3d.js`, `src/base/transformation.js`, and
  `src/options3d.js` is translated. It
  covers View3D registration and selection, parallel and central camera
  matrices, forward/inverse projection helpers, cube bounds, dynamic 3D
  coordinates/vectors/ranges, ordinary 2D proxies, Curve3D sampling,
  Circle3D normal/frame/radius evaluation, Surface3D two-parameter sampling,
  Line3D and Plane3D
  transforms, finite plane outlines and Mesh3D wireframes, exact
  rectangle/triangle Plane3D and Surface3D tiling, cyclic color arrays, HSL shaders,
  height colormaps, fully infinite plane/box clipping, projected Text3D,
  Ticks3D curves and labels, automatic
  `border`/`none` View3D axes, explicit Axes3D scene expansion, direct and
  transformed Curve3D, direct Circle3D, direct Sphere3D with parallel Circle
  and central Ellipse proxies, direct/transformed Surface3D,
  Polygon3D, and Polyhedron3D creators,
  static Sphere3D linear/radial fill-gradient parameters and portable
  Compose Canvas radial rendering,
  open Polygon3D vertex
  storage with a closed ordinary Polygon proxy, Point-backed/function-valued/
  homogeneous vertices, nested Polygon3D vertex/border styles, cyclic and
  per-face styles, HSL Face3D shaders, local ascending face-depth ordering,
  `transform3d` binding, updates, `applyOnce`, removal, JSON and JessieCode
  entry points, resource limits, rollback, structured failures, host-neutral
  View3D interaction sessions, and Compose pointer/keyboard routing. Official
  lifecycle fixtures and focused JVM tests pass. Point3D has focused static
  plus proxy-drag visual parity; Line3D, finite Plane3D with its visible
  Mesh3D, Plane3D surface modes, Axis3D, automatic border axes, Polygon3D,
  Curve3D, VectorField3D, Circle3D, IntersectionLine3D,
  IntersectionCircle3D, Sphere3D,
  Surface3D, and Polyhedron3D have focused
  Desktop/Compact static parity.
- Portable transform and 3D behavior is translated, including transformed
  Text, post-construction Image transform mutation, element rendering, and
  Point3D gliders on Line3D, Plane3D, Curve3D, Circle3D, Sphere3D, and
  Surface3D. The 3D surface remains outside the current Stable corpus.
  Point3D gliders cover official position state, host updates, proxy drag,
  finite-range clamping where applicable, and direct/construction/JessieCode
  creation.
  Point3D `moveTo`/`moveAlong` easing, path replacement, interval, callback,
  typed session trigger, host-driven scene snapshots, and Compose frame-clock
  scheduling have focused coverage.
  View3D `depthOrder.enabled/layers` and global Canvas ordering by layer,
  3D ownership, z-index, and creation order match the official fixture.
  Sphere3D runtime projection-mode switching is translated with atomic owned
  proxy cleanup. Curve3D, Circle3D, and
  Surface3D parametric projection now use the source-mapped
  `src/math/nlp.js -> Nlp.FindMinimum` translation.
  `src/math/extrapolate.js -> Mat.Extrapolate` is also translated as a
  standalone pure Kotlin kernel with all four sequence transformations,
  iterative limit classification, official fixtures, sparse-state
  propagation, and structured boundary failures. Raw
  `src/math/plot.js -> updateParametricCurve_v2` is translated for native
  Curve/FunctionGraph/Plot with its default recursion depth, callback
  suspension, smoothness/cusp/jump/undefined/border decisions, canonical
  bbox-derived pixel projection, parameter metadata, and bounded point
  growth. Native Ellipse/Hyperbola/Parabola, Derivative, Spline, and
  CardinalSpline wrappers inherit the same adaptive default. The experimental
  `updateParametricCurve_v3` path is also translated as an explicit opt-in,
  including its iterative stack, cusp/jump/border classification, one-sided
  limits through `Extrapolate`, FunctionGraph-only 30% bounding-box crop,
  callback suspension, parameter metadata, and bounded point growth. Its
  official fixture, common/JVM tests, 109-case workbench, and focused
  Desktop/Compact parity gates pass at `0.987021`/`0.976125`. Plot v2 remains
  the default. The separate explicit Plot v4 path now covers difference-table
  critical-point detection, border/singularity refinement, bounded JessieCode
  interval arithmetic, and scalar `fminbr` fallback. Its focused
  Desktop/Compact gates pass at `0.987222`/`0.976707`. The translated
  post-sampling Ramer-Douglas-Peucker branch preserves FunctionGraph/`plot`
  default-on and ordinary Curve default-off behavior, threshold overrides,
  and non-finite path separators. Its official fixture, common tests, and
  focused Desktop/Compact gates pass at `0.986736`/`0.977750`. The legacy
  Plot v1 recursive sampler and callback-suspension order are also translated
  with an official fixture and focused tests.
  The upstream 3D `generic`
  six-evaluator/16-read defect is preserved as a structured evaluation error.
- Spatial, implicit, and polynomial math kernels:
  `src/math/{bqdt,qdt,implicitplot,metapost,poly}.js` now have traceable
  commonMain translations. They cover both quadtree variants, complete
  implicit component search/tracing, Hobby/MetaPost control solving, and the
  usable polynomial ring/monomial operations. Official fixtures and focused
  tests preserve the separate direct-`ImplicitPlot` and
  `ImplicitCurve` `h_max` defaults. Quadtree recursion, implicit output, and
  invalid polynomial operations use structured bounds or failures; the
  upstream nonfunctional polynomial string parser remains explicitly
  unsupported.
- The MIT-compatible `src/math/numerics.js` kernel now has source-specific
  Kotlin mappings for all 82 included symbols. Direct official fixtures and
  focused common tests cover its interpolation, spline, dynamic cache,
  degenerate-input, regression, integration, root, minimization, and ODE
  behavior. The separately licensed `glomin` function remains excluded.
- Data sources: `src/parser/datasource.js` array loading, header extraction,
  cell normalization, and row/column lookup are translated. Browser table
  lookup is an injected host adapter, and upstream unimplemented mutations
  remain structured unsupported results.
- Compatibility parsers and the null renderer:
  `src/parser/3dmodels.js`, `src/parser/geonext.js`, and
  `src/renderer/no.js` now have traceable commonMain translations. They cover
  ASCII STL polyhedra, the complete GEONExT expression-conversion and
  dependency kernel, and every no-op renderer method. Malformed input and
  resource exhaustion are contained as structured failures while successful
  output, including historical GEONExT quirks, remains fixture-compatible.
- Theme and package assembly: all values from `src/themes/dark.js`,
  `gui.js`, and `mono_thin.js` are retained as typed commonMain option trees.
  The package-registered `mono_thin` theme participates in supported JSON and
  JessieCode Board rendering with upstream generic/primitive/creator/user
  precedence. The legacy dark/GUI scripts remain non-global option patches,
  and the `src/index.js`/`src/plot.js` side-effect bundle role maps to static
  Kotlin module composition rather than runtime imports.
- Legacy construction syntax: `src/parser/jessiescript.js` now has a bounded
  commonMain compiler for all upstream construction branches and named macro
  expansion. It delegates execution to the existing native JessieCode/Board
  path, preserving element order and observable geometry without adding a
  JavaScript runtime. Element property assignments and `draft` styling now
  flow through the translated mutable visual-property model.
- JessieCode computer algebra: `src/parser/ca.js` is translated between AST
  parsing and evaluation. It covers every upstream elementary derivative,
  nested and higher-order `D`, map lookup and assignment wrapping, recursive
  math marking, and the complete trivial-node simplification rule set.
  Malformed input, unknown derivatives, transformation work, output size and
  derivative order are reported through bounded `GMResult` failures.
- Dump serialization: the pure `src/utils/dump.js` marker, minimization,
  Board traversal, `str`, `arrayToParamStr`, `toJCAN`, `toJessie`, `toJSON`,
  and `toJavaScript` paths are translated for typed Kotlin and JSON values.
  Retained JSON, JessieCode, and legacy sessions expose bounded live Board
  export with element-specific parent/sub snapshots, dependency metadata,
  user logs, bounding-box replay, axis tick removal, and polygon-glider state.
  Translated runtime function parents retain reconstructable JessieCode and
  JavaScript source, while JSON function-array entries preserve the official
  `null` representation. The complete Board export slice is verified.
- Type utilities: the pure `src/utils/type.js` predicates/defaults,
  including `isFunction`, typed/map-backed Board and geometry predicates,
  Board ID/name/group lookup, Point/Point3D parent-form recognition,
  Transformation-array recognition, explicit prototype/method-map adapters,
  adapter-driven trace clone/cache helpers,
  JavaScript-ordered key/deduplication helpers,
  deprecated CSS parse/key-value/stringify conversion with structured
  malformed-input failures, translated-`Slider` `evalSlider`, bounded
  recursive array/function `evaluate`, reference-preserving
  `clone`/callable `cloneAndCopy`, bounded
  standard/custom `toJSON`, bounded `deepCopy`/`keysToLowerCase` including
  function and JSXGraph-handle behavior, bounded map-based `copyAttributes`,
  explicit owner-aware `bind`, adapter-driven `filterElements`, bounded
  mutable-container `merge`/`mergeAttr`, strict-equality list/object operations,
  `swap`, `concat`,
  `coordsArrayToMatrix`, `str2Bool`, `uniqueArray`,
  `toUniqueArrayFloat`, `cmpArrays`, decimal adjustment, `toFixed`, `trunc`,
  `autoDigits`, `parseNumber`, `parsePosition`, `escapeHTML`, `unescapeHTML`,
  fallback `sanitizeHTML`, `capitalize`, `trimNumber`, `trim`, `toFraction`,
  and `stack2jsxgraph` functions are translated with JavaScript
  number/identity semantics, mutation order, stable sorting, unit precedence,
  typed mixed-value results, structured callback/range/index/copy/merge
  failures, and ECMAScript whitespace handling. Browser DOM predicates and
  host-specific adapters remain platform-owned in their owning slices.
- Environment utilities: the platform-neutral `src/utils/env.js` kernel now
  covers input-event classification and coordinates, legacy user-agent
  predicates, non-browser default dimensions, inline CSS property parsing,
  and CSS transform/zoom extraction. DOM capability probes, event listener
  registration, timed browser chunks, offsets, and fullscreen mutation stay
  in platform adapters.
- Board viewport navigation: `src/base/board.js` pan, centered and
  pointer-centered zoom, navigation-arrow commands, `zoomAllPoints`,
  `zoomElements`, zoom reset and limits, `maxBoundingBox` rejection, default
  Shift modifiers, focused-element keyboard movement, and focus lifecycle are
  translated in commonMain. The official Board fixture and focused JVM tests
  match viewport, origin, unit, zoom, and element-coordinate outputs.
  Pointer hover now follows the official dehighlight/highlight ordering,
  preserves keyboard-focused elements, publishes construction/JessieCode
  session snapshots, and routes unpressed Compose Move plus Exit events.
  Color/number and array-/function-path movement animation now has official
  frame, callback, host-driven session, and Compose frame-clock evidence.
  Multi-pointer pointer ownership, Board pan/pinch, and
  Line/Polygon/Circle transforms are translated through Core sessions and
  Compose routing. The portable navigation surface preserves the official
  11-control order, symbols, navbar style subset, zoom/reset/pan/clear-traces
  commands, and event consumption. Fullscreen, screenshot, and reload are
  explicit host actions because their execution is platform-owned. Existing
  construction and JessieCode scenes opt in through `showNavigation`.
- Encoding, color, and normalization utilities:
  `src/utils/encoding.js`, `base64.js`, `color.js`, `expect.js`, and `uuid.js`
  now have pure commonMain translations. They cover UTF-8 and Windows-1252
  conversion, filtered Base64 encode/decode, all pure color parsing and
  transformation functions, the exact named-color and Wong palettes,
  Point/Coords/array normalization, and pooled-random UUID generation.
  Invalid Kotlin inputs return structured failures where applicable, UUID
  randomness is injectable, and the renderer-facing CSS adapter remains
  browser-free. Browser-native arbitrary CSS validation and global
  `setClassicColors` mutation remain owned by future platform/options work.
  Four official fixtures and 13 focused JVM tests pass, together with the Wasm
  compilation gate.
- Composite and HTML-backed base elements: `src/base/chart.js`,
  `foreignobject.js`, `group.js`, `turtle.js`, and
  `src/element/{button,checkbox,input}.js` now have traceable commonMain
  implementations and official fixtures. The translated slice covers all
  direct-array Chart styles and vertical Legend output, opaque ForeignObject
  geometry, Group movement constraints, Turtle drawing/state operations, and
  typed control state and interactions, including official missing-label
  `undefined` rendering. Transformed ForeignObject hit testing and portable
  attribute/hit-testing APIs are translated; DOM table ingestion, intrinsic
  HTML measurement, and renderer event nodes are platform-owned. The focused
  JVM and Wasm gates cover this batch; it is not yet Stable.
- XML and archive utilities: `src/utils/xml.js`, `src/utils/zip.js`, and
  `src/compressor.js` now have pure commonMain implementations. They cover the
  reader-facing DOM shape and internal entities; ZIP/GZIP/zlib containers;
  stored, fixed-Huffman, and dynamic-Huffman DEFLATE blocks; multi-file and
  data-descriptor archives; and first-entry Base64/URI decompression.
  Malformed input and resource limits return structured failures. Two
  official fixtures and 12 focused JVM tests pass; the same implementation
  also passed the Reader Wasm compilation gate.
- File readers: source-mapped commonMain slices now cover
  `src/reader/file.js`, `graph.js`, `geonext.js`, `intergeo.js`,
  `geogebra.js`, `cinderella.js`, and `tracenpoche.js`. The batch includes
  case-insensitive registry/string dispatch; bounded Base64/ZIP/XML
  preparation; Graph parsing and drawing; format-specific properties,
  expressions, tokenization, parsing, supported geometry creation, dynamic
  dependencies, strict partial-import rejection, and atomic rollback.
  GeoGebra additionally consumes the official `setDefaultOptions` values and
  per-output XML visual properties, including labels in public scene
  snapshots. Public
  `JsxGraphReader.parse` accepts already loaded text for the registered
  built-in aliases and returns bounded portable scenes with typed failures.
  Browser XMLHttpRequest/Blob loading and decoding policy remain
  platform-owned, while custom reader registration stays internal and
  unsupported input outside the audited source branches returns explicit
  diagnostics or structured failures. Official fixtures, two direct upstream
  Geogebra worksheets, focused JVM tests, and the Wasm gate cover this batch.
  Independent production visual qualification remains outside the current
  Stable corpus.
- Function-coordinate Points: native JessieCode now supports mixed
  number/string/function coordinate terms and one function returning a
  numeric coordinate array, with fresh evaluation budgets, persistent Board
  updates, structured failures, and same-source static/interaction parity.
  The documented function-parent route through `createMidpoint` remains
  rejected because JSXGraph `1.13.3` itself loses the function in its leading
  `board.select` pass.
- Point incidence: `src/base/point.js -> isOn` now covers Point, ordinary
  Line, finite Segment, circle boundary, Polygon border, and ordinary or
  transformed data-Plot/continuous Curve targets with strict tolerance
  comparisons. Static Point `alwaysIntersect` and Circle/Polygon
  `hasInnerPoints` preserve the upstream infinite-Line and interior-area
  branches. Transformed Curve incidence preserves the upstream
  projection-then-transform order instead of projecting onto final rendered
  geometry. Curve evaluation or projection failure returns `false`;
  function-valued and runtime-mutated visual properties are translated.
  JSXGraph `1.13.3`'s Turtle branch dereferences the two-item projection result
  incorrectly and always throws; Kotlin intentionally returns `false`.
- StepFunction: `src/base/curve.js -> createStepfunction` now retains source
  arrays, rebuilds output during regular Board updates, preserves empty,
  mismatched-Y, and function-arity behavior, and enforces expanded point
  limits through JSON and JessieCode entry points. Core, creator, persistent
  session, document, official fixture, and Desktop/Compact same-source gates
  pass. It remains outside the Stable corpus until independent production
  qualification is added.
- Ticks: `src/base/ticks.js -> Ticks/createTicks` now covers registered Line
  and Curve parents, fixed/equidistant positions, anchors and boundaries,
  automatic Line spacing, minor/major paths, faces, finite/infinite endings,
  polar paths, Curve endpoint/interior normals, fixed-label source indexes,
  number formatting, and parent/label visibility inheritance. Pixel-sized
  paths resolve against the final Compose viewport. JSON and JessieCode entry
  points enforce bounded creation and structured failures. Core, creator,
  document/session, official fixture, and focused Desktop/Compact static
  parity gates pass. `createHatchmark` plus its `hatch`/`hash` aliases reuse
  the translated Ticks path with the exact centered fixed-position loop,
  official defaults, parent inheritance, fractional-count behavior, bounded
  creation, and focused Desktop/Compact parity. Function-valued tick
  distances, Curve-parent hit testing, labels, and runtime mutation are
  translated. Independent production qualification remains outside the
  current Stable corpus.
- Axis: `src/base/line.js -> createAxis`, `src/base/board.js -> getPointLoc`,
  and the `src/options.js` defaults now cover the official Line wrapper,
  non-draggable AxisPoint endpoints, generated default Ticks, arrow/style
  defaults, fixed and sticky horizontal/vertical positioning, numeric and
  unit-bearing distances, viewport-resolved `ticksAutoPos`, dynamic parent
  updates, recursive cleanup, JSON/JessieCode resource limits, and atomic
  rollback. Core, creator, document/session, official fixture, and focused
  Desktop/Compact static parity gates pass. Axis-specific mutation, labels,
  hit testing, and default-axis integration are translated. Independent
  production qualification remains outside the current Stable corpus.
- Grid: `src/element/grid.js -> createGrid` and the `src/options.js` defaults
  and themes now cover linked major/minor Curves, all documented faces,
  themes `0..6`, `majorStep`, `minorElements`, `forceSquare`, boundary and
  zero-axis controls, user/`px`/`%`/`fr` units, and explicit Axis parents.
  JSON, JessieCode, persistent sessions, scene snapshots, and Compose use the
  same viewport-dependent definitions; point growth is checked before
  allocation and partial creation rolls back atomically. Core, creator,
  document/session, official fixture, and focused Desktop/Compact static
  parity gates pass. Major and minor Grid curves preserve the upstream
  non-interactive `hasPoint = false` contract. Default Board-axis fallback and
  Grid-specific mutation are translated. Independent production qualification
  remains outside the current Stable corpus.
- Image: `src/base/image.js -> Image/createImage`,
  `src/base/element.js -> addRotation`, and the Canvas renderer path now cover
  static data sources in JSON and native JessieCode, plus function-valued
  sources, coordinates, and dimensions in native JessieCode. Absolute pixel
  sizing for negative dimensions, `W`/`H`/`setSize`, opacity, the exact
  five-transform rotation chain, bounded source lengths, Scene snapshots, and
  affine Compose drawing are translated. The default resolver decodes bounded
  base64 image data URIs without network or filesystem access; applications
  explicitly supply any external-resource resolver. Official fixture, common
  tests, and focused Desktop/Compact static parity gates pass. Dragging,
  transformed hit testing, and arbitrary transform mutation are translated;
  non-data-URI loading is host-owned. Independent production qualification
  remains outside the current Stable corpus.
- Glider/Slider: the Point, Circle, finite-coordinate Line/Segment,
  Polygon-border, Arc/Sector, ordinary Curve, and transformed-Curve branches
  of
  `src/base/point.js -> createGlider`,
  `src/base/coordselement.js -> updateGlider/updateGliderFromParent`, and
  `src/math/geometry.js -> projectCoordsToCurve`, plus
  `src/element/slider.js -> createSlider` now cover Line projection and
  finite-end clamping; FunctionGraph, true-parametric Curve, and data-Plot
  projection; parent updates and relative-parameter preservation; direct
  position updates; structured Curve evaluation/projection failures; helper
  ownership/removal; labels; ticks; `snapWidth`; `snapValues`; and the Slider
  value/mutation method map. Native JessieCode and construction documents
  expand Slider into the official helper order with bounded object accounting
  and atomic rollback. Core, creator, document/session, official fixtures, and
  focused Desktop/Compact static parity gates pass. The Curve Glider fixture
  scores `0.986233`/`0.977524` statically and `0.986150`/`0.977598` after a
  data-Plot drag. JSXGraph `1.13.3` rejects Ticks and its public Turtle path
  fails because Turtle is absent from `board.objects`; Kotlin preserves those
  unavailable paths as structured failures without the upstream partial-Point
  leak. Point, Circle, Line/Segment, ordinary Curve, Arc/Sector, and Polygon
  attractor conversion and detaching now match the official fixture. Bounded
  Glider animation, function-valued movement, host-driven session frames, and
  Compose frame-clock scheduling are source-mapped and fixture-backed.
  Hyperbola/Parabola Glider projection and lifecycle plus Slider baseline
  pointer-up movement, `moveOnUp`, and fixed gating are fixture-backed.
  Locale-aware behavior is translated; independent production qualification
  remains outside the current Stable corpus.
- Tapemeasure: `src/element/measure.js -> createTapemeasure` and the
  corresponding `src/options.js` defaults now cover coordinate endpoint
  creation, finite Segment composition, optional dynamic value label and
  equidistant Ticks, nested helper identity, `subs`/`inherits`, `Value()`/`V`,
  endpoint dependency updates, official root/helper removal behavior, bounded
  JSON/JessieCode scene expansion, and atomic rollback. Core, creator,
  document/session, official fixture, and focused Desktop/Compact static plus
  endpoint-drag parity gates pass. Static scores are
  `0.984781`/`0.969197`; moved scores are `0.985490`/`0.969261`.
  Automatic/none digits, locale-aware `formatValue`, function-valued visual
  attributes, and hit testing are translated. Independent production
  qualification remains outside the current Stable corpus.
- Measurement: `src/element/measure.js -> createMeasurement`,
  `src/parser/prefix.js -> PrefixParser`, and the corresponding
  `src/options.js` defaults now cover numeric/string, arithmetic,
  element-method, nested Measurement, and bounded `exec` prefix terms;
  dimension and per-dimension unit propagation; fixed/`auto`/`none` digits;
  function-valued coordinates and Measurement-specific attributes; custom
  coordinate/direction formatters; script markup; translated method aliases;
  dependency updates; structured failures; and atomic rollback. Core,
  creator, document/session, official fixture, common tests, 109-case
  workbench, and focused Desktop/Compact static plus radius-Point-drag parity
  gates pass. Static scores are `0.982740`/`0.970961`; moved scores are
  `0.982735`/`0.969542`. Locale formatting, the portable Math/JXG.Math `exec`
  surface, and Text method access are translated. Independent production
  qualification remains outside the current Stable corpus.
- SmartLabel: `src/element/smartlabel.js -> createSmartLabel` and the
  parent-specific `src/options.js` defaults now cover Point, Line, Circle,
  Polygon, and Angle parents; parent-specific measures and placement; dynamic
  Point/Angle measures; units, prefix/suffix visibility, fixed digits, custom
  formatting, row/column coordinates, Line rotation and visibility
  thresholds, Angle rotation and final-viewport 12 CSS-pixel offset, CSS box
  kinds, method-map access, dependency updates, JessieCode HTML sanitization,
  structured failures, and atomic rollback. Core, creator, session, official
  fixture, common tests, 109-case workbench, and focused Desktop/Compact
  static plus Angle-parent-drag parity gates pass. Static scores are
  `0.984719`/`0.974114`; moved scores are `0.984582`/`0.975467`. Locale and
  portable Text APIs are translated; MathJax/KaTeX and arbitrary browser CSS
  are renderer-owned. Independent production qualification remains outside the
  current Stable corpus.
- Integral: `src/element/composition.js -> createIntegral` and the corresponding
  `src/options.js` defaults now cover Curve/interval parent order, fixed and
  string/function bounds, hidden constrained dynamic-bound Gliders, draggable
  fixed-bound Gliders, x/y-axis base Points and closed fill paths, reverse
  signed `Value()`, optional fixed-digit labels, helper identity/order,
  `subs`/`inherits`, dependency updates, official root-removal behavior,
  bounded JSON/JessieCode scene expansion, and atomic rollback. Core, creator,
  document/session, official fixture, and focused Desktop/Compact static plus
  driver-drag parity gates pass. Static scores are
  `0.984526`/`0.974454`; moved scores are `0.984815`/`0.975287`. Adaptive
  plotting, locale-aware formatting, and `digits: auto`/`none` are translated.
  Independent production qualification remains outside the current Stable
  corpus.
- SlopeTriangle: `src/element/slopetriangle.js -> createSlopeTriangle` now
  covers `[Tangent]`, `[Glider]`, and `[Line, Point]` parents for the translated
  Glider/Tangent subset, including ordinary Curve-backed Gliders through the
  Curve Tangent path. It preserves the optional private Tangent, helper and
  Polygon-border order, helper ownership/removal, `subs`/`inherits`, finite
  Segment borders, default arrows and fill, the read-only method map, dynamic
  fixed-digit label content, and final-viewport 10 CSS-pixel label offset.
  Native JessieCode and construction documents enforce complete scene
  accounting and atomic rollback. Core, creator, document/session, official
  fixture, and focused Desktop/Compact static parity gates pass.
  `digits: auto`/`none`, locale-aware `formatValue`, function-valued visual
  attributes, and hit testing are translated. Independent production
  qualification remains outside the current Stable corpus.
- PolygonalChain: `src/base/polygon.js -> createPolygonalChain` now follows
  the official Polygon wrapper and removes only the duplicated closing vertex
  and final closing Segment. Border storage order, registered/helper Point
  ownership and removal, `Area`/`Perimeter`/`L`/bounds behavior,
  JSON/JessieCode limits, open Compose rendering, and focused static plus
  parent-drag parity pass. Nested Polygon styles, mutable vertex APIs, and
  Polygon hit testing, transformed chains, and labels share the translated
  base implementation. Independent production qualification remains outside
  the current Stable corpus.
- Parallelogram: `src/base/polygon.js -> createParallelogram` now composes the
  translated ParallelPoint and Polygon factories with the official vertex,
  border, and Board creation order. It preserves the exposed helper identity,
  forced draggable/non-fixed helper state, source updates, surviving-helper
  removal behavior, coordinate-Point ownership, nested `parallelpoint`
  attributes, JSON/JessieCode limits, and atomic rollback. Core, creator,
  document/session, official fixture, and focused static plus parent-drag
  parity gates pass. Mutable Polygon APIs, hit testing, and labels use the
  shared translated Polygon implementation. Independent production
  qualification remains outside the current Stable corpus.
- RegularPolygon: `src/base/polygon.js -> createRegularPolygon` now covers
  both `[point1, point2, n]` generation and the existing `n`-Point form. The
  translation preserves the original rotation angle, non-integer
  `ceil(n)` loop behavior, generated CAS Point identity, complete
  `vertices.ids` mapping, forced draggable/non-fixed helpers, source updates,
  Point/Polygon ownership, removal behavior, and Board creation order.
  JSON/JessieCode vertex limits and atomic rollback prevent unbounded or
  partial construction. Core, creator, document/session, official fixture,
  and focused static plus parent-drag parity gates pass. Nested Polygon
  vertex/border styling, shared Polygon hit testing, and labels are translated.
  Independent production qualification remains outside the current Stable
  corpus.
- RadicalAxis: `src/base/line.js -> createRadicalAxis` now covers two
  registered Circle parents, the exact standard-form coefficient closure,
  one-function constrained helper Points, parent/dependency rebinding,
  dynamic Circle updates, successful-removal helper survival, repeated and
  degenerate Circle arithmetic, nested helper identity, native JessieCode and
  construction-document entry points, and atomic duplicate-ID rollback. Core,
  creator, document/session, official fixture, and focused static plus
  radius-Point-drag Desktop/Compact parity gates pass. Nested helper styling
  and labels are translated. Independent production qualification remains
  outside the current Stable corpus.
- PolePoint: `src/base/point.js -> createPolePoint` now covers registered
  Circle/Ellipse/Hyperbola/Parabola and Line parents in either order,
  canonical parent metadata, the exact
  determinant coordinate closure, constrained dependency updates, direct and
  parent removal, non-finite degenerate arithmetic, native JessieCode and
  construction-document entry points, and structured atomic rollback. Core,
  creator, document/session, official fixture, and focused static plus
  four-parent-drag Desktop/Compact parity gates pass. All Open Conic branches
  have parent-update and removal evidence. Independent production
  qualification remains outside the current Stable corpus.
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
  Ellipse, Hyperbola, and Parabola Tangent/Polar interoperation is covered
  through parent updates and removal. PolarLine remains Circle-only as upstream
  defines it; upstream-invalid Turtle and Glider branches return structured
  failures. Independent production qualification remains outside the current
  Stable corpus.
- TangentTo: the Circle/Ellipse/Hyperbola/Parabola branches of
  `src/base/line.js -> createTangentTo` now composes the source Point's polar,
  one polar/Circle Intersection selected by upstream numeric truthiness, and
  the Tangent at that contact Point. It preserves the exposed polar/contact
  identities, nested Line/Point attributes, dependencies, removal lifecycle,
  non-real and degenerate arithmetic, and three-object scene/resource
  accounting. Native JessieCode, construction-document, Core lifecycle,
  official fixture, and focused static plus source-Point-drag
  Desktop/Compact parity gates pass. All four Conic branches have dedicated
  parent-update and removal evidence. Independent production qualification
  remains outside the current Stable corpus.
- Ellipse: `src/element/conic.js -> createEllipse` now covers
  Point/reference/coordinate foci with either a Point on the Ellipse or a
  numeric/function-valued major axis, optional numeric parameter domains,
  constrained center creation, `foci`/`center`/`midpoint`/`majorAxis`
  metadata, quadratic-form updates, JavaScript `Double` behavior for
  degenerate axes, parent/helper ownership, and removal lifecycle. Bounded
  JSON/JessieCode entry points, native session updates, duplicate-ID
  rejection, resource checks, and atomic helper rollback are covered. Core,
  creator, document/session, method-map, official fixture, and focused static
  plus point-parent-drag Desktop/Compact parity gates pass. Native adaptive
  Plot v2, exact focus-distance `hasInnerPoints`, and Tangent, Polar,
  PolePoint, TangentTo, Normal, Intersection, OtherIntersection, and Glider
  interoperation through parent updates and removal are covered. Independent
  production qualification remains outside the current Stable corpus.
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
  gates pass. Native adaptive Plot v2 and Tangent, Polar, PolePoint, TangentTo,
  Normal, Intersection, OtherIntersection, and Glider interoperation are
  covered; PolarLine remains Circle-only. Independent production qualification
  remains outside the current Stable corpus.
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
  focus-drag Desktop/Compact parity gates pass. Native adaptive Plot v2,
  implicit-Line attributes, and Tangent, Polar, PolePoint, TangentTo, Normal,
  Intersection, OtherIntersection, and Glider interoperation are covered;
  PolarLine remains Circle-only. Independent production qualification remains
  outside the current Stable corpus.
- Derivative: `src/base/curve.js -> createDerivative` now creates a
  `curveType=parameter` Curve from `X(t)` and `Numerics.D(Y)(t) /
  Numerics.D(X)(t)`, with the source domain values captured at creation.
  FunctionGraph, parametric, and linearly interpolated data-Plot
  parents are covered, including the viewport-padded data domain, infinite
  vertical slopes, regular Board recomputation, metadata-only parent
  registration, source-removal survival, bounded JSON/JessieCode entry points,
  and structured atomic failures. Core, creator, document/session, official
  fixture, and focused static plus coefficient-Point-drag Desktop/Compact
  parity gates pass. Native adaptive Plot v2 and transformed Curves are
  covered. Independent production qualification remains outside the current
  Stable corpus.
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
  upstream `1.13.3` stack overflow. Transformed-Curve, degree-three Plot/Bezier,
  and all Open Conic branches have dedicated evidence; upstream-invalid
  Glider/Turtle forms return structured failures. Independent production
  qualification remains outside the current Stable corpus.
- Spline/CardinalSpline: `src/base/curve.js -> createSpline` and
  `createCardinalSpline` now cover natural-cubic interpolation from Points,
  coordinate pairs, parallel X/Y arrays, and coordinate functions, plus
  cardinal interpolation from existing or generated Points with dynamic
  tension and uniform, centripetal, or chordal parameterization. Sorted-knot
  updates, metadata-only Spline parents, CardinalSpline generated-Point
  ownership, `createPoints`/`isArrayOfCoordinates`, bounded JSON/JessieCode
  paths, structured failures, and atomic rollback are covered. Core, creator,
  document/session, official fixture, and focused static plus tension-drag
  Desktop/Compact parity gates pass. Native adaptive Plot v2 and ordinary Curve
  fill are covered. Independent production qualification remains outside the
  current Stable corpus.
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
  Desktop/Compact parity gates pass. Function-valued visual attributes are
  translated. Independent production qualification remains outside the current
  Stable corpus.
- Comb: `src/element/comb.js -> createComb` now covers Point/reference and
  coordinate-array endpoints, exact upstream clipping and `NaN` tooth breaks,
  numeric/function-valued frequency, width, angle, and reverse attributes,
  hidden endpoint identity/fixed defaults and removal survival, bounded
  static/dynamic output, and structured atomic rollback. Core, creator,
  document/session, official fixture, and focused static plus driver-drag
  Desktop/Compact parity gates pass. Function-returning Point/coordinate
  parents are translated. Independent production qualification remains
  outside the current Stable corpus.
- Inequality: `src/element/composition.js -> createInequality` now covers Line
  and FunctionGraph sources, the exact expanded half-plane and segmented
  closure algorithms, Board bounds, non-finite FunctionGraph runs, dynamic
  `inverse`, official defaults, metadata-only parents and source-removal
  survival, bounded output, and structured failures. Core, creator,
  document/session, official fixture, focused static parity, Line-parent drag,
  and FunctionGraph-driver/inverse drag pass at Desktop and Compact sizes.
  These are the active upstream source kinds. Independent production
  qualification remains outside the current Stable corpus.
- VectorField: `src/element/vectorfield.js -> createVectorField` now covers
  two component functions/expressions or one array-returning function/
  expression, dynamic three-term meshes, scale, and nested arrow enable/size/
  angle attributes. It preserves inclusive fractional/zero/negative step
  loops, `NaN` path breaks, nonzero-vector arrows, metadata-only function
  dependencies, source-removal survival, viewport CSS-pixel arrow sizing,
  bounded output, and structured atomic failure. Core, creator,
  document/session, official fixture, and focused static plus driver-drag
  Desktop/Compact parity gates pass. Independent production qualification
  remains outside the current Stable corpus.
- SlopeField: `src/element/vectorfield.js -> createSlopeField` now wraps the
  translated VectorField path for one scalar function/expression. It preserves
  the exact unit-direction normalization, non-finite arithmetic, disabled
  arrowhead default, dynamic mesh/scale/arrow evaluation, metadata-only
  closure dependencies, bounded output, and structured atomic failure. Core,
  creator, document/session, official fixture, and focused static plus
  driver-drag Desktop/Compact parity gates pass. Runtime `setF` is translated.
  Independent production qualification remains outside the current Stable
  corpus.
- VectorField3D: `src/3d/curve3d.js -> createVectorfield3D` now covers
  component and array-returning functions, inclusive dynamic X/Y/Z meshes,
  scale, zero-vector suppression, nested arrow settings, ordinary Curve3D
  proxy identity, viewport-sized 3D arrowheads, bounded output, structured
  failures, official fixtures, focused tests, and Desktop/Compact parity.
  Runtime `setF` replacement and Curve3D/Text3D coordinate accessors are also
  source-mapped and covered. Independent production qualification remains
  outside the current Stable corpus.
- Circumcircle creators: `createCircumcenter`, the
  `circumcirclemidpoint` alias, and `createCircumcircle` now cover
  Point/reference/coordinate parents, public and hidden center metadata,
  `subs`/`inherits`, dependency updates, helper ownership/removal, degenerate
  arithmetic, atomic rollback, native JessieCode and construction-document
  entry points, and same-source static/interaction parity. They remain outside
  the Stable corpus until an independent production case and the full
  qualification gates are added.
- Reflections: `createReflection` and `createMirrorElement` now cover Point,
  Line, Curve, Polygon, Circle, Arc, and Sector sources, while
  `createMirrorPoint` remains the Point-only alias. The translation preserves
  dynamic Line reflection, Point-centered π rotation, transformed helper
  Points, Arc/Sector-to-Curve output, Euclidean Circle radius preservation,
  projective Circle-to-Conic conversion, nested helper attributes, upstream
  parent metadata, reflector-owned dependencies, frozen regular-update
  behavior, structured failures, native JessieCode and construction-document
  entry points, and official lifecycle evidence. Stable qualification remains
  pending without expanding the existing 30-case Stable corpus.
- Parallel constructions: `createParallelPoint` and `createParallel` now cover
  three-Point and Line/Point forms, coordinate helper ownership, finite and
  ideal endpoints, dependency updates, removal behavior, native JessieCode and
  construction-document entry points, and same-source static/interaction
  parity. They remain outside the Stable corpus until an independent
  production case and the full qualification gates are added.
- Line arrows: `createArrow` and `createArrowParallel` now preserve vector
  identity, forced visible line-end flags, upstream defaults, explicit
  disabled heads, and Boolean/object head attributes. Native JessieCode scene
  updates preserve whole function-valued Segment arrow objects and nested
  `type`, `size`, and `highlightSize` functions with structured callback
  failures. Compose Canvas translates head types `1..7`, per-type endpoint
  shortening, cubic shapes, filled/open behavior, ideal-endpoint clipping,
  type 7's fixed effective size, and highlight sizes. Core/Compose tests,
  official fixtures, and focused same-source static and parent-drag parity
  pass. Touch-point adjustment is translated; non-Canvas renderers are explicit
  backend exclusions. Independent production qualification remains outside
  the current Stable corpus.
- Curve/Arc/Sector arrows: `firstArrow` and `lastArrow` Boolean/object values
  now produce the same seven Canvas head types as Line arrows. Curve paths use
  the first and last drawable subpath tangents; Arc/Sector cubic paths use
  endpoint control tangents with degenerate fallback. The official fixture,
  common/Compose tests, and the `curve_arrows` same-source case pass at
  `0.988830` Desktop and `0.985632` Compact. This focused case remains outside
  the 30-case Stable corpus. Whole function-valued Curve/Arc/Sector arrows and
  nested arrow fields update retained scenes with official and JVM/Wasm
  evidence.
- Composition and two-Line angle bisectors: the reusable non-Board
  `JXG.Composition` subset and
  `createAngularBisectorsOfTwoLines` now cover role/ID/name membership,
  ordered output access, lifecycle forwarding, two registered output Lines,
  four hidden constrained helpers, normalized coefficient geometry, nested
  attributes, dependency updates, removal, degenerate arithmetic, atomic
  rollback, native JessieCode access, generic group/filter selection,
  composition-wide mutable visual properties, and same-source
  static/interaction parity. JSON compound results remain outside the bounded
  document schema. Independent production qualification remains outside the
  current Stable corpus.
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
  entry points, translated Open Conic dispatch, dynamic visual-property forms,
  and same-source static/interaction parity. Independent production
  qualification remains outside the current Stable corpus.
- Curve Boolean clipping: `createCurveIntersection`, `createCurveUnion`, and
  `createCurveDifference` now cover Circle/Curve/Arc/Sector/Polygon paths and
  the low-level mixed Point/Coords/coordinate-pair array form, including
  homogeneous normalization, original-position retention, and duplicate
  filtering. Degenerate intersection classification and chains, entry/exit
  marking, empty/containment cases, multi-component output, path tracing,
  regular Board updates, structured topology/traversal failures, native
  JessieCode and construction-document entry points, and same-source
  static/parent-drag parity are translated. Independent production cases and
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
  Polygon styling, Axis/Ticks/Grid options, function-valued runtime layer
  mutation, renderer objects, and Board layer options are translated for the
  portable Canvas backend.
- Canvas dash rendering: supported Point, Line, Circle, Curve, and
  Polygon-border strokes now use all seven
  `src/renderer/abstract.js -> dashArray` patterns. `dashScale` multiplies
  intervals by `strokeWidth / 2` as upstream does; the Compose backend maps
  resolved CSS-pixel intervals to dp and clamps only the zero-length dotted
  interval to Skia's `0.001` minimum. Portable visual properties can be
  mutated statically or supplied as JessieCode functions; browser DOM and
  non-Canvas renderer objects are explicit backend exclusions.

## Implementation Order

### 1. Foundation

- constants, coordinate conversion and event dispatch
- `JXG.Math`, probability functions and complex arithmetic
- shared result/error model
- upstream version and source mapping

Exit gate: common tests pass on JVM, iOS simulator and Wasm; all target
artifacts assemble.

### 2. Geometry And Numerics

- statistics, numerical integration/root finding, interpolation, and sequence
  extrapolation
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
- complete source-mapped portable 2D element-factory surface

Exit gate: every portable upstream 2D factory has construction, update and
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
