# Production Roadmap

## Scope

- Upstream baseline: JSXGraph `1.13.3`
- Runtime: pure Kotlin Multiplatform
- Rendering: Compose Multiplatform Canvas
- Platforms: Android, iOS, JVM/Desktop, Wasm
- Explicit exclusion: `src/unused/symbolic.js`

The upstream baseline contains 104 JavaScript source files outside
`src/unused`. Completion is tracked by source symbol and behavior, not by raw
line count.

## Implementation Order

### 1. Foundation

- constants, coordinate conversion and event dispatch
- `JXG.Math`, probability functions and complex arithmetic
- shared result/error model
- upstream version and source mapping

Exit gate: common tests pass on JVM, iOS simulator and Wasm; all target
artifacts assemble.

### 2. Geometry And Numerics

- statistics, numerical integration/root finding and interpolation
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
- remaining 2D element factories

Exit gate: every supported upstream 2D factory has construction, update and
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

## Continuous Loop

For each upstream slice:

1. Record the source file and symbols.
2. Capture official reference outputs for representative and edge cases.
3. Translate the implementation without unrelated redesign.
4. Run focused tests, then all common tests and target assembly.
5. Audit the diff for private/internal content and license attribution.
6. Commit and push a reviewable batch.
