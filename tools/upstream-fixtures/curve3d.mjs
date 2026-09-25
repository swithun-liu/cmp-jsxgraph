/*
 * Official JSXGraph 1.13.3 Curve3D fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/curve3d.mjs
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
        let height = 1;
        const component = view.create(
            "curve3d",
            [
                (u) => Math.cos(u),
                (u) => Math.sin(u),
                (u) => height * u / Math.PI,
                [0, 2 * Math.PI]
            ],
            {name: "", numberPointsHigh: 5}
        );
        const vector = view.create(
            "curve3d",
            [(u) => [u, u * u, -u], [-2, 2]],
            {name: "", numberPointsHigh: 5}
        );
        const discrete = view.create(
            "curve3d",
            [[[-1, 0, 1], [0, 2, 2], [2, 0, 3]]],
            {name: ""}
        );
        const translation = view.create(
            "transform3d",
            [3, -1, 2],
            {type: "translate"}
        );
        const transformed = view.create(
            "curve3d",
            [discrete, translation],
            {name: ""}
        );
        board.update();
        const initial = {
            component: snapshot(component),
            vector: snapshot(vector),
            discrete: snapshot(discrete),
            transformed: snapshot(transformed)
        };

        height = 2;
        board.update();
        const updated = {
            component: snapshot(component),
            vector: snapshot(vector),
            discrete: snapshot(discrete),
            transformed: snapshot(transformed)
        };

        function snapshot(curve) {
            return {
                type: curve.type,
                elType: curve.elType,
                numberPoints: curve.numberPoints,
                points: curve.points.map((point) =>
                    Array.from(point)
                ),
                proxyX: Array.from(curve.element2D.dataX),
                proxyY: Array.from(curve.element2D.dataY),
                range: curve.range,
                parentIds: curve.parents
            };
        }

        return {
            version: JXG.version,
            initial,
            updated
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
