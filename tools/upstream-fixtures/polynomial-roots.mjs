/*
 * Official JSXGraph 1.13.3 polynomial root behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/polynomial-roots.mjs
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
    process.env.CHROME_BIN ||
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
        const roots = (coefficients, ...options) =>
            JXG.Math.Numerics.polzeros(coefficients, ...options)
                .map((root) => [root.real, root.imaginary]);
        const captureRoots = (coefficients, ...options) => {
            try {
                return {
                    threw: false,
                    roots: roots(coefficients, ...options)
                };
            } catch (error) {
                return {
                    threw: true,
                    name: error?.name ?? null,
                    message: String(error?.message ?? error)
                };
            }
        };

        return {
            version: JXG.version,
            real: roots([-1, 0, 1]),
            zeroRoots: roots([0, 0, -1, 1]),
            explicitDegree: roots([-1, 0, 1, 5], 2),
            rootsAboveOne: roots([6, -5, 1]),
            complex: roots([
                new JXG.Complex(0, -1),
                new JXG.Complex(0, 0),
                new JXG.Complex(1, 0)
            ]),
            constant: captureRoots([2]),
            allZero: captureRoots([0, 0, 0])
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
