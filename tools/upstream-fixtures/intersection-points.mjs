/*
 * Official JSXGraph 1.13.3 Intersection and OtherIntersection behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/intersection-points.mjs
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
        const snapshotPoint = (point) => ({
            id: point.id,
            name: point.name,
            elType: point.elType,
            type: point.type,
            originalType: point._org_type,
            elementClass: point.elementClass,
            coords: vector(point.coords.usrCoords),
            real: point.isReal,
            draggable: point.isDraggable,
            fixed: point.evalVisProp("fixed"),
            alwaysIntersect: point.evalVisProp("alwaysintersect"),
            precision: point.evalVisProp("precision"),
            intersectionNumbers:
                point.intersectionNumbers === undefined
                    ? null
                    : point.intersectionNumbers.map((value) =>
                        typeof value === "function"
                            ? classify(value())
                            : classify(value)
                    ),
            getParents: point.getParents().map((value) =>
                typeof value === "function"
                    ? classify(value())
                    : classify(value)
            ),
            relations: relations(point)
        });
        const point = (board, coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false
            });
        const line = (board, parents, id, attributes = {}) =>
            board.create("line", parents, {
                id,
                name: "",
                withLabel: false,
                ...attributes
            });
        const segment = (board, parents, id) =>
            board.create("segment", parents, {
                id,
                name: "",
                withLabel: false
            });
        const circle = (board, parents, id) =>
            board.create("circle", parents, {
                id,
                name: "",
                withLabel: false
            });
        const intersection = (
            board,
            parents,
            id,
            attributes = {}
        ) =>
            board.create("intersection", parents, {
                id,
                name: "",
                withLabel: false,
                ...attributes
            });
        const otherIntersection = (
            board,
            parents,
            id,
            attributes = {}
        ) =>
            board.create("otherintersection", parents, {
                id,
                name: "",
                withLabel: false,
                ...attributes
            });

        const board = createBoard();
        const horizontalStart = point(board, [-6, 0], "horizontalStart");
        const horizontalEnd = point(board, [6, 0], "horizontalEnd");
        const verticalStart = point(board, [1, -5], "verticalStart");
        const verticalEnd = point(board, [1, 5], "verticalEnd");
        const horizontal = line(
            board,
            [horizontalStart, horizontalEnd],
            "horizontal"
        );
        const vertical = line(
            board,
            [verticalStart, verticalEnd],
            "vertical"
        );
        const center = point(board, [0, 0], "center");
        const firstCircle = circle(board, [center, 3], "firstCircle");
        const secondCenter = point(board, [3, 0], "secondCenter");
        const secondCircle = circle(
            board,
            [secondCenter, 2.5],
            "secondCircle"
        );
        const finiteStart = point(board, [-1, 2], "finiteStart");
        const finiteEnd = point(board, [1, 2], "finiteEnd");
        const finite = segment(board, [finiteStart, finiteEnd], "finite");

        let dynamicIndex = 0;
        const points = {
            lineLine: intersection(
                board,
                [horizontal, vertical],
                "lineLine"
            ),
            lineCircle0: intersection(
                board,
                [horizontal, firstCircle, 0],
                "lineCircle0"
            ),
            circleLine1: intersection(
                board,
                [firstCircle, horizontal, 1],
                "circleLine1"
            ),
            dynamicIndex: intersection(
                board,
                [firstCircle, horizontal, () => dynamicIndex],
                "dynamicIndex"
            ),
            circleCircle0: intersection(
                board,
                [firstCircle, secondCircle, 0],
                "circleCircle0"
            ),
            circleCircle1: intersection(
                board,
                [firstCircle, secondCircle, 1],
                "circleCircle1"
            ),
            otherLineCircle: null,
            otherCircleCircle: null,
            finiteDefault: intersection(
                board,
                [finite, firstCircle, 0],
                "finiteDefault"
            ),
            finiteClipped: intersection(
                board,
                [finite, firstCircle, 0],
                "finiteClipped",
                {alwaysIntersect: false}
            )
        };
        points.otherLineCircle = otherIntersection(
            board,
            [horizontal, firstCircle, points.lineCircle0],
            "otherLineCircle"
        );
        points.otherCircleCircle = otherIntersection(
            board,
            [
                firstCircle,
                secondCircle,
                [points.circleCircle0]
            ],
            "otherCircleCircle"
        );
        board.update();
        const initial = Object.fromEntries(
            Object.entries(points).map(([name, output]) => [
                name,
                snapshotPoint(output)
            ])
        );

        dynamicIndex = NaN;
        board.update();
        const dynamicNaN = snapshotPoint(points.dynamicIndex);

        dynamicIndex = 1;
        secondCenter.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [8, 0]
        );
        verticalStart.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-5, -5]
        );
        verticalEnd.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-5, 5]
        );
        board.update();
        const moved = Object.fromEntries(
            Object.entries(points).map(([name, output]) => [
                name,
                snapshotPoint(output)
            ])
        );

        const parallelBoard = createBoard();
        const parallelA = point(parallelBoard, [-2, 0], "parallelA");
        const parallelB = point(parallelBoard, [2, 0], "parallelB");
        const parallelC = point(parallelBoard, [-2, 2], "parallelC");
        const parallelD = point(parallelBoard, [2, 2], "parallelD");
        const parallelFirst = line(
            parallelBoard,
            [parallelA, parallelB],
            "parallelFirst"
        );
        const parallelSecond = line(
            parallelBoard,
            [parallelC, parallelD],
            "parallelSecond"
        );
        const parallelOutput = intersection(
            parallelBoard,
            [parallelFirst, parallelSecond],
            "parallelOutput"
        );
        parallelBoard.update();

        const removalBoard = createBoard();
        const removalA = point(removalBoard, [-2, 0], "removalA");
        const removalB = point(removalBoard, [2, 0], "removalB");
        const removalC = point(removalBoard, [0, -2], "removalC");
        const removalD = point(removalBoard, [0, 2], "removalD");
        const removalFirst = line(
            removalBoard,
            [removalA, removalB],
            "removalFirst"
        );
        const removalSecond = line(
            removalBoard,
            [removalC, removalD],
            "removalSecond"
        );
        const removalOutput = intersection(
            removalBoard,
            [removalFirst, removalSecond],
            "removalOutput"
        );
        const removalBefore = {
            output: snapshotPoint(removalOutput),
            first: relations(removalFirst),
            second: relations(removalSecond)
        };
        removalBoard.removeObject(removalOutput);
        const removalAfter = {
            output: Boolean(removalBoard.objects.removalOutput),
            first: relations(removalFirst),
            second: relations(removalSecond)
        };

        const failures = {};
        for (const [name, callback] of Object.entries({
            missingParent: () =>
                board.create("intersection", [horizontal]),
            unknownParent: () =>
                board.create("intersection", ["missing", firstCircle]),
            otherMissingPoint: () =>
                board.create(
                    "otherintersection",
                    [horizontal, firstCircle]
                ),
            otherTwoLines: () =>
                board.create(
                    "otherintersection",
                    [horizontal, vertical, points.lineLine]
                ),
            otherInvalidExcludedElement: () =>
                board.create(
                    "otherintersection",
                    [horizontal, firstCircle, vertical]
                )
        })) {
            try {
                callback();
                failures[name] = null;
            } catch (failure) {
                failures[name] = failure.message;
            }
        }

        return {
            version: JXG.version,
            initial,
            dynamicNaN,
            moved,
            parallel: snapshotPoint(parallelOutput),
            removal: {
                before: removalBefore,
                after: removalAfter
            },
            failures
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
