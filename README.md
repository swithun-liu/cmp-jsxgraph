# CMP JSXGraph

Pure Kotlin Multiplatform translation of JSXGraph with an early Compose
Multiplatform Canvas renderer.

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
- event emitter behavior from `src/utils/event.js`;
- homogeneous user/screen coordinate conversion from `src/base/coords.js`;
- an interactive Compose geometry playground backed by the translated
  line-circle intersection math.
- a separate `jsxgraph-debug-ui` comparison dependency with Source, official
  JSXGraph `1.13.3`, and native Compose previews.

The playground is a renderer and interaction test surface. It is not yet a
JSXGraph input parser or a complete element renderer.

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
