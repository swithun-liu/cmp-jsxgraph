# Translation Status

## Upstream Baseline

- JSXGraph version: `1.13.3`
- tag: `RELEASE-v1.13.3`
- commit: `7c2176d479ae256cb9d38265bce81fa18709d01f`
- license option: MIT

## Source Mapping

| Upstream source | Upstream symbols | Kotlin target | Status |
| --- | --- | --- | --- |
| `src/base/constants.js` | `COORDS_BY_USER`, `COORDS_BY_SCREEN` | `base/Const.kt` | Translated |
| `src/math/math.js` | `JXG.Math` | `math/Mat.kt` | Translated |
| `src/math/probfuncs.js` | `Mat.ProbFuncs` | `math/ProbFuncs.kt` | Translated |
| `src/math/complex.js` | `JXG.Complex`, `JXG.C` | `math/Complex.kt` | Translated |
| `src/math/statistics.js` | `Mat.Statistics` | `math/Statistics.kt` | Translated |
| `src/math/numerics.js` | linear algebra; Neville and Lagrange interpolation; polynomial generation and regression; cardinal, Catmull-Rom, Bezier, and B-splines; Riemann sums; Newton-Cotes, Romberg, Gauss-Legendre, Gauss-Kronrod, and QAG integration; natural cubic splines; numerical derivative; scalar, multidimensional, and complex polynomial roots; domain search; Brent minimization; Runge-Kutta ODE solvers; Ramer-Douglas-Peucker and Visvalingam curve simplification | `math/Numerics.kt` | Partially translated |
| `src/math/geometry.js` | angles, distances, affine ratios, orientation, reflection, rotation, perpendicular and circumcenter constructions, point-to-line/segment distance, and line/circle/segment intersections | `math/Geometry.kt` | Partially translated |
| `src/utils/event.js` | `EventEmitter.trigger`, `on`, `off` | `utils/EventEmitter.kt` | Translated |
| `src/base/board.js` | coordinate origin, zoom, units | `base/Board.kt` | Translated dependency subset |
| `src/base/coords.js` | `Coords` | `base/Coords.kt` | Translated |
| `src/base/coordselement.js` | coordinate storage and `X`, `Y`, `Z`, `Coords`, evaluator, and distance accessors | `base/CoordsElement.kt` | Translated dependency subset |
| `src/unused/symbolic.js` | `Symbolic` | None | Intentionally unsupported |

## Update Procedure

1. Fetch the new public JSXGraph tag into `third_party/jsxgraph-src`.
2. Diff each mapped upstream source against the previous baseline.
3. Port behavioral changes into the mapped Kotlin file.
4. Add or update parity tests before changing the baseline recorded above.
5. Update this file, `AGENTS.md`, and `THIRD_PARTY_NOTICES.md` together.
