# Translation Status

## Upstream Baseline

- JSXGraph version: `1.13.3`
- tag: `RELEASE-v1.13.3`
- commit: `7c2176d479ae256cb9d38265bce81fa18709d01f`
- license option: MIT

## Source Mapping

| Upstream source | Upstream symbols | Kotlin target | Status |
| --- | --- | --- | --- |
| `src/base/constants.js` | `COORDS_BY_USER`, `COORDS_BY_SCREEN` | `base/Const.kt` | Translated |
| `src/math/math.js` | `eps`, `hypot` | `math/Mat.kt` | Translated dependency subset |
| `src/utils/event.js` | `EventEmitter.trigger`, `on`, `off` | `utils/EventEmitter.kt` | Translated |
| `src/base/board.js` | coordinate origin, zoom, units | `base/Board.kt` | Translated dependency subset |
| `src/base/coords.js` | `Coords` | `base/Coords.kt` | Translated |
| `src/math/symbolic.js` | `Symbolic` | None | Intentionally unsupported |

## Update Procedure

1. Fetch the new public JSXGraph tag into `third_party/jsxgraph-src`.
2. Diff each mapped upstream source against the previous baseline.
3. Port behavioral changes into the mapped Kotlin file.
4. Add or update parity tests before changing the baseline recorded above.
5. Update this file, `AGENTS.md`, and `THIRD_PARTY_NOTICES.md` together.
