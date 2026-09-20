/*
 * Official JSXGraph 1.13.3 BoxPlot behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/boxplot.mjs
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

        const createBoard = (boundingbox = [-8, 8, 8, -8]) => {
            const id = `board-${boardIndex}`;
            boardIndex += 1;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = "640px";
            container.style.height = "480px";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox,
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
            if (Array.isArray(value)) {
                return value.map(classify);
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
            quantiles: curve.Q.map((term) => classify(term())),
            axis: classify(curve.x()),
            width: classify(curve.w()),
            dir: curve.evalVisProp("dir"),
            smallWidth: curve.evalVisProp("smallwidth"),
            outlierFace: curve.evalVisProp("outlier.face"),
            outlierSize: curve.evalVisProp("outlier.size"),
            strokeWidth: curve.evalVisProp("strokewidth"),
            strokeColor: curve.evalVisProp("strokecolor"),
            fillColor: curve.evalVisProp("fillcolor"),
            fillOpacity: curve.evalVisProp("fillopacity"),
            relations: relations(curve)
        });
        const captureError = (parents, attributes = {}) => {
            const board = createBoard();
            const before = board.objectsList.length;
            try {
                const value = board.create("boxplot", parents, {
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
                    value: snapshot(value)
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

        const staticBoard = createBoard();
        const vertical = staticBoard.create(
            "boxplot",
            [[-4, -2, 0, 3, 5, [-6, 7]], -2, 4],
            {
                id: "vertical",
                name: "",
                withLabel: false
            }
        );
        const horizontal = staticBoard.create(
            "boxplot",
            [[-5, -3, -1, 2, 4, [-7, 6]], 3, 2],
            {
                id: "horizontal",
                name: "",
                withLabel: false,
                dir: "horizontal",
                smallWidth: 0.25,
                outlier: {
                    face: "square",
                    size: 5
                }
            }
        );
        staticBoard.update();

        const dynamicBoard = createBoard();
        const driver = dynamicBoard.create("point", [2, 3], {
            id: "driver",
            name: "",
            withLabel: false
        });
        const dynamic = dynamicBoard.create(
            "boxplot",
            [
                [
                    "driver.Y() - 7",
                    "driver.X() - 4",
                    "0.5 * driver.X()",
                    "driver.X()",
                    "driver.Y() + 2",
                    [-6, 7]
                ],
                "driver.X() - 1",
                "driver.Y() + 1"
            ],
            {
                id: "dynamic",
                name: "",
                withLabel: false,
                smallWidth: 0.75,
                outlier: {
                    face: "plus",
                    size: 4
                }
            }
        );
        dynamicBoard.update();
        const dynamicInitial = {
            curve: snapshot(dynamic),
            driver: relations(driver),
            objectOrder: dynamicBoard.objectsList.map(
                (element) => element.id
            )
        };
        driver.setPositionDirectly(JXG.COORDS_BY_USER, [4, 5]);
        dynamicBoard.update();
        const dynamicUpdated = {
            curve: snapshot(dynamic),
            driver: relations(driver)
        };

        const faceBoard = createBoard([-4, 4, 4, -4]);
        const faces = [
            "x",
            "cross",
            "[]",
            "square",
            "<>",
            "diamond",
            "<<>>",
            "diamond2",
            "+",
            "plus",
            "-",
            "minus",
            "|",
            "divide",
            "o",
            "circle",
            "unknown"
        ];
        const faceSnapshots = {};
        for (const face of faces) {
            const curve = faceBoard.create(
                "boxplot",
                [[-2, -1, 0, 1, 2, [3]], 0, 2],
                {
                    id: `face-${face}`,
                    name: "",
                    withLabel: false,
                    outlier: {
                        face,
                        size: 6
                    }
                }
            );
            faceBoard.update();
            faceSnapshots[face] = snapshot(curve);
        }

        const directionBoard = createBoard();
        const unknownDirection = directionBoard.create(
            "boxplot",
            [[-3, -1, 0, 1, 4], 2, 3],
            {
                id: "unknownDirection",
                name: "",
                withLabel: false,
                dir: "diagonal"
            }
        );
        directionBoard.update();

        const removalBoard = createBoard();
        const removalDriver = removalBoard.create("point", [2, 3], {
            id: "source",
            name: "",
            withLabel: false
        });
        const removable = removalBoard.create(
            "boxplot",
            [
                [
                    "source.Y() - 6",
                    -1,
                    0,
                    1,
                    "source.Y()"
                ],
                "source.X()",
                2
            ],
            {
                id: "removable",
                name: "",
                withLabel: false
            }
        );
        removalBoard.update();
        const removalBefore = {
            driver: relations(removalDriver),
            curve: snapshot(removable),
            objectOrder: removalBoard.objectsList.map(
                (element) => element.id
            )
        };
        removalBoard.removeObject(removable);
        const removalAfterCurve = {
            driverPresent:
                removalBoard.objects.source === removalDriver,
            curvePresent: removalBoard.objects.removable === removable,
            driver: relations(removalDriver),
            objectOrder: removalBoard.objectsList.map(
                (element) => element.id
            )
        };

        return {
            version: JXG.version,
            boardUnits: {
                static: {
                    unitX: staticBoard.unitX,
                    unitY: staticBoard.unitY
                },
                faces: {
                    unitX: faceBoard.unitX,
                    unitY: faceBoard.unitY
                }
            },
            static: {
                vertical: snapshot(vertical),
                horizontal: snapshot(horizontal),
                unknownDirection: snapshot(unknownDirection)
            },
            dynamic: {
                initial: dynamicInitial,
                updated: dynamicUpdated
            },
            faces: faceSnapshots,
            invalid: {
                missingParents: captureError([]),
                onlyQuantiles: captureError([[-2, -1, 0, 1, 2]]),
                tooManyParents:
                    captureError([[-2, -1, 0, 1, 2], 0, 2, 3]),
                tooFewQuantiles: captureError([[-2, -1, 0, 1], 0, 2]),
                nonArrayQuantiles: captureError([7, 0, 2]),
                objectQuantile:
                    captureError([[-2, -1, {}, 1, 2], 0, 2]),
                objectAxis:
                    captureError([[-2, -1, 0, 1, 2], {}, 2]),
                objectWidth:
                    captureError([[-2, -1, 0, 1, 2], 0, {}]),
                nonArrayOutliers:
                    captureError([[-2, -1, 0, 1, 2, 3], 0, 2])
            },
            removal: {
                before: removalBefore,
                afterCurve: removalAfterCurve
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
