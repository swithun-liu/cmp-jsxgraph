/*
 * Official JSXGraph 1.13.3 SlopeField behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/slopefield.mjs
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
                boundingbox: [-8, 6, 8, -6],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
        };
        const classify = (value) => {
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (value === Number.POSITIVE_INFINITY) {
                return "Infinity";
            }
            if (value === Number.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
            if (value === undefined) {
                return "undefined";
            }
            return value;
        };
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            descendants: Object.keys(element.descendants),
            ancestors: Object.keys(element.ancestors)
        });
        const evaluatedMesh = (mesh) => mesh.map((value) => (
            classify(typeof value === "function" ? value() : value)
        ));
        const snapshot = (curve) => ({
            id: curve.id,
            elType: curve.elType,
            type: curve.type,
            elementClass: curve.elementClass,
            curveType: curve.evalVisProp("curvetype"),
            bezierDegree: curve.bezierDegree,
            numberPoints: curve.numberPoints,
            dataX: curve.dataX.map(classify),
            dataY: curve.dataY.map(classify),
            xData: evaluatedMesh(curve.xData),
            yData: evaluatedMesh(curve.yData),
            scale: classify(curve.evalVisProp("scale")),
            arrowHead: {
                enabled: curve.evalVisProp("arrowhead.enabled"),
                size: classify(curve.evalVisProp("arrowhead.size")),
                angle: classify(curve.evalVisProp("arrowhead.angle")),
                lastArrow: curve.evalVisProp("arrowhead.lastarrow")
            },
            strokeWidth: curve.evalVisProp("strokewidth"),
            strokeColor: curve.evalVisProp("strokecolor"),
            fillColor: curve.evalVisProp("fillcolor"),
            relations: relations(curve)
        });
        const captureError = (parents, attributes = {}) => {
            const board = createBoard();
            const before = board.objectsList.length;
            try {
                const value = board.create("slopefield", parents, {
                    id: "candidate",
                    name: "",
                    withLabel: false,
                    ...attributes
                });
                board.update();
                return {
                    threw: false,
                    before,
                    after: board.objectsList.length,
                    value: snapshot(value),
                    objectOrder: board.objectsList.map(
                        (element) => element.id
                    )
                };
            } catch (error) {
                return {
                    threw: true,
                    name: error?.name ?? null,
                    message: String(error?.message ?? error),
                    before,
                    after: board.objectsList.length,
                    objectOrder: board.objectsList.map(
                        (element) => element.id
                    )
                };
            }
        };

        const defaultBoard = createBoard();
        const defaultField = defaultBoard.create(
            "slopefield",
            [
                (x, y) => x - y,
                [-2, 2, 2],
                [-1, 1, 1]
            ],
            {
                id: "defaultField",
                name: "",
                withLabel: false
            }
        );
        defaultBoard.update();

        const stringBoard = createBoard();
        const stringField = stringBoard.create(
            "slopefield",
            [
                "x + y",
                [-1, 1, 1],
                [-1, 1, 1]
            ],
            {
                id: "stringField",
                name: "",
                withLabel: false,
                scale: 0.5,
                arrowHead: {enabled: false}
            }
        );
        stringBoard.update();

        const dynamicBoard = createBoard();
        const driver = dynamicBoard.create("point", [1, 0], {
            id: "driver",
            name: "",
            withLabel: false
        });
        const state = {
            xSteps: 1,
            ySteps: 1,
            scale: 1,
            arrowEnabled: true,
            arrowSize: 6,
            arrowAngle: Math.PI / 6
        };
        const dynamicField = dynamicBoard.create(
            "slopefield",
            [
                (x, y) => driver.X() * x - y,
                [-2, () => state.xSteps, 2],
                [-1, () => state.ySteps, 1]
            ],
            {
                id: "dynamicField",
                name: "",
                withLabel: false,
                scale: () => state.scale,
                arrowHead: {
                    enabled: () => state.arrowEnabled,
                    size: () => state.arrowSize,
                    angle: () => state.arrowAngle
                }
            }
        );
        dynamicBoard.update();
        const dynamicInitial = snapshot(dynamicField);
        driver.setPositionDirectly(JXG.COORDS_BY_USER, [2, 0]);
        state.xSteps = 2.5;
        state.ySteps = 2;
        state.scale = 0.5;
        state.arrowEnabled = false;
        state.arrowSize = 9;
        state.arrowAngle = Math.PI / 4;
        dynamicBoard.update();
        const dynamicUpdated = snapshot(dynamicField);

        const setFBoard = createBoard();
        const setFField = setFBoard.create(
            "slopefield",
            [
                () => 0,
                [0, 0, 0],
                [0, 0, 0]
            ],
            {
                id: "setFField",
                name: "",
                withLabel: false,
                arrowHead: {enabled: false}
            }
        );
        setFBoard.update();
        const setFInitial = snapshot(setFField);
        const setFReturn = setFField.setF((x, y) => x + y, "x, y");
        setFBoard.update();

        return {
            version: JXG.version,
            boardUnits: {
                unitX: defaultBoard.unitX,
                unitY: defaultBoard.unitY
            },
            defaults: snapshot(defaultField),
            stringParent: snapshot(stringField),
            dynamic: {
                initial: dynamicInitial,
                updated: dynamicUpdated,
                driver: relations(driver)
            },
            setF: {
                initial: setFInitial,
                returnValue: classify(setFReturn),
                updated: snapshot(setFField)
            },
            scalarEdges: {
                positiveInfinity: captureError(
                    [
                        () => Number.POSITIVE_INFINITY,
                        [0, 0, 0],
                        [0, 0, 0]
                    ],
                    {arrowHead: {enabled: false}}
                ),
                negativeInfinity: captureError(
                    [
                        () => Number.NEGATIVE_INFINITY,
                        [0, 0, 0],
                        [0, 0, 0]
                    ],
                    {arrowHead: {enabled: false}}
                ),
                nan: captureError(
                    [
                        () => Number.NaN,
                        [0, 0, 0],
                        [0, 0, 0]
                    ],
                    {arrowHead: {enabled: false}}
                )
            },
            invalid: {
                missingParents: captureError([]),
                twoParents: captureError([
                    (x, y) => x + y,
                    [-1, 1, 1]
                ]),
                numericFunction: captureError([
                    1,
                    [-1, 1, 1],
                    [-1, 1, 1]
                ]),
                arrayFunction: captureError([
                    [(x, y) => x, (x, y) => y],
                    [-1, 1, 1],
                    [-1, 1, 1]
                ]),
                shortXData: captureError([
                    (x, y) => x + y,
                    [-1, 1],
                    [-1, 1, 1]
                ]),
                shortYData: captureError([
                    (x, y) => x + y,
                    [-1, 1, 1],
                    [-1, 1]
                ])
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
