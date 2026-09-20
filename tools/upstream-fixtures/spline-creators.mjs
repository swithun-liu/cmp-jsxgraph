/*
 * Official JSXGraph 1.13.3 Spline/CardinalSpline behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/spline-creators.mjs
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
                .slice(0, Math.min(curve.numberPoints, 9))
                .map((point) => [
                    classify(point.usrCoords[1]),
                    classify(point.usrCoords[2])
                ]),
            relations: relations(curve)
        });
        const pointSnapshot = (point) => ({
            id: point.id,
            coordinates: point.coords.usrCoords.map(classify),
            visible: point.visProp.visible,
            fixed: point.visProp.fixed,
            relations: relations(point)
        });
        const capture = (action) => {
            try {
                const value = action();
                return {
                    threw: false,
                    value: value
                        ? snapshot(value, [-1, 0, 0.5, 1, 2, 3, 4])
                        : null
                };
            } catch (error) {
                return {
                    threw: true,
                    name: error?.name ?? null,
                    message: String(error?.message ?? error)
                };
            }
        };
        const point = (board, coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false
            });

        const splineBoard = createBoard();
        const a = point(splineBoard, [2, 0], "A");
        const b = point(splineBoard, [-2, 2], "B");
        const c = point(splineBoard, [0, -1], "C");
        const d = point(splineBoard, [4, 1], "D");
        const pointSpline = splineBoard.create(
            "spline",
            [a, b, c, d],
            {
                id: "pointSpline",
                name: "",
                withLabel: false,
                doAdvancedPlot: false,
                numberPointsHigh: 8
            }
        );
        splineBoard.update();
        const pointSplineInitial = snapshot(
            pointSpline,
            [-3, -2, -1, 0, 1, 2, 3, 4, 5]
        );
        a.setPositionDirectly(JXG.COORDS_BY_USER, [3, -2]);
        splineBoard.update();
        const pointSplineUpdated = snapshot(
            pointSpline,
            [-3, -2, -1, 0, 1, 2, 3, 4, 5]
        );

        const splineFormsBoard = createBoard();
        const splineForms = {
            coordinatePairs: capture(() =>
                splineFormsBoard.create(
                    "spline",
                    [[-3, 1], [-1, 4], [2, -2], [5, 2]],
                    {
                        id: "coordinatePairs",
                        name: "",
                        withLabel: false,
                        doAdvancedPlot: false,
                        numberPointsHigh: 8
                    }
                )
            ),
            coordinateArrays: capture(() =>
                splineFormsBoard.create(
                    "spline",
                    [[-3, -1, 2, 5], [1, 4, -2, 2]],
                    {
                        id: "coordinateArrays",
                        name: "",
                        withLabel: false,
                        doAdvancedPlot: false,
                        numberPointsHigh: 8
                    }
                )
            ),
            twoCoordinatePairs: capture(() =>
                splineFormsBoard.create(
                    "spline",
                    [[-2, 1], [3, 4]],
                    {
                        id: "twoCoordinatePairs",
                        name: "",
                        withLabel: false,
                        doAdvancedPlot: false,
                        numberPointsHigh: 8
                    }
                )
            )
        };

        const cardinalBoard = createBoard();
        const p0 = point(cardinalBoard, [-4, 0], "P0");
        const p1 = point(cardinalBoard, [-2, 3], "P1");
        const p2 = point(cardinalBoard, [1, -2], "P2");
        const p3 = point(cardinalBoard, [4, 2], "P3");
        let tension = 0.35;
        const uniform = cardinalBoard.create(
            "cardinalspline",
            [[p0, p1, p2, p3], () => tension, "uniform"],
            {
                id: "uniform",
                name: "",
                withLabel: false,
                doAdvancedPlot: false,
                numberPointsHigh: 8
            }
        );
        const centripetal = cardinalBoard.create(
            "cardinalspline",
            [[p0, p1, p2, p3], 0.5, "centripetal"],
            {
                id: "centripetal",
                name: "",
                withLabel: false,
                doAdvancedPlot: false,
                numberPointsHigh: 8
            }
        );
        const unknownType = cardinalBoard.create(
            "cardinalspline",
            [[p0, p1, p2, p3], 0.5, "unknown"],
            {
                id: "unknownType",
                name: "",
                withLabel: false,
                doAdvancedPlot: false,
                numberPointsHigh: 8
            }
        );
        cardinalBoard.update();
        const cardinalInitial = {
            uniform: snapshot(uniform, [-1, 0, 0.5, 1, 1.5, 2, 3, 4]),
            centripetal: snapshot(
                centripetal,
                [-1, 0, 0.5, 1, 1.5, 2, 3, 4]
            ),
            unknownType: snapshot(
                unknownType,
                [-1, 0, 0.5, 1, 1.5, 2, 3, 4]
            ),
            pointChildren: [p0, p1, p2, p3].map((p) => ({
                id: p.id,
                children: Object.keys(p.childElements)
            }))
        };
        tension = 0.8;
        p2.setPositionDirectly(JXG.COORDS_BY_USER, [2, -3]);
        cardinalBoard.update();
        const cardinalUpdated = {
            uniform: snapshot(uniform, [0, 0.5, 1, 1.5, 2, 3]),
            centripetal: snapshot(
                centripetal,
                [0, 0.5, 1, 1.5, 2, 3]
            )
        };

        const cardinalFormsBoard = createBoard();
        const existing = point(cardinalFormsBoard, [0, 4], "existing");
        const cardinalForms = {};
        for (const [name, points, attributes] of [
            [
                "coordinatePairsDefault",
                [[-4, 0], [-2, 3], [1, -2], [4, 2]],
                {}
            ],
            [
                "coordinatePairsNoCreate",
                [[-4, 0], [-2, 3], [1, -2], [4, 2]],
                {createPoints: false}
            ],
            [
                "coordinateArrays",
                [[-4, -2, 1, 4], [0, 3, -2, 2]],
                {isArrayOfCoordinates: false}
            ],
            [
                "mixed",
                [[-4, 0], existing, [1, -2], [4, 2]],
                {}
            ]
        ]) {
            const beforeIds = cardinalFormsBoard.objectsList.map(
                (element) => element.id
            );
            const outcome = capture(() =>
                cardinalFormsBoard.create(
                    "cardinalspline",
                    [points, 0.5, "uniform"],
                    {
                        id: name,
                        name: "",
                        withLabel: false,
                        doAdvancedPlot: false,
                        numberPointsHigh: 8,
                        ...attributes
                    }
                )
            );
            const before = new Set(beforeIds);
            const generatedPoints = cardinalFormsBoard.objectsList
                .filter(
                    (element) =>
                        element.elType === "point" &&
                        !before.has(element.id)
                )
                .map(pointSnapshot);
            cardinalForms[name] = {
                outcome,
                generatedPoints
            };
        }

        const invalidBoard = createBoard();
        const invalid = {
            splineEmpty: capture(() =>
                invalidBoard.create("spline", [], {
                    doAdvancedPlot: false,
                    numberPointsHigh: 8
                })
            ),
            splineOnePoint: capture(() =>
                invalidBoard.create(
                    "spline",
                    [point(invalidBoard, [0, 0], "singleSplinePoint")],
                    {doAdvancedPlot: false, numberPointsHigh: 8}
                )
            ),
            cardinalNoParents: capture(() =>
                invalidBoard.create("cardinalspline", [])
            ),
            cardinalNoTension: capture(() =>
                invalidBoard.create(
                    "cardinalspline",
                    [[[0, 0], [1, 1]]]
                )
            ),
            cardinalInvalidPoints: capture(() =>
                invalidBoard.create(
                    "cardinalspline",
                    ["not-an-array", 0.5]
                )
            ),
            cardinalOnePoint: capture(() =>
                invalidBoard.create(
                    "cardinalspline",
                    [[[0, 0]], 0.5],
                    {doAdvancedPlot: false, numberPointsHigh: 8}
                )
            ),
            cardinalMissingType: capture(() =>
                invalidBoard.create(
                    "cardinalspline",
                    [[[0, 0], [1, 1], [2, 0]], 0.5],
                    {doAdvancedPlot: false, numberPointsHigh: 8}
                )
            ),
            cardinalNonStringType: capture(() =>
                invalidBoard.create(
                    "cardinalspline",
                    [[[0, 0], [1, 1], [2, 0]], 0.5, 7],
                    {doAdvancedPlot: false, numberPointsHigh: 8}
                )
            ),
            cardinalInvalidTension: capture(() =>
                invalidBoard.create(
                    "cardinalspline",
                    [[[0, 0], [1, 1], [2, 0]], "bad"]
                )
            )
        };

        return {
            version: JXG.version,
            pointSpline: {
                initial: pointSplineInitial,
                updated: pointSplineUpdated,
                pointChildren: [a, b, c, d].map((p) => ({
                    id: p.id,
                    children: Object.keys(p.childElements)
                }))
            },
            splineForms,
            cardinal: {
                initial: cardinalInitial,
                updated: cardinalUpdated
            },
            cardinalForms,
            invalid
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
