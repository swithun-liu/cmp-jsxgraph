/*
 * Official JSXGraph 1.13.3 VectorField3D behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/vectorfield3d.mjs
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
            boundingbox: [-6, 5, 6, -5],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const view = board.create(
            "view3d",
            [
                [-5, -4],
                [10, 8],
                [[-5, 5], [-4, 4], [-3, 5]]
            ],
            {
                name: "",
                projection: "parallel",
                axesPosition: "none",
                xPlaneRear: {visible: false, type: "wireframe"},
                yPlaneRear: {visible: false, type: "wireframe"},
                zPlaneRear: {visible: false, type: "wireframe"},
                depthOrder: {enabled: false},
                az: {slider: {visible: false, start: 1}},
                el: {slider: {visible: false, start: 0.3}},
                bank: {slider: {visible: false, start: 0}}
            }
        );
        const state = {
            scale: 0.5,
            arrowEnabled: true
        };
        const field = view.create(
            "vectorfield3d",
            [
                [(x, y, z) => 1, (x, y, z) => 0, (x, y, z) => z],
                [0, 1, 1],
                [0, 0, 0],
                [0, 1, 1]
            ],
            {
                id: "field",
                name: "",
                withLabel: false,
                scale: () => state.scale,
                arrowHead: {
                    enabled: () => state.arrowEnabled
                }
            }
        );
        const zero = view.create(
            "vectorfield3d",
            [
                (x, y, z) => [0, 0, 0],
                [0, 1, 1],
                [0, 0, 0],
                [0, 1, 1]
            ],
            {
                id: "zero",
                name: "",
                withLabel: false
            }
        );
        board.update();
        const initial = snapshot(field);
        const zeroSnapshot = snapshot(zero);

        state.scale = 1;
        state.arrowEnabled = false;
        board.update();
        const updated = snapshot(field);

        let malformed;
        try {
            view.create(
                "vectorfield3d",
                [
                    [(x, y, z) => 1, (x, y, z) => 0, (x, y, z) => z],
                    [0, 1, 1],
                    [0, 0, 0],
                    [0, 1]
                ],
                {id: "malformed", name: ""}
            );
            board.update();
            malformed = {threw: false};
        } catch (error) {
            malformed = {
                threw: true,
                name: error?.name ?? null,
                message: String(error?.message ?? error)
            };
        }

        function classify(value) {
            return Number.isNaN(value) ? "NaN" : value;
        }

        function snapshot(curve) {
            return {
                id: curve.id,
                type: curve.type,
                elType: curve.elType,
                elementClass: curve.elementClass,
                numberPoints: curve.numberPoints,
                dataX: curve.dataX.map(classify),
                dataY: curve.dataY.map(classify),
                dataZ: curve.dataZ.map(classify),
                xData: curve.xData.map((value) =>
                    classify(typeof value === "function" ? value() : value)
                ),
                yData: curve.yData.map((value) =>
                    classify(typeof value === "function" ? value() : value)
                ),
                zData: curve.zData.map((value) =>
                    classify(typeof value === "function" ? value() : value)
                ),
                scale: classify(curve.evalVisProp("scale")),
                arrowHead: {
                    enabled: curve.evalVisProp("arrowhead.enabled"),
                    size: classify(curve.evalVisProp("arrowhead.size")),
                    angle: classify(curve.evalVisProp("arrowhead.angle"))
                },
                strokeWidth: curve.evalVisProp("strokewidth"),
                layer: curve.evalVisProp("layer")
            };
        }

        return {
            version: JXG.version,
            unitX: board.unitX,
            unitY: board.unitY,
            initial,
            updated,
            zero: zeroSnapshot,
            malformed
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
