/*
 * Official JSXGraph 1.13.3 extrapolation fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/extrapolate.mjs
 */
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const require = createRequire(
    resolve(repositoryRoot, "tools/visual-parity/package.json")
);
const puppeteer = require("puppeteer");
const executablePath =
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent("<main>JSXGraph extrapolation fixture</main>");
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const extrapolate = JXG.Math.Extrapolate;
        const sequence = Array.from(
            {length: 6},
            (_, index) => 1 - 2 ** -(index + 1)
        );
        const runTransformation = (name, count) => {
            const state = [];
            return sequence
                .slice(0, count)
                .map((value, index) =>
                    extrapolate[name](value, index, state)
                );
        };

        const numeratorState = [];
        const denominatorState = [];
        let partialSum = 0;
        const levin = Array.from({length: 8}, (_, index) => {
            const sign = index % 2 === 0 ? 1 : -1;
            partialSum += sign / (index + 1);
            return extrapolate.levin(
                partialSum,
                index,
                sign,
                1,
                numeratorState,
                denominatorState
            );
        });

        const constantState = [];
        const degenerateAitken = [0, 1, 2].map((index) =>
            extrapolate.aitken(1, index, constantState)
        );
        const normalizeResult = (result) => ({
            value: Number.isNaN(result[0]) ? "NaN" : result[0],
            classification: result[1],
            reliability: result[2]
        });
        const linear = (x) => 1 + x;
        const x0 = 1e-7;
        const initialStep = 0.1;

        return {
            version: JXG.version,
            defaults: {
                upper: extrapolate.upper,
                infty: extrapolate.infty
            },
            transformations: {
                wynnEps: runTransformation("wynnEps", 4),
                aitken: runTransformation("aitken", 4),
                brezinski: runTransformation("brezinski", 6),
                levin,
                degenerateAitken
            },
            iteration: {
                wynnEps: normalizeResult(
                    extrapolate.iteration(0, 1, linear, "wynnEps", 0)
                ),
                aitken: normalizeResult(
                    extrapolate.iteration(0, 1, linear, "aitken", 1)
                ),
                brezinski: normalizeResult(
                    extrapolate.iteration(0, 1, linear, "brezinski", 0)
                ),
                nan: normalizeResult(
                    extrapolate.iteration(
                        0,
                        1,
                        () => Number.NaN,
                        "wynnEps",
                        0
                    )
                )
            },
            iterationLevin: {
                finite: normalizeResult(
                    extrapolate.iteration_levin(0, 1, linear, 0)
                ),
                infinite: normalizeResult(
                    extrapolate.iteration_levin(0, 1, (x) => 1 / x, 0)
                ),
                nan: normalizeResult(
                    extrapolate.iteration_levin(
                        0,
                        1,
                        () => Number.NaN,
                        0
                    )
                )
            },
            limit: {
                logarithm: normalizeResult(
                    extrapolate.limit(x0, initialStep, (x) => Math.log(x))
                ),
                tangent: normalizeResult(
                    extrapolate.limit(
                        x0,
                        initialStep,
                        (x) => Math.tan(x - Math.PI * 0.5)
                    )
                ),
                reciprocal: normalizeResult(
                    extrapolate.limit(x0, initialStep, (x) => 4 / x)
                )
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
