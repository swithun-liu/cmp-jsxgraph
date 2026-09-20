/*
 * Official JSXGraph 1.13.3 Arrow and ArrowParallel behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/line-arrows.mjs
 */
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

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
            return value;
        };
        const vector = (values) => values.map(classify);
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            ancestors: Object.keys(element.ancestors)
        });
        const arrowValue = (value, line) => {
            if (!value || typeof value !== "object") {
                return value;
            }
            return {
                type:
                    typeof value.type === "function"
                        ? value.type.call(line)
                        : value.type,
                size:
                    typeof value.size === "function"
                        ? value.size.call(line)
                        : value.size,
                highlightSize:
                    typeof value.highlightsize === "function"
                        ? value.highlightsize.call(line)
                        : value.highlightsize
            };
        };
        const snapshotLine = (line) => ({
            id: line.id,
            elType: line.elType,
            type: line.type,
            elementClass: line.elementClass,
            stdform: vector(line.stdform),
            point1: {
                id: line.point1.id,
                coords: vector(line.point1.coords.usrCoords)
            },
            point2: {
                id: line.point2.id,
                coords: vector(line.point2.coords.usrCoords)
            },
            straightFirst: line.evalVisProp("straightfirst"),
            straightLast: line.evalVisProp("straightlast"),
            firstArrow: arrowValue(line.evalVisProp("firstarrow"), line),
            lastArrow: arrowValue(line.evalVisProp("lastarrow"), line),
            relations: relations(line)
        });
        const point = (board, coords, id) =>
            board.create("point", coords, {
                id,
                name: "",
                withLabel: false
            });

        const board = createBoard();
        const a = point(board, [-5, 2], "a");
        const b = point(board, [-1, 2], "b");
        const c = point(board, [2, -2], "c");
        const defaultArrow = board.create("arrow", [a, b], {
            id: "defaultArrow",
            name: "",
            withLabel: false
        });
        const noHeadArrow = board.create("arrow", [a, c], {
            id: "noHeadArrow",
            name: "",
            withLabel: false,
            lastArrow: false
        });
        const doubleArrow = board.create("arrow", [b, c], {
            id: "doubleArrow",
            name: "",
            withLabel: false,
            firstArrow: {type: 2, size: 4, highlightSize: 5},
            lastArrow: {type: 3, size: 7, highlightSize: 8}
        });
        const arrowTypes = [];
        for (let type = 1; type <= 7; type += 1) {
            const y = 5 - type;
            const arrow = board.create(
                "arrow",
                [[-7, y], [-4, y]],
                {
                    id: `arrowType${type}`,
                    name: "",
                    withLabel: false,
                    lastArrow: {type, size: 6}
                }
            );
            arrowTypes.push(snapshotLine(arrow));
        }

        const baseLine = board.create("line", [a, b], {
            id: "baseLine",
            name: "",
            withLabel: false
        });
        const parallelThree = board.create(
            "arrowparallel",
            [a, b, c],
            {
                id: "parallelThree",
                name: "",
                withLabel: false,
                lastArrow: false
            }
        );
        const parallelLinePoint = board.create(
            "arrowparallel",
            [baseLine, c],
            {
                id: "parallelLinePoint",
                name: "",
                withLabel: false,
                firstArrow: {type: 2, size: 5}
            }
        );

        board.update();
        const initial = {
            defaultArrow: snapshotLine(defaultArrow),
            noHeadArrow: snapshotLine(noHeadArrow),
            doubleArrow: snapshotLine(doubleArrow),
            parallelThree: snapshotLine(parallelThree),
            parallelLinePoint: snapshotLine(parallelLinePoint),
            sources: {
                a: relations(a),
                b: relations(b),
                c: relations(c),
                baseLine: relations(baseLine)
            }
        };

        a.setPositionDirectly(JXG.COORDS_BY_USER, [-4, -1]);
        b.setPositionDirectly(JXG.COORDS_BY_USER, [0, 3]);
        c.setPositionDirectly(JXG.COORDS_BY_USER, [3, -3]);
        board.update();
        const moved = {
            defaultArrow: snapshotLine(defaultArrow),
            parallelThree: snapshotLine(parallelThree),
            parallelLinePoint: snapshotLine(parallelLinePoint)
        };

        const helperId = parallelLinePoint.point.id;
        board.removeObject(parallelLinePoint);
        const removal = {
            output: Boolean(board.objects.parallelLinePoint),
            helper: Boolean(board.objects[helperId]),
            baseLine: Boolean(board.objects.baseLine),
            throughPoint: Boolean(board.objects.c)
        };

        const failures = {};
        for (const [name, callback] of Object.entries({
            arrowMissingParent: () => board.create("arrow", [a])
        })) {
            try {
                callback();
                failures[name] = null;
            } catch (failure) {
                failures[name] = failure.message;
            }
        }
        let twoPointArrowParallel;
        try {
            const output = board.create("arrowparallel", [a, b], {
                id: "twoPointArrowParallel",
                name: "",
                withLabel: false
            });
            const beforeUpdate = snapshotLine(output);
            board.update();
            twoPointArrowParallel = {
                failure: null,
                beforeUpdate,
                afterUpdate: snapshotLine(output),
                helper: {
                    id: output.point.id,
                    coords: vector(output.point.coords.usrCoords),
                    stdform: vector(output.point.stdform)
                }
            };
        } catch (failure) {
            twoPointArrowParallel = {
                failure: failure.message
            };
        }

        return {
            version: JXG.version,
            initial,
            moved,
            arrowTypes,
            removal,
            twoPointArrowParallel,
            failures
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
