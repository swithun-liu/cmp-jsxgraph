/*
 * Official JSXGraph 1.13.3 IntersectionLine3D fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/intersectionline3d.mjs
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
        const horizontal = view.create(
            "plane3d",
            [[0, 0, 0], [1, 0, 0], [0, 1, 0]],
            {name: "", visible: false, type: "wireframe"}
        );
        let verticalX = 1;
        const vertical = view.create(
            "plane3d",
            [() => [verticalX, 0, 0], [0, 1, 0], [0, 0, 1]],
            {name: "", visible: false, type: "wireframe"}
        );
        const intersection = view.create(
            "intersectionline3d",
            [horizontal, vertical],
            {
                name: "",
                point1: {id: "helper1", name: "", visible: false},
                point2: {id: "helper2", name: "", visible: false}
            }
        );
        board.update();
        const initial = snapshot(intersection);

        verticalX = 2;
        board.update();
        const updated = snapshot(intersection);

        return {
            version: JXG.version,
            initial,
            updated
        };

        function snapshot(element) {
            return {
                type: element.type,
                elType: element.elType,
                point1: Array.from(element.point1.coords),
                point2: Array.from(element.point2.coords),
                point1Visible: element.point1.visProp.visible,
                point2Visible: element.point2.visProp.visible,
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
