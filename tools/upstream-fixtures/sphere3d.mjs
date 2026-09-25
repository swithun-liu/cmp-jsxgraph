/*
 * Official JSXGraph 1.13.3 Sphere3D fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/sphere3d.mjs
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
        const commonViewAttributes = {
            name: "",
            axesPosition: "none",
            xPlaneRear: {visible: false, type: "wireframe"},
            yPlaneRear: {visible: false, type: "wireframe"},
            zPlaneRear: {visible: false, type: "wireframe"},
            depthOrder: {enabled: false},
            az: {slider: {visible: false, start: 1}},
            el: {slider: {visible: false, start: 0.3}},
            bank: {slider: {visible: false, start: 0}}
        };
        const parallelView = board.create(
            "view3d",
            [
                [-5, -4],
                [10, 8],
                [[-5, 5], [-4, 4], [-3, 5]]
            ],
            {...commonViewAttributes, projection: "parallel"}
        );
        const centralView = board.create(
            "view3d",
            [
                [-5, -4],
                [10, 8],
                [[-5, 5], [-4, 4], [-3, 5]]
            ],
            {...commonViewAttributes, projection: "central"}
        );
        let radius = -2;
        const parallel = parallelView.create(
            "sphere3d",
            [[0, 0, 0], () => radius],
            {name: "", numberPointsHigh: 17}
        );
        const center = centralView.create(
            "point3d",
            [0, 0, 0],
            {name: "", visible: false}
        );
        const surfacePoint = centralView.create(
            "point3d",
            [1.5, 0, 0],
            {name: "", visible: false}
        );
        const central = centralView.create(
            "sphere3d",
            [center, surfacePoint],
            {name: "", numberPointsHigh: 17}
        );
        board.update();
        const initial = snapshot(parallel, central);

        radius = -3;
        board.update();
        const updated = snapshot(parallel, central);
        const initialParallelProxyId = parallel.element2D.id;
        const projectionSwitch = {
            initial: projectionSnapshot(parallelView, parallel)
        };

        parallelView.setAttribute({projection: "central"});
        board.update();
        const centralProxyId = parallel.element2D.id;
        const centralAuxiliaryIds = parallel.aux2D.map((point) => point.id);
        projectionSwitch.central = {
            ...projectionSnapshot(parallelView, parallel),
            oldProxyRemoved:
                board.objects[initialParallelProxyId] === undefined
        };

        parallelView.setAttribute({projection: "parallel"});
        board.update();
        projectionSwitch.restored = {
            ...projectionSnapshot(parallelView, parallel),
            centralProxyRemoved:
                board.objects[centralProxyId] === undefined,
            centralAuxiliariesRemoved: centralAuxiliaryIds.every(
                (id) => board.objects[id] === undefined
            )
        };
        const parameters = [Number.NaN, Number.NaN];

        return {
            version: JXG.version,
            initial,
            updated,
            projectionSwitch,
            projection: Array.from(
                parallel.projectCoords([4, 0, 0], parameters)
            ),
            parameters
        };

        function snapshot(parallelSphere, centralSphere) {
            return {
                parallel: {
                    type: parallelSphere.type,
                    elType: parallelSphere.elType,
                    method: parallelSphere.method,
                    radius: parallelSphere.Radius(),
                    proxyType: parallelSphere.element2D.elType,
                    proxyRadius: parallelSphere.element2D.Radius(),
                    parentIds: parallelSphere.parents,
                    proxyParentIds: parallelSphere.element2D.parents
                },
                central: {
                    type: centralSphere.type,
                    elType: centralSphere.elType,
                    method: centralSphere.method,
                    radius: centralSphere.Radius(),
                    proxyType: centralSphere.element2D.elType,
                    proxyPointCount:
                        centralSphere.element2D.numberPoints,
                    auxiliaryPointCount: centralSphere.aux2D.length,
                    parentIds: centralSphere.parents,
                    proxyParentIds: centralSphere.element2D.parents
                }
            };
        }

        function projectionSnapshot(view, sphere) {
            return {
                viewProjection: view.projectionType,
                sphereProjection: sphere.projectionType,
                proxyId: sphere.element2D.id,
                proxyType: sphere.element2D.elType,
                auxiliaryIds: sphere.aux2D.map((point) => point.id),
                auxiliaryPointCount: sphere.aux2D.length,
                inheritedIds: sphere.inherits.map((element) => element.id),
                directChildIds: Object.keys(sphere.childElements),
                boardObjectCount: Object.keys(board.objects).length
            };
        }
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
