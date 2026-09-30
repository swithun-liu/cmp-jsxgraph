/*
 * Official JSXGraph 1.13.3 FunctionGraph RDP behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/functiongraph-rdp.mjs
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
        const snapshot = (curve) => {
            const points = curve.points.slice(0, curve.numberPoints);
            return {
                numberPoints: curve.numberPoints,
                firstParameter: points[0]?._t,
                lastParameter: points[points.length - 1]?._t,
                nanIndices: points.flatMap((point, index) =>
                    Number.isNaN(point.usrCoords[1]) ||
                    Number.isNaN(point.usrCoords[2])
                        ? [index]
                        : []
                )
            };
        };
        const capture = (creator) => {
            const board = createBoard();
            const result = snapshot(creator(board));
            const container = board.containerObj;
            JXG.JSXGraph.freeBoard(board);
            container.remove();
            return result;
        };
        const attributes = {
            doAdvancedPlot: true,
            plotVersion: 2,
            withLabel: false,
            name: ""
        };

        return {
            version: JXG.version,
            linearDefault: capture((board) => board.create(
                "functiongraph",
                [(x) => x, -2, 2],
                attributes
            )),
            quadraticDefault: capture((board) => board.create(
                "functiongraph",
                [(x) => x * x, -2, 2],
                attributes
            )),
            quadraticDisabled: capture((board) => board.create(
                "functiongraph",
                [(x) => x * x, -2, 2],
                {...attributes, RDPsmoothing: false}
            )),
            quadraticThresholdOne: capture((board) => board.create(
                "functiongraph",
                [(x) => x * x, -2, 2],
                {...attributes, RDPthreshold: 1}
            )),
            quadraticThresholdZero: capture((board) => board.create(
                "functiongraph",
                [(x) => x * x, -2, 2],
                {...attributes, RDPthreshold: 0}
            )),
            reciprocalDefault: capture((board) => board.create(
                "functiongraph",
                [(x) => 1 / x, -2, 2],
                attributes
            )),
            parametricDefault: capture((board) => board.create(
                "curve",
                [
                    (t) => Math.cos(t),
                    (t) => Math.sin(t),
                    0,
                    2 * Math.PI
                ],
                attributes
            )),
            parametricEnabled: capture((board) => board.create(
                "curve",
                [
                    (t) => Math.cos(t),
                    (t) => Math.sin(t),
                    0,
                    2 * Math.PI
                ],
                {...attributes, RDPsmoothing: true}
            ))
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
