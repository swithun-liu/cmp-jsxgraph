/*
 * Official JSXGraph 1.13.3 Line/Circle/Curve Normal behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/normal.mjs
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
            point: line.point ? snapshotPoint(line.point) : null,
            subs: Object.fromEntries(
                Object.entries(line.subs ?? {}).map(([key, value]) => [
                    key,
                    value?.id ?? null
                ])
            ),
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
        const normal = (board, parents, id, attributes = {}) =>
            board.create("normal", parents, {
                id,
                name: "",
                withLabel: false,
                point: {
                    id: `${id}Point`,
                    name: "",
                    withLabel: false
                },
                point1: {
                    id: `${id}Point1`,
                    name: "",
                    withLabel: false
                },
                point2: {
                    id: `${id}Point2`,
                    name: "",
                    withLabel: false
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
        const a = point(board, [-4, -2], "A");
        const b = point(board, [3, 2], "B");
        const sourceLine = board.create("line", [a, b], {
            id: "sourceLine",
            name: "",
            withLabel: false
        });
        const linePoint = point(board, [-1, 4], "linePoint");
        const lineNormal = normal(
            board,
            [sourceLine, linePoint],
            "lineNormal",
            {straightFirst: false, straightLast: true}
        );
        const reverseLineNormal = normal(
            board,
            [linePoint, sourceLine],
            "reverseLineNormal",
            {straightFirst: true, straightLast: false}
        );

        const center = point(board, [2, -1], "center");
        const radiusPoint = point(board, [5, -1], "radiusPoint");
        const sourceCircle = board.create(
            "circle",
            [center, radiusPoint],
            {id: "sourceCircle", name: "", withLabel: false}
        );
        const circlePoint = point(board, [4, 3], "circlePoint");
        const circleNormal = normal(
            board,
            [sourceCircle, circlePoint],
            "circleNormal"
        );
        const reverseCircleNormal = normal(
            board,
            [circlePoint, sourceCircle],
            "reverseCircleNormal"
        );

        const functionCurve = board.create(
            "functiongraph",
            [(x) => 0.5 * x * x - 2, -5, 5],
            {id: "functionCurve", name: "", withLabel: false}
        );
        const functionPoint = point(board, [-2, 3], "functionPoint");
        const functionNormal = normal(
            board,
            [functionCurve, functionPoint],
            "functionNormal"
        );

        const parametricCurve = board.create(
            "curve",
            [
                (t) => 2 * Math.cos(t) + 3,
                (t) => 1.5 * Math.sin(t) - 1,
                0,
                2 * Math.PI
            ],
            {id: "parametricCurve", name: "", withLabel: false}
        );
        const parametricPoint = point(
            board,
            [5.5, 1.25],
            "parametricPoint"
        );
        const parametricNormal = normal(
            board,
            [parametricPoint, parametricCurve],
            "parametricNormal"
        );

        const plotCurve = board.create(
            "curve",
            [[-4, -1, 2, 5], [-2, 2, -1, 3]],
            {id: "plotCurve", name: "", withLabel: false}
        );
        const plotPoint = point(board, [0.25, 2.5], "plotPoint");
        const plotNormal = normal(
            board,
            [plotCurve, plotPoint],
            "plotNormal",
            {straightFirst: false, straightLast: false}
        );
        board.update();

        const normalLines = [
            lineNormal,
            reverseLineNormal,
            circleNormal,
            reverseCircleNormal,
            functionNormal,
            parametricNormal,
            plotNormal
        ];
        const initial = {
            normals: normalLines.map(snapshotLine),
            objectOrder: board.objectsList.map((element) => element.id),
            ignoredPointIdsPresent: [
                "lineNormalPoint1",
                "lineNormalPoint2",
                "circleNormalPoint",
                "functionNormalPoint",
                "parametricNormalPoint",
                "plotNormalPoint"
            ].filter((id) => Boolean(board.objects[id]))
        };

        a.setPositionDirectly(JXG.COORDS_BY_USER, [-5, 3]);
        b.setPositionDirectly(JXG.COORDS_BY_USER, [2, -4]);
        linePoint.setPositionDirectly(JXG.COORDS_BY_USER, [4, 4]);
        center.setPositionDirectly(JXG.COORDS_BY_USER, [-2, 2]);
        radiusPoint.setPositionDirectly(JXG.COORDS_BY_USER, [2, 2]);
        circlePoint.setPositionDirectly(JXG.COORDS_BY_USER, [2, -2]);
        functionPoint.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [1.5, 4]
        );
        parametricPoint.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [0.5, -1.5]
        );
        plotPoint.setPositionDirectly(JXG.COORDS_BY_USER, [4, 4]);
        board.update();
        const moved = {
            normals: normalLines.map(snapshotLine)
        };

        const coordinateBoard = createBoard();
        const coordinateA = point(
            coordinateBoard,
            [-2, -1],
            "coordinateA"
        );
        const coordinateB = point(
            coordinateBoard,
            [3, 2],
            "coordinateB"
        );
        const coordinateLine = coordinateBoard.create(
            "line",
            [coordinateA, coordinateB],
            {id: "coordinateLine", name: "", withLabel: false}
        );
        const coordinate = {
            result: captureError(() =>
                normal(
                    coordinateBoard,
                    [coordinateLine, [1, 4]],
                    "coordinateNormal"
                )
            ),
            objectOrder: coordinateBoard.objectsList.map(
                (element) => element.id
            )
        };

        const removalCases = {};
        for (const kind of ["line", "circle", "curve"]) {
            for (const removedParent of ["object", "point", "normal"]) {
                const removalBoard = createBoard();
                const p = point(
                    removalBoard,
                    [3, 3],
                    `${kind}-${removedParent}-point`
                );
                let source;
                if (kind === "line") {
                    const p1 = point(
                        removalBoard,
                        [-2, 0],
                        `${kind}-${removedParent}-p1`
                    );
                    const p2 = point(
                        removalBoard,
                        [2, 1],
                        `${kind}-${removedParent}-p2`
                    );
                    source = removalBoard.create("line", [p1, p2], {
                        id: `${kind}-${removedParent}-source`,
                        name: "",
                        withLabel: false
                    });
                } else if (kind === "circle") {
                    const centerPoint = point(
                        removalBoard,
                        [0, 0],
                        `${kind}-${removedParent}-center`
                    );
                    const radialPoint = point(
                        removalBoard,
                        [2, 0],
                        `${kind}-${removedParent}-radius`
                    );
                    source = removalBoard.create(
                        "circle",
                        [centerPoint, radialPoint],
                        {
                            id: `${kind}-${removedParent}-source`,
                            name: "",
                            withLabel: false
                        }
                    );
                } else {
                    source = removalBoard.create(
                        "functiongraph",
                        [(x) => x * x, -4, 4],
                        {
                            id: `${kind}-${removedParent}-source`,
                            name: "",
                            withLabel: false
                        }
                    );
                }
                const output = normal(
                    removalBoard,
                    [source, p],
                    `${kind}-${removedParent}-normal`
                );
                const tracked = [
                    source,
                    p,
                    output,
                    output.point1,
                    output.point2
                ];
                if (output.point) {
                    tracked.push(output.point);
                }
                removalBoard.removeObject(
                    removedParent === "object"
                        ? source
                        : removedParent === "point"
                            ? p
                            : output
                );
                removalCases[`${kind}-${removedParent}`] = {
                    presence: presence(removalBoard, tracked),
                    objectOrder: removalBoard.objectsList.map(
                        (element) => element.id
                    )
                };
            }
        }

        const invalidBoard = createBoard();
        const invalidPointA = point(
            invalidBoard,
            [0, 0],
            "invalidPointA"
        );
        const invalidPointB = point(
            invalidBoard,
            [1, 1],
            "invalidPointB"
        );
        const invalid = {
            noParents: captureError(() =>
                invalidBoard.create("normal", [])
            ),
            onePoint: captureError(() =>
                invalidBoard.create("normal", [invalidPointA])
            ),
            pointPair: captureError(() =>
                invalidBoard.create(
                    "normal",
                    [invalidPointA, invalidPointB]
                )
            ),
            threeParents: captureError(() =>
                invalidBoard.create(
                    "normal",
                    [invalidPointA, invalidPointB, invalidPointA]
                )
            )
        };

        return {
            initial,
            moved,
            coordinate,
            removalCases,
            invalid
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
