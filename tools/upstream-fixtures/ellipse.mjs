/*
 * Official JSXGraph 1.13.3 ellipse creator behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/ellipse.mjs
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
        const matrix = (rows) => rows.map(vector);
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
            visible: point.visProp.visible,
            needsRegularUpdate: point.needsRegularUpdate,
            dump: point.dump,
            relations: relations(point)
        });
        const sampleAt = (curve, phi) => ({
            phi: classify(phi),
            x: classify(curve.X(phi)),
            y: classify(curve.Y(phi))
        });
        const snapshotEllipse = (ellipse) => ({
            id: ellipse.id,
            name: ellipse.name,
            elType: ellipse.elType,
            type: ellipse.type,
            elementClass: ellipse.elementClass,
            curveType: ellipse.curveType,
            majorAxis: classify(ellipse.majorAxis()),
            minX: classify(ellipse.minX()),
            maxX: classify(ellipse.maxX()),
            numberPoints: ellipse.numberPoints,
            samples: [
                sampleAt(ellipse, ellipse.minX()),
                sampleAt(
                    ellipse,
                    (ellipse.minX() + ellipse.maxX()) * 0.5
                ),
                sampleAt(ellipse, ellipse.maxX())
            ],
            center: snapshotPoint(ellipse.center),
            midpoint: ellipse.midpoint.id,
            quadraticForm: matrix(ellipse.quadraticform),
            draggable: ellipse.isDraggable,
            fixed: ellipse.visProp.fixed,
            needsRegularUpdate: ellipse.needsRegularUpdate,
            dump: ellipse.dump,
            inherits: ellipse.inherits.map((element) => element.id),
            subs: Object.fromEntries(
                Object.entries(ellipse.subs).map(([key, value]) => [
                    key,
                    value.id
                ])
            ),
            relations: relations(ellipse)
        });
        const point = (board, coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false
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
        const ellipsePoint = point(pointBoard, [0, 5], "ellipsePoint");
        const pointEllipse = pointBoard.create(
            "ellipse",
            [focus1, focus2, ellipsePoint],
            {
                id: "pointEllipse",
                name: "point-ellipse",
                withLabel: false,
                fixed: false,
                needsRegularUpdate: false,
                strokeColor: "#123456",
                strokeWidth: 4
            }
        );
        pointBoard.update();
        const pointInitial = {
            output: snapshotEllipse(pointEllipse),
            sources: {
                focus1: relations(focus1),
                focus2: relations(focus2),
                ellipsePoint: relations(ellipsePoint)
            },
            objectOrder: objectOrder(pointBoard)
        };
        focus1.setPositionDirectly(JXG.COORDS_BY_USER, [-4, 1]);
        focus2.setPositionDirectly(JXG.COORDS_BY_USER, [2, -1]);
        ellipsePoint.setPositionDirectly(JXG.COORDS_BY_USER, [1, 4]);
        pointBoard.update();
        const pointMoved = {
            output: snapshotEllipse(pointEllipse),
            sources: {
                focus1: snapshotPoint(focus1),
                focus2: snapshotPoint(focus2),
                ellipsePoint: snapshotPoint(ellipsePoint)
            }
        };

        const numericBoard = createBoard();
        const numericFocus1 = point(
            numericBoard,
            [-2, 1],
            "numericFocus1"
        );
        const numericFocus2 = point(
            numericBoard,
            [4, 1],
            "numericFocus2"
        );
        const numericEllipse = numericBoard.create(
            "ellipse",
            [numericFocus1, numericFocus2, 10, -Math.PI * 0.5, Math.PI],
            {
                id: "numericEllipse",
                name: "",
                withLabel: false
            }
        );
        numericBoard.update();
        const numericDomain = snapshotEllipse(numericEllipse);

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
        let majorAxis = 10;
        const functionEllipse = functionBoard.create(
            "ellipse",
            [functionFocus1, functionFocus2, () => majorAxis],
            {
                id: "functionEllipse",
                name: "",
                withLabel: false
            }
        );
        functionBoard.update();
        const functionInitial = snapshotEllipse(functionEllipse);
        majorAxis = 14;
        functionBoard.update();
        const functionUpdated = snapshotEllipse(functionEllipse);

        const referenceBoard = createBoard();
        const referenceFocus1 = point(
            referenceBoard,
            [-4, 0],
            "referenceFocus1"
        );
        const referenceFocus2 = point(
            referenceBoard,
            [2, 0],
            "referenceFocus2"
        );
        const referencePoint = point(
            referenceBoard,
            [0, 4],
            "referencePoint"
        );
        const stringEllipse = referenceBoard.create(
            "ellipse",
            ["referenceFocus1", "referenceFocus2", "referencePoint"],
            {
                id: "stringEllipse",
                name: "",
                withLabel: false
            }
        );
        const functionPointEllipse = referenceBoard.create(
            "ellipse",
            [
                () => referenceFocus1,
                () => referenceFocus2,
                () => referencePoint
            ],
            {
                id: "functionPointEllipse",
                name: "",
                withLabel: false
            }
        );
        referenceBoard.update();
        const referenceForms = {
            stringParents: snapshotEllipse(stringEllipse),
            functionParents: snapshotEllipse(functionPointEllipse)
        };

        const coordinateBoard = createBoard();
        const coordinateBefore = objectOrder(coordinateBoard);
        const coordinateEllipse = coordinateBoard.create(
            "ellipse",
            [[-3, 0], [3, 0], [0, 5]],
            {
                id: "coordinateEllipse",
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
            output: snapshotEllipse(coordinateEllipse),
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
        coordinateBoard.removeObject(coordinateEllipse);
        const coordinateRemoval = presence(
            coordinateBoard,
            coordinateAdded
        );

        const parentRemovalBoard = createBoard();
        const parentRemovalBefore = objectOrder(parentRemovalBoard);
        const parentRemovalEllipse = parentRemovalBoard.create(
            "ellipse",
            [[-3, 0], [3, 0], [0, 5]],
            {
                id: "parentRemovalEllipse",
                name: "",
                withLabel: false
            }
        );
        const parentRemovalAdded = objectOrder(parentRemovalBoard).slice(
            parentRemovalBefore.length
        );
        const firstImplicitFocus = parentRemovalEllipse.inherits[1];
        parentRemovalBoard.removeObject(firstImplicitFocus);
        const parentRemoval = presence(
            parentRemovalBoard,
            parentRemovalAdded
        );

        const duplicateBoard = createBoard();
        point(duplicateBoard, [7, 5], "takenEllipse");
        const duplicateBefore = objectOrder(duplicateBoard);
        const duplicateResult = captureError(() =>
            duplicateBoard.create(
                "ellipse",
                [[-3, 0], [3, 0], [0, 5]],
                {
                    id: "takenEllipse",
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
            ["firstLine", [invalidLine, invalidOther, 6]],
            ["secondLine", [invalidPoint, invalidLine, 6]],
            ["thirdLine", [invalidPoint, invalidOther, invalidLine]],
            ["unknownString", ["missing", invalidOther, 6]],
            ["stringMajorAxis", [invalidPoint, invalidOther, "6"]]
        ].map(([name, parents]) => {
            const before = objectOrder(invalidBoard);
            const result = captureError(() =>
                invalidBoard.create("ellipse", parents, {
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
            zero: 0,
            negative: -10,
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
                    "ellipse",
                    [first, second, major],
                    {
                        id: `${name}Ellipse`,
                        name: "",
                        withLabel: false
                    }
                )
            );
            degenerateBoard.update();
            const ellipse = degenerateBoard.objects[`${name}Ellipse`];
            degenerateCases[name] = {
                result,
                output: ellipse ? snapshotEllipse(ellipse) : null,
                objectOrder: objectOrder(degenerateBoard)
            };
        }

        const conicBoard = createBoard();
        const conicFocus1 = point(
            conicBoard,
            [-3, 0],
            "conicFocus1"
        );
        const conicFocus2 = point(
            conicBoard,
            [3, 0],
            "conicFocus2"
        );
        const conicPoint = point(
            conicBoard,
            [0, 5],
            "conicPoint"
        );
        const conicEllipse = conicBoard.create(
            "ellipse",
            [conicFocus1, conicFocus2, conicPoint],
            {
                id: "conicEllipse",
                name: "",
                withLabel: false
            }
        );
        const externalPoint = point(
            conicBoard,
            [7, 4],
            "externalPoint"
        );
        const tangentPoint = point(
            conicBoard,
            [0, 5],
            "tangentPoint"
        );
        const poleLine = conicBoard.create(
            "line",
            [[-8, 4], [8, 4]],
            {
                id: "poleLine",
                name: "",
                withLabel: false
            }
        );
        const conicInterop = {};
        for (const [name, action] of Object.entries({
            tangent: () =>
                conicBoard.create(
                    "tangent",
                    [tangentPoint, conicEllipse],
                    {
                        id: "conicTangent",
                        name: "",
                        withLabel: false
                    }
                ),
            polar: () =>
                conicBoard.create(
                    "polar",
                    [conicEllipse, externalPoint],
                    {
                        id: "conicPolar",
                        name: "",
                        withLabel: false
                    }
                ),
            polepoint: () =>
                conicBoard.create(
                    "polepoint",
                    [conicEllipse, poleLine],
                    {
                        id: "conicPole",
                        name: "",
                        withLabel: false
                    }
                ),
            tangentto: () =>
                conicBoard.create(
                    "tangentto",
                    [conicEllipse, externalPoint, 0],
                    {
                        id: "conicTangentTo",
                        name: "",
                        withLabel: false
                    }
                )
        })) {
            const before = objectOrder(conicBoard);
            const result = captureError(action);
            conicBoard.update();
            const output = result.id ? conicBoard.objects[result.id] : null;
            conicInterop[name] = {
                result,
                stdform: output?.stdform
                    ? vector(output.stdform)
                    : null,
                coordinates: output?.coords
                    ? vector(output.coords.usrCoords)
                    : null,
                addedObjects: objectOrder(conicBoard).slice(before.length)
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
            degenerateCases,
            conicInterop
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
