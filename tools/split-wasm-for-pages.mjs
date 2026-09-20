import { createHash } from 'node:crypto';
import { readdir, readFile, rm, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { gzipSync } from 'node:zlib';

const CHUNK_SIZE_BYTES = 512 * 1024;
const EMBEDDED_WASM_MARKER = '<!-- CMP_JSXGRAPH_EMBEDDED_WASM -->';
const outputDirectory = process.argv[2];

if (!outputDirectory) {
  throw new Error('Expected the Web distribution directory');
}

const outputFiles = await readdir(outputDirectory);
await Promise.all(
  outputFiles
    .filter((fileName) =>
      /\.wasm\.part-\d+$|\.payload\.part-\d+(?:\.json|\.js)?$/.test(fileName),
    )
    .map((fileName) => rm(resolve(outputDirectory, fileName))),
);

const wasmFiles = outputFiles
  .filter((fileName) => fileName.endsWith('.wasm'))
  .sort();

if (wasmFiles.length === 0) {
  throw new Error(`No Wasm files found in ${outputDirectory}`);
}

const embeddedAssets = {};
for (const wasmFile of wasmFiles) {
  const wasmPath = resolve(outputDirectory, wasmFile);
  const wasmBytes = await readFile(wasmPath);
  const compressedWasmBytes = gzipSync(wasmBytes, { level: 9 });
  const compressedBase64Chunks = encodeChunks(compressedWasmBytes);
  const chunks = await writeChunks(wasmFile, wasmBytes);
  const wasmAssetId = wasmFile.slice(0, -'.wasm'.length);
  const compressedChunks = await writeScriptChunks(
    `${wasmAssetId}.payload`,
    compressedBase64Chunks,
  );
  const manifest = {
    byteLength: wasmBytes.length,
    chunks,
    compression: {
      format: 'gzip-base64-script',
      byteLength: compressedWasmBytes.length,
      chunks: compressedChunks,
    },
  };

  await writeFile(
    `${wasmPath}.chunks.json`,
    `${JSON.stringify(manifest)}\n`,
  );
  embeddedAssets[wasmFile] = {
    ...manifest,
    compression: {
      format: 'gzip-base64-embedded',
      byteLength: compressedWasmBytes.length,
      chunks: compressedBase64Chunks,
    },
  };
  console.log(
    `Split ${wasmFile} into ${chunks.length} raw and ` +
      `${compressedChunks.length} gzip script chunks.`,
  );
}

await embedWasmAssets({
  schemaVersion: 1,
  assets: embeddedAssets,
});
await versionEntryScript();

async function writeChunks(fileName, bytes) {
  const chunks = [];
  for (
    let offset = 0, index = 0;
    offset < bytes.length;
    offset += CHUNK_SIZE_BYTES, index += 1
  ) {
    const chunkName = `${fileName}.part-${index.toString().padStart(3, '0')}`;
    await writeFile(
      resolve(outputDirectory, chunkName),
      bytes.subarray(offset, offset + CHUNK_SIZE_BYTES),
    );
    chunks.push(chunkName);
  }
  return chunks;
}

function encodeChunks(bytes) {
  const chunks = [];
  for (
    let offset = 0;
    offset < bytes.length;
    offset += CHUNK_SIZE_BYTES
  ) {
    chunks.push(
      Buffer.from(
        bytes.subarray(offset, offset + CHUNK_SIZE_BYTES),
      ).toString('base64'),
    );
  }
  return chunks;
}

async function writeScriptChunks(fileName, base64Chunks) {
  const chunks = [];
  for (const [index, base64] of base64Chunks.entries()) {
    const chunkName =
      `${fileName}.part-${index.toString().padStart(3, '0')}.js`;
    const source =
      `globalThis.__cmpJsxGraphRegisterWasmChunk(` +
      `${JSON.stringify(chunkName)},${JSON.stringify(base64)});\n`;
    await writeFile(resolve(outputDirectory, chunkName), source);
    chunks.push(chunkName);
  }
  return chunks;
}

async function embedWasmAssets(payload) {
  const indexPath = resolve(outputDirectory, 'index.html');
  const indexHtml = await readFile(indexPath, 'utf8');
  const embeddedScript =
    /<script id="cmp-jsxgraph-embedded-wasm" type="application\/json">[^<]*<\/script>/;
  const target = indexHtml.includes(EMBEDDED_WASM_MARKER)
    ? EMBEDDED_WASM_MARKER
    : embeddedScript;
  if (
    typeof target !== 'string' &&
    !target.test(indexHtml)
  ) {
    throw new Error(`Could not find Wasm marker in ${indexPath}`);
  }
  const embeddedPayload =
    `    <script id="cmp-jsxgraph-embedded-wasm" ` +
    `type="application/json">${JSON.stringify(payload)}</script>`;
  await writeFile(
    indexPath,
    indexHtml.replace(target, embeddedPayload),
  );
  console.log(
    `Embedded ${Object.keys(payload.assets).length} Wasm assets in index.html.`,
  );
}

async function versionEntryScript() {
  const entryScriptName = 'cmp-jsxgraph.js';
  const entryScript = await readFile(
    resolve(outputDirectory, entryScriptName),
  );
  const version = createHash('sha256')
    .update(entryScript)
    .digest('hex')
    .slice(0, 12);
  const indexPath = resolve(outputDirectory, 'index.html');
  const indexHtml = await readFile(indexPath, 'utf8');
  const scriptSource = /src="cmp-jsxgraph\.js(?:\?v=[^"]*)?"/;
  if (!scriptSource.test(indexHtml)) {
    throw new Error(`Could not find ${entryScriptName} in ${indexPath}`);
  }
  await writeFile(
    indexPath,
    indexHtml.replace(
      scriptSource,
      `src="${entryScriptName}?v=${version}"`,
    ),
  );
  console.log(`Versioned ${entryScriptName} as ${version}.`);
}
