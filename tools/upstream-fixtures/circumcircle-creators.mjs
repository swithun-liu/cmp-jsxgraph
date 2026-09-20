/*
 * Official JSXGraph 1.13.3 circumcenter/circumcircle creator behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/circumcircle-creators.mjs
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
            needsRegularUpdate: point.needsRegularUpdate,
            dump: point.dump,
            relations: relations(point)
        });
        const snapshotCircle = (circle) => ({
            id: circle.id,
            name: circle.name,
            elType: circle.elType,
            type: circle.type,
            elementClass: circle.elementClass,
            center: snapshotPoint(circle.center),
            midpoint: circle.midpoint.id,
            radiuspoint: circle.point2.id,
            radius: classify(circle.Radius()),
            standardForm: vector(circle.stdform),
            quadraticForm: circle.quadraticform.map(vector),
            draggable: circle.isDraggable,
            needsRegularUpdate: circle.needsRegularUpdate,
            dump: circle.dump,
            inherits: circle.inherits.map((element) => element.id),
            subs: Object.fromEntries(
                Object.entries(circle.subs).map(([key, value]) => [
                    key,
                    value.id
                ])
            ),
            relations: relations(circle)
        });
        const point = (board, coords, id) =>
            board.create("point", coords, {
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
        const b = point(board, [1, 4], "b");
        const c = point(board, [5, -2], "c");
        const center = board.create("circumcenter", [a, b, c], {
            id: "center",
            name: "center-name",
            fixed: true,
            needsRegularUpdate: false,
            withLabel: false
        });
        const midpointAlias = board.create(
            "circumcirclemidpoint",
            [a, b, c],
            {
                id: "midpointAlias",
                name: "",
                withLabel: false
            }
        );
        const circumcircle = board.create("circumcircle", [a, b, c], {
            id: "circumcircle",
            name: "",
            withLabel: false
        });
        const snapshot = () => ({
            center: snapshotPoint(center),
            midpointAlias: snapshotPoint(midpointAlias),
            circumcircle: snapshotCircle(circumcircle),
            sources: {
                a: relations(a),
                b: relations(b),
                c: relations(c)
            }
        });

        board.update();
        const initial = snapshot();
        a.setPositionDirectly(JXG.COORDS_BY_USER, [-6, 2]);
        b.setPositionDirectly(JXG.COORDS_BY_USER, [0, 3]);
        c.setPositionDirectly(JXG.COORDS_BY_USER, [4, -4]);
        board.update();
        const moved = snapshot();

        const coordinateCenterBoard = createBoard();
        const coordinateCenter = coordinateCenterBoard.create(
            "circumcenter",
            [[-4, -1], [1, 4], [5, -2]],
            {name: "", withLabel: false}
        );
        const coordinateCenterIds = [
            coordinateCenter.id,
            ...coordinateCenter.parents
        ];
        const coordinateCenterInitial = {
            output: snapshotPoint(coordinateCenter),
            objects: coordinateCenterIds.map((id) => ({
                id,
                relations: relations(coordinateCenterBoard.objects[id])
            }))
        };
        coordinateCenterBoard.removeObject(coordinateCenter);
        const coordinateCenterRemoval = presence(
            coordinateCenterBoard,
            coordinateCenterIds
        );

        const coordinateAliasBoard = createBoard();
        const coordinateAlias = coordinateAliasBoard.create(
            "circumcirclemidpoint",
            [[-4, -1], [1, 4], [5, -2]],
            {name: "", withLabel: false}
        );
        const coordinateAliasIds = [
            coordinateAlias.id,
            ...coordinateAlias.parents
        ];
        const coordinateAliasInitial = snapshotPoint(coordinateAlias);
        coordinateAliasBoard.removeObject(coordinateAlias);
        const coordinateAliasRemoval = presence(
            coordinateAliasBoard,
            coordinateAliasIds
        );

        const coordinateCircleBoard = createBoard();
        const coordinateCircle = coordinateCircleBoard.create(
            "circumcircle",
            [[-4, -1], [1, 4], [5, -2]],
            {name: "", withLabel: false}
        );
        const coordinateCircleIds = [
            coordinateCircle.id,
            coordinateCircle.center.id,
            ...coordinateCircle.parents
        ];
        const coordinateCircleInitial = {
            output: snapshotCircle(coordinateCircle),
            objects: coordinateCircleIds.map((id) => ({
                id,
                relations: relations(coordinateCircleBoard.objects[id])
            }))
        };
        coordinateCircleBoard.removeObject(coordinateCircle);
        const coordinateCircleRemoval = presence(
            coordinateCircleBoard,
            coordinateCircleIds
        );
        coordinateCircleBoard.removeObject(coordinateCircle.center);
        const coordinateCenterRemovalAfterCircle = presence(
            coordinateCircleBoard,
            coordinateCircleIds
        );

        const failures = {};
        const failureBoard = createBoard();
        const failurePoint = point(failureBoard, [0, 0], "failurePoint");
        const failureLine = failureBoard.create(
            "line",
            [[0, 0], [1, 1]],
            {name: "", withLabel: false}
        );
        for (const [name, callback] of Object.entries({
            centerTooFew: () =>
                failureBoard.create("circumcenter", [
                    [0, 0],
                    [1, 1]
                ]),
            centerWrongType: () =>
                failureBoard.create("circumcenter", [
                    failurePoint,
                    failurePoint,
                    failureLine
                ]),
            aliasTooFew: () =>
                failureBoard.create("circumcirclemidpoint", [
                    [0, 0],
                    [1, 1]
                ]),
            circleTooFew: () =>
                failureBoard.create("circumcircle", [
                    [0, 0],
                    [1, 1]
                ]),
            circleWrongType: () =>
                failureBoard.create("circumcircle", [
                    failurePoint,
                    failurePoint,
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
        for (const [name, coordinates] of Object.entries({
            collinear: [[-3, 0], [0, 0], [4, 0]],
            allCoincident: [[1, 2], [1, 2], [1, 2]],
            firstSecondCoincident: [[1, 2], [1, 2], [-2, 3]]
        })) {
            const degenerateBoard = createBoard();
            const points = coordinates.map((coords, index) =>
                point(degenerateBoard, coords, `${name}${index}`)
            );
            const degenerateCenter = degenerateBoard.create(
                "circumcenter",
                points,
                {name: "", withLabel: false}
            );
            const degenerateCircle = degenerateBoard.create(
                "circumcircle",
                points,
                {name: "", withLabel: false}
            );
            degenerateBoard.update();
            degenerateCases[name] = {
                center: snapshotPoint(degenerateCenter),
                circle: snapshotCircle(degenerateCircle)
            };
        }

        return {
            version: JXG.version,
            initial,
            moved,
            coordinateCenterInitial,
            coordinateCenterRemoval,
            coordinateAliasInitial,
            coordinateAliasRemoval,
            coordinateCircleInitial,
            coordinateCircleRemoval,
            coordinateCenterRemovalAfterCircle,
            failures,
            degenerateCases
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
