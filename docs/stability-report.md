# JSXGraph 1.13.3 Stable Test Report

This report records the evidence behind CMP JSXGraph's **Stable** rating for
the documented support scope. Independent production scenarios, generated
Native stress inputs, and the earlier development parity corpus are separate
evidence layers and are not counted as substitutes for each other.

## Decision

| Item | Result |
| --- | --- |
| Current rating | **Stable for the documented support scope** |
| JSXGraph compatibility baseline | `1.13.3` |
| Independent production scenarios | 24 |
| Declared capability coverage | 44/44 |
| Native core results | 24 production cases plus 512 generated stress inputs passed |
| Web Native/Official captures | 48: 24 Desktop plus 24 Compact |
| Automated visual parity | 48/48 passed SSIM `0.93` |
| Lowest Desktop SSIM | 0.967798, `prod_angle_auto_wedge` |
| Lowest Compact SSIM | 0.950843, `prod_angle_auto_wedge` |
| Deterministic scene replay | 24/24 |
| Deterministic interaction replay | 2/2 |
| Generated interaction updates | 64/64 |
| Core production soak | 720 renders; 121ms total; 1.185ms P95; 0 retained bytes |
| Runtime load matrix | Android Emulator, iOS Simulator, Desktop, and Web passed |
| Android Internet permission | Not declared in debug or release APK |
| Public-source safety scan | No organization-specific endpoint or credential pattern found |

**Conclusion:** every gate in
[`production-readiness.md`](production-readiness.md) passes for the bounded
Point, Line, Circle, Curve, FunctionGraph, Plot, Polygon, Text, Arc, Sector,
Angle, and Point-interaction contract. This scope is rated **Stable**.
Untranslated JSXGraph APIs and element families remain explicitly outside the
rating.

## What This Report Proves

The source-controlled production corpus:

- parses through `JsxGraphEngine` and the translated Board/creator pipeline;
- produces finite, non-empty, deterministic `JsxGraphScene` values;
- is rendered from the same source by Native Compose Canvas and the pinned
  official JSXGraph `1.13.3` audit renderer;
- completes deterministic Point move, capture, restore, and reset scenarios;
- remains inside source-controlled parser, scene, latency, and heap budgets;
- reaches the final shared load case on Android, iOS, Desktop, and Web.

The report does not claim every legal JSXGraph program is supported. SSIM
detects visual regression but does not prove semantic equivalence by itself.
Platform font rasterization and small antialiasing differences remain
expected. Unsupported source crosses the production boundary as
`GMResult.Err` instead of being silently replaced by different geometry.

## Evidence Layers

1. **24 independent production scenarios.** Hand-authored operational boards
   used for capability coverage, deterministic replay, visual parity, runtime
   loading, and the regular Quality Gate.
2. **512 deterministic Native stress inputs.** Eight generated document
   families exercise geometry, curves, plots, polygons, text, circular
   regions, and interaction updates. These are robustness evidence, not
   official-renderer parity claims.
3. **Eight development parity scenarios.** These remain focused regression
   fixtures for individual implementation batches and the source/official/
   native debug workflow.

The canonical production corpus is
[`tools/stability/production-corpus.mjs`](../tools/stability/production-corpus.mjs).
[`generate-production-corpus.mjs`](../tools/stability/generate-production-corpus.mjs)
validates IDs, source uniqueness, expected elements, interaction references,
and 44-feature coverage before generating independent Kotlin copies for core
tests and debug/runtime consumers. CI rejects generated-file drift.

## Production Corpus

| Category | Cases | Main coverage |
| --- | ---: | --- |
| Geometry | 4 | free/fixed/hidden Points, infinite Lines, Segments, Circles, coordinate and element parents, shifted/stretched boards |
| Curves | 4 | FunctionGraph, parametric Curve, sampled data Plot, mixed functions |
| Polygons | 4 | convex, concave, borderless, implicit and shared vertices |
| Text | 4 | static, numeric, dynamic, anchored, colored and translucent text |
| Circular regions | 5 | minor/major/clockwise Arc, Sector, fixed and automatic Angle radius |
| Interaction | 2 | Point-driven Line and Circle dependency updates plus state replay |
| Mixed board | 1 | Point, Line, Circle, Polygon, Plot, and Text in one scene |

## Visual Evidence

Every page contains eight same-source pairs. The contact images label each
case and its measured SSIM.

### Desktop `1200 x 900`

![Desktop production parity page 1](assets/stability-report/production-desktop-page-1.png)

![Desktop production parity page 2](assets/stability-report/production-desktop-page-2.png)

![Desktop production parity page 3](assets/stability-report/production-desktop-page-3.png)

Machine-readable results:
[`production-desktop-report.json`](assets/stability-report/production-desktop-report.json).

### Compact `390 x 844`

![Compact production parity page 1](assets/stability-report/production-compact-page-1.png)

![Compact production parity page 2](assets/stability-report/production-compact-page-2.png)

