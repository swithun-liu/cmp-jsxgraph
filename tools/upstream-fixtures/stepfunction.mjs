/*
 * Official JSXGraph 1.13.3 Stepfunction behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/stepfunction.mjs
 */
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const visualParityPackage = resolve(
    repositoryRoot,
    "tools/visual-parity/package.json"
);
const require = createRequire(visualParityPackage);
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
    await page.setContent(
        '<div id="board" style="width: 640px; height: 480px"></div>'
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
            boundingbox: [-2, 6, 8, -2],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const classify = (value) => {
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (value === undefined) {
                return "undefined";
            }
            return value;
        };
        const snapshot = (curve) => ({
            id: curve.id,
            elType: curve.elType,
            curveType: curve.evalVisProp("curvetype"),
            numberPoints: curve.numberPoints,
            parents: [...curve.parents],
            childElements: Object.keys(curve.childElements),
            xterm: Array.isArray(curve.xterm)
                ? curve.xterm.map(classify)
                : typeof curve.xterm,
            yterm: Array.isArray(curve.yterm)
                ? curve.yterm.map(classify)
                : typeof curve.yterm,
            dataX: curve.dataX.map(classify),
            dataY: curve.dataY.map(classify),
            points: curve.points
                .slice(0, curve.numberPoints)
                .map((point) => [
                    classify(point.usrCoords[1]),
                    classify(point.usrCoords[2])
                ])
        });

        const regular = board.create(
            "stepfunction",
            [[0, 1, 3, 4], [2, -1, 3, 1]],
            {id: "regular", name: "", withLabel: false}
        );
        const mismatched = board.create(
            "stepfunction",
            [[0, 2, 5], [4]],
            {id: "mismatched", name: "", withLabel: false}
        );
        const empty = board.create(
            "stepfunction",
            [[], []],
            {id: "empty", name: "", withLabel: false}
        );
        const mutable = board.create(
            "stepfunction",
            [[-1, 2], [5, 7]],
            {id: "mutable", name: "", withLabel: false}
        );
        const initialMutable = snapshot(mutable);
        mutable.xterm.splice(0, mutable.xterm.length, -2, 0, 3);
        mutable.yterm.splice(0, mutable.yterm.length, 1, 4, -2);
        board.update();
        const updatedMutable = snapshot(mutable);

        const functionParent = board.create(
            "stepfunction",
            [
                function () {
                    return [0, 1, 2];
                },
                function () {
                    return [3, 4, 5];
                }
            ],
            {id: "function-parent", name: "", withLabel: false}
        );

        const invalid = [];
        for (const parents of [[], [[0, 1]], [[0], [1], [2]]]) {
            try {
                board.create("stepfunction", parents);
                invalid.push(null);
            } catch (error) {
                invalid.push(String(error));
            }
        }

        return {
            version: JXG.version,
            regular: snapshot(regular),
            mismatched: snapshot(mismatched),
            empty: snapshot(empty),
            initialMutable,
            updatedMutable,
            functionParent: snapshot(functionParent),
            invalid
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
