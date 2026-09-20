/*
 * Official JSXGraph 1.13.3 RiemannSum behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/riemannsum.mjs
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
        const coordinates = (curve) => {
            const length = curve.dataX.length;
            const indices = [...new Set([
                0,
                1,
                Math.floor(length / 2),
                Math.max(0, length - 2),
                Math.max(0, length - 1)
            ])].filter((index) => index < length);
            return indices.map((index) => ({
                index,
                x: classify(curve.dataX[index]),
                y: classify(curve.dataY[index])
            }));
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
            minX: classify(curve.minX()),
            maxX: classify(curve.maxX()),
            numberPoints: curve.numberPoints,
            dataLength: curve.dataX.length,
            value: classify(curve.Value()),
            hasValue: typeof curve.Value === "function",
            fillColor: curve.visProp.fillcolor,
            fillOpacity: curve.visProp.fillopacity,
            coordinates: coordinates(curve),
            relations: relations(curve)
        });
        const captureError = (parents) => {
            const board = createBoard();
            const before = board.objectsList.length;
            try {
                const value = board.create("riemannsum", parents, {
                    id: "candidate",
                    name: "",
                    withLabel: false
                });
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

        const quadratic = (x) => x * x + 1;
        const typeBoard = createBoard();
        const types = [
            "left",
            "right",
            "middle",
            "lower",
            "upper",
            "simpson",
            "trapezoidal",
            "unknown"
        ];
        const typeSnapshots = {};
        for (const type of types) {
            const curve = typeBoard.create(
                "riemannsum",
                [quadratic, 3, type, -1, 2],
                {
                    id: `type-${type}`,
                    name: "",
                    withLabel: false
                }
            );
            typeBoard.update();
            typeSnapshots[type] = snapshot(curve);
        }

        const betweenBoard = createBoard();
        const between = betweenBoard.create(
            "riemannsum",
            [
                [(x) => x * 0.5, quadratic],
                3,
                "lower",
                -1,
                2
            ],
            {
                id: "between",
                name: "",
                withLabel: false,
                fillColor: "#336699",
                fillOpacity: 0.45
            }
        );
        betweenBoard.update();

        const dynamicBoard = createBoard();
        const driver = dynamicBoard.create("point", [3.8, -1], {
            id: "A",
            name: "A",
            withLabel: false
        });
        let dynamicType = "left";
        let dynamicStart = -1;
        let dynamicEnd = 2;
        const dynamic = dynamicBoard.create(
            "riemannsum",
            [
                quadratic,
                "A.X()",
                () => dynamicType,
                () => dynamicStart,
                () => dynamicEnd
            ],
            {
                id: "dynamic",
                name: "",
                withLabel: false
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
        driver.setPositionDirectly(JXG.COORDS_BY_USER, [5.2, -1]);
        dynamicType = "right";
        dynamicStart = 0;
        dynamicEnd = 3;
        dynamicBoard.update();
        const dynamicUpdated = {
            curve: snapshot(dynamic),
            driver: relations(driver)
        };

        const defaultBoard = createBoard();
        const defaultInterval = defaultBoard.create(
            "riemannsum",
            [quadratic, 2, "left"],
            {id: "defaultInterval", name: "", withLabel: false}
        );
        defaultBoard.update();

        const numericTypeBoard = createBoard();
        const numericType = numericTypeBoard.create(
            "riemannsum",
            [quadratic, 2, 7, -1, 1],
            {id: "numericType", name: "", withLabel: false}
        );
        numericTypeBoard.update();

        const removalBoard = createBoard();
        const removalDriver = removalBoard.create("point", [3, 0], {
            id: "removalDriver",
            name: "",
            withLabel: false
        });
        const removable = removalBoard.create(
            "riemannsum",
            [quadratic, "removalDriver.X()", "left", -1, 2],
            {id: "removable", name: "", withLabel: false}
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
                removalBoard.objects.removalDriver === removalDriver,
            curvePresent: removalBoard.objects.removable === removable,
            driver: relations(removalDriver),
            objectOrder: removalBoard.objectsList.map(
                (element) => element.id
            )
        };

        return {
            version: JXG.version,
            types: typeSnapshots,
            between: snapshot(between),
            dynamic: {
                initial: dynamicInitial,
                updated: dynamicUpdated
            },
            defaultInterval: snapshot(defaultInterval),
            numericType: snapshot(numericType),
            invalid: {
                missingN: captureError([quadratic]),
                objectN: captureError([quadratic, {}, "left", -1, 1]),
                missingType: captureError([quadratic, 2]),
                objectType: captureError([quadratic, 2, {}, -1, 1]),
                numericFunction:
                    captureError([5, 2, "left", -1, 1]),
                emptyFunctionArray:
                    captureError([[], 2, "left", -1, 1]),
                oneFunctionArray:
                    captureError([[quadratic], 2, "left", -1, 1])
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
