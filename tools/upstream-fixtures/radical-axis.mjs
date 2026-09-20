/*
 * Official JSXGraph 1.13.3 RadicalAxis behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/radical-axis.mjs
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
            center: vector(circle.center.coords.usrCoords),
            radius: classify(circle.Radius()),
            relations: relations(circle)
        });
        const snapshotAxis = (axis) => ({
            id: axis.id,
            name: axis.name,
            elType: axis.elType,
            type: axis.type,
            elementClass: axis.elementClass,
            stdform: vector(axis.stdform),
            constrained: axis.constrained,
            draggable: axis.isDraggable,
            straightFirst: axis.visProp.straightfirst,
            straightLast: axis.visProp.straightlast,
            parents: [...axis.parents],
            inherits: axis.inherits.map((element) => element.id),
            point1: snapshotPoint(axis.point1),
            point2: snapshotPoint(axis.point2),
            relations: relations(axis)
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
        const radicalAxis = (board, parents, id, attributes = {}) =>
            board.create("radicalaxis", parents, {
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
                },
                ...attributes
            });

        const board = createBoard();
        const center1 = point(board, [-3, -1], "center1");
        const radius1 = point(board, [-1, -1], "radius1");
        const circle1 = circle(board, center1, radius1, "circle1");
        const center2 = point(board, [2, 2], "center2");
        const radius2 = point(board, [5, 2], "radius2");
        const circle2 = circle(board, center2, radius2, "circle2");
        const axis = radicalAxis(board, [circle1, circle2], "axis", {
            straightFirst: false,
            straightLast: true
        });
        board.update();
        const initial = {
            axis: snapshotAxis(axis),
            circles: [snapshotCircle(circle1), snapshotCircle(circle2)],
            objectOrder: board.objectsList.map((element) => element.id)
        };

        center1.setPositionDirectly(JXG.COORDS_BY_USER, [-4, 1]);
        radius1.setPositionDirectly(JXG.COORDS_BY_USER, [-1, 1]);
        center2.setPositionDirectly(JXG.COORDS_BY_USER, [1, -2]);
        radius2.setPositionDirectly(JXG.COORDS_BY_USER, [3, -2]);
        board.update();
        const moved = {
            axis: snapshotAxis(axis),
            circles: [snapshotCircle(circle1), snapshotCircle(circle2)]
        };

        const removalIds = [axis.id, axis.point1.id, axis.point2.id];
        board.removeObject(axis);
        const removal = {
            presence: Object.fromEntries(
                removalIds.map((id) => [id, Boolean(board.objects[id])])
            ),
            circle1: relations(circle1),
            circle2: relations(circle2),
            objectOrder: board.objectsList.map((element) => element.id)
        };

        const parentRemovalBoard = createBoard();
        const parentCenter1 = point(
            parentRemovalBoard,
            [-2, 0],
            "parentCenter1"
        );
        const parentRadius1 = point(
            parentRemovalBoard,
            [-1, 0],
            "parentRadius1"
        );
        const parentCircle1 = circle(
            parentRemovalBoard,
            parentCenter1,
            parentRadius1,
            "parentCircle1"
        );
        const parentCenter2 = point(
            parentRemovalBoard,
            [2, 0],
            "parentCenter2"
        );
        const parentRadius2 = point(
            parentRemovalBoard,
            [4, 0],
            "parentRadius2"
        );
        const parentCircle2 = circle(
            parentRemovalBoard,
            parentCenter2,
            parentRadius2,
            "parentCircle2"
        );
        const parentAxis = radicalAxis(
            parentRemovalBoard,
            [parentCircle1, parentCircle2],
            "parentAxis"
        );
        const parentRemovalIds = [
            parentCircle1.id,
            parentAxis.id,
            parentAxis.point1.id,
            parentAxis.point2.id
        ];
        parentRemovalBoard.removeObject(parentCircle1);
        const parentRemoval = {
            presence: Object.fromEntries(
                parentRemovalIds.map((id) => [
                    id,
                    Boolean(parentRemovalBoard.objects[id])
                ])
            ),
            secondCircle: relations(parentCircle2),
            objectOrder: parentRemovalBoard.objectsList.map(
                (element) => element.id
            )
        };

        const degenerateBoard = createBoard();
        const commonCenter = point(
            degenerateBoard,
            [1, -1],
            "commonCenter"
        );
        const radiusA = point(degenerateBoard, [3, -1], "radiusA");
        const radiusB = point(degenerateBoard, [4, -1], "radiusB");
        const concentricA = circle(
            degenerateBoard,
            commonCenter,
            radiusA,
            "concentricA"
        );
        const concentricB = circle(
            degenerateBoard,
            commonCenter,
            radiusB,
            "concentricB"
        );
        const concentric = radicalAxis(
            degenerateBoard,
            [concentricA, concentricB],
            "concentric"
        );
        const duplicateParent = radicalAxis(
            degenerateBoard,
            [concentricA, concentricA],
            "duplicateParent"
        );
        const identicalCenter1 = point(
            degenerateBoard,
            [-2, 2],
            "identicalCenter1"
        );
        const identicalRadius1 = point(
            degenerateBoard,
            [0, 2],
            "identicalRadius1"
        );
        const identicalCenter2 = point(
            degenerateBoard,
            [-2, 2],
            "identicalCenter2"
        );
        const identicalRadius2 = point(
            degenerateBoard,
            [0, 2],
            "identicalRadius2"
        );
        const identicalCircle1 = circle(
            degenerateBoard,
            identicalCenter1,
            identicalRadius1,
            "identicalCircle1"
        );
        const identicalCircle2 = circle(
            degenerateBoard,
            identicalCenter2,
            identicalRadius2,
            "identicalCircle2"
        );
        const identical = radicalAxis(
            degenerateBoard,
            [identicalCircle1, identicalCircle2],
            "identical"
        );
        degenerateBoard.update();
        const degenerate = {
            concentric: snapshotAxis(concentric),
            duplicateParent: snapshotAxis(duplicateParent),
            identical: snapshotAxis(identical)
        };

        const failures = {};
        const failureBoard = createBoard();
        const failureCenter1 = point(
            failureBoard,
            [-2, 0],
            "failureCenter1"
        );
        const failureRadius1 = point(
            failureBoard,
            [-1, 0],
            "failureRadius1"
        );
        const failureCircle1 = circle(
            failureBoard,
            failureCenter1,
            failureRadius1,
            "failureCircle1"
        );
        const failureCenter2 = point(
            failureBoard,
            [2, 0],
            "failureCenter2"
        );
        const failureRadius2 = point(
            failureBoard,
            [3, 0],
            "failureRadius2"
        );
        const failureCircle2 = circle(
            failureBoard,
            failureCenter2,
            failureRadius2,
            "failureCircle2"
        );
        for (const [name, callback] of Object.entries({
            missingParent: () =>
                failureBoard.create("radicalaxis", [failureCircle1]),
            extraParent: () =>
                failureBoard.create(
                    "radicalaxis",
                    [failureCircle1, failureCircle2, failureCircle1]
                ),
            firstPoint: () =>
                failureBoard.create(
                    "radicalaxis",
                    [failureCenter1, failureCircle2]
                ),
            secondPoint: () =>
                failureBoard.create(
                    "radicalaxis",
                    [failureCircle1, failureCenter2]
                )
        })) {
            try {
                callback();
                failures[name] = null;
            } catch (failure) {
                failures[name] = failure.message;
            }
        }

        point(failureBoard, [0, 4], "duplicateAxis");
        const beforeDuplicate = failureBoard.objectsList.map(
            (element) => element.id
        );
        try {
            radicalAxis(
                failureBoard,
                [failureCircle1, failureCircle2],
                "duplicateAxis"
            );
            failures.duplicateId = null;
        } catch (failure) {
            failures.duplicateId = failure.message;
        }
        const afterDuplicate = failureBoard.objectsList.map(
            (element) => element.id
        );
        const duplicateLeak = {
            before: beforeDuplicate,
            after: afterDuplicate,
            added: afterDuplicate.filter(
                (id) => !beforeDuplicate.includes(id)
            ),
            snapshots: afterDuplicate
                .filter((id) => !beforeDuplicate.includes(id))
                .map((id) => snapshotPoint(failureBoard.objects[id]))
        };

        return {
            version: JXG.version,
            initial,
            moved,
            removal,
            parentRemoval,
            degenerate,
            failures,
            duplicateLeak
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
