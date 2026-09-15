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
  reader format. The current production subset creates Point, Line, and Circle
  through the translated native registry and then snapshots the resulting
  Board elements into a platform-independent scene. Unsupported element types,
  fields, attributes, point faces, labels, arrows, and dash styles return
  `JsxGraphDocumentError` instead of being ignored.
- Construction documents are limited by source length, JSON depth, JSON value
  count, and object count. JSON and factory failures are converted to
  `GMResult.Err`; object IDs are required and duplicate IDs are rejected.
  Colors currently accept CSS hex forms plus a small named-color subset.
  Top-level Point/Line/Circle defaults match JSXGraph `1.13.3`; helper Points
  created from coordinate-array parents remain in the internal Board but are
  omitted from the source-element scene, matching their role as sub-elements.
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
  adapter and gives them precedence over the native `point`, `line`, and
  `circle` registry. Attributes on an ordinary function return
  `UnexpectedCreatorAttributes` instead of throwing, and attribute
  nesting/collection growth shares the evaluator resource limits.
- Native JessieCode creators currently apply `id`, `name`, and
  `needsRegularUpdate`. Other visual and nested element attributes are
  evaluated and merged but not applied because the visual-property model is
  not translated yet. Invalid supported attribute types, unavailable Boards,
  unsupported parent combinations, and native factory failures return
  `CreatorFailure` instead of throwing.
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
  coordinate elements, lines, circles, and common element names, plus the
  bounded writable subset described above. `Bounds` and `addChild` preserve
  the translated element return values. `move` and `moveTo` accept numeric
  two- or three-coordinate arrays when the duration is omitted or zero, and
  Point `addConstraint` accepts arrays of number/string terms. Nonzero
  movement durations return `ElementMethodUnavailable` until the animation
  scheduler exists; function-valued constraints, remaining mutating methods,
  visual-property fallback, generic `Value()`, and untranslated element
  classes return structured unavailable-property/value errors.
- The translated JessieCode built-ins now include coordinate access,
  Line/Circle measurements, names, angles, binomial/GCD, `randint`, `IfThen`,
  recursive `eval`, and `remove`. `V`/`Value` delegates to
  `JessieCodeElementRuntime` until Slider/Glider exists, and area/perimeter
  currently accept Circle only until Polygon is translated. `randint` uses an
  injectable `RandomSource`; its default remains nondeterministic.
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
- `Point.create` accepts numeric free-point coordinates or a list of at least
  two JessieCode string coordinate expressions. The native JessieCode creator
  also accepts mixed numeric/string terms by preserving string expressions and
  converting numeric constants to JessieCode number sources. String-expression
  compilation, first evaluation, and numeric validation return `GMResult.Err`
  before registration. Later failures are exposed through
  `coordinateConstraintResult()` and `coordinateEvaluationError`; the regular
  update path writes `NaN` coordinate values instead of throwing or retaining
  stale geometry. Direct function and slider terms, a single function
  returning coordinates, and transformed points remain pending. JSXGraph
  throws from `createPoint` when its dynamic parent array cannot be interpreted
  as a free, constrained, or transformed point.
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
  JavaScript coercion. The native JessieCode creator resolves names/IDs,
  creates unnamed helper Points from coordinate arrays, and accepts the
  translated radius forms in either upstream order. Function radii,
  three-point circumcircles, and transformations remain pending.
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
