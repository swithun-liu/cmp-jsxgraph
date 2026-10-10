/*
 * Official JSXGraph 1.13.3 Curve.generateTerm behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/curve-generate-term.mjs
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
    process.env.PUPPETEER_EXECUTABLE_PATH ??
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent(
        '<main><div id="board" style="width:640px;height:480px"></div></main>'
    );
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-5, 5, 5, -5],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const driver = board.create("point", [1, 0], {
            id: "driver",
            name: "",
            withLabel: false
        });
        const curve = board.create("curve", [[0, 1], [0, 1]], {
            id: "curve",
            name: "",
            withLabel: false,
            doAdvancedPlot: false,
            numberPointsHigh: 8
        });
        const classify = (value) =>
            value === undefined ? "undefined" : value;
        const pointCoordinates = () =>
            curve.points
                .slice(0, curve.numberPoints)
                .map((point) => Array.from(point.usrCoords));

        const result = curve.generateTerm(
            "t",
            (t) => t + driver.X(),
            (t) => t * t,
            -1,
            2
        );
        board.update();
        const continuous = {
            result: classify(result),
            curveType: curve.visProp.curvetype,
            minimum: curve.minX(),
            maximum: curve.maxX(),
            x0: curve.X(0),
            y2: curve.Y(2)
        };
        driver.setPositionDirectly(JXG.COORDS_BY_USER, [3, 0]);
        board.update();
        const dynamic = {
            x0: curve.X(0),
            firstPoint: Array.from(curve.points[0].usrCoords)
        };

        curve.generateTerm("phi", () => 2, [1, -1]);
        board.update();
        const polar = {
            curveType: curve.visProp.curvetype,
            minimum: curve.minX(),
            maximum: curve.maxX(),
            x0: curve.X(0),
            y0: curve.Y(0),
            xPi: curve.X(Math.PI),
            yPi: curve.Y(Math.PI)
        };

        curve.generateTerm("t", [[1, 2], [3, 4]]);
        board.update();
        const transposed = {
            curveType: curve.visProp.curvetype,
            dataX: Array.from(curve.dataX),
            dataY: Array.from(curve.dataY),
            points: pointCoordinates()
        };

        curve.generateTerm("x", [1, 2], (x) => x * 10);
        board.update();
        const mixed = {
            dataX: Array.from(curve.dataX),
            dataY: curve.dataY,
            points: pointCoordinates()
        };

        return {
            version: JXG.version,
            continuous,
            dynamic,
            polar,
            transposed,
            mixed
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
