/*
 * Official JSXGraph 1.13.3 Inequality behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/inequality.mjs
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
            inverse: classify(curve.evalVisProp("inverse")),
            strokeColor: curve.evalVisProp("strokecolor"),
            strokeWidth: curve.evalVisProp("strokewidth"),
            fillColor: curve.evalVisProp("fillcolor"),
            fillOpacity: curve.evalVisProp("fillopacity"),
            hasPointAtOrigin: curve.hasPoint(0, 0),
            relations: relations(curve)
        });
        const captureError = (action, board) => {
            const before = board.objectsList.length;
            try {
                const value = action();
                board.update();
                return {
                    threw: false,
                    before,
                    after: board.objectsList.length,
                    value: value ? snapshot(value) : null,
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

        const lineBoard = createBoard();
        const first = lineBoard.create("point", [-3, -1], {
            id: "first",
            name: "",
            withLabel: false
        });
        const second = lineBoard.create("point", [3, 2], {
            id: "second",
            name: "",
            withLabel: false
        });
        let lineInverse = false;
        const sourceLine = lineBoard.create("line", [first, second], {
            id: "sourceLine",
            name: "",
            withLabel: false
        });
        const lineInequality = lineBoard.create(
            "inequality",
            [sourceLine],
            {
                id: "lineInequality",
                name: "",
                withLabel: false,
                inverse: () => lineInverse
            }
        );
        lineBoard.update();
        const lineInitial = snapshot(lineInequality);
        second.setPositionDirectly(JXG.COORDS_BY_USER, [1, 4]);
        lineInverse = true;
        lineBoard.update();
        const lineUpdated = snapshot(lineInequality);

        const functionBoard = createBoard();
        const driver = functionBoard.create("point", [1, 0], {
            id: "driver",
            name: "A",
            withLabel: false
        });
        let functionInverse = false;
        const sourceFunction = functionBoard.create(
            "functiongraph",
            [
                (x) => (
                    Math.abs(x) < 0.75
                        ? NaN
                        : driver.X() * x * x - 2
                ),
                -3,
                3
            ],
            {
                id: "sourceFunction",
                name: "",
                withLabel: false,
                doAdvancedPlot: false,
                numberPointsHigh: 8,
                numberPointsLow: 8
            }
        );
        const functionInequality = functionBoard.create(
            "inequality",
            [sourceFunction],
            {
                id: "functionInequality",
                name: "",
                withLabel: false,
                inverse: () => functionInverse
            }
        );
        functionBoard.update();
        const functionInitial = snapshot(functionInequality);
        driver.setPositionDirectly(JXG.COORDS_BY_USER, [2, 0]);
        functionInverse = true;
        functionBoard.update();
        const functionUpdated = snapshot(functionInequality);

        const emptyBoard = createBoard();
        const emptySource = emptyBoard.create(
            "functiongraph",
            [() => NaN, -2, 2],
            {
                id: "emptySource",
                name: "",
                withLabel: false,
                doAdvancedPlot: false,
                numberPointsHigh: 4,
                numberPointsLow: 4
            }
        );
        const emptyInequality = emptyBoard.create(
            "inequality",
            [emptySource],
            {
                id: "emptyInequality",
                name: "",
                withLabel: false
            }
        );
        emptyBoard.update();

        const removalBoard = createBoard();
        const removableLine = removalBoard.create(
            "line",
            [[-2, 0], [2, 0]],
            {id: "removableLine", name: "", withLabel: false}
        );
        const removableInequality = removalBoard.create(
            "inequality",
            [removableLine],
            {
                id: "removableInequality",
                name: "",
                withLabel: false
            }
        );
        removalBoard.update();
        const beforeRemoval = {
            source: relations(removableLine),
            inequality: relations(removableInequality),
            objectOrder: removalBoard.objectsList.map(
                (element) => element.id
            )
        };
        removalBoard.removeObject(removableLine);
        const afterRemoval = {
            sourcePresent:
                removalBoard.objects.removableLine === removableLine,
            inequalityPresent:
                removalBoard.objects.removableInequality ===
                    removableInequality,
            objectOrder: removalBoard.objectsList.map(
                (element) => element.id
            )
        };

        const invalidBoard = createBoard();
        const point = invalidBoard.create("point", [0, 0], {
            id: "point",
            name: "",
            withLabel: false
        });
        const parametric = invalidBoard.create(
            "curve",
            [(t) => t, (t) => t * t, -2, 2],
            {
                id: "parametric",
                name: "",
                withLabel: false,
                doAdvancedPlot: false,
                numberPointsHigh: 4,
                numberPointsLow: 4
            }
        );
        const data = invalidBoard.create(
            "curve",
            [[-2, 0, 2], [1, -1, 1]],
            {id: "data", name: "", withLabel: false}
        );
        const invalid = {
            empty: captureError(
                () => invalidBoard.create("inequality", []),
                invalidBoard
            ),
            point: captureError(
                () => invalidBoard.create("inequality", [point]),
                invalidBoard
            ),
            parametric: captureError(
                () => invalidBoard.create("inequality", [parametric]),
                invalidBoard
            ),
            data: captureError(
                () => invalidBoard.create("inequality", [data]),
                invalidBoard
            ),
            twoParents: captureError(
                () => invalidBoard.create(
                    "inequality",
                    [sourceLine, point]
                ),
                invalidBoard
            )
        };

        return {
            version: JXG.version,
            boundingBox: lineBoard.getBoundingBox(),
            line: {
                initial: lineInitial,
                updated: lineUpdated,
                source: relations(sourceLine)
            },
            functionGraph: {
                initial: functionInitial,
                updated: functionUpdated,
                source: relations(sourceFunction),
                driver: relations(driver)
            },
            emptyFunctionGraph: snapshot(emptyInequality),
            removal: {
                before: beforeRemoval,
                after: afterRemoval
            },
            invalid
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
