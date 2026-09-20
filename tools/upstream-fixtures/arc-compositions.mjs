/*
 * Official JSXGraph 1.13.3 arc-composition behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/arc-compositions.mjs
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
            if (Object.is(value, -0)) {
                return "-0";
            }
            return value;
        };
        const vector = (values) => Array.from(values, classify);
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            descendants: Object.keys(element.descendants),
            ancestors: Object.keys(element.ancestors)
        });
        const snapshotPoint = (point) => ({
            id: point.id,
            elType: point.elType,
            type: point.type,
            elementClass: point.elementClass,
            coords: vector(point.coords.usrCoords),
            draggable: point.isDraggable,
            dump: point.dump,
            relations: relations(point)
        });
        const snapshotArc = (arc) => ({
            id: arc.id,
            elType: arc.elType,
            type: arc.type,
            elementClass: arc.elementClass,
            center: snapshotPoint(arc.center),
            midpoint: arc.midpoint ? arc.midpoint.id : null,
            radiuspoint: arc.radiuspoint.id,
            anglepoint: arc.anglepoint.id,
            point2: arc.point2.id,
            point3: arc.point3.id,
            useDirection: arc.useDirection,
            selection: arc.visProp.selection,
            orientation: arc.visProp.orientation,
            radius: classify(arc.Radius()),
            radians: classify(arc.Value("radians")),
            dataX: vector(arc.dataX),
            dataY: vector(arc.dataY),
            inherits: arc.inherits.map((element) => element.id),
            subs: Object.fromEntries(
                Object.entries(arc.subs).map(([key, value]) => [
                    key,
                    value.id
                ])
            ),
            relations: relations(arc)
        });
        const point = (board, coords, id) =>
            board.create("point", coords, {
                id,
                name: "",
                withLabel: false
            });
        const arc = (board, type, parents, id, attributes = {}) =>
            board.create(type, parents, {
                id,
                name: "",
                withLabel: false,
                ...attributes
            });
        const presence = (board, ids) =>
            Object.fromEntries(
                ids.map((id) => [id, Boolean(board.objects[id])])
            );

        const board = createBoard();
        const a = point(board, [-4, -1], "a");
        const b = point(board, [1, 4], "b");
        const c = point(board, [5, -2], "c");
        const semicircle = arc(board, "semicircle", [a, b], "semicircle");
        const circumcircleArc = arc(
            board,
            "circumcirclearc",
            [a, b, c],
            "circumcircleArc"
        );
        const minorArc = arc(
            board,
            "minorarc",
            [a, b, c],
            "minorArc",
            {selection: "major"}
        );
        const majorArc = arc(
            board,
            "majorarc",
            [a, b, c],
            "majorArc",
            {selection: "minor"}
        );
        const snapshot = () => ({
            semicircle: snapshotArc(semicircle),
            circumcircleArc: snapshotArc(circumcircleArc),
            minorArc: snapshotArc(minorArc),
            majorArc: snapshotArc(majorArc),
            sources: {
                a: relations(a),
                b: relations(b),
                c: relations(c)
            }
        });

        const afterCreation = snapshot();
        board.update();
        const afterFirstUpdate = snapshot();
        a.setPositionDirectly(JXG.COORDS_BY_USER, [-6, 2]);
        b.setPositionDirectly(JXG.COORDS_BY_USER, [0, 3]);
        c.setPositionDirectly(JXG.COORDS_BY_USER, [4, -4]);
        board.update();
        const movedOnce = snapshot();
        board.update();
        const movedTwice = snapshot();

        const directionBoard = createBoard();
        const directionCenter = point(directionBoard, [0, 0], "directionCenter");
        const directionFirst = point(directionBoard, [2, 0], "directionFirst");
        const directionThird = point(directionBoard, [0, 2], "directionThird");
        const directionSelector = point(
            directionBoard,
            [0, -2],
            "directionSelector"
        );
        const directionArc = arc(
            directionBoard,
            "arc",
            [
                directionCenter,
                directionFirst,
                directionThird,
                directionSelector
            ],
            "directionArc",
            {useDirection: true}
        );
        const directionInitial = snapshotArc(directionArc);
        directionSelector.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [0, 3]
        );
        directionBoard.update();
        const directionMovedOnce = snapshotArc(directionArc);
        directionBoard.update();
        const directionMovedTwice = snapshotArc(directionArc);

        const coordinateDirectionBoard = createBoard();
        const coordinateDirectionArc = arc(
            coordinateDirectionBoard,
            "arc",
            [[0, 0], [2, 0], [0, 2], [0, -2]],
            undefined,
            {useDirection: true}
        );
        const coordinateDirectionIds = [
            coordinateDirectionArc.id,
            ...coordinateDirectionArc.parents
        ];
        const coordinateDirectionInitial = {
            output: snapshotArc(coordinateDirectionArc),
            objects: coordinateDirectionIds.map((id) => ({
                id,
                relations: relations(coordinateDirectionBoard.objects[id])
            }))
        };
        coordinateDirectionBoard.removeObject(coordinateDirectionArc);
        const coordinateDirectionRemoval = presence(
            coordinateDirectionBoard,
            coordinateDirectionIds
        );

        const coordinateSemicircleBoard = createBoard();
        const coordinateSemicircle = arc(
            coordinateSemicircleBoard,
            "semicircle",
            [[-4, -1], [1, 4]]
        );
        const coordinateSemicircleIds = [
            coordinateSemicircle.id,
            coordinateSemicircle.midpoint.id,
            ...coordinateSemicircle.parents
        ];
        const coordinateSemicircleInitial = {
            output: snapshotArc(coordinateSemicircle),
            objects: coordinateSemicircleIds.map((id) => ({
                id,
                relations: relations(coordinateSemicircleBoard.objects[id])
            }))
        };
        coordinateSemicircleBoard.removeObject(coordinateSemicircle);
        const coordinateSemicircleRemoval = presence(
            coordinateSemicircleBoard,
            coordinateSemicircleIds
        );

        const coordinateCircumcircleArcBoard = createBoard();
        const coordinateCircumcircleArc = arc(
            coordinateCircumcircleArcBoard,
            "circumcirclearc",
            [[-4, -1], [1, 4], [5, -2]]
        );
        const coordinateCircumcircleArcIds = [
            coordinateCircumcircleArc.id,
            coordinateCircumcircleArc.center.id,
            ...coordinateCircumcircleArc.parents
        ];
        const coordinateCircumcircleArcInitial = {
            output: snapshotArc(coordinateCircumcircleArc),
            objects: coordinateCircumcircleArcIds.map((id) => ({
                id,
                relations: relations(
                    coordinateCircumcircleArcBoard.objects[id]
                )
            }))
        };
        coordinateCircumcircleArcBoard.removeObject(
            coordinateCircumcircleArc
        );
        const coordinateCircumcircleArcRemoval = presence(
            coordinateCircumcircleArcBoard,
            coordinateCircumcircleArcIds
        );

        const helperRemovalBoard = createBoard();
        const helperA = point(helperRemovalBoard, [-4, -1], "helperA");
        const helperB = point(helperRemovalBoard, [1, 4], "helperB");
        const helperC = point(helperRemovalBoard, [5, -2], "helperC");
        const helperSemicircle = arc(
            helperRemovalBoard,
            "semicircle",
            [helperA, helperB],
            "helperSemicircle"
        );
        const helperCircumcircleArc = arc(
            helperRemovalBoard,
            "circumcirclearc",
            [helperA, helperB, helperC],
            "helperCircumcircleArc"
        );
        const midpointId = helperSemicircle.midpoint.id;
        const circumcenterId = helperCircumcircleArc.center.id;
        helperRemovalBoard.removeObject(helperSemicircle.midpoint);
        const midpointRemoval = presence(helperRemovalBoard, [
            "helperA",
            "helperB",
            "helperC",
            "helperSemicircle",
            midpointId,
            "helperCircumcircleArc",
            circumcenterId
        ]);
        helperRemovalBoard.removeObject(helperCircumcircleArc.center);
        const circumcenterRemoval = presence(helperRemovalBoard, [
            "helperA",
            "helperB",
            "helperC",
            "helperCircumcircleArc",
            circumcenterId
        ]);

        const failures = {};
        const failureBoard = createBoard();
        const failurePoint = point(
            failureBoard,
            [0, 0],
            "failurePoint"
        );
        const failureLine = failureBoard.create(
            "line",
            [failurePoint, [1, 1]],
            {name: "", withLabel: false}
        );
        for (const [name, callback] of Object.entries({
            semicircleTooFew: () =>
                failureBoard.create("semicircle", [failurePoint]),
            semicircleWrongType: () =>
                failureBoard.create("semicircle", [
                    failurePoint,
                    failureLine
                ]),
            circumcircleArcTooFew: () =>
                failureBoard.create("circumcirclearc", [
                    failurePoint,
                    [1, 1]
                ]),
            circumcircleArcWrongType: () =>
                failureBoard.create("circumcirclearc", [
                    failurePoint,
                    [1, 1],
                    failureLine
                ]),
            minorArcTooFew: () =>
                failureBoard.create("minorarc", [
                    failurePoint,
                    [1, 1]
                ]),
            majorArcWrongType: () =>
                failureBoard.create("majorarc", [
                    failurePoint,
                    [1, 1],
                    failureLine
                ])
        })) {
            try {
                callback();
                failures[name] = null;
            } catch (failure) {
                failures[name] = failure.message;
            }
        }

        const degenerateCases = {};
        for (const [name, configuration] of Object.entries({
            semicircleCoincident: {
                type: "semicircle",
                coordinates: [[1, 2], [1, 2]]
            },
            circumcircleArcCollinear: {
                type: "circumcirclearc",
                coordinates: [[-3, 0], [0, 0], [4, 0]]
            },
            circumcircleArcCoincident: {
                type: "circumcirclearc",
                coordinates: [[1, 2], [1, 2], [1, 2]]
            },
            minorArcCoincident: {
                type: "minorarc",
                coordinates: [[1, 2], [1, 2], [1, 2]]
            },
            majorArcCoincident: {
                type: "majorarc",
                coordinates: [[1, 2], [1, 2], [1, 2]]
            }
        })) {
            const degenerateBoard = createBoard();
            try {
                const output = arc(
                    degenerateBoard,
                    configuration.type,
                    configuration.coordinates,
                    `${name}Output`
                );
                degenerateBoard.update();
                degenerateCases[name] = {
                    output: snapshotArc(output),
                    error: null
                };
            } catch (failure) {
                degenerateCases[name] = {
                    output: null,
                    error: failure.message
                };
            }
        }

        return {
            version: JXG.version,
            afterCreation,
            afterFirstUpdate,
            movedOnce,
            movedTwice,
            directionInitial,
            directionMovedOnce,
            directionMovedTwice,
            coordinateDirectionInitial,
            coordinateDirectionRemoval,
            coordinateSemicircleInitial,
            coordinateSemicircleRemoval,
            coordinateCircumcircleArcInitial,
            coordinateCircumcircleArcRemoval,
            midpointRemoval,
            circumcenterRemoval,
            failures,
            degenerateCases
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
