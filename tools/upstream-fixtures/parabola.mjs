/*
 * Official JSXGraph 1.13.3 parabola creator behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/parabola.mjs
 */
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

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
            if (value === undefined) {
                return "undefined";
            }
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (value === Number.POSITIVE_INFINITY) {
                return "Infinity";
            }
            if (value === Number.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
            if (Object.is(value, -0)) {
                return "-0";
            }
            return value;
        };
        const vector = (values) => Array.from(values, classify);
        const matrix = (rows) => rows.map(vector);
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            descendants: Object.keys(element.descendants),
            ancestors: Object.keys(element.ancestors)
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
        const snapshotPoint = (element) => ({
            id: element.id,
            name: element.name,
            elType: element.elType,
            type: element.type,
            elementClass: element.elementClass,
            coordinates: vector(element.coords.usrCoords),
            constrained: element.isConstrained,
            draggable: element.isDraggable,
            fixed: element.visProp.fixed,
            visible: element.visProp.visible,
            needsRegularUpdate: element.needsRegularUpdate,
            dump: element.dump,
            relations: relations(element)
        });
        const snapshotLine = (element) => ({
            id: element.id,
            name: element.name,
            elType: element.elType,
            type: element.type,
            elementClass: element.elementClass,
            point1: element.point1.id,
            point2: element.point2.id,
            stdform: vector(element.stdform),
            fixed: element.visProp.fixed,
            visible: element.visProp.visible,
            needsRegularUpdate: element.needsRegularUpdate,
            inherits: element.inherits.map((item) => item.id),
            relations: relations(element)
        });
        const sampleAt = (curve, phi) => ({
            phi: classify(phi),
            x: classify(curve.X(phi)),
            y: classify(curve.Y(phi))
        });
        const snapshotParabola = (parabola) => ({
            id: parabola.id,
            name: parabola.name,
            elType: parabola.elType,
            type: parabola.type,
            elementClass: parabola.elementClass,
            curveType: parabola.curveType,
            minX: classify(parabola.minX()),
            maxX: classify(parabola.maxX()),
            numberPoints: parabola.numberPoints,
            samples: [
                sampleAt(parabola, parabola.minX()),
                sampleAt(parabola, -Math.PI * 0.5),
                sampleAt(parabola, 0),
                sampleAt(parabola, Math.PI * 0.5),
                sampleAt(parabola, parabola.maxX())
            ],
            center: snapshotPoint(parabola.center),
            midpoint: parabola.midpoint.id,
            quadraticForm: matrix(parabola.quadraticform),
            draggable: parabola.isDraggable,
            fixed: parabola.visProp.fixed,
            needsRegularUpdate: parabola.needsRegularUpdate,
            dump: parabola.dump,
            inherits: parabola.inherits.map((element) => element.id),
            subs: Object.fromEntries(
                Object.entries(parabola.subs).map(([key, value]) => [
                    key,
                    value.id
                ])
            ),
            relations: relations(parabola)
        });
        const objectOrder = (board) =>
            board.objectsList.map((element) => element.id);
        const presence = (board, ids) =>
            Object.fromEntries(
                ids.map((id) => [id, Boolean(board.objects[id])])
            );
        const captureError = (action) => {
            try {
                const value = action();
                return {
                    threw: false,
                    id: value?.id ?? null,
                    elType: value?.elType ?? null,
                    type: value?.type ?? null
                };
            } catch (error) {
                return {
                    threw: true,
                    name: error?.name ?? null,
                    message: String(error?.message ?? error)
                };
            }
        };

        const pointBoard = createBoard();
        const directrixPoint1 = point(
            pointBoard,
            [-1, 4],
            "directrixPoint1"
        );
        const directrixPoint2 = point(
            pointBoard,
            [-1, -4],
            "directrixPoint2"
        );
        const directrix = line(
            pointBoard,
            directrixPoint1,
            directrixPoint2,
            "directrix"
        );
        const focus = point(pointBoard, [1, 1], "focus");
        const pointParabola = pointBoard.create(
            "parabola",
            [focus, directrix],
            {
                id: "pointParabola",
                name: "point-parabola",
                withLabel: false,
                fixed: false,
                needsRegularUpdate: false,
                strokeColor: "#123456",
                strokeWidth: 4
            }
        );
        pointBoard.update();
        const pointInitial = {
            output: snapshotParabola(pointParabola),
            sources: {
                focus: relations(focus),
                directrix: relations(directrix),
                directrixPoint1: relations(directrixPoint1),
                directrixPoint2: relations(directrixPoint2)
            },
            objectOrder: objectOrder(pointBoard)
        };
        focus.setPositionDirectly(JXG.COORDS_BY_USER, [3, 2]);
        directrixPoint1.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-2, 3]
        );
        directrixPoint2.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [0, -3]
        );
        pointBoard.update();
        const pointMoved = {
            output: snapshotParabola(pointParabola),
            directrix: snapshotLine(directrix)
        };

        const domainBoard = createBoard();
        const domainFocus = point(domainBoard, [2, 1], "domainFocus");
        const domainDirectrix = line(
            domainBoard,
            point(domainBoard, [-1, 3], "domainLine1"),
            point(domainBoard, [-1, -3], "domainLine2"),
            "domainDirectrix"
        );
        const customDomain = snapshotParabola(
            domainBoard.create(
                "parabola",
                [
                    domainFocus,
                    domainDirectrix,
                    -Math.PI * 0.5,
                    Math.PI
                ],
                {
                    id: "customDomain",
                    name: "",
                    withLabel: false
                }
            )
        );

        const referenceBoard = createBoard();
        const referenceFocus = point(
            referenceBoard,
            [3, 1],
            "referenceFocus"
        );
        const referenceDirectrix = line(
            referenceBoard,
            point(referenceBoard, [0, 3], "referenceLine1"),
            point(referenceBoard, [0, -3], "referenceLine2"),
            "referenceDirectrix"
        );
        const referenceForms = {
            stringFocus: snapshotParabola(
                referenceBoard.create(
                    "parabola",
                    ["referenceFocus", referenceDirectrix],
                    {
                        id: "stringFocusParabola",
                        name: "",
                        withLabel: false
                    }
                )
            ),
            functionFocus: snapshotParabola(
                referenceBoard.create(
                    "parabola",
                    [() => referenceFocus, referenceDirectrix],
                    {
                        id: "functionFocusParabola",
                        name: "",
                        withLabel: false
                    }
                )
            )
        };

        const coordinateBoard = createBoard();
        const coordinateBefore = objectOrder(coordinateBoard);
        const coordinateParabola = coordinateBoard.create(
            "parabola",
            [[3.25, 0], [[0.25, 1], [0.25, 0]]],
            {
                id: "coordinateParabola",
                name: "",
                withLabel: false,
                foci: {
                    fixed: true,
                    visible: true
                },
                center: {
                    fixed: true,
                    visible: true
                },
                line: {
                    fixed: true,
                    visible: true
                }
            }
        );
        coordinateBoard.update();
        const coordinateAdded = objectOrder(coordinateBoard).slice(
            coordinateBefore.length
        );
        const coordinateInitial = {
            output: snapshotParabola(coordinateParabola),
            addedObjects: coordinateAdded,
            objects: coordinateAdded.map((id) => {
                const element = coordinateBoard.objects[id];
                return {
                    id,
                    elType: element.elType,
                    point:
                        element.elementClass === JXG.OBJECT_CLASS_POINT
                            ? snapshotPoint(element)
                            : null,
                    line:
                        element.elementClass === JXG.OBJECT_CLASS_LINE
                            ? snapshotLine(element)
                            : null,
                    relations: relations(element)
                };
            })
        };
        coordinateBoard.removeObject(coordinateParabola);
        const coordinateRemoval = presence(
            coordinateBoard,
            coordinateAdded
        );

        const parentRemoval = {};
        for (const parentName of ["focus", "directrix"]) {
            const board = createBoard();
            const localFocus = point(board, [2, 1], `${parentName}Focus`);
            const localLine = line(
                board,
                point(board, [-1, 3], `${parentName}Line1`),
                point(board, [-1, -3], `${parentName}Line2`),
                `${parentName}Directrix`
            );
            const before = objectOrder(board);
            const parabola = board.create(
                "parabola",
                [localFocus, localLine],
                {
                    id: `${parentName}Parabola`,
                    name: "",
                    withLabel: false
                }
            );
            const added = objectOrder(board).slice(before.length);
            board.removeObject(
                parentName === "focus" ? localFocus : localLine
            );
            parentRemoval[parentName] = {
                addedObjects: added,
                presence: presence(
                    board,
                    [
                        localFocus.id,
                        localLine.id,
                        parabola.center.id,
                        parabola.id
                    ]
                )
            };
        }

        const duplicateBoard = createBoard();
        point(duplicateBoard, [7, 5], "takenParabola");
        const duplicateBefore = objectOrder(duplicateBoard);
        const duplicateResult = captureError(() =>
            duplicateBoard.create(
                "parabola",
                [[3, 0], [[0, 1], [0, -1]]],
                {
                    id: "takenParabola",
                    name: "",
                    withLabel: false
                }
            )
        );
        const duplicateId = {
            result: duplicateResult,
            addedObjects: objectOrder(duplicateBoard).slice(
                duplicateBefore.length
            )
        };

        const invalidBoard = createBoard();
        const invalidFocus = point(
            invalidBoard,
            [2, 0],
            "invalidFocus"
        );
        const invalidOther = point(
            invalidBoard,
            [0, 0],
            "invalidOther"
        );
        const invalidLine = line(
            invalidBoard,
            invalidOther,
            point(invalidBoard, [0, 2], "invalidLine2"),
            "invalidLine"
        );
        const invalidParents = [
            ["tooFew", [invalidFocus]],
            ["firstLine", [invalidLine, invalidLine]],
            ["secondPoint", [invalidFocus, invalidOther]],
            ["unknownFocus", ["missing", invalidLine]],
            [
                "malformedDirectrix",
                [invalidFocus, [[0, 1], [0, 0], [0, -1]]]
            ]
        ].map(([name, parents]) => {
            const before = objectOrder(invalidBoard);
            const result = captureError(() =>
                invalidBoard.create("parabola", parents, {
                    id: `invalid-${name}`,
                    name: "",
                    withLabel: false
                })
            );
            return {
                name,
                result,
                addedObjects: objectOrder(invalidBoard).slice(before.length)
            };
        });

        const degenerateCases = {};
        for (const [name, focusCoordinates, lineCoordinates] of [
            ["focusOnLine", [0, 1], [[0, 3], [0, -3]]],
            ["coincidentLinePoints", [2, 1], [[0, 0], [0, 0]]],
            ["idealFirstEndpoint", [2, 1], [[0, 1, 0], [1, 0, 0]]],
            ["idealSecondEndpoint", [2, 1], [[1, 0, 0], [0, 1, 0]]]
        ]) {
            const board = createBoard();
            const localFocus = point(
                board,
                focusCoordinates,
                `${name}Focus`
            );
            const linePoint1 = point(
                board,
                lineCoordinates[0],
                `${name}Line1`
            );
            const linePoint2 = point(
                board,
                lineCoordinates[1],
                `${name}Line2`
            );
            const localLine = line(
                board,
                linePoint1,
                linePoint2,
                `${name}Directrix`
            );
            const result = captureError(() =>
                board.create(
                    "parabola",
                    [localFocus, localLine],
                    {
                        id: `${name}Parabola`,
                        name: "",
                        withLabel: false
                    }
                )
            );
            board.update();
            const parabola = board.objects[`${name}Parabola`];
            degenerateCases[name] = {
                result,
                output: parabola
                    ? snapshotParabola(parabola)
                    : null,
                directrix: snapshotLine(localLine),
                objectOrder: objectOrder(board)
            };
        }

        return {
            version: JXG.version,
            pointInitial,
            pointMoved,
            customDomain,
            referenceForms,
            coordinateInitial,
            coordinateRemoval,
            parentRemoval,
            duplicateId,
            invalidParents,
            degenerateCases
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
