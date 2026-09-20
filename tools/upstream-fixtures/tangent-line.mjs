/*
 * Official JSXGraph 1.13.3 Line/Point Tangent behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/tangent-line.mjs
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
            relations: relations(point)
        });
        const snapshotLine = (line) => ({
            id: line.id,
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
        const line = (board, first, second, id, attributes = {}) =>
            board.create("line", [first, second], {
                id,
                name: "",
                withLabel: false,
                ...attributes
            });
        const tangent = (board, kind, parents, id, attributes = {}) =>
            board.create(kind, parents, {
                id,
                name: "",
                withLabel: false,
                point1: {
                    id: `${id}IgnoredPoint1`,
                    name: "ignored-point-1"
                },
                point2: {
                    id: `${id}IgnoredPoint2`,
                    name: "ignored-point-2"
                },
                ...attributes
            });
        const presence = (board, elements) =>
            Object.fromEntries(
                elements.map((element) => [
                    element.id,
                    board.objects[element.id] === element
                ])
            );
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
        const a = point(board, [-3, -1], "A");
        const b = point(board, [4, 2], "B");
        const source = line(board, a, b, "source");
        const p = point(board, [1, 5], "P");
        const lineFirst = tangent(
            board,
            "tangent",
            [source, p],
            "lineFirst",
            {straightFirst: false, straightLast: true}
        );
        const pointFirst = tangent(
            board,
            "tangent",
            [p, source],
            "pointFirst",
            {straightFirst: true, straightLast: false}
        );
        const polarAlias = tangent(
            board,
            "polar",
            [p, source],
            "polarAlias"
        );
        board.update();
        const initial = {
            source: snapshotLine(source),
            point: snapshotPoint(p),
            tangents: [lineFirst, pointFirst, polarAlias].map(snapshotLine),
            ignoredPointIdsPresent: [
                "lineFirstIgnoredPoint1",
                "lineFirstIgnoredPoint2",
                "pointFirstIgnoredPoint1",
                "pointFirstIgnoredPoint2",
                "polarAliasIgnoredPoint1",
                "polarAliasIgnoredPoint2"
            ].filter((id) => Boolean(board.objects[id])),
            objectOrder: board.objectsList.map((element) => element.id)
        };

        a.setPositionDirectly(JXG.COORDS_BY_USER, [-5, 3]);
        b.setPositionDirectly(JXG.COORDS_BY_USER, [2, -4]);
        p.setPositionDirectly(JXG.COORDS_BY_USER, [7, 1]);
        board.update();
        const moved = {
            source: snapshotLine(source),
            point: snapshotPoint(p),
            tangents: [lineFirst, pointFirst, polarAlias].map(snapshotLine)
        };

        const sourceRemovalBoard = createBoard();
        const sourceRemovalA = point(
            sourceRemovalBoard,
            [-2, 0],
            "sourceRemovalA"
        );
        const sourceRemovalB = point(
            sourceRemovalBoard,
            [3, 1],
            "sourceRemovalB"
        );
        const removedSource = line(
            sourceRemovalBoard,
            sourceRemovalA,
            sourceRemovalB,
            "removedSource"
        );
        const retainedPoint = point(
            sourceRemovalBoard,
            [0, 4],
            "retainedPoint"
        );
        const retainedTangent = tangent(
            sourceRemovalBoard,
            "tangent",
            [removedSource, retainedPoint],
            "retainedTangent"
        );
        sourceRemovalBoard.removeObject(removedSource);
        const sourceRemoval = {
            presence: presence(
                sourceRemovalBoard,
                [
                    sourceRemovalA,
                    sourceRemovalB,
                    removedSource,
                    retainedPoint,
                    retainedTangent
                ]
            ),
            firstEndpoint: relations(sourceRemovalA),
            secondEndpoint: relations(sourceRemovalB),
            point: relations(retainedPoint),
            tangent: snapshotLine(retainedTangent),
            objectOrder: sourceRemovalBoard.objectsList.map(
                (element) => element.id
            )
        };

        const pointRemovalBoard = createBoard();
        const pointRemovalA = point(
            pointRemovalBoard,
            [-2, 0],
            "pointRemovalA"
        );
        const pointRemovalB = point(
            pointRemovalBoard,
            [3, 1],
            "pointRemovalB"
        );
        const retainedSource = line(
            pointRemovalBoard,
            pointRemovalA,
            pointRemovalB,
            "retainedSource"
        );
        const removedPoint = point(
            pointRemovalBoard,
            [0, 4],
            "removedPoint"
        );
        const pointRetainedTangent = tangent(
            pointRemovalBoard,
            "tangent",
            [retainedSource, removedPoint],
            "pointRetainedTangent"
        );
        pointRemovalBoard.removeObject(removedPoint);
        const pointRemoval = {
            presence: presence(
                pointRemovalBoard,
                [
                    pointRemovalA,
                    pointRemovalB,
                    retainedSource,
                    removedPoint,
                    pointRetainedTangent
                ]
            ),
            firstEndpoint: relations(pointRemovalA),
            secondEndpoint: relations(pointRemovalB),
            source: snapshotLine(retainedSource),
            tangent: snapshotLine(pointRetainedTangent),
            objectOrder: pointRemovalBoard.objectsList.map(
                (element) => element.id
            )
        };

        const endpointRemovalBoard = createBoard();
        const removedEndpoint = point(
            endpointRemovalBoard,
            [-2, 0],
            "removedEndpoint"
        );
        const retainedEndpoint = point(
            endpointRemovalBoard,
            [3, 1],
            "retainedEndpoint"
        );
        const endpointSource = line(
            endpointRemovalBoard,
            removedEndpoint,
            retainedEndpoint,
            "endpointSource"
        );
        const endpointPoint = point(
            endpointRemovalBoard,
            [0, 4],
            "endpointPoint"
        );
        const endpointTangent = tangent(
            endpointRemovalBoard,
            "tangent",
            [endpointSource, endpointPoint],
            "endpointTangent"
        );
        endpointRemovalBoard.removeObject(removedEndpoint);
        const endpointRemoval = {
            presence: presence(
                endpointRemovalBoard,
                [
                    removedEndpoint,
                    retainedEndpoint,
                    endpointSource,
                    endpointPoint,
                    endpointTangent
                ]
            ),
            retainedEndpoint: relations(retainedEndpoint),
            point: relations(endpointPoint),
            objectOrder: endpointRemovalBoard.objectsList.map(
                (element) => element.id
            )
        };

        const directRemovalBoard = createBoard();
        const directA = point(directRemovalBoard, [-2, 0], "directA");
        const directB = point(directRemovalBoard, [3, 1], "directB");
        const directSource = line(
            directRemovalBoard,
            directA,
            directB,
            "directSource"
        );
        const directPoint = point(
            directRemovalBoard,
            [0, 4],
            "directPoint"
        );
        const directTangent = tangent(
            directRemovalBoard,
            "tangent",
            [directSource, directPoint],
            "directTangent"
        );
        directRemovalBoard.removeObject(directTangent);
        const directRemoval = {
            presence: presence(
                directRemovalBoard,
                [
                    directA,
                    directB,
                    directSource,
                    directPoint,
                    directTangent
                ]
            ),
            firstEndpoint: relations(directA),
            secondEndpoint: relations(directB),
            source: snapshotLine(directSource),
            point: relations(directPoint),
            objectOrder: directRemovalBoard.objectsList.map(
                (element) => element.id
            )
        };

        const degenerateBoard = createBoard();
        const degenerateA = point(
            degenerateBoard,
            [2, -1],
            "degenerateA"
        );
        const degenerateB = point(
            degenerateBoard,
            [2, -1],
            "degenerateB"
        );
        const degenerateSource = line(
            degenerateBoard,
            degenerateA,
            degenerateB,
            "degenerateSource"
        );
        const degeneratePoint = point(
            degenerateBoard,
            [8, 8],
            "degeneratePoint"
        );
        const degenerateTangent = tangent(
            degenerateBoard,
            "tangent",
            [degeneratePoint, degenerateSource],
            "degenerateTangent"
        );
        degenerateBoard.update();

        const invalidBoard = createBoard();
        const invalidA = point(invalidBoard, [0, 0], "invalidA");
        const invalidB = point(invalidBoard, [2, 0], "invalidB");
        const invalidLine = line(
            invalidBoard,
            invalidA,
            invalidB,
            "invalidLine"
        );
        const invalidPoint = point(
            invalidBoard,
            [1, 1],
            "invalidPoint"
        );
        const otherBoard = createBoard();
        const foreignA = point(otherBoard, [0, 0], "foreignA");
        const foreignB = point(otherBoard, [0, 2], "foreignB");
        const foreignLine = line(
            otherBoard,
            foreignA,
            foreignB,
            "foreignLine"
        );
        const foreignPoint = point(otherBoard, [2, 2], "foreignPoint");
        const invalid = {
            polarLineWithLine: captureError(
                () => tangent(
                    invalidBoard,
                    "polarline",
                    [invalidLine, invalidPoint],
                    "invalidPolarLine"
                )
            ),
            crossBoardLine: captureError(
                () => tangent(
                    invalidBoard,
                    "tangent",
                    [foreignLine, invalidPoint],
                    "crossBoardLine"
                )
            ),
            crossBoardPoint: captureError(
                () => tangent(
                    invalidBoard,
                    "tangent",
                    [invalidLine, foreignPoint],
                    "crossBoardPoint"
                )
            )
        };

        return {
            version: JXG.version,
            initial,
            moved,
            sourceRemoval,
            pointRemoval,
            endpointRemoval,
            directRemoval,
            degenerate: {
                source: snapshotLine(degenerateSource),
                point: snapshotPoint(degeneratePoint),
                tangent: snapshotLine(degenerateTangent)
            },
            invalid
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
