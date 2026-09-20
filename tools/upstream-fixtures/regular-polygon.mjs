/*
 * Official JSXGraph 1.13.3 RegularPolygon behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/regular-polygon.mjs
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
        const point = (board, coords, id) =>
            board.create("point", coords, {
                id,
                name: "",
                withLabel: false
            });
        const snapshot = (polygon) => ({
            id: polygon.id,
            elType: polygon.elType,
            type: polygon.type,
            elementClass: polygon.elementClass,
            draggable: polygon.isDraggable,
            withLines: polygon.withLines,
            vertices: polygon.vertices.map((vertex) => ({
                id: vertex.id,
                elType: vertex.elType,
                type: vertex.type,
                coordinates: coordinates(vertex),
                draggable: vertex.isDraggable,
                fixed: vertex.visProp.fixed,
                baseElement: vertex.baseElement?.id ?? null,
                transformationTypes: vertex.transformations.map(
                    (transformation) => transformation.type
                ),
                relations: relations(vertex)
            })),
            borders: polygon.borders.map((border) => ({
                id: border.id,
                point1: border.point1.id,
                point2: border.point2.id
            })),
            area: polygon.Area(),
            perimeter: polygon.Perimeter(),
            relations: relations(polygon)
        });

        const generatedBoard = createBoard();
        const generatedFirst = point(
            generatedBoard,
            [-3, -1],
            "generatedFirst"
        );
        const generatedSecond = point(
            generatedBoard,
            [0, 2],
            "generatedSecond"
        );
        const generated = generatedBoard.create(
            "regularpolygon",
            [generatedFirst, generatedSecond, 5],
            {
                id: "generated",
                name: "",
                withLabel: false,
                vertices: {
                    ids: [
                        "generatedThird",
                        "generatedFourth",
                        "generatedFifth"
                    ],
                    name: "",
                    withLabel: false,
                    fixed: true
                }
            }
        );
        generatedBoard.update();
        const generatedInitial = {
            snapshot: snapshot(generated),
            objectOrder: generatedBoard.objectsList.map(
                (element) => element.id
            )
        };

        generatedFirst.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-4, 1]
        );
        generatedSecond.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [1, 3]
        );
        generatedBoard.update();
        const generatedMoved = snapshot(generated);

        const generatedIds = generatedBoard.objectsList.map(
            (element) => element.id
        );
        generatedBoard.removeObject(generated);
        const generatedRemoval = {
            presence: Object.fromEntries(
                generatedIds.map((id) => [
                    id,
                    Boolean(generatedBoard.objects[id])
                ])
            ),
            objectOrder: generatedBoard.objectsList.map(
                (element) => element.id
            )
        };

        const existingBoard = createBoard();
        const existingPoints = [
            point(existingBoard, [-2, -2], "existingFirst"),
            point(existingBoard, [1, -2], "existingSecond"),
            point(existingBoard, [7, 3], "existingThird"),
            point(existingBoard, [-6, 5], "existingFourth")
        ];
        const existing = existingBoard.create(
            "regularpolygon",
            existingPoints,
            {
                id: "existing",
                name: "",
                withLabel: false,
                withLines: false
            }
        );
        existingBoard.update();
        const existingInitial = {
            snapshot: snapshot(existing),
            objectOrder: existingBoard.objectsList.map(
                (element) => element.id
            )
        };

        existingPoints[0].setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-3, -1]
        );
        existingPoints[1].setPositionDirectly(
            JXG.COORDS_BY_USER,
            [2, 0]
        );
        existingBoard.update();
        const existingMoved = snapshot(existing);
        existingBoard.removeObject(existing);
        const existingRemoval = {
            presence: Object.fromEntries(
                existingPoints.map((vertex) => [
                    vertex.id,
                    Boolean(existingBoard.objects[vertex.id])
                ])
            ),
            snapshots: existingPoints.map((vertex) => ({
                id: vertex.id,
                coordinates: coordinates(vertex),
                baseElement: vertex.baseElement?.id ?? null,
                transformationCount: vertex.transformations.length,
                relations: relations(vertex)
            }))
        };

        const coordinateBoard = createBoard();
        const coordinate = coordinateBoard.create(
            "regularpolygon",
            [[-2, -1], [1, 1], 4],
            {
                id: "coordinate",
                name: "",
                withLabel: false,
                withLines: false,
                vertices: {
                    ids: ["coordinateThird", "coordinateFourth"],
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
        const coordinateRemoval = {
            presence: Object.fromEntries(
                coordinateIds.map((id) => [
                    id,
                    Boolean(coordinateBoard.objects[id])
                ])
            ),
            objectOrder: coordinateBoard.objectsList.map(
                (element) => element.id
            )
        };

        const fractionalBoard = createBoard();
        const fractional = fractionalBoard.create(
            "regularpolygon",
            [[-1, 0], [1, 0], 3.5],
            {
                id: "fractional",
                name: "",
                withLabel: false,
                withLines: false,
                vertices: {
                    ids: ["fractionalThird", "fractionalFourth"],
                    name: "",
                    withLabel: false
                }
            }
        );
        fractionalBoard.update();
        const fractionalN = {
            snapshot: snapshot(fractional),
            objectOrder: fractionalBoard.objectsList.map(
                (element) => element.id
            )
        };

        const failures = {};
        const failureCases = {
            tooSmallN: {
                parents: [[0, 0], [1, 0], 2]
            },
            numericWrongParentCount: {
                parents: [[0, 0], [1, 0], [1, 1], 4]
            },
            invalidAfterCoordinate: {
                parents: [[0, 0], "not-a-point", 4]
            },
            duplicateFirstHelperId: {
                setup(board) {
                    point(board, [8, 8], "taken");
                    return [
                        point(board, [-1, 0], "A"),
                        point(board, [1, 0], "B"),
                        4
                    ];
                },
                attributes: {
                    vertices: {
                        ids: ["taken", "unused"]
                    }
                }
            },
            duplicateSecondHelperId: {
                setup(board) {
                    point(board, [8, 8], "taken");
                    return [
                        point(board, [-1, 0], "A"),
                        point(board, [1, 0], "B"),
                        4
                    ];
                },
                attributes: {
                    vertices: {
                        ids: ["retainedFirstHelper", "taken"]
                    }
                }
            },
            duplicatePolygonId: {
                setup(board) {
                    point(board, [8, 8], "taken");
                    return [
                        point(board, [-1, 0], "A"),
                        point(board, [1, 0], "B"),
                        4
                    ];
                },
                attributes: {
                    id: "taken",
                    vertices: {
                        ids: ["retainedThird", "retainedFourth"]
                    }
                }
            }
        };
        for (const [name, specification] of Object.entries(failureCases)) {
            const failureBoard = createBoard();
            const parents = specification.setup
                ? specification.setup(failureBoard)
                : specification.parents;
            const beforeIds = new Set(
                failureBoard.objectsList.map((element) => element.id)
            );
            try {
                const result = failureBoard.create(
                    "regularpolygon",
                    parents,
                    specification.attributes ?? {}
                );
                failures[name] = {
                    threw: false,
                    result: snapshot(result),
                    retainedObjects: failureBoard.objectsList
                        .filter((element) => !beforeIds.has(element.id))
                        .map((element) => ({
                            id: element.id,
                            elType: element.elType,
                            type: element.type
                        })),
                    objectOrder: failureBoard.objectsList.map(
                        (element) => element.id
                    )
                };
            } catch (error) {
                failures[name] = {
                    threw: true,
                    message: String(error),
                    retainedObjects: failureBoard.objectsList
                        .filter((element) => !beforeIds.has(element.id))
                        .map((element) => ({
                            id: element.id,
                            elType: element.elType,
                            type: element.type
                        })),
                    objectOrder: failureBoard.objectsList.map(
                        (element) => element.id
                    )
                };
            }
        }

        return {
            version: JXG.version,
            generatedInitial,
            generatedMoved,
            generatedRemoval,
            existingInitial,
            existingMoved,
            existingRemoval,
            coordinateInitial,
            coordinateRemoval,
            fractionalN,
            failures
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
