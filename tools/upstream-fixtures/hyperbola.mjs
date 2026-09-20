/*
 * Official JSXGraph 1.13.3 hyperbola creator behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/hyperbola.mjs
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
        const sampleAt = (curve, phi) => ({
            phi: classify(phi),
            x: classify(curve.X(phi)),
            y: classify(curve.Y(phi))
        });
        const snapshotHyperbola = (hyperbola) => ({
            id: hyperbola.id,
            name: hyperbola.name,
            elType: hyperbola.elType,
            type: hyperbola.type,
            elementClass: hyperbola.elementClass,
            curveType: hyperbola.curveType,
            majorAxis: classify(hyperbola.majorAxis()),
            minX: classify(hyperbola.minX()),
            maxX: classify(hyperbola.maxX()),
            numberPoints: hyperbola.numberPoints,
            samples: [
                sampleAt(hyperbola, hyperbola.minX()),
                sampleAt(hyperbola, 0),
                sampleAt(hyperbola, hyperbola.maxX())
            ],
            center: snapshotPoint(hyperbola.center),
            midpoint: hyperbola.midpoint.id,
            quadraticForm: matrix(hyperbola.quadraticform),
            draggable: hyperbola.isDraggable,
            fixed: hyperbola.visProp.fixed,
            needsRegularUpdate: hyperbola.needsRegularUpdate,
            dump: hyperbola.dump,
            inherits: hyperbola.inherits.map((element) => element.id),
            subs: Object.fromEntries(
                Object.entries(hyperbola.subs).map(([key, value]) => [
                    key,
                    value.id
                ])
            ),
            relations: relations(hyperbola)
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
        const focus1 = point(pointBoard, [-3, 0], "focus1");
        const focus2 = point(pointBoard, [3, 0], "focus2");
        const hyperbolaPoint = point(pointBoard, [5, 2], "hyperbolaPoint");
        const pointHyperbola = pointBoard.create(
            "hyperbola",
            [focus1, focus2, hyperbolaPoint],
            {
                id: "pointHyperbola",
                name: "point-hyperbola",
                withLabel: false,
                fixed: false,
                needsRegularUpdate: false,
                strokeColor: "#123456",
                strokeWidth: 4
            }
        );
        pointBoard.update();
        const pointInitial = {
            output: snapshotHyperbola(pointHyperbola),
            sources: {
                focus1: relations(focus1),
                focus2: relations(focus2),
                hyperbolaPoint: relations(hyperbolaPoint)
            },
            objectOrder: objectOrder(pointBoard)
        };
        focus1.setPositionDirectly(JXG.COORDS_BY_USER, [-4, 1]);
        focus2.setPositionDirectly(JXG.COORDS_BY_USER, [2, -1]);
        hyperbolaPoint.setPositionDirectly(JXG.COORDS_BY_USER, [5, 3]);
        pointBoard.update();
        const pointMoved = snapshotHyperbola(pointHyperbola);

        const numericBoard = createBoard();
        const numericHyperbola = numericBoard.create(
            "hyperbola",
            [
                point(numericBoard, [-3, 1], "numericFocus1"),
                point(numericBoard, [3, 1], "numericFocus2"),
                4,
                -Math.PI * 0.5,
                Math.PI * 0.5
            ],
            {
                id: "numericHyperbola",
                name: "",
                withLabel: false
            }
        );
        numericBoard.update();
        const numericDomain = snapshotHyperbola(numericHyperbola);

        const functionBoard = createBoard();
        const functionFocus1 = point(
            functionBoard,
            [-3, 0],
            "functionFocus1"
        );
        const functionFocus2 = point(
            functionBoard,
            [3, 0],
            "functionFocus2"
        );
        let majorAxis = 4;
        const functionHyperbola = functionBoard.create(
            "hyperbola",
            [functionFocus1, functionFocus2, () => majorAxis],
            {
                id: "functionHyperbola",
                name: "",
                withLabel: false
            }
        );
        functionBoard.update();
        const functionInitial = snapshotHyperbola(functionHyperbola);
        majorAxis = 2;
        functionBoard.update();
        const functionUpdated = snapshotHyperbola(functionHyperbola);

        const referenceBoard = createBoard();
        const referenceFocus1 = point(
            referenceBoard,
            [-3, 0],
            "referenceFocus1"
        );
        const referenceFocus2 = point(
            referenceBoard,
            [3, 0],
            "referenceFocus2"
        );
        const referencePoint = point(
            referenceBoard,
            [5, 2],
            "referencePoint"
        );
        const referenceForms = {
            stringParents: snapshotHyperbola(
                referenceBoard.create(
                    "hyperbola",
                    [
                        "referenceFocus1",
                        "referenceFocus2",
                        "referencePoint"
                    ],
                    {
                        id: "stringHyperbola",
                        name: "",
                        withLabel: false
                    }
                )
            ),
            functionParents: snapshotHyperbola(
                referenceBoard.create(
                    "hyperbola",
                    [
                        () => referenceFocus1,
                        () => referenceFocus2,
                        () => referencePoint
                    ],
                    {
                        id: "functionPointHyperbola",
                        name: "",
                        withLabel: false
                    }
                )
            )
        };

        const coordinateBoard = createBoard();
        const coordinateBefore = objectOrder(coordinateBoard);
        const coordinateHyperbola = coordinateBoard.create(
            "hyperbola",
            [[-3, 0], [3, 0], [5, 2]],
            {
                id: "coordinateHyperbola",
                name: "",
                withLabel: false,
                foci: {
                    fixed: true,
                    visible: true
                },
                center: {
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
            output: snapshotHyperbola(coordinateHyperbola),
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
                    relations: relations(element)
                };
            })
        };
        coordinateBoard.removeObject(coordinateHyperbola);
        const coordinateRemoval = presence(
            coordinateBoard,
            coordinateAdded
        );

        const parentRemovalBoard = createBoard();
        const parentRemovalBefore = objectOrder(parentRemovalBoard);
        const parentRemovalHyperbola = parentRemovalBoard.create(
            "hyperbola",
            [[-3, 0], [3, 0], [5, 2]],
            {
                id: "parentRemovalHyperbola",
                name: "",
                withLabel: false
            }
        );
        const parentRemovalAdded = objectOrder(parentRemovalBoard).slice(
            parentRemovalBefore.length
        );
        parentRemovalBoard.removeObject(parentRemovalHyperbola.inherits[1]);
        const parentRemoval = presence(
            parentRemovalBoard,
            parentRemovalAdded
        );

        const duplicateBoard = createBoard();
        point(duplicateBoard, [7, 5], "takenHyperbola");
        const duplicateBefore = objectOrder(duplicateBoard);
        const duplicateResult = captureError(() =>
            duplicateBoard.create(
                "hyperbola",
                [[-3, 0], [3, 0], [5, 2]],
                {
                    id: "takenHyperbola",
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
        const invalidPoint = point(
            invalidBoard,
            [0, 0],
            "invalidPoint"
        );
        const invalidOther = point(
            invalidBoard,
            [2, 0],
            "invalidOther"
        );
        const invalidLine = invalidBoard.create(
            "line",
            [invalidPoint, invalidOther],
            {
                id: "invalidLine",
                name: "",
                withLabel: false
            }
        );
        const invalidParents = [
            ["tooFew", [invalidPoint, invalidOther]],
            ["firstLine", [invalidLine, invalidOther, 4]],
            ["secondLine", [invalidPoint, invalidLine, 4]],
            ["thirdLine", [invalidPoint, invalidOther, invalidLine]],
            ["unknownString", ["missing", invalidOther, 4]],
            ["stringMajorAxis", [invalidPoint, invalidOther, "4"]]
        ].map(([name, parents]) => {
            const before = objectOrder(invalidBoard);
            const result = captureError(() =>
                invalidBoard.create("hyperbola", parents, {
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
        for (const [name, major] of Object.entries({
            shorterThanFocalDistance: 4,
            equalToFocalDistance: 6,
            zero: 0,
            negative: -4,
            nan: Number.NaN,
            positiveInfinity: Number.POSITIVE_INFINITY
        })) {
            const degenerateBoard = createBoard();
            const first = point(
                degenerateBoard,
                [-3, 0],
                `${name}Focus1`
            );
            const second = point(
                degenerateBoard,
                [3, 0],
                `${name}Focus2`
            );
            const result = captureError(() =>
                degenerateBoard.create(
                    "hyperbola",
                    [first, second, major],
                    {
                        id: `${name}Hyperbola`,
                        name: "",
                        withLabel: false
                    }
                )
            );
            degenerateBoard.update();
            const hyperbola =
                degenerateBoard.objects[`${name}Hyperbola`];
            degenerateCases[name] = {
                result,
                output: hyperbola
                    ? snapshotHyperbola(hyperbola)
                    : null,
                objectOrder: objectOrder(degenerateBoard)
            };
        }

        return {
            version: JXG.version,
            pointInitial,
            pointMoved,
            numericDomain,
            functionInitial,
            functionUpdated,
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
