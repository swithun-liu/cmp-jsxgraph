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
node tools/stability/generate-production-corpus.mjs
./gradlew :jsxgraph-core:allTests
./gradlew :jsxgraph-compose:allTests :jsxgraph-compose:assemble
./gradlew :jsxgraph-debug-ui:allTests :jsxgraph-debug-ui:assemble
./gradlew verifyPublicationCoordinates
./gradlew \
  :sample:androidApp:assembleDebug \
  :sample:androidApp:assembleRelease \
  :sample:desktopApp:createDistributable \
  :sample:webApp:wasmJsBrowserDistribution
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

Replay the production Point drag against both renderers:

```bash
BASE_URL=http://127.0.0.1:8093/ \
  OUTPUT_DIR=captures/local/web-interaction/current \
  PARITY_CASE_IDS=baseline_geometry \
  INTERACTION_TRACE=baseline_point_drag \
  npm --prefix tools/visual-parity run capture
INPUT_DIR=captures/local/web-interaction/current \
  PARITY_CASE_IDS=baseline_geometry \
  npm --prefix tools/visual-parity run audit
```

The scheduled `Visual Parity` workflow runs the same audit at `1200 x 900` and
`390 x 844`, requires nontrivial captures, enforces a provisional board SSIM
floor of `0.90` for the development corpus, repeats `baseline_point_drag`, and
uploads the PNG pairs, contact sheets, TSV summaries, and JSON reports as
workflow artifacts. Its independent production-corpus pass uses the Stable
floor of `0.93` at both viewports.

Capture and audit the independent production corpus:

```bash
CORPUS_SOURCE=production \
BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/stable-production/desktop \
VIEWPORT_WIDTH=1200 \
VIEWPORT_HEIGHT=900 \
npm --prefix tools/visual-parity run capture

CORPUS_SOURCE=production \
INPUT_DIR=captures/local/stable-production/desktop \
MIN_BOARD_SSIM=0.93 \
npm --prefix tools/visual-parity run audit
```

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

`JsxGraphParityCorpus` is the source of truth for the eight development parity
cases. Its documents contain `boundingBox` and ordered
`objects[{id,type,parents,attributes}]`; the debug UI no longer converts a
separate demo schema into handwritten native geometry. A case is added only
after the native implementation supports every feature declared by that case.

The independent Stable corpus is defined in
`tools/stability/production-corpus.mjs`. Its generator validates 24 unique
sources and 44 declared capability points, then emits separate Kotlin copies
for core tests and debug/runtime consumers. CI regenerates both copies and
rejects drift.

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
| `circular_regions` | 0.987047 | 0.974687 |

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
The circular-region capture verifies degree-three Bezier paths, Arc
selection/orientation, filled Sector geometry, and fixed-radius Angle
geometry.
The baseline Native preview additionally exposes its amber Point through the
production `JsxGraphSession` interaction path. Core behavior tests verify the
same Point movement, dependent constrained-Point/Line/Circle updates,
interaction-state capture/reset/restore, fixed/constrained rejection, and
atomic rollback when a drag would produce invalid geometry. Compose tests
verify upstream Point hit tolerance, visibility/fixed filtering, and reverse
creation-order priority.

The source-controlled `baseline_point_drag` trace moves the amber Point from
`(3.2, 2.1)` to `(1.1, 0.55)` in both renderers. The latest post-drag
Native/Official full-board SSIM was `0.984672` on Desktop and `0.973887` on
Compact; both profiles completed without browser errors.

Latest independent production evidence (2026-09-15):

| Profile | Cases | Lowest SSIM | Stable floor |
| --- | ---: | ---: | ---: |
| Desktop `1200 x 900` | 24/24 | 0.967798 | 0.93 |
| Compact `390 x 844` | 24/24 | 0.950843 | 0.93 |

The lowest case in both profiles is `prod_angle_auto_wedge`. Its geometry and
content were reviewed in the paged contact sheets linked from
[`stability-report.md`](stability-report.md).

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
5. Behavior tests cover source/session replacement, preview-tab retention,
   Point drag dependency updates, and interaction-state restoration.
6. Screen captures cover compact, medium, and expanded widths and representative
   heights, plus 1.5 font scale.
7. Android, iOS Simulator, JVM, and Wasm builds remain green.
8. Every accepted mismatch is documented; unreviewed visual drift fails the
   gate.
9. Independent scene and interaction replay is deterministic.
10. Generated stress and JVM soak tests remain inside their source-controlled
    element, time, P95, and retained-heap limits.
11. Android, iOS, Desktop, and Web run the same 24-case load screen to its
    final case.
12. Production Maven coordinates, MIT POM metadata, source-safety scans, and
    debug/release APK permission audits pass.

All gates above pass for the documented Point/Line/Circle/Curve/Polygon/Text/
Arc/Sector/Angle construction, rendering, and Point-interaction scope. That
scope is rated **Stable**. Unsupported JSXGraph APIs and element families
remain outside the rating and fail explicitly where they cross the production
document boundary.
