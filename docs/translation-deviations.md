# Translation Deviations

This file records intentional behavior differences from JSXGraph `1.13.3`.
Every difference must be explicit, narrow, and covered by tests where
practical.

## Unsupported

- `src/unused/symbolic.js`: Symbolic/CAS operations and symbolic locus
  derivation are not implemented.

## Kotlin Runtime Adaptations

- Numeric vectors and matrices use `DoubleArray` and `Array<DoubleArray>`.
  Malformed dimensions are outside the internal contract and may fail
  differently from malformed JavaScript arrays.
- `EventEmitter` passes the registered context as an explicit callback
  argument because Kotlin has no dynamic JavaScript `this`.
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
- The translated geometry primitives accept homogeneous coordinate and
  standard-form arrays directly. JSXGraph's `Point`, `Line`, `Circle`, and
  `Coords` overloads will wrap these functions when the element model is
  translated; `PerpendicularPointRole` preserves identity-dependent branches.
- `Numerics.Romberg` honors its documented default configuration. JSXGraph
  `1.13.3` dereferences `config.eps` when the optional config is omitted.
- `Complex.toString(digits)` uses a common Kotlin fixed-decimal formatter.
  Extremely large values and unsupported digit counts do not reproduce
  JavaScript `Number.toFixed` exceptions byte-for-byte.
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
