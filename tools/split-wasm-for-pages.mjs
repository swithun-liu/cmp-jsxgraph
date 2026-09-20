import { createHash } from 'node:crypto';
import { readdir, readFile, rm, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { gzipSync } from 'node:zlib';

const CHUNK_SIZE_BYTES = 512 * 1024;
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

for (const wasmFile of wasmFiles) {
  const wasmPath = resolve(outputDirectory, wasmFile);
  const wasmBytes = await readFile(wasmPath);
  const compressedWasmBytes = gzipSync(wasmBytes, { level: 9 });
  const chunks = await writeChunks(wasmFile, wasmBytes);
  const wasmAssetId = wasmFile.slice(0, -'.wasm'.length);
  const compressedChunks = await writeScriptChunks(
    `${wasmAssetId}.payload`,
    compressedWasmBytes,
  );

  await writeFile(
    `${wasmPath}.chunks.json`,
    `${JSON.stringify({
      byteLength: wasmBytes.length,
      chunks,
      compression: {
        format: 'gzip-base64-script',
        byteLength: compressedWasmBytes.length,
        chunks: compressedChunks,
      },
    })}\n`,
  );
  console.log(
    `Split ${wasmFile} into ${chunks.length} raw and ` +
      `${compressedChunks.length} gzip script chunks.`,
  );
}

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

async function writeScriptChunks(fileName, bytes) {
  const chunks = [];
  for (
    let offset = 0, index = 0;
    offset < bytes.length;
    offset += CHUNK_SIZE_BYTES, index += 1
  ) {
    const chunkName =
      `${fileName}.part-${index.toString().padStart(3, '0')}.js`;
    const base64 = Buffer.from(
      bytes.subarray(offset, offset + CHUNK_SIZE_BYTES),
    ).toString('base64');
    const source =
      `globalThis.__cmpJsxGraphRegisterWasmChunk(` +
      `${JSON.stringify(chunkName)},${JSON.stringify(base64)});\n`;
    await writeFile(resolve(outputDirectory, chunkName), source);
    chunks.push(chunkName);
  }
  return chunks;
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
