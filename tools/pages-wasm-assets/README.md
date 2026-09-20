# Pages Wasm Assets

These content-addressed gzip chunks provide a CDN transport for the GitHub
Pages demo. `tools/split-wasm-for-pages.mjs` verifies that each reconstructed
payload exactly matches the corresponding production Wasm file before adding
its jsDelivr URLs to the generated manifest.

When a production Wasm hash changes, regenerate and replace its matching
`*.payload.part-*.bin` files. The generated Pages distribution also contains
same-origin copies for automatic fallback.
