/*
 * Official JSXGraph 1.13.3 Plane3D surface fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/plane3d-surfaces.mjs
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
            const id = `board-${boardIndex}`;
            boardIndex += 1;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = "640px";
            container.style.height = "480px";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: [-8, 8, 8, -8],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
        };
        const hiddenWireframePlanes = {
            xPlaneRear: {visible: false, type: "wireframe"},
            yPlaneRear: {visible: false, type: "wireframe"},
            zPlaneRear: {visible: false, type: "wireframe"}
        };
        const createView = (board, attributes = {}) =>
            board.create(
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
                    depthOrder: {enabled: true},
                    az: {slider: {visible: false, start: 1}},
                    el: {slider: {visible: false, start: 0.3}},
                    bank: {slider: {visible: false, start: 0}},
                    ...hiddenWireframePlanes,
                    ...attributes
                }
            );
        const snapshotSurface = (plane) => ({
            planeType: plane.evalVisProp("type"),
            tiling: plane.evalVisProp("tiling"),
            stepsU: plane.evalVisProp("stepsu"),
            stepsV: plane.evalVisProp("stepsv"),
            outlinePointCount: plane.element2D.numberPoints,
            vertexKeys: Object.keys(plane.polyhedron.def.vertices),
            coordinates: Object.fromEntries(
                Object.entries(plane.polyhedron.def.coords).map(
                    ([key, value]) => [key, Array.from(value)]
                )
            ),
            faces: plane.polyhedron.def.faces.map((face) => [...face]),
            faceStyles: plane.polyhedron.faces.map((face) => ({
                fillColor: face.element2D.evalVisProp("fillcolor"),
                fillOpacity: face.element2D.evalVisProp("fillopacity"),
                strokeColor: face.element2D.evalVisProp("strokecolor"),
                strokeWidth: face.element2D.evalVisProp("strokewidth"),
                shader: {
                    enabled: face.evalVisProp("shader.enabled"),
                    type: face.evalVisProp("shader.type"),
                    hue: face.evalVisProp("shader.hue"),
                    saturation: face.evalVisProp("shader.saturation"),
                    minLightness: face.evalVisProp("shader.minlightness"),
                    maxLightness: face.evalVisProp("shader.maxlightness")
                }
            }))
        });

        const board = createBoard();
        const view = createView(board);
        const colorArray = view.create(
            "plane3d",
            [
                [0, 0, -2],
                [1, 0, 0],
                [0, 1, 0],
                [-2, 2],
                [-1, 1]
            ],
            {
                id: "colors",
                name: "",
                type: "colorarray",
                stepsU: 2,
                stepsV: 1,
                polyhedron: {
                    strokeColor: "#123456",
                    strokeWidth: 0.25,
                    fillOpacity: 0.75,
                    fillColorArray: ["#ff0000", "#0000ff"]
                }
            }
        );
        const shader = view.create(
            "plane3d",
            [
                [0, 0, 0],
                [1, 0, 0],
                [0, 1, 0],
                [-2, 2],
                [-1, 1]
            ],
            {
                id: "shader",
                name: "",
                type: "shader",
                tiling: "triangle",
                stepsU: 2,
                stepsV: 2,
                polyhedron: {
                    fillColorArray: ["#ff0000"],
                    shader: {
                        hue: 120,
                        saturation: 100,
                        minLightness: 50,
                        maxLightness: 50
                    }
                }
            }
        );
        const colormap = view.create(
            "plane3d",
            [
                [0, 0, 0],
                [1, 0, 0],
                [0, 1, 0],
                [-1, 1],
                [-1, 1]
            ],
            {
                id: "colormap",
                name: "",
                type: "colormap",
                stepsU: 1,
                stepsV: 1,
                colormap: {
                    min: [-1, 240],
                    max: [1, 0],
                    s: 1,
                    v: 1
                }
            }
        );
        board.update();

        const axesBoard = createBoard();
        const axesView = axesBoard.create(
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
                depthOrder: {enabled: true},
                az: {slider: {visible: false, start: 1}},
                el: {slider: {visible: false, start: 0.3}},
                bank: {slider: {visible: false, start: 0}}
            }
        );
        axesBoard.update();
        const rear = axesView.defaultAxes.xPlaneRear;
        const rearFace = rear.polyhedron.faces[0];

        return {
            version: JXG.version,
            colorArray: snapshotSurface(colorArray),
            shader: snapshotSurface(shader),
            colormap: snapshotSurface(colormap),
            defaultRearPlane: {
                type: rear.evalVisProp("type"),
                tiling: rear.evalVisProp("tiling"),
                stepsU: rear.evalVisProp("stepsu"),
                stepsV: rear.evalVisProp("stepsv"),
                faceCount: rear.polyhedron.numberFaces,
                faceStyle: {
                    visible: rearFace.element2D.evalVisProp("visible"),
                    fillColor: rearFace.element2D.evalVisProp("fillcolor"),
                    fillOpacity: rearFace.element2D.evalVisProp("fillopacity"),
                    strokeColor: rearFace.element2D.evalVisProp("strokecolor"),
                    strokeOpacity:
                        rearFace.element2D.evalVisProp("strokeopacity"),
                    strokeWidth:
                        rearFace.element2D.evalVisProp("strokewidth"),
                    shaderType: rearFace.evalVisProp("shader.type"),
                    shaderMinLightness:
                        rearFace.evalVisProp("shader.minlightness"),
                    shaderMaxLightness:
                        rearFace.evalVisProp("shader.maxlightness")
                }
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
