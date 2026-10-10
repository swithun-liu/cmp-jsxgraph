/*
 * Official JSXGraph 1.13.3 JXG.Validator fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/options-validator.mjs
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
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const values = [
            ["null", null],
            ["false", false],
            ["true", true],
            ["negativeInteger", -1],
            ["zero", 0],
            ["positiveInteger", 1],
            ["fraction", 1.25],
            ["nearInteger", 3.0000001],
            ["nan", Number.NaN],
            ["positiveInfinity", Number.POSITIVE_INFINITY],
            ["negativeInfinity", Number.NEGATIVE_INFINITY],
            ["empty", ""],
            ["integerString", "1"],
            ["decimalString", "1.25"],
            ["paddedString", " 1 "],
            ["leadingZeroString", "01"],
            ["exponentString", "1e2"],
            ["nanString", "NaN"],
            ["infinityString", "Infinity"],
            ["pixel", "12px"],
            ["negativePixel", "-1px"],
            ["html", "html"],
            ["internal", "internal"],
            ["svg", "svg"],
            ["triangleUp", "triangleUp"],
            ["pointFaceAlias", "[]"],
            ["unknownText", "unknown"],
            ["array", [1, 2]],
            ["object", {value: 1}]
        ];
        const validators = Object.fromEntries(
            Object.entries(JXG.Validator).map(([name, validator]) => [
                name,
                validator === false
                    ? {kind: "unvalidated"}
                    : {
                        kind: "function",
                        results: Object.fromEntries(
                            values.map(([label, value]) => [
                                label,
                                Boolean(validator(value))
                            ])
                        )
                    }
            ])
        );
        return {
            version: JXG.version,
            keys: Object.keys(JXG.Validator),
            validators
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
