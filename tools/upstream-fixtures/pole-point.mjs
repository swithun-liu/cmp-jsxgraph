/*
 * Official JSXGraph 1.13.3 PolePoint behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/pole-point.mjs
 */
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

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
            descendants: Object.keys(element.descendants),
            ancestors: Object.keys(element.ancestors)
        });
        const snapshotPoint = (point) => ({
            id: point.id,
            name: point.name,
            elType: point.elType,
            type: point.type,
            elementClass: point.elementClass,
            coordinates: vector(point.coords.usrCoords),
            constrained: point.isConstrained,
            draggable: point.isDraggable,
            fixed: point.visProp.fixed,
            dump: point.dump,
            relations: relations(point)
        });
        const snapshotCircle = (circle) => ({
            id: circle.id,
            stdform: vector(circle.stdform),
            quadraticform: circle.quadraticform.map(vector),
            relations: relations(circle)
        });
        const snapshotLine = (line) => ({
            id: line.id,
            stdform: vector(line.stdform),
            relations: relations(line)
        });
        const point = (board, coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false
            });
        const line = (board, first, second, id) =>
            board.create("line", [first, second], {
                id,
                name: "",
                withLabel: false
            });
        const circle = (board, center, radiusPoint, id) =>
            board.create("circle", [center, radiusPoint], {
                id,
                name: "",
                withLabel: false
            });
        const polePoint = (board, parents, id) =>
            board.create("polepoint", parents, {
                id,
                name: "",
                withLabel: false
            });

        const board = createBoard();
        const center = point(board, [1, 1], "center");
        const radiusPoint = point(board, [3, 1], "radiusPoint");
        const sourceCircle = circle(
            board,
            center,
            radiusPoint,
            "sourceCircle"
        );
        const linePoint1 = point(board, [-1, 4], "linePoint1");
        const linePoint2 = point(board, [4, -1], "linePoint2");
        const sourceLine = line(
            board,
            linePoint1,
            linePoint2,
            "sourceLine"
        );
        const circleFirst = polePoint(
            board,
            [sourceCircle, sourceLine],
            "circleFirst"
        );
        const lineFirst = polePoint(
            board,
            [sourceLine, sourceCircle],
            "lineFirst"
        );
        board.update();
        const initial = {
            circle: snapshotCircle(sourceCircle),
            line: snapshotLine(sourceLine),
            circleFirst: snapshotPoint(circleFirst),
            lineFirst: snapshotPoint(lineFirst),
            objectOrder: board.objectsList.map((element) => element.id)
        };

        center.setPositionDirectly(JXG.COORDS_BY_USER, [-2, 2]);
        radiusPoint.setPositionDirectly(JXG.COORDS_BY_USER, [1, 2]);
        linePoint1.setPositionDirectly(JXG.COORDS_BY_USER, [-3, -1]);
        linePoint2.setPositionDirectly(JXG.COORDS_BY_USER, [2, 4]);
        board.update();
        const moved = {
            circle: snapshotCircle(sourceCircle),
            line: snapshotLine(sourceLine),
            circleFirst: snapshotPoint(circleFirst),
            lineFirst: snapshotPoint(lineFirst)
        };

        const removalBoard = createBoard();
        const removalCenter = point(
            removalBoard,
            [0, 0],
            "removalCenter"
        );
        const removalRadius = point(
            removalBoard,
            [2, 0],
            "removalRadius"
        );
        const removalCircle = circle(
            removalBoard,
            removalCenter,
            removalRadius,
            "removalCircle"
        );
        const removalLinePoint1 = point(
            removalBoard,
            [-2, -2],
            "removalLinePoint1"
        );
        const removalLinePoint2 = point(
            removalBoard,
            [2, 2],
            "removalLinePoint2"
        );
        const removalLine = line(
            removalBoard,
            removalLinePoint1,
            removalLinePoint2,
            "removalLine"
        );
        const removalPole = polePoint(
            removalBoard,
            [removalCircle, removalLine],
            "removalPole"
        );
        removalBoard.removeObject(removalPole);
        const directRemoval = {
            presence: {
                circle: Boolean(removalBoard.objects.removalCircle),
                line: Boolean(removalBoard.objects.removalLine),
                pole: Boolean(removalBoard.objects.removalPole)
            },
            circle: relations(removalCircle),
            line: relations(removalLine)
        };

        const parentRemovalBoard = createBoard();
        const parentCenter = point(
            parentRemovalBoard,
            [0, 0],
            "parentCenter"
        );
        const parentRadius = point(
            parentRemovalBoard,
            [2, 0],
            "parentRadius"
        );
        const parentCircle = circle(
            parentRemovalBoard,
            parentCenter,
            parentRadius,
            "parentCircle"
        );
        const parentLinePoint1 = point(
            parentRemovalBoard,
            [-2, -2],
            "parentLinePoint1"
        );
        const parentLinePoint2 = point(
            parentRemovalBoard,
            [2, 2],
            "parentLinePoint2"
        );
        const parentLine = line(
            parentRemovalBoard,
            parentLinePoint1,
            parentLinePoint2,
            "parentLine"
        );
        const parentPole = polePoint(
            parentRemovalBoard,
            [parentCircle, parentLine],
            "parentPole"
        );
        parentRemovalBoard.removeObject(parentCircle);
        const parentRemoval = {
            presence: {
                circle: Boolean(parentRemovalBoard.objects.parentCircle),
                line: Boolean(parentRemovalBoard.objects.parentLine),
                pole: Boolean(parentRemovalBoard.objects.parentPole)
            },
            line: relations(parentLine),
            objectOrder: parentRemovalBoard.objectsList.map(
                (element) => element.id
            )
        };

        const degenerateBoard = createBoard();
        const degenerateCenter = point(
            degenerateBoard,
            [0, 0],
            "degenerateCenter"
        );
        const degenerateRadius = point(
            degenerateBoard,
            [2, 0],
            "degenerateRadius"
        );
        const degenerateCircle = circle(
            degenerateBoard,
            degenerateCenter,
            degenerateRadius,
            "degenerateCircle"
        );
        const idealLine = degenerateBoard.create(
            "line",
            [1, 0, 0],
            {id: "idealLine", name: "", withLabel: false}
        );
        const idealPole = polePoint(
            degenerateBoard,
            [degenerateCircle, idealLine],
            "idealPole"
        );
        degenerateBoard.update();
        const degenerate = {
            line: snapshotLine(idealLine),
            pole: snapshotPoint(idealPole)
        };

        const failures = {};
        const failureBoard = createBoard();
        const failureCenter = point(
            failureBoard,
            [0, 0],
            "failureCenter"
        );
        const failureRadius = point(
            failureBoard,
            [2, 0],
            "failureRadius"
        );
        const failureCircle = circle(
            failureBoard,
            failureCenter,
            failureRadius,
            "failureCircle"
        );
        const failurePoint1 = point(
            failureBoard,
            [-1, -1],
            "failurePoint1"
        );
        const failurePoint2 = point(
            failureBoard,
            [1, 1],
            "failurePoint2"
        );
        const failureLine = line(
            failureBoard,
            failurePoint1,
            failurePoint2,
            "failureLine"
        );
        try {
            polePoint(
                failureBoard,
                [failureCircle, failurePoint1],
                "invalidSecond"
            );
            failures.invalidSecond = "accepted";
        } catch (error) {
            failures.invalidSecond = String(error);
        }
        const firstDuplicate = polePoint(
            failureBoard,
            [failureCircle, failureLine],
            "duplicate"
        );
        const secondDuplicate = polePoint(
            failureBoard,
            [failureCircle, failureLine],
            "duplicate"
        );
        failures.duplicateId = {
            first: snapshotPoint(firstDuplicate),
            second: snapshotPoint(secondDuplicate),
            registryIsSecond:
                failureBoard.objects.duplicate === secondDuplicate,
            objectOrder: failureBoard.objectsList.map(
                (element) => element.id
            )
        };

        return {
            initial,
            moved,
            directRemoval,
            parentRemoval,
            degenerate,
            failures
        };
    });

    process.stdout.write(`${JSON.stringify(evidence, null, 2)}\n`);
} finally {
    await browser.close();
}
