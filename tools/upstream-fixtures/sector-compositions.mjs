/*
 * Official JSXGraph 1.13.3 sector-composition behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/sector-compositions.mjs
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
            radiuspoint: arc.radiuspoint.id,
            anglepoint: arc.anglepoint.id,
            useDirection: Boolean(arc.useDirection),
            selection: arc.visProp.selection,
            orientation: arc.visProp.orientation,
            visible: arc.visProp.visible,
            radius: classify(arc.Radius()),
            dataX: vector(arc.dataX),
            dataY: vector(arc.dataY),
            relations: relations(arc)
        });
        const values = (sector) =>
            Object.fromEntries(
                [
                    ["default", undefined],
                    ["empty", ""],
                    ["length", "length"],
                    ["radians", "radians"],
                    ["degrees", "degrees"],
                    ["semicircle", "semicircle"],
                    ["circle", "circle"],
                    ["invalid", "turn"]
                ].map(([name, unit]) => [
                    name,
                    classify(
                        unit === undefined
                            ? sector.Value()
                            : sector.Value(unit)
                    )
                ])
            );
        const snapshotSector = (sector) => ({
            id: sector.id,
            elType: sector.elType,
            type: sector.type,
            elementClass: sector.elementClass,
            center: snapshotPoint(sector.center),
            radiuspoint: sector.radiuspoint.id,
            anglepoint: sector.anglepoint.id,
            point1: sector.point1.id,
            point2: sector.point2.id,
            point3: sector.point3.id,
            point4: sector.point4 ? sector.point4.id : null,
            useDirection: Boolean(sector.useDirection),
            selection: sector.visProp.selection,
            orientation: sector.visProp.orientation,
            radius: classify(sector.Radius()),
            values: values(sector),
            dataX: vector(sector.dataX),
            dataY: vector(sector.dataY),
            arc: snapshotArc(sector.arc),
            inherits: sector.inherits.map((element) => element.id),
            subs: Object.fromEntries(
                Object.entries(sector.subs).map(([key, value]) => [
                    key,
                    value.id
                ])
            ),
            relations: relations(sector)
        });
        const point = (board, coords, id) =>
            board.create("point", coords, {
                id,
                name: "",
                withLabel: false
            });
        const sector = (board, type, parents, id, attributes = {}) =>
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
        const circumcircleSector = sector(
            board,
            "circumcirclesector",
            [a, b, c],
            "circumcircleSector"
        );
        const minorSector = sector(
            board,
            "minorsector",
            [a, b, c],
            "minorSector",
            {selection: "major"}
        );
        const majorSector = sector(
            board,
            "majorsector",
            [a, b, c],
            "majorSector",
            {selection: "minor"}
        );
        const nonreflexAngle = sector(
            board,
            "nonreflexangle",
            [a, b, c],
            "nonreflexAngle",
            {radius: 2, selection: "major"}
        );
        const reflexAngle = sector(
            board,
            "reflexangle",
            [a, b, c],
            "reflexAngle",
            {radius: 2, selection: "minor"}
        );
        const snapshot = () => ({
            circumcircleSector: snapshotSector(circumcircleSector),
            minorSector: snapshotSector(minorSector),
            majorSector: snapshotSector(majorSector),
            nonreflexAngle: snapshotSector(nonreflexAngle),
            reflexAngle: snapshotSector(reflexAngle),
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
        const directionSector = sector(
            directionBoard,
            "sector",
            [
                directionCenter,
                directionFirst,
                directionThird,
                directionSelector
            ],
            "directionSector",
            {usedirection: true}
        );
        const directionInitial = snapshotSector(directionSector);
        directionSelector.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [0, 3]
        );
        directionBoard.update();
        const directionMovedOnce = snapshotSector(directionSector);
        directionBoard.update();
        const directionMovedTwice = snapshotSector(directionSector);

        const coordinateDirectionBoard = createBoard();
        const coordinateDirectionSector = sector(
            coordinateDirectionBoard,
            "sector",
            [[0, 0], [2, 0], [0, 2], [0, -2]],
            undefined,
            {usedirection: true}
        );
        const coordinateDirectionIds = [
            coordinateDirectionSector.id,
            coordinateDirectionSector.arc.id,
            ...coordinateDirectionSector.parents
        ];
        const coordinateDirectionInitial = {
            output: snapshotSector(coordinateDirectionSector),
            objects: coordinateDirectionIds.map((id) => ({
                id,
                relations: relations(
                    coordinateDirectionBoard.objects[id]
                )
            }))
        };
        coordinateDirectionBoard.removeObject(coordinateDirectionSector);
        const coordinateDirectionRemoval = presence(
            coordinateDirectionBoard,
            coordinateDirectionIds
        );

        const camelCaseDirectionBoard = createBoard();
        const camelCaseDirectionSector = sector(
            camelCaseDirectionBoard,
            "sector",
            [[0, 0], [2, 0], [0, 2], [0, -2]],
            undefined,
            {useDirection: true}
        );
        const camelCaseDirectionIgnored = snapshotSector(
            camelCaseDirectionSector
        );

        const coordinateTypes = {};
        for (const type of [
            "circumcirclesector",
            "minorsector",
            "majorsector",
            "nonreflexangle",
            "reflexangle"
        ]) {
            const coordinateBoard = createBoard();
            const output = sector(
                coordinateBoard,
                type,
                [[-4, -1], [1, 4], [5, -2]],
                undefined,
                type.includes("angle") ? {radius: 2} : {}
            );
            const ids = [
                output.id,
                output.arc.id,
                ...(output.center.id in coordinateBoard.objects &&
                !output.parents.includes(output.center.id)
                    ? [output.center.id]
                    : []),
                ...output.parents
            ];
            coordinateTypes[type] = {
                initial: {
                    output: snapshotSector(output),
                    objects: ids.map((id) => ({
                        id,
                        relations: relations(coordinateBoard.objects[id])
                    }))
                }
            };
            coordinateBoard.removeObject(output);
            coordinateTypes[type].removal = presence(coordinateBoard, ids);
        }

        const helperRemovalBoard = createBoard();
        const helperA = point(helperRemovalBoard, [-4, -1], "helperA");
        const helperB = point(helperRemovalBoard, [1, 4], "helperB");
        const helperC = point(helperRemovalBoard, [5, -2], "helperC");
        const helperOutput = sector(
            helperRemovalBoard,
            "circumcirclesector",
            [helperA, helperB, helperC],
            "helperOutput"
        );
        const helperCenterId = helperOutput.center.id;
        const helperArcId = helperOutput.arc.id;
        helperRemovalBoard.removeObject(helperOutput.center);
        const centerRemoval = presence(helperRemovalBoard, [
            "helperA",
            "helperB",
            "helperC",
            "helperOutput",
            helperCenterId,
            helperArcId
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
            circumcircleSectorTooFew: () =>
                failureBoard.create("circumcirclesector", [
                    failurePoint,
                    [1, 1]
                ]),
            circumcircleSectorWrongType: () =>
                failureBoard.create("circumcirclesector", [
                    failurePoint,
                    [1, 1],
                    failureLine
                ]),
            minorSectorTooFew: () =>
                failureBoard.create("minorsector", [
                    failurePoint,
                    [1, 1]
                ]),
            majorSectorWrongType: () =>
                failureBoard.create("majorsector", [
                    failurePoint,
                    [1, 1],
                    failureLine
                ]),
            nonreflexAngleTooFew: () =>
                failureBoard.create("nonreflexangle", [
                    failurePoint,
                    [1, 1]
                ]),
            reflexAngleWrongType: () =>
                failureBoard.create("reflexangle", [
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
            circumcircleSectorCollinear: {
                type: "circumcirclesector",
                coordinates: [[-3, 0], [0, 0], [4, 0]]
            },
            circumcircleSectorCoincident: {
                type: "circumcirclesector",
                coordinates: [[1, 2], [1, 2], [1, 2]]
            },
            minorSectorCoincident: {
                type: "minorsector",
                coordinates: [[1, 2], [1, 2], [1, 2]]
            },
            majorSectorCoincident: {
                type: "majorsector",
                coordinates: [[1, 2], [1, 2], [1, 2]]
            },
            nonreflexAngleCoincident: {
                type: "nonreflexangle",
                coordinates: [[1, 2], [1, 2], [1, 2]],
                attributes: {radius: 2}
            },
            reflexAngleCoincident: {
                type: "reflexangle",
                coordinates: [[1, 2], [1, 2], [1, 2]],
                attributes: {radius: 2}
            }
        })) {
            const degenerateBoard = createBoard();
            try {
                const output = sector(
                    degenerateBoard,
                    configuration.type,
                    configuration.coordinates,
                    undefined,
                    configuration.attributes
                );
                degenerateBoard.update();
                degenerateCases[name] = {
                    output: snapshotSector(output),
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
            camelCaseDirectionIgnored,
            coordinateTypes,
            centerRemoval,
            failures,
            degenerateCases
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
