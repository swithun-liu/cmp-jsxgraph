/*
 * Official JSXGraph 1.13.3 ZIP/GZIP/ZLIB and compressor fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/zip-compressor.mjs
 */
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";
import {
    constants,
    deflateRawSync,
    deflateSync,
    gzipSync
} from "node:zlib";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const require = createRequire(
    resolve(repositoryRoot, "tools/visual-parity/package.json")
);
const puppeteer = require("puppeteer");
const executablePath =
    process.env.CHROME_BIN ??
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const littleEndian16 = (value) =>
    Buffer.from([value & 0xff, (value >>> 8) & 0xff]);
const littleEndian32 = (value) =>
    Buffer.from([
        value & 0xff,
        (value >>> 8) & 0xff,
        (value >>> 16) & 0xff,
        (value >>> 24) & 0xff
    ]);

const zipEntry = (
    name,
    content,
    options,
    useDataDescriptor = false
) => {
    const nameBytes = Buffer.from(name, "latin1");
    const contentBytes = Buffer.from(content, "latin1");
    const compressed = deflateRawSync(contentBytes, options);
    const flags = useDataDescriptor ? 8 : 0;
    const header = Buffer.concat([
        Buffer.from([0x50, 0x4b, 0x03, 0x04]),
        littleEndian16(20),
        littleEndian16(flags),
        littleEndian16(8),
        littleEndian16(0),
        littleEndian16(0),
        littleEndian32(0),
        littleEndian32(useDataDescriptor ? 0 : compressed.length),
        littleEndian32(useDataDescriptor ? 0 : contentBytes.length),
        littleEndian16(nameBytes.length),
        littleEndian16(0),
        nameBytes
    ]);
    const descriptor = useDataDescriptor
        ? Buffer.concat([
            Buffer.from([0x50, 0x4b, 0x07, 0x08]),
            littleEndian32(0),
            littleEndian32(compressed.length),
            littleEndian32(contentBytes.length)
        ])
        : Buffer.alloc(0);
    return {
        bytes: Buffer.concat([header, compressed, descriptor]),
        blockType: (compressed[0] >>> 1) & 0x03
    };
};

const zipArchive = (entries) =>
    Buffer.concat([
        ...entries.map((entry) => entry.bytes),
        Buffer.from([0x50, 0x4b, 0x05, 0x06])
    ]);

const storedContent = "stored-block:0123456789";
const fixedContent =
    "fixed-block:" + "ABRACADABRA-".repeat(32);
const dynamicContent = Array.from(
    {length: 7},
    (_, index) =>
        `n${index % 17}:` +
        "abcde".repeat((index % 5) + 1) +
        ":" +
        `${(index * 7919) % 104729};`
).join("|");
const storedEntry = zipEntry(
    "stored.txt",
    storedContent,
    {level: 0}
);
const fixedEntry = zipEntry(
    "nested/fixed.txt",
    fixedContent,
    {level: 9, strategy: constants.Z_FIXED}
);
const dynamicEntry = zipEntry(
    "dynamic.txt",
    dynamicContent,
    {level: 9}
);
const descriptorEntry = zipEntry(
    "descriptor.txt",
    fixedContent,
    {level: 9, strategy: constants.Z_FIXED},
    true
);
const gzipContent = "gzip:" + "XYZ-".repeat(48);
const gzipHeaderContent = "gzip-header:" + "Q-".repeat(32);
const gzipHeaderPayload = deflateRawSync(
    Buffer.from(gzipHeaderContent, "latin1"),
    {level: 9}
);
const gzipWithHeaders = Buffer.concat([
    Buffer.from([0x1f, 0x8b, 8, 26]),
    Buffer.alloc(4),
    Buffer.from([0, 3]),
    Buffer.from("source.gxt\0fixture-comment\0", "latin1"),
    Buffer.alloc(2),
    gzipHeaderPayload,
    Buffer.alloc(4),
    littleEndian32(gzipHeaderContent.length)
]);
const compressorSource =
    "JXG.create('point', [1, 2]); Grüße 😀";
const percentEncodedSource = encodeURIComponent(compressorSource);
const zlibPayload = deflateSync(
    Buffer.from(percentEncodedSource, "latin1"),
    {level: 9}
);

const fixtures = {
    storedZip: zipArchive([storedEntry]).toString("base64"),
    fixedZip: zipArchive([fixedEntry]).toString("base64"),
    dynamicZip: zipArchive([dynamicEntry]).toString("base64"),
    descriptorZip: zipArchive([descriptorEntry]).toString("base64"),
    multiZip: zipArchive([fixedEntry, dynamicEntry]).toString("base64"),
    gzip: gzipSync(Buffer.from(gzipContent, "latin1"), {
        level: 9,
        mtime: 0
    }).toString("base64"),
    gzipWithHeaders: gzipWithHeaders.toString("base64"),
    zlib: zlibPayload.toString("base64"),
    blockTypes: {
        stored: storedEntry.blockType,
        fixed: fixedEntry.blockType,
        dynamic: dynamicEntry.blockType
    },
    expectedLengths: {
        stored: storedContent.length,
        fixed: fixedContent.length,
        dynamic: dynamicContent.length,
        gzip: gzipContent.length,
        gzipWithHeaders: gzipHeaderContent.length
    }
};

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent("<main></main>");
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    await page.addScriptTag({
        content:
            "JXG.decompress = function (str) {" +
            "return decodeURIComponent(" +
            "new JXG.Util.Unzip(" +
            "JXG.Util.Base64.decodeAsArray(str)" +
            ").unzip()[0][0]);" +
            "};"
    });
    const evidence = await page.evaluate((input) => {
        const bytes = (base64) =>
            Array.from(atob(base64), (character) =>
                character.charCodeAt(0)
            );
        const hash = (value) => {
            let result = 2166136261;
            for (let index = 0; index < value.length; index += 1) {
                result ^= value.charCodeAt(index);
                result = Math.imul(result, 16777619);
            }
            return result >>> 0;
        };
        const unzip = (base64) =>
            new JXG.Util.Unzip(bytes(base64))
                .unzip()
                .map(([content, name]) => ({
                    name,
                    length: content.length,
                    hash: hash(content),
                    prefix: content.slice(0, 32),
                    suffix: content.slice(-32)
                }));

        return {
            version: JXG.version,
            blockTypes: input.blockTypes,
            expectedLengths: input.expectedLengths,
            storedZip: unzip(input.storedZip),
            fixedZip: unzip(input.fixedZip),
            dynamicZip: unzip(input.dynamicZip),
            descriptorZip: unzip(input.descriptorZip),
            multiZip: unzip(input.multiZip),
            gzip: unzip(input.gzip),
            gzipWithHeaders: unzip(input.gzipWithHeaders),
            zlib: unzip(input.zlib),
            missingFile:
                new JXG.Util.Unzip(bytes(input.multiZip))
                    .unzipFile("missing.txt"),
            unknownContainer:
                new JXG.Util.Unzip([1, 2, 3, 4]).unzip(),
            decompressed: JXG.decompress(input.zlib)
        };
    }, fixtures);
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
