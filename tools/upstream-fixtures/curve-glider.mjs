/*
 * Official JSXGraph 1.13.3 Curve-backed Glider behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   CHROME_BIN=/path/to/chrome node tools/upstream-fixtures/curve-glider.mjs
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
            const id = `board-${boardIndex++}`;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = "640px";
            container.style.height = "480px";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: [-8, 6, 8, -6],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
        };
        const pointSnapshot = (point) => ({
            id: point.id,
            coordinates: point.coords.usrCoords.slice(),
            position: point.position,
            slideObject: point.slideObject?.id ?? null,
            slideObjects: point.slideObjects.map((element) => element.id),
            parents: [...point.parents],
            ancestors: Object.keys(point.ancestors)
        });

        const functionBoard = createBoard();
        const driver = functionBoard.create("point", [0, 1], {
            id: "driver",
            name: ""
        });
        const functionGraph = functionBoard.create(
            "functiongraph",
            [(x) => driver.Y() + 0.25 * x * x, -4, 4],
            {id: "functionGraph", name: ""}
        );
        const functionGlider = functionBoard.create(
            "glider",
            [1.5, 4, functionGraph],
            {id: "functionGlider", name: ""}
        );
        const functionInitial = pointSnapshot(functionGlider);
        functionGlider.setPosition(
            JXG.COORDS_BY_USER,
            [3, 0]
        );
        functionBoard.update(functionGlider);
        const functionDragged = pointSnapshot(functionGlider);
        driver.setPosition(JXG.COORDS_BY_USER, [0, 2]);
        functionBoard.update();
        const functionParentMoved = pointSnapshot(functionGlider);
        functionBoard.update();
        const functionParentSettled = pointSnapshot(functionGlider);
        functionGlider.setPosition(
            JXG.COORDS_BY_USER,
            [3, 0]
        );
        functionBoard.update(functionGlider);
        const functionDraggedAfterParent =
            pointSnapshot(functionGlider);

        const parametricBoard = createBoard();
        const parametric = parametricBoard.create(
            "curve",
            [(t) => 2 * Math.cos(t), (t) => Math.sin(t), 0, 2 * Math.PI],
            {id: "parametric", name: "", curveType: "parameter"}
        );
        const parametricGlider = parametricBoard.create(
            "glider",
            [3, 0.4, parametric],
            {id: "parametricGlider", name: ""}
        );
        const parametricInitial = pointSnapshot(parametricGlider);
        parametricGlider.setPosition(
            JXG.COORDS_BY_USER,
            [-3, 0.2]
        );
        parametricBoard.update(parametricGlider);
        const parametricDragged = pointSnapshot(parametricGlider);
        parametricGlider.setGliderPosition(Math.PI * 0.5);
        const parametricPositioned = pointSnapshot(parametricGlider);

        const plotBoard = createBoard();
        const plot = plotBoard.create(
            "curve",
            [[-4, -1, 2, 4], [-2, 2, -1, 2]],
            {id: "plot", name: ""}
        );
        const plotGlider = plotBoard.create(
            "glider",
            [0, 0, plot],
            {id: "plotGlider", name: ""}
        );
        const plotInitial = pointSnapshot(plotGlider);
        plotGlider.setPosition(JXG.COORDS_BY_USER, [3.5, 0]);
        plotBoard.update(plotGlider);
        const plotDragged = pointSnapshot(plotGlider);
        plotGlider.setGliderPosition(1.5);
        const plotPositioned = pointSnapshot(plotGlider);
        plotBoard.removeObject(plot);

        const hyperbolaBoard = createBoard();
        const hyperbolaFocus1 = hyperbolaBoard.create(
            "point",
            [-3, 0],
            {id: "hyperbolaFocus1", name: ""}
        );
        const hyperbolaFocus2 = hyperbolaBoard.create(
            "point",
            [3, 0],
            {id: "hyperbolaFocus2", name: ""}
        );
        const hyperbolaPoint = hyperbolaBoard.create(
            "point",
            [5, 2],
            {id: "hyperbolaPoint", name: ""}
        );
        const hyperbola = hyperbolaBoard.create(
            "hyperbola",
            [hyperbolaFocus1, hyperbolaFocus2, hyperbolaPoint],
            {id: "hyperbola", name: ""}
        );
        const hyperbolaGlider = hyperbolaBoard.create(
            "glider",
            [4, 3, hyperbola],
            {id: "hyperbolaGlider", name: ""}
        );
        const hyperbolaInitial = pointSnapshot(hyperbolaGlider);
        hyperbolaGlider.setPosition(
            JXG.COORDS_BY_USER,
            [6, -1]
        );
        hyperbolaBoard.update(hyperbolaGlider);
        const hyperbolaDragged = pointSnapshot(hyperbolaGlider);
        hyperbolaPoint.setPosition(JXG.COORDS_BY_USER, [6, 3]);
        hyperbolaBoard.update();
        const hyperbolaParentMoved = pointSnapshot(hyperbolaGlider);
        hyperbolaBoard.update();
        const hyperbolaParentSettled = pointSnapshot(hyperbolaGlider);
        hyperbolaBoard.removeObject(hyperbola);

        const parabolaBoard = createBoard();
        const directrixPoint1 = parabolaBoard.create(
            "point",
            [-1, 4],
            {id: "directrixPoint1", name: ""}
        );
        const directrixPoint2 = parabolaBoard.create(
            "point",
            [-1, -4],
            {id: "directrixPoint2", name: ""}
        );
        const directrix = parabolaBoard.create(
            "line",
            [directrixPoint1, directrixPoint2],
            {id: "directrix", name: ""}
        );
        const parabolaFocus = parabolaBoard.create(
            "point",
            [1, 1],
            {id: "parabolaFocus", name: ""}
        );
        const parabola = parabolaBoard.create(
            "parabola",
            [parabolaFocus, directrix],
            {id: "parabola", name: ""}
        );
        const parabolaGlider = parabolaBoard.create(
            "glider",
            [3, 3, parabola],
            {id: "parabolaGlider", name: ""}
        );
        const parabolaInitial = pointSnapshot(parabolaGlider);
        parabolaGlider.setPosition(
            JXG.COORDS_BY_USER,
            [4, -2]
        );
        parabolaBoard.update(parabolaGlider);
        const parabolaDragged = pointSnapshot(parabolaGlider);
        parabolaFocus.setPosition(JXG.COORDS_BY_USER, [2, 2]);
        parabolaBoard.update();
        const parabolaParentMoved = pointSnapshot(parabolaGlider);
        parabolaBoard.update();
        const parabolaParentSettled = pointSnapshot(parabolaGlider);
        parabolaBoard.removeObject(parabola);

        return {
            version: JXG.version,
            functionGraph: {
                initial: functionInitial,
                dragged: functionDragged,
                parentMoved: functionParentMoved,
                parentSettled: functionParentSettled,
                draggedAfterParent: functionDraggedAfterParent
            },
            parametric: {
                initial: parametricInitial,
                dragged: parametricDragged,
                positioned: parametricPositioned
            },
            plot: {
                initial: plotInitial,
                dragged: plotDragged,
                positioned: plotPositioned,
                plotPresent: plotBoard.objects.plot !== undefined,
                gliderPresent:
                    plotBoard.objects.plotGlider !== undefined
            },
            hyperbola: {
                initial: hyperbolaInitial,
                dragged: hyperbolaDragged,
                parentMoved: hyperbolaParentMoved,
                parentSettled: hyperbolaParentSettled,
                hostPresent:
                    hyperbolaBoard.objects.hyperbola !== undefined,
                gliderPresent:
                    hyperbolaBoard.objects.hyperbolaGlider !== undefined
            },
            parabola: {
                initial: parabolaInitial,
                dragged: parabolaDragged,
                parentMoved: parabolaParentMoved,
                parentSettled: parabolaParentSettled,
                hostPresent:
                    parabolaBoard.objects.parabola !== undefined,
                gliderPresent:
                    parabolaBoard.objects.parabolaGlider !== undefined
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
