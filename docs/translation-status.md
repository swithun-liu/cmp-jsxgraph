# Translation Status

## Upstream Baseline

- JSXGraph version: `1.13.3`
- tag: `RELEASE-v1.13.3`
- commit: `7c2176d479ae256cb9d38265bce81fa18709d01f`
- license option: MIT

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
| `src/parser/jessiecode.js` | generated jison-lex token rules; generic AST nodes; empty programs and single expression statements covering literals, variables, arrays, calls, properties, indexes, conditionals, and unary/binary precedence; interpreter support for that expression subset, JavaScript primitive/array coercion, short-circuit evaluation, core math calls, board element lookup through a bounded adapter, and static dependency discovery; bounded lexing, parsing, evaluation, and dependency traversal; remaining statements, assignment/function/map/object syntax, nested mutable scopes, the complete built-in set, and the complete element `methodMap` remain pending | `parser/JessieCodeLexer.kt`, `parser/JessieCodeAst.kt`, `parser/JessieCodeExpressionParser.kt`, `parser/JessieCodeRuntime.kt`, `parser/JessieCodeEvaluator.kt`, `parser/JessieCodeDependencyCollector.kt` | Partially translated |
| `src/utils/event.js` | `EventEmitter.trigger`, `on`, `off` | `utils/EventEmitter.kt` | Translated |
| `src/base/board.js` | coordinate origin, zoom, units, ID/name registration and selection, automatic element names, object/ancestor removal and creation order, recursive child cleanup and position reindexing, and the basic prepare/update/render/full/suspend lifecycle | `base/Board.kt` | Translated dependency subset |
| `src/base/coords.js` | `Coords` | `base/Coords.kt` | Translated |
| `src/base/element.js` | element identity, original/current type and name changes; draggable state; standard/quadratic-form storage and normalization; direct children; transitive descendants/ancestors; explicit element/ID parents; child removal/counting; and the basic prepare/update/visibility/render/remove lifecycle | `base/GeometryElement.kt` | Translated dependency subset |
| `src/base/coordselement.js` | GeometryElement integration; coordinate storage; glider/constraint base state; `X`, `Y`, `Z`, `Coords`, evaluator, and distance accessors; and the free untransformed `updateCoords` / `setPositionDirectly` lifecycle | `base/CoordsElement.kt` | Translated dependency subset |
| `src/base/point.js` | numeric free-point construction, board registration, coordinate update, bounds, and `isOn` incidence checks for translated points, ordinary lines, and circle boundaries | `base/Point.kt` | Translated dependency subset |
| `src/base/line.js` | two-registered-point construction and dependencies; `update`, `updateStdform`, `getRise`, `Slope`, `getAngle`, `Direction`, `isVertical`, `isHorizontal`, parametric `X`/`Y`/`Z`/`Ft` with `minX`/`maxX`, `L`, and `bounds` | `base/Line.kt` | Translated dependency subset |
| `src/base/circle.js` | construction and dependencies for two registered points, a fixed numeric radius, a line-derived radius, or a circle-derived radius; `update`, standard/quadratic forms, cubic Bezier approximation, `Radius`, `Diameter`, parametric coordinates, `Area`, `Perimeter`, and `bounds` | `base/Circle.kt` | Translated dependency subset |
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
