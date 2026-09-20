/*
 * Official JSXGraph 1.13.3 Comb behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/comb.mjs
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
                boundingbox: [-8, 8, 8, -8],
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
            frequency: classify(curve.evalVisProp("frequency")),
            width: classify(curve.evalVisProp("width")),
            angle: classify(curve.evalVisProp("angle")),
            reverse: classify(curve.evalVisProp("reverse")),
            strokeWidth: curve.evalVisProp("strokewidth"),
            strokeColor: curve.evalVisProp("strokecolor"),
            fillColor: curve.evalVisProp("fillcolor"),
            relations: relations(curve)
        });
        const captureError = (parents, attributes = {}) => {
            const board = createBoard();
            const before = board.objectsList.length;
            try {
                const value = board.create("comb", parents, {
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

        const staticBoard = createBoard();
        const first = staticBoard.create("point", [-2, 1], {
            id: "first",
            name: "",
            withLabel: false
        });
        const second = staticBoard.create("point", [4, 1], {
            id: "second",
            name: "",
            withLabel: false
        });
        const regular = staticBoard.create("comb", [first, second], {
            id: "regular",
            name: "",
            withLabel: false,
            frequency: 2,
            width: 1,
            angle: Math.PI / 2
        });
        const reverse = staticBoard.create("comb", [first, second], {
            id: "reverse",
            name: "",
            withLabel: false,
            frequency: 4,
            width: 2,
            angle: Math.PI / 4,
            reverse: true
        });
        staticBoard.update();

        const dynamicBoard = createBoard();
        const dynamicFirst = dynamicBoard.create("point", [-3, -1], {
            id: "dynamicFirst",
            name: "",
            withLabel: false
        });
        const dynamicSecond = dynamicBoard.create("point", [3, -1], {
            id: "dynamicSecond",
            name: "",
            withLabel: false
        });
        const state = {
            frequency: 2,
            width: 1.5,
            angle: Math.PI / 2,
            reverse: false
        };
        const dynamic = dynamicBoard.create(
            "comb",
            [dynamicFirst, dynamicSecond],
            {
                id: "dynamic",
                name: "",
                withLabel: false,
                frequency: () => state.frequency,
                width: () => state.width,
                angle: () => state.angle,
                reverse: () => state.reverse
            }
        );
        dynamicBoard.update();
        const dynamicInitial = snapshot(dynamic);
        dynamicSecond.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [1, 3]
        );
        state.frequency = 1.5;
        state.width = 0.75;
        state.angle = Math.PI / 3;
        state.reverse = true;
        dynamicBoard.update();
        const dynamicUpdated = snapshot(dynamic);

        const coordinateBoard = createBoard();
        const coordinateComb = coordinateBoard.create(
            "comb",
            [[-2, 0], [2, 0]],
            {
                id: "coordinateComb",
                name: "",
                withLabel: false,
                frequency: 1.5,
                width: 0.5
            }
        );
        coordinateBoard.update();
        const coordinateBefore = {
            curve: snapshot(coordinateComb),
            objectOrder: coordinateBoard.objectsList.map(
                (element) => element.id
            ),
            objects: coordinateBoard.objectsList.map((element) => ({
                id: element.id,
                elType: element.elType,
                visible: element.evalVisProp("visible"),
                fixed: element.evalVisProp("fixed"),
                relations: relations(element)
            }))
        };
        coordinateBoard.removeObject(coordinateComb);
        const coordinateAfter = {
            objectOrder: coordinateBoard.objectsList.map(
                (element) => element.id
            )
        };

        const degenerateBoard = createBoard();
        const same = degenerateBoard.create("point", [1, 2], {
            id: "same",
            name: "",
            withLabel: false
        });
        const zeroLength = degenerateBoard.create(
            "comb",
            [same, same],
            {
                id: "zeroLength",
                name: "",
                withLabel: false
            }
        );
        degenerateBoard.update();

        return {
            version: JXG.version,
            defaults: captureError([
                [-1, 0],
                [1, 0]
            ]),
            static: {
                regular: snapshot(regular),
                reverse: snapshot(reverse),
                first: relations(first),
                second: relations(second)
            },
            dynamic: {
                initial: dynamicInitial,
                updated: dynamicUpdated
            },
            coordinateParents: {
                before: coordinateBefore,
                afterCurveRemoval: coordinateAfter
            },
            degenerate: {
                zeroLength: snapshot(zeroLength)
            },
            invalid: {
                missingParents: captureError([]),
                oneParent: captureError([[0, 0]]),
                threeParents:
                    captureError([[0, 0], [1, 0], [2, 0]]),
                invalidFirst: captureError([{}, [1, 0]]),
                invalidSecond: captureError([[0, 0], {}])
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
