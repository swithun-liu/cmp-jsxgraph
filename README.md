# CMP JSXGraph

Pure Kotlin Multiplatform translation of JSXGraph, with Compose Multiplatform
rendering planned through Compose Canvas.

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
- linear systems, determinants, symmetric eigensystems, integration, natural
  cubic splines, root finding, domain search and minimization from
  `src/math/numerics.js`;
- event emitter behavior from `src/utils/event.js`;
- homogeneous user/screen coordinate conversion from `src/base/coords.js`.

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
```

## Coordinates

The project group and package namespace include the author name:

- package root: `com.swithun.jsxgraph`
- core artifact: `com.swithun:jsxgraph-core`
- Compose artifact (planned): `com.swithun:jsxgraph-compose`

## License

CMP JSXGraph is released under the MIT License. JSXGraph is used under its
MIT license option. See [LICENSE](LICENSE) and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
