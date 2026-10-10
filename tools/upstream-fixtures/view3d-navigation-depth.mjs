/*
 * Official JSXGraph 1.13.3 View3D navigation and depth-order fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/view3d-navigation-depth.mjs
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
    process.env.CHROME_BIN ??
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent(
        '<style>body{margin:0}</style><main id="fixtures"></main>'
    );
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        let boardIndex = 0;
        const clean = (value) => Object.is(value, -0) ? 0 : value;
        const vector = (values) => Array.from(values, clean);
        const matrix = (rows) => rows.map(vector);
        const angles = (view) => ({
            az: clean(view.angles.az),
            el: clean(view.angles.el),
            bank: clean(view.angles.bank)
        });
        const snapshot = (view) => ({
            angles: angles(view),
            cameraDistance: clean(view.getCameraDistance()),
            matrix3DRot: matrix(view.matrix3DRot)
        });
        const createBoard = (renderer = "svg") => {
            const id = `board-${boardIndex++}`;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = "500px";
            container.style.height = "400px";
            container.style.position = "absolute";
            container.style.left = "0";
            container.style.top = "0";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: [-5, 4, 5, -4],
                axis: false,
                renderer,
                showCopyright: false,
                showNavigation: false
            });
        };
        const createView = (board, attributes = {}) =>
            board.create(
                "view3d",
                [
                    [-4, -3],
                    [8, 6],
                    [[-5, 5], [-5, 5], [-5, 5]]
                ],
                {
                    name: "",
                    axesPosition: "none",
                    az: {slider: {visible: false, start: 1}},
                    el: {slider: {visible: false, start: 0.3}},
                    bank: {slider: {visible: false, start: 0}},
                    ...attributes
                }
            );

        const viewBoard = createBoard();
        const view = createView(viewBoard, {
            values: [
                [0.2, 0.3, 2],
                [1.2, 0.7, 3]
            ]
        });
        view.setCurrentView(-1);
        const stored = snapshot(view);
        view.nextView();
        const next = snapshot(view);
        view.previousView();
        const previous = snapshot(view);
        view.freeR();
        const freed = snapshot(view);

        viewBoard._change3DView = true;
        view._azEventHandler({
            key: "ArrowRight",
            screenX: 0,
            screenY: 0
        });
        const keyAzimuth = snapshot(view);
        view._elEventHandler({
            key: "ArrowUp",
            screenX: 0,
            screenY: 0
        });
        const keyElevation = snapshot(view);
        view._bankEventHandler({
            key: ">",
            deltaY: 0,
            preventDefault() {}
        });
        const keyBank = snapshot(view);
        viewBoard._change3DView = false;

        view.pointerDownHandler({
            button: 0,
            screenX: 100,
            screenY: 100,
            shiftKey: false,
            ctrlKey: false
        });
        view._az_elEventHandler({
            button: 0,
            key: null,
            screenX: 150,
            screenY: 120
        });
        view.pointerUpHandler({});
        const pointerAngles = snapshot(view);

        viewBoard._change3DView = true;
        view._bankEventHandler({
            key: null,
            deltaY: 25,
            preventDefault() {}
        });
        viewBoard._change3DView = false;
        const wheelBank = snapshot(view);

        const trackballBoard = createBoard();
        const trackball = createView(trackballBoard, {
            trackball: {enabled: true}
        });
        trackball.pointerDownHandler({
            button: 0,
            screenX: 100,
            screenY: 100,
            shiftKey: false,
            ctrlKey: false
        });
        trackball._trackballHandler({
            button: 0,
            screenX: 130,
            screenY: 115,
            clientX: 130,
            clientY: 115
        });
        const trackballState = {...trackball._trackball};
        trackball.pointerUpHandler({});
        const trackballMoved = snapshot(trackball);

        const depthBoard = createBoard("canvas");
        const depthView = createView(depthBoard, {
            az: {slider: {visible: false, start: Math.PI / 2}},
            el: {slider: {visible: false, start: 0}},
            depthOrder: {enabled: true, layers: [12, 13]}
        });
        const far = depthView.create("point3d", [-2, 0, 0], {
            id: "far",
            name: "",
            layer: 12
        });
        const near = depthView.create("point3d", [2, 0, 0], {
            id: "near",
            name: "",
            layer: 12
        });
        const middle = depthView.create("point3d", [0, 0, 0], {
            id: "middle",
            name: "",
            layer: 12
        });
        const upperLayer = depthView.create("point3d", [-4, 0, 0], {
            id: "upperLayer",
            name: "",
            layer: 13
        });
        const ordinary = depthBoard.create("point", [0, 0], {
            id: "ordinary",
            name: "",
            layer: 12
        });
        const renderOrder = [];
        for (const element of [
            far.element2D,
            near.element2D,
            middle.element2D,
            upperLayer.element2D,
            ordinary
        ]) {
            const original = element.updateRenderer.bind(element);
            element.updateRenderer = function () {
                renderOrder.push(
                    this.evalVisProp("element3d")?.id ?? this.id
                );
                return original();
            };
        }
        depthBoard.fullUpdate();
        depthView.updateDepthOrdering();
        const targetIds = new Set([
            "far",
            "near",
            "middle",
            "upperLayer"
        ]);
        const depth = {
            zIndices: {
                far: clean(far.zIndex),
                near: clean(near.zIndex),
                middle: clean(middle.zIndex),
                upperLayer: clean(upperLayer.zIndex)
            },
            layer12: depthView.depthOrdered[12]
                .map((element) => element.id)
                .filter((id) => targetIds.has(id)),
            layer13: depthView.depthOrdered[13]
                .map((element) => element.id)
                .filter((id) => targetIds.has(id)),
            canvasRenderOrder: renderOrder
        };

        return {
            stored,
            next,
            previous,
            freed,
            keyAzimuth,
            keyElevation,
            keyBank,
            pointerAngles,
            wheelBank,
            trackballState,
            trackballMoved,
            depth
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
