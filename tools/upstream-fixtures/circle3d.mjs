/*
 * Official JSXGraph 1.13.3 Circle3D fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/circle3d.mjs
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
        let centerX = -1;
        let normalY = 0;
        let normalZ = 1;
        let radius = -2;
        const circle = view.create(
            "circle3d",
            [
                () => [centerX, 1, 0],
                () => [0, 0, normalY, normalZ],
                () => radius
            ],
            {
                name: "",
                numberPointsHigh: 9,
                strokeColor: "#0072B2",
                strokeWidth: 3
            }
        );
        board.update();
        const initial = snapshot(circle);

        centerX = 2;
        normalY = 1;
        normalZ = 0;
        radius = -3;
        board.update();
        const updated = snapshot(circle);

        radius = Number.NaN;
        board.update();
        const hidden = snapshot(circle);

        function snapshot(element) {
            return {
                type: element.type,
                elType: element.elType,
                radius: element.Radius(),
                center: Array.from(element.center.coords),
                normal: Array.from(element.normal),
                frame1: Array.from(element.frame1),
                frame2: Array.from(element.frame2),
                numberPoints: element.curve.numberPoints,
                points: element.curve.points.map((point) =>
                    Array.from(point)
                ),
                proxyX: Array.from(element.curve.element2D.dataX),
                proxyY: Array.from(element.curve.element2D.dataY),
                proxyVisible: element.curve.visProp.visible,
                parentIds: element.parents,
                curveParentIds: element.curve.parents
            };
        }

        return {
            version: JXG.version,
            initial,
            updated,
            hidden
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
