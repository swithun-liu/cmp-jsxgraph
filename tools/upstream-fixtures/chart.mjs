/*
 * Official JSXGraph 1.13.3 Chart behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/chart.mjs
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
                boundingbox: [-8, 8, 12, -8],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
        };
        const elementSnapshot = (element) => {
            if (Array.isArray(element)) {
                return element.map(elementSnapshot);
            }
            if (!element || typeof element !== "object") {
                return element ?? null;
            }
            const result = {
                id: element.id ?? null,
                type: element.type ?? null,
                elementClass: element.elementClass ?? null,
                elType: element.elType ?? null
            };
            if (Array.isArray(element.dataX)) {
                result.dataX = element.dataX.slice();
            }
            if (Array.isArray(element.dataY)) {
                result.dataY = element.dataY.slice();
            }
            if (element.coords?.usrCoords) {
                result.coordinates = element.coords.usrCoords.slice();
            }
            if (Array.isArray(element.vertices)) {
                result.vertices = element.vertices.map((point) =>
                    point.coords.usrCoords.slice()
                );
            }
            if (element.point1 && element.point2 && element.point3) {
                result.sectorPoints = [
                    element.point1.coords.usrCoords.slice(),
                    element.point2.coords.usrCoords.slice(),
                    element.point3.coords.usrCoords.slice()
                ];
            }
            return result;
        };
        const boardSnapshot = (board) =>
            board.objectsList.map((element) => ({
                id: element.id,
                type: element.type,
                elementClass: element.elementClass,
                elType: element.elType,
                visible: element.visProp.visible,
                name: element.name
            }));
        const create = (style, parents, attributes = {}) => {
            const board = createBoard();
            const value = board.create("chart", parents, {
                chartStyle: style,
                name: "",
                ...attributes
            });
            board.update();
            return {
                result: elementSnapshot(value),
                objects: boardSnapshot(board)
            };
        };
        const captureError = (parents, attributes = {}) => {
            const board = createBoard();
            const before = board.objectsList.length;
            try {
                const value = board.create("chart", parents, attributes);
                return {
                    threw: false,
                    result: elementSnapshot(value),
                    objectDelta: board.objectsList.length - before
                };
            } catch (error) {
                return {
                    threw: true,
                    name: error.name,
                    message: String(error.message),
                    objectDelta: board.objectsList.length - before
                };
            }
        };
        const dynamicLine = (() => {
            const board = createBoard();
            let y = 2;
            const beforeCount = board.objectsList.length;
            try {
                const value = board.create(
                    "chart",
                    [[1, 2], [() => y, 4]],
                    {chartStyle: "line", name: ""}
                );
                const curve = value[0];
                board.update();
                const before = curve.points.map((point) =>
                    point.usrCoords.slice()
                );
                y = 6;
                board.update();
                return {
                    threw: false,
                    before,
                    after: curve.points.map((point) =>
                        point.usrCoords.slice()
                    )
                };
            } catch (error) {
                return {
                    threw: true,
                    name: error.name,
                    message: String(error.message),
                    objectDelta: board.objectsList.length - beforeCount
                };
            }
        })();
        const dynamicPieLabel = (() => {
            const board = createBoard();
            let second = 3;
            const value = board.create(
                "chart",
                [1, () => second],
                {
                    chartStyle: "pie",
                    labels: ["A", "B"],
                    radius: 4,
                    name: ""
                }
            );
            const sector = value[0].sectors[0];
            board.update();
            const before = sector.label?.coords?.usrCoords?.slice() ?? null;
            second = 1;
            board.update();
            return {
                before,
                after: sector.label?.coords?.usrCoords?.slice() ?? null
            };
        })();
        const legend = (() => {
            const board = createBoard();
            const value = board.create("legend", [1, 2], {
                labels: ["A", "B"],
                colors: ["#ff0000", "#0000ff"]
            });
            board.update();
            return {
                result: elementSnapshot(value),
                objects: boardSnapshot(board)
            };
        })();

        return {
            version: JXG.version,
            line: create("line", [[1, 2, 4], [3, -1, 2]]),
            point: create("point", [3, -1, 2]),
            bar: create(
                "bar",
                [[1, 3, 6], [2, -1, 4]],
                {
                    colors: ["#ff0000", "#00ff00"],
                    labels: ["A", "B", "C"]
                }
            ),
            pie: create(
                "pie",
                [2, 3, 5],
                {
                    center: [1, -1],
                    radius: 3,
                    labels: ["A", "B", "C"],
                    colors: ["#ff0000", "#00ff00", "#0000ff"]
                }
            ),
            spline: create("spline", [[1, 2, 4], [3, -1, 2]]),
            fit: create(
                "fit",
                [[1, 2, 3], [2, 5, 10]],
                {degree: 2}
            ),
            combined: create("line, point", [3, -1, 2]),
            dynamicLine,
            dynamicPieLabel,
            radar: create(
                "radar",
                [
                    [2, 4, 3],
                    [4, 1, 5]
                ],
                {
                    paramArray: ["A", "B", "C"],
                    labelArray: ["first", "second"],
                    colors: ["#ff0000", "#0000ff"],
                    radius: 4,
                    start: 0,
                    showCircles: true,
                    circleLabelArray: [0, 50, 100]
                }
            ),
            radarThreeRows: create(
                "radar",
                [
                    [2, 4, 3],
                    [4, 1, 5],
                    [3, 2, 4]
                ],
                {
                    paramArray: ["A", "B", "C"],
                    labelArray: ["first", "second", "third"]
                }
            ),
            legend,
            failures: {
                noData: captureError([], {chartStyle: "line"}),
                emptyNested: captureError([[]], {chartStyle: "line"}),
                unknownStyle: captureError([1, 2], {
                    chartStyle: "unknown"
                }),
                radarMissingParams: captureError([[1, 2], [3, 4]], {
                    chartStyle: "radar"
                }),
                radarMismatchedData: captureError([[1, 2]], {
                    chartStyle: "radar",
                    paramArray: ["A", "B", "C"]
                })
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
