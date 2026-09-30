/*
 * Official JSXGraph 1.13.3 Tracecurve behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/tracecurve.mjs
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
            const id = `board-${boardIndex++}`;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = "640px";
            container.style.height = "480px";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: [-8, 6, 8, -6],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
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
            return value;
        };
        const pointSnapshot = (point) => ({
            coordinates: point.coords.usrCoords.map(classify),
            position: classify(point.position)
        });
        const curveSnapshot = (curve) => ({
            elType: curve.elType,
            curveType: curve.evalVisProp("curvetype"),
            numberPointsOption: curve.evalVisProp("numberpoints"),
            numberPoints: curve.numberPoints,
            dataX: curve.dataX.map(classify),
            dataY: curve.dataY.map(classify),
            points: curve.points
                .slice(0, curve.numberPoints)
                .map((point) => [
                    classify(point.usrCoords[1]),
                    classify(point.usrCoords[2])
                ]),
            parents: [...curve.parents],
            childElements: Object.keys(curve.childElements)
        });
        const relations = (element) => ({
            parents: [...element.parents],
            childElements: Object.keys(element.childElements)
        });

        const circleBoard = createBoard();
        const center = circleBoard.create(
            "point",
            [0, 0],
            {id: "circle-center", name: ""}
        );
        const radius = circleBoard.create(
            "point",
            [2, 0],
            {id: "circle-radius", name: ""}
        );
        const circle = circleBoard.create(
            "circle",
            [center, radius],
            {id: "circle-host", name: ""}
        );
        const fixed = circleBoard.create(
            "point",
            [-3, 1],
            {id: "circle-fixed", name: ""}
        );
        const circleGlider = circleBoard.create(
            "glider",
            [2, 1, circle],
            {id: "circle-glider", name: ""}
        );
        const segment = circleBoard.create(
            "segment",
            [circleGlider, fixed],
            {id: "circle-segment", name: ""}
        );
        const midpoint = circleBoard.create(
            "midpoint",
            [segment],
            {id: "circle-midpoint", name: ""}
        );
        const circleGliderBefore = pointSnapshot(circleGlider);
        const midpointBefore = pointSnapshot(midpoint);
        const circleTrace = circleBoard.create(
            "tracecurve",
            [circleGlider, midpoint],
            {
                id: "circle-trace",
                name: "",
                numberPoints: 4
            }
        );
        const circleImmediatelyAfterCreate = {
            trace: curveSnapshot(circleTrace),
            glider: pointSnapshot(circleGlider),
            midpoint: pointSnapshot(midpoint)
        };
        circleBoard.update();
        const circleAfterUpdate = {
            trace: curveSnapshot(circleTrace),
            glider: pointSnapshot(circleGlider),
            midpoint: pointSnapshot(midpoint),
            relations: {
                trace: relations(circleTrace),
                glider: relations(circleGlider),
                midpoint: relations(midpoint)
            }
        };
        radius.setPositionDirectly(JXG.COORDS_BY_USER, [4, 0]);
        circleBoard.update();
        const circleAfterParentMove = {
            trace: curveSnapshot(circleTrace),
            glider: pointSnapshot(circleGlider),
            midpoint: pointSnapshot(midpoint)
        };

        const curveBoard = createBoard();
        const curveHost = curveBoard.create(
            "curve",
            [(t) => t, (t) => t * t - 1, -2, 2],
            {
                id: "curve-host",
                name: "",
                curveType: "parameter",
                doAdvancedPlot: false,
                numberPointsHigh: 5,
                RDPsmoothing: false
            }
        );
        const curveGlider = curveBoard.create(
            "glider",
            [0, -1, curveHost],
            {id: "curve-glider", name: ""}
        );
        const curveFixed = curveBoard.create(
            "point",
            [2, 3],
            {id: "curve-fixed", name: ""}
        );
        const curveMidpoint = curveBoard.create(
            "midpoint",
            [curveGlider, curveFixed],
            {id: "curve-midpoint", name: ""}
        );
        const curveTrace = curveBoard.create(
            "tracecurve",
            [curveGlider, curveMidpoint],
            {
                id: "curve-trace",
                name: "",
                numberPoints: 4
            }
        );
        curveBoard.update();

        const lineBoard = createBoard();
        const lineStart = lineBoard.create(
            "point",
            [-2, -1],
            {id: "line-start", name: ""}
        );
        const lineEnd = lineBoard.create(
            "point",
            [2, 3],
            {id: "line-end", name: ""}
        );
        const line = lineBoard.create(
            "segment",
            [lineStart, lineEnd],
            {id: "line-host", name: ""}
        );
        const lineGlider = lineBoard.create(
            "glider",
            [0, 1, line],
            {id: "line-glider", name: ""}
        );
        const lineTrace = lineBoard.create(
            "tracecurve",
            [lineGlider, lineGlider],
            {
                id: "line-trace",
                name: "",
                numberPoints: 4
            }
        );
        lineBoard.update();

        return {
            version: JXG.version,
            circle: {
                before: {
                    glider: circleGliderBefore,
                    midpoint: midpointBefore
                },
                immediatelyAfterCreate: circleImmediatelyAfterCreate,
                afterUpdate: circleAfterUpdate,
                afterParentMove: circleAfterParentMove
            },
            curve: {
                trace: curveSnapshot(curveTrace),
                glider: pointSnapshot(curveGlider),
                midpoint: pointSnapshot(curveMidpoint)
            },
            line: {
                trace: curveSnapshot(lineTrace),
                glider: pointSnapshot(lineGlider)
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
