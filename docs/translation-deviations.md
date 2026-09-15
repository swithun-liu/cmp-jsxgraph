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

- JessieCode tokenization and the translated expression parser return
  `GMResult.Err` when configured source-length, token-count, AST-node, or
  AST-depth limits are exceeded. The upstream generated lexer and Jison parser
  have no resource limits. A separate parser-nesting limit is capped at 64 so
  malformed recursive syntax is rejected before exhausting the browser Wasm
  stack.
- The translated parser currently accepts an empty program or one
  `ConditionalExpression ';' EOF` statement from the upstream
  `Expression ';' EOF` entry. Successful supported expressions preserve the
  upstream AST node/value/child shape, `isMath` flags, and generated-action
  locations. The evaluator covers every AST node produced by this subset,
  including JavaScript truthiness and primitive/array coercion, short-circuit
  logic, calls, properties, indexes, board element references, and the first
  core math built-ins. Remaining statements, assignment/function/map/object
  syntax, nested mutable scopes, the complete built-in set, and the complete
  element `methodMap` remain pending.
- JessieCode evaluation returns structured `GMResult.Err` values for malformed
  ASTs, unsupported operand combinations, unavailable element properties or
  values, and non-callable values instead of propagating JavaScript
  exceptions. Evaluation is capped by node-step and depth limits; the maximum
  configurable recursive depth is 64 to stay below the browser Wasm stack
  limit.
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
  calls, predefined constants, and reverse child traversal. It returns
  structured node/depth-limit errors instead of traversing without resource
  limits. Assignment-local discovery remains pending with the statement and
  mutable-scope grammar.
- `JessieCodeExpressionFunction` covers the string branch of
  `Type.createFunction`, including argument binding, stable references, and
  dependency metadata. Direct Kotlin number, array, and function adapters are
  deferred until a translated caller needs those parent forms.
- The core element runtime exposes the translated read-only `methodMap`
  subset for coordinate elements, lines, and circles. Mutating methods,
  visual-property fallback, generic `Value()`, and untranslated element
  classes return structured unavailable-property/value errors.
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
- `CoordsElement.setPositionDirectly` currently covers free elements without
  relative coordinates or transformations. Its snap-to-grid, snap-to-point,
  and attractor calls are lifecycle hooks with no-op defaults until the
  visual-property and attractor models are translated.
- `Point.create` currently accepts numeric free-point coordinates only and
  returns `GMResult.Err` for fewer than two coordinates or a board registration
  failure. JSXGraph throws from `createPoint` when its dynamic parent array
  cannot be interpreted as a free, constrained, or transformed point.
- `Point.isOn` currently supports translated `Point`, ordinary `Line`, and
  circle-boundary targets. Segment clipping, circle interior hits, curves,
  polygons, and turtles remain pending on their element and visual-property
  models.
- `Line.create` currently accepts two already registered `Point` instances from
  the same `Board`. It returns `GMResult.Err` for a cross-board or unregistered
  parent and for board registration failure. JSXGraph's dynamic `createLine`
  also accepts point IDs, coordinate arrays, functions, transformations, and
  three standard-form coordinates, and throws for unsupported parent values.
- `Line.getAngle(String)` returns `GMResult.Err(UnsupportedAngleUnit)` for an
  unknown unit. JSXGraph returns JavaScript `undefined`; valid unit prefixes
  and the no-unit radians result retain upstream behavior.
- `Circle.create` currently accepts an already registered center plus an
  already registered circumference `Point`, a fixed numeric or JessieCode
  string radius, an already registered `Line`, or an already registered source
  `Circle` from the same `Board`. String-radius compilation, first evaluation,
  and numeric validation return `GMResult.Err` before registration; later
  evaluation failures are available through `radiusResult()` and
  `radiusEvaluationError`, while the legacy numeric `Radius()` path returns
  `NaN`. JSXGraph can instead throw during expression execution or propagate
  JavaScript coercion. Its dynamic `createCircle` also accepts reversed parent
  order, point IDs, coordinate arrays, function radii, three-point
  circumcircles, and transformations.
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
  `Circle3DIntersection` snapshot. JSXGraph returns element-bound functions
  that recalculate center and radius; the future Plane3D/Sphere3D layer will
  provide that dynamic wrapper around these pure calculations.
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
