/*
 * Official JSXGraph 1.13.3 TraceEnPoche parser fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/reader-tracenpoche-parser.mjs
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
        const parse = (source) => {
            try {
                const tokens = reader.tokenize(
                    source,
                    "=<>!+-*&|/%^#",
                    "=<>&|"
                );
                return {
                    source,
                    output: reader.parse(tokens, "tep"),
                    error: null
                };
            } catch (error) {
                return {
                    source,
                    output: null,
                    error: error.message
                };
            }
        };
        return {
            version: JXG.version,
            cases: [
                parse(
                    "A=point(-1,0);" +
                        "B=point(1,1){rouge,sansnom};" +
                        "s=segment(A,B);"
                ),
                parse("d=A#B;"),
                parse("f=fonction(x^2+1);"),
                parse("v=[A<B,A,B];"),
                parse("var i=1;for i=1 to 3 do;A=point(i,i^2);end;")
            ]
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
