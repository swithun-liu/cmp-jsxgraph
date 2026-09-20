/*
 * Official JSXGraph 1.13.3 bisector-lines and Composition behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/bisector-lines.mjs
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
            name: point.name,
            elType: point.elType,
            type: point.type,
            elementClass: point.elementClass,
            coords: vector(point.coords.usrCoords),
            draggable: point.isDraggable,
            fixed: point.visProp.fixed,
            visible: point.visProp.visible,
            dump: point.dump,
            relations: relations(point)
        });
        const snapshotLine = (line) => ({
            id: line.id,
            name: line.name,
            elType: line.elType,
            type: line.type,
            elementClass: line.elementClass,
            stdform: vector(line.stdform),
            point1: snapshotPoint(line.point1),
            point2: snapshotPoint(line.point2),
            draggable: line.isDraggable,
            fixed: line.visProp.fixed,
            visible: line.visProp.visible,
            strokeColor: line.visProp.strokecolor,
            strokeWidth: line.visProp.strokewidth,
            layer: line.visProp.layer,
            straightFirst: line.visProp.straightfirst,
            straightLast: line.visProp.straightlast,
            dump: line.dump,
            relations: relations(line)
        });
        const snapshotComposition = (composition) => ({
            id: composition.id ?? null,
            name: composition.name ?? null,
            elType: composition.elType,
            dump: composition.dump,
            parents: composition.parents ?? null,
            getParents: composition.getParents() ?? null,
            elements: Object.keys(composition.elements),
            elementsByName: Object.keys(composition.elementsByName),
            objectsList: composition.objectsList.map(
                (element) => element.id
            ),
            line1Matches: composition.line1 === composition.subs.line1,
            line2Matches: composition.line2 === composition.subs.line2,
            selectedLine1ById:
                composition.select(composition.line1.id) ===
                composition.line1,
            selectedLine2ByName:
                composition.select(composition.line2.name) ===
                composition.line2,
            line1: snapshotLine(composition.line1),
            line2: snapshotLine(composition.line2)
        });
        const point = (board, coordinates, id) =>
            board.create("point", coordinates, {
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
        const presence = (board, ids) =>
            Object.fromEntries(
                ids.map((id) => [id, Boolean(board.objects[id])])
            );

        const board = createBoard();
        const a = point(board, [-4, -1], "a");
        const b = point(board, [2, 3], "b");
        const c = point(board, [-3, 4], "c");
        const d = point(board, [4, -2], "d");
        const first = line(board, [a, b], "first");
        const second = line(board, [c, d], "second");
        const bisectors = board.create(
            "bisectorlines",
            [first, second],
            {
                layer: 5,
                line1: {
                    id: "bisectorFirst",
                    name: "first-bisector",
                    strokeColor: "#1565c0",
                    strokeWidth: 3,
                    withLabel: false
                },
                line2: {
                    id: "bisectorSecond",
                    name: "second-bisector",
                    strokeColor: "#c62828",
                    strokeWidth: 2,
                    withLabel: false
                }
            }
        );

        board.update();
        const initial = {
            composition: snapshotComposition(bisectors),
            sources: {
                a: relations(a),
                b: relations(b),
                c: relations(c),
                d: relations(d),
                first: snapshotLine(first),
                second: snapshotLine(second)
            },
            boardObjectIds: Object.keys(board.objects)
        };

        a.setPositionDirectly(JXG.COORDS_BY_USER, [-2, -3]);
        d.setPositionDirectly(JXG.COORDS_BY_USER, [3, 1]);
        board.update();
        const moved = {
            composition: snapshotComposition(bisectors),
            sources: {
                first: snapshotLine(first),
                second: snapshotLine(second)
            }
        };

        bisectors.setAttribute({
            strokeColor: "#00897b",
            strokeWidth: 5
        });
        const afterForwardedAttribute = {
            line1: {
                strokeColor: bisectors.line1.visProp.strokecolor,
                strokeWidth: bisectors.line1.visProp.strokewidth
            },
            line2: {
                strokeColor: bisectors.line2.visProp.strokecolor,
                strokeWidth: bisectors.line2.visProp.strokewidth
            }
        };

        const compositionRemovalBoard = createBoard();
        const removalA = point(
            compositionRemovalBoard,
            [-4, -1],
            "removalA"
        );
        const removalB = point(
            compositionRemovalBoard,
            [2, 3],
            "removalB"
        );
        const removalC = point(
            compositionRemovalBoard,
            [-3, 4],
            "removalC"
        );
        const removalD = point(
            compositionRemovalBoard,
            [4, -2],
            "removalD"
        );
        const removalFirst = line(
            compositionRemovalBoard,
            [removalA, removalB],
            "removalFirst"
        );
        const removalSecond = line(
            compositionRemovalBoard,
            [removalC, removalD],
            "removalSecond"
        );
        const removalComposition = compositionRemovalBoard.create(
            "bisectorlines",
            [removalFirst, removalSecond],
            {
                line1: {
                    id: "removalBisectorFirst",
                    name: "",
                    withLabel: false
                },
                line2: {
                    id: "removalBisectorSecond",
                    name: "",
                    withLabel: false
                }
            }
        );
        const removalIds = [
            "removalA",
            "removalB",
            "removalC",
            "removalD",
            "removalFirst",
            "removalSecond",
            "removalBisectorFirst",
            "removalBisectorSecond",
            removalComposition.line1.point1.id,
            removalComposition.line1.point2.id,
            removalComposition.line2.point1.id,
            removalComposition.line2.point2.id
        ];
        const beforeCompositionRemoval = presence(
            compositionRemovalBoard,
            removalIds
        );
        let compositionRemovalError = null;
        try {
            compositionRemovalBoard.removeObject(removalComposition);
        } catch (failure) {
            compositionRemovalError = failure.message;
        }
        const afterCompositionRemoval = presence(
            compositionRemovalBoard,
            removalIds
        );
        const removedFromContainer =
            removalComposition.remove("line1");
        const afterContainerRemoval = {
            removedFromContainer,
            hasLine1Property:
                Object.prototype.hasOwnProperty.call(
                    removalComposition,
                    "line1"
                ),
            boardLine1Present: Boolean(
                compositionRemovalBoard.objects.removalBisectorFirst
            ),
            objectsList: removalComposition.objectsList.map(
                (element) => element.id
            )
        };
        compositionRemovalBoard.removeObject(removalFirst);
        const afterSourceLineRemoval = presence(
            compositionRemovalBoard,
            removalIds
        );
        removalA.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-1, -4]
        );
        compositionRemovalBoard.update();
        const afterRemovedSourcePointMove = {
            line1: snapshotLine(removalComposition.subs.line1),
            line2: snapshotLine(removalComposition.subs.line2)
        };

        const failures = {};
        const failureBoard = createBoard();
        const failureA = point(failureBoard, [0, 0], "failureA");
        const failureB = point(failureBoard, [1, 1], "failureB");
        const failureLine = line(
            failureBoard,
            [failureA, failureB],
            "failureLine"
        );
        for (const [name, callback] of Object.entries({
            missingParent: () =>
                failureBoard.create(
                    "bisectorlines",
                    [failureLine]
                ),
            pointParent: () =>
                failureBoard.create(
                    "bisectorlines",
                    [failureLine, failureA]
                ),
            unknownReference: () =>
                failureBoard.create(
                    "bisectorlines",
                    [failureLine, "missing"]
                ),
            duplicateChildId: () =>
                failureBoard.create(
                    "bisectorlines",
                    [failureLine, failureLine],
                    {
                        line1: {id: "duplicateBisector"},
                        line2: {id: "duplicateBisector"}
                    }
                )
        })) {
            const before = Object.keys(failureBoard.objects);
            try {
                const value = callback();
                failures[name] = {
                    error: null,
                    resultType: value?.elType ?? null,
                    before,
                    after: Object.keys(failureBoard.objects)
                };
            } catch (failure) {
                failures[name] = {
                    error: failure.message,
                    before,
                    after: Object.keys(failureBoard.objects)
                };
            }
        }

        const degenerateBoard = createBoard();
        const sameA = point(degenerateBoard, [1, 1], "sameA");
        const sameB = point(degenerateBoard, [1, 1], "sameB");
        const regularA = point(degenerateBoard, [-2, 0], "regularA");
        const regularB = point(degenerateBoard, [2, 0], "regularB");
        const collapsed = line(
            degenerateBoard,
            [sameA, sameB],
            "collapsed"
        );
        const regular = line(
            degenerateBoard,
            [regularA, regularB],
            "regular"
        );
        const degenerate = degenerateBoard.create(
            "bisectorlines",
            [collapsed, regular],
            {
                line1: {id: "degenerateFirst", withLabel: false},
                line2: {id: "degenerateSecond", withLabel: false}
            }
        );
        degenerateBoard.update();

        return {
            version: JXG.version,
            initial,
            moved,
            afterForwardedAttribute,
            removal: {
                beforeCompositionRemoval,
                compositionRemovalError,
                afterCompositionRemoval,
                afterContainerRemoval,
                afterSourceLineRemoval,
                afterRemovedSourcePointMove
            },
            failures,
            degenerate: snapshotComposition(degenerate)
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
