import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { copyFile, readdir, readFile, rm, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { deflateSync, gunzipSync, gzipSync } from 'node:zlib';

const CHUNK_SIZE_BYTES = 512 * 1024;
const PNG_WIDTH_PIXELS = 1024;
const PNG_SIGNATURE = Buffer.from([
  0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
]);
const CRC_TABLE = createCrcTable();
const outputDirectory = process.argv[2];
const repositoryRoot = resolve(
  fileURLToPath(new URL('..', import.meta.url)),
);
const prebuiltAssetDirectory = resolve(
  repositoryRoot,
  'tools/pages-wasm-assets',
);
const repositoryRevision = execFileSync(
  'git',
  ['rev-parse', 'HEAD'],
  { cwd: repositoryRoot, encoding: 'utf8' },
).trim();
const cdnBaseUrl =
  `https://cdn.jsdelivr.net/gh/swithun-liu/cmp-jsxgraph@` +
  `${repositoryRevision}/tools/pages-wasm-assets`;

if (!outputDirectory) {
  throw new Error('Expected the Web distribution directory');
}

const outputFiles = await readdir(outputDirectory);
await Promise.all(
  outputFiles
    .filter((fileName) =>
      /\.wasm\.part-\d+$|\.payload\.part-\d+(?:\.(?:bin|json|js|png))?$/.test(
        fileName,
      ),
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
  const chunks = await writeChunks(wasmFile, wasmBytes);
  const wasmAssetId = wasmFile.slice(0, -'.wasm'.length);
  const prebuiltCompression = await preparePrebuiltCompression(
    wasmFile,
    wasmBytes,
  );
  const compressedWasmBytes = prebuiltCompression === null
    ? gzipSync(wasmBytes, { level: 9 })
    : null;
  const compression = prebuiltCompression ?? {
    format: 'gzip-rgb-png',
    byteLength: compressedWasmBytes.length,
    chunks: await writePngChunks(
      `${wasmAssetId}.payload`,
      compressedWasmBytes,
    ),
  };
  const manifest = {
    byteLength: wasmBytes.length,
    chunks,
    compression,
  };

  await writeFile(
    `${wasmPath}.chunks.json`,
    `${JSON.stringify(manifest)}\n`,
  );
  console.log(
    `Split ${wasmFile} into ${chunks.length} raw and ` +
      `${compression.chunks.length} ${compression.format} chunks.`,
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

async function writePngChunks(fileName, bytes) {
  const chunks = [];
  for (
    let offset = 0, index = 0;
    offset < bytes.length;
    offset += CHUNK_SIZE_BYTES, index += 1
  ) {
    const chunkBytes = bytes.subarray(
      offset,
      offset + CHUNK_SIZE_BYTES,
    );
    const chunkName =
      `${fileName}.part-${index.toString().padStart(3, '0')}.png`;
    await writeFile(
      resolve(outputDirectory, chunkName),
      encodeRgbPng(chunkBytes),
    );
    chunks.push({
      name: chunkName,
      byteLength: chunkBytes.length,
    });
  }
  return chunks;
}

async function preparePrebuiltCompression(wasmFile, wasmBytes) {
  const wasmAssetId = wasmFile.slice(0, -'.wasm'.length);
  const prefix = `${wasmAssetId}.payload.part-`;
  const chunkNames = (await readdir(prebuiltAssetDirectory))
    .filter((fileName) =>
      fileName.startsWith(prefix) && fileName.endsWith('.bin'),
    )
    .sort();
  if (chunkNames.length === 0) {
    return null;
  }

  const chunkBytes = await Promise.all(
    chunkNames.map((chunkName) =>
      readFile(resolve(prebuiltAssetDirectory, chunkName)),
    ),
  );
  const compressedBytes = Buffer.concat(chunkBytes);
  const decodedWasm = gunzipSync(compressedBytes);
  if (!decodedWasm.equals(wasmBytes)) {
    throw new Error(
      `Prebuilt payload for ${wasmFile} does not match the Web build`,
    );
  }
  await Promise.all(
    chunkNames.map((chunkName) =>
      copyFile(
        resolve(prebuiltAssetDirectory, chunkName),
        resolve(outputDirectory, chunkName),
      ),
    ),
  );
  return {
    format: 'gzip-cdn-chunks',
    byteLength: compressedBytes.length,
    chunks: chunkNames.map((chunkName, index) => ({
      name: chunkName,
      byteLength: chunkBytes[index].length,
      cdnUrl: `${cdnBaseUrl}/${chunkName}`,
    })),
  };
}

function encodeRgbPng(bytes) {
  const pixelCount = Math.ceil(bytes.length / 3);
  const height = Math.ceil(pixelCount / PNG_WIDTH_PIXELS);
  const rowByteLength = PNG_WIDTH_PIXELS * 3;
  const scanlines = Buffer.alloc((rowByteLength + 1) * height);
  for (let row = 0; row < height; row += 1) {
    const sourceOffset = row * rowByteLength;
    const sourceEnd = Math.min(sourceOffset + rowByteLength, bytes.length);
    bytes.copy(
      scanlines,
      row * (rowByteLength + 1) + 1,
      sourceOffset,
      sourceEnd,
    );
  }
  const header = Buffer.alloc(13);
  header.writeUInt32BE(PNG_WIDTH_PIXELS, 0);
  header.writeUInt32BE(height, 4);
  header[8] = 8;
  header[9] = 2;
  return Buffer.concat([
    PNG_SIGNATURE,
    createPngChunk('IHDR', header),
    createPngChunk(
      'IDAT',
      deflateSync(scanlines, { level: 9 }),
    ),
    createPngChunk('IEND', Buffer.alloc(0)),
  ]);
}

function createPngChunk(type, data) {
  const typeBytes = Buffer.from(type, 'ascii');
  const chunk = Buffer.alloc(data.length + 12);
  chunk.writeUInt32BE(data.length, 0);
  typeBytes.copy(chunk, 4);
  data.copy(chunk, 8);
  chunk.writeUInt32BE(
    crc32(Buffer.concat([typeBytes, data])),
    data.length + 8,
  );
  return chunk;
}

function crc32(bytes) {
  let crc = 0xffffffff;
  for (const byte of bytes) {
    crc = CRC_TABLE[(crc ^ byte) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function createCrcTable() {
  return Array.from({ length: 256 }, (_, index) => {
    let value = index;
    for (let bit = 0; bit < 8; bit += 1) {
      value = (value & 1) === 1
        ? 0xedb88320 ^ (value >>> 1)
        : value >>> 1;
    }
    return value >>> 0;
  });
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
