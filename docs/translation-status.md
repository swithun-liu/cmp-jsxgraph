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
| `src/utils/event.js` | `EventEmitter.trigger`, `on`, `off` | `utils/EventEmitter.kt` | Translated |
| `src/base/board.js` | coordinate origin, zoom, units, ID/name registration and selection, automatic element names, object/ancestor removal and creation order, recursive child cleanup and position reindexing, and the basic prepare/update/render/full/suspend lifecycle | `base/Board.kt` | Translated dependency subset |
| `src/base/coords.js` | `Coords` | `base/Coords.kt` | Translated |
| `src/base/element.js` | element identity, original/current type and name changes; direct children; transitive descendants/ancestors; explicit element/ID parents; child removal/counting; and the basic prepare/update/visibility/render/remove lifecycle | `base/GeometryElement.kt` | Translated dependency subset |
| `src/base/coordselement.js` | GeometryElement integration; coordinate storage; glider/constraint base state; `X`, `Y`, `Z`, `Coords`, evaluator, and distance accessors; and the free untransformed `updateCoords` / `setPositionDirectly` lifecycle | `base/CoordsElement.kt` | Translated dependency subset |
| `src/base/point.js` | numeric free-point construction, board registration, coordinate update, and point bounds | `base/Point.kt` | Translated dependency subset |
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
