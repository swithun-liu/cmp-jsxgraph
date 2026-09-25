/*
 * Official JSXGraph 1.13.3 Polygon3D fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/polygon3d.mjs
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
            boundingbox: [-8, 8, 8, -8],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const view = board.create(
            "view3d",
            [
                [-5, -4],
                [8, 7],
                [[-5, 5], [-4, 6], [-3, 7]]
            ],
            {
                name: "",
                projection: "parallel",
                axesPosition: "none",
                xPlaneRear: {visible: false, type: "wireframe"},
                yPlaneRear: {visible: false, type: "wireframe"},
                zPlaneRear: {visible: false, type: "wireframe"},
                depthOrder: {enabled: true},
                az: {slider: {visible: false, start: 1}},
                el: {slider: {visible: false, start: 0.3}},
                bank: {slider: {visible: false, start: 0}}
            }
        );
        let dynamicX = 2;
        const direct = view.create(
            "polygon3d",
            [
                [-2, -1, 0],
                [() => dynamicX, -1, 0],
                [2, 3, 1],
                [-2, 3, 0]
            ],
            {
                id: "direct",
                name: "",
                fillColor: "#F0E442",
                fillOpacity: 0.35,
                vertices: {
                    name: "",
                    withLabel: false,
                    size: 5,
                    strokeColor: "none",
                    fillColor: "#FFFFFF",
                    fixed: true
                },
                borders: {
                    strokeColor: "#49545D",
                    strokeWidth: 3
                }
            }
        );
        const translation = view.create(
            "transform3d",
            [1, 2, 3],
            {type: "translate"}
        );
        board.update();
        view.updateZIndices();
        const initialDirect = snapshotPolygon(direct);
        dynamicX = 4;
        board.update();
        view.updateZIndices();
        const updatedDirect = snapshotPolygon(direct);

        let transformed = null;
        let transformedError = null;
        try {
            transformed = view.create(
                "polygon3d",
                [direct, translation],
                {
                    id: "transformed",
                    name: "",
                    fillColor: "#56B4E9",
                    vertices: {
                        name: "",
                        withLabel: false,
                        visible: false
                    }
                }
            );
        } catch (error) {
            transformedError = `${error.name}: ${error.message}`;
        }

        function snapshotPolygon(polygon) {
            return {
                type: polygon.type,
                elType: polygon.elType,
                vertexCount: polygon.vertices.length,
                vertices: polygon.vertices.map((point) =>
                    Array.from(point.coords)
                ),
                proxyVertexCount: polygon.element2D.vertices.length,
                proxyBorderCount: polygon.element2D.borders.length,
                fillColor: polygon.element2D.evalVisProp("fillcolor"),
                fillOpacity: polygon.element2D.evalVisProp("fillopacity"),
                borderColor:
                    polygon.element2D.borders[0]?.evalVisProp("strokecolor"),
                borderWidth:
                    polygon.element2D.borders[0]?.evalVisProp("strokewidth"),
                zIndex: polygon.zIndex
            };
        }

        const initial = {
            direct: initialDirect,
            transformed:
                transformed === null ? null : snapshotPolygon(transformed),
            transformedError
        };
        const updated = {
            direct: updatedDirect,
            transformed:
                transformed === null ? null : snapshotPolygon(transformed),
            transformedError
        };

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
