# Testing And Visual Parity

## Current Stack

- Common Kotlin tests use `kotlin.test`.
- Core tests run on JVM, iOS Simulator, and Wasm browser targets.
- `jsxgraph-compose` is a pure Compose Multiplatform renderer module.
- `jsxgraph-debug-ui` is a separate comparison dependency.
- The Android sample is a thin launcher for the debug UI.
- The Web sample is a thin Kotlin/Wasm launcher for the debug UI.
- Official JSXGraph `1.13.3` runs from local debug-only assets in an Android
  WebView or same-origin Web iframe.
- The native axis renderer uses bundled Arimo for the upstream Arial-compatible
  default instead of the device theme font.
- The parity corpus uses the production
  `JsxGraphEngine.parse -> Board -> JsxGraphScene` path. The official adapter
  converts each object in the same ordered source to
  `board.create(type, parents, attributes)`.
- Production artifacts do not use WebView or a JavaScript engine.

The repository does not use dependency injection, mocking, a database, or
navigation. Adding frameworks for those concerns would not improve the current
test surface.

## Commands

Use JDK 17 or newer.

```bash
./gradlew :jsxgraph-core:allTests
./gradlew :jsxgraph-compose:allTests :jsxgraph-compose:assemble
./gradlew :jsxgraph-debug-ui:allTests :jsxgraph-debug-ui:assemble
./gradlew :sample:androidApp:assembleDebug
./gradlew :sample:webApp:wasmJsBrowserDistribution
```

Capture and audit the current Web parity corpus:

```bash
npm ci --prefix tools/visual-parity
./gradlew :sample:webApp:wasmJsBrowserDistribution
python3 -m http.server 8093 \
  --directory sample/webApp/build/dist/wasmJs/productionExecutable

# Run these in another shell.
BASE_URL=http://127.0.0.1:8093/ \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/web-parity/current \
  npm --prefix tools/visual-parity run audit
```

The scheduled `Visual Parity` workflow runs the same audit at `1200 x 900` and
`390 x 844`, requires nontrivial captures, enforces a provisional board SSIM
floor of `0.90`, and uploads the PNG pairs, contact sheets, TSV summary, and
JSON report as workflow artifacts.

Capture the same source as source text, official JSXGraph, and Compose Canvas:

```bash
ANDROID_SERIAL=<device-serial> ./tools/capture-android-parity.sh
ANDROID_SERIAL=<device-serial> ./tools/capture-android-parity-matrix.sh
```

The script writes ignored local evidence to:

```text
captures/local/android-parity/current/
├── summary.tsv
└── baseline_geometry/
    ├── source.png
    ├── official.png
    ├── native.png
    ├── contact-sheet.png
    └── metrics.txt
```

The script reads the board bounds from Compose semantics and compares the
official and native board crops with FFmpeg SSIM. The provisional regression
floor is `0.90` and can be overridden with `MIN_BOARD_SSIM`. This coarse metric
detects large visual regressions; it does not replace contact-sheet review.
Use `PARITY_CASE_IDS` with comma- or space-separated case IDs to select a
corpus subset. Unknown IDs fail explicitly instead of falling back to the
default case.

`JsxGraphParityCorpus` is the source of truth for current slice cases. Its
documents contain `boundingBox` and ordered
`objects[{id,type,parents,attributes}]`; the debug UI no longer converts a
separate demo schema into handwritten native geometry. A case is added only
after the native implementation supports every feature declared by that case.
This slice corpus is separate from the future full JSXGraph stable corpus.

The Web audit uses `?audit=true&caseId=<id>&preview=official|native` to render
only the comparison board. This removes the surrounding debug UI from image
metrics while retaining the exact same source lookup and renderer adapters.

Latest construction-document evidence (2026-09-15):

| Case | Desktop SSIM | `390 x 844` SSIM |
|---|---:|---:|
| `baseline_geometry` | 0.987120 | 0.974780 |
| `finite_segment` | 0.987172 | 0.974567 |
| `coordinate_parents` | 0.986543 | 0.978321 |
| `shifted_geometry` | 0.986997 | 0.974236 |
| `curves` | 0.986698 | 0.975236 |
| `polygons` | 0.986562 | 0.973171 |
| `text` | 0.981600 | 0.954662 |

All captures passed the nonblank and browser-error checks. These numbers are
evidence for this translated slice only; they do not satisfy the full Stable
Gate. The Curve capture additionally verifies nonempty official SVG paths for
the function graph, parametric curve, and discrete data plot. The capture
harness rejects JSXGraph's `error compiling function` console warning so a
JessieCode CSP failure cannot pass as an empty official curve. The Polygon
capture verifies independent fill and default border styles, `withLines:
false`, and coordinate-array helper vertices.
The Text capture verifies static and numeric content, dynamic
`<value>` JessieCode evaluation, font size, stroke color/opacity, and all
translated horizontal/vertical anchor directions.

The matrix script captures these logical window profiles and restores the
device's previous size, density, and font scale on success or failure:

- `400 x 400 dp`
- `400 x 500 dp`
- `610 x 500 dp`
- `900 x 1000 dp`
- `400 x 500 dp` at `1.5` font scale

## Comparison Contract

Each parity case must contain one source document. Both the official adapter
and the native renderer consume that exact document. A case is invalid when
either side silently substitutes hard-coded geometry.

The official renderer is isolated in `jsxgraph-debug-ui` and must:

- load the pinned JSXGraph distribution from local assets;
- block network, file URL, content, popup, and mixed-content access;
- report explicit loading, ready, or error state;
- release its WebView and renderer process resources.

The native renderer must not depend on those assets, WebView, or JavaScript.

## Stable Gate

`stable` requires evidence across the supported corpus, not one successful
sample:

1. The source parses without fallback on both renderers.
2. Official and native captures are nonblank and report ready.
3. Geometry is compared for viewport, axes, object bounds, intersections, and
   relative placement.
4. Text, color, stroke, point, and interaction differences are reviewed.
5. Behavior tests cover source changes, tab changes, and state restoration.
6. Screen captures cover compact, medium, and expanded widths and representative
   heights, plus 1.5 font scale.
7. Android, iOS Simulator, JVM, and Wasm builds remain green.
8. Every accepted mismatch is documented; unreviewed visual drift fails the
   gate.

Current status is pre-stable. The initial case proves the pipeline but is not
evidence of full JSXGraph parity.
