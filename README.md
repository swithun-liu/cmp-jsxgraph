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
[finite segment](https://swithun-liu.github.io/cmp-jsxgraph/?openParity=true&caseId=finite_segment),
[coordinate parents](https://swithun-liu.github.io/cmp-jsxgraph/?openParity=true&caseId=coordinate_parents),
[shifted](https://swithun-liu.github.io/cmp-jsxgraph/?openParity=true&caseId=shifted_geometry),
[curves](https://swithun-liu.github.io/cmp-jsxgraph/?openParity=true&caseId=curves),
[polygons](https://swithun-liu.github.io/cmp-jsxgraph/?openParity=true&caseId=polygons),
[text](https://swithun-liu.github.io/cmp-jsxgraph/?openParity=true&caseId=text),
and
[arcs and sectors](https://swithun-liu.github.io/cmp-jsxgraph/?openParity=true&caseId=circular_regions).

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
  JavaScript-compatible coercion, core math functions, coordinate and geometry
  measurement built-ins, angle/combinatoric helpers, element naming and
  removal, `IfThen`, recursive `eval`, injectable `randint`, static dependency
  discovery, source locations, stable name-to-ID replacement and current-name
  restoration, reusable expression functions, persistent JessieCode sessions,
  assignment-LHS creator naming, native Point/Line/Circle/Curve/FunctionGraph/
  Plot/Polygon/Text/Arc/Sector/Angle creation, the translated
  Point/Line/Circle/Polygon/Text `methodMap` subset, Point `X`/`Y` assignment,
  element names, bounds, child links, immediate movement, coordinate
  constraints, static and `<value>` Text content, regular-update assignment,
  and resource limits;
- Circle radii defined by JessieCode strings, including functional update
  dependencies that remain separate from geometric parents;
- Point coordinates defined by two or more JessieCode strings, including
  Euclidean and homogeneous updates;
- event emitter behavior from `src/utils/event.js`;
- homogeneous user/screen coordinate conversion from `src/base/coords.js`;
- a bounded production construction-document path for `boundingBox` and
  ordered `objects[]`, currently creating Point, Line, Circle, Curve,
  FunctionGraph, Plot, Polygon, Text, Arc, Sector, and Angle elements through
  the translated Board and native creator registry;
- a platform-independent Point/Line/Circle/Curve/Polygon/Text/Arc/Sector/Angle
  render scene
  consumed by Compose, including discrete data plots, right-open naive sampling
  for explicit-domain function and parametric curves, cubic Bezier arcs,
  filled sectors, fixed-radius angles, filled/bordered polygons, and anchored
  Canvas text using a bundled Arial-compatible font;
- an interactive Compose geometry playground backed by the translated
  line-circle intersection math;
- a separate `jsxgraph-debug-ui` comparison dependency with Source, official
  JSXGraph `1.13.3`, and native Compose previews.

The playground is a renderer and interaction test surface. The remaining
JessieCode creator registry, visual-property and function-valued element
mutation, Slider/Glider-backed built-ins, `import`/`$log`/`D`, the complete
element `methodMap`, the remaining construction-document element types and
attributes, and the complete element renderer are not yet translated.

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

## Construction Source

The first production source contract maps directly to ordered
`Board.create(type, parents, attributes)` calls:

```json
{
  "schemaVersion": 1,
  "boundingBox": [-5, 5, 5, -5],
  "axis": true,
  "grid": true,
  "keepAspectRatio": true,
  "objects": [
    {
      "id": "A",
      "type": "point",
      "parents": [1, 2],
      "attributes": {"name": "", "withLabel": false}
    },
    {
      "id": "lineA",
      "type": "line",
      "parents": ["A", [3, -1]],
      "attributes": {"name": "", "withLabel": false}
    }
  ]
}
```

`JsxGraphEngine.parse(source)` returns
`GMResult<JsxGraphScene, JsxGraphDocumentError>`. Current accepted element
types are `point`, `line`, `circle`, `curve`, `functiongraph`, `plot`,
`polygon`, `text`, `arc`, `sector`, and `angle`. Continuous curves currently require
`doAdvancedPlot: false`; Polygon currently supports Point or coordinate-array
vertices, `withLines`, top-level fill styling, and default border/vertex
styles. Text supports static strings/numbers, dynamic `<value>` JessieCode
terms, constrained coordinates, number formatting, font size, color/opacity,
and horizontal/vertical anchors. Arc and Sector support three Point or
coordinate-array parents, minor/major/auto selection, both orientations, and
cubic Bezier rendering. Angle supports the three-point form with numeric or
`auto` radius and sector display. Unsupported types, parent forms, rich-text
features, plotting modes, nested styles, and visual attributes fail explicitly
instead of being omitted from the native render.

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
