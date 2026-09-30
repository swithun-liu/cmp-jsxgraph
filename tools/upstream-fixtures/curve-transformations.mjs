/*
 * Official JSXGraph 1.13.3 transformed Curve and Glider behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/curve-transformations.mjs
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
        const coordinates = (curve) => curve.points
            .slice(0, curve.numberPoints)
            .map((point) => point.usrCoords.slice());
        const pointSnapshot = (point) => ({
            coordinates: point.coords.usrCoords.slice(),
            position: point.position
        });

        const dataBoard = createBoard();
        const dataSource = dataBoard.create(
            "curve",
            [[-1, 0, 2], [0, 2, -1]],
            {id: "dataSource", name: ""}
        );
        const scale = dataBoard.create(
            "transform",
            [2, -1],
            {type: "scale"}
        );
        const translate = dataBoard.create(
            "transform",
            [3, 4],
            {type: "translate"}
        );
        const dataTransformed = dataBoard.create(
            "curve",
            [dataSource, [scale, translate]],
            {id: "dataTransformed", name: ""}
        );

        const dynamicBoard = createBoard();
        const driver = dynamicBoard.create(
            "point",
            [1, 0],
            {id: "driver", name: ""}
        );
        const continuousSource = dynamicBoard.create(
            "curve",
            [(t) => t, (t) => t * t, -2, 2],
            {
                id: "continuousSource",
                name: "",
                curveType: "parameter",
                doAdvancedPlot: false,
                numberPointsHigh: 5,
                RDPsmoothing: false
            }
        );
        const dynamicTranslate = dynamicBoard.create(
            "transform",
            [() => driver.X(), 2],
            {type: "translate"}
        );
        const rotate = dynamicBoard.create(
            "transform",
            [Math.PI / 2],
            {type: "rotate"}
        );
        const firstTransform = dynamicBoard.create(
            "curve",
            [continuousSource, dynamicTranslate],
            {id: "firstTransform", name: ""}
        );
        const nestedTransform = dynamicBoard.create(
            "curve",
            [firstTransform, rotate],
            {id: "nestedTransform", name: ""}
        );
        const initialContinuous = {
            first: coordinates(firstTransform),
            nested: coordinates(nestedTransform),
            ft: nestedTransform.Ft(1)
        };
        const glider = dynamicBoard.create(
            "glider",
            [0, 4, nestedTransform],
            {id: "glider", name: ""}
        );
        const gliderInitial = pointSnapshot(glider);
        glider.setPosition(JXG.COORDS_BY_USER, [-3, 4]);
        dynamicBoard.update(glider);
        const gliderDragged = pointSnapshot(glider);
        driver.setPosition(JXG.COORDS_BY_USER, [3, 0]);
        dynamicBoard.update();
        const gliderParentMoved = pointSnapshot(glider);
        dynamicBoard.update();
        const gliderParentSettled = pointSnapshot(glider);
        const updatedContinuous = {
            first: coordinates(firstTransform),
            nested: coordinates(nestedTransform),
            ft: nestedTransform.Ft(1)
        };

        return {
            version: JXG.version,
            data: {
                source: coordinates(dataSource),
                transformedDataX: dataTransformed.dataX.slice(),
                transformedDataY: dataTransformed.dataY.slice(),
                transformed: coordinates(dataTransformed),
                transformMat: dataTransformed.transformMat.map(
                    (row) => row.slice()
                ),
                parents: [...dataTransformed.parents],
                sourceChildren: Object.keys(dataSource.childElements)
            },
            continuous: {
                initial: initialContinuous,
                updated: updatedContinuous
            },
            glider: {
                initial: gliderInitial,
                dragged: gliderDragged,
                parentMoved: gliderParentMoved,
                parentSettled: gliderParentSettled
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
