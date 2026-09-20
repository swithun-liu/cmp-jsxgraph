/*
 * Official JSXGraph 1.13.3 parallel-construction behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/parallel-constructions.mjs
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
        const coordinates = (point) => vector(point.coords.usrCoords);
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            ancestors: Object.keys(element.ancestors)
        });
        const snapshotPoint = (point) => ({
            id: point.id,
            elType: point.elType,
            type: point.type,
            elementClass: point.elementClass,
            coords: coordinates(point),
            draggable: point.isDraggable,
            relations: relations(point)
        });
        const snapshotLine = (line) => ({
            id: line.id,
            elType: line.elType,
            type: line.type,
            elementClass: line.elementClass,
            stdform: vector(line.stdform),
            point1: snapshotPoint(line.point1),
            point2: snapshotPoint(line.point2),
            point: snapshotPoint(line.point),
            straightFirst: line.visProp.straightfirst,
            straightLast: line.visProp.straightlast,
            inherits: line.inherits.map((element) => element.id),
            relations: relations(line)
        });
        const point = (board, coords, id) =>
            board.create("point", coords, {
                id,
                name: "",
                withLabel: false
            });
        const line = (board, parents, id) =>
            board.create("line", parents, {
                id,
                name: "",
                withLabel: false
            });
        const parallelPoint = (board, parents, id) =>
            board.create("parallelpoint", parents, {
                id,
                name: "",
                withLabel: false
            });
        const parallel = (board, parents, id, attributes = {}) =>
            board.create("parallel", parents, {
                id,
                name: "",
                withLabel: false,
                ...attributes
            });

        const board = createBoard();
        const a = point(board, [-4, -1], "a");
        const b = point(board, [2, 3], "b");
        const c = point(board, [3, -3], "c");
        const baseLine = line(board, [a, b], "baseLine");
        const ppThree = parallelPoint(board, [a, b, c], "ppThree");
        const ppLinePoint = parallelPoint(
            board,
            [baseLine, c],
            "ppLinePoint"
        );
        const ppPointLine = parallelPoint(
            board,
            [c, baseLine],
            "ppPointLine"
        );
        const parallelThree = parallel(
            board,
            [a, b, c],
            "parallelThree",
            {straightFirst: false, straightLast: false}
        );
        const parallelLinePoint = parallel(
            board,
            [baseLine, c],
            "parallelLinePoint"
        );
        const parallelPointLine = parallel(
            board,
            [c, baseLine],
            "parallelPointLine"
        );

        const snapshot = () => ({
            points: {
                ppThree: snapshotPoint(ppThree),
                ppLinePoint: snapshotPoint(ppLinePoint),
                ppPointLine: snapshotPoint(ppPointLine)
            },
            lines: {
                parallelThree: snapshotLine(parallelThree),
                parallelLinePoint: snapshotLine(parallelLinePoint),
                parallelPointLine: snapshotLine(parallelPointLine)
            },
            sources: {
                a: relations(a),
                b: relations(b),
                c: relations(c),
                baseLine: relations(baseLine)
            }
        });

        board.update();
        const initial = snapshot();
        a.setPositionDirectly(JXG.COORDS_BY_USER, [-2, 2]);
        b.setPositionDirectly(JXG.COORDS_BY_USER, [4, 1]);
        c.setPositionDirectly(JXG.COORDS_BY_USER, [1, -4]);
        board.update();
        const moved = snapshot();

        const helperBoard = createBoard();
        const helperPoint = parallelPoint(
            helperBoard,
            [[-4, -1], [2, 3], [3, -3]]
        );
        const helperPointIds = [
            helperPoint.id,
            ...helperPoint.parents
        ];
        const helperPointInitial = {
            point: snapshotPoint(helperPoint),
            objects: helperPointIds.map((id) => ({
                id,
                present: Boolean(helperBoard.objects[id]),
                relations: relations(helperBoard.objects[id])
            }))
        };
        helperBoard.removeObject(helperPoint);
        const helperPointRemoval = Object.fromEntries(
            helperPointIds.map((id) => [
                id,
                Boolean(helperBoard.objects[id])
            ])
        );

        const lineRemovalBoard = createBoard();
        const first = point(lineRemovalBoard, [-3, -2], "first");
        const second = point(lineRemovalBoard, [2, 2], "second");
        const sourceLine = line(
            lineRemovalBoard,
            [first, second],
            "sourceLine"
        );
        const driver = point(lineRemovalBoard, [1, -3], "driver");
        const output = parallel(
            lineRemovalBoard,
            [sourceLine, driver],
            "output"
        );
        const outputPointId = output.point.id;
        const lineRemovalInitial = {
            line: snapshotLine(output),
            sourceLine: relations(sourceLine),
            driver: relations(driver),
            helper: relations(output.point)
        };
        lineRemovalBoard.removeObject(output);
        const lineRemoval = {
            output: Boolean(lineRemovalBoard.objects.output),
            helper: Boolean(lineRemovalBoard.objects[outputPointId]),
            sourceLine: Boolean(lineRemovalBoard.objects.sourceLine),
            driver: Boolean(lineRemovalBoard.objects.driver)
        };

        const failures = {};
        for (const [name, callback] of Object.entries({
            parallelPointMissingParent: () =>
                helperBoard.create("parallelpoint", [[0, 0], [1, 1]]),
            parallelMissingLine: () =>
                helperBoard.create("parallel", [[0, 0], [1, 1]])
        })) {
            try {
                callback();
                failures[name] = null;
            } catch (failure) {
                failures[name] = failure.message;
            }
        }

        const degenerateBoard = createBoard();
        const sameA = point(degenerateBoard, [1, 2], "sameA");
        const sameB = point(degenerateBoard, [1, 2], "sameB");
        const sameC = point(degenerateBoard, [-2, 3], "sameC");
        const degeneratePoint = parallelPoint(
            degenerateBoard,
            [sameA, sameB, sameC],
            "degeneratePoint"
        );
        const degenerateLine = parallel(
            degenerateBoard,
            [sameA, sameB, sameC],
            "degenerateLine"
        );
        degenerateBoard.update();

        return {
            version: JXG.version,
            initial,
            moved,
            helperPointInitial,
            helperPointRemoval,
            lineRemovalInitial,
            lineRemoval,
            failures,
            degenerate: {
                point: snapshotPoint(degeneratePoint),
                line: snapshotLine(degenerateLine)
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
