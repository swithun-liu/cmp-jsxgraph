/*
 * Official JSXGraph 1.13.3 legacy adaptive Plot v1 behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/plot-v1.mjs
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
    await page.setContent('<main id="fixtures"></main>');
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        let boardIndex = 0;
        const createBoard = () => {
            const id = `board-${boardIndex}`;
            boardIndex += 1;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = "500px";
            container.style.height = "500px";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: [-5, 5, 5, -5],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
        };
        const classify = (value) => {
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (value === undefined) {
                return "undefined";
            }
            return value;
        };
        const snapshot = (curve) => {
            const points = curve.points
                .slice(0, curve.numberPoints)
                .map((point) => ({
                    t: classify(point._t),
                    x: classify(point.usrCoords[1]),
                    y: classify(point.usrCoords[2])
                }));
            return {
                numberPoints: curve.numberPoints,
                first: points[0],
                second: points[1],
                last: points[points.length - 1],
                nanIndices: points.flatMap((point, index) =>
                    point.x === "NaN" || point.y === "NaN"
                        ? [index]
                        : []
                )
            };
        };
        const capture = (creator, lowQuality = false) => {
            const board = createBoard();
            if (lowQuality) {
                board.updateQuality = board.BOARD_QUALITY_LOW;
            }
            const result = snapshot(creator(board));
            const container = board.containerObj;
            JXG.JSXGraph.freeBoard(board);
            container.remove();
            return result;
        };
        const attributes = {
            doAdvancedPlot: true,
            plotVersion: 1,
            RDPsmoothing: false,
            withLabel: false,
            name: ""
        };
        const callbackTrace = [];

        return {
            version: JXG.version,
            quadratic: capture((board) => board.create(
                "functiongraph",
                [(x) => x * x, -2, 2],
                attributes
            )),
            reciprocal: capture((board) => board.create(
                "functiongraph",
                [(x) => 1 / x, -2, 2],
                attributes
            )),
            lowQualityQuadratic: capture(
                (board) => board.create(
                    "functiongraph",
                    [(x) => x * x, -2, 2],
                    attributes
                ),
                true
            ),
            parametric: capture((board) => board.create(
                "curve",
                [
                    (t, suspendedUpdate) => {
                        callbackTrace.push(["x", t, suspendedUpdate]);
                        return t;
                    },
                    (t, suspendedUpdate) => {
                        callbackTrace.push(["y", t, suspendedUpdate]);
                        return t * t;
                    },
                    -2,
                    2
                ],
                attributes
            )),
            callbackTrace: {
                count: callbackTrace.length,
                prefix: callbackTrace.slice(0, 8).map(
                    ([axis, parameter, suspendedUpdate]) => [
                        axis,
                        classify(parameter),
                        suspendedUpdate
                    ]
                ),
                firstPairIsUnsuspended:
                    callbackTrace[0]?.[2] === false &&
                    callbackTrace[1]?.[2] === false,
                unsuspendedIndices: callbackTrace.flatMap(
                    (entry, index) => entry[2] === false ? [index] : []
                )
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
