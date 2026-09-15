# CMP JSXGraph

[![Quality Gate](https://github.com/swithun-liu/cmp-jsxgraph/actions/workflows/quality.yml/badge.svg?branch=main)](https://github.com/swithun-liu/cmp-jsxgraph/actions/workflows/quality.yml)
[![Web Progress](https://github.com/swithun-liu/cmp-jsxgraph/actions/workflows/deploy-pages.yml/badge.svg?branch=main)](https://github.com/swithun-liu/cmp-jsxgraph/actions/workflows/deploy-pages.yml)
[![Visual Parity](https://github.com/swithun-liu/cmp-jsxgraph/actions/workflows/visual-parity.yml/badge.svg)](https://github.com/swithun-liu/cmp-jsxgraph/actions/workflows/visual-parity.yml)
[![JSXGraph 1.13.3](https://img.shields.io/badge/JSXGraph-1.13.3-246BCE)](https://jsxgraph.org/)
[![MIT License](https://img.shields.io/badge/license-MIT-16877A)](LICENSE)

Pure Kotlin Multiplatform translation of JSXGraph with an early Compose
Multiplatform Canvas renderer.

**[Open the live Kotlin/Wasm progress site](https://swithun-liu.github.io/cmp-jsxgraph/)**
to inspect the roadmap and switch the same parity source between Source,
official JSXGraph `1.13.3`, and native Compose Canvas rendering.

Current parity cases:
[baseline](https://swithun-liu.github.io/cmp-jsxgraph/?openParity=true&caseId=baseline_geometry),
[tangent](https://swithun-liu.github.io/cmp-jsxgraph/?openParity=true&caseId=tangent_intersection),
[disjoint](https://swithun-liu.github.io/cmp-jsxgraph/?openParity=true&caseId=disjoint_intersection),
and
[shifted](https://swithun-liu.github.io/cmp-jsxgraph/?openParity=true&caseId=shifted_geometry).

## Status

This repository is under active development and is not ready for production
use. The current compatibility baseline is JSXGraph `1.13.3`.

Implemented translation slices:

- coordinate constants from `src/base/constants.js`;
- core numeric, matrix, combinatoric, special and probability functions from
  `src/math/math.js` and `src/math/probfuncs.js`;
- complex arithmetic from `src/math/complex.js`;
- statistics, random distributions, histogram generation and vectorized
  arithmetic from `src/math/statistics.js`;
- linear systems, determinants, symmetric eigensystems, fixed and adaptive
  integration, natural cubic splines, scalar and multidimensional root
  finding, complex polynomial roots, domain search, minimization, and
  Runge-Kutta ODE solvers from `src/math/numerics.js`;
- foundational angles, distances, orientation, transformations,
  perpendicular/circumcenter constructions, and analytic intersections from
  `src/math/geometry.js`;
- JessieCode lexical analysis, the first statement/expression AST/parser
  slices, and their interpreter from `src/parser/jessiecode.js`, including
  statement lists, blocks, `if`/`else`, `while`/`do`/`for`, assignment,
  return/delete statements, multi-Board `use`, literals, variables, mutable
  arrays and objects, function/map expressions, nested closures, calls,
  creator attribute lists with recursive merge semantics, properties,
  indexes, conditionals,
  JavaScript-compatible coercion, core math functions, static dependency
  discovery, source locations, stable name-to-ID replacement and current-name
  restoration, reusable expression functions, persistent JessieCode sessions,
  assignment-LHS creator naming, native Point/Line/Circle creation, the
  translated Point/Line/Circle read-only `methodMap` subset, and resource
  limits;
- Circle radii defined by JessieCode strings, including functional update
  dependencies that remain separate from geometric parents;
- Point coordinates defined by two or more JessieCode strings, including
  Euclidean and homogeneous updates;
- event emitter behavior from `src/utils/event.js`;
- homogeneous user/screen coordinate conversion from `src/base/coords.js`;
- an interactive Compose geometry playground backed by the translated
  line-circle intersection math.
- a separate `jsxgraph-debug-ui` comparison dependency with Source, official
  JSXGraph `1.13.3`, and native Compose previews.

The playground is a renderer and interaction test surface. The remaining
JessieCode creator registry, element mutation, complete built-in and element
`methodMap` surfaces, and the JSXGraph construction parser are not yet
translated, and the element renderer is incomplete.

Symbolic algebra (`src/unused/symbolic.js`) is intentionally out of scope for
the initial implementation.

## Translation Model

Kotlin implementations retain links to their upstream source files and
function names. This makes upstream JSXGraph changes reviewable as explicit
translation diffs instead of independent reimplementations.

See [docs/translation-status.md](docs/translation-status.md) for the exact
upstream commit and source-to-Kotlin mapping. Intentional edge-case
differences are tracked in
[docs/translation-deviations.md](docs/translation-deviations.md).
Production milestones and acceptance gates are tracked in
[docs/production-roadmap.md](docs/production-roadmap.md).

The intended runtime pipeline is:

```text
JSXGraph input
    -> Kotlin parser and JessieCode runtime
    -> translated Board, geometry, dependency and update engines
    -> platform-independent render model
    -> Compose Canvas
```

The runtime does not use WebView or an embedded JavaScript engine.

## Build

JDK 17 or newer is required.

```bash
./gradlew :jsxgraph-core:allTests
./gradlew :jsxgraph-compose:allTests :jsxgraph-debug-ui:allTests
./gradlew :sample:androidApp:assembleDebug
./gradlew :sample:webApp:wasmJsBrowserDistribution
ANDROID_SERIAL=<device-serial> ./tools/capture-android-parity.sh
```

See [`docs/testing.md`](docs/testing.md) for the screenshot parity and stable
gates.

## Coordinates

The project group and package namespace include the author name:

- package root: `com.swithun.jsxgraph`
- core artifact: `com.swithun:jsxgraph-core`
- Compose artifact: `com.swithun:jsxgraph-compose`
- debug comparison artifact: `com.swithun:jsxgraph-debug-ui`

## License

CMP JSXGraph is released under the MIT License. JSXGraph is used under its
MIT license option. See [LICENSE](LICENSE) and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
