/*
 * Official JSXGraph 1.13.3 function-coordinate Point behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/point-function-coordinates.mjs
 */
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const visualParityPackage = resolve(
    repositoryRoot,
    "tools/visual-parity/package.json"
);
const require = createRequire(visualParityPackage);
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
            boundingbox: [-8, 6, 8, -6],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const driver = board.create("point", [2, 3], {
            id: "driver",
            name: "Driver",
            withLabel: false
        });
        let arrayCalls = 0;
        const arrayPoint = board.create(
            "point",
            [
                function () {
                    arrayCalls += 1;
                    return [driver.X() + 1, driver.Y() + 2];
                }
            ],
            {id: "arrayPoint", name: "", withLabel: false}
        );
        let mixedCalls = 0;
        const mixedPoint = board.create(
            "point",
            [
                function () {
                    mixedCalls += 1;
                    return driver.X() * 2;
                },
                "Driver.Y() - 1"
            ],
            {id: "mixedPoint", name: "", withLabel: false}
        );
        let homogeneousCalls = 0;
        const homogeneousPoint = board.create(
            "point",
            [
                function () {
                    homogeneousCalls += 1;
                    return [
                        2,
                        driver.X() * 2,
                        driver.Y() * 2,
                        99
                    ];
                }
            ],
            {id: "homogeneousPoint", name: "", withLabel: false}
        );

        const snapshot = () => ({
            arrayPoint: [...arrayPoint.coords.usrCoords],
            mixedPoint: [...mixedPoint.coords.usrCoords],
            homogeneousPoint: [...homogeneousPoint.coords.usrCoords],
            arrayCalls,
            mixedCalls,
            homogeneousCalls,
            arrayParents: [...arrayPoint.parents],
            mixedParents: [...mixedPoint.parents],
            homogeneousParents: [...homogeneousPoint.parents],
            driverChildren: Object.keys(driver.childElements),
            arrayDraggable: arrayPoint.isDraggable,
            mixedDraggable: mixedPoint.isDraggable,
            homogeneousDraggable: homogeneousPoint.isDraggable
        });

        const initial = snapshot();
        driver.setPositionDirectly(JXG.COORDS_BY_USER, [5, -1]);
        board.update();
        const moved = snapshot();

        let midpointFunctionError = null;
        try {
            board.create(
                "midpoint",
                [
                    function () {
                        return [0, 0];
                    },
                    [2, 2]
                ],
                {withLabel: false}
            );
        } catch (error) {
            midpointFunctionError = String(error);
        }

        return {
            version: JXG.version,
            initial,
            moved,
            midpointFunctionError
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
