/*
 * Official JSXGraph 1.13.3 Circle/Point Tangent and Polar behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/tangent-polar-circle.mjs
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
            coordinates: vector(point.coords.usrCoords),
            constrained: point.isConstrained,
            draggable: point.isDraggable,
            fixed: point.visProp.fixed,
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
            name: line.name,
            elType: line.elType,
            type: line.type,
            elementClass: line.elementClass,
            stdform: vector(line.stdform),
            constrained: line.constrained,
            draggable: line.isDraggable,
            straightFirst: line.visProp.straightfirst,
            straightLast: line.visProp.straightlast,
            parents: [...line.parents],
            inherits: line.inherits.map((element) => element.id),
            glider: line.glider?.id ?? null,
            point1: snapshotPoint(line.point1),
            point2: snapshotPoint(line.point2),
            relations: relations(line)
        });
        const point = (board, coordinates, id) =>
            board.create("point", coordinates, {
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
        const derivedLine = (board, kind, parents, id) =>
            board.create(kind, parents, {
                id,
                name: "",
                withLabel: false,
                point1: {
                    id: `${id}Point1`,
                    name: "",
                    withLabel: false
                },
                point2: {
                    id: `${id}Point2`,
                    name: "",
                    withLabel: false
                }
            });
        const captureError = (action) => {
            try {
                const value = action();
                return {
                    threw: false,
                    id: value?.id ?? null,
                    elType: value?.elType ?? null
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
        const center = point(board, [1, 1], "center");
        const radiusPoint = point(board, [4, 1], "radiusPoint");
        const sourceCircle = circle(
            board,
            center,
            radiusPoint,
            "sourceCircle"
        );
        const offCircle = point(board, [5, 4], "offCircle");
        const tangentCircleFirst = derivedLine(
            board,
            "tangent",
            [sourceCircle, offCircle],
            "tangentCircleFirst"
        );
        const tangentPointFirst = derivedLine(
            board,
            "tangent",
            [offCircle, sourceCircle],
            "tangentPointFirst"
        );
        const polarAlias = derivedLine(
            board,
            "polar",
            [offCircle, sourceCircle],
            "polarAlias"
        );
        const polarLineCircleFirst = derivedLine(
            board,
            "polarline",
            [sourceCircle, offCircle],
            "polarLineCircleFirst"
        );
        const polarLinePointFirst = derivedLine(
            board,
            "polarline",
            [offCircle, sourceCircle],
            "polarLinePointFirst"
        );
        board.update();
        const initial = {
            circle: snapshotCircle(sourceCircle),
            point: snapshotPoint(offCircle),
            lines: [
                tangentCircleFirst,
                tangentPointFirst,
                polarAlias,
                polarLineCircleFirst,
                polarLinePointFirst
            ].map(snapshotLine),
            objectOrder: board.objectsList.map((element) => element.id)
        };

        center.setPositionDirectly(JXG.COORDS_BY_USER, [-2, 2]);
        radiusPoint.setPositionDirectly(JXG.COORDS_BY_USER, [2, 2]);
        offCircle.setPositionDirectly(JXG.COORDS_BY_USER, [2, -2]);
        board.update();
        const moved = {
            circle: snapshotCircle(sourceCircle),
            point: snapshotPoint(offCircle),
            lines: [
                tangentCircleFirst,
                tangentPointFirst,
                polarAlias,
                polarLineCircleFirst,
                polarLinePointFirst
            ].map(snapshotLine)
        };

        const geometryBoard = createBoard();
        const geometryCenter = point(
            geometryBoard,
            [0, 0],
            "geometryCenter"
        );
        const geometryRadius = point(
            geometryBoard,
            [3, 0],
            "geometryRadius"
        );
        const geometryCircle = circle(
            geometryBoard,
            geometryCenter,
            geometryRadius,
            "geometryCircle"
        );
        const onCirclePoint = point(
            geometryBoard,
            [0, 3],
            "onCirclePoint"
        );
        const offCirclePoint = point(
            geometryBoard,
            [4, 2],
            "offCirclePoint"
        );
        const centerPoint = point(
            geometryBoard,
            [0, 0],
            "centerPoint"
        );
        const onCircleLine = derivedLine(
            geometryBoard,
            "tangent",
            [onCirclePoint, geometryCircle],
            "onCircleLine"
        );
        const offCircleLine = derivedLine(
            geometryBoard,
            "tangent",
            [offCirclePoint, geometryCircle],
            "offCircleLine"
        );
        const centerLine = derivedLine(
            geometryBoard,
            "tangent",
            [centerPoint, geometryCircle],
            "centerLine"
        );
        geometryBoard.update();
        const geometry = {
            onCircle: snapshotLine(onCircleLine),
            offCircle: snapshotLine(offCircleLine),
            center: snapshotLine(centerLine)
        };

        const degenerateBoard = createBoard();
        const degenerateCenter = point(
            degenerateBoard,
            [2, -1],
            "degenerateCenter"
        );
        const degenerateRadius = point(
            degenerateBoard,
            [2, -1],
            "degenerateRadius"
        );
        const degenerateCircle = circle(
            degenerateBoard,
            degenerateCenter,
            degenerateRadius,
            "degenerateCircle"
        );
        const degeneratePoint = point(
            degenerateBoard,
            [4, 3],
            "degeneratePoint"
        );
        const degenerateLine = derivedLine(
            degenerateBoard,
            "tangent",
            [degenerateCircle, degeneratePoint],
            "degenerateLine"
        );
        degenerateBoard.update();
        const degenerate = {
            circle: snapshotCircle(degenerateCircle),
            line: snapshotLine(degenerateLine)
        };

        const directRemovalBoard = createBoard();
        const directCenter = point(
            directRemovalBoard,
            [0, 0],
            "directCenter"
        );
        const directRadius = point(
            directRemovalBoard,
            [2, 0],
            "directRadius"
        );
        const directCircle = circle(
            directRemovalBoard,
            directCenter,
            directRadius,
            "directCircle"
        );
        const directPoint = point(
            directRemovalBoard,
            [3, 2],
            "directPoint"
        );
        const directLine = derivedLine(
            directRemovalBoard,
            "tangent",
            [directCircle, directPoint],
            "directLine"
        );
        const directIds = [
            directLine.id,
            directLine.point1.id,
            directLine.point2.id
        ];
        directRemovalBoard.removeObject(directLine);
        const directRemoval = {
            presence: Object.fromEntries(
                directIds.map((id) => [
                    id,
                    Boolean(directRemovalBoard.objects[id])
                ])
            ),
            circle: relations(directCircle),
            point: relations(directPoint),
            objectOrder: directRemovalBoard.objectsList.map(
                (element) => element.id
            )
        };

        const parentRemoval = {};
        for (const parentKind of ["circle", "point"]) {
            const removalBoard = createBoard();
            const removalCenter = point(
                removalBoard,
                [0, 0],
                `${parentKind}RemovalCenter`
            );
            const removalRadius = point(
                removalBoard,
                [2, 0],
                `${parentKind}RemovalRadius`
            );
            const removalCircle = circle(
                removalBoard,
                removalCenter,
                removalRadius,
                `${parentKind}RemovalCircle`
            );
            const removalPoint = point(
                removalBoard,
                [3, 2],
                `${parentKind}RemovalPoint`
            );
            const removalLine = derivedLine(
                removalBoard,
                "tangent",
                [removalCircle, removalPoint],
                `${parentKind}RemovalLine`
            );
            removalBoard.removeObject(
                parentKind === "circle" ? removalCircle : removalPoint
            );
            parentRemoval[parentKind] = {
                presence: {
                    circle: Boolean(
                        removalBoard.objects[removalCircle.id]
                    ),
                    point: Boolean(removalBoard.objects[removalPoint.id]),
                    line: Boolean(removalBoard.objects[removalLine.id]),
                    point1: Boolean(
                        removalBoard.objects[removalLine.point1.id]
                    ),
                    point2: Boolean(
                        removalBoard.objects[removalLine.point2.id]
                    )
                },
                objectOrder: removalBoard.objectsList.map(
                    (element) => element.id
                )
            };
        }

        const invalidBoard = createBoard();
        const invalidCenter = point(
            invalidBoard,
            [0, 0],
            "invalidCenter"
        );
        const invalidRadius = point(
            invalidBoard,
            [2, 0],
            "invalidRadius"
        );
        const invalidCircle = circle(
            invalidBoard,
            invalidCenter,
            invalidRadius,
            "invalidCircle"
        );
        const invalidPoint = point(
            invalidBoard,
            [3, 1],
            "invalidPoint"
        );
        const otherPoint = point(
            invalidBoard,
            [-2, 1],
            "otherPoint"
        );
        const otherCircle = circle(
            invalidBoard,
            invalidCenter,
            otherPoint,
            "otherCircle"
        );
        const foreignBoard = createBoard();
        const foreignPoint = point(
            foreignBoard,
            [4, 2],
            "foreignPoint"
        );
        const beforeInvalid = invalidBoard.objectsList.map(
            (element) => element.id
        );
        const invalid = {
            circleCircle: captureError(() =>
                derivedLine(
                    invalidBoard,
                    "tangent",
                    [invalidCircle, otherCircle],
                    "invalidCircleCircle"
                )
            ),
            pointPoint: captureError(() =>
                derivedLine(
                    invalidBoard,
                    "polarline",
                    [invalidPoint, otherPoint],
                    "invalidPointPoint"
                )
            ),
            crossBoard: captureError(() =>
                derivedLine(
                    invalidBoard,
                    "tangent",
                    [invalidCircle, foreignPoint],
                    "crossBoard"
                )
            )
        };
        const afterInvalid = invalidBoard.objectsList.map(
            (element) => element.id
        );

        const duplicateBoard = createBoard();
        const duplicateCenter = point(
            duplicateBoard,
            [0, 0],
            "duplicateCenter"
        );
        const duplicateRadius = point(
            duplicateBoard,
            [2, 0],
            "duplicateRadius"
        );
        const duplicateCircle = circle(
            duplicateBoard,
            duplicateCenter,
            duplicateRadius,
            "duplicateCircle"
        );
        const duplicatePoint = point(
            duplicateBoard,
            [3, 1],
            "duplicatePoint"
        );
        point(duplicateBoard, [8, 8], "taken");
        const duplicateMainId = captureError(() =>
            derivedLine(
                duplicateBoard,
                "tangent",
                [duplicateCircle, duplicatePoint],
                "taken"
            )
        );
        point(duplicateBoard, [9, 9], "takenPoint1");
        const duplicateHelperId = captureError(() =>
            derivedLine(
                duplicateBoard,
                "tangent",
                [duplicateCircle, duplicatePoint],
                "taken"
            )
        );

        return {
            initial,
            moved,
            geometry,
            degenerate,
            directRemoval,
            parentRemoval,
            invalid: {
                ...invalid,
                before: beforeInvalid,
                after: afterInvalid
            },
            duplicateIds: {
                main: duplicateMainId,
                helper: duplicateHelperId,
                objectOrder: duplicateBoard.objectsList.map(
                    (element) => element.id
                )
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
