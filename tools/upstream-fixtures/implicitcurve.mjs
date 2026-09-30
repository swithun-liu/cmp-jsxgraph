/*
 * Official JSXGraph 1.13.3 ImplicitPlot behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/implicitcurve.mjs
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
        const classify = (value) => {
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (value === Number.POSITIVE_INFINITY) {
                return "Infinity";
            }
            if (value === Number.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
            return value;
        };
        const classifyArray = (values) => values.map(classify);
        const config = {
            resolution_out: 5,
            resolution_in: 5,
            max_steps: 64,
            h_initial: 0.1,
            h_max: 0.5,
            unitX: 10,
            unitY: 10
        };
        const defaults = new JXG.Math.ImplicitPlot(
            [-2, 2, 2, -2],
            {},
            (x, y) => x * x + y * y - 1,
            (x) => 2 * x,
            (_, y) => 2 * y
        ).config;
        const snapshot = (plot) => {
            const result = plot.plot();
            return {
                lengthX: result[0].length,
                lengthY: result[1].length,
                separatorIndex: result[0].findIndex(Number.isNaN),
                dataX: classifyArray(result[0]),
                dataY: classifyArray(result[1]),
                componentCount: result[2],
                qdt: plot.qdt.analyzeTree()
            };
        };

        const circle = new JXG.Math.ImplicitPlot(
            [-2, 2, 2, -2],
            config,
            (x, y) => x * x + y * y - 1,
            (x) => 2 * x,
            (_, y) => 2 * y
        );
        const circleTrace = circle.traceComponent([1, 1, 0]);
        const circleEvidence = {
            plot: snapshot(circle),
            trace: {
                x: classifyArray(circleTrace[0]),
                y: classifyArray(circleTrace[1])
            },
            tangent: classifyArray(circle.tangent([1, 0])),
            tangentA: classifyArray(circle.tangent_A([2, 0])),
            updateA: classifyArray(
                circle.updateA([2, 0], [1, 0], [0.9, 0.1])
            )
        };

        const folium = new JXG.Math.ImplicitPlot(
            [-2, 2, 2, -2],
            config,
            (x, y) => x ** 3 - 2 * x * y + y ** 3,
            (x, y) => 3 * x * x - 2 * y,
            (x, y) => -2 * x + 3 * y * y
        );
        const bifurcation = new JXG.Math.ImplicitPlot(
            [-2, 2, 2, -2],
            config,
            (x, y) => x * x - y * y,
            (x) => 2 * x,
            (_, y) => -2 * y
        );

        return {
            version: JXG.version,
            defaults: {
                hMax: defaults.h_max
            },
            folium: snapshot(folium),
            circle: circleEvidence,
            bifurcation: {
                atOrigin: bifurcation.isBifurcation([0, 0], 0.05),
                awayFromOrigin:
                    bifurcation.isBifurcation([1, 1], 0.05)
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
