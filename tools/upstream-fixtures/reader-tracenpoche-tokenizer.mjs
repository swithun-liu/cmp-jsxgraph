/*
 * Official JSXGraph 1.13.3 Tracenpoche tokenizer fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/reader-tracenpoche-tokenizer.mjs
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
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "third_party/jsxgraph-src/src/reader/tracenpoche.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const reader = new JXG.TracenpocheReader({}, "");
        const run = (source, prefix, suffix) => {
            const diagnostics = [];
            const originalLog = console.log;
            console.log = (...values) => {
                diagnostics.push(values.join(" "));
            };
            try {
                return {
                    tokens: reader.tokenize(source, prefix, suffix) ?? [],
                    diagnostics
                };
            } finally {
                console.log = originalLog;
            }
        };

        return {
            version: JXG.version,
            names: run("AB abC2' var X for Y"),
            numbers: run("12 3.5 6e-2 7e+ 8a .5"),
            strings: run("'a\\n\\u0042' \"x\\t\" 'unterminated"),
            commentsAndOperators: run(
                "A// ignored\n<= B && C := D",
                "=<>!+-*&|/%^#",
                "=<>&|"
            ),
            empty: run("")
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
