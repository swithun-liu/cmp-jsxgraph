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
- `Complex.toString(digits)` uses a common Kotlin fixed-decimal formatter.
  Extremely large values and unsupported digit counts do not reproduce
  JavaScript `Number.toFixed` exceptions byte-for-byte.
- Native math functions can differ from JavaScript by a few final binary
  digits. Official-reference assertions use narrow numeric tolerances.

## Safety Guards

The following upstream edge cases can loop indefinitely or recurse without a
base case. The Kotlin translation returns `NaN` or the mathematically defined
result instead:

- `factorial(NaN)` and `factorial(Infinity)` return `NaN`.
- `binomial` returns `NaN` for non-finite inputs.
- `gcd(0, x)` returns `abs(x)` and non-finite inputs return `NaN`.
- `squampow` uses arithmetic halving rather than JavaScript 32-bit bitwise
  coercion for large integer exponents.
