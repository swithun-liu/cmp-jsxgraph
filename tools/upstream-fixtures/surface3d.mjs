/*
 * Official JSXGraph 1.13.3 Surface3D fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/surface3d.mjs
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
        const wireframe = view.create(
            "functiongraph3d",
            [
                (x, y) => height * (x + y),
                [-2, 2],
                [-1, 1]
            ],
            {
                name: "",
                stepsU: 2,
                stepsV: 1,
                strokeColor: "#D55E00",
                strokeWidth: 2
            }
        );
        const rectangle = view.create(
            "parametricsurface3d",
            [
                (u, v) => u,
                (u, v) => v,
                (u, v) => u - v,
                [-1, 1],
                [-1, 1]
            ],
            {
                name: "",
                type: "shader",
                tiling: "rectangle",
                stepsU: 2,
                stepsV: 2,
                polyhedron: {
                    strokeWidth: 0.2,
                    fillOpacity: 0.8
                }
            }
        );
        board.update();
        const initial = snapshot(wireframe, rectangle);

        height = 2;
        board.update();
        const updated = snapshot(wireframe, rectangle);

        function snapshot(wire, filled) {
            return {
                wireframe: {
                    type: wire.type,
                    elType: wire.elType,
                    rangeU: wire.range_u,
                    rangeV: wire.range_v,
                    rows: wire.points.length,
                    columns: wire.points[0].length,
                    first: Array.from(wire.points[0][0]),
                    last: Array.from(
                        wire.points[wire.points.length - 1][
                            wire.points[0].length - 1
                        ]
                    ),
                    proxyX: Array.from(wire.element2D.dataX),
                    proxyY: Array.from(wire.element2D.dataY),
                    parentIds: wire.parents,
                    proxyParentIds: wire.element2D.parents
                },
                rectangle: {
                    type: filled.type,
                    elType: filled.elType,
                    proxyVisible: filled.element2D.visProp.visible,
                    vertexCount: Object.keys(
                        filled.polyhedron.def.vertices
                    ).length,
                    faceCount: filled.polyhedron.faces.length,
                    firstFace: filled.polyhedron.def.faces[0],
                    parentIds: filled.parents,
                    polyhedronParentIds: filled.polyhedron.parents
                }
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
