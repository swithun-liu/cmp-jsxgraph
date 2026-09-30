/*
 * Official JSXGraph 1.13.3 generic Conic behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/conic.mjs
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
            return value;
        };
        const classifyMatrix = (matrix) =>
            matrix.map((row) => row.map(classify));
        const relations = (element) => ({
            id: element.id,
            parents: [...element.parents],
            childElements: Object.keys(element.childElements)
        });
        const snapshot = (curve) => ({
            elType: curve.elType,
            type: curve.type,
            curveType: curve.evalVisProp("curvetype"),
            numberPoints: curve.numberPoints,
            quadraticform: classifyMatrix(curve.quadraticform),
            samples: [0, Math.PI / 6, Math.PI / 2, Math.PI]
                .map((phi) => [
                    classify(curve.X(phi)),
                    classify(curve.Y(phi))
                ]),
            center: curve.center.coords.usrCoords.map(classify),
            midpointIsCenter: curve.midpoint === curve.center,
            subsCenterIsCenter: curve.subs.center === curve.center,
            inherits: curve.inherits.map((element) => element.id),
            relations: relations(curve)
        });
        const attempt = (operation) => {
            try {
                const value = operation();
                return {
                    ok: true,
                    value: value?.elType ?? typeof value
                };
            } catch (error) {
                return {
                    ok: false,
                    name: error?.name ?? null,
                    message: error?.message ?? String(error)
                };
            }
        };

        const pointBoard = createBoard();
        const pointParents = [
            pointBoard.create("point", [1, 5], {id: "A", name: ""}),
            pointBoard.create("point", [1, 2], {id: "B", name: ""}),
            pointBoard.create("point", [2, 0], {id: "C", name: ""}),
            pointBoard.create("point", [0, 0], {id: "D", name: ""}),
            pointBoard.create("point", [-1, 5], {id: "E", name: ""})
        ];
        const pointConic = pointBoard.create(
            "conic",
            pointParents,
            {
                id: "point-conic",
                name: "",
                doAdvancedPlot: false,
                numberPointsHigh: 9,
                center: {id: "point-center", name: "", fixed: true}
            }
        );
        const pointInitial = snapshot(pointConic);
        pointParents[4].setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-2, 4]
        );
        pointBoard.update();
        const pointUpdated = snapshot(pointConic);

        const coefficientBoard = createBoard();
        const coefficientConic = coefficientBoard.create(
            "conic",
            [1, 2, -4, 0, 0, 0],
            {
                id: "coefficient-conic",
                name: "",
                doAdvancedPlot: false,
                numberPointsHigh: 9,
                center: {
                    id: "coefficient-center",
                    name: "",
                    fixed: true
                }
            }
        );
        const coefficient = snapshot(coefficientConic);

        const branchBoard = createBoard();
        const secondBranch = branchBoard.create(
            "conic",
            [1, -1, -1, 0, 0, 0],
            {
                id: "second-branch",
                name: "",
                doAdvancedPlot: false,
                numberPointsHigh: 9
            }
        );
        const thirdBranch = branchBoard.create(
            "conic",
            [1, -1, 1, 0, 0, 0],
            {
                id: "third-branch",
                name: "",
                doAdvancedPlot: false,
                numberPointsHigh: 9
            }
        );

        const dynamicBoard = createBoard();
        let xSquare = 1;
        let ySquare = 2;
        let constant = -4;
        let xLinear = 0;
        const dynamicConic = dynamicBoard.create(
            "conic",
            [
                () => xSquare,
                () => ySquare,
                () => constant,
                0,
                () => xLinear,
                0
            ],
            {
                id: "dynamic-conic",
                name: "",
                doAdvancedPlot: false,
                numberPointsHigh: 9,
                center: {id: "dynamic-center", name: ""}
            }
        );
        const dynamicInitial = snapshot(dynamicConic);
        xSquare = 2;
        ySquare = 1;
        constant = -9;
        xLinear = 2;
        dynamicBoard.update();
        const dynamicUpdated = snapshot(dynamicConic);

        const implicitBoard = createBoard();
        const implicitConic = implicitBoard.create(
            "conic",
            [[1, 5], [1, 2], [2, 0], [0, 0], [-1, 5]],
            {
                id: "implicit-conic",
                name: "",
                doAdvancedPlot: false,
                numberPointsHigh: 9,
                point: {fixed: true}
            }
        );
        const implicit = {
            conic: snapshot(implicitConic),
            objectCount: implicitBoard.objectsList.length,
            points: implicitConic.inherits
                .filter((element) => element.elType === "point")
                .map((point) => ({
                    coordinates: point.coords.usrCoords.map(classify),
                    fixed: point.visProp.fixed,
                    relations: relations(point)
                }))
        };

        const degenerateBoard = createBoard();
        const degenerate = degenerateBoard.create(
            "conic",
            [[0, 0], [1, 0], [2, 0], [3, 0], [4, 0]],
            {
                id: "degenerate",
                name: "",
                doAdvancedPlot: false,
                numberPointsHigh: 9
            }
        );

        const invalidBoard = createBoard();
        const invalidPoint = invalidBoard.create("point", [0, 0]);
        const invalidLine = invalidBoard.create(
            "line",
            [[0, 0], [1, 1]]
        );

        return {
            version: JXG.version,
            point: {
                initial: pointInitial,
                updated: pointUpdated,
                parentRelations: pointParents.map(relations),
                centerRelations: relations(pointConic.center)
            },
            coefficient,
            branches: {
                second: snapshot(secondBranch),
                third: snapshot(thirdBranch)
            },
            dynamic: {
                initial: dynamicInitial,
                updated: dynamicUpdated,
                centerRelations: relations(dynamicConic.center)
            },
            implicit,
            degenerate: snapshot(degenerate),
            invalid: {
                tooFew: attempt(() =>
                    invalidBoard.create("conic", [1, 2, 3, 4])
                ),
                matrixArray: attempt(() =>
                    invalidBoard.create(
                        "conic",
                        [[[1, 0, 0], [0, 1, 0], [0, 0, -1]]]
                    )
                ),
                wrongPointParent: attempt(() =>
                    invalidBoard.create(
                        "conic",
                        [
                            invalidPoint,
                            invalidLine,
                            [1, 0],
                            [0, 1],
                            [-1, 0]
                        ]
                    )
                )
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
