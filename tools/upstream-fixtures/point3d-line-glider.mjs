/*
 * Official JSXGraph 1.13.3 Point3D-on-Line3D glider fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/point3d-line-glider.mjs
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
        '<div id="board" style="width:640px;height:480px"></div>'
    );
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const clean = (value) => Object.is(value, -0) ? 0 : value;
        const vector = (values) => Array.from(values, clean);
        const snapshot = (point) => ({
            coordinates: vector(point.coords),
            position: vector(point.position),
            projected: vector(point.element2D.coords.usrCoords),
            parents: [...point.parents],
            ancestors: Object.keys(point.ancestors),
            draggable: point.element2D.draggable()
        });
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
                id: "view",
                name: "",
                projection: "parallel",
                axesPosition: "none",
                depthOrder: {enabled: false},
                az: {slider: {visible: false, start: 1}},
                el: {slider: {visible: false, start: 0.3}},
                bank: {slider: {visible: false, start: 0}}
            }
        );
        const anchor = view.create("point3d", [0, 0, 0], {
            id: "anchor",
            name: ""
        });
        const host = view.create(
            "line3d",
            [anchor, [2, 1, -1], [-1, 2]],
            {id: "host", name: ""}
        );
        const glider = view.create("point3d", [4, 5, 2, host], {
            id: "glider",
            name: ""
        });
        const clamped = view.create("point3d", [20, 20, -20, host], {
            id: "clamped",
            name: ""
        });
        const interactive = view.create("point3d", [0, 0, 0, host], {
            id: "interactive",
            name: ""
        });
        const center = view.create("point3d", [0, 0, 0], {
            id: "center",
            name: ""
        });
        const circle = view.create(
            "circle3d",
            [center, [0, 0, 1], 2],
            {id: "circle", name: ""}
        );
        const curve = view.create(
            "curve3d",
            [
                (u) => u,
                (u) => u * u,
                (u) => -u,
                [-2, 2]
            ],
            {id: "curve", name: ""}
        );
        const plane = view.create(
            "plane3d",
            [center, [1, 0, 0], [0, 1, 0], [-2, 2], [-2, 2]],
            {id: "plane", name: ""}
        );
        const sphere = view.create(
            "sphere3d",
            [center, 2],
            {id: "sphere", name: ""}
        );
        const surface = view.create(
            "parametricsurface3d",
            [
                (u, v) => u,
                (u, v) => v,
                (u, v) => u + v,
                [-2, 2],
                [-2, 2]
            ],
            {id: "surface", name: ""}
        );
        const hosted = {
            circle: view.create(
                "point3d",
                [1.4, 1.4, 0.7, circle],
                {id: "circleGlider", name: ""}
            ),
            curve: view.create(
                "point3d",
                [0.7, 0.8, -0.9, curve],
                {id: "curveGlider", name: ""}
            ),
            plane: view.create(
                "point3d",
                [1.5, -0.5, 2, plane],
                {id: "planeGlider", name: ""}
            ),
            sphere: view.create(
                "point3d",
                [1.4, 1.4, 0.2, sphere],
                {id: "sphereGlider", name: ""}
            ),
            surface: view.create(
                "point3d",
                [0.8, -0.4, 1.1, surface],
                {id: "surfaceGlider", name: ""}
            )
        };

        board.fullUpdate();
        const initial = {
            glider: snapshot(glider),
            clamped: snapshot(clamped),
            hostChildren: Object.keys(host.childElements),
            hosted: Object.fromEntries(
                Object.entries(hosted).map(([name, point]) => [
                    name,
                    snapshot(point)
                ])
            )
        };

        const hostedDrag = {};
        for (const [name, point] of Object.entries(hosted)) {
            try {
                point.element2D.setPositionDirectly(
                    JXG.COORDS_BY_USER,
                    [-0.25, 0.75]
                );
                point.update();
                hostedDrag[name] = {
                    result: snapshot(point),
                    error: null
                };
            } catch (error) {
                hostedDrag[name] = {
                    result: snapshot(point),
                    error: `${error.name}: ${error.message}`
                };
            }
        }

        interactive.element2D.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-0.25, 0.75]
        );
        interactive.update();
        const initialDragProjection = snapshot(interactive);
        interactive.update();
        const initialDragSettled = snapshot(interactive);

        anchor.setPosition([1, -2, 3]);
        board.fullUpdate();
        const hostMoved = {
            anchor: snapshot(anchor),
            glider: snapshot(glider),
            clamped: snapshot(clamped)
        };

        glider.element2D.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-0.25, 0.75]
        );
        glider.update();
        const dragProjection = snapshot(glider);

        glider.update();
        const settled = snapshot(glider);

        return {
            initial,
            hostedDrag,
            initialDragProjection,
            initialDragSettled,
            hostMoved,
            dragProjection,
            settled
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
