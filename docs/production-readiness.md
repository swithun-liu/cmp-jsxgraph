# Production Code Readiness

This document defines the code-level evidence required for CMP JSXGraph to be
rated Stable. A successful build or a single visual demo is not sufficient.

## Current State

- JSXGraph compatibility baseline: `1.13.3`
- Native production modules: `jsxgraph-core` and `jsxgraph-compose`
- Runtime implementation: Kotlin Multiplatform parser, translated Board and
  geometry model, scene snapshots, and Compose Canvas rendering
- Official JSXGraph usage: isolated in `jsxgraph-debug-ui`
- Release status: **Stable for the documented support scope**

The Stable scope is the bounded construction-document API for Point, Line,
Circle, Curve, FunctionGraph, Plot, Polygon, Text, Arc, Sector, and Angle,
including free Point drag, dependent updates, and interaction-state
capture/restore. The remaining JSXGraph API and element families are not
included.

## Expected Behavior

Stable means supported input can be parsed, rendered, and interacted with in a
production application without WebView or a JavaScript engine in the
production path. Results are deterministic, resource use is bounded, expected
failures use `GMResult`, unsupported behavior is documented, and no known
high-severity defect remains in the supported contract.

## Promotion Gates

| Gate | Requirement | Current evidence | Status |
| --- | --- | --- | --- |
| Visual fidelity | No blocked mismatch in the independent production corpus | 24 Native/Official pairs at Desktop and Compact; 48/48 pass SSIM `0.93` | Passing |
| Capability coverage | Every declared production capability appears in an independent case | 44/44 capability points across 24 scenarios | Passing |
| Determinism | Repeated parsing returns the same complete scene | 24/24 scene replays | Passing |
| Interaction replay | Move, capture, restore, and reset are repeatable | 2/2 production interaction scenarios plus 64 generated updates | Passing |
| Parser/render robustness | Deterministic generated inputs stay finite and bounded | 512 unique Native stress inputs | Passing |
| Core throughput | 720 warmed renders complete within 45s and P95 is at most 500ms | Local baseline: 121ms total, 1.185ms P95 | Passing |
| Core retained heap | The same soak retains at most 64MiB after forced GC | Local baseline: 0 retained bytes | Passing |
| Runtime matrix | Android, iOS Simulator, Desktop, and Web reach the final shared load case | All four platforms completed the 24-case traversal | Passing locally |
| Build matrix | Production modules and samples compile on supported targets | Android debug/release, Desktop, Web, iOS device/simulator targets | Passing locally |
| Publication | Production coordinates and MIT POM metadata are exact | `com.swithun:jsxgraph-core:0.1.0` and `com.swithun:jsxgraph-compose:0.1.0` | Passing |
| Public-source safety | Published source and artifacts contain no private endpoint or credential material | Repository scan plus Android debug/release permission audit | Passing |

## Production Boundaries

The production dependency graph is:

```text
application
    -> jsxgraph-compose
        -> jsxgraph-core
```

`jsxgraph-debug-ui` is not a production dependency and is excluded from the
production publication set. It alone contains the pinned official JSXGraph
assets and the Android WebView/same-origin Web iframe comparison adapters.
Neither production module depends on WebView, JavaScriptCore, QuickJS, or a
browser JavaScript runtime.

Construction and JessieCode inputs are bounded by source length, JSON depth
and value count, object count, parser depth, evaluation steps, collection
growth, curve sample count, polygon vertex count, and text length. Invalid or
unsupported input returns a structured `GMResult.Err`; it is not silently
replaced with different geometry.

## Performance And Runtime Gate

The core soak measures parse, translated Board creation, dependency updates,
and scene generation after three warmup rounds. Canvas/runtime measurements
remain separate because Compose, Skia, device density, and GPU behavior are
platform-specific.

| Platform | Corpus | Latency | Memory | Outcome |
| --- | ---: | --- | --- | --- |
| Android Emulator | 24 | Automatic traversal reached final case | 119.9MiB PSS; 210.0MiB RSS; WebViews 0 | Passed |
| iOS Simulator | 24 | Automatic traversal reached final case | 279.7MiB observed peak host RSS; 195.9MiB final | Passed |
| Desktop JVM | 24 | Process survived beyond automatic traversal | 233.0MiB RSS | Passed |
| Web | 24 | 352ms first content; 1.662s traversal | 7.61MiB retained JS heap | Passed 15s/96MiB budget |

The Desktop run has machine-readable process evidence but no current
screenshot because macOS screen-recording permission prevented window capture.

## Integration Guidance

A production adopter should:

- handle `JsxGraphDocumentError` and `JsxGraphInteractionError` without retry
  loops;
- retain the default engine limits unless a larger trusted document requires
  an explicit increase;
- record duration and error category without logging source text;
- preserve a fallback to source text or another safe representation;
- keep `jsxgraph-debug-ui` in debug or audit configurations only.

Application canaries and rollout controls remain useful, but they belong to
the adopting application and are not part of this repository's code-level
rating.

## Stable Rating Rule

Stable applies only while every gate above passes, the latest evidence is
linked from [`stability-report.md`](stability-report.md), and no open
severity-1 correctness, crash, resource, licensing, or data-exposure defect is
known in the supported contract.

All current code-level gates pass for the documented JSXGraph `1.13.3` subset.
CMP JSXGraph is therefore rated **Stable for that support scope**. This does
not imply complete JSXGraph compatibility; unsupported features remain listed
in [`translation-status.md`](translation-status.md) and
[`translation-deviations.md`](translation-deviations.md).
