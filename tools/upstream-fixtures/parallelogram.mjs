/*
 * Official JSXGraph 1.13.3 Parallelogram behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/parallelogram.mjs
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
        const coordinates = (point) => point.coords.usrCoords.slice();
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            descendants: Object.keys(element.descendants),
            ancestors: Object.keys(element.ancestors)
        });
        const snapshot = (parallelogram) => ({
            id: parallelogram.id,
            elType: parallelogram.elType,
            type: parallelogram.type,
            elementClass: parallelogram.elementClass,
            draggable: parallelogram.isDraggable,
            withLines: parallelogram.withLines,
            vertices: parallelogram.vertices.map((vertex) => ({
                id: vertex.id,
                coordinates: coordinates(vertex)
            })),
            borders: parallelogram.borders.map((border) => ({
                id: border.id,
                point1: border.point1.id,
                point2: border.point2.id
            })),
            parallelPoint: {
                id: parallelogram.parallelPoint.id,
                elType: parallelogram.parallelPoint.elType,
                type: parallelogram.parallelPoint.type,
                coordinates: coordinates(parallelogram.parallelPoint),
                draggable: parallelogram.parallelPoint.isDraggable,
                fixed: parallelogram.parallelPoint.visProp.fixed,
                isVertex2:
                    parallelogram.parallelPoint ===
                    parallelogram.vertices[2],
                relations: relations(parallelogram.parallelPoint)
            },
            area: parallelogram.Area(),
            perimeter: parallelogram.Perimeter(),
            relations: relations(parallelogram)
        });
        const point = (board, coords, id) =>
            board.create("point", coords, {
                id,
                name: "",
                withLabel: false
            });

        const registeredBoard = createBoard();
        const a = point(registeredBoard, [-4, -1], "A");
        const b = point(registeredBoard, [2, 3], "B");
        const c = point(registeredBoard, [3, -3], "C");
        const registered = registeredBoard.create(
            "parallelogram",
            [a, b, c],
            {
                id: "registered",
                name: "",
                withLabel: false,
                parallelpoint: {
                    id: "registeredHelper",
                    name: "",
                    withLabel: false,
                    fixed: true
                }
            }
        );
        registeredBoard.update();
        const registeredInitial = {
            snapshot: snapshot(registered),
            objectOrder: registeredBoard.objectsList.map(
                (element) => element.id
            ),
            sourceRelations: {
                A: relations(a),
                B: relations(b),
                C: relations(c)
            }
        };

        a.setPositionDirectly(JXG.COORDS_BY_USER, [-2, 2]);
        b.setPositionDirectly(JXG.COORDS_BY_USER, [4, 1]);
        c.setPositionDirectly(JXG.COORDS_BY_USER, [1, -4]);
        registeredBoard.update();
        const registeredMoved = snapshot(registered);

        const registeredIds = [
            "A",
            "B",
            "C",
            "registeredHelper",
            "registered",
            ...registered.borders.map((border) => border.id)
        ];
        registeredBoard.removeObject(registered);
        const registeredRemoval = {
            presence: Object.fromEntries(
                registeredIds.map((id) => [
                    id,
                    Boolean(registeredBoard.objects[id])
                ])
            ),
            helperRelations: relations(
                registeredBoard.objects.registeredHelper
            ),
            sourceRelations: {
                A: relations(a),
                B: relations(b),
                C: relations(c)
            }
        };

        const coordinateBoard = createBoard();
        const coordinate = coordinateBoard.create(
            "parallelogram",
            [[-4, -1], [2, 3], [3, -3]],
            {
                id: "coordinate",
                name: "",
                withLabel: false,
                withLines: false,
                parallelpoint: {
                    id: "coordinateHelper",
                    name: "",
                    withLabel: false
                }
            }
        );
        coordinateBoard.update();
        const coordinateIds = coordinateBoard.objectsList.map(
            (element) => element.id
        );
        const coordinateInitial = {
            snapshot: snapshot(coordinate),
            objectOrder: coordinateIds
        };
        coordinateBoard.removeObject(coordinate);
        const coordinateRemoval = Object.fromEntries(
            coordinateIds.map((id) => [
                id,
                Boolean(coordinateBoard.objects[id])
            ])
        );

        const failures = {};
        for (const [name, parents] of Object.entries({
            tooFew: [[0, 0], [1, 1]],
            invalidAfterCoordinate: [[0, 0], "not-a-point", [1, 1]]
        })) {
            const failureBoard = createBoard();
            const before = failureBoard.objectsList.length;
            try {
                failureBoard.create("parallelogram", parents);
                failures[name] = {threw: false};
            } catch (error) {
                failures[name] = {
                    threw: true,
                    message: String(error),
                    retainedObjects:
                        failureBoard.objectsList.length - before,
                    objectTypes: failureBoard.objectsList.map(
                        (element) => element.elType
                    )
                };
            }
        }

        return {
            version: JXG.version,
            registeredInitial,
            registeredMoved,
            registeredRemoval,
            coordinateInitial,
            coordinateRemoval,
            failures
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
