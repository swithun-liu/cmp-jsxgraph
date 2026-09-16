# Translation Status

## Upstream Baseline

- JSXGraph version: `1.13.3`
- tag: `RELEASE-v1.13.3`
- commit: `7c2176d479ae256cb9d38265bce81fa18709d01f`
- license option: MIT

## Stable Support Scope

Release `0.1.0` is Stable for the translated construction-document and
Compose rendering subset listed in the source map below: Point, Line, Circle,
Curve, FunctionGraph, Plot, Polygon, Text, Arc, Sector, Angle, and free Point
interaction with dependency updates and state restoration.

Rows marked `Partially translated` remain accurate. The Stable rating applies
only to the explicitly listed symbols and parent/attribute forms, not to the
complete upstream file or full JSXGraph API. See
[`stability-report.md`](stability-report.md) for qualification evidence and
[`translation-deviations.md`](translation-deviations.md) for unsupported
behavior.

## Source Mapping

| Upstream source | Upstream symbols | Kotlin target | Status |
| --- | --- | --- | --- |
| `src/base/constants.js` | coordinate constants and object type/class constants | `base/Const.kt` | Translated |
| `src/math/math.js` | `JXG.Math` | `math/Mat.kt` | Translated |
| `src/math/probfuncs.js` | `Mat.ProbFuncs` | `math/ProbFuncs.kt` | Translated |
| `src/math/complex.js` | `JXG.Complex`, `JXG.C` | `math/Complex.kt` | Translated |
| `src/math/statistics.js` | `Mat.Statistics` | `math/Statistics.kt` | Translated |
| `src/math/numerics.js` | linear algebra; Neville and Lagrange interpolation; polynomial generation and regression; cardinal, Catmull-Rom, Bezier, and B-splines; Riemann sums; Newton-Cotes, Romberg, Gauss-Legendre, Gauss-Kronrod, and QAG integration; natural cubic splines; numerical derivative; scalar, multidimensional, polynomial, and curve-intersection roots; domain search; Brent minimization; Runge-Kutta ODE solvers; Ramer-Douglas-Peucker, its deprecated alias, and Visvalingam curve simplification | `math/Numerics.kt` | Partially translated |
| `src/math/geometry.js` | angles, distances, affine ratios, orientation, reflection, rotation, perpendicular and circumcenter constructions, polygon area/sorting/convexity, Graham-scan hulls, winding-number and screen-coordinate point containment, arc-range membership, point-to-line/segment distance, standard-form intersection dispatch, line/circle/segment/polyline and multi-segment Bezier intersections, cubic Bezier splitting/evaluation/bounds/overlap/subdivision intersections, Bezier arc/sector and Reuleaux-polygon generation, point-to-circle/line/segment/Bezier-segment/polygon projection, discrete/continuous curve projection before element transforms, and pure 3D plane/sphere intersection, projection, and bounds helpers | `math/Geometry.kt` | Partially translated |
| `src/parser/jessiecode.js` | generated jison-lex token rules; generic AST nodes; empty programs, expression statement lists, blocks, `if`/`else`, `while`/`do`/`for`, deprecated `use` and delete, return, function, and map expressions covering right-associative assignment, literals, variables, mutable arrays/objects, calls, creator attribute lists, properties, indexes, conditionals, and unary/binary precedence; interpreter support for that subset, JavaScript truthiness and primitive/array coercion, lazy branch evaluation, ordered loop evaluation, persistent parser/evaluator sessions, bounded multi-Board switching through an explicit container registry, Board element deletion, object-property construction, array holes/length/properties, short-circuit evaluation, reusable function scopes, nested closures, map validation, recursive lower-case creator-attribute merge, assignment-LHS creator naming, native `point`/`line`/`circle`/`curve`/`functiongraph`/`plot`/`polygon`/`text`/`arc`/`sector`/`angle` creator lookup, Point numeric/string `X`/`Y` assignment, element `name` and `needsRegularUpdate` assignment, `Bounds`/`addChild`/immediate `move`/`moveTo`/Point `addConstraint`/Text `setText` calls, Polygon vertices/borders/area/perimeter/bounds access, core math calls, `X`/`Y`/`V`/`Value`, line length, Circle/Polygon area and perimeter, Circle radius, coordinate distance, line slope, element-name, angle, binomial/GCD, random integer, `IfThen`, recursive `eval`, and `remove` built-ins, board element lookup through a bounded adapter, stable name-to-ID replacement, ID-to-current-name restoration, and static dependency discovery; bounded lexing, parsing, evaluation, loop/function execution, collection growth, name replacement/restoration, and dependency traversal; visual-property and function-valued element mutation, animated movement, the remaining creator registry, Slider/Glider-backed built-ins, `import`/`$log`/`D`, and the complete element `methodMap` remain pending | `parser/JessieCodeLexer.kt`, `parser/JessieCodeAst.kt`, `parser/JessieCodeExpressionParser.kt`, `parser/JessieCodeRuntime.kt`, `parser/JessieCodeEvaluator.kt`, `parser/JessieCodeSession.kt`, `parser/NativeJessieCodeCreators.kt`, `parser/JessieCodeNameReplacer.kt`, `parser/JessieCodeDependencyCollector.kt` | Partially translated |
| `src/utils/type.js` | `Type.createFunction` string-expression branch backed by JessieCode `snippet` semantics, reusable argument binding, single assignment expressions with function-local targets, stable board references, and dependency metadata | `parser/JessieCodeExpressionFunction.kt` | Partially translated |
| `src/utils/event.js` | `EventEmitter.trigger`, `on`, `off` | `utils/EventEmitter.kt` | Translated |
| `src/base/board.js` | coordinate origin, zoom, units, ID/name registration and selection, automatic element names, object/ancestor removal and creation order, recursive child cleanup and position reindexing, basic prepare/update/render/full/suspend lifecycle, and retained production sessions for Point drag updates | `base/Board.kt`, `JsxGraphEngine.kt` | Translated dependency and Point-interaction subset |
| `src/base/board.js`, `src/jxg.js`, `src/options.js` | ordered `Board.create(type, parents, attributes)` construction, native registry dispatch, bounded JSON document validation, Point/Line/Circle/Curve/Polygon/Text/Arc/Sector/Angle default and supplied visual properties, platform-independent scene snapshots, and atomic Point interaction-state capture/restore | `JsxGraphEngine.kt`, `JsxGraphScene.kt` | Translated Point/Line/Circle/Curve/Polygon/Text/Arc/Sector/Angle document subset |
| `src/base/coords.js` | `Coords` | `base/Coords.kt` | Translated |
| `src/base/transformation.js` | static numeric 2D `translate`, `scale`, `reflect`, `rotate`, `shear`, `affine`, `affinematrix`, `generic`, and `matrix` construction; line/two-point reflection snapshots; `update`, `apply`, `applyOnce`, `clone`, and left-multiplying `melt`; dynamic parameters, `bindTo`/`meltTo`, the coordinate-element transformation lifecycle, transformed element factories, and all 3D forms remain pending | `base/Transformation.kt` | Partially translated |
| `src/base/element.js` | element identity, original/current type and name changes; draggable and regular-update state; standard/quadratic-form storage and normalization; direct children; transitive descendants/ancestors; explicit element/ID parents; JessieCode functional dependency edges; JessieCode `name`, `Name`, `getName`, `setName`, `Bounds`, `addChild`, and `needsRegularUpdate` access; child removal/counting; and the basic prepare/update/visibility/render/remove lifecycle | `base/GeometryElement.kt`, `parser/CoreGeometryElementRuntime.kt` | Translated dependency subset |
| `src/base/coordselement.js` | GeometryElement integration; coordinate storage; glider/constraint base state; JessieCode string-coordinate constraints with explicit evaluation errors and atomic dependency replacement; `X`, `Y`, `Z`, `Coords`, evaluator, distance accessors, immediate `move`/`moveTo`, Point `addConstraint`, and the translated JessieCode `methodMap` subset; and the free/constrained untransformed `updateCoords` / `setPositionDirectly` lifecycle | `base/CoordsElement.kt`, `parser/CoreGeometryElementRuntime.kt` | Translated dependency subset |
| `src/base/point.js` | numeric free-point and JessieCode string-coordinate construction, Board registration, coordinate update, numeric/string JessieCode `X`/`Y` assignment, bounds, `isOn` incidence checks for translated points, ordinary lines, and circle boundaries, plus native JessieCode creation from numeric or mixed numeric/string coordinates | `base/Point.kt`, `parser/NativeJessieCodeCreators.kt`, `parser/CoreGeometryElementRuntime.kt` | Translated dependency subset |
| `src/base/line.js` | two-registered-point construction and dependencies; native JessieCode creation from points, point names/IDs, coordinate arrays, or three numeric standard-form coefficients; `update`, `updateStdform`, `getRise`, `Slope`, `getAngle`, `Direction`, `isVertical`, `isHorizontal`, parametric `X`/`Y`/`Z`/`Ft` with `minX`/`maxX`, `L`, `bounds`, and the translated read-only JessieCode `methodMap` subset | `base/Line.kt`, `parser/NativeJessieCodeCreators.kt`, `parser/CoreGeometryElementRuntime.kt` | Translated dependency subset |
| `src/base/circle.js` | construction and dependencies for two registered points, a fixed numeric or JessieCode string radius, a line-derived radius, or a circle-derived radius; native JessieCode creation supports those parent forms in either upstream order and coordinate-array Point providers; `update`, standard/quadratic forms, cubic Bezier approximation, `Radius`, `Diameter`, parametric coordinates, `Area`, `Perimeter`, `bounds`, and the translated read-only JessieCode `methodMap` subset | `base/Circle.kt`, `parser/NativeJessieCodeCreators.kt`, `parser/CoreGeometryElementRuntime.kt` | Translated dependency subset |
| `src/base/curve.js`, `src/math/plot.js` | Curve identity and registration; discrete numeric X/Y data plots; explicit-domain string/number parametric expressions; FunctionGraph and Plot aliases; JessieCode dependency attachment; `X`, `Y`, `minX`, `maxX`, `update`, `updateCurve`, and `updateParametricCurveNaive` right-open sampling; adaptive plotting, function-valued terms, transforms, polar curves, cubic Bezier paths, fills, arrows, labels, and hit testing remain pending | `base/Curve.kt`, `parser/NativeJessieCodeCreators.kt` | Translated naive-sampling subset |
| `src/base/polygon.js` | Polygon identity; closed vertex list; Point and coordinate-array parents; upstream border Segment order; existing/helper Point dependency ownership; `withLines`; `Area`, `Perimeter`/`L`, bounds, vertices, and borders; transformations, mutable vertices, clipping/intersections, `polygonalchain`, nested styles, labels, and hit testing remain pending | `base/Polygon.kt`, `parser/NativeJessieCodeCreators.kt`, `parser/CoreGeometryElementRuntime.kt` | Translated construction and measurement subset |
| `src/base/text.js` | Text identity and registration; numeric/static content; dynamic `<value>` JessieCode content and dependency updates; free and constrained coordinates; `setText`; number formatting; explicit boundaries for rich text, function content, element anchors, rotation, measured bounds, and hit testing | `base/Text.kt`, `parser/NativeJessieCodeCreators.kt`, `parser/CoreGeometryElementRuntime.kt` | Translated plain-text subset |
| `src/element/arc.js` | three-point Arc construction from registered or coordinate-array Points; dependency ownership; `selection`, `orientation`, cubic Bezier updates, `Radius`, and `Value`; transformation form, direction-point form, labels, arrows, and hit testing remain pending | `base/Arc.kt`, `parser/NativeJessieCodeCreators.kt` | Translated three-point subset |
| `src/element/sector.js` | three-point Sector and Angle construction from registered or coordinate-array Points; dependency ownership; `selection`, `orientation`, fixed/auto Angle radius, parent reordering, and cubic Bezier sector updates; two-line forms, sub-elements, labels, alternate Angle displays, mutation, and hit testing remain pending | `base/Sector.kt`, `parser/NativeJessieCodeCreators.kt` | Translated three-point sector-display subset |
| `src/renderer/abstract.js`, `src/renderer/canvas.js`, `src/base/point.js -> hasPoint` | degree-one Curve paths and non-finite path breaks; degree-three cubic Bezier paths and filled closed curves; Polygon even-odd fills, default Segment borders, and implicit helper Points; plain Text with font size, color/opacity, and horizontal/vertical anchors; supported stroke/fill color, opacity, and width; visible free Point hit testing and drag pointer capture | `compose/JsxGraphGeometryPlayground.kt` | Translated Curve/Arc/Sector/Angle/Polygon/Text render and Point-interaction subset |
| `src/unused/symbolic.js` | `Symbolic` | None | Intentionally unsupported |

## License-Constrained Upstream Symbols

- `src/math/numerics.js -> glomin` is not imported. Its embedded source notice
  identifies that algorithm as GNU LGPL code, while this project only accepts
  the JSXGraph MIT license option. Translation requires a verified
  MIT-compatible provenance or an independent implementation.

## Update Procedure

1. Fetch the new public JSXGraph tag into `third_party/jsxgraph-src`.
2. Diff each mapped upstream source against the previous baseline.
3. Port behavioral changes into the mapped Kotlin file.
4. Add or update parity tests before changing the baseline recorded above.
5. Update this file, `AGENTS.md`, and `THIRD_PARTY_NOTICES.md` together.
