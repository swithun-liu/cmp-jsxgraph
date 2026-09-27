/*
 * Official JSXGraph 1.13.3 adaptive Plot v3 behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/plot-v3.mjs
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
    const evidence = await page.evaluate((selectedCase) => {
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
        const disposeBoard = (board) => {
            const container = board.containerObj;
            JXG.JSXGraph.freeBoard(board);
            container.remove();
        };
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
            if (value === undefined) {
                return "undefined";
            }
            return value;
        };
        const pointSnapshot = (point) => ({
            t: classify(point._t),
            x: classify(point.usrCoords[1]),
            y: classify(point.usrCoords[2]),
            screenX: classify(point.scrCoords[1]),
            screenY: classify(point.scrCoords[2])
        });
        const snapshot = (curve) => {
            const points = curve.points
                .slice(0, curve.numberPoints)
                .map(pointSnapshot);
            const nanIndices = [];
            for (let index = 0; index < points.length; index += 1) {
                if (points[index].x === "NaN" || points[index].y === "NaN") {
                    nanIndices.push(index);
                }
            }
            return {
                numberPoints: curve.numberPoints,
                visibleArea: JXG.Math.Plot._visibleArea.map(classify),
                first: points[0],
                last: points[points.length - 1],
                nanIndices,
                points
            };
        };
        const attributes = {
            doAdvancedPlot: true,
            plotVersion: 3,
            recursionDepthHigh: 17,
            RDPsmoothing: false,
            withLabel: false,
            name: ""
        };
        const enabled = (name) =>
            selectedCase === "all" || selectedCase === name;
        const capture = (evidence, name, creator) => {
            if (!enabled(name)) {
                return;
            }
            const board = createBoard();
            evidence[name] = snapshot(creator(board));
            disposeBoard(board);
        };
        const originalRandom = Math.random;
        Math.random = () => 0.375;

        try {
            const evidence = {version: JXG.version};
            capture(
                evidence,
                "smooth",
                (board) => board.create(
                    "functiongraph",
                    [(x) => x * x, -2, 2],
                    attributes
                )
            );
            capture(
                evidence,
                "domain",
                (board) => board.create(
                    "functiongraph",
                    [(x) => x, -20, 20],
                    attributes
                )
            );
            capture(
                evidence,
                "cusp",
                (board) => board.create(
                    "functiongraph",
                    [(x) => Math.abs(x), -2, 2],
                    attributes
                )
            );
            capture(
                evidence,
                "jump",
                (board) => board.create(
                    "functiongraph",
                    [(x) => 1 / x, -2, 2],
                    attributes
                )
            );
            capture(
                evidence,
                "border",
                (board) => board.create(
                    "functiongraph",
                    [(x) => x < 0 ? Number.NaN : Math.sqrt(x), -2, 2],
                    attributes
                )
            );
            capture(
                evidence,
                "isolated",
                (board) => board.create(
                    "functiongraph",
                    [(x) => x === 0 ? Number.NaN : x, -2, 2],
                    attributes
                )
            );
            if (enabled("parametric")) {
                const board = createBoard();
                const callbackTrace = [];
                const curve = board.create(
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
                );
                evidence.parametric = snapshot(curve);
                evidence.callbackTrace = {
                    count: callbackTrace.length,
                    falseCount: callbackTrace.filter(
                        (entry) => entry[2] === false
                    ).length,
                    trueCount: callbackTrace.filter(
                        (entry) => entry[2] === true
                    ).length,
                    prefix: callbackTrace.slice(0, 12).map(
                        ([axis, t, suspended]) => [
                            axis,
                            classify(t),
                            classify(suspended)
                        ]
                    )
                };
                disposeBoard(board);
            }
            capture(
                evidence,
                "ellipse",
                (board) => board.create(
                    "ellipse",
                    [[-2, 0], [2, 0], 6],
                    attributes
                )
            );
            capture(
                evidence,
                "hyperbola",
                (board) => board.create(
                    "hyperbola",
                    [[-2, 0], [2, 0], 2],
                    attributes
                )
            );
            capture(
                evidence,
                "parabola",
                (board) => board.create(
                    "parabola",
                    [[1, 0], [[-1, -2], [-1, 2]]],
                    attributes
                )
            );
            capture(
                evidence,
                "spline",
                (board) => board.create(
                    "spline",
                    [[-3, -1, 1, 3], [0, 2, -1, 1]],
                    attributes
                )
            );
            capture(
                evidence,
                "cardinalspline",
                (board) => board.create(
                    "cardinalspline",
                    [
                        [[-3, 0], [-1, 2], [1, -1], [3, 1]],
                        0.5,
                        "uniform"
                    ],
                    {
                        ...attributes,
                        createPoints: false
                    }
                )
            );
            if (enabled("derivative")) {
                const board = createBoard();
                const source = board.create(
                    "functiongraph",
                    [(x) => x * x * x, -2, 2],
                    {
                        ...attributes,
                        doAdvancedPlot: false,
                        numberPointsHigh: 32
                    }
                );
                evidence.derivative = snapshot(
                    board.create(
                        "derivative",
                        [source],
                        attributes
                    )
                );
                disposeBoard(board);
            }
            return evidence;
        } finally {
            Math.random = originalRandom;
        }
    }, process.env.PLOT_CASE ?? "all");

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
