/*
 * Official JSXGraph 1.13.3 UTF8 and Base64 utility fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/encoding-base64.mjs
 */
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const require = createRequire(
    resolve(repositoryRoot, "tools/visual-parity/package.json")
);
const puppeteer = require("puppeteer");
const executablePath =
    process.env.CHROME_BIN ??
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

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
    const evidence = await page.evaluate(() => {
        const charCodes = (value) =>
            Array.from(value, (character) => character.charCodeAt(0));
        const capture = (operation) => {
            try {
                return {value: operation()};
            } catch (cause) {
                return {
                    error: {
                        name: cause?.name ?? null,
                        message: cause?.message ?? null
                    }
                };
            }
        };
        const unicode = "Grüße € 😀";
        const encodedUnicode = JXG.Util.UTF8.encode(unicode);

        return {
            version: JXG.version,
            utf8: {
                ascii: charCodes(JXG.Util.UTF8.encode("ABC")),
                crlf: charCodes(JXG.Util.UTF8.encode("a\r\nb")),
                unicode: charCodes(encodedUnicode),
                roundTrip: JXG.Util.UTF8.decode(encodedUnicode),
                invalidDecode: JXG.Util.UTF8.decode(
                    String.fromCharCode(0xe2, 0x28, 0xa1, 0x41)
                ),
                loneSurrogate: capture(() =>
                    JXG.Util.UTF8.encode("\ud800")
                ),
                asciiCharCodes: [
                    JXG.Util.UTF8.asciiCharCodeAt("€‚ƒ„…†‡ˆ‰Š‹ŒŽ", 0),
                    JXG.Util.UTF8.asciiCharCodeAt("€‚ƒ„…†‡ˆ‰Š‹ŒŽ", 1),
                    JXG.Util.UTF8.asciiCharCodeAt("€‚ƒ„…†‡ˆ‰Š‹ŒŽ", 12),
                    JXG.Util.UTF8.asciiCharCodeAt("A", 0),
                    JXG.Util.UTF8.asciiCharCodeAt("Ā", 0)
                ]
            },
            base64: {
                ascii: JXG.Util.Base64.encode("Man"),
                paddingOne: JXG.Util.Base64.encode("Ma"),
                paddingTwo: JXG.Util.Base64.encode("M"),
                unicode: JXG.Util.Base64.encode(unicode),
                decodedBinary: charCodes(
                    JXG.Util.Base64.decode(
                        JXG.Util.Base64.encode(unicode)
                    )
                ),
                decodedUtf8: JXG.Util.Base64.decode(
                    JXG.Util.Base64.encode(unicode),
                    true
                ),
                noisy: JXG.Util.Base64.decode(" T!W\nF\tu "),
                array: JXG.Util.Base64.decodeAsArray("AAEC/f7/"),
                invalidLength: capture(() =>
                    JXG.Util.Base64.decode("abc")
                )
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
