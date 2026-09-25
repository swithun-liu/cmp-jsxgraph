/*
 * Official JSXGraph 1.13.3 IntersectionCircle3D fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/intersectioncircle3d.mjs
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
        const plane = view.create(
            "plane3d",
            [
                [0, 0, 1],
                [1, 0, 0],
                [0, 1, 0],
                [-3, 3],
                [-3, 3]
            ],
            {name: "", visible: false, type: "wireframe"}
        );
        const planeSphere = view.create(
            "sphere3d",
            [[0, 0, 0], 2],
            {name: "", visible: false, numberPointsHigh: 4}
        );
        const planeCircle = view.create(
            "intersectioncircle3d",
            [plane, planeSphere],
            {name: "", numberPointsHigh: 9}
        );
        let rightX = 1;
        const left = view.create(
            "sphere3d",
            [[-1, 0, 0], 2],
            {name: "", visible: false, numberPointsHigh: 4}
        );
        const right = view.create(
            "sphere3d",
            [() => [rightX, 0, 0], 2],
            {name: "", visible: false, numberPointsHigh: 4}
        );
        const sphereCircle = view.create(
            "intersectioncircle3d",
            [left, right],
            {name: "", numberPointsHigh: 9}
        );
        board.update();
        const initial = {
            planeSphere: snapshot(planeCircle),
            sphereSphere: snapshot(sphereCircle)
        };

        rightX = 4;
        board.update();
        const disjoint = snapshot(sphereCircle);

        return {
            version: JXG.version,
            initial,
            disjoint
        };

        function snapshot(element) {
            return {
                type: element.type,
                elType: element.elType,
                radius: element.Radius(),
                center: Array.from(element.center.coords),
                centerVisible: element.center.visProp.visible,
                normal: Array.from(element.normal),
                numberPoints: element.curve.numberPoints,
                proxyVisible: element.curve.visProp.visible,
                parentIds: element.parents,
                firstParentHasChild:
                    element.parents[0] in board.objects &&
                    element.id in
                        board.objects[element.parents[0]].childElements,
                secondParentHasChild:
                    element.parents[1] in board.objects &&
                    element.id in
                        board.objects[element.parents[1]].childElements
            };
        }
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