![Compact production parity page 3](assets/stability-report/production-compact-page-3.png)

Machine-readable results:
[`production-compact-report.json`](assets/stability-report/production-compact-report.json).

All 48 pairs passed the `0.93` Stable floor. The contact pages show no missing
source element, blank board, clipping, or geometry displacement that blocks
the supported contract. The largest remaining difference is the automatic
Angle wedge, where browser SVG and Compose Canvas rasterize the small filled
Bezier region differently; both profiles remain above the floor.

## Determinism And Performance

`ProductionCorpusTest` compares complete scene values across repeated parses,
including bounds, element order, geometry, text, and styles. The two
interaction documents also compare moved scenes with a fresh session restored
from captured state, then verify reset returns the exact initial scene.

`GeneratedStressTest` creates 512 unique documents from fixed seed
`0x4A535847`, parses every one, rejects non-finite or oversized scenes, and
moves the `driver` Point in all 64 generated interaction cases.

The JVM production soak performs three warmup rounds followed by 30 measured
rounds over all 24 scenarios:

```text
renders=720
totalMs=121
p95Micros=1185
retainedHeapBytes=0
```

Enforced budgets are 45 seconds total, 500ms P95, and 64MiB retained heap
after forced GC.

## Runtime Load Matrix

The shared `StableLoadScreen` parses the same 24 sources and traverses from the
first geometry case to `prod_mixed_operations_board`.

| Platform | Result | Local evidence |
| --- | --- | --- |
| Android Emulator | Passed | Final case visible; 125,681,664-byte PSS; 220,151,808-byte RSS; WebViews 0 |
| iOS Simulator | Passed | Final case visible; 293,322,752-byte observed peak host RSS; 205,438,976-byte final RSS |
| Desktop JVM | Passed | Process survived beyond automatic traversal; 244,301,824-byte RSS |
| Web | Passed | 352ms first content; 1.662s traversal; 7,978,584-byte retained JS heap; no browser errors |

Machine-readable measurements:
[Android](assets/runtime-load/android-emulator-metrics.json),
[iOS](assets/runtime-load/ios-simulator-metrics.json),
[Desktop](assets/runtime-load/desktop-metrics.json), and
[Web](assets/runtime-load/web-metrics.json).

| Android Emulator | iOS Simulator |
| :---: | :---: |
| <img src="assets/runtime-load/android-emulator-bottom.png" alt="Android Stable load final case" width="360"> | <img src="assets/runtime-load/ios-simulator-bottom.png" alt="iOS Stable load final case" width="360"> |

| Web |
| :---: |
| <img src="assets/runtime-load/web-bottom.png" alt="Web Stable load final case" width="700"> |

The Desktop run has machine-readable process evidence but no current screenshot
because macOS screen-recording permission prevented window capture.

## Build, Publication, And Safety

The final gate covers:

- all tests for `jsxgraph-core`, `jsxgraph-compose`, and
  `jsxgraph-debug-ui`;
- Android debug/release APKs;
- Desktop distributable;
- Web production distribution;
- iOS Arm64, Simulator Arm64, and X64 compilation plus the iOS sample;
- production POM coordinates and MIT metadata;
- generated-corpus drift;
- public-source and credential patterns;
- Android debug/release APK permissions.

Production publication coordinates are:

```text
com.swithun:jsxgraph-core:0.1.0
com.swithun:jsxgraph-compose:0.1.0
```

`jsxgraph-debug-ui` is not published as a production module. The Android
sample's debug and release APKs do not declare
`android.permission.INTERNET`. The official JSXGraph distribution, WebView,
and iframe adapters remain isolated in debug UI and are absent from the
production module dependency graph.

The security review of this change set found no attacker-controlled path to a
command, filesystem, XML, browser-HTML, authentication, cryptographic, or
sensitive-data sink. Build scripts consume source-controlled files and trusted
CI environment values. POM XML parsing rejects document type declarations.

## Reproduce The Report

Use JDK 17 or newer:

```bash
node tools/stability/generate-production-corpus.mjs

./gradlew \
  :jsxgraph-core:allTests \
  :jsxgraph-compose:allTests \
  :jsxgraph-debug-ui:allTests \
  verifyPublicationCoordinates \
  :sample:androidApp:assembleDebug \
  :sample:androidApp:assembleRelease \
  :sample:desktopApp:createDistributable \
  :sample:webApp:wasmJsBrowserDistribution
```

Serve the Web distribution:

```bash
python3 -m http.server 8093 \
  --directory sample/webApp/build/dist/wasmJs/productionExecutable
```

In another shell:

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

BASE_URL=http://127.0.0.1:8093/ \
OUTPUT_DIR=captures/local/web-load/current \
npm --prefix tools/visual-parity run test:load
```

The capture command rejects missing/nontrivial Native Canvas and Official SVG
output plus browser errors. The audit compares board crops with FFmpeg SSIM.
The contact-page generator verifies every expected pair before producing the
paged evidence images.
