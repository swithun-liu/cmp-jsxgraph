import { readdir, readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { gzipSync } from 'node:zlib';

const CHUNK_SIZE_BYTES = 512 * 1024;
const outputDirectory = process.argv[2];

if (!outputDirectory) {
  throw new Error('Expected the Web distribution directory');
}

const wasmFiles = (await readdir(outputDirectory))
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
  const compressedChunks = await writeBase64Chunks(
    `${wasmAssetId}.payload`,
    compressedWasmBytes,
  );

  await writeFile(
    `${wasmPath}.chunks.json`,
    `${JSON.stringify({
      byteLength: wasmBytes.length,
      chunks,
      compression: {
        format: 'gzip-base64',
        byteLength: compressedWasmBytes.length,
        chunks: compressedChunks,
      },
    })}\n`,
  );
  console.log(
    `Split ${wasmFile} into ${chunks.length} raw and ` +
      `${compressedChunks.length} gzip-base64 chunks.`,
  );
}

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

async function writeBase64Chunks(fileName, bytes) {
  const chunks = [];
  for (
    let offset = 0, index = 0;
    offset < bytes.length;
    offset += CHUNK_SIZE_BYTES, index += 1
  ) {
    const chunkName =
      `${fileName}.part-${index.toString().padStart(3, '0')}.json`;
    const base64 = bytes
      .subarray(offset, offset + CHUNK_SIZE_BYTES)
      .toString('base64');
    await writeFile(
      resolve(outputDirectory, chunkName),
      `${JSON.stringify(base64)}\n`,
    );
    chunks.push(chunkName);
  }
  return chunks;
}
