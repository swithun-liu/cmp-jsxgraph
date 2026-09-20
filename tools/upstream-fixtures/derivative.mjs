/*
 * Official JSXGraph 1.13.3 Derivative behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/derivative.mjs
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
        const snapshot = (curve, parameters) => ({
            id: curve.id,
            elType: curve.elType,
            type: curve.type,
            elementClass: curve.elementClass,
            curveType: curve.evalVisProp("curvetype"),
            minX: classify(curve.minX()),
            maxX: classify(curve.maxX()),
            numberPoints: curve.numberPoints,
            values: parameters.map((parameter) => ({
                parameter,
                x: classify(curve.X(parameter)),
                y: classify(curve.Y(parameter))
            })),
            points: curve.points
                .slice(0, Math.min(curve.numberPoints, 5))
                .map((point) => [
                    classify(point.usrCoords[1]),
                    classify(point.usrCoords[2])
                ]),
            relations: relations(curve)
        });
        const captureError = (action) => {
            try {
                const value = action();
                return {
                    threw: false,
                    id: value?.id ?? null,
                    elType: value?.elType ?? null,
                    parents: value ? [...value.parents] : []
                };
            } catch (error) {
                return {
                    threw: true,
                    name: error?.name ?? null,
                    message: String(error?.message ?? error)
                };
            }
        };

        const board = createBoard();
        const coefficient = board.create("point", [2, 0], {
            id: "A",
            name: "A",
            withLabel: false
        });
        let dynamicMinimum = -3;
        const functionCurve = board.create(
            "functiongraph",
            [
                "A.X() * x * x + 3 * x - 1",
                () => dynamicMinimum,
                4
            ],
            {id: "functionCurve", name: "", withLabel: false}
        );
        const functionDerivative = board.create(
            "derivative",
            [functionCurve],
            {
                id: "functionDerivative",
                name: "",
                withLabel: false,
                doAdvancedPlot: false,
                numberPointsHigh: 8
            }
        );
        board.update();
        const functionInitial = {
            source: snapshot(functionCurve, [-2, 0, 2]),
            derivative: snapshot(functionDerivative, [-2, 0, 2]),
            coefficientChildren: Object.keys(coefficient.childElements)
        };
        coefficient.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [4, 0]
        );
        dynamicMinimum = -2;
        board.update();
        const functionUpdated = {
            source: snapshot(functionCurve, [-2, 0, 2]),
            derivative: snapshot(functionDerivative, [-2, 0, 2])
        };

        const parametricCurve = board.create(
            "curve",
            [
                (t) => 2 * Math.cos(t),
                (t) => 3 * Math.sin(t),
                0,
                2 * Math.PI
            ],
            {id: "parametricCurve", name: "", withLabel: false}
        );
        const parametricDerivative = board.create(
            "derivative",
            [parametricCurve],
            {
                id: "parametricDerivative",
                name: "",
                withLabel: false,
                doAdvancedPlot: false,
                numberPointsHigh: 8
            }
        );
        board.update();

        const dataCurve = board.create(
            "curve",
            [[-4, -1, 2, 5], [-2, 2, -1, 3]],
            {id: "dataCurve", name: "", withLabel: false}
        );
        const dataDerivative = board.create(
            "derivative",
            [dataCurve],
            {
                id: "dataDerivative",
                name: "",
                withLabel: false,
                doAdvancedPlot: false,
                numberPointsHigh: 8
            }
        );
        board.update();

        const ignoredPoint = board.create("point", [1, 1], {
            id: "ignoredPoint",
            name: "",
            withLabel: false
        });
        const invalid = {
            empty: captureError(() => board.create("derivative", [])),
            point: captureError(
                () => board.create("derivative", [ignoredPoint])
            ),
            curveThenPoint: captureError(
                () => board.create(
                    "derivative",
                    [functionCurve, ignoredPoint]
                )
            ),
            pointThenCurve: captureError(
                () => board.create(
                    "derivative",
                    [ignoredPoint, functionCurve]
                )
            ),
            twoCurves: captureError(
                () => board.create(
                    "derivative",
                    [functionCurve, parametricCurve]
                )
            )
        };

        const removalBoard = createBoard();
        const removableSource = removalBoard.create(
            "functiongraph",
            [(x) => x * x, -2, 2],
            {id: "removableSource", name: "", withLabel: false}
        );
        const removableDerivative = removalBoard.create(
            "derivative",
            [removableSource],
            {
                id: "removableDerivative",
                name: "",
                withLabel: false
            }
        );
        const beforeRemoval = {
            source: relations(removableSource),
            derivative: relations(removableDerivative),
            objectOrder: removalBoard.objectsList.map(
                (element) => element.id
            )
        };
        removalBoard.removeObject(removableSource);
        const afterRemoval = {
            sourcePresent:
                removalBoard.objects.removableSource === removableSource,
            derivativePresent:
                removalBoard.objects.removableDerivative ===
                    removableDerivative,
            objectOrder: removalBoard.objectsList.map(
                (element) => element.id
            )
        };

        return {
            version: JXG.version,
            functionInitial,
            functionUpdated,
            parametric: {
                source: snapshot(
                    parametricCurve,
                    [0, Math.PI / 4, Math.PI / 2, Math.PI]
                ),
                derivative: snapshot(
                    parametricDerivative,
                    [0, Math.PI / 4, Math.PI / 2, Math.PI]
                )
            },
            data: {
                source: snapshot(dataCurve, [-1, 0, 1, 2]),
                derivative: snapshot(dataDerivative, [-1, 0, 1, 2])
            },
            invalid,
            removal: {
                before: beforeRemoval,
                after: afterRemoval
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
