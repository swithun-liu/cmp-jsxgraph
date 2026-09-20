/*
 * Official JSXGraph 1.13.3 triangle-center construction behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/triangle-centers.mjs
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
        const vector = (values) => values.map(classify);
        const coordinates = (point) => vector(point.coords.usrCoords);
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
            coords: coordinates(point),
            draggable: point.isDraggable,
            dump: point.dump,
            relations: relations(point)
        });
        const snapshotLine = (line) => ({
            id: line.id,
            elType: line.elType,
            type: line.type,
            elementClass: line.elementClass,
            stdform: vector(line.stdform),
            point1: line.point1.id,
            point2: line.point2.id,
            point: snapshotPoint(line.point),
            inherits: line.inherits.map((element) => element.id),
            subs: Object.fromEntries(
                Object.entries(line.subs).map(([key, value]) => [
                    key,
                    value.id
                ])
            ),
            relations: relations(line)
        });
        const snapshotCircle = (circle) => ({
            id: circle.id,
            elType: circle.elType,
            type: circle.type,
            elementClass: circle.elementClass,
            center: snapshotPoint(circle.center),
            midpoint: circle.midpoint.id,
            radius: classify(circle.Radius()),
            standardForm: vector(circle.stdform),
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
        const withId = (id) => id ? {id} : {};
        const bisector = (board, parents, id) =>
            board.create("bisector", parents, {
                ...withId(id),
                name: "",
                withLabel: false
            });
        const incenter = (board, parents, id) =>
            board.create("incenter", parents, {
                ...withId(id),
                name: "",
                withLabel: false
            });
        const incircle = (board, parents, id) =>
            board.create("incircle", parents, {
                ...withId(id),
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
        const angleBisector = bisector(
            board,
            [a, b, c],
            "angleBisector"
        );
        const triangleIncenter = incenter(
            board,
            [a, b, c],
            "triangleIncenter"
        );
        const triangleIncircle = incircle(
            board,
            [a, b, c],
            "triangleIncircle"
        );
        const snapshot = () => ({
            bisector: snapshotLine(angleBisector),
            incenter: snapshotPoint(triangleIncenter),
            incircle: snapshotCircle(triangleIncircle),
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

        const coordinateBisectorBoard = createBoard();
        const coordinateBisector = bisector(
            coordinateBisectorBoard,
            [[-4, -1], [1, 4], [5, -2]]
        );
        const coordinateBisectorIds = [
            coordinateBisector.id,
            coordinateBisector.point.id,
            ...coordinateBisector.parents
        ];
        const coordinateBisectorInitial = {
            output: snapshotLine(coordinateBisector),
            objects: coordinateBisectorIds.map((id) => ({
                id,
                relations: relations(coordinateBisectorBoard.objects[id])
            }))
        };
        coordinateBisectorBoard.removeObject(coordinateBisector);
        const coordinateBisectorRemoval = presence(
            coordinateBisectorBoard,
            coordinateBisectorIds
        );

        const coordinateIncenterBoard = createBoard();
        const coordinateIncenter = incenter(
            coordinateIncenterBoard,
            [[-4, -1], [1, 4], [5, -2]]
        );
        const coordinateIncenterIds = [
            coordinateIncenter.id,
            ...coordinateIncenter.parents
        ];
        const coordinateIncenterInitial = {
            output: snapshotPoint(coordinateIncenter),
            objects: coordinateIncenterIds.map((id) => ({
                id,
                relations: relations(coordinateIncenterBoard.objects[id])
            }))
        };
        coordinateIncenterBoard.removeObject(coordinateIncenter);
        const coordinateIncenterRemoval = presence(
            coordinateIncenterBoard,
            coordinateIncenterIds
        );

        const coordinateIncircleBoard = createBoard();
        const coordinateIncircle = incircle(
            coordinateIncircleBoard,
            [[-4, -1], [1, 4], [5, -2]]
        );
        const coordinateIncircleIds = [
            coordinateIncircle.id,
            coordinateIncircle.center.id,
            ...coordinateIncircle.parents
        ];
        const coordinateIncircleInitial = {
            output: snapshotCircle(coordinateIncircle),
            objects: coordinateIncircleIds.map((id) => ({
                id,
                relations: relations(coordinateIncircleBoard.objects[id])
            }))
        };
        coordinateIncircleBoard.removeObject(coordinateIncircle);
        const coordinateIncircleRemoval = presence(
            coordinateIncircleBoard,
            coordinateIncircleIds
        );

        const helperRemovalBoard = createBoard();
        const helperA = point(helperRemovalBoard, [-4, -1], "helperA");
        const helperB = point(helperRemovalBoard, [1, 4], "helperB");
        const helperC = point(helperRemovalBoard, [5, -2], "helperC");
        const helperBisector = bisector(
            helperRemovalBoard,
            [helperA, helperB, helperC],
            "helperBisector"
        );
        const helperIncircle = incircle(
            helperRemovalBoard,
            [helperA, helperB, helperC],
            "helperIncircle"
        );
        const bisectorHelperId = helperBisector.point.id;
        const incircleCenterId = helperIncircle.center.id;
        helperRemovalBoard.removeObject(helperBisector.point);
        const bisectorHelperRemoval = presence(helperRemovalBoard, [
            "helperA",
            "helperB",
            "helperC",
            "helperBisector",
            bisectorHelperId,
            "helperIncircle",
            incircleCenterId
        ]);
        helperRemovalBoard.removeObject(helperIncircle.center);
        const incircleCenterRemoval = presence(helperRemovalBoard, [
            "helperA",
            "helperB",
            "helperC",
            "helperIncircle",
            incircleCenterId
        ]);

        const failures = {};
        const failureBoard = createBoard();
        const failureA = point(failureBoard, [0, 0], "failureA");
        for (const [name, callback] of Object.entries({
            bisectorTooFew: () =>
                failureBoard.create("bisector", [[0, 0], [1, 1]]),
            bisectorWrongType: () =>
                failureBoard.create("bisector", [
                    failureA,
                    failureA,
                    2
                ]),
            incenterTooFew: () =>
                failureBoard.create("incenter", [[0, 0], [1, 1]]),
            incenterWrongType: () =>
                failureBoard.create("incenter", [
                    failureA,
                    failureA,
                    2
                ]),
            incircleTooFew: () =>
                failureBoard.create("incircle", [[0, 0], [1, 1]]),
            incircleWrongType: () =>
                failureBoard.create("incircle", [
                    failureA,
                    failureA,
                    2
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
            allCoincident: [[1, 2], [1, 2], [1, 2]],
            firstSecondCoincident: [[1, 2], [1, 2], [-2, 3]],
            vertexThirdCoincident: [[-2, 3], [1, 2], [1, 2]],
            collinear: [[-3, 0], [0, 0], [4, 0]]
        })) {
            const degenerateBoard = createBoard();
            const points = coordinates.map((coords, index) =>
                point(degenerateBoard, coords, `${name}${index}`)
            );
            const outputBisector = bisector(
                degenerateBoard,
                points,
                `${name}Bisector`
            );
            const outputIncenter = incenter(
                degenerateBoard,
                points,
                `${name}Incenter`
            );
            const outputIncircle = incircle(
                degenerateBoard,
                points,
                `${name}Incircle`
            );
            degenerateBoard.update();
            degenerateCases[name] = {
                bisector: snapshotLine(outputBisector),
                incenter: snapshotPoint(outputIncenter),
                incircle: snapshotCircle(outputIncircle)
            };
        }

        return {
            version: JXG.version,
            initial,
            moved,
            coordinateBisectorInitial,
            coordinateBisectorRemoval,
            coordinateIncenterInitial,
            coordinateIncenterRemoval,
            coordinateIncircleInitial,
            coordinateIncircleRemoval,
            bisectorHelperRemoval,
            incircleCenterRemoval,
            failures,
            degenerateCases
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
